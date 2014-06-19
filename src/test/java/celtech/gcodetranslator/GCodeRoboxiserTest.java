/*
 * Copyright 2014 CEL UK
 */
package celtech.gcodetranslator;

import celtech.JavaFXConfiguredTest;
import celtech.configuration.PrintProfileContainer;
import celtech.services.slicer.RoboxProfile;
import java.io.IOException;
import java.net.URL;
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

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testRoboxiseFileProduces67Layers() throws IOException
    {
        GCodeRoboxiser gCodeRoboxiser = new GCodeRoboxiser();
        URL url = this.getClass().getResource("/pyramid.gcode");
        String outputFilePath = temporaryFolder.newFile("pyramid.gcode").getCanonicalPath();

        RoboxProfile roboxProfile = PrintProfileContainer.getCompleteProfileList().get(0);
        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
        RoboxiserResult roboxiserResult = gCodeRoboxiser.roboxiseFile(url.getFile(),
                                                                      outputFilePath,
                                                                      roboxProfile,
                                                                      progressProperty);

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

    @Test
    public void testRoboxiseFileProducesFirstExtrusionLineNumber() throws IOException
    {
        GCodeRoboxiser gCodeRoboxiser = new GCodeRoboxiser();
        URL url = this.getClass().getResource("/pyramid.gcode");
        String outputFilePath = temporaryFolder.newFile("pyramid.gcode").getCanonicalPath();

        RoboxProfile roboxProfile = PrintProfileContainer.getCompleteProfileList().get(0);
        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
        RoboxiserResult roboxiserResult = gCodeRoboxiser.roboxiseFile(url.getFile(),
                                                                      outputFilePath,
                                                                      roboxProfile,
                                                                      progressProperty);
        assertEquals(34, (long) roboxiserResult.getLineNumberOfFirstExtrusion());
    }
    
    @Test
    public void testRoboxiseFileTotalDistanceAndTime() throws IOException
    {
        GCodeRoboxiser gCodeRoboxiser = new GCodeRoboxiser();
        URL url = this.getClass().getResource("/pyramid.gcode");
        String outputFilePath = temporaryFolder.newFile("pyramid.gcode").getCanonicalPath();

        RoboxProfile roboxProfile = PrintProfileContainer.getCompleteProfileList().get(0);
        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
        RoboxiserResult roboxiserResult = gCodeRoboxiser.roboxiseFile(url.getFile(),
                                                                      outputFilePath,
                                                                      roboxProfile,
                                                                      progressProperty);
        
        List<Double> distanceForLayers = roboxiserResult.getLayerNumberToDistanceTravelled();
        List<Double> durationForLayers = roboxiserResult.getLayerNumberToPredictedDuration();
        double totalDistance = 0;
        for (Double distance : distanceForLayers)
        {
            totalDistance += distance;
        }
        assertEquals(9465d, totalDistance, 1); 
       
       double totalDuration = 0;
       for (Double duration : durationForLayers)
        {
            totalDuration += duration;
        }
        assertEquals(536d, totalDuration, 1);        

    }    

}
