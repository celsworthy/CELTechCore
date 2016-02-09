package celtech.configuration.datafileaccessors;

import celtech.configuration.ApplicationConfiguration;
import celtech.roboxbase.configuration.SlicerType;
import celtech.configuration.UserPreferences;
import celtech.configuration.fileRepresentation.UserPreferenceFile;
import celtech.roboxbase.configuration.BaseConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class UserPreferenceContainer
{

    private static final Stenographer steno = StenographerFactory.getStenographer(UserPreferenceContainer.class.getName());
    private static UserPreferenceContainer instance = null;
    private static UserPreferenceFile userPreferenceFile = null;
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final String defaultUserPreferenceFilename = "roboxpreferences.pref";

    private UserPreferenceContainer()
    {
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        File userPreferenceInputFile = new File(BaseConfiguration.getUserStorageDirectory() + defaultUserPreferenceFilename);
        if (!userPreferenceInputFile.exists())
        {
            userPreferenceFile = new UserPreferenceFile();
            userPreferenceFile.setSlicerType(SlicerType.Cura);
            try
            {
                mapper.writeValue(userPreferenceInputFile, userPreferenceFile);
            } catch (IOException ex)
            {
                steno.exception("Error trying to create user preferences file at " + userPreferenceInputFile.getAbsolutePath(), ex);
            }
        } else
        {
            try
            {
                userPreferenceFile = mapper.readValue(userPreferenceInputFile, UserPreferenceFile.class);

            } catch (IOException ex)
            {
                steno.error("Error loading user preferences " + userPreferenceInputFile.getAbsolutePath() + ": " + ex.getMessage());
            }
        }
    }

    /**
     *
     * @return
     */
    public static UserPreferenceContainer getInstance()
    {
        if (instance == null)
        {
            instance = new UserPreferenceContainer();
        }

        return instance;
    }

    /**
     *
     * @return
     */
    public static UserPreferenceFile getUserPreferenceFile()
    {
        if (instance == null)
        {
            instance = new UserPreferenceContainer();
        }

        return userPreferenceFile;
    }

    public static void savePreferences(UserPreferences userPreferences)
    {
        File userPreferenceInputFile = new File(BaseConfiguration.getUserStorageDirectory() + defaultUserPreferenceFilename);

        userPreferenceFile.populateFromSettings(userPreferences);

        try
        {
            mapper.writeValue(userPreferenceInputFile, userPreferenceFile);
        } catch (IOException ex)
        {
            steno.error("Error trying to write user preferences");
        }
    }
}
