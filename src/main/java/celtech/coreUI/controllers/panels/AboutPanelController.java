package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

/**
 *
 * @author Ian
 */
public class AboutPanelController implements Initializable, PrinterListChangesListener
{

    private SettingsScreenState settingsScreenState = null;

    @FXML
    private Label roboxSerialNumber;

    @FXML
    private Label headSerialNumber;

    @FXML
    private Label version;

    @FXML
    private void okPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().returnToLastMode();
    }

    @FXML
    private void systemInformationPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.SYSTEM_INFORMATION);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        settingsScreenState = SettingsScreenState.getInstance();
        Lookup.getPrinterListChangesNotifier().addListener(this);
        version.setText(ApplicationConfiguration.getApplicationVersion());
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
        roboxSerialNumber.setText(printer.getPrinterIdentity().printerUniqueIDProperty().get());
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
        roboxSerialNumber.setText("");
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
        headSerialNumber.setText(printer.headProperty().get().uniqueIDProperty().get());
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        headSerialNumber.setText("");
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
