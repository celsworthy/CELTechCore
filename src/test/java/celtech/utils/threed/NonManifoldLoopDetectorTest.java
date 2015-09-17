/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import static celtech.utils.threed.TriangleCutter.getVertex;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        Set<ManifoldEdge> edges = NonManifoldLoopDetector.getNonManifoldEdges(mesh);

        Set<ManifoldEdge> expectedEdges = new HashSet<>();
        expectedEdges.add(new ManifoldEdge(0, 1, getVertex(mesh, 0), getVertex(mesh, 1)));
        expectedEdges.add(new ManifoldEdge(1, 2, getVertex(mesh, 1), getVertex(mesh, 2)));
        expectedEdges.add(new ManifoldEdge(0, 2, getVertex(mesh, 0), getVertex(mesh, 2)));

        assertEquals(expectedEdges, edges);
    }

    @Test
    public void testGetNextNonManifoldLoopSimple()
    {
        TriangleMesh mesh = createSimpleCubeWithMissingFace();

        Set<ManifoldEdge> manifoldEdges = new HashSet<>();
        ManifoldEdge edge0 = new ManifoldEdge(0, 1, getVertex(mesh, 0), getVertex(mesh, 1));
        manifoldEdges.add(edge0);
        manifoldEdges.add(new ManifoldEdge(1, 2, getVertex(mesh, 1), getVertex(mesh, 2)));
        manifoldEdges.add(new ManifoldEdge(0, 2, getVertex(mesh, 0), getVertex(mesh, 2)));

        Map<Integer, Set<ManifoldEdge>> edgesWithVertex = NonManifoldLoopDetector.makeEdgesWithVertex(
            manifoldEdges);
        Optional<List<ManifoldEdge>> loop = NonManifoldLoopDetector.getLoopForEdgeInDirection(edge0,
                                     edgesWithVertex, NonManifoldLoopDetector.Direction.FORWARDS);

        List<ManifoldEdge> expectedLoop = new ArrayList<>();
        expectedLoop.add(new ManifoldEdge(0, 1, null, null));
        expectedLoop.add(new ManifoldEdge(1, 2, null, null));
        expectedLoop.add(new ManifoldEdge(2, 0, null, null));

        assertEquals(expectedLoop, loop.get());

    }

    @Test
    public void testGetNextNonManifoldLoopTwoLoops()
    {

        TriangleMesh mesh = createSimpleCubeWithMissingFace();

        Set<Edge> manifoldEdges = new HashSet<>();
        manifoldEdges.add(new Edge(0, 1));
        manifoldEdges.add(new Edge(1, 2));
        manifoldEdges.add(new Edge(0, 2));

        manifoldEdges.add(new Edge(10, 11));
        manifoldEdges.add(new Edge(11, 12));
        manifoldEdges.add(new Edge(10, 12));

//        Map<Integer, Set<Edge>> edgesWithVertex = NonManifoldLoopDetector.makeEdgesWithVertex(
//            manifoldEdges);
//
//        Optional<List<Edge>> loop1 = NonManifoldLoopDetector.getNextNonManifoldLoop(manifoldEdges,
//                                                                                    edgesWithVertex,
//                                                                                    mesh);
//        Optional<List<Edge>> loop2 = NonManifoldLoopDetector.getNextNonManifoldLoop(manifoldEdges,
//                                                                                    edgesWithVertex,
//                                                                                    mesh);
//
//        assertEquals(3, loop1.get().size());
//        assertEquals(3, loop2.get().size());
//        assertTrue(manifoldEdges.isEmpty());

    }

    @Test
    public void testIdentifyNonManifoldLoops()
    {

        TriangleMesh mesh = createSimpleCubeWithMissingFace();

//        Set<List<Edge>> loops = NonManifoldLoopDetector.identifyNonManifoldLoops(mesh);
//        System.out.println(loops);
//        assertEquals(1, loops.size());
//
//        List<Edge> expectedLoop = new ArrayList<>();
//        expectedLoop.add(new Edge(0, 1));
//        expectedLoop.add(new Edge(1, 2));
//        expectedLoop.add(new Edge(2, 0));
//
//        assertEquals(expectedLoop, loops.iterator().next());
    }

}
