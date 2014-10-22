/*
 * Copyright 2014 CEL UK
 */
package celtech.services.slicer;

import celtech.appManager.Project;
import celtech.configuration.fileRepresentation.SlicerParameters;
import celtech.printerControl.model.Printer;
import celtech.services.ControllableService;
import javafx.concurrent.Service;

/**
 *
 * @author tony
 */
public abstract class AbstractSlicerService extends Service<SliceResult> implements
        ControllableService
{
    
    public abstract void setProject(Project project);

    public abstract void setSettings(SlicerParameters settings);
    
    public abstract void setPrintJobUUID(String printJobUUID);
    
    public abstract void setPrinterToUse(Printer printerToUse);

}
