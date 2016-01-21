/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

import celtech.comms.remote.TxPacketTypeEnum;
import celtech.comms.remote.RoboxTxPacket;

/**
 *
 * @author ianhudson
 */
public class UpdateFirmware extends RoboxTxPacket
{

    /**
     *
     */
    public UpdateFirmware()
    {
        super(TxPacketTypeEnum.UPDATE_FIRMWARE, false, false);
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
