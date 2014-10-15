/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model;

import celtech.Lookup;
import celtech.appManager.SystemNotificationManager;

/**
 * This test class is used to provide an implementation of Printer for which it is easy
 * to add/remove the head and reels.
 * @author tony
 */
public class TestPrinterForListeners extends Printer
{
    
    public TestPrinterForListeners() {
        super(null, null);
        SystemNotificationManager mockSystemNotificationManager = new SystemNotificationManager();
        Lookup.setSystemNotificationHandler(mockSystemNotificationManager);
        
    }
    
}
