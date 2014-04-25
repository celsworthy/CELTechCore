/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.services.calibration;

import celtech.printerControl.Printer;
import celtech.services.ControllableService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *
 * @author Ian
 */
public class CalibrateBService extends Service<Boolean> implements ControllableService
{
    private Printer printerToUse = null;
    
    public void setPrinterToUse(Printer printer)
    {
        this.printerToUse = printer;
    }
    
    @Override
    protected Task<Boolean> createTask()
    {
        return new CalibrateBTask(printerToUse);
    }

    @Override
    public boolean cancelRun()
    {
        return cancel();
    }
}
