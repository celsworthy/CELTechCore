/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI;

import celtech.coreUI.visualisation.SelectedModelContainers;
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

    private final SelectedModelContainers selectedModelContainers;

    public ProjectGUIRules(SelectedModelContainers selectedModelContainers,
        ObservableSet<ModelContainer> excludedFromSelection)
    {
        this.selectedModelContainers = selectedModelContainers;
        this.excludedFromSelection = excludedFromSelection;
    }

    public BooleanBinding canTranslateRotateOrScaleSelection()
    {
        return selectedModelContainers.getSelectionHasChildOfGroup().not();
    }

    public BooleanBinding canSnapToGroundSelection()
    {
        return selectedModelContainers.getSelectionHasChildOfGroup().not();
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
