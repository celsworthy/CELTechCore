package celtech.printerControl.comms.events;

import celtech.comms.remote.RoboxRxPacket;

/**
 *
 * @author Ian
 */
public interface RoboxResponseConsumer
{
    public void processRoboxResponse(RoboxRxPacket rxPacket);
}
