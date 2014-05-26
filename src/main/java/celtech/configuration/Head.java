/*
 * To change this license header.set(choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.configuration;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author ianhudson
 */
public class Head implements Cloneable
{

    private StringProperty typeCode = new SimpleStringProperty("");
    private StringProperty friendlyName = new SimpleStringProperty("");
    private StringProperty uniqueID = new SimpleStringProperty("");
    private FloatProperty maximumTemperature = new SimpleFloatProperty(0);
    private FloatProperty beta = new SimpleFloatProperty(0);
    private FloatProperty tcal = new SimpleFloatProperty(0);
    private FloatProperty nozzle1_X_offset = new SimpleFloatProperty(0);
    private FloatProperty nozzle1_Y_offset = new SimpleFloatProperty(0);
    private FloatProperty nozzle1_Z_offset = new SimpleFloatProperty(0);
    private FloatProperty nozzle1_B_offset = new SimpleFloatProperty(0);
    private FloatProperty nozzle2_X_offset = new SimpleFloatProperty(0);
    private FloatProperty nozzle2_Y_offset = new SimpleFloatProperty(0);
    private FloatProperty nozzle2_Z_offset = new SimpleFloatProperty(0);
    private FloatProperty nozzle2_B_offset = new SimpleFloatProperty(0);
    private FloatProperty lastFilamentTemperature = new SimpleFloatProperty(0);
    private FloatProperty headHours = new SimpleFloatProperty(0);

    /**
     *
     * @param typeCode
     * @param friendlyName
     * @param maximumTemperature
     * @param beta
     * @param tcal
     * @param nozzle1_X_offset
     * @param nozzle1_Y_offset
     * @param nozzle1_Z_offset
     * @param nozzle1_B_offset
     * @param nozzle2_X_offset
     * @param nozzle2_Y_offset
     * @param nozzle2_Z_offset
     * @param nozzle2_B_offset
     */
    public Head(String typeCode, String friendlyName,
            float maximumTemperature,
            float beta,
            float tcal,
            float nozzle1_X_offset,
            float nozzle1_Y_offset,
            float nozzle1_Z_offset,
            float nozzle1_B_offset,
            float nozzle2_X_offset,
            float nozzle2_Y_offset,
            float nozzle2_Z_offset,
            float nozzle2_B_offset)
    {
        this.typeCode.set(typeCode);
        this.friendlyName.set(friendlyName);
        this.maximumTemperature.set(maximumTemperature);
        this.beta.set(beta);
        this.tcal.set(tcal);
        this.nozzle1_X_offset.set(nozzle1_X_offset);
        this.nozzle1_Y_offset.set(nozzle1_Y_offset);
        this.nozzle1_Z_offset.set(nozzle1_Z_offset);
        this.nozzle1_B_offset.set(nozzle1_B_offset);
        this.nozzle2_X_offset.set(nozzle2_X_offset);
        this.nozzle2_Y_offset.set(nozzle2_Y_offset);
        this.nozzle2_Z_offset.set(nozzle2_Z_offset);
        this.nozzle2_B_offset.set(nozzle2_B_offset);
    }

    /**
     *
     * @param value
     */
    public void setTypeCode(String value)
    {
        typeCode.set(value);
    }

    /**
     *
     * @return
     */
    public String getTypeCode()
    {
        return typeCode.get();
    }

    /**
     *
     * @return
     */
    public StringProperty typeCodeProperty()
    {
        return typeCode;
    }

    /**
     *
     * @param value
     */
    public void setFriendlyName(String value)
    {
        friendlyName.set(value);
    }

    /**
     *
     * @return
     */
    public String getFriendlyName()
    {
        return friendlyName.get();
    }

    /**
     *
     * @return
     */
    public StringProperty friendlyNameProperty()
    {
        return friendlyName;
    }

    /**
     *
     * @param value
     */
    public void setUniqueID(String value)
    {
        uniqueID.set(value);
    }

    /**
     *
     * @return
     */
    public String getUniqueID()
    {
        return uniqueID.get();
    }

    /**
     *
     * @return
     */
    public StringProperty uniqueIDProperty()
    {
        return uniqueID;
    }

    /**
     *
     * @return
     */
    public FloatProperty getMaximumTemperatureProperty()
    {
        return maximumTemperature;
    }

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
    public FloatProperty getBetaProperty()
    {
        return beta;
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
    public FloatProperty getTcalProperty()
    {
        return tcal;
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
    public float getTCal()
    {
        return tcal.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle1XOffsetProperty()
    {
        return nozzle1_X_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle1_X_offset(float value)
    {
        nozzle1_X_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle1XOffset()
    {
        return nozzle1_X_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle1_Y_offsetProperty()
    {
        return nozzle1_Y_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle1_Y_offset(float value)
    {
        nozzle1_Y_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle1YOffset()
    {
        return nozzle1_Y_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle1_Z_offsetProperty()
    {
        return nozzle1_Z_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle1_Z_offset(float value)
    {
        nozzle1_Z_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle1ZOffset()
    {
        return nozzle1_Z_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle1_B_offsetProperty()
    {
        return nozzle1_B_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle1_B_offset(float value)
    {
        nozzle1_B_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle1BOffset()
    {
        return nozzle1_B_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle2_X_offsetProperty()
    {
        return nozzle2_X_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle2_X_offset(float value)
    {
        nozzle2_X_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle2XOffset()
    {
        return nozzle2_X_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle2_Y_offsetProperty()
    {
        return nozzle2_Y_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle2_Y_offset(float value)
    {
        nozzle2_Y_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle2YOffset()
    {
        return nozzle2_Y_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle2_Z_offsetProperty()
    {
        return nozzle2_Z_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle2_Z_offset(float value)
    {
        nozzle2_Z_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle2ZOffset()
    {
        return nozzle2_Z_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle2_B_offsetProperty()
    {
        return nozzle2_B_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle2_B_offset(float value)
    {
        nozzle2_B_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle2BOffset()
    {
        return nozzle2_B_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getHeadHoursProperty()
    {
        return headHours;
    }

    /**
     *
     * @param value
     */
    public void setHeadHours(float value)
    {
        headHours.set(value);
    }

    /**
     *
     * @return
     */
    public float getHeadHours()
    {
        return headHours.get();
    }

    /**
     *
     * @param value
     */
    public void setLastFilamentTemperature(float value)
    {
        lastFilamentTemperature.set(value);
    }

    /**
     *
     * @return
     */
    public float getLastFilamentTemperature()
    {
        return lastFilamentTemperature.get();
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        return friendlyName.get();
    }

    /**
     *
     * @return
     */
    @Override
    public Head clone()
    {
        Head clone = new Head(
                this.getTypeCode(),
                this.getFriendlyName(),
                this.getMaximumTemperature(),
                this.getBeta(),
                this.getTCal(),
                this.getNozzle1XOffset(),
                this.getNozzle1YOffset(),
                this.getNozzle1ZOffset(),
                this.getNozzle1BOffset(),
                this.getNozzle2XOffset(),
                this.getNozzle2YOffset(),
                this.getNozzle2ZOffset(),
                this.getNozzle2BOffset()
        );

        return clone;
    }
}
