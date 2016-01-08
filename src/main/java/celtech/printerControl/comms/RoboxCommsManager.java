package celtech.printerControl.comms;

import celtech.Lookup;
import celtech.comms.DiscoveryAgentClientEnd;
import celtech.configuration.UserPreferences;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.model.HardwarePrinter;
import celtech.printerControl.model.Printer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class RoboxCommsManager extends Thread implements PrinterStatusConsumer
{

    private static RoboxCommsManager instance = null;
    private boolean keepRunning = true;

    private final String printerToSearchFor = "Robox";
    private final String roboxVendorID = "16D0";
    private final String roboxProductID = "081B";

    private Stenographer steno = null;
    private final List<Printer> dummyPrinters = new ArrayList<>();
    private final HashMap<DeviceDetector.DetectedPrinter, Printer> pendingPrinters = new HashMap<>();
    private final HashMap<DeviceDetector.DetectedPrinter, Printer> activePrinters = new HashMap<>();
    private boolean suppressPrinterIDChecks = false;
    private int sleepBetweenStatusChecksMS = 1000;

    private String dummyPrinterPort = "DummyPrinterPort";

    private int dummyPrinterCounter = 0;

    private final SerialDeviceDetector usbSerialDeviceDetector;
    private Thread remoteHostDiscoveryThread;
    private final DiscoveryAgentClientEnd remoteHostDiscoveryClient = new DiscoveryAgentClientEnd(this);

    private boolean doNotCheckForPresenceOfHead = false;
    private boolean searchForRemotePrinters = false;

    private RoboxCommsManager(String pathToBinaries,
            boolean suppressPrinterIDChecks,
            boolean doNotCheckForPresenceOfHead,
            boolean searchForRemotePrinters)
    {
        this.suppressPrinterIDChecks = suppressPrinterIDChecks;
        this.doNotCheckForPresenceOfHead = doNotCheckForPresenceOfHead;
        this.searchForRemotePrinters = searchForRemotePrinters;

        usbSerialDeviceDetector = new SerialDeviceDetector(pathToBinaries, roboxVendorID, roboxProductID, printerToSearchFor);

        this.setName("Robox Comms Manager");
        steno = StenographerFactory.getStenographer(this.getClass().getName());
        this.setDaemon(true);
    }

    /**
     *
     * @return
     */
    public static RoboxCommsManager getInstance()
    {
        return instance;
    }

    /**
     *
     * @param pathToBinaries
     * @return
     */
    public static RoboxCommsManager getInstance(String pathToBinaries)
    {
        if (instance == null)
        {
            instance = new RoboxCommsManager(pathToBinaries, false, false, true);
        }

        return instance;
    }

    /**
     *
     * @param pathToBinaries
     * @param doNotCheckForHeadPresence
     * @param searchForRemotePrinters
     * @return
     */
    public static RoboxCommsManager getInstance(String pathToBinaries,
            boolean doNotCheckForHeadPresence,
            boolean searchForRemotePrinters)
    {
        if (instance == null)
        {
            instance = new RoboxCommsManager(pathToBinaries,
                    false,
                    doNotCheckForHeadPresence,
                    searchForRemotePrinters);
        }

        return instance;
    }

    /**
     *
     */
    @Override
    public void run()
    {
        if (searchForRemotePrinters)
        {
            remoteHostDiscoveryThread = new Thread(remoteHostDiscoveryClient);
            remoteHostDiscoveryThread.setDaemon(true);
            remoteHostDiscoveryThread.start();
        }

        while (keepRunning)
        {
            List<DeviceDetector.DetectedPrinter> serialPrinters = usbSerialDeviceDetector.searchForDevices();
            assessCandidatePrinters(serialPrinters);

            if (searchForRemotePrinters)
            {
                List<DeviceDetector.DetectedPrinter> remotePrinters = remoteHostDiscoveryClient.searchForDevices();
                assessCandidatePrinters(remotePrinters);
            }

            try
            {
                this.sleep(500);
            } catch (InterruptedException ex)
            {
                steno.error("Interrupted");
            }
        }
    }

    private void assessCandidatePrinters(List<DeviceDetector.DetectedPrinter> connnectionHandles)
    {
        if (connnectionHandles != null)
        {
            boolean noNeedToAddPrinter = false;

            for (DeviceDetector.DetectedPrinter detectedPrinter : connnectionHandles)
            {
//                steno.info("Found printer on " + detectedPrinter);

                for (DeviceDetector.DetectedPrinter pendingPrinterToCheck : pendingPrinters.keySet())
                {
                    if (detectedPrinter.equals(pendingPrinterToCheck))
                    {
//                        steno.info("Found already pending printer " + detectedPrinter);
                        noNeedToAddPrinter = true;
                        break;
                    }
                }

                if (!noNeedToAddPrinter)
                {
                    for (DeviceDetector.DetectedPrinter activePrinterToCheck : activePrinters.keySet())
                    {
                        if (detectedPrinter.equals(activePrinterToCheck))
                        {
//                            steno.info("Found already active printer " + detectedPrinter);
                            noNeedToAddPrinter = true;
                            break;
                        }
                    }
                }

                if (!noNeedToAddPrinter)
                {
                    // We need to connect!
                    if (keepRunning)
                    {
                        steno.info("Adding new printer " + detectedPrinter);

                        Printer newPrinter = makePrinter(detectedPrinter);
                        pendingPrinters.put(detectedPrinter, newPrinter);
                        newPrinter.startComms();
                    } else
                    {
                        steno.info("Aborted add of printer as we are shutting down");
                    }
                }
            }
        }
    }

    private Printer makePrinter(DeviceDetector.DetectedPrinter detectedPrinter)
    {
        final UserPreferences userPreferences = Lookup.getUserPreferences();
        HardwarePrinter.FilamentLoadedGetter filamentLoadedGetter
                = (StatusResponse statusResponse, int extruderNumber) ->
                {
                    if (!userPreferences.getDetectLoadedFilament())
                    {
                        // if this preference has been deselected then always say that the filament
                        // has been detected as loaded.
                        return true;
                    } else
                    {
                        if (extruderNumber == 1)
                        {
                            return statusResponse.isFilament1SwitchStatus();
                        } else
                        {
                            return statusResponse.isFilament2SwitchStatus();
                        }
                    }
                };
        Printer newPrinter = null;

        switch (detectedPrinter.getConnectionType())
        {
            case SERIAL:
                newPrinter = new HardwarePrinter(this, new HardwareCommandInterface(
                        this, detectedPrinter, suppressPrinterIDChecks,
                        sleepBetweenStatusChecksMS), filamentLoadedGetter,
                        doNotCheckForPresenceOfHead);
                break;
            case ROBOX_REMOTE:
                newPrinter = new HardwarePrinter(this, new RoboxRemoteCommandInterface(
                        this, (DeviceDetector.RemoteDetectedPrinter)detectedPrinter, suppressPrinterIDChecks,
                        sleepBetweenStatusChecksMS), filamentLoadedGetter,
                        doNotCheckForPresenceOfHead);
                break;
            default:
                steno.error("Don't know how to handle connected printer: " + detectedPrinter);
                break;
        }
        return newPrinter;
    }

    /**
     *
     */
    public void shutdown()
    {
        keepRunning = false;
        remoteHostDiscoveryClient.shutdown();

        for (Printer printer : Lookup.getConnectedPrinters())
        {
            steno.info("Shutdown printer " + printer);
            try
            {
                printer.shutdown(true);
            } catch (Exception ex)
            {
                steno.error("Error shutting down printer");
            }
        }
    }

    /**
     *
     * @param detectedPrinter
     */
    @Override
    public void printerConnected(DeviceDetector.DetectedPrinter detectedPrinter)
    {
        Printer printer = pendingPrinters.get(detectedPrinter);
        activePrinters.put(detectedPrinter, printer);
        printer.connectionEstablished();

        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            Lookup.getConnectedPrinters().add(printer);
        });
    }

    /**
     *
     */
    @Override
    public void failedToConnect(DeviceDetector.DetectedPrinter printerHandle)
    {
        pendingPrinters.remove(printerHandle);
    }

    /**
     *
     */
    @Override
    public void disconnected(DeviceDetector.DetectedPrinter printerHandle)
    {
        pendingPrinters.remove(printerHandle);

        final Printer printerToRemove = activePrinters.get(printerHandle);
        if (printerToRemove != null)
        {
            printerToRemove.shutdown(false);
        }
        activePrinters.remove(printerHandle);

        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            Lookup.getConnectedPrinters().remove(printerToRemove);
        });
    }

    public void addDummyPrinter()
    {
        dummyPrinterCounter++;
        String actualPrinterPort = dummyPrinterPort + " " + dummyPrinterCounter;
        DeviceDetector.DetectedPrinter printerHandle = new DeviceDetector.DetectedPrinter(DeviceDetector.PrinterConnectionType.SERIAL, actualPrinterPort);
        Printer nullPrinter = new HardwarePrinter(this,
                new DummyPrinterCommandInterface(this,
                        printerHandle,
                        suppressPrinterIDChecks,
                        sleepBetweenStatusChecksMS,
                        "DP "
                        + dummyPrinterCounter));
        pendingPrinters.put(printerHandle, nullPrinter);
        dummyPrinters.add(nullPrinter);
        nullPrinter.startComms();
    }

    public void removeDummyPrinter(DeviceDetector.DetectedPrinter printerHandle)
    {
        disconnected(printerHandle);
    }

    public List<Printer> getDummyPrinters()
    {
        return dummyPrinters;
    }

    /**
     *
     * @param milliseconds
     */
    public void setSleepBetweenStatusChecks(int milliseconds)
    {
        sleepBetweenStatusChecksMS = milliseconds;
    }
}
