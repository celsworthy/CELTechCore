/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.services.calibration.CalibrationXAndYState;


/**
 *
 * @author tony
 */
public class CalibrationXAndYGUIStateHandler
{
    private final CalibrationInsetPanelController controller;
    private final CalibrationHelper calibrationHelper;
    
    public CalibrationXAndYGUIStateHandler(CalibrationInsetPanelController controller,
        CalibrationHelper calibrationHelper) {
        this.controller = controller;
        this.calibrationHelper = calibrationHelper;
    }

    public void setXAndYState(CalibrationXAndYState state)
    {
        switch (state)
        {
            case IDLE:
                controller.backToStatus.setVisible(false);
                controller.setCalibrationProgressVisible(false);
                controller.stepNumber.setVisible(true);
                controller.buttonAAlt.setVisible(false);
                controller.buttonBAlt.setVisible(false);
                controller.startCalibrationButton.setVisible(true);
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(false);
                controller.buttonB.setVisible(false);
                controller.buttonA.setVisible(false);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 1));
                break;
            case HEATING:
                controller.setCalibrationProgressVisible(true);
                controller.startCalibrationButton.setVisible(false);
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(false);
                controller.buttonB.setVisible(false);
                controller.buttonA.setVisible(false);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 2));
                break;
            case PRINT_PATTERN:
                controller.setCalibrationProgressVisible(false);
                controller.startCalibrationButton.setVisible(false);
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(false);
                controller.buttonB.setVisible(false);
                controller.buttonA.setVisible(false);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 2));
                break;
            case FINISHED:
                controller.backToStatus.setVisible(true);
                controller.startCalibrationButton.setVisible(false);
                controller.cancelCalibrationButton.setVisible(true);
                controller.buttonB.setVisible(false);
                controller.buttonA.setVisible(false);
                controller.buttonAAlt.setVisible(false);
                controller.buttonBAlt.setVisible(false);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 9));
                controller.setCalibrationMode(CalibrationMode.CHOICE);
                break;
            case FAILED:
                controller.buttonAAlt.disableProperty().unbind();
                controller.startCalibrationButton.setVisible(false);
                controller.cancelCalibrationButton.setVisible(false);
                controller.buttonB.setVisible(false);
                controller.buttonA.setVisible(false);
                controller.buttonAAlt.setVisible(false);
                controller.buttonBAlt.setVisible(false);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText("");
                controller.setCalibrationMode(CalibrationMode.CHOICE);
                break;
        }
    }

}
