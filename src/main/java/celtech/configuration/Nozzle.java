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

    public void setMaximumTemperature(float value)
    {
        maximumTemperature.set(value);
    }

    public float getMaximumTemperature()
    {
        return maximumTemperature.get();
    }

    public FloatProperty maximumTemperatureProperty()
    {
        return maximumTemperature;
    }

    public void setBeta(float value)
    {
        beta.set(value);
    }

    public float getBeta()
    {
        return beta.get();
    }

    public FloatProperty betaProperty()
    {
        return beta;
    }

    public void setTcal(float value)
    {
        tcal.set(value);
    }

    public float getTcal()
    {
        return tcal.get();
    }

    public FloatProperty tcalProperty()
    {
        return tcal;
    }

    public void setX_offset(float value)
    {
        X_offset.set(value);
    }

    public float getX_offset()
    {
        return X_offset.get();
    }

    public FloatProperty X_offsetProperty()
    {
        return X_offset;
    }
    
        public void setY_offset(float value)
    {
        Y_offset.set(value);
    }

    public float getY_offset()
    {
        return Y_offset.get();
    }

    public FloatProperty Y_offsetProperty()
    {
        return Y_offset;
    }
    
        public void setZ_offset(float value)
    {
        Z_offset.set(value);
    }

    public float getZ_offset()
    {
        return Z_offset.get();
    }

    public FloatProperty Z_offsetProperty()
    {
        return Z_offset;
    }
    
        public void setB_offset(float value)
    {
        B_offset.set(value);
    }

    public float getB_offset()
    {
        return B_offset.get();
    }

    public FloatProperty B_offsetProperty()
    {
        return B_offset;
    }
}
