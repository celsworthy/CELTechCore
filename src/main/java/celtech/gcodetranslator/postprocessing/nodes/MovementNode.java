package celtech.gcodetranslator.postprocessing.nodes;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Ian
 */
public abstract class MovementNode extends CommentableNode
{

    private boolean isXSet = false;
    private double x;
    
    private boolean isYSet = false;
    private double y;
    
    private boolean isZSet = false;
    private double z;

    private boolean isFeedRateSet = false;
    private double feedRate_mmPerMin = 0;
    private double feedRate_mmPerSec = 0;

    private boolean isESet = false;
    private float e = 0;
    private boolean isDSet = false;
    private float d = 0;

    /**
     *
     * @return
     */
    public double getX()
    {
        return x;
    }

    /**
     *
     * @param x
     */
    public void setX(double x)
    {
        isXSet = true;
        this.x = x;
    }

    /**
     *
     * @return
     */
    public double getY()
    {
        return y;
    }

    /**
     *
     * @param y
     */
    public void setY(double y)
    {
        isYSet = true;
        this.y = y;
    }

    /**
     *
     * @return
     */
    public double getZ()
    {
        return z;
    }

    /**
     *
     * @param z
     */
    public void setZ(double z)
    {
        isZSet = true;
        this.z = z;
    }

    /**
     * Feedrate is in mm per minute
     * @return
     */
    public double getFeedRate_mmPerMin()
    {
        return feedRate_mmPerMin;
    }

    /**
     * Feedrate in mm per second
     * @return
     */
    public double getFeedRate_mmPerSec()
    {
        return feedRate_mmPerSec;
    }

    /**
     *
     * @param feedRate_mmPerMin
     */
    public void setFeedRate_mmPerMin(double feedRate_mmPerMin)
    {
        isFeedRateSet = true;
        this.feedRate_mmPerMin = feedRate_mmPerMin;
        this.feedRate_mmPerSec = feedRate_mmPerMin / 60;
    }

    /**
     *
     * @param feedRate_mmPerSec
     */
    public void setFeedRate_mmPerSec(double feedRate_mmPerSec)
    {
        isFeedRateSet = true;
        this.feedRate_mmPerSec = feedRate_mmPerSec;
        this.feedRate_mmPerMin = feedRate_mmPerSec * 60;
    }

    /**
     *
     * @return
     */
    public float getE()
    {
        return e;
    }

    /**
     *
     * @param value
     */
    public void setE(float value)
    {
        isESet = true;
        this.e = value;
    }
    
    public boolean isEInUse()
    {
        return isESet;
    }
    
    public void eNotInUse()
    {
        this.e = 0;
        isESet = false;
    }

    public boolean isDInUse()
    {
        return isDSet;
    }
    
    public void dNotInUse()
    {
        this.d = 0;
        isDSet = false;
    }

    /**
     *
     * @return
     */
    public float getD()
    {
        isDSet = true;
        return d;
    }

    /**
     *
     * @param value
     */
    public void setD(float value)
    {
        this.d = value;
    }

    /**
     *
     * @return
     */
    @Override
    public String renderForOutput()
    {
        NumberFormat threeDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        threeDPformatter.setMaximumFractionDigits(3);
        threeDPformatter.setGroupingUsed(false);

        NumberFormat fiveDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        fiveDPformatter.setMaximumFractionDigits(5);
        fiveDPformatter.setGroupingUsed(false);

        StringBuilder stringToReturn = new StringBuilder();

        if (isFeedRateSet)
        {
            stringToReturn.append('F');
            stringToReturn.append(threeDPformatter.format(feedRate_mmPerMin));
            stringToReturn.append(' ');
        }

        if (isXSet)
        {
            stringToReturn.append('X');
            stringToReturn.append(threeDPformatter.format(x));
            stringToReturn.append(' ');
        }
        
        if (isYSet)
        {
            stringToReturn.append('Y');
            stringToReturn.append(threeDPformatter.format(y));
            stringToReturn.append(' ');
        }

        if (isZSet)
        {
            stringToReturn.append('Z');
            stringToReturn.append(threeDPformatter.format(z));
            stringToReturn.append(' ');
        }

        if (isDSet)
        {
            stringToReturn.append('D');
            stringToReturn.append(fiveDPformatter.format(d));
            stringToReturn.append(' ');
        }

        if (isESet)
        {
            stringToReturn.append('E');
            stringToReturn.append(fiveDPformatter.format(e));
        }

        return stringToReturn.toString();
    }
    
    public double timeToReach(MovementNode destinationNode)
    {
        Vector2D source = new Vector2D(x, y);
        Vector2D destination = new Vector2D(destinationNode.getX(), destinationNode.getY());
        
        double distance = source.distance(destination);
        
        double time = distance / feedRate_mmPerSec;
        
        return time;
    }
    
    public Vector2D toVector2D()
    {
        return new Vector2D(x, y);
    }
}
