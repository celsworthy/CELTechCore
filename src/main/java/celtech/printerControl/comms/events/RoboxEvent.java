/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.events;

import celtech.printerControl.comms.PrinterID;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;

/**
 *
 * @author Ian Hudson
 * @ Liberty Systems Limited
 */
public class RoboxEvent
{
    private PrinterID sourceOrDestination = new PrinterID();
    private RoboxEventType eventType = null;
    private RoboxRxPacket payload = null;

    public RoboxEvent(RoboxEventType eventType)
    {
//        this.sourceOrDestination = sourceOrDestination;
        this.eventType = eventType;
    }
    
    public RoboxEvent(RoboxEventType eventType, RoboxRxPacket payload)
    {
//        this.sourceOrDestination = sourceOrDestination;
        this.eventType = eventType;
        this.payload = payload;
    } 

    @Override
    public String toString()
    {
        return "Source/Dest=" + sourceOrDestination.toString() + ":" + eventType.name();
    }
    
    public RoboxEventType getEventType()
    {
        return eventType;
    }
    
    public RoboxRxPacket getPayload()
    {
        return payload;
    }
    
    public PrinterID getSourceOrDestination()
    {
        return sourceOrDestination;
    }
}
