/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.gcodetranslator;

/**
 *
 * @author Ian
 */
public class RoboxiserResult
{
    private boolean success = false;
    private double predictedDuration = 0.0;
    private double volumeUsed = 0.0;

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public double getPredictedDuration()
    {
        return predictedDuration;
    }

    public void setPredictedDuration(double predictedDuration)
    {
        this.predictedDuration = predictedDuration;
    }

    public double getVolumeUsed()
    {
        return volumeUsed;
    }

    public void setVolumeUsed(double volumeUsed)
    {
        this.volumeUsed = volumeUsed;
    }
    
    
}
