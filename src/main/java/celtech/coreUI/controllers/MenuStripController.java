/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.ProjectMode;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.visualisation.SelectionContainer;
import java.io.File;
import java.net.URL;
import java.util.ListIterator;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class MenuStripController
{

    private Stenographer steno = StenographerFactory.getStenographer(MenuStripController.class.getName());
    private ApplicationStatus applicationStatus = null;
    private DisplayManager displayManager = null;
    private final FileChooser modelFileChooser = new FileChooser();
    private Project boundProject = null;
    private ResourceBundle i18nBundle = null;

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button backwardButton;

    @FXML
    private Button forwardButton;

    @FXML
    private HBox layoutButtonHBox;

    @FXML
    private Button addModelButton;

    @FXML
    private Button deleteModelButton;

    @FXML
    void forwardPressed(ActionEvent event)
    {
        switch (applicationStatus.getMode())
        {
            case STATUS:
                applicationStatus.setMode(ApplicationMode.LAYOUT);
                break;
            case LAYOUT:
                applicationStatus.setMode(ApplicationMode.SETTINGS);
                break;
            default:
                break;
        }
    }

    @FXML
    void backwardPressed(ActionEvent event)
    {
        switch (applicationStatus.getMode())
        {
            case LAYOUT:
                applicationStatus.setMode(ApplicationMode.STATUS);
                break;
            case SETTINGS:
                applicationStatus.setMode(ApplicationMode.LAYOUT);
                break;
            default:
                break;
        }
    }

    @FXML
    void addModel(ActionEvent event)
    {
        Platform.runLater(() ->
        {
            ListIterator iterator = modelFileChooser.getExtensionFilters().listIterator();

            while (iterator.hasNext())
            {
                iterator.next();
                iterator.remove();
            }

            ProjectMode projectMode = ProjectMode.NONE;

            if (displayManager.getCurrentlyVisibleProject() != null)
            {
                projectMode = displayManager.getCurrentlyVisibleProject().getProjectMode();
            }

            String descriptionOfFile = null;

            switch (projectMode)
            {
                case NONE:
                    descriptionOfFile = i18nBundle.getString("dialogs.anyFileChooserDescription");
                    break;
                case MESH:
                    descriptionOfFile = i18nBundle.getString("dialogs.meshFileChooserDescription");
                    break;
                case GCODE:
                    descriptionOfFile = i18nBundle.getString("dialogs.gcodeFileChooserDescription");
                    break;
                default:
                    break;
            }
            modelFileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter(descriptionOfFile,
                            ApplicationConfiguration.getSupportedFileExtensionWildcards(projectMode)));

            final File file = modelFileChooser.showOpenDialog(displayManager.getMainStage());

            if (file != null)
            {
                displayManager.loadExternalModel(file);
            }
        });
    }

    @FXML
    void deleteModel(ActionEvent event)
    {
        displayManager.deleteSelectedModels();
    }

    @FXML
    void copyModel(ActionEvent event)
    {
        displayManager.copySelectedModels();
    }

    @FXML
    void autoLayoutModels(ActionEvent event)
    {
        displayManager.autoLayout();
    }

    @FXML
    void snapToGround(ActionEvent event)
    {
        displayManager.activateSnapToGround();
    }

    @FXML
    void initialize()
    {
        displayManager = DisplayManager.getInstance();
        i18nBundle = DisplayManager.getLanguageBundle();
        applicationStatus = ApplicationStatus.getInstance();

        backwardButton.visibleProperty().bind(applicationStatus.modeProperty().isNotEqualTo(ApplicationMode.STATUS));
        forwardButton.visibleProperty().bind(applicationStatus.modeProperty().isNotEqualTo(ApplicationMode.SETTINGS));

        layoutButtonHBox.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(ApplicationMode.LAYOUT));

        modelFileChooser.setTitle(i18nBundle.getString("dialogs.modelFileChooser"));
        modelFileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(i18nBundle.getString("dialogs.modelFileChooserDescription"), ApplicationConfiguration.getSupportedFileExtensionWildcards(ProjectMode.NONE)));

    }

    public void bindSelectedModels(SelectionContainer selectionContainer)
    {
        deleteModelButton.disableProperty().unbind();
//        copyModelButton.disableProperty().unbind();
//        snapToGroundButton.disableProperty().unbind();
//        autoLayoutButton.disableProperty().unbind();

        deleteModelButton.disableProperty().bind(Bindings.isEmpty(selectionContainer.selectedModelsProperty()));
//        copyModelButton.disableProperty().bind(Bindings.isEmpty(selectionContainer.selectedModelsProperty()));
//        snapToGroundButton.setDisable(true);
//        autoLayoutButton.setDisable(true);
//        snapToGroundButton.disableProperty().bind(Bindings.isEmpty(selectionContainer.selectedModelsProperty()));
//        autoLayoutButton.disableProperty().bind(Bindings.isEmpty(selectionContainer.selectedModelsProperty()));

//        if (boundViewManager != null)
//        {
//            boundViewManager.layoutSubmodeProperty().removeListener(layoutChangeListener);
//        }
//
//        boundViewManager = displayManager.getCurrentlyVisibleViewManager();
//        boundViewManager.layoutSubmodeProperty().addListener(layoutChangeListener);
        if (boundProject != null)
        {
            addModelButton.disableProperty().unbind();
        }

        boundProject = displayManager.getCurrentlyVisibleProject();
        addModelButton.disableProperty().bind(Bindings.isNotEmpty(boundProject.getLoadedModels()).and(boundProject.projectModeProperty().isEqualTo(ProjectMode.GCODE)));
    }
}
