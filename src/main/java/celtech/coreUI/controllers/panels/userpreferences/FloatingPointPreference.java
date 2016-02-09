package celtech.coreUI.controllers.panels.userpreferences;

import celtech.Lookup;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.controllers.panels.PreferencesInnerPanelController;
import javafx.beans.property.FloatProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Control;

/**
 *
 * @author Ian
 */
public class FloatingPointPreference implements PreferencesInnerPanelController.Preference
{

    private final RestrictedNumberField control;
    private final FloatProperty floatProperty;
    private final String caption;

    public FloatingPointPreference(FloatProperty floatProperty,
            int decimalPlaces,
            int digits,
            boolean negativeAllowed,
            String caption)
    {
        this.floatProperty = floatProperty;
        this.caption = caption;

        control = new RestrictedNumberField();
        control.setPrefWidth(150);
        control.setMinWidth(control.getPrefWidth());
        control.setAllowedDecimalPlaces(decimalPlaces);
        control.setAllowNegative(negativeAllowed);
        control.setMaxLength(digits);
        control.floatValueProperty().addListener((ObservableValue<? extends Number> ov, Number t, Number t1) ->
        {
            updateValueFromControl();
        });
    }

    @Override
    public void updateValueFromControl()
    {
        floatProperty.set(control.floatValueProperty().get());

        // User Preferences controls whether the property can be set - read back just in case our selection was overridden
        control.floatValueProperty().set(floatProperty.get());
    }

    @Override
    public void populateControlWithCurrentValue()
    {
        control.floatValueProperty().set(floatProperty.get());
    }

    @Override
    public Control getControl()
    {
        return control;
    }

    @Override
    public String getDescription()
    {
        return Lookup.i18n(caption);
    }
}
