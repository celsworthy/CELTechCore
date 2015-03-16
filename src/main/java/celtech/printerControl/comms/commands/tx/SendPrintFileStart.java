package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class SendPrintFileStart extends RoboxTxPacket
{

    /**
     *
     */
    public SendPrintFileStart()
    {
        super(TxPacketTypeEnum.SEND_PRINT_FILE_START, false, false);
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
