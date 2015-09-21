package celtech.printerControl.comms;

import java.util.List;

/**
 *
 * @author Ian
 */
public interface DeviceDetector
{

    /**
     *
     * @return
     */
    public List<DetectedPrinter> searchForDevices();

    public enum PrinterConnectionType
    {

        SERIAL,
        ROBOX_REMOTE
    }

    public class DetectedPrinter
    {

        private final PrinterConnectionType connectionType;
        private final String connectionHandle;

        public DetectedPrinter(PrinterConnectionType connectionType,
                String connectionHandle)
        {
            this.connectionType = connectionType;
            this.connectionHandle = connectionHandle;
        }

        public PrinterConnectionType getConnectionType()
        {
            return connectionType;
        }

        public String getConnectionHandle()
        {
            return connectionHandle;
        }

        @Override
        public String toString()
        {
            return connectionType.name() + ":" + connectionHandle;
        }
    }
}
