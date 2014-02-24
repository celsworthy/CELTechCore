/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class SendDataFileEnd extends RoboxTxPacket
{
    public SendDataFileEnd()
    {
        super(TxPacketTypeEnum.END_OF_DATA_FILE, true, true);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }
}
