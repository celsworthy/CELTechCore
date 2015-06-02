/*
 * Copyright 2015 CEL UK
 */
package celtech.configuration.datafileaccessors;

import celtech.JavaFXConfiguredTest;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import java.io.File;
import javafx.collections.ObservableList;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class SlicerParametersContainerTest extends JavaFXConfiguredTest
{

    @Before
    public void clearUserProfileDir()
    {
        System.out.println("USD " + userStorageFolderPath + " exists " + new File(userStorageFolderPath).exists());
        String userProfileDir = ApplicationConfiguration.getUserPrintProfileDirectory();
        System.out.println("UPD " + userProfileDir + " exists " + new File(userProfileDir).exists());
        File[] files = new File(userProfileDir).listFiles();
        if (files != null && files.length > 0)
        {
            for (File profileFile : files)
            {
                System.out.println("Delete file " + profileFile);
                profileFile.delete();
            }
        }
        SlicerParametersContainer.reload();
    }

    @Test
    public void testLoadProfiles()
    {
        SlicerParametersContainer.getInstance();
        assertEquals(4, SlicerParametersContainer.getApplicationProfileList().size());
        assertEquals(0, SlicerParametersContainer.getUserProfileList().size());
    }

    @Test
    public void testCreateNewProfile()
    {
        String NEW_NAME = "draftCopy1";
        SlicerParametersContainer.getInstance();
        ObservableList<SlicerParametersFile> userProfiles = SlicerParametersContainer.getUserProfileList();
        ObservableList<SlicerParametersFile> completeProfiles = SlicerParametersContainer.getCompleteProfileList();
        SlicerParametersFile draftSlicerParametersFile = SlicerParametersContainer.getSettingsByProfileName(
            ApplicationConfiguration.draftSettingsProfileName);

        SlicerParametersFile draftCopy = draftSlicerParametersFile.clone();
        draftCopy.setProfileName(NEW_NAME);
        assertEquals(0, userProfiles.size());

        SlicerParametersContainer.saveProfile(draftCopy);
        assertEquals(1, userProfiles.size());
        assertEquals(5, completeProfiles.size());

        SlicerParametersFile retrievedProfile = SlicerParametersContainer.getSettingsByProfileName(
            NEW_NAME);
        assertEquals(retrievedProfile, draftCopy);
    }

    @Test
    public void testCreateNewProfileAndDelete()
    {
        String NEW_NAME = "draftCopy1";
        SlicerParametersContainer.getInstance();
        ObservableList<SlicerParametersFile> userProfiles = SlicerParametersContainer.getUserProfileList();
        ObservableList<SlicerParametersFile> completeProfiles = SlicerParametersContainer.getCompleteProfileList();
        SlicerParametersFile draftSlicerParametersFile = SlicerParametersContainer.getSettingsByProfileName(
            ApplicationConfiguration.draftSettingsProfileName);

        SlicerParametersFile draftCopy = draftSlicerParametersFile.clone();
        draftCopy.setProfileName(NEW_NAME);

        SlicerParametersContainer.saveProfile(draftCopy);

        SlicerParametersContainer.deleteUserProfile(NEW_NAME);
        assertEquals(0, userProfiles.size());
        assertEquals(4, completeProfiles.size());
        SlicerParametersFile retrievedProfile = SlicerParametersContainer.getSettingsByProfileName(
            NEW_NAME);
        Assert.assertNull(retrievedProfile);

    }

    @Test
    public void testCreateNewProfileAndChangeAndSave()
    {
        String NEW_NAME = "draftCopy1";
        SlicerParametersContainer.getInstance();
        ObservableList<SlicerParametersFile> userProfiles = SlicerParametersContainer.getUserProfileList();
        ObservableList<SlicerParametersFile> completeProfiles = SlicerParametersContainer.getCompleteProfileList();
        SlicerParametersFile draftSlicerParametersFile = SlicerParametersContainer.getSettingsByProfileName(
            ApplicationConfiguration.draftSettingsProfileName);

        SlicerParametersFile draftCopy = draftSlicerParametersFile.clone();
        draftCopy.setProfileName(NEW_NAME);

        SlicerParametersContainer.saveProfile(draftCopy);

        draftCopy.setBrimWidth_mm(10);
        SlicerParametersContainer.saveProfile(draftCopy);

        SlicerParametersContainer.reload();
        SlicerParametersFile newEditedProfile = SlicerParametersContainer.getSettingsByProfileName(
            NEW_NAME);
        assertEquals(10, newEditedProfile.getBrimWidth_mm());
        assertNotSame(draftCopy, newEditedProfile);

    }
    
    @Test
    public void testCreateNewProfileAndChangeNameAndSave()
    {
        String NEW_NAME = "draftCopy1";
        SlicerParametersContainer.getInstance();
        ObservableList<SlicerParametersFile> userProfiles = SlicerParametersContainer.getUserProfileList();
        ObservableList<SlicerParametersFile> completeProfiles = SlicerParametersContainer.getCompleteProfileList();
        SlicerParametersFile draftSlicerParametersFile = SlicerParametersContainer.getSettingsByProfileName(
            ApplicationConfiguration.draftSettingsProfileName);

        SlicerParametersFile draftCopy = draftSlicerParametersFile.clone();
        draftCopy.setProfileName(NEW_NAME);

        SlicerParametersContainer.saveProfile(draftCopy);

        draftCopy.setBrimWidth_mm(5);
        String CHANGED_NAME = "draftCopy2";
        draftCopy.setProfileName(CHANGED_NAME);
        SlicerParametersContainer.saveProfile(draftCopy);
        assertEquals(1, userProfiles.size());
        assertEquals(5, completeProfiles.size());

        SlicerParametersContainer.reload();
        assertEquals(1, userProfiles.size());
        assertEquals(5, completeProfiles.size());        
        SlicerParametersFile newEditedProfile = SlicerParametersContainer.getSettingsByProfileName(
            CHANGED_NAME);
        assertEquals(5, newEditedProfile.getBrimWidth_mm());
        assertNotSame(draftCopy, newEditedProfile);

    }    

}
