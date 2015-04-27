package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.coreUI.components.LargeProgress;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.components.buttons.GraphicButtonWithLabel;
import celtech.printerControl.PrinterStatus;
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
import celtech.printerControl.model.StateTransition;
import celtech.printerControl.model.StateTransitionManager;
import celtech.printerControl.model.StateTransitionManager.GUIName;
import celtech.services.slicer.PrintQualityEnumeration;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class PurgeInsetPanelController2 implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        PurgeInsetPanelController2.class.getName());

    private Project project = null;
    private Printer printer = null;
    private double printPercent;
    private DiagramHandler diagramHandler;

    PurgeStateTransitionManager transitionManager;

    Map<StateTransitionManager.GUIName, Region> namesToButtons = new HashMap<>();

    private final ChangeListener<Number> purgeTempEntryListener
        = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            transitionManager.setPurgeTemperature(newValue.intValue());
        };

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
    protected LargeProgress purgeProgressBar;
    private Filament purgeFilament;

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

        purgeProgressBar.setTargetLegend("");
        purgeProgressBar.setProgressDescription(Lookup.i18n("calibrationPanel.printingCaps"));
        purgeProgressBar.setTargetValue("");

        startPurgeButton.installTag();
        proceedButton.installTag();
        
        diagramHandler = new DiagramHandler(diagramContainer, resources);
        diagramHandler.initialise();
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
                steno.debug("show button " + allowedTransition.getGUIName());
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
        purgeProgressBar.setVisible(false);
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
                lastMaterialTemperature.setText(String.valueOf(
                    transitionManager.getLastMaterialTemperature()));
                currentMaterialTemperature.setText(String.valueOf(
                    transitionManager.getCurrentMaterialTemperature()));
                purgeTemperature.setText(String.valueOf(transitionManager.getPurgeTemperature()));
                purgeTemperature.intValueProperty().addListener(purgeTempEntryListener);
                purgeDetailsGrid.setVisible(true);
                break;
            case HEATING:
                break;
            case RUNNING_PURGE:
                purgeProgressBar.setVisible(true);
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

    private final ChangeListener<Number> printPercentListener
        = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            printPercent = newValue.doubleValue();
            updateProgressPrint();
        };

    private void updateProgressPrint()
    {
        if (purgeProgressBar.isVisible())
        {
            String currentPrintPercentStr = ((int) (printPercent * 100)) + "%";
            purgeProgressBar.setCurrentValue(currentPrintPercentStr);
            purgeProgressBar.setProgressPercent(printPercent);
        }
    }

    private void removePrintProgressListeners(Printer printer)
    {
        printer.getPrintEngine().progressProperty().removeListener(printPercentListener);
    }

    private void setupPrintProgressListeners(Printer printer)
    {
        printer.getPrintEngine().progressProperty().addListener(printPercentListener);
    }

    public void purgeAndPrint(Project project, Filament filament,
        PrintQualityEnumeration printQuality, SlicerParametersFile settings, Printer printerToUse)
    {
        this.project = project;
        //TODO what about multiple reels etc
        bindPrinter(printerToUse, project.getPrinterSettings().getFilament0());

        ApplicationStatus.getInstance().setMode(ApplicationMode.PURGE);
    }

    private void bindPrinter(Printer printer, Filament purgeFilament)
    {
        if (this.printer != null)
        {
            removePrintProgressListeners(this.printer);
            startPurgeButton.getTag().removeAllConditionalText();
            proceedButton.getTag().removeAllConditionalText();
        }
        this.printer = printer;
        setupPrintProgressListeners(printer);

        this.purgeFilament = purgeFilament;

        installTag(printer, startPurgeButton);
        installTag(printer, proceedButton);
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

    public void purge(Printer printer)
    {
        bindPrinter(printer, null);

        ApplicationStatus.getInstance().setMode(ApplicationMode.PURGE2);

        try
        {
            transitionManager = printer.startPurge();
            transitionManager.setPurgeFilament(purgeFilament);
            transitionManager.stateGUITProperty().addListener(new ChangeListener()
            {
                @Override
                public void changed(ObservableValue observable, Object oldValue, Object newValue)
                {
                    setState((PurgeState) newValue);
                }
            });
            transitionManager.start();
            setState(PurgeState.IDLE);
        } catch (PrinterException ex)
        {
            steno.error("Error starting purge: " + ex);
        }
    }
    
}
