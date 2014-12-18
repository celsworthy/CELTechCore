package celtech.printerControl.comms.commands.tx;

import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;

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
