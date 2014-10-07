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
        CalibrationHelper calibrationHelper)
    {
        this.controller = controller;
        this.calibrationHelper = calibrationHelper;
    }

    public void setXAndYState(CalibrationXAndYState state)
    {
        switch (state)
        {
            case IDLE:
                controller.hideAllInputControlsExceptStepNumber();
                controller.stepNumber.setVisible(true);
                controller.startCalibrationButton.setVisible(true);
                controller.cancelCalibrationButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 1));
                break;
//            case HEATING:
//                controller.hideAllInputControlsExceptStepNumber();
//                controller.calibrationMenu.disableNonSelectedItems();
//                controller.setCalibrationProgressVisible(
//                    CalibrationInsetPanelController.ProgressVisibility.TEMP);
//                controller.cancelCalibrationButton.setVisible(true);
//                controller.calibrationStatus.setText(state.getStepTitle());
//                controller.stepNumber.setText(String.format("Step %s of 10", 2));
//                break;
            case PRINT_PATTERN:
                controller.calibrationMenu.disableNonSelectedItems();
                controller.hideAllInputControlsExceptStepNumber();
                controller.showWaitTimer(true);
                controller.setCalibrationProgressVisible(
                    CalibrationInsetPanelController.ProgressVisibility.PRINT);
                controller.cancelCalibrationButton.setVisible(true);
                controller.cancelCalibrationButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 3));
                break;
            case GET_Y_OFFSET:
                controller.hideAllInputControlsExceptStepNumber();
                controller.setCalibrationProgressVisible(
                    CalibrationInsetPanelController.ProgressVisibility.NONE);
                controller.offsetCombosContainer.setVisible(true);
                controller.retryPrintButton.setVisible(true);
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 4));
                break;
            case PRINT_CIRCLE:    
                controller.hideAllInputControlsExceptStepNumber();
                controller.showWaitTimer(true);
                controller.setCalibrationProgressVisible(
                    CalibrationInsetPanelController.ProgressVisibility.PRINT);
                controller.cancelCalibrationButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 5));
                break;     
            case PRINT_CIRCLE_CHECK:
                controller.hideAllInputControlsExceptStepNumber();
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(true);
                controller.stepNumber.setText(String.format("Step %s of 10", 6));
                break;
            case FINISHED:
                controller.hideAllInputControlsExceptStepNumber();
                controller.backToStatus.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 7));
                break;
            case FAILED:
                controller.hideAllInputControlsExceptStepNumber();
                controller.buttonAAlt.disableProperty().unbind();
                controller.backToStatus.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText("");
                break;
        }
    }

}
