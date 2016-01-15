package celtech.gcodetranslator.postprocessing;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.GCodeOutputWriter;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.PrintJobStatistics;
import celtech.gcodetranslator.RoboxiserResult;
import celtech.gcodetranslator.postprocessing.nodes.GCodeDirectiveNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.DurationCalculationException;
import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.SupportsPrintTimeCalculation;
import celtech.gcodetranslator.postprocessing.nodes.providers.ExtrusionProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.FeedrateProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Head.HeadType;
import celtech.printerControl.model.Printer;
import celtech.utils.SystemUtils;
import celtech.utils.Time.TimeUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.DoubleProperty;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.ParsingResult;

/**
 *
 * @author Ian
 */
public class PostProcessor
{

    private final Stenographer steno = StenographerFactory.getStenographer(PostProcessor.class.getName());

    private final String unretractTimerName = "Unretract";
    private final String orphanTimerName = "Orphans";
    private final String nozzleControlTimerName = "NozzleControl";
    private final String perRetractTimerName = "PerRetract";
    private final String closeTimerName = "Close";
    private final String unnecessaryToolchangeTimerName = "UnnecessaryToolchange";
    private final String cameraEventTimerName = "CameraEvent";
    private final String openTimerName = "Open";
    private final String assignExtrusionTimerName = "AssignExtrusion";
    private final String layerResultTimerName = "LayerResult";
    private final String heaterSaverTimerName = "HeaterSaver";
    private final String parseLayerTimerName = "ParseLayer";
    private final String writeOutputTimerName = "WriteOutput";
    private final String countLinesTimerName = "CountLines";

    private final Printer printer;
    private final String gcodeFileToProcess;
    private final String gcodeOutputFile;
    private final HeadFile headFile;
    private final Project project;
    private final SlicerParametersFile slicerParametersFile;
    private final DoubleProperty taskProgress;

    private final List<NozzleProxy> nozzleProxies = new ArrayList<>();

    private final PostProcessorFeatureSet featureSet;

    private PostProcessingMode postProcessingMode = PostProcessingMode.TASK_BASED_NOZZLE_SELECTION;

    protected List<Integer> layerNumberToLineNumber;
    protected List<Double> layerNumberToPredictedDuration;
    protected double predictedDuration = 0;

    private final UtilityMethods postProcessorUtilityMethods;
    private final NodeManagementUtilities nodeManagementUtilities;
    private final NozzleAssignmentUtilities nozzleControlUtilities;
    private final CloseLogic closeLogic;
    private final NozzleManagementUtilities nozzleUtilities;
    private final UtilityMethods utilities;

    private final TimeUtils timeUtils = new TimeUtils();

    public PostProcessor(Printer printer,
            String gcodeFileToProcess,
            String gcodeOutputFile,
            HeadFile headFile,
            Project project,
            SlicerParametersFile settings,
            PostProcessorFeatureSet postProcessorFeatureSet,
            String headType,
            DoubleProperty taskProgress)
    {
        this.printer = printer;
        this.gcodeFileToProcess = gcodeFileToProcess;
        this.gcodeOutputFile = gcodeOutputFile;
        this.headFile = headFile;
        this.project = project;
        this.featureSet = postProcessorFeatureSet;

        this.slicerParametersFile = settings;

        this.taskProgress = taskProgress;

        nozzleProxies.clear();

        for (int nozzleIndex = 0;
                nozzleIndex < slicerParametersFile.getNozzleParameters()
                .size(); nozzleIndex++)
        {
            NozzleProxy proxy = new NozzleProxy(slicerParametersFile.getNozzleParameters().get(nozzleIndex));
            proxy.setNozzleReferenceNumber(nozzleIndex);
            nozzleProxies.add(proxy);
        }

        if (headFile.getType() == HeadType.DUAL_MATERIAL_HEAD)
        {
            switch (project.getPrinterSettings().getPrintSupportTypeOverride())
            {
                case MATERIAL_1:
                    postProcessingMode = PostProcessingMode.SUPPORT_IN_FIRST_MATERIAL;
                    break;
                case MATERIAL_2:
                    postProcessingMode = PostProcessingMode.SUPPORT_IN_SECOND_MATERIAL;
                    break;
            }
        } else
        {
            postProcessingMode = PostProcessingMode.TASK_BASED_NOZZLE_SELECTION;
        }

        postProcessorUtilityMethods = new UtilityMethods(featureSet, project, settings, headType);
        nodeManagementUtilities = new NodeManagementUtilities(featureSet);
        nozzleControlUtilities = new NozzleAssignmentUtilities(nozzleProxies, slicerParametersFile, headFile, featureSet, project, postProcessingMode);
        closeLogic = new CloseLogic(project, slicerParametersFile, featureSet, headType);
        nozzleUtilities = new NozzleManagementUtilities(nozzleProxies, slicerParametersFile, headFile);
        utilities = new UtilityMethods(featureSet, project, settings, headType);
    }

    public RoboxiserResult processInput()
    {
        RoboxiserResult result = new RoboxiserResult();

        BufferedReader fileReader = null;
        GCodeOutputWriter writer = null;

        float finalEVolume = 0;
        float finalDVolume = 0;
        double timeForPrint_secs = 0;

        layerNumberToLineNumber = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();

        predictedDuration = 0;

        int layerCounter = -1;

        OutputUtilities outputUtilities = new OutputUtilities();

        timeUtils.timerStart(this, "PostProcessor");
        steno.info("Beginning post-processing operation");

        //Cura has line delineators like this ';LAYER:1'
        try
        {
            File inputFile = new File(gcodeFileToProcess);
            timeUtils.timerStart(this, countLinesTimerName);
            int linesInGCodeFile = SystemUtils.countLinesInFile(inputFile);
            timeUtils.timerStop(this, countLinesTimerName);

            int linesRead = 0;
            double lastPercentSoFar = 0;

            fileReader = new BufferedReader(new FileReader(inputFile));

            writer = Lookup.getPostProcessorOutputWriterFactory().create(gcodeOutputFile);

            boolean nozzle0HeatRequired = false;
            boolean nozzle1HeatRequired = false;

            boolean eRequired = false;
            boolean dRequired = false;

            int defaultObjectNumber = 0;

            if (headFile.getType() == Head.HeadType.DUAL_MATERIAL_HEAD)
            {
                nozzle0HeatRequired = project.getUsedExtruders(printer).contains(1)
                        || postProcessingMode == PostProcessingMode.SUPPORT_IN_SECOND_MATERIAL;
                eRequired = project.getUsedExtruders(printer).contains(0);
                nozzle1HeatRequired = project.getUsedExtruders(printer).contains(0)
                        || postProcessingMode == PostProcessingMode.SUPPORT_IN_FIRST_MATERIAL;
                dRequired = project.getUsedExtruders(printer).contains(1);
            } else
            {
                nozzle0HeatRequired = false;
                nozzle1HeatRequired = false;
                eRequired = true;
            }

            outputUtilities.prependPrePrintHeader(writer, headFile.getTypeCode(),
                    nozzle0HeatRequired,
                    nozzle1HeatRequired);

            StringBuilder layerBuffer = new StringBuilder();

            LayerPostProcessResult parseResultCycle1 = new LayerPostProcessResult(null, 0, 0, 0, defaultObjectNumber, null, null, null, 0, -1);
            LayerPostProcessResult parseResultCycle2 = null;
            OpenResult lastOpenResult = null;

            for (String lineRead = fileReader.readLine(); lineRead != null; lineRead = fileReader.readLine())
            {
                linesRead++;
                double percentSoFar = ((double) linesRead / (double) linesInGCodeFile) * 100;
                if (percentSoFar - lastPercentSoFar >= 1)
                {
                    if (taskProgress != null)
                    {
                        taskProgress.set(percentSoFar);
                    }
                    lastPercentSoFar = percentSoFar;
                }

                lineRead = lineRead.trim();
                if (lineRead.matches(";LAYER:[-]*[0-9]+"))
                {
                    if (layerCounter >= 0)
                    {
                        if (parseResultCycle2 != null
                                && parseResultCycle2.getLayerData() != null)
                        {
                            timeUtils.timerStart(this, assignExtrusionTimerName);
                            NozzleAssignmentUtilities.ExtrusionAssignmentResult assignmentResult = nozzleControlUtilities.assignExtrusionToCorrectExtruder(parseResultCycle2.getLayerData());
                            timeUtils.timerStop(this, assignExtrusionTimerName);

                            //Now output the layer before the LAST layer - it was held until now in case it needed to be modified before output
                            //Add the opens first - we leave it until now as the layer we have just processed may have affected the one before
                            //NOTE
                            //Since we're using the open/close state here we need to make sure this is the last open/close thing we do...
                            //NOTE
                            if (featureSet.isEnabled(PostProcessorFeature.OPEN_AND_CLOSE_NOZZLES))
                            {
                                timeUtils.timerStart(this, openTimerName);
                                lastOpenResult = postProcessorUtilityMethods.insertOpens(parseResultCycle2.getLayerData(), lastOpenResult, nozzleProxies, headFile.getTypeCode());
                                timeUtils.timerStop(this, openTimerName);
                            }

                            timeUtils.timerStart(this, writeOutputTimerName);
                            outputUtilities.writeLayerToFile(parseResultCycle2.getLayerData(), writer);
                            timeUtils.timerStop(this, writeOutputTimerName);
                            postProcessorUtilityMethods.updateLayerToLineNumber(parseResultCycle2, layerNumberToLineNumber, writer);
                            predictedDuration += postProcessorUtilityMethods.updateLayerToPredictedDuration(parseResultCycle2, layerNumberToPredictedDuration, writer);

                            if (parseResultCycle2.getLayerData().getLayerNumber() == 0)
                            {
                                outputUtilities.outputTemperatureCommands(writer, nozzle0HeatRequired, nozzle1HeatRequired, eRequired, dRequired);
                            }

                            finalEVolume += assignmentResult.getEVolume();
                            finalDVolume += assignmentResult.getDVolume();
                            timeForPrint_secs += parseResultCycle2.getTimeForLayer();
                        }
                        parseResultCycle2 = parseResultCycle1;

                        //Parse anything that has gone before
                        LayerPostProcessResult parseResultCycle0 = parseLayer(layerBuffer, parseResultCycle1, writer, headFile.getType());

                        parseResultCycle1 = parseResultCycle0;
                        parseResultCycle0 = null;
                    }

                    layerCounter++;
                    layerBuffer = new StringBuilder();
                    // Make sure this layer command is at the start
                    layerBuffer.append(lineRead);
                    layerBuffer.append('\n');
                } else if (!lineRead.equals(""))
                {
                    //Ignore blank lines
                    // stash it in the buffer
                    layerBuffer.append(lineRead);
                    layerBuffer.append('\n');
                }
            }

            //This catches the last layer - if we had no data it won't do anything
            LayerPostProcessResult parseResult = parseLayer(layerBuffer, parseResultCycle1, writer, headFile.getType());

            finalEVolume += parseResult.getEVolume();
            finalDVolume += parseResult.getDVolume();
            timeForPrint_secs += parseResult.getTimeForLayer();

            if (parseResultCycle2 != null
                    && parseResultCycle2.getLayerData() != null)
            {
                timeUtils.timerStart(this, assignExtrusionTimerName);
                NozzleAssignmentUtilities.ExtrusionAssignmentResult assignmentResult = nozzleControlUtilities.assignExtrusionToCorrectExtruder(parseResultCycle2.getLayerData());
                timeUtils.timerStop(this, assignExtrusionTimerName);

                //Now output the layer before the LAST layer - it was held until now in case it needed to be modified before output
                //Add the opens first - we leave it until now as the layer we have just processed may have affected the one before
                //NOTE
                //Since we're using the open/close state here we need to make sure this is the last open/close thing we do...
                //NOTE
                if (featureSet.isEnabled(PostProcessorFeature.OPEN_AND_CLOSE_NOZZLES))
                {
                    timeUtils.timerStart(this, openTimerName);
                    lastOpenResult = postProcessorUtilityMethods.insertOpens(parseResultCycle2.getLayerData(), lastOpenResult, nozzleProxies, headFile.getTypeCode());
                    timeUtils.timerStop(this, openTimerName);
                }

                timeUtils.timerStart(this, writeOutputTimerName);
                outputUtilities.writeLayerToFile(parseResultCycle2.getLayerData(), writer);
                timeUtils.timerStop(this, writeOutputTimerName);
                postProcessorUtilityMethods.updateLayerToLineNumber(parseResultCycle2, layerNumberToLineNumber, writer);
                predictedDuration += postProcessorUtilityMethods.updateLayerToPredictedDuration(parseResultCycle2, layerNumberToPredictedDuration, writer);

                finalEVolume += assignmentResult.getEVolume();
                finalDVolume += assignmentResult.getDVolume();
                timeForPrint_secs += parseResultCycle2.getTimeForLayer();
            }

            if (parseResultCycle1 != null
                    && parseResultCycle1.getLayerData() != null)
            {
                timeUtils.timerStart(this, assignExtrusionTimerName);
                NozzleAssignmentUtilities.ExtrusionAssignmentResult assignmentResult = nozzleControlUtilities.assignExtrusionToCorrectExtruder(parseResultCycle1.getLayerData());
                timeUtils.timerStop(this, assignExtrusionTimerName);

                //Now output the layer before the LAST layer - it was held until now in case it needed to be modified before output
                //Add the opens first - we leave it until now as the layer we have just processed may have affected the one before
                //NOTE
                //Since we're using the open/close state here we need to make sure this is the last open/close thing we do...
                //NOTE
                if (featureSet.isEnabled(PostProcessorFeature.OPEN_AND_CLOSE_NOZZLES))
                {
                    timeUtils.timerStart(this, openTimerName);
                    lastOpenResult = postProcessorUtilityMethods.insertOpens(parseResultCycle1.getLayerData(), lastOpenResult, nozzleProxies, headFile.getTypeCode());
                    timeUtils.timerStop(this, openTimerName);
                }

                timeUtils.timerStart(this, writeOutputTimerName);
                outputUtilities.writeLayerToFile(parseResultCycle1.getLayerData(), writer);
                timeUtils.timerStop(this, writeOutputTimerName);
                postProcessorUtilityMethods.updateLayerToLineNumber(parseResultCycle1, layerNumberToLineNumber, writer);
                predictedDuration += postProcessorUtilityMethods.updateLayerToPredictedDuration(parseResultCycle1, layerNumberToPredictedDuration, writer);

                finalEVolume += assignmentResult.getEVolume();
                finalDVolume += assignmentResult.getDVolume();
                timeForPrint_secs += parseResultCycle1.getTimeForLayer();
            }

            //Now output the final result
            timeUtils.timerStart(this, assignExtrusionTimerName);
            NozzleAssignmentUtilities.ExtrusionAssignmentResult assignmentResult = nozzleControlUtilities.assignExtrusionToCorrectExtruder(parseResult.getLayerData());
            timeUtils.timerStop(this, assignExtrusionTimerName);

            //Add the opens first - we leave it until now as the layer we have just processed may have affected the one before
            //NOTE
            //Since we're using the open/close state here we need to make sure this is the last open/close thing we do...
            //NOTE
            if (featureSet.isEnabled(PostProcessorFeature.OPEN_AND_CLOSE_NOZZLES))
            {
                timeUtils.timerStart(this, openTimerName);
                lastOpenResult = postProcessorUtilityMethods.insertOpens(parseResult.getLayerData(), lastOpenResult, nozzleProxies, headFile.getTypeCode());
                timeUtils.timerStop(this, openTimerName);
            }

            timeUtils.timerStart(this, writeOutputTimerName);
            outputUtilities.writeLayerToFile(parseResult.getLayerData(), writer);
            timeUtils.timerStop(this, writeOutputTimerName);
            postProcessorUtilityMethods.updateLayerToLineNumber(parseResult, layerNumberToLineNumber, writer);
            predictedDuration += postProcessorUtilityMethods.updateLayerToPredictedDuration(parseResultCycle1, layerNumberToPredictedDuration, writer);

            finalEVolume += assignmentResult.getEVolume();
            finalDVolume += assignmentResult.getDVolume();
            timeForPrint_secs += parseResult.getTimeForLayer();

            timeUtils.timerStart(this, writeOutputTimerName);
            outputUtilities.appendPostPrintFooter(writer, finalEVolume, finalDVolume, timeForPrint_secs,
                    headFile.getTypeCode(),
                    nozzle0HeatRequired,
                    nozzle1HeatRequired);
            timeUtils.timerStop(this, writeOutputTimerName);

            /**
             * TODO: layerNumberToLineNumber uses lines numbers from the GCode
             * file so are a little less than the line numbers for each layer
             * after roboxisation. As a quick fix for now set the line number of
             * the last layer to the actual maximum line number.
             */
            layerNumberToLineNumber.set(layerNumberToLineNumber.size() - 1,
                    writer.getNumberOfLinesOutput());
            int numLines = writer.getNumberOfLinesOutput();

            PrintJobStatistics roboxisedStatistics = new PrintJobStatistics(
                    project.getProjectName(),
                    project.getPrinterSettings().getSettings(headFile.getTypeCode()).getProfileName(),
                    project.getPrinterSettings().getSettings(headFile.getTypeCode()).getLayerHeight_mm(),
                    numLines,
                    finalEVolume,
                    finalDVolume,
                    0,
                    layerNumberToLineNumber,
                    layerNumberToPredictedDuration,
                    predictedDuration);

            result.setRoboxisedStatistics(roboxisedStatistics);

            outputPostProcessingTimerReport();

            timeUtils.timerStop(this, "PostProcessor");
            steno.info("Post-processing took " + timeUtils.timeTimeSoFar_ms(this, "PostProcessor") + "ms");

            result.setSuccess(true);
        } catch (IOException ex)
        {
            steno.error("Error reading post-processor input file: " + gcodeFileToProcess);
        } catch (RuntimeException ex)
        {
            if (ex.getCause() != null)
            {
                steno.error("Fatal postprocessing error on layer " + layerCounter + " got exception: " + ex.getCause().getMessage());
            } else
            {
                steno.error("Fatal postprocessing error on layer " + layerCounter);
            }
            ex.printStackTrace();
        } finally
        {
            if (fileReader != null)
            {
                try
                {
                    fileReader.close();
                } catch (IOException ex)
                {
                    steno.error("Failed to close post processor input file - " + gcodeFileToProcess);
                }
            }

            if (writer != null)
            {
                try
                {
                    writer.close();
                } catch (IOException ex)
                {
                    steno.error("Failed to close post processor output file - " + gcodeOutputFile);
                }
            }
        }
        steno.info("About to exit post processor with result " + result.isSuccess());

        return result;
    }

    private LayerPostProcessResult parseLayer(StringBuilder layerBuffer, LayerPostProcessResult lastLayerParseResult, GCodeOutputWriter writer, HeadType headType)
    {
        LayerPostProcessResult parseResultAtEndOfThisLayer = null;

        // Parse the last layer if it exists...
        if (layerBuffer.length() > 0)
        {
            CuraGCodeParser gcodeParser = Parboiled.createParser(CuraGCodeParser.class
            );

            if (lastLayerParseResult
                    != null)
            {
                gcodeParser.setFeedrateInForce(lastLayerParseResult.getLastFeedrateInForce());
            }

            BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.Layer());

            timeUtils.timerStart(this, parseLayerTimerName);
            ParsingResult result = runner.run(layerBuffer.toString());

            timeUtils.timerStop(this, parseLayerTimerName);

            if (result.hasErrors()
                    || !result.matched)
            {
                throw new RuntimeException("Parsing failure");
            } else
            {
                LayerNode layerNode = gcodeParser.getLayerNode();
                int lastFeedrate = gcodeParser.getFeedrateInForce();
                parseResultAtEndOfThisLayer = postProcess(layerNode, lastLayerParseResult, headType);
                parseResultAtEndOfThisLayer.setLastFeedrateInForce(lastFeedrate);
            }
        } else
        {
            parseResultAtEndOfThisLayer = lastLayerParseResult;
        }

        return parseResultAtEndOfThisLayer;
    }

    private LayerPostProcessResult postProcess(LayerNode layerNode, LayerPostProcessResult lastLayerParseResult, HeadType headType)
    {
        // We never want unretracts
        timeUtils.timerStart(this, unretractTimerName);
        nodeManagementUtilities.removeUnretractNodes(layerNode);
        timeUtils.timerStop(this, unretractTimerName);

        timeUtils.timerStart(this, orphanTimerName);
        nodeManagementUtilities.rehomeOrphanObjects(layerNode, lastLayerParseResult);
        timeUtils.timerStop(this, orphanTimerName);

        int lastObjectNumber = -1;

        timeUtils.timerStart(this, nozzleControlTimerName);
        lastObjectNumber = nozzleControlUtilities.insertNozzleControlSectionsByObject(layerNode, lastLayerParseResult);
        timeUtils.timerStop(this, nozzleControlTimerName);

        nodeManagementUtilities.recalculateSectionExtrusion(layerNode);

        timeUtils.timerStart(this, perRetractTimerName);
        nodeManagementUtilities.calculatePerRetractExtrusionAndNode(layerNode);
        timeUtils.timerStop(this, perRetractTimerName);

        timeUtils.timerStart(this, closeTimerName);
        closeLogic.insertCloseNodes(layerNode, lastLayerParseResult, nozzleProxies);
        timeUtils.timerStop(this, closeTimerName);

        timeUtils.timerStart(this, unnecessaryToolchangeTimerName);
        postProcessorUtilityMethods.suppressUnnecessaryToolChangesAndInsertToolchangeCloses(layerNode, lastLayerParseResult, nozzleProxies);
        timeUtils.timerStop(this, unnecessaryToolchangeTimerName);

        timeUtils.timerStart(this, cameraEventTimerName);
        postProcessorUtilityMethods.insertCameraTriggersAndCloses(layerNode, lastLayerParseResult, nozzleProxies);
        timeUtils.timerStop(this, cameraEventTimerName);

        timeUtils.timerStart(this, layerResultTimerName);
        LayerPostProcessResult postProcessResult = determineLayerPostProcessResult(layerNode, lastLayerParseResult);
        postProcessResult.setLastObjectNumber(lastObjectNumber);
        timeUtils.timerStop(this, layerResultTimerName);

//        if (printer.headProperty().get().headTypeProperty().get() == HeadType.DUAL_MATERIAL_HEAD)
//        {
//            timeUtils.timerStart(this, heaterSaverTimerName);
//            postProcessorUtilityMethods.heaterSave(layerNode, lastLayerParseResult);
//            timeUtils.timerStop(this, heaterSaverTimerName);
//        }
        
        return postProcessResult;
    }

    private LayerPostProcessResult determineLayerPostProcessResult(LayerNode layerNode, LayerPostProcessResult lastLayerPostProcessResult)
    {
        Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);

        float eValue = 0;
        float dValue = 0;
        double timeForLayer = 0;
        double timeInThisTool = 0;
        int lastFeedrate = -1;

        SupportsPrintTimeCalculation lastMovementProvider = null;
        SectionNode lastSectionNode = null;
        ToolSelectNode lastToolSelectNode = null;
        ToolSelectNode firstToolSelectNodeWithSameNumber = null;
        double cumulativeTimeInLastTool = 0;

        while (layerIterator.hasNext())
        {
            GCodeEventNode foundNode = layerIterator.next();

            if (foundNode instanceof ExtrusionProvider)
            {
                ExtrusionProvider extrusionProvider = (ExtrusionProvider) foundNode;
                eValue += extrusionProvider.getExtrusion().getE();
                dValue += extrusionProvider.getExtrusion().getD();
            }

            if (foundNode instanceof SupportsPrintTimeCalculation)
            {
                SupportsPrintTimeCalculation timeCalculationNode = (SupportsPrintTimeCalculation) foundNode;

                if (lastMovementProvider != null)
                {
                    try
                    {
                        double time = lastMovementProvider.timeToReach((MovementProvider) foundNode);
                        timeForLayer += time;
                        timeInThisTool += time;
                    } catch (DurationCalculationException ex)
                    {
                        if (ex.getFromNode() instanceof Renderable
                                && ex.getToNode() instanceof Renderable)
                        {
                            steno.error("Unable to calculate duration correctly for nodes source:"
                                    + ((Renderable) ex.getFromNode()).renderForOutput()
                                    + " destination:"
                                    + ((Renderable) ex.getToNode()).renderForOutput());
                        } else
                        {
                            steno.error("Unable to calculate duration correctly for nodes source:"
                                    + ex.getFromNode().getMovement().renderForOutput()
                                    + " destination:"
                                    + ex.getToNode().getMovement().renderForOutput());
                        }

                        throw new RuntimeException("Unable to calculate duration correctly on layer "
                                + layerNode.getLayerNumber(), ex);
                    }
                }
                lastMovementProvider = timeCalculationNode;
                if (((FeedrateProvider) lastMovementProvider).getFeedrate().getFeedRate_mmPerMin() < 0)
                {
                    ((FeedrateProvider) lastMovementProvider).getFeedrate().setFeedRate_mmPerMin(lastLayerPostProcessResult.getLastFeedrateInForce());
                }
                lastFeedrate = ((FeedrateProvider) lastMovementProvider).getFeedrate().getFeedRate_mmPerMin();
            }

            if (foundNode instanceof ToolSelectNode)
            {
                ToolSelectNode newToolSelectNode = (ToolSelectNode) foundNode;

                if (lastToolSelectNode != null)
                {
                    lastToolSelectNode.setEstimatedDuration(timeInThisTool);

                    if (newToolSelectNode.getToolNumber() != lastToolSelectNode.getToolNumber())
                    {
                        cumulativeTimeInLastTool = 0;
                        firstToolSelectNodeWithSameNumber = newToolSelectNode;
                    }

                    cumulativeTimeInLastTool += timeInThisTool;
                } else
                {
                    firstToolSelectNodeWithSameNumber = newToolSelectNode;
                }

                lastToolSelectNode = newToolSelectNode;
                timeInThisTool = 0;
            } else if (foundNode instanceof SectionNode)
            {
                lastSectionNode = (SectionNode) foundNode;
            } else if (foundNode instanceof GCodeDirectiveNode
                    && ((GCodeDirectiveNode) foundNode).getGValue() == 4)
            {
                GCodeDirectiveNode directive = (GCodeDirectiveNode) foundNode;
                if (directive.getGValue() == 4)
                {
                    //Found a dwell
                    Optional<Integer> sValue = directive.getSValue();
                    if (sValue.isPresent())
                    {
                        //Seconds
                        timeForLayer += sValue.get();
                    }
                    Optional<Integer> pValue = directive.getPValue();
                    if (pValue.isPresent())
                    {
                        //Microseconds
                        timeForLayer += pValue.get() / 1000.0;
                    }
                }
            }
        }

        if (lastSectionNode == null)
        {
            lastSectionNode = lastLayerPostProcessResult.getLastSectionNodeInForce();
        }

        if (lastToolSelectNode == null)
        {
            lastToolSelectNode = lastLayerPostProcessResult.getLastToolSelectInForce();
            cumulativeTimeInLastTool = lastLayerPostProcessResult.getTimeUsingLastTool();
        } else
        {
            lastToolSelectNode.setEstimatedDuration(timeInThisTool);
            cumulativeTimeInLastTool += timeInThisTool;
        }

        return new LayerPostProcessResult(layerNode, eValue, dValue, timeForLayer, -1,
                lastSectionNode, lastToolSelectNode, firstToolSelectNodeWithSameNumber, cumulativeTimeInLastTool, lastFeedrate);
    }

    private void outputPostProcessingTimerReport()
    {
        steno.debug("Post Processor Timer Report");
        steno.debug("============");
        steno.debug(unretractTimerName + " " + timeUtils.timeTimeSoFar_ms(this, unretractTimerName));
        steno.debug(orphanTimerName + " " + timeUtils.timeTimeSoFar_ms(this, orphanTimerName));
        steno.debug(nozzleControlTimerName + " " + timeUtils.timeTimeSoFar_ms(this, nozzleControlTimerName));
        steno.debug(perRetractTimerName + " " + timeUtils.timeTimeSoFar_ms(this, perRetractTimerName));
        steno.debug(unnecessaryToolchangeTimerName + " " + timeUtils.timeTimeSoFar_ms(this, unnecessaryToolchangeTimerName));
        steno.debug(cameraEventTimerName + " " + timeUtils.timeTimeSoFar_ms(this, cameraEventTimerName));
        if (featureSet.isEnabled(PostProcessorFeature.OPEN_AND_CLOSE_NOZZLES))
        {
            steno.debug(closeTimerName + " " + timeUtils.timeTimeSoFar_ms(this, closeTimerName));
            steno.debug(openTimerName + " " + timeUtils.timeTimeSoFar_ms(this, openTimerName));
        }
        steno.debug(assignExtrusionTimerName + " " + timeUtils.timeTimeSoFar_ms(this, assignExtrusionTimerName));
        steno.debug(layerResultTimerName + " " + timeUtils.timeTimeSoFar_ms(this, layerResultTimerName));
        steno.debug(parseLayerTimerName + " " + timeUtils.timeTimeSoFar_ms(this, parseLayerTimerName));
//        steno.info(heaterSaverTimerName + " " + timeUtils.timeTimeSoFar_ms(this, heaterSaverTimerName));
        steno.debug(writeOutputTimerName + " " + timeUtils.timeTimeSoFar_ms(this, writeOutputTimerName));
        steno.debug("============");
    }
}
