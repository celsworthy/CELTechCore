/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.panels;

import celtech.coreUI.components.RestrictedTextField;
import celtech.coreUI.controllers.SlidablePanel;
import celtech.coreUI.controllers.SlideOutHandleController;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.model.Printer;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterStatusSlideOutPanelController implements Initializable, SlidablePanel
{

    private StatusScreenState statusScreenState = null;
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
        statusScreenState.getCurrentlySelectedPrinter().sendRawGCode(gcodeEntryField.getText(), true);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        statusScreenState = StatusScreenState.getInstance();

        gcodeTranscript.setEditable(false);
        gcodeTranscript.setScrollTop(Double.MAX_VALUE);

        gcodeTranscriptListener = new ListChangeListener<String>()
        {

            @Override
            public void onChanged(ListChangeListener.Change<? extends String> change)
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
            }
        };

        populateGCodeArea();

        statusScreenState.currentlySelectedPrinterProperty().addListener((ObservableValue<? extends Printer> ov, Printer t, Printer t1) ->
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
        gcodeEditParent.visibleProperty().bind(statusScreenState.currentlySelectedPrinterProperty().isNotNull());

//        sendGCodeButton.setDefaultButton(true);
        gcodeEntryField.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>()
                                    {

                                        @Override
                                        public void handle(KeyEvent t)
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
                                        }
        });
    }

    private void populateGCodeArea()
    {
        if (statusScreenState.getCurrentlySelectedPrinter() != null)
        {
            gcodeTranscript.setText("");
            for (String gcodeLine : statusScreenState.getCurrentlySelectedPrinter().gcodeTranscriptProperty())
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
}
