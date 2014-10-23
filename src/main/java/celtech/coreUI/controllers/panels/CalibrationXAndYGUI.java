/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.printerControl.model.calibration.CalibrationAlignmentManager;
import celtech.printerControl.model.calibration.CalibrationAlignmentManager.GUIName;
import celtech.printerControl.model.calibration.StateTransition;
import celtech.services.calibration.CalibrationXAndYState;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;

/**
 *
 * @author tony
 */
public class CalibrationXAndYGUI
{

    private CalibrationInsetPanelController controller;
    CalibrationAlignmentManager stateManager;
    Map<GUIName, Button> namesToButtons = new HashMap<>();

    public CalibrationXAndYGUI(CalibrationInsetPanelController controller,
        CalibrationAlignmentManager stateManager)
    {
        this.controller = controller;
        this.stateManager = stateManager;
        stateManager.stateProperty().addListener(
            (ObservableValue<? extends CalibrationXAndYState> observable, CalibrationXAndYState oldValue, CalibrationXAndYState newValue) ->
            {
                setState(newValue);
            });
        populateNamesToButtons(controller);
    }

    private void showAppropriateButtons(CalibrationXAndYState state)
    {
        controller.hideAllInputControlsExceptStepNumber();
        for (StateTransition allowedTransition : this.stateManager.getTransitions())
        {
            namesToButtons.get(allowedTransition.getGUIName()).setVisible(true);
        }
    }

    public void setState(CalibrationXAndYState state)
    {
        showAppropriateButtons(state);
        switch (state)
        {
            case IDLE:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzlealignment",
                                       "Nozzle Alignment Illustrations_Step 1.fxml");
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
                controller.showSpinner();
                controller.setCalibrationProgressVisible(
                    CalibrationInsetPanelController.ProgressVisibility.PRINT);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 6", 2));
                break;
            case GET_Y_OFFSET:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzlealignment",
                                       "Nozzle Alignment Illustrations_Step 4.fxml", false);
                controller.stepNumber.setText(String.format("Step %s of 6", 3));
                break;
            case PRINT_CIRCLE:
                controller.showSpinner();
                controller.setCalibrationProgressVisible(
                    CalibrationInsetPanelController.ProgressVisibility.PRINT);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 6", 4));
                break;
            case PRINT_CIRCLE_CHECK:
                controller.hideAllInputControlsExceptStepNumber();
                controller.showDiagram("nozzlealignment",
                                       "Nozzle Alignment Illustrations_Step 5.fxml");
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 6", 5));
                break;
            case FINISHED:
                controller.hideAllInputControlsExceptStepNumber();
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzlealignment",
                                       "Nozzle Alignment Illustrations_Step 6.fxml");
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

    private void populateNamesToButtons(CalibrationInsetPanelController controller)
    {
        namesToButtons.put(GUIName.YES, controller.buttonA);
        namesToButtons.put(GUIName.NO, controller.buttonB);
        namesToButtons.put(GUIName.NEXT, controller.nextButton);
        namesToButtons.put(GUIName.CANCEL, controller.cancelCalibrationButton);
        namesToButtons.put(GUIName.RETRY, controller.retryPrintButton);
        namesToButtons.put(GUIName.START, controller.startCalibrationButton);
        namesToButtons.put(GUIName.BACK, controller.backToStatus);
    }

}
