/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

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
        calibrationMenu.addItem("Nozzle Opening", (Callable) () ->
                            {
                                calibrationInsetPanelController.setCalibrationMode(
                                    CalibrationMode.NOZZLE_OPENING);
                                return null;
        });
        calibrationMenu.addItem("Nozzle Height", (Callable) () ->
                            {
                                calibrationInsetPanelController.setCalibrationMode(
                                    CalibrationMode.NOZZLE_HEIGHT);
                                return null;
        });
        calibrationMenu.addItem("Nozzle Offsets", (Callable) () ->
                            {
                                calibrationInsetPanelController.setCalibrationMode(
                                    CalibrationMode.NOZZLE_OFFSETS);
                                return null;
        });        
        calibrationMenu.addItem("Nozzle Alignment", (Callable) () ->
                            {
                                calibrationInsetPanelController.setCalibrationMode(
                                    CalibrationMode.X_AND_Y_OFFSET);
                                return null;
        });
        calibrationMenu.addItem("Gantry Level", (Callable) () ->
                            {
                                System.out.println("Called GL");
                                return null;
        });
    }
    
}
