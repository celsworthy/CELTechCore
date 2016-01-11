package celtech.printerControl.comms;

import celtech.Lookup;
import celtech.comms.RemoteDeviceDetector;
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
public class RoboxCommsManager implements PrinterStatusConsumer, DeviceDetectionListener
{

    private static RoboxCommsManager instance = null;

    private final String printerToSearchFor = "Robox";
    private final String roboxVendorID = "16D0";
    private final String roboxProductID = "081B";

    private Stenographer steno = null;
    private final List<Printer> dummyPrinters = new ArrayList<>();
    private final HashMap<DetectedDevice, Printer> pendingPrinters = new HashMap<>();
    private final HashMap<DetectedDevice, Printer> activePrinters = new HashMap<>();
    private boolean suppressPrinterIDChecks = false;
    private int sleepBetweenStatusChecksMS = 1000;

    private String dummyPrinterPort = "DummyPrinterPort";

    private int dummyPrinterCounter = 0;

    private final SerialDeviceDetector usbSerialDeviceDetector;
    private final RemoteDeviceDetector remoteDeviceDetector;

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

        usbSerialDeviceDetector = new SerialDeviceDetector(pathToBinaries, roboxVendorID, roboxProductID, printerToSearchFor, this);
        remoteDeviceDetector = new RemoteDeviceDetector(this);

        steno = StenographerFactory.getStenographer(this.getClass().getName());

        if (searchForRemotePrinters)
        {
            remoteDeviceDetector.start();
        }
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

    private void assessCandidatePrinter(DetectedDevice detectedPrinter)
    {
        if (detectedPrinter != null)
        {
            boolean noNeedToAddPrinter = false;

//                steno.info("Found printer on " + detectedPrinter);
            for (DetectedDevice pendingPrinterToCheck : pendingPrinters.keySet())
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
                for (DetectedDevice activePrinterToCheck : activePrinters.keySet())
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
                steno.info("Adding new printer " + detectedPrinter);

                Printer newPrinter = makePrinter(detectedPrinter);
                pendingPrinters.put(detectedPrinter, newPrinter);
                newPrinter.startComms();
            }
        }
    }

    private Printer makePrinter(DetectedDevice detectedPrinter)
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
                        this, (RemoteDetectedPrinter) detectedPrinter, suppressPrinterIDChecks,
                        sleepBetweenStatusChecksMS), filamentLoadedGetter,
                        doNotCheckForPresenceOfHead);
                break;
            default:
                steno.error("Don't know how to handle connected printer: " + detectedPrinter);
                break;
        }
        return newPrinter;
    }

    public void start()
    {
        if (!usbSerialDeviceDetector.isAlive())
        {
            usbSerialDeviceDetector.start();
        }

        if (!remoteDeviceDetector.isAlive() && searchForRemotePrinters)
        {
            remoteDeviceDetector.start();
        }
    }

    /**
     *
     */
    public void shutdown()
    {
        usbSerialDeviceDetector.shutdownDetector();
        remoteDeviceDetector.shutdownDetector();

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
    public void printerConnected(DetectedDevice detectedPrinter)
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
    public void failedToConnect(DetectedDevice printerHandle)
    {
        pendingPrinters.remove(printerHandle);
    }

    /**
     *
     */
    @Override
    public void disconnected(DetectedDevice printerHandle)
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
        DetectedDevice printerHandle = new DetectedDevice(DeviceDetector.PrinterConnectionType.SERIAL, actualPrinterPort);
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

    public void removeDummyPrinter(DetectedDevice printerHandle)
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

    @Override
    public void deviceDetected(DetectedDevice detectedDevice)
    {
        assessCandidatePrinter(detectedDevice);
    }

    @Override
    public void deviceNoLongerPresent(DetectedDevice detectedDevice)
    {
        steno.info("Robox Comms Manager has been told that a printer is no longer detected: " + detectedDevice);
    }
}
