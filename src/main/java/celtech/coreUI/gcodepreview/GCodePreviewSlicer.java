/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.gcodepreview;

import celtech.Lookup;
import celtech.appManager.ModelContainerProject;
import celtech.configuration.ApplicationConfiguration;
import celtech.roboxbase.configuration.SlicerType;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.RoboxProfile;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.configuration.slicer.Cura3ConfigConvertor;
import celtech.roboxbase.configuration.slicer.SlicerConfigWriter;
import celtech.roboxbase.configuration.slicer.SlicerConfigWriterFactory;
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
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * This class uses SlicerTask and PostProcessorTask to get the estimated time,
 * weight and cost for the given project and settings.
 *
 * @author tony
 */
public class GCodePreviewSlicer
{

    private final Stenographer steno = StenographerFactory.getStenographer(GCodePreviewSlicer.class.getName());

    //We are allowed to use ModelContainerProject here since this class can only run calcs for projects with meshes
    private final ModelContainerProject project;
    private final String temporaryDirectory;

    private File printJobDirectory;
    private final Cancellable cancellable;
    private Random random = new Random();

    public GCodePreviewSlicer(ModelContainerProject project, Cancellable cancellable)
    {
        this.project = project;
        this.cancellable = cancellable;

        temporaryDirectory = BaseConfiguration.getApplicationStorageDirectory()
                + ApplicationConfiguration.previewGCodeFileSubpath
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
        // Maybe kill preview?
    }

    private boolean isCancelled()
    {
        return cancellable.cancelled().get();
    }

    public Optional<GCodePostProcessingResult> runSlicerAndPostProcessor() throws IOException
    {

//        steno.debug("launch time cost process for project " + project + " and settings "
//                + settings.getProfileName());
        if (isCancelled())
        {
            return Optional.empty();
        }

        List<MeshForProcessing> meshesForProcessing = new ArrayList<>();
        List<Integer> extruderForModel = new ArrayList<>();

        // Only to be run on a ModelContainerProject
        for (ProjectifiableThing modelContainer : project.getTopLevelThings())
        {
            for (ModelContainer modelContainerWithMesh : ((ModelContainer)modelContainer).getModelsHoldingMeshViews())
            {
                MeshForProcessing meshForProcessing = new MeshForProcessing(modelContainerWithMesh.getMeshView(), modelContainerWithMesh);
                meshesForProcessing.add(meshForProcessing);
                extruderForModel.add(modelContainerWithMesh.getAssociateWithExtruderNumberProperty().get());
            }
        }

        Printer printer = Lookup.getSelectedPrinterProperty().get();
        String currentHeadType = HeadContainer.defaultHeadID;
        SlicerType slicerType = Lookup.getUserPreferences().getSlicerType();
        
        if (printer != null && printer.headProperty().get() != null)
        {
            currentHeadType = printer.headProperty().get().typeCodeProperty().get();
        }
        RoboxProfile settings = project.getPrinterSettings().getSettings(currentHeadType, slicerType);
        
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
                "Preview",
                "preview",
                settings,
                project.getPrinterSettings(),
                project.getPrintQuality(),
                slicerType,
                centreOfPrintedObject,
                Lookup.getUserPreferences().isSafetyFeaturesOn(),
                false,
                null);

        boolean succeeded = doSlicing(printableMeshes, settings);
        if (!succeeded || isCancelled())
        {
            return Optional.empty();
        }

        steno.debug("start post processing");

        GCodePostProcessingResult result = PostProcessorTask.doPostProcessing(
                settings.getName(),
                printableMeshes,
                temporaryDirectory,
                printer,
                null,
                slicerType);

        if (isCancelled())
        {
            return Optional.empty();
        }

        return Optional.of(result);
    }

    /**
     * Set up a print job directory etc run the slicer.
     */
    private boolean doSlicing(PrintableMeshes printableMeshes, RoboxProfile settings)
    {
        settings = project.getPrinterSettings().applyOverrides(Optional.ofNullable(settings));

        //Create the print job directory
        printJobDirectory = new File(temporaryDirectory);
        printJobDirectory.mkdirs();

        //Write out the slicer config
        SlicerType slicerTypeToUse = null;
        
        slicerTypeToUse = Lookup.getUserPreferences().getSlicerType();
        
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
}
