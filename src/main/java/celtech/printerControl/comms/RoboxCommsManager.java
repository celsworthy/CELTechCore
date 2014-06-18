/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.MachineType;
import celtech.printerControl.PrinterImpl;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.rx.RoboxRxPacketFactory;
import celtech.printerControl.comms.commands.rx.RxPacketTypeEnum;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
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
 * @author Ian Hudson @ Liberty Systems Limited
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

    private final String roboxVendorID = "16D0";
    private final String roboxProductID = "081B";

    private final String notConnectedString = "NOT_CONNECTED";
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private DataInputStream dataInputStream = null;
    private Stenographer steno = null;
    private RoboxEventProducer eventProducer = new RoboxEventProducer();
    private final HashMap<String, PrinterHandler> pendingPrinterConnections = new HashMap<>();
    private final HashMap<String, PrinterImpl> pendingPrinters = new HashMap<>();
    private final HashMap<String, PrinterHandler> activePrinterConnections = new HashMap<>();
    private final HashMap<String, PrinterImpl> activePrinterStatuses = new HashMap<>();
    private final ObservableList<PrinterImpl> printerStatus = FXCollections.observableArrayList();
    private boolean suppressPrinterIDChecks = false;
    private int sleepBetweenStatusChecks = 1000;

    private final String nullPrinterString = "NullPrinter";
    private PrinterImpl nullPrinter = null;

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
                roboxDetectorCommand = roboxDetectorWindows + " " + roboxVendorID + " " + roboxProductID;
                break;
            case MAC:
                roboxDetectorCommand = roboxDetectorMac + " " + printerToSearchFor;
                break;
            case LINUX_X86:
            case LINUX_X64:
                roboxDetectorCommand = roboxDetectorLinux + " " + printerToSearchFor + " " + roboxVendorID;
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
     * @param pathToBinaries
     * @param suppressPrinterIDCheck
     * @return
     */
    public static RoboxCommsManager getInstance(String pathToBinaries, boolean suppressPrinterIDCheck)
    {
        if (instance == null)
        {
            instance = new RoboxCommsManager(pathToBinaries, true);
        }

        return instance;
    }

    /**
     *
     */
    @Override
    public void run()
    {
//        enableNullPrinter(true);

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
                        PrinterImpl newPrinter = new PrinterImpl(port, this);
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

    /**
     *
     */
    public void shutdown()
    {
        for (PrinterImpl printer : printerStatus)
        {

            switch (printer.getPrinterStatus())
            {
                case IDLE:
                case PAUSED:
                case PRINTING:
                    break;
                case SENDING_TO_PRINTER:
                case POST_PROCESSING:
                case SLICING:
                case ERROR:
                    printer.getPrintQueue().abortPrint();
                    break;
            }
        }

        for (PrinterHandler printerHandler : activePrinterConnections.values())
        {
            printerHandler.shutdown();
        }
        for (PrinterHandler printerHandler : pendingPrinterConnections.values())
        {
            printerHandler.shutdown();
        }
        keepRunning = false;
    }

    /**
     * Detect any attached Robox printers and return an array of port names
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
     * @param printerName
     * @param gcodePacket
     * @return
     * @throws RoboxCommsException
     */
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
            if (printerName.equals(nullPrinterString))
            {
                response = RoboxRxPacketFactory.createPacket(gcodePacket.getPacketType().getExpectedResponse());
            } else
            {
                steno.error("Rejected request to send packet of type " + gcodePacket.getPacketType().name());
            }
        }

        return response;
    }

    /**
     *
     * @param portName
     * @param event
     */
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
                        PrinterImpl printerStat = activePrinterStatuses.get(portName);
                        activePrinterStatuses.remove(portName);
                        printerStatus.remove(printerStat);
//                        printerStat.setPrinterConnected(false);
                    }
                });
                break;

            default:
                PrinterImpl printerStat = activePrinterStatuses.get(portName);
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
                    PrinterImpl pendingPrinterStat = pendingPrinters.get(portName);
                    if (pendingPrinterStat != null)
                    {
                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                pendingPrinterStat.processRoboxEvent(event);
                            }
                        });
                    }
                    steno.warning("Update " + event.getEventType() + " received for printer on port " + portName + " but printer is not in active list");
                }
                break;
        }
    }

    /**
     *
     * @param portName
     */
    @Override
    public void printerConnected(String portName)
    {
        PrinterHandler handler = pendingPrinterConnections.get(portName);
        pendingPrinterConnections.remove(portName);

        activePrinterConnections.put(portName, handler);

        PrinterImpl printer = pendingPrinters.get(portName);
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

    /**
     *
     * @param portName
     */
    @Override
    public void failedToConnect(String portName)
    {
        pendingPrinterConnections.remove(portName);
        pendingPrinters.remove(portName);
    }

    /**
     *
     * @param portName
     */
    @Override
    public void disconnected(String portName)
    {
        publishEvent(portName, new RoboxEvent(RoboxEventType.PRINTER_OFFLINE));
        pendingPrinterConnections.remove(portName);
        pendingPrinters.remove(portName);
        activePrinterConnections.remove(portName);
    }

    /**
     *
     * @return
     */
    public ObservableList<PrinterImpl> getPrintStatusList()
    {
        return printerStatus;
    }

    /**
     *
     * @param sleepValue
     */
    public void setSleepBetweenStatusChecks(int sleepValue)
    {
        sleepBetweenStatusChecks = sleepValue;
    }

    /**
     *
     * @param connectedPrinter
     * @param sleepMillis
     */
    public void setSleepBetweenStatusChecks(PrinterImpl connectedPrinter, int sleepMillis)
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

    /**
     *
     * @param enable
     */
    public void enableNullPrinter(boolean enable)
    {

        if (nullPrinter == null)
        {
            nullPrinter = new PrinterImpl(nullPrinterString, this);
        }

        if (enable)
        {
            if (activePrinterStatuses.containsKey(nullPrinterString) == false)
            {
                activePrinterStatuses.put(nullPrinterString, nullPrinter);
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        printerStatus.add(nullPrinter);
                        nullPrinter.setPrinterConnected(true);
                    }
                });
            }
        } else
        {
            activePrinterStatuses.remove(nullPrinterString);
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    printerStatus.remove(nullPrinter);
                }
            });
        }
    }
}
