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
    public void publishEvent(String portName, RoboxEvent event);

    public void printerConnected(String portName);

    public void failedToConnect(String portName);

    public void disconnected(String portName);
    
}
