package celtech.coreUI.controllers;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PauseStatus;
import celtech.configuration.PrinterColourMap;
import celtech.coreUI.components.JogButton;
import celtech.coreUI.components.ProgressDisplay;
import celtech.coreUI.components.TesStatusBar;
import celtech.coreUI.controllers.utilityPanels.OuterPanelController;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterStatusPageController implements Initializable, PrinterListChangesListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        PrinterStatusPageController.class.getName());
    private Printer printerToUse = null;
    private ChangeListener<Color> printerColourChangeListener = null;
    private ChangeListener<PrinterStatus> printerStatusChangeListener = null;
    private ChangeListener<PauseStatus> pauseStatusChangeListener = null;

    private String transferringDataString = null;

    private PrinterColourMap colourMap = PrinterColourMap.getInstance();

    private NumberFormat threeDPformatter;
    private NumberFormat fiveDPformatter;

    @FXML
    private AnchorPane container;

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
    private JogButton extruder_minus100;

    @FXML
    private JogButton x_minus10;

    @FXML
    private JogButton y_plus1;

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
    private JogButton z_minus10;

    @FXML
    private Group temperatureWarning;

    @FXML
    private ProgressDisplay progressDisplay;

    @FXML
    private TesStatusBar jim1;

    @FXML
    private TesStatusBar jim2;

    @FXML
    void appear(ActionEvent event)
    {
        jim1.startSlidingInToView();
    }

    @FXML
    void disappear(ActionEvent event)
    {
        jim1.startSlidingOutOfView();
    }

    private Node[] advancedControls = null;

    private Printer lastSelectedPrinter = null;

    @FXML
    void jogButton(ActionEvent event)
    {
        JogButton button = (JogButton) event.getSource();

        try
        {
            printerToUse.jogAxis(button.getAxis(), button.getDistance(), button.getFeedRate(),
                                 button.getUseG1());
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
        Lookup.getPrinterListChangesNotifier().addListener(this);

        Lookup.getUserPreferences().advancedModeProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                setAdvancedControlsVisibility();
            });

        threeDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        threeDPformatter.setMaximumFractionDigits(3);
        threeDPformatter.setGroupingUsed(false);

        fiveDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        fiveDPformatter.setMaximumFractionDigits(5);
        fiveDPformatter.setGroupingUsed(false);

        ResourceBundle i18nBundle = Lookup.getLanguageBundle();

        transferringDataString = i18nBundle.getString(
            "PrintQueue.SendingToPrinter");

        printerColourChangeListener = (ObservableValue<? extends Color> observable, Color oldValue, Color newValue) ->
        {
            printerColourRectangle.setFill(
                colourMap.printerToDisplayColour(
                    newValue));
        };

        printerStatusChangeListener = (ObservableValue<? extends PrinterStatus> observable, PrinterStatus oldValue, PrinterStatus newValue) ->
        {
            setAdvancedControlsVisibility();
        };

        pauseStatusChangeListener = (ObservableValue<? extends PauseStatus> observable, PauseStatus oldValue, PauseStatus newValue) ->
        {
            setAdvancedControlsVisibility();
        };

        printerSilhouette.setVisible(true);
        printerClosedImage.setVisible(false);
        printerOpenImage.setVisible(false);
        printerColourRectangle.setVisible(false);

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

        setAdvancedControlsVisibility();
        loadInsetPanels();

        Lookup.getSelectedPrinterProperty().addListener(
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

                        printerColourRectangle.setVisible(false);

                        temperatureWarning.setVisible(false);

                        reel.setVisible(false);
                        filamentRectangle.setVisible(false);
                    } else
                    {
                        unbindFromSelectedPrinter();

                        progressDisplay.bindToPrinter(printerToUse);

                        printerColourRectangle.setVisible(true);
                        printerColourRectangle.setFill(
                            colourMap.printerToDisplayColour(
                                selectedPrinter.getPrinterIdentity().printerColourProperty().get()));
                        selectedPrinter.getPrinterIdentity().printerColourProperty().addListener(
                            printerColourChangeListener);

                        temperatureWarning.visibleProperty().bind(
                            selectedPrinter.getPrinterAncillarySystems().bedTemperatureProperty()
                            .greaterThan(ApplicationConfiguration.bedHotAboveDegrees));

                        //TODO The printer status page only has room to display one filament colour at the moment...
                        if (selectedPrinter.reelsProperty().get(0) != null)
                        {
                            filamentRectangle.setFill(selectedPrinter.reelsProperty().get(0).
                                displayColourProperty().get());
                        }
                        selectedPrinter.printerStatusProperty().addListener(
                            printerStatusChangeListener);
                        selectedPrinter.pauseStatusProperty().addListener(
                            pauseStatusChangeListener);
                        printerOpenImage.visibleProperty().bind(selectedPrinter.
                            getPrinterAncillarySystems().doorOpenProperty());
                        printerClosedImage.visibleProperty().bind(selectedPrinter.
                            getPrinterAncillarySystems().doorOpenProperty().not());

                    }

                    setAdvancedControlsVisibility();

                    lastSelectedPrinter = selectedPrinter;
                }
            });

    }

    private void setAdvancedControlsVisibility()
    {
        boolean visible = false;

        if (printerToUse != null
            && Lookup.getUserPreferences().isAdvancedMode())
        {
            switch (printerToUse.printerStatusProperty().get())
            {
                case IDLE:
                    visible = true;
                    break;
                default:
                    break;
            }

            switch (printerToUse.pauseStatusProperty().get())
            {
                case PAUSED:
                    visible = true;
                    break;
                case PAUSE_PENDING:
                case RESUME_PENDING:
                    visible = false;
                    break;
                default:
                    break;
            }
        }

        for (Node node : advancedControls)
        {
            node.setVisible(visible);
        }
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
        progressDisplay.unbindFromPrinter();

        reel.visibleProperty().unbind();
        if (lastSelectedPrinter != null)
        {
            lastSelectedPrinter.getPrinterIdentity().printerColourProperty().removeListener(
                printerColourChangeListener);
            lastSelectedPrinter.printerStatusProperty().removeListener(printerStatusChangeListener);
            lastSelectedPrinter.pauseStatusProperty().removeListener(pauseStatusChangeListener);
        }

        filamentRectangle.visibleProperty().unbind();

        temperatureWarning.visibleProperty().unbind();
        temperatureWarning.setVisible(false);

        printerOpenImage.visibleProperty().unbind();
        printerOpenImage.setVisible(false);
        printerClosedImage.visibleProperty().unbind();
        printerClosedImage.setVisible(false);
    }

    private Node loadInsetPanel(String innerPanelFXMLName, String title)
    {
        URL insetPanelURL = getClass().getResource(
            ApplicationConfiguration.fxmlUtilityPanelResourcePath + innerPanelFXMLName);
        FXMLLoader loader = new FXMLLoader(insetPanelURL, Lookup.getLanguageBundle());
        Node wrappedPanel = null;
        try
        {
            Node insetPanel = loader.load();
            if (title != null) {
                wrappedPanel = wrapPanelInOuterPanel(insetPanel, title);
            } else {
                wrappedPanel = insetPanel;
            }    
        } catch (IOException ex)
        {
            steno.exception("Unable to load inset panel: " + innerPanelFXMLName, ex);
        }
        return wrappedPanel;
    }

    
    private Node wrapPanelInOuterPanel(Node insetPanel, String title)
    {
        URL outerPanelURL = getClass().getResource(
            ApplicationConfiguration.fxmlUtilityPanelResourcePath + "outerStatusPanel.fxml");
        FXMLLoader loader = new FXMLLoader(outerPanelURL, Lookup.getLanguageBundle());
        Node outerPanel = null;
        try
        {
            outerPanel = loader.load();
            OuterPanelController outerPanelController = loader.getController();
            outerPanelController.setInnerPanel(insetPanel);
            outerPanelController.setTitle(Lookup.i18n(title));
        } catch (IOException ex)
        {
            steno.exception("Unable to load outer panel", ex);
        }
        return outerPanel;
    }
    
    private void loadInsetPanels()
    {
        VBox vBoxLeft = new VBox();
        vBoxLeft.setSpacing(30);
        Node diagnosticPanel = loadInsetPanel("DiagnosticPanel.fxml", "diagnosticPanel.title");
        Node gcodePanel = loadInsetPanel("GCodePanel.fxml", "gcodeEntry.title");
        vBoxLeft.getChildren().add(diagnosticPanel);
        vBoxLeft.getChildren().add(gcodePanel);

        VBox vBoxRight = new VBox();
        vBoxRight.setSpacing(30);
        Node projectPanel = loadInsetPanel("ProjectPanel.fxml", null);
        Node printAdjustmentsPanel = loadInsetPanel("tweakPanel.fxml", "printAdjustmentsPanel.title");
        vBoxRight.getChildren().add(projectPanel);
        vBoxRight.getChildren().add(printAdjustmentsPanel);
        
        container.getChildren().add(vBoxLeft);
        AnchorPane.setTopAnchor(vBoxLeft, 30.0);
        AnchorPane.setLeftAnchor(vBoxLeft, 30.0);
        container.getChildren().add(vBoxRight);
        AnchorPane.setTopAnchor(vBoxRight, 30.0);
        AnchorPane.setRightAnchor(vBoxRight, 30.0);
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
        if (reelIndex == 0)
        {
            //TODO modify to support multiple reels
            filamentRectangle.setFill(printer.reelsProperty().get(0).displayColourProperty().get());
            filamentRectangle.setVisible(true);
            reel.setVisible(true);
        }
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
        if (reelIndex == 0)
        {
            //TODO modify to support multiple reels
            filamentRectangle.setVisible(false);
            this.reel.setVisible(false);
        }
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
        if (printer.reelsProperty().get(0) != null)
        {
            filamentRectangle.setFill(printer.reelsProperty().get(0).displayColourProperty().get());
        }
    }

    @Override
    public void whenExtruderAdded(Printer printer, int extruderIndex)
    {
    }

    @Override
    public void whenExtruderRemoved(Printer printer, int extruderIndex)
    {
    }

}
