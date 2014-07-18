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

    /**
     *
     * @return
     */
    public static ApplicationStatus getInstance()
    {
        if (instance == null)
        {
            instance = new ApplicationStatus();
        }

        return instance;
    }

    /**
     *
     * @param newMode
     */
    public void setMode(ApplicationMode newMode)
    {
        currentMode.setValue(newMode);
    }

    /**
     *
     * @return
     */
    public final ApplicationMode getMode()
    {
        return currentMode.getValue();
    }
    
    /**
     *
     * @return
     */
    public final ObjectProperty<ApplicationMode> modeProperty()
    {
        return currentMode;
    }

    /**
     *
     * @param isExpertMode
     */
    public void setExpertMode(boolean isExpertMode)
    {
        expertMode = isExpertMode;
    }

    /**
     *
     * @return
     */
    public boolean isExpertMode()
    {
        return expertMode;
    }

    /**
     *
     * @param value
     */
    public final void setAverageTimePerFrame(double value)
    {
        averageTimePerFrameProperty.set(value);
    }

    /**
     *
     * @return
     */
    public final double getAverageTimePerFrame()
    {
        return averageTimePerFrameProperty.get();
    }

    /**
     *
     * @return
     */
    public final DoubleProperty averageTimePerFrameProperty()
    {
        return averageTimePerFrameProperty;
    }
}
