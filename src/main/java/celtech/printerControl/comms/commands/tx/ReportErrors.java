/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class ReportErrors extends RoboxTxPacket
{

    public ReportErrors()
    {
        super(TxPacketTypeEnum.REPORT_ERRORS, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }
}
