/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.controllers;

import celtech.appManager.Project;

/**
 *
 * @author tony
 */
public interface ProjectAwareController
{
    public void setProject(Project project);
}
