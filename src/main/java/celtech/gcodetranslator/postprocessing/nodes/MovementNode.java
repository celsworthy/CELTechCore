package celtech.gcodetranslator.postprocessing.nodes;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

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
    private double feedRate = 0;

    private boolean isESet = false;
    private double e = 0;
    private boolean isDSet = false;
    private double d = 0;

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
     *
     * @return
     */
    public double getFeedRate()
    {
        return feedRate;
    }

    /**
     *
     * @param feedRate
     */
    public void setFeedRate(double feedRate)
    {
        isFeedRateSet = true;
        this.feedRate = feedRate;
    }

    /**
     *
     * @return
     */
    public double getE()
    {
        return e;
    }

    /**
     *
     * @param value
     */
    public void setE(double value)
    {
        isESet = true;
        this.e = value;
    }

    /**
     *
     * @return
     */
    public double getD()
    {
        isDSet = true;
        return d;
    }

    /**
     *
     * @param value
     */
    public void setD(double value)
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
            stringToReturn.append(threeDPformatter.format(feedRate));
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
            stringToReturn.append(' ');
        }

        // Allow comments to be added
        stringToReturn.append(super.renderForOutput());

        return stringToReturn.toString();
    }
}
