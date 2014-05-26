/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.events;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class RoboxEventProducer implements RoboxEventProducerInterface
{

    private ExecutorService es = Executors.newSingleThreadExecutor();
    private ArrayList<RoboxEventListener> eventListeners = new ArrayList<RoboxEventListener>();

    /**
     *
     * @param eventListener
     * @return
     */
    @Override
    public boolean addRoboxEventListener(RoboxEventListener eventListener)
    {
        return eventListeners.add(eventListener);
    }

    /**
     *
     * @param eventListener
     * @return
     */
    @Override
    public boolean removeRoboxEventListener(RoboxEventListener eventListener)
    {
        return eventListeners.remove(eventListener);
    }

    /**
     *
     * @param event
     */
    public void publishEvent(final RoboxEvent event)
    {
        for (final RoboxEventListener listener : eventListeners)
        {
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    listener.eventFired(event);
                }
            });

        }
    }

    static class PublishEventCallable implements Callable<Integer>
    {

        private RoboxEventListener listener;
        private RoboxEvent event;

        public PublishEventCallable(RoboxEventListener listener, RoboxEvent event)
        {
            this.listener = listener;
            this.event = event;
        }

        @Override
        public Integer call() throws Exception
        {
            listener.eventFired(event);
            return 0;
        }
    }
}
