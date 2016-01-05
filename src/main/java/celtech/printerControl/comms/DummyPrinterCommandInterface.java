package celtech.printerControl.comms;

import celtech.Lookup;
import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.HeaterMode;
import celtech.configuration.PauseStatus;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.comms.commands.rx.GCodeDataResponse;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.PrinterIDResponse;
import celtech.printerControl.comms.commands.rx.ReelEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.rx.RoboxRxPacketFactory;
import celtech.printerControl.comms.commands.rx.RxPacketTypeEnum;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.comms.commands.tx.AbortPrint;
import celtech.printerControl.comms.commands.tx.FormatHeadEEPROM;
import celtech.printerControl.comms.commands.tx.InitiatePrint;
import celtech.printerControl.comms.commands.tx.PausePrint;
import celtech.printerControl.comms.commands.tx.QueryFirmwareVersion;
import celtech.printerControl.comms.commands.tx.ReadHeadEEPROM;
import celtech.printerControl.comms.commands.tx.ReadPrinterID;
import celtech.printerControl.comms.commands.tx.ReadReel0EEPROM;
import celtech.printerControl.comms.commands.tx.ReadReel1EEPROM;
import celtech.printerControl.comms.commands.tx.ReportErrors;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.SendDataFileChunk;
import celtech.printerControl.comms.commands.tx.SendDataFileEnd;
import celtech.printerControl.comms.commands.tx.SendDataFileStart;
import celtech.printerControl.comms.commands.tx.SendGCodeRequest;
import celtech.printerControl.comms.commands.tx.SendPrintFileStart;
import celtech.printerControl.comms.commands.tx.SendResetErrors;
import celtech.printerControl.comms.commands.tx.StatusRequest;
import celtech.printerControl.comms.commands.tx.WriteHeadEEPROM;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Reel;
import javafx.scene.paint.Color;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class DummyPrinterCommandInterface extends CommandInterface
{

    private Stenographer steno = StenographerFactory.getStenographer(
            DummyPrinterCommandInterface.class.getName());

    public static final String defaultRoboxAttachCommand = "DEFAULT";
    public static final String defaultRoboxAttachCommand2 = "DEFAULS";
    public static final String defaultRoboxAttachCommand3 = "DEFAULT3";
    private final String attachHeadCommand = "ATTACH HEAD ";
    private final String detachHeadCommand = "DETACH HEAD";
    private final String attachReelCommand = "ATTACH REEL ";
    private final String detachReelCommand = "DETACH REEL ";
    private final String detachPrinterCommand = "DETACH PRINTER";
    private final String goToPrintLineCommand = "GOTO LINE ";
    private final String finishPrintCommand = "FINISH PRINT";
    private final String attachExtruderCommand = "ATTACH EXTRUDER ";
    private final String detachExtruderCommand = "DETACH EXTRUDER ";
    private final String loadFilamentCommand = "LOAD ";
    private final String unloadFilamentCommand = "UNLOAD ";
    private final String insertSDCardCommand = "INSERT SD";
    private final String removeSDCardCommand = "REMOVE SD";
    private final String errorCommand = "ERROR ";

    private final StatusResponse currentStatus = (StatusResponse) RoboxRxPacketFactory.createPacket(
            RxPacketTypeEnum.STATUS_RESPONSE);
    private final AckResponse errorStatus = (AckResponse) RoboxRxPacketFactory.createPacket(
            RxPacketTypeEnum.ACK_WITH_ERRORS);
    private Head attachedHead = null;
    private final Reel[] attachedReels = new Reel[2];
    private String printerName;

    private static String NOTHING_PRINTING_JOB_ID = "\0000";
    private String printJobID = NOTHING_PRINTING_JOB_ID;
    protected int printJobLineNo = 0;
    private int linesInCurrentPrintJob = 0;

    private static int ROOM_TEMPERATURE = 20;
    HeaterMode nozzleHeaterModeS = HeaterMode.OFF;
    HeaterMode nozzleHeaterModeT = HeaterMode.OFF;
    protected int currentNozzleTemperatureS = ROOM_TEMPERATURE;
    protected int currentNozzleTemperatureT = ROOM_TEMPERATURE;
    protected int nozzleTargetTemperatureS = 210;
    protected int nozzleTargetTemperatureT = 210;
    HeaterMode bedHeaterMode = HeaterMode.OFF;
    protected int currentBedTemperature = ROOM_TEMPERATURE;
    protected int bedTargetTemperature = 30;

    public DummyPrinterCommandInterface(PrinterStatusConsumer controlInterface,
            DeviceDetector.DetectedPrinter printerHandle,
            boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks, String printerName)
    {
        super(controlInterface, printerHandle, suppressPrinterIDChecks, sleepBetweenStatusChecks);
        this.setName(printerName);
        this.printerName = printerName;

        currentStatus.setSdCardPresent(true);
    }

    public DummyPrinterCommandInterface(PrinterStatusConsumer controlInterface,
            DeviceDetector.DetectedPrinter printerHandle,
            boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks)
    {
        this(controlInterface, printerHandle, suppressPrinterIDChecks, sleepBetweenStatusChecks,
                "Dummy Printer");
    }

    @Override
    protected void setSleepBetweenStatusChecks(int sleepMillis)
    {
    }

    private void handleNozzleTempChange()
    {
        if (nozzleHeaterModeS != HeaterMode.OFF && currentNozzleTemperatureS
                < nozzleTargetTemperatureS)
        {
            currentNozzleTemperatureS += 10;
            if (currentNozzleTemperatureS > nozzleTargetTemperatureS)
            {
                currentNozzleTemperatureS = nozzleTargetTemperatureS;
            }
        } else if (nozzleHeaterModeS == HeaterMode.OFF && currentNozzleTemperatureS
                > ROOM_TEMPERATURE)
        {
            currentNozzleTemperatureS -= 10;
            if (currentNozzleTemperatureS < ROOM_TEMPERATURE)
            {
                currentNozzleTemperatureS = ROOM_TEMPERATURE;
            }
        }
        if (nozzleHeaterModeT != HeaterMode.OFF && currentNozzleTemperatureT
                < nozzleTargetTemperatureT)
        {
            currentNozzleTemperatureT += 7;
            if (currentNozzleTemperatureT > nozzleTargetTemperatureT)
            {
                currentNozzleTemperatureT = nozzleTargetTemperatureT;
            }
        } else if (nozzleHeaterModeT == HeaterMode.OFF && currentNozzleTemperatureT
                > ROOM_TEMPERATURE)
        {
            currentNozzleTemperatureT -= 7;
            if (currentNozzleTemperatureT < ROOM_TEMPERATURE)
            {
                currentNozzleTemperatureT = ROOM_TEMPERATURE;
            }
        }
        currentStatus.setNozzle0HeaterMode(nozzleHeaterModeS);
        currentStatus.setNozzle0Temperature(currentNozzleTemperatureS);
        currentStatus.setNozzle0TargetTemperature(nozzleTargetTemperatureS);
        currentStatus.setNozzle1HeaterMode(nozzleHeaterModeT);
        currentStatus.setNozzle1Temperature(currentNozzleTemperatureT);
        currentStatus.setNozzle1TargetTemperature(nozzleTargetTemperatureT);
    }

    private void handleBedTempChange()
    {
        if (bedHeaterMode != HeaterMode.OFF && currentBedTemperature < bedTargetTemperature)
        {
            currentBedTemperature += 5;
            if (currentBedTemperature > bedTargetTemperature)
            {
                currentBedTemperature = bedTargetTemperature;
            }
        } else if (bedHeaterMode == HeaterMode.OFF && currentBedTemperature > ROOM_TEMPERATURE)
        {
            currentBedTemperature -= 5;
            if (currentBedTemperature < ROOM_TEMPERATURE)
            {
                currentBedTemperature = ROOM_TEMPERATURE;
            }
        }
        currentStatus.setBedHeaterMode(bedHeaterMode);
        currentStatus.setBedTemperature(currentBedTemperature);
        currentStatus.setBedTargetTemperature(bedTargetTemperature);
    }

    private void detachReel(int reelNumber)
    {
        switch (reelNumber)
        {
            case 0:
                currentStatus.setReel0EEPROMState(EEPROMState.NOT_PRESENT);
                break;
            case 1:
                currentStatus.setReel1EEPROMState(EEPROMState.NOT_PRESENT);
                break;
        }
        attachedReels[reelNumber] = null;
    }

    private boolean attachReel(String filamentId, int reelNumber) throws NumberFormatException
    {
        boolean success = false;

        Filament filament = Lookup.getFilamentContainer().getFilamentByID(filamentId);
        if (filament != null && reelNumber >= 0 && reelNumber <= 2)
        {
            switch (reelNumber)
            {
                case 0:
                    currentStatus.setReel0EEPROMState(EEPROMState.PROGRAMMED);
                    currentStatus.setFilament1SwitchStatus(true);
                    break;
                case 1:
                    currentStatus.setReel1EEPROMState(EEPROMState.PROGRAMMED);
                    currentStatus.setFilament2SwitchStatus(true);
                    break;
            }
            attachedReels[reelNumber] = new Reel();
            attachedReels[reelNumber].updateContents(filament);
            success = true;
        }

        return success;
    }

    @Override
    protected boolean connectToPrinter()
    {
        steno.info("Dummy printer connected");
        return true;
    }

    @Override
    protected void disconnectPrinter()
    {
        steno.info("Dummy printer disconnected");
    }

    private boolean attachHead(String headName)
    {
        boolean success = false;

        HeadFile headData = HeadContainer.getHeadByID(headName);

        if (headData != null)
        {
            currentStatus.setHeadEEPROMState(EEPROMState.PROGRAMMED);
            attachedHead = new Head(headData);
            currentStatus.setHeadPowerOn(true);
            success = true;
        } else if (headName.equalsIgnoreCase("BLANK"))
        {
            currentStatus.setHeadEEPROMState(EEPROMState.PROGRAMMED);
            attachedHead = new Head();
            success = true;
        } else if (headName.equalsIgnoreCase("UNFORMATTED"))
        {
            currentStatus.setHeadEEPROMState(EEPROMState.NOT_PROGRAMMED);
            attachedHead = new Head();
            success = true;
        } else if (headName.equalsIgnoreCase("BADTYPE"))
        {
            currentStatus.setHeadEEPROMState(EEPROMState.PROGRAMMED);
            attachedHead = new Head();
            attachedHead.typeCodeProperty().set("WRONG");
            success = true;
        } else if (headName.equalsIgnoreCase("UNREAL"))
        {
            currentStatus.setHeadEEPROMState(EEPROMState.PROGRAMMED);
            attachedHead = new Head();
            attachedHead.typeCodeProperty().set("RBX01-??");
            success = true;
        }

        return success;
    }

    private boolean attachExtruder(int extruderNumber)
    {
        boolean success = false;

        switch (extruderNumber)
        {
            case 0:
                currentStatus.setExtruderEPresent(true);
                success = true;
                break;
            case 1:
                currentStatus.setExtruderDPresent(true);
                success = true;
                break;
            default:
        }

        return success;
    }

    private boolean detachExtruder(int extruderNumber)
    {
        boolean success = false;

        switch (extruderNumber)
        {
            case 0:
                currentStatus.setExtruderEPresent(false);
                success = true;
                break;
            case 1:
                currentStatus.setExtruderDPresent(false);
                success = true;
                break;
            default:
        }

        return success;
    }

    private void setPrintLine(int printLineNumber)
    {
        currentStatus.setPrintJobLineNumber(printLineNumber);
    }

    protected void finishPrintJob()
    {
        currentStatus.setPrintJobLineNumberString("");
        currentStatus.setRunningPrintJobID("");
    }

    protected void raiseError(FirmwareError error)
    {
        errorStatus.getFirmwareErrors().add(error);
    }

    protected void clearError(FirmwareError error)
    {
        errorStatus.getFirmwareErrors().remove(error);
    }

    protected void clearAllErrors()
    {
        errorStatus.getFirmwareErrors().clear();
    }

    @Override
    public RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite, boolean dontPublishResult) throws RoboxCommsException
    {
        RoboxRxPacket response = null;

//        steno.debug("Dummy printer received " + messageToWrite.getPacketType().name());
        if (messageToWrite instanceof QueryFirmwareVersion)
        {
            FirmwareResponse firmwareResponse = (FirmwareResponse) RoboxRxPacketFactory.
                    createPacket(RxPacketTypeEnum.FIRMWARE_RESPONSE);
            firmwareResponse.setFirmwareRevision("123");
            response = (RoboxRxPacket) firmwareResponse;
        } else if (messageToWrite instanceof ReadPrinterID)
        {
            PrinterIDResponse idResponse = (PrinterIDResponse) RoboxRxPacketFactory.createPacket(
                    RxPacketTypeEnum.PRINTER_ID_RESPONSE);
            idResponse.setEdition("KS");
            idResponse.setPrinterFriendlyName(printerName);
            idResponse.setPrinterColour(Color.web("#FF0082"));
            response = (RoboxRxPacket) idResponse;
        } else if (messageToWrite instanceof StatusRequest)
        {
            currentStatus.setAmbientTemperature((int) (Math.random() * 100));
            handleNozzleTempChange();
            handleBedTempChange();

            if (!printJobID.equals(NOTHING_PRINTING_JOB_ID))
            {
                printJobLineNo += 100;

                if (printJobLineNo >= linesInCurrentPrintJob)
                {
                    printJobLineNo = 0;
                    printJobID = NOTHING_PRINTING_JOB_ID;
                }
            }
            currentStatus.setPrintJobLineNumber(printJobLineNo);
            currentStatus.setRunningPrintJobID(printJobID);
            currentStatus.setHeadPowerOn(true);

            response = (RoboxRxPacket) currentStatus;
        } else if (messageToWrite instanceof AbortPrint)
        {
            steno.debug("ABORT print");
            printJobLineNo = 0;
            printJobID = NOTHING_PRINTING_JOB_ID;
            currentStatus.setPrintJobLineNumber(printJobLineNo);
            currentStatus.setRunningPrintJobID(printJobID);
            currentStatus.setNozzle0HeaterMode(HeaterMode.OFF);
            currentStatus.setNozzle0Temperature(0);
            currentStatus.setNozzle0TargetTemperature(0);
            currentStatus.setAmbientTargetTemperature(0);
            currentStatus.setAmbientTemperature(0);
            currentStatus.setBedHeaterMode(HeaterMode.OFF);
            currentStatus.setBedTargetTemperature(0);
            currentStatus.setBedTemperature(0);
            response = (AckResponse) RoboxRxPacketFactory.createPacket(
                    RxPacketTypeEnum.ACK_WITH_ERRORS);
        } else if (messageToWrite instanceof ReportErrors)
        {
            response = errorStatus;
        } else if (messageToWrite instanceof SendResetErrors)
        {
            errorStatus.getFirmwareErrors().clear();
            response = errorStatus;
        } else if (messageToWrite instanceof SendGCodeRequest)
        {
            SendGCodeRequest request = (SendGCodeRequest) messageToWrite;
            GCodeDataResponse gcodeResponse = (GCodeDataResponse) RoboxRxPacketFactory.createPacket(
                    RxPacketTypeEnum.GCODE_RESPONSE);

            String messageData = request.getMessageData().trim();
            if (messageData.equalsIgnoreCase(defaultRoboxAttachCommand))
            {
                gcodeResponse.setMessagePayload(
                        "Adding single material head, 1 extruder loaded with orange PLA to dummy printer");
                attachExtruder(0);
                attachHead("RBX01-SM");
                currentStatus.setFilament1SwitchStatus(true);
            } else if (messageData.equalsIgnoreCase(defaultRoboxAttachCommand2))
            {
                gcodeResponse.setMessagePayload(
                        "Adding dual material head, 1 extruder loaded with red ABS to dummy printer");
                attachExtruder(0);
                attachHead("RBX01-DM");
                attachReel("RBX-ABS-RD537", 0);

            }else if (messageData.equalsIgnoreCase(defaultRoboxAttachCommand3))
            {
                gcodeResponse.setMessagePayload(
                        "Adding dual material head, 2 extruders loaded with red and black ABS to dummy printer");
                attachExtruder(0);
                attachExtruder(1);
                attachHead("RBX01-DM");
                attachReel("RBX-ABS-RD537", 0);
                attachReel("RBX-ABS-BK091", 1);

            } else if (messageData.startsWith(attachHeadCommand))
            {
                String headName = messageData.replaceAll(attachHeadCommand, "");
                boolean headAttached = attachHead(headName);
                if (headAttached)
                {
                    gcodeResponse.setMessagePayload("Adding head " + headName + " to dummy printer");
                } else
                {
                    gcodeResponse.setMessagePayload("Couldn't add head " + headName
                            + " to dummy printer");
                }
            } else if (messageData.startsWith(detachHeadCommand))
            {
                currentStatus.setHeadEEPROMState(EEPROMState.NOT_PRESENT);
                currentStatus.setHeadPowerOn(false);
            } else if (messageData.equalsIgnoreCase(detachPrinterCommand))
            {
                RoboxCommsManager.getInstance().removeDummyPrinter(printerHandle);
            } else if (messageData.startsWith(attachReelCommand))
            {
                boolean attachSuccess = false;
                String filamentName = "";

                String[] attachReelElements = messageData.replaceAll(attachReelCommand, "").trim().
                        split(" ");
                if (attachReelElements.length == 2)
                {
                    filamentName = attachReelElements[0];
                    int reelNumber = Integer.valueOf(attachReelElements[1]);
                    attachSuccess = attachReel(filamentName, reelNumber);
                }

                if (attachSuccess)
                {
                    gcodeResponse.setMessagePayload("Adding reel " + filamentName
                            + " to dummy printer");
                } else
                {
                    gcodeResponse.setMessagePayload("Couldn't attach reel - " + filamentName);
                }
            } else if (messageData.startsWith(detachReelCommand))
            {
                int reelNumber = Integer.valueOf(messageData.replaceAll(detachReelCommand, "").
                        trim());
                detachReel(reelNumber);
            } else if (messageData.startsWith(goToPrintLineCommand))
            {
                String printJobLineNumberString = messageData.replaceAll(goToPrintLineCommand, "");
                setPrintLine(Integer.valueOf(printJobLineNumberString));
            } else if (messageData.equalsIgnoreCase(finishPrintCommand))
            {
                finishPrintJob();
            } else if (messageData.startsWith(attachExtruderCommand))
            {
                String extruderNumberString = messageData.replaceAll(attachExtruderCommand, "");
                switch (extruderNumberString)
                {
                    case "0":
                        attachExtruder(0);
                        break;
                    case "1":
                        attachExtruder(1);
                        break;
                }
            } else if (messageData.startsWith(detachExtruderCommand))
            {
                String extruderNumberString = messageData.replaceAll(unloadFilamentCommand, "");
                switch (extruderNumberString)
                {
                    case "0":
                        detachExtruder(0);
                        break;
                    case "1":
                        detachExtruder(1);
                        break;
                }
            } else if (messageData.startsWith(loadFilamentCommand))
            {
                String extruderNumberString = messageData.replaceAll(loadFilamentCommand, "");
                switch (extruderNumberString)
                {
                    case "0":
                        currentStatus.setFilament1SwitchStatus(true);
                        break;
                    case "1":
                        currentStatus.setFilament2SwitchStatus(true);
                        break;
                }
            } else if (messageData.startsWith(unloadFilamentCommand))
            {
                String extruderNumberString = messageData.replaceAll(unloadFilamentCommand, "");
                switch (extruderNumberString)
                {
                    case "0":
                        currentStatus.setFilament1SwitchStatus(false);
                        break;
                    case "1":
                        currentStatus.setFilament2SwitchStatus(false);
                        break;
                }
            } else if (messageData.equalsIgnoreCase(insertSDCardCommand))
            {
                currentStatus.setSdCardPresent(true);
            } else if (messageData.equalsIgnoreCase(removeSDCardCommand))
            {
                currentStatus.setSdCardPresent(false);
            } else if (messageData.startsWith("M104 S"))
            {
                nozzleTargetTemperatureS = Integer.parseInt(messageData.substring(6));
                steno.debug("set S temp to " + nozzleTargetTemperatureS);
                if (nozzleTargetTemperatureS == 0)
                {
                    nozzleHeaterModeS = HeaterMode.OFF;
                    steno.debug("set heater mode off for S");
                }
            } else if (messageData.startsWith("M104 T"))
            {
                if (messageData.substring(6).length() > 0)
                {
                    nozzleTargetTemperatureT = Integer.parseInt(messageData.substring(6));
                    steno.debug("set T temp to " + nozzleTargetTemperatureT);
                    if (nozzleTargetTemperatureT == 0)
                    {
                        nozzleHeaterModeT = HeaterMode.OFF;
                        steno.debug("set heater mode off for T");
                    }
                } else
                {
                    nozzleHeaterModeT = HeaterMode.NORMAL;
                    steno.debug("set heater mode T to normal");
                }
            } else if (messageData.startsWith("M104"))
            {
                nozzleHeaterModeS = HeaterMode.NORMAL;
                steno.debug("set heater mode S to normal");
            } else if (messageData.startsWith("M140 S") || messageData.startsWith("M139 S"))
            {
                bedTargetTemperature = Integer.parseInt(messageData.substring(6));
                steno.debug("set bed target temp to " + bedTargetTemperature);
                if (bedTargetTemperature == 0)
                {
                    bedHeaterMode = HeaterMode.OFF;
                    steno.debug("set bed heater mode off");
                }
            } else if (messageData.startsWith("M140") || messageData.startsWith("M139"))
            {
                bedHeaterMode = HeaterMode.NORMAL;
                steno.debug("set bed heater mode normal");
            } else if (messageData.startsWith("M113"))
            {
                // ZDelta
                gcodeResponse.populatePacket("0000eZdelta:0.01\nok".getBytes(), RoboxRxPacketFactory.USE_LATEST_FIRMWARE_VERSION);
            } else if (messageData.startsWith(errorCommand))
            {
                String errorString = messageData.replaceAll(errorCommand, "");
                try
                {
                    FirmwareError fwError = FirmwareError.valueOf(errorString);
                    if (fwError != null)
                    {
                        errorStatus.getFirmwareErrors().add(fwError);
                    }
                } catch (IllegalArgumentException ex)
                {
                    steno.info("Dummy printer didn't understand error " + errorString);
                }
            } else if (messageData.startsWith("M121"))
            {
                String extruderAxis = messageData.replaceAll("M121", "").trim();
                switch (extruderAxis)
                {
                    case "E":
                        currentStatus.setFilament1SwitchStatus(false);
                        break;
                    case "D":
                        currentStatus.setFilament2SwitchStatus(false);
                        break;
                }
            }

            response = (RoboxRxPacket) gcodeResponse;
        } else if (messageToWrite instanceof FormatHeadEEPROM)
        {
            currentStatus.setHeadEEPROMState(EEPROMState.PROGRAMMED);
            attachedHead = new Head();
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().
                    getExpectedResponse());
        } else if (messageToWrite instanceof ReadHeadEEPROM)
        {
            HeadEEPROMDataResponse headResponse = (HeadEEPROMDataResponse) RoboxRxPacketFactory.
                    createPacket(RxPacketTypeEnum.HEAD_EEPROM_DATA);

            headResponse.updateContents(attachedHead);
            response = (RoboxRxPacket) headResponse;
        } else if (messageToWrite instanceof WriteHeadEEPROM)
        {
            WriteHeadEEPROM headWriteCommand = (WriteHeadEEPROM) messageToWrite;

            HeadEEPROMDataResponse headResponse = (HeadEEPROMDataResponse) RoboxRxPacketFactory.
                    createPacket(RxPacketTypeEnum.HEAD_EEPROM_DATA);

            headResponse.updateFromWrite(headWriteCommand);
            attachedHead.updateFromEEPROMData(headResponse);

            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().
                    getExpectedResponse());
        } else if (messageToWrite instanceof ReadReel0EEPROM)
        {
            ReelEEPROMDataResponse reelResponse = (ReelEEPROMDataResponse) RoboxRxPacketFactory.
                    createPacket(RxPacketTypeEnum.REEL_0_EEPROM_DATA);

            reelResponse.updateContents(attachedReels[0]);
            response = (RoboxRxPacket) reelResponse;
        } else if (messageToWrite instanceof ReadReel1EEPROM)
        {
            ReelEEPROMDataResponse reelResponse = (ReelEEPROMDataResponse) RoboxRxPacketFactory.
                    createPacket(RxPacketTypeEnum.REEL_1_EEPROM_DATA);

            reelResponse.updateContents(attachedReels[1]);
            reelResponse.setReelNumber(1);
            response = (RoboxRxPacket) reelResponse;
        } else if (messageToWrite instanceof PausePrint)
        {
            switch (messageToWrite.getMessageData())
            {
                case "0":
                    currentStatus.setPauseStatus(PauseStatus.NOT_PAUSED);
                    break;
                case "1":
                    currentStatus.setPauseStatus(PauseStatus.PAUSED);
                    break;
            }
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().
                    getExpectedResponse());
        } else if (messageToWrite instanceof SendPrintFileStart)
        {
            linesInCurrentPrintJob = 0;
            printJobID = messageToWrite.getMessageData();
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().
                    getExpectedResponse());
            steno.debug("Got start of print file - job " + printJobID);
        } else if (messageToWrite instanceof SendDataFileStart)
        {
            linesInCurrentPrintJob = 0;
            printJobID = messageToWrite.getMessageData();
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().
                    getExpectedResponse());
            steno.debug("Got start of data file - job " + printJobID);
        } else if (messageToWrite instanceof SendDataFileChunk)
        {
            String payload = messageToWrite.getMessageData();
            String onlyCommas = payload.replaceAll("[^\r]+", "");
            linesInCurrentPrintJob += onlyCommas.length();
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().
                    getExpectedResponse());
            steno.debug("Got data file chunk");
        } else if (messageToWrite instanceof SendDataFileEnd)
        {
            String payload = messageToWrite.getMessageData();
            String onlyCommas = payload.replaceAll("[^\r]+", "");
            linesInCurrentPrintJob += onlyCommas.length();
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().
                    getExpectedResponse());
            steno.debug("Got end of data file");
        } else if (messageToWrite instanceof InitiatePrint)
        {
            printJobID = messageToWrite.getMessageData();
            steno.debug("Dummy printer asked to initiate print " + printJobID);
            currentStatus.setRunningPrintJobID(printJobID);
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().
                    getExpectedResponse());
            steno.debug("Asked to initiate print " + printJobID);
        } else if (messageToWrite instanceof AbortPrint)
        {
            currentStatus.setRunningPrintJobID(null);
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().
                    getExpectedResponse());
        } else
        {
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().
                    getExpectedResponse());
        }

        if (!dontPublishResult)
        {
            printerToUse.processRoboxResponse(response);
        }

        return response;
    }
}
