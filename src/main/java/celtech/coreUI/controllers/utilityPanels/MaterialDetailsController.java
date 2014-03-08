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

import celtech.configuration.Filament;
import celtech.configuration.FilamentContainer;
import celtech.coreUI.components.RestrictedTextField;
import celtech.utils.FXUtils;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

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
    private RestrictedTextField filename;

    @FXML
    private RestrictedTextField bedTemperature;

    @FXML
    private RestrictedTextField reelID;

    @FXML
    private RestrictedTextField firstLayerNozzleTemperature;

    @FXML
    private ColorPicker colour;

    @FXML
    private RestrictedTextField material;

    @FXML
    private RestrictedTextField filamentDiameter;

    @FXML
    private RestrictedTextField firstLayerBedTemperature;

    @FXML
    private RestrictedTextField name;

    @FXML
    private RestrictedTextField nozzleTemperature;

    @FXML
    private RestrictedTextField ambientTemperature;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    @FXML
    void saveMaterialData(ActionEvent event)
    {
        final Filament filamentToSave = getMaterialData();
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                FilamentContainer.saveFilament(filamentToSave);
            }
        });
    }

    @FXML
    void cancelEdit(ActionEvent event)
    {
        if (lastFilamentUpdate != null)
        {
            updateMaterialData(lastFilamentUpdate);
        }
    }

    private BooleanProperty isDirty = new SimpleBooleanProperty(false);
    private BooleanProperty isMutable = new SimpleBooleanProperty(false);
    private BooleanProperty showButtons = new SimpleBooleanProperty(true);

    private StringConverter<Integer> intConverter = FXUtils.getIntConverter();
    private StringConverter<Float> floatConverter = FXUtils.getFloatConverter(2);

    private ChangeListener<String> dirtyStringListener = new ChangeListener<String>()
    {
        @Override
        public void changed(ObservableValue<? extends String> ov, String t, String t1)
        {
            isDirty.set(true);
        }
    };

    private Filament lastFilamentUpdate = null;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        saveButton.visibleProperty().bind(isDirty.and(showButtons));
        cancelButton.visibleProperty().bind(isDirty.and(showButtons));

        container.disableProperty().bind(isMutable.not());

        filename.textProperty().addListener(dirtyStringListener);
        bedTemperature.textProperty().addListener(dirtyStringListener);
        reelID.textProperty().addListener(dirtyStringListener);
        firstLayerNozzleTemperature.textProperty().addListener(dirtyStringListener);
        colour.valueProperty().asString().addListener(dirtyStringListener);
        material.textProperty().addListener(dirtyStringListener);
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
            filename.setText(filament.getFileName());
            reelID.setText(filament.getReelTypeCode());
            name.setText(filament.getFriendlyFilamentName());
            material.setText(filament.getMaterial());
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
        return new Filament(filename.getText(),
                reelID.getText(),
                name.getText(),
                material.getText(),
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
    
    public void showButtons(boolean show)
    {
        showButtons.set(show);
    }
}
