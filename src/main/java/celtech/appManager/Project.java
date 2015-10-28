package celtech.appManager;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.PrintBed;
import celtech.configuration.SlicerType;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.configuration.fileRepresentation.ProjectFile;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.printerControl.model.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.utils.Math.Packing.PackingThing;
import celtech.utils.threed.MeshUtils;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;


/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class Project implements Serializable
{

    private static final long serialVersionUID = 1L;


    public static class ProjectLoadException extends Exception
    {

        public ProjectLoadException(String message)
        {
            super(message);
        }

    }

    private Filament DEFAULT_FILAMENT;

    private static final String ASSOCIATE_WITH_EXTRUDER_NUMBER = "associateWithExtruderNumber";

    private static final Stenographer steno = StenographerFactory.getStenographer(
        Project.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    Set<ProjectChangesListener> projectChangesListeners;

    private ObservableList<ModelContainer> topLevelModels;
    private String lastPrintJobID = "";
    private ObjectProperty<Filament> extruder0Filament;
    private ObjectProperty<Filament> extruder1Filament;
    private BooleanProperty modelColourChanged;
    private BooleanProperty canPrint;
    private BooleanProperty customSettingsNotChosen;
    private BooleanBinding hasInvalidMeshes;

    private final PrinterSettings printerSettings;

    private final StringProperty projectNameProperty;
    private ObjectProperty<Date> lastModifiedDate;
    private FilamentContainer filamentContainer;

    private boolean suppressProjectChanged = false;

    public Project()
    {
        initialise();

        initialiseExtruderFilaments();
        printerSettings = new PrinterSettings();
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("-hhmmss-ddMMYY");
        projectNameProperty = new SimpleStringProperty(Lookup.i18n("projectLoader.untitled")
            + formatter.format(now));
        lastModifiedDate.set(now);
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);

        customSettingsNotChosen.bind(
            printerSettings.printQualityProperty().isEqualTo(PrintQualityEnumeration.CUSTOM)
            .and(printerSettings.getSettingsNameProperty().isEmpty()));
        // Cannot print if quality is CUSTOM and no custom settings have been chosen
        canPrint.bind(customSettingsNotChosen.not());

        printerSettings.getDataChanged().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                projectModified();
                fireWhenPrinterSettingsChanged(printerSettings);
            });

        Lookup.getUserPreferences().getSlicerTypeProperty().addListener(
            (ObservableValue<? extends SlicerType> observable, SlicerType oldValue, SlicerType newValue) ->
            {
                projectModified();
            });

    }

    private void initialise()
    {
        topLevelModels = FXCollections.observableArrayList();
        hasInvalidMeshes = new BooleanBinding()
        {
            {
                super.bind(topLevelModels);
            }

            @Override
            protected boolean computeValue()
            {
                if (getModelContainersWithInvalidMesh().isEmpty())
                {
                    return false;
                } else
                {
                    return true;
                }
            }
        };
        extruder0Filament = new SimpleObjectProperty<>();
        extruder1Filament = new SimpleObjectProperty<>();
        modelColourChanged = new SimpleBooleanProperty();
        canPrint = new SimpleBooleanProperty(true);
        customSettingsNotChosen = new SimpleBooleanProperty(true);
        lastModifiedDate = new SimpleObjectProperty<>();
        filamentContainer = Lookup.getFilamentContainer();
        DEFAULT_FILAMENT = filamentContainer.getFilamentByID("RBX-ABS-GR499");
        projectChangesListeners = new HashSet<>();
    }

    public final void setProjectName(String value)
    {
        projectNameProperty.set(value);
    }

    public final String getProjectName()
    {
        return projectNameProperty.get();
    }

    public final StringProperty projectNameProperty()
    {
        return projectNameProperty;
    }

    public final String getAbsolutePath()
    {
        return ApplicationConfiguration.getProjectDirectory() + File.separator
            + projectNameProperty.get()
            + ApplicationConfiguration.projectFileExtension;
    }

    public Set<ModelContainer> getModelContainersWithInvalidMesh()
    {
        Set<ModelContainer> invalidModelContainers = new HashSet<>();
        getAllModels().stream().filter((modelContainer)
            -> (modelContainer.isInvalidMesh())).forEach((modelContainer) ->
                {
                    invalidModelContainers.add(modelContainer);
            });
        return invalidModelContainers;
    }

    public BooleanBinding hasInvalidMeshes()
    {
        return hasInvalidMeshes;
    }

    static Project loadProject(String basePath)
    {
        try
        {
            Project project = new Project();
            char firstNonWhitespaceCharacter = getFirstNonWhitespaceCharacter(basePath);
            if (firstNonWhitespaceCharacter == '{')
            {
                project.load(basePath);
            } else
            {
                loadLegacyProjectFile(basePath, project);
            }
            return project;
        } catch (Exception ex)
        {
            steno.exception("Unable to load project file at " + basePath, ex);
        }
        return null;
    }

    /**
     * Load a Serialisable format project.
     */
    private static void loadLegacyProjectFile(String basePath, Project project)
    {
        try
        {
            FileInputStream projectFileStream = new FileInputStream(basePath
                + ApplicationConfiguration.projectFileExtension);
            ObjectInputStream reader = new ObjectInputStream(new BufferedInputStream(
                projectFileStream));
            Project loadedProject = (Project) reader.readObject();
            reader.close();
            for (ModelContainer modelContainer : loadedProject.topLevelModels)
            {
                project.addModel(modelContainer);
            }
            String[] fileNameElements = basePath.split(File.separator);
            project.setProjectName(fileNameElements[fileNameElements.length - 1]);

        } catch (Exception ex)
        {
            steno.exception("Unable to load old project format file", ex);
        }
    }

    /**
     * Get the first non-whitespace character in a file. If there is no non-whitespace character
     * then return a space.
     */
    private static char getFirstNonWhitespaceCharacter(String basePath) throws FileNotFoundException, IOException
    {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(basePath
            + ApplicationConfiguration.projectFileExtension));
        String line;
        while ((line = bufferedReader.readLine()) != null)
        {
            for (char c : line.toCharArray())
            {
                if (!Character.isWhitespace(c))
                {
                    return c;
                }
            }
        }
        return ' ';
    }

    private void load(String basePath) throws ProjectLoadException
    {
        suppressProjectChanged = true;

        File file = new File(basePath + ApplicationConfiguration.projectFileExtension);

        try
        {
            ProjectFile projectFile = mapper.readValue(file, ProjectFile.class);

            projectNameProperty.set(projectFile.getProjectName());
            lastModifiedDate.set(projectFile.getLastModifiedDate());
            lastPrintJobID = projectFile.getLastPrintJobID();

            String filamentID0 = projectFile.getExtruder0FilamentID();
            String filamentID1 = projectFile.getExtruder1FilamentID();
            if (!filamentID0.equals("NULL"))
            {
                Filament filament0 = filamentContainer.getFilamentByID(filamentID0);
                if (filament0 != null)
                {
                    extruder0Filament.set(filament0);
                }
            }
            if (!filamentID1.equals("NULL"))
            {
                Filament filament1 = filamentContainer.getFilamentByID(filamentID1);
                if (filament1 != null)
                {
                    extruder1Filament.set(filament1);
                }
            }

            printerSettings.setSettingsName(projectFile.getSettingsName());
            printerSettings.setPrintQuality(projectFile.getPrintQuality());
            printerSettings.setBrimOverride(projectFile.getBrimOverride());
            printerSettings.setFillDensityOverride(projectFile.getFillDensityOverride());
            printerSettings.setPrintSupportOverride(projectFile.getPrintSupportOverride());
            printerSettings.setRaftOverride(projectFile.getPrintRaft());

            loadModels(basePath);

            recreateGroups(projectFile.getGroupStructure(), projectFile.getGroupState());

        } catch (IOException ex)
        {
            steno.exception("Failed to load project " + basePath, ex);
        } catch (ClassNotFoundException ex)
        {
            steno.exception("Failed to load project " + basePath, ex);
        }
        suppressProjectChanged = false;
    }

    private void loadModels(String basePath) throws IOException, ClassNotFoundException
    {
        FileInputStream fileInputStream = new FileInputStream(basePath
            + ApplicationConfiguration.projectModelsFileExtension);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        ObjectInputStream modelsInput = new ObjectInputStream(bufferedInputStream);
        int numModels = modelsInput.readInt();
        for (int i = 0; i < numModels; i++)
        {
            ModelContainer modelContainer = (ModelContainer) modelsInput.readObject();
            Optional<MeshUtils.MeshError> error = MeshUtils.validate(
                (TriangleMesh) modelContainer.getMeshView().getMesh());
            if (error.isPresent())
            {
                modelContainer.setIsInvalidMesh(true);
            }
            addModel(modelContainer);
        }
    }

    public static void saveProject(Project project)
    {
        String basePath = ApplicationConfiguration.getProjectDirectory() + File.separator
            + project.getProjectName();
        project.save(basePath);
    }

    private void saveModels(String path) throws IOException
    {
        ObjectOutputStream modelsOutput = new ObjectOutputStream(new FileOutputStream(path));

        Set<ModelContainer> modelsHoldingMeshViews = getModelsHoldingMeshViews();

        modelsOutput.writeInt(modelsHoldingMeshViews.size());
        for (ModelContainer modelsHoldingMeshView : modelsHoldingMeshViews)
        {
            modelsOutput.writeObject(modelsHoldingMeshView);
        }
    }

    private void save(String basePath)
    {
        if (topLevelModels.size() > 0)
        {
            try
            {
                ProjectFile projectFile = new ProjectFile();
                projectFile.populateFromProject(this);
                File file = new File(basePath + ApplicationConfiguration.projectFileExtension);
                mapper.writeValue(file, projectFile);
                saveModels(basePath + ApplicationConfiguration.projectModelsFileExtension);
            } catch (FileNotFoundException ex)
            {
                steno.exception("Failed to save project state", ex);
            } catch (IOException ex)
            {
                steno.exception(
                    "Couldn't write project state to file for project "
                    + projectNameProperty.get(), ex);
            }
        }
    }

    /**
     * Return true if all objects are on the same extruder, else return false.
     */
    public boolean allModelsOnSameExtruder()
    {
        return getUsedExtruders().size() < 2;
    }

    private void getUsedExtruders(ModelContainer modelContainer, Set<Integer> usedExtruders)
    {
        if (modelContainer instanceof ModelGroup)
        {
            for (ModelContainer subModel : ((ModelGroup) modelContainer).getChildModelContainers())
            {
                getUsedExtruders(subModel, usedExtruders);
            }
        } else
        {
            usedExtruders.add(modelContainer.getAssociateWithExtruderNumberProperty().get());
        }
    }

    /**
     * Return which extruders are used by the project, as a set of the extruder numbers.
     */
    public Set<Integer> getUsedExtruders()
    {
        Set<Integer> usedExtruders = new HashSet<>();
        for (ModelContainer loadedModel : topLevelModels)
        {
            getUsedExtruders(loadedModel, usedExtruders);
        }
        return usedExtruders;
    }

    /**
     * This is the Project file reader for version 1.01.04 and is used for reading old project
     * files.
     */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        initialise();

        ProjectHeader projectHeader = (ProjectHeader) in.readObject();

        int numberOfModels = in.readInt();
        for (int counter = 0; counter < numberOfModels; counter++)
        {
            ModelContainer model = (ModelContainer) in.readObject();
            addModel(model);
        }
    }

    /**
     * Return all top-level groups and model containers.
     */
    public ObservableList<ModelContainer> getTopLevelModels()
    {
        return topLevelModels;
    }

    /**
     * Return all ModelGroups and ModelContainers within the project.
     */
    public Set<ModelContainer> getAllModels()
    {
        Set<ModelContainer> allModelContainers = new HashSet<>();
        for (ModelContainer loadedModel : topLevelModels)
        {
            allModelContainers.add(loadedModel);
            allModelContainers.addAll(loadedModel.getDescendentModelContainers());
        }
        return allModelContainers;
    }

    @Override
    public String toString()
    {
        return projectNameProperty.get();
    }

    public void setLastPrintJobID(String printJobID)
    {
        lastPrintJobID = printJobID;
    }

    private void projectModified()
    {
        if (!suppressProjectChanged)
        {
            lastPrintJobID = "";
            lastModifiedDate.set(new Date());
        }
    }

    public String getLastPrintJobID()
    {
        return lastPrintJobID;
    }

    public ReadOnlyBooleanProperty getModelColourChanged()
    {
        return modelColourChanged;
    }

    public PrintQualityEnumeration getPrintQuality()
    {
        return printerSettings.getPrintQuality();
    }

    public void setPrintQuality(PrintQualityEnumeration printQuality)
    {
        if (printerSettings.getPrintQuality() != printQuality)
        {
            projectModified();
            printerSettings.setPrintQuality(printQuality);
        }
    }

    public void setExtruder0Filament(Filament filament)
    {
        extruder0Filament.set(filament);
    }

    public void setExtruder1Filament(Filament filament)
    {
        extruder1Filament.set(filament);
    }

    public ObjectProperty<Filament> getExtruder0FilamentProperty()
    {
        return extruder0Filament;
    }

    public ObjectProperty<Filament> getExtruder1FilamentProperty()
    {
        return extruder1Filament;
    }

    /**
     * For new projects this should be called to initialise the extruder filaments according to the
     * currently selected printer.
     */
    private void initialiseExtruderFilaments()
    {
        // set defaults in case of no printer or reel
        extruder0Filament.set(DEFAULT_FILAMENT);
        extruder1Filament.set(DEFAULT_FILAMENT);

        Printer printer = Lookup.getSelectedPrinterProperty().get();
        if (printer != null)
        {
            if (printer.reelsProperty().containsKey(0))
            {
                String filamentID = printer.reelsProperty().get(0).filamentIDProperty().get();
                extruder0Filament.set(filamentContainer.getFilamentByID(filamentID));
            }
            if (printer.reelsProperty().containsKey(1))
            {
                String filamentID = printer.reelsProperty().get(1).filamentIDProperty().get();
                extruder1Filament.set(filamentContainer.getFilamentByID(filamentID));
            }
        }
    }

    public PrinterSettings getPrinterSettings()
    {
        return printerSettings;
    }

    public void addModel(ModelContainer modelContainer)
    {
        topLevelModels.add(modelContainer);
        projectModified();
        fireWhenModelAdded(modelContainer);
        addModelListeners(modelContainer);
        for (ModelContainer childModelContainer : modelContainer.getChildModelContainers())
        {
            addModelListeners(childModelContainer);
        }
    }

    private void fireWhenModelAdded(ModelContainer modelContainer)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelAdded(modelContainer);
        }
    }

    private void fireWhenPrinterSettingsChanged(PrinterSettings printerSettings)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenPrinterSettingsChanged(printerSettings);
        }
    }

    public void removeModels(Set<ModelContainer> modelContainers)
    {
        
        for (ModelContainer modelContainer : modelContainers)
        {
            assert modelContainer != null;
        }    
        
        topLevelModels.removeAll(modelContainers);

        for (ModelContainer modelContainer : modelContainers)
        {
            removeModelListeners(modelContainer);
            for (ModelContainer childModelContainer : modelContainer.getChildModelContainers())
            {
                removeModelListeners(childModelContainer);
            }
        }
        projectModified();
        fireWhenModelsRemoved(modelContainers);
    }

    private void fireWhenModelsRemoved(Set<ModelContainer> modelContainers)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelsRemoved(modelContainers);
        }
    }

    public void addProjectChangesListener(ProjectChangesListener projectChangesListener)
    {
        projectChangesListeners.add(projectChangesListener);
    }

    public void removeProjectChangesListener(ProjectChangesListener projectChangesListener)
    {
        projectChangesListeners.remove(projectChangesListener);
    }

    public ObjectProperty<Date> getLastModifiedDate()
    {
        return lastModifiedDate;
    }

    private ChangeListener<Number> modelExtruderNumberListener;

    private void addModelListeners(ModelContainer modelContainer)
    {
        // XXX this is a bug, modelExtruderNumberListener is being overridden for each model
        modelExtruderNumberListener
            = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                fireWhenModelChanged(modelContainer, ASSOCIATE_WITH_EXTRUDER_NUMBER);
                modelColourChanged.set(!modelColourChanged.get());
            };
        modelContainer.getAssociateWithExtruderNumberProperty().addListener(
            modelExtruderNumberListener);
    }

    private void removeModelListeners(ModelContainer modelContainer)
    {
        modelContainer.getAssociateWithExtruderNumberProperty().removeListener(
            modelExtruderNumberListener);
    }

    public BooleanProperty canPrintProperty()
    {
        return canPrint;
    }

    public BooleanProperty customSettingsNotChosenProperty()
    {
        return customSettingsNotChosen;
    }

    /**
     * Create a new group from models that are not yet in the project.
     */
    public ModelGroup createNewGroup(Set<ModelContainer> modelContainers)
    {
        checkNotAlreadyInGroup(modelContainers);
        
        ModelGroup modelGroup = new ModelGroup(modelContainers);
        modelGroup.checkOffBed();
        modelGroup.recalculateScreenExtents();
        return modelGroup;
    }

    private void checkNotAlreadyInGroup(Set<ModelContainer> modelContainers)
    {
        Set<ModelContainer> modelsAlreadyInGroups = getDescendentModelsInAllGroups();
        for (ModelContainer model : modelContainers)
        {
            if (modelsAlreadyInGroups.contains(model)) {
                throw new RuntimeException("Model " + model + " is already in a group");
            }
        }
    }

    /**
     * Create a new group from models that are not yet in the project, and add model listeners to
     * all descendent children.
     */
    public ModelGroup createNewGroupAndAddModelListeners(Set<ModelContainer> modelContainers)
    {
        checkNotAlreadyInGroup(modelContainers);
        ModelGroup modelGroup = new ModelGroup(modelContainers);
        addModelListeners(modelGroup);
        for (ModelContainer childModelContainer : modelGroup.getDescendentModelContainers())
        {
            addModelListeners(childModelContainer);
        }
        modelGroup.checkOffBed();
        return modelGroup;
    }

    /**
     * Create a new group from models that are not yet in the project.
     */
    public ModelGroup createNewGroup(Set<ModelContainer> modelContainers, int groupModelId)
    {
        checkNotAlreadyInGroup(modelContainers);
        ModelGroup modelGroup = new ModelGroup(modelContainers, groupModelId);
        modelGroup.checkOffBed();
        return modelGroup;
    }

    public ModelGroup group(Set<ModelContainer> modelContainers)
    {
        removeModels(modelContainers);
        ModelGroup modelGroup = createNewGroup(modelContainers);
        addModel(modelGroup);
        return modelGroup;
    }

    public ModelGroup group(Set<ModelContainer> modelContainers, int groupModelId)
    {
        removeModels(modelContainers);
        ModelGroup modelGroup = createNewGroup(modelContainers, groupModelId);
        addModel(modelGroup);
        return modelGroup;
    }

    public void ungroup(Set<? extends ModelContainer> modelContainers)
    {
        for (ModelContainer modelContainer : modelContainers)
        {
            if (modelContainer instanceof ModelGroup)
            {
                ModelGroup modelGroup = (ModelGroup) modelContainer;
                Set<ModelContainer> modelGroups = new HashSet<>();
                modelGroups.add(modelGroup);
                removeModels(modelGroups);
                for (ModelContainer childModelContainer : modelGroup.getChildModelContainers())
                {
                    addModel(childModelContainer);
                    childModelContainer.setBedCentreOffsetTransform();
//                    childModelContainer.applyGroupTransformToThis(modelGroup);
                    childModelContainer.checkOffBed();
                }
            }
        }
    }

    private Set<ModelContainer> getModelsHoldingMeshViews()
    {
        Set<ModelContainer> modelsHoldingMeshViews = new HashSet<>();
        for (ModelContainer model : topLevelModels)
        {
            modelsHoldingMeshViews.addAll(model.getModelsHoldingMeshViews());
        }
        return modelsHoldingMeshViews;
    }

    private Set<ModelContainer> getModelsHoldingModels()
    {
        Set<ModelContainer> modelsHoldingMeshViews = new HashSet<>();
        for (ModelContainer model : topLevelModels)
        {
            modelsHoldingMeshViews.addAll(model.getModelsHoldingModels());
        }
        return modelsHoldingMeshViews;
    }
    
    /**
     * Return the set of those ModelContainers which are in any group.
     */
    private Set<ModelContainer> getDescendentModelsInAllGroups()
    {
        Set<ModelContainer> modelsInGroups = new HashSet<>();
        for (ModelContainer model : topLevelModels)
        {
            if (model instanceof ModelGroup) {
                modelsInGroups.addAll(getDescendentModelsInGroup((ModelGroup) model));
            } 
        }
        return modelsInGroups;
    }
    
    /**
     * Return the set of those ModelContainers which are in any group descending from the
     * given group.
     */
    private Set<ModelContainer> getDescendentModelsInGroup(ModelGroup modelGroup)
    {
        Set<ModelContainer> modelsInGroups = new HashSet<>();
        for (ModelContainer model : modelGroup.getChildModelContainers())
        {
            if (model instanceof ModelGroup) {
                modelsInGroups.addAll(getDescendentModelsInGroup((ModelGroup) model));
            } else {
                modelsInGroups.add(model);
            }
        }
        return modelsInGroups;
    }

    /**
     * Return a Map of child_model_id -> parent_model_id for all model:group and group:group
     * relationships.
     */
    public Map<Integer, Set<Integer>> getGroupStructure()
    {
        Map<Integer, Set<Integer>> groupStructure = new HashMap<>();
        for (ModelContainer modelContainer : getModelsHoldingModels())
        {
            modelContainer.addGroupStructure(groupStructure);
        }
        return groupStructure;
    }

    /**
     * Return a Map of model_id -> state for all models holding models (ie groups).
     */
    public Map<Integer, ModelContainer.State> getGroupState()
    {
        Map<Integer, ModelContainer.State> groupState = new HashMap<>();
        for (ModelContainer modelContainer : getModelsHoldingModels())
        {
            groupState.put(modelContainer.getModelId(), modelContainer.getState());
        }
        return groupState;
    }

    /**
     * Using the group function, reapply the groupings as given by the groupStructure. The first
     * groups to be created must be those containing only non-groups, and then each level of the
     * group hierarchy.<p>
     * First create new groups where all children are already instantiated. Then repeat until no new
     * groups are created.
     * </p>
     */
    public void recreateGroups(Map<Integer, Set<Integer>> groupStructure,
        Map<Integer, ModelContainer.State> groupStates) throws ProjectLoadException
    {
        int numNewGroups;
        do
        {
            numNewGroups = makeNewGroups(groupStructure, groupStates);
        } while (numNewGroups > 0);
    }

    /**
     * Create groups where all the children are already instantiated, based on the structure
     * and state given in the parameters.
     *
     * @return the number of groups created
     */
    private int makeNewGroups(Map<Integer, Set<Integer>> groupStructure,
        Map<Integer, ModelContainer.State> groupStates) throws ProjectLoadException
    {
        int numGroups = 0;
        for (Map.Entry<Integer, Set<Integer>> entry : groupStructure.entrySet())
        {
            if (allModelsInstantiated(entry.getValue()))
            {
                Set<ModelContainer> modelContainers = getModelContainersOfIds(entry.getValue());
                int groupModelId = entry.getKey();
                ModelGroup group = group(modelContainers, groupModelId);
                recreateGroupState(group, groupStates);
                numGroups++;
            }
        }
        return numGroups;
    }

    /**
     * Return true if loadedModels contains models for all the given modelIds, else return false.
     */
    private boolean allModelsInstantiated(Set<Integer> modelIds)
    {
        for (int modelId : modelIds)
        {
            boolean modelFound = false;
            for (ModelContainer modelContainer : topLevelModels)
            {
                if (modelContainer.getModelId() == modelId)
                {
                    modelFound = true;
                    break;
                }

            }
            if (!modelFound)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Return the set of models for the given set of modelIds.
     */
    public Set<ModelContainer> getModelContainersOfIds(Set<Integer> modelIds) throws ProjectLoadException
    {
        Set<ModelContainer> modelContainers = new HashSet<>();
        for (int modelId : modelIds)
        {
            Optional<ModelContainer> modelContainer = getModelContainerOfModelId(modelId);
            if (modelContainer.isPresent())
            {
                modelContainers.add(modelContainer.get());
            } else
            {
                throw new ProjectLoadException("unexpected model id when recreating groups");
            }
        }
        return modelContainers;
    }

    private Optional<ModelContainer> getModelContainerOfModelId(int modelId)
    {
        for (ModelContainer modelContainer : topLevelModels)
        {
            if (modelContainer.getModelId() == modelId)
            {
                return Optional.of(modelContainer);
            }
        }
        return Optional.empty();

    }

    /**
     * Update the transforms of the given group as indicated by groupState.
     */
    private void recreateGroupState(ModelGroup group, Map<Integer, ModelContainer.State> groupStates) throws ProjectLoadException
    {
        group.setState(groupStates.get(group.getModelId()));
        group.checkOffBed();
    }


    /**
     * ProjectChangesListener allows other objects to observe when models are added or removed etc
     * to the project.
     */
    public interface ProjectChangesListener
    {

        /**
         * This should be fired when a model is added to the project.
         */
        void whenModelAdded(ModelContainer modelContainer);

        /**
         * This should be fired when a model is removed from the project.
         */
        void whenModelsRemoved(Set<ModelContainer> modelContainers);

        /**
         * This should be fired when the project is auto laid out.
         */
        void whenAutoLaidOut();

        /**
         * This should be fired when one or more models have been moved, rotated or scaled etc. If
         * possible try to fire just once for any given group change.
         */
        void whenModelsTransformed(Set<ModelContainer> modelContainers);

        /**
         * This should be fired when certain details of the model change. Currently this is only: -
         * associatedExtruder
         */
        void whenModelChanged(ModelContainer modelContainer, String propertyName);

        /**
         * This should be fired whenever the PrinterSettings of the project changes.
         *
         * @param printerSettings
         */
        void whenPrinterSettingsChanged(PrinterSettings printerSettings);
    }

    public void autoLayout()
    {
        Collections.sort(topLevelModels);
        PackingThing thing = new PackingThing((int) PrintBed.maxPrintableXSize,
                                              (int) PrintBed.maxPrintableZSize);

        thing.reference(topLevelModels, 10);
        thing.pack();
        thing.relocateBlocks();

        projectModified();
        fireWhenAutoLaidOut();
    }

    private void fireWhenAutoLaidOut()
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenAutoLaidOut();
        }
    }

    /**
     * Scale X, Y and Z by the given factor, apply the given ratio to the given scale. I.e. the
     * ratio is not an absolute figure to be applied to the models but a ratio to be applied to the
     * current scale.
     */
    public void scaleXYZRatioSelection(Set<ModelContainer> modelContainers, double ratio)
    {
        for (ModelContainer model : modelContainers)
        {
            model.setXScale(model.getXScale() * ratio);
            model.setYScale(model.getYScale() * ratio);
            model.setZScale(model.getZScale() * ratio);
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void scaleXModels(Set<ModelContainer> modelContainers, double newScale,
        boolean preserveAspectRatio)
    {
        if (preserveAspectRatio)
        {
            // this only happens for non-multiselect
            assert (modelContainers.size() == 1);
            ModelContainer model = modelContainers.iterator().next();
            double ratio = newScale / model.getXScale();
            scaleXYZRatioSelection(modelContainers, ratio);
        } else
        {
            for (ModelContainer model : modelContainers)
            {
                {
                    model.setXScale(newScale);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void scaleYModels(Set<ModelContainer> modelContainers, double newScale,
        boolean preserveAspectRatio)
    {
        if (preserveAspectRatio)
        {
            // this only happens for non-multiselect
            assert (modelContainers.size() == 1);
            ModelContainer model = modelContainers.iterator().next();
            double ratio = newScale / model.getYScale();
            scaleXYZRatioSelection(modelContainers, ratio);
        } else
        {
            for (ModelContainer model : modelContainers)
            {
                {
                    model.setYScale(newScale);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void scaleZModels(Set<ModelContainer> modelContainers, double newScale,
        boolean preserveAspectRatio)
    {
        if (preserveAspectRatio)
        {
            // this only happens for non-multiselect
            assert (modelContainers.size() == 1);
            ModelContainer model = modelContainers.iterator().next();
            double ratio = newScale / model.getZScale();
            scaleXYZRatioSelection(modelContainers, ratio);
        } else
        {
            for (ModelContainer model : modelContainers)
            {
                {
                    model.setZScale(newScale);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void rotateLeanModels(Set<ModelContainer> modelContainers, double rotation)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.setRotationLean(rotation);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void rotateTwistModels(Set<ModelContainer> modelContainers, double rotation)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.setRotationTwist(rotation);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void rotateTurnModels(Set<ModelContainer> modelContainers, double rotation)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.setRotationTurn(rotation);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void dropToBed(Set<ModelContainer> modelContainers)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.dropToBed();
                model.checkOffBed();
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void snapToGround(ModelContainer modelContainer, MeshView pickedMesh, int faceNumber)
    {
        modelContainer.snapToGround(pickedMesh, faceNumber);
        projectModified();
        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(modelContainer);
        fireWhenModelsTransformed(modelContainers);
    }

    public void resizeModelsDepth(Set<ModelContainer> modelContainers, double depth)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.resizeDepth(depth);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void resizeModelsHeight(Set<ModelContainer> modelContainers, double height)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.resizeHeight(height);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void resizeModelsWidth(Set<ModelContainer> modelContainers, double width)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.resizeWidth(width);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void translateModelsBy(Set<ModelContainer> modelContainers, double x, double z)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.translateBy(x, z);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    private void fireWhenModelsTransformed(Set<ModelContainer> modelContainers)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelsTransformed(modelContainers);
        }
    }

    private void fireWhenModelChanged(ModelContainer modelContainer, String propertyName)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelChanged(modelContainer, propertyName);
        }
    }

    public void translateModelsXTo(Set<ModelContainer> modelContainers, double x)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.translateXTo(x);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void translateModelsZTo(Set<ModelContainer> modelContainers, double z)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.translateZTo(z);
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public void setUseExtruder0Filament(ModelContainer modelContainer, boolean useExtruder0)
    {
        modelContainer.setUseExtruder0(useExtruder0);
        projectModified();
    }

    public Set<ModelContainer.State> getModelStates()
    {
        Set<ModelContainer.State> modelStates = new HashSet<>();
        for (ModelContainer model : topLevelModels)
        {
            modelStates.add(model.getState());
        }
        return modelStates;
    }

    public void setModelStates(Set<ModelContainer.State> modelStates)
    {
        Set<ModelContainer> modelContainers = new HashSet<>();
        for (ModelContainer.State modelState : modelStates)
        {
            for (ModelContainer model : getAllModels())
            {
                if (model.getModelId() == modelState.modelId)
                {
                    model.setState(modelState);
                    modelContainers.add(model);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }
}
