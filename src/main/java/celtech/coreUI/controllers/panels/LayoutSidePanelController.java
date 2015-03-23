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
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
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
    private RestrictedNumberField scaleTextWidthField;

    @FXML
    private RestrictedNumberField scaleTextHeightField;

    @FXML
    private RestrictedNumberField scaleTextDepthField;

    @FXML
    private RestrictedNumberField heightTextField;

    @FXML
    private RestrictedNumberField yAxisTextField;

    @FXML
    private RestrictedNumberField rotationXTextField;

    @FXML
    private RestrictedNumberField rotationYTextField;

    @FXML
    private RestrictedNumberField rotationZTextField;

    @FXML
    private ToggleButton preserveAspectRatio;

    @FXML
    private TableView<ModelContainer> modelDataTableView;

    @FXML
    private VBox materialContainer;

    private final TableColumn modelNameColumn = new TableColumn();

    private SelectedModelContainers selectionModel;
    private SelectedModelContainersListener tableViewSelectionListener = null;

    private ChangeListener<Number> modelScaleXChangeListener = null;
    private ChangeListener<Number> modelScaleYChangeListener = null;
    private ChangeListener<Number> modelScaleZChangeListener = null;
    private ChangeListener<Number> modelLeanChangeListener = null;
    private ChangeListener<Number> modelTwistChangeListener = null;
    private ChangeListener<Number> modelTurnChangeListener = null;
    private ChangeListener<Number> widthListener = null;
    private ChangeListener<Number> heightListener = null;
    private ChangeListener<Number> depthListener = null;
    private ChangeListener<Number> xAxisListener = null;
    private ChangeListener<Number> yAxisListener = null;

    private final String scaleFormat = "######.###";
    private final String rotationFormat = "%.0f";

    private ListChangeListener selectionListener = null;
    private boolean suppressModelDataTableViewNotifications = false;
    private IntegerProperty numSelectedModels = new SimpleIntegerProperty(0);
    /**
     * The last scale ratio that was applied to the current selection. This figure is reset to 1.0
     * when the selection changes.
     */
    private double lastScaleRatio = 1.0d;

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

        scaleTextWidthField.setText("100");
        scaleTextHeightField.setText("100");
        scaleTextDepthField.setText("100");
        rotationXTextField.setText("0");
        rotationYTextField.setText("0");
        rotationZTextField.setText("0");
        widthTextField.setText("-");
        depthTextField.setText("-");
        heightTextField.setText("-");
        xAxisTextField.setText("-");
        yAxisTextField.setText("-");

        setUpTableView(modelNameLabelString, languageBundle);

        setUpModelGeometryListeners();
        setUpKeyPressListeners();
        setupMaterialContainer();
        setupProjectSelectedListener();

        numSelectedModels.addListener(
            (ObservableValue< ? extends Number> observable, Number oldValue, Number newValue) ->
            {
                whenNumSelectedModelsChanged();
            });
        
        setFieldsEditable();
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

        modelScaleXChangeListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
        {
            if (!inMultiSelect())
            {
                populateScaleXField(t1);
            }
        };

        modelScaleYChangeListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
        {
            if (!inMultiSelect())
            {
                populateScaleYField(t1);
            }
        };

        modelScaleZChangeListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
        {
            if (!inMultiSelect())
            {
                populateScaleZField(t1);
            }
        };

        modelLeanChangeListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
        {
            populateRotationXField(t1);
        };

        modelTwistChangeListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
        {
            populateRotationYField(t1);
        };

        modelTurnChangeListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
        {
            populateRotationZField(t1);
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

    private void populateRotationXField(Number t1)
    {
        rotationXTextField.doubleValueProperty().set(t1.doubleValue());
        rotationXTextField.setText(String.format(rotationFormat, t1));
    }

    private void populateRotationZField(Number t1)
    {
        rotationZTextField.doubleValueProperty().set(t1.doubleValue());
        rotationZTextField.setText(String.format(rotationFormat, t1));
    }

    private void populateRotationYField(Number t1)
    {
        rotationYTextField.doubleValueProperty().set(t1.doubleValue());
        rotationYTextField.setText(String.format(rotationFormat, t1));
    }

    /**
     * Return if we are in a multi-select and fixed aspect ratio is being applied.
     */
    private boolean inMultiSelectWithFixedAR()
    {
        return preserveAspectRatio.isSelected()
            && (selectionModel.getNumModelsSelectedProperty().get() > 1);
    }

    private boolean inMultiSelect()
    {
        return selectionModel.getNumModelsSelectedProperty().get() > 1;
    }

    /**
     * Return if fixed aspect ratio is being applied.
     */
    private boolean inFixedAR()
    {
        return preserveAspectRatio.isSelected();
    }

    private void showScaleForXYZ(double scaleRatio)
    {
        DecimalFormat myFormatter = new DecimalFormat(scaleFormat);
        String scaleString = myFormatter.format(scaleRatio * 100f);
        scaleTextWidthField.doubleValueProperty().set(scaleRatio * 100);
        scaleTextWidthField.setText(scaleString);
        scaleTextHeightField.doubleValueProperty().set(scaleRatio * 100);
        scaleTextHeightField.setText(scaleString);
        scaleTextDepthField.doubleValueProperty().set(scaleRatio * 100);
        scaleTextDepthField.setText(scaleString);
    }

    private void populateScaleXField(Number t1)
    {
        if (!inMultiSelectWithFixedAR())
        {
            scaleTextWidthField.doubleValueProperty().set(t1.doubleValue() * 100);
            DecimalFormat myFormatter = new DecimalFormat(scaleFormat);
            String scaleString = myFormatter.format(t1.doubleValue() * 100f);
            scaleTextWidthField.setText(scaleString);
        }
    }

    private void populateScaleYField(Number t1)
    {
        if (!inMultiSelectWithFixedAR())
        {
            scaleTextHeightField.doubleValueProperty().set(t1.doubleValue() * 100);
            DecimalFormat myFormatter = new DecimalFormat(scaleFormat);
            String scaleString = myFormatter.format(t1.doubleValue() * 100f);
            scaleTextHeightField.setText(scaleString);
        }
    }

    private void populateScaleZField(Number t1)
    {
        if (!inMultiSelectWithFixedAR())
        {
            scaleTextDepthField.doubleValueProperty().set(t1.doubleValue() * 100);
            DecimalFormat myFormatter = new DecimalFormat(scaleFormat);
            String scaleString = myFormatter.format(t1.doubleValue() * 100f);
            scaleTextDepthField.setText(scaleString);
        }
    }

    private void setUpKeyPressListeners()
    {
        scaleTextWidthField.setOnKeyPressed((KeyEvent t) ->
        {
            switch (t.getCode())
            {
                case ENTER:
                case TAB:
                    try
                    {
                        double scaleFactor = scaleTextWidthField.getAsDouble() / 100.0;
                        if (inMultiSelectWithFixedAR())
                        {
                            double ratio = scaleFactor / lastScaleRatio;
                            lastScaleRatio = scaleFactor;
                            boundProject.scaleXYZRatioSelection(
                                selectionModel.getSelectedModelsSnapshot(),
                                ratio);
                            showScaleForXYZ(lastScaleRatio);
                        } else if (inFixedAR())
                        {
                            boundProject.scaleXModels(selectionModel.getSelectedModelsSnapshot(),
                                                      scaleFactor, true);
                        }
                        {
                            boundProject.scaleXModels(selectionModel.getSelectedModelsSnapshot(),
                                                      scaleFactor, false);
                        }
                    } catch (ParseException ex)
                    {
                        steno.warning("Error converting scale " + scaleTextWidthField.getText());
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

        scaleTextHeightField.setOnKeyPressed((KeyEvent t) ->
        {
            switch (t.getCode())
            {
                case ENTER:
                case TAB:
                    try
                    {
                        double scaleFactor = scaleTextHeightField.getAsDouble() / 100.0;
                        if (inMultiSelectWithFixedAR())
                        {
                            double ratio = scaleFactor / lastScaleRatio;
                            lastScaleRatio = scaleFactor;
                            boundProject.scaleXYZRatioSelection(
                                selectionModel.getSelectedModelsSnapshot(),
                                ratio);
                            showScaleForXYZ(lastScaleRatio);
                        } else if (inFixedAR())
                        {
                            boundProject.scaleYModels(selectionModel.getSelectedModelsSnapshot(),
                                                      scaleFactor, true);
                        }
                        {
                            boundProject.scaleYModels(selectionModel.getSelectedModelsSnapshot(),
                                                      scaleFactor, false);
                        }

                    } catch (ParseException ex)
                    {
                        steno.warning("Error converting scale " + scaleTextHeightField.getText());
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

        scaleTextDepthField.setOnKeyPressed((KeyEvent t) ->
        {
            switch (t.getCode())
            {
                case ENTER:
                case TAB:
                    try
                    {
                        double scaleFactor = scaleTextDepthField.getAsDouble() / 100.0;
                        if (inMultiSelectWithFixedAR())
                        {
                            double ratio = scaleFactor / lastScaleRatio;
                            lastScaleRatio = scaleFactor;
                            boundProject.scaleXYZRatioSelection(
                                selectionModel.getSelectedModelsSnapshot(),
                                ratio);
                            showScaleForXYZ(lastScaleRatio);
                        } else if (inFixedAR())
                        {
                            boundProject.scaleZModels(selectionModel.getSelectedModelsSnapshot(),
                                                      scaleFactor, true);
                        }
                        {
                            boundProject.scaleZModels(selectionModel.getSelectedModelsSnapshot(),
                                                      scaleFactor, false);
                        }
                    } catch (ParseException ex)
                    {
                        steno.warning("Error converting scale " + scaleTextDepthField.getText());
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

        rotationXTextField.setOnKeyPressed((KeyEvent t) ->
        {
            switch (t.getCode())
            {
                case ENTER:
                case TAB:
                    try
                    {
                        boundProject.rotateLeanModels(selectionModel.getSelectedModelsSnapshot(),
                                                      rotationXTextField.getAsDouble());
                    } catch (ParseException ex)
                    {
                        steno.warning("Error converting rotation "
                            + rotationXTextField.getText());
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

        rotationYTextField.setOnKeyPressed((KeyEvent t) ->
        {
            switch (t.getCode())
            {
                case ENTER:
                case TAB:
                    try
                    {
                        boundProject.rotateTwistModels(selectionModel.getSelectedModelsSnapshot(),
                                                       rotationYTextField.getAsDouble());
                    } catch (ParseException ex)
                    {
                        steno.warning("Error converting rotation "
                            + rotationYTextField.getText());
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

        rotationZTextField.setOnKeyPressed((KeyEvent t) ->
        {
            switch (t.getCode())
            {
                case ENTER:
                case TAB:
                    try
                    {
                        boundProject.rotateTurnModels(selectionModel.getSelectedModelsSnapshot(),
                                                      rotationZTextField.getAsDouble());
                    } catch (ParseException ex)
                    {
                        steno.warning("Error converting rotation "
                            + rotationZTextField.getText());
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

        widthTextField.setOnKeyPressed((KeyEvent t) ->
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
        });

        heightTextField.setOnKeyPressed((KeyEvent t) ->
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
        });

        depthTextField.setOnKeyPressed((KeyEvent t) ->
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
                            boundProject.translateModelsXTo(
                                selectionModel.getSelectedModelsSnapshot(),
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

        yAxisTextField.setOnKeyPressed((KeyEvent t) ->
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
        });
    }

    private void setUpTableView(String modelNameLabelString, ResourceBundle languageBundle)
    {
        modelNameColumn.setText(modelNameLabelString);
        modelNameColumn.setCellValueFactory(
            new PropertyValueFactory<>("modelName"));
        modelNameColumn.setMinWidth(250);
        modelNameColumn.setMaxWidth(250);
        modelNameColumn.setEditable(false);

        modelDataTableView.getColumns().addAll(modelNameColumn);
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

    /**
     * This updates size and scale fields to be editable or not according to whether we are in a
     * multi-selection or not.
     */
    private void setFieldsEditable()
    {
        widthTextField.disableProperty().bind(numSelectedModels.greaterThan(1));
        heightTextField.disableProperty().bind(numSelectedModels.greaterThan(1));
        depthTextField.disableProperty().bind(numSelectedModels.greaterThan(1));
        xAxisTextField.disableProperty().bind(numSelectedModels.greaterThan(1));
        yAxisTextField.disableProperty().bind(numSelectedModels.greaterThan(1));
        rotationXTextField.disableProperty().bind(numSelectedModels.greaterThan(1));
        rotationYTextField.disableProperty().bind(numSelectedModels.greaterThan(1));
        rotationZTextField.disableProperty().bind(numSelectedModels.greaterThan(1));
    }

    private void whenNumSelectedModelsChanged()
    {
        lastScaleRatio = 1.0d;
        if (inMultiSelect())
        {
            showScaleForXYZ(1.0d);
        }
    }

    private void unbindProject(Project project)
    {
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
        if (boundProject != null)
        {
            unbindProject(boundProject);
        }
        boundProject = project;

        selectionModel = Lookup.getProjectGUIState(project).getSelectedModelContainers();
        numSelectedModels.bind(selectionModel.getNumModelsSelectedProperty());

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

        selectedModelDetails.getScaleX().addListener(modelScaleXChangeListener);
        selectedModelDetails.getScaleY().addListener(modelScaleYChangeListener);
        selectedModelDetails.getScaleZ().addListener(modelScaleZChangeListener);
        selectedModelDetails.getRotationLean().addListener(modelLeanChangeListener);
        selectedModelDetails.getRotationTwist().addListener(modelTwistChangeListener);
        selectedModelDetails.getRotationTurn().addListener(modelTurnChangeListener);

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
        populateScaleXField(selectedModelDetails.getScaleX().get());
        populateScaleYField(selectedModelDetails.getScaleY().get());
        populateScaleZField(selectedModelDetails.getScaleZ().get());
        populateRotationXField(selectedModelDetails.getRotationLean().get());
        populateRotationYField(selectedModelDetails.getRotationTwist().get());
        populateRotationZField(selectedModelDetails.getRotationTurn().get());
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
        switch (layoutSubmode.get())
        {
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
