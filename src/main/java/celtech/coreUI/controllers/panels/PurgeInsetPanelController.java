package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.coreUI.components.ProgressDisplay;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.components.buttons.GraphicButtonWithLabel;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.PurgeState;
import static celtech.printerControl.model.PurgeState.CONFIRM_TEMPERATURE;
import static celtech.printerControl.model.PurgeState.FAILED;
import static celtech.printerControl.model.PurgeState.FINISHED;
import static celtech.printerControl.model.PurgeState.HEATING;
import static celtech.printerControl.model.PurgeState.IDLE;
import static celtech.printerControl.model.PurgeState.INITIALISING;
import static celtech.printerControl.model.PurgeState.RUNNING_PURGE;
import celtech.printerControl.model.PurgeStateTransitionManager;
import celtech.printerControl.model.Reel;
import celtech.printerControl.model.StateTransition;
import celtech.printerControl.model.StateTransitionManager;
import celtech.printerControl.model.StateTransitionManager.GUIName;
import celtech.utils.PrinterListChangesListener;
import celtech.utils.PrinterUtils;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javax.print.PrintException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class PurgeInsetPanelController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        PurgeInsetPanelController.class.getName());

    private Project project = null;
    private Printer printer = null;
    private DiagramHandler diagramHandler;
    private Filament currentMaterial0;
    private Filament currentMaterial1;

    BooleanBinding purgeTwoNozzleHeaters;

    PurgeStateTransitionManager transitionManager;

    Map<StateTransitionManager.GUIName, Region> namesToButtons = new HashMap<>();

    private final ChangeListener<Number> purgeTempEntryListener0
        = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            transitionManager.setPurgeTemperature(0, newValue.intValue());
        };

    private final ChangeListener<Number> purgeTempEntryListener1
        = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            transitionManager.setPurgeTemperature(1, newValue.intValue());
        };

    @FXML
    private VBox diagramContainer;

    @FXML
    private GraphicButtonWithLabel startPurgeButton;

    @FXML
    private Text purgeStatus;

    @FXML
    private Text resettingPrinter;

    @FXML
    private GraphicButtonWithLabel cancelPurgeButton;

    @FXML
    private GraphicButtonWithLabel proceedButton;

    @FXML
    private GraphicButtonWithLabel okButton;

    @FXML
    private GraphicButtonWithLabel backButton;

    @FXML
    private GraphicButtonWithLabel repeatButton;

    @FXML
    private Text lastMaterialTemperature0;

    @FXML
    private RestrictedNumberField purgeTemperature0;

    @FXML
    private Text currentMaterialTemperature0;

    @FXML
    private Text textCurrentMaterial0;

    @FXML
    private ComboBox<Filament> cmbCurrentMaterial0;

    @FXML
    private GridPane purgeDetailsGrid0;

    @FXML
    private Text lastMaterialTemperature1;

    @FXML
    private RestrictedNumberField purgeTemperature1;

    @FXML
    private Text currentMaterialTemperature1;

    @FXML
    private Text textCurrentMaterial1;

    @FXML
    private ComboBox<Filament> cmbCurrentMaterial1;

    @FXML
    private GridPane purgeDetailsGrid1;

    @FXML
    private ProgressDisplay progressDisplay;

    @FXML
    private ToggleButton purgeThisNozzle0;

    @FXML
    private ToggleButton purgeThisNozzle1;

    @FXML
    void start(ActionEvent event)
    {
        transitionManager.followTransition(GUIName.START);
    }

    @FXML
    void proceed(ActionEvent event)
    {
        transitionManager.followTransition(GUIName.NEXT);
    }

    @FXML
    void cancel(ActionEvent event)
    {
        transitionManager.cancel();
    }

    @FXML
    void repeat(ActionEvent event)
    {
        transitionManager.followTransition(GUIName.RETRY);
    }

    @FXML
    void okPressed(ActionEvent event)
    {
        closeWindow(null);
    }

    @FXML
    void closeWindow(ActionEvent event)
    {

        ApplicationStatus.getInstance().returnToLastMode();

        if (project != null)
        {
            Lookup.getSystemNotificationHandler().askUserToClearBed();

            // Need to go to settings page for this project
            ApplicationStatus.getInstance().setMode(ApplicationMode.SETTINGS);
            project = null;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        populateNamesToButtons();

        diagramHandler = new DiagramHandler(diagramContainer, resources);
        diagramHandler.initialise();

        FXMLUtilities.addColonsToLabels(purgeDetailsGrid0);

        setupMaterialCombos();

        Lookup.getPrinterListChangesNotifier().addListener(new PrinterListChangesListener()
        {

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
            }

            @Override
            public void whenHeadRemoved(Printer printer, Head head)
            {
            }

            @Override
            public void whenReelAdded(Printer printer, int reelIndex)
            {
                if (PurgeInsetPanelController.this.printer == printer)
                {
                    PurgeInsetPanelController.this.showCurrentMaterial0();
                    PurgeInsetPanelController.this.showCurrentMaterial1();
                }
            }

            @Override
            public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
            {
                if (PurgeInsetPanelController.this.printer == printer)
                {
                    PurgeInsetPanelController.this.showCurrentMaterial0();
                    PurgeInsetPanelController.this.showCurrentMaterial1();
                }
            }

            @Override
            public void whenReelChanged(Printer printer, Reel reel)
            {
                if (PurgeInsetPanelController.this.printer == printer)
                {
                    PurgeInsetPanelController.this.showCurrentMaterial0();
                    PurgeInsetPanelController.this.showCurrentMaterial1();
                }
            }

            @Override
            public void whenExtruderAdded(Printer printer, int extruderIndex)
            {
            }

            @Override
            public void whenExtruderRemoved(Printer printer, int extruderIndex)
            {
            }
        });
    }

    private void populateNamesToButtons()
    {
        namesToButtons.put(StateTransitionManager.GUIName.NEXT, proceedButton);
        namesToButtons.put(StateTransitionManager.GUIName.RETRY, repeatButton);
        namesToButtons.put(StateTransitionManager.GUIName.START, startPurgeButton);
        namesToButtons.put(StateTransitionManager.GUIName.BACK, backButton);
        namesToButtons.put(StateTransitionManager.GUIName.COMPLETE, okButton);
    }

    /**
     * According to the available transitions, show the appropriate buttons.
     */
    private void showAppropriateButtons(PurgeState state)
    {
        if (state.showCancelButton())
        {
            cancelPurgeButton.setVisible(true);
        }
        for (StateTransition<PurgeState> allowedTransition : transitionManager.getTransitions())
        {
            if (namesToButtons.containsKey(allowedTransition.getGUIName()))
            {
                namesToButtons.get(allowedTransition.getGUIName()).setVisible(true);
            }
        }
    }

    private void hideAllButtons()
    {
        cancelPurgeButton.setVisible(false);
        proceedButton.setVisible(false);
        repeatButton.setVisible(false);
        startPurgeButton.setVisible(false);
        backButton.setVisible(false);
        okButton.setVisible(false);
        purgeDetailsGrid0.setVisible(false);
        purgeDetailsGrid1.setVisible(false);
        diagramContainer.setVisible(false);
    }

    public void setState(PurgeState state)
    {
        steno.debug("go to state " + state);
        steno.debug("printer status is " + printer.printerStatusProperty().get());
        hideAllButtons();
        showAppropriateButtons(state);
        resettingPrinter.setVisible(false);
        purgeStatus.setVisible(true);
        purgeStatus.setText(state.getStepTitle());
        purgeTemperature0.intValueProperty().removeListener(purgeTempEntryListener0);
        if (purgeTwoNozzleHeaters.get())
        {
            purgeTemperature1.intValueProperty().removeListener(purgeTempEntryListener1);
        }
        switch (state)
        {
            case IDLE:
                break;
            case INITIALISING:
                break;
            case CONFIRM_TEMPERATURE:
                showCurrentMaterial0();
                purgeTemperature0.intValueProperty().addListener(purgeTempEntryListener0);
                purgeDetailsGrid0.setVisible(true);

                if (purgeTwoNozzleHeaters.get())
                {
                    showCurrentMaterial1();
                    purgeTemperature1.intValueProperty().addListener(purgeTempEntryListener1);
                } else
                {
                    purgeThisNozzle0.setVisible(false);
                }
                purgeDetailsGrid1.setVisible(purgeTwoNozzleHeaters.get());
                break;
            case HEATING:
                break;
            case RUNNING_PURGE:
                diagramContainer.setVisible(true);
                break;
            case FINISHED:
                diagramContainer.setVisible(true);
                break;
            case DONE:
                closeWindow(null);
                break;
            case FAILED:
                break;
            case CANCELLING:
                resettingPrinter.setVisible(true);
                purgeStatus.setVisible(false);
                break;
            case CANCELLED:
                closeWindow(null);
                break;
        }
    }

    /**
     * Bind to the given printer. This is only called once for a given purge and should not be
     * called again during the purge.
     */
    private void bindPrinter(Printer printer)
    {
        if (this.printer != null)
        {
            progressDisplay.unbindFromPrinter();
            startPurgeButton.getTag().removeAllConditionalText();
            proceedButton.getTag().removeAllConditionalText();
            cmbCurrentMaterial0.visibleProperty().unbind();
            textCurrentMaterial0.visibleProperty().unbind();
            cmbCurrentMaterial1.visibleProperty().unbind();
            textCurrentMaterial1.visibleProperty().unbind();
            proceedButton.disableProperty().unbind();
            purgeThisNozzle0.onActionProperty().unbind();
        }

        this.printer = printer;

        BooleanBinding reel0Present = Bindings.valueAt(printer.reelsProperty(), 0).isNotNull();
        BooleanBinding reel1Present = Bindings.valueAt(printer.reelsProperty(), 1).isNotNull();

        purgeTwoNozzleHeaters = Bindings.size(
            printer.headProperty().get().getNozzleHeaters()).greaterThan(1);

        cmbCurrentMaterial0.visibleProperty().bind(reel0Present.not());
        textCurrentMaterial0.visibleProperty().bind(reel0Present);

        cmbCurrentMaterial1.visibleProperty().bind(reel1Present.not());
        textCurrentMaterial1.visibleProperty().bind(reel1Present);

        progressDisplay.bindToPrinter(printer);
    }

    /**
     * If a reel is loaded then show and select its material, else show and select the material from
     * the combo box. This should be called whenever a reel is loaded/unloaded or changed.
     */
    private void showCurrentMaterial0()
    {
        if (printer.reelsProperty().containsKey(0))
        {
            currentMaterial0 = Lookup.getFilamentContainer().getFilamentByID(
                printer.reelsProperty().get(0).filamentIDProperty().get());
        } else
        {
            currentMaterial0 = cmbCurrentMaterial0.getValue();
        }
        if (currentMaterial0 != null)
        {
            selectMaterial0(currentMaterial0);
        } else
        {
            transitionManager.setPurgeTemperature(0, 0);
            purgeTemperature0.textProperty().set("0");
        }
    }

    /**
     * If a reel is loaded then show and select its material, else show and select the material from
     * the combo box. This should be called whenever a reel is loaded/unloaded or changed.
     */
    private void showCurrentMaterial1()
    {
        if (printer.reelsProperty().containsKey(1))
        {
            currentMaterial1 = Lookup.getFilamentContainer().getFilamentByID(
                printer.reelsProperty().get(1).filamentIDProperty().get());
        } else
        {
            currentMaterial1 = cmbCurrentMaterial1.getValue();
        }
        if (currentMaterial1 != null)
        {
            selectMaterial1(currentMaterial1);
        } else
        {
            transitionManager.setPurgeTemperature(1, 0);
            purgeTemperature1.textProperty().set("0");
        }
    }

    private boolean headHasTwoNozzleHeaters(Printer printer)
    {
        return printer.headProperty().get().getNozzleHeaters().size() == 2;
    }

    private void installTag(PurgeStateTransitionManager transitionManager,
        Printer printer, GraphicButtonWithLabel button)
    {

        button.uninstallTag();
        button.installTag();

        BooleanBinding doorIsOpen = printer.getPrinterAncillarySystems().doorOpenProperty().and(
            Lookup.getUserPreferences().safetyFeaturesOnProperty());

        BooleanBinding extruder0NotLoaded = printer.extrudersProperty().get(0).
            filamentLoadedProperty().not();

        BooleanBinding notPurgingAndNotIdle = Bindings.and(
            printer.printerStatusProperty().isNotEqualTo(PrinterStatus.PURGING_HEAD),
            printer.printerStatusProperty().isNotEqualTo(PrinterStatus.IDLE));

        button.getTag().addConditionalText("dialogs.cantPurgeDoorIsOpenMessage", doorIsOpen);

        if (!headHasTwoNozzleHeaters(printer))
        {
            button.getTag().addConditionalText("dialogs.cantPrintNoFilamentMessage",
                                               extruder0NotLoaded);

            BooleanBinding isDisabled = notPurgingAndNotIdle.or(doorIsOpen).or(extruder0NotLoaded);
            button.disableProperty().bind(isDisabled);
        } else
        {

            BooleanBinding extruder1NotLoaded = printer.extrudersProperty().get(1).
                filamentLoadedProperty().not();

            BooleanBinding purgingNozzleHeater0 = new BooleanBinding()
            {
                {
                    super.bind(transitionManager.getPurgeNozzleHeater0());
                }

                @Override
                protected boolean computeValue()
                {
                    return transitionManager.getPurgeNozzleHeater0().get();
                }
            };

            BooleanBinding purgingNozzleHeater1 = new BooleanBinding()
            {
                {
                    super.bind(transitionManager.getPurgeNozzleHeater1());
                }

                @Override
                protected boolean computeValue()
                {
                    return transitionManager.getPurgeNozzleHeater1().get();
                }
            };

            if (button == proceedButton)
            {

                button.getTag().addConditionalText("dialogs.cantPrintNoFilamentMessage0",
                                                   purgingNozzleHeater0.and(extruder0NotLoaded));

                button.getTag().addConditionalText("dialogs.cantPrintNoFilamentMessage1",
                                                   purgingNozzleHeater1.and(extruder1NotLoaded));

                BooleanBinding isDisabled = notPurgingAndNotIdle.or(doorIsOpen)
                    .or(purgingNozzleHeater0.and(extruder0NotLoaded))
                    .or(purgingNozzleHeater1.and(extruder1NotLoaded))
                    .or(purgingNozzleHeater0.not().and(purgingNozzleHeater1.not()));
                button.disableProperty().bind(isDisabled);
            }
        }
    }

    public void purgeAndPrint(Project project, PrinterSettings printerSettings, Printer printerToUse)
    {
        this.project = project;
        bindPrinter(printerToUse);
        selectMaterial0(printerSettings.getFilament0());
        cmbCurrentMaterial0.setValue(printerSettings.getFilament0());
        selectMaterial1(printerSettings.getFilament1());
        cmbCurrentMaterial1.setValue(printerSettings.getFilament1());

        setPurgeForRequiredNozzles(project);

        startPurge();
    }

    /**
     * If a nozzle needs purging then set the appropriate flag to true.
     */
    private void setPurgeForRequiredNozzles(Project project)
    {
        if (!headHasTwoNozzleHeaters(printer))
        {
            purgeThisNozzle0.setSelected(true);
            purgeThisNozzle1.setSelected(false);
        } else
        {
            purgeThisNozzle0.setSelected(false);
            purgeThisNozzle1.setSelected(false);
            if (PrinterUtils.isPurgeNecessaryForNozzleHeater(project, printer, 0))
            {
                purgeThisNozzle0.setSelected(true);
            }
            if (PrinterUtils.isPurgeNecessaryForNozzleHeater(project, printer, 1))
            {
                purgeThisNozzle1.setSelected(true);
            }
        }
    }

    public void purge(Printer printer)
    {
        bindPrinter(printer);

        // default to purging both nozzles.
        purgeThisNozzle0.setSelected(true);
        purgeThisNozzle1.setSelected(true);

        startPurge();
    }

    private void startPurge()
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.PURGE);

        try
        {
            transitionManager = printer.startPurge();

            currentMaterialTemperature0.textProperty().unbind();
            lastMaterialTemperature0.textProperty().unbind();
            currentMaterialTemperature0.textProperty().bind(
                transitionManager.getCurrentMaterialTemperature(0).asString());

            lastMaterialTemperature0.textProperty().bind(
                transitionManager.getLastMaterialTemperature(0).asString());

            purgeThisNozzle0.onActionProperty().set(
                (EventHandler<ActionEvent>) (ActionEvent event) ->
                {
                    transitionManager.setPurgeNozzleHeater0(purgeThisNozzle0.isSelected());
                });

            purgeThisNozzle1.onActionProperty().set(
                (EventHandler<ActionEvent>) (ActionEvent event) ->
                {
                    transitionManager.setPurgeNozzleHeater1(purgeThisNozzle1.isSelected());
                });

            if (purgeTwoNozzleHeaters.get())
            {

                currentMaterialTemperature1.textProperty().unbind();
                lastMaterialTemperature1.textProperty().unbind();
                currentMaterialTemperature1.textProperty().bind(
                    transitionManager.getCurrentMaterialTemperature(1).asString());

                lastMaterialTemperature1.textProperty().bind(
                    transitionManager.getLastMaterialTemperature(1).asString());

            }

            transitionManager.stateGUITProperty().addListener(new ChangeListener()
            {
                @Override
                public void changed(ObservableValue observable, Object oldValue, Object newValue)
                {
                    setState((PurgeState) newValue);
                }
            });

            transitionManager.start();

            transitionManager.setPurgeNozzleHeater0(purgeThisNozzle0.isSelected());
            transitionManager.setPurgeNozzleHeater1(purgeThisNozzle1.isSelected());

            installTag(transitionManager, printer, startPurgeButton);
            installTag(transitionManager, printer, proceedButton);

            setState(PurgeState.IDLE);
        } catch (PrinterException ex)
        {
            steno.error("Error starting purge: " + ex);
        }
    }

    private void setupMaterialCombos()
    {
        cmbCurrentMaterial0.setCellFactory(
            (ListView<Filament> param) -> new FilamentCell());

        cmbCurrentMaterial0.setButtonCell(cmbCurrentMaterial0.getCellFactory().call(null));

        repopulateCmbCurrentMaterials();

        cmbCurrentMaterial0.valueProperty().addListener(
            (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
            {
                selectMaterial0(newValue);
            });

        cmbCurrentMaterial1.setCellFactory(
            (ListView<Filament> param) -> new FilamentCell());

        cmbCurrentMaterial1.setButtonCell(cmbCurrentMaterial1.getCellFactory().call(null));

        cmbCurrentMaterial1.valueProperty().addListener(
            (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
            {
                selectMaterial1(newValue);
            });

    }

    /**
     * Tell the purge state machine about the (changed) current material, and update the relevant
     * text fields.
     */
    private void selectMaterial0(Filament filament)
    {
        currentMaterial0 = filament;
        if (transitionManager != null)
        {
            try
            {
                transitionManager.setPurgeFilament(0, filament);
            } catch (PrintException ex)
            {
                ex.printStackTrace();
                steno.error("Error setting purge filament");
            }
            textCurrentMaterial0.setText(currentMaterial0.getLongFriendlyName() + " "
                + currentMaterial0.getMaterial().getFriendlyName());
            purgeTemperature0.setText(transitionManager.getPurgeTemperature(0).asString().get());
        }
    }

    /**
     * Tell the purge state machine about the (changed) current material, and update the relevant
     * text fields.
     */
    private void selectMaterial1(Filament filament)
    {
        currentMaterial1 = filament;
        if (transitionManager != null)
        {
            try
            {
                transitionManager.setPurgeFilament(1, filament);
            } catch (PrintException ex)
            {
                ex.printStackTrace();
                steno.error("Error setting purge filament");
            }
            textCurrentMaterial1.setText(currentMaterial1.getLongFriendlyName() + " "
                + currentMaterial1.getMaterial().getFriendlyName());
            purgeTemperature1.setText(transitionManager.getPurgeTemperature(1).asString().get());
        }
    }

    private void repopulateCmbCurrentMaterials()
    {
        FilamentContainer filamentContainer = Lookup.getFilamentContainer();
        try
        {
            ObservableList<Filament> filamentList = FXCollections.observableArrayList();
            ObservableList<Filament> appFilaments = FXCollections.observableArrayList();
            ObservableList<Filament> userFilaments = FXCollections.observableArrayList();
            appFilaments.addAll(filamentContainer.getAppFilamentList().sorted(
                (Filament o1, Filament o2)
                -> o1.getFriendlyFilamentName().compareTo(o2.getFriendlyFilamentName())));
            if (Lookup.getUserPreferences().isAdvancedMode())
            {
                appFilaments.addAll(filamentContainer.getUserFilamentList().sorted(
                    (Filament o1, Filament o2)
                    -> o1.getFriendlyFilamentName().compareTo(o2.getFriendlyFilamentName())));
                userFilaments.addAll(filamentContainer.getUserFilamentList().sorted(
                    (Filament o1, Filament o2)
                    -> o1.getFriendlyFilamentName().compareTo(o2.getFriendlyFilamentName())));
            }
            filamentList.addAll(appFilaments);
            filamentList.addAll(userFilaments);
            cmbCurrentMaterial0.setItems(filamentList);
            cmbCurrentMaterial1.setItems(filamentList);
        } catch (NoClassDefFoundError exception)
        {
            // this should only happen in SceneBuilder            
        }
    }

    public static class FilamentCell extends ListCell<Filament>
    {

        private static int SWATCH_SQUARE_SIZE = 16;

        HBox cellContainer;
        Rectangle rectangle = new Rectangle();
        Label label;

        public FilamentCell()
        {
            cellContainer = new HBox();
            cellContainer.setAlignment(Pos.CENTER_LEFT);
            rectangle = new Rectangle(SWATCH_SQUARE_SIZE, SWATCH_SQUARE_SIZE);
            label = new Label();
            label.setId("materialComponentComboLabel");
            cellContainer.getChildren().addAll(rectangle, label);
        }

        @Override
        protected void updateItem(Filament item, boolean empty)
        {
            super.updateItem(item, empty);
            if (item != null && !empty)
            {
                Filament filament = (Filament) item;
                setGraphic(cellContainer);
                rectangle.setFill(filament.getDisplayColour());

                label.setText(filament.getLongFriendlyName() + " "
                    + filament.getMaterial().getFriendlyName());
                label.getStyleClass().add("filamentSwatchPadding");
            } else
            {
                setGraphic(null);
            }

        }

    }
}
