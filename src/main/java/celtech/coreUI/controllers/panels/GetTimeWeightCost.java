/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.SlicerType;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.slicer.SlicerConfigWriter;
import celtech.configuration.slicer.SlicerConfigWriterFactory;
import celtech.gcodetranslator.PrintJobStatistics;
import celtech.services.postProcessor.GCodePostProcessingResult;
import celtech.services.postProcessor.PostProcessorTask;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SliceResult;
import celtech.services.slicer.SlicerTask;
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
 * This class uses SlicerTask and PostProcessorTask to get the estimated time, weight and cost for
 * the given project and settings.
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

        steno.debug("launch time cost process for project " + project + " and settings "
            + settings.getProfileName());

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

        GCodePostProcessingResult result = PostProcessorTask.doPostProcessing(
            settings.getProfileName(),
            settings, temporaryDirectory,
            null, null);
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
        double duration = printJobStatistics.getLayerNumberToPredictedDuration().stream().
            mapToDouble(
                Double::doubleValue).sum();

        String formattedDuration = formatDuration(duration);

        double volumeUsed = printJobStatistics.getVolumeUsed();
        //TODO make work with dual extruders
        Filament filament = project.getPrinterSettings().getFilament0();

        lblTime.setText(formattedDuration);

        if (filament != null)
        {
            double weight = filament.getWeightForVolume(volumeUsed * 1e-9);
            String formattedWeight = formatWeight(weight);

            double costGBP = filament.getCostForVolume(volumeUsed * 1e-9);
            String formattedCost = formatCost(costGBP);

            lblWeight.setText(formattedWeight);
            lblCost.setText(formattedCost);
        } else
        {
            // If there is no filament loaded...
            String noFilament = Lookup.i18n("timeCost.noFilament");
            lblWeight.setText(noFilament);
            lblCost.setText(noFilament);
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
        Vector3D centreOfPrintedObject = ThreeDUtils.calculateCentre(project.getLoadedModels());
        configWriter.setPrintCentre((float) (centreOfPrintedObject.getX()),
                                    (float) (centreOfPrintedObject.getZ()));
        configWriter.generateConfigForSlicer(settings,
                                             temporaryDirectory
                                             + settings.getProfileName()
                                             + ApplicationConfiguration.printProfileFileExtension);

        SliceResult sliceResult = SlicerTask.doSlicing(settings.getProfileName(), settings,
                                                       temporaryDirectory,
                                                       project,
                                                       PrintQualityEnumeration.DRAFT,
                                                       null, null, steno);
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
    private String formatCost(double cost)
    {
        int numPounds = (int) cost;
        int numPence = (int) ((cost - numPounds) * 100);
        return String.format("£%s.%02d", numPounds, numPence);
    }

}
