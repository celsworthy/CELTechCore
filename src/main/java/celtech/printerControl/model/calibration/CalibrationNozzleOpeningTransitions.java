/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.Transitions;
import celtech.printerControl.model.CalibrationNozzleOpeningActions;
import celtech.printerControl.model.ArrivalAction;
import celtech.printerControl.model.StateTransitionManager;
import celtech.printerControl.model.StateTransition;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author tony
 */
public class CalibrationNozzleOpeningTransitions extends Transitions<NozzleOpeningCalibrationState>
{

    public CalibrationNozzleOpeningTransitions(CalibrationNozzleOpeningActions actions)
    {
        
        arrivals = new HashMap<>();

        arrivals.put(NozzleOpeningCalibrationState.T0_EXTRUDING,
                     new ArrivalAction<>(() ->
                         {
                             actions.doT0Extrusion();
                     },
                                         NozzleOpeningCalibrationState.FAILED));

        arrivals.put(NozzleOpeningCalibrationState.T1_EXTRUDING,
                     new ArrivalAction<>(() ->
                         {
                             actions.doT1Extrusion();
                     },
                                         NozzleOpeningCalibrationState.FAILED));

        arrivals.put(NozzleOpeningCalibrationState.FINISHED,
                     new ArrivalAction<>(() ->
                         {
                             actions.doFinishedAction();
                     },
                                         NozzleOpeningCalibrationState.FAILED));

        arrivals.put(NozzleOpeningCalibrationState.FAILED,
                     new ArrivalAction<>(() ->
                         {
                             actions.doFailedAction();
                     },
                                         NozzleOpeningCalibrationState.DONE));

        transitions = new HashSet<>();

        // IDLE
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.IDLE,
                                            StateTransitionManager.GUIName.START,
                                            NozzleOpeningCalibrationState.HEATING,
                                            NozzleOpeningCalibrationState.FAILED));

        transitions.add(new StateTransition(NozzleOpeningCalibrationState.IDLE,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOpeningCalibrationState.DONE,
                                            NozzleOpeningCalibrationState.FAILED));

        // HEATING
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.HEATING,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOpeningCalibrationState.NO_MATERIAL_CHECK_NO_YES_NO_BUTTONS,
                                            () ->
                                            {
                                                actions.doHeatingAction();
                                            },
                                            NozzleOpeningCalibrationState.FAILED));

        // NO_MATERIAL_CHECK_NO_YES_NO_BUTTONS
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.NO_MATERIAL_CHECK_NO_YES_NO_BUTTONS,
            StateTransitionManager.GUIName.AUTO,
            NozzleOpeningCalibrationState.NO_MATERIAL_CHECK,
            () ->
            {
                actions.doNoMaterialCheckAction();
            },
            NozzleOpeningCalibrationState.FAILED));

        // NO MATERIAL CHECK
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.NO_MATERIAL_CHECK,
                                            StateTransitionManager.GUIName.B_BUTTON,
                                            NozzleOpeningCalibrationState.T0_EXTRUDING,
                                            NozzleOpeningCalibrationState.FAILED));

        transitions.add(new StateTransition(NozzleOpeningCalibrationState.NO_MATERIAL_CHECK,
                                            StateTransitionManager.GUIName.A_BUTTON,
                                            NozzleOpeningCalibrationState.FAILED,
                                            NozzleOpeningCalibrationState.FAILED));

        // T0_EXTRUDING
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.T0_EXTRUDING,
                                            StateTransitionManager.GUIName.A_BUTTON,
                                            NozzleOpeningCalibrationState.T1_EXTRUDING,
                                            NozzleOpeningCalibrationState.FAILED));

        transitions.add(new StateTransition(NozzleOpeningCalibrationState.T0_EXTRUDING,
                                            StateTransitionManager.GUIName.B_BUTTON,
                                            NozzleOpeningCalibrationState.FAILED,
                                            NozzleOpeningCalibrationState.FAILED));

        // T1_EXTRUDING
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.T1_EXTRUDING,
                                            StateTransitionManager.GUIName.A_BUTTON,
                                            NozzleOpeningCalibrationState.HEAD_CLEAN_CHECK_AFTER_EXTRUDE,
                                            NozzleOpeningCalibrationState.FAILED));

        transitions.add(new StateTransition(NozzleOpeningCalibrationState.T1_EXTRUDING,
                                            StateTransitionManager.GUIName.B_BUTTON,
                                            NozzleOpeningCalibrationState.FAILED,
                                            NozzleOpeningCalibrationState.FAILED));

        // HEAD_CLEAN_CHECK_AFTER_EXTRUDE
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.HEAD_CLEAN_CHECK_AFTER_EXTRUDE,
            StateTransitionManager.GUIName.NEXT,
            NozzleOpeningCalibrationState.PRE_CALIBRATION_PRIMING_FINE,
            () ->
            {
                actions.doPreCalibrationPrimingFine();
            },
            NozzleOpeningCalibrationState.FAILED));

        // PRE_CALIBRATION_PRIMING_FINE
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.PRE_CALIBRATION_PRIMING_FINE,
            StateTransitionManager.GUIName.AUTO,
            NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE,
            () ->
            {
                actions.doCalibrateFineNozzle();
            },
            NozzleOpeningCalibrationState.FAILED));

        // CALIBRATE_FINE_NOZZLE
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE,
                                            StateTransitionManager.GUIName.B_BUTTON,
                                            NozzleOpeningCalibrationState.INCREMENT_FINE_NOZZLE_POSITION_NO_BUTTONS,
                                            NozzleOpeningCalibrationState.FAILED));

        transitions.add(new StateTransition(NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE,
                                            StateTransitionManager.GUIName.A_BUTTON,
                                            NozzleOpeningCalibrationState.PRE_CALIBRATION_PRIMING_FILL,
                                            () ->
                                            {
                                                actions.doFinaliseCalibrateFineNozzle();
                                                actions.doPreCalibrationPrimingFill();
                                            },
                                            NozzleOpeningCalibrationState.FAILED));

        // INCREMENT_FINE_NOZZLE_POSITION
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.INCREMENT_FINE_NOZZLE_POSITION_NO_BUTTONS,
            StateTransitionManager.GUIName.AUTO,
            NozzleOpeningCalibrationState.INCREMENT_FINE_NOZZLE_POSITION,
            () ->
            {
                actions.doIncrementFineNozzlePosition();
            },
            NozzleOpeningCalibrationState.FAILED));

        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.INCREMENT_FINE_NOZZLE_POSITION,
            StateTransitionManager.GUIName.AUTO,
            NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE,
            NozzleOpeningCalibrationState.FAILED));

        // PRE_CALIBRATION_PRIMING_FILL
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.PRE_CALIBRATION_PRIMING_FILL,
            StateTransitionManager.GUIName.AUTO,
            NozzleOpeningCalibrationState.CALIBRATE_FILL_NOZZLE,
            () ->
            {
                actions.doCalibrateFillNozzle();
            },
            NozzleOpeningCalibrationState.FAILED));

        // CALIBRATE_FILL_NOZZLE
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.CALIBRATE_FILL_NOZZLE,
                                            StateTransitionManager.GUIName.B_BUTTON,
                                            NozzleOpeningCalibrationState.INCREMENT_FILL_NOZZLE_POSITION_NO_BUTTONS,
                                            NozzleOpeningCalibrationState.FAILED));

        transitions.add(new StateTransition(NozzleOpeningCalibrationState.CALIBRATE_FILL_NOZZLE,
                                            StateTransitionManager.GUIName.A_BUTTON,
                                            NozzleOpeningCalibrationState.HEAD_CLEAN_CHECK_FILL_NOZZLE,
                                            () ->
                                            {
                                                actions.doFinaliseCalibrateFillNozzle();
                                            },
                                            NozzleOpeningCalibrationState.FAILED));

        // INCREMENT_FILL_NOZZLE_POSITION
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.INCREMENT_FILL_NOZZLE_POSITION_NO_BUTTONS,
            StateTransitionManager.GUIName.AUTO,
            NozzleOpeningCalibrationState.INCREMENT_FILL_NOZZLE_POSITION,
            () ->
            {
                actions.doIncrementFillNozzlePosition();
            },
            NozzleOpeningCalibrationState.FAILED));

        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.INCREMENT_FILL_NOZZLE_POSITION,
            StateTransitionManager.GUIName.AUTO,
            NozzleOpeningCalibrationState.CALIBRATE_FILL_NOZZLE,
            NozzleOpeningCalibrationState.FAILED));

        // HEAD_CLEAN_CHECK_FILL_NOZZLE
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.HEAD_CLEAN_CHECK_FILL_NOZZLE,
            StateTransitionManager.GUIName.NEXT,
            NozzleOpeningCalibrationState.CONFIRM_NO_MATERIAL_NO_YESNO_BUTTONS,
            NozzleOpeningCalibrationState.FAILED));

        // CONFIRM_NO_MATERIAL_NO_YESNO_BUTTON
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.CONFIRM_NO_MATERIAL_NO_YESNO_BUTTONS,
            StateTransitionManager.GUIName.AUTO,
            NozzleOpeningCalibrationState.CONFIRM_NO_MATERIAL,
            () ->
            {
                actions.doConfirmNoMaterialAction();
            },
            NozzleOpeningCalibrationState.FAILED));

        // CONFIRM_NO_MATERIAL
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.CONFIRM_NO_MATERIAL,
            StateTransitionManager.GUIName.B_BUTTON,
            NozzleOpeningCalibrationState.FINISHED,
            NozzleOpeningCalibrationState.FAILED));

        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.CONFIRM_NO_MATERIAL,
            StateTransitionManager.GUIName.A_BUTTON,
            NozzleOpeningCalibrationState.FAILED,
            NozzleOpeningCalibrationState.FAILED));

        // FINISHED
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.FINISHED,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOpeningCalibrationState.DONE,
                                            NozzleOpeningCalibrationState.FAILED));

        // FAILED
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.FAILED,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOpeningCalibrationState.DONE,
                                            NozzleOpeningCalibrationState.DONE));
        
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.FAILED,
                                            StateTransitionManager.GUIName.RETRY,
                                            NozzleOpeningCalibrationState.IDLE,
                                            NozzleOpeningCalibrationState.DONE));        
    }

}
