/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.slicer;

import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.FilamentContainer;
import celtech.configuration.MachineType;
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
    private RoboxProfile settings = null;
    private Printer printerToUse = null;
    private String tempModelFilename = null;
    private String tempGcodeFilename = null;

    /**
     *
     * @param printJobUUID
     * @param project
     * @param printQuality
     * @param settings
     * @param printerToUse
     */
    public SlicerTask(String printJobUUID, Project project, PrintQualityEnumeration printQuality, RoboxProfile settings, Printer printerToUse)
    {
        this.printJobUUID = printJobUUID;
        this.project = project;
        this.printQuality = printQuality;
        this.settings = settings;
        this.printerToUse = printerToUse;

        tempModelFilename = printJobUUID + ApplicationConfiguration.stlTempFileExtension;
        tempGcodeFilename = printJobUUID + ApplicationConfiguration.gcodeTempFileExtension;
    }

    @Override
    protected SliceResult call() throws Exception
    {
        boolean succeeded = false;

        String workingDirectory = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator;
        String configFile = printJobUUID + ApplicationConfiguration.printProfileFileExtension;

        updateTitle("Slicer");
        updateMessage("Preparing model for conversion");
        updateProgress(0, 100);

        STLOutputConverter outputConverter = new STLOutputConverter(project, printJobUUID);
        outputConverter.outputSTLFile();

        MachineType machineType = ApplicationConfiguration.getMachineType();
        ArrayList<String> commands = new ArrayList<>();

        switch (machineType)
        {
            case WINDOWS_95:
                commands.add("command.com");
                commands.add("/S");
                commands.add("/C");
                commands.add("\"\"" + ApplicationConfiguration.getCommonApplicationDirectory() + "Slic3r\\slic3r.exe\" --load " + configFile + " -o " + tempGcodeFilename + " " + tempModelFilename + "\"");
                break;
            case WINDOWS:
                commands.add("cmd.exe");
                commands.add("/S");
                commands.add("/C");
                commands.add("\"\"" + ApplicationConfiguration.getCommonApplicationDirectory() + "Slic3r\\slic3r.exe\" --load " + configFile + " -o " + tempGcodeFilename + " " + tempModelFilename + "\"");
                break;
            case MAC:
                commands.add(ApplicationConfiguration.getCommonApplicationDirectory() + "Slic3r.app/Contents/MacOS/slic3r");
                commands.add("--load");
                commands.add(configFile);
                commands.add("-o");
                commands.add(tempGcodeFilename);
                commands.add(tempModelFilename);
                break;
            case LINUX_X86:
            case LINUX_X64:
                commands.add(ApplicationConfiguration.getCommonApplicationDirectory() + "Slic3r/bin/slic3r");
                commands.add("--load");
                commands.add(configFile);
                commands.add("-o");
                commands.add(tempGcodeFilename);
                commands.add(tempModelFilename);
                break;
            default:
                steno.error("Couldn't determine how to run slicer");
        }

        if (commands.size() > 0)
        {
            ProcessBuilder slicerProcessBuilder = new ProcessBuilder(commands);
            slicerProcessBuilder.directory(new File(workingDirectory));
            
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
            steno.error("Couldn't run autoupdate - no commands for OS ");
        }

        return new SliceResult(printJobUUID, project, filament, printQuality, settings, printerToUse, succeeded);
    }

    /**
     *
     * @param message
     * @param workDone
     */
    protected void progressUpdateFromSlicer(String message, int workDone)
    {
        updateMessage(message);
        updateProgress(workDone, 100);
    }
}
