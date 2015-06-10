package celtech.gcodetranslator.postprocessing;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.NozzleData;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.slicer.NozzleParameters;
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
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.comms.commands.MacroLoadException;
import celtech.utils.Math.MathUtils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyIntegerProperty;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.twod.Line;
import org.apache.commons.math3.geometry.euclidean.twod.Segment;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
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
    private final Project project;
    private final SlicerParametersFile slicerParametersFile;
    private final List<Integer> objectToNozzleNumberMap;
    private final Map<Integer, Integer> extruderToNozzleMap;
    private final Map<Integer, Integer> nozzleToExtruderMap;

    private final List<NozzleProxy> nozzleProxies = new ArrayList<>();

    private final PostProcessorFeatureSet featureSet;

    private PostProcessingMode postProcessingMode = PostProcessingMode.TASK_BASED_NOZZLE_SELECTION;

    protected List<Integer> layerNumberToLineNumber;
    protected List<Double> layerNumberToPredictedDuration;

    private final int maxNumberOfIntersectionsToConsider;
    private final float maxDistanceFromEndPoint;

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

        extruderToNozzleMap = new HashMap<>();
        nozzleToExtruderMap = new HashMap<>();
        for (int extruderNumber = 0; extruderNumber < 2; extruderNumber++)
        {
            Optional<NozzleProxy> proxy = chooseNozzleProxyByExtruderNumber(extruderNumber);
            if (proxy.isPresent())
            {
                extruderToNozzleMap.put(extruderNumber, proxy.get().getNozzleReferenceNumber());
                nozzleToExtruderMap.put(proxy.get().getNozzleReferenceNumber(), extruderNumber);
            }
        }

        objectToNozzleNumberMap = new ArrayList<>();
        project.getLoadedModels().stream()
                .map(ModelContainer::getAssociateWithExtruderNumberProperty)
                .map(ReadOnlyIntegerProperty::get)
                .forEach(extruderNumber ->
                        {
                            objectToNozzleNumberMap.add(extruderToNozzleMap.get(extruderNumber));
                });

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

        maxNumberOfIntersectionsToConsider = project.getPrinterSettings().getSettings().getNumberOfPerimeters();
        maxDistanceFromEndPoint = project.getPrinterSettings().getSettings().getPerimeterExtrusionWidth_mm()
                * 1.01f * maxNumberOfIntersectionsToConsider;

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

        int layerCounter = -1;

        //Cura has line delineators like this ';LAYER:1'
        try
        {
            fileReader = new BufferedReader(new FileReader(gcodeFileToProcess));
            writer = Lookup.getPostProcessorOutputWriterFactory().create(gcodeOutputFile);

            prependPrePrintHeader(writer);

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

        assignExtrusionToCorrectExtruder(layerNode);

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

                            if (potentialObjectNumber
                            < 0)
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
                }
                );
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

    protected void insertNozzleCloses(GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
        //Assume the nozzle is always fully open...
        nozzleInUse.setCurrentPosition(1.0);
        if (featureSet.isEnabled(PostProcessorFeature.GRADUAL_CLOSE))
        {
            insertProgressiveNozzleCloseUpToEvent(node, nozzleInUse);
        } else
        {
            insertNozzleCloseFullyAfterEvent(node, nozzleInUse);
        }
    }

    protected void insertNozzleCloseFullyAfterEvent(GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
        NozzleValvePositionNode newNozzleValvePositionNode = new NozzleValvePositionNode();
        newNozzleValvePositionNode.setDesiredValvePosition(nozzleInUse.getNozzleParameters().getClosedPosition());
        node.addSiblingAfter(newNozzleValvePositionNode);
    }

    protected void insertProgressiveNozzleCloseUpToEvent(GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
        if (node.getParent() instanceof FillSectionNode)
        {
            closeToEndOfFill(node, nozzleInUse);
        } else if (node.getParent() instanceof OuterPerimeterSectionNode)
        {
            closeInwardFromOuterPerimeter(node, nozzleInUse);
        }
//        NozzleValvePositionNode newNozzleValvePositionNode = new NozzleValvePositionNode();
//        newNozzleValvePositionNode.setDesiredValvePosition(nozzleInUse.getNozzleParameters().getClosedPosition());
//        node.addSiblingAfter(newNozzleValvePositionNode);
    }

    protected void closeToEndOfFill(GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
        SectionNode thisSection = (SectionNode) (node.getParent());

        float extrusionInSection = thisSection.streamChildrenAndMeBackwards()
                .filter(extrusionnode -> extrusionnode instanceof ExtrusionNode)
                .map(ExtrusionNode.class::cast)
                .map(ExtrusionNode::getE)
                .reduce(0f, (s1, s2) -> s1 + s2);

        NozzleParameters nozzleParams = nozzleInUse.getNozzleParameters();

        double volumeToCloseOver = nozzleParams.getEjectionVolume();

        if (extrusionInSection >= volumeToCloseOver)
        {
            List<ExtrusionNode> extrusionNodes = thisSection.streamChildrenAndMeBackwards()
                    .filter(foundNode -> foundNode instanceof ExtrusionNode)
                    .map(ExtrusionNode.class::cast)
                    .collect(Collectors.toList());

            double runningTotalOfExtrusion = 0;

            for (ExtrusionNode extrusionNodeBeingExamined : extrusionNodes)
            {
                int comparisonResult = MathUtils.compareDouble(runningTotalOfExtrusion + extrusionNodeBeingExamined.getE(), volumeToCloseOver, 0.00001);

                if (comparisonResult == MathUtils.LESS_THAN)
                {
                    //One step along the way
                    double bValue = runningTotalOfExtrusion / volumeToCloseOver;
                    extrusionNodeBeingExamined.setB(bValue);
                    runningTotalOfExtrusion += extrusionNodeBeingExamined.getE();
                    //No extrusion during a close
                    extrusionNodeBeingExamined.eNotInUse();
                } else if (comparisonResult == MathUtils.EQUAL)
                {
                    //All done
                    double bValue = runningTotalOfExtrusion / volumeToCloseOver;
                    extrusionNodeBeingExamined.setB(bValue);
                    runningTotalOfExtrusion += extrusionNodeBeingExamined.getE();
                    //No extrusion during a close
                    extrusionNodeBeingExamined.eNotInUse();
                    break;
                } else
                {
                    //If we got here then we need to split this extrusion
                    Optional<GCodeEventNode> siblingBefore = extrusionNodeBeingExamined.getSiblingBefore();

                    if (!siblingBefore.isPresent())
                    {
                        throw new RuntimeException("Unable to find prior sibling when splitting extrusion at node " + extrusionNodeBeingExamined.renderForOutput());
                    }

                    if (siblingBefore.get() instanceof MovementNode)
                    {
                        // We can work out how to split this extrusion
                        MovementNode priorMovement = (MovementNode) siblingBefore.get();
                        Vector2D firstPoint = new Vector2D(priorMovement.getX(), priorMovement.getY());
                        Vector2D secondPoint = new Vector2D(extrusionNodeBeingExamined.getX(), extrusionNodeBeingExamined.getY());

                        double extrusionInFirstSection = runningTotalOfExtrusion + extrusionNodeBeingExamined.getE() - volumeToCloseOver;
                        double extrusionInSecondSection = extrusionNodeBeingExamined.getE() - extrusionInFirstSection;

                        double proportionOfDistanceInFirstSection = extrusionInFirstSection / extrusionNodeBeingExamined.getE();

                        Vector2D actualVector = secondPoint.subtract(firstPoint);
                        Vector2D firstSegment = firstPoint.add(proportionOfDistanceInFirstSection,
                                actualVector);

                        ExtrusionNode newExtrusionNode = new ExtrusionNode();
                        newExtrusionNode.setComment("Segment remainder");
                        newExtrusionNode.setE((float) extrusionInFirstSection);
                        newExtrusionNode.setX(firstSegment.getX());
                        newExtrusionNode.setY(firstSegment.getY());

                        extrusionNodeBeingExamined.addSiblingBefore(newExtrusionNode);

                        extrusionNodeBeingExamined.setE((float) extrusionInSecondSection);
                        extrusionNodeBeingExamined.appendComment("Start of close segment");
                        double bValue = runningTotalOfExtrusion / volumeToCloseOver;
                        extrusionNodeBeingExamined.setB(bValue);

                        runningTotalOfExtrusion += extrusionNodeBeingExamined.getE();
                        //No extrusion during a close
                        extrusionNodeBeingExamined.eNotInUse();
                    } else
                    {
                        throw new RuntimeException("Prior sibling was not movement node when splitting extrusion");
                    }
                    break;
                }
            }
        } else
        {
            throw new RuntimeException("Not enough extrusion volume to close in Fill / Object");
        }
    }

    protected void addClosesUsingSpecifiedNode(GCodeEventNode nodeToAddClosesTo,
            GCodeEventNode nodeToCopyCloseFrom,
            final NozzleProxy nozzleInUse,
            boolean towardsEnd,
            double availableVolume,
            boolean closeOverAvailableVolume)
    {
        List<ExtrusionNode> extrusionNodesToCopy = null;

        if (nodeToAddClosesTo.getParent() == null
                || !(nodeToAddClosesTo.getParent() instanceof SectionNode))
        {
            throw new RuntimeException("Parent of specified node " + nodeToAddClosesTo.renderForOutput() + " is not a section");
        }

        SectionNode parentSectionToAddClosesTo = (SectionNode) nodeToAddClosesTo.getParent();

        try
        {
            if (towardsEnd)
            {
                extrusionNodesToCopy = nodeToCopyCloseFrom.streamSiblingsFromHere()
                        .filter(extrusionnode -> extrusionnode instanceof ExtrusionNode)
                        .map(ExtrusionNode.class::cast)
                        .collect(Collectors.toList());
            } else
            {
                extrusionNodesToCopy = nodeToCopyCloseFrom.streamSiblingsBackwardsFromHere()
                        .filter(extrusionnode -> extrusionnode instanceof ExtrusionNode)
                        .map(ExtrusionNode.class::cast)
                        .collect(Collectors.toList());
                extrusionNodesToCopy.add(extrusionNodesToCopy.size(), (ExtrusionNode)nodeToCopyCloseFrom);
            }
        } catch (NodeProcessingException ex)
        {
            throw new RuntimeException("Failed to stream siblings", ex);
        }

        NozzleParameters nozzleParams = nozzleInUse.getNozzleParameters();

        double volumeToCloseOver = nozzleParams.getEjectionVolume();

        double runningTotalOfExtrusion = 0;
        double currentNozzlePosition = nozzleInUse.getCurrentPosition();
        double closePermm3Volume = closeOverAvailableVolume == false ? currentNozzlePosition / volumeToCloseOver : currentNozzlePosition / availableVolume;
        double requiredVolumeToCloseOver = currentNozzlePosition / closePermm3Volume;

        ExtrusionNode lastExtrusionNode = null;

        for (ExtrusionNode extrusionNodeToCopy : extrusionNodesToCopy)
        {
            int comparisonResult = MathUtils.compareDouble(runningTotalOfExtrusion + extrusionNodeToCopy.getE(), requiredVolumeToCloseOver, 0.00001);

            ExtrusionNode copy = extrusionNodeToCopy.clone();
            parentSectionToAddClosesTo.addChildAtEnd(copy);

            if (comparisonResult == MathUtils.LESS_THAN)
            {
                //One step along the way
                currentNozzlePosition = currentNozzlePosition - copy.getE() * closePermm3Volume;
                double bValue = currentNozzlePosition;
                copy.setB(bValue);
                runningTotalOfExtrusion += copy.getE();
                //No extrusion during a close
                copy.eNotInUse();
            } else if (comparisonResult == MathUtils.EQUAL)
            {
                //All done
                currentNozzlePosition = 0;
                double bValue = 0;
                copy.setB(bValue);
                runningTotalOfExtrusion += copy.getE();
                //No extrusion during a close
                copy.eNotInUse();
                break;
            } else
            {
                if (lastExtrusionNode == null)
                {
                    throw new RuntimeException("No prior node to extrapolate from");
                }

                // We can work out how to split this extrusion
                Vector2D firstPoint = new Vector2D(lastExtrusionNode.getX(), lastExtrusionNode.getY());
                Vector2D secondPoint = new Vector2D(extrusionNodeToCopy.getX(), extrusionNodeToCopy.getY());

                double extrusionInFirstSection = runningTotalOfExtrusion + extrusionNodeToCopy.getE() - volumeToCloseOver;

                double proportionOfDistanceInFirstSection = extrusionInFirstSection / extrusionNodeToCopy.getE();

                Vector2D actualVector = secondPoint.subtract(firstPoint);
                Vector2D firstSegment = firstPoint.add(proportionOfDistanceInFirstSection,
                        actualVector);

                copy.setX(firstSegment.getX());
                copy.setY(firstSegment.getY());
                copy.setE(0);
                copy.setD(0);
                copy.appendComment("End of close segment");
                copy.setB(0);

                runningTotalOfExtrusion += copy.getE();
                //No extrusion during a close
                copy.eNotInUse();
                break;
            }

            lastExtrusionNode = extrusionNodeToCopy;
        }
    }

    protected void closeInwardFromOuterPerimeter(final GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
        //Pick the earliest possible InnerPerimeter

        Optional<GCodeEventNode> priorSection = node.getParent().getSiblingBefore();
        if (priorSection.isPresent())
        {
            if (priorSection.get() instanceof InnerPerimeterSectionNode)
            {
                //We can use this
                Optional<IntersectionResult> result = findClosestExtrusionNode((ExtrusionNode) node, (SectionNode) (priorSection.get()));
                if (result.isPresent())
                {
                    //Found a node
                    //Add a travel to the intersection point
                    TravelNode travelToClosestNode = new TravelNode();
                    travelToClosestNode.setFeedRate_mmPerMin(400);
                    travelToClosestNode.setX(result.get().getClosestNode().getX());
                    travelToClosestNode.setY(result.get().getClosestNode().getY());
                    node.addSiblingAfter(travelToClosestNode);

                    //We'll close in the direction that has most space
                    int indexOfFoundNode = priorSection.get().getChildren().indexOf(result.get().getClosestNode());

                    double volumeToCloseOver = nozzleInUse.getNozzleParameters().getEjectionVolume();

                    try
                    {

                        //Try forwards first
                        float forwardExtrusionTotal = result.get().getClosestNode().streamSiblingsFromHere()
                                .filter(searchNode -> searchNode instanceof ExtrusionNode)
                                .map(ExtrusionNode.class::cast)
                                .map(ExtrusionNode::getE)
                                .reduce(0f, (s1, s2) -> s1 + s2);

                        if (forwardExtrusionTotal >= volumeToCloseOver)
                        {
                            addClosesUsingSpecifiedNode(node, result.get().getClosestNode(), nozzleInUse, true, forwardExtrusionTotal, false);
                        } else
                        {
                            //Try backwards
                            float backwardExtrusionTotal = result.get().getClosestNode().streamSiblingsBackwardsFromHere()
                                    .filter(searchNode -> searchNode instanceof ExtrusionNode)
                                    .map(ExtrusionNode.class::cast)
                                    .map(ExtrusionNode::getE)
                                    .reduce(0f, (s1, s2) -> s1 + s2);

                            if (backwardExtrusionTotal >= volumeToCloseOver)
                            {
                                addClosesUsingSpecifiedNode(node, result.get().getClosestNode(), nozzleInUse, false, backwardExtrusionTotal, false);
                            } else
                            {
                                //Close over the largest available volume - no lower limit!!
                                if (forwardExtrusionTotal > backwardExtrusionTotal)
                                {
                                    addClosesUsingSpecifiedNode(node, result.get().getClosestNode(), nozzleInUse, true, forwardExtrusionTotal, true);
                                } else
                                {
                                    addClosesUsingSpecifiedNode(node, result.get().getClosestNode(), nozzleInUse, false, backwardExtrusionTotal, true);
                                }
                            }
                        }
                    } catch (NodeProcessingException ex)
                    {
                        throw new RuntimeException("Failure to find correct direction to traverse in for node " + node.renderForOutput());
                    }
                } else
                {
                    throw new RuntimeException("Couldn't find closest node when looking for inner perimeter close trajectory");
                }

            } else
            {
                throw new RuntimeException("Error attempting close - no prior sibling for node " + node.renderForOutput());
            }
        }
    }

    protected Optional<IntersectionResult> findClosestExtrusionNode(ExtrusionNode node, SectionNode priorSection)
    {
        ExtrusionNode closestNode = null;
        Vector2D intersectionPoint = null;
        Optional<IntersectionResult> result = Optional.empty();

        //If we got here then we need to split this extrusion
        Optional<GCodeEventNode> siblingBefore = node.getSiblingBefore();

        if (!siblingBefore.isPresent())
        {
            throw new RuntimeException("Unable to find prior sibling when looking for inward move");
        }

        if (siblingBefore.get() instanceof ExtrusionNode)
        {
            // We can work out how to split this extrusion
            ExtrusionNode priorExtrusion = (ExtrusionNode) siblingBefore.get();

            //Get an orthogonal to the extrusion we're considering
            Vector2D priorPoint = priorExtrusion.toVector2D();
            Vector2D thisPoint = ((MovementNode) node).toVector2D();

            Segment orthogonalSegment = MathUtils.getOrthogonalLineToLinePoints(maxDistanceFromEndPoint, priorPoint, thisPoint);
            Vector2D orthogonalSegmentMidpoint = MathUtils.findMidPoint(orthogonalSegment.getStart(),
                    orthogonalSegment.getEnd());

            List<ExtrusionNode> extrusionNodesUnderConsideration = priorSection.stream()
                    .filter(extrusionnode -> extrusionnode instanceof ExtrusionNode)
                    .map(ExtrusionNode.class::cast)
                    .collect(Collectors.toList());

            Vector2D lastPointConsidered = null;

            double closestDistanceSoFar = 999;

            for (ExtrusionNode extrusionNodeUnderConsideration : extrusionNodesUnderConsideration)
            {
                Vector2D extrusionPoint = extrusionNodeUnderConsideration.toVector2D();

                if (lastPointConsidered != null)
                {
                    Segment segmentUnderConsideration = new Segment(lastPointConsidered,
                            extrusionPoint, new Line(
                                    lastPointConsidered,
                                    extrusionPoint, 1e-12));

                    Vector2D tempIntersectionPoint = MathUtils.getSegmentIntersection(
                            orthogonalSegment, segmentUnderConsideration);

                    if (tempIntersectionPoint != null)
                    {
                        double distanceFromMidPoint = tempIntersectionPoint.distance(
                                orthogonalSegmentMidpoint);

                        if (distanceFromMidPoint < closestDistanceSoFar)
                        {
                            closestNode = extrusionNodeUnderConsideration;
                            closestDistanceSoFar = distanceFromMidPoint;
                            intersectionPoint = tempIntersectionPoint;
                        }
                    }
                }

                lastPointConsidered = extrusionPoint;
            }
        } else
        {
            //Default to using the Outer...
            throw new RuntimeException("Error attempting close - have to use outer perimeter " + node.renderForOutput());
        }

        if (closestNode != null
                && intersectionPoint != null)
        {
            result = Optional.of(new IntersectionResult(closestNode, intersectionPoint));
        }

        return result;
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

            if (objectNodes.size()
                    > 0)
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

        if (objectNodes.size()
                > 0)
        {
            lastObjectReferenceNumber = objectNodes.get(objectNodes.size() - 1).getObjectNumber();
        }

        //We'll need at least one of these per layer
        ToolSelectNode toolSelectNode = null;

        SectionNode lastSectionNode = null;

        for (GCodeEventNode objectNode : objectNodes)
        {
            ObjectDelineationNode objectNodeBeingExamined = (ObjectDelineationNode) objectNode;

            NozzleProxy requiredNozzle = nozzleProxies.get(objectToNozzleNumberMap.get(objectNodeBeingExamined.getObjectNumber()));

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

                    lastSectionNode = sectionNodeUnderExamination;
                } else
                {
                    //Probably a travel node - move it over without changing it
                    childNode.removeFromParent();
                    toolSelectNode.addChildAtEnd(childNode);
                }
            }
        }

        return lastObjectReferenceNumber;
    }

    protected void assignExtrusionToCorrectExtruder(LayerNode layerNode)
    {
        // Don't change anything if we're in task-based selection as this always uses extruder E

        if (postProcessingMode != PostProcessingMode.TASK_BASED_NOZZLE_SELECTION)
        {
            layerNode.stream()
                    .filter(node -> node instanceof ToolSelectNode)
                    .map(ToolSelectNode.class::cast)
                    .forEach(toolSelectNode ->
                            {
                                switch (nozzleToExtruderMap.get(toolSelectNode.getToolNumber()))
                                {
                                    case 0:
                                        toolSelectNode.stream()
                                        .filter(node -> node instanceof ExtrusionNode)
                                        .map(ExtrusionNode.class::cast)
                                        .forEach(extrusionNode ->
                                                {
                                                    extrusionNode.extrudeUsingEOnly();
                                        });
                                        break;
                                    case 1:
                                        toolSelectNode.stream()
                                        .filter(node -> node instanceof ExtrusionNode)
                                        .map(ExtrusionNode.class::cast)
                                        .forEach(extrusionNode ->
                                                {
                                                    extrusionNode.extrudeUsingDOnly();
                                        });
                                        break;
                                }
                    });
        }
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
                                                        insertNozzleCloses(priorExtrusionNode.get(), nozzleInUse);

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
                                                            insertNozzleCloses(priorExtrusionNodeLastLayer.orElseThrow(NodeProcessingException::new), nozzleInUse);

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
                                    insertNozzleCloses(lastExtrusionNode, nozzleInUse);
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
