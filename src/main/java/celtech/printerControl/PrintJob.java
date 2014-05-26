/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl;

import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;

/**
 *
 * @author Ian
 */
public class PrintJob
{

    private String printUUID = null;
    private PrintQualityEnumeration printQuality = null;
    private RoboxProfile settings = null;

    /**
     *
     * @param printUUID
     * @param printQuality
     * @param settings
     */
    public PrintJob(String printUUID, PrintQualityEnumeration printQuality, RoboxProfile settings)
    {
        this.printUUID = printUUID;
        this.printQuality = printQuality;
        this.settings = settings;
    }

}
