package celtech.printerControl;

/**
 *
 * @author Ian
 * 
 * This enumeration holds the exclusive operations that can be carried out by the printer.
 * Every operation that is singular and that may not be carried out in parallel with other operations must be present.
 */
public enum NewPrinterStatus
{
    IDLE, REMOVING_HEAD, PRINTING
}
