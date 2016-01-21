package celtech.printerControl.comms.commands.tx;

import celtech.comms.remote.TxPacketTypeEnum;
import celtech.comms.remote.RoboxTxPacket;

/**
 *
 * @author ianhudson
 */
public class ReadSendFileReport extends RoboxTxPacket
{

    /**
     *
     */
    public ReadSendFileReport()
    {
        super(TxPacketTypeEnum.READ_SEND_FILE_REPORT, false, false);
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
