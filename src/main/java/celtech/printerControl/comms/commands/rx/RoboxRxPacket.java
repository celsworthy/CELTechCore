/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.rx;

import java.io.UnsupportedEncodingException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public abstract class RoboxRxPacket
{

    private RxPacketTypeEnum packetType = null;
    private String messagePayload = null;
    private int sequenceNumber = -1;
    private boolean includeSequenceNumber = false;
    private final int sequenceNumberLength = 8;
    private boolean includeCharsOfDataInOutput = false;
    private final int charsOfDataLength = 4;

    /**
     *
     */
    protected static Stenographer steno = StenographerFactory.getStenographer(RoboxRxPacket.class.getName());

    /**
     *
     * @param packetType
     * @param includeSequenceNumber
     * @param includeCharsOfDataInOutput
     */
    public RoboxRxPacket(RxPacketTypeEnum packetType, boolean includeSequenceNumber, boolean includeCharsOfDataInOutput)
    {
        this.packetType = packetType;
        this.includeSequenceNumber = includeSequenceNumber;
        this.includeCharsOfDataInOutput = includeCharsOfDataInOutput;
    }

    /**
     *
     * @return
     */
    public RxPacketTypeEnum getPacketType()
    {
        return packetType;
    }

    /**
     *
     * @return
     */
    public String getMessageData()
    {
        return messagePayload;
    }

    /**
     *
     * @param messagePayload
     */
    public void setMessagePayload(String messagePayload)
    {
        this.messagePayload = messagePayload;
    }
    
    /**
     *
     * @param sequenceNumber
     */
    public void setSequenceNumber(int sequenceNumber)
    {
        this.sequenceNumber = sequenceNumber;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        StringBuilder output = new StringBuilder();

        output.append("\n>>>---<<<\n");
        output.append("Command code: " + String.format("0x%02X", packetType.getCommandByte()));
        output.append("\n");
        output.append("Message:");
        output.append(messagePayload);
        output.append("\n");
        output.append(">>>---<<<\n");

        return output.toString();
    }

    /**
     *
     * @return
     */
    public byte[] toByteArray()
    {
        byte[] outputArray = null;

        int bufferSize = 1; // 1 for the command

        if (includeSequenceNumber)
        {
            bufferSize += sequenceNumberLength;
        }

        if (includeCharsOfDataInOutput && messagePayload != null)
        {
            bufferSize += charsOfDataLength;
        }

        if (messagePayload != null)
        {
            bufferSize += messagePayload.length();
        }

        outputArray = new byte[bufferSize];

        outputArray[0] = packetType.getCommandByte();

        StringBuilder finalPayload = new StringBuilder();

        if (includeSequenceNumber)
        {
            finalPayload.append(String.format("%08X", sequenceNumber));
        }

        if (includeCharsOfDataInOutput && messagePayload != null)
        {
            finalPayload.append(String.format("%04X", messagePayload.length()));
        }

        if (messagePayload != null)
        {
            finalPayload.append(messagePayload);
        }

        if (bufferSize > 1)
        {
            try
            {
                byte[] payloadBytes = finalPayload.toString().getBytes("US-ASCII");
                //TODO - replace this with a ByteBuffer or equivalent
                for (int i = 1; i <= payloadBytes.length; i++)
                {
                    outputArray[i] = payloadBytes[i - 1];
                }
            } catch (UnsupportedEncodingException ex)
            {
                steno.error("Couldn't encode message for output");
            }
        }

        return outputArray;
    }

    /**
     *
     * @param byteData
     * @param requiredFirmwareVersion
     * @return
     */
    public abstract boolean populatePacket(byte[] byteData, float requiredFirmwareVersion);
    
    /**
     * 
     * @param requiredFirmwareVersion
     * @return 
     */
    public abstract int packetLength(float requiredFirmwareVersion);
}
