/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public enum RoboxCommsState
{

    /**
     *
     */
    FOUND,
    /**
     *
     */
    CHECKING_SD_CARD_IS_PRESENT,
    /**
     *
     */
    CHECKING_FIRMWARE,
    /**
     *
     */
    CHECKING_ID,
    /**
     *
     */
    DETERMINING_PRINTER_STATUS,
    /**
     *
     */
    CONNECTED,
    
    DISCONNECTED

}
