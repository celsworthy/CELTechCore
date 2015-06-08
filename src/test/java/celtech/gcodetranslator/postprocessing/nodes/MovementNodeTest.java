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
public class MovementNodeTest
{

    public MovementNodeTest()
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
     * Test of timeToReach method, of class MovementNode.
     */
    @Test
    public void testTimeToReach()
    {
        System.out.println("timeToReach");

        MovementNode sourceNode = new MovementNodeImpl();
        sourceNode.setX(0);
        sourceNode.setY(0);
        sourceNode.setFeedRate_mmPerMin(600);

        MovementNode destinationNode = new MovementNodeImpl();
        destinationNode.setX(10);
        destinationNode.setY(0);
        destinationNode.setFeedRate_mmPerMin(0);

        double result = sourceNode.timeToReach(destinationNode);

        assertEquals(1, result, 0.0);

        //Should be half the time
        sourceNode.setX(0);
        sourceNode.setY(0);
        sourceNode.setFeedRate_mmPerMin(6000);
        destinationNode.setX(10);
        destinationNode.setY(0);
        destinationNode.setFeedRate_mmPerMin(0);

        double result2 = sourceNode.timeToReach(destinationNode);

        assertEquals(0.1, result2, 0.0);

        sourceNode.setX(0);
        sourceNode.setY(0);
        sourceNode.setFeedRate_mmPerMin(600);
        destinationNode.setX(3);
        destinationNode.setY(4);
        destinationNode.setFeedRate_mmPerMin(0);

        double result3 = sourceNode.timeToReach(destinationNode);

        assertEquals(0.5, result3, 0.0);

    }

    public class MovementNodeImpl extends MovementNode
    {
    }

}
