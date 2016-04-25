package celtech.printerControl.comms;

import java.util.List;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

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

    public void shutdownDetector();

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

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder(13, 37).
                    append(connectionType).
                    append(connectionHandle).
                    toHashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof DetectedPrinter))
            {
                return false;
            }
            if (obj == this)
            {
                return true;
            }

            DetectedPrinter rhs = (DetectedPrinter) obj;
            return new EqualsBuilder().
                    append(connectionType, rhs.getConnectionType()).
                    append(connectionHandle, rhs.getConnectionHandle()).
                    isEquals();
        }
    }
}
