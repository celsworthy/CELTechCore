/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.firmware;

import celtech.printerControl.PrinterImpl;
import celtech.services.ControllableService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *
 * @author ianhudson
 */
public class FirmwareLoadService extends Service<Integer> implements ControllableService
{

    private StringProperty firmwareFileToLoad = new SimpleStringProperty();
    private PrinterImpl printerToUse = null;

    /**
     *
     * @param value
     */
    public final void setFirmwareFileToLoad(String value)
    {
        firmwareFileToLoad.set(value);
    }

    /**
     *
     * @return
     */
    public final String getFirmwareFileToLoad()
    {
        return firmwareFileToLoad.get();
    }

    /**
     *
     * @return
     */
    public final StringProperty firmwareFileToLoadProperty()
    {
        return firmwareFileToLoad;
    }

    @Override
    protected Task<Integer> createTask()
    {
        return new FirmwareLoadTask(getFirmwareFileToLoad(), printerToUse);
    }

    /**
     *
     * @return
     */
    @Override
    public boolean cancelRun()
    {
        return cancel();
    }

    /**
     *
     * @param printerToUse
     */
    public void setPrinterToUse(PrinterImpl printerToUse)
    {
        this.printerToUse = printerToUse;
    }
}
