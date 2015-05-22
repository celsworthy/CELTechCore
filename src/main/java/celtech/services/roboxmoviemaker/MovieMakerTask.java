/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.roboxmoviemaker;

import celtech.configuration.ApplicationConfiguration;
import celtech.printerControl.model.Printer;
import java.io.File;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.CvSize;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_highgui;
import static org.bytedeco.javacpp.opencv_highgui.CV_FOURCC;
import org.bytedeco.javacpp.opencv_highgui.CvCapture;
import org.bytedeco.javacpp.opencv_highgui.CvVideoWriter;

/**
 *
 * @author ianhudson
 */
public class MovieMakerTask extends Task<MovieMakerResult>
{
    
    private final Stenographer steno = StenographerFactory.getStenographer(MovieMakerTask.class.getName());
    private String printJobUUID = null;
    private Printer printerToUse = null;
    
    private final ChangeListener<Boolean> triggerListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
        if (oldValue == false && newValue == true)
        {
            takeImage();
        }
    };
    
    private CvCapture frameGrabber = null;
    private CvVideoWriter videoWriter = null;
    private boolean keepRunning = false;

    /**
     *
     * @param printJobUUID
     * @param printerToUse
     */
    public MovieMakerTask(String printJobUUID, Printer printerToUse)
    {
        this.printJobUUID = printJobUUID;
        this.printerToUse = printerToUse;
    }
    
    @Override
    protected MovieMakerResult call() throws Exception
    {
        boolean succeeded = false;
        keepRunning = true;
        
        String movieFile = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator + "movie.avi";
        String movieFile2 = "C:\\Users\\Ian\\Documents\\CEL Robox\\PrintJobs\\movie.avi";
        
        updateTitle("Movie Maker");
        updateMessage("Preparing to capture images");
        updateProgress(0, 100);
        
        frameGrabber = opencv_highgui.cvCaptureFromCAM(0);
        
        IplImage img = opencv_highgui.cvQueryFrame(frameGrabber);
        if (img != null)
        {
            CvSize frameSize = opencv_core.cvGetSize(img);
            videoWriter = opencv_highgui.cvCreateVideoWriter(movieFile2, CV_FOURCC((byte) 'M', (byte) 'J', (byte) 'P', (byte) 'G'), 1.0, frameSize);
            
            if (videoWriter != null)
            {
                printerToUse.getPrinterAncillarySystems().xStopSwitchProperty().addListener(triggerListener);
                
                succeeded = true;
                
                while (keepRunning)
                {
                    Thread.sleep(100);
                }
                
                opencv_highgui.cvReleaseVideoWriter(videoWriter);
                steno.info("Releasing video writer");
                opencv_highgui.cvReleaseCapture(frameGrabber);
                steno.info("Releasing video grabber");
            }
        }
        
        return new MovieMakerResult(succeeded);
    }

    /**
     *
     * @param message
     * @param workDone
     */
    protected void progressUpdate(String message, int workDone)
    {
        updateMessage(message);
        updateProgress(workDone, 100);
    }
    
    private void takeImage()
    {
        steno.info("Movie maker about to grab frame ");
//        IplImage img = opencv_highgui.cvQueryFrame(frameGrabber);
//        opencv_highgui.cvWriteFrame(videoWriter, img);
    }
    
    public void shutdown()
    {
        steno.info("Shutting down movie maker task");
        keepRunning = false;
    }
}
