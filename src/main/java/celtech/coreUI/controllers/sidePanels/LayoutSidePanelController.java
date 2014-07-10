/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.sidePanels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.coreUI.visualisation.SelectedModelContainers;
import celtech.coreUI.visualisation.ThreeDViewManager;
import celtech.modelcontrol.ModelContainer;
import java.net.URL;
import java.text.ParseException;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class LayoutSidePanelController implements Initializable,
    SidePanelManager
{

    private Stenographer steno = StenographerFactory.getStenographer(
        LayoutSidePanelController.class.getName());
    private Project boundProject = null;
    private ModelContainer boundModel = null;
    private StatusScreenState statusScreenState = null;

    @FXML
    private RestrictedNumberField widthTextField;

    @FXML
    private RestrictedNumberField depthTextField;

    @FXML
    private VBox selectedItemDetails;

    @FXML
    private RestrictedNumberField xAxisTextField;

    @FXML
    private RestrictedNumberField scaleTextField;

    @FXML
    private RestrictedNumberField heightTextField;

    @FXML
    private RestrictedNumberField yAxisTextField;

    @FXML
    private RestrictedNumberField rotationTextField;

    @FXML
    private TableView<ModelContainer> modelDataTableView;

    private TableColumn modelNameColumn = new TableColumn();
    private TableColumn scaleColumn = new TableColumn();
    private TableColumn rotationColumn = new TableColumn();

    private SelectedModelContainers selectionModel;
    private ListChangeListener<ModelContainer> selectionContainerModelsListener = null;
    private ChangeListener<ModelContainer> selectedItemListener = null;
    private DisplayManager displayManager = DisplayManager.getInstance();

    private ListChangeListener<ModelContainer> modelChangeListener = null;

    private ChangeListener<Number> modelScaleChangeListener = null;
    private ChangeListener<Number> modelRotationChangeListener = null;
    private ChangeListener<Number> widthListener = null;
    private ChangeListener<Number> heightListener = null;
    private ChangeListener<Number> depthListener = null;
    private ChangeListener<Number> xAxisListener = null;
    private ChangeListener<Number> yAxisListener = null;

    private final String scaleFormat = "%.0f";
    private final String rotationFormat = "%.0f";

    private ListChangeListener selectionListener = null;

    @FXML
    void changeToSettings(MouseEvent event)
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.SETTINGS);
    }

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        statusScreenState = StatusScreenState.getInstance();

        ResourceBundle languageBundle = DisplayManager.getLanguageBundle();
        String modelNameLabelString = languageBundle.getString(
            "sidePanel_layout.ModelNameLabel");
        String scaleLabelString = languageBundle.getString(
            "sidePanel_layout.ScaleLabel");
        String rotationLabelString = languageBundle.getString(
            "sidePanel_layout.RotationLabel");

        scaleTextField.setText("-");
        rotationTextField.setText("-");
        widthTextField.setText("-");
        depthTextField.setText("-");
        heightTextField.setText("-");
        xAxisTextField.setText("-");
        yAxisTextField.setText("-");

        modelNameColumn.setText(modelNameLabelString);
        modelNameColumn.setCellValueFactory(
            new PropertyValueFactory<ModelContainer, String>("modelName"));
        modelNameColumn.setMinWidth(170);
        modelNameColumn.setMaxWidth(170);
        modelNameColumn.setEditable(false);

        scaleColumn.setText(scaleLabelString);
        scaleColumn.setCellValueFactory(
            new PropertyValueFactory<ModelContainer, Double>("scale"));
        scaleColumn.setMinWidth(60);
        scaleColumn.setPrefWidth(60);
        scaleColumn.setMaxWidth(60);
        scaleColumn.setCellFactory(
            new Callback<TableColumn<ModelContainer, Double>, TableCell<ModelContainer, Double>>()
            {
                @Override
                public TableCell<ModelContainer, Double> call(
                    TableColumn<ModelContainer, Double> param)
                {
                    return new TableCell<ModelContainer, Double>()
                    {

                        @Override
                        protected void updateItem(Double item, boolean empty)
                        {
                            super.updateItem(item, empty);

                            if (!empty)
                            {
                                // Use a SimpleDateFormat or similar in the format method
                                setText(String.format("%.0f%%", item * 100));
                            } else
                            {
                                setText(null);
                            }
                        }
                    };
                    }
            });

        rotationColumn.setText(rotationLabelString);
        rotationColumn.setCellValueFactory(
            new PropertyValueFactory<ModelContainer, Double>("rotationY"));
        rotationColumn.setMinWidth(60);
        rotationColumn.setPrefWidth(60);
        rotationColumn.setMaxWidth(60);
        rotationColumn.setCellFactory(
            new Callback<TableColumn<ModelContainer, Double>, TableCell<ModelContainer, Double>>()
            {
                @Override
                public TableCell<ModelContainer, Double> call(
                    TableColumn<ModelContainer, Double> param)
                {
                    return new TableCell<ModelContainer, Double>()
                    {

                        @Override
                        protected void updateItem(Double item, boolean empty)
                        {
                            super.updateItem(item, empty);

                            if (!empty)
                            {
                                // Use a SimpleDateFormat or similar in the format method
                                setText(String.format("%.0f°", item));
                            } else
                            {
                                setText(null);
                            }
                        }
                    };
                    }
            });

        modelDataTableView.getColumns().addAll(modelNameColumn, scaleColumn,
                                               rotationColumn);
        modelDataTableView.getSelectionModel().setSelectionMode(
            SelectionMode.SINGLE);
        modelDataTableView.setEditable(true);
        modelDataTableView.getSortOrder().add(modelNameColumn);

        Label noModelsLoadedPlaceholder = new Label();
        noModelsLoadedPlaceholder.setText(languageBundle.getString(
            "sidePanel_layout.noModelsLoaded"));
        modelDataTableView.setPlaceholder(noModelsLoadedPlaceholder);

        modelScaleChangeListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t,
                Number t1)
            {
                scaleTextField.setText(String.format(scaleFormat,
                                                     t1.doubleValue() * 100));
            }
        };

        modelRotationChangeListener = new ChangeListener<Number>()
        {

            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t,
                Number t1)
            {
                rotationTextField.setText(String.format(rotationFormat, t1));
            }
        };

        selectionListener = new ListChangeListener<ModelContainer>()
        {

            @Override
            public void onChanged(
                ListChangeListener.Change<? extends ModelContainer> change)
            {
                try
                {
                    ModelContainer changedModel = null;
                    while (change.next())
                    {
                        if (change.wasAdded())
                        {
                            for (ModelContainer additem : change.getAddedSubList())
                            {
                                displayManager.selectModel(additem);
                                changedModel = additem;
                            }
                        } else if (change.wasRemoved())
                        {
                            for (ModelContainer additem : change.getRemoved())
                            {
                                displayManager.deselectModel(additem);
                            }
                        } else if (change.wasReplaced())
                        {
                            steno.info("Replaced: ");
                        } else if (change.wasUpdated())
                        {
                            steno.info("Updated: ");
                        }
                    }
                } catch (Exception ex)
                {
                    ex.printStackTrace();
                    steno.error("Got exception handling model change event: " + ex);
                }
            }
        };

        modelDataTableView.getSelectionModel().getSelectedItems().addListener(
            selectionListener);

        selectionContainerModelsListener = (ListChangeListener.Change<? extends ModelContainer> change) ->
        {
            while (change.next())
            {
                if (change.wasAdded())
                {
                    for (ModelContainer additem : change.getAddedSubList())
                    {
                        modelDataTableView.getSelectionModel().select(additem);
                    }
                } else if (change.wasRemoved())
                {
                    for (ModelContainer additem : change.getRemoved())
                    {
                        int modelIndex = modelDataTableView.getItems().indexOf(
                            additem);
                        if (modelIndex != -1)
                        {
                            modelDataTableView.getSelectionModel().clearSelection(
                                modelIndex);
                        }
                    }
                } else if (change.wasReplaced())
                {
                    steno.info("Replaced: ");
                } else if (change.wasUpdated())
                {
                    steno.info("Updated: ");
                }
            }
        };

        scaleTextField.setOnKeyPressed(
            new EventHandler<KeyEvent>()
            {

                @Override
                public void handle(KeyEvent t
                )
                {
                    switch (t.getCode())
                    {
                        case ENTER:
                        case TAB:
                            try
                            {
                                displayManager.getCurrentlyVisibleViewManager().scaleSelection(
                                    scaleTextField.getAsDouble() / 100.0);
                            } catch (ParseException ex)
                            {
                                steno.warning("Error converting scale "
                                    + scaleTextField.getText());
                            }
                            break;
                        case DECIMAL:
                        case BACK_SPACE:
                        case LEFT:
                        case RIGHT:
                            break;
                        default:
                            t.consume();
                            break;
                    }
                }
            }
        );

        scaleTextField.focusedProperty().addListener(
            new ChangeListener<Boolean>()
            {
                @Override
                public void changed(
                    ObservableValue<? extends Boolean> observable,
                    Boolean oldValue, Boolean newValue)
                {
                    if (newValue == false)
                    {
                        try
                        {
                            displayManager.getCurrentlyVisibleViewManager().scaleSelection(
                                scaleTextField.getAsDouble() / 100.0);
                        } catch (ParseException ex)
                        {
                            steno.warning("Error converting scale "
                                + scaleTextField.getText());
                        }
                    }
                    }
            });

        rotationTextField.setOnKeyPressed(
            new EventHandler<KeyEvent>()
            {

                @Override
                public void handle(KeyEvent t
                )
                {
                    switch (t.getCode())
                    {
                        case ENTER:
                        case TAB:
                            try
                            {
                                displayManager.getCurrentlyVisibleViewManager().rotateSelection(
                                    rotationTextField.getAsDouble());
                            } catch (ParseException ex)
                            {
                                steno.warning("Error converting rotation "
                                    + rotationTextField.getText());
                            }
                            break;
                        case DECIMAL:
                        case BACK_SPACE:
                        case LEFT:
                        case RIGHT:
                            break;
                        default:
                            t.consume();
                            break;
                    }
                }
            }
        );

        rotationTextField.focusedProperty().addListener(
            new ChangeListener<Boolean>()
            {
                @Override
                public void changed(
                    ObservableValue<? extends Boolean> observable,
                    Boolean oldValue, Boolean newValue)
                {
                    if (newValue == false)
                    {
                        try
                        {
                            displayManager.getCurrentlyVisibleViewManager().rotateSelection(
                                rotationTextField.getAsDouble());
                        } catch (ParseException ex)
                        {
                            steno.warning("Error converting rotation "
                                + rotationTextField.getText());
                        }
                    }
                    }
            });

        widthTextField.setOnKeyPressed(
            new EventHandler<KeyEvent>()
            {

                @Override
                public void handle(KeyEvent t
                )
                {
                    switch (t.getCode())
                    {
                        case ENTER:
                        case TAB:
                            try
                            {
                                displayManager.getCurrentlyVisibleViewManager().resizeSelectionWidth(
                                    widthTextField.getAsDouble());
                            } catch (ParseException ex)
                            {
                                steno.warning("Error converting width "
                                    + widthTextField.getText());
                            }
                            break;
                        case DECIMAL:
                        case BACK_SPACE:
                        case LEFT:
                        case RIGHT:
                            break;
                        default:
                            t.consume();
                            break;
                    }
                }
            }
        );

        widthTextField.focusedProperty().addListener(
            new ChangeListener<Boolean>()
            {
                @Override
                public void changed(
                    ObservableValue<? extends Boolean> observable,
                    Boolean oldValue, Boolean newValue)
                {
                    if (newValue == false)
                    {
                        try
                        {
                            displayManager.getCurrentlyVisibleViewManager().resizeSelectionWidth(
                                widthTextField.getAsDouble());
                        } catch (ParseException ex)
                        {
                            steno.warning("Error converting width "
                                + widthTextField.getText());
                        }
                    }
                    }
            });

        heightTextField.setOnKeyPressed(
            new EventHandler<KeyEvent>()
            {

                @Override
                public void handle(KeyEvent t
                )
                {
                    switch (t.getCode())
                    {
                        case ENTER:
                        case TAB:
                            try
                            {
                                displayManager.getCurrentlyVisibleViewManager().resizeSelectionHeight(
                                    heightTextField.getAsDouble());
                            } catch (ParseException ex)
                            {
                                steno.warning("Error converting height "
                                    + heightTextField.getText());
                            }
                            break;
                        case DECIMAL:
                        case BACK_SPACE:
                        case LEFT:
                        case RIGHT:
                            break;
                        default:
                            t.consume();
                            break;
                    }
                }
            }
        );

        heightTextField.focusedProperty().addListener(
            new ChangeListener<Boolean>()
            {
                @Override
                public void changed(
                    ObservableValue<? extends Boolean> observable,
                    Boolean oldValue, Boolean newValue)
                {
                    if (newValue == false)
                    {
                        try
                        {
                            displayManager.getCurrentlyVisibleViewManager().resizeSelectionHeight(
                                heightTextField.getAsDouble());
                        } catch (ParseException ex)
                        {
                            steno.warning("Error converting height "
                                + heightTextField.getText());
                        }
                    }
                    }
            });

        depthTextField.setOnKeyPressed(
            new EventHandler<KeyEvent>()
            {
                @Override
                public void handle(KeyEvent t
                )
                {
                    switch (t.getCode())
                    {
                        case ENTER:
                        case TAB:
                            try
                            {
                                displayManager.getCurrentlyVisibleViewManager().resizeSelectionDepth(
                                    depthTextField.getAsDouble());
                            } catch (ParseException ex)
                            {
                                steno.error("Error parsing depth string " + ex
                                    + " : " + ex.getMessage());
                            }
                            break;
                        case DECIMAL:
                        case BACK_SPACE:
                        case LEFT:
                        case RIGHT:
                            break;
                        default:
                            t.consume();
                            break;
                    }
                }
            }
        );

        depthTextField.focusedProperty().addListener(
            new ChangeListener<Boolean>()
            {
                @Override
                public void changed(
                    ObservableValue<? extends Boolean> observable,
                    Boolean oldValue, Boolean newValue)
                {
                    if (newValue == false)
                    {
                        try
                        {
                            displayManager.getCurrentlyVisibleViewManager().resizeSelectionDepth(
                                depthTextField.getAsDouble());
                        } catch (ParseException ex)
                        {
                            steno.error("Error parsing depth string " + ex
                                + " : " + ex.getMessage());
                        }
                    }
                    }
            });

        xAxisTextField.setOnKeyPressed(
            (KeyEvent t) ->
            {
                switch (t.getCode())
                {
                    case ENTER:
                    case TAB:
                        try
                        {
                            displayManager.getCurrentlyVisibleViewManager().translateSelectionXTo(
                                xAxisTextField.getAsDouble());
                        } catch (ParseException ex)
                        {
                            steno.error("Error parsing x translate string " + ex
                                + " : " + ex.getMessage());
                        }
                        break;
                    case DECIMAL:
                    case BACK_SPACE:
                    case LEFT:
                    case RIGHT:
                        break;
                    default:
                        t.consume();
                        break;
                }
            });

        xAxisTextField.focusedProperty().addListener(
            new ChangeListener<Boolean>()
            {
                @Override
                public void changed(
                    ObservableValue<? extends Boolean> observable,
                    Boolean oldValue, Boolean newValue)
                {
                    if (newValue == false)
                    {
                        ThreeDViewManager viewManager = displayManager.getCurrentlyVisibleViewManager();
                        if (viewManager != null)
                        {
                            try
                            {
                                displayManager.getCurrentlyVisibleViewManager().translateSelectionXTo(
                                    xAxisTextField.getAsDouble());
                            } catch (ParseException ex)
                            {
                                steno.error("Error parsing x translate string "
                                    + ex + " : " + ex.getMessage());
                            }
                        }

                    }
                    }
            });

        yAxisTextField.setOnKeyPressed(
            new EventHandler<KeyEvent>()
            {

                @Override
                public void handle(KeyEvent t
                )
                {
                    switch (t.getCode())
                    {
                        case ENTER:
                        case TAB:
                            try
                            {
                                displayManager.getCurrentlyVisibleViewManager().translateSelectionZTo(
                                    yAxisTextField.getAsDouble());
                            } catch (ParseException ex)
                            {
                                steno.error("Error parsing y translate string "
                                    + ex + " : " + ex.getMessage());
                            }
                            break;
                        case DECIMAL:
                        case BACK_SPACE:
                        case LEFT:
                        case RIGHT:
                            break;
                        default:
                            t.consume();
                            break;
                    }
                }
            }
        );

        yAxisTextField.focusedProperty().addListener(
            new ChangeListener<Boolean>()
            {
                @Override
                public void changed(
                    ObservableValue<? extends Boolean> observable,
                    Boolean oldValue, Boolean newValue)
                {
                    if (newValue == false)
                    {
                        ThreeDViewManager viewManager = displayManager.getCurrentlyVisibleViewManager();
                        if (viewManager != null)
                        {
                            try
                            {
                                displayManager.getCurrentlyVisibleViewManager().translateSelectionZTo(
                                    yAxisTextField.getAsDouble());
                            } catch (ParseException ex)
                            {
                                steno.error("Error parsing y translate string "
                                    + ex + " : " + ex.getMessage());
                            }
                        }
                    }
                    }
            });

        modelChangeListener = new ListChangeListener<ModelContainer>()
        {

            @Override
            public void onChanged(
                ListChangeListener.Change<? extends ModelContainer> change)
            {
                while (change.next())
                {
                    if (change.wasAdded())
                    {
                        for (ModelContainer additem : change.getAddedSubList())
                        {
                            boundModel = additem;
                            boundModel = boundProject.getLoadedModels().get(0);
                        }
                    } else if (change.wasRemoved())
                    {
                        for (ModelContainer additem : change.getRemoved())
                        {
                        }
                    } else if (change.wasReplaced())
                    {
                        steno.info("Replaced: ");
                    } else if (change.wasUpdated())
                    {
                        steno.info("Updated: ");
                    }
                }
            }
        };

        widthListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t,
                Number t1)
            {
                widthTextField.doubleValueProperty().set(t1.doubleValue());
            }
        };

        heightListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t,
                Number t1)
            {
                heightTextField.doubleValueProperty().set(t1.doubleValue());
            }
        };

        depthListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t,
                Number t1)
            {
                depthTextField.doubleValueProperty().set(t1.doubleValue());
            }
        };

        xAxisListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t,
                Number t1)
            {
                xAxisTextField.doubleValueProperty().set(t1.doubleValue());
            }
        };

        yAxisListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t,
                Number t1)
            {
                yAxisTextField.doubleValueProperty().set(t1.doubleValue());
            }
        };
    }

    /**
     *
     * @param viewManager
     */
    public void bindLoadedModels(final ThreeDViewManager viewManager)
    {
        ObservableList<ModelContainer> loadedModels = viewManager.getLoadedModels();

//        if (selectionModel != null)
//        {
//            widthTextField.doubleValueProperty().unbind();
////            selectionModel.selectedModelsProperty().removeListener(
////                selectionContainerModelsListener);
////            selectionModel.widthProperty().removeListener(widthListener);
////            selectionContainer.heightProperty().removeListener(heightListener);
////            selectionContainer.depthProperty().removeListener(depthListener);
////            selectionContainer.centreXProperty().removeListener(xAxisListener);
////            selectionContainer.centreZProperty().removeListener(yAxisListener);
////            selectionContainer.scaleProperty().removeListener(
////                modelScaleChangeListener);
////            selectionContainer.rotationYProperty().removeListener(
////                modelRotationChangeListener);
//            selectedItemDetails.visibleProperty().unbind();
//        }
//
        selectionModel = viewManager.getSelectedModelContainers();
//
        modelDataTableView.setItems(loadedModels);
//
//        selectionContainer.selectedModelsProperty().addListener(
//            selectionContainerModelsListener);

        SelectedModelContainers.PrimarySelectedModelDetails selectedModelDetails = 
                                                selectionModel.getPrimarySelectedModelDetails();
        if (selectedModelDetails != null)
        {
            widthTextField.doubleValueProperty().bind(
                selectedModelDetails.getWidth());
//            heightTextField.doubleValueProperty().set(
//                selectionContainer.getHeight());
//            depthTextField.doubleValueProperty().set(
//                selectionContainer.getDepth());
//            xAxisTextField.doubleValueProperty().set(
//                selectionContainer.getCentreX());
//            yAxisTextField.doubleValueProperty().set(
//                selectionContainer.getCentreZ());
//            scaleTextField.doubleValueProperty().set(
//                selectionContainer.getScale() * 100);
//            rotationTextField.doubleValueProperty().set(
//                selectionContainer.getRotationY());
        } else
        {
            widthTextField.setText("-");
            heightTextField.setText("-");
            depthTextField.setText("-");
            xAxisTextField.setText("-");
            yAxisTextField.setText("-");
            scaleTextField.setText("-");
            rotationTextField.setText("-");
        }

//        selectedItemDetails.visibleProperty().bind(Bindings.isNotEmpty(
//            selectionContainer.selectedModelsProperty()));
//        selectionContainer.centreXProperty().addListener(xAxisListener);
//        selectionContainer.centreZProperty().addListener(yAxisListener);
//        selectionContainer.widthProperty().addListener(widthListener);
//        selectionContainer.heightProperty().addListener(heightListener);
//        selectionContainer.depthProperty().addListener(depthListener);
//        selectionContainer.scaleProperty().addListener(modelScaleChangeListener);
//        selectionContainer.rotationYProperty().addListener(
//            modelRotationChangeListener);
        if (boundProject != null)
        {
            boundProject.getLoadedModels().removeListener(modelChangeListener);
        }

        if (boundModel != null)
        {
//            boundModel.maxLayerVisibleProperty().unbind();
//            boundModel.minLayerVisibleProperty().unbind();
        }

        boundProject = displayManager.getCurrentlyVisibleProject();

        if (boundProject.getLoadedModels().size() > 0)
        {
            boundModel = boundProject.getLoadedModels().get(0);
        }

        boundProject.getLoadedModels().addListener(modelChangeListener);
    }

    /**
     *
     * @param slideOutController
     */
    @Override
    public void configure(Initializable slideOutController)
    {
    }
}
