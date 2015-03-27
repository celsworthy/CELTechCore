package celtech.printerControl.comms;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.MachineType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class DeviceDetector
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
        DeviceDetector.class.getName());
    private final String pathToBinaries;
    private final String deviceDetectorStringMac;
    private final String deviceDetectorStringWindows;
    private final String deviceDetectorStringLinux;
    private final String deviceDetectionCommand;
    private final String notConnectedString = "NOT_CONNECTED";

    public DeviceDetector(String pathToBinaries, String vendorID, String productID,
        String deviceNameToSearchFor)
    {
        this.pathToBinaries = pathToBinaries;

        deviceDetectorStringMac = pathToBinaries + "RoboxDetector.mac.sh";
        deviceDetectorStringLinux = pathToBinaries + "RoboxDetector.linux.sh";
        deviceDetectorStringWindows = pathToBinaries + "RoboxDetector.exe";

        MachineType machineType = ApplicationConfiguration.getMachineType();

        switch (machineType)
        {
            case WINDOWS:
                deviceDetectionCommand = deviceDetectorStringWindows + " " + vendorID + " "
                    + productID;
                break;
            case MAC:
                deviceDetectionCommand = deviceDetectorStringMac + " " + deviceNameToSearchFor;
                break;
            case LINUX_X86:
            case LINUX_X64:
                deviceDetectionCommand = deviceDetectorStringLinux + " " + deviceNameToSearchFor
                    + " "
                    + vendorID;
                break;
            default:
                deviceDetectionCommand = null;
                steno.error("Unsupported OS - cannot establish comms.");
                break;
        }
        steno.trace("Device detector command: " + deviceDetectionCommand);
    }

    /**
     * Detect any attached device and return an array of port names
     *
     * @return an array of com or dev (e.g. /dev/ttyACM0) names
     */
    public String[] searchForDevice()
    {
        StringBuilder outputBuffer = new StringBuilder();
        List<String> command = new ArrayList<String>();
        for (String subcommand : deviceDetectionCommand.split(" "))
        {
            command.add(subcommand);
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        Map<String, String> environ = builder.environment();

        Process process = null;

        try
        {
            process = builder.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null)
            {
                if (line.equalsIgnoreCase(notConnectedString) == false)
                {
                    outputBuffer.append(line);
                }
            }
        } catch (IOException ex)
        {
            steno.error("Error " + ex);
        }

        String[] ports = null;

        if (outputBuffer.length() > 0)
        {
            ports = outputBuffer.toString().split(" ");
        }

        return ports;
    }
}
