package celtech.services.slicer;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.MachineType;
import celtech.configuration.SlicerType;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.utils.threed.exporters.STLOutputConverter;
import celtech.printerControl.model.Printer;
import celtech.utils.threed.ThreeDUtils;
import celtech.utils.threed.exporters.MeshFileOutputConverter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author ianhudson
 */
public class SlicerTask extends Task<SliceResult> implements ProgressReceiver
{

    private final Stenographer steno = StenographerFactory.getStenographer(SlicerTask.class.
            getName());
    private String printJobUUID = null;
    private String printJobDirectory;
    private Project project = null;
    private PrintQualityEnumeration printQuality = null;
    private SlicerParametersFile settings = null;
    private Printer printerToUse = null;

    public SlicerTask(String printJobUUID, Project project, PrintQualityEnumeration printQuality,
            SlicerParametersFile settings, Printer printerToUse)
    {
        this.printJobUUID = printJobUUID;
        this.printJobDirectory = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID
                + File.separator;
        this.project = project;
        this.printQuality = printQuality;
        this.settings = settings;
        this.printerToUse = printerToUse;
        updateProgress(0.0, 100.0);
    }

    public SlicerTask(String printJobUUID,
            String printJobDirectory,
            Project project, PrintQualityEnumeration printQuality,
            SlicerParametersFile settings, Printer printerToUse)
    {
        this.printJobUUID = printJobUUID;
        this.printJobDirectory = printJobDirectory;
        this.project = project;
        this.printQuality = printQuality;
        this.settings = settings;
        this.printerToUse = printerToUse;
        updateProgress(0.0, 100.0);
    }

    @Override
    protected SliceResult call() throws Exception
    {
        if (isCancelled())
        {
            return null;
        }

        steno.info("slice " + project + " " + settings.getProfileName());
        updateTitle("Slicer");
        updateMessage("Preparing model for conversion");
        updateProgress(0.0, 100.0);

        return doSlicing(printJobUUID, settings, printJobDirectory, project,
                printQuality, printerToUse, this, steno);
    }

    public static SliceResult doSlicing(String printJobUUID, SlicerParametersFile settings,
            String printJobDirectory, Project project, PrintQualityEnumeration printQuality,
            Printer printerToUse, ProgressReceiver progressReceiver, Stenographer steno)
    {
        boolean succeeded = false;

        SlicerType slicerType = Lookup.getUserPreferences().getSlicerType();
        if (settings.getSlicerOverride() != null)
        {
            slicerType = settings.getSlicerOverride();
        }

        String tempModelFilename;
        MeshFileOutputConverter outputConverter = null;

//        if (slicerType == SlicerType.Cura)
//        {
//            tempModelFilename = printJobUUID + ApplicationConfiguration.amfTempFileExtension;
//            outputConverter = new AMFOutputConverter();
//        } else
        {
            tempModelFilename = printJobUUID + ApplicationConfiguration.stlTempFileExtension;
            outputConverter = new STLOutputConverter();
        }

        List<String> createdMeshFiles = null;

        // Output multiple files if we are using Cura
//        if (slicerType == SlicerType.Cura)
//        {
//            createdMeshFiles = outputConverter.outputFile(project, printJobUUID, printJobDirectory,
//                                                          false);
//        } else
        {
            createdMeshFiles = outputConverter.outputFile(project, printJobUUID, printJobDirectory,
                    true);
        }

        String tempGcodeFilename = printJobUUID + ApplicationConfiguration.gcodeTempFileExtension;

        String configFile = printJobUUID + ApplicationConfiguration.printProfileFileExtension;

        MachineType machineType = ApplicationConfiguration.getMachineType();
        ArrayList<String> commands = new ArrayList<>();

        String windowsSlicerCommand = "";
        String macSlicerCommand = "";
        String linuxSlicerCommand = "";
        String configLoadCommand = "";
        //The next variable is only required for Slic3r
        String printCenterCommand = "";
        String combinedConfigSection = "";
        String verboseOutputCommand = "";
        String progressOutputCommand = "";

        switch (slicerType)
        {
            case Slic3r:
                windowsSlicerCommand = "\"" + ApplicationConfiguration.
                        getCommonApplicationDirectory() + "Slic3r\\slic3r.exe\"";
                macSlicerCommand = "Slic3r.app/Contents/MacOS/slic3r";
                linuxSlicerCommand = "Slic3r/bin/slic3r";
                configLoadCommand = "--load";
                combinedConfigSection = configLoadCommand + " " + configFile;
                printCenterCommand = "--print-center";
                break;
            case Cura:
                windowsSlicerCommand = "\"" + ApplicationConfiguration.
                        getCommonApplicationDirectory() + "Cura\\CuraEngine.exe\"";
                macSlicerCommand = "Cura/CuraEngine";
                linuxSlicerCommand = "Cura/CuraEngine";
                verboseOutputCommand = "-v";
                configLoadCommand = "-c";
                progressOutputCommand = "-p";
                combinedConfigSection = configLoadCommand + " " + configFile;
                break;
        }

        steno.debug("Selected slicer is " + slicerType + " : " + Thread.currentThread().getName());

        switch (machineType)
        {
            case WINDOWS_95:
                commands.add("command.com");
                commands.add("/S");
                commands.add("/C");
                String win95PrintCommand = "\"pushd \""
                        + printJobDirectory
                        + "\" && "
                        + windowsSlicerCommand
                        + " "
                        + verboseOutputCommand
                        + " "
                        + progressOutputCommand
                        + " "
                        + combinedConfigSection
                        + " -o "
                        + tempGcodeFilename;
                for (String fileName : createdMeshFiles)
                {
                    win95PrintCommand += " \"";
                    win95PrintCommand += fileName;
                    win95PrintCommand += "\"";
                }
                win95PrintCommand += " && popd\"";
                commands.add(win95PrintCommand);
                break;
            case WINDOWS:
                commands.add("cmd.exe");
                commands.add("/S");
                commands.add("/C");
                String windowsPrintCommand = "\"pushd \""
                        + printJobDirectory
                        + "\" && "
                        + windowsSlicerCommand
                        + " "
                        + verboseOutputCommand
                        + " "
                        + progressOutputCommand
                        + " "
                        + combinedConfigSection
                        + " -o "
                        + tempGcodeFilename;

                if (!printCenterCommand.equals(""))
                {
                    windowsPrintCommand += " " + printCenterCommand;
                    Vector3D centreOfPrintedObject = ThreeDUtils.calculateCentre(project.getLoadedModels());
                    windowsPrintCommand += " "
                            + String.format(Locale.UK, "%.3f", centreOfPrintedObject.getX())
                            + ","
                            + String.format(Locale.UK, "%.3f", centreOfPrintedObject.getZ());
                }

                for (String fileName : createdMeshFiles)
                {
                    windowsPrintCommand += " \"";
                    windowsPrintCommand += fileName;
                    windowsPrintCommand += "\"";
                }
                windowsPrintCommand += " && popd\"";
                commands.add(windowsPrintCommand);
                break;
            case MAC:
                commands.add(ApplicationConfiguration.getCommonApplicationDirectory()
                        + macSlicerCommand);
                if (!verboseOutputCommand.equals(""))
                {
                    commands.add(verboseOutputCommand);
                }
                if (!progressOutputCommand.equals(""))
                {
                    commands.add(progressOutputCommand);
                }
                commands.add(configLoadCommand);
                commands.add(configFile);
                commands.add("-o");
                commands.add(tempGcodeFilename);
                if (!printCenterCommand.equals(""))
                {
                    commands.add(printCenterCommand);
                    Vector3D centreOfPrintedObject = ThreeDUtils.calculateCentre(project.getLoadedModels());
                    commands.add(String.format(Locale.UK, "%.3f", centreOfPrintedObject.getX())
                            + ","
                            + String.format(Locale.UK, "%.3f", centreOfPrintedObject.getZ()));
                }
                for (String fileName : createdMeshFiles)
                {
                    commands.add(fileName);
                }
                break;
            case LINUX_X86:
            case LINUX_X64:
                commands.add(ApplicationConfiguration.getCommonApplicationDirectory()
                        + linuxSlicerCommand);
                if (!verboseOutputCommand.equals(""))
                {
                    commands.add(verboseOutputCommand);
                }
                if (!progressOutputCommand.equals(""))
                {
                    commands.add(progressOutputCommand);
                }
                commands.add(configLoadCommand);
                commands.add(configFile);
                commands.add("-o");
                commands.add(tempGcodeFilename);
                if (!printCenterCommand.equals(""))
                {
                    commands.add(printCenterCommand);
                    Vector3D centreOfPrintedObject = ThreeDUtils.calculateCentre(project.getLoadedModels());
                    commands.add(String.format(Locale.UK, "%.3f", centreOfPrintedObject.getX())
                            + ","
                            + String.format(Locale.UK, "%.3f", centreOfPrintedObject.getZ()));
                }
                for (String fileName : createdMeshFiles)
                {
                    commands.add(fileName);
                }
                break;
            default:
                steno.error("Couldn't determine how to run slicer");
        }

        if (commands.size() > 0)
        {
            steno.debug("Slicer command is " + String.join(" ", commands));
            ProcessBuilder slicerProcessBuilder = new ProcessBuilder(commands);
            if (machineType != MachineType.WINDOWS && machineType != MachineType.WINDOWS_95)
            {
                steno.debug("Set working directory (Non-Windows) to " + printJobDirectory);
                slicerProcessBuilder.directory(new File(printJobDirectory));
            }

            Process slicerProcess = null;
            try
            {
                slicerProcess = slicerProcessBuilder.start();
                // any error message?
                SlicerOutputGobbler errorGobbler = new SlicerOutputGobbler(progressReceiver, slicerProcess.
                        getErrorStream(), "ERROR",
                        slicerType);

                // any output?
                SlicerOutputGobbler outputGobbler = new SlicerOutputGobbler(progressReceiver, slicerProcess.
                        getInputStream(),
                        "OUTPUT", slicerType);

                // kick them off
                errorGobbler.start();
                outputGobbler.start();

                int exitStatus = slicerProcess.waitFor();
                switch (exitStatus)
                {
                    case 0:
                        steno.debug("Slicer terminated successfully ");
                        succeeded = true;
                        break;
                    default:
                        steno.error("Failure when invoking slicer with command line: " + String.join(
                                " ", commands));
                        steno.error("Slicer terminated with unknown exit code " + exitStatus);
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

        return new SliceResult(printJobUUID, project, printQuality, settings, printerToUse,
                succeeded);
    }

    @Override
    public void progressUpdateFromSlicer(String message, float workDone)
    {
        updateMessage(message);
        updateProgress(workDone, 100.0);
    }

}
