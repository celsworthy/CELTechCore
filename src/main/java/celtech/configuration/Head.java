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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author ianhudson
 */
public class Head
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
    private FloatProperty headHours = new SimpleFloatProperty(0);

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
        this.nozzle2_B_offset.set(nozzle1_B_offset);
    }

    public void setTypeCode(String value)
    {
        typeCode.set(value);
    }

    public String getTypeCode()
    {
        return typeCode.get();
    }

    public StringProperty typeCodeProperty()
    {
        return typeCode;
    }

    public void setFriendlyName(String value)
    {
        friendlyName.set(value);
    }

    public String getFriendlyName()
    {
        return friendlyName.get();
    }

    public StringProperty friendlyNameProperty()
    {
        return friendlyName;
    }

    public void setUniqueID(String value)
    {
        uniqueID.set(value);
    }

    public String getUniqueID()
    {
        return uniqueID.get();
    }

    public StringProperty uniqueIDProperty()
    {
        return uniqueID;
    }

    public FloatProperty getMaximumTemperatureProperty()
    {
        return maximumTemperature;
    }

    public void setMaximumTemperature(float value)
    {
        maximumTemperature.set(value);
    }

    public float getMaximumTemperature()
    {
        return maximumTemperature.get();
    }

    public FloatProperty getBetaProperty()
    {
        return beta;
    }

    public void setBeta(float value)
    {
        beta.set(value);
    }

    public float getBeta()
    {
        return beta.get();
    }

    public FloatProperty getTcalProperty()
    {
        return tcal;
    }

    public void setTcal(float value)
    {
        tcal.set(value);
    }

    public float getTcal()
    {
        return tcal.get();
    }

    public FloatProperty getNozzle1_X_offsetProperty()
    {
        return nozzle1_X_offset;
    }

    public void setNozzle1_X_offset(float value)
    {
        nozzle1_X_offset.set(value);
    }

    public float getNozzle1_X_offset()
    {
        return nozzle1_X_offset.get();
    }

    public FloatProperty getNozzle1_Y_offsetProperty()
    {
        return nozzle1_Y_offset;
    }

    public void setNozzle1_Y_offset(float value)
    {
        nozzle1_Y_offset.set(value);
    }

    public float getNozzle1_Y_offset()
    {
        return nozzle1_Y_offset.get();
    }

    public FloatProperty getNozzle1_Z_offsetProperty()
    {
        return nozzle1_Z_offset;
    }

    public void setNozzle1_Z_offset(float value)
    {
        nozzle1_Z_offset.set(value);
    }

    public float getNozzle1_Z_offset()
    {
        return nozzle1_Z_offset.get();
    }

    public FloatProperty getNozzle1_B_offsetProperty()
    {
        return nozzle1_B_offset;
    }

    public void setNozzle1_B_offset(float value)
    {
        nozzle1_B_offset.set(value);
    }

    public float getNozzle1_B_offset()
    {
        return nozzle1_B_offset.get();
    }

    public FloatProperty getNozzle2_X_offsetProperty()
    {
        return nozzle2_X_offset;
    }

    public void setNozzle2_X_offset(float value)
    {
        nozzle2_X_offset.set(value);
    }

    public float getNozzle2_X_offset()
    {
        return nozzle2_X_offset.get();
    }

    public FloatProperty getNozzle2_Y_offsetProperty()
    {
        return nozzle2_Y_offset;
    }

    public void setNozzle2_Y_offset(float value)
    {
        nozzle2_Y_offset.set(value);
    }

    public float getNozzle2_Y_offset()
    {
        return nozzle2_Y_offset.get();
    }

    public FloatProperty getNozzle2_Z_offsetProperty()
    {
        return nozzle2_Z_offset;
    }

    public void setNozzle2_Z_offset(float value)
    {
        nozzle2_Z_offset.set(value);
    }

    public float getNozzle2_Z_offset()
    {
        return nozzle2_Z_offset.get();
    }

    public FloatProperty getNozzle2_B_offsetProperty()
    {
        return nozzle2_B_offset;
    }

    public void setNozzle2_B_offset(float value)
    {
        nozzle2_B_offset.set(value);
    }

    public float getNozzle2_B_offset()
    {
        return nozzle2_B_offset.get();
    }

    public FloatProperty getHeadHoursProperty()
    {
        return headHours;
    }

    public void setHeadHours(float value)
    {
        headHours.set(value);
    }

    public float getHeadHours()
    {
        return headHours.get();
    }

    @Override
    public String toString()
    {
        return friendlyName.get();
    }
}
