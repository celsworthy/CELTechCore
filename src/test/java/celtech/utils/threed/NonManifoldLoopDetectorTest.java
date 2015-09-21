/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import static celtech.utils.threed.TriangleCutterTest.makeNullConverter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();
        TriangleMesh mesh = createSimpleCubeWithMissingFace();
        Set<ManifoldEdge> edges = NonManifoldLoopDetector.getNonManifoldEdges(mesh,
                                                                          nullBedToLocalConverter);

        Set<ManifoldEdge> expectedEdges = new HashSet<>();
        Point3D point0InBed = MeshCutter.makePoint3D(mesh, 0);
        Point3D point1InBed = MeshCutter.makePoint3D(mesh, 1);
        Point3D point2InBed = MeshCutter.makePoint3D(mesh, 2);
        
        expectedEdges.add(new ManifoldEdge(0, 1, point0InBed, point1InBed));
        expectedEdges.add(new ManifoldEdge(1, 2, point1InBed, point2InBed));
        expectedEdges.add(new ManifoldEdge(0, 2, point0InBed, point2InBed));

        assertEquals(expectedEdges, edges);
    }

    @Test
    public void testLoopForEdgeInDirectionSimple()
    {
        Point3D vertex0 = new Point3D(1, 0, 0);
        Point3D vertex1 = new Point3D(2, 0, 0);
        Point3D vertex2 = new Point3D(1, 0, 1);
        Point3D vertex3 = new Point3D(2, 0, 1);
        Point3D vertex4 = new Point3D(3, 0, 1);
        Point3D vertex5 = new Point3D(4, 0, 1);
        Point3D vertex6 = new Point3D(1, 0, 3);
        Point3D vertex7 = new Point3D(2, 0, 3);
        Point3D vertex8 = new Point3D(0, 0, 2);
        Point3D vertex9 = new Point3D(0, 0, 4);

        Set<ManifoldEdge> manifoldEdges = new HashSet<>();
        ManifoldEdge edge0 = new ManifoldEdge(0, 1, vertex0, vertex1);
        manifoldEdges.add(edge0);
        manifoldEdges.add(new ManifoldEdge(1, 2, vertex1, vertex2));
        manifoldEdges.add(new ManifoldEdge(0, 2, vertex0, vertex2));

        Map<Integer, Set<ManifoldEdge>> edgesWithPoint3D = NonManifoldLoopDetector.makeEdgesWithVertex(
            manifoldEdges);
        Optional<List<ManifoldEdge>> loop = NonManifoldLoopDetector.getLoopForEdgeInDirection(edge0,
                                                                                              edgesWithPoint3D,
                                                                                              NonManifoldLoopDetector.Direction.FORWARDS);

        List<ManifoldEdge> expectedLoop = new ArrayList<>();
        expectedLoop.add(new ManifoldEdge(0, 1, null, null));
        expectedLoop.add(new ManifoldEdge(1, 2, null, null));
        expectedLoop.add(new ManifoldEdge(2, 0, null, null));

        assertEquals(expectedLoop, loop.get());

    }

    @Test
    public void testGetLoopForEdgeInDirectionTwoAdjacentLoops()
    {

        Point3D vertex0 = new Point3D(1, 0, 0);
        Point3D vertex1 = new Point3D(2, 0, 0);
        Point3D vertex2 = new Point3D(1, 0, 1);
        Point3D vertex3 = new Point3D(2, 0, 1);
        Point3D vertex4 = new Point3D(3, 0, 1);
        Point3D vertex5 = new Point3D(4, 0, 1);
        Point3D vertex6 = new Point3D(1, 0, 3);
        Point3D vertex7 = new Point3D(2, 0, 3);
        Point3D vertex8 = new Point3D(0, 0, 2);
        Point3D vertex9 = new Point3D(0, 0, 4);

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
        assertEquals(6, loop2.get().size());
        
        Set<List<ManifoldEdge>> loops = new HashSet<>();
        loops.add(loop1.get());
        loops.add(loop2.get());
        
//        MeshDebug.visualiseEdgeLoops(manifoldEdges, loops);
        
        assertFalse(NonManifoldLoopDetector.loopHasChord(loop1.get(), edgesWithVertex));
        assertTrue(NonManifoldLoopDetector.loopHasChord(loop2.get(), edgesWithVertex));
        
        loops = NonManifoldLoopDetector.removeLoopsWithChords(loops, edgesWithVertex);
        assertEquals(1, loops.size());
        

    }

    @Test
    public void testIdentifyNonManifoldLoops()
    {

        TriangleMesh mesh = createSimpleCubeWithMissingFace();

        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();
        
        Set<List<ManifoldEdge>> loops = NonManifoldLoopDetector.identifyNonManifoldLoops(mesh,
                                                                          nullBedToLocalConverter);
        for (List<ManifoldEdge> loop : loops)
        {
            System.out.println("XXXX");
            for (ManifoldEdge edge : loop)
            {
                System.out.println(edge.v0 + "->" + edge.v1);
            }
        }
        assertEquals(1, loops.size());

        List<ManifoldEdge> expectedLoop = new ArrayList<>();
        expectedLoop.add(new ManifoldEdge(0, 1, null, null));
        expectedLoop.add(new ManifoldEdge(0, 2, null, null));
        expectedLoop.add(new ManifoldEdge(2, 1, null, null));

        assertEquals(expectedLoop, loops.iterator().next());
    }

    @Test
    public void testGetRightmostEdge()
    {

        Point3D vertex0 = new Point3D(1, 0, 0);
        Point3D vertex1 = new Point3D(2, 0, 0);
        Point3D vertex2 = new Point3D(1, 0, 1);
        Point3D vertex3 = new Point3D(2, 0, 1);
        Point3D vertex4 = new Point3D(3, 0, 1);
        Point3D vertex5 = new Point3D(4, 0, 1);
        Point3D vertex6 = new Point3D(1, 0, 3);
        Point3D vertex7 = new Point3D(2, 0, 3);
        Point3D vertex8 = new Point3D(0, 0, 2);
        Point3D vertex9 = new Point3D(0, 0, 4);

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

        Map<Integer, Set<ManifoldEdge>> edgesWithPoint3D = NonManifoldLoopDetector.makeEdgesWithVertex(
            manifoldEdges);
        
        int vertexId = 2;
        Set<ManifoldEdge> availableEdges = new HashSet<>(edgesWithPoint3D.get(vertexId));
        availableEdges.remove(edge3);
        
        ManifoldEdge rightmostEdge = NonManifoldLoopDetector.getRightmostEdge(vertexId, edge3,
                                                                availableEdges);
        assertEquals(edge2, rightmostEdge);

        
        vertexId = 3;
        availableEdges = new HashSet<>(edgesWithPoint3D.get(vertexId));
        availableEdges.remove(edge1);
        
        rightmostEdge = NonManifoldLoopDetector.getRightmostEdge(vertexId, edge1,
                                                                availableEdges);
        assertEquals(edge4, rightmostEdge);
        
        vertexId = 3;
        availableEdges = new HashSet<>(edgesWithPoint3D.get(vertexId));
        availableEdges.remove(edge4);
        
        rightmostEdge = NonManifoldLoopDetector.getRightmostEdge(vertexId, edge4,
                                                                availableEdges);
        assertEquals(edge2, rightmostEdge);
        
    }
    
    @Test
    public void testGetLoop() {
        
        Point3D vertex9 = new Point3D(-6.999998f, -9.999f, -14.001001f);
        Point3D vertex8 = new Point3D(-16.999998f, -9.999f, -14.001001f);
        Point3D vertex10 = new Point3D(3.000002f, -9.999f, -14.001001f);
        Point3D vertex11 = new Point3D(3.000002f, -9.999f, -4.0005016f);
        Point3D vertex14 = new Point3D(-16.999998f, -9.999f, -4.0005016f);
        Point3D vertex13 = new Point3D(-16.999998f, -9.999f, 6.0f);
        Point3D vertex12 = new Point3D(3.000002f, -9.999f, 6.0f);
        Point3D vertex15 = new Point3D(-7.000002f, -9.999f, 6.0f);
        
        Set<ManifoldEdge> manifoldEdges = new HashSet<>();
        ManifoldEdge edge0 = new ManifoldEdge(9, 8, vertex9, vertex8); 
        ManifoldEdge edge1 = new ManifoldEdge(10, 9, vertex10, vertex9); 
        ManifoldEdge edge2 = new ManifoldEdge(11, 10, vertex11, vertex10); 
        ManifoldEdge edge3 = new ManifoldEdge(8, 14, vertex8, vertex14); 
        ManifoldEdge edge4 = new ManifoldEdge(12, 11, vertex12, vertex11); 
        ManifoldEdge edge5 = new ManifoldEdge(14, 13, vertex14, vertex13); 
        ManifoldEdge edge6 = new ManifoldEdge(15, 12, vertex15, vertex12); 
        ManifoldEdge edge7 = new ManifoldEdge(13, 15, vertex13, vertex15); 
        
        manifoldEdges.add(edge0);
        manifoldEdges.add(edge1);
        manifoldEdges.add(edge2);
        manifoldEdges.add(edge3);
        manifoldEdges.add(edge4);
        manifoldEdges.add(edge5);
        manifoldEdges.add(edge6);
        manifoldEdges.add(edge7);
        
        Map<Integer, Set<ManifoldEdge>> edgesWithPoint3D = NonManifoldLoopDetector.makeEdgesWithVertex(
            manifoldEdges);

        Optional<List<ManifoldEdge>> loop = NonManifoldLoopDetector.getLoopForEdgeInDirection(
                edge0, edgesWithPoint3D, NonManifoldLoopDetector.Direction.FORWARDS);
        for (ManifoldEdge manifoldEdge : loop.get())
        {
            System.out.println(manifoldEdge);
        }
        assertEquals(8, loop.get().size());
        
        Set<List<ManifoldEdge>> loops = new HashSet<>();
        loops.add(loop.get());
        
//        MeshDebug.visualiseEdgeLoops(manifoldEdges, loops);
        
        
       }

}
