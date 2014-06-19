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
    /**
     * Line number for the start of the layer at the given index
     */
    private List<Integer> layerNumberToLineNumber;
    /**
     *  Distance travelled for the layer at the given index
     */
    private List<Double> layerNumberToDistanceTravelled;
    /**
     * The line number where the first extrusion is performed
     */
    private Integer lineNumberOfFirstExtrusion;
    
    /**
     * The predicted duration in seconds for the layer at the given index
     */
    private List<Double> layerNumberToPredictedDuration;

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

    /**
     * @return the lineNumberOfFirstExtrusion
     */
    public Integer getLineNumberOfFirstExtrusion()
    {
        return lineNumberOfFirstExtrusion;
    }

    /**
     * @param lineNumberOfFirstExtrusion the lineNumberOfFirstExtrusion to set
     */
    public void setLineNumberOfFirstExtrusion(Integer lineNumberOfFirstExtrusion)
    {
        this.lineNumberOfFirstExtrusion = lineNumberOfFirstExtrusion;
    }

    /**
     * @return the layerNumberToPredictedDuration
     */
    public List<Double> getLayerNumberToPredictedDuration()
    {
        return layerNumberToPredictedDuration;
    }

    /**
     * @param layerNumberToPredictedDuration the layerNumberToPredictedDuration to set
     */
    public void setLayerNumberToPredictedDuration(
            List<Double> layerNumberToPredictedDuration)
    {
        this.layerNumberToPredictedDuration = layerNumberToPredictedDuration;
    }
    
    
}
