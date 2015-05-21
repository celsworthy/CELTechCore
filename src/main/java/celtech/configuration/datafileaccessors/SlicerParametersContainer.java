package celtech.configuration.datafileaccessors;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintProfileFileFilter;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/**
 *
 * @author ianhudson
 */
public class SlicerParametersContainer
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
        SlicerParametersContainer.class.getName());
    private static SlicerParametersContainer instance = null;
    private static final ObservableList<SlicerParametersFile> appProfileList = FXCollections.observableArrayList();
    private static final ObservableList<SlicerParametersFile> userProfileList = FXCollections.observableArrayList();
    private static final ObservableList<SlicerParametersFile> completeProfileList = FXCollections.observableArrayList();
    private static final ObservableMap<String, SlicerParametersFile> profileMap = FXCollections.observableHashMap();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Set<String> getProfileNames()
    {
        return Collections.unmodifiableSet(profileMap.keySet());
    }

    private SlicerParametersContainer()
    {
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        loadProfileData();
    }

    public static String constructFilePath(String profileName)
    {
        return ApplicationConfiguration.getUserPrintProfileDirectory() + profileName
            + ApplicationConfiguration.printProfileFileExtension;
    }

    private static void loadProfileData()
    {
        completeProfileList.clear();
        appProfileList.clear();
        userProfileList.clear();
        profileMap.clear();

        File applicationDirHandle = new File(
            ApplicationConfiguration.getApplicationPrintProfileDirectory());
        File[] applicationprofiles = applicationDirHandle.listFiles(new PrintProfileFileFilter());
        ArrayList<SlicerParametersFile> profiles = ingestProfiles(applicationprofiles, false);
        appProfileList.addAll(profiles);
        completeProfileList.addAll(profiles);

        File userDirHandle = new File(ApplicationConfiguration.getUserPrintProfileDirectory());
        File[] userprofiles = userDirHandle.listFiles(new PrintProfileFileFilter());
        profiles = ingestProfiles(userprofiles, true);
        userProfileList.addAll(profiles);
        completeProfileList.addAll(profiles);
    }

    private static ArrayList<SlicerParametersFile> ingestProfiles(File[] userprofiles,
        boolean mutableProfiles)
    {
        ArrayList<SlicerParametersFile> profileList = new ArrayList<>();

        for (File profileFile : userprofiles)
        {
            SlicerParametersFile newSettings = new SlicerParametersFile();
            String profileName = profileFile.getName().replaceAll(
                ApplicationConfiguration.printProfileFileExtension, "");

            if (profileMap.containsKey(profileName) == false)
            {
                try
                {
                    newSettings = mapper.readValue(profileFile, SlicerParametersFile.class);
                    
                    convertToCurrentVersion(newSettings);
                    
                    profileList.add(newSettings);
                    profileMap.put(profileName, newSettings);
                } catch (IOException ex)
                {
                    steno.error("Error reading profile " + profileName + ": " + ex.getMessage());
                }
            } else
            {
                steno.warning("Profile with name " + profileName
                    + " has already been loaded - ignoring " + profileFile.getAbsolutePath());
            }
        }

        return profileList;
    }

    private static void convertToCurrentVersion(SlicerParametersFile newSettings)
    {
        
        if (newSettings.getVersion() < 4) {
            steno.info("Convert " + newSettings.getProfileName() + " profile to version 4");
            newSettings.setRaftAirGapLayer0_mm(0.285f);
            newSettings.setRaftBaseLinewidth_mm(1.0f);
            newSettings.setInterfaceLayers(1);
            newSettings.setVersion(4);
            doSaveEditedUserProfile(newSettings);
        }
    }

    public static void saveProfile(SlicerParametersFile settingsToSave)
    {
        if (!profileMap.containsKey(settingsToSave.getProfileName()))
        {
            if (userProfileList.contains(settingsToSave))
            {
                doSaveAndChangeUserProfileName(settingsToSave);
            } else
            {
                doAddNewUserProfile(settingsToSave);
            }
        } else
        {
            doSaveEditedUserProfile(settingsToSave);
        }
    }

    /**
     * save the given user profile which has had its name changed. This amounts to a delete and add
     * new.
     *
     * @param profile
     */
    private static void doSaveAndChangeUserProfileName(SlicerParametersFile profile)
    {
        // The original name can be retrieved from profileMap
        String originalName = "";
        for (Map.Entry<String, SlicerParametersFile> entrySet : profileMap.entrySet())
        {
            String name = entrySet.getKey();
            SlicerParametersFile value = entrySet.getValue();
            if (value == profile) {
                originalName = name;
                break;
            }
        }
        if (originalName.equals("")) {
            steno.error("Severe error saving profile of changed name.");
        } else {
            deleteUserProfile(originalName);
            doAddNewUserProfile(profile);
        }
    }

    /**
     * Save the new user profile to disk.
     *
     * @param profile
     */
    private static void doAddNewUserProfile(SlicerParametersFile profile)
    {
        doSaveEditedUserProfile(profile);
        userProfileList.add(profile);
        completeProfileList.add(profile);
        profileMap.put(profile.getProfileName(), profile);
    }

    /**
     * Save the edited user profile to disk.
     */
    private static void doSaveEditedUserProfile(SlicerParametersFile profile)
    {
        try
        {
            mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
            mapper.writeValue(new File(constructFilePath(profile.getProfileName())), profile);
        } catch (IOException ex)
        {
            steno.error("Error whilst saving profile " + profile.getProfileName());
        }
    }

    public static void saveProfileWithoutReloading(SlicerParametersFile settingsToSave)
    {
        try
        {
            mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
            mapper.writeValue(new File(constructFilePath(settingsToSave.getProfileName())),
                              settingsToSave);
        } catch (IOException ex)
        {
            steno.error("Error whilst saving profile " + settingsToSave.getProfileName());
        }
    }

    public static void deleteUserProfile(String profileName)
    {
        File profileToDelete = new File(constructFilePath(profileName));
        profileToDelete.delete();
        SlicerParametersFile deletedProfile = getSettingsByProfileName(profileName);
        userProfileList.remove(deletedProfile);
        completeProfileList.remove(deletedProfile);
        profileMap.remove(profileName);
    }

    public static SlicerParametersContainer getInstance()
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }

        return instance;
    }

    public static SlicerParametersFile getSettingsByProfileName(String profileName)
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }

        return profileMap.get(profileName);
    }

    public static ObservableList<SlicerParametersFile> getCompleteProfileList()
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }

        return completeProfileList;
    }

    public static ObservableList<SlicerParametersFile> getUserProfileList()
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }

        return userProfileList;
    }

    public static ObservableList<SlicerParametersFile> getApplicationProfileList()
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }

        return appProfileList;
    }

    public static boolean applicationProfileListContainsProfile(String profileName)
    {
        return appProfileList.stream()
            .anyMatch((profile) -> profile.getProfileName().equalsIgnoreCase(profileName));
    }

    /**
     * For testing only
     */
    protected static void reload()
    {
        loadProfileData();
    }
}
