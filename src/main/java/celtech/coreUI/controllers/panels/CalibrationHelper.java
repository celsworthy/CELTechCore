/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.printerControl.Printer;
import celtech.services.calibration.NozzleOpeningCalibrationState;

/**
 *
 * @author tony
 */
public interface CalibrationHelper
{
    void buttonAAction();

    void buttonBAction();

    void cancelCalibrationAction();

    void nextButtonAction();

    void saveSettings();

    void setPrinterToUse(Printer printer);

    void goToIdleState();
    
}