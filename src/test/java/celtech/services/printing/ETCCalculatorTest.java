/*
 * Copyright 2014 CEL UK
 */
package celtech.services.printing;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class ETCCalculatorTest
{

    List<Double> layerNumberToDistanceTravelled;
    List<Integer> layerNumberToLineNumber;
    ETCCalculator etcCalculator;

    public ETCCalculatorTest()
    {
    }

    @Before
    public void setUp()
    {
        layerNumberToDistanceTravelled = new ArrayList<>();
        layerNumberToLineNumber = new ArrayList<>();

        layerNumberToDistanceTravelled.add(0, 10d);
        layerNumberToDistanceTravelled.add(1, 9d);
        layerNumberToDistanceTravelled.add(2, 8d);
        layerNumberToDistanceTravelled.add(3, 7d);
        layerNumberToDistanceTravelled.add(4, 6d);
        layerNumberToDistanceTravelled.add(5, 5d);
        layerNumberToDistanceTravelled.add(6, 12d);
        layerNumberToDistanceTravelled.add(7, 12d);

        layerNumberToLineNumber.add(0, 0);
        layerNumberToLineNumber.add(1, 20);
        layerNumberToLineNumber.add(2, 40);
        layerNumberToLineNumber.add(3, 50);
        layerNumberToLineNumber.add(4, 60);
        layerNumberToLineNumber.add(5, 80);
        layerNumberToLineNumber.add(6, 91);
        layerNumberToLineNumber.add(7, 94);

        etcCalculator = new ETCCalculator(layerNumberToDistanceTravelled, 
                                            layerNumberToLineNumber, 100);
    }

    @After
    public void tearDown()
    {
    }

    @Test
    public void testGetProgressAndETCAtTimeZero()
    {
        etcCalculator.initialise(0);
        String etcMessage = etcCalculator.getProgressAndETC(0);
        assertEquals("0% Elapsed Time (HH:MM) 00:00", etcMessage);
    }

    @Test
    public void testGetProgressAndETCAtLine55TwoMinutes()
    {
        etcCalculator.initialise(0);
        etcCalculator.elapseMinutes(20);
        String etcMessage = etcCalculator.getProgressAndETC(55);
        /**
         * lineNumber = 55, layerNumber = 3
         * totalDistanceTravelled = 10 + 9 + 8 + 7 = 34
         * totalDistance = 10 + 9 + 8 + 7 + 6 + 5 + 12 + 12 = 69
         * currentRate = 120s / 34 = 35.29
         * remainingTime = (69 - 34) * 35.29 = 1235.1 = 20.59minutes
         */
        assertEquals("55% Elapsed Time (HH:MM) 00:20 ETC 00:20", etcMessage);
    }
    
    @Test
    public void testGetProgressAndETCAtLine55And100Minutes()
    {
        etcCalculator.initialise(0);
        etcCalculator.elapseMinutes(100);
        String etcMessage = etcCalculator.getProgressAndETC(55);
        /**
         * lineNumber = 55, layerNumber = 3
         * totalDistanceTravelled = 10 + 9 + 8 + 7 = 34
         * totalDistance = 10 + 9 + 8 + 7 + 6 + 5 + 12 + 12 = 69
         * currentRate = 6000s / 34 = 176.47
         * remainingTime = (69 - 34) * 176.47 = 6176s = 102.94minutes = 1hr 42mins
         */
        assertEquals("55% Elapsed Time (HH:MM) 01:40 ETC 01:42", etcMessage);
    }   
    
    @Test
    public void testGetProgressAndETCAtAllLinesThrowsNoExceptions()
    {
        etcCalculator.initialise(0);
        etcCalculator.elapseMinutes(20);
        for (int i = 0; i < 101; i++)
        {
            String etcMessage = etcCalculator.getProgressAndETC(i);
        }
    }    

}
