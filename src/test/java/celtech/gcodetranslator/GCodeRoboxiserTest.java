/*
 * Copyright 2014 CEL UK
 */
package celtech.gcodetranslator;

import celtech.JavaFXConfiguredTest;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParameters;
import celtech.gcodetranslator.events.CommentEvent;
import celtech.gcodetranslator.events.ExtrusionEvent;
import celtech.gcodetranslator.events.GCodeParseEvent;
import celtech.gcodetranslator.events.LayerChangeWithTravelEvent;
import celtech.gcodetranslator.events.TravelEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
    static String NORMAL_SETTINGS = "NormalSettings";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testInsertTravelAndClosePathBox() throws IOException, URISyntaxException
    {
        SlicerParameters parameters = SlicerParametersContainer.getSettingsByProfileName(DRAFT_SETTINGS);

        ArrayList<GCodeParseEvent> extrusionBuffer = new ArrayList<>();

        //G0 F18000 X0.750 Y24.250 Z0.400
        //;TYPE:WALL-INNER
        //G1 F960 X0.750 Y0.750 E1.41095
        //G1 X24.250 Y0.750 E1.41095
        //G1 X24.250 Y24.250 E1.41095
        //G1 X0.750 Y24.250 E1.41095
        //G0 F18000 X0.450 Y24.550
        //G1 F960 X0.450 Y0.450 E1.44697
        //G1 X24.550 Y0.450 E1.44697
        //G1 X24.550 Y24.550 E1.44697
        //G1 X0.450 Y24.550 E1.44697
        //G0 F18000 X0.150 Y24.850
        //;TYPE:WALL-OUTER
        //G1 F960 X0.150 Y0.150 E1.48300
        //G1 X24.850 Y0.150 E1.48300
        //G1 X24.850 Y24.850 E1.48300
        //G1 X0.150 Y24.850 E1.48300
        //G0 F18000 X1.276 Y24.179
        extrusionBuffer.add(constructLayerChangeWithTravelEvent(0.4, 0.75, 24.25, 18000));

        CommentEvent innerWallComment = new CommentEvent();
        innerWallComment.setComment(";TYPE:WALL-INNER");
        extrusionBuffer.add(innerWallComment);

        extrusionBuffer.add(constructExtrusionEvent(1.41095, 0, 0.75, 0.75, 960, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(1.41095, 0, 24.25, 0.75, 0, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(1.41095, 0, 24.25, 24.25, 0, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(1.41095, 0, 0.75, 24.25, 0, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructTravelEvent(0.45, 24.55, 18000));

        extrusionBuffer.add(constructExtrusionEvent(1.44697, 0, 0.45, 0.45, 960, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(1.44697, 0, 24.55, 0.45, 0, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(1.44697, 0, 24.55, 24.55, 0, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(1.44697, 0, 0.45, 24.55, 0, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructTravelEvent(0.15, 24.85, 18000));

        CommentEvent outerWallComment = new CommentEvent();
        outerWallComment.setComment(";TYPE:WALL-OUTER");
        extrusionBuffer.add(outerWallComment);

        extrusionBuffer.add(constructExtrusionEvent(1.483, 0, 0.15, 0.15, 960, ExtrusionTask.ExternalPerimeter));
        extrusionBuffer.add(constructExtrusionEvent(1.483, 0, 24.85, 0.15, 0, ExtrusionTask.ExternalPerimeter));
        extrusionBuffer.add(constructExtrusionEvent(1.483, 0, 24.85, 24.85, 0, ExtrusionTask.ExternalPerimeter));
        extrusionBuffer.add(constructExtrusionEvent(1.483, 0, 0.15, 24.85, 0, ExtrusionTask.ExternalPerimeter));

        double finalTravelToX = 1.276;
        double finalTravelToY = 24.179;
        extrusionBuffer.add(constructTravelEvent(1.276, 24.179, 18000));

        GCodeRoboxiser gCodeRoboxiser = new GCodeRoboxiser();
        gCodeRoboxiser.currentSettings = parameters;
        gCodeRoboxiser.extrusionBuffer = extrusionBuffer;

        try
        {
            gCodeRoboxiser.insertTravelAndClosePath(2, 16, "nothing", false, null, 0.5);

            // Check that there is a travel event to the next part
            TravelEvent travelToWipe = (TravelEvent) gCodeRoboxiser.extrusionBuffer.get(17);
            assertEquals(travelToWipe.getX(), 24.25, 1e-12);
            assertEquals(travelToWipe.getY(), 24.25, 1e-12);

            // Check that there is a single wipe extrusion event - we're only wiping 0.5mm3 so this only requires part of the line
            ExtrusionEvent wipeEvent = (ExtrusionEvent) gCodeRoboxiser.extrusionBuffer.get(18);
            assertEquals(15.922, wipeEvent.getX(), 1e-3);
            assertEquals(24.25, wipeEvent.getY(), 1e-12);

            // Check that the final event is the travel to the next part
            TravelEvent finalTravel = (TravelEvent) gCodeRoboxiser.extrusionBuffer.get(gCodeRoboxiser.extrusionBuffer.size() - 1);
            assertEquals(finalTravelToX, finalTravel.getX(), 1e-12);
            assertEquals(finalTravelToY, finalTravel.getY(), 1e-12);
        } catch (PostProcessingError ex)
        {
            fail("Got PostProcessingError");
        }
    }

    @Test
    public void testInsertTravelAndClosePathConcentricBox() throws IOException, URISyntaxException
    {
        SlicerParameters parameters = SlicerParametersContainer.getSettingsByProfileName(DRAFT_SETTINGS);

        ArrayList<GCodeParseEvent> extrusionBuffer = new ArrayList<>();

        /*
         * 2.25 - 22.75
         * E 1.23083
         * Goto RF
         * LF->LB->RB->RF
         */
        //G0 F18000 X22.750 Y2.250 Z3.800
        //;TYPE:WALL-INNER
        //G1 F780 X2.250 Y2.250 E1.23083
        //G1 X2.250 Y22.750 E1.23083
        //G1 X22.750 Y22.750 E1.23083
        //G1 X22.750 Y2.250 E1.23083
        
        /*
         * 0.75 - 24.25
         * E 1.41095
         * Goto RF
         * LF->LB->RB->RF
         */
        //G0 F18000 X24.250 Y0.750
        // RB->LB->LF->RF
        //G1 F780 X24.250 Y24.250 E1.41095
        //G1 X0.750 Y24.250 E1.41095
        //G1 X0.750 Y0.750 E1.41095
        //G1 X24.250 Y0.750 E1.41095
        
        /*
         * 0.45 - 24.55
         * E 1.44697
         * Goto RF
         * RB->LB->LF->RF
         */
        //G0 F18000 X24.550 Y0.450
        //G1 F780 X24.550 Y24.550 E1.44697
        //G1 X0.450 Y24.550 E1.44697
        //G1 X0.450 Y0.450 E1.44697
        //G1 X24.550 Y0.450 E1.44697

        /*
         * 2.55 - 22.45
         * E 1.19480
         * Goto RF
         * LF->LB->RB->RF
         */
        //G0 F18000 X22.450 Y2.550
        //G1 F780 X2.550 Y2.550 E1.19480
        //G1 X2.550 Y22.450 E1.19480
        //G1 X22.450 Y22.450 E1.19480
        //G1 X22.450 Y2.550 E1.19480

        /*
         * 2.85 - 22.15
         * E 1.15878
         * Goto RF
         * LF->LB->RB->RF
         */
        //G0 F18000 X22.150 Y2.850
        //;TYPE:WALL-OUTER
        //G1 F780 X2.850 Y2.850 E1.15878
        //G1 X2.850 Y22.150 E1.15878
        //G1 X22.150 Y22.150 E1.15878
        //G1 X22.150 Y2.850 E1.15878

        /*
         * 0.15 - 24.85
         * E 1.48300
         * Goto RF
         * RB->LB->LF->RF
         */
        //G0 F18000 X22.150 Y2.600
        //G0 X24.600 Y0.310
        //G0 X24.850 Y0.150
        //G1 F780 X24.850 Y24.850 E1.48300
        //G1 X0.150 Y24.850 E1.48300
        //G1 X0.150 Y0.150 E1.48300
        //G1 X24.850 Y0.150 E1.48300
        //G0 F18000 X24.179 Y1.269
        
        
        /*
         * 2.25 - 22.75
         * E 1.23083
         * Goto RF
         * LF->LB->RB->RF
         */
        double lhX = 2.25;
        double rhX = 22.75;
        double backY = 22.75;
        double frontY = 2.25;
        double extrusion = 1.23083;

        extrusionBuffer.add(constructLayerChangeWithTravelEvent(3.8, rhX, frontY, 18000));

        CommentEvent innerWallComment = new CommentEvent();
        innerWallComment.setComment(";TYPE:WALL-INNER");
        extrusionBuffer.add(innerWallComment);

        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, lhX, frontY, 780, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, lhX, backY, 0, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, rhX, backY, 0, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, rhX, frontY, 0, ExtrusionTask.Perimeter));

        /*
         * 0.75 - 24.25
         * E 1.41095
         * Goto RF
         * LF->LB->RB->RF
         */
        lhX = frontY = 0.75;
        rhX = backY = 24.25;
        extrusion = 1.41095;

        extrusionBuffer.add(constructTravelEvent(rhX, frontY, 18000));

        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, lhX, frontY, 960, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, lhX, backY, 0, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, rhX, backY, 0, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, rhX, frontY, 0, ExtrusionTask.Perimeter));

        /*
         * 0.45 - 24.55
         * E 1.44697
         * Goto RF
         * RB->LB->LF->RF
         */
        lhX = frontY = 0.45;
        rhX = backY = 24.55;
        extrusion = 1.44697;

        extrusionBuffer.add(constructTravelEvent(rhX, frontY, 18000));

        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, rhX, backY, 960, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, lhX, backY, 0, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, lhX, frontY, 0, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, rhX, frontY, 0, ExtrusionTask.Perimeter));

        /*
         * 2.55 - 22.45
         * E 1.19480
         * Goto RF
         * LF->LB->RB->RF
         */
        lhX = frontY = 2.55;
        rhX = backY = 22.45;
        extrusion = 1.19480;

        extrusionBuffer.add(constructTravelEvent(rhX, frontY, 18000));

        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, lhX, frontY, 960, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, lhX, backY, 0, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, rhX, backY, 0, ExtrusionTask.Perimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, rhX, frontY, 0, ExtrusionTask.Perimeter));

        /*
         * 2.85 - 22.15
         * E 1.15878
         * Goto RF
         * LF->LB->RB->RF
         */
        lhX = frontY = 2.85;
        rhX = backY = 22.15;
        extrusion = 1.15878;

        extrusionBuffer.add(constructTravelEvent(rhX, frontY, 18000));

        CommentEvent outerWallComment = new CommentEvent();
        outerWallComment.setComment(";TYPE:WALL-OUTER");
        extrusionBuffer.add(outerWallComment);

        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, lhX, frontY, 960, ExtrusionTask.ExternalPerimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, lhX, backY, 0, ExtrusionTask.ExternalPerimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, rhX, backY, 0, ExtrusionTask.ExternalPerimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, rhX, frontY, 0, ExtrusionTask.ExternalPerimeter));

        /*
         * 0.15 - 24.85
         * E 1.48300
         * Goto RF
         * RB->LB->LF->RF
         */
        //G1 F780 X24.850 Y24.850 E1.48300
        //G1 X0.150 Y24.850 E1.48300
        //G1 X0.150 Y0.150 E1.48300
        //G1 X24.850 Y0.150 E1.48300
        //G0 F18000 X24.179 Y1.269
        lhX = frontY = 0.15;
        rhX = backY = 24.85;
        extrusion = 1.48300;

        extrusionBuffer.add(constructTravelEvent(22.15, 2.6, 18000));
        extrusionBuffer.add(constructTravelEvent(24.6, 0.310, 0));
        extrusionBuffer.add(constructTravelEvent(rhX, frontY, 0));

        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, rhX, backY, 960, ExtrusionTask.ExternalPerimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, lhX, backY, 0, ExtrusionTask.ExternalPerimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, lhX, frontY, 0, ExtrusionTask.ExternalPerimeter));
        extrusionBuffer.add(constructExtrusionEvent(extrusion, 0, rhX, frontY, 0, ExtrusionTask.ExternalPerimeter));

        double finalTravelToX = 24.179;
        double finalTravelToY = 1.269;
        extrusionBuffer.add(constructTravelEvent(finalTravelToX, finalTravelToY, 18000));

        GCodeRoboxiser gCodeRoboxiser = new GCodeRoboxiser();
        gCodeRoboxiser.currentSettings = parameters;
        gCodeRoboxiser.extrusionBuffer = extrusionBuffer;

        try
        {
            gCodeRoboxiser.insertTravelAndClosePath(2, 33, "nothing", false, null, 0.5);

            // Check that there is a travel event to the next part
            TravelEvent travelToWipe = (TravelEvent) gCodeRoboxiser.extrusionBuffer.get(34);
            assertEquals(0.45, travelToWipe.getX(), 1e-12);
            assertEquals(0.45, travelToWipe.getY(), 1e-12);

            // Check that there is a single wipe extrusion event - we're only wiping 0.5mm3 so this only requires part of the line
            ExtrusionEvent wipeEvent = (ExtrusionEvent) gCodeRoboxiser.extrusionBuffer.get(35);
            assertEquals(8.777, wipeEvent.getX(), 1e-3);
            assertEquals(0.45, wipeEvent.getY(), 1e-12);

            // Check that the final event is the travel to the next part
            TravelEvent finalTravel = (TravelEvent) gCodeRoboxiser.extrusionBuffer.get(gCodeRoboxiser.extrusionBuffer.size() - 1);
            assertEquals(finalTravelToX, finalTravel.getX(), 1e-12);
            assertEquals(finalTravelToY, finalTravel.getY(), 1e-12);
        } catch (PostProcessingError ex)
        {
            fail("Got PostProcessingError");
        }
    }

//    @Test
//    public void testRoboxiseFileProducesFirstExtrusionLineNumber() throws IOException
//    {
//        GCodeRoboxiser gCodeRoboxiser = new GCodeRoboxiser();
//        URL inputURL = this.getClass().getResource("/pyramid.gcode");
//        String outputFilePath = temporaryFolder.newFile("pyramid.gcode").getCanonicalPath()
//            + "out";
//
//        SlicerParameters SlicerParameters = SlicerParametersContainer.getSettingsByProfileName(DRAFT_SETTINGS);
//        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
//        RoboxiserResult roboxiserResult = gCodeRoboxiser.roboxiseFile(
//            inputURL.getFile(), outputFilePath,
//            SlicerParameters, progressProperty);
//        assertEquals(20, (long) roboxiserResult.getPrintJobStatistics().getLineNumberOfFirstExtrusion());
//    }
//    
//    @Test
//    public void testRoboxiseFileTotalDistanceAndTime() throws IOException
//    {
//        GCodeRoboxiser gCodeRoboxiser = new GCodeRoboxiser();
//        URL url = this.getClass().getResource("/pyramid.gcode");
//        String outputFilePath = temporaryFolder.newFile("pyramid.gcode").getCanonicalPath()
//            + "out";
//
//        SlicerParameters SlicerParameters = SlicerParametersContainer.getSettingsByProfileName(DRAFT_SETTINGS);
//        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
//        RoboxiserResult roboxiserResult = gCodeRoboxiser.roboxiseFile(
//            url.getFile(),
//            outputFilePath,
//            SlicerParameters,
//            progressProperty);
//
//        List<Double> durationForLayers = roboxiserResult.getPrintJobStatistics().getLayerNumberToPredictedDuration();
//
//        double totalDuration = 0;
//        for (Double duration : durationForLayers)
//        {
//            totalDuration += duration;
//        }
//        assertEquals(487d, totalDuration, 1);
//
//    }
//
//    @Test
//    public void testRoboxiseFileAsExpectedRegressionTest() throws IOException, URISyntaxException
//    {
//        GCodeRoboxiser gCodeRoboxiser = new GCodeRoboxiser();
//        URL inputURL = this.getClass().getResource("/pyramid.gcode");
//        String outputFilePath = temporaryFolder.newFile("pyramid.gcode").getCanonicalPath()
//            + "out";
//        URL expectedDataURL = this.getClass().getResource(
//            "/pyramid.expectedroboxgcode");
//
//        SlicerParameters SlicerParameters = SlicerParametersContainer.getSettingsByProfileName(DRAFT_SETTINGS);
//        
//        DoubleProperty progressProperty = new SimpleDoubleProperty(0);
//        gCodeRoboxiser.roboxiseFile(inputURL.getFile(), outputFilePath,
//                                    SlicerParameters,
//                                    progressProperty);
//
//        String producedFileContents = getFileContentsAsString(Paths.get(outputFilePath));
//        String expectedFileContents = getFileContentsAsString(
//            Paths.get(expectedDataURL.toURI()));
//        assertEquals(expectedFileContents, producedFileContents);
//
//    }
//
//    private String getFileContentsAsString(Path outputFilePath) throws IOException
//    {
//        byte[] producedData = Files.readAllBytes(outputFilePath);
//        return new String(producedData).replaceAll("\r", "");
//    }
//    
//    @Test
//    public void writeEventsWithNozzleCloseTest()
//    {
//        GCodeRoboxiser roboxiser = new GCodeRoboxiser();
//        
////        roboxiser.writeEventsWithNozzleClose();
//    }
    private GCodeParseEvent constructExtrusionEvent(double e, double d, double x, double y, int feedrate, ExtrusionTask task)
    {
        ExtrusionEvent extrusionEvent = new ExtrusionEvent();
        if (feedrate > 0)
        {
            extrusionEvent.setFeedRate(feedrate);
        }
        extrusionEvent.setE(e);
        extrusionEvent.setD(d);
        extrusionEvent.setX(x);
        extrusionEvent.setY(y);
        extrusionEvent.setExtrusionTask(task);

        return extrusionEvent;
    }

    private GCodeParseEvent constructLayerChangeWithTravelEvent(double z, double x, double y, int feedrate)
    {
        LayerChangeWithTravelEvent layerChange = new LayerChangeWithTravelEvent();
        layerChange.setZ(z);
        layerChange.setFeedRate(feedrate);
        layerChange.setX(x);
        layerChange.setY(y);

        return layerChange;
    }

    private GCodeParseEvent constructTravelEvent(double x, double y, int feedrate)
    {
        TravelEvent travelEvent = new TravelEvent();
        if (feedrate > 0)
        {
            travelEvent.setFeedRate(feedrate);
        }
        travelEvent.setX(x);
        travelEvent.setY(y);

        return travelEvent;
    }

}
