package celtech.gcodetranslator.events;

import celtech.gcodetranslator.Nozzle;
import java.io.IOException;

/**
 *
 * @author Ian
 */
public class NozzleOpenFullyEvent extends GCodeParseEvent
{
    @Override
    public String renderForOutput()
    {
        String stringToReturn = "G1 B1.0";

        if (getComment() != null)
        {
            stringToReturn += " ; " + getComment();
        }

        return stringToReturn + "\n";
    }
}
