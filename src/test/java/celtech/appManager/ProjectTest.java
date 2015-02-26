/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager;

import celtech.JavaFXConfiguredTest;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.configuration.fileRepresentation.ProjectFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.ClassRule;
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
        boolean PRINT_SUPPORT = true;
        String PRINT_JOB_ID = "PJ1";
        Filament FILAMENT_0 = FilamentContainer.getFilamentByID("RBX-ABS-GR499");
        Filament FILAMENT_1 = FilamentContainer.getFilamentByID("RBX-PLA-PP157");
        
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
        
        Project newProject = new Project();
//        newProject.load(tempFile.getAbsolutePath());
        assertEquals(PROJECT_NAME, newProject.getProjectName());
        assertEquals(BRIM, newProject.getPrinterSettings().getBrimOverride());
        assertEquals(FILL_DENSITY, newProject.getPrinterSettings().getFillDensityOverride(), 1e-10);
        assertEquals(PRINT_SUPPORT, newProject.getPrinterSettings().getPrintSupportOverride());
        assertEquals(FILAMENT_0, newProject.getExtruder0FilamentProperty().get());
        assertEquals(FILAMENT_1, newProject.getExtruder1FilamentProperty().get());
    }
}
