package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.LargeProgress;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.components.buttons.GraphicButtonWithLabel;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.services.purge.PurgeState;
import celtech.services.slicer.PrintQualityEnumeration;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
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

    private PurgeHelper purgeHelper;

    private Project project = null;
    private Printer printerToUse = null;
    private String macroToExecuteAfterPurge = null;
    private double printPercent;
    private Bounds diagramBounds;
    private Pane diagramNode;
    private ResourceBundle resources;

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
//            final Project projectCopy = project;

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
                    // Need to go to settings page for this project
                    ApplicationStatus.getInstance().setMode(ApplicationMode.SETTINGS);
//                    printerToUse.printProject(projectCopy, filament, printQuality, settings);
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
        this.resources = resources;

        purgeProgressBar.setTargetLegend("");
        purgeProgressBar.setProgressDescription(Lookup.i18n("calibrationPanel.printingCaps"));
        purgeProgressBar.setTargetValue("");

        startPurgeButton.installTag();
        proceedButton.installTag();

        loadDiagram();
        resizeDiagram();
        addDiagramMoveScaleListeners();

    }

    @Override
    public void setState(PurgeState state)
    {
        switch (state)
        {
            case IDLE:
                startPurgeButton.setVisible(true);
                backButton.setVisible(false);
                cancelPurgeButton.setVisible(true);
                purgeDetailsGrid.setVisible(false);
                purgeProgressBar.setVisible(false);
                proceedButton.setVisible(false);
                repeatButton.setVisible(false);
                okButton.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                purgeTemperature.intValueProperty().removeListener(purgeTempEntryListener);
                diagramContainer.setVisible(false);
                break;
            case INITIALISING:
                startPurgeButton.setVisible(false);
                backButton.setVisible(false);
                cancelPurgeButton.setVisible(true);
                proceedButton.setVisible(false);
                purgeProgressBar.setVisible(false);
                repeatButton.setVisible(false);
                okButton.setVisible(false);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                diagramContainer.setVisible(false);
                break;
            case CONFIRM_TEMPERATURE:
                startPurgeButton.setVisible(false);
                backButton.setVisible(false);
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
                diagramContainer.setVisible(false);
                break;
            case HEATING:
                startPurgeButton.setVisible(false);
                backButton.setVisible(false);
                cancelPurgeButton.setVisible(true);
                proceedButton.setVisible(false);
                repeatButton.setVisible(false);
                purgeProgressBar.setVisible(false);
                okButton.setVisible(false);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                purgeTemperature.intValueProperty().removeListener(purgeTempEntryListener);
                diagramContainer.setVisible(false);
                break;
            case RUNNING_PURGE:
                startPurgeButton.setVisible(false);
                backButton.setVisible(false);
                cancelPurgeButton.setVisible(true);
                proceedButton.setVisible(false);
                repeatButton.setVisible(false);
                purgeProgressBar.setVisible(true);
                okButton.setVisible(false);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                diagramContainer.setVisible(true);
                break;
            case FINISHED:
                startPurgeButton.setVisible(false);
                backButton.setVisible(false);
                cancelPurgeButton.setVisible(false);
                proceedButton.setVisible(false);
                repeatButton.setVisible(true);
                purgeProgressBar.setVisible(false);
                okButton.setVisible(true);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                purgeTemperature.intValueProperty().removeListener(purgeTempEntryListener);
                diagramContainer.setVisible(true);
                break;
            case FAILED:
                startPurgeButton.setVisible(false);
                backButton.setVisible(true);
                cancelPurgeButton.setVisible(false);
                proceedButton.setVisible(false);
                repeatButton.setVisible(false);
                purgeProgressBar.setVisible(false);
                okButton.setVisible(false);
                purgeDetailsGrid.setVisible(false);
                purgeStatus.setText(state.getStepTitle());
                purgeTemperature.intValueProperty().removeListener(purgeTempEntryListener);
                diagramContainer.setVisible(false);
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
        bindPrinter(printerToUse);

        ApplicationStatus.getInstance().setMode(ApplicationMode.PURGE);
    }

    private void bindPrinter(Printer printerToUse)
    {
        if (this.printerToUse != null)
        {
            removePrintProgressListeners(this.printerToUse);
            startPurgeButton.getTag().removeAllConditionalText();
            proceedButton.getTag().removeAllConditionalText();
        }
        this.printerToUse = printerToUse;
        purgeHelper = new PurgeHelper(printerToUse);
        purgeHelper.addStateListener(this);
        purgeHelper.setState(PurgeState.IDLE);
        setupPrintProgressListeners(printerToUse);

        installTag(printerToUse, startPurgeButton);
        installTag(printerToUse, proceedButton);
    }

    private void installTag(Printer printerToUse, GraphicButtonWithLabel button)
    {
        button.getTag().addConditionalText("dialogs.cantPurgeDoorIsOpenMessage",
                                           printerToUse.getPrinterAncillarySystems().
                                           lidOpenProperty().not().not());
        button.getTag().addConditionalText("dialogs.cantPrintNoFilamentMessage",
                                           printerToUse.extrudersProperty().get(0).
                                           filamentLoadedProperty().not());

        button.disableProperty().bind(printerToUse.canPrintProperty().not()
            .or(printerToUse.getPrinterAncillarySystems().lidOpenProperty())
            .or(printerToUse.extrudersProperty().get(0).filamentLoadedProperty().not()));
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

    private Bounds getBoundsOfNotYetDisplayedNode(Pane loadedDiagramNode)
    {
        Group group = new Group(loadedDiagramNode);
        Scene scene = new Scene(group);
        scene.getStylesheets().add(ApplicationConfiguration.getMainCSSFile());
        group.applyCss();
        group.layout();
        Bounds bounds = loadedDiagramNode.getLayoutBounds();
        return bounds;
    }

    private void addDiagramMoveScaleListeners()
    {

        diagramContainer.widthProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                resizeDiagram();
            });

        diagramContainer.heightProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                resizeDiagram();
            });

    }

    private void loadDiagram()
    {
        URL fxmlFileName = getClass().getResource(
            ApplicationConfiguration.fxmlDiagramsResourcePath + "purge/purge_simplified.fxml");
        try
        {
            FXMLLoader loader = new FXMLLoader(fxmlFileName, resources);
            diagramNode = loader.load();
            diagramBounds = getBoundsOfNotYetDisplayedNode(diagramNode);
            diagramContainer.getChildren().clear();
            diagramContainer.getChildren().add(diagramNode);

        } catch (IOException ex)
        {
            ex.printStackTrace();
            steno.error("Could not load diagram: " + fxmlFileName);
        }
    }

    private void resizeDiagram()
    {
        double diagramWidth = diagramBounds.getWidth();
        double diagramHeight = diagramBounds.getHeight();

        double availableWidth = diagramContainer.getWidth();
        double availableHeight = diagramContainer.getHeight();

        double requiredScaleHeight = availableHeight / diagramHeight * 0.95;
        double requiredScaleWidth = availableWidth / diagramWidth * 0.95;
        double requiredScale = Math.min(requiredScaleHeight, requiredScaleWidth);
//        requiredScale = Math.min(requiredScale, 1.3d);
        steno.debug("Scale is " + requiredScale);
        diagramNode.setScaleX(requiredScale);
        diagramNode.setScaleY(requiredScale);

        diagramNode.setPrefWidth(0);
        diagramNode.setPrefHeight(0);

        double scaledDiagramWidth = diagramWidth * requiredScale;

        double xTranslate = 0;
        double yTranslate = 0;
//        
        xTranslate += availableWidth / 2.0 - diagramWidth / 2.0;
        yTranslate -= availableHeight;

        diagramNode.setTranslateX(xTranslate);
        diagramNode.setTranslateY(yTranslate);

    }

}
