package celtech.coreUI.controllers.utilityPanels;

import celtech.Lookup;
import celtech.configuration.HeaterMode;
import celtech.coreUI.controllers.StatusInsetController;
import celtech.printerControl.PrintQueueStatus;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class TweakPanelController implements Initializable, StatusInsetController, PrinterListChangesListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(TweakPanelController.class.getName());

    @FXML
    private VBox container;

    @FXML
    private Label printSpeedDisplay;

    @FXML
    private Slider speedMultiplierSlider;

    @FXML
    private VBox extrusionMultiplier1Box;

    @FXML
    private Label extrusionMultiplier1Text;

    @FXML
    private Label extrusionMultiplier1Display;

    @FXML
    private Slider extrusionMultiplier1Slider;

    @FXML
    private VBox extrusionMultiplier2Box;

    @FXML
    private Label extrusionMultiplier2Display;

    @FXML
    private Slider extrusionMultiplier2Slider;

    @FXML
    private VBox nozzleTemperature1Box;

    @FXML
    private Label nozzle1Text;

    @FXML
    private Label nozzle1Display;

    @FXML
    private Slider nozzleTemperature1Slider;

    @FXML
    private VBox nozzleTemperature2Box;

    @FXML
    private Label nozzle2Display;

    @FXML
    private Slider nozzleTemperature2Slider;

    @FXML
    private Label bedDisplay;

    @FXML
    private Slider bedTemperatureSlider;

    private Printer currentPrinter = null;

    private final ChangeListener<PrintQueueStatus> printQueueStatusListener = new ChangeListener<PrintQueueStatus>()
    {
        @Override
        public void changed(ObservableValue<? extends PrintQueueStatus> ov, PrintQueueStatus t, PrintQueueStatus newStatus)
        {
            if (newStatus == PrintQueueStatus.PRINTING)
            {
                unbind();
                bind();
            } else
            {
                unbind();
            }
        }
    };

    private boolean inhibitFeedrate = false;
    private final ChangeListener<Number> feedRateChangeListener
            = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                inhibitFeedrate = true;
                speedMultiplierSlider.setValue(newValue.doubleValue() * 100.0);
                printSpeedDisplay.setText(String.format("%d%%", (int) (newValue.doubleValue() * 100.0)));
                inhibitFeedrate = false;
            };

    private final ChangeListener<Number> speedMultiplierSliderListener
            = (ObservableValue<? extends Number> observable, Number was, Number now) ->
            {
                if (!speedMultiplierSlider.isValueChanging() && !inhibitFeedrate)
                {
                    try
                    {
                        steno.info("Writing feedrate");
                        currentPrinter.changeFeedRateMultiplier(now.doubleValue() / 100.0);
                    } catch (PrinterException ex)
                    {
                        steno.error("Error setting feed rate multiplier - " + ex.getMessage());
                    }
                }
            };

    private boolean inhibitBed = false;
    private final ChangeListener<Number> bedTargetTemperatureChangeListener
            = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                inhibitBed = true;
                bedTemperatureSlider.setValue(newValue.doubleValue());
                bedDisplay.setText(String.format("%d°C", (int) newValue.doubleValue()));
                inhibitBed = false;
            };

    private final ChangeListener<Number> bedTempSliderListener
            = (ObservableValue<? extends Number> observable, Number was, Number now) ->
            {
                if (!bedTemperatureSlider.isValueChanging() && !inhibitBed)
                {
                    steno.info("Writing bed");
                    currentPrinter.setBedTargetTemperature(now.intValue());
                }
            };

    private boolean inhibitExtrusion1 = false;
    private final ChangeListener<Number> extrusionMultiplier1ChangeListener
            = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                inhibitExtrusion1 = true;
                extrusionMultiplier1Slider.setValue(newValue.doubleValue() * 100.0);
                extrusionMultiplier1Display.setText(String.format("%d%%", (int) (newValue.doubleValue() * 100.0)));
                inhibitExtrusion1 = false;
            };

    private final ChangeListener<Number> extrusionMultiplier1SliderListener
            = (ObservableValue<? extends Number> observable, Number was, Number now) ->
            {
                if (!extrusionMultiplier1Slider.isValueChanging() && !inhibitExtrusion1)
                {
                    try
                    {
                        steno.info("Writing extrusion 1");

                        currentPrinter.changeFilamentInfo("E", currentPrinter.extrudersProperty().get(0).filamentDiameterProperty().get(), extrusionMultiplier1Slider.valueProperty().doubleValue() / 100.0);
                    } catch (PrinterException ex)
                    {
                        steno.error("Failed to set extrusion multiplier");
                    }
                }
            };

    private boolean inhibitExtrusion2 = false;
    private final ChangeListener<Number> extrusionMultiplier2ChangeListener
            = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                inhibitExtrusion2 = true;
                extrusionMultiplier2Slider.setValue(newValue.doubleValue() * 100.0);
                extrusionMultiplier2Display.setText(String.format("%d%%", (int) (newValue.doubleValue() * 100.0)));
                inhibitExtrusion2 = false;
            };

    private final ChangeListener<Number> extrusionMultiplier2SliderListener
            = (ObservableValue<? extends Number> observable, Number was, Number now) ->
            {
                if (!extrusionMultiplier2Slider.isValueChanging() && !inhibitExtrusion2)
                {
                    try
                    {
                        steno.info("Writing extrusion 2");

                        currentPrinter.changeFilamentInfo("D", currentPrinter.extrudersProperty().get(1).filamentDiameterProperty().get(), extrusionMultiplier2Slider.valueProperty().doubleValue() / 100.0);
                    } catch (PrinterException ex)
                    {
                        steno.error("Failed to set extrusion multiplier");
                    }
                }
            };

    private boolean inhibitNozzleTemp1 = false;
    private final ChangeListener<Number> nozzleTemp1ChangeListener
            = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                inhibitNozzleTemp1 = true;
                nozzleTemperature1Slider.setValue(newValue.intValue());
                nozzle1Display.setText(String.format("%d°C", (int) newValue.doubleValue()));
                inhibitNozzleTemp1 = false;
            };

    private final ChangeListener<Number> nozzleTemp1SliderListener
            = (ObservableValue<? extends Number> observable, Number was, Number now) ->
            {
                if (!nozzleTemperature1Slider.isValueChanging() && !inhibitNozzleTemp1)
                {
                    //This is getting fired inappropriately
                    steno.info("Writing nozzle 1");

                    currentPrinter.setNozzleHeaterTargetTemperature(0, now.intValue());
                }
            };

    private boolean inhibitNozzleTemp2 = false;
    private final ChangeListener<Number> nozzleTemp2ChangeListener
            = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                inhibitNozzleTemp2 = true;
                nozzleTemperature2Slider.setValue(newValue.intValue());
                nozzle2Display.setText(String.format("%d°C", (int) newValue.doubleValue()));
                inhibitNozzleTemp2 = false;
            };

    private final ChangeListener<Number> nozzleTemp2SliderListener
            = (ObservableValue<? extends Number> observable, Number was, Number now) ->
            {
                if (!nozzleTemperature2Slider.isValueChanging() && !inhibitNozzleTemp2)
                {
                    steno.info("Writing nozzle 2");

                    currentPrinter.setNozzleHeaterTargetTemperature(1, nozzleTemperature2Slider.valueProperty().intValue());
                }
            };

    private final ChangeListener<HeaterMode> heaterModeListener
            = (ObservableValue<? extends HeaterMode> observable, HeaterMode oldValue, HeaterMode newValue) ->
            {
                if (newValue != oldValue)
                {
                    bindNozzleTemperatureDisplay();
                }
            };

    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        container.setVisible(false);
        Lookup.getSelectedPrinterProperty().addListener(
                (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newPrinter) ->
                {
                    bindToPrintEngineStatus(newPrinter);

                    if (newPrinter == null)
                    {
                        currentPrinter = null;
                    } else
                    {
                        currentPrinter = newPrinter;
                    }
                });

        Lookup.getPrinterListChangesNotifier().addListener(this);

        if (Lookup.getSelectedPrinterProperty().get() != null)
        {
            bindToPrintEngineStatus(Lookup.getSelectedPrinterProperty().get());
        }
    }

    private void bindToPrintEngineStatus(Printer printer)
    {
        if (currentPrinter != null)
        {
            currentPrinter.getPrintEngine().printQueueStatusProperty().removeListener(printQueueStatusListener);
            unbind();
        }

        currentPrinter = printer;
        if (currentPrinter != null)
        {
            printer.getPrintEngine().printQueueStatusProperty().addListener(printQueueStatusListener);
            if (printer.getPrintEngine().printQueueStatusProperty().get() == PrintQueueStatus.PRINTING)
            {
                bind();
            }
            Lookup.getPrinterListChangesNotifier().addListener(this);

        }
    }

    private void bind()
    {
        container.setVisible(true);
        printSpeedDisplay.setText(String.format("%d%%", (int) (currentPrinter.getPrinterAncillarySystems().
                feedRateMultiplierProperty().get() * 100.0)));
        speedMultiplierSlider.setValue(currentPrinter.getPrinterAncillarySystems().
                feedRateMultiplierProperty().get() * 100.0);
        speedMultiplierSlider.valueProperty().addListener(speedMultiplierSliderListener);

        currentPrinter.getPrinterAncillarySystems().feedRateMultiplierProperty().addListener(
                feedRateChangeListener);

        if (currentPrinter.getPrinterAncillarySystems().bedHeaterModeProperty().get() == HeaterMode.FIRST_LAYER)
        {
            bedDisplay.setText(String.format("%d°C", (int) currentPrinter.getPrinterAncillarySystems().
                    bedFirstLayerTargetTemperatureProperty().get()));
            bedTemperatureSlider.setValue(currentPrinter.getPrinterAncillarySystems().
                    bedFirstLayerTargetTemperatureProperty().get());
        } else
        {
            bedDisplay.setText(String.format("%d°C", (int) currentPrinter.getPrinterAncillarySystems().
                    bedTargetTemperatureProperty().get()));
            bedTemperatureSlider.setValue(currentPrinter.getPrinterAncillarySystems().
                    bedTargetTemperatureProperty().get());
        }
        bedTemperatureSlider.valueProperty().addListener(bedTempSliderListener);
        currentPrinter.getPrinterAncillarySystems().bedTargetTemperatureProperty().addListener(
                bedTargetTemperatureChangeListener);

        if (currentPrinter.extrudersProperty().get(0).isFittedProperty().get())
        {
            extrusionMultiplier1Display.setText(String.format("%d%%", (int) (currentPrinter.extrudersProperty().get(0).extrusionMultiplierProperty().doubleValue() * 100.0)));
            extrusionMultiplier1Slider.setValue(currentPrinter.extrudersProperty().get(0).extrusionMultiplierProperty().doubleValue() * 100.0);
            extrusionMultiplier1Slider.valueProperty().addListener(extrusionMultiplier1SliderListener);
            currentPrinter.extrudersProperty().get(0).extrusionMultiplierProperty().addListener(extrusionMultiplier1ChangeListener);
        }

        if (currentPrinter.extrudersProperty().get(1).isFittedProperty().get())
        {
            extrusionMultiplier2Display.setText(String.format("%d%%", (int) (currentPrinter.extrudersProperty().get(1).extrusionMultiplierProperty().doubleValue() * 100.0)));
            extrusionMultiplier2Slider.setValue(currentPrinter.extrudersProperty().get(1).extrusionMultiplierProperty().doubleValue() * 100.0);
            extrusionMultiplier2Slider.valueProperty().addListener(extrusionMultiplier2SliderListener);
            currentPrinter.extrudersProperty().get(1).extrusionMultiplierProperty().addListener(extrusionMultiplier2ChangeListener);
            extrusionMultiplier2Box.setVisible(true);
            extrusionMultiplier2Box.setMaxHeight(1000);
            extrusionMultiplier2Box.setMinHeight(0);
        } else
        {
            extrusionMultiplier2Box.setVisible(false);
            extrusionMultiplier2Box.setMaxHeight(0);
            extrusionMultiplier2Box.setMinHeight(0);
        }

        updateNozzleTemperatureDisplay();
        container.setVisible(true);
    }

    private void unbind()
    {
        container.setVisible(false);
        speedMultiplierSlider.valueProperty().removeListener(speedMultiplierSliderListener);
        bedTemperatureSlider.valueProperty().removeListener(bedTempSliderListener);
        extrusionMultiplier1Slider.valueProperty().removeListener(extrusionMultiplier1SliderListener);
        extrusionMultiplier2Slider.valueProperty().removeListener(extrusionMultiplier2SliderListener);

        currentPrinter.getPrinterAncillarySystems().feedRateMultiplierProperty().removeListener(
                feedRateChangeListener);
        currentPrinter.getPrinterAncillarySystems().bedTargetTemperatureProperty().removeListener(
                bedTargetTemperatureChangeListener);
        if (currentPrinter.extrudersProperty().get(0).isFittedProperty().get())
        {
            currentPrinter.extrudersProperty().get(0).extrusionMultiplierProperty().removeListener(
                    extrusionMultiplier1ChangeListener);
        }
        if (currentPrinter.extrudersProperty().get(1).isFittedProperty().get())
        {
            currentPrinter.extrudersProperty().get(1).extrusionMultiplierProperty().removeListener(
                    extrusionMultiplier2ChangeListener);
        }

        unbindNozzleTemperatureDisplay();
        updateNozzleTemperatureDisplay();
    }

    private void updateNozzleTemperatureDisplay()
    {
        if (currentPrinter != null
                && currentPrinter.headProperty().get() != null)
        {
            if (currentPrinter.headProperty().get().headTypeProperty().get() == Head.HeadType.SINGLE_MATERIAL_HEAD)
            {
                if (currentPrinter.headProperty().get().getNozzleHeaters().get(0).heaterModeProperty().get() == HeaterMode.FIRST_LAYER)
                {
                    nozzle1Display.setText(String.format("%d°C", (int) currentPrinter.headProperty().get().getNozzleHeaters().get(0).nozzleFirstLayerTargetTemperatureProperty().doubleValue()));
                    nozzleTemperature1Slider.setValue(currentPrinter.headProperty().get().getNozzleHeaters().get(0).nozzleFirstLayerTargetTemperatureProperty().get());
                } else
                {
                    nozzle1Display.setText(String.format("%d°C", (int) currentPrinter.headProperty().get().getNozzleHeaters().get(0).nozzleTargetTemperatureProperty().doubleValue()));
                    nozzleTemperature1Slider.setValue(currentPrinter.headProperty().get().getNozzleHeaters().get(0).nozzleTargetTemperatureProperty().get());
                }

                nozzleTemperature1Box.setVisible(true);
                nozzleTemperature1Box.setMaxHeight(1000);
                nozzleTemperature1Box.setMinHeight(0);
                nozzleTemperature2Box.setVisible(false);
                nozzleTemperature2Box.setMaxHeight(0);
                nozzleTemperature2Box.setMinHeight(0);
            } else
            {
                if (currentPrinter.headProperty().get().getNozzleHeaters().get(0).heaterModeProperty().get() == HeaterMode.FIRST_LAYER)
                {
                    nozzle1Display.setText(String.format("%d°C", (int) currentPrinter.headProperty().get().getNozzleHeaters().get(0).nozzleFirstLayerTargetTemperatureProperty().doubleValue()));
                    nozzleTemperature1Slider.setValue(currentPrinter.headProperty().get().getNozzleHeaters().get(0).nozzleFirstLayerTargetTemperatureProperty().get());
                } else if (currentPrinter.headProperty().get().getNozzleHeaters().get(0).heaterModeProperty().get() == HeaterMode.NORMAL)
                {
                    nozzle1Display.setText(String.format("%d°C", (int) currentPrinter.headProperty().get().getNozzleHeaters().get(0).nozzleTargetTemperatureProperty().doubleValue()));
                    nozzleTemperature1Slider.setValue(currentPrinter.headProperty().get().getNozzleHeaters().get(0).nozzleTargetTemperatureProperty().get());
                }

                if (currentPrinter.headProperty().get().getNozzleHeaters().get(1).heaterModeProperty().get() == HeaterMode.FIRST_LAYER)
                {
                    nozzle2Display.setText(String.format("%d°C", (int) currentPrinter.headProperty().get().getNozzleHeaters().get(1).nozzleFirstLayerTargetTemperatureProperty().doubleValue()));
                    nozzleTemperature2Slider.setValue(currentPrinter.headProperty().get().getNozzleHeaters().get(1).nozzleFirstLayerTargetTemperatureProperty().get());
                } else if (currentPrinter.headProperty().get().getNozzleHeaters().get(1).heaterModeProperty().get() == HeaterMode.NORMAL)
                {
                    nozzle2Display.setText(String.format("%d°C", (int) currentPrinter.headProperty().get().getNozzleHeaters().get(1).nozzleTargetTemperatureProperty().doubleValue()));
                    nozzleTemperature2Slider.setValue(currentPrinter.headProperty().get().getNozzleHeaters().get(1).nozzleTargetTemperatureProperty().get());
                }

                nozzleTemperature1Box.setVisible(true);
                nozzleTemperature1Box.setMaxHeight(1000);
                nozzleTemperature1Box.setMinHeight(0);
                nozzleTemperature2Box.setVisible(true);
                nozzleTemperature2Box.setMaxHeight(1000);
                nozzleTemperature2Box.setMinHeight(0);
            }
        } else
        {
            nozzleTemperature1Box.setVisible(false);
            nozzleTemperature1Box.setMaxHeight(0);
            nozzleTemperature1Box.setMinHeight(0);

            nozzleTemperature2Box.setVisible(false);
            nozzleTemperature2Box.setMaxHeight(0);
            nozzleTemperature2Box.setMinHeight(0);
        }
    }

    private void bindNozzleTemperatureDisplay()
    {
        unbindNozzleTemperatureDisplay();

        if (currentPrinter != null
                && currentPrinter.headProperty().get() != null)
        {
            nozzleTemperature1Slider.valueProperty().addListener(nozzleTemp1SliderListener);
            currentPrinter.headProperty().get().getNozzleHeaters().get(0).nozzleTargetTemperatureProperty().addListener(nozzleTemp1ChangeListener);
            currentPrinter.headProperty().get().getNozzleHeaters().get(0).nozzleFirstLayerTargetTemperatureProperty().addListener(nozzleTemp1ChangeListener);
            currentPrinter.headProperty().get().getNozzleHeaters().get(0).heaterModeProperty().addListener(heaterModeListener);

            if (currentPrinter.headProperty().get().getNozzleHeaters().size() > 1)
            {
                nozzleTemperature2Slider.valueProperty().addListener(nozzleTemp2SliderListener);
                currentPrinter.headProperty().get().getNozzleHeaters().get(1).nozzleTargetTemperatureProperty().addListener(nozzleTemp2ChangeListener);
                currentPrinter.headProperty().get().getNozzleHeaters().get(1).nozzleFirstLayerTargetTemperatureProperty().addListener(nozzleTemp2ChangeListener);
                currentPrinter.headProperty().get().getNozzleHeaters().get(1).heaterModeProperty().addListener(heaterModeListener);
            }
        }
    }

    private void unbindNozzleTemperatureDisplay()
    {
        nozzleTemperature1Slider.valueProperty().removeListener(nozzleTemp1SliderListener);
        nozzleTemperature2Slider.valueProperty().removeListener(nozzleTemp2SliderListener);

        if (currentPrinter != null
                && currentPrinter.headProperty().get() != null)
        {
            currentPrinter.headProperty().get().getNozzleHeaters().get(0).nozzleTargetTemperatureProperty().removeListener(nozzleTemp1ChangeListener);
            currentPrinter.headProperty().get().getNozzleHeaters().get(0).nozzleFirstLayerTargetTemperatureProperty().removeListener(nozzleTemp1ChangeListener);
            currentPrinter.headProperty().get().getNozzleHeaters().get(0).heaterModeProperty().removeListener(heaterModeListener);

            if (currentPrinter.headProperty().get().getNozzleHeaters().size() > 1)
            {
                currentPrinter.headProperty().get().getNozzleHeaters().get(1).nozzleTargetTemperatureProperty().removeListener(nozzleTemp2ChangeListener);
                currentPrinter.headProperty().get().getNozzleHeaters().get(1).nozzleFirstLayerTargetTemperatureProperty().removeListener(nozzleTemp2ChangeListener);
                currentPrinter.headProperty().get().getNozzleHeaters().get(1).heaterModeProperty().removeListener(heaterModeListener);
            }
        }
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
        bindNozzleTemperatureDisplay();
        updateNozzleTemperatureDisplay();
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        unbindNozzleTemperatureDisplay();
        updateNozzleTemperatureDisplay();
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
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
