package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.components.material.MaterialComponent;
import celtech.coreUI.visualisation.SelectedModelContainers;
import celtech.coreUI.visualisation.SelectedModelContainers.SelectedModelContainersListener;
import celtech.modelcontrol.ModelContainer;
import celtech.utils.threed.exporters.AMFOutputConverter;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
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
public class LayoutSidePanelController implements Initializable, SidePanelManager
{

    private Stenographer steno = StenographerFactory.getStenographer(
        LayoutSidePanelController.class.getName());
    private Project boundProject = null;

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

    @FXML
    private VBox materialContainer;

    private final TableColumn modelNameColumn = new TableColumn();
    private final TableColumn scaleColumn = new TableColumn();
    private final TableColumn rotationColumn = new TableColumn();

    private SelectedModelContainers selectionModel;
    private SelectedModelContainersListener tableViewSelectionListener = null;

    private ChangeListener<Number> modelScaleChangeListener = null;
    private ChangeListener<Number> modelRotationChangeListener = null;
    private ChangeListener<Number> widthListener = null;
    private ChangeListener<Number> heightListener = null;
    private ChangeListener<Number> depthListener = null;
    private ChangeListener<Number> xAxisListener = null;
    private ChangeListener<Number> yAxisListener = null;

    private final String scaleFormat = "######.###";
    private final String rotationFormat = "%.0f";

    private ListChangeListener selectionListener = null;
    private boolean suppressModelDataTableViewNotifications = false;

    private MaterialComponent materialComponent0;
    private MaterialComponent materialComponent1;

    private MaterialComponent selectedMaterialComponent;
    private ObjectProperty<LayoutSubmode> layoutSubmode;

    private AMFOutputConverter outputConverter = new AMFOutputConverter();

    @FXML
    void outputAMF(ActionEvent event)
    {
        outputConverter.outputFile(boundProject, "Blargle");
    }

    @FXML
    void changeToSettings(MouseEvent event)
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.SETTINGS);
    }

    /**
     * Initialises the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        ResourceBundle languageBundle = Lookup.getLanguageBundle();
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

        setUpTableView(modelNameLabelString, scaleLabelString, rotationLabelString, languageBundle);
        
        setUpModelGeometryListeners();
        setUpKeyPressListeners();
        setupMaterialContainer();
        setupProjectSelectedListener();
    }

    private void setupProjectSelectedListener()
    {
        Lookup.getSelectedProjectProperty().addListener(
            (ObservableValue<? extends Project> observable, Project oldValue, Project newValue) ->
        {
            bindProject(newValue);
        });
    }

    private void setUpModelGeometryListeners()
    {

        modelScaleChangeListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
        {
            populateScaleField(t1);
        };

        modelRotationChangeListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
        {
            populateRotationField(t1);
        };

        widthListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
        {
            populateWidthField(t1);
        };

        heightListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
        {
            populateHeightField(t1);
        };

        depthListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
        {
            populateDepthField(t1);
        };

        xAxisListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
        {
            populateXAxisField(t1);
        };

        yAxisListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
        {
            populateYAxisField(t1);
        };
    }

    private void populateYAxisField(Number t1)
    {
        yAxisTextField.doubleValueProperty().set(t1.doubleValue());
    }

    private void populateXAxisField(Number t1)
    {
        xAxisTextField.doubleValueProperty().set(t1.doubleValue());
    }

    private void populateDepthField(Number t1)
    {
        depthTextField.doubleValueProperty().set(t1.doubleValue());
    }

    private void populateHeightField(Number t1)
    {
        heightTextField.doubleValueProperty().set(t1.doubleValue());
    }

    private void populateWidthField(Number t1)
    {
        widthTextField.doubleValueProperty().set(t1.doubleValue());
    }

    private void populateRotationField(Number t1)
    {
        rotationTextField.doubleValueProperty().set(t1.doubleValue());
        rotationTextField.setText(String.format(rotationFormat, t1));
    }

    private void populateScaleField(Number t1)
    {
        scaleTextField.doubleValueProperty().set(t1.doubleValue() * 100);
        DecimalFormat myFormatter = new DecimalFormat(scaleFormat);
        String scaleString = myFormatter.format(t1.doubleValue() * 100f);
        scaleTextField.setText(scaleString);
    }

    private void setUpKeyPressListeners()
    {
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
                                boundProject.scaleModels(selectionModel.getSelectedModelsSnapshot(),
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
                                boundProject.rotateModels(selectionModel.getSelectedModelsSnapshot(),
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
                                boundProject.resizeModelsWidth(selectionModel.getSelectedModelsSnapshot(),
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
                                boundProject.resizeModelsHeight(selectionModel.getSelectedModelsSnapshot(),
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
                                boundProject.resizeModelsDepth(selectionModel.getSelectedModelsSnapshot(),
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

        xAxisTextField.setOnKeyPressed(
            (KeyEvent t) ->
            {
                switch (t.getCode())
                {
                    case ENTER:
                    case TAB:
                        try
                        {
                            boundProject.translateModelsXTo(selectionModel.getSelectedModelsSnapshot(),
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
                                boundProject.translateModelsZTo(selectionModel.getSelectedModelsSnapshot(),
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
    }

    private void setUpTableView(String modelNameLabelString, String scaleLabelString,
        String rotationLabelString, ResourceBundle languageBundle)
    {
        modelNameColumn.setText(modelNameLabelString);
        modelNameColumn.setCellValueFactory(
            new PropertyValueFactory<>("modelName"));
        modelNameColumn.setMinWidth(170);
        modelNameColumn.setMaxWidth(170);
        modelNameColumn.setEditable(false);

        scaleColumn.setText(scaleLabelString);
        scaleColumn.setCellValueFactory(
            new PropertyValueFactory<>("preferredScale"));
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
            new PropertyValueFactory<>("preferredRotationY"));
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

        modelDataTableView.getColumns().addAll(modelNameColumn, scaleColumn, rotationColumn);
        modelDataTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        modelDataTableView.setEditable(true);
        modelDataTableView.getSortOrder().add(modelNameColumn);

        Label noModelsLoadedPlaceholder = new Label();
        noModelsLoadedPlaceholder.setText(languageBundle.getString(
            "sidePanel_layout.noModelsLoaded"));
        modelDataTableView.setPlaceholder(noModelsLoadedPlaceholder);

        setUpTableViewListeners();
    }

    private void setUpTableViewListeners()
    {
        selectionListener = new ListChangeListener<ModelContainer>()
        {
            @Override
            public void onChanged(
                ListChangeListener.Change<? extends ModelContainer> change)
            {
                if (suppressModelDataTableViewNotifications)
                {
                    return;
                }
                suppressModelDataTableViewNotifications = true;
                selectionModel.deselectAllModels();
                for (ModelContainer modelContainer : modelDataTableView.getSelectionModel().
                    getSelectedItems())
                {
                    selectionModel.addModelContainer(modelContainer);
                }
                suppressModelDataTableViewNotifications = false;
            }
        };

        modelDataTableView.getSelectionModel().getSelectedItems().addListener(
            selectionListener);

        tableViewSelectionListener = new SelectedModelContainersListener()
        {

            @Override
            public void whenAdded(ModelContainer modelContainer)
            {
                if (suppressModelDataTableViewNotifications)
                {
                    return;
                }
                suppressModelDataTableViewNotifications = true;
                modelDataTableView.getSelectionModel().select(modelContainer);
                suppressModelDataTableViewNotifications = false;
            }

            @Override
            public void whenRemoved(ModelContainer modelContainer)
            {
                if (suppressModelDataTableViewNotifications)
                {
                    return;
                }
                suppressModelDataTableViewNotifications = true;
                int modelIndex = modelDataTableView.getItems().indexOf(modelContainer);
                if (modelIndex != -1)
                {
                    if (modelDataTableView.getSelectionModel().isSelected(modelIndex))
                    {
                        modelDataTableView.getSelectionModel().clearSelection(modelIndex);
                    }
                }
                suppressModelDataTableViewNotifications = false;
            }

        };
    }
    
    private void unbindProject(Project project) {
        layoutSubmode.removeListener(layoutSubmodeListener);
        selectionModel.removeListener(tableViewSelectionListener);
    }

    /**
     * Bind the given viewManager to the controller's widgets. Unbind any widget tied to a previous
     * viewManager.
     *
     * @param viewManager
     */
    private void bindProject(final Project project)
    {
        if (boundProject != null) {
            unbindProject(boundProject);
        }
        boundProject = project;

        selectionModel = Lookup.getProjectGUIState(project).getSelectedModelContainers();
        layoutSubmode = Lookup.getProjectGUIState(project).getLayoutSubmodeProperty();

        modelDataTableView.setItems(project.getLoadedModels());
        resetTableViewSelection(selectionModel);
        selectionModel.addListener(tableViewSelectionListener);
        layoutSubmode.addListener(layoutSubmodeListener);

        SelectedModelContainers.PrimarySelectedModelDetails selectedModelDetails
            = selectionModel.getPrimarySelectedModelDetails();
        selectedModelDetails.getWidth().addListener(widthListener);
        selectedModelDetails.getHeight().addListener(heightListener);
        selectedModelDetails.getDepth().addListener(depthListener);

        selectedModelDetails.getCentreX().addListener(xAxisListener);
        selectedModelDetails.getCentreZ().addListener(yAxisListener);

        selectedModelDetails.getScale().addListener(modelScaleChangeListener);
        selectedModelDetails.getRotationY().addListener(modelRotationChangeListener);

        repopulate(selectedModelDetails);

        selectedItemDetails.visibleProperty().bind(
            Bindings.lessThan(0, selectionModel.getNumModelsSelectedProperty()));

        materialComponent0.setSelectedFilamentInComboBox(
            boundProject.getExtruder0FilamentProperty().get());
        materialComponent1.setSelectedFilamentInComboBox(
            boundProject.getExtruder1FilamentProperty().get());
    }

    /**
     *
     * @param slideOutController
     */
    @Override
    public void configure(Initializable slideOutController)
    {
    }

    /**
     * Reset the table view selection to the current selection in the viewManager, used when
     * switching ProjectTabs.
     */
    private void resetTableViewSelection(SelectedModelContainers selectionModel)
    {
        suppressModelDataTableViewNotifications = true;
        modelDataTableView.getSelectionModel().clearSelection();
        for (ModelContainer modelContainer : selectionModel.getSelectedModelsSnapshot())
        {
            modelDataTableView.getSelectionModel().select(modelContainer);
        }
        suppressModelDataTableViewNotifications = false;
    }

    /**
     * Repopulate the widgets for the given model details.
     *
     * @param selectedModelDetails
     */
    private void repopulate(SelectedModelContainers.PrimarySelectedModelDetails selectedModelDetails)
    {
        populateScaleField(selectedModelDetails.getScale().get());
        populateRotationField(selectedModelDetails.getRotationY().get());
        populateWidthField(selectedModelDetails.getWidth().get());
        populateHeightField(selectedModelDetails.getHeight().get());
        populateDepthField(selectedModelDetails.getDepth().get());
        populateXAxisField(selectedModelDetails.getCentreX().get());
        populateYAxisField(selectedModelDetails.getCentreZ().get());
    }

    private void setupMaterialContainer()
    {
        materialComponent0 = new MaterialComponent(MaterialComponent.Mode.LAYOUT, null, 0);
        materialComponent1 = new MaterialComponent(MaterialComponent.Mode.LAYOUT, null, 1);
        materialContainer.getChildren().addAll(materialComponent0, materialComponent1);

        materialComponent0.setOnMouseClicked((MouseEvent event) ->
        {
            selectMaterialComponent(materialComponent0);
        });

        materialComponent1.setOnMouseClicked((MouseEvent event) ->
        {
            selectMaterialComponent(materialComponent1);
        });

        materialComponent0.getSelectedFilamentProperty().addListener(
            (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
            {
                boundProject.setExtruder0Filament(newValue);
            });

        materialComponent1.getSelectedFilamentProperty().addListener(
            (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
            {
                boundProject.setExtruder1Filament(newValue);
            });
    }

    private void selectMaterialComponent(MaterialComponent materialComponent)
    {
        materialComponent.select(true);
        selectedMaterialComponent = materialComponent;
        if (materialComponent == materialComponent0)
        {
            layoutSubmode.set(LayoutSubmode.ASSOCIATE_WITH_EXTRUDER0);
            materialComponent1.select(false);
        } else
        {
            layoutSubmode.set(LayoutSubmode.ASSOCIATE_WITH_EXTRUDER1);
            materialComponent0.select(false);
        }
    }

    private final ChangeListener<LayoutSubmode> layoutSubmodeListener = (ObservableValue<? extends LayoutSubmode> observable, LayoutSubmode oldValue, LayoutSubmode newValue) ->
    {
        switch (layoutSubmode.get()) {
            case ASSOCIATE_WITH_EXTRUDER0:
                selectMaterialComponent(materialComponent0);
                break;
            case ASSOCIATE_WITH_EXTRUDER1:
                selectMaterialComponent(materialComponent1);
                break;           
            default:
                materialComponent0.select(false);
                materialComponent1.select(false);
        }
       
    };


}
