/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.configuration;

import celtech.services.slicer.RoboxProfile;
import java.io.File;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class PrintProfileContainer
{

    private static final Stenographer steno = StenographerFactory.getStenographer(PrintProfileContainer.class.getName());
    private static PrintProfileContainer instance = null;
    private static final ObservableList<RoboxProfile> appProfileList = FXCollections.observableArrayList();
    private static final ObservableList<RoboxProfile> userProfileList = FXCollections.observableArrayList();
    private static final ObservableList<RoboxProfile> completeProfileList = FXCollections.observableArrayList();
    private static final ObservableMap<String, RoboxProfile> profileMap = FXCollections.observableHashMap();

    /**
     *
     */
    public static final RoboxProfile createNewProfile = new RoboxProfile();

    private PrintProfileContainer()
    {
        loadProfileData();
    }

    /**
     *
     * @param profileName
     * @return
     */
    public static String constructFilePath(String profileName)
    {
        return ApplicationConfiguration.getUserPrintProfileDirectory() + profileName + ApplicationConfiguration.printProfileFileExtension;
    }

    private static void loadProfileData()
    {
        completeProfileList.clear();
        appProfileList.clear();
        userProfileList.clear();

        File applicationDirHandle = new File(ApplicationConfiguration.getApplicationPrintProfileDirectory());
        File[] applicationprofiles = applicationDirHandle.listFiles(new PrintProfileFileFilter());
        ArrayList<RoboxProfile> profiles = ingestProfiles(applicationprofiles, false);
        appProfileList.addAll(profiles);
        completeProfileList.addAll(profiles);

        File userDirHandle = new File(ApplicationConfiguration.getUserPrintProfileDirectory());
        File[] userprofiles = userDirHandle.listFiles(new PrintProfileFileFilter());
        profiles = ingestProfiles(userprofiles, true);
        userProfileList.addAll(profiles);
        completeProfileList.addAll(profiles);
    }

    private static ArrayList<RoboxProfile> ingestProfiles(File[] userprofiles, boolean mutableProfiles)
    {
        ArrayList<RoboxProfile> profileList = new ArrayList<>();

        for (File profileFile : userprofiles)
        {
            RoboxProfile newSettings = new RoboxProfile();
            String profileName = profileFile.getName().replaceAll(ApplicationConfiguration.printProfileFileExtension, "");
            newSettings.readFromFile(profileName, mutableProfiles, profileFile.getAbsolutePath());

            profileList.add(newSettings);
            profileMap.put(profileName, newSettings);
        }

        return profileList;
    }

    /**
     *
     * @param settingsToSave
     */
    public static void saveProfile(RoboxProfile settingsToSave)
    {
        settingsToSave.writeToFile(constructFilePath(settingsToSave.getProfileName()));
        loadProfileData();
    }

    /**
     *
     * @param profileName
     */
    public static void deleteProfile(String profileName)
    {
        File profileToDelete = new File(constructFilePath(profileName));
        profileToDelete.delete();
        loadProfileData();
    }

    /**
     *
     * @return
     */
    public static PrintProfileContainer getInstance()
    {
        if (instance == null)
        {
            instance = new PrintProfileContainer();
        }

        return instance;
    }

    /**
     *
     * @param profileName
     * @return
     */
    public static RoboxProfile getSettingsByProfileName(String profileName)
    {
        if (instance == null)
        {
            instance = new PrintProfileContainer();
        }

        return profileMap.get(profileName);
    }

    /**
     *
     * @return
     */
    public static ObservableList<RoboxProfile> getCompleteProfileList()
    {
        if (instance == null)
        {
            instance = new PrintProfileContainer();
        }

        return completeProfileList;
    }

    /**
     *
     * @return
     */
    public static ObservableList<RoboxProfile> getUserProfileList()
    {
        if (instance == null)
        {
            instance = new PrintProfileContainer();
        }

        return userProfileList;
    }
}
