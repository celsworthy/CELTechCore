/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

/**
 *
 * @author tony
 */
public class PurgeStateTransitionManager extends StateTransitionManager<PurgeState>
{
    private final PurgeActions actions;

    public PurgeStateTransitionManager(Transitions<PurgeState> transitions, PurgeActions actions)
    {
        super(transitions, PurgeState.IDLE,
              PurgeState.CANCELLED);
        this.actions = actions;
    }
    
}
