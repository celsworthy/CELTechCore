/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import static celtech.utils.threed.MeshSeparator.makeFacesWithVertex;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.scene.shape.TriangleMesh;

/**
 * MeshCutter cuts a mesh along a given horizontal plane, triangulates the two resultant open faces,
 * and creates two child meshes.
 *
 * @author tony
 */
public class MeshCutter
{

    private class CutResult
    {

        /**
         * The child nesh that was created by the split.
         */
        TriangleMesh childMesh;
        /**
         * The indices of the vertices of the child mesh, in sequence, that form the perimeter of
         * the new open face that needs to be triangulated. The first List is for the outer
         * perimeter and any other lists are holes.
         */
        List<List<Integer>> vertexsOnOpenFace;
    }

    /**
     * Cut the given mesh into two, at the given height.
     */
    public static Set<TriangleMesh> cut(TriangleMesh mesh, double cutHeight)
    {
        Set<TriangleMesh> triangleMeshs = new HashSet<>();

        Set<CutResult> splitResults = getCutFaces(mesh, cutHeight);
        for (CutResult splitResult : splitResults)
        {
            TriangleMesh childMesh = closeOpenFace(splitResult);
            triangleMeshs.add(childMesh);
        }

        return triangleMeshs;
    }

    private static Set<CutResult> getCutFaces(TriangleMesh mesh, double cutHeight)
    {
        Set<CutResult> cutResults = new HashSet<>();
        List<Integer> cutFaces = getCutFaceIndices(mesh, cutHeight);
        return cutResults;
    }

    /**
     * Get the ordered list of face indices that are cut by the plane. The faces must be ordered
     * according to adjacency, so that we can get a correct list of ordered vertices on the
     * perimeter.
     */
    private static List<Integer> getCutFaceIndices(TriangleMesh mesh, double cutHeight)
    {
        Map<Integer, Set<Integer>> facesWithVertices = makeFacesWithVertex(mesh);
        System.out.println("faces with vertices " + facesWithVertices);
        List<Integer> cutFaceIndices = new ArrayList<>();
        boolean[] faceVisited = new boolean[mesh.getFaces().size() / 6];
        int previousFaceIndex = -1;
        int faceIndex = getFirstUnvisitedIntersectingFace(faceVisited, mesh, cutHeight);
        while (true)
        {
            System.out.println("treat face B " + faceIndex);
            Set<Integer> edges = planeIntersectsEdgesOfFace(mesh, faceIndex, cutHeight);
            if (edges.size() != 0)
            {
                faceVisited[faceIndex] = true;
                cutFaceIndices.add(faceIndex);

                // there should be two faces adjacent to this one that the plane also cuts
                Set<Integer> facesAdjacentToEdgesOfFace
                    = getFacesAdjacentToEdgesOfFace(mesh, faceIndex, facesWithVertices, edges);

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
        return cutFaceIndices;
    }

    private static int getFirstUnvisitedIntersectingFace(boolean[] faceVisited, TriangleMesh mesh,
        double cutHeight)
    {
        int faceIndex = findFirstUnvisitedFace(faceVisited);
        while (planeIntersectsEdgesOfFace(mesh, faceIndex, cutHeight).size() == 0)
        {
            System.out.println("consider face A " + faceIndex);
            faceVisited[faceIndex] = true;
            faceIndex = findFirstUnvisitedFace(faceVisited);
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
        System.out.println("fc index " + faceIndex + " " + mesh.getFaces().get(faceIndex * 6
            + vertexIndexOffset0 * 2)
            + " " + mesh.getFaces().get(faceIndex * 6 + vertexIndexOffset1 * 2));
        Set<Integer> facesWithVertex0 = new HashSet(facesWithVertices.get(
            mesh.getFaces().get(faceIndex * 6 + vertexIndexOffset0 * 2)));

        Set<Integer> facesWithVertex1 = facesWithVertices.get(
            mesh.getFaces().get(faceIndex * 6 + vertexIndexOffset1 * 2));
        System.out.println("A " + facesWithVertex0);
        System.out.println("B " + facesWithVertex1);
        facesWithVertex0.remove(faceIndex);
        facesWithVertex0.retainAll(facesWithVertex1);
        System.out.println(facesWithVertex0);
        assert (facesWithVertex0.size() == 1);
        return facesWithVertex0.iterator().next();
    }

    /**
     * Return the two edges that the plane intersects. V0 -> V1 is called edge 1, V1 -> V2 is edge 2
     * and V0 -> V2 is edge 3.
     */
    private static Set<Integer> planeIntersectsEdgesOfFace(TriangleMesh mesh, int faceIndex,
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

    private static boolean lineIntersectsPlane(TriangleMesh mesh, int vertex0, int vertex1,
        double cutHeight)
    {
        double z0 = mesh.getPoints().get(vertex0 + 2);
        double z1 = mesh.getPoints().get(vertex1 + 2);
        if (((z0 <= cutHeight) && (cutHeight <= z1))
            || ((z1 <= cutHeight) && (cutHeight <= z0)))
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
    private static TriangleMesh closeOpenFace(CutResult splitResult)
    {
        return splitResult.childMesh;
    }

}
