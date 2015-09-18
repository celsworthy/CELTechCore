/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import static celtech.utils.threed.MeshCutter2.makeSplitMesh;
import static celtech.utils.threed.TriangleCutterTest.makeNullConverter;
import celtech.utils.threed.importers.stl.STLFileParsingException;
import celtech.utils.threed.importers.stl.STLImporter;
import java.io.File;
import java.net.URL;
import java.util.Optional;
import javafx.scene.shape.TriangleMesh;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Test;


/**
 *
 * @author tony
 */
public class MeshCutter2Test
{
    
    @Test
    public void testCutSimpleCube() {
        TriangleMesh mesh = TriangleCutterTest.createSimpleCube();
        assertEquals(12, mesh.getFaces().size() / 6);
        
        float cutHeight = 1f;
        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();

        MeshCutter.MeshPair meshes = MeshCutter2.cut(mesh, cutHeight, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.bottomMesh);
        Assert.assertNotNull(meshes.topMesh);
        
    }

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
    
    @Test
    public void testEnricoSTLAt1() throws STLFileParsingException
    {

        URL stlURL = this.getClass().getResource("/enrico.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        Optional<MeshUtils.MeshError> error = MeshUtils.validate(mesh);
        Assert.assertFalse(error.isPresent());

        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();

        MeshCutter.MeshPair meshes = MeshCutter2.cut(mesh, -1f, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.bottomMesh);
        Assert.assertNotNull(meshes.topMesh);
    }
    
    @Test
    public void testEnricoSTLAt3p8() throws STLFileParsingException
    {

        URL stlURL = this.getClass().getResource("/enrico.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        Optional<MeshUtils.MeshError> error = MeshUtils.validate(mesh);
        Assert.assertFalse(error.isPresent());

        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();

        MeshCutter.MeshPair meshes = MeshCutter2.cut(mesh, -3.8f, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.bottomMesh);
        Assert.assertNotNull(meshes.topMesh);
    }    
    
    
    
    @Test
    public void testEnricoSTLAt2() throws STLFileParsingException
    {

        URL stlURL = this.getClass().getResource("/enrico.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        Optional<MeshUtils.MeshError> error = MeshUtils.validate(mesh);
        Assert.assertFalse(error.isPresent());

        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();

        MeshCutter.MeshPair meshes = MeshCutter2.cut(mesh, -2f, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.bottomMesh);
        Assert.assertNotNull(meshes.topMesh);
    }    

    @Test
    public void testEnricoSTLAt15() throws STLFileParsingException
    {

        URL stlURL = this.getClass().getResource("/enrico.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        Optional<MeshUtils.MeshError> error = MeshUtils.validate(mesh);
        Assert.assertFalse(error.isPresent());

        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();

        MeshCutter.MeshPair meshes = MeshCutter2.cut(mesh, -1.5f, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.bottomMesh);
        Assert.assertNotNull(meshes.topMesh);
    }
    
    @Test
    public void testEnricoSTLAt3() throws STLFileParsingException
    {

        URL stlURL = this.getClass().getResource("/enrico.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        Optional<MeshUtils.MeshError> error = MeshUtils.validate(mesh);
        Assert.assertFalse(error.isPresent());

        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();

        MeshCutter.MeshPair meshes = MeshCutter2.cut(mesh, -3f, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.bottomMesh);
        Assert.assertNotNull(meshes.topMesh);
    }      
    
    @Test
    public void testEnricoFaceIntersectionsFace1600at3() throws STLFileParsingException
    {

        URL stlURL = this.getClass().getResource("/enrico.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        Optional<MeshUtils.MeshError> error = MeshUtils.validate(mesh);
        Assert.assertFalse(error.isPresent());

        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();

        MeshCutter.getFaceIntersections(1600, mesh, -3f, nullBedToLocalConverter);
    }      

    
}
