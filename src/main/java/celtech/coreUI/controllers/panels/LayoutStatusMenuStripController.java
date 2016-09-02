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
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.coreUI.AmbientLEDState;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.ProjectGUIRules;
import celtech.coreUI.ProjectGUIState;
import celtech.coreUI.components.Notifications.ConditionalNotificationBar;
import celtech.coreUI.components.Notifications.NotificationDisplay;
import celtech.coreUI.components.buttons.GraphicButtonWithLabel;
import celtech.coreUI.components.buttons.GraphicToggleButtonWithLabel;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.coreUI.visualisation.ModelLoader;
import celtech.coreUI.visualisation.ProjectSelection;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import celtech.utils.PrinterUtils;
import static celtech.utils.StringMetrics.getWidthOfString;
import celtech.utils.tasks.TaskResponse;
import java.io.File;
import static java.lang.Double.max;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;
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
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
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
    private GraphicButtonWithLabel addCloudModelButton;

    @FXML
    private GraphicToggleButtonWithLabel snapToGroundButton;

    @FXML
    private GraphicButtonWithLabel groupButton;

    @FXML
    private GraphicButtonWithLabel ungroupButton;

    @FXML
    private GraphicButtonWithLabel purgeButton;

//    @FXML
//    private GraphicButtonWithLabel cutButton;
    private Project selectedProject;
    private UndoableProject undoableSelectedProject;
    private ObjectProperty<LayoutSubmode> layoutSubmode;
    private ProjectSelection projectSelection;
    private final ModelLoader modelLoader = new ModelLoader();

    private ConditionalNotificationBar oneExtruderNoFilamentSelectedNotificationBar;
    private ConditionalNotificationBar oneExtruderNoFilamentNotificationBar;
    private ConditionalNotificationBar twoExtrudersNoFilament0SelectedNotificationBar;
    private ConditionalNotificationBar twoExtrudersNoFilament0NotificationBar;
    private ConditionalNotificationBar twoExtrudersNoFilament1SelectedNotificationBar;
    private ConditionalNotificationBar twoExtrudersNoFilament1NotificationBar;
    private ConditionalNotificationBar doorOpenConditionalNotificationBar;
    private ConditionalNotificationBar invalidMeshInProjectNotificationBar;
    private ConditionalNotificationBar chooseACustomProfileNotificationBar;
    private ConditionalNotificationBar printHeadPowerOffNotificationBar;
    private ConditionalNotificationBar noHeadNotificationBar;
    private ConditionalNotificationBar noModelsNotificationBar;

    private final BooleanProperty modelsOffBed = new SimpleBooleanProperty(false);
    private final BooleanProperty modelsOffBedWithRaft = new SimpleBooleanProperty(false);
    private final BooleanProperty modelOffBedWithSpiral = new SimpleBooleanProperty(false);
    private ConditionalNotificationBar modelsOffBedNotificationBar;
    private ConditionalNotificationBar modelsOffBedWithRaftNotificationBar;
    private ConditionalNotificationBar modelOffBedWithSpiralNotificationBar;

    private final MapChangeListener<Integer, Filament> effectiveFilamentListener = (MapChangeListener.Change<? extends Integer, ? extends Filament> change) ->
    {
        whenProjectOrSettingsPrinterChange();
    };

    @FXML
    void group(ActionEvent event)
    {
        Project currentProject = Lookup.getSelectedProjectProperty().get();

        Set<ModelContainer> modelGroups = currentProject.getTopLevelModels().stream().filter(
                mc -> mc instanceof ModelGroup).collect(Collectors.toSet());
        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
        undoableSelectedProject.group(modelContainers);
        Set<ModelContainer> changedModelGroups = currentProject.getTopLevelModels().stream().filter(
                mc -> mc instanceof ModelGroup).collect(Collectors.toSet());
        changedModelGroups.removeAll(modelGroups);

        Lookup.getProjectGUIState(currentProject).getProjectSelection().deselectAllModels();
        if (changedModelGroups.size() == 1)
        {
            changedModelGroups.iterator().next().notifyScreenExtentsChange();
            Lookup.getProjectGUIState(currentProject).getProjectSelection().addModelContainer(
                    changedModelGroups.iterator().next());
        }
    }

    @FXML
    void ungroup(ActionEvent event)
    {
        Project currentProject = Lookup.getSelectedProjectProperty().get();

        Set<ModelContainer> modelContainers = Lookup.getProjectGUIState(currentProject).getProjectSelection().getSelectedModelsSnapshot();
        undoableSelectedProject.ungroup(modelContainers);
        Lookup.getProjectGUIState(currentProject).getProjectSelection().deselectAllModels();
    }

    @FXML
    void startCut(ActionEvent event)
    {
        layoutSubmode.set(LayoutSubmode.Z_CUT);
    }

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
        Printer printer = Lookup.getSelectedPrinterProperty().get();

        Project currentProject = Lookup.getSelectedProjectProperty().get();

        PurgeResponse purgeConsent = printerUtils.offerPurgeIfNecessary(printer, currentProject.getUsedExtruders(printer));

        try
        {
            if (purgeConsent == PurgeResponse.PRINT_WITH_PURGE)
            {
                displayManager.getPurgeInsetPanelController().purgeAndPrint(
                        currentProject, printer);
            } else if (purgeConsent == PurgeResponse.PRINT_WITHOUT_PURGE)
            {
                ObservableList<Boolean> usedExtruders = currentProject.getUsedExtruders(printer);
                for (int extruderNumber = 0; extruderNumber < usedExtruders.size(); extruderNumber++)
                {
                    if (usedExtruders.get(extruderNumber))
                    {
                        if (extruderNumber == 0)
                        {
                            if (currentPrinter.headProperty().get().headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD)
                            {
                                currentPrinter.resetPurgeTemperatureForNozzleHeater(currentPrinter.headProperty().get(), 1);
                            } else
                            {
                                currentPrinter.resetPurgeTemperatureForNozzleHeater(currentPrinter.headProperty().get(), 0);
                            }
                        } else
                        {
                            currentPrinter.resetPurgeTemperatureForNozzleHeater(currentPrinter.headProperty().get(), 0);
                        }
                    }
                }
                printer.printProject(currentProject);
                applicationStatus.setMode(ApplicationMode.STATUS);
            } else if (purgeConsent == PurgeResponse.NOT_NECESSARY)
            {
                printer.printProject(currentProject);
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
    void purge(ActionEvent event)
    {
        DisplayManager.getInstance().getPurgeInsetPanelController().purge(currentPrinter);
    }

    @FXML
    void register(ActionEvent event
    )
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.REGISTRATION);
    }

    @FXML
    void addModel(ActionEvent event
    )
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
        undoableSelectedProject.deleteModels(projectSelection.getSelectedModelsSnapshot());
    }

    @FXML
    void copyModel(ActionEvent event)
    {
        undoableSelectedProject.copyModels(projectSelection.getSelectedModelsSnapshot());
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
        try
        {
            currentPrinter.goToOpenDoorPosition(null);
        } catch (PrinterException ex)
        {
            steno.error("Error opening door " + ex.getMessage());
        }
    }

    @FXML
    void ejectFilament(ActionEvent event)
    {

        Printer printer = Lookup.getSelectedPrinterProperty().get();
        if (printer.extrudersProperty().get(0).filamentLoadedProperty().get()
                && printer.extrudersProperty().get(1).filamentLoadedProperty().get())
        {

            ContextMenu contextMenu = new ContextMenu();

            String cm1Text;
            String cm2Text;

            if (printer.reelsProperty().containsKey(0))
            {
                cm1Text = "1: "
                        + printer.reelsProperty().get(0).friendlyFilamentNameProperty().get();
            } else
            {
                cm1Text = "1: " + Lookup.i18n("materialComponent.unknown");
            }

            if (printer.reelsProperty().containsKey(1))
            {
                cm2Text = "2: "
                        + printer.reelsProperty().get(1).friendlyFilamentNameProperty().get();
            } else
            {
                cm2Text = "2: " + Lookup.i18n("materialComponent.unknown");
            }

            MenuItem cmItem1 = new MenuItem(cm1Text);
            MenuItem cmItem2 = new MenuItem(cm2Text);
            MenuItem bothItem = new MenuItem(Lookup.i18n("misc.Both"));
            cmItem1.setOnAction((ActionEvent e) ->
            {
                ejectFilament(0);
            });
            cmItem2.setOnAction((ActionEvent e) ->
            {
                ejectFilament(1);
            });
            bothItem.setOnAction((ActionEvent e) ->
            {
                ejectFilament(1);
                ejectFilament(0);
            });

            contextMenu.getItems().add(cmItem1);
            contextMenu.getItems().add(cmItem2);
            contextMenu.getItems().add(bothItem);

            double cm1Width = getWidthOfString(cm1Text, "lightText", 14);
            double cm2Width = getWidthOfString(cm2Text, "lightText", 14);

            contextMenu.show(ejectFilamentButton, Side.TOP,
                    35 - ((max(cm1Width, cm2Width) + 20) / 2.0), -25);
        } else if (printer.extrudersProperty().get(0).filamentLoadedProperty().get())
        {
            ejectFilament(0);
        } else if (printer.extrudersProperty().get(1).filamentLoadedProperty().get())
        {
            ejectFilament(1);
        }
    }

    private void ejectFilament(int extruder)
    {
        try
        {
            currentPrinter.ejectFilament(extruder, null);
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
            currentPrinter.selectNozzle(0);
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
            currentPrinter.selectNozzle(1);
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
            currentPrinter.openNozzleFully();
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
            currentPrinter.closeNozzleFully();
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
            currentPrinter.homeAllAxes(false, null);
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
            if (currentPrinter.getPrinterAncillarySystems().headFanOnProperty().get())
            {
                currentPrinter.switchOffHeadFan();
            } else
            {
                currentPrinter.switchOnHeadFan();
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
                currentPrinter.switchOffHeadLEDs();
                headLEDOn = false;
            } else
            {
                currentPrinter.switchOnHeadLEDs();
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
                    currentPrinter.setAmbientLEDColour(Color.BLACK);
                    break;
                case WHITE:
                    currentPrinter.setAmbientLEDColour(
                            colourMap.displayToPrinterColour(Color.WHITE));
                    break;
                case COLOUR:
                    currentPrinter.setAmbientLEDColour(
                            currentPrinter.getPrinterIdentity().printerColourProperty().get());
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
            currentPrinter.removeHead((TaskResponse taskResponse) ->
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
    private Printer currentPrinter = null;
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
        oneExtruderNoFilamentSelectedNotificationBar = new ConditionalNotificationBar("dialogs.cantPrintNoFilamentSelectedMessage", NotificationDisplay.NotificationType.CAUTION);
        oneExtruderNoFilamentNotificationBar = new ConditionalNotificationBar("dialogs.cantPrintNoFilamentMessage", NotificationDisplay.NotificationType.CAUTION);
        twoExtrudersNoFilament0SelectedNotificationBar = new ConditionalNotificationBar("dialogs.cantPrintNoFilamentSelectedMessage0", NotificationDisplay.NotificationType.CAUTION);
        twoExtrudersNoFilament0NotificationBar = new ConditionalNotificationBar("dialogs.cantPrintNoFilamentMessage0", NotificationDisplay.NotificationType.CAUTION);
        twoExtrudersNoFilament1SelectedNotificationBar = new ConditionalNotificationBar("dialogs.cantPrintNoFilamentSelectedMessage1", NotificationDisplay.NotificationType.CAUTION);
        twoExtrudersNoFilament1NotificationBar = new ConditionalNotificationBar("dialogs.cantPrintNoFilamentMessage1", NotificationDisplay.NotificationType.CAUTION);
        doorOpenConditionalNotificationBar = new ConditionalNotificationBar("dialogs.cantPrintDoorIsOpenMessage", NotificationDisplay.NotificationType.CAUTION);
        invalidMeshInProjectNotificationBar = new ConditionalNotificationBar("dialogs.invalidMeshInProjectMessage", NotificationDisplay.NotificationType.NOTE);
        chooseACustomProfileNotificationBar = new ConditionalNotificationBar("dialogs.chooseACustomProfile", NotificationDisplay.NotificationType.CAUTION);
        printHeadPowerOffNotificationBar = new ConditionalNotificationBar("dialogs.printHeadPowerOff", NotificationDisplay.NotificationType.CAUTION);
        noHeadNotificationBar = new ConditionalNotificationBar("dialogs.cantPrintNoHeadMessage", NotificationDisplay.NotificationType.CAUTION);
        noModelsNotificationBar = new ConditionalNotificationBar("dialogs.cantPrintNoModelOnBed", NotificationDisplay.NotificationType.CAUTION);

        modelsOffBedNotificationBar = new ConditionalNotificationBar("dialogs.modelsOffBed", NotificationDisplay.NotificationType.CAUTION);
        modelsOffBedWithRaftNotificationBar = new ConditionalNotificationBar("dialogs.modelsOffBedWithRaft", NotificationDisplay.NotificationType.CAUTION);
        modelOffBedWithSpiralNotificationBar = new ConditionalNotificationBar("dialogs.modelOffBedWithSpiral", NotificationDisplay.NotificationType.CAUTION);

        modelsOffBedNotificationBar.setAppearanceCondition(ApplicationStatus.getInstance().modeProperty().isEqualTo(ApplicationMode.SETTINGS).and(modelsOffBed).and(modelsOffBedWithRaft.not()).and(modelOffBedWithSpiral.not()));
        modelsOffBedWithRaftNotificationBar.setAppearanceCondition(ApplicationStatus.getInstance().modeProperty().isEqualTo(ApplicationMode.SETTINGS).and(modelsOffBedWithRaft));
        modelOffBedWithSpiralNotificationBar.setAppearanceCondition(ApplicationStatus.getInstance().modeProperty().isEqualTo(ApplicationMode.SETTINGS).and(modelOffBedWithSpiral));

        displayManager = DisplayManager.getInstance();
        applicationStatus = ApplicationStatus.getInstance();
        printerUtils = PrinterUtils.getInstance();

        statusButtonHBox.setVisible(false);

        createStatusPrinterListener();

        printButton.disableProperty().bind(canPrintProject.not());

        setupButtonVisibility();

        Lookup.getSelectedProjectProperty().addListener(
                (ObservableValue<? extends Project> observable, Project oldValue, Project newValue) ->
                {
                    whenProjectChanges(newValue);
                });

        Lookup.getSelectedPrinterProperty().addListener(
                (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
                {
                    currentPrinter = newValue;
                    if (oldValue != null)
                    {
                        oldValue.effectiveFilamentsProperty().removeListener(effectiveFilamentListener);
                    }

                    if (newValue != null)
                    {
                        newValue.effectiveFilamentsProperty().addListener(effectiveFilamentListener);
                    }
                });
        currentPrinter = Lookup.getSelectedPrinterProperty().get();

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

        groupButton.setVisible(true);
        ungroupButton.setVisible(false);

        // Prevent the status bar affecting layout when it is invisible
        statusButtonHBox.visibleProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(
                    ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
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
            Lookup.getSelectedPrinterProperty().addListener(printerSettingsListener);
        }
    }

    private void updatePrintButtonConditionalText(Printer printer, Project project)
    {
        if (printer == null || project == null)
        {
            return;
        }

        doorOpenConditionalNotificationBar.setAppearanceCondition(
                printer.getPrinterAncillarySystems().doorOpenProperty()
                .and(Lookup.getUserPreferences().safetyFeaturesOnProperty())
                .and(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS)));
        chooseACustomProfileNotificationBar.setAppearanceCondition(project.customSettingsNotChosenProperty()
                .and(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS)));
        printHeadPowerOffNotificationBar.setAppearanceCondition(printer.headPowerOnFlagProperty().not()
                .and(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS))
                .and(printer.headProperty().isNotNull()));
        noHeadNotificationBar.setAppearanceCondition(printer.headProperty().isNull()
                .and(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS)));

        BooleanBinding oneExtruderPrinter = printer.extrudersProperty().get(1).isFittedProperty().not();
        BooleanBinding twoExtruderPrinter = printer.extrudersProperty().get(1).isFittedProperty().not().not();
        BooleanBinding noFilament0Selected = Bindings.valueAt(printer.effectiveFilamentsProperty(), 0).isEqualTo(FilamentContainer.UNKNOWN_FILAMENT);
        BooleanBinding noFilament1Selected = Bindings.valueAt(printer.effectiveFilamentsProperty(), 1).isEqualTo(FilamentContainer.UNKNOWN_FILAMENT);

        ObservableList<Boolean> usedExtruders = project.getUsedExtruders(printer);

        oneExtruderNoFilamentSelectedNotificationBar.setAppearanceCondition(oneExtruderPrinter.and(Bindings.booleanValueAt(usedExtruders, 0)).and(
                noFilament0Selected).and(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS)));

        oneExtruderNoFilamentNotificationBar.setAppearanceCondition(oneExtruderPrinter.and(Bindings.booleanValueAt(usedExtruders, 0)).and(
                printer.extrudersProperty().get(0).
                filamentLoadedProperty().not()).and(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS)));

        twoExtrudersNoFilament0SelectedNotificationBar.setAppearanceCondition(twoExtruderPrinter.and(Bindings.booleanValueAt(usedExtruders, 0)).and(
                noFilament0Selected).and(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS)));

        twoExtrudersNoFilament0NotificationBar.setAppearanceCondition(twoExtruderPrinter.and(Bindings.booleanValueAt(usedExtruders, 0)).and(
                printer.extrudersProperty().get(0).
                filamentLoadedProperty().not()).and(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS)));

        twoExtrudersNoFilament1SelectedNotificationBar.setAppearanceCondition(twoExtruderPrinter.and(Bindings.booleanValueAt(usedExtruders, 1)).and(
                noFilament1Selected).and(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS)));

        twoExtrudersNoFilament1NotificationBar.setAppearanceCondition(twoExtruderPrinter.and(Bindings.booleanValueAt(usedExtruders, 1)).and(
                printer.extrudersProperty().get(1).
                filamentLoadedProperty().not()).and(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS)));

        invalidMeshInProjectNotificationBar.setAppearanceCondition(project.hasInvalidMeshes().
                and(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS)));

        noModelsNotificationBar.setAppearanceCondition(Bindings.isEmpty(project.getTopLevelModels())
                .and(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS)));
    }

    /**
     * Create the bindings to the Status selected printer.
     */
    private void createStatusPrinterListener()
    {
        currentPrinter = Lookup.getSelectedPrinterProperty().get();

        Lookup.getSelectedPrinterProperty().addListener((ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
        {
            if (newValue != null)
            {
                printerAvailable.set(true);

                if (currentPrinter != null)
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
                    currentPrinter.getPrinterAncillarySystems().headFanOnProperty().
                            removeListener(headFanStatusListener);
                    headLightsButton.disableProperty().unbind();
                    ambientLightsButton.disableProperty().unbind();
                    calibrateButton.disableProperty().unbind();
                    removeHeadButton.disableProperty().unbind();
                    purgeButton.disableProperty().unbind();

                    clearConditionalNotificationBarConditions();
                }

                unlockDoorButton.disableProperty().bind(newValue.canOpenDoorProperty().not());
                ejectFilamentButton.disableProperty().bind(newValue.extrudersProperty().get(0).
                        canEjectProperty().not().and(newValue.extrudersProperty().get(1).
                                canEjectProperty().not()));

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
                purgeButton.disableProperty()
                        .bind(newValue.canPurgeHeadProperty().not());

                if (newValue.headProperty().get() != null)
                {
                    bindNozzleControls(newValue);
                }

                currentPrinter = newValue;

            } else
            {
                printerAvailable.set(false);
                clearConditionalNotificationBarConditions();
            }
        });
    }

    private void clearConditionalNotificationBarConditions()
    {
        oneExtruderNoFilamentSelectedNotificationBar.clearAppearanceCondition();
        oneExtruderNoFilamentNotificationBar.clearAppearanceCondition();
        twoExtrudersNoFilament0SelectedNotificationBar.clearAppearanceCondition();
        twoExtrudersNoFilament0NotificationBar.clearAppearanceCondition();
        twoExtrudersNoFilament1SelectedNotificationBar.clearAppearanceCondition();
        twoExtrudersNoFilament1NotificationBar.clearAppearanceCondition();
        doorOpenConditionalNotificationBar.clearAppearanceCondition();
        invalidMeshInProjectNotificationBar.clearAppearanceCondition();
        chooseACustomProfileNotificationBar.clearAppearanceCondition();
        printHeadPowerOffNotificationBar.clearAppearanceCondition();
        noHeadNotificationBar.clearAppearanceCondition();
        noModelsNotificationBar.clearAppearanceCondition();
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
        public void whenModelsRemoved(Set<ModelContainer> modelContainers)
        {
            whenProjectOrSettingsPrinterChange();
        }

        @Override
        public void whenAutoLaidOut()
        {
            dealWithOutOfBoundsModels();
        }

        @Override
        public void whenModelsTransformed(Set<ModelContainer> modelContainers)
        {
            dealWithOutOfBoundsModels();
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
        Lookup.getSelectedPrinterProperty().removeListener(printerSettingsListener);
        layoutSubmode.removeListener(layoutSubmodeListener);
        project.removeProjectChangesListener(projectChangesListener);
        undoButton.disableProperty().unbind();
        redoButton.disableProperty().unbind();
    }

    private void bindProject(Project project)
    {

        createPrinterSettingsListener(printerSettings);
        bindSelectedModels(project);

        if (currentPrinter != null && project != null)
        {
            whenProjectOrSettingsPrinterChange();
        }

        layoutSubmode.addListener(layoutSubmodeListener);
        project.addProjectChangesListener(projectChangesListener);

        undoButton.disableProperty().bind(
                Lookup.getProjectGUIState(project).getCommandStack().getCanUndo().not());
        redoButton.disableProperty().bind(
                Lookup.getProjectGUIState(project).getCommandStack().getCanRedo().not());

    }

    private void dealWithOutOfBoundsModels()
    {
        boolean aModelIsOffTheBed = false;
        boolean aModelIsOffTheBedWithRaft = false;
        boolean aModelIsOffTheBedWithSpiral = false;

        if (selectedProject != null
                && currentPrinter != null
                && currentPrinter.headProperty().get() != null)
        {
            SlicerParametersFile slicerParameters = selectedProject.getPrinterSettings().getSettings(currentPrinter.headProperty().get().typeCodeProperty().get());

            for (ModelContainer modelContainer : selectedProject.getAllModels())
            {
                aModelIsOffTheBed |= modelContainer.isOffBedProperty().get();

                //NOTE - this needs to change if raft settings in slicermapping.dat is changed
                double raftOffset = slicerParameters.getRaftBaseThickness_mm()
                        //Raft interface thickness
                        + 0.28
                        //Raft surface layer thickness * surface layers
                        + (slicerParameters.getInterfaceLayers() * 0.27)
                        + slicerParameters.getRaftAirGapLayer0_mm();

                if (selectedProject.getPrinterSettings().getRaftOverride()
                        && modelContainer.isModelTooHighWithOffset(raftOffset))
                {
                    aModelIsOffTheBedWithRaft = true;
                }

                //TODO use settings derived offset values
                if (selectedProject.getPrinterSettings().getSpiralPrintOverride()
                        && modelContainer.isModelTooHighWithOffset(0.5))
                {
                    aModelIsOffTheBedWithSpiral = true;
                }
            };
        }

        if (aModelIsOffTheBed != modelsOffBed.get())
        {
            modelsOffBed.set(aModelIsOffTheBed);
        }

        if (aModelIsOffTheBedWithRaft != modelsOffBedWithRaft.get())
        {
            modelsOffBedWithRaft.set(aModelIsOffTheBedWithRaft);
        }

        if (aModelIsOffTheBedWithSpiral != modelOffBedWithSpiral.get())
        {
            modelOffBedWithSpiral.set(aModelIsOffTheBedWithSpiral);
        }
    }

    private void whenProjectOrSettingsPrinterChange()
    {
        try
        {
            updateCanPrintProjectBindings(currentPrinter, selectedProject);
            updatePrintButtonConditionalText(currentPrinter, selectedProject);
            dealWithOutOfBoundsModels();
        } catch (Exception ex)
        {
            steno.warning("Error updating can print or print button conditionals: " + ex);
        }
    }

    /**
     * This must be called whenever the project is changed.
     *
     * @param project
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
        currentPrinter = Lookup.getSelectedPrinterProperty().get();
        projectSelection = Lookup.getProjectGUIState(project).getProjectSelection();
        layoutSubmode = Lookup.getProjectGUIState(project).getLayoutSubmodeProperty();

        bindProject(project);

    }

    /**
     * This should be called whenever the printer or project changes and updates
     * the bindings for the canPrintProject property.
     */
    private void updateCanPrintProjectBindings(Printer printer, Project project)
    {
        if (printer != null && project != null)
        {
            printButton.disableProperty().unbind();

            ObservableList<Boolean> usedExtruders = project.getUsedExtruders(printer);

            if (usedExtruders.get(0) && usedExtruders.get(1))
            {
                //Both extruders are required
                BooleanBinding filament0Selected = Bindings.valueAt(printer.effectiveFilamentsProperty(), 0).isNotEqualTo(FilamentContainer.UNKNOWN_FILAMENT);
                BooleanBinding filament1Selected = Bindings.valueAt(printer.effectiveFilamentsProperty(), 1).isNotEqualTo(FilamentContainer.UNKNOWN_FILAMENT);

                canPrintProject.bind(
                        Bindings.isNotEmpty(project.getTopLevelModels())
                        .and(printer.canPrintProperty())
                        .and(project.canPrintProperty())
                        .and(filament0Selected)
                        .and(filament1Selected)
                        .and(printer.getPrinterAncillarySystems().doorOpenProperty().not()
                                .or(Lookup.getUserPreferences().safetyFeaturesOnProperty().not()))
                        .and(printer.extrudersProperty().get(0).filamentLoadedProperty())
                        .and(printer.extrudersProperty().get(1).filamentLoadedProperty()
                                .and(printer.headPowerOnFlagProperty()))
                        .and(modelsOffBed.not())
                        .and(modelsOffBedWithRaft.not())
                        .and(modelOffBedWithSpiral.not())
                );
            } else
            {
                // only one extruder required, which one is it?
                int extruderNumber = (project.getUsedExtruders(printer).get(0)) ? 0 : 1;
                BooleanBinding filamentPresentBinding = Bindings.valueAt(printer.effectiveFilamentsProperty(), extruderNumber).isNotEqualTo(FilamentContainer.UNKNOWN_FILAMENT);

                canPrintProject.bind(
                        Bindings.isNotEmpty(project.getTopLevelModels())
                        .and(printer.canPrintProperty())
                        .and(project.canPrintProperty())
                        .and(filamentPresentBinding)
                        .and(printer.getPrinterAncillarySystems().doorOpenProperty().not()
                                .or(Lookup.getUserPreferences().safetyFeaturesOnProperty().not()))
                        .and(printer.extrudersProperty().get(extruderNumber).
                                filamentLoadedProperty()
                                .and(printer.headPowerOnFlagProperty()))
                        .and(modelsOffBed.not())
                        .and(modelsOffBedWithRaft.not())
                        .and(modelOffBedWithSpiral.not())
                );
            }
//            }
            printButton.disableProperty().bind(canPrintProject.not());
        }
    }

    /**
     * Binds button disabled properties to the selection container This disables
     * and enables buttons depending on whether a model is selected.
     */
    private void bindSelectedModels(Project project)
    {
        ProjectGUIState projectGUIState = Lookup.getProjectGUIState(project);
        ProjectSelection projectSelection = projectGUIState.getProjectSelection();
        ProjectGUIRules projectGUIRules = projectGUIState.getProjectGUIRules();
        ReadOnlyObjectProperty<LayoutSubmode> layoutSubmodeProperty = projectGUIState.
                getLayoutSubmodeProperty();

        addModelButton.disableProperty().unbind();
        deleteModelButton.disableProperty().unbind();
        duplicateModelButton.disableProperty().unbind();
        snapToGroundButton.disableProperty().unbind();
        distributeModelsButton.disableProperty().unbind();
        groupButton.disableProperty().unbind();
        groupButton.visibleProperty().unbind();
        ungroupButton.disableProperty().unbind();
        groupButton.visibleProperty().unbind();
//        cutButton.disableProperty().unbind();

        BooleanBinding notSelectModeOrNoSelectedModels
                = Bindings.notEqual(LayoutSubmode.SELECT, layoutSubmodeProperty).or(
                        Bindings.equal(0, projectSelection.getNumModelsSelectedProperty()));
        BooleanBinding notSelectModeOrNoLoadedModels
                = Bindings.notEqual(LayoutSubmode.SELECT, layoutSubmodeProperty).or(
                        Bindings.isEmpty(project.getTopLevelModels()));
        BooleanBinding snapToGround
                = Bindings.equal(LayoutSubmode.SNAP_TO_GROUND, layoutSubmodeProperty);
        BooleanBinding noLoadedModels = Bindings.isEmpty(project.getTopLevelModels());
        deleteModelButton.disableProperty().bind(
                notSelectModeOrNoSelectedModels.or(projectGUIRules.canRemoveOrDuplicateSelection().not()));
        duplicateModelButton.disableProperty().bind(
                notSelectModeOrNoSelectedModels.or(projectGUIRules.canRemoveOrDuplicateSelection().not()));
        distributeModelsButton.setDisable(true);

        addModelButton.disableProperty().bind(
                snapToGround.or(projectGUIRules.canAddModel().not()));
        addCloudModelButton.disableProperty().bind(snapToGround.or(projectGUIRules.canAddModel().not()));

        distributeModelsButton.disableProperty().bind(
                notSelectModeOrNoLoadedModels.or(projectGUIRules.canAddModel().not()));
        snapToGroundButton.disableProperty().bind(
                noLoadedModels.or(projectGUIRules.canSnapToGroundSelection().not()));

        groupButton.disableProperty().bind(
                noLoadedModels.or(projectGUIRules.canGroupSelection().not()));
        groupButton.visibleProperty().bind(ungroupButton.visibleProperty().not());

        ungroupButton.visibleProperty().bind(
                noLoadedModels.not().and(projectGUIRules.canGroupSelection().not()).and(projectGUIRules.canUngroupSelection()));

//        cutButton.disableProperty().bind(
//                noLoadedModels.or(projectGUIRules.canCutModel().not()));
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
        if (printer == currentPrinter)
        {
            bindNozzleControls(printer);
        }
    }

    private void bindNozzleControls(Printer printer)
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
        if (printer == currentPrinter)
        {
            whenProjectOrSettingsPrinterChange();
        }
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
        if (printer == currentPrinter)
        {
            whenProjectOrSettingsPrinterChange();
        }
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
        if (printer == currentPrinter)
        {
            whenProjectOrSettingsPrinterChange();
        }
    }

    @Override
    public void whenExtruderAdded(Printer printer, int extruderIndex)
    {
        if (printer == currentPrinter)
        {
            whenProjectOrSettingsPrinterChange();
        }
    }

    @Override
    public void whenExtruderRemoved(Printer printer, int extruderIndex)
    {
        if (printer == currentPrinter)
        {
            whenProjectOrSettingsPrinterChange();
        }
    }

}
