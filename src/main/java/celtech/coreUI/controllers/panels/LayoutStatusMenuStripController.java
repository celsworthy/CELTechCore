package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.Project.ProjectChangesListener;
import celtech.appManager.ProjectMode;
import celtech.appManager.PurgeResponse;
import celtech.appManager.undo.CommandStack;
import celtech.appManager.undo.UndoableProject;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.DirectoryMemoryProperty;
import celtech.configuration.Filament;
import celtech.configuration.PrinterColourMap;
import celtech.coreUI.AmbientLEDState;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.ProjectGUIState;
import celtech.coreUI.components.buttons.GraphicButtonWithLabel;
import celtech.coreUI.components.buttons.GraphicToggleButtonWithLabel;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.coreUI.visualisation.ModelLoader;
import celtech.coreUI.visualisation.SelectedModelContainers;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.model.CanPrintConditionalTextBindings;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import celtech.utils.PrinterUtils;
import celtech.utils.tasks.TaskResponse;
import java.io.File;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class LayoutStatusMenuStripController implements PrinterListChangesListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        LayoutStatusMenuStripController.class.getName());
    private PrinterSettings printerSettings = null;
    private ApplicationStatus applicationStatus = null;
    private DisplayManager displayManager = null;
    private final FileChooser modelFileChooser = new FileChooser();
    private PrinterUtils printerUtils = null;
    private final PrinterColourMap colourMap = PrinterColourMap.getInstance();

    private final IntegerProperty currentNozzle = new SimpleIntegerProperty(0);

    private final BooleanProperty canPrintProject = new SimpleBooleanProperty(false);

    @FXML
    private GraphicButtonWithLabel undoButton;
    
    @FXML
    private GraphicButtonWithLabel redoButton;    

    @FXML
    private GraphicButtonWithLabel backwardFromSettingsButton;

    @FXML
    private GraphicButtonWithLabel backwardFromLayoutButton;

    @FXML
    private GraphicButtonWithLabel calibrateButton;

    @FXML
    private GraphicButtonWithLabel forwardButtonSettings;

    @FXML
    private GraphicButtonWithLabel forwardButtonLayout;

    @FXML
    private GraphicButtonWithLabel unlockDoorButton;

    @FXML
    private GraphicButtonWithLabel ejectFilamentButton;

    @FXML
    private GraphicButtonWithLabel fineNozzleButton;

    @FXML
    private GraphicButtonWithLabel fillNozzleButton;

    @FXML
    private GraphicButtonWithLabel openNozzleButton;

    @FXML
    private GraphicButtonWithLabel closeNozzleButton;

    @FXML
    private GraphicButtonWithLabel homeButton;

    @FXML
    private GraphicButtonWithLabel removeHeadButton;

    @FXML
    private GraphicToggleButtonWithLabel headFanButton;

    @FXML
    private GraphicButtonWithLabel headLightsButton;

    @FXML
    private GraphicButtonWithLabel ambientLightsButton;

    @FXML
    private GraphicButtonWithLabel printButton;

    @FXML
    private FlowPane layoutButtonHBox;

    @FXML
    private FlowPane statusButtonHBox;

    @FXML
    private GraphicButtonWithLabel addModelButton;

    @FXML
    private GraphicButtonWithLabel deleteModelButton;

    @FXML
    private GraphicButtonWithLabel duplicateModelButton;

    @FXML
    private GraphicButtonWithLabel distributeModelsButton;

    @FXML
    private GraphicToggleButtonWithLabel snapToGroundButton;
    private Project selectedProject;
    private UndoableProject undoableSelectedProject;
    private ObjectProperty<LayoutSubmode> layoutSubmode;
    private SelectedModelContainers modelSelection;
    private final ModelLoader modelLoader = new ModelLoader();

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
    void printPressed(ActionEvent event)
    {
        Printer printer = printerSettings.getSelectedPrinter();

        Project currentProject = Lookup.getSelectedProjectProperty().get();

        PurgeResponse purgeConsent = printerUtils.offerPurgeIfNecessary(printer, currentProject);

        try
        {
            if (purgeConsent == PurgeResponse.PRINT_WITH_PURGE)
            {
                displayManager.getPurgeInsetPanelController().purgeAndPrint(
                    currentProject,
                    printerSettings.getFilament0(),
                    printerSettings.getPrintQuality(),
                    printerSettings.getSettings(), printer);
            } else if (purgeConsent == PurgeResponse.PRINT_WITHOUT_PURGE)
            {
                currentStatusPrinter.resetPurgeTemperature(printerSettings);
                printer.printProject(currentProject, printerSettings.getFilament0(),
                                     printerSettings.getPrintQuality(),
                                     printerSettings.getSettings());
                applicationStatus.setMode(ApplicationMode.STATUS);
            } else if (purgeConsent == PurgeResponse.NOT_NECESSARY)
            {
                printer.printProject(currentProject, printerSettings.getFilament0(),
                                     printerSettings.getPrintQuality(),
                                     printerSettings.getSettings());
                applicationStatus.setMode(ApplicationMode.STATUS);
            }
        } catch (PrinterException ex)
        {
            steno.error("Error during print project " + ex.getMessage());
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
    void calibrate(ActionEvent event)
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.CALIBRATION_CHOICE);
    }

    @FXML
    void register(ActionEvent event)
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.REGISTRATION);
    }

    @FXML
    void addModel(ActionEvent event)
    {
        Platform.runLater(() ->
        {
            List<File> files = selectFiles();

            if (files != null && !files.isEmpty())
            {
                ApplicationConfiguration.setLastDirectory(
                    DirectoryMemoryProperty.MODEL,
                    files.get(0).getParentFile().getAbsolutePath());
                modelLoader.loadExternalModels(selectedProject, files, true);
            }
        });
    }

    /**
     * Allow the user to select the files they want to load.
     */
    private List<File> selectFiles()
    {
        ListIterator iterator = modelFileChooser.getExtensionFilters().listIterator();
        while (iterator.hasNext())
        {
            iterator.next();
            iterator.remove();
        }
        String descriptionOfFile = Lookup.i18n("dialogs.meshFileChooserDescription");

        modelFileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(descriptionOfFile,
                                            ApplicationConfiguration.
                                            getSupportedFileExtensionWildcards(
                                                ProjectMode.MESH)));
        modelFileChooser.setInitialDirectory(new File(ApplicationConfiguration.getLastDirectory(
            DirectoryMemoryProperty.MODEL)));
        List<File> files;

        files = modelFileChooser.showOpenMultipleDialog(displayManager.getMainStage());

        return files;
    }

    @FXML
    void undo(ActionEvent event)
    {
        CommandStack commandStack = Lookup.getProjectGUIState(selectedProject).getCommandStack();
        if (commandStack.getCanUndo().get())
        {
            try
            {
                commandStack.undo();
            } catch (CommandStack.UndoException ex)
            {
                steno.error("Unable to undo: " + ex);
            }
        }
    }

    @FXML
    void redo(ActionEvent event)
    {
        CommandStack commandStack = Lookup.getProjectGUIState(selectedProject).getCommandStack();
        if (commandStack.getCanRedo().get())
        {
            try
            {
                commandStack.redo();
            } catch (CommandStack.UndoException ex)
            {
                steno.error("Unable to redo: " + ex);
            }
        }
    }

    @FXML
    void addCloudModel(ActionEvent event)
    {
        applicationStatus.modeProperty().set(ApplicationMode.MY_MINI_FACTORY);
    }

    @FXML
    void deleteModel(ActionEvent event)
    {
        undoableSelectedProject.deleteModels(modelSelection.getSelectedModelsSnapshot());
    }

    @FXML
    void copyModel(ActionEvent event)
    {
        undoableSelectedProject.copyModels(modelSelection.getSelectedModelsSnapshot());
    }

    @FXML
    void autoLayoutModels(ActionEvent event)
    {
        undoableSelectedProject.autoLayout();
    }

    @FXML
    void snapToGround(ActionEvent event)
    {
        layoutSubmode.set(LayoutSubmode.SNAP_TO_GROUND);
    }

    @FXML
    void unlockDoor(ActionEvent event)
    {
        boolean goAheadAndOpenTheDoor = false;

        if (currentStatusPrinter.getPrinterAncillarySystems().bedTemperatureProperty().get() > 60)
        {
            if (Lookup.getUserPreferences().isSafetyFeaturesOn() == false)
            {
                try
                {
                    currentStatusPrinter.goToOpenDoorPositionDontWait(null);
                } catch (PrinterException ex)
                {
                    steno.error("Error opening door " + ex.getMessage());
                }
            } else
            {
                goAheadAndOpenTheDoor = Lookup.getSystemNotificationHandler().showOpenDoorDialog();

                if (goAheadAndOpenTheDoor)
                {
                    try
                    {
                        currentStatusPrinter.goToOpenDoorPosition(null);
                    } catch (PrinterException ex)
                    {
                        steno.error("Error opening door " + ex.getMessage());
                    }
                }
            }
        } else
        {
            try
            {
                currentStatusPrinter.goToOpenDoorPosition(null);
            } catch (PrinterException ex)
            {
                steno.error("Error opening door " + ex.getMessage());
            }
        }
    }

    @FXML
    void ejectFilament0(ActionEvent event)
    {
        //TODO modify for multiple extruders
        try
        {
            currentStatusPrinter.ejectFilament(0, null);
        } catch (PrinterException ex)
        {
            steno.error("Error when sending eject filament - " + ex.getMessage());
        }
    }

    @FXML
    void selectNozzle0(ActionEvent event)
    {
        try
        {
            currentStatusPrinter.selectNozzle(0);
            currentNozzle.set(0);
        } catch (PrinterException ex)
        {
            steno.error("Error when selecting nozzle 0" + ex.getMessage());
        }
    }

    @FXML
    void selectNozzle1(ActionEvent event)
    {
        try
        {
            currentStatusPrinter.selectNozzle(1);
            currentNozzle.set(1);
        } catch (PrinterException ex)
        {
            steno.error("Error when selecting nozzle 1" + ex.getMessage());
        }
    }

    @FXML
    void openNozzle(ActionEvent event)
    {
        try
        {
            currentStatusPrinter.openNozzleFully();
        } catch (PrinterException ex)
        {
            steno.error("Error when opening nozzle" + ex.getMessage());
        }
    }

    @FXML
    void closeNozzle(ActionEvent event)
    {
        try
        {
            currentStatusPrinter.closeNozzleFully();
        } catch (PrinterException ex)
        {
            steno.error("Error when closing nozzle" + ex.getMessage());
        }
    }

    @FXML
    void homeAll(ActionEvent event)
    {
        try
        {
            currentStatusPrinter.executeMacro("Home_all");
        } catch (PrinterException ex)
        {
            steno.error("Couldn't run home macro");
        }
    }

    @FXML
    void toggleHeadFan(ActionEvent event)
    {
        try
        {
            if (currentStatusPrinter.getPrinterAncillarySystems().headFanOnProperty().get())
            {
                currentStatusPrinter.switchOffHeadFan();
            } else
            {
                currentStatusPrinter.switchOnHeadFan();
            }
        } catch (PrinterException ex)
        {
            steno.error("Failed to send head fan command - " + ex.getMessage());
        }
    }

    boolean headLEDOn = false;

    @FXML
    void toggleHeadLights(ActionEvent event)
    {
        try
        {
            if (headLEDOn == true)
            {
                currentStatusPrinter.switchOffHeadLEDs();
                headLEDOn = false;
            } else
            {
                currentStatusPrinter.switchOnHeadLEDs();
                headLEDOn = true;
            }
        } catch (PrinterException ex)
        {
            steno.error("Failed to send head LED command - " + ex.getMessage());
        }
    }

    private AmbientLEDState ambientLEDState = AmbientLEDState.COLOUR;

    @FXML
    void toggleAmbientLights(ActionEvent event)
    {
        try
        {
            // Off, White, Colour
            ambientLEDState = ambientLEDState.getNextState();

            switch (ambientLEDState)
            {
                case OFF:
                    currentStatusPrinter.setAmbientLEDColour(Color.BLACK);
                    break;
                case WHITE:
                    currentStatusPrinter.setAmbientLEDColour(
                        colourMap.displayToPrinterColour(Color.WHITE));
                    break;
                case COLOUR:
                    currentStatusPrinter.setAmbientLEDColour(
                        currentStatusPrinter.getPrinterIdentity().printerColourProperty().get());
                    break;
            }
        } catch (PrinterException ex)
        {
            steno.error("Failed to send ambient LED command");
        }
    }

    @FXML
    void removeHead(ActionEvent event)
    {
        try
        {
            currentStatusPrinter.removeHead((TaskResponse taskResponse) ->
            {
                removeHeadFinished(taskResponse);
            });
        } catch (PrinterException ex)
        {
            steno.error("PrinterException whilst invoking remove head: " + ex.getMessage());
        }
    }

    private void removeHeadFinished(TaskResponse taskResponse)
    {
        if (taskResponse.succeeded())
        {
            Lookup.getSystemNotificationHandler().showInformationNotification(Lookup.i18n(
                "removeHead.title"), Lookup.i18n("removeHead.finished"));
            steno.debug("Head remove completed");
        } else
        {
            Lookup.getSystemNotificationHandler().showWarningNotification(Lookup.i18n(
                "removeHead.title"), Lookup.i18n("removeHead.failed"));
        }
    }

    /**
     * The printer selected on the Status screen.
     */
    private Printer currentStatusPrinter = null;
    /**
     * The printer selected on the Settings screen.
     */
    private Printer currentSettingsPrinter = null;
    private final BooleanProperty printerAvailable = new SimpleBooleanProperty(false);

    private final ChangeListener<Boolean> headFanStatusListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
        headFanButton.setSelected(newValue);
    };

    /*
     * JavaFX initialisation method
     */
    @FXML
    void initialize()
    {
        displayManager = DisplayManager.getInstance();
        applicationStatus = ApplicationStatus.getInstance();
        printerUtils = PrinterUtils.getInstance();

        statusButtonHBox.setVisible(false);

        createStatusPrinterListener();

        printButton.installTag();

        printButton.disableProperty().bind(canPrintProject.not());

        setupButtonVisibility();

        Lookup.getSelectedProjectProperty().addListener(
            (ObservableValue<? extends Project> observable, Project oldValue, Project newValue) ->
            {
                whenProjectChanges(newValue);
            });

        Lookup.getPrinterListChangesNotifier().addListener(this);
    }

    private void setupButtonVisibility()
    {

        backwardFromLayoutButton.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(
            ApplicationMode.LAYOUT));
        backwardFromSettingsButton.visibleProperty().bind(applicationStatus.modeProperty().
            isEqualTo(ApplicationMode.SETTINGS));

        printButton.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(
            ApplicationMode.SETTINGS));

        closeNozzleButton.setVisible(false);
        fillNozzleButton.setVisible(false);

        // Prevent the status bar affecting layout when it is invisible
        statusButtonHBox.visibleProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(
                ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                steno.info("Status box managed=" + newValue.booleanValue());
                statusButtonHBox.setManaged(newValue);
            }
        });

        // Prevent the layout bar affecting layout when it is invisible
        layoutButtonHBox.visibleProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(
                ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                steno.info("Layout box managed=" + newValue.booleanValue());
                layoutButtonHBox.setManaged(newValue);
            }
        });

        statusButtonHBox.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(
            ApplicationMode.STATUS)
            .and(printerAvailable));
        layoutButtonHBox.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(
            ApplicationMode.LAYOUT));
        modelFileChooser.setTitle(Lookup.i18n("dialogs.modelFileChooser"));
        modelFileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(Lookup.i18n(
                    "dialogs.modelFileChooserDescription"),
                                            ApplicationConfiguration.
                                            getSupportedFileExtensionWildcards(
                                                ProjectMode.NONE)));

        forwardButtonSettings.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(
            ApplicationMode.LAYOUT));
        forwardButtonLayout.visibleProperty().bind((applicationStatus.modeProperty().isEqualTo(
            ApplicationMode.STATUS)));
    }

    ChangeListener<Printer> printerSettingsListener = (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
    {
        currentSettingsPrinter = newValue;
        if (newValue != null)
        {
            whenProjectOrSettingsPrinterChange();
        } else
        {
            printButton.disableProperty().unbind();
            printButton.setDisable(true);
        }
    };

    /**
     * Create the bindings for when tied to the SettingsScreen.
     */
    private void createPrinterSettingsListener(PrinterSettings printerSettings)
    {
        if (printerSettings != null)
        {
            printerSettings.selectedPrinterProperty().addListener(printerSettingsListener);
        }
    }

    private void updatePrintButtonConditionalText(Printer printer, Project project)
    {
        System.out.println("update conditional text");
        
        if (printer == null)
        {
            return;
        }

        printButton.getTag().removeAllConditionalText();

        printButton.getTag().addConditionalText("dialogs.cantPrintDoorIsOpenMessage",
                                                printer.getPrinterAncillarySystems().
                                                lidOpenProperty()
                                                .and(Lookup.getUserPreferences().
                                                    safetyFeaturesOnProperty()));
        
        printButton.getTag().addConditionalText("dialogs.chooseACustomProfile",
                                                project.customSettingsNotChosenProperty());

        CanPrintConditionalTextBindings conditionalTextBindings
            = new CanPrintConditionalTextBindings(project, printer);
        BooleanBinding extruder0FilamentMismatch = conditionalTextBindings.getExtruder0FilamentMismatch();
        BooleanBinding extruder1FilamentMismatch = conditionalTextBindings.getExtruder1FilamentMismatch();
        BooleanBinding filament0Reqd = conditionalTextBindings.getFilament0Required();
        BooleanBinding filament1Reqd = conditionalTextBindings.getFilament1Required();

        BooleanBinding oneExtruderPrinter = printer.extrudersProperty().get(1).isFittedProperty().not();
        BooleanBinding twoExtruderPrinter = printer.extrudersProperty().get(1).isFittedProperty().not().not();
        BooleanBinding noFilament0Selected = project.getPrinterSettings().getFilament0Property().isNull();
        BooleanBinding noFilament1Selected = project.getPrinterSettings().getFilament1Property().isNull();

        printButton.getTag().addConditionalText("dialogs.cantPrintNoFilamentSelectedMessage",
                                                oneExtruderPrinter.and(filament0Reqd).and(
                                                    noFilament0Selected));
        printButton.getTag().addConditionalText("dialogs.cantPrintNoFilamentMessage",
                                                oneExtruderPrinter.and(filament0Reqd).and(
                                                    printer.extrudersProperty().get(0).
                                                    filamentLoadedProperty().not()));
        printButton.getTag().addConditionalText("dialogs.filamentMismatchMessage",
                                                oneExtruderPrinter.and(extruder0FilamentMismatch));

        printButton.getTag().addConditionalText("dialogs.cantPrintNoFilamentSelectedMessage0",
                                                twoExtruderPrinter.and(filament0Reqd).and(
                                                    noFilament0Selected));
        printButton.getTag().addConditionalText("dialogs.cantPrintNoFilamentMessage0",
                                                twoExtruderPrinter.and(filament0Reqd).and(
                                                    printer.extrudersProperty().get(0).
                                                    filamentLoadedProperty().not()));
        printButton.getTag().addConditionalText("dialogs.filament0MismatchMessage",
                                                twoExtruderPrinter.and(extruder0FilamentMismatch));
        printButton.getTag().addConditionalText("dialogs.cantPrintNoFilamentSelectedMessage1",
                                                twoExtruderPrinter.and(filament1Reqd).and(
                                                    noFilament1Selected));
        printButton.getTag().addConditionalText("dialogs.cantPrintNoFilamentMessage1",
                                                twoExtruderPrinter.and(filament1Reqd).and(
                                                    printer.extrudersProperty().get(1).
                                                    filamentLoadedProperty().not()));
        printButton.getTag().addConditionalText("dialogs.filament1MismatchMessage",
                                                twoExtruderPrinter.and(filament1Reqd).and(
                                                    extruder1FilamentMismatch));
    }

    /**
     * Create the bindings to the Status selected printer.
     */
    private void createStatusPrinterListener()
    {
        currentStatusPrinter = Lookup.getCurrentlySelectedPrinterProperty().get();

        Lookup.getCurrentlySelectedPrinterProperty().addListener(
            (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
            {
                if (newValue != null)
                {
                    printerAvailable.set(true);

                    if (currentStatusPrinter != null)
                    {
                        unlockDoorButton.disableProperty().unbind();
                        ejectFilamentButton.disableProperty().unbind();
                        fineNozzleButton.visibleProperty().unbind();
                        fillNozzleButton.visibleProperty().unbind();
                        openNozzleButton.visibleProperty().unbind();
                        closeNozzleButton.visibleProperty().unbind();
                        fineNozzleButton.disableProperty().unbind();
                        fillNozzleButton.disableProperty().unbind();
                        openNozzleButton.disableProperty().unbind();
                        closeNozzleButton.disableProperty().unbind();
                        homeButton.disableProperty().unbind();
                        currentStatusPrinter.getPrinterAncillarySystems().headFanOnProperty().
                        removeListener(headFanStatusListener);
                        headLightsButton.disableProperty().unbind();
                        ambientLightsButton.disableProperty().unbind();
                        calibrateButton.disableProperty().unbind();
                        removeHeadButton.disableProperty().unbind();

                        printButton.getTag().removeAllConditionalText();
                    }

                    unlockDoorButton.disableProperty().bind(newValue.canOpenDoorProperty().not());
                    ejectFilamentButton.disableProperty().bind(newValue.extrudersProperty().get(0).
                        canEjectProperty().not());

                    // These buttons should only be available in advanced mode
                    fineNozzleButton.disableProperty().bind(
                        newValue.canOpenCloseNozzleProperty().not()
                        .or(Lookup.getUserPreferences().advancedModeProperty().not()));
                    fillNozzleButton.disableProperty().bind(
                        newValue.canOpenCloseNozzleProperty().
                        not().or(Lookup.getUserPreferences().advancedModeProperty().not()));
                    openNozzleButton.disableProperty().bind(
                        newValue.canOpenCloseNozzleProperty().not()
                        .or(Lookup.getUserPreferences().advancedModeProperty().not()));
                    closeNozzleButton.disableProperty().bind(
                        newValue.canOpenCloseNozzleProperty().not()
                        .or(Lookup.getUserPreferences().advancedModeProperty().not()));
                    homeButton.disableProperty().bind(newValue.canPrintProperty().not()
                        .or(Lookup.getUserPreferences().advancedModeProperty().not()));

                    newValue.getPrinterAncillarySystems().headFanOnProperty().addListener(
                        headFanStatusListener);

                    calibrateButton.disableProperty()
                    .bind(newValue.canCalibrateHeadProperty().not());
                    removeHeadButton.disableProperty().bind(newValue.canPrintProperty().not());

                    currentStatusPrinter = newValue;

                } else
                {
                    printerAvailable.set(false);
                }
            });
    }

    private final ChangeListener<LayoutSubmode> layoutSubmodeListener = (ObservableValue<? extends LayoutSubmode> observable, LayoutSubmode oldValue, LayoutSubmode newValue) ->
    {
        if (newValue != LayoutSubmode.SNAP_TO_GROUND)
        {
            snapToGroundButton.selectedProperty().set(false);
        }
    };

    ProjectChangesListener projectChangesListener = new ProjectChangesListener()
    {

        @Override
        public void whenModelAdded(ModelContainer modelContainer)
        {
            whenProjectOrSettingsPrinterChange();
        }

        @Override
        public void whenModelRemoved(ModelContainer modelContainer)
        {
            whenProjectOrSettingsPrinterChange();
        }

        @Override
        public void whenAutoLaidOut()
        {
        }

        @Override
        public void whenModelsTransformed(Set<ModelContainer> modelContainers)
        {
        }

        @Override
        public void whenModelChanged(ModelContainer modelContainer, String propertyName)
        {
            whenProjectOrSettingsPrinterChange();
        }

        @Override
        public void whenPrinterSettingsChanged(PrinterSettings printerSettings)
        {
            whenProjectOrSettingsPrinterChange();
        }
    };

    private void unbindProject(Project project)
    {
        printerSettings.selectedPrinterProperty().removeListener(printerSettingsListener);
        layoutSubmode.removeListener(layoutSubmodeListener);
        project.removeProjectChangesListener(projectChangesListener);
        undoButton.disableProperty().unbind();
        redoButton.disableProperty().unbind();
    }

    private void bindProject(Project project)
    {
        createPrinterSettingsListener(printerSettings);
        bindSelectedModels(project);

        if (currentSettingsPrinter != null && project != null)
        {
            whenProjectOrSettingsPrinterChange();
        }

        layoutSubmode.addListener(layoutSubmodeListener);
        project.addProjectChangesListener(projectChangesListener);
        
        undoButton.disableProperty().bind(Lookup.getProjectGUIState(project).getCommandStack().getCanUndo().not());
        redoButton.disableProperty().bind(Lookup.getProjectGUIState(project).getCommandStack().getCanRedo().not());

    }

    private void whenProjectOrSettingsPrinterChange()
    {
        updateCanPrintProjectBindings(currentSettingsPrinter, selectedProject);
        updatePrintButtonConditionalText(currentSettingsPrinter, selectedProject);
    }

    /**
     * This must be called whenever the project is changed.
     *
     * @param projectTab
     */
    public void whenProjectChanges(Project project)
    {
        if (selectedProject != null)
        {
            unbindProject(selectedProject);
        }
        selectedProject = project;
        undoableSelectedProject = new UndoableProject(project);
        printerSettings = project.getPrinterSettings();
        currentSettingsPrinter = printerSettings.getSelectedPrinter();
        modelSelection = Lookup.getProjectGUIState(project).getSelectedModelContainers();
        layoutSubmode = Lookup.getProjectGUIState(project).getLayoutSubmodeProperty();

        bindProject(project);

    }

    /**
     * This should be called whenever the printer or project changes and updates the bindings for
     * the canPrintProject property.
     */
    private void updateCanPrintProjectBindings(Printer printer, Project project)
    {
        PrinterSettings printerSettings = project.getPrinterSettings();
        if (project == null || printer == null)
        {
            return;
        }
        printButton.disableProperty().unbind();
        if (!printer.extrudersProperty().get(1).isFittedProperty().get()) // only one extruder
        {
            canPrintProject.bind(
                printer.canPrintProperty()
                .and(project.canPrintProperty())    
                .and(printerSettings.getFilament0Property().isNotNull())
                .and(printer.getPrinterAncillarySystems().lidOpenProperty().not()
                    .or(Lookup.getUserPreferences().safetyFeaturesOnProperty().not()))
                .and(printer.extrudersProperty().get(0).filamentLoadedProperty())
            );
        } else // this printer has two extruders
        {
            if (project.allModelsOnSameExtruder())
            {
                // only one extruder required, which one is it?
                int extruderNumber = project.getUsedExtruders().iterator().next();
                ObjectProperty<Filament> requiredFilamentProperty = null;
                if (extruderNumber == 0)
                {
                    requiredFilamentProperty = printerSettings.getFilament0Property();
                } else
                {
                    requiredFilamentProperty = printerSettings.getFilament1Property();
                }

                canPrintProject.bind(
                    printer.canPrintProperty()
                    .and(project.canPrintProperty())    
                    .and(requiredFilamentProperty.isNotNull())
                    .and(printer.getPrinterAncillarySystems().lidOpenProperty().not()
                        .or(Lookup.getUserPreferences().safetyFeaturesOnProperty().not()))
                    .and(printer.extrudersProperty().get(extruderNumber).
                        filamentLoadedProperty())
                );
            } else // both extruders are required
            {
                canPrintProject.bind(
                    printer.canPrintProperty()
                    .and(project.canPrintProperty())    
                    .and(printerSettings.getFilament0Property().isNotNull())
                    .and(printerSettings.getFilament1Property().isNotNull())
                    .and(printer.getPrinterAncillarySystems().lidOpenProperty().not()
                        .or(Lookup.getUserPreferences().safetyFeaturesOnProperty().not()))
                    .and(printer.extrudersProperty().get(0).filamentLoadedProperty())
                    .and(printer.extrudersProperty().get(1).filamentLoadedProperty())
                );

            }
        }
        printButton.disableProperty().bind(canPrintProject.not());
    }

    /**
     * Binds button disabled properties to the selection container This disables and enables buttons
     * depending on whether a model is selected.
     */
    private void bindSelectedModels(Project project)
    {
        ProjectGUIState projectGUIState = Lookup.getProjectGUIState(project);
        SelectedModelContainers selectionModel = projectGUIState.getSelectedModelContainers();
        ReadOnlyObjectProperty<LayoutSubmode> layoutSubmodeProperty = projectGUIState.
            getLayoutSubmodeProperty();

        addModelButton.disableProperty().unbind();
        deleteModelButton.disableProperty().unbind();
        duplicateModelButton.disableProperty().unbind();
        snapToGroundButton.disableProperty().unbind();
        distributeModelsButton.disableProperty().unbind();

        BooleanBinding notSelectModeOrNoSelectedModels
            = Bindings.notEqual(LayoutSubmode.SELECT, layoutSubmodeProperty).or(
                Bindings.equal(0, selectionModel.getNumModelsSelectedProperty()));
        BooleanBinding notSelectModeOrNoLoadedModels
            = Bindings.notEqual(LayoutSubmode.SELECT, layoutSubmodeProperty).or(
                Bindings.isEmpty(project.getLoadedModels()));
        BooleanBinding snapToGround
            = Bindings.equal(LayoutSubmode.SNAP_TO_GROUND, layoutSubmodeProperty);
        BooleanBinding noLoadedModels = Bindings.isEmpty(project.getLoadedModels());
        deleteModelButton.disableProperty().bind(notSelectModeOrNoSelectedModels);
        duplicateModelButton.disableProperty().bind(notSelectModeOrNoSelectedModels);
        distributeModelsButton.setDisable(true);

        addModelButton.disableProperty().bind(snapToGround);

        distributeModelsButton.disableProperty().bind(notSelectModeOrNoLoadedModels);
        snapToGroundButton.disableProperty().bind(noLoadedModels);

        ChangeListener<LayoutSubmode> whenSubModeChanges
            = (ObservableValue<? extends LayoutSubmode> ov, LayoutSubmode oldMode, LayoutSubmode newMode) ->
            {
                if (oldMode.equals(LayoutSubmode.SNAP_TO_GROUND) && newMode.equals(
                    LayoutSubmode.SELECT))
                {
                    snapToGroundButton.setSelected(false);
                }
            };
        layoutSubmodeProperty.addListener(whenSubModeChanges);

    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
        if (printer == currentStatusPrinter)
        {
            openNozzleButton.visibleProperty().bind(
                printer.headProperty().get().bPositionProperty().lessThan(0.5));
            closeNozzleButton.visibleProperty().bind(printer.headProperty().get().
                bPositionProperty().greaterThan(0.5));
            fineNozzleButton.visibleProperty().bind(printer.headProperty().get().
                nozzleInUseProperty().isEqualTo(1));
            fillNozzleButton.visibleProperty().bind(printer.headProperty().get().
                nozzleInUseProperty().isEqualTo(0));
        }
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        openNozzleButton.visibleProperty().unbind();
        openNozzleButton.setVisible(false);
        closeNozzleButton.visibleProperty().unbind();
        closeNozzleButton.setVisible(false);
        fineNozzleButton.visibleProperty().unbind();
        fineNozzleButton.setVisible(false);
        fillNozzleButton.visibleProperty().unbind();
        fillNozzleButton.setVisible(false);
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
        if (printer == currentSettingsPrinter)
        {
            whenProjectOrSettingsPrinterChange();
        }
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
        if (printer == currentSettingsPrinter)
        {
            whenProjectOrSettingsPrinterChange();
        }
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
        if (printer == currentSettingsPrinter)
        {
            whenProjectOrSettingsPrinterChange();
        }
    }

    @Override
    public void whenExtruderAdded(Printer printer, int extruderIndex)
    {
        if (printer == currentSettingsPrinter)
        {
            whenProjectOrSettingsPrinterChange();
        }
    }

    @Override
    public void whenExtruderRemoved(Printer printer, int extruderIndex)
    {
        if (printer == currentSettingsPrinter)
        {
            whenProjectOrSettingsPrinterChange();
        }
    }

}
