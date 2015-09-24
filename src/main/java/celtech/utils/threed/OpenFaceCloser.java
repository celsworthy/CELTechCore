/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import static celtech.utils.threed.MeshCutter2.makePoint3D;
import static celtech.utils.threed.TriangleCutter.reverseFaceNormal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;


/**
 * OpenFaceCloser takes a cut mesh, which therefore has an open face) and closes the open face by
 * triangulating the perimeters of the cut walls. It must identify which perimeters are inside
 * which, thereby identifying holes which must be present in the closing face.
 *
 * @author tony
 */
public class OpenFaceCloser
{
    
    private final static Stenographer steno = StenographerFactory.getStenographer(
        OpenFaceCloser.class.getName());

    /**
     * Take the given mesh and vertices of the open face, close the face and add the new face to the
     * mesh and return it.
     */
    static TriangleMesh closeOpenFace(CutResult cutResult, double cutHeight,
        MeshCutter2.BedToLocalConverter bedToLocalConverter)
    {
        TriangleMesh mesh = cutResult.mesh;

//        MeshDebug.visualiseEdgeLoops(mesh, cutResult.loopsOfVerticesOnOpenFace,
//                                           bedToLocalConverter);
        Set<LoopSet> loopSets = cutResult.identifyOuterLoopsAndInnerLoops();
        int MAX_ATTEMPTS = 30;
        for (LoopSet loopSet : loopSets)
        {

            for (Region region : loopSet.getRegions())
            {
                Set<Integer> facesAdded = new HashSet<>();
                int attempts = 0;
                boolean succeeded = false;
                while (!succeeded && attempts < MAX_ATTEMPTS)
                {
                    try
                    {
                        PolygonIndices vertices = region.outerLoop;
//                        if (attempts != 0) {
                        perturbVertices(mesh, vertices);
//                        }
                        Polygon outerPolygon = makePolygon(vertices, mesh, bedToLocalConverter);
                        for (PolygonIndices innerPolygonIndices : region.innerLoops)
                        {
//                            if (attempts != 0) {
                            perturbVertices(mesh, innerPolygonIndices);
//                            }
                            Polygon innerPolygon = makePolygon(innerPolygonIndices, mesh,
                                                               bedToLocalConverter);
                            outerPolygon.addHole(innerPolygon);
                        }

//                        MeshDebug.visualiseDLPolygon(outerPolygon);
//                        System.out.println("outer polygon has vertices: " + outerPolygon.getPoints().size());
                        Poly2Tri.triangulate(outerPolygon);
                        succeeded = true;
                        addTriangulatedFacesToMesh(mesh, outerPolygon, vertices,
                                                   cutHeight, bedToLocalConverter,
                                                   cutResult.topBottom, facesAdded);
                    } catch (Exception | Error ex)
                    {
                        steno.debug("attempts = " + attempts);
                        attempts++;
                    }
                }
                if (attempts == MAX_ATTEMPTS)
                {
                    steno.debug("Unable to triangulate");
                    //                    throw new RuntimeException("Unable to triangulate");
                    
                    // debugging code follows (visualise & also output test code to reproduce
                    // problem in unit test)

//                    System.out.println("outer loop is " + region.outerLoop);
//                    System.out.println("there are inner loops: " + region.innerLoops.size());
//                    for (PolygonIndices innerPolygonIndices : region.innerLoops) {
//                        System.out.println("inner loop is " + innerPolygonIndices);
//                    }
//                    Set<PolygonIndices> polygonIndices = new HashSet<>();
//                    polygonIndices.add(region.outerLoop);
//                    MeshDebug.visualisePolygonIndices(mesh, polygonIndices, region.innerLoops,
//                                  bedToLocalConverter, java.awt.Color.BLUE, java.awt.Color.RED);

                    // get edge data for failing loop (debug only)
//                    List<ManifoldEdge> edges = MeshCutter2.debugLoopToEdges.get(region.outerLoop);
//                    debugOutputEdges(cutResult, edges);
                }
                boolean orientable = MeshUtils.testMeshIsOrientable(mesh);
                if (!orientable)
                {
                    steno.debug("reverse covering face normals for region");
                    for (Integer faceIndex : facesAdded)
                    {
                        reverseFaceNormal(mesh, faceIndex);
                    }
                    orientable = MeshUtils.testMeshIsOrientable(mesh);
                    if (!orientable)
                    {
                        throw new RuntimeException(
                            "mesh is not orientable after triangulating last region!");
                    }
                }
            }

        }

        return mesh;
    }

    /**
     * The code produced by this method can be easily used in NonManifoldLoopDetectorTest class.
     * @param cutResult
     * @param edges 
     */
    private static void debugOutputEdges(CutResult cutResult, List<ManifoldEdge> edges)
    {
        Set<Integer> vertexIndices = new HashSet<>();
        for (ManifoldEdge edge : edges)
        {
            vertexIndices.add(edge.v0);
            vertexIndices.add(edge.v1);
        }
        TriangleMesh mesh = cutResult.mesh;
        MeshCutter2.BedToLocalConverter bedToLocalConverter = cutResult.bedToLocalConverter;

        for (Integer vertexIndex : vertexIndices)
        {
            Point3D point = bedToLocalConverter.localToBed(makePoint3D(mesh, vertexIndex));
            System.out.println(String.format("Point3D p%s = new Point3D(%s, %s, %s);",
                                             vertexIndex, point.getX(), point.getY(), point.getZ()));
        }

        int i = 0;
        for (ManifoldEdge edge : edges)
        {
            System.out.println(String.format(
                "ManifoldEdge edge%s = new ManifoldEdge(%s, %s, p%s, p%s, %s);",
                i, edge.v0, edge.v1, edge.v0, edge.v1, edge.faceIndex));
            i++;
        }
    }


    /**
     * We need to capture the vertex id of perimeter points so that when we get the point back after
     * triangulation we know which point it was (after eg it was perturbed).
     */
    static class PolygonPointWithVertexId extends PolygonPoint
    {

        final int vertexId;

        public PolygonPointWithVertexId(double x, double y, int vertexId)
        {
            super(x, y);
            this.vertexId = vertexId;
        }

    }

    /**
     * Make a Polygon for the given vertices. 3D points should be in bed coordinates so that only X
     * and Z are required (Y being a constant at the cut height in bed coordinates).
     */
    private static Polygon makePolygon(List<Integer> vertices, TriangleMesh mesh,
        MeshCutter2.BedToLocalConverter bedToLocalConverter)
    {
        List<PolygonPoint> points = new ArrayList<>();
        for (Integer vertexIndex : vertices)
        {
            Point3D pointInBed = bedToLocalConverter.localToBed(makePoint3D(mesh, vertexIndex));
            if (pointInBed.getX() > 1e5 || pointInBed.getX() < -1e5 || pointInBed.getZ() > 1e5
                || pointInBed.getZ() < -1e5)
            {
                throw new RuntimeException("invalid point calculated");
            }
            points.add(new PolygonPointWithVertexId(
                pointInBed.getX(),
                pointInBed.getZ(), vertexIndex));
        }
        Polygon outerPolygon = new Polygon(points);
        return outerPolygon;
    }

    /**
     * For each triangle in the polygon add a face to the mesh. If any point in any triangle is not
     * one of the outerVertices then also add that point to the mesh.
     */
    private static void addTriangulatedFacesToMesh(TriangleMesh mesh, Polygon outerPolygon,
        List<Integer> outerVertices, double cutHeight,
        MeshCutter2.BedToLocalConverter bedToLocalConverter, MeshCutter2.TopBottom topBottom,
        Set<Integer> facesAdded)
    {
        // vertexToVertex allows us to identify equal vertices (but different instances) and then
        // get the definitive instance of that vertex, to avoid superfluous vertices in the mesh.
        Map<Vertex, Vertex> vertexToVertex = new HashMap<>();
        // first add already existing vertices for outer perimeter of polygon to vertexToVertex
        for (Integer vertexIndex : outerVertices)
        {
            Point3D point = makePoint3D(mesh, vertexIndex);
            Point3D pointInBed = bedToLocalConverter.localToBed(point);
            Vertex vertex = new Vertex(vertexIndex,
                                       (float) pointInBed.getX(), (float) pointInBed.getY(),
                                       (float) pointInBed.getZ());
            vertexToVertex.put(vertex, vertex);
        }

       steno.debug("add " + outerPolygon.getTriangles().size()
            + " delauney triangles to mesh");
        Set<Integer> outerVerticesUsed = new HashSet<>();
        for (DelaunayTriangle triangle : outerPolygon.getTriangles())
        {
            TriangulationPoint[] points = triangle.points;

            Vertex vertex0;
            int vertex0Index = -1;
            if (points[0] instanceof PolygonPointWithVertexId)
            {
                vertex0Index = ((PolygonPointWithVertexId) points[0]).vertexId;
                outerVerticesUsed.add(vertex0Index);
            } else
            {
                vertex0 = getOrMakeVertexForPoint(mesh, points[0], vertexToVertex, cutHeight,
                                                  bedToLocalConverter);
                vertex0Index = vertex0.meshVertexIndex;
            }
            Vertex vertex1;
            int vertex1Index = -1;
            if (points[1] instanceof PolygonPointWithVertexId)
            {
                vertex1Index = ((PolygonPointWithVertexId) points[1]).vertexId;
                outerVerticesUsed.add(vertex1Index);
            } else
            {
                vertex1 = getOrMakeVertexForPoint(mesh, points[1], vertexToVertex, cutHeight,
                                                  bedToLocalConverter);
                vertex1Index = vertex1.meshVertexIndex;
            }
            Vertex vertex2;
            int vertex2Index = -1;
            if (points[2] instanceof PolygonPointWithVertexId)
            {
                vertex2Index = ((PolygonPointWithVertexId) points[2]).vertexId;
                outerVerticesUsed.add(vertex1Index);
            } else
            {
                vertex2 = getOrMakeVertexForPoint(mesh, points[2], vertexToVertex, cutHeight,
                                                  bedToLocalConverter);
                vertex2Index = vertex2.meshVertexIndex;
            }
            if (topBottom == MeshCutter2.TopBottom.BOTTOM)
            {
                int addedFaceIndex = makeFace(mesh, vertex0Index, vertex1Index,
                                              vertex2Index);
                facesAdded.add(addedFaceIndex);
            } else
            {
                int addedFaceIndex = makeFace(mesh, vertex0Index, vertex2Index,
                                              vertex1Index);
                facesAdded.add(addedFaceIndex);
            }
        }
        steno.debug("Num outer vertices used in triangulation: " + outerVerticesUsed.size());
    }

    private static int makeFace(TriangleMesh mesh, int meshVertexIndex0, int meshVertexIndex1,
        int meshVertexIndex2)
    {
        int[] vertices = new int[6];
        vertices[0] = meshVertexIndex0;
        vertices[2] = meshVertexIndex1;
        vertices[4] = meshVertexIndex2;
        mesh.getFaces().addAll(vertices);
//        steno.debug("make face " + (mesh.getFaces().size() / 6 - 1));
        return mesh.getFaces().size() / 6 - 1;
    }

    /**
     * Make a Vertex for the point, in bed coordinates (so that equality comparisons are all in bed
     * coordinates), and add the points to the mesh in local coordinates.
     */
    private static Vertex getOrMakeVertexForPoint(TriangleMesh mesh, TriangulationPoint point,
        Map<Vertex, Vertex> vertexToVertex, double cutHeight,
        MeshCutter2.BedToLocalConverter bedToLocalConverter)
    {
        Point3D pointInBed = new Point3D(point.getX(), cutHeight, point.getY());
        Vertex vertex = new Vertex((float) pointInBed.getX(), (float) pointInBed.getY(),
                                   (float) pointInBed.getZ());

        Point3D localPoint = bedToLocalConverter.bedToLocal(pointInBed);
        Vertex localVertex = new Vertex((float) localPoint.getX(), (float) localPoint.getY(),
                                        (float) localPoint.getZ());
        if (!vertexToVertex.containsKey(vertex))
        {
            int vertexIndex = TriangleCutter.addNewOrGetVertex(mesh, localVertex);
            steno.debug("triangulation new Vertex");
            vertex.meshVertexIndex = vertexIndex;
            vertexToVertex.put(vertex, vertex);
            return vertex;
        } else
        {
            return vertexToVertex.get(vertex);
        }

    }

    /**
     * Introduce a tiny bit of noise into the XZ position of each perimeter vertex, to avoid
     * problems in the Delauney triangulation.
     */
    private static void perturbVertices(TriangleMesh mesh, PolygonIndices vertices)
    {
        for (Integer vertexIndex : vertices)
        {

            float xValue = mesh.getPoints().get(vertexIndex * 3);
            float zValue = mesh.getPoints().get(vertexIndex * 3 + 2);

            int rawBitsX = Float.floatToIntBits(xValue);
            int rawBitsZ = Float.floatToIntBits(zValue);

            // twiddle the last few bits (out of 22) of the mantissa of the floating point value
            rawBitsX += Math.random() * 10;
            rawBitsZ += Math.random() * 10;

            float newXValue = Float.intBitsToFloat(rawBitsX);
            float newZValue = Float.intBitsToFloat(rawBitsZ);

            mesh.getPoints().set(vertexIndex * 3, newXValue);
            mesh.getPoints().set(vertexIndex * 3 + 2, newZValue);
        }
    }

}
