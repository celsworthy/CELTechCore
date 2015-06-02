package celtech.gcodetranslator.postprocessing.nodes;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 *
 * @author Ian
 */
public class NozzleValvePositionNode extends GCodeEventNode
{
    private float desiredValvePosition = -1;

    public float getDesiredValvePosition()
    {
        return desiredValvePosition;
    }

    public void setDesiredValvePosition(float desiredValvePosition)
    {
        this.desiredValvePosition = desiredValvePosition;
    }

    @Override
    public String renderForOutput()
    {
        NumberFormat threeDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        threeDPformatter.setMaximumFractionDigits(3);
        threeDPformatter.setGroupingUsed(false);
        return "B" + desiredValvePosition;
    }
}
