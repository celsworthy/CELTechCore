/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class UpdateFirmware extends RoboxTxPacket
{

    public UpdateFirmware()
    {
        super(TxPacketTypeEnum.UPDATE_FIRMWARE, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }
}
