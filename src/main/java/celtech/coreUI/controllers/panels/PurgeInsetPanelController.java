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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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
    private double printPercent;
    private Filament currentMaterial;

    PurgeStateTransitionManager transitionManager;

    Map<StateTransitionManager.GUIName, Region> namesToButtons = new HashMap<>();

    private final ChangeListener<Number> purgeTempEntryListener
            = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                transitionManager.setPurgeTemperature(newValue.intValue());
            };

    @FXML
    private VBox container;

    @FXML
    private Group diagram;

    @FXML
    private VBox diagramContainer;

    @FXML
    private GraphicButtonWithLabel startPurgeButton;

    @FXML
    private Text purgeStatus;

    @FXML
    private RestrictedNumberField purgeTemperature;

    @FXML
    private Text currentMaterialTemperature;

    @FXML
    private Text resettingPrinter;

    @FXML
    private GridPane purgeDetailsGrid;

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
    private Text lastMaterialTemperature;

    @FXML
    private ProgressDisplay progressDisplay;

    @FXML
    private Text textCurrentMaterial;

    @FXML
    private ComboBox<Filament> cmbCurrentMaterial;

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

    private double initialDiagramWidth = 0;
    private double initialDiagramHeight = 0;
    private double diagramAspectRatio = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        populateNamesToButtons();

        startPurgeButton.installTag();
        proceedButton.installTag();

        FXMLUtilities.addColonsToLabels(purgeDetailsGrid);

        setupMaterialCombo();

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
                    PurgeInsetPanelController.this.showCurrentMaterial();
                }
            }

            @Override
            public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
            {
                if (PurgeInsetPanelController.this.printer == printer)
                {
                    PurgeInsetPanelController.this.showCurrentMaterial();
                }
            }

            @Override
            public void whenReelChanged(Printer printer, Reel reel)
            {
                if (PurgeInsetPanelController.this.printer == printer)
                {
                    PurgeInsetPanelController.this.showCurrentMaterial();
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

        initialDiagramWidth = diagramContainer.getWidth();
        initialDiagramHeight = diagramContainer.getHeight();
        diagramAspectRatio = initialDiagramWidth / initialDiagramHeight;

        diagramContainer.widthProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                resizePurgeDisplay(diagramContainer.getWidth(), diagramContainer.getHeight());
            }
        });

        diagramContainer.heightProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                resizePurgeDisplay(diagramContainer.getWidth(), diagramContainer.getHeight());
            }
        });

        diagramContainer.visibleProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                resizePurgeDisplay(diagramContainer.getWidth(), diagramContainer.getHeight());
            }
        });

        resizePurgeDisplay(diagramContainer.getWidth(), diagramContainer.getHeight());
    }

    private void resizePurgeDisplay(double displayWidth, double displayHeight)
    {
        final double beginWidth = 804;
        final double beginHeight = 345;
        final double aspect = beginWidth / beginHeight;

        displayWidth = displayWidth * 0.9;
        displayHeight = displayHeight * 0.9;

        double displayAspect = displayWidth / displayHeight;

        double newWidth = 0;
        double newHeight = 0;

        steno.info("Being called with " + displayWidth + " : " + displayHeight);

        if (displayAspect >= aspect)
        {
            // Drive from height
            newWidth = displayHeight * aspect;
            newHeight = displayHeight;
        } else
        {
            //Drive from width
            newHeight = displayWidth / aspect;
            newWidth = displayWidth;
        }

        double xScale = newWidth / beginWidth;
        double yScale = newHeight / beginHeight;

        diagram.setScaleX(xScale);
        diagram.setScaleY(yScale);
//        diagramContainer.setPrefHeight(newHeight);
//        diagramContainer.setPrefWidth(newWidth);
//        diagramContainer.setTranslateX(newWidth / 2);
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
        purgeDetailsGrid.setVisible(false);
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
        purgeTemperature.intValueProperty().removeListener(purgeTempEntryListener);
        switch (state)
        {
            case IDLE:
                break;
            case INITIALISING:
                break;
            case CONFIRM_TEMPERATURE:
                showCurrentMaterial();
                purgeTemperature.intValueProperty().addListener(purgeTempEntryListener);
                purgeDetailsGrid.setVisible(true);
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
     * Bind to the given printer. This is only called once for a given purge and
     * should not be called again during the purge.
     */
    private void bindPrinter(Printer printer)
    {
        progressDisplay.unbindFromPrinter();

        if (this.printer != null)
        {
            startPurgeButton.getTag().removeAllConditionalText();
            proceedButton.getTag().removeAllConditionalText();
            cmbCurrentMaterial.visibleProperty().unbind();
            textCurrentMaterial.visibleProperty().unbind();
            proceedButton.disableProperty().unbind();
        }

        this.printer = printer;
        progressDisplay.bindToPrinter(printer);

        installTag(printer, startPurgeButton);
        installTag(printer, proceedButton);

        BooleanBinding reel0Present = Bindings.valueAt(printer.reelsProperty(), 0).isNotNull();

        cmbCurrentMaterial.visibleProperty().bind(reel0Present.not());
        textCurrentMaterial.visibleProperty().bind(reel0Present);

    }

    /**
     * If a reel is loaded then show and select its material, else show and
     * select the material from the combo box. This should be called whenever a
     * reel is loaded/unloaded or changed.
     */
    private void showCurrentMaterial()
    {
        if (printer.reelsProperty().containsKey(0))
        {
            currentMaterial = Lookup.getFilamentContainer().getFilamentByID(
                    printer.reelsProperty().get(0).filamentIDProperty().get());
        } else
        {
            currentMaterial = cmbCurrentMaterial.getValue();
        }
        if (currentMaterial != null)
        {
            selectMaterial(currentMaterial);
        } else
        {
            transitionManager.setPurgeTemperature(-1);
            purgeTemperature.textProperty().set("-1");
        }
    }

    private void installTag(Printer printer, GraphicButtonWithLabel button)
    {
        button.getTag().addConditionalText("dialogs.cantPurgeDoorIsOpenMessage",
                printer.getPrinterAncillarySystems().doorOpenProperty().and(
                        Lookup.getUserPreferences().safetyFeaturesOnProperty()));
        button.getTag().addConditionalText("dialogs.cantPrintNoFilamentMessage",
                printer.extrudersProperty().get(0).
                filamentLoadedProperty().not());

        button.disableProperty().bind(Bindings.and(
                printer.printerStatusProperty().isNotEqualTo(PrinterStatus.PURGING_HEAD),
                printer.printerStatusProperty().isNotEqualTo(PrinterStatus.IDLE))
                .or(printer.getPrinterAncillarySystems().doorOpenProperty().and(
                                Lookup.getUserPreferences().safetyFeaturesOnProperty()))
                .or(printer.extrudersProperty().get(0).filamentLoadedProperty().not()));
    }

    public void purgeAndPrint(Project project, Filament filament, Printer printerToUse)
    {
        this.project = project;
        //TODO what about multiple reels etc
        Filament purgeFilament = project.getPrinterSettings().getFilament0();
        bindPrinter(printerToUse);
        selectMaterial(filament);
        cmbCurrentMaterial.setValue(filament);
        startPurge();
    }

    public void purge(Printer printer)
    {
        bindPrinter(printer);
        startPurge();
    }

    private void startPurge()
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.PURGE);

        try
        {
            transitionManager = printer.startPurge();

            currentMaterialTemperature.textProperty().unbind();
            lastMaterialTemperature.textProperty().unbind();
            currentMaterialTemperature.textProperty().bind(
                    transitionManager.getCurrentMaterialTemperature().asString());

            lastMaterialTemperature.textProperty().bind(
                    transitionManager.getLastMaterialTemperature().asString());

            transitionManager.stateGUITProperty().addListener(new ChangeListener()
            {
                @Override
                public void changed(ObservableValue observable, Object oldValue, Object newValue)
                {
                    setState((PurgeState) newValue);
                }
            });

            setupProceedButton();

            transitionManager.start();
            setState(PurgeState.IDLE);
        } catch (PrinterException ex)
        {
            steno.error("Error starting purge: " + ex);
        }
    }

    private void setupMaterialCombo()
    {
        cmbCurrentMaterial.setCellFactory(
                (ListView<Filament> param) -> new FilamentCell());

        cmbCurrentMaterial.setButtonCell(cmbCurrentMaterial.getCellFactory().call(null));

        repopulateCmbCurrentMaterial();

        cmbCurrentMaterial.valueProperty().addListener(
                (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
                {
                    selectMaterial(newValue);
                });

    }

    /**
     * Tell the purge state machine about the (changed) current material, and
     * update the relevant text fields.
     */
    private void selectMaterial(Filament filament)
    {
        currentMaterial = filament;
        if (transitionManager != null)
        {
            try
            {
                transitionManager.setPurgeFilament(filament);
            } catch (PrintException ex)
            {
                ex.printStackTrace();
                steno.error("Error setting purge filament");
            }
            textCurrentMaterial.setText(currentMaterial.getLongFriendlyName() + " " + currentMaterial.getMaterial().getFriendlyName());
            purgeTemperature.setText(transitionManager.getPurgeTemperature().asString().get());
        }
    }

    private void repopulateCmbCurrentMaterial()
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
            cmbCurrentMaterial.setItems(filamentList);
        } catch (NoClassDefFoundError exception)
        {
            // this should only happen in SceneBuilder            
        }
    }

    /**
     * The proceed button should be disabled on the CONFIRM_TEMPERATURE page if
     * the purge temperature is not greater than 0.
     */
    private void setupProceedButton()
    {
        BooleanBinding proceedDisabled = new BooleanBinding()
        {
            {
                super.bind(transitionManager.stateGUITProperty(),
                        transitionManager.getPurgeTemperature());
            }

            @Override
            protected boolean computeValue()
            {
                if (!transitionManager.stateGUITProperty().get().equals(CONFIRM_TEMPERATURE))
                {
                    return false;
                }
                if (transitionManager.getPurgeTemperature().greaterThan(0).get())
                {
                    return false;
                }
                return true;
            }
        };
        proceedButton.disableProperty().bind(proceedDisabled);
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
