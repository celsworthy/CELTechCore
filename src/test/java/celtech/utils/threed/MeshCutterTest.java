/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import celtech.utils.threed.MeshCutter.BedToLocalConverter;
import celtech.utils.threed.importers.stl.STLFileParsingException;
import celtech.utils.threed.importers.stl.STLImporter;
import java.io.File;
import java.net.URL;
import java.util.Set;
import javafx.geometry.Point3D;
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
        BedToLocalConverter nullBedToLocalConverter = new BedToLocalConverter()
        {

            @Override
            public Point3D localToBed(Point3D point)
            {
                return point;
            }

            @Override
            public Point3D bedToLocal(Point3D point)
            {
                return point;
            }
        };
        
        Set<TriangleMesh> triangleMeshs = MeshCutter.cut(mesh, -7, nullBedToLocalConverter);
        assertEquals(2, triangleMeshs.size());
    }
    
        @Test
    public void testCutCubeWithHoleReturnsTwoMeshes() throws STLFileParsingException
    {
        
        URL stlURL = this.getClass().getResource("/cubewithhole.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        
        BedToLocalConverter nullBedToLocalConverter = new BedToLocalConverter()
        {

            @Override
            public Point3D localToBed(Point3D point)
            {
                return point;
            }

            @Override
            public Point3D bedToLocal(Point3D point)
            {
                return point;
            }
        };
        
        Set<TriangleMesh> triangleMeshs = MeshCutter.cut(mesh, -15, nullBedToLocalConverter);
        assertEquals(2, triangleMeshs.size());
    }
    
}
