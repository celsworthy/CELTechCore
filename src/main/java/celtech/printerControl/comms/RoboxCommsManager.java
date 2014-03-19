/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms;

import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.events.RoboxEvent;
import celtech.printerControl.comms.events.RoboxEventProducer;
import celtech.printerControl.comms.events.RoboxEventType;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson
 * @ Liberty Systems Limited
 */
public class RoboxCommsManager extends Thread implements PrinterControlInterface
{

    private static RoboxCommsManager instance = null;
    private boolean keepRunning = true;
    private String roboxDetectorMac = null;
    private String roboxDetectorLinux = null;
    private String roboxDetectorWindows = null;
    private String roboxDetectorCommand = null;
    private final String printerToSearchFor = "Robox";

    private final String roboxVendorID = "16d0";
    private final String roboxProductID = "081b";

    private final String notConnectedString = "NOT_CONNECTED";
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private DataInputStream dataInputStream = null;
    private Stenographer steno = null;
    private RoboxEventProducer eventProducer = new RoboxEventProducer();
    private final HashMap<String, PrinterHandler> pendingPrinterConnections = new HashMap<>();
    private final HashMap<String, Printer> pendingPrinters = new HashMap<>();
    private final HashMap<String, PrinterHandler> activePrinterConnections = new HashMap<>();
    private final HashMap<String, Printer> activePrinterStatuses = new HashMap<>();
    private final ObservableList<Printer> printerStatus = FXCollections.observableArrayList();
    private boolean suppressPrinterIDChecks = false;
    private int sleepBetweenStatusChecks = 1000;

    private RoboxCommsManager(String installDirectory, boolean suppressPrinterIDChecks)
    {
        this.suppressPrinterIDChecks = suppressPrinterIDChecks;

        roboxDetectorMac = installDirectory + "bin/RoboxDetector.mac.sh";
        roboxDetectorLinux = installDirectory + "bin/RoboxDetector.linux.sh";
        roboxDetectorWindows = installDirectory + "bin\\RoboxDetector.exe";

        this.setName("Robox Comms Manager");
        steno = StenographerFactory.getStenographer(this.getClass().getName());

        String osName = System.getProperty("os.name");

        if (osName.startsWith("Windows"))
        {
            roboxDetectorCommand = roboxDetectorWindows + " " + printerToSearchFor;
            steno.info("Got OS of " + osName + ". Using " + roboxDetectorCommand);

        } else if (osName.equalsIgnoreCase("Mac OS X"))
        {
            roboxDetectorCommand = roboxDetectorMac + " " + printerToSearchFor;
            steno.debug("Got OS of " + osName + ". Using " + roboxDetectorCommand);
        } else if (osName.equalsIgnoreCase("Linux"))
        {
            roboxDetectorCommand = roboxDetectorLinux + " " + roboxVendorID + ":" + roboxProductID;
            steno.debug("Got OS of " + osName + ". Using " + roboxDetectorCommand);
        } else
        {
            steno.error("Got OS of " + osName + ". Unsupported OS - cannot establish comms.");
            keepRunning = false;
        }
    }

    public static RoboxCommsManager getInstance()
    {
        return instance;
    }

    public static RoboxCommsManager getInstance(String installDirectory)
    {
        if (instance == null)
        {
            instance = new RoboxCommsManager(installDirectory, false);
        }

        return instance;
    }

    public static RoboxCommsManager getInstance(String installDirectory, boolean suppressPrinterIDCheck)
    {
        if (instance == null)
        {
            instance = new RoboxCommsManager(installDirectory, true);
        }

        return instance;
    }

    @Override
    public void run()
    {
        //Give everything else a chance to get going...
        // This should be replaced with an interlock from JavaFX startup
        try
        {
            sleep(2000);
        } catch (InterruptedException ex)
        {
            steno.error("Exception whilst waiting for startup");
        }

        while (keepRunning)
        {
//            steno.info("Looking for printers");
            String[] activePorts = searchForPrinter();

            if (activePorts != null)
            {
                for (String port : activePorts)
                {
//                    steno.info("Found printer on " + port);
                    if (pendingPrinterConnections.containsKey(port))
                    {
                        //A connection to this printer is pending...
                    } else if (activePrinterConnections.containsKey(port))
                    {
                        //We're already connected to this printer
                    } else
                    {
                        // We need to connect!
                        steno.info("Adding new printer on " + port);
                        PrinterHandler newPrinterHandler = new PrinterHandler(this, port, suppressPrinterIDChecks, sleepBetweenStatusChecks);
                        pendingPrinterConnections.put(port, newPrinterHandler);
                        Printer newPrinter = new Printer(port, this);
                        pendingPrinters.put(port, newPrinter);
                        newPrinterHandler.setPrinterToUse(newPrinter);

                        newPrinterHandler.start();
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

    public void shutdown()
    {

        for (PrinterHandler printerHandler : activePrinterConnections.values())
        {
            printerHandler.shutdown();
        }
        keepRunning = false;
    }

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

    public RoboxRxPacket submitForWrite(String printerName, RoboxTxPacket gcodePacket) throws RoboxCommsException
    {
        RoboxRxPacket response = null;
        PrinterHandler handler = null;

        if (printerName != null)
        {
            handler = activePrinterConnections.get(printerName);
            if (handler == null)
            {
//                steno.warning("Had to fall back to pending list to send packet of type " + gcodePacket.getPacketType().name());
                handler = pendingPrinterConnections.get(printerName);
            }
        }

        if (handler != null)
        {
            response = (RoboxRxPacket) handler.writeToPrinter(gcodePacket);
        } else
        {
            steno.error("Rejected request to send packet of type " + gcodePacket.getPacketType().name());
        }

        return response;
    }

    @Override
    public void publishEvent(String portName, RoboxEvent event)
    {
        switch (event.getEventType())
        {
            case PRINTER_OFFLINE:
                activePrinterConnections.remove(portName);
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Printer printerStat = activePrinterStatuses.get(portName);
                        activePrinterStatuses.remove(portName);
                        printerStatus.remove(printerStat);
//                        printerStat.setPrinterConnected(false);
                    }
                });
                break;

            default:
                Printer printerStat = activePrinterStatuses.get(portName);
                if (printerStat != null)
                {
                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            printerStat.processRoboxEvent(event);
                        }
                    });
                } else
                {
                    steno.warning("Update " + event.getEventType() + " received for printer on port " + portName + " but printer is not in active list");
                }
                break;
        }
    }

    @Override
    public void printerConnected(String portName)
    {
        PrinterHandler handler = pendingPrinterConnections.get(portName);
        pendingPrinterConnections.remove(portName);

        activePrinterConnections.put(portName, handler);

        Printer printer = pendingPrinters.get(portName);
        activePrinterStatuses.put(portName, printer);
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                printerStatus.add(printer);
                printer.setPrinterConnected(true);
                try
                {
                    printer.transmitReadHeadEEPROM();
                    printer.transmitReadReelEEPROM();
                } catch (RoboxCommsException ex)
                {
                    steno.error("Couldn't request EEPROM data");
                }
            }
        });
    }

    @Override
    public void failedToConnect(String portName)
    {
        pendingPrinterConnections.remove(portName);
        pendingPrinters.remove(portName);
    }

    @Override
    public void disconnected(String portName)
    {
        publishEvent(portName, new RoboxEvent(RoboxEventType.PRINTER_OFFLINE));
        pendingPrinterConnections.remove(portName);
        pendingPrinters.remove(portName);
        activePrinterConnections.remove(portName);
    }

    public ObservableList<Printer> getPrintStatusList()
    {
        return printerStatus;
    }

    public void setSleepBetweenStatusChecks(int sleepValue)
    {
        sleepBetweenStatusChecks = sleepValue;
    }

    public void setSleepBetweenStatusChecks(Printer connectedPrinter, int sleepMillis)
    {
        if (connectedPrinter != null)
        {
            String port = connectedPrinter.getPrinterPort();
            if (port != null)
            {
                PrinterHandler handler = activePrinterConnections.get(connectedPrinter.getPrinterPort());

                if (handler != null)
                {
                    handler.setSleepBetweenStatusChecks(sleepMillis);
                }
            }
        }
    }
}
