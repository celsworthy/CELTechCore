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
public class SetTemperatures extends RoboxTxPacket
{

    private char[] firstPad = new char[41];
    private char[] secondPad = new char[162];

    /**
     *
     */
    public SetTemperatures()
    {
        super(TxPacketTypeEnum.SET_TEMPERATURES, false, false);
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
     * @param nozzleFirstLayerTarget
     * @param nozzleTarget
     * @param bedFirstLayerTarget
     * @param bedTarget
     * @param ambientTarget
     */
    public void setTemperatures(double nozzleFirstLayerTarget, double nozzleTarget, double bedFirstLayerTarget, double bedTarget, double ambientTarget)
    {
        StringBuilder payload = new StringBuilder();

        payload.append(String.format("%08.2f", nozzleTarget));
        payload.append(String.format("%08.2f", nozzleFirstLayerTarget));
        payload.append(String.format("%08.2f", bedTarget));
        payload.append(String.format("%08.2f", bedFirstLayerTarget));
        payload.append(String.format("%08.2f", ambientTarget));

        this.setMessagePayload(payload.toString());
    }
}
