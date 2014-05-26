/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation.modelDisplay;

/**
 *
 * @author ianhudson
 */
public class ModelBounds
{

    double minX = 0;
    double maxX = 0;
    double minY = 0;
    double maxY = 0;
    double minZ = 0;
    double maxZ = 0;

    double width = 0;
    double height = 0;
    double depth = 0;

    double centreX = 0;
    double centreY = 0;
    double centreZ = 0;

    /**
     *
     */
    public ModelBounds()
    {
    }

    /**
     *
     * @param minX
     * @param maxX
     * @param minY
     * @param maxY
     * @param minZ
     * @param maxZ
     * @param width
     * @param height
     * @param depth
     * @param centreX
     * @param centreY
     * @param centreZ
     */
    public ModelBounds(double minX,
            double maxX,
            double minY,
            double maxY,
            double minZ,
            double maxZ,
            double width,
            double height,
            double depth,
            double centreX,
            double centreY,
            double centreZ)
    {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.centreX = centreX;
        this.centreY = centreY;
        this.centreZ = centreZ;
    }

    /**
     *
     * @return
     */
    public double getMinX()
    {
        return minX;
    }

    /**
     *
     * @param minX
     */
    public void setMinX(double minX)
    {
        this.minX = minX;
    }

    /**
     *
     * @return
     */
    public double getMaxX()
    {
        return maxX;
    }

    /**
     *
     * @param maxX
     */
    public void setMaxX(double maxX)
    {
        this.maxX = maxX;
    }

    /**
     *
     * @return
     */
    public double getMinY()
    {
        return minY;
    }

    /**
     *
     * @param minY
     */
    public void setMinY(double minY)
    {
        this.minY = minY;
    }

    /**
     *
     * @return
     */
    public double getMaxY()
    {
        return maxY;
    }

    /**
     *
     * @param maxY
     */
    public void setMaxY(double maxY)
    {
        this.maxY = maxY;
    }

    /**
     *
     * @return
     */
    public double getMinZ()
    {
        return minZ;
    }

    /**
     *
     * @param minZ
     */
    public void setMinZ(double minZ)
    {
        this.minZ = minZ;
    }

    /**
     *
     * @return
     */
    public double getMaxZ()
    {
        return maxZ;
    }

    /**
     *
     * @param maxZ
     */
    public void setMaxZ(double maxZ)
    {
        this.maxZ = maxZ;
    }

    /**
     *
     * @return
     */
    public double getWidth()
    {
        return width;
    }

    /**
     *
     * @param width
     */
    public void setWidth(double width)
    {
        this.width = width;
    }

    /**
     *
     * @return
     */
    public double getHeight()
    {
        return height;
    }

    /**
     *
     * @param height
     */
    public void setHeight(double height)
    {
        this.height = height;
    }

    /**
     *
     * @return
     */
    public double getDepth()
    {
        return depth;
    }

    /**
     *
     * @param depth
     */
    public void setDepth(double depth)
    {
        this.depth = depth;
    }

    /**
     *
     * @return
     */
    public double getCentreX()
    {
        return centreX;
    }

    /**
     *
     * @param centreX
     */
    public void setCentreX(double centreX)
    {
        this.centreX = centreX;
    }

    /**
     *
     * @return
     */
    public double getCentreY()
    {
        return centreY;
    }

    /**
     *
     * @param centreY
     */
    public void setCentreY(double centreY)
    {
        this.centreY = centreY;
    }

    /**
     *
     * @return
     */
    public double getCentreZ()
    {
        return centreZ;
    }

    /**
     *
     * @param centreZ
     */
    public void setCentreZ(double centreZ)
    {
        this.centreZ = centreZ;
    }

    /**
     *
     * @return
     */
    public String toString()
    {
        return "Model bounds {MinX:" + minX
                + " MaxX:" + maxX
                + " MinY:" + minY
                + " MaxY:" + maxY
                + " MinZ:" + minZ
                + " MaxZ:" + maxZ
                + " W:" + width
                + " H:" + height
                + " D:" + depth
                + " Centre X:" + centreX
                + " Y:" + centreY
                + " Z:" + centreZ
                + "}";
    }
}
