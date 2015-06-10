package celtech.services.postProcessor;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.SlicerType;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.GCodeRoboxiser;
import celtech.gcodetranslator.GCodeRoboxisingEngine;
import celtech.gcodetranslator.RoboxiserResult;
import celtech.gcodetranslator.postprocessing.PostProcessor;
import celtech.gcodetranslator.postprocessing.PostProcessorFeature;
import celtech.gcodetranslator.postprocessing.PostProcessorFeatureSet;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.PrintJob;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
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
            Printer printerToUse,
            Project project,
            DoubleProperty taskProgress) throws IOException
    {
        GCodePostProcessingResult postProcessingResult = null;

        SlicerType selectedSlicer = null;
        if (project.getPrinterSettings().getSettings().getSlicerOverride() != null)
        {
            selectedSlicer = project.getPrinterSettings().getSettings().getSlicerOverride();
        } else
        {
            selectedSlicer = Lookup.getUserPreferences().getSlicerType();
        }

        PrintJob printJob = PrintJob.readJobFromDirectory(printJobUUID, printJobDirectory);
        String gcodeFileToProcess = printJob.getGCodeFileLocation();
        String gcodeOutputFile = printJob.getRoboxisedFileLocation();

        if (selectedSlicer == SlicerType.Cura)
        {
            HeadFile headFileToUse = null;
            if (printerToUse == null
                    || printerToUse.headProperty().get() == null)
            {
                headFileToUse = HeadContainer.getHeadByID(HeadContainer.defaultHeadID);
            } else
            {
                headFileToUse = HeadContainer.getHeadByID(printerToUse.headProperty().get().typeCodeProperty().get());
            }

            PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
            ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
            ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
            ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
            ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);
            ppFeatures.enableFeature(PostProcessorFeature.GRADUAL_CLOSE);

            PostProcessor postProcessor = new PostProcessor(
                    gcodeFileToProcess,
                    gcodeOutputFile,
                    headFileToUse,
                    project,
                    ppFeatures);

            RoboxiserResult roboxiserResult = postProcessor.processInput();
            if (roboxiserResult.isSuccess())
            {
                roboxiserResult.getPrintJobStatistics().writeToFile(printJob.getStatisticsFileLocation());
                postProcessingResult = new GCodePostProcessingResult(printJobUUID, gcodeOutputFile, printerToUse, roboxiserResult);
            }
        } else
        {
            GCodeRoboxisingEngine roboxiser = new GCodeRoboxiser();
            RoboxiserResult roboxiserResult = roboxiser.roboxiseFile(
                    gcodeFileToProcess, gcodeOutputFile, project.getPrinterSettings().getSettings(), taskProgress);
            roboxiserResult.getPrintJobStatistics().writeToFile(printJob.getStatisticsFileLocation());
            postProcessingResult = new GCodePostProcessingResult(printJobUUID, gcodeOutputFile, printerToUse, roboxiserResult);
        }

        return postProcessingResult;
    }

}
