/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.modelcontrol.ModelContainer;
import java.util.Set;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * UndoableProject decorates Project and puts each change into a Command that can be undone.
 *
 * @author tony
 */
public class UndoableProject
{

    private Stenographer steno = StenographerFactory.getStenographer(
        Stenographer.class.getName());

    Project project;
    private CommandStack commandStack;

    /**
     * A point interface (ie only one method) that takes no arguments and returns void.
     */
    public interface NoArgsVoidFunc
    {

        void run() throws Exception;
    }

    private void doTransformCommand(NoArgsVoidFunc func)
    {
        Command command = new TransformCommand(project, func);
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
    
    public void rotateLeanModels(Set<ModelContainer> modelContainers, double rotation) {
                doTransformCommand(() ->
        {
            project.rotateLeanModels(modelContainers, rotation);
        });
    }
    
    public void rotateTwistModels(Set<ModelContainer> modelContainers, double rotation) {
                doTransformCommand(() ->
        {
            project.rotateTwistModels(modelContainers, rotation);
        });
    }
    
    public void rotateTurnModels(Set<ModelContainer> modelContainers, double rotation) {
                doTransformCommand(() ->
        {
            project.rotateTurnModels(modelContainers, rotation);
        });
    }    

}
