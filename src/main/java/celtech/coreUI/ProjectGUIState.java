/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI;

import celtech.coreUI.visualisation.SelectedModelContainers;
import celtech.coreUI.visualisation.ThreeDViewManager;

/**
 * The ProjectGUIState class contains GUI information for a project such as the selected models. It
 * is put here to keep the Project class clean of GUI data.
 */
public class ProjectGUIState
{
    private SelectedModelContainers selectedModelContainers;
    
    private ThreeDViewManager threeDViewManager;
    
    public SelectedModelContainers getSelectedModelContainers() {
        return selectedModelContainers;
    }

    public ThreeDViewManager getThreeDViewManager()
    {
        return threeDViewManager;
    }
    
    public void setThreeDViewManager(ThreeDViewManager threeDViewManager) {
        this.threeDViewManager = threeDViewManager;
    }

    public void setSelectedModelContainers(SelectedModelContainers selectedModelContainers)
    {
        this.selectedModelContainers = selectedModelContainers;
    }
}