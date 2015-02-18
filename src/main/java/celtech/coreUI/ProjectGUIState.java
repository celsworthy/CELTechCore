/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI;

import celtech.coreUI.visualisation.SelectedModelContainers;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * The ProjectGUIState class contains GUI information for a project such as the selected models. It
 * is put here to keep the Project class clean of GUI data.
 */
public class ProjectGUIState
{
    private final SelectedModelContainers selectedModelContainers = new SelectedModelContainers();
    
    private final ObjectProperty<LayoutSubmode> layoutSubmode = new SimpleObjectProperty<>(LayoutSubmode.SELECT);
    
    public SelectedModelContainers getSelectedModelContainers() {
        return selectedModelContainers;
    }
    
    public ObjectProperty<LayoutSubmode> getLayoutSubmodeProperty() {
        return layoutSubmode;
    }
}
