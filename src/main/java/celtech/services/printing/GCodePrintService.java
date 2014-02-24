/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.printing;

import celtech.printerControl.Printer;
import celtech.services.ControllableService;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class GCodePrintService extends Service<Boolean> implements ControllableService
{

    private Printer printerToUse = null;
    private final StringProperty modelFileToPrint = new SimpleStringProperty();
    private final StringProperty currentPrintJobID = new SimpleStringProperty();
    private final IntegerProperty linesInGCodeFile = new SimpleIntegerProperty(1);
    private final Stenographer steno = StenographerFactory.getStenographer(this.getClass().getName());
    private boolean printUsingSDCard = true;
    
    public void setPrinterToUse(Printer printerToUse)
    {
        this.printerToUse = printerToUse;
    }
    
    private final Printer getPrinterToUse()
    {
        return printerToUse;
    }

    public final void setModelFileToPrint(String value)
    {
        modelFileToPrint.set(value);
    }

    public final String getModelFileToPrint()
    {
        return modelFileToPrint.get();
    }

    public final StringProperty modelFileToPrintProperty()
    {
        return modelFileToPrint;
    }

    public final void setCurrentPrintJobID(String value)
    {
        currentPrintJobID.set(value);
    }

    public final String getCurrentPrintJobID()
    {
        return currentPrintJobID.get();
    }

    public final StringProperty currentPrintJobIDProperty()
    {
        return currentPrintJobID;
    }

    public final void setLinesInGCodeFile(int value)
    {
        linesInGCodeFile.set(value);
    }

    public final int getLinesInGCodeFile()
    {
        return linesInGCodeFile.get();
    }

    public final IntegerProperty linesInGCodeFileProperty()
    {
        return linesInGCodeFile;
    }
    
    public void setPrintUsingSDCard(boolean useSDCard)
    {
        printUsingSDCard = useSDCard;
    }

    @Override
    protected Task<Boolean> createTask()
    {
        return new GCodePrinterTask(getPrinterToUse(), getModelFileToPrint(), getCurrentPrintJobID(), linesInGCodeFileProperty(), printUsingSDCard);
    }

    @Override
    public boolean cancelRun()
    {
        return cancel();
    }
}
