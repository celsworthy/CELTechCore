package celtech.printerControl.comms.commands.tx;

import celtech.utils.FixedDecimalFloatFormat;

/**
 *
 * @author ianhudson
 */
public class SetFeedRateMultiplier extends RoboxTxPacket
{

    /**
     *
     */
    public SetFeedRateMultiplier()
    {
        super(TxPacketTypeEnum.SET_FEED_RATE_MULTIPLIER, false, false);
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
     * @param feedRateMultiplier
     */
    public void setFeedRateMultiplier(double feedRateMultiplier)
    {
        StringBuilder payload = new StringBuilder();

        FixedDecimalFloatFormat decimalFloatFormatter = new FixedDecimalFloatFormat();

        payload.append(decimalFloatFormatter.format(feedRateMultiplier));

        this.setMessagePayload(payload.toString());
    }
}
