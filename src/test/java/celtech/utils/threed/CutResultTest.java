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
    
}
