
package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public abstract class GCodeParseEvent
{
    private String comment = null;
    private double feedRate = -1;
    private double length = 0;

    /**
     *
     * @return
     */
    public String getComment()
    {
        return comment;
    }

    /**
     *
     * @param comment
     */
    public void setComment(String comment)
    {
        this.comment = comment;
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
        this.feedRate = feedRate;
    }

    /**
     *
     * @return
     */
    public double getLength()
    {
        return length;
    }

    /**
     *
     * @param length
     */
    public void setLength(double length)
    {
        this.length = length;
    }
    
    /**
     *
     * @return
     */
    public abstract String renderForOutput();
}
