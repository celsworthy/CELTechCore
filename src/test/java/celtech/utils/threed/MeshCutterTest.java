/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import celtech.utils.threed.importers.stl.STLFileParsingException;
import celtech.utils.threed.importers.stl.STLImporter;
import java.io.File;
import java.net.URL;
import java.util.Set;
import javafx.scene.shape.TriangleMesh;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class MeshCutterTest
{
    
    public MeshCutterTest()
    {
    }

    @Test
    public void testCutCubeReturnsTwoMeshes() throws STLFileParsingException
    {
        URL stlURL = this.getClass().getResource("/simplecube.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        Set<TriangleMesh> triangleMeshs = MeshCutter.cut(mesh, -5);
        assertEquals(2, triangleMeshs.size());
    }
    
}
