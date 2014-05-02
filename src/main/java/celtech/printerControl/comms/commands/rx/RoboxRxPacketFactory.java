/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.rx;

import celtech.printerControl.comms.commands.exceptions.UnableToGenerateRoboxPacketException;
import celtech.printerControl.comms.commands.exceptions.UnknownPacketTypeException;
import celtech.printerControl.comms.commands.exceptions.InvalidCommandByteException;

/**
 *
 * @author ianhudson
 */
public class RoboxRxPacketFactory
{

    public final static byte commandByteMask = (byte) 0x80;

    private RoboxRxPacketFactory()
    {
    }

    public static RoboxRxPacket createPacket(RxPacketTypeEnum packetType)
    {
        RoboxRxPacket returnVal = null;

        if (packetType != null)
        {
            switch (packetType)
            {
                case STATUS_RESPONSE:
                    returnVal = new StatusResponse();
                    break;
                case FIRMWARE_RESPONSE:
                    returnVal = new FirmwareResponse();
                    break;
                case ACK_WITH_ERRORS:
                    returnVal = new AckResponse();
                    break;
                case PRINTER_ID_RESPONSE:
                    returnVal = new PrinterIDResponse();
                    break;
                case REEL_EEPROM_DATA:
                    returnVal = new ReelEEPROMDataResponse();
                    break;
                case HEAD_EEPROM_DATA:
                    returnVal = new HeadEEPROMDataResponse();
                    break;
                case GCODE_RESPONSE:
                    returnVal = new GCodeDataResponse();
                    break;
                case LIST_FILES_RESPONSE:
                    returnVal = new ListFilesResponse();
                    break;
                default:
                    break;
            }
        }

        return returnVal;
    }

    public static RoboxRxPacket createPacket(byte[] inputBytes) throws InvalidCommandByteException, UnableToGenerateRoboxPacketException, UnknownPacketTypeException
    {
        RoboxRxPacket returnVal = null;

        if ((inputBytes[0] & commandByteMask) != commandByteMask)
        {
            throw new InvalidCommandByteException();
        }

        RxPacketTypeEnum packetType = RxPacketTypeEnum.getEnumForCommand(inputBytes[0]);

        if (packetType != null)
        {
            switch (packetType)
            {
                case STATUS_RESPONSE:
                    StatusResponse statusResponse = new StatusResponse();
                    statusResponse.populatePacket(inputBytes);
                    returnVal = statusResponse;
                    break;
                case ACK_WITH_ERRORS:
                    AckResponse ackResponse = new AckResponse();
                    ackResponse.populatePacket(inputBytes);
                    returnVal = ackResponse;
                    break;
                case FIRMWARE_RESPONSE:
                    FirmwareResponse fwResponse = new FirmwareResponse();
                    fwResponse.populatePacket(inputBytes);
                    returnVal = fwResponse;
                    break;
                case PRINTER_ID_RESPONSE:
                    PrinterIDResponse printerIDResponse = new PrinterIDResponse();
                    printerIDResponse.populatePacket(inputBytes);
                    returnVal = printerIDResponse;
                    break;
                case REEL_EEPROM_DATA:
                    ReelEEPROMDataResponse reelEepromDataResponse = new ReelEEPROMDataResponse();
                    reelEepromDataResponse.populatePacket(inputBytes);
                    returnVal = reelEepromDataResponse;
                    break;
                case HEAD_EEPROM_DATA:
                    HeadEEPROMDataResponse headEepromDataResponse = new HeadEEPROMDataResponse();
                    headEepromDataResponse.populatePacket(inputBytes);
                    returnVal = headEepromDataResponse;
                    break;
                case GCODE_RESPONSE:
                    GCodeDataResponse gcodeDataResponse = new GCodeDataResponse();
                    gcodeDataResponse.populatePacket(inputBytes);
                    returnVal = gcodeDataResponse;
                    break;
                default:
                    throw new UnknownPacketTypeException();
            }

            if (returnVal == null)
            {
                throw new UnableToGenerateRoboxPacketException();
            }
        }

        return returnVal;
    }
}
