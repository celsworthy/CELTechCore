package celtech.printerControl.comms;

import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.rx.RoboxRxPacketFactory;
import celtech.printerControl.comms.commands.rx.RxPacketTypeEnum;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.StatusRequest;

/**
 *
 * @author Ian
 */
public class DummyPrinterCommandInterface extends CommandInterface
{

    public DummyPrinterCommandInterface(PrinterStatusConsumer controlInterface, String portName,
        boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks)
    {
        super(controlInterface, portName, suppressPrinterIDChecks, sleepBetweenStatusChecks);
        this.setName("Dummy Printer");
    }

    @Override
    protected void setSleepBetweenStatusChecks(int sleepMillis)
    {
    }

    @Override
    public RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite) throws RoboxCommsException
    {
        RoboxRxPacket response = null;
        
        if (messageToWrite instanceof StatusRequest)
        {
            response = RoboxRxPacketFactory.createPacket(RxPacketTypeEnum.STATUS_RESPONSE);
        }
        
        return response;
    }

}
