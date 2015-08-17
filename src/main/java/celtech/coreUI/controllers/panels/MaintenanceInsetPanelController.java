package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.DirectoryMemoryProperty;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ProgressDialog;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.services.printing.GCodePrintResult;
import celtech.services.printing.TransferGCodeToPrinterService;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class MaintenanceInsetPanelController implements Initializable
{

    private static final Stenographer steno = StenographerFactory.getStenographer(MaintenanceInsetPanelController.class.getName());
    private Printer connectedPrinter;

    private ProgressDialog firmwareUpdateProgress;
    private final FileChooser firmwareFileChooser = new FileChooser();

    private ProgressDialog gcodeUpdateProgress;
    private final FileChooser gcodeFileChooser = new FileChooser();
    private final TransferGCodeToPrinterService gcodePrintService = new TransferGCodeToPrinterService();

    private final BooleanProperty printingDisabled = new SimpleBooleanProperty(false);
    private final BooleanProperty noHead = new SimpleBooleanProperty(false);
    private final BooleanProperty dualHead = new SimpleBooleanProperty(false);
    ;
    private final BooleanProperty singleHead = new SimpleBooleanProperty(false);
    ;
    private final BooleanProperty noFilamentE = new SimpleBooleanProperty(false);
    private final BooleanProperty noFilamentD = new SimpleBooleanProperty(false);
    private final BooleanProperty noFilamentEOrD = new SimpleBooleanProperty(false);

    @FXML
    private VBox container;

    @FXML
    private Button YTestButton;

    @FXML
    private Button PurgeMaterialButton;

    @FXML
    private Button loadFirmwareButton;

    @FXML
    private Button T1CleanButton;

    @FXML
    private Button EjectStuckMaterialButton1;

    @FXML
    private Button EjectStuckMaterialButton2;

    @FXML
    private Button SpeedTestButton;

    @FXML
    private Button XTestButton;

    @FXML
    private Button T0CleanButton;

    @FXML
    private Label currentFirmwareField;

    @FXML
    private Button LevelGantryButton;

    @FXML
    private Button sendGCodeSDButton;

    @FXML
    private Button ZTestButton;
    
    @FXML
    void back(ActionEvent event) {
        ApplicationStatus.getInstance().setMode(ApplicationMode.STATUS);
    }

    @FXML
    void ejectStuckMaterial1(ActionEvent event)
    {
        if (connectedPrinter != null)
        {
            try
            {
                connectedPrinter.ejectStuckMaterialE(false, null);
            } catch (PrinterException ex)
            {
                steno.info("Error attempting to run eject stuck material E");
            }
        }
    }

    @FXML
    void ejectStuckMaterial2(ActionEvent event)
    {
        if (connectedPrinter != null)
        {
            try
            {
                connectedPrinter.ejectStuckMaterialD(false, null);
            } catch (PrinterException ex)
            {
                steno.info("Error attempting to run eject stuck material D");
            }
        }
    }

    @FXML
    void levelGantry(ActionEvent event)
    {
        try
        {
            connectedPrinter.levelGantry(false, null);
        } catch (PrinterException ex)
        {
            steno.error("Couldn't level gantry");
        }
    }

    @FXML
    void testX(ActionEvent event)
    {
        try
        {
            connectedPrinter.testX(false, null);
        } catch (PrinterException ex)
        {
            steno.error("Couldn't level gantry");
        }
    }

    @FXML
    void testY(ActionEvent event)
    {
        try
        {
            connectedPrinter.testY(false, null);
        } catch (PrinterException ex)
        {
            steno.error("Couldn't level gantry");
        }
    }

    @FXML
    void testZ(ActionEvent event)
    {
        try
        {
            connectedPrinter.testZ(false, null);
        } catch (PrinterException ex)
        {
            steno.error("Couldn't level gantry");
        }
    }

    @FXML
    void cleanNozzleT0(ActionEvent event)
    {
        try
        {
            connectedPrinter.cleanNozzle(0, false, null);
        } catch (PrinterException ex)
        {
            steno.error("Couldn't clean nozzle 0");
        }
    }

    @FXML
    void cleanNozzleT1(ActionEvent event)
    {
        try
        {
            connectedPrinter.cleanNozzle(1, false, null);
        } catch (PrinterException ex)
        {
            steno.error("Couldn't clean nozzle 1");
        }
    }

    @FXML
    void speedTest(ActionEvent event)
    {
        try
        {
            connectedPrinter.speedTest(false, null);
        } catch (PrinterException ex)
        {
            steno.error("Couldn't run speed test");
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
            ApplicationConfiguration.setLastDirectory(DirectoryMemoryProperty.FIRMWARE, file.
                    getParentFile().getAbsolutePath());
            connectedPrinter.loadFirmware(file.getAbsolutePath());
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

    /**
     * Initialises the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try
        {
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    gcodeUpdateProgress = new ProgressDialog(gcodePrintService);
                }
            });

            YTestButton.disableProperty().bind(printingDisabled);
            PurgeMaterialButton.disableProperty().bind(
                noFilamentEOrD.or(noHead).or(printingDisabled));

            T0CleanButton.disableProperty().bind(
                noHead
                .or(printingDisabled)
                .or(dualHead.and(noFilamentE))
                .or(singleHead.and(noFilamentEOrD)));
            T1CleanButton.disableProperty().bind(
                noHead
                .or(printingDisabled)
                .or(dualHead.and(noFilamentD))
                .or(singleHead.and(noFilamentEOrD)));

            EjectStuckMaterialButton1.disableProperty().bind(printingDisabled.or(noFilamentE));
            EjectStuckMaterialButton2.disableProperty().bind(printingDisabled.or(noFilamentD));

            SpeedTestButton.disableProperty().bind(printingDisabled);
            XTestButton.disableProperty().bind(printingDisabled);

            LevelGantryButton.disableProperty().bind(printingDisabled);
            ZTestButton.disableProperty().bind(printingDisabled);
            loadFirmwareButton.disableProperty().bind(printingDisabled.or(Lookup.
                getUserPreferences().advancedModeProperty().not()));
            sendGCodeSDButton.disableProperty().bind(printingDisabled.or(Lookup.
                getUserPreferences().advancedModeProperty().not()));

            currentFirmwareField.setStyle("-fx-font-weight: bold;");

            gcodeFileChooser.setTitle(Lookup.i18n("maintenancePanel.gcodeFileChooserTitle"));
            gcodeFileChooser.getExtensionFilters()
                    .addAll(
                            new FileChooser.ExtensionFilter(Lookup.i18n(
                                            "maintenancePanel.gcodeFileDescription"),
                                    "*.gcode"));

            gcodePrintService.setOnSucceeded(new EventHandler<WorkerStateEvent>()
            {
                @Override
                public void handle(WorkerStateEvent t)
                {
                    GCodePrintResult result = (GCodePrintResult) (t.getSource().getValue());
                    if (result.isSuccess())
                    {
                        Lookup.getSystemNotificationHandler().showInformationNotification(Lookup.i18n(
                                "maintenancePanel.gcodePrintSuccessTitle"),
                                Lookup.i18n(
                                        "maintenancePanel.gcodePrintSuccessMessage"));
                    } else
                    {
                        Lookup.getSystemNotificationHandler().showErrorNotification(Lookup.i18n(
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
                    Lookup.getSystemNotificationHandler().
                            showErrorNotification(Lookup.i18n("maintenancePanel.gcodePrintFailedTitle"),
                                    Lookup.i18n("maintenancePanel.gcodePrintFailedMessage"));
                }
            });

            firmwareFileChooser.setTitle(Lookup.i18n("maintenancePanel.firmwareFileChooserTitle"));
            firmwareFileChooser.getExtensionFilters()
                    .addAll(
                            new FileChooser.ExtensionFilter(Lookup.i18n(
                                            "maintenancePanel.firmwareFileDescription"), "*.bin"));

            Lookup.getSelectedPrinterProperty().addListener(
                (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
                {
                    if (connectedPrinter != null)
                    {
                        sendGCodeSDButton.disableProperty().unbind();
                        loadFirmwareButton.disableProperty().unbind();

                        printingDisabled.unbind();
                        printingDisabled.set(true);
                        noHead.unbind();
                        noHead.set(true);
                        noFilamentE.unbind();
                        noFilamentE.set(true);
                        noFilamentD.unbind();
                        noFilamentD.set(true);
                        noFilamentEOrD.unbind();
                        noFilamentEOrD.set(true);

                        dualHead.unbind();
                        dualHead.set(false);
                        singleHead.unbind();
                        singleHead.set(false);
                    }

                    connectedPrinter = newValue;

                    if (connectedPrinter != null)
                    {
                        readFirmwareVersion();

                        printingDisabled.bind(connectedPrinter.printerStatusProperty().isNotEqualTo(
                                PrinterStatus.IDLE));

                        noHead.bind(connectedPrinter.headProperty().isNull());

                        if (noHead.not().get())
                        {
                            dualHead.bind(Bindings.size(
                                    connectedPrinter.headProperty().get().getNozzleHeaters()).isEqualTo(
                                    2));
                            singleHead.bind(Bindings.size(
                                    connectedPrinter.headProperty().get().getNozzleHeaters()).isEqualTo(
                                    1));
                        }

                        noFilamentE.bind(
                            connectedPrinter.extrudersProperty().get(0).filamentLoadedProperty().not());
                        noFilamentD.bind(
                            connectedPrinter.extrudersProperty().get(1).filamentLoadedProperty().not());

                        noFilamentEOrD.bind(
                            connectedPrinter.extrudersProperty().get(0).filamentLoadedProperty().not()
                            .and(
                                connectedPrinter.extrudersProperty().get(1).filamentLoadedProperty().not()));
                    }
                });
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
