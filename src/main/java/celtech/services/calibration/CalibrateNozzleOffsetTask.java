/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.calibration;

import celtech.configuration.HeaterMode;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.GCodeDataResponse;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.services.ControllableService;
import celtech.utils.PrinterUtils;
import java.util.ArrayList;
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

        StatusScreenState statusScreenState = StatusScreenState.getInstance();
        printerToUse = statusScreenState.getCurrentlySelectedPrinter();

        switch (desiredState)
        {
            case INITIALISING:
                try
                {
                    printerToUse.transmitStoredGCode("Home_all");
                    if (PrinterUtils.waitOnMacroFinished(printerToUse, this) == false)
                    {
                        StatusResponse response = printerToUse.transmitStatusRequest();
                        printerToUse.transmitDirectGCode("M104", false);
                        if (response.getNozzleHeaterMode() == HeaterMode.FIRST_LAYER)
                        {
                            waitUntilNozzleReaches(printerToUse.getNozzleFirstLayerTargetTemperature(), 5);
                        } else
                        {
                            waitUntilNozzleReaches(printerToUse.getNozzleTargetTemperature(), 5);
                        }

                        if (PrinterUtils.waitOnBusy(printerToUse, this) == false)
                        {
                            printerToUse.transmitDirectGCode(GCodeConstants.switchOnHeadLEDs, false);
                            success = true;
                        }
                    }
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in nozzle offset calibration - mode=" + desiredState.name());
                } catch (InterruptedException ex)
                {
                    steno.error("Interrrupted during nozzle offset calibration - mode=" + desiredState.name());
                }

                break;

            case MEASURE_Z_DIFFERENCE:
                // Do 10 z probes on T0
                boolean logToTranscript = false;
                final int numberOfProbeAccuracyTests = 10;
                final int numberOfNozzleHeightDifferenceTests = 11;

//                for (int startingNozzle = 0; startingNozzle <= 1; startingNozzle++)
//                {
//                    steno.info("T" + startingNozzle + " probe accuracy test");
//                    printerToUse.transmitDirectGCode("T" + startingNozzle, logToTranscript);
//                    PrinterUtils.waitOnBusy(printerToUse, this);
//                    printerToUse.transmitDirectGCode("G28 Z", logToTranscript);
//                    PrinterUtils.waitOnBusy(printerToUse, this);
//                    printerToUse.transmitDirectGCode("G0 Z5", logToTranscript);
//                    PrinterUtils.waitOnBusy(printerToUse, this);
//
//                    for (int testCount = 0; testCount < numberOfProbeAccuracyTests; testCount++)
//                    {
//                        printerToUse.transmitDirectGCode("G28 Z?", logToTranscript);
//                        PrinterUtils.waitOnBusy(printerToUse, this);
//                        String measurementString = printerToUse.transmitDirectGCode("M113", logToTranscript);
//                        measurementString = measurementString.replaceFirst("Zdelta:", "").replaceFirst("\nok", "");
//                        steno.info("Delta " + testCount + ": " + measurementString);
//                    }
//                }
                steno.info("Nozzle height difference test");
                printerToUse.transmitDirectGCode("T0", logToTranscript);
                PrinterUtils.waitOnBusy(printerToUse, this);

                // Level the gantry - manual rather than using the macro
                printerToUse.transmitDirectGCode("G0 X30 Y75", logToTranscript);
                PrinterUtils.waitOnBusy(printerToUse, this);
                printerToUse.transmitDirectGCode("G28 Z", logToTranscript);
                PrinterUtils.waitOnBusy(printerToUse, this);
                printerToUse.transmitDirectGCode("G0 Z5", logToTranscript);
                PrinterUtils.waitOnBusy(printerToUse, this);
                printerToUse.transmitDirectGCode("G0 X190 Y75", logToTranscript);
                PrinterUtils.waitOnBusy(printerToUse, this);
                printerToUse.transmitDirectGCode("G28 Z", logToTranscript);
                PrinterUtils.waitOnBusy(printerToUse, this);
                printerToUse.transmitDirectGCode("G0 Z5", logToTranscript);
                PrinterUtils.waitOnBusy(printerToUse, this);
                printerToUse.transmitDirectGCode("G38", logToTranscript);
                PrinterUtils.waitOnBusy(printerToUse, this);
                printerToUse.transmitDirectGCode("G0 X105 Y75", logToTranscript);
                PrinterUtils.waitOnBusy(printerToUse, this);

                PrinterUtils.waitOnBusy(printerToUse, this);
                printerToUse.transmitDirectGCode("G28 Z", logToTranscript);
                PrinterUtils.waitOnBusy(printerToUse, this);
                printerToUse.transmitDirectGCode("G0 Z5", logToTranscript);
                PrinterUtils.waitOnBusy(printerToUse, this);

                ArrayList<Float> t0Deltas = new ArrayList<>();
                t0Deltas.add(0f);
                ArrayList<Float> t1Deltas = new ArrayList<>();

                boolean flipFlop = false;
                for (int testCount = 0; testCount < numberOfNozzleHeightDifferenceTests; testCount++)
                {
                    int nozzleFrom = ((flipFlop == false) ? 0 : 1);
                    int nozzleTo = ((flipFlop == false) ? 1 : 0);

                    printerToUse.transmitDirectGCode("T" + nozzleTo, logToTranscript);
                    PrinterUtils.waitOnBusy(printerToUse, this);
                    printerToUse.transmitDirectGCode("G28 Z?", logToTranscript);
                    PrinterUtils.waitOnBusy(printerToUse, this);
                    String measurementString = printerToUse.transmitDirectGCode("M113", logToTranscript);
                    measurementString = measurementString.replaceFirst("Zdelta:", "").replaceFirst("\nok", "");
                    float deltaValue = Float.valueOf(measurementString.trim());

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
                
                printerToUse.transmitDirectGCode("T0", logToTranscript);
                
                float sumOfDeltas = 0;
                int numberOfSamples = ((numberOfNozzleHeightDifferenceTests + 1) / 2);
                
                for (int deltaCount = 0; deltaCount < numberOfSamples; deltaCount++)
                {
                    sumOfDeltas += t1Deltas.get(deltaCount) - t0Deltas.get(deltaCount);
                }

                float averageDifference = sumOfDeltas / numberOfSamples;
                returnFloat = averageDifference;

                steno.info("Average Z Offset was " + returnFloat);
                success = true;

                break;
        }

        return new NozzleOffsetCalibrationStepResult(desiredState, returnFloat, success);
    }

    private boolean extrudeUntilStall()
    {
        boolean success = false;
        try
        {
            printerToUse.transmitDirectGCode("M909 S4", false);
            PrinterUtils.waitOnBusy(printerToUse, this);

            printerToUse.transmitDirectGCode("T" + nozzleNumber, false);

            AckResponse errors = printerToUse.transmitReportErrors();
            if (errors.isError())
            {
                printerToUse.transmitResetErrors();
            }

            while (errors.isEFilamentSlipError() == false && isCancelled() == false)
            {
                printerToUse.transmitDirectGCode("G0 E10", false);
                PrinterUtils.waitOnBusy(printerToUse, this);

                errors = printerToUse.transmitReportErrors();
            }

            printerToUse.transmitResetErrors();

            printerToUse.transmitDirectGCode("M909 S70", false);
            PrinterUtils.waitOnBusy(printerToUse, this);

            success = true;
        } catch (RoboxCommsException ex)
        {
            steno.error("Error in needle valve priming - mode=" + desiredState.name());
        }

        return success;
    }

    private void waitUntilNozzleReaches(int temperature, int tolerance) throws InterruptedException
    {
        int minTemp = temperature - tolerance;
        int maxTemp = temperature + tolerance;

        while ((printerToUse.extruderTemperatureProperty().get() < minTemp || printerToUse.extruderTemperatureProperty().get() > maxTemp) && isCancelled() == false)
        {
            Thread.sleep(250);
        }
    }

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
