/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.appManager.Project;
import celtech.modelcontrol.ModelContainer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * The TransformCommand is a Command that allows x,y, scaleX, scaleY, scaleZ,
 * lean, twist and turn changes to be undone.
 *
 * @author tony
 */
class TransformCommand extends Command
{

    private Stenographer steno = StenographerFactory.getStenographer(
        TransformCommand.class.getName());

    private final UndoableProject.NoArgsVoidFunc func;
    private Set<ModelContainer.State> originalStates;
    private Set<ModelContainer.State> newStates;
    private final boolean canMerge;
    private final Project project;

    public TransformCommand(Project project, UndoableProject.NoArgsVoidFunc func, boolean canMerge)
    {
        this.project = project;
        this.func = func;
        this.canMerge = canMerge;
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
            steno.exception("Failed running command ", ex);
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

    @Override
    public boolean canMergeWith(Command command)
    {
        if (command instanceof TransformCommand)
        {
            TransformCommand transformCommand = (TransformCommand) command;
            return transformCommand.getCanMerge();
        } else {
            return false;
        }
    }

    @Override
    public void merge(Command command)
    {
        if (command instanceof TransformCommand)
        {
            TransformCommand transformCommand = (TransformCommand) command;
            if (transformCommand.getCanMerge())
            {
                mergeStates(newStates, transformCommand.newStates);
            }
        }

    }

    protected boolean getCanMerge()
    {
        return canMerge;
    }

    /**
     * Update states to include the changes in toStates.
     */
    private void mergeStates(Set<ModelContainer.State> states, Set<ModelContainer.State> toStates)
    {
        Map<Integer, ModelContainer.State> toStatesById = makeStatesById(toStates);
        for (ModelContainer.State state : states)
        {
            state.assignFrom(toStatesById.get(state.modelId));
        }

    }

    private Map<Integer, ModelContainer.State> makeStatesById(Set<ModelContainer.State> states)
    {
        Map<Integer, ModelContainer.State> statesById = new HashMap<>();
        for (ModelContainer.State state : states)
        {
            statesById.put(state.modelId, state);
        }
        return statesById;
    }
}
