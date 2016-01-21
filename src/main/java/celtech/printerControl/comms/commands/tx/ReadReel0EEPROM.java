package celtech.printerControl.comms.commands.tx;

import celtech.comms.remote.TxPacketTypeEnum;
import celtech.comms.remote.RoboxTxPacket;

/**
 *
 * @author ianhudson
 */
public class ReadReel0EEPROM extends RoboxTxPacket
{

    /**
     *
     */
    public ReadReel0EEPROM()
    {
        super(TxPacketTypeEnum.READ_REEL_0_EEPROM, false, false);
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
