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
import javafx.scene.shape.MeshView;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class ModelContainerTest extends JavaFXConfiguredTest
{

    private static final int BED_CENTRE_X = 105;
    private static final int BED_CENTRE_Z = 75;

    private ModelContainer loadSTL(String stlLocation) throws InterruptedException, ExecutionException
    {
        List<File> modelFiles = new ArrayList<>();
        URL statisticsFile = this.getClass().getResource(stlLocation);
        modelFiles.add(new File(statisticsFile.getFile()));
        ModelLoaderTask modelLoaderTask = new ModelLoaderTask(modelFiles);
        Thread th = new Thread(modelLoaderTask);
        th.setDaemon(true);
        th.start();
        ModelLoadResults modelLoadResults = modelLoaderTask.get();
        ModelLoadResult modelLoadResult = modelLoadResults.getResults().get(0);
        Set<ModelContainer> modelContainers = modelLoadResult.getModelContainers();
        return modelContainers.iterator().next();
    }

    @Test
    public void testGetRootModelContainerNoGroup()
    {
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        MeshView meshView = mc.getMeshView();
        assertEquals(mc, ModelContainer.getRootModelContainer(meshView));
    }

    @Test
    public void testGetRootModelContainerInGroup()
    {
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);

        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(mc);
        ModelContainer groupModelContainer = new ModelGroup(modelContainers);

        MeshView meshView = mc.getMeshView();

        assertEquals(groupModelContainer, ModelContainer.getRootModelContainer(meshView));
    }

    @Test
    public void testCalculateBounds()
    {
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        ModelBounds bounds = mc.calculateBoundsInLocal();
        assertEquals(-1, bounds.getMinX(), 0);
        assertEquals(1, bounds.getMaxX(), 0);
        assertEquals(-1.5, bounds.getMinY(), 0);
        assertEquals(1.5, bounds.getMaxY(), 0);
        assertEquals(0, bounds.getMinZ(), 0);
        assertEquals(0, bounds.getMaxZ(), 0);
    }

    @Test
    public void testCalculateBoundsInBedWithNoTransforms()
    {
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        mc.moveToCentre();
        ModelBounds bounds = mc.calculateBoundsInBedCoordinateSystem();
        assertEquals(BED_CENTRE_X - 1, bounds.getMinX(), 0);
        assertEquals(BED_CENTRE_X + 1, bounds.getMaxX(), 0);
        assertEquals(-3.0, bounds.getMinY(), 0);
        assertEquals(0, bounds.getMaxY(), 0);
        assertEquals(BED_CENTRE_Z + 0, bounds.getMinZ(), 0);
        assertEquals(BED_CENTRE_Z + 0, bounds.getMaxZ(), 0);
    }

    @Test
    public void testCalculateBoundsInBedWithUniformScale()
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
    public void testCalculateBoundsInBedWithTranslateX()
    {
        int TRANSLATE_X = 10;
        int TRANSLATE_Z = 5;
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        mc.moveToCentre();
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
    public void testCalculateBoundsInBedWithUniformScaleAndTranslate()
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
    public void testCalculateBoundsInBedInGroupWithNoTransforms()
    {
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        mc.moveToCentre();

        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(mc);
        ModelContainer groupModelContainer = new ModelGroup(modelContainers);
        ModelBounds bounds = groupModelContainer.calculateBoundsInBedCoordinateSystem();
        assertEquals(BED_CENTRE_X - 1, bounds.getMinX(), 0);
        assertEquals(BED_CENTRE_X + 1, bounds.getMaxX(), 0);
        assertEquals(-3.0, bounds.getMinY(), 0);
        assertEquals(0, bounds.getMaxY(), 0);
        assertEquals(BED_CENTRE_Z + 0, bounds.getMinZ(), 0);
        assertEquals(BED_CENTRE_Z + 0, bounds.getMaxZ(), 0);
    }

    @Test
    public void testCalculateBoundsInBedInGroupWithScaleInGroup()
    {
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);

        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(mc);
        ModelContainer groupModelContainer = new ModelGroup(modelContainers);
        groupModelContainer.setId("mc group");
        groupModelContainer.setXScale(2.0);
        groupModelContainer.setYScale(2.0);
        groupModelContainer.setZScale(2.0);

        mc.printTransforms();
        groupModelContainer.printTransforms();
        ModelBounds bounds = groupModelContainer.calculateBoundsInBedCoordinateSystem();

        assertEquals(2 * 2, bounds.getWidth(), 0);
        assertEquals(2 * 3, bounds.getHeight(), 0);

        assertEquals(BED_CENTRE_X - (1 * 2), bounds.getMinX(), 0);
        assertEquals(BED_CENTRE_X + (1 * 2), bounds.getMaxX(), 0);
        assertEquals(-3.0 * 2, bounds.getMinY(), 0);
        assertEquals(0, bounds.getMaxY(), 0);
        assertEquals(BED_CENTRE_Z + 0, bounds.getMinZ(), 0);
        assertEquals(BED_CENTRE_Z + 0, bounds.getMaxZ(), 0);
    }

    @Test
    public void testCalculateBoundsInBedInGroupWithTranslateInGroup()
    {
        int TRANSLATE_X = 10;
        int TRANSLATE_Z = 5;
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        mc.moveToCentre();

        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(mc);
        ModelContainer groupModelContainer = new ModelGroup(modelContainers);
        groupModelContainer.setId("mc group");
        groupModelContainer.translateBy(TRANSLATE_X, TRANSLATE_Z);
        mc.printTransforms();
        groupModelContainer.printTransforms();
        ModelBounds bounds = groupModelContainer.calculateBoundsInBedCoordinateSystem();

        assertEquals(BED_CENTRE_X - 1 + TRANSLATE_X, bounds.getMinX(), 0);
        assertEquals(BED_CENTRE_X + 1 + TRANSLATE_X, bounds.getMaxX(), 0);
        assertEquals(-3.0, bounds.getMinY(), 0);
        assertEquals(0, bounds.getMaxY(), 0);
        assertEquals(BED_CENTRE_Z + 0 + TRANSLATE_Z, bounds.getMinZ(), 0);
        assertEquals(BED_CENTRE_Z + 0 + TRANSLATE_Z, bounds.getMaxZ(), 0);
    }

    @Test
    public void testCalculateBoundsInBedInGroupWithTranslateInModelAndGroup()
    {
        int TRANSLATE_X = 10;
        int TRANSLATE_Z = 5;
        int TRANSLATE_X_GROUP = 7;
        int TRANSLATE_Z_GROUP = 8;
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        mc.moveToCentre();

        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(mc);
        mc.translateBy(TRANSLATE_X, TRANSLATE_Z);
        ModelContainer groupModelContainer = new ModelGroup(modelContainers);
        groupModelContainer.setId("mc group");
        groupModelContainer.translateBy(TRANSLATE_X_GROUP, TRANSLATE_Z_GROUP);
        mc.printTransforms();
        groupModelContainer.printTransforms();
        ModelBounds bounds = groupModelContainer.calculateBoundsInBedCoordinateSystem();

        assertEquals(BED_CENTRE_X - 1 + TRANSLATE_X + TRANSLATE_X_GROUP, bounds.getMinX(), 0);
        assertEquals(BED_CENTRE_X + 1 + TRANSLATE_X + TRANSLATE_X_GROUP, bounds.getMaxX(), 0);
        assertEquals(-3.0, bounds.getMinY(), 0);
        assertEquals(0, bounds.getMaxY(), 0);
        assertEquals(BED_CENTRE_Z + 0 + TRANSLATE_Z + TRANSLATE_Z_GROUP, bounds.getMinZ(), 0);
        assertEquals(BED_CENTRE_Z + 0 + TRANSLATE_Z + TRANSLATE_Z_GROUP, bounds.getMaxZ(), 0);
    }

    @Test
    public void testCalculateBoundsInBedInGroupInGroupWithNoTransforms()
    {
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        mc.moveToCentre();

        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(mc);
        ModelContainer groupModelContainer = new ModelGroup(modelContainers);
        Set<ModelContainer> modelContainers2 = new HashSet<>();
        modelContainers2.add(groupModelContainer);
        ModelContainer groupGroupModelContainer = new ModelGroup(modelContainers2);

        ModelBounds bounds = groupGroupModelContainer.calculateBoundsInBedCoordinateSystem();
        assertEquals(BED_CENTRE_X - 1, bounds.getMinX(), 0);
        assertEquals(BED_CENTRE_X + 1, bounds.getMaxX(), 0);
        assertEquals(-3.0, bounds.getMinY(), 0);
        assertEquals(0, bounds.getMaxY(), 0);
        assertEquals(BED_CENTRE_Z + 0, bounds.getMinZ(), 0);
        assertEquals(BED_CENTRE_Z + 0, bounds.getMaxZ(), 0);
    }

    @Test
    public void testCalculateBoundsInBedInGroupWithTranslateInModelAndGroupAndGroup()
    {
        int TRANSLATE_X = 10;
        int TRANSLATE_Z = 5;
        int TRANSLATE_X_GROUP = 7;
        int TRANSLATE_Z_GROUP = 8;
        int TRANSLATE_X_GROUP_GROUP = 20;
        int TRANSLATE_Z_GROUP_GROUP = 21;
        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        mc.moveToCentre();

        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(mc);
        mc.translateBy(TRANSLATE_X, TRANSLATE_Z);
        ModelContainer groupModelContainer = new ModelGroup(modelContainers);
        groupModelContainer.setId("mc group");
        groupModelContainer.translateBy(TRANSLATE_X_GROUP, TRANSLATE_Z_GROUP);

        Set<ModelContainer> modelContainers2 = new HashSet<>();
        modelContainers2.add(groupModelContainer);
        ModelContainer groupGroupModelContainer = new ModelGroup(modelContainers2);
        groupGroupModelContainer.translateBy(TRANSLATE_X_GROUP_GROUP, TRANSLATE_Z_GROUP_GROUP);

        mc.printTransforms();
        groupModelContainer.printTransforms();
        groupGroupModelContainer.printTransforms();
        ModelBounds bounds = groupGroupModelContainer.calculateBoundsInBedCoordinateSystem();

        assertEquals(BED_CENTRE_X - 1 + TRANSLATE_X + TRANSLATE_X_GROUP + TRANSLATE_X_GROUP_GROUP,
                     bounds.getMinX(), 0);
        assertEquals(BED_CENTRE_X + 1 + TRANSLATE_X + TRANSLATE_X_GROUP + TRANSLATE_X_GROUP_GROUP,
                     bounds.getMaxX(), 0);
        assertEquals(-3.0, bounds.getMinY(), 0);
        assertEquals(0, bounds.getMaxY(), 0);
        assertEquals(BED_CENTRE_Z + 0 + TRANSLATE_Z + TRANSLATE_Z_GROUP + TRANSLATE_Z_GROUP_GROUP,
                     bounds.getMinZ(), 0);
        assertEquals(BED_CENTRE_Z + 0 + TRANSLATE_Z + TRANSLATE_Z_GROUP + TRANSLATE_Z_GROUP_GROUP,
                     bounds.getMaxZ(), 0);
    }

    @Test
    public void testCalculateBoundsInLocalInGroupInGroupWithNoTransforms()
    {
        int TRANSLATE_X = 10;
        int TRANSLATE_Z = 5;
        int TRANSLATE_X_GROUP = 7;
        int TRANSLATE_Z_GROUP = 8;
        int TRANSLATE_X_GROUP_GROUP = 20;
        int TRANSLATE_Z_GROUP_GROUP = 21;

        TestUtils utils = new TestUtils();
        ModelContainer mc = utils.makeModelContainer(true);
        mc.moveToCentre();

        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(mc);
        mc.translateBy(TRANSLATE_X, TRANSLATE_Z);
        ModelContainer groupModelContainer = new ModelGroup(modelContainers);
        groupModelContainer.setId("mc group");
        groupModelContainer.translateBy(TRANSLATE_X_GROUP, TRANSLATE_Z_GROUP);

        Set<ModelContainer> modelContainers2 = new HashSet<>();
        modelContainers2.add(groupModelContainer);
        ModelContainer groupGroupModelContainer = new ModelGroup(modelContainers2);
        groupGroupModelContainer.setId("mc group group");
        groupGroupModelContainer.translateBy(TRANSLATE_X_GROUP_GROUP, TRANSLATE_Z_GROUP_GROUP);

        ModelBounds bounds = mc.calculateBoundsInLocal();
        assertEquals(-1, bounds.getMinX(), 0);
        assertEquals(+1, bounds.getMaxX(), 0);
        assertEquals(-1.5, bounds.getMinY(), 0);
        assertEquals(1.5, bounds.getMaxY(), 0);
        assertEquals(0, bounds.getMinZ(), 0);
        assertEquals(0, bounds.getMaxZ(), 0);

        groupModelContainer.printTransforms();
        mc.printTransforms();
        bounds = groupModelContainer.calculateBoundsInLocal();
        assertEquals(-1 + TRANSLATE_X, bounds.getMinX(), 0);
        assertEquals(1 + TRANSLATE_X, bounds.getMaxX(), 0);
        assertEquals(-3.0, bounds.getMinY(), 0);
        assertEquals(0, bounds.getMaxY(), 0);
        assertEquals(0 + TRANSLATE_Z, bounds.getMinZ(), 0);
        assertEquals(0 + TRANSLATE_Z, bounds.getMaxZ(), 0);

        bounds = groupGroupModelContainer.calculateBoundsInLocal();
        assertEquals(-1 + TRANSLATE_X + TRANSLATE_X_GROUP, bounds.getMinX(), 0);
        assertEquals(1 + TRANSLATE_X + TRANSLATE_X_GROUP, bounds.getMaxX(), 0);
        assertEquals(-3.0, bounds.getMinY(), 0);
        assertEquals(0, bounds.getMaxY(), 0);
        assertEquals(0 + TRANSLATE_Z + TRANSLATE_Z_GROUP, bounds.getMinZ(), 0);
        assertEquals( 0 + TRANSLATE_Z + TRANSLATE_Z_GROUP, bounds.getMaxZ(), 0);
    }


}
