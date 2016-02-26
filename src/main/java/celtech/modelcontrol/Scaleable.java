package celtech.modelcontrol;

/**
 *
 * @author ianhudson
 */
public interface Scaleable
{

    public double getXScale();

    public void setXScale(double scaleFactor);

    public double getYScale();

    public void setYScale(double scaleFactor);

    public double getZScale();

    public void setZScale(double scaleFactor);
}
