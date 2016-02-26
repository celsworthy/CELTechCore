/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.Lookup;
import celtech.appManager.ModelContainerProject;
import celtech.appManager.Project;
import celtech.roboxbase.configuration.Filament;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ProjectifiableThing;
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

    public void translateModelsXTo(Set<ProjectifiableThing> modelContainers, double x)
    {
        doTransformCommand(() ->
        {
            project.translateModelsXTo(modelContainers, x);
        });
    }

    public void translateModelsZTo(Set<ProjectifiableThing> modelContainers, double z)
    {
        doTransformCommand(() ->
        {
            project.translateModelsZTo(modelContainers, z);
        });
    }

    public void scaleXModels(Set<ProjectifiableThing> modelContainers, double newScale,
            boolean preserveAspectRatio)
    {
        doTransformCommand(() ->
        {
            project.scaleXModels(modelContainers, newScale, preserveAspectRatio);
        });
    }

    public void scaleYModels(Set<ProjectifiableThing> modelContainers, double newScale,
            boolean preserveAspectRatio)
    {
        doTransformCommand(() ->
        {
            project.scaleYModels(modelContainers, newScale, preserveAspectRatio);
        });
    }

    public void scaleZModels(Set<ProjectifiableThing> modelContainers, double newScale,
            boolean preserveAspectRatio)
    {
        doTransformCommand(() ->
        {
            project.scaleZModels(modelContainers, newScale, preserveAspectRatio);
        });
    }

    public void scaleXYZRatioSelection(Set<ProjectifiableThing> modelContainers, double ratio)
    {
        doTransformCommand(() ->
        {
            project.scaleXYZRatioSelection(modelContainers, ratio);
        });
    }

    public void resizeModelsDepth(Set<ProjectifiableThing> modelContainers, double depth)
    {
        doTransformCommand(() ->
        {
            project.resizeModelsDepth(modelContainers, depth);
        });
    }

    public void resizeModelsHeight(Set<ProjectifiableThing> modelContainers, double height)
    {
        doTransformCommand(() ->
        {
            project.resizeModelsHeight(modelContainers, height);
        });
    }

    public void resizeModelsWidth(Set<ProjectifiableThing> modelContainers, double width)
    {
        doTransformCommand(() ->
        {
            project.resizeModelsWidth(modelContainers, width);
        });
    }

    public void rotateLeanModels(Set<ModelContainer> modelContainers, double rotation)
    {
        if (project instanceof ModelContainerProject)
        {
            doTransformCommand(() ->
            {
                ((ModelContainerProject) project).rotateLeanModels(modelContainers, rotation);
            });
        }
    }

    public void rotateTwistModels(Set<ModelContainer> modelContainers, double rotation)
    {
        if (project instanceof ModelContainerProject)
        {
            doTransformCommand(() ->
            {
                ((ModelContainerProject) project).rotateTwistModels(modelContainers, rotation);
            });
        }
    }

    public void rotateTurnModels(Set<ModelContainer> modelContainers, double rotation)
    {
        if (project instanceof ModelContainerProject)
        {
            doTransformCommand(() ->
            {
                ((ModelContainerProject) project).rotateTurnModels(modelContainers, rotation);
            });
        }
    }

    public void translateModelsBy(Set<ProjectifiableThing> modelContainers, double x, double z,
            boolean canMerge)
    {
        doTransformCommand(() ->
        {
            project.translateModelsBy(modelContainers, x, z);
        }, canMerge);
    }

    public void translateModelsTo(Set<ProjectifiableThing> modelContainers, double x, double z,
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
        if (project instanceof ModelContainerProject)
        {
            doTransformCommand(() ->
            {
                ((ModelContainerProject) project).dropToBed(modelContainers);
            });
        }
    }

    public void snapToGround(ModelContainer modelContainer, MeshView meshView, int faceNumber)
    {
        if (project instanceof ModelContainerProject)
        {
            doTransformCommand(() ->
            {
                ((ModelContainerProject) project).snapToGround(modelContainer, meshView, faceNumber);
            });
        }
    }

    public void addModel(ProjectifiableThing modelContainer)
    {
        Command addModelCommand = new AddModelCommand(project, modelContainer);
        commandStack.do_(addModelCommand);
    }

    public void deleteModels(Set<ProjectifiableThing> modelContainers)
    {
        Command deleteModelCommand = new DeleteModelsCommand(project, modelContainers);
        commandStack.do_(deleteModelCommand);
    }

    public void copyModels(Set<ProjectifiableThing> modelContainers)
    {
        Command copyModelsCommand = new CopyModelsCommand(project, modelContainers);
        commandStack.do_(copyModelsCommand);
    }

    public void setExtruder0Filament(Filament filament)
    {
        if (project instanceof ModelContainerProject)
        {
            if (filament != ((ModelContainerProject) project).getExtruder0FilamentProperty().get())
            {
                SetExtruderFilamentCommand setExtruderCommand = new SetExtruderFilamentCommand(((ModelContainerProject) project),
                        filament,
                        0);
                commandStack.do_(setExtruderCommand);
            }
        }
    }

    public void setExtruder1Filament(Filament filament)
    {
        if (project instanceof ModelContainerProject)
        {
            if (filament != ((ModelContainerProject) project).getExtruder1FilamentProperty().get())
            {
                SetExtruderFilamentCommand setExtruderCommand = new SetExtruderFilamentCommand(((ModelContainerProject) project),
                        filament,
                        1);
                commandStack.do_(setExtruderCommand);
            }
        }
    }

    public void setUseExtruder0Filament(ModelContainer modelContainer, boolean useExtruder0)
    {
        if (project instanceof ModelContainerProject)
        {
            Command setUserExtruder0Command = new SetUserExtruder0Command(((ModelContainerProject) project),
                    modelContainer,
                    useExtruder0);
            commandStack.do_(setUserExtruder0Command);
        }
    }

    public void group(Set<ModelContainer> modelContainers)
    {
        if (project instanceof ModelContainerProject)
        {
            Command groupCommand = new GroupCommand(((ModelContainerProject) project), modelContainers);
            commandStack.do_(groupCommand);
        }
    }

    public void ungroup(Set<ModelContainer> modelContainers)
    {
        if (project instanceof ModelContainerProject)
        {
            Command ungroupCommand = new UngroupCommand(((ModelContainerProject) project), modelContainers);
            commandStack.do_(ungroupCommand);
        }
    }

    public void cut(Set<ModelContainer> modelContainers, float cutHeightValue)
    {
        if (project instanceof ModelContainerProject)
        {
            Command cutCommand = new CutCommand(((ModelContainerProject) project), modelContainers, cutHeightValue);
            commandStack.do_(cutCommand);
        }
    }
}
