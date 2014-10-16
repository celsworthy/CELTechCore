package celtech.printerControl.comms;

import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.FilamentContainer;
import celtech.configuration.HeadContainer;
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
import celtech.printerControl.comms.commands.tx.ReadReelEEPROM;
import celtech.printerControl.comms.commands.tx.ReportErrors;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.SendGCodeRequest;
import celtech.printerControl.comms.commands.tx.StatusRequest;
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

    private final StatusResponse currentStatus = (StatusResponse) RoboxRxPacketFactory.createPacket(RxPacketTypeEnum.STATUS_RESPONSE);
    private Head attachedHead = null;
    private Reel attachedReel = null;
    private String printerName;

    public DummyPrinterCommandInterface(PrinterStatusConsumer controlInterface, String portName,
        boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks, String printerName)
    {
        super(controlInterface, portName, suppressPrinterIDChecks, sleepBetweenStatusChecks);
        this.setName(printerName);
        this.printerName = printerName;
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

        steno.debug("Dummy printer received " + messageToWrite.getPacketType().name());

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
                } else
                {
                    gcodeResponse.setMessagePayload("Didn't recognise head name - " + headName);
                }
            } else if (messageData.startsWith(detachHeadCommand))
            {
                currentStatus.setHeadEEPROMState(EEPROMState.NOT_PRESENT);
            }
            else if (messageData.startsWith(attachReelCommand))
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
            } else if (messageData.startsWith(detachReelCommand))
            {
                currentStatus.setReelEEPROMState(EEPROMState.NOT_PRESENT);
            }

            response = (RoboxRxPacket) gcodeResponse;
        } else if (messageToWrite instanceof ReadHeadEEPROM)
        {
            HeadEEPROMDataResponse headResponse = (HeadEEPROMDataResponse) RoboxRxPacketFactory.createPacket(RxPacketTypeEnum.HEAD_EEPROM_DATA);

            headResponse.updateContents(attachedHead);
            response = (RoboxRxPacket) headResponse;
        } 
         else if (messageToWrite instanceof ReadReelEEPROM)
        {
            ReelEEPROMDataResponse reelResponse = (ReelEEPROMDataResponse) RoboxRxPacketFactory.createPacket(RxPacketTypeEnum.REEL_EEPROM_DATA);

            reelResponse.updateContents(attachedReel);
            response = (RoboxRxPacket) reelResponse;
        }
        else
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

}
