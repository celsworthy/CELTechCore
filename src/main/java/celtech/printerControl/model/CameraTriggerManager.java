package celtech.printerControl.model;

import celtech.Lookup;
import celtech.gcodetranslator.postprocessing.nodes.CommentNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeDirectiveNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.Movement;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CameraTriggerManager
{

    private Stenographer steno = StenographerFactory.getStenographer(CameraTriggerManager.class.getName());
    private Printer associatedPrinter = null;
    private static final float xTriggerPosition = 30;
    private static final float yTriggerPosition = 0;
    private static final float yForwardPosition = 75;

    public CameraTriggerManager(Printer printer)
    {
        associatedPrinter = printer;
    }

    private ChangeListener<Boolean> cameraTriggerListener = new ChangeListener<Boolean>()
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            if (!oldValue && newValue)
            {
                triggerCamera();
            }
        }
    };

    public static void appendLayerEndTriggerCode(LayerNode layer, Movement lastMovement)
    {
        CommentNode beginComment = new CommentNode("Start of camera trigger");
        CommentNode endComment = new CommentNode("End of camera trigger");

        GCodeDirectiveNode relativeMove = new GCodeDirectiveNode();
        relativeMove.setGValue(91);

        GCodeDirectiveNode absoluteMove = new GCodeDirectiveNode();
        absoluteMove.setGValue(90);

        TravelNode moveOnUp = new TravelNode();
        moveOnUp.getMovement().setZ(0.5);
        moveOnUp.getFeedrate().setFeedRate_mmPerMin(100);

        TravelNode moveToCameraPosition = new TravelNode();
        moveToCameraPosition.getMovement().setX(xTriggerPosition);
        moveToCameraPosition.getMovement().setY(yTriggerPosition);
        moveToCameraPosition.getFeedrate().setFeedRate_mmPerMin(1000);

        GCodeDirectiveNode dwellToAllowTrigger = new GCodeDirectiveNode();
        dwellToAllowTrigger.setGValue(4);
        dwellToAllowTrigger.setPValue(700);

        TravelNode moveBedForward = new TravelNode();
        moveBedForward.getMovement().setY(yForwardPosition);
        moveBedForward.getFeedrate().setFeedRate_mmPerMin(100);

        GCodeDirectiveNode dwellWhilePictureTaken = new GCodeDirectiveNode();
        dwellWhilePictureTaken.setGValue(4);
        dwellWhilePictureTaken.setSValue(2);

        TravelNode returnToPreviousPosition = new TravelNode();
        returnToPreviousPosition.getMovement().setX(lastMovement.getX());
        returnToPreviousPosition.getMovement().setY(lastMovement.getY());
        returnToPreviousPosition.getFeedrate().setFeedRate_mmPerMin(1000);

        TravelNode moveDown = new TravelNode();
        moveDown.getMovement().setZ(-0.5);
        moveDown.getFeedrate().setFeedRate_mmPerMin(100);

        layer.addChildAtEnd(beginComment);
        layer.addChildAtEnd(relativeMove);
        layer.addChildAtEnd(moveOnUp);
        layer.addChildAtEnd(absoluteMove);
        layer.addChildAtEnd(moveToCameraPosition);
        layer.addChildAtEnd(dwellToAllowTrigger);
        layer.addChildAtEnd(moveBedForward);
        layer.addChildAtEnd(dwellWhilePictureTaken);
        layer.addChildAtEnd(returnToPreviousPosition);
        layer.addChildAtEnd(relativeMove);
        layer.addChildAtEnd(moveDown);
        layer.addChildAtEnd(absoluteMove);
        layer.addChildAtEnd(endComment);
    }

    public void listenForCameraTrigger()
    {
        steno.info("Started listening");
        associatedPrinter.getPrinterAncillarySystems().YStopSwitch.addListener(cameraTriggerListener);
    }

    public void stopListeningForCameraTrigger()
    {
        steno.info("Stopped listening");
        associatedPrinter.getPrinterAncillarySystems().YStopSwitch.removeListener(cameraTriggerListener);
    }

    private void triggerCamera()
    {
        String goProURLString = "http://10.5.5.9/camera/SH?t=" + Lookup.getUserPreferences().getGoProWifiPassword() + "&p=%01";

        steno.info("Snap");
        try
        {
            URL obj = new URL(goProURLString);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.44 (KHTML, like Gecko) JavaFX/8.0 Safari/537.44");

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setConnectTimeout(500);
            int responseCode = con.getResponseCode();

            if (responseCode == 200
                    && con.getContentLength() > 0)
            {
                steno.info("Took picture");
            } else
            {
                steno.info("Failed to take picture - response was " + responseCode);
            }
        } catch (IOException ex)
        {
            steno.error("Exception whilst attempting to take GoPro picture");
        }
    }
}
