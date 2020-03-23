package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.ModelContainerProject;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.StandardColours;
import celtech.coreUI.components.buttons.GraphicButtonWithLabel;
import celtech.roboxbase.ApplicationFeature;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.datafileaccessors.FilamentContainer;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.printerControl.model.Head;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.services.gcodegenerator.GCodeGeneratorResult;
import celtech.roboxbase.services.slicer.PrintQualityEnumeration;
import celtech.services.gcodepreview.GCodePreviewExecutorService;
import celtech.services.gcodepreview.GCodePreviewTask;
import java.util.Optional;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.paint.Color;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PreviewManager
{
    private final Stenographer steno = StenographerFactory.getStenographer(PreviewManager.class.getName());

    private enum PreviewState
    {
        CLOSED,
        LOADING,
        OPEN,
        SLICE_UNAVAILABLE,
        NOT_SUPPORTED
    }
    
    private ObjectProperty<PreviewState> previewState = new SimpleObjectProperty<>(PreviewState.CLOSED);
    private DisplayManager displayManager = null;
    private Project currentProject = null;
    private GCodePreviewExecutorService buttonExecutor = new GCodePreviewExecutorService();
    private GCodePreviewExecutorService updateExecutor = new GCodePreviewExecutorService();
    private GCodePreviewExecutorService previewExecutor = new GCodePreviewExecutorService();
    private final GraphicButtonWithLabel previewButton;
    private GCodePreviewTask previewTask = null;
    
    private final ChangeListener<Boolean> previewRunningListener =(observable, wasRunning, isRunning) -> {
        if (wasRunning && !isRunning) {
            removePreview();
        }
    };
    
    private final ChangeListener<Boolean> gCodePrepChangeListener = (observable, oldValue, newValue) -> {
        autoStartAndUpdatePreview();
    };

    private final ChangeListener<PrintQualityEnumeration> printQualityChangeListener = (observable, oldValue, newValue) -> {
        autoStartAndUpdatePreview();
    };
    
    private final ChangeListener<ApplicationMode> applicationModeChangeListener = new ChangeListener<ApplicationMode>()
    {
        @Override
        public void changed(ObservableValue<? extends ApplicationMode> observable, ApplicationMode oldValue, ApplicationMode newValue)
        {
            if (newValue == ApplicationMode.SETTINGS)
            {
                autoStartAndUpdatePreview();
            }
        }
    };
    
    private final ChangeListener<PreviewState> previewStateChangeListener = (observable, oldState, newState) -> {
        BaseLookup.getTaskExecutor().runOnGUIThread(() ->
        {
            updatePreviewButton(newState);
        });
    };
    
    public PreviewManager(GraphicButtonWithLabel previewButton,
                          DisplayManager displayManager)
    {
        this.previewButton = previewButton;
        this.previewState.addListener(previewStateChangeListener);
        
        if(BaseConfiguration.isWindows32Bit())
        {
            previewState.set(PreviewState.NOT_SUPPORTED);
        }
        this.displayManager = displayManager;
        try
        {
            ApplicationStatus.getInstance().modeProperty().addListener(applicationModeChangeListener);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void previewPressed(ActionEvent event)
    {
        if(BaseConfiguration.isApplicationFeatureEnabled(ApplicationFeature.GCODE_VISUALISATION)) {
            if(previewState.get() != PreviewState.OPEN)
            {  
                updatePreview();
            }
        }
        else {
            BaseLookup.getSystemNotificationHandler().showPurchaseLicenseDialog();
        }
    }

    public void setProjectAndPrinter(Project project, Printer printer)
    {
        if (currentProject != project)
        {
            if (currentProject != null && currentProject instanceof ModelContainerProject)
            {
                ((ModelContainerProject)currentProject).getGCodeGenManager().getDataChangedProperty().removeListener(this.gCodePrepChangeListener);
                ((ModelContainerProject)currentProject).getGCodeGenManager().getPrintQualityProperty().removeListener(this.printQualityChangeListener);
            }

            currentProject = project;
            if (currentProject != null && currentProject instanceof ModelContainerProject)
            {
                ((ModelContainerProject)currentProject).getGCodeGenManager().getDataChangedProperty().addListener(this.gCodePrepChangeListener);
                ((ModelContainerProject)currentProject).getGCodeGenManager().getPrintQualityProperty().addListener(this.printQualityChangeListener);
                if (previewState.get() == PreviewState.OPEN ||
                    previewState.get() == PreviewState.LOADING)
                {
                    updatePreview();
                }
                else if (previewState.get() != PreviewState.NOT_SUPPORTED)
                {
                    previewState.set(PreviewState.CLOSED);
                }
            }
            else
            {
                clearPreview();
            }
        }
    }

    public void shutdown()
    {
        removePreview();

        if (currentProject != null && currentProject instanceof ModelContainerProject)
            ((ModelContainerProject)currentProject).getGCodeGenManager().getDataChangedProperty().removeListener(this.gCodePrepChangeListener);
        currentProject = null;

        ApplicationStatus.getInstance().modeProperty().removeListener(applicationModeChangeListener);
    }
    
    private boolean modelIsSuitable()
    {
        return (currentProject != null &&
                currentProject instanceof ModelContainerProject &&
                ((ModelContainerProject)currentProject).getGCodeGenManager().modelIsSuitable());
    }
   
    private void updatePreviewButton(PreviewState newState)
    {
        switch(newState)
        {
            case CLOSED:
            case OPEN:
                previewButton.setFxmlFileName("previewButton");
                previewButton.disableProperty().set(false);
                break;
            case NOT_SUPPORTED:
            case SLICE_UNAVAILABLE:
                previewButton.setFxmlFileName("previewButton");
                previewButton.disableProperty().set(true);
                break;
            case LOADING:
                previewButton.setFxmlFileName("previewLoadingButton");
                previewButton.disableProperty().set(true);
                break;
        }
    }
    
    private void clearPreview()
    {
        if (previewTask != null)
        {
            steno.debug("Clearing preview");
            previewTask.clearGCode();
        }
    }

    private void removePreview()
    {
        if (previewTask != null)
        {
            previewTask.runningProperty().removeListener(previewRunningListener);
            previewTask.terminatePreview();
            previewState.set(PreviewState.CLOSED);
        }
        previewTask = null;
    }
    
    private void startPreview() {
        if (previewTask == null)
        {
            String printerType = null;
            Printer printer = Lookup.getSelectedPrinterProperty().get();
            if (printer != null)
                printerType = printer.printerConfigurationProperty().get().getTypeCode();
            String projDirectory = ApplicationConfiguration.getProjectDirectory()
                                       + currentProject.getProjectName(); 
            steno.debug("Starting preview task");
            previewTask = new GCodePreviewTask(projDirectory, printerType, displayManager.getNormalisedPreviewRectangle());
            previewTask.runningProperty().addListener(previewRunningListener);
            previewExecutor.runTask(previewTask);
        }
    }

    private void autoStartAndUpdatePreview()
    {
            if (previewState.get() == PreviewState.OPEN ||
                    previewState.get() == PreviewState.LOADING ||
                (Lookup.getUserPreferences().isAutoGCodePreview() &&
                BaseConfiguration.isApplicationFeatureEnabled(ApplicationFeature.GCODE_VISUALISATION)))
        {
            updatePreview();
        }
    }

    private void updatePreview()
    {
        steno.debug("Updating preview");
        
        boolean modelUnsuitable = !modelIsSuitable();
        if (modelUnsuitable)
        {
            previewState.set(PreviewState.SLICE_UNAVAILABLE);
            clearPreview();
        }
        else
        {
            Runnable doUpdatePreview = () ->
            {
                // Showing preview preview button.
                steno.info("Showing preview");
                previewState.set(PreviewState.LOADING);

                if (previewTask == null)
                    startPreview();
                else
                    clearPreview();
                ModelContainerProject mProject = (ModelContainerProject)currentProject;
                steno.debug("Waiting for prep result");
                Optional<GCodeGeneratorResult> resultOpt = mProject.getGCodeGenManager().getPrepResult(currentProject.getPrintQuality());
                steno.debug("Got prep result - ifPresent() = " + Boolean.toString(resultOpt.isPresent()) + "isSuccess() = " + (resultOpt.isPresent() ? Boolean.toString(resultOpt.get().isSuccess()) : "---"));
                if (resultOpt.isPresent() && resultOpt.get().isSuccess())
                {
                    steno.debug("GCodePrepResult = " + resultOpt.get().getPostProcOutputFileName());

                    // Get tool colours.
                    Color t0Colour = StandardColours.ROBOX_BLUE;
                    Color t1Colour = StandardColours.HIGHLIGHT_ORANGE;
                    String printerType = null;
                    String headTypeCode = HeadContainer.defaultHeadID;
                    Printer printer = Lookup.getSelectedPrinterProperty().get();
                    if (printer != null)
                    {
                        printerType = printer.printerConfigurationProperty().get().getTypeCode();

                        Head head = printer.headProperty().get();
                        if (head != null)
                        {
                            headTypeCode = head.typeCodeProperty().get();
                            
                            // Assume we have at least one extruder.
                            Filament filamentInUse;
                            filamentInUse = printer.effectiveFilamentsProperty().get(0);
                            if (filamentInUse != null && filamentInUse != FilamentContainer.UNKNOWN_FILAMENT)
                            {
                                Color colour = filamentInUse.getDisplayColour();
                                if (colour != null)
                                    t0Colour = colour;
                            }
                            if (head.headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD)
                            {
                                t1Colour = t0Colour;
                                t0Colour = Color.ORANGE;
                                filamentInUse = printer.effectiveFilamentsProperty().get(1);
                                if (filamentInUse != null && filamentInUse != FilamentContainer.UNKNOWN_FILAMENT)
                                {
                                    Color colour = filamentInUse.getDisplayColour();
                                    if (colour != null)
                                        t0Colour = colour;
                                }
                            }
                            else
                                t1Colour = t0Colour;
                        }
                    }

                    if (previewTask == null)
                        startPreview();
                    else
                        previewTask.setPrinterType(printerType);
                    steno.debug("Loading GCode file = " + resultOpt.get().getPostProcOutputFileName());
                    previewTask.setToolColour(0, t0Colour);
                    previewTask.setToolColour(1, t1Colour);
                    previewTask.loadGCodeFile(resultOpt.get().getPostProcOutputFileName());
                    if (Lookup.getUserPreferences().isAutoGCodePreview())
                        previewTask.giveFocus();
                    
                    previewState.set(PreviewState.OPEN);
                }
                else
                {
                    // Failed.
                    steno.info("Setting button state to failed");
                    previewState.set(PreviewState.SLICE_UNAVAILABLE);
                }
            };

            steno.debug("Cancelling update tasks");
            updateExecutor.cancelTask();
            steno.debug("Running update tasks");
            updateExecutor.runTask(doUpdatePreview);
        }
    }
}
