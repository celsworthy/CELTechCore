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

}
