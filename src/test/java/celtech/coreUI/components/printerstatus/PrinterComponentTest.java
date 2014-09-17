/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.printerstatus;

import java.beans.PropertyChangeEvent;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tony
 */
public class PrinterComponentTest
{
    
    public PrinterComponentTest()
    {
    }
    
    @BeforeClass
    public static void setUpClass()
    {
    }
    
    @AfterClass
    public static void tearDownClass()
    {
    }
    
    @Before
    public void setUp()
    {
    }
    
    @After
    public void tearDown()
    {
    }

    @Test
    public void testSetStatus()
    {
        System.out.println("setStatus");
        PrinterComponent.Status status = null;
        PrinterComponent instance = null;
        instance.setStatus(status);
        fail("The test case is a prototype.");
    }

    @Test
    public void testSetName()
    {
        System.out.println("setName");
        String value = "";
        PrinterComponent instance = null;
        instance.setName(value);
        fail("The test case is a prototype.");
    }

    @Test
    public void testNameTextProperty()
    {
        System.out.println("nameTextProperty");
        PrinterComponent instance = null;
        StringProperty expResult = null;
        StringProperty result = instance.nameTextProperty();
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

    @Test
    public void testSetProgress()
    {
        System.out.println("setProgress");
        double progress = 0.0;
        PrinterComponent instance = null;
        instance.setProgress(progress);
        fail("The test case is a prototype.");
    }

    @Test
    public void testSetColour()
    {
        System.out.println("setColour");
        Color color = null;
        PrinterComponent instance = null;
        instance.setColour(color);
        fail("The test case is a prototype.");
    }

    @Test
    public void testSetSelected()
    {
        System.out.println("setSelected");
        boolean select = false;
        PrinterComponent instance = null;
        instance.setSelected(select);
        fail("The test case is a prototype.");
    }

    @Test
    public void testSetSize()
    {
        System.out.println("setSize");
        PrinterComponent.Size size = null;
        PrinterComponent instance = null;
        instance.setSize(size);
        fail("The test case is a prototype.");
    }

    @Test
    public void testPropertyChange()
    {
        System.out.println("propertyChange");
        PropertyChangeEvent evt = null;
        PrinterComponent instance = null;
        instance.propertyChange(evt);
        fail("The test case is a prototype.");
    }

    @Test
    public void testComputeMinHeight()
    {
        System.out.println("computeMinHeight");
        double width = 0.0;
        PrinterComponent instance = null;
        double expResult = 0.0;
        double result = instance.computeMinHeight(width);
        assertEquals(expResult, result, 0.0);
        fail("The test case is a prototype.");
    }

    @Test
    public void testComputeMinWidth()
    {
        System.out.println("computeMinWidth");
        double height = 0.0;
        PrinterComponent instance = null;
        double expResult = 0.0;
        double result = instance.computeMinWidth(height);
        assertEquals(expResult, result, 0.0);
        fail("The test case is a prototype.");
    }

    @Test
    public void testComputeMaxHeight()
    {
        System.out.println("computeMaxHeight");
        double width = 0.0;
        PrinterComponent instance = null;
        double expResult = 0.0;
        double result = instance.computeMaxHeight(width);
        assertEquals(expResult, result, 0.0);
        fail("The test case is a prototype.");
    }

    @Test
    public void testComputeMaxWidth()
    {
        System.out.println("computeMaxWidth");
        double height = 0.0;
        PrinterComponent instance = null;
        double expResult = 0.0;
        double result = instance.computeMaxWidth(height);
        assertEquals(expResult, result, 0.0);
        fail("The test case is a prototype.");
    }

    @Test
    public void testComputePrefHeight()
    {
        System.out.println("computePrefHeight");
        double width = 0.0;
        PrinterComponent instance = null;
        double expResult = 0.0;
        double result = instance.computePrefHeight(width);
        assertEquals(expResult, result, 0.0);
        fail("The test case is a prototype.");
    }

    @Test
    public void testComputePrefWidth()
    {
        System.out.println("computePrefWidth");
        double height = 0.0;
        PrinterComponent instance = null;
        double expResult = 0.0;
        double result = instance.computePrefWidth(height);
        assertEquals(expResult, result, 0.0);
        fail("The test case is a prototype.");
    }

    @Test
    public void testFitNameToWidth()
    {
        System.out.println("fitNameToWidth");
        PrinterComponent instance = null;
        instance.fitNameToWidth();
        fail("The test case is a prototype.");
    }
    
}
