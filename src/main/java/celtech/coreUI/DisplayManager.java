/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI;

import celtech.CoreTest;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.ProjectManager;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.ProgressDialog;
import celtech.coreUI.components.ProjectLoader;
import celtech.coreUI.components.ProjectTab;
import celtech.coreUI.controllers.LayoutControlsController;
import celtech.coreUI.controllers.MenuStripController;
import celtech.coreUI.controllers.sidePanels.LayoutSidePanelController;
import celtech.coreUI.controllers.sidePanels.SettingsSidePanelController;
import celtech.coreUI.visualisation.CameraPositionPreset;
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
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import libertysystems.configuration.Configuration;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class DisplayManager implements EventHandler<KeyEvent>
{

    private static Stenographer steno = StenographerFactory.getStenographer(DisplayManager.class.getName());
    private static ApplicationStatus applicationStatus = ApplicationStatus.getInstance();
    private static ProjectManager projectManager = ProjectManager.getInstance();
    private static Configuration configuration = null;

    private static ResourceBundle i18nBundle = null;
    /*
    
     */
    private static DisplayManager instance = null;
    private static Stage mainStage = null;
    private static Scene scene = null;
    /*
     *
     */
    private static AnchorPane root = null;
    private HBox mainHolder = null;
    private StackPane sidePanelContainer = null;
    private AnchorPane modeSelectionControl = null;
    private final HashMap<ApplicationMode, SidePanel> sidePanels = new HashMap<>();
    private static TabPane tabDisplay = null;
    private MenuStripController menuStripController = null;
    private static SingleSelectionModel<Tab> tabDisplaySelectionModel = null;
    private static Tab printerStatusTab = null;
    private static Tab addPageTab = null;

    /*
     * Project loading
     */
    private FileChooser projectFileChooser = new FileChooser();
    private ProjectLoader projectLoader = null;
    /*
     * Mesh Model loading
     */
    private ModelLoaderService modelLoaderService = new ModelLoaderService();
    private ProgressDialog modelLoadDialog = null;
    private ModalDialog modelTooLargeDialog = null;

    /*
     * GCode model loading
     */
    private ArrayList<String> gcodeFileLinesBacking = new ArrayList<String>();
    private ObservableList<String> gcodeFileLines = FXCollections.observableArrayList();
    /*
     * GCode-related
     */
    private final IntegerProperty layersInGCode = new SimpleIntegerProperty(0);
    private final BooleanProperty noGCodeLoaded = new SimpleBooleanProperty(true);

    private AnchorPane statusAnchor = null;
    private HBox statusSubContainer = null;
    private VBox supplementaryStatusControlContainer = null;
    private Parent gcodeEntrySlideout = null;

    private DisplayManager()
    {

        Locale appLocale = Locale.getDefault();
        appLocale = Locale.forLanguageTag(ApplicationConfiguration.getApplicationLanguage());

        Font.loadFont(CoreTest.class.getResource(ApplicationConfiguration.fontResourcePath + "SourceSansPro-Light.ttf").toExternalForm(), 10);
        i18nBundle = ResourceBundle.getBundle("celtech.resources.i18n.LanguageData", appLocale);

        modelLoadDialog = new ProgressDialog(modelLoaderService);
        modelTooLargeDialog = new ModalDialog();

        /*
         * Configure the dialog for when the model is too large...
         */
        modelTooLargeDialog.setTitle(i18nBundle.getString("dialogs.ModelTooLargeTitle"));
        modelTooLargeDialog.setMessage(i18nBundle.getString("dialogs.ModelTooLargeDescription"));
        int modelShrinkToFit = modelTooLargeDialog.addButton(i18nBundle.getString("dialogs.ShrinkModelToFit"));
//        int modelCutToSize = modelTooLargeDialog.addButton(i18nBundle.getString("dialogs.CutModelToSize"));
        int dontLoadModel = modelTooLargeDialog.addButton(i18nBundle.getString("dialogs.ModelTooLargeNo"));

        modelLoaderService.setOnSucceeded((WorkerStateEvent t) ->
        {
            ModelLoadResult loadResult = (ModelLoadResult) modelLoaderService.getValue();

            if (loadResult != null)
            {
                if (loadResult.isModelTooLarge())
                {
                    int buttonPressed = modelTooLargeDialog.show();

                    if (buttonPressed == modelShrinkToFit)
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
        ArrayList<String> preloadedModelNames = pm.getOpenModelNames();
        for (String fileName : preloadedModelNames)
        {
            ProjectTab newProjectTab = new ProjectTab(instance, fileName + ApplicationConfiguration.projectFileExtension, tabDisplay.widthProperty(), tabDisplay.heightProperty());
            tabDisplay.getTabs().add(tabDisplay.getTabs().size() - 1, newProjectTab);
            tabDisplaySelectionModel.select(newProjectTab);
        }
    }

    private void switchPagesForMode(ApplicationMode oldMode, ApplicationMode newMode)
    {
        // Remove the existing side panel
        if (oldMode != null)
        {
            sidePanelContainer.getChildren().remove(sidePanels.get(oldMode).getSidePanelNode());
        }

        // Now add the relevant new one...
        sidePanelContainer.getChildren().add(sidePanels.get(newMode).getSidePanelNode());

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
                    tabDisplaySelectionModel.select(1);
                }
            }

            projectTab = (ProjectTab) tabDisplaySelectionModel.getSelectedItem();
            ((LayoutSidePanelController) (sidePanels.get(ApplicationMode.LAYOUT).getSidePanelController())).bindLoadedModels(projectTab.getThreeDViewManager());
            menuStripController.bindSelectedModels(projectTab.getSelectionContainer());
        } else if (newMode == ApplicationMode.STATUS)
        {
            tabDisplaySelectionModel.select(0);
        }
    }

    public static DisplayManager getInstance()
    {
        if (instance == null)
        {
            instance = new DisplayManager();
        }

        return instance;
    }

    public void configureDisplayManager(Stage mainStage, String applicationName)
    {
        this.mainStage = mainStage;
        mainStage.setTitle(applicationName + " - " + ApplicationConfiguration.getApplicationVersion());
        ApplicationConfiguration.setTitleAndVersion(i18nBundle.getString("application.title") + " - " + ApplicationConfiguration.getApplicationVersion());

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
            URL fxmlFileName = getClass().getResource(mode.getSidePanelFXMLName());
            steno.debug("About to load side panel fxml: " + fxmlFileName);
            try
            {
                FXMLLoader sidePanelLoader = new FXMLLoader(fxmlFileName, i18nBundle);
                HBox sidePanel = (HBox) sidePanelLoader.load();
                Object sidePanelController = sidePanelLoader.getController();
                sidePanel.setId(mode.name());
                sidePanels.put(mode, new SidePanel(sidePanel, sidePanelController));
            } catch (IOException ex)
            {
                steno.error("Couldn't load side panel for mode:" + mode + ". " + ex);
                System.out.println("Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        // Create a place to hang the side panels from
        sidePanelContainer = new StackPane();
        sidePanelContainer.getStyleClass().add("sidePanelContainer");
        HBox.setHgrow(sidePanelContainer, Priority.NEVER);

        mainHolder.getChildren().add(sidePanelContainer);

        VBox rhPanel = new VBox();
        rhPanel.setPrefSize(-1, -1);
        HBox.setHgrow(rhPanel, Priority.ALWAYS);
        mainHolder.getChildren().add(rhPanel);

        // Configure the main display tab pane - just the printer status page to start with
        tabDisplay = new TabPane();
        tabDisplay.setPickOnBounds(false);
        tabDisplay.setOnKeyPressed(this);
        tabDisplay.setTabMinHeight(30);
        tabDisplay.setTabMaxHeight(30);
        tabDisplaySelectionModel = tabDisplay.getSelectionModel();

        VBox.setVgrow(tabDisplay, Priority.ALWAYS);

        // The printer status tab will always be visible - the page is static
        try
        {
            Parent printerStatusPage = FXMLLoader.<Parent>load(getClass().getResource(ApplicationConfiguration.fxmlResourcePath + "PrinterStatusPage.fxml"), i18nBundle);
//            gcodeEntrySlideout = FXMLLoader.<Parent>load(getClass().getResource(ApplicationConfiguration.fxmlResourcePath + "GCodeEntryPanel.fxml"), i18nBundle);

            printerStatusTab = new Tab();
            printerStatusTab.setText(i18nBundle.getString("printerStatusTabTitle"));
            final Image printerStatusImage = new Image(CoreTest.class.getResource(ApplicationConfiguration.imageResourcePath + "PrinterStatusIndicator.png").toExternalForm());
            final ImageView printerStatusImageView = new ImageView();
            printerStatusImageView.setImage(printerStatusImage);
            printerStatusImageView.setPreserveRatio(true);
            printerStatusImageView.setFitHeight(28);
            printerStatusTab.setGraphic(printerStatusImageView);
            printerStatusTab.setClosable(false);
            printerStatusTab.setContent(printerStatusPage);
            tabDisplay.getTabs().add(printerStatusTab);

            addPageTab = new Tab();
            addPageTab.setText("+");
            addPageTab.setClosable(false);
            tabDisplay.getTabs().add(addPageTab);

            tabDisplaySelectionModel.selectedItemProperty().addListener((ObservableValue<? extends Tab> ov, Tab t, Tab t1) ->
            {
                if (t1 == addPageTab)
                {
                    ProjectTab projectTab = new ProjectTab(instance, tabDisplay.widthProperty(), tabDisplay.heightProperty());
                    tabDisplay.getTabs().add(tabDisplay.getTabs().size() - 1, projectTab);
                    tabDisplaySelectionModel.select(projectTab);
                } else if (t1 instanceof ProjectTab)
                {
                    if (applicationStatus.getMode() != ApplicationMode.LAYOUT)
                    {
                        applicationStatus.setMode(ApplicationMode.LAYOUT);
                    }

                    if (t != t1)
                    {
                        ProjectTab projectTab = (ProjectTab) tabDisplaySelectionModel.getSelectedItem();
                        ((LayoutSidePanelController) (sidePanels.get(ApplicationMode.LAYOUT).getSidePanelController())).bindLoadedModels(projectTab.getThreeDViewManager());
                        menuStripController.bindSelectedModels(projectTab.getSelectionContainer());
                        ((SettingsSidePanelController) (sidePanels.get(ApplicationMode.SETTINGS).getSidePanelController())).bindLoadedModels(projectTab.getProject());
                    }
                } else
                {
                    //Must have clicked on the status tab
                    if (applicationStatus.getMode() != ApplicationMode.STATUS)
                    {
                        applicationStatus.setMode(ApplicationMode.STATUS);
                    }
                }
            });

            rhPanel.getChildren().add(tabDisplay);

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
            FXMLLoader menuStripLoader = new FXMLLoader(menuStripURL, i18nBundle);
            BorderPane menuStripControls = (BorderPane) menuStripLoader.load();
            menuStripController = menuStripLoader.getController();
            menuStripControls.prefWidthProperty().bind(rhPanel.widthProperty());
            VBox.setVgrow(menuStripControls, Priority.NEVER);
            rhPanel.getChildren().add(menuStripControls);
        } catch (IOException ex)
        {
            steno.error("Failed to load menu strip controls:" + ex);
        }

        projectFileChooser.setTitle(i18nBundle.getString("dialogs.projectFileChooser"));
        projectFileChooser.getExtensionFilters()
                .addAll(
                        new FileChooser.ExtensionFilter(i18nBundle.getString("dialogs.projectFileChooserDescription"), "*." + ApplicationConfiguration.projectFileExtension));
        projectFileChooser.setInitialDirectory(new File(ApplicationConfiguration.getProjectDirectory()));

        projectLoader = new ProjectLoader();

        scene = new Scene(root, ApplicationConfiguration.DEFAULT_WIDTH, ApplicationConfiguration.DEFAULT_HEIGHT);

//        scene.getStylesheets()
//                .add(ApplicationConfiguration.mainCSSFile);
        scene.getStylesheets()
                .add("/celtech/resources/css/JMetroDarkTheme.css");

        // Camera required to allow 2D shapes to be rotated in 3D in the '2D' UI
        PerspectiveCamera controlOverlaycamera = new PerspectiveCamera(false);

        scene.setCamera(controlOverlaycamera);

        mainStage.setScene(scene);

        loadProjectsAtStartup();
    }

    public static Stage getMainStage()
    {
        return mainStage;
    }

    public static ResourceBundle getLanguageBundle()
    {
        return i18nBundle;
    }

    /*
    
     */
    private void addGCode(Group gCodeParts)
    {
//        viewControl.addGCodeParts(gCodeParts);
    }

    /* 
     * GCode display controls
     */
    public void showGCodeTravel(boolean equalsIgnoreCase)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void showGCodeRetracts(boolean equalsIgnoreCase)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void showGCodeSupport(boolean equalsIgnoreCase)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void changeVisibleGCodeLayers(int intValue, int i)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /*
    
     */
    public ObservableList<String> gcodeFileLinesProperty()
    {
        return gcodeFileLines;
    }

    public final void setLayersInGCode(int value)
    {
        layersInGCode.set(value);
    }

    public final int getLayersInGCode()
    {
        return layersInGCode.get();
    }

    public final IntegerProperty layersInGCodeProperty()
    {
        return layersInGCode;
    }

    public final BooleanProperty noGCodeLoadedProperty()
    {
        return noGCodeLoaded;
    }

    public void loadExternalModels(List<File> modelsToLoad)
    {
        //Load the first one for the moment - we should deal with multiple loads in all cases
        loadExternalModel(modelsToLoad.get(0));
    }

    public void loadExternalModel(File modelToLoad)
    {
        if (!modelLoaderService.isRunning() && tabDisplaySelectionModel.selectedItemProperty().get() instanceof ProjectTab)
        {
            String modelNameToLoad = modelToLoad.getName();
            if (modelNameToLoad.endsWith(ApplicationConfiguration.projectFileExtension))
            {
                ProjectTab currentProjectTab = ((ProjectTab) (tabDisplaySelectionModel.selectedItemProperty().get()));
                currentProjectTab.addProjectContainer(modelNameToLoad);
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

    public ReadOnlyBooleanProperty modelLoadingProperty()
    {
        return modelLoaderService.runningProperty();
    }

    public void shutdown()
    {
        projectManager.saveState();

        tabDisplay.getTabs().stream().filter((tab) -> (tab instanceof ProjectTab)).forEach((tab) ->
        {
            ((ProjectTab) tab).saveProject();
        });
    }

    public void deleteSelectedModels()
    {
        Tab currentTab = tabDisplaySelectionModel.selectedItemProperty().get();
        if (currentTab instanceof ProjectTab)
        {
            ((ProjectTab) currentTab).deleteSelectedModels();
        }
    }

    public void copySelectedModels()
    {
        Tab currentTab = tabDisplaySelectionModel.selectedItemProperty().get();
        if (currentTab instanceof ProjectTab)
        {
            ((ProjectTab) currentTab).copySelectedModels();
        }
    }

    public void autoLayout()
    {
        Tab currentTab = tabDisplaySelectionModel.getSelectedItem();
        if (currentTab instanceof ProjectTab)
        {
            ((ProjectTab) currentTab).autoLayout();
        }
    }

    public void activateSnapToGround()
    {
        Tab currentTab = tabDisplaySelectionModel.getSelectedItem();
        if (currentTab instanceof ProjectTab)
        {
            ((ProjectTab) currentTab).getThreeDViewManager().activateSnapToGround();
        }
    }

    public void selectModel(ModelContainer selectedModel)
    {
        Tab currentTab = tabDisplaySelectionModel.getSelectedItem();
        if (currentTab instanceof ProjectTab)
        {
            ((ProjectTab) currentTab).selectModel(selectedModel);
        }
    }

    public void deselectModel(ModelContainer selectedModel)
    {
        Tab currentTab = tabDisplaySelectionModel.getSelectedItem();
        if (currentTab instanceof ProjectTab)
        {
            ((ProjectTab) currentTab).deselectModel(selectedModel);
        }
    }

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

    public Project getCurrentlyVisibleProject()
    {
        Tab currentTab = tabDisplaySelectionModel.getSelectedItem();
        if (currentTab instanceof ProjectTab)
        {
            return ((ProjectTab) currentTab).getProject();
        } else
        {
            return null;
        }
    }

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
                    case BACK_SPACE:
                        projectTab.deleteSelectedModels();
                        break;
                    case Z:
                        projectTab.switchToPresetCameraView(CameraPositionPreset.FRONT);
                        break;
                    default:
                        break;
                }
            }
        }
    }
    
    public VBox getSidePanelSlideOutHandle(ApplicationMode mode)
    {
         return sidePanels.get(mode).getSlideOutContainer();         
    }

    public void showAdvancedStatusPanel(boolean showPanel)
    {
//        if (showPanel)
//        {
//            if (statusSubContainer.getChildren().contains(supplementaryStatusControlContainer) == false)
//            {
//                statusSubContainer.getChildren().add(0, supplementaryStatusControlContainer);
//            }
//        } else
//        {
//            if (statusSubContainer.getChildren().contains(supplementaryStatusControlContainer) == true)
//            {
//                statusSubContainer.getChildren().remove(supplementaryStatusControlContainer);
//            }
//        }
    }
}
