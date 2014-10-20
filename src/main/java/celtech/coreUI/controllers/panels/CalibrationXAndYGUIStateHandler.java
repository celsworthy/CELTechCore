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
                controller.showDiagram("nozzlealignment", "Nozzle Alignment Illustrations_Step 1.fxml");
                controller.stepNumber.setText(String.format("Step %s of 6", 1));
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
                controller.showSpinner();
                controller.setCalibrationProgressVisible(
                    CalibrationInsetPanelController.ProgressVisibility.PRINT);
                controller.cancelCalibrationButton.setVisible(true);
                controller.cancelCalibrationButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 6", 2));
                break;
            case GET_Y_OFFSET:
                controller.hideAllInputControlsExceptStepNumber();
                controller.setCalibrationProgressVisible(
                    CalibrationInsetPanelController.ProgressVisibility.NONE);
                controller.retryPrintButton.setVisible(true);
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzlealignment", "Nozzle Alignment Illustrations_Step 4.fxml", false);
                controller.stepNumber.setText(String.format("Step %s of 6", 3));
                break;
            case PRINT_CIRCLE:    
                controller.hideAllInputControlsExceptStepNumber();
                controller.showSpinner();
                controller.setCalibrationProgressVisible(
                    CalibrationInsetPanelController.ProgressVisibility.PRINT);
                controller.cancelCalibrationButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 6", 4));
                break;     
            case PRINT_CIRCLE_CHECK:
                controller.hideAllInputControlsExceptStepNumber();
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(true);
                controller.showDiagram("nozzlealignment", "Nozzle Alignment Illustrations_Step 5.fxml");
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 6", 5));
                break;
            case FINISHED:
                controller.hideAllInputControlsExceptStepNumber();
                controller.backToStatus.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzlealignment", "Nozzle Alignment Illustrations_Step 6.fxml");
                controller.stepNumber.setText(String.format("Step %s of 6", 6));
                break;
            case FAILED:
                controller.hideAllInputControlsExceptStepNumber();
//                controller.buttonAAlt.disableProperty().unbind();
                controller.backToStatus.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText("");
                break;
        }
    }

}
