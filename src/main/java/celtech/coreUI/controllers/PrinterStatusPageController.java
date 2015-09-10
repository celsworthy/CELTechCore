package celtech.coreUI.controllers;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PauseStatus;
import celtech.configuration.PrinterColourMap;
import celtech.coreUI.components.JogButton;
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
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
    private StackPane statusPane;

    @FXML
    private ImageView baseNoReels;

    @FXML
    private ImageView baseReel2;

    @FXML
    private ImageView baseReel1;

    @FXML
    private ImageView reel1Background;
    private ColorAdjust reel1BackgroundColourEffect = new ColorAdjust();

    @FXML
    private ImageView reel2Background;
    private ColorAdjust reel2BackgroundColourEffect = new ColorAdjust();

    @FXML
    private ImageView baseReelBoth;

    @FXML
    private ImageView doorClosed;

    @FXML
    private ImageView doorOpen;

    @FXML
    private ImageView singleMaterialHead;

    @FXML
    private ImageView ambientLight;

    private ColorAdjust ambientColourEffect = new ColorAdjust();

    @FXML
    private ImageView dualMaterialHead;

    @FXML
    private ImageView bed;

    @FXML
    private Group temperatureWarning;

    @FXML
    private JogButton extruder_minus100;

    @FXML
    private JogButton extruder_minus20;

    @FXML
    private JogButton extruder_minus5;

    @FXML
    private JogButton extruder_plus100;

    @FXML
    private JogButton extruder_plus20;

    @FXML
    private JogButton extruder_plus5;

    @FXML
    private JogButton x_minus100;

    @FXML
    private JogButton x_minus10;

    @FXML
    private JogButton x_minus1;

    @FXML
    private JogButton x_plus100;

    @FXML
    private JogButton x_plus10;

    @FXML
    private JogButton x_plus1;

    @FXML
    private JogButton y_minus100;

    @FXML
    private JogButton y_minus10;

    @FXML
    private JogButton y_minus1;

    @FXML
    private JogButton y_plus100;

    @FXML
    private JogButton y_plus10;

    @FXML
    private JogButton y_plus1;

    @FXML
    private JogButton z_minus10;

    @FXML
    private JogButton z_minus1;

    @FXML
    private JogButton z_minus0_1;

    @FXML
    private JogButton z_plus10;

    @FXML
    private JogButton z_plus1;

    @FXML
    private JogButton z_plus0_1;

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
            setupAmbientLight();
        };

        printerStatusChangeListener = (ObservableValue<? extends PrinterStatus> observable, PrinterStatus oldValue, PrinterStatus newValue) ->
        {
            setAdvancedControlsVisibility();
        };

        pauseStatusChangeListener = (ObservableValue<? extends PauseStatus> observable, PauseStatus oldValue, PauseStatus newValue) ->
        {
            setAdvancedControlsVisibility();
        };

        doorClosed.setVisible(false);
        doorOpen.setVisible(false);

        ambientLight.setEffect(ambientColourEffect);
        reel1Background.setEffect(reel1BackgroundColourEffect);
        reel2Background.setEffect(reel2BackgroundColourEffect);

        setupBaseDisplay();
        setupAmbientLight();
        setupHead();

        temperatureWarning.setVisible(false);

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

                        setupBaseDisplay();

                        if (selectedPrinter == null)
                        {
                            unbindFromSelectedPrinter();

                            setupBaseDisplay();
                            setupAmbientLight();

                            temperatureWarning.setVisible(false);
                        } else
                        {
                            unbindFromSelectedPrinter();

                            setupBaseDisplay();
                            setupAmbientLight();
                            selectedPrinter.getPrinterIdentity().printerColourProperty().addListener(
                                    printerColourChangeListener);

                            temperatureWarning.visibleProperty().bind(
                                    selectedPrinter.getPrinterAncillarySystems().bedTemperatureProperty()
                                    .greaterThan(ApplicationConfiguration.bedHotAboveDegrees));

                            selectedPrinter.printerStatusProperty().addListener(
                                    printerStatusChangeListener);
                            selectedPrinter.pauseStatusProperty().addListener(
                                    pauseStatusChangeListener);
                            doorOpen.visibleProperty().bind(selectedPrinter.
                                    getPrinterAncillarySystems().doorOpenProperty());
                            doorClosed.visibleProperty().bind(selectedPrinter.
                                    getPrinterAncillarySystems().doorOpenProperty().not());

                        }

                        setAdvancedControlsVisibility();

                        lastSelectedPrinter = selectedPrinter;
                    }
                });

    }

    private void setupBaseDisplay()
    {
        if (printerToUse == null)
        {
            baseNoReels.setVisible(false);
            baseReel1.setVisible(false);
            baseReel2.setVisible(false);
            baseReelBoth.setVisible(false);
            bed.setVisible(false);
        } else
        {
            if (printerToUse.reelsProperty().containsKey(0)
                    && printerToUse.reelsProperty().containsKey(1))
            {
                baseNoReels.setVisible(false);
                baseReel1.setVisible(false);
                baseReel2.setVisible(false);
                baseReelBoth.setVisible(true);
                bed.setVisible(true);
            } else if (printerToUse.reelsProperty().containsKey(0))
            {
                baseNoReels.setVisible(false);
                baseReel1.setVisible(true);
                baseReel2.setVisible(false);
                baseReelBoth.setVisible(false);
                bed.setVisible(true);
            } else if (printerToUse.reelsProperty().containsKey(1))
            {
                baseNoReels.setVisible(false);
                baseReel1.setVisible(false);
                baseReel2.setVisible(true);
                baseReelBoth.setVisible(false);
                bed.setVisible(true);
            } else
            {
                baseNoReels.setVisible(true);
                baseReel1.setVisible(false);
                baseReel2.setVisible(false);
                baseReelBoth.setVisible(false);
                bed.setVisible(true);
            }
        }

        setupReel1Colour();
        setupReel2Colour();
    }

    private void setColorAdjustFromDesiredColour(ColorAdjust effect, Color desiredColor)
    {
        effect.setHue(hueConverter(desiredColor.getHue()));
        effect.setBrightness(desiredColor.getBrightness() - 1);
        effect.setSaturation(desiredColor.getSaturation());
        steno.info("Colour - h=" + hueConverter(desiredColor.getHue()) + " s=" + desiredColor.getSaturation() + " b" + desiredColor.getBrightness());
    }

    private void setupReel1Colour()
    {
        if (printerToUse == null
                || !printerToUse.reelsProperty().containsKey(0))
        {
            reel1Background.setVisible(false);
        } else
        {
            Color reel1Colour = printerToUse.reelsProperty().get(0).displayColourProperty().get();
            setColorAdjustFromDesiredColour(reel1BackgroundColourEffect, reel1Colour);
            reel1Background.setVisible(true);
        }
    }

    private void setupReel2Colour()
    {
        if (printerToUse == null
                || !printerToUse.reelsProperty().containsKey(1))
        {
            reel2Background.setVisible(false);
        } else
        {
            Color reel2Colour = printerToUse.reelsProperty().get(1).displayColourProperty().get();
            setColorAdjustFromDesiredColour(reel2BackgroundColourEffect, reel2Colour);
            reel2Background.setVisible(true);
        }
    }

    private double hueConverter(double hueCyl)
    {
        double returnedHue = 0;
        if (hueCyl <= 180)
        {
            returnedHue = hueCyl / 180.0;
        } else
        {
            returnedHue = -(360 - hueCyl) / 180.0;
        }
        return returnedHue;
    }

    private void setupAmbientLight()
    {
        if (printerToUse == null)
        {
            ambientLight.setVisible(false);
        } else
        {
            Color ambientColour = colourMap.printerToDisplayColour(printerToUse.getPrinterIdentity().printerColourProperty().get());
            setColorAdjustFromDesiredColour(ambientColourEffect, ambientColour);
            ambientLight.setVisible(true);
        }
    }

    private void setupHead()
    {
        if (printerToUse == null
                || printerToUse.headProperty().get() == null)
        {
            singleMaterialHead.setVisible(false);
            dualMaterialHead.setVisible(false);
        } else
        {
            if (printerToUse.headProperty().get().headTypeProperty().get() == Head.HeadType.SINGLE_MATERIAL_HEAD)
            {
                singleMaterialHead.setVisible(true);
                dualMaterialHead.setVisible(false);
            } else if (printerToUse.headProperty().get().headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD)
            {
                singleMaterialHead.setVisible(false);
                dualMaterialHead.setVisible(true);
            } else
            {
                singleMaterialHead.setVisible(false);
                dualMaterialHead.setVisible(false);
            }
        }
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
        final double beginWidth = 1500;
        final double beginHeight = 1106;
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
        if (lastSelectedPrinter != null)
        {
            lastSelectedPrinter.getPrinterIdentity().printerColourProperty().removeListener(
                    printerColourChangeListener);
            lastSelectedPrinter.printerStatusProperty().removeListener(printerStatusChangeListener);
            lastSelectedPrinter.pauseStatusProperty().removeListener(pauseStatusChangeListener);
        }

        temperatureWarning.visibleProperty().unbind();
        temperatureWarning.setVisible(false);

        doorOpen.visibleProperty().unbind();
        doorOpen.setVisible(false);
        doorClosed.visibleProperty().unbind();
        doorClosed.setVisible(false);
    }

    private Node loadInsetPanel(String innerPanelFXMLName, String title,
            BooleanProperty visibleProperty)
    {
        URL insetPanelURL = getClass().getResource(
                ApplicationConfiguration.fxmlUtilityPanelResourcePath + innerPanelFXMLName);
        FXMLLoader loader = new FXMLLoader(insetPanelURL, Lookup.getLanguageBundle());
        Node wrappedPanel = null;
        try
        {
            Node insetPanel = loader.load();
            if (title != null)
            {
                wrappedPanel = wrapPanelInOuterPanel(insetPanel, title, visibleProperty);
                wrappedPanel.visibleProperty().bind(visibleProperty);
            } else
            {
                wrappedPanel = insetPanel;
            }
        } catch (IOException ex)
        {
            steno.exception("Unable to load inset panel: " + innerPanelFXMLName, ex);
        }
        return wrappedPanel;
    }

    private Node wrapPanelInOuterPanel(Node insetPanel, String title,
            BooleanProperty visibleProperty)
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
            outerPanelController.setPreferredVisibility(visibleProperty);
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
        Node diagnosticPanel = loadInsetPanel("DiagnosticPanel.fxml", "diagnosticPanel.title",
                Lookup.getUserPreferences().showDiagnosticsProperty());
        Node gcodePanel = loadInsetPanel("GCodePanel.fxml", "gcodeEntry.title",
                Lookup.getUserPreferences().showGCodeProperty());
        vBoxLeft.getChildren().add(diagnosticPanel);
        vBoxLeft.getChildren().add(gcodePanel);

        VBox vBoxRight = new VBox();
        vBoxRight.setSpacing(30);
        Node projectPanel = loadInsetPanel("ProjectPanel.fxml", null, null);
        Node printAdjustmentsPanel = loadInsetPanel("tweakPanel.fxml", "printAdjustmentsPanel.title",
                Lookup.getUserPreferences().showAdjustmentsProperty());
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
        setupHead();
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        setupHead();
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
        setupBaseDisplay();
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
        setupBaseDisplay();
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
        setupBaseDisplay();
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
