package celtech.printerControl.comms;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public interface PrinterStatusConsumer
{
    /**
     *
     * @param portName
     */
    public void printerConnected(String portName);

    /**
     *
     * @param portName
     */
    public void failedToConnect(String portName);

    /**
     *
     * @param portName
     */
    public void disconnected(String portName);  
}
