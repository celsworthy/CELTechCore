/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class SendDataFileChunk extends RoboxTxPacket
{

    public SendDataFileChunk()
    {
        super(TxPacketTypeEnum.DATA_FILE_CHUNK, true, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }

     
    
}
