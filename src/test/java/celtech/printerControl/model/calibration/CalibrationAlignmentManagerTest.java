/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.JavaFXConfiguredTest;
import celtech.printerControl.comms.PrinterStatusConsumer;
import celtech.printerControl.comms.TestCommandInterface;
import celtech.printerControl.model.HardwarePrinter;
import celtech.services.calibration.CalibrationXAndYState;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class CalibrationAlignmentManagerTest extends JavaFXConfiguredTest
{

    private TestCommandInterface testCommandInterface = null;
    private HardwarePrinter printer = null;
    private final int statusTimer = 500;
    private StatusConsumer statusConsumer = null;

    @Before
    public void setUp()
    {
        statusConsumer = new StatusConsumer();
        testCommandInterface = new TestCommandInterface(statusConsumer, "Test Printer", false,
                                                        statusTimer);
        printer = new HardwarePrinter(null, testCommandInterface);
    }

    @Test
    public void testSetAndGetTransitions()
    {
        Set<StateTransition> transitions = new HashSet<>();
        transitions.add(new StateTransition(CalibrationXAndYState.IDLE,
                                            CalibrationXAndYState.PRINT_CIRCLE,
                                            CalibrationAlignmentManager.GUIName.NEXT,
                                            (Callable) () ->
                                            {
                                                doAction1();
                                                return null;
                                            }));

        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_CIRCLE,
                                            CalibrationXAndYState.GET_Y_OFFSET,
                                            CalibrationAlignmentManager.GUIName.NEXT,
                                            (Callable) () ->
                                            {
                                                doAction2();
                                                return null;
                                            }));

        CalibrationAlignmentManager manager = new CalibrationAlignmentManager(printer);
        manager.setTransitions(transitions);
        Set<StateTransition> allowedTransitions = manager.getTransitions(CalibrationXAndYState.IDLE);

        assertEquals(1, allowedTransitions.size());
    }

    private void doAction1()
    {

    }

    private void doAction2()
    {

    }

    class StatusConsumer implements PrinterStatusConsumer
    {

        @Override
        public void printerConnected(String portName)
        {
        }

        @Override
        public void failedToConnect(String portName)
        {
        }

        @Override
        public void disconnected(String portName)
        {
        }
    }

}
