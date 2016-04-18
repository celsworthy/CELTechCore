package celtech.coreUI.controllers.panels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.printerControl.model.Head;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.printerControl.model.PrinterIdentity;
import celtech.roboxbase.printerControl.model.PrinterListChangesListener;
import celtech.roboxbase.printerControl.model.Reel;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

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
    private Label infoLabel;

    @FXML
    private Text bdLabel;

    @FXML
    private Text bdNames;

    @FXML
    private Text hwengLabel;

    @FXML
    private Text hwengNames;

    @FXML
    private Text swengLabel;

    @FXML
    private Text swengNames;

    @FXML
    private Text amTitleText1;

    @FXML
    private Text amTitleText2;

    @FXML
    private Text amTitleText3;

    @FXML
    private VBox logoBox;

    private Printer currentPrinter = null;

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
        BaseLookup.getPrinterListChangesNotifier().addListener(this);
        version.setText(BaseConfiguration.getApplicationVersion());

        DisplayManager.getInstance().getDisplayScalingModeProperty().addListener(new ChangeListener<DisplayManager.DisplayScalingMode>()
        {
            @Override
            public void changed(ObservableValue<? extends DisplayManager.DisplayScalingMode> ov, DisplayManager.DisplayScalingMode t, DisplayManager.DisplayScalingMode scalingMode)
            {
                switch (scalingMode)
                {
                    case NORMAL:
                        infoLabel.setStyle("-fx-font-size: 21px");
                        hwengLabel.setStyle("-fx-font-size: 21px");
                        hwengNames.setStyle("-fx-font-size: 21px");
                        swengLabel.setStyle("-fx-font-size: 21px");
                        swengNames.setStyle("-fx-font-size: 21px");
                        bdLabel.setStyle("-fx-font-size: 21px");
                        bdNames.setStyle("-fx-font-size: 21px");
                        amTitleText1.setStyle("-fx-font-size: 100px");
                        amTitleText2.setStyle("-fx-font-size: 100px");
                        amTitleText3.setStyle("-fx-font-size: 14px");
                        logoBox.setScaleX(1);
                        logoBox.setScaleY(1);
                        break;
                    default:
                        infoLabel.setStyle("-fx-font-size: 14px");
                        hwengLabel.setStyle("-fx-font-size: 14px");
                        hwengNames.setStyle("-fx-font-size: 14px");
                        swengLabel.setStyle("-fx-font-size: 14px");
                        swengNames.setStyle("-fx-font-size: 14px");
                        bdLabel.setStyle("-fx-font-size: 14px");
                        bdNames.setStyle("-fx-font-size: 14px");
                        amTitleText1.setStyle("-fx-font-size: 70px");
                        amTitleText2.setStyle("-fx-font-size: 70px");
                        amTitleText3.setStyle("-fx-font-size: 10px");
                        logoBox.setScaleX(0.8);
                        logoBox.setScaleY(0.8);
                        break;

                }
            }
        });

        ApplicationStatus.getInstance().modeProperty().addListener(new ChangeListener<ApplicationMode>()
        {
            @Override
            public void changed(ObservableValue<? extends ApplicationMode> ov, ApplicationMode t, ApplicationMode t1)
            {
                if (t1 == ApplicationMode.ABOUT
                        && currentPrinter != null)
                {
                    headSerialNumber.setText(currentPrinter.headProperty().get().getFormattedSerial());
                }
            }
        });
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

        currentPrinter = printer;
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
        roboxSerialNumber.setText("");
        currentPrinter = null;
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
        headSerialNumber.setText(printer.headProperty().get().getFormattedSerial());
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
