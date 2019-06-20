package celtech.appManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 *
 * @author tony aldhous
 */

public class StylusSettings 
{
    private boolean hasDragKnife = false;
    private double dragKnifeRadius = 1.0;
    private double xOffset = 0.0;
    private double yOffset = 0.0;
    private double zOffset = 0.0;
    private final BooleanProperty dataChanged = new SimpleBooleanProperty(false);

    StylusSettings()
    {
    }

    public boolean getHasDragKnife()
    {
        return hasDragKnife;
    }

    public void setHasDragKnife(boolean hasDragKnife)
    {
        if (this.hasDragKnife != hasDragKnife)
        {
            this.hasDragKnife = hasDragKnife;
            toggleDataChanged();
        }
    }

    public double getDragKnifeRadius()
    {
        return dragKnifeRadius;
    }

    public void setDragKnifeRadius(double dragKnifeRadius)
    {
        if (this.dragKnifeRadius != dragKnifeRadius)
        {
            this.dragKnifeRadius = dragKnifeRadius;
            toggleDataChanged();
        }
    }

    public double getXOffset()
    {
        return xOffset;
    }

    public void setXOffset(double xOffset)
    {
        if (this.xOffset != xOffset)
        {
            this.xOffset = xOffset;
            toggleDataChanged();
        }
    }

    public double getYOffset()
    {
        return yOffset;
    }

    public void setYOffset(double yOffset)
    {
        if (this.yOffset != yOffset)
        {
            this.yOffset = yOffset;
            toggleDataChanged();
        }
    }

    public double getZOffset()
    {
        return zOffset;
    }

    public void setZOffset(double zOffset)
    {
        if (this.zOffset != zOffset)
        {
            this.zOffset = zOffset;
            toggleDataChanged();
        }
    }

    public void setOffsets(double xOffset, double yOffset, double zOffset)
    {
        if (this.xOffset != xOffset ||
            this.yOffset != yOffset ||
            this.zOffset != zOffset)
        {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.zOffset = zOffset;
            toggleDataChanged();
        }
    }

    public ReadOnlyBooleanProperty getDataChanged()
    {
        return dataChanged;
    }

    private void toggleDataChanged()
    {
        dataChanged.set(dataChanged.not().get());
    }
}
