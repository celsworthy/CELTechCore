/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.appManager.ProjectMode;
import celtech.coreUI.components.SlidingComponentDirection;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.model.Printer;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class GCodeEditorPanelController extends SlidingElementController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(GCodeEditorPanelController.class.getName());
    private ModelContainer boundModel = null;
    private IntegerProperty selectedGCodeLine = new SimpleIntegerProperty();

    @FXML
    private StackPane gcodeEditParent;

    @FXML
    private TabPane gcodeEditorTabPane;

    @FXML
    private ListView<String> gcodeListView;

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        setDimensions(gcodeEditParent.getPrefWidth(), gcodeEditParent.getPrefHeight(), gcodeEditParent.getLayoutX(), gcodeEditParent.getLayoutY());
        configurePanel(gcodeEditParent, SlidingComponentDirection.IN_FROM_RIGHT);

        if (Lookup.getCurrentlySelectedPrinterProperty().get() != null)
        {
            gcodeListView.setItems(Lookup.getCurrentlySelectedPrinterProperty().get().gcodeTranscriptProperty());
        }

        Lookup.getCurrentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {
            @Override
            public void changed(ObservableValue<? extends Printer> ov, Printer t, Printer t1)
            {
                if (t1 != null)
                {
                    gcodeListView.setItems(t1.gcodeTranscriptProperty());
                }
            }
        });

//        gcodeEntryField.addEventFilter(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>()
//        {
//            @Override
//            public void handle(KeyEvent t)
//            {
//                KeyCode key = t.getCode();
//                if (key.)
//            }
//        });
//        gcodeEditParent.visibleProperty().bind(statusScreenState.currentlySelectedPrinterProperty().isNotNull().and(statusScreenState.modeProperty().isEqualTo(StatusScreenMode.ADVANCED)));
        gcodeEditorTabPane.setOnMousePressed(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent t)
            {
                if (isSlidIn())
                {
                    startSlidingOut();
                } else
                {
                    if (!(t.getTarget() instanceof Text))
                    {
                        toggleSlide();
                    }
                } 
            }
        });

        gcodeListView.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                selectedGCodeLine.set(t1.intValue() + 1);
            }
        });

        selectedGCodeLine.addListener(new ChangeListener<Number>()
        {

            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                int lineToSelect = t1.intValue() - 1;
                gcodeListView.getSelectionModel().select(lineToSelect);
                gcodeListView.scrollTo(lineToSelect);
            }
        });

        slideIn();

    }

    /**
     *
     * @param modelList
     * @param project
     */
    public void configure(ObservableList<ModelContainer> modelList, Project project)
    {
//        gcodeEditParent.visibleProperty().bind(project.projectModeProperty().isEqualTo(ProjectMode.GCODE));
        gcodeEditParent.setVisible(false);
        modelList.addListener(new ListChangeListener<ModelContainer>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends ModelContainer> change)
            {
                while (change.next())
                {
                    if (change.wasAdded())
                    {
                        for (ModelContainer additem : change.getAddedSubList())
                        {
//                            steno.info("Added: " + additem.toString());
                            gcodeListView.setItems(additem.getGCodeLines());
                            boundModel = additem;
                            boundModel.selectedGCodeLineProperty().bindBidirectional(selectedGCodeLine);
                        }
                    } else if (change.wasRemoved())
                    {
                        for (ModelContainer additem : change.getRemoved())
                        {
                            if (boundModel != null)
                            {
                                boundModel.selectedGCodeLineProperty().unbind();
                            }
//                            steno.info("Removed: " + additem.toString());
                        }
                    } else if (change.wasReplaced())
                    {
                        steno.debug("Replaced: ");
                    } else if (change.wasUpdated())
                    {
                        steno.debug("Updated: ");
                    }
                }
            }
        });
    }
}
