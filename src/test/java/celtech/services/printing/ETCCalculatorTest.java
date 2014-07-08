/*
 * Copyright 2014 CEL UK
 */
package celtech.services.printing;

import celtech.gcodetranslator.PrintJobStatistics;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class ETCCalculatorTest
{

    List<Double> layerNumberToDistanceTravelled;
    List<Double> layerNumberToPredictedDuration;
    List<Integer> layerNumberToLineNumber;
    ETCCalculator etcCalculator;
    TestPrinter testPrinter;

    @Before
    public void setUp()
    {

        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();
        layerNumberToLineNumber = new ArrayList<>();

        // total duration = 100.0s
        layerNumberToPredictedDuration.add(0, 0d);
        layerNumberToPredictedDuration.add(1, 10d);
        layerNumberToPredictedDuration.add(2, 10d);
        layerNumberToPredictedDuration.add(3, 20d);
        layerNumberToPredictedDuration.add(4, 10d);
        layerNumberToPredictedDuration.add(5, 30d);
        layerNumberToPredictedDuration.add(6, 15d);
        layerNumberToPredictedDuration.add(7, 5d);

        // model has 100 lines
        layerNumberToLineNumber.add(0, 0);
        layerNumberToLineNumber.add(1, 20);
        layerNumberToLineNumber.add(2, 40);
        layerNumberToLineNumber.add(3, 50);
        layerNumberToLineNumber.add(4, 60);
        layerNumberToLineNumber.add(5, 80);
        layerNumberToLineNumber.add(6, 91);
        layerNumberToLineNumber.add(7, 100);

        testPrinter = new TestPrinter();
        testPrinter.setBedTargetTemperature(120);
        testPrinter.setBedTemperature(120);

        etcCalculator = new ETCCalculator(testPrinter,
                                          layerNumberToPredictedDuration,
                                          layerNumberToLineNumber);
    }

    @Test
    public void testGetLayerNumberForLineNumber100()
    {
        int layerNumber = etcCalculator.getCompletedLayerNumberForLineNumber(100);
        assertEquals(6, layerNumber);
    }

    @Test
    public void testGetLayerNumberForLineNumber94()
    {
        int layerNumber = etcCalculator.getCompletedLayerNumberForLineNumber(94);
        assertEquals(6, layerNumber);
    }

    @Test
    public void testGetProgressAndPredictedETCAtLine55()
    {
        int ETC = etcCalculator.getETCPredicted(55);
        /**
         * lineNumber = 55, layerNumber = 3 totalDuration = 40 + 10/2 = 45 totalTime = 100
         * remainingTime = (100 - 45) = 55
         */
        assertEquals(55, ETC);
    }

    @Test
    public void testGetProgressAndPredictedETCAtLine55WithBedDifferential30()
    {
        testPrinter.setBedTemperature(120 - 30);
        int ETC = etcCalculator.getETCPredicted(55);
        /**
         * lineNumber = 55, layerNumber = 3 totalDuration = 40 + 10/2 = 45 totalTime = 100
         * remainingTime = (100 - 45) = 55 to warm up bed 60 seconds
         */
        assertEquals(115, ETC);
    }

    @Test
    public void testGetPercentCompleteIs1AtEnd()
    {
        double progressPercent = etcCalculator.getPercentCompleteAtLine(100);
        assertEquals(1, progressPercent, 0.001);
    }

    @Test
    public void testPercentCompleteAtLine0_2EqualLayers()
    {
        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();
        layerNumberToLineNumber = new ArrayList<>();

        layerNumberToPredictedDuration.add(0, 0d);
        layerNumberToPredictedDuration.add(1, 10d);
        layerNumberToPredictedDuration.add(2, 10d);

        layerNumberToLineNumber.add(0, 0);
        layerNumberToLineNumber.add(1, 10);
        layerNumberToLineNumber.add(2, 20);

        testPrinter = new TestPrinter();
        testPrinter.setBedTargetTemperature(120);
        testPrinter.setBedTemperature(120);

        etcCalculator = new ETCCalculator(testPrinter,
                                          layerNumberToPredictedDuration,
                                          layerNumberToLineNumber);
        double progressPercent = etcCalculator.getPercentCompleteAtLine(0);
        assertEquals(0.0, progressPercent, 0.001);
    }

    @Test
    public void testPercentCompleteAtLine10_2EqualLayers()
    {
        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();
        layerNumberToLineNumber = new ArrayList<>();

        layerNumberToPredictedDuration.add(0, 0d);
        layerNumberToPredictedDuration.add(1, 10d);
        layerNumberToPredictedDuration.add(2, 10d);

        layerNumberToLineNumber.add(0, 0);
        layerNumberToLineNumber.add(1, 10);
        layerNumberToLineNumber.add(2, 20);

        testPrinter = new TestPrinter();
        testPrinter.setBedTargetTemperature(120);
        testPrinter.setBedTemperature(120);

        etcCalculator = new ETCCalculator(testPrinter,
                                          layerNumberToPredictedDuration,
                                          layerNumberToLineNumber);
        double progressPercent = etcCalculator.getPercentCompleteAtLine(10);
        assertEquals(0.5, progressPercent, 0.001);
    }

    @Test
    public void testPercentCompleteAtLine20_2EqualLayers()
    {
        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();
        layerNumberToLineNumber = new ArrayList<>();

        layerNumberToPredictedDuration.add(0, 0d);
        layerNumberToPredictedDuration.add(1, 10d);
        layerNumberToPredictedDuration.add(2, 10d);

        layerNumberToLineNumber.add(0, 0);
        layerNumberToLineNumber.add(1, 10);
        layerNumberToLineNumber.add(2, 20);

        testPrinter = new TestPrinter();
        testPrinter.setBedTargetTemperature(120);
        testPrinter.setBedTemperature(120);

        etcCalculator = new ETCCalculator(testPrinter,
                                          layerNumberToPredictedDuration,
                                          layerNumberToLineNumber);
        double progressPercent = etcCalculator.getPercentCompleteAtLine(20);
        assertEquals(1.0, progressPercent, 0.001);
    }

    @Test
    public void testPercentCompleteAtLine0_2DifferingLayers()
    {
        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();
        layerNumberToLineNumber = new ArrayList<>();

        layerNumberToPredictedDuration.add(0, 0d);
        layerNumberToPredictedDuration.add(1, 10d);
        layerNumberToPredictedDuration.add(2, 20d);

        layerNumberToLineNumber.add(0, 0);
        layerNumberToLineNumber.add(1, 10);
        layerNumberToLineNumber.add(2, 20);

        testPrinter = new TestPrinter();
        testPrinter.setBedTargetTemperature(120);
        testPrinter.setBedTemperature(120);

        etcCalculator = new ETCCalculator(testPrinter,
                                          layerNumberToPredictedDuration,
                                          layerNumberToLineNumber);
        double progressPercent = etcCalculator.getPercentCompleteAtLine(0);
        assertEquals(0.0, progressPercent, 0.001);
    }

    @Test
    public void testPercentCompleteAtLine10_2DifferingLayers()
    {
        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();
        layerNumberToLineNumber = new ArrayList<>();

        layerNumberToPredictedDuration.add(0, 0d);
        layerNumberToPredictedDuration.add(1, 10d);
        layerNumberToPredictedDuration.add(2, 20d);

        layerNumberToLineNumber.add(0, 0);
        layerNumberToLineNumber.add(1, 10);
        layerNumberToLineNumber.add(2, 20);

        testPrinter = new TestPrinter();
        testPrinter.setBedTargetTemperature(120);
        testPrinter.setBedTemperature(120);

        etcCalculator = new ETCCalculator(testPrinter,
                                          layerNumberToPredictedDuration,
                                          layerNumberToLineNumber);
        double progressPercent = etcCalculator.getPercentCompleteAtLine(10);
        assertEquals(0.333, progressPercent, 0.001);
    }

    @Test
    public void testPercentCompleteAtLine30_2DifferingLayers()
    {
        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();
        layerNumberToLineNumber = new ArrayList<>();

        layerNumberToPredictedDuration.add(0, 0d);
        layerNumberToPredictedDuration.add(1, 10d);
        layerNumberToPredictedDuration.add(2, 20d);

        layerNumberToLineNumber.add(0, 0);
        layerNumberToLineNumber.add(1, 10);
        layerNumberToLineNumber.add(2, 20);

        testPrinter = new TestPrinter();
        testPrinter.setBedTargetTemperature(120);
        testPrinter.setBedTemperature(120);

        etcCalculator = new ETCCalculator(testPrinter,
                                          layerNumberToPredictedDuration,
                                          layerNumberToLineNumber);
        double progressPercent = etcCalculator.getPercentCompleteAtLine(20);
        assertEquals(1.000, progressPercent, 0.001);
    }

    @Test
    public void testPercentCompleteAtLine0_1Layer()
    {
        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();
        layerNumberToLineNumber = new ArrayList<>();

        layerNumberToPredictedDuration.add(0, 0d);
        layerNumberToPredictedDuration.add(1, 100d);

        layerNumberToLineNumber.add(0, 0);
        layerNumberToLineNumber.add(1, 10);

        testPrinter = new TestPrinter();
        testPrinter.setBedTargetTemperature(120);
        testPrinter.setBedTemperature(120);

        etcCalculator = new ETCCalculator(testPrinter,
                                          layerNumberToPredictedDuration,
                                          layerNumberToLineNumber);
        double progressPercent = etcCalculator.getPercentCompleteAtLine(0);
        assertEquals(0.0, progressPercent, 0.0001);
    }

    @Test
    public void testPercentCompleteAtLine1_1Layer()
    {
        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();
        layerNumberToLineNumber = new ArrayList<>();

        layerNumberToPredictedDuration.add(0, 0d);
        layerNumberToPredictedDuration.add(1, 10d);

        layerNumberToLineNumber.add(0, 0);
        layerNumberToLineNumber.add(1, 10);

        testPrinter = new TestPrinter();
        testPrinter.setBedTargetTemperature(120);
        testPrinter.setBedTemperature(120);

        etcCalculator = new ETCCalculator(testPrinter,
                                          layerNumberToPredictedDuration,
                                          layerNumberToLineNumber);
        double progressPercent = etcCalculator.getPercentCompleteAtLine(1);
        assertEquals(0.1, progressPercent, 0.001);
    }

    @Test
    public void testPercentCompleteAtLine5_1Layer()
    {
        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();
        layerNumberToLineNumber = new ArrayList<>();

        layerNumberToPredictedDuration.add(0, 0d);
        layerNumberToPredictedDuration.add(1, 10d);

        layerNumberToLineNumber.add(0, 0);
        layerNumberToLineNumber.add(1, 10);

        testPrinter = new TestPrinter();
        testPrinter.setBedTargetTemperature(120);
        testPrinter.setBedTemperature(120);

        etcCalculator = new ETCCalculator(testPrinter,
                                          layerNumberToPredictedDuration,
                                          layerNumberToLineNumber);
        double progressPercent = etcCalculator.getPercentCompleteAtLine(5);
        assertEquals(0.5, progressPercent, 0.001);
    }

    @Test
    public void testPercentCompleteAtLine10_1Layer()
    {
        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();
        layerNumberToLineNumber = new ArrayList<>();

        layerNumberToPredictedDuration.add(0, 0d);
        layerNumberToPredictedDuration.add(1, 10d);

        layerNumberToLineNumber.add(0, 0);
        layerNumberToLineNumber.add(1, 10);

        testPrinter = new TestPrinter();
        testPrinter.setBedTargetTemperature(120);
        testPrinter.setBedTemperature(120);

        etcCalculator = new ETCCalculator(testPrinter,
                                          layerNumberToPredictedDuration,
                                          layerNumberToLineNumber);
        double progressPercent = etcCalculator.getPercentCompleteAtLine(10);
        assertEquals(1.0, progressPercent, 0.001);
    }

    @Test
    public void testSpecificCaseWhereAppearsInAppFinishedWhenNot() throws IOException
    {
        URL statisticsFile = this.getClass().getResource("/badetcprintjob/job.statistics");
        PrintJobStatistics readIntoPrintJobStatistics = PrintJobStatistics.readFromFile(
            statisticsFile.getFile());
        etcCalculator = new ETCCalculator(testPrinter,
                                          readIntoPrintJobStatistics.getLayerNumberToPredictedDuration(),
                                          readIntoPrintJobStatistics.getLayerNumberToLineNumber());
        
        assertEquals(0, etcCalculator.getCompletedLayerNumberForLineNumber(1));
        assertEquals(7, etcCalculator.getCompletedLayerNumberForLineNumber(3140));
        assertEquals(71, etcCalculator.getCompletedLayerNumberForLineNumber(20631));
        assertEquals(157, etcCalculator.getCompletedLayerNumberForLineNumber(26609));
        assertEquals(167, etcCalculator.getCompletedLayerNumberForLineNumber(27191));
        assertEquals(226, etcCalculator.getCompletedLayerNumberForLineNumber(30020));
        
        assertEquals(437, etcCalculator.getETCPredicted(27191));
        assertEquals(8, etcCalculator.getETCPredicted(30205));
        assertEquals(2, etcCalculator.getETCPredicted(30239));
        
    }

}
