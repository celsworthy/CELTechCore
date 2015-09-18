/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model;

import celtech.Lookup;
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

    private boolean failedActionPerformed = false;

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
        PrinterUtils.setCancelledIfPrinterDisconnected(printer, errorCancellable);
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

        printer.homeAllAxes(true, userOrErrorCancellable);
        if (PrinterUtils.waitOnMacroFinished(printer, userOrErrorCancellable))
        {
            return;
        }

        printer.goToTargetNozzleHeaterTemperature(0);

        waitOnNozzleTemperature(0);
        if (PrinterUtils.waitOnMacroFinished(printer, userOrErrorCancellable))
        {
            return;
        }

        printer.miniPurge(true, userOrErrorCancellable, 0);
        if (PrinterUtils.waitOnMacroFinished(printer, userOrErrorCancellable))
        {
            return;
        }

        if (printer.headProperty().get().headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD)
        {
            printer.goToTargetNozzleHeaterTemperature(1);

            waitOnNozzleTemperature(1);
            if (PrinterUtils.waitOnMacroFinished(printer, userOrErrorCancellable))
            {
                return;
            }
        }

        printer.miniPurge(true, userOrErrorCancellable, 1);
        if (PrinterUtils.waitOnMacroFinished(printer, userOrErrorCancellable))
        {
            return;
        }

        if (headReferenceData
                != null)
        {
            steno.debug("Setting B offsets to defaults ("
                    + headReferenceData.getNozzles().get(0).getMinBOffset()
                    + " - "
                    + headReferenceData.getNozzles().get(1).getMinBOffset()
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
                    savedHeadData.getFilamentID(0),
                    savedHeadData.getFilamentID(1),
                    0,
                    0,
                    0,
                    headReferenceData.getNozzles().get(1).
                    getDefaultBOffset(),
                    savedHeadData.getLastFilamentTemperature(0),
                    savedHeadData.getLastFilamentTemperature(1),
                    savedHeadData.getHeadHours());
        } else
        {
            // We shouldn't ever get here, but just in case...
            steno.debug("Setting B offsets to safe values ("
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
                    savedHeadData.getFilamentID(0),
                    savedHeadData.getFilamentID(1),
                    0,
                    0,
                    0,
                    -1,
                    savedHeadData.getLastFilamentTemperature(0),
                    savedHeadData.getLastFilamentTemperature(1),
                    savedHeadData.getHeadHours());
        }

        printer.readHeadEEPROM();

        printer.goToXYZPosition(PrintBed.getPrintVolumeCentre().getX(),
                PrintBed.getPrintVolumeCentre().getZ(),
                -PrintBed.getPrintVolumeCentre().getY());
        if (PrinterUtils.waitOnBusy(printer, userOrErrorCancellable))
        {
            return;
        }

        printer.goToTargetNozzleHeaterTemperature(
                0);
        if (printer.headProperty()
                .get().headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD)
        {
            printer.goToTargetNozzleHeaterTemperature(1);
        }

        waitOnNozzleTemperature(
                0);
        if (PrinterUtils.waitOnMacroFinished(printer, userOrErrorCancellable))
        {
            return;
        }

        if (printer.headProperty()
                .get().headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD)
        {
            waitOnNozzleTemperature(1);
            if (PrinterUtils.waitOnMacroFinished(printer, userOrErrorCancellable))
            {
                return;
            }
        }

        printer.switchOnHeadLEDs();
    }

    private void waitOnNozzleTemperature(int nozzleNumber) throws InterruptedException
    {
        NozzleHeater nozzleHeater = printer.headProperty().get()
                .getNozzleHeaters().get(nozzleNumber);
        PrinterUtils.waitUntilTemperatureIsReached(
                nozzleHeater.nozzleTemperatureProperty(), null,
                nozzleHeater
                .nozzleTargetTemperatureProperty().get(), 5, 300, userOrErrorCancellable);
    }

    public void doNoMaterialCheckAction() throws CalibrationException, InterruptedException, PrinterException
    {
        extrudeUntilStall(0);
        pressuriseSystem(0);
        if (printer.headProperty().get().headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD)
        {
            extrudeUntilStall(1);
            pressuriseSystem(1);
        }
        Thread.sleep(3000);
        printer.selectNozzle(1);
        // 
        nozzlePosition.set(0);
    }

    public void doT0Extrusion() throws PrinterException, CalibrationException
    {
        printer.selectNozzle(0);
        printer.openNozzleFullyExtra();
        if (printer.headProperty().get().headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD)
        {
            printer.sendRawGCode("G1 D15 F300", false);
        } else
        {
            printer.sendRawGCode("G1 E15 F300", false);
        }
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
        printer.closeNozzleFully();
    }

    public void doT1Extrusion() throws PrinterException, CalibrationException
    {
        printer.selectNozzle(1);
        printer.openNozzleFullyExtra();
        if (printer.headProperty().get().headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD)
        {
            printer.sendRawGCode("G1 E15 F300", false);
        } else
        {
            printer.sendRawGCode("G1 E45 F600", false);
        }
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
        printer.closeNozzleFully();
    }

    public void doPreCalibrationPrimingFine() throws RoboxCommsException, CalibrationException
    {
        nozzlePosition.set(0);

        steno.debug("Setting B offsets to calibration values ("
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
                savedHeadData.getFilamentID(0),
                savedHeadData.getFilamentID(1),
                0,
                0,
                0,
                -bOffsetStartingValue,
                savedHeadData.getLastFilamentTemperature(0),
                savedHeadData.getLastFilamentTemperature(1),
                savedHeadData.getHeadHours());
        extrudeUntilStall(0);
        pressuriseSystem(0);
    }

    public void doCalibrateFineNozzle() throws CalibrationException
    {
        printer.gotoNozzlePosition(nozzlePosition.get());
    }

    public void doIncrementFineNozzlePosition() throws CalibrationException, InterruptedException
    {
        nozzlePosition.set(nozzlePosition.get() + 0.05f);
        steno.debug("(FINE) nozzle position set to " + nozzlePosition.get());
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
        steno.debug("(FILL) nozzle position set to " + nozzlePosition);
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
        pressuriseSystem(1);
    }

    public void doCalibrateFillNozzle() throws CalibrationException
    {
        printer.gotoNozzlePosition(nozzlePosition.get());
    }

    public void doFinaliseCalibrateFineNozzle() throws PrinterException, CalibrationException
    {
        printer.closeNozzleFully();
        // calculate offset
        steno.debug("(FINE) finalise nozzle position set at " + nozzlePosition.get());
        nozzle0BOffset = bOffsetStartingValue - 0.1f + nozzlePosition.get();
    }

    public void doFinaliseCalibrateFillNozzle() throws PrinterException, CalibrationException
    {
        printer.closeNozzleFully();
        // calculate offset
        steno.debug("(FILL) finalise nozzle position set at " + nozzlePosition);
        nozzle1BOffset = -bOffsetStartingValue + 0.1f - nozzlePosition.get();
    }

    public void doConfirmNoMaterialAction() throws PrinterException, RoboxCommsException, InterruptedException, CalibrationException
    {
        // set to just about to be open
        saveSettings();
        printer.closeNozzleFully();
        printer.selectNozzle(0);
        extrudeUntilStall(0);
        pressuriseSystem(0);
        pressuriseSystem(1);
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
                savedHeadData.getFilamentID(0),
                savedHeadData.getFilamentID(1),
                savedHeadData.getNozzle2XOffset(),
                savedHeadData.getNozzle2YOffset(),
                savedHeadData.getNozzle2ZOffset(),
                nozzle1BOffset,
                savedHeadData.getLastFilamentTemperature(0),
                savedHeadData.getLastFilamentTemperature(1),
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
                        savedHeadData.getFilamentID(0),
                        savedHeadData.getFilamentID(1),
                        savedHeadData.getNozzle2XOffset(),
                        savedHeadData.getNozzle2YOffset(),
                        savedHeadData.getNozzle2ZOffset(),
                        savedHeadData.getNozzle2BOffset(),
                        savedHeadData.getLastFilamentTemperature(0),
                        savedHeadData.getLastFilamentTemperature(1),
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
            if (nozzleNumber == 0
                    && printer.headProperty().get().headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD)
            {
                printer.sendRawGCode("G36 D100 F800", false);
            } else
            {
                printer.sendRawGCode("G36 E100 F800", false);
            }
            PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);

        } catch (PrinterException ex)
        {
            steno.error("Error in needle valve priming");
        }
    }

    public ReadOnlyFloatProperty getBPositionGUITProperty()
    {
        return bPositionGUIT;
    }

    private void pressuriseSystem(int nozzleNumber)
    {
        if (nozzleNumber == 0
                && printer.headProperty().get().headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD)
        {
            printer.sendRawGCode("G1 D4 F400", false);
        } else
        {
            printer.sendRawGCode("G1 E4 F400", false);
        }
        PrinterUtils.waitOnBusy(printer, userOrErrorCancellable);
    }

    public void doFinishedAction()
    {
        try
        {
            printerErrorHandler.deregisterForPrinterErrors();
            saveSettings();
            resetPrinter();
        } catch (CalibrationException | RoboxCommsException | PrinterException ex)
        {
            steno.error("Error in finished action: " + ex);
        }
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }

    public void doFailedAction()
    {
        // this can be called twice if an error occurs
        if (failedActionPerformed)
        {
            return;
        }

        failedActionPerformed = true;
        try
        {
            printerErrorHandler.deregisterForPrinterErrors();
            restoreHeadData();
            resetPrinter();
            abortAnyOngoingPrint();
            printer.setPrinterStatus(PrinterStatus.IDLE);
        } catch (CalibrationException | PrinterException ex)
        {
            steno.error("Error in failed action: " + ex);
        }
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
            doFailedAction();
        } catch (Exception ex)
        {
            ex.printStackTrace();
            steno.error("error resetting printer " + ex);
        }
    }

    private void resetPrinter() throws PrinterException, CalibrationException
    {
        printerErrorHandler.deregisterForPrinterErrors();
        printer.closeNozzleFully();
        steno.debug("Switch heaters and lights off");
        switchHeatersAndHeadLightOff();
        steno.debug("bring bed to front");
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
                steno.debug("Cancel ongoing job");
                printer.cancel(null);
            } else
            {
                steno.debug("Nothing ongoing to cancel");
            }
        } catch (PrinterException ex)
        {
            steno.error("Failed to abort print - " + ex.getMessage());
        }
    }
}
