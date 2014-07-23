/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.panels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.ProjectMode;
import celtech.configuration.EEPROMState;
import celtech.configuration.WhyAreWeWaitingState;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.ErrorHandler;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.coreUI.visualisation.SelectionContainer;
import celtech.printerControl.Printer;
import celtech.printerControl.PrinterStatusEnumeration;
import celtech.utils.PrinterUtils;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class LayoutStatusMenuStripController
{
    
    private Stenographer steno = StenographerFactory.getStenographer(LayoutStatusMenuStripController.class.getName());
    private SettingsScreenState settingsScreenState = null;
    private ApplicationStatus applicationStatus = null;
    private DisplayManager displayManager = null;
    private Project boundProject = null;
    private PrinterUtils printerUtils = null;
    
    private ErrorHandler errorHandler = ErrorHandler.getInstance();
    
    @FXML
    private ResourceBundle resources;
    
    @FXML
    private URL location;
    
    @FXML
    private Button backwardButton;
    
    @FXML
    private Button forwardButton;
    
    @FXML
    private Button printButton;
    
    @FXML
    private HBox layoutButtonHBox;
    
    @FXML
    private Button addModelButton;
    
    @FXML
    private Button deleteModelButton;
    
    @FXML
    private Button duplicateModelButton;
    
    @FXML
    private Button distributeModelsButton;
    
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
        
        boolean purgeConsent = printerUtils.offerPurgeIfNecessary(printer);
        applicationStatus.setMode(ApplicationMode.STATUS);
        
        if (purgeConsent)
        {
            PrinterUtils.runPurge(currentProject, settingsScreenState.getFilament(), settingsScreenState.getPrintQuality(), settingsScreenState.getSettings(), printer);
        } else
        {
            printer.printProject(currentProject, settingsScreenState.getFilament(), settingsScreenState.getPrintQuality(), settingsScreenState.getSettings());
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
    void addModel(ActionEvent event)
    {
        applicationStatus.setMode(ApplicationMode.ADD_MODEL);
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
    
    private Printer currentPrinter = null;
    private BooleanProperty printerOKToPrint = new SimpleBooleanProperty(false);

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
        
        backwardButton.visibleProperty().bind(applicationStatus.modeProperty().isNotEqualTo(ApplicationMode.STATUS));
//        forwardButton.visibleProperty().bind(applicationStatus.modeProperty().isNotEqualTo(ApplicationMode.SETTINGS).and(printerOKToPrint));
        printButton.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS).and(printerOKToPrint));
        
        settingsScreenState.selectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {
            @Override
            public void changed(ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue)
            {
                if (newValue != null)
                {
                    if (currentPrinter != null)
                    {
                        printerOKToPrint.unbind();
                        printerOKToPrint.set(false);
                    }
                    printerOKToPrint.bind(newValue.printerStatusProperty().isEqualTo(PrinterStatusEnumeration.IDLE)
                        .and(newValue.whyAreWeWaitingProperty().isEqualTo(WhyAreWeWaitingState.NOT_WAITING))
                        .and(newValue.headEEPROMStatusProperty().isEqualTo(EEPROMState.PROGRAMMED))
                        .and((newValue.Filament1LoadedProperty().or(newValue.Filament2LoadedProperty())))
                        .and(settingsScreenState.filamentProperty().isNotNull().or(newValue.loadedFilamentProperty().isNotNull())));
                    currentPrinter = newValue;
                }
            }
        });
        
        layoutButtonHBox.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(ApplicationMode.LAYOUT));
    }

    /**
     * Binds button disabled properties to the selection container This disables and enables buttons depending on whether a model is selected
     *
     * @param selectionContainer The selection container associated with the currently displayed project.
     */
    public void bindSelectedModels(SelectionContainer selectionContainer)
    {
        deleteModelButton.disableProperty().unbind();
        duplicateModelButton.disableProperty().unbind();
//        snapToGroundButton.disableProperty().unbind();
        distributeModelsButton.disableProperty().unbind();
        
        deleteModelButton.disableProperty().bind(Bindings.isEmpty(selectionContainer.selectedModelsProperty()));
        duplicateModelButton.disableProperty().bind(Bindings.isEmpty(selectionContainer.selectedModelsProperty()));
//        snapToGroundButton.setDisable(true);
        distributeModelsButton.setDisable(true);
//        snapToGroundButton.disableProperty().bind(Bindings.isEmpty(selectionContainer.selectedModelsProperty()));

//        if (boundViewManager != null)
//        {
//            boundViewManager.layoutSubmodeProperty().removeListener(layoutChangeListener);
//        }
//
//        boundViewManager = displayManager.getCurrentlyVisibleViewManager();
//        boundViewManager.layoutSubmodeProperty().addListener(layoutChangeListener);
        if (boundProject != null)
        {
            addModelButton.disableProperty().unbind();
        }
        
        boundProject = displayManager.getCurrentlyVisibleProject();
        addModelButton.disableProperty().bind(Bindings.isNotEmpty(boundProject.getLoadedModels()).and(boundProject.projectModeProperty().isEqualTo(ProjectMode.GCODE)));
        
        distributeModelsButton.disableProperty().bind(Bindings.isEmpty(boundProject.getLoadedModels()));
        
        forwardButton.visibleProperty().unbind();
        forwardButton.visibleProperty().bind((applicationStatus.modeProperty().isEqualTo(ApplicationMode.LAYOUT).and(Bindings.isNotEmpty(boundProject.getLoadedModels())).or(applicationStatus.
            modeProperty().isEqualTo(ApplicationMode.STATUS))));
    }
}
