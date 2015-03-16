/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class ControlReelLED extends RoboxTxPacket
{

    /**
     *
     */
    public ControlReelLED()
    {
        super(TxPacketTypeEnum.CONTROL_REEL_LED, false, false);
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
     * @param on
     */
    public void setLEDStatus(boolean on)
    {
        StringBuffer payload = new StringBuffer();
        String redString = "00";
        String greenString = "00";
        String blueString = "00";

        if (on)
        {
            redString = "FF";
            greenString = "FF";
            blueString = "FF";
        }

        payload.append(redString);
        payload.append(greenString);
        payload.append(blueString);

        this.setMessagePayload(payload.toString());
    }
}
