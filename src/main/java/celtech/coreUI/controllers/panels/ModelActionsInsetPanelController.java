/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.undo.UndoableProject;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.utils.threed.MeshCutter;
import celtech.utils.threed.MeshCutter2;
import celtech.utils.threed.MeshDebug;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point3D;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class ModelActionsInsetPanelController implements Initializable, ProjectAwareController
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            ModelActionsInsetPanelController.class.getName());

    @FXML
    private HBox modelActionsInsetRoot;

    @FXML
    private Button group;

    @FXML
    private Button ungroup;

    @FXML
    private Button cut;

    @FXML
    private Button selectSameMaterial;

    @FXML
    private TextField cutHeight;

    private Project currentProject;
    private UndoableProject undoableProject;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {

//        Lookup.getSelectedProjectProperty().addListener(
//            (ObservableValue<? extends Project> observable, Project oldValue, Project newValue) ->
//            {
//                whenProjectChanged(newValue);
//            });
        ApplicationStatus.getInstance().modeProperty().addListener(
                (ObservableValue<? extends ApplicationMode> observable, ApplicationMode oldValue, ApplicationMode newValue) ->
                {
                    if (newValue == ApplicationMode.SETTINGS)
                    {
                        modelActionsInsetRoot.setVisible(false);
                    } else
                    {
                        modelActionsInsetRoot.setVisible(true);
                    }

                });
    }

    private void whenProjectChanged(Project project)
    {
        currentProject = project;
        undoableProject = new UndoableProject(project);

        ReadOnlyIntegerProperty numModelsSelected = Lookup.getProjectGUIState(project).getProjectSelection().getNumModelsSelectedProperty();
        ReadOnlyIntegerProperty numGroupsSelected = Lookup.getProjectGUIState(project).getProjectSelection().getNumGroupsSelectedProperty();
        group.disableProperty().bind(numModelsSelected.lessThan(2));
        cut.disableProperty().bind(numModelsSelected.lessThan(1));
        ungroup.disableProperty().bind(numGroupsSelected.lessThan(1));

        ungroup.setVisible(false);
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
        float cutHeightValue = -Float.valueOf(cutHeight.getText());
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
        ModelContainer modelContainer = modelContainers.iterator().next();

        if (modelContainer instanceof ModelGroup)
        {
            ModelGroup modelGroup = (ModelGroup) modelContainer;
            Set<ModelContainer> topModelContainers = new HashSet<>();
            Set<ModelContainer> bottomModelContainers = new HashSet<>();
            for (ModelContainer descendentModelContainer : modelGroup.getModelsHoldingMeshViews())
            {
                ModelContainerPair modelContainerPair = cutModelContainerAtHeight(
                        descendentModelContainer, cutHeightValue);
                topModelContainers.add(modelContainerPair.topModelContainer);
                bottomModelContainers.add(modelContainerPair.bottomModelContainer);
            }
            ModelGroup topGroup = currentProject.createNewGroupAndAddModelListeners(
                    topModelContainers);
            ModelGroup bottomGroup = currentProject.createNewGroupAndAddModelListeners(
                    bottomModelContainers);
            topGroup.setState(modelGroup.getState());
            topGroup.moveToCentre();
            topGroup.dropToBed();
            bottomGroup.setState(modelGroup.getState());
            bottomGroup.moveToCentre();
            bottomGroup.dropToBed();
            bottomGroup.translateBy(20, 20);

            currentProject.addModel(topGroup);
            currentProject.addModel(bottomGroup);
        } else
        {
            ModelContainerPair modelContainerPair = cutModelContainerAtHeight(modelContainer,
                    cutHeightValue);
            modelContainerPair.bottomModelContainer.moveToCentre();
            modelContainerPair.bottomModelContainer.dropToBed();
            undoableProject.addModel(modelContainerPair.bottomModelContainer);
            modelContainerPair.topModelContainer.moveToCentre();
            modelContainerPair.topModelContainer.dropToBed();
            modelContainerPair.topModelContainer.translateBy(20, 20);
            undoableProject.addModel(modelContainerPair.topModelContainer);
        }
    }

    class ModelContainerPair
    {

        final ModelContainer topModelContainer;
        final ModelContainer bottomModelContainer;

        public ModelContainerPair(ModelContainer topModelContainer,
                ModelContainer bottomModelContainer)
        {
            this.topModelContainer = topModelContainer;
            this.bottomModelContainer = bottomModelContainer;
        }
    }

    private ModelContainerPair cutModelContainerAtHeight(ModelContainer modelContainer,
            float cutHeightValue)
    {
        ModelContainerPair modelContainerPair = null;

        cutHeightValue -= modelContainer.getYAdjust();

        //these transforms must be cleared so that bedToLocal conversions work properly in the cutter.
        modelContainer.saveAndClearBedTransform();
        modelContainer.saveAndClearDropToBedYTransform();

        MeshCutter.BedToLocalConverter nullBedToLocalConverter = new MeshCutter.BedToLocalConverter()
        {

            @Override
            public Point3D localToBed(Point3D point)
            {
                return point;
            }

            @Override
            public Point3D bedToLocal(Point3D point)
            {
                return point;
            }
        };

        try
        {
            List<TriangleMesh> meshPair = MeshCutter2.cut(
                (TriangleMesh) modelContainer.getMeshView().getMesh(),
                cutHeightValue, modelContainer.getBedToLocalConverter());
            
//            MeshPair meshPair = MeshCutter2.cut(
//                (TriangleMesh) modelContainer.getMeshView().getMesh(),
//                cutHeightValue, nullBedToLocalConverter);

            String modelName = modelContainer.getModelName();

            ModelContainer topModelContainer = null;
            ModelContainer bottomModelContainer = null;
            int ix = 1;
            for (TriangleMesh subMesh : meshPair)
            {
                MeshView meshView = new MeshView(subMesh);
                meshView.cullFaceProperty().set(CullFace.NONE);
                ModelContainer newModelContainer = new ModelContainer(
                        modelContainer.getModelFile(), meshView);
                MeshDebug.setDebuggingNode(newModelContainer);
                newModelContainer.setModelName(modelName + " " + ix);
                newModelContainer.setState(modelContainer.getState());
                newModelContainer.getAssociateWithExtruderNumberProperty().set(
                        modelContainer.getAssociateWithExtruderNumberProperty().get());
                if (ix == 1)
                {
                    topModelContainer = newModelContainer;
                } else
                {
                    bottomModelContainer = newModelContainer;
                }
                
//                newModelContainer.getMeshView().setDrawMode(DrawMode.LINE);
                
                ix++;
            }

            modelContainerPair = new ModelContainerPair(topModelContainer, bottomModelContainer);

//            undoableProject.deleteModels(modelContainers);
        } finally
        {
            modelContainer.restoreBedTransform();
            modelContainer.restoreDropToBedYTransform();
        }

        return modelContainerPair;
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

}
