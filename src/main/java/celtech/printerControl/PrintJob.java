/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl;

import celtech.configuration.ApplicationConfiguration;
import celtech.gcodetranslator.PrintJobStatistics;
import java.io.File;
import java.io.IOException;

/**
 * A PrintJob represents a print run of a Project, and is associated with a
 * print job directory in the print spool directory.
 *
 * @author Ian
 */
public class PrintJob
{

    private String jobUUID = null;

    private PrintJob(String jobUUID)
    {
        this.jobUUID = jobUUID;
    }

    /**
     * Instantiate a PrintJob from the data in the spool directory
     * @param jobUUID
     * @return 
     */
    public static PrintJob readJobFromDirectory(String jobUUID)
    {
        return new PrintJob(jobUUID);
    }

    /**
     * Get the location of the gcode file as produced by the slicer
     * @return 
     */
    public String getGCodeFileLocation()
    {
        String printjobFilename = ApplicationConfiguration.
            getPrintSpoolDirectory() + jobUUID + File.separator
            + jobUUID
            + ApplicationConfiguration.gcodeTempFileExtension;
        return printjobFilename;
    }

    /**
     * Return if the roboxised file is found in the print spool directory
     * @return 
     */
    public boolean roboxisedFileExists()
    {
        File printJobFile = new File(getRoboxisedFileLocation());
        return printJobFile.exists();
    }

    /**
     * @return the jobUUID
     */
    public String getJobUUID()
    {
        return jobUUID;
    }

    /**
     * Get the location of the roboxised file
     * @return 
     */
    public String getRoboxisedFileLocation()
    {
        return ApplicationConfiguration.getPrintSpoolDirectory() + jobUUID
            + File.separator + jobUUID
            + ApplicationConfiguration.gcodePostProcessedFileHandle
            + ApplicationConfiguration.gcodeTempFileExtension;
    }
    
    /**
     * Get the location of the statistics file
     * @return 
     */
    public String getStatisticsFileLocation() {
        return ApplicationConfiguration.getPrintSpoolDirectory() + jobUUID
            + File.separator + jobUUID
            + ApplicationConfiguration.statisticsFileExtension;
    }

    public PrintJobStatistics getStatistics() throws IOException
    {
        return PrintJobStatistics.readFromFile(getStatisticsFileLocation());
    }

}
