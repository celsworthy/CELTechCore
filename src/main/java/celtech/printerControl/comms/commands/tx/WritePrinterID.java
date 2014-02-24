/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

import celtech.printerControl.comms.commands.PrinterIDDataStructure;
import javafx.scene.paint.Color;

/**
 *
 * @author ianhudson
 */
public class WritePrinterID extends RoboxTxPacket
{

    private char[] firstPad = new char[41];
    private char[] secondPad = new char[162];

    public WritePrinterID()
    {
        super(TxPacketTypeEnum.WRITE_PRINTER_ID, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }

    public void setIDAndColour(String model, String edition, String weekOfManufacture, String yearOfManufacture, String poNumber, String serialNumber, String checkByte, String printerFriendlyName, Color colour)
    {
        //The ID is in the first 200 characters
        //The colour is stored in 6 bytes at the end - eg FF FF FF
        StringBuffer payload = new StringBuffer();

        payload.append(String.format("%1$-5s", model));
        payload.append(String.format("%1$-2s", edition));
        payload.append(String.format("%1$-2s", weekOfManufacture));
        payload.append(String.format("%1$-2s", yearOfManufacture));
        payload.append(String.format("%1$-7s", poNumber));
        payload.append(String.format("%1$-4s", serialNumber));
        payload.append(String.format("%1$1s", checkByte));
        payload.append(firstPad);
        payload.append(String.format("%1$24s", printerFriendlyName));
        payload.append(secondPad);

        int redValue = (int) (255 * colour.getRed());
        String redString = String.format("%02X", redValue);
        int greenValue = (int) (255 * colour.getGreen());
        String greenString = String.format("%02X", greenValue);
        int blueValue = (int) (255 * colour.getBlue());
        String blueString = String.format("%02X", blueValue);

        payload.append(redString);
        payload.append(greenString);
        payload.append(blueString);

        steno.info("Outputting string of " + payload.length());
        this.setMessagePayload(payload.toString());
    }
}
