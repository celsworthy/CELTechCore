package celtech.coreUI.controllers.panels.userpreferences;

import celtech.Lookup;
import celtech.roboxbase.configuration.SlicerType;
import celtech.configuration.UserPreferences;
import celtech.coreUI.controllers.panels.PreferencesInnerPanelController;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
        ObservableList<SlicerType> slicerTypes = FXCollections.observableArrayList();
        slicerTypes.add(SlicerType.Cura);
        control.setItems(slicerTypes);
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
        SlicerType chosenType = userPreferences.getSlicerType();
        if (chosenType == SlicerType.Slic3r)
        {
            chosenType = SlicerType.Cura;
        }
        control.setValue(chosenType);
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

    @Override
    public void disableProperty(ObservableValue<Boolean> disableProperty)
    {
        control.disableProperty().unbind();
        control.disableProperty().bind(disableProperty);
    }
}
