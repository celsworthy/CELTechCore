package celtech.configuration.fileRepresentation;

import celtech.JavaFXConfiguredTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian
 */
public class ModelContainerProjectFileTest extends JavaFXConfiguredTest
{

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String jsonifiedClass = "{\"@class\":\"celtech.configuration.fileRepresentation.ModelContainerProjectFile\",\"version\":3,\"projectName\":null,\"lastModifiedDate\":null,\"lastPrintJobID\":\"\",\"subVersion\":3,\"brimOverride\":0,\"fillDensityOverride\":0.0,\"printSupportOverride\":false,\"printSupportTypeOverride\":\"MATERIAL_1\",\"printRaft\":false,\"spiralPrint\":false,\"extruder0FilamentID\":null,\"extruder1FilamentID\":null,\"settingsName\":\"Draft\",\"printQuality\":\"DRAFT\",\"groupStructure\":{},\"groupState\":{}}";
    private static final String jsonifiedClass_2_03_01 = "{\"version\":3,\"projectName\":null,\"lastModifiedDate\":null,\"lastPrintJobID\":\"\",\"subVersion\":3,\"brimOverride\":0,\"fillDensityOverride\":0.0,\"printSupportOverride\":false,\"printSupportTypeOverride\":\"MATERIAL_1\",\"printRaft\":false,\"spiralPrint\":false,\"extruder0FilamentID\":null,\"extruder1FilamentID\":null,\"settingsName\":\"Draft\",\"printQuality\":\"DRAFT\",\"groupStructure\":{},\"groupState\":{}}";

    public ModelContainerProjectFileTest()
    {
    }

    @Test
    public void serializesToJSON() throws Exception
    {
        final ModelContainerProjectFile projectFile = createTestProjectFile();

        String mappedValue = mapper.writeValueAsString(projectFile);
        assertEquals(jsonifiedClass, mappedValue);
    }

    @Test
    public void deserializesFromJSON() throws Exception
    {
        try
        {
            ModelContainerProjectFile projectFileReceived = mapper.readValue(jsonifiedClass, ModelContainerProjectFile.class);
        } catch (Exception e)
        {
            System.out.println(e.getCause().getMessage());
            fail();
        }
    }

//    @Test
//    public void deserializes_2_03_01_FromJSON() throws Exception
//    {
//        try
//        {
//            ModelContainerProjectFile projectFileReceived = mapper.readValue(jsonifiedClass_2_03_01, ModelContainerProjectFile.class);
//        } catch (Exception e)
//        {
//            fail();
//        }
//    }

    private ModelContainerProjectFile createTestProjectFile()
    {
        ModelContainerProjectFile projectFile = new ModelContainerProjectFile();
        return projectFile;
    }
}
