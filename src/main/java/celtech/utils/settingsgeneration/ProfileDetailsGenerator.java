package celtech.utils.settingsgeneration;

import celtech.Lookup;
import celtech.coreUI.components.HideableTooltip;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.roboxbase.configuration.PrintProfileSetting;
import celtech.roboxbase.configuration.PrintProfileSettingsWrapper;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.configuration.fileRepresentation.HeadFile;
import celtech.roboxbase.configuration.fileRepresentation.NozzleData;
import celtech.roboxbase.printerControl.model.Head;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
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
    private static final String NOZZLE = "nozzle";
    private static final String EXTRUSION = "extrusion";
    private static final String NUMBER_LIST = "numbers";
    
    private static final double POINT_8_MIN_WIDTH = 0.5;
    private static final double POINT_8_MAX_WIDTH = 1.2;
    private static final double POINT_6_MIN_WIDTH = 0.4;
    private static final double POINT_6_MAX_WIDTH = 0.8;
    private static final double POINT_4_MIN_WIDTH = 0.2;
    private static final double POINT_4_MAX_WIDTH = 0.6;
    private static final double POINT_3_MIN_WIDTH = 0.2;
    private static final double POINT_3_MAX_WIDTH = 0.6;
    
    private static final Pattern NUMBER_LIST_PATTERN = Pattern.compile("^-?[0-9]*(,-?[0-9]*)*,?");
    
    private PrintProfileSettingsWrapper printProfileSettings;
    
    private String headType;
    
    private ObservableList<String> nozzleOptions;
    
    public ProfileDetailsGenerator(PrintProfileSettingsWrapper printProfileSettings, BooleanProperty isDirty) {
        this.printProfileSettings = printProfileSettings;
        this.isDirty = isDirty;
    }
    
    public void setPrintProfilesettings(PrintProfileSettingsWrapper printProfileSettings) {
        this.printProfileSettings = printProfileSettings;
    }
    
    public void setHeadType(String headType) {
        this.headType = headType;
    }
    
    public void setNozzleOptions(ObservableList<String> nozzleOptions) {
        this.nozzleOptions = nozzleOptions;
    }
    
    public void generateProfileSettingsForTab(GridPane gridPane) {
        clearGrid(gridPane);
        String gridId = gridPane.getId();
        List<PrintProfileSetting> profileSettingsForTab = printProfileSettings.getPrintProfileSettings().get(gridId);
        
        if(profileSettingsForTab == null)
        {
            return;
        }
        
        setupColumnsForGridPane(gridPane);
        
        int rowNumber = 0;
        
        for(PrintProfileSetting printProfileSetting : profileSettingsForTab) {
            gridPane.getRowConstraints().add(new RowConstraints());
            
            // Some changes to nozzle settings if there are no valves on the head
            boolean valvesFitted = HeadContainer.getHeadByID(headType).getValves() == Head.ValveType.FITTED;
            if(printProfileSetting.getId().equals("ejectionVolume")) {
                changeLabelingOfEjectionVolumeBasedOnValves(printProfileSetting, valvesFitted);
            }
            if(printProfileSetting.getId().equals("partialBMinimum") && !valvesFitted) {
                continue;
            }
            
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
                    if(printProfileSetting.isPerExtruder() && printProfileSetting.getValue().contains(":")) {
                        addPerExtruderValueRow(gridPane, printProfileSetting, rowNumber);
                    } else {
                        addSingleFieldRow(gridPane, printProfileSetting, rowNumber);
                    }
                    rowNumber++;
                    break;
                case BOOLEAN:
                    addCheckBoxRow(gridPane, printProfileSetting, rowNumber);
                    rowNumber++;
                    break;
                case OPTION:
                    if(printProfileSetting.getOptions().isPresent()) {
                        addComboBoxRow(gridPane, printProfileSetting, rowNumber);
                        rowNumber++;
                    } else {
                        STENO.warning("Option setting, "+ printProfileSetting.getId() + ", has no options, setting will be ignored");
                    }
                    break;
                case NOZZLE:
                    addSelectionAndValueRow(gridPane, printProfileSetting, rowNumber);
                    rowNumber++;
                    break;
                case NUMBER_LIST:
                    addListFieldRow(gridPane, printProfileSetting, rowNumber, NUMBER_LIST_PATTERN);
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
        ComboBox comboBox = createComboBox(printProfileSetting);
        gridPane.add(comboBox, 2, row);
        if(printProfileSetting.getChildren().isPresent()) {
            PrintProfileSetting extrusionSetting = printProfileSetting.getChildren().get().get(0);
            gridPane.add(createInputFieldForNozzleSelection(extrusionSetting, extrusionSetting.getValue(), comboBox), 4, row);
        }
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
    
    protected GridPane addListFieldRow(GridPane gridPane, PrintProfileSetting printProfileSetting, int row, Pattern numberListPattern) {
        gridPane.add(createLabelElement(printProfileSetting.getSettingName(), true), 0, row);
        gridPane.add(createRestrictedNumberFieldWithPattern(printProfileSetting, numberListPattern), 2, row);
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
        String translation = Lookup.i18n(labelText);
        Label label = new Label(translation);
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
        restrictedNumberField.setPrefWidth(60);
        if(printProfileSetting.getValueType().equals(FLOAT) || 
                printProfileSetting.getValueType().equals(EXTRUSION)) {
            restrictedNumberField.setAllowedDecimalPlaces(2);
        }
        restrictedNumberField.setMaxLength(5);
        
        if(printProfileSetting.getMinimumValue().isPresent()) {
            restrictedNumberField.setMinValue(Double.valueOf(printProfileSetting.getMinimumValue().get()));
        }
        if(printProfileSetting.getMaximumValue().isPresent()) {
            restrictedNumberField.setMaxValue(Double.valueOf(printProfileSetting.getMaximumValue().get()));
        }
        
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
            comboBox = setupStandardComboBox(printProfileSetting, comboBox);
        } else if(printProfileSetting.getValueType().equals(NOZZLE)) {
            comboBox = setupComboBoxForNozzleSelection(printProfileSetting, comboBox);
        }
        
        comboBox.setPrefWidth(150);
        comboBox.setTooltip(createTooltipElement(printProfileSetting.getTooltip()));
        comboBox.getStyleClass().add("cmbCleanCombo");
        
        return comboBox;
    }
    
    private ComboBox setupStandardComboBox(PrintProfileSetting printProfileSetting, ComboBox comboBox) {
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
        
        comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            Option option = (Option) newValue;
            printProfileSetting.setValue(option.getOptionId());
            isDirty.set(true);
        });
        
        return comboBox;
    }
    
    private ComboBox setupComboBoxForNozzleSelection(PrintProfileSetting printProfileSetting, ComboBox comboBox) {
        List<String> nozzles = new ArrayList<>();
        nozzles.addAll(nozzleOptions);
        
        HeadFile currentHead = HeadContainer.getHeadByID(headType);
        if(currentHead.getNozzleHeaters().size() == 2 && 
                Lookup.getUserPreferences().getSlicerType() == SlicerType.Cura3) {
            nozzles.set(0, nozzleOptions.get(0) + " (Material 2)");
            nozzles.set(1, nozzleOptions.get(1) + " (Material 1)");
            
            if(printProfileSetting.getNonOverrideAllowed().isPresent() &&
                printProfileSetting.getNonOverrideAllowed().get()) {
                nozzles.add("Model Material");
            }
        } else if (currentHead.getNozzleHeaters().size() == 2 || currentHead.getNozzles().size() == 1) {
            comboBox.setDisable(true);
        }
        
        comboBox.setItems(FXCollections.observableList(nozzles));
        int selectionIndex = Integer.parseInt(printProfileSetting.getValue());
        comboBox.getSelectionModel().select(selectionIndex); 

        comboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            printProfileSetting.setValue(String.valueOf(newValue));
            isDirty.set(true);
        });
                    
        return comboBox;
    }
    
    /**
     * Create a {@link HBox} that contains a {@link RestrictedNumberField} with an
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
    
    private HBox createRestrictedNumberFieldWithPattern(PrintProfileSetting printProfileSetting, Pattern restrictionPattern) {
        RestrictedNumberField field = createRestrictedNumberField(printProfileSetting, printProfileSetting.getValue(), Nozzle.SINGLE);
        field.setRestrictionPattern(restrictionPattern);
        field.setPrefWidth(150);
        HBox hbox = new HBox(field);
        return hbox;
    }
    
    /**
     * Create a {@link HBox} that contains a {@link RestrictedNumberField} with an
     * Optional {@link Label} for the unit. Also pass in the {@link ComboBox} that links
     * to the field.
     * 
     * @param printProfileSetting the setting parameters
     * @return a HBox
     */
    private HBox createInputFieldForNozzleSelection(PrintProfileSetting printProfileSetting, String value, ComboBox nozzleSelection) {
        RestrictedNumberField field = createRestrictedNumberField(printProfileSetting, value, Nozzle.SINGLE);
       
        setExtrusionWidthLimits(nozzleSelection.getSelectionModel().getSelectedIndex(), field);
        nozzleSelection.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            setExtrusionWidthLimits(newValue, field);
        });
        
        HBox hbox = new HBox(field);
        if(printProfileSetting.getUnit().isPresent()) {
            Label unitLabel = createLabelElement(printProfileSetting.getUnit().get(), false);
            hbox.getChildren().add(unitLabel);
        }
        hbox.setSpacing(4);
        return hbox;
    }
    
    private void setExtrusionWidthLimits(Number newValue, RestrictedNumberField extrusionSetting) {
        int index = newValue.intValue();
        if(index < 0 || index > 1) {
            index = 0;
        }
        String widthOption = nozzleOptions.get(index);
        Optional<NozzleData> optionalNozzleData = HeadContainer.getHeadByID(headType).getNozzles()
                .stream()
                .filter(nozzle -> nozzle.getMinExtrusionWidth() > 0.0)
                .filter(nozzle -> (Float.toString(nozzle.getDiameter()) + " mm").equals(widthOption))
                .findFirst();
        
        if (optionalNozzleData.isPresent()) {
            NozzleData nozzleData = optionalNozzleData.get();
            extrusionSetting.setMinValue(nozzleData.getMinExtrusionWidth());
            extrusionSetting.setMaxValue(nozzleData.getMaxExtrusionWidth());
        // For some reason these don't actually exist in the head file so we always do this...
        } else {
            switch (widthOption) {
                case "0.3mm":
                    extrusionSetting.setMinValue(POINT_3_MIN_WIDTH);
                    extrusionSetting.setMaxValue(POINT_3_MAX_WIDTH);
                    break;
                case "0.4mm":
                    extrusionSetting.setMinValue(POINT_4_MIN_WIDTH);
                    extrusionSetting.setMaxValue(POINT_4_MAX_WIDTH);
                    break;
                case "0.6mm":
                    extrusionSetting.setMinValue(POINT_6_MIN_WIDTH);
                    extrusionSetting.setMaxValue(POINT_6_MAX_WIDTH);
                    break;
                case "0.8mm":
                    extrusionSetting.setMinValue(POINT_8_MIN_WIDTH);
                    extrusionSetting.setMaxValue(POINT_8_MAX_WIDTH);
                    break;
                default:
                    break;
            }
        }
    }
    
    /**
     * A very specific method. We need to change the label and tooltip text if
     * there are no valves, i.e. it's actually a retraction amount.
     * 
     * @param ejectionVolume
     */
    private void changeLabelingOfEjectionVolumeBasedOnValves(PrintProfileSetting ejectionVolume, boolean valvesFitted) {
        if(valvesFitted) {
            ejectionVolume.setSettingName("nozzle.ejectionVolume");
            ejectionVolume.setTooltip("profileLibraryHelp.nozzleEjectionVolume");
        } else {
            ejectionVolume.setSettingName("nozzle.retractionVolume");
            ejectionVolume.setTooltip("profileLibraryHelp.nozzleRetractionVolume");
        }
    }
}
