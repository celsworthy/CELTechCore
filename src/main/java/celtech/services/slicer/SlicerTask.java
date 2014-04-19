/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.slicer;

import celtech.CoreTest;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.FilamentContainer;
import celtech.coreUI.visualisation.exporters.STLOutputConverter;
import celtech.printerControl.Printer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class SlicerTask extends Task<SliceResult>
{

    private final Stenographer steno = StenographerFactory.getStenographer(SlicerTask.class.getName());
    private String printJobUUID = null;
    private Project project = null;
    private FilamentContainer filament = null;
    private PrintQualityEnumeration printQuality = null;
    private SlicerSettings settings = null;
    private Printer printerToUse = null;
    private String tempModelFilenameWithPath = null;
    private String tempGcodeFilenameWithPath = null;

    public SlicerTask(String printJobUUID, Project project, PrintQualityEnumeration printQuality, SlicerSettings settings, Printer printerToUse)
    {
        this.printJobUUID = printJobUUID;
        this.project = project;
        this.printQuality = printQuality;
        this.settings = settings;
        this.printerToUse = printerToUse;

        tempModelFilenameWithPath = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator + printJobUUID + ApplicationConfiguration.stlTempFileExtension;
        tempGcodeFilenameWithPath = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator + printJobUUID + ApplicationConfiguration.gcodeTempFileExtension;
    }

    @Override
    protected SliceResult call() throws Exception
    {
        boolean succeeded = false;

        String configFile = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator + printJobUUID + ApplicationConfiguration.printProfileFileExtension;

        updateTitle("Slicer");
        updateMessage("Preparing model for conversion");
        updateProgress(0, 100);

        STLOutputConverter outputConverter = new STLOutputConverter(project, printJobUUID);
        outputConverter.outputSTLFile();

        String osName = System.getProperty("os.name");
        ArrayList<String> commands = new ArrayList<>();

        if (osName.equals("Windows 95"))
        {
            commands.add("command.com");
            commands.add("/S");
            commands.add("/C");
            commands.add("\"\"" + ApplicationConfiguration.getApplicationInstallDirectory(CoreTest.class) + "Slic3r\\slic3r-console.exe\" --load \"" + configFile + "\" -o \"" + tempGcodeFilenameWithPath + "\" \"" + tempModelFilenameWithPath + "\"\"");
        } else if (osName.startsWith("Windows"))
        {
            commands.add("cmd.exe");
            commands.add("/S");
            commands.add("/C");
            commands.add("\"\"" + ApplicationConfiguration.getApplicationInstallDirectory(CoreTest.class) + "Slic3r\\slic3r-console.exe\" --load \"" + configFile + "\" -o \"" + tempGcodeFilenameWithPath + "\" \"" + tempModelFilenameWithPath + "\"\"");
        } else if (osName.equals("Mac OS X"))
        {
            commands.add(ApplicationConfiguration.getApplicationInstallDirectory(CoreTest.class) + "Slic3r/slic3r-console");
            commands.add("--load");
            commands.add(configFile);
            commands.add("-o");
            commands.add(tempGcodeFilenameWithPath);
            commands.add(tempModelFilenameWithPath);
        } else
        {
            steno.error("Couldn't determine how to run slicer on " + osName);
        }

        if (commands.size() > 0)
        {
            ProcessBuilder slicerProcessBuilder = new ProcessBuilder(commands);
            Process slicerProcess = null;

            try
            {
                slicerProcess = slicerProcessBuilder.start();
                // any error message?
                StreamGobbler errorGobbler = new StreamGobbler(slicerProcess.getErrorStream(), "ERROR");

                // any output?
                SlicerOutputGobbler outputGobbler = new SlicerOutputGobbler(this, slicerProcess.getInputStream(), "OUTPUT");

                // kick them off
                errorGobbler.start();
                outputGobbler.start();

                int exitStatus = slicerProcess.waitFor();
                switch (exitStatus)
                {
                    case 0:
                        steno.info("Slicer terminated successfully ");
                        succeeded = true;
                        break;
                    default:
                        steno.info("Slicer terminated with unknown exit code " + exitStatus);
                        break;
                }
            } catch (IOException ex)
            {
                steno.error("Exception whilst running slicer: " + ex);
            } catch (InterruptedException ex)
            {
                steno.warning("Interrupted whilst waiting for slicer to complete");
                if (slicerProcess != null)
                {
                    slicerProcess.destroyForcibly();
                }
            }
        } else
        {
            steno.error("Couldn't run autoupdate - no commands for OS " + osName);
        }

        return new SliceResult(printJobUUID, project, filament, printQuality, settings, printerToUse, succeeded);
    }

    protected void progressUpdateFromSlicer(String message, int workDone)
    {
        updateMessage(message);
        updateProgress(workDone, 100);
    }
}
