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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
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
    
    private static String GROUP_NAME = "group";

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
        Assert.assertEquals(FILL_DENSITY, newProject.getPrinterSettings().getFillDensityOverride(), 1e-10);
        Assert.assertEquals(PRINT_SUPPORT, newProject.getPrinterSettings().getPrintSupportOverride());
        Assert.assertEquals(FILAMENT_0, newProject.getExtruder0FilamentProperty().get());
        Assert.assertEquals(FILAMENT_1, newProject.getExtruder1FilamentProperty().get());

        assert (true);
    }
    
    private Pair<Project, ModelContainer> makeProject() {
        TestUtils utils = new TestUtils();
        ModelContainer mc1 = utils.makeModelContainer(true);
        ModelContainer mc2 = utils.makeModelContainer(true);
        ModelContainer mc3 = utils.makeModelContainer(true);
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
        ModelContainer group = project.group(modelContainers);
        group.setId(GROUP_NAME);
        return new Pair<>(project, group);
    }
    
    @Test
    public void testSaveProjectWithGroup() throws IOException {
        
        Pair<Project, ModelContainer> pair = makeProject();
        Project project = pair.getKey();
        ModelContainer group = pair.getValue();
        
        ProjectFile projectFile = new ProjectFile();
        projectFile.populateFromProject(project);

        Project.saveProject(project);

        Project newProject = Project.loadProject(ApplicationConfiguration.getProjectDirectory() 
            + File.separator + project.getProjectName());

        Assert.assertEquals(1, newProject.getLoadedModels().size());
        Assert.assertEquals(GROUP_NAME, newProject.getLoadedModels().get(0).getId());
    }
}
