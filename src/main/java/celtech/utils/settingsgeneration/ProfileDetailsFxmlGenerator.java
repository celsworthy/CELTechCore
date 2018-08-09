package celtech.utils.settingsgeneration;

import celtech.Lookup;
import celtech.coreUI.components.HideableTooltip;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.roboxbase.configuration.PrintProfileSetting;
import celtech.roboxbase.configuration.PrintProfileSettings;
import celtech.roboxbase.configuration.datafileaccessors.PrintProfileSettingsContainer;
import java.util.List;
import javafx.geometry.HPos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;

/**
 * Class to provide methods for adding generated settings to the FXML print profile pages.
 * 
 * @author George Salter
 */
public class ProfileDetailsFxmlGenerator {
    
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
            addSingleFieldRow(gridPane, printProfileSetting, rowNumber);
            rowNumber++;
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
        field0Col.setPercentWidth(25);
        label2Col.setPercentWidth(15);
        field1Col.setPercentWidth(25);
        
        label0Col.setHalignment(HPos.LEFT);
        
        gridPane.getColumnConstraints().addAll(label0Col, label1Col, field0Col, label2Col, field1Col);
    }
    
    /**
     * Add a single field setting to the given {@link GridPane}
     * It consists of a {@link Label} and a {@link RestrictedNumberField}.
     * 
     * @param gridPane the pane to add to
     * @param slicerSetting the setting parameters
     * @param row the row number of the grid to add to
     * @return the GridPane
     */
    protected GridPane addSingleFieldRow(GridPane gridPane, PrintProfileSetting slicerSetting, int row) {
        gridPane.add(createLabelElement(slicerSetting.getSettingName(), true), 0, row);
        gridPane.add(createInputFieldWithOptionalUnit(slicerSetting), 2, row);
        return gridPane;
    }
    
    /**
     * Add a combobox field setting to the given {@link GridPane}
     * It consists of a {@link Label} and a {@link ComboBox}.
     * 
     * @param gridPane the pane to add to
     * @param slicerSetting the setting parameters
     * @param row the row number of the grid to add to
     * @return the GridPane
     */
    protected GridPane addComboBoxRow(GridPane gridPane, PrintProfileSetting slicerSetting, int row) {
        gridPane.add(createLabelElement(slicerSetting.getSettingName(), true), 0, row);
        gridPane.add(createComboBox(slicerSetting.getTooltip()), 2, row);
        return gridPane;
    }
    
    /**
     * Add a nozzle selection box and field setting to the given {@link GridPane}
     * It consists of a {@link Label} a {@link ComboBox} for selection of a nozzle
     * and a {@link RestrictedNumberField} for the value.
     * 
     * @param gridPane the pane to add to
     * @param slicerSetting the setting parameters
     * @param row the row number of the grid to add to
     * @return the GridPane
     */
    protected GridPane addSelectionAndValueRow(GridPane gridPane, PrintProfileSetting slicerSetting, int row) {
        gridPane.add(createLabelElement(slicerSetting.getSettingName(), true), 0, row);
        gridPane.add(createLabelElement("extrusion.nozzle", true), 1, row);
        gridPane.add(createComboBox(slicerSetting.getTooltip()), 2, row);
        gridPane.add(createInputFieldWithOptionalUnit(slicerSetting), 4, row);
        return gridPane;
    }
    
    /**
     * Add a double field setting to the given {@link GridPane} for settings with
     * a value per nozzle.
     * It consists of a {@link Label} and two {@link RestrictedNumberField}
     * 
     * @param gridPane the pane to add to
     * @param slicerSetting the setting parameters
     * @param row the row number of the grid to add to
     * @return the GridPane
     */
    protected GridPane addPerExtruderValueRow(GridPane gridPane, PrintProfileSetting slicerSetting, int row) {
        gridPane.add(createLabelElement(slicerSetting.getSettingName(), true), 0, row);
        gridPane.add(createLabelElement("Left Nozzle", true), 1, row);
        gridPane.add(createInputFieldWithOptionalUnit(slicerSetting), 2, row);
        gridPane.add(createLabelElement("Right Nozzle", true), 3, row);
        gridPane.add(createInputFieldWithOptionalUnit(slicerSetting), 4, row);
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
     * Create a {@link RestrictedNumberField}.
     * 
     * @param hideableTooltip the tooltip to add to the field
     * @return the RestrictedNumberField
     */
    private RestrictedNumberField createRestrictedNumberField(PrintProfileSetting slicerSetting) {
        HideableTooltip hideableTooltip = createTooltipElement(slicerSetting.getTooltip());
        RestrictedNumberField restrictedNumberField = new RestrictedNumberField();
        restrictedNumberField.setTooltip(hideableTooltip);
        restrictedNumberField.setText(slicerSetting.getDefaultValue());
        restrictedNumberField.setPrefWidth(50);
        return restrictedNumberField;
    }
    
    /**
     * Create a {@link ComboBox} element.
     * 
     * @param tooltipText text to display in the tooltip
     * @return the ComboBox
     */
    private ComboBox createComboBox(String tooltipText) {
        ComboBox comboBox = new ComboBox();
        comboBox.setTooltip(createTooltipElement(tooltipText));
        comboBox.getStyleClass().add("cmbCleanCombo");
        return comboBox;
    }
    
    /**
     * Crate a {@link HBox} that contains a {@link RestrictedNumberField} with an
     * Optional {@link Label} for the unit.
     * 
     * @param unit the text for the unit label
     * @param tooltipText the text for the tooltip
     * @return a HBox
     */
    private HBox createInputFieldWithOptionalUnit(PrintProfileSetting slicerSetting) {
        RestrictedNumberField field = createRestrictedNumberField(slicerSetting);
        HBox hbox = new HBox(field);
        if(slicerSetting.getUnit().isPresent()) {
            Label unitLabel = createLabelElement(slicerSetting.getUnit().get(), false);
            hbox.getChildren().add(unitLabel);
        }
        hbox.setSpacing(4);
        return hbox;
    }
}
