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
public class MaterialDetailsController implements Initializable, PopupCommandTransmitter
{
    
    private final Stenographer steno = StenographerFactory.getStenographer(MaterialDetailsController.class.getName());
    private PopupCommandReceiver commandReceiver = null;
    
    @FXML
    private VBox container;
    
    @FXML
    private RestrictedNumberField bedTemperature;
    
    @FXML
    private HBox notEditingOptions;
    
    @FXML
    private RestrictedNumberField firstLayerBedTemperature;
    
    @FXML
    private RestrictedNumberField nozzleTemperature;
    
    @FXML
    private HBox editingOptions;
    
    @FXML
    private RestrictedNumberField ambientTemperature;
    
    @FXML
    private Button saveAsButton;
    
    @FXML
    private Button staticSaveAsButton;
    
    @FXML
    private ColorPicker colour;
    
    @FXML
    private RestrictedNumberField firstLayerNozzleTemperature;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private ComboBox<MaterialType> material;
    
    @FXML
    private RestrictedNumberField filamentDiameter;
    
    @FXML
    private HBox immutableOptions;
    
    @FXML
    private RestrictedNumberField feedRateMultiplier;
    
    @FXML
    private Button deleteProfileButton;
    
    @FXML
    private CustomTextField name;
    
    @FXML
    private Button saveButton;
    
    @FXML
    private RestrictedNumberField filamentMultiplier;
    
    @FXML
    void saveData(ActionEvent event)
    {
        final Filament filamentToSave = getMaterialData();
        if (commandReceiver != null)
        {
            commandReceiver.triggerSave(filamentToSave);
        }
        isDirty.set(false);
    }
    
    @FXML
    void cancelEdit(ActionEvent event)
    {
        if (lastFilamentUpdate != null)
        {
            updateMaterialData(lastFilamentUpdate);
        }
    }
    
    @FXML
    void deleteProfile(ActionEvent event)
    {
        final Filament filamentToDelete = getMaterialData();
        FilamentContainer.deleteFilament(filamentToDelete);
    }
    
    @FXML
    void launchSaveAsDialogue(ActionEvent event)
    {
        if (commandReceiver != null)
        {
            commandReceiver.triggerSaveAs(this);
        }
    }
    
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
    private final BooleanProperty isMutable = new SimpleBooleanProperty(false);
    private final BooleanProperty showButtons = new SimpleBooleanProperty(true);
    
    private final ChangeListener<String> dirtyStringListener = new ChangeListener<String>()
    {
        @Override
        public void changed(ObservableValue<? extends String> ov, String t, String t1)
        {
            isDirty.set(true);
        }
    };
    
    private final ChangeListener<MaterialType> dirtyMaterialTypeListener = new ChangeListener<MaterialType>()
    {
        @Override
        public void changed(ObservableValue<? extends MaterialType> ov, MaterialType t, MaterialType t1)
        {
            isDirty.set(true);
        }
    };
    
    private Filament lastFilamentUpdate = null;
    
    private BooleanProperty materialNameInvalid = new SimpleBooleanProperty(true);
    private final Image redcrossImage = new Image(CoreTest.class.getResource(ApplicationConfiguration.imageResourcePath + "redcross.png").toExternalForm());
    private final ImageView redcrossHolder = new ImageView(redcrossImage);

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        staticSaveAsButton.disableProperty().bind(Lookup.getUserPreferences().advancedModeProperty().not());
        
        name.setRight(redcrossHolder);
        name.getRight().visibleProperty().bind(materialNameInvalid.and(isDirty));
        name.textProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
            {
                validateMaterialName();
            }
        });
        
        editingOptions.visibleProperty().bind(isDirty.and(showButtons).and(isMutable));
        notEditingOptions.visibleProperty().bind(isDirty.not().and(showButtons).and(isMutable));
        immutableOptions.visibleProperty().bind(isDirty.not().and(showButtons).and(isMutable.not()));
        
        bedTemperature.disableProperty().bind(isMutable.not());
        firstLayerNozzleTemperature.disableProperty().bind(isMutable.not());
        colour.disableProperty().bind(isMutable.not());
        material.disableProperty().bind(isMutable.not());
        filamentDiameter.disableProperty().bind(isMutable.not());
        filamentMultiplier.disableProperty().bind(isMutable.not());
        feedRateMultiplier.disableProperty().bind(isMutable.not());
        firstLayerBedTemperature.disableProperty().bind(isMutable.not());
        name.disableProperty().bind(isMutable.not());
        nozzleTemperature.disableProperty().bind(isMutable.not());
        ambientTemperature.disableProperty().bind(isMutable.not());
        
        for (MaterialType materialType : MaterialType.values())
        {
            material.getItems().add(materialType);
        }
        
        bedTemperature.textProperty().addListener(dirtyStringListener);
        firstLayerNozzleTemperature.textProperty().addListener(dirtyStringListener);
        colour.valueProperty().asString().addListener(dirtyStringListener);
        material.valueProperty().addListener(dirtyMaterialTypeListener);
        filamentDiameter.textProperty().addListener(dirtyStringListener);
        filamentMultiplier.textProperty().addListener(dirtyStringListener);
        feedRateMultiplier.textProperty().addListener(dirtyStringListener);
        firstLayerBedTemperature.textProperty().addListener(dirtyStringListener);
        name.textProperty().addListener(dirtyStringListener);
        nozzleTemperature.textProperty().addListener(dirtyStringListener);
        ambientTemperature.textProperty().addListener(dirtyStringListener);
    }

    /**
     *
     * @param filament
     */
    public void updateMaterialData(Filament filament)
    {
        if (filament == null)
        {
            container.setVisible(false);
        } else
        {
            container.setVisible(true);
            name.setText(filament.getFriendlyFilamentName());
            material.getSelectionModel().select(filament.getMaterial());
            filamentDiameter.floatValueProperty().set(filament.getDiameter());
            filamentMultiplier.floatValueProperty().set(filament.getFilamentMultiplier());
            feedRateMultiplier.floatValueProperty().set(filament.getFeedRateMultiplier());
            ambientTemperature.intValueProperty().set(filament.getAmbientTemperature());
            firstLayerBedTemperature.intValueProperty().set(filament.getFirstLayerBedTemperature());
            bedTemperature.intValueProperty().set(filament.getBedTemperature());
            firstLayerNozzleTemperature.intValueProperty().set(filament.getFirstLayerNozzleTemperature());
            nozzleTemperature.intValueProperty().set(filament.getNozzleTemperature());
            colour.setValue(filament.getDisplayColour());
            isMutable.set(filament.isMutable());
            isDirty.set(false);
        }
        lastFilamentUpdate = filament;
    }

    /**
     *
     * @return
     */
    public Filament getMaterialData()
    {
        Filament filamentToReturn = null;
        
        try
        {
            filamentToReturn = new Filament(
                    name.getText(),
                    material.getSelectionModel().getSelectedItem(),
                    null,
                    filamentDiameter.getAsFloat(),
                    filamentMultiplier.getAsFloat(),
                    feedRateMultiplier.getAsFloat(),
                    ambientTemperature.getAsInt(),
                    firstLayerBedTemperature.getAsInt(),
                    bedTemperature.getAsInt(),
                    firstLayerNozzleTemperature.getAsInt(),
                    nozzleTemperature.getAsInt(),
                    colour.getValue(),
                0,
                    isMutable.get()
            );
        } catch (ParseException ex)
        {
            steno.error("Error parsing filament data : " + ex);
        }
        
        return filamentToReturn;
    }
    
    private void validateMaterialName()
    {
        boolean invalid = false;
        String profileNameText = name.getText();
        
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
     * @param show
     */
    public void showButtons(boolean show)
    {
        showButtons.set(show);
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
     * @param receiver
     */
    @Override
    public void provideReceiver(PopupCommandReceiver receiver)
    {
        commandReceiver = receiver;
    }
}
