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
import celtech.utils.threed.ThreeDUtils;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.control.Label;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.io.FileUtils;
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
    
    private final static Random random = new Random();

    private final Project project;
    private final Label lblTime;
    private final Label lblWeight;
    private final Label lblCost;
    private final SlicerParametersFile settings;

    private SlicerTask slicerTask;
    private PostProcessorTask postProcessorTask;
    private final Runnable whenComplete;

    private final String temporaryDirectory;

    private File printJobDirectory;

    public GetTimeWeightCost(Project project, SlicerParametersFile settings,
        Label lblTime, Label lblWeight, Label lblCost, Runnable whenComplete)
    {
        this.project = project;
        this.lblTime = lblTime;
        this.lblWeight = lblWeight;
        this.lblCost = lblCost;
        this.settings = settings;
        this.whenComplete = whenComplete;

        this.temporaryDirectory = ApplicationConfiguration.getApplicationStorageDirectory()
            + ApplicationConfiguration.timeAndCostFileSubpath
            + settings.getProfileName() + random.nextInt()
            + File.separator;
    }

    public SlicerTask setupAndRunSlicerTask()
    {

        steno.debug("launch time cost process for project " + project + " and settings "
            + settings.getProfileName());

        slicerTask = makeSlicerTask(project, settings);

        slicerTask.setOnFailed((WorkerStateEvent event) ->
        {
            clearPrintJobDirectory();
        });

        slicerTask.setOnCancelled((WorkerStateEvent event) ->
        {
            clearPrintJobDirectory();
            if (postProcessorTask != null && postProcessorTask.isRunning())
            {
                postProcessorTask.cancel();
            }
            lblTime.setText("Cancelled");
            lblWeight.setText("Cancelled");
            lblCost.setText("Cancelled");
        });

        slicerTask.setOnSucceeded((WorkerStateEvent event) ->
        {
            try
            {
                SliceResult sliceResult = slicerTask.getValue();

                postProcessorTask = new PostProcessorTask(
                    sliceResult.getPrintJobUUID(),
                    temporaryDirectory,
                    settings, null);

                postProcessorTask.setOnSucceeded((WorkerStateEvent event1) ->
                {
                    try
                    {
                        GCodePostProcessingResult result = postProcessorTask.getValue();
                        PrintJobStatistics printJobStatistics = result.getRoboxiserResult().
                            getPrintJobStatistics();

                        updateFieldsForStatistics(printJobStatistics);
                        clearPrintJobDirectory();

                        if (whenComplete != null)
                        {
                            whenComplete.run();
                        }

                    } catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                });

                postProcessorTask.setOnFailed((WorkerStateEvent event1) ->
                {
                    clearPrintJobDirectory();
                    lblTime.setText("NA");
                    lblWeight.setText("NA");
                    lblCost.setText("NA");
                });

                steno.info("launch post processor for project " + project + " and settings "
                    + settings.getProfileName());
                Lookup.getTaskExecutor().runTaskAsDaemon(postProcessorTask);
            } catch (Exception ex)
            {
                ex.printStackTrace();
            }
        });

        Lookup.getTaskExecutor().runTaskAsDaemon(slicerTask);
        return slicerTask;
    }

    public void clearPrintJobDirectory()
    {
        try
        {
            FileUtils.deleteDirectory(new File(temporaryDirectory));
        } catch (IOException ex)
        {
            steno.error("Could not delete directory " + temporaryDirectory+ " "
                + ex);
        }
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
            lblWeight.setText("No filament");
            lblCost.setText("No filament");
        }
    }

    /**
     * Set up a print job directory etc and return a SlicerTask based on it.
     */
    private SlicerTask makeSlicerTask(Project project, SlicerParametersFile settings)
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
        configWriter.setPrintCentre((float) (centreOfPrintedObject.getX()
            + ApplicationConfiguration.xPrintOffset),
                                    (float) (centreOfPrintedObject.getZ()
                                    + ApplicationConfiguration.yPrintOffset));
        configWriter.generateConfigForSlicer(settings,
                                             temporaryDirectory
                                             + settings.getProfileName()
                                             + ApplicationConfiguration.printProfileFileExtension);

        return new SlicerTask(settings.getProfileName(),
                              temporaryDirectory,
                              project,
                              PrintQualityEnumeration.DRAFT,
                              settings, null);

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
