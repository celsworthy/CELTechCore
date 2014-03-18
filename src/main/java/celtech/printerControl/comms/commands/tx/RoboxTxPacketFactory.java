/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

import celtech.printerControl.comms.commands.exceptions.UnableToGenerateRoboxPacketException;
import celtech.printerControl.comms.commands.exceptions.UnknownPacketTypeException;
import celtech.printerControl.comms.commands.exceptions.InvalidCommandByteException;

/**
 *
 * @author ianhudson
 */
public class RoboxTxPacketFactory
{

    public final static byte commandByteMask = (byte) 0x80;

    private RoboxTxPacketFactory()
    {
    }

    public static RoboxTxPacket createPacket(TxPacketTypeEnum packetType)
    {
        RoboxTxPacket returnVal = null;

        if (packetType != null)
        {
            switch (packetType)
            {
                case STATUS_REQUEST:
                    returnVal = new StatusRequest();
                    break;
                case EXECUTE_GCODE:
                    returnVal = new SendGCodeRequest();
                    break;
                case START_OF_DATA_FILE:
                    returnVal = new SendDataFileStart();
                    break;
                case DATA_FILE_CHUNK:
                    returnVal = new SendDataFileChunk();
                    break;
                case END_OF_DATA_FILE:
                    returnVal = new SendDataFileEnd();
                    break;
                case RESET_ERRORS:
                    returnVal = new SendResetErrors();
                    break;
                case UPDATE_FIRMWARE:
                    returnVal = new UpdateFirmware();
                    break;
                case INITIATE_PRINT:
                    returnVal = new InitiatePrint();
                    break;
                case ABORT_PRINT:
                    returnVal = new AbortPrint();
                    break;
                case PAUSE_RESUME_PRINT:
                    returnVal = new PausePrint();
                    break;
                case QUERY_FIRMWARE_VERSION:
                    returnVal = new QueryFirmwareVersion();
                    break;
                case READ_PRINTER_ID:
                    returnVal = new ReadPrinterID();
                    break;
                case WRITE_PRINTER_ID:
                    returnVal = new WritePrinterID();
                    break;
                case FORMAT_HEAD_EEPROM:
                    returnVal = new FormatHeadEEPROM();
                    break;
                case FORMAT_REEL_EEPROM:
                    returnVal = new FormatReelEEPROM();
                    break;
                case READ_HEAD_EEPROM:
                    returnVal = new ReadHeadEEPROM();
                    break;
                case READ_REEL_EEPROM:
                    returnVal = new ReadReelEEPROM();
                    break;
                case WRITE_HEAD_EEPROM:
                    returnVal = new WriteHeadEEPROM();
                    break;
                case WRITE_REEL_EEPROM:
                    returnVal = new WriteReelEEPROM();
                    break;
                case SET_AMBIENT_LED_COLOUR:
                    returnVal = new SetAmbientLEDColour();
                    break;
                case CONTROL_REEL_LED:
                    returnVal = new ControlReelLED();
                    break;
                case SET_TEMPERATURES:
                    returnVal = new SetTemperatures();
                    break;
                case SET_FILAMENT_INFO:
                    returnVal = new SetTemperatures();
                    break;
                default:
                    break;
            }
        }

        return returnVal;
    }

    public static RoboxTxPacket createPacket(byte[] inputBytes) throws InvalidCommandByteException, UnableToGenerateRoboxPacketException, UnknownPacketTypeException
    {
        RoboxTxPacket returnVal = null;

        if ((inputBytes[0] & commandByteMask) != commandByteMask)
        {
            throw new InvalidCommandByteException();
        }

        TxPacketTypeEnum packetType = TxPacketTypeEnum.getEnumForCommand(inputBytes[0]);

        if (packetType != null)
        {
            switch (packetType)
            {
                default:
                    throw new UnknownPacketTypeException();
            }

//            if (returnVal == null)
//            {
//                throw new UnableToGenerateRoboxPacketException();
//            }
        }

        return returnVal;
    }
}
