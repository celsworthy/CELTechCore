package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.coreUI.components.RestrictedTextField;
import celtech.coreUI.controllers.SlidablePanel;
import celtech.coreUI.controllers.SlideOutHandleController;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterStatusSlideOutPanelController implements Initializable, SlidablePanel, PrinterListChangesListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(PrinterStatusSlideOutPanelController.class.getName());
    private ListChangeListener<String> gcodeTranscriptListener = null;

    @FXML
    private SlideOutHandleController SlideOutHandleController;

    @FXML
    private VBox gcodeEditParent;

    @FXML
    private RestrictedTextField gcodeEntryField;

    @FXML
    private TextArea gcodeTranscript;

    @FXML
    private Button sendGCodeButton;

    @FXML
    private HBox slideout;
    
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
        Lookup.getCurrentlySelectedPrinterProperty().get().sendRawGCode(gcodeEntryField.getText(), true);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        gcodeEntryField.disableProperty().bind(Lookup.getUserPreferences().advancedModeProperty().not());
        sendGCodeButton.disableProperty().bind(Lookup.getUserPreferences().advancedModeProperty().not());
        
        gcodeTranscript.setEditable(false);
        gcodeTranscript.setScrollTop(Double.MAX_VALUE);

        gcodeTranscriptListener = (ListChangeListener.Change<? extends String> change) ->
        {
            while (change.next())
            {
                if (change.wasAdded())
                {
                    for (String additem : change.getAddedSubList())
                    {
                        gcodeTranscript.appendText(additem);
                    }
                } else if (change.wasRemoved())
                {
                    for (String additem : change.getRemoved())
                    {
                    }
                } else if (change.wasReplaced())
                {
                } else if (change.wasUpdated())
                {
                }
            }
        };

        populateGCodeArea();

        Lookup.getCurrentlySelectedPrinterProperty().addListener((ObservableValue<? extends Printer> ov, Printer t, Printer t1) ->
        {
            if (t1 != null)
            {
                t1.gcodeTranscriptProperty().addListener(gcodeTranscriptListener);
                
            }

            if (t != null)
            {
                t.gcodeTranscriptProperty().removeListener(gcodeTranscriptListener);
            }

            populateGCodeArea();
        });

//        gcodeEntryField.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>()
//        {
//            @Override
//            public void handle(KeyEvent t)
//            {
//                KeyCode key = t.getCode();
//                if (key == KeyCode.ENTER && gcodeEntryField.getText().length() != 0)
//                {
//                    sendGCode(null);
//                }
//            }
//        });
        gcodeEditParent.visibleProperty().bind(Lookup.getCurrentlySelectedPrinterProperty().isNotNull());

//        sendGCodeButton.setDefaultButton(true);
        gcodeEntryField.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent t) ->
                                    {
                                        if (t.getCode() == KeyCode.UP)
                                        {
                                            String allText = gcodeTranscript.getText();

                                            if (gcodeTranscript.getSelectedText().length() > 0)
                                            {
                                                int selectionStart = gcodeTranscript.getSelection().getStart();
                                                int selectionEnd = gcodeTranscript.getSelection().getEnd();

                                                int lastposition = allText.lastIndexOf('\n', selectionStart);

                                                if (lastposition > 0)
                                                {
                                                    int penultimatePosition = allText.lastIndexOf("\n", lastposition - 1);
                                                    if (penultimatePosition > 0)
                                                    {
                                                        gcodeTranscript.selectRange(penultimatePosition, lastposition);
                                                    }
                                                }
                                            } else
                                            {
                                                int lastposition = allText.lastIndexOf('\n');

                                                if (lastposition > 0)
                                                {
                                                    int penultimatePosition = allText.lastIndexOf("\n", lastposition - 1);
                                                    if (penultimatePosition > 0)
                                                    {
                                                        gcodeTranscript.selectRange(penultimatePosition, lastposition);
                                                    }
                                                }
                                            }
                                        } else if (t.getCode() == KeyCode.DOWN)
                                        {
                                            gcodeTranscript.selectNextWord();
                                        }
        });

    }

    

    private void populateGCodeArea()
    {
        if (Lookup.getCurrentlySelectedPrinterProperty().get() != null)
        {
            gcodeTranscript.setText("");
            for (String gcodeLine : Lookup.getCurrentlySelectedPrinterProperty().get().gcodeTranscriptProperty())
            {
                gcodeTranscript.appendText(gcodeLine);
            }
        } else
        {
            gcodeTranscript.setText("");
        }

    }

    /**
     *
     */
    @Override
    public void slideIn()
    {
        SlideOutHandleController.slideIn();
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
        gcodePanel.disableProperty().bind(Lookup.getUserPreferences().advancedModeProperty().not());
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
        gcodePanel.disableProperty().unbind();
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
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
    }
}
