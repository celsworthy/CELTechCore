/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import celtech.utils.threed.importers.stl.STLFileParsingException;
import celtech.utils.threed.importers.stl.STLImporter;
import java.io.File;
import java.net.URL;
import java.util.List;
import javafx.scene.shape.TriangleMesh;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class MeshSeparatorTest
{

    @Test
    public void testMeshOfOneObject() throws STLFileParsingException
    {
        URL pyramidSTLURL = this.getClass().getResource("/pyramid1.stl");
        File singleObjectSTLFile = new File(pyramidSTLURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        List<TriangleMesh> meshes = MeshSeparator.separate(mesh);    
        assertEquals(1, meshes.size());
    }
    
    @Test
    public void testMeshOfTwoObjects() throws STLFileParsingException
    {
        URL pyramidSTLURL = this.getClass().getResource("/twodiscs.stl");
        File singleObjectSTLFile = new File(pyramidSTLURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        List<TriangleMesh> meshes = MeshSeparator.separate(mesh);    
        assertEquals(2, meshes.size());
    }    
    
}
