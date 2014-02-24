/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;


/**
 *
 * @author ianhudson
 */
public class ReadPrinterID extends RoboxTxPacket
{

    public ReadPrinterID()
    {
        super(TxPacketTypeEnum.READ_PRINTER_ID, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }
}
