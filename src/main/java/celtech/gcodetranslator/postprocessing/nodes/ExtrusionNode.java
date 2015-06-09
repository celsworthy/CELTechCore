package celtech.gcodetranslator.postprocessing.nodes;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 *
 * @author Ian
 */
public class ExtrusionNode extends MovementNode
{

    private boolean isBSet = false;
    private double b;

    /**
     *
     * @return
     */
    public boolean isBSet()
    {
        return isBSet;
    }

    /**
     *
     * @return
     */
    public double getB()
    {
        return b;
    }

    /**
     *
     * @param b
     */
    public void setB(double b)
    {
        isBSet = true;
        this.b = b;
    }

    //Extrusion events should always use G1
    @Override
    public String renderForOutput()
    {
        NumberFormat twoDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        twoDPformatter.setMaximumFractionDigits(2);
        twoDPformatter.setGroupingUsed(false);

        StringBuilder stringToReturn = new StringBuilder();

        stringToReturn.append("G1 ");

        stringToReturn.append(super.renderForOutput());

        if (isBSet)
        {
            stringToReturn.append(" B");
            stringToReturn.append(twoDPformatter.format(b));
        }

        stringToReturn.append(renderComments());

        return stringToReturn.toString();
    }

    public void extrudeUsingEOnly()
    {
        setE(getE() + getD());
        dNotInUse();
    }

    public void extrudeUsingDOnly()
    {
        setD(getE() + getD());
        eNotInUse();
    }

    public ExtrusionNode clone()
    {
        ExtrusionNode returnedNode = new ExtrusionNode();

        if (isBSet)
        {
            returnedNode.setB(b);
        }

        if (super.isDInUse())
        {
            returnedNode.setD(super.getD());
        }

        if (super.isEInUse())
        {
            returnedNode.setE(super.getE());
        }

        if (super.isFeedrateSet())
        {
            returnedNode.setFeedRate_mmPerMin(super.getFeedRate_mmPerMin());
        }

        if (super.isXSet())
        {
            returnedNode.setX(super.getX());
        }
        if (super.isYSet())
        {
            returnedNode.setY(super.getY());
        }
        if (super.isZSet())
        {
            returnedNode.setZ(super.getZ());
        }

        return returnedNode;
    }
}
