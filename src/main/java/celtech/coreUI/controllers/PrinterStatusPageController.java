package celtech.coreUI.controllers;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.EEPROMState;
import celtech.configuration.PauseStatus;
import celtech.configuration.PrinterColourMap;
import celtech.configuration.WhyAreWeWaitingState;
import celtech.coreUI.AmbientLEDState;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.JogButton;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.utils.tasks.TaskResponse;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.dialog.Dialogs.CommandLink;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterStatusPageController implements Initializable
{

    private Stenographer steno = StenographerFactory.getStenographer(
        PrinterStatusPageController.class.getName());
    private StatusScreenState statusScreenState = null;
    private Printer printerToUse = null;
    private ChangeListener<Boolean> reelDataChangeListener = null;
    private ChangeListener<EEPROMState> reelChangeListener = null;
    private ChangeListener<Color> printerColourChangeListener = null;
    private ChangeListener<PrinterStatus> printerStatusChangeListener = null;
    private ChangeListener<PauseStatus> pauseStatusChangeListener = null;

    private CommandLink goAheadAndOpenTheLid = null;
    private CommandLink dontOpenTheLid = null;
    private String openLidPrinterTooHotTitle = null;
    private String openLidPrinterTooHotInfo = null;

    private String transferringDataString = null;

    private PrinterColourMap colourMap = PrinterColourMap.getInstance();

    private NumberFormat threeDPformatter;
    private NumberFormat fiveDPformatter;

    @FXML
    private ImageView printerSilhouette;

    @FXML
    private Button headLEDButton;

    @FXML
    private Button ambientLEDButton;

    @FXML
    private Button homeButton;

    @FXML
    private JogButton x_minus1;

    @FXML
    private JogButton x_minus100;

    @FXML
    private JogButton z_plus0_1;

    @FXML
    private JogButton z_plus10;

    @FXML
    private JogButton z_minus0_1;

    @FXML
    private JogButton y_plus10;

    @FXML
    private JogButton extruder_minus20;

    @FXML
    private JogButton z_minus1;

    @FXML
    private JogButton x_plus100;

    @FXML
    private JogButton y_minus1;

    @FXML
    private JogButton x_plus10;

    @FXML
    private JogButton y_minus10;

    @FXML
    private ImageView printerOpenImage;

    @FXML
    private Button ejectReelButton;

    @FXML
    private ImageView reel;

    @FXML
    private JogButton x_plus1;

    @FXML
    private Button pausePrintButton;

    @FXML
    private JogButton extruder_plus100;

    @FXML
    private Rectangle printerColourRectangle;

    @FXML
    private Button selectNozzle2;

    @FXML
    private Button openNozzleButton;

    @FXML
    private StackPane statusPane;

    @FXML
    private Button selectNozzle1;

    @FXML
    private JogButton extruder_minus5;

    @FXML
    private Button headFanButton;

    @FXML
    private Button removeHeadButton;

    @FXML
    private JogButton y_plus100;

    @FXML
    private JogButton y_minus100;

    @FXML
    private Text progressTitle;

    @FXML
    private Button resumePrintButton;

    @FXML
    private JogButton extruder_minus100;

    @FXML
    private Button unlockLidButton;

    @FXML
    private JogButton x_minus10;

    @FXML
    private Button closeNozzleButton;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ProgressBar secondProgressBar;

    @FXML
    private JogButton y_plus1;

    @FXML
    private Text progressPercent;

    @FXML
    private Text secondProgressPercent;

    @FXML
    private Text progressETC;

    @FXML
    private Text progressLayers;

    @FXML
    private Text progressLayerLabel;

    @FXML
    private Text progressETCLabel;

    @FXML
    private Button cancelPrintButton;

    @FXML
    private JogButton extruder_plus20;

    @FXML
    private JogButton z_plus1;

    @FXML
    private Rectangle filamentRectangle;

    @FXML
    private JogButton extruder_plus5;

    @FXML
    private ImageView printerClosedImage;

    @FXML
    private VBox progressGroup;

    @FXML
    private JogButton z_minus10;

    @FXML
    private Text progressMessage;

    @FXML
    private Group temperatureWarning;

    @FXML
    private HBox printControlButtons;

    private Node[] advancedControls = null;
    private final BooleanProperty advancedControlsVisible = new SimpleBooleanProperty(false);

    private Printer lastSelectedPrinter = null;

    private final BooleanProperty showProgressGroup = new SimpleBooleanProperty(false);

    @FXML
    void home(ActionEvent event)
    {
        try
        {
            printerToUse.runMacro("Home_all");
        } catch (PrinterException ex)
        {
            steno.error("Couldn't run home macro");
        }
    }

    @FXML
    void pausePrint(ActionEvent event)
    {
        try
        {
            printerToUse.pause();
        } catch (PrinterException ex)
        {
            steno.error("Couldn't pause printer");
        }
    }

    @FXML
    void resumePrint(ActionEvent event)
    {
        try
        {
            printerToUse.resume();
        } catch (PrinterException ex)
        {
            steno.error("Couldn't resume print");
        }
    }

    @FXML
    void cancelPrint(ActionEvent event)
    {
        try
        {
            printerToUse.cancel(null);
        } catch (PrinterException ex)
        {
            steno.error("Couldn't resume print");
        }
    }

    @FXML
    void ejectReel(ActionEvent event)
    {
        //TODO modify for multiple extruders
        try
        {
            printerToUse.ejectFilament(0, null);
        } catch (PrinterException ex)
        {
            steno.error("Error when sending eject filament - " + ex.getMessage());
        }
    }

    @FXML
    void unlockLid(ActionEvent event)
    {
        boolean openTheLid = true;

        if (printerToUse.getPrinterAncillarySystems().bedTemperatureProperty().get() > 60)
        {
            Action tooBigResponse = Dialogs.create().title(
                openLidPrinterTooHotTitle)
                .message(openLidPrinterTooHotInfo)
                .masthead(null)
                .showCommandLinks(dontOpenTheLid, dontOpenTheLid,
                                  goAheadAndOpenTheLid);

            if (tooBigResponse != goAheadAndOpenTheLid)
            {
                openTheLid = false;
            }
        }

        if (openTheLid)
        {
            try
            {
                printerToUse.goToOpenDoorPosition(null);
            } catch (PrinterException ex)
            {
                steno.error("Error opening door " + ex.getMessage());
            }
        }
    }

    @FXML
    void jogButton(ActionEvent event)
    {
        JogButton button = (JogButton) event.getSource();

        try
        {
            printerToUse.jogAxis(button.getAxis(), button.getDistance(), button.getFeedRate(), button.getUseG1());
        } catch (PrinterException ex)
        {
            steno.error("Failed to jog printer - " + ex.getMessage());
        }
    }

    boolean headLEDOn = false;

    @FXML
    void toggleHeadLED(ActionEvent event)
    {
        try
        {
            if (headLEDOn == true)
            {
                printerToUse.switchOffHeadLEDs();
                headLEDOn = false;
            } else
            {
                printerToUse.switchOnHeadLEDs();
                headLEDOn = true;
            }
        } catch (PrinterException ex)
        {
            steno.error("Failed to send head LED command - " + ex.getMessage());
        }
    }

    @FXML
    void toggleHeadFan(ActionEvent event)
    {
        try
        {
            if (printerToUse.getPrinterAncillarySystems().headFanOnProperty().get())
            {
                printerToUse.switchOffHeadFan();
            } else
            {
                printerToUse.switchOnHeadFan();
            }
        } catch (PrinterException ex)
        {
            steno.error("Failed to send head fan command - " + ex.getMessage());
        }
    }

    @FXML
    void removeHead(ActionEvent event)
    {
        try
        {
            printerToUse.removeHead((TaskResponse taskResponse) ->
            {
                removeHeadFinished(taskResponse);
            });
        } catch (PrinterException ex)
        {
            steno.error("PrinterException whilst invoking remove head: " + ex.getMessage());
        }
    }

    private void removeHeadFinished(TaskResponse taskResponse)
    {
        if (taskResponse.succeeded())
        {
            Dialogs.create()
                .owner(null)
                .title(Lookup.i18n("removeHead.title"))
                .masthead(null)
                .message(Lookup.i18n("removeHead.finished"))
                .showInformation();
            steno.debug("Head remove completed");
        } else
        {
            Dialogs.create()
                .owner(null)
                .title(Lookup.i18n("removeHead.title"))
                .masthead(null)
                .message(Lookup.i18n("removeHead.failed"))
                .showWarning();
        }
    }

    private AmbientLEDState ambientLEDState = AmbientLEDState.COLOUR;

    @FXML
    void toggleAmbientLED(ActionEvent event)
    {
        try
        {
            // Off, White, Colour
            ambientLEDState = ambientLEDState.getNextState();

            switch (ambientLEDState)
            {
                case OFF:
                    printerToUse.setAmbientLEDColour(Color.BLACK);
                    printerColourRectangle.setFill(Color.BLACK);
                    break;
                case WHITE:
                    printerToUse.setAmbientLEDColour(
                        colourMap.displayToPrinterColour(Color.WHITE));
                    printerColourRectangle.setFill(Color.WHITE);
                    break;
                case COLOUR:
                    printerToUse.setAmbientLEDColour(
                        printerToUse.getPrinterIdentity().printerColourProperty().get());
                    printerColourRectangle.setFill(
                        colourMap.printerToDisplayColour(
                            printerToUse.getPrinterIdentity().printerColourProperty().get()));
                    break;
            }
        } catch (PrinterException ex)
        {
            steno.error("Failed to send ambient LED command");
        }
    }

    @FXML
    void selectNozzle1(ActionEvent event)
    {
        try
        {
            printerToUse.selectNozzle(0);
        } catch (PrinterException ex)
        {
            steno.error("Failed to send open nozzle");
        }
    }

    @FXML
    void selectNozzle2(ActionEvent event)
    {
        try
        {
            printerToUse.selectNozzle(1);
        } catch (PrinterException ex)
        {
            steno.error("Failed to send open nozzle");
        }
    }

    @FXML
    void openNozzle(ActionEvent event)
    {
        try
        {
            printerToUse.openNozzleFully();
        } catch (PrinterException ex)
        {
            steno.error("Failed to send open nozzle");
        }
    }

    @FXML
    void closeNozzle(ActionEvent event)
    {
        try
        {
            printerToUse.closeNozzleFully();
        } catch (PrinterException ex)
        {
            steno.error("Failed to send close nozzle");
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
        threeDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        threeDPformatter.setMaximumFractionDigits(3);
        threeDPformatter.setGroupingUsed(false);

        fiveDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        fiveDPformatter.setMaximumFractionDigits(5);
        fiveDPformatter.setGroupingUsed(false);

        statusScreenState = StatusScreenState.getInstance();

        ResourceBundle i18nBundle = DisplayManager.getLanguageBundle();

        transferringDataString = i18nBundle.getString(
            "PrintQueue.SendingToPrinter");

        goAheadAndOpenTheLid = new Dialogs.CommandLink(i18nBundle.getString(
            "dialogs.openLidPrinterHotGoAheadHeading"), i18nBundle.getString("dialogs.openLidPrinterHotGoAheadInfo"));
        dontOpenTheLid = new Dialogs.CommandLink(i18nBundle.getString(
            "dialogs.openLidPrinterHotDontOpenHeading"), null);
        openLidPrinterTooHotTitle = i18nBundle.getString(
            "dialogs.openLidPrinterHotTitle");
        openLidPrinterTooHotInfo = i18nBundle.getString(
            "dialogs.openLidPrinterHotInfo");
        progressLayerLabel.setText(i18nBundle.getString("dialogs.progressLayerLabel"));
        progressETCLabel.setText(i18nBundle.getString("dialogs.progressETCLabel"));

        reelDataChangeListener = (ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) ->
        {
            //TODO modify to support multiple reels
            filamentRectangle.setFill(printerToUse.reelsProperty().get(0).displayColourProperty().get());
        };

        reelChangeListener = (ObservableValue<? extends EEPROMState> ov, EEPROMState t, EEPROMState t1) ->
        {
            if (t1 == EEPROMState.PROGRAMMED)
            {
                //TODO modify to support multiple reels
                filamentRectangle.setFill(printerToUse.reelsProperty().get(0).displayColourProperty().get());
            }
        };

        printerColourChangeListener = (ObservableValue<? extends Color> observable, Color oldValue, Color newValue) ->
        {
            if (ambientLEDState == AmbientLEDState.COLOUR)
            {
                printerColourRectangle.setFill(
                    colourMap.printerToDisplayColour(
                        newValue));
            }
        };

        printerStatusChangeListener = (ObservableValue<? extends PrinterStatus> observable, PrinterStatus oldValue, PrinterStatus newValue) ->
        {
            processPrinterStatusChange(printerToUse.printerStatusProperty().get());
        };

        printerSilhouette.setVisible(true);
        printerClosedImage.setVisible(false);
        printerOpenImage.setVisible(false);
        printerColourRectangle.setVisible(false);

        progressGroup.visibleProperty().bind(showProgressGroup);

        ejectReelButton.setVisible(false);
        unlockLidButton.setVisible(false);
        temperatureWarning.setVisible(false);

        reel.setVisible(false);
        filamentRectangle.setVisible(false);

        printControlButtons.setVisible(false);

        if (statusScreenState.getCurrentlySelectedPrinter() != null)
        {
            Printer printer = statusScreenState.getCurrentlySelectedPrinter();
            processPrinterStatusChange(printer.printerStatusProperty().get());
        }

        statusScreenState.currentlySelectedPrinterProperty().addListener(
            new ChangeListener<Printer>()
            {
                @Override
                public void changed(ObservableValue<? extends Printer> ov,
                    Printer t, Printer selectedPrinter)
                {
                    printerToUse = selectedPrinter;

                    if (selectedPrinter == null)
                    {
                        printerSilhouette.setVisible(true);
                    } else
                    {
                        printerSilhouette.setVisible(false);
                    }

                    if (selectedPrinter == null)
                    {
                        unbindFromSelectedPrinter();

                        showProgressGroup.set(false);
                        printerColourRectangle.setVisible(false);

                        ejectReelButton.setVisible(false);
                        unlockLidButton.setVisible(false);
                        temperatureWarning.setVisible(false);

                        reel.setVisible(false);
                        filamentRectangle.setVisible(false);

                        processPrinterStatusChange(null);
                    } else
                    {
                        unbindFromSelectedPrinter();

                        progressBar.progressProperty().bind(selectedPrinter.getPrintEngine().progressProperty());
                        progressPercent.textProperty().bind(
                            Bindings.multiply(selectedPrinter.getPrintEngine().progressProperty(), 100).asString("%.0f%%"));
                        BooleanBinding progressVisible
                        = selectedPrinter.printerStatusProperty().isNotEqualTo(
                            PrinterStatus.PRINTING)
                        .or(Bindings.and(
                                selectedPrinter.getPrintEngine().
                                linesInPrintingFileProperty().greaterThan(
                                    0),
                                selectedPrinter.getPrintEngine().
                                progressProperty().greaterThanOrEqualTo(
                                    0)));
                        BooleanBinding progressETCVisible
                        = Bindings.and(
                            selectedPrinter.getPrintEngine().etcAvailableProperty(),
                            Bindings.or(
                                selectedPrinter.printerStatusProperty().isEqualTo(
                                    PrinterStatus.PRINTING),
                                selectedPrinter.printerStatusProperty().isEqualTo(
                                    PrinterStatus.SENDING_TO_PRINTER)));
                        progressPercent.visibleProperty().bind(progressVisible);
                        progressETC.visibleProperty().bind(progressETCVisible);
                        progressETC.textProperty().bind(new StringBinding()
                            {
                                {
                                    super.bind(
                                        selectedPrinter.getPrintEngine().
                                        progressETCProperty());
                                }

                                @Override
                                protected String computeValue()
                                {
                                    int secondsRemaining = selectedPrinter.getPrintEngine().
                                    progressETCProperty().get();
                                    secondsRemaining += 30;
                                    if (secondsRemaining > 60)
                                    {
                                        String hoursMinutes = convertToHoursMinutes(
                                            secondsRemaining);
                                        return hoursMinutes;
                                    } else
                                    {
                                        return i18nBundle.getString("dialogs.lessThanOneMinute");
                                    }
                                }
                        });
                        progressLayers.textProperty().bind(new StringBinding()
                            {
                                {
                                    super.bind(
                                        selectedPrinter.getPrintEngine().progressCurrentLayerProperty(),
                                        selectedPrinter.getPrintEngine().progressNumLayersProperty());
                                }

                                @Override
                                protected String computeValue()
                                {
                                    int currentLayer = selectedPrinter.getPrintEngine().progressCurrentLayerProperty().get();
                                    int totalLayers = selectedPrinter.getPrintEngine().progressNumLayersProperty().get();
                                    return (currentLayer + 1) + "/" + totalLayers;
                                }
                        });
                        progressLayers.visibleProperty().bind(progressETCVisible);
                        progressLayerLabel.visibleProperty().bind(progressETCVisible);
                        progressETCLabel.visibleProperty().bind(progressETCVisible);
                        secondProgressBar.visibleProperty().bind(
                            selectedPrinter.getPrintEngine().
                            sendingDataToPrinterProperty());
                        secondProgressBar.progressProperty().bind(
                            selectedPrinter.getPrintEngine().
                            secondaryProgressProperty());
                        secondProgressPercent.visibleProperty().bind(
                            selectedPrinter.getPrintEngine().
                            sendingDataToPrinterProperty());
                        secondProgressPercent.textProperty().bind(
                            Bindings.format(
                                "%s %.0f%%", transferringDataString,
                                Bindings.multiply(
                                    selectedPrinter.getPrintEngine().
                                    secondaryProgressProperty(),
                                    100)));
                        progressTitle.textProperty().bind(
                            selectedPrinter.getPrintEngine().titleProperty());
                        progressMessage.textProperty().bind(
                            selectedPrinter.getPrintEngine().messageProperty());

                        printerColourRectangle.setVisible(true);
                        printerColourRectangle.setFill(
                            colourMap.printerToDisplayColour(
                                selectedPrinter.getPrinterIdentity().printerColourProperty().get()));
                        selectedPrinter.getPrinterIdentity().printerColourProperty().addListener(
                            printerColourChangeListener);

                        //TODO modify for multiple extruders
                        ejectReelButton.visibleProperty().bind(
                            selectedPrinter.extrudersProperty().get(0).filamentLoadedProperty()
                            .and(selectedPrinter.printerStatusProperty()
                                .isNotEqualTo(PrinterStatus.PRINTING)));

                        unlockLidButton.setVisible(true);
                        unlockLidButton.disableProperty().bind(
                            selectedPrinter.getPrinterAncillarySystems().lidOpenProperty().or(
                                selectedPrinter.printerStatusProperty().
                                isNotEqualTo(PrinterStatus.IDLE)));
                        temperatureWarning.visibleProperty().bind(
                            selectedPrinter.getPrinterAncillarySystems().bedTemperatureProperty()
                            .greaterThan(ApplicationConfiguration.bedHotAboveDegrees));

                        //TODO modify to support multiple reels
                        selectedPrinter.reelsProperty().get(0).dataChangedToggleProperty().addListener(
                            reelDataChangeListener);
                        //TODO modify to support multiple reels
                        selectedPrinter.reelsProperty().get(0)
                        .reelEEPROMStatusProperty().addListener(reelChangeListener);
                        //TODO modify to support multiple reels
                        filamentRectangle.setFill(selectedPrinter.reelsProperty().get(0).displayColourProperty().get());
                        //TODO modify to support multiple reels
                        filamentRectangle.visibleProperty().bind(
                            selectedPrinter.reelsProperty().get(0)
                            .reelEEPROMStatusProperty().isEqualTo(EEPROMState.PROGRAMMED));
                        reel.visibleProperty().bind(
                            selectedPrinter.reelsProperty().get(0)
                            .reelEEPROMStatusProperty()
                            .isEqualTo(EEPROMState.PROGRAMMED));

                        processPrinterStatusChange(selectedPrinter.printerStatusProperty().get());
                        selectedPrinter.printerStatusProperty().addListener(printerStatusChangeListener);

                        printerOpenImage.visibleProperty().bind(selectedPrinter.getPrinterAncillarySystems().lidOpenProperty());
                        printerClosedImage.visibleProperty().bind(selectedPrinter.getPrinterAncillarySystems().lidOpenProperty().not());

                        advancedControlsVisible.unbind();
                        advancedControlsVisible.bind(
                            (selectedPrinter.printerStatusProperty().isEqualTo(
                                PrinterStatus.IDLE).and(
                                selectedPrinter.getPrinterAncillarySystems().whyAreWeWaitingProperty()
                                .isEqualTo(WhyAreWeWaitingState.NOT_WAITING))).
                            or(selectedPrinter.printerStatusProperty().
                                isEqualTo(PrinterStatus.PAUSED)));
                    }

                    lastSelectedPrinter = selectedPrinter;
                }
            });

        advancedControls = new Node[]
        {
            extruder_minus100, extruder_minus20, extruder_minus5, extruder_plus100, extruder_plus20, extruder_plus5,
            homeButton, x_minus1, x_minus10, x_minus100, x_plus1, x_plus10, x_plus100,
            y_minus1, y_minus10, y_minus100, y_plus1, y_plus10, y_plus100,
            z_minus0_1, z_minus1, z_minus10, z_plus0_1, z_plus1, z_plus10,
            openNozzleButton, closeNozzleButton, selectNozzle1, selectNozzle2,
            ambientLEDButton,
            headFanButton, headLEDButton, removeHeadButton
        };

        for (Node node : advancedControls)
        {
            node.setVisible(false);
        }

        if (statusScreenState.getCurrentlySelectedPrinter() != null)
        {
            boolean visible = (statusScreenState.getCurrentlySelectedPrinter().printerStatusProperty().get() == PrinterStatus.IDLE
                || statusScreenState.getCurrentlySelectedPrinter().printerStatusProperty().get() == PrinterStatus.PAUSED);
            for (Node node : advancedControls)
            {
                node.setVisible(visible);
            }
        }

        advancedControlsVisible.addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                for (Node node : advancedControls)
                {
                    node.setVisible(newValue);
                }
            });
    }

    private void processPrinterStatusChange(PrinterStatus printerStatus)
    {
        if (printerStatus == null)
        {
            printControlButtons.setVisible(false);
        } else
        {
            switch (printerStatus)
            {
                case PAUSING:
                    resumePrintButton.setVisible(true);
                    resumePrintButton.setDisable(false);
                    pausePrintButton.setVisible(false);
                    pausePrintButton.setDisable(false);
                    cancelPrintButton.setVisible(true);
                    showProgressGroup.set(false);
                    break;
                case RESUMING:
                    resumePrintButton.setVisible(true);
                    resumePrintButton.setDisable(true);
                    pausePrintButton.setVisible(false);
                    pausePrintButton.setDisable(false);
                    cancelPrintButton.setVisible(false);
                    showProgressGroup.set(false);
                    break;
                case PAUSED:
                    resumePrintButton.setVisible(true);
                    resumePrintButton.setDisable(false);
                    pausePrintButton.setVisible(false);
                    pausePrintButton.setDisable(true);
                    cancelPrintButton.setVisible(true);
                    showProgressGroup.set(false);
                    break;
                case SENDING_TO_PRINTER:
                case PRINTING:
                    resumePrintButton.setVisible(false);
                    resumePrintButton.setDisable(true);
                    pausePrintButton.setVisible(true);
                    pausePrintButton.setDisable(false);
                    cancelPrintButton.setVisible(false);
                    printControlButtons.setVisible(true);
                    showProgressGroup.set(true);
                    break;
                case SLICING:
                case POST_PROCESSING:
                    resumePrintButton.setVisible(false);
                    resumePrintButton.setDisable(true);
                    pausePrintButton.setVisible(true);
                    pausePrintButton.setDisable(false);
                    cancelPrintButton.setVisible(false);
                    printControlButtons.setVisible(true);
                    cancelPrintButton.setVisible(true);
                    pausePrintButton.setVisible(false);
                    showProgressGroup.set(true);
                    break;
                default:
                    printControlButtons.setVisible(false);
                    showProgressGroup.set(false);
                    break;
            }
        }
    }

    private String convertToHoursMinutes(int seconds)
    {
        int minutes = (int) (seconds / 60);
        int hours = minutes / 60;
        minutes = minutes - (60 * hours);
        return String.format("%02d:%02d", hours, minutes);
    }

    /**
     *
     * @param parent
     */
    public void configure(VBox parent)
    {
        parent.widthProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                Number oldValue, Number newValue)
            {
                resizePrinterDisplay(parent);
            }
        });
        parent.heightProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                Number oldValue, Number newValue)
            {
                resizePrinterDisplay(parent);
            }
        });
    }

    private void resizePrinterDisplay(VBox parent)
    {
        final double beginWidth = 750;
        final double beginHeight = 660;
        final double aspect = beginWidth / beginHeight;

        double displayAspect = parent.getWidth() / parent.getHeight();

        double newWidth = 0;
        double newHeight = 0;

        if (displayAspect >= aspect)
        {
            // Drive from height
            newWidth = parent.getHeight() * aspect;
            newHeight = parent.getHeight();
        } else
        {
            //Drive from width
            newHeight = parent.getWidth() / aspect;
            newWidth = parent.getWidth();
        }

        double xScale = Double.min((newWidth / beginWidth), 1.0);
        double yScale = Double.min((newHeight / beginHeight), 1.0);

        statusPane.setScaleX(xScale);
        statusPane.setScaleY(yScale);
    }

    private void unbindFromSelectedPrinter()
    {
        progressBar.progressProperty().unbind();
        progressPercent.textProperty().unbind();
        progressTitle.textProperty().unbind();
        progressMessage.textProperty().unbind();
        secondProgressBar.visibleProperty().unbind();
        secondProgressBar.progressProperty().unbind();
        secondProgressPercent.textProperty().unbind();

        reel.visibleProperty().unbind();
        if (lastSelectedPrinter != null)
        {
            lastSelectedPrinter.getPrinterIdentity().printerColourProperty().removeListener(
                printerColourChangeListener);
            //TODO modify to support multiple reels
            lastSelectedPrinter.reelsProperty().get(0).dataChangedToggleProperty().removeListener(
                reelDataChangeListener);
            //TODO modify to support multiple reels
            lastSelectedPrinter.reelsProperty().get(0).reelEEPROMStatusProperty().removeListener(
                reelChangeListener);
            lastSelectedPrinter.printerStatusProperty().removeListener(printerStatusChangeListener);
        }

        filamentRectangle.visibleProperty().unbind();

        ejectReelButton.visibleProperty().unbind();
        ejectReelButton.setVisible(false);
        unlockLidButton.setVisible(false);
        temperatureWarning.visibleProperty().unbind();
        temperatureWarning.setVisible(false);

        printerOpenImage.visibleProperty().unbind();
        printerOpenImage.setVisible(false);
        printerClosedImage.visibleProperty().unbind();
        printerClosedImage.setVisible(false);
    }
}
