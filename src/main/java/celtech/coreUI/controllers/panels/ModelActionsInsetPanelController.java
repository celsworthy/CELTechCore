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
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
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

        ReadOnlyIntegerProperty numModelsSelected = Lookup.getProjectGUIState(project).getSelectedModelContainers().getNumModelsSelectedProperty();
        ReadOnlyIntegerProperty numGroupsSelected = Lookup.getProjectGUIState(project).getSelectedModelContainers().getNumGroupsSelectedProperty();
        group.disableProperty().bind(numModelsSelected.lessThan(2));
        ungroup.disableProperty().bind(numGroupsSelected.lessThan(1));

    }

    @FXML
    void doGroup(ActionEvent event)
    {
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getSelectedModelContainers().getSelectedModelsSnapshot();
        undoableProject.group(modelContainers);
    }

    @FXML
    void doUngroup(ActionEvent event)
    {
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getSelectedModelContainers().getSelectedModelsSnapshot();
        undoableProject.ungroup(modelContainers);
    }

    @FXML
    void doApplyMaterial0(ActionEvent event)
    {
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getSelectedModelContainers().getSelectedModelsSnapshot();
        for (ModelContainer modelContainer : modelContainers)
        {
            undoableProject.setUseExtruder0Filament(modelContainer, true);
        }

    }

    @FXML
    void doApplyMaterial1(ActionEvent event)
    {
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getSelectedModelContainers().getSelectedModelsSnapshot();
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
