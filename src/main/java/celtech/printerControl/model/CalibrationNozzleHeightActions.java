/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model;

import celtech.Lookup;
import celtech.configuration.HeaterMode;
import celtech.configuration.PrintBed;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.NozzleData;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.utils.PrinterUtils;
import celtech.utils.tasks.Cancellable;
import java.util.ArrayList;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class CalibrationNozzleHeightActions extends StateTransitionActions
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationNozzleHeightActions.class.getName());

    private final Printer printer;
    private HeadEEPROMDataResponse savedHeadData;
    private final DoubleProperty zco = new SimpleDoubleProperty();
    private final DoubleProperty zcoGUIT = new SimpleDoubleProperty();
    private double zDifference;
    private final CalibrationPrinterErrorHandler printerErrorHandler;

    public CalibrationNozzleHeightActions(Printer printer, Cancellable userCancellable, Cancellable errorCancellable)
    {
        super(userCancellable, errorCancellable);
        this.printer = printer;
        zco.addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                Lookup.getTaskExecutor().runOnGUIThread(() ->
                    {
                        // zcoGUIT mirrors zco but is only changed on the GUI Thread
                        steno.debug("set zcoGUIT to " + zco.get());
                        zcoGUIT.set(zco.get());
                });
            });
        printerErrorHandler = new CalibrationPrinterErrorHandler(printer, errorCancellable);
    }

    public void doInitialiseAndHeatNozzleAction() throws InterruptedException, PrinterException, RoboxCommsException, CalibrationException
    {
        printerErrorHandler.registerForPrinterErrors();
        
        zco.set(0);

        printer.setPrinterStatus(PrinterStatus.CALIBRATING_NOZZLE_HEIGHT);
        savedHeadData = printer.readHeadEEPROM();
        clearZOffsetsOnHead();
        heatNozzle();

    }

    private void clearZOffsetsOnHead() throws RoboxCommsException
    {
        HeadFile headDataFile = HeadContainer.getHeadByID(savedHeadData.getTypeCode());
        //TODO modify to support multiple nozzles
        NozzleData nozzle1Data = headDataFile.getNozzles().get(0);
        NozzleData nozzle2Data = headDataFile.getNozzles().get(1);

        printer.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
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
    }

    private void heatNozzle() throws InterruptedException, PrinterException
    {
        printer.goToTargetNozzleTemperature();
        printer.executeMacro("Home_all");
        if (PrinterUtils.waitOnMacroFinished(printer, userOrErrorCancellable) == false)
        {
            printer.goToTargetNozzleTemperature();
            printer.goToZPosition(50);
            printer.goToXYPosition(PrintBed.getPrintVolumeCentre().getX(), PrintBed.
                                   getPrintVolumeCentre().getZ());
            if (printer.headProperty().get()
                .getNozzleHeaters().get(0)
                .heaterModeProperty().get() == HeaterMode.FIRST_LAYER)
            {
                NozzleHeater nozzleHeater = printer.headProperty().get()
                    .getNozzleHeaters().get(0);
                PrinterUtils.waitUntilTemperatureIsReached(
                    nozzleHeater.nozzleTemperatureProperty(), null,
                    nozzleHeater
                    .nozzleFirstLayerTargetTemperatureProperty().get(), 5, 300, userOrErrorCancellable);
            } else
            {
                NozzleHeater nozzleHeater = printer.headProperty().get()
                    .getNozzleHeaters().get(0);
                PrinterUtils.waitUntilTemperatureIsReached(
                    nozzleHeater.nozzleTemperatureProperty(), null,
                    nozzleHeater
                    .nozzleTargetTemperatureProperty().get(), 5, 300, userOrErrorCancellable);
            }
            if (PrinterUtils.waitOnBusy(printer, userOrErrorCancellable) == false)
            {
                printer.switchOnHeadLEDs();
            } else
            {
                return;
            }
        } else
        {
            return;
        }
    }

    public void doHomeZAction() throws CalibrationException
    {
        printer.homeZ();
    }

    public void doLiftHeadAction() throws PrinterException, CalibrationException
    {
        printer.switchToAbsoluteMoveMode();
        printer.goToZPosition(30);
    }

    public void doMeasureZDifferenceAction() throws PrinterException, CalibrationException
    {
        boolean success = false;

        final int numberOfNozzleHeightDifferenceTests = 11;

        steno.info("Nozzle height difference test");
        printer.selectNozzle(0);

        // Level the gantry - manual rather than using the macro
        printer.goToXYPosition(30.0, 75.0);
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
        printer.homeZ();
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
        printer.goToZPosition(5.0);
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
        printer.goToXYPosition(190.0, 75.0);
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
        printer.homeZ();
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
        printer.goToZPosition(5.0);
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
        printer.levelGantry();
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
        printer.goToXYPosition(105.0, 75.0);
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
        printer.homeZ();
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
        printer.goToZPosition(5.0);
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);

        ArrayList<Float> t0Deltas = new ArrayList<>();
        t0Deltas.add(0f);
        ArrayList<Float> t1Deltas = new ArrayList<>();

        boolean flipFlop = false;
        for (int testCount = 0; testCount < numberOfNozzleHeightDifferenceTests; testCount++)
        {
            int nozzleFrom = ((flipFlop == false) ? 0 : 1);
            int nozzleTo = ((flipFlop == false) ? 1 : 0);

            printer.selectNozzle(nozzleTo);
            PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
            printer.probeZ();
            PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
            float deltaValue = printer.getZDelta();

            if (nozzleTo == 0)
            {
                t0Deltas.add(deltaValue);
            } else
            {
                t1Deltas.add(deltaValue);
            }
            steno.info("Delta from " + nozzleFrom + " to " + nozzleTo + " -> " + deltaValue);
            flipFlop = !flipFlop;
        }

        printer.selectNozzle(0);

        float sumOfDeltas = 0;
        int numberOfSamples = ((numberOfNozzleHeightDifferenceTests + 1) / 2);

        for (int deltaCount = 0; deltaCount < numberOfSamples; deltaCount++)
        {
            sumOfDeltas += t1Deltas.get(deltaCount) - t0Deltas.get(deltaCount);
        }

        zDifference = sumOfDeltas / numberOfSamples;
        success = true;
        steno.info("Average Z Offset was " + zDifference);

        printer.selectNozzle(0);
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
        if (!success)
        {
            throw new CalibrationException("ZCO could not be established");
        }
    }

    public void doIncrementZAction() throws CalibrationException
    {
        zco.set(zco.get() + 0.05);
        printer.goToZPosition(zco.get());
    }

    public void doDecrementZAction() throws CalibrationException
    {
        zco.set(zco.get() - 0.05);
        if (zco.get() < 0)
        {
            zco.set(0);
        }
        printer.goToZPosition(zco.get());
    }

    public void doFinishedAction() throws PrinterException, RoboxCommsException
    {
        printerErrorHandler.deregisterForPrinterErrors();
        saveSettings();
        switchHeatersAndHeadLightOff();
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }

    public void doFailedAction() throws PrinterException, RoboxCommsException, CalibrationException
    {
        printerErrorHandler.deregisterForPrinterErrors();
        restoreHeadData();
        switchHeatersAndHeadLightOff();
        doBringBedToFrontAndRaiseHead();
        steno.info("Am cancelling... with printer status " + printer.printerStatusProperty().get() + " " + printer.macroTypeProperty().get());
        if (printer.canCancelProperty().get())
        {
            printer.cancel(null);
        }
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }

    public void doBringBedToFrontAndRaiseHead() throws PrinterException, CalibrationException
    {
        printer.switchToAbsoluteMoveMode();
        printer.goToXYZPosition(105, 150, 25);
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
    }

    private void cancelOngoingActionAndResetPrinter() 
    {
        try
        {
            // wait for any current actions to respect cancelled flag
            Thread.sleep(500);
        } catch (InterruptedException ex)
        {
            steno.info("interrupted during wait of cancel");
        }
        try
        {
            doFailedAction();
        } catch (CalibrationException | RoboxCommsException | PrinterException ex)
        {
           steno.error("Error cancelling calibration: " + ex);
        } 
    }

    private void switchHeatersAndHeadLightOff() throws PrinterException
    {
        printer.switchAllNozzleHeatersOff();
        printer.switchOffHeadLEDs();
    }

    private void restoreHeadData() throws RoboxCommsException
    {
        if (savedHeadData != null)
        {
            printer.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
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
    }

    public void saveSettings() throws RoboxCommsException
    {
        steno.info("zDifference is " + zDifference);
        steno.info("zco is " + zDifference);
        printer.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
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
    }

    public ReadOnlyDoubleProperty getZcoGUITProperty()
    {
        return zcoGUIT;
    }

    @Override
    void whenUserCancelDetected()
    {
        cancelOngoingActionAndResetPrinter();

    }

    @Override
    void whenErrorDetected()
    {
        cancelOngoingActionAndResetPrinter();
    }

}
