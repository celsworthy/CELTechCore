/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.configuration;

import celtech.services.slicer.SlicerSettings;
import java.io.File;
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
    private static final ObservableList<SlicerSettings> profileList = FXCollections.observableArrayList();
    private static final ObservableMap<String, SlicerSettings> profileMap = FXCollections.observableHashMap();

    private PrintProfileContainer()
    {
        File applicationDirHandle = new File(ApplicationConfiguration.getApplicationPrintProfileDirectory());
        File[] applicationprofiles = applicationDirHandle.listFiles(new PrintProfileFileFilter());
        ingestProfiles(applicationprofiles, true);

        File userDirHandle = new File(ApplicationConfiguration.getUserPrintProfileDirectory());
        File[] userprofiles = userDirHandle.listFiles(new PrintProfileFileFilter());
        ingestProfiles(userprofiles, false);
    }

    private void ingestProfiles(File[] userprofiles, boolean lockProfiles)
    {
        for (File profileFile : userprofiles)
        {
            SlicerSettings newSettings = new SlicerSettings();
            String profileName = profileFile.getName().replaceAll(ApplicationConfiguration.printProfileFileExtension, "");
            newSettings.readFromFile(profileName, lockProfiles, profileFile.getAbsolutePath());

            profileMap.put(profileName, newSettings);
        }
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

    public static ObservableList<SlicerSettings> getObservableValues()
    {
        if (instance == null)
        {
            instance = new PrintProfileContainer();
        }

        return profileList;
    }
}
