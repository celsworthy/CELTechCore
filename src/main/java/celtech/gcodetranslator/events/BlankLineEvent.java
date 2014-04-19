package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class BlankLineEvent extends GCodeParseEvent
{
    @Override
    public String renderForOutput()
    {
        return "\n";
    }
}
