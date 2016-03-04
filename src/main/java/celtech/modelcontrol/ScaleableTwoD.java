package celtech.modelcontrol;

/**
 *
 * @author ianhudson
 */
public interface ScaleableTwoD extends ContainerOperation
{

    public double getXScale();

    public void setXScale(double scaleFactor);

    public double getYScale();

    public void setYScale(double scaleFactor);
}
