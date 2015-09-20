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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.scene.shape.TriangleMesh;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class MeshCutter2Test {

    @Test
    public void testCutSimpleCube() {
        TriangleMesh mesh = TriangleCutterTest.createSimpleCube();
        assertEquals(12, mesh.getFaces().size() / 6);

        float cutHeight = 1f;
        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();

        List<TriangleMesh> meshes = MeshCutter2.cut(mesh, cutHeight, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.get(0));
        Assert.assertNotNull(meshes.get(1));

    }

    @Test
    public void testMakeSplitMeshSimpleCubeBottom() {
        TriangleMesh mesh = TriangleCutterTest.createSimpleCube();
        assertEquals(12, mesh.getFaces().size() / 6);

        float cutHeight = 1f;
        TriangleMesh childMesh = makeSplitMesh(mesh, cutHeight,
                TriangleCutterTest.makeNullConverter(),
                MeshCutter.TopBottom.BOTTOM);

        assertEquals(14, childMesh.getFaces().size() / 6);
    }

    @Test
    public void testMakeSplitMeshSimpleCubeTop() {
        TriangleMesh mesh = TriangleCutterTest.createSimpleCube();
        assertEquals(12, mesh.getFaces().size() / 6);

        float cutHeight = 1f;
        TriangleMesh childMesh = makeSplitMesh(mesh, cutHeight,
                TriangleCutterTest.makeNullConverter(),
                MeshCutter.TopBottom.TOP);

        assertEquals(14, childMesh.getFaces().size() / 6);
    }

    @Test
    public void testEnricoSTLAt1() throws STLFileParsingException {

        URL stlURL = this.getClass().getResource("/enrico.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        Optional<MeshUtils.MeshError> error = MeshUtils.validate(mesh);
        Assert.assertFalse(error.isPresent());

        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();

        List<TriangleMesh> meshes = MeshCutter2.cut(mesh, -1f, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.get(0));
        Assert.assertNotNull(meshes.get(1));
    }

    @Test
    public void testEnricoSTLAt3p8() throws STLFileParsingException {

        URL stlURL = this.getClass().getResource("/enrico.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        Optional<MeshUtils.MeshError> error = MeshUtils.validate(mesh);
        Assert.assertFalse(error.isPresent());

        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();

        List<TriangleMesh> meshes = MeshCutter2.cut(mesh, -3.8f, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.get(0));
        Assert.assertNotNull(meshes.get(1));
    }

    @Test
    public void testEnricoSTLAt2() throws STLFileParsingException {

        URL stlURL = this.getClass().getResource("/enrico.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        Optional<MeshUtils.MeshError> error = MeshUtils.validate(mesh);
        Assert.assertFalse(error.isPresent());

        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();

        List<TriangleMesh> meshes = MeshCutter2.cut(mesh, -2f, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.get(0));
        Assert.assertNotNull(meshes.get(1));
    }

    @Test
    public void testEnricoSTLAt15() throws STLFileParsingException {

        URL stlURL = this.getClass().getResource("/enrico.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        Optional<MeshUtils.MeshError> error = MeshUtils.validate(mesh);
        Assert.assertFalse(error.isPresent());

        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();

        List<TriangleMesh> meshes = MeshCutter2.cut(mesh, -1.5f, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.get(0));
        Assert.assertNotNull(meshes.get(1));
    }

    @Test
    public void testEnricoSTLAt3() throws STLFileParsingException {

        URL stlURL = this.getClass().getResource("/enrico.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        Optional<MeshUtils.MeshError> error = MeshUtils.validate(mesh);
        Assert.assertFalse(error.isPresent());

        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();

        List<TriangleMesh> meshes = MeshCutter2.cut(mesh, -3f, nullBedToLocalConverter);
        Assert.assertNotNull(meshes.get(0));
        Assert.assertNotNull(meshes.get(1));
    }

    @Test
    public void testEnricoFaceIntersectionsFace1600at3() throws STLFileParsingException {

        URL stlURL = this.getClass().getResource("/enrico.stl");
        File singleObjectSTLFile = new File(stlURL.getFile());
        TriangleMesh mesh = new STLImporter().processBinarySTLData(singleObjectSTLFile);
        Optional<MeshUtils.MeshError> error = MeshUtils.validate(mesh);
        Assert.assertFalse(error.isPresent());

        MeshCutter.BedToLocalConverter nullBedToLocalConverter = makeNullConverter();

        MeshCutter.getFaceIntersections(1600, mesh, -3f, nullBedToLocalConverter);
    }

    @Test
    public void testConvertEdgesToPolygonIndices() {
        
        ManifoldEdge edge0 = new ManifoldEdge(9, 8, null, null); 
        ManifoldEdge edge1 = new ManifoldEdge(10, 9, null, null); 
        ManifoldEdge edge2 = new ManifoldEdge(11, 10, null, null); 
        ManifoldEdge edge3 = new ManifoldEdge(11, 12, null, null); 
        ManifoldEdge edge4 = new ManifoldEdge(12, 13, null, null); 
        ManifoldEdge edge5 = new ManifoldEdge(14, 13, null, null); 
        ManifoldEdge edge6 = new ManifoldEdge(15, 14, null, null); 
        ManifoldEdge edge7 = new ManifoldEdge(15, 16, null, null); 
        List<ManifoldEdge> loop = new ArrayList<>();
        loop.add(edge0);
        loop.add(edge1);
        loop.add(edge2);
        loop.add(edge3);
        loop.add(edge4);
        loop.add(edge5);
        loop.add(edge6);
        loop.add(edge7);
        
        PolygonIndices polygonIndices = MeshCutter2.convertEdgesToPolygonIndices(loop);
        System.out.println(polygonIndices) ;
        PolygonIndices expectedPolygonIndices = new PolygonIndices();
        expectedPolygonIndices.add(8);
        expectedPolygonIndices.add(9);
        expectedPolygonIndices.add(10);
        expectedPolygonIndices.add(11);
        expectedPolygonIndices.add(12);
        expectedPolygonIndices.add(13);
        expectedPolygonIndices.add(14);
        expectedPolygonIndices.add(15);
        expectedPolygonIndices.add(16);
        
        assertEquals(expectedPolygonIndices, polygonIndices);
        
                
    }

}
