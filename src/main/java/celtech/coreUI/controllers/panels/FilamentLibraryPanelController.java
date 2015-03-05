package celtech.coreUI.controllers.panels;

import celtech.configuration.Filament;
import celtech.configuration.MaterialType;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.utils.DeDuplicator;
import java.net.URL;
import java.text.ParseException;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

public class FilamentLibraryPanelController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        ExtrasMenuPanelController.class.getName());

    enum State
    {

        /**
         * Editing a new profile that has not yet been saved
         */
        NEW,
        /**
         * Editing a custom profile
         */
        CUSTOM,
        /**
         * Viewing a standard profile
         */
        ROBOX
    };

    private final ObjectProperty<State> state = new SimpleObjectProperty<>();
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    private final BooleanProperty isEditable = new SimpleBooleanProperty(false);
    private final BooleanProperty canSave = new SimpleBooleanProperty(false);
    private final BooleanProperty canDelete = new SimpleBooleanProperty(false);

    private String currentFilamentID;
    private final ObservableList<Filament> allFilaments = FXCollections.observableArrayList();
    private ObservableList<Filament> comboItems;

    @FXML
    private ComboBox<Filament> cmbFilament;

    @FXML
    private RestrictedNumberField bedTemperature;

    @FXML
    private RestrictedNumberField firstLayerBedTemperature;

    @FXML
    private RestrictedNumberField nozzleTemperature;

    @FXML
    private RestrictedNumberField ambientTemperature;

    @FXML
    private ColorPicker colour;

    @FXML
    private RestrictedNumberField firstLayerNozzleTemperature;

    @FXML
    private ComboBox<MaterialType> material;

    @FXML
    private RestrictedNumberField filamentDiameter;

    @FXML
    private RestrictedNumberField feedRateMultiplier;

    @FXML
    private TextField name;

    @FXML
    private TextField filamentID;

    @FXML
    private RestrictedNumberField filamentMultiplier;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        canSave.bind(isDirty.and(
            state.isEqualTo(State.NEW).
            or(state.isEqualTo(State.CUSTOM))));

        canDelete.bind(state.isNotEqualTo(State.ROBOX));

        isEditable.bind(state.isNotEqualTo(State.ROBOX));

        for (MaterialType materialType : MaterialType.values())
        {
            material.getItems().add(materialType);
        }

        setupWidgetEditableBindings();

        setupWidgetChangeListeners();

        setupFilamentCombo();

        selectFirstFilament();
    }

    private void selectFirstFilament()
    {
        cmbFilament.setValue(cmbFilament.getItems().get(0));
    }

    private void setupFilamentCombo()
    {
        cmbFilament.setCellFactory((ListView<Filament> param) -> new FilamentCell());

        repopulateCmbFilament();

        cmbFilament.valueProperty().addListener(
            (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
            {
                selectFilament(newValue);
            });
    }

    private void repopulateCmbFilament()
    {
        try
        {
            allFilaments.clear();
            allFilaments.addAll(FilamentContainer.getAppFilamentList());
            allFilaments.addAll(FilamentContainer.getUserFilamentList().sorted(
                (Filament o1, Filament o2)
                -> o1.getFriendlyFilamentName().compareTo(o2.getFriendlyFilamentName())));
            comboItems = FXCollections.observableArrayList(allFilaments);
            cmbFilament.setItems(comboItems);
        } catch (NoClassDefFoundError exception)
        {
            // this should only happen in SceneBuilder            
        }
    }

    private void clearWidgets()
    {
        name.setText("");
        filamentID.setText("");
//        material.getSelectionModel().select(filament.getMaterial());
        filamentDiameter.floatValueProperty().set(0f);
        filamentMultiplier.floatValueProperty().set(0f);

        feedRateMultiplier.floatValueProperty().set(0f);
        ambientTemperature.intValueProperty().set(0);
        firstLayerBedTemperature.intValueProperty().set(0);
        bedTemperature.intValueProperty().set(0);
        firstLayerNozzleTemperature.intValueProperty().set(0);
        nozzleTemperature.intValueProperty().set(0);
//        colour.setValue(filament.getDisplayColour());
        isDirty.set(false);
    }

    private void setupWidgetChangeListeners()
    {
        name.textProperty().addListener(dirtyStringListener);
        colour.valueProperty().asString().addListener(dirtyStringListener);
        material.valueProperty().addListener(dirtyMaterialTypeListener);
        filamentDiameter.textProperty().addListener(dirtyStringListener);
        filamentMultiplier.textProperty().addListener(dirtyStringListener);
        feedRateMultiplier.textProperty().addListener(dirtyStringListener);
        firstLayerBedTemperature.textProperty().addListener(dirtyStringListener);
        bedTemperature.textProperty().addListener(dirtyStringListener);
        firstLayerNozzleTemperature.textProperty().addListener(dirtyStringListener);
        nozzleTemperature.textProperty().addListener(dirtyStringListener);
        ambientTemperature.textProperty().addListener(dirtyStringListener);
    }

    private void setupWidgetEditableBindings()
    {
        filamentID.disableProperty().bind(isEditable.not());
        bedTemperature.disableProperty().bind(isEditable.not());
        firstLayerNozzleTemperature.disableProperty().bind(isEditable.not());
        colour.disableProperty().bind(isEditable.not());
        material.disableProperty().bind(isEditable.not());
        filamentDiameter.disableProperty().bind(isEditable.not());
        filamentMultiplier.disableProperty().bind(isEditable.not());
        feedRateMultiplier.disableProperty().bind(isEditable.not());
        firstLayerBedTemperature.disableProperty().bind(isEditable.not());
        name.disableProperty().bind(isEditable.not());
        nozzleTemperature.disableProperty().bind(isEditable.not());
        ambientTemperature.disableProperty().bind(isEditable.not());
    }

    private final ChangeListener<String> dirtyStringListener
        = (ObservableValue<? extends String> ov, String t, String t1) ->
        {
            isDirty.set(true);
        };

    private final ChangeListener<MaterialType> dirtyMaterialTypeListener
        = (ObservableValue<? extends MaterialType> ov, MaterialType t, MaterialType t1) ->
        {
            isDirty.set(true);
        };

    private void selectFilament(Filament filament)
    {
        currentFilamentID = filament.getFilamentID();
        updateWidgets(filament);
        if (currentFilamentID.startsWith("U"))
        {
            state.set(State.CUSTOM);
        } else
        {
            state.set(State.ROBOX);
        }
    }

    public void updateWidgets(Filament filament)
    {
        name.setText(filament.getFriendlyFilamentName());
        filamentID.setText(filament.getFilamentID());
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
        isDirty.set(false);
    }

    /**
     * Construct a new Filament from the contents of the widgets. If filamentID is null then a new
     * one is generated.
     */
    public Filament getFilament(String filamentID)
    {
        Filament filamentToReturn = null;

        try
        {
            filamentToReturn = new Filament(
                name.getText(),
                material.getSelectionModel().getSelectedItem(),
                filamentID,
                filamentDiameter.getAsFloat(),
                filamentMultiplier.getAsFloat(),
                feedRateMultiplier.getAsFloat(),
                ambientTemperature.getAsInt(),
                firstLayerBedTemperature.getAsInt(),
                bedTemperature.getAsInt(),
                firstLayerNozzleTemperature.getAsInt(),
                nozzleTemperature.getAsInt(),
                colour.getValue(),
                false);
        } catch (ParseException ex)
        {
            steno.error("Error parsing filament data : " + ex);
        }

        return filamentToReturn;
    }

    private void validateMaterialName(String name)
    {
        boolean invalid = false;

        if (name.equals(""))
        {
            invalid = true;
        } else
        {
            ObservableList<Filament> existingMaterialList = FilamentContainer.getUserFilamentList();
            for (Filament material : existingMaterialList)
            {
                if (material.getFriendlyFilamentName().equals(name))
                {
                    invalid = true;
                    break;
                }
            }
        }
    }

    void whenSavePressed()
    {
        assert (state.get() != State.ROBOX);
        Filament filament = getFilament(currentFilamentID);
        FilamentContainer.saveFilament(filament);
        repopulateCmbFilament();
        cmbFilament.setValue(FilamentContainer.getFilamentByID(filament.getFilamentID()));
    }

    void whenNewPressed()
    {
        state.set(State.NEW);
        clearWidgets();
        currentFilamentID = null;
    }

    void whenCopyPressed()
    {
        Filament filament = getFilament(null);
        Set<String> allCurrentNames = new HashSet<>();
        allFilaments.forEach((Filament filament1) ->
        {
            allCurrentNames.add(filament1.getFriendlyFilamentName());
        });
        String newName = DeDuplicator.suggestNonDuplicateName(filament.getFriendlyFilamentName(),
                                                              allCurrentNames);
        filament.setFriendlyFilamentName(newName);
        FilamentContainer.saveFilament(filament);
        repopulateCmbFilament();
        cmbFilament.setValue(FilamentContainer.getFilamentByID(filament.getFilamentID()));
    }

    void whenDeletePressed()
    {
        if (state.get() != State.NEW)
        {
            FilamentContainer.deleteFilament(FilamentContainer.getFilamentByID(currentFilamentID));
        }
        repopulateCmbFilament();
        clearWidgets();
        selectFirstFilament();
    }

    public ReadOnlyBooleanProperty getCanSave()
    {
        return canSave;
    }

    ReadOnlyBooleanProperty getCanDelete()
    {
        return canDelete;
    }

    public class FilamentCell extends ListCell<Filament>
    {

        private int SWATCH_SQUARE_SIZE = 16;

        HBox cellContainer;
        Rectangle rectangle = new Rectangle();
        Label label;

        public FilamentCell()
        {
            cellContainer = new HBox();
            cellContainer.setAlignment(Pos.CENTER_LEFT);
            rectangle = new Rectangle(SWATCH_SQUARE_SIZE, SWATCH_SQUARE_SIZE);
            label = new Label();
            cellContainer.getChildren().addAll(rectangle, label);
        }

        @Override
        protected void updateItem(Filament item, boolean empty)
        {
            super.updateItem(item, empty);
            if (item != null && !empty)
            {
                Filament filament = (Filament) item;
                setGraphic(cellContainer);
                rectangle.setFill(filament.getDisplayColour());

                label.setText(filament.getLongFriendlyName() + " "
                    + filament.getMaterial().getFriendlyName());
                label.getStyleClass().add("filamentSwatchPadding");
            } else
            {
                setGraphic(null);
            }
        }
    }

}
