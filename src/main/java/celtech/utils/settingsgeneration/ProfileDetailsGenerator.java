package celtech.utils.settingsgeneration;

import celtech.Lookup;
import celtech.coreUI.components.HideableTooltip;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.roboxbase.configuration.PrintProfileSetting;
import celtech.roboxbase.configuration.PrintProfileSettings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventType;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * Class to provide methods for adding generated settings to the FXML print profile pages.
 * 
 * @author George Salter
 */
public class ProfileDetailsGenerator {
    
    private static final Stenographer STENO = StenographerFactory.getStenographer(ProfileDetailsGenerator.class.getName());
    
    private final BooleanProperty isDirty;
    
    enum Nozzle {
        LEFT,
        RIGHT,
        SINGLE
    }
    
    private static final String FLOAT = "float";
    private static final String INT = "int";
    private static final String BOOLEAN = "boolean";
    private static final String OPTION = "option";
    private static final String EXTRUSION = "extrusion";
    
    private PrintProfileSettings printProfileSettings;
    
    public ProfileDetailsGenerator(PrintProfileSettings printProfileSettings, BooleanProperty isDirty) {
        this.printProfileSettings = printProfileSettings;
        this.isDirty = isDirty;
    }
    
    public void setPrintProfilesettings(PrintProfileSettings printProfileSettings) {
        this.printProfileSettings = printProfileSettings;
    }
    
    public void generateProfileSettingsForTab(GridPane gridPane) {
        clearGrid(gridPane);
        String gridId = gridPane.getId();
        List<PrintProfileSetting> profileSettingsForTab = printProfileSettings.getPrintProfileSettings().get(gridId);
        
        setupColumnsForGridPane(gridPane);
        
        int rowNumber = 0;
        
        for(PrintProfileSetting printProfileSetting : profileSettingsForTab) {
            gridPane.getRowConstraints().add(new RowConstraints());
            
            String valueType = printProfileSetting.getValueType();
            switch(valueType) {
                case FLOAT:
                    if(printProfileSetting.isPerExtruder() && printProfileSetting.getValue().contains(":")) {
                        addPerExtruderValueRow(gridPane, printProfileSetting, rowNumber);
                    } else {
                        addSingleFieldRow(gridPane, printProfileSetting, rowNumber);
                    }
                    rowNumber++;
                    break;
                case INT:
                    addSingleFieldRow(gridPane, printProfileSetting, rowNumber);
                    rowNumber++;
                    break;
                case BOOLEAN:
                    addCheckBoxRow(gridPane, printProfileSetting, rowNumber);
                    rowNumber++;
                case OPTION:
                    if(printProfileSetting.getOptions().isPresent()) {
                        addComboBoxRow(gridPane, printProfileSetting, rowNumber);
                        rowNumber++;
                    } else {
                        STENO.error("Option setting has no options, setting will be ignored");
                    }
                    break;
                case EXTRUSION:
                    addSelectionAndValueRow(gridPane, printProfileSetting, rowNumber);
                    rowNumber++;
                    break;
                default:
                    STENO.error("Value type of " + valueType + " not recognised, setting will be ignored");
            }
        }
    }
    
    /**
     * Clear the {@link GridPane} for regeneration.
     * 
     * @param gridPane 
     */
    private void clearGrid(GridPane gridPane) {
        gridPane.getChildren().clear();
        gridPane.getColumnConstraints().clear();
        gridPane.getRowConstraints().clear();
    }
    
    /**
     * Sets up the {@link ColumnConstraints} for the provided {@link GridPane}
     * This set up is for a clean and consistent setting generation.
     * 
     * @param gridPane the pane to set the columns up for 
     */
    private void setupColumnsForGridPane(GridPane gridPane) {
        ColumnConstraints label0Col = new ColumnConstraints();
        ColumnConstraints label1Col = new ColumnConstraints();
        ColumnConstraints field0Col = new ColumnConstraints();
        ColumnConstraints label2Col = new ColumnConstraints();
        ColumnConstraints field1Col = new ColumnConstraints();      
        
        label0Col.setPercentWidth(20);
        label1Col.setPercentWidth(15);
        field0Col.setPercentWidth(20);
        label2Col.setPercentWidth(15);
        field1Col.setPercentWidth(20);
        
        label0Col.setHalignment(HPos.LEFT);
        label1Col.setHalignment(HPos.RIGHT);
        label2Col.setHalignment(HPos.RIGHT);
        
        gridPane.getColumnConstraints().addAll(label0Col, label1Col, field0Col, label2Col, field1Col);
    }
    
    /**
     * Add a single field setting to the given {@link GridPane}
     * It consists of a {@link Label} and a {@link RestrictedNumberField}.
     * 
     * @param gridPane the pane to add to
     * @param printProfileSetting the setting parameters
     * @param row the row number of the grid to add to
     * @return the GridPane
     */
    protected GridPane addSingleFieldRow(GridPane gridPane, PrintProfileSetting printProfileSetting, int row) {
        gridPane.add(createLabelElement(printProfileSetting.getSettingName(), true), 0, row);
        gridPane.add(createInputFieldWithOptionalUnit(printProfileSetting, printProfileSetting.getValue(), Nozzle.SINGLE), 2, row);
        return gridPane;
    }
    
    /**
     * Add a combobox field setting to the given {@link GridPane}
     * It consists of a {@link Label} and a {@link ComboBox}.
     * 
     * @param gridPane the pane to add to
     * @param printProfileSetting the setting parameters
     * @param row the row number of the grid to add to
     * @return the GridPane
     */
    protected GridPane addComboBoxRow(GridPane gridPane, PrintProfileSetting printProfileSetting, int row) {
        gridPane.add(createLabelElement(printProfileSetting.getSettingName(), true), 0, row);
        gridPane.add(createComboBox(printProfileSetting), 2, row);
        return gridPane;
    }
    
    /**
     * Add a nozzle selection box and field setting to the given {@link GridPane}
     * It consists of a {@link Label} a {@link ComboBox} for selection of a nozzle
     * and a {@link RestrictedNumberField} for the value.
     * 
     * @param gridPane the pane to add to
     * @param printProfileSetting the setting parameters
     * @param row the row number of the grid to add to
     * @return the GridPane
     */
    protected GridPane addSelectionAndValueRow(GridPane gridPane, PrintProfileSetting printProfileSetting, int row) {
        gridPane.add(createLabelElement(printProfileSetting.getSettingName(), true), 0, row);
        gridPane.add(createLabelElement("extrusion.nozzle", true), 1, row);
        gridPane.add(createComboBox(printProfileSetting), 2, row);
        gridPane.add(createInputFieldWithOptionalUnit(printProfileSetting, printProfileSetting.getValue(), Nozzle.SINGLE), 4, row);
        return gridPane;
    }
    
    /**
     * Add a double field setting to the given {@link GridPane} for settings with
     * a value per nozzle.
     * It consists of a {@link Label} and two {@link RestrictedNumberField}
     * 
     * @param gridPane the pane to add to
     * @param printProfileSetting the setting parameters
     * @param row the row number of the grid to add to
     * @return the GridPane
     */
    protected GridPane addPerExtruderValueRow(GridPane gridPane, PrintProfileSetting printProfileSetting, int row) {
        String[] values = printProfileSetting.getValue().split(":");
        gridPane.add(createLabelElement(printProfileSetting.getSettingName(), true), 0, row);
        gridPane.add(createLabelElement("Left Nozzle", true), 1, row);
        gridPane.add(createInputFieldWithOptionalUnit(printProfileSetting, values[0], Nozzle.LEFT), 2, row);
        gridPane.add(createLabelElement("Right Nozzle", true), 3, row);
        gridPane.add(createInputFieldWithOptionalUnit(printProfileSetting, values[1], Nozzle.RIGHT), 4, row);
        return gridPane;
    }
    
    /**
     * Add a check box row to the given {@link GridPane}.
     * 
     * @param gridPane the pane to add to
     * @param printProfileSetting the setting parameters
     * @param row the row number of the grid to add to
     * @return the GridPane
     */
    protected GridPane addCheckBoxRow(GridPane gridPane, PrintProfileSetting printProfileSetting, int row) {
        gridPane.add(createLabelElement(printProfileSetting.getSettingName(), true), 0, row);
        gridPane.add(createCheckBoxElement(printProfileSetting), 2, row);
        return gridPane;
    }
    
    /**
     * Create a fxml {@link Label}.
     * 
     * @param labelText text to display as label
     * @param addColon if we want a colon added at the end
     * @return the Label
     */
    private Label createLabelElement(String labelText, boolean addColon) {
        Label label = new Label(Lookup.i18n(labelText));
        if(addColon) {
            label.getStyleClass().add("colon");
        }
        label.setPadding(new Insets(0, 4, 0, 0));
        return label;
    }
    
    /**
     * Create a {@link HideableTooltip} element.
     * 
     * @param text the text to display in the tooltip
     * @return the Tooltip
     */
    private HideableTooltip createTooltipElement(String text) {
        HideableTooltip tooltip = new HideableTooltip();
        tooltip.setText(Lookup.i18n(text));
        return tooltip;
    }
    
    /**
     * Create a {@link CheckBox} element.
     * 
     * @param selected is the check box selected
     * @return the CheckBox
     */
    private CheckBox createCheckBoxElement(PrintProfileSetting printProfileSetting) {
        HideableTooltip hideableTooltip = createTooltipElement(printProfileSetting.getTooltip());
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(Boolean.valueOf(printProfileSetting.getValue()));
        checkBox.setTooltip(hideableTooltip);
        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            printProfileSetting.setValue(String.valueOf(newValue));
            isDirty.set(true);
        });
        return checkBox;
    }
   
    /**
     * Create a {@link RestrictedNumberField}.
     * 
     * @param printProfileSetting the setting parameters
     * @return the RestrictedNumberField
     */
    private RestrictedNumberField createRestrictedNumberField(PrintProfileSetting printProfileSetting, String value, Nozzle nozzle) {
        HideableTooltip hideableTooltip = createTooltipElement(printProfileSetting.getTooltip());
        RestrictedNumberField restrictedNumberField = new RestrictedNumberField();
        restrictedNumberField.setTooltip(hideableTooltip);
        restrictedNumberField.setText(value);
        restrictedNumberField.setPrefWidth(50);
        if(printProfileSetting.getValueType().equals(FLOAT)) {
            restrictedNumberField.setAllowedDecimalPlaces(2);
        }
        restrictedNumberField.setMaxLength(5);
        
        restrictedNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
            String originalValues = printProfileSetting.getValue();
            switch(nozzle) {
            case SINGLE:
                printProfileSetting.setValue(newValue);
                break;
            case LEFT:
                String updatedLeftValue = newValue + ":" + originalValues.split(":")[1];
                printProfileSetting.setValue(updatedLeftValue);
                break;
            case RIGHT:
                String updatedRightValue = originalValues.split(":")[0] + ":" + newValue;
                printProfileSetting.setValue(updatedRightValue);
                break;
            }
            
            isDirty.set(true);
        });
        return restrictedNumberField;
    }
    
    /**
     * Create a {@link ComboBox} element.
     * 
     * @param printProfileSetting the setting parameters
     * @return the ComboBox
     */
    private ComboBox createComboBox(PrintProfileSetting printProfileSetting) {
        ComboBox comboBox = new ComboBox();
        if(printProfileSetting.getOptions().isPresent()) {
            Map<String, String> optionMap = printProfileSetting.getOptions().get();
            ObservableList<Option> options = optionMap.entrySet().stream()
                .map(entry -> new Option(entry.getKey(), entry.getValue()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
            comboBox.setItems(options);

            Optional<Option> value = options.stream()
                    .filter(option -> option.getOptionId().equals(printProfileSetting.getValue()))
                    .findFirst();
            if(value.isPresent()) {
                comboBox.setValue(value.get());
            }
        }
        comboBox.setTooltip(createTooltipElement(printProfileSetting.getTooltip()));
        comboBox.getStyleClass().add("cmbCleanCombo");
        
        comboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if(printProfileSetting.getValueType().equals(EXTRUSION)) {
                
            } else {
                printProfileSetting.setValue(String.valueOf(newValue));
            }
            
            isDirty.set(true);
        });
        
        return comboBox;
    }
    
    /**
     * Crate a {@link HBox} that contains a {@link RestrictedNumberField} with an
     * Optional {@link Label} for the unit.
     * 
     * @param printProfileSetting the setting parameters
     * @return a HBox
     */
    private HBox createInputFieldWithOptionalUnit(PrintProfileSetting printProfileSetting, String value, Nozzle nozzle) {
        RestrictedNumberField field = createRestrictedNumberField(printProfileSetting, value, nozzle);
        HBox hbox = new HBox(field);
        if(printProfileSetting.getUnit().isPresent()) {
            Label unitLabel = createLabelElement(printProfileSetting.getUnit().get(), false);
            hbox.getChildren().add(unitLabel);
        }
        hbox.setSpacing(4);
        return hbox;
    }
}
