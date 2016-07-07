package celtech.utils;

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
        Locale.setDefault(Locale.ENGLISH);
        Properties testProperties = new Properties();

        testProperties.setProperty("language", "UK");
        
        URL applicationInstallURL = AppSpecificLanguageDataResourceBundleTest.class.getResource("/InstallDir/AutoMaker/");        
        String installDir = applicationInstallURL.getPath();

        BaseConfiguration.setInstallationProperties(
                testProperties,
                installDir,
                "");

        Locale.setDefault(Locale.ENGLISH);
        ResourceBundle bundle = ResourceBundle.getBundle("celtech.roboxbase.i18n.languagedata.LanguageData");

        assertEquals("Bed", bundle.getString("reelPanel.bed"));
        assertEquals(1033, bundle.keySet().size());
    }
}
