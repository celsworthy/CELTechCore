/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.appManager.Project;
import celtech.modelcontrol.ModelContainer;
import java.util.Set;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * The TransformCommand is a Command that allows x,y and scaleX, scaleY, scaleZ changes
 * to be undone.
 * @author tony
 */
class TransformCommand extends Command
{

    private Stenographer steno = StenographerFactory.getStenographer(
        TransformCommand.class.getName());

    UndoableProject.NoArgsVoidFunc func;
    Set<ModelContainer.State> originalStates;
    Set<ModelContainer.State> newStates;
    private final Project project;

    public TransformCommand(Project project, UndoableProject.NoArgsVoidFunc func)
    {
        this.project = project;
        this.func = func;
    }

    @Override
    public void do_()
    {
        originalStates = project.getModelStates();
        try
        {
            func.run();
            newStates = project.getModelStates();
        } catch (Exception ex)
        {
            steno.error("Failed running command " + ex);
        }
    }

    @Override
    public void undo()
    {
        project.setModelStates(originalStates);
    }

    @Override
    public void redo()
    {
        project.setModelStates(newStates);
    }

}
