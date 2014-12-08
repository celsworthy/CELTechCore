package celtech.coreUI.controllers;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PauseStatus;
import celtech.configuration.PrinterColourMap;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.JogButton;
import celtech.coreUI.visualisation.threed.StaticModelOverlay;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.Reel;
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
import javafx.collections.MapChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterStatusPageController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        PrinterStatusPageController.class.getName());
    private Printer printerToUse = null;
    private ChangeListener<Boolean> reelDataChangeListener = null;
    private MapChangeListener<Integer, Reel> reelChangeListener = null;
    private ChangeListener<Color> printerColourChangeListener = null;
    private ChangeListener<PrinterStatus> printerStatusChangeListener = null;
    private ChangeListener<PauseStatus> pauseStatusChangeListener = null;
    private StaticModelOverlay staticModelOverlay = null;

    private String transferringDataString = null;

    private PrinterColourMap colourMap = PrinterColourMap.getInstance();

    private NumberFormat threeDPformatter;
    private NumberFormat fiveDPformatter;

    @FXML
    private ImageView printerSilhouette;

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
    private StackPane statusPane;

    @FXML
    private JogButton extruder_minus5;

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
    private JogButton x_minus10;

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
    private AnchorPane container;

    private Node[] advancedControls = null;

    private Printer lastSelectedPrinter = null;

    private final BooleanProperty showProgressGroup = new SimpleBooleanProperty(false);

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

        ResourceBundle i18nBundle = DisplayManager.getLanguageBundle();

        transferringDataString = i18nBundle.getString(
            "PrintQueue.SendingToPrinter");

        progressLayerLabel.setText(i18nBundle.getString("dialogs.progressLayerLabel"));
        progressETCLabel.setText(i18nBundle.getString("dialogs.progressETCLabel"));

        reelDataChangeListener = (ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) ->
        {
            //TODO modify to support multiple reels
            filamentRectangle.setFill(printerToUse.reelsProperty().get(0).displayColourProperty().get());
        };

        reelChangeListener = new MapChangeListener<Integer, Reel>()
        {
            @Override
            public void onChanged(MapChangeListener.Change<? extends Integer, ? extends Reel> change)
            {
                if (change.wasAdded())
                {
                    filamentRectangle.setFill(change.getValueAdded().displayColourProperty().get());
                    filamentRectangle.setVisible(true);
                    reel.setVisible(true);
                } else
                {
                    //TODO modify to support multiple reels
                    filamentRectangle.setVisible(false);
                    reel.setVisible(false);
                }
            }
        };

        printerColourChangeListener = (ObservableValue<? extends Color> observable, Color oldValue, Color newValue) ->
        {
            printerColourRectangle.setFill(
                colourMap.printerToDisplayColour(
                    newValue));
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

        temperatureWarning.setVisible(false);

        reel.setVisible(false);
        filamentRectangle.setVisible(false);

        advancedControls = new Node[]
        {
            extruder_minus100, extruder_minus20, extruder_minus5, extruder_plus100, extruder_plus20, extruder_plus5,
            x_minus1, x_minus10, x_minus100, x_plus1, x_plus10, x_plus100,
            y_minus1, y_minus10, y_minus100, y_plus1, y_plus10, y_plus100,
            z_minus0_1, z_minus1, z_minus10, z_plus0_1, z_plus1, z_plus10
        };
        setAdvancedControlsVisibility(false);

        pausePrintButton.setVisible(false);
        resumePrintButton.setVisible(false);
        cancelPrintButton.setVisible(false);

        if (Lookup.getCurrentlySelectedPrinterProperty().get() != null)
        {
            Printer printer = Lookup.getCurrentlySelectedPrinterProperty().get();
            processPrinterStatusChange(printer.printerStatusProperty().get());
        }

        Lookup.currentlySelectedPrinterProperty().addListener(
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

                        temperatureWarning.visibleProperty().bind(
                            selectedPrinter.getPrinterAncillarySystems().bedTemperatureProperty()
                            .greaterThan(ApplicationConfiguration.bedHotAboveDegrees));

                        selectedPrinter.reelsProperty().addListener(reelChangeListener);

                        //TODO The printer status page only has room to display one filament colour at the moment...
                        if (selectedPrinter.reelsProperty().get(0) != null)
                        {
                            filamentRectangle.setFill(selectedPrinter.reelsProperty().get(0).displayColourProperty().get());
                        }

                        processPrinterStatusChange(selectedPrinter.printerStatusProperty().get());
                        bindToSelectedPrinter(selectedPrinter);
                        selectedPrinter.printerStatusProperty().addListener(printerStatusChangeListener);

                        printerOpenImage.visibleProperty().bind(selectedPrinter.getPrinterAncillarySystems().lidOpenProperty());
                        printerClosedImage.visibleProperty().bind(selectedPrinter.getPrinterAncillarySystems().lidOpenProperty().not());

                    }

                    lastSelectedPrinter = selectedPrinter;
                }
            });

//        staticModelOverlay = new StaticModelOverlay(statusPane.getWidth(), statusPane.getHeight());
//        statusPane.getChildren().add(staticModelOverlay.getSubScene());
    }

    private void setAdvancedControlsVisibility(boolean visible)
    {
        for (Node node : advancedControls)
        {
            node.setVisible(visible);
        }
    }

    private void bindToSelectedPrinter(Printer printer)
    {
        pausePrintButton.visibleProperty().bind(printer.canPauseProperty());
        resumePrintButton.visibleProperty().bind(printer.canResumeProperty());
        cancelPrintButton.visibleProperty().bind(printer.canCancelProperty());
    }

    private void processPrinterStatusChange(PrinterStatus printerStatus)
    {
        if (printerStatus == null)
        {
            setAdvancedControlsVisibility(false);
        } else
        {
            switch (printerStatus)
            {
                case IDLE:
                    showProgressGroup.set(false);
                    setAdvancedControlsVisibility(true);
                    break;
                case PAUSING:
                    showProgressGroup.set(false);
                    setAdvancedControlsVisibility(false);
                    break;
                case RESUMING:
                    showProgressGroup.set(false);
                    setAdvancedControlsVisibility(false);
                    break;
                case PAUSED:
                    showProgressGroup.set(false);
                    setAdvancedControlsVisibility(true);
                    break;
                case SENDING_TO_PRINTER:
                    if (!lastSelectedPrinter.macroTypeProperty().isNotNull().get())
                    {
                        showProgressGroup.set(true);
                    }
                    break;
                case PRINTING:
                    showProgressGroup.set(true);
                    setAdvancedControlsVisibility(false);
//                    staticModelOverlay.showModelForPrintJob(lastSelectedPrinter.printJobIDProperty().get());
                    break;
                case EXECUTING_MACRO:
                    if (lastSelectedPrinter.macroTypeProperty().isNotNull().get()
                        && lastSelectedPrinter.macroTypeProperty().get().isInterruptible())
                    {
                        showProgressGroup.set(true);
                    }
                    setAdvancedControlsVisibility(false);
                    break;
                case SLICING:
                case POST_PROCESSING:
                    showProgressGroup.set(true);
                    setAdvancedControlsVisibility(false);
                    break;
                default:
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

            lastSelectedPrinter.reelsProperty()
                .values()
                .stream()
                .forEach(reel -> reel.dataChangedToggleProperty().removeListener(reelDataChangeListener));

            //TODO modify to support multiple reels
            lastSelectedPrinter.reelsProperty().removeListener(reelChangeListener);
            lastSelectedPrinter.printerStatusProperty().removeListener(printerStatusChangeListener);

            pausePrintButton.visibleProperty().unbind();
            pausePrintButton.setVisible(false);
            resumePrintButton.visibleProperty().unbind();
            resumePrintButton.setVisible(false);
            cancelPrintButton.visibleProperty().unbind();
            cancelPrintButton.setVisible(false);
        }

        filamentRectangle.visibleProperty().unbind();

        temperatureWarning.visibleProperty().unbind();
        temperatureWarning.setVisible(false);

        printerOpenImage.visibleProperty().unbind();
        printerOpenImage.setVisible(false);
        printerClosedImage.visibleProperty().unbind();
        printerClosedImage.setVisible(false);
    }
}
