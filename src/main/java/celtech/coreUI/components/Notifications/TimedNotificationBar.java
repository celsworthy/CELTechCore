package celtech.coreUI.components.Notifications;

import celtech.Lookup;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Ian
 */
public class TimedNotificationBar extends AppearingNotificationBar
{
    private final int displayFor_ms = 4000;
    
    @Override
    public void show()
    {
        Lookup.getNotificationDisplay().addNotificationBar(this);
        startSlidingInToView();
    }

    @Override
    public void finishedSlidingIntoView()
    {
        Timer putItAwayTimer = new Timer("TimedNotificationDisposer", true);
        putItAwayTimer.schedule(new SlideAwayTask(), displayFor_ms);
    }

    @Override
    public void finishedSlidingOutOfView()
    {
        Lookup.getNotificationDisplay().removeNotificationBar(this);
    }
    
    
    private class SlideAwayTask extends TimerTask
    {
        @Override
        public void run()
        {
            startSlidingOutOfView();
        }
    }
}
