/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.postProcessor;

import celtech.configuration.ApplicationConfiguration;
import celtech.gcodetranslator.GCodeRoboxiser;
import celtech.printerControl.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import java.io.File;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

/**
 *
 * @author Ian
 */
public class PostProcessorTask extends Task<GCodePostProcessingResult> {

    private String printJobUUID = null;
    private RoboxProfile settings = null;
    private Printer printerToUse = null;
    private DoubleProperty taskProgress = new SimpleDoubleProperty(0);

    public PostProcessorTask(String printJobUUID, RoboxProfile settings, Printer printerToUse) {
        this.printJobUUID = printJobUUID;
        this.settings = settings;
        this.printerToUse = printerToUse;
    }

    @Override
    protected GCodePostProcessingResult call() throws Exception {
        updateMessage("");
        updateProgress(0, 100);

        GCodeRoboxiser roboxiser = new GCodeRoboxiser();
        String gcodeFileToProcess = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator + printJobUUID + ApplicationConfiguration.gcodeTempFileExtension;
        String gcodeOutputFile = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator + printJobUUID + ApplicationConfiguration.gcodePostProcessedFileHandle + ApplicationConfiguration.gcodeTempFileExtension;

        taskProgress.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                updateProgress(newValue.doubleValue(), 100.0);
            }
        });
        
        boolean success = roboxiser.roboxiseFile(gcodeFileToProcess, gcodeOutputFile, settings, taskProgress);

        GCodePostProcessingResult result = new GCodePostProcessingResult(printJobUUID, gcodeOutputFile, printerToUse, success);
        return result;
    }

}
