/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.scene.shape.TriangleMesh;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 *
 * @author tony
 */
public class CutResultTest
{

    @Test
    public void testGetNestedPolygonSetsSingleLoop()
    {
        float Y = 1;
        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getPoints().addAll(0, Y, 0);
        triangleMesh.getPoints().addAll(0, Y, 3);
        triangleMesh.getPoints().addAll(3, Y, 3);
        triangleMesh.getPoints().addAll(3, Y, 0);
        List<List<Integer>> loopsOfVertices = new ArrayList<>();
        List<Integer> outerLoop = new ArrayList<>();
        outerLoop.add(0);
        outerLoop.add(1);
        outerLoop.add(2);
        outerLoop.add(3);
        loopsOfVertices.add(outerLoop);
        CutResult cutResult = new CutResult(triangleMesh, loopsOfVertices);
        Set<Set<List<Integer>>> nestedPolygonSets = cutResult.getNestedPolygonSets();
        assertEquals(1, nestedPolygonSets.size());
    }

    @Test
    public void testGetNestedPolygonSetsTwoOuterLoops()
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

        List<List<Integer>> loopsOfVertices = new ArrayList<>();
        List<Integer> outerLoop1 = new ArrayList<>();
        outerLoop1.add(0);
        outerLoop1.add(1);
        outerLoop1.add(2);
        outerLoop1.add(3);
        loopsOfVertices.add(outerLoop1);
        List<Integer> outerLoop2 = new ArrayList<>();
        outerLoop2.add(4);
        outerLoop2.add(5);
        outerLoop2.add(6);
        outerLoop2.add(7);
        loopsOfVertices.add(outerLoop2);
        CutResult cutResult = new CutResult(triangleMesh, loopsOfVertices);
        Set<Set<List<Integer>>> nestedPolygonSets = cutResult.getNestedPolygonSets();
        assertEquals(2, nestedPolygonSets.size());
    }

    @Test
    public void testGetNestedPolygonSetsOuterLoopWithInner()
    {
        float Y = 1;
        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getPoints().addAll(0, Y, 0);
        triangleMesh.getPoints().addAll(0, Y, 3);
        triangleMesh.getPoints().addAll(3, Y, 3);
        triangleMesh.getPoints().addAll(3, Y, 0);
        
        triangleMesh.getPoints().addAll(1, Y, 1);
        triangleMesh.getPoints().addAll(1, Y, 2);
        triangleMesh.getPoints().addAll(2, Y, 2);
        triangleMesh.getPoints().addAll(2, Y, 1);
        
        List<List<Integer>> loopsOfVertices = new ArrayList<>();
        List<Integer> outerLoop1 = new ArrayList<>();
        outerLoop1.add(0);
        outerLoop1.add(1);
        outerLoop1.add(2);
        outerLoop1.add(3);
        loopsOfVertices.add(outerLoop1);
        List<Integer> outerLoop2 = new ArrayList<>();
        outerLoop2.add(4);
        outerLoop2.add(5);
        outerLoop2.add(6);
        outerLoop2.add(7);
        loopsOfVertices.add(outerLoop2);
        CutResult cutResult = new CutResult(triangleMesh, loopsOfVertices);
        Set<Set<List<Integer>>> nestedPolygonSets = cutResult.getNestedPolygonSets();
        assertEquals(1, nestedPolygonSets.size());
        Set<List<Integer>> nestedPolygonSet = nestedPolygonSets.iterator().next();
        assertEquals(2, nestedPolygonSet.size());
    }

}
