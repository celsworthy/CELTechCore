package celtech.printerControl.comms;

import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.GCodeDataResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacketFactory;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;
import celtech.utils.SystemUtils;

/**
 *
 * @author Ian
 */
public class HardwareCommandInterface implements CommandInterface
{
    private PrinterHandler printerHandler;
    
    private HardwareCommandInterface()
    {        
    }
    
    public HardwareCommandInterface(PrinterHandler printerHandler)
    {
        this.printerHandler = printerHandler;
    }

    @Override
    public void setSleepBetweenStatusChecks(int sleepMillis)
    {
        printerHandler.setSleepBetweenStatusChecks(sleepMillis);
    }

    @Override
    public RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite) throws RoboxCommsException
    {
        return printerHandler.writeToPrinter(messageToWrite);
    }

    /**
     *
     * @param gcodeToSend
     * @return
     * @throws RoboxCommsException
     */
    @Override
    public GCodeDataResponse transmitDirectGCode(final String gcodeToSend) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.EXECUTE_GCODE);

        String gcodeToSendWithLF = SystemUtils.cleanGCodeForTransmission(gcodeToSend) + "\n";

        gcodePacket.setMessagePayload(gcodeToSendWithLF);

        GCodeDataResponse response = (GCodeDataResponse) writeToPrinter(gcodePacket);

        return response;
    }
}
