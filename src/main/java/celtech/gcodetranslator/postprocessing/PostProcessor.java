package celtech.gcodetranslator.postprocessing;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.GCodeOutputWriter;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.PrintJobStatistics;
import celtech.gcodetranslator.RoboxiserResult;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.NodeProcessingException;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.gcodetranslator.postprocessing.nodes.ReplenishNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.SupportsPrintTimeCalculation;
import celtech.gcodetranslator.postprocessing.nodes.providers.Extrusion;
import celtech.gcodetranslator.postprocessing.nodes.providers.ExtrusionProvider;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.parboiled.Parboiled;
import static org.parboiled.errors.ErrorUtils.printParseErrors;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.ParsingResult;

/**
 *
 * @author Ian
 */
public class PostProcessor
{

    private final Stenographer steno = StenographerFactory.getStenographer(PostProcessor.class.getName());
    private final String gcodeFileToProcess;
    private final String gcodeOutputFile;
    private final HeadFile headFile;
    private final Project project;
    private final SlicerParametersFile slicerParametersFile;

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

    public PostProcessor(String gcodeFileToProcess,
            String gcodeOutputFile,
            HeadFile headFile,
            Project project,
            PostProcessorFeatureSet postProcessorFeatureSet)
    {
        this.gcodeFileToProcess = gcodeFileToProcess;
        this.gcodeOutputFile = gcodeOutputFile;
        this.headFile = headFile;
        this.project = project;
        this.featureSet = postProcessorFeatureSet;

        this.slicerParametersFile = project.getPrinterSettings().getSettings();

        nozzleProxies.clear();

        for (int nozzleIndex = 0;
                nozzleIndex < slicerParametersFile.getNozzleParameters()
                .size(); nozzleIndex++)
        {
            NozzleProxy proxy = new NozzleProxy(slicerParametersFile.getNozzleParameters().get(nozzleIndex));
            proxy.setNozzleReferenceNumber(nozzleIndex);
            nozzleProxies.add(proxy);
        }

        if (headFile.getTypeCode().equals("RBX01-DM"))
        {
            switch (project.getPrinterSettings().getPrintSupportOverride())
            {
                case NO_SUPPORT:
                case OBJECT_MATERIAL:
                    postProcessingMode = PostProcessingMode.USE_OBJECT_MATERIAL;
                    break;
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

        postProcessorUtilityMethods = new UtilityMethods();
        nodeManagementUtilities = new NodeManagementUtilities(featureSet);
        nozzleControlUtilities = new NozzleAssignmentUtilities(nozzleProxies, slicerParametersFile, headFile, featureSet, project, postProcessingMode);
        closeLogic = new CloseLogic(project, featureSet);
        nozzleUtilities = new NozzleManagementUtilities(nozzleProxies, slicerParametersFile, headFile);
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

        //Cura has line delineators like this ';LAYER:1'
        try
        {
            fileReader = new BufferedReader(new FileReader(gcodeFileToProcess));
            writer = Lookup.getPostProcessorOutputWriterFactory().create(gcodeOutputFile);

            outputUtilities.prependPrePrintHeader(writer);

            StringBuilder layerBuffer = new StringBuilder();
            LayerPostProcessResult lastLayerParseResult = new LayerPostProcessResult(Optional.empty(), null, 0, 0, 0, 0);

            for (String lineRead = fileReader.readLine(); lineRead != null; lineRead = fileReader.readLine())
            {
                lineRead = lineRead.trim();
                if (lineRead.matches(";LAYER:[0-9]+"))
                {
                    //Parse anything that has gone before
                    LayerPostProcessResult parseResult = parseLayer(layerBuffer, lastLayerParseResult, writer);
                    finalEVolume += parseResult.getEVolume();
                    finalDVolume += parseResult.getDVolume();
                    timeForPrint_secs += parseResult.getTimeForLayer();

                    //Now output the LAST layer - it was held until now in case it needed to be modified before output
                    outputUtilities.writeLayerToFile(lastLayerParseResult.getLayerData(), writer);
                    postProcessorUtilityMethods.updateLayerToLineNumber(lastLayerParseResult, layerNumberToLineNumber, writer);
                    predictedDuration += postProcessorUtilityMethods.updateLayerToPredictedDuration(lastLayerParseResult, layerNumberToPredictedDuration, writer);

                    if (parseResult.getNozzleStateAtEndOfLayer().isPresent())
                    {
                        lastLayerParseResult = parseResult;
                        if (lastLayerParseResult.getLayerData().getLayerNumber() == 1)
                        {
                            outputUtilities.outputTemperatureCommands(writer);
                        }
                    } else
                    {
                        lastLayerParseResult = new LayerPostProcessResult(lastLayerParseResult.getNozzleStateAtEndOfLayer(),
                                parseResult.getLayerData(),
                                parseResult.getEVolume(),
                                parseResult.getDVolume(),
                                parseResult.getTimeForLayer(),
                                parseResult.getLastObjectNumber().orElse(-1));
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
            LayerPostProcessResult parseResult = parseLayer(layerBuffer, lastLayerParseResult, writer);
            finalEVolume += parseResult.getEVolume();
            finalDVolume += parseResult.getDVolume();
            timeForPrint_secs += parseResult.getTimeForLayer();

            //Now output the LAST layer - it was held until now in case it needed to be modified before output
            outputUtilities.writeLayerToFile(lastLayerParseResult.getLayerData(), writer);
            postProcessorUtilityMethods.updateLayerToLineNumber(lastLayerParseResult, layerNumberToLineNumber, writer);
            predictedDuration += postProcessorUtilityMethods.updateLayerToPredictedDuration(lastLayerParseResult, layerNumberToPredictedDuration, writer);

            //Now output the final result
            outputUtilities.writeLayerToFile(parseResult.getLayerData(), writer);
            postProcessorUtilityMethods.updateLayerToLineNumber(parseResult, layerNumberToLineNumber, writer);
            predictedDuration += postProcessorUtilityMethods.updateLayerToPredictedDuration(lastLayerParseResult, layerNumberToPredictedDuration, writer);

            outputUtilities.appendPostPrintFooter(writer, finalEVolume, finalDVolume, timeForPrint_secs);

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
                    numLines,
                    finalEVolume,
                    finalDVolume,
                    0,
                    layerNumberToLineNumber,
                    layerNumberToPredictedDuration,
                    predictedDuration);

            result.setRoboxisedStatistics(roboxisedStatistics);

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

        return result;
    }

    private LayerPostProcessResult parseLayer(StringBuilder layerBuffer, LayerPostProcessResult lastLayerParseResult, GCodeOutputWriter writer)
    {
        LayerPostProcessResult parseResultAtEndOfThisLayer = null;

        // Parse the last layer if it exists...
        if (layerBuffer.length() > 0)
        {
            GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
            );
            BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.Layer());
            ParsingResult result = runner.run(layerBuffer.toString());

            if (result.hasErrors())
            {
                System.out.println("\nParse Errors:\n" + printParseErrors(result));
            } else
            {
                LayerNode layerNode = gcodeParser.getLayerNode();
                parseResultAtEndOfThisLayer = postProcess(layerNode, lastLayerParseResult);
            }
        } else
        {
            parseResultAtEndOfThisLayer = lastLayerParseResult;
        }

        return parseResultAtEndOfThisLayer;
    }

    private LayerPostProcessResult postProcess(LayerNode layerNode, LayerPostProcessResult lastLayerParseResult)
    {
        // We never want unretracts
        nodeManagementUtilities.removeUnretractNodes(layerNode);

        nodeManagementUtilities.rehomeOrphanObjects(layerNode, lastLayerParseResult);

        int lastObjectNumber = -1;

        switch (postProcessingMode)
        {
            case TASK_BASED_NOZZLE_SELECTION:
                lastObjectNumber = nozzleControlUtilities.insertNozzleControlSectionsByTask(layerNode);
                break;
            case USE_OBJECT_MATERIAL:
                lastObjectNumber = nozzleControlUtilities.insertNozzleControlSectionsByObject(layerNode);
                break;
            default:
                break;
        }

        nodeManagementUtilities.calculatePerRetractExtrusionAndNode(layerNode);

        insertOpenAndCloseNodes(layerNode, lastLayerParseResult);

        nozzleControlUtilities.assignExtrusionToCorrectExtruder(layerNode);

        postProcessorUtilityMethods.suppressUnnecessaryToolChanges(layerNode, lastLayerParseResult);

        LayerPostProcessResult postProcessResult = determineLayerPostProcessResult(layerNode);
        postProcessResult.setLastObjectNumber(lastObjectNumber);

        return postProcessResult;
    }

    protected void insertNozzleOpenFullyBeforeEvent(GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
        // Insert a replenish if required
        if (featureSet.isEnabled(PostProcessorFeature.REPLENISH_BEFORE_OPEN))
        {
            float elidedExtrusion = (float) nozzleInUse.getAndClearElidedExtrusion();

            if (elidedExtrusion > 0)
            {
                ReplenishNode replenishNode = new ReplenishNode();
                replenishNode.getExtrusion().setE(elidedExtrusion);
                replenishNode.setCommentText("Replenishing elided extrusion");
                node.addSiblingBefore(replenishNode);
            }
        }

        NozzleValvePositionNode newNozzleValvePositionNode = new NozzleValvePositionNode();
        newNozzleValvePositionNode.getNozzlePosition().setB(nozzleInUse.getNozzleParameters().getOpenPosition());
        node.addSiblingBefore(newNozzleValvePositionNode);
    }

    /**
     *
     * @param extrusionUpToClose
     * @param node
     * @param nozzleInUse
     * @return True if the close succeeded
     */
    protected boolean insertNozzleCloses(double extrusionUpToClose, GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
        boolean closeSucceeeded = false;

        //Assume the nozzle is always fully open...
        nozzleInUse.setCurrentPosition(1.0);
        if (featureSet.isEnabled(PostProcessorFeature.GRADUAL_CLOSE))
        {
            closeSucceeeded = closeLogic.insertProgressiveNozzleCloseUpToEvent(extrusionUpToClose, node, nozzleInUse);
        } else
        {
            closeSucceeeded = closeLogic.insertNozzleCloseFullyAfterEvent(node, nozzleInUse);
        }

        return closeSucceeeded;
    }

    protected void insertOpenAndCloseNodes(LayerNode layerNode, LayerPostProcessResult lastLayerParseResult)
    {
        layerNode.stream()
                .filter(node -> node instanceof ToolSelectNode)
                .forEach(node ->
                        {
                            ToolSelectNode toolSelectNode = (ToolSelectNode) node;
                            NozzleProxy nozzleInUse = nozzleProxies.get(toolSelectNode.getToolNumber());

                            try
                            {
                                if (featureSet.isEnabled(PostProcessorFeature.CLOSES_ON_RETRACT))
                                {
                                    // Find all of the retracts in this layer
                                    layerNode.stream()
                                    .filter(foundnode -> foundnode instanceof RetractNode)
                                    .forEach(foundnode ->
                                            {
                                                boolean closeSucceeded = false;
                                                RetractNode retractNode = (RetractNode) foundnode;
                                                Optional<GCodeEventNode> nextExtrusionNode = Optional.empty();
                                                try
                                                {
                                                    Optional<GCodeEventNode> priorExtrusionNode = nodeManagementUtilities.findPriorExtrusion(retractNode);
                                                    if (priorExtrusionNode.isPresent())
                                                    {
                                                        closeSucceeded = insertNozzleCloses(retractNode.getExtrusionSinceLastRetract(), priorExtrusionNode.get(), nozzleInUse);

                                                        if (closeSucceeded)
                                                        {
                                                            nextExtrusionNode = nodeManagementUtilities.findNextExtrusion(retractNode);
                                                            if (nextExtrusionNode.isPresent())
                                                            {
                                                                insertNozzleOpenFullyBeforeEvent(nextExtrusionNode.get(), nozzleInUse);
                                                            } else
                                                            {
                                                                steno.warning("No next extrusion found in layer " + layerNode.getLayerNumber() + " near node " + retractNode.toString() + " therefore not attempting to reopen nozzle");
                                                            }
                                                        }
                                                    } else
                                                    {
                                                        LayerNode lastLayer = lastLayerParseResult.getLayerData();

                                                        //Look for the last extrusion on the previous layer
                                                        if (lastLayer.getLayerNumber() < 0)
                                                        {
                                                            // There wasn't a last layer - this is a lone retract at the start of the file
                                                            steno.warning("Discarding retract from layer " + layerNode.getLayerNumber());
                                                        } else
                                                        {
                                                            Optional<GCodeEventNode> priorExtrusionNodeLastLayer = nodeManagementUtilities.findLastExtrusionEventInLayer(lastLayer);
                                                            if (!priorExtrusionNodeLastLayer.isPresent())
                                                            {
                                                                throw new NodeProcessingException("No suitable prior extrusion in previous layer", node);
                                                            }
                                                            double availableExtrusion = nodeManagementUtilities.findAvailableExtrusion(priorExtrusionNodeLastLayer.get(), false);

                                                            closeSucceeded = insertNozzleCloses(availableExtrusion, priorExtrusionNodeLastLayer.get(), nozzleInUse);

                                                            if (closeSucceeded)
                                                            {
                                                                nextExtrusionNode = nodeManagementUtilities.findNextExtrusion(retractNode);
                                                                if (nextExtrusionNode.isPresent())
                                                                {
                                                                    //Only do this if there appears to be somewhere to open...
                                                                    insertNozzleOpenFullyBeforeEvent(nextExtrusionNode.get(), nozzleInUse);
                                                                }
                                                            }
                                                        }
                                                    }
                                                } catch (NodeProcessingException ex)
                                                {
                                                    throw new RuntimeException("Failed to process retract on layer " + layerNode.getLayerNumber() + " this will affect open and close", ex);
                                                }

                                                if (closeSucceeded)
                                                {
                                                    retractNode.removeFromParent();
                                                } else
                                                {
                                                    retractNode.appendCommentText("Retract retained");
                                                    if (nextExtrusionNode.isPresent())
                                                    {
                                                        //Insert an unretract to complement the retract
                                                        UnretractNode newUnretract = new UnretractNode();
                                                        newUnretract.getExtrusion().setE(Math.abs(retractNode.getExtrusion().getE()));
                                                        newUnretract.setCommentText("Compensation for retract");
                                                        nextExtrusionNode.get().addSiblingBefore(newUnretract);
                                                    }
                                                }
                                    });
                                }

                                //Insert an open at the start if there isn't already an open preceding the first extrusion
                                GCodeEventNode firstExtrusionNode = nodeManagementUtilities.findNextExtrusion(toolSelectNode).orElseThrow(NodeProcessingException::new);

                                boolean needToAddInitialOpen = false;

                                Optional potentialOpenNode = firstExtrusionNode.getSiblingBefore();
                                if (!potentialOpenNode.isPresent())
                                {
                                    //There was no node before the first extrusion event - add one
                                    needToAddInitialOpen = true;
                                } else
                                {
                                    //There was an event - was it an open?
                                    if (potentialOpenNode.get() instanceof NozzleValvePositionNode)
                                    {
                                        NozzleValvePositionNode nozzleNode = (NozzleValvePositionNode) potentialOpenNode.get();
                                        if (nozzleNode.getNozzlePosition().getB() != nozzleInUse.getNozzleParameters().getOpenPosition())
                                        {
                                            //It wasn't an open - add one
                                            needToAddInitialOpen = true;
                                        }
                                    }
                                }

                                if (needToAddInitialOpen)
                                {
                                    insertNozzleOpenFullyBeforeEvent(firstExtrusionNode, nozzleInUse);
                                }

                                //Insert a close at the end if there isn't already a close following the last extrusion
                                GCodeEventNode lastExtrusionNode = nodeManagementUtilities.findPriorExtrusion(toolSelectNode.getChildren().get(toolSelectNode.getChildren().size() - 1)).orElseThrow(NodeProcessingException::new);

                                boolean needToAddFinalClose = false;

                                Optional potentialCloseNode = lastExtrusionNode.getSiblingAfter();
                                if (!potentialCloseNode.isPresent())
                                {
                                    //There was no node after the last extrusion event - add one
                                    needToAddFinalClose = true;
                                } else
                                {
                                    //There was an event - was it a close?
                                    if (potentialCloseNode.get() instanceof NozzleValvePositionNode)
                                    {
                                        NozzleValvePositionNode nozzleNode = (NozzleValvePositionNode) potentialCloseNode.get();
                                        if (nozzleNode.getNozzlePosition().getB() != nozzleInUse.getNozzleParameters().getClosedPosition())
                                        {
                                            //It wasn't a close - add one
                                            needToAddFinalClose = true;
                                        }
                                    }
                                }

                                if (needToAddFinalClose)
                                {
                                    double availableExtrusion = nodeManagementUtilities.findAvailableExtrusion(lastExtrusionNode, false);

                                    insertNozzleCloses(availableExtrusion, lastExtrusionNode, nozzleInUse);
                                }
                            } catch (NodeProcessingException ex)
                            {
                                throw new RuntimeException("Failed to insert opens and closes on layer " + layerNode.getLayerNumber() + " tool " + toolSelectNode.getToolNumber(), ex);
                            }
                }
                );
    }

    private LayerPostProcessResult determineLayerPostProcessResult(LayerNode layerNode)
    {
        Optional<NozzleProxy> lastNozzleInUse = nozzleUtilities.determineNozzleStateAtEndOfLayer(layerNode);

        float eValue = layerNode.stream()
                .filter(node -> node instanceof ExtrusionProvider)
                .map(ExtrusionProvider.class::cast)
                .map(ExtrusionProvider::getExtrusion)
                .map(Extrusion::getE)
                .reduce(0f, Float::sum);

        float dValue = layerNode.stream()
                .filter(node -> node instanceof ExtrusionProvider)
                .map(ExtrusionProvider.class::cast)
                .map(ExtrusionProvider::getExtrusion)
                .map(Extrusion::getD)
                .reduce(0f, Float::sum);

        List<SupportsPrintTimeCalculation> movementNodes = layerNode.stream()
                .filter(node -> node instanceof SupportsPrintTimeCalculation)
                .map(SupportsPrintTimeCalculation.class::cast)
                .collect(Collectors.toList());

        double timeForLayer = 0;
        for (int movementCounter = 0;
                movementCounter < movementNodes.size()
                - 1; movementCounter++)
        {
            double time = movementNodes.get(movementCounter).timeToReach(movementNodes.get(movementCounter + 1));
            timeForLayer += time;
        }

        return new LayerPostProcessResult(lastNozzleInUse, layerNode, eValue, dValue, timeForLayer,
                -1);
    }
}
