package celtech.services.diagnostics;

import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.configuration.fileRepresentation.SlicerParameters;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.services.ControllableService;
import celtech.services.slicer.PrintQualityEnumeration;
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
    private SlicerParameters settings = null;
    private Printer printerToUse = null;
    private String macroName = null;

    /**
     *
     * @param printerToUse
     */
    public BedLevelCheckTask(Printer printerToUse)
    {
        this.printerToUse = printerToUse;
    }

    @Override
    protected BedLevelCheckResult call() throws Exception
    {
        BedLevelCheckResult result = new BedLevelCheckResult();

        try
        {
            printerToUse.executeMacro("Home_all");
            //Go to centre
            printerToUse.executeMacro("level_gantry");
            printerToUse.executeMacro("level_Y");
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
