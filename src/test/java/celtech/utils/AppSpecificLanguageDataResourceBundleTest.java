package celtech.utils;

import celtech.Lookup;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.i18n.UTF8Control;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ianhudson
 */
public class AppSpecificLanguageDataResourceBundleTest
{

    public AppSpecificLanguageDataResourceBundleTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }

    @Test
    public void testLocaleUK_appdata()
    {
        URL applicationURL = AppSpecificLanguageDataResourceBundleTest.class.getResource("/");        
        String configDir = applicationURL.getPath();
        String configFile = configDir + "AutoMaker.configFile.xml";
        System.setProperty("libertySystems.configFile", configFile);
        System.out.println("System.getProperty(libertySystems.configFile) returns " + System.getProperty("libertySystems.configFile"));

        Locale.setDefault(Locale.ENGLISH);
        Properties testProperties = new Properties();

        testProperties.setProperty("language", "UK");
        
        applicationURL = AppSpecificLanguageDataResourceBundleTest.class.getResource("/InstallDir/AutoMaker/");        
        String installDir = applicationURL.getPath();
        
        BaseConfiguration.setInstallationProperties(
                testProperties,
                installDir,
                "");
        Lookup.setupDefaultValues(); // Need to do this to load the resource bundles, otherwise the getBundle fails because of a recursive call to getBundle() in LanugagePropertiesResourceBundle.
        
        ResourceBundle bundle = ResourceBundle.getBundle("celtech.roboxbase.i18n.languagedata.LanguageData");
        assertEquals("Bed", bundle.getString("reelPanel.bed"));
        assertEquals(1042, bundle.keySet().size());
    }
}
