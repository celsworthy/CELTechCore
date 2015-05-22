package celtech.services.postProcessor;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.postprocessing.PostProcessor;
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

    private String printJobUUID;
    private String printJobDirectory;
    private SlicerParametersFile settings = null;
    private Printer printerToUse = null;
    private DoubleProperty taskProgress = new SimpleDoubleProperty(0);

    public PostProcessorTask(String printJobUUID,
        String printJobDirectory,
        SlicerParametersFile settings,
        Printer printerToUse)
    {
        initialise(printJobUUID, printJobDirectory, settings, printerToUse);
    }

    public PostProcessorTask(String printJobUUID, SlicerParametersFile settings,
        Printer printerToUse)
    {
        initialise(printJobUUID, ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator, settings,
                   printerToUse);
    }

    private void initialise(String printJobUUID,
        String printJobDirectory,
        SlicerParametersFile settings,
        Printer printerToUse)
    {
        this.printJobUUID = printJobUUID;
        this.printJobDirectory = printJobDirectory;
        this.settings = settings;
        this.printerToUse = printerToUse;
        updateTitle("Post Processor");
        updateProgress(0.0, 100.0);
    }

    @Override
    protected GCodePostProcessingResult call() throws Exception
    {
        try
        {
            updateMessage("");
            updateProgress(0.0, 100.0);
            taskProgress.addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                updateProgress(newValue.doubleValue(), 100.0);
            });            
            GCodePostProcessingResult postProcessingResult = doPostProcessing(printJobUUID, settings,
                                                          printJobDirectory, printerToUse, taskProgress);
            return postProcessingResult;
        } catch (Exception ex)
        {
            ex.printStackTrace();
            steno.error("Error in post processing");
        }
        return null;

    }

    public static GCodePostProcessingResult doPostProcessing(String printJobUUID, SlicerParametersFile settings,
        String printJobDirectory, Printer printerToUse, DoubleProperty taskProgress) throws IOException
    {
        PrintJob printJob = PrintJob.readJobFromDirectory(printJobUUID, printJobDirectory);
        String gcodeFileToProcess = printJob.getGCodeFileLocation();
        String gcodeOutputFile = printJob.getRoboxisedFileLocation();
        
        PostProcessor postProcessor = new PostProcessor(gcodeFileToProcess, gcodeOutputFile);
        postProcessor.processInput();
        
//        GCodeRoboxisingEngine roboxiser = new GCodeRoboxiser();
//        RoboxiserResult roboxiserResult = roboxiser.roboxiseFile(
//            gcodeFileToProcess, gcodeOutputFile, settings, taskProgress);
//        roboxiserResult.getPrintJobStatistics().writeToFile(printJob.getStatisticsFileLocation());
//        GCodePostProcessingResult postProcessingResult = new GCodePostProcessingResult(
//            printJobUUID, gcodeOutputFile, printerToUse, roboxiserResult);
//        
//        return postProcessingResult;
        return null;
    }

}
