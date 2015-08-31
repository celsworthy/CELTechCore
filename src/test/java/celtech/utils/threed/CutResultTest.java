/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 *
 * @author tony
 */
public class CutResultTest
{
    
    MeshCutter.BedToLocalConverter nullConverter = new MeshCutter.BedToLocalConverter()
    {

        @Override
        public Point3D localToBed(Point3D point)
        {
            return point;
        }

        @Override
        public Point3D bedToLocal(Point3D point)
        {
            return point;
        }
    };

    TriangleMesh makeTriangleMesh()
    {
        float Y = 1;
        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getPoints().addAll(0, Y, 0);
        triangleMesh.getPoints().addAll(0, Y, 3);
        triangleMesh.getPoints().addAll(3, Y, 3);
        triangleMesh.getPoints().addAll(3, Y, 0);

        triangleMesh.getPoints().addAll(4, Y, 4);
        triangleMesh.getPoints().addAll(4, Y, 7);
        triangleMesh.getPoints().addAll(7, Y, 7);
        triangleMesh.getPoints().addAll(7, Y, 4);

        triangleMesh.getPoints().addAll(1, Y, 1);
        triangleMesh.getPoints().addAll(1, Y, 2);
        triangleMesh.getPoints().addAll(2, Y, 2);
        triangleMesh.getPoints().addAll(2, Y, 1);

        return triangleMesh;
    }

    private PolygonIndices makeLoop0_3()
    {
        PolygonIndices outerLoop = new PolygonIndices();
        outerLoop.add(0);
        outerLoop.add(1);
        outerLoop.add(2);
        outerLoop.add(3);
        return outerLoop;
    }

    private PolygonIndices makeLoop4_7()
    {
        PolygonIndices outerLoop2 = new PolygonIndices();
        outerLoop2.add(4);
        outerLoop2.add(5);
        outerLoop2.add(6);
        outerLoop2.add(7);
        return outerLoop2;
    }

    private PolygonIndices makeLoop1_2()
    {
        PolygonIndices outerLoop3 = new PolygonIndices();
        outerLoop3.add(8);
        outerLoop3.add(9);
        outerLoop3.add(10);
        outerLoop3.add(11);
        return outerLoop3;
    }

    @Test
    public void testGetNestedPolygonSetsSingleLoop()
    {
        TriangleMesh triangleMesh = makeTriangleMesh();
        List<PolygonIndices> loopsOfVertices = new ArrayList<>();
        PolygonIndices outerLoop = makeLoop0_3();
        loopsOfVertices.add(outerLoop);
        CutResult cutResult = new CutResult(triangleMesh, loopsOfVertices, nullConverter, MeshCutter.TopBottom.BOTTOM);
        Set<Set<PolygonIndices>> nestedPolygonSets = cutResult.getNestedPolygonSets();
        assertEquals(1, nestedPolygonSets.size());
    }

    @Test
    public void testGetNestedPolygonSetsTwoOuterLoops()
    {
        TriangleMesh triangleMesh = makeTriangleMesh();

        List<PolygonIndices> loopsOfVertices = new ArrayList<>();
        PolygonIndices outerLoop1 = makeLoop0_3();
        loopsOfVertices.add(outerLoop1);
        PolygonIndices outerLoop2 = makeLoop4_7();
        loopsOfVertices.add(outerLoop2);
        CutResult cutResult = new CutResult(triangleMesh, loopsOfVertices, nullConverter, MeshCutter.TopBottom.BOTTOM);
        Set<Set<PolygonIndices>> nestedPolygonSets = cutResult.getNestedPolygonSets();
        assertEquals(2, nestedPolygonSets.size());
    }

    @Test
    public void testGetNestedPolygonSetsOuterLoopWithInner()
    {
        TriangleMesh triangleMesh = makeTriangleMesh();

        List<PolygonIndices> loopsOfVertices = new ArrayList<>();
        PolygonIndices outerLoop1 = makeLoop0_3();
        loopsOfVertices.add(outerLoop1);

        PolygonIndices outerLoop3 = makeLoop1_2();
        loopsOfVertices.add(outerLoop3);
        CutResult cutResult = new CutResult(triangleMesh, loopsOfVertices, nullConverter, MeshCutter.TopBottom.BOTTOM);
        Set<Set<PolygonIndices>> nestedPolygonSets = cutResult.getNestedPolygonSets();
        assertEquals(1, nestedPolygonSets.size());
        Set<PolygonIndices> nestedPolygonSet = nestedPolygonSets.iterator().next();
        assertEquals(2, nestedPolygonSet.size());
    }
    
    @Test
    public void testIdentifyOuterLoopsAndInnerLoopsOuterLoopWithInner()
    {
        TriangleMesh triangleMesh = makeTriangleMesh();

        List<PolygonIndices> loopsOfVertices = new ArrayList<>();
        PolygonIndices outerLoop1 = makeLoop0_3();
        loopsOfVertices.add(outerLoop1);

        PolygonIndices outerLoop3 = makeLoop1_2();
        loopsOfVertices.add(outerLoop3);
        CutResult cutResult = new CutResult(triangleMesh, loopsOfVertices, nullConverter, MeshCutter.TopBottom.BOTTOM);
        Set<LoopSet> nestedPolygonSets = cutResult.identifyOuterLoopsAndInnerLoops();
        assertEquals(1, nestedPolygonSets.size());
        LoopSet loopSet = nestedPolygonSets.iterator().next();
        assertEquals(1, loopSet.innerLoops.size());
    }    

}
