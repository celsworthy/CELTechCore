package celtech.printerControl.model;

import celtech.Lookup;
import celtech.gcodetranslator.postprocessing.nodes.CommentNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeDirectiveNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerChangeDirectiveNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.Movement;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    private static Stenographer steno = StenographerFactory.getStenographer(CameraTriggerManager.class.getName());
    private Printer associatedPrinter = null;
    private static final int moveFeedrate_mm_per_min = 12000;
    private final ScheduledExecutorService scheduledPhoto;
    private final Runnable photoRun;

    public CameraTriggerManager(Printer printer)
    {
        associatedPrinter = printer;
        scheduledPhoto = Executors.newSingleThreadScheduledExecutor();
        photoRun = new Runnable()
        {
            @Override
            public void run()
            {
                steno.debug("Firing camera");
                String goProURLString = "http://10.5.5.9/camera/SH?t=" + Lookup.getUserPreferences().getGoProWifiPassword() + "&p=%01";
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
                        steno.debug("Took picture");
                    } else
                    {
                        steno.error("Failed to take picture - response was " + responseCode);
                    }
                } catch (IOException ex)
                {
                    steno.error("Exception whilst attempting to take GoPro picture");
                }
            }
        };
    }

    private final ChangeListener<Number> cameraTriggerListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        if (newValue.intValue() > oldValue.intValue())
        {
            triggerCamera();
        }
    };

    public static void appendLayerEndTriggerCode(LayerChangeDirectiveNode layerChangeNode)
    {
        CommentNode beginComment = new CommentNode("Start of camera trigger");
        CommentNode endComment = new CommentNode("End of camera trigger");

        TravelNode moveBedForward = new TravelNode();

        boolean outputMoveCommand = false;
        int xMoveInt = Lookup.getUserPreferences().getGoProXMove();
        moveBedForward.getMovement().setX(xMoveInt);
        outputMoveCommand = true;

        int yMoveInt = Lookup.getUserPreferences().getGoProYMove();
        moveBedForward.getMovement().setY(yMoveInt);
        outputMoveCommand = true;

        moveBedForward.getFeedrate().setFeedRate_mmPerMin(moveFeedrate_mm_per_min);

        GCodeDirectiveNode dwellWhilePictureTaken = new GCodeDirectiveNode();
        dwellWhilePictureTaken.setGValue(4);
        dwellWhilePictureTaken.setSValue(Lookup.getUserPreferences().getGoProDelay());

        TravelNode returnToPreviousPosition = new TravelNode();
        returnToPreviousPosition.getMovement().setX(layerChangeNode.getMovement().getX());
        returnToPreviousPosition.getMovement().setY(layerChangeNode.getMovement().getY());
        returnToPreviousPosition.getFeedrate().setFeedRate_mmPerMin(moveFeedrate_mm_per_min);

        layerChangeNode.addSiblingAfter(endComment);
        layerChangeNode.addSiblingAfter(returnToPreviousPosition);
        layerChangeNode.addSiblingAfter(dwellWhilePictureTaken);
        if (outputMoveCommand)
        {
            layerChangeNode.addSiblingAfter(moveBedForward);
        }
        layerChangeNode.addSiblingAfter(beginComment);
    }

    public void listenForCameraTrigger()
    {
        steno.debug("Started listening for camera trigger");
        associatedPrinter.getPrintEngine().progressCurrentLayerProperty().addListener(cameraTriggerListener);
    }

    public void stopListeningForCameraTrigger()
    {
        steno.debug("Stopped listening for camera trigger");
        associatedPrinter.getPrintEngine().progressCurrentLayerProperty().removeListener(cameraTriggerListener);
    }

    private void triggerCamera()
    {
        steno.debug("Asked to trigger camera");
        scheduledPhoto.schedule(photoRun, Lookup.getUserPreferences().getGoProDelayBeforeCapture(), TimeUnit.SECONDS);
    }
}
