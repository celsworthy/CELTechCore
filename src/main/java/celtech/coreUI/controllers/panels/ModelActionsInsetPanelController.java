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
import celtech.coreUI.visualisation.ApplicationMaterials;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.utils.threed.MeshCutter;
import celtech.utils.threed.MeshSeparator;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
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
        ungroup.disableProperty().bind(numGroupsSelected.lessThan(1));

    }

    /**
     * Group the selection. If one group was made then select it.
     * @param event 
     */
    @FXML
    void doGroup(ActionEvent event)
    {
        Set<ModelContainer> modelGroups = currentProject.getTopLevelModels().stream().filter(mc -> mc instanceof ModelGroup).collect(
                Collectors.toSet());
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
        undoableProject.group(modelContainers);
        Set<ModelContainer> changedModelGroups = currentProject.getTopLevelModels().stream().filter(mc -> mc instanceof ModelGroup).collect(
                Collectors.toSet());
        changedModelGroups.removeAll(modelGroups);
        Lookup.getProjectGUIState(currentProject).getProjectSelection().deselectAllModels();
        if (changedModelGroups.size() == 1) {
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
    void doCut(ActionEvent event) {
        double cutHeightValue = - Double.valueOf(cutHeight.getText());
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
        ModelContainer modelContainer = modelContainers.iterator().next();
        
        MeshCutter.setDebuggingNode(modelContainer);
        
        Set<TriangleMesh> subMeshes = MeshCutter.cut((TriangleMesh) modelContainer.getMeshView().getMesh(), cutHeightValue);
        
        ModelContainer.State state = modelContainer.getState();
        String modelName = modelContainer.getModelName();

        if (subMeshes.size() > 1)
        {
            int ix = 1;
            for (TriangleMesh subMesh : subMeshes)
            {
                MeshView meshView = new MeshView(subMesh);
                ModelContainer newModelContainer = new ModelContainer(
                    modelContainer.getModelFile(), meshView);
                newModelContainer.setState(state);
                newModelContainer.setModelName(modelName + " " + ix);
                undoableProject.addModel(newModelContainer);
                ix++;
            }    
//            undoableProject.deleteModels(modelContainers);
        }
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

    @Override
    public void setProject(Project project)
    {
        whenProjectChanged(project);
    }

}
