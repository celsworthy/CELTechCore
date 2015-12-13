package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterIdentity;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 *
 * @author Ian
 */
public class AboutPanelController implements Initializable, PrinterListChangesListener
{
    private final Clipboard clipboard = Clipboard.getSystemClipboard();
    private final ClipboardContent content = new ClipboardContent();
    
    @FXML
    private Label roboxSerialNumber;

    @FXML
    private Label headSerialNumber;

    @FXML
    private Label version;

    @FXML
    private void viewREADME(ActionEvent event)
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.WELCOME);
    }

    @FXML
    private void okPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().returnToLastMode();
    }

    @FXML
    private void copyPrinterSerialNumber(ActionEvent event)
    {
        content.putString(roboxSerialNumber.getText());
        clipboard.setContent(content);
    }

    @FXML
    private void copyHeadSerialNumber(ActionEvent event)
    {
        content.putString(headSerialNumber.getText());
        clipboard.setContent(content);
    }

    @FXML
    private void systemInformationPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.SYSTEM_INFORMATION);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        Lookup.getPrinterListChangesNotifier().addListener(this);
        version.setText(ApplicationConfiguration.getApplicationVersion());
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
        StringBuilder idString = new StringBuilder();
        PrinterIdentity identity = printer.getPrinterIdentity();
        idString.append(identity.printermodelProperty().get());
        idString.append("-");
        idString.append(identity.printereditionProperty().get());
        idString.append("-");
        idString.append(identity.printerweekOfManufactureProperty().get());
        idString.append(identity.printeryearOfManufactureProperty().get());
        idString.append("-");
        idString.append(identity.printerpoNumberProperty().get());
        idString.append("-");
        idString.append(identity.printerserialNumberProperty().get());
        idString.append("-");
        idString.append(identity.printercheckByteProperty().get());
        roboxSerialNumber.setText(idString.toString());
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
        roboxSerialNumber.setText("");
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
        headSerialNumber.setText(printer.headProperty().get().typeCodeProperty().get() + "-"
                + printer.headProperty().get().getWeekNumber() + printer.headProperty().get().getYearNumber() + "-"
                + printer.headProperty().get().getPONumber() + "-"
                + printer.headProperty().get().getSerialNumber() + "-"
                + printer.headProperty().get().getChecksum());
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
