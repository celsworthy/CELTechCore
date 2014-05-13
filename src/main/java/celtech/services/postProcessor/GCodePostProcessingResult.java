/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.postProcessor;

import celtech.gcodetranslator.RoboxiserResult;
import celtech.printerControl.Printer;

/**
 *
 * @author ianhudson
 */
public class GCodePostProcessingResult
{
    private String printJobUUID = null;
    private String outputFilename = null;
    private Printer printerToUse = null;
    private RoboxiserResult result = null;

    public GCodePostProcessingResult(String printJobUUID, String outputFilename, Printer printerToUse, RoboxiserResult result)
    {
        this.printJobUUID = printJobUUID;
        this.outputFilename = outputFilename;
        this.printerToUse = printerToUse;
        this.result = result;
    }

    public String getPrintJobUUID()
    {
        return printJobUUID;
    }

    public String getOutputFilename()
    {
        return outputFilename;
    }

    public Printer getPrinterToUse()
    {
        return printerToUse;
    }

    public RoboxiserResult getRoboxiserResult()
    {
        return result;
    }

    public void setRoboxiserResult(RoboxiserResult result)
    {
        this.result = result;
    }
}
