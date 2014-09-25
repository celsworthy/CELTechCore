/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms;

import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.GCodeDataResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;

/**
 *
 * @author Ian
 */
public interface CommandInterface
{
    public void setSleepBetweenStatusChecks(int sleepMillis);

    public RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite) throws RoboxCommsException;

    /**
     *
     * @param gcodeToSend
     * @return
     * @throws RoboxCommsException
     */
    public GCodeDataResponse transmitDirectGCode(String gcodeToSend) throws RoboxCommsException;
}
