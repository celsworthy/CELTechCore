package celtech.printerControl.comms;

import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
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
import celtech.printerControl.comms.commands.tx.QueryFirmwareVersion;
import celtech.printerControl.comms.commands.tx.ReadHeadEEPROM;
import celtech.printerControl.comms.commands.tx.ReadPrinterID;
import celtech.printerControl.comms.commands.tx.ReadReel0EEPROM;
import celtech.printerControl.comms.commands.tx.ReportErrors;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacketFactory;
import celtech.printerControl.comms.commands.tx.SendGCodeRequest;
import celtech.printerControl.comms.commands.tx.StatusRequest;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;
import celtech.printerControl.comms.commands.tx.WriteHeadEEPROM;
import celtech.printerControl.comms.commands.tx.WritePrinterID;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Reel;
import javafx.scene.paint.Color;

/**
 *
 * @author Ian
 */
public class TestCommandInterface extends CommandInterface
{

    public static final Color defaultPrinterColour = Color.CRIMSON;

    private final String attachHeadCommand = "ATTACH HEAD ";
    private final String detachHeadCommand = "DETACH HEAD";
    private final String attachReelCommand = "ATTACH REEL ";
    private final String detachReelCommand = "DETACH REEL";

    private StatusResponse currentStatus = null;
    private Reel attachedReel = null;
    private PrinterIDResponse printerID = null;
    private HeadEEPROMDataResponse headResponse = (HeadEEPROMDataResponse) RoboxRxPacketFactory.createPacket(
        RxPacketTypeEnum.HEAD_EEPROM_DATA);

    public TestCommandInterface(PrinterStatusConsumer controlInterface, String portName,
        boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks)
    {
        super(controlInterface, portName, suppressPrinterIDChecks, sleepBetweenStatusChecks);
        this.setName("Dummy Printer");

        preTestInitialisation();
    }

    @Override
    protected void setSleepBetweenStatusChecks(int sleepMillis)
    {
    }

    @Override
    public RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite) throws RoboxCommsException
    {
        RoboxRxPacket response = null;

        steno.debug("Dummy printer received " + messageToWrite.getPacketType().name());

        if (messageToWrite instanceof QueryFirmwareVersion)
        {
            FirmwareResponse firmwareResponse = (FirmwareResponse) RoboxRxPacketFactory.createPacket(
                RxPacketTypeEnum.FIRMWARE_RESPONSE);
            firmwareResponse.setFirmwareRevision("123");
            firmwareResponse.setFirmwareRevisionInt(123);
            response = (RoboxRxPacket) firmwareResponse;
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
            GCodeDataResponse gcodeResponse = (GCodeDataResponse) RoboxRxPacketFactory.createPacket(
                RxPacketTypeEnum.GCODE_RESPONSE);

            if (request.getMessageData().startsWith(attachHeadCommand))
            {
                String headName = request.getMessageData().replaceAll(attachHeadCommand, "");
                HeadFile headData = HeadContainer.getHeadByID(headName);
                if (headData != null)
                {
                    currentStatus.setHeadEEPROMState(EEPROMState.PROGRAMMED);
                    Head attachedHead = new Head(headData);
                    headResponse.updateContents(attachedHead);
                    gcodeResponse.setMessagePayload("Adding head " + headName + " to dummy printer");
                } else
                {
                    gcodeResponse.setMessagePayload("Didn't recognise head name - " + headName);
                }
            } else if (request.getMessageData().startsWith(detachHeadCommand))
            {
                currentStatus.setHeadEEPROMState(EEPROMState.NOT_PRESENT);
            } else if (request.getMessageData().startsWith(attachReelCommand))
            {
                String filamentName = request.getMessageData().replaceAll(attachReelCommand, "");
                Filament filament = FilamentContainer.getFilamentByID(filamentName);
                if (filament != null)
                {
                    currentStatus.setReel0EEPROMState(EEPROMState.PROGRAMMED);
                    attachedReel = new Reel();
                    attachedReel.updateContents(filament);
                    gcodeResponse.setMessagePayload("Adding reel " + filamentName
                        + " to dummy printer");
                } else
                {
                    gcodeResponse.setMessagePayload("Didn't recognise filament name - "
                        + filamentName);
                }
            } else if (request.getMessageData().startsWith(detachReelCommand))
            {
                currentStatus.setReel0EEPROMState(EEPROMState.NOT_PRESENT);
            }

            response = (RoboxRxPacket) gcodeResponse;
        } else if (messageToWrite instanceof ReadHeadEEPROM)
        {
            response = (RoboxRxPacket) headResponse;
        } else if (messageToWrite instanceof ReadReel0EEPROM)
        {
            ReelEEPROMDataResponse reelResponse = (ReelEEPROMDataResponse) RoboxRxPacketFactory.createPacket(
                RxPacketTypeEnum.REEL_EEPROM_DATA);

            reelResponse.updateContents(attachedReel);
            response = (RoboxRxPacket) reelResponse;
        } else if (messageToWrite instanceof WriteHeadEEPROM)
        {
            response = RoboxRxPacketFactory.createPacket(
                messageToWrite.getPacketType().getExpectedResponse());
            copyWriteHeadEEPROMToDataResponse((WriteHeadEEPROM) messageToWrite, headResponse);
        } else if (messageToWrite instanceof WritePrinterID)
        {
            response = RoboxRxPacketFactory.createPacket(
                messageToWrite.getPacketType().getExpectedResponse());

            WritePrinterID writeID = (WritePrinterID) messageToWrite;
            printerID.setEdition(writeID.getEdition());
            printerID.setModel(writeID.getModel());
            printerID.setPoNumber(writeID.getPoNumber());
            printerID.setPrinterColour(writeID.getColour());
            printerID.setPrinterFriendlyName(writeID.getPrinterFriendlyName());
            printerID.setSerialNumber(writeID.getSerialNumber());
            printerID.setWeekOfManufacture(writeID.getWeekOfManufacture());
            printerID.setYearOfManufacture(writeID.getYearOfManufacture());
        } else if (messageToWrite instanceof ReadPrinterID)
        {
            response = (RoboxRxPacket) printerID;
        } else
        {
            response = RoboxRxPacketFactory.createPacket(
                messageToWrite.getPacketType().getExpectedResponse());
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

    public void noHead()
    {
        currentStatus.setHeadEEPROMState(EEPROMState.NOT_PRESENT);
    }

    public void noReels()
    {
        currentStatus.setReel0EEPROMState(EEPROMState.NOT_PRESENT);
    }

    public void addHead(HeadEEPROMDataResponse headResponse)
    {
        this.headResponse = headResponse;
        currentStatus.setHeadEEPROMState(EEPROMState.PROGRAMMED);
    }

    public void preTestInitialisation()
    {
        currentStatus = (StatusResponse) RoboxRxPacketFactory.createPacket(
            RxPacketTypeEnum.STATUS_RESPONSE);
        noHead();
        noReels();

        printerID = (PrinterIDResponse) RoboxRxPacketFactory.createPacket(
            RxPacketTypeEnum.PRINTER_ID_RESPONSE);
        printerID.setEdition("KS");
        printerID.setPrinterFriendlyName("Dummy");
        printerID.setPrinterColour(defaultPrinterColour);
    }

    private void copyWriteHeadEEPROMToDataResponse(WriteHeadEEPROM message,
        HeadEEPROMDataResponse headResponse)
    {
        headResponse.setHeadTypeCode(message.getHeadTypeCode());
        headResponse.setUniqueID(message.getHeadUniqueID());
        headResponse.setMaximumTemperature(message.getMaximumTemperature());
        headResponse.setThermistorBeta(message.getThermistorBeta());
        headResponse.setThermistorTCal(message.getThermistorTCal());
        headResponse.setNozzle1XOffset(message.getNozzle1XOffset());
        headResponse.setNozzle1YOffset(message.getNozzle1YOffset());
        headResponse.setNozzle1ZOffset(message.getNozzle1ZOffset());
        headResponse.setNozzle1BOffset(message.getNozzle1BOffset());
        headResponse.setNozzle2XOffset(message.getNozzle2XOffset());
        headResponse.setNozzle2YOffset(message.getNozzle2YOffset());
        headResponse.setNozzle2ZOffset(message.getNozzle2ZOffset());
        headResponse.setNozzle2BOffset(message.getNozzle2BOffset());
        headResponse.setLastFilamentTemperature(message.getLastFilamentTemperature());
        headResponse.setHoursUsed(message.getHourCounter());

    }
}
