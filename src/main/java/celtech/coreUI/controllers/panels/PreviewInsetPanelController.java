package celtech.coreUI.controllers.panels;

import celtech.coreUI.gcodepreview.GCodePreviewSlicer;
import celtech.coreUI.gcodepreview.GCodePreviewManager;
import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.ModelContainerProject;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.coreUI.gcodepreview.model.GCodeModel;
import celtech.coreUI.gcodepreview.representation.PreviewContainer;
import celtech.coreUI.visualisation.Xform;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.RoboxProfile;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.printerControl.model.PrinterListChangesAdapter;
import celtech.roboxbase.printerControl.model.PrinterListChangesListener;
import celtech.roboxbase.services.gcodepreview.GCodePreviewTask;
import celtech.roboxbase.services.postProcessor.GCodePostProcessingResult;
import celtech.roboxbase.utils.tasks.Cancellable;
import celtech.roboxbase.utils.tasks.SimpleCancellable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.transform.Rotate;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.io.FileUtils;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PreviewInsetPanelController implements Initializable, ProjectAwareController, ModelContainerProject.ProjectChangesListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            PreviewInsetPanelController.class.getName());

    @FXML
    private HBox previewInsetRoot;
    
    @FXML
    private HBox showPreviewHBox;

    @FXML
    private CheckBox showPreviewButton;

    @FXML
    private Slider topVisibleLayerSlider;

    @FXML
    private CheckBox movesVisibleButton;
    
    private Printer currentPrinter = null;
    private String currentHeadTypeCode = null;
    private String previewFile = "";
    private Project currentProject = null;
    private GCodePreviewTask previewTask = null;
    
    private final ChangeListener<Printer> selectedPrinterChangeListener = new ChangeListener<Printer>()
    {
        @Override
        public void changed(ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue)
        {
            whenPrinterChanged(newValue);
        }
    };

    private final ChangeListener<ApplicationMode> applicationModeChangeListener = new ChangeListener<ApplicationMode>()
    {
        @Override
        public void changed(ObservableValue<? extends ApplicationMode> observable, ApplicationMode oldValue, ApplicationMode newValue)
        {
            if (newValue == ApplicationMode.SETTINGS)
            {
                previewInsetRoot.setVisible(true);
                previewInsetRoot.setMouseTransparent(false);
                showPreviewButton.selectedProperty().set(false);
            } else
            {
                previewInsetRoot.setVisible(false);
                previewInsetRoot.setMouseTransparent(true);
                hidePreview();
            }
        }
    };

    private final PrinterListChangesListener printerListChangesListener = new PrinterListChangesAdapter()
    {
        @Override
        public void whenHeadAdded(Printer printer)
        {
            if (printer == currentPrinter)
            {
                whenPrinterChanged(printer);
            }
        }

        @Override
        public void whenExtruderAdded(Printer printer, int extruderIndex)
        {
            if (printer == currentPrinter)
            {
                whenPrinterChanged(printer);
            }
        }

    };

    private final MapChangeListener<Integer, Filament> filamentListener = new MapChangeListener<Integer, Filament>()
    {
        @Override
        public void onChanged(MapChangeListener.Change<? extends Integer, ? extends Filament> change)
        {
            updatePreview();
        }
    };
    
    private final ChangeListener<Number> layerCountChangeListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        double value = newValue.intValue();
        topVisibleLayerSlider.setMax(value);
        topVisibleLayerSlider.setValue(value);
    };

    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try
        {
            Lookup.getSelectedPrinterProperty().addListener(selectedPrinterChangeListener);
            ApplicationStatus.getInstance().modeProperty().addListener(applicationModeChangeListener);
            //whenPrinterChanged(Lookup.getSelectedPrinterProperty().get());
            BaseLookup.getPrinterListChangesNotifier().addListener(printerListChangesListener);
            topVisibleLayerSlider.disableProperty().set(true);
            topVisibleLayerSlider.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                if (currentProject != null && currentProject instanceof ModelContainerProject)
                {
                    PreviewContainer pvc = (PreviewContainer)((ModelContainerProject)currentProject).getPreviewContainerProperty().getValue();
                    if (pvc != null)
                        pvc.setTopVisibleLayerIndex(newValue.intValue());
                }
            });
            
            movesVisibleButton.selectedProperty().addListener((ob, ov, nv) -> {
                if (currentProject != null && currentProject instanceof ModelContainerProject)
                {
                    PreviewContainer pvc = (PreviewContainer)((ModelContainerProject)currentProject).getPreviewContainerProperty().getValue();
                    if (pvc != null)
                        pvc.setMovesVisibility(nv);
                }
            });
            movesVisibleButton.disableProperty().bind(showPreviewButton.selectedProperty().not());

            showPreviewButton.selectedProperty().set(false);
            showPreviewButton.selectedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean selected) ->
                {
                    if (selected)
                    {
                        updatePreview();
                    }
                    else
                    {
                        removePreview();
                    }
                });
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void whenPrinterChanged(Printer printer)
    {
        if (currentPrinter != null)
        {
            currentPrinter.effectiveFilamentsProperty().removeListener(filamentListener);
        }

        currentPrinter = printer;
        if (printer != null)
        {
            String headTypeCode;
            if (printer.headProperty().get() != null)
            {
                headTypeCode = printer.headProperty().get().typeCodeProperty().get();
            } else
            {
                headTypeCode = HeadContainer.defaultHeadID;
            }
            if (!headTypeCode.equals(currentHeadTypeCode))
            {
                if (currentProject != null)
                {
                    currentProject.invalidate();
                }
            }
            currentHeadTypeCode = headTypeCode;
        }
        hidePreview();
    }

    @Override
    public void setProject(Project project)
    {
        if (currentProject != null)
        {
            currentProject.removeProjectChangesListener(this);
        }

        if (project != null)
        {
            project.addProjectChangesListener(this);
            whenProjectChanged(project);
        }
    }

    private void whenProjectChanged(Project project)
    {
        currentProject = project;
        hidePreview();
    }

    @Override
    public void whenModelAdded(ProjectifiableThing modelContainer)
    {
        hidePreview();
     }

    @Override
    public void whenModelsRemoved(Set<ProjectifiableThing> modelContainers)
    {
        hidePreview();
    }

    @Override
    public void whenAutoLaidOut()
    {
        hidePreview();
    }

    @Override
    public void whenModelsTransformed(Set<ProjectifiableThing> modelContainers)
    {
        hidePreview();
    }

    @Override
    public void whenModelChanged(ProjectifiableThing modelContainer, String propertyName)
    {
        hidePreview();
    }

    @Override
    public void whenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings)
    {
        hidePreview();
    }

    @Override
    public void shutdownController()
    {
        hidePreview();

        if (currentPrinter != null)
        {
            currentPrinter.effectiveFilamentsProperty().removeListener(filamentListener);
        }

        if (currentProject != null)
        {
            currentProject.removeProjectChangesListener(this);
        }
        currentProject = null;

        Lookup.getSelectedPrinterProperty().removeListener(selectedPrinterChangeListener);
        ApplicationStatus.getInstance().modeProperty().removeListener(applicationModeChangeListener);
        BaseLookup.getPrinterListChangesNotifier().removeListener(printerListChangesListener);
    }
    
    private boolean modelIsSuitable()
    {
        if (currentProject != null && currentProject instanceof ModelContainerProject)
        {
            RoboxProfile slicerParameters = null; // This is sometimes returned as null. Not sure why.
            if (currentProject.getNumberOfProjectifiableElements() > 0)
            {
                String headType = HeadContainer.defaultHeadID;
                if (currentPrinter != null && currentPrinter.headProperty().get() != null)
                {
                    headType = currentPrinter.headProperty().get().typeCodeProperty().get();
                }
                slicerParameters = currentProject.getPrinterSettings().getSettings(headType, Lookup.getUserPreferences().getSlicerType());
            }
            if (slicerParameters != null)
            {
                //NOTE - this needs to change if raft settings in slicermapping.dat is changed
                
                // Needed as heads differ in size and will need to adjust print volume for this
                final float zReduction = currentPrinter.headProperty().get().getZReductionProperty().get();
            
                //NOTE - this needs to change if raft settings in slicermapping.dat is changed
                double raftOffset = slicerParameters.getSpecificFloatSetting("raftBaseThickness_mm")
                    //Raft interface thickness
                    + 0.28
                    //Raft surface layer thickness * surface layers
                    + 0.27 * slicerParameters.getSpecificIntSetting("interfaceLayers")
                    + slicerParameters.getSpecificFloatSetting("raftAirGapLayer0_mm")
                    + zReduction;

                for (ProjectifiableThing projectifiableThing : currentProject.getTopLevelThings())
                {
                    if (projectifiableThing instanceof ModelContainer)
                    {
                        ModelContainer modelContainer = (ModelContainer) projectifiableThing;

                        //TODO use settings derived offset values for spiral
                        if (modelContainer.isOffBedProperty().get()
                                || (currentProject.getPrinterSettings().getRaftOverride()
                                && modelContainer.isModelTooHighWithOffset(raftOffset))
                                || (currentProject.getPrinterSettings().getSpiralPrintOverride()
                                && modelContainer.isModelTooHighWithOffset(0.5)))
                        {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        
        return false;
    }
        
    public void clearPrintJobDirectories()
    {
        try
        {
            String filePath = BaseConfiguration.getApplicationStorageDirectory()
                    + ApplicationConfiguration.previewGCodeFileSubpath;
            File folder = new File(filePath);
            for (final File fileEntry : folder.listFiles())
            {
                if (fileEntry.isDirectory())
                {
                    try
                    {
                        FileUtils.deleteDirectory(fileEntry);
                    } catch (IOException ex)
                    {
                    }
                }
            }
        } catch (Exception ex)
        {
        }
    }
    
    private void hidePreview()
    {
        showPreviewButton.selectedProperty().set(false);
        //removePreview();
    }
    
    private void removePreview()
    {
        if (currentProject != null && currentProject instanceof ModelContainerProject)
        {
            ModelContainerProject mProject = (ModelContainerProject)currentProject;
            
            GCodePreviewManager.getInstance().cancelRunningTasks();
            mProject.getPreviewContainerProperty().setValue(null);
            mProject.getAllModels()
                    .stream()
                    .filter(m -> !(m instanceof ModelGroup))
                    .map(ModelContainer.class::cast)
                    .forEach(mc -> mc.setVisible(true));
            topVisibleLayerSlider.disableProperty().set(true);
        }
    }
    
    private void updatePreview()
    {
        boolean showPreview = showPreviewButton.selectedProperty().get();
        if (!modelIsSuitable() || !showPreview)
        {
            removePreview();
        }
        else
        {
            Cancellable cancellable = new SimpleCancellable();        
            Runnable runUpdatePreview = () ->
            {
                if (cancellable.cancelled().get())
                {
                    return;
                }

                GCodePreviewSlicer gcodePreviewGetter = new GCodePreviewSlicer((ModelContainerProject) currentProject, cancellable);

                try
                {
                    Optional<GCodePostProcessingResult> result = gcodePreviewGetter.runSlicerAndPostProcessor();
                    if (result.isPresent())
                    {   steno.info("GCodePostProcessingResult = " + result.get().getOutputFilename());


                        PreviewContainer previewContainer = new PreviewContainer(result.get().getOutputFilename());
                        previewContainer.render(movesVisibleButton.selectedProperty().get());
                        Platform.runLater(() -> {
                            currentProject.getAllModels()
                                          .stream()
                                          .filter(mc -> !(mc instanceof ModelGroup))
                                          .map(ModelContainer.class::cast)
                                          .forEach(m -> m.setVisible(false));
                            ModelContainerProject mProject = (ModelContainerProject)currentProject;
                            mProject.getPreviewContainerProperty().setValue(previewContainer);
                            topVisibleLayerSlider.setMax(previewContainer.getNumberOfLayers());
                            topVisibleLayerSlider.setValue(previewContainer.getNumberOfLayers());
                            topVisibleLayerSlider.disableProperty().set(false);
                        });
                    }
                    else
                        steno.info("GCodePostProcessingResult = NotPresent");

                } catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                steno.info("Finished post processing");
            };

            clearPrintJobDirectories();

            GCodePreviewManager.getInstance().cancelRunningAndRun(runUpdatePreview, cancellable);
        }
    }
}
