/*
 * Copyright 2014 CEL UK
 */
package celtech.services.printing;

import celtech.printerControl.Printer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * ETCCalculator calculates the ETC (estimated time to complete) of print jobs for a given
 * line number being processed.
 *
 * @author tony
 */
class ETCCalculator
{

    Instant timeAtFirstLine;
    /**
     * The first lineNumber we were notified of by the printer
     */
    int firstLineNotified;
    int totalNumberOfLines;
    private final List<Integer> layerNumberToLineNumber;
    private final List<Double> layerNumberToPredictedDuration;
    private final List<Double> layerNumberToTotalPredictedDuration = new ArrayList<Double>();
    double totalPredictedDurationAllLayers;
    private final int lineNumberOfFirstExtrusion;

    // Current estimate 1s per distance unit
    protected static int ESTIMATED_TIME_PER_DISTANCE_UNIT = 1;
    protected static int NUM_LINES_BEFORE_USING_ACTUAL_FIGURES = 150;
    /**
     * The number of seconds it takes to heat the bed up by one degree
     */
    protected static int PREDICTED_BED_HEAT_RATE = 2;
    private final Printer printer;

    ETCCalculator(Printer printer, 
            List<Double> layerNumberToPredictedDuration,
            List<Integer> layerNumberToLineNumber,
            int totalNumberOfLines, int lineNumberOfFirstExtrusion)
    {
        this.printer = printer;
        this.layerNumberToLineNumber = layerNumberToLineNumber;
        this.layerNumberToPredictedDuration = layerNumberToPredictedDuration;
        this.totalNumberOfLines = totalNumberOfLines;
        this.lineNumberOfFirstExtrusion = lineNumberOfFirstExtrusion;

        double totalPredictedDuration = 0;
        for (int i = 0; i < layerNumberToPredictedDuration.size(); i++)
        {
            Double duration = layerNumberToPredictedDuration.get(i);
            totalPredictedDuration += duration;
            layerNumberToTotalPredictedDuration.add(i, totalPredictedDuration);
        }
        totalPredictedDurationAllLayers = totalPredictedDuration;
    }

    public void initialise(int lineNumber)
    {
        firstLineNotified = lineNumber;
        timeAtFirstLine = Instant.now();
    }

    /**
     * Calculate the ETC based on predicted durations.
     * @return the number of seconds
     */
    int getETCPredicted(int lineNumber)
    {
        int remainingTimeSeconds = getPredictedPrintTime(lineNumber);
        remainingTimeSeconds += getBedHeatingTime();
        return remainingTimeSeconds;
    }
    
    /**
     * Estimate the time to heat the bed up to the target temperature
     * @return the time in seconds
     */
    private int getBedHeatingTime() {
        int bedTargetTemperature = printer.getBedTargetTemperature();
        int bedTemperature = printer.getBedTemperature();
        if (bedTemperature < bedTargetTemperature) {
            return (bedTargetTemperature - bedTemperature) * PREDICTED_BED_HEAT_RATE;
        } else {
            return 0;
        }
    }

    /**
     * Return the predicted print time by calculating the predicted time to reach
     * the current layer, and subtract from the predicted total time.
     */
    private int getPredictedPrintTime(int lineNumber)
    {
        int remainingTimeSeconds;
        int layerNumber = getLayerNumberForLineNumber(lineNumber);
        double totalPredictedDurationForLayer = layerNumberToTotalPredictedDuration.get(
                layerNumber);
        double predictedDurationInNextLayer = getPartialDurationInLayer(
                layerNumber, lineNumber);
        double totalDurationSoFar = totalPredictedDurationForLayer + predictedDurationInNextLayer;
        remainingTimeSeconds = (int) ((totalPredictedDurationAllLayers - totalDurationSoFar));
        return remainingTimeSeconds;
    }

    /**
     * TESTING ONLY - simulate the passing of minutes by pushing back timeAtFirstLayer;
     */
    void elapseMinutes(int minutes)
    {
        timeAtFirstLine = timeAtFirstLine.minusSeconds(minutes * 60);
    }

    /**
     * Get the layer number for the given line number
     */
    protected int getLayerNumberForLineNumber(int lineNumber)
    {
        for (int layerNumber = 1; layerNumber < layerNumberToLineNumber.size(); layerNumber++)
        {
            Integer lineNumberForLayer = layerNumberToLineNumber.get(layerNumber);
            if (lineNumberForLayer > lineNumber)
            {
                return layerNumber - 1;
            }

        }
        if (lineNumber <= totalNumberOfLines)
        {
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
        return (timeAtFirstLine != null);
    }

    protected double getPartialDurationInLayer(int layerNumber, int lineNumber)
    {
        double extraLinesProgressInNextlayer = lineNumber - layerNumberToLineNumber.get(
                layerNumber);
        double numLinesAtEndOfLayer;
        double durationInNextLayer;
        if (layerNumber == layerNumberToLineNumber.size() - 1)
        {
            numLinesAtEndOfLayer = totalNumberOfLines;
            durationInNextLayer = 0;
        } else
        {
            numLinesAtEndOfLayer = layerNumberToLineNumber.get(layerNumber + 1);
            durationInNextLayer = layerNumberToPredictedDuration.get(
                    layerNumber + 1);
        }
        double totalLinesInNextLayer = numLinesAtEndOfLayer - layerNumberToLineNumber.get(
                layerNumber);
        return (extraLinesProgressInNextlayer / totalLinesInNextLayer) * durationInNextLayer;
    }    

}
