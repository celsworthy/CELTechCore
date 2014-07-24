/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.PauseStatus;
import celtech.configuration.PrinterColourMap;
import celtech.configuration.WhyAreWeWaitingState;
import celtech.coreUI.AmbientLEDState;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.JogButton;
import celtech.printerControl.Printer;
import celtech.printerControl.PrinterStatusEnumeration;
import static celtech.printerControl.PrinterStatusEnumeration.PAUSED;
import static celtech.printerControl.PrinterStatusEnumeration.PRINTING;
import static celtech.printerControl.PrinterStatusEnumeration.SENDING_TO_PRINTER;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.utils.AxisSpecifier;
import java.net.URL;
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
    private ChangeListener<PrinterStatusEnumeration> printerStatusChangeListener = null;
    private ChangeListener<PauseStatus> pauseStatusChangeListener = null;

    private CommandLink goAheadAndOpenTheLid = null;
    private CommandLink dontOpenTheLid = null;
    private String openLidPrinterTooHotTitle = null;
    private String openLidPrinterTooHotInfo = null;

    private String transferringDataString = null;

    private PrinterColourMap colourMap = PrinterColourMap.getInstance();

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
    private BooleanProperty advancedControlsVisible = new SimpleBooleanProperty(
        false);

    private Printer lastSelectedPrinter = null;

    @FXML
    void home(ActionEvent event)
    {
        try
        {
            printerToUse.transmitStoredGCode("Home_all", false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Couldn't run home macro");
        }
    }

    @FXML
    void pausePrint(ActionEvent event)
    {
        printerToUse.getPrintQueue().pausePrint();
    }

    @FXML
    void resumePrint(ActionEvent event)
    {
        printerToUse.getPrintQueue().resumePrint();
    }

    @FXML
    void cancelPrint(ActionEvent event)
    {
        printerToUse.getPrintQueue().abortPrint();
    }

    @FXML
    void ejectReel(ActionEvent event)
    {
        if (printerToUse != null)
        {
            try
            {
                printerToUse.transmitDirectGCode(GCodeConstants.ejectFilament1,
                                                 false);
            } catch (RoboxCommsException ex)
            {
                steno.error("Error when sending eject filament");
            }
        }
    }

    @FXML
    void unlockLid(ActionEvent event)
    {
        boolean openTheLid = true;

        if (printerToUse.bedTemperatureProperty().get() > 60)
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
            if (printerToUse.getPrintQueue().printInProgressProperty().get()
                == true)
            {
                printerToUse.getPrintQueue().abortPrint();
            }
            try
            {
                printerToUse.
                    transmitDirectGCode(GCodeConstants.goToOpenLidPosition,
                                        false);
            } catch (RoboxCommsException ex)
            {
                steno.error("Error when moving sending open lid command");
            }
        }
    }

    @FXML
    void jogButton(ActionEvent event)
    {
        JogButton button = (JogButton) event.getSource();
        AxisSpecifier axis = button.getAxis();
        float distance = button.getDistance();

        try
        {
            printerToUse.
                transmitDirectGCode(GCodeConstants.carriageRelativeMoveMode,
                                    true);
            if (button.getUseG1())
            {
                if (button.getFeedRate() > 0)
                {
                    printerToUse.transmitDirectGCode(String.format(
                        "G1 " + axis.name() + "%.2f F%d", distance,
                        button.getFeedRate()), true);
                } else
                {
                    printerToUse.transmitDirectGCode(String.format(
                        "G1 " + axis.name() + "%.2f", distance), true);
                }
            } else
            {
                printerToUse.transmitDirectGCode(String.format(
                    "G0 " + axis.name() + "%.2f", distance), true);
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send printer jog command");
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
                printerToUse.transmitDirectGCode(
                    GCodeConstants.switchOffHeadLEDs, true);
                headLEDOn = false;
            } else
            {
                printerToUse.transmitDirectGCode(GCodeConstants.switchOnHeadLEDs,
                                                 true);
                headLEDOn = true;
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send head LED command");
        }
    }

    @FXML
    void toggleHeadFan(ActionEvent event)
    {
        try
        {
            if (printerToUse.getHeadFanOn())
            {
                printerToUse.transmitDirectGCode(GCodeConstants.switchOffHeadFan,
                                                 true);
            } else
            {
                printerToUse.transmitDirectGCode(GCodeConstants.switchOnHeadFan,
                                                 true);
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send head fan command");
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
                    printerToUse.transmitSetAmbientLEDColour(Color.BLACK);
                    printerColourRectangle.setFill(Color.BLACK);
                    break;
                case WHITE:
                    printerToUse.transmitSetAmbientLEDColour(
                        colourMap.displayToPrinterColour(Color.WHITE));
                    printerColourRectangle.setFill(Color.WHITE);
                    break;
                case COLOUR:
                    printerToUse.transmitSetAmbientLEDColour(
                        printerToUse.getPrinterColour());
                    printerColourRectangle.setFill(
                        colourMap.printerToDisplayColour(
                            printerToUse.getPrinterColour()));
                    break;
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send ambient LED command");
        }
    }

    @FXML
    void selectNozzle1(ActionEvent event)
    {
        selectNozzle(0);
    }

    @FXML
    void selectNozzle2(ActionEvent event)
    {
        selectNozzle(1);
    }

    @FXML
    void openNozzle(ActionEvent event)
    {
        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.openNozzle, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send open nozzle");
        }
    }

    @FXML
    void closeNozzle(ActionEvent event)
    {
        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.closeNozzle, true);
        } catch (RoboxCommsException ex)
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
        statusScreenState = StatusScreenState.getInstance();

        ResourceBundle i18nBundle = DisplayManager.getLanguageBundle();

        transferringDataString = i18nBundle.getString(
            "PrintQueue.SendingToPrinter");

        goAheadAndOpenTheLid = new Dialogs.CommandLink(i18nBundle.getString(
            "dialogs.openLidPrinterHotGoAheadHeading"), i18nBundle.getString(
                                                           "dialogs.openLidPrinterHotGoAheadInfo"));
        dontOpenTheLid = new Dialogs.CommandLink(i18nBundle.getString(
            "dialogs.openLidPrinterHotDontOpenHeading"), null);
        openLidPrinterTooHotTitle = i18nBundle.getString(
            "dialogs.openLidPrinterHotTitle");
        openLidPrinterTooHotInfo = i18nBundle.getString(
            "dialogs.openLidPrinterHotInfo");
        progressLayerLabel.setText(i18nBundle.getString("dialogs.progressLayerLabel"));
        progressETCLabel.setText(i18nBundle.getString("dialogs.progressETCLabel"));

        reelDataChangeListener = new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t,
                Boolean t1)
            {
                setFilamentColour(printerToUse.loadedFilamentProperty().get());
            }
        };

        reelChangeListener = new ChangeListener<EEPROMState>()
        {
            @Override
            public void changed(ObservableValue<? extends EEPROMState> ov,
                EEPROMState t,
                EEPROMState t1)
            {
                if (t1 == EEPROMState.PROGRAMMED)
                {
                    setFilamentColour(
                        lastSelectedPrinter.loadedFilamentProperty().get());
                }
            }
        };

        printerColourChangeListener = new ChangeListener<Color>()
        {
            @Override
            public void changed(ObservableValue<? extends Color> observable,
                Color oldValue, Color newValue)
            {
                if (ambientLEDState == AmbientLEDState.COLOUR)
                {
                    printerColourRectangle.setFill(
                        colourMap.printerToDisplayColour(
                            newValue));
                }
            }
        };

        printerStatusChangeListener = (ObservableValue<? extends PrinterStatusEnumeration> observable, PrinterStatusEnumeration oldValue, PrinterStatusEnumeration newValue) ->
        {
            processPrinterStatusChange(newValue);
        };

        pauseStatusChangeListener = new ChangeListener<PauseStatus>()
        {
            @Override
            public void changed(
                ObservableValue<? extends PauseStatus> observable, PauseStatus oldValue, PauseStatus newValue)
            {
                processPauseStatusChange(newValue);
            }
        };

        printerSilhouette.setVisible(true);
        printerClosedImage.setVisible(false);
        printerOpenImage.setVisible(false);
        printerColourRectangle.setVisible(false);

        progressGroup.setVisible(false);

        ejectReelButton.setVisible(false);
        unlockLidButton.setVisible(false);
        temperatureWarning.setVisible(false);

        reel.setVisible(false);
        filamentRectangle.setVisible(false);

        printControlButtons.setVisible(false);

        if (statusScreenState.getCurrentlySelectedPrinter() != null)
        {
            Printer printer = statusScreenState.getCurrentlySelectedPrinter();
            processPrinterStatusChange(printer.getPrinterStatus());
            processPauseStatusChange(printer.getPauseStatus());
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

                        progressGroup.setVisible(false);
                        printerColourRectangle.setVisible(false);

                        ejectReelButton.setVisible(false);
                        unlockLidButton.setVisible(false);
                        temperatureWarning.setVisible(false);

                        reel.setVisible(false);
                        filamentRectangle.setVisible(false);
                    } else
                    {
                        unbindFromSelectedPrinter();

                        progressGroup.visibleProperty().bind(
                            selectedPrinter.getPrintQueue().
                            printInProgressProperty());
                        progressBar.progressProperty().bind(
                            selectedPrinter.getPrintQueue().progressProperty());
                        progressPercent.textProperty().bind(Bindings.multiply(
                                selectedPrinter.getPrintQueue().progressProperty(),
                                100).asString("%.0f%%"));
                        BooleanBinding progressVisible
                        = selectedPrinter.printerStatusProperty().isNotEqualTo(
                            PrinterStatusEnumeration.PRINTING)
                        .or(Bindings.and(
                                selectedPrinter.getPrintQueue().
                                linesInPrintingFileProperty().greaterThan(
                                    0),
                                selectedPrinter.getPrintQueue().
                                progressProperty().greaterThanOrEqualTo(
                                    0)));
                        BooleanBinding progressETCVisible
                        = Bindings.and(
                            selectedPrinter.getPrintQueue().etcAvailableProperty(),
                            Bindings.or(
                                selectedPrinter.printerStatusProperty().isEqualTo(
                                    PrinterStatusEnumeration.PRINTING),
                                selectedPrinter.printerStatusProperty().isEqualTo(
                                    PrinterStatusEnumeration.SENDING_TO_PRINTER)));
                        progressPercent.visibleProperty().bind(progressVisible);
                        progressETC.visibleProperty().bind(progressETCVisible);
                        progressETC.textProperty().bind(new StringBinding()
                            {
                                {
                                    super.bind(
                                        selectedPrinter.getPrintQueue().
                                        progressETCProperty());
                                }

                                @Override
                                protected String computeValue()
                                {
                                    int secondsRemaining = selectedPrinter.getPrintQueue().
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
                                        selectedPrinter.getPrintQueue().progressCurrentLayerProperty(),
                                        selectedPrinter.getPrintQueue().progressNumLayersProperty());
                                }

                                @Override
                                protected String computeValue()
                                {
                                    int currentLayer = selectedPrinter.getPrintQueue().progressCurrentLayerProperty().get();
                                    int totalLayers = selectedPrinter.getPrintQueue().progressNumLayersProperty().get();
                                    return (currentLayer + 1) + "/" + totalLayers;
                                }
                        });
                        progressLayers.visibleProperty().bind(progressETCVisible);
                        progressLayerLabel.visibleProperty().bind(progressETCVisible);
                        progressETCLabel.visibleProperty().bind(progressETCVisible);
                        secondProgressBar.visibleProperty().bind(
                            selectedPrinter.getPrintQueue().
                            sendingDataToPrinterProperty());
                        secondProgressBar.progressProperty().bind(
                            selectedPrinter.getPrintQueue().
                            secondaryProgressProperty());
                        secondProgressPercent.visibleProperty().bind(
                            selectedPrinter.getPrintQueue().
                            sendingDataToPrinterProperty());
                        secondProgressPercent.textProperty().bind(
                            Bindings.format(
                                "%s %.0f%%", transferringDataString,
                                Bindings.multiply(
                                    selectedPrinter.getPrintQueue().
                                    secondaryProgressProperty(),
                                    100)));
                        progressTitle.textProperty().bind(
                            selectedPrinter.getPrintQueue().titleProperty());
                        progressMessage.textProperty().bind(
                            selectedPrinter.getPrintQueue().messageProperty());

                        printerColourRectangle.setVisible(true);
                        printerColourRectangle.setFill(
                            colourMap.printerToDisplayColour(
                                selectedPrinter.getPrinterColour()));
                        selectedPrinter.printerColourProperty().addListener(
                            printerColourChangeListener);

                        ejectReelButton.visibleProperty().bind(
                            selectedPrinter.Filament1LoadedProperty().and(
                                selectedPrinter.printerStatusProperty().
                                isNotEqualTo(
                                    PrinterStatusEnumeration.PRINTING)));

                        unlockLidButton.setVisible(true);
                        unlockLidButton.disableProperty().bind(
                            selectedPrinter.LidOpenProperty().or(
                                selectedPrinter.whyAreWeWaitingProperty().
                                isNotEqualTo(
                                    WhyAreWeWaitingState.NOT_WAITING)));
                        temperatureWarning.visibleProperty().bind(
                            selectedPrinter.bedTemperatureProperty().greaterThan(
                                ApplicationConfiguration.bedHotAboveDegrees));

                        selectedPrinter.reelDataChangedProperty().addListener(
                            reelDataChangeListener);
                        selectedPrinter.reelEEPROMStatusProperty().addListener(
                            reelChangeListener);
                        setFilamentColour(
                            selectedPrinter.loadedFilamentProperty().get());
                        filamentRectangle.visibleProperty().bind(
                            selectedPrinter.reelEEPROMStatusProperty().isEqualTo(
                                EEPROMState.PROGRAMMED));
                        reel.visibleProperty().bind(
                            selectedPrinter.reelEEPROMStatusProperty().isEqualTo(
                                EEPROMState.PROGRAMMED));

                        processPrinterStatusChange(selectedPrinter.printerStatusProperty().get());
                        selectedPrinter.printerStatusProperty().addListener(printerStatusChangeListener);
                        processPauseStatusChange(selectedPrinter.pauseStatusProperty().get());
                        selectedPrinter.pauseStatusProperty().addListener(pauseStatusChangeListener);

                        printerOpenImage.visibleProperty().bind(selectedPrinter.LidOpenProperty());
                        printerClosedImage.visibleProperty().bind(selectedPrinter.LidOpenProperty().not());

                        advancedControlsVisible.unbind();
                        advancedControlsVisible.bind(
                            (selectedPrinter.printerStatusProperty().isEqualTo(
                                PrinterStatusEnumeration.IDLE).and(
                                selectedPrinter.whyAreWeWaitingProperty().
                                isEqualTo(
                                    WhyAreWeWaitingState.NOT_WAITING))).
                            or(
                                selectedPrinter.printerStatusProperty().
                                isEqualTo(
                                    PrinterStatusEnumeration.PAUSED)));
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
            headFanButton, headLEDButton
        };

        for (Node node : advancedControls)
        {
            node.setVisible(false);
        }

        if (statusScreenState.getCurrentlySelectedPrinter() != null)
        {
            boolean visible = (statusScreenState.getCurrentlySelectedPrinter().
                getPrinterStatus() == PrinterStatusEnumeration.IDLE
                || statusScreenState.getCurrentlySelectedPrinter().getPrinterStatus()
                == PrinterStatusEnumeration.PAUSED);
            for (Node node : advancedControls)
            {
                node.setVisible(visible);
            }
        }

        advancedControlsVisible.addListener(
            new ChangeListener<Boolean>()
            {
                @Override
                public void changed(
                    ObservableValue<? extends Boolean> observable,
                    Boolean oldValue, Boolean newValue
                )
                {
                    for (Node node : advancedControls)
                    {
                        node.setVisible(newValue);
                    }
                }
            }
        );
    }

    private void processPrinterStatusChange(PrinterStatusEnumeration printerStatus)
    {
        switch (printerStatus)
        {
            case PAUSED:
            case SENDING_TO_PRINTER:
            case PRINTING:
                printControlButtons.setVisible(true);
                break;
            default:
                printControlButtons.setVisible(false);
                break;
        }
    }

    private void processPauseStatusChange(PauseStatus pauseStatus)
    {
        switch (pauseStatus)
        {
            case NOT_PAUSED:
                resumePrintButton.setVisible(false);
                resumePrintButton.setDisable(true);
                pausePrintButton.setVisible(true);
                pausePrintButton.setDisable(false);
                cancelPrintButton.setVisible(false);
                break;
            case PAUSED:
                resumePrintButton.setVisible(true);
                resumePrintButton.setDisable(false);
                pausePrintButton.setVisible(false);
                pausePrintButton.setDisable(true);
                cancelPrintButton.setVisible(true);
                break;
            case PAUSE_PENDING:
                resumePrintButton.setVisible(false);
                resumePrintButton.setDisable(false);
                pausePrintButton.setVisible(true);
                pausePrintButton.setDisable(true);
                cancelPrintButton.setVisible(false);
                break;
            case RESUME_PENDING:
                resumePrintButton.setVisible(true);
                resumePrintButton.setDisable(true);
                pausePrintButton.setVisible(false);
                pausePrintButton.setDisable(false);
                cancelPrintButton.setVisible(false);
                break;
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

    private void selectNozzle(int nozzleNumber)
    {
        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.selectNozzle
                + nozzleNumber,
                                             true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error selecting nozzle");
        }
    }

    private void unbindFromSelectedPrinter()
    {
        progressGroup.visibleProperty().unbind();
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
            lastSelectedPrinter.printerColourProperty().removeListener(
                printerColourChangeListener);
            lastSelectedPrinter.reelDataChangedProperty().removeListener(
                reelDataChangeListener);
            lastSelectedPrinter.reelEEPROMStatusProperty().removeListener(
                reelChangeListener);
            lastSelectedPrinter.printerStatusProperty().removeListener(printerStatusChangeListener);
            lastSelectedPrinter.pauseStatusProperty().removeListener(pauseStatusChangeListener);
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

    private void setFilamentColour(Filament filament)
    {
        if (filament != null)
        {
            filamentRectangle.setFill(filament.getDisplayColour());
        }
    }
}
