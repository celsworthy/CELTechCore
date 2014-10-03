package celtech.printerControl.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 *
 * @author Ian
 */
public class Extruder
{

    private String extruderAxisLetter;
    protected final BooleanProperty filamentLoaded = new SimpleBooleanProperty(false);
    protected final BooleanProperty indexWheelState = new SimpleBooleanProperty(false);

    public Extruder(String extruderAxisLetter)
    {
        this.extruderAxisLetter = extruderAxisLetter;
    }

    public ReadOnlyBooleanProperty getFilamentLoadedProperty()
    {
        return filamentLoaded;
    }

    public ReadOnlyBooleanProperty getIndexWheelStateProperty()
    {
        return filamentLoaded;
    }

    public String getExtruderAxisLetter()
    {
        return extruderAxisLetter;
    }
}
