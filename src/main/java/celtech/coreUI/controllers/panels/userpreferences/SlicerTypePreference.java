package celtech.coreUI.controllers.panels.userpreferences;

import celtech.Lookup;
import celtech.configuration.UserPreferences;
import celtech.coreUI.controllers.panels.PreferencesInnerPanelController;
import celtech.roboxbase.ApplicationFeature;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.licence.Licence;
import celtech.roboxbase.licence.LicenceType;
import celtech.roboxbase.licensing.LicenceManager;
import celtech.roboxbase.licensing.LicenceManager.LicenceChangeListener;
import java.util.Optional;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.ListView;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class SlicerTypePreference implements PreferencesInnerPanelController.Preference, LicenceChangeListener
{
    private static final Stenographer STENO = StenographerFactory.getStenographer(SlicerTypePreference.class.getName());

    private final ComboBox<SlicerType> control;
    private final UserPreferences userPreferences;
    private final ObservableList<SlicerType> slicerTypes = FXCollections.observableArrayList();

    private final ChangeListener<SlicerType> slicerTypeChangeListener = (observable, oldValue, newValue) -> {
        updateValueFromControl();
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
        control.valueProperty().addListener(slicerTypeChangeListener);
        
        LicenceManager.getInstance().addLicenceChangeListener(this);
    }

    @Override
    public void updateValueFromControl()
    {
        SlicerType slicerType = control.getValue();
        if(slicerType == null) 
        {
            STENO.warning("SlicerType from Slicer setting is null. Setting to default Cura");
            slicerType = SlicerType.Cura;
        } 
        else if (slicerType == SlicerType.Cura3)
        {
            if(!BaseConfiguration.isApplicationFeatureEnabled(ApplicationFeature.LATEST_CURA_VERSION)) 
            {
                BaseLookup.getSystemNotificationHandler().showPurchaseLicenseDialog();
                slicerType = SlicerType.Cura;
            }
        }
        
        // Remove and re-add the listener around setting the value so this method is not called twice.
        control.valueProperty().removeListener(slicerTypeChangeListener);
        control.setValue(slicerType);
        control.valueProperty().addListener(slicerTypeChangeListener);
        
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

    @Override
    public void onLicenceChange(Optional<Licence> licenceOption) {
        SlicerType currentSelection = control.getSelectionModel().getSelectedItem();
        // Reset list in order to invoke SlicerTypeCell updateItem method
        control.setItems(FXCollections.observableArrayList(SlicerType.Cura));
        control.setItems(slicerTypes);
       if (licenceOption.map(Licence::getLicenceType).orElse(LicenceType.AUTOMAKER_FREE) == LicenceType.AUTOMAKER_FREE) {
             currentSelection = SlicerType.Cura;
        }
        control.getSelectionModel().select(currentSelection);
    }
}
