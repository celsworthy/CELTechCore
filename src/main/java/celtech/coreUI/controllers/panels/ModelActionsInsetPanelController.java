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
    private Button splitIntoParts;

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
        splitIntoParts.disableProperty().bind(numModelsSelected.isEqualTo(0));
        group.disableProperty().bind(numModelsSelected.isEqualTo(0));
        ungroup.disableProperty().bind(numModelsSelected.isEqualTo(0));

    }

    @FXML
    void doSplit(ActionEvent event)
    {
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getSelectedModelContainers().getSelectedModelsSnapshot();
        try
        {
            undoableProject.splitIntoParts(modelContainers);
        } catch (StackOverflowError soe)
        {
            Lookup.getSystemNotificationHandler().showWarningNotification(Lookup.i18n(
                "splitParts.title"), Lookup.i18n("splitParts.message"));
        }
    }

    @FXML
    void doGroup(ActionEvent event)
    {
    }

    @FXML
    void doUngroup(ActionEvent event)
    {
    }

    @Override
    public void setProject(Project project)
    {
        whenProjectChanged(project);
    }

}
