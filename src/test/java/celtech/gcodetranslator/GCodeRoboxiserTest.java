/*
 * Copyright 2014 CEL UK
 */
package celtech.gcodetranslator;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintProfileContainer;
import celtech.services.slicer.RoboxProfile;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author tony
 */
public class GCodeRoboxiserTest
{

    public GCodeRoboxiserTest()
    {
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

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
        Properties testProperties = new Properties();
        testProperties.setProperty("language", "UK");
        ApplicationConfiguration.setInstallationProperties(testProperties);
        Lookup.initialise();

        // force initialisation
        System.setProperty("libertySystems.configFile",
                           "/home/tony/CEL/AutoMaker/AutoMaker.configFile.xml");
        String installDir = ApplicationConfiguration.getApplicationInstallDirectory(Lookup.class);
        PrintProfileContainer.getInstance();
    }

    @After
    public void tearDown()
    {
    }

    @Test
    public void testRoboxiseFileProduces67Layers() throws IOException
    {
        GCodeRoboxiser gCodeRoboxiser = new GCodeRoboxiser();
        URL url = this.getClass().getResource("/pyramid.gcode");
        String outputFilePath = temporaryFolder.newFile("pyramid.gcode").getCanonicalPath();

        RoboxProfile roboxProfile = PrintProfileContainer.getCompleteProfileList().get(0);
        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
        RoboxiserResult roboxiserResult = gCodeRoboxiser.roboxiseFile(url.getFile(), outputFilePath, roboxProfile, progressProperty);
        
        int numLayers = roboxiserResult.getLayerNumberToDistanceTravelled().size();
        assertEquals(67, numLayers);
        assertEquals(roboxiserResult.getLayerNumberToDistanceTravelled().size(),
                     roboxiserResult.getLayerNumberToLineNumber().size());
        for (int i = 0; i < 67; i++)
        {
            Double distance = roboxiserResult.getLayerNumberToDistanceTravelled().get(i);
            System.out.println(i + " " + distance);
        }
    }

}
