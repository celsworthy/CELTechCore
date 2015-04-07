package celtech.gcodetranslator;

import celtech.gcodetranslator.events.GCodeParseEvent;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class GCodeRoboxiser2 extends GCodeRoboxisingEngine
{
    private final Stenographer steno = StenographerFactory.getStenographer(
        GCodeRoboxiser2.class.getName());

    @Override
    public void processEvent(GCodeParseEvent event) throws PostProcessingError
    {
    }

    @Override
    public void unableToParse(String line)
    {
    }    
}
