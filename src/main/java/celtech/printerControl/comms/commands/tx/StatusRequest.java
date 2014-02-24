/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class StatusRequest extends RoboxTxPacket
{

    public StatusRequest()
    {
        super(TxPacketTypeEnum.STATUS_REQUEST, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }
}
