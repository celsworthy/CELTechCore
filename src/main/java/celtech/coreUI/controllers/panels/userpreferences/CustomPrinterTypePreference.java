package celtech.coreUI.controllers.panels.userpreferences;

import celtech.Lookup;
import celtech.configuration.UserPreferences;
import celtech.coreUI.controllers.panels.PreferencesInnerPanelController;
import celtech.roboxbase.configuration.hardwarevariants.PrinterType;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;

/**
 *
 * @author George Salter
 */
public class CustomPrinterTypePreference implements PreferencesInnerPanelController.Preference {

    private final ComboBox<PrinterType> control;
    private final UserPreferences userPreferences;
    
    public CustomPrinterTypePreference(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
        
        control = new ComboBox<>();
        control.getStyleClass().add("cmbCleanCombo");
        control.setPrefWidth(200);
        control.getItems().setAll(PrinterType.values());
        control.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> updateValueFromControl());
    }
    
    @Override
    public void updateValueFromControl() {
        userPreferences.setCustomPrinterType(control.getSelectionModel().selectedItemProperty().get());
    }

    @Override
    public void populateControlWithCurrentValue() {
        control.getSelectionModel().select(userPreferences.getCustomPrinterType());
    }

    @Override
    public Control getControl() {
        return control;
    }

    @Override
    public String getDescription() {
        return Lookup.i18n("preferences.printerType");
    }

    @Override
    public void disableProperty(ObservableValue<Boolean> disableProperty) {
        control.disableProperty().unbind();
        control.disableProperty().bind(disableProperty);
    }
    
}
