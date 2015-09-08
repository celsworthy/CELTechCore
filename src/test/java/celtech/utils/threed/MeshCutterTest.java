/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import celtech.utils.threed.MeshCutter.BedToLocalConverter;
import celtech.utils.threed.MeshCutter.MeshPair;
import static celtech.utils.threed.MeshCutter.getLoopsOfVertices;
import celtech.utils.threed.MeshUtils.MeshError;
import celtech.utils.threed.importers.stl.STLFileParsingException;
import celtech.utils.threed.importers.stl.STLImporter;
import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.Set;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
        
        MeshPair meshes = MeshCutter.cut(mesh, -7, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.bottomMesh);
        Assert.assertNotNull(meshes.topMesh);
    }
    
    @Test
    public void testCutCubeWithHoleReturnsTwoMeshes() throws STLFileParsingException
    {
        
        URL stlURL = this.getClass().getResource("/cubewithhole.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        Optional<MeshError> error = MeshUtils.validate(mesh);
        Assert.assertFalse(error.isPresent());
        
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
        
        MeshPair meshes = MeshCutter.cut(mesh, -15, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.bottomMesh);
        Assert.assertNotNull(meshes.topMesh);
    }
    
    @Test
    public void testCutCubeAlongMeshingLineReturnsTwoMeshes() throws STLFileParsingException
    {
        
        // this stl is meshed so that many vertices lie along Y=20
        URL stlURL = this.getClass().getResource("/onecubeabovetheother_remeshed.stl");
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
        
        MeshPair meshes = MeshCutter.cut(mesh, -20, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.bottomMesh);
        Assert.assertNotNull(meshes.topMesh);
    }
    
    @Test
    public void testMeshWithPointsOnCutPlane()
    {
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(0, 0, 0);
        mesh.getPoints().addAll(0, 0, 1);
        mesh.getPoints().addAll(1, 0, 1);
        mesh.getPoints().addAll(1, 0, 0);
        
        mesh.getPoints().addAll(0, 1, 0);
        mesh.getPoints().addAll(0, 1, 1);
        mesh.getPoints().addAll(1, 1, 1);
        mesh.getPoints().addAll(1, 1, 0);
        
        mesh.getPoints().addAll(0, 2, 0);
        mesh.getPoints().addAll(0, 2, 1);
        mesh.getPoints().addAll(1, 2, 1);
        mesh.getPoints().addAll(1, 2, 0);
        
        // one cube upon another
        mesh.getFaces().addAll(0, 0, 2, 0, 1, 0);
        mesh.getFaces().addAll(0, 0, 3, 0, 2, 0);
        
        mesh.getFaces().addAll(0, 0, 1, 0, 5, 0);
        mesh.getFaces().addAll(0, 0, 5, 0, 4, 0);
        mesh.getFaces().addAll(1, 0, 6, 0, 5, 0);
        mesh.getFaces().addAll(1, 0, 2, 0, 6, 0);
        mesh.getFaces().addAll(2, 0, 7, 0, 6, 0);
        mesh.getFaces().addAll(2, 0, 3, 0, 7, 0);
        mesh.getFaces().addAll(3, 0, 4, 0, 7, 0);
        mesh.getFaces().addAll(3, 0, 0, 0, 4, 0);
        
        mesh.getFaces().addAll(4, 0, 5, 0, 9, 0);
        mesh.getFaces().addAll(4, 0, 9, 0, 8, 0);
        mesh.getFaces().addAll(5, 0, 10, 0, 9, 0);
        mesh.getFaces().addAll(5, 0, 6, 0, 10, 0);
        mesh.getFaces().addAll(6, 0, 11, 0, 10, 0);
        mesh.getFaces().addAll(6, 0, 7, 0, 11, 0);
        mesh.getFaces().addAll(7, 0, 8, 0, 11, 0);
        mesh.getFaces().addAll(7, 0, 4, 0, 8, 0);
        
        mesh.getFaces().addAll(11, 0, 8, 0, 10, 0);
        mesh.getFaces().addAll(8, 0, 9, 0, 10, 0);
        
        Optional<MeshError> error = MeshUtils.validate(mesh);
        assertTrue(! error.isPresent());
        
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
        
        Set<MeshCutter.LoopOfVerticesAndCutFaces> cutFaces = getLoopsOfVertices(mesh, 1, nullBedToLocalConverter);
        System.out.println("loopsOfFaces" + cutFaces);
                
        MeshPair meshes = MeshCutter.cut(mesh, 1, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.bottomMesh);
        Assert.assertNotNull(meshes.topMesh);
        

    }    
    
    @Test
    public void testMeshWithNoPointsOnCutPlaneSimpleCube()
    {
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(0, 0, 0);
        mesh.getPoints().addAll(0, 0, 2);
        mesh.getPoints().addAll(2, 0, 2);
        mesh.getPoints().addAll(2, 0, 0);
        
        mesh.getPoints().addAll(0, 2, 0);
        mesh.getPoints().addAll(0, 2, 2);
        mesh.getPoints().addAll(2, 2, 2);
        mesh.getPoints().addAll(2, 2, 0);
        
        // one cube
        mesh.getFaces().addAll(0, 0, 2, 0, 1, 0);
        mesh.getFaces().addAll(0, 0, 3, 0, 2, 0);
        
        mesh.getFaces().addAll(0, 0, 1, 0, 5, 0);
        mesh.getFaces().addAll(0, 0, 5, 0, 4, 0);
        mesh.getFaces().addAll(1, 0, 6, 0, 5, 0);
        mesh.getFaces().addAll(1, 0, 2, 0, 6, 0);
        mesh.getFaces().addAll(2, 0, 7, 0, 6, 0);
        mesh.getFaces().addAll(2, 0, 3, 0, 7, 0);
        mesh.getFaces().addAll(3, 0, 4, 0, 7, 0);
        mesh.getFaces().addAll(3, 0, 0, 0, 4, 0);
        
        mesh.getFaces().addAll(7, 0, 4, 0, 5, 0);
        mesh.getFaces().addAll(7, 0, 5, 0, 6, 0);
        
        Optional<MeshError> error = MeshUtils.validate(mesh);
        assertTrue(! error.isPresent());
        
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
        
        float cutHeight = 1f;
        Set<MeshCutter.LoopOfVerticesAndCutFaces> cutFaces = getLoopsOfVertices(mesh, cutHeight, nullBedToLocalConverter);
        assertEquals(1, cutFaces.size());
                
        MeshPair meshes = MeshCutter.cut(mesh, cutHeight, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.bottomMesh);
        Assert.assertNotNull(meshes.topMesh);
        

    }   
    
    @Test
    public void testMeshWithTrianglesWithOneVertexOnPlane()
    {
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(0, 0, 0);
        mesh.getPoints().addAll(0, 0, 1);
        mesh.getPoints().addAll(1, 0, 1);
        mesh.getPoints().addAll(1, 0, 0);
        
        mesh.getPoints().addAll(0, 1, 0);
        mesh.getPoints().addAll(0, 1, 1);
        mesh.getPoints().addAll(1, 1, 1);
        mesh.getPoints().addAll(1, 1, 0);
        
        mesh.getPoints().addAll(0, 2, 0);
        mesh.getPoints().addAll(0, 2, 1);
        mesh.getPoints().addAll(1, 2, 1);
        mesh.getPoints().addAll(1, 2, 0);
        
        // double height parallelepiped
        mesh.getFaces().addAll(0, 0, 1, 0, 2, 0);
        mesh.getFaces().addAll(0, 0, 2, 0, 3, 0);
        
        mesh.getFaces().addAll(0, 0, 7, 0, 8, 0);
        mesh.getFaces().addAll(0, 0, 3, 0, 7, 0);
        mesh.getFaces().addAll(7, 0, 11, 0, 8, 0);
        
        mesh.getFaces().addAll(2, 0, 10, 0, 7, 0);
        mesh.getFaces().addAll(2, 0, 7, 0, 3, 0);
        mesh.getFaces().addAll(7, 0, 10, 0, 11, 0);
        
        mesh.getFaces().addAll(2, 0, 5, 0, 10, 0);
        mesh.getFaces().addAll(2, 0, 1, 0, 5, 0);
        mesh.getFaces().addAll(5, 0, 9, 0, 10, 0);
        
        mesh.getFaces().addAll(0, 0, 8, 0, 5, 0);
        mesh.getFaces().addAll(0, 0, 5, 0, 1, 0);
        mesh.getFaces().addAll(8, 0, 9, 0, 5, 0);
        
        mesh.getFaces().addAll(11, 0, 10, 0, 8, 0);
        mesh.getFaces().addAll(8, 0, 10, 0, 9, 0);
        
        Optional<MeshError> error = MeshUtils.validate(mesh);
        assertTrue(! error.isPresent());
        
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
        
        float cutHeight = 1f;
        Set<MeshCutter.LoopOfVerticesAndCutFaces> cutFaces = getLoopsOfVertices(mesh, cutHeight, nullBedToLocalConverter);
        assertEquals(1, cutFaces.size());
                
        MeshPair meshes = MeshCutter.cut(mesh, cutHeight, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.bottomMesh);
        Assert.assertNotNull(meshes.topMesh);
        

    }   
    
}
