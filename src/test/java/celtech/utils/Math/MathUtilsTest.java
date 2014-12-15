/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils.Math;

import javafx.geometry.Point2D;
import org.apache.commons.math3.geometry.euclidean.twod.Line;
import org.apache.commons.math3.geometry.euclidean.twod.Segment;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
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
public class MathUtilsTest
{

    public MathUtilsTest()
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

//    /**
//     * Test of invSqrt method, of class MathUtils.
//     */
//    @Test
//    public void testInvSqrt()
//    {
//        System.out.println("invSqrt");
//        double x = 0.0;
//        double expResult = 0.0;
//        double result = MathUtils.invSqrt(x);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of matrixRotateNode method, of class MathUtils.
//     */
//    @Test
//    public void testMatrixRotateNode()
//    {
//        System.out.println("matrixRotateNode");
//        Node n = null;
//        double alf = 0.0;
//        double bet = 0.0;
//        double gam = 0.0;
//        MathUtils.matrixRotateNode(n, alf, bet, gam);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of sphericalToCartesianLocalSpaceAdjusted method, of class MathUtils.
//     */
//    @Test
//    public void testSphericalToCartesianLocalSpaceAdjusted()
//    {
//        System.out.println("sphericalToCartesianLocalSpaceAdjusted");
//        PolarCoordinate polarCoordinate = null;
//        Point3D expResult = null;
//        Point3D result = MathUtils.sphericalToCartesianLocalSpaceAdjusted(polarCoordinate);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of cartesianToSphericalLocalSpaceAdjusted method, of class MathUtils.
//     */
//    @Test
//    public void testCartesianToSphericalLocalSpaceAdjusted()
//    {
//        System.out.println("cartesianToSphericalLocalSpaceAdjusted");
//        Point3D cartesianCoordinate = null;
//        PolarCoordinate expResult = null;
//        PolarCoordinate result = MathUtils.cartesianToSphericalLocalSpaceAdjusted(cartesianCoordinate);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of sphericalToCartesianLocalSpaceUnadjusted method, of class MathUtils.
//     */
//    @Test
//    public void testSphericalToCartesianLocalSpaceUnadjusted()
//    {
//        System.out.println("sphericalToCartesianLocalSpaceUnadjusted");
//        PolarCoordinate polarCoordinate = null;
//        Point3D expResult = null;
//        Point3D result = MathUtils.sphericalToCartesianLocalSpaceUnadjusted(polarCoordinate);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of cartesianToSphericalLocalSpaceUnadjusted method, of class MathUtils.
//     */
//    @Test
//    public void testCartesianToSphericalLocalSpaceUnadjusted()
//    {
//        System.out.println("cartesianToSphericalLocalSpaceUnadjusted");
//        Point3D cartesianCoordinate = null;
//        PolarCoordinate expResult = null;
//        PolarCoordinate result = MathUtils.cartesianToSphericalLocalSpaceUnadjusted(cartesianCoordinate);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of cartesianToAngleDegreesCWFromTop method, of class MathUtils.
//     */
//    @Test
//    public void testCartesianToAngleDegreesCWFromTop()
//    {
//        System.out.println("cartesianToAngleDegreesCWFromTop");
//        double xPos = 0.0;
//        double yPos = 0.0;
//        double expResult = 0.0;
//        double result = MathUtils.cartesianToAngleDegreesCWFromTop(xPos, yPos);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
    /**
     * Test of angleDegreesToCartesianCWFromTop method, of class MathUtils.
     */
    @Test
    public void testAngleDegreesToCartesianCWFromTop()
    {
        System.out.println("angleDegreesToCartesianCWFromTop");
        double angle = 0.0;
        double radius = 10.0;
        Point2D expResult = new Point2D(0, -10);
        Point2D result = MathUtils.angleDegreesToCartesianCWFromTop(angle, radius, true);
        assertEquals(expResult, result);
    }
//
//    /**
//     * Test of cartesianToAngleDegreesCCWFromRight method, of class MathUtils.
//     */
//    @Test
//    public void testCartesianToAngleDegreesCCWFromRight()
//    {
//        System.out.println("cartesianToAngleDegreesCCWFromRight");
//        double xPos = 0.0;
//        double yPos = 0.0;
//        double expResult = 0.0;
//        double result = MathUtils.cartesianToAngleDegreesCCWFromRight(xPos, yPos);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of boundAzimuthRadians method, of class MathUtils.
//     */
//    @Test
//    public void testBoundAzimuthRadians()
//    {
//        System.out.println("boundAzimuthRadians");
//        double azimuth = 0.0;
//        double expResult = 0.0;
//        double result = MathUtils.boundAzimuthRadians(azimuth);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of boundAzimuthDegrees method, of class MathUtils.
//     */
//    @Test
//    public void testBoundAzimuthDegrees()
//    {
//        System.out.println("boundAzimuthDegrees");
//        double azimuth = 0.0;
//        double expResult = 0.0;
//        double result = MathUtils.boundAzimuthDegrees(azimuth);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    /**
     * Test of getOrthogonalLineToLinePoints method, of class MathUtils.
     */
    @Test
    public void testGetOrthogonalLineToLinePoints()
    {
        System.out.println("getOrthogonalLineToLinePoints");
        double orthogonalLength = 3;
        Vector2D startPoint = new Vector2D(0, 5);
        Vector2D endPoint = new Vector2D(10, 5);
        Vector2D expectedStartPoint = new Vector2D(5, 8);
        Vector2D expectedEndPoint = new Vector2D(5, 2);
        Segment result = MathUtils.getOrthogonalLineToLinePoints(orthogonalLength, startPoint, endPoint);
        assertEquals(expectedStartPoint, result.getStart());
        assertEquals(expectedEndPoint, result.getEnd());
    }
    
        /**
     * Test of getOrthogonalLineToLinePoints method, of class MathUtils.
     */
    @Test
    public void testGetOrthogonalLineToLinePointsReverseDirection()
    {
        System.out.println("getOrthogonalLineToLinePoints");
        double orthogonalLength = 3;
        Vector2D startPoint = new Vector2D(0, -3);
        Vector2D endPoint = new Vector2D(10, -3);
        Vector2D expectedStartPoint = new Vector2D(5,-6);
        Vector2D expectedEndPoint = new Vector2D(5, 0);
        Segment result = MathUtils.getOrthogonalLineToLinePoints(orthogonalLength, endPoint, startPoint);
        assertEquals(expectedStartPoint, result.getStart());
        assertEquals(expectedEndPoint, result.getEnd());
    }

    /**
     * Test of getSegmentIntersection method, of class MathUtils.
     */
    @Test
    public void testGetSegmentIntersection()
    {
        System.out.println("getSegmentIntersection");

        Vector2D firstLineStart = new Vector2D(0, 0);
        Vector2D firstLineEnd = new Vector2D(10, 0);
        Line firstLine = new Line(firstLineStart, firstLineEnd, 1e-12);
        Segment firstSegment = new Segment(firstLineStart, firstLineEnd, firstLine);

        Vector2D secondLineStart = new Vector2D(5, 5);
        Vector2D secondLineEnd = new Vector2D(5, -5);
        Line secondLine = new Line(secondLineStart, secondLineEnd, 1e-12);
        Segment secondSegment = new Segment(secondLineStart, secondLineEnd, secondLine);

        Vector2D thirdLineStart = new Vector2D(15, 5);
        Vector2D thirdLineEnd = new Vector2D(15, -5);
        Line thirdLine = new Line(thirdLineStart, thirdLineEnd, 1e-12);
        Segment thirdSegment = new Segment(thirdLineStart, thirdLineEnd, thirdLine);

        Vector2D expResult = new Vector2D(5, 0);
        Vector2D result = MathUtils.getSegmentIntersection(firstSegment, secondSegment);

        assertEquals(expResult, result);

        Vector2D nullResult = MathUtils.getSegmentIntersection(firstSegment, thirdSegment);
        assertNull(nullResult);
    }

    /**
     * Test of doesPointLieWithinSegment method, of class MathUtils.
     */
    @Test
    public void testDoesPointLieWithinSegment()
    {
        System.out.println("doesPointLieWithinSegment");
        Vector2D passPoint = new Vector2D(107.47999999999999, 64.53900000000002);
        Vector2D failPoint = new Vector2D(107.47999999999999, 61.53900000000002);

        Vector2D firstLineStart = new Vector2D(107.48, 65.55500005626678);
        Vector2D firstLineEnd = new Vector2D(107.48, 62.322999943733215);
        
        Line firstLine = new Line(firstLineStart, firstLineEnd, 1e-12);
        Segment firstSegment = new Segment(firstLineStart, firstLineEnd, firstLine);

        boolean passResult = MathUtils.doesPointLieWithinSegment(passPoint, firstSegment);

        assertTrue(passResult);

        boolean failResult = MathUtils.doesPointLieWithinSegment(failPoint, firstSegment);
        assertFalse(failResult);
    }

}
