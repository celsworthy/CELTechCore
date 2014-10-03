package celtech.printerControl.model;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.SimpleFloatProperty;

/**
 *
 * @author ianhudson
 */
public class Nozzle implements Cloneable
{
    protected final FloatProperty BPosition = new SimpleFloatProperty(0);

    protected final FloatProperty diameter = new SimpleFloatProperty(0);
    protected final FloatProperty xOffset = new SimpleFloatProperty(0);
    protected final FloatProperty yOffset = new SimpleFloatProperty(0);
    protected final FloatProperty zOffset = new SimpleFloatProperty(0);
    protected final FloatProperty bOffset = new SimpleFloatProperty(0);

    /**
     * @param diameter
     * @param xOffset
     * @param yOffset
     * @param zOffset
     * @param bOffset
     */
    public Nozzle(float diameter,
        float xOffset,
        float yOffset,
        float zOffset,
        float bOffset)
    {
        this.diameter.set(diameter);
        this.xOffset.set(xOffset);
        this.yOffset.set(yOffset);
        this.zOffset.set(zOffset);
        this.bOffset.set(bOffset);
    }

    /**
     *
     * @return
     */
    public ReadOnlyFloatProperty getBPositionProperty()
    {
        return BPosition;
    }

    /**
     *
     * @return
     */
    public ReadOnlyFloatProperty getDiameterProperty()
    {
        return diameter;
    }

    /**
     *
     * @return
     */
    public ReadOnlyFloatProperty getXOffsetProperty()
    {
        return xOffset;
    }

    /**
     *
     * @return
     */
    public ReadOnlyFloatProperty getYOffsetProperty()
    {
        return yOffset;
    }

    /**
     *
     * @return
     */
    public ReadOnlyFloatProperty getZOffsetProperty()
    {
        return zOffset;
    }

    /**
     *
     * @return
     */
    public ReadOnlyFloatProperty getBOffsetProperty()
    {
        return bOffset;
    }

    /**
     *
     * @return
     */
    @Override
    public Nozzle clone()
    {
        Nozzle clone = new Nozzle(
            diameter.floatValue(),
            xOffset.floatValue(),
            yOffset.floatValue(),
            zOffset.floatValue(),
            bOffset.floatValue()
        );

        return clone;
    }
}
