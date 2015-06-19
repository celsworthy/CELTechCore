package celtech.configuration.datafileaccessors;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintProfileFileFilter;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.fileRepresentation.SlicerParametersFile.HeadType;
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
    
    private static String getSettingsKey(String profileName, HeadType headType) {
        return profileName + "#" + headType.name();
    }

    public static String constructFilePath(String profileName, HeadType headType)
    {
        return ApplicationConfiguration.getUserPrintProfileDirectory() + getSettingsKey(profileName, headType)
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
        ArrayList<SlicerParametersFile> profiles = ingestProfiles(applicationprofiles);
        appProfileList.addAll(profiles);
        completeProfileList.addAll(profiles);

        File userDirHandle = new File(ApplicationConfiguration.getUserPrintProfileDirectory());
        File[] userprofiles = userDirHandle.listFiles(new PrintProfileFileFilter());
        profiles = ingestProfiles(userprofiles);
        userProfileList.addAll(profiles);
        completeProfileList.addAll(profiles);
        
        for (SlicerParametersFile profile : completeProfileList)
        {
            profileMap.put(profile.getProfileKey(), profile);
        }
    }

    private static ArrayList<SlicerParametersFile> ingestProfiles(File[] userprofiles)
    {
        System.out.println("ingest");
        ArrayList<SlicerParametersFile> profileList = new ArrayList<>();

        for (File profileFile : userprofiles)
        {
            SlicerParametersFile newSettings = null;
            String profileKey = profileFile.getName().replaceAll(
                ApplicationConfiguration.printProfileFileExtension, "");

            if (profileMap.containsKey(profileKey) == false)
            {
                try
                {
                    newSettings = mapper.readValue(profileFile, SlicerParametersFile.class);
                    
                    convertToCurrentVersion(newSettings);
                    
                    profileList.add(newSettings);
                    profileMap.put(newSettings.getProfileKey(), newSettings);
                    System.out.println("loaded profile " + newSettings.getProfileKey());
                } catch (Exception ex)
                {
                    steno.error("Error reading profile " + profileKey + ": " + ex.getMessage());
                }
            } else
            {
                steno.warning("Profile with name " + profileKey
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
            newSettings.setInterfaceSpeed_mm_per_s(40);
            newSettings.setVersion(4);
            doSaveEditedUserProfile(newSettings);
        }
        
        if (newSettings.getVersion() < 5) {
            steno.info("Convert " + newSettings.getProfileName() + " profile to version 5");
            newSettings.setHeadType(HeadContainer.defaultHeadType);
            newSettings.setVersion(5);
            doSaveEditedUserProfile(newSettings);
        }
    }

    public static void saveProfile(SlicerParametersFile settingsToSave)
    {
        if (!profileMap.containsKey(settingsToSave.getProfileKey()))
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
     * Save the given user profile which has had its name changed. This amounts to a delete and add
     * new.
     *
     * @param profile
     */
    private static void doSaveAndChangeUserProfileName(SlicerParametersFile profile)
    {
        String originalName = "";
        HeadType originalHeadType = null;
        for (Map.Entry<String, SlicerParametersFile> entrySet : profileMap.entrySet())
        {
            originalName = entrySet.getKey().split("#")[0];
            SlicerParametersFile value = entrySet.getValue();
            if (value == profile) {
                originalHeadType = profile.getHeadType();
                break;
            }
        }
        if (originalName.equals("")) {
            steno.error("Severe error saving profile of changed name.");
        } else {
            deleteUserProfile(originalName, originalHeadType);
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
        profileMap.put(profile.getProfileKey(), profile);
    }

    /**
     * Save the edited user profile to disk.
     */
    private static void doSaveEditedUserProfile(SlicerParametersFile profile)
    {
        try
        {
            mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
            mapper.writeValue(new File(constructFilePath(profile.getProfileName(), profile.getHeadType())), profile);
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
            mapper.writeValue(new File(constructFilePath(settingsToSave.getProfileName(), settingsToSave.getHeadType())),
                              settingsToSave);
        } catch (IOException ex)
        {
            steno.error("Error whilst saving profile " + settingsToSave.getProfileName());
        }
    }

    public static void deleteUserProfile(String profileName, HeadType headType)
    {
        SlicerParametersFile deletedProfile = getSettings(profileName, headType);
        assert(deletedProfile != null);
        File profileToDelete = new File(constructFilePath(profileName, headType));
        profileToDelete.delete();
        
        userProfileList.remove(deletedProfile);
        completeProfileList.remove(deletedProfile);
        profileMap.remove(deletedProfile.getProfileKey());
    }

    public static SlicerParametersContainer getInstance()
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }

        return instance;
    }

    public static SlicerParametersFile getSettings(String profileName, HeadType headType)
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }

        return profileMap.get(getSettingsKey(profileName, headType));
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
