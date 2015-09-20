/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import static celtech.utils.threed.MeshCutter.makePoint3D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

/**
 * OpenFaceCloser takes a cut mesh, which therefore has an open face) and closes
 * the open face by triangulating the perimeters of the cut walls. It must
 * identify which perimeters are inside which, thereby identifying holes which
 * must be present in the closing face.
 *
 * @author tony
 */
public class OpenFaceCloser {

    /**
     * Take the given mesh and vertices of the open face, close the face and add
     * the new face to the mesh and return it.
     */
    static TriangleMesh closeOpenFace(CutResult cutResult, double cutHeight,
            MeshCutter.BedToLocalConverter bedToLocalConverter) {
        TriangleMesh mesh = cutResult.mesh;
        Set<LoopSet> loopSets = cutResult.identifyOuterLoopsAndInnerLoops();
        int MAX_ATTEMPTS = 30;
        for (LoopSet loopSet : loopSets) {

            for (Region region : loopSet.getRegions()) {
                int attempts = 0;
                boolean succeeded = false;
                while (!succeeded && attempts < MAX_ATTEMPTS) {
                    try {
                        PolygonIndices vertices = region.outerLoop;
                        if (attempts != 0) {
                            perturbVertices(mesh, vertices);
                        }
                        Polygon outerPolygon = makePolygon(vertices, mesh, bedToLocalConverter);
                        for (PolygonIndices innerPolygonIndices : region.innerLoops) {
                            if (attempts != 0) {
                                perturbVertices(mesh, innerPolygonIndices);
                            }
                            Polygon innerPolygon = makePolygon(innerPolygonIndices, mesh,
                                    bedToLocalConverter);
                            outerPolygon.addHole(innerPolygon);
                        }

//                        MeshDebug.visualiseDLPolygon(outerPolygon);
                        Poly2Tri.triangulate(outerPolygon);
                        succeeded = true;
                        addTriangulatedFacesToMesh(mesh, outerPolygon, vertices,
                                cutHeight, bedToLocalConverter,
                                cutResult.topBottom);
                    } catch (Exception ex) {
                        System.out.println("attempts = " + attempts);
                        attempts++;
                    }
                }
                if (attempts == MAX_ATTEMPTS) {
                    System.out.println("outer loop is " + region.outerLoop);
                    for (PolygonIndices innerPolygonIndices : region.innerLoops) {
                        System.out.println("inner loop is " + innerPolygonIndices);
                    }
//                    throw new RuntimeException("Unable to triangulate");
                    System.out.println("Unable to triangulate");
                }
            }
        }

        return mesh;
    }

    /**
     * We need to capture the vertex id of perimeter points so that when we get
     * the point back after triangulation we know which point it was (after eg
     * it was perturbed).
     */
    static class PolygonPointWithVertexId extends PolygonPoint {

        final int vertexId;

        public PolygonPointWithVertexId(double x, double y, int vertexId) {
            super(x, y);
            this.vertexId = vertexId;
        }

    }

    /**
     * Make a Polygon for the given vertices. 3D points should be in bed
     * coordinates so that only X and Z are required (Y being a constant at the
     * cut height in bed coordinates).
     */
    private static Polygon makePolygon(List<Integer> vertices, TriangleMesh mesh,
            MeshCutter.BedToLocalConverter bedToLocalConverter) {
        List<PolygonPoint> points = new ArrayList<>();
        for (Integer vertexIndex : vertices) {
            Point3D pointInBed = bedToLocalConverter.localToBed(makePoint3D(mesh, vertexIndex));
            if (pointInBed.getX() > 1e5 || pointInBed.getX() < -1e5 || pointInBed.getZ() > 1e5
                    || pointInBed.getZ() < -1e5) {
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
     * For each triangle in the polygon add a face to the mesh. If any point in
     * any triangle is not one of the outerVertices then also add that point to
     * the mesh.
     */
    private static void addTriangulatedFacesToMesh(TriangleMesh mesh, Polygon outerPolygon,
            List<Integer> outerVertices, double cutHeight,
            MeshCutter.BedToLocalConverter bedToLocalConverter, MeshCutter.TopBottom topBottom) {
        // vertexToVertex allows us to identify equal vertices (but different instances) and then
        // get the definitive instance of that vertex, to avoid superfluous vertices in the mesh.
        Map<Vertex, Vertex> vertexToVertex = new HashMap<>();
        // first add already existing vertices for outer perimeter of polygon to vertexToVertex
        for (Integer vertexIndex : outerVertices) {
            Point3D point = makePoint3D(mesh, vertexIndex);
            Point3D pointInBed = bedToLocalConverter.localToBed(point);
            Vertex vertex = new Vertex(vertexIndex,
                    (float) pointInBed.getX(), (float) pointInBed.getY(),
                    (float) pointInBed.getZ());
            vertexToVertex.put(vertex, vertex);
        }

        System.out.println("add " + outerPolygon.getTriangles().size() + " delauney triangles to mesh");
        for (DelaunayTriangle triangle : outerPolygon.getTriangles()) {
            TriangulationPoint[] points = triangle.points;

            Vertex vertex0;
            int vertex0Index = -1;
            if (points[0] instanceof PolygonPointWithVertexId) {
                vertex0Index = ((PolygonPointWithVertexId) points[0]).vertexId;
            } else {
                vertex0 = getOrMakeVertexForPoint(mesh, points[0], vertexToVertex, cutHeight,
                        bedToLocalConverter);
                vertex0Index = vertex0.meshVertexIndex;
            }
            Vertex vertex1;
            int vertex1Index = -1;
            if (points[1] instanceof PolygonPointWithVertexId) {
                vertex1Index = ((PolygonPointWithVertexId) points[1]).vertexId;
            } else {
                vertex1 = getOrMakeVertexForPoint(mesh, points[1], vertexToVertex, cutHeight,
                        bedToLocalConverter);
                vertex1Index = vertex1.meshVertexIndex;
            }
            Vertex vertex2;
            int vertex2Index = -1;
            if (points[2] instanceof PolygonPointWithVertexId) {
                vertex2Index = ((PolygonPointWithVertexId) points[2]).vertexId;
            } else {
                vertex2 = getOrMakeVertexForPoint(mesh, points[2], vertexToVertex, cutHeight,
                        bedToLocalConverter);
                vertex2Index = vertex2.meshVertexIndex;
            }
            if (topBottom == MeshCutter.TopBottom.BOTTOM) {
                makeFace(mesh, vertex0Index, vertex1Index,
                        vertex2Index);
            } else {
                makeFace(mesh, vertex0Index, vertex2Index,
                        vertex1Index);
            }
        }
    }

    private static void makeFace(TriangleMesh mesh, int meshVertexIndex0, int meshVertexIndex1,
            int meshVertexIndex2) {
        int[] vertices = new int[6];
        vertices[0] = meshVertexIndex0;
        vertices[2] = meshVertexIndex1;
        vertices[4] = meshVertexIndex2;
        mesh.getFaces().addAll(vertices);
    }

    /**
     * Make a Vertex for the point, in bed coordinates (so that equality
     * comparisons are all in bed coordinates), and add the points to the mesh
     * in local coordinates.
     */
    private static Vertex getOrMakeVertexForPoint(TriangleMesh mesh, TriangulationPoint point,
            Map<Vertex, Vertex> vertexToVertex, double cutHeight,
            MeshCutter.BedToLocalConverter bedToLocalConverter) {
        Point3D pointInBed = new Point3D(point.getX(), cutHeight, point.getY());
        Vertex vertex = new Vertex((float) pointInBed.getX(), (float) pointInBed.getY(),
                (float) pointInBed.getZ());

        Point3D pointInLocal = bedToLocalConverter.bedToLocal(pointInBed);
        if (!vertexToVertex.containsKey(vertex)) {
            int vertexIndex = TriangleCutter.addNewOrGetVertex(mesh, vertex);
            vertex.meshVertexIndex = vertexIndex;
            vertexToVertex.put(vertex, vertex);
            return vertex;
        } else {
            return vertexToVertex.get(vertex);
        }

    }

    /**
     * Introduce a tiny bit of noise (maximum 1 micron) into the XZ position of
     * each perimeter vertex, to avoid problems in the Delauney triangulation.
     */
    private static void perturbVertices(TriangleMesh mesh, PolygonIndices vertices) {
        for (Integer vertexIndex : vertices) {
            float perturbationX = (float) (Math.random() / 1e3);
            float perturbationY = (float) (Math.random() / 1e3);
            float perturbationZ = (float) (Math.random() / 1e3);

            mesh.getPoints().set(vertexIndex * 3, mesh.getPoints().get(vertexIndex * 3)
                    + perturbationX);
            // we add a perturbation in Y to introduce some noise into rotated models 
            mesh.getPoints().set(vertexIndex * 3 + 1, mesh.getPoints().get(vertexIndex * 3 + 1)
                    + perturbationY);
            mesh.getPoints().set(vertexIndex * 3 + 2, mesh.getPoints().get(vertexIndex * 3 + 2)
                    + perturbationZ);
        }
    }

}
