package celtech.utils.settingsgeneration;

import celtech.Lookup;
import celtech.coreUI.components.HideableTooltip;
import celtech.coreUI.components.RestrictedNumberField;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
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
        gridPane.add(createInputFieldWithOptionalUnit(slicerSetting.getUnit(), slicerSetting.getTooltip()), 2, row);
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
    public GridPane addSelectionAndValueRow(GridPane gridPane, SlicerSetting slicerSetting, int row) {
        gridPane.add(createLabelElement(slicerSetting.getSettingName(), true), 0, row);
        gridPane.add(createLabelElement(Lookup.i18n("extrusion.nozzle"), true), 1, row);
        gridPane.add(createComboBox(slicerSetting.getTooltip()), 2, row);
        gridPane.add(createInputFieldWithOptionalUnit(slicerSetting.getUnit(), slicerSetting.getTooltip()), 4, row);
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
        gridPane.add(createLabelElement("Left Nozzle", true), 1, row);
        gridPane.add(createInputFieldWithOptionalUnit(Optional.empty(), slicerSetting.getTooltip()), 2, row);
        gridPane.add(createLabelElement("Right Nozzle", true), 3, row);
        gridPane.add(createInputFieldWithOptionalUnit(slicerSetting.getUnit(), slicerSetting.getTooltip()), 4, row);
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
     * Crate a {@link HBox} that contains a {@link RestrictedNumberField} with an
     * Optional {@link Label} for the unit.
     * 
     * @param unit the text for the unit label
     * @param tooltipText the text for the tooltip
     * @return a HBox
     */
    private HBox createInputFieldWithOptionalUnit(Optional<String> unit, String tooltipText) {
        HideableTooltip tooltip = createTooltipElement(tooltipText);
        RestrictedNumberField field = createRestrictedNumberField(tooltip);
        HBox hbox = new HBox(field);
        if(unit.isPresent()) {
            Label unitLabel = createLabelElement(unit.get(), false);
            hbox.getChildren().add(unitLabel);
        }
        hbox.setSpacing(4);
        return hbox;
    }
}
