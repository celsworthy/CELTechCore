/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.roboxmoviemaker;

import celtech.services.slicer.*;
import celtech.appManager.Project;
import celtech.configuration.FilamentContainer;
import celtech.printerControl.model.HardwarePrinter;

/**
 *
 * @author ianhudson
 */
public class MovieMakerResult
{
    private boolean success = false;

    /**
     *
     * @param success
     */
    public MovieMakerResult(boolean success)
    {
        this.success = success;
    }

    /**
     *
     * @return
     */
    public boolean isSuccess()
    {
        return success;
    }

    /**
     *
     * @param success
     */
    public void setSuccess(boolean success)
    {
        this.success = success;
    }
}
