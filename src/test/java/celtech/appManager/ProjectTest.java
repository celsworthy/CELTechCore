/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager;

import celtech.JavaFXConfiguredTest;
import celtech.Lookup;
import celtech.TestUtils;
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
        
        Set<ModelContainer> setMC = new HashSet<>();
        setMC.add(mc1);
        setMC.add(mc2);
        ModelContainer group = project.group(setMC);
        return new Pair<>(project, group);
    }
    
    @Test
    public void testGroupInitialTransforms() {
        
        Pair<Project, ModelContainer> pair = makeProject();
        Project project = pair.getKey();
        ModelContainer group = pair.getValue();
        
        Assert.assertEquals(0, group.getTransformMoveToCentre().getX(), 0);
        Assert.assertEquals(0, group.getTransformMoveToCentre().getY(), 0);
        Assert.assertEquals(0, group.getTransformMoveToCentre().getZ(), 0);
        
    }
    
    @Test
    public void testInitialCentre() {
        
        Pair<Project, ModelContainer> pair = makeProject();
        Project project = pair.getKey();
        ModelContainer group = pair.getValue();
        
        group.printTransforms();
        
        Assert.assertEquals(110, group.getCentreX(), 0);
        Assert.assertEquals(52, group.getCentreY(), 0);
        Assert.assertEquals(85, group.getCentreZ(), 0);
        
        Assert.assertEquals(110, group.getTransformedCentreX(), 0);
        Assert.assertEquals(52, group.getTransformedCentreZ(), 0);
        
    }
    
}
