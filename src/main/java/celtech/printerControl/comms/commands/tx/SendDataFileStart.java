/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class SendDataFileStart extends RoboxTxPacket
{

    public SendDataFileStart()
    {
        super(TxPacketTypeEnum.START_OF_DATA_FILE, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }
}
