/*
 * Copyright 2014 CEL UK
 */
package celtech.gcodetranslator;

import celtech.JavaFXConfiguredTest;
import celtech.configuration.PrintProfileContainer;
import celtech.services.slicer.RoboxProfile;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author tony
 */
public class GCodeRoboxiserTest extends JavaFXConfiguredTest
{
    static String DRAFT_SETTINGS = "DraftSettings";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testRoboxiseFileProducesFirstExtrusionLineNumber() throws IOException
    {
        GCodeRoboxiser gCodeRoboxiser = new GCodeRoboxiser();
        URL inputURL = this.getClass().getResource("/pyramid.gcode");
        String outputFilePath = temporaryFolder.newFile("pyramid.gcode").getCanonicalPath()
            + "out";

        RoboxProfile roboxProfile = PrintProfileContainer.getSettingsByProfileName(DRAFT_SETTINGS);
        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
        RoboxiserResult roboxiserResult = gCodeRoboxiser.roboxiseFile(
            inputURL.getFile(), outputFilePath,
            roboxProfile, progressProperty);
        assertEquals(34, (long) roboxiserResult.getPrintJobStatistics().getLineNumberOfFirstExtrusion());
    }
    
    @Test
    public void testRoboxiseFileTotalDistanceAndTime() throws IOException
    {
        GCodeRoboxiser gCodeRoboxiser = new GCodeRoboxiser();
        URL url = this.getClass().getResource("/pyramid.gcode");
        String outputFilePath = temporaryFolder.newFile("pyramid.gcode").getCanonicalPath()
            + "out";

        RoboxProfile roboxProfile = PrintProfileContainer.getSettingsByProfileName(DRAFT_SETTINGS);
        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
        RoboxiserResult roboxiserResult = gCodeRoboxiser.roboxiseFile(
            url.getFile(),
            outputFilePath,
            roboxProfile,
            progressProperty);

        List<Double> durationForLayers = roboxiserResult.getPrintJobStatistics().getLayerNumberToPredictedDuration();

        double totalDuration = 0;
        for (Double duration : durationForLayers)
        {
            totalDuration += duration;
        }
        assertEquals(551d, totalDuration, 1);

    }

    @Test
    public void testRoboxiseFileAsExpectedRegressionTest() throws IOException
    {
        GCodeRoboxiser gCodeRoboxiser = new GCodeRoboxiser();
        URL inputURL = this.getClass().getResource("/pyramid.gcode");
        String outputFilePath = temporaryFolder.newFile("pyramid.gcode").getCanonicalPath()
            + "out";
        URL expectedDataURL = this.getClass().getResource(
            "/pyramid.expectedroboxgcode");

        RoboxProfile roboxProfile = PrintProfileContainer.getSettingsByProfileName(DRAFT_SETTINGS);
        
        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
        gCodeRoboxiser.roboxiseFile(inputURL.getFile(), outputFilePath,
                                    roboxProfile,
                                    progressProperty);

        String producedFileContents = getFileContentsAsString(outputFilePath);
        String expectedFileContents = getFileContentsAsString(
            expectedDataURL.getFile());
        assertEquals(expectedFileContents, producedFileContents);

    }

    private String getFileContentsAsString(String outputFilePath) throws IOException
    {
        byte[] producedData = Files.readAllBytes(Paths.get(outputFilePath));
        return new String(producedData);
    }

}
