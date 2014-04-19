
package celtech.gcodetranslator;

import celtech.gcodetranslator.events.GCodeParseEvent;

/**
 *
 * @author Ian
 */
public interface GCodeTranslationEventHandler
{    
    public void processEvent(GCodeParseEvent event) throws NozzleCloseSettingsError;
    public void unableToParse(String line);
}
