package celtech.services.postProcessor;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.SlicerType;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.SlicerParametersFile.HeadType;
import celtech.gcodetranslator.GCodeRoboxiser;
import celtech.gcodetranslator.GCodeRoboxisingEngine;
import celtech.gcodetranslator.RoboxiserResult;
import celtech.gcodetranslator.postprocessing.PostProcessor;
import celtech.gcodetranslator.postprocessing.PostProcessorFeature;
import celtech.gcodetranslator.postprocessing.PostProcessorFeatureSet;
import celtech.printerControl.PrintJob;
import celtech.printerControl.model.Printer;
import java.io.File;
import java.io.IOException;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class PostProcessorTask extends Task<GCodePostProcessingResult>
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            PostProcessorTask.class.getName());

    private final String printJobUUID;
    private String printJobDirectory;
    private final Printer printerToUse;
    private final Project project;
    private DoubleProperty taskProgress = new SimpleDoubleProperty(0);

    public PostProcessorTask(String printJobUUID,
            Printer printerToUse,
            Project project)
    {
        this.printJobUUID = printJobUUID;
        this.printJobDirectory = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator;
        this.printerToUse = printerToUse;
        this.project = project;
        updateTitle("Post Processor");
        updateProgress(0.0, 100.0);
    }

    @Override
    protected GCodePostProcessingResult call() throws Exception
    {
        GCodePostProcessingResult postProcessingResult = null;
        
        try
        {
            updateMessage("");
            updateProgress(0.0, 100.0);
            taskProgress.addListener(
                    (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
                    {
                        updateProgress(newValue.doubleValue(), 100.0);
                    });
            postProcessingResult = doPostProcessing(printJobUUID,
                    printJobDirectory, printerToUse, project, taskProgress);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            steno.error("Error in post processing");
        }
            return postProcessingResult;
    }

    public static GCodePostProcessingResult doPostProcessing(String printJobUUID,
            String printJobDirectory,
            Printer printer,
            Project project,
            DoubleProperty taskProgress) throws IOException
    {
        SlicerType selectedSlicer = null;
        HeadType headType;
        if (printer != null && printer.headProperty().get() != null) {
            headType = printer.headProperty().get().headTypeProperty().get();
        } else {
            headType = HeadContainer.defaultHeadType;
        }
        if (project.getPrinterSettings().getSettings(headType).getSlicerOverride() != null)
        {
            selectedSlicer = project.getPrinterSettings().getSettings(headType).getSlicerOverride();
        } else
        {
            selectedSlicer = Lookup.getUserPreferences().getSlicerType();
        }

        PrintJob printJob = PrintJob.readJobFromDirectory(printJobUUID, printJobDirectory);
        String gcodeFileToProcess = printJob.getGCodeFileLocation();
        String gcodeOutputFile = printJob.getRoboxisedFileLocation();

        GCodePostProcessingResult postProcessingResult = new GCodePostProcessingResult(printJobUUID, gcodeOutputFile, printer, new RoboxiserResult());

        if (selectedSlicer == SlicerType.Cura)
        {
            HeadFile headFileToUse = null;
            if (printer == null
                    || printer.headProperty().get() == null)
            {
                headFileToUse = HeadContainer.getHeadByID(HeadContainer.defaultHeadID);
            } else
            {
                headFileToUse = HeadContainer.getHeadByID(printer.headProperty().get().typeCodeProperty().get());
            }

            PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
            ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
            ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
            ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
            ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);
            ppFeatures.enableFeature(PostProcessorFeature.GRADUAL_CLOSE);
            ppFeatures.enableFeature(PostProcessorFeature.REPLENISH_BEFORE_OPEN);

            PostProcessor postProcessor = new PostProcessor(
                    gcodeFileToProcess,
                    gcodeOutputFile,
                    headFileToUse,
                    project,
                    ppFeatures,
                    headType);

            RoboxiserResult roboxiserResult = postProcessor.processInput();
            if (roboxiserResult.isSuccess())
            {
                roboxiserResult.getPrintJobStatistics().writeToFile(printJob.getStatisticsFileLocation());
                postProcessingResult = new GCodePostProcessingResult(printJobUUID, gcodeOutputFile, printer, roboxiserResult);
            }
        } else
        {
            GCodeRoboxisingEngine roboxiser = new GCodeRoboxiser();
            RoboxiserResult roboxiserResult = roboxiser.roboxiseFile(
                    gcodeFileToProcess, gcodeOutputFile, project.getPrinterSettings().getSettings(headType), taskProgress);
            roboxiserResult.getPrintJobStatistics().writeToFile(printJob.getStatisticsFileLocation());
            postProcessingResult = new GCodePostProcessingResult(printJobUUID, gcodeOutputFile, printer, roboxiserResult);
        }

        return postProcessingResult;
    }

}
