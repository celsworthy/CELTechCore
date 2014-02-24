/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.slicer;

import celtech.configuration.ApplicationConfiguration;
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
    private static final String applicationDirectory = ApplicationConfiguration.getApplicationInstallDirectory(SlicerManager.class);

    public SlicerManager()
    {
//        this.application = application;
        steno = StenographerFactory.getStenographer(this.getClass().getName());
        steno.changeLogLevel(LogLevel.DEBUG);
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

            String osName = System.getProperty("os.name");
            if (osName.equals("Windows 95"))
            {
                String[] longCommand = new String[3];
                longCommand[0] = "command.com";
                longCommand[1] = "/C";
                longCommand[2] = applicationDirectory + "\\RoboxPerl\\slic3r-console.exe --load " + configFilename + " -o " + gcodeFilename + " " + stlFilename;

                command = longCommand;
                
            } else if (osName.startsWith("Windows"))
            {
                String[] longCommand = new String[3];
                longCommand[0] = "cmd.exe";
                longCommand[1] = "/C";
                longCommand[2] = applicationDirectory +
                    "\\RoboxPerl\\slic3r-console.exe --load " + configFilename + " -o " + gcodeFilename + " " + stlFilename;

                command = longCommand;

            } else if (osName.equals("Mac OS X"))
            {
                command[0] = "/Applications/Slic3r.app/Contents/MacOS/slic3r";
                command[1] = "--load";
                command[2] = configFilename;
                command[3] = "-o";
                command[4] = gcodeFilename;
                command[5] = stlFilename;
            } else
            {
                String[] longCommand = new String[3];
                longCommand[0] = "cmd.exe";
                longCommand[1] = "/C";
                longCommand[2] = "Slic3r\\slic3r-console.exe --load " + configFilename + " -o " + gcodeFilename + " " + stlFilename;

                command = longCommand;
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
