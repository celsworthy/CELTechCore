package celtech.services.postProcessor;

import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.GCodeRoboxiser;
import celtech.gcodetranslator.GCodeRoboxisingEngine;
import celtech.gcodetranslator.RoboxiserResult;
import celtech.printerControl.PrintJob;
import celtech.printerControl.model.Printer;
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

    private String printJobUUID = null;
    private SlicerParametersFile settings = null;
    private Printer printerToUse = null;
    private DoubleProperty taskProgress = new SimpleDoubleProperty(0);
    
    public PostProcessorTask(String printJobUUID, SlicerParametersFile settings,
        Printer printerToUse)
    {
        this.printJobUUID = printJobUUID;
        this.settings = settings;
        this.printerToUse = printerToUse;
        updateTitle("Post Processor");
    }

    @Override
    protected GCodePostProcessingResult call() throws Exception
    {
        try {
        updateMessage("");
        updateProgress(0, 100);

        GCodeRoboxisingEngine roboxiser = new GCodeRoboxiser();
        PrintJob printJob = PrintJob.readJobFromDirectory(printJobUUID);
        String gcodeFileToProcess = printJob.getGCodeFileLocation();
        String gcodeOutputFile = printJob.getRoboxisedFileLocation();

        taskProgress.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            updateProgress(newValue.doubleValue(), 100.0);
        });

        RoboxiserResult roboxiserResult = roboxiser.roboxiseFile(
            gcodeFileToProcess, gcodeOutputFile, settings, taskProgress);
        roboxiserResult.getPrintJobStatistics().writeToFile(printJob.getStatisticsFileLocation());

        GCodePostProcessingResult postProcessingResult = new GCodePostProcessingResult(
            printJobUUID, gcodeOutputFile, printerToUse, roboxiserResult);
        return postProcessingResult;
        } catch (Exception ex) {
            ex.printStackTrace();
            steno.error("Error in post processing");
        }
        return null;

        
    }

}
