/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


package celtech.printerControl.comms.commands.rx;

import java.text.NumberFormat;

/**
 *
 * @author ianhudson
 */
public class AckResponse extends RoboxRxPacket
{

    private final String charsetToUse = "US-ASCII";
    private byte[] errorFlags = new byte[32];
    private final int errorFlagBytes = 32;

    private NumberFormat numberFormatter = NumberFormat.getNumberInstance();
    /*
     * Error flags - starting with byte 0
     */
    private boolean sdCardError = false;
    private boolean chunkSequenceError = false;
    private boolean fileTooLargeError = false;
    private boolean gcodeLineTooLongError = false;
    private boolean usbRXError = false;
    private boolean usbTXError = false;
    private boolean badCommandError = false;
    private boolean headEEPROMError = false;
    private boolean badFirmwareFileError = false;
    private boolean flashChecksumError = false;
    private boolean gcodeBufferOverrunError = false;
    private boolean fileReadClobbered = false;
    private boolean maxGantryAdjustment = false;
    private boolean reelEEPROMError = false;

    public boolean isSdCardError()
    {
        return sdCardError;
    }

    public boolean isChunkSequenceError()
    {
        return chunkSequenceError;
    }

    public boolean isFileTooLargeError()
    {
        return fileTooLargeError;
    }

    public boolean isGcodeLineTooLongError()
    {
        return gcodeLineTooLongError;
    }

    public boolean isUsbRXError()
    {
        return usbRXError;
    }

    public boolean isUsbTXError()
    {
        return usbTXError;
    }

    public boolean isBadCommandError()
    {
        return badCommandError;
    }

    public boolean isHeadEepromError()
    {
        return headEEPROMError;
    }

    public boolean isBadFirmwareFileError()
    {
        return badFirmwareFileError;
    }

    public boolean isFlashChecksumError()
    {
        return flashChecksumError;
    }

    public boolean isGCodeBufferOverrunError()
    {
        return gcodeBufferOverrunError;
    }

    public boolean isFileReadClobbered()
    {
        return fileReadClobbered;
    }

    public boolean isMaxGantryAdjustment()
    {
        return maxGantryAdjustment;
    }

    public boolean isReelEEPROMError()
    {
        return reelEEPROMError;
    }

    public boolean isError()
    {
        return sdCardError || chunkSequenceError || fileTooLargeError
                || gcodeLineTooLongError || usbRXError || usbTXError
                || badCommandError || headEEPROMError || badFirmwareFileError
                || flashChecksumError || gcodeBufferOverrunError
                || fileReadClobbered || maxGantryAdjustment
                || reelEEPROMError;
    }

    /*
     * Errors...
     */
    public AckResponse()
    {
        super(RxPacketTypeEnum.ACK_WITH_ERRORS, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        boolean success = false;

        int byteOffset = 1;

        this.sdCardError = (byteData[byteOffset] & 1) > 0 ? true : false;
        byteOffset += 1;

        this.chunkSequenceError = (byteData[byteOffset] & 1) > 0 ? true : false;
        byteOffset += 1;

        this.fileTooLargeError = (byteData[byteOffset] & 1) > 0 ? true : false;
        byteOffset += 1;

        this.gcodeLineTooLongError = (byteData[byteOffset] & 1) > 0 ? true : false;
        byteOffset += 1;

        this.usbRXError = (byteData[byteOffset] & 1) > 0 ? true : false;
        byteOffset += 1;

        this.usbTXError = (byteData[byteOffset] & 1) > 0 ? true : false;
        byteOffset += 1;

        this.badCommandError = (byteData[byteOffset] & 1) > 0 ? true : false;
        byteOffset += 1;

        this.headEEPROMError = (byteData[byteOffset] & 1) > 0 ? true : false;
        byteOffset += 1;

        this.badFirmwareFileError = (byteData[byteOffset] & 1) > 0 ? true : false;
        byteOffset += 1;

        this.flashChecksumError = (byteData[byteOffset] & 1) > 0 ? true : false;
        byteOffset += 1;

        this.gcodeBufferOverrunError = (byteData[byteOffset] & 1) > 0 ? true : false;
        byteOffset += 1;

        this.fileReadClobbered = (byteData[byteOffset] & 1) > 0 ? true : false;
        byteOffset += 1;
        
        this.maxGantryAdjustment = (byteData[byteOffset] & 1) > 0 ? true : false;
        byteOffset += 1;
        
        this.reelEEPROMError = (byteData[byteOffset] & 1) > 0 ? true : false;
        byteOffset += 1;

        byteOffset += errorFlagBytes - 8;

        success = true;

        return success;
    }

    public String toString()
    {
        StringBuilder outputString = new StringBuilder();

        outputString.append(">>>>>>>>>>\n");
        outputString.append("Packet type:");
        outputString.append(getPacketType().name());
        outputString.append("\n");
        outputString.append("SD card error: " + isSdCardError());
        outputString.append("\n");
        outputString.append("Chunk sequence error: " + isChunkSequenceError());
        outputString.append("\n");
        outputString.append("File too large error: " + isFileTooLargeError());
        outputString.append("\n");
        outputString.append("GCode line too long error: " + isGcodeLineTooLongError());
        outputString.append("\n");
        outputString.append("USB RX error: " + isUsbRXError());
        outputString.append("\n");
        outputString.append("USB TX error: " + isUsbTXError());
        outputString.append("\n");
        outputString.append("Bad command error: " + isBadCommandError());
        outputString.append("\n");
        outputString.append("Head EEPROM error: " + isHeadEepromError());
        outputString.append("\n");
        outputString.append("Reel EEPROM error: " + isReelEEPROMError());
        outputString.append("\n");
        outputString.append("Bad firmware error: " + isBadFirmwareFileError());
        outputString.append("\n");
        outputString.append("Flash Checksum error: " + isFlashChecksumError());
        outputString.append("\n");
        outputString.append("GCode overrun error: " + isGCodeBufferOverrunError());
        outputString.append("\n");
        outputString.append(">>>>>>>>>>\n");

        return outputString.toString();
    }
}
