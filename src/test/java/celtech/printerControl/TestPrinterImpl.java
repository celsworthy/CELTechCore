/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl;

import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.StatusResponse;

/**
 * TestPrinterImpl extends PrinterImpl and stubs out the GUI interaction.
 *
 * @author tony
 */
public class TestPrinterImpl extends PrinterImpl
{

    private boolean headFormatted = false;

    public TestPrinterImpl(String portName, RoboxCommsManager commsManager)
    {
        super(portName, commsManager);
    }

    @Override
    void initialiseSDDialog()
    {
    }

    @Override
    void showSDDialogIfNotShowing(StatusResponse statusResponse)
    {
    }

    @Override
    public StatusResponse transmitStatusRequest() throws RoboxCommsException
    {
        return new TestStatusResponse();
    }

    @Override
    public AckResponse transmitFormatHeadEEPROM() throws RoboxCommsException
    {
        headFormatted = true;
        return null;
    }

    /**
     * @return the headFormatted
     */
    public boolean isHeadFormatted()
    {
        return headFormatted;
    }

}
