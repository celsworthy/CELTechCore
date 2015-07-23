/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI;

import celtech.coreUI.visualisation.ProjectSelection;
import celtech.modelcontrol.ModelContainer;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableSet;

/**
 * ProjectGUIRules indicates eg if the project selection can be translated, removed, scaled etc.
 */
public class ProjectGUIRules
{

    private final ObservableSet<ModelContainer> excludedFromSelection;

    private final ProjectSelection projectSelection;

    public ProjectGUIRules(ProjectSelection projectSelection,
        ObservableSet<ModelContainer> excludedFromSelection)
    {
        this.projectSelection = projectSelection;
        this.excludedFromSelection = excludedFromSelection;
    }

    public BooleanBinding canTranslateRotateOrScaleSelection()
    {
        return projectSelection.getSelectionHasChildOfGroup().not();
    }

    public BooleanBinding canSnapToGroundSelection()
    {
        return projectSelection.getSelectionHasChildOfGroup().not();
    }

    public BooleanBinding canRemoveOrDuplicateSelection()
    {
        return Bindings.isEmpty(excludedFromSelection);
    }

    public BooleanBinding canAddModel()
    {
        return Bindings.isEmpty(excludedFromSelection);
    }

}
