/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI;

import celtech.coreUI.visualisation.SelectedModelContainers;

/**
 * The ProjectGUIState class contains GUI information for a project such as the selected models. It
 * is put here to keep the Project class clean of GUI data.
 */
public class ProjectGUIState
{
    private final SelectedModelContainers selectedModelContainers = new SelectedModelContainers();
}
