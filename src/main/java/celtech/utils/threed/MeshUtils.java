/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils.threed;

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

    /**
     * Validate the mesh.
     */
    public static boolean validate(TriangleMesh childMesh)
    {
        // validate vertex indices
        int numVertices = childMesh.getPoints().size() / 3;
        for (int i = 0; i < childMesh.getFaces().size(); i += 2)
        {
            int vertexIndex = childMesh.getFaces().get(i);
            if (vertexIndex < 0 || vertexIndex > numVertices)
            {
                return false;
            }
        }
        
        // validate mesh is not open (all edges are incident to two faces)
        
        
        // validate mesh is orientable
        
        return true;
    }

}
