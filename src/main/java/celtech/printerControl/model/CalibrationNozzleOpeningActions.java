/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model;

import celtech.configuration.HeaterMode;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.utils.PrinterUtils;
import celtech.utils.tasks.Cancellable;
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
    private float nozzlePosition = 0;

    private final Cancellable cancellable = new Cancellable();

    public CalibrationNozzleOpeningActions(Printer printer)
    {
        this.printer = printer;
        cancellable.cancelled = false;
    }

    public boolean doHeatingAction() throws RoboxCommsException, PrinterException, InterruptedException
    {
        cancellable.cancelled = false;

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
            printer.runMacroWithoutPurgeCheck("Home_all");
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
                            .nozzleFirstLayerTargetTemperatureProperty().get(), 5, 300);
                    } else
                    {
                        NozzleHeater nozzleHeater = printer.headProperty().get()
                            .getNozzleHeaters().get(0);
                        PrinterUtils.waitUntilTemperatureIsReached(
                            nozzleHeater.nozzleTemperatureProperty(), null,
                            nozzleHeater
                            .nozzleTargetTemperatureProperty().get(), 5, 300);
                    }
                    printer.switchOnHeadLEDs();
                }
            }

        }
        return true;
    }

    public boolean doNoMaterialCheckAction()
    {
        extrudeUntilStall(0);
        return true;
    }

    public boolean doPreCalibrationPrimingFine() throws RoboxCommsException
    {
        nozzlePosition = 0;

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
        return true;
    }

    public boolean doCalibrateFineNozzle()
    {
        printer.gotoNozzlePosition(nozzlePosition);
        return true;
    }

    public boolean doIncrementFineNozzlePosition()
    {
        nozzlePosition += 0.05;
        steno.info("(FINE) nozzle position set to " + nozzlePosition);
        if (nozzlePosition >= 2f)
        {
            return false;
        }
        printer.gotoNozzlePosition(nozzlePosition);
        return true;
    }

    public boolean doIncrementFillNozzlePosition()
    {
        nozzlePosition += 0.05;
        steno.info("(FILL) nozzle position set to " + nozzlePosition);
        if (nozzlePosition >= 2f)
        {
            return false;
        }
        printer.gotoNozzlePosition(nozzlePosition);
        return true;
    }

    public boolean doPreCalibrationPrimingFill() throws RoboxCommsException
    {
        nozzlePosition = 0;
        extrudeUntilStall(1);
        return true;
    }

    public boolean doCalibrateFillNozzle()
    {
        printer.gotoNozzlePosition(nozzlePosition);
        return true;
    }

    public boolean doFinaliseCalibrateFineNozzle() throws PrinterException
    {
        printer.closeNozzleFully();
        // calculate offset
        steno.info("(FINE) finalise nozzle position set at " + nozzlePosition);
        nozzle0BOffset = bOffsetStartingValue - 0.1f + nozzlePosition;
        return true;
    }

    public boolean doFinaliseCalibrateFillNozzle() throws PrinterException
    {
        printer.closeNozzleFully();
        // calculate offset
        steno.info("(FILL) finalise nozzle position set at " + nozzlePosition);
        nozzle1BOffset = -bOffsetStartingValue + 0.1f - nozzlePosition;
        return true;
    }

    public boolean doConfirmNoMaterialAction()
    {
        nozzlePosition = 0;
        return true;
    }

    public boolean doConfirmMaterialExtrudingAction() throws PrinterException
    {
        printer.selectNozzle(0);
        printer.openNozzleFully();
        printer.sendRawGCode("G1 E10 F75", false);
        PrinterUtils.waitOnBusy(printer, cancellable);
        printer.selectNozzle(1);
        printer.openNozzleFully();
        printer.sendRawGCode("G1 E10 F100", false);
        PrinterUtils.waitOnBusy(printer, cancellable);
        return true;
    }

    public boolean doFinishedAction() throws RoboxCommsException
    {
        saveSettings();
        turnHeaterAndLEDSOff();
        printer.setPrinterStatus(PrinterStatus.IDLE);
        return true;
    }

    public boolean doFailedAction() throws RoboxCommsException
    {
        restoreHeadState();
        turnHeaterAndLEDSOff();
        printer.setPrinterStatus(PrinterStatus.IDLE);
        return true;
    }

    public boolean doCancelledAction() throws RoboxCommsException
    {
        cancellable.cancelled = true;
        restoreHeadState();
        turnHeaterAndLEDSOff();
        printer.setPrinterStatus(PrinterStatus.IDLE);
        return true;
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

    private boolean extrudeUntilStall(int nozzleNumber)
    {
        boolean success = false;
        try
        {
            // select nozzle
            printer.selectNozzle(nozzleNumber);
            // G36 = extrude until stall E700 = top extruder F2000 = feed rate mm/min (?)
            // extrude either requested volume or until filament slips
            printer.sendRawGCode("G36 E700 F2000", false);

            PrinterUtils.waitOnBusy(printer, cancellable);

            success = true;
        } catch (PrinterException ex)
        {
            steno.error("Error in needle valve priming");
        }
        return success;
    }

}
