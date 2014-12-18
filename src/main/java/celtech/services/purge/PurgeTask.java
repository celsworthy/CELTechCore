package celtech.services.purge;

import celtech.Lookup;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.services.ControllableService;
import celtech.utils.PrinterUtils;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class PurgeTask extends Task<PurgeStepResult> implements ControllableService
{

    private final Stenographer steno = StenographerFactory.getStenographer(PurgeTask.class.getName());
    private PurgeState desiredState = null;
    private int nozzleNumber = -1;

    private Printer printerToUse = null;
    private boolean keyPressed = false;

    private int purgeTemperature = 0;

    /**
     *
     * @param desiredState
     */
    public PurgeTask(PurgeState desiredState)
    {
        this.desiredState = desiredState;
    }

    @Override
    protected PurgeStepResult call() throws Exception
    {
        boolean success = false;

        printerToUse = Lookup.getCurrentlySelectedPrinterProperty().get();

        switch (desiredState)
        {
            case HEATING:

                //Set the bed to 90 degrees C
                int desiredBedTemperature = 90;
//                int desiredBedTemperature = 60;
                printerToUse.setBedTargetTemperature(desiredBedTemperature);
                printerToUse.goToTargetBedTemperature();
                boolean bedHeatFailed = PrinterUtils.waitUntilTemperatureIsReached(printerToUse.getPrinterAncillarySystems().bedTemperatureProperty(), this, desiredBedTemperature, 5, 600);

                printerToUse.setNozzleTargetTemperature(purgeTemperature);
                printerToUse.goToTargetNozzleTemperature();
                //TODO modify to support multiple heaters
                boolean extruderHeatFailed = PrinterUtils.
                    waitUntilTemperatureIsReached(printerToUse.headProperty().get().getNozzleHeaters().get(0).nozzleTemperatureProperty(), this, purgeTemperature, 5, 300);

                if (!bedHeatFailed && !extruderHeatFailed)
                {
                    success = true;
                }

                break;

            case RUNNING_PURGE:
//                try
//                {
//                    Thread.sleep(5000);
//                } catch (InterruptedException ex)
//                {
//                    steno.error("Error running purge");
//                }
//                break;                
                try
                {
                    printerToUse.executeMacro("Purge Material");
                } catch (PrinterException ex)
                {
                    steno.error("Error running purge");
                }
                PrinterUtils.waitOnMacroFinished(printerToUse, this);
                break;
        }

        return new PurgeStepResult(desiredState, success);
    }

    public void setPurgeTemperature(int purgeTemperature)
    {
        this.purgeTemperature = purgeTemperature;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean cancelRun()
    {
        if (desiredState == PurgeState.RUNNING_PURGE)
        {
            try
            {
                printerToUse.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Error whilst running purge - " + ex.getMessage());
            }
        }
        return cancel();
    }
}
