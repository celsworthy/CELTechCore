package celtech.printerControl.comms.events;

import celtech.printerControl.comms.commands.rx.FirmwareError;

/**
 *
 * @author Ian
 */
public interface ErrorConsumer
{
    public void consumeError(FirmwareError error);
}
