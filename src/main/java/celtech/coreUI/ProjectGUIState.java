/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI;

import celtech.appManager.Project;
import celtech.appManager.undo.CommandStack;
import celtech.coreUI.visualisation.SelectedModelContainers;
import celtech.modelcontrol.ModelContainer;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * The ProjectGUIState class contains GUI information for a project such as the selected models. It
 * is put here to keep the Project class clean of GUI data.
 */
public class ProjectGUIState
{
    private final SelectedModelContainers selectedModelContainers;
    
    private final ObjectProperty<LayoutSubmode> layoutSubmode;
    
    private final CommandStack commandStack;
    
    private final Set<ModelContainer> excludedFromSelection = new HashSet<>();
    
    private final ProjectGUIRules projectGUIRules;

    public ProjectGUIState(Project project)
    {
        selectedModelContainers = new SelectedModelContainers(project);
        layoutSubmode = new SimpleObjectProperty<>(LayoutSubmode.SELECT);
        commandStack = new CommandStack();
        projectGUIRules = new ProjectGUIRules(selectedModelContainers, excludedFromSelection);
    }

    public CommandStack getCommandStack()
    {
        return commandStack;
    }
    
    public ProjectGUIRules getProjectGUIRules() {
        return projectGUIRules;
    }
    
    public Set<ModelContainer> getExcludedFromSelection() {
        return excludedFromSelection;
    }
    
    public SelectedModelContainers getSelectedModelContainers() {
        return selectedModelContainers;
    }
    
    public ObjectProperty<LayoutSubmode> getLayoutSubmodeProperty() {
        return layoutSubmode;
    }
}
