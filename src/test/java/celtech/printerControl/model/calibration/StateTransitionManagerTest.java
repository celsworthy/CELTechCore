/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.JavaFXConfiguredTest;
import celtech.printerControl.model.calibration.StateTransitionManager.GUIName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    enum TestState
    {

        IDLE, PRINT_CIRCLE, GET_Y_OFFSET, DONE, FAILED, CANCELLED;
    }

    TestStateTransitionManager manager;

    @Before
    public void setUp()
    {
        TestActions actions = new TestActions();
        Transitions transitions = new TestTransitions(actions);
        manager = new TestStateTransitionManager(transitions, actions);

    }

//    @Test
//    public void testSetAndGetTransitions()
//    {
//        Set<StateTransition> allowedTransitions = manager.getTransitions();
//        assertEquals(1, allowedTransitions.size());
//        assertEquals(GUIName.NEXT, allowedTransitions.iterator().next().guiName);
//    }
//
//    @Test
//    public void testFollowTransitionFromIdleByNext()
//    {
//        manager.followTransition(GUIName.NEXT);
//        assertEquals(TestState.PRINT_CIRCLE, manager.stateGUITProperty().get());
//        assertEquals(10, manager.getX());
//    }
//
//    @Test
//    public void testFollow2TransitionsFromIdleToGetYOffset()
//    {
//        manager.followTransition(GUIName.NEXT);
//        manager.followTransition(GUIName.NEXT);
//        assertEquals(TestState.GET_Y_OFFSET, manager.stateGUITProperty().get());
//        assertEquals(11, manager.getX());
//    }
//
//    @Test
//    public void testFollow2TransitionsFromIdleToPrintCircleToFailed()
//    {
//        manager.followTransition(GUIName.NEXT);
//        manager.followTransition(GUIName.CANCEL);
//        assertEquals(TestState.FAILED, manager.stateGUITProperty().get());
//        assertEquals(10, manager.getX());
//        assertTrue(manager.isCancelled());
//    }
//
//    @Test
//    public void testFailedTransitionsEndsInFailedState()
//    {
//        manager.followTransition(GUIName.NEXT);
//        manager.followTransition(GUIName.COMPLETE);
//        assertEquals(TestState.FAILED, manager.stateGUITProperty().get());
//        assertEquals(22, manager.getX());
//    }
//
//    @Test
//    public void testCancelledTransitionsEndsInCancelledState()
//    {
//        manager.followTransition(GUIName.NEXT);
//        manager.followTransition(GUIName.UP);
//        assertEquals(TestState.CANCELLED, manager.stateGUITProperty().get());
//        assertEquals(22, manager.getX());
//    }
//
//    @Test
//    public void testStateListenerCorrectlyUpdated()
//    {
//        final List<TestState> states = new ArrayList<>();
//
//        manager.stateGUITProperty().addListener(
//            (ObservableValue observable, Object oldValue, Object newValue) ->
//            {
//                states.add((TestState) newValue);
//            });
//
//        manager.followTransition(GUIName.NEXT);
//        manager.followTransition(GUIName.NEXT);
//        assertEquals(2, states.size());
//        assertEquals(TestState.PRINT_CIRCLE, states.get(0));
//        assertEquals(TestState.GET_Y_OFFSET, states.get(1));
//    }
//
//    @Test
//    public void testArrivalActionPerformed()
//    {
//        manager.followTransition(GUIName.NEXT);
//        manager.followTransition(GUIName.NEXT);
//        manager.followTransition(GUIName.NEXT);
//        assertEquals(43, manager.getX());
//    }

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
            throw new RuntimeException();
        }

        private boolean doAction1ButCancelled()
        {
            x += 12;
            return false;
        }

        private boolean doDoneAction()
        {
            x = 43;
            return true;
        }

        private boolean doAction2()
        {
            x += 1;
            return true;
        }

        private boolean cancel()
        {
            cancelled = true;
            return true;
        }
    }

    public class TestTransitions implements Transitions
    {

        TestActions actions;
        Set<StateTransition<TestState>> transitions;
        Map<TestState, ArrivalAction<TestState>> arrivals;

        public TestTransitions(TestActions actions)
        {
            this.actions = actions;
            transitions = new HashSet<>();
            transitions.add(new StateTransition(TestState.IDLE,
                                                StateTransitionManager.GUIName.NEXT,
                                                TestState.PRINT_CIRCLE,
                                                (Callable) () ->
                                                {
                                                    return actions.doAction1();
                                                },
                                                TestState.FAILED));

            transitions.add(new StateTransition(TestState.PRINT_CIRCLE,
                                                StateTransitionManager.GUIName.NEXT,
                                                TestState.GET_Y_OFFSET,
                                                (Callable) () ->
                                                {
                                                    return actions.doAction2();
                                                },
                                                TestState.FAILED));

            transitions.add(new StateTransition(TestState.GET_Y_OFFSET,
                                                StateTransitionManager.GUIName.NEXT,
                                                TestState.DONE,
                                                TestState.FAILED));

            transitions.add(new StateTransition(TestState.PRINT_CIRCLE,
                                                StateTransitionManager.GUIName.COMPLETE,
                                                TestState.GET_Y_OFFSET,
                                                (Callable) () ->
                                                {
                                                    return actions.doAction1ButFails();
                                                },
                                                TestState.FAILED));

            transitions.add(new StateTransition(TestState.PRINT_CIRCLE,
                                                StateTransitionManager.GUIName.UP,
                                                TestState.GET_Y_OFFSET,
                                                (Callable) () ->
                                                {
                                                    return actions.doAction1ButCancelled();
                                                },
                                                TestState.FAILED));

            transitions.add(new StateTransition(TestState.PRINT_CIRCLE,
                                                StateTransitionManager.GUIName.CANCEL,
                                                TestState.FAILED,
                                                (Callable) () ->
                                                {
                                                    return actions.cancel();
                                                },
                                                TestState.FAILED));

            arrivals = new HashMap<>();

            arrivals.put(TestState.DONE, new ArrivalAction<>((Callable) () ->
                     {
                         return actions.doDoneAction();
            }, TestState.FAILED));

        }

        @Override
        public Set getTransitions()
        {
            return transitions;
        }

        @Override
        public Map getArrivals()
        {
            return arrivals;
        }

        @Override
        public void cancel() throws Exception
        {
            actions.cancel();
        }

    }

    public class TestStateTransitionManager extends StateTransitionManager
    {

        private final TestActions actions;

        public TestStateTransitionManager(Transitions transitions, TestActions actions)
        {
            super(transitions, TestState.IDLE, TestState.CANCELLED);
            this.actions = actions;
        }

        public int getX()
        {
            return actions.x;
        }

        public boolean isCancelled()
        {
            return actions.cancelled;
        }

    }
}
