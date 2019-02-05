/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.RoboxProfile;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.printerControl.model.PrinterListChangesAdapter;
import celtech.roboxbase.printerControl.model.PrinterListChangesListener;
import celtech.roboxbase.services.CameraTriggerData;
import celtech.roboxbase.services.gcodegenerator.GCodeGeneratorResult;
import celtech.roboxbase.services.gcodegenerator.GCodeGeneratorTask;
import celtech.roboxbase.services.slicer.PrintQualityEnumeration;
import celtech.roboxbase.utils.models.MeshForProcessing;
import celtech.roboxbase.utils.models.PrintableMeshes;
import celtech.roboxbase.utils.tasks.Cancellable;
import celtech.roboxbase.utils.tasks.SimpleCancellable;
import celtech.roboxbase.utils.threed.CentreCalculations;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.geometry.Bounds;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * GCodePreparationManager deals with {@link GCodeGeneratorTask}s.
 * It's job is to restart and cancel the tasks when appropriate and to also keep
 * track of the currently selected task.
 *
 * @author Tony and George
 */
public class GCodeGeneratorManager implements ModelContainerProject.ProjectChangesListener
{
    private static final Stenographer STENO = StenographerFactory.getStenographer(GCodeGeneratorManager.class.getName());
    
    private final ExecutorService executorService;
    private final Project project;
    
    private final BooleanProperty dataChanged = new SimpleBooleanProperty(false);
    private final DoubleProperty selectedTaskProgress = new SimpleDoubleProperty(-1);
    private final BooleanProperty selectedTaskRunning = new SimpleBooleanProperty(false);
    private String selectedTaskMessage = "";
    
    private final BooleanProperty gCodeForPrintOrSave = new SimpleBooleanProperty(false);
    
    private ChangeListener<Number> taskProgressChangeListener;
    private ChangeListener<Boolean> taskRunningChangeListener;
    private ChangeListener<String> taskMessageChangeListener;
    
    private Future restartTask = null;
    private Map<PrintQualityEnumeration, Future> taskMap = new HashMap<>();
    private ObservableMap<PrintQualityEnumeration, Future> observableTaskMap = FXCollections.observableMap(taskMap);
    
    private Cancellable cancellable = null;
    private Printer currentPrinter = null;
    private boolean projectListenerInstalled = false;
    private boolean suppressReaction = false;
    
    private ObjectProperty<PrintQualityEnumeration> currentPrintQuality = new SimpleObjectProperty<>(PrintQualityEnumeration.DRAFT);
    private List<PrintQualityEnumeration> slicingOrder = Arrays.asList(PrintQualityEnumeration.values()); 
    private GCodeGeneratorTask selectedTask;

    private ChangeListener applicationModeChangeListener;
    private ChangeListener selectedPrinterReactionChangeListener;
    private PrinterListChangesListener printerListChangesListener;
    private MapChangeListener<Integer, Filament> filamentListener;
    
    private boolean projectNeedsSlicing = true;
    
    public GCodeGeneratorManager(Project project)
    {
        this.project = project;
        ThreadFactory threadFactory = (Runnable runnable) ->
        {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setDaemon(true);
            return thread;
        };
        executorService = Executors.newFixedThreadPool(1, threadFactory);
        currentPrinter = Lookup.getSelectedPrinterProperty().get();
        
        initialiseListeners();
    }
    
    private void initialiseListeners() 
    {
        
        filamentListener = (MapChangeListener.Change<? extends Integer, ? extends Filament> change) -> 
        {
            reactToChange();
        };
        
        applicationModeChangeListener = (o, oldValue, newValue) -> 
        {
            if (newValue == ApplicationMode.SETTINGS)
            {
                if (!projectListenerInstalled)
                {
                    project.addProjectChangesListener(this);
                    projectListenerInstalled = true;
                }
                if(isCurrentProjectSelected() && projectNeedsSlicing) 
                {
                    restartAllTasks();
                    waitAndThenBindNewTask();
                    projectNeedsSlicing = false;
                }
            }
        };
        
        selectedPrinterReactionChangeListener = (o, oldValue, newValue) -> 
        {
            if (currentPrinter != newValue) 
            {
                if (currentPrinter != null) 
                {
                    currentPrinter.effectiveFilamentsProperty().removeListener(filamentListener);
                }
                purgeAllTasks();
                currentPrinter = (Printer) newValue;
                if (currentPrinter != null) 
                {
                    currentPrinter.effectiveFilamentsProperty().addListener(filamentListener);
                }
                reactToChange();
            }
        };
        
        printerListChangesListener = new PrinterListChangesAdapter() 
        {
            @Override
            public void whenHeadAdded(Printer printer)
            {
                if (printer == currentPrinter)
                {
                    reactToChange();
                }
            }

            @Override
            public void whenExtruderAdded(Printer printer, int extruderIndex) 
            {
                if (printer == currentPrinter)
                {
                    reactToChange();
                }
            }
        };
        
        taskProgressChangeListener = (observable, oldValue, newValue) -> 
        {
            selectedTaskProgress.set((double) newValue);
        };
        
        taskRunningChangeListener = (observable, oldValue, newValue) -> 
        {
            selectedTaskRunning.set(newValue);
        };
        
        taskMessageChangeListener = (observable, oldValue, newValue) -> 
        {
            selectedTaskMessage = newValue;
        };
        
        ApplicationStatus.getInstance().modeProperty().addListener(applicationModeChangeListener);
        Lookup.getSelectedPrinterProperty().addListener(selectedPrinterReactionChangeListener);
        BaseLookup.getPrinterListChangesNotifier().addListener(printerListChangesListener);
    }
    
    public Optional<GCodeGeneratorResult> getPrepResult(PrintQualityEnumeration quality, boolean gCodeForPrintOrSave)
    {
        Future<GCodeGeneratorResult> resultFuture = taskMap.get(quality);
        
        // If we have already flagged that we are printing or saving, leave the flag be.
        // When canceled or finished the flag will be explicitly set to false.
        if(!this.gCodeForPrintOrSave.get())
        {
            this.gCodeForPrintOrSave.set(gCodeForPrintOrSave);
        }
        if (resultFuture != null)
        {
            try 
            {
                GCodeGeneratorResult result = resultFuture.get();
                // Once result fetched reset the flag.
                this.gCodeForPrintOrSave.set(false);
                return Optional.ofNullable(result);
            }
            catch (InterruptedException ex)
            {
                STENO.debug("Thread interrupted, usually the case when the user has selected differen't profile settings");
            }
            catch (CancellationException ex) 
            {
                STENO.debug("Thread cancelled, usually the case when the user has started a print then cancelled during slicing");
            }
            catch (ExecutionException ex)
            {
                STENO.exception("Unexpected error when fetching GCodeGeneratorResult", ex);
            }
        }
        this.gCodeForPrintOrSave.set(false);
        return Optional.empty();
    }
    
    private void toggleDataChanged()
    {
        dataChanged.set(dataChanged.not().get());
    }

    public void setSuppressReaction(boolean flag)
    {
        suppressReaction = flag;
    }

    
    
    public void purgeAllTasks()
    {
        if(isCurrentProjectSelected()) 
        {
            if (cancellable != null)
            {
                cancellable.cancelled().set(true);
            }
            
            if (restartTask != null)
            {
                restartTask.cancel(true);
                restartTask = null;
            }
            
            observableTaskMap.forEach((q, t) -> t.cancel(true));
            observableTaskMap.clear();
            
            gCodeForPrintOrSave.set(false);
            projectNeedsSlicing = true;
        }
    }

    private void restartAllTasks()
    {
        STENO.info("Restarting all GCodePrep tasks");
        purgeAllTasks();
        
        Runnable restartAfterDelay = () ->
        {
            try
            {
                Thread.sleep(500);
            } catch (InterruptedException ex)
            {
                return;
            }
            
            slicingOrder.forEach(printQuality ->
            {
                String headType = HeadContainer.defaultHeadID;
                SlicerType slicerType = Lookup.getUserPreferences().getSlicerType();

                if (currentPrinter != null && currentPrinter.headProperty().get() != null)
                {
                    headType = currentPrinter.headProperty().get().typeCodeProperty().get();
                }
                
                PrinterSettingsOverrides printerSettingsOverrides = project.getPrinterSettings().duplicate();
                printerSettingsOverrides.setPrintQuality(printQuality);
                RoboxProfile profileSettings = printerSettingsOverrides.getSettings(headType, slicerType);
                if (profileSettings != null)
                {
                    GCodeGeneratorTask prepTask = new GCodeGeneratorTask();
                    Supplier<PrintableMeshes> meshSupplier = () ->
                    {
                        List<MeshForProcessing> meshesForProcessing = new ArrayList<>();
                        List<Integer> extruderForModel = new ArrayList<>();

                        // Only to be run on a ModelContainerProject
                        if(project instanceof ModelContainerProject)
                        {
                            project.getTopLevelThings().forEach((modelContainer) -> 
                            {
                                ((ModelContainer)modelContainer).getModelsHoldingMeshViews().forEach((modelContainerWithMesh) ->
                                {
                                    MeshForProcessing meshForProcessing = new MeshForProcessing(modelContainerWithMesh.getMeshView(), modelContainerWithMesh);
                                    meshesForProcessing.add(meshForProcessing);
                                    extruderForModel.add(modelContainerWithMesh.getAssociateWithExtruderNumberProperty().get());
                                });
                            });
                        }

                        // We need to tell the slicers where the centre of the printed objects is - otherwise everything is put in the centre of the bed...
                        CentreCalculations centreCalc = new CentreCalculations();

                        project.getTopLevelThings().forEach(model ->
                        {
                            Bounds modelBounds = model.getBoundsInParent();
                            centreCalc.processPoint(modelBounds.getMinX(), modelBounds.getMinY(), modelBounds.getMinZ());
                            centreCalc.processPoint(modelBounds.getMaxX(), modelBounds.getMaxY(), modelBounds.getMaxZ());
                        });

                        Vector3D centreOfPrintedObject = centreCalc.getResult();

                        CameraTriggerData cameraTriggerData = null;

                        if (Lookup.getUserPreferences().isTimelapseTriggerEnabled())
                        {
                            cameraTriggerData = new CameraTriggerData(
                                    Lookup.getUserPreferences().getGoProWifiPassword(),
                                    Lookup.getUserPreferences().isTimelapseMoveBeforeCapture(),
                                    Lookup.getUserPreferences().getTimelapseXMove(),
                                    Lookup.getUserPreferences().getTimelapseYMove(),
                                    Lookup.getUserPreferences().getTimelapseDelayBeforeCapture(),
                                    Lookup.getUserPreferences().getTimelapseDelay());
                        }
                        
                        return new PrintableMeshes(
                                meshesForProcessing,
                                project.getUsedExtruders(currentPrinter),
                                extruderForModel,
                                project.getProjectName(),
                                project.getProjectName(),
                                profileSettings,
                                printerSettingsOverrides,
                                printQuality,
                                slicerType,
                                centreOfPrintedObject,
                                Lookup.getUserPreferences().isSafetyFeaturesOn(),
                                Lookup.getUserPreferences().isTimelapseTriggerEnabled(),
                                cameraTriggerData);
                    };

                    if (cancellable.cancelled().get())
                        return;
                    observableTaskMap.put(printQuality, prepTask);
                    tidyProjectDirectory(getGCodeDirectory(printQuality));
                    prepTask.initialise(currentPrinter, meshSupplier, getGCodeDirectory(printQuality));
                    executorService.submit(prepTask);
                }
                toggleDataChanged();
            });
        };

        cancellable = new SimpleCancellable();
        restartTask = executorService.submit(restartAfterDelay);
    }
    
    public String getGCodeDirectory(PrintQualityEnumeration printQuality)
    {
        String directoryName = ApplicationConfiguration.getProjectDirectory()
                + project.getProjectName() 
                + File.separator 
                + printQuality.getFriendlyName()
                + File.separator;
        File dirHandle = new File(directoryName);

        if (!dirHandle.exists())
        {
            dirHandle.mkdirs();
        }
        return directoryName;
    }
    
    private void tidyProjectDirectory(String directoryName)
    {
        File projectDirectory = new File(directoryName);
        File[] filesOnDisk = projectDirectory.listFiles();
        if (filesOnDisk != null)
        {
            for (int i = 0; i < filesOnDisk.length; ++i)
            {
                FileUtils.deleteQuietly(filesOnDisk[i]);
            }
        }
    }
    
    private void reactToChange() 
    {
        if (!suppressReaction && isCurrentProjectSelected()) 
        {
            if ((ApplicationStatus.getInstance().modeProperty().get() == ApplicationMode.SETTINGS) 
                    && modelIsSuitable()) 
            {
                restartAllTasks();
                waitAndThenBindNewTask();
                projectNeedsSlicing = false;
            }
            else 
            {
                purgeAllTasks();
            }
        }
    }
    
    public boolean modelIsSuitable()
    {
        if (project != null)
        {
            RoboxProfile slicerParameters = null; // This is sometimes returned as null. Not sure why.
            if (project.getNumberOfProjectifiableElements() > 0)
            {
                String headType = HeadContainer.defaultHeadID;
                if (currentPrinter != null && currentPrinter.headProperty().get() != null)
                {
                    headType = currentPrinter.headProperty().get().typeCodeProperty().get();
                }
                slicerParameters = project.getPrinterSettings().getSettings(headType, Lookup.getUserPreferences().getSlicerType());
            }
            if (slicerParameters != null)
            {
                // NOTE - this needs to change if raft settings in slicermapping.dat is changed                
                // Needed as heads differ in size and will need to adjust print volume for this
                double zReduction = 0.0;
                if (currentPrinter != null && currentPrinter.headProperty().get() != null)
                {
                    zReduction = currentPrinter.headProperty().get().getZReductionProperty().get();
                }
            
                //NOTE - this needs to change if raft settings in slicermapping.dat is changed
                double raftOffset = slicerParameters.getSpecificFloatSetting("raftBaseThickness_mm")
                    //Raft interface thickness
                    + 0.28
                    //Raft surface layer thickness * surface layers
                    + 0.27 * slicerParameters.getSpecificIntSetting("interfaceLayers")
                    + slicerParameters.getSpecificFloatSetting("raftAirGapLayer0_mm")
                    + zReduction;

                for (ProjectifiableThing projectifiableThing : project.getTopLevelThings())
                {
                    if (projectifiableThing instanceof ModelContainer)
                    {
                        ModelContainer modelContainer = (ModelContainer) projectifiableThing;

                        //TODO use settings derived offset values for spiral
                        if (modelContainer.isOffBedProperty().get()
                                || (project.getPrinterSettings().getRaftOverride()
                                && modelContainer.isModelTooHighWithOffset(raftOffset))
                                || (project.getPrinterSettings().getSpiralPrintOverride()
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
    
    private boolean isCurrentProjectSelected()
    {
        return Lookup.getSelectedProjectProperty().get() == project;
    }
    
    public void changeSlicingOrder(List<PrintQualityEnumeration> slicingOrder) 
    {
        this.slicingOrder = slicingOrder;
        PrintQualityEnumeration selectedQuality = slicingOrder.get(0);
        bindToSelectedTask(selectedQuality);
    }
    
    /**
     * Return the {@link GCodeGeneratorTask} attached to the given {@link PrintQualityEnumeration}
     * 
     * @param printQuality
     * @return An Optional task as the map may not contain anything
     */
    public Optional<GCodeGeneratorTask> getTaskFromTaskMap(PrintQualityEnumeration printQuality) 
    {
        Future gCodeGenTask = taskMap.get(printQuality);
        if(gCodeGenTask != null && gCodeGenTask instanceof GCodeGeneratorTask) 
        {
            return Optional.of((GCodeGeneratorTask) gCodeGenTask);
        }
        
        return Optional.empty();
    }
    
    /**
     * Bind the {@link GCodeGeneratorTask} denoted by the selected print quality.
     * Attaches listeners to the Tasks running and progress properties.
     * 
     * @param selectedQuality 
     */
    private void bindToSelectedTask(PrintQualityEnumeration selectedQuality) 
    {
        unbindCurrentTask();
        Optional<GCodeGeneratorTask> potentialSelectedTask = getTaskFromTaskMap(selectedQuality);
        if(potentialSelectedTask.isPresent())
        {
            selectedTask = potentialSelectedTask.get();
            selectedTask.runningProperty().addListener(taskRunningChangeListener);
            selectedTask.progressProperty().addListener(taskProgressChangeListener);
            selectedTask.messageProperty().addListener(taskMessageChangeListener);
        }
    }
    
    /**
     * Unbind the currently bound {@link GCodeGeneratorTask} by removing the listeners attached 
     */
    private void unbindCurrentTask()
    {
        if(selectedTask != null) 
        {
            selectedTask.runningProperty().removeListener(taskRunningChangeListener);
            selectedTask.progressProperty().removeListener(taskProgressChangeListener);
            selectedTask.messageProperty().removeListener(taskMessageChangeListener);
        }
    }
    
    private void waitAndThenBindNewTask() 
    {
        executorService.submit(() -> 
        {
            try {
                restartTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                STENO.exception("Restarting tasks in GCodeGeneratorManager has caused an error", ex);
            }
            PrintQualityEnumeration selectedQuality = slicingOrder.get(0);
            BaseLookup.getTaskExecutor().runOnGUIThread(() -> {bindToSelectedTask(selectedQuality);});
        });
    }
    
    public Project getProject()
    {
        return project;
    }
    
    public BooleanProperty getGCodeForPrintOrSaveProperty()
    {
        return gCodeForPrintOrSave;
    }
    
    public ObservableMap<PrintQualityEnumeration, Future> getObservableTaskMap() 
    {
        return observableTaskMap;
    }
    
    public Cancellable getCancellable()
    {
        return cancellable;
    }
    
    /**
     * Listen to this property to be notified when the GCode files are being regenerated.
     * 
     * @return
     */
    public ReadOnlyBooleanProperty getDataChangedProperty()
    {
        return dataChanged;
    }
    
    /**
     * This property mirrors the progress of the selected profile task.
     * 
     * @return 
     */
    public final ReadOnlyDoubleProperty selectedTaskProgressProperty() 
    { 
        return selectedTaskProgress;
    }
    
    /**
     * This property mirrors the running property from the selected profile task.
     * 
     * @return 
     */
    public final ReadOnlyBooleanProperty selectedTaskRunningProperty()
    {
        return selectedTaskRunning;
    }

    public ReadOnlyObjectProperty<PrintQualityEnumeration> getPrintQualityProperty()
    {
        return currentPrintQuality;
    }
    
    public String getSelectedTaskMessage() 
    {
        return selectedTaskMessage;
    }
    
    @Override
    public void whenModelAdded(ProjectifiableThing projectifiableThing)
    {
        reactToChange();
    }

    @Override
    public void whenModelsRemoved(Set<ProjectifiableThing> projectifiableThing) 
    {
        reactToChange();
    }

    @Override
    public void whenAutoLaidOut() 
    {
        reactToChange();
    }

    @Override
    public void whenModelsTransformed(Set<ProjectifiableThing> projectifiableThing)
    {
        reactToChange();
    }

    @Override
    public void whenModelChanged(ProjectifiableThing modelContainer, String propertyName) 
    {
        reactToChange();
    }

    @Override
    public void whenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings) 
    {
        reactToChange();
        if (printerSettings.getPrintQuality() != currentPrintQuality.get())
        {
            currentPrintQuality.set(printerSettings.getPrintQuality());
        }
    }
}
