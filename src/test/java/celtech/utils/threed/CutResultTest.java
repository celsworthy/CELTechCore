/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import java.util.HashSet;
import java.util.Set;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 *
 * @author tony
 */
public class CutResultTest
{

    MeshCutter.BedToLocalConverter nullConverter = new MeshCutter.BedToLocalConverter()
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

    TriangleMesh makeTriangleMesh()
    {
        float Y = 1;
        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getPoints().addAll(0, Y, 0);
        triangleMesh.getPoints().addAll(0, Y, 3);
        triangleMesh.getPoints().addAll(3, Y, 3);
        triangleMesh.getPoints().addAll(3, Y, 0);

        triangleMesh.getPoints().addAll(4, Y, 4);
        triangleMesh.getPoints().addAll(4, Y, 7);
        triangleMesh.getPoints().addAll(7, Y, 7);
        triangleMesh.getPoints().addAll(7, Y, 4);

        triangleMesh.getPoints().addAll(1, Y, 1);
        triangleMesh.getPoints().addAll(1, Y, 2);
        triangleMesh.getPoints().addAll(2, Y, 2);
        triangleMesh.getPoints().addAll(2, Y, 1);

        triangleMesh.getPoints().addAll(5, Y, 5);
        triangleMesh.getPoints().addAll(5, Y, 6);
        triangleMesh.getPoints().addAll(6, Y, 6);
        triangleMesh.getPoints().addAll(6, Y, 5);

        triangleMesh.getPoints().addAll(3, Y, 8);
        triangleMesh.getPoints().addAll(8, Y, 8);
        triangleMesh.getPoints().addAll(8, Y, 3);
        
        triangleMesh.getPoints().addAll(2, Y, 9);
        triangleMesh.getPoints().addAll(9, Y, 9);
        triangleMesh.getPoints().addAll(9, Y, 2);        

        return triangleMesh;
    }

    private PolygonIndices makeLoop0_3()
    {
        PolygonIndices outerLoop = new PolygonIndices();
        outerLoop.add(0);
        outerLoop.add(1);
        outerLoop.add(2);
        outerLoop.add(3);
        outerLoop.setName("0_3");
        return outerLoop;
    }

    private PolygonIndices makeLoop4_7()
    {
        PolygonIndices outerLoop = new PolygonIndices();
        outerLoop.add(4);
        outerLoop.add(5);
        outerLoop.add(6);
        outerLoop.add(7);
        outerLoop.setName("4_7");
        return outerLoop;
    }

    private PolygonIndices makeLoop1_2()
    {
        PolygonIndices outerLoop = new PolygonIndices();
        outerLoop.add(8);
        outerLoop.add(9);
        outerLoop.add(10);
        outerLoop.add(11);
        outerLoop.setName("1_2");
        return outerLoop;
    }

    private PolygonIndices makeLoop5_6()
    {
        PolygonIndices outerLoop = new PolygonIndices();
        outerLoop.add(12);
        outerLoop.add(13);
        outerLoop.add(14);
        outerLoop.add(15);
        outerLoop.setName("5_6");
        return outerLoop;
    }

    private PolygonIndices makeLoop3_8()
    {
        PolygonIndices outerLoop = new PolygonIndices();
        outerLoop.add(2);
        outerLoop.add(16);
        outerLoop.add(17);
        outerLoop.add(18);
        outerLoop.setName("3_8");
        return outerLoop;
    }
    
    private PolygonIndices makeLoop2_9()
    {
        PolygonIndices outerLoop = new PolygonIndices();
        outerLoop.add(10);
        outerLoop.add(19);
        outerLoop.add(20);
        outerLoop.add(21);
        outerLoop.setName("2_9");
        return outerLoop;
    }    

    @Test
    public void testGetNestedPolygonSetsSingleLoop()
    {
        TriangleMesh triangleMesh = makeTriangleMesh();
        PolygonIndices outerLoop = makeLoop0_3();
        
        MeshCutter.LoopOfFacesAndVertices cutFaces = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop);
        Set<MeshCutter.LoopOfFacesAndVertices> loopsOfFacesAndVertices = new HashSet<>();
        loopsOfFacesAndVertices.add(cutFaces);
        
        CutResult cutResult = new CutResult(triangleMesh, loopsOfFacesAndVertices, nullConverter,
                                            MeshCutter.TopBottom.BOTTOM);
        Set<LoopSet> loopSets = cutResult.identifyOuterLoopsAndInnerLoops();
        assertEquals(1, loopSets.size());
    }

    @Test
    public void testGetNestedPolygonSetsTwoOuterLoops()
    {
        TriangleMesh triangleMesh = makeTriangleMesh();

        PolygonIndices outerLoop1 = makeLoop0_3();
        PolygonIndices outerLoop2 = makeLoop4_7();
        
        MeshCutter.LoopOfFacesAndVertices cutFaces = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop1);
        MeshCutter.LoopOfFacesAndVertices cutFaces2 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop2);
        Set<MeshCutter.LoopOfFacesAndVertices> loopsOfFacesAndVertices = new HashSet<>();
        loopsOfFacesAndVertices.add(cutFaces);  
        loopsOfFacesAndVertices.add(cutFaces2);
        
        CutResult cutResult = new CutResult(triangleMesh, loopsOfFacesAndVertices, nullConverter,
                                            MeshCutter.TopBottom.BOTTOM);
        Set<LoopSet> nestedPolygonSets = cutResult.identifyOuterLoopsAndInnerLoops();
        assertEquals(2, nestedPolygonSets.size());
    }

    @Test
    public void testGetNestedPolygonSetsOuterLoopWithInner()
    {
        TriangleMesh triangleMesh = makeTriangleMesh();

        PolygonIndices outerLoop1 = makeLoop0_3();
        PolygonIndices outerLoop3 = makeLoop1_2();
        
        MeshCutter.LoopOfFacesAndVertices cutFaces = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop1);
        MeshCutter.LoopOfFacesAndVertices cutFaces2 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop3);
        Set<MeshCutter.LoopOfFacesAndVertices> loopsOfFacesAndVertices = new HashSet<>();
        loopsOfFacesAndVertices.add(cutFaces);  
        loopsOfFacesAndVertices.add(cutFaces2);        
        
        CutResult cutResult = new CutResult(triangleMesh, loopsOfFacesAndVertices, nullConverter,
                                            MeshCutter.TopBottom.BOTTOM);
        Set<LoopSet> nestedLoopSets = cutResult.identifyOuterLoopsAndInnerLoops();
        assertEquals(1, nestedLoopSets.size());
        LoopSet nestedLoopSet = nestedLoopSets.iterator().next();
        assertEquals(1, nestedLoopSet.innerLoopSets.size());
    }

    @Test
    public void testIdentifyOuterLoopsAndInnerLoopsOuterLoopWithInner()
    {
        TriangleMesh triangleMesh = makeTriangleMesh();

        PolygonIndices outerLoop1 = makeLoop0_3();
        PolygonIndices outerLoop3 = makeLoop1_2();
        
        MeshCutter.LoopOfFacesAndVertices cutFaces = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop1);
        MeshCutter.LoopOfFacesAndVertices cutFaces2 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop3);
        Set<MeshCutter.LoopOfFacesAndVertices> loopsOfFacesAndVertices = new HashSet<>();
        loopsOfFacesAndVertices.add(cutFaces);  
        loopsOfFacesAndVertices.add(cutFaces2);        
        
        CutResult cutResult = new CutResult(triangleMesh, loopsOfFacesAndVertices, nullConverter,
                                            MeshCutter.TopBottom.BOTTOM);
        Set<LoopSet> nestedPolygonSets = cutResult.identifyOuterLoopsAndInnerLoops();
        assertEquals(1, nestedPolygonSets.size());
        LoopSet loopSet = nestedPolygonSets.iterator().next();
        assertEquals(1, loopSet.innerLoopSets.size());
    }

    @Test
    public void testGetRegionsforSingleLoopHasOneRegionWithNoHoles()
    {
        TriangleMesh triangleMesh = makeTriangleMesh();
        PolygonIndices outerLoop = makeLoop0_3();
        
        MeshCutter.LoopOfFacesAndVertices cutFaces = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop);
        Set<MeshCutter.LoopOfFacesAndVertices> loopsOfFacesAndVertices = new HashSet<>();
        loopsOfFacesAndVertices.add(cutFaces);  
        
        CutResult cutResult = new CutResult(triangleMesh, loopsOfFacesAndVertices, nullConverter,
                                            MeshCutter.TopBottom.BOTTOM);
        Set<LoopSet> loopSets = cutResult.identifyOuterLoopsAndInnerLoops();
        assertEquals(1, loopSets.size());
        Set<Region> regions = loopSets.iterator().next().getRegions();
        assertEquals(1, regions.size());
        Region region = regions.iterator().next();
        assertEquals(0, region.innerLoops.size());
    }

    @Test
    public void testGetRegionsforOneOuterLoopWithOneInnerLoopHasOneRegionWithOneHole()
    {
        TriangleMesh triangleMesh = makeTriangleMesh();

        PolygonIndices outerLoop1 = makeLoop0_3();
        PolygonIndices outerLoop3 = makeLoop1_2();
        
        MeshCutter.LoopOfFacesAndVertices cutFaces = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop1);
        MeshCutter.LoopOfFacesAndVertices cutFaces2 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop3);
        Set<MeshCutter.LoopOfFacesAndVertices> loopsOfFacesAndVertices = new HashSet<>();
        loopsOfFacesAndVertices.add(cutFaces);  
        loopsOfFacesAndVertices.add(cutFaces2);  
        
        CutResult cutResult = new CutResult(triangleMesh, loopsOfFacesAndVertices, nullConverter,
                                            MeshCutter.TopBottom.BOTTOM);
        Set<LoopSet> loopSets = cutResult.identifyOuterLoopsAndInnerLoops();
        assertEquals(1, loopSets.size());
        LoopSet loopSet = loopSets.iterator().next();
        Set<Region> regions = loopSet.getRegions();
        assertEquals(1, regions.size());
        Region region = regions.iterator().next();
        assertEquals(1, region.innerLoops.size());
    }

    @Test
    public void testGetRegionsforTwoOuterLoopsHasTwoRegionsWithNoHole()
    {
        TriangleMesh triangleMesh = makeTriangleMesh();

        PolygonIndices outerLoop1 = makeLoop0_3();
        PolygonIndices outerLoop2 = makeLoop4_7();
        
        MeshCutter.LoopOfFacesAndVertices cutFaces = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop1);
        MeshCutter.LoopOfFacesAndVertices cutFaces2 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop2);
        Set<MeshCutter.LoopOfFacesAndVertices> loopsOfFacesAndVertices = new HashSet<>();
        loopsOfFacesAndVertices.add(cutFaces);  
        loopsOfFacesAndVertices.add(cutFaces2);          
        
        CutResult cutResult = new CutResult(triangleMesh, loopsOfFacesAndVertices, nullConverter,
                                            MeshCutter.TopBottom.BOTTOM);
        Set<LoopSet> loopSets = cutResult.identifyOuterLoopsAndInnerLoops();
        assertEquals(2, loopSets.size());
        for (LoopSet loopSet : loopSets)
        {
            Set<Region> regions = loopSet.getRegions();
            assertEquals(1, regions.size());
            Region region = regions.iterator().next();
            assertEquals(0, region.innerLoops.size());
        }

    }

    @Test
    public void testGetRegionsforTwoOuterLoopsEachWithInnerLoopHasTwoRegionsWithOneHole()
    {
        TriangleMesh triangleMesh = makeTriangleMesh();

        PolygonIndices outerLoop1 = makeLoop0_3();
        PolygonIndices outerLoop2 = makeLoop4_7();
        PolygonIndices outerLoop3 = makeLoop1_2();
        PolygonIndices outerLoop4 = makeLoop5_6();
        
        MeshCutter.LoopOfFacesAndVertices cutFaces = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop1);
        MeshCutter.LoopOfFacesAndVertices cutFaces2 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop2);
        MeshCutter.LoopOfFacesAndVertices cutFaces3 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop3);
        MeshCutter.LoopOfFacesAndVertices cutFaces4 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop4);
        Set<MeshCutter.LoopOfFacesAndVertices> loopsOfFacesAndVertices = new HashSet<>();
        loopsOfFacesAndVertices.add(cutFaces);  
        loopsOfFacesAndVertices.add(cutFaces2);          
        loopsOfFacesAndVertices.add(cutFaces3); 
        loopsOfFacesAndVertices.add(cutFaces4); 
        
        CutResult cutResult = new CutResult(triangleMesh, loopsOfFacesAndVertices, nullConverter,
                                            MeshCutter.TopBottom.BOTTOM);
        Set<LoopSet> loopSets = cutResult.identifyOuterLoopsAndInnerLoops();
        assertEquals(2, loopSets.size());
        for (LoopSet loopSet : loopSets)
        {
            Set<Region> regions = loopSet.getRegions();
            assertEquals(1, regions.size());
            Region region = regions.iterator().next();
            assertEquals(1, region.innerLoops.size());
        }

    }

    @Test
    public void testGetRegionsforOneOuterLoopsWithInnerLoopWhichHasInnerLoop()
    {
        TriangleMesh triangleMesh = makeTriangleMesh();

        PolygonIndices outerLoop2 = makeLoop4_7();
        PolygonIndices outerLoop4 = makeLoop5_6();
        PolygonIndices outerLoop5 = makeLoop3_8();
        
        MeshCutter.LoopOfFacesAndVertices cutFaces2 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop2);
        MeshCutter.LoopOfFacesAndVertices cutFaces4 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop4);
        MeshCutter.LoopOfFacesAndVertices cutFaces5 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop5);
        Set<MeshCutter.LoopOfFacesAndVertices> loopsOfFacesAndVertices = new HashSet<>();
        loopsOfFacesAndVertices.add(cutFaces2);          
        loopsOfFacesAndVertices.add(cutFaces4); 
        loopsOfFacesAndVertices.add(cutFaces5);         
        
        CutResult cutResult = new CutResult(triangleMesh, loopsOfFacesAndVertices, nullConverter,
                                            MeshCutter.TopBottom.BOTTOM);
        Set<LoopSet> loopSets = cutResult.identifyOuterLoopsAndInnerLoops();
        assertEquals(1, loopSets.size());
        LoopSet loopSet = loopSets.iterator().next();
        Set<Region> regions = loopSet.getRegions();
        assertEquals(2, regions.size());
    }
    
    @Test
    public void testGetRegionsforOneOuterLoopWithInnerLoopWhichHasInnerLoopThatAlsoHasInnerLoop()
    {
        TriangleMesh triangleMesh = makeTriangleMesh();

        PolygonIndices outerLoop2 = makeLoop4_7();
        PolygonIndices outerLoop4 = makeLoop5_6();
        PolygonIndices outerLoop5 = makeLoop3_8();
        PolygonIndices outerLoop6 = makeLoop2_9();
        
        MeshCutter.LoopOfFacesAndVertices cutFaces2 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop2);
        MeshCutter.LoopOfFacesAndVertices cutFaces4 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop4);
        MeshCutter.LoopOfFacesAndVertices cutFaces5 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop5);
        MeshCutter.LoopOfFacesAndVertices cutFaces6 = new MeshCutter.LoopOfFacesAndVertices(null, outerLoop6);
        Set<MeshCutter.LoopOfFacesAndVertices> loopsOfFacesAndVertices = new HashSet<>();
        loopsOfFacesAndVertices.add(cutFaces2);          
        loopsOfFacesAndVertices.add(cutFaces4); 
        loopsOfFacesAndVertices.add(cutFaces5);             
        loopsOfFacesAndVertices.add(cutFaces6); 
        
        CutResult cutResult = new CutResult(triangleMesh, loopsOfFacesAndVertices, nullConverter,
                                            MeshCutter.TopBottom.BOTTOM);
        Set<LoopSet> loopSets = cutResult.identifyOuterLoopsAndInnerLoops();
        assertEquals(1, loopSets.size());
        LoopSet loopSet = loopSets.iterator().next();
        Set<Region> regions = loopSet.getRegions();

        for (Region region : regions)
        {
            System.out.println("region outer:" + region.outerLoop);
            System.out.println("region inner:" + region.innerLoops);
            if (region.outerLoop.name.equals("3_8")) {
                assertEquals(1, region.innerLoops.size());
                assertEquals("4_7", region.innerLoops.iterator().next().name);
            }
            if (region.outerLoop.name.equals("4_7")) {
                assertEquals(1, region.innerLoops.size());
                assertEquals("5_6", region.innerLoops.iterator().next().name);
            }
        }
        
        assertEquals(2, regions.size());

    }
    
}
