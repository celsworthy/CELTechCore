/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.calibration;

import celtech.coreUI.DisplayManager;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.services.ControllableService;
import java.util.ResourceBundle;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrateBTask extends Task<Boolean> implements ControllableService
{

    private final Stenographer steno = StenographerFactory.getStenographer(CalibrateBTask.class.getName());
    private Printer printerToUse = null;
    private String progressTitle = null;
    private String initialisingMessage = null;
    private String heatingMessage = null;
    private String readyToBeginMessage = null;
    private String pressAKeyMessage = null;
    private String pressAKeyToContinueMessage = null;
    private String preparingExtruderMessage = null;
    private ResourceBundle i18nBundle = null;
    private boolean keyPressed = false;
    private int progressPercent = 0;
    private boolean lookingForKeyPress = false;

    public CalibrateBTask(Printer printerToUse)
    {
        this.printerToUse = printerToUse;

        i18nBundle = DisplayManager.getLanguageBundle();
        progressTitle = i18nBundle.getString("calibrationPanel.BCalibrationProgressTitle");
        initialisingMessage = i18nBundle.getString("calibrationPanel.BCalibrationInitialising");
        heatingMessage = i18nBundle.getString("calibrationPanel.BCalibrationHeating");
        pressAKeyMessage = i18nBundle.getString("calibrationPanel.pressAKey");
        preparingExtruderMessage = i18nBundle.getString("calibrationPanel.preparingExtruder");
        pressAKeyToContinueMessage = i18nBundle.getString("calibrationPanel.pressAKeyToContinue");
        readyToBeginMessage = i18nBundle.getString("calibrationPanel.readyToBeginTest");
    }

    @Override
    protected Boolean call() throws Exception
    {
        boolean success = false;
        float storedNozzle0Offset = 0f;

        RoboxCommsManager.getInstance().setSleepBetweenStatusChecks(printerToUse, 100);

        updateTitle(progressTitle);
        try
        {
            updateMessage(initialisingMessage);
            progressPercent += 5;
            updateProgress(progressPercent, 100);
            printerToUse.transmitDirectGCode("G90", false);
            waitOnBusy();
            printerToUse.transmitDirectGCode("G0 B0", false);
            waitOnBusy();
            printerToUse.transmitDirectGCode("G28 X Y", false);
            waitOnBusy();
            printerToUse.transmitDirectGCode("G0 X116.5 Y75", false);
            waitOnBusy();
            printerToUse.transmitDirectGCode("G28 Z", false);
            waitOnBusy();
            printerToUse.transmitDirectGCode("G0 Z15", false);
            waitOnBusy();

            updateMessage(heatingMessage);
            progressPercent += 5;
            updateProgress(progressPercent, 100);
            printerToUse.transmitDirectGCode("M104", false);
            waitUntilNozzleReaches(printerToUse.getNozzleTargetTemperature(), 5);


            progressPercent += 5;
            updateProgress(progressPercent, 100);

            HeadEEPROMDataResponse headData = printerToUse.transmitReadHeadEEPROM();
            printerToUse.transmitWriteHeadEEPROM(headData.getHeadTypeCode(),
                    headData.getUniqueID(),
                    headData.getMaximumTemperature(),
                    headData.getThermistorBeta(),
                    headData.getThermistorTCal(),
                    headData.getNozzle1XOffset(),
                    headData.getNozzle1YOffset(),
                    headData.getNozzle1ZOffset(),
                    0.9f,
                    headData.getNozzle2XOffset(),
                    headData.getNozzle2YOffset(),
                    headData.getNozzle2ZOffset(),
                    -0.9f,
                    headData.getHoursUsed());

            waitOnBusy();

            progressPercent += 5;
            updateProgress(progressPercent, 100);

            updateMessage(readyToBeginMessage);
            waitForKeyPress();

            boolean keepRunning = true;

            for (int nozzleNumber = 0; nozzleNumber < 2; nozzleNumber++)
            {
                if (keepRunning == false)
                {
                    break;
                }

                updateMessage(preparingExtruderMessage);
                printerToUse.transmitDirectGCode("T" + nozzleNumber, false);

                AckResponse errors = printerToUse.transmitReportErrors();
                if (errors.isError())
                {
                    printerToUse.transmitResetErrors();
                }

                while (errors.isEFilamentSlipError() == false && isCancelled() == false)
                {
                    printerToUse.transmitDirectGCode("G0 E10", false);
                    waitOnBusy();

                    errors = printerToUse.transmitReportErrors();
                }

                printerToUse.transmitResetErrors();

                progressPercent += 5;
                updateProgress(progressPercent, 100);

                updateMessage(pressAKeyMessage + " " + nozzleNumber);

                float offsetValue = 0f;

                lookingForKeyPress = true;
                while (keyPressed == false && offsetValue < 1.75)
                {
                    offsetValue += 0.05;

                    printerToUse.transmitDirectGCode("G0 B" + offsetValue, false);
                    waitOnBusy();
                    Thread.sleep(250);
                }
                lookingForKeyPress = false;

                if (keyPressed)
                {
                    storedNozzle0Offset = 0.8f + offsetValue;
                    printerToUse.transmitDirectGCode("G0 B0", false);
                    keyPressed = false;

                    if (nozzleNumber == 0)
                    {
                        updateMessage(pressAKeyToContinueMessage);
                        waitForKeyPress();
                    }

                    if (nozzleNumber == 0)
                    {
                        printerToUse.transmitWriteHeadEEPROM(headData.getHeadTypeCode(),
                                headData.getUniqueID(),
                                headData.getMaximumTemperature(),
                                headData.getThermistorBeta(),
                                headData.getThermistorTCal(),
                                headData.getNozzle1XOffset(),
                                headData.getNozzle1YOffset(),
                                headData.getNozzle1ZOffset(),
                                storedNozzle0Offset,
                                headData.getNozzle2XOffset(),
                                headData.getNozzle2YOffset(),
                                headData.getNozzle2ZOffset(),
                                headData.getNozzle2BOffset(),
                                headData.getHoursUsed());
                    } else
                    {
                        printerToUse.transmitWriteHeadEEPROM(headData.getHeadTypeCode(),
                                headData.getUniqueID(),
                                headData.getMaximumTemperature(),
                                headData.getThermistorBeta(),
                                headData.getThermistorTCal(),
                                headData.getNozzle1XOffset(),
                                headData.getNozzle1YOffset(),
                                headData.getNozzle1ZOffset(),
                                storedNozzle0Offset,
                                headData.getNozzle2XOffset(),
                                headData.getNozzle2YOffset(),
                                headData.getNozzle2ZOffset(),
                                -(0.8f + offsetValue),
                                headData.getHoursUsed());
                        success = true;
                    }

                    progressPercent += 5;
                    updateProgress(progressPercent, 100);
                } else
                {
                    keepRunning = false;
                }
            }

            printerToUse.transmitDirectGCode("G0 B0", false);
            printerToUse.transmitDirectGCode("M104 S0", false);

        } catch (RoboxCommsException ex)
        {
            steno.error("Error during B calibration routine");
        } finally
        {
            RoboxCommsManager.getInstance().setSleepBetweenStatusChecks(printerToUse, 500);
        }
        return success;
    }

    private void waitForKeyPress() throws InterruptedException
    {
        lookingForKeyPress = true;
        while (keyPressed == false && isCancelled() == false)
        {
            Thread.sleep(100);
        }
        keyPressed = false;
        lookingForKeyPress = false;
    }

    private void waitOnBusy() throws InterruptedException
    {
        Thread.sleep(100);
        while (printerToUse.busyProperty().get() == true && isCancelled() == false)
        {
            Thread.sleep(100);
        }
    }

    private void waitUntilNozzleReaches(int temperature, int tolerance) throws InterruptedException
    {
        int minTemp = temperature - tolerance;
        int maxTemp = temperature + tolerance;

        while ((printerToUse.extruderTemperatureProperty().get() < minTemp || printerToUse.extruderTemperatureProperty().get() > maxTemp) && isCancelled() == false)
        {
            Thread.sleep(100);
        }
    }

    @Override
    public boolean cancelRun()
    {
        return cancel();
    }

    public void keyPressed()
    {
        if (lookingForKeyPress)
        {
            keyPressed = true;
        }
    }
}
