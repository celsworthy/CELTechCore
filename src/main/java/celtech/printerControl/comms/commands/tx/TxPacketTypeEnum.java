/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

import celtech.printerControl.comms.commands.rx.RxPacketTypeEnum;

/**
 *
 * @author ianhudson
 */
public enum TxPacketTypeEnum
{

    STATUS_REQUEST((byte) 0xB0, 1, false, RxPacketTypeEnum.STATUS_RESPONSE),
    EXECUTE_GCODE((byte) 0x95, 1, true, RxPacketTypeEnum.GCODE_RESPONSE),
    START_OF_DATA_FILE((byte) 0x90, 1, true, RxPacketTypeEnum.ACK_WITH_ERRORS),
    DATA_FILE_CHUNK((byte) 0x91, 1, true, RxPacketTypeEnum.ACK_WITH_ERRORS),
    END_OF_DATA_FILE((byte) 0x92, 1, true, RxPacketTypeEnum.ACK_WITH_ERRORS),
    INITIATE_PRINT((byte) 0x94, 17, false, RxPacketTypeEnum.ACK_WITH_ERRORS),
    ABORT_PRINT((byte) 0xFF, 1, false, RxPacketTypeEnum.ACK_WITH_ERRORS),
    PAUSE_RESUME_PRINT((byte) 0x98, 2, false, RxPacketTypeEnum.ACK_WITH_ERRORS),
    REPORT_ERRORS((byte) 0xb3, 1, false, RxPacketTypeEnum.ACK_WITH_ERRORS),
    RESET_ERRORS((byte) 0xc0, 1, false, RxPacketTypeEnum.ACK_WITH_ERRORS),
    QUERY_FIRMWARE_VERSION((byte) 0xB4, 1, false, RxPacketTypeEnum.FIRMWARE_RESPONSE),
    UPDATE_FIRMWARE((byte) 0x8f, 1, false, RxPacketTypeEnum.ACK_WITH_ERRORS),
    WRITE_PRINTER_ID((byte) 0xc1, 257, false, RxPacketTypeEnum.ACK_WITH_ERRORS),
    READ_PRINTER_ID((byte) 0xb2, 1, false, RxPacketTypeEnum.PRINTER_ID_RESPONSE),
    WRITE_REEL_EEPROM((byte) 0xa2, 1, true, RxPacketTypeEnum.ACK_WITH_ERRORS),
    WRITE_HEAD_EEPROM((byte) 0xa0, 1, true, RxPacketTypeEnum.ACK_WITH_ERRORS),
    READ_REEL_EEPROM((byte) 0xa3, 5, false, RxPacketTypeEnum.REEL_EEPROM_DATA),
    READ_HEAD_EEPROM((byte) 0xa1, 5, false, RxPacketTypeEnum.HEAD_EEPROM_DATA),
    FORMAT_REEL_EEPROM((byte) 0xf9, 1, false, RxPacketTypeEnum.ACK_WITH_ERRORS),
    FORMAT_HEAD_EEPROM((byte) 0xf8, 1, false, RxPacketTypeEnum.ACK_WITH_ERRORS),
    SET_AMBIENT_LED_COLOUR((byte) 0xc2, 7, false, RxPacketTypeEnum.ACK_WITH_ERRORS),
    SET_REEL_LED_COLOUR((byte) 0xc4, 7, false, RxPacketTypeEnum.ACK_WITH_ERRORS),
    CONTROL_REEL_LED((byte) 0xc4, 7, false, RxPacketTypeEnum.ACK_WITH_ERRORS),
    SET_TEMPERATURES((byte) 0xc3, 41, false, RxPacketTypeEnum.ACK_WITH_ERRORS),
    SET_FILAMENT_INFO((byte) 0xc5, 25, false, RxPacketTypeEnum.ACK_WITH_ERRORS);

    private final byte commandByte;
    private final int packetSize;
    private final boolean containsLengthField;
    private final RxPacketTypeEnum expectsResponse;

    private TxPacketTypeEnum(byte commandByte, int packetSize, boolean containsLengthField, RxPacketTypeEnum expectsResponse)
    {
        this.commandByte = commandByte;
        this.packetSize = packetSize;
        this.containsLengthField = containsLengthField;
        this.expectsResponse = expectsResponse;
    }

    public byte getCommandByte()
    {
        return commandByte;
    }

    public int getPacketSize()
    {
        return packetSize;
    }

    public boolean containsLengthField()
    {
        return containsLengthField;
    }

    public RxPacketTypeEnum getExpectedResponse()
    {
        return expectsResponse;
    }

    public static TxPacketTypeEnum getEnumForCommand(byte commandByte)
    {
        TxPacketTypeEnum returnVal = null;

        for (TxPacketTypeEnum packetType : TxPacketTypeEnum.values())
        {
            if (packetType.getCommandByte() == commandByte)
            {
                returnVal = packetType;
                break;
            }
        }

        return returnVal;
    }
}
