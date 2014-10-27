/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.printerControl.model.calibration.StateTransitionManager;
import celtech.printerControl.model.calibration.StateTransitionManager.GUIName;
import celtech.printerControl.model.calibration.StateTransition;
import celtech.services.calibration.CalibrationXAndYState;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class CalibrationXAndYGUI
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationXAndYGUI.class.getName());

    private CalibrationInsetPanelController controller;
    StateTransitionManager<CalibrationXAndYState> stateManager;
    Map<GUIName, Button> namesToButtons = new HashMap<>();

    public CalibrationXAndYGUI(CalibrationInsetPanelController controller,
        StateTransitionManager stateManager)
    {
        this.controller = controller;
        this.stateManager = stateManager;
        
        stateManager.stateProperty().addListener(new ChangeListener()
        {

            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue)
            {
                setState((CalibrationXAndYState) newValue);
            }
        });
        populateNamesToButtons(controller);
    }

    private void showAppropriateButtons(CalibrationXAndYState state)
    {
        controller.hideAllInputControlsExceptStepNumber();
        for (StateTransition<CalibrationXAndYState> allowedTransition : this.stateManager.getTransitions())
        {
            if (namesToButtons.containsKey(allowedTransition.getGUIName()))
            {
                namesToButtons.get(allowedTransition.getGUIName()).setVisible(true);
            }
        }
    }

    public void setState(CalibrationXAndYState state)
    {
        steno.info("GUI going to state " + state);
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
                controller.showDiagram("nozzlealignment",
                                       "Nozzle Alignment Illustrations_Step 5.fxml");
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 6", 5));
                break;
            case FINISHED:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzlealignment",
                                       "Nozzle Alignment Illustrations_Step 6.fxml");
                controller.stepNumber.setText(String.format("Step %s of 6", 6));
                break;
            case FAILED:
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
