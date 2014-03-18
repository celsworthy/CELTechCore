/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


package celtech.coreUI.controllers.utilityPanels;

import celtech.CoreTest;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.FilamentContainer;
import celtech.configuration.MaterialType;
import celtech.configuration.PrintProfileContainer;
import celtech.coreUI.components.RestrictedTextField;
import celtech.services.slicer.SlicerSettings;
import celtech.utils.FXUtils;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
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
import javafx.util.StringConverter;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.control.textfield.CustomTextField;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class MaterialDetailsController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(MaterialDetailsController.class.getName());

    @FXML
    private VBox container;

    @FXML
    private HBox editingOptions;

    @FXML
    private HBox notEditingOptions;

    @FXML
    private HBox immutableOptions;

    @FXML
    private RestrictedTextField bedTemperature;

    @FXML
    private RestrictedTextField firstLayerNozzleTemperature;

    @FXML
    private ColorPicker colour;

    @FXML
    private ComboBox<MaterialType> material;

    @FXML
    private RestrictedTextField filamentDiameter;

    @FXML
    private RestrictedTextField firstLayerBedTemperature;

    @FXML
    private CustomTextField name;

    @FXML
    private RestrictedTextField nozzleTemperature;

    @FXML
    private RestrictedTextField ambientTemperature;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    @FXML
    void saveData(ActionEvent event)
    {
        final Filament filamentToSave = getMaterialData();
        FilamentContainer.saveFilament(filamentToSave);
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
//        if (commandReceiver != null)
//        {
//            commandReceiver.triggerSaveAs();
//        }
    }

    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
    private final BooleanProperty isMutable = new SimpleBooleanProperty(false);
    private final BooleanProperty showButtons = new SimpleBooleanProperty(true);

    private final StringConverter<Integer> intConverter = FXUtils.getIntConverter();
    private final StringConverter<Float> floatConverter = FXUtils.getFloatConverter(2);

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
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        name.setRight(redcrossHolder);
        name.getRight().visibleProperty().bind(materialNameInvalid);
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

        container.disableProperty().bind(isMutable.not());

        for (MaterialType materialType : MaterialType.values())
        {
            material.getItems().add(materialType);
        }

        bedTemperature.textProperty().addListener(dirtyStringListener);
        firstLayerNozzleTemperature.textProperty().addListener(dirtyStringListener);
        colour.valueProperty().asString().addListener(dirtyStringListener);
        material.valueProperty().addListener(dirtyMaterialTypeListener);
        filamentDiameter.textProperty().addListener(dirtyStringListener);
        firstLayerBedTemperature.textProperty().addListener(dirtyStringListener);
        name.textProperty().addListener(dirtyStringListener);
        nozzleTemperature.textProperty().addListener(dirtyStringListener);
        ambientTemperature.textProperty().addListener(dirtyStringListener);
    }

    public void updateMaterialData(Filament filament)
    {
        if (filament != null)
        {
            name.setText(filament.getFriendlyFilamentName());
            material.getSelectionModel().select(filament.getMaterial());
            filamentDiameter.setText(floatConverter.toString(filament.getDiameter()));
            //No extrusion rate here...        
            //No extrusion multiplier
            ambientTemperature.setText(intConverter.toString(filament.getAmbientTemperature()));
            firstLayerBedTemperature.setText(intConverter.toString(filament.getFirstLayerBedTemperature()));
            bedTemperature.setText(intConverter.toString(filament.getBedTemperature()));
            firstLayerNozzleTemperature.setText(intConverter.toString(filament.getFirstLayerNozzleTemperature()));
            nozzleTemperature.setText(intConverter.toString(filament.getNozzleTemperature()));
            colour.setValue(filament.getDisplayColour());
            isMutable.set(filament.isMutable());
            isDirty.set(false);
        }
        lastFilamentUpdate = filament;
    }

    public Filament getMaterialData()
    {
        return new Filament(
                name.getText(),
                material.getSelectionModel().getSelectedItem(),
                null,
                floatConverter.fromString(filamentDiameter.getText()),
                0.0f, //Extrusion rate
                1.0f, //Extrusion multiplier
                intConverter.fromString(ambientTemperature.getText()),
                intConverter.fromString(firstLayerBedTemperature.getText()),
                intConverter.fromString(bedTemperature.getText()),
                intConverter.fromString(firstLayerNozzleTemperature.getText()),
                intConverter.fromString(nozzleTemperature.getText()),
                colour.getValue(),
                isMutable.get()
        );
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

    public void showButtons(boolean show)
    {
        showButtons.set(show);
    }

    public ReadOnlyBooleanProperty getProfileNameInvalidProperty()
    {
        return materialNameInvalid;
    }
}
