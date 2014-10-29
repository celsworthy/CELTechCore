/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.calibration.old;

import celtech.Lookup;
import celtech.configuration.HeaterMode;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.NozzleHeater;
import celtech.printerControl.model.PrinterException;
import celtech.services.ControllableService;
import celtech.services.calibration.NozzleOffsetCalibrationState;
import celtech.utils.PrinterUtils;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrateNozzleOffsetTask extends Task<NozzleOffsetCalibrationStepResult> implements ControllableService
{

    private final Stenographer steno = StenographerFactory.getStenographer(CalibrateNozzleOffsetTask.class.getName());
    private NozzleOffsetCalibrationState desiredState = null;
    private int nozzleNumber = -1;

    private Printer printerToUse = null;
    private ResourceBundle i18nBundle = null;

    private Pattern zDeltaPattern = Pattern.compile(".*(?<offset>[\\-0-9.]+).*");
    private Matcher zDeltaMatcher = null;

    /**
     *
     * @param desiredState
     */
    public CalibrateNozzleOffsetTask(NozzleOffsetCalibrationState desiredState)
    {
        this.desiredState = desiredState;
    }

    /**
     *
     * @param desiredState
     * @param nozzleNumber
     */
    public CalibrateNozzleOffsetTask(NozzleOffsetCalibrationState desiredState, int nozzleNumber)
    {
        this.desiredState = desiredState;
        this.nozzleNumber = nozzleNumber;
    }

    @Override
    protected NozzleOffsetCalibrationStepResult call() throws Exception
    {
        boolean success = false;
        float returnFloat = 0;

        printerToUse = Lookup.getCurrentlySelectedPrinter();

        switch (desiredState)
        {
            case HEATING:
                try
                {
                    printerToUse.goToTargetNozzleTemperature();
                    printerToUse.runMacroWithoutPurgeCheck("Home_all");
                    if (PrinterUtils.waitOnMacroFinished(printerToUse, this) == false)
                    {
                        printerToUse.goToTargetNozzleTemperature();
                        if (printerToUse.headProperty().get()
                                    .getNozzleHeaters().get(0)
                                    .heaterModeProperty().get() == HeaterMode.FIRST_LAYER)
                        {
                            NozzleHeater nozzleHeater = printerToUse.headProperty().get()
                                        .getNozzleHeaters().get(0);
                            PrinterUtils.waitUntilTemperatureIsReached(
                                        nozzleHeater.nozzleTemperatureProperty(), this,
                                        nozzleHeater
                                        .nozzleFirstLayerTargetTemperatureProperty().get(), 5, 300);
                        } else
                        {
                            NozzleHeater nozzleHeater = printerToUse.headProperty().get()
                                        .getNozzleHeaters().get(0);
                                    PrinterUtils.waitUntilTemperatureIsReached(
                                        nozzleHeater.nozzleTemperatureProperty(), this,
                                        nozzleHeater
                                        .nozzleTargetTemperatureProperty().get(), 5, 300);
                        }
                        if (PrinterUtils.waitOnBusy(printerToUse, this) == false)
                        {
                            printerToUse.switchOnHeadLEDs();
                            success = true;
                        }
                    }
                } catch (PrinterException ex)
                {
                    steno.error("Error in nozzle offset calibration - mode=" + desiredState.name());
                } catch (InterruptedException ex)
                {
                    steno.error("Interrrupted during nozzle offset calibration - mode=" + desiredState.name());
                }

                break;

            case MEASURE_Z_DIFFERENCE:
                float[] zDifferenceMeasurement = new float[3];

                try
                {
                    float sumOfZDifferences = 0;
                    boolean failed = false;
                    int testCounter = 0;
                    boolean testFinished = false;

                    while (testCounter < 3 && !testFinished && isCancelled() == false)
                    {
                        for (int i = 0; i < 3; i++)
                        {
                            printerToUse.selectNozzle(0);
                            PrinterUtils.waitOnBusy(printerToUse, this);
                            printerToUse.homeZ();
                            PrinterUtils.waitOnBusy(printerToUse, this);
                            printerToUse.goToZPosition(5);
                            PrinterUtils.waitOnBusy(printerToUse, this);
                            printerToUse.selectNozzle(1);
                            PrinterUtils.waitOnBusy(printerToUse, this);
                            printerToUse.probeBed();
                            PrinterUtils.waitOnBusy(printerToUse, this);
                            printerToUse.goToZPosition(5);
                            PrinterUtils.waitOnBusy(printerToUse, this);
                            String measurementString = printerToUse.getZDelta();
                            measurementString = measurementString.replaceFirst("Zdelta:", "").replaceFirst("\nok", "");
                            try
                            {
                                zDifferenceMeasurement[i] = Float.valueOf(measurementString);

                                if (i > 0)
                                {
                                    if (Math.abs(zDifferenceMeasurement[i] - zDifferenceMeasurement[i - 1]) > 0.02)
                                    {
                                        failed = true;
                                        break;
                                    }
                                }
                                sumOfZDifferences += zDifferenceMeasurement[i];
                                steno.info("Z Offset measurement " + i + " was " + zDifferenceMeasurement[i]);
                            } catch (NumberFormatException ex)
                            {
                                steno.error("Failed to convert z offset measurement from Robox - " + measurementString);
                                failed = true;
                                break;
                            }
                        }

                        if (failed == false)
                        {
                            returnFloat = sumOfZDifferences / 3;

                            steno.info("Average Z Offset was " + returnFloat);

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

                    printerToUse.selectNozzle(0);
                    PrinterUtils.waitOnBusy(printerToUse, this);

                } catch (PrinterException ex)
                {
                    steno.error("Error in nozzle offset calibration - mode=" + desiredState.name());
                }
                break;
        }

        return new NozzleOffsetCalibrationStepResult(desiredState, returnFloat, success);
    }

//    private boolean extrudeUntilStall()
//    {
//        boolean success = false;
//        try
//        {
//            printerToUse.transmitDirectGCode("M909 S4", false);
//            PrinterUtils.waitOnBusy(printerToUse, this);
//
//            printerToUse.transmitDirectGCode("T" + nozzleNumber, false);
//
//            AckResponse errors = printerToUse.transmitReportErrors();
//            if (errors.isError())
//            {
//                printerToUse.transmitResetErrors();
//            }
//
//            while (errors.isEFilamentSlipError() == false && isCancelled() == false)
//            {
//                printerToUse.transmitDirectGCode("G0 E10", false);
//                PrinterUtils.waitOnBusy(printerToUse, this);
//
//                errors = printerToUse.transmitReportErrors();
//            }
//
//            printerToUse.transmitResetErrors();
//
//            printerToUse.transmitDirectGCode("M909 S70", false);
//            PrinterUtils.waitOnBusy(printerToUse, this);
//
//            success = true;
//        } catch (RoboxCommsException ex)
//        {
//            steno.error("Error in needle valve priming - mode=" + desiredState.name());
//        }
//        
//        return success;
//    }

//    private void waitUntilNozzleReaches(int temperature, int tolerance) throws InterruptedException
//    {
//        int minTemp = temperature - tolerance;
//        int maxTemp = temperature + tolerance;
//
//        while ((printerToUse.extruderTemperatureProperty().get() < minTemp || printerToUse.extruderTemperatureProperty().get() > maxTemp) && isCancelled() == false)
//        {
//            Thread.sleep(250);
//        }
//    }

    /**
     *
     * @return
     */
    @Override
    public boolean cancelRun()
    {
        return cancel();
    }
}
