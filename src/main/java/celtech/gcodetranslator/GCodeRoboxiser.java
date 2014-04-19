package celtech.gcodetranslator;

import celtech.gcodetranslator.events.CommentEvent;
import celtech.gcodetranslator.events.EndOfFileEvent;
import celtech.gcodetranslator.events.ExtrusionEvent;
import celtech.gcodetranslator.events.GCodeEvent;
import celtech.gcodetranslator.events.GCodeParseEvent;
import celtech.gcodetranslator.events.LayerChangeEvent;
import celtech.gcodetranslator.events.MCodeEvent;
import celtech.gcodetranslator.events.NozzleChangeEvent;
import celtech.gcodetranslator.events.NozzleCloseFullyEvent;
import celtech.gcodetranslator.events.NozzleOpenFullyEvent;
import celtech.gcodetranslator.events.NozzlePositionChangeEvent;
import celtech.gcodetranslator.events.RetractEvent;
import celtech.gcodetranslator.events.TravelEvent;
import celtech.gcodetranslator.events.UnretractEvent;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.services.slicer.SlicerSettings;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.FloatProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
    private Vector2D precursorPoint = null;

    private boolean triggerCloseFromTravel = false;
    private boolean triggerCloseFromRetract = true;

    public GCodeRoboxiser()
    {
        gcodeParser.addListener(this);
    }

    public boolean roboxiseFile(String inputFilename, String outputFilename, SlicerSettings settings)
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

        Nozzle point3mmNozzle = new Nozzle(0, settings.getNozzle_start_close_by().get(0).doubleValue(), settings.getNozzle_finish_close_by().get(0).doubleValue());
        Nozzle point8mmNozzle = new Nozzle(1, settings.getNozzle_start_close_by().get(1).doubleValue(), settings.getNozzle_finish_close_by().get(1).doubleValue());
        nozzles.add(point3mmNozzle);
        nozzles.add(point8mmNozzle);

        try
        {
            File outputFile = new File(outputFilename);
            fileWriter = new BufferedWriter(new FileWriter(outputFile));

            fileWriter.write("; File post-processed by roboxiser\n");

            fileWriter.write(";\n; Pre print gcode\n");
            for (String macroLine : GCodeMacros.PRE_PRINT.getMacroContents())
            {
                fileWriter.write(macroLine + "\n");
            }
            fileWriter.write("; End of Pre print gcode\n");

            insertInitialTemperatures();

            gcodeParser.parse(inputFilename);

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
            extrusionBuffer.add(subsequentLayerNozzleTemp);

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

        if (event instanceof ExtrusionEvent)
        {
            if (extrusionBuffer.size() == 0)
            {
                precursorPoint = lastPoint;
            }

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
                    writeEventsWithNozzleClose(precursorPoint, "travel trigger");
                } else
                {
                    extrusionBuffer.add(event);
                }
            }
            lastPoint = currentPoint;
        } else if (event instanceof LayerChangeEvent)
        {
            LayerChangeEvent layerChangeEvent = (LayerChangeEvent) event;

            currentZHeight = layerChangeEvent.getZ();

            if (layer == 1)
            {
                insertSubsequentLayerTemperatures();
            }

            layer++;

            if (currentNozzle.getState() != NozzleState.CLOSED)
            {
                writeEventsWithNozzleClose(precursorPoint, "layer change trigger");
            } else
            {
                extrusionBuffer.add(event);
            }
        } else if (event instanceof NozzleChangeEvent)
        {
            NozzleChangeEvent nozzleChangeEvent = (NozzleChangeEvent) event;

            Nozzle newNozzle = nozzles.get(nozzleChangeEvent.getNozzleNumber());
            if (currentNozzle == null)
            {
                extrusionBuffer.add(event);
                currentNozzle = newNozzle;
            } else
            {
                if (newNozzle != currentNozzle)
                {
                    if (currentNozzle.getState() != NozzleState.CLOSED)
                    {
                        writeEventsWithNozzleClose(precursorPoint, "nozzle change trigger");
                    } else
                    {
                        extrusionBuffer.add(event);
                    }
                    currentNozzle = newNozzle;
                }
            }
        } else if (event instanceof RetractEvent)
        {
            RetractEvent retractEvent = (RetractEvent) event;

            totalExtrudedVolume += retractEvent.getE();

            if (triggerCloseFromRetract == true && currentNozzle.getState() != NozzleState.CLOSED)
            {
                writeEventsWithNozzleClose(precursorPoint, "retract trigger");
                writeEventToFile(event);
            } else
            {
                extrusionBuffer.add(event);
            }
            resetMeasuringThing();
        } else if (event instanceof UnretractEvent)
        {
            UnretractEvent unretractEvent = (UnretractEvent) event;

            totalExtrudedVolume += unretractEvent.getE();

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
        } else if (event instanceof EndOfFileEvent)
        {
            if (currentNozzle.getState() != NozzleState.CLOSED)
            {
                writeEventsWithNozzleClose(precursorPoint, "End of file");
            }

            try
            {
                fileWriter.write(";\n; Post print gcode\n");
                for (String macroLine : GCodeMacros.POST_PRINT.getMacroContents())
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

        int startCloseAt = -1;
        int finishCloseAt = -1;

        double currentNozzlePosition = 1.0;

        // Bit inefficient doing this here - move later so the checks are carried out once only when the parameters are loaded
        if (compareDouble(currentNozzle.getStartCloseBy(), currentNozzle.getFinishCloseBy()) == EQUAL && currentNozzle.getStartCloseBy() > 0 && currentNozzle.getFinishCloseBy() > 0)
        {
            throw new NozzleCloseSettingsError("Start and finish close are equal and greater than 0");
        }

        if (compareDouble(currentNozzle.getStartCloseBy(), currentNozzle.getFinishCloseBy()) == LESS_THAN)
        {
            throw new NozzleCloseSettingsError("Start is less than finish");
        }

        if (closeAtEndOfPath || (currentNozzle.getStartCloseBy() == 0 && currentNozzle.getFinishCloseBy() == 0))
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
            int eventIndex = extrusionBuffer.size() - 1;
            double lengthConsidered = 0;

            while (eventIndex >= 0)
            {
                if (extrusionBuffer.get(eventIndex) instanceof ExtrusionEvent)
                {
                    ExtrusionEvent currentEvent = (ExtrusionEvent) extrusionBuffer.get(eventIndex);

                    double segmentLength = currentEvent.getLength();
                    lengthConsidered += segmentLength;

                    if (finishCloseAt == -1)
                    {
                        if (currentNozzle.getFinishCloseBy() == 0)
                        {
                        // Special case where the finish of the close is at the end of the path

                            // No need to split line - replace the current event with a nozzle change event
//                        NozzlePositionChangeEvent nozzlePositionChangeEvent = new NozzlePositionChangeEvent();
//                        nozzlePositionChangeEvent.setX(currentEvent.getX());
//                        nozzlePositionChangeEvent.setY(currentEvent.getY());
//                        nozzlePositionChangeEvent.setLength(currentEvent.getLength());
//                        nozzlePositionChangeEvent.setB(0);
//                        nozzlePositionChangeEvent.setNoExtrusionFlag(true);
//                        extrusionBuffer.add(eventIndex, nozzlePositionChangeEvent);
//                        extrusionBuffer.remove(eventIndex + 1);
                            finishCloseAt = eventIndex;
                            break;
                        } else if (compareDouble(lengthConsidered, currentNozzle.getFinishCloseBy()) == EQUAL)
                        {
                            // The specified finish is at the start of this segment
                            // and therefore at the end of the previous segment
                            // The previous segment should have the close command
                            // This segment changes to be a travel (no extrusion no nozzle position change)
//                        TravelEvent noBNoETravel = new TravelEvent();
//                        noBNoETravel.setX(currentEvent.getX());
//                        noBNoETravel.setY(currentEvent.getY());
//                        noBNoETravel.setLength(currentEvent.getLength());
//                        noBNoETravel.setFeedRate(currentEvent.getFeedRate());
//
//                        extrusionBuffer.add(eventIndex, noBNoETravel);
//                        extrusionBuffer.remove(eventIndex + 1);

                            finishCloseAt = eventIndex;
                            break;
                        } else if (compareDouble(lengthConsidered, currentNozzle.getFinishCloseBy()) == MORE_THAN)
                        {
                            // The nozzle must close before the end of this segment
                            // Divide the segment
                            // Make the final part of the segment a travel only event keeping the current xy
                            // Add a new nozzle close event and replace the first part of the segment

                            double partialDistance = lengthConsidered - currentNozzle.getFinishCloseBy();
                            double scaleFactor = partialDistance / segmentLength;

                            Vector2D fromPosition = null;

                            if (eventIndex > 0)
                            {
                                ExtrusionEvent previousEvent = getPreviousExtrusionEvent(eventIndex);
                                if (previousEvent != null)
                                {
                                    fromPosition = new Vector2D(previousEvent.getX(), previousEvent.getY());
                                } else
                                {
                                    steno.error("Couldn't get previous event");
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
                            initialEvent.setLength(segmentLength * scaleFactor);
                            initialEvent.setFeedRate(currentEvent.getFeedRate());
                            initialEvent.setE(((ExtrusionEvent) currentEvent).getE() * scaleFactor);

                            ExtrusionEvent subsequentEvent = new ExtrusionEvent();
                            subsequentEvent.setX(currentEvent.getX());
                            subsequentEvent.setY(currentEvent.getY());
                            subsequentEvent.setLength(segmentLength - initialEvent.getLength());
                            subsequentEvent.setFeedRate(currentEvent.getFeedRate());
                            subsequentEvent.setE(((ExtrusionEvent) currentEvent).getE() * (1 - scaleFactor));

//                        NozzlePositionChangeEvent nozzlePositionChangeEvent = new NozzlePositionChangeEvent();
//                        nozzlePositionChangeEvent.setX(firstSegment.getX());
//                        nozzlePositionChangeEvent.setY(firstSegment.getY());
//                        nozzlePositionChangeEvent.setB(0);
//                        nozzlePositionChangeEvent.setLength(actualVector.getNorm() * scaleFactor);
//                        nozzlePositionChangeEvent.setNoExtrusionFlag(true);
//                        nozzlePositionChangeEvent.setFeedRate(currentEvent.getFeedRate());
//                        TravelEvent noBNoETravel = new TravelEvent();
//                        noBNoETravel.setX(currentEvent.getX());
//                        noBNoETravel.setY(currentEvent.getY());
//                        noBNoETravel.setLength(segmentLength - nozzlePositionChangeEvent.getLength());
//                        noBNoETravel.setFeedRate(currentEvent.getFeedRate());
//                        extrusionBuffer.add(eventIndex, nozzlePositionChangeEvent);
//                        extrusionBuffer.remove(eventIndex + 1);
//                        extrusionBuffer.add(eventIndex + 1, noBNoETravel);
                            extrusionBuffer.add(eventIndex, initialEvent);
                            extrusionBuffer.remove(eventIndex + 1);
                            extrusionBuffer.add(eventIndex + 1, subsequentEvent);

                            finishCloseAt = eventIndex + 1;
                            break;
                        }
                    }

                }
                eventIndex--;
            }

            if (finishCloseAt == -1 && lengthConsidered < currentNozzle.getFinishCloseBy())
            {
                //Trigger small path behaviour - close across the length of the path
                finishCloseAt = extrusionBuffer.size() - 1;
            }

            eventIndex = extrusionBuffer.size() - 1;
            lengthConsidered = 0;

            while (eventIndex >= 0)
            {
                if (extrusionBuffer.get(eventIndex) instanceof ExtrusionEvent)
                {
                    ExtrusionEvent currentEvent = (ExtrusionEvent) extrusionBuffer.get(eventIndex);

                    double segmentLength = currentEvent.getLength();
                    lengthConsidered += segmentLength;

                    if (startCloseAt == -1)
                    {
                        if (compareDouble(lengthConsidered, currentNozzle.getStartCloseBy()) == EQUAL)
                        {
                            // No need to split line - replace the current event with a nozzle change event
//                        NozzlePositionChangeEvent nozzlePositionChangeEvent = new NozzlePositionChangeEvent();
//                        nozzlePositionChangeEvent.setB(0);
//                        nozzlePositionChangeEvent.setNoExtrusionFlag(true);
//                        nozzlePositionChangeEvent.setX(currentEvent.getX());
//                        nozzlePositionChangeEvent.setY(currentEvent.getY());
//                        nozzlePositionChangeEvent.setLength(currentEvent.getLength());
//
//                        extrusionBuffer.add(eventIndex, nozzlePositionChangeEvent);
//                        extrusionBuffer.remove(eventIndex + 1);

                            startCloseAt = eventIndex;
                            break;
                        } else if (compareDouble(lengthConsidered, currentNozzle.getStartCloseBy()) == MORE_THAN)
                        {
                            // Split the line
                            double initialSegmentLength = lengthConsidered - currentNozzle.getStartCloseBy();
                            double scaleFactor = initialSegmentLength / segmentLength;

                            Vector2D fromPosition = null;

                            if (eventIndex > 0)
                            {
                                ExtrusionEvent previousEvent = getPreviousExtrusionEvent(eventIndex);
                                if (previousEvent != null)
                                {
                                    fromPosition = new Vector2D(previousEvent.getX(), previousEvent.getY());
                                } else
                                {
                                    steno.error("Couldn't get previous event");
                                }
                            } else
                            {
                                fromPosition = precursorPoint;
                            }

                            Vector2D toPosition = new Vector2D(currentEvent.getX(), currentEvent.getY());
                            Vector2D actualVector = toPosition.subtract(fromPosition);
                            Vector2D firstSegment = toPosition.subtract(scaleFactor, actualVector);

                            ExtrusionEvent firstSegmentExtrusionEvent = new ExtrusionEvent();
                            firstSegmentExtrusionEvent.setComment(comment + " split segment - before start of nozzle close");
                            firstSegmentExtrusionEvent.setX(firstSegment.getX());
                            firstSegmentExtrusionEvent.setY(firstSegment.getY());
                            firstSegmentExtrusionEvent.setE(((ExtrusionEvent) currentEvent).getE() * scaleFactor);
                            firstSegmentExtrusionEvent.setLength(segmentLength * scaleFactor);
                            firstSegmentExtrusionEvent.setFeedRate(currentEvent.getFeedRate());

                            ExtrusionEvent secondSegmentExtrusionEvent = new ExtrusionEvent();
                            secondSegmentExtrusionEvent.setComment(comment + " split segment - after start of nozzle close");
                            secondSegmentExtrusionEvent.setX(currentEvent.getX());
                            secondSegmentExtrusionEvent.setY(currentEvent.getY());
                            secondSegmentExtrusionEvent.setLength(segmentLength - firstSegmentExtrusionEvent.getLength());
                            secondSegmentExtrusionEvent.setFeedRate(currentEvent.getFeedRate());

//                        NozzlePositionChangeEvent nozzlePositionChangeEvent = new NozzlePositionChangeEvent();
//                        nozzlePositionChangeEvent.setComment("split segment - after start of nozzle close");
//                        currentNozzlePosition = currentNozzlePosition - ((segmentLength - firstSegmentExtrusionEvent.getLength()) / (startCloseBy - finishCloseBeforeEnd));
//                        nozzlePositionChangeEvent.setB(currentNozzlePosition);
//                        nozzlePositionChangeEvent.setNoExtrusionFlag(true);
//                        nozzlePositionChangeEvent.setX(currentEvent.getX());
//                        nozzlePositionChangeEvent.setY(currentEvent.getY());
//                        nozzlePositionChangeEvent.setLength(segmentLength - firstSegmentExtrusionEvent.getLength());
//
//                        extrusionBuffer.add(eventIndex, firstSegmentExtrusionEvent);
//                        extrusionBuffer.remove(eventIndex + 1);
//                        extrusionBuffer.add(eventIndex + 1, nozzlePositionChangeEvent);
                            if (finishCloseAt >= eventIndex)
                            {
                                finishCloseAt++;
                            }
                            extrusionBuffer.add(eventIndex, firstSegmentExtrusionEvent);
                            extrusionBuffer.remove(eventIndex + 1);
                            extrusionBuffer.add(eventIndex + 1, secondSegmentExtrusionEvent);

                            startCloseAt = eventIndex + 1;
                            break;
                        }
                    }
                }
                eventIndex--;
            }

            if (startCloseAt == -1 && lengthConsidered < currentNozzle.getStartCloseBy())
            {
                // Trigger short path behaviour
                startCloseAt = 0;
            }

            if (startCloseAt != -1 && finishCloseAt != -1)
            {
                // We've done it!
                // Output the extrusion data and break out of the loop

                double nozzlePosition = 1.0;

//                if (startCloseAt == finishCloseAt)
//                {
//                    // Must be a short line
//                    // Half extrusion
//                    // B0
//                    steno.error("Hit the short line code");
//                    CommentEvent commmentEvent = new CommentEvent();
//                    commmentEvent.setComment("ERROR - short line");
//                    writeEventToFile(commmentEvent);
////                    ExtrusionEvent event = extrusionBuffer.get(0);
////                    NozzlePositionChangeEvent nozzleEvent = new NozzlePositionChangeEvent();
////                    nozzleEvent.setX(event.getX());
////                    nozzleEvent.setY(event.getY());
////                    nozzleEvent.setLength(event.getLength());
////                    nozzleEvent.setFeedRate(event.getFeedRate());
////                    nozzleEvent.setComment(event.getComment() + " short path trigger");
////                    nozzleEvent.setB(0);
////                    nozzleEvent.setE(event.getE() * .5);
////                    nozzleEvent.setNoExtrusionFlag(false);
////                    writeEventToFile(nozzleEvent);
//                } else
                {

                    for (int eventWriteIndex = 0; eventWriteIndex < extrusionBuffer.size(); eventWriteIndex++)
                    {
                        GCodeParseEvent candidateevent = extrusionBuffer.get(eventWriteIndex);

                        if (candidateevent instanceof ExtrusionEvent)
                        {
                            ExtrusionEvent event = (ExtrusionEvent) candidateevent;

                            if (eventWriteIndex >= finishCloseAt)
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
                            } //                        else if (eventWriteIndex == finishCloseAt)
                            //                        {
                            //                            // No extrusion
                            //                            // B0
                            //                            NozzlePositionChangeEvent nozzleEvent = new NozzlePositionChangeEvent();
                            //                            nozzleEvent.setX(event.getX());
                            //                            nozzleEvent.setY(event.getY());
                            //                            nozzleEvent.setLength(event.getLength());
                            //                            nozzleEvent.setFeedRate(event.getFeedRate());
                            //                            nozzleEvent.setComment(event.getComment() + " at finish of close");
                            //                            nozzleEvent.setB(0);
                            //                            nozzleEvent.setNoExtrusionFlag(true);
                            //                            writeEventToFile(nozzleEvent);
                            //                        }
                            else if (eventWriteIndex >= startCloseAt)
                            {
                                // No extrusion
                                // Proportional B value
                                NozzlePositionChangeEvent nozzleEvent = new NozzlePositionChangeEvent();
                                nozzleEvent.setX(event.getX());
                                nozzleEvent.setY(event.getY());
                                nozzleEvent.setLength(event.getLength());
                                nozzleEvent.setFeedRate(event.getFeedRate());
                                nozzleEvent.setComment(event.getComment() + " after start of close");
                                nozzlePosition = nozzlePosition - (nozzleEvent.getLength() / (currentNozzle.getStartCloseBy() - currentNozzle.getFinishCloseBy()));
                                nozzleEvent.setB(nozzlePosition);
                                nozzleEvent.setNoExtrusionFlag(true);
                                writeEventToFile(nozzleEvent);
                            } else
                            {
                                writeEventToFile(event);
                            }
                        } else
                        {
                            writeEventToFile(candidateevent);
                        }
                    }
                }

                extrusionBuffer.clear();

                currentNozzle.closeNozzleFully();

            }

            if (startCloseAt == -1 || finishCloseAt == -1)
            {
                throw new NozzleCloseSettingsError("Didn't locate start / end of close");
            }
        }
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
}
