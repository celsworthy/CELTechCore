package celtech.coreUI.controllers.panels.userpreferences;

import celtech.Lookup;
import celtech.roboxbase.configuration.SlicerType;
import celtech.configuration.UserPreferences;
import celtech.coreUI.controllers.panels.PreferencesInnerPanelController;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;

/**
 *
 * @author Ian
 */
public class SlicerTypePreference implements PreferencesInnerPanelController.Preference
{

    private final ComboBox<SlicerType> control;
    private final UserPreferences userPreferences;

    public SlicerTypePreference(UserPreferences userPreferences)
    {
        this.userPreferences = userPreferences;

        control = new ComboBox<>();
        control.getStyleClass().add("cmbCleanCombo");
        control.setItems(FXCollections.observableArrayList(SlicerType.values()));
        control.setPrefWidth(150);
        control.setMinWidth(control.getPrefWidth());
        control.valueProperty().addListener(
            (ObservableValue<? extends SlicerType> observable, SlicerType oldValue, SlicerType newValue) ->
            {
                updateValueFromControl();
            });
    }

    @Override
    public void updateValueFromControl()
    {
        SlicerType slicerType = control.getValue();
        userPreferences.setSlicerType(slicerType);
    }

    @Override
    public void populateControlWithCurrentValue()
    {
        control.setValue(userPreferences.getSlicerType());
    }

    @Override
    public Control getControl()
    {
        return control;
    }

    @Override
    public String getDescription()
    {
        return Lookup.i18n("preferences.slicerType");
    }
}
