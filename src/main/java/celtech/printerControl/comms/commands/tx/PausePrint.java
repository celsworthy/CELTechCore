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
public class PausePrint extends RoboxTxPacket
{

    /**
     *
     */
    public PausePrint()
    {
        super(TxPacketTypeEnum.PAUSE_RESUME_PRINT, false, false);
    }

    /**
     *
     * @param byteData
     * @return
     */
    @Override
    public boolean populatePacket(byte[] byteData)
    {
        setMessagePayload(byteData);
        return false;
    }

    /**
     *
     */
    public void setPause()
    {
        this.setMessagePayload("1");
    }

    /**
     *
     */
    public void setResume()
    {
        this.setMessagePayload("0");
    }
}
