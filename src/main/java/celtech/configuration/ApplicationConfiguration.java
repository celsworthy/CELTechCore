/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.configuration;

import celtech.appManager.ProjectMode;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;
import javafx.scene.paint.Color;
import libertysystems.configuration.ConfigNotLoadedException;
import libertysystems.configuration.Configuration;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ApplicationConfiguration
{

    public static final String resourcePath = "/celtech/resources/";
    public static final String modelResourcePath = resourcePath + "models/";
    public static final String imageResourcePath = resourcePath + "images/";
    public static final String fxmlResourcePath = resourcePath + "fxml/";
    public static final String fxmlSidePanelResourcePath = resourcePath + "fxml/sidePanels/";
    public static final String fontResourcePath = resourcePath + "fonts/";
    public static final String cssResourcePath = resourcePath + "css/";

    public static final String macroFileExtension = ".gcode";
    public static final String macroFileSubpath = "macros/";

    public static final String mainCSSFile = cssResourcePath + "AutoMaker.css";

    public static final double DEFAULT_WIDTH = 1440;
    public static final double DEFAULT_HEIGHT = 900;
    public static final double DESIRED_ASPECT_RATIO = DEFAULT_WIDTH / DEFAULT_HEIGHT;

    private static final Stenographer steno = StenographerFactory.getStenographer(ApplicationConfiguration.class.getName());
    private static Configuration configuration = null;
    private static String applicationInstallDirectory = null;
    public static final String applicationConfigComponent = "ApplicationConfiguration";
    private static String userStorageDirectory = null;
    public static final String userStorageDirectoryComponent = "UserDataStorageDirectory";
    private static String applicationStorageDirectory = null;
    public static final String applicationStorageDirectoryComponent = "ApplicationDataStorageDirectory";

    private static String projectFileStorageDirectory = null;
    private static String projectFileDirectoryPath = "Projects";
    public static final String projectFileExtension = ".robox";
    private static final String supportedProjectFileExtension = projectFileExtension.replaceFirst("\\.", "");

    public static final String[] supportedModelExtensions =
    {
        "stl"
    };
    public static final String[] supportedProcessedModelExtensions =
    {
//        "gcode"
    };

    private static String printFileSpoolDirectory = null;
    public static final String printSpoolStorageDirectoryPath = "PrintJobs";
    public static final String printFileExtension = ".prt";

    private static String filamentFileDirectory = null;
    private static String userFilamentFileDirectory = null;
    public static final String filamentDirectoryPath = "Filaments";
    public static final String filamentFileExtension = ".roboxfilament";

    private static String headFileDirectory = null;
    public static final String headDirectoryPath = "Heads";
    public static final String headFileExtension = ".roboxhead";

    private static String printProfileFileDirectory = null;
    private static String userPrintProfileFileDirectory = null;
    public static final String printProfileDirectoryPath = "PrintProfiles";
    public static final String printProfileFileExtension = ".roboxprofile";
    public static final String customSettingsProfileName = "CustomSettings";
    public static final String draftSettingsProfileName = "DraftSettings";
    public static final String normalSettingsProfileName = "NormalSettings";
    public static final String fineSettingsProfileName = "FineSettings";

    public static final String stlTempFileExtension = ".stl";
    public static final String gcodeTempFileExtension = ".gcode";

    private static Properties projectProperties = null;

    private static String applicationVersion = null;
    private static String applicationLanguageRaw = null;

    public static final String projectDataFilename = "projects.dat";

    private static String applicationTitleAndVersion = null;

    public static final Color xAxisColour = Color.RED;
    public static final Color zAxisColour = Color.GREEN;

    private static MachineType machineType = null;

    public static MachineType getMachineType()
    {
        if (machineType == null)
        {
            String osName = System.getProperty("os.name");

            if (osName.startsWith("Windows"))
            {
                machineType = MachineType.WINDOWS;
            } else if (osName.startsWith("Mac"))
            {
                machineType = MachineType.MAC;
            }
        }

        return machineType;
    }

    public static String getApplicationInstallDirectory(Class classToCheck)
    {
        if (configuration == null)
        {
            try
            {
                configuration = Configuration.getInstance();
            } catch (ConfigNotLoadedException ex)
            {
                steno.error("Couldn't load configuration - the application cannot derive the install directory");
            }
        }

        if (configuration != null && applicationInstallDirectory == null)
        {
            try
            {
                String fakeAppDirectory = configuration.getFilenameString(applicationConfigComponent, "FakeInstallDirectory", null);
                if (fakeAppDirectory == null)
                {
                    try
                    {
                        String path = classToCheck.getProtectionDomain().getCodeSource().getLocation().getPath();
                        URI uri = new URI(path);
                        File file = new File(uri.getSchemeSpecificPart());
                        String actualPath = file.getCanonicalPath();
                        actualPath = actualPath.replaceFirst("[a-zA-Z0-9]*\\.jar", "");
                        applicationInstallDirectory = actualPath;
                    } catch (URISyntaxException ex)
                    {
                        steno.error("URI Syntax Exception whilst attempting to determine the application path - the application is unlikely to run correctly.");
                    } catch (IOException ex)
                    {
                        steno.error("IO Exception whilst attempting to determine the application path - the application is unlikely to run correctly.");
                    }
                } else
                {
                    applicationInstallDirectory = fakeAppDirectory;
                }
            } catch (ConfigNotLoadedException ex)
            {
                steno.error("Couldn't load configuration - the application cannot derive the install directory");
            }
        }
        return applicationInstallDirectory;
    }

    public static String getUserStorageDirectory()
    {
        if (configuration == null)
        {
            try
            {
                configuration = Configuration.getInstance();
            } catch (ConfigNotLoadedException ex)
            {
                steno.error("Couldn't load configuration - the application cannot derive the install directory");
            }
        }

        if (configuration != null && userStorageDirectory == null)
        {
            try
            {
                userStorageDirectory = configuration.getFilenameString(applicationConfigComponent, userStorageDirectoryComponent, null);
                steno.info("User storage directory = " + userStorageDirectory);
            } catch (ConfigNotLoadedException ex)
            {
                steno.error("Couldn't determine user storage location - the application will not run correctly");
            }
        }
        return userStorageDirectory;
    }

    public static String getApplicationStorageDirectory()
    {
        if (configuration == null)
        {
            try
            {
                configuration = Configuration.getInstance();
            } catch (ConfigNotLoadedException ex)
            {
                steno.error("Couldn't load configuration - the application cannot derive the install directory");
            }
        }

        if (configuration != null && applicationStorageDirectory == null)
        {
            try
            {
                applicationStorageDirectory = configuration.getFilenameString(applicationConfigComponent, applicationStorageDirectoryComponent, null);
                steno.info("Application storage directory = " + applicationStorageDirectory);
            } catch (ConfigNotLoadedException ex)
            {
                steno.error("Couldn't determine application storage location - the application will not run correctly");
            }
        }
        return applicationStorageDirectory;
    }

    public static String getProjectDirectory()
    {
        if (projectFileStorageDirectory == null)
        {
            projectFileStorageDirectory = getUserStorageDirectory() + projectFileDirectoryPath + '/';

            File dirHandle = new File(projectFileStorageDirectory);

            if (!dirHandle.exists())
            {
                dirHandle.mkdirs();
            }
        }

        return projectFileStorageDirectory;
    }

    public static String getPrintSpoolDirectory()
    {
        if (printFileSpoolDirectory == null)
        {
            printFileSpoolDirectory = getUserStorageDirectory() + printSpoolStorageDirectoryPath + '/';

            File dirHandle = new File(printFileSpoolDirectory);

            if (!dirHandle.exists())
            {
                dirHandle.mkdirs();
            }
        }

        return printFileSpoolDirectory;
    }

    public static String getUserFilamentDirectory()
    {
        if (userFilamentFileDirectory == null)
        {
            userFilamentFileDirectory = getUserStorageDirectory() + filamentDirectoryPath + '/';

            File dirHandle = new File(userFilamentFileDirectory);

            if (!dirHandle.exists())
            {
                dirHandle.mkdirs();
            }
        }

        return userFilamentFileDirectory;
    }

    public static String getApplicationFilamentDirectory()
    {
        if (filamentFileDirectory == null)
        {
            filamentFileDirectory = applicationInstallDirectory + filamentDirectoryPath + '/';
        }

        return filamentFileDirectory;
    }

    public static String getApplicationHeadDirectory()
    {
        if (headFileDirectory == null)
        {
            headFileDirectory = applicationInstallDirectory + headDirectoryPath + '/';
        }

        return headFileDirectory;
    }

    public static String getUserPrintProfileDirectory()
    {
        if (userPrintProfileFileDirectory == null)
        {
            userPrintProfileFileDirectory = getUserStorageDirectory() + printProfileDirectoryPath + '/';

            File dirHandle = new File(userPrintProfileFileDirectory);

            if (!dirHandle.exists())
            {
                dirHandle.mkdirs();
            }
        }

        return userPrintProfileFileDirectory;
    }

    public static String getApplicationPrintProfileDirectory()
    {
        if (printProfileFileDirectory == null)
        {
            printProfileFileDirectory = applicationInstallDirectory + printProfileDirectoryPath + '/';
        }

        return printProfileFileDirectory;
    }

    private static void loadProjectProperties()
    {
        InputStream input = null;

        try
        {
            input = new FileInputStream(applicationInstallDirectory + "application.properties");

            // load a properties file
            projectProperties = new Properties();
            projectProperties.load(input);
        } catch (IOException ex)
        {
            ex.printStackTrace();
        } finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getApplicationVersion()
    {
        if (projectProperties == null)
        {
            loadProjectProperties();
        }
        if (applicationVersion == null)
        {
            applicationVersion = projectProperties.getProperty("version");
        }

        return applicationVersion;
    }

    public static String getApplicationLanguage()
    {
        if (projectProperties == null)
        {
            loadProjectProperties();
        }

        if (applicationLanguageRaw == null)
        {
            applicationLanguageRaw = projectProperties.getProperty("language").replaceAll("_", "-");
        }

        return applicationLanguageRaw;
    }

    public static void setTitleAndVersion(String titleAndVersion)
    {
        applicationTitleAndVersion = titleAndVersion;
    }

    public static String getTitleAndVersion()
    {
        return applicationTitleAndVersion;
    }

    public static ArrayList<String> getSupportedFileExtensionWildcards(ProjectMode projectMode)
    {
        ArrayList<String> returnVal = new ArrayList<>();

        switch (projectMode)
        {
            case NONE:
                for (String extension : supportedModelExtensions)
                {
                    returnVal.add("*." + extension);
                }
                for (String extension : supportedProcessedModelExtensions)
                {
                    returnVal.add("*." + extension);
                }
                returnVal.add("*." + supportedProjectFileExtension);
                break;
            case MESH:
                for (String extension : supportedModelExtensions)
                {
                    returnVal.add("*." + extension);
                }
                break;
            case GCODE:
                for (String extension : supportedProcessedModelExtensions)
                {
                    returnVal.add("*." + extension);
                }
                break;
            default:
                break;
        }

        return returnVal;
    }

    public static ArrayList<String> getSupportedFileExtensions(ProjectMode projectMode)
    {
        ArrayList<String> returnVal = new ArrayList<>();

        switch (projectMode)
        {
            case NONE:
                for (String extension : supportedModelExtensions)
                {
                    returnVal.add(extension);
                }
                for (String extension : supportedProcessedModelExtensions)
                {
                    returnVal.add(extension);
                }
                returnVal.add(supportedProjectFileExtension);
                break;
            case MESH:
                for (String extension : supportedModelExtensions)
                {
                    returnVal.add(extension);
                }
                break;
            case GCODE:
                for (String extension : supportedProcessedModelExtensions)
                {
                    returnVal.add(extension);
                }
                break;
            default:
                break;
        }

        return returnVal;
    }
}
