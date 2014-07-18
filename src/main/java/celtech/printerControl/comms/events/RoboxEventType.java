/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.events;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public enum RoboxEventType
{

    /**
     *
     */
    PRINTER_CONNECTED,

    /**
     *
     */
    PRINTER_ONLINE,

    /**
     *
     */
    PRINTER_DISCONNECTED,

    /**
     *
     */
    PRINTER_OFFLINE,

    /**
     *
     */
    PRINTER_STATUS_UPDATE,

    /**
     *
     */
    PRINTER_ACK,

    /**
     *
     */
    FIRMWARE_VERSION_INFO,

    /**
     *
     */
    PRINTER_ID_INFO,

    /**
     *
     */
    PRINTER_COMMS_ERROR,

    /**
     *
     */
    PRINTER_INVALID_RESPONSE,

    /**
     *
     */
    REEL_EEPROM_DATA,

    /**
     *
     */
    HEAD_EEPROM_DATA
}
