/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model;

import celtech.Lookup;
import celtech.configuration.HeaterMode;
import celtech.configuration.PrintBed;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.utils.PrinterUtils;
import celtech.utils.tasks.Cancellable;
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
public class CalibrationNozzleOpeningActions
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

    private final CalibrationOpeningErrorHandler printerErrorHandler;
    private final Cancellable cancellable = new Cancellable();

    public CalibrationNozzleOpeningActions(Printer printer)
    {
        this.printer = printer;
        cancellable.cancelled.set(false);
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
        printerErrorHandler = new CalibrationOpeningErrorHandler(printer, cancellable);
    }

//    private void wrapInPrinterErrorCheck(NoArgsVoidFunc action, NoArgsVoidFunc continueHandler,
//        NoArgsVoidFunc abortHandler, NoArgsVoidFunc retryHandler) throws Exception
//    {
//        errorOccurred = false;
//        List<FirmwareError> errors = new ArrayList<>();
//        errors.add(FirmwareError.ALL_ERRORS);
//
//        ErrorConsumer errorConsumer = (FirmwareError error) ->
//        {
//            errorOccurred = true;
//        };
//        printer.registerErrorConsumer(errorConsumer, errors);
//
//        try
//        {
//            action.run();
//        } finally
//        {
//            printer.deregisterErrorConsumer(errorConsumer);
//        }
//    }
//    public void doHeatingActionHandled() throws Exception
//    {
//        wrapInPrinterErrorCheck(this::doHeatingAction, continueHandler, abortHandler, retryHandler);
//    }
    public void doHeatingAction() throws RoboxCommsException, PrinterException, InterruptedException, CalibrationException
    {
        printerErrorHandler.registerForPrinterErrors();

        printer.inhibitHeadIntegrityChecks(true);
        printer.setPrinterStatus(PrinterStatus.CALIBRATING_NOZZLE_OPENING);

        cancelIfErrorsDetected();

        savedHeadData = printer.readHeadEEPROM();

        HeadFile headReferenceData = HeadContainer.getHeadByID(savedHeadData.getTypeCode());

        if (headReferenceData != null)
        {
            steno.info("Setting B offsets to defaults ("
                + headReferenceData.getNozzles().get(0).getDefaultBOffset()
                + " - "
                + headReferenceData.getNozzles().get(1).getDefaultBOffset()
                + ")");
            printer.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                            savedHeadData.getUniqueID(),
                                            savedHeadData.getMaximumTemperature(),
                                            savedHeadData.getBeta(),
                                            savedHeadData.getTCal(),
                                            0,
                                            0,
                                            0,
                                            headReferenceData.getNozzles().get(0).
                                            getDefaultBOffset(),
                                            0,
                                            0,
                                            0,
                                            headReferenceData.getNozzles().get(1).
                                            getDefaultBOffset(),
                                            savedHeadData.getLastFilamentTemperature(),
                                            savedHeadData.getHeadHours());
        } else
        {
            // We shouldn't ever get here, but just in case...
            steno.info("Setting B offsets to safe values ("
                + headReferenceData.getNozzles().get(0).getDefaultBOffset()
                + " - "
                + headReferenceData.getNozzles().get(1).getDefaultBOffset()
                + ")");
            printer.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                            savedHeadData.getUniqueID(),
                                            savedHeadData.getMaximumTemperature(),
                                            savedHeadData.getBeta(),
                                            savedHeadData.getTCal(),
                                            0,
                                            0,
                                            0,
                                            1,
                                            0,
                                            0,
                                            0,
                                            -1,
                                            savedHeadData.getLastFilamentTemperature(),
                                            savedHeadData.getHeadHours());
        }

        cancelIfErrorsDetected();

        printer.goToTargetNozzleTemperature();
        if (PrinterUtils.waitOnBusy(printer, cancellable) == false)
        {
            printer.executeMacro("Home_all");
            if (PrinterUtils.waitOnMacroFinished(printer, cancellable) == false)
            {
                printer.goToTargetNozzleTemperature();
                miniPurge();
                printer.goToXYZPosition(PrintBed.getPrintVolumeCentre().getX(),
                                        PrintBed.getPrintVolumeCentre().getZ(),
                                        -PrintBed.getPrintVolumeCentre().getY());
                if (PrinterUtils.waitOnBusy(printer, cancellable) == false)
                {

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
        cancelIfErrorsDetected();
    }

    public void doNoMaterialCheckAction() throws CalibrationException, InterruptedException, PrinterException
    {
        extrudeUntilStall(0);
        pressuriseSystem();
        Thread.sleep(3000);
        printer.selectNozzle(1);
        // 
        nozzlePosition.set(0);

        cancelIfErrorsDetected();
    }

    public void doT0Extrusion() throws PrinterException, CalibrationException
    {
        printer.selectNozzle(0);
        printer.openNozzleFullyExtra();
        printer.sendRawGCode("G1 E15 F300", false);
        PrinterUtils.waitOnBusy(printer, cancellable);
        printer.closeNozzleFully();
        cancelIfErrorsDetected();
    }

    public void doT1Extrusion() throws PrinterException, CalibrationException
    {
        printer.selectNozzle(1);
        printer.openNozzleFullyExtra();
        printer.sendRawGCode("G1 E45 F600", false);
        PrinterUtils.waitOnBusy(printer, cancellable);
        printer.closeNozzleFully();
        cancelIfErrorsDetected();
    }

    public void doPreCalibrationPrimingFine() throws RoboxCommsException, CalibrationException
    {
        nozzlePosition.set(0);

        steno.info("Setting B offsets to calibration values ("
            + bOffsetStartingValue
            + " - "
            + -bOffsetStartingValue
            + ")");

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
        pressuriseSystem();
        cancelIfErrorsDetected();
    }

    public void doCalibrateFineNozzle() throws CalibrationException
    {
        printer.gotoNozzlePosition(nozzlePosition.get());
        cancelIfErrorsDetected();
    }

    public void doIncrementFineNozzlePosition() throws CalibrationException, InterruptedException
    {
        nozzlePosition.set(nozzlePosition.get() + 0.05f);
        steno.info("(FINE) nozzle position set to " + nozzlePosition.get());
        if (nozzlePosition.get() >= 2f)
        {
            throw new CalibrationException("Nozzle position beyond limit");
        }
        printer.gotoNozzlePosition(nozzlePosition.get());
        Thread.sleep(1000);
        cancelIfErrorsDetected();
    }

    private void cancelIfErrorsDetected() throws CalibrationException
    {
        if (printerErrorHandler.checkIfPrinterErrorHasOccurred())
        {
            try
            {
                cancel();
            } catch (RoboxCommsException ex)
            {
                steno.error("Failed to cancel calibration");
            }
        }
    }

    public void doIncrementFillNozzlePosition() throws CalibrationException, InterruptedException
    {
        nozzlePosition.set(nozzlePosition.get() + 0.05f);
        steno.info("(FILL) nozzle position set to " + nozzlePosition);
        if (nozzlePosition.get() >= 2f)
        {
            throw new CalibrationException("Nozzle position beyond limit");
        }
        printer.gotoNozzlePosition(nozzlePosition.get());
        Thread.sleep(1000);
        cancelIfErrorsDetected();
    }

    public void doPreCalibrationPrimingFill() throws RoboxCommsException, CalibrationException
    {
        nozzlePosition.set(0);
        extrudeUntilStall(1);
        pressuriseSystem();
        cancelIfErrorsDetected();
    }

    public void doCalibrateFillNozzle() throws CalibrationException
    {
        printer.gotoNozzlePosition(nozzlePosition.get());
        cancelIfErrorsDetected();
    }

    public void doFinaliseCalibrateFineNozzle() throws PrinterException, CalibrationException
    {
        printer.closeNozzleFully();
        // calculate offset
        steno.info("(FINE) finalise nozzle position set at " + nozzlePosition.get());
        nozzle0BOffset = bOffsetStartingValue - 0.1f + nozzlePosition.get();
        cancelIfErrorsDetected();
    }

    public void doFinaliseCalibrateFillNozzle() throws PrinterException, CalibrationException
    {
        printer.closeNozzleFully();
        // calculate offset
        steno.info("(FILL) finalise nozzle position set at " + nozzlePosition);
        nozzle1BOffset = -bOffsetStartingValue + 0.1f - nozzlePosition.get();
        cancelIfErrorsDetected();
    }

    public void doConfirmNoMaterialAction() throws PrinterException, RoboxCommsException, InterruptedException, CalibrationException
    {
        // set to just about to be open
        saveSettings();
        printer.closeNozzleFully();
        printer.selectNozzle(0);
        extrudeUntilStall(0);
        pressuriseSystem();
        Thread.sleep(3000);
        printer.selectNozzle(1);
        // 
        nozzlePosition.set(0);
        cancelIfErrorsDetected();
    }

    public void doConfirmMaterialExtrudingAction() throws PrinterException, CalibrationException
    {
        printer.selectNozzle(0);
        printer.openNozzleFully();
        printer.sendRawGCode("G1 E10 F75", false);
        PrinterUtils.waitOnBusy(printer, cancellable);
        cancelIfErrorsDetected();
        printer.selectNozzle(1);
        printer.openNozzleFully();
        printer.sendRawGCode("G1 E10 F100", false);
        PrinterUtils.waitOnBusy(printer, cancellable);
        cancelIfErrorsDetected();
    }

    public void doFinishedAction() throws RoboxCommsException
    {
        printerErrorHandler.deregisterForPrinterErrors();
        saveSettings();
        turnHeaterAndLEDSOff();
        printer.inhibitHeadIntegrityChecks(false);
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }

    public void doFailedAction() throws RoboxCommsException
    {
        printerErrorHandler.deregisterForPrinterErrors();
        restoreHeadState();
        turnHeaterAndLEDSOff();
        printer.inhibitHeadIntegrityChecks(false);
        try
        {
            if (printer.canCancelProperty().get())
            {
                printer.cancel(null);
            }
        } catch (PrinterException ex)
        {
            steno.error("Failed to cancel print - " + ex.getMessage());
        }
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }

    public void cancel() throws RoboxCommsException
    {
        cancellable.cancelled.set(true);
        try
        {
            // wait for any current actions to respect cancelled flag
            Thread.sleep(500);
        } catch (InterruptedException ex)
        {
            steno.warning("interrupted during wait of cancel");
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
            printer.sendRawGCode("G36 E100 F800", false);
            PrinterUtils.waitOnBusy(printer, cancellable);

        } catch (PrinterException ex)
        {
            steno.error("Error in needle valve priming");
        }
    }

    private void miniPurge()
    {
        //;Short Purge T0
        //G0 Y-6 X14 Z10
        //T0
        //G0 Z3
        //G1 Y-4 F400
        //G36 E500 F400
        //G0 B2
        //G1 E2 F250
        //G1 E30 X36 F250
        //G0 B0
        //G0 Z5
        //G0 Y3
        //
        //;Short Purge T1
        //G0 Y-6 Z8
        //T1
        //G0 Z4
        //G1 Y-4 F400
        //G36 E500 F400
        //G0 B2
        //G1 E4 F300
        //G1 E35 X14 F300
        //G0 B0
        //G0 Z5
        //G0 Y3

        try
        {
            printer.sendRawGCode("M109", false);
            printer.sendRawGCode("G90", false);
            printer.sendRawGCode("G0 Y-1 X29 Z10", false);
            printer.selectNozzle(0);
            printer.sendRawGCode("G0 Z3", false);
            printer.sendRawGCode("G1 Y1 F400", false);
            printer.sendRawGCode("G36 E500 F400", false);
            printer.sendRawGCode("G0 B2", false);
            printer.sendRawGCode("G1 E2 F250", false);
            printer.sendRawGCode("G1 E30 X51 F250", false);
            printer.sendRawGCode("G0 B0", false);
            printer.sendRawGCode("G0 Z5", false);
            printer.sendRawGCode("G0 Y8", false);

            printer.sendRawGCode("G0 Y-1 X36 Z8", false);
            printer.selectNozzle(1);
            printer.sendRawGCode("G0 Z4", false);
            printer.sendRawGCode("G1 Y1 F400", false);
            printer.sendRawGCode("G36 E500 F400", false);
            printer.sendRawGCode("G0 B2", false);
            printer.sendRawGCode("G1 E4 F300", false);
            printer.sendRawGCode("G1 E35 X14 F300", false);
            printer.sendRawGCode("G0 B0", false);
            printer.sendRawGCode("G0 Z5", false);
            printer.sendRawGCode("G0 Y8", false);

            PrinterUtils.waitOnBusy(printer, cancellable);

        } catch (PrinterException ex)
        {
            steno.error("Error in calibration mini purge");
        }
    }

    public ReadOnlyFloatProperty getBPositionGUITProperty()
    {
        return bPositionGUIT;
    }

    private void pressuriseSystem()
    {
        printer.sendRawGCode("G1 E4 F400", false);
    }
}
