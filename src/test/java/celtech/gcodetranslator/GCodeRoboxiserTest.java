/*
 * Copyright 2014 CEL UK
 */
package celtech.gcodetranslator;

import celtech.JavaFXConfiguredTest;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParameters;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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

        SlicerParameters SlicerParameters = SlicerParametersContainer.getSettingsByProfileName(DRAFT_SETTINGS);
        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
        RoboxiserResult roboxiserResult = gCodeRoboxiser.roboxiseFile(
            inputURL.getFile(), outputFilePath,
            SlicerParameters, progressProperty);
        assertEquals(20, (long) roboxiserResult.getPrintJobStatistics().getLineNumberOfFirstExtrusion());
    }
    
    @Test
    public void testRoboxiseFileTotalDistanceAndTime() throws IOException
    {
        GCodeRoboxiser gCodeRoboxiser = new GCodeRoboxiser();
        URL url = this.getClass().getResource("/pyramid.gcode");
        String outputFilePath = temporaryFolder.newFile("pyramid.gcode").getCanonicalPath()
            + "out";

        SlicerParameters SlicerParameters = SlicerParametersContainer.getSettingsByProfileName(DRAFT_SETTINGS);
        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
        RoboxiserResult roboxiserResult = gCodeRoboxiser.roboxiseFile(
            url.getFile(),
            outputFilePath,
            SlicerParameters,
            progressProperty);

        List<Double> durationForLayers = roboxiserResult.getPrintJobStatistics().getLayerNumberToPredictedDuration();

        double totalDuration = 0;
        for (Double duration : durationForLayers)
        {
            totalDuration += duration;
        }
        assertEquals(487d, totalDuration, 1);

    }

    @Test
    public void testRoboxiseFileAsExpectedRegressionTest() throws IOException, URISyntaxException
    {
        GCodeRoboxiser gCodeRoboxiser = new GCodeRoboxiser();
        URL inputURL = this.getClass().getResource("/pyramid.gcode");
        String outputFilePath = temporaryFolder.newFile("pyramid.gcode").getCanonicalPath()
            + "out";
        URL expectedDataURL = this.getClass().getResource(
            "/pyramid.expectedroboxgcode");

        SlicerParameters SlicerParameters = SlicerParametersContainer.getSettingsByProfileName(DRAFT_SETTINGS);
        
        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
        gCodeRoboxiser.roboxiseFile(inputURL.getFile(), outputFilePath,
                                    SlicerParameters,
                                    progressProperty);

        String producedFileContents = getFileContentsAsString(Paths.get(outputFilePath));
        String expectedFileContents = getFileContentsAsString(
            Paths.get(expectedDataURL.toURI()));
        assertEquals(expectedFileContents, producedFileContents);

    }

    private String getFileContentsAsString(Path outputFilePath) throws IOException
    {
        byte[] producedData = Files.readAllBytes(outputFilePath);
        return new String(producedData).replaceAll("\r", "");
    }
    
    @Test
    public void writeEventsWithNozzleCloseTest()
    {
        GCodeRoboxiser roboxiser = new GCodeRoboxiser();
        
//        roboxiser.writeEventsWithNozzleClose();
    }

}
