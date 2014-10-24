/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model;

import celtech.configuration.HeaterMode;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.NozzleData;
import celtech.coreUI.controllers.panels.CalibrationXAndYHelper;
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
public class CalibrationNozzleHeightActions
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationXAndYHelper.class.getName());

    private final Printer printer;
    private HeadEEPROMDataResponse savedHeadData;
    private double zco;
    private double zDifference;

    public CalibrationNozzleHeightActions(Printer printer)
    {
        this.printer = printer;
    }

    public boolean doInitialiseAndHeatBedAction() throws InterruptedException, PrinterException, RoboxCommsException
    {
        boolean success = false;
        zco = 0;
        zDifference = 0;
        
        printer.setPrinterStatus(PrinterStatus.CALIBRATING_NOZZLE_HEIGHT);

        savedHeadData = printer.readHeadEEPROM();

        zco = 0.5 * (savedHeadData.getNozzle1ZOffset()
            + savedHeadData.getNozzle2ZOffset());
        zDifference = savedHeadData.getNozzle2ZOffset()
            - savedHeadData.getNozzle1ZOffset();

        clearZOffsetsOnHead();
        success = heatBed(success);
        return success;

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

    private boolean heatBed(boolean success) throws InterruptedException, PrinterException
    {
        printer.goToTargetNozzleTemperature();
        printer.getPrintEngine().printGCodeFile(GCodeMacros.getFilename("Home_all"), true);
        if (PrinterUtils.waitOnMacroFinished(printer, (Cancellable) null) == false)
        {
            printer.goToTargetNozzleTemperature();
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
            if (PrinterUtils.waitOnBusy(printer, (Cancellable) null) == false)
            {
                printer.switchOnHeadLEDs();
                success = true;
            }
        }
        return success;
    }
    
    public boolean doHomeZAction() {
        printer.homeZ();
        return true;
    }

    public boolean doLiftHeadAction() throws PrinterException
    {
        printer.switchToAbsoluteMoveMode();
        printer.goToZPosition(30);
//        printer.goToOpenDoorPosition(null);
        return true;
    }

    public boolean doMeasureZDifferenceAction() throws PrinterException
    {
        boolean success = false;

        float[] zDifferenceMeasurement = new float[3];

        float sumOfZDifferences = 0;
        boolean failed = false;
        int testCounter = 0;
        boolean testFinished = false;

        while (testCounter < 3 && !testFinished)
        {
            for (int i = 0; i < 3; i++)
            {
                printer.selectNozzle(0);
                PrinterUtils.waitOnBusy(printer, (Cancellable) null);
                printer.homeZ();
                PrinterUtils.waitOnBusy(printer, (Cancellable) null);
                printer.goToZPosition(5);
                PrinterUtils.waitOnBusy(printer, (Cancellable) null);
                printer.selectNozzle(1);
                PrinterUtils.waitOnBusy(printer, (Cancellable) null);
                printer.probeBed();
                PrinterUtils.waitOnBusy(printer, (Cancellable) null);
                printer.goToZPosition(5);
                PrinterUtils.waitOnBusy(printer, (Cancellable) null);
                String measurementString = printer.getZDelta();
                measurementString = measurementString.replaceFirst("Zdelta:", "").replaceFirst(
                    "\nok", "");
                try
                {
                    zDifferenceMeasurement[i] = Float.valueOf(measurementString);

                    if (i > 0)
                    {
                        if (Math.abs(zDifferenceMeasurement[i] - zDifferenceMeasurement[i - 1])
                            > 0.02)
                        {
                            failed = true;
                            break;
                        }
                    }
                    sumOfZDifferences += zDifferenceMeasurement[i];
                    steno.info("Z Offset measurement " + i + " was " + zDifferenceMeasurement[i]);
                } catch (NumberFormatException ex)
                {
                    steno.error("Failed to convert z offset measurement from Robox - "
                        + measurementString);
                    failed = true;
                    break;
                }
            }

            if (failed == false)
            {
                zDifference = sumOfZDifferences / 3;

                steno.info("Average Z Offset was " + zDifference);

                success = true;
                testFinished = true;
            } else
            {
                sumOfZDifferences = 0;
                zDifferenceMeasurement = new float[3];
                failed = false;
            }

            testCounter++;
        }

        printer.selectNozzle(0);
        PrinterUtils.waitOnBusy(printer, (Cancellable) null);
        return success;

    }
    
    public boolean doIncrementZAction() {
         zco += 0.05;
         printer.goToZPosition(zco);
         return true;
    }
    
    public boolean doDecrementZAction() {
         zco -= 0.05;
         printer.goToZPosition(zco);
         return true;
    }    
    
    public boolean doFinishedAction() throws PrinterException, RoboxCommsException
    {
        saveSettings();
        printer.setPrinterStatus(PrinterStatus.IDLE);
        switchHeaterOffAndRaiseHead();
        return true;
    }

    public boolean doFailedAction() throws PrinterException, RoboxCommsException
    {
        printer.setPrinterStatus(PrinterStatus.IDLE);
        restoreHeadData();
        switchHeaterOffAndRaiseHead();
        
        return true;
    }
    
    public boolean doCancelledAction() throws PrinterException, RoboxCommsException
    {
        return doFailedAction();
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

    public boolean saveSettings() throws RoboxCommsException
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
                                        (float) (-zco - (0.5 * zDifference)),
                                        savedHeadData.getNozzle1BOffset(),
                                        savedHeadData.getNozzle2XOffset(),
                                        savedHeadData.getNozzle2YOffset(),
                                        (float) (-zco + (0.5 * zDifference)),
                                        savedHeadData.getNozzle2BOffset(),
                                        savedHeadData.getLastFilamentTemperature(),
                                        savedHeadData.getHeadHours());
        return true;
    }

    public double getZco()
    {
        return zco;
    }
}
