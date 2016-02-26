package celtech.utils;

import celtech.roboxbase.UTF8Control;
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
    public void testLocaleUK_basedata()
    {
        Locale.setDefault(Locale.ENGLISH);
        ResourceBundle bundle = ResourceBundle.getBundle("celtech.roboxbase.utils.language.LanguageDataResourceBundle", new UTF8Control());
        assertEquals("Nozzle firmware control", bundle.getString("error.ERROR_B_POSITION_LOST"));
        assertEquals(1012, bundle.keySet().size());
    }
    
    @Test
    public void testLocaleUK_appdata()
    {
        Locale.setDefault(Locale.ENGLISH);
        ResourceBundle bundle = ResourceBundle.getBundle("celtech.roboxbase.utils.language.LanguageDataResourceBundle", new UTF8Control());
        assertEquals("Bed", bundle.getString("reelPanel.bed"));
        assertEquals(1012, bundle.keySet().size());
    }
    
}
