/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import celtech.configuration.Filament;

/**
 * The PurgeStateTransitionManager manages state and transitions between different states, and also
 * functions as the data transfer interface between the StateActions instance and the GUI.
 *
 * @author tony
 */
public class PurgeStateTransitionManager extends StateTransitionManager<PurgeState>
{

    public PurgeStateTransitionManager(StateTransitionActionsFactory stateTransitionActionsFactory,
        TransitionsFactory transitionsFactory)
    {
        super(stateTransitionActionsFactory, transitionsFactory, PurgeState.IDLE,
              PurgeState.CANCELLED, PurgeState.FAILED);
    }

    public void setPurgeTemperature(int purgeTemperature)
    {
        ((PurgeActions) actions).setPurgeTemperature(purgeTemperature);
    }

    public int getLastMaterialTemperature()
    {
        return ((PurgeActions) actions).getLastMaterialTemperature();
    }

    public int getCurrentMaterialTemperature()
    {
        return ((PurgeActions) actions).getCurrentMaterialTemperature();
    }

    public int getPurgeTemperature()
    {
        return ((PurgeActions) actions).getPurgeTemperature();
    }

    public void setPurgeFilament(Filament filament)
    {
        ((PurgeActions) actions).setPurgeFilament(filament);
    }

}
