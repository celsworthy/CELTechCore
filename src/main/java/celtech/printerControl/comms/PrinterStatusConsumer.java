package celtech.printerControl.comms;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public interface PrinterStatusConsumer
{
    /**
     *
     * @param printerHandle
     */
    public void printerConnected(DeviceDetector.DetectedPrinter printerHandle);

    /**
     *
     * @param printerHandle
     */
    public void failedToConnect(DeviceDetector.DetectedPrinter printerHandle);

    /**
     *
     * @param printerHandle
     */
    public void disconnected(DeviceDetector.DetectedPrinter printerHandle);  
}
