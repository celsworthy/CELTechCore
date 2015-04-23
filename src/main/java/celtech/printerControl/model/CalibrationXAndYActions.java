/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model;

import celtech.configuration.ApplicationConfiguration;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.GCodeMacros;
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
public class CalibrationXAndYActions extends StateTransitionActions
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationXAndYActions.class.getName());

    private final Printer printer;
    private HeadEEPROMDataResponse savedHeadData;
    private int xOffset = 0;
    private int yOffset = 0;
    private final CalibrationPrinterErrorHandler printerErrorHandler;

    public CalibrationXAndYActions(Printer printer, Cancellable userCancellable, Cancellable errorCancellable)
    {
        super(userCancellable, errorCancellable);
        this.printer = printer;

        printerErrorHandler = new CalibrationPrinterErrorHandler(printer, errorCancellable);
    }

    public void doSaveHead() throws PrinterException, RoboxCommsException, InterruptedException, CalibrationException
    {
        printerErrorHandler.registerForPrinterErrors();

        printer.setPrinterStatus(PrinterStatus.CALIBRATING_NOZZLE_ALIGNMENT);
        savedHeadData = printer.readHeadEEPROM();
    }

    public void doPrintPattern() throws PrinterException, RoboxCommsException, InterruptedException, CalibrationException
    {
//        Thread.sleep(3000);
        printer.executeGCodeFile(ApplicationConfiguration.getApplicationModelDirectory().concat(
            "rbx_test_xy-offset-1_roboxised.gcode"), false);
        PrinterUtils.waitOnMacroFinished(printer, userOrErrorCancellable);
        // keep bed temp up to keep remaining part on the bed
//        printer.goToTargetBedTemperature();
    }

    public void doSaveSettingsAndPrintCircle() throws PrinterException, InterruptedException, CalibrationException
    {
        saveSettings();
//        Thread.sleep(3000);
        printer.executeGCodeFile(GCodeMacros.getFilename("rbx_test_xy-offset-2_roboxised"), false);
        PrinterUtils.waitOnMacroFinished(printer, userOrErrorCancellable);
    }

    public void doFinishedAction() throws PrinterException
    {
        saveSettings();
        switchHeatersOffAndRaiseHead();
        printerErrorHandler.deregisterForPrinterErrors();
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }

    public void doFailedAction() throws PrinterException, RoboxCommsException
    {
        restoreHeadData();
        switchHeatersOffAndRaiseHead();
        printerErrorHandler.deregisterForPrinterErrors();
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }

    public void cancelOngoingActionAndResetPrinter()
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
            steno.debug("Cancelling macro");
            if (printer.canCancelProperty().get())
            {
                printer.cancel(null);
            }
        } catch (PrinterException ex)
        {
            steno.error("Cannot cancel print: " + ex);
        }
        try
        {
            doFailedAction();
        } catch (RoboxCommsException | PrinterException ex)
        {
            steno.error("Error in cancelling calibration: " + ex);
        } 
    }

    private void switchHeatersOffAndRaiseHead() throws PrinterException
    {
        printer.switchBedHeaterOff();
        printer.switchAllNozzleHeatersOff();
        printer.switchOffHeadLEDs();
        printer.switchToAbsoluteMoveMode();
        printer.goToZPosition(25);
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

    private void saveSettings()
    {

        // F and 6 are zero values
        float nozzle1XCorrection = -xOffset * 0.025f;
        float nozzle2XCorrection = xOffset * 0.025f;

        float nozzle1YCorrection = (yOffset - 6) * 0.025f;
        float nozzle2YCorrection = -(yOffset - 6) * 0.025f;

        steno.info(String.format("Saving XY with correction %1.2f %1.2f %1.2f %1.2f ",
                                 nozzle1XCorrection,
                                 nozzle2XCorrection, nozzle1YCorrection, nozzle2YCorrection));

        try
        {
            printer.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                            savedHeadData.getUniqueID(),
                                            savedHeadData.getMaximumTemperature(),
                                            savedHeadData.getBeta(),
                                            savedHeadData.getTCal(),
                                            savedHeadData.getNozzle1XOffset()
                                            + nozzle1XCorrection,
                                            savedHeadData.getNozzle1YOffset()
                                            + nozzle1YCorrection,
                                            savedHeadData.getNozzle1ZOffset(),
                                            savedHeadData.getNozzle1BOffset(),
                                            savedHeadData.getNozzle2XOffset()
                                            + nozzle2XCorrection,
                                            savedHeadData.getNozzle2YOffset()
                                            + nozzle2YCorrection,
                                            savedHeadData.getNozzle2ZOffset(),
                                            savedHeadData.getNozzle2BOffset(),
                                            savedHeadData.getLastFilamentTemperature(),
                                            savedHeadData.getHeadHours());

        } catch (RoboxCommsException ex)
        {
            steno.error("Error in needle valve calibration - saving settings");
        }
    }

    public void setXOffset(String xStr)
    {
        switch (xStr)
        {
            case "A":
                xOffset = -5;
                break;
            case "B":
                xOffset = -4;
                break;
            case "C":
                xOffset = -3;
                break;
            case "D":
                xOffset = -2;
                break;
            case "E":
                xOffset = -1;
                break;
            case "F":
                xOffset = 0;
                break;
            case "G":
                xOffset = 1;
                break;
            case "H":
                xOffset = 2;
                break;
            case "I":
                xOffset = 3;
                break;
            case "J":
                xOffset = 4;
                break;
            case "K":
                xOffset = 5;
                break;
        }
    }

    public void setYOffset(int yOffset)
    {
        this.yOffset = yOffset;
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
