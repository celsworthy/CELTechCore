package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.ModelContainerProject;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.StandardColours;
import celtech.coreUI.components.buttons.GraphicToggleButtonWithLabel;
import celtech.roboxbase.ApplicationFeature;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.datafileaccessors.FilamentContainer;
import celtech.roboxbase.printerControl.model.Head;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.services.gcodegenerator.GCodeGeneratorResult;
import celtech.roboxbase.services.slicer.PrintQualityEnumeration;
import celtech.services.gcodepreview.GCodePreviewExecutorService;
import celtech.services.gcodepreview.GCodePreviewTask;
import java.util.Optional;
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

    private DisplayManager displayManager = null;
    private Project currentProject = null;
    private GCodePreviewExecutorService buttonExecutor = new GCodePreviewExecutorService();
    private GCodePreviewExecutorService updateExecutor = new GCodePreviewExecutorService();
    private GCodePreviewExecutorService previewExecutor = new GCodePreviewExecutorService();
    private final GraphicToggleButtonWithLabel previewButton;
    private GCodePreviewTask previewTask = null;
    
    private final ChangeListener<Boolean> previewRunningListener =(ObservableValue<? extends Boolean> observable, Boolean wasRunning, Boolean isRunning) -> {
        if (wasRunning && !isRunning) {
            removePreview();
        }
    };
    
    private final ChangeListener<Boolean> gCodePrepChangeListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
        updatePreview();
    };

    private final ChangeListener<PrintQualityEnumeration> printQualityChangeListener = (ObservableValue<? extends PrintQualityEnumeration> observable, PrintQualityEnumeration oldValue, PrintQualityEnumeration newValue) -> {
        updatePreview();
    };

    private final ChangeListener<ApplicationMode> applicationModeChangeListener = new ChangeListener<ApplicationMode>()
    {
        @Override
        public void changed(ObservableValue<? extends ApplicationMode> observable, ApplicationMode oldValue, ApplicationMode newValue)
        {
            if (newValue == ApplicationMode.SETTINGS)
            {
                if (Lookup.getUserPreferences().isAutoGCodePreview() && BaseConfiguration.isApplicationFeatureEnabled(ApplicationFeature.GCODE_VISUALISATION))
                {
                    BaseLookup.getTaskExecutor().runOnGUIThread(() ->
                    {
                        previewButton.selectedProperty().set(true);
                        updatePreview();
                    });
                }
            }
            else
            {
                removePreview();
            }
        }
    };
    
    public PreviewManager(GraphicToggleButtonWithLabel previewButton,
                          DisplayManager displayManager)
    {
        this.previewButton = previewButton;
        this.displayManager = displayManager;
        try
        {
            ApplicationStatus.getInstance().modeProperty().addListener(applicationModeChangeListener);
            previewButton.selectedProperty().set(false);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void previewPressed(ActionEvent event)
    {
        if(BaseConfiguration.isApplicationFeatureEnabled(ApplicationFeature.GCODE_VISUALISATION)) {
            if (previewButton.selectedProperty().get())
                updatePreview();
            else
                removePreview();
        }
        else {
            BaseLookup.getSystemNotificationHandler().showPurchaseLicenseDialog();
            previewButton.selectedProperty().set(false);
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
            }
            else
            {
                removePreview();
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
   
    private void removePreview()
    {
        if (previewTask != null)
        {
            previewTask.runningProperty().removeListener(previewRunningListener);
            previewTask.terminatePreview();
        }
        previewTask = null;

        BaseLookup.getTaskExecutor().runOnGUIThread(() ->
        {
            previewButton.selectedProperty().set(false);
        });
    }
    
    private void updatePreview()
    {
        steno.info("Updating preview");
        updatePreviewButtonIcon();
        
        boolean showPreview = previewButton.selectedProperty().get();
        boolean modelUnsuitable = !modelIsSuitable();
        steno.info("showPreview = " + Boolean.toString(showPreview) + ", modelUnsuitable = " + Boolean.toString(modelUnsuitable));
        if (modelUnsuitable || !showPreview)
        {
            BaseLookup.getTaskExecutor().runOnGUIThread(() ->
            {
                steno.info("Setting preview button disabled state to " + Boolean.toString(modelUnsuitable));
                // Disable preview button.
                previewButton.disableProperty().set(modelUnsuitable);
            });
            removePreview();
        }
        else
        {
            Runnable doUpdatePreview = () ->
            {
                // Showing preview preview button.
                steno.info("Showing preview");
                BaseLookup.getTaskExecutor().runOnGUIThread(() ->
                {
                    // Enable preview button.
                    steno.info("Enabling preview button from doUpdatePreview");
                    previewButton.disableProperty().set(false);
                    previewButton.selectedProperty().set(true);
                });
                if (previewTask != null)
                {
                    steno.info("Clearing preview");
                    previewTask.clearGCode();
                }
                ModelContainerProject mProject = (ModelContainerProject)currentProject;
                steno.info("Waiting for prep result");
                Optional<GCodeGeneratorResult> resultOpt = mProject.getGCodeGenManager().getPrepResult(currentProject.getPrintQuality());
                steno.info("Got prep result - ifPresent() = " + Boolean.toString(resultOpt.isPresent()) + "isSuccess() = " + (resultOpt.isPresent() ? Boolean.toString(resultOpt.get().isSuccess()) : "---"));
                if (resultOpt.isPresent() && resultOpt.get().isSuccess())
                {
                    steno.info("GCodePrepResult = " + resultOpt.get().getPostProcOutputFileName());

                    // Get tool colours.
                    Color t0Colour = StandardColours.ROBOX_BLUE;
                    Color t1Colour = StandardColours.HIGHLIGHT_ORANGE;
                    String printerType = "DEFAULT";
                    Printer printer = Lookup.getSelectedPrinterProperty().get();
                    if (printer != null)
                    {
                        printerType = printer.printerConfigurationProperty().get().getTypeCode();
                        Head head = printer.headProperty().get();
                        if (head != null)
                        {
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

                    BaseLookup.getTaskExecutor().runOnGUIThread(() ->
                    {
                        steno.info("Setting preview button selected");
                        previewButton.selectedProperty().set(true);
                    });

                    if (previewTask == null)
                    {
                        String projDirectory = ApplicationConfiguration.getProjectDirectory()
                                                   + currentProject.getProjectName(); 
                        steno.info("Starting preview task");
                        previewTask = new GCodePreviewTask(projDirectory, printerType, displayManager.getNormalisedPreviewRectangle());
                        previewTask.runningProperty().addListener(previewRunningListener);
                        previewExecutor.runTask(previewTask);
                    }
                    else
                    {
                        previewTask.setPrinterType(printerType);
                    }
                    steno.info("Loading GCode file = " + resultOpt.get().getPostProcOutputFileName());
                    previewTask.setToolColour(0, t0Colour);
                    previewTask.setToolColour(1, t1Colour);
                    previewTask.loadGCodeFile(resultOpt.get().getPostProcOutputFileName());
                }
            };

            steno.info("Cancelling update tasks");
            updateExecutor.cancelTask();
            steno.info("Running update tasks");
            updateExecutor.runTask(doUpdatePreview);
        }
    }

    private void updatePreviewButtonIcon()
    {
        Runnable updatePreviewButtonIcon = () ->
        {
            steno.info("Updating preview button state");
            BaseLookup.getTaskExecutor().runOnGUIThread(() ->
            {
                steno.info("Setting button state to Waiting");
                previewButton.setFxmlFileName("waitPreviewButton");
            });
            ModelContainerProject mProject = (ModelContainerProject)currentProject;
            Optional<GCodeGeneratorResult> resultOpt = mProject.getGCodeGenManager().getPrepResult(currentProject.getPrintQuality());
            if (resultOpt.isPresent() && resultOpt.get().isSuccess())
            {
                BaseLookup.getTaskExecutor().runOnGUIThread(() ->
                {
                    steno.info("Setting button state to ready");
                    previewButton.setFxmlFileName("previewButton");
                });
            }
            else
            {
                BaseLookup.getTaskExecutor().runOnGUIThread(() ->
                {
                    steno.info("Setting button state to failed");
                    previewButton.setFxmlFileName("noPreviewButton");
                });
            }
        };

        steno.info("Cancelling button executor");
        buttonExecutor.cancelTask();
        steno.info("Running button executor");
        buttonExecutor.runTask(updatePreviewButtonIcon);
    }
}
