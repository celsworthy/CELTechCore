/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.visualisation;

import celtech.Lookup;
import celtech.appManager.ModelContainerProject;
import celtech.appManager.Project;
import celtech.appManager.ProjectCallback;
import celtech.appManager.ProjectMode;
import celtech.appManager.SVGProject;
import celtech.appManager.undo.UndoableProject;
import celtech.roboxbase.configuration.PrintBed;
import celtech.coreUI.visualisation.metaparts.ModelLoadResult;
import celtech.coreUI.visualisation.metaparts.ModelLoadResultType;
import celtech.modelcontrol.Groupable;
import celtech.roboxbase.utils.RectangularBounds;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.services.modelLoader.ModelLoadResults;
import celtech.services.modelLoader.ModelLoaderService;
import celtech.utils.threed.MeshUtils;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * ModelLoader contains methods for loading models from a file.
 *
 * @author tony
 */
public class ModelLoader
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
            ModelLoader.class.getName());
    /*
     * Mesh Model loading
     */
    public static final ModelLoaderService modelLoaderService = new ModelLoaderService();

    private void offerShrinkAndAddToProject(Project project, boolean relayout, ProjectCallback callMeBack)
    {
        ModelLoadResults loadResults = modelLoaderService.getValue();
        if (loadResults.getResults().isEmpty())
        {
            return;
        }

        if (loadResults.getType() == ModelLoadResultType.Mesh)
        {
            project.setMode(ProjectMode.MESH);
            // validate incoming meshes
            // Associate the loaded meshes with extruders in turn, respecting the original groups / model files
            int numExtruders = 1;

            Printer selectedPrinter = Lookup.getSelectedPrinterProperty().get();
            if (selectedPrinter != null)
            {
                numExtruders = selectedPrinter.extrudersProperty().size();
            }

            int currentExtruder = 0;

            for (ModelLoadResult loadResult : loadResults.getResults())
            {
                Set<ModelContainer> modelContainers = (Set) loadResult.getProjectifiableThings();
                Set<String> invalidModelNames = new HashSet<>();
                for (ModelContainer modelContainer : modelContainers)
                {
                    Optional<MeshUtils.MeshError> error = MeshUtils.validate((TriangleMesh) modelContainer.getMeshView().getMesh());
                    if (error.isPresent())
                    {
                        invalidModelNames.add(modelContainer.getModelName());
                        modelContainer.setIsInvalidMesh(true);
                    }

                    //Assign the models incrementally to the extruders
                    modelContainer.getAssociateWithExtruderNumberProperty().set(currentExtruder);
                }

                if (currentExtruder < numExtruders - 1)
                {
                    currentExtruder++;
                } else
                {
                    currentExtruder = 0;
                }

                if (!invalidModelNames.isEmpty())
                {
                    boolean load
                            = BaseLookup.getSystemNotificationHandler().showModelIsInvalidDialog(invalidModelNames);
                    if (!load)
                    {
                        return;
                    }
                }
            }

            boolean projectIsEmpty = project.getNumberOfProjectifiableElements() == 0;
            Set<ModelContainer> allModelContainers = new HashSet<>();
            boolean shouldCentre = loadResults.isShouldCentre();

            for (ModelLoadResult loadResult : loadResults.getResults())
            {
                if (loadResult != null)
                {
                    Set<ModelContainer> modelContainersToOperateOn = (Set) loadResult.getProjectifiableThings();
                    if (Lookup.getUserPreferences().isLoosePartSplitOnLoad())
                    {
                        allModelContainers.add(makeGroup(modelContainersToOperateOn));
                    } else
                    {
                        allModelContainers.addAll(modelContainersToOperateOn);
                    }
                } else
                {
                    steno.error("Error whilst attempting to load model");
                }
            }
            Set<ProjectifiableThing> allProjectifiableThings = (Set) allModelContainers;

            addToProject(project, allProjectifiableThings, shouldCentre);
            if (relayout && projectIsEmpty && loadResults.getResults().size() > 1)
            {
//            project.autoLayout();
            }
        } else if (loadResults.getType() == ModelLoadResultType.SVG)
        {

            Set<ProjectifiableThing> allProjectifiableThings = new HashSet<>();
            for (ModelLoadResult result : loadResults.getResults())
            {
                allProjectifiableThings.addAll(result.getProjectifiableThings());
            }

            addToProject(project, allProjectifiableThings, false);

            project.setMode(ProjectMode.SVG);
        }
        
        if (project != null
                && callMeBack != null)
        {
            callMeBack.heresTheProject(project);
        }
    }

    public ReadOnlyBooleanProperty modelLoadingProperty()
    {
        return modelLoaderService.runningProperty();
    }

    /**
     * Load each model in modelsToLoad, do not lay them out on the bed.
     *
     * @param project
     * @param modelsToLoad
     * @param callMeBack
     */
    public void loadExternalModels(Project project, List<File> modelsToLoad, ProjectCallback callMeBack)
    {
        loadExternalModels(project, modelsToLoad, false, callMeBack);
    }

    /**
     * Load each model in modelsToLoad and relayout if requested. If there are
     * already models loaded in the project then do not relayout even if
     * relayout=true;
     *
     * @param project
     * @param modelsToLoad
     * @param relayout
     * @param callMeBack
     */
    public void loadExternalModels(Project project, List<File> modelsToLoad, boolean relayout, ProjectCallback callMeBack)
    {
        modelLoaderService.reset();
        modelLoaderService.setModelFilesToLoad(modelsToLoad);
        modelLoaderService.setOnSucceeded((WorkerStateEvent t) ->
        {
            Project projectToUse = null;

            if (project == null)
            {
                ModelLoadResults loadResults = modelLoaderService.getValue();
                if (!loadResults.getResults().isEmpty())
                {
                    switch (loadResults.getType())
                    {
                        case Mesh:
                            projectToUse = new ModelContainerProject();
                            break;
                        case SVG:
                            projectToUse = new SVGProject();
                            break;
                    }
                }
            } else
            {
                projectToUse = project;
            }
            offerShrinkAndAddToProject(projectToUse, relayout, callMeBack);
        });
        modelLoaderService.start();
    }

    /**
     * Add the given ModelContainers to the project. Some may be ModelGroups. If
     * there is more than one ModelContainer/Group then put them in one
     * overarching group.
     */
    private void addToProject(Project project, Set<ProjectifiableThing> modelContainers, boolean shouldCentre)
    {
        UndoableProject undoableProject = new UndoableProject(project);

        ProjectifiableThing projectifiableThing = null;

        if (project instanceof ModelContainerProject)
        {
            ModelContainer modelContainer;

            if (modelContainers.size() == 1)
            {
                modelContainer = (ModelContainer) modelContainers.iterator().next();
            } else
            {
                Set<Groupable> thingsToGroup = (Set) modelContainers;
                modelContainer = ((ModelContainerProject) project).createNewGroupAndAddModelListeners(thingsToGroup);
            }
            if (shouldCentre)
            {
                modelContainer.moveToCentre();
                modelContainer.dropToBed();
            }
            shrinkIfRequested(modelContainer);
            modelContainer.checkOffBed();
            projectifiableThing = modelContainer;
        } else
        {
            projectifiableThing = modelContainers.iterator().next();
        }

        undoableProject.addModel(projectifiableThing);
    }

    private void shrinkIfRequested(ModelContainer modelContainer)
    {
        boolean shrinkModel = false;
        RectangularBounds originalBounds = modelContainer.getOriginalModelBounds();
        boolean modelIsTooLarge = PrintBed.isBiggerThanPrintVolume(originalBounds);
        if (modelIsTooLarge)
        {
            shrinkModel = BaseLookup.getSystemNotificationHandler().
                    showModelTooBigDialog(modelContainer.getModelName());
        }
        if (shrinkModel)
        {
            modelContainer.shrinkToFitBed();
        }
    }

    private ModelContainer makeGroup(Set<ModelContainer> modelContainers)
    {
        Set<ModelContainer> splitModelContainers = new HashSet<>();
        for (ModelContainer modelContainer : modelContainers)
        {
            try
            {
                ModelContainer splitContainerOrGroup = modelContainer.splitIntoParts();
                splitModelContainers.add(splitContainerOrGroup);
            } catch (StackOverflowError ex)
            {
                splitModelContainers.add(modelContainer);
            }
        }
        if (splitModelContainers.size() == 1)
        {
            return splitModelContainers.iterator().next();
        } else
        {
            return new ModelGroup(splitModelContainers);
        }
    }

}
