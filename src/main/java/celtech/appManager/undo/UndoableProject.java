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

    public void autoLayout()
    {
        doTransformCommand(() ->
        {
            project.autoLayout();
        });
    }

    public void snapToGround(ModelContainer modelContainer, int faceNumber)
    {
        doTransformCommand(() ->
        {
            project.snapToGround(modelContainer, faceNumber);
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

    public void setExtruder0Filament(Filament filament)
    {
        if (filament != project.getExtruder0FilamentProperty().get())
        {
            SetExtruderFilamentCommand setExtruderCommand = new SetExtruderFilamentCommand(project,
                    filament,
                    0);
            commandStack.do_(setExtruderCommand);
        }
    }

    public void setExtruder1Filament(Filament filament)
    {
        if (filament != project.getExtruder1FilamentProperty().get())
        {
            SetExtruderFilamentCommand setExtruderCommand = new SetExtruderFilamentCommand(project,
                    filament,
                    1);
            commandStack.do_(setExtruderCommand);
        }

    }

    public void setUseExtruder0Filament(ModelContainer modelContainer, boolean useExtruder0)
    {
        Command setUserExtruder0Command = new SetUserExtruder0Command(project,
                modelContainer,
                useExtruder0);
        commandStack.do_(setUserExtruder0Command);
    }

    public void splitIntoParts(Set<ModelContainer> modelContainers)
    {
        Command splitIntoPartsCommand = new SplitIntoPartsCommand(project, modelContainers);
        commandStack.do_(splitIntoPartsCommand);
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

}
