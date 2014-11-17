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
    protected final BooleanProperty canEject = new SimpleBooleanProperty(false);
    protected final BooleanProperty isFitted = new SimpleBooleanProperty(false);

    public Extruder(String extruderAxisLetter)
    {
        this.extruderAxisLetter = extruderAxisLetter;
    }

    public String getExtruderAxisLetter()
    {
        return extruderAxisLetter;
    }

    public ReadOnlyBooleanProperty filamentLoadedProperty()
    {
        return filamentLoaded;
    }

    public ReadOnlyBooleanProperty indexWheelStateProperty()
    {
        return filamentLoaded;
    }

    public ReadOnlyBooleanProperty canEjectProperty()
    {
        return canEject;
    }

    public ReadOnlyBooleanProperty isFittedProperty()
    {
        return isFitted;
    }
}
