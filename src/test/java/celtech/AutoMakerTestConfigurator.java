/*
 * Copyright 2014 CEL UK
 */
package celtech;

import celtech.appManager.TestSystemNotificationManager;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.utils.tasks.TestTaskExecutor;
import java.io.File;
import java.net.URL;
import java.util.Properties;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author tony
 */
public class AutoMakerTestConfigurator
{
    public static void setUp(TemporaryFolder temporaryUserStorageFolder)
    {
        Properties testProperties = new Properties();

        testProperties.setProperty("language", "UK");
        URL applicationInstallURL = AutoMakerTestConfigurator.class.getResource("/");
        URL applicationCommonURL = AutoMakerTestConfigurator.class.getResource("/Common/");
        String userStorageFolder = temporaryUserStorageFolder.getRoot().getAbsolutePath()
            + File.separator;
        ApplicationConfiguration.setInstallationProperties(
            testProperties,
            applicationInstallURL.getFile(),
            applicationCommonURL.getFile(),
            userStorageFolder);
        Lookup.initialise();

        new File(userStorageFolder
            + ApplicationConfiguration.printSpoolStorageDirectoryPath
            + File.separator).mkdir();

        // force initialisation
        URL configURL = AutoMakerTestConfigurator.class.getResource("/AutoMaker.configFile.xml");
        System.setProperty("libertySystems.configFile", configURL.getFile());
        String installDir = ApplicationConfiguration.getApplicationInstallDirectory(
            Lookup.class);
        SlicerParametersContainer.getInstance();

        Lookup.setTaskExecutor(new TestTaskExecutor());
        Lookup.setSystemNotificationHandler(new TestSystemNotificationManager());
    }

}
