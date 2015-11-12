package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.MaterialType;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.coreUI.components.RestrictedNumberField;
import static celtech.printerControl.comms.commands.ColourStringConverter.colourToString;
import static celtech.printerControl.comms.commands.ColourStringConverter.stringToColor;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesAdapter;
import celtech.utils.PrinterListChangesListener;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

public class FilamentLibraryPanelController implements Initializable, MenuInnerPanel
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            FilamentLibraryPanelController.class.getName());

    enum Fields
    {

        NAME("name"), COLOUR("colour"), AMBIENT_TEMP("ambientTemp"), MATERIAL("material"),
        DIAMETER("diameter"), MULTIPLIER("multiplier"), FEED_RATE_MULTIPLIER("feedRateMultiplier"),
        FIRST_LAYER_BED_TEMP("firstlayerBedTemp"), BED_TEMP("bedTemp"),
        FIRST_LAYER_NOZZLE_TEMP("firstLayerNozzleTemp"), NOZZLE_TEMP("nozzleTemp"),
        COST_GBP_PER_KG("costGBPPerKG"), REMAINING_FILAMENT_M("remainingFilamentM");

        private final String helpTextId;

        Fields(String helpTextId)
        {
            this.helpTextId = helpTextId;
        }

        String getHelpText()
        {
            return Lookup.i18n("filamentLibraryHelp." + helpTextId);
        }
    }

    private final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    enum State
    {

        /**
         * Editing a new profile that has not yet been saved.
         */
        NEW,
        /**
         * Editing a custom profile.
         */
        CUSTOM,
        /**
         * Viewing a standard profile.
         */
        ROBOX
    };

    private final ObjectProperty<State> state = new SimpleObjectProperty<>();
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    private final BooleanProperty isEditable = new SimpleBooleanProperty(false);
    private final BooleanProperty canSave = new SimpleBooleanProperty(false);
    private final BooleanProperty canSaveAs = new SimpleBooleanProperty(false);
    private final BooleanProperty canDelete = new SimpleBooleanProperty(false);
    private final BooleanProperty canWriteToReel1 = new SimpleBooleanProperty(false);
    private final BooleanProperty canWriteToReel2 = new SimpleBooleanProperty(false);
    /**
     * Indicates if the contents of all widgets is valid or not.
     */
    private final BooleanProperty isValid = new SimpleBooleanProperty(false);
    private final BooleanProperty isNameValid = new SimpleBooleanProperty(false);
    private final BooleanProperty isNozzleTempValid = new SimpleBooleanProperty(true);

    private String currentFilamentID;
    private final ObservableList<Filament> allFilaments = FXCollections.observableArrayList();
    private ObservableList<Filament> comboItems;
    private final ObjectProperty<Printer> currentPrinter = new SimpleObjectProperty<>();
    private final FilamentContainer filamentContainer = Lookup.getFilamentContainer();
    private PrinterListChangesListener listener;
    private Filament reel0Filament;
    private Filament reel1Filament;

    private Filament currentFilament;
    private Filament currentFilamentAsEdited;

    private StringProperty loadedFilamentID0 = new SimpleStringProperty();
    private StringProperty loadedFilamentID1 = new SimpleStringProperty();

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
    private RestrictedNumberField costGBPPerKG;

    @FXML
    private RestrictedNumberField remainingOnReelM;

    @FXML
    private RestrictedNumberField feedRateMultiplier;

    @FXML
    private TextField name;

    @FXML
    private TextField filamentID;

    @FXML
    private RestrictedNumberField filamentMultiplier;

    @FXML
    private TextArea helpText;

    @FXML
    private GridPane filamentsGridPane;
    
    private final String REMAINING_ON_REEL_UNCHANGED = "-";

    private ListChangeListener<EEPROMState> reelEEPROMChangeListener = new ListChangeListener<EEPROMState>()
    {
        @Override
        public void onChanged(ListChangeListener.Change<? extends EEPROMState> change)
        {
            updateWriteToReelBindings();
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {

        currentPrinter.bind(Lookup.getSelectedPrinterProperty());

        updateSaveBindings();

        canSaveAs.bind(state.isNotEqualTo(State.NEW));

        canDelete.bind(state.isNotEqualTo(State.ROBOX));

        isEditable.bind(state.isNotEqualTo(State.ROBOX));

        isValid.bind(isNameValid.and(isNozzleTempValid));

        updateWriteToReelBindings();
        currentPrinter.addListener(
                (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
                {
                    if (oldValue != null)
                    {
                        oldValue.getReelEEPROMStateProperty().removeListener(reelEEPROMChangeListener);
                    }

                    if (newValue != null)
                    {
                        newValue.getReelEEPROMStateProperty().addListener(reelEEPROMChangeListener);
                    }
                    updateWriteToReelBindings();
                    showReelsAtTopOfCombo();
                });

        for (MaterialType materialType : MaterialType.values())
        {
            material.getItems().add(materialType);
        }

        setupWidgetEditableBindings();

        setupWidgetChangeListeners();

        setupHelpTextListeners();

        setupFilamentCombo();

        selectFirstFilament();

        setupPrinterChangesListener();

        FXMLUtilities.addColonsToLabels(filamentsGridPane);
    }

    private void setupPrinterChangesListener()
    {
        listener = new PrinterListChangesAdapter()
        {

            @Override
            public void whenReelAdded(Printer printer, int reelIndex)
            {
                if (printer == currentPrinter.get())
                {
                    showReelsAtTopOfCombo();
                    updateWriteToReelBindings();
                }
            }

            @Override
            public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
            {
                if (printer == currentPrinter.get())
                {
                    showReelsAtTopOfCombo();
                    updateWriteToReelBindings();
                }
            }

            @Override
            public void whenReelChanged(Printer printer, Reel reel)
            {
                if (printer == currentPrinter.get())
                {
                    showReelsAtTopOfCombo();
                    updateWriteToReelBindings();
                }
            }

        };
        Lookup.getPrinterListChangesNotifier().addListener(listener);
    }

    private void updateSaveBindings()
    {
        if (currentFilament != null && currentFilament.equals(currentFilamentAsEdited))
        {
            canSave.unbind();
            canSave.setValue(false);
        } else
        {
            canSave.bind(isValid.and(isDirty).and(
                    state.isEqualTo(State.NEW).
                    or(state.isEqualTo(State.CUSTOM))));
        }
    }

    /**
     * This should be called whenever any field is edited or the reel is
     * changed.
     */
    private void updateWriteToReelBindings()
    {
        updatedLoadedFilamentIDs();

        canWriteToReel1.set(false);
        canWriteToReel2.set(false);

        if (currentPrinter.get() != null)
        {
            boolean filament0OfDifferentID = false;
            boolean filament1OfDifferentID = false;

            if (loadedFilamentID0.get() != null)
            {
                filament0OfDifferentID = !loadedFilamentID0.get().equals(currentFilamentID);
            }

            if (loadedFilamentID1.get() != null)
            {
                filament1OfDifferentID = !loadedFilamentID1.get().equals(currentFilamentID);
            }

            if ((currentPrinter.get().reelsProperty().containsKey(0)
                    && (filament0OfDifferentID || !currentFilament.equals(currentFilamentAsEdited)))
                    || currentPrinter.get().getReelEEPROMStateProperty().get(0) == EEPROMState.NOT_PROGRAMMED)
            {
                canWriteToReel1.set(true);
            }
            if ((currentPrinter.get().reelsProperty().containsKey(1)
                    && (filament1OfDifferentID || !currentFilament.equals(currentFilamentAsEdited)))
                    || currentPrinter.get().getReelEEPROMStateProperty().get(1) == EEPROMState.NOT_PROGRAMMED)
            {
                canWriteToReel2.set(true);
            }
        }
    }

    private void updatedLoadedFilamentIDs()
    {
        if (currentPrinter.get() == null)
        {
            loadedFilamentID0.set(null);
            loadedFilamentID1.set(null);
        } else
        {
            if (currentPrinter.get().reelsProperty().containsKey(0))
            {
                loadedFilamentID0.set(
                        currentPrinter.get().reelsProperty().get(0).filamentIDProperty().get());
            } else
            {
                loadedFilamentID0.set(null);
            }
            if (currentPrinter.get().reelsProperty().containsKey(1))
            {
                loadedFilamentID1.set(
                        currentPrinter.get().reelsProperty().get(1).filamentIDProperty().get());
            } else
            {
                loadedFilamentID1.set(null);
            }
        }
    }

    private void selectFirstFilament()
    {
        if (cmbFilament.getItems().size() > 0)
        {
            cmbFilament.setValue(cmbFilament.getItems().get(0));
        }
    }

    private void setupFilamentCombo()
    {
        cmbFilament.setCellFactory((ListView<Filament> param) -> new FilamentCell());
        cmbFilament.setButtonCell(cmbFilament.getCellFactory().call(null));

        repopulateCmbFilament();

        cmbFilament.valueProperty().addListener(
                (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
                {
                    if (newValue != null)
                    {
                        selectFilament(newValue);
                    }
                    updateWriteToReelBindings();
                });

        filamentContainer.getUserFilamentList().addListener(
                (ListChangeListener.Change<? extends Filament> c) ->
                {
                    repopulateCmbFilament();
                });
    }

    private void repopulateCmbFilament()
    {
        try
        {
            allFilaments.clear();
            allFilaments.addAll(filamentContainer.getAppFilamentList().sorted(
                    (Filament o1, Filament o2)
                    -> o1.getFriendlyFilamentName().compareTo(o2.getFriendlyFilamentName())));
            allFilaments.addAll(filamentContainer.getUserFilamentList().sorted(
                    (Filament o1, Filament o2)
                    -> o1.getFriendlyFilamentName().compareTo(o2.getFriendlyFilamentName())));
            comboItems = FXCollections.observableArrayList(allFilaments);
            cmbFilament.setItems(comboItems);
        } catch (NoClassDefFoundError exception)
        {
            // this should only happen in SceneBuilder            
        }
        showReelsAtTopOfCombo();
    }

    /**
     * Show each reel at the top of the combo, with a special icon at the end
     * indicating it is on the reel. Remove the filament from the regular list.
     * This method is called whenever a reel
     *
     */
    private void showReelsAtTopOfCombo()
    {
        reel0Filament = null;
        reel1Filament = null;
        if (currentPrinter.get() != null && currentPrinter.get().reelsProperty().containsKey(1))
        {
            showReelAtTopOfCombo(1);
        }
        if (currentPrinter.get() != null && currentPrinter.get().reelsProperty().containsKey(0))
        {
            showReelAtTopOfCombo(0);
        }
    }

    private void showReelAtTopOfCombo(int reelIndex)
    {
        String filamentId = currentPrinter.get().reelsProperty().get(reelIndex).filamentIDProperty().get();
        Filament filament = filamentContainer.getFilamentByID(filamentId);
        comboItems.remove(filament);
        if (reelIndex == 0)
        {
            reel0Filament = filament;
        } else
        {
            reel1Filament = filament;
        }
        comboItems.add(0, filament);
        cmbFilament.valueProperty().set(filament);

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
        costGBPPerKG.floatValueProperty().set(0);
        remainingOnReelM.floatValueProperty().set(0);
//        colour.setValue(filament.getDisplayColour());
        isDirty.set(false);
    }

    private void setupWidgetChangeListeners()
    {

        name.textProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) ->
                {
                    if (!validateMaterialName(newValue))
                    {
                        isNameValid.set(false);
                        name.pseudoClassStateChanged(ERROR, true);
                    } else
                    {
                        isNameValid.set(true);
                        name.pseudoClassStateChanged(ERROR, false);
                    }
                });

        name.textProperty().addListener(dirtyStringListener);
        colour.valueProperty().addListener(
                (ObservableValue<? extends Color> observable, Color oldValue, Color newValue) ->
                {
                    isDirty.set(true);
                });
        material.valueProperty().addListener(dirtyMaterialTypeListener);
        filamentDiameter.textProperty().addListener(dirtyStringListener);
        filamentMultiplier.textProperty().addListener(dirtyStringListener);
        feedRateMultiplier.textProperty().addListener(dirtyStringListener);
        firstLayerBedTemperature.textProperty().addListener(dirtyStringListener);
        bedTemperature.textProperty().addListener(dirtyStringListener);
        firstLayerNozzleTemperature.textProperty().addListener(dirtyStringListener);
        nozzleTemperature.textProperty().addListener(dirtyStringListener);
        ambientTemperature.textProperty().addListener(dirtyStringListener);
        costGBPPerKG.textProperty().addListener(dirtyStringListener);
        remainingOnReelM.textProperty().addListener(dirtyStringListener);
    }

    private void setupWidgetEditableBindings()
    {
        filamentID.setDisable(true);
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
        costGBPPerKG.disableProperty().bind(isEditable.not());
        remainingOnReelM.disableProperty().bind(isEditable.not());
    }

    private void showHelpText(Fields field)
    {
        helpText.setText(field.getHelpText());
    }

    private void setupHelpTextListeners()
    {
        name.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.NAME);
                });
        colour.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.COLOUR);
                });
        material.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.MATERIAL);
                });
        filamentDiameter.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.DIAMETER);
                });
        filamentMultiplier.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.MULTIPLIER);
                });
        feedRateMultiplier.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.FEED_RATE_MULTIPLIER);
                });
        firstLayerBedTemperature.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.FIRST_LAYER_BED_TEMP);
                });
        bedTemperature.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.BED_TEMP);
                });
        firstLayerNozzleTemperature.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.FIRST_LAYER_NOZZLE_TEMP);
                });
        nozzleTemperature.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.NOZZLE_TEMP);
                });
        ambientTemperature.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.AMBIENT_TEMP);
                });
        costGBPPerKG.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.COST_GBP_PER_KG);
                });
        remainingOnReelM.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.REMAINING_FILAMENT_M);
                });

        name.hoverProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.NAME);
                });
        colour.hoverProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.COLOUR);
                });
        material.hoverProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.MATERIAL);
                });
        filamentDiameter.hoverProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.DIAMETER);
                });
        filamentMultiplier.hoverProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.MULTIPLIER);
                });
        feedRateMultiplier.hoverProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.FEED_RATE_MULTIPLIER);
                });
        firstLayerBedTemperature.hoverProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.FIRST_LAYER_BED_TEMP);
                });
        bedTemperature.hoverProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.BED_TEMP);
                });
        firstLayerNozzleTemperature.hoverProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.FIRST_LAYER_NOZZLE_TEMP);
                });
        nozzleTemperature.hoverProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.NOZZLE_TEMP);
                });
        ambientTemperature.hoverProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.AMBIENT_TEMP);
                });
        remainingOnReelM.hoverProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.REMAINING_FILAMENT_M);
                });
        costGBPPerKG.hoverProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    showHelpText(Fields.COST_GBP_PER_KG);
                });

    }

    private final ChangeListener<String> dirtyStringListener
            = (ObservableValue<? extends String> ov, String t, String t1) ->
            {
                isDirty.set(true);
                currentFilamentAsEdited = currentFilament.clone();
                updateFilamentFromWidgets(currentFilamentAsEdited);
                updateWriteToReelBindings();
                updateSaveBindings();
            };

    private final ChangeListener<MaterialType> dirtyMaterialTypeListener
            = (ObservableValue<? extends MaterialType> ov, MaterialType t, MaterialType t1) ->
            {
                isDirty.set(true);
                currentFilamentAsEdited = currentFilament.clone();
                updateFilamentFromWidgets(currentFilamentAsEdited);
                updateWriteToReelBindings();
                updateSaveBindings();
            };

    private void selectFilament(Filament filament)
    {
        currentFilamentID = filament.getFilamentID();
        currentFilament = filament;
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
        costGBPPerKG.floatValueProperty().set(filament.getCostGBPPerKG());
        remainingOnReelM.textProperty().set(REMAINING_ON_REEL_UNCHANGED);
        isDirty.set(false);
    }

    /**
     * Update the given from the contents of the widgets.
     */
    public void updateFilamentFromWidgets(Filament filament)
    {
        try
        {
            filament.setFriendlyFilamentName(name.getText());
            filament.setMaterial(material.getSelectionModel().getSelectedItem());
            filament.setFilamentDiameter(filamentDiameter.getAsFloat());
            filament.setFilamentMultiplier(filamentMultiplier.getAsFloat());
            filament.setFeedRateMultiplier(feedRateMultiplier.getAsFloat());
            filament.setAmbientTemperature(ambientTemperature.getAsInt());
            filament.setFirstLayerBedTemperature(firstLayerBedTemperature.getAsInt());
            filament.setBedTemperature(bedTemperature.getAsInt());
            filament.setFirstLayerNozzleTemperature(firstLayerNozzleTemperature.getAsInt());
            filament.setNozzleTemperature(nozzleTemperature.getAsInt());
            Color webDomainColor = stringToColor(colourToString(colour.getValue()));
            filament.setDisplayColour(webDomainColor);
            filament.setCostGBPPerKG(costGBPPerKG.getAsFloat());

        } catch (ParseException ex)
        {
            steno.error("Error parsing filament data : " + ex);
        }

    }

    private boolean validateMaterialName(String name)
    {

        boolean valid = true;

        if (name.equals(""))
        {
            valid = false;
        } else if (currentFilamentID == null || currentFilamentID.startsWith("U"))
        {
            ObservableList<Filament> existingMaterialList = filamentContainer.getCompleteFilamentList();
            for (Filament existingMaterial : existingMaterialList)
            {
                if ((!existingMaterial.getFilamentID().equals(currentFilamentID))
                        && existingMaterial.getFriendlyFilamentName().equals(name))
                {
                    valid = false;
                    break;
                }
            }
        }
        return valid;
    }

    private float getRemainingFilament(int reelIndex)
    {
        return currentPrinter.get().reelsProperty().get(reelIndex).remainingFilamentProperty().get();
    }

    void whenSavePressed()
    {
        assert (state.get() != State.ROBOX);
        updateFilamentFromWidgets(currentFilament);
        String saveCurrentFilamentID = currentFilament.getFilamentID();
        filamentContainer.saveFilament(currentFilament);
        repopulateCmbFilament();
        cmbFilament.setValue(filamentContainer.getFilamentByID(saveCurrentFilamentID));
    }

    void whenNewPressed()
    {
        state.set(State.NEW);
        clearWidgets();
        currentFilamentID = null;
        currentFilament = currentFilament.clone();
        currentFilament.setFilamentID(Filament.generateUserFilamentID());
        filamentID.setText(currentFilament.getFilamentID());
    }

    void whenSaveAsPressed()
    {
        state.set(State.NEW);
        currentFilamentID = null;
        currentFilament = currentFilament.clone();
        currentFilament.setFilamentID(Filament.generateUserFilamentID());
        updateWidgets(currentFilament);
        name.requestFocus();
        name.selectAll();
        // visually marks name as needing to be changed
        name.pseudoClassStateChanged(ERROR, true);
    }

    void whenDeletePressed()
    {
        if (state.get() != State.NEW)
        {
            filamentContainer.deleteFilament(currentFilament);
        }
        repopulateCmbFilament();
        clearWidgets();
        selectFirstFilament();
    }

    void whenWriteToReel1Pressed()
    {
        try
        {
            Filament filament = cmbFilament.getValue();

            if (currentPrinter.get().getReelEEPROMStateProperty().get(0) == EEPROMState.NOT_PROGRAMMED)
            {
                currentPrinter.get().formatReelEEPROM(0);
            } else
            {

                if (isEditable.get())
                {
                    whenSavePressed();
                }
                float remainingFilament = getRemainingFilament(0);
                if (state.get() == State.CUSTOM && !remainingOnReelM.getText().equals(REMAINING_ON_REEL_UNCHANGED))
                {
                    try
                    {
                        remainingFilament = remainingOnReelM.getAsFloat() * 1000f;
                    } catch (ParseException ex)
                    {
                        steno.error("parsing remaining filament");
                    }
                }

                filament.setRemainingFilament(remainingFilament);
            }

            currentPrinter.get().transmitWriteReelEEPROM(0, filament);
        } catch (RoboxCommsException | PrinterException ex)
        {
            steno.error("Unable to write to Reel 0 " + ex);
        }
    }

    void whenWriteToReel2Pressed()
    {
        try
        {
            Filament filament = cmbFilament.getValue();

            if (currentPrinter.get().getReelEEPROMStateProperty().get(1) == EEPROMState.NOT_PROGRAMMED)
            {
                currentPrinter.get().formatReelEEPROM(1);
            } else
            {

                if (isEditable.get())
                {
                    whenSavePressed();
                }
                float remainingFilament = getRemainingFilament(1);
                if (state.get() == State.CUSTOM && !remainingOnReelM.getText().equals(REMAINING_ON_REEL_UNCHANGED))
                {
                    try
                    {
                        remainingFilament = remainingOnReelM.getAsFloat() * 1000f;
                    } catch (ParseException ex)
                    {
                        steno.error("parsing remaining filament");
                    }
                }

                filament.setRemainingFilament(remainingFilament);
            }

            currentPrinter.get().transmitWriteReelEEPROM(1, filament);
        } catch (RoboxCommsException | PrinterException ex)
        {
            steno.error("Unable to write to Reel 1 " + ex);
        }
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
        SVGPath reelIcon = new SVGPath();
        Label reelIndexLabel;

        public FilamentCell()
        {
            reelIcon.setContent(
                    "m 17.0867,23 c 0,-1.6092 1.3044,-2.9136 2.9133,-2.9136 1.6083,0 2.9127,1.3044 2.9133,2.9136 H 26 v -0.8517 c 0,-0.0678 -0.021,-0.129 -0.0627,-0.1836 -0.0417,-0.0546 -0.0966,-0.087 -0.1641,-0.0975 l -1.4298,-0.2187 c -0.078,-0.2499 -0.1851,-0.5052 -0.3204,-0.7656 0.0939,-0.1302 0.2343,-0.3138 0.4218,-0.5508 0.1875,-0.237 0.3204,-0.4098 0.3984,-0.5193 0.0417,-0.0573 0.0627,-0.1173 0.0627,-0.1797 0,-0.0729 -0.0183,-0.1305 -0.0546,-0.1719 -0.1875,-0.2658 -0.6177,-0.7083 -1.2891,-1.3281 -0.0627,-0.0522 -0.1278,-0.0783 -0.1956,-0.0783 -0.078,0 -0.1407,0.0234 -0.1875,0.0702 L 22.07,18.9608 C 21.8561,18.8516 21.6218,18.755 21.3668,18.6716 l -0.219,-1.4373 C 21.1424,17.1665 21.1133,17.1107 21.0581,17.0663 21.0038,17.0222 20.9399,17 20.8667,17 H 19.133 c -0.1512,0 -0.2448,0.0729 -0.2814,0.2187 -0.0681,0.2607 -0.1437,0.7449 -0.2268,1.4532 -0.2448,0.0786 -0.4815,0.1773 -0.7107,0.297 L 16.8353,18.1331 C 16.7681,18.0809 16.7,18.0548 16.6325,18.0548 c -0.1149,0 -0.3609,0.1863 -0.7383,0.5586 -0.378,0.3723 -0.6339,0.6522 -0.7692,0.8397 -0.0468,0.0678 -0.0708,0.1278 -0.0708,0.18 0,0.0624 0.0264,0.1248 0.0786,0.1875 0.3486,0.4215 0.6276,0.7812 0.8361,1.0779 -0.1308,0.2397 -0.2319,0.4794 -0.3048,0.7188 l -1.4538,0.2187 c -0.0567,0.0105 -0.1065,0.0444 -0.1482,0.1017 C 14.0204,21.9947 14,22.0547 14,22.1171 V 23 h 3.0867 z");
            cellContainer = new HBox();
            cellContainer.setAlignment(Pos.CENTER_LEFT);
            rectangle = new Rectangle(SWATCH_SQUARE_SIZE, SWATCH_SQUARE_SIZE);
            label = new Label();
            label.setId("cmbFilamentTextField");
            reelIndexLabel = new Label();
            reelIndexLabel.setId("cmbFilamentReelIndexLabel");
            cellContainer.getChildren().addAll(rectangle, label, reelIcon, reelIndexLabel);
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

                if (item == reel0Filament || item == reel1Filament)
                {
                    if (item == reel0Filament)
                    {
                        reelIndexLabel.setText("(1)");
                    } else
                    {
                        reelIndexLabel.setText("(2)");
                    }
                    reelIcon.setVisible(true);
                    reelIndexLabel.setVisible(true);
                } else
                {
                    reelIcon.setVisible(false);
                    reelIndexLabel.setVisible(false);
                }
            } else
            {
                setGraphic(null);
            }
        }
    }

    @Override
    public String getMenuTitle()
    {
        return "extrasMenu.filament";
    }

    @Override
    public List<MenuInnerPanel.OperationButton> getOperationButtons()
    {
        List<MenuInnerPanel.OperationButton> operationButtons = new ArrayList<>();

        MenuInnerPanel.OperationButton saveButton = new MenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "genericFirstLetterCapitalised.Save";
            }

            @Override
            public String getFXMLName()
            {
                return "saveButton";
            }

            @Override
            public String getTooltipTextId()
            {
                return "genericFirstLetterCapitalised.Save";
            }

            @Override
            public void whenClicked()
            {
                whenSavePressed();
            }

            @Override
            public BooleanProperty whenEnabled()
            {
                return canSave;
            }

        };
        operationButtons.add(saveButton);
        MenuInnerPanel.OperationButton saveAsButton = new MenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "genericFirstLetterCapitalised.SaveAs";
            }

            @Override
            public String getFXMLName()
            {
                return "saveAsButton";
            }

            @Override
            public String getTooltipTextId()
            {
                return "genericFirstLetterCapitalised.SaveAs";
            }

            @Override
            public void whenClicked()
            {
                whenSaveAsPressed();
            }

            @Override
            public BooleanProperty whenEnabled()
            {
                return canSaveAs;
            }

        };
        operationButtons.add(saveAsButton);
        MenuInnerPanel.OperationButton deleteButton = new MenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "genericFirstLetterCapitalised.Delete";
            }

            @Override
            public String getFXMLName()
            {
                return "deleteButton";
            }

            @Override
            public String getTooltipTextId()
            {
                return "genericFirstLetterCapitalised.Delete";
            }

            @Override
            public void whenClicked()
            {
                whenDeletePressed();
            }

            @Override
            public BooleanProperty whenEnabled()
            {
                return canDelete;
            }

        };
        operationButtons.add(deleteButton);
        MenuInnerPanel.OperationButton writeToReel1Button = new MenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "filamentLibrary.writeToReel1";
            }

            @Override
            public String getFXMLName()
            {
                return "writeToReel1Button";
            }

            @Override
            public String getTooltipTextId()
            {
                return "filamentLibrary.writeToReel1";
            }

            @Override
            public void whenClicked()
            {
                whenWriteToReel1Pressed();
            }

            @Override
            public BooleanProperty whenEnabled()
            {
                return canWriteToReel1;
            }

        };
        operationButtons.add(writeToReel1Button);
        MenuInnerPanel.OperationButton writeToReel2Button = new MenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "filamentLibrary.writeToReel2";
            }

            @Override
            public String getFXMLName()
            {
                return "writeToReel2Button";
            }

            @Override
            public String getTooltipTextId()
            {
                return "filamentLibrary.writeToReel2";
            }

            @Override
            public void whenClicked()
            {
                whenWriteToReel2Pressed();
            }

            @Override
            public BooleanProperty whenEnabled()
            {
                return canWriteToReel2;
            }

        };
        operationButtons.add(writeToReel2Button);

        return operationButtons;
    }

}
