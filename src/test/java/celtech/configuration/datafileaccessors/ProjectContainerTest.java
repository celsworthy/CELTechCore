/*
 * Copyright 2015 CEL UK
 */
package celtech.configuration.datafileaccessors;

import celtech.JavaFXConfiguredTest;
import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.configuration.fileRepresentation.ProjectFile;
import java.io.IOException;
import javafx.collections.ObservableList;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class ProjectContainerTest extends JavaFXConfiguredTest
{
    
    @Test
    public void testSaveOneProject() throws IOException
    {
        ProjectContainer projectContainer = new ProjectContainer();
        ObservableList<ProjectFile> projectList = projectContainer.getCompleteProjectList();
        
        String PROJECT_NAME = "TestA";
        int BRIM = 2;
        float FILL_DENSITY = 0.45f;
        boolean PRINT_SUPPORT = true;
        String PRINT_JOB_ID = "PJ1";
        Filament FILAMENT_0 = FilamentContainer.getFilamentByID("RBX-ABS-GR499");
        Filament FILAMENT_1 = FilamentContainer.getFilamentByID("RBX-PLA-PP157");
        
        Project project = new Project();
        project.setProjectName(PROJECT_NAME);
        project.setBrimOverride(BRIM);
        project.setFillDensityOverride(FILL_DENSITY);
        project.setPrintSupportOverride(PRINT_SUPPORT);
        project.setLastPrintJobID(PRINT_JOB_ID);
        project.setExtruder0Filament(FILAMENT_0);
        project.setExtruder1Filament(FILAMENT_1);
        
        ProjectFile projectFile = new ProjectFile();
        projectFile.populateFromProject(project);
        projectList.add(projectFile);
        
        projectContainer.saveProjectFiles();
        
        projectList.clear();
        assertEquals(0, projectList.size());
        projectContainer.ingestProjectFiles();
        assertEquals(1, projectList.size());
        Project newProject = new Project(projectList.get(0));
        assertEquals(PROJECT_NAME, newProject.getProjectName());
        assertEquals(BRIM, newProject.getBrimOverride());
        assertEquals(FILL_DENSITY, newProject.getFillDensityOverride(), 1e-10);
        assertEquals(PRINT_SUPPORT, newProject.getPrintSupportOverride());
        assertEquals(FILAMENT_0, newProject.getExtruder0FilamentProperty().get());
        assertEquals(FILAMENT_1, newProject.getExtruder1FilamentProperty().get());
    }
    
}
