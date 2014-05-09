/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.slicer;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.MachineType;
import libertysystems.stenographer.LogLevel;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class SlicerManager
{

    private Stenographer steno = null;
    private static String applicationDirectory = null;

    public SlicerManager()
    {
//        this.application = application;
        steno = StenographerFactory.getStenographer(this.getClass().getName());
        steno.changeLogLevel(LogLevel.DEBUG);
        applicationDirectory = ApplicationConfiguration.getCommonApplicationDirectory();
    }

    public void sliceModel(String stlFilename, String gcodeFilename, String configFilename)
    {
        if (configFilename == null)
        {
            configFilename = "robox_default.ini";
        }

        try
        {
            String[] command = new String[6];

            MachineType machineType = ApplicationConfiguration.getMachineType();

            switch (machineType)
            {
                case WINDOWS_95:
                    String[] longWin95Command = new String[3];
                    longWin95Command[0] = "command.com";
                    longWin95Command[1] = "/C";
                    longWin95Command[2] = applicationDirectory + "\\Slic3r\\slic3r-console.exe --load " + configFilename + " -o " + gcodeFilename + " " + stlFilename;
                    command = longWin95Command;
                    break;
                case WINDOWS:
                    String[] longWindowsCommand = new String[3];
                    longWindowsCommand[0] = "cmd.exe";
                    longWindowsCommand[1] = "/C";
                    longWindowsCommand[2] = applicationDirectory + "Slic3r\\slic3r-console.exe --load " + configFilename + " -o " + gcodeFilename + " " + stlFilename;
                    command = longWindowsCommand;
                    break;
                case MAC:
                    command[0] = "/Applications/Slic3r.app/Contents/MacOS/slic3r";
                    command[1] = "--load";
                    command[2] = configFilename;
                    command[3] = "-o";
                    command[4] = gcodeFilename;
                    command[5] = stlFilename;
                    break;
                case LINUX_X86:
                case LINUX_X64:
                    command[0] = applicationDirectory + "/Slic3r/bin/slic3r";
                    command[1] = "--load";
                    command[2] = configFilename;
                    command[3] = "-o";
                    command[4] = gcodeFilename;
                    command[5] = stlFilename;
                    break;
            }

            StringBuilder comString = new StringBuilder();
            for (String com : command)
            {
                comString.append(com);
                comString.append(' ');
            }
            steno.debug("Using command " + comString.toString());

            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(command);
            // any error message?
            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR");

            // any output?
//            SlicerOutputGobbler outputGobbler = new SlicerOutputGobbler(application, proc.getInputStream(), "OUTPUT");
            // kick them off
            errorGobbler.start();
//            outputGobbler.start();

            // any error???
            int exitVal = proc.waitFor();
            steno.trace("ExitValue: " + exitVal);
        } catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}
