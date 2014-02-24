/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.rx;

/**
 *
 * @author ianhudson
 */
public enum RxPacketTypeEnum
{
    STATUS_RESPONSE((byte)0xE1, 157, false, 0),
    FIRMWARE_RESPONSE((byte)0xE4, 9, false, 0),
    ACK_WITH_ERRORS((byte)0xE3, 33, false, 0),
    PRINTER_ID_RESPONSE((byte)0xE5, 257, false, 0),
    REEL_EEPROM_DATA((byte)0xE6, 193, false, 0),
    HEAD_EEPROM_DATA((byte)0xE2, 193, false, 0),
    GCODE_RESPONSE((byte)0xE7, 5, true, 4);
    
    private final byte commandByte;
    private final int packetSize;
    private final boolean containsLengthField;
    private final int lengthFieldSize;

    private RxPacketTypeEnum(byte commandByte, int packetSize, boolean containsLengthField, int lengthFieldSize)
    {
        this.commandByte = commandByte;
        this.packetSize = packetSize;
        this.containsLengthField = containsLengthField;
        this.lengthFieldSize = lengthFieldSize;
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
    
    public int getLengthFieldSize()
    {
        return lengthFieldSize;
    }
    
    public static RxPacketTypeEnum getEnumForCommand(byte commandByte)
    {
        RxPacketTypeEnum returnVal = null;
        
        for (RxPacketTypeEnum packetType : RxPacketTypeEnum.values())
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
