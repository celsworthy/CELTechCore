/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.components;

import celtech.appManager.ApplicationMode;
import celtech.appManager.Project;
import celtech.appManager.ProjectManager;
import celtech.appManager.ProjectMode;
import celtech.appManager.UndoBuffer;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.GCodeEditorPanelController;
import celtech.coreUI.controllers.GizmoOverlayController;
import celtech.coreUI.visualisation.CameraPositionPreset;
import celtech.coreUI.visualisation.SelectionContainer;
import celtech.coreUI.visualisation.ThreeDViewManager;
import celtech.coreUI.visualisation.Xform;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelContentsEnumeration;
import celtech.utils.Math.MathUtils;
import celtech.utils.Math.Packing.Block;
import celtech.utils.Math.Packing.PackingThing;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.effect.Glow;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
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

    private final Stenographer steno = StenographerFactory.getStenographer(ProjectTab.class.getName());

    private final HBox nonEditableTab = new HBox();
    private final Label nonEditableProjectNameField = new Label();
    private final TextField editableProjectNameField = new TextField();
    private Project project = null;
    private AnchorPane basePane = null;
    private UndoBuffer undoBuffer = new UndoBuffer();
    private ThreeDViewManager viewManager = null;
    private DisplayManager displayManager = null;
    private ProjectManager projectManager = ProjectManager.getInstance();
    private boolean titleBeingEdited = false;
    private Xform gizmoXform = new Xform(Xform.RotateOrder.YXZ);
    private AnchorPane gizmoOverlay = null;
    private final Menu projectMenu = new Menu();
    private final MenuItem projectMenuItem = new MenuItem();

    final Rectangle testRect = new Rectangle(5, 5);

    private ChangeListener<Number> selectionContainerMoveListener = new ChangeListener<Number>()
    {
        @Override
        public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
        {
            Point2D reference = basePane.localToScreen(0, 0);
            double x = viewManager.getSelectionContainer().getScreenX() - reference.getX();
            double y = viewManager.getSelectionContainer().getScreenY() - reference.getY();
            gizmoXform.setTx(x);
            gizmoXform.setTy(y);
        }
    };

    public ProjectTab(DisplayManager dispManagerRef, ReadOnlyDoubleProperty tabDisplayWidthProperty, ReadOnlyDoubleProperty tabDisplayHeightProperty)
    {
        displayManager = dispManagerRef;
        project = new Project();
        initialise(tabDisplayWidthProperty, tabDisplayHeightProperty);
    }

    public ProjectTab(DisplayManager dispManagerRef, String projectName, ReadOnlyDoubleProperty tabDisplayWidthProperty, ReadOnlyDoubleProperty tabDisplayHeightProperty) throws ProjectNotLoadedException
    {
        project = loadProject(projectName);

        if (project != null)
        {
            // No need to tell the PM that this is open - since the list came from the PM in the first place
            displayManager = dispManagerRef;
            initialise(tabDisplayWidthProperty, tabDisplayHeightProperty);
        }
        else
        {
            throw new ProjectNotLoadedException(projectName);
        }
    }

    public ProjectTab(DisplayManager dispManagerRef, Project inboundProject, ReadOnlyDoubleProperty tabDisplayWidthProperty, ReadOnlyDoubleProperty tabDisplayHeightProperty)
    {
        project = inboundProject;
        displayManager = dispManagerRef;
        initialise(tabDisplayWidthProperty, tabDisplayHeightProperty);
    }

    private Project loadProject(String projectNameWithPath)
    {
        Project loadedProject = null;

        try
        {
            FileInputStream projectFile = new FileInputStream(projectNameWithPath);
            ObjectInputStream reader = new ObjectInputStream(projectFile);
            loadedProject = (Project) reader.readObject();
            reader.close();
        } catch (IOException ex)
        {
            steno.error("Failed to load project " + projectNameWithPath);
        } catch (ClassNotFoundException ex)
        {
            steno.error("Couldn't locate class while loading project " + projectNameWithPath);
        }

        return loadedProject;
    }

    private void initialise(ReadOnlyDoubleProperty tabDisplayWidthProperty, ReadOnlyDoubleProperty tabDisplayHeightProperty)
    {
//        projectMenu.getItems().add(projectMenuItem);
//        nonEditableTab.getChildren().add(nonEditableProjectNameField);
//        nonEditableTab.getChildren().add(projectMenu);
        nonEditableProjectNameField.getStyleClass().add("nonEditableProjectTab");
        editableProjectNameField.getStyleClass().add("editableProjectTab");

        setOnCloseRequest((Event t) ->
        {
            steno.info("Beginning save");
            saveProject();
            projectManager.projectClosed(project.getProjectHeader().getProjectName() + project.getProjectName());
            steno.info("Completed save");
        });

        viewManager = new ThreeDViewManager(project, tabDisplayWidthProperty, tabDisplayHeightProperty);
//        camera = viewManager.getCamera();

        basePane = new AnchorPane();
        basePane.getStyleClass().add("project-view-background");

        basePane.setOnDragOver(new EventHandler<DragEvent>()
        {
            @Override
            public void handle(DragEvent event)
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
                            for (String extension : ApplicationConfiguration.getSupportedFileExtensions(project.getProjectMode()))
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
                            event.acceptTransferModes(TransferMode.COPY);
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
                steno.info("onDragEntered");
                /* show to the user that it is an actual gesture target */
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
                            for (String extension : ApplicationConfiguration.getSupportedFileExtensions(project.getProjectMode()))
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
                    displayManager.loadExternalModels(db.getFiles());
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

//        try
//        {
//            URL layoutControlsURL = getClass().getResource(ApplicationConfiguration.fxmlResourcePath + "GizmoOverlay.fxml");
//            FXMLLoader gizmoOverlayLoader = new FXMLLoader(layoutControlsURL, DisplayManager.getLanguageBundle());
//            gizmoOverlay = (AnchorPane) gizmoOverlayLoader.load();
//            GizmoOverlayController gizmoOverlayController = gizmoOverlayLoader.getController();
//            gizmoOverlayController.configure(viewManager);
//            viewManager.associateGizmoOverlayController(gizmoOverlayController);
//
//            gizmoOverlay.setRotationAxis(MathUtils.xAxis);
//            gizmoOverlay.setRotate(90);
//            gizmoXform.getChildren().add(gizmoOverlay);
//            gizmoOverlay.setPickOnBounds(false);
//            basePane.getChildren().add(gizmoXform);
//
//            gizmoXform.setRotateX(viewManager.demandedCameraRotationXProperty().get());
//            gizmoXform.setRotateY(viewManager.demandedCameraRotationYProperty().get());
//
//            viewManager.demandedCameraRotationXProperty().addListener(new ChangeListener<Number>()
//            {
//                @Override
//                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
//                {
//                    gizmoXform.setRotateX(newValue.doubleValue());
//                }
//            });
//
//            viewManager.demandedCameraRotationYProperty().addListener(new ChangeListener<Number>()
//            {
//                @Override
//                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
//                {
//                    gizmoXform.setRotateY(newValue.doubleValue());
//                }
//            });
//
//        } catch (IOException ex)
//        {
//            steno.error("Failed to load 3d Gizmo:" + ex);
//        }
        viewManager.getSelectionContainer().screenXProperty().addListener(selectionContainerMoveListener);
        viewManager.getSelectionContainer().screenYProperty().addListener(selectionContainerMoveListener);

        try
        {
            URL gcodeEditorURL = getClass().getResource(ApplicationConfiguration.fxmlResourcePath + "GCodeEditorPanel.fxml");
            FXMLLoader gcodeEditorLoader = new FXMLLoader(gcodeEditorURL, DisplayManager.getLanguageBundle());
            StackPane gcodeEditor = (StackPane) gcodeEditorLoader.load();
            GCodeEditorPanelController gcodeEditorController = gcodeEditorLoader.getController();
            gcodeEditorController.configure(viewManager.getLoadedModels(), project);
            AnchorPane.setTopAnchor(gcodeEditor, 30.0);
            AnchorPane.setRightAnchor(gcodeEditor, 0.0);

            basePane.getChildren().add(gcodeEditor);
        } catch (IOException ex)
        {
            steno.error("Failed to load gcode editor:" + ex);
        }

        this.setContent(basePane);

        this.setGraphic(nonEditableProjectNameField);
        nonEditableProjectNameField.textProperty().bind(project.projectNameProperty());

        nonEditableProjectNameField.setOnMouseClicked((MouseEvent event) ->
        {
            if (event.getClickCount() == 2 && project.getProjectMode() != ProjectMode.GCODE)
            {
                editableProjectNameField.setText(nonEditableProjectNameField.getText());
                setGraphic(editableProjectNameField);
                editableProjectNameField.selectAll();
                editableProjectNameField.requestFocus();
                titleBeingEdited = true;
            }
        });

        editableProjectNameField.focusedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
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
            projectManager.projectClosed(project.getProjectHeader().getProjectPath() + project.getProjectHeader().getProjectName());
            project.setProjectName(editableProjectNameField.getText());
            projectManager.projectOpened(project.getProjectHeader().getProjectPath() + project.getProjectHeader().getProjectName());
            setGraphic(nonEditableProjectNameField);
            titleBeingEdited = false;
        }
    }

    public void addProjectContainer(String projectName)
    {
        project = loadProject(projectName);
            projectManager.projectOpened(project.getProjectHeader().getProjectPath() + project.getProjectHeader().getProjectName());
        for (ModelContainer model : project.getLoadedModels())
        {
            viewManager.addModel(model);
        }
    }

    public void addModelContainer(String fullFilename, ModelContainer modelGroup)
    {
        steno.info("I am loading " + fullFilename);
        if (project.getProjectMode() == ProjectMode.NONE)
        {
            switch (modelGroup.getModelContentsType())
            {
                case GCODE:
                    project.setProjectMode(ProjectMode.GCODE);
                    project.setProjectName(modelGroup.getModelName());
                    project.setGCodeFilename(fullFilename);
                    viewManager.activateGCodeVisualisationMode();
                    break;
                case MESH:
                    project.setProjectMode(ProjectMode.MESH);
                    projectManager.projectOpened(fullFilename);
                    break;
                default:
                    break;
            }
        }

        if ((project.getProjectMode() == ProjectMode.GCODE && modelGroup.getModelContentsType() == ModelContentsEnumeration.GCODE)
                || (project.getProjectMode() == ProjectMode.MESH && modelGroup.getModelContentsType() == ModelContentsEnumeration.MESH))
        {
            viewManager.addModel(modelGroup);
            displayManager.selectModel(modelGroup);
        } else
        {
            steno.warning("Discarded load of " + modelGroup.getModelName() + " due to conflict with project type");
        }
    }

    public void removeModel(ModelContainer modelMesh)
    {
        viewManager.removeModel(modelMesh);
    }

    public void deleteSelectedModels()
    {
        viewManager.deleteSelectedModels();
    }

    public void copySelectedModels()
    {
        viewManager.copySelectedModels();
    }

    public SelectionContainer getSelectionContainer()
    {
        return viewManager.getSelectionContainer();
    }

    public ObservableList<ModelContainer> getLoadedModels()
    {
        return viewManager.getLoadedModels();
    }

    public void autoLayout()
    {
        Collections.sort(viewManager.getLoadedModels());
        PackingThing thing = new PackingThing(210, 150);

        ArrayList<Block> blocks = new ArrayList<>();

        thing.reference(viewManager.getLoadedModels(), 10);
        thing.pack();
        thing.relocateBlocks();

        viewManager.recalculateSelectionBounds(false);

//        for (Block block : blocks)
//        {
//            System.out.println(">>>>>>>>>>");
//            System.out.println("W:" + block.getW());
//            System.out.println("H:" + block.getH());
//            if (block.getFit() != null)
//            {
//                System.out.println("Fit X:" + block.getFit().getX());
//                System.out.println("Fit Y:" + block.getFit().getY());
//                System.out.println("Fit w:" + block.getFit().getW());
//                System.out.println("Fit h:" + block.getFit().getH());
//            }
//            else
//            {
//                System.out.println("No fit");
//            }
//            block.relocate();
//            System.out.println("<<<<<<<<<<");
//        }
    }

    public void selectModel(ModelContainer selectedModel)
    {
        viewManager.selectModel(selectedModel, false);
    }

    public void saveProject()
    {
        //Only save if there are some models and we aren't showing a GCODE project ...

        if (project.getProjectMode() == ProjectMode.MESH)
        {
            if (viewManager.getLoadedModels().size() > 0)
            {
                try
                {
                    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ApplicationConfiguration.getProjectDirectory() + project.getProjectName() + ApplicationConfiguration.projectFileExtension));
                    out.writeObject(project);
                    out.close();
                } catch (FileNotFoundException ex)
                {
                    steno.error("Failed to save project state");
                } catch (IOException ex)
                {
                    steno.error("Couldn't write project state to file for project " + project.getUUID());
                }
            }
        }

        viewManager.shutdown();
    }

    public ThreeDViewManager getThreeDViewManager()
    {
        return viewManager;
    }

    public Project getProject()
    {
        return project;
    }

    public void switchToPresetCameraView(CameraPositionPreset cameraPositionPreset)
    {
//        viewManager.getCamera().gotoPreset(cameraPositionPreset);
    }

    public void deselectModel(ModelContainer selectedModel)
    {
        viewManager.deselectModel(selectedModel);
    }

    private void recentreGizmoX(int screenX)
    {
        Point2D newPosition = basePane.screenToLocal(screenX, 0);
        gizmoXform.setTx(newPosition.getX());
        steno.info("New X pos " + newPosition.getX() + " for " + screenX);
    }

    private void recentreGizmoY(int screenY)
    {
        Point2D newPosition = basePane.screenToLocal(0, screenY);
        gizmoXform.setTy(newPosition.getY());
        steno.info("New Y pos " + newPosition.getY() + " for " + screenY);
    }

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
                viewManager.startSettingsAnimation();
                break;
        }
    }
}
