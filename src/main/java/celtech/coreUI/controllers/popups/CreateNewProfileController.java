/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.popups;

import celtech.CoreTest;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintProfileContainer;
import celtech.coreUI.controllers.utilityPanels.ProfileDetailsController;
import celtech.services.slicer.SlicerSettings;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.controlsfx.control.textfield.CustomTextField;

/**
 *
 * @author Ian
 */
public class CreateNewProfileController implements Initializable
{

    @FXML
    ProfileDetailsController profileDetailsController;

    @FXML
    private VBox container;

    @FXML
    private CustomTextField profileNameField;

    private BooleanProperty profileNameInvalid = new SimpleBooleanProperty(true);

    private final Image redcrossImage = new Image(CoreTest.class.getResource(ApplicationConfiguration.imageResourcePath + "redcross.png").toExternalForm());
    private final ImageView redcrossHolder = new ImageView(redcrossImage);

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        profileNameField.setRight(redcrossHolder);

        profileNameField.getRight().visibleProperty().bind(profileNameInvalid);

        profileNameField.textProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
            {
                validateProfileName();
            }
        });
    }

    private void validateProfileName()
    {
        boolean invalid = false;
        String profileNameText = profileNameField.getText();

        if (profileNameText.equals(""))
        {
            invalid = true;
        } else
        {
            ObservableList<SlicerSettings> existingProfileList = PrintProfileContainer.getUserProfileList();
            for (SlicerSettings settings : existingProfileList)
            {
                if (settings.getProfileName().equals(profileNameText))
                {
                    invalid = true;
                    break;
                }
            }
        }
        profileNameInvalid.set(invalid);
    }

    public void updateProfileData(SlicerSettings slicerSettings)
    {
        profileNameField.setText(slicerSettings.getProfileName());
        profileDetailsController.updateProfileData(slicerSettings);
    }

    public void showButtons(boolean b)
    {
        profileDetailsController.showButtons(b);
    }

    public SlicerSettings getProfileData()
    {
        return profileDetailsController.getProfileData();
    }
    
    public ReadOnlyBooleanProperty getProfileNameInvalidProperty()
    {
        return profileNameInvalid;
    }
    
    public String getProfileName()
    {
        return profileNameField.getText();
    }
}
