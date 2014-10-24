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
        CalibrationXAndYHelper.class.getName());

    private CalibrationInsetPanelController controller;
    StateTransitionManager<NozzleOffsetCalibrationState> stateManager;
    Map<GUIName, Button> namesToButtons = new HashMap<>();

    public CalibrationNozzleHeightGUI(CalibrationInsetPanelController controller,
        StateTransitionManager<NozzleOffsetCalibrationState> stateManager)
    {
        this.controller = controller;
        this.stateManager = stateManager;
        
        stateManager.stateProperty().addListener(new ChangeListener()
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
        for (StateTransition<NozzleOffsetCalibrationState> allowedTransition : this.stateManager.getTransitions())
        {
            if (namesToButtons.containsKey(allowedTransition.getGUIName()))
            {
                namesToButtons.get(allowedTransition.getGUIName()).setVisible(true);
                steno.info(("Show button " + allowedTransition.getGUIName()));
            }
        }
    }

    public void setState(NozzleOffsetCalibrationState state)
    {
        steno.info("GUI going to state " + state);
        controller.updateZCO();
        showAppropriateButtons(state);
        switch (state)
        {
         case IDLE:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 1 and 5.fxml");
                controller.stepNumber.setText(String.format("Step %s of 9", 1));
                break;
            case INITIALISING:
                controller.calibrationMenu.disableNonSelectedItems();
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 2.fxml");
                controller.stepNumber.setText(String.format("Step %s of 9", 2));
                break;
            case HEATING:
                controller.showSpinner();
                controller.setCalibrationProgressVisible(CalibrationInsetPanelController.ProgressVisibility.TEMP);
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.stepNumber.setText(String.format("Step %s of 9", 3));
                break;
            case HEAD_CLEAN_CHECK:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 4.fxml");
                controller.stepNumber.setText(String.format("Step %s of 9", 4));
                break;
            case MEASURE_Z_DIFFERENCE:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 1 and 5.fxml");
                controller.stepNumber.setText(String.format("Step %s of 9", 5));
                break;
            case INSERT_PAPER:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 6.fxml");
                controller.stepNumber.setText(String.format("Step %s of 9", 6));
                break;
            case PROBING:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 7.fxml", false); 
                controller.stepNumber.setText(String.format("Step %s of 9", 7));
                break;
            case LIFT_HEAD:
                break;
            case REPLACE_PEI_BED:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 8.fxml");
                controller.stepNumber.setText(String.format("Step %s of 9", 8));
                break;
            case FINISHED:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Step 9.fxml");
                controller.stepNumber.setText(String.format("Step %s of 9", 9));
                break;
            case FAILED:
                controller.calibrationStatus.setText(state.getStepTitle());
                controller.showDiagram("nozzleheight", "Nozzle Height Illustrations_Failure.fxml");
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
