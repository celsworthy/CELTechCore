/*
 * Copyright 2015 CEL UK
 */
package celtech.configuration.datafileaccessors;

import celtech.JavaFXConfiguredTest;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.printerControl.model.Head.HeadType;
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
        String userProfileDir = ApplicationConfiguration.getUserPrintProfileDirectory();
        File[] files = new File(userProfileDir).listFiles();
        if (files != null && files.length > 0)
        {
            for (File profileFile : files)
            {
                profileFile.delete();
            }
        }
        SlicerParametersContainer.reload();
    }

    @Test
    public void testLoadProfiles()
    {
        SlicerParametersContainer.getInstance();
        assertEquals(8, SlicerParametersContainer.getApplicationProfileList().size());
        assertEquals(0, SlicerParametersContainer.getUserProfileList().size());
    }

    @Test
    public void testCreateNewProfile()
    {
        String NEW_NAME = "draftCopy1";
        SlicerParametersContainer.getInstance();
        ObservableList<SlicerParametersFile> userProfiles = SlicerParametersContainer.getUserProfileList();
        ObservableList<SlicerParametersFile> completeProfiles = SlicerParametersContainer.getCompleteProfileList();
        SlicerParametersFile draftSlicerParametersFile = SlicerParametersContainer.getSettings(ApplicationConfiguration.draftSettingsProfileName, HeadType.SINGLE_MATERIAL_HEAD);

        SlicerParametersFile draftCopy = draftSlicerParametersFile.clone();
        draftCopy.setProfileName(NEW_NAME);
        assertEquals(0, userProfiles.size());

        SlicerParametersContainer.saveProfile(draftCopy);
        assertEquals(1, userProfiles.size());
        assertEquals(9, completeProfiles.size());

        SlicerParametersFile retrievedProfile = SlicerParametersContainer.getSettings(NEW_NAME, HeadType.SINGLE_MATERIAL_HEAD);
        assertEquals(retrievedProfile, draftCopy);
    }

    @Test
    public void testCreateNewProfileAndDelete()
    {
        String NEW_NAME = "draftCopy1";
        SlicerParametersContainer.getInstance();
        ObservableList<SlicerParametersFile> userProfiles = SlicerParametersContainer.getUserProfileList();
        ObservableList<SlicerParametersFile> completeProfiles = SlicerParametersContainer.getCompleteProfileList();
        SlicerParametersFile draftSlicerParametersFile = SlicerParametersContainer.getSettings(
            ApplicationConfiguration.draftSettingsProfileName, HeadType.SINGLE_MATERIAL_HEAD);

        SlicerParametersFile draftCopy = draftSlicerParametersFile.clone();
        draftCopy.setProfileName(NEW_NAME);

        SlicerParametersContainer.saveProfile(draftCopy);
        assertEquals(1, userProfiles.size());

        SlicerParametersContainer.deleteUserProfile(NEW_NAME, HeadType.SINGLE_MATERIAL_HEAD);
        assertEquals(0, userProfiles.size());
        assertEquals(8, completeProfiles.size());
        SlicerParametersFile retrievedProfile = SlicerParametersContainer.getSettings(
            NEW_NAME, HeadType.SINGLE_MATERIAL_HEAD);
        Assert.assertNull(retrievedProfile);

    }

    @Test
    public void testCreateNewProfileAndChangeAndSave()
    {
        String NEW_NAME = "draftCopy1";
        SlicerParametersContainer.getInstance();
        ObservableList<SlicerParametersFile> userProfiles = SlicerParametersContainer.getUserProfileList();
        ObservableList<SlicerParametersFile> completeProfiles = SlicerParametersContainer.getCompleteProfileList();
        SlicerParametersFile draftSlicerParametersFile = SlicerParametersContainer.getSettings(ApplicationConfiguration.draftSettingsProfileName, HeadType.SINGLE_MATERIAL_HEAD);

        SlicerParametersFile draftCopy = draftSlicerParametersFile.clone();
        draftCopy.setProfileName(NEW_NAME);

        SlicerParametersContainer.saveProfile(draftCopy);

        draftCopy.setBrimWidth_mm(10);
        SlicerParametersContainer.saveProfile(draftCopy);

        SlicerParametersContainer.reload();
        SlicerParametersFile newEditedProfile = SlicerParametersContainer.getSettings(NEW_NAME, HeadType.SINGLE_MATERIAL_HEAD);
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
        SlicerParametersFile draftSlicerParametersFile = SlicerParametersContainer.getSettings(ApplicationConfiguration.draftSettingsProfileName, HeadType.SINGLE_MATERIAL_HEAD);

        SlicerParametersFile draftCopy = draftSlicerParametersFile.clone();
        draftCopy.setProfileName(NEW_NAME);

        SlicerParametersContainer.saveProfile(draftCopy);

        draftCopy.setBrimWidth_mm(5);
        String CHANGED_NAME = "draftCopy2";
        draftCopy.setProfileName(CHANGED_NAME);
        SlicerParametersContainer.saveProfile(draftCopy);
        assertEquals(1, userProfiles.size());
        assertEquals(9, completeProfiles.size());

        SlicerParametersContainer.reload();
        assertEquals(1, userProfiles.size());
        assertEquals(9, completeProfiles.size());        
        SlicerParametersFile newEditedProfile = SlicerParametersContainer.getSettings(
            CHANGED_NAME, HeadType.SINGLE_MATERIAL_HEAD);
        assertEquals(5, newEditedProfile.getBrimWidth_mm());
        assertNotSame(draftCopy, newEditedProfile);

    }    

}
