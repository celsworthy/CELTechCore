/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import static celtech.utils.threed.MeshCutter2.makeSplitMesh;
import javafx.scene.shape.TriangleMesh;
import static org.junit.Assert.assertEquals;
import org.junit.Test;


/**
 *
 * @author tony
 */
public class MeshCutter2Test
{

    @Test
    public void testMakeSplitMeshSimpleCubeBottom()
    {
        TriangleMesh mesh = TriangleCutterTest.createSimpleCube();
        assertEquals(12, mesh.getFaces().size() / 6);
        
        float cutHeight = 1f;
        TriangleMesh childMesh = makeSplitMesh(mesh, cutHeight, 
                           TriangleCutterTest.makeNullConverter(), MeshCutter.TopBottom.BOTTOM);
        
        assertEquals(14, childMesh.getFaces().size() / 6);
    }
    
    @Test
    public void testMakeSplitMeshSimpleCubeTop()
    {
        TriangleMesh mesh = TriangleCutterTest.createSimpleCube();
        assertEquals(12, mesh.getFaces().size() / 6);
        
        float cutHeight = 1f;
        TriangleMesh childMesh = makeSplitMesh(mesh, cutHeight, 
                           TriangleCutterTest.makeNullConverter(), MeshCutter.TopBottom.TOP);
        
        assertEquals(14, childMesh.getFaces().size() / 6);
    }    
    
}
