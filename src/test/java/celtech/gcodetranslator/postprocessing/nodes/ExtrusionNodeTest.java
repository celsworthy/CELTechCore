/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.gcodetranslator.postprocessing.nodes;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian
 */
public class ExtrusionNodeTest
{

    public ExtrusionNodeTest()
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

    /**
     * Test of clone method, of class ExtrusionNode.
     */
    @Test
    public void testClone() throws Exception
    {
        System.out.println("clone");
        ExtrusionNode instance = new ExtrusionNode();
        instance.setB(4);
        instance.setX(1);
        instance.setY(2);
        instance.setZ(3);
        instance.setFeedRate_mmPerMin(14);
        instance.setD(5);
        instance.setE(6);

        ExtrusionNode result = instance.clone();
        double epsilon = 0.001;
        assertEquals(4, result.getB(), epsilon);
        assertEquals(1, result.getX(), epsilon);
        assertEquals(2, result.getY(), epsilon);
        assertEquals(3, result.getZ(), epsilon);
        assertEquals(14, result.getFeedRate_mmPerMin(), epsilon);
        assertEquals(5, result.getD(), epsilon);
        assertEquals(6, result.getE(), epsilon);
    }

}
