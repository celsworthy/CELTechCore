/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.scene.shape.TriangleMesh;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;


/**
 *
 * @author tony
 */
public class NonManifoldLoopDetectorTest
{

    public static TriangleMesh createSimpleCubeWithMissingFace()
    {
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(0, 0, 0);
        mesh.getPoints().addAll(0, 0, 2);
        mesh.getPoints().addAll(2, 0, 2);
        mesh.getPoints().addAll(2, 0, 0);
        mesh.getPoints().addAll(0, 2, 0);
        mesh.getPoints().addAll(0, 2, 2);
        mesh.getPoints().addAll(2, 2, 2);
        mesh.getPoints().addAll(2, 2, 0);
        // one cube
//        mesh.getFaces().addAll(0, 0, 2, 0, 1, 0);
        mesh.getFaces().addAll(0, 0, 3, 0, 2, 0);
        mesh.getFaces().addAll(0, 0, 1, 0, 5, 0);
        mesh.getFaces().addAll(0, 0, 5, 0, 4, 0);
        mesh.getFaces().addAll(1, 0, 6, 0, 5, 0);
        mesh.getFaces().addAll(1, 0, 2, 0, 6, 0);
        mesh.getFaces().addAll(2, 0, 7, 0, 6, 0);
        mesh.getFaces().addAll(2, 0, 3, 0, 7, 0);
        mesh.getFaces().addAll(3, 0, 4, 0, 7, 0);
        mesh.getFaces().addAll(3, 0, 0, 0, 4, 0);
        mesh.getFaces().addAll(7, 0, 4, 0, 5, 0);
        mesh.getFaces().addAll(7, 0, 5, 0, 6, 0);
        return mesh;
    }

    @Test
    public void testGetNonManifoldEdges()
    {
        TriangleMesh mesh = createSimpleCubeWithMissingFace();
        Set<Edge> edges = NonManifoldLoopDetector.getNonManifoldEdges(mesh);

        Set<Edge> expectedEdges = new HashSet<>();
        expectedEdges.add(new Edge(0, 1));
        expectedEdges.add(new Edge(1, 2));
        expectedEdges.add(new Edge(0, 2));

        assertEquals(expectedEdges, edges);
    }

    @Test
    public void testGetNextNonManifoldLoopSimple()
    {
        Set<Edge> manifoldEdges = new HashSet<>();
        manifoldEdges.add(new Edge(0, 1));
        manifoldEdges.add(new Edge(1, 2));
        manifoldEdges.add(new Edge(0, 2));

        Optional<List<Edge>> loop = NonManifoldLoopDetector.getNextNonManifoldLoop(manifoldEdges);
        
        List<Edge> expectedLoop = new ArrayList<>();
        expectedLoop.add(new Edge(0, 1));
        expectedLoop.add(new Edge(1, 2));
        expectedLoop.add(new Edge(2, 0));
        
        assertEquals(expectedLoop, loop.get());
        
    }
    
    @Test
    public void testGetNextNonManifoldLoopTwoLoops()
    {
        Set<Edge> manifoldEdges = new HashSet<>();
        manifoldEdges.add(new Edge(0, 1));
        manifoldEdges.add(new Edge(1, 2));
        manifoldEdges.add(new Edge(0, 2));
        
        manifoldEdges.add(new Edge(10, 11));
        manifoldEdges.add(new Edge(11, 12));
        manifoldEdges.add(new Edge(10, 12));

        Optional<List<Edge>> loop1 = NonManifoldLoopDetector.getNextNonManifoldLoop(manifoldEdges);
        Optional<List<Edge>> loop2 = NonManifoldLoopDetector.getNextNonManifoldLoop(manifoldEdges);
        
        assertEquals(3, loop1.get().size());
        assertEquals(3, loop2.get().size());
        assertTrue(manifoldEdges.isEmpty());
        
    }    

}
