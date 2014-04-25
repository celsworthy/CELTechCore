/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.utilityPanels;

import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.ProgressDialog;
import celtech.coreUI.controllers.CalibrationNozzleBPageController;
import celtech.coreUI.controllers.ModalDialogController;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.services.calibration.CalibrateBService;
import celtech.services.calibration.CalibrateBTask;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class CalibrationPanelController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(CalibrationPanelController.class.getName());
    private Printer connectedPrinter = null;
    private ModalDialog generalPurposeDialog = null;
    private ProgressDialog bCalibrationProgressDialog = null;
    private CalibrateBService bCalibrationService = new CalibrateBService();
    private CalibrateBTask bCalibrationTask = null;
    private String bCalibrationDialogTitle = null;
    private String calibrationFailed = null;
    private String calibrationSucceeded = null;
    private ResourceBundle i18nBundle = null;

    private Stage needleValvecalibrationStage = null;
    private CalibrationNozzleBPageController needleValveCalibrationController = null;

    private EventHandler<KeyEvent> keyPressHandler = new EventHandler<KeyEvent>()
    {
        @Override
        public void handle(KeyEvent event)
        {
            if (bCalibrationTask != null)
            {
                bCalibrationTask.keyPressed();
            }
        }
    };

    private EventHandler<WorkerStateEvent> calibrationFailedHandler = new EventHandler<WorkerStateEvent>()
    {
        @Override
        public void handle(WorkerStateEvent event)
        {
            try
            {
                connectedPrinter.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
            } catch (RoboxCommsException ex)
            {
                steno.error("Error switching heater off after failed calibration");
            }

            generalPurposeDialog.setMessage(bCalibrationDialogTitle);
            generalPurposeDialog.setMessage(calibrationFailed);
            generalPurposeDialog.show();
            bCalibrationTask = null;
        }
    };

    private EventHandler<WorkerStateEvent> calibrationCancelledHandler = new EventHandler<WorkerStateEvent>()
    {
        @Override
        public void handle(WorkerStateEvent event)
        {
            try
            {
                connectedPrinter.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
            } catch (RoboxCommsException ex)
            {
                steno.error("Error switching heater off after cancelled calibration");
            }
            bCalibrationTask = null;
        }
    };

    private EventHandler<WorkerStateEvent> calibrationSucceededHandler = new EventHandler<WorkerStateEvent>()
    {
        @Override
        public void handle(WorkerStateEvent event)
        {
            generalPurposeDialog.setMessage(bCalibrationDialogTitle);
            if ((boolean) (event.getSource().getValue()) == true)
            {
                generalPurposeDialog.setMessage(calibrationSucceeded);
            } else
            {
                generalPurposeDialog.setMessage(calibrationFailed);

            }
            generalPurposeDialog.show();
            bCalibrationTask = null;
        }
    };

    @FXML
    private AnchorPane container;

    @FXML
    void calibrateB(ActionEvent event)
    {

        if (needleValvecalibrationStage == null)
        {
            needleValvecalibrationStage = new Stage(StageStyle.UTILITY);
            URL needleValveCalibrationFXMLURL = ModalDialog.class.getResource(ApplicationConfiguration.fxmlResourcePath + "CalibrationNozzleBPage.fxml");
            FXMLLoader needleValveCalibrationLoader = new FXMLLoader(needleValveCalibrationFXMLURL, i18nBundle);
            try
            {
                Parent dialogBoxScreen = (Parent) needleValveCalibrationLoader.load();
                needleValveCalibrationController = (CalibrationNozzleBPageController) needleValveCalibrationLoader.getController();
                Scene dialogScene = new Scene(dialogBoxScreen, Color.TRANSPARENT);
                dialogScene.getStylesheets().add(ApplicationConfiguration.mainCSSFile);
                needleValvecalibrationStage.setScene(dialogScene);
                needleValvecalibrationStage.initOwner(DisplayManager.getMainStage());
                needleValvecalibrationStage.initModality(Modality.WINDOW_MODAL);
            } catch (IOException ex)
            {
                steno.error("Couldn't load needle valve calibration FXML");
            }
        }

        needleValvecalibrationStage.showAndWait();

//        if (bCalibrationTask == null)
//        {
//            bCalibrationTask = new CalibrateBTask(connectedPrinter);
//            bCalibrationTask.setOnFailed(calibrationFailedHandler);
//            bCalibrationTask.setOnSucceeded(calibrationSucceededHandler);
//            bCalibrationTask.setOnCancelled(calibrationCancelledHandler);
//            bCalibrationProgressDialog.associateControllableService(bCalibrationTask);
//
//            Thread calibrationThread = new Thread(bCalibrationTask);
//            calibrationThread.start();
//        }
    }

    @FXML
    void calibrateZOffset(ActionEvent event)
    {
        try
        {
            connectedPrinter.transmitStoredGCode(GCodeMacros.EJECT_ABS, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending eject ABS commands");
        }
    }

    @FXML
    void calibrateZContact(ActionEvent event)
    {
        try
        {
            connectedPrinter.transmitStoredGCode(GCodeMacros.EJECT_PLA, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending eject PLA commands");
        }
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        i18nBundle = DisplayManager.getLanguageBundle();
        bCalibrationDialogTitle = i18nBundle.getString("calibrationPanel.BCalibrationProgressTitle");
        calibrationFailed = i18nBundle.getString("calibrationPanel.calibrationFailed");
        calibrationSucceeded = i18nBundle.getString("calibrationPanel.calibrationSucceeded");

        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                generalPurposeDialog = new ModalDialog();
                generalPurposeDialog.addButton(DisplayManager.getLanguageBundle().getString("genericFirstLetterCapitalised.Ok"));
            }
        });

        bCalibrationProgressDialog = new ProgressDialog();
        bCalibrationProgressDialog.addKeyHandler(KeyEvent.KEY_PRESSED, keyPressHandler);

        StatusScreenState statusScreenState = StatusScreenState.getInstance();
        statusScreenState.currentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {
            @Override
            public void changed(ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue)
            {
                connectedPrinter = newValue;
            }
        });
    }
}
