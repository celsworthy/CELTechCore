/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class SendGCodeRequest extends RoboxTxPacket
{

    public SendGCodeRequest()
    {
        super(TxPacketTypeEnum.EXECUTE_GCODE, false, true);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }
}
