/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.exceptions;

/**
 *
 * @author ianhudson
 */
public class InvalidCommandByteException extends Exception
{

    /**
     * Creates a new instance of
     * <code>RoboxPacketInvalidCommandByteException</code> without detail
     * message.
     */
    public InvalidCommandByteException()
    {
    }

    /**
     * Constructs an instance of
     * <code>RoboxPacketInvalidCommandByteException</code> with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public InvalidCommandByteException(String msg)
    {
        super(msg);
    }
}
