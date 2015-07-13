/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

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
