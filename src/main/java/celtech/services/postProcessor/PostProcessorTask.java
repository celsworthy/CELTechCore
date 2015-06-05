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
    private final SlicerParametersFile settings;
    private final Printer printerToUse;
    private final Project project;
    private DoubleProperty taskProgress = new SimpleDoubleProperty(0);

    public PostProcessorTask(String printJobUUID,
            SlicerParametersFile settings,
            Printer printerToUse,
            Project project)
    {
        this.printJobUUID = printJobUUID;
        this.printJobDirectory = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator;
        this.settings = settings;
        this.printerToUse = printerToUse;
        this.project = project;
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
                    printJobDirectory, printerToUse, project, taskProgress);
            return postProcessingResult;
        } catch (Exception ex)
        {
            ex.printStackTrace();
            steno.error("Error in post processing");
        }
        return null;

    }

    public static GCodePostProcessingResult doPostProcessing(String printJobUUID,
            SlicerParametersFile settings,
            String printJobDirectory,
            Printer printerToUse,
            Project project,
            DoubleProperty taskProgress) throws IOException
    {
        GCodePostProcessingResult postProcessingResult = null;

        SlicerType selectedSlicer = null;
        if (settings.getSlicerOverride() != null)
        {
            selectedSlicer = settings.getSlicerOverride();
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

            List<Integer> modelExtruderAssociation = project.getLoadedModels().stream()
                    .map(ModelContainer::getAssociateWithExtruderNumberProperty)
                    .map(ReadOnlyIntegerProperty::get)
                    .collect(Collectors.toList());

            PostProcessor postProcessor = new PostProcessor(gcodeFileToProcess, gcodeOutputFile,
                    headFileToUse,
                    settings,
                    modelExtruderAssociation);

            RoboxiserResult roboxiserResult = postProcessor.processInput();
            roboxiserResult.getPrintJobStatistics().writeToFile(printJob.getStatisticsFileLocation());
            postProcessingResult = new GCodePostProcessingResult(printJobUUID, gcodeOutputFile, printerToUse, roboxiserResult);
        } else
        {
            GCodeRoboxisingEngine roboxiser = new GCodeRoboxiser();
            RoboxiserResult roboxiserResult = roboxiser.roboxiseFile(
                    gcodeFileToProcess, gcodeOutputFile, settings, taskProgress);
            roboxiserResult.getPrintJobStatistics().writeToFile(printJob.getStatisticsFileLocation());
            postProcessingResult = new GCodePostProcessingResult(printJobUUID, gcodeOutputFile, printerToUse, roboxiserResult);
        }

        return postProcessingResult;
    }

}
