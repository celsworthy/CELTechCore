/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.JavaFXConfiguredTest;
import celtech.printerControl.model.calibration.StateTransitionManager.GUIName;
import celtech.services.calibration.CalibrationXAndYState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import javafx.beans.value.ObservableValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class StateTransitionManagerTest extends JavaFXConfiguredTest
{

    Set<StateTransition> transitions;
    TestStateTransitionManager manager;

    @Before
    public void setUp()
    {
        TestActions actions = new TestActions();
        transitions = new HashSet<>();
        transitions.add(new StateTransition(CalibrationXAndYState.IDLE,
                                            StateTransitionManager.GUIName.NEXT,
                                            CalibrationXAndYState.PRINT_CIRCLE,
                                            (Callable) () ->
                                            {
                                                return actions.doAction1();
                                            }));

        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_CIRCLE,
                                            StateTransitionManager.GUIName.NEXT,
                                            CalibrationXAndYState.GET_Y_OFFSET,
                                            (Callable) () ->
                                            {
                                                return actions.doAction2();
                                            }));

        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_CIRCLE,
                                            StateTransitionManager.GUIName.COMPLETE,
                                            CalibrationXAndYState.GET_Y_OFFSET,
                                            (Callable) () ->
                                            {
                                                return actions.doAction1ButFails();
                                            }));

        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_CIRCLE,
                                            StateTransitionManager.GUIName.CANCEL,
                                            CalibrationXAndYState.FAILED,
                                            (Callable) () ->
                                            {
                                                return actions.doCancelled();
                                            }));

        manager = new TestStateTransitionManager(transitions, actions);

    }

    @Test
    public void testSetAndGetTransitions()
    {

        Set<StateTransition> allowedTransitions = manager.getTransitions();
        assertEquals(1, allowedTransitions.size());
        assertEquals(GUIName.NEXT, allowedTransitions.iterator().next().guiName);
    }

    @Test
    public void testFollowTransitionFromIdleByNext()
    {
        manager.followTransition(GUIName.NEXT);
        assertEquals(CalibrationXAndYState.PRINT_CIRCLE, manager.stateProperty().get());
        assertEquals(10, manager.getX());
    }

    @Test
    public void testFollow2TransitionsFromIdleToGetYOffset()
    {
        manager.followTransition(GUIName.NEXT);
        manager.followTransition(GUIName.NEXT);
        assertEquals(CalibrationXAndYState.GET_Y_OFFSET, manager.stateProperty().get());
        assertEquals(11, manager.getX());
    }

    @Test
    public void testFollow2TransitionsFromIdleToPrintCircleToFailed()
    {
        manager.followTransition(GUIName.NEXT);
        manager.followTransition(GUIName.CANCEL);
        assertEquals(CalibrationXAndYState.FAILED, manager.stateProperty().get());
        assertEquals(10, manager.getX());
        assertTrue(manager.isCancelled());
    }

    @Test
    public void testFailedTransitionsEndsInFailedState()
    {
        manager.followTransition(GUIName.NEXT);
        manager.followTransition(GUIName.COMPLETE);
        assertEquals(CalibrationXAndYState.FAILED, manager.stateProperty().get());
        assertEquals(22, manager.getX());
    }

    @Test
    public void testStateListenerCorrectlyUpdated()
    {
        final List<CalibrationXAndYState> states = new ArrayList<>();
        manager.stateProperty().addListener(
            (ObservableValue<? extends CalibrationXAndYState> observable, CalibrationXAndYState oldValue, CalibrationXAndYState newValue) ->
            {
                states.add(newValue);
            });
        manager.followTransition(GUIName.NEXT);
        manager.followTransition(GUIName.NEXT);
        assertEquals(2, states.size());
        assertEquals(CalibrationXAndYState.PRINT_CIRCLE, states.get(0));
        assertEquals(CalibrationXAndYState.GET_Y_OFFSET, states.get(1));
    }

    static class TestActions
    {

        int x = 0;
        boolean cancelled = false;

        private boolean doAction1()
        {
            x += 10;
            return true;
        }

        private boolean doAction1ButFails()
        {
            x += 12;
            return false;
        }

        private boolean doAction2()
        {
            x += 1;
            return true;
        }

        private boolean doCancelled()
        {
            cancelled = true;
            return true;
        }
    }

    public class TestStateTransitionManager extends StateTransitionManager
    {

        private final TestActions actions;

        public TestStateTransitionManager(Set<StateTransition> allowedTransitions,
            TestActions actions)
        {
            super(allowedTransitions);
            this.actions = actions;
        }
        
        public int getX() {
            return actions.x;
        }
        
        public boolean isCancelled() {
            return actions.cancelled;
        }

    }
}
