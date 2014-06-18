/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.gcodetranslator;

import java.util.List;

/**
 *
 * @author Ian
 */
public class RoboxiserResult
{
    private boolean success = false;
    private double predictedDuration = 0.0;
    private double volumeUsed = 0.0;
    private List<Integer> layerNumberToLineNumber;
    private List<Double> layerNumberToDistanceTravelled;

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

    /**
     *
     * @return
     */
    public double getPredictedDuration()
    {
        return predictedDuration;
    }

    /**
     *
     * @param predictedDuration
     */
    public void setPredictedDuration(double predictedDuration)
    {
        this.predictedDuration = predictedDuration;
    }

    /**
     *
     * @return
     */
    public double getVolumeUsed()
    {
        return volumeUsed;
    }

    /**
     *
     * @param volumeUsed
     */
    public void setVolumeUsed(double volumeUsed)
    {
        this.volumeUsed = volumeUsed;
    }

    /**
     * @return the layerNumberToLineNumber
     */
    public List<Integer> getLayerNumberToLineNumber()
    {
        return layerNumberToLineNumber;
    }

    /**
     * @param layerNumberToLineNumber the layerNumberToLineNumber to set
     */
    public void setLayerNumberToLineNumber(List<Integer> layerNumberToLineNumber)
    {
        this.layerNumberToLineNumber = layerNumberToLineNumber;
    }

    /**
     * @return the layerNumberToDistanceTravelled
     */
    public List<Double> getLayerNumberToDistanceTravelled()
    {
        return layerNumberToDistanceTravelled;
    }

    /**
     * @param layerNumberToDistanceTravelled the layerNumberToDistanceTravelled to set
     */
    public void setLayerNumberToDistanceTravelled(List<Double> layerNumberToDistanceTravelled)
    {
        this.layerNumberToDistanceTravelled = layerNumberToDistanceTravelled;
    }
    
    
}
