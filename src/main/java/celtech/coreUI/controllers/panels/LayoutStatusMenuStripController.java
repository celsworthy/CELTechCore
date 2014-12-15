package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.ProjectMode;
import static celtech.appManager.ProjectMode.GCODE;
import static celtech.appManager.ProjectMode.MESH;
import static celtech.appManager.ProjectMode.NONE;
import celtech.appManager.PurgeResponse;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.DirectoryMemoryProperty;
import celtech.configuration.PrinterColourMap;
import celtech.coreUI.AmbientLEDState;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.components.ProjectTab;
import celtech.coreUI.components.tips.ArrowTag;
import celtech.coreUI.components.buttons.GraphicButtonWithLabel;
import celtech.coreUI.components.buttons.GraphicToggleButtonWithLabel;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.coreUI.visualisation.SelectedModelContainers;
import celtech.coreUI.visualisation.ThreeDViewManager;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import celtech.utils.PrinterUtils;
import celtech.utils.tasks.TaskResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
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

    private final Stenographer steno = StenographerFactory.getStenographer(LayoutStatusMenuStripController.class.getName());
    private SettingsScreenState settingsScreenState = null;
    private ApplicationStatus applicationStatus = null;
    private DisplayManager displayManager = null;
    private final FileChooser modelFileChooser = new FileChooser();
    private Project boundProject = null;
    private PrinterUtils printerUtils = null;
    private PrinterColourMap colourMap = PrinterColourMap.getInstance();
    private ChangeListener<Color> printerColourChangeListener = null;

    //Temporary - until the firmware indicates selected nozzle
    private IntegerProperty currentNozzle = new SimpleIntegerProperty(0);

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
        Printer printer = settingsScreenState.getSelectedPrinter();

        Project currentProject = DisplayManager.getInstance().getCurrentlyVisibleProject();

        PurgeResponse purgeConsent = printerUtils.offerPurgeIfNecessary(printer);

        if (purgeConsent == PurgeResponse.PRINT_WITH_PURGE)
        {
            displayManager.getPurgeInsetPanelController().purgeAndPrint(currentProject, settingsScreenState.getFilament(),
                                                                        settingsScreenState.getPrintQuality(),
                                                                        settingsScreenState.getSettings(), printer);
        } else if (purgeConsent == PurgeResponse.PRINT_WITHOUT_PURGE)
        {
            currentPrinter.resetPurgeTemperature();
            printer.printProject(currentProject, settingsScreenState.getFilament(),
                                 settingsScreenState.getPrintQuality(),
                                 settingsScreenState.getSettings());
            applicationStatus.setMode(ApplicationMode.STATUS);
        } else if (purgeConsent == PurgeResponse.NOT_NECESSARY)
        {
            printer.printProject(currentProject, settingsScreenState.getFilament(),
                                 settingsScreenState.getPrintQuality(),
                                 settingsScreenState.getSettings());
            applicationStatus.setMode(ApplicationMode.STATUS);
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
//        applicationStatus.setMode(ApplicationMode.ADD_MODEL);
        Platform.runLater(() ->
        {
            ListIterator iterator = modelFileChooser.getExtensionFilters().listIterator();

            while (iterator.hasNext())
            {
                iterator.next();
                iterator.remove();
            }

            ProjectMode projectMode = ProjectMode.NONE;

            if (displayManager.getCurrentlyVisibleProject() != null)
            {
                projectMode = displayManager.getCurrentlyVisibleProject().getProjectMode();
            }

            String descriptionOfFile = null;

            switch (projectMode)
            {
                case NONE:
                    descriptionOfFile = DisplayManager.getLanguageBundle().getString("dialogs.anyFileChooserDescription");
                    break;
                case MESH:
                    descriptionOfFile = DisplayManager.getLanguageBundle().getString("dialogs.meshFileChooserDescription");
                    break;
                case GCODE:
                    descriptionOfFile = DisplayManager.getLanguageBundle().getString("dialogs.gcodeFileChooserDescription");
                    break;
                default:
                    break;
            }
            modelFileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(descriptionOfFile,
                                                ApplicationConfiguration.getSupportedFileExtensionWildcards(
                                                    projectMode)));

            modelFileChooser.setInitialDirectory(new File(ApplicationConfiguration.getLastDirectory(
                DirectoryMemoryProperty.MODEL)));

            List<File> files;
            if (projectMode == ProjectMode.NONE || projectMode == ProjectMode.MESH)
            {
                files = modelFileChooser.showOpenMultipleDialog(displayManager.getMainStage());
            } else
            {
                File file = modelFileChooser.showOpenDialog(displayManager.getMainStage());
                files = new ArrayList<>();
                if (file != null)
                {
                    files.add(file);
                }
            }

            if (files != null && !files.isEmpty())
            {
                ApplicationConfiguration.setLastDirectory(
                    DirectoryMemoryProperty.MODEL,
                    files.get(0).getParentFile().getAbsolutePath());
                displayManager.loadExternalModels(files, true);
            }
        });
    }

    @FXML
    void addCloudModel(ActionEvent event)
    {
        applicationStatus.modeProperty().set(ApplicationMode.MY_MINI_FACTORY);
//            miniFactoryController.loadWebData();
//            myMiniFactoryLoaderStage.showAndWait();
    }

    @FXML
    void deleteModel(ActionEvent event)
    {
        displayManager.deleteSelectedModels();
    }

    @FXML
    void copyModel(ActionEvent event)
    {
        displayManager.copySelectedModels();
    }

    @FXML
    void autoLayoutModels(ActionEvent event)
    {
        displayManager.autoLayout();
    }

    @FXML
    void snapToGround(ActionEvent event)
    {
        displayManager.activateSnapToGround();
    }

    @FXML
    void unlockDoor(ActionEvent event)
    {
        boolean goAheadAndOpenTheDoor = false;

        if (currentPrinter.getPrinterAncillarySystems().bedTemperatureProperty().get() > 60)
        {
            if (Lookup.getUserPreferences().isOverrideSafeties() == true)
            {
                try
                {
                    currentPrinter.goToOpenDoorPositionDontWait(null);
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
                        currentPrinter.goToOpenDoorPosition(null);
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
                currentPrinter.goToOpenDoorPosition(null);
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
            currentPrinter.ejectFilament(0, null);
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
            currentPrinter.executeMacro("Home_all");
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
            Lookup.getSystemNotificationHandler().showInformationNotification(Lookup.i18n("removeHead.title"), Lookup.i18n("removeHead.finished"));
            steno.debug("Head remove completed");
        } else
        {
            Lookup.getSystemNotificationHandler().showWarningNotification(Lookup.i18n("removeHead.title"), Lookup.i18n("removeHead.failed"));
        }
    }

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
        displayManager = DisplayManager.getInstance();
        applicationStatus = ApplicationStatus.getInstance();
        settingsScreenState = SettingsScreenState.getInstance();
        printerUtils = PrinterUtils.getInstance();

        backwardFromLayoutButton.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(ApplicationMode.LAYOUT));
        backwardFromSettingsButton.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS));

        printButton.setVisible(false);
        printButton.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS));

        printButton.installTag("dialogs.cantPrintDoorIsOpenTitle");

        statusButtonHBox.setVisible(false);

        Lookup.getCurrentlySelectedPrinterProperty().addListener((ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
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
                    currentPrinter.getPrinterAncillarySystems().headFanOnProperty().removeListener(headFanStatusListener);
                    headLightsButton.disableProperty().unbind();
                    ambientLightsButton.disableProperty().unbind();
                    calibrateButton.disableProperty().unbind();
                    removeHeadButton.disableProperty().unbind();

                    printButton.getTag().removeAllConditionalText();
                }

                unlockDoorButton.disableProperty().bind(newValue.canOpenDoorProperty().not());
                ejectFilamentButton.disableProperty().bind(newValue.extrudersProperty().get(0).canEjectProperty().not());
                fineNozzleButton.visibleProperty().bind(currentNozzle.isEqualTo(1));
                fillNozzleButton.visibleProperty().bind(currentNozzle.isEqualTo(0));
                fineNozzleButton.disableProperty().bind(newValue.canPrintProperty().not());
                fillNozzleButton.disableProperty().bind(newValue.canPrintProperty().not());
                openNozzleButton.disableProperty().bind(newValue.canPrintProperty().not());
                closeNozzleButton.disableProperty().bind(newValue.canPrintProperty().not());
                homeButton.disableProperty().bind(newValue.canPrintProperty().not());
                newValue.getPrinterAncillarySystems().headFanOnProperty().addListener(headFanStatusListener);
                calibrateButton.disableProperty().bind(newValue.canCalibrateHeadProperty().not());
                removeHeadButton.disableProperty().bind(newValue.canPrintProperty().not());

                printButton.getTag().addConditionalText("dialogs.cantPrintNoFilamentSelectedMessage", settingsScreenState.filamentProperty().isNull());
                printButton.getTag().addConditionalText("dialogs.cantPrintDoorIsOpenMessage", newValue.getPrinterAncillarySystems().lidOpenProperty().not().not());
                printButton.getTag().addConditionalText("dialogs.cantPrintNoFilamentMessage", newValue.extrudersProperty().get(0).filamentLoadedProperty().not());
            } else
            {
                printerAvailable.set(false);
            }
        });

        if (settingsScreenState.selectedPrinterProperty().get() != null)
        {
            Printer printer = settingsScreenState.selectedPrinterProperty().get();
            printButton.setDisable(!printer.canPrintProperty().get()
                || settingsScreenState.filamentProperty() == null
                || printer.getPrinterAncillarySystems().lidOpenProperty().get()
                || !printer.extrudersProperty().get(0).filamentLoadedProperty().get());
        }

        settingsScreenState.selectedPrinterProperty().addListener((ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
        {
            if (newValue != null)
            {
                if (currentPrinter != null)
                {
                    printButton.disableProperty().unbind();
                }
                printButton.disableProperty().bind(newValue.canPrintProperty().not()
                    .or(settingsScreenState.filamentProperty().isNull())
                    .or(newValue.getPrinterAncillarySystems().lidOpenProperty())
                    .or(newValue.extrudersProperty().get(0).filamentLoadedProperty().not()));

                currentPrinter = newValue;
            }
        });

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

        statusButtonHBox.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(ApplicationMode.STATUS)
            .and(printerAvailable));
        layoutButtonHBox.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(ApplicationMode.LAYOUT));
        modelFileChooser.setTitle(DisplayManager.getLanguageBundle().getString("dialogs.modelFileChooser"));
        modelFileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(DisplayManager.getLanguageBundle().getString(
                    "dialogs.modelFileChooserDescription"),
                                            ApplicationConfiguration.getSupportedFileExtensionWildcards(
                                                ProjectMode.NONE)));

        forwardButtonSettings.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(ApplicationMode.LAYOUT));
        forwardButtonLayout.visibleProperty().bind((applicationStatus.modeProperty().isEqualTo(ApplicationMode.STATUS)));

        Lookup.getPrinterListChangesNotifier().addListener(this);
    }

    /**
     * Binds button disabled properties to the selection container This disables and enables buttons depending on whether a model is selected
     *
     * @param selectionContainer The selection container associated with the currently displayed project.
     */
    public void bindSelectedModels(ProjectTab projectTab)
    {
        SelectedModelContainers selectionModel = projectTab.getSelectionModel();
        ThreeDViewManager viewManager = projectTab.getThreeDViewManager();

        addModelButton.disableProperty().unbind();
        deleteModelButton.disableProperty().unbind();
        duplicateModelButton.disableProperty().unbind();
        snapToGroundButton.disableProperty().unbind();
        distributeModelsButton.disableProperty().unbind();

        BooleanBinding snapToGroundOrNoSelectedModels
            = Bindings.equal(LayoutSubmode.SNAP_TO_GROUND, viewManager.layoutSubmodeProperty()).or(
                Bindings.equal(0, selectionModel.getNumModelsSelectedProperty()));
        BooleanBinding snapToGroundOrNoLoadedModels
            = Bindings.equal(LayoutSubmode.SNAP_TO_GROUND, viewManager.layoutSubmodeProperty()).or(
                Bindings.isEmpty(viewManager.getLoadedModels()));
        BooleanBinding snapToGround
            = Bindings.equal(LayoutSubmode.SNAP_TO_GROUND, viewManager.layoutSubmodeProperty());
        BooleanBinding noLoadedModels = Bindings.isEmpty(viewManager.getLoadedModels());
        deleteModelButton.disableProperty().bind(snapToGroundOrNoSelectedModels);
        duplicateModelButton.disableProperty().bind(snapToGroundOrNoSelectedModels);
        distributeModelsButton.setDisable(true);

        if (boundProject != null)
        {
            addModelButton.disableProperty().unbind();
        }

        boundProject = displayManager.getCurrentlyVisibleProject();
        addModelButton.disableProperty().bind(snapToGround);

        distributeModelsButton.disableProperty().bind(snapToGroundOrNoLoadedModels);
        snapToGroundButton.disableProperty().bind(noLoadedModels);

        ChangeListener<LayoutSubmode> whenSubModeChanges
            = (ObservableValue<? extends LayoutSubmode> ov, LayoutSubmode oldMode, LayoutSubmode newMode) ->
            {
                if (oldMode.equals(LayoutSubmode.SNAP_TO_GROUND) && newMode.equals(LayoutSubmode.SELECT))
                {
                    snapToGroundButton.setSelected(false);
                }
            };
        viewManager.layoutSubmodeProperty().addListener(whenSubModeChanges);

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
            openNozzleButton.visibleProperty().bind(printer.headProperty().get().bPositionProperty().lessThan(0.5));
            closeNozzleButton.visibleProperty().bind(printer.headProperty().get().bPositionProperty().greaterThan(0.5));
        }
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        openNozzleButton.visibleProperty().unbind();
        openNozzleButton.setVisible(false);
        closeNozzleButton.visibleProperty().unbind();
        closeNozzleButton.setVisible(false);
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
    }
}
