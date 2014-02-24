/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class AbortPrint extends RoboxTxPacket
{

    public AbortPrint()
    {
        super(TxPacketTypeEnum.ABORT_PRINT, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }
}
