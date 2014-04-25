/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.coreUI.DisplayManager;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrationNozzleBPageController implements Initializable
{

    private Stenographer steno = StenographerFactory.getStenographer(CalibrationNozzleBPageController.class.getName());

    private enum CalibrationState
    {

        CANCEL, IDLE, INITIALISING, HEATING, PRIMING, NO_MATERIAL_CHECK, CALIBRATE_NOZZLE
    };

    private CalibrationState state = CalibrationState.IDLE;

    private String initialisingMessage = null;
    private String heatingMessage = null;
    private String readyToBeginMessage = null;
    private String primingNozzleMessage = null;
    private String pressAKeyMessage = null;
    private String pressAKeyToContinueMessage = null;
    private String preparingExtruderMessage = null;
    private ResourceBundle i18nBundle = null;
    private Printer printerToUse = null;
    private int currentNozzleNumber = 0;

    private HeadEEPROMDataResponse savedHeadData = null;

    private boolean cancelled = false;

    @FXML
    private Text calibrationInstruction;

    @FXML
    private Button startCalibrationButton;

    @FXML
    private Button cancelCalibrationButton;

    @FXML
    private Text calibrationStatus;

    @FXML
    private Button yesButton;

    @FXML
    private Button noButton;

    @FXML
    void startCalibration(ActionEvent event)
    {
        cancelled = false;
        setState(CalibrationState.INITIALISING);
    }

    @FXML
    void cancelCalibration(ActionEvent event)
    {
        cancelled = true;
        setState(CalibrationState.CANCEL);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        i18nBundle = DisplayManager.getLanguageBundle();

        initialisingMessage = i18nBundle.getString("calibrationPanel.BCalibrationInitialising");
        heatingMessage = i18nBundle.getString("calibrationPanel.BCalibrationHeating");
        pressAKeyMessage = i18nBundle.getString("calibrationPanel.pressAKey");
        preparingExtruderMessage = i18nBundle.getString("calibrationPanel.preparingExtruder");
        pressAKeyToContinueMessage = i18nBundle.getString("calibrationPanel.pressAKeyToContinue");
        readyToBeginMessage = i18nBundle.getString("calibrationPanel.readyToBeginTest");
        primingNozzleMessage = i18nBundle.getString("calibrationPanel.primingNozzle");
        
        StatusScreenState statusScreenState = StatusScreenState.getInstance();
        statusScreenState.currentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {
            @Override
            public void changed(ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue)
            {
                printerToUse = newValue;
            }
        });

        printerToUse = statusScreenState.getCurrentlySelectedPrinter();
        
        setState(CalibrationState.IDLE);
    }

    private void waitOnBusy() throws InterruptedException
    {
        Thread.sleep(100);
        while (printerToUse.busyProperty().get() == true && cancelled == false)
        {
            Thread.sleep(100);
        }
    }

    private void waitUntilNozzleReaches(int temperature, int tolerance) throws InterruptedException
    {
        int minTemp = temperature - tolerance;
        int maxTemp = temperature + tolerance;

        while ((printerToUse.extruderTemperatureProperty().get() < minTemp || printerToUse.extruderTemperatureProperty().get() > maxTemp) && cancelled == false)
        {
            Thread.sleep(100);
        }
    }

    private void setState(CalibrationState state)
    {
        this.state = state;
        switch (state)
        {
            case CANCEL:
                try
                {
                    printerToUse.transmitWriteHeadEEPROM(savedHeadData.getHeadTypeCode(),
                            savedHeadData.getUniqueID(),
                            savedHeadData.getMaximumTemperature(),
                            savedHeadData.getThermistorBeta(),
                            savedHeadData.getThermistorTCal(),
                            savedHeadData.getNozzle1XOffset(),
                            savedHeadData.getNozzle1YOffset(),
                            savedHeadData.getNozzle1ZOffset(),
                            savedHeadData.getNozzle1BOffset(),
                            savedHeadData.getNozzle2XOffset(),
                            savedHeadData.getNozzle2YOffset(),
                            savedHeadData.getNozzle2ZOffset(),
                            savedHeadData.getNozzle2BOffset(),
                            savedHeadData.getHoursUsed());
                    setState(CalibrationState.IDLE);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }
                break;
            case IDLE:
                startCalibrationButton.setVisible(true);
                cancelCalibrationButton.setVisible(false);
                calibrationInstruction.setText("");
                calibrationStatus.setText(readyToBeginMessage);
                break;
            case INITIALISING:
                cancelled = false;
                currentNozzleNumber = 0;
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                calibrationStatus.setText(initialisingMessage);
                calibrationInstruction.setText("");

                try
                {
                    savedHeadData = printerToUse.transmitReadHeadEEPROM();

                    printerToUse.transmitWriteHeadEEPROM(savedHeadData.getHeadTypeCode(),
                            savedHeadData.getUniqueID(),
                            savedHeadData.getMaximumTemperature(),
                            savedHeadData.getThermistorBeta(),
                            savedHeadData.getThermistorTCal(),
                            0,
                            0,
                            0,
                            0.9f,
                            0,
                            0,
                            0,
                            -0.9f,
                            savedHeadData.getHoursUsed());

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
                    setState(CalibrationState.HEATING);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                } catch (InterruptedException ex)
                {
                    steno.error("Interrrupted during needle valve calibration - mode=" + state.name());
                }
                break;
            case HEATING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                calibrationStatus.setText(heatingMessage);
                calibrationInstruction.setText("");

                try
                {
                    printerToUse.transmitDirectGCode("M104", false);
                    waitUntilNozzleReaches(printerToUse.getNozzleTargetTemperature(), 5);
                    setState(CalibrationState.PRIMING);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                } catch (InterruptedException ex)
                {
                    steno.error("Interrrupted during needle valve calibration - mode=" + state.name());
                }
                break;

            case PRIMING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                calibrationStatus.setText(primingNozzleMessage);
                calibrationInstruction.setText("");

                try
                {
                    printerToUse.transmitDirectGCode("M909 S4", false);
                    waitOnBusy();

                    printerToUse.transmitDirectGCode("T" + currentNozzleNumber, false);

                    AckResponse errors = printerToUse.transmitReportErrors();
                    if (errors.isError())
                    {
                        printerToUse.transmitResetErrors();
                    }

                    while (errors.isEFilamentSlipError() == false && cancelled == false)
                    {
                        printerToUse.transmitDirectGCode("G0 E10", false);
                        waitOnBusy();

                        errors = printerToUse.transmitReportErrors();
                    }

                    printerToUse.transmitResetErrors();

                    setState(CalibrationState.NO_MATERIAL_CHECK);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                } catch (InterruptedException ex)
                {
                    steno.error("Interrrupted during needle valve calibration - mode=" + state.name());
                }
                break;

            case NO_MATERIAL_CHECK:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                calibrationStatus.setText(primingNozzleMessage);
                calibrationInstruction.setText("");
                break;
        }
    }

}
