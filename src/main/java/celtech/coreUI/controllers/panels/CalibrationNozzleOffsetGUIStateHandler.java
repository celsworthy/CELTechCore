/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.services.calibration.NozzleOffsetCalibrationState;

/**
 *
 * @author tony
 */
public class CalibrationNozzleOffsetGUIStateHandler
{
    private final CalibrationInsetPanelController controller;
    private final CalibrationHelper calibrationHelper;
    
    public CalibrationNozzleOffsetGUIStateHandler(CalibrationInsetPanelController controller,
        CalibrationHelper calibrationHelper) {
        this.controller = controller;
        this.calibrationHelper = calibrationHelper;
    }

    public void setNozzleHeightState(NozzleOffsetCalibrationState state)
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
            case INITIALISING:
                controller.calibrationMenu.disableNonSelectedItems();
                controller.startCalibrationButton.setVisible(false);
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(true);
                controller.buttonB.setVisible(false);
                controller.buttonA.setVisible(false);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 2));
                break;
            case HEATING:
                controller.setCalibrationProgressVisible(true);
                controller.startCalibrationButton.setVisible(false);
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(false);
                controller.buttonB.setVisible(false);
                controller.buttonA.setVisible(false);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 3));
                break;
            case HEAD_CLEAN_CHECK:
                controller.setCalibrationProgressVisible(false);
                controller.startCalibrationButton.setVisible(false);
                controller.cancelCalibrationButton.setVisible(true);
                controller.buttonA.setVisible(false);
                controller.buttonB.setVisible(false);
                controller.nextButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 4));
                break;
            case MEASURE_Z_DIFFERENCE:
                controller.startCalibrationButton.setVisible(false);
                controller.cancelCalibrationButton.setVisible(true);
                controller.buttonB.setVisible(false);
                controller.buttonA.setVisible(false);
                controller.nextButton.setVisible(false);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 5));
                break;
            case INSERT_PAPER:
                controller.startCalibrationButton.setVisible(false);
                controller.cancelCalibrationButton.setVisible(true);
                controller.buttonB.setVisible(false);
                controller.buttonA.setVisible(false);
                controller.nextButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 6));
                break;
            case PROBING:
                controller.buttonAAlt.disableProperty().bind(
                    ((CalibrationNozzleOffsetHelper) calibrationHelper).showDownButton.not());
                controller.startCalibrationButton.setVisible(false);
                controller.cancelCalibrationButton.setVisible(true);
                controller.buttonAAlt.setVisible(true);
                controller.buttonBAlt.setVisible(true);
                controller.buttonB.setVisible(false);
                controller.buttonA.setVisible(false);
                controller.nextButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 7));
                break;
            case LIFT_HEAD:
                break;
            case REPLACE_PEI_BED:
                controller.buttonAAlt.disableProperty().unbind();
                controller.startCalibrationButton.setVisible(false);
                controller.cancelCalibrationButton.setVisible(true);
                controller.buttonB.setVisible(false);
                controller.buttonA.setVisible(false);
                controller.buttonAAlt.setVisible(false);
                controller.buttonBAlt.setVisible(false);
                controller.nextButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 8));
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