package celtech.coreUI.controllers.utilityPanels;

import celtech.Lookup;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class TweakPanelController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(TweakPanelController.class.getName());

    @FXML
    private Slider speedMultiplierSlider;

    @FXML
    private HBox speedSliderHBox;

    private Printer selectedPrinter;

    private final ChangeListener<Number> feedRateChangeListener
        = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            speedMultiplierSlider.setValue(newValue.doubleValue());
        };

    private final ChangeListener<Number> speedMultiplierListener
        = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            try
            {
                selectedPrinter.changeFeedRateMultiplier(newValue.doubleValue());
            } catch (PrinterException ex)
            {
                steno.error("Error setting feed rate multiplier - " + ex.getMessage());
            }
        };

    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        speedSliderHBox.setVisible(false);

        Lookup.getCurrentlySelectedPrinterProperty().addListener(
            (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
            {
                whenPrinterSelected(newValue);
            });
    }

    private void whenPrinterSelected(Printer printer)
    {
        if (selectedPrinter != null)
        {
            unbindPrinter(selectedPrinter);
        }

        if (printer != null)
        {
            selectedPrinter = printer;
            bindPrinter(printer);
        }
    }

    private void bindPrinter(Printer printer)
    {
        speedSliderHBox.setVisible(printer.getPrinterMetaStatus().printerStatusProperty().get() == PrinterStatus.PRINTING);
        speedSliderHBox.visibleProperty().bind(printer.getPrinterMetaStatus().printerStatusProperty().isEqualTo(
            PrinterStatus.PRINTING));
        speedMultiplierSlider.setValue(printer.getPrinterAncillarySystems().
            feedRateMultiplierProperty().get());
        speedMultiplierSlider.valueProperty().addListener(speedMultiplierListener);

        printer.getPrinterAncillarySystems().feedRateMultiplierProperty().addListener(
            feedRateChangeListener);
    }

    private void unbindPrinter(Printer printer)
    {
        speedSliderHBox.visibleProperty().unbind();
        speedSliderHBox.setVisible(false);
        speedMultiplierSlider.valueProperty().removeListener(speedMultiplierListener);

        printer.getPrinterAncillarySystems().feedRateMultiplierProperty().removeListener(
            feedRateChangeListener);

    }

}
