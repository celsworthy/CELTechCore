/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.printing;

/**
 *
 * @author Ian
 */
public class GCodePrintResult
{
    private boolean success = false;
    private boolean isMacro = false;
    private String printJobID = null;

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public boolean isIsMacro()
    {
        return isMacro;
    }

    public void setIsMacro(boolean isMacro)
    {
        this.isMacro = isMacro;
    }

    public String getPrintJobID()
    {
        return printJobID;
    }

    public void setPrintJobID(String printJobID)
    {
        this.printJobID = printJobID;
    }
}
