/*
 * Copyright 2014 CEL UK
 */
package celtech.services.printing;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
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
    List<Double> layerNumberTPredictedDuration;
    List<Integer> layerNumberToLineNumber;
    ETCCalculator etcCalculator;
    TestPrinter testPrinter;

    public ETCCalculatorTest()
    {
    }

    @Before
    public void setUp()
    {

        ETCCalculator.NUM_LINES_BEFORE_USING_ACTUAL_FIGURES = 0;
        ETCCalculator.ESTIMATED_TIME_PER_DISTANCE_UNIT = 100;

        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberTPredictedDuration = new ArrayList<>();
        layerNumberToLineNumber = new ArrayList<>();

        layerNumberToDistanceTravelled.add(0, 0d);
        layerNumberToDistanceTravelled.add(1, 9d);
        layerNumberToDistanceTravelled.add(2, 8d);
        layerNumberToDistanceTravelled.add(3, 7d);
        layerNumberToDistanceTravelled.add(4, 6d);
        layerNumberToDistanceTravelled.add(5, 5d);
        layerNumberToDistanceTravelled.add(6, 12d);
        layerNumberToDistanceTravelled.add(7, 12d);

        layerNumberTPredictedDuration.add(0, 0d);
        layerNumberTPredictedDuration.add(1, 60d);
        layerNumberTPredictedDuration.add(2, 100d);
        layerNumberTPredictedDuration.add(3, 120d);
        layerNumberTPredictedDuration.add(4, 150d);
        layerNumberTPredictedDuration.add(5, 180d);
        layerNumberTPredictedDuration.add(6, 200d);
        layerNumberTPredictedDuration.add(7, 230d);

        layerNumberToLineNumber.add(0, 0);
        layerNumberToLineNumber.add(1, 20);
        layerNumberToLineNumber.add(2, 40);
        layerNumberToLineNumber.add(3, 50);
        layerNumberToLineNumber.add(4, 60);
        layerNumberToLineNumber.add(5, 80);
        layerNumberToLineNumber.add(6, 91);
        layerNumberToLineNumber.add(7, 94);

        testPrinter = new TestPrinter();
        testPrinter.setBedTargetTemperature(120);
        testPrinter.setBedTemperature(120);

        etcCalculator = new ETCCalculator(testPrinter,
                                          layerNumberToDistanceTravelled,
                                          layerNumberTPredictedDuration,
                                          layerNumberToLineNumber, 100, 3);
    }

    @After
    public void tearDown()
    {
    }

    @Test
    public void testGetProgressAndETCAtTimeZero()
    {
        etcCalculator.initialise(0);
        /**
         * Estimated time should be total distance * extrusion rate Current guess for rate
         * = 1s per distance unit Total distance = 59, * 100 = 5900 Estimate = 98mins
         */
        String etcMessage = etcCalculator.getProgressAndETC(0);
        assertEquals("0% Elapsed Time (HH:MM) 00:00 ETC 01:38", etcMessage);
    }

    @Test
    public void testGetProgressAndETCAtLine55TwoMinutes()
    {
        etcCalculator.initialise(0);
        etcCalculator.elapseMinutes(2);
        String etcMessage = etcCalculator.getProgressAndETC(55);
        /**
         * lineNumber = 55, layerNumber = 3 totalDistanceTravelled = 24 + 3 totalDistance
         * = 59 currentRate = 120s / 27 remainingTime = (59 - 27) * (120 / 27) = 142 =
         * 2minutes
         */
        assertEquals("55% Elapsed Time (HH:MM) 00:02 ETC 00:02", etcMessage);
    }

    @Test
    public void testGetProgressAndETCAtLine55And100Minutes()
    {
        etcCalculator.initialise(0);
        etcCalculator.elapseMinutes(100);
        String etcMessage = etcCalculator.getProgressAndETC(55);
        /**
         * lineNumber = 55, layerNumber = 3 totalDistanceTravelled = 24 + 3 totalDistance
         * = 59 currentRate = 6000s / 27 remainingTime = (59 - 27) * (6000 / 27) = 7111 =
         * 118.5minutes
         */
        assertEquals("55% Elapsed Time (HH:MM) 01:40 ETC 01:58", etcMessage);
    }

    @Test
    public void testGetLayerNumberForLineNumber100()
    {
        etcCalculator.initialise(0);
        int layerNumber = etcCalculator.getLayerNumberForLineNumber(100);
        assertEquals(7, layerNumber);
    }

    @Test
    public void testGetLayerNumberForLineNumber94()
    {
        etcCalculator.initialise(0);
        int layerNumber = etcCalculator.getLayerNumberForLineNumber(94);
        assertEquals(7, layerNumber);
    }

    @Test
    public void testGetLayerNumberForLineNumber93()
    {
        etcCalculator.initialise(0);
        int layerNumber = etcCalculator.getLayerNumberForLineNumber(93);
        assertEquals(6, layerNumber);
    }

    @Test
    public void testGetProgressAndETCAtLine5And10Minutes()
    {
        etcCalculator.initialise(0);
        etcCalculator.elapseMinutes(10);
        String etcMessage = etcCalculator.getProgressAndETC(5);
        /**
         * lineNumber = 5, layerNumber = 0 totalDistanceTravelled = 2.25 (9 / 4)
         * totalDistance = 59 currentRate = 600s / 2.25 = 266.6 remainingTime = (59 -
         * 2.25) * 266.6 = 15129s = 252.15minutes = 4hr 12mins
         */
        assertEquals("5% Elapsed Time (HH:MM) 00:10 ETC 04:12", etcMessage);
    }

    @Test
    public void testGetProgressAndETCAtLine20And10Minutes()
    {
        etcCalculator.initialise(0);
        etcCalculator.elapseMinutes(10);
        String etcMessage = etcCalculator.getProgressAndETC(20);
        /**
         * lineNumber = 20, layerNumber = 1 totalDistanceTravelled = 9 totalDistance = 59
         * currentRate = 600s / 9 remainingTime = (59 - 9) * (600 / 9) = 3333 =
         * 55.55minutes
         */
        assertEquals("20% Elapsed Time (HH:MM) 00:10 ETC 00:55", etcMessage);
    }

    @Test
    public void testGetProgressAndETCAtLine100And10Minutes()
    {
        etcCalculator.initialise(0);
        etcCalculator.elapseMinutes(10);
        String etcMessage = etcCalculator.getProgressAndETC(100);
        assertEquals("100% Elapsed Time (HH:MM) 00:10 ETC 00:00", etcMessage);
    }

    @Test
    public void testGetProgressAndETCAtAllLinesThrowsNoExceptions()
    {
        etcCalculator.initialise(0);
        etcCalculator.elapseMinutes(20);
        for (int i = 0; i < 101; i++)
        {
            System.out.println(i);
            String etcMessage = etcCalculator.getProgressAndETC(i);
            System.out.println(etcMessage);
        }
    }

    @Test
    public void testGetPartialDistanceInLayer()
    {
        etcCalculator.initialise(0);
        double partialDistance = etcCalculator.getPartialDistanceInLayer(4, 65);
        assertEquals(5.0 / 20.0 * 5.0, partialDistance, 0.001);
    }

    @Test
    public void testGetProgressAndETCBeforeFirstExtrusion()
    {
        etcCalculator.initialise(0);
        etcCalculator.elapseMinutes(10);
        String etcMessage = etcCalculator.getProgressAndETC(3);
        /**
         * Estimated time should be total distance * extrusion rate Current guess for rate
         * = 1s per distance unit Total distance = 59, * 100 = 5900 Estimate = 98mins,
         * -10mins elapsed = 88min
         */
        assertEquals("3% Elapsed Time (HH:MM) 00:10 ETC 01:28", etcMessage);
    }

    @Test
    public void testGetProgressAndPredictedETCAtLine55()
    {
        etcCalculator.initialise(0);
        int ETC = etcCalculator.getETCPredicted(55);
        /**
         * lineNumber = 55, layerNumber = 3 totalDuration = 280 + 150/2 = 355 totalTime =
         * 1040 remainingTime = (1040 - 355) = 685 = 11mins
         */
        assertEquals(685, ETC);
    }

    @Test
    public void testGetProgressAndPredictedETCAtLine55WithBedDifferential60()
    {
        testPrinter.setBedTemperature(120 - 30);
        etcCalculator.initialise(0);
        int ETC = etcCalculator.getETCPredicted(55);
        /**
         * lineNumber = 55, layerNumber = 3 totalDuration = 280 + 150/2 = 355 totalTime =
         * 1040 remainingTime = (1040 - 355) = 685 = 11mins
         * ADD time to warm up bed 60 seconds
         */
        assertEquals(745, ETC);
    }

}
