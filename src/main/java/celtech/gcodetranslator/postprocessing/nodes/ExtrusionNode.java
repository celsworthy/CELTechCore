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
}
