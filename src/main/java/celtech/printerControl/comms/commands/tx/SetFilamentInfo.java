/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

import celtech.utils.FixedDecimalFloatFormat;
import celtech.utils.PrinterUtils;

/**
 *
 * @author ianhudson
 */
public class SetFilamentInfo extends RoboxTxPacket
{

    /**
     *
     */
    public SetFilamentInfo()
    {
        super(TxPacketTypeEnum.SET_FILAMENT_INFO, false, false);
    }

    /**
     *
     * @param byteData
     * @return
     */
    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }

    /**
     *
     * @param filamentDiameter
     * @param filamentMultiplier
     * @param feedRateMultiplier
     */
    public void setFilamentInfo(double filamentDiameter, double filamentMultiplier, double feedRateMultiplier)
    {
        StringBuilder payload = new StringBuilder();

        FixedDecimalFloatFormat decimalFloatFormatter = new FixedDecimalFloatFormat();

        payload.append(decimalFloatFormatter.format(filamentDiameter));
        payload.append(decimalFloatFormatter.format(filamentMultiplier));
        payload.append(decimalFloatFormatter.format(feedRateMultiplier));

        this.setMessagePayload(payload.toString());
    }
}
