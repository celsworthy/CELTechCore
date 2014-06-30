/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.coreUI.components.RestrictedTextField;
import celtech.printerControl.PrinterImpl;
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
public class GCodeEntryPanelController implements Initializable
{

    private StatusScreenState statusScreenState = null;
    private final Stenographer steno = StenographerFactory.getStenographer(GCodeEntryPanelController.class.getName());
    private ListChangeListener<String> gcodeTranscriptListener = null;

    @FXML
    private VBox gcodeEditParent;

    @FXML
    private RestrictedTextField gcodeEntryField;

    @FXML
    private TextArea gcodeListView;

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
        try
        {
            statusScreenState.getCurrentlySelectedPrinter().transmitDirectGCode(gcodeEntryField.getText(), true);
        } catch (RoboxCommsException ex)
        {
            steno.info("Error sending GCode");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        statusScreenState = StatusScreenState.getInstance();

        gcodeListView.setEditable(false);
        gcodeListView.setScrollTop(Double.MAX_VALUE);

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
                            gcodeListView.appendText(additem);
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

        statusScreenState.currentlySelectedPrinterProperty().addListener((ObservableValue<? extends PrinterImpl> ov, PrinterImpl t, PrinterImpl t1) ->
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
        gcodeEditParent.visibleProperty().bind(statusScreenState.currentlySelectedPrinterProperty().isNotNull().and(statusScreenState.modeProperty().isEqualTo(StatusScreenMode.ADVANCED)));

        sendGCodeButton.setDefaultButton(true);

        gcodeEntryField.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>()
        {

            @Override
            public void handle(KeyEvent t)
            {
                if (t.getCode() == KeyCode.UP)
                {
                    String allText = gcodeListView.getText();

                    if (gcodeListView.getSelectedText().length() > 0)
                    {
                        int selectionStart = gcodeListView.getSelection().getStart();
                        int selectionEnd = gcodeListView.getSelection().getEnd();

                        int lastposition = allText.lastIndexOf('\n', selectionStart);

                        if (lastposition > 0)
                        {
                            int penultimatePosition = allText.lastIndexOf("\n", lastposition - 1);
                            if (penultimatePosition > 0)
                            {
                                gcodeListView.selectRange(penultimatePosition, lastposition);
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
                                gcodeListView.selectRange(penultimatePosition, lastposition);
                            }
                        }
                    }
                } else if (t.getCode() == KeyCode.DOWN)
                {
                    gcodeListView.selectNextWord();
                }
            }
        });
    }

    private void populateGCodeArea()
    {
        if (statusScreenState.getCurrentlySelectedPrinter() != null)
        {
            for (String gcodeLine : statusScreenState.getCurrentlySelectedPrinter().gcodeTranscriptProperty())
            {
                gcodeListView.appendText(gcodeLine);
            }
        } else
        {
            gcodeListView.setText("");
        }

    }
}
