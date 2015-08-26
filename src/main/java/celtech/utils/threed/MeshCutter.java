/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import celtech.coreUI.visualisation.ApplicationMaterials;
import celtech.modelcontrol.ModelContainer;
import static celtech.utils.threed.MeshSeparator.addPointToMesh;
import static celtech.utils.threed.MeshSeparator.setTextureAndSmoothing;
import static celtech.utils.threed.MeshSeparator.makeFacesWithVertex;
import com.sun.javafx.scene.shape.ObservableFaceArrayImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;


/**
 * MeshCutter cuts a mesh along a given horizontal plane, triangulates the two resultant open faces,
 * and creates two child meshes.
 *
 * @author tony
 */
public class MeshCutter
{

    public interface BedToLocalConverter
    {

        Point3D localToBed(Point3D point);

        Point3D bedToLocal(Point3D point);
    }

    /**
     * Cut the given mesh into two, at the given height.
     */
    public static Set<TriangleMesh> cut(TriangleMesh mesh, double cutHeight,
        BedToLocalConverter bedToLocalConverter)
    {
//        showIncomingMesh(mesh);

        Set<TriangleMesh> triangleMeshs = new HashSet<>();

        Set<CutResult> splitResults = getUncoveredUpperAndLowerMeshes(mesh, cutHeight,
                                                                      bedToLocalConverter);
        for (CutResult splitResult : splitResults)
        {
            TriangleMesh childMesh = closeOpenFace(splitResult, cutHeight);
            setTextureAndSmoothing(childMesh, childMesh.getFaces().size() / 6);
            triangleMeshs.add(childMesh);
        }

        return triangleMeshs;
    }

    private static Set<CutResult> getUncoveredUpperAndLowerMeshes(TriangleMesh mesh,
        double cutHeight, BedToLocalConverter bedToLocalConverter)
    {
        Set<CutResult> cutResults = new HashSet<>();
        List<PolygonIndices> loopsOfFaces = getCutFaceIndices(mesh, cutHeight);

        List<PolygonIndices> loopsOfVertices = new ArrayList<>();

        for (PolygonIndices loopOfFaces : loopsOfFaces)
        {
            PolygonIndices newVertices = makeNewVerticesAlongCut(mesh, cutHeight, loopOfFaces);
            loopsOfVertices.add(newVertices);
        }

        TriangleMesh lowerMesh = makeLowerMesh(mesh, loopsOfFaces, loopsOfVertices, cutHeight,
                                               bedToLocalConverter);
        CutResult cutResult = new CutResult(lowerMesh, loopsOfVertices);
        cutResults.add(cutResult);
        return cutResults;
    }

    /**
     * Given the mesh, cut faces and intersection points, create the lower child mesh. Copy the
     * original mesh, remove all the cut faces and replace with a new set of faces using the new
     * intersection points. Remove all the faces from above the cut faces.
     */
    private static TriangleMesh makeLowerMesh(TriangleMesh mesh, List<PolygonIndices> loopsOfFaces,
        List<PolygonIndices> loopsOfVertices, double cutHeight,
        BedToLocalConverter bedToLocalConverter)
    {
        TriangleMesh childMesh = new TriangleMesh();
        childMesh.getPoints().addAll(mesh.getPoints());
        childMesh.getFaces().addAll(mesh.getFaces());
        setTextureAndSmoothing(childMesh, childMesh.getFaces().size() / 6);

        List<Integer> allCutFaces = new ArrayList<>();
        for (List<Integer> loopOfFaces : loopsOfFaces)
        {
            allCutFaces.addAll(loopOfFaces);
        }

        removeCutFacesAndFacesAboveCutFaces(childMesh, allCutFaces, cutHeight, bedToLocalConverter);
        for (int i = 0; i < loopsOfFaces.size(); i++)
        {
            List<Integer> loopOfFaces = loopsOfFaces.get(i);
            List<Integer> loopOfVertices = loopsOfVertices.get(i);
            addLowerFacesAroundCut(mesh, childMesh, loopOfFaces, loopOfVertices, cutHeight,
                                   bedToLocalConverter);
        }

        showFace(mesh, 0);

        return childMesh;
    }

    private static void addLowerFacesAroundCut(TriangleMesh mesh, TriangleMesh childMesh,
        List<Integer> cutFaces, List<Integer> newVertices, double cutHeight,
        BedToLocalConverter bedToLocalConverter)
    {
        assert (cutFaces.size() == newVertices.size());
        for (int index = 0; index < cutFaces.size(); index++)
        {
            int vertexIndex1 = 0;
            int cutFaceIndex = cutFaces.get(index);
            int vertexIndex0 = newVertices.get(index);
            if (index == cutFaces.size() - 1)
            {
                vertexIndex1 = newVertices.get(0);
            } else
            {
                vertexIndex1 = newVertices.get(index + 1);
            }
            addLowerDividedFaceToChild(mesh, childMesh, cutFaceIndex, vertexIndex0, vertexIndex1,
                                       cutHeight, bedToLocalConverter);
        }
        setTextureAndSmoothing(childMesh, childMesh.getFaces().size() / 6);
    }

    private static Vertex getVertex(TriangleMesh mesh, int vertexIndex)
    {
        float x = mesh.getPoints().get(vertexIndex * 3);
        float y = mesh.getPoints().get(vertexIndex * 3 + 1);
        float z = mesh.getPoints().get(vertexIndex * 3 + 2);
        return new Vertex(x, y, z);
    }
    
    private static Point3D makePoint3D(TriangleMesh mesh, int v0)
    {
        float x = mesh.getPoints().get(v0 * 3);
        float y = mesh.getPoints().get(v0 * 3 + 1);
        float z = mesh.getPoints().get(v0 * 3 + 2);
        return new Point3D(x, y, z);
    }


    /**
     * Cut the face using the given vertexes, and add the lower face(s) to the child mesh.
     */
    private static void addLowerDividedFaceToChild(TriangleMesh mesh, TriangleMesh childMesh,
        int faceIndex, int vertexIntersect0, int vertexIntersect1, double cutHeight,
        BedToLocalConverter bedToLocalConverter)
    {
        int v0 = mesh.getFaces().get(faceIndex * 6);
        int v1 = mesh.getFaces().get(faceIndex * 6 + 2);
        int v2 = mesh.getFaces().get(faceIndex * 6 + 4);

        boolean b01 = lineIntersectsPlane(mesh, v0, v1, cutHeight);
        boolean b12 = lineIntersectsPlane(mesh, v1, v2, cutHeight);
        boolean b02 = lineIntersectsPlane(mesh, v0, v2, cutHeight);

        // indices of intersecting vertices between v0->v1 etc
        int v01 = -1;
        int v12 = -1;
        int v02 = -1;

        // get vertex index for intersections v01, v12, v02
        if (b01)
        {
            Vertex vertex01 = getIntersectingVertex(new Edge(v0, v1), mesh, cutHeight);
            if (vertex01.equals(getVertex(mesh, vertexIntersect0)))
            {
                v01 = vertexIntersect0;
            } else
            {
                assert (vertex01.equals(getVertex(mesh, vertexIntersect1)));
                v01 = vertexIntersect1;
            }
        }

        if (b12)
        {
            Vertex vertex12 = getIntersectingVertex(new Edge(v1, v2), mesh, cutHeight);
            if (vertex12.equals(getVertex(mesh, vertexIntersect0)))
            {
                v12 = vertexIntersect0;
            } else
            {
                assert (vertex12.equals(getVertex(mesh, vertexIntersect1))) : vertex12 + " "
                    + getVertex(mesh, vertexIntersect1);
                v12 = vertexIntersect1;
            }
        }

        if (b02)
        {
            Vertex vertex20 = getIntersectingVertex(new Edge(v0, v2), mesh, cutHeight);
            if (vertex20.equals(getVertex(mesh, vertexIntersect0)))
            {
                v02 = vertexIntersect0;
            } else
            {
                assert (vertex20.equals(getVertex(mesh, vertexIntersect1)));
                v02 = vertexIntersect1;
            }
        }
        
        float v0Height = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, v0)).getY();
        float v1Height = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, v1)).getY();
        float v2Height = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, v2)).getY();

        // are points below cut?
        boolean v0belowCut = v0Height > cutHeight;
        boolean v1belowCut = v1Height > cutHeight;
        boolean v2belowCut = v2Height > cutHeight;

        int numPointsBelowCut = 0;
        numPointsBelowCut += v0belowCut ? 1 : 0;
        numPointsBelowCut += v1belowCut ? 1 : 0;
        numPointsBelowCut += v2belowCut ? 1 : 0;

        // corner indices for new face A
        int c0 = -1;
        int c1 = -1;
        int c2 = -1;
        // corner indices for new face B
        int c3 = -1;
        int c4 = -1;
        int c5 = -1;
        if (numPointsBelowCut == 1)
        {
            // add face A
            if (v0belowCut)
            {
                c0 = v0;
                c1 = v01;
                c2 = v02;
            } else if (v1belowCut)
            {
                c0 = v1;
                c1 = v12;
                c2 = v01;
            } else if (v2belowCut)
            {
                c0 = v2;
                c1 = v02;
                c2 = v12;
            } else
            {
                throw new RuntimeException("Unexpected condition");
            }

            assert (c0 != -1 && c1 != -1 && c2 != -1);
            assert (c0 != c1 && c1 != c2 && c2 != c0) : c0 + " " + c1 + " " + c2;

            int[] vertices = new int[6];
            vertices[0] = c0;
            vertices[2] = c1;
            vertices[4] = c2;
            childMesh.getFaces().addAll(vertices);
            System.out.println("add face type A " + c0 + " " + c1 + " " + c2);

        } else
        {
            // add faces A and B 
            if (v0belowCut && v1belowCut)
            {
                c0 = v0;
                c1 = v1;
                c2 = v12;
                c3 = v0;
                c4 = v12;
                c5 = v02;
            } else if (v1belowCut && v2belowCut)
            {
                c0 = v1;
                c1 = v2;
                c2 = v02;
                c3 = v1;
                c4 = v02;
                c5 = v01;
            } else if (v2belowCut && v0belowCut)
            {
                c0 = v2;
                c1 = v0;
                c2 = v01;
                c3 = v2;
                c4 = v01;
                c5 = v12;
            } else
            {
                throw new RuntimeException("Unexpected condition");
            }

            assert (c0 != -1 && c1 != -1 && c2 != -1 && c3 != -1 && c4 != -1 && c5 != -1);
            assert (c0 != c1 && c1 != c2 && c2 != c0) : c0 + " " + c1 + " " + c2;
            assert (c3 != c4 && c4 != c5 && c5 != c3) : c3 + " " + c4 + " " + c5;

            int[] vertices = new int[6];
            vertices[0] = c0;
            vertices[2] = c1;
            vertices[4] = c2;
            childMesh.getFaces().addAll(vertices);
            System.out.println("add face type B " + c0 + " " + c1 + " " + c2);
            vertices[0] = c3;
            vertices[2] = c4;
            vertices[4] = c5;
            childMesh.getFaces().addAll(vertices);
            System.out.println("add face type C " + c3 + " " + c4 + " " + c5);
        }

    }

    /**
     * Remove the cut faces and any other faces above cut height from the mesh.
     */
    private static void removeCutFacesAndFacesAboveCutFaces(TriangleMesh mesh,
        List<Integer> cutFaces, double cutHeight, BedToLocalConverter bedToLocalConverter)
    {
        Set<Integer> facesAboveCut = new HashSet<>();

        // compare vertices -Y to cutHeight
        for (int faceIndex = 0; faceIndex < mesh.getFaces().size() / 6; faceIndex++)
        {
            int vertex0 = mesh.getFaces().get(faceIndex * 6);
            float vertex0YInBed = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, vertex0)).getY();
            if (vertex0YInBed < cutHeight)
            {
                facesAboveCut.add(faceIndex);
                continue;
            }
            int vertex1 = mesh.getFaces().get(faceIndex * 6 + 2);
            float vertex1YInBed = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, vertex1)).getY();
            if (vertex1YInBed < cutHeight)
            {
                facesAboveCut.add(faceIndex);
                continue;
            }
            int vertex2 = mesh.getFaces().get(faceIndex * 6 + 4);
            float vertex2YInBed = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, vertex2)).getY();
            if (vertex2YInBed < cutHeight)
            {
                facesAboveCut.add(faceIndex);
                continue;
            }
        }

        ObservableFaceArray newFaceArray = new ObservableFaceArrayImpl();
        for (int faceIndex = 0; faceIndex < mesh.getFaces().size() / 6; faceIndex++)
        {
            if (facesAboveCut.contains(faceIndex))
            {
                continue;
            }
            int[] vertices = new int[6];
            vertices[0] = mesh.getFaces().get(faceIndex * 6);
            vertices[2] = mesh.getFaces().get(faceIndex * 6 + 2);
            vertices[4] = mesh.getFaces().get(faceIndex * 6 + 4);
            newFaceArray.addAll(vertices);
        }
        mesh.getFaces().setAll(newFaceArray);
        setTextureAndSmoothing(mesh, mesh.getFaces().size() / 6);
    }

    /**
     * Taking the ordered list of cut faces, return an ordered list of new intersecting vertices.
     */
    private static PolygonIndices makeNewVerticesAlongCut(TriangleMesh mesh, double cutHeight,
        PolygonIndices cutFaces)
    {
//        System.out.println("make new vertices for cut faces " + cutFaces);
        PolygonIndices newVertices = new PolygonIndices();

        Edge commonEdgeOfFace0And1 = getCommonEdge(mesh, cutFaces.get(0), cutFaces.get(1));
//        System.out.println("common edge of 0 and 1 is " + commonEdgeOfFace0And1);
        Set<Edge> face0Edges = getEdgesOfFaceThatPlaneIntersects(mesh, cutFaces.get(0), cutHeight);;
//        System.out.println("face 0 edges are " + face0Edges);
//        System.out.println("face 1 edges are " + getFaceEdges(mesh, cutFaces.get(1)));
        face0Edges.remove(commonEdgeOfFace0And1);
//        System.out.println("face 0 edges are " + face0Edges);
        Edge firstEdge = face0Edges.iterator().next();
//        System.out.println("first edge is " + firstEdge);
        newVertices.add(makeIntersectingVertex(mesh, firstEdge, cutHeight));

        Edge previousEdge = firstEdge;
        for (Integer faceIndex : cutFaces)
        {
//            System.out.println("face index " + faceIndex);
            Set<Edge> faceEdges = getEdgesOfFaceThatPlaneIntersects(mesh, faceIndex, cutHeight);
//            System.out.println("previous edge is " + previousEdge);
//            System.out.println("face edges is " + faceEdges);
            faceEdges.remove(previousEdge);
//            System.out.println("face edges is " + faceEdges);
            assert (faceEdges.size() == 1) : faceIndex + " " + faceEdges.size();
            Edge nextEdge = faceEdges.iterator().next();
            newVertices.add(makeIntersectingVertex(mesh, nextEdge, cutHeight));
            previousEdge = nextEdge;
        }

        // last added vertex should be same as first - remove it
        newVertices.remove(newVertices.get(newVertices.size() - 1));

//        System.out.println("new vertices are " + newVertices);
        showNewVertices(newVertices, mesh);
        return newVertices;
    }

    /**
     * Return the edge that is shared between the two faces.
     */
    private static Edge getCommonEdge(TriangleMesh mesh, int faceIndex0, int faceIndex1)
    {
        Set<Edge> edges0 = getFaceEdges(mesh, faceIndex0);
        Set<Edge> edges1 = getFaceEdges(mesh, faceIndex1);
        edges0.retainAll(edges1);
        assert (edges0.size() == 1);
        return edges0.iterator().next();
    }

    private static Set<Edge> getFaceEdges(TriangleMesh mesh, int faceIndex)
    {
        int vertex0 = mesh.getFaces().get(faceIndex * 6);
        int vertex1 = mesh.getFaces().get(faceIndex * 6 + 2);
        int vertex2 = mesh.getFaces().get(faceIndex * 6 + 4);
        Edge edge1 = new Edge(vertex0, vertex1);
        Edge edge2 = new Edge(vertex1, vertex2);
        Edge edge3 = new Edge(vertex0, vertex2);
        Set<Edge> edges = new HashSet<>();
        edges.add(edge1);
        edges.add(edge2);
        edges.add(edge3);
        return edges;
    }

    /**
     * Calculate the coordinates of the intersection with the edge, add a new vertex at that point
     * and return the index of the new vertex.
     */
    private static Integer makeIntersectingVertex(TriangleMesh mesh, Edge edge, double cutHeight)
    {
        Vertex vertex = getIntersectingVertex(edge, mesh, cutHeight);
        mesh.getPoints().addAll((float) vertex.x, (float) vertex.y, (float) vertex.z);
        return mesh.getPoints().size() / 3 - 1;
    }

    private static Vertex getIntersectingVertex(Edge edge, TriangleMesh mesh, double cutHeight)
    {
        int v0 = edge.v0;
        int v1 = edge.v1;
        double v0X = mesh.getPoints().get(v0 * 3);
        double v1X = mesh.getPoints().get(v1 * 3);
        double v0Y = mesh.getPoints().get(v0 * 3 + 1);
        double v1Y = mesh.getPoints().get(v1 * 3 + 1);
        double v0Z = mesh.getPoints().get(v0 * 3 + 2);
        double v1Z = mesh.getPoints().get(v1 * 3 + 2);
        double proportionAlongEdge;
        if (Math.abs(v1Y - v0Y) < 1e-7)
        {
            proportionAlongEdge = 0.5;
        } else
        {
            proportionAlongEdge = (cutHeight - v0Y) / (v1Y - v0Y);
        }
        float interX = (float) (v0X + (v1X - v0X) * proportionAlongEdge);
        float interZ = (float) (v0Z + (v1Z - v0Z) * proportionAlongEdge);
        Vertex vertex = new Vertex(interX, (float) cutHeight, interZ);
        return vertex;
    }

    /**
     * Get the ordered list of face indices that are cut by the plane. The faces must be ordered
     * according to adjacency, so that we can get a correct list of ordered vertices on the
     * perimeter.
     */
    private static List<PolygonIndices> getCutFaceIndices(TriangleMesh mesh, double cutHeight)
    {
        boolean[] faceVisited = new boolean[mesh.getFaces().size() / 6];
        Map<Integer, Set<Integer>> facesWithVertices = makeFacesWithVertex(mesh);

        List<PolygonIndices> loopsOfFaces = new ArrayList<>();

        while (true)
        {
            PolygonIndices cutFaceIndices = getNextFaceLoop(faceVisited, mesh, cutHeight,
                                                            facesWithVertices);
            if (cutFaceIndices.size() > 0)
            {
                loopsOfFaces.add(cutFaceIndices);
            } else
            {
                break;
            }
        }

        return loopsOfFaces;
    }

    private static PolygonIndices getNextFaceLoop(boolean[] faceVisited, TriangleMesh mesh,
        double cutHeight, Map<Integer, Set<Integer>> facesWithVertices)
    {
        PolygonIndices cutFaceIndices = new PolygonIndices();
        int previousFaceIndex = -1;
        int faceIndex = getFirstUnvisitedIntersectingFace(faceVisited, mesh, cutHeight);
        if (faceIndex != -1)
        {
            while (true)
            {
//                System.out.println("treat face B " + faceIndex);
                Set<Integer> edges = getEdgeIndicesOfFaceThatPlaneIntersects(mesh, faceIndex,
                                                                             cutHeight);
                if (edges.size() != 0)
                {
                    faceVisited[faceIndex] = true;
                    cutFaceIndices.add(faceIndex);

                    // there should be two faces adjacent to this one that the plane also cuts
                    Set<Integer> facesAdjacentToEdgesOfFace
                        = getFacesAdjacentToEdgesOfFace(mesh, faceIndex, facesWithVertices, edges);
//                    System.out.println("adjacent faces is " + facesAdjacentToEdgesOfFace);

                    // remove the previously visited face leaving the next face to visit
                    if (previousFaceIndex != -1)
                    {
                        facesAdjacentToEdgesOfFace.remove(previousFaceIndex);
                    }
                    previousFaceIndex = faceIndex;
                    faceIndex = facesAdjacentToEdgesOfFace.iterator().next();

                    if (faceVisited[faceIndex])
                    {
                        // we've completed the loop back to the first intersecting face
                        break;
                    }
                }
            }
        }
        return cutFaceIndices;
    }

    private static int getFirstUnvisitedIntersectingFace(boolean[] faceVisited, TriangleMesh mesh,
        double cutHeight)
    {
        int faceIndex = findFirstUnvisitedFace(faceVisited);
        if (faceIndex != -1)
        {
            while (getEdgeIndicesOfFaceThatPlaneIntersects(mesh, faceIndex, cutHeight).size() == 0)
            {
                faceVisited[faceIndex] = true;
                faceIndex = findFirstUnvisitedFace(faceVisited);
                if (faceIndex == -1)
                {
                    break;
                }
            }
        }
        return faceIndex;
    }

    private static Set<Integer> getFacesAdjacentToEdgesOfFace(TriangleMesh mesh, int faceIndex,
        Map<Integer, Set<Integer>> facesWithVertices, Set<Integer> edges)
    {
        Set<Integer> faces = new HashSet<>();
        for (Integer edge : edges)
        {
            switch (edge)
            {
                case 1:
                    faces.add(getFaceAdjacentToVertices(mesh, facesWithVertices, faceIndex, 0, 1));
                    break;
                case 2:
                    faces.add(getFaceAdjacentToVertices(mesh, facesWithVertices, faceIndex, 1, 2));
                    break;
                case 3:
                    faces.add(getFaceAdjacentToVertices(mesh, facesWithVertices, faceIndex, 0, 2));
                    break;
            }
        }
        return faces;
    }

    private static int getFaceAdjacentToVertices(TriangleMesh mesh,
        Map<Integer, Set<Integer>> facesWithVertices,
        int faceIndex, int vertexIndexOffset0, int vertexIndexOffset1)
    {
        Set<Integer> facesWithVertex0 = new HashSet(facesWithVertices.get(
            mesh.getFaces().get(faceIndex * 6 + vertexIndexOffset0 * 2)));

        Set<Integer> facesWithVertex1 = facesWithVertices.get(
            mesh.getFaces().get(faceIndex * 6 + vertexIndexOffset1 * 2));
        facesWithVertex0.remove(faceIndex);
        facesWithVertex0.retainAll(facesWithVertex1);
        assert (facesWithVertex0.size() == 1);
        return facesWithVertex0.iterator().next();
    }

    /**
     * Return the two edge indices that the plane intersects. V0 -> V1 is called edge 1, V1 -> V2 is
     * edge 2 and V0 -> V2 is edge 3.
     */
    private static Set<Integer> getEdgeIndicesOfFaceThatPlaneIntersects(TriangleMesh mesh,
        int faceIndex,
        double cutHeight)
    {
        Set<Integer> edges = new HashSet<>();
        int vertex0 = mesh.getFaces().get(faceIndex * 6);
        int vertex1 = mesh.getFaces().get(faceIndex * 6 + 2);
        int vertex2 = mesh.getFaces().get(faceIndex * 6 + 4);

        if (lineIntersectsPlane(mesh, vertex0, vertex1, cutHeight))
        {
            edges.add(1);
        }
        if (lineIntersectsPlane(mesh, vertex1, vertex2, cutHeight))
        {
            edges.add(2);
        }
        if (lineIntersectsPlane(mesh, vertex0, vertex2, cutHeight))
        {
            edges.add(3);
        }
        return edges;
    }

    /**
     * Return the two edge indices that the plane intersects. V0 -> V1 is called edge 1, V1 -> V2 is
     * edge 2 and V0 -> V2 is edge 3.
     */
    private static Set<Edge> getEdgesOfFaceThatPlaneIntersects(TriangleMesh mesh, int faceIndex,
        double cutHeight)
    {
        Set<Edge> edges = new HashSet<>();
        int vertex0 = mesh.getFaces().get(faceIndex * 6);
        int vertex1 = mesh.getFaces().get(faceIndex * 6 + 2);
        int vertex2 = mesh.getFaces().get(faceIndex * 6 + 4);

        if (lineIntersectsPlane(mesh, vertex0, vertex1, cutHeight))
        {
            edges.add(new Edge(vertex0, vertex1));
        }
        if (lineIntersectsPlane(mesh, vertex1, vertex2, cutHeight))
        {
            edges.add(new Edge(vertex1, vertex2));
        }
        if (lineIntersectsPlane(mesh, vertex0, vertex2, cutHeight))
        {
            edges.add(new Edge(vertex0, vertex2));
        }
        return edges;
    }

    private static boolean lineIntersectsPlane(TriangleMesh mesh, int vertex0, int vertex1,
        double cutHeight)
    {
        double y0 = mesh.getPoints().get(vertex0 * 3 + 1);
        double y1 = mesh.getPoints().get(vertex1 * 3 + 1);
        if (((y0 <= cutHeight) && (cutHeight <= y1))
            || ((y1 <= cutHeight) && (cutHeight <= y0)))
        {
            return true;
        }
        return false;
    }

    private static int findFirstUnvisitedFace(boolean[] faceVisited)
    {
        for (int i = 0; i < faceVisited.length; i++)
        {
            if (!faceVisited[i])
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Take the given mesh and vertices of the open face, close the face and add the new face to the
     * mesh and return it.
     */
    private static TriangleMesh closeOpenFace(CutResult cutResult, double cutHeight)
    {
        TriangleMesh mesh = cutResult.childMesh;
        Set<LoopSet> loopSets = cutResult.identifyOuterLoopsAndInnerLoops();
        for (LoopSet loopSet : loopSets)
        {
            try
            {
                List<Integer> vertices = loopSet.outerLoop;
                Polygon outerPolygon = makePolygon(vertices, mesh);
                for (List<Integer> innerVertices : loopSet.innerLoops)
                {
                    Polygon innerPolygon = makePolygon(innerVertices, mesh);
                    outerPolygon.addHole(innerPolygon);
                }

                Poly2Tri.triangulate(outerPolygon);
                addTriangulatedFacesToMesh(mesh, outerPolygon, vertices, cutHeight);
            } catch (Exception ex)
            {
                System.out.println("unable to close loop: " + loopSet);
            }
        }

        return mesh;
    }

    private static Polygon makePolygon(List<Integer> vertices, TriangleMesh mesh)
    {
        List<PolygonPoint> points = new ArrayList<>();
        for (Integer vertexIndex : vertices)
        {
            if (mesh.getPoints().get(vertexIndex * 3) > 1e5 || mesh.getPoints().get(vertexIndex * 3)
                < -1e5
                || mesh.getPoints().get(vertexIndex * 3 + 2) > 1e5 || mesh.getPoints().get(
                    vertexIndex * 3 + 2) < -1e5)
            {
                throw new RuntimeException("invalid point calculated");
            }
            points.add(new PolygonPoint(
                mesh.getPoints().get(vertexIndex * 3),
                mesh.getPoints().get(vertexIndex * 3 + 2)));
        }
        Polygon outerPolygon = new Polygon(points);
        return outerPolygon;
    }

    /**
     * For each triangle in the polygon add a face to the mesh. If any point in any triangle is not
     * one of the outerVertices then also add that point to the mesh.
     */
    private static void addTriangulatedFacesToMesh(TriangleMesh mesh, Polygon outerPolygon,
        List<Integer> outerVertices, double cutHeight)
    {
        Map<Vertex, Vertex> vertexToVertex = new HashMap<>();
        for (Integer vertexIndex : outerVertices)
        {
            Vertex vertex = new Vertex(vertexIndex, mesh.getPoints().get(vertexIndex * 3),
                                       mesh.getPoints().get(vertexIndex * 3 + 1),
                                       mesh.getPoints().get(vertexIndex * 3 + 2));
            vertexToVertex.put(vertex, vertex);
        }

        for (DelaunayTriangle triangle : outerPolygon.getTriangles())
        {
            TriangulationPoint[] points = triangle.points;
            Vertex vertex0 = getOrMakeVertexForPoint(mesh, points[0], vertexToVertex, cutHeight);
            Vertex vertex1 = getOrMakeVertexForPoint(mesh, points[1], vertexToVertex, cutHeight);
            Vertex vertex2 = getOrMakeVertexForPoint(mesh, points[2], vertexToVertex, cutHeight);
//            System.out.println(vertex0 + " " + vertex1 + " " + vertex2);
            makeFace(mesh, vertex0.meshVertexIndex, vertex1.meshVertexIndex, vertex2.meshVertexIndex);
        }
    }

    private static void makeFace(TriangleMesh mesh, int meshVertexIndex0, int meshVertexIndex1,
        int meshVertexIndex2)
    {
        int[] vertices = new int[6];
        vertices[0] = meshVertexIndex0;
        vertices[2] = meshVertexIndex1;
        vertices[4] = meshVertexIndex2;
        mesh.getFaces().addAll(vertices);
    }

    private static Vertex getOrMakeVertexForPoint(TriangleMesh mesh, TriangulationPoint point,
        Map<Vertex, Vertex> vertexToVertex, double cutHeight)
    {
        Vertex vertex = new Vertex((float) point.getX(), (float) cutHeight, (float) point.getY());
        if (!vertexToVertex.containsKey(vertex))
        {
            mesh.getPoints().addAll(point.getXf(), (float) cutHeight, point.getYf());
            int vertexIndex = mesh.getPoints().size() / 3 - 1;
            vertex.meshVertexIndex = vertexIndex;
            vertexToVertex.put(vertex, vertex);
            return vertex;
        } else
        {
            return vertexToVertex.get(vertex);
        }

    }

    public static void setDebuggingNode(ModelContainer node)
    {
        MeshCutter.node = node;
    }

    private static ModelContainer node;

    private static void showNewVertices(List<Integer> newVertices, TriangleMesh mesh)
    {
        if (node != null)
        {
            for (Integer newVertex : newVertices)
            {
                Sphere sphere = new Sphere(0.5);
                sphere.translateXProperty().set(mesh.getPoints().get(newVertex * 3));
                sphere.translateYProperty().set(mesh.getPoints().get(newVertex * 3 + 1));
                sphere.translateZProperty().set(mesh.getPoints().get(newVertex * 3 + 2));
                sphere.setMaterial(ApplicationMaterials.getOffBedModelMaterial());
                node.addChildNode(sphere);
            }
        }
    }

    private static void showSphere(double x, double y, double z)
    {
        Sphere sphere = new Sphere(0.5);
        sphere.translateXProperty().set(x);
        sphere.translateYProperty().set(y);
        sphere.translateZProperty().set(z);
        sphere.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
        if (node != null)
        {
            node.addChildNode(sphere);
        }
    }

    private static void showFace(TriangleMesh mesh, int faceIndex)
    {
        TriangleMesh triangle = new TriangleMesh();

        int[] vertices = new int[6];
        vertices[0] = mesh.getFaces().get(faceIndex * 6);
        vertices[2] = mesh.getFaces().get(faceIndex * 6 + 2);
        vertices[4] = mesh.getFaces().get(faceIndex * 6 + 4);
        triangle.getFaces().addAll(vertices);

        addPointToMesh(mesh, vertices[0], triangle);
        addPointToMesh(mesh, vertices[2], triangle);
        addPointToMesh(mesh, vertices[4], triangle);

        setTextureAndSmoothing(triangle, triangle.getFaces().size() / 6);

        MeshView meshView = new MeshView(triangle);
        meshView.setMaterial(ApplicationMaterials.pickedGCodeMaterial);
        if (node != null)
        {
            node.addChildNode(meshView);
        }
    }

    private static void showFaceCentres(List<Integer> cutFaces, TriangleMesh mesh)
    {
        for (Integer faceIndex : cutFaces)
        {
            int v0 = mesh.getFaces().get(faceIndex * 6);
            int v1 = mesh.getFaces().get(faceIndex * 6 + 2);
            int v2 = mesh.getFaces().get(faceIndex * 6 + 4);

            double x0 = mesh.getPoints().get(v0 * 3);
            double y0 = mesh.getPoints().get(v0 * 3 + 1);
            double z0 = mesh.getPoints().get(v0 * 3 + 2);

            double x1 = mesh.getPoints().get(v1 * 3);
            double y1 = mesh.getPoints().get(v1 * 3 + 1);
            double z1 = mesh.getPoints().get(v1 * 3 + 2);

            double x2 = mesh.getPoints().get(v2 * 3);
            double y2 = mesh.getPoints().get(v2 * 3 + 1);
            double z2 = mesh.getPoints().get(v2 * 3 + 2);

            double xMin = Math.min(x0, Math.min(x1, x2));
            double xMax = Math.max(x0, Math.max(x1, x2));
            double x = (xMin + xMax) / 2;

            double yMin = Math.min(y0, Math.min(y1, y2));
            double yMax = Math.max(y0, Math.max(y1, y2));
            double y = (yMin + yMax) / 2;

            double zMin = Math.min(z0, Math.min(z1, z2));
            double zMax = Math.max(z0, Math.max(z1, z2));
            double z = (zMin + zMax) / 2;

            Sphere sphere = new Sphere(0.5);
            sphere.translateXProperty().set((x0 + x1 + x2) / 3d);
            sphere.translateYProperty().set((y0 + y1 + y2) / 3d);
            sphere.translateZProperty().set((z0 + z1 + z2) / 3d);
//            System.out.println("add face centre " + (x0 + x1 + x2) / 3d + " " + (y0 + y1 + y2) / 3d
//                + " " + (z0 + z1 + z2) / 3d);
            sphere.setMaterial(ApplicationMaterials.getDefaultModelMaterial());

            Text text = new Text(Integer.toString(faceIndex));
            text.translateXProperty().set((x0 + x1 + x2) / 3d);
            text.translateYProperty().set((y0 + y1 + y2) / 3d);
            text.translateZProperty().set((z0 + z1 + z2) / 3d);
            Font font = new Font("Source Sans Pro Regular", 8);
            text.setFont(font);

            if (node != null)
            {
                node.addChildNode(sphere);
                node.addChildNode(text);
            }
        }
    }

    private static void showIncomingMesh(TriangleMesh mesh)
    {
        System.out.println(mesh.getVertexFormat());
        System.out.println(mesh.getVertexFormat().getVertexIndexSize());
        System.out.println(mesh.getVertexFormat().getPointIndexOffset());

        for (int i = 0; i < mesh.getPoints().size() / 3; i++)
        {
            System.out.println("point " + i + " is " + mesh.getPoints().get(i * 3) + " "
                + mesh.getPoints().get(i * 3 + 1) + " " + mesh.getPoints().get(i * 3 + 2));

            showSphere(mesh.getPoints().get(i * 3),
                       mesh.getPoints().get(i * 3 + 1),
                       mesh.getPoints().get(i * 3 + 2));
        }

        for (int i = 0; i < mesh.getFaces().size() / 6; i++)
        {
            System.out.println("face " + i + " is " + mesh.getFaces().get(i * 6) + " "
                + mesh.getFaces().get(i * 6 + 2) + " " + mesh.getFaces().get(i * 6 + 4));
        }
    }
}


class PolygonIndices extends ArrayList<Integer>
{

}


final class Edge
{

    final int v0;
    final int v1;

    public Edge(int v0, int v1)
    {
        this.v0 = v0;
        this.v1 = v1;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Edge))
        {
            return false;
        }
        if (obj == this)
        {
            return true;
        }

        Edge other = (Edge) obj;
        if ((other.v0 == v0 && other.v1 == v1) || (other.v1 == v0 && other.v0 == v1))
        {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return v0 + v1;
    }
}

// The main purpose of this Vertex class is to provide an equality operation.

final class Vertex
{

    int meshVertexIndex;
    final float x;
    final float y;
    final float z;

    public Vertex(int meshVertexIndex, float x, float y, float z)
    {
        this.meshVertexIndex = meshVertexIndex;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vertex(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString()
    {
        return "Vertex{" + "meshVertexIndex=" + meshVertexIndex + ", x=" + x + ", y=" + y + ", z="
            + z + '}';
    }

    static boolean equalto6places(double a, double b)
    {
        return Math.round(a * 10e6) == Math.round(b * 10e6);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Vertex))
        {
            return false;
        }
        if (obj == this)
        {
            return true;
        }

        Vertex other = (Vertex) obj;
        if (!equalto6places(other.x, x))
        {
            return false;
        }
        if (!equalto6places(other.y, y))
        {
            return false;
        }
        if (!equalto6places(other.z, z))
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        return (int) (Math.round(x * 10e6) + Math.round(y * 10e6) + Math.round(z * 10e6));
    }
}


class LoopSet
{

    final List<Integer> outerLoop;
    final List<PolygonIndices> innerLoops;

    public LoopSet(List<Integer> outerLoop, List<PolygonIndices> innerLoops)
    {
        this.outerLoop = outerLoop;
        this.innerLoops = innerLoops;
    }

}


class Point
{

    final double x;
    final double y;

    public Point(double x, double y)
    {
        this.x = x;
        this.y = y;
    }
}
