package celtech.printerControl.comms.commands.tx;

import celtech.utils.FixedDecimalFloatFormat;

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
     * @param filamentDiameterE
     * @param filamentMultiplierE
     * @param feedRateMultiplierE
     * @param filamentDiameterD
     * @param filamentMultiplierD
     * @param feedRateMultiplierD
     */
    public void setFilamentInfo(double filamentDiameterE, double filamentMultiplierE, double feedRateMultiplierE,
        double filamentDiameterD, double filamentMultiplierD, double feedRateMultiplierD)
    {
        StringBuilder payload = new StringBuilder();

        FixedDecimalFloatFormat decimalFloatFormatter = new FixedDecimalFloatFormat();

        payload.append(decimalFloatFormatter.format(filamentDiameterE));
        payload.append(decimalFloatFormatter.format(filamentMultiplierE));
        payload.append(decimalFloatFormatter.format(feedRateMultiplierE));
        payload.append(decimalFloatFormatter.format(filamentDiameterD));
        payload.append(decimalFloatFormatter.format(filamentMultiplierD));
        payload.append(decimalFloatFormatter.format(feedRateMultiplierD));

        this.setMessagePayload(payload.toString());
    }
}
