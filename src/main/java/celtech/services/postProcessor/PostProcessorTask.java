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
import celtech.printerControl.model.Printer;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.scene.shape.MeshView;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class PostProcessorTask extends Task<GCodePostProcessingResult>
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
            PostProcessorTask.class.getName());

    private final String printJobUUID;
    private String printJobDirectory;
    private final Printer printerToUse;
    private final Project project;
    private DoubleProperty taskProgress = new SimpleDoubleProperty(0);
    private final SlicerParametersFile settings;

    public PostProcessorTask(String printJobUUID,
            Printer printerToUse,
            Project project,
            SlicerParametersFile settings)
    {
        this.printJobUUID = printJobUUID;
        this.printJobDirectory = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator;
        this.printerToUse = printerToUse;
        this.project = project;
        this.settings = settings;
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
                    printJobDirectory, printerToUse, project, settings, taskProgress);
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
            SlicerParametersFile settings,
            DoubleProperty taskProgress) throws IOException
    {
        SlicerType selectedSlicer = null;
        String headType;
        if (printer != null && printer.headProperty().get() != null)
        {
            headType = printer.headProperty().get().typeCodeProperty().get();
        } else
        {
            headType = HeadContainer.defaultHeadID;
        }
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

        GCodePostProcessingResult postProcessingResult = new GCodePostProcessingResult(printJobUUID, gcodeOutputFile, printer, new RoboxiserResult());

        if (selectedSlicer == SlicerType.Cura)
        {
            PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();

            HeadFile headFileToUse = null;
            if (printer == null
                    || printer.headProperty().get() == null)
            {
                headFileToUse = HeadContainer.getHeadByID(HeadContainer.defaultHeadID);
            } else
            {
                headFileToUse = HeadContainer.getHeadByID(printer.headProperty().get().typeCodeProperty().get());
                if (!headFileToUse.getTypeCode().equals("RBX01-SL")
                        && !headFileToUse.getTypeCode().equals("RBX01-DL"))
                {
                    ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
                    ppFeatures.enableFeature(PostProcessorFeature.OPEN_AND_CLOSE_NOZZLES);
                    ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
                    ppFeatures.enableFeature(PostProcessorFeature.REPLENISH_BEFORE_OPEN);
                }
            }

            Map<Integer, Integer> objectToNozzleNumberMap = new HashMap<>();
            int objectIndex = 0;

            headFileToUse.getNozzles().get(0).getAssociatedExtruder();
            for (ModelContainer model : project.getTopLevelModels())
            {
                for (MeshView meshView : model.descendentMeshViews())
                {
                    int extruderNumber = ((ModelContainer) meshView.getParent()).getAssociateWithExtruderNumberProperty().get();
                    Optional<Integer> nozzleForExtruder = headFileToUse.getNozzleNumberForExtruderNumber(extruderNumber);
                    if (nozzleForExtruder.isPresent())
                    {
                        objectToNozzleNumberMap.put(objectIndex, nozzleForExtruder.get());
                    } else
                    {
                        steno.warning("Couldn't get extruder number for object " + objectIndex);
                    }
                    objectIndex++;
                }
            }
                        
            PostProcessor postProcessor = new PostProcessor(
                    project.getUsedExtruders(printer),
                    gcodeFileToProcess,
                    gcodeOutputFile,
                    headFileToUse,
                    settings,
                    ppFeatures,
                    headType,
                    taskProgress,
                    project.getProjectName(),
                    project.getPrinterSettings(),
                    objectToNozzleNumberMap);

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
