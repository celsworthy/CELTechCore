/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.configuration;

import celtech.services.slicer.SlicerSettings;
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
    private static final ObservableList<SlicerSettings> appProfileList = FXCollections.observableArrayList();
    private static final ObservableList<SlicerSettings> userProfileList = FXCollections.observableArrayList();
    private static final ObservableList<SlicerSettings> completeProfileList = FXCollections.observableArrayList();
    private static final ObservableMap<String, SlicerSettings> profileMap = FXCollections.observableHashMap();
    public static final SlicerSettings createNewProfile = new SlicerSettings();

    private PrintProfileContainer()
    {
        loadProfileData();
    }

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
        ArrayList<SlicerSettings> profiles = ingestProfiles(applicationprofiles, false);
        appProfileList.addAll(profiles);
        completeProfileList.addAll(profiles);

        File userDirHandle = new File(ApplicationConfiguration.getUserPrintProfileDirectory());
        File[] userprofiles = userDirHandle.listFiles(new PrintProfileFileFilter());
        profiles = ingestProfiles(userprofiles, true);
        userProfileList.addAll(profiles);
        completeProfileList.addAll(profiles);
    }

    private static ArrayList<SlicerSettings> ingestProfiles(File[] userprofiles, boolean mutableProfiles)
    {
        ArrayList<SlicerSettings> profileList = new ArrayList<>();

        for (File profileFile : userprofiles)
        {
            SlicerSettings newSettings = new SlicerSettings();
            String profileName = profileFile.getName().replaceAll(ApplicationConfiguration.printProfileFileExtension, "");
            newSettings.readFromFile(profileName, mutableProfiles, profileFile.getAbsolutePath());

            profileList.add(newSettings);
            profileMap.put(profileName, newSettings);
        }

        return profileList;
    }

    public static void saveProfile(SlicerSettings settingsToSave)
    {
        settingsToSave.writeToFile(constructFilePath(settingsToSave.getProfileName()));
        loadProfileData();
    }

    public static void deleteProfile(String profileName)
    {
        File profileToDelete = new File(constructFilePath(profileName));
        profileToDelete.delete();
        loadProfileData();
    }

    public static PrintProfileContainer getInstance()
    {
        if (instance == null)
        {
            instance = new PrintProfileContainer();
        }

        return instance;
    }

    public static SlicerSettings getSettingsByProfileName(String profileName)
    {
        if (instance == null)
        {
            instance = new PrintProfileContainer();
        }

        return profileMap.get(profileName);
    }

    public static ObservableList<SlicerSettings> getCompleteProfileList()
    {
        if (instance == null)
        {
            instance = new PrintProfileContainer();
        }

        return completeProfileList;
    }

    public static ObservableList<SlicerSettings> getUserProfileList()
    {
        if (instance == null)
        {
            instance = new PrintProfileContainer();
        }

        return userProfileList;
    }
}
