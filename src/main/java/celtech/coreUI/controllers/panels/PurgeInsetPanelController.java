package celtech.coreUI.controllers.panels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.services.purge.PurgeState;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
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

    private final Stenographer steno = StenographerFactory.getStenographer(PurgeInsetPanelController.class.getName());

    private final PurgeHelper purgeHelper = new PurgeHelper();

    private ResourceBundle i18nBundle = null;

    private Project project = null;
    private Filament filament = null;
    private PrintQualityEnumeration printQuality = null;
    private RoboxProfile settings = null;
    private Printer printerToUse = null;
    private String macroToExecuteAfterPurge = null;

    @FXML
    private VBox container;

    @FXML
    private Button startPurgeButton;

    @FXML
    private Text purgeStatus;

    @FXML
    private Text instruction;

    @FXML
    private RestrictedNumberField purgeTemperature;

    @FXML
    private Text currentMaterialTemperature;

    @FXML
    private GridPane purgeDetailsGrid;

    @FXML
    private Button cancelPurgeButton;

    @FXML
    private Button proceedButton;

    @FXML
    private Button okButton;

    @FXML
    private Text lastMaterialTemperature;

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
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    Dialogs.create()
                        .owner(null)
                        .title(DisplayManager.getLanguageBundle().getString("dialogs.clearBedTitle"))
                        .masthead(null)
                        .message(DisplayManager.getLanguageBundle().getString("dialogs.clearBedInstruction"))
                        .showWarning();
                    printerToUse.printProject(project, filament, printQuality, settings);
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
                        .title(DisplayManager.getLanguageBundle().getString("dialogs.clearBedTitle"))
                        .masthead(null)
                        .message(DisplayManager.getLanguageBundle().getString("dialogs.clearBedInstruction"))
                        .showWarning();
                    printerToUse.getPrintQueue().printGCodeFile(GCodeMacros.getFilename(macroToExecuteAfterPurge), true);
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

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        i18nBundle = DisplayManager.getLanguageBundle();

        StatusScreenState statusScreenState = StatusScreenState.getInstance();

        purgeHelper.addStateListener(this);
        purgeHelper.setState(PurgeState.IDLE);
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
                proceedButton.setVisible(false);
                okButton.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                break;
            case INITIALISING:
                startPurgeButton.setVisible(false);
                cancelPurgeButton.setVisible(true);
                proceedButton.setVisible(false);
                okButton.setVisible(false);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                break;
            case CONFIRM_TEMPERATURE:
                startPurgeButton.setVisible(false);
                cancelPurgeButton.setVisible(true);
                proceedButton.setVisible(true);
                okButton.setVisible(false);
                lastMaterialTemperature.setText(String.valueOf(purgeHelper.getLastMaterialTemperature()));
                currentMaterialTemperature.setText(String.valueOf(purgeHelper.getCurrentMaterialTemperature()));
                purgeTemperature.setText(String.valueOf(purgeHelper.getPurgeTemperature()));
                purgeDetailsGrid.setVisible(true);
                purgeStatus.setText(state.getStepTitle());
                break;
            case HEATING:
                startPurgeButton.setVisible(false);
                cancelPurgeButton.setVisible(true);
                proceedButton.setVisible(false);
                okButton.setVisible(false);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                break;
            case RUNNING_PURGE:
                startPurgeButton.setVisible(false);
                cancelPurgeButton.setVisible(true);
                proceedButton.setVisible(false);
                okButton.setVisible(false);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                break;
            case FINISHED:
                startPurgeButton.setVisible(false);
                cancelPurgeButton.setVisible(false);
                proceedButton.setVisible(false);
                okButton.setVisible(true);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                break;
            case FAILED:
                startPurgeButton.setVisible(true);
                cancelPurgeButton.setVisible(true);
                proceedButton.setVisible(false);
                okButton.setVisible(false);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                break;
        }
    }

    public void purgeAndPrint(Project project, Filament filament, PrintQualityEnumeration printQuality, RoboxProfile settings, Printer printerToUse)
    {
        this.project = project;
        this.filament = filament;
        this.printQuality = printQuality;
        this.settings = settings;
        this.printerToUse = printerToUse;

        purgeHelper.setPrinterToUse(printerToUse);

        ApplicationStatus.getInstance().setMode(ApplicationMode.PURGE);
    }

    public void purgeAndRunMacro(String macroName, Printer printerToUse)
    {
        this.macroToExecuteAfterPurge = macroName;
        this.printerToUse = printerToUse;

        purgeHelper.setPrinterToUse(printerToUse);

        ApplicationStatus.getInstance().setMode(ApplicationMode.PURGE);
    }

    public void purge(Printer printerToUse)
    {
        this.printerToUse = printerToUse;

        purgeHelper.setPrinterToUse(printerToUse);

        ApplicationStatus.getInstance().setMode(ApplicationMode.PURGE);
    }
}
