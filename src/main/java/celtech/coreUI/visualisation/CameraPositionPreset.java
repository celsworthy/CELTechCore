/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation;

import celtech.configuration.PrintBed;
import javafx.geometry.Point3D;

/**
 *
 * @author ianhudson
 */
public enum CameraPositionPreset
{

    /**
     *
     */
    TOP(0, 89, 400, PrintBed.getPrintVolumeCentreZeroHeight()),

    /**
     *
     */
    FRONT(0, 30, 400, PrintBed.getPrintVolumeCentreZeroHeight()),

    /**
     *
     */
    LEFT(90, 30, 400, PrintBed.getPrintVolumeCentreZeroHeight()),

    /**
     *
     */
    RIGHT(-90, 30, 400, PrintBed.getPrintVolumeCentreZeroHeight()),

    /**
     *
     */
    VIEW_OVER_OUTLINE(337, 20, 580, new Point3D(76.69f, 92.61f, -93.08f));

    private final float azimuth;
    private final float elevation;
    private final float distance;
    private final Point3D pointToLookAt;

    CameraPositionPreset(float azimuth, float elevation, float distance, Point3D pointToLookAt)
    {
        this.azimuth = azimuth;
        this.elevation = elevation;
        this.distance = distance;
        this.pointToLookAt = pointToLookAt;
    }

    /**
     *
     * @return
     */
    public float getAzimuth()
    {
        return azimuth;
    }

    /**
     *
     * @return
     */
    public float getElevation()
    {
        return elevation;
    }

    /**
     *
     * @return
     */
    public float getDistance()
    {
        return distance;
    }

    /**
     *
     * @return
     */
    public Point3D getPointToLookAt()
    {
        return pointToLookAt;
    }
}
