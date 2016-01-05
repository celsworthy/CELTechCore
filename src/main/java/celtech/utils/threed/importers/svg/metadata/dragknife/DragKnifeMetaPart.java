package celtech.utils.threed.importers.svg.metadata.dragknife;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author ianhudson
 */
public abstract class DragKnifeMetaPart
{

    private final Vector2D start;
    private final Vector2D end;
    private final NumberFormat threeDPformatter;
    private final String comment;

    public DragKnifeMetaPart(double startX, double startY, double endX, double endY, String comment)
    {
        start = new Vector2D(startX, startY);
        end = new Vector2D(endX, endY);

        threeDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        threeDPformatter.setMaximumFractionDigits(3);
        threeDPformatter.setGroupingUsed(false);
        
        this.comment = comment;
    }

    public Vector2D getStart()
    {
        return start;
    }

    public Vector2D getEnd()
    {
        return end;
    }
    
    public String getComment()
    {
        return comment;
    }

    public abstract String renderToGCode();

    protected String generateXYMove(double xValue, double yValue, String comment)
    {
        String generatedOutput = "G1 X" + threeDPformatter.format(xValue)
                + " Y" + threeDPformatter.format(yValue)
                + " ; " + comment;

        return generatedOutput;
    }

    protected String generateXMove(double xValue, String comment)
    {
        String generatedOutput = "G1 X" + threeDPformatter.format(xValue)
                + " ; " + comment;

        return generatedOutput;
    }

    protected String generateYMove(double yValue, String comment)
    {
        String generatedOutput = "G1 Y" + threeDPformatter.format(yValue)
                + " ; " + comment;

        return generatedOutput;
    }
}
