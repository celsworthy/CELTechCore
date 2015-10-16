/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.undo.UndoableProject;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.ProjectGUIRules;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.coreUI.visualisation.ProjectSelection;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import jfxtras.styles.jmetro8.ToggleSwitch;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class ModelEditInsetPanelController implements Initializable, ProjectAwareController
{

    private final Stenographer steno = StenographerFactory.getStenographer(ModelEditInsetPanelController.class.getName());

    @FXML
    private VBox modelEditInsetRoot;

    @FXML
    private Label modelName;

    @FXML
    private ToggleButton setMaterial0Button;

    @FXML
    private ToggleButton setMaterial1Button;

    @FXML
    private ToggleGroup materialButtons;

    @FXML
    private RestrictedNumberField widthTextField;

    @FXML
    private RestrictedNumberField depthTextField;

    @FXML
    private RestrictedNumberField xAxisTextField;

    @FXML
    private Label scaleXCaption;

    @FXML
    private Label scaleYCaption;

    @FXML
    private Label scaleZCaption;

    @FXML
    private Label widthCaption;

    @FXML
    private Label heightCaption;

    @FXML
    private Label depthCaption;

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
    private ToggleSwitch useProportionalScaleSwitch;

    private Project currentProject;
    private UndoableProject undoableProject;

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
    /**
     * The last scale ratio that was applied to the current selection. This
     * figure is reset to 1.0 when the selection changes.
     */
    private double lastScaleRatio = 1.0d;
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

    private IntegerProperty numSelectedModels = new SimpleIntegerProperty(0);
    private ProjectSelection projectSelection;
    private ProjectGUIRules projectGUIRules;
    private ObjectProperty<LayoutSubmode> layoutSubmode;

    @FXML
    private void setMaterial0(ActionEvent event)
    {
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
        for (ModelContainer modelContainer : modelContainers)
        {
            undoableProject.setUseExtruder0Filament(modelContainer, true);
        }
    }

    @FXML
    private void setMaterial1(ActionEvent event)
    {
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
        for (ModelContainer modelContainer : modelContainers)
        {
            undoableProject.setUseExtruder0Filament(modelContainer, false);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        initialiseTextFieldValues();

        setUpModelGeometryListeners();
        setUpNumberFieldListeners();
        setupProjectSelectedListener();
        setUpNumSelectedModelsListener();
        setUpAspectRatioListener(resources);

        Image image = new Image(getClass().getResourceAsStream(
                ApplicationConfiguration.imageResourcePath + "link.png"));
        linkedImage = new ImageView(image);
        image = new Image(getClass().getResourceAsStream(
                ApplicationConfiguration.imageResourcePath + "unlink.png"));
        unlinkedImage = new ImageView(image);

        Lookup.getSelectedProjectProperty().addListener(
                (ObservableValue<? extends Project> observable, Project oldValue, Project newValue) ->
                {
                    whenProjectChanged(newValue);
                });

        ApplicationStatus.getInstance().modeProperty().addListener(
                (ObservableValue<? extends ApplicationMode> observable, ApplicationMode oldValue, ApplicationMode newValue) ->
                {
                    if (newValue == ApplicationMode.SETTINGS)
                    {
                        modelEditInsetRoot.setVisible(false);
                    } else
                    {
                        modelEditInsetRoot.setVisible(true);
                    }

                });
 
        useProportionalScaleSwitch.selectedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                scaleTextDepthField.setVisible(newValue);
                scaleTextHeightField.setVisible(newValue);
                scaleTextWidthField.setVisible(newValue);
                depthTextField.setVisible(!newValue);
                heightTextField.setVisible(!newValue);
                widthTextField.setVisible(!newValue);
                scaleXCaption.setVisible(newValue);
                scaleYCaption.setVisible(newValue);
                scaleZCaption.setVisible(newValue);
                depthCaption.setVisible(!newValue);
                heightCaption.setVisible(!newValue);
                widthCaption.setVisible(!newValue);
            }
        });

        updateDisplay();
    }

    private void updateDisplay()
    {
        boolean showDisplay = false;

        if (currentProject != null)
        {
            ReadOnlyIntegerProperty numModelsSelected = Lookup.getProjectGUIState(currentProject).getProjectSelection().getNumModelsSelectedProperty();
            ReadOnlyIntegerProperty numGroupsSelected = Lookup.getProjectGUIState(currentProject).getProjectSelection().getNumGroupsSelectedProperty();

            if (numGroupsSelected.get() > 1 || numModelsSelected.get() > 1)
            {
                modelName.setText(Lookup.i18n("modelEdit.MultipleModelsSelected"));
                showDisplay = true;
            } else if (numGroupsSelected.get() == 1 || numModelsSelected.get() == 1)
            {
                modelName.setText(Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot().iterator().next().getModelName());
                showDisplay = true;
            }

            boolean foundMaterial0 = false;
            boolean foundMaterial1 = false;

            Iterator<ModelContainer> selectedModelIterator = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot().iterator();
            while (selectedModelIterator.hasNext())
            {
                ModelContainer container = selectedModelIterator.next();
                if (container.getAssociateWithExtruderNumberProperty().get() == 0)
                {
                    foundMaterial0 = true;
                } else
                {
                    foundMaterial1 = true;
                }
            }

            if (foundMaterial0 && !foundMaterial1)
            {
                materialButtons.selectToggle(setMaterial0Button);
            } else if (!foundMaterial0 && foundMaterial1)
            {
                materialButtons.selectToggle(setMaterial1Button);
            } else
            {
                materialButtons.selectToggle(null);
            }
        }

        modelEditInsetRoot.setVisible(showDisplay);
    }

    private void whenProjectChanged(Project project)
    {
        if (currentProject != null)
        {
            numSelectedModels.unbind();
        }

        currentProject = project;
        undoableProject = new UndoableProject(project);
        updateDisplay();

        projectSelection = Lookup.getProjectGUIState(project).getProjectSelection();
        projectGUIRules = Lookup.getProjectGUIState(project).getProjectGUIRules();
        numSelectedModels.bind(projectSelection.getNumModelsSelectedProperty());

        layoutSubmode = Lookup.getProjectGUIState(project).getLayoutSubmodeProperty();

        ProjectSelection.PrimarySelectedModelDetails selectedModelDetails
                = projectSelection.getPrimarySelectedModelDetails();
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

        modelEditInsetRoot.visibleProperty().bind(
                Bindings.lessThan(0, projectSelection.getNumModelsSelectedProperty()));

        setFieldsEditable();

//        group.disableProperty().bind(numModelsSelected.lessThan(2));
//        cut.disableProperty().bind(numModelsSelected.lessThan(1));
//        ungroup.disableProperty().bind(numGroupsSelected.lessThan(1));
    }

    /**
     * Group the selection. If one group was made then select it.
     */
    @FXML
    void doGroup(ActionEvent event)
    {
        Set<ModelContainer> modelGroups = currentProject.getTopLevelModels().stream().filter(
                mc -> mc instanceof ModelGroup).collect(Collectors.toSet());
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
        undoableProject.group(modelContainers);
        Set<ModelContainer> changedModelGroups = currentProject.getTopLevelModels().stream().filter(
                mc -> mc instanceof ModelGroup).collect(Collectors.toSet());
        changedModelGroups.removeAll(modelGroups);
        Lookup.getProjectGUIState(currentProject).getProjectSelection().deselectAllModels();
        if (changedModelGroups.size() == 1)
        {
            Lookup.getProjectGUIState(currentProject).getProjectSelection().addModelContainer(
                    changedModelGroups.iterator().next());
        }
    }

    @FXML
    void doUngroup(ActionEvent event)
    {
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
        undoableProject.ungroup(modelContainers);
        Lookup.getProjectGUIState(currentProject).getProjectSelection().deselectAllModels();
    }

    @FXML
    void doSelectSameMaterial(ActionEvent event)
    {
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
        if (modelContainers.size() > 0)
        {
            ModelContainer firstModelContainer = modelContainers.iterator().next();
            int associatedExtruder = firstModelContainer.getAssociateWithExtruderNumberProperty().get();
            Set<ModelContainer> allModels = currentProject.getAllModels();

            allModels.forEach(candidateModel ->
            {
                if (candidateModel.getAssociateWithExtruderNumberProperty().get()
                        == associatedExtruder)
                {
                    Lookup.getProjectGUIState(currentProject).getProjectSelection().addModelContainer(candidateModel);
                }
            });
        }
    }

    @FXML
    void doCut(ActionEvent event)
    {
//        float cutHeightValue = -Float.valueOf(cutHeight.getText());
//        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
//        
//        undoableProject.cut(modelContainers, cutHeightValue);
    }

    @FXML
    void doApplyMaterial0(ActionEvent event)
    {
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
        for (ModelContainer modelContainer : modelContainers)
        {
            undoableProject.setUseExtruder0Filament(modelContainer, true);
        }

    }

    @FXML
    void doApplyMaterial1(ActionEvent event)
    {
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
        for (ModelContainer modelContainer : modelContainers)
        {
            undoableProject.setUseExtruder0Filament(modelContainer, false);
        }
    }

    @FXML
    void doDropToBed(ActionEvent event)
    {
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
        undoableProject.dropToBed(modelContainers);
    }

    @Override
    public void setProject(Project project)
    {
        whenProjectChanged(project);
    }

    /**
     * Repopulate the widgets for the given model details.
     *
     * @param selectedModelDetails
     */
    private void repopulate(ProjectSelection.PrimarySelectedModelDetails selectedModelDetails)
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

    /**
     * Change the preserve aspect ratio icon to linked / unlinked according to
     * whether it is selected or not.
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
        useProportionalScaleSwitch.setSelected(false);
    }

    private void setupProjectSelectedListener()
    {
        Lookup.getSelectedProjectProperty().addListener(
                (ObservableValue<? extends Project> observable, Project oldValue, Project newValue) ->
                {
                    whenProjectChanged(newValue);
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
     * Return if we are in a multi-select and fixed aspect ratio is being
     * applied.
     */
    private boolean inMultiSelectWithFixedAR()
    {
        return preserveAspectRatio.isSelected()
                && (projectSelection.getNumModelsSelectedProperty().get() > 1);
    }

    private boolean inMultiSelect()
    {
        return projectSelection.getNumModelsSelectedProperty().get() > 1;
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
            undoableProject.rotateLeanModels(projectSelection.getSelectedModelsSnapshot(),
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
            undoableProject.rotateTwistModels(projectSelection.getSelectedModelsSnapshot(),
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
            undoableProject.rotateTurnModels(projectSelection.getSelectedModelsSnapshot(),
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
    private void addNumberFieldListener(RestrictedNumberField textField, LayoutSidePanelController.NoArgsVoidFunc func)
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
            undoableProject.translateModelsZTo(projectSelection.getSelectedModelsSnapshot(), newY);
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
            undoableProject.translateModelsXTo(projectSelection.getSelectedModelsSnapshot(), newX);

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
                        projectSelection.getSelectedModelsSnapshot(), ratio);

            } else
            {
                undoableProject.resizeModelsDepth(projectSelection.getSelectedModelsSnapshot(),
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
                        projectSelection.getSelectedModelsSnapshot(), ratio);
            } else
            {
                undoableProject.resizeModelsHeight(projectSelection.getSelectedModelsSnapshot(),
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
                        projectSelection.getSelectedModelsSnapshot(), ratio);
            } else
            {
                undoableProject.resizeModelsWidth(projectSelection.getSelectedModelsSnapshot(),
                        widthTextField.getAsDouble());
            }
        } catch (ParseException ex)
        {
            steno.warning("Error converting width " + widthTextField.getText());
        }
    }

    private ModelContainer getSingleSelection()
    {
        assert (projectSelection.getNumModelsSelectedProperty().get() == 1);
        ModelContainer modelContainer = projectSelection.getSelectedModelsSnapshot().iterator().next();
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
                        projectSelection.getSelectedModelsSnapshot(),
                        ratio);
                showScaleForXYZ(lastScaleRatio);
            } else
            {
                undoableProject.scaleZModels(projectSelection.getSelectedModelsSnapshot(),
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
                        projectSelection.getSelectedModelsSnapshot(),
                        ratio);
                showScaleForXYZ(lastScaleRatio);
            } else
            {
                undoableProject.scaleYModels(projectSelection.getSelectedModelsSnapshot(),
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
                        projectSelection.getSelectedModelsSnapshot(),
                        ratio);
                showScaleForXYZ(lastScaleRatio);
            } else
            {
                undoableProject.scaleXModels(projectSelection.getSelectedModelsSnapshot(),
                        scaleFactor, inFixedAR());
            }
        } catch (ParseException ex)
        {
            steno.warning("Error converting scale " + scaleTextWidthField.getText());
        }
    }

    /**
     * This updates location, size and scale fields to be editable or not
     * according to whether we are in a multi-selection or not or child of a
     * group.
     */
    private void setFieldsEditable()
    {
        xAxisTextField.disableProperty().bind(numSelectedModels.greaterThan(1).or(projectGUIRules.canTranslateRotateOrScaleSelection().not()));
        yAxisTextField.disableProperty().bind(numSelectedModels.greaterThan(1).or(projectGUIRules.canTranslateRotateOrScaleSelection().not()));
        widthTextField.disableProperty().bind(numSelectedModels.greaterThan(1).or(projectGUIRules.canTranslateRotateOrScaleSelection().not()));
        heightTextField.disableProperty().bind(numSelectedModels.greaterThan(1).or(projectGUIRules.canTranslateRotateOrScaleSelection().not()));
        depthTextField.disableProperty().bind(numSelectedModels.greaterThan(1).or(projectGUIRules.canTranslateRotateOrScaleSelection().not()));
        rotationXTextField.disableProperty().bind(numSelectedModels.greaterThan(1).or(projectGUIRules.canTranslateRotateOrScaleSelection().not()));
        rotationYTextField.disableProperty().bind(numSelectedModels.greaterThan(1).or(projectGUIRules.canTranslateRotateOrScaleSelection().not()));
        rotationZTextField.disableProperty().bind(numSelectedModels.greaterThan(1).or(projectGUIRules.canTranslateRotateOrScaleSelection().not()));

        scaleTextWidthField.disableProperty().bind(projectGUIRules.canTranslateRotateOrScaleSelection().not());
        scaleTextHeightField.disableProperty().bind(projectGUIRules.canTranslateRotateOrScaleSelection().not());
        scaleTextDepthField.disableProperty().bind(projectGUIRules.canTranslateRotateOrScaleSelection().not());

    }

    private void whenNumSelectedModelsChanged()
    {
        lastScaleRatio = 1.0d;
        if (inMultiSelect())
        {
            showScaleForXYZ(1.0d);
        } else if (projectSelection.getNumModelsSelectedProperty().get() == 1)
        {
            ModelContainer modelContainer = getSingleSelection();
            populateScaleXField(modelContainer.getXScale());
            populateScaleYField(modelContainer.getYScale());
            populateScaleZField(modelContainer.getZScale());
        }
    }

}
