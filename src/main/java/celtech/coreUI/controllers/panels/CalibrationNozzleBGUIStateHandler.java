/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.services.calibration.NozzleOpeningCalibrationState;

/**
 *
 * @author tony
 */
public class CalibrationNozzleBGUIStateHandler
{

    private final CalibrationInsetPanelController controller;
    private final CalibrationHelper calibrationHelper;

    public CalibrationNozzleBGUIStateHandler(CalibrationInsetPanelController controller,
        CalibrationHelper calibrationHelper)
    {
        this.controller = controller;
        this.calibrationHelper = calibrationHelper;
    }

    public void setNozzleOpeningState(NozzleOpeningCalibrationState state)
    {
        switch (state)
        {
            case IDLE:
                controller.hideAllInputControlsExceptStepNumber();
                controller.startCalibrationButton.setVisible(true);
                controller.cancelCalibrationButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 1));
                break;
            case HEATING:
                controller.hideAllInputControlsExceptStepNumber();
                controller.calibrationMenu.disableNonSelectedItems();
                controller.setCalibrationProgressVisible(
                    CalibrationInsetPanelController.ProgressVisibility.TEMP);
                controller.cancelCalibrationButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 2));
                break;
            case NO_MATERIAL_CHECK:
                controller.hideAllInputControlsExceptStepNumber();
                controller.setCalibrationProgressVisible(
                    CalibrationInsetPanelController.ProgressVisibility.NONE);
                controller.cancelCalibrationButton.setVisible(true);
                controller.buttonA.setVisible(true);
                controller.buttonB.setVisible(true);
                controller.buttonA.setText("Yes");
                controller.buttonB.setText("No");
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 3));
                break;
            case PRE_CALIBRATION_PRIMING_FINE:
                controller.hideAllInputControlsExceptStepNumber();
                controller.cancelCalibrationButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                break;
            case CALIBRATE_FINE_NOZZLE:
                controller.hideAllInputControlsExceptStepNumber();
                controller.cancelCalibrationButton.setVisible(true);
                controller.buttonA.setVisible(true);
                controller.buttonB.setVisible(true);
                controller.buttonA.setText("Flowing");
                controller.buttonB.setText("Not flowing");
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 4));
                break;
            case HEAD_CLEAN_CHECK_FINE_NOZZLE:
                controller.hideAllInputControlsExceptStepNumber();
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 5));
                break;
            case CALIBRATE_FILL_NOZZLE:
                controller.hideAllInputControlsExceptStepNumber();
                controller.cancelCalibrationButton.setVisible(true);
                controller.buttonA.setVisible(true);
                controller.buttonB.setVisible(true);
                controller.buttonA.setText("Flowing");
                controller.buttonB.setText("Not flowing");
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 6));
                break;
            case HEAD_CLEAN_CHECK_FILL_NOZZLE:
                controller.hideAllInputControlsExceptStepNumber();
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 7));
                break;
            case CONFIRM_NO_MATERIAL:
                controller.hideAllInputControlsExceptStepNumber();
                controller.cancelCalibrationButton.setVisible(true);
                controller.buttonA.setVisible(true);
                controller.buttonB.setVisible(true);
                controller.buttonA.setText("No");
                controller.buttonB.setText("Yes");
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 8));
                break;
            case CONFIRM_MATERIAL_EXTRUDING:
                controller.hideAllInputControlsExceptStepNumber();
                controller.cancelCalibrationButton.setVisible(true);
                controller.buttonA.setVisible(true);
                controller.buttonB.setVisible(true);
                controller.buttonA.setText("Yes");
                controller.buttonB.setText("No");
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 9));
                break;
            case FINISHED:
                controller.hideAllInputControlsExceptStepNumber();
                controller.backToStatus.setVisible(true);
                controller.cancelCalibrationButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 10));
                break;
            case FAILED:
                controller.hideAllInputControlsExceptStepNumber();
                controller.backToStatus.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText("");
                break;
        }
    }

}
