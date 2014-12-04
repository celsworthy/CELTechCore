/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model;

import celtech.Lookup;
import celtech.configuration.HeaterMode;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.comms.events.ErrorConsumer;
import celtech.utils.PrinterUtils;
import celtech.utils.tasks.Cancellable;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.value.ObservableValue;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class CalibrationNozzleOpeningActions implements ErrorConsumer
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationNozzleOpeningActions.class.getName());

    private final Printer printer;
    private HeadEEPROMDataResponse savedHeadData;

    private final float bOffsetStartingValue = 0.8f;
    private float nozzle0BOffset = 0;
    private float nozzle1BOffset = 0;
    private final FloatProperty nozzlePosition = new SimpleFloatProperty();
    private final FloatProperty bPositionGUIT = new SimpleFloatProperty();

    private final Cancellable cancellable = new Cancellable();
    private boolean errorsConsumed = false;

    public CalibrationNozzleOpeningActions(Printer printer)
    {
        this.printer = printer;
        cancellable.cancelled = false;
        nozzlePosition.addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                Lookup.getTaskExecutor().runOnGUIThread(() ->
                    {
                        // bPositionGUIT mirrors nozzlePosition but is only changed on the GUI Thread
                        steno.debug("set bPositionGUIT to " + nozzlePosition.get());
                        bPositionGUIT.set(nozzlePosition.get());
                });
            });
    }
    
    

    public void doHeatingAction() throws RoboxCommsException, PrinterException, InterruptedException, CalibrationException
    {
        List<FirmwareError> errors = new ArrayList<>();
        errors.add(FirmwareError.ALL_ERRORS);
        printer.registerErrorConsumer(this, errors);
        errorsConsumed = false;
        printer.inhibitHeadIntegrityChecks(true);
        printer.setPrinterStatus(PrinterStatus.CALIBRATING_NOZZLE_OPENING);

        savedHeadData = printer.readHeadEEPROM();
        printer.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                        savedHeadData.getUniqueID(),
                                        savedHeadData.getMaximumTemperature(),
                                        savedHeadData.getBeta(),
                                        savedHeadData.getTCal(),
                                        0,
                                        0,
                                        0,
                                        bOffsetStartingValue,
                                        0,
                                        0,
                                        0,
                                        -bOffsetStartingValue,
                                        savedHeadData.getLastFilamentTemperature(),
                                        savedHeadData.getHeadHours());

        printer.goToTargetNozzleTemperature();
        if (PrinterUtils.waitOnBusy(printer, cancellable) == false)
        {
            printer.getPrintEngine().printGCodeFile(GCodeMacros.getFilename("Home_all"), true);
            if (PrinterUtils.waitOnMacroFinished(printer, cancellable) == false)
            {
                printer.goToZPosition(50);
                if (PrinterUtils.waitOnBusy(printer, cancellable) == false)
                {
                    printer.goToTargetNozzleTemperature();
//                                if (printer.headProperty().get().getNozzleHeaters().size() < 1) {
//                                    
//                                }
                    if (printer.headProperty().get()
                        .getNozzleHeaters().get(0)
                        .heaterModeProperty().get() == HeaterMode.FIRST_LAYER)
                    {
                        NozzleHeater nozzleHeater = printer.headProperty().get()
                            .getNozzleHeaters().get(0);
                        PrinterUtils.waitUntilTemperatureIsReached(
                            nozzleHeater.nozzleTemperatureProperty(), null,
                            nozzleHeater
                            .nozzleFirstLayerTargetTemperatureProperty().get(), 5, 300, cancellable);
                    } else
                    {
                        NozzleHeater nozzleHeater = printer.headProperty().get()
                            .getNozzleHeaters().get(0);
                        PrinterUtils.waitUntilTemperatureIsReached(
                            nozzleHeater.nozzleTemperatureProperty(), null,
                            nozzleHeater
                            .nozzleTargetTemperatureProperty().get(), 5, 300, cancellable);
                    }
                    printer.switchOnHeadLEDs();
                } 
            } 
        }
        printer.deregisterErrorConsumer(this);
        if (errorsConsumed) {
            throw new CalibrationException("Firmware errors were detected");
        }
    }

    public void doNoMaterialCheckAction()
    {
        extrudeUntilStall(0);
    }
    
    public void doT0Extrusion() throws PrinterException {
        printer.selectNozzle(0);
        printer.openNozzleFully();
        printer.sendRawGCode("G1 E10 F300", false);
        PrinterUtils.waitOnBusy(printer, cancellable);
    }
    
    public void doT1Extrusion() throws PrinterException {
        printer.selectNozzle(1);
        printer.openNozzleFully();
        printer.sendRawGCode("G1 E10 F600", false);
        PrinterUtils.waitOnBusy(printer, cancellable);
    }      

    public void doPreCalibrationPrimingFine() throws RoboxCommsException
    {
        nozzlePosition.set(0);

        printer.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                        savedHeadData.getUniqueID(),
                                        savedHeadData.getMaximumTemperature(),
                                        savedHeadData.getBeta(),
                                        savedHeadData.getTCal(),
                                        0,
                                        0,
                                        0,
                                        bOffsetStartingValue,
                                        0,
                                        0,
                                        0,
                                        -bOffsetStartingValue,
                                        savedHeadData.getLastFilamentTemperature(),
                                        savedHeadData.getHeadHours());
        extrudeUntilStall(0);
    }
    
    public void doCalibrateFineNozzle()
    {
        printer.gotoNozzlePosition(nozzlePosition.get());
    }

    public void doIncrementFineNozzlePosition() throws CalibrationException
    {
        nozzlePosition.set(nozzlePosition.get() + 0.05f);
        steno.info("(FINE) nozzle position set to " + nozzlePosition.get());
        if (nozzlePosition.get() >= 2f)
        {
            throw new CalibrationException("Nozzle position beyond limit");
        }
        printer.gotoNozzlePosition(nozzlePosition.get());
    }

    public void doIncrementFillNozzlePosition() throws CalibrationException
    {
        nozzlePosition.set(nozzlePosition.get() + 0.05f);
        steno.info("(FILL) nozzle position set to " + nozzlePosition);
        if (nozzlePosition.get() >= 2f)
        {
            throw new CalibrationException("Nozzle position beyond limit");
        }
        printer.gotoNozzlePosition(nozzlePosition.get());
    }

    public void doPreCalibrationPrimingFill() throws RoboxCommsException
    {
        nozzlePosition.set(0);
        extrudeUntilStall(1);
    }

    public void doCalibrateFillNozzle()
    {
        printer.gotoNozzlePosition(nozzlePosition.get());
    }

    public void doFinaliseCalibrateFineNozzle() throws PrinterException
    {
        printer.closeNozzleFully();
        // calculate offset
        steno.info("(FINE) finalise nozzle position set at " + nozzlePosition.get());
        nozzle0BOffset = bOffsetStartingValue - 0.1f + nozzlePosition.get();
    }

    public boolean doFinaliseCalibrateFillNozzle() throws PrinterException
    {
        printer.closeNozzleFully();
        // calculate offset
        steno.info("(FILL) finalise nozzle position set at " + nozzlePosition);
        nozzle1BOffset = -bOffsetStartingValue + 0.1f - nozzlePosition.get();
        return true;
    }

    public void doConfirmNoMaterialAction()throws PrinterException, RoboxCommsException, InterruptedException
    {
        // set to just about to be open
        saveSettings();
        printer.closeNozzleFully();
        printer.selectNozzle(0);
        extrudeUntilStall(0);
        Thread.sleep(3000);
        printer.selectNozzle(1);
        // 
        nozzlePosition.set(0);
    }

    public void doConfirmMaterialExtrudingAction() throws PrinterException 
    {
        printer.selectNozzle(0);
        printer.openNozzleFully();
        printer.sendRawGCode("G1 E10 F75", false);
        PrinterUtils.waitOnBusy(printer, cancellable);
        printer.selectNozzle(1);
        printer.openNozzleFully();
        printer.sendRawGCode("G1 E10 F100", false);
        PrinterUtils.waitOnBusy(printer, cancellable);
    }

    public void doFinishedAction() throws RoboxCommsException
    {
        saveSettings();
        turnHeaterAndLEDSOff();
        printer.inhibitHeadIntegrityChecks(false);
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }

    public void doFailedAction() throws RoboxCommsException
    {
        restoreHeadState();
        turnHeaterAndLEDSOff();
        printer.inhibitHeadIntegrityChecks(false);
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }

    public void cancel() throws RoboxCommsException
    {
        cancellable.cancelled = true;
        try
        {
            // wait for any current actions to respect cancelled flag
            Thread.sleep(500);
        } catch (InterruptedException ex)
        {
            steno.info("interrupted during wait of cancel");
        }
        doFailedAction();
    }

    private void turnHeaterAndLEDSOff() throws RoboxCommsException
    {
        try
        {
            printer.closeNozzleFully();
            printer.switchAllNozzleHeatersOff();
            printer.switchOffHeadLEDs();
        } catch (PrinterException ex)
        {
            steno.error("Error turning off heater and LEDs: " + ex);
        }
    }

    private void saveSettings() throws RoboxCommsException
    {
        printer.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                        savedHeadData.getUniqueID(),
                                        savedHeadData.getMaximumTemperature(),
                                        savedHeadData.getBeta(),
                                        savedHeadData.getTCal(),
                                        savedHeadData.getNozzle1XOffset(),
                                        savedHeadData.getNozzle1YOffset(),
                                        savedHeadData.getNozzle1ZOffset(),
                                        nozzle0BOffset,
                                        savedHeadData.getNozzle2XOffset(),
                                        savedHeadData.getNozzle2YOffset(),
                                        savedHeadData.getNozzle2ZOffset(),
                                        nozzle1BOffset,
                                        savedHeadData.getLastFilamentTemperature(),
                                        savedHeadData.getHeadHours());

    }

    private void restoreHeadState() throws RoboxCommsException
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

    private void extrudeUntilStall(int nozzleNumber)
    {
        try
        {
            // select nozzle
            printer.selectNozzle(nozzleNumber);
            // G36 = extrude until stall E700 = top extruder F2000 = feed rate mm/min (?)
            // extrude either requested volume or until filament slips
            printer.sendRawGCode("G36 E700 F2000", false);

            PrinterUtils.waitOnBusy(printer, cancellable);

        } catch (PrinterException ex)
        {
            steno.error("Error in needle valve priming");
        }
    }

    @Override
    public void consume(FirmwareError error)
    {
        errorsConsumed = true;
    }

    public ReadOnlyFloatProperty getBPositionGUITProperty()
    {
        return bPositionGUIT;
    }

}
