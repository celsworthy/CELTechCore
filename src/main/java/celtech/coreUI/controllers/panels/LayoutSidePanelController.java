package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.undo.UndoableProject;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.components.material.MaterialComponent;
import celtech.coreUI.visualisation.SelectedModelContainers;
import celtech.coreUI.visualisation.SelectedModelContainers.SelectedModelContainersListener;
import celtech.modelcontrol.ModelContainer;
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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import static javafx.scene.input.KeyCode.ENTER;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
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

    public interface NoArgsVoidFunc
    {

        void run() throws Exception;
    }

    private Stenographer steno = StenographerFactory.getStenographer(
        LayoutSidePanelController.class.getName());
    private Project boundProject;
    private UndoableProject undoableProject;

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

    @FXML
    private BorderPane layoutBorder;

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

    private double lastScaleWidth;
    private double lastScaleHeight;
    private double lastScaleDepth;
    private double lastRotationX;
    private double lastRotationY;
    private double lastRotationZ;
    private double lastWidth;
    private double lastHeight;
    private double lastDepth;
    private double lastX;
    private double lastY;

    private ImageView linkedImage;
    private ImageView unlinkedImage;

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

        initialiseTextFieldValues();

        setUpTableView(modelNameLabelString, languageBundle);

        setUpModelGeometryListeners();
        setUpNumberFieldListeners();
        setupMaterialContainer();
        setupProjectSelectedListener();
        setFieldsEditable();
        setUpNumSelectedModelsListener();
        setUpAspectRatioListener(rb);

        Image image = new Image(getClass().getResourceAsStream(
            ApplicationConfiguration.imageResourcePath + "link.png"));
        linkedImage = new ImageView(image);
        image = new Image(getClass().getResourceAsStream(
            ApplicationConfiguration.imageResourcePath + "unlink.png"));
        unlinkedImage = new ImageView(image);

        FXMLUtilities.addColonsToLabels(layoutBorder);
    }

    /**
     * Change the preserve aspect ratio icon to linked / unlinked according to whether it is
     * selected or not.
     */
    private void setUpAspectRatioListener(ResourceBundle rb)
    {
        preserveAspectRatio.setSelected(true);
        preserveAspectRatio.selectedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                if (newValue)
                {
                    preserveAspectRatio.setGraphic(linkedImage);
                } else
                {
                    preserveAspectRatio.setGraphic(unlinkedImage);
                }
            });
    }

    private void setUpNumSelectedModelsListener()
    {
        numSelectedModels.addListener(
            (ObservableValue< ? extends Number> observable, Number oldValue, Number newValue) ->
            {
                whenNumSelectedModelsChanged();
            });
    }

    private void initialiseTextFieldValues()
    {
        scaleTextWidthField.setText("100");
        lastScaleWidth = 100;
        scaleTextHeightField.setText("100");
        lastScaleHeight = 100;
        scaleTextDepthField.setText("100");
        lastScaleDepth = 100;
        rotationXTextField.setText("0");
        rotationYTextField.setText("0");
        rotationZTextField.setText("0");
        widthTextField.setText("-");
        depthTextField.setText("-");
        heightTextField.setText("-");
        xAxisTextField.setText("-");
        yAxisTextField.setText("-");
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
        lastY = t1.doubleValue();
    }

    private void populateXAxisField(Number t1)
    {
        xAxisTextField.doubleValueProperty().set(t1.doubleValue());
        lastX = t1.doubleValue();
    }

    private void populateDepthField(Number t1)
    {
        depthTextField.doubleValueProperty().set(t1.doubleValue());
        lastDepth = t1.doubleValue();
    }

    private void populateHeightField(Number t1)
    {
        heightTextField.doubleValueProperty().set(t1.doubleValue());
        lastHeight = t1.doubleValue();
    }

    private void populateWidthField(Number t1)
    {
        widthTextField.doubleValueProperty().set(t1.doubleValue());
        lastWidth = t1.doubleValue();
    }

    private void populateRotationXField(Number t1)
    {
        rotationXTextField.doubleValueProperty().set(t1.doubleValue());
        rotationXTextField.setText(String.format(rotationFormat, t1));
        lastRotationX = t1.doubleValue();
    }

    private void populateRotationZField(Number t1)
    {
        rotationZTextField.doubleValueProperty().set(t1.doubleValue());
        rotationZTextField.setText(String.format(rotationFormat, t1));
        lastRotationZ = t1.doubleValue();
    }

    private void populateRotationYField(Number t1)
    {
        rotationYTextField.doubleValueProperty().set(t1.doubleValue());
        rotationYTextField.setText(String.format(rotationFormat, t1));
        lastRotationY = t1.doubleValue();
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

        lastScaleWidth = 100.0;
        lastScaleHeight = 100.0;
        lastScaleDepth = 100.0;
    }

    private void populateScaleXField(Number t1)
    {
        if (!inMultiSelectWithFixedAR())
        {
            scaleTextWidthField.doubleValueProperty().set(t1.doubleValue() * 100);
            DecimalFormat myFormatter = new DecimalFormat(scaleFormat);
            String scaleString = myFormatter.format(t1.doubleValue() * 100f);
            scaleTextWidthField.setText(scaleString);
            lastScaleWidth = t1.doubleValue() * 100;
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
            lastScaleHeight = t1.doubleValue() * 100;
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
            lastScaleDepth = t1.doubleValue() * 100;
        }
    }

    private void setUpNumberFieldListeners()
    {
        addNumberFieldListener(scaleTextWidthField, this::updateScaleWidth);
        addNumberFieldListener(scaleTextHeightField, this::updateScaleHeight);
        addNumberFieldListener(scaleTextDepthField, this::updateScaleDepth);
        addNumberFieldListener(rotationXTextField, this::updateRotationX);
        addNumberFieldListener(rotationYTextField, this::updateRotationY);
        addNumberFieldListener(rotationZTextField, this::updateRotationZ);
        addNumberFieldListener(widthTextField, this::updateWidth);
        addNumberFieldListener(heightTextField, this::updateHeight);
        addNumberFieldListener(depthTextField, this::updateDepth);
        addNumberFieldListener(xAxisTextField, this::updateX);
        addNumberFieldListener(yAxisTextField, this::updateZ);
    }

    private void updateRotationX()
    {
        try
        {
            double newRotationX = rotationXTextField.getAsDouble();
            if (newRotationX == lastRotationX)
            {
                return;
            } else
            {
                lastRotationX = newRotationX;
            }
            undoableProject.rotateLeanModels(selectionModel.getSelectedModelsSnapshot(),
                                             rotationXTextField.getAsDouble());
        } catch (ParseException ex)
        {
            steno.warning("Error converting rotation "
                + rotationXTextField.getText());
        }
    }

    private void updateRotationY()
    {
        try
        {
            double newRotationY = rotationYTextField.getAsDouble();
            if (newRotationY == lastRotationY)
            {
                return;
            } else
            {
                lastRotationY = newRotationY;
            }
            undoableProject.rotateTwistModels(selectionModel.getSelectedModelsSnapshot(),
                                              rotationYTextField.getAsDouble());
        } catch (ParseException ex)
        {
            steno.warning("Error converting rotation "
                + rotationYTextField.getText());
        }
    }

    private void updateRotationZ()
    {
        try
        {
            double newRotationZ = rotationZTextField.getAsDouble();
            if (newRotationZ == lastRotationZ)
            {
                return;
            } else
            {
                lastRotationZ = newRotationZ;
            }
            undoableProject.rotateTurnModels(selectionModel.getSelectedModelsSnapshot(),
                                             rotationZTextField.getAsDouble());
        } catch (ParseException ex)
        {
            steno.warning("Error converting rotation "
                + rotationZTextField.getText());
        }
    }

    /**
     * When focus is lost or ENTER is pressed, run the given function.
     */
    private void addNumberFieldListener(RestrictedNumberField textField, NoArgsVoidFunc func)
    {
        textField.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                try
                {
                    if (!newValue)
                    {
                        func.run();
                    }
                } catch (Exception ex)
                {
                    steno.debug("exception updating number field " + ex);
                }
            });

        textField.setOnKeyPressed((KeyEvent t) ->
        {
            switch (t.getCode())
            {
                case ENTER:
                    try
                    {
                        func.run();
                    } catch (Exception ex)
                    {
                        steno.debug("exception updating number field " + ex);
                    }
                    break;
            }
        });
    }

    private void updateZ()
    {
        try
        {
            double newY = yAxisTextField.getAsDouble();
            if (newY == lastY)
            {
                return;
            } else
            {
                lastY = newY;
            }
            undoableProject.translateModelsZTo(selectionModel.getSelectedModelsSnapshot(), newY);
        } catch (ParseException ex)
        {
            steno.error("Error parsing y translate string " + ex + " : " + ex.getMessage());
        }
    }

    private void updateX()
    {
        try
        {
            double newX = xAxisTextField.getAsDouble();
            if (newX == lastX)
            {
                return;
            } else
            {
                lastX = newX;
            }
            undoableProject.translateModelsXTo(selectionModel.getSelectedModelsSnapshot(), newX);

        } catch (ParseException ex)
        {
            steno.error("Error parsing x translate string " + ex + " : " + ex.getMessage());
        }
    }

    private void updateDepth()
    {
        try
        {
            double newDepth = depthTextField.getAsDouble();
            if (newDepth == lastDepth)
            {
                return;
            } else
            {
                lastDepth = newDepth;
            }
            if (inFixedAR())
            {
                ModelContainer modelContainer = getSingleSelection();
                double ratio = depthTextField.getAsDouble() / modelContainer.getScaledDepth();

                undoableProject.scaleXYZRatioSelection(
                    selectionModel.getSelectedModelsSnapshot(), ratio);

            } else
            {
                undoableProject.resizeModelsDepth(selectionModel.getSelectedModelsSnapshot(),
                                                  depthTextField.getAsDouble());
            }
        } catch (ParseException ex)
        {
            steno.warning("Error converting height " + heightTextField.getText());
        }
    }

    private void updateHeight()
    {
        try
        {
            double newHeight = heightTextField.getAsDouble();
            if (newHeight == lastHeight)
            {
                return;
            } else
            {
                lastHeight = newHeight;
            }
            if (inFixedAR())
            {
                ModelContainer modelContainer = getSingleSelection();
                double ratio = heightTextField.getAsDouble() / modelContainer.getScaledHeight();
                undoableProject.scaleXYZRatioSelection(
                    selectionModel.getSelectedModelsSnapshot(), ratio);
            } else
            {
                undoableProject.resizeModelsHeight(selectionModel.getSelectedModelsSnapshot(),
                                                   heightTextField.getAsDouble());
            }
        } catch (ParseException ex)
        {
            steno.warning("Error converting height " + heightTextField.getText());
        }
    }

    private void updateWidth()
    {
        try
        {
            double newWidth = widthTextField.getAsDouble();
            if (newWidth == lastWidth)
            {
                return;
            } else
            {
                lastWidth = newWidth;
            }
            if (inFixedAR())
            {
                ModelContainer modelContainer = getSingleSelection();
                double ratio = widthTextField.getAsDouble() / modelContainer.getScaledWidth();
                undoableProject.scaleXYZRatioSelection(
                    selectionModel.getSelectedModelsSnapshot(), ratio);
            } else
            {
                undoableProject.resizeModelsWidth(selectionModel.getSelectedModelsSnapshot(),
                                                  widthTextField.getAsDouble());
            }
        } catch (ParseException ex)
        {
            steno.warning("Error converting width " + widthTextField.getText());
        }
    }

    private ModelContainer getSingleSelection()
    {
        assert (selectionModel.getNumModelsSelectedProperty().get() == 1);
        ModelContainer modelContainer = selectionModel.getSelectedModelsSnapshot().iterator().next();
        return modelContainer;
    }

    private void updateScaleDepth()
    {
        try
        {
            double newScaleDepth = scaleTextDepthField.getAsDouble();
            if (newScaleDepth == lastScaleDepth)
            {
                return;
            } else
            {
                lastScaleDepth = newScaleDepth;
            }
            double scaleFactor = scaleTextDepthField.getAsDouble() / 100.0;
            if (inMultiSelectWithFixedAR())
            {
                double ratio = scaleFactor / lastScaleRatio;
                lastScaleRatio = scaleFactor;
                undoableProject.scaleXYZRatioSelection(
                    selectionModel.getSelectedModelsSnapshot(),
                    ratio);
                showScaleForXYZ(lastScaleRatio);
            } else
            {
                undoableProject.scaleZModels(selectionModel.getSelectedModelsSnapshot(),
                                             scaleFactor, inFixedAR());
            }
        } catch (ParseException ex)
        {
            steno.warning("Error converting scale " + scaleTextDepthField.getText());
        }
    }

    private void updateScaleHeight()
    {
        try
        {
            double newScaleHeight = scaleTextHeightField.getAsDouble();
            if (newScaleHeight == lastScaleHeight)
            {
                return;
            } else
            {
                lastScaleHeight = newScaleHeight;
            }
            double scaleFactor = scaleTextHeightField.getAsDouble() / 100.0;
            if (inMultiSelectWithFixedAR())
            {
                double ratio = scaleFactor / lastScaleRatio;
                lastScaleRatio = scaleFactor;
                undoableProject.scaleXYZRatioSelection(
                    selectionModel.getSelectedModelsSnapshot(),
                    ratio);
                showScaleForXYZ(lastScaleRatio);
            } else
            {
                undoableProject.scaleYModels(selectionModel.getSelectedModelsSnapshot(),
                                             scaleFactor, inFixedAR());
            }
        } catch (ParseException ex)
        {
            steno.warning("Error converting scale " + scaleTextHeightField.getText());
        }
    }

    private void updateScaleWidth()
    {
        try
        {
            double newScaleWidth = scaleTextWidthField.getAsDouble();
            if (newScaleWidth == lastScaleWidth)
            {
                return;
            } else
            {
                lastScaleWidth = newScaleWidth;
            }
            double scaleFactor = scaleTextWidthField.getAsDouble() / 100.0;
            if (inMultiSelectWithFixedAR())
            {
                double ratio = scaleFactor / lastScaleRatio;
                lastScaleRatio = scaleFactor;
                undoableProject.scaleXYZRatioSelection(
                    selectionModel.getSelectedModelsSnapshot(),
                    ratio);
                showScaleForXYZ(lastScaleRatio);
            } else
            {
                undoableProject.scaleXModels(selectionModel.getSelectedModelsSnapshot(),
                                             scaleFactor, inFixedAR());
            }
        } catch (ParseException ex)
        {
            steno.warning("Error converting scale " + scaleTextWidthField.getText());
        }
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
        } else if (selectionModel.getNumModelsSelectedProperty().get() == 1)
        {
            ModelContainer modelContainer = getSingleSelection();
            populateScaleXField(modelContainer.getXScale());
            populateScaleYField(modelContainer.getYScale());
            populateScaleZField(modelContainer.getZScale());
        }
        getInitialValuesOfNumberFields();
    }

    private void getInitialValuesOfNumberFields()
    {

    }

    private void unbindProject(Project project)
    {
        layoutSubmode.removeListener(layoutSubmodeListener);
        selectionModel.removeListener(tableViewSelectionListener);
        numSelectedModels.unbind();
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
        undoableProject = new UndoableProject(project);

        materialComponent0.setLayoutProject(project);
        materialComponent1.setLayoutProject(project);

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
    }

    private void selectMaterialComponent(MaterialComponent materialComponent)
    {
        materialComponent.select(true);
        selectedMaterialComponent = materialComponent;
        if (materialComponent == materialComponent0)
        {
            layoutSubmode.set(LayoutSubmode.ASSOCIATE_WITH_EXTRUDER0);
        } else
        {
            layoutSubmode.set(LayoutSubmode.ASSOCIATE_WITH_EXTRUDER1);
        }
    }

    private final ChangeListener<LayoutSubmode> layoutSubmodeListener = (ObservableValue<? extends LayoutSubmode> observable, LayoutSubmode oldValue, LayoutSubmode newValue) ->
    {
        materialComponent0.select(false);
        materialComponent1.select(false);
        switch (layoutSubmode.get())
        {
            case ASSOCIATE_WITH_EXTRUDER0:
                selectMaterialComponent(materialComponent0);
                break;
            case ASSOCIATE_WITH_EXTRUDER1:
                selectMaterialComponent(materialComponent1);
                break;
        }

    };

}
