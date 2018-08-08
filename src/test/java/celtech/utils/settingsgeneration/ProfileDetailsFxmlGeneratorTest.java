package celtech.utils.settingsgeneration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import celtech.JavaFXConfiguredTest;
import celtech.Lookup;
import celtech.coreUI.components.RestrictedNumberField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import org.junit.Before;


import org.junit.Test;

/**
 * Test class for the {@link ProfileDetailsFxmlGenerator}
 * 
 * @author George Salter
 */
public class ProfileDetailsFxmlGeneratorTest extends JavaFXConfiguredTest {
    
    private static final String SLICER_SETTING_NAME = "Slicer Setting Name";
    private static final String TOOLTIP = "this is a tooltip";
    private static final String UNIT = "mm";
    private static final String COLON_STYLE = "colon";
    
    ProfileDetailsFxmlGenerator profileDetailsFxmlGenerator = new ProfileDetailsFxmlGenerator();
    
    GridPane gridPane;
    
    @Before
    public void setup() {
        gridPane = new GridPane();
        
        RowConstraints row0 = new RowConstraints();
        gridPane.getRowConstraints().add(row0);
        
        ColumnConstraints col0 = new ColumnConstraints();
        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();
        gridPane.getColumnConstraints().addAll(col0, col1, col2);
    }
    
    @Test
    public void testAddSingleFieldRow() {
        SlicerSetting slicerSetting = new SlicerSetting(SLICER_SETTING_NAME);
        slicerSetting.setTooltip(TOOLTIP);
        slicerSetting.setUnit(UNIT);
        gridPane = profileDetailsFxmlGenerator.addSingleFieldRow(gridPane, slicerSetting, 0);
        
        Label label = (Label) gridPane.getChildren().get(0);
        HBox hbox = (HBox) gridPane.getChildren().get(1);
        
        assertThat(label.getText(), is(equalTo(SLICER_SETTING_NAME)));
        assertTrue(label.getStyleClass().contains(COLON_STYLE));
        
        RestrictedNumberField restrictedNumberField = (RestrictedNumberField) hbox.getChildren().get(0);
        Label unitLabel = (Label) hbox.getChildren().get(1);
        
        assertThat(restrictedNumberField.getTooltip().getText(), is(equalTo(TOOLTIP)));
        assertThat(unitLabel.getText(), is(equalTo(UNIT)));
    }
    
    @Test
    public void testAddComboBoxRow() {
        SlicerSetting slicerSetting = new SlicerSetting(SLICER_SETTING_NAME);
        slicerSetting.setTooltip(TOOLTIP);
        gridPane = profileDetailsFxmlGenerator.addComboBoxRow(gridPane, slicerSetting, 0);
        
        Label label = (Label) gridPane.getChildren().get(0);
        ComboBox combo = (ComboBox) gridPane.getChildren().get(1);
        
        assertThat(label.getText(), is(equalTo(SLICER_SETTING_NAME)));
        assertTrue(label.getStyleClass().contains(COLON_STYLE));
        
        assertThat(combo.getTooltip().getText(), is(equalTo(TOOLTIP)));
        assertTrue(combo.getStyleClass().contains("cmbCleanCombo"));
    }
    
    @Test
    public void testAddSelectionAndValueRow() {
        SlicerSetting slicerSetting = new SlicerSetting(SLICER_SETTING_NAME);
        slicerSetting.setTooltip(TOOLTIP);
        slicerSetting.setUnit(UNIT);
        gridPane = profileDetailsFxmlGenerator.addSelectionAndValueRow(gridPane, slicerSetting, 0);
        
        Label label = (Label) gridPane.getChildren().get(0);
        HBox comboHBox = (HBox) gridPane.getChildren().get(1);
        HBox fieldHBox = (HBox) gridPane.getChildren().get(2);
        
        assertThat(label.getText(), is(equalTo(SLICER_SETTING_NAME)));
        assertTrue(label.getStyleClass().contains(COLON_STYLE));
        
        Label boxLabel = (Label) comboHBox.getChildren().get(0);
        ComboBox combo = (ComboBox) comboHBox.getChildren().get(1);
        
        assertThat(boxLabel.getText(), is(equalTo(Lookup.i18n("extrusion.nozzle"))));
        assertThat(combo.getTooltip().getText(), is(equalTo(TOOLTIP)));
        assertTrue(combo.getStyleClass().contains("cmbCleanCombo"));
        
        RestrictedNumberField restrictedNumberField = (RestrictedNumberField) fieldHBox.getChildren().get(0);
        Label unitLabel = (Label) fieldHBox.getChildren().get(1);
        
        assertThat(restrictedNumberField.getTooltip().getText(), is(equalTo(TOOLTIP)));
        assertThat(unitLabel.getText(), is(equalTo(UNIT)));
        assertFalse(unitLabel.getStyleClass().contains(COLON_STYLE));
    }
    
    @Test
    public void testAddPerExtruderValueRow() {
        SlicerSetting slicerSetting = new SlicerSetting(SLICER_SETTING_NAME);
        slicerSetting.setTooltip(TOOLTIP);
        slicerSetting.setUnit(UNIT);
        gridPane = profileDetailsFxmlGenerator.addPerExtruderValueRow(gridPane, slicerSetting, 0);
        
        Label label = (Label) gridPane.getChildren().get(0);
        HBox leftHBox = (HBox) gridPane.getChildren().get(1);
        HBox rightHBox = (HBox) gridPane.getChildren().get(2);
        
        assertThat(label.getText(), is(equalTo(SLICER_SETTING_NAME)));
        assertTrue(label.getStyleClass().contains(COLON_STYLE));
        
        Label leftNozzleLabel = (Label) leftHBox.getChildren().get(0);
        RestrictedNumberField leftField = (RestrictedNumberField) leftHBox.getChildren().get(1);
        
        assertThat(leftNozzleLabel.getText(), is(equalTo("Left Nozzle")));
        assertTrue(leftNozzleLabel.getStyleClass().contains(COLON_STYLE));
        assertThat(leftField.getTooltip().getText(), is(equalTo(TOOLTIP)));
        
        Label rightNozzleLabel = (Label) rightHBox.getChildren().get(0);
        RestrictedNumberField rightField = (RestrictedNumberField) rightHBox.getChildren().get(1);
        Label rightUnitLabel = (Label) rightHBox.getChildren().get(2);
        
        assertThat(rightNozzleLabel.getText(), is(equalTo("Right Nozzle")));
        assertTrue(rightNozzleLabel.getStyleClass().contains(COLON_STYLE));
        assertThat(rightField.getTooltip().getText(), is(equalTo(TOOLTIP)));
        assertThat(rightUnitLabel.getText(), is(equalTo(UNIT)));
        assertFalse(rightUnitLabel.getStyleClass().contains(COLON_STYLE));
    }
}
