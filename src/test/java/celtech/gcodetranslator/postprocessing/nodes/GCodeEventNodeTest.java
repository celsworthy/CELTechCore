package celtech.gcodetranslator.postprocessing.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
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
public class GCodeEventNodeTest
{

    public GCodeEventNodeTest()
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
     * Test of isLeaf method, of class GCodeEventNode.
     */
    @Test
    public void testIsLeaf()
    {
        System.out.println("isLeaf");
        GCodeEventNode instance = new GCodeEventNodeTestImpl();
        assertEquals(true, instance.isLeaf());

        instance.addChild(0, new GCodeEventNodeTestImpl());
        assertEquals(false, instance.isLeaf());
    }

    /**
     * Test of stream method, of class GCodeEventNode.
     */
    @Test
    public void testStream()
    {
        System.out.println("stream");
        GCodeEventNode node1 = new GCodeEventNodeTestImpl();
        GCodeEventNode node1_1 = new GCodeEventNodeTestImpl();
        GCodeEventNode node1_2 = new GCodeEventNodeTestImpl();
        GCodeEventNode node1_3 = new GCodeEventNodeTestImpl();
        GCodeEventNode node1_3_1 = new GCodeEventNodeTestImpl();

        node1.addChild(0, node1_3);
        node1_3.addChild(0, node1_3_1);
        node1.addChild(0, node1_2);
        node1.addChild(0, node1_1);

        //Should yied a stream of:
        //node1_2, node1_3, node1_3_1
        Stream<GCodeEventNode> result = node1.stream();

        ArrayList<GCodeEventNode> resultList = new ArrayList<>();
        result.forEach(node -> resultList.add(node));
        assertEquals(5, resultList.size());
        assertSame(node1, resultList.get(0));
        assertSame(node1_1, resultList.get(1));
        assertSame(node1_2, resultList.get(2));
        assertSame(node1_3, resultList.get(3));
        assertSame(node1_3_1, resultList.get(4));
    }

    /**
     * Test of stream method, of class GCodeEventNode.
     */
    @Test
    public void testStreamChildrenBackwards()
    {
        System.out.println("streamChildrenBackwards");
        GCodeEventNode nodeA = new GCodeEventNodeTestImpl("nodeA");
        GCodeEventNode nodeB1 = new GCodeEventNodeTestImpl("nodeB1");
        GCodeEventNode nodeB2 = new GCodeEventNodeTestImpl("nodeB2");
        GCodeEventNode nodeB3 = new GCodeEventNodeTestImpl("nodeB3");
        GCodeEventNode nodeC1 = new GCodeEventNodeTestImpl("nodeC1");
        GCodeEventNode nodeC2 = new GCodeEventNodeTestImpl("nodeC2");
        GCodeEventNode nodeC3 = new GCodeEventNodeTestImpl("nodeC3");
        GCodeEventNode nodeC4 = new GCodeEventNodeTestImpl("nodeC4");
        GCodeEventNode nodeC5 = new GCodeEventNodeTestImpl("nodeC5");
        GCodeEventNode nodeC6 = new GCodeEventNodeTestImpl("nodeC6");
        GCodeEventNode nodeC7 = new GCodeEventNodeTestImpl("nodeC7");
        GCodeEventNode nodeC8 = new GCodeEventNodeTestImpl("nodeC8");
        GCodeEventNode nodeC9 = new GCodeEventNodeTestImpl("nodeC9");
        GCodeEventNode nodeD1 = new GCodeEventNodeTestImpl("nodeD1");
        GCodeEventNode nodeD2 = new GCodeEventNodeTestImpl("nodeD2");
        GCodeEventNode nodeD3 = new GCodeEventNodeTestImpl("nodeD3");

        nodeB1.addChild(0, nodeC3);
        nodeB1.addChild(0, nodeC2);
        nodeB1.addChild(0, nodeC1);

        nodeB2.addChild(0, nodeC6);
        nodeB2.addChild(0, nodeC5);
        nodeB2.addChild(0, nodeC4);

        nodeB3.addChild(0, nodeC9);
        nodeB3.addChild(0, nodeC8);
        nodeB3.addChild(0, nodeC7);

        nodeC7.addChild(0, nodeD3);
        nodeC7.addChild(0, nodeD2);
        nodeC7.addChild(0, nodeD1);

        nodeA.addChild(0, nodeB3);
        nodeA.addChild(0, nodeB2);
        nodeA.addChild(0, nodeB1);

        // From C8 should be C8
        Stream<GCodeEventNode> result1 = nodeC8.streamChildrenAndMeBackwards();

        ArrayList<GCodeEventNode> resultList1 = new ArrayList<>();
        result1.forEach(node -> resultList1.add(node));

        assertEquals(1, resultList1.size());
        assertSame(nodeC8, resultList1.get(0));

        // From C7 should be D3,D2,D1,C7
        Stream<GCodeEventNode> result2 = nodeC7.streamChildrenAndMeBackwards();

        ArrayList<GCodeEventNode> resultList2 = new ArrayList<>();
        result2.forEach(node -> resultList2.add(node));

        assertEquals(4, resultList2.size());
        assertSame(nodeD3, resultList2.get(0));
        assertSame(nodeD2, resultList2.get(1));
        assertSame(nodeD1, resultList2.get(2));
        assertSame(nodeC7, resultList2.get(3));

        // From B3 should be C9,C8,D3,D2,D1,C7,B3
        Stream<GCodeEventNode> result3 = nodeB3.streamChildrenAndMeBackwards();

        ArrayList<GCodeEventNode> resultList3 = new ArrayList<>();
        result3.forEach(node -> resultList3.add(node));

        assertEquals(7, resultList3.size());
        assertSame(nodeC9, resultList3.get(0));
        assertSame(nodeC8, resultList3.get(1));
        assertSame(nodeD3, resultList3.get(2));
        assertSame(nodeD2, resultList3.get(3));
        assertSame(nodeD1, resultList3.get(4));
        assertSame(nodeC7, resultList3.get(5));
        assertSame(nodeB3, resultList3.get(6));
    }

    /**
     * Test of streamFromHere method, of class GCodeEventNode.
     */
    @Test
    public void testStreamFromHere()
    {
        System.out.println("streamFromHere");
        GCodeEventNode nodeA = new GCodeEventNodeTestImpl("nodeA");
        GCodeEventNode nodeB1 = new GCodeEventNodeTestImpl("nodeB1");
        GCodeEventNode nodeB2 = new GCodeEventNodeTestImpl("nodeB2");
        GCodeEventNode nodeB3 = new GCodeEventNodeTestImpl("nodeB3");
        GCodeEventNode nodeC1 = new GCodeEventNodeTestImpl("nodeC1");
        GCodeEventNode nodeC2 = new GCodeEventNodeTestImpl("nodeC2");
        GCodeEventNode nodeC3 = new GCodeEventNodeTestImpl("nodeC3");
        GCodeEventNode nodeC4 = new GCodeEventNodeTestImpl("nodeC4");
        GCodeEventNode nodeC5 = new GCodeEventNodeTestImpl("nodeC5");
        GCodeEventNode nodeC6 = new GCodeEventNodeTestImpl("nodeC6");
        GCodeEventNode nodeC7 = new GCodeEventNodeTestImpl("nodeC7");
        GCodeEventNode nodeC8 = new GCodeEventNodeTestImpl("nodeC8");
        GCodeEventNode nodeC9 = new GCodeEventNodeTestImpl("nodeC9");

        nodeB1.addChild(0, nodeC3);
        nodeB1.addChild(0, nodeC2);
        nodeB1.addChild(0, nodeC1);

        nodeB2.addChild(0, nodeC6);
        nodeB2.addChild(0, nodeC5);
        nodeB2.addChild(0, nodeC4);

        nodeB3.addChild(0, nodeC9);
        nodeB3.addChild(0, nodeC8);
        nodeB3.addChild(0, nodeC7);

        nodeA.addChild(0, nodeB3);
        nodeA.addChild(0, nodeB2);
        nodeA.addChild(0, nodeB1);

        //Stream from here node C5 should yield a stream of:
        //C5, C6, B3, C7, C8, C9
        try
        {
            Stream<GCodeEventNode> result = nodeC5.streamFromHere();

            ArrayList<GCodeEventNode> resultList = new ArrayList<>();
            result.forEach(node ->
            {
                System.out.println("Adding node " + node.toString());
                resultList.add(node);
            });

            assertEquals(6, resultList.size());
            assertSame(nodeC5, resultList.get(0));
            assertSame(nodeC6, resultList.get(1));
            assertSame(nodeB3, resultList.get(2));
            assertSame(nodeC7, resultList.get(3));
            assertSame(nodeC8, resultList.get(4));
            assertSame(nodeC9, resultList.get(5));
        } catch (NodeProcessingException ex)
        {
            fail("Node processing exception");
        }
    }

    /**
     * Test of streamBackwardsFromHere method, of class GCodeEventNode.
     */
    @Test
    public void testStreamBackwardsFromHere()
    {
        System.out.println("streamBackwardsFromHere");
        GCodeEventNode nodeA = new GCodeEventNodeTestImpl("nodeA");
        GCodeEventNode nodeB1 = new GCodeEventNodeTestImpl("nodeB1");
        GCodeEventNode nodeB2 = new GCodeEventNodeTestImpl("nodeB2");
        GCodeEventNode nodeB3 = new GCodeEventNodeTestImpl("nodeB3");
        GCodeEventNode nodeC1 = new GCodeEventNodeTestImpl("nodeC1");
        GCodeEventNode nodeC2 = new GCodeEventNodeTestImpl("nodeC2");
        GCodeEventNode nodeC3 = new GCodeEventNodeTestImpl("nodeC3");
        GCodeEventNode nodeC4 = new GCodeEventNodeTestImpl("nodeC4");
        GCodeEventNode nodeC5 = new GCodeEventNodeTestImpl("nodeC5");
        GCodeEventNode nodeC6 = new GCodeEventNodeTestImpl("nodeC6");
        GCodeEventNode nodeC7 = new GCodeEventNodeTestImpl("nodeC7");
        GCodeEventNode nodeC8 = new GCodeEventNodeTestImpl("nodeC8");
        GCodeEventNode nodeC9 = new GCodeEventNodeTestImpl("nodeC9");
        GCodeEventNode nodeD1 = new GCodeEventNodeTestImpl("nodeD1");
        GCodeEventNode nodeD2 = new GCodeEventNodeTestImpl("nodeD2");
        GCodeEventNode nodeD3 = new GCodeEventNodeTestImpl("nodeD3");

        nodeB1.addChild(0, nodeC3);
        nodeB1.addChild(0, nodeC2);
        nodeB1.addChild(0, nodeC1);

        nodeB2.addChild(0, nodeC6);
        nodeB2.addChild(0, nodeC5);
        nodeB2.addChild(0, nodeC4);

        nodeB3.addChild(0, nodeC9);
        nodeB3.addChild(0, nodeC8);
        nodeB3.addChild(0, nodeC7);

        nodeC7.addChild(0, nodeD3);
        nodeC7.addChild(0, nodeD2);
        nodeC7.addChild(0, nodeD1);

        nodeA.addChild(0, nodeB3);
        nodeA.addChild(0, nodeB2);
        nodeA.addChild(0, nodeB1);

        //Stream backwards from node C8 should yield a stream of:
        //C8,D3,D2,D1,C7,B3,C6,C5,C4,B2,C3,C2,C1,B1,A
        try
        {
            Stream<GCodeEventNode> result = nodeC8.streamBackwardsFromHere();

            ArrayList<GCodeEventNode> resultList = new ArrayList<>();
            result.forEach(node ->
            {
                System.out.println("Adding node " + node.toString());
                resultList.add(node);
            });

            assertEquals(15, resultList.size());
            assertSame(nodeC8, resultList.get(0));
            assertSame(nodeD3, resultList.get(1));
            assertSame(nodeD2, resultList.get(2));
            assertSame(nodeD1, resultList.get(3));
            assertSame(nodeC7, resultList.get(4));
            assertSame(nodeB3, resultList.get(5));
            assertSame(nodeC6, resultList.get(6));
            assertSame(nodeC5, resultList.get(7));
            assertSame(nodeC4, resultList.get(8));
            assertSame(nodeB2, resultList.get(9));
            assertSame(nodeC3, resultList.get(10));
            assertSame(nodeC2, resultList.get(11));
            assertSame(nodeC1, resultList.get(12));
            assertSame(nodeB1, resultList.get(13));
            assertSame(nodeA, resultList.get(14));
        } catch (NodeProcessingException ex)
        {
            fail("Node processing exception");
        }
    }

    /**
     * Test of addSiblingBefore method, of class GCodeEventNode.
     */
    @Test
    public void testAddSiblingBefore()
    {
        System.out.println("addSiblingBefore");

        GCodeEventNode nodeA = new GCodeEventNodeTestImpl("nodeA");
        GCodeEventNode nodeB1 = new GCodeEventNodeTestImpl("nodeB1");
        GCodeEventNode nodeB2 = new GCodeEventNodeTestImpl("nodeB2");
        GCodeEventNode nodeB3 = new GCodeEventNodeTestImpl("nodeB3");
        GCodeEventNode nodeC1 = new GCodeEventNodeTestImpl("nodeC1");
        GCodeEventNode nodeC2 = new GCodeEventNodeTestImpl("nodeC2");
        GCodeEventNode nodeC3 = new GCodeEventNodeTestImpl("nodeC3");
        GCodeEventNode nodeC4 = new GCodeEventNodeTestImpl("nodeC4");
        GCodeEventNode nodeC5 = new GCodeEventNodeTestImpl("nodeC5");
        GCodeEventNode nodeC6 = new GCodeEventNodeTestImpl("nodeC6");
        GCodeEventNode nodeC7 = new GCodeEventNodeTestImpl("nodeC7");
        GCodeEventNode nodeC8 = new GCodeEventNodeTestImpl("nodeC8");
        GCodeEventNode nodeC9 = new GCodeEventNodeTestImpl("nodeC9");
        GCodeEventNode nodeD1 = new GCodeEventNodeTestImpl("nodeD1");
        GCodeEventNode nodeD2 = new GCodeEventNodeTestImpl("nodeD2");
        GCodeEventNode nodeD3 = new GCodeEventNodeTestImpl("nodeD3");

        nodeB1.addChild(0, nodeC3);
        nodeB1.addChild(0, nodeC2);
        nodeB1.addChild(0, nodeC1);

        nodeB2.addChild(0, nodeC6);
        nodeB2.addChild(0, nodeC5);
        nodeB2.addChild(0, nodeC4);

        nodeB3.addChild(0, nodeC9);
        nodeB3.addChild(0, nodeC8);
        nodeB3.addChild(0, nodeC7);

        nodeC7.addChild(0, nodeD3);
        nodeC7.addChild(0, nodeD2);
        nodeC7.addChild(0, nodeD1);

        nodeA.addChild(0, nodeB3);
        nodeA.addChild(0, nodeB2);
        nodeA.addChild(0, nodeB1);

        GCodeEventNode insertedNode = new GCodeEventNodeTestImpl("InsertedNode");

        nodeC7.addSiblingBefore(insertedNode);

        List<GCodeEventNode> children = nodeC7.getParent().getChildren();

        assertEquals(4, children.size());
        assertSame(insertedNode, children.get(0));
        assertSame(nodeC7, children.get(1));
        assertSame(nodeC8, children.get(2));
        assertSame(nodeC9, children.get(3));
    }

    /**
     * Test of addSiblingAfter method, of class GCodeEventNode.
     */
    @Test
    public void testAddSiblingAfter()
    {
        System.out.println("addSiblingAfter");

        GCodeEventNode nodeA = new GCodeEventNodeTestImpl("nodeA");
        GCodeEventNode nodeB1 = new GCodeEventNodeTestImpl("nodeB1");
        GCodeEventNode nodeB2 = new GCodeEventNodeTestImpl("nodeB2");
        GCodeEventNode nodeB3 = new GCodeEventNodeTestImpl("nodeB3");
        GCodeEventNode nodeC1 = new GCodeEventNodeTestImpl("nodeC1");
        GCodeEventNode nodeC2 = new GCodeEventNodeTestImpl("nodeC2");
        GCodeEventNode nodeC3 = new GCodeEventNodeTestImpl("nodeC3");
        GCodeEventNode nodeC4 = new GCodeEventNodeTestImpl("nodeC4");
        GCodeEventNode nodeC5 = new GCodeEventNodeTestImpl("nodeC5");
        GCodeEventNode nodeC6 = new GCodeEventNodeTestImpl("nodeC6");
        GCodeEventNode nodeC7 = new GCodeEventNodeTestImpl("nodeC7");
        GCodeEventNode nodeC8 = new GCodeEventNodeTestImpl("nodeC8");
        GCodeEventNode nodeC9 = new GCodeEventNodeTestImpl("nodeC9");
        GCodeEventNode nodeD1 = new GCodeEventNodeTestImpl("nodeD1");
        GCodeEventNode nodeD2 = new GCodeEventNodeTestImpl("nodeD2");
        GCodeEventNode nodeD3 = new GCodeEventNodeTestImpl("nodeD3");

        nodeB1.addChild(0, nodeC3);
        nodeB1.addChild(0, nodeC2);
        nodeB1.addChild(0, nodeC1);

        nodeB2.addChild(0, nodeC6);
        nodeB2.addChild(0, nodeC5);
        nodeB2.addChild(0, nodeC4);

        nodeB3.addChild(0, nodeC9);
        nodeB3.addChild(0, nodeC8);
        nodeB3.addChild(0, nodeC7);

        nodeC7.addChild(0, nodeD3);
        nodeC7.addChild(0, nodeD2);
        nodeC7.addChild(0, nodeD1);

        nodeA.addChild(0, nodeB3);
        nodeA.addChild(0, nodeB2);
        nodeA.addChild(0, nodeB1);

        GCodeEventNode insertedNode = new GCodeEventNodeTestImpl("InsertedNode");

        nodeC7.addSiblingAfter(insertedNode);

        List<GCodeEventNode> children = nodeC7.getParent().getChildren();

        assertEquals(4, children.size());
        assertSame(nodeC7, children.get(0));
        assertSame(insertedNode, children.get(1));
        assertSame(nodeC8, children.get(2));
        assertSame(nodeC9, children.get(3));
    }

    /**
     * Test of addSiblingAfter method, of class GCodeEventNode. Add the sibling
     * as the last element
     */
    @Test
    public void testAddSiblingAfter_lastElement()
    {
        System.out.println("addSiblingAfter");

        GCodeEventNode nodeA = new GCodeEventNodeTestImpl("nodeA");
        GCodeEventNode nodeB1 = new GCodeEventNodeTestImpl("nodeB1");
        GCodeEventNode nodeB2 = new GCodeEventNodeTestImpl("nodeB2");
        GCodeEventNode nodeB3 = new GCodeEventNodeTestImpl("nodeB3");
        GCodeEventNode nodeC1 = new GCodeEventNodeTestImpl("nodeC1");
        GCodeEventNode nodeC2 = new GCodeEventNodeTestImpl("nodeC2");
        GCodeEventNode nodeC3 = new GCodeEventNodeTestImpl("nodeC3");
        GCodeEventNode nodeC4 = new GCodeEventNodeTestImpl("nodeC4");
        GCodeEventNode nodeC5 = new GCodeEventNodeTestImpl("nodeC5");
        GCodeEventNode nodeC6 = new GCodeEventNodeTestImpl("nodeC6");
        GCodeEventNode nodeC7 = new GCodeEventNodeTestImpl("nodeC7");
        GCodeEventNode nodeC8 = new GCodeEventNodeTestImpl("nodeC8");
        GCodeEventNode nodeC9 = new GCodeEventNodeTestImpl("nodeC9");
        GCodeEventNode nodeD1 = new GCodeEventNodeTestImpl("nodeD1");
        GCodeEventNode nodeD2 = new GCodeEventNodeTestImpl("nodeD2");
        GCodeEventNode nodeD3 = new GCodeEventNodeTestImpl("nodeD3");

        nodeB1.addChild(0, nodeC3);
        nodeB1.addChild(0, nodeC2);
        nodeB1.addChild(0, nodeC1);

        nodeB2.addChild(0, nodeC6);
        nodeB2.addChild(0, nodeC5);
        nodeB2.addChild(0, nodeC4);

        nodeB3.addChild(0, nodeC9);
        nodeB3.addChild(0, nodeC8);
        nodeB3.addChild(0, nodeC7);

        nodeC7.addChild(0, nodeD3);
        nodeC7.addChild(0, nodeD2);
        nodeC7.addChild(0, nodeD1);

        nodeA.addChild(0, nodeB3);
        nodeA.addChild(0, nodeB2);
        nodeA.addChild(0, nodeB1);

        GCodeEventNode insertedNode = new GCodeEventNodeTestImpl("InsertedNode");

        assertEquals(3, nodeC7.getChildren().size());
        
        nodeD3.addSiblingAfter(insertedNode);

        List<GCodeEventNode> children = nodeC7.getChildren();

        assertEquals(4, children.size());
        assertSame(nodeD1, children.get(0));
        assertSame(nodeD2, children.get(1));
        assertSame(nodeD3, children.get(2));
        assertSame(insertedNode, children.get(3));
    }

    /**
     * Test of removeFromParent method, of class GCodeEventNode.
     */
    @Test
    public void testRemoveFromParent()
    {
        System.out.println("removeFromParent");

        GCodeEventNode nodeA = new GCodeEventNodeTestImpl("nodeA");
        GCodeEventNode nodeB1 = new GCodeEventNodeTestImpl("nodeB1");
        GCodeEventNode nodeB2 = new GCodeEventNodeTestImpl("nodeB2");
        GCodeEventNode nodeB3 = new GCodeEventNodeTestImpl("nodeB3");
        GCodeEventNode nodeC1 = new GCodeEventNodeTestImpl("nodeC1");
        GCodeEventNode nodeC2 = new GCodeEventNodeTestImpl("nodeC2");
        GCodeEventNode nodeC3 = new GCodeEventNodeTestImpl("nodeC3");
        GCodeEventNode nodeC4 = new GCodeEventNodeTestImpl("nodeC4");
        GCodeEventNode nodeC5 = new GCodeEventNodeTestImpl("nodeC5");
        GCodeEventNode nodeC6 = new GCodeEventNodeTestImpl("nodeC6");
        GCodeEventNode nodeC7 = new GCodeEventNodeTestImpl("nodeC7");
        GCodeEventNode nodeC8 = new GCodeEventNodeTestImpl("nodeC8");
        GCodeEventNode nodeC9 = new GCodeEventNodeTestImpl("nodeC9");
        GCodeEventNode nodeD1 = new GCodeEventNodeTestImpl("nodeD1");
        GCodeEventNode nodeD2 = new GCodeEventNodeTestImpl("nodeD2");
        GCodeEventNode nodeD3 = new GCodeEventNodeTestImpl("nodeD3");

        nodeB1.addChild(0, nodeC3);
        nodeB1.addChild(0, nodeC2);
        nodeB1.addChild(0, nodeC1);

        nodeB2.addChild(0, nodeC6);
        nodeB2.addChild(0, nodeC5);
        nodeB2.addChild(0, nodeC4);

        nodeB3.addChild(0, nodeC9);
        nodeB3.addChild(0, nodeC8);
        nodeB3.addChild(0, nodeC7);

        nodeC7.addChild(0, nodeD3);
        nodeC7.addChild(0, nodeD2);
        nodeC7.addChild(0, nodeD1);

        nodeA.addChild(0, nodeB3);
        nodeA.addChild(0, nodeB2);
        nodeA.addChild(0, nodeB1);

        assertEquals(3, nodeC7.getChildren().size());

        nodeD3.removeFromParent();

        List<GCodeEventNode> children = nodeC7.getChildren();

        assertEquals(2, children.size());
        assertSame(nodeD1, children.get(0));
        assertSame(nodeD2, children.get(1));
    }

    /**
     * Test of addChildAtEnd method, of class GCodeEventNode.
     */
    @Test
    public void testAddChildAtEnd()
    {
        System.out.println("addChildAtEnd");

        GCodeEventNode nodeA = new GCodeEventNodeTestImpl("nodeA");
        GCodeEventNode nodeB1 = new GCodeEventNodeTestImpl("nodeB1");
        GCodeEventNode nodeB2 = new GCodeEventNodeTestImpl("nodeB2");
        GCodeEventNode nodeB3 = new GCodeEventNodeTestImpl("nodeB3");
        GCodeEventNode nodeC1 = new GCodeEventNodeTestImpl("nodeC1");
        GCodeEventNode nodeC2 = new GCodeEventNodeTestImpl("nodeC2");
        GCodeEventNode nodeC3 = new GCodeEventNodeTestImpl("nodeC3");
        GCodeEventNode nodeC4 = new GCodeEventNodeTestImpl("nodeC4");
        GCodeEventNode nodeC5 = new GCodeEventNodeTestImpl("nodeC5");
        GCodeEventNode nodeC6 = new GCodeEventNodeTestImpl("nodeC6");
        GCodeEventNode nodeC7 = new GCodeEventNodeTestImpl("nodeC7");
        GCodeEventNode nodeC8 = new GCodeEventNodeTestImpl("nodeC8");
        GCodeEventNode nodeC9 = new GCodeEventNodeTestImpl("nodeC9");
        GCodeEventNode nodeD1 = new GCodeEventNodeTestImpl("nodeD1");
        GCodeEventNode nodeD2 = new GCodeEventNodeTestImpl("nodeD2");
        GCodeEventNode nodeD3 = new GCodeEventNodeTestImpl("nodeD3");

        nodeB1.addChild(0, nodeC3);
        nodeB1.addChild(0, nodeC2);
        nodeB1.addChild(0, nodeC1);

        nodeB2.addChild(0, nodeC6);
        nodeB2.addChild(0, nodeC5);
        nodeB2.addChild(0, nodeC4);

        nodeB3.addChild(0, nodeC9);
        nodeB3.addChild(0, nodeC8);
        nodeB3.addChild(0, nodeC7);

        nodeC7.addChild(0, nodeD3);
        nodeC7.addChild(0, nodeD2);
        nodeC7.addChild(0, nodeD1);

        nodeA.addChild(0, nodeB3);
        nodeA.addChild(0, nodeB2);
        nodeA.addChild(0, nodeB1);

        assertEquals(3, nodeC7.getChildren().size());

        GCodeEventNode addedNode = new GCodeEventNodeTestImpl("AddedNode");

        nodeC7.addChildAtEnd(addedNode);

        List<GCodeEventNode> children = nodeC7.getChildren();

        assertEquals(4, children.size());
        assertSame(nodeD1, children.get(0));
        assertSame(nodeD2, children.get(1));
        assertSame(nodeD3, children.get(2));
        assertSame(addedNode, children.get(3));
    }

    /**
     * Test of getSiblingBefore method, of class GCodeEventNode.
     */
    @Test
    public void testGetSiblingBefore()
    {
        System.out.println("getSiblingBefore");

        GCodeEventNode nodeA = new GCodeEventNodeTestImpl("nodeA");
        GCodeEventNode nodeB1 = new GCodeEventNodeTestImpl("nodeB1");
        GCodeEventNode nodeB2 = new GCodeEventNodeTestImpl("nodeB2");
        GCodeEventNode nodeB3 = new GCodeEventNodeTestImpl("nodeB3");
        GCodeEventNode nodeC1 = new GCodeEventNodeTestImpl("nodeC1");
        GCodeEventNode nodeC2 = new GCodeEventNodeTestImpl("nodeC2");
        GCodeEventNode nodeC3 = new GCodeEventNodeTestImpl("nodeC3");
        GCodeEventNode nodeC4 = new GCodeEventNodeTestImpl("nodeC4");
        GCodeEventNode nodeC5 = new GCodeEventNodeTestImpl("nodeC5");
        GCodeEventNode nodeC6 = new GCodeEventNodeTestImpl("nodeC6");
        GCodeEventNode nodeC7 = new GCodeEventNodeTestImpl("nodeC7");
        GCodeEventNode nodeC8 = new GCodeEventNodeTestImpl("nodeC8");
        GCodeEventNode nodeC9 = new GCodeEventNodeTestImpl("nodeC9");
        GCodeEventNode nodeD1 = new GCodeEventNodeTestImpl("nodeD1");
        GCodeEventNode nodeD2 = new GCodeEventNodeTestImpl("nodeD2");
        GCodeEventNode nodeD3 = new GCodeEventNodeTestImpl("nodeD3");

        nodeB1.addChild(0, nodeC3);
        nodeB1.addChild(0, nodeC2);
        nodeB1.addChild(0, nodeC1);

        nodeB2.addChild(0, nodeC6);
        nodeB2.addChild(0, nodeC5);
        nodeB2.addChild(0, nodeC4);

        nodeB3.addChild(0, nodeC9);
        nodeB3.addChild(0, nodeC8);
        nodeB3.addChild(0, nodeC7);

        nodeC7.addChild(0, nodeD3);
        nodeC7.addChild(0, nodeD2);
        nodeC7.addChild(0, nodeD1);

        nodeA.addChild(0, nodeB3);
        nodeA.addChild(0, nodeB2);
        nodeA.addChild(0, nodeB1);

        Optional<GCodeEventNode> result1 = nodeC8.getSiblingBefore();
        assertTrue(result1.isPresent());
        assertSame(nodeC7, result1.get());

        Optional<GCodeEventNode> result2 = nodeC4.getSiblingBefore();
        assertFalse(result2.isPresent());
    }

    /**
     * Test of getSiblingAfter method, of class GCodeEventNode.
     */
    @Test
    public void testGetSiblingAfter()
    {
        System.out.println("getSiblingAfter");

        GCodeEventNode nodeA = new GCodeEventNodeTestImpl("nodeA");
        GCodeEventNode nodeB1 = new GCodeEventNodeTestImpl("nodeB1");
        GCodeEventNode nodeB2 = new GCodeEventNodeTestImpl("nodeB2");
        GCodeEventNode nodeB3 = new GCodeEventNodeTestImpl("nodeB3");
        GCodeEventNode nodeC1 = new GCodeEventNodeTestImpl("nodeC1");
        GCodeEventNode nodeC2 = new GCodeEventNodeTestImpl("nodeC2");
        GCodeEventNode nodeC3 = new GCodeEventNodeTestImpl("nodeC3");
        GCodeEventNode nodeC4 = new GCodeEventNodeTestImpl("nodeC4");
        GCodeEventNode nodeC5 = new GCodeEventNodeTestImpl("nodeC5");
        GCodeEventNode nodeC6 = new GCodeEventNodeTestImpl("nodeC6");
        GCodeEventNode nodeC7 = new GCodeEventNodeTestImpl("nodeC7");
        GCodeEventNode nodeC8 = new GCodeEventNodeTestImpl("nodeC8");
        GCodeEventNode nodeC9 = new GCodeEventNodeTestImpl("nodeC9");
        GCodeEventNode nodeD1 = new GCodeEventNodeTestImpl("nodeD1");
        GCodeEventNode nodeD2 = new GCodeEventNodeTestImpl("nodeD2");
        GCodeEventNode nodeD3 = new GCodeEventNodeTestImpl("nodeD3");

        nodeB1.addChild(0, nodeC3);
        nodeB1.addChild(0, nodeC2);
        nodeB1.addChild(0, nodeC1);

        nodeB2.addChild(0, nodeC6);
        nodeB2.addChild(0, nodeC5);
        nodeB2.addChild(0, nodeC4);

        nodeB3.addChild(0, nodeC9);
        nodeB3.addChild(0, nodeC8);
        nodeB3.addChild(0, nodeC7);

        nodeC7.addChild(0, nodeD3);
        nodeC7.addChild(0, nodeD2);
        nodeC7.addChild(0, nodeD1);

        nodeA.addChild(0, nodeB3);
        nodeA.addChild(0, nodeB2);
        nodeA.addChild(0, nodeB1);

        Optional<GCodeEventNode> result1 = nodeC7.getSiblingAfter();
        assertTrue(result1.isPresent());
        assertSame(nodeC8, result1.get());

        Optional<GCodeEventNode> result2 = nodeB3.getSiblingAfter();
        assertFalse(result2.isPresent());
    }
}
