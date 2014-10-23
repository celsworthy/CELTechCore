/*
 * Copyright 2014 CEL UK
 */
package celtech.services.calibration;

import celtech.coreUI.controllers.panels.CalibrationXAndYHelper;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.utils.PrinterUtils;
import celtech.utils.tasks.Cancellable;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class CalibrationXAndYActions
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationXAndYHelper.class.getName());

    private final Printer printer;
    private HeadEEPROMDataResponse savedHeadData;
    private int xOffset = 0;
    private int yOffset = 0;

    public CalibrationXAndYActions(Printer printer)
    {
        this.printer = printer;
    }

    public boolean doSaveHeadAndPrintPattern() throws PrinterException, RoboxCommsException
    {
        savedHeadData = printer.readHeadEEPROM();
        printer.runMacro("rbx_XY_offset_roboxised");
//                printer.runMacro("tiny_robox");
        boolean interrupted = PrinterUtils.waitOnMacroFinished(printer, (Cancellable) null);
        return !interrupted;
    }

    public boolean doSaveSettingsAndPrintCircle() throws PrinterException
    {
        saveSettings();
        printer.runMacro("rbx_XY_offset_roboxised");
//                printer.runMacro("tiny_robox");
        boolean interrupted = PrinterUtils.waitOnMacroFinished(printer, (Cancellable) null);
        return !interrupted;
    }

    public boolean doFinishedAction() throws PrinterException
    {
        saveSettings();
        return true;
    }

    public boolean doFailedAction() throws PrinterException, RoboxCommsException
    {
        restoreHeadData();
        switchHeaterOffAndRaiseHead();
        return true;
    }

    private void switchHeaterOffAndRaiseHead() throws PrinterException
    {
        printer.switchAllNozzleHeatersOff();
        printer.switchOffHeadLEDs();
        printer.switchToAbsoluteMoveMode();
        printer.goToZPosition(25);
    }

    private void restoreHeadData() throws RoboxCommsException
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

}
