package celtech.coreUI.controllers.utilityPanels;

import celtech.Lookup;
import celtech.appManager.Notifier;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.DirectoryMemoryProperty;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.GCodeMacroButton;
import celtech.coreUI.components.ProgressDialog;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.services.firmware.FirmwareLoadResult;
import celtech.services.firmware.FirmwareLoadService;
import celtech.services.printing.GCodePrintResult;
import celtech.services.printing.TransferGCodeToPrinterService;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class MaintenancePanelController implements Initializable
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
        MaintenancePanelController.class.getName());
    private Printer connectedPrinter = null;

    private ProgressDialog firmwareUpdateProgress = null;
    private final FirmwareLoadService firmwareLoadService = new FirmwareLoadService();
    private final FileChooser firmwareFileChooser = new FileChooser();

    private ProgressDialog gcodeUpdateProgress = null;
    private final FileChooser gcodeFileChooser = new FileChooser();
    private final TransferGCodeToPrinterService gcodePrintService = new TransferGCodeToPrinterService();

    private final BooleanProperty printingdisabled = new SimpleBooleanProperty(false);
    private final BooleanProperty noFilamentOrPrintingdisabled = new SimpleBooleanProperty(false);

    @FXML
    private AnchorPane container;

    @FXML
    private GCodeMacroButton YTestButton;

    @FXML
    private Button PurgeMaterialButton;
    
    @FXML
    private Button PurgeMaterialButton1;    

    @FXML
    private Button loadFirmwareGCodeMacroButton;

    @FXML
    private GCodeMacroButton T1CleanButton;

    @FXML
    private GCodeMacroButton EjectStuckMaterialButton;

    @FXML
    private GCodeMacroButton SpeedTestButton;

    @FXML
    private GCodeMacroButton XTestButton;

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
//                try
//                {
//                    connectedPrinter.executeMacro(macroName);
//                } catch (PrinterException ex)
//                {
//                    steno.error("Error sending macro : " + macroName);
//                }
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
//                try
//                {
//                    connectedPrinter.executeMacroWithoutPurgeCheck(macroName);
//                } catch (PrinterException ex)
//                {
//                    steno.error("Error sending macro : " + macroName);
//                }
            }
        }
    }

    @FXML
    void loadFirmware(ActionEvent event)
    {
        firmwareFileChooser.setInitialFileName("Untitled");

        firmwareFileChooser.setInitialDirectory(new File(ApplicationConfiguration.getLastDirectory(
            DirectoryMemoryProperty.FIRMWARE)));

        final File file = firmwareFileChooser.showOpenDialog(DisplayManager.getMainStage());
        if (file != null)
        {
            firmwareLoadService.reset();
            firmwareLoadService.setPrinterToUse(connectedPrinter);
            firmwareLoadService.setFirmwareFileToLoad(file.getAbsolutePath());
            firmwareLoadService.start();
            ApplicationConfiguration.setLastDirectory(DirectoryMemoryProperty.FIRMWARE, file.
                                                      getParentFile().getAbsolutePath());
        }
    }

    void readFirmwareVersion()
    {
        try
        {
            FirmwareResponse response = connectedPrinter.readFirmwareVersion();
            if (response != null)
            {
                currentFirmwareField.setText(response.getFirmwareRevision());
            }
        } catch (PrinterException ex)
        {
            steno.error("Error reading firmware version");
        }
    }

    @FXML
    void sendGCodeSD(ActionEvent event)
    {
        gcodeFileChooser.setInitialFileName("Untitled");

        gcodeFileChooser.setInitialDirectory(new File(ApplicationConfiguration.getLastDirectory(
            DirectoryMemoryProperty.GCODE)));

        final File file = gcodeFileChooser.showOpenDialog(container.getScene().getWindow());

        if (file != null)
        {
            try
            {
                connectedPrinter.executeGCodeFile(file.getAbsolutePath(), true);
            } catch (PrinterException ex)
            {
                steno.error("Error sending SD job");
            }
            ApplicationConfiguration.setLastDirectory(DirectoryMemoryProperty.GCODE, file.
                                                      getParentFile().getAbsolutePath());
        }
    }

    @FXML
    void purge(ActionEvent event)
    {
        DisplayManager.getInstance().getPurgeInsetPanelController().purge(connectedPrinter);
    }
    
    @FXML
    void purge2(ActionEvent event)
    {
        DisplayManager.getInstance().getPurgeInsetPanelController2().purge(connectedPrinter);
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
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                gcodeUpdateProgress = new ProgressDialog(gcodePrintService);
                firmwareUpdateProgress = new ProgressDialog(firmwareLoadService);
            }
        });

        YTestButton.disableProperty().bind(printingdisabled);
        PurgeMaterialButton.disableProperty().bind(noFilamentOrPrintingdisabled);
        PurgeMaterialButton1.disableProperty().bind(noFilamentOrPrintingdisabled);
        T1CleanButton.disableProperty().bind(noFilamentOrPrintingdisabled);
        EjectStuckMaterialButton.disableProperty().bind(noFilamentOrPrintingdisabled);
        SpeedTestButton.disableProperty().bind(printingdisabled);
        XTestButton.disableProperty().bind(printingdisabled);
        T0CleanButton.disableProperty().bind(noFilamentOrPrintingdisabled);
        LevelGantryButton.disableProperty().bind(printingdisabled);
        ZTestButton.disableProperty().bind(printingdisabled);
        loadFirmwareGCodeMacroButton.disableProperty().bind(printingdisabled.or(Lookup.
            getUserPreferences().advancedModeProperty().not()));
        sendGCodeSDGCodeMacroButton.disableProperty().bind(printingdisabled.or(Lookup.
            getUserPreferences().advancedModeProperty().not()));

        currentFirmwareField.setStyle("-fx-font-weight: bold;");

        gcodeFileChooser.setTitle(Lookup.i18n("maintenancePanel.gcodeFileChooserTitle"));
        gcodeFileChooser.getExtensionFilters()
            .addAll(
                new FileChooser.ExtensionFilter(Lookup.i18n("maintenancePanel.gcodeFileDescription"),
                                                "*.gcode"));

        gcodePrintService.setOnSucceeded(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                GCodePrintResult result = (GCodePrintResult) (t.getSource().getValue());
                if (result.isSuccess())
                {
                    Notifier.showInformationNotification(Lookup.i18n(
                        "maintenancePanel.gcodePrintSuccessTitle"),
                                                         Lookup.i18n(
                                                             "maintenancePanel.gcodePrintSuccessMessage"));
                } else
                {
                    Notifier.showErrorNotification(Lookup.i18n(
                        "maintenancePanel.gcodePrintFailedTitle"),
                                                   Lookup.i18n(
                                                       "maintenancePanel.gcodePrintFailedMessage"));

                    steno.warning("In gcode print succeeded but with failure flag");
                }
            }
        });

        gcodePrintService.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                Notifier.
                    showErrorNotification(Lookup.i18n("maintenancePanel.gcodePrintFailedTitle"),
                                          Lookup.i18n("maintenancePanel.gcodePrintFailedMessage"));
            }
        });

        firmwareFileChooser.setTitle(Lookup.i18n("maintenancePanel.firmwareFileChooserTitle"));
        firmwareFileChooser.getExtensionFilters()
            .addAll(
                new FileChooser.ExtensionFilter(Lookup.i18n(
                        "maintenancePanel.firmwareFileDescription"), "*.bin"));

        firmwareLoadService.setOnSucceeded((WorkerStateEvent t) ->
        {
            FirmwareLoadResult result = (FirmwareLoadResult) t.getSource().getValue();
            Lookup.getSystemNotificationHandler().showFirmwareUpgradeStatusNotification(result);
        });

        firmwareLoadService.setOnFailed((WorkerStateEvent t) ->
        {
            FirmwareLoadResult result = (FirmwareLoadResult) t.getSource().getValue();
            Lookup.getSystemNotificationHandler().showFirmwareUpgradeStatusNotification(result);
        });

        Lookup.getCurrentlySelectedPrinterProperty().addListener(
            (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
            {
                if (connectedPrinter != null)
                {
                    sendGCodeSDGCodeMacroButton.disableProperty().unbind();
                    loadFirmwareGCodeMacroButton.disableProperty().unbind();

                    printingdisabled.unbind();
                    printingdisabled.set(true);
                    noFilamentOrPrintingdisabled.unbind();
                    noFilamentOrPrintingdisabled.set(true);
                }

                connectedPrinter = newValue;

                if (connectedPrinter != null)
                {
                    readFirmwareVersion();

                    printingdisabled.bind(connectedPrinter.printerStatusProperty().isNotEqualTo(
                            PrinterStatus.IDLE));

                    //TODO modify for multiple extruders
                    noFilamentOrPrintingdisabled.bind(printingdisabled
                        .or(connectedPrinter.extrudersProperty().get(0).filamentLoadedProperty().
                            not()
                            .and(connectedPrinter.extrudersProperty().get(1).
                                filamentLoadedProperty().not())));
                }
            });
    }
}
