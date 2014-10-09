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
                controller.hideAllInputControlsExceptStepNumber();
                controller.startCalibrationButton.setVisible(true);
                controller.cancelCalibrationButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 1 and 5.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 1));
                break;
            case INITIALISING:
                controller.hideAllInputControlsExceptStepNumber();
                controller.calibrationMenu.disableNonSelectedItems();
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 2.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 2));
                break;
            case HEATING:
                controller.hideAllInputControlsExceptStepNumber();
                controller.showWaitTimer(true);
                controller.setCalibrationProgressVisible(CalibrationInsetPanelController.ProgressVisibility.TEMP);
                controller.cancelCalibrationButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 3));
                break;
            case HEAD_CLEAN_CHECK:
                controller.hideAllInputControlsExceptStepNumber();
                controller.setCalibrationProgressVisible(CalibrationInsetPanelController.ProgressVisibility.NONE);
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 4.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 4));
                break;
            case MEASURE_Z_DIFFERENCE:
                controller.hideAllInputControlsExceptStepNumber();
                controller.cancelCalibrationButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 1 and 5.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 5));
                break;
            case INSERT_PAPER:
                controller.hideAllInputControlsExceptStepNumber();
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 6.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 6));
                break;
            case PROBING:
                controller.hideAllInputControlsExceptStepNumber();
//                controller.buttonAAlt.disableProperty().bind(
//                    ((CalibrationNozzleOffsetHelper) calibrationHelper).showDownButton.not());
                controller.cancelCalibrationButton.setVisible(true);
//                controller.buttonAAlt.setVisible(true);
//                controller.buttonBAlt.setVisible(true);
                controller.nextButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 7.fxml"); 
                controller.stepNumber.setText(String.format("Step %s of 10", 7));
                break;
            case LIFT_HEAD:
                controller.hideAllInputControlsExceptStepNumber();
                controller.nextButton.setVisible(true);
                break;
            case REPLACE_PEI_BED:
                controller.hideAllInputControlsExceptStepNumber();
//                controller.buttonAAlt.disableProperty().unbind();
                controller.cancelCalibrationButton.setVisible(true);
                controller.nextButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 8.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 8));
                break;
            case FINISHED:
                controller.hideAllInputControlsExceptStepNumber();
                controller.backToStatus.setVisible(true);
                controller.cancelCalibrationButton.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 9.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 9));
                break;
            case FAILED:
                controller.hideAllInputControlsExceptStepNumber();
//                controller.buttonAAlt.disableProperty().unbind();
                controller.backToStatus.setVisible(true);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Failure.fxml");
                controller.stepNumber.setText("");
                break;
        }
    }

}
