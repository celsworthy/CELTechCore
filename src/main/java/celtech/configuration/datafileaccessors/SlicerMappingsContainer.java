package celtech.configuration.datafileaccessors;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.fileRepresentation.SlicerMappings;
import java.io.File;
import java.io.IOException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/**
 *
 * @author ianhudson
 */
public class SlicerMappingsContainer
{

    private static final Stenographer steno = StenographerFactory.getStenographer(SlicerMappingsContainer.class.getName());
    private static SlicerMappingsContainer instance = null;
    private static SlicerMappings slicerMappingsFile = null;
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final String defaultSlicerMappingsFileName = "slicermapping.dat";

    private SlicerMappingsContainer()
    {
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        File slicerMappingsInputFile = new File(ApplicationConfiguration.getApplicationPrintProfileDirectory() + defaultSlicerMappingsFileName);
        if (!slicerMappingsInputFile.exists())
        {
            slicerMappingsFile = new SlicerMappings();
            try
            {
                mapper.writeValue(slicerMappingsInputFile, slicerMappingsFile);
            } catch (IOException ex)
            {
                steno.error("Error trying to load slicer mapping file");
            }
        } else
        {
            try
            {
                slicerMappingsFile = mapper.readValue(slicerMappingsInputFile, SlicerMappings.class);

            } catch (IOException ex)
            {
                ex.printStackTrace();
                steno.error("Error loading slicer mapping file " + slicerMappingsInputFile.getAbsolutePath() + " " + ex.getMessage());
            }
        }
    }

    /**
     *
     * @return
     */
    public static SlicerMappingsContainer getInstance()
    {
        if (instance == null)
        {
            instance = new SlicerMappingsContainer();
        }

        return instance;
    }

    /**
     *
     * @return
     */
    public static SlicerMappings getSlicerMappings()
    {
        if (instance == null)
        {
            instance = new SlicerMappingsContainer();
        }

        return slicerMappingsFile;
    }
}
