package celtech.printerControl;

import celtech.printerControl.model.PrinterException;

/**
 *
 * @author Ian
 */
public class PrintJobRejectedException extends PrinterException
{

    public PrintJobRejectedException(String loggingMessage)
    {
        super(loggingMessage);
    }
    
}
