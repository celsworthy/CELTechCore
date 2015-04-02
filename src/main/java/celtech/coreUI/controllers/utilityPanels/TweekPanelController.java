package celtech.coreUI.controllers.utilityPanels;

import celtech.CoreTest;
import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.configuration.MaterialType;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.controllers.popups.PopupCommandReceiver;
import celtech.coreUI.controllers.popups.PopupCommandTransmitter;
import java.net.URL;
import java.text.ParseException;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.control.textfield.CustomTextField;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class TweekPanelController implements Initializable
{
    
    private final Stenographer steno = StenographerFactory.getStenographer(TweekPanelController.class.getName());
    
    /**
     * Initialises the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
       
    }

}
