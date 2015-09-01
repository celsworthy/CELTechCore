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
public class MeshUtils {

    /**
     * Remove vertices that are not used by any faces.
     */
    static void removeUnusedVertices(TriangleMesh childMesh) {
    
        int[] newVertexIndices = new int[childMesh.getPoints().size()];
        
        
        for (int i = 0; i < childMesh.getFaces().size() / 2; i++) {
            int vertexIndex = childMesh.getFaces().get(i * 2);
            
        }
        
    
    }
    
}
