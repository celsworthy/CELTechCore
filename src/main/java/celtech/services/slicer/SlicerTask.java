package celtech.services.slicer;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.MachineType;
import celtech.configuration.SlicerType;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.configuration.fileRepresentation.SlicerParameters;
import celtech.coreUI.visualisation.exporters.STLOutputConverter;
import celtech.printerControl.model.Printer;
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
    private SlicerParameters settings = null;
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
    public SlicerTask(String printJobUUID, Project project, PrintQualityEnumeration printQuality, SlicerParameters settings, Printer printerToUse)
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

        SlicerType slicerType = Lookup.getUserPreferences().getSlicerType();
        String windowsSlicerCommand = null;
        String macSlicerCommand = null;
        String linuxSlicerCommand = null;
        String configLoadCommand = null;
        String combinedConfigSection = null;

        switch (slicerType)
        {
            case Slic3r:
                windowsSlicerCommand = "\"" + ApplicationConfiguration.getCommonApplicationDirectory() + "Slic3r\\slic3r.exe\"";
                macSlicerCommand = "Slic3r.app/Contents/MacOS/slic3r";
                linuxSlicerCommand = "Slic3r/bin/slic3r";
                configLoadCommand = "--load";
                combinedConfigSection = configLoadCommand + " " + configFile;
                break;
            case Cura:
                windowsSlicerCommand = "\"" + ApplicationConfiguration.getCommonApplicationDirectory() + "Cura\\CuraEngine.exe\"";
                macSlicerCommand = "?";
                linuxSlicerCommand = "Cura/bin/CuraEngine";
                configLoadCommand = "-c";
                configFile = "\"" + ApplicationConfiguration.getUserPrintProfileDirectory() + "curaProfile.ini\"";
                combinedConfigSection = configLoadCommand + " " + configFile;
                break;
        }

        steno.info("Selected slicer is " + slicerType);

        switch (machineType)
        {
            case WINDOWS_95:
                commands.add("command.com");
                commands.add("/S");
                commands.add("/C");
                commands.add("\"pushd \"" + workingDirectory + "\" && "
                    + windowsSlicerCommand + " " + combinedConfigSection + " -o "
                    + tempGcodeFilename + " " + tempModelFilename
                    + " && popd\"");
                break;
            case WINDOWS:
                commands.add("cmd.exe");
                commands.add("/S");
                commands.add("/C");
                commands.add("\"pushd \"" + workingDirectory + "\" && "
                    + windowsSlicerCommand + " " + combinedConfigSection + " -o "
                    + tempGcodeFilename + " " + tempModelFilename
                    + " && popd\"");
                break;
            case MAC:
                commands.add(ApplicationConfiguration.getCommonApplicationDirectory() + macSlicerCommand);
                commands.add(configLoadCommand);
                commands.add(configFile);
                commands.add("-o");
                commands.add(tempGcodeFilename);
                commands.add(tempModelFilename);
                break;
            case LINUX_X86:
            case LINUX_X64:
                commands.add(ApplicationConfiguration.getCommonApplicationDirectory() + linuxSlicerCommand);
                commands.add(configLoadCommand);
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
            if (machineType != MachineType.WINDOWS && machineType != MachineType.WINDOWS_95)
            {
                slicerProcessBuilder.directory(new File(workingDirectory));
            }

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
