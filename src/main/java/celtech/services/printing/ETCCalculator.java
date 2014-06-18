/*
 * Copyright 2014 CEL UK
 */
package celtech.services.printing;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * ETCCalculator calculates the ETC (estimated time to complete) of print jobs
 *
 * @author tony
 */
class ETCCalculator
{

    Instant timeAtFirstLayer;
    /**
     * The first layer we were notified of by the printer
     */
    int firstLayer;
    int totalNumberOfLines;
    private final List<Integer> layerNumberToLineNumber;
    private final List<Double> layerNumberToTotalDistanceTravelled = new ArrayList<Double>();
    double totalDistanceTravelledAllLayers;

    ETCCalculator(List<Double> layerNumberToDistanceTravelled,
            List<Integer> layerNumberToLineNumber,
            int totalNumberOfLines)
    {
        this.layerNumberToLineNumber = layerNumberToLineNumber;
        this.totalNumberOfLines = totalNumberOfLines;
        double totalDistanceTravelled = 0;
        for (int i = 0; i < layerNumberToDistanceTravelled.size(); i++)
        {
            Double distance = layerNumberToDistanceTravelled.get(i);
            totalDistanceTravelled += distance;
            layerNumberToTotalDistanceTravelled.add(i, totalDistanceTravelled);
        }
        totalDistanceTravelledAllLayers = totalDistanceTravelled;
    }

    public void initialise(int layerNumber)
    {
        firstLayer = layerNumber;
        timeAtFirstLayer = Instant.now();
    }
    

    /**
     * Calculate the progress percent and ETC, and return as a user displayable string
     */
    String getProgressAndETC(int lineNumber)
    {
        if (timeAtFirstLayer == null)
        {
            throw new RuntimeException(
                    "initialise must be called once before calls to getProgressAndETC");
        }
        int elapsedTimeSeconds = (int) Duration.between(timeAtFirstLayer, Instant.now()).toMillis() / 1000;
        String hoursMinutes = convertToHoursMinutes(elapsedTimeSeconds);
        int percentComplete = (int) (lineNumber * 100 / totalNumberOfLines);
        String elapsedTimeFormatted = String.format("%d%% Elapsed Time (HH:MM) %s",
                                                    percentComplete, hoursMinutes);

        if (elapsedTimeSeconds > 30)
        {
            // Some time has passed so we can provide an ETC
            /**
             * The principle to get the ETC is get the total distance traversed for all
             * layers up until this layer, together with the time taken to traverse that
             * distance. Then we can calculate the remaining distance and hence remaining
             * time.
             */
            int layerNumber = getLayerNumberForLineNumber(lineNumber);
            double totalDistanceTraversed = layerNumberToTotalDistanceTravelled.get(
                    layerNumber);
            double timerPerUnitTravelled = elapsedTimeSeconds / totalDistanceTraversed;
            int remainingTimeSeconds = (int) ((totalDistanceTravelledAllLayers - totalDistanceTraversed)
                    * timerPerUnitTravelled);
            hoursMinutes = convertToHoursMinutes(remainingTimeSeconds);
            elapsedTimeFormatted += " ETC " + hoursMinutes;
        }

        return elapsedTimeFormatted;
    }

    /**
     * TESTING ONLY - simulate the passing of minutes by pushing back timeAtFirstLayer;
     */
    void elapseMinutes(int minutes)
    {
        timeAtFirstLayer = timeAtFirstLayer.minusSeconds(minutes * 60);
    }

    /**
     * Get the layer number for the given line number
     */
    private int getLayerNumberForLineNumber(int lineNumber)
    {
        for (int layerNumber = 1; layerNumber < layerNumberToLineNumber.size(); layerNumber++)
        {
            Integer lineNumberForLayer = layerNumberToLineNumber.get(layerNumber);
            if (lineNumberForLayer > lineNumber)
            {
                return layerNumber - 1;
            }

        }
        if (lineNumber <= totalNumberOfLines) {
            return layerNumberToLineNumber.size() - 1;
        }
        throw new RuntimeException("Did not calculate layer number");
    }

    private String convertToHoursMinutes(int seconds)
    {
        int minutes = (int) (seconds / 60);
        int hours = minutes / 60;
        minutes = minutes - (60 * hours);
        return String.format("%02d:%02d", hours, minutes);
    }

    boolean isInitialised()
    {
        return (timeAtFirstLayer != null);
    }

}
