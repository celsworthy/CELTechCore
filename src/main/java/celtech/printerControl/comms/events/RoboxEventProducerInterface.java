/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.events;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public interface RoboxEventProducerInterface
{

    /**
     *
     * @param eventListener
     * @return
     */
    public boolean addRoboxEventListener(RoboxEventListener eventListener);

    /**
     *
     * @param eventListener
     * @return
     */
    public boolean removeRoboxEventListener(RoboxEventListener eventListener);
}
