/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.events;

/**
 *
 * @author Ian Hudson
 * @ Liberty Systems Limited
 */
public interface RoboxEventProducerInterface
{
    public boolean addRoboxEventListener(RoboxEventListener eventListener);

    public boolean removeRoboxEventListener(RoboxEventListener eventListener);
}
