/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.rx;

import celtech.printerControl.comms.commands.PrinterIDDataStructure;
import java.io.UnsupportedEncodingException;
import javafx.scene.paint.Color;

/**
 *
 * @author ianhudson
 */
public class PrinterIDResponse extends RoboxRxPacket
{

    private final String charsetToUse = "US-ASCII";

    private String model;
    private String edition;
    private String weekOfManufacture;
    private String yearOfManufacture;
    private String poNumber;
    private String serialNumber;
    private String checkByte;
    private String printerFriendlyName;
    private Color printerColour;

    /**
     *
     */
    public PrinterIDResponse()
    {
        super(RxPacketTypeEnum.PRINTER_ID_RESPONSE, false, false);
    }

    /**
     *
     * @param byteData
     * @return
     */
    @Override
    public boolean populatePacket(byte[] byteData)
    {
        boolean success = false;

        try
        {
            int byteOffset = 1;

            this.model = new String(byteData, byteOffset, PrinterIDDataStructure.modelBytes, charsetToUse);
            this.model = this.model.trim();
            byteOffset += PrinterIDDataStructure.modelBytes;

            this.edition = new String(byteData, byteOffset, PrinterIDDataStructure.editionBytes, charsetToUse);
            this.edition = this.edition.trim();
            byteOffset += PrinterIDDataStructure.editionBytes;

            this.weekOfManufacture = new String(byteData, byteOffset, PrinterIDDataStructure.weekOfManufactureBytes, charsetToUse);
            this.weekOfManufacture = this.weekOfManufacture.trim();
            byteOffset += PrinterIDDataStructure.weekOfManufactureBytes;

            this.yearOfManufacture = new String(byteData, byteOffset, PrinterIDDataStructure.yearOfManufactureBytes, charsetToUse);
            this.yearOfManufacture = this.yearOfManufacture.trim();
            byteOffset += PrinterIDDataStructure.yearOfManufactureBytes;

            this.poNumber = new String(byteData, byteOffset, PrinterIDDataStructure.poNumberBytes, charsetToUse);
            this.poNumber = this.poNumber.trim();
            byteOffset += PrinterIDDataStructure.poNumberBytes;

            this.serialNumber = new String(byteData, byteOffset, PrinterIDDataStructure.serialNumberBytes, charsetToUse);
            this.serialNumber = this.serialNumber.trim();
            byteOffset += PrinterIDDataStructure.serialNumberBytes;

            this.checkByte = new String(byteData, byteOffset, PrinterIDDataStructure.checkByteBytes, charsetToUse);
            this.checkByte = this.checkByte.trim();
            byteOffset += PrinterIDDataStructure.checkByteBytes;

            byteOffset += 41;

            this.printerFriendlyName = new String(byteData, byteOffset, PrinterIDDataStructure.printerFriendlyNameBytes, charsetToUse);
            this.printerFriendlyName = this.printerFriendlyName.trim();
            byteOffset += PrinterIDDataStructure.printerFriendlyNameBytes;

            byteOffset += 162;

            try
            {
                String redDigits = new String(byteData, byteOffset, PrinterIDDataStructure.colourBytes, charsetToUse);
                int redIntValue = Integer.parseInt(redDigits, 16);
                double redValue = (double) redIntValue / 255;
                byteOffset += PrinterIDDataStructure.colourBytes;

                String greenDigits = new String(byteData, byteOffset, PrinterIDDataStructure.colourBytes, charsetToUse);
                int greenIntValue = Integer.parseInt(greenDigits, 16);
                double greenValue = (double) greenIntValue / 255;
                byteOffset += PrinterIDDataStructure.colourBytes;

                String blueDigits = new String(byteData, byteOffset, PrinterIDDataStructure.colourBytes, charsetToUse);
                int blueIntValue = Integer.parseInt(blueDigits, 16);
                double blueValue = (double) blueIntValue / 255;
                byteOffset += PrinterIDDataStructure.colourBytes;

                printerColour = new Color(redValue, greenValue, blueValue, 1);
            } catch (NumberFormatException ex)
            {
                steno.error("Failed to convert colour information");
                printerColour = Color.WHITE;
            }

            success = true;

        } catch (UnsupportedEncodingException ex)
        {
            steno.error("Failed to convert byte array to Printer ID Response");
        }

        return success;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        StringBuilder outputString = new StringBuilder();

        outputString.append(">>>>>>>>>>\n");
        outputString.append("Packet type:");
        outputString.append(getPacketType().name());
        outputString.append("\n");
        outputString.append("ID: " + getModel());
        outputString.append("\n");
        outputString.append(">>>>>>>>>>\n");

        return outputString.toString();
    }

    /**
     *
     * @return
     */
    public Color getPrinterColour()
    {
        return printerColour;
    }

    /**
     *
     * @return
     */
    public String getModel()
    {
        return model;
    }

    /**
     *
     * @return
     */
    public String getEdition()
    {
        return edition;
    }

    /**
     *
     * @return
     */
    public String getWeekOfManufacture()
    {
        return weekOfManufacture;
    }

    /**
     *
     * @return
     */
    public String getYearOfManufacture()
    {
        return yearOfManufacture;
    }

    /**
     *
     * @return
     */
    public String getPoNumber()
    {
        return poNumber;
    }

    /**
     *
     * @return
     */
    public String getSerialNumber()
    {
        return serialNumber;
    }

    /**
     *
     * @return
     */
    public String getCheckByte()
    {
        return checkByte;
    }

    /**
     *
     * @return
     */
    public String getPrinterFriendlyName()
    {
        return printerFriendlyName;
    }

}
