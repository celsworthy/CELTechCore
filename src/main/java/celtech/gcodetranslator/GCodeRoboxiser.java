package celtech.gcodetranslator;

import celtech.gcodetranslator.events.CommentEvent;
import celtech.gcodetranslator.events.EndOfFileEvent;
import celtech.gcodetranslator.events.ExtrusionEvent;
import celtech.gcodetranslator.events.GCodeEvent;
import celtech.gcodetranslator.events.GCodeParseEvent;
import celtech.gcodetranslator.events.LayerChangeEvent;
import celtech.gcodetranslator.events.MCodeEvent;
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.DoubleProperty;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Ian
 */
public class GCodeRoboxiser implements GCodeTranslationEventHandler
{

    private final Stenographer steno = StenographerFactory.getStenographer(GCodeTranslationEventHandler.class.getName());
    private GCodeFileParser gcodeParser = new GCodeFileParser();

    private Pattern passThroughPattern = Pattern.compile("\\b(?:M106 S[0-9.]+|M107|G[0-9]{2,}|M[0-9]{2,}|G28 [XYZ]+[0-9]*)(?:[\\s]*;.*)?");
    private Matcher passThroughMatcher = null;

    private Pattern removePattern = Pattern.compile("\\b(?:M104 S[0-9.]+(?:\\sT[0-9]+)?|M109 S[0-9.]+(?:\\sT[0-9]+)?|M107)(?:[\\s]*;.*)?");
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
    private BufferedWriter fileWriter = null;

    private ArrayList<GCodeParseEvent> extrusionBuffer = new ArrayList<>();
//    private Vector2D precursorPoint = null;

    private boolean triggerCloseFromTravel = false;
    private boolean triggerCloseFromRetract = true;

    private int tempNozzleMemory = -1;
    private int nozzleInUse = -1;
    private boolean forcedNozzle = false;

    private int ejectionVolumeIndex = -1;
    private int wipeVolumeIndex = -1;

    private boolean pastFirstLayer = false;

    public GCodeRoboxiser()
    {
        gcodeParser.addListener(this);
    }

    public boolean roboxiseFile(String inputFilename, String outputFilename, RoboxProfile settings, DoubleProperty percentProgress)
    {
        boolean success = false;

        lastPoint = new Vector2D(0, 0);

        initialTemperaturesWritten = false;
        subsequentLayersTemperaturesWritten = false;
        distanceSoFar = 0;
        totalExtrudedVolume = 0;
        totalXYMovement = 0;
        layer = 0;
        currentFeedrate = 0;
        currentZHeight = 0;

        Nozzle point3mmNozzle = new Nozzle(0,
                                           settings.getNozzle_ejection_volume().get(0).doubleValue(),
                                           settings.getNozzle_wipe_volume().get(0).doubleValue(),
                                           settings.getNozzle_partial_b_minimum().get(0).doubleValue());
        Nozzle point8mmNozzle = new Nozzle(1,
                                           settings.getNozzle_ejection_volume().get(1).doubleValue(),
                                           settings.getNozzle_wipe_volume().get(1).doubleValue(),
                                           settings.getNozzle_partial_b_minimum().get(1).doubleValue());
        nozzles.add(point3mmNozzle);
        nozzles.add(point8mmNozzle);

        currentNozzle = point3mmNozzle;

        try
        {
            File outputFile = new File(outputFilename);
            fileWriter = new BufferedWriter(new FileWriter(outputFile));

            fileWriter.write("; File post-processed by roboxiser\n");

            fileWriter.write(";\n; Pre print gcode\n");
            for (String macroLine : GCodeMacros.getMacroContents("before_print"))
            {
                fileWriter.write(macroLine + "\n");
            }
            fileWriter.write("; End of Pre print gcode\n");

            insertInitialTemperatures();

            gcodeParser.parse(inputFilename, percentProgress);

            fileWriter.close();

            steno.info("Finished roboxising " + inputFilename);
            steno.info("Total extrusion volume " + totalExtrudedVolume + " mm3");
            steno.info("Total XY movement distance " + totalXYMovement + " mm");

            success = true;
        } catch (IOException ex)
        {
            steno.error("Error roboxising file " + inputFilename);
        }

        return success;
    }

    @Override
    public void unableToParse(String line)
    {
        try
        {
            if ((removeMatcher = removePattern.matcher(line)).matches())
            {
                steno.info("Removed " + line);
                fileWriter.write("; Removed: " + line);
            } else if ((passThroughMatcher = passThroughPattern.matcher(line)).matches())
            {
                fileWriter.write(line);
                fileWriter.newLine();
            } else
            {
                steno.warning("Unable to parse " + line);
                fileWriter.write("; >>>ERROR PARSING: " + line);
                fileWriter.newLine();
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
            firstLayerBedTemp.setComment("take 1st layer bed temperature from reel");
            extrusionBuffer.add(firstLayerBedTemp);

            MCodeEvent waitForBedTemp = new MCodeEvent();
            waitForBedTemp.setMNumber(190);
            waitForBedTemp.setComment("wait for bed temperature to be reached");
            extrusionBuffer.add(waitForBedTemp);

            MCodeEvent firstLayerNozzleTemp = new MCodeEvent();
            firstLayerNozzleTemp.setMNumber(103);
            firstLayerNozzleTemp.setComment("take 1st layer nozzle temperature from loaded reel");
            extrusionBuffer.add(firstLayerNozzleTemp);

            MCodeEvent waitForNozzleTemp = new MCodeEvent();
            waitForNozzleTemp.setMNumber(109);
            waitForNozzleTemp.setComment("wait for nozzle temperature to be reached");
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
            subsequentLayerNozzleTemp.setComment("take post layer 1 nozzle temperature from loaded reel - don't wait");
            extrusionBuffer.add(subsequentLayerNozzleTemp);

            MCodeEvent subsequentLayerBedTemp = new MCodeEvent();
            subsequentLayerBedTemp.setMNumber(140);
            subsequentLayerBedTemp.setComment("take post layer 1 bed temperature from loaded reel - don't wait");
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
    public void processEvent(GCodeParseEvent event) throws NozzleCloseSettingsError
    {
        //Buffer extrusion events only
        // Triggers to empty the buffer are written after the buffer has been dealt with
        // Non-triggers are written immediately

        Vector2D currentPoint = null;
        double distance = 0;

        try
        {

            if (event instanceof ExtrusionEvent)
            {
//            if (extrusionBuffer.size() == 0)
//            {
//                precursorPoint = lastPoint;
//            }

                ExtrusionEvent extrusionEvent = (ExtrusionEvent) event;
                currentPoint = new Vector2D(extrusionEvent.getX(), extrusionEvent.getY());

                // Open the nozzle if it isn't already open
                // This will always be a single event prior to extrusion
                if (currentNozzle.getState() != NozzleState.OPEN)
                {
                    NozzleOpenFullyEvent openNozzle = new NozzleOpenFullyEvent();
                    openNozzle.setComment("extrusion trigger");
                    extrusionBuffer.add(openNozzle);
                    currentNozzle.openNozzleFully();
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
                currentPoint = new Vector2D(extrusionEvent.getX(), extrusionEvent.getY());

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
                currentPoint = new Vector2D(travelEvent.getX(), travelEvent.getY());

                if (lastPoint != null)
                {
                    distance = lastPoint.distance(currentPoint);
                    travelEvent.setLength(distance);
//                        System.out.println("Distance " + distance);
                    distanceSoFar += distance;
                    totalXYMovement += distance;
//                        System.out.println("Total Distance " + distanceSoFar);

                    if (triggerCloseFromTravel == true && (currentNozzle.getState() != NozzleState.CLOSED && distance > currentNozzle.getAllowedTravelBeforeClose()))
                    {
                        writeEventsWithNozzleClose(lastPoint, "travel trigger");
                    }
                    extrusionBuffer.add(event);
                }
                lastPoint = currentPoint;
            } else if (event instanceof LayerChangeEvent)
            {
                LayerChangeEvent layerChangeEvent = (LayerChangeEvent) event;

                currentZHeight = layerChangeEvent.getZ();

                if (layer == 1)
                {
                    writeEventsWithNozzleClose(lastPoint, "closing nozzle after forced nozzle select on layer 0");
                    insertSubsequentLayerTemperatures();
                    NozzleChangeEvent nozzleChangeEvent = new NozzleChangeEvent();
                    nozzleChangeEvent.setNozzleNumber(tempNozzleMemory);
                    nozzleChangeEvent.setComment("return to intended nozzle");
                    extrusionBuffer.add(nozzleChangeEvent);
                    pastFirstLayer = true;
                }

                layer++;

//            if (currentNozzle.getState() != NozzleState.CLOSED)
//            {
//                writeEventsWithNozzleClose(precursorPoint, "layer change trigger");
//            } else
                {
                    extrusionBuffer.add(event);

//                    NozzleChangeEvent nozzleChangeEvent = new NozzleChangeEvent();
//                    nozzleChangeEvent.setComment("Auto nozzle select - homing");
//                    nozzleChangeEvent.setNozzleNumber(tempNozzleMemory.getReferenceNumber());
//                    extrusionBuffer.add(nozzleChangeEvent);
                }
            } else if (event instanceof NozzleChangeEvent)
            {
                NozzleChangeEvent nozzleChangeEvent = (NozzleChangeEvent) event;

                if (layer == 0 && forcedNozzle == false)
                {
                    tempNozzleMemory = nozzleChangeEvent.getNozzleNumber();
//Force to nozzle 1
                    nozzleChangeEvent.setNozzleNumber(1);
                    nozzleChangeEvent.setComment(nozzleChangeEvent.getComment() + " - force to nozzle 1 on first layer");
                    extrusionBuffer.add(nozzleChangeEvent);
                    nozzleInUse = 1;
                    forcedNozzle = true;
                } else if (layer == 1)
                {
                    tempNozzleMemory = nozzleChangeEvent.getNozzleNumber();
                } else if (layer > 1)
                {
                    extrusionBuffer.add(nozzleChangeEvent);
                    nozzleInUse = nozzleChangeEvent.getNozzleNumber();
                }

//                Nozzle newNozzle = nozzles.get()
//                tempNozzleMemory = currentNozzle;
//                if (currentNozzle == null)
//                {
//                    extrusionBuffer.add(event);
//                    currentNozzle = newNozzle;
//                } else
//                {
//                    if (newNozzle != currentNozzle)
//                    {
//                        if (currentNozzle.getState() != NozzleState.CLOSED)
//                        {
//                            writeEventsWithNozzleClose(lastPoint, "nozzle change trigger");
//                        }
//                        extrusionBuffer.add(event);
//
//                        currentNozzle = newNozzle;
//                    }
//                }
            } else if (event instanceof RetractEvent)
            {
                RetractEvent retractEvent = (RetractEvent) event;

                totalExtrudedVolume += retractEvent.getE();

                if (triggerCloseFromRetract == true && currentNozzle.getState() != NozzleState.CLOSED)
                {
                    writeEventsWithNozzleClose(lastPoint, "retract trigger");
                }
                extrusionBuffer.add(event);

                resetMeasuringThing();
            } else if (event instanceof UnretractEvent)
            {
                UnretractEvent unretractEvent = (UnretractEvent) event;

                totalExtrudedVolume += unretractEvent.getE();

                if (currentNozzle.getState() != NozzleState.OPEN)
                {
                    NozzleOpenFullyEvent openNozzle = new NozzleOpenFullyEvent();
                    openNozzle.setComment("unretract trigger");
                    extrusionBuffer.add(openNozzle);
                    currentNozzle.openNozzleFully();
                }

                resetMeasuringThing();
                extrusionBuffer.add(event);
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
                    writeEventsWithNozzleClose(lastPoint, "End of file");
                }

                try
                {
                    fileWriter.write(";\n; Post print gcode\n");
                    for (String macroLine : GCodeMacros.getMacroContents("after_print"))
                    {
                        fileWriter.write(macroLine + "\n");
                    }
                    fileWriter.write("; End of Post print gcode\n");
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

    private void writeEventToFile(GCodeParseEvent event)
    {
        try
        {
            fileWriter.write(event.renderForOutput());
            fileWriter.flush();
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

    private void writeEventsWithNozzleClose(Vector2D precursorPoint, String comment) throws NozzleCloseSettingsError
    {
        boolean closeAtEndOfPath = false;

        ejectionVolumeIndex = -1;
        double ejectionVolumeConsidered = 0;
        wipeVolumeIndex = -1;
        double wipeVolumeConsidered = 0;

        double nozzleStartPosition = 1.0;
        double nozzleCloseOverVolume = 1;

        try
        {

            // Bit inefficient doing this here - move later so the checks are carried out once only when the parameters are loaded
            if (compareDouble(currentNozzle.getEjectionVolume(), currentNozzle.getWipeVolume()) == EQUAL && currentNozzle.getEjectionVolume() > 0 && currentNozzle.getWipeVolume() > 0)
            {
                CommentEvent commentEvent = new CommentEvent();
                commentEvent.setComment("ERROR -- Ejection volume and wipe volume are greater than zero and equal");
                writeEventToFile(commentEvent);
                throw new NozzleCloseSettingsError("Ejection volume and wipe volume are greater than zero and equal");
            }

            if (compareDouble(currentNozzle.getEjectionVolume(), currentNozzle.getWipeVolume()) == LESS_THAN)
            {
                CommentEvent commentEvent = new CommentEvent();
                commentEvent.setComment("ERROR -- Start is less than finish");
                writeEventToFile(commentEvent);
                throw new NozzleCloseSettingsError("Start is less than finish");
            }

            if (closeAtEndOfPath || (currentNozzle.getEjectionVolume() == 0 && currentNozzle.getWipeVolume() == 0))
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

                for (GCodeParseEvent event : extrusionBuffer)
                {
                    if (event instanceof ExtrusionEvent)
                    {
                        totalExtrusionForPath += ((ExtrusionEvent) event).getE();
                    }
                }

                if (totalExtrusionForPath > currentNozzle.getEjectionVolume() + currentNozzle.getWipeVolume())
                {
                    //OK - we're go for a normal close   
                    nozzleStartPosition = 1.0;
                    nozzleCloseOverVolume = currentNozzle.getEjectionVolume();
                    findWipeIndex(currentNozzle.getWipeVolume(), precursorPoint, comment);
                    findEjectionIndex(currentNozzle.getEjectionVolume() + currentNozzle.getWipeVolume(), precursorPoint, comment);
                } else
                {
                    //Keep the wipe volume until the minimum B is exceeded
                    double extrusionVolumeAfterWipe = totalExtrusionForPath - currentNozzle.getWipeVolume();
                    double bValue = extrusionVolumeAfterWipe / currentNozzle.getEjectionVolume();
                    double minimumBEjectionVolume = currentNozzle.getEjectionVolume() * currentNozzle.getPartialBMinimum();

                    if (bValue < currentNozzle.getPartialBMinimum())
                    {
                        //Shorten the wipe
                        double requiredWipeVolume = totalExtrusionForPath - minimumBEjectionVolume;

                        if (requiredWipeVolume <= 0)
                        {
                            //Not enough volume for the wipe even at minimum B
                            nozzleStartPosition = currentNozzle.getPartialBMinimum();
                            nozzleCloseOverVolume = totalExtrusionForPath;
                            replaceOpenNozzleWithPartialOpen(currentNozzle.getPartialBMinimum());
                            ejectionVolumeIndex = getNextExtrusionEventIndex(0);
                            extrusionBuffer.get(ejectionVolumeIndex).setComment("Short path");
                            wipeVolumeIndex = getPreviousExtrusionEventIndex(extrusionBuffer.size() - 1);
                        } else
                        {
                            //We can use a shortened wipe with minimum B
                            nozzleStartPosition = currentNozzle.getPartialBMinimum();
                            nozzleCloseOverVolume = minimumBEjectionVolume;
                            replaceOpenNozzleWithPartialOpen(currentNozzle.getPartialBMinimum());
                            findWipeIndex(requiredWipeVolume, precursorPoint, comment);
                            ejectionVolumeIndex = getNextExtrusionEventIndex(0);
                            extrusionBuffer.get(ejectionVolumeIndex).setComment("Shortened wipe volume");
                        }
                    } else
                    {
                        //Retain the full wipe but open partially and use a proportionately smaller ejection volume
                        nozzleStartPosition = bValue;
                        nozzleCloseOverVolume = extrusionVolumeAfterWipe;
                        replaceOpenNozzleWithPartialOpen(bValue);
                        findWipeIndex(currentNozzle.getWipeVolume(), precursorPoint, comment);
                        findEjectionIndex(extrusionVolumeAfterWipe + currentNozzle.getWipeVolume(), precursorPoint, comment);
                        extrusionBuffer.get(ejectionVolumeIndex).setComment("Partial open - full wipe volume");
                    }
                }

                if (ejectionVolumeIndex != -1 && wipeVolumeIndex != -1)
                {
                    // We've done it!
                    // Output the extrusion data and break out of the loop

                    {

                        int foundRetractDuringExtrusion = -1;
                        int foundNozzleChange = -1;
                        double currentNozzlePosition = nozzleStartPosition;

                        for (int tSearchIndex = extrusionBuffer.size() - 1; tSearchIndex > wipeVolumeIndex; tSearchIndex--)
                        {

                            GCodeParseEvent event = extrusionBuffer.get(tSearchIndex);
                            if (event instanceof RetractDuringExtrusionEvent && foundRetractDuringExtrusion < 0)
                            {
                                foundRetractDuringExtrusion = tSearchIndex;
                            }

                            if (event instanceof NozzleChangeEvent && foundRetractDuringExtrusion >= 0)
                            {
                                foundNozzleChange = tSearchIndex;
                                break;
                            }
                        }

                        if (foundNozzleChange >= 0 && foundRetractDuringExtrusion >= 0)
                        {
                            NozzleChangeEvent eventToMove = (NozzleChangeEvent) extrusionBuffer.get(foundNozzleChange);
                            extrusionBuffer.remove(foundNozzleChange);
                            extrusionBuffer.add(foundRetractDuringExtrusion, eventToMove);
                        }

                        for (int eventWriteIndex = 0; eventWriteIndex < extrusionBuffer.size(); eventWriteIndex++)
                        {
                            GCodeParseEvent candidateevent = extrusionBuffer.get(eventWriteIndex);

                            if (candidateevent instanceof ExtrusionEvent)
                            {
                                ExtrusionEvent event = (ExtrusionEvent) candidateevent;

                                if (eventWriteIndex == wipeVolumeIndex && eventWriteIndex == ejectionVolumeIndex)
                                {
                                    // No extrusion
                                    // Proportional B value
                                    NozzlePositionChangeEvent nozzleEvent = new NozzlePositionChangeEvent();
                                    nozzleEvent.setX(event.getX());
                                    nozzleEvent.setY(event.getY());
                                    nozzleEvent.setLength(event.getLength());
                                    nozzleEvent.setFeedRate(event.getFeedRate());
                                    nozzleEvent.setComment(event.getComment() + " after start of close");
                                    nozzleStartPosition = 0;
                                    nozzleEvent.setB(0);
                                    nozzleEvent.setNoExtrusionFlag(true);
                                    writeEventToFile(nozzleEvent);
                                } else if (eventWriteIndex >= wipeVolumeIndex)
                                {
                                    // No extrusion
                                    // No B
                                    TravelEvent noBNoETravel = new TravelEvent();
                                    noBNoETravel.setX(event.getX());
                                    noBNoETravel.setY(event.getY());
                                    noBNoETravel.setLength(event.getLength());
                                    noBNoETravel.setFeedRate(event.getFeedRate());
                                    noBNoETravel.setComment(event.getComment() + " after finish of close");
                                    writeEventToFile(noBNoETravel);
                                } else if (eventWriteIndex >= ejectionVolumeIndex)
                                {
                                    // No extrusion
                                    // Proportional B value
                                    NozzlePositionChangeEvent nozzleEvent = new NozzlePositionChangeEvent();
                                    nozzleEvent.setX(event.getX());
                                    nozzleEvent.setY(event.getY());
                                    nozzleEvent.setLength(event.getLength());
                                    nozzleEvent.setFeedRate(event.getFeedRate());
                                    nozzleEvent.setComment(event.getComment() + " after start of close");
                                    currentNozzlePosition = currentNozzlePosition - (nozzleStartPosition * (event.getE() / nozzleCloseOverVolume));
                                    if (compareDouble(currentNozzlePosition, 0) == EQUAL)
                                    {
                                        currentNozzlePosition = 0;
                                    }
                                    nozzleEvent.setB(currentNozzlePosition);
                                    nozzleEvent.setNoExtrusionFlag(true);
                                    writeEventToFile(nozzleEvent);
                                } else
                                {
                                    writeEventToFile(event);
                                }
                            } else
                            {
                                writeEventToFile(candidateevent);
                                if (candidateevent instanceof NozzleChangeEvent)
                                {
                                    Nozzle newNozzle = nozzles.get(((NozzleChangeEvent) candidateevent).getNozzleNumber());
                                    currentNozzle = newNozzle;
                                }
                            }
                        }
                    }

                    extrusionBuffer.clear();

                    currentNozzle.closeNozzleFully();

                } else
                {
                    CommentEvent failureComment = new CommentEvent();
                    failureComment.setComment("Error locating start / end of close");
                    writeEventToFile(failureComment);
                    throw new NozzleCloseSettingsError("Didn't locate start / end of close");
                }
            }
        } catch (Exception e)
        {
            steno.error("Exception whilst attempting nozzle close");

            e.printStackTrace();
        }
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

    private ExtrusionEvent getPreviousExtrusionEvent(int eventIndex)
    {
        ExtrusionEvent foundEvent = null;

        for (int index = eventIndex - 1; index >= 0; index--)
        {
            if (extrusionBuffer.get(index) instanceof ExtrusionEvent)
            {
                foundEvent = (ExtrusionEvent) extrusionBuffer.get(index);
                break;
            }
        }

        return foundEvent;
    }

    private void findWipeIndex(double requiredWipeVolume, Vector2D precursorPoint, String comment)
    {
        int eventIndex = extrusionBuffer.size() - 1;
        double wipeVolumeConsidered = 0;

        while (eventIndex >= 0)
        {
            if (extrusionBuffer.get(eventIndex) instanceof ExtrusionEvent)
            {
                ExtrusionEvent currentEvent = (ExtrusionEvent) extrusionBuffer.get(eventIndex);

                double segmentExtrusion = currentEvent.getE();
                wipeVolumeConsidered += segmentExtrusion;

                if (wipeVolumeIndex == -1)
                {
                    if (requiredWipeVolume == 0)
                    {
                        // Special case where the finish of the close is at the end of the path
                        wipeVolumeIndex = eventIndex;
                        break;
                    } else if (compareDouble(wipeVolumeConsidered, requiredWipeVolume) == EQUAL)
                    {
                        // The specified finish is at the start of this segment
                        // and therefore at the end of the previous segment
                        // The previous segment should have the close command
                        // This segment changes to be a travel (no extrusion no nozzle position change)

                        wipeVolumeIndex = eventIndex;
                        break;
                    } else if (compareDouble(wipeVolumeConsidered, requiredWipeVolume) == MORE_THAN)
                    {
                        // The nozzle must close before the end of this segment
                        // Divide the segment
                        // Make the final part of the segment a travel only event keeping the current xy
                        // Add a new nozzle close event and replace the first part of the segment

                        double partialVolume = wipeVolumeConsidered - requiredWipeVolume;
                        double scaleFactor = partialVolume / segmentExtrusion;

                        Vector2D fromPosition = null;

                        if (eventIndex > 0)
                        {
                            ExtrusionEvent previousEvent = getPreviousExtrusionEvent(eventIndex);
                            if (previousEvent != null)
                            {
                                fromPosition = new Vector2D(previousEvent.getX(), previousEvent.getY());
                            } else
                            {
                                fromPosition = precursorPoint;
                            }
                        } else
                        {
                            fromPosition = precursorPoint;
                        }

                        Vector2D toPosition = new Vector2D(currentEvent.getX(), currentEvent.getY());
                        Vector2D actualVector = toPosition.subtract(fromPosition);
                        Vector2D firstSegment = fromPosition.add(scaleFactor, actualVector);

                        ExtrusionEvent initialEvent = new ExtrusionEvent();
                        initialEvent.setX(firstSegment.getX());
                        initialEvent.setY(firstSegment.getY());
                        initialEvent.setLength(((ExtrusionEvent) currentEvent).getLength() * scaleFactor);
                        initialEvent.setFeedRate(currentEvent.getFeedRate());
                        initialEvent.setE(segmentExtrusion * scaleFactor);

                        ExtrusionEvent subsequentEvent = new ExtrusionEvent();
                        subsequentEvent.setX(currentEvent.getX());
                        subsequentEvent.setY(currentEvent.getY());
                        subsequentEvent.setLength(((ExtrusionEvent) currentEvent).getLength() * (1 - scaleFactor));
                        subsequentEvent.setFeedRate(currentEvent.getFeedRate());
                        subsequentEvent.setE(segmentExtrusion - initialEvent.getE());

                        extrusionBuffer.add(eventIndex, initialEvent);
                        extrusionBuffer.remove(eventIndex + 1);
                        extrusionBuffer.add(eventIndex + 1, subsequentEvent);

                        wipeVolumeIndex = eventIndex + 1;

                        break;
                    }
                }

            }
            eventIndex--;
        }
    }

    private void findEjectionIndex(double requiredEjectionVolume, Vector2D precursorPoint, String comment)
    {
        int eventIndex = extrusionBuffer.size() - 1;
        double ejectionVolumeConsidered = 0;

        while (eventIndex >= 0)
        {
            if (extrusionBuffer.get(eventIndex) instanceof ExtrusionEvent)
            {
                ExtrusionEvent currentEvent = (ExtrusionEvent) extrusionBuffer.get(eventIndex);

                double segmentExtrusion = currentEvent.getE();
                ejectionVolumeConsidered += segmentExtrusion;

                if (ejectionVolumeIndex == -1)
                {
                    if (compareDouble(ejectionVolumeConsidered, requiredEjectionVolume) == EQUAL)
                    {
                        // No need to split line - replace the current event with a nozzle change event
                        ejectionVolumeIndex = eventIndex;
                        break;
                    } else if (compareDouble(ejectionVolumeConsidered, requiredEjectionVolume) == MORE_THAN)
                    {
                        // Split the line
                        double initialSegmentExtrusion = ejectionVolumeConsidered - requiredEjectionVolume;
                        double scaleFactor = initialSegmentExtrusion / segmentExtrusion;

                        Vector2D fromPosition = null;

                        if (eventIndex > 0)
                        {
                            ExtrusionEvent previousEvent = getPreviousExtrusionEvent(eventIndex);
                            if (previousEvent != null)
                            {
                                fromPosition = new Vector2D(previousEvent.getX(), previousEvent.getY());
                            } else
                            {
                                fromPosition = precursorPoint;
                            }
                        } else
                        {
                            fromPosition = precursorPoint;
                        }

                        Vector2D toPosition = new Vector2D(currentEvent.getX(), currentEvent.getY());
                        Vector2D actualVector = toPosition.subtract(fromPosition);
                        Vector2D firstSegment = fromPosition.add(scaleFactor, actualVector);

                        ExtrusionEvent firstSegmentExtrusionEvent = new ExtrusionEvent();
                        firstSegmentExtrusionEvent.setComment(comment + " split segment - before start of nozzle close");
                        firstSegmentExtrusionEvent.setX(firstSegment.getX());
                        firstSegmentExtrusionEvent.setY(firstSegment.getY());
                        firstSegmentExtrusionEvent.setE(segmentExtrusion * scaleFactor);
                        firstSegmentExtrusionEvent.setLength(((ExtrusionEvent) currentEvent).getLength() * scaleFactor);
                        firstSegmentExtrusionEvent.setFeedRate(currentEvent.getFeedRate());

                        ExtrusionEvent secondSegmentExtrusionEvent = new ExtrusionEvent();
                        secondSegmentExtrusionEvent.setComment(comment + " split segment - after start of nozzle close");
                        secondSegmentExtrusionEvent.setX(currentEvent.getX());
                        secondSegmentExtrusionEvent.setY(currentEvent.getY());
                        secondSegmentExtrusionEvent.setE(segmentExtrusion - firstSegmentExtrusionEvent.getE());
                        secondSegmentExtrusionEvent.setLength(((ExtrusionEvent) currentEvent).getLength() * (1 - scaleFactor));
                        secondSegmentExtrusionEvent.setFeedRate(currentEvent.getFeedRate());

                        if (wipeVolumeIndex >= eventIndex)
                        {
                            wipeVolumeIndex++;
                        }
                        extrusionBuffer.add(eventIndex, firstSegmentExtrusionEvent);
                        extrusionBuffer.remove(eventIndex + 1);
                        extrusionBuffer.add(eventIndex + 1, secondSegmentExtrusionEvent);

                        ejectionVolumeIndex = eventIndex + 1;
                        break;
                    }
                }
            }
            eventIndex--;
        }
    }
}
