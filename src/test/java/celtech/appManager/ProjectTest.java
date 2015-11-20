/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager;

import celtech.JavaFXConfiguredTest;
import celtech.Lookup;
import celtech.TestUtils;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.fileRepresentation.ProjectFile;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.util.Pair;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author tony
 */
public class ProjectTest extends JavaFXConfiguredTest
{

    private static final String GROUP_NAME = "group";
    private static final String MC3_ID = "mc3";

    @ClassRule
    public static TemporaryFolder temporaryUserStorageFolder = new TemporaryFolder();

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testSaveOneProject() throws IOException
    {
        String PROJECT_NAME = "TestA";
        int BRIM = 2;
        float FILL_DENSITY = 0.45f;
        SlicerParametersFile.SupportType PRINT_SUPPORT = SlicerParametersFile.SupportType.MATERIAL_2;
        String PRINT_JOB_ID = "PJ1";

        Filament FILAMENT_0 = Lookup.getFilamentContainer().getFilamentByID("RBX-ABS-GR499");
        Filament FILAMENT_1 = Lookup.getFilamentContainer().getFilamentByID("RBX-PLA-PP157");

        Project project = new Project();
        project.setProjectName(PROJECT_NAME);
        project.getPrinterSettings().setBrimOverride(BRIM);
        project.getPrinterSettings().setFillDensityOverride(FILL_DENSITY);
        project.getPrinterSettings().setPrintSupportOverride(PRINT_SUPPORT);
        project.setLastPrintJobID(PRINT_JOB_ID);
        project.setExtruder0Filament(FILAMENT_0);
        project.setExtruder1Filament(FILAMENT_1);

        ProjectFile projectFile = new ProjectFile();
        projectFile.populateFromProject(project);

        File tempFile = temporaryUserStorageFolder.newFile("projA.robox");
        objectMapper.writeValue(tempFile, projectFile);

        String filePath = tempFile.getAbsolutePath();
        Project newProject = Project.loadProject(filePath.substring(0, filePath.length() - 6));

        Assert.assertEquals(PROJECT_NAME, newProject.getProjectName());
        Assert.assertEquals(BRIM, newProject.getPrinterSettings().getBrimOverride());
        Assert.assertEquals(FILL_DENSITY, newProject.getPrinterSettings().getFillDensityOverride(),
                            1e-10);
        Assert.assertEquals(PRINT_SUPPORT, newProject.getPrinterSettings().getPrintSupportOverride());
        Assert.assertEquals(FILAMENT_0, newProject.getExtruder0FilamentProperty().get());
        Assert.assertEquals(FILAMENT_1, newProject.getExtruder1FilamentProperty().get());

        assert (true);
    }

    private Pair<Project, ModelGroup> makeProject()
    {
        TestUtils utils = new TestUtils();
        ModelContainer mc1 = utils.makeModelContainer(true);
        ModelContainer mc2 = utils.makeModelContainer(true);
        ModelContainer mc3 = utils.makeModelContainer(true);
        mc3.setId("mc3");
        Project project = new Project();
        project.addModel(mc1);
        project.addModel(mc2);
        project.addModel(mc3);

        Set<ModelContainer> toTranslate = new HashSet<>();
        toTranslate.add(mc2);
        project.translateModelsBy(toTranslate, 10, 20);

        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(mc1);
        modelContainers.add(mc2);
        ModelGroup group = project.group(modelContainers);
        group.setId(GROUP_NAME);
        return new Pair<>(project, group);
    }

    private Pair<Project, ModelGroup> makeProjectWithGroupOfGroups()
    {
        TestUtils utils = new TestUtils();
        ModelContainer mc1 = utils.makeModelContainer(true);
        ModelContainer mc2 = utils.makeModelContainer(true);
        ModelContainer mc3 = utils.makeModelContainer(true);
        ModelContainer mc4 = utils.makeModelContainer(true);
        Project project = new Project();
        project.addModel(mc1);
        project.addModel(mc2);
        project.addModel(mc3);
        project.addModel(mc4);

        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(mc1);
        modelContainers.add(mc2);
        ModelContainer group = project.group(modelContainers);
        group.setId(GROUP_NAME);

        modelContainers = new HashSet<>();
        modelContainers.add(mc3);
        modelContainers.add(mc4);
        modelContainers.add(group);

        ModelGroup superGroup = project.group(modelContainers);

        return new Pair<>(project, superGroup);
    }

    @Test
    public void testSaveProjectWithGroup() throws IOException
    {

        Pair<Project, ModelGroup> pair = makeProject();
        Project project = pair.getKey();
        Set<Integer> expectedIds = project.getTopLevelModels().stream().map(
            x -> ((ModelContainer) x).getModelId()).collect(Collectors.toSet());

        ProjectFile projectFile = new ProjectFile();
        projectFile.populateFromProject(project);

        Project.saveProject(project);

        Project newProject = Project.loadProject(ApplicationConfiguration.getProjectDirectory()
            + File.separator + project.getProjectName());

        Assert.assertEquals(2, newProject.getTopLevelModels().size());

        Assert.assertEquals(expectedIds,
                            newProject.getTopLevelModels().stream().map(x -> x.getModelId()).collect(
                                Collectors.toSet()));
    }

    @Test
    public void testSaveProjectWithGroupOfGroupsThenLoadAndUngroup() throws IOException
    {

        Pair<Project, ModelGroup> pair = makeProjectWithGroupOfGroups();
        Project project = pair.getKey();
        ModelGroup superGroup = pair.getValue();
        Set<Integer> expectedIds = superGroup.getChildModelContainers().stream().map(
            x -> ((ModelContainer) x).getModelId()).collect(Collectors.toSet());

        ProjectFile projectFile = new ProjectFile();
        projectFile.populateFromProject(project);

        Project.saveProject(project);

        Project newProject = Project.loadProject(ApplicationConfiguration.getProjectDirectory()
            + File.separator + project.getProjectName());

        Assert.assertEquals(1, newProject.getTopLevelModels().size());

        Set<ModelContainer> modelContainers = new HashSet<>(newProject.getTopLevelModels());
        newProject.ungroup(modelContainers);

        Assert.assertEquals(3, newProject.getTopLevelModels().size());

        Assert.assertEquals(expectedIds,
                            newProject.getTopLevelModels().stream().map(x -> x.getModelId()).collect(
                                Collectors.toSet()));

        Set<ModelGroup> modelGroups = newProject.getTopLevelModels().stream().
            filter(x -> x instanceof ModelGroup).map(x -> (ModelGroup) x).collect(Collectors.toSet());

        Assert.assertEquals(1, modelGroups.size());
        ModelGroup modelGroup = modelGroups.iterator().next();
        Assert.assertEquals(2, modelGroup.getChildModelContainers().size());

    }
    
    @Test
    public void testSaveProjectWithGroupWithRotation() throws IOException
    {
        
        double ROTATION = 20.1f;

        Pair<Project, ModelGroup> pair = makeProject();
        Project project = pair.getKey();
        ModelGroup group = pair.getValue();
        group.setRotationLean(ROTATION);
        
        ProjectFile projectFile = new ProjectFile();
        projectFile.populateFromProject(project);

        Project.saveProject(project);

        Project newProject = Project.loadProject(ApplicationConfiguration.getProjectDirectory()
            + File.separator + project.getProjectName());
        
        Set<ModelGroup> modelGroups = newProject.getTopLevelModels().stream().
            filter(x -> x instanceof ModelGroup).map(x -> (ModelGroup) x).collect(Collectors.toSet());
        
        Assert.assertEquals(1, modelGroups.size());
        ModelGroup modelGroup = modelGroups.iterator().next();
        Assert.assertEquals(ROTATION, modelGroup.getRotationLean(), 0.001);
    }    
}
