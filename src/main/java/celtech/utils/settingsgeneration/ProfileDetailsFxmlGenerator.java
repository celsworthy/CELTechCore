package celtech.utils.settingsgeneration;

import celtech.Lookup;
import celtech.coreUI.components.HideableTooltip;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.roboxbase.configuration.PrintProfileSetting;
import celtech.roboxbase.configuration.PrintProfileSettings;
import celtech.roboxbase.configuration.datafileaccessors.PrintProfileSettingsContainer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
public class ProfileDetailsFxmlGenerator {
    
    private static final Stenographer STENO = StenographerFactory.getStenographer(ProfileDetailsFxmlGenerator.class.getName());
    
    PrintProfileSettings printProfileSettings;
    
    public void generateProfileSettingsForTab(GridPane gridPane) {
        String gridId = gridPane.getId();
        if(printProfileSettings == null) {
            printProfileSettings = PrintProfileSettingsContainer.getInstance().getPrintProfileSettings();
        }
        List<PrintProfileSetting> profileSettingsForTab = printProfileSettings.getPrintProfileSettings().get(gridId);
        
        setupColumnsForGridPane(gridPane);
        
        int rowNumber = 0;
        
        for(PrintProfileSetting printProfileSetting : profileSettingsForTab) {
            gridPane.getRowConstraints().add(new RowConstraints());
            
            String valueType = printProfileSetting.getValueType();
            switch(valueType) {
                case "float":
                    if(printProfileSetting.isPerExtruder()) {
                        addPerExtruderValueRow(gridPane, printProfileSetting, rowNumber);
                    } else {
                        addSingleFieldRow(gridPane, printProfileSetting, rowNumber);
                    }
                    rowNumber++;
                    break;
                case "int":
                    addSingleFieldRow(gridPane, printProfileSetting, rowNumber);
                    rowNumber++;
                    break;
                case "boolean":
                    addCheckBoxRow(gridPane, printProfileSetting, rowNumber);
                    rowNumber++;
                case "option":
                    if(printProfileSetting.getOptions().isPresent()) {
                        addComboBoxRow(gridPane, printProfileSetting, rowNumber);
                        rowNumber++;
                    } else {
                        STENO.error("Option setting has no options, setting will be ignored");
                    }
                    break;
                case "extrusion":
                    addSelectionAndValueRow(gridPane, printProfileSetting, rowNumber);
                    rowNumber++;
                    break;
                default:
                    STENO.error("Value type of " + valueType + " not recognised, setting will be ignored");
            }
        }
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
        gridPane.add(createInputFieldWithOptionalUnit(printProfileSetting), 2, row);
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
        gridPane.add(createInputFieldWithOptionalUnit(printProfileSetting), 4, row);
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
        gridPane.add(createLabelElement(printProfileSetting.getSettingName(), true), 0, row);
        gridPane.add(createLabelElement("Left Nozzle", true), 1, row);
        gridPane.add(createInputFieldWithOptionalUnit(printProfileSetting), 2, row);
        gridPane.add(createLabelElement("Right Nozzle", true), 3, row);
        gridPane.add(createInputFieldWithOptionalUnit(printProfileSetting), 4, row);
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
        checkBox.setSelected(Boolean.valueOf(printProfileSetting.getDefaultValue()));
        checkBox.setTooltip(hideableTooltip);
        return checkBox;
    }
   
    /**
     * Create a {@link RestrictedNumberField}.
     * 
     * @param printProfileSetting the setting parameters
     * @return the RestrictedNumberField
     */
    private RestrictedNumberField createRestrictedNumberField(PrintProfileSetting printProfileSetting) {
        HideableTooltip hideableTooltip = createTooltipElement(printProfileSetting.getTooltip());
        RestrictedNumberField restrictedNumberField = new RestrictedNumberField();
        restrictedNumberField.setTooltip(hideableTooltip);
        restrictedNumberField.setText(printProfileSetting.getDefaultValue());
        restrictedNumberField.setPrefWidth(50);
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
                    .filter(option -> option.getOptionId().equals(printProfileSetting.getDefaultValue()))
                    .findFirst();
            if(value.isPresent()) {
                comboBox.setValue(value.get());
            }
        }
        comboBox.setTooltip(createTooltipElement(printProfileSetting.getTooltip()));
        comboBox.getStyleClass().add("cmbCleanCombo");
        return comboBox;
    }
    
    /**
     * Crate a {@link HBox} that contains a {@link RestrictedNumberField} with an
     * Optional {@link Label} for the unit.
     * 
     * @param printProfileSetting the setting parameters
     * @return a HBox
     */
    private HBox createInputFieldWithOptionalUnit(PrintProfileSetting printProfileSetting) {
        RestrictedNumberField field = createRestrictedNumberField(printProfileSetting);
        HBox hbox = new HBox(field);
        if(printProfileSetting.getUnit().isPresent()) {
            Label unitLabel = createLabelElement(printProfileSetting.getUnit().get(), false);
            hbox.getChildren().add(unitLabel);
        }
        hbox.setSpacing(4);
        return hbox;
    }
}
