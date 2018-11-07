/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.RoboxProfile;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.configuration.datafileaccessors.RoboxProfileSettingsContainer;
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
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.geometry.Bounds;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * GCodePreparationManager makes sure that all tasks are properly cancelled.
 *
 * @author tony
 */
public class GCodeGeneratorManager implements ModelContainerProject.ProjectChangesListener 
{
    private static final Stenographer steno = StenographerFactory.getStenographer(GCodeGeneratorManager.class.getName());
    private final ExecutorService executorService;
    private final ModelContainerProject project;
    private Future restartTask = null;
    private Map<PrintQualityEnumeration, Future> taskMap = new HashMap<>();
    private Cancellable cancellable = null;
    private Printer currentPrinter = null;
    private boolean projectListenerInstalled = false;
    private boolean suppressReaction = false;
    private ObjectProperty<PrintQualityEnumeration> currentPrintQuality = new SimpleObjectProperty<>(PrintQualityEnumeration.DRAFT);
    private final BooleanProperty dataChanged = new SimpleBooleanProperty(false);

    public GCodeGeneratorManager(ModelContainerProject project)
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
        
        setupListeners();
    }
    
    public ModelContainerProject getProject()
    {
        return project;
    }
    
    public Optional<GCodeGeneratorResult> getPrepResult(PrintQualityEnumeration quality)
    {
        Future<GCodeGeneratorResult> resultFuture = taskMap.get(quality);
        if (resultFuture != null)
        {
            try 
            {
                return Optional.ofNullable(resultFuture.get());
            }
            catch (InterruptedException | ExecutionException | CancellationException ex)
            {
            }
        }
        return Optional.empty();
    }
    
    public Cancellable getCancellable()
    {
        return cancellable;
    }
    
    private void toggleDataChanged()
    {
        dataChanged.set(dataChanged.not().get());
    }
    
    // Listen to this property to be notified when the GCode files are being regenerated.
    public ReadOnlyBooleanProperty getDataChangedProperty()
    {
        return dataChanged;
    }

    public ReadOnlyObjectProperty<PrintQualityEnumeration> getPrintQualityProperty()
    {
        return currentPrintQuality;
    }

    public void setSuppressReaction(boolean flag)
    {
        suppressReaction = flag;
    }

    @Override
    public void whenModelAdded(ProjectifiableThing projectifiableThing) {
        reactToChange();
    }

    @Override
    public void whenModelsRemoved(Set<ProjectifiableThing> projectifiableThing) {
        reactToChange();
    }

    @Override
    public void whenAutoLaidOut() {
        reactToChange();
    }

    @Override
    public void whenModelsTransformed(Set<ProjectifiableThing> projectifiableThing) {
        reactToChange();
    }

    @Override
    public void whenModelChanged(ProjectifiableThing modelContainer, String propertyName) {
        reactToChange();
    }

    @Override
    public void whenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings) {
        reactToChange();
        if (printerSettings.getPrintQuality() != currentPrintQuality.get())
            currentPrintQuality.set(printerSettings.getPrintQuality());
    }
    
    private final PrinterListChangesListener printerListChangesListener = new PrinterListChangesAdapter()
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

    private final MapChangeListener<Integer, Filament> filamentListener = (MapChangeListener.Change<? extends Integer, ? extends Filament> change) -> {
        reactToChange();
    };

    public void purgeAllTasks()
    {
        if (cancellable != null)
                cancellable.cancelled().set(true);
        if (restartTask != null)
        {
            restartTask.cancel(true);
            restartTask = null;
        }
        taskMap.forEach((q, t) -> t.cancel(true));
        taskMap.clear();
    }

    public void restartAllTasks()
    {
        steno.info("Restarting all GCodePrep tasks");
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
            
            Stream.of(PrintQualityEnumeration.values()).forEach(printQuality ->
            {
                String headType = HeadContainer.defaultHeadID;
                SlicerType slicerType = Lookup.getUserPreferences().getSlicerType();

                if (currentPrinter != null && currentPrinter.headProperty().get() != null)
                {
                    headType = currentPrinter.headProperty().get().typeCodeProperty().get();
                }
                Optional<RoboxProfile> profileSettings = getProfileSettings(headType, slicerType, printQuality);
                if (profileSettings.isPresent())
                {
                    GCodeGeneratorTask prepTask = new GCodeGeneratorTask();
                    Supplier<PrintableMeshes> meshSupplier = () ->
                    {
                        List<MeshForProcessing> meshesForProcessing = new ArrayList<>();
                        List<Integer> extruderForModel = new ArrayList<>();

                        // Only to be run on a ModelContainerProject
                        for (ProjectifiableThing modelContainer : project.getTopLevelThings())
                        {
                            for (ModelContainer modelContainerWithMesh : ((ModelContainer)modelContainer).getModelsHoldingMeshViews())
                            {
                                MeshForProcessing meshForProcessing = new MeshForProcessing(modelContainerWithMesh.getMeshView(), modelContainerWithMesh);
                                meshesForProcessing.add(meshForProcessing);
                                extruderForModel.add(modelContainerWithMesh.getAssociateWithExtruderNumberProperty().get());
                            }
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

                        PrinterSettingsOverrides printerSettings = project.getPrinterSettings().duplicate();
                        // Fill density is reset to default for all but the current print quality.
                        if (printQuality != currentPrintQuality.get())
                        {
                            float fillDensity = profileSettings.get().getSpecificFloatSetting("fillDensity_normalised");
                            printerSettings.setFillDensityOverride(fillDensity);
                        }
                        
                        return new PrintableMeshes(
                                meshesForProcessing,
                                project.getUsedExtruders(currentPrinter),
                                extruderForModel,
                                project.getProjectName(),
                                project.getProjectName(),
                                profileSettings.get(),
                                printerSettings,
                                printQuality,
                                slicerType,
                                centreOfPrintedObject,
                                Lookup.getUserPreferences().isSafetyFeaturesOn(),
                                Lookup.getUserPreferences().isTimelapseTriggerEnabled(),
                                cameraTriggerData);
                    };

                    if (cancellable.cancelled().get())
                        return;
                    taskMap.put(printQuality, prepTask);
                    prepTask.initialise(currentPrinter, meshSupplier, getGCodeDirectory(printQuality));
                    executorService.submit(prepTask);
                }
                toggleDataChanged();
            });
        };

        cancellable = new SimpleCancellable();
        restartTask = executorService.submit(restartAfterDelay);
    }
    
    private Optional<RoboxProfile> getProfileSettings(String headType,
                                                      SlicerType slicerType,
                                                      PrintQualityEnumeration printQuality)
    {
        Optional<RoboxProfile> profileSettings = Optional.empty();

        switch (printQuality)
        {
            case CUSTOM:
                if (!project.getPrinterSettings().getSettingsName().equals(""))
                {
                    String settingsName = project.getPrinterSettings().getSettingsName();
                    profileSettings = RoboxProfileSettingsContainer.getInstance().getRoboxProfileWithName(
                            settingsName, slicerType, headType);
                }
                break;
                
            case DRAFT:
                profileSettings = RoboxProfileSettingsContainer.getInstance().getRoboxProfileWithName(
                            BaseConfiguration.draftSettingsProfileName, slicerType, headType);
                break;

            case NORMAL:
                profileSettings = RoboxProfileSettingsContainer.getInstance().getRoboxProfileWithName(
                            BaseConfiguration.normalSettingsProfileName, slicerType, headType);
                break;

            case FINE:
                profileSettings = RoboxProfileSettingsContainer.getInstance().getRoboxProfileWithName(
                            BaseConfiguration.fineSettingsProfileName, slicerType, headType);
                break;
                
            default:
                break;
        }
        
        return profileSettings;
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
    
    // NOT USED CURRENTLY?? NEED TO THINK ABOUT PROJECTS A BIT MORE
    public void tidyProjectDirectories()
    {
        // Erase old gCode directories
        String directoryName = ApplicationConfiguration.getProjectDirectory()
                + project.getProjectName() + "-GCode";
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

    private void setupListeners()
    {
        ApplicationStatus.getInstance().modeProperty().addListener((o, oldValue, newValue) -> 
        {
            if (newValue == ApplicationMode.SETTINGS)
            {
                if (!projectListenerInstalled)
                {
                    project.addProjectChangesListener(this);
                    projectListenerInstalled = true;
                }
                restartAllTasks();
            }
            else
            {
                purgeAllTasks();
            }
        });
        
        Lookup.getSelectedPrinterProperty().addListener((o, oldValue, newValue) -> 
        {
            if (currentPrinter != newValue)
            {
                purgeAllTasks();
                if (currentPrinter != null)
                {
                    currentPrinter.effectiveFilamentsProperty().removeListener(filamentListener);
                }

                    
                currentPrinter = newValue;
                if (currentPrinter != null)
                {
                    currentPrinter.effectiveFilamentsProperty().addListener(filamentListener);
                }
                reactToChange();
            }
        });
        BaseLookup.getPrinterListChangesNotifier().addListener(printerListChangesListener);
    }
    
    private void reactToChange()
    {
        if (!suppressReaction)
        {
            if ((ApplicationStatus.getInstance().modeProperty().get() == ApplicationMode.SETTINGS ||
                 ApplicationStatus.getInstance().modeProperty().get() == ApplicationMode.STATUS) &&
                modelIsSuitable())
            {
                restartAllTasks(); 
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
}
