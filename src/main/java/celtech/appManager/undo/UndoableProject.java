/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.modelcontrol.ModelContainer;
import java.util.Set;
import javafx.scene.shape.MeshView;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * UndoableProject wraps Project and puts each change into a Command that can be
 * undone.
 *
 * @author tony
 */
public class UndoableProject
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            Stenographer.class.getName());

    Project project;
    private final CommandStack commandStack;

    /**
     * A point interface (ie only one method) that takes no arguments and
     * returns void.
     */
    public interface NoArgsVoidFunc
    {

        void run() throws Exception;
    }

    private void doTransformCommand(NoArgsVoidFunc func)
    {
        doTransformCommand(func, false);
    }

    private void doTransformCommand(NoArgsVoidFunc func, boolean canMerge)
    {
        Command command = new TransformCommand(project, func, canMerge);
        commandStack.do_(command);
    }

    public UndoableProject(Project project)
    {
        this.project = project;
        commandStack = Lookup.getProjectGUIState(project).getCommandStack();
    }

    public void translateModelsXTo(Set<ModelContainer> modelContainers, double x)
    {
        doTransformCommand(() ->
        {
            project.translateModelsXTo(modelContainers, x);
        });
    }

    public void translateModelsZTo(Set<ModelContainer> modelContainers, double z)
    {
        doTransformCommand(() ->
        {
            project.translateModelsZTo(modelContainers, z);
        });
    }

    public void scaleXModels(Set<ModelContainer> modelContainers, double newScale,
            boolean preserveAspectRatio)
    {
        doTransformCommand(() ->
        {
            project.scaleXModels(modelContainers, newScale, preserveAspectRatio);
        });
    }

    public void scaleYModels(Set<ModelContainer> modelContainers, double newScale,
            boolean preserveAspectRatio)
    {
        doTransformCommand(() ->
        {
            project.scaleYModels(modelContainers, newScale, preserveAspectRatio);
        });
    }

    public void scaleZModels(Set<ModelContainer> modelContainers, double newScale,
            boolean preserveAspectRatio)
    {
        doTransformCommand(() ->
        {
            project.scaleZModels(modelContainers, newScale, preserveAspectRatio);
        });
    }

    public void scaleXYZRatioSelection(Set<ModelContainer> modelContainers, double ratio)
    {
        doTransformCommand(() ->
        {
            project.scaleXYZRatioSelection(modelContainers, ratio);
        });
    }

    public void resizeModelsDepth(Set<ModelContainer> modelContainers, double depth)
    {
        doTransformCommand(() ->
        {
            project.resizeModelsDepth(modelContainers, depth);
        });
    }

    public void resizeModelsHeight(Set<ModelContainer> modelContainers, double height)
    {
        doTransformCommand(() ->
        {
            project.resizeModelsHeight(modelContainers, height);
        });
    }

    public void resizeModelsWidth(Set<ModelContainer> modelContainers, double width)
    {
        doTransformCommand(() ->
        {
            project.resizeModelsWidth(modelContainers, width);
        });
    }

    public void rotateLeanModels(Set<ModelContainer> modelContainers, double rotation)
    {
        doTransformCommand(() ->
        {
            project.rotateLeanModels(modelContainers, rotation);
        });
    }

    public void rotateTwistModels(Set<ModelContainer> modelContainers, double rotation)
    {
        doTransformCommand(() ->
        {
            project.rotateTwistModels(modelContainers, rotation);
        });
    }

    public void rotateTurnModels(Set<ModelContainer> modelContainers, double rotation)
    {
        doTransformCommand(() ->
        {
            project.rotateTurnModels(modelContainers, rotation);
        });
    }

    public void translateModelsBy(Set<ModelContainer> modelContainers, double x, double z,
            boolean canMerge)
    {
        doTransformCommand(() ->
        {
            project.translateModelsBy(modelContainers, x, z);
        }, canMerge);
    }

    public void translateModelsTo(Set<ModelContainer> modelContainers, double x, double z,
            boolean canMerge)
    {
        doTransformCommand(() ->
        {
            project.translateModelsTo(modelContainers, x, z);
        }, canMerge);
    }

    public void autoLayout()
    {
        doTransformCommand(() ->
        {
            project.autoLayout();
        });
    }

    public void dropToBed(Set<ModelContainer> modelContainers)
    {
        doTransformCommand(() ->
        {
            project.dropToBed(modelContainers);
        });
    }

    public void snapToGround(ModelContainer modelContainer, MeshView meshView, int faceNumber)
    {
        doTransformCommand(() ->
        {
            project.snapToGround(modelContainer, meshView, faceNumber);
        });
    }

    public void addModel(ModelContainer modelContainer)
    {
        Command addModelCommand = new AddModelCommand(project, modelContainer);
        commandStack.do_(addModelCommand);
    }

    public void deleteModels(Set<ModelContainer> modelContainers)
    {
        Command deleteModelCommand = new DeleteModelsCommand(project, modelContainers);
        commandStack.do_(deleteModelCommand);
    }

    public void copyModels(Set<ModelContainer> modelContainers)
    {
        Command copyModelsCommand = new CopyModelsCommand(project, modelContainers);
        commandStack.do_(copyModelsCommand);
    }

    public void assignModelToExtruder(ModelContainer modelContainer,
            boolean assignToExtruder0)
    {
        Command setUserExtruder0Command = new AssignModelToExtruderCommand(project,
                modelContainer,
                assignToExtruder0);
        commandStack.do_(setUserExtruder0Command);
    }

    public void assignModelsToExtruders(Set<ModelContainer> modelContainersToAssignToExtruder0,
            Set<ModelContainer> modelContainersToAssignToExtruder1)
    {
        Command setUserExtruder0Command = new AssignModelToExtruderCommand(project,
                modelContainersToAssignToExtruder0,
                modelContainersToAssignToExtruder1);
        commandStack.do_(setUserExtruder0Command);
    }

    public void group(Set<ModelContainer> modelContainers)
    {
        Command groupCommand = new GroupCommand(project, modelContainers);
        commandStack.do_(groupCommand);

    }

    public void ungroup(Set<ModelContainer> modelContainers)
    {
        Command ungroupCommand = new UngroupCommand(project, modelContainers);
        commandStack.do_(ungroupCommand);
    }

    public void cut(Set<ModelContainer> modelContainers, float cutHeightValue)
    {
        Command cutCommand = new CutCommand(project, modelContainers, cutHeightValue);
        commandStack.do_(cutCommand);
    }

}
