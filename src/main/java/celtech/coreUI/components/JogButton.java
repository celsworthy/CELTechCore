/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.components;

import celtech.utils.AxisSpecifier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;

/**
 *
 * @author Ian
 */
public class JogButton extends Button
{

    private final ObjectProperty<AxisSpecifier> axis = new SimpleObjectProperty<>();
    private final FloatProperty distance = new SimpleFloatProperty();
    private final BooleanProperty useG1 = new SimpleBooleanProperty(false);
    private final IntegerProperty feedRate = new SimpleIntegerProperty(0);

    public void setAxis(AxisSpecifier value)
    {
        axis.set(value);
    }

    public AxisSpecifier getAxis()
    {
        return axis.get();
    }

    public ObjectProperty<AxisSpecifier> getAxisProperty()
    {
        return axis;
    }

    public void setDistance(float value)
    {
        distance.set(value);
    }

    public float getDistance()
    {
        return distance.get();
    }

    public FloatProperty getDistanceProperty()
    {
        return distance;
    }

    public void setUseG1(boolean value)
    {
        useG1.set(value);
    }

    public boolean getUseG1()
    {
        return useG1.get();
    }

    public BooleanProperty getUseG1Property()
    {
        return useG1;
    }

    public void setFeedRate(int value)
    {
        feedRate.set(value);
    }

    public int getFeedRate()
    {
        return feedRate.get();
    }

    public IntegerProperty getFeedRateProperty()
    {
        return feedRate;
    }

    public JogButton()
    {
        getStyleClass().add("jog-button");
        setPickOnBounds(false);
    }
}
