/*
 * Copyright 2014 CEL UK
 */
package celtech.services.printing;

import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.FilamentContainer;
import celtech.printerControl.Printer;
import celtech.services.slicer.AbstractSlicerService;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import celtech.services.slicer.SliceResult;
import celtech.services.slicer.SlicerTask;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.concurrent.Task;

/**
 * TestSlicerService copies pyramid.gcode into the job directory when Task.call executes.
 *
 * @author tony
 */
public class TestSlicerService extends AbstractSlicerService
{

    private String printJobUUID = null;
    private Project project = null;
    private PrintQualityEnumeration printQuality = null;
    private RoboxProfile settings = null;
    private Printer printerToUse = null;

    /**
     *
     * @param printJobUUID
     */
    public void setPrintJobUUID(String printJobUUID)
    {
        this.printJobUUID = printJobUUID;
    }

    /**
     *
     * @param project
     */
    public void setProject(Project project)
    {
        this.project = project;
    }

    /**
     *
     * @param printQuality
     */
    public void setPrintQuality(PrintQualityEnumeration printQuality)
    {
        this.printQuality = printQuality;
    }

    /**
     *
     * @param settings
     */
    public void setSettings(RoboxProfile settings)
    {
        this.settings = settings;
    }

    /**
     *
     * @param printerToUse
     */
    public void setPrinterToUse(Printer printerToUse)
    {
        this.printerToUse = printerToUse;
    }

    @Override
    protected Task<SliceResult> createTask()
    {
        return new TestSlicerTask(printJobUUID, project, printQuality, settings,
                                  printerToUse);
    }

    /**
     *
     * @return
     */
    @Override
    public boolean cancelRun()
    {
        return cancel();
    }

    class TestSlicerTask extends SlicerTask
    {

        private FilamentContainer filament = null;

        public TestSlicerTask(String printJobUUID, Project project,
                PrintQualityEnumeration printQuality, RoboxProfile settings,
                Printer printerToUse)
        {
            super(printJobUUID, project, printQuality, settings, printerToUse);
        }

        @Override
        /**
         * Copies pyramid.gcode into the job directory when Task.call executes.
         * @return the standard SliceResult 
         */
        protected SliceResult call() throws Exception
        {
            try {
            // copy presliced file to user storage project area
            String workingDirectory = ApplicationConfiguration.getPrintSpoolDirectory()
                    + printJobUUID + File.separator;
            Path destinationFilePath = Paths.get(workingDirectory + printJobUUID
                    + ApplicationConfiguration.gcodeTempFileExtension);
            URL pyramidGCodeURL = this.getClass().getResource("/pyramid.gcode");
            Path sourceFilePath = Paths.get(pyramidGCodeURL.toURI());
            Files.copy(sourceFilePath, destinationFilePath);
            return new SliceResult(printJobUUID, project, filament, printQuality, settings,
                                   printerToUse, true);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("ex" + ex.getMessage());
                return null;
            }
        }
    }

}
