package celtech.coreUI.keycommands;

import celtech.printerControl.comms.RoboxCommsManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class HiddenKey
{

    private Stenographer steno = StenographerFactory.getStenographer(HiddenKey.class.getName());
    private boolean captureKeys = false;
    private ArrayList<String> commandSequences = new ArrayList<>();
    private ArrayList<KeyCommandListener> keyCommandListeners = new ArrayList<>();
    private String hiddenCommandKeyBuffer = "";

    private final EventHandler<KeyEvent> hiddenCommandEventHandler = (KeyEvent event) ->
    {
        steno.info("Got character " + event.getCharacter());
        boolean matchedOne = false;
        for (String commandSequence : commandSequences)
        {            if (commandSequence.equals(hiddenCommandKeyBuffer + event.
                getCharacter()))
            {
                hiddenCommandKeyBuffer = "";
                triggerListeners(commandSequence);
                break;
            } else if (commandSequence.startsWith(hiddenCommandKeyBuffer + event.
                getCharacter()))
            {
                matchedOne = true;
            }
        }

        if (matchedOne)
        {
            hiddenCommandKeyBuffer += event.getCharacter();
        }
        else
        {
            hiddenCommandKeyBuffer = "";
        }
    };

    public void stopCapturingHiddenKeys(Scene scene)
    {
        if (captureKeys)
        {
            scene.removeEventHandler(KeyEvent.KEY_TYPED, hiddenCommandEventHandler);
            hiddenCommandKeyBuffer = "";
            captureKeys = false;
        }
    }

    public void captureHiddenKeys(Scene scene)
    {
        if (!captureKeys)
        {
            scene.addEventHandler(KeyEvent.KEY_TYPED, hiddenCommandEventHandler);
            captureKeys = true;
        }
    }

    public void addCommandSequence(String commandSequence)
    {
        commandSequences.add(commandSequence);
    }

    public void addKeyCommandListener(KeyCommandListener listener)
    {
        keyCommandListeners.add(listener);
    }

    private void triggerListeners(String commandSequence)
    {
        for (KeyCommandListener listener : keyCommandListeners)
        {
            listener.trigger(commandSequence);
        }
    }
}
