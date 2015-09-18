package celtech.coreUI.controllers.utilityPanels;

import celtech.Lookup;
import celtech.coreUI.controllers.StatusInsetController;
import celtech.printerControl.PrintJob;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.model.Printer;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ProjectPanelController implements Initializable, StatusInsetController
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            ProjectPanelController.class.getName());

    @FXML
    private Label projectName;

    private Printer currentPrinter = null;
    private ChangeListener<PrintJob> printJobChangeListener = (ObservableValue<? extends PrintJob> ov, PrintJob t, PrintJob t1) ->
    {
        if (t1 != null)
        {
            try
            {
                projectName.setText(t1.getStatistics().getProjectName());
            } catch (IOException ex)
            {
                projectName.setText("");
                steno.warning("Unable to retrieve project name");
            }
        } else
        {
            projectName.setText("");
        }
    };

    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        Lookup.getSelectedPrinterProperty().addListener((ObservableValue<? extends Printer> ov, Printer lastPrinter, Printer newPrinter) ->
        {
            if (currentPrinter != null)
            {
                currentPrinter.getPrintEngine().printJobProperty().removeListener(printJobChangeListener);
            }
            
            if (newPrinter == null)
            {
                projectName.setText("");
            } else
            {
                newPrinter.getPrintEngine().printJobProperty().addListener(printJobChangeListener);
            }
        });
    }
}
