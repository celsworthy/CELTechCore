/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.printing;

import celtech.appManager.Notifier;
import celtech.appManager.Project;
import celtech.appManager.ProjectMode;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.printerControl.PrintJob;
import celtech.printerControl.Printer;
import celtech.printerControl.PrinterStatusEnumeration;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.services.ControllableService;
import celtech.services.modelLoader.ModelLoaderService;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SliceResult;
import celtech.services.slicer.SlicerService;
import celtech.services.slicer.SlicerSettings;
import celtech.utils.SystemUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class PrintQueue implements ControllableService
{

    private Stenographer steno = StenographerFactory.getStenographer(PrintQueue.class.getName());

    private Printer associatedPrinter = null;
    private PrinterStatusEnumeration printState = PrinterStatusEnumeration.IDLE;
    private PrintService printService = new PrintService();
    private SlicerService slicerService = new SlicerService();
    private GCodePrintService gcodePrintService = new GCodePrintService();
    private int linesInCurrentGCodeFile = 0;
    /*
     * 
     */
    private ExecutorService exec;
    private EventHandler<WorkerStateEvent> cancelSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> failedSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> succeededSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> cancelPrintEventHandler = null;
    private EventHandler<WorkerStateEvent> failedPrintEventHandler = null;
    private EventHandler<WorkerStateEvent> succeededPrintEventHandler = null;
    /*
     * 
     */
    private StringProperty printQueueStatusString = new SimpleStringProperty();
    private StringProperty printProgressTitle = new SimpleStringProperty();
    private StringProperty printProgressMessage = new SimpleStringProperty();
    private BooleanProperty dialogRequired = new SimpleBooleanProperty(false);
    private BooleanProperty printInProgress = new SimpleBooleanProperty(false);
    private DoubleProperty printProgressPercent = new SimpleDoubleProperty(0);
    /*
     * 
     */
    private ObservableList<Printer> printerStatusList = RoboxCommsManager.getInstance().getPrintStatusList();
    private ChangeListener<Number> printLineNumberListener = null;
    private ChangeListener<String> printJobIDListener = null;
    private ModelLoaderService modelLoaderService = new ModelLoaderService();
    private int numberOfLinesInGCode = 0;

    private ResourceBundle i18nBundle = null;
    private String printTransferSuccessfulNotification = null;
    private String printJobCancelledNotification = null;
    private String printJobCompletedNotification = null;
    private String printJobFailedNotification = null;
    private String sliceSuccessfulNotification = null;
    private String sliceFailedNotification = null;
    private String detectedPrintInProgressNotification = null;
    private String notificationTitle = null;

    public PrintQueue(Printer associatedPrinter)
    {
        this.associatedPrinter = associatedPrinter;
        exec = Executors.newFixedThreadPool(1);

        i18nBundle = DisplayManager.getLanguageBundle();
        printTransferSuccessfulNotification = i18nBundle.getString("notification.printTransferredSuccessfully");
        printJobCancelledNotification = i18nBundle.getString("notification.printJobCancelled");
        printJobCompletedNotification = i18nBundle.getString("notification.printJobCompleted");
        printJobFailedNotification = i18nBundle.getString("notification.printJobFailed");
        sliceSuccessfulNotification = i18nBundle.getString("notification.sliceSuccessful");
        sliceFailedNotification = i18nBundle.getString("notification.sliceFailed");
        notificationTitle = i18nBundle.getString("notification.PrintQueueTitle");
        detectedPrintInProgressNotification = i18nBundle.getString("notification.activePrintDetected");

        cancelSliceEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                steno.info(t.getSource().getTitle() + " has been cancelled");
            }
        };

        failedSliceEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                steno.info(t.getSource().getTitle() + " has failed");
                setPrintStatus(PrinterStatusEnumeration.IDLE);
                Notifier.showErrorNotification(notificationTitle, sliceFailedNotification);
            }
        };

        succeededSliceEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                SliceResult result = (SliceResult) (t.getSource().getValue());

                if (result.isSuccess())
                {
                    steno.info(t.getSource().getTitle() + " has succeeded");
                    String jobUUID = result.getPrintJobUUID();
                    String modelFileToPrint = ApplicationConfiguration.getPrintSpoolDirectory() + jobUUID + File.separator + jobUUID + ApplicationConfiguration.gcodeTempFileExtension;
                    modelLoaderService.reset();
                    modelLoaderService.setModelFileToLoad(modelFileToPrint);
                    modelLoaderService.start();
//                fxToJMEInterface.sendGCodeModel(null, null)
                    gcodePrintService.reset();
                    gcodePrintService.setCurrentPrintJobID(jobUUID);
                    gcodePrintService.setModelFileToPrint(modelFileToPrint);
                    gcodePrintService.setPrinterToUse(result.getPrinterToUse());
                    gcodePrintService.start();
                    File gcodeFromPrintJob = new File(ApplicationConfiguration.getPrintSpoolDirectory() + jobUUID + File.separator + jobUUID + ApplicationConfiguration.gcodeTempFileExtension);
                    int numberOfLines = SystemUtils.countLinesInFile(gcodeFromPrintJob);
                    linesInCurrentGCodeFile = numberOfLines;
                    Notifier.showInformationNotification(notificationTitle, sliceSuccessfulNotification);
                    setPrintStatus(PrinterStatusEnumeration.SENDING_TO_PRINTER);
                } else
                {
                    Notifier.showErrorNotification(notificationTitle, sliceFailedNotification);
                    setPrintStatus(PrinterStatusEnumeration.IDLE);
                }
            }
        };

        cancelPrintEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                steno.info(t.getSource().getTitle() + " has been cancelled");
                Notifier.showInformationNotification(notificationTitle, printJobCancelledNotification);
            }
        };

        failedPrintEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                steno.error(t.getSource().getTitle() + " has failed");
                Notifier.showErrorNotification(notificationTitle, printJobFailedNotification);
                setPrintStatus(PrinterStatusEnumeration.IDLE);
            }
        };

        succeededPrintEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                boolean succeeded = (boolean) (t.getSource().getValue());
                if (succeeded)
                {
                    steno.info(t.getSource().getTitle() + " has succeeded");
                    Notifier.showInformationNotification(notificationTitle, printTransferSuccessfulNotification + " " + associatedPrinter.getPrinterFriendlyName());
                    setPrintStatus(PrinterStatusEnumeration.PRINTING);
                } else
                {
                    Notifier.showErrorNotification(notificationTitle, printJobFailedNotification);
                    steno.error("Submission of job to printer failed");
                    abortPrint();
                }
            }
        };

        printJobIDListener = new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> ov, String oldValue, String newValue)
            {
//                steno.info("Print job ID number is " + newValue + " and was " + oldValue);

                detectAlreadyPrinting();
            }
        };

        printLineNumberListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue)
            {
//                steno.info("Line number is " + newValue.toString() + " and was " + oldValue.toString());
                switch (printState)
                {
                    case IDLE:
                    case SENDING_TO_PRINTER:
//Ignore this state...
                        break;
                    case PRINTING:
                        double percentDone = newValue.doubleValue() / (double) linesInCurrentGCodeFile;
                        printProgressPercent.set(percentDone);
//                        steno.info("Printing " + newValue.intValue() + " of " + linesInCurrentGCodeFile);
//                        printQueueStatusString.setValue(String.format("Printing %.1f%%", percentDone));
//                        fxToJMEInterface.exposeGCodeModel(percentDone);
                        break;
                    default:
                        break;
                }
            }
        };

        slicerService.setOnCancelled(cancelSliceEventHandler);

        slicerService.setOnFailed(failedSliceEventHandler);

        slicerService.setOnSucceeded(succeededSliceEventHandler);

        gcodePrintService.setOnCancelled(cancelPrintEventHandler);

        gcodePrintService.setOnFailed(failedPrintEventHandler);

        gcodePrintService.setOnSucceeded(succeededPrintEventHandler);

        setPrintStatus(PrinterStatusEnumeration.IDLE);

        associatedPrinter.printJobLineNumberProperty().addListener(printLineNumberListener);
        associatedPrinter.printJobIDProperty().addListener(printJobIDListener);
    }

    private void detectAlreadyPrinting()
    {
        boolean roboxIsPrinting = false;

        if (associatedPrinter != null)
        {
            String printJobID = associatedPrinter.printJobIDProperty().get();
            if (printJobID != null)
            {
                if (printJobID.codePointAt(0) != 0)
                {
                    roboxIsPrinting = true;
                }
            }

            switch (printState)
            {
                case IDLE:
                    if (roboxIsPrinting)
                    {
                        //We've detected a print job when we're idle...
                        //Try to find the print job and determine how many lines there were in it

                        File gcodeFromPrintJob = new File(ApplicationConfiguration.getPrintSpoolDirectory() + associatedPrinter.getPrintJobID() + File.separator + associatedPrinter.getPrintJobID() + ApplicationConfiguration.gcodeTempFileExtension);
                        int numberOfLines = SystemUtils.countLinesInFile(gcodeFromPrintJob);
                        linesInCurrentGCodeFile = numberOfLines;
                        double percentDone = (double) associatedPrinter.getPrintJobLineNumber() / numberOfLines;

                        printProgressPercent.set(percentDone);
//                            fxToJMEInterface.exposeGCodeModel(percentDone);
                        Notifier.showInformationNotification(notificationTitle, detectedPrintInProgressNotification);

                        if (associatedPrinter.getPaused() == true)
                        {
                            setPrintStatus(PrinterStatusEnumeration.PAUSED);
                        } else
                        {
                            setPrintStatus(PrinterStatusEnumeration.PRINTING);
                        }
                    }
                    break;
                case SENDING_TO_PRINTER:
                case PRINTING:
                    if (roboxIsPrinting == false)
                    {
//                            fxToJMEInterface.exposeGCodeModel(0);
                        setPrintStatus(PrinterStatusEnumeration.IDLE);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void shutdown()
    {
        exec.shutdown();
    }

    public void shutdownNow()
    {
        exec.shutdownNow();
    }

    /*
     * Properties
     */
    public synchronized boolean printProject(Project project, PrintQualityEnumeration printQuality, SlicerSettings settings)
    {
        boolean acceptedPrintRequest = false;

        if (printState == PrinterStatusEnumeration.IDLE)
        {
            //Create the print job directory
            String printUUID = SystemUtils.generate16DigitID();

            String printJobDirectoryName = ApplicationConfiguration.getPrintSpoolDirectory() + printUUID;

            //TODO PUT SOMETHING HERE TO GET RID OF OLD PRINT FILES
            File printJobDirectory = new File(printJobDirectoryName);
            printJobDirectory.mkdirs();

            PrintJob printJob = new PrintJob(printUUID, printQuality, settings);

            if (project.getProjectMode() == ProjectMode.MESH)
            {

                //Write out the slicer config
                settings.setStart_gcode(GCodeMacros.PRE_PRINT.getMacroContentsInOneLine());
                settings.setEnd_gcode(GCodeMacros.POST_PRINT.getMacroContentsInOneLine());
                settings.writeToFile(printJobDirectoryName + File.separator + printUUID + ApplicationConfiguration.printProfileFileExtension);

                setPrintStatus(PrinterStatusEnumeration.SLICING);
                slicerService.reset();
                slicerService.setProject(project);
                slicerService.setSettings(settings);
                slicerService.setPrintJobUUID(printUUID);
                slicerService.setPrinterToUse(associatedPrinter);
                slicerService.start();

//            fxToJMEInterface.clearGCodeDisplay();
                // Do we need to slice?
                acceptedPrintRequest = true;
            } else if (project.getProjectMode() == ProjectMode.GCODE)
            {
                String printjobFilename = ApplicationConfiguration.getPrintSpoolDirectory() + printUUID + File.separator + printUUID + ApplicationConfiguration.gcodeTempFileExtension;
                String fileToCopyname = project.getGCodeFilename();
                File printjobFile = new File(printjobFilename);
                File fileToCopy = new File(fileToCopyname);
                try
                {
                    Files.copy(fileToCopy.toPath(), printjobFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    modelLoaderService.reset();
                    modelLoaderService.setModelFileToLoad(printjobFilename);
                    modelLoaderService.start();
//                fxToJMEInterface.sendGCodeModel(null, null)
                    gcodePrintService.reset();
                    gcodePrintService.setCurrentPrintJobID(printUUID);
                    gcodePrintService.setModelFileToPrint(printjobFilename);
                    gcodePrintService.setPrinterToUse(associatedPrinter);
                    gcodePrintService.start();
                    setPrintStatus(PrinterStatusEnumeration.SENDING_TO_PRINTER);
                    acceptedPrintRequest = true;
                } catch (IOException ex)
                {
                    steno.error("Error whilt preparing for print. Can't copy " + fileToCopyname + " to " + printjobFilename);
                }
            }
        }

        return acceptedPrintRequest;
    }

    public PrinterStatusEnumeration getPrintStatus()
    {
        return printState;
    }

    private void setPrintStatus(PrinterStatusEnumeration newState)
    {
        switch (newState)
        {
            case IDLE:
                printProgressMessage.unbind();
                setPrintProgressMessage("");
                printProgressPercent.unbind();
                setPrintProgressPercent(0);
                setPrintInProgress(false);
                setDialogRequired(false);
                if (associatedPrinter != null)
                {
                    associatedPrinter.printJobIDProperty().unbind();
                    associatedPrinter.printJobLineNumberProperty().unbind();
                }
                break;
            case SLICING:
                printProgressMessage.unbind();
                printProgressMessage.bind(slicerService.messageProperty());
                printProgressPercent.unbind();
                setPrintProgressPercent(0);
                printProgressPercent.bind(slicerService.progressProperty());
                setPrintInProgress(true);
                setDialogRequired(true);
                break;
            case SENDING_TO_PRINTER:
                printProgressMessage.unbind();
                printProgressMessage.bind(gcodePrintService.messageProperty());
                printProgressPercent.unbind();
                setPrintProgressPercent(0);
                printProgressPercent.bind(gcodePrintService.progressProperty());
                setPrintInProgress(true);
                setDialogRequired(true);
                break;
            case PAUSED:
                printProgressMessage.unbind();
                printProgressPercent.unbind();
                printProgressMessage.set("");
                setPrintInProgress(true);
                setDialogRequired(false);
                break;
            case PRINTING:
                printProgressMessage.unbind();
                printProgressPercent.unbind();
                if (printState != PrinterStatusEnumeration.PAUSED)
                {
                    setPrintProgressPercent(0);
                }
                printProgressMessage.set("");
                setPrintInProgress(true);
                setDialogRequired(false);
                break;
            default:
                break;
        }
        setPrintProgressTitle(newState.getDescription());
        printState = newState;
    }

    private void setDialogRequired(boolean value)
    {
        dialogRequired.set(value);
    }

    private void setPrintInProgress(boolean value)
    {
        printInProgress.set(value);
    }

    public BooleanProperty printInProgressProperty()
    {
        return printInProgress;
    }

    private void setPrintProgressMessage(String value)
    {
        printProgressMessage.set(value);
    }

    private void setPrintProgressTitle(String value)
    {
        printProgressTitle.set(value);
    }

    private void setPrintProgressPercent(double value)
    {
        printProgressPercent.set(value);
    }

    @Override
    public ReadOnlyBooleanProperty runningProperty()
    {
        return dialogRequired;
    }

    @Override
    public ReadOnlyStringProperty messageProperty()
    {
        return printProgressMessage;
    }

    @Override
    public ReadOnlyDoubleProperty progressProperty()
    {
        return printProgressPercent;
    }

    @Override
    public ReadOnlyStringProperty titleProperty()
    {
        return printProgressTitle;
    }

    public void pausePrint()
    {
        switch (printState)
        {
            case SENDING_TO_PRINTER:
            case PRINTING:
                try
                {
                    associatedPrinter.transmitPausePrint();
                    setPrintStatus(PrinterStatusEnumeration.PAUSED);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Robox comms exception when sending pause print command " + ex);
                }
                break;
            default:
                steno.warning("Attempt to pause print in print state " + printState);
                break;
        }
    }

    public void resumePrint()
    {
        switch (printState)
        {
            case PAUSED:
                try
                {
                    associatedPrinter.transmitResumePrint();
                    setPrintStatus(PrinterStatusEnumeration.PRINTING);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Robox comms exception when sending resume print command " + ex);
                }
                break;
            default:
                steno.warning("Attempt to resume print in print state " + printState);
                break;
        }
    }

    public boolean abortPrint()
    {
        boolean cancelledRun = false;

        switch (printState)
        {
            case SLICING:
                if (slicerService.isRunning())
                {
                    slicerService.cancelRun();
                    setPrintStatus(PrinterStatusEnumeration.IDLE);
                    cancelledRun = true;
                }
                break;
            case PAUSED:
            case SENDING_TO_PRINTER:
            case PRINTING:
                if (gcodePrintService.isRunning())
                {
                    gcodePrintService.cancelRun();
                }
                try
                {
                    associatedPrinter.transmitAbortPrint();
                    String response = associatedPrinter.transmitStoredGCode(GCodeMacros.ABORT_PRINT, true);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Robox comms exception when sending abort print command " + ex);
                }
                setPrintStatus(PrinterStatusEnumeration.IDLE);
//                fxToJMEInterface.clearGCodeDisplay();
                cancelledRun = true;
                break;
            default:
                steno.warning("Attempt to abort print in print state " + printState);
                break;
        }

        return cancelledRun;
    }

    @Override
    public boolean cancelRun()
    {
        return false;
    }
}
