package celtech.coreUI.components;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.ProjectManager;
import celtech.appManager.ProjectMode;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.coreUI.visualisation.BedAxes;
import celtech.coreUI.visualisation.DimensionLineManager;
import celtech.coreUI.visualisation.DragMode;
import celtech.coreUI.visualisation.ModelLoader;
import celtech.coreUI.visualisation.ThreeDViewManager;
import celtech.modelcontrol.ModelContainer;
import static celtech.utils.DeDuplicator.suggestNonDuplicateName;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
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
public class ProjectTab extends Tab
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
    private final ProjectManager projectManager = ProjectManager.getInstance();
    private boolean titleBeingEdited = false;
    private final ModelLoader modelLoader = new ModelLoader();
    private DimensionLineManager dimensionLineManager = null;
    private BedAxes bedAxes = null;
    private ZCutEntryBox zCutEntryBox = null;
    private ObjectProperty<LayoutSubmode> layoutSubmode;
    private ProjectAwareController projectAwareController = null;

    public ProjectTab(
            ReadOnlyDoubleProperty tabDisplayWidthProperty,
            ReadOnlyDoubleProperty tabDisplayHeightProperty)
    {
        project = new Project();
        projectManager.projectOpened(project);
        initialise(tabDisplayWidthProperty, tabDisplayHeightProperty);
    }

    public ProjectTab(Project inboundProject,
            ReadOnlyDoubleProperty tabDisplayWidthProperty,
            ReadOnlyDoubleProperty tabDisplayHeightProperty)
    {
        project = inboundProject;
        projectManager.projectOpened(project);
        initialise(tabDisplayWidthProperty, tabDisplayHeightProperty);
    }

    private void initialise(ReadOnlyDoubleProperty tabDisplayWidthProperty,
            ReadOnlyDoubleProperty tabDisplayHeightProperty)
    {
        setOnClosed((Event t) ->
        {
            steno.debug("Beginning project save");
            saveAndCloseProject();
            steno.debug("Completed project save");
        });

        setOnSelectionChanged((Event t) ->
        {
            bedAxes.updateArrowAndTextPosition();
        });

        viewManager = new ThreeDViewManager(project,
                tabDisplayWidthProperty,
                tabDisplayHeightProperty);

        VBox rhInsetContainer = new VBox();
        rhInsetContainer.setSpacing(30);
        Node settingsInsetPanel = loadInsetPanel("settingsInsetPanel.fxml", project);
        Node timeCostInsetPanel = loadInsetPanel("timeCostInsetPanel.fxml", project);
        rhInsetContainer.getChildren().addAll(timeCostInsetPanel, settingsInsetPanel);

        rhInsetContainer.mouseTransparentProperty().bind(ApplicationStatus.getInstance().modeProperty().isNotEqualTo(ApplicationMode.SETTINGS));

        Node modelActionsInsetPanel = loadInsetPanel("modelEditInsetPanel.fxml", project);
        modelActionsInsetPanel.mouseTransparentProperty().bind(viewManager.getDragModeProperty().isNotEqualTo(DragMode.IDLE));

        baseContainer = new Pane();

        basePane = new AnchorPane();
        basePane.getStyleClass().add("project-view-background");
        basePane.prefWidthProperty().bind(tabDisplayWidthProperty);
        basePane.prefHeightProperty().bind(tabDisplayHeightProperty);
        basePane.maxWidth(Region.USE_PREF_SIZE);
        basePane.maxHeight(Region.USE_PREF_SIZE);

        overlayPane = new AnchorPane();
        overlayPane.setMouseTransparent(true);
        overlayPane.setPickOnBounds(false);

        setupDragHandlers();

        bedAxes = new BedAxes(viewManager);

        VBox dimensionContainer = new VBox();
        dimensionContainer.setMouseTransparent(true);
        AnchorPane.setBottomAnchor(dimensionContainer, 0.0);
        AnchorPane.setTopAnchor(dimensionContainer, 0.0);
        AnchorPane.setRightAnchor(dimensionContainer, 0.0);
        AnchorPane.setLeftAnchor(dimensionContainer, 0.0);

        basePane.getChildren().addAll(viewManager.getSubScene(), rhInsetContainer, modelActionsInsetPanel);
        overlayPane.getChildren().addAll(bedAxes);

        setupDragHandlers();
        dimensionLineManager = new DimensionLineManager(basePane, project, viewManager.getDragModeProperty());
        viewManager.addCameraViewChangeListener(bedAxes);

        layoutSubmode = Lookup.getProjectGUIState(project).getLayoutSubmodeProperty();
        zCutEntryBox = new ZCutEntryBox(overlayPane, layoutSubmode, viewManager, project);

        layoutSubmode.addListener(new ChangeListener<LayoutSubmode>()
        {
            @Override
            public void changed(ObservableValue<? extends LayoutSubmode> observable, LayoutSubmode oldValue, LayoutSubmode newValue)
            {
                if (newValue == LayoutSubmode.Z_CUT)
                {
                    Set<ModelContainer> selectedModelContainers
                            = Lookup.getProjectGUIState(project).getProjectSelection().getSelectedModelsSnapshot();
                    zCutEntryBox.prime(selectedModelContainers.iterator().next());
                    overlayPane.getChildren().add(zCutEntryBox);
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

        baseContainer.getChildren().addAll(basePane, overlayPane);
        this.setContent(baseContainer);

        this.setGraphic(nonEditableProjectNameField);

        setupNameFields();
        
    }

    private Node loadInsetPanel(String innerPanelFXMLName, Project project)
    {
        URL settingsInsetPanelURL = getClass().getResource(
                ApplicationConfiguration.fxmlPanelResourcePath + innerPanelFXMLName);
        FXMLLoader loader = new FXMLLoader(settingsInsetPanelURL, Lookup.getLanguageBundle());
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
                                for (String extension : ApplicationConfiguration.
                                        getSupportedFileExtensions(
                                                ProjectMode.MESH))
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
                                for (String extension : ApplicationConfiguration.
                                        getSupportedFileExtensions(
                                                ProjectMode.MESH))
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
                                overlayPane.setEffect(new Glow());
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
                    modelLoader.loadExternalModels(project, db.getFiles(), true);
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
            String newProjectName = editableProjectNameField.getText();
            Set<String> currentProjectNames = projectManager.getOpenAndAvailableProjectNames();
            newProjectName = suggestNonDuplicateName(newProjectName, currentProjectNames);
            project.setProjectName(newProjectName);
            setGraphic(nonEditableProjectNameField);
            titleBeingEdited = false;
        }
    }

    public void saveAndCloseProject()
    {
        viewManager.shutdown();
        Project.saveProject(project);
        projectAwareController.setProject(null);
        projectManager.projectClosed(project);
        project = null;
    }

    public void fireProjectSelected()
    {
        Lookup.setSelectedProject(project);
    }
}
