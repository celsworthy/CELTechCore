/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import static celtech.utils.threed.MeshSeparator.setTextureAndSmoothing;
import static celtech.utils.threed.MeshSeparator.makeFacesWithVertex;
import static celtech.utils.threed.OpenFaceCloser.closeOpenFace;
import com.sun.javafx.scene.shape.ObservableFaceArrayImpl;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javafx.geometry.Point3D;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;


/**
 * MeshCutter cuts a mesh along a given horizontal plane, triangulates the two resultant open faces,
 * and creates two child meshes.
 *
 * @author tony
 */
public class MeshCutter
{

    enum TopBottom
    {

        TOP, BOTTOM
    };


    public interface BedToLocalConverter
    {

        Point3D localToBed(Point3D point);

        Point3D bedToLocal(Point3D point);
    }


    public static class MeshPair
    {

        final TriangleMesh topMesh;
        final TriangleMesh bottomMesh;

        public MeshPair(TriangleMesh topMesh, TriangleMesh bottomMesh)
        {
            this.topMesh = topMesh;
            this.bottomMesh = bottomMesh;
        }

        public List<TriangleMesh> getMeshes()
        {
            List<TriangleMesh> meshes = new ArrayList<>();
            meshes.add(topMesh);
            meshes.add(bottomMesh);
            return meshes;
        }
    }

    /**
     * Cut the given mesh into two, at the given height.
     */
    public static MeshPair cut(TriangleMesh mesh, float cutHeight,
        BedToLocalConverter bedToLocalConverter)
    {

//        perturbVerticesOnCut(mesh, cutHeight, bedToLocalConverter);
        CutResult cutResult = getUncoveredUpperMesh(mesh, cutHeight, bedToLocalConverter);

        TriangleMesh topMesh = closeOpenFace(cutResult, cutHeight, bedToLocalConverter);
        MeshUtils.removeUnusedAndDuplicateVertices(topMesh);
        setTextureAndSmoothing(topMesh, topMesh.getFaces().size() / 6);

        Optional<MeshUtils.MeshError> error = MeshUtils.validate(topMesh);
        if (error.isPresent())
        {
            throw new RuntimeException("Invalid mesh: " + error.toString());
        }

        cutResult = getUncoveredLowerMesh(mesh, cutHeight, bedToLocalConverter);
        TriangleMesh bottomMesh = closeOpenFace(cutResult, cutHeight, bedToLocalConverter);
        MeshUtils.removeUnusedAndDuplicateVertices(bottomMesh);
        setTextureAndSmoothing(bottomMesh, bottomMesh.getFaces().size() / 6);

        error = MeshUtils.validate(bottomMesh);
        if (error.isPresent())
        {
            throw new RuntimeException("Invalid mesh: " + error.toString());
        }

        return new MeshPair(topMesh, bottomMesh);
    }


    /**
     * IntersectedFace captures details about a face that intersects the cutting plane.
     */
    static class IntersectedFace
    {

        /**
         * The faceIndex of the face that intersects (not just touches) the cutting plane.
         */
        final int faceIndex;
        /**
         * The vertices where the face crosses the cutting plane, there should be two.
         */
        final List<Integer> vertexIndices;

        public IntersectedFace(int faceIndex, List<Integer> vertexIndices)
        {
            this.faceIndex = faceIndex;
            this.vertexIndices = vertexIndices;
        }

        @Override
        public String toString()
        {
            return "IntersectedFace{" + "faceIndex=" + faceIndex + ", vertexIndices="
                + vertexIndices + '}';
        }

    }


    /**
     * For each loop of faces/vertices that is intersected by the cut, one LoopOfVerticesAndCutFaces
     * is instantiated. cutFaces are those faces that are actually intersected and is later used to
     * create the new smaller faces around the cut. loopOfVertices is the loop of vertices composing
     * this loop and is used to create the covering surface as either an outer perimeter or a hole.
     */
    static class LoopOfVerticesAndCutFaces
    {

        /**
         * The faces that actually cross (not just touch) the cutting plane. It will be necessary to
         * cut these faces in two and to create new faces on either side of the cutting plane.
         */
        final Set<IntersectedFace> cutFaces;
        /**
         * All the vertices on the cutting plane that make this loop, in sequence. These are used as
         * the perimeter to create the top surface where the mesh was cut.
         */
        final PolygonIndices loopOfVertices;

        public LoopOfVerticesAndCutFaces()
        {
            this.cutFaces = new HashSet<>();
            this.loopOfVertices = new PolygonIndices();
        }

        public LoopOfVerticesAndCutFaces(Set<IntersectedFace> cutFaces,
            PolygonIndices loopOfVertices)
        {
            this.cutFaces = cutFaces;
            this.loopOfVertices = loopOfVertices;
        }
    }

    private static CutResult getUncoveredUpperMesh(TriangleMesh mesh,
        float cutHeight, BedToLocalConverter bedToLocalConverter)
    {
        Set<LoopOfVerticesAndCutFaces> cutFaces = getLoopsOfVertices(mesh, cutHeight,
                                                                     bedToLocalConverter);

        TriangleMesh upperMesh = makeSplitMesh(mesh, cutFaces,
                                               cutHeight, bedToLocalConverter, TopBottom.TOP);
        CutResult cutResultUpper = new CutResult(upperMesh, cutFaces,
                                                 bedToLocalConverter, TopBottom.TOP);
        return cutResultUpper;
    }

    private static CutResult getUncoveredLowerMesh(TriangleMesh mesh,
        float cutHeight, BedToLocalConverter bedToLocalConverter)
    {
        Set<LoopOfVerticesAndCutFaces> cutFaces = getLoopsOfVertices(mesh, cutHeight,
                                                                     bedToLocalConverter);

        TriangleMesh lowerMesh = makeSplitMesh(mesh, cutFaces,
                                               cutHeight, bedToLocalConverter, TopBottom.BOTTOM);
        CutResult cutResultLower = new CutResult(lowerMesh, cutFaces,
                                                 bedToLocalConverter, TopBottom.BOTTOM);

        return cutResultLower;
    }

    /**
     * Given the mesh, cut faces and intersection points, create the lower child mesh. Copy the
     * original mesh, remove all the cut faces and replace with a new set of faces using the new
     * intersection points. Remove all the faces from above the cut faces.
     */
    private static TriangleMesh makeSplitMesh(TriangleMesh mesh,
        Set<LoopOfVerticesAndCutFaces> cutFaces,
        float cutHeight, BedToLocalConverter bedToLocalConverter, TopBottom topBottom)
    {
        TriangleMesh childMesh = copyMesh(mesh);

        removeCutFacesAndFacesAboveCutPlane(childMesh, cutFaces, cutHeight,
                                            bedToLocalConverter, topBottom);

        for (LoopOfVerticesAndCutFaces cutFace : cutFaces)
        {
            addLowerFacesAroundCut(mesh, childMesh, cutFace, cutHeight,
                                   bedToLocalConverter, topBottom);
        }

        MeshDebug.showFace(mesh, 0);

        return childMesh;
    }

    private static TriangleMesh copyMesh(TriangleMesh mesh)
    {
        TriangleMesh childMesh = new TriangleMesh();
        childMesh.getPoints().addAll(mesh.getPoints());
        childMesh.getFaces().addAll(mesh.getFaces());
        setTextureAndSmoothing(childMesh, childMesh.getFaces().size() / 6);
        return childMesh;
    }

    private static void addLowerFacesAroundCut(TriangleMesh mesh, TriangleMesh childMesh,
        LoopOfVerticesAndCutFaces cutFaces, float cutHeight,
        BedToLocalConverter bedToLocalConverter, TopBottom topBottom)
    {

        for (IntersectedFace cutFace : cutFaces.cutFaces)
        {
            System.out.println("add new faces for cutFace " + cutFace);
            addLowerDividedFaceToChild(mesh, childMesh, cutFace.faceIndex,
                                       cutFace.vertexIndices.get(0),
                                       cutFace.vertexIndices.get(1),
                                       cutHeight, bedToLocalConverter, topBottom);
        }
        setTextureAndSmoothing(childMesh, childMesh.getFaces().size() / 6);
    }

    static Vertex getVertex(TriangleMesh mesh, int vertexIndex)
    {
        float x = mesh.getPoints().get(vertexIndex * 3);
        float y = mesh.getPoints().get(vertexIndex * 3 + 1);
        float z = mesh.getPoints().get(vertexIndex * 3 + 2);
        return new Vertex(x, y, z);
    }

    static Point3D makePoint3D(TriangleMesh mesh, int vertexIndex)
    {
        float x = mesh.getPoints().get(vertexIndex * 3);
        float y = mesh.getPoints().get(vertexIndex * 3 + 1);
        float z = mesh.getPoints().get(vertexIndex * 3 + 2);
        return new Point3D(x, y, z);
    }

    /**
     * Cut the face using the given vertices, and add the lower face(s) to the child mesh.
     */
    private static void addLowerDividedFaceToChild(TriangleMesh mesh, TriangleMesh childMesh,
        int faceIndex, int vertexIntersect0, int vertexIntersect1, float cutHeight,
        BedToLocalConverter bedToLocalConverter, TopBottom topBottom)
    {

        int v0 = mesh.getFaces().get(faceIndex * 6);
        int v1 = mesh.getFaces().get(faceIndex * 6 + 2);
        int v2 = mesh.getFaces().get(faceIndex * 6 + 4);

        float v0Height = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, v0)).getY();
        float v1Height = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, v1)).getY();
        float v2Height = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, v2)).getY();

        // are points below/above cut?
        boolean v0belowCut;
        boolean v1belowCut;
        boolean v2belowCut;

        if (topBottom == TopBottom.BOTTOM)
        {
            v0belowCut = v0Height > cutHeight;
            v1belowCut = v1Height > cutHeight;
            v2belowCut = v2Height > cutHeight;
        } else
        {
            v0belowCut = v0Height < cutHeight;
            v1belowCut = v1Height < cutHeight;
            v2belowCut = v2Height < cutHeight;
        }

        boolean b01 = lineIntersectsPlane(mesh, v0, v1, cutHeight, bedToLocalConverter);
        boolean b12 = lineIntersectsPlane(mesh, v1, v2, cutHeight, bedToLocalConverter);
        boolean b02 = lineIntersectsPlane(mesh, v0, v2, cutHeight, bedToLocalConverter);

        // first check for special case where one vertex of face is on cutting plane
        Set<Integer> vertexIndices = getFaceVerticesIntersectingPlane(
            mesh, faceIndex, cutHeight, bedToLocalConverter);

        if (vertexIndices.size() == 1)
        {

            int vertexIndexOnPlane = vertexIndices.iterator().next();
            int vertexIndexCutting;
            if (vertexIndexOnPlane == vertexIntersect0)
            {
                vertexIndexCutting = vertexIntersect1;
            } else
            {
                assert vertexIndexOnPlane == vertexIntersect1;
                vertexIndexCutting = vertexIntersect0;
            }

            int c0 = -1;
            int c1 = -1;
            int c2 = -1;
            if (v0belowCut)
            {
                if (vertexIndexOnPlane == v1)
                {
                    c0 = v0;
                    c1 = vertexIndexOnPlane;
                    c2 = vertexIndexCutting;
                    System.out.println("A add new face " + c0 + " " + c1 + " " + c2);
                } else
                {
                    c0 = v0;
                    c1 = vertexIndexCutting;
                    c2 = vertexIndexOnPlane;
                    System.out.println("B add new face " + c0 + " " + c1 + " " + c2);
                }
            } else if (v1belowCut)
            {
                if (vertexIndexOnPlane == v0)
                {
                    c0 = vertexIndexOnPlane;
                    c1 = v1;
                    c2 = vertexIndexCutting;
                    System.out.println("C add new face " + c0 + " " + c1 + " " + c2);
                } else
                {
                    c0 = vertexIndexCutting;
                    c1 = v1;
                    c2 = vertexIndexOnPlane;
                    System.out.println("D add new face " + c0 + " " + c1 + " " + c2);
                }
            } else if (v2belowCut)
            {
                if (vertexIndexOnPlane == v0)
                {
                    c0 = vertexIndexOnPlane;
                    c1 = vertexIndexCutting;
                    c2 = v2;
                    System.out.println("E add new face " + c0 + " " + c1 + " " + c2);
                } else
                {
                    c0 = vertexIndexCutting;
                    c1 = vertexIndexOnPlane;
                    c2 = v2;
                    System.out.println("F add new face " + c0 + " " + c1 + " " + c2);
                }
            }
            int[] vertices = new int[6];
            System.out.println("add new face " + c0 + " " + c1 + " " + c2);
            vertices[0] = c0;
            vertices[2] = c1;
            vertices[4] = c2;
            childMesh.getFaces().addAll(vertices);
            return;
        } else
        {
            assert vertexIndices.isEmpty();
        }

        // indices of intersecting vertices between v0->v1 etc
        int v01 = -1;
        int v12 = -1;
        int v02 = -1;

        // get vertex index for intersections v01, v12, v02
        if (b01)
        {
            Vertex vertex01 = getIntersectingVertex(new Edge(v0, v1), mesh, cutHeight,
                                                    bedToLocalConverter);
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
            Vertex vertex12 = getIntersectingVertex(new Edge(v1, v2), mesh, cutHeight,
                                                    bedToLocalConverter);
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
            Vertex vertex20 = getIntersectingVertex(new Edge(v0, v2), mesh, cutHeight,
                                                    bedToLocalConverter);
            if (vertex20.equals(getVertex(mesh, vertexIntersect0)))
            {
                v02 = vertexIntersect0;
            } else
            {
                assert (vertex20.equals(getVertex(mesh, vertexIntersect1)));
                v02 = vertexIntersect1;
            }
        }

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
            if (c0 != c1 && c1 != c2 && c2 != c0)
            {

                int[] vertices = new int[6];
                vertices[0] = c0;
                vertices[2] = c1;
                vertices[4] = c2;
                childMesh.getFaces().addAll(vertices);
            }

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
            vertices[0] = c3;
            vertices[2] = c4;
            vertices[4] = c5;
            childMesh.getFaces().addAll(vertices);
        }

    }

    /**
     * Remove the cut faces and any other faces above cut height from the mesh.
     */
    private static void removeCutFacesAndFacesAboveCutPlane(TriangleMesh mesh,
        Set<LoopOfVerticesAndCutFaces> cutFaces, float cutHeight,
        BedToLocalConverter bedToLocalConverter,
        TopBottom topBottom)
    {
        Set<Integer> facesAboveBelowCut = new HashSet<>();

        // compare vertices' -Y to cutHeight
        for (int faceIndex = 0; faceIndex < mesh.getFaces().size() / 6; faceIndex++)
        {
            int vertex0 = mesh.getFaces().get(faceIndex * 6);
            float vertex0YInBed = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, vertex0)).getY();

            // for BOTTOM we want vY is "above" the cut
            // if a vertex is on the line then ignore it, one of the other vertices will be
            // above or below the line.
            if (topBottom == TopBottom.BOTTOM && vertex0YInBed < cutHeight || topBottom
                == TopBottom.TOP && vertex0YInBed > cutHeight)
            {
                facesAboveBelowCut.add(faceIndex);
                continue;
            }
            int vertex1 = mesh.getFaces().get(faceIndex * 6 + 2);
            float vertex1YInBed = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, vertex1)).getY();
            if (topBottom == TopBottom.BOTTOM && vertex1YInBed < cutHeight || topBottom
                == TopBottom.TOP && vertex1YInBed > cutHeight)
            {
                facesAboveBelowCut.add(faceIndex);
                continue;
            }
            int vertex2 = mesh.getFaces().get(faceIndex * 6 + 4);
            float vertex2YInBed = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, vertex2)).getY();
            if (topBottom == TopBottom.BOTTOM && vertex2YInBed < cutHeight || topBottom
                == TopBottom.TOP && vertex2YInBed > cutHeight)
            {
                facesAboveBelowCut.add(faceIndex);
                continue;
            }
        }

        ObservableFaceArray newFaceArray = new ObservableFaceArrayImpl();
        for (int faceIndex = 0; faceIndex < mesh.getFaces().size() / 6; faceIndex++)
        {
            if (facesAboveBelowCut.contains(faceIndex))
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
     * Return the edge that is shared between the two faces.
     */
//    private static Edge getCommonEdge(TriangleMesh mesh, int faceIndex0, int faceIndex1)
//    {
//        Set<Edge> edges0 = getFaceEdges(mesh, faceIndex0);
//        Set<Edge> edges1 = getFaceEdges(mesh, faceIndex1);
//        edges0.retainAll(edges1);
//        assert (edges0.size() == 1);
//        return edges0.iterator().next();
//    }
    static Set<Edge> getFaceEdges(TriangleMesh mesh, int faceIndex)
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
    private static Integer makeIntersectingVertex(TriangleMesh mesh, Edge edge, float cutHeight,
        BedToLocalConverter bedToLocalConverter)
    {
        Vertex vertex = getIntersectingVertex(edge, mesh, cutHeight, bedToLocalConverter);
        int vertexIndex = addNewOrGetVertex(mesh, vertex);
        return vertexIndex;
    }

    private static Vertex getIntersectingVertex(Edge edge, TriangleMesh mesh, float cutHeight,
        BedToLocalConverter bedToLocalConverter)
    {
        int v0 = edge.v0;
        int v1 = edge.v1;

        Point3D p0Bed = bedToLocalConverter.localToBed(makePoint3D(mesh, v0));
        Point3D p1Bed = bedToLocalConverter.localToBed(makePoint3D(mesh, v1));

        double v0X = p0Bed.getX();
        double v1X = p1Bed.getX();
        double v0Y = p0Bed.getY();
        double v1Y = p1Bed.getY();
        double v0Z = p0Bed.getZ();
        double v1Z = p1Bed.getZ();
        double proportionAlongEdge;
        if (Math.abs(v1Y - v0Y) < 1e-7)
        {
            assert false;
            proportionAlongEdge = 0;
        } else
        {
            proportionAlongEdge = (cutHeight - v0Y) / (v1Y - v0Y);
        }
//        System.out.println("proportion is " + proportionAlongEdge);
        float interX = (float) (v0X + (v1X - v0X) * proportionAlongEdge);
        float interZ = (float) (v0Z + (v1Z - v0Z) * proportionAlongEdge);

        Point3D intersectingPointInBed = new Point3D(interX, (float) cutHeight, interZ);
        Point3D intersectingPoint = bedToLocalConverter.bedToLocal(intersectingPointInBed);

        Vertex vertex = new Vertex((float) intersectingPoint.getX(),
                                   (float) intersectingPoint.getY(),
                                   (float) intersectingPoint.getZ());
        return vertex;
    }

    /**
     * Get all loops of faces and their matching vertices along the cutting plane. Each loop of
     * vertices must be ordered according to adjacency, so that we can get a correct perimeter for
     * forming the cover surface. Any new vertices that are required must be added to the mesh.
     */
    static Set<LoopOfVerticesAndCutFaces> getLoopsOfVertices(TriangleMesh mesh, float cutHeight,
        BedToLocalConverter bedToLocalConverter)
    {
        boolean[] faceVisited = new boolean[mesh.getFaces().size() / 6];
        boolean[] perimeterVertex = new boolean[mesh.getPoints().size()];
        Map<Integer, Set<Integer>> facesWithVertices = makeFacesWithVertex(mesh);

        Set<LoopOfVerticesAndCutFaces> loopsOfFaces = new HashSet<>();

        while (true)
        {
            Optional<LoopOfVerticesAndCutFaces> loopOfFaces
                = getNextLoopOfVertices(faceVisited, perimeterVertex, mesh, cutHeight,
                                        facesWithVertices, bedToLocalConverter);
            if (loopOfFaces.isPresent() && loopOfFaces.get().loopOfVertices.size() > 2)
            {
                loopsOfFaces.add(loopOfFaces.get());
            } else
            {
                if (!loopOfFaces.isPresent())
                {
                    break;
                }
            }
        }

        return loopsOfFaces;
    }


    static class Intersection
    {

        final int faceIndex;
        final Optional<Edge> edge;
        final int vertexIndex;

        public Intersection(int faceIndex, Optional<Edge> edge, int vertexIndex)
        {
            this.faceIndex = faceIndex;
            this.edge = edge;
            this.vertexIndex = vertexIndex;
        }

        @Override
        public String toString()
        {
            String edgeString;
            if (edge.isPresent())
            {
                edgeString = edge.get().toString();
            } else
            {
                edgeString = "Corner";
            }

            return "Intersection{" + "faceIndex=" + faceIndex + ", edge=" + edgeString
                + ", vertexIndex=" + vertexIndex + '}';
        }

        @Override
        public int hashCode()
        {
            return faceIndex + vertexIndex * 101;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final Intersection other = (Intersection) obj;
            if (this.faceIndex != other.faceIndex)
            {
                return false;
            }

            if (!this.edge.isPresent() && other.edge.isPresent())
            {
                return false;
            }

            if (this.edge.isPresent() && !other.edge.isPresent())
            {
                return false;
            }

            if (this.edge.isPresent() && other.edge.isPresent() && (!Objects.equals(this.edge.get(),
                                                                                    other.edge.get())))
            {
                return false;
            }
            if (this.vertexIndex != other.vertexIndex)
            {
                return false;
            }
            return true;
        }
    }

    static Set<Intersection> getFaceIntersections(final int faceIndex, TriangleMesh mesh,
        float cutHeight, BedToLocalConverter bedToLocalConverter)
    {

        Set<Intersection> intersections = new HashSet<>();

        Set<Edge> edges = getEdgesOfFaceThatPlaneIntersects(mesh, faceIndex, cutHeight,
                                                            bedToLocalConverter);
        for (Edge edge : edges)
        {
            int vertexIndex = makeIntersectingVertex(mesh, edge, cutHeight,
                                                     bedToLocalConverter);

            intersections.add(new Intersection(faceIndex, Optional.of(edge), vertexIndex));
        }

        Set<Integer> vertexIndices = getFaceVerticesIntersectingPlane(
            mesh, faceIndex, cutHeight, bedToLocalConverter);
        for (Integer vertexIndex : vertexIndices)
        {
            intersections.add(new Intersection(faceIndex, Optional.empty(), vertexIndex));
        }
        return intersections;

    }

    static int getFaceOppositeEdge(int faceIndex, Edge edge,
        Map<Integer, Set<Integer>> facesWithVertices)
    {
        Set<Integer> facesWithV0 = new HashSet(facesWithVertices.get(edge.v0));
        Set<Integer> facesWithV1 = facesWithVertices.get(edge.v1);
        facesWithV0.retainAll(facesWithV1);
        facesWithV0.remove(faceIndex);
        assert facesWithV0.size() == 1;
        int otherFaceIndex = facesWithV0.iterator().next();
        return otherFaceIndex;
    }

    /**
     * Get the adjacent intersections(face/vertex) to the given intersection. Find the other
     * intersection in the same face as the given intersection, which has vertex V2. Then, for each
     * face adjacent to that intersection, if it has two intersections then include the intersection
     * on the other face which is for vertex V2.
     */
    static Set<Intersection> getAdjacentIntersections(Intersection intersection,
        TriangleMesh mesh,
        float cutHeight, BedToLocalConverter bedToLocalConverter,
        Map<Integer, Set<Integer>> facesWithVertices, boolean[] faceVisited)
    {

        Set<Intersection> intersectionsForThisFace = getFaceIntersections(intersection.faceIndex,
                                                                          mesh, cutHeight,
                                                                          bedToLocalConverter);
        assert intersectionsForThisFace.size() == 2 : "num intersections for face "
            + intersectionsForThisFace.size();
        assert intersectionsForThisFace.contains(intersection);
        intersectionsForThisFace.remove(intersection);
        assert intersectionsForThisFace.size() == 1;
        Intersection nextIntersectionSameFace = intersectionsForThisFace.iterator().next();

        System.out.println("nexy intersection same face is " + nextIntersectionSameFace);

        int previousVertexIndex = intersection.vertexIndex;
        int nextVertexIndex = nextIntersectionSameFace.vertexIndex;

        Set<Intersection> possibleIntersections = new HashSet<>();

        if (nextIntersectionSameFace.edge.isPresent())
        {
            int oppositeFaceIndex = getFaceOppositeEdge(nextIntersectionSameFace.faceIndex,
                                                        nextIntersectionSameFace.edge.get(),
                                                        facesWithVertices);
            faceVisited[oppositeFaceIndex] = true;
            Set<Intersection> intersections = getFaceIntersections(oppositeFaceIndex, mesh,
                                                                   cutHeight, bedToLocalConverter);
            for (Intersection oppositeFaceIntersection : intersections)
            {
                if (oppositeFaceIntersection.vertexIndex == nextVertexIndex)
                {
                    possibleIntersections.add(oppositeFaceIntersection);
                    break;
                }
            }
        } else
        {
            for (Integer otherFaceIndex : facesWithVertices.get(nextVertexIndex))
            {
                if (otherFaceIndex == nextIntersectionSameFace.faceIndex)
                {
                    continue;
                }
                faceVisited[otherFaceIndex] = true;
                Set<Intersection> otherFaceIntersections = getFaceIntersections(otherFaceIndex, mesh,
                                                                                cutHeight,
                                                                                bedToLocalConverter);
                System.out.println("other face intersections for " + otherFaceIndex + "are "
                    + otherFaceIntersections);
                if (otherFaceIntersections.size() > 1)
                {
                    boolean intersectionBacktracks = false;
                    for (Intersection otherFaceIntersection : otherFaceIntersections)
                    {
                        if (otherFaceIntersection.vertexIndex == previousVertexIndex)
                        {
                            intersectionBacktracks = true;
                        }
                    }
                    if (intersectionBacktracks)
                    {
                        continue;
                    }
                    for (Intersection otherFaceIntersection : otherFaceIntersections)
                    {
                        if (otherFaceIntersection.vertexIndex == nextVertexIndex)
                        {
                            possibleIntersections.add(otherFaceIntersection);
                        }
                    }

                }
            }
        }

        return possibleIntersections;
    }


    static class NextVertexResult
    {

        final int faceIndex;
        final int vertexIndex;

        public NextVertexResult(int faceIndex, int vertexIndex)
        {
            this.faceIndex = faceIndex;
            this.vertexIndex = vertexIndex;
        }

    }

    /**
     * Get the next LoopOfVerticesAndCutFaces for faces that have not yet been visited.
     */
    private static Optional<LoopOfVerticesAndCutFaces> getNextLoopOfVertices(
        boolean[] faceVisited, boolean[] perimeterVertex, TriangleMesh mesh,
        float cutHeight, Map<Integer, Set<Integer>> facesWithVertices,
        BedToLocalConverter bedToLocalConverter)
    {
        LoopOfVerticesAndCutFaces loopOfFacesAndVertices = new LoopOfVerticesAndCutFaces();

        int firstFaceIndex = getFirstUnvisitedIntersectingOrTouchingFace(faceVisited,
                                                                         perimeterVertex,
                                                                         mesh,
                                                                         cutHeight,
                                                                         bedToLocalConverter);
        if (firstFaceIndex == -1)
        {
            return Optional.empty();
        }
        faceVisited[firstFaceIndex] = true;

        Set<Intersection> firstIntersections = getFaceIntersections(firstFaceIndex, mesh, cutHeight,
                                                                    bedToLocalConverter);
        Intersection firstIntersection = firstIntersections.iterator().next();
        Intersection lastIntersection = firstIntersection;

        System.out.println("first intersection is " + firstIntersection);

        perimeterVertex[firstIntersection.vertexIndex] = true;
        loopOfFacesAndVertices.loopOfVertices.add(firstIntersection.vertexIndex);

        List<Integer> faceIndices = new ArrayList<>();
        faceIndices.add(firstFaceIndex);
        List<Intersection> intersections = new ArrayList<>();
        intersections.add(firstIntersection);

        // loop finding vertex loop and any cut faces
        while (true)
        {

            System.out.println("faces so far: " + faceIndices);
            System.out.println("vertices so far: " + loopOfFacesAndVertices.loopOfVertices);
            Set<Intersection> possibleIntersections = getAdjacentIntersections(lastIntersection,
                                                                               mesh, cutHeight,
                                                                               bedToLocalConverter,
                                                                               facesWithVertices,
                                                                               faceVisited);

            Intersection nextIntersection = null;
            System.out.println("possible intersections: ");
            for (Intersection possibleIntersection : possibleIntersections)
            {
                System.out.println(possibleIntersection);
            }
            for (Intersection intersection : possibleIntersections)
            {
                if (faceIndices.contains(intersection.faceIndex)
                    || loopOfFacesAndVertices.loopOfVertices.contains(intersection.vertexIndex))
                {
                    continue;
                }
                if (!intersections.contains(intersection))
                {
                    nextIntersection = intersection;
                    break;
                }
            }
            if (nextIntersection == null)
            {
                for (Intersection intersection : possibleIntersections)
                {
                    if (intersection.equals(firstIntersection))
                    {
                        nextIntersection = intersection;
                        break;
                    }
                }
            }
            assert nextIntersection != null;
            lastIntersection = nextIntersection;
            System.out.println("next intersection: " + nextIntersection);

            int vertexIndex = nextIntersection.vertexIndex;
            int faceIndex = nextIntersection.faceIndex;

            if (vertexIndex == firstIntersection.vertexIndex)
            {
                break;
            }

            System.out.println("found next vertex: " + vertexIndex);
            if (vertexIndex != loopOfFacesAndVertices.loopOfVertices.get(
                loopOfFacesAndVertices.loopOfVertices.size() - 1))
            {
                loopOfFacesAndVertices.loopOfVertices.add(vertexIndex);
                perimeterVertex[vertexIndex] = true;
            }
            System.out.println("face index is " + faceIndex);
            faceIndices.add(faceIndex);
            faceVisited[faceIndex] = true;

        }

        addIntersectedFaces(faceIndices, mesh, cutHeight, bedToLocalConverter,
                            loopOfFacesAndVertices);

        // mark all faces touching loop vertices as done
        for (Integer vertexIndex : loopOfFacesAndVertices.loopOfVertices)
        {
            if (facesWithVertices.containsKey(vertexIndex))
            {
                for (int faceIndex2 : facesWithVertices.get(vertexIndex))
                {
                    faceVisited[faceIndex2] = true;
                }
            }
        }

        return Optional.of(loopOfFacesAndVertices);
    }

    /**
     * Find the next vertex/face in the loop, given that the previous vertex lay along an edge of
     * the previous face.
     */
    private static NextVertexResult getNextVertexInLoopEdgeBased(TriangleMesh mesh, float cutHeight,
        BedToLocalConverter bedToLocalConverter, boolean[] faceVisited,
        Map<Integer, Set<Integer>> facesWithVertices,
        PolygonIndices loopOfVertices, int previousFaceIndex)
    {

        int vertexIndex = -1;
        int faceIndex = -1;

        Edge previousEdge = getEdgeOnVertex(mesh, previousFaceIndex, loopOfVertices.get(
                                            loopOfVertices.size() - 1),
                                            bedToLocalConverter, cutHeight);
        Set<Integer> facesWithV0 = new HashSet(facesWithVertices.get(previousEdge.v0));
        Set<Integer> facesWithV1 = facesWithVertices.get(previousEdge.v1);
        facesWithV0.retainAll(facesWithV1);
        facesWithV0.remove(previousFaceIndex);
        assert facesWithV0.size() == 1;
        int otherFaceIndex = facesWithV0.iterator().next();
        System.out.println("face on other side of edge is " + otherFaceIndex);
        faceVisited[otherFaceIndex] = true;
        Set<Edge> otherEdges = getEdgesOfFaceThatPlaneIntersects(mesh, otherFaceIndex,
                                                                 cutHeight,
                                                                 bedToLocalConverter);
        System.out.println("previous edge: " + previousEdge + " " + previousEdge.v0 + " "
            + previousEdge.v1);
        otherEdges.remove(previousEdge);

        if (otherEdges.size() == 1)
        {

            Edge nextEdge = otherEdges.iterator().next();
            System.out.println("found other edge: " + nextEdge + " " + nextEdge.v0 + " "
                + nextEdge.v1);
            Vertex nextVertex = getIntersectingVertex(nextEdge, mesh, cutHeight,
                                                      bedToLocalConverter);
            vertexIndex = addNewOrGetVertex(mesh, nextVertex);
            faceIndex = otherFaceIndex;
        } else
        {
            System.out.println("must have vertex on plane: other edges: " + otherEdges.size());
            // this triangle must have one vertex on the plane
            Set<Integer> vertexIndices = getFaceVerticesIntersectingPlane(
                mesh, otherFaceIndex, cutHeight, bedToLocalConverter);
            assert vertexIndices.size() == 1;
            vertexIndex = vertexIndices.iterator().next();
            faceIndex = otherFaceIndex;
        }
        assert vertexIndex != -1 : "no vertex found for loop " + loopOfVertices;
        return new NextVertexResult(faceIndex, vertexIndex);

    }

    /**
     * Go through faces that use this vertex (other than the previous face) and if the face has an
     * edge that intersects the plane, or two vertices on the plane, then this is the next face.
     */
    private static NextVertexResult getNextVertexInLoopVertexBased(TriangleMesh mesh,
        float cutHeight,
        BedToLocalConverter bedToLocalConverter, boolean[] faceVisited,
        Map<Integer, Set<Integer>> facesWithVertices,
        PolygonIndices loopOfVertices, int previousFaceIndex)
    {
        int previousVertexIndex = loopOfVertices.get(loopOfVertices.size() - 1);
        int vertexIndex = -1;
        int faceIndex = -1;

        Set<NextVertexResult> possibleResults = new HashSet<>();
        for (Integer otherFaceIndex : facesWithVertices.get(previousVertexIndex))
        {
            if (otherFaceIndex == previousFaceIndex)
            {
                continue;
            }

            Set<Edge> edges = getEdgesOfFaceThatPlaneIntersects(mesh, otherFaceIndex,
                                                                cutHeight,
                                                                bedToLocalConverter);
            if (!edges.isEmpty())
            {
                System.out.println("found next face which has an edge: " + otherFaceIndex);
                faceIndex = otherFaceIndex;
                faceVisited[faceIndex] = true;

                Vertex previousVertex = getVertex(mesh, previousVertexIndex);
                for (Edge edge : edges)
                {
                    Vertex vertex = getIntersectingVertex(edge, mesh, cutHeight,
                                                          bedToLocalConverter);
                    if (vertex.equals(previousVertex))
                    {
                        continue;
                    } else
                    {
                        vertexIndex = addNewOrGetVertex(mesh, vertex);
                        possibleResults.add(new NextVertexResult(otherFaceIndex, vertexIndex));
                        break;
                    }
                }
            } else
            {

                Set<Integer> vertexIndices = getFaceVerticesIntersectingPlane(
                    mesh, otherFaceIndex, cutHeight, bedToLocalConverter);
                if (vertexIndices.size() == 2)
                {
                    System.out.println("found face with 2 intersecting vertices " + otherFaceIndex);
                    faceIndex = otherFaceIndex;
                    faceVisited[faceIndex] = true;

                    for (Integer vertexIndex2 : vertexIndices)
                    {
                        if (vertexIndex2 == previousVertexIndex)
                        {
                            continue;
                        } else
                        {
                            vertexIndex = vertexIndex2;
                            possibleResults.add(new NextVertexResult(otherFaceIndex, vertexIndex));
                            break;
                        }
                    }
                }
            }
        }

        assert vertexIndex != -1;

        System.out.println("num possible results " + possibleResults.size());
        if (possibleResults.size() == 1)
        {
            return possibleResults.iterator().next();
        } else
        {
            for (NextVertexResult possibleResult : possibleResults)
            {
                // add new vertex if there is one
                if (!loopOfVertices.contains(possibleResult.vertexIndex))
                {
                    return possibleResult;
                }
            }
            for (NextVertexResult possibleResult : possibleResults)
            {
                // we're back to the beginning
                if (possibleResult.vertexIndex == loopOfVertices.get(0))
                {
                    return possibleResult;
                }
            }
        }
        throw new RuntimeException("should not get here");

    }

    /**
     * Find the next vertexIndex/faceIndex to extend this cutting loop.
     */
    private static NextVertexResult getNextVertexInLoop(TriangleMesh mesh, float cutHeight,
        BedToLocalConverter bedToLocalConverter, boolean[] faceVisited,
        Map<Integer, Set<Integer>> facesWithVertices,
        PolygonIndices loopOfVertices, int previousFaceIndex)
    {

        int vertexIndex = -1;
        int faceIndex = -1;

        int previousVertexIndex = loopOfVertices.get(loopOfVertices.size() - 1);
        System.out.println("previous face passed in is " + previousFaceIndex);

        if (facesWithVertices.containsKey(previousVertexIndex))
        {
            System.out.println("previous vertex is in original mesh");

            NextVertexResult result = getNextVertexInLoopVertexBased(mesh, cutHeight,
                                                                     bedToLocalConverter,
                                                                     faceVisited, facesWithVertices,
                                                                     loopOfVertices,
                                                                     previousFaceIndex);
            vertexIndex = result.vertexIndex;
            faceIndex = result.faceIndex;

        } else
        {
            System.out.println("previous vertex is on an edge");

            NextVertexResult result = getNextVertexInLoopEdgeBased(mesh, cutHeight,
                                                                   bedToLocalConverter,
                                                                   faceVisited, facesWithVertices,
                                                                   loopOfVertices,
                                                                   previousFaceIndex);
            vertexIndex = result.vertexIndex;
            faceIndex = result.faceIndex;

        }
        assert vertexIndex != -1;
        if (vertexIndex == loopOfVertices.get(0))
        {
            System.out.println("next vertex is same as first vertex: stopping " + vertexIndex);
            System.out.println("loop is " + loopOfVertices);
            return new NextVertexResult(faceIndex, vertexIndex);
        }

        int lastVertexIndex = loopOfVertices.get(loopOfVertices.size() - 1);
        if (loopOfVertices.contains(vertexIndex) && (vertexIndex != lastVertexIndex))
        {
            System.out.println("new vertex " + getVertex(mesh, vertexIndex));
            System.out.println("loop: " + loopOfVertices);
            System.out.println("face: " + faceIndex);
            int vertex0 = mesh.getFaces().get(faceIndex * 6);
            int vertex1 = mesh.getFaces().get(faceIndex * 6 + 2);
            int vertex2 = mesh.getFaces().get(faceIndex * 6 + 4);
            System.out.println("vertices " + vertex0 + " " + vertex1 + " " + vertex2);
            System.out.println(getVertex(mesh, vertex0));
            System.out.println(getVertex(mesh, vertex1));
            System.out.println(getVertex(mesh, vertex2));
            Set<Edge> edges = getEdgesOfFaceThatPlaneIntersects(mesh, faceIndex,
                                                                cutHeight,
                                                                bedToLocalConverter);
            for (Edge edge : edges)
            {
                System.out.println("Face Edge " + edge + " " + edge.v0 + " " + edge.v1);
            }
            throw new RuntimeException("vertex already in loop: " + vertexIndex);
        }

        return new NextVertexResult(faceIndex, vertexIndex);

    }

    /**
     * Return the edge that the vertex lies on.
     */
    private static Edge getEdgeOnVertex(TriangleMesh mesh, int faceIndex, int vertexIndex,
        BedToLocalConverter bedToLocalConverter, float cutHeight)
    {
        Set<Edge> edges = getEdgesOfFaceThatPlaneIntersects(mesh, faceIndex,
                                                            cutHeight,
                                                            bedToLocalConverter);
        for (Edge edge : edges)
        {
            Vertex vertex = getIntersectingVertex(edge, mesh, cutHeight,
                                                  bedToLocalConverter);
            if (vertex.equals(getVertex(mesh, vertexIndex)))
            {
                return edge;
            }

        }
        throw new RuntimeException("Edge not found");
    }

    private static void addIntersectedFaces(List<Integer> faceIndices, TriangleMesh mesh,
        float cutHeight, BedToLocalConverter bedToLocalConverter,
        LoopOfVerticesAndCutFaces loopOfFacesAndVertices)
    {
        for (Integer faceIndex2 : faceIndices)
        {
            System.out.println("check for intersected face: " + faceIndex2);
            // add intersectedFace if necessary
            Set<Edge> edges = getEdgesOfFaceThatPlaneIntersects(mesh, faceIndex2, cutHeight,
                                                                bedToLocalConverter);
            System.out.println(edges.size() + " intersected edges");
            if (!edges.isEmpty())
            {
                List<Integer> vertexIndices = new ArrayList<>();
                for (Edge edge : edges)
                {
                    int vertexIndex = makeIntersectingVertex(mesh, edge, cutHeight,
                                                             bedToLocalConverter);
                    vertexIndices.add(vertexIndex);
                }
                if (edges.size() == 1)
                {
                    // one vertex of the face must intersect the plane
                    Set<Integer> faceVertexIndices = getFaceVerticesIntersectingPlane(
                        mesh, faceIndex2, cutHeight, bedToLocalConverter);
                    assert faceVertexIndices.size() == 1;
                    vertexIndices.add(faceVertexIndices.iterator().next());
                }

                if (vertexIndices.get(0).equals(vertexIndices.get(1)))
                {
                    // can happen when intersection is very near to vertex with acute angle

//                    System.out.println("2 intersected edges of face hava same vertex");
//                    System.out.println("v0 " + vertexIndices.get(0) + " " + getVertex(mesh, vertexIndices.get(0)));
//                    System.out.println("v1 " + vertexIndices.get(1) + " "  + getVertex(mesh, vertexIndices.get(1)));
//                    assert false;
                }
                System.out.println("add intersected face " + vertexIndices);
                IntersectedFace intersectedFace = new IntersectedFace(faceIndex2, vertexIndices);
                loopOfFacesAndVertices.cutFaces.add(intersectedFace);
            }
        }
    }

    /**
     * Return the intersecting vertices of the face.
     */
    private static Set<Vertex> getIntersectingVertices(TriangleMesh mesh, int faceIndex,
        float cutHeight, BedToLocalConverter bedToLocalConverter)
    {
        Set<Vertex> vertices = new HashSet<>();
        Set<Edge> edges = getEdgesOfFaceThatPlaneIntersects(mesh, faceIndex, cutHeight,
                                                            bedToLocalConverter);
        for (Edge edge : edges)
        {
            int vertexIndex = makeIntersectingVertex(mesh, edge, cutHeight,
                                                     bedToLocalConverter);

            vertices.add(getVertex(mesh, vertexIndex));
        }

        Set<Integer> vertexIndices = getFaceVerticesIntersectingPlane(
            mesh, faceIndex, cutHeight, bedToLocalConverter);
        for (Integer vertexIndex : vertexIndices)
        {
            vertices.add(getVertex(mesh, vertexIndex));
        }

        return vertices;
    }

    private static Set<Integer> getFaceVerticesIntersectingPlane(TriangleMesh mesh, int faceIndex,
        float cutHeight, BedToLocalConverter bedToLocalConverter)
    {
        Set<Integer> vertexIndices = new HashSet<>();

        int vertex0 = mesh.getFaces().get(faceIndex * 6);
        int vertex1 = mesh.getFaces().get(faceIndex * 6 + 2);
        int vertex2 = mesh.getFaces().get(faceIndex * 6 + 4);
        Point3D point0InBed = bedToLocalConverter.localToBed(makePoint3D(mesh, vertex0));
        Point3D point1InBed = bedToLocalConverter.localToBed(makePoint3D(mesh, vertex1));
        Point3D point2InBed = bedToLocalConverter.localToBed(makePoint3D(mesh, vertex2));
        int num = 0;
        if (point0InBed.getY() == cutHeight)
        {
            vertexIndices.add(vertex0);
        }
        if (point1InBed.getY() == cutHeight)
        {
            vertexIndices.add(vertex1);
        }
        if (point2InBed.getY() == cutHeight)
        {
            vertexIndices.add(vertex2);
        }
        return vertexIndices;
    }

    /**
     * Add the vertex if it does not already exist in the mesh, and return its index. This is
     * inefficient and could easily be improved by caching vertices.
     */
    private static int addNewOrGetVertex(TriangleMesh mesh, Vertex intersectingVertex)
    {
        for (int i = 0; i < mesh.getPoints().size() / 3; i++)
        {
            Vertex vertex = getVertex(mesh, i);
            if (vertex.equals(intersectingVertex))
            {
                System.out.println("vertex already exists at " + i);
                return i;
            }
        }
        System.out.println("add new vertex at " + intersectingVertex);
        mesh.getPoints().addAll((float) intersectingVertex.x, (float) intersectingVertex.y,
                                (float) intersectingVertex.z);
        return mesh.getPoints().size() / 3 - 1;
    }

    private static int getFirstUnvisitedIntersectingOrTouchingFace(boolean[] faceVisited,
        boolean[] perimeterVertex, TriangleMesh mesh, float cutHeight,
        BedToLocalConverter bedToLocalConverter)
    {
        int faceIndex = findFirstUnvisitedFace(faceVisited);
        if (faceIndex != -1)
        {
            while (true)
            {

                int v0 = mesh.getFaces().get(faceIndex * 6);
                int v1 = mesh.getFaces().get(faceIndex * 6 + 2);
                int v2 = mesh.getFaces().get(faceIndex * 6 + 4);
                
                if (perimeterVertex[v0] || perimeterVertex[v1] || perimeterVertex[v2]) {
                    
                }

                int numIntersectingVertices = getIntersectingVertices(mesh, faceIndex, cutHeight,
                                                                      bedToLocalConverter).size();
                if (!perimeterVertex[v0] && !perimeterVertex[v1] && !perimeterVertex[v2]
                    && numIntersectingVertices > 1)
                {
                    break;
                }

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

    static int getFaceAdjacentToVertices(TriangleMesh mesh,
        Map<Integer, Set<Integer>> facesWithVertices,
        int faceIndex, int vertexIndexOffset0, int vertexIndexOffset1)
    {
        Set<Integer> facesWithVertex0 = new HashSet(facesWithVertices.get(
            mesh.getFaces().get(faceIndex * 6 + vertexIndexOffset0 * 2)));

        Set<Integer> facesWithVertex1 = facesWithVertices.get(
            mesh.getFaces().get(faceIndex * 6 + vertexIndexOffset1 * 2));
        facesWithVertex0.remove(faceIndex);
        facesWithVertex0.retainAll(facesWithVertex1);
        assert facesWithVertex0.size() == 1 : "faces with vertex0: " + facesWithVertex0.size();
        return facesWithVertex0.iterator().next();
    }

    /**
     * Return the edges that the plane intersects (not touches).
     */
    private static Set<Edge> getEdgesOfFaceThatPlaneIntersects(TriangleMesh mesh, int faceIndex,
        float cutHeight, BedToLocalConverter bedToLocalConverter)
    {
        Set<Edge> edges = new HashSet<>();
        int vertex0 = mesh.getFaces().get(faceIndex * 6);
        int vertex1 = mesh.getFaces().get(faceIndex * 6 + 2);
        int vertex2 = mesh.getFaces().get(faceIndex * 6 + 4);

        if (lineIntersectsPlane(mesh, vertex0, vertex1, cutHeight, bedToLocalConverter))
        {
            edges.add(new Edge(vertex0, vertex1));
        }
        if (lineIntersectsPlane(mesh, vertex1, vertex2, cutHeight, bedToLocalConverter))
        {
            edges.add(new Edge(vertex1, vertex2));
        }
        if (lineIntersectsPlane(mesh, vertex0, vertex2, cutHeight, bedToLocalConverter))
        {
            edges.add(new Edge(vertex0, vertex2));
        }
        return edges;
    }

//    private static boolean lineIntersectsOrTouchesPlane(TriangleMesh mesh, int vertex0, int vertex1,
//        float cutHeight, BedToLocalConverter bedToLocalConverter)
//    {
//
//        float y0 = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, vertex0)).getY();
//        float y1 = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, vertex1)).getY();
//
//        if (((y0 <= cutHeight) && (cutHeight <= y1))
//            || ((y1 <= cutHeight) && (cutHeight <= y0)))
//        {
//            return true;
//        }
//        return false;
//    }
    /**
     * Does the line intersect the plane (not just touch).
     */
    private static boolean lineIntersectsPlane(TriangleMesh mesh, int vertex0, int vertex1,
        float cutHeight, BedToLocalConverter bedToLocalConverter)
    {

        float y0 = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, vertex0)).getY();
        float y1 = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, vertex1)).getY();

        if (((y0 < cutHeight) && (cutHeight < y1))
            || ((y1 < cutHeight) && (cutHeight < y0)))
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

    private static void perturbVerticesOnCut(TriangleMesh mesh, float cutHeight,
        BedToLocalConverter bedToLocalConverter)
    {
        for (int i = 0; i < mesh.getPoints().size() / 3; i++)
        {
            Point3D pointInBed = bedToLocalConverter.localToBed(makePoint3D(mesh, i));
            if (Math.abs(pointInBed.getY() - cutHeight) < 1e-6)
            {
                Point3D perturbedPointInBed = new Point3D(
                    pointInBed.getX(),
                    pointInBed.getY() + 1e-6,
                    pointInBed.getZ());
                Point3D perturbedPointInLocal = bedToLocalConverter.bedToLocal(perturbedPointInBed);
                mesh.getPoints().set(i, (float) perturbedPointInLocal.getX());
                mesh.getPoints().set(i + 1, (float) perturbedPointInLocal.getY());
                mesh.getPoints().set(i + 2, (float) perturbedPointInLocal.getZ());
            }
        }
    }
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
    public String toString()
    {
        return "Edge{" + "v0=" + v0 + ", v1=" + v1 + '}';
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


/**
 * The main purpose of this Vertex class is to provide an equality operation.
 *
 * @author tony
 */
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

    static boolean equaltoNplaces(double a, double b)
    {
//        return Math.round(a * 10e8) == Math.round(b * 10e8);
        return a == b;
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
        if (!equaltoNplaces(other.x, x))
        {
            return false;
        }
        if (!equaltoNplaces(other.y, y))
        {
            return false;
        }
        if (!equaltoNplaces(other.z, z))
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        return (int) (Math.round(x * 10e8) + Math.round(y * 10e8) + Math.round(z * 10e8));
    }
}
