/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.panels;

import celtech.appManager.TaskController;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.NozzleData;
import celtech.printerControl.model.Printer;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.model.PrinterException;
import celtech.services.calibration.CalibrateNozzleOffsetTask;
import celtech.services.calibration.NozzleOffsetCalibrationState;
import celtech.services.calibration.NozzleOffsetCalibrationStepResult;
import java.util.ArrayList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrationNozzleOffsetHelper implements CalibrationHelper
{

    private Stenographer steno = StenographerFactory.getStenographer(
        CalibrationNozzleOffsetHelper.class.getName());

    private Printer printerToUse = null;

    private DoubleProperty zco = new SimpleDoubleProperty(0);
    private double zDifference = 0;

    private HeadEEPROMDataResponse savedHeadData = null;

    private CalibrateNozzleOffsetTask calibrationTask = null;

    private NozzleOffsetCalibrationState state = NozzleOffsetCalibrationState.IDLE;
    private ArrayList<CalibrationNozzleOffsetStateListener> stateListeners = new ArrayList<>();

    private final EventHandler<WorkerStateEvent> failedTaskHandler = new EventHandler<WorkerStateEvent>()
    {
        @Override
        public void handle(WorkerStateEvent event)
        {
            cancelCalibrationAction();
        }
    };
    public BooleanProperty showDownButton = new SimpleBooleanProperty(true);
    private final CalibrationInsetPanelController parentController;

    CalibrationNozzleOffsetHelper(CalibrationInsetPanelController parentController)
    {
        this.parentController = parentController;
        zco.addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            parentController.whenZCoChanged(zco.get());
        });
    }

    public void addStateListener(CalibrationNozzleOffsetStateListener stateListener)
    {
        stateListeners.add(stateListener);
    }

    public void removeStateListener(CalibrationNozzleOffsetStateListener stateListener)
    {
        stateListeners.remove(stateListener);
    }

    private final EventHandler<WorkerStateEvent> succeededTaskHandler = new EventHandler<WorkerStateEvent>()
    {
        @Override
        public void handle(WorkerStateEvent event)
        {
            if (state == NozzleOffsetCalibrationState.MEASURE_Z_DIFFERENCE)
            {
                NozzleOffsetCalibrationStepResult result = (NozzleOffsetCalibrationStepResult) event.getSource().getValue();
                if (result.isSuccess())
                {
                    zDifference = result.getFloatValue();
                    setState(state.getNextState());
                } else
                {
                    setState(NozzleOffsetCalibrationState.FAILED);
                }
            } else
            {
                setState(state.getNextState());
            }
        }
    };

    @Override
    public void setPrinterToUse(Printer printer)
    {
        this.printerToUse = printer;
    }

    @Override
    public void buttonBAction() // Alt = UP button = too tight
    {

        zco.set(zco.get() + 0.05);

        if (zco.get() <= 0)
        {
            zco.set(0);
        }

        printerToUse.goToZPosition(zco.get());
    }

    @Override
    public void buttonAAction() // ALT = down button = too loose

    {
        zco.set(zco.get() - 0.05);

        printerToUse.goToZPosition(zco.get());

        if (zco.get() <= 0.0001)
        {
            showDownButton.set(false);
        } else
        {
            showDownButton.set(true);
        }
    }

    @Override
    public void cancelCalibrationAction()
    {
        if (calibrationTask != null)
        {
            if (calibrationTask.isRunning())
            {
                calibrationTask.cancel();
            }
        }

        if (state != NozzleOffsetCalibrationState.IDLE)
        {
            try
            {
                if (savedHeadData != null && state != NozzleOffsetCalibrationState.FINISHED)
                {
                    steno.info("Calibration cancelled - restoring head data");
                    restoreHeadData();
                }

                switchHeaterOffAndRaiseHead();
            } catch (PrinterException ex)
            {
                steno.error("Error in nozzle offset calibration - mode=" + state.name());
            } catch (RoboxCommsException ex)
            {
                steno.error("Error in nozzle offset calibration - mode=" + state.name());
            }
        } else
        {
            steno.info("Cancelling from state " + state.name() + " - no change to head data");
        }
    }

    private void restoreHeadData() throws RoboxCommsException
    {
        printerToUse.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
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
                                             savedHeadData.getLastFilamentTemperature(),
                                             savedHeadData.getHeadHours());
    }

    public void setState(NozzleOffsetCalibrationState newState)
    {
        this.state = newState;
        for (CalibrationNozzleOffsetStateListener listener : stateListeners)
        {
            listener.setNozzleHeightState(state);
        }

        switch (newState)
        {
            case IDLE:
                break;
            case INITIALISING:
                break;
            case HEATING:
                try
                {
                    savedHeadData = printerToUse.readHeadEEPROM();

                    zco.set(0.5 * (savedHeadData.getNozzle1ZOffset()
                        + savedHeadData.getNozzle2ZOffset()));
                    zDifference = savedHeadData.getNozzle2ZOffset()
                        - savedHeadData.getNozzle1ZOffset();

                    HeadFile headDataFile = HeadContainer.getHeadByID(savedHeadData.getTypeCode());
                    //TODO modify to support multiple nozzles
                    NozzleData nozzle1Data = headDataFile.getNozzles().get(0);
                    NozzleData nozzle2Data = headDataFile.getNozzles().get(1);

                    steno.info("Initialising head data prior to calibration");
                    printerToUse.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                                         savedHeadData.getUniqueID(),
                                                         savedHeadData.getMaximumTemperature(),
                                                         savedHeadData.getBeta(),
                                                         savedHeadData.getTCal(),
                                                         nozzle1Data.getDefaultXOffset(),
                                                         nozzle1Data.getDefaultYOffset(),
                                                         0,
                                                         savedHeadData.getNozzle1BOffset(),
                                                         nozzle2Data.getDefaultXOffset(),
                                                         nozzle2Data.getDefaultYOffset(),
                                                         0,
                                                         savedHeadData.getNozzle2BOffset(),
                                                         savedHeadData.getLastFilamentTemperature(),
                                                         savedHeadData.getHeadHours());
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in nozzle offset calibration - mode=" + state.name());
                }

                calibrationTask = new CalibrateNozzleOffsetTask(state);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread initialiseTaskThread = new Thread(calibrationTask);
                initialiseTaskThread.setName("Calibration N - initialising");
                initialiseTaskThread.start();
                break;
            case HEAD_CLEAN_CHECK:
                break;
            case MEASURE_Z_DIFFERENCE:
                calibrationTask = new CalibrateNozzleOffsetTask(state);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread measureTaskThread = new Thread(calibrationTask);
                measureTaskThread.setName("Calibration N - measuring");
                measureTaskThread.start();
                break;
            case INSERT_PAPER:
                break;
            case PROBING:
                break;
            case LIFT_HEAD:
                try
                {
                    printerToUse.switchToAbsoluteMoveMode();
                    printerToUse.goToZPosition(30);
                    printerToUse.goToOpenDoorPosition(null);
                } catch (PrinterException ex)
                {
                    steno.error("Error in nozzle offset calibration - mode=" + state.name());
                }
                setState(NozzleOffsetCalibrationState.REPLACE_PEI_BED);
            case FINISHED:
                try
                {
                    steno.info("Writing new calibration data to head");
                    printerToUse.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                                         savedHeadData.getUniqueID(),
                                                         savedHeadData.getMaximumTemperature(),
                                                         savedHeadData.getBeta(),
                                                         savedHeadData.getTCal(),
                                                         savedHeadData.getNozzle1XOffset(),
                                                         savedHeadData.getNozzle1YOffset(),
                                                         (float) (-zco.get() - (0.5 * zDifference)),
                                                         savedHeadData.getNozzle1BOffset(),
                                                         savedHeadData.getNozzle2XOffset(),
                                                         savedHeadData.getNozzle2YOffset(),
                                                         (float) (-zco.get() + (0.5 * zDifference)),
                                                         savedHeadData.getNozzle2BOffset(),
                                                         savedHeadData.getLastFilamentTemperature(),
                                                         savedHeadData.getHeadHours());

                    switchHeaterOffAndRaiseHead();
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in nozzle offset calibration - mode=" + state.name());
                } catch (PrinterException ex)
                {
                    steno.error("Error in nozzle offset calibration - mode=" + state.name());
                }
                break;
            case FAILED:
                try
                {
                    switchHeaterOffAndRaiseHead();
                } catch (PrinterException ex)
                {
                    steno.error("Error clearing up after failed calibration");
                }

                break;
            case NUDGE_MODE:
                try
                {
                    savedHeadData = printerToUse.readHeadEEPROM();
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error retrieving current settings from the head");
                }
                break;
        }
    }

    private void switchHeaterOffAndRaiseHead() throws PrinterException
    {
        printerToUse.switchAllNozzleHeatersOff();
        printerToUse.switchOffHeadLEDs();
        printerToUse.switchToRelativeMoveMode();
        printerToUse.goToZPosition(25);
    }

    public HeadEEPROMDataResponse getSavedHeadData()
    {
        return savedHeadData;
    }

    public void saveNozzleOffsets(double fineNozzleOffset, double coarseNozzleOffset)
    {
        steno.info("Saving nozzle offsets");
        try
        {
            printerToUse.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                                 savedHeadData.getUniqueID(),
                                                 savedHeadData.getMaximumTemperature(),
                                                 savedHeadData.getBeta(),
                                                 savedHeadData.getTCal(),
                                                 savedHeadData.getNozzle1XOffset(),
                                                 savedHeadData.getNozzle1YOffset(),
                                                 (float) fineNozzleOffset,
                                                 savedHeadData.getNozzle1BOffset(),
                                                 savedHeadData.getNozzle2XOffset(),
                                                 savedHeadData.getNozzle2YOffset(),
                                                 (float) coarseNozzleOffset,
                                                 savedHeadData.getNozzle2BOffset(),
                                                 savedHeadData.getLastFilamentTemperature(),
                                                 savedHeadData.getHeadHours());
        } catch (RoboxCommsException ex)
        {
            steno.error("Unable to write new nozzle offsets to head");
        }
    }

    @Override
    public void nextButtonAction()
    {
        if (state == NozzleOffsetCalibrationState.INSERT_PAPER)
        {
            printerToUse.homeZ();
        }
        setState(state.getNextState());
    }

    @Override
    public void goToIdleState()
    {
        setState(NozzleOffsetCalibrationState.IDLE);
    }

    @Override
    public void retryAction()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setXOffset(String toString)
    {
    }

    @Override
    public void setYOffset(Integer integer)
    {
    }

}
