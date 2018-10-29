package celtech.coreUI.controllers.panels.userpreferences;

import celtech.Lookup;
import celtech.configuration.UserPreferences;
import celtech.coreUI.controllers.panels.PreferencesInnerPanelController;
import celtech.roboxbase.comms.DetectedDevice;
import celtech.roboxbase.comms.RoboxCommsManager;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import java.util.Optional;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;

/**
 *
 * @author George Salter
 */
public class CustomPrinterHeadPreference implements PreferencesInnerPanelController.Preference {

    private final ComboBox<String> control;
    private final UserPreferences userPreferences;
    
    public CustomPrinterHeadPreference(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
        
        control = new ComboBox<>();
        control.getStyleClass().add("cmbCleanCombo");
        control.setPrefWidth(200);
        HeadContainer.getCompleteHeadList().forEach(headFile -> control.getItems().add(headFile.getTypeCode()));
        control.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> updateValueFromControl());
    }
    
    @Override
    public void updateValueFromControl() {
        RoboxCommsManager comms = RoboxCommsManager.getInstance();
        
        String headType = control.getSelectionModel().selectedItemProperty().get();
        userPreferences.setCustomPrinterHead(headType);
        comms.setDummyPrinterHeadType(headType);
        
        Optional<DetectedDevice> dummyPrinterHandle = comms.getDummyPrinter(RoboxCommsManager.CUSTOM_CONNECTION_HANDLE);
        if(dummyPrinterHandle.isPresent()) {
            comms.removeDummyPrinter(dummyPrinterHandle.get());
            comms.addDummyPrinter(true);
        }
    }

    @Override
    public void populateControlWithCurrentValue() {
        control.getSelectionModel().select(userPreferences.getCustomPrinterHead());
    }

    @Override
    public Control getControl() {
        return control;
    }

    @Override
    public String getDescription() {
        return Lookup.i18n("preferences.printerHead");
    }

    @Override
    public void disableProperty(ObservableValue<Boolean> disableProperty) {
        control.disableProperty().unbind();
        control.disableProperty().bind(disableProperty);
    }
    
}
