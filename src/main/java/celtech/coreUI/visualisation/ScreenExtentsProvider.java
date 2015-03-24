package celtech.coreUI.visualisation;

import javafx.geometry.Point2D;

/**
 *
 * @author Ian
 */
public interface ScreenExtentsProvider
{

    public ScreenExtents getScreenExtents();

    public double getTransformedHeight();

    public double getTransformedWidth();

    public double getTransformedDepth();

    public void addShapeChangeListener(ScreenExtentsListener listener);

    public void removeShapeChangeListener(ScreenExtentsListener listener);

    public interface ScreenExtentsListener
    {

        public void screenExtentsChanged(ScreenExtentsProvider screenExtentsProvider);
    }
}
