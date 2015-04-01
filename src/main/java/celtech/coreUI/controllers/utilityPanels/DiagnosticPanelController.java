package celtech.coreUI.controllers.utilityPanels;

import celtech.Lookup;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class DiagnosticPanelController implements Initializable
{

    private Printer connectedPrinter = null;

    @FXML
    private Label extruder1Loaded;

    @FXML
    private Label lidSwitch;

    @FXML
    private Label zPositiveLimitSwitch;

    @FXML
    private Label extruder2Loaded;

    @FXML
    private Label extruder2Index;
    
    @FXML
    private Label extruder2Label;
    
    @FXML
    private Label extruder2LoadedLabel;
    
    @FXML
    private Label extruder2IndexLabel;
    
    @FXML
    private Label reelButtonSwitch;

    @FXML
    private Label printerID;

    @FXML
    private Label zLimitSwitch;

    @FXML
    private Label xLimitSwitch;

    @FXML
    private Label yLimitSwitch;

    @FXML
    private Label extruder1Index;

    @FXML
    private Label headID;

    private final ChangeListener<Head> headChangeListener = (ObservableValue<? extends Head> observable, Head oldValue, Head newValue) ->
    {
        if (newValue != null)
        {
            headID.textProperty().bind(newValue.uniqueIDProperty());
        } else if (oldValue != null)
        {
            headID.textProperty().unbind();
        }
    };

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        Lookup.getCurrentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {

            @Override
            public void changed(ObservableValue<? extends Printer> observable, Printer oldValue,
                Printer newValue)
            {
                if (connectedPrinter != null)
                {
                    unbindFromPrinter(connectedPrinter);
                }

                if (newValue != null)
                {
                    bindToPrinter(newValue);
                }
            }
        });
    }

    private void unbindFromPrinter(Printer printer)
    {
        if (connectedPrinter != null)
        {
            printer.headProperty().removeListener(headChangeListener);

            xLimitSwitch.textProperty().unbind();
            xLimitSwitch.setText("");
            printerID.textProperty().unbind();
            printerID.setText("");
            headID.textProperty().unbind();
            headID.setText("");
            yLimitSwitch.textProperty().unbind();
            yLimitSwitch.setText("");
            zLimitSwitch.textProperty().unbind();
            zLimitSwitch.setText("");
            zPositiveLimitSwitch.textProperty().unbind();
            zPositiveLimitSwitch.setText("");
            lidSwitch.textProperty().unbind();
            lidSwitch.setText("");
            reelButtonSwitch.textProperty().unbind();
            reelButtonSwitch.setText("");
            extruder1Loaded.visibleProperty().unbind();
            extruder1Loaded.textProperty().unbind();
            extruder2Label.visibleProperty().unbind();
            extruder2Label.setVisible(false);
            extruder2LoadedLabel.visibleProperty().unbind();
            extruder2LoadedLabel.setVisible(false);
            extruder2IndexLabel.visibleProperty().unbind();
            extruder2IndexLabel.setVisible(false);
            extruder2Loaded.visibleProperty().unbind();
            extruder1Loaded.setText("");
            extruder2Loaded.textProperty().unbind();
            extruder2Loaded.setText("");
            extruder1Index.visibleProperty().unbind();
            extruder1Index.textProperty().unbind();
            extruder1Index.setText("");
            extruder2Index.visibleProperty().unbind();
            extruder2Index.textProperty().unbind();
            extruder2Index.setText("");

            connectedPrinter = null;
        }
    }

    private void bindToPrinter(Printer printer)
    {
        if (connectedPrinter == null)
        {
            connectedPrinter = printer;

            printerID.textProperty().bind(printer.getPrinterIdentity().printerUniqueIDProperty());
            xLimitSwitch.textProperty().bind(printer.getPrinterAncillarySystems().
                xStopSwitchProperty().asString());
            yLimitSwitch.textProperty().bind(printer.getPrinterAncillarySystems().
                yStopSwitchProperty().asString());
            zLimitSwitch.textProperty().bind(printer.getPrinterAncillarySystems().
                zStopSwitchProperty().asString());
            zPositiveLimitSwitch.textProperty().bind(printer.getPrinterAncillarySystems().
                zTopStopSwitchProperty().asString());
            lidSwitch.textProperty().bind(printer.getPrinterAncillarySystems().doorOpenProperty().
                asString());
            reelButtonSwitch.textProperty().bind(printer.getPrinterAncillarySystems().
                reelButtonProperty().asString());
            //TODO modify to work with multiple extruders
            extruder1Loaded.visibleProperty().bind(printer.extrudersProperty().get(0).
                isFittedProperty());
            extruder1Loaded.textProperty().bind(printer.extrudersProperty().get(0).
                filamentLoadedProperty().asString());
            extruder1Index.visibleProperty().bind(printer.extrudersProperty().get(0).
                isFittedProperty());
            extruder1Index.textProperty().bind(printer.extrudersProperty().get(0).
                indexWheelStateProperty().asString());
            extruder2Label.visibleProperty().bind(printer.extrudersProperty().get(1).
                isFittedProperty());
            extruder2LoadedLabel.visibleProperty().bind(printer.extrudersProperty().get(1).
                isFittedProperty());
            extruder2IndexLabel.visibleProperty().bind(printer.extrudersProperty().get(1).
                isFittedProperty());
            extruder2Loaded.visibleProperty().bind(printer.extrudersProperty().get(1).
                isFittedProperty());
            extruder2Loaded.textProperty().bind(printer.extrudersProperty().get(1).
                filamentLoadedProperty().asString());
            extruder2Index.visibleProperty().bind(printer.extrudersProperty().get(1).
                isFittedProperty());
            extruder2Index.textProperty().bind(printer.extrudersProperty().get(1).
                indexWheelStateProperty().asString());

            printer.headProperty().addListener(headChangeListener);
        }
    }

}
