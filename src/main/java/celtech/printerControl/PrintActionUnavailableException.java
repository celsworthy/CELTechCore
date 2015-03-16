package celtech.printerControl;

import celtech.printerControl.model.PrinterException;

/**
 *
 * @author Ian
 */
public class PrintActionUnavailableException extends PrinterException
{
    public PrintActionUnavailableException(String loggingMessage)
    {
        super(loggingMessage);
    }
}
