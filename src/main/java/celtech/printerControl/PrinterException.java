package celtech.printerControl;

/**
 *
 * @author Ian
 */
class PrinterException extends Exception
{
    private String userFriendlyMessage = "";
    
    public PrinterException(String loggingMessage, String userFriendlyMessage)
    {
        super(loggingMessage);
        this.userFriendlyMessage = userFriendlyMessage;
    }
    
    public String getUserFriendlyMessage()
    {
        return userFriendlyMessage;
    }
}
