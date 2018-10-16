package celtech.coreUI.controllers.panels.userpreferences;

import celtech.Lookup;
import celtech.roboxbase.configuration.SlicerType;
import celtech.configuration.UserPreferences;
import celtech.coreUI.controllers.panels.PreferencesInnerPanelController;
import celtech.roboxbase.ApplicationFeature;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.BaseConfiguration.ApplicationFeatureListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.ListView;

/**
 *
 * @author Ian
 */
public class SlicerTypePreference implements PreferencesInnerPanelController.Preference
{

    private final ComboBox<SlicerType> control;
    private final UserPreferences userPreferences;
    private final ObservableList<SlicerType> slicerTypes = FXCollections.observableArrayList();
    
    private final ApplicationFeatureListener latestCuraApplicationFeatureListener = new ApplicationFeatureListener() {
        
        @Override
        public void onFeatureEnabled(ApplicationFeature applicationFeature) {
            if(applicationFeature.equals(ApplicationFeature.LATEST_CURA_VERSION)) {
                control.setItems(slicerTypes);
            }
        }

        @Override
        public void onFeatureDisabled(ApplicationFeature applicationFeature) {
            if(applicationFeature.equals(ApplicationFeature.LATEST_CURA_VERSION)) {
                control.setItems(slicerTypes);
            }
        }
    };

    public SlicerTypePreference(UserPreferences userPreferences)
    {
        this.userPreferences = userPreferences;

        control = new ComboBox<>();
        control.getStyleClass().add("cmbCleanCombo");
        
        slicerTypes.add(SlicerType.Cura);
        slicerTypes.add(SlicerType.Cura3);
        control.setItems(slicerTypes);
        control.setPrefWidth(150);
        control.setMinWidth(control.getPrefWidth());
        control.setCellFactory((ListView<SlicerType> param) -> new SlicerTypeCell());
        control.valueProperty()
                .addListener((ObservableValue<? extends SlicerType> observable, SlicerType oldValue, SlicerType newValue) -> {
                    updateValueFromControl();
                });
        BaseConfiguration.addApplicationFeatureListener(latestCuraApplicationFeatureListener);
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
