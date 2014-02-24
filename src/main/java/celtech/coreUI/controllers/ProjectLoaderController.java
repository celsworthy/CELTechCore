/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.appManager.Project;
import celtech.appManager.ProjectManager;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ProjectLoaderController implements Initializable
{

    @FXML
    private Button cancelButton;

    @FXML
    private Button newButton;

    @FXML
    private Button openButton;

    @FXML
    private ListView<Project> projectTitleList;

    @FXML
    private Label selectedProjectTitleLabel;

    @FXML
    void cancelButton(MouseEvent event)
    {
        myStage.close();
    }

    @FXML
    void openButton(MouseEvent event)
    {
        buttonValue = 2;
        selectedProject = projectTitleList.getSelectionModel().getSelectedItem();
        myStage.close();
    }

    @FXML
    void newButton(MouseEvent event)
    {
        buttonValue = 1;
        myStage.close();
    }

    private int buttonValue = -1;
    private Project selectedProject = null;

    private Stage myStage = null;

    private ProjectManager projectManager = null;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        projectManager = ProjectManager.getInstance();
        
        projectTitleList.setEditable(false);
        projectTitleList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        openButton.disableProperty().bind(projectTitleList.getSelectionModel().selectedItemProperty().isNull());
    }

    public void configure(Stage dialogStage)
    {
        myStage = dialogStage;
    }

    public int getButtonValue()
    {
        return buttonValue;
    }

    public Project getSelectedProject()
    {
        return selectedProject;
    }

    public void repopulateProjects()
    {
        ObservableList<Project> projects = FXCollections.observableArrayList(projectManager.getAvailableProjects());
        projectTitleList.setItems(projects);
        projectTitleList.getSelectionModel().clearSelection();
    }

}
