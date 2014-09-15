package celtech.gcodetranslator;

import celtech.gcodetranslator.events.CommentEvent;
import celtech.gcodetranslator.events.EndOfFileEvent;
import celtech.gcodetranslator.events.ExtrusionEvent;
import celtech.gcodetranslator.events.GCodeEvent;
import celtech.gcodetranslator.events.GCodeParseEvent;
import celtech.gcodetranslator.events.LayerChangeEvent;
import celtech.gcodetranslator.events.MCodeEvent;
import celtech.gcodetranslator.events.MovementEvent;
import celtech.gcodetranslator.events.NozzleChangeBValueEvent;
import celtech.gcodetranslator.events.NozzleChangeEvent;
import celtech.gcodetranslator.events.NozzleCloseFullyEvent;
import celtech.gcodetranslator.events.NozzleOpenFullyEvent;
import celtech.gcodetranslator.events.NozzlePositionChangeEvent;
import celtech.gcodetranslator.events.RetractDuringExtrusionEvent;
import celtech.gcodetranslator.events.RetractEvent;
import celtech.gcodetranslator.events.TravelEvent;
import celtech.gcodetranslator.events.UnretractEvent;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.services.slicer.RoboxProfile;
import celtech.utils.Math.MathUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.DoubleProperty;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.twod.Line;
import org.apache.commons.math3.geometry.euclidean.twod.Segment;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Ian
 */
public class GCodeRoboxiser implements GCodeTranslationEventHandler
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        GCodeTranslationEventHandler.class.getName());
    private GCodeFileParser gcodeParser = new GCodeFileParser();

    private Pattern passThroughPattern = Pattern.compile(
        "\\b(?:M106 S[0-9.]+|M107|G[0-9]{2,}|M[0-9]{2,}|G28 [XYZ]+[0-9]*)(?:[\\s]*;.*)?");
    private Matcher passThroughMatcher = null;

    private Pattern removePattern = Pattern.compile(
        "\\b(?:M104 S[0-9.]+(?:\\sT[0-9]+)?|M109 S[0-9.]+(?:\\sT[0-9]+)?|M107)(?:[\\s]*;.*)?");
    private Matcher removeMatcher = null;

    private boolean initialTemperaturesWritten = false;
    private boolean subsequentLayersTemperaturesWritten = false;

    private double distanceSoFar = 0;
    private double totalExtrudedVolume = 0;
    private double totalXYMovement = 0;
    private int layer = 0;

    private final ArrayList<Nozzle> nozzles = new ArrayList<>();
    private Nozzle currentNozzle = null;

    private Tool selectedTool = Tool.Unknown;
    private double currentFeedrate = 0;
    private double currentZHeight = 0;

    //Profile variables
    private double startClosingByMM = 2;

    private Vector2D lastPoint = null;
    private Vector2D nozzleLastOpenedAt = null;
    private Vector2D nozzleLastClosedAt = null;

    private ArrayList<GCodeParseEvent> extrusionBuffer = new ArrayList<>();
//    private Vector2D precursorPoint = null;

    private boolean triggerCloseFromTravel = false;
    private boolean triggerCloseFromRetract = true;

    private int tempNozzleMemory = 0;
    private int nozzleInUse = -1;
    private int forcedNozzleOnFirstLayer = -1;
    private boolean nozzleHasBeenForced = false;
    private boolean nozzleHasBeenReinstated = false;

    private boolean internalClose = true;
    private boolean autoGenerateNozzleChangeEvents = true;

    private double predictedDurationInLayer = 0.0;
    private double volumeUsed = 0.0;
    private double autoUnretractEValue = 0.0;
    private double autoUnretractDValue = 0.0;

    private boolean mixExtruderOutputs = false;
    private ArrayList<ExtruderMix> extruderMixPoints = new ArrayList<>();
    private double currentEMixValue = 1;
    private double currentDMixValue = 0;
    private double startingEMixValue = 1;
    private double startingDMixValue = 0;
    private double endEMixValue = 1;
    private double endDMixValue = 0;
    private int mixFromLayer = 0;
    private int mixToLayer = 0;
    private int currentMixPoint = 0;

    private OutputWriter outputWriter;
    private int layerIndex = 0;
    private List<Integer> layerNumberToLineNumber;
    private List<Double> layerNumberToDistanceTravelled;
    private List<Double> layerNumberToPredictedDuration;
    private double distanceSoFarInLayer = 0;
    private Integer lineNumberOfFirstExtrusion;

    private Nozzle point3mmNozzle = null;
    private Nozzle point8mmNozzle = null;

    private RoboxProfile currentSettings = null;
    private int wipeFeedRate_mmPerMin = 0;

    // Causes home and return events to be inserted, triggering the camera
    private boolean movieMakerEnabled = false;

    /**
     * OutputWriter is a wrapper to a file writer that allows us to count the number of non-comment and non-blank lines.
     */
    class OutputWriter
    {

        private int numberOfLinesOutput = 0;
        private BufferedWriter fileWriter = null;

        OutputWriter(String fileLocation) throws IOException
        {
            File outputFile = new File(fileLocation);
            fileWriter = new BufferedWriter(new FileWriter(outputFile));
        }

        void writeOutput(String outputLine) throws IOException
        {
            fileWriter.write(outputLine);
            // if it's not a comment or blank line
            if (!outputLine.trim().startsWith(";") && !"".equals(
                outputLine.trim()))
            {
                numberOfLinesOutput++;
            }
        }

        void close() throws IOException
        {
            fileWriter.close();
        }

        void newLine() throws IOException
        {
            fileWriter.newLine();
        }

        void flush() throws IOException
        {
            fileWriter.flush();
        }

        /**
         * @return the numberOfLinesOutput
         */
        public int getNumberOfLinesOutput()
        {
            return numberOfLinesOutput;
        }
    }

    /**
     *
     */
    public GCodeRoboxiser()
    {
        gcodeParser.addListener(this);
    }

    /**
     *
     * @param inputFilename
     * @param outputFilename
     * @param settings
     * @param percentProgress
     * @return
     */
    public RoboxiserResult roboxiseFile(String inputFilename,
        String outputFilename,
        RoboxProfile settings, DoubleProperty percentProgress)
    {
        currentSettings = settings;

        layerNumberToLineNumber = new ArrayList<>();
        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();
        layerNumberToDistanceTravelled.add(0, 0d);
        layerNumberToPredictedDuration.add(0, 0d);
        layerNumberToLineNumber.add(0, 0);

        RoboxiserResult result = new RoboxiserResult();
        boolean success = false;

        extruderMixPoints.clear();

        extruderMixPoints.add(
            new ExtruderMix(1, 0, 5));
        extruderMixPoints.add(
            new ExtruderMix(0, 1, 30));
        extruderMixPoints.add(
            new ExtruderMix(0.5, 0.5, 31));
        extruderMixPoints.add(
            new ExtruderMix(0.5, 0.5, 40));
        extruderMixPoints.add(
            new ExtruderMix(1, 0, 46));

        if (mixExtruderOutputs
            && extruderMixPoints.size()
            >= 2)
        {
            ExtruderMix firstMixPoint = extruderMixPoints.get(0);
            startingEMixValue = firstMixPoint.getEFactor();
            startingDMixValue = firstMixPoint.getDFactor();
            mixFromLayer = firstMixPoint.getLayerNumber();

            ExtruderMix secondMixPoint = extruderMixPoints.get(1);
            endEMixValue = secondMixPoint.getEFactor();
            endDMixValue = secondMixPoint.getDFactor();
            mixToLayer = secondMixPoint.getLayerNumber();
        } else
        {
            mixExtruderOutputs = false;
        }

        predictedDurationInLayer = 0.0;

        lastPoint = new Vector2D(0, 0);
        nozzleLastOpenedAt = new Vector2D(0, 0);
        nozzleLastClosedAt = new Vector2D(0, 0);

        initialTemperaturesWritten = false;
        subsequentLayersTemperaturesWritten = false;
        distanceSoFar = 0;
        totalExtrudedVolume = 0;
        totalXYMovement = 0;
        layer = 0;
        currentFeedrate = 0;
        currentZHeight = 0;

        forcedNozzleOnFirstLayer = settings.getForce_nozzle_on_first_layer().get();

//        internalClose = settings.getInternalClose();
        point3mmNozzle = new Nozzle(0,
                                    settings.getNozzle_open_over_volume().get(0).doubleValue(),
                                    settings.getNozzle_preejection_volume().get(0).doubleValue(),
                                    settings.getNozzle_ejection_volume().get(0).doubleValue(),
                                    settings.getNozzle_wipe_volume().get(0).doubleValue(),
                                    settings.getNozzle_close_at_midpoint().get(0).doubleValue(),
                                    settings.getNozzle_close_midpoint_percent().get(0).doubleValue(),
                                    settings.getNozzle_partial_b_minimum().get(0).doubleValue());
        point8mmNozzle = new Nozzle(1,
                                    settings.getNozzle_open_over_volume().get(1).doubleValue(),
                                    settings.getNozzle_preejection_volume().get(1).doubleValue(),
                                    settings.getNozzle_ejection_volume().get(1).doubleValue(),
                                    settings.getNozzle_wipe_volume().get(1).doubleValue(),
                                    settings.getNozzle_close_at_midpoint().get(1).doubleValue(),
                                    settings.getNozzle_close_midpoint_percent().get(1).doubleValue(),
                                    settings.getNozzle_partial_b_minimum().get(1).doubleValue());

        nozzles.add(point3mmNozzle);

        nozzles.add(point8mmNozzle);

        wipeFeedRate_mmPerMin = currentSettings.perimeter_speedProperty().get() * 60;

        try
        {
            outputWriter = new OutputWriter(outputFilename);

            outputWriter.writeOutput("; File post-processed by the CEL Tech Roboxiser\n");

            outputWriter.writeOutput(";\n; Pre print gcode\n");
            for (String macroLine : GCodeMacros.getMacroContents("before_print"))
            {
                outputWriter.writeOutput(macroLine + "\n");
            }
            outputWriter.writeOutput("; End of Pre print gcode\n");

            insertInitialTemperatures();

            gcodeParser.parse(inputFilename, percentProgress);

            outputWriter.close();

            steno.info("Finished roboxising " + inputFilename);
            steno.info("Total extrusion volume " + totalExtrudedVolume + " mm3");
            steno.info("Total XY movement distance " + totalXYMovement + " mm");

            success = true;
        } catch (IOException ex)
        {
            steno.error("Error roboxising file " + inputFilename);
        }

        result.setSuccess(success);
        /**
         * TODO: layerNumberToLineNumber uses lines numbers from the GCode file so are a little less than the line numbers for each layer after roboxisation. As a quick fix for now set the line number
         * of the last layer to the actual maximum line number.
         */
        layerNumberToLineNumber.set(layerNumberToLineNumber.size() - 1, outputWriter.getNumberOfLinesOutput());
        PrintJobStatistics roboxisedStatistics = new PrintJobStatistics(
            outputWriter.getNumberOfLinesOutput(),
            volumeUsed, lineNumberOfFirstExtrusion,
            layerNumberToLineNumber, layerNumberToPredictedDuration);

        result.setRoboxisedStatistics(roboxisedStatistics);

        return result;
    }

    /**
     *
     * @param line
     */
    @Override
    public void unableToParse(String line)
    {
        try
        {
            if ((removeMatcher = removePattern.matcher(line)).matches())
            {
                steno.info("Removed " + line);
                outputWriter.writeOutput("; Removed: " + line);
            } else if ((passThroughMatcher = passThroughPattern.matcher(line)).matches())
            {
                outputWriter.writeOutput(line);
                outputWriter.newLine();
            } else
            {
                steno.warning("Unable to parse " + line);
                outputWriter.writeOutput("; >>>ERROR PARSING: " + line);
                outputWriter.newLine();
            }
        } catch (IOException ex)
        {
            steno.error("Parse error - " + line);
        }
    }

    private void resetMeasuringThing()
    {
        distanceSoFar = 0;
    }

    private void insertInitialTemperatures()
    {
        if (initialTemperaturesWritten == false)
        {
            MCodeEvent firstLayerBedTemp = new MCodeEvent();
            firstLayerBedTemp.setMNumber(139);
            firstLayerBedTemp.setComment(
                "take 1st layer bed temperature from reel");
            extrusionBuffer.add(firstLayerBedTemp);

            MCodeEvent waitForBedTemp = new MCodeEvent();
            waitForBedTemp.setMNumber(190);
            waitForBedTemp.setComment("wait for bed temperature to be reached");
            extrusionBuffer.add(waitForBedTemp);

            MCodeEvent firstLayerNozzleTemp = new MCodeEvent();
            firstLayerNozzleTemp.setMNumber(103);
            firstLayerNozzleTemp.setComment(
                "take 1st layer nozzle temperature from loaded reel");
            extrusionBuffer.add(firstLayerNozzleTemp);

            MCodeEvent waitForNozzleTemp = new MCodeEvent();
            waitForNozzleTemp.setMNumber(109);
            waitForNozzleTemp.setComment(
                "wait for nozzle temperature to be reached");
            extrusionBuffer.add(waitForNozzleTemp);

            MCodeEvent ambientTemp = new MCodeEvent();
            ambientTemp.setMNumber(170);
            ambientTemp.setComment("take ambient temperature from loaded reel");
            extrusionBuffer.add(ambientTemp);

            initialTemperaturesWritten = true;
        }
    }

    private void insertSubsequentLayerTemperatures()
    {
        if (subsequentLayersTemperaturesWritten == false)
        {
            MCodeEvent subsequentLayerNozzleTemp = new MCodeEvent();
            subsequentLayerNozzleTemp.setMNumber(104);
            subsequentLayerNozzleTemp.setComment(
                "take post layer 1 nozzle temperature from loaded reel - don't wait");
            extrusionBuffer.add(subsequentLayerNozzleTemp);

            MCodeEvent subsequentLayerBedTemp = new MCodeEvent();
            subsequentLayerBedTemp.setMNumber(140);
            subsequentLayerBedTemp.setComment(
                "take post layer 1 bed temperature from loaded reel - don't wait");
            extrusionBuffer.add(subsequentLayerBedTemp);

            subsequentLayersTemperaturesWritten = true;
        }
    }

    /**
     *
     * @param event
     * @throws celtech.gcodetranslator.NozzleCloseSettingsError
     */
    @Override
    public void processEvent(GCodeParseEvent event) throws PostProcessingError
    {
        //Buffer extrusion events only
        // Triggers to empty the buffer are written after the buffer has been dealt with
        // Non-triggers are written immediately

        Vector2D currentPoint = null;
        double distance = 0;

        // Home event - used when triggering the camera
        GCodeEvent homeEvent = new GCodeEvent();
        homeEvent.setComment("Home for camera");
        homeEvent.setGNumber(28);
        homeEvent.setGString("X");

        // Dwell event - used when triggering the camera to allow the home to be registered
        GCodeEvent dwellEvent = new GCodeEvent();
        dwellEvent.setComment("Dwell for camera - say cheese! :)");
        dwellEvent.setGNumber(4);
        dwellEvent.setGString("P550");

        GCodeEvent moveUpEvent = new GCodeEvent();
        moveUpEvent.setComment("Move up");
        moveUpEvent.setGNumber(0);
        moveUpEvent.setGString("Z5");

        GCodeEvent relativeMoveEvent = new GCodeEvent();
        relativeMoveEvent.setComment("Relative move");
        relativeMoveEvent.setGNumber(91);

        GCodeEvent absoluteMoveEvent = new GCodeEvent();
        absoluteMoveEvent.setComment("Absolute move");
        absoluteMoveEvent.setGNumber(90);

        GCodeEvent moveToMiddleYEvent = new GCodeEvent();
        moveToMiddleYEvent.setComment("Go to middle of bed in Y");
        moveToMiddleYEvent.setGNumber(0);
        moveToMiddleYEvent.setGString("Y50");

        try
        {

            if (event instanceof ExtrusionEvent)
            {
                if (lineNumberOfFirstExtrusion == null)
                {
                    lineNumberOfFirstExtrusion = event.getLinesSoFar();
                }

                if (layer == 2 && forcedNozzleOnFirstLayer >= 0 && nozzleHasBeenReinstated == false)
                {
                    nozzleHasBeenReinstated = true;
                    int nozzleToUse = chooseNozzleByTask(event);

                    if (nozzleToUse >= 0)
                    {
                        NozzleChangeEvent nozzleChangeEvent = new NozzleChangeEvent();
                        nozzleChangeEvent.setNozzleNumber(nozzleToUse);
                        nozzleChangeEvent.setComment("return to required nozzle");
                        extrusionBuffer.add(nozzleChangeEvent);

//                        //Open the nozzle fully                         
//                        NozzleOpenFullyEvent openNozzle = new NozzleOpenFullyEvent();
//                        openNozzle.setComment("Open nozzle");
//                        extrusionBuffer.add(openNozzle);
                        currentNozzle = nozzles.get(nozzleToUse);
//                        currentNozzle.openNozzleFully();
                    } else
                    {
                        steno.warning("Couldn't derive required nozzle to return to...");
                    }
                } else if (autoGenerateNozzleChangeEvents && ((forcedNozzleOnFirstLayer >= 0 && nozzleHasBeenReinstated)
                    || forcedNozzleOnFirstLayer < 0))
                {
                    int requiredNozzle = chooseNozzleByTask(event);

                    if (currentNozzle != null && requiredNozzle != currentNozzle.getReferenceNumber())
                    {
                        // Close the old nozzle
                        writeEventsWithNozzleClose("Closing last used nozzle");
                    }

                    if (currentNozzle == null || requiredNozzle != currentNozzle.getReferenceNumber())
                    {
                        //Select the nozzle
                        NozzleChangeEvent nozzleChangeEvent = new NozzleChangeEvent();
                        nozzleChangeEvent.setComment("Selecting nozzle " + requiredNozzle);
                        nozzleChangeEvent.setNozzleNumber(requiredNozzle);
                        extrusionBuffer.add(nozzleChangeEvent);

//                        //Open the nozzle fully                         
//                        NozzleOpenFullyEvent openNozzle = new NozzleOpenFullyEvent();
//                        openNozzle.setComment("Open nozzle");
//                        extrusionBuffer.add(openNozzle);
                        currentNozzle = nozzles.get(requiredNozzle);

//                        currentNozzle.openNozzleFully();
//                        nozzleLastOpenedAt = lastPoint;
                    }
                }

                ExtrusionEvent extrusionEvent = (ExtrusionEvent) event;
                currentPoint = new Vector2D(extrusionEvent.getX(),
                                            extrusionEvent.getY());

                totalExtrudedVolume += extrusionEvent.getE() + extrusionEvent.getD();

                if (currentNozzle.getState() != NozzleState.OPEN
                    && currentNozzle.getOpenOverVolume() == 0)
                {
                    // Unretract and open
                    NozzleOpenFullyEvent openNozzle = new NozzleOpenFullyEvent();
                    openNozzle.setComment("Open and replenish");
                    openNozzle.setE(autoUnretractEValue);
                    openNozzle.setD(autoUnretractDValue);

                    autoUnretractEValue = 0;
                    autoUnretractDValue = 0;

                    extrusionBuffer.add(openNozzle);
                    currentNozzle.openNozzleFully();
                    nozzleLastOpenedAt = lastPoint;
                }

                resetMeasuringThing();

                // Open the nozzle if it isn't already open
                // This will always be a single event prior to extrusion
                if (currentNozzle.getState() != NozzleState.OPEN)
                {
                    if (currentNozzle.getOpenOverVolume() <= 0)
                    {
                        NozzleOpenFullyEvent openNozzle = new NozzleOpenFullyEvent();
                        openNozzle.setComment("Extrusion trigger - open without replenish");
                        extrusionBuffer.add(openNozzle);
                    }
                    currentNozzle.openNozzleFully();
                    nozzleLastOpenedAt = currentPoint;
                }

                // Calculate how long this line is
                if (lastPoint != null)
                {
                    distance = lastPoint.distance(currentPoint);
                    extrusionEvent.setLength(distance);
//                        System.out.println("Distance " + distance);
                    distanceSoFar += distance;
                    totalXYMovement += distance;
//                        System.out.println("Total Distance " + distanceSoFar);
                }

                lastPoint = currentPoint;

                extrusionBuffer.add(extrusionEvent);
            } else if (event instanceof RetractDuringExtrusionEvent)
            {
                RetractDuringExtrusionEvent extrusionEvent = (RetractDuringExtrusionEvent) event;
                currentPoint = new Vector2D(extrusionEvent.getX(),
                                            extrusionEvent.getY());

                // Calculate how long this line is
                if (lastPoint != null)
                {
                    distance = lastPoint.distance(currentPoint);
                    extrusionEvent.setLength(distance);
//                        System.out.println("Distance " + distance);
                    distanceSoFar += distance;
                    totalXYMovement += distance;
//                        System.out.println("Total Distance " + distanceSoFar);
                }

                lastPoint = currentPoint;

                extrusionBuffer.add(extrusionEvent);
            } else if (event instanceof TravelEvent)
            {
                TravelEvent travelEvent = (TravelEvent) event;
                currentPoint = new Vector2D(travelEvent.getX(),
                                            travelEvent.getY());

                if (lastPoint != null)
                {
                    distance = lastPoint.distance(currentPoint);
                    travelEvent.setLength(distance);
//                        System.out.println("Distance " + distance);
                    distanceSoFar += distance;
                    totalXYMovement += distance;
//                        System.out.println("Total Distance " + distanceSoFar);

                    if (triggerCloseFromTravel == true
                        && (currentNozzle.getState() != NozzleState.CLOSED
                        && distance
                        > currentNozzle.getAllowedTravelBeforeClose()))
                    {
                        writeEventsWithNozzleClose("travel trigger");
                    }

                    extrusionBuffer.add(event);
                }
                lastPoint = currentPoint;

            } else if (event instanceof LayerChangeEvent)
            {
                LayerChangeEvent layerChangeEvent = (LayerChangeEvent) event;

                currentZHeight = layerChangeEvent.getZ();

                if (layer == 0 && forcedNozzleOnFirstLayer >= 0 && nozzleHasBeenForced == false)
                {
                    nozzleHasBeenForced = true;
                    NozzleChangeEvent nozzleChangeEvent = new NozzleChangeEvent();
                    //Force to required nozzle
                    nozzleChangeEvent.setNozzleNumber(forcedNozzleOnFirstLayer);
                    if (nozzleChangeEvent.getComment() != null)
                    {
                        nozzleChangeEvent.setComment(
                            nozzleChangeEvent.getComment()
                            + " - force to nozzle " + forcedNozzleOnFirstLayer + " on first layer");
                    } else
                    {
                        nozzleChangeEvent.setComment(" - force to nozzle " + forcedNozzleOnFirstLayer + " on first layer");
                    }
                    tempNozzleMemory = 0;
                    extrusionBuffer.add(nozzleChangeEvent);
                    nozzleInUse = forcedNozzleOnFirstLayer;
                    currentNozzle = nozzles.get(nozzleInUse);
                }

                if (layer == 1 && forcedNozzleOnFirstLayer >= 0)
                {
                    writeEventsWithNozzleClose(
                        "closing nozzle after forced nozzle select on layer 0");
                    insertSubsequentLayerTemperatures();
                }

                layer++;

                if (movieMakerEnabled && lastPoint != null)
                {
                    GCodeEvent moveBackIntoPlace = new GCodeEvent();
                    moveBackIntoPlace.setComment("Return to last position");
                    moveBackIntoPlace.setGNumber(0);
                    moveBackIntoPlace.setGString("X" + lastPoint.getX() + " Y" + lastPoint.getY());

                    if (currentNozzle.getState() != NozzleState.CLOSED)
                    {
                        extrusionBuffer.add(new NozzleCloseFullyEvent());
                    }

                    extrusionBuffer.add(relativeMoveEvent);
                    extrusionBuffer.add(moveUpEvent);
                    extrusionBuffer.add(absoluteMoveEvent);
                    extrusionBuffer.add(moveToMiddleYEvent);
                    extrusionBuffer.add(homeEvent);
                    extrusionBuffer.add(dwellEvent);
                    extrusionBuffer.add(moveBackIntoPlace);

                    if (currentNozzle.getState() != NozzleState.CLOSED)
                    {
                        extrusionBuffer.add(new NozzleOpenFullyEvent());
                    }

                }

                extrusionBuffer.add(event);
            } else if (event instanceof NozzleChangeEvent && !autoGenerateNozzleChangeEvents)
            {
                NozzleChangeEvent nozzleChangeEvent = (NozzleChangeEvent) event;

                if (layer == 0 && forcedNozzleOnFirstLayer >= 0 && nozzleHasBeenForced == false)
                {
                    nozzleHasBeenForced = true;
                    tempNozzleMemory = nozzleChangeEvent.getNozzleNumber();
                    //Force to required nozzle
                    nozzleChangeEvent.setNozzleNumber(forcedNozzleOnFirstLayer);
                    nozzleChangeEvent.setComment(
                        nozzleChangeEvent.getComment()
                        + " - force to nozzle " + forcedNozzleOnFirstLayer + " on first layer");
                    extrusionBuffer.add(nozzleChangeEvent);
                    nozzleInUse = forcedNozzleOnFirstLayer;
                    currentNozzle = nozzles.get(nozzleInUse);
                } else if (layer < 1)
                {
                    tempNozzleMemory = nozzleChangeEvent.getNozzleNumber();
                } else if (layer > 1)
                {
                    extrusionBuffer.add(nozzleChangeEvent);
                    nozzleInUse = nozzleChangeEvent.getNozzleNumber();
                    currentNozzle = nozzles.get(nozzleInUse);
                }
            } else if (event instanceof RetractEvent)
            {
                RetractEvent retractEvent = (RetractEvent) event;

                if (triggerCloseFromRetract == true && currentNozzle.getState()
                    != NozzleState.CLOSED)
                {
                    writeEventsWithNozzleClose("retract trigger");
                }

                resetMeasuringThing();
            } else if (event instanceof UnretractEvent && !autoGenerateNozzleChangeEvents)
            {
                UnretractEvent unretractEvent = (UnretractEvent) event;

                totalExtrudedVolume += unretractEvent.getE();

                if (currentNozzle.getState() != NozzleState.OPEN
                    && currentNozzle.getOpenOverVolume() <= 0)
                {
                    // Unretract and open
                    NozzleOpenFullyEvent openNozzle = new NozzleOpenFullyEvent();
                    openNozzle.setComment("Open and replenish");
                    openNozzle.setE(autoUnretractEValue);
                    openNozzle.setD(autoUnretractDValue);
                    extrusionBuffer.add(openNozzle);
                } else if (autoUnretractDValue > 0 || autoUnretractEValue > 0)
                {
                    // Just unretract
                    unretractEvent.setComment("Replenish before open");
                    unretractEvent.setE(autoUnretractEValue);
                    unretractEvent.setD(autoUnretractDValue);
                    extrusionBuffer.add(unretractEvent);
                }

                if (currentNozzle.getState() != NozzleState.OPEN)
                {
                    currentNozzle.openNozzleFully();
                    nozzleLastOpenedAt = lastPoint;
                }

                resetMeasuringThing();
                autoUnretractEValue = 0;
                autoUnretractDValue = 0;
            } else if (event instanceof MCodeEvent)
            {
                MCodeEvent mCodeEvent = (MCodeEvent) event;
                if (mCodeEvent.getMNumber() != 104
                    && mCodeEvent.getMNumber() != 109)
                {
                    extrusionBuffer.add(event);
                }
            } else if (event instanceof GCodeEvent)
            {
                extrusionBuffer.add(event);
            } else if (event instanceof CommentEvent)
            {
                extrusionBuffer.add(event);
            } else if (event instanceof EndOfFileEvent)
            {
                if (currentNozzle.getState() != NozzleState.CLOSED)
                {
                    writeEventsWithNozzleClose("End of file");
                }

                try
                {
                    outputWriter.writeOutput(";\n; Post print gcode\n");
                    for (String macroLine : GCodeMacros.getMacroContents(
                        "after_print"))
                    {
                        outputWriter.writeOutput(macroLine + "\n");
                    }
                    outputWriter.writeOutput("; End of Post print gcode\n");
                } catch (IOException ex)
                {
                    steno.error("Error whilst writing post-print gcode to file");
                }

                writeEventToFile(event);
            }
        } catch (Exception e)
        {
            steno.error("Exception whilst processing event " + event.toString());
            e.printStackTrace();
        }
    }

    private int chooseNozzleByTask(GCodeParseEvent event)
    {
        int nozzleToUse = -1;
        switch (getExtrusionType((ExtrusionEvent) event))
        {
            case Perimeter:
                nozzleToUse = currentSettings.getPerimeterNozzleProperty().get();
                break;
            case Infill:
                nozzleToUse = currentSettings.getFillNozzleProperty().get();
                break;
            case Support:
                nozzleToUse = currentSettings.getSupportNozzleProperty().get();
                break;
            case Support_Interface:
                nozzleToUse = currentSettings.getSupportInterfaceNozzleProperty().get();
                break;
            default:
                break;
        }
        return nozzleToUse;
    }

    private ExtrusionTask getExtrusionType(ExtrusionEvent eventToAssess)
    {
        ExtrusionTask determinedTask = ExtrusionTask.Perimeter;

        String comment = eventToAssess.getComment();

        if (comment != null)
        {
            if (comment.contains("perimeter"))
            {
                determinedTask = ExtrusionTask.Perimeter;
            } else if (comment.contains("fill"))
            {
                determinedTask = ExtrusionTask.Infill;
            } else if (comment.contains("support"))
            {
                determinedTask = ExtrusionTask.Support;
            } else
            {
                steno.warning("Couldn't determine type of extrusion event using comment: " + comment);
            }
        } else
        {
            steno.warning("Couldn't determine type of extrusion event using comment because comment was null");
        }

        return determinedTask;
    }

    private void writeEventToFile(GCodeParseEvent event)
    {
        try
        {
            outputWriter.writeOutput(event.renderForOutput());
            outputWriter.flush();
        } catch (IOException ex)
        {
            steno.error("Error whilst writing event to file");
        }
    }

    private void writeAllEvents()
    {
        for (GCodeParseEvent event : extrusionBuffer)
        {
            writeEventToFile(event);
        }

        extrusionBuffer.clear();
    }

    private final int EQUAL = 0;
    private final int MORE_THAN = 1;
    private final int LESS_THAN = -1;

    private int compareDouble(double a, double b)
    {
        double epsilon = 10e-5;
        double result = a - b;

        if (Math.abs(result) < epsilon)
        {
            return EQUAL;
        } else if (result > 0)
        {
            return MORE_THAN;
        } else
        {
            return LESS_THAN;
        }
    }

    protected void writeEventsWithNozzleClose(String comment) throws PostProcessingError
    {
        boolean closeAtEndOfPath = false;

        Map<EventType, Integer> eventIndices = new HashMap<EventType, Integer>();

        double nozzleStartPosition = 1.0;
        double nozzleCloseOverVolume = 1;

        if (closeAtEndOfPath
            || (currentNozzle.getPreejectionVolume() == 0
            && currentNozzle.getEjectionVolume() == 0
            && currentNozzle.getWipeVolume() == 0))
        {
            // Write the extrudes unchanged
            for (GCodeParseEvent extrusionEvent : extrusionBuffer)
            {
                writeEventToFile(extrusionEvent);
            }

            extrusionBuffer.clear();

            // Now write a close nozzle at the end of the path
            NozzleCloseFullyEvent closeNozzle = new NozzleCloseFullyEvent();
            closeNozzle.setComment(comment);
            closeNozzle.setLength(0);
            writeEventToFile(closeNozzle);
            currentNozzle.closeNozzleFully();
        } else
        {
            //Quick check to see if the extrusion volumes are sufficient to allow normal close
            double totalExtrusionForPath = 0;

            ArrayList<Integer> inwardsMoveIndexList = new ArrayList<>();

            int extrusionBufferIndex = 0;

            for (GCodeParseEvent event : extrusionBuffer)
            {
                if (event instanceof ExtrusionEvent)
                {
                    totalExtrusionForPath += ((ExtrusionEvent) event).getE() + ((ExtrusionEvent) event).getD();
                } else if (event instanceof TravelEvent)
                {
                    String eventComment = event.getComment();
                    if (eventComment != null)
                    {
                        //TODO Slic3r specific code!
                        if (eventComment.contains("move inwards"))
                        {
                            inwardsMoveIndexList.add(extrusionBufferIndex);
                        }
                    }
                }

                extrusionBufferIndex++;
            }

            TravelEvent lastInwardsMove = null;

            if (!inwardsMoveIndexList.isEmpty())
            {
                int lastInwardsMoveIndex = inwardsMoveIndexList.get(inwardsMoveIndexList.size() - 1);
                int currentFinalExtrusionEventIndex = getPreviousExtrusionEventIndex(extrusionBuffer.size() - 1);

                if (lastInwardsMoveIndex > currentFinalExtrusionEventIndex)
                {
                    lastInwardsMove = (TravelEvent) extrusionBuffer.get(inwardsMoveIndexList.get(inwardsMoveIndexList.size() - 1));
                }

                for (int i = inwardsMoveIndexList.size() - 1; i >= 0; i--)
                {
                    int indexToRemove = inwardsMoveIndexList.get(i);
                    extrusionBuffer.remove(indexToRemove);
                }
            }

            int finalExtrusionEventIndex = getPreviousExtrusionEventIndex(extrusionBuffer.size() - 1);

            if (finalExtrusionEventIndex >= 0)
            {
                ExtrusionTask extrusionTask = getExtrusionType((ExtrusionEvent) extrusionBuffer.get(finalExtrusionEventIndex));

                if (extrusionTask != ExtrusionTask.Infill)
                {

                    if ((totalExtrusionForPath >= currentNozzle.getOpenOverVolume()
                        + currentNozzle.getPreejectionVolume())
                        && (totalExtrusionForPath >= currentNozzle.getEjectionVolume()
                        + currentNozzle.getWipeVolume()))
                    {
                        //OK - we're go for a normal close   
                        nozzleStartPosition = 1.0;
                        nozzleCloseOverVolume = currentNozzle.getEjectionVolume();

                        //Always insert the open first
                        if (currentNozzle.getOpenOverVolume() > 0)
                        {
                            insertVolumeBreak(extrusionBuffer,
                                              eventIndices,
                                              EventType.NOZZLE_OPEN_END,
                                              currentNozzle.getOpenOverVolume(),
                                              comment,
                                              FindEventDirection.FORWARDS_FROM_START);
                        }

                        int sizeOfBuffer = extrusionBuffer.size();
                        int startOfClose = insertTravelAndClosePath(finalExtrusionEventIndex, "Move to start of wipe", false, lastInwardsMove);
                        int newSizeOfBuffer = extrusionBuffer.size();

//                    if (currentNozzle.getWipeVolume() > 0)
//                    {
//
//                        insertVolumeBreak(extrusionBuffer,
//                                          eventIndices,
//                                          EventType.WIPE_START,
//                                          currentNozzle.getWipeVolume(),
//                                          comment,
//                                          FindEventDirection.BACKWARDS_FROM_END);
//                    }
                        eventIndices.put(EventType.NOZZLE_CLOSE_START, startOfClose);

                    } else if (totalExtrusionForPath > 0)
                    {
                        double volumeToConsider = 2 * totalExtrusionForPath;

                        if (totalExtrusionForPath < currentNozzle.getOpenOverVolume()
                            || totalExtrusionForPath < currentNozzle.getEjectionVolume())
                        {
                            // We don't have enough volume to open and close as per settings
                            double bValue = Math.min(1, totalExtrusionForPath / currentNozzle.getEjectionVolume());
                            bValue = Math.max(currentNozzle.getPartialBMinimum(), bValue);

                            nozzleStartPosition = bValue;
                            nozzleCloseOverVolume = Math.min(currentNozzle.getEjectionVolume(), totalExtrusionForPath);

                            if (currentNozzle.getOpenOverVolume() > 0)
                            {
                                NozzleChangeBValueEvent partialOpen = new NozzleChangeBValueEvent();
                                partialOpen.setB(bValue);
                                int firstExtrusion = getNextExtrusionEventIndex(0);
                                extrusionBuffer.add(firstExtrusion, partialOpen);
                            } else
                            {
                                replaceOpenNozzleWithPartialOpen(bValue);
                            }

                            int penultimateExtrusionEventIndex = getPreviousExtrusionEventIndex(finalExtrusionEventIndex - 1);

                            if (penultimateExtrusionEventIndex < 0)
                            {
                                // There is only one extrusion event, so nothing to copy
                                int previousTravelEventIndex = getPreviousEventIndex(finalExtrusionEventIndex - 1, TravelEvent.class);

                                if (previousTravelEventIndex
                                    >= 0)
                                {
                                    ExtrusionEvent finalExtrusionEvent = (ExtrusionEvent) extrusionBuffer.get(finalExtrusionEventIndex);
                                    TravelEvent previousTravelEvent = (TravelEvent) extrusionBuffer.get(previousTravelEventIndex);

                                    ExtrusionEvent reverseEvent = new ExtrusionEvent();
                                    reverseEvent.setComment("Synthesised extrusion for close");
                                    reverseEvent.setE(finalExtrusionEvent.getE());
                                    reverseEvent.setD(finalExtrusionEvent.getD());
                                    reverseEvent.setX(previousTravelEvent.getX());
                                    reverseEvent.setY(previousTravelEvent.getY());
                                    reverseEvent.setFeedRate(wipeFeedRate_mmPerMin);

                                    extrusionBuffer.add(reverseEvent);

                                    eventIndices.put(EventType.NOZZLE_CLOSE_START, finalExtrusionEventIndex + 1);
                                } else
                                {
                                    // Last ditch...
                                    // Close the nozzle at the end of the line...
                                    closeAtEndOfPath = true;
                                    NozzleCloseFullyEvent closeNozzle = new NozzleCloseFullyEvent();
                                    closeNozzle.setComment("Single element path - closing at end");
                                    closeNozzle.setLength(0);

                                    if (extrusionBuffer.get(extrusionBuffer.size() - 1) instanceof LayerChangeEvent)
                                    {
                                        extrusionBuffer.add(extrusionBuffer.size() - 1, closeNozzle);
                                    } else
                                    {
                                        extrusionBuffer.add(closeNozzle);
                                    }
                                }
                            } else
                            {
                                int nozzleCloseStartIndex = insertTravelAndClosePath(finalExtrusionEventIndex, "Move to start of wipe - partial open", false, lastInwardsMove);

                                eventIndices.put(EventType.NOZZLE_CLOSE_START, nozzleCloseStartIndex);

                                ExtrusionEvent finalExtrusionEvent = ((ExtrusionEvent) extrusionBuffer.get(finalExtrusionEventIndex));
                                nozzleCloseOverVolume = totalExtrusionForPath - (finalExtrusionEvent.getE() + finalExtrusionEvent.getD());
                            }
                        } else
                        {
                            //Full open and close
                            nozzleStartPosition = 1.0;
                            nozzleCloseOverVolume = Math.min(currentNozzle.getWipeVolume(), totalExtrusionForPath);

                            if (currentNozzle.getOpenOverVolume() > 0)
                            {
                                insertVolumeBreak(extrusionBuffer,
                                                  eventIndices,
                                                  EventType.NOZZLE_OPEN_END,
                                                  currentNozzle.getOpenOverVolume(),
                                                  comment,
                                                  FindEventDirection.FORWARDS_FROM_START);
                            }

//                        if (currentNozzle.getPreejectionVolume() > 0)
//                        {
//                            double preejectVolume = Math.min(currentNozzle.getPreejectionVolume(), totalExtrusionForPath - currentNozzle.getOpenOverVolume());
//
//                            if (preejectVolume > 0)
//                            {
//                                insertVolumeBreak(extrusionBuffer,
//                                                  eventIndices,
//                                                  EventType.PRE_CLOSE_STARVATION_START,
//                                                  preejectVolume,
//                                                  comment,
//                                                  FindEventDirection.BACKWARDS_FROM_END);
//                            }
//                        }
                            int nozzleCloseStartIndex = insertTravelAndClosePath(finalExtrusionEventIndex, "Move to start of wipe - full open", true, lastInwardsMove);

//                        double wipeVolume = Math.min(currentNozzle.getWipeVolume(), totalExtrusionForPath - currentNozzle.getEjectionVolume());
//
//                        if (currentNozzle.getWipeVolume() > 0)
//                        {
//                            if (wipeVolume > 0)
//                            {
//
//                                insertVolumeBreak(extrusionBuffer,
//                                                  eventIndices,
//                                                  EventType.WIPE_START,
//                                                  wipeVolume,
//                                                  comment,
//                                                  FindEventDirection.BACKWARDS_FROM_END);
//                            }
//                        } else
//                        {
//                            wipeVolume = 0;
//                        }
                            eventIndices.put(EventType.NOZZLE_CLOSE_START, nozzleCloseStartIndex);

//                        // Now the nozzle close starting point
//                        insertVolumeBreak(extrusionBuffer,
//                                          eventIndices,
//                                          EventType.NOZZLE_CLOSE_START,
//                                          currentNozzle.getEjectionVolume()
//                                          + wipeVolume,
//                                          comment,
//                                          FindEventDirection.BACKWARDS_FROM_END);
                        }
                    }
                } else
                {
                    // Infill 
                    if (totalExtrusionForPath >= currentNozzle.getOpenOverVolume()
                        + currentNozzle.getPreejectionVolume()
                        + currentNozzle.getEjectionVolume()
                        + currentNozzle.getWipeVolume())
                    {
                        //OK - we're go for a normal close   
                        nozzleStartPosition = 1.0;
                        nozzleCloseOverVolume = currentNozzle.getEjectionVolume();

                        if (currentNozzle.getOpenOverVolume() > 0)
                        {

                            insertVolumeBreak(extrusionBuffer,
                                              eventIndices,
                                              EventType.NOZZLE_OPEN_END,
                                              currentNozzle.getOpenOverVolume(),
                                              comment,
                                              FindEventDirection.FORWARDS_FROM_START);
                        }

                        // Calculate the wipe point (if we need to...)
                        if (currentNozzle.getWipeVolume() > 0)
                        {

                            insertVolumeBreak(extrusionBuffer,
                                              eventIndices,
                                              EventType.WIPE_START,
                                              currentNozzle.getWipeVolume(),
                                              comment,
                                              FindEventDirection.BACKWARDS_FROM_END);
                        }

                        // Calculate the pre-ejection point (if we need to...)
                        if (currentNozzle.getPreejectionVolume() > 0)
                        {
                            insertVolumeBreak(extrusionBuffer,
                                              eventIndices,
                                              EventType.PRE_CLOSE_STARVATION_START,
                                              currentNozzle.getPreejectionVolume()
                                              + currentNozzle.getEjectionVolume()
                                              + currentNozzle.getWipeVolume(),
                                              comment,
                                              FindEventDirection.BACKWARDS_FROM_END);
                        }

                        // Now the nozzle close starting point
                        insertVolumeBreak(extrusionBuffer,
                                          eventIndices,
                                          EventType.NOZZLE_CLOSE_START,
                                          currentNozzle.getEjectionVolume()
                                          + currentNozzle.getWipeVolume(),
                                          comment,
                                          FindEventDirection.BACKWARDS_FROM_END);

                        if (currentNozzle.getOpenAtMidPoint() > 0)
                        {
                            // Now the nozzle close break point
                            insertVolumeBreak(extrusionBuffer,
                                              eventIndices,
                                              EventType.NOZZLE_CLOSE_MIDPOINT,
                                              currentNozzle.getEjectionVolume() * (1
                                              - (currentNozzle.getMidPointPercent() / 100.0))
                                              + currentNozzle.getWipeVolume(),
                                              comment,
                                              FindEventDirection.BACKWARDS_FROM_END);
                        }
                    } else if (totalExtrusionForPath > 0)
                    {
                        //Keep the wipe volume until the minimum B is exceeded
                        double extrusionVolumeAfterWipe = totalExtrusionForPath
                            - currentNozzle.getWipeVolume();
                        double bValue = Math.min(1, extrusionVolumeAfterWipe
                                                 / currentNozzle.getEjectionVolume());

                        double minimumBEjectionVolume = currentNozzle.getEjectionVolume()
                            * currentNozzle.getPartialBMinimum();

                        if (bValue < currentNozzle.getPartialBMinimum())
                        {
                            //Shorten the wipe
                            double requiredWipeVolume = totalExtrusionForPath
                                - minimumBEjectionVolume;

                            if (requiredWipeVolume <= 0)
                            {
                                //Not enough volume for the wipe even at minimum B
                                nozzleStartPosition = currentNozzle.getPartialBMinimum();
                                nozzleCloseOverVolume = totalExtrusionForPath;

                                if (currentNozzle.getOpenOverVolume() > 0)
                                {
                                    NozzleChangeBValueEvent partialOpen = new NozzleChangeBValueEvent();
                                    partialOpen.setB(currentNozzle.getPartialBMinimum());
                                    int firstExtrusion = getNextExtrusionEventIndex(0);
                                    extrusionBuffer.add(firstExtrusion, partialOpen);
                                } else
                                {
                                    replaceOpenNozzleWithPartialOpen(currentNozzle.getPartialBMinimum());
                                }

                                int nozzleCloseStartIndex = getNextExtrusionEventIndex(0);
                                eventIndices.put(EventType.NOZZLE_CLOSE_START, nozzleCloseStartIndex);
                                extrusionBuffer.get(nozzleCloseStartIndex).setComment("Short path");
                                eventIndices.put(EventType.WIPE_START,
                                                 getPreviousExtrusionEventIndex(extrusionBuffer.size()
                                                     - 1));
                            } else
                            {
                                //We can use a shortened wipe with minimum B
                                nozzleStartPosition = currentNozzle.getPartialBMinimum();
                                nozzleCloseOverVolume = minimumBEjectionVolume;

                                if (currentNozzle.getOpenOverVolume() > 0)
                                {
                                    NozzleChangeBValueEvent partialOpen = new NozzleChangeBValueEvent();
                                    partialOpen.setB(currentNozzle.getPartialBMinimum());
                                    int firstExtrusion = getNextExtrusionEventIndex(0);
                                    extrusionBuffer.add(firstExtrusion, partialOpen);
                                } else
                                {
                                    replaceOpenNozzleWithPartialOpen(currentNozzle.getPartialBMinimum());
                                }

                                insertVolumeBreak(extrusionBuffer,
                                                  eventIndices,
                                                  EventType.WIPE_START,
                                                  requiredWipeVolume,
                                                  comment,
                                                  FindEventDirection.BACKWARDS_FROM_END);

                                int closeStartIndex = getNextExtrusionEventIndex(0);
                                eventIndices.put(EventType.NOZZLE_CLOSE_START, closeStartIndex);
                                extrusionBuffer.get(closeStartIndex).setComment("Shortened wipe volume");
                            }
                        } else
                        {
                            //Retain the full wipe but open partially and use a proportionately smaller ejection volume
                            nozzleStartPosition = bValue;
                            nozzleCloseOverVolume = Math.min(currentNozzle.getEjectionVolume(), extrusionVolumeAfterWipe);

                            if (currentNozzle.getOpenOverVolume() > 0)
                            {
                                NozzleChangeBValueEvent partialOpen = new NozzleChangeBValueEvent();
                                partialOpen.setB(bValue);
                                int firstExtrusion = getNextExtrusionEventIndex(0);
                                extrusionBuffer.add(firstExtrusion, partialOpen);
                            } else
                            {
                                replaceOpenNozzleWithPartialOpen(bValue);
                            }

                            if (currentNozzle.getWipeVolume() > 0)
                            {
                                insertVolumeBreak(
                                    extrusionBuffer,
                                    eventIndices,
                                    EventType.WIPE_START,
                                    currentNozzle.getWipeVolume(),
                                    comment,
                                    FindEventDirection.BACKWARDS_FROM_END);
                            }

                            int nozzleCloseStartIndex = insertVolumeBreak(extrusionBuffer,
                                                                          eventIndices,
                                                                          EventType.NOZZLE_CLOSE_START,
                                                                          nozzleCloseOverVolume
                                                                          + currentNozzle.getWipeVolume(),
                                                                          comment,
                                                                          FindEventDirection.BACKWARDS_FROM_END);

                            if (compareDouble(nozzleCloseOverVolume, currentNozzle.getEjectionVolume()) == EQUAL)
                            {
                                extrusionBuffer.get(nozzleCloseStartIndex).setComment(
                                    "Full open - full eject volume full wipe volume");
                            } else
                            {
                                extrusionBuffer.get(nozzleCloseStartIndex).setComment(
                                    "Partial open - full wipe volume");
                            }
                        }
                    }
                }
            }

            if (eventIndices.containsKey(EventType.NOZZLE_CLOSE_START) || closeAtEndOfPath)
            {
                {
                    int foundRetractDuringExtrusion = -1;
                    int foundNozzleChange = -1;
                    double currentNozzlePosition = nozzleStartPosition;
                    double currentFeedrate = 0;

                    int minimumSearchIndex = 0;

                    if (eventIndices.containsKey(EventType.WIPE_START))
                    {
                        minimumSearchIndex = eventIndices.get(EventType.WIPE_START);
                    }

                    for (int tSearchIndex = extrusionBuffer.size() - 1;
                        tSearchIndex > minimumSearchIndex; tSearchIndex--)
                    {

                        GCodeParseEvent event = extrusionBuffer.get(
                            tSearchIndex);
                        if (event instanceof RetractDuringExtrusionEvent
                            && foundRetractDuringExtrusion < 0)
                        {
                            foundRetractDuringExtrusion = tSearchIndex;
                        }

                        if (event instanceof NozzleChangeEvent
                            && foundRetractDuringExtrusion >= 0)
                        {
                            foundNozzleChange = tSearchIndex;
                            break;
                        }
                    }

                    if (foundNozzleChange >= 0
                        && foundRetractDuringExtrusion >= 0)
                    {
                        NozzleChangeEvent eventToMove = (NozzleChangeEvent) extrusionBuffer.get(
                            foundNozzleChange);
                        extrusionBuffer.remove(foundNozzleChange);
                        extrusionBuffer.add(foundRetractDuringExtrusion,
                                            eventToMove);
                    }

                    int nozzleOpenEndIndex = -1;
                    if (eventIndices.containsKey(EventType.NOZZLE_OPEN_END))
                    {
                        nozzleOpenEndIndex = eventIndices.get(
                            EventType.NOZZLE_OPEN_END);

                        currentNozzlePosition = 0;
                    }

                    int preCloseStarveIndex = -1;
                    if (eventIndices.containsKey(EventType.PRE_CLOSE_STARVATION_START))
                    {
                        preCloseStarveIndex = eventIndices.get(
                            EventType.PRE_CLOSE_STARVATION_START);
                    }

                    int nozzleCloseStartIndex = -1;
                    if (eventIndices.containsKey(EventType.NOZZLE_CLOSE_START))
                    {
                        nozzleCloseStartIndex = eventIndices.get(EventType.NOZZLE_CLOSE_START);
                    }

                    int nozzleCloseMidpointIndex = -1;
                    if (eventIndices.containsKey(EventType.NOZZLE_CLOSE_MIDPOINT))
                    {
                        nozzleCloseMidpointIndex = eventIndices.get(
                            EventType.NOZZLE_CLOSE_MIDPOINT);
                    }

                    int wipeIndex = -1;
                    if (eventIndices.containsKey(EventType.WIPE_START))
                    {
                        wipeIndex = eventIndices.get(EventType.WIPE_START);
                    }

                    for (int eventWriteIndex = 0; eventWriteIndex
                        < extrusionBuffer.size(); eventWriteIndex++)
                    {
                        GCodeParseEvent candidateevent = extrusionBuffer.get(
                            eventWriteIndex);

                        if (candidateevent.getFeedRate() > 0)
                        {
                            currentFeedrate = candidateevent.getFeedRate();
                        }

                        if (candidateevent.getLength() > 0
                            && currentFeedrate > 0)
                        {
                            double timePerEvent = candidateevent.getLength()
                                / currentFeedrate * 60d;
                            predictedDurationInLayer += timePerEvent;
                            distanceSoFarInLayer += candidateevent.getLength();
                        }

                        if (candidateevent instanceof RetractEvent)
                        {
                            volumeUsed += ((RetractEvent) candidateevent).getE();
                        } else if (candidateevent instanceof UnretractEvent)
                        {
                            volumeUsed += ((UnretractEvent) candidateevent).getE();
                        } else if (candidateevent instanceof RetractDuringExtrusionEvent)
                        {
                            volumeUsed += ((RetractDuringExtrusionEvent) candidateevent).getE();
                        }

                        if (candidateevent instanceof LayerChangeEvent)
                        {
                            if (mixExtruderOutputs)
                            {
                                if (layer == mixFromLayer)
                                {
                                    currentEMixValue = startingEMixValue;
                                } else if (layer == mixToLayer)
                                {
                                    currentEMixValue = endEMixValue;

                                    if (currentMixPoint
                                        < extruderMixPoints.size() - 1)
                                    {
                                        ExtruderMix firstMixPoint = extruderMixPoints.get(
                                            currentMixPoint);
                                        startingEMixValue = firstMixPoint.getEFactor();
                                        startingDMixValue = firstMixPoint.getDFactor();
                                        mixFromLayer = firstMixPoint.getLayerNumber();

                                        currentMixPoint++;
                                        ExtruderMix secondMixPoint = extruderMixPoints.get(
                                            currentMixPoint);
                                        endEMixValue = secondMixPoint.getEFactor();
                                        endDMixValue = secondMixPoint.getDFactor();
                                        mixToLayer = secondMixPoint.getLayerNumber();
                                    }
                                } else if (layer > mixFromLayer && layer
                                    < mixToLayer)
                                {
                                    // Mix the values
                                    int layerSpan = mixToLayer
                                        - mixFromLayer;
                                    double layerRatio = (layer
                                        - mixFromLayer) / (double) layerSpan;
                                    double eSpan = endEMixValue
                                        - startingEMixValue;
                                    double dSpan = endDMixValue
                                        - startingDMixValue;
                                    currentEMixValue = startingEMixValue
                                        + (layerRatio * eSpan);
                                }
                                currentDMixValue = 1 - currentEMixValue;
                            }

                            layerIndex++;
                            layerNumberToLineNumber.add(layerIndex,
                                                        outputWriter.getNumberOfLinesOutput());
                            layerNumberToDistanceTravelled.add(layerIndex,
                                                               distanceSoFarInLayer);
                            layerNumberToPredictedDuration.add(layerIndex,
                                                               predictedDurationInLayer);
                            distanceSoFarInLayer = 0;
                            predictedDurationInLayer = 0;

                        }

                        if (candidateevent instanceof ExtrusionEvent)
                        {
                            ExtrusionEvent event = (ExtrusionEvent) candidateevent;

                            if (eventWriteIndex == wipeIndex
                                && eventWriteIndex == nozzleCloseStartIndex)
                            {
                                // No extrusion
                                // Proportional B value
                                NozzlePositionChangeEvent nozzleEvent = new NozzlePositionChangeEvent();
                                nozzleEvent.setX(event.getX());
                                nozzleEvent.setY(event.getY());
                                nozzleEvent.setLength(event.getLength());
                                nozzleEvent.setFeedRate(event.getFeedRate());
                                nozzleEvent.setComment(
                                    event.getComment()
                                    + " after start of close");
                                nozzleStartPosition = 0;
                                nozzleEvent.setB(0);
                                nozzleEvent.setNoExtrusionFlag(true);
                                // Set E and D so we have a record of the elided extrusion
                                nozzleEvent.setE(event.getE());
                                nozzleEvent.setD(event.getD());

                                writeEventToFile(nozzleEvent);
                                if (mixExtruderOutputs)
                                {
                                    autoUnretractEValue += event.getE()
                                        * currentEMixValue;
                                    autoUnretractDValue += event.getE()
                                        * currentDMixValue;
                                } else
                                {
                                    autoUnretractEValue += event.getE() + event.getD();
                                }
                            } else if (eventWriteIndex <= nozzleOpenEndIndex
                                && nozzleOpenEndIndex != -1)
                            {
                                // Normal extrusion plus auto unretract
                                // Proportional B value
                                NozzlePositionChangeEvent nozzleEvent = new NozzlePositionChangeEvent();
                                nozzleEvent.setX(event.getX());
                                nozzleEvent.setY(event.getY());
                                nozzleEvent.setLength(event.getLength());
                                nozzleEvent.setFeedRate(event.getFeedRate());

                                nozzleEvent.setComment("Normal open");
                                currentNozzlePosition = currentNozzlePosition
                                    + (event.getE()
                                    / currentNozzle.getOpenOverVolume());

                                if (compareDouble(currentNozzlePosition, 1)
                                    == EQUAL
                                    || currentNozzlePosition > 1)
                                {
                                    currentNozzlePosition = 1;
                                }
                                nozzleEvent.setB(currentNozzlePosition);
                                nozzleEvent.setE(event.getE());
                                nozzleEvent.setD(event.getD());
                                writeEventToFile(nozzleEvent);
                            } else if (wipeIndex != -1 && eventWriteIndex >= wipeIndex)
                            {
                                outputNoBNoE(event, "Wipe");
                            } else if ((nozzleCloseStartIndex >= 0 && eventWriteIndex >= nozzleCloseStartIndex)
                                && (nozzleCloseMidpointIndex == -1
                                || eventWriteIndex < nozzleCloseMidpointIndex))
                            {
                                // No extrusion
                                // Proportional B value
                                NozzlePositionChangeEvent nozzleEvent = new NozzlePositionChangeEvent();
                                nozzleEvent.setX(event.getX());
                                nozzleEvent.setY(event.getY());
                                nozzleEvent.setLength(event.getLength());
                                nozzleEvent.setFeedRate(event.getFeedRate());

                                if (nozzleCloseMidpointIndex == -1)
                                {
                                    nozzleEvent.setComment(event.getComment() + " Normal close");
                                    currentNozzlePosition = currentNozzlePosition
                                        - (nozzleStartPosition * (event.getE()
                                        / nozzleCloseOverVolume));
                                } else
                                {
                                    nozzleEvent.setComment(event.getComment() + " Differential close - part 1");
                                    currentNozzlePosition = currentNozzlePosition
                                        - (nozzleStartPosition
                                        * (1
                                        - currentNozzle.getOpenAtMidPoint()) * (event.getE()
                                        / (nozzleCloseOverVolume
                                        * (currentNozzle.getMidPointPercent() / 100.0))));
                                }
                                if (compareDouble(currentNozzlePosition, 0)
                                    == EQUAL
                                    || currentNozzlePosition < 0)
                                {
                                    currentNozzlePosition = 0;
                                }
                                nozzleEvent.setB(currentNozzlePosition);
                                nozzleEvent.setNoExtrusionFlag(true);
                                // Set E and D so we have a record of the elided extrusion
                                nozzleEvent.setE(event.getE());
                                nozzleEvent.setD(event.getD());

                                writeEventToFile(nozzleEvent);
                                if (mixExtruderOutputs)
                                {
                                    autoUnretractEValue += event.getE()
                                        * currentEMixValue;
                                    autoUnretractDValue += event.getE()
                                        * currentDMixValue;
                                } else
                                {
                                    autoUnretractEValue += event.getE() + event.getD();
                                }
                            } else if (nozzleCloseMidpointIndex != -1
                                && eventWriteIndex >= nozzleCloseMidpointIndex)
                            {
                                // No extrusion
                                // Proportional B value
                                NozzlePositionChangeEvent nozzleEvent = new NozzlePositionChangeEvent();
                                nozzleEvent.setX(event.getX());
                                nozzleEvent.setY(event.getY());
                                nozzleEvent.setLength(event.getLength());
                                nozzleEvent.setFeedRate(event.getFeedRate());
                                nozzleEvent.setComment("Differential close - part 2");
                                currentNozzlePosition = currentNozzlePosition
                                    - (nozzleStartPosition * currentNozzle.getOpenAtMidPoint()
                                    * (event.getE()
                                    / (nozzleCloseOverVolume * (1
                                    - (currentNozzle.getMidPointPercent() / 100.0)))));
                                if (compareDouble(currentNozzlePosition, 0)
                                    == EQUAL
                                    || currentNozzlePosition < 0)
                                {
                                    currentNozzlePosition = 0;
                                }
                                nozzleEvent.setB(currentNozzlePosition);
                                nozzleEvent.setNoExtrusionFlag(true);
                                // Set E and D so we have a record of the elided extrusion
                                nozzleEvent.setE(event.getE());
                                nozzleEvent.setD(event.getD());

                                writeEventToFile(nozzleEvent);
                                if (mixExtruderOutputs)
                                {
                                    autoUnretractEValue += event.getE()
                                        * currentEMixValue;
                                    autoUnretractDValue += event.getE()
                                        * currentDMixValue;
                                } else
                                {
                                    autoUnretractEValue += event.getE() + event.getD();
                                }
                            } else if (preCloseStarveIndex != -1
                                && eventWriteIndex >= preCloseStarveIndex)
                            {
                                outputNoBNoE(event, "Pre-close starvation - eliding " + event.getE() + event.getD());
                            } else
                            {
                                volumeUsed += event.getE();
                                event.setD(event.getE() * currentDMixValue);
                                event.setE(event.getE() * currentEMixValue);
                                writeEventToFile(event);
                            }
                        } else
                        {
                            writeEventToFile(candidateevent);
                            if (candidateevent instanceof NozzleChangeEvent)
                            {
                                Nozzle newNozzle = nozzles.get(
                                    ((NozzleChangeEvent) candidateevent).getNozzleNumber());
                                currentNozzle = newNozzle;
                            }
                        }
                    }
                }

                extrusionBuffer.clear();

                currentNozzle.closeNozzleFully();

            } else if (extrusionBuffer.size() > 0 && containsExtrusionEvents(extrusionBuffer))
            {
                CommentEvent failureComment = new CommentEvent();
                failureComment.setComment(
                    "Error locating start / end of close");
                writeEventToFile(failureComment);
                throw new PostProcessingError("Didn't locate start / end of close");
            } else
            {
                // Pass through the events - no extrusions to deal with
                for (GCodeParseEvent event : extrusionBuffer)
                {
                    writeEventToFile(event);
                    if (event instanceof NozzleChangeEvent)
                    {
                        Nozzle newNozzle = nozzles.get(
                            ((NozzleChangeEvent) event).getNozzleNumber());
                        currentNozzle = newNozzle;
                    }
                }
            }
        }
    }

    private int insertTravelAndClosePath(final int finalExtrusionEventIndex, final String comment, boolean forceReverse, TravelEvent lastInwardsMove) throws PostProcessingError
    {
        boolean reverseWipePath = forceReverse;

        int startOfClose = -1;

        final String perimeterKey = "perimeter";

        ExtrusionEvent finalExtrusionEvent = (ExtrusionEvent) extrusionBuffer.get(finalExtrusionEventIndex);
        Vector2D endOfExtrusion = new Vector2D(finalExtrusionEvent.getX(), finalExtrusionEvent.getY());
        int penultimateExtrusionEventIndex = getPreviousExtrusionEventIndex(finalExtrusionEventIndex);

        // Fairly horrible, but we're going to scan through the events to find the start of the perimeter
        // by looking for the ketword - this is because Slic3r chops up the perimeter into pieces in some circumstances.
        int startOfPerimeter = -1;
        int endOfPenultimatePerimeterSegment = -1;
        int closestEventIndex = -1;

        boolean finalExtrusionWasPerimeter = false;

        int indexToCopyFrom = -1;
        int minimumIndexToCopyFrom = 0;

        if (reverseWipePath == false && penultimateExtrusionEventIndex >= 0)
        {
            String finalExtrusionEventComment = finalExtrusionEvent.getComment();

            if (finalExtrusionEventComment != null)
            {
                if (finalExtrusionEventComment.contains(perimeterKey))
                {
                    finalExtrusionWasPerimeter = true;

                    // We can't go back over a layer boundary to wipe...
                    int lastLayerChangeIndex = getPreviousEventIndex(finalExtrusionEventIndex, LayerChangeEvent.class
                    );

                    if (lastLayerChangeIndex
                        <= 0)
                    {
                        lastLayerChangeIndex = 0;
                    }

                    Segment orthogonalSegment = null;
                    Vector2D lastPointConsidered = null;

                    Comparator<Double> intersectedPointComparator = new Comparator<Double>()
                    {
                        @Override
                        public int compare(Double t, Double t1)
                        {
                            return compareDouble(t, t1);
                        }
                    };
                    TreeMap<Double, Integer> intersectedPointDistances = new TreeMap<>();

                    int intersectionCounter = 0;
                    int maxNumberOfIntersectionsToConsider = currentSettings.perimetersProperty().get();
                    float maxDistanceFromEndPoint = currentSettings.getPerimeter_extrusion_width().get() * maxNumberOfIntersectionsToConsider * 4;

                    // Attempt to use the inwards move to find the innermost perimeter
                    if (lastInwardsMove
                        != null)
                    {
                        Vector2D inwardsMoveEndPoint = new Vector2D(lastInwardsMove.getX(), lastInwardsMove.getY());
                        inwardsMoveEndPoint.scalarMultiply(4);
                        orthogonalSegment = new Segment(endOfExtrusion, inwardsMoveEndPoint, new Line(endOfExtrusion, inwardsMoveEndPoint, 1e-12));
                    }

                    for (int eventIndex = finalExtrusionEventIndex - 1;
                        eventIndex > lastLayerChangeIndex && intersectionCounter <= maxNumberOfIntersectionsToConsider;
                        eventIndex--)
                    {
                        if (extrusionBuffer.get(eventIndex) instanceof MovementEvent)
                        {
                            MovementEvent thisMovementEvent = (MovementEvent) extrusionBuffer.get(eventIndex);
                            Vector2D thisMovement = new Vector2D(thisMovementEvent.getX(), thisMovementEvent.getY());

                            // Default 
                            if (orthogonalSegment == null)
                            {
                                orthogonalSegment = MathUtils.getOrthogonalLineToLinePoints(maxDistanceFromEndPoint, thisMovement, endOfExtrusion);
                            } else if (extrusionBuffer.get(eventIndex) instanceof ExtrusionEvent && lastPointConsidered != null)
                            {
                                // Detect intersections
                                Segment segmentUnderConsideration = new Segment(lastPointConsidered, thisMovement, new Line(lastPointConsidered, thisMovement, 1e-12));
                                Vector2D intersectionPoint = MathUtils.getSegmentIntersection(orthogonalSegment, segmentUnderConsideration);
                                if (intersectionPoint != null)
                                {
                                    double distanceFromEndPoint = intersectionPoint.distance(endOfExtrusion);
                                    intersectedPointDistances.put(distanceFromEndPoint, eventIndex);
                                    intersectionCounter++;
                                }
                            }

                            lastPointConsidered = thisMovement;
                        }
                    }

                    if (intersectedPointDistances.size()
                        >= maxNumberOfIntersectionsToConsider)
                    {
                        closestEventIndex = (int) intersectedPointDistances.values().toArray()[maxNumberOfIntersectionsToConsider - 1];
                    } else if (intersectedPointDistances.size()
                        > 0)
                    {
                        closestEventIndex = (int) intersectedPointDistances.values().toArray()[intersectedPointDistances.size() - 1];
                    }

                    if (closestEventIndex < 0)
                    {
                        steno.warning("Couldn't find closest point to end of line when analysing inward wipe - defaulting to reverse");
                        finalExtrusionWasPerimeter = false;
                    } else
                    {
                        indexToCopyFrom = closestEventIndex;
                    }
                }
            }

//
//                    for (int eventScanIndex = lastLayerChangeIndex; eventScanIndex < finalExtrusionEventIndex; eventScanIndex++)
//                    {
//                        if (extrusionBuffer.get(eventScanIndex).getClass() == ExtrusionEvent.class)
//                        {
//                            ExtrusionEvent eventToAnalyse = (ExtrusionEvent) extrusionBuffer.get(eventScanIndex);
//                            String commentToAnalyse = eventToAnalyse.getComment();
//                            if (commentToAnalyse
//                                != null)
//                            {
//                                if (commentToAnalyse.contains(perimeterKey))
//                                {
//                                    startOfPerimeter = eventScanIndex;
//                                    break;
//                                }
//                            }
//                        }
//
//                    }
//
//                    if (startOfPerimeter >= 0)
//                    {
//                        int travelBeforeFinalExtrusionIndex = getPreviousEventIndex(finalExtrusionEventIndex, TravelEvent.class
//                        );
//
//                        if (travelBeforeFinalExtrusionIndex
//                            > 0)
//                        {
//                            for (int eventScanIndex = travelBeforeFinalExtrusionIndex - 1; eventScanIndex > 0; eventScanIndex--)
//                            {
//                                if (extrusionBuffer.get(eventScanIndex).getClass() == ExtrusionEvent.class)
//                                {
//                                    ExtrusionEvent eventToAnalyse = (ExtrusionEvent) extrusionBuffer.get(eventScanIndex);
//                                    String commentToAnalyse = eventToAnalyse.getComment();
//                                    if (commentToAnalyse != null)
//                                    {
//                                        if (commentToAnalyse.contains(perimeterKey))
//                                        {
//                                            endOfPenultimatePerimeterSegment = eventScanIndex;
//                                            break;
//                                        }
//                                    }
//                                }
//                            }
//
//                            if (endOfPenultimatePerimeterSegment > 0)
//                            {
//                                //Success!
//                                // We can now look for the closest point
//                                double smallestDistance = Double.MAX_VALUE;
//                                for (int eventIndex = startOfPerimeter; eventIndex <= endOfPenultimatePerimeterSegment; eventIndex++)
//                                {
//                                    if (extrusionBuffer.get(eventIndex) instanceof ExtrusionEvent)
//                                    {
//                                        ExtrusionEvent eventBeingConsidered = (ExtrusionEvent) extrusionBuffer.get(eventIndex);
//                                        Vector2D pointBeingConsidered = new Vector2D(eventBeingConsidered.getX(), eventBeingConsidered.getY());
//
//                                        double distance = endOfExtrusion.distance(pointBeingConsidered);
//
//                                        if (distance < smallestDistance)
//                                        {
//                                            closestEventIndex = eventIndex;
//                                            smallestDistance = distance;
//                                        }
//                                    }
//                                }
//                            }
//
//                            if (closestEventIndex < 0)
//                            {
//                                steno.warning("Couldn't find closest point to end of line when analysing inward wipe - defaulting to reverse");
//                                finalExtrusionWasPerimeter = false;
//                            } else
//                            {
//                                if (closestEventIndex == endOfPenultimatePerimeterSegment)
//                                {
//                                    closestEventIndex = startOfPerimeter;
//                                    indexToCopyFrom = startOfPerimeter + 1;
//                                }
//                            }
//                        } else
//                        {
//                            closestEventIndex = startOfPerimeter;
//                        }
//                    } else
//                    {
//                        steno.warning("Couldn't determine start of extrusion when analysing inward wipe - defaulting to reverse - single extrusion path?");
//                        finalExtrusionWasPerimeter = false;
//                    }
//                }
//            }
        }

        int insertedEventIndex = finalExtrusionEventIndex + 1;

        String wipeTypeComment = "";

        if (finalExtrusionWasPerimeter)
        {
            wipeTypeComment = "Wipe - inwards";

            reverseWipePath = false;

            // Add a travel to the closest point
            TravelEvent travelToClosestPoint = new TravelEvent();
            ExtrusionEvent closestEvent = (ExtrusionEvent) extrusionBuffer.get(closestEventIndex);
            travelToClosestPoint.setX(closestEvent.getX());
            travelToClosestPoint.setY(closestEvent.getY());
            travelToClosestPoint.setComment(comment);
            travelToClosestPoint.setFeedRate(wipeFeedRate_mmPerMin);

            extrusionBuffer.add(insertedEventIndex, travelToClosestPoint);
            insertedEventIndex++;

        } else
        {
            wipeTypeComment = "Wipe - reverse";

            reverseWipePath = true;
            indexToCopyFrom = finalExtrusionEventIndex - 1;
            minimumIndexToCopyFrom = getNextExtrusionEventIndex(0);
        }

        startOfClose = insertedEventIndex;

        double cumulativeExtrusionVolume = 0;

        double targetVolume = currentNozzle.getEjectionVolume() + currentNozzle.getWipeVolume();

        MovementEvent lastMovement = null;

//        int indexDelta = (reverseWipePath == true) ? -1 : 1;
        int indexDelta = -1;

        while (cumulativeExtrusionVolume < targetVolume
            && indexToCopyFrom <= finalExtrusionEventIndex
            && indexToCopyFrom >= minimumIndexToCopyFrom)
        {
            if (extrusionBuffer.get(indexToCopyFrom) instanceof ExtrusionEvent)
            {
                ExtrusionEvent eventToCopy = (ExtrusionEvent) extrusionBuffer.get(indexToCopyFrom);

                double segmentVolume = eventToCopy.getE() + eventToCopy.getD();
                double volumeDifference = targetVolume - cumulativeExtrusionVolume - segmentVolume;

                if (volumeDifference < 0)
                {
                    double requiredSegmentVolume = segmentVolume + volumeDifference;
                    double segmentAlterationRatio = requiredSegmentVolume / segmentVolume;

                    ExtrusionEvent eventToInsert = new ExtrusionEvent();
                    eventToInsert.setE(eventToCopy.getE() * segmentAlterationRatio);
                    eventToInsert.setD(eventToCopy.getD() * segmentAlterationRatio);
                    eventToInsert.setFeedRate(wipeFeedRate_mmPerMin);

                    Vector2D fromPosition = null;

                    Vector2D fromReferencePosition = null;

                    if (reverseWipePath)
                    {
                        fromReferencePosition = getNextPosition(indexToCopyFrom, finalExtrusionEventIndex);
                    } else
                    {
                        fromReferencePosition = getLastPosition(indexToCopyFrom);
                    }

                    if (fromReferencePosition != null)
                    {
                        fromPosition = fromReferencePosition;
                    } else
                    {
                        throw new PostProcessingError("Couldn't locate from position for auto wipe");
                    }

                    Vector2D toPosition = new Vector2D(eventToCopy.getX(),
                                                       eventToCopy.getY());

                    Vector2D actualVector = toPosition.subtract(fromPosition);
                    Vector2D firstSegment = fromPosition.add(segmentAlterationRatio,
                                                             actualVector);

                    eventToInsert.setX(firstSegment.getX());
                    eventToInsert.setY(firstSegment.getY());
                    eventToInsert.setComment(wipeTypeComment + " end");

                    extrusionBuffer.add(insertedEventIndex, eventToInsert);
                    cumulativeExtrusionVolume += requiredSegmentVolume;
                    lastMovement = eventToInsert;
                } else
                {
                    ExtrusionEvent eventToInsert = new ExtrusionEvent();
                    eventToInsert.setE(eventToCopy.getE());
                    eventToInsert.setD(eventToCopy.getD());
                    eventToInsert.setX(eventToCopy.getX());
                    eventToInsert.setY(eventToCopy.getY());
                    eventToInsert.setComment(wipeTypeComment + " start");
                    eventToInsert.setFeedRate(wipeFeedRate_mmPerMin);

                    extrusionBuffer.add(insertedEventIndex, eventToInsert);
                    cumulativeExtrusionVolume += eventToCopy.getE() + eventToCopy.getD();
                    lastMovement = eventToInsert;
                }
            } else
            {
                GCodeParseEvent event = extrusionBuffer.get(indexToCopyFrom);

                if (event instanceof TravelEvent)
                {
                    lastMovement = (TravelEvent) event;

                    TravelEvent eventToCopy = (TravelEvent) event;
                    TravelEvent eventToInsert = new TravelEvent();
                    eventToInsert.setX(eventToCopy.getX());
                    eventToInsert.setY(eventToCopy.getY());
                    eventToInsert.setComment(eventToCopy.getComment());
                    eventToInsert.setFeedRate(wipeFeedRate_mmPerMin);

                    extrusionBuffer.add(insertedEventIndex, eventToInsert);
                } else
                {
                    extrusionBuffer.add(insertedEventIndex, extrusionBuffer.get(indexToCopyFrom));
                }
            }

            insertedEventIndex++;

            indexToCopyFrom += indexDelta;
        }

        return startOfClose;
    }

    private void outputNoBNoE(ExtrusionEvent event, String comment)
    {
        // No extrusion
        // No B
        TravelEvent noBNoETravel = new TravelEvent();
        noBNoETravel.setX(event.getX());
        noBNoETravel.setY(event.getY());
        noBNoETravel.setLength(event.getLength());
        noBNoETravel.setFeedRate(event.getFeedRate());
        noBNoETravel.setComment(comment);
        writeEventToFile(noBNoETravel);
        if (mixExtruderOutputs)
        {
            autoUnretractEValue += event.getE()
                * currentEMixValue;
            autoUnretractDValue += event.getE()
                * currentDMixValue;
        } else
        {
            autoUnretractEValue += event.getE();
        }
    }

    private boolean containsExtrusionEvents(ArrayList<GCodeParseEvent> buffer)
    {
        boolean foundExtrusionEvent = false;
        for (GCodeParseEvent event : buffer)
        {
            if (event instanceof ExtrusionEvent)
            {
                foundExtrusionEvent = true;
                break;
            }
        }
        return foundExtrusionEvent;
    }

    private int getNextExtrusionEventIndex(int startingIndex)
    {
        int indexOfEvent = -1;

        for (int index = startingIndex; index < extrusionBuffer.size(); index++)
        {
            if (extrusionBuffer.get(index) instanceof ExtrusionEvent)
            {
                indexOfEvent = index;
                break;
            }
        }
        return indexOfEvent;
    }

    private int getNextEventIndex(int startingIndex, Class<?> eventClass)
    {
        int indexOfEvent = -1;

        for (int index = startingIndex; index < extrusionBuffer.size(); index++)
        {
            if (extrusionBuffer.get(index).getClass() == eventClass)
            {
                indexOfEvent = index;
                break;
            }
        }
        return indexOfEvent;
    }

    private int getPreviousExtrusionEventIndex(int startingIndex)
    {
        int indexOfEvent = -1;

        for (int index = startingIndex; index >= 0; index--)
        {
            if (extrusionBuffer.get(index) instanceof ExtrusionEvent)
            {
                indexOfEvent = index;
                break;
            }
        }
        return indexOfEvent;
    }

    private int getPreviousEventIndex(int startingIndex, Class<?> eventClass)
    {
        int indexOfEvent = -1;

        for (int index = startingIndex; index >= 0; index--)
        {
            if (extrusionBuffer.get(index).getClass() == eventClass)
            {
                indexOfEvent = index;
                break;
            }
        }
        return indexOfEvent;
    }

    private boolean replaceOpenNozzleWithPartialOpen(double partialOpenValue)
    {
        boolean success = false;

        int eventSearchIndex = extrusionBuffer.size() - 1;
        while (eventSearchIndex >= 0)
        {
            if (extrusionBuffer.get(eventSearchIndex) instanceof NozzleOpenFullyEvent)
            {
                NozzleChangeBValueEvent newBEvent = new NozzleChangeBValueEvent();
                newBEvent.setB(partialOpenValue);
                newBEvent.setComment("Partial open");

                extrusionBuffer.add(eventSearchIndex + 1, newBEvent);
                extrusionBuffer.remove(eventSearchIndex);
                success = true;
                break;
            }

            eventSearchIndex--;
        }

        return success;
    }

    private Vector2D getLastPosition(int eventIndex)
    {
        Vector2D position = null;

        for (int index = eventIndex - 1; index >= 0; index--)
        {
            if (extrusionBuffer.get(index) instanceof MovementEvent)
            {
                MovementEvent event = (MovementEvent) extrusionBuffer.get(index);
                position = new Vector2D(event.getX(), event.getY());
                break;
            }
        }

        return position;
    }

    private Vector2D getNextPosition(int eventIndex, int indexMax)
    {
        Vector2D position = null;

        for (int index = eventIndex; index <= indexMax; index++)
        {
            if (extrusionBuffer.get(index) instanceof MovementEvent)
            {
                MovementEvent event = (MovementEvent) extrusionBuffer.get(index);
                position = new Vector2D(event.getX(), event.getY());
                break;
            }
        }

        return position;
    }

    private int insertVolumeBreak(ArrayList<GCodeParseEvent> buffer,
        Map<EventType, Integer> eventIndices,
        EventType eventType,
        double requiredEjectionVolume,
        String comment,
        FindEventDirection findEventDirection)
    {
        int volumeIndex = -1;
        int eventIndex;
        double volumeConsidered = 0;

        if (findEventDirection == FindEventDirection.BACKWARDS_FROM_END)
        {
            eventIndex = buffer.size() - 1;
            while (eventIndex >= 0)
            {
                if (buffer.get(eventIndex) instanceof ExtrusionEvent)
                {
                    ExtrusionEvent currentEvent = (ExtrusionEvent) buffer.get(eventIndex);

                    double segmentExtrusion = currentEvent.getE();
                    volumeConsidered += segmentExtrusion;

                    if (volumeIndex == -1)
                    {
                        if (compareDouble(volumeConsidered,
                                          requiredEjectionVolume) == EQUAL)
                        {
                            // No need to split line - replace the current event with a nozzle change event
                            volumeIndex = eventIndex;
                            eventIndices.put(eventType, volumeIndex);
                            break;
                        } else if (compareDouble(volumeConsidered,
                                                 requiredEjectionVolume)
                            == MORE_THAN)
                        {
                            // Split the line
                            double initialSegmentExtrusion = volumeConsidered
                                - requiredEjectionVolume;
                            double scaleFactor = initialSegmentExtrusion
                                / segmentExtrusion;

                            Vector2D fromPosition = null;

                            if (eventIndex > 0)
                            {
                                Vector2D lastPosition = getLastPosition(eventIndex);
                                if (lastPosition != null)
                                {
                                    fromPosition = lastPosition;
                                } else
                                {
                                    fromPosition = nozzleLastClosedAt;
                                }
                            } else
                            {
                                fromPosition = nozzleLastClosedAt;
                            }

                            Vector2D toPosition = new Vector2D(currentEvent.getX(),
                                                               currentEvent.getY());

//                            steno.debug("Vector from " + fromPosition + " to " + toPosition);
                            Vector2D actualVector = toPosition.subtract(fromPosition);
                            Vector2D firstSegment = fromPosition.add(scaleFactor,
                                                                     actualVector);

                            ExtrusionEvent firstSegmentExtrusionEvent = new ExtrusionEvent();
                            firstSegmentExtrusionEvent.setX(firstSegment.getX());
                            firstSegmentExtrusionEvent.setY(firstSegment.getY());
                            firstSegmentExtrusionEvent.setE(segmentExtrusion
                                * scaleFactor);
                            firstSegmentExtrusionEvent.setLength(
                                ((ExtrusionEvent) currentEvent).getLength()
                                * scaleFactor);
                            firstSegmentExtrusionEvent.setFeedRate(
                                currentEvent.getFeedRate());
                            firstSegmentExtrusionEvent.setComment(comment);

                            ExtrusionEvent secondSegmentExtrusionEvent = new ExtrusionEvent();
                            secondSegmentExtrusionEvent.setX(currentEvent.getX());
                            secondSegmentExtrusionEvent.setY(currentEvent.getY());
                            secondSegmentExtrusionEvent.setE(
                                segmentExtrusion - firstSegmentExtrusionEvent.getE());
                            secondSegmentExtrusionEvent.setLength(
                                ((ExtrusionEvent) currentEvent).getLength() * (1
                                - scaleFactor));
                            secondSegmentExtrusionEvent.setFeedRate(
                                currentEvent.getFeedRate());
                            secondSegmentExtrusionEvent.setComment(comment);

                            for (Entry<EventType, Integer> eventEntry : eventIndices.entrySet())
                            {
                                if (eventEntry.getValue() > eventIndex)
                                {
                                    eventEntry.setValue(eventEntry.getValue() + 1);
                                }
                            }

                            buffer.add(eventIndex, firstSegmentExtrusionEvent);
                            buffer.remove(eventIndex + 1);
                            buffer.add(eventIndex + 1, secondSegmentExtrusionEvent);

                            volumeIndex = eventIndex + 1;
                            eventIndices.put(eventType, volumeIndex);
                            break;
                        }
                    }
                }
                eventIndex--;
            }
        } else
        {
            eventIndex = 0;
            while (eventIndex < buffer.size())
            {
                if (buffer.get(eventIndex) instanceof ExtrusionEvent)
                {
                    ExtrusionEvent currentEvent = (ExtrusionEvent) buffer.get(eventIndex);

                    double segmentExtrusion = currentEvent.getE();
                    volumeConsidered += segmentExtrusion;

                    if (volumeIndex == -1)
                    {
                        if (compareDouble(volumeConsidered,
                                          requiredEjectionVolume) == EQUAL)
                        {
                            // No need to split line - replace the current event with a nozzle change event
                            volumeIndex = eventIndex;
                            eventIndices.put(eventType, volumeIndex);
                            break;
                        } else if (compareDouble(volumeConsidered,
                                                 requiredEjectionVolume)
                            == MORE_THAN)
                        {
                            // Split the line
                            double secondSegmentExtrusion = volumeConsidered - requiredEjectionVolume;

                            double scaleFactor = 1 - (secondSegmentExtrusion
                                / segmentExtrusion);

                            Vector2D fromPosition = null;

                            if (eventIndex > 0)
                            {
                                Vector2D lastPosition = getLastPosition(eventIndex);
                                if (lastPosition != null)
                                {
                                    fromPosition = lastPosition;
                                } else
                                {
                                    fromPosition = nozzleLastOpenedAt;
                                }
                            } else
                            {
                                fromPosition = nozzleLastOpenedAt;
                            }

                            Vector2D toPosition = new Vector2D(currentEvent.getX(),
                                                               currentEvent.getY());
//                            steno.debug("Vector from " + fromPosition + " to " + toPosition);
                            Vector2D actualVector = toPosition.subtract(fromPosition);
                            Vector2D firstSegment = fromPosition.add(scaleFactor,
                                                                     actualVector);

                            ExtrusionEvent firstSegmentExtrusionEvent = new ExtrusionEvent();
                            firstSegmentExtrusionEvent.setX(firstSegment.getX());
                            firstSegmentExtrusionEvent.setY(firstSegment.getY());
                            firstSegmentExtrusionEvent.setE(segmentExtrusion
                                * scaleFactor);
                            firstSegmentExtrusionEvent.setLength(
                                ((ExtrusionEvent) currentEvent).getLength()
                                * scaleFactor);
                            firstSegmentExtrusionEvent.setFeedRate(
                                currentEvent.getFeedRate());
                            firstSegmentExtrusionEvent.setComment(comment);

                            ExtrusionEvent secondSegmentExtrusionEvent = new ExtrusionEvent();
                            secondSegmentExtrusionEvent.setX(currentEvent.getX());
                            secondSegmentExtrusionEvent.setY(currentEvent.getY());
                            secondSegmentExtrusionEvent.setE(secondSegmentExtrusion);
                            secondSegmentExtrusionEvent.setLength(
                                ((ExtrusionEvent) currentEvent).getLength() * (1
                                - scaleFactor));
                            secondSegmentExtrusionEvent.setFeedRate(
                                currentEvent.getFeedRate());
                            secondSegmentExtrusionEvent.setComment(comment);

                            for (Entry<EventType, Integer> eventEntry : eventIndices.entrySet())
                            {
                                if (eventEntry.getValue() > eventIndex)
                                {
                                    eventEntry.setValue(eventEntry.getValue() + 1);
                                }
                            }

                            buffer.add(eventIndex, firstSegmentExtrusionEvent);
                            buffer.remove(eventIndex + 1);
                            buffer.add(eventIndex + 1, secondSegmentExtrusionEvent);

                            volumeIndex = eventIndex;
                            eventIndices.put(eventType, volumeIndex);
                            break;
                        }
                    }
                }
                eventIndex++;
            }
        }

        return volumeIndex;
    }
}
