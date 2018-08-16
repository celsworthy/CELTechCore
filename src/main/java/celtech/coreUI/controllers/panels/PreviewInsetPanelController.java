package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.ModelContainerProject;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.datafileaccessors.FilamentContainer;
import celtech.roboxbase.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import celtech.roboxbase.postprocessor.PrintJobStatistics;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.printerControl.model.PrinterListChangesAdapter;
import celtech.roboxbase.printerControl.model.PrinterListChangesListener;
import celtech.roboxbase.services.gcodepreview.GCodePreviewTask;
import celtech.roboxbase.services.postProcessor.GCodePostProcessingResult;
import celtech.roboxbase.services.postProcessor.PostProcessorTask;
import celtech.roboxbase.services.slicer.PrintQualityEnumeration;
import celtech.roboxbase.utils.models.MeshForProcessing;
import celtech.roboxbase.utils.models.PrintableMeshes;
import celtech.roboxbase.utils.tasks.Cancellable;
import celtech.roboxbase.utils.tasks.SimpleCancellable;
import celtech.roboxbase.utils.threed.CentreCalculations;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

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
    private Slider layerTopSlider;
    
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
                GCodePreviewThreadManager.getInstance().cancelRunningTasks();
                if (previewTask != null)
                    previewTask.terminatePreview();
                previewTask = null;
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
            showPreviewButton.selectedProperty().set(false);
            showPreviewButton.selectedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean selected) ->
                {
                    if (selected)
                    {
                        if (previewTask == null) {
                            previewTask = new GCodePreviewTask();
                            Thread t = new Thread(previewTask);
                            t.start();
                        }
                        updatePreview();
                    }
                    else
                    {
                        if (previewTask != null)
                            previewTask.terminatePreview();
                        previewTask = null;
                    }
                });
            layerTopSlider.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
                {
                    if (previewTask != null) {
                        previewTask.setTopLayer(newValue.intValue());
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
        updatePreview();
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
        updatePreview();
    }

    @Override
    public void whenModelAdded(ProjectifiableThing modelContainer)
    {
        updatePreview();
     }

    @Override
    public void whenModelsRemoved(Set<ProjectifiableThing> modelContainers)
    {
        updatePreview();
    }

    @Override
    public void whenAutoLaidOut()
    {
        updatePreview();
    }

    @Override
    public void whenModelsTransformed(Set<ProjectifiableThing> modelContainers)
    {
        updatePreview();
    }

    @Override
    public void whenModelChanged(ProjectifiableThing modelContainer, String propertyName)
    {
        updatePreview();
    }

    @Override
    public void whenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings)
    {
        updatePreview();
    }

    @Override
    public void shutdownController()
    {
        if (previewTask != null)
            previewTask.terminatePreview();
        previewTask = null;

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
            SlicerParametersFile slicerParameters = null; // This is sometimes returned as null. Not sure why.
            if (currentProject.getNumberOfProjectifiableElements() > 0)
            {
                String headType = HeadContainer.defaultHeadID;
                if (currentPrinter != null && currentPrinter.headProperty().get() != null)
                {
                    headType = currentPrinter.headProperty().get().typeCodeProperty().get();
                }
                slicerParameters = currentProject.getPrinterSettings().getSettings(headType);
            }
            if (slicerParameters != null)
            {
                //NOTE - this needs to change if raft settings in slicermapping.dat is changed
                double raftOffset = slicerParameters.getRaftBaseThickness_mm()
                        //Raft interface thickness
                        + 0.28
                        //Raft surface layer thickness * surface layers
                        + (slicerParameters.getInterfaceLayers() * 0.27)
                        + slicerParameters.getRaftAirGapLayer0_mm();

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
    
    private void updatePreview()
    {
        if (previewTask != null && modelIsSuitable())
        {
            Cancellable cancellable = new SimpleCancellable();        
            {
                Runnable runUpdatePreview = () ->
                {
                    if (cancellable.cancelled().get())
                    {
                        return;
                    }
                    
                    GetGCodePreview gcodePreviewGetter = new GetGCodePreview((ModelContainerProject) currentProject, cancellable);
                    
                    try
                    {
                        Optional<GCodePostProcessingResult> result = gcodePreviewGetter.runSlicerAndPostProcessor();
                        if (previewTask != null && result.isPresent())
                            previewTask.loadGCodeFile(result.get().getOutputFilename());
                    } catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                };

                clearPrintJobDirectories();

                GCodePreviewThreadManager.getInstance().cancelRunningAndRun(runUpdatePreview, cancellable);
            }
        }
        else
        {
            GCodePreviewThreadManager.getInstance().cancelRunningTasks();
        }
    }
}
