package celtech.services.printing;

import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;

/**
 *
 * @author Ian
 */
public class PrintHelper
{
     public void pausePrint()
    {
        switch (printState)
        {
            case SENDING_TO_PRINTER:
            case PRINTING:
            case EXECUTING_MACRO:
                lastStateBeforePause = printState;
                try
                {
                    associatedPrinter.transmitPausePrint();
                    setPrintStatus(PrinterStatus.PAUSED);
                } catch (RoboxCommsException ex)
                {
                    steno.error(
                        "Robox comms exception when sending pause print command "
                        + ex);
                }
                break;
            default:
                steno.warning("Attempt to pause print in print state "
                    + printState);
                break;
        }
    }
}
