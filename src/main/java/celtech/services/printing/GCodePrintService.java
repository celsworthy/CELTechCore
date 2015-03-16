package celtech.services.printing;

import celtech.printerControl.model.Printer;
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
public class GCodePrintService extends Service<GCodePrintResult> implements ControllableService
{

    private Printer printerToUse = null;
    private final StringProperty modelFileToPrint = new SimpleStringProperty();
    private final StringProperty currentPrintJobID = new SimpleStringProperty();
    private final IntegerProperty linesInGCodeFile = new SimpleIntegerProperty(1);
    private final Stenographer steno = StenographerFactory.
        getStenographer(this.getClass().getName());
    private boolean printUsingSDCard = true;
    private int startFromSequenceNumber = 0;
    private boolean canBeReprinted = true;

    /**
     *
     * @param printerToUse
     */
    public void setPrinterToUse(Printer printerToUse)
    {
        this.printerToUse = printerToUse;
    }

    private final Printer getPrinterToUse()
    {
        return printerToUse;
    }

    /**
     *
     * @param value
     */
    public final void setModelFileToPrint(String value)
    {
        modelFileToPrint.set(value);
    }

    /**
     *
     * @return
     */
    public final String getModelFileToPrint()
    {
        return modelFileToPrint.get();
    }

    /**
     *
     * @return
     */
    public final StringProperty modelFileToPrintProperty()
    {
        return modelFileToPrint;
    }

    /**
     *
     * @param value
     */
    public final void setCurrentPrintJobID(String value)
    {
        currentPrintJobID.set(value);
    }

    /**
     *
     * @return
     */
    public final String getCurrentPrintJobID()
    {
        return currentPrintJobID.get();
    }

    /**
     *
     * @return
     */
    public final StringProperty currentPrintJobIDProperty()
    {
        return currentPrintJobID;
    }

    /**
     *
     * @param value
     */
    public final void setLinesInGCodeFile(int value)
    {
        linesInGCodeFile.set(value);
    }

    /**
     *
     * @return
     */
    public final int getLinesInGCodeFile()
    {
        return linesInGCodeFile.get();
    }

    /**
     *
     * @return
     */
    public final IntegerProperty linesInGCodeFileProperty()
    {
        return linesInGCodeFile;
    }

    /**
     *
     * @param useSDCard
     */
    public void setPrintUsingSDCard(boolean useSDCard)
    {
        printUsingSDCard = useSDCard;
    }

    @Override
    protected Task<GCodePrintResult> createTask()
    {
        return new GCodePrinterTask(getPrinterToUse(), getModelFileToPrint(), getCurrentPrintJobID(),
                                    linesInGCodeFileProperty(), printUsingSDCard,
                                    startFromSequenceNumber, canBeReprinted);
    }

    /**
     *
     * @return
     */
    @Override
    public boolean cancelRun()
    {
        steno.info("Print service cancelled - job " + getCurrentPrintJobID());
        return cancel();
    }

    @Override
    public void reset()
    {
        super.reset();
    }

    public void setStartFromSequenceNumber(int startFromSequenceNumber)
    {
        this.startFromSequenceNumber = startFromSequenceNumber;
    }

    public void setThisCanBeReprinted(boolean thisJobCanBeReprinted)
    {
        canBeReprinted = thisJobCanBeReprinted;
    }
}
