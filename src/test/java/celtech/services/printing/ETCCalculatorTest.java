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
                                          layerNumberTPredictedDuration,
                                          layerNumberToLineNumber, 100, 3);
    }

    @After
    public void tearDown()
    {
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
