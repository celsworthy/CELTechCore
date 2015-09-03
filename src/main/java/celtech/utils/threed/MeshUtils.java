/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils.threed;

import static celtech.utils.threed.MeshSeparator.makeFacesWithVertex;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javafx.scene.shape.TriangleMesh;


/**
 *
 * @author alynch
 */
public class MeshUtils
{

    /**
     * Remove vertices that are not used by any faces.
     */
    static void removeUnusedVertices(TriangleMesh childMesh)
    {

        // array of new vertex index for previous index
        int[] newVertexIndices = new int[childMesh.getPoints().size()];
        for (int i = 0; i < newVertexIndices.length; i++)
        {
            newVertexIndices[i] = -1;
        }
        float[] newPoints = new float[childMesh.getPoints().size()];
        int nextNewPointIndex = 0;

        for (int i = 0; i < childMesh.getFaces().size(); i += 2)
        {
            int vertexIndex = childMesh.getFaces().get(i);
            if (newVertexIndices[vertexIndex] == -1)
            {
                newVertexIndices[vertexIndex] = nextNewPointIndex;
                newPoints[nextNewPointIndex * 3] = childMesh.getPoints().get(vertexIndex * 3);
                newPoints[nextNewPointIndex * 3 + 1] = childMesh.getPoints().get(vertexIndex * 3 + 1);
                newPoints[nextNewPointIndex * 3 + 2] = childMesh.getPoints().get(vertexIndex * 3 + 2);
                nextNewPointIndex++;
            }
            childMesh.getFaces().set(i, newVertexIndices[vertexIndex]);
        }

        childMesh.getPoints().clear();
        childMesh.getPoints().addAll(newPoints, 0, nextNewPointIndex * 3);
    }
    
    public enum MeshError {
        INVALID_VERTEX_ID, OPEN_MESH;
    }

    /**
     * Validate the mesh.
     */
    public static Optional<MeshError> validate(TriangleMesh mesh)
    {
        // validate vertex indices
        int numVertices = mesh.getPoints().size() / 3;
        for (int i = 0; i < mesh.getFaces().size(); i += 2)
        {
            int vertexIndex = mesh.getFaces().get(i);
            if (vertexIndex < 0 || vertexIndex > numVertices)
            {
                return Optional.of(MeshError.INVALID_VERTEX_ID);
            }
        }

        // validate mesh is not open (all edges are incident to two faces)
        Map<Integer, Set<Integer>> facesWithVertices = makeFacesWithVertex(mesh);
        for (int faceIndex = 0; faceIndex < mesh.getFaces().size() / 6; faceIndex++)
        {
            if (countFacesAdjacentToVertices(mesh, facesWithVertices, faceIndex, 0, 1) != 1)
            {
                System.out.println("actual count: " + countFacesAdjacentToVertices(mesh, facesWithVertices, faceIndex, 0, 1));
                return Optional.of(MeshError.OPEN_MESH);
            }
            if (countFacesAdjacentToVertices(mesh, facesWithVertices, faceIndex, 1, 2) != 1)
            {
                System.out.println("actual count: " + countFacesAdjacentToVertices(mesh, facesWithVertices, faceIndex, 1, 2));
                return Optional.of(MeshError.OPEN_MESH);
            }
            if (countFacesAdjacentToVertices(mesh, facesWithVertices, faceIndex, 0, 2) != 1)
            {
                System.out.println("actual count: " + countFacesAdjacentToVertices(mesh, facesWithVertices, faceIndex, 0, 2));
                return Optional.of(MeshError.OPEN_MESH);
            }
        }

        // validate mesh is orientable (winding order correct for all faces)
        
        
        return Optional.empty();
    }

    static int countFacesAdjacentToVertices(TriangleMesh mesh,
        Map<Integer, Set<Integer>> facesWithVertices,
        int faceIndex, int vertexIndexOffset0, int vertexIndexOffset1)
    {
        Set<Integer> facesWithVertex0 = new HashSet(facesWithVertices.get(
            mesh.getFaces().get(faceIndex * 6 + vertexIndexOffset0 * 2)));

        Set<Integer> facesWithVertex1 = facesWithVertices.get(
            mesh.getFaces().get(faceIndex * 6 + vertexIndexOffset1 * 2));
        facesWithVertex0.remove(faceIndex);
        facesWithVertex0.retainAll(facesWithVertex1);
        return facesWithVertex0.size();
    }

}
