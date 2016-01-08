package celtech.printerControl.comms.commands.tx;

import java.io.UnsupportedEncodingException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public abstract class RoboxTxPacket
{

    private TxPacketTypeEnum packetType = null;

    /**
     *
     */
    protected String messagePayload = null;
    private int sequenceNumber = -1;
    private boolean includeSequenceNumber = false;
    private final int sequenceNumberLength = 8;
    private boolean includeCharsOfDataInOutput = false;
    private final int charsOfDataLength = 4;

    /**
     *
     */
    protected static Stenographer steno = StenographerFactory.getStenographer(RoboxTxPacket.class.getName());

    /**
     *
     * @param packetType
     * @param includeSequenceNumber
     * @param includeCharsOfDataInOutput
     */
    public RoboxTxPacket(TxPacketTypeEnum packetType, boolean includeSequenceNumber, boolean includeCharsOfDataInOutput)
    {
        this.packetType = packetType;
        this.includeSequenceNumber = includeSequenceNumber;
        this.includeCharsOfDataInOutput = includeCharsOfDataInOutput;
    }

    /**
     *
     * @return
     */
    public TxPacketTypeEnum getPacketType()
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

        String finalPayload = constructPayloadString();

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

    public String constructPayloadString()
    {
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
        return finalPayload.toString();
    }

    /**
     *
     * @param byteData
     * @return
     */
    public abstract boolean populatePacket(byte[] byteData);
}
