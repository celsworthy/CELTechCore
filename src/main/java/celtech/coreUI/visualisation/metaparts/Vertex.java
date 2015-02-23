package celtech.coreUI.visualisation.metaparts;

import celtech.utils.Math.MathUtils;

/**
 *
 * @author Ian
 */
public class Vertex
{

    private double x;
    private double y;
    private double z;
    private final double epsilon = 1e-12;

    public Vertex(double x, double y, double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX()
    {
        return x;
    }

    public void setX(double x)
    {
        this.x = x;
    }

    public double getY()
    {
        return y;
    }

    public void setY(double y)
    {
        this.y = y;
    }

    public double getZ()
    {
        return z;
    }

    public void setZ(double z)
    {
        this.z = z;
    }

    public boolean equals(Vertex vertex)
    {
        return (MathUtils.compareDouble(this.x, vertex.getX(), epsilon) == MathUtils.EQUAL
            && MathUtils.compareDouble(this.y, vertex.getY(), epsilon) == MathUtils.EQUAL
            && MathUtils.compareDouble(this.z, vertex.getZ(), epsilon) == MathUtils.EQUAL);
    }
}
