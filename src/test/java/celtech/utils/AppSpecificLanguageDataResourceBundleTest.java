package celtech.utils;

import celtech.roboxbase.i18n.UTF8Control;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
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
        File file = new File("src/test/resources/InstallDir/AutoMaker/Language");
        ResourceBundle bundle = null;

        try
        {
            URL[] urls =
            {
                file.toURI().toURL()
            };
            ClassLoader loader = new URLClassLoader(urls);
            bundle = ResourceBundle.getBundle("AutoMakerLanguageData", Locale.getDefault(), loader, new UTF8Control());

            assertEquals("Bed", bundle.getString("reelPanel.bed"));
            assertEquals(862, bundle.keySet().size());
        } catch (MalformedURLException ex)
        {
            fail();
        }
    }

}
