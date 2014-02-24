/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.printerControl.comms;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterID
{
    private String printerID = "";

    public PrinterID()
    {
    }

    public String getPrinterID()
    {
        return printerID;
    }

    public void setPrinterID(String printerID)
    {
        this.printerID = printerID;
    }
    
    public String toString()
    {
        return printerID;
    }
}
