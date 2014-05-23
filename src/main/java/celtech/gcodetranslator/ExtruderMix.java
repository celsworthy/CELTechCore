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
public class ExtruderMix
{

    double eFactor = 0;
    double dFactor = 0;
    int layerNumber = 0;

    public ExtruderMix(double eFactor, double dFactor, int layerNumber)
    {
        this.eFactor = eFactor;
        this.dFactor = dFactor;
        this.layerNumber = layerNumber;
    }

    public double getEFactor()
    {
        return eFactor;
    }

    public void setEFactor(double eFactor)
    {
        this.eFactor = eFactor;
    }

    public double getDFactor()
    {
        return dFactor;
    }

    public void setDFactor(double dFactor)
    {
        this.dFactor = dFactor;
    }

    public int getLayerNumber()
    {
        return layerNumber;
    }

    public void setLayerNumber(int layerNumber)
    {
        this.layerNumber = layerNumber;
    }
}
