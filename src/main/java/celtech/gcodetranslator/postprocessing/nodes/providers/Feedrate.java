package celtech.gcodetranslator.postprocessing.nodes.providers;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 *
 * @author Ian
 */
public final class Feedrate implements Renderable
{
    private boolean isFeedRateSet = false;
    private double feedRate_mmPerMin = 0;
    private double feedRate_mmPerSec = 0;
    
    public boolean isFeedrateSet()
    {
        return isFeedRateSet;
    }

    /**
     * Feedrate is in mm per minute
     *
     * @return
     */
    public double getFeedRate_mmPerMin()
    {
        return feedRate_mmPerMin;
    }

    /**
     * Feedrate in mm per second
     *
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
    @Override
    public String renderForOutput()
    {
        NumberFormat threeDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        threeDPformatter.setMaximumFractionDigits(3);
        threeDPformatter.setGroupingUsed(false);

        StringBuilder stringToReturn = new StringBuilder();

        if (isFeedRateSet)
        {
            stringToReturn.append('F');
            stringToReturn.append(threeDPformatter.format(feedRate_mmPerMin));
        }

        return stringToReturn.toString().trim();
    }
    
    public Feedrate clone()
    {
        Feedrate newNode = new Feedrate();
        
        newNode.feedRate_mmPerMin = this.feedRate_mmPerMin;
        newNode.feedRate_mmPerSec = this.feedRate_mmPerSec;
        newNode.isFeedRateSet = this.isFeedRateSet;
        
        return newNode;
    }
}
