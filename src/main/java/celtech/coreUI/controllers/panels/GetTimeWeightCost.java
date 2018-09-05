/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ModelContainerProject;
import celtech.configuration.ApplicationConfiguration;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.datafileaccessors.FilamentContainer;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.RoboxProfile;
import celtech.roboxbase.configuration.slicer.Cura3ConfigConvertor;
import celtech.roboxbase.configuration.slicer.SlicerConfigWriter;
import celtech.roboxbase.configuration.slicer.SlicerConfigWriterFactory;
import celtech.roboxbase.postprocessor.PrintJobStatistics;
import celtech.roboxbase.utils.models.PrintableMeshes;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.services.postProcessor.GCodePostProcessingResult;
import celtech.roboxbase.services.postProcessor.PostProcessorTask;
import celtech.roboxbase.services.slicer.SliceResult;
import celtech.roboxbase.services.slicer.SlicerTask;
import celtech.roboxbase.utils.models.MeshForProcessing;
import celtech.roboxbase.utils.tasks.Cancellable;
import celtech.roboxbase.utils.threed.CentreCalculations;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * This class uses SlicerTask and PostProcessorTask to get the estimated time,
 * weight and cost for the given project and settings.
 *
 * @author tony
 */
public class GetTimeWeightCost
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            GetTimeWeightCost.class.getName());

    //We are allowed to use ModelContainerProject here since this class can only run calcs for projects with meshes
    private final ModelContainerProject project;
    private final Label lblTime;
    private final Label lblWeight;
    private final Label lblCost;
    private final RoboxProfile settings;
    private final String temporaryDirectory;

    private File printJobDirectory;
    private final Cancellable cancellable;
    private Random random = new Random();

    public GetTimeWeightCost(ModelContainerProject project, RoboxProfile settings,
            Label lblTime, Label lblWeight, Label lblCost, Cancellable cancellable)
    {
        this.project = project;
        this.lblTime = lblTime;
        this.lblWeight = lblWeight;
        this.lblCost = lblCost;
        this.settings = settings;
        this.cancellable = cancellable;

        temporaryDirectory = BaseConfiguration.getApplicationStorageDirectory()
                + ApplicationConfiguration.timeAndCostFileSubpath
                + random.nextInt(10000)
                + File.separator;

        new File(temporaryDirectory).mkdirs();

        cancellable.cancelled().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showCancelled();
                });
    }

    private void showCancelled()
    {
        String cancelled = Lookup.i18n("timeCost.cancelled");
        BaseLookup.getTaskExecutor().runOnGUIThread(() ->
        {
            lblTime.setText(cancelled);
            lblWeight.setText(cancelled);
            lblCost.setText(cancelled);
        });
    }

    private boolean isCancelled()
    {
        return cancellable.cancelled().get();
    }

    public boolean runSlicerAndPostProcessor() throws IOException
    {

//        steno.debug("launch time cost process for project " + project + " and settings "
//                + settings.getProfileName());
        if (isCancelled())
        {
            return false;
        }

        List<MeshForProcessing> meshesForProcessing = new ArrayList<>();
        List<Integer> extruderForModel = new ArrayList<>();

        // Only to be run on a ModelContainerProject
        for (ProjectifiableThing thing : project.getTopLevelThings())
        {
            if (thing instanceof ModelContainer)
            {
                for (ModelContainer modelContainerWithMesh : ((ModelContainer)thing).getModelsHoldingMeshViews())
                {
                    MeshForProcessing meshForProcessing = new MeshForProcessing(modelContainerWithMesh.getMeshView(), modelContainerWithMesh);
                    meshesForProcessing.add(meshForProcessing);
                    extruderForModel.add(modelContainerWithMesh.getAssociateWithExtruderNumberProperty().get());
                }
            }
        }

        Printer printer = Lookup.getSelectedPrinterProperty().get();

        //We need to tell the slicers where the centre of the printed objects is - otherwise everything is put in the centre of the bed...
        CentreCalculations centreCalc = new CentreCalculations();

        project.getTopLevelThings().forEach(model ->
        {
            Bounds modelBounds = model.getBoundsInParent();
            centreCalc.processPoint(modelBounds.getMinX(), modelBounds.getMinY(), modelBounds.getMinZ());
            centreCalc.processPoint(modelBounds.getMaxX(), modelBounds.getMaxY(), modelBounds.getMaxZ());
        });

        Vector3D centreOfPrintedObject = centreCalc.getResult();

        PrintableMeshes printableMeshes = new PrintableMeshes(
                meshesForProcessing,
                project.getUsedExtruders(printer),
                extruderForModel,
                "Time and Cost",
                "bart",
                settings,
                project.getPrinterSettings(),
                project.getPrintQuality(),
                Lookup.getUserPreferences().getSlicerType(),
                centreOfPrintedObject,
                Lookup.getUserPreferences().isSafetyFeaturesOn(),
                false,
                null);

        boolean succeeded = doSlicing(printableMeshes, settings);
        if (!succeeded)
        {
            return false;
        }

        if (isCancelled())
        {
            return false;
        }

        steno.debug("start post processing");

        GCodePostProcessingResult result = PostProcessorTask.doPostProcessing(
                settings.getName(),
                printableMeshes,
                temporaryDirectory,
                printer,
                null,
                Lookup.getUserPreferences().getSlicerType());

        PrintJobStatistics printJobStatistics = result.getRoboxiserResult().
                getPrintJobStatistics();

        if (isCancelled())
        {
            return false;
        }

        if (result.getRoboxiserResult().isSuccess())
        {
            BaseLookup.getTaskExecutor().runOnGUIThread(() ->
            {
                updateFieldsForStatistics(printJobStatistics, printer);
            });
        }

        return result.getRoboxiserResult().isSuccess();
    }

    /**
     * Update the time/cost/weight fields based on the given statistics.
     */
    private void updateFieldsForStatistics(PrintJobStatistics printJobStatistics, Printer printer)
    {
        String formattedDuration = formatDuration(printJobStatistics.getPredictedDuration());

        double eVolumeUsed = printJobStatistics.geteVolumeUsed();
        double dVolumeUsed = printJobStatistics.getdVolumeUsed();

        Filament filament0 = FilamentContainer.UNKNOWN_FILAMENT;
        Filament filament1 = FilamentContainer.UNKNOWN_FILAMENT;

        if (printer != null)
        {
            filament0 = printer.effectiveFilamentsProperty().get(0);
            filament1 = printer.effectiveFilamentsProperty().get(1);
        }

        lblTime.setText(formattedDuration);

        double weight = 0;
        double costGBP = 0;

        if (filament0 == FilamentContainer.UNKNOWN_FILAMENT
                && filament1 == FilamentContainer.UNKNOWN_FILAMENT)
        {
            // If there is no filament loaded...
            String noFilament = Lookup.i18n("timeCost.noFilament");
            lblWeight.setText(noFilament);
            lblCost.setText(noFilament);
        } else
        {
            if (filament0 != FilamentContainer.UNKNOWN_FILAMENT)
            {
                weight += filament0.getWeightForVolume(eVolumeUsed * 1e-9);
                costGBP += filament0.getCostForVolume(eVolumeUsed * 1e-9);
            }

            if (filament1 != FilamentContainer.UNKNOWN_FILAMENT)
            {
                weight += filament1.getWeightForVolume(dVolumeUsed * 1e-9);
                costGBP += filament1.getCostForVolume(dVolumeUsed * 1e-9);
            }

            String formattedWeight = formatWeight(weight);
            String formattedCost = formatCost(costGBP);
            lblWeight.setText(formattedWeight);
            lblCost.setText(formattedCost);
        }
    }

    /**
     * Set up a print job directory etc run the slicer.
     */
    private boolean doSlicing(PrintableMeshes printableMeshes, RoboxProfile settings)
    {
        settings = project.getPrinterSettings().applyOverrides(Optional.of(settings));

        //Create the print job directory
        printJobDirectory = new File(temporaryDirectory);
        printJobDirectory.mkdirs();

        //Write out the slicer config
        SlicerType slicerTypeToUse = Lookup.getUserPreferences().getSlicerType();
        
        Printer printerToUse = null;

        if (Lookup.getSelectedPrinterProperty().isNotNull().get())
        {
            printerToUse = Lookup.getSelectedPrinterProperty().get();
        }
 
        SlicerConfigWriter configWriter = SlicerConfigWriterFactory.getConfigWriter(
                slicerTypeToUse);

        configWriter.setPrintCentre((float) (printableMeshes.getCentreOfPrintedObject().getX()),
                (float) (printableMeshes.getCentreOfPrintedObject().getZ()));
        
        String configFileDest = temporaryDirectory
                + settings.getName()
                + BaseConfiguration.printProfileFileExtension;
        
        configWriter.generateConfigForSlicer(settings, configFileDest);

         if(slicerTypeToUse == SlicerType.Cura3) {
             Cura3ConfigConvertor cura3ConfigConvertor = new Cura3ConfigConvertor(printerToUse, printableMeshes);
             cura3ConfigConvertor.injectConfigIntoCura3SettingsFile(configFileDest);
        }
        
        SliceResult sliceResult = SlicerTask.doSlicing(
                settings.getName(),
                printableMeshes,
                temporaryDirectory,
                printerToUse,
                null,
                steno);
        return sliceResult.isSuccess();
    }
    
    /**
     * Take the duration in seconds and return a string in the format H MM.
     */
    private String formatDuration(double duration)
    {
        int SECONDS_PER_HOUR = 3600;
        int numHours = (int) (duration / SECONDS_PER_HOUR);
        int numMinutes = (int) ((duration - (numHours * SECONDS_PER_HOUR)) / 60);
        return String.format("%sh %sm", numHours, numMinutes);
    }

    /**
     * Take the weight in grammes and return a string in the format NNNg.
     */
    private String formatWeight(double weight)
    {
        return String.format("%sg", (int) weight);
    }

    /**
     * Take the cost in pounds and return a string in the format £1.43.
     */
    private String formatCost(final double cost)
    {
        double convertedCost = cost * Lookup.getUserPreferences().getcurrencyGBPToLocalMultiplier();
        int numPounds = (int) convertedCost;
        int numPence = (int) ((convertedCost - numPounds) * 100);
        return String.format(Lookup.getUserPreferences().getCurrencySymbol().getDisplayString() + "%s.%02d", numPounds, numPence);
    }

}
