/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl;

import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SlicerSettings;

/**
 *
 * @author Ian
 */
public class PrintJob
{

    private String printUUID = null;
    private PrintQualityEnumeration printQuality = null;
    private SlicerSettings settings = null;

    public PrintJob(String printUUID, PrintQualityEnumeration printQuality, SlicerSettings settings)
    {
        this.printUUID = printUUID;
        this.printQuality = printQuality;
        this.settings = settings;
    }

}
