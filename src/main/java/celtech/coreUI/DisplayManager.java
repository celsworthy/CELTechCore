package celtech.coreUI;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.ProjectManager;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.components.ProgressDialog;
import celtech.coreUI.components.ProjectLoader;
import celtech.coreUI.components.ProjectTab;
import celtech.coreUI.components.SlideoutAndProjectHolder;
import celtech.coreUI.components.Spinner;
import celtech.coreUI.components.TopMenuStrip;
import celtech.coreUI.controllers.InfoScreenIndicatorController;
import celtech.coreUI.controllers.PrinterStatusPageController;
import celtech.coreUI.controllers.panels.LayoutSidePanelController;
import celtech.coreUI.controllers.panels.LayoutSlideOutPanelController;
import celtech.coreUI.controllers.panels.LayoutStatusMenuStripController;
import celtech.coreUI.controllers.panels.PurgeInsetPanelController;
import celtech.coreUI.controllers.panels.SettingsSidePanelController;
import celtech.coreUI.controllers.panels.SidePanelManager;
import celtech.coreUI.keycommands.HiddenKey;
import celtech.coreUI.keycommands.KeyCommandListener;
import celtech.coreUI.visualisation.ThreeDViewManager;
import celtech.coreUI.visualisation.metaparts.ModelLoadResult;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.services.modelLoader.ModelLoadResults;
import celtech.services.modelLoader.ModelLoaderService;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyEvent;
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
public class DisplayManager implements EventHandler<KeyEvent>, KeyCommandListener
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
        DisplayManager.class.getName());
    private static final ApplicationStatus applicationStatus = ApplicationStatus.getInstance();
    private static final ProjectManager projectManager = ProjectManager.getInstance();

    private static DisplayManager instance = null;
    private static Stage mainStage = null;
    private static Scene scene = null;

    private HBox mainHolder = null;
    private StackPane sidePanelContainer = null;
    private final HashMap<ApplicationMode, Pane> insetPanels = new HashMap<>();
    private final HashMap<ApplicationMode, HBox> sidePanels = new HashMap<>();
    private final HashMap<ApplicationMode, HBox> slideOutPanels = new HashMap<>();
    private final StackPane rhPanel = new StackPane();
    private final SlideoutAndProjectHolder slideoutAndProjectHolder = new SlideoutAndProjectHolder();
    private final HashMap<ApplicationMode, Initializable> insetPanelControllers = new HashMap<>();
    private final HashMap<ApplicationMode, SidePanelManager> sidePanelControllers = new HashMap<>();
    private final HashMap<ApplicationMode, Initializable> slideOutControllers = new HashMap<>();

    private static TabPane tabDisplay = null;
    private LayoutStatusMenuStripController layoutStatusMenuStripController = null;
    private static SingleSelectionModel<Tab> tabDisplaySelectionModel = null;
    private static Tab printerStatusTab = null;
    private static Tab addPageTab = null;
    private Tab lastLayoutTab = null;

    private final HashMap<String, SidePanelManager> sidePanelControllerCache = new HashMap<>();
    private final HashMap<String, HBox> sidePanelCache = new HashMap<>();
    private final HashMap<String, Initializable> slideoutPanelControllerCache = new HashMap<>();
    private final HashMap<String, HBox> slideoutPanelCache = new HashMap<>();

    /*
     * Project loading
     */
    private ProjectLoader projectLoader = null;
    /*
     * Mesh Model loading
     */
    private final ModelLoaderService modelLoaderService = new ModelLoaderService();
    private ProgressDialog modelLoadDialog = null;

    /*
     * GCode-related
     */
    private final IntegerProperty layersInGCode = new SimpleIntegerProperty(0);

    private InfoScreenIndicatorController infoScreenIndicatorController = null;

    private static final String addDummyPrinterCommand = "AddDummy";
    private static final String dummyCommandPrefix = "dummy:";

    private AnchorPane root;
    private Pane spinnerContainer;
    private Spinner spinner;

    private BooleanProperty nodesMayHaveMoved = new SimpleBooleanProperty(false);

    private DisplayManager()
    {
        steno.debug("Starting AutoMaker - intialising display manager...");
        switch (ApplicationConfiguration.getMachineType())
        {
            case LINUX_X64:
            case LINUX_X86:
                System.setProperty("prism.lcdtext", "false");
                break;
            default:
                System.setProperty("prism.lcdtext", "true");
                break;
        }
        steno.debug("Starting AutoMaker - machine type is " + ApplicationConfiguration.
            getMachineType());

        modelLoadDialog = new ProgressDialog(modelLoaderService);

        modelLoaderService.setOnSucceeded((WorkerStateEvent t) ->
        {
            whenModelLoadSucceeded();
        });
    }

    private void whenModelLoadSucceeded()
    {
        ModelLoadResults loadResults = modelLoaderService.getValue();
        if (loadResults.getResults().isEmpty())
        {
            return;
        }
        ModelLoadResult firstResult = loadResults.getResults().get(0);
        boolean projectIsEmpty = firstResult.getTargetProjectTab().getLoadedModels().isEmpty();
        for (ModelLoadResult loadResult : loadResults.getResults())
        {
            if (loadResult != null)
            {
                if (loadResult.isModelTooLarge())
                {
                    boolean shrinkModel = Lookup.getSystemNotificationHandler().
                        showModelTooBigDialog(loadResult.getModelFilename());

                    if (shrinkModel)
                    {
                        ModelContainer modelContainer = loadResult.getModelContainer();
                        modelContainer.shrinkToFitBed();
                        loadResult.getTargetProjectTab().addModelContainer(
                            loadResult.getFullFilename(), modelContainer);
                    }
                } else
                {
                    ModelContainer modelContainer = loadResult.getModelContainer();
                    loadResult.getTargetProjectTab().addModelContainer(loadResult.getFullFilename(),
                                                                       modelContainer);
                }
            } else
            {
                steno.error("Error whilst attempting to load model");
            }
        }
        if (loadResults.isRelayout() && projectIsEmpty && loadResults.getResults().size() > 1)
        {
            autoLayout();
        }

    }

    private void loadProjectsAtStartup()
    {
        // Load up any projects that were open last time we shut down....
        ProjectManager pm = ProjectManager.getInstance();
        List<Project> preloadedProjects = pm.getOpenProjects();
        for (Project project : preloadedProjects)
        {
            ProjectTab newProjectTab = new ProjectTab(instance, project, tabDisplay.widthProperty(),
                                                      tabDisplay.heightProperty());
            tabDisplay.getTabs().add(tabDisplay.getTabs().size() - 1, newProjectTab);
        }
    }

    private void switchPagesForMode(ApplicationMode oldMode, ApplicationMode newMode)
    {
        infoScreenIndicatorController.setSelected(newMode == ApplicationMode.STATUS);

        // Remove the existing side panel
        if (oldMode != null)
        {
            sidePanelContainer.getChildren().remove(sidePanels.get(oldMode));
            Pane lastInsetPanel = insetPanels.get(oldMode);
            if (lastInsetPanel != null)
            {
                rhPanel.getChildren().remove(lastInsetPanel);
            } else
            {
                if (rhPanel.getChildren().contains(slideoutAndProjectHolder))
                {
                    rhPanel.getChildren().remove(slideoutAndProjectHolder);
                }
            }
        }

        // Now add the relevant new one...
        sidePanelContainer.getChildren().add(sidePanels.get(newMode));

        slideoutAndProjectHolder.switchInSlideout(slideOutPanels.get(newMode));

        Pane newInsetPanel = insetPanels.get(newMode);
        if (newInsetPanel != null)
        {
            rhPanel.getChildren().add(0, newInsetPanel);
        }

        if (newMode == ApplicationMode.LAYOUT)
        {
            rhPanel.getChildren().add(0, slideoutAndProjectHolder);

            ProjectTab projectTab = null;

            //Create a tab if one doesn't already exist
            if (tabDisplay.getTabs().size() <= 1)
            {
                projectTab = new ProjectTab(this, tabDisplay.widthProperty(),
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

            projectTab = (ProjectTab) tabDisplaySelectionModel.getSelectedItem();
            ((LayoutSlideOutPanelController) slideOutControllers.get(ApplicationMode.LAYOUT)).
                bindLoadedModels(
                    projectTab.getProject());
            ((LayoutSidePanelController) (sidePanelControllers.get(ApplicationMode.LAYOUT))).
                bindLoadedModels(
                    projectTab.getThreeDViewManager());
            layoutStatusMenuStripController.bindSelectedModels(projectTab);
            projectTab.setMode(newMode);
        } else if (newMode == ApplicationMode.SETTINGS)
        {
            rhPanel.getChildren().add(0, slideoutAndProjectHolder);
            ProjectTab projectTab = (ProjectTab) tabDisplaySelectionModel.getSelectedItem();
            projectTab.setMode(newMode);
        } else if (newMode == ApplicationMode.STATUS)
        {
            rhPanel.getChildren().add(0, slideoutAndProjectHolder);
            tabDisplaySelectionModel.select(0);
        }
    }

    /**
     *
     * @return
     */
    public static DisplayManager getInstance()
    {
        if (instance == null)
        {
            instance = new DisplayManager();
        }

        return instance;
    }

    /**
     * Show the spinner, and keep it centred on the given region.
     */
    public void startSpinning(Region centreRegion)
    {
        spinner.setVisible(true);
        spinner.startSpinning();
        spinner.setCentreNode(centreRegion);
    }

    /**
     * Stop and hide the spinner.
     */
    public void stopSpinning()
    {
        spinner.setVisible(false);
        spinner.stopSpinning();
    }

    /**
     *
     * @param mainStage
     * @param applicationName
     */
    public void configureDisplayManager(Stage mainStage, String applicationName)
    {
        this.mainStage = mainStage;
        mainStage.setTitle(applicationName + " - "
            + ApplicationConfiguration.getApplicationVersion());
        ApplicationConfiguration.setTitleAndVersion(Lookup.i18n(
            "application.title")
            + " - " + ApplicationConfiguration.getApplicationVersion());

        root = new AnchorPane();

        spinnerContainer = new Pane();
        spinnerContainer.setMouseTransparent(true);
        spinnerContainer.setPickOnBounds(false);
        spinner = new Spinner();
        spinnerContainer.getChildren().add(spinner);

        AnchorPane.setBottomAnchor(root, 0.0);
        AnchorPane.setLeftAnchor(root, 0.0);
        AnchorPane.setRightAnchor(root, 0.0);
        AnchorPane.setTopAnchor(root, 0.0);

        mainHolder = new HBox();
        mainHolder.setPrefSize(-1, -1);

        AnchorPane.setBottomAnchor(mainHolder, 0.0);
        AnchorPane.setLeftAnchor(mainHolder, 0.0);
        AnchorPane.setRightAnchor(mainHolder, 0.0);
        AnchorPane.setTopAnchor(mainHolder, 0.0);

        root.getChildren().add(mainHolder);
        root.getChildren().add(spinnerContainer);

        // Load in all of the side panels
        for (ApplicationMode mode : ApplicationMode.values())
        {
            setupPanelsForMode(mode);
        }

        // Create a place to hang the side panels from
        sidePanelContainer = new StackPane();
        HBox.setHgrow(sidePanelContainer, Priority.NEVER);

        mainHolder.getChildren().add(sidePanelContainer);

        slideoutAndProjectHolder.getStyleClass().add("master-details-pane");
        HBox.setHgrow(slideoutAndProjectHolder, Priority.ALWAYS);

        HBox.setHgrow(rhPanel, Priority.ALWAYS);

        addTopMenuStripController();

        mainHolder.getChildren().add(rhPanel);
        
        // Configure the main display tab pane - just the printer status page to start with
        tabDisplay = new TabPane();
        tabDisplay.setPickOnBounds(false);
        tabDisplay.setOnKeyPressed(this);
        tabDisplay.setTabMinHeight(56);
        tabDisplay.setTabMaxHeight(56);
        tabDisplaySelectionModel = tabDisplay.getSelectionModel();
        tabDisplay.getStyleClass().add("main-project-tabPane");

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
                configure(slideoutAndProjectHolder.getProjectTabPaneHolder());

            printerStatusTab = new Tab();
            printerStatusTab.setText(Lookup.i18n("printerStatusTabTitle"));
            FXMLLoader printerStatusPageLabelLoader = new FXMLLoader(getClass().getResource(
                ApplicationConfiguration.fxmlResourcePath
                + "infoScreenIndicator.fxml"), Lookup.getLanguageBundle());
            VBox printerStatusLabelGroup = printerStatusPageLabelLoader.load();
            infoScreenIndicatorController = printerStatusPageLabelLoader.getController();
            printerStatusTab.setGraphic(printerStatusLabelGroup);
            printerStatusTab.setClosable(false);
            printerStatusTab.setContent(printerStatusPage);
            tabDisplay.getTabs().add(printerStatusTab);

            addPageTab = new Tab();
            addPageTab.setText("+");
            addPageTab.setClosable(false);
            tabDisplay.getTabs().add(addPageTab);

            tabDisplaySelectionModel.selectedItemProperty().addListener(
                (ObservableValue<? extends Tab> ov, Tab lastTab, Tab newTab) ->
                {
                    if (newTab == addPageTab)
                    {
                        if (lastTab instanceof ProjectTab)
                        {
                            ((ProjectTab) lastTab).setMode(ApplicationMode.LAYOUT);
                        }

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

                        if (lastTab instanceof ProjectTab)
                        {
                            ((ProjectTab) lastTab).setMode(ApplicationMode.LAYOUT);
                        }

                        if (lastTab != newTab)
                        {
                            ProjectTab projectTab = (ProjectTab) tabDisplaySelectionModel.
                            getSelectedItem();
                            ((LayoutSidePanelController) (sidePanelControllers.get(
                                ApplicationMode.LAYOUT))).bindLoadedModels(
                                projectTab.getThreeDViewManager());
                            layoutStatusMenuStripController.bindSelectedModels(projectTab);
                            ((SettingsSidePanelController) sidePanelControllers.get(
                                ApplicationMode.SETTINGS)).projectChanged(projectTab.getProject());
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

            slideoutAndProjectHolder.populateProjectDisplay(tabDisplay);
        } catch (IOException ex)
        {
            steno.error("Failed to load printer status page:" + ex);
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
            layoutStatusMenuStripController = menuStripLoader.getController();
            menuStripControls.prefWidthProperty().bind(slideoutAndProjectHolder.widthProperty());
            slideoutAndProjectHolder.populateProjectDisplay(menuStripControls);
        } catch (IOException ex)
        {
            steno.error("Failed to load menu strip controls:" + ex);
            ex.printStackTrace();
        }

        projectLoader = new ProjectLoader();

        scene = new Scene(root, ApplicationConfiguration.DEFAULT_WIDTH,
                          ApplicationConfiguration.DEFAULT_HEIGHT);

        scene.getStylesheets().add(ApplicationConfiguration.getMainCSSFile());

        scene.widthProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(
                ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                fireNodeMayHaveMovedTrigger();
            }
        });

        scene.heightProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(
                ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                fireNodeMayHaveMovedTrigger();
            }
        });

        slideoutAndProjectHolder.widthProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(
                ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                fireNodeMayHaveMovedTrigger();
            }
        });

        mainStage.maximizedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(
                ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                fireNodeMayHaveMovedTrigger();
            }
        });

        mainStage.fullScreenProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(
                ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                steno.info("Stage fullscreen = " + newValue.booleanValue());
                fireNodeMayHaveMovedTrigger();
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

        loadProjectsAtStartup();

        root.layout();
    }

    private void setupPanelsForMode(ApplicationMode mode)
    {
        try
        {
            URL fxmlFileName = getClass().getResource(mode.getInsetPanelFXMLName());
            if (fxmlFileName != null)
            {
                steno.debug("About to load inset panel fxml: " + fxmlFileName);
                FXMLLoader insetPanelLoader = new FXMLLoader(fxmlFileName, Lookup.getLanguageBundle());
                Pane insetPanel = (Pane) insetPanelLoader.load();
                Initializable insetPanelController = insetPanelLoader.getController();
                insetPanel.setId(mode.name());
                insetPanels.put(mode, insetPanel);
                insetPanelControllers.put(mode, insetPanelController);
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
            insetPanels.put(mode, null);
            insetPanelControllers.put(mode, null);
            steno.warning("Couldn't load inset panel for mode:" + mode + ". " + ex.getMessage());
        }

        SidePanelManager sidePanelController = null;
        HBox sidePanel = null;
        boolean sidePanelLoadedOK = false;

        if (sidePanelControllerCache.containsKey(mode.getSidePanelFXMLName()) == false)
        {
            try
            {
                URL fxmlFileName = getClass().getResource(mode.getSidePanelFXMLName());
                steno.debug("About to load side panel fxml: " + fxmlFileName);
                FXMLLoader sidePanelLoader = new FXMLLoader(fxmlFileName, Lookup.getLanguageBundle());
                sidePanel = (HBox) sidePanelLoader.load();
                sidePanelController = sidePanelLoader.getController();
                sidePanel.setId(mode.name());
                sidePanelLoadedOK = true;
                sidePanelControllerCache.put(mode.getSidePanelFXMLName(), sidePanelController);
                sidePanelCache.put(mode.getSidePanelFXMLName(), sidePanel);
            } catch (Exception ex)
            {
                ex.printStackTrace();
                sidePanels.put(mode, null);
                sidePanelControllers.put(mode, null);
                steno.error("Couldn't load side panel for mode:" + mode + ". "
                    + ex);
                System.out.println("Exception: " + ex.getMessage());
            }
        } else
        {
            sidePanelController = sidePanelControllerCache.get(mode.getSidePanelFXMLName());
            sidePanel = sidePanelCache.get(mode.getSidePanelFXMLName());
            sidePanelLoadedOK = true;
        }

        if (sidePanelLoadedOK)
        {
            sidePanels.put(mode, sidePanel);
            sidePanelControllers.put(mode, sidePanelController);
        }

        Initializable slideOutController = null;
        HBox slideOut = null;
        boolean slideoutPanelLoadedOK = false;

        if (slideoutPanelControllerCache.containsKey(mode.getSlideOutFXMLName()) == false)
        {
            try
            {
                URL fxmlSlideOutFileName = getClass().getResource(mode.getSlideOutFXMLName());
                steno.debug("About to load slideout fxml: "
                    + fxmlSlideOutFileName);
                FXMLLoader slideOutLoader = new FXMLLoader(fxmlSlideOutFileName, Lookup.getLanguageBundle());
                slideOut = (HBox) slideOutLoader.load();
                slideOutController = slideOutLoader.getController();
                sidePanelControllers.get(mode).configure(slideOutController);
                slideoutPanelLoadedOK = true;
            } catch (Exception ex)
            {
                slideOutPanels.put(mode, null);
                slideOutControllers.put(mode, null);
                steno.error("Couldn't load slideout panel for mode:" + mode
                    + ". " + ex + " : " + ex.getCause());
                System.out.println("Exception: " + ex.getMessage());
            }
        } else
        {
            slideOutController = slideoutPanelControllerCache.get(mode.getSlideOutFXMLName());
            slideOut = slideoutPanelCache.get(mode.getSlideOutFXMLName());
        }

        if (slideoutPanelLoadedOK)
        {
            slideOutPanels.put(mode, slideOut);
            slideOutControllers.put(mode, slideOutController);
        }
    }

    private void addTopMenuStripController()
    {
        HBox topMenuStrip = new TopMenuStrip();
        rhPanel.getChildren().add(topMenuStrip);
    }

    private ProjectTab createAndAddNewProjectTab()
    {
        ProjectTab projectTab = new ProjectTab(instance, tabDisplay.widthProperty(),
                                               tabDisplay.heightProperty());
        tabDisplay.getTabs().add(tabDisplay.getTabs().size() - 1, projectTab);
        tabDisplaySelectionModel.select(projectTab);

        return projectTab;
    }

    /**
     *
     * @return
     */
    public static Stage getMainStage()
    {
        return mainStage;
    }

    /**
     *
     * @param value
     */
    public final void setLayersInGCode(int value)
    {
        layersInGCode.set(value);
    }

    /**
     * Load each model in modelsToLoad, do not lay them out on the bed. ,
     *
     * @param modelsToLoad
     */
    public void loadExternalModels(List<File> modelsToLoad)
    {
        loadExternalModels(modelsToLoad, false);
    }

    /**
     * Load each model in modelsToLoad and then optionally lay them out on the bed.
     *
     * @param modelsToLoad
     * @param relayout
     */
    public void loadExternalModels(List<File> modelsToLoad, boolean relayout)
    {
        loadExternalModels(modelsToLoad, false, relayout);
    }

    /**
     * Load each model in modelsToLoad, do not lay them out on the bed. , If there are already
     * models loaded in the project then do not relayout even if relayout=true;
     *
     * @param modelsToLoad
     * @param newTab
     * @param relayout
     */
    public void loadExternalModels(List<File> modelsToLoad, boolean newTab, boolean relayout)
    {
        ProjectTab tabToUse = null;

        if (!modelsToLoad.isEmpty())
        {
            if (newTab)
            {
                tabToUse = createAndAddNewProjectTab();
            } else if (!modelLoaderService.isRunning()
                && tabDisplaySelectionModel.selectedItemProperty().get() instanceof ProjectTab)
            {
                tabToUse = (ProjectTab) (tabDisplaySelectionModel.selectedItemProperty().get());
            }

            if (tabToUse != null)
            {
                modelLoaderService.reset();
                modelLoaderService.setModelFilesToLoad(modelsToLoad, relayout);
                modelLoaderService.setTargetTab(tabToUse);
                modelLoaderService.start();
            }
        }
    }

    /**
     *
     * @return
     */
    public ReadOnlyBooleanProperty modelLoadingProperty()
    {
        return modelLoaderService.runningProperty();
    }

    /**
     *
     */
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
     *
     */
    public void deleteSelectedModels()
    {
        Tab currentTab = tabDisplaySelectionModel.selectedItemProperty().get();
        if (currentTab instanceof ProjectTab)
        {
            ((ProjectTab) currentTab).deleteSelectedModels();
        }
    }

    /**
     *
     */
    public void copySelectedModels()
    {
        Tab currentTab = tabDisplaySelectionModel.selectedItemProperty().get();
        if (currentTab instanceof ProjectTab)
        {
            ((ProjectTab) currentTab).copySelectedModels();
        }
    }

    /**
     *
     */
    public void autoLayout()
    {
        Tab currentTab = tabDisplaySelectionModel.getSelectedItem();
        if (currentTab instanceof ProjectTab)
        {
            ((ProjectTab) currentTab).autoLayout();
        }
    }

    /**
     *
     */
    public void activateSnapToGround()
    {
        Tab currentTab = tabDisplaySelectionModel.getSelectedItem();
        if (currentTab instanceof ProjectTab)
        {
            ((ProjectTab) currentTab).getThreeDViewManager().activateSnapToGround();
        }
    }

    /**
     *
     * @param selectedModel
     */
    public void deselectModel(ModelContainer selectedModel)
    {
        Tab currentTab = tabDisplaySelectionModel.getSelectedItem();
        if (currentTab instanceof ProjectTab)
        {
            ((ProjectTab) currentTab).deselectModel(selectedModel);
        }
    }

    /**
     *
     * @return
     */
    public ThreeDViewManager getCurrentlyVisibleViewManager()
    {
        Tab currentTab = tabDisplaySelectionModel.getSelectedItem();
        if (currentTab instanceof ProjectTab)
        {
            return ((ProjectTab) currentTab).getThreeDViewManager();
        } else
        {
            return null;
        }
    }

    /**
     *
     * @return
     */
    public Project getCurrentlyVisibleProject()
    {
        Project projectToReturn = null;

        if (tabDisplaySelectionModel != null)
        {
            Tab currentTab = tabDisplaySelectionModel.getSelectedItem();
            if (currentTab instanceof ProjectTab)
            {
                projectToReturn = ((ProjectTab) currentTab).getProject();
            }
        }

        return projectToReturn;
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
                ProjectTab projectTab = (ProjectTab) currentTab;
                switch (event.getCode())
                {
                    case DELETE:
                    case BACK_SPACE:
                        projectTab.deleteSelectedModels();
                        break;
                    case A:
                        if (event.isShortcutDown())
                        {
                            projectTab.selectAllModels();
                        }
                    default:
                        break;
                }
            }
        }
    }

    /**
     *
     * @param mode
     * @return
     */
    public VBox getSidePanelSlideOutHandle(ApplicationMode mode)
    {
        HBox slideOut = slideOutPanels.get(mode);

        VBox container = null;

        for (Node subNode : slideOut.getChildren())
        {
            if (subNode.getId().equalsIgnoreCase("Container")
                && subNode instanceof VBox)
            {
                container = (VBox) subNode;
                break;
            }
        }
        return container;
    }

    /**
     *
     */
    public void slideOutAdvancedPanel()
    {
        if (slideoutAndProjectHolder.isSlidIn() && slideoutAndProjectHolder.isSliding() == false)
        {
            slideoutAndProjectHolder.startSlidingOut();
        }
    }

    public PurgeInsetPanelController getPurgeInsetPanelController()
    {
        return (PurgeInsetPanelController) insetPanelControllers.get(ApplicationMode.PURGE);
    }

    private void fireNodeMayHaveMovedTrigger()
    {
        nodesMayHaveMoved.set(!nodesMayHaveMoved.get());
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
}
