/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.printerControl.model.calibration.StateTransitionManager;
import celtech.printerControl.model.calibration.StateTransitionManager.GUIName;
import celtech.printerControl.model.calibration.StateTransition;
import celtech.services.calibration.NozzleOffsetCalibrationState;
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
public class CalibrationNozzleHeightGUI
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationNozzleHeightGUI.class.getName());

    private CalibrationInsetPanelController controller;
    StateTransitionManager<NozzleOffsetCalibrationState> stateManager;
    Map<GUIName, Button> namesToButtons = new HashMap<>();

    public CalibrationNozzleHeightGUI(CalibrationInsetPanelController controller,
        StateTransitionManager<NozzleOffsetCalibrationState> stateManager)
    {
        this.controller = controller;
        this.stateManager = stateManager;

        stateManager.stateGUITProperty().addListener(new ChangeListener()
        {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue)
            {
                setState((NozzleOffsetCalibrationState) newValue);
            }
        });
        populateNamesToButtons(controller);
    }

    private void showAppropriateButtons(NozzleOffsetCalibrationState state)
    {
        controller.hideAllInputControlsExceptStepNumber();
        if (state.showCancelButton())
        {
            controller.cancelCalibrationButton.setVisible(true);
        }
        for (StateTransition<NozzleOffsetCalibrationState> allowedTransition : this.stateManager.getTransitions())
        {
            if (namesToButtons.containsKey(allowedTransition.getGUIName()))
            {
                namesToButtons.get(allowedTransition.getGUIName()).setVisible(true);
            }
        }
    }

    public void setState(NozzleOffsetCalibrationState state)
    {
        steno.info("GUI going to state " + state);
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
            case INITIALISING:
                controller.calibrationMenu.disableNonSelectedItems();
                stepNo = 1;
                break;
            case HEATING:
                controller.showSpinner();
                controller.setCalibrationProgressVisible(
                    CalibrationInsetPanelController.ProgressVisibility.TEMP);
                stepNo = 2;
                break;
            case HEAD_CLEAN_CHECK:
                stepNo = 3;
                break;
            case MEASURE_Z_DIFFERENCE:
                stepNo = 4;
                break;
            case INSERT_PAPER:
                stepNo = 5;
                break;
            case PROBING:
                stepNo = 6;
                break;
            case LIFT_HEAD:
                break;
            case REPLACE_PEI_BED:
                stepNo = 7;
                break;
            case DONE:
                controller.resetMenuAndGoToChoiceMode();
                break;
            case FINISHED:
                controller.calibrationMenu.reset();
                break;
            case FAILED:
                controller.calibrationMenu.enableNonSelectedItems();
                break;
        }
        if (stepNo != 0)
        {
            controller.stepNumber.setText(String.format("Step %s of 7", stepNo));
        }
    }

    private void populateNamesToButtons(CalibrationInsetPanelController controller)
    {
        namesToButtons.put(GUIName.YES, controller.buttonA);
        namesToButtons.put(GUIName.NO, controller.buttonB);
        namesToButtons.put(GUIName.NEXT, controller.nextButton);
        namesToButtons.put(GUIName.RETRY, controller.retryPrintButton);
        namesToButtons.put(GUIName.START, controller.startCalibrationButton);
        namesToButtons.put(GUIName.BACK, controller.backToStatus);
    }

}
