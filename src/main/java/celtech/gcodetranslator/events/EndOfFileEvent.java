package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class EndOfFileEvent extends GCodeParseEvent
{
    @Override
    public String renderForOutput()
    {
        return "; EOF\n";
    }
}
