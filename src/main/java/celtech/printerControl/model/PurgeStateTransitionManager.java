/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import celtech.configuration.Filament;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javax.print.PrintException;

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
              PurgeState.CANCELLING, PurgeState.CANCELLED, PurgeState.FAILED);
    }

    public void setPurgeTemperature(int purgeTemperature)
    {
        ((PurgeActions) actions).setPurgeTemperature(purgeTemperature);
    }

    public ReadOnlyIntegerProperty getLastMaterialTemperature()
    {
        return ((PurgeActions) actions).getLastMaterialTemperatureProperty();
    }

    public ReadOnlyIntegerProperty getCurrentMaterialTemperature()
    {
        return ((PurgeActions) actions).getCurrentMaterialTemperatureProperty();
    }

    public ReadOnlyIntegerProperty getPurgeTemperature()
    {
        return ((PurgeActions) actions).getPurgeTemperatureProperty();
    }

    public void setPurgeFilament(Filament filament) throws PrintException
    {
        ((PurgeActions) actions).setPurgeFilament(filament);
    }

}
