package celtech.configuration;

import celtech.printerControl.PrinterStatus;

/**
 *
 * @author Ian
 */
public enum Macro
{
    CANCEL_PRINT("abort_print", PrinterStatus.CANCELLING);

    private String macroFileName;
    private PrinterStatus status;

    private Macro(String macroFileName, PrinterStatus status)
    {
        this.macroFileName = macroFileName;
        this.status = status;
    }
    
    public String getMacroFileName()
    {
        return macroFileName;
    }

    public PrinterStatus getStateWhilstRunning()
    {
        return status;
    }
}
