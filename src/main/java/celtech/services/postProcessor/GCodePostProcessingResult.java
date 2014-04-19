/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.postProcessor;

import celtech.services.slicer.*;
import celtech.appManager.Project;
import celtech.configuration.FilamentContainer;
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
    private boolean success = false;

    public GCodePostProcessingResult(String printJobUUID, String outputFilename, Printer printerToUse, boolean success)
    {
        this.printJobUUID = printJobUUID;
        this.outputFilename = outputFilename;
        this.printerToUse = printerToUse;
        this.success = success;
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

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }
}
