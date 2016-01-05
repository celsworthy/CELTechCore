/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.appManager.Project;
import celtech.modelcontrol.ModelContainer;
import javafx.scene.shape.MeshView;

/**
 *
 * @author tony
 */
public class SetUserExtruder0Command extends Command
{

    Project project;
    ModelContainer modelContainer;
    MeshView pickedMesh;
    boolean useExtruder0;

    public SetUserExtruder0Command(Project project,
            ModelContainer modelContainer,
            boolean useExtruder0)
    {
        this.project = project;
        this.modelContainer = modelContainer;
        this.useExtruder0 = useExtruder0;
    }

    @Override
    public void do_()
    {
        redo();
    }

    @Override
    public void undo()
    {
        project.setUseExtruder0Filament(modelContainer, !useExtruder0);
    }

    @Override
    public void redo()
    {
        project.setUseExtruder0Filament(modelContainer, useExtruder0);
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

}
