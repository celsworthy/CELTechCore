package celtech.services.diagnostics;

import celtech.appManager.Project;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.printerControl.model.PrinterException;
import celtech.roboxbase.services.ControllableService;
import celtech.roboxbase.services.slicer.PrintQualityEnumeration;
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
    private SlicerParametersFile settings = null;
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
            printerToUse.homeAllAxes(true, null);
            //Go to centre
            printerToUse.levelGantry(true, null);
            printerToUse.levelY(true, null);
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
