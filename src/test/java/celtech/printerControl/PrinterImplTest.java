/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl;

import celtech.JavaFXConfiguredTest;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.comms.events.RoboxEvent;
import celtech.printerControl.comms.events.RoboxEventType;
import javafx.application.Application;
import javafx.stage.Stage;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class PrinterImplTest extends JavaFXConfiguredTest
{

    public static class AsNonApp extends Application
    {

        @Override
        public void start(Stage primaryStage) throws Exception
        {
            // noop
        }
    }

    @BeforeClass
    public static void initJFX()
    {
        Thread t = new Thread("JavaFX Init Thread")
        {
            public void run()
            {
                Application.launch(AsNonApp.class, new String[0]);
            }
        };
        t.setDaemon(true);
        t.start();
    }

    // This test must be run manually only as it needs a GUI action to be performed
//    @Test
    public void testFormatHeadIsTriggeredForNOTPROGRAMMEDHead()
    {
        RoboxCommsManager roboxCommsManager = RoboxCommsManager.getInstance("/path/to/bin");
        PrinterImpl printer = new TestPrinterImpl("ttyACM0", roboxCommsManager);

        StatusResponse statusResponse = new TestStatusResponse();

        assertFalse(((TestPrinterImpl) printer).isHeadFormatted());
        RoboxEvent roboxEvent = new RoboxEvent(RoboxEventType.PRINTER_STATUS_UPDATE, statusResponse);
        printer.processRoboxEvent(roboxEvent);
        printer.processRoboxEvent(roboxEvent);
        assertTrue(((TestPrinterImpl) printer).isHeadFormatted());
        
    }

}
