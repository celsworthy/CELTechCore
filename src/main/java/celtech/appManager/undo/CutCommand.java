/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.utils.threed.MeshCutter2;
import celtech.utils.threed.MeshDebug;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;


/**
 *
 * @author tony
 */
public class CutCommand extends Command
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CutCommand.class.getName());

    final Project project;
    final float cutHeightValue;
    final Set<ModelContainer> modelContainers;
    final Set<ModelContainer> childModelContainers = new HashSet<>();
    boolean cutWorked = false;

    public CutCommand(Project project, Set<ModelContainer> modelContainers, float cutHeightValue)
    {
        this.project = project;
        this.cutHeightValue = cutHeightValue;
        this.modelContainers = modelContainers;
    }

    @Override
    public void do_()
    {
        multiCut(modelContainers, cutHeightValue);
    }

    @Override
    public void undo()
    {
        if (cutWorked)
        {
            for (ModelContainer modelContainer : modelContainers)
            {
                project.addModel(modelContainer);
            }
            project.removeModels(childModelContainers);
        }
    }

    @Override
    public void redo()
    {
        if (cutWorked)
        {
            for (ModelContainer modelContainer : childModelContainers)
            {
                project.addModel(modelContainer);
            }
            project.removeModels(modelContainers);
        }
    }

    @Override
    public boolean canMergeWith(Command command)
    {
        return false;
    }

    @Override
    public void merge(Command command)
    {
        throw new UnsupportedOperationException("Should never be called");
    }

    private void multiCut(Set<ModelContainer> modelContainers, float cutHeightValue)
    {
        try
        {
            for (ModelContainer modelContainer : modelContainers)
            {
                childModelContainers.addAll(cut(modelContainer, cutHeightValue));
            }
            for (ModelContainer childModelContainer : childModelContainers)
            {
                project.addModel(childModelContainer);
            }
            project.removeModels(modelContainers);
            cutWorked = true;
        } catch (Exception ex)
        {
            cutWorked = false;
            steno.exception("an error occurred during cutting ", ex);
            Lookup.getSystemNotificationHandler().showErrorNotification(
                Lookup.i18n("cutOperation.title"), Lookup.i18n("cutOperation.message"));
        }
    }

    private List<ModelContainer> cut(ModelContainer modelContainer, float cutHeightValue)
    {
        List<ModelContainer> childModelContainers = new ArrayList<>();

        if (modelContainer instanceof ModelGroup)
        {
            cutGroup(modelContainer, cutHeightValue, childModelContainers);
        } else
        {
            cutSingleModel(modelContainer, cutHeightValue, childModelContainers);
        }
        return childModelContainers;
    }

    private void cutSingleModel(ModelContainer modelContainer, float cutHeightValue,
        List<ModelContainer> childModelContainers)
    {
        List<Optional<ModelContainer>> modelContainerPair = cutModelContainerAtHeight(modelContainer,
                                                                                      cutHeightValue);
        Optional<ModelContainer> topModelContainer = modelContainerPair.get(0);
        Optional<ModelContainer> bottomModelContainer = modelContainerPair.get(1);
        if (bottomModelContainer.isPresent())
        {
            bottomModelContainer.get().moveToCentre();
            bottomModelContainer.get().dropToBed();
            topModelContainer.get().translateBy(-20, -20);
            childModelContainers.add(bottomModelContainer.get());
        } 
        if (topModelContainer.isPresent())
        {
            topModelContainer.get().moveToCentre();
            topModelContainer.get().dropToBed();
            topModelContainer.get().translateBy(20, 20);
            childModelContainers.add(topModelContainer.get());
        }
    }

    private void cutGroup(ModelContainer modelContainer, float cutHeightValue,
        List<ModelContainer> childModelContainers)
    {
        ModelGroup modelGroup = (ModelGroup) modelContainer;
        Set<ModelContainer> topModelContainers = new HashSet<>();
        Set<ModelContainer> bottomModelContainers = new HashSet<>();
        for (ModelContainer descendentModelContainer : modelGroup.getModelsHoldingMeshViews())
        {
            List<Optional<ModelContainer>> modelContainerPair = cutModelContainerAtHeight(descendentModelContainer,
                                                                                          cutHeightValue);
            if (modelContainerPair.get(0).isPresent())
            {
                topModelContainers.add(modelContainerPair.get(0).get());
            }
            if (modelContainerPair.get(1).isPresent())
            {
                bottomModelContainers.add(modelContainerPair.get(1).get());
            }
        }
        ModelGroup topGroup = project.createNewGroupAndAddModelListeners(
            topModelContainers);
        ModelGroup bottomGroup = project.createNewGroupAndAddModelListeners(
            bottomModelContainers);
        topGroup.setState(modelGroup.getState());
        topGroup.moveToCentre();
        topGroup.dropToBed();
        bottomGroup.translateBy(-20, -20);
        bottomGroup.setState(modelGroup.getState());
        bottomGroup.moveToCentre();
        bottomGroup.dropToBed();
        bottomGroup.translateBy(20, 20);
        childModelContainers.add(topGroup);
        childModelContainers.add(bottomGroup);
    }

    private List<Optional<ModelContainer>> cutModelContainerAtHeight(ModelContainer modelContainer,
        float cutHeightValue)
    {
        List<Optional<ModelContainer>> modelContainerPair = new ArrayList<>();

        cutHeightValue -= modelContainer.getYAdjust();

        //these transforms must be cleared so that bedToLocal conversions work properly in the cutter.
        modelContainer.saveAndClearBedTransform();
        modelContainer.saveAndClearDropToBedYTransform();

        try
        {
            
            List<Optional<TriangleMesh>> meshPair = MeshCutter2.cut(
                (TriangleMesh) modelContainer.getMeshView().getMesh(),
                cutHeightValue, modelContainer.getBedToLocalConverter());

            String modelName = modelContainer.getModelName();

            int ix = 1;
            for (Optional<TriangleMesh> subMesh : meshPair)
            {
                if (subMesh.isPresent())
                {
                    MeshView meshView = new MeshView(subMesh.get());
                    meshView.cullFaceProperty().set(CullFace.NONE);
                    ModelContainer newModelContainer = new ModelContainer(
                        modelContainer.getModelFile(), meshView);
                    MeshDebug.setDebuggingNode(newModelContainer);
                    newModelContainer.setModelName(modelName + " " + ix);
                    newModelContainer.setState(modelContainer.getState());
                    newModelContainer.getAssociateWithExtruderNumberProperty().set(
                        modelContainer.getAssociateWithExtruderNumberProperty().get());
                    modelContainerPair.add(Optional.of(newModelContainer));
                } else
                {
                    modelContainerPair.add(Optional.empty());
                }

//                newModelContainer.getMeshView().setDrawMode(DrawMode.LINE);
                ix++;
            }
        } finally
        {
            modelContainer.restoreBedTransform();
            modelContainer.restoreDropToBedYTransform();
        }

        return modelContainerPair;
    }

}
