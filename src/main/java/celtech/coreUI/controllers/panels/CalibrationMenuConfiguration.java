/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.coreUI.components.calibration.CalibrationMenu;
import java.util.concurrent.Callable;

/**
 *
 * @author tony
 */
public class CalibrationMenuConfiguration
{
    
    public static void configureCalibrationMenu(CalibrationMenu calibrationMenu,
        CalibrationInsetPanelController calibrationInsetPanelController) {
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
        calibrationMenu.addItem("X And Y Offset", (Callable) () ->
                            {
                                System.out.println("Called XY");
                                return null;
        });
        calibrationMenu.addItem("Gantry Level", (Callable) () ->
                            {
                                System.out.println("Called GL");
                                return null;
        });
    }
    
}
