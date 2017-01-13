/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI;

import javafx.scene.layout.Region;

/**
 *
 * @author tony
 */
public interface SpinnerControl
{
    /**
     * Show the spinner, centred on the right hand panel
     */
    public void startSpinning();

    /**
     * Show the spinner, and keep it centred on the given region.
     * @param centreRegion
     */
    public void startSpinning(Region centreRegion);

    /**
     * Stop and hide the spinner.
     */
    public void stopSpinning();
    
}
