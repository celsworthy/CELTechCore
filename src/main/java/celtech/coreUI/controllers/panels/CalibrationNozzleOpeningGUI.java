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
import javafx.scene.layout.Region;
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
    Map<GUIName, Region> namesToButtons = new HashMap<>();

    public CalibrationNozzleOpeningGUI(CalibrationInsetPanelController controller,
        StateTransitionManager<NozzleOpeningCalibrationState> stateManager)
    {
        this.controller = controller;
        this.stateManager = stateManager;

        stateManager.stateGUITProperty().addListener(new ChangeListener()
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
        if (state.showCancelButton())
        {
            controller.cancelCalibrationButton.setVisible(true);
        }
        for (StateTransition<NozzleOpeningCalibrationState> allowedTransition : this.stateManager.getTransitions())
        {
            if (namesToButtons.containsKey(allowedTransition.getGUIName()))
            {
                namesToButtons.get(allowedTransition.getGUIName()).setVisible(true);
            }
        }
    }

    public void setState(NozzleOpeningCalibrationState state)
    {
        steno.debug("GUI going to state " + state);
        controller.calibrationStatus.setText(state.getStepTitle());
        showAppropriateButtons(state);
        if (state.getDiagramFXMLFileName().isPresent())
        {
            controller.showDiagram(state.getDiagramFXMLFileName().get());
        }
        int stepNo = 0;
        switch (state)
        {
            case IDLE:
                break;
            case HEATING:
                controller.showSpinner();
                controller.calibrationMenu.disableNonSelectedItems();
                controller.setCalibrationProgressVisible(
                    CalibrationInsetPanelController.ProgressVisibility.TEMP);
                stepNo = 1;
                break;
            case NO_MATERIAL_CHECK:
                controller.buttonA.setText("Yes");
                controller.buttonB.setText("No");
                stepNo = 2;
                break;
            case T0_EXTRUDING:
                controller.buttonA.setText("Yes");
                controller.buttonB.setText("No");
                stepNo = 3;
                break;
            case T1_EXTRUDING:
                controller.buttonA.setText("Yes");
                controller.buttonB.setText("No");
                stepNo = 4;
                break;
            case HEAD_CLEAN_CHECK_AFTER_EXTRUDE:
                stepNo = 5;
                break;
            case PRE_CALIBRATION_PRIMING_FINE:
                break;
            case CALIBRATE_FINE_NOZZLE:
                controller.buttonA.setText("Flowing");
                controller.buttonB.setText("Not flowing");
                stepNo = 6;
                break;
            case CALIBRATE_FILL_NOZZLE:
                controller.buttonA.setText("Flowing");
                controller.buttonB.setText("Not flowing");
                stepNo = 7;
                break;
            case HEAD_CLEAN_CHECK_FILL_NOZZLE:
                stepNo = 8;
                break;
            case CONFIRM_NO_MATERIAL_NO_YESNO_BUTTONS:
            case CONFIRM_NO_MATERIAL:
                controller.buttonA.setText("Yes");
                controller.buttonB.setText("No");
                stepNo = 9;
                break;
            case FINISHED:
                controller.calibrationMenu.reset();
                break;
            case DONE:    
                controller.resetMenuAndGoToChoiceMode();
                break;
            case FAILED:
                controller.calibrationMenu.enableNonSelectedItems();
                break;
        }
        if (stepNo != 0)
        {
            controller.stepNumber.setText(String.format("Step %s of 9", stepNo));
        }
    }

    private void populateNamesToButtons(CalibrationInsetPanelController controller)
    {
        namesToButtons.put(GUIName.A_BUTTON, controller.buttonA);
        namesToButtons.put(GUIName.B_BUTTON, controller.buttonB);
        namesToButtons.put(GUIName.NEXT, controller.nextButton);
        namesToButtons.put(GUIName.RETRY, controller.retryPrintButton);
        namesToButtons.put(GUIName.START, controller.startCalibrationButton);
        namesToButtons.put(GUIName.BACK, controller.backToStatus);
    }

}
