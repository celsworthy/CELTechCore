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
    public void testLoopForEdgeInDirectionSimple()
    {
        Vertex vertex0 = new Vertex(1, 0, 0);
        Vertex vertex1 = new Vertex(2, 0, 0);
        Vertex vertex2 = new Vertex(1, 0, 1);
        Vertex vertex3 = new Vertex(2, 0, 1);
        Vertex vertex4 = new Vertex(3, 0, 1);
        Vertex vertex5 = new Vertex(4, 0, 1);
        Vertex vertex6 = new Vertex(1, 0, 3);
        Vertex vertex7 = new Vertex(2, 0, 3);
        Vertex vertex8 = new Vertex(0, 0, 2);
        Vertex vertex9 = new Vertex(0, 0, 4);

        Set<ManifoldEdge> manifoldEdges = new HashSet<>();
        ManifoldEdge edge0 = new ManifoldEdge(0, 1, vertex0, vertex1);
        manifoldEdges.add(edge0);
        manifoldEdges.add(new ManifoldEdge(1, 2, vertex1, vertex2));
        manifoldEdges.add(new ManifoldEdge(0, 2, vertex0, vertex2));

        Map<Integer, Set<ManifoldEdge>> edgesWithVertex = NonManifoldLoopDetector.makeEdgesWithVertex(
            manifoldEdges);
        Optional<List<ManifoldEdge>> loop = NonManifoldLoopDetector.getLoopForEdgeInDirection(edge0,
                                                                                              edgesWithVertex,
                                                                                              NonManifoldLoopDetector.Direction.FORWARDS);

        List<ManifoldEdge> expectedLoop = new ArrayList<>();
        expectedLoop.add(new ManifoldEdge(0, 1, null, null));
        expectedLoop.add(new ManifoldEdge(1, 2, null, null));
        expectedLoop.add(new ManifoldEdge(2, 0, null, null));

        assertEquals(expectedLoop, loop.get());

    }

    @Test
    public void testGetLoopForEdgeInDirectionTwoLoops()
    {

        Vertex vertex0 = new Vertex(1, 0, 0);
        Vertex vertex1 = new Vertex(2, 0, 0);
        Vertex vertex2 = new Vertex(1, 0, 1);
        Vertex vertex3 = new Vertex(2, 0, 1);
        Vertex vertex4 = new Vertex(3, 0, 1);
        Vertex vertex5 = new Vertex(4, 0, 1);
        Vertex vertex6 = new Vertex(1, 0, 3);
        Vertex vertex7 = new Vertex(2, 0, 3);
        Vertex vertex8 = new Vertex(0, 0, 2);
        Vertex vertex9 = new Vertex(0, 0, 4);

        Set<ManifoldEdge> manifoldEdges = new HashSet<>();
        ManifoldEdge edge0 = new ManifoldEdge(0, 1, vertex0, vertex1);
        manifoldEdges.add(edge0);
        manifoldEdges.add(new ManifoldEdge(1, 3, vertex1, vertex3));
        manifoldEdges.add(new ManifoldEdge(3, 2, vertex3, vertex2));
        manifoldEdges.add(new ManifoldEdge(2, 0, vertex2, vertex0));
        ManifoldEdge edge1 = new ManifoldEdge(3, 7, vertex3, vertex7);
        manifoldEdges.add(edge1);
        manifoldEdges.add(new ManifoldEdge(7, 6, vertex7, vertex6));
        manifoldEdges.add(new ManifoldEdge(6, 2, vertex6, vertex2));

        Map<Integer, Set<ManifoldEdge>> edgesWithVertex = NonManifoldLoopDetector.makeEdgesWithVertex(
            manifoldEdges);

        Optional<List<ManifoldEdge>> loop1 = NonManifoldLoopDetector.getLoopForEdgeInDirection(edge0,
                                                                                               edgesWithVertex,
                                                                                               NonManifoldLoopDetector.Direction.BACKWARDS);
        for (ManifoldEdge manifoldEdge : loop1.get())
        {
            System.out.println(manifoldEdge);
        }
        assertEquals(4, loop1.get().size());

        Optional<List<ManifoldEdge>> loop2 = NonManifoldLoopDetector.getLoopForEdgeInDirection(edge1,
                                                                                               edgesWithVertex,
                                                                                               NonManifoldLoopDetector.Direction.FORWARDS);
        assertEquals(3, loop2.get().size());

    }

    @Test
    public void testIdentifyNonManifoldLoops()
    {

        TriangleMesh mesh = createSimpleCubeWithMissingFace();

        Set<List<ManifoldEdge>> loops = NonManifoldLoopDetector.identifyNonManifoldLoops(mesh);
        System.out.println(loops);
        assertEquals(1, loops.size());

        List<ManifoldEdge> expectedLoop = new ArrayList<>();
        expectedLoop.add(new ManifoldEdge(0, 1, null, null));
        expectedLoop.add(new ManifoldEdge(1, 2, null, null));
        expectedLoop.add(new ManifoldEdge(2, 0, null, null));

        assertEquals(expectedLoop, loops.iterator().next());
    }

    @Test
    public void testGetRightmostEdge()
    {

        Vertex vertex0 = new Vertex(1, 0, 0);
        Vertex vertex1 = new Vertex(2, 0, 0);
        Vertex vertex2 = new Vertex(1, 0, 1);
        Vertex vertex3 = new Vertex(2, 0, 1);
        Vertex vertex4 = new Vertex(3, 0, 1);
        Vertex vertex5 = new Vertex(4, 0, 1);
        Vertex vertex6 = new Vertex(1, 0, 3);
        Vertex vertex7 = new Vertex(2, 0, 3);
        Vertex vertex8 = new Vertex(0, 0, 2);
        Vertex vertex9 = new Vertex(0, 0, 4);

        Set<ManifoldEdge> manifoldEdges = new HashSet<>();
        ManifoldEdge edge0 = new ManifoldEdge(0, 1, vertex0, vertex1);
        ManifoldEdge edge1 = new ManifoldEdge(1, 3, vertex1, vertex3);
        ManifoldEdge edge2 = new ManifoldEdge(3, 2, vertex3, vertex2);
        ManifoldEdge edge3 = new ManifoldEdge(0, 2, vertex0, vertex2);
        ManifoldEdge edge4 = new ManifoldEdge(3, 7, vertex3, vertex7);
        ManifoldEdge edge5 = new ManifoldEdge(7, 6, vertex7, vertex6);
        ManifoldEdge edge6 = new ManifoldEdge(6, 2, vertex6, vertex2);
        manifoldEdges.add(edge0);
        manifoldEdges.add(edge1);
        manifoldEdges.add(edge2);
        manifoldEdges.add(edge3);
        manifoldEdges.add(edge4);
        manifoldEdges.add(edge5);
        manifoldEdges.add(edge6);

        Map<Integer, Set<ManifoldEdge>> edgesWithVertex = NonManifoldLoopDetector.makeEdgesWithVertex(
            manifoldEdges);
        
        int vertexId = 2;
        Set<ManifoldEdge> availableEdges = new HashSet<>(edgesWithVertex.get(vertexId));
        availableEdges.remove(edge3);
        
        ManifoldEdge rightmostEdge = NonManifoldLoopDetector.getRightmostEdge(vertexId, edge3,
                                                                availableEdges);
        assertEquals(edge2, rightmostEdge);

        
        vertexId = 3;
        availableEdges = new HashSet<>(edgesWithVertex.get(vertexId));
        availableEdges.remove(edge1);
        
        rightmostEdge = NonManifoldLoopDetector.getRightmostEdge(vertexId, edge1,
                                                                availableEdges);
        assertEquals(edge4, rightmostEdge);
        
        vertexId = 3;
        availableEdges = new HashSet<>(edgesWithVertex.get(vertexId));
        availableEdges.remove(edge4);
        
        rightmostEdge = NonManifoldLoopDetector.getRightmostEdge(vertexId, edge4,
                                                                availableEdges);
        assertEquals(edge2, rightmostEdge);
        
    }

}
