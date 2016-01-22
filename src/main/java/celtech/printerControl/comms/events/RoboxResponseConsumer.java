package celtech.printerControl.comms.events;

import celtech.comms.remote.rx.RoboxRxPacket;

/**
 *
 * @author Ian
 */
public interface RoboxResponseConsumer
{
    public void processRoboxResponse(RoboxRxPacket rxPacket);
}
