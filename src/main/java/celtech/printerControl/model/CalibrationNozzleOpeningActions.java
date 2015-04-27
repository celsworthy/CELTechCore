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
public class CalibrationNozzleOpeningActions extends StateTransitionActions
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

    private final CalibrationPrinterErrorHandler printerErrorHandler;

    public CalibrationNozzleOpeningActions(Printer printer, Cancellable userCancellable,
        Cancellable errorCancellable)
    {
        super(userCancellable, errorCancellable);
        this.printer = printer;
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
        printerErrorHandler = new CalibrationPrinterErrorHandler(printer, errorCancellable);
        printerErrorHandler.registerForPrinterErrors();
    }

    @Override
    public void initialise()
    {
        nozzle0BOffset = 0;
        nozzle1BOffset = 0;
    }

    public void doHeatingAction() throws RoboxCommsException, PrinterException, InterruptedException, CalibrationException
    {
        printerErrorHandler.registerForPrinterErrors();

        printer.inhibitHeadIntegrityChecks(true);
        printer.setPrinterStatus(PrinterStatus.CALIBRATING_NOZZLE_OPENING);

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

        printer.goToTargetNozzleTemperature();
        if (PrinterUtils.waitOnBusy(printer, userOrErrorCancellable))
        {
            return;
        }
        printer.executeMacro("Home_all");
        if (PrinterUtils.waitOnMacroFinished(printer, userOrErrorCancellable))
        {
            return;
        }
        printer.goToTargetNozzleTemperature();
        miniPurge();
        if (PrinterUtils.waitOnMacroFinished(printer, userOrErrorCancellable))
        {
            return;
        }
        printer.goToXYZPosition(PrintBed.getPrintVolumeCentre().getX(),
                                PrintBed.getPrintVolumeCentre().getZ(),
                                -PrintBed.getPrintVolumeCentre().getY());
        if (PrinterUtils.waitOnBusy(printer, userOrErrorCancellable))
        {
            return;
        }

        if (printer.headProperty().get()
            .getNozzleHeaters().get(0)
            .heaterModeProperty().get() == HeaterMode.FIRST_LAYER)
        {
            NozzleHeater nozzleHeater = printer.headProperty().get()
                .getNozzleHeaters().get(0);
            PrinterUtils.waitUntilTemperatureIsReached(
                nozzleHeater.nozzleTemperatureProperty(), null,
                nozzleHeater
                .nozzleFirstLayerTargetTemperatureProperty().get(), 5, 300,
                userOrErrorCancellable);
        } else
        {
            NozzleHeater nozzleHeater = printer.headProperty().get()
                .getNozzleHeaters().get(0);
            PrinterUtils.waitUntilTemperatureIsReached(
                nozzleHeater.nozzleTemperatureProperty(), null,
                nozzleHeater
                .nozzleTargetTemperatureProperty().get(), 5, 300, userOrErrorCancellable);
        }
        printer.switchOnHeadLEDs();
    }

    public void doNoMaterialCheckAction() throws CalibrationException, InterruptedException, PrinterException
    {
        extrudeUntilStall(0);
        pressuriseSystem();
        Thread.sleep(3000);
        printer.selectNozzle(1);
        // 
        nozzlePosition.set(0);
    }

    public void doT0Extrusion() throws PrinterException, CalibrationException
    {
        printer.selectNozzle(0);
        printer.openNozzleFullyExtra();
        printer.sendRawGCode("G1 E15 F300", false);
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
        printer.closeNozzleFully();
    }

    public void doT1Extrusion() throws PrinterException, CalibrationException
    {
        printer.selectNozzle(1);
        printer.openNozzleFullyExtra();
        printer.sendRawGCode("G1 E45 F600", false);
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
        printer.closeNozzleFully();
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
    }

    public void doCalibrateFineNozzle() throws CalibrationException
    {
        printer.gotoNozzlePosition(nozzlePosition.get());
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
    }

    public void doPreCalibrationPrimingFill() throws RoboxCommsException, CalibrationException
    {
        nozzlePosition.set(0);
        extrudeUntilStall(1);
        pressuriseSystem();
    }

    public void doCalibrateFillNozzle() throws CalibrationException
    {
        printer.gotoNozzlePosition(nozzlePosition.get());
    }

    public void doFinaliseCalibrateFineNozzle() throws PrinterException, CalibrationException
    {
        printer.closeNozzleFully();
        // calculate offset
        steno.info("(FINE) finalise nozzle position set at " + nozzlePosition.get());
        nozzle0BOffset = bOffsetStartingValue - 0.1f + nozzlePosition.get();
    }

    public void doFinaliseCalibrateFillNozzle() throws PrinterException, CalibrationException
    {
        printer.closeNozzleFully();
        // calculate offset
        steno.info("(FILL) finalise nozzle position set at " + nozzlePosition);
        nozzle1BOffset = -bOffsetStartingValue + 0.1f - nozzlePosition.get();
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
    }

    public void doConfirmMaterialExtrudingAction() throws PrinterException, CalibrationException
    {
        printer.selectNozzle(0);
        printer.openNozzleFully();
        printer.sendRawGCode("G1 E10 F75", false);
        if (PrinterUtils.waitOnBusy(printer, userOrErrorCancellable))
        {
            return;
        }
        printer.selectNozzle(1);
        printer.openNozzleFully();
        printer.sendRawGCode("G1 E10 F100", false);
        if (PrinterUtils.waitOnBusy(printer, userOrErrorCancellable))
        {
            return;
        }
    }

    private void saveSettings() throws RoboxCommsException
    {
        steno.debug("save new head data");
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

    private void restoreHeadData()
    {
        if (savedHeadData != null)
        {
            try
            {
                steno.debug("Restore head data");
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
            } catch (RoboxCommsException ex)
            {
                steno.error("Unable to restore head! " + ex);
            }
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
            PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);

        } catch (PrinterException ex)
        {
            steno.error("Error in needle valve priming");
        }
    }

    private void miniPurge()
    {
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

    public void doFinishedAction() throws RoboxCommsException, PrinterException, CalibrationException
    {
        printerErrorHandler.deregisterForPrinterErrors();
        saveSettings();
        resetPrinter();
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }

    public void doFailedAction() throws RoboxCommsException, PrinterException, CalibrationException
    {
        printerErrorHandler.deregisterForPrinterErrors();
        restoreHeadData();
        resetPrinter();
        abortAnyOngoingPrint();
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }

    @Override
    void whenUserCancelDetected()
    {
        restoreHeadData();
        abortAnyOngoingPrint();

    }

    @Override
    void whenErrorDetected()
    {
        printerErrorHandler.deregisterForPrinterErrors();
        restoreHeadData();
        abortAnyOngoingPrint();
    }

    @Override
    void resetAfterCancelOrError()
    {
        try
        {
            restoreHeadData();
            resetPrinter();
        } catch (CalibrationException | PrinterException ex)
        {
            steno.error("Error cancelling: " + ex);
        }
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }

    private void resetPrinter() throws PrinterException, CalibrationException
    {
        printerErrorHandler.deregisterForPrinterErrors();
        printer.closeNozzleFully();
        switchHeatersAndHeadLightOff();
        doBringBedToFrontAndRaiseHead();
        printer.inhibitHeadIntegrityChecks(false);
        PrinterUtils.waitOnBusy(printer, (Cancellable) null);
    }

    public void doBringBedToFrontAndRaiseHead() throws PrinterException, CalibrationException
    {
        printer.switchToAbsoluteMoveMode();
        printer.goToXYZPosition(105, 150, 25);
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
    }

    private void switchHeatersAndHeadLightOff() throws PrinterException
    {
        printer.switchAllNozzleHeatersOff();
        printer.switchOffHeadLEDs();
    }

    private void abortAnyOngoingPrint()
    {
        try
        {
            if (printer.canCancelProperty().get())
            {
                printer.cancel(null);
            }
        } catch (PrinterException ex)
        {
            steno.error("Failed to abort print - " + ex.getMessage());
        }
    }
}
