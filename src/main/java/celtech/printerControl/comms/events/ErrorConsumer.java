package celtech.printerControl.comms.events;

import celtech.comms.remote.rx.FirmwareError;

/**
 *
 * @author Ian
 */
public interface ErrorConsumer
{
    public void consumeError(FirmwareError error);
}
