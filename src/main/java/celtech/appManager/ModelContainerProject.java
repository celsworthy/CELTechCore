package celtech.appManager;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.fileRepresentation.ModelContainerProjectFile;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.PrintBed;
import celtech.roboxbase.configuration.datafileaccessors.FilamentContainer;
import celtech.configuration.fileRepresentation.ProjectFile;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.printerControl.model.Head.HeadType;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.utils.Math.packing.PackableItem;
import celtech.roboxbase.utils.Math.packing.PackingThing;
import celtech.utils.threed.MeshUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.SortedList;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ModelContainerProject extends Project
{

    private int version = -1;

    private Filament DEFAULT_FILAMENT;

    private static final String ASSOCIATE_WITH_EXTRUDER_NUMBER = "associateWithExtruderNumber";

    private static final Stenographer steno = StenographerFactory.getStenographer(ModelContainerProject.class.getName());

    private ObjectProperty<Filament> extruder0Filament;
    private ObjectProperty<Filament> extruder1Filament;
    private BooleanProperty modelColourChanged;
    private BooleanBinding hasInvalidMeshes;

    private FilamentContainer filamentContainer;

    public ModelContainerProject()
    {
        super();
    }

    @Override
    protected void initialise()
    {
        hasInvalidMeshes = new BooleanBinding()
        {
            {
                super.bind(topLevelThings);
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
        filamentContainer = BaseLookup.getFilamentContainer();
        DEFAULT_FILAMENT = filamentContainer.getFilamentByID("RBX-ABS-GR499");

        initialiseExtruderFilaments();
    }

    public Set<ModelContainer> getModelContainersWithInvalidMesh()
    {
        Set<ModelContainer> invalidModelContainers = new HashSet<>();
        getAllModels().stream().map(ModelContainer.class::cast).filter((modelContainer)
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

    protected void load(ProjectFile projectFile, String basePath) throws ProjectLoadException
    {
        suppressProjectChanged = true;

        if (projectFile instanceof ModelContainerProjectFile)
        {
            ModelContainerProjectFile mcProjectFile = (ModelContainerProjectFile)projectFile;
            try
            {
                version = projectFile.getVersion();

                projectNameProperty.set(projectFile.getProjectName());
                lastModifiedDate.set(projectFile.getLastModifiedDate());
                lastPrintJobID = projectFile.getLastPrintJobID();

                String filamentID0 = mcProjectFile.getExtruder0FilamentID();
                String filamentID1 = mcProjectFile.getExtruder1FilamentID();
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

                printerSettings.setSettingsName(mcProjectFile.getSettingsName());
                printerSettings.setPrintQuality(mcProjectFile.getPrintQuality());
                printerSettings.setBrimOverride(mcProjectFile.getBrimOverride());
                printerSettings.setFillDensityOverride(mcProjectFile.getFillDensityOverride());
                printerSettings.setPrintSupportOverride(mcProjectFile.getPrintSupportOverride());
                printerSettings.setPrintSupportTypeOverride(mcProjectFile.getPrintSupportTypeOverride());
                printerSettings.setRaftOverride(mcProjectFile.getPrintRaft());

                loadModels(basePath);

                recreateGroups(mcProjectFile.getGroupStructure(), mcProjectFile.getGroupState());

            } catch (IOException ex)
            {
                steno.exception("Failed to load project " + basePath, ex);
            } catch (ClassNotFoundException ex)
            {
                steno.exception("Failed to load project " + basePath, ex);
            }
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

    public static void saveProject(ModelContainerProject project)
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

    @Override
    protected void save(String basePath)
    {
        if (topLevelThings.size() > 0)
        {
            try
            {
                ProjectFile projectFile = new ModelContainerProjectFile();
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
    public boolean allModelsOnSameExtruder(Printer printer)
    {
        return getUsedExtruders(printer).size() < 2;
    }

    private void getUsedExtruders(ModelContainer modelContainer, Set<Integer> usedExtruders, Printer printer)
    {
        if (modelContainer instanceof ModelGroup)
        {
            for (ModelContainer subModel : ((ModelGroup) modelContainer).getChildModelContainers())
            {
                getUsedExtruders(subModel, usedExtruders, printer);
            }
        } else
        {
            if (printer != null
                    && printer.headProperty().get() != null)
            {
                //Single material heads can only use 1 material
                if (printer.headProperty().get().headTypeProperty().get() == HeadType.SINGLE_MATERIAL_HEAD)
                {
                    usedExtruders.add(0);
                } else if (printer.headProperty().get().headTypeProperty().get() == HeadType.DUAL_MATERIAL_HEAD)
                {
                    usedExtruders.add(modelContainer.getAssociateWithExtruderNumberProperty().get());
                }
            } else
            {
                usedExtruders.add(modelContainer.getAssociateWithExtruderNumberProperty().get());
            }
        }
    }

    /**
     * Return which extruders are used by the project, as a set of the extruder
     * numbers.
     *
     * @param printer
     * @return
     */
    public Set<Integer> getUsedExtruders(Printer printer)
    {
        Set<Integer> usedExtruders = new HashSet<>();
        for (ProjectifiableThing loadedModel : topLevelThings)
        {
            getUsedExtruders((ModelContainer) loadedModel, usedExtruders, printer);
        }

        if (printerSettings.getPrintSupportTypeOverride() == SlicerParametersFile.SupportType.MATERIAL_1)
        {
            if (!usedExtruders.contains(0))
            {
                usedExtruders.add(0);
            }
        } else if (printerSettings.getPrintSupportTypeOverride() == SlicerParametersFile.SupportType.MATERIAL_2)
        {
            if (!usedExtruders.contains(1))
            {
                usedExtruders.add(1);
            }
        }
        return usedExtruders;
    }

    /**
     * Return all ModelGroups and ModelContainers within the project.
     *
     * @return
     */
    @Override
    public Set<ProjectifiableThing> getAllModels()
    {
        Set<ProjectifiableThing> allModelContainers = new HashSet<>();
        for (ProjectifiableThing loadedModel : topLevelThings)
        {
            allModelContainers.add(loadedModel);
            allModelContainers.addAll(((ModelContainer) loadedModel).getDescendentModelContainers());
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

    public String getLastPrintJobID()
    {
        return lastPrintJobID;
    }

    public ReadOnlyBooleanProperty getModelColourChanged()
    {
        return modelColourChanged;
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
     * For new projects this should be called to initialise the extruder
     * filaments according to the currently selected printer.
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

    @Override
    public void addModel(ProjectifiableThing projectifiableThing)
    {
        if (projectifiableThing instanceof ModelContainer)
        {
            ModelContainer modelContainer = (ModelContainer) projectifiableThing;
            topLevelThings.add(modelContainer);
            projectModified();
            fireWhenModelAdded(modelContainer);
            addModelListeners(modelContainer);
            for (ModelContainer childModelContainer : modelContainer.getChildModelContainers())
            {
                addModelListeners(childModelContainer);
            }
        }
    }

    private void fireWhenModelAdded(ModelContainer modelContainer)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelAdded(modelContainer);
        }
    }

    @Override
    protected void fireWhenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenPrinterSettingsChanged(printerSettings);
        }
    }

    @Override
    public void removeModels(Set<ProjectifiableThing> projectifiableThings)
    {
        Set<ModelContainer> modelContainers = (Set) projectifiableThings;

        for (ModelContainer modelContainer : modelContainers)
        {
            assert modelContainer != null;
        }

        topLevelThings.removeAll(modelContainers);

        for (ModelContainer modelContainer : modelContainers)
        {
            removeModelListeners(modelContainer);
            for (ModelContainer childModelContainer : modelContainer.getChildModelContainers())
            {
                removeModelListeners(childModelContainer);
            }
        }
        projectModified();
        fireWhenModelsRemoved(projectifiableThings);
    }

    private void fireWhenModelsRemoved(Set<ProjectifiableThing> modelContainers)
    {
        for (ProjectChangesListener projectChangesListener : projectChangesListeners)
        {
            projectChangesListener.whenModelsRemoved(modelContainers);
        }
    }

    private ChangeListener<Number> modelExtruderNumberListener;

    private void addModelListeners(ModelContainer modelContainer)
    {
        //TODO XXX this is a bug, modelExtruderNumberListener is being overridden for each model
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

    /**
     * Create a new group from models that are not yet in the project.
     *
     * @param modelContainers
     * @return
     */
    public ModelGroup createNewGroup(Set<ModelContainer> modelContainers)
    {
        checkNotAlreadyInGroup(modelContainers);

        ModelGroup modelGroup = new ModelGroup(modelContainers);
        modelGroup.checkOffBed();
        modelGroup.notifyScreenExtentsChange();
        return modelGroup;
    }

    private void checkNotAlreadyInGroup(Set<ModelContainer> modelContainers)
    {
        Set<ModelContainer> modelsAlreadyInGroups = getDescendentModelsInAllGroups();
        for (ModelContainer model : modelContainers)
        {
            if (modelsAlreadyInGroups.contains(model))
            {
                throw new RuntimeException("Model " + model + " is already in a group");
            }
        }
    }

    /**
     * Create a new group from models that are not yet in the project, and add
     * model listeners to all descendent children.
     *
     * @param modelContainers
     * @return
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
     *
     * @param modelContainers
     * @param groupModelId
     * @return
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
        Set<ProjectifiableThing> projectifiableThings = (Set) modelContainers;

        removeModels(projectifiableThings);
        ModelGroup modelGroup = createNewGroup(modelContainers);
        addModel(modelGroup);
        return modelGroup;
    }

    public ModelGroup group(Set<ModelContainer> modelContainers, int groupModelId)
    {
        Set<ProjectifiableThing> projectifiableThings = (Set) modelContainers;

        removeModels(projectifiableThings);
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
                Set<ProjectifiableThing> modelGroups = new HashSet<>();
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
        for (ProjectifiableThing model : topLevelThings)
        {
            modelsHoldingMeshViews.addAll(((ModelContainer) model).getModelsHoldingMeshViews());
        }
        return modelsHoldingMeshViews;
    }

    private Set<ModelContainer> getModelsHoldingModels()
    {
        Set<ModelContainer> modelsHoldingMeshViews = new HashSet<>();
        for (ProjectifiableThing model : topLevelThings)
        {
            modelsHoldingMeshViews.addAll(((ModelContainer) model).getModelsHoldingModels());
        }
        return modelsHoldingMeshViews;
    }

    /**
     * Return the set of those ModelContainers which are in any group.
     */
    private Set<ModelContainer> getDescendentModelsInAllGroups()
    {
        Set<ModelContainer> modelsInGroups = new HashSet<>();
        for (ProjectifiableThing model : topLevelThings)
        {
            if (model instanceof ModelGroup)
            {
                modelsInGroups.addAll(getDescendentModelsInGroup((ModelGroup) model));
            }
        }
        return modelsInGroups;
    }

    /**
     * Return the set of those ModelContainers which are in any group descending
     * from the given group.
     */
    private Set<ModelContainer> getDescendentModelsInGroup(ModelGroup modelGroup)
    {
        Set<ModelContainer> modelsInGroups = new HashSet<>();
        for (ModelContainer model : modelGroup.getChildModelContainers())
        {
            if (model instanceof ModelGroup)
            {
                modelsInGroups.addAll(getDescendentModelsInGroup((ModelGroup) model));
            } else
            {
                modelsInGroups.add(model);
            }
        }
        return modelsInGroups;
    }

    /**
     * Return a Map of child_model_id -> parent_model_id for all model:group and
     * group:grou
     *
     * @return p relationships.
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
     * Return a Map of model_id -> state for all models holding models (ie
     * groups).
     *
     * @return
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
     * Using the group function, reapply the groupings as given by the
     * groupStructure. The first groups to be created must be those containing
     * only non-groups, and then each level of the group hierarchy.<p>
     * First create new groups where all children are already instantiated. Then
     * repeat until no new groups are created.
     * </p>
     *
     * @param groupStructure
     * @param groupStates
     * @throws celtech.appManager.ModelContainerProject.ProjectLoadException
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
     * Create groups where all the children are already instantiated, based on
     * the structure and state given in the parameters.
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
     * Return true if loadedModels contains models for all the given modelIds,
     * else return false.
     */
    private boolean allModelsInstantiated(Set<Integer> modelIds)
    {
        for (int modelId : modelIds)
        {
            boolean modelFound = false;
            for (ProjectifiableThing modelContainer : topLevelThings)
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
     *
     * @param modelIds
     * @return
     * @throws celtech.appManager.ModelContainerProject.ProjectLoadException
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
        for (ProjectifiableThing modelContainer : topLevelThings)
        {
            if (modelContainer.getModelId() == modelId)
            {
                return Optional.of((ModelContainer) modelContainer);
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

    @Override
    public void autoLayout()
    {
        List<PackableItem> sortedPackables = new ArrayList<>();

        SortedList<ProjectifiableThing> sortedContainers = topLevelThings.sorted();

        sortedContainers.stream().forEach(model ->
        {
            sortedPackables.add((PackableItem) model);
        });

        PackingThing thing = new PackingThing((int) PrintBed.maxPrintableXSize,
                (int) PrintBed.maxPrintableZSize);

        thing.reference(sortedPackables, 10);
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

    public void rotateLeanModels(Set<ModelContainer> modelContainers, double rotation)
    {
        for (ModelContainer model : modelContainers)
        {
            {
                model.setRotationLean(rotation);
            }
        }
        projectModified();

        Set<ProjectifiableThing> projectifiableThings = (Set) modelContainers;
        fireWhenModelsTransformed(projectifiableThings);
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

        Set<ProjectifiableThing> projectifiableThings = (Set) modelContainers;
        fireWhenModelsTransformed(projectifiableThings);
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

        Set<ProjectifiableThing> projectifiableThings = (Set) modelContainers;
        fireWhenModelsTransformed(projectifiableThings);
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

        Set<ProjectifiableThing> projectifiableThings = (Set) modelContainers;
        fireWhenModelsTransformed(projectifiableThings);
    }

    public void snapToGround(ModelContainer modelContainer, MeshView pickedMesh, int faceNumber)
    {
        modelContainer.snapToGround(pickedMesh, faceNumber);
        projectModified();
        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(modelContainer);

        Set<ProjectifiableThing> projectifiableThings = (Set) modelContainers;
        fireWhenModelsTransformed(projectifiableThings);
    }

    @Override
    protected void fireWhenModelsTransformed(Set<ProjectifiableThing> modelContainers)
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

    public void setUseExtruder0Filament(ModelContainer modelContainer, boolean useExtruder0)
    {
        modelContainer.setUseExtruder0(useExtruder0);

        boolean usingDifferentExtruders = false;
        int lastExtruder = -1;
        for (ProjectifiableThing projectifiableThing : getAllModels())
        {
            ModelContainer model = (ModelContainer) projectifiableThing;
            int thisExtruder = model.getAssociateWithExtruderNumberProperty().get();
            if (lastExtruder >= 0
                    && lastExtruder != thisExtruder)
            {
                usingDifferentExtruders = true;
                break;
            }
            lastExtruder = thisExtruder;
        }

        if (!usingDifferentExtruders)
        {
            printerSettings.getPrintSupportTypeOverrideProperty().set(
                    (modelContainer.getAssociateWithExtruderNumberProperty().get() == 0)
                            ? SlicerParametersFile.SupportType.MATERIAL_1
                            : SlicerParametersFile.SupportType.MATERIAL_2);
            fireWhenPrinterSettingsChanged(printerSettings);
        }

        projectModified();
    }
}