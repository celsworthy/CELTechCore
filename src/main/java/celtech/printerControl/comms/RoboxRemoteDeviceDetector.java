package celtech.printerControl.comms;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Ian
 */
public class RoboxRemoteDeviceDetector implements DeviceDetector
{
private final int discoveryPort;

    public RoboxRemoteDeviceDetector(final int discoveryPort)
    {
        this.discoveryPort = discoveryPort;
    }
    
    /**
     *
     * @return
     */
    @Override
    public List<DetectedPrinter> searchForDevices()
    {
        return new ArrayList<DetectedPrinter>();
    }    
}
