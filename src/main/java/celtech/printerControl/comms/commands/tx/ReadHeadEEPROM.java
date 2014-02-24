/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class ReadHeadEEPROM extends RoboxTxPacket
{

    public ReadHeadEEPROM()
    {
        super(TxPacketTypeEnum.READ_HEAD_EEPROM, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }
}
