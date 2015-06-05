package celtech.gcodetranslator.postprocessing;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.NozzleData;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.GCodeOutputWriter;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.PrintJobStatistics;
import celtech.gcodetranslator.RoboxiserResult;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.MCodeNode;
import celtech.gcodetranslator.postprocessing.nodes.MovementNode;
import celtech.gcodetranslator.postprocessing.nodes.NodeProcessingException;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.gcodetranslator.postprocessing.nodes.ObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OrphanObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OrphanSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SupportInterfaceSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SupportSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.comms.commands.MacroLoadException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
    private final HeadFile headFile;
    private final SlicerParametersFile slicerParametersFile;
    private final List<Integer> modelExtruderAssociation;

    private final List<NozzleProxy> nozzleProxies = new ArrayList<>();

    private final PostProcessorFeatureSet featureSet = new PostProcessorFeatureSet();

    private PostProcessingMode postProcessingMode = PostProcessingMode.TASK_BASED_NOZZLE_SELECTION;

    protected List<Integer> layerNumberToLineNumber;
    protected List<Double> layerNumberToPredictedDuration;

    public PostProcessor(String gcodeFileToProcess,
            String gcodeOutputFile,
            HeadFile headFile,
            SlicerParametersFile slicerParametersFile,
            List<Integer> modelExtruderAssociation)
    {
        this.gcodeFileToProcess = gcodeFileToProcess;
        this.gcodeOutputFile = gcodeOutputFile;
        this.headFile = headFile;
        this.slicerParametersFile = slicerParametersFile;
        this.modelExtruderAssociation = modelExtruderAssociation;

        if (headFile.getTypeCode().equals("RBX01-DM"))
        {
            postProcessingMode = PostProcessingMode.USE_OBJECT_MATERIAL;
        }

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

        //Cura has line delineators like this ';LAYER:1'
        try
        {
            fileReader = new BufferedReader(new FileReader(gcodeFileToProcess));
            writer = Lookup.getPostProcessorOutputWriterFactory().create(gcodeOutputFile);

            prependPrePrintHeader(writer);

            StringBuilder layerBuffer = new StringBuilder();
            int layerCounter = 0;
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
                    writeLayerToFile(lastLayerParseResult.getLayerData(), writer);
                    updateLayerToLineNumber(lastLayerParseResult, writer);
                    updateLayerToPredictedDuration(lastLayerParseResult, writer);

                    if (parseResult.getNozzleStateAtEndOfLayer().isPresent())
                    {
                        lastLayerParseResult = parseResult;
                        if (lastLayerParseResult.getLayerData().getLayerNumber() == 1)
                        {
                            outputTemperatureCommands(writer);
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
            writeLayerToFile(lastLayerParseResult.getLayerData(), writer);
            updateLayerToLineNumber(lastLayerParseResult, writer);
            updateLayerToPredictedDuration(lastLayerParseResult, writer);

            //Now output the final result
            writeLayerToFile(parseResult.getLayerData(), writer);
            updateLayerToLineNumber(parseResult, writer);
            updateLayerToPredictedDuration(lastLayerParseResult, writer);

            appendPostPrintFooter(writer, finalEVolume, finalDVolume, timeForPrint_secs);

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
                    layerNumberToPredictedDuration);

            result.setRoboxisedStatistics(roboxisedStatistics);

            result.setSuccess(true);
        } catch (IOException ex)
        {
            steno.error("Error reading post-processor input file: " + gcodeFileToProcess);
        } catch (RuntimeException ex)
        {
            if (ex.getCause() != null)
            {
                steno.error(ex.getCause().getMessage());
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

    private void updateLayerToLineNumber(LayerPostProcessResult lastLayerParseResult, GCodeOutputWriter writer)
    {
        if (lastLayerParseResult.getLayerData() != null)
        {
            int layerNumber = lastLayerParseResult.getLayerData().getLayerNumber();
            if (layerNumber >= 0)
            {
                layerNumberToLineNumber.add(layerNumber, writer.getNumberOfLinesOutput());
            }
        }
    }

    private void updateLayerToPredictedDuration(LayerPostProcessResult lastLayerParseResult, GCodeOutputWriter writer)
    {
        if (lastLayerParseResult.getLayerData() != null)
        {
            int layerNumber = lastLayerParseResult.getLayerData().getLayerNumber();
            if (layerNumber >= 0)
            {
                layerNumberToPredictedDuration.add(layerNumber, lastLayerParseResult.getTimeForLayer());
            }
        }
    }

    private LayerPostProcessResult parseLayer(StringBuilder layerBuffer, LayerPostProcessResult lastLayerParseResult, GCodeOutputWriter writer)
    {
        LayerPostProcessResult parseResultAtEndOfThisLayer = null;

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

    private void writeLayerToFile(LayerNode layerNode, GCodeOutputWriter writer)
    {
        if (layerNode != null)
        {
            layerNode.stream().forEach(node ->
            {
                try
                {
                    writer.writeOutput(node.renderForOutput());
                    writer.newLine();
                } catch (IOException ex)
                {
                    throw new RuntimeException("Error outputting post processed data at node " + node.renderForOutput(), ex);
                }
            });
        }
    }

    private LayerPostProcessResult postProcess(LayerNode layerNode, LayerPostProcessResult lastLayerParseResult)
    {
        // We never want unretracts
        removeUnretractNodes(layerNode);

        rehomeOrphanObjects(layerNode, lastLayerParseResult);

        int lastObjectNumber = -1;

        switch (postProcessingMode)
        {
            case TASK_BASED_NOZZLE_SELECTION:
                lastObjectNumber = insertNozzleControlSectionsByTask(layerNode);
                break;
            case USE_OBJECT_MATERIAL:
                lastObjectNumber = insertNozzleControlSectionsByObject(layerNode);
                break;
            default:
                break;
        }

        insertOpenAndCloseNodes(layerNode, lastLayerParseResult);

        LayerPostProcessResult postProcessResult = determineLayerPostProcessResult(layerNode);
        postProcessResult.setLastObjectNumber(lastObjectNumber);

        return postProcessResult;
    }

    protected void removeUnretractNodes(LayerNode layerNode)
    {
        if (featureSet.isEnabled(PostProcessorFeature.REMOVE_ALL_UNRETRACTS))
        {
            layerNode.stream()
                    .filter(node -> node instanceof UnretractNode)
                    .forEach(node ->
                            {
                                TreeUtils.removeChild(node.getParent(), node);
                    });
        }
    }

    protected void rehomeOrphanObjects(LayerNode layerNode, final LayerPostProcessResult lastLayerParseResult)
    {
        // Orphans occur when there is no Tn directive in a layer
        //
        // At the start of the file we should treat this as object 0
        // Subsequently we should look at the last layer to see which object was in force and create an object with the same reference

        layerNode.stream()
                .filter(node -> node instanceof OrphanObjectDelineationNode)
                .map(OrphanObjectDelineationNode.class::cast)
                .forEach(orphanNode ->
                        {
                            ObjectDelineationNode newObjectNode = new ObjectDelineationNode();

                            int potentialObjectNumber = orphanNode.getPotentialObjectNumber();

                            if (potentialObjectNumber < 0)
                            {
                                if (layerNode.getLayerNumber() == 0)
                                {
                                    // Has to be 0 if we're on the first layer
                                    potentialObjectNumber = 0;
                                } else if (lastLayerParseResult.getLastObjectNumber().isPresent())
                                {
                                    potentialObjectNumber = lastLayerParseResult.getLastObjectNumber().get();
                                } else
                                {
                                    throw new RuntimeException("Cannot determine object number for orphan on layer " + layerNode.getLayerNumber());
                                }
                            }

                            newObjectNode.setObjectNumber(potentialObjectNumber);

                            //Transfer the children from the orphan to the new node
                            List<GCodeEventNode> children = orphanNode.getChildren().stream().collect(Collectors.toList());
                            for (GCodeEventNode childNode : children)
                            {
                                childNode.removeFromParent();
                                newObjectNode.addChildAtEnd(childNode);
                            }

                            //Add the new node
                            orphanNode.addSiblingBefore(newObjectNode);

                            //Remove the orphan
                            orphanNode.removeFromParent();
                });
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

    /**
     *
     * @param layerNode
     * @return The reference number of the last object in the layer
     */
    protected int insertNozzleControlSectionsByTask(LayerNode layerNode)
    {
        int lastObjectReferenceNumber = -1;

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
            //We'll need at least one of these per layer
            ToolSelectNode toolSelectNode = null;

            // Find all of the objects in this layer
            List<ObjectDelineationNode> objectNodes = layerNode.stream()
                    .filter(node -> node instanceof ObjectDelineationNode)
                    .map(ObjectDelineationNode.class::cast)
                    .collect(Collectors.toList());

            if (objectNodes.size() > 0)
            {
                lastObjectReferenceNumber = objectNodes.get(objectNodes.size() - 1).getObjectNumber();
            }

            SectionNode lastSectionNode = null;

            for (ObjectDelineationNode objectNode : objectNodes)
            {
                List<GCodeEventNode> childNodes = objectNode.getChildren().stream().collect(Collectors.toList());

                for (GCodeEventNode childNode : childNodes)
                {
                    if (childNode instanceof SectionNode)
                    {
                        SectionNode sectionNodeBeingExamined = (SectionNode) childNode;

                        NozzleProxy requiredNozzle = null;

                        try
                        {
                            if (sectionNodeBeingExamined instanceof OrphanSectionNode)
                            {
                                if (lastSectionNode == null)
                                {
                                    throw new RuntimeException("Failed to process orphan section on layer " + layerNode.getLayerNumber() + " as last section didn't exist");
                                }

                                requiredNozzle = chooseNozzleProxyByTask(lastSectionNode);
                                try
                                {
                                    SectionNode replacementSection = lastSectionNode.getClass().newInstance();

                                    // Move the child nodes to the replacement section
                                    List<GCodeEventNode> sectionChildren = sectionNodeBeingExamined.stream().collect(Collectors.toList());
                                    for (GCodeEventNode child : sectionChildren)
                                    {
                                        child.removeFromParent();
                                        replacementSection.addChildAtEnd(child);
                                    }

                                    sectionNodeBeingExamined.removeFromParent();
                                    lastSectionNode.addSiblingAfter(replacementSection);
                                    sectionNodeBeingExamined = replacementSection;
                                } catch (InstantiationException | IllegalAccessException ex)
                                {
                                    throw new RuntimeException("Failed to process orphan section on layer " + layerNode.getLayerNumber(), ex);
                                }
                            } else
                            {
                                requiredNozzle = chooseNozzleProxyByTask(sectionNodeBeingExamined);
                            }

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

                        lastSectionNode = sectionNodeBeingExamined;
                    } else
                    {
                        //Probably a travel node - move it over without changing it
                        childNode.removeFromParent();
                        toolSelectNode.addChildAtEnd(childNode);
                    }
                }

                if (!objectNode.getChildren().isEmpty())
                {
                    throw new RuntimeException("Transfer of children from object " + objectNode.getObjectNumber() + " failed");
                }
                objectNode.removeFromParent();
            }
        }

        return lastObjectReferenceNumber;
    }

    protected int insertNozzleControlSectionsByObject(LayerNode layerNode)
    {
        int lastObjectReferenceNumber = -1;

        // Find all of the objects in this layer
        List<ObjectDelineationNode> objectNodes = layerNode.stream()
                .filter(node -> node instanceof ObjectDelineationNode)
                .map(ObjectDelineationNode.class::cast)
                .collect(Collectors.toList());

        if (objectNodes.size() > 0)
        {
            lastObjectReferenceNumber = objectNodes.get(objectNodes.size() - 1).getObjectNumber();
        }

        //We'll need at least one of these per layer
        ToolSelectNode toolSelectNode = null;

        try
        {
            SectionNode lastSectionNode = null;

            for (GCodeEventNode objectNode : objectNodes)
            {
                ObjectDelineationNode objectNodeBeingExamined = (ObjectDelineationNode) objectNode;

                int extruderNumber = modelExtruderAssociation.get(objectNodeBeingExamined.getObjectNumber());
                NozzleProxy requiredNozzle = chooseNozzleProxyByExtruderNumber(extruderNumber).orElseThrow(NodeProcessingException::new);

                if (toolSelectNode == null
                        || toolSelectNode.getToolNumber() != requiredNozzle.getNozzleReferenceNumber())
                {
                    //Need to create a new Tool Select Node
                    toolSelectNode = new ToolSelectNode();
                    toolSelectNode.setToolNumber(requiredNozzle.getNozzleReferenceNumber());
                    layerNode.addChildAtEnd(toolSelectNode);
                }

                objectNodeBeingExamined.removeFromParent();
                List<GCodeEventNode> sectionNodes = objectNodeBeingExamined.getChildren().stream().collect(Collectors.toList());

                for (GCodeEventNode childNode : sectionNodes)
                {
                    if (childNode instanceof SectionNode)
                    {
                        SectionNode sectionNodeUnderExamination = (SectionNode) childNode;

                        if (sectionNodeUnderExamination instanceof OrphanSectionNode)
                        {
                            if (lastSectionNode == null)
                            {
                                throw new RuntimeException("Failed to process orphan section on layer " + layerNode.getLayerNumber() + " as last section didn't exist");
                            }

                            try
                            {
                                requiredNozzle = chooseNozzleProxyByTask(lastSectionNode);

                                SectionNode replacementSection = lastSectionNode.getClass().newInstance();

                                // Move the child nodes to the replacement section
                                List<GCodeEventNode> sectionChildren = sectionNodes.stream().collect(Collectors.toList());
                                for (GCodeEventNode child : sectionChildren)
                                {
                                    child.removeFromParent();
                                    replacementSection.addChildAtEnd(child);
                                }

                                sectionNodeUnderExamination.removeFromParent();
                                lastSectionNode.addSiblingAfter(replacementSection);
                                sectionNodeUnderExamination = replacementSection;
                            } catch (InstantiationException | IllegalAccessException | UnableToFindSectionNodeException ex)
                            {
                                throw new RuntimeException("Failed to process orphan section on layer " + layerNode.getLayerNumber(), ex);
                            }
                        }

                        sectionNodeUnderExamination.removeFromParent();
                        toolSelectNode.addChildAtEnd(sectionNodeUnderExamination);

                        if (extruderNumber == 1)
                        {
                            toolSelectNode.stream()
                                    .filter(node -> node instanceof ExtrusionNode)
                                    .map(ExtrusionNode.class::cast)
                                    .forEach(extrusionNode ->
                                            {
                                                extrusionNode.extrudeUsingEOnly();
                                    });
                        } else
                        {
                            toolSelectNode.stream()
                                    .filter(node -> node instanceof ExtrusionNode)
                                    .map(ExtrusionNode.class::cast)
                                    .forEach(extrusionNode ->
                                            {
                                                extrusionNode.extrudeUsingDOnly();
                                    });
                        }

                        lastSectionNode = sectionNodeUnderExamination;
                    } else
                    {
                        //Probably a travel node - move it over without changing it
                        childNode.removeFromParent();
                        toolSelectNode.addChildAtEnd(childNode);
                    }
                }
            }
        } catch (NodeProcessingException ex)
        {
            throw new RuntimeException("Failure during tool select by object - layer " + layerNode.getLayerNumber(), ex);
        }

        return lastObjectReferenceNumber;
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
                                                RetractNode retractNode = (RetractNode) foundnode;
                                                try
                                                {
                                                    Optional<GCodeEventNode> priorExtrusionNode = findPriorExtrusion(retractNode);
                                                    if (priorExtrusionNode.isPresent())
                                                    {
                                                        insertNozzleCloseFullyAfterEvent(priorExtrusionNode.get(), nozzleInUse);

                                                        Optional<GCodeEventNode> nextExtrusionNode = findNextExtrusion(retractNode);
                                                        if (nextExtrusionNode.isPresent())
                                                        {
                                                            insertNozzleOpenFullyBeforeEvent(nextExtrusionNode.get(), nozzleInUse);
                                                        } else
                                                        {
                                                            steno.warning("No next extrusion found in layer " + layerNode.getLayerNumber() + " near node " + retractNode.renderForOutput() + " therefore not attempting to reopen nozzle");
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
                                                            Optional<GCodeEventNode> priorExtrusionNodeLastLayer = findLastExtrusionEventInLayer(lastLayer);
                                                            insertNozzleCloseFullyAfterEvent(priorExtrusionNodeLastLayer.orElseThrow(NodeProcessingException::new), nozzleInUse);

                                                            Optional<GCodeEventNode> nextExtrusionNode = findNextExtrusion(retractNode);
                                                            if (nextExtrusionNode.isPresent())
                                                            {
                                                                //Only do this if there appears to be somewhere to open...
                                                                insertNozzleOpenFullyBeforeEvent(nextExtrusionNode.get(), nozzleInUse);
                                                            }
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

    private Optional<NozzleProxy> chooseNozzleProxyByExtruderNumber(final int extruderNumber)
    {
        Optional<NozzleProxy> nozzleProxy = Optional.empty();

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

        for (int nozzleIndex = 0; nozzleIndex < headFile.getNozzles().size(); nozzleIndex++)
        {
            NozzleData nozzleData = headFile.getNozzles().get(nozzleIndex);
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

    private void prependPrePrintHeader(GCodeOutputWriter writer)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE d MMM y HH:mm:ss", Locale.UK);
        try
        {
            writer.writeOutput("; File post-processed by the CEL Tech Roboxiser on "
                    + formatter.format(new Date()) + "\n");
            writer.writeOutput("; " + ApplicationConfiguration.getTitleAndVersion() + "\n");

            writer.writeOutput(";\n; Pre print gcode\n");

            for (String macroLine : GCodeMacros.getMacroContents("before_print"))
            {
                writer.writeOutput(macroLine);
                writer.newLine();
            }

            writer.writeOutput("; End of Pre print gcode\n");
        } catch (IOException | MacroLoadException ex)
        {
            throw new RuntimeException("Failed to add pre-print header in post processor - " + ex.getMessage(), ex);
        }
    }

    private void appendPostPrintFooter(GCodeOutputWriter writer, final float totalEVolume, final float totalDVolume, final double totalTimeInSecs)
    {
        try
        {
            writer.writeOutput(";\n; Post print gcode\n");
            for (String macroLine : GCodeMacros.getMacroContents("after_print"))
            {
                writer.writeOutput(macroLine);
                writer.newLine();
            }
            writer.writeOutput("; End of Post print gcode\n");
            writer.writeOutput(";\n");
            writer.writeOutput("; Volume of material - Extruder E - " + totalEVolume + "\n");
            writer.writeOutput("; Volume of material - Extruder D - " + totalDVolume + "\n");
            writer.writeOutput("; Total print time estimate - " + totalTimeInSecs + " seconds\n");
            writer.writeOutput(";\n");
        } catch (IOException | MacroLoadException ex)
        {
            throw new RuntimeException("Failed to add post-print footer in post processor - " + ex.getMessage(), ex);
        }
    }

    private void outputTemperatureCommands(GCodeOutputWriter writer)
    {
        try
        {
            MCodeNode nozzleTemp = new MCodeNode(104);
            nozzleTemp.setComment("Go to nozzle temperature from loaded reel - don't wait");
            writer.writeOutput(nozzleTemp.renderForOutput());
            writer.newLine();

            MCodeNode bedTemp = new MCodeNode(140);
            bedTemp.setComment("Go to bed temperature from loaded reel - don't wait");
            writer.writeOutput(bedTemp.renderForOutput());
            writer.newLine();
        } catch (IOException ex)
        {
            throw new RuntimeException("Failed to add post layer 1 temperature commands in post processor - " + ex.getMessage(), ex);
        }
    }

    private LayerPostProcessResult determineLayerPostProcessResult(LayerNode layerNode)
    {
        Optional<NozzleProxy> lastNozzleInUse = determineNozzleStateAtEndOfLayer(layerNode);

        float eValue = layerNode.stream()
                .filter(node -> node instanceof ExtrusionNode)
                .map(ExtrusionNode.class::cast)
                .map(ExtrusionNode::getE)
                .reduce(0f, Float::sum);

        float dValue = layerNode.stream()
                .filter(node -> node instanceof ExtrusionNode)
                .map(ExtrusionNode.class::cast)
                .map(ExtrusionNode::getD)
                .reduce(0f, Float::sum);

        List<MovementNode> movementNodes = layerNode.stream()
                .filter(node -> node instanceof MovementNode)
                .map(MovementNode.class::cast)
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
