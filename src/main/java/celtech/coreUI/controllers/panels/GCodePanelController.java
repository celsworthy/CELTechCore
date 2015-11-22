package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.components.RestrictedTextField;
import celtech.coreUI.controllers.StatusInsetController;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.codehaus.plexus.util.FileUtils;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class GCodePanelController implements Initializable, StatusInsetController
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            GCodePanelController.class.getName());
    private ListChangeListener<String> gcodeTranscriptListener = null;
    private Printer currentPrinter = null;

    @FXML
    private VBox gcodeEditParent;

    @FXML
    private RestrictedTextField gcodeEntryField;

    @FXML
    private ListView<String> gcodeTranscript;

    @FXML
    private Button sendGCodeButton;

    @FXML
    private HBox gcodePanel;

    @FXML
    void sendGCodeM(MouseEvent event)
    {
        fireGCodeAtPrinter();
    }

    @FXML
    void sendGCodeA(ActionEvent event)
    {
        fireGCodeAtPrinter();
    }

    private void fireGCodeAtPrinter()
    {
        gcodeEntryField.selectAll();
        String text = gcodeEntryField.getText();

        if (text.startsWith("!"))
        {
            String macroFilename = text.substring(1);
            String gcodeFileWithPathApp = ApplicationConfiguration.getCommonApplicationDirectory() + ApplicationConfiguration.macroFileSubpath + macroFilename + ".gcode";
            String gcodeFileWithPathUser = ApplicationConfiguration.getUserStorageDirectory() + ApplicationConfiguration.macroFileSubpath + macroFilename + ".gcode";
            String gcodeFileToUse = null;

            if (FileUtils.fileExists(gcodeFileWithPathUser))
            {
                gcodeFileToUse = gcodeFileWithPathUser;
            } else if (FileUtils.fileExists(gcodeFileWithPathApp))
            {
                gcodeFileToUse = gcodeFileWithPathApp;
            }

            //See if we can run a macro
            if (currentPrinter != null && gcodeFileToUse != null)
            {
                try
                {
                    currentPrinter.executeGCodeFile(gcodeFileToUse, true);
                    currentPrinter.gcodeTranscriptProperty().add(text);
                } catch (PrinterException ex)
                {
                    steno.error("Failed to run macro: " + macroFilename);
                }
            } else
            {
                steno.error("Can't run requested macro: " + macroFilename);
            }
        } else if (!text.equals(""))
        {
            Lookup.getSelectedPrinterProperty().get().sendRawGCode(text.toUpperCase(), true);
        }
    }

    private void selectLastItemInTranscript()
    {
        gcodeTranscript.getSelectionModel().selectLast();
        gcodeTranscript.scrollTo(gcodeTranscript.getSelectionModel().getSelectedIndex());
    }

    private boolean suppressReactionToGCodeEntryChange = false;

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        gcodeEntryField.disableProperty().bind(
                Lookup.getUserPreferences().advancedModeProperty().not());
        sendGCodeButton.disableProperty().bind(
                Lookup.getUserPreferences().advancedModeProperty().not());

        gcodeTranscriptListener = (ListChangeListener.Change<? extends String> change) ->
        {
            while (change.next())
            {
            }

            suppressReactionToGCodeEntryChange = true;
            selectLastItemInTranscript();
            suppressReactionToGCodeEntryChange = false;
        };

        gcodeTranscript.selectionModelProperty().get().selectedItemProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
            {
                if (!suppressReactionToGCodeEntryChange)
                {
                    gcodeEntryField.setText(gcodeTranscript.getSelectionModel().getSelectedItem());
                }
            }
        });

        Lookup.getSelectedPrinterProperty().addListener(
                (ObservableValue<? extends Printer> ov, Printer t, Printer t1) ->
                {
                    if (currentPrinter != null)
                    {
                        currentPrinter.gcodeTranscriptProperty().removeListener(gcodeTranscriptListener);
                    }

                    if (t1 != null)
                    {
                        gcodeTranscript.setItems(t1.gcodeTranscriptProperty());
                        t1.gcodeTranscriptProperty().addListener(gcodeTranscriptListener);
                    } else
                    {
                        gcodeTranscript.setItems(null);
                    }

                    currentPrinter = t1;
                });

        gcodeEditParent.visibleProperty().bind(Lookup.getSelectedPrinterProperty().isNotNull());

        gcodeEntryField.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent t) ->
        {
            if (t.getCode() == KeyCode.UP)
            {
                gcodeTranscript.getSelectionModel().selectPrevious();
                t.consume();
            } else if (t.getCode() == KeyCode.DOWN)
            {
                gcodeTranscript.getSelectionModel().selectNext();
                t.consume();
            }
        });

        gcodeTranscript.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent t) ->
        {
            if (t.getCode() == KeyCode.ENTER)
            {
                fireGCodeAtPrinter();
            }
        });

        gcodeTranscript.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() > 1)
                {
                    fireGCodeAtPrinter();
                }
            }
        });
    }
}
