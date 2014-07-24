/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.utilityPanels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Notifier;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.DirectoryMemoryProperty;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.GCodeMacroButton;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.ProgressDialog;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.coreUI.controllers.panels.CalibrationNozzleBInsetPanelController;
import celtech.coreUI.controllers.panels.CalibrationNozzleOffsetInsetPanelController;
import celtech.printerControl.Printer;
import celtech.printerControl.PrinterStatusEnumeration;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.services.firmware.FirmwareLoadService;
import celtech.services.firmware.FirmwareLoadTask;
import celtech.services.printing.GCodePrintResult;
import celtech.services.printing.GCodePrintService;
import celtech.utils.PrinterUtils;
import java.io.File;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class MaintenancePanelController implements Initializable
{

    private static final Stenographer steno = StenographerFactory.getStenographer(MaintenancePanelController.class.getName());
    private Printer connectedPrinter = null;
    private ResourceBundle i18nBundle = null;

    private ProgressDialog firmwareUpdateProgress = null;
    private final FirmwareLoadService firmwareLoadService = new FirmwareLoadService();
    private FileChooser firmwareFileChooser = new FileChooser();

    private static Stage needleValvecalibrationStage = null;
    private static CalibrationNozzleBInsetPanelController needleValveCalibrationController = null;
    private static Stage offsetCalibrationStage = null;
    private static CalibrationNozzleOffsetInsetPanelController nozzleOffsetCalibrationController = null;

    private ProgressDialog gcodeUpdateProgress = null;
    private FileChooser gcodeFileChooser = new FileChooser();
    private final GCodePrintService gcodePrintService = new GCodePrintService();

    private ChangeListener<PrinterStatusEnumeration> printerStatusListener = new ChangeListener<PrinterStatusEnumeration>()
    {
        @Override
        public void changed(ObservableValue<? extends PrinterStatusEnumeration> observable, PrinterStatusEnumeration oldValue, PrinterStatusEnumeration newValue)
        {
            setButtonVisibility();
        }
    };

    private ChangeListener<Boolean> filamentLoadedListener = new ChangeListener<Boolean>()
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            setButtonVisibility();
        }
    };

    @FXML
    private AnchorPane container;

    @FXML
    private GCodeMacroButton YTestButton;

    @FXML
    private Button PurgeMaterialButton;

    @FXML
    private Button loadFirmwareGCodeMacroButton;

    @FXML
    private GCodeMacroButton T1CleanButton;

    @FXML
    private GCodeMacroButton EjectStuckMaterialButton;

    @FXML
    private GCodeMacroButton SpeedTestButton;

    @FXML
    private Button CalibrateOffsetButton;

    @FXML
    private Button sendGCodeStreamGCodeMacroButton;

    @FXML
    private Button CalibrateBButton;

    @FXML
    private GCodeMacroButton XTestButton;

    @FXML
    private GCodeMacroButton Level_YButton;

    @FXML
    private GCodeMacroButton T0CleanButton;

    @FXML
    private Label currentFirmwareField;

    @FXML
    private GCodeMacroButton LevelGantryButton;

    @FXML
    private Button sendGCodeSDGCodeMacroButton;

    @FXML
    private GCodeMacroButton ZTestButton;

    @FXML
    void macroButtonPress(ActionEvent event)
    {
        if (event.getSource() instanceof GCodeMacroButton)
        {
            GCodeMacroButton button = (GCodeMacroButton) event.getSource();
            String macroName = button.getMacroName();

            if (macroName != null)
            {
                try
                {
                    connectedPrinter.transmitStoredGCode(macroName);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error sending macro : " + macroName);
                }
            }
        }
    }

    @FXML
    void macroButtonPressNoPurgeCheck(ActionEvent event)
    {
        if (event.getSource() instanceof GCodeMacroButton)
        {
            GCodeMacroButton button = (GCodeMacroButton) event.getSource();
            String macroName = button.getMacroName();

            if (macroName != null)
            {
                try
                {
                    connectedPrinter.transmitStoredGCode(macroName, false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error sending macro : " + macroName);
                }
            }
        }
    }

    @FXML
    void loadFirmware(ActionEvent event)
    {
        firmwareFileChooser.setInitialFileName("Untitled");

        firmwareFileChooser.setInitialDirectory(new File(ApplicationConfiguration.getLastDirectory(DirectoryMemoryProperty.FIRMWARE)));

        final File file = firmwareFileChooser.showOpenDialog(DisplayManager.getMainStage());
        if (file != null)
        {
            firmwareLoadService.reset();
            firmwareLoadService.setPrinterToUse(connectedPrinter);
            firmwareLoadService.setFirmwareFileToLoad(file.getAbsolutePath());
            firmwareLoadService.start();
            ApplicationConfiguration.setLastDirectory(DirectoryMemoryProperty.FIRMWARE, file.getParentFile().getAbsolutePath());
        }
    }

    void readFirmwareVersion()
    {
        try
        {
            FirmwareResponse response = connectedPrinter.transmitReadFirmwareVersion();
            if (response != null)
            {
                currentFirmwareField.setText(response.getFirmwareRevision());
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Error reading firmware version");
        }
    }

    @FXML
    void calibrateB(ActionEvent event)
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.NOZZLE_OPEN_CALIBRATION);
    }

    public static void calibrateBAction()
    {
        if (needleValvecalibrationStage == null)
        {
            needleValvecalibrationStage = new Stage(StageStyle.UNDECORATED);
            URL needleValveCalibrationFXMLURL = ModalDialog.class.getResource(ApplicationConfiguration.fxmlResourcePath + "CalibrationNozzleBPage.fxml");
            FXMLLoader needleValveCalibrationLoader = new FXMLLoader(needleValveCalibrationFXMLURL, DisplayManager.getLanguageBundle());
            try
            {
                Parent dialogBoxScreen = (Parent) needleValveCalibrationLoader.load();
                needleValveCalibrationController = (CalibrationNozzleBInsetPanelController) needleValveCalibrationLoader.getController();
                Scene dialogScene = new Scene(dialogBoxScreen, Color.TRANSPARENT);
                dialogScene.getStylesheets().add(ApplicationConfiguration.mainCSSFile);
                needleValvecalibrationStage.setScene(dialogScene);
                needleValvecalibrationStage.initOwner(DisplayManager.getMainStage());
                needleValvecalibrationStage.initModality(Modality.WINDOW_MODAL);
                needleValvecalibrationStage.setOnCloseRequest(new EventHandler<WindowEvent>()
                {
                    @Override
                    public void handle(WindowEvent event)
                    {
                        needleValveCalibrationController.cancelCalibrationAction();
                    }
                });
            } catch (IOException ex)
            {
                steno.error("Couldn't load needle valve calibration FXML");
            }
        }

        needleValvecalibrationStage.showAndWait();
    }

    @FXML
    void calibrateZOffset(ActionEvent event)
    {

        calibrateZOffsetAction();
    }

    public static void calibrateZOffsetAction()
    {
        if (offsetCalibrationStage == null)
        {
            offsetCalibrationStage = new Stage(StageStyle.UNDECORATED);
            URL needleValveCalibrationFXMLURL = ModalDialog.class.getResource(ApplicationConfiguration.fxmlResourcePath + "CalibrationNozzleOffsetPage.fxml");
            FXMLLoader nozzleOffsetCalibrationLoader = new FXMLLoader(needleValveCalibrationFXMLURL, DisplayManager.getLanguageBundle());
            try
            {
                Parent dialogBoxScreen = (Parent) nozzleOffsetCalibrationLoader.load();
                nozzleOffsetCalibrationController = (CalibrationNozzleOffsetInsetPanelController) nozzleOffsetCalibrationLoader.getController();
                Scene dialogScene = new Scene(dialogBoxScreen, Color.TRANSPARENT);
                dialogScene.getStylesheets().add(ApplicationConfiguration.mainCSSFile);
                offsetCalibrationStage.setScene(dialogScene);
                offsetCalibrationStage.initOwner(DisplayManager.getMainStage());
                offsetCalibrationStage.initModality(Modality.WINDOW_MODAL);
            } catch (IOException ex)
            {
                steno.error("Couldn't load nozzle offset calibration FXML");
            }
        }

        offsetCalibrationStage.showAndWait();
    }

    @FXML
    void sendGCodeStream(ActionEvent event)
    {
        gcodeFileChooser.setInitialFileName("Untitled");

        gcodeFileChooser.setInitialDirectory(new File(ApplicationConfiguration.getLastDirectory(DirectoryMemoryProperty.MACRO)));

        final File file = gcodeFileChooser.showOpenDialog(container.getScene().getWindow());
        if (file != null)
        {
            if (connectedPrinter.getPrintQueue().getPrintStatus() == PrinterStatusEnumeration.IDLE)
            {
                gcodePrintService.reset();
                gcodePrintService.setPrintUsingSDCard(false);
                gcodePrintService.setPrinterToUse(connectedPrinter);
                gcodePrintService.setModelFileToPrint(file.getAbsolutePath());
                gcodePrintService.start();
            }
            ApplicationConfiguration.setLastDirectory(DirectoryMemoryProperty.MACRO, file.getParentFile().getAbsolutePath());
        }
    }

    @FXML
    void sendGCodeSD(ActionEvent event)
    {
        gcodeFileChooser.setInitialFileName("Untitled");

        gcodeFileChooser.setInitialDirectory(new File(ApplicationConfiguration.getLastDirectory(DirectoryMemoryProperty.MACRO)));

        final File file = gcodeFileChooser.showOpenDialog(container.getScene().getWindow());

        if (file != null)
        {
            if (connectedPrinter.getPrintQueue().getPrintStatus() == PrinterStatusEnumeration.IDLE)
            {
                connectedPrinter.getPrintQueue().printGCodeFile(file.getAbsolutePath(), true);
            }
            ApplicationConfiguration.setLastDirectory(DirectoryMemoryProperty.MACRO, file.getParentFile().getAbsolutePath());
        }
    }

    @FXML
    void purge(ActionEvent event)
    {
        if (connectedPrinter != null)
        {
            PrinterUtils.runPurge(connectedPrinter);
        }
    }

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        i18nBundle = DisplayManager.getLanguageBundle();

        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                gcodeUpdateProgress = new ProgressDialog(gcodePrintService);
                firmwareUpdateProgress = new ProgressDialog(firmwareLoadService);
            }
        });

        currentFirmwareField.setStyle("-fx-font-weight: bold;");

        gcodeFileChooser.setTitle(DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodeFileChooserTitle"));
        gcodeFileChooser.getExtensionFilters()
            .addAll(
                new FileChooser.ExtensionFilter(DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodeFileDescription"), "*.gcode"));

        gcodePrintService.setOnSucceeded(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                GCodePrintResult result = (GCodePrintResult) (t.getSource().getValue());
                if (result.isSuccess())
                {
                    Notifier.showInformationNotification(DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodePrintSuccessTitle"),
                                                         DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodePrintSuccessMessage"));
                } else
                {
                    Notifier.showErrorNotification(DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodePrintFailedTitle"),
                                                   DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodePrintFailedMessage"));

                    steno.warning("In gcode print succeeded but with failure flag");
                }
            }
        });

        gcodePrintService.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                Notifier.showErrorNotification(DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodePrintFailedTitle"),
                                               DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodePrintFailedMessage"));
            }
        });

        firmwareFileChooser.setTitle(DisplayManager.getLanguageBundle().getString("maintenancePanel.firmwareFileChooserTitle"));
        firmwareFileChooser.getExtensionFilters()
            .addAll(
                new FileChooser.ExtensionFilter(DisplayManager.getLanguageBundle().getString("maintenancePanel.firmwareFileDescription"), "*.bin"));

        firmwareLoadService.setOnSucceeded(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                int firmwareUpgradeState = (int) t.getSource().getValue();

                switch (firmwareUpgradeState)
                {
                    case FirmwareLoadTask.SDCARD_ERROR:
                        Notifier.showErrorNotification(DisplayManager.getLanguageBundle().getString("dialogs.firmwareUpgradeFailedTitle"),
                                                       DisplayManager.getLanguageBundle().getString("dialogs.sdCardError"));
                        break;
                    case FirmwareLoadTask.FILE_ERROR:
                        Notifier.showErrorNotification(DisplayManager.getLanguageBundle().getString("dialogs.firmwareUpgradeFailedTitle"),
                                                       DisplayManager.getLanguageBundle().getString("dialogs.firmwareFileError"));
                        break;
                    case FirmwareLoadTask.OTHER_ERROR:
                        Notifier.showErrorNotification(DisplayManager.getLanguageBundle().getString("dialogs.firmwareUpgradeFailedTitle"),
                                                       DisplayManager.getLanguageBundle().getString("dialogs.firmwareUpgradeFailedMessage"));
                        break;
                    case FirmwareLoadTask.SUCCESS:
                        Notifier.showInformationNotification(DisplayManager.getLanguageBundle().getString("dialogs.firmwareUpgradeSuccessTitle"),
                                                             DisplayManager.getLanguageBundle().getString("dialogs.firmwareUpgradeSuccessMessage"));
                        break;
                }
            }
        });

        firmwareLoadService.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {

                Notifier.showErrorNotification(DisplayManager.getLanguageBundle().getString("dialogs.firmwareUpgradeFailedTitle"),
                                               DisplayManager.getLanguageBundle().getString("dialogs.firmwareUpgradeFailedMessage"));
            }
        });

        StatusScreenState statusScreenState = StatusScreenState.getInstance();
        statusScreenState.currentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {
            @Override
            public void changed(ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue)
            {
                if (connectedPrinter != null)
                {
                    connectedPrinter.printerStatusProperty().unbind();
                }

                connectedPrinter = newValue;

                if (connectedPrinter != null)
                {
                    readFirmwareVersion();
                    connectedPrinter.printerStatusProperty().addListener(printerStatusListener);
                    connectedPrinter.Filament1LoadedProperty().addListener(filamentLoadedListener);
                    connectedPrinter.Filament2LoadedProperty().addListener(filamentLoadedListener);
                    setButtonVisibility();
                }
            }
        });
    }

    private void setButtonVisibility()
    {
        boolean printingdisabled = false;
        boolean noFilamentOrPrintingdisabled = printingdisabled || (connectedPrinter.getFilament1Loaded() == false && connectedPrinter.getFilament2Loaded() == false);

        if (connectedPrinter == null)
        {
            printingdisabled = true;
        } else
        {
            printingdisabled = connectedPrinter.getPrinterStatus() != PrinterStatusEnumeration.IDLE;
        }

        YTestButton.setDisable(printingdisabled);

        PurgeMaterialButton.setDisable(noFilamentOrPrintingdisabled);

        T1CleanButton.setDisable(noFilamentOrPrintingdisabled);

        EjectStuckMaterialButton.setDisable(noFilamentOrPrintingdisabled);

        SpeedTestButton.setDisable(printingdisabled);

        CalibrateOffsetButton.setDisable(noFilamentOrPrintingdisabled);

        sendGCodeStreamGCodeMacroButton.setDisable(printingdisabled);

        CalibrateBButton.setDisable(noFilamentOrPrintingdisabled);

        XTestButton.setDisable(printingdisabled);

        Level_YButton.setDisable(printingdisabled);

        T0CleanButton.setDisable(noFilamentOrPrintingdisabled);

        LevelGantryButton.setDisable(printingdisabled);

        sendGCodeSDGCodeMacroButton.setDisable(printingdisabled);

        ZTestButton.setDisable(printingdisabled);
    }
}
