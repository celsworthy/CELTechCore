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
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
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
                        modelEditInsetRoot.setVisible(false);
                    } else
                    {
                        modelEditInsetRoot.setVisible(true);
                    }

                });
    }

    private void whenProjectChanged(Project project)
    {
        currentProject = project;
        undoableProject = new UndoableProject(project);

        ReadOnlyIntegerProperty numModelsSelected = Lookup.getProjectGUIState(project).getProjectSelection().getNumModelsSelectedProperty();
        ReadOnlyIntegerProperty numGroupsSelected = Lookup.getProjectGUIState(project).getProjectSelection().getNumGroupsSelectedProperty();
        
        if (numGroupsSelected.get() > 1 || numModelsSelected.get() > 1)
        {
            modelName.setText(Lookup.i18n("modelEdit.MultipleModelsSelected"));
        }
        else if (numGroupsSelected.get() == 1 || numModelsSelected.get() == 1)
        {
            modelName.setText(Lookup.getProjectGUIState(project).getProjectSelection().getSelectedModelsSnapshot().iterator().next().getModelName());
        }
        else
        {
            modelName.setText("");
        }
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

}
