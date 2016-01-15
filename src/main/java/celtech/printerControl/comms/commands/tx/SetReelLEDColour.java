/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

import javafx.scene.paint.Color;

/**
 *
 * @author ianhudson
 */
public class SetReelLEDColour extends RoboxTxPacket
{

    /**
     *
     */
    public SetReelLEDColour()
    {
        super(TxPacketTypeEnum.SET_REEL_LED_COLOUR, false, false);
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
     * @param colour
     */
    public void setLEDColour(Color colour)
    {
        StringBuffer payload = new StringBuffer();

        int redValue = (int) (255 * colour.getRed());
        String redString = String.format("%02X", redValue);
        int greenValue = (int) (255 * colour.getGreen());
        String greenString = String.format("%02X", greenValue);
        int blueValue = (int) (255 * colour.getBlue());
        String blueString = String.format("%02X", blueValue);

        payload.append(redString);
        payload.append(greenString);
        payload.append(blueString);

        this.setMessagePayload(payload.toString());
    }
}
