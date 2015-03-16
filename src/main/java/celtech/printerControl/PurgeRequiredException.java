package celtech.printerControl;

import celtech.printerControl.model.PrinterException;

/**
 *
 * @author Ian
 */
public class PurgeRequiredException extends PrinterException
{
    public PurgeRequiredException(String loggingMessage)
    {
        super(loggingMessage);
    }
}
