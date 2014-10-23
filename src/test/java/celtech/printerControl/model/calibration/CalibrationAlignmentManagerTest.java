/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.JavaFXConfiguredTest;
import celtech.printerControl.model.calibration.CalibrationAlignmentManager.GUIName;
import celtech.services.calibration.CalibrationXAndYState;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class CalibrationAlignmentManagerTest extends JavaFXConfiguredTest
{

    Set<StateTransition> transitions;
    CalibrationAlignmentManager manager;
    TestResults results;

    @Before
    public void setUp()
    {
        results = new TestResults();
        transitions = new HashSet<>();
        transitions.add(new StateTransition(CalibrationXAndYState.IDLE,
                                            CalibrationXAndYState.PRINT_CIRCLE,
                                            CalibrationAlignmentManager.GUIName.NEXT,
                                            (Callable) () ->
                                            {
                                                return results.doAction1();
                                            }));

        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_CIRCLE,
                                            CalibrationXAndYState.GET_Y_OFFSET,
                                            CalibrationAlignmentManager.GUIName.NEXT,
                                            (Callable) () ->
                                            {
                                                return results.doAction2();
                                            }));
        
        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_CIRCLE,
                                            CalibrationXAndYState.FAILED,
                                            CalibrationAlignmentManager.GUIName.CANCEL,
                                            (Callable) () ->
                                            {
                                                return results.doCancelled();
                                            }));        

        manager = new CalibrationAlignmentManager(transitions);
        
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
        assertEquals(10, results.x);
    }
    
    @Test
    public void testFollow2TransitionsFromIdleToGetYOffset()
    {
        manager.followTransition(GUIName.NEXT);
        manager.followTransition(GUIName.NEXT);
        assertEquals(CalibrationXAndYState.GET_Y_OFFSET, manager.stateProperty().get());
        assertEquals(11, results.x);
    }    
    
    @Test
    public void testFollow2TransitionsFromIdleToPrintCircleToFailed()
    {
        manager.followTransition(GUIName.NEXT);
        manager.followTransition(GUIName.CANCEL);
        assertEquals(CalibrationXAndYState.FAILED, manager.stateProperty().get());
        assertEquals(10, results.x);
        assertTrue(results.cancelled);
    }       

    static class TestResults
    {

        int x = 0;
        boolean cancelled = false;
        
        private boolean doAction1()
        {
            x += 10;
            return true;
        }

        private boolean doAction2()
        {
            x += 1;
            return true;
        }

        private void doCancelled()
        {
            cancelled = true;
        }
    }
}
