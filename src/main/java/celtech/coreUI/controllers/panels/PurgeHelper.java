/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.panels;

import celtech.appManager.TaskController;
import celtech.configuration.Filament;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.services.purge.PurgeState;
import celtech.services.purge.PurgeTask;
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

    private Stenographer steno = StenographerFactory.getStenographer(CalibrationNozzleBHelper.class.getName());

    private Printer printerToUse = null;

    private HeadEEPROMDataResponse savedHeadData = null;

    private PurgeTask purgeTask = null;

    private PurgeState state = PurgeState.IDLE;
    private ArrayList<PurgeStateListener> stateListeners = new ArrayList<>();

    private float reelNozzleTemperature = 0;
    private int lastDisplayTemperature = 0;
    private int currentDisplayTemperature = 0;
    private int purgeTemperature = 0;

    private final EventHandler<WorkerStateEvent> failedTaskHandler = (WorkerStateEvent event) ->
    {
        cancelPurgeAction();
    };

    private final EventHandler<WorkerStateEvent> succeededTaskHandler = (WorkerStateEvent event) ->
    {
        setState(state.getNextState());
    };

    public void setPrinterToUse(Printer printer)
    {
        this.printerToUse = printer;
    }

    /**
     *
     */
    public void cancelPurgeAction()
    {
        if (purgeTask != null)
        {
            if (purgeTask.isRunning())
            {
                purgeTask.cancelRun();
            }
        }
        if (state != PurgeState.IDLE)
        {
            try
            {
                printerToUse.transmitDirectGCode("G0 B0", false);
                printerToUse.transmitDirectGCode(GCodeConstants.switchBedHeaterOff, false);
                printerToUse.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
                printerToUse.transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, false);

            } catch (RoboxCommsException ex)
            {
                steno.error("Error in purge routine - mode=" + state.name());
            }
        }
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

                break;

            case INITIALISING:
                // put the write after the purge routine once the firmware no longer raises an error whilst connected to the host computer
                try
                {
                    savedHeadData = printerToUse.transmitReadHeadEEPROM();

                    // The nozzle should be heated to a temperature halfway between the last temperature stored on the head and the current required temperature stored on the reel
                    SettingsScreenState settingsScreenState = SettingsScreenState.getInstance();

                    Filament settingsFilament = settingsScreenState.getFilament();

                    if (settingsFilament != null)
                    {
                        reelNozzleTemperature = settingsFilament.getNozzleTemperature();
                    } else
                    {
                        reelNozzleTemperature = (float) printerToUse.getReelNozzleTemperature().get();
                    }

                    float temperatureDifference = reelNozzleTemperature - savedHeadData.getLastFilamentTemperature();
                    lastDisplayTemperature = (int) savedHeadData.getLastFilamentTemperature();
                    currentDisplayTemperature = (int) reelNozzleTemperature;
                    purgeTemperature = (int) Math.min(savedHeadData.getMaximumTemperature(), Math.max(180.0, savedHeadData.getLastFilamentTemperature() + (temperatureDifference / 2)));
                    setState(state.getNextState());
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error during purge operation");
                    cancelPurgeAction();
                }
                break;

            case RUNNING_PURGE:
                purgeTask = new PurgeTask(state);
                purgeTask.setOnSucceeded(succeededTaskHandler);
                purgeTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(purgeTask);

                purgeTask.setPurgeTemperature(purgeTemperature);

                Thread purgingTaskThread = new Thread(purgeTask);
                purgingTaskThread.setName("Purge - running purge");
                purgingTaskThread.start();
                break;

            case HEATING:
                purgeTask = new PurgeTask(state);
                purgeTask.setOnSucceeded(succeededTaskHandler);
                purgeTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(purgeTask);

                purgeTask.setPurgeTemperature(purgeTemperature);

                Thread heatingTaskThread = new Thread(purgeTask);
                heatingTaskThread.setName("Purge - heating");
                heatingTaskThread.start();
                break;

            case FINISHED:
                try
                {
                    AckResponse ackResponse = printerToUse.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
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

                    printerToUse.transmitDirectGCode("G0 B0", false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchBedHeaterOff, false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in purge - mode=" + state.name());
                }
                break;
            case FAILED:
                try
                {
                    printerToUse.transmitDirectGCode("G0 B0", false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchBedHeaterOff, false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error clearing up after failed purge");
                }
                break;
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
