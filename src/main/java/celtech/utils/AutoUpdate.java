/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils;

import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ModalDialog;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.stage.Stage;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class AutoUpdate extends Thread
{

    private static final Stenographer steno = StenographerFactory.getStenographer(AutoUpdate.class.getName());

    private String applicationName = null;
    private Stage stage = null;
    private final int ERROR = -1;
    private final int UPGRADE_REQUIRED = 1;
    private final int UPGRADE_NOT_REQUIRED = 0;
    private ModalDialog upgradeDialog = null;
    private boolean keepRunning = true;
    private Class parentClass = null;

    public AutoUpdate(String applicationName, Stage stage, Class parentClass)
    {
        this.applicationName = applicationName;
        this.stage = stage;
        this.setName("AutoUpdate");
        this.parentClass = parentClass;
    }

    @Override
    public void run()
    {
        int strikes = 0;

        while (strikes < 1 && keepRunning)
        {
            int status = checkForUpdates();

            if (status == UPGRADE_NOT_REQUIRED)
            {
                break;
            } else if (status == UPGRADE_REQUIRED)
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        upgradeDialog = new ModalDialog();
                        upgradeDialog.setTitle(DisplayManager.getLanguageBundle().getString("dialogs.updateApplicationTitle"));
                        upgradeDialog.addButton(DisplayManager.getLanguageBundle().getString("dialogs.OK"));
                        upgradeDialog.addButton(DisplayManager.getLanguageBundle().getString("dialogs.no"));
                        upgradeDialog.setMessage(DisplayManager.getLanguageBundle().getString("dialogs.updateApplicationMessagePart1")
                                + DisplayManager.getLanguageBundle().getString("application.title")
                                + DisplayManager.getLanguageBundle().getString("dialogs.updateApplicationMessagePart2"));

                        int choice = upgradeDialog.show();
                        if (choice == 0)
                        {
                            //Run the autoupdater in the background in download mode
                            startUpdate();
                            //Exit
                            Platform.exit();
                        }
                    }
                });
                break;
            }

            try
            {
                this.sleep(5000);
            } catch (InterruptedException ex)
            {
                steno.warning("AutoUpdate sleep was interrupted");
            }

            strikes++;

        }
    }

    private int checkForUpdates()
    {
        int upgradeStatus = ERROR;

        String osName = System.getProperty("os.name");

        ArrayList<String> commands = new ArrayList<>();

        if (osName.equals("Windows 95"))
        {
            commands.add("command.com");
            commands.add("/S");
            commands.add("/W");
            commands.add("/C");
            commands.add("\"" + ApplicationConfiguration.getApplicationInstallDirectory(parentClass) + applicationName + "-update-windows.exe\"");
            commands.add("--mode");
            commands.add("unattended");
            commands.add("--unattendedmodebehavior");
            commands.add("onlycheck");
            commands.add("--unattendedmodeui");
            commands.add("minimalWithDialogs");

        } else if (osName.startsWith("Windows"))
        {
            commands.add("cmd.exe");
            commands.add("/S");
            commands.add("/W");
            commands.add("/C");
            commands.add("\"" + ApplicationConfiguration.getApplicationInstallDirectory(parentClass) + applicationName + "-update-windows.exe\"");

            commands.add("--mode");
            commands.add("unattended");
            commands.add("--unattendedmodebehavior");
            commands.add("onlycheck");
            commands.add("--unattendedmodeui");
            commands.add("minimalWithDialogs");
        } else if (osName.equals("Mac OS X"))
        {
            commands.add(ApplicationConfiguration.getApplicationInstallDirectory(parentClass) + applicationName + "-update-osx.app/Contents/MacOS/installbuilder.sh");

            commands.add("--mode");
            commands.add("unattended");
            commands.add("--unattendedmodebehavior");
            commands.add("onlycheck");
            commands.add("--unattendedmodeui");
            commands.add("minimalWithDialogs");
        }
        /*
         * Return codes from the (BitRock) autoupdater
         * 
         * 0: Successfully downloaded and executed the installer.
         * 1: No updates available
         * 2: Error connecting to remote server or invalid XML file
         * 3: An error occurred downloading the file
         * 4: An error occurred executing the downloaded update or evaluating its <postUpdateDownloadActionList>
         * 5: Update check disabled through check_for_updates setting
         */

        if (commands.size() > 0)
        {
            ProcessBuilder autoupdateProcess = new ProcessBuilder(commands);
            autoupdateProcess.inheritIO();
            try
            {
                final Process updateProc = autoupdateProcess.start();
                boolean checkFinished = updateProc.waitFor(10, TimeUnit.SECONDS);

                if (checkFinished == false)
                {
                    //The autoupdater is still waiting. Kill it and try again.
                    steno.info("Couldn't get a response from autoupdate - killing it...");
                    updateProc.destroyForcibly();
                    sleep(5000);
                } else
                {
                    switch (updateProc.exitValue())
                    {
                        case 0:
                            upgradeStatus = UPGRADE_REQUIRED;
                            steno.info("Upgrade required");
                            break;
                        case 1:
                            upgradeStatus = UPGRADE_NOT_REQUIRED;
                            steno.info("No upgrade required");
                            break;
                        case 2:
                            steno.info("Error connecting to remote upgrade server");
                            break;
                        case 3:
                            steno.info("Error during upgrade download");
                            break;
                        case 4:
                            steno.info("Failure during upgrade installation");
                            break;
                        case 5:
                            steno.info("Update check disabled through update.ini settings");
                            break;
                        default:
                            steno.info("Unknown code returned from autoupdater");
                            break;
                    }
                }
            } catch (IOException ex)
            {
                steno.error("Exception whilst running autoupdate: " + ex);
            } catch (InterruptedException ex)
            {
                steno.error("Interrupted whilst waiting for autoupdate to complete");
            }
        } else
        {
            steno.error("Couldn't run autoupdate - no commands for OS " + osName);
        }

        return upgradeStatus;
    }

    private void startUpdate()
    {
        String osName = System.getProperty("os.name");

        ArrayList<String> commands = new ArrayList<>();

        if (osName.equals("Windows 95"))
        {
            commands.add("command.com");
            commands.add("/S");
            commands.add("/W");
            commands.add("/C");
            commands.add("\"" + ApplicationConfiguration.getApplicationInstallDirectory(parentClass) + applicationName + "-update-windows.exe\"");
            commands.add("--mode");
            commands.add("unattended");
            commands.add("--unattendedmodebehavior");
            commands.add("download");
            commands.add("--unattendedmodeui");
            commands.add("minimalWithDialogs");

        } else if (osName.startsWith("Windows"))
        {
            commands.add("cmd.exe");
            commands.add("/S");
            commands.add("/W");
            commands.add("/C");
            commands.add("\"" + ApplicationConfiguration.getApplicationInstallDirectory(parentClass) + applicationName + "-update-windows.exe\"");

            commands.add("--mode");
            commands.add("unattended");
            commands.add("--unattendedmodebehavior");
            commands.add("download");
            commands.add("--unattendedmodeui");
            commands.add("minimalWithDialogs");
        } else if (osName.equals("Mac OS X"))
        {
            commands.add(ApplicationConfiguration.getApplicationInstallDirectory(parentClass) + applicationName + "-update-osx.app/Contents/MacOS/installbuilder.sh");

            commands.add("--mode");
            commands.add("unattended");
            commands.add("--unattendedmodebehavior");
            commands.add("download");
            commands.add("--unattendedmodeui");
            commands.add("minimalWithDialogs");
        }
        /*
         * Return codes from the (BitRock) autoupdater
         * 
         * 0: Successfully downloaded and executed the installer.
         * 1: No updates available
         * 2: Error connecting to remote server or invalid XML file
         * 3: An error occurred downloading the file
         * 4: An error occurred executing the downloaded update or evaluating its <postUpdateDownloadActionList>
         * 5: Update check disabled through check_for_updates setting
         */

        if (commands.size() > 0)
        {
            ProcessBuilder autoupdateProcess = new ProcessBuilder(commands);
            autoupdateProcess.inheritIO();
            try
            {
                final Process updateProc = autoupdateProcess.start();
            } catch (IOException ex)
            {
                steno.error("Exception whilst running autoupdate: " + ex);
            }
        } else
        {
            steno.error("Couldn't run autoupdate - no commands for OS " + osName);
        }
    }
    
    public void shutdown()
    {
        keepRunning = false;
    }
}
