/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class PausePrint extends RoboxTxPacket
{
    public PausePrint()
    {
        super(TxPacketTypeEnum.PAUSE_RESUME_PRINT, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }

    public void setPause()
    {
        this.setMessagePayload("1");
    }

    public void setResume()
    {
        this.setMessagePayload("0");
    }
}
