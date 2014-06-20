/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

import celtech.utils.FixedDecimalFloatFormat;

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
        
        FixedDecimalFloatFormat decimalFloatFormatter = new FixedDecimalFloatFormat();

        payload.append(decimalFloatFormatter.format(nozzleTarget));
        payload.append(decimalFloatFormatter.format(nozzleFirstLayerTarget));
        payload.append(decimalFloatFormatter.format(bedTarget));
        payload.append(decimalFloatFormatter.format(bedFirstLayerTarget));
        payload.append(decimalFloatFormatter.format(ambientTarget));

        this.setMessagePayload(payload.toString());
    }
}
