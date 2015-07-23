/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI;

import celtech.coreUI.visualisation.SelectedModelContainers;
import celtech.modelcontrol.ModelContainer;
import java.util.Set;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * ProjectGUIRules indicates if the project selection can be translated, removed, scaled etc.
 */
public class ProjectGUIRules
{

    private final Set<ModelContainer> excludedFromSelection;
    
   private final SelectedModelContainers selectedModelContainers;

    public ProjectGUIRules(SelectedModelContainers selectedModelContainers,
        Set<ModelContainer> excludedFromSelection)
    {
        this.selectedModelContainers = selectedModelContainers;
        this.excludedFromSelection = excludedFromSelection;
    }

    public BooleanBinding canTranslateRotateOrScaleSelection()
    {
       return selectedModelContainers.getSelectionHasChildOfGroup().not();
    }


}
