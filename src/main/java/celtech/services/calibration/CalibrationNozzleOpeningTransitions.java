/*
 * Copyright 2014 CEL UK
 */
package celtech.services.calibration;

import celtech.printerControl.model.CalibrationNozzleOpeningActions;
import celtech.printerControl.model.calibration.ArrivalAction;
import celtech.printerControl.model.calibration.StateTransitionManager;
import celtech.printerControl.model.calibration.StateTransition;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 *
 * @author tony
 */
public class CalibrationNozzleOpeningTransitions
{

    CalibrationNozzleOpeningActions actions;
    Set<StateTransition<NozzleOpeningCalibrationState>> transitions;
    Map<NozzleOpeningCalibrationState, ArrivalAction<NozzleOpeningCalibrationState>> arrivals;
    StateTransitionManager manager;

    public CalibrationNozzleOpeningTransitions(CalibrationNozzleOpeningActions actions)
    {
        this.actions = actions;
        arrivals = new HashMap<>();

        arrivals.put(NozzleOpeningCalibrationState.NO_MATERIAL_CHECK,
                     new ArrivalAction<>(() ->
                         {
                             return actions.doNoMaterialCheckAction();
                     },
                                         NozzleOpeningCalibrationState.FAILED));

        arrivals.put(NozzleOpeningCalibrationState.FINISHED,
                     new ArrivalAction<>(() ->
                         {
                             return actions.doFinishedAction();
                     },
                                         NozzleOpeningCalibrationState.FAILED));

        arrivals.put(NozzleOpeningCalibrationState.FAILED,
                     new ArrivalAction<>(() ->
                         {
                             return actions.doFailedAction();
                     },
                                         NozzleOpeningCalibrationState.FAILED));

        transitions = new HashSet<>();

        // IDLE
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.IDLE,
                                            StateTransitionManager.GUIName.START,
                                            NozzleOpeningCalibrationState.HEATING,
                                            NozzleOpeningCalibrationState.FAILED));

        // HEATING
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.HEATING,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOpeningCalibrationState.NO_MATERIAL_CHECK,
                                            (Callable) () ->
                                            {
                                                return actions.doHeatingAction();
                                            },
                                            NozzleOpeningCalibrationState.FAILED));

        transitions.add(makeCancelledStateTransition(NozzleOpeningCalibrationState.HEATING));

        // NO MATERIAL CHECK
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.NO_MATERIAL_CHECK,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOpeningCalibrationState.PRE_CALIBRATION_PRIMING_FINE,
                                            (Callable) () ->
                                            {
                                                return actions.doPreCalibrationPrimingFine();
                                            },
                                            NozzleOpeningCalibrationState.FAILED));

        transitions.add(
            makeCancelledStateTransition(NozzleOpeningCalibrationState.NO_MATERIAL_CHECK));

        // PRE_CALIBRATION_PRIMING_FINE
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.PRE_CALIBRATION_PRIMING_FINE,
            StateTransitionManager.GUIName.AUTO,
            NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE,
            (Callable) () ->
            {
                return actions.doCalibrateFillNozzle();
            },
            NozzleOpeningCalibrationState.FAILED));

        transitions.add(
            makeCancelledStateTransition(NozzleOpeningCalibrationState.PRE_CALIBRATION_PRIMING_FINE));

        // CALIBRATE_FINE_NOZZLE
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE,
                                            StateTransitionManager.GUIName.B_BUTTON,
                                            NozzleOpeningCalibrationState.INCREMENT_FINE_NOZZLE_POSITION,
                                            (Callable) () ->
                                            {
                                                return actions.doIncrementFineNozzlePosition();
                                            },
                                            NozzleOpeningCalibrationState.FAILED));

        transitions.add(new StateTransition(NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE,
                                            StateTransitionManager.GUIName.A_BUTTON,
                                            NozzleOpeningCalibrationState.HEAD_CLEAN_CHECK_FINE_NOZZLE,
                                            (Callable) () ->
                                            {
                                                return actions.doFinaliseCalibrateFillNozzle();
                                            },
                                            NozzleOpeningCalibrationState.FAILED));

        transitions.add(
            makeCancelledStateTransition(NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE));
        
        // INCREMENT_FINE_NOZZLE_POSITION
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.INCREMENT_FINE_NOZZLE_POSITION,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE,
                                            NozzleOpeningCalibrationState.FAILED));


        // HEAD_CLEAN_CHECK_FINE_NOZZLE
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.HEAD_CLEAN_CHECK_FINE_NOZZLE,
            StateTransitionManager.GUIName.NEXT,
            NozzleOpeningCalibrationState.PRE_CALIBRATION_PRIMING_FILL,
            NozzleOpeningCalibrationState.FAILED));

        transitions.add(
            makeCancelledStateTransition(NozzleOpeningCalibrationState.HEAD_CLEAN_CHECK_FINE_NOZZLE));

        // FINISHED
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.FINISHED,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOpeningCalibrationState.IDLE,
                                            NozzleOpeningCalibrationState.FAILED));

        // FAILED
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.FAILED,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOpeningCalibrationState.IDLE,
                                            NozzleOpeningCalibrationState.IDLE));

    }

    private StateTransition makeCancelledStateTransition(NozzleOpeningCalibrationState fromState)
    {
        return new StateTransition(fromState,
                                   StateTransitionManager.GUIName.CANCEL,
                                   NozzleOpeningCalibrationState.IDLE,
                                   (Callable) () ->
                                   {
                                       return actions.doCancelledAction();
                                   },
                                   NozzleOpeningCalibrationState.FAILED);
    }

    public Set<StateTransition<NozzleOpeningCalibrationState>> getTransitions()
    {
        return transitions;
    }

    public Map<NozzleOpeningCalibrationState, ArrivalAction<NozzleOpeningCalibrationState>> getArrivals()
    {
        return arrivals;
    }

}
