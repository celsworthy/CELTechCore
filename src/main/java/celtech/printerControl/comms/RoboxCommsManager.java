package celtech.printerControl.comms;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.MachineType;
import celtech.printerControl.model.HardwarePrinter;
import celtech.printerControl.model.Printer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private String roboxDetectorMac = null;
    private String roboxDetectorLinux = null;
    private String roboxDetectorWindows = null;
    private String roboxDetectorCommand = null;
    private final String printerToSearchFor = "Robox";

    private final String roboxVendorID = "16D0";
    private final String roboxProductID = "081B";

    private final String notConnectedString = "NOT_CONNECTED";
    private Stenographer steno = null;
    private final List<Printer> dummyPrinters = new ArrayList<>();
    private final HashMap<String, Printer> pendingPrinters = new HashMap<>();
    private final HashMap<String, Printer> activePrinters = new HashMap<>();
    private boolean suppressPrinterIDChecks = false;
    private int sleepBetweenStatusChecks = 1000;

    private String dummyPrinterPort = "DummyPrinterPort";

    private int dummyPrinterCounter = 0;

    private RoboxCommsManager(String pathToBinaries, boolean suppressPrinterIDChecks)
    {
        this.suppressPrinterIDChecks = suppressPrinterIDChecks;

        roboxDetectorMac = pathToBinaries + "RoboxDetector.mac.sh";
        roboxDetectorLinux = pathToBinaries + "RoboxDetector.linux.sh";
        roboxDetectorWindows = pathToBinaries + "RoboxDetector.exe";

        this.setName("Robox Comms Manager");
        steno = StenographerFactory.getStenographer(this.getClass().getName());

        MachineType machineType = ApplicationConfiguration.getMachineType();

        switch (machineType)
        {
            case WINDOWS:
                roboxDetectorCommand = roboxDetectorWindows + " " + roboxVendorID + " "
                    + roboxProductID;
                break;
            case MAC:
                roboxDetectorCommand = roboxDetectorMac + " " + printerToSearchFor;
                break;
            case LINUX_X86:
            case LINUX_X64:
                roboxDetectorCommand = roboxDetectorLinux + " " + printerToSearchFor + " "
                    + roboxVendorID;
                break;
            default:
                steno.error("Unsupported OS - cannot establish comms.");
                keepRunning = false;
                break;
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
            instance = new RoboxCommsManager(pathToBinaries, false);
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
            String[] activePorts = searchForPrinter();

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
                        steno.info("Adding new printer on " + port);

                        Printer newPrinter = new HardwarePrinter(this, new HardwareCommandInterface(
                                                                 this, port, suppressPrinterIDChecks,
                                                                 sleepBetweenStatusChecks));
                        pendingPrinters.put(port, newPrinter);
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

    /**
     *
     */
    public void shutdown()
    {
        for (Printer printer : Lookup.getConnectedPrinters())
        {
            steno.info("Shutdown printer " + printer);
            try
            {
                printer.shutdown();
            } catch (Exception ex)
            {
                steno.error("Error shutting down printer");
            }
        }

        keepRunning = false;
    }

    /**
     * Detect any attached Robox printers and return an array of port names
     *
     * @return an array of com or dev (e.g. /dev/ttyACM0) names
     */
    private String[] searchForPrinter()
    {
        StringBuilder outputBuffer = new StringBuilder();
        List<String> command = new ArrayList<String>();
        for (String subcommand : roboxDetectorCommand.split(" "))
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
        activePrinters.remove(portName);

        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                Lookup.getConnectedPrinters().remove(printerToRemove);
            }
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
                                                                                   sleepBetweenStatusChecks,
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
}
