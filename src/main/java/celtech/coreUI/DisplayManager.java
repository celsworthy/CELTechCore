/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import celtech.coreUI.controllers.InfoScreenIndicatorController;
import celtech.coreUI.controllers.MenuStripController;
import celtech.coreUI.controllers.PrinterStatusPageController;
import celtech.coreUI.controllers.sidePanels.LayoutSidePanelController;
import celtech.coreUI.controllers.sidePanels.LayoutSlideOutPanelController;
import celtech.coreUI.controllers.sidePanels.SettingsSidePanelController;
import celtech.coreUI.controllers.sidePanels.SidePanelManager;
import celtech.coreUI.visualisation.ThreeDViewManager;
import celtech.coreUI.visualisation.importers.ModelLoadResult;
import celtech.modelcontrol.ModelContainer;
import celtech.services.modelLoader.ModelLoaderService;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.dialog.Dialogs.CommandLink;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class DisplayManager implements EventHandler<KeyEvent>
{

    private static final Stenographer steno = StenographerFactory.getStenographer(DisplayManager.class.getName());
    private static final ApplicationStatus applicationStatus = ApplicationStatus.getInstance();
    private static final ProjectManager projectManager = ProjectManager.getInstance();

    private static DisplayManager instance = null;
    private static Stage mainStage = null;
    private static Scene scene = null;
    
    private static AnchorPane root = null;
    private HBox mainHolder = null;
    private StackPane sidePanelContainer = null;
    private final Map<ApplicationMode, HBox> sidePanels = new HashMap<>();
    private final Map<ApplicationMode, HBox> slideOutPanels = new HashMap<>();
    private SlideoutAndProjectHolder rhPanel;
    private final Map<ApplicationMode, SidePanelManager> sidePanelControllers = new HashMap<>();
    private final Map<ApplicationMode, Initializable> slideOutControllers = new HashMap<>();
    private static TabPane tabDisplay = null;
    private MenuStripController menuStripController = null;
    private static SingleSelectionModel<Tab> tabDisplaySelectionModel = null;
    private static Tab printerStatusTab = null;
    private static Tab addPageTab = null;
    private Tab lastLayoutTab = null;

    /*
     * Project loading
     */
    private ProjectLoader projectLoader = null;
    /*
     * Mesh Model loading
     */
    private ModelLoaderService modelLoaderService = new ModelLoaderService();
    private ProgressDialog modelLoadDialog = null;

    /*
     * GCode model loading
     */
    private final ObservableList<String> gcodeFileLines = FXCollections.observableArrayList();
    /*
     * GCode-related
     */
    private final IntegerProperty layersInGCode = new SimpleIntegerProperty(0);
    private final BooleanProperty noGCodeLoaded = new SimpleBooleanProperty(true);

    private InfoScreenIndicatorController infoScreenIndicatorController = null;

    /**
     * The primary font used throughout the GUI, at various font sizes
     */
    private final Font primaryFont;

    private Locale usersLocale = null;

    private DisplayManager()
    {
        usersLocale = Locale.getDefault();

        String primaryFontLocation = DisplayManager.class.getResource(ApplicationConfiguration.fontResourcePath + "SourceSansPro-Light.ttf").toExternalForm();
        primaryFont = Font.loadFont(primaryFontLocation, 10);
        
        modelLoadDialog = new ProgressDialog(modelLoaderService);

        CommandLink dontLoadModel = new Dialogs.CommandLink(getLanguageBundle().getString("dialogs.ModelTooLargeNo"), null);
        CommandLink shrinkModel = new Dialogs.CommandLink(getLanguageBundle().getString("dialogs.ShrinkModelToFit"), null);

        modelLoaderService.setOnSucceeded((WorkerStateEvent t) ->
        {
            ModelLoadResult loadResult = (ModelLoadResult) modelLoaderService.getValue();

            if (loadResult != null)
            {
                if (loadResult.isModelTooLarge())
                {

                    Action tooBigResponse = Dialogs.create().title(getLanguageBundle().getString("dialogs.ModelTooLargeTitle"))
                            .message(getLanguageBundle().getString("dialogs.ModelTooLargeDescription"))
                            .masthead(null)
                            .showCommandLinks(shrinkModel, shrinkModel, dontLoadModel);

                    if (tooBigResponse == shrinkModel)
                    {
                        ModelContainer modelContainer = loadResult.getModelContainer();
                        modelContainer.shrinkToFitBed();
                        loadResult.getTargetProjectTab().addModelContainer(loadResult.getFullFilename(), modelContainer);
                    }
//                    else if (buttonPressed == modelCutToSize)
//                    {
//                        ModelContainer modelContainer = loadResult.getModelContainer();
//                        ArrayList<ModelContainer> cutPieces = modelContainer.cutToSize();
//                        for (ModelContainer model : cutPieces)
//                        {
//                            loadResult.getTargetProjectTab().addModelContainer(model.getModelName(), model);
//                        }
//                    }
                } else
                {
                    ModelContainer modelContainer = loadResult.getModelContainer();
                    loadResult.getTargetProjectTab().addModelContainer(loadResult.getFullFilename(), modelContainer);
                }
            } else
            {
                steno.error("Error whilst attempting to load model");
            }
        });
    }

    private void loadProjectsAtStartup()
    {
        // Load up any projects that were open last time we shut down....
        ProjectManager pm = ProjectManager.getInstance();
        ArrayList<Project> preloadedProjects = pm.getLoadedModels();
        for (Project project : preloadedProjects)
        {
            ProjectTab newProjectTab = new ProjectTab(instance, project, tabDisplay.widthProperty(), tabDisplay.heightProperty());
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
        }

        // Now add the relevant new one...
        sidePanelContainer.getChildren().add(sidePanels.get(newMode));

        rhPanel.switchInSlideout(slideOutPanels.get(newMode));

        if (newMode == ApplicationMode.LAYOUT)
        {
            ProjectTab projectTab = null;

            //Create a tab if one doesnt already exist
            if (tabDisplay.getTabs().size() <= 1)
            {
                projectTab = new ProjectTab(this, tabDisplay.widthProperty(), tabDisplay.heightProperty());
                tabDisplay.getTabs().add(projectTab);
                tabDisplaySelectionModel.select(projectTab);
            } else
            {
                //Switch tabs if necessary
                if (tabDisplaySelectionModel.getSelectedItem() instanceof ProjectTab == false)
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
            ((LayoutSlideOutPanelController) slideOutControllers.get(ApplicationMode.LAYOUT)).bindLoadedModels(projectTab.getProject());
            ((LayoutSidePanelController) (sidePanelControllers.get(ApplicationMode.LAYOUT))).bindLoadedModels(projectTab.getThreeDViewManager());
            menuStripController.bindSelectedModels(projectTab.getSelectionContainer());
            projectTab.setMode(newMode);
        } else if (newMode == ApplicationMode.SETTINGS)
        {
            ProjectTab projectTab = (ProjectTab) tabDisplaySelectionModel.getSelectedItem();
            projectTab.setMode(newMode);
        } else if (newMode == ApplicationMode.STATUS)
        {
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
     *
     * @param mainStage
     * @param applicationName
     */
    public void configureDisplayManager(Stage mainStage, String applicationName)
    {
        rhPanel = new SlideoutAndProjectHolder();
        
        this.mainStage = mainStage;
        mainStage.setTitle(applicationName + " - " + ApplicationConfiguration.getApplicationVersion());
        ApplicationConfiguration.setTitleAndVersion(getLanguageBundle().getString("application.title") + " - " + ApplicationConfiguration.getApplicationVersion());

        root = new AnchorPane();
        mainHolder = new HBox();
        AnchorPane.setBottomAnchor(mainHolder, 0.0);
        AnchorPane.setLeftAnchor(mainHolder, 0.0);
        AnchorPane.setRightAnchor(mainHolder, 0.0);
        AnchorPane.setTopAnchor(mainHolder, 0.0);

        mainHolder.setPrefSize(-1, -1);
        root.getChildren().add(mainHolder);

        // Load in all of the side panels
        for (ApplicationMode mode : ApplicationMode.values())
        {
            try
            {
                URL fxmlFileName = getClass().getResource(mode.getSidePanelFXMLName());
                steno.debug("About to load side panel fxml: " + fxmlFileName);
                FXMLLoader sidePanelLoader = new FXMLLoader(fxmlFileName, getLanguageBundle());
                HBox sidePanel = (HBox) sidePanelLoader.load();
                SidePanelManager sidePanelController = sidePanelLoader.getController();
                sidePanel.setId(mode.name());
                sidePanels.put(mode, sidePanel);
                sidePanelControllers.put(mode, sidePanelController);
            } catch (Exception ex)
            {
                ex.printStackTrace();
                sidePanels.put(mode, null);
                sidePanelControllers.put(mode, null);
                steno.error("Couldn't load side panel for mode:" + mode + ". " + ex);
                System.out.println("Exception: " + ex.getMessage());
            }

            try
            {
                URL fxmlSlideOutFileName = getClass().getResource(mode.getSlideOutFXMLName());
                steno.debug("About to load slideout fxml: " + fxmlSlideOutFileName);
                FXMLLoader slideOutLoader = new FXMLLoader(fxmlSlideOutFileName, getLanguageBundle());
                HBox slideOut = (HBox) slideOutLoader.load();
                Initializable slideOutController = slideOutLoader.getController();
                slideOutPanels.put(mode, slideOut);
                slideOutControllers.put(mode, slideOutController);
                sidePanelControllers.get(mode).configure(slideOutController);
            } catch (Exception ex)
            {
                slideOutPanels.put(mode, null);
                slideOutControllers.put(mode, null);
                steno.error("Couldn't load slideout panel for mode:" + mode + ". " + ex + " : " + ex.getCause());
                System.out.println("Exception: " + ex.getMessage());
            }
        }

        // Create a place to hang the side panels from
        sidePanelContainer = new StackPane();
        HBox.setHgrow(sidePanelContainer, Priority.NEVER);

        mainHolder.getChildren().add(sidePanelContainer);

        rhPanel.setPrefSize(-1, -1);
        rhPanel.getStyleClass().add("master-details-pane");
        HBox.setHgrow(rhPanel, Priority.ALWAYS);
        mainHolder.getChildren().add(rhPanel);

        // Configure the main display tab pane - just the printer status page to start with
        tabDisplay = new TabPane();
        tabDisplay.setPickOnBounds(false);
        tabDisplay.setOnKeyPressed(this);
        tabDisplay.setTabMinHeight(30);
        tabDisplay.setTabMaxHeight(30);
        tabDisplaySelectionModel = tabDisplay.getSelectionModel();
        tabDisplay.getStyleClass().add("main-project-tabPane");

        VBox.setVgrow(tabDisplay, Priority.ALWAYS);

        // The printer status tab will always be visible - the page is static
        try
        {
            FXMLLoader printerStatusPageLoader = new FXMLLoader(getClass().getResource(ApplicationConfiguration.fxmlResourcePath + "PrinterStatusPage.fxml"), getLanguageBundle());
            AnchorPane printerStatusPage = printerStatusPageLoader.load();
            PrinterStatusPageController printerStatusPageController = printerStatusPageLoader.getController();
            printerStatusPageController.configure(rhPanel.getProjectTabPaneHolder());

            printerStatusTab = new Tab();
            printerStatusTab.setText(getLanguageBundle().getString("printerStatusTabTitle"));
            FXMLLoader printerStatusPageLabelLoader = new FXMLLoader(getClass().getResource(ApplicationConfiguration.fxmlResourcePath + "infoScreenIndicator.fxml"), getLanguageBundle());
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

            tabDisplaySelectionModel.selectedItemProperty().addListener((ObservableValue<? extends Tab> ov, Tab lastTab, Tab newTab) ->
            {
                if (newTab == addPageTab)
                {
                    ProjectTab projectTab = new ProjectTab(instance, tabDisplay.widthProperty(), tabDisplay.heightProperty());
                    tabDisplay.getTabs().add(tabDisplay.getTabs().size() - 1, projectTab);
                    tabDisplaySelectionModel.select(projectTab);
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
                        ProjectTab projectTab = (ProjectTab) tabDisplaySelectionModel.getSelectedItem();
                        ((LayoutSidePanelController) (sidePanelControllers.get(ApplicationMode.LAYOUT))).bindLoadedModels(projectTab.getThreeDViewManager());
                        menuStripController.bindSelectedModels(projectTab.getSelectionContainer());
                        ((SettingsSidePanelController) sidePanelControllers.get(ApplicationMode.SETTINGS)).projectChanged(projectTab.getProject());
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

            rhPanel.populateProjectDisplay(tabDisplay);
        } catch (IOException ex)
        {
            steno.error("Failed to load printer status page:" + ex);
        }

        applicationStatus.modeProperty().addListener((ObservableValue<? extends ApplicationMode> ov, ApplicationMode oldMode, ApplicationMode newMode) ->
        {
            switchPagesForMode(oldMode, newMode);
        });

        applicationStatus.setMode(ApplicationMode.STATUS);

        try
        {
            URL menuStripURL = getClass().getResource(ApplicationConfiguration.fxmlResourcePath + "MenuStrip.fxml");
            FXMLLoader menuStripLoader = new FXMLLoader(menuStripURL, getLanguageBundle());
            BorderPane menuStripControls = (BorderPane) menuStripLoader.load();
            menuStripController = menuStripLoader.getController();
            menuStripControls.prefWidthProperty().bind(rhPanel.widthProperty());
            VBox.setVgrow(menuStripControls, Priority.NEVER);
            rhPanel.populateProjectDisplay(menuStripControls);
        } catch (IOException ex)
        {
            steno.error("Failed to load menu strip controls:" + ex);
        }

        projectLoader = new ProjectLoader();

        scene = new Scene(root, ApplicationConfiguration.DEFAULT_WIDTH, ApplicationConfiguration.DEFAULT_HEIGHT);

        scene.getStylesheets()
                .add("/celtech/resources/css/JMetroDarkTheme.css");
//        root.setStyle("-fx-font-family: FreeMono;");
        String primaryFontFamily = primaryFont.getFamily();
        root.setStyle("-fx-font-family: " + primaryFontFamily + ";");

        // Camera required to allow 2D shapes to be rotated in 3D in the '2D' UI
        PerspectiveCamera controlOverlaycamera = new PerspectiveCamera(false);

        scene.setCamera(controlOverlaycamera);

        mainStage.setScene(scene);

        loadProjectsAtStartup();

        root.layout();
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
     * @return
     */
    public static ResourceBundle getLanguageBundle()
    {
        return Lookup.getApplicationEnvironment().getLanguageBundle();
    }

    private void addGCode(Group gCodeParts)
    {
//        viewControl.addGCodeParts(gCodeParts);
    }

    /* 
     * GCode display controls
     */
    /**
     *
     * @param equalsIgnoreCase
     */
    public void showGCodeTravel(boolean equalsIgnoreCase)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param equalsIgnoreCase
     */
    public void showGCodeRetracts(boolean equalsIgnoreCase)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param equalsIgnoreCase
     */
    public void showGCodeSupport(boolean equalsIgnoreCase)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param intValue
     * @param i
     */
    public void changeVisibleGCodeLayers(int intValue, int i)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /*
    
     */
    /**
     *
     * @return
     */
    public ObservableList<String> gcodeFileLinesProperty()
    {
        return gcodeFileLines;
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
     *
     * @return
     */
    public final int getLayersInGCode()
    {
        return layersInGCode.get();
    }

    /**
     *
     * @return
     */
    public final IntegerProperty layersInGCodeProperty()
    {
        return layersInGCode;
    }

    /**
     *
     * @return
     */
    public final BooleanProperty noGCodeLoadedProperty()
    {
        return noGCodeLoaded;
    }

    /**
     *
     * @param modelsToLoad
     */
    public void loadExternalModels(List<File> modelsToLoad)
    {
        //Load the first one for the moment - we should deal with multiple loads in all cases
        loadExternalModel(modelsToLoad.get(0));
    }

    /**
     *
     * @param modelToLoad
     */
    public void loadExternalModel(File modelToLoad)
    {
        if (!modelLoaderService.isRunning() && tabDisplaySelectionModel.selectedItemProperty().get() instanceof ProjectTab)
        {
            String modelNameToLoad = modelToLoad.getName();
            if (modelNameToLoad.endsWith(ApplicationConfiguration.projectFileExtension))
            {
                ProjectTab currentProjectTab = ((ProjectTab) (tabDisplaySelectionModel.selectedItemProperty().get()));
                currentProjectTab.addProjectContainer(modelToLoad);
                tabDisplaySelectionModel.select(currentProjectTab);
            } else
            {
                modelLoaderService.reset();
                modelLoaderService.setModelFileToLoad(modelToLoad.getAbsolutePath());
                modelLoaderService.setShortModelName(modelNameToLoad);
                modelLoaderService.setTargetTab((ProjectTab) (tabDisplaySelectionModel.selectedItemProperty().get()));
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
        projectManager.saveState();

        tabDisplay.getTabs().stream().filter((tab) -> (tab instanceof ProjectTab)).forEach((tab) ->
        {
            ((ProjectTab) tab).saveProject();
        });
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
    public void selectModel(ModelContainer selectedModel)
    {
        Tab currentTab = tabDisplaySelectionModel.getSelectedItem();
        if (currentTab instanceof ProjectTab)
        {
            ((ProjectTab) currentTab).selectModel(selectedModel);
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
            if (subNode.getId().equalsIgnoreCase("Container") && subNode instanceof VBox)
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
        if (rhPanel.isSlidIn() && rhPanel.isSliding() == false)
        {
            rhPanel.startSlidingOut();
        }
    }

    public Locale getApplicationLocale()
    {
        return Lookup.getApplicationEnvironment().getAppLocale();
    }

    public Locale getUsersLocale()
    {
        return usersLocale;
    }

    /**
     * Return the font family name of the primary font used in the GUI
     *
     * @return
     */
    public String getPrimaryFontFamily()
    {
        return primaryFont.getFamily();
    }
}
