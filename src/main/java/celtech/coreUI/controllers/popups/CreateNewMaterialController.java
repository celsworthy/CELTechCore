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
package celtech.coreUI.controllers.popups;

import celtech.CoreTest;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.FilamentContainer;
import celtech.coreUI.controllers.utilityPanels.MaterialDetailsController;
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
public class CreateNewMaterialController implements Initializable
{

    @FXML
    MaterialDetailsController materialDetailsController;

    @FXML
    private VBox container;

    @FXML
    private CustomTextField materialNameField;

    private BooleanProperty materialNameInvalid = new SimpleBooleanProperty(true);

    private final Image redcrossImage = new Image(CoreTest.class.getResource(ApplicationConfiguration.imageResourcePath + "redcross.png").toExternalForm());
    private final ImageView redcrossHolder = new ImageView(redcrossImage);

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        materialNameField.setRight(redcrossHolder);

        materialNameField.getRight().visibleProperty().bind(materialNameInvalid);

        materialNameField.textProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
            {
                validateMaterialName();
            }
        });
    }

    private void validateMaterialName()
    {
        boolean invalid = false;
        String profileNameText = materialNameField.getText();

        if (profileNameText.equals(""))
        {
            invalid = true;
        } else
        {
            ObservableList<Filament> existingMaterialList = FilamentContainer.getUserFilamentList();
            for (Filament material : existingMaterialList)
            {
                if (material.getFriendlyFilamentName().equals(profileNameText))
                {
                    invalid = true;
                    break;
                }
            }
        }
        materialNameInvalid.set(invalid);
    }

    /**
     *
     * @param material
     */
    public void updateMaterialData(Filament material)
    {
        materialNameField.setText(material.getFriendlyFilamentName());
        materialDetailsController.updateMaterialData(material);
    }

    /**
     *
     * @param b
     */
    public void showButtons(boolean b)
    {
        materialDetailsController.showButtons(b);
    }

    /**
     *
     * @return
     */
    public Filament getMaterialData()
    {
        return materialDetailsController.getMaterialData();
    }
    
    /**
     *
     * @return
     */
    public ReadOnlyBooleanProperty getProfileNameInvalidProperty()
    {
        return materialNameInvalid;
    }
    
    /**
     *
     * @return
     */
    public String getProfileName()
    {
        return materialNameField.getText();
    }
}
