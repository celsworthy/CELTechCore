package celtech.printerControl.comms.commands.tx;

import celtech.utils.FixedDecimalFloatFormat;

/**
 *
 * @author ianhudson
 */
public class SetDFilamentInfo extends RoboxTxPacket
{

    /**
     *
     */
    public SetDFilamentInfo()
    {
        super(TxPacketTypeEnum.SET_D_FILAMENT_INFO, false, false);
    }

    /**
     *
     * @param byteData
     * @return
     */
    @Override
    public boolean populatePacket(byte[] byteData)
    {
        setMessagePayload(byteData);
        return false;
    }

    /**
     *
     * @param filamentDiameter
     * @param filamentMultiplier
     */
    public void setFilamentInfo(double filamentDiameter, double filamentMultiplier)
    {
        StringBuilder payload = new StringBuilder();

        FixedDecimalFloatFormat decimalFloatFormatter = new FixedDecimalFloatFormat();

        payload.append(decimalFloatFormatter.format(filamentDiameter));
        payload.append(decimalFloatFormatter.format(filamentMultiplier));

        this.setMessagePayload(payload.toString());
    }
}
