package celtech.gcodetranslator.events;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 *
 * @author Ian
 */
public class NozzleChangeBValueEvent extends GCodeParseEvent
{

    private double b;

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
        this.b = b;
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

        String stringToReturn = "G0 B" + threeDPformatter.format(b);

        if (getComment() != null)
        {
            stringToReturn += " ; " + getComment();
        }

        return stringToReturn + "\n";
    }
}
