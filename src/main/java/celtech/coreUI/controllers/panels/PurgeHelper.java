package celtech.coreUI.controllers.panels;

import celtech.appManager.TaskController;
import celtech.configuration.Filament;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.services.purge.PurgePrinterErrorHandler;
import celtech.services.purge.PurgeState;
import celtech.services.purge.PurgeTask;
import celtech.utils.tasks.Cancellable;
import java.util.ArrayList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class PurgeHelper
{

    private Stenographer steno = StenographerFactory.getStenographer(PurgeHelper.class.getName());

    private Printer printerToUse = null;

    private HeadEEPROMDataResponse savedHeadData = null;

    private PurgeTask purgeTask = null;

    private PurgeState state = PurgeState.IDLE;
    private final ArrayList<PurgeStateListener> stateListeners = new ArrayList<>();

    private float reelNozzleTemperature = 0;
    private int lastDisplayTemperature = 0;
    private int currentDisplayTemperature = 0;
    private int purgeTemperature = 0;

    private final PurgePrinterErrorHandler printerErrorHandler;
    private final Cancellable cancellable = new Cancellable();
    
    private final EventHandler<WorkerStateEvent> failedTaskHandler = (WorkerStateEvent event) ->
    {
        cancelPurgeAction();
    };

    private final EventHandler<WorkerStateEvent> succeededTaskHandler = (WorkerStateEvent event) ->
    {
        setState(state.getNextState());
    };

    PurgeHelper(Printer printer)
    {
        this.printerToUse = printer;
        printerErrorHandler = new PurgePrinterErrorHandler(printer, cancellable);
        printerErrorHandler.registerForPrinterErrors();
    }

    public void cancelPurgeAction()
    {
        printerErrorHandler.deregisterForPrinterErrors();
        if (purgeTask != null)
        {
            if (purgeTask.isRunning())
            {
                purgeTask.cancelRun();
            }
        }
        if (state != PurgeState.IDLE)
        {
            resetPrinter();
        }
    }

    public void repeatPurgeAction()
    {
        setState(PurgeState.INITIALISING);
    }

    public PurgeState getState()
    {
        return state;
    }

    public void addStateListener(PurgeStateListener stateListener)
    {
        stateListeners.add(stateListener);
    }

    public void removeStateListener(PurgeStateListener stateListener)
    {
        stateListeners.remove(stateListener);
    }

    public void setState(PurgeState newState)
    {
        this.state = newState;
        for (PurgeStateListener listener : stateListeners)
        {
            listener.setState(state);
        }

        switch (state)
        {
            case IDLE:
                printerErrorHandler.checkIfPrinterErrorHasOccurred();
                break;

            case INITIALISING:
                // put the write after the purge routine once the firmware no longer raises an error whilst connected to the host computer
                try
                {
                    savedHeadData = printerToUse.readHeadEEPROM();

                    // The nozzle should be heated to a temperature halfway between the last temperature stored on the head and the current required temperature stored on the reel
                    SettingsScreenState settingsScreenState = SettingsScreenState.getInstance();

                    Filament settingsFilament = settingsScreenState.getFilament();

                    if (settingsFilament != null)
                    {
                        reelNozzleTemperature = settingsFilament.getNozzleTemperature();
                    } else
                    {
                        //TODO modify for multiple reels
                        reelNozzleTemperature = (float) printerToUse.reelsProperty().get(0).nozzleTemperatureProperty().get();
                    }

                    float temperatureDifference = reelNozzleTemperature
                        - savedHeadData.getLastFilamentTemperature();
                    lastDisplayTemperature = (int) savedHeadData.getLastFilamentTemperature();
                    currentDisplayTemperature = (int) reelNozzleTemperature;
                    purgeTemperature = (int) Math.min(savedHeadData.getMaximumTemperature(),
                                                      Math.max(180.0,
                                                               savedHeadData.getLastFilamentTemperature()
                                                               + (temperatureDifference / 2)));
                    setState(state.getNextState());
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error during purge operation");
                    cancelPurgeAction();
                }
                printerErrorHandler.checkIfPrinterErrorHasOccurred();
                break;

            case RUNNING_PURGE:
                purgeTask = new PurgeTask(state, cancellable);
                purgeTask.setOnSucceeded(succeededTaskHandler);
                purgeTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(purgeTask);

                purgeTask.setPurgeTemperature(purgeTemperature);

                Thread purgingTaskThread = new Thread(purgeTask);
                purgingTaskThread.setName("Purge - running purge");
                purgingTaskThread.start();
                printerErrorHandler.checkIfPrinterErrorHasOccurred();
                break;

            case HEATING:
                purgeTask = new PurgeTask(state, cancellable);
                purgeTask.setOnSucceeded(succeededTaskHandler);
                purgeTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(purgeTask);

                purgeTask.setPurgeTemperature(purgeTemperature);

                Thread heatingTaskThread = new Thread(purgeTask);
                heatingTaskThread.setName("Purge - heating");
                heatingTaskThread.start();
                printerErrorHandler.checkIfPrinterErrorHasOccurred();
                break;

            case FINISHED:
                try
                {
                    AckResponse ackResponse = printerToUse.transmitWriteHeadEEPROM(
                        savedHeadData.getTypeCode(),
                        savedHeadData.getUniqueID(),
                        savedHeadData.getMaximumTemperature(),
                        savedHeadData.getBeta(),
                        savedHeadData.getTCal(),
                        savedHeadData.getNozzle1XOffset(),
                        savedHeadData.getNozzle1YOffset(),
                        savedHeadData.getNozzle1ZOffset(),
                        savedHeadData.getNozzle1BOffset(),
                        savedHeadData.getNozzle2XOffset(),
                        savedHeadData.getNozzle2YOffset(),
                        savedHeadData.getNozzle2ZOffset(),
                        savedHeadData.getNozzle2BOffset(),
                        reelNozzleTemperature,
                        savedHeadData.getHeadHours());
                    printerToUse.readHeadEEPROM();
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in purge - mode=" + state.name());
                }
                printerErrorHandler.deregisterForPrinterErrors();
                break;
            case FAILED:
                resetPrinter();
                printerErrorHandler.deregisterForPrinterErrors();
                break;
        }
    }

    private void resetPrinter()
    {
        try
        {
            printerToUse.gotoNozzlePosition(0);
            printerToUse.switchBedHeaterOff();
            //TODO modify for multiple nozzle heater support
            printerToUse.switchNozzleHeaterOff(0);
            printerToUse.switchOffHeadLEDs();
        } catch (PrinterException ex)
        {
            steno.error("Error resetting printer");
        }
    }

    public int getLastMaterialTemperature()
    {
        return lastDisplayTemperature;
    }

    public int getCurrentMaterialTemperature()
    {
        return currentDisplayTemperature;
    }

    public int getPurgeTemperature()
    {
        return purgeTemperature;
    }

    public void setPurgeTemperature(int newPurgeTemperature)
    {
        purgeTemperature = newPurgeTemperature;
    }
}
