/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.shape.TriangleMesh;

/**
 * MeshSeparator takes an input {@link javafx.scene.shape.TriangleMesh} and returns multiple
 * TriangleMeshes according to the number of separate objects (non-joined) in the input mesh. The
 * concept of 'joining' is based on having a common vertex.
 *
 * @author tony
 */
public class MeshSeparator
{

    /**
     * Separate the given mesh into multiple meshes according to the number of separate (non-joined)
     * objects in the given mesh.
     */
    static List<TriangleMesh> separate(TriangleMesh mesh)
    {
        List<TriangleMesh> meshes = new ArrayList<>();

        System.out.println("create VV of size " + mesh.getPoints().size() / 3) ;
        System.out.println("create FV of size " + mesh.getFaces().size() / 6) ;
        boolean[] vertexVisited = new boolean[mesh.getPoints().size() / 3];
        boolean[] faceVisited = new boolean[mesh.getFaces().size() / 6];

        /**
         * For first vertex:.
         * <p>
         * 1) Create a new face group.</p><p>
         * 2) Find all faces using that vertex. Put into the group. Mark vertex and faces as done. </p><p>
         * 3) For each face found, process vertices of the faces. Mark faces and vertices as done. </p><p>
         * 4) Continue until all found connected faces / vertices are already marked. Then </p><p>
         * 5) Find first unmarked vertex. Create a new face group. Continue as before. Then No
         * unmarked vertices left. We have a number of groups. Each group is a separate object.</p>
         */
        // we choose vertex 0 as the first vertex.
        Set<Integer> faceGroup = new HashSet<>();
        visitVertex(faceGroup, mesh, vertexVisited, faceVisited, 0);
        
        return meshes;
    }

    /**
     * Get the unmarked vertices of this face and visit them, marking it and its connected faces.
     */
    private static void visitFace(Set<Integer> faceGroup, TriangleMesh mesh, boolean[] vertexVisited,
        boolean[] faceVisited,
        Integer faceIndex)
    {
        System.out.println("visit face " + faceIndex);
        faceVisited[faceIndex] = true;
        int vertex0 = mesh.getFaces().get(faceIndex * 6);
        if (!vertexVisited[vertex0])
        {
            visitVertex(faceGroup, mesh, vertexVisited, faceVisited, vertex0);
        }
        int vertex1 = mesh.getFaces().get(faceIndex * 6 + 2);
        if (!vertexVisited[vertex1])
        {
            visitVertex(faceGroup, mesh, vertexVisited, faceVisited, vertex1);
        }
        int vertex2 = mesh.getFaces().get(faceIndex * 6 + 4);
        if (!vertexVisited[vertex2])
        {
            visitVertex(faceGroup, mesh, vertexVisited, faceVisited, vertex2);
        }
    }

    /**
     * Find unmarked faces that use this vertex and add them to the group. Mark all the faces and
     * visit each of them.
     */
    private static void visitVertex(Set<Integer> faceGroup, TriangleMesh mesh,
        boolean[] vertexVisited,
        boolean[] faceVisited, int vertexIndex)
    {
        System.out.println("visit vertex " + vertexIndex);
        vertexVisited[vertexIndex] = true;
        Set<Integer> faceIndices = getFacesWithVertex(mesh, faceVisited, vertexIndex);
        faceGroup.addAll(faceIndices);
        for (Integer faceIndex : faceIndices)
        {
            visitFace(faceGroup, mesh, vertexVisited, faceVisited, faceIndex);
        }
    }

    /**
     * Return the indices of those faces that use the vertex of the given index.
     */
    private static Set<Integer> getFacesWithVertex(TriangleMesh mesh, boolean[] faceVisited,
        int vertexIndex)
    {
        Set<Integer> faceIndices = new HashSet<>();
        int faceIndex = -1;
        while (faceIndex < mesh.getFaceElementSize() - 1)
        {
            faceIndex++;
            if (faceVisited[faceIndex])
            {
                continue;
            }
            int vertex0 = mesh.getFaces().get(faceIndex * 6);
            if (vertex0 == vertexIndex)
            {
                faceIndices.add(faceIndex);
                continue;
            }
            int vertex1 = mesh.getFaces().get(faceIndex * 6 + 2);
            if (vertex1 == vertexIndex)
            {
                faceIndices.add(faceIndex);
                continue;
            }
            int vertex2 = mesh.getFaces().get(faceIndex * 6 + 4);
            if (vertex2 == vertexIndex)
            {
                faceIndices.add(faceIndex);
            }
        }
        return faceIndices;
    }

}
