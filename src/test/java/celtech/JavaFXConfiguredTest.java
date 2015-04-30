/*
 * Copyright 2014 CEL UK
 */
package celtech;

import celtech.appManager.TestSystemNotificationManager;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.gcodetranslator.TestGCodeOutputWriter;
import celtech.utils.tasks.TestTaskExecutor;
import java.io.File;
import java.net.URL;
import java.util.Properties;
import javafx.application.Application;
import javafx.stage.Stage;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author tony
 */
public class JavaFXConfiguredTest
{

    @Rule
    public TemporaryFolder temporaryUserStorageFolder = new TemporaryFolder();
    public String userStorageFolderPath;

    @Before
    public void setUp()
    {
        Properties testProperties = new Properties();

        testProperties.setProperty("language", "UK");
        URL applicationInstallURL = JavaFXConfiguredTest.class.getResource("/InstallDir/AutoMaker/");
        userStorageFolderPath = temporaryUserStorageFolder.getRoot().getAbsolutePath()
            + File.separator;
        ApplicationConfiguration.setInstallationProperties(
            testProperties,
            applicationInstallURL.getFile(),
            userStorageFolderPath);
        
        File filamentDir = new File(userStorageFolderPath
            + ApplicationConfiguration.filamentDirectoryPath
            + File.separator);
        filamentDir.mkdirs();
        
        new File(userStorageFolderPath
            + ApplicationConfiguration.printSpoolStorageDirectoryPath
            + File.separator).mkdirs();

        new File(userStorageFolderPath
            + ApplicationConfiguration.projectFileDirectoryPath
            + File.separator).mkdirs();
        
        Lookup.setupDefaultValues();

        // force initialisation
        URL configURL = JavaFXConfiguredTest.class.getResource("/AutoMaker.configFile.xml");
        System.setProperty("libertySystems.configFile", configURL.getFile());
        String installDir = ApplicationConfiguration.getApplicationInstallDirectory(
            Lookup.class);
        SlicerParametersContainer.getInstance();

        Lookup.setTaskExecutor(new TestTaskExecutor());
        Lookup.setSystemNotificationHandler(new TestSystemNotificationManager());

        Lookup.setPostProcessorOutputWriterFactory(TestGCodeOutputWriter::new);
    }

    public static class AsNonApp extends Application
    {

        @Override
        public void start(Stage primaryStage) throws Exception
        {
            // noop
        }
    }

    public static boolean startedJFX = false;

    @BeforeClass
    public static void initJFX()
    {
        if (!startedJFX)
        {
            Thread t = new Thread("JavaFX Init Thread")
            {
                public void run()
                {
                    Application.launch(AsNonApp.class, new String[0]);
                }
            };
            t.setDaemon(true);
            t.start();
            startedJFX = true;
        }
    }

}
