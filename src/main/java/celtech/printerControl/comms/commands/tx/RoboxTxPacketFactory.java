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

    /**
     *
     */
    public final static byte commandByteMask = (byte) 0x80;

    private RoboxTxPacketFactory()
    {
    }

    /**
     *
     * @param packetType
     * @return
     */
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
                case REPORT_ERRORS:
                    returnVal = new ReportErrors();
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
                case FORMAT_REEL_0_EEPROM:
                    returnVal = new FormatReel0EEPROM();
                    break;
                case FORMAT_REEL_1_EEPROM:
                    returnVal = new FormatReel1EEPROM();
                    break;
                case READ_HEAD_EEPROM:
                    returnVal = new ReadHeadEEPROM();
                    break;
                case READ_REEL_0_EEPROM:
                    returnVal = new ReadReel0EEPROM();
                    break;
                case READ_REEL_1_EEPROM:
                    returnVal = new ReadReel1EEPROM();
                    break;
                case WRITE_HEAD_EEPROM:
                    returnVal = new WriteHeadEEPROM();
                    break;
                case WRITE_REEL_0_EEPROM:
                    returnVal = new WriteReel0EEPROM();
                    break;
                case WRITE_REEL_1_EEPROM:
                    returnVal = new WriteReel1EEPROM();
                    break;
                case SET_AMBIENT_LED_COLOUR:
                    returnVal = new SetAmbientLEDColour();
                    break;
                case SET_REEL_LED_COLOUR:
                    returnVal = new SetReelLEDColour();
                    break;
                case CONTROL_REEL_LED:
                    returnVal = new ControlReelLED();
                    break;
                case SET_TEMPERATURES:
                    returnVal = new SetTemperatures();
                    break;
                case SET_FILAMENT_INFO:
                    returnVal = new SetFilamentInfo();
                    break;
                case LIST_FILES:
                    returnVal = new ListFiles();
                    break;
                case READ_HOURS_COUNTER:
                    returnVal = new ReadHoursCounter();
                    break;
                case READ_DEBUG_DATA:
                    returnVal = new ReadDebugData();
                    break;
                case READ_SEND_FILE_REPORT:
                    returnVal = new ReadSendFileReport();
                    break;
                default:
                    break;
            }
        }

        return returnVal;
    }

    /**
     *
     * @param inputBytes
     * @return
     * @throws InvalidCommandByteException
     * @throws UnableToGenerateRoboxPacketException
     * @throws UnknownPacketTypeException
     */
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
        }

        return returnVal;
    }
}
