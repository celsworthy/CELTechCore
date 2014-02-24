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
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.components.EnhancedToggleGroup;
import celtech.coreUI.visualisation.SelectionContainer;
import celtech.coreUI.visualisation.ThreeDViewManager;
import java.io.File;
import java.net.URL;
import java.util.ListIterator;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class LayoutControlsController extends ButtonController implements Initializable
{

    private final ApplicationStatus applicationStatus = ApplicationStatus.getInstance();
    private DisplayManager displayManager = null;
    private EventHandler<MouseEvent> mouseHandler = null;
    private final FileChooser modelFileChooser = new FileChooser();
    private ObjectProperty<Toggle> selectedToggle = new SimpleObjectProperty<>();
    private ChangeListener<LayoutSubmode> layoutChangeListener = null;
    private ThreeDViewManager boundViewManager = null;
    private Project boundProject = null;
    private ResourceBundle i18nBundle = null;

    @FXML
    private VBox container;
    @FXML
    private Button addModelButton;
    @FXML
    private Button deleteModelButton;
    @FXML
    private Button copyModelButton;
    @FXML
    private ToggleButton snapToGroundButton;
    @FXML
    private Button autoLayoutButton;

    @FXML
    void addModel(MouseEvent event)
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
    void deleteModel(MouseEvent event)
    {
        displayManager.deleteSelectedModels();
    }

    @FXML
    void copyModel(MouseEvent event)
    {
        displayManager.copySelectedModels();
    }

    @FXML
    void snapToGround(MouseEvent event)
    {
        displayManager.activateSnapToGround();
    }

    @FXML
    void autoLayout(MouseEvent event)
    {
        displayManager.autoLayout();
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        displayManager = DisplayManager.getInstance();

        i18nBundle = DisplayManager.getLanguageBundle();

        layoutChangeListener = new ChangeListener<LayoutSubmode>()
        {
            @Override
            public void changed(ObservableValue<? extends LayoutSubmode> ov, LayoutSubmode t, LayoutSubmode t1)
            {
                if (t1 == LayoutSubmode.SNAP_TO_GROUND)
                {
                    selectedToggle.setValue(snapToGroundButton);
                }
            }
        };

        modelFileChooser.setTitle(i18nBundle.getString("dialogs.modelFileChooser"));
        modelFileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(i18nBundle.getString("dialogs.modelFileChooserDescription"), ApplicationConfiguration.getSupportedFileExtensionWildcards(ProjectMode.NONE)));

        container.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(ApplicationMode.LAYOUT));

        EnhancedToggleGroup snapToGroundGroup = new EnhancedToggleGroup();
        snapToGroundButton.setToggleGroup(snapToGroundGroup);
        selectedToggle.bindBidirectional(snapToGroundGroup.writableSelectedToggleProperty());

        snapToGroundButton.selectedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                if (t1.booleanValue() == true)
                {
                    displayManager.getCurrentlyVisibleViewManager().activateSnapToGround();
                }
            }
        });

    }

    public void bindSelectedModels(SelectionContainer selectionContainer)
    {
        deleteModelButton.disableProperty().unbind();
        copyModelButton.disableProperty().unbind();
        snapToGroundButton.disableProperty().unbind();
        autoLayoutButton.disableProperty().unbind();

        deleteModelButton.disableProperty().bind(Bindings.isEmpty(selectionContainer.selectedModelsProperty()));
        copyModelButton.disableProperty().bind(Bindings.isEmpty(selectionContainer.selectedModelsProperty()));
        snapToGroundButton.setDisable(true);
        autoLayoutButton.setDisable(true);
        snapToGroundButton.disableProperty().bind(Bindings.isEmpty(selectionContainer.selectedModelsProperty()));
        autoLayoutButton.disableProperty().bind(Bindings.isEmpty(selectionContainer.selectedModelsProperty()));

        if (boundViewManager != null)
        {
            boundViewManager.layoutSubmodeProperty().removeListener(layoutChangeListener);
        }

        boundViewManager = displayManager.getCurrentlyVisibleViewManager();
        boundViewManager.layoutSubmodeProperty().addListener(layoutChangeListener);

        if (boundProject != null)
        {
            addModelButton.disableProperty().unbind();
        }

        boundProject = displayManager.getCurrentlyVisibleProject();
        addModelButton.disableProperty().bind(Bindings.isNotEmpty(boundProject.getLoadedModels()).and(boundProject.projectModeProperty().isEqualTo(ProjectMode.GCODE)));
    }
}
