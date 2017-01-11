package celtech.configuration;

import celtech.appManager.ProjectMode;
import celtech.crypto.CryptoFileStore;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import libertysystems.configuration.ConfigNotLoadedException;
import libertysystems.configuration.Configuration;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.codehaus.plexus.util.FileUtils;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ApplicationConfiguration
{
    
    private static String applicationName = null;
    private static String applicationShortName = null;
    
    public static final String resourcePath = "/celtech/resources/";
    
    public static final String modelResourcePath = resourcePath + "models/";
    
    public static final String imageResourcePath = resourcePath + "images/";
    
    public static final String fxmlResourcePath = resourcePath + "fxml/";
    
    public static final String fxmlPanelResourcePath = resourcePath + "fxml/panels/";
    
    public static final String fxmlDiagramsResourcePath = resourcePath + "fxml/diagrams/";
    
    public static final String fxmlButtonsResourcePath = resourcePath + "fxml/buttons/";
    
    public static final String fxmlUtilityPanelResourcePath = resourcePath + "fxml/utilityPanels/";
    
    public static final String fxmlPopupResourcePath = resourcePath + "fxml/popups/";
    
    public static final String fontResourcePath = resourcePath + "fonts/";
    
    public static final String cssResourcePath = resourcePath + "css/";
    
    public static final String macroFileExtension = ".gcode";
    
    public static final String macroFileSubpath = "Macros/";
    
    public static final String timeAndCostFileSubpath = "TimeCostTemp/";
    
    private static final String mainCSSFile = cssResourcePath + "JMetroDarkTheme.css";
    
    private static final String dialogsCSSFile = cssResourcePath + "dialogsOverride.css";
    
    public static final double DEFAULT_WIDTH = 1440;
    
    public static final double DEFAULT_HEIGHT = 900;
    
    public static final double DESIRED_ASPECT_RATIO = DEFAULT_WIDTH / DEFAULT_HEIGHT;
    
    public static final int NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP = 210;
    
    private static final Stenographer steno = StenographerFactory.getStenographer(
            ApplicationConfiguration.class.getName());
    private static Configuration configuration = null;
    private static String applicationInstallDirectory = null;
    
    public static final String applicationConfigComponent = "ApplicationConfiguration";
    private static String userStorageDirectory = null;
    
    public static final String userStorageDirectoryComponent = "UserDataStorageDirectory";
    private static String applicationStorageDirectory = null;
    
    public static final String applicationStorageDirectoryComponent = "ApplicationDataStorageDirectory";
    
    private static String commonApplicationDirectory = null;
    
    private static String projectFileStorageDirectory = null;
    public static String projectFileDirectoryPath = "Projects";
    
    public static final String projectFileExtension = ".robox";
    public static final String projectModelsFileExtension = ".models";
    public static final String demoPrintFilename = "demoPrint.gcode";
    private static final String supportedProjectFileExtension = projectFileExtension.replaceFirst(
            "\\.", "");
    
    public static final String[] supportedModelExtensions =
    {
        "stl",
        "obj"
    };
    
    public static final String[] supportedProcessedModelExtensions =
    {
//        "gcode"
    };
    
    private static String printFileSpoolDirectory = null;
    
    public static final String printSpoolStorageDirectoryPath = "PrintJobs";
    
    public static final String modelStorageDirectoryPath = "Models";
    
    public static final String printFileExtension = ".prt";
    
    private static String filamentFileDirectory = null;
    private static String userFilamentFileDirectory = null;
    
    public static final String filamentDirectoryPath = "Filaments";
    
    public static final String filamentFileExtension = ".roboxfilament";
    
    public static final int mmOfFilamentOnAReel = 240000;
    
    public static final float filamentDiameterToYieldVolumetricExtrusion = 1.1283791670955125738961589031215f;
    
    private static final String commonFileDirectoryPath = "CEL Robox" + File.separator;
    
    private static String myMiniFactoryDownloadsDirectory = null;
    
    private static String headFileDirectory = null;
    public static final String headDirectoryPath = "Heads";
    
    public static final String headFileExtension = ".roboxhead";
    
    private static String printProfileFileDirectory = null;
    private static String userPrintProfileFileDirectory = null;
    
    public static final String printProfileDirectoryPath = "PrintProfiles";
    
    public static final String printProfileFileExtension = ".roboxprofile";
    
    public static final String customSettingsProfileName = "Custom";
    
    public static final String draftSettingsProfileName = "Draft";
    
    public static final String normalSettingsProfileName = "Normal";
    
    public static final String fineSettingsProfileName = "Fine";
    
    public static final String stlTempFileExtension = ".stl";
    
    public static final String amfTempFileExtension = ".amf";
    public static final String gcodeTempFileExtension = ".gcode";
    
    public static final String gcodePostProcessedFileHandle = "_robox";
    
    public static final float bedHotAboveDegrees = 60.0f;
    
    public static final float maxTempToDisplayOnGraph = 300;
    
    public static final float minTempToDisplayOnGraph = 35;
    
    public static final int maxPermittedTempDifferenceForPurge = 15;

    //Data for the CEL Robox.properties file in APPDATA
    private static Properties installationProperties = null;
    private static Properties applicationMemoryProperties = null;
    private static final String fileMemoryItem = "FileMemory";
    public static final String lastPrinterAttachedMemoryItem = "LastPrinter";
    public static final String timeOfLastNewsRetrievalItem = "LastSuccessfulNewsRetrieval";    
    private static final String userLocaleItem = "Locale";
    
    private static String applicationVersion = null;
    private static String applicationLanguageRaw = null;
    
    public static final String projectDataFilename = "projects.dat";
    
    private static String applicationTitleAndVersion = null;
    
    public static final Color xAxisColour = Color.RED;
    
    public static final Color zAxisColour = Color.GREEN;
    
    private static MachineType machineType = null;
    
    public static final Duration notificationDisplayDelay = Duration.seconds(5);
    
    public static final Pos notificationPosition = Pos.BOTTOM_RIGHT;
    
    public static final int maxPrintSpoolFiles = 20;
    /**
     * The extension for statistics files in print spool directories
     */
    public static String statisticsFileExtension = ".statistics";

    /**
     * Used in testing only
     */
    public static void setInstallationProperties(Properties testingProperties,
            String applicationInstallDirectory, String userStorageDirectory)
    {
        installationProperties = testingProperties;
        ApplicationConfiguration.applicationInstallDirectory = applicationInstallDirectory;
        ApplicationConfiguration.userStorageDirectory = userStorageDirectory;
    }
    
    private static boolean autoRepairHeads = true;
    
    private static boolean autoRepairReels = true;

    /**
     * These variables are used to position the head correctly over the bed The
     * actual travel of the mechanical system is not the same as the theoretical
     * travel (to allow for door opening positions etc)
     */
    public static final int xPrintOffset = 6;
    public static final int yPrintOffset = 6;
    
    public static MachineType getMachineType()
    {
        if (machineType == null)
        {
            String osName = System.getProperty("os.name");
            
            if (osName.startsWith("Windows 95"))
            {
                machineType = MachineType.WINDOWS_95;
            } else if (osName.startsWith("Windows"))
            {
                machineType = MachineType.WINDOWS;
            } else if (osName.startsWith("Mac"))
            {
                machineType = MachineType.MAC;
            } else if (osName.startsWith("Linux"))
            {
                steno.debug("We have a linux variant");
                ProcessBuilder builder = new ProcessBuilder("uname", "-m");
                
                Process process = null;
                
                try
                {
                    process = builder.start();
                    InputStream is = process.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    String line;
                    
                    machineType = MachineType.LINUX_X86;
                    
                    while ((line = br.readLine()) != null)
                    {
                        if (line.equalsIgnoreCase("x86_64") == true)
                        {
                            machineType = MachineType.LINUX_X64;
                            steno.debug("Linux 64 bit detected");
                            break;
                        }
                    }
                } catch (IOException ex)
                {
                    machineType = MachineType.UNKNOWN;
                    steno.error("Error whilst determining linux machine type " + ex);
                }
                
            }
        }
        
        return machineType;
    }
    
    public static String getApplicationName()
    {
        if (configuration == null)
        {
            try
            {
                configuration = Configuration.getInstance();
            } catch (ConfigNotLoadedException ex)
            {
                steno.error(
                        "Couldn't load configuration - the application cannot derive the install directory");
            }
        }
        
        if (configuration != null && applicationName == null)
        {
            try
            {
                applicationName = configuration.getFilenameString(applicationConfigComponent,
                        "ApplicationName", null);
            } catch (ConfigNotLoadedException ex)
            {
                steno.error(
                        "Couldn't determine application name - the application will not run correctly");
            }
        }
        return applicationName;
    }
    
    public static String getApplicationShortName()
    {
        if (configuration == null)
        {
            try
            {
                configuration = Configuration.getInstance();
            } catch (ConfigNotLoadedException ex)
            {
                steno.error(
                        "Couldn't load configuration - the application cannot derive the install directory");
            }
        }
        
        if (configuration != null && applicationShortName == null)
        {
            try
            {
                applicationShortName = configuration.getFilenameString(applicationConfigComponent,
                        "ApplicationShortName", null);
                steno.debug("Application short name = " + applicationShortName);
            } catch (ConfigNotLoadedException ex)
            {
                steno.error(
                        "Couldn't determine application short name - the application will not run correctly");
            }
        }
        return applicationShortName;
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
                steno.error(
                        "Couldn't load configuration - the application cannot derive the install directory");
            }
        }
        
        if (configuration != null && applicationInstallDirectory == null)
        {
            try
            {
                String fakeAppDirectory = configuration.getFilenameString(applicationConfigComponent,
                        "FakeInstallDirectory",
                        null);
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
                        steno.error(
                                "URI Syntax Exception whilst attempting to determine the application path - the application is unlikely to run correctly.");
                    } catch (IOException ex)
                    {
                        steno.error(
                                "IO Exception whilst attempting to determine the application path - the application is unlikely to run correctly.");
                    }
                } else
                {
                    applicationInstallDirectory = fakeAppDirectory;
                }
            } catch (ConfigNotLoadedException ex)
            {
                steno.error(
                        "Couldn't load configuration - the application cannot derive the install directory");
            }
        }
        return applicationInstallDirectory;
    }
    
    public static String getCommonApplicationDirectory()
    {
        if (commonApplicationDirectory == null)
        {
            commonApplicationDirectory = applicationInstallDirectory + "../Common/";
        }
        
        return commonApplicationDirectory;
    }
    
    public static String getBinariesDirectory()
    {
        return getCommonApplicationDirectory() + "bin/";
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
                steno.error(
                        "Couldn't load configuration - the application cannot derive the install directory");
            }
        }
        
        if (configuration != null && userStorageDirectory == null)
        {
            if (getMachineType() == MachineType.WINDOWS)
            {
                String registryValue = WindowsRegistry.currentUser("Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders", "Personal");
                
                if (registryValue != null)
                {
                    Path regPath = Paths.get(registryValue);
                    if (Files.exists(regPath, LinkOption.NOFOLLOW_LINKS))
                    {
                        userStorageDirectory = registryValue + "\\"
                                + commonFileDirectoryPath;
                    }
                }
            }

            // Other OSes +
            // Just in case we're on a windows machine and the lookup failed...
            if (userStorageDirectory == null)
            {
                try
                {
                    userStorageDirectory = configuration.getFilenameString(
                            applicationConfigComponent, userStorageDirectoryComponent, null)
                            + commonFileDirectoryPath;
                    steno.debug("User storage directory = " + userStorageDirectory);
                } catch (ConfigNotLoadedException ex)
                {
                    steno.error(
                            "Couldn't determine user storage location - the application will not run correctly");
                }
            }
        }
        
        if (userStorageDirectory != null)
        {
            File userStorageDirRef = new File(userStorageDirectory);
            
            if (!userStorageDirRef.exists())
            {
                try
                {
                    FileUtils.forceMkdir(userStorageDirRef);
                } catch (IOException ex)
                {
                    steno.exception("Couldn't create user storage directory: " + userStorageDirectory, ex);
                }
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
                steno.error(
                        "Couldn't load configuration - the application cannot derive the install directory");
            }
        }
        
        if (configuration != null && applicationStorageDirectory == null)
        {
            if (getMachineType() == MachineType.WINDOWS)
            {
                String registryValue = WindowsRegistry.currentUser("Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders", "AppData");
                
                if (registryValue != null)
                {
                    Path regPath = Paths.get(registryValue);
                    if (Files.exists(regPath, LinkOption.NOFOLLOW_LINKS))
                    {
                        applicationStorageDirectory = registryValue + "\\"
                                + getApplicationName() + File.separator;
                    }
                }
            } else
            {
                try
                {
                    applicationStorageDirectory = configuration.getFilenameString(
                            applicationConfigComponent, applicationStorageDirectoryComponent, null);
                    steno.debug("Application storage directory = " + applicationStorageDirectory);
                } catch (ConfigNotLoadedException ex)
                {
                    steno.error(
                            "Couldn't determine application storage location - the application will not run correctly");
                }
            }
        }
        return applicationStorageDirectory;
    }
    
    public static String getProjectDirectory()
    {
        
        projectFileStorageDirectory = getUserStorageDirectory() + projectFileDirectoryPath + '/';
        
        File dirHandle = new File(projectFileStorageDirectory);
        
        if (!dirHandle.exists())
        {
            dirHandle.mkdirs();
        }
        
        return projectFileStorageDirectory;
    }
    
    public static String getPrintSpoolDirectory()
    {
        if (printFileSpoolDirectory == null)
        {
            printFileSpoolDirectory = getUserStorageDirectory() + printSpoolStorageDirectoryPath
                    + File.separator;
            
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
        userFilamentFileDirectory = getUserStorageDirectory() + filamentDirectoryPath + '/';
        
        File dirHandle = new File(userFilamentFileDirectory);
        
        if (!dirHandle.exists())
        {
            dirHandle.mkdirs();
        }
        
        return userFilamentFileDirectory;
    }
    
    public static String getApplicationFilamentDirectory()
    {
        if (filamentFileDirectory == null)
        {
            filamentFileDirectory = getCommonApplicationDirectory() + filamentDirectoryPath + '/';
        }
        
        return filamentFileDirectory;
    }
    
    public static String getApplicationHeadDirectory()
    {
        if (headFileDirectory == null)
        {
            headFileDirectory = getCommonApplicationDirectory() + headDirectoryPath + '/';
        }
        
        return headFileDirectory;
    }
    
    public static String getUserPrintProfileDirectory()
    {
        userPrintProfileFileDirectory = getUserStorageDirectory() + printProfileDirectoryPath
                + '/';
        
        File dirHandle = new File(userPrintProfileFileDirectory);
        
        if (!dirHandle.exists())
        {
            dirHandle.mkdirs();
        }
        
        return userPrintProfileFileDirectory;
    }
    
    public static String getApplicationPrintProfileDirectory()
    {
        if (printProfileFileDirectory == null)
        {
            printProfileFileDirectory = getCommonApplicationDirectory() + printProfileDirectoryPath
                    + '/';
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
            installationProperties = new Properties();
            installationProperties.load(input);
        } catch (IOException ex)
        {
            steno.exception("loading project", ex);
        } finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                } catch (IOException ex)
                {
                    steno.exception("loading project", ex);
                }
            }
        }
    }
    
    public static String getApplicationVersion()
    {
        if (installationProperties == null)
        {
            loadProjectProperties();
        }
        if (applicationVersion == null)
        {
            applicationVersion = installationProperties.getProperty("version");
        }
        
        return applicationVersion;
    }
    
    public static String getApplicationInstallationLanguage()
    {
        if (installationProperties == null)
        {
            loadProjectProperties();
        }
        
        if (applicationLanguageRaw == null)
        {
            applicationLanguageRaw = installationProperties.getProperty("language").replaceAll("_",
                    "-");
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
                break;
            case MESH:
                for (String extension : supportedModelExtensions)
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
            default:
                break;
        }
        
        return returnVal;
    }
    
    private static void loadApplicationMemoryProperties()
    {
        InputStream input = null;
        
        if (applicationMemoryProperties == null)
        {
            applicationMemoryProperties = new Properties();
        }
        
        try
        {
            File inputFile = new File(getApplicationStorageDirectory() + getApplicationName()
                    + ".properties");
            if (inputFile.exists())
            {
                input = new FileInputStream(inputFile);

                // load a properties file
                applicationMemoryProperties.load(input);
                
                for (DirectoryMemoryProperty directoryMemory : DirectoryMemoryProperty.values())
                {
                    String directory = getLastDirectory(directoryMemory);
                    
                    File directoryFile = new File(directory);
                    if (directoryFile.exists() == false)
                    {
                        setLastDirectory(directoryMemory, getUserStorageDirectory());
                    }
                }
                
            }
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
    
    public static String getLastDirectory(DirectoryMemoryProperty whichProperty)
    {
        if (applicationMemoryProperties == null)
        {
            loadApplicationMemoryProperties();
        }
        
        String directory = applicationMemoryProperties.getProperty(fileMemoryItem
                + whichProperty.name());
        
        return directory;
    }
    
    public static void setLastDirectory(DirectoryMemoryProperty whichProperty, String directoryName)
    {
        if (applicationMemoryProperties == null)
        {
            loadApplicationMemoryProperties();
        }
        
        applicationMemoryProperties.setProperty(fileMemoryItem + whichProperty.name(), directoryName);
        writeApplicationMemory();
    }
    
    public static String getLastPrinterAttached()
    {
        if (applicationMemoryProperties == null)
        {
            loadApplicationMemoryProperties();
        }
        
        String directory = applicationMemoryProperties.getProperty(lastPrinterAttachedMemoryItem);
        
        return directory;
    }
    
    public static void setLastPrinterAttached(String printerID, String firmwareVersion)
    {
        if (applicationMemoryProperties == null)
        {
            loadApplicationMemoryProperties();
        }
        
        String idToWrite = "No Serial";
        if (printerID != null
                && !printerID.equals(""))
        {
            CryptoFileStore a = new CryptoFileStore(lastPrinterAttachedMemoryItem);
            a.encrypt(printerID);
            idToWrite = a.encrypt(printerID);
        }
        applicationMemoryProperties.setProperty(lastPrinterAttachedMemoryItem, idToWrite + "->" + firmwareVersion);
        writeApplicationMemory();
    }
    
    public static String getLastNewsRetrievalTimeAsString()
    {
        if (applicationMemoryProperties == null)
        {
            loadApplicationMemoryProperties();
        }
        
        String lastNewsCheckTimeString = applicationMemoryProperties.getProperty(timeOfLastNewsRetrievalItem);
        
        return lastNewsCheckTimeString;
    }
    
    public static void setLastNewsRetrievalTime(String dateString)
    {
        if (applicationMemoryProperties == null)
        {
            loadApplicationMemoryProperties();
        }
        
        applicationMemoryProperties.setProperty(timeOfLastNewsRetrievalItem, dateString);
        writeApplicationMemory();
    }
    
    public static Locale getUserPreferredLocale()
    {
        if (applicationMemoryProperties == null)
        {
            loadApplicationMemoryProperties();
        }
        
        Locale localeToReturn = null;
        
        if (applicationMemoryProperties.getProperty(userLocaleItem) != null)
        {
            localeToReturn = Locale.forLanguageTag(applicationMemoryProperties.getProperty(
                    userLocaleItem));
        } else
        {
            localeToReturn = Locale.forLanguageTag(getApplicationInstallationLanguage());
        }
        
        return localeToReturn;
    }
    
    public static void setUserPreferredLocale(Locale locale)
    {
        if (applicationMemoryProperties == null)
        {
            loadApplicationMemoryProperties();
        }
        
        applicationMemoryProperties.setProperty(userLocaleItem, locale.getLanguage());
        writeApplicationMemory();
    }

    /**
     *
     */
    public static void writeApplicationMemory()
    {
        if (applicationMemoryProperties == null)
        {
            loadApplicationMemoryProperties();
        }
        
        OutputStream output = null;
        
        try
        {
            output = new FileOutputStream(getApplicationStorageDirectory() + getApplicationName()
                    + ".properties");
            
            applicationMemoryProperties.save(output, getApplicationName() + " runtime properties");
        } catch (IOException ex)
        {
            ex.printStackTrace();
        } finally
        {
            if (output != null)
            {
                try
                {
                    output.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This method supplies the application-specific download directory
     * component for updates It is a hack and should be removed...
     *
     * @param applicationName
     * @return
     */
    public static String getDownloadModifier(String applicationName)
    {
        if (applicationName.equals("CEL AutoMaker"))
        {
            return "0abc523fc24";
        } else if (applicationName.equals("CEL ReelProgrammer"))
        {
            return "14f690bc22c";
        } else if (applicationName.equals("CEL Commissionator"))
        {
            return "1532f2c4ab";
        } else
        {
            return null;
        }
    }
    
    public static boolean isAutoRepairHeads()
    {
        return autoRepairHeads;
    }
    
    public static void setAutoRepairHeads(boolean value)
    {
        autoRepairHeads = value;
    }
    
    public static boolean isAutoRepairReels()
    {
        return autoRepairReels;
    }
    
    public static void setAutoRepairReels(boolean value)
    {
        autoRepairReels = value;
    }
    
    public static String getMainCSSFile()
    {
        return ApplicationConfiguration.class.getResource(mainCSSFile).toExternalForm();
    }
    
    public static String getDialogsCSSFile()
    {
        return ApplicationConfiguration.class.getResource(dialogsCSSFile).toExternalForm();
    }
    
    public static String getMyMiniFactoryDownloadDirectory()
    {
        if (myMiniFactoryDownloadsDirectory == null)
        {
            myMiniFactoryDownloadsDirectory = getUserStorageDirectory() + "MyMiniFactory" + '/';
            
            File dirHandle = new File(myMiniFactoryDownloadsDirectory);
            
            if (!dirHandle.exists())
            {
                dirHandle.mkdirs();
            }
        }
        
        return myMiniFactoryDownloadsDirectory;
    }
    
    public static String getApplicationModelDirectory()
    {
        return getCommonApplicationDirectory().concat(modelStorageDirectoryPath).concat("/");
    }
}
