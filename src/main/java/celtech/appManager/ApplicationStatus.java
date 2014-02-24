/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.appManager;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author ianhudson
 */
public class ApplicationStatus
{

    private static ApplicationStatus instance = null;
    private ObjectProperty<ApplicationMode> currentMode = new SimpleObjectProperty<ApplicationMode>(null);
    private StringProperty modeStringProperty = new SimpleStringProperty();
    private StringProperty modeDisplayStringProperty = new SimpleStringProperty();
    private boolean expertMode = false;
    private DoubleProperty averageTimePerFrameProperty = new SimpleDoubleProperty(0);

    private ApplicationStatus()
    {
    }

    public static ApplicationStatus getInstance()
    {
        if (instance == null)
        {
            instance = new ApplicationStatus();
        }

        return instance;
    }

    public void setMode(ApplicationMode newMode)
    {
        currentMode.setValue(newMode);
    }

    public final ApplicationMode getMode()
    {
        return currentMode.getValue();
    }
    
    public final ObjectProperty<ApplicationMode> modeProperty()
    {
        return currentMode;
    }

    public void setExpertMode(boolean isExpertMode)
    {
        expertMode = isExpertMode;
    }

    public boolean isExpertMode()
    {
        return expertMode;
    }

    public final void setAverageTimePerFrame(double value)
    {
        averageTimePerFrameProperty.set(value);
    }

    public final double getAverageTimePerFrame()
    {
        return averageTimePerFrameProperty.get();
    }

    public final DoubleProperty averageTimePerFrameProperty()
    {
        return averageTimePerFrameProperty;
    }
}
