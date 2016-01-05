package celtech.coreUI;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.ProjectManager;
import celtech.appManager.undo.CommandStack;
import celtech.appManager.undo.UndoableProject;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.coreUI.components.Notifications.NotificationArea;
import celtech.coreUI.components.ProgressDialog;
import celtech.coreUI.components.ProjectTab;
import celtech.coreUI.components.Spinner;
import celtech.coreUI.components.TopMenuStrip;
import celtech.coreUI.controllers.InfoScreenIndicatorController;
import celtech.coreUI.controllers.PrinterStatusPageController;
import celtech.coreUI.controllers.panels.LibraryMenuPanelController;
import celtech.coreUI.controllers.panels.PurgeInsetPanelController;
import celtech.coreUI.keycommands.HiddenKey;
import celtech.coreUI.keycommands.KeyCommandListener;
import celtech.coreUI.visualisation.ModelLoader;
import celtech.coreUI.visualisation.ProjectSelection;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.comms.RoboxCommsManager;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.effect.Glow;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class DisplayManager implements EventHandler<KeyEvent>, KeyCommandListener, SpinnerControl
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
            DisplayManager.class.getName());

    private static final int START_SCALING_WINDOW_HEIGHT = 770;
    private static final double MINIMUM_SCALE_FACTOR = 0.7;

    private static ApplicationStatus applicationStatus;
    private static ProjectManager projectManager;

    private static DisplayManager instance;
    private static Stage mainStage;
    private static Scene scene;

    private HBox mainHolder;
    private StackPane sidePanelContainer;
    private final HashMap<ApplicationMode, Pane> insetPanels;
    private final AnchorPane rhPanel;
    private final VBox projectTabPaneHolder;
    private final HashMap<ApplicationMode, Initializable> insetPanelControllers;
    private VBox sidePanel;

    private static TabPane tabDisplay;
    private static SingleSelectionModel<Tab> tabDisplaySelectionModel;
    private static Tab printerStatusTab;
    private static Tab addPageTab;
    private Tab lastLayoutTab;

    /*
     * Project loading
     */
    private ProgressDialog modelLoadDialog = null;

    private InfoScreenIndicatorController infoScreenIndicatorController = null;

    private static final String addDummyPrinterCommand = "AddDummy";
    private static final String dummyCommandPrefix = "dummy:";

    private StackPane rootStackPane;
    private AnchorPane rootAnchorPane;
    private Pane spinnerContainer;
    private Spinner spinner;

    //Display scaling
    private BooleanProperty nodesMayHaveMoved;

    public enum DisplayScalingMode
    {

        NORMAL,
        SHORT,
        VERY_SHORT
    }

    private ObjectProperty<DisplayScalingMode> displayScalingModeProperty = new SimpleObjectProperty<>(DisplayScalingMode.NORMAL);
    private final int SHORT_SCALE_BELOW_HEIGHT = 890;
    private final int VERY_SHORT_SCALE_BELOW_HEIGHT = 700;

    private DisplayManager()
    {
        this.rootStackPane = new StackPane();
        this.nodesMayHaveMoved = new SimpleBooleanProperty(false);
        this.insetPanelControllers = new HashMap<>();
        this.insetPanels = new HashMap<>();
        applicationStatus = ApplicationStatus.getInstance();
        projectManager = ProjectManager.getInstance();
        this.projectTabPaneHolder = new VBox();
        AnchorPane.setBottomAnchor(projectTabPaneHolder, 0.0);
        AnchorPane.setTopAnchor(projectTabPaneHolder, 0.0);
        AnchorPane.setLeftAnchor(projectTabPaneHolder, 0.0);
        AnchorPane.setRightAnchor(projectTabPaneHolder, 0.0);
        this.rhPanel = new AnchorPane();
        steno.debug("Starting AutoMaker - initialising display manager...");
//        switch (ApplicationConfiguration.getMachineType())
//        {
//            case LINUX_X64:
//            case LINUX_X86:
//                System.setProperty("prism.lcdtext", "false");
//                break;
//            default:
//                System.setProperty("prism.lcdtext", "true");
//                break;
//        }
        steno.debug("Starting AutoMaker - machine type is " + ApplicationConfiguration.
                getMachineType());
    }

    private void loadProjectsAtStartup()
    {
        steno.debug("start load projects");
        // Load up any projects that were open last time we shut down....
        ProjectManager pm = ProjectManager.getInstance();
        List<Project> preloadedProjects = pm.getOpenProjects();

        for (int projectNumber = preloadedProjects.size() - 1; projectNumber >= 0; projectNumber--)
        {
            Project project = preloadedProjects.get(projectNumber);
            ProjectTab newProjectTab = new ProjectTab(project, tabDisplay.widthProperty(),
                    tabDisplay.heightProperty());
            tabDisplay.getTabs().add(1, newProjectTab);
        }

        if (Lookup.getUserPreferences().isFirstUse())
        {
            steno.debug("get first use stl file");
            File firstUsePrintFile = new File(ApplicationConfiguration.
                    getApplicationModelDirectory().concat("Robox CEL RB robot.stl"));

            Project newProject = new Project();
            newProject.setProjectName(Lookup.i18n("myFirstPrintTitle"));

            List<File> fileToLoad = new ArrayList<>();
            fileToLoad.add(firstUsePrintFile);
            ModelLoader loader = new ModelLoader();
            loader.loadExternalModels(newProject, fileToLoad, false);

            ProjectTab projectTab = new ProjectTab(newProject, tabDisplay.widthProperty(),
                    tabDisplay.heightProperty());
            tabDisplay.getTabs().add(projectTab);

            Lookup.getUserPreferences().setFirstUse(false);
        }
        steno.debug("end load projects");
    }

    public void showAndSelectPrintProfile(SlicerParametersFile printProfile)
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.LIBRARY);
        Initializable initializable = insetPanelControllers.get(ApplicationMode.LIBRARY);
        LibraryMenuPanelController controller = (LibraryMenuPanelController) initializable;
        controller.showAndSelectPrintProfile(printProfile);
    }

    private void switchPagesForMode(ApplicationMode oldMode, ApplicationMode newMode)
    {
        infoScreenIndicatorController.setSelected(newMode == ApplicationMode.STATUS);

        // Remove the existing side panel
        if (oldMode != null)
        {
            Pane lastInsetPanel = insetPanels.get(oldMode);
            if (lastInsetPanel != null)
            {
                rhPanel.getChildren().remove(lastInsetPanel);
            } else
            {
                if (rhPanel.getChildren().contains(projectTabPaneHolder))
                {
                    rhPanel.getChildren().remove(projectTabPaneHolder);
                }
            }
        }

        // Now add the relevant new one...
        Pane newInsetPanel = insetPanels.get(newMode);
        if (newInsetPanel != null)
        {
            AnchorPane.setBottomAnchor(newInsetPanel, 0.0);
            AnchorPane.setTopAnchor(newInsetPanel, 0.0);
            AnchorPane.setLeftAnchor(newInsetPanel, 0.0);
            AnchorPane.setRightAnchor(newInsetPanel, 0.0);
            rhPanel.getChildren().add(0, newInsetPanel);
        }

        if (newMode == ApplicationMode.LAYOUT)
        {
            rhPanel.getChildren().add(0, projectTabPaneHolder);

            ProjectTab projectTab = null;

            //Create a tab if one doesn't already exist
            if (tabDisplay.getTabs().size() <= 1)
            {
                projectTab = new ProjectTab(tabDisplay.widthProperty(),
                        tabDisplay.heightProperty());
                tabDisplay.getTabs().add(projectTab);
                tabDisplaySelectionModel.select(projectTab);
            } else
            {
                //Switch tabs if necessary
                if (tabDisplaySelectionModel.getSelectedItem() instanceof ProjectTab
                        == false)
                {
                    //Select the second tab (first is always status)
                    if (lastLayoutTab != null)
                    {
                        tabDisplaySelectionModel.select(lastLayoutTab);
                    } else
                    {
                        tabDisplaySelectionModel.select(1);
                    }
                }
            }

        } else if (newMode == ApplicationMode.SETTINGS)
        {
            rhPanel.getChildren().add(0, projectTabPaneHolder);
        } else if (newMode == ApplicationMode.STATUS)
        {
            rhPanel.getChildren().add(0, projectTabPaneHolder);
            tabDisplaySelectionModel.select(0);
        }
    }

    public static DisplayManager getInstance()
    {
        if (instance == null)
        {
            try
            {
                instance = new DisplayManager();
            } catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }

        return instance;
    }

    /**
     * Show the spinner, and keep it centred on the given region.
     */
    @Override
    public void startSpinning(Region centreRegion)
    {
        spinner.setVisible(true);
        spinner.startSpinning();
        spinner.setCentreNode(centreRegion);
    }

    /**
     * Stop and hide the spinner.
     */
    @Override
    public void stopSpinning()
    {
        spinner.setVisible(false);
        spinner.stopSpinning();
    }

    public void configureDisplayManager(Stage mainStage, String applicationName)
    {
        steno.debug("start configure display manager");
        this.mainStage = mainStage;
        mainStage.setTitle(applicationName + " - "
                + ApplicationConfiguration.getApplicationVersion());
        ApplicationConfiguration.setTitleAndVersion(Lookup.i18n(
                "application.title")
                + " - " + ApplicationConfiguration.getApplicationVersion());

        rootAnchorPane = new AnchorPane();

        rootStackPane.getChildren().add(rootAnchorPane);

        spinnerContainer = new Pane();
        spinnerContainer.setMouseTransparent(true);
        spinnerContainer.setPickOnBounds(false);
        spinner = new Spinner();
        spinner.setVisible(false);
        spinnerContainer.getChildren().add(spinner);
        Lookup.setSpinnerControl(this);

        AnchorPane.setBottomAnchor(rootAnchorPane, 0.0);
        AnchorPane.setLeftAnchor(rootAnchorPane, 0.0);
        AnchorPane.setRightAnchor(rootAnchorPane, 0.0);
        AnchorPane.setTopAnchor(rootAnchorPane, 0.0);

        mainHolder = new HBox();
        mainHolder.setPrefSize(-1, -1);

        AnchorPane.setBottomAnchor(mainHolder, 0.0);
        AnchorPane.setLeftAnchor(mainHolder, 0.0);
        AnchorPane.setRightAnchor(mainHolder, 0.0);
        AnchorPane.setTopAnchor(mainHolder, 0.0);

        rootAnchorPane.getChildren().add(mainHolder);
        rootAnchorPane.getChildren().add(spinnerContainer);

        // Load in all of the side panels
        steno.debug("setup panels for mode");
        for (ApplicationMode mode : ApplicationMode.values())
        {
            setupPanelsForMode(mode);
        }

        // Create a place to hang the side panels from
        sidePanelContainer = new StackPane();
        HBox.setHgrow(sidePanelContainer, Priority.NEVER);

        try
        {
            URL fxmlFileName = getClass().getResource(ApplicationConfiguration.fxmlPanelResourcePath + "printerStatusSidePanel.fxml");
            steno.debug("About to load side panel fxml: " + fxmlFileName);
            FXMLLoader sidePanelLoader = new FXMLLoader(fxmlFileName, Lookup.getLanguageBundle());
            sidePanel = (VBox) sidePanelLoader.load();
        } catch (Exception ex)
        {
            steno.exception("Couldn't load side panel", ex);
        }
        sidePanelContainer.getChildren().add(sidePanel);

        mainHolder.getChildren().add(sidePanelContainer);

        projectTabPaneHolder.getStyleClass().add("master-details-pane");
        HBox.setHgrow(projectTabPaneHolder, Priority.ALWAYS);

        HBox.setHgrow(rhPanel, Priority.ALWAYS);

        addTopMenuStripController();
        addNotificationArea();

        mainHolder.getChildren().add(rhPanel);

        // Configure the main display tab pane - just the printer status page to start with
        tabDisplay = new TabPane();
        tabDisplay.setPickOnBounds(false);
        tabDisplay.setOnKeyPressed(this);
        tabDisplay.setTabMinHeight(56);
        tabDisplay.setTabMaxHeight(56);
        tabDisplaySelectionModel = tabDisplay.getSelectionModel();
        tabDisplay.getStyleClass().add("main-project-tabPane");
        configureProjectDragNDrop(tabDisplay);

        VBox.setVgrow(tabDisplay, Priority.ALWAYS);

        // The printer status tab will always be visible - the page is static
        try
        {
            FXMLLoader printerStatusPageLoader = new FXMLLoader(getClass().getResource(
                    ApplicationConfiguration.fxmlResourcePath
                    + "PrinterStatusPage.fxml"), Lookup.getLanguageBundle());
            AnchorPane printerStatusPage = printerStatusPageLoader.load();
            PrinterStatusPageController printerStatusPageController = printerStatusPageLoader.
                    getController();
            printerStatusPageController.
                    configure(projectTabPaneHolder);

            printerStatusTab = new Tab();
            FXMLLoader printerStatusPageLabelLoader = new FXMLLoader(getClass().getResource(
                    ApplicationConfiguration.fxmlResourcePath
                    + "infoScreenIndicator.fxml"), Lookup.getLanguageBundle());
            VBox printerStatusLabelGroup = printerStatusPageLabelLoader.load();
            infoScreenIndicatorController = printerStatusPageLabelLoader.getController();
            printerStatusTab.setGraphic(printerStatusLabelGroup);
            printerStatusTab.setClosable(false);
            printerStatusTab.setContent(printerStatusPage);
            tabDisplay.getTabs().add(printerStatusTab);

            tabDisplaySelectionModel.selectedItemProperty().addListener(
                    (ObservableValue<? extends Tab> ov, Tab lastTab, Tab newTab) ->
                    {
                        if (newTab == addPageTab)
                        {
                            createAndAddNewProjectTab();

                            if (applicationStatus.getMode() != ApplicationMode.LAYOUT)
                            {
                                applicationStatus.setMode(ApplicationMode.LAYOUT);
                            }
                        } else if (newTab instanceof ProjectTab)
                        {
                            if (applicationStatus.getMode() != ApplicationMode.LAYOUT)
                            {
                                applicationStatus.setMode(ApplicationMode.LAYOUT);
                            }

                            if (lastTab != newTab)
                            {
                                ProjectTab projectTab = (ProjectTab) tabDisplaySelectionModel.
                                getSelectedItem();
                                projectTab.fireProjectSelected();
                            }
                        } else
                        {
                            if (lastTab instanceof ProjectTab)
                            {
                                lastLayoutTab = lastTab;
                            }
                            //Must have clicked on the status tab
                            if (applicationStatus.getMode() != ApplicationMode.STATUS)
                            {
                                applicationStatus.setMode(ApplicationMode.STATUS);
                            }
                        }
                    });

            projectTabPaneHolder.getChildren().add(tabDisplay);
        } catch (IOException ex)
        {
            steno.exception("Failed to load printer status page", ex);
        }

        applicationStatus.modeProperty().addListener(
                (ObservableValue<? extends ApplicationMode> ov, ApplicationMode oldMode, ApplicationMode newMode) ->
                {
                    switchPagesForMode(oldMode, newMode);
                });

        applicationStatus.setMode(ApplicationMode.STATUS);

        try
        {
            URL menuStripURL = getClass().getResource(ApplicationConfiguration.fxmlPanelResourcePath
                    + "LayoutStatusMenuStrip.fxml");
            FXMLLoader menuStripLoader = new FXMLLoader(menuStripURL, Lookup.getLanguageBundle());
            VBox menuStripControls = (VBox) menuStripLoader.load();
            menuStripControls.prefWidthProperty().bind(projectTabPaneHolder.widthProperty());
            projectTabPaneHolder.getChildren().add(menuStripControls);
        } catch (IOException ex)
        {
            steno.exception("Failed to load menu strip controls", ex);
        }

        modelLoadDialog = new ProgressDialog(ModelLoader.modelLoaderService);

        scene = new Scene(rootStackPane, ApplicationConfiguration.DEFAULT_WIDTH,
                ApplicationConfiguration.DEFAULT_HEIGHT);

        scene.getStylesheets().add(ApplicationConfiguration.getMainCSSFile());

        scene.widthProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(
                    ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                whenWindowChangesSize();
            }
        });

        scene.heightProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(
                    ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                whenWindowChangesSize();
            }
        });

        projectTabPaneHolder.widthProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(
                    ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                whenWindowChangesSize();
            }
        });

        mainStage.maximizedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(
                    ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                whenWindowChangesSize();
            }
        });

        mainStage.fullScreenProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(
                    ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                steno.debug("Stage fullscreen = " + newValue.booleanValue());
                whenWindowChangesSize();
            }
        });

        HiddenKey hiddenKeyThing = new HiddenKey();
        hiddenKeyThing.addCommandSequence(addDummyPrinterCommand);
        hiddenKeyThing.addCommandWithParameterSequence(dummyCommandPrefix);
        hiddenKeyThing.addKeyCommandListener(this);
        hiddenKeyThing.captureHiddenKeys(scene);

        // Camera required to allow 2D shapes to be rotated in 3D in the '2D' UI
        PerspectiveCamera controlOverlaycamera = new PerspectiveCamera(false);

        scene.setCamera(controlOverlaycamera);

        mainStage.setScene(scene);

        steno.debug("load projects");
        loadProjectsAtStartup();

        addPageTab = new Tab();
        addPageTab.setText("+");
        addPageTab.setClosable(false);
        tabDisplay.getTabs().add(addPageTab);

        rootAnchorPane.layout();

        steno.debug("end configure display manager");
    }

    private void setupPanelsForMode(ApplicationMode mode)
    {
        try
        {
            URL fxmlFileName = getClass().getResource(mode.getInsetPanelFXMLName());
            if (fxmlFileName != null)
            {
                steno.debug("About to load inset panel fxml: " + fxmlFileName);
                FXMLLoader insetPanelLoader = new FXMLLoader(fxmlFileName,
                        Lookup.getLanguageBundle());
                insetPanelLoader.setController(mode.getControllerClass().newInstance());
                Pane insetPanel = (Pane) insetPanelLoader.load();
                Initializable insetPanelController = insetPanelLoader.getController();
                insetPanel.setId(mode.name());
                insetPanels.put(mode, insetPanel);
                insetPanelControllers.put(mode, insetPanelController);
            }
        } catch (Exception ex)
        {
            insetPanels.put(mode, null);
            insetPanelControllers.put(mode, null);
            steno.exception("Couldn't load inset panel for mode:" + mode, ex);
        }
    }

    private void addTopMenuStripController()
    {
        HBox topMenuStrip = new TopMenuStrip();
        AnchorPane.setTopAnchor(topMenuStrip, 0.0);
        AnchorPane.setLeftAnchor(topMenuStrip, 0.0);
        AnchorPane.setRightAnchor(topMenuStrip, 0.0);
        rhPanel.getChildren().add(topMenuStrip);
    }

    private void addNotificationArea()
    {
        NotificationArea notificationArea = new NotificationArea();
        AnchorPane.setBottomAnchor(notificationArea, 90.0);
        AnchorPane.setLeftAnchor(notificationArea, 0.0);
        AnchorPane.setRightAnchor(notificationArea, 0.0);
        rhPanel.getChildren().add(notificationArea);
    }

    private ProjectTab createAndAddNewProjectTab()
    {
        ProjectTab projectTab = new ProjectTab(tabDisplay.widthProperty(),
                tabDisplay.heightProperty());
        tabDisplay.getTabs().add(tabDisplay.getTabs().size() - 1, projectTab);
        tabDisplaySelectionModel.select(projectTab);
        return projectTab;
    }

    public static Stage getMainStage()
    {
        return mainStage;
    }

    public void shutdown()
    {
        if (projectManager != null)
        {
            projectManager.saveState();
        }

        if (tabDisplay != null)
        {
            tabDisplay.getTabs().stream().filter((tab) -> (tab instanceof ProjectTab)).forEach(
                    (tab) ->
                    {
                        ((ProjectTab) tab).saveProject();
                    });
        }
    }

    /**
     * Key handler for whole application Delete - deletes selected model
     *
     * @param event
     */
    @Override
    public void handle(KeyEvent event)
    {
        if (applicationStatus.getMode() == ApplicationMode.LAYOUT)
        {
            Tab currentTab = tabDisplaySelectionModel.getSelectedItem();
            if (currentTab instanceof ProjectTab)
            {
                Project project = Lookup.getSelectedProjectProperty().get();
                UndoableProject undoableProject = new UndoableProject(project);
                switch (event.getCode())
                {
                    case DELETE:
                    case BACK_SPACE:
                        deleteSelectedModels(project, undoableProject);
                        break;
                    case A:
                        if (event.isShortcutDown())
                        {
                            selectAllModels(project);
                        }
                        break;
                    case Z:
                        if (event.isShortcutDown() && (!event.isShiftDown()))
                        {
                            undoCommand(project);
                        } else if (event.isShortcutDown() && event.isShiftDown())
                        {
                            redoCommand(project);
                        }
                        break;
                    case Y:
                        if (event.isShortcutDown())
                        {
                            redoCommand(project);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void selectAllModels(Project project)
    {
        ProjectSelection projectSelection
                = Lookup.getProjectGUIState(project).getProjectSelection();
        for (ModelContainer modelContainer : project.getTopLevelModels())
        {
            projectSelection.addModelContainer(modelContainer);
        }
    }

    private void deleteSelectedModels(Project project, UndoableProject undoableProject)
    {
        Set<ModelContainer> selectedModels
                = Lookup.getProjectGUIState(project).getProjectSelection().
                getSelectedModelsSnapshot();
        undoableProject.deleteModels(selectedModels);
    }

    private void undoCommand(Project project)
    {
        CommandStack commandStack = Lookup.getProjectGUIState(project).getCommandStack();
        if (commandStack.getCanUndo().get())
        {
            try
            {
                commandStack.undo();
            } catch (CommandStack.UndoException ex)
            {
                steno.debug("cannot undo " + ex);
            }
        }
    }

    private void redoCommand(Project project)
    {
        CommandStack commandStack = Lookup.getProjectGUIState(project).getCommandStack();
        if (commandStack.getCanRedo().get())
        {
            try
            {
                commandStack.redo();
            } catch (CommandStack.UndoException ex)
            {
                steno.debug("cannot undo " + ex);
            }
        }
    }

    public PurgeInsetPanelController getPurgeInsetPanelController()
    {
        return (PurgeInsetPanelController) insetPanelControllers.get(ApplicationMode.PURGE);
    }

    /**
     * This is fired when the main window or one of the internal windows may
     * have changed size.
     */
    private void whenWindowChangesSize()
    {
        nodesMayHaveMoved.set(!nodesMayHaveMoved.get());

//        steno.info("Window size change: " + scene.getWidth() + " : " + scene.getHeight());

        if (scene.getHeight() < VERY_SHORT_SCALE_BELOW_HEIGHT)
        {
            if (displayScalingModeProperty.get() != DisplayScalingMode.VERY_SHORT)
            {
                displayScalingModeProperty.set(DisplayScalingMode.VERY_SHORT);
            }
        } else if (scene.getHeight() < SHORT_SCALE_BELOW_HEIGHT)
        {
            if (displayScalingModeProperty.get() != DisplayScalingMode.SHORT)
            {
                displayScalingModeProperty.set(DisplayScalingMode.SHORT);
            }
        } else
        {
            if (displayScalingModeProperty.get() != DisplayScalingMode.NORMAL)
            {
                displayScalingModeProperty.set(DisplayScalingMode.NORMAL);
            }
        }

        double scaleFactor = 1.0;
        if (scene.getHeight() < START_SCALING_WINDOW_HEIGHT)
        {
            scaleFactor = scene.getHeight() / START_SCALING_WINDOW_HEIGHT;
            if (scaleFactor < MINIMUM_SCALE_FACTOR)
            {
                scaleFactor = MINIMUM_SCALE_FACTOR;
            }
        }

        rootAnchorPane.setScaleX(scaleFactor);
        rootAnchorPane.setScaleY(scaleFactor);
        rootAnchorPane.setScaleZ(scaleFactor);

        rootAnchorPane.setPrefWidth(scene.getWidth() / scaleFactor);
        rootAnchorPane.setMinWidth(scene.getWidth() / scaleFactor);
        rootAnchorPane.setPrefHeight(scene.getHeight() / scaleFactor);
        rootAnchorPane.setMinHeight(scene.getHeight() / scaleFactor);
    }

    public ReadOnlyBooleanProperty nodesMayHaveMovedProperty()
    {
        return nodesMayHaveMoved;
    }

    @Override
    public void trigger(String commandSequence, String capturedParameter)
    {
        switch (commandSequence)
        {
            case addDummyPrinterCommand:
                RoboxCommsManager.getInstance().addDummyPrinter();
                break;
            case dummyCommandPrefix:
                if (RoboxCommsManager.getInstance().getDummyPrinters().size() > 0)
                {
                    RoboxCommsManager.getInstance().getDummyPrinters().get(0).sendRawGCode(
                            capturedParameter.replaceAll("/", " ").trim().toUpperCase(), true);
                }
                break;
        }
    }

    private void configureProjectDragNDrop(Node basePane)
    {
        basePane.setOnDragOver((DragEvent event) ->
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

                        if (file.getName().toUpperCase().endsWith(
                                ApplicationConfiguration.projectFileExtension
                                .toUpperCase()))
                        {
                            extensionFound = true;
                            break;
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
        });

        basePane.setOnDragEntered((DragEvent event) ->
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
                            if (file.getName().toUpperCase().endsWith(
                                    ApplicationConfiguration.projectFileExtension
                                    .toUpperCase()))
                            {
                                extensionFound = true;
                                break;
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
        });

        basePane.setOnDragExited((DragEvent event) ->
        {
            /* mouse moved away, remove the graphical cues */
            basePane.setEffect(null);

            event.consume();
        });

        basePane.setOnDragDropped((DragEvent event) ->
        {
            /* data dropped */
            steno.debug("onDragDropped");
            /* if there is a string data on dragboard, read it and use it */
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles())
            {
                db.getFiles().forEach(file ->
                {
                    Project newProject = ProjectManager.loadProject(file.getAbsolutePath());
                    if (newProject != null)
                    {
                        ProjectTab newProjectTab = new ProjectTab(newProject,
                                tabDisplay.widthProperty(),
                                tabDisplay.heightProperty());

                        tabDisplay.getTabs().add(tabDisplay.getTabs().size() - 1, newProjectTab);
                        tabDisplaySelectionModel.select(newProjectTab);

                        if (applicationStatus.getMode() != ApplicationMode.LAYOUT)
                        {
                            applicationStatus.setMode(ApplicationMode.LAYOUT);
                        }
                    }
                });

            } else
            {
                steno.error("No files in dragboard");
            }
            /* let the source know whether the string was successfully
             * transferred and used */
            event.setDropCompleted(success);

            event.consume();
        });
    }

    public ReadOnlyObjectProperty<DisplayScalingMode> getDisplayScalingModeProperty()
    {
        return displayScalingModeProperty;
    }
}
