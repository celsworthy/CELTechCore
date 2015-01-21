package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class ReadDebugData extends RoboxTxPacket
{

    /**
     *
     */
    public ReadDebugData()
    {
        super(TxPacketTypeEnum.READ_DEBUG_DATA, false, false);
    }

    /**
     *
     * @param byteData
     * @return
     */
    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }
}
