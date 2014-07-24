/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

import static celtech.printerControl.comms.commands.ColourStringConverter.colourToString;
import celtech.printerControl.comms.commands.PrinterIDDataStructure;
import celtech.printerControl.comms.commands.StringToBase64Encoder;
import java.io.UnsupportedEncodingException;
import javafx.scene.paint.Color;

/**
 *
 * @author ianhudson
 */
public class WritePrinterID extends RoboxTxPacket
{

    public static final int BYTES_FOR_NAME = PrinterIDDataStructure.printerFriendlyNameBytes;
    public static final int BYTES_FOR_FIRST_PAD = 41;
    public static final int BYTES_FOR_SECOND_PAD = 186 - BYTES_FOR_NAME;

    private final char[] firstPad = new char[BYTES_FOR_FIRST_PAD];
    private final char[] secondPad = new char[BYTES_FOR_SECOND_PAD];

    /**
     *
     */
    public WritePrinterID()
    {
        super(TxPacketTypeEnum.WRITE_PRINTER_ID, false, false);
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
     * @param model
     * @param edition
     * @param weekOfManufacture
     * @param yearOfManufacture
     * @param poNumber
     * @param serialNumber
     * @param checkByte
     * @param printerFriendlyName
     * @param colour
     */
    public void setIDAndColour(String model, String edition,
        String weekOfManufacture, String yearOfManufacture, String poNumber,
        String serialNumber, String checkByte, String printerFriendlyName,
        Color colour)
    {

        try
        {
            printerFriendlyName = StringToBase64Encoder.encode(printerFriendlyName,
                                                     BYTES_FOR_NAME);
        } catch (UnsupportedEncodingException ex)
        {
            steno.error("Couldn't encode printer name: " + printerFriendlyName);
            printerFriendlyName = "";
        }

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
        payload.append(String.format("%1$" + BYTES_FOR_NAME + "s",
                                     printerFriendlyName));
        payload.append(secondPad);

        payload.append(colourToString(colour));

        steno.info("Outputting string of length " + payload.length());
        this.setMessagePayload(payload.toString());
    }
}
