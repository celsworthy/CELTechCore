package celtech.utils.settingsgeneration;

import celtech.Lookup;
import celtech.coreUI.components.HideableTooltip;
import celtech.coreUI.components.RestrictedNumberField;
import java.util.Optional;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/**
 * Class to provide methods for adding generated settings to the FXML print profile pages.
 * 
 * @author George Salter
 */
public class ProfileDetailsFxmlGenerator {
    
    /**
     * Add a single field setting to the given {@link GridPane}
     * It consists of a {@link Label} and a {@link RestrictedNumberField}.
     * 
     * @param gridPane the pane to add to
     * @param slicerSetting the setting parameters
     * @param row the row number of the grid to add to
     * @return the GridPane
     */
    public GridPane addSingleFieldRow(GridPane gridPane, SlicerSetting slicerSetting, int row) {
        gridPane.add(createLabelElement(slicerSetting.getSettingName(), true), 0, row);
        gridPane.add(createInputFieldWithUnit(slicerSetting.getUnit(), slicerSetting.getTooltip()), 1, row);
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
    public GridPane addComboBoxRow(GridPane gridPane, SlicerSetting slicerSetting, int row) {
        gridPane.add(createLabelElement(slicerSetting.getSettingName(), true), 0, row);
        gridPane.add(createComboBox(slicerSetting.getTooltip()), 1, row);
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
    public GridPane addSelectionAndValueRow(GridPane gridPane, SlicerSetting slicerSetting, int row) {
        gridPane.add(createLabelElement(slicerSetting.getSettingName(), true), 0, row);
        gridPane.add(createLabeledComboBox(Lookup.i18n("extrusion.nozzle"), slicerSetting.getTooltip()), 1, row);
        gridPane.add(createInputFieldWithUnit(slicerSetting.getUnit(), slicerSetting.getTooltip()), 2, row);
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
    public GridPane addPerExtruderValueRow(GridPane gridPane, SlicerSetting slicerSetting, int row) {
        gridPane.add(createLabelElement(slicerSetting.getSettingName(), true), 0, row);
        gridPane.add(createLabeledInputFieldWithOptionalUnit("Left Nozzle", slicerSetting.getTooltip(), Optional.empty()), 1, row);
        gridPane.add(createLabeledInputFieldWithOptionalUnit("Right Nozzle", slicerSetting.getTooltip(), Optional.of(slicerSetting.getUnit())), 2, row);
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
        Label label = new Label(labelText);
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
        tooltip.setText(text);
        return tooltip;
    }
   
    /**
     * Create a {@link RestrictedNumberField}.
     * 
     * @param hideableTooltip the tooltip to add to the field
     * @return the RestrictedNumberField
     */
    private RestrictedNumberField createRestrictedNumberField(HideableTooltip hideableTooltip) {
        RestrictedNumberField restrictedNumberField = new RestrictedNumberField();
        restrictedNumberField.setTooltip(hideableTooltip);
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
     * Create a {@link HBox} that contains a {@link ComboBox} with a {@link Label}
     * 
     * @param labelText text for the label
     * @param tooltipText test for the tooltip
     * @return a HBox
     */
    private HBox createLabeledComboBox(String labelText, String tooltipText) {
        Label label = createLabelElement(labelText, true);
        ComboBox comboBox = createComboBox(tooltipText);
        HBox hbox = new HBox(label, comboBox);
        return hbox;
    }
    
    /**
     * Create a {@link HBox} that contains a {@link RestrictedNumberField} with
     * a {@link Label} and an optional unit label.
     * 
     * @param labelText the text for the label
     * @param tooltipText the text for the tooltip
     * @param unit an Optional to be provided for a unit label
     * @return a HBox
     */
    private HBox createLabeledInputFieldWithOptionalUnit(String labelText, String tooltipText, Optional<String> unit) {
        HideableTooltip tooltip = createTooltipElement(tooltipText);
        Label label = createLabelElement(labelText, true);
        RestrictedNumberField field = createRestrictedNumberField(tooltip);
        HBox hbox = new HBox(label, field);
        if(unit.isPresent()) {
            hbox.getChildren().add(createLabelElement(unit.get(), false));
        }
        return hbox;
    }
    
    /**
     * Crate a {@link HBox} that contains a {@link RestrictedNumberField} with a
     * {@link Label} for the unit.
     * 
     * @param unit the text for the unit label
     * @param tooltipText the text for the tooltip
     * @return a HBox
     */
    private HBox createInputFieldWithUnit(String unit, String tooltipText) {
        HideableTooltip tooltip = createTooltipElement(tooltipText);
        RestrictedNumberField field = createRestrictedNumberField(tooltip);
        Label unitLabel = createLabelElement(unit, false);
        HBox hbox = new HBox(field, unitLabel);
        return hbox;
    }
}
