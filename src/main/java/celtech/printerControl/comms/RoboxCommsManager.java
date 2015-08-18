package celtech.printerControl.comms;

import celtech.Lookup;
import celtech.configuration.UserPreferences;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.model.HardwarePrinter;
import celtech.printerControl.model.Printer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.application.Platform;
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
    private final HashMap<String, Printer> pendingPrinters = new HashMap<>();
    private final HashMap<String, Printer> activePrinters = new HashMap<>();
    private boolean suppressPrinterIDChecks = false;
    private int sleepBetweenStatusChecksMS = 1000;

    private String dummyPrinterPort = "DummyPrinterPort";

    private int dummyPrinterCounter = 0;

    private final DeviceDetector deviceDetector;

    private boolean doNotCheckForPresenceOfHead = false;

    private RoboxCommsManager(String pathToBinaries,
            boolean suppressPrinterIDChecks,
            boolean doNotCheckForPresenceOfHead)
    {
        this.suppressPrinterIDChecks = suppressPrinterIDChecks;
        this.doNotCheckForPresenceOfHead = doNotCheckForPresenceOfHead;

        deviceDetector = new DeviceDetector(pathToBinaries, roboxVendorID, roboxProductID, printerToSearchFor);

        this.setName("Robox Comms Manager");
        steno = StenographerFactory.getStenographer(this.getClass().getName());
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
            instance = new RoboxCommsManager(pathToBinaries, false, false);
        }

        return instance;
    }

    /**
     *
     * @param pathToBinaries
     * @param doNotCheckForHeadPresence
     * @return
     */
    public static RoboxCommsManager getInstance(String pathToBinaries, boolean doNotCheckForHeadPresence)
    {
        if (instance == null)
        {
            instance = new RoboxCommsManager(pathToBinaries, false, doNotCheckForHeadPresence);
        }

        return instance;
    }

    /**
     *
     */
    @Override
    public void run()
    {
        while (keepRunning)
        {
//            steno.info("Looking for printers");
            String[] activePorts = deviceDetector.searchForDevice();

            if (activePorts != null)
            {
                String port = activePorts[0];
                //Multiple printer support disabled - ROB-334
//                for (String port : activePorts)
                {
//                    steno.info("Found printer on " + port);
                    if (pendingPrinters.containsKey(port))
                    {
                        //A connection to this printer is pending...
//                        System.out.println("PENDING FOUND");
                    } else if (activePrinters.containsKey(port))
                    {
                        //We're already connected to this printer
//                        System.out.println("ACTIVE FOUND");
                    } else
                    {
                        // We need to connect!
                        if(keepRunning)
                        {
                        steno.info("Adding new printer on " + port);

                        Printer newPrinter = makeHardwarePrinter(port);
                        pendingPrinters.put(port, newPrinter);
                        }
                        else
                        {
                            steno.info("Aborted add of printer as we are shutting down");
                        }
                    }
                }
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

    private Printer makeHardwarePrinter(String port)
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
        Printer newPrinter = new HardwarePrinter(this, new HardwareCommandInterface(
                this, port, suppressPrinterIDChecks,
                sleepBetweenStatusChecksMS), filamentLoadedGetter,
                doNotCheckForPresenceOfHead);
        return newPrinter;
    }

    /**
     *
     */
    public void shutdown()
    {
        keepRunning = false;

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
     * @param portName
     */
    @Override
    public void printerConnected(String portName)
    {
        Printer printer = pendingPrinters.get(portName);
        activePrinters.put(portName, printer);
        printer.connectionEstablished();

        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            Lookup.getConnectedPrinters().add(printer);
        });
    }

    /**
     *
     * @param portName
     */
    @Override
    public void failedToConnect(String portName)
    {
        pendingPrinters.remove(portName);
    }

    /**
     *
     * @param portName
     */
    @Override
    public void disconnected(String portName)
    {
        pendingPrinters.remove(portName);

        final Printer printerToRemove = activePrinters.get(portName);
        if (printerToRemove != null)
        {
            printerToRemove.shutdown(false);
        }
        activePrinters.remove(portName);

        Platform.runLater(() ->
        {
            Lookup.getConnectedPrinters().remove(printerToRemove);
        });
    }

    public void addDummyPrinter()
    {
        dummyPrinterCounter++;
        String actualPrinterPort = dummyPrinterPort + " " + dummyPrinterCounter;
        Printer nullPrinter = new HardwarePrinter(this,
                new DummyPrinterCommandInterface(this,
                        actualPrinterPort,
                        suppressPrinterIDChecks,
                        sleepBetweenStatusChecksMS,
                        "DP "
                        + dummyPrinterCounter));
        pendingPrinters.put(actualPrinterPort, nullPrinter);
        dummyPrinters.add(nullPrinter);
    }

    public void removeDummyPrinter(String portName)
    {
        disconnected(portName);
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
