/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.undo.UndoableProject;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.ProjectGUIRules;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.coreUI.visualisation.ProjectSelection;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.utils.Math.MathUtils;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
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

    public interface NoArgsVoidFunc
    {

        void run() throws Exception;
    }

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

    @FXML
    private VBox movePanel;

    @FXML
    private VBox scalePanel;

    @FXML
    private VBox rotatePanel;

    @FXML
    private ToggleButton moveButton;

    @FXML
    private ToggleButton scaleButton;

    @FXML
    private ToggleButton rotateButton;

    @FXML
    private ToggleGroup tabButtons;

    @FXML
    private SVGPath linkIcon;

    @FXML
    private Group unlinkIcon;

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

    @FXML
    private void flipMaterials(ActionEvent event)
    {
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
        for (ModelContainer modelContainer : modelContainers)
        {
            if (modelContainer instanceof ModelGroup)
            {
                Set<ModelContainer> descendentModels = modelContainer.getDescendentModelContainers();
                for (ModelContainer descendentModelContainer : descendentModels)
                {
                    undoableProject.setUseExtruder0Filament(descendentModelContainer, !(descendentModelContainer.getAssociateWithExtruderNumberProperty().get() == 0));
                }
            } else
            {
                undoableProject.setUseExtruder0Filament(modelContainer, !(modelContainer.getAssociateWithExtruderNumberProperty().get() == 0));
            }
        }
        updateDisplay();
    }

    private ChangeListener<LayoutSubmode> layoutSubmodeListener = new ChangeListener<LayoutSubmode>()
    {
        @Override
        public void changed(ObservableValue<? extends LayoutSubmode> observable, LayoutSubmode oldValue, LayoutSubmode newValue)
        {
            modelEditInsetRoot.setDisable(newValue == LayoutSubmode.Z_CUT);
        }
    };

    private void updateProportionalLabels(boolean proportionalOn)
    {
        scaleTextDepthField.setVisible(proportionalOn);
        scaleTextHeightField.setVisible(proportionalOn);
        scaleTextWidthField.setVisible(proportionalOn);
        depthTextField.setVisible(!proportionalOn);
        heightTextField.setVisible(!proportionalOn);
        widthTextField.setVisible(!proportionalOn);
        scaleXCaption.setVisible(proportionalOn);
        scaleYCaption.setVisible(proportionalOn);
        scaleZCaption.setVisible(proportionalOn);
        depthCaption.setVisible(!proportionalOn);
        heightCaption.setVisible(!proportionalOn);
        widthCaption.setVisible(!proportionalOn);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        unlinkIcon.setVisible(false);
        linkIcon.visibleProperty().bind(preserveAspectRatio.selectedProperty());
        unlinkIcon.visibleProperty().bind(preserveAspectRatio.selectedProperty().not());

        initialiseTextFieldValues();

        setUpModelGeometryListeners();
        setUpNumberFieldListeners();
        setupProjectSelectedListener();
        setUpNumSelectedModelsListener();
        preserveAspectRatio.setSelected(true);

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
                        updateDisplay();
                    }

                });

        useProportionalScaleSwitch.setSelected(false);
        updateProportionalLabels(false);

        useProportionalScaleSwitch.selectedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                updateProportionalLabels(newValue);
            }
        });

        tabButtons.selectedToggleProperty().addListener(new ChangeListener<Toggle>()
        {
            @Override
            public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue)
            {
                if (newValue == moveButton
                        && oldValue != moveButton)
                {
                    movePanel.setVisible(true);
                    scalePanel.setVisible(false);
                    rotatePanel.setVisible(false);
                } else if (newValue == scaleButton
                        && oldValue != scaleButton)
                {
                    movePanel.setVisible(false);
                    scalePanel.setVisible(true);
                    rotatePanel.setVisible(false);
                } else if (newValue == rotateButton
                        && oldValue != rotateButton)
                {
                    movePanel.setVisible(false);
                    scalePanel.setVisible(false);
                    rotatePanel.setVisible(true);
                }

                if (newValue == null
                        && oldValue != null)
                {
                    oldValue.setSelected(true);
                }
            }
        });

        tabButtons.selectToggle(moveButton);

        updateDisplay();
    }

    private void updateDisplay()
    {
        boolean showDisplay = false;

        if (currentProject != null)
        {
            Set<ModelContainer> selectedModels = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();

            if (selectedModels.size() > 1)
            {
                modelName.setText(Lookup.i18n("modelEdit.MultipleModelsSelected"));
                modelName.setTooltip(null);
                showDisplay = true;
            } else if (selectedModels.size() == 1)
            {
                String name = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot().iterator().next().getModelName();
                modelName.setText(name);
                modelName.setTooltip(new Tooltip(name));
                showDisplay = true;
            }

            if (showDisplay)
            {
                boolean foundMaterial0 = false;
                boolean foundMaterial1 = false;

                Iterator<ModelContainer> selectedModelIterator = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot().iterator();
                while (selectedModelIterator.hasNext())
                {
                    ModelContainer container = selectedModelIterator.next();
                    if (container instanceof ModelGroup)
                    {
                        Set<ModelContainer> childModels = container.getDescendentModelContainers();
                        for (ModelContainer childModel : childModels)
                        {
                            if (childModel.getAssociateWithExtruderNumberProperty().get() == 0)
                            {
                                foundMaterial0 = true;
                            } else
                            {
                                foundMaterial1 = true;
                            }
                        }
                    } else
                    {
                        if (container.getAssociateWithExtruderNumberProperty().get() == 0)
                        {
                            foundMaterial0 = true;
                        } else
                        {
                            foundMaterial1 = true;
                        }
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
        }

        modelEditInsetRoot.setVisible(showDisplay);
    }

    private void whenProjectChanged(Project project)
    {
        if (currentProject != null)
        {
            numSelectedModels.unbind();
        }

        if (layoutSubmode != null)
        {
            layoutSubmode.removeListener(layoutSubmodeListener);
        }

        currentProject = project;
        undoableProject = new UndoableProject(project);

        projectSelection = Lookup.getProjectGUIState(project).getProjectSelection();
        projectGUIRules = Lookup.getProjectGUIState(project).getProjectGUIRules();
        numSelectedModels.bind(projectSelection.getNumModelsSelectedProperty());

        layoutSubmode = Lookup.getProjectGUIState(project).getLayoutSubmodeProperty();
        layoutSubmode.addListener(layoutSubmodeListener);

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

        setFieldsEditable();
        updateDisplay();

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
                        steno.warning("exception updating number field " + ex);
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
                        steno.warning("exception updating number field " + ex);
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
            double newDepth = limitDimension(depthTextField.getAsDouble());
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
                double ratio = newDepth / modelContainer.getScaledDepth();

                undoableProject.scaleXYZRatioSelection(
                        projectSelection.getSelectedModelsSnapshot(), ratio);

            } else
            {
                undoableProject.resizeModelsDepth(projectSelection.getSelectedModelsSnapshot(),
                        newDepth);
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
            double newHeight = limitDimension(heightTextField.getAsDouble());
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
                double ratio = newHeight / modelContainer.getScaledHeight();
                undoableProject.scaleXYZRatioSelection(
                        projectSelection.getSelectedModelsSnapshot(), ratio);
            } else
            {
                undoableProject.resizeModelsHeight(projectSelection.getSelectedModelsSnapshot(),
                        newHeight);
            }
        } catch (ParseException ex)
        {
            steno.warning("Error converting height " + heightTextField.getText());
        }
    }

    private final double MINIMUM_DIMENSION = 0.1;

    private double limitDimension(final double inboundDimension)
    {
        return Math.max(MINIMUM_DIMENSION, inboundDimension);
    }

    private enum ApplicableDimension
    {

        WIDTH,
        HEIGHT,
        DEPTH
    }

    private double limitScaleFactor(final double scaleFactor, final ApplicableDimension applicableDimension)
    {
        double minDimension = 9999999;
        for (ModelContainer modelContainer : projectSelection.getSelectedModelsSnapshot())
        {
            if (applicableDimension == ApplicableDimension.WIDTH || inFixedAR())
            {
                minDimension = Math.min(modelContainer.getScaledWidth(), minDimension);
            }
            if (applicableDimension == ApplicableDimension.HEIGHT || inFixedAR())
            {
                minDimension = Math.min(modelContainer.getScaledHeight(), minDimension);
            }
            if (applicableDimension == ApplicableDimension.DEPTH || inFixedAR())
            {
                minDimension = Math.min(modelContainer.getScaledDepth(), minDimension);
            }
        }

        if (minDimension * scaleFactor < MINIMUM_DIMENSION)
        {
            return MINIMUM_DIMENSION / minDimension;
        } else
        {
            return scaleFactor;
        }
    }

    private void updateWidth()
    {
        try
        {
            double newWidth = limitDimension(widthTextField.getAsDouble());
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
                double ratio = newWidth / modelContainer.getScaledWidth();
                undoableProject.scaleXYZRatioSelection(
                        projectSelection.getSelectedModelsSnapshot(), ratio);
            } else
            {
                undoableProject.resizeModelsWidth(projectSelection.getSelectedModelsSnapshot(),
                        newWidth);
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
            if (MathUtils.compareDouble(newScaleDepth, lastScaleDepth, 0.001) == MathUtils.EQUAL
                    || newScaleDepth < 0)
            {
                return;
            } else
            {
                lastScaleDepth = newScaleDepth;
            }

            double scaleFactor = limitScaleFactor(newScaleDepth / 100.0, ApplicableDimension.DEPTH);
            if (MathUtils.compareDouble(scaleFactor, newScaleDepth / 100.0, 0.00001) != MathUtils.EQUAL)
            {
                newScaleDepth = scaleFactor * 100.0;
                lastScaleDepth = newScaleDepth;
            }

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

            if (MathUtils.compareDouble(newScaleHeight, lastScaleHeight, 0.001) == MathUtils.EQUAL
                    || newScaleHeight < 0)
            {
                return;
            } else
            {
                lastScaleHeight = newScaleHeight;
            }

            double scaleFactor = limitScaleFactor(newScaleHeight / 100.0, ApplicableDimension.HEIGHT);
            if (MathUtils.compareDouble(scaleFactor, newScaleHeight / 100.0, 0.00001) != MathUtils.EQUAL)
            {
                newScaleHeight = scaleFactor * 100.0;
                lastScaleDepth = newScaleHeight;
            }

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

            if (MathUtils.compareDouble(newScaleWidth, lastScaleWidth, 0.001) == MathUtils.EQUAL
                    || newScaleWidth < 0)
            {
                return;
            } else
            {
                lastScaleWidth = newScaleWidth;
            }

            double scaleFactor = limitScaleFactor(newScaleWidth / 100.0, ApplicableDimension.WIDTH);
            if (MathUtils.compareDouble(scaleFactor, newScaleWidth / 100.0, 0.00001) != MathUtils.EQUAL)
            {
                newScaleWidth = scaleFactor * 100.0;
                lastScaleDepth = newScaleWidth;
            }

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
        xAxisTextField.disableProperty().bind(numSelectedModels.greaterThan(1));
        yAxisTextField.disableProperty().bind(numSelectedModels.greaterThan(1));
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
        } else if (projectSelection.getNumModelsSelectedProperty().get() == 1)
        {
            ModelContainer modelContainer = getSingleSelection();
            populateScaleXField(modelContainer.getXScale());
            populateScaleYField(modelContainer.getYScale());
            populateScaleZField(modelContainer.getZScale());
        }
        updateDisplay();
    }

}
