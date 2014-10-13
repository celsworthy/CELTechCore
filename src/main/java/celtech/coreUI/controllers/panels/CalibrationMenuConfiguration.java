/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.coreUI.components.VerticalMenu;
import java.util.concurrent.Callable;

/**
 *
 * @author tony
 */
public class CalibrationMenuConfiguration
{
    
    public static void configureCalibrationMenu(VerticalMenu calibrationMenu,
        CalibrationInsetPanelController calibrationInsetPanelController) {
        calibrationMenu.setTitle("Calibration");
        calibrationMenu.addItem(Lookup.i18n("calibrationMenu.nozzleOpening"), (Callable) () ->
                            {
                                calibrationInsetPanelController.setCalibrationMode(
                                    CalibrationMode.NOZZLE_OPENING);
                                return null;
        });
        calibrationMenu.addItem(Lookup.i18n("calibrationMenu.nozzleHeight"), (Callable) () ->
                            {
                                calibrationInsetPanelController.setCalibrationMode(
                                    CalibrationMode.NOZZLE_HEIGHT);
                                return null;
        });
    
        calibrationMenu.addItem(Lookup.i18n("calibrationMenu.nozzleAlignment"), (Callable) () ->
                            {
                                calibrationInsetPanelController.setCalibrationMode(
                                    CalibrationMode.X_AND_Y_OFFSET);
                                return null;
        });
        calibrationMenu.addItem(Lookup.i18n("calibrationMenu.gantryLevel"), (Callable) () ->
                            {
                                System.out.println("Called GL");
                                return null;
        });
    }
    
}
