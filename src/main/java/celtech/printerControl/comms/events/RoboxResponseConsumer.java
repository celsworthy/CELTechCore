package celtech.printerControl.comms.events;

import celtech.printerControl.comms.commands.rx.RoboxRxPacket;

/**
 *
 * @author Ian
 */
public interface RoboxResponseConsumer
{
    public void processRoboxResponse(RoboxRxPacket rxPacket);
}
