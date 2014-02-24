/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class ReadReelEEPROM extends RoboxTxPacket
{

    public ReadReelEEPROM()
    {
        super(TxPacketTypeEnum.READ_REEL_EEPROM, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }
}
