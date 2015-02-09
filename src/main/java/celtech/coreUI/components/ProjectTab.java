/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.components;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.ProjectManager;
import celtech.appManager.ProjectMode;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintBed;
import static celtech.utils.DeDuplicator.suggestNonDuplicateName;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.GCodeEditorPanelController;
import celtech.coreUI.visualisation.CameraPositionPreset;
import celtech.coreUI.visualisation.SelectedModelContainers;
import celtech.coreUI.visualisation.ThreeDViewManager;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelContentsEnumeration;
import celtech.utils.Math.Packing.PackingThing;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.effect.Glow;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ProjectTab extends Tab
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        ProjectTab.class.getName());

    private final Label nonEditableProjectNameField = new Label();
    private final RestrictedTextField editableProjectNameField = new RestrictedTextField();
    private Project project = null;
    private AnchorPane basePane = null;
    private ThreeDViewManager viewManager = null;
    private DisplayManager displayManager = null;
    private final ProjectManager projectManager = ProjectManager.getInstance();
    private boolean titleBeingEdited = false;
//    private Xform gizmoXform = new Xform(Xform.RotateOrder.YXZ);

    final Rectangle testRect = new Rectangle(5, 5);

//    private final ChangeListener<Number> selectionContainerMoveListener = new ChangeListener<Number>()
//    {
//        @Override
//        public void changed(ObservableValue<? extends Number> ov, Number t,
//            Number t1)
//        {
////            Point2D reference = basePane.localToScreen(0, 0);
////            double x = viewManager.getSelectionContainer().getScreenX()
////                - reference.getX();
////            double y = viewManager.getSelectionContainer().getScreenY()
////                - reference.getY();
////            gizmoXform.setTx(x);
////            gizmoXform.setTy(y);
//        }
//    };
    /**
     *
     * @param dispManagerRef
     * @param tabDisplayWidthProperty
     * @param tabDisplayHeightProperty
     */
    public ProjectTab(DisplayManager dispManagerRef,
        ReadOnlyDoubleProperty tabDisplayWidthProperty,
        ReadOnlyDoubleProperty tabDisplayHeightProperty)
    {
        displayManager = dispManagerRef;
        project = new Project();
        initialise(tabDisplayWidthProperty, tabDisplayHeightProperty);
    }

    /**
     *
     * @param dispManagerRef
     * @param projectName
     * @param tabDisplayWidthProperty
     * @param tabDisplayHeightProperty
     * @throws ProjectNotLoadedException
     */
    public ProjectTab(DisplayManager dispManagerRef, String projectName,
        ReadOnlyDoubleProperty tabDisplayWidthProperty,
        ReadOnlyDoubleProperty tabDisplayHeightProperty) throws ProjectNotLoadedException
    {
        project = ProjectManager.loadProject(projectName);

        if (project != null)
        {
            // No need to tell the PM that this is open - since the list came from the PM in the first place
            displayManager = dispManagerRef;
            initialise(tabDisplayWidthProperty, tabDisplayHeightProperty);
        } else
        {
            throw new ProjectNotLoadedException(projectName);
        }
    }

    /**
     *
     * @param dispManagerRef
     * @param inboundProject
     * @param tabDisplayWidthProperty
     * @param tabDisplayHeightProperty
     */
    public ProjectTab(DisplayManager dispManagerRef, Project inboundProject,
        ReadOnlyDoubleProperty tabDisplayWidthProperty,
        ReadOnlyDoubleProperty tabDisplayHeightProperty)
    {
        project = inboundProject;
        displayManager = dispManagerRef;
        initialise(tabDisplayWidthProperty, tabDisplayHeightProperty);
    }

    private void initialise(ReadOnlyDoubleProperty tabDisplayWidthProperty,
        ReadOnlyDoubleProperty tabDisplayHeightProperty)
    {
        nonEditableProjectNameField.getStyleClass().add("nonEditableProjectTab");
        editableProjectNameField.getStyleClass().add("editableProjectTab");
        editableProjectNameField.setDirectorySafeName(true);
        editableProjectNameField.setRestrict(" -_0-9a-zA-Z\\p{L}\\p{M}*+");
        editableProjectNameField.setMaxLength(25);

        setOnCloseRequest((Event t) ->
        {
            steno.info("Beginning save");
            saveProject();
            projectManager.projectClosed(project);
            steno.info("Completed save");
        });

        viewManager = new ThreeDViewManager(project.getLoadedModels(),
                                            tabDisplayWidthProperty,
                                            tabDisplayHeightProperty);
//        camera = viewManager.getCamera();

        basePane = new AnchorPane();
        basePane.getStyleClass().add("project-view-background");

        basePane.setOnDragOver(new EventHandler<DragEvent>()
        {
            @Override
            public void handle(DragEvent event)
            {
                if (ApplicationStatus.getInstance().modeProperty().getValue() == ApplicationMode.LAYOUT)
                {
                    if (event.getGestureSource() != basePane)
                    {
                        Dragboard dragboard = event.getDragboard();
                        if (dragboard.hasFiles())
                        {
                            List<File> fileList = dragboard.getFiles();
                            boolean accept = true;
                            for (File file : fileList)
                            {
                                boolean extensionFound = false;
                                for (String extension : ApplicationConfiguration.getSupportedFileExtensions(
                                    project.getProjectMode()))
                                {
                                    if (file.getName().toUpperCase().endsWith(extension.toUpperCase()))
                                    {
                                        extensionFound = true;
                                        break;
                                    }
                                }

                                if (!extensionFound)
                                {
                                    accept = false;
                                    break;
                                }
                            }

                            if (accept)
                            {
                                event.acceptTransferModes(TransferMode.COPY);
                            }
                        }
                    }
                }

                event.consume();
            }
        });

        basePane.setOnDragEntered(new EventHandler<DragEvent>()
        {
            public void handle(DragEvent event)
            {
                /* the drag-and-drop gesture entered the target */
                /* show to the user that it is an actual gesture target */
                if (ApplicationStatus.getInstance().modeProperty().getValue() == ApplicationMode.LAYOUT)
                {
                    if (event.getGestureSource() != basePane)
                    {
                        Dragboard dragboard = event.getDragboard();
                        if (dragboard.hasFiles())
                        {
                            List<File> fileList = dragboard.getFiles();
                            boolean accept = true;
                            for (File file : fileList)
                            {
                                boolean extensionFound = false;
                                for (String extension : ApplicationConfiguration.getSupportedFileExtensions(
                                    project.getProjectMode()))
                                {
                                    if (file.getName().endsWith(extension))
                                    {
                                        extensionFound = true;
                                        break;
                                    }
                                }

                                if (!extensionFound)
                                {
                                    accept = false;
                                    break;
                                }
                            }

                            if (accept)
                            {
                                basePane.setEffect(new Glow());
                            }
                        }
                    }
                }
                event.consume();
            }
        });

        basePane.setOnDragExited(new EventHandler<DragEvent>()
        {
            public void handle(DragEvent event)
            {
                /* mouse moved away, remove the graphical cues */
                basePane.setEffect(null);

                event.consume();
            }
        });

        basePane.setOnDragDropped(new EventHandler<DragEvent>()
        {
            @Override
            public void handle(DragEvent event)
            {
                /* data dropped */
                steno.info("onDragDropped");
                /* if there is a string data on dragboard, read it and use it */
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles())
                {
                    displayManager.loadExternalModels(db.getFiles(), true);
                } else
                {
                    steno.error("No files in dragboard");
                }
                /* let the source know whether the string was successfully 
                 * transferred and used */
                event.setDropCompleted(success);

                event.consume();
            }
        });

        basePane.getChildren().add(viewManager.getSubScene());

        try
        {
            URL gcodeEditorURL = getClass().getResource(
                ApplicationConfiguration.fxmlResourcePath
                + "GCodeEditorPanel.fxml");
            FXMLLoader gcodeEditorLoader = new FXMLLoader(gcodeEditorURL,
                                                          Lookup.getLanguageBundle());
            StackPane gcodeEditor = (StackPane) gcodeEditorLoader.load();
            GCodeEditorPanelController gcodeEditorController = gcodeEditorLoader.getController();
            gcodeEditorController.configure(viewManager.getLoadedModels(),
                                            project);
            AnchorPane.setTopAnchor(gcodeEditor, 30.0);
            AnchorPane.setRightAnchor(gcodeEditor, 0.0);

            basePane.getChildren().add(gcodeEditor);
        } catch (IOException ex)
        {
            steno.error("Failed to load gcode editor:" + ex);
        }

        this.setContent(basePane);

        this.setGraphic(nonEditableProjectNameField);
        nonEditableProjectNameField.textProperty().bind(
            project.projectNameProperty());

        nonEditableProjectNameField.setOnMouseClicked((MouseEvent event) ->
        {
            if (event.getClickCount() == 2 && project.getProjectMode()
                != ProjectMode.GCODE)
            {
                editableProjectNameField.setText(
                    nonEditableProjectNameField.getText());
                setGraphic(editableProjectNameField);
                editableProjectNameField.selectAll();
                editableProjectNameField.requestFocus();
                titleBeingEdited = true;
            }
        });

        editableProjectNameField.focusedProperty().addListener(
            new ChangeListener<Boolean>()
            {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov,
                    Boolean t, Boolean t1)
                {
                    if (t1.booleanValue() == false)
                    {
                        switchToNonEditableTitle();
                    }
                }
            });

        editableProjectNameField.setOnAction((ActionEvent event) ->
        {
            switchToNonEditableTitle();
        });

    }

    private void switchToNonEditableTitle()
    {
        if (titleBeingEdited == true)
        {
            projectManager.projectClosed(project);
            String newProjectName = editableProjectNameField.getText();
            Set<String> currentProjectNames = projectManager.getOpenAndAvailableProjectNames();
            newProjectName = suggestNonDuplicateName(newProjectName, currentProjectNames);
            project.setProjectName(newProjectName);
            projectManager.projectOpened(project);
            setGraphic(nonEditableProjectNameField);
            titleBeingEdited = false;
        }
    }

    /**
     *
     * @param projectToLoad
     */
    public void addProjectContainer(File projectToLoad)
    {
        nonEditableProjectNameField.textProperty().unbind();
        nonEditableProjectNameField.setText("");

        project = ProjectManager.loadProject(projectToLoad);
        nonEditableProjectNameField.textProperty().bind(
            project.projectNameProperty());
        viewManager.setLoadedModels(project.getLoadedModels());

        projectManager.projectOpened(project);
    }

    /**
     *
     * @param fullFilename
     * @param modelContainer
     */
    public void addModelContainer(String fullFilename, ModelContainer modelContainer)
    {
        steno.info("I am loading " + fullFilename);
        if (project.getProjectMode() == ProjectMode.NONE)
        {
            switch (modelContainer.getModelContentsType())
            {
                case GCODE:
                    project.setProjectMode(ProjectMode.GCODE);
                    project.setProjectName(modelContainer.getModelName());
                    project.setGCodeFilename(fullFilename);
                    viewManager.activateGCodeVisualisationMode();
                    break;
                case MESH:
                    project.setProjectMode(ProjectMode.MESH);
                    projectManager.projectOpened(project);
                    break;
                default:
                    break;
            }
        }

        if ((project.getProjectMode() == ProjectMode.GCODE
            && modelContainer.getModelContentsType()
            == ModelContentsEnumeration.GCODE)
            || (project.getProjectMode() == ProjectMode.MESH
            && modelContainer.getModelContentsType()
            == ModelContentsEnumeration.MESH))
        {
            viewManager.addModel(modelContainer);
            viewManager.selectModel(modelContainer, false);
        } else
        {
            steno.warning("Discarded load of " + modelContainer.getModelName()
                + " due to conflict with project type");
        }
    }

    /**
     *
     * @param modelMesh
     */
    public void removeModel(ModelContainer modelMesh)
    {
        viewManager.removeModel(modelMesh);
    }

    /**
     *
     */
    public void deleteSelectedModels()
    {
        viewManager.deleteSelectedModels();
    }

    /**
     *
     */
    public void copySelectedModels()
    {
        viewManager.copySelectedModels();
    }

    /**
     *
     * @return
     */
    public ObservableList<ModelContainer> getLoadedModels()
    {
        return viewManager.getLoadedModels();
    }

    /**
     *
     */
    public void autoLayout()
    {
        Collections.sort(viewManager.getLoadedModels());
        PackingThing thing = new PackingThing((int) PrintBed.maxPrintableXSize,
                                              (int) PrintBed.maxPrintableZSize);

        thing.reference(viewManager.getLoadedModels(), 10);
        thing.pack();
        thing.relocateBlocks();

        viewManager.collideModels();

    }

    /**
     *
     */
    public void saveProject()
    {
        //Only save if there are some models and we aren't showing a GCODE project ...

        if (project.getProjectMode() == ProjectMode.MESH)
        {
            if (viewManager.getLoadedModels().size() > 0)
            {
                try
                {
                    ObjectOutputStream out = new ObjectOutputStream(
                        new FileOutputStream(
                            project.getProjectHeader().getProjectPath()
                            + File.separator + project.getProjectName()
                            + ApplicationConfiguration.projectFileExtension));
                    out.writeObject(project);
                    out.close();
                } catch (FileNotFoundException ex)
                {
                    steno.error("Failed to save project state");
                } catch (IOException ex)
                {
                    steno.error(
                        "Couldn't write project state to file for project "
                        + project.getUUID());
                }
            }
        }

        viewManager.shutdown();
    }

    /**
     *
     * @return
     */
    public ThreeDViewManager getThreeDViewManager()
    {
        return viewManager;
    }

    /**
     *
     * @return
     */
    public Project getProject()
    {
        return project;
    }

    /**
     *
     * @param cameraPositionPreset
     */
    public void switchToPresetCameraView(
        CameraPositionPreset cameraPositionPreset)
    {
//        viewManager.getCamera().gotoPreset(cameraPositionPreset);
    }

    /**
     *
     * @param selectedModel
     */
    public void deselectModel(ModelContainer selectedModel)
    {
        viewManager.deselectModel(selectedModel);
    }

//    private void recentreGizmoX(int screenX)
//    {
//        Point2D newPosition = basePane.screenToLocal(screenX, 0);
//        gizmoXform.setTx(newPosition.getX());
//        steno.info("New X pos " + newPosition.getX() + " for " + screenX);
//    }
//
//    private void recentreGizmoY(int screenY)
//    {
//        Point2D newPosition = basePane.screenToLocal(0, screenY);
//        gizmoXform.setTy(newPosition.getY());
//        steno.info("New Y pos " + newPosition.getY() + " for " + screenY);
//    }
    /**
     *
     * @param newMode
     */
    public void setMode(ApplicationMode newMode)
    {
        switch (newMode)
        {
            case LAYOUT:
                //stop rotation
                viewManager.stopSettingsAnimation();
                break;
            case SETTINGS:
                //start rotation
                viewManager.deselectAllModels();
                viewManager.startSettingsAnimation();
                break;
        }
    }

    public SelectedModelContainers getSelectionModel()
    {
        return viewManager.getSelectedModelContainers();
    }

    public void selectAllModels()
    {
        viewManager.selectAllModels();
    }
}
