/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.printerControl.comms;

import celtech.printerControl.comms.events.RoboxEvent;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public interface PrinterControlInterface
{

    /**
     *
     * @param portName
     * @param event
     */
    public void publishEvent(String portName, RoboxEvent event);

    /**
     *
     * @param portName
     */
    public void printerConnected(String portName);

    /**
     *
     * @param portName
     */
    public void failedToConnect(String portName);

    /**
     *
     * @param portName
     */
    public void disconnected(String portName);
    
}
