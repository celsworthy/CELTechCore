package celtech.coreUI.components;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.ModelContainerProject;
import celtech.appManager.Project;
import celtech.appManager.ProjectCallback;
import celtech.appManager.ProjectManager;
import celtech.appManager.ProjectMode;
import celtech.appManager.SVGProject;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.coreUI.visualisation.BedAxes;
import celtech.coreUI.visualisation.DimensionLineManager;
import celtech.coreUI.visualisation.DragMode;
import celtech.coreUI.visualisation.ModelLoader;
import celtech.coreUI.visualisation.SVGViewManager;
import celtech.coreUI.visualisation.ThreeDViewManager;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.printerControl.model.Printer;
import static celtech.roboxbase.utils.DeDuplicator.suggestNonDuplicateName;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.effect.Glow;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ProjectTab extends Tab implements ProjectCallback
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            ProjectTab.class.getName());

    private final Label nonEditableProjectNameField = new Label();
    private final RestrictedTextField editableProjectNameField = new RestrictedTextField();
    private Project project = null;
    private Pane baseContainer = null;
    private AnchorPane basePane = null;
    private AnchorPane overlayPane = null;
    private ThreeDViewManager viewManager = null;
    private SVGViewManager svgViewManager = null;
    private final ProjectManager projectManager = ProjectManager.getInstance();
    private boolean titleBeingEdited = false;
    private final ModelLoader modelLoader = new ModelLoader();
    private DimensionLineManager dimensionLineManager = null;
    private BedAxes bedAxes = null;
    private ZCutEntryBox zCutEntryBox = null;
    private ObjectProperty<LayoutSubmode> layoutSubmode;
    private ProjectAwareController projectAwareController = null;

    private ReadOnlyDoubleProperty tabDisplayWidthProperty;
    private ReadOnlyDoubleProperty tabDisplayHeightProperty;

    private Node modelActionsInsetPanel;

    private BooleanProperty hideDimensions = new SimpleBooleanProperty(false);

    private VBox nonSpecificModelIndicator = new VBox();

    public ProjectTab(
            ReadOnlyDoubleProperty tabDisplayWidthProperty,
            ReadOnlyDoubleProperty tabDisplayHeightProperty)
    {
        this.tabDisplayWidthProperty = tabDisplayWidthProperty;
        this.tabDisplayHeightProperty = tabDisplayHeightProperty;
        coreInitialisation();
    }

    public ProjectTab(Project inboundProject,
            ReadOnlyDoubleProperty tabDisplayWidthProperty,
            ReadOnlyDoubleProperty tabDisplayHeightProperty)
    {
        project = inboundProject;
        this.tabDisplayWidthProperty = tabDisplayWidthProperty;
        this.tabDisplayHeightProperty = tabDisplayHeightProperty;
        coreInitialisation();
        initialiseWithProject();
    }

    private void coreInitialisation()
    {
        setOnClosed((Event t) ->
        {
            steno.debug("Beginning project save");
            saveAndCloseProject();
            steno.debug("Completed project save");
        });

        setOnSelectionChanged((Event t) ->
        {
            if (bedAxes != null)
            {
                bedAxes.updateArrowAndTextPosition();
            }
        });

        AnchorPane.setBottomAnchor(nonSpecificModelIndicator, 0.0);
        AnchorPane.setTopAnchor(nonSpecificModelIndicator, 0.0);
        AnchorPane.setLeftAnchor(nonSpecificModelIndicator, 0.0);
        AnchorPane.setRightAnchor(nonSpecificModelIndicator, 0.0);
        nonSpecificModelIndicator.setAlignment(Pos.CENTER);
        nonSpecificModelIndicator.setMouseTransparent(true);
        nonSpecificModelIndicator.setPickOnBounds(false);
        Label loadAModel = new Label(BaseLookup.i18n("projectTab.loadAModel"));
        loadAModel.getStyleClass().add("load-a-model-text");
        nonSpecificModelIndicator.getChildren().add(loadAModel);

        baseContainer = new Pane();

        basePane = new AnchorPane();
        basePane.getStyleClass().add("project-view-background");

        overlayPane = new AnchorPane();
        overlayPane.setMouseTransparent(true);
        overlayPane.setPickOnBounds(false);

        basePane.getChildren().add(nonSpecificModelIndicator);

        setupDragHandlers();

        this.setContent(basePane);

        this.setGraphic(nonEditableProjectNameField);
    }

    private void initialiseWithProject()
    {

        VBox rhInsetContainer = new VBox();
        rhInsetContainer.setSpacing(30);
        Node settingsInsetPanel = loadInsetPanel("settingsInsetPanel.fxml", project);
        Node timeCostInsetPanel = loadInsetPanel("timeCostInsetPanel.fxml", project);
        rhInsetContainer.getChildren().addAll(timeCostInsetPanel, settingsInsetPanel);

        rhInsetContainer.mouseTransparentProperty().bind(ApplicationStatus.getInstance().modeProperty().isNotEqualTo(ApplicationMode.SETTINGS));

        modelActionsInsetPanel = loadInsetPanel("modelEditInsetPanel.fxml", project);

        VBox dimensionContainer = new VBox();
        dimensionContainer.setMouseTransparent(true);
        AnchorPane.setBottomAnchor(dimensionContainer, 0.0);
        AnchorPane.setTopAnchor(dimensionContainer, 0.0);
        AnchorPane.setRightAnchor(dimensionContainer, 0.0);
        AnchorPane.setLeftAnchor(dimensionContainer, 0.0);

        basePane.getChildren().addAll(rhInsetContainer, modelActionsInsetPanel);

        dimensionLineManager = new DimensionLineManager(basePane, project, hideDimensions);

        layoutSubmode = Lookup.getProjectGUIState(project).getLayoutSubmodeProperty();

        layoutSubmode.addListener(new ChangeListener<LayoutSubmode>()
        {
            @Override
            public void changed(ObservableValue<? extends LayoutSubmode> observable, LayoutSubmode oldValue, LayoutSubmode newValue)
            {
                if (newValue == LayoutSubmode.Z_CUT)
                {
                    Set<ProjectifiableThing> selectedModelContainers
                            = Lookup.getProjectGUIState(project).getProjectSelection().getSelectedModelsSnapshot();
                    if (project instanceof ModelContainerProject)
                    {
                        zCutEntryBox.prime((ModelContainer) selectedModelContainers.iterator().next());
                        overlayPane.getChildren().add(zCutEntryBox);
                    }
                } else
                {
                    if (overlayPane.getChildren().contains(zCutEntryBox))
                    {
                        overlayPane.getChildren().remove(zCutEntryBox);
                    }
                }
            }
        });

        settingsInsetPanel.setVisible(
                false);
        timeCostInsetPanel.setVisible(
                false);
        AnchorPane.setTopAnchor(rhInsetContainer,
                30.0);
        AnchorPane.setRightAnchor(rhInsetContainer,
                30.0);
        AnchorPane.setTopAnchor(modelActionsInsetPanel,
                30.0);
        AnchorPane.setLeftAnchor(modelActionsInsetPanel,
                30.0);

        setupNameFields();

        if (project instanceof ModelContainerProject)
        {
            setup3DView();
        } else if (project instanceof SVGProject)
        {
            setupSVGView();
        }
        
        fireProjectSelected();
        
        projectManager.projectOpened(project);
    }

    private void setup3DView()
    {
        nonSpecificModelIndicator.setVisible(false);
        viewManager = new ThreeDViewManager((ModelContainerProject) project,
                tabDisplayWidthProperty,
                tabDisplayHeightProperty);

        modelActionsInsetPanel.mouseTransparentProperty().bind(viewManager.getDragModeProperty().isNotEqualTo(DragMode.IDLE));

        zCutEntryBox = new ZCutEntryBox(overlayPane, layoutSubmode, viewManager, (ModelContainerProject) project);
        bedAxes = new BedAxes(viewManager);
        viewManager.addCameraViewChangeListener(bedAxes);

        basePane.getChildren().add(0, viewManager.getSubScene());
        overlayPane.getChildren().add(bedAxes);

        hideDimensions.bind(viewManager.getDragModeProperty().isNotEqualTo(DragMode.IDLE));

    }

    private void setupSVGView()
    {
        nonSpecificModelIndicator.setVisible(false);
        svgViewManager = new SVGViewManager(project, basePane.getWidth(), basePane.getHeight());

        AnchorPane.setBottomAnchor(svgViewManager, 0.0);
        AnchorPane.setTopAnchor(svgViewManager, 0.0);
        AnchorPane.setLeftAnchor(svgViewManager, 0.0);
        AnchorPane.setRightAnchor(svgViewManager, 0.0);

        basePane.getChildren().add(0, svgViewManager);
    }

    private Node loadInsetPanel(String innerPanelFXMLName, Project project)
    {
        URL settingsInsetPanelURL = getClass().getResource(
                ApplicationConfiguration.fxmlPanelResourcePath + innerPanelFXMLName);
        FXMLLoader loader = new FXMLLoader(settingsInsetPanelURL, BaseLookup.getLanguageBundle());
        Node insetPanel = null;
        try
        {
            insetPanel = loader.load();
            projectAwareController = (ProjectAwareController) loader.getController();
            projectAwareController.setProject(project);
        } catch (IOException ex)
        {
            steno.error("Unable to load inset panel: " + innerPanelFXMLName + "  " + ex);
        }
        return insetPanel;
    }

    private void setupNameFields()
    {
        nonEditableProjectNameField.getStyleClass().add("nonEditableProjectTab");
        editableProjectNameField.getStyleClass().add("editableProjectTab");
        editableProjectNameField.setDirectorySafeName(true);
        editableProjectNameField.setRestrict(" -_0-9a-zA-Z\\p{L}\\p{M}*+");
        editableProjectNameField.setMaxLength(25);

        nonEditableProjectNameField.textProperty().bind(
                project.projectNameProperty());

        nonEditableProjectNameField.setOnMouseClicked((MouseEvent event) ->
        {
            if (event.getClickCount() == 2)
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
                        if (!t1)
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

    private void setupDragHandlers()
    {
        basePane.setOnDragOver(new EventHandler<DragEvent>()
        {
            @Override
            public void handle(DragEvent event)
            {
                if (ApplicationStatus.getInstance().modeProperty().getValue()
                        == ApplicationMode.LAYOUT)
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
                                ProjectMode projectMode = ProjectMode.NONE;
                                if (project != null)
                                {
                                    projectMode = project.getMode();
                                }
                                List<String> extensions = ApplicationConfiguration.
                                        getSupportedFileExtensions(projectMode);

                                for (String extension : extensions)
                                {
                                    if (file.getName().toUpperCase().endsWith(
                                            extension.toUpperCase()))
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
                                event.consume();
                            }
                        }
                    }
                }
            }
        });

        basePane.setOnDragEntered(new EventHandler<DragEvent>()
        {
            public void handle(DragEvent event)
            {
                /* the drag-and-drop gesture entered the target */
                /* show to the user that it is an actual gesture target */
                if (ApplicationStatus.getInstance().modeProperty().getValue()
                        == ApplicationMode.LAYOUT)
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
                                ProjectMode projectMode = ProjectMode.NONE;
                                if (project != null)
                                {
                                    projectMode = project.getMode();
                                }
                                List<String> extensions = ApplicationConfiguration.
                                        getSupportedFileExtensions(projectMode);
                                for (String extension : extensions)
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
                                event.consume();
                            }
                        }
                    }
                }
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

        basePane.setOnDragDropped((DragEvent event) ->
        {
            boolean success = false;
            if (event.getGestureTarget() == basePane)
            {
                /* data dropped */
                steno.debug("onDragDropped");
                /* if there is a string data on dragboard, read it and use it */
                Dragboard db = event.getDragboard();
                if (db.hasFiles())
                {
                    modelLoader.loadExternalModels(project, db.getFiles(), true, this, false);
                } else
                {
                    steno.error("No files in dragboard");
                }
                /* let the source know whether the string was successfully
                 * transferred and used */
                event.setDropCompleted(success);

                event.consume();
            }
            /* let the source know whether the string was successfully
             * transferred and used */
            event.setDropCompleted(success);

            event.consume();
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

    public void saveAndCloseProject()
    {
        if (viewManager != null)
        {
            viewManager.shutdown();
        }
        Project.saveProject(project);
        projectAwareController.setProject(null);
        projectManager.projectClosed(project);
        project = null;
    }

    public void fireProjectSelected()
    {
        if (project != null)
        {
            Lookup.setSelectedProject(project);
        }
    }

    @Override
    public void modelAddedToProject(Project project)
    {
        if (this.project == null)
        {
            this.project = project;
            initialiseWithProject();
        }
    }

    public void initialiseBlank3DProject()
    {
        if (this.project == null)
        {
            ModelContainerProject newProject = new ModelContainerProject();
            this.project = newProject;
            initialiseWithProject();
        }
    }

    public void initialiseBlank2DProject()
    {
        if (this.project == null)
        {
            SVGProject newProject = new SVGProject();
            this.project = newProject;
            initialiseWithProject();
        }
    }
}
