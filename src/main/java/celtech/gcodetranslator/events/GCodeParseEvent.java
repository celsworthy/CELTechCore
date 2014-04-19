
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

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public double getFeedRate()
    {
        return feedRate;
    }

    public void setFeedRate(double feedRate)
    {
        this.feedRate = feedRate;
    }

    public double getLength()
    {
        return length;
    }

    public void setLength(double length)
    {
        this.length = length;
    }
    
    public abstract String renderForOutput();
}
