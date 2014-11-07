package celtech.printerControl.comms;

import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.PauseStatus;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
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
import celtech.printerControl.comms.commands.tx.ReadReelEEPROM;
import celtech.printerControl.comms.commands.tx.ReportErrors;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.SendDataFileStart;
import celtech.printerControl.comms.commands.tx.SendGCodeRequest;
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

    private Stenographer steno = StenographerFactory.getStenographer(DummyPrinterCommandInterface.class.getName());

    private final String attachHeadCommand = "ATTACH HEAD ";
    private final String detachHeadCommand = "DETACH HEAD";
    private final String attachReelCommand = "ATTACH REEL ";
    private final String detachReelCommand = "DETACH REEL";
    private final String detachPrinterCommand = "DETACH PRINTER";
    private final String goToPrintLineCommand = "GOTO LINE ";
    private final String finishPrintCommand = "FINISH PRINT";
    private final String loadFilamentCommand = "LOAD ";
    private final String unloadFilamentCommand = "UNLOAD ";
    private final String insertSDCardCommand = "INSERT SD";
    private final String removeSDCardCommand = "REMOVE SD";

    private final StatusResponse currentStatus = (StatusResponse) RoboxRxPacketFactory.createPacket(RxPacketTypeEnum.STATUS_RESPONSE);
    private Head attachedHead = null;
    private Reel attachedReel = null;
    private String printerName;

    private String printJobID = null;

    public DummyPrinterCommandInterface(PrinterStatusConsumer controlInterface, String portName,
        boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks, String printerName)
    {
        super(controlInterface, portName, suppressPrinterIDChecks, sleepBetweenStatusChecks);
        this.setName(printerName);
        this.printerName = printerName;

        currentStatus.setSdCardPresent(true);
    }

    public DummyPrinterCommandInterface(PrinterStatusConsumer controlInterface, String portName,
        boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks)
    {
        this(controlInterface, portName, suppressPrinterIDChecks, sleepBetweenStatusChecks, "Dummy Printer");
    }

    @Override
    protected void setSleepBetweenStatusChecks(int sleepMillis)
    {
    }

    @Override
    public RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite) throws RoboxCommsException
    {
        RoboxRxPacket response = null;

//        steno.debug("Dummy printer received " + messageToWrite.getPacketType().name());
        if (messageToWrite instanceof QueryFirmwareVersion)
        {
            FirmwareResponse firmwareResponse = (FirmwareResponse) RoboxRxPacketFactory.createPacket(RxPacketTypeEnum.FIRMWARE_RESPONSE);
            firmwareResponse.setFirmwareRevision("123");
            firmwareResponse.setFirmwareRevisionInt(123);
            response = (RoboxRxPacket) firmwareResponse;
        } else if (messageToWrite instanceof ReadPrinterID)
        {
            PrinterIDResponse idResponse = (PrinterIDResponse) RoboxRxPacketFactory.createPacket(RxPacketTypeEnum.PRINTER_ID_RESPONSE);
            idResponse.setEdition("KS");
            idResponse.setPrinterFriendlyName(printerName);
            idResponse.setPrinterColour(Color.web("#FF0082"));
            response = (RoboxRxPacket) idResponse;
        } else if (messageToWrite instanceof StatusRequest)
        {
            currentStatus.setAmbientTemperature((int) (Math.random() * 100));
            response = (RoboxRxPacket) currentStatus;
        } else if (messageToWrite instanceof ReportErrors)
        {
            response = RoboxRxPacketFactory.createPacket(RxPacketTypeEnum.ACK_WITH_ERRORS);
        } else if (messageToWrite instanceof SendGCodeRequest)
        {
            SendGCodeRequest request = (SendGCodeRequest) messageToWrite;
            GCodeDataResponse gcodeResponse = (GCodeDataResponse) RoboxRxPacketFactory.createPacket(RxPacketTypeEnum.GCODE_RESPONSE);

            String messageData = request.getMessageData().trim();
            if (messageData.startsWith(attachHeadCommand))
            {
                String headName = messageData.replaceAll(attachHeadCommand, "");
                HeadFile headData = HeadContainer.getHeadByID(headName);

                if (headData != null)
                {
                    currentStatus.setHeadEEPROMState(EEPROMState.PROGRAMMED);
                    attachedHead = new Head(headData);
                    gcodeResponse.setMessagePayload("Adding head " + headName + " to dummy printer");
                } else if (headName.equalsIgnoreCase("BLANK"))
                {
                    currentStatus.setHeadEEPROMState(EEPROMState.PROGRAMMED);
                    attachedHead = new Head();
                    gcodeResponse.setMessagePayload("Adding blank head to dummy printer");
                } else if (headName.equalsIgnoreCase("UNFORMATTED"))
                {
                    currentStatus.setHeadEEPROMState(EEPROMState.NOT_PROGRAMMED);
                    attachedHead = new Head();
                    gcodeResponse.setMessagePayload("Adding unformatted head to dummy printer");
                } else if (headName.equalsIgnoreCase("BADTYPE"))
                {
                    currentStatus.setHeadEEPROMState(EEPROMState.PROGRAMMED);
                    attachedHead = new Head();
                    attachedHead.typeCodeProperty().set("WRONG");
                    gcodeResponse.setMessagePayload("Adding head with invalid type code to dummy printer");
                } else if (headName.equalsIgnoreCase("UNREAL"))
                {
                    currentStatus.setHeadEEPROMState(EEPROMState.PROGRAMMED);
                    attachedHead = new Head();
                    attachedHead.typeCodeProperty().set("RBX01-??");
                    gcodeResponse.setMessagePayload("Adding head with valid but unknown type code to dummy printer");
                } else
                {
                    gcodeResponse.setMessagePayload("Didn't recognise head name - " + headName);
                }
            } else if (messageData.startsWith(detachHeadCommand))
            {
                currentStatus.setHeadEEPROMState(EEPROMState.NOT_PRESENT);
            } else if (messageData.equalsIgnoreCase(detachPrinterCommand))
            {
                RoboxCommsManager.getInstance().removeDummyPrinter(portName);
            } else if (messageData.startsWith(attachReelCommand))
            {
                String filamentName = messageData.replaceAll(attachReelCommand, "");
                Filament filament = FilamentContainer.getFilamentByID(filamentName);
                if (filament != null)
                {
                    currentStatus.setReelEEPROMState(EEPROMState.PROGRAMMED);
                    attachedReel = new Reel();
                    attachedReel.updateContents(filament);
                    gcodeResponse.setMessagePayload("Adding reel " + filamentName + " to dummy printer");
                } else
                {
                    gcodeResponse.setMessagePayload("Didn't recognise filament name - " + filamentName);
                }
            } else if (messageData.equalsIgnoreCase(detachReelCommand))
            {
                currentStatus.setReelEEPROMState(EEPROMState.NOT_PRESENT);
                attachedReel = null;
            } else if (messageData.startsWith(goToPrintLineCommand))
            {
                String printJobLineNumberString = messageData.replaceAll(goToPrintLineCommand, "");
                setPrintLine(Integer.valueOf(printJobLineNumberString));
            } else if (messageData.equalsIgnoreCase(finishPrintCommand))
            {
                finishPrintJob();
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
            }

            response = (RoboxRxPacket) gcodeResponse;
        } else if (messageToWrite instanceof FormatHeadEEPROM)
        {
            currentStatus.setHeadEEPROMState(EEPROMState.PROGRAMMED);
            attachedHead = new Head();
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().getExpectedResponse());
        } else if (messageToWrite instanceof ReadHeadEEPROM)
        {
            HeadEEPROMDataResponse headResponse = (HeadEEPROMDataResponse) RoboxRxPacketFactory.createPacket(RxPacketTypeEnum.HEAD_EEPROM_DATA);

            headResponse.updateContents(attachedHead);
            response = (RoboxRxPacket) headResponse;
        } else if (messageToWrite instanceof WriteHeadEEPROM)
        {
            WriteHeadEEPROM headWriteCommand = (WriteHeadEEPROM)messageToWrite;
            
            HeadEEPROMDataResponse headResponse = (HeadEEPROMDataResponse) RoboxRxPacketFactory.createPacket(RxPacketTypeEnum.HEAD_EEPROM_DATA);

            headResponse.updateFromWrite(headWriteCommand);
            attachedHead.updateFromEEPROMData(headResponse);
            
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().getExpectedResponse());
        }  else if (messageToWrite instanceof ReadReelEEPROM)
        {
            ReelEEPROMDataResponse reelResponse = (ReelEEPROMDataResponse) RoboxRxPacketFactory.createPacket(RxPacketTypeEnum.REEL_EEPROM_DATA);

            reelResponse.updateContents(attachedReel);
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
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().getExpectedResponse());
        } else if (messageToWrite instanceof SendDataFileStart)
        {
            printJobID = messageToWrite.getMessageData();
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().getExpectedResponse());
        } else if (messageToWrite instanceof InitiatePrint)
        {
            currentStatus.setRunningPrintJobID(printJobID);
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().getExpectedResponse());
        } else if (messageToWrite instanceof AbortPrint)
        {
            currentStatus.setRunningPrintJobID(null);
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().getExpectedResponse());
        } else
        {
            response = RoboxRxPacketFactory.createPacket(messageToWrite.getPacketType().getExpectedResponse());
        }

        printerToUse.processRoboxResponse(response);

        return response;
    }

    @Override
    protected boolean connectToPrinter(String commsPortName)
    {
        steno.info("Dummy printer connected");
        return true;
    }

    @Override
    protected void disconnectSerialPort()
    {
        steno.info("Dummy printer disconnected");
    }

    private void setPrintLine(int printLineNumber)
    {
        currentStatus.setPrintJobLineNumber(printLineNumber);
    }

    private void finishPrintJob()
    {
        currentStatus.setPrintJobLineNumberString("");
        currentStatus.setRunningPrintJobID("");
    }
}
