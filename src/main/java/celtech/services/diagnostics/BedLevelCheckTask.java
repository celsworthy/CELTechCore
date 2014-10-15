/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.diagnostics;

import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.printerControl.model.HardwarePrinter;
import celtech.printerControl.model.PrinterException;
import celtech.services.ControllableService;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class BedLevelCheckTask extends Task<BedLevelCheckResult> implements ControllableService
{

    private final Stenographer steno = StenographerFactory.getStenographer(BedLevelCheckTask.class.getName());
    private Project project = null;
    private Filament filament = null;
    private PrintQualityEnumeration printQuality = null;
    private RoboxProfile settings = null;
    private HardwarePrinter printerToUse = null;
    private String macroName = null;

    /**
     *
     * @param printerToUse
     */
    public BedLevelCheckTask(HardwarePrinter printerToUse)
    {
        this.printerToUse = printerToUse;
    }

    @Override
    protected BedLevelCheckResult call() throws Exception
    {
        BedLevelCheckResult result = new BedLevelCheckResult();

        try
        {
            printerToUse.runMacro("Home_all");
            //Go to centre
            printerToUse.runMacro("level_gantry");
            printerToUse.runMacro("level_Y");
        } catch (PrinterException ex)
        {
            steno.error("Error levelling bed");
        }
        return result;
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

}
