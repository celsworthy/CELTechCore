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
public class SetAmbientLEDColour extends RoboxTxPacket
{

    /**
     *
     */
    public SetAmbientLEDColour()
    {
        super(TxPacketTypeEnum.SET_AMBIENT_LED_COLOUR, false, false);
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
