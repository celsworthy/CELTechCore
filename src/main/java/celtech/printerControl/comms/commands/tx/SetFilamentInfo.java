/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class SetFilamentInfo extends RoboxTxPacket
{
    public SetFilamentInfo()
    {
        super(TxPacketTypeEnum.SET_FILAMENT_INFO, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }

    public void setFilamentInfo(double filamentDiameter, double filamentMultiplier, double feedRateMultiplier)
    {
        StringBuilder payload = new StringBuilder();

        payload.append(String.format("%08.2f", filamentDiameter));
        payload.append(String.format("%08.2f", filamentMultiplier));
        payload.append(String.format("%08.2f", feedRateMultiplier));

        this.setMessagePayload(payload.toString());
    }
}
