package celtech.gcodetranslator;

import celtech.configuration.slicer.NozzleParameters;
import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.SlicerType;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.events.CommentEvent;
import celtech.gcodetranslator.events.EndOfFileEvent;
import celtech.gcodetranslator.events.ExtrusionEvent;
import celtech.gcodetranslator.events.GCodeEvent;
import celtech.gcodetranslator.events.GCodeParseEvent;
import celtech.gcodetranslator.events.LayerChangeEvent;
import celtech.gcodetranslator.events.LayerChangeWithTravelEvent;
import celtech.gcodetranslator.events.LayerChangeWithoutTravelEvent;
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
import celtech.printerControl.comms.commands.MacroLoadException;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.utils.Math.MathUtils;
import static celtech.utils.Math.MathUtils.EQUAL;
import static celtech.utils.Math.MathUtils.MORE_THAN;
import static celtech.utils.Math.MathUtils.compareDouble;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
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
        "\\b(?:M106 S[0-9.]+|M107|G[0-9]{1,}.*|M[0-9]{2,})(?:[\\s]*;.*)?");
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

    protected ArrayList<NozzleProxy> nozzleProxies = null;
    protected NozzleProxy currentNozzle = null;

    private Tool selectedTool = Tool.Unknown;
    private double unretractFeedRate = 0;
    private double currentZHeight = 0;

    //Profile variables
    private double startClosingByMM = 2;

    private Vector2D lastPoint = null;
    private Vector2D nozzleLastOpenedAt = null;
    private MovementEvent lastProcessedMovementEvent = null;

    protected ExtrusionBuffer extrusionBuffer = new ExtrusionBuffer();

    private boolean triggerCloseFromTravel = true;
    private boolean triggerCloseFromRetract = true;

    private int tempNozzleMemory = 0;
    private int nozzleInUse = -1;
    private int forcedNozzleOnFirstLayer = -1;
    private boolean nozzleHasBeenForced = false;
    private boolean nozzleHasBeenReinstated = false;
    private static final int POINT_3MM_NOZZLE = 0;
    private static final int POINT_8MM_NOZZLE = 1;

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

    private GCodeOutputWriter outputWriter;
    private int layerIndex = 0;
    private List<Integer> layerNumberToLineNumber;
    private List<Double> layerNumberToDistanceTravelled;
    private List<Double> layerNumberToPredictedDuration;
    private double distanceSoFarInLayer = 0;
    private Integer lineNumberOfFirstExtrusion;

    protected SlicerParametersFile currentSettings = null;
    private int wipeFeedRate_mmPerMin = 0;

    // Causes home and return events to be inserted, triggering the camera
    private boolean movieMakerEnabled = false;

    protected SlicerType slicerType;

    // This counter is used to determine when to reselect the nozzle in use
    // When printing with a single nozzle it is possible for the home to be lost after many closes
    private int triggerNozzleReselectAfterNCloses = 0;
    private int closeCounter = 0;

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
        SlicerParametersFile settings, DoubleProperty percentProgress)
    {
        RoboxiserResult result = new RoboxiserResult();
        result.setSuccess(false);

        if (initialise(settings, outputFilename))
        {
            boolean success = false;

            try
            {
                SimpleDateFormat formatter = new SimpleDateFormat("EEE d MMM y HH:mm:ss", Locale.UK);
                outputWriter.writeOutput("; File post-processed by the CEL Tech Roboxiser on "
                    + formatter.format(new Date()) + "\n");
                outputWriter.
                    writeOutput("; " + ApplicationConfiguration.getTitleAndVersion() + "\n");

                outputWriter.writeOutput(";\n; Pre print gcode\n");
                for (String macroLine : GCodeMacros.getMacroContents("before_print"))
                {
                    outputWriter.writeOutput(macroLine + "\n");
                }
                outputWriter.writeOutput("; End of Pre print gcode\n");

                gcodeParser.parse(inputFilename, percentProgress);

                outputWriter.close();

                steno.info("Finished roboxising " + inputFilename);
                steno.info("Total extrusion volume " + totalExtrudedVolume + " mm3");
                steno.info("Total XY movement distance " + totalXYMovement + " mm");

                success = true;
            } catch (IOException ex)
            {
                steno.error("Error roboxising file " + inputFilename);
            } catch (MacroLoadException ex)
            {
                steno.error(
                    "Error roboxising file - couldn't add before print header due to circular macro reference "
                    + inputFilename);
            }

            result.setSuccess(success);
            /**
             * TODO: layerNumberToLineNumber uses lines numbers from the GCode file so are a little
             * less than the line numbers for each layer after roboxisation. As a quick fix for now
             * set the line number of the last layer to the actual maximum line number.
             */
            layerNumberToLineNumber.set(layerNumberToLineNumber.size() - 1, outputWriter.
                                        getNumberOfLinesOutput());
            PrintJobStatistics roboxisedStatistics = new PrintJobStatistics(
                outputWriter.getNumberOfLinesOutput(),
                volumeUsed, lineNumberOfFirstExtrusion,
                layerNumberToLineNumber, layerNumberToPredictedDuration);

            result.setRoboxisedStatistics(roboxisedStatistics);
        }

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

    protected boolean initialise(SlicerParametersFile settings, String outputFilename)
    {
        boolean initialised = false;

        currentSettings = settings;

        if (currentSettings.getSlicerOverride() != null)
        {
            slicerType = currentSettings.getSlicerOverride();
        } else
        {
            slicerType = Lookup.getUserPreferences().getSlicerType();
        }

        layerNumberToLineNumber = new ArrayList<>();
        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();
        layerNumberToDistanceTravelled.add(0, 0d);
        layerNumberToPredictedDuration.add(0, 0d);
        layerNumberToLineNumber.add(0, 0);

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

        initialTemperaturesWritten = false;
        subsequentLayersTemperaturesWritten = false;
        distanceSoFar = 0;
        totalExtrudedVolume = 0;
        totalXYMovement = 0;
        layer = 0;
        unretractFeedRate = 0;
        currentZHeight = 0;

        forcedNozzleOnFirstLayer = settings.getFirstLayerNozzle();

        nozzleProxies = new ArrayList<NozzleProxy>();

        for (int nozzleIndex = 0; nozzleIndex < settings.getNozzleParameters().size(); nozzleIndex++)
        {
            NozzleProxy proxy = new NozzleProxy(settings.getNozzleParameters().get(nozzleIndex));
            proxy.setNozzleReferenceNumber(nozzleIndex);
            nozzleProxies.add(proxy);
        }

        wipeFeedRate_mmPerMin = currentSettings.getPerimeterSpeed_mm_per_s() * 60;

        try
        {
            outputWriter = Lookup.getPostProcessorOutputWriterFactory().create(outputFilename);
            initialised = true;
        } catch (IOException ex)
        {
            steno.error("Failed to initialise post processor");
            ex.printStackTrace();
        }

        triggerNozzleReselectAfterNCloses = settings.getMaxClosesBeforeNozzleReselect();

        return initialised;
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
        if (((slicerType == SlicerType.Slic3r && layer == 1)
            || (slicerType == SlicerType.Cura && layer == 1))
            && subsequentLayersTemperaturesWritten == false)
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

        if (currentNozzle == null)
        {
            NozzleChangeEvent nozzleChangeEvent = new NozzleChangeEvent();
            nozzleChangeEvent.setNozzleNumber(POINT_3MM_NOZZLE);
            nozzleChangeEvent.setComment("Initialise using nozzle 0");
            extrusionBuffer.add(nozzleChangeEvent);
            currentNozzle = nozzleProxies.get(POINT_3MM_NOZZLE);
        }

        if (event instanceof ExtrusionEvent)
        {
            if (lineNumberOfFirstExtrusion == null)
            {
                lineNumberOfFirstExtrusion = event.getLinesSoFar();
            }

            if (((slicerType == SlicerType.Slic3r && layer == 2) || (slicerType == SlicerType.Cura
                && layer == 2)) && forcedNozzleOnFirstLayer >= 0 && nozzleHasBeenReinstated == false)
            {
                nozzleHasBeenReinstated = true;
                int nozzleToUse = chooseNozzleByTask(event);

                if (nozzleToUse >= 0)
                {
                    NozzleChangeEvent nozzleChangeEvent = new NozzleChangeEvent();
                    nozzleChangeEvent.setNozzleNumber(nozzleToUse);
                    nozzleChangeEvent.setComment("return to required nozzle");
                    extrusionBuffer.add(nozzleChangeEvent);
                    closeCounter = 0;

                    currentNozzle = nozzleProxies.get(nozzleToUse);
                } else
                {
                    steno.warning("Couldn't derive required nozzle to return to...");
                }
            } else if (autoGenerateNozzleChangeEvents && ((forcedNozzleOnFirstLayer >= 0
                && nozzleHasBeenReinstated)
                || forcedNozzleOnFirstLayer < 0))
            {
                int requiredNozzle = chooseNozzleByTask(event);

                if (currentNozzle != null && nozzleProxies.get(requiredNozzle) != currentNozzle)
                {
                    // Close the old nozzle
                    writeEventsWithNozzleClose("Closing last used nozzle");
                }

                if (currentNozzle == null || nozzleProxies.get(requiredNozzle) != currentNozzle)
                {
                    //Select the nozzle
                    NozzleChangeEvent nozzleChangeEvent = new NozzleChangeEvent();
                    nozzleChangeEvent.setComment("Selecting nozzle " + requiredNozzle);
                    nozzleChangeEvent.setNozzleNumber(requiredNozzle);
                    extrusionBuffer.add(nozzleChangeEvent);

                    currentNozzle = nozzleProxies.get(requiredNozzle);
                }
            }

            ExtrusionEvent extrusionEvent = (ExtrusionEvent) event;
            currentPoint = new Vector2D(extrusionEvent.getX(),
                                        extrusionEvent.getY());

            totalExtrudedVolume += extrusionEvent.getE() + extrusionEvent.getD();

            if (currentNozzle.getState() != NozzleState.OPEN
                && currentNozzle.getNozzleParameters().getOpenOverVolume() == 0)
            {
                // Unretract and open
                NozzleOpenFullyEvent openNozzle = new NozzleOpenFullyEvent();
                openNozzle.setComment("Open and replenish from extrusion");
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
                if (currentNozzle.getNozzleParameters().getOpenOverVolume() <= 0)
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

            if (unretractFeedRate > 0)
            {
                if (extrusionEvent.getFeedRate() <= 0)
                {
                    extrusionEvent.setFeedRate(unretractFeedRate);
                }
                unretractFeedRate = 0;
            }

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

                if (layer == 1
                    && currentNozzle.getNozzleParameters().getTravelBeforeForcedClose() > 0
                    && (currentNozzle.getState() != NozzleState.CLOSED
                    && distance
                    > currentNozzle.getNozzleParameters().getTravelBeforeForcedClose()))
                {
                    writeEventsWithNozzleClose("travel trigger");
                }

                extrusionBuffer.add(event);
            }
            lastPoint = currentPoint;

        } else if (event instanceof LayerChangeWithoutTravelEvent)
        {
            LayerChangeWithoutTravelEvent layerChangeEvent = (LayerChangeWithoutTravelEvent) event;

            currentZHeight = layerChangeEvent.getZ();

            handleForcedNozzleAtLayerChange();

            insertSubsequentLayerTemperatures();

            layer++;

            handleMovieMakerAtLayerChange(relativeMoveEvent, moveUpEvent, absoluteMoveEvent,
                                          moveToMiddleYEvent, homeEvent, dwellEvent);

            extrusionBuffer.add(event);
        } else if (event instanceof LayerChangeWithTravelEvent)
        {
            LayerChangeWithTravelEvent layerChangeEvent = (LayerChangeWithTravelEvent) event;

            currentZHeight = layerChangeEvent.getZ();

            handleForcedNozzleAtLayerChange();

            insertSubsequentLayerTemperatures();

            layer++;

            handleMovieMakerAtLayerChange(relativeMoveEvent, moveUpEvent, absoluteMoveEvent,
                                          moveToMiddleYEvent, homeEvent, dwellEvent);

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
                currentNozzle = nozzleProxies.get(nozzleInUse);
            } else if (layer < 1)
            {
                tempNozzleMemory = nozzleChangeEvent.getNozzleNumber();
            } else if (layer > 1)
            {
                extrusionBuffer.add(nozzleChangeEvent);
                nozzleInUse = nozzleChangeEvent.getNozzleNumber();
                currentNozzle = nozzleProxies.get(nozzleInUse);
            }

            // Reset the nozzle close counter
            closeCounter = 0;
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
                && currentNozzle.getNozzleParameters().getOpenOverVolume() <= 0)
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

            if (unretractEvent.getFeedRate() > 0)
            {
                unretractFeedRate = unretractEvent.getFeedRate();
            }
        } else if (event instanceof UnretractEvent)
        {
            UnretractEvent unretractEvent = (UnretractEvent) event;

            if (unretractEvent.getFeedRate() > 0)
            {
                unretractFeedRate = unretractEvent.getFeedRate();
            }
        } else if (event instanceof MCodeEvent)
        {
            MCodeEvent mCodeEvent = (MCodeEvent) event;
            if (mCodeEvent.getMNumber() != 104
                && mCodeEvent.getMNumber() != 109
                && mCodeEvent.getMNumber() != 140
                && mCodeEvent.getMNumber() != 190)
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
                for (String macroLine : GCodeMacros.getMacroContents("after_print"))
                {
                    outputWriter.writeOutput(macroLine + "\n");
                }
                outputWriter.writeOutput("; End of Post print gcode\n");
            } catch (IOException ex)
            {
                throw new PostProcessingError("IO Error whilst writing post-print gcode to file: " + ex.getMessage());
            } catch (MacroLoadException ex)
            {
                throw new PostProcessingError("Error whilst writing post-print gcode to file - couldn't add after print footer due to circular macro reference");
            }

            writeEventToFile(event);
        }
    }

    private void handleMovieMakerAtLayerChange(GCodeEvent relativeMoveEvent, GCodeEvent moveUpEvent,
        GCodeEvent absoluteMoveEvent, GCodeEvent moveToMiddleYEvent, GCodeEvent homeEvent,
        GCodeEvent dwellEvent)
    {
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
    }

    private void handleForcedNozzleAtLayerChange() throws PostProcessingError
    {
        if (((slicerType == SlicerType.Slic3r && layer == 0) || (slicerType == SlicerType.Cura
            && layer == 0)) && forcedNozzleOnFirstLayer >= 0 && nozzleHasBeenForced == false)
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
                nozzleChangeEvent.setComment(" - force to nozzle " + forcedNozzleOnFirstLayer
                    + " on first layer");
            }
            tempNozzleMemory = 0;
            extrusionBuffer.add(nozzleChangeEvent);
            nozzleInUse = forcedNozzleOnFirstLayer;
            currentNozzle = nozzleProxies.get(nozzleInUse);
        }

        if (((slicerType == SlicerType.Slic3r && layer == 1) || (slicerType == SlicerType.Cura
            && layer == 1))
            && forcedNozzleOnFirstLayer >= 0
            && currentNozzle.getState() == NozzleState.OPEN)
        {
            writeEventsWithNozzleClose(
                "closing nozzle after forced nozzle select on layer 0");
        }
    }

    private int chooseNozzleByTask(GCodeParseEvent event)
    {
        int nozzleToUse;

        ExtrusionTask extrusionTask = ((ExtrusionEvent) event).getExtrusionTask();

        if (extrusionTask != null)
        {
            switch (extrusionTask)
            {
                case Perimeter:
                case ExternalPerimeter:
                    nozzleToUse = currentSettings.getPerimeterNozzle();
                    break;
                case Fill:
                    nozzleToUse = currentSettings.getFillNozzle();
                    break;
                case Support:
                    nozzleToUse = currentSettings.getSupportNozzle();
                    break;
                case Support_Interface:
                    nozzleToUse = currentSettings.getSupportInterfaceNozzle();
                    break;
                default:
                    nozzleToUse = currentSettings.getFillNozzle();
                    break;
            }
        } else
        {
            nozzleToUse = currentSettings.getFillNozzle();
        }
        return nozzleToUse;
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

    protected void writeEventsWithNozzleClose(String comment) throws PostProcessingError
    {
        boolean closeAtEndOfPath = false;

        Map<EventType, Integer> eventIndices = new EnumMap<EventType, Integer>(EventType.class);

        double nozzleStartPosition = 1.0;
        double nozzleCloseOverVolume = 1;

        if ((currentNozzle.getNozzleParameters().getPreejectionVolume() == 0
            && currentNozzle.getNozzleParameters().getEjectionVolume() == 0
            && currentNozzle.getNozzleParameters().getWipeVolume() == 0)
            && extrusionBuffer.containsExtrusionEvents())
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
        } else if (extrusionBuffer.containsExtrusionEvents())
        {
            //Quick check to see if the extrusion volumes are sufficient to allow normal close
            double totalExtrusionForPath = 0;

            ArrayList<Integer> inwardsMoveIndexList = new ArrayList<>();

            int tempFinalExtrusionEventIndex = extrusionBuffer.getExtrusionEventIndexBackwards(
                extrusionBuffer.size() - 1);
            if (((ExtrusionEvent) extrusionBuffer.get(tempFinalExtrusionEventIndex)).
                getExtrusionTask() == ExtrusionTask.ExternalPerimeter)
            {
                int endOfPerimeter = extrusionBuffer.getPreviousExtrusionTask(extrusionBuffer.size()
                    - 1, ExtrusionTask.Perimeter);
                if (endOfPerimeter >= 0)
                {
                    tempFinalExtrusionEventIndex = endOfPerimeter;
                }
            }
            int tempFirstExtrusionEventIndex = extrusionBuffer.
                getStartOfExtrusionEventBoundaryIndex(tempFinalExtrusionEventIndex);

            for (int extrusionBufferIndex = 0; extrusionBufferIndex <= extrusionBuffer.size() - 1; extrusionBufferIndex++)
            {
                GCodeParseEvent event = extrusionBuffer.get(extrusionBufferIndex);

                if (event instanceof ExtrusionEvent && extrusionBufferIndex
                    >= tempFirstExtrusionEventIndex && extrusionBufferIndex
                    <= tempFinalExtrusionEventIndex)
                {
                    totalExtrusionForPath += ((ExtrusionEvent) event).getE()
                        + ((ExtrusionEvent) event).getD();
                } else if (event instanceof TravelEvent)
                {
                    switch (slicerType)
                    {
                        case Slic3r:
                            String eventComment = event.getComment();
                            if (eventComment != null)
                            {
                                //TODO Slic3r specific code!
                                if (eventComment.contains("move inwards"))
                                {
                                    inwardsMoveIndexList.add(extrusionBufferIndex);
                                }
                            }
                            break;
                    }
                }
            }

            TravelEvent lastInwardMoveEvent = null;

            if (!inwardsMoveIndexList.isEmpty())
            {
                int tempLastInwardsMoveIndex = inwardsMoveIndexList.get(inwardsMoveIndexList.size()
                    - 1);

                if (tempLastInwardsMoveIndex < tempFinalExtrusionEventIndex
                    || tempLastInwardsMoveIndex > tempFirstExtrusionEventIndex)
                {
                    lastInwardMoveEvent = (TravelEvent) extrusionBuffer.
                        get(tempLastInwardsMoveIndex);
                }

                for (int i = inwardsMoveIndexList.size() - 1; i >= 0; i--)
                {
                    int indexToRemove = inwardsMoveIndexList.get(i);
                    extrusionBuffer.remove(indexToRemove);
                }
            }

            int finalExtrusionEventIndex = extrusionBuffer.getFinalExtrusionEventIndex();
            int firstUsableExtrusionEventIndex = extrusionBuffer.
                getStartOfExtrusionEventBoundaryIndex(finalExtrusionEventIndex);
            int firstExtrusionEventIndex = extrusionBuffer.getNextExtrusionEventIndex(0);

            if (finalExtrusionEventIndex >= 0)
            {
                ExtrusionTask extrusionTask = ((ExtrusionEvent) extrusionBuffer.get(
                    finalExtrusionEventIndex)).getExtrusionTask();

                boolean failedToInsertClose = false;
                boolean openInserted = false;

                if (extrusionTask != ExtrusionTask.Fill)
                {

//                    if ((totalExtrusionForPath >= currentNozzle.getNozzleParameters().
//                        getOpenOverVolume()
//                        + currentNozzle.getNozzleParameters().getPreejectionVolume())
//                        && (totalExtrusionForPath >= currentNozzle.getNozzleParameters().
//                        getEjectionVolume()
//                        + currentNozzle.getNozzleParameters().getWipeVolume()))
                    // At the moment we only look at ejection volume for a normal perimeter close
                    if (totalExtrusionForPath >= currentNozzle.getNozzleParameters().
                        getEjectionVolume())
                    {
                        //OK - we're go for a normal close   
                        nozzleStartPosition = 1.0;
                        nozzleCloseOverVolume = currentNozzle.getNozzleParameters().
                            getEjectionVolume();

                        //Don't include open over for the moment - we'll need to do it after the attempt to close and then compensate the close index
//                      openInserted = insertOpenNozzleOverVolume(eventIndices, comment);
                        int sizeOfBuffer = extrusionBuffer.size();

                        try
                        {
                            int startOfClose = insertTravelAndClosePath(
                                firstUsableExtrusionEventIndex,
                                finalExtrusionEventIndex,
                                "Move to start of wipe", false,
                                false,
                                lastInwardMoveEvent,
                                currentNozzle.
                                getNozzleParameters().
                                getEjectionVolume());

                            double actualClosedOverVolumeAvailable = calculateVolume(extrusionBuffer,
                                                                                     startOfClose,
                                                                                     extrusionBuffer.
                                                                                     size() - 1);

                            if (actualClosedOverVolumeAvailable < nozzleCloseOverVolume)
                            {
                                nozzleCloseOverVolume = actualClosedOverVolumeAvailable;
                            }

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
                        } catch (CannotCloseOnInnerPerimeterException ex)
                        {
                            // We must have been unable to find an inner line to close on. We need to default to the old school close over distance
                            failedToInsertClose = true;
                        }

                    } else if (totalExtrusionForPath > 0)
                    {
                        double volumeToConsider = 2 * totalExtrusionForPath;

                        if (totalExtrusionForPath < currentNozzle.getNozzleParameters().
                            getOpenOverVolume()
                            || totalExtrusionForPath < currentNozzle.getNozzleParameters().
                            getEjectionVolume())
                        {
                            // We don't have enough volume to open and close as per settings
                            double bValue = Math.min(1, totalExtrusionForPath / currentNozzle.
                                                     getNozzleParameters().getEjectionVolume());
                            bValue = Math.max(currentNozzle.getNozzleParameters().
                                getPartialBMinimum(), bValue);

                            nozzleStartPosition = bValue;
                            nozzleCloseOverVolume = Math.min(currentNozzle.getNozzleParameters().
                                getEjectionVolume(), totalExtrusionForPath);

                            if (currentNozzle.getNozzleParameters().getOpenOverVolume() > 0)
                            {
                                NozzleChangeBValueEvent partialOpen = new NozzleChangeBValueEvent();
                                partialOpen.setB(bValue);
                                int firstExtrusion = extrusionBuffer.getNextExtrusionEventIndex(0);
                                extrusionBuffer.add(firstExtrusion, partialOpen);
                            } else
                            {
                                replaceOpenNozzleWithPartialOpen(bValue);
                            }

                            int penultimateExtrusionEventIndex = extrusionBuffer.
                                getPreviousExtrusionEventIndex(finalExtrusionEventIndex - 1);

                            if (penultimateExtrusionEventIndex < 0)
                            {
                                // There is only one extrusion event, so nothing to copy
                                int previousTravelEventIndex = extrusionBuffer.
                                    getPreviousEventIndex(finalExtrusionEventIndex - 1,
                                                          TravelEvent.class);

                                if (previousTravelEventIndex
                                    >= 0)
                                {
                                    ExtrusionEvent finalExtrusionEvent = (ExtrusionEvent) extrusionBuffer.
                                        get(finalExtrusionEventIndex);
                                    TravelEvent previousTravelEvent = (TravelEvent) extrusionBuffer.
                                        get(previousTravelEventIndex);

                                    ExtrusionEvent reverseEvent = new ExtrusionEvent();
                                    reverseEvent.setComment("Synthesised extrusion for close");
                                    reverseEvent.setE(finalExtrusionEvent.getE());
                                    reverseEvent.setD(finalExtrusionEvent.getD());
                                    reverseEvent.setX(previousTravelEvent.getX());
                                    reverseEvent.setY(previousTravelEvent.getY());
                                    reverseEvent.setFeedRate(wipeFeedRate_mmPerMin);

                                    extrusionBuffer.add(reverseEvent);

                                    eventIndices.put(EventType.NOZZLE_CLOSE_START,
                                                     finalExtrusionEventIndex + 1);
                                } else
                                {
                                    // Last ditch...
                                    // Close the nozzle at the end of the line...
                                    closeAtEndOfPath = true;
                                    NozzleCloseFullyEvent closeNozzle = new NozzleCloseFullyEvent();
                                    closeNozzle.setComment("Single element path - closing at end");
                                    closeNozzle.setLength(0);

                                    GCodeParseEvent lastEvent = extrusionBuffer.get(extrusionBuffer.
                                        size() - 1);
                                    if (lastEvent instanceof LayerChangeEvent)
                                    {
                                        extrusionBuffer.add(extrusionBuffer.size() - 1, closeNozzle);
                                    } else
                                    {
                                        extrusionBuffer.add(closeNozzle);
                                    }
                                }
                            } else
                            {
                                try
                                {
                                    int nozzleCloseStartIndex = insertTravelAndClosePath(
                                        firstUsableExtrusionEventIndex, finalExtrusionEventIndex,
                                        "Move to start of wipe - partial open", false,
                                        true,
                                        lastInwardMoveEvent, currentNozzle.getNozzleParameters().
                                        getEjectionVolume());

                                    eventIndices.
                                        put(EventType.NOZZLE_CLOSE_START, nozzleCloseStartIndex);

                                    ExtrusionEvent finalExtrusionEvent = ((ExtrusionEvent) extrusionBuffer.
                                        get(finalExtrusionEventIndex));
                                    nozzleCloseOverVolume = totalExtrusionForPath
                                        - (finalExtrusionEvent.getE() + finalExtrusionEvent.getD());
                                } catch (CannotCloseOnInnerPerimeterException ex)
                                {
                                    steno.error("Failed to close in partial open for non-fill");
                                }

                            }
                        } else
                        {
                            //Full open and close
                            nozzleStartPosition = 1.0;
                            nozzleCloseOverVolume = Math.min(currentNozzle.getNozzleParameters().
                                getWipeVolume(), totalExtrusionForPath);

                            insertOpenNozzleOverVolume(eventIndices, comment);

//                        if (currentNozzle.getPreejectionVolume() > 0)
//                        {
//                            double preejectVolume = Math.min(currentNozzle.getPreejectionVolume(),
                            //totalExtrusionForPath - currentNozzle.getOpenOverVolume());
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
                            int nozzleCloseStartIndex = -1;
                            try
                            {
                                nozzleCloseStartIndex = insertTravelAndClosePath(
                                    firstUsableExtrusionEventIndex, finalExtrusionEventIndex,
                                    "Move to start of wipe - full open", false, true,
                                    lastInwardMoveEvent, currentNozzle.getNozzleParameters().
                                    getEjectionVolume());
                            } catch (CannotCloseOnInnerPerimeterException ex)
                            {
                                steno.error("Failed to close in full open and close");
                            }
//                        double wipeVolume = Math.min(currentNozzle.getWipeVolume(), 
                            //totalExtrusionForPath - currentNozzle.getEjectionVolume());
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
                }

                if (extrusionTask == ExtrusionTask.Fill || failedToInsertClose)
                {
                    // Infill or failed attempt at inserting close for perimeter

                    if (totalExtrusionForPath >= currentNozzle.getNozzleParameters().
                        getOpenOverVolume()
                        + currentNozzle.getNozzleParameters().getPreejectionVolume()
                        + currentNozzle.getNozzleParameters().getEjectionVolume()
                        + currentNozzle.getNozzleParameters().getWipeVolume())
                    {
                        //OK - we're go for a normal close   
                        nozzleStartPosition = 1.0;
                        nozzleCloseOverVolume = currentNozzle.getNozzleParameters().
                            getEjectionVolume();

                        insertOpenNozzleOverVolume(eventIndices, comment);

                        insertWipeOverVolume(eventIndices, comment);

                        insertPreejectionOverVolume(eventIndices, comment);

                        insertEjectionOverVolume(eventIndices,
                                                 comment);

                        if (currentNozzle.getNozzleParameters().getOpenValueAtMidPoint() > 0)
                        {
                            // Now the nozzle close break point
                            insertVolumeBreak(extrusionBuffer,
                                              eventIndices,
                                              EventType.NOZZLE_CLOSE_MIDPOINT,
                                              currentNozzle.getNozzleParameters().
                                              getEjectionVolume() * (1
                                              - (currentNozzle.getNozzleParameters().
                                              getMidPointPercent() / 100.0))
                                              + currentNozzle.getNozzleParameters().
                                              getWipeVolume(),
                                              comment,
                                              FindEventDirection.BACKWARDS_FROM_END);
                        }
                    } else if (totalExtrusionForPath > 0)
                    {
                        //Keep the wipe volume until the minimum B is exceeded
                        double extrusionVolumeAfterWipe = totalExtrusionForPath
                            - currentNozzle.getNozzleParameters().getWipeVolume();
                        double bValue = Math.min(1, extrusionVolumeAfterWipe
                                                 / currentNozzle.getNozzleParameters().
                                                 getEjectionVolume());

                        double minimumBEjectionVolume = currentNozzle.getNozzleParameters().
                            getEjectionVolume()
                            * currentNozzle.getNozzleParameters().getPartialBMinimum();

                        if (bValue < currentNozzle.getNozzleParameters().getPartialBMinimum())
                        {
                            //Shorten the wipe
                            double requiredWipeVolume = totalExtrusionForPath
                                - minimumBEjectionVolume;

                            if (requiredWipeVolume <= 0)
                            {
                                //Not enough volume for the wipe even at minimum B
                                nozzleStartPosition = currentNozzle.getNozzleParameters().
                                    getPartialBMinimum();
                                nozzleCloseOverVolume = totalExtrusionForPath;

                                if (firstUsableExtrusionEventIndex == firstExtrusionEventIndex)
                                {
                                    if (currentNozzle.getNozzleParameters().getOpenOverVolume()
                                        > 0)
                                    {
                                        NozzleChangeBValueEvent partialOpen = new NozzleChangeBValueEvent();
                                        partialOpen.setB(currentNozzle.getNozzleParameters().
                                            getPartialBMinimum());
                                        int firstExtrusion = extrusionBuffer.
                                            getNextExtrusionEventIndex(0);
                                        extrusionBuffer.add(firstExtrusion, partialOpen);
                                    } else
                                    {
                                        replaceOpenNozzleWithPartialOpen(currentNozzle.
                                            getNozzleParameters().getPartialBMinimum());
                                    }
                                }

                                int nozzleCloseStartIndex = firstUsableExtrusionEventIndex;
                                eventIndices.
                                    put(EventType.NOZZLE_CLOSE_START, nozzleCloseStartIndex);
                                extrusionBuffer.get(nozzleCloseStartIndex).setComment(
                                    "Short path");
                            } else
                            {
                                //We can use a shortened wipe with minimum B
                                nozzleStartPosition = currentNozzle.getNozzleParameters().
                                    getPartialBMinimum();
                                nozzleCloseOverVolume = minimumBEjectionVolume;

                                if (firstUsableExtrusionEventIndex == firstExtrusionEventIndex)
                                {
                                    if (currentNozzle.getNozzleParameters().getOpenOverVolume()
                                        > 0)
                                    {
                                        NozzleChangeBValueEvent partialOpen = new NozzleChangeBValueEvent();
                                        partialOpen.setB(currentNozzle.getNozzleParameters().
                                            getPartialBMinimum());
                                        int firstExtrusion = extrusionBuffer.
                                            getNextExtrusionEventIndex(0);
                                        extrusionBuffer.add(firstExtrusion, partialOpen);
                                    } else
                                    {
                                        replaceOpenNozzleWithPartialOpen(currentNozzle.
                                            getNozzleParameters().getPartialBMinimum());
                                    }
                                }

                                insertVolumeBreak(extrusionBuffer,
                                                  eventIndices,
                                                  EventType.WIPE_START,
                                                  requiredWipeVolume,
                                                  comment,
                                                  FindEventDirection.BACKWARDS_FROM_END);

                                int nozzleCloseStartIndex = firstUsableExtrusionEventIndex;
                                eventIndices.
                                    put(EventType.NOZZLE_CLOSE_START, nozzleCloseStartIndex);
                                extrusionBuffer.get(nozzleCloseStartIndex).setComment(
                                    "Shortened wipe volume");
                            }
                        } else
                        {
                            //Retain the full wipe but open partially and use a proportionately smaller ejection volume
                            nozzleStartPosition = bValue;
                            nozzleCloseOverVolume = Math.min(
                                currentNozzle.getNozzleParameters().
                                getEjectionVolume(), extrusionVolumeAfterWipe);

                            if (firstUsableExtrusionEventIndex == firstExtrusionEventIndex)
                            {
                                if (currentNozzle.getNozzleParameters().getOpenOverVolume() > 0)
                                {
                                    NozzleChangeBValueEvent partialOpen = new NozzleChangeBValueEvent();
                                    partialOpen.setB(bValue);
                                    int firstExtrusion = extrusionBuffer.
                                        getNextExtrusionEventIndex(
                                            0);
                                    extrusionBuffer.add(firstExtrusion, partialOpen);
                                } else
                                {
                                    replaceOpenNozzleWithPartialOpen(bValue);
                                }
                            }

                            insertWipeOverVolume(eventIndices, comment);

                            int nozzleCloseStartIndex = insertVolumeBreak(extrusionBuffer,
                                                                          eventIndices,
                                                                          EventType.NOZZLE_CLOSE_START,
                                                                          nozzleCloseOverVolume
                                                                          + currentNozzle.
                                                                          getNozzleParameters().
                                                                          getWipeVolume(),
                                                                          comment,
                                                                          FindEventDirection.BACKWARDS_FROM_END);

                            if (compareDouble(nozzleCloseOverVolume, currentNozzle.
                                              getNozzleParameters().getEjectionVolume(), 10e-5)
                                == EQUAL)
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

                        if (candidateevent instanceof MovementEvent)
                        {
                            lastProcessedMovementEvent = (MovementEvent) candidateevent;
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
                                    / currentNozzle.getNozzleParameters().getOpenOverVolume());

                                if (compareDouble(currentNozzlePosition, 1, 10e-5)
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
                            } else if ((nozzleCloseStartIndex >= 0 && eventWriteIndex
                                >= nozzleCloseStartIndex)
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
                                    String commentToOutput = ((event.getComment() == null) ? "" : event.
                                        getComment()) + " Normal close";
                                    nozzleEvent.setComment(commentToOutput);
                                    currentNozzlePosition = currentNozzlePosition
                                        - (nozzleStartPosition * (event.getE()
                                        / nozzleCloseOverVolume));
                                } else
                                {
                                    nozzleEvent.setComment(event.getComment()
                                        + " Differential close - part 1");
                                    currentNozzlePosition = currentNozzlePosition
                                        - (nozzleStartPosition
                                        * (1
                                        - currentNozzle.getNozzleParameters().
                                        getOpenValueAtMidPoint()) * (event.getE()
                                        / (nozzleCloseOverVolume
                                        * (currentNozzle.getNozzleParameters().
                                        getMidPointPercent()
                                        / 100.0))));
                                }
                                if (compareDouble(currentNozzlePosition, 0, 10e-5)
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
                                    - (nozzleStartPosition
                                    * currentNozzle.getNozzleParameters().
                                    getOpenValueAtMidPoint()
                                    * (event.getE()
                                    / (nozzleCloseOverVolume * (1
                                    - (currentNozzle.getNozzleParameters().getMidPointPercent()
                                    / 100.0)))));
                                if (compareDouble(currentNozzlePosition, 0, 10e-5)
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
                                outputNoBNoE(event, "Pre-close starvation - eliding " + event.
                                             getE()
                                             + event.getD());
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
                                NozzleProxy newNozzle = nozzleProxies.get(
                                    ((NozzleChangeEvent) candidateevent).getNozzleNumber());
                                currentNozzle = newNozzle;
                                closeCounter = 0;
                            }
                        }
                    }
                }

                extrusionBuffer.clear();

                currentNozzle.closeNozzleFully();

                // Determine whether to insert a nozzle reselect at the end of this extrusion path
                if (closeCounter >= triggerNozzleReselectAfterNCloses)
                {
                    if (triggerNozzleReselectAfterNCloses >= 0)
                    {
                        NozzleChangeEvent nozzleReselect = new NozzleChangeEvent();
                        nozzleReselect.setComment("Reselect nozzle");
                        nozzleReselect.setNozzleNumber(currentNozzle.getNozzleReferenceNumber());
                        writeEventToFile(nozzleReselect);
                    }
                    closeCounter = 0;
                } else
                {
                    closeCounter++;
                }

                // Always output an M109 after nozzle close
                // Required to ensure that print temperature is maintained if nozzle heater inhibit is active
                MCodeEvent m109Event = new MCodeEvent();
                m109Event.setMNumber(109);
                writeEventToFile(m109Event);

            } else if (extrusionBuffer.size() > 0 && extrusionBuffer.containsExtrusionEvents())
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
                        NozzleProxy newNozzle = nozzleProxies.get(
                            ((NozzleChangeEvent) event).getNozzleNumber());
                        currentNozzle = newNozzle;
                    }
                }
            }
        }
    }

    private void insertEjectionOverVolume(Map<EventType, Integer> eventIndices, String comment)
    {
        // Now the nozzle close starting point
        insertVolumeBreak(extrusionBuffer,
                          eventIndices,
                          EventType.NOZZLE_CLOSE_START,
                          currentNozzle.getNozzleParameters().getEjectionVolume()
                          + currentNozzle.getNozzleParameters().getWipeVolume(),
                          comment,
                          FindEventDirection.BACKWARDS_FROM_END);
    }

    private void insertPreejectionOverVolume(Map<EventType, Integer> eventIndices, String comment)
    {
        // Calculate the pre-ejection point (if we need to...)
        if (currentNozzle.getNozzleParameters().getPreejectionVolume() > 0)
        {
            insertVolumeBreak(extrusionBuffer,
                              eventIndices,
                              EventType.PRE_CLOSE_STARVATION_START,
                              currentNozzle.getNozzleParameters().
                              getPreejectionVolume()
                              + currentNozzle.getNozzleParameters().
                              getEjectionVolume()
                              + currentNozzle.getNozzleParameters().getWipeVolume(),
                              comment,
                              FindEventDirection.BACKWARDS_FROM_END);
        }
    }

    private void insertWipeOverVolume(Map<EventType, Integer> eventIndices, String comment)
    {
        // Calculate the wipe point (if we need to...)
        if (currentNozzle.getNozzleParameters().getWipeVolume() > 0)
        {

            insertVolumeBreak(extrusionBuffer,
                              eventIndices,
                              EventType.WIPE_START,
                              currentNozzle.getNozzleParameters().getWipeVolume(),
                              comment,
                              FindEventDirection.BACKWARDS_FROM_END);
        }
    }

    private boolean insertOpenNozzleOverVolume(Map<EventType, Integer> eventIndices, String comment)
    {
        boolean insertAttempt = false;

        if (currentNozzle.getNozzleParameters().getOpenOverVolume() > 0)
        {

            insertVolumeBreak(extrusionBuffer,
                              eventIndices,
                              EventType.NOZZLE_OPEN_END,
                              currentNozzle.getNozzleParameters().
                              getOpenOverVolume(),
                              comment,
                              FindEventDirection.FORWARDS_FROM_START);

            insertAttempt = true;
        }

        return insertAttempt;
    }

    protected int insertTravelAndClosePath(final int firstExtrusionEventIndex,
        final int finalExtrusionEventIndex, final String originalComment, boolean forceReverse,
        final boolean reverseAllowed,
        TravelEvent lastInwardsMoveEvent,
        double targetVolume) throws PostProcessingError, CannotCloseOnInnerPerimeterException
    {
        boolean reverseWipePath = forceReverse;

        ExtrusionEvent originalFinalExtrusionEvent = (ExtrusionEvent) extrusionBuffer.get(
            finalExtrusionEventIndex);
        int modifiedFinalExtrusionEventIndex = finalExtrusionEventIndex;

        int insertedEventIndex = modifiedFinalExtrusionEventIndex + 1;

        int startOfClose = -1;

        ExtrusionEvent finalExtrusionEvent = (ExtrusionEvent) extrusionBuffer.get(
            modifiedFinalExtrusionEventIndex);
        Vector2D endOfExtrusion = new Vector2D(finalExtrusionEvent.getX(), finalExtrusionEvent.
                                               getY());

        int closestEventIndex = -1;

        boolean finalExtrusionWasPerimeter = false;

        int indexToCopyFrom = -1;
        int minimumIndexToCopyFrom = firstExtrusionEventIndex;

        // We can't go back over a layer boundary to wipe...
        int lastLayerChangeIndex = extrusionBuffer.getPreviousEventIndex(
            modifiedFinalExtrusionEventIndex, LayerChangeEvent.class);

        if (lastLayerChangeIndex <= 0)
        {
            lastLayerChangeIndex = 0;
        }

        if (reverseWipePath == false && extrusionBuffer.getPreviousExtrusionEventIndex(
            modifiedFinalExtrusionEventIndex) >= 0)
        {
            if (finalExtrusionEvent.getExtrusionTask() == ExtrusionTask.ExternalPerimeter)
            {
                // We have to make sure we only close on the inner perimeters (if we can!)
                // Calculate new start/end values
                int endOfInnerPerimeter = extrusionBuffer.getPreviousExtrusionTask(
                    modifiedFinalExtrusionEventIndex, ExtrusionTask.Perimeter);
                if (endOfInnerPerimeter < 0
                    || extrusionBuffer.getPreviousExtrusionEventIndex(
                        modifiedFinalExtrusionEventIndex) < 0)
                {
                    // We couldn't find an inner perimeter - revert to reverse
                    steno.info("No inner perimeter found - reversing - on layer " + layer);
                    finalExtrusionWasPerimeter = false;
                } else
                {
                    modifiedFinalExtrusionEventIndex = endOfInnerPerimeter;
                    finalExtrusionEvent = (ExtrusionEvent) extrusionBuffer.get(
                        modifiedFinalExtrusionEventIndex);
                    finalExtrusionWasPerimeter = true;
                }
            } else if (finalExtrusionEvent.getExtrusionTask() == ExtrusionTask.Perimeter)
            {
                // We're OK to use the start/end values we were passed
                finalExtrusionWasPerimeter = true;
            }

            if (finalExtrusionWasPerimeter)
            {
                Segment orthogonalSegment = null;
                Vector2D orthogonalSegmentMidpoint = null;
                Vector2D lastPointConsidered = null;

                TreeMap<Double, Integer> intersectedPointDistances = new TreeMap<>();

                int intersectionCounter = 0;
                int maxNumberOfIntersectionsToConsider = currentSettings.getNumberOfPerimeters();
                float maxDistanceFromEndPoint = currentSettings.getPerimeterExtrusionWidth_mm()
                    * 1.01f * maxNumberOfIntersectionsToConsider;

                // Attempt to use the inwards move to find the innermost perimeter
                if (lastInwardsMoveEvent != null)
                {
                    Vector2D inwardsMoveEndPoint = new Vector2D(lastInwardsMoveEvent.getX(),
                                                                lastInwardsMoveEvent.getY());
                    inwardsMoveEndPoint.scalarMultiply(4);
                    orthogonalSegment = new Segment(endOfExtrusion, inwardsMoveEndPoint, new Line(
                                                    endOfExtrusion, inwardsMoveEndPoint, 1e-12));
                } else
                {
//                    steno.warning("Using backup orthogonal segment");
                    int absolutelyTheLastExtrusionEventIndexEver = extrusionBuffer.
                        getPreviousExtrusionEventIndex(extrusionBuffer.size() - 1);
                    ExtrusionEvent absolutelyTheLastExtrusionEventEver = (ExtrusionEvent) extrusionBuffer.
                        get(absolutelyTheLastExtrusionEventIndexEver);

                    endOfExtrusion = new Vector2D(absolutelyTheLastExtrusionEventEver.getX(),
                                                  absolutelyTheLastExtrusionEventEver.getY());

                    int absolutelyTheLastMovementEventIndexEver = extrusionBuffer.
                        getPreviousMovementEventIndex(absolutelyTheLastExtrusionEventIndexEver);
                    MovementEvent absolutelyTheLastMovementEventEver = (MovementEvent) extrusionBuffer.
                        get(absolutelyTheLastMovementEventIndexEver);
                    Vector2D absolutelyTheLastMovementVectorEver = new Vector2D(
                        absolutelyTheLastMovementEventEver.getX(),
                        absolutelyTheLastMovementEventEver.getY());

                    orthogonalSegment = MathUtils.getOrthogonalLineToLinePoints(
                        maxDistanceFromEndPoint, absolutelyTheLastMovementVectorEver, endOfExtrusion);
                }

                orthogonalSegmentMidpoint = MathUtils.findMidPoint(orthogonalSegment.getStart(),
                                                                   orthogonalSegment.getEnd());

                //Prime the last movement if we can...
                int indexToBeginSearchAt = Math.max(lastLayerChangeIndex, firstExtrusionEventIndex);
                int indexOfPriorMovement = extrusionBuffer.getPreviousMovementEventIndex(
                    indexToBeginSearchAt);

                if (indexOfPriorMovement >= 0)
                {
                    MovementEvent priorMovementEvent = (MovementEvent) extrusionBuffer.get(
                        indexOfPriorMovement);
                    lastPointConsidered = new Vector2D(priorMovementEvent.getX(),
                                                       priorMovementEvent.getY());
                } else
                {
                    indexOfPriorMovement = extrusionBuffer.getPreviousEventIndex(
                        indexToBeginSearchAt, LayerChangeWithTravelEvent.class);

                    if (indexOfPriorMovement >= 0)
                    {
                        LayerChangeWithTravelEvent priorMovementEvent = (LayerChangeWithTravelEvent) extrusionBuffer.
                            get(indexOfPriorMovement);
                        lastPointConsidered = new Vector2D(priorMovementEvent.getX(),
                                                           priorMovementEvent.getY());
                    }
                }

                for (int eventIndex = indexToBeginSearchAt;
                    intersectionCounter <= maxNumberOfIntersectionsToConsider
                    && eventIndex <= modifiedFinalExtrusionEventIndex;
                    eventIndex++)
                {
                    if (extrusionBuffer.get(eventIndex) instanceof MovementEvent)
                    {
                        MovementEvent thisMovementEvent = (MovementEvent) extrusionBuffer.get(
                            eventIndex);
                        Vector2D thisMovement = new Vector2D(thisMovementEvent.getX(),
                                                             thisMovementEvent.getY());

                        if (extrusionBuffer.get(eventIndex) instanceof ExtrusionEvent
                            && lastPointConsidered != null)
                        {
                            // Detect intersections
                            Segment segmentUnderConsideration = new Segment(lastPointConsidered,
                                                                            thisMovement, new Line(
                                                                                lastPointConsidered,
                                                                                thisMovement, 1e-12));
                            Vector2D intersectionPoint = MathUtils.getSegmentIntersection(
                                orthogonalSegment, segmentUnderConsideration);
                            if (intersectionPoint != null)
                            {
                                double distanceFromMidPoint = intersectionPoint.distance(
                                    orthogonalSegmentMidpoint);

//                                if (distanceFromEndPoint <= maxDistanceFromEndPoint)
//                                {
                                intersectedPointDistances.put(distanceFromMidPoint, eventIndex);
                                intersectionCounter++;
//                                }
                            }
                        }

                        lastPointConsidered = thisMovement;
                    }
                }

                if (intersectedPointDistances.size() > 0)
                {
                    closestEventIndex = (int) intersectedPointDistances.values().toArray()[intersectedPointDistances.
                        size() - 1];
                }

                if (closestEventIndex < 0)
                {

                    if (reverseAllowed)
                    {
                        steno.warning(
                            "Couldn't find inner perimeter for close - defaulting to reverse. Got up to line "
                            + extrusionBuffer.get(extrusionBuffer.size() - 1).getLinesSoFar());
                        finalExtrusionWasPerimeter = false;
                    } else
                    {
                        steno.warning(
                            "Couldn't find inner perimeter for close - fall back to normal close. Got up to line "
                            + extrusionBuffer.get(extrusionBuffer.size() - 1).getLinesSoFar());
                        throw new CannotCloseOnInnerPerimeterException("Line: " + extrusionBuffer.
                            get(
                                extrusionBuffer.size() - 1).getLinesSoFar());
                    }
                } else
                {
                    indexToCopyFrom = closestEventIndex;
                }
            }
        }

        String wipeTypeComment = "";

        boolean reverseAlongPath = true;
        int modifiedFirstExtrusionIndex = Math.max(lastLayerChangeIndex, firstExtrusionEventIndex);

        // If we're dealing with a perimeter then determine whether we need to go forward or backwards
        if (finalExtrusionWasPerimeter)
        {
            MovementEvent closestEvent = null;

            boolean forwardsSearch = true;

            double forwardsVolume = 0;
            double reverseVolume = 0;

            for (int iterations = 0; iterations <= 1; iterations++)
            {
                double volumeTotal = 0;

                //Count up the available volume - forwards first
                for (int movementIndex = closestEventIndex;
                    movementIndex >= modifiedFirstExtrusionIndex && movementIndex
                    <= modifiedFinalExtrusionEventIndex;
                    movementIndex += ((forwardsSearch) ? 1 : -1))
                {
                    GCodeParseEvent eventUnderExamination = extrusionBuffer.get(movementIndex);
                    if (eventUnderExamination instanceof ExtrusionEvent)
                    {
                        volumeTotal += ((ExtrusionEvent) eventUnderExamination).getE()
                            + ((ExtrusionEvent) eventUnderExamination).getD();
                    }
                }

                if (forwardsSearch)
                {
                    forwardsVolume = volumeTotal;
                } else
                {
                    reverseVolume = volumeTotal;
                }

                forwardsSearch = !forwardsSearch;
            }

            if (forwardsVolume >= reverseVolume)
            {
                reverseAlongPath = false;
            } else
            {
                reverseAlongPath = true;
            }
        }

        if (!reverseAlongPath)
        {
            wipeTypeComment = "Close - forwards";

            reverseWipePath = false;

            // Add a travel to the closest point
            MovementEvent closestEvent = null;

            int previousMovement = extrusionBuffer.getPreviousMovementEventIndex(closestEventIndex);
            if (previousMovement > 0)
            {
                closestEvent = (MovementEvent) extrusionBuffer.get(previousMovement);

            } else
            {
                closestEvent = (MovementEvent) extrusionBuffer.get(closestEventIndex);
            }

            TravelEvent travelToClosestPoint = new TravelEvent();
            travelToClosestPoint.setX(closestEvent.getX());
            travelToClosestPoint.setY(closestEvent.getY());
            travelToClosestPoint.setComment(originalComment);
            travelToClosestPoint.setFeedRate(wipeFeedRate_mmPerMin);

            extrusionBuffer.add(insertedEventIndex, travelToClosestPoint);
            insertedEventIndex++;

        } else
        {
            if (finalExtrusionEventIndex != modifiedFinalExtrusionEventIndex)
            {
                // We need to travel to the start of the close 
                // Add a travel to the closest point
                TravelEvent travelToClosestPoint = new TravelEvent();
                ExtrusionEvent closestEvent = (ExtrusionEvent) extrusionBuffer.get(
                    modifiedFinalExtrusionEventIndex);
                travelToClosestPoint.setX(closestEvent.getX());
                travelToClosestPoint.setY(closestEvent.getY());
                travelToClosestPoint.setComment("Travelling to start of close");
                travelToClosestPoint.setFeedRate(wipeFeedRate_mmPerMin);

                extrusionBuffer.add(insertedEventIndex, travelToClosestPoint);
                insertedEventIndex++;
            }

            wipeTypeComment = "Close - reverse";

            reverseWipePath = true;

            indexToCopyFrom = modifiedFinalExtrusionEventIndex - 1;

            if (modifiedFirstExtrusionIndex != modifiedFinalExtrusionEventIndex)
            {
                minimumIndexToCopyFrom = modifiedFirstExtrusionIndex;
            } else
            {
                int previousTravelEvent = extrusionBuffer.getPreviousEventIndex(
                    modifiedFinalExtrusionEventIndex, TravelEvent.class);
                if (previousTravelEvent >= 0)
                {
                    minimumIndexToCopyFrom = previousTravelEvent;
                }
            }
        }

        startOfClose = insertedEventIndex;

        double cumulativeExtrusionVolume = 0;

        MovementEvent lastMovement = null;

        int indexDelta = (reverseWipePath == true) ? -1 : 1;

        if (minimumIndexToCopyFrom >= 0)
        {
            boolean startMessageOutput = false;

            while (cumulativeExtrusionVolume < targetVolume
                && indexToCopyFrom <= modifiedFinalExtrusionEventIndex
                && indexToCopyFrom >= minimumIndexToCopyFrom + 1)
            {
                boolean dontIncrementEventIndex = false;

                if (extrusionBuffer.get(indexToCopyFrom) instanceof ExtrusionEvent)
                {
                    ExtrusionEvent eventToCopy = (ExtrusionEvent) extrusionBuffer.get(
                        indexToCopyFrom);

                    double segmentVolume = eventToCopy.getE() + eventToCopy.getD();
                    double volumeDifference = targetVolume - cumulativeExtrusionVolume
                        - segmentVolume;

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
                            fromReferencePosition = getNextPosition(indexToCopyFrom,
                                                                    modifiedFinalExtrusionEventIndex);
                        } else
                        {
                            fromReferencePosition = getLastPosition(indexToCopyFrom);
                        }

                        if (fromReferencePosition != null)
                        {
                            fromPosition = fromReferencePosition;
                        } else
                        {
                            throw new PostProcessingError(
                                "Couldn't locate from position for auto wipe");
                        }

                        Vector2D toPosition = new Vector2D(eventToCopy.getX(),
                                                           eventToCopy.getY());

                        Vector2D actualVector = toPosition.subtract(fromPosition);
                        Vector2D firstSegment = fromPosition.add(segmentAlterationRatio,
                                                                 actualVector);

                        eventToInsert.setX(firstSegment.getX());
                        eventToInsert.setY(firstSegment.getY());
                        eventToInsert.setComment(originalComment + ":" + wipeTypeComment
                            + " - end -");

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
                        eventToInsert.setComment(originalComment + ":" + wipeTypeComment
                            + ((startMessageOutput == false) ? " - start -" : " - in progress -"));
                        startMessageOutput = true;
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
                        dontIncrementEventIndex = true;
                        steno.info("Elided event of type " + event.getClass().getName()
                            + " during close");
                    }
                }

                if (!dontIncrementEventIndex)
                {
                    insertedEventIndex++;
                }

                indexToCopyFrom += indexDelta;
            }
        } else
        {
            steno.warning("Failed to close nozzle correctly");
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

    private boolean replaceOpenNozzleWithPartialOpen(double partialOpenValue)
    {
        boolean success = false;

        int eventSearchIndex = extrusionBuffer.size() - 1;
        while (eventSearchIndex >= 0)
        {
            if (extrusionBuffer.get(eventSearchIndex) instanceof NozzleOpenFullyEvent)
            {
                NozzleOpenFullyEvent originalEvent = (NozzleOpenFullyEvent) extrusionBuffer.get(
                    eventSearchIndex);

                NozzleChangeBValueEvent newBEvent = new NozzleChangeBValueEvent();
                newBEvent.setB(partialOpenValue);
                newBEvent.setComment("Partial open with replenish");
                newBEvent.setE(originalEvent.getE());
                newBEvent.setD(originalEvent.getD());

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
                                          requiredEjectionVolume, 10e-5) == EQUAL)
                        {
                            // No need to split line - replace the current event with a nozzle change event
                            volumeIndex = eventIndex;
                            eventIndices.put(eventType, volumeIndex);
                            break;
                        } else if (compareDouble(volumeConsidered,
                                                 requiredEjectionVolume, 10e-5)
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
                                    fromPosition = new Vector2D(lastProcessedMovementEvent.getX(),
                                                                lastProcessedMovementEvent.getY());
                                }
                            } else
                            {
                                fromPosition = new Vector2D(lastProcessedMovementEvent.getX(),
                                                            lastProcessedMovementEvent.getY());
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
                                          requiredEjectionVolume, 10e-5) == EQUAL)
                        {
                            // No need to split line - replace the current event with a nozzle change event
                            volumeIndex = eventIndex;
                            eventIndices.put(eventType, volumeIndex);
                            break;
                        } else if (compareDouble(volumeConsidered,
                                                 requiredEjectionVolume, 10e-5)
                            == MORE_THAN)
                        {
                            // Split the line
                            double secondSegmentExtrusion = volumeConsidered
                                - requiredEjectionVolume;

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

    private double calculateVolume(ArrayList<GCodeParseEvent> extrusionBufferToAssess,
        int startIndex, int endIndex)
    {
        double totalExtrusionForPath = 0;

        for (int extrusionBufferIndex = startIndex; extrusionBufferIndex <= endIndex; extrusionBufferIndex++)
        {
            GCodeParseEvent event = extrusionBufferToAssess.get(extrusionBufferIndex);

            if (event instanceof ExtrusionEvent)
            {
                totalExtrusionForPath += ((ExtrusionEvent) event).getE() + ((ExtrusionEvent) event).
                    getD();
            }
        }

        return totalExtrusionForPath;
    }
}
