/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.postProcessor;

import celtech.configuration.ApplicationConfiguration;
import celtech.gcodetranslator.GCodeRoboxiser;
import celtech.printerControl.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SlicerSettings;
import java.io.File;
import javafx.concurrent.Task;

/**
 *
 * @author Ian
 */
public class PostProcessorTask extends Task<GCodePostProcessingResult>
{
    private String printJobUUID = null;
    private SlicerSettings settings = null;
    private Printer printerToUse = null;

    public PostProcessorTask(String printJobUUID, SlicerSettings settings, Printer printerToUse)
    {
        this.printJobUUID = printJobUUID;
        this.settings = settings;
        this.printerToUse = printerToUse;
    }

    @Override
    protected GCodePostProcessingResult call() throws Exception
    {
        GCodeRoboxiser roboxiser = new GCodeRoboxiser();
        String gcodeFileToProcess = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator + printJobUUID + ApplicationConfiguration.gcodeTempFileExtension;
        String gcodeOutputFile = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator + printJobUUID + ApplicationConfiguration.gcodePostProcessedFileHandle + ApplicationConfiguration.gcodeTempFileExtension;

        boolean success = roboxiser.roboxiseFile(gcodeFileToProcess, gcodeOutputFile, settings);
        
        GCodePostProcessingResult result = new GCodePostProcessingResult(printJobUUID, gcodeOutputFile, printerToUse, success);
        return result;
    }

}
