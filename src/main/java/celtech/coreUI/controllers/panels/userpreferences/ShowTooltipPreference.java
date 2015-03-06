package celtech.coreUI.controllers.panels.userpreferences;

import celtech.Lookup;
import celtech.configuration.UserPreferences;
import celtech.coreUI.controllers.panels.PreferencesInnerPanelController;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;

/**
 *
 * @author Ian
 */
public class ShowTooltipPreference implements PreferencesInnerPanelController.Preference
{
    private final CheckBox control;
    private final UserPreferences userPreferences;

    public ShowTooltipPreference(UserPreferences userPreferences)
    {
        this.userPreferences = userPreferences;
        
        control = new CheckBox();
        control.setPrefWidth(150);
        control.setMinWidth(control.getPrefWidth());
        control.selectedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                updateValueFromControl();
            });
    }

    @Override
    public void updateValueFromControl()
    {
        boolean showTooltips = control.isSelected();
        userPreferences.setShowTooltips(showTooltips);
    }

    @Override
    public void populateControlWithCurrentValue()
    {
        control.setSelected(userPreferences.isShowTooltips());
    }

    @Override
    public Control getControl()
    {
        return control;
    }

    @Override
    public String getDescription()
    {
        return Lookup.i18n("preferences.showTooltips");
    }
}
