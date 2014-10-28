/*
 * Copyright 2014 CEL UK
 */
package celtech.services.calibration;

import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.model.CalibrationNozzleOpeningActions;
import celtech.printerControl.model.calibration.ArrivalAction;
import celtech.printerControl.model.calibration.StateTransitionManager;
import celtech.printerControl.model.calibration.StateTransition;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class CalibrationNozzleOpeningTransitions implements Transitions
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationNozzleOpeningTransitions.class.getName());

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

        arrivals.put(NozzleOpeningCalibrationState.CONFIRM_MATERIAL_EXTRUDING,
                     new ArrivalAction<>(() ->
                         {
                             return actions.doConfirmMaterialExtrudingAction();
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
                                            NozzleOpeningCalibrationState.FAILED,
                                            NozzleOpeningCalibrationState.CANCELLED));

        // HEATING
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.HEATING,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOpeningCalibrationState.NO_MATERIAL_CHECK,
                                            (Callable) () ->
                                            {
                                                return actions.doHeatingAction();
                                            },
                                            NozzleOpeningCalibrationState.FAILED,
                                            NozzleOpeningCalibrationState.CANCELLED));

        // NO MATERIAL CHECK
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.NO_MATERIAL_CHECK,
                                            StateTransitionManager.GUIName.B_BUTTON,
                                            NozzleOpeningCalibrationState.PRE_CALIBRATION_PRIMING_FINE,
                                            (Callable) () ->
                                            {
                                                return actions.doPreCalibrationPrimingFine();
                                            },
                                            NozzleOpeningCalibrationState.FAILED,
                                            NozzleOpeningCalibrationState.CANCELLED));

        transitions.add(new StateTransition(NozzleOpeningCalibrationState.NO_MATERIAL_CHECK,
                                            StateTransitionManager.GUIName.A_BUTTON,
                                            NozzleOpeningCalibrationState.FAILED,
                                            NozzleOpeningCalibrationState.FAILED,
                                            NozzleOpeningCalibrationState.CANCELLED));

        // PRE_CALIBRATION_PRIMING_FINE
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.PRE_CALIBRATION_PRIMING_FINE,
            StateTransitionManager.GUIName.AUTO,
            NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE,
            (Callable) () ->
            {
                return actions.doCalibrateFineNozzle();
            },
            NozzleOpeningCalibrationState.FAILED));

        // CALIBRATE_FINE_NOZZLE
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE,
                                            StateTransitionManager.GUIName.B_BUTTON,
                                            NozzleOpeningCalibrationState.INCREMENT_FINE_NOZZLE_POSITION,
                                            (Callable) () ->
                                            {
                                                return actions.doIncrementFineNozzlePosition();
                                            },
                                            NozzleOpeningCalibrationState.FAILED,
                                            NozzleOpeningCalibrationState.CANCELLED));

        transitions.add(new StateTransition(NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE,
                                            StateTransitionManager.GUIName.A_BUTTON,
                                            NozzleOpeningCalibrationState.HEAD_CLEAN_CHECK_FINE_NOZZLE,
                                            (Callable) () ->
                                            {
                                                return actions.doFinaliseCalibrateFineNozzle();
                                            },
                                            NozzleOpeningCalibrationState.FAILED,
                                            NozzleOpeningCalibrationState.CANCELLED));

        // INCREMENT_FINE_NOZZLE_POSITION
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.INCREMENT_FINE_NOZZLE_POSITION,
            StateTransitionManager.GUIName.AUTO,
            NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE,
            NozzleOpeningCalibrationState.FAILED,
            NozzleOpeningCalibrationState.CANCELLED));

        // HEAD_CLEAN_CHECK_FINE_NOZZLE
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.HEAD_CLEAN_CHECK_FINE_NOZZLE,
            StateTransitionManager.GUIName.NEXT,
            NozzleOpeningCalibrationState.PRE_CALIBRATION_PRIMING_FILL,
            (Callable) () ->
            {
                return actions.doPreCalibrationPrimingFill();
            },
            NozzleOpeningCalibrationState.FAILED,
            NozzleOpeningCalibrationState.CANCELLED));

        // PRE_CALIBRATION_PRIMING_FILL
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.PRE_CALIBRATION_PRIMING_FILL,
            StateTransitionManager.GUIName.AUTO,
            NozzleOpeningCalibrationState.CALIBRATE_FILL_NOZZLE,
            (Callable) () ->
            {
                return actions.doCalibrateFillNozzle();
            },
            NozzleOpeningCalibrationState.FAILED));

        // CALIBRATE_FILL_NOZZLE
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.CALIBRATE_FILL_NOZZLE,
                                            StateTransitionManager.GUIName.B_BUTTON,
                                            NozzleOpeningCalibrationState.INCREMENT_FILL_NOZZLE_POSITION,
                                            (Callable) () ->
                                            {
                                                return actions.doIncrementFillNozzlePosition();
                                            },
                                            NozzleOpeningCalibrationState.FAILED,
                                            NozzleOpeningCalibrationState.CANCELLED));

        transitions.add(new StateTransition(NozzleOpeningCalibrationState.CALIBRATE_FILL_NOZZLE,
                                            StateTransitionManager.GUIName.A_BUTTON,
                                            NozzleOpeningCalibrationState.HEAD_CLEAN_CHECK_FILL_NOZZLE,
                                            (Callable) () ->
                                            {
                                                return actions.doFinaliseCalibrateFillNozzle();
                                            },
                                            NozzleOpeningCalibrationState.FAILED,
                                            NozzleOpeningCalibrationState.CANCELLED));

        // INCREMENT_FILL_NOZZLE_POSITION
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.INCREMENT_FILL_NOZZLE_POSITION,
            StateTransitionManager.GUIName.AUTO,
            NozzleOpeningCalibrationState.CALIBRATE_FILL_NOZZLE,
            NozzleOpeningCalibrationState.FAILED,
            NozzleOpeningCalibrationState.CANCELLED));

        // HEAD_CLEAN_CHECK_FILL_NOZZLE
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.HEAD_CLEAN_CHECK_FILL_NOZZLE,
            StateTransitionManager.GUIName.NEXT,
            NozzleOpeningCalibrationState.CONFIRM_NO_MATERIAL,
            (Callable) () ->
            {
                return actions.doConfirmNoMaterialAction();
            },
            NozzleOpeningCalibrationState.FAILED,
            NozzleOpeningCalibrationState.CANCELLED));

        // CONFIRM_NO_MATERIAL
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.CONFIRM_NO_MATERIAL,
            StateTransitionManager.GUIName.B_BUTTON,
            NozzleOpeningCalibrationState.CONFIRM_MATERIAL_EXTRUDING,
            NozzleOpeningCalibrationState.FAILED,
            NozzleOpeningCalibrationState.CANCELLED));

        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.CONFIRM_NO_MATERIAL,
            StateTransitionManager.GUIName.A_BUTTON,
            NozzleOpeningCalibrationState.FAILED,
            NozzleOpeningCalibrationState.FAILED,
            NozzleOpeningCalibrationState.CANCELLED));

        // CONFIRM_MATERIAL_EXTRUDING
        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.CONFIRM_MATERIAL_EXTRUDING,
            StateTransitionManager.GUIName.A_BUTTON,
            NozzleOpeningCalibrationState.FINISHED,
            NozzleOpeningCalibrationState.FAILED,
            NozzleOpeningCalibrationState.CANCELLED));

        transitions.add(new StateTransition(
            NozzleOpeningCalibrationState.CONFIRM_MATERIAL_EXTRUDING,
            StateTransitionManager.GUIName.B_BUTTON,
            NozzleOpeningCalibrationState.FAILED,
            NozzleOpeningCalibrationState.FAILED,
            NozzleOpeningCalibrationState.CANCELLED));

        // FINISHED
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.FINISHED,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOpeningCalibrationState.DONE,
                                            NozzleOpeningCalibrationState.FAILED,
                                            NozzleOpeningCalibrationState.CANCELLED));

        // FAILED
        transitions.add(new StateTransition(NozzleOpeningCalibrationState.FAILED,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOpeningCalibrationState.DONE,
                                            NozzleOpeningCalibrationState.DONE,
                                            NozzleOpeningCalibrationState.CANCELLED));

    }

    @Override
    public Set<StateTransition<NozzleOpeningCalibrationState>> getTransitions()
    {
        return transitions;
    }

    @Override
    public Map<NozzleOpeningCalibrationState, ArrivalAction<NozzleOpeningCalibrationState>> getArrivals()
    {
        return arrivals;
    }

    @Override
    public void cancel() throws RoboxCommsException
    {
        actions.cancel();
    }

}
