/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.coreUI.components.VerticalMenu;
import celtech.printerControl.model.Printer;
import java.util.concurrent.Callable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 *
 * @author tony
 */
public class CalibrationMenuConfiguration
{

    public BooleanProperty nozzleOpeningCalibrationEnabled = new SimpleBooleanProperty(false);
    public BooleanProperty nozzleHeightCalibrationEnabled = new SimpleBooleanProperty(false);
    public BooleanProperty xyAlignmentCalibrationEnabled = new SimpleBooleanProperty(false);

    public Printer currentlySelectedPrinter;

    public void configureCalibrationMenu(VerticalMenu calibrationMenu,
        CalibrationInsetPanelController calibrationInsetPanelController)
    {

        if (currentlySelectedPrinter != null)
        {
            nozzleOpeningCalibrationEnabled.bind(
                currentlySelectedPrinter.canCalibrateNozzleOpeningProperty());
            nozzleHeightCalibrationEnabled.bind(
                currentlySelectedPrinter.canCalibrateNozzleHeightProperty());
            xyAlignmentCalibrationEnabled.bind(
                currentlySelectedPrinter.canCalibrateXYAlignmentProperty());
        }

        calibrationMenu.setTitle(Lookup.i18n("calibrationPanel.title"));
        VerticalMenu.NoArgsVoidFunc doOpeningCalibration = () ->
        {
            calibrationInsetPanelController.setCalibrationMode(
                CalibrationMode.NOZZLE_OPENING);
        };
        calibrationMenu.addItem(Lookup.i18n("calibrationMenu.nozzleOpening"),
                                doOpeningCalibration, null);
        VerticalMenu.NoArgsVoidFunc doHeightCalibration = () ->
        {
            calibrationInsetPanelController.setCalibrationMode(
                CalibrationMode.NOZZLE_HEIGHT);
        };
        calibrationMenu.addItem(Lookup.i18n("calibrationMenu.nozzleHeight"),
                                doHeightCalibration, null);
        VerticalMenu.NoArgsVoidFunc doXYAlignmentCalibration = () ->
        {
            calibrationInsetPanelController.setCalibrationMode(
                CalibrationMode.X_AND_Y_OFFSET);
        };
        calibrationMenu.addItem(Lookup.i18n("calibrationMenu.nozzleAlignment"),
                                doXYAlignmentCalibration, null);

        Lookup.getCurrentlySelectedPrinterProperty().addListener(selectedPrinterListener);
    }

    private ChangeListener<Printer> selectedPrinterListener = (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newPrinter) ->
    {
        currentlySelectedPrinter = newPrinter;
        nozzleOpeningCalibrationEnabled.unbind();
        nozzleHeightCalibrationEnabled.unbind();
        xyAlignmentCalibrationEnabled.unbind();
        if (newPrinter != null)
        {
            nozzleOpeningCalibrationEnabled.bind(newPrinter.canCalibrateNozzleOpeningProperty());
            nozzleHeightCalibrationEnabled.bind(newPrinter.canCalibrateNozzleHeightProperty());
            xyAlignmentCalibrationEnabled.bind(newPrinter.canCalibrateXYAlignmentProperty());
        }
    };

}
