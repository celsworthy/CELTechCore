/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

import celtech.coreUI.components.Notifications.AppearingProgressBar;

/**
 *
 * @author tony
 */
public class TesStatusBar extends AppearingProgressBar
{

    public TesStatusBar()
    {
    }
    
    public void setName(String name)
    {
        largeProgressDescription.setText(name);
    }

}
