package celtech.gcodetranslator.postprocessing;

import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.NozzleData;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.CommentNode;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.MCodeNode;
import celtech.gcodetranslator.postprocessing.nodes.NodeProcessingException;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.gcodetranslator.postprocessing.nodes.ObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SupportInterfaceSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SupportSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.comms.commands.MacroLoadException;
import celtech.printerControl.model.Head;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.parboiled.Parboiled;
import static org.parboiled.errors.ErrorUtils.printParseErrors;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.ParsingResult;
import org.parboiled.trees.TreeUtils;

/**
 *
 * @author Ian
 */
public class PostProcessor
{

    private final Stenographer steno = StenographerFactory.getStenographer(PostProcessor.class.getName());
    private final String gcodeFileToProcess;
    private final String gcodeOutputFile;
    private final Head head;
    private final SlicerParametersFile slicerParametersFile;
    private final Project project;

    private final List<NozzleProxy> nozzleProxies = new ArrayList<>();

    private final PostProcessorFeatureSet featureSet = new PostProcessorFeatureSet();

    private final PostProcessingMode postProcessingMode = PostProcessingMode.TASK_BASED_NOZZLE_SELECTION;

    public PostProcessor(String gcodeFileToProcess,
            String gcodeOutputFile,
            Head head,
            SlicerParametersFile slicerParametersFile,
            Project project)
    {
        this.gcodeFileToProcess = gcodeFileToProcess;
        this.gcodeOutputFile = gcodeOutputFile;
        this.head = head;
        this.slicerParametersFile = slicerParametersFile;
        this.project = project;

        nozzleProxies.clear();

        for (int nozzleIndex = 0; nozzleIndex < slicerParametersFile.getNozzleParameters().size(); nozzleIndex++)
        {
            NozzleProxy proxy = new NozzleProxy(slicerParametersFile.getNozzleParameters().get(nozzleIndex));
            proxy.setNozzleReferenceNumber(nozzleIndex);
            nozzleProxies.add(proxy);
        }

        featureSet.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        featureSet.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
//        featureSet.enableFeature(PostProcessorFeature.REPLENISH_BEFORE_OPEN);
        featureSet.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        featureSet.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);
        featureSet.enableFeature(PostProcessorFeature.GRADUAL_CLOSE);
    }

    public boolean processInput()
    {
        BufferedReader fileReader = null;
        BufferedWriter writer = null;

        boolean succeeded = false;

        //Cura has line delineators like this ';LAYER:1'
        try
        {
            fileReader = new BufferedReader(new FileReader(gcodeFileToProcess));
            writer = new BufferedWriter(new FileWriter(gcodeOutputFile));

            prependPrePrintHeader(writer);

            StringBuilder layerBuffer = new StringBuilder();
            int layerCounter = 0;
            LayerParseResult lastLayerParseResult = new LayerParseResult(Optional.empty(), null);

            for (String lineRead = fileReader.readLine(); lineRead != null; lineRead = fileReader.readLine())
            {
                lineRead = lineRead.trim();
                if (lineRead.matches(";LAYER:[0-9]+"))
                {
                    //Parse anything that has gone before
                    LayerParseResult parseResult = parseLayer(layerBuffer, lastLayerParseResult, writer);

                    //Now output the LAST layer - it was held until now in case it needed to be modified before output
                    writeLayerToFile(lastLayerParseResult.getLayerData(), writer);

                    if (parseResult.getNozzleStateAtEndOfLayer().isPresent())
                    {
                        lastLayerParseResult = parseResult;
                        if (lastLayerParseResult.getLayerData().getLayerNumber() == 1)
                        {
                            outputTemperatureCommands(writer);
                        }
                    } else
                    {
                        lastLayerParseResult = new LayerParseResult(lastLayerParseResult.getNozzleStateAtEndOfLayer(), parseResult.getLayerData());
                    }

                    layerCounter++;
                    layerBuffer = new StringBuilder();
                    // Make sure this layer command is at the start
                    layerBuffer.append(lineRead);
                    layerBuffer.append('\n');
                } else
                {
                    // stash it in the buffer
                    layerBuffer.append(lineRead);
                    layerBuffer.append('\n');
                }
            }

            //This catches the last layer - if we had no data it won't do anything
            LayerParseResult parseResult = parseLayer(layerBuffer, lastLayerParseResult, writer);
            //Now output the LAST layer - it was held until now in case it needed to be modified before output
            writeLayerToFile(lastLayerParseResult.getLayerData(), writer);
            //Now output the final result
            writeLayerToFile(parseResult.getLayerData(), writer);

            appendPostPrintFooter(writer);

            succeeded = true;
        } catch (IOException ex)
        {
            steno.error("Error reading post-processor input file: " + gcodeFileToProcess);
        } catch (RuntimeException ex)
        {
            if (ex.getCause() != null)
            {
                steno.error(ex.getMessage());
            }
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

        return succeeded;
    }

    private LayerParseResult parseLayer(StringBuilder layerBuffer, LayerParseResult lastLayerParseResult, BufferedWriter writer)
    {
        LayerParseResult parseResultAtEndOfThisLayer = null;

        // Parse the last layer if it exists...
        if (layerBuffer.length() > 0)
        {
            GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
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

    protected void outputNodes(GCodeEventNode node, int level)
    {
        //Output me
        StringBuilder outputBuilder = new StringBuilder();

        for (int levelCount = 0; levelCount < level; levelCount++)
        {
            outputBuilder.append('\t');
        }
        outputBuilder.append(node.renderForOutput());
        System.out.println(outputBuilder.toString());

        //Output my children
        List<GCodeEventNode> children = node.getChildren();
        for (GCodeEventNode child : children)
        {
            level++;
            outputNodes(child, level);
            level--;
        }
    }

    private void writeLayerToFile(LayerNode layerNode, BufferedWriter writer)
    {
        if (layerNode != null)
        {
            layerNode.stream().forEach(node ->
            {
                try
                {
                    writer.write(node.renderForOutput());
                    writer.newLine();
                } catch (IOException ex)
                {
                    throw new RuntimeException("Error outputting post processed data at node " + node.renderForOutput(), ex);
                }
            });
        }
    }

    private LayerParseResult postProcess(LayerNode layerNode, LayerParseResult lastLayerParseResult)
    {
        // We never want unretracts
        removeUnretractNodes(layerNode);

        insertNozzleControlSectionsByTask(layerNode);
        insertOpenAndCloseNodes(layerNode, lastLayerParseResult);

        Optional<NozzleProxy> nozzleInUseAtEndOfLayer = determineNozzleStateAtEndOfLayer(layerNode);
        return new LayerParseResult(nozzleInUseAtEndOfLayer, layerNode);
    }

    protected void removeUnretractNodes(LayerNode layerNode)
    {
        if (featureSet.isEnabled(PostProcessorFeature.REMOVE_ALL_UNRETRACTS))
        {
            layerNode.stream().filter(node ->
            {
                return node instanceof UnretractNode;
            }).forEach(node ->
            {
                TreeUtils.removeChild(node.getParent(), node);
            });
        }
    }

    protected void insertNozzleOpenFullyBeforeEvent(GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
        // Insert a replenish if required
        if (featureSet.isEnabled(PostProcessorFeature.REPLENISH_BEFORE_OPEN))
        {
            UnretractNode newUnretractNode = new UnretractNode();
            newUnretractNode.setComment("New unretract node");
            newUnretractNode.setE(999);

            node.addSiblingBefore(newUnretractNode);
        }

        NozzleValvePositionNode newNozzleValvePositionNode = new NozzleValvePositionNode();
        newNozzleValvePositionNode.setDesiredValvePosition(nozzleInUse.getNozzleParameters().getOpenPosition());
        node.addSiblingBefore(newNozzleValvePositionNode);
    }

    protected void insertNozzleCloseFullyAfterEvent(GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
        NozzleValvePositionNode newNozzleValvePositionNode = new NozzleValvePositionNode();
        newNozzleValvePositionNode.setDesiredValvePosition(nozzleInUse.getNozzleParameters().getClosedPosition());
        node.addSiblingAfter(newNozzleValvePositionNode);
    }

    protected void insertNozzleControlSectionsByTask(LayerNode layerNode)
    {
        if (featureSet.isEnabled(PostProcessorFeature.CLOSE_ON_TASK_CHANGE))
        {
            //TODO put in first layer forced nozzle select
            //
//        if (layerNode.getLayerNumber() == 0)
//        {
//            //First layer
//            //Look for travels that exceed 2mm and close/open the nozzle as necessary
//
//            if (postProcessingMode == PostProcessingMode.TASK_BASED_NOZZLE_SELECTION)
//            {
//                // Look to see if a first layer nozzle has been selected
//                if (slicerParameters.getFirstLayerNozzle() > -1)
//                {
//                    currentNozzleProxy = nozzleProxies.get(slicerParameters.getFirstLayerNozzle());
//                } else
//                {
//                    currentNozzleProxy
//                }
//            }
//        }

            // Find all of the sections in this layer
            List<GCodeEventNode> sectionNodes = layerNode.stream()
                    .filter(node -> node instanceof SectionNode)
                    .collect(Collectors.toList());

            //We'll need at least one of these per layer
            ToolSelectNode toolSelectNode = null;

            for (GCodeEventNode sectionNodeBeingExamined : sectionNodes)
            {
                try
                {
                    NozzleProxy requiredNozzle = chooseNozzleProxyByTask(sectionNodeBeingExamined);

                    if (toolSelectNode == null
                            || toolSelectNode.getToolNumber() != requiredNozzle.getNozzleReferenceNumber())
                    {
                        //Need to create a new Tool Select Node
                        toolSelectNode = new ToolSelectNode();
                        toolSelectNode.setToolNumber(requiredNozzle.getNozzleReferenceNumber());
                        layerNode.addChildAtEnd(toolSelectNode);
                    }

                    sectionNodeBeingExamined.removeFromParent();
                    toolSelectNode.addChildAtEnd(sectionNodeBeingExamined);

                } catch (UnableToFindSectionNodeException ex)
                {
                    throw new RuntimeException("Error attempting to insert nozzle control sections by task - " + ex.getMessage(), ex);
                }
            }

            // Find all of the objects in this layer
            layerNode.stream()
                    .filter(node -> node instanceof ObjectDelineationNode)
                    .forEach(node ->
                            {
                                ObjectDelineationNode objNode = (ObjectDelineationNode) node;
                                if (!objNode.getChildren().isEmpty())
                                {
                                    throw new RuntimeException("Transfer of children from object " + objNode.getObjectNumber() + " failed");
                                }
                                objNode.removeFromParent();
                    });

        }
    }

    protected void insertNozzleControlSectionsByObject(LayerNode layerNode) throws NodeProcessingException
    {
        // Find all of the objects in this layer
        List<GCodeEventNode> objectNodes = layerNode.stream()
                .filter(node -> node instanceof ObjectDelineationNode)
                .collect(Collectors.toList());

        //We'll need at least one of these per layer
        ToolSelectNode toolSelectNode = null;

        for (GCodeEventNode objectNode : objectNodes)
        {
            ObjectDelineationNode objectNodeBeingExamined = (ObjectDelineationNode) objectNode;

            NozzleProxy requiredNozzle = chooseNozzleProxyByObject(objectNodeBeingExamined.getObjectNumber()).orElseThrow(NodeProcessingException::new);

            if (toolSelectNode == null
                    || toolSelectNode.getToolNumber() != requiredNozzle.getNozzleReferenceNumber())
            {
                //Need to create a new Tool Select Node
                toolSelectNode = new ToolSelectNode();
                toolSelectNode.setToolNumber(requiredNozzle.getNozzleReferenceNumber());
                layerNode.addChildAtEnd(toolSelectNode);
            }

            objectNodeBeingExamined.removeFromParent();
            for (GCodeEventNode sectionNode : objectNodeBeingExamined.getChildren())
            {
                sectionNode.removeFromParent();
                toolSelectNode.addChildAtEnd(sectionNode);
            }
        }
    }

    protected void insertOpenAndCloseNodes(LayerNode layerNode, LayerParseResult lastLayerParseResult)
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
                                                RetractNode retractNode = (RetractNode) foundnode;
                                                try
                                                {
                                                    Optional<GCodeEventNode> priorExtrusionNode = findPriorExtrusion(retractNode);
                                                    if (priorExtrusionNode.isPresent())
                                                    {
                                                        insertNozzleCloseFullyAfterEvent(priorExtrusionNode.get(), nozzleInUse);

                                                        Optional<GCodeEventNode> nextExtrusionNode = findNextExtrusion(retractNode);
                                                        insertNozzleOpenFullyBeforeEvent(nextExtrusionNode.orElseThrow(NodeProcessingException::new), nozzleInUse);
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
                                                            Optional<GCodeEventNode> priorExtrusionNodeLastLayer = findLastExtrusionEventInLayer(lastLayer);
                                                            insertNozzleCloseFullyAfterEvent(priorExtrusionNodeLastLayer.orElseThrow(NodeProcessingException::new), nozzleInUse);

                                                            Optional<GCodeEventNode> nextExtrusionNode = findNextExtrusion(retractNode);
                                                            insertNozzleOpenFullyBeforeEvent(nextExtrusionNode.orElseThrow(NodeProcessingException::new), nozzleInUse);
                                                        }
                                                    }
                                                } catch (NodeProcessingException ex)
                                                {
                                                    throw new RuntimeException("Failed to process retract on layer " + layerNode.getLayerNumber() + " this will affect open and close", ex);
                                                }
                                                retractNode.removeFromParent();
                                    });
                                }

                                //Insert an open at the start if there isn't already an open preceding the first extrusion
                                GCodeEventNode firstExtrusionNode = findNextExtrusion(toolSelectNode).orElseThrow(NodeProcessingException::new);

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
                                        if (nozzleNode.getDesiredValvePosition() != nozzleInUse.getNozzleParameters().getOpenPosition())
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
                                GCodeEventNode lastExtrusionNode = findPriorExtrusion(toolSelectNode.getChildren().get(toolSelectNode.getChildren().size() - 1)).orElseThrow(NodeProcessingException::new);

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
                                        if (nozzleNode.getDesiredValvePosition() != nozzleInUse.getNozzleParameters().getClosedPosition())
                                        {
                                            //It wasn't a close - add one
                                            needToAddFinalClose = true;
                                        }
                                    }
                                }

                                if (needToAddFinalClose)
                                {
                                    insertNozzleCloseFullyAfterEvent(lastExtrusionNode, nozzleInUse);
                                }
                            } catch (NodeProcessingException ex)
                            {
                                throw new RuntimeException("Failed to insert opens and closes on layer " + layerNode.getLayerNumber() + " tool " + toolSelectNode.getToolNumber(), ex);
                            }
                }
                );
    }

    private NozzleProxy chooseNozzleProxyByTask(final GCodeEventNode node) throws UnableToFindSectionNodeException
    {
        NozzleProxy nozzleProxy = null;

        //Go up through the parents until we either reach the top or find a section node
        GCodeEventNode foundNode = null;
        GCodeEventNode searchNode = node;

        do
        {
            if (searchNode instanceof SectionNode)
            {
                foundNode = searchNode;
                break;
            } else
            {
                if (searchNode.hasParent())
                {
                    searchNode = searchNode.getParent();
                }
            }
        } while (searchNode.hasParent());

        if (foundNode == null)
        {
            throw new UnableToFindSectionNodeException("Unable to find section parent of " + node.renderForOutput());
        }

        if (foundNode instanceof FillSectionNode)
        {
            nozzleProxy = nozzleProxies.get(slicerParametersFile.getFillNozzle());
        } else if (foundNode instanceof OuterPerimeterSectionNode)
        {
            nozzleProxy = nozzleProxies.get(slicerParametersFile.getPerimeterNozzle());
        } else if (foundNode instanceof InnerPerimeterSectionNode)
        {
            nozzleProxy = nozzleProxies.get(slicerParametersFile.getPerimeterNozzle());
        } else if (foundNode instanceof SupportSectionNode)
        {
            nozzleProxy = nozzleProxies.get(slicerParametersFile.getSupportNozzle());
        } else if (foundNode instanceof SupportInterfaceSectionNode)
        {
            nozzleProxy = nozzleProxies.get(slicerParametersFile.getSupportInterfaceNozzle());
        } else
        {
            nozzleProxy = nozzleProxies.get(slicerParametersFile.getFillNozzle());
        }
        return nozzleProxy;
    }

    private Optional<NozzleProxy> chooseNozzleProxyByObject(final int objectNumber)
    {
        Optional<NozzleProxy> nozzleProxy = Optional.empty();

        int extruderNumber = project.getLoadedModels().get(objectNumber).getAssociateWithExtruderNumberProperty().get();
        String extruderLetter = "";

        switch (extruderNumber)
        {
            case 0:
                extruderLetter = "E";
                break;
            case 1:
                extruderLetter = "D";
                break;
        }

        HeadFile headDataFile = HeadContainer.getHeadByID(head.typeCodeProperty().get());
        for (int nozzleIndex = 0; nozzleIndex < headDataFile.getNozzles().size(); nozzleIndex++)
        {
            NozzleData nozzleData = headDataFile.getNozzles().get(nozzleIndex);
            if (nozzleData.getAssociatedExtruder().equals(extruderLetter))
            {
                nozzleProxy = Optional.of(nozzleProxies.get(nozzleIndex));
                break;
            }
        }

        return nozzleProxy;
    }

    private Optional<GCodeEventNode> findNextExtrusion(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<GCodeEventNode> nextExtrusion = node.streamFromHere()
                .filter(filteredNode -> filteredNode instanceof ExtrusionNode)
                .findFirst();

        return nextExtrusion;
    }

    private Optional<GCodeEventNode> findPriorExtrusion(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<GCodeEventNode> nextExtrusion = node.streamBackwardsFromHere()
                .filter(filteredNode -> filteredNode instanceof ExtrusionNode)
                .findFirst();

        return nextExtrusion;
    }

    private Optional<GCodeEventNode> findLastExtrusionEventInLayer(LayerNode layerNode)
    {
        Optional<GCodeEventNode> lastExtrusionNode = Optional.empty();

        List<GCodeEventNode> toolSelectNodes = layerNode
                .stream()
                .filter(node -> node instanceof ToolSelectNode)
                .collect(Collectors.toList());

        if (!toolSelectNodes.isEmpty())
        {
            ToolSelectNode lastToolSelect = (ToolSelectNode) toolSelectNodes.get(toolSelectNodes.size() - 1);

            List<GCodeEventNode> extrusionNodes = lastToolSelect
                    .stream()
                    .filter(node -> node instanceof ExtrusionNode)
                    .collect(Collectors.toList());

            if (!extrusionNodes.isEmpty())
            {
                lastExtrusionNode = Optional.of(extrusionNodes.get(extrusionNodes.size() - 1));
            }
        }

        return lastExtrusionNode;
    }

    private Optional<NozzleProxy> determineNozzleStateAtEndOfLayer(LayerNode layerNode)
    {
        Optional<NozzleProxy> nozzleInUse = Optional.empty();

        List<GCodeEventNode> toolSelectNodes = layerNode
                .stream()
                .filter(node -> node instanceof ToolSelectNode)
                .collect(Collectors.toList());

        if (!toolSelectNodes.isEmpty())
        {
            ToolSelectNode lastToolSelect = (ToolSelectNode) toolSelectNodes.get(toolSelectNodes.size() - 1);

            Optional<GCodeEventNode> lastNozzleValePositionNode = lastToolSelect.streamChildrenAndMeBackwards().filter(node -> node instanceof NozzleValvePositionNode).findFirst();

            if (lastNozzleValePositionNode.isPresent())
            {
                NozzleValvePositionNode nozzleNode = (NozzleValvePositionNode) lastNozzleValePositionNode.get();
                NozzleProxy proxy = nozzleProxies.get(lastToolSelect.getToolNumber());
                proxy.setCurrentPosition(nozzleNode.getDesiredValvePosition());
                nozzleInUse = Optional.of(proxy);
            }
        }

        return nozzleInUse;
    }

    private void prependPrePrintHeader(BufferedWriter writer)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE d MMM y HH:mm:ss", Locale.UK);
        try
        {
            writer.write("; File post-processed by the CEL Tech Roboxiser on "
                    + formatter.format(new Date()) + "\n");
            writer.write("; " + ApplicationConfiguration.getTitleAndVersion() + "\n");

            writer.write(";\n; Pre print gcode\n");

            for (String macroLine : GCodeMacros.getMacroContents("before_print"))
            {
                writer.write(macroLine);
                writer.newLine();
            }

            writer.write("; End of Pre print gcode\n");
        } catch (IOException | MacroLoadException ex)
        {
            throw new RuntimeException("Failed to add pre-print header in post processor - " + ex.getMessage(), ex);
        }
    }

    private void appendPostPrintFooter(BufferedWriter writer)
    {
        try
        {
            writer.write(";\n; Post print gcode\n");
            for (String macroLine : GCodeMacros.getMacroContents("after_print"))
            {
                writer.write(macroLine);
                writer.newLine();
            }
            writer.write("; End of Post print gcode\n");
        } catch (IOException | MacroLoadException ex)
        {
            throw new RuntimeException("Failed to add post-print footer in post processor - " + ex.getMessage(), ex);
        }
    }

    private void outputTemperatureCommands(BufferedWriter writer)
    {
        try
        {
            MCodeNode nozzleTemp = new MCodeNode(104);
            nozzleTemp.setComment("Go to nozzle temperature from loaded reel - don't wait");
            writer.write(nozzleTemp.renderForOutput());
            writer.newLine();

            MCodeNode bedTemp = new MCodeNode(140);
            bedTemp.setComment("Go to bed temperature from loaded reel - don't wait");
            writer.write(bedTemp.renderForOutput());
            writer.newLine();
        } catch (IOException ex)
        {
            throw new RuntimeException("Failed to add post layer 1 temperature commands in post processor - " + ex.getMessage(), ex);
        }
    }
}
