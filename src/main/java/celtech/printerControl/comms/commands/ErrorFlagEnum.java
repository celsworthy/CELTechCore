/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands;

/**
 *
 * @author ianhudson
 */
public enum ErrorFlagEnum
{
    NO_HEAD(0),
    NO_FILAMENT_REEL(1),
    OUT_OF_FILAMENT(2),
    FILAMENT(3),
    SD_CARD_FAULT(13),
    NOZZLE_THERMISTOR_FAULT(14),
    BED_THERMISTOR_FAULT(15),
    AMBIENT_THERMISTOR_FAULT(16),
    FIRMWARE_CHECKSUM_FAULT(17),
    LID_OPENED_DURING_PRINTING(24),
    CHUNK_SEQUENCE_ERROR(25),
    FILE_TOO_LARGE(26);
    
    private final int bytePosition;

    private ErrorFlagEnum(int bytePosition)
    {
        this.bytePosition = bytePosition;
    }
    
    public int getBytePosition()
    {
        return bytePosition;
    }
}
