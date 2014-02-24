/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.configuration;

import celtech.coreUI.visualisation.modelDisplay.ModelBounds;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point3D;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class PrintBed
{

    private final String bedOuterModelName = "bedOuter.j3o";
    private final String bedInnerModelName = "bedInner.j3o";
    private static PrintBed instance = null;
    //The origin of the print volume is the front left corner
    // X is right
    // Y is down
    // Z is into the screen
    private static final float maxPrintableXSize = 210;
    private static final float maxPrintableZSize = 150;
    private static final float maxPrintableYSize = -100;
    private static final Point3D printVolumeMaximums = new Point3D(maxPrintableXSize, 0, maxPrintableZSize);
    private static final Point3D printVolumeMinimums = new Point3D(0, maxPrintableYSize, 0);
    private static final Point3D centre = new Point3D(maxPrintableXSize / 2, maxPrintableYSize / 2, maxPrintableZSize / 2);
    private static final Point3D centreZeroHeight = new Point3D(maxPrintableXSize / 2, 0, maxPrintableZSize / 2);
    private BoundingBox printVolumeBoundingBox = null;
    private BoundingBox printBedBounds = null;
    private Stenographer steno = null;

    private PrintBed()
    {
        steno = StenographerFactory.getStenographer(this.getClass().getName());

        printVolumeBoundingBox = new BoundingBox(0,0,0, maxPrintableXSize, maxPrintableYSize, maxPrintableZSize);
        steno.debug("Print volume bounds " + printVolumeBoundingBox);
    }

    public static PrintBed getInstance()
    {
        if (instance == null)
        {
            instance = new PrintBed();
        }

        return instance;
    }

    public Point3D getPrintVolumeMinimums()
    {
        return printVolumeMinimums;
    }

    public Point3D getPrintVolumeMaximums()
    {
        return printVolumeMaximums;
    }

    public String getBedOuterModelName()
    {
        return bedOuterModelName;
    }

    public String getBedInnerModelName()
    {
        return bedInnerModelName;
    }

    public static Point3D getPrintVolumeCentre()
    {
        return centre;
    }
    
    public static Point3D getPrintVolumeCentreZeroHeight()
    {
        return centreZeroHeight;
    }

    public BoundingBox getPrintVolumeBounds()
    {
        return printVolumeBoundingBox;
    }

    public BoundingBox getBedBounds()
    {
        return printBedBounds;
    }
    
    public static boolean isBiggerThanPrintVolume(ModelBounds bounds)
    {
        boolean biggerThanPrintArea = false;

        double xSize = bounds.getWidth();
        double ySize = bounds.getHeight();
        double zSize = bounds.getDepth();

        if (xSize > maxPrintableXSize
                || ySize > -maxPrintableYSize
                || zSize > maxPrintableZSize)
        {
            biggerThanPrintArea = true;
        }

        return biggerThanPrintArea;
    }
}
