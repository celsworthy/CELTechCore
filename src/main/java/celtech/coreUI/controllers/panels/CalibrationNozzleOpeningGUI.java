/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.printerControl.model.calibration.StateTransitionManager;
import celtech.printerControl.model.calibration.StateTransitionManager.GUIName;
import celtech.printerControl.model.calibration.StateTransition;
import celtech.services.calibration.NozzleOpeningCalibrationState;
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
public class CalibrationNozzleOpeningGUI
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationNozzleOpeningGUI.class.getName());

    private final CalibrationInsetPanelController controller;
    StateTransitionManager<NozzleOpeningCalibrationState> stateManager;
    Map<GUIName, Button> namesToButtons = new HashMap<>();

    public CalibrationNozzleOpeningGUI(CalibrationInsetPanelController controller,
        StateTransitionManager<NozzleOpeningCalibrationState> stateManager)
    {
        this.controller = controller;
        this.stateManager = stateManager;
        
        stateManager.stateProperty().addListener(new ChangeListener()
        {

            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue)
            {
                setState((NozzleOpeningCalibrationState) newValue);
            }
        });
        populateNamesToButtons(controller);
    }

    private void showAppropriateButtons(NozzleOpeningCalibrationState state)
    {
        controller.hideAllInputControlsExceptStepNumber();
        for (StateTransition<NozzleOpeningCalibrationState> allowedTransition : this.stateManager.getTransitions())
        {
            if (namesToButtons.containsKey(allowedTransition.getGUIName()))
            {
                namesToButtons.get(allowedTransition.getGUIName()).setVisible(true);
                steno.info(("Show button " + allowedTransition.getGUIName()));
            }
        }
    }

    public void setState(NozzleOpeningCalibrationState state)
    {
        steno.info("GUI going to state " + state);
        showAppropriateButtons(state);
        switch (state)
        {
         case IDLE:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleopening", "Nozzle Opening Illustrations_Step 1.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 1));
                break;
            case HEATING:
                controller.showSpinner();
                controller.calibrationMenu.disableNonSelectedItems();
                controller.setCalibrationProgressVisible(
                    CalibrationInsetPanelController.ProgressVisibility.TEMP);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 10", 2));
                break;
            case NO_MATERIAL_CHECK:
                controller.buttonA.setVisible(true);
                controller.buttonB.setVisible(true);
                controller.buttonA.setText("Yes");
                controller.buttonB.setText("No");
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleopening", "Nozzle Opening Illustrations_Step 3.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 3));
                break;
            case PRE_CALIBRATION_PRIMING_FINE:
                controller.calibrationStatus.setText(state.getStepTitle());
                break;
            case CALIBRATE_FINE_NOZZLE:
                controller.buttonA.setVisible(true);
                controller.buttonB.setVisible(true);
                controller.buttonA.setText("Flowing");
                controller.buttonB.setText("Not flowing");
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleopening", "Nozzle Opening Illustrations_Step 4.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 4));
                break;
            case HEAD_CLEAN_CHECK_FINE_NOZZLE:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleopening", "Nozzle Opening Illustrations_Step 5 and 7.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 5));
                break;
            case CALIBRATE_FILL_NOZZLE:
                controller.buttonA.setVisible(true);
                controller.buttonB.setVisible(true);
                controller.buttonA.setText("Flowing");
                controller.buttonB.setText("Not flowing");
                controller.calibrationStatus.setText(state.getStepTitle());
                 controller.showDiagram("nozzleopening", "Nozzle Opening Illustrations_Step 6.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 6));
                break;
            case HEAD_CLEAN_CHECK_FILL_NOZZLE:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleopening", "Nozzle Opening Illustrations_Step 5 and 7.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 7));
                break;
            case CONFIRM_NO_MATERIAL:
                controller.buttonA.setVisible(true);
                controller.buttonB.setVisible(true);
                controller.buttonA.setText("No");
                controller.buttonB.setText("Yes");
                controller.calibrationStatus.setText(state.getStepTitle());
                 controller.showDiagram("nozzleopening", "Nozzle Opening Illustrations_Step 8.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 8));
                break;
            case CONFIRM_MATERIAL_EXTRUDING:
                controller.buttonA.setVisible(true);
                controller.buttonB.setVisible(true);
                controller.buttonA.setText("Yes");
                controller.buttonB.setText("No");
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleopening", "Nozzle Opening Illustrations_Step 9.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 9));
                break;
            case FINISHED:
                controller.calibrationStatus.setText(state.getStepTitle());
                 controller.showDiagram("nozzleopening", "Nozzle Opening Illustrations_Step 10.fxml");
                controller.stepNumber.setText(String.format("Step %s of 10", 10));
                break;
            case FAILED:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleopening", "Nozzle Opening Illustrations_Failure.fxml");
                controller.stepNumber.setText("");
                break;
        }
    }

    private void populateNamesToButtons(CalibrationInsetPanelController controller)
    {
        namesToButtons.put(GUIName.A_BUTTON, controller.buttonA);
        namesToButtons.put(GUIName.B_BUTTON, controller.buttonB);
        namesToButtons.put(GUIName.NEXT, controller.nextButton);
        namesToButtons.put(GUIName.CANCEL, controller.cancelCalibrationButton);
        namesToButtons.put(GUIName.RETRY, controller.retryPrintButton);
        namesToButtons.put(GUIName.START, controller.startCalibrationButton);
        namesToButtons.put(GUIName.BACK, controller.backToStatus);
    }

}
