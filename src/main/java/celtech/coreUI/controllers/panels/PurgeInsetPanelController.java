package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.LargeProgress;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.components.buttons.GraphicButton;
import celtech.coreUI.components.buttons.GraphicButtonWithLabel;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.services.purge.PurgeState;
import celtech.services.slicer.PrintQualityEnumeration;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.dialog.Dialogs;

/**
 *
 * @author Ian
 */
public class PurgeInsetPanelController implements Initializable, PurgeStateListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        PurgeInsetPanelController.class.getName());

    private final PurgeHelper purgeHelper = new PurgeHelper();

    private Project project = null;
    private Filament filament = null;
    private PrintQualityEnumeration printQuality = null;
    private SlicerParametersFile settings = null;
    private Printer printerToUse = null;
    private String macroToExecuteAfterPurge = null;
    private double printPercent;

    private final ChangeListener<Number> purgeTempEntryListener = new ChangeListener<Number>()
    {
        @Override
        public void changed(
            ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
        {
            purgeHelper.setPurgeTemperature(newValue.intValue());
        }
    };

    @FXML
    private VBox container;

    @FXML
    private GraphicButtonWithLabel startPurgeButton;

    @FXML
    private Text purgeStatus;

    @FXML
    private RestrictedNumberField purgeTemperature;

    @FXML
    private Text currentMaterialTemperature;

    @FXML
    private GridPane purgeDetailsGrid;

    @FXML
    private GraphicButtonWithLabel cancelPurgeButton;

    @FXML
    private GraphicButtonWithLabel proceedButton;

    @FXML
    private GraphicButtonWithLabel okButton;

    @FXML
    private GraphicButtonWithLabel repeatButton;

    @FXML
    private Text lastMaterialTemperature;

    @FXML
    protected LargeProgress purgeProgressBar;

    @FXML
    void start(ActionEvent event)
    {
        purgeHelper.setState(PurgeState.INITIALISING);
    }

    @FXML
    void proceed(ActionEvent event)
    {
        purgeHelper.setState(PurgeState.HEATING);
    }

    @FXML
    void cancel(ActionEvent event)
    {
        cancelPurgeAction();
    }

    @FXML
    void repeat(ActionEvent event)
    {
        repeatPurgeAction();
    }

    @FXML
    void okPressed(ActionEvent event)
    {
        closeWindow(null);
    }

    @FXML
    void closeWindow(ActionEvent event)
    {
        boolean purgeCompletedOK = (purgeHelper.getState() == PurgeState.FINISHED);

        purgeHelper.setState(PurgeState.IDLE);
        ApplicationStatus.getInstance().returnToLastMode();

        if (project != null && purgeCompletedOK)
        {
            final Project projectCopy = project;

            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    Dialogs.create()
                        .owner(null)
                        .title(DisplayManager.getLanguageBundle().getString("dialogs.clearBedTitle")).
                        masthead(null)
                        .message(DisplayManager.getLanguageBundle().getString(
                                "dialogs.clearBedInstruction"))
                        .showWarning();
                    printerToUse.printProject(projectCopy, filament, printQuality, settings);
                }
            });

            project = null;

        } else if (macroToExecuteAfterPurge != null)
        {
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    Dialogs.create()
                        .owner(null)
                        .title(DisplayManager.getLanguageBundle().getString("dialogs.clearBedTitle")).
                        masthead(null)
                        .message(DisplayManager.getLanguageBundle().getString(
                                "dialogs.clearBedInstruction"))
                        .showWarning();
                    try
                    {
                        printerToUse.executeMacro(GCodeMacros.getFilename(macroToExecuteAfterPurge));
                    } catch (PrinterException ex)
                    {
                        steno.error("Error running macro");
                    }
                }
            });

            macroToExecuteAfterPurge = null;
        }
    }

    /**
     *
     */
    public void cancelPurgeAction()
    {
        purgeHelper.cancelPurgeAction();
        closeWindow(null);
    }

    public void repeatPurgeAction()
    {
        purgeHelper.repeatPurgeAction();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        purgeHelper.addStateListener(this);
        purgeHelper.setState(PurgeState.IDLE);

        purgeProgressBar.setTargetLegend("");
        purgeProgressBar.setProgressDescription(Lookup.i18n("calibrationPanel.printingCaps"));
        purgeProgressBar.setTargetValue("");

        startPurgeButton.installTag();
    }

    @Override
    public void setState(PurgeState state)
    {
        switch (state)
        {
            case IDLE:
                startPurgeButton.setVisible(true);
                cancelPurgeButton.setVisible(true);
                purgeDetailsGrid.setVisible(false);
                purgeProgressBar.setVisible(false);
                proceedButton.setVisible(false);
                repeatButton.setVisible(false);
                okButton.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                purgeTemperature.intValueProperty().removeListener(purgeTempEntryListener);
                break;
            case INITIALISING:
                startPurgeButton.setVisible(false);
                cancelPurgeButton.setVisible(true);
                proceedButton.setVisible(false);
                purgeProgressBar.setVisible(false);
                repeatButton.setVisible(false);
                okButton.setVisible(false);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                break;
            case CONFIRM_TEMPERATURE:
                startPurgeButton.setVisible(false);
                cancelPurgeButton.setVisible(true);
                proceedButton.setVisible(true);
                purgeProgressBar.setVisible(false);
                repeatButton.setVisible(false);
                okButton.setVisible(false);
                lastMaterialTemperature.setText(String.valueOf(
                    purgeHelper.getLastMaterialTemperature()));
                currentMaterialTemperature.setText(String.valueOf(
                    purgeHelper.getCurrentMaterialTemperature()));
                purgeTemperature.setText(String.valueOf(purgeHelper.getPurgeTemperature()));
                purgeTemperature.intValueProperty().addListener(purgeTempEntryListener);
                purgeDetailsGrid.setVisible(true);
                purgeStatus.setText(state.getStepTitle());
                break;
            case HEATING:
                startPurgeButton.setVisible(false);
                cancelPurgeButton.setVisible(true);
                proceedButton.setVisible(false);
                repeatButton.setVisible(false);
                purgeProgressBar.setVisible(false);
                okButton.setVisible(false);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                purgeTemperature.intValueProperty().removeListener(purgeTempEntryListener);
                break;
            case RUNNING_PURGE:
                startPurgeButton.setVisible(false);
                cancelPurgeButton.setVisible(true);
                proceedButton.setVisible(false);
                repeatButton.setVisible(false);
                purgeProgressBar.setVisible(true);
                okButton.setVisible(false);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                break;
            case FINISHED:
                startPurgeButton.setVisible(false);
                cancelPurgeButton.setVisible(false);
                proceedButton.setVisible(false);
                repeatButton.setVisible(true);
                purgeProgressBar.setVisible(false);
                okButton.setVisible(true);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                purgeTemperature.intValueProperty().removeListener(purgeTempEntryListener);
                break;
            case FAILED:
                startPurgeButton.setVisible(true);
                cancelPurgeButton.setVisible(true);
                proceedButton.setVisible(false);
                repeatButton.setVisible(false);
                purgeProgressBar.setVisible(false);
                okButton.setVisible(false);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                purgeTemperature.intValueProperty().removeListener(purgeTempEntryListener);
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
            purgeProgressBar.setProgress(printPercent);
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
        this.filament = filament;
        this.printQuality = printQuality;
        this.settings = settings;
        bindPrinter(printerToUse);

        ApplicationStatus.getInstance().setMode(ApplicationMode.PURGE);
    }

    private void bindPrinter(Printer printerToUse1)
    {
        if (this.printerToUse != null)
        {
            removePrintProgressListeners(this.printerToUse);
            startPurgeButton.getTag().removeAllConditionalText();
        }
        this.printerToUse = printerToUse1;
        purgeHelper.setPrinterToUse(printerToUse1);
        setupPrintProgressListeners(printerToUse1);
        startPurgeButton.getTag().addConditionalText("dialogs.cantPurgeDoorIsOpenMessage",
                                                     printerToUse1.getPrinterAncillarySystems().
                                                     lidOpenProperty().not().not());
        startPurgeButton.getTag().addConditionalText("dialogs.cantPrintNoFilamentMessage",
                                                     printerToUse1.extrudersProperty().get(0).
                                                     filamentLoadedProperty().not());

        startPurgeButton.disableProperty().bind(printerToUse1.canPrintProperty().not()
            .or(printerToUse1.getPrinterAncillarySystems().lidOpenProperty())
            .or(printerToUse1.extrudersProperty().get(0).filamentLoadedProperty().not()));
    }

    public void purgeAndRunMacro(String macroName, Printer printerToUse)
    {
        this.macroToExecuteAfterPurge = macroName;

        bindPrinter(printerToUse);

        ApplicationStatus.getInstance().setMode(ApplicationMode.PURGE);
    }

    public void purge(Printer printerToUse)
    {

        bindPrinter(printerToUse);

        ApplicationStatus.getInstance().setMode(ApplicationMode.PURGE);
    }
}
