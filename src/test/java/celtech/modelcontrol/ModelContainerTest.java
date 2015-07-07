/*
 * Copyright 2015 CEL UK
 */
package celtech.modelcontrol;

import celtech.JavaFXConfiguredTest;
import celtech.TestUtils;
import celtech.coreUI.visualisation.metaparts.ModelLoadResult;
import celtech.coreUI.visualisation.modelDisplay.ModelBounds;
import celtech.services.modelLoader.ModelLoadResults;
import celtech.services.modelLoader.ModelLoaderTask;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class ModelContainerTest extends JavaFXConfiguredTest
{

    private static int BED_CENTRE_X = 105;
    private static int BED_CENTRE_Z = 75;

    private ModelContainer loadSTL(String stlLocation) throws InterruptedException, ExecutionException
    {
        List<File> modelFiles = new ArrayList<>();
        URL statisticsFile = this.getClass().getResource(stlLocation);
        modelFiles.add(new File(statisticsFile.getFile()));
        ModelLoaderTask modelLoaderTask = new ModelLoaderTask(modelFiles, null, true);
        Thread th = new Thread(modelLoaderTask);
        th.setDaemon(true);
        th.start();
        ModelLoadResults modelLoadResults = modelLoaderTask.get();
        ModelLoadResult modelLoadResult = modelLoadResults.getResults().get(0);
        ModelContainer modelContainer = modelLoadResult.getModelContainer();
        return modelContainer;
    }

    @Test
    public void testCalculateBounds()
    {
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        ModelBounds bounds = mc.calculateBounds();
        assertEquals(-1, bounds.getMinX(), 0);
        assertEquals(1, bounds.getMaxX(), 0);
        assertEquals(-1.5, bounds.getMinY(), 0);
        assertEquals(1.5, bounds.getMaxY(), 0);
        assertEquals(0, bounds.getMinZ(), 0);
        assertEquals(0, bounds.getMaxZ(), 0);
    }

    @Test
    public void testCalculateBoundsInParentWithNoTransforms()
    {
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        ModelBounds bounds = mc.calculateBoundsInBedCoordinateSystem();
        assertEquals(BED_CENTRE_X - 1, bounds.getMinX(), 0);
        assertEquals(BED_CENTRE_X + 1, bounds.getMaxX(), 0);
        assertEquals(-3.0, bounds.getMinY(), 0);
        assertEquals(0, bounds.getMaxY(), 0);
        assertEquals(BED_CENTRE_Z + 0, bounds.getMinZ(), 0);
        assertEquals(BED_CENTRE_Z + 0, bounds.getMaxZ(), 0);
    }

    @Test
    public void testCalculateBoundsInParentWithUniformScale()
    {
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        mc.setXScale(2.0);
        mc.setYScale(2.0);
        mc.setZScale(2.0);
        ModelBounds bounds = mc.calculateBoundsInBedCoordinateSystem();
        assertEquals(BED_CENTRE_X - (1 * 2), bounds.getMinX(), 0);
        assertEquals(BED_CENTRE_X + (1 * 2), bounds.getMaxX(), 0);
        assertEquals(-3.0 * 2, bounds.getMinY(), 0);
        assertEquals(0, bounds.getMaxY(), 0);
        assertEquals(BED_CENTRE_Z + 0, bounds.getMinZ(), 0);
        assertEquals(BED_CENTRE_Z + 0, bounds.getMaxZ(), 0);
    }
    
    @Test
    public void testCalculateBoundsInParentWithTranslateX()
    {
        int TRANSLATE_X = 10;
        int TRANSLATE_Z = 5;
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        mc.translateBy(TRANSLATE_X, TRANSLATE_Z);
        ModelBounds bounds = mc.calculateBoundsInBedCoordinateSystem();
        assertEquals(BED_CENTRE_X - 1 + TRANSLATE_X, bounds.getMinX(), 0);
        assertEquals(BED_CENTRE_X + 1 + TRANSLATE_X, bounds.getMaxX(), 0);
        assertEquals(-3.0, bounds.getMinY(), 0);
        assertEquals(0, bounds.getMaxY(), 0);
        assertEquals(BED_CENTRE_Z + 0 + TRANSLATE_Z, bounds.getMinZ(), 0);
        assertEquals(BED_CENTRE_Z + 0 + TRANSLATE_Z, bounds.getMaxZ(), 0);
    }
    
    @Test
    public void testCalculateBoundsInParentWithUniformScaleAndTranslate()
    {
        int TRANSLATE_X = 10;
        int TRANSLATE_Z = 5;
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        mc.setXScale(2.0);
        mc.setYScale(2.0);
        mc.setZScale(2.0);
        mc.translateBy(TRANSLATE_X, TRANSLATE_Z);
        ModelBounds bounds = mc.calculateBoundsInBedCoordinateSystem();
        assertEquals(BED_CENTRE_X - (1 * 2) + TRANSLATE_X, bounds.getMinX(), 0);
        assertEquals(BED_CENTRE_X + (1 * 2) + TRANSLATE_X, bounds.getMaxX(), 0);
        assertEquals(-3.0 * 2, bounds.getMinY(), 0);
        assertEquals(0, bounds.getMaxY(), 0);
        assertEquals(BED_CENTRE_Z + 0 + TRANSLATE_Z, bounds.getMinZ(), 0);
        assertEquals(BED_CENTRE_Z + 0 + TRANSLATE_Z, bounds.getMaxZ(), 0);
    }    
    
    @Test
    public void testCalculateBoundsInParentInGroupWithNoTransforms()
    {
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        
        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(mc);
        ModelContainer groupModelContainer = new ModelContainer(modelContainers);
        ModelBounds bounds = groupModelContainer.calculateBoundsInBedCoordinateSystem();
        assertEquals(BED_CENTRE_X - 1, bounds.getMinX(), 0);
        assertEquals(BED_CENTRE_X + 1, bounds.getMaxX(), 0);
        assertEquals(-3.0, bounds.getMinY(), 0);
        assertEquals(0, bounds.getMaxY(), 0);
        assertEquals(BED_CENTRE_Z + 0, bounds.getMinZ(), 0);
        assertEquals(BED_CENTRE_Z + 0, bounds.getMaxZ(), 0);
    }

}
