package celtech.printerControl.comms;

import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.GCodeDataResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;

/**
 *
 * @author Ian
 */
public class TestCommandInterface implements CommandInterface
{

    private int tickCounter = 0;

    @Override
    public void setSleepBetweenStatusChecks(int sleepMillis)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public GCodeDataResponse transmitDirectGCode(String gcodeToSend) throws RoboxCommsException
    {
        return null;
    }

    public void tick(int numberOfTicks)
    {
        tickCounter += numberOfTicks;
    }
}
