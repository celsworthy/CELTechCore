/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.configuration;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;

/**
 *
 * @author ianhudson
 */
public class Nozzle
{

    private FloatProperty maximumTemperature = new SimpleFloatProperty(0);
    private FloatProperty beta = new SimpleFloatProperty(0);
    private FloatProperty tcal = new SimpleFloatProperty(0);
    private FloatProperty X_offset = new SimpleFloatProperty(0);
    private FloatProperty Y_offset = new SimpleFloatProperty(0);
    private FloatProperty Z_offset = new SimpleFloatProperty(0);
    private FloatProperty B_offset = new SimpleFloatProperty(0);

    /**
     *
     * @param value
     */
    public void setMaximumTemperature(float value)
    {
        maximumTemperature.set(value);
    }

    /**
     *
     * @return
     */
    public float getMaximumTemperature()
    {
        return maximumTemperature.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty maximumTemperatureProperty()
    {
        return maximumTemperature;
    }

    /**
     *
     * @param value
     */
    public void setBeta(float value)
    {
        beta.set(value);
    }

    /**
     *
     * @return
     */
    public float getBeta()
    {
        return beta.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty betaProperty()
    {
        return beta;
    }

    /**
     *
     * @param value
     */
    public void setTcal(float value)
    {
        tcal.set(value);
    }

    /**
     *
     * @return
     */
    public float getTcal()
    {
        return tcal.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty tcalProperty()
    {
        return tcal;
    }

    /**
     *
     * @param value
     */
    public void setX_offset(float value)
    {
        X_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getX_offset()
    {
        return X_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty X_offsetProperty()
    {
        return X_offset;
    }
    
    /**
     *
     * @param value
     */
    public void setY_offset(float value)
    {
        Y_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getY_offset()
    {
        return Y_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty Y_offsetProperty()
    {
        return Y_offset;
    }
    
    /**
     *
     * @param value
     */
    public void setZ_offset(float value)
    {
        Z_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getZ_offset()
    {
        return Z_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty Z_offsetProperty()
    {
        return Z_offset;
    }
    
    /**
     *
     * @param value
     */
    public void setB_offset(float value)
    {
        B_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getB_offset()
    {
        return B_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty B_offsetProperty()
    {
        return B_offset;
    }
}
