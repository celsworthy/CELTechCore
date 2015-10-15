/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.SlicerType;
import celtech.configuration.UserPreferences;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.slicer.SlicerConfigWriter;
import celtech.configuration.slicer.SlicerConfigWriterFactory;
import celtech.gcodetranslator.PrintJobStatistics;
import celtech.printerControl.model.Printer;
import celtech.services.postProcessor.GCodePostProcessingResult;
import celtech.services.postProcessor.PostProcessorTask;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SliceResult;
import celtech.services.slicer.SlicerTask;
import celtech.utils.Time.TimeUtils;
import celtech.utils.tasks.Cancellable;
import celtech.utils.threed.ThreeDUtils;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javafx.beans.value.ObservableValue;
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

    private final Project project;
    private final Label lblTime;
    private final Label lblWeight;
    private final Label lblCost;
    private final SlicerParametersFile settings;
    private final String temporaryDirectory;

    private File printJobDirectory;
    private final Cancellable cancellable;
    private Random random = new Random();

    public GetTimeWeightCost(Project project, SlicerParametersFile settings,
            Label lblTime, Label lblWeight, Label lblCost, Cancellable cancellable)
    {
        this.project = project;
        this.lblTime = lblTime;
        this.lblWeight = lblWeight;
        this.lblCost = lblCost;
        this.settings = settings;
        this.cancellable = cancellable;

        temporaryDirectory = ApplicationConfiguration.getApplicationStorageDirectory()
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
        Lookup.getTaskExecutor().runOnGUIThread(() ->
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

        boolean succeeded = doSlicing(project, settings);
        if (! succeeded) {
            return false;
        }

        if (isCancelled())
        {
            return false;
        }

        Printer printer = Lookup.getSelectedPrinterProperty().get();

        steno.debug("start post processing");

        GCodePostProcessingResult result = PostProcessorTask.doPostProcessing(
                settings.getProfileName(),
                temporaryDirectory,
                printer,
                project,
                settings,
                null);
        PrintJobStatistics printJobStatistics = result.getRoboxiserResult().
                getPrintJobStatistics();

        if (isCancelled())
        {
            return false;
        }

        if (result.getRoboxiserResult().isSuccess())
        {
            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                updateFieldsForStatistics(printJobStatistics);
            });
        }

        return result.getRoboxiserResult().isSuccess();
    }

    /**
     * Update the time/cost/weight fields based on the given statistics.
     */
    private void updateFieldsForStatistics(PrintJobStatistics printJobStatistics)
    {
        String formattedDuration = formatDuration(printJobStatistics.getPredictedDuration());

        double eVolumeUsed = printJobStatistics.geteVolumeUsed();
        double dVolumeUsed = printJobStatistics.getdVolumeUsed();

        Filament filament0 = project.getPrinterSettings().getFilament0();
        Filament filament1 = project.getPrinterSettings().getFilament1();

        lblTime.setText(formattedDuration);

        double weight = 0;
        double costGBP = 0;

        if (filament0 == null
                && filament1 == null)
        {
            // If there is no filament loaded...
            String noFilament = Lookup.i18n("timeCost.noFilament");
            lblWeight.setText(noFilament);
            lblCost.setText(noFilament);
        } else
        {
            if (filament0 != null)
            {
                weight += filament0.getWeightForVolume(eVolumeUsed * 1e-9);
                costGBP += filament0.getCostForVolume(eVolumeUsed * 1e-9);
            }

            if (filament1 != null)
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
    private boolean doSlicing(Project project, SlicerParametersFile settings)
    {
        settings = project.getPrinterSettings().applyOverrides(settings);

        //Create the print job directory
        printJobDirectory = new File(temporaryDirectory);
        printJobDirectory.mkdirs();

        //Write out the slicer config
        SlicerType slicerTypeToUse = null;
        if (settings.getSlicerOverride() != null)
        {
            slicerTypeToUse = settings.getSlicerOverride();
        } else
        {
            slicerTypeToUse = Lookup.getUserPreferences().getSlicerType();
        }

        SlicerConfigWriter configWriter = SlicerConfigWriterFactory.getConfigWriter(
                slicerTypeToUse);

        //We need to tell the slicers where the centre of the printed objects is - otherwise everything is put in the centre of the bed...
        Vector3D centreOfPrintedObject = ThreeDUtils.calculateCentre(project.getTopLevelModels());
        configWriter.setPrintCentre((float) (centreOfPrintedObject.getX()),
                (float) (centreOfPrintedObject.getZ()));
        configWriter.generateConfigForSlicer(settings,
                temporaryDirectory
                + settings.getProfileName()
                + ApplicationConfiguration.printProfileFileExtension);

        Printer printerToUse = null;

        if (Lookup.getSelectedPrinterProperty().isNotNull().get())
        {
            printerToUse = Lookup.getSelectedPrinterProperty().get();
        }

        SliceResult sliceResult = SlicerTask.doSlicing(settings.getProfileName(), settings,
                temporaryDirectory,
                project,
                PrintQualityEnumeration.DRAFT,
                printerToUse, null, steno);
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
