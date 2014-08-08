/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.printing;

import celtech.appManager.Project;
import celtech.appManager.ProjectMode;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PauseStatus;
import celtech.coreUI.DisplayManager;
import celtech.gcodetranslator.PrintJobStatistics;
import celtech.printerControl.PrintJob;
import celtech.printerControl.Printer;
import celtech.printerControl.PrinterStatusEnumeration;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.ListFilesResponse;
import celtech.services.ControllableService;
import celtech.services.postProcessor.GCodePostProcessingResult;
import celtech.services.postProcessor.PostProcessorService;
import celtech.services.slicer.AbstractSlicerService;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import celtech.services.slicer.SliceResult;
import celtech.services.slicer.SlicerService;
import celtech.utils.PrinterUtils;
import celtech.utils.SystemUtils;
import celtech.utils.threed.ThreeDUtils;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author ianhudson
 */
public class PrintQueue implements ControllableService
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        PrintQueue.class.getName());

    private Printer associatedPrinter = null;
    private PrinterStatusEnumeration printState = PrinterStatusEnumeration.IDLE;
    private PrinterStatusEnumeration lastStateBeforePause = PrinterStatusEnumeration.IDLE;
    private AbstractSlicerService slicerService;
    private final PostProcessorService gcodePostProcessorService = new PostProcessorService();
    private final GCodePrintService gcodePrintService = new GCodePrintService();
    private final IntegerProperty linesInPrintingFile = new SimpleIntegerProperty(0);

    /**
     * Indicates if ETC data is available for the current print
     */
    private final BooleanProperty etcAvailable = new SimpleBooleanProperty(false);
    /*
     * 
     */
    private EventHandler<WorkerStateEvent> cancelSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> failedSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> succeededSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> cancelGCodePostProcessEventHandler = null;
    private EventHandler<WorkerStateEvent> failedGCodePostProcessEventHandler = null;
    private EventHandler<WorkerStateEvent> succeededGCodePostProcessEventHandler = null;
    private EventHandler<WorkerStateEvent> cancelPrintEventHandler = null;
    private EventHandler<WorkerStateEvent> failedPrintEventHandler = null;
    private EventHandler<WorkerStateEvent> succeededPrintEventHandler = null;

    private final StringProperty printProgressTitle = new SimpleStringProperty();
    private final StringProperty printProgressMessage = new SimpleStringProperty();
    private final BooleanProperty dialogRequired = new SimpleBooleanProperty(false);
    private final BooleanProperty printInProgress = new SimpleBooleanProperty(false);
    private final DoubleProperty primaryProgressPercent = new SimpleDoubleProperty(0);
    private final DoubleProperty secondaryProgressPercent = new SimpleDoubleProperty(0);
    private final BooleanProperty sendingDataToPrinter = new SimpleBooleanProperty(false);
    private final ObjectProperty<Date> printJobStartTime = new SimpleObjectProperty<>();
    /*
     * 
     */
    private ChangeListener<Number> printLineNumberListener = null;
    private ChangeListener<String> printJobIDListener = null;

    private ResourceBundle i18nBundle = null;
    private String printTransferInitiatedNotification = null;
    private String printTransferSuccessfulNotification = null;
    private String printTransferSuccessfulNotificationEnd = null;
    private String printJobCancelledNotification = null;
    private String printJobFailedNotification = null;
    private String sliceSuccessfulNotification = null;
    private String sliceFailedNotification = null;
    private String gcodePostProcessSuccessfulNotification = null;
    private String gcodePostProcessFailedNotification = null;
    private String detectedPrintInProgressNotification = null;
    private String notificationTitle = null;

    private final Map<String, Project> printJobsAgainstProjects = new HashMap<>();

    private boolean consideringPrintRequest = false;
    private final NotificationsHandler notificationsHandler;
    ETCCalculator etcCalculator;
    /**
     * progressETC holds the number of seconds predicted for the ETC of the print
     */
    private final IntegerProperty progressETC = new SimpleIntegerProperty();
    /**
     * The current layer being processed
     */
    private final IntegerProperty progressCurrentLayer = new SimpleIntegerProperty();
    /**
     * The total number of layers in the model being printed
     */
    private final IntegerProperty progressNumLayers = new SimpleIntegerProperty();
    private Set<PropertyChangeListener> propertyChangeListeners = new HashSet<>();

    public PrintQueue(Printer associatedPrinter)
    {
        this(associatedPrinter, new NotificationsHandlerImpl(),
             new SlicerService());
    }

    public PrintQueue(Printer associatedPrinter,
        NotificationsHandler notificationsHandler,
        AbstractSlicerService slicerService)
    {
        this.associatedPrinter = associatedPrinter;
        this.notificationsHandler = notificationsHandler;
        this.slicerService = slicerService;

        i18nBundle = DisplayManager.getLanguageBundle();
        printTransferInitiatedNotification = i18nBundle.getString(
            "notification.printTransferInitiated");
        printTransferSuccessfulNotification = i18nBundle.getString(
            "notification.printTransferredSuccessfully");
        printTransferSuccessfulNotificationEnd = i18nBundle.getString(
            "notification.printTransferredSuccessfullyEnd");
        printJobCancelledNotification = i18nBundle.getString(
            "notification.printJobCancelled");
        printJobFailedNotification = i18nBundle.getString(
            "notification.printJobFailed");
        sliceSuccessfulNotification = i18nBundle.getString(
            "notification.sliceSuccessful");
        sliceFailedNotification = i18nBundle.getString(
            "notification.sliceFailed");
        gcodePostProcessSuccessfulNotification = i18nBundle.getString(
            "notification.gcodePostProcessSuccessful");
        gcodePostProcessFailedNotification = i18nBundle.getString(
            "notification.gcodePostProcessFailed");
        notificationTitle = i18nBundle.getString("notification.PrintQueueTitle");
        detectedPrintInProgressNotification = i18nBundle.getString(
            "notification.activePrintDetected");

        cancelSliceEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                steno.info(t.getSource().getTitle() + " has been cancelled");
                abortPrint();
            }
        };

        failedSliceEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                steno.info(t.getSource().getTitle() + " has failed");
                PrintQueue.this.notificationsHandler.showErrorNotification(
                    notificationTitle, sliceFailedNotification);
                abortPrint();
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
                    gcodePostProcessorService.reset();
                    gcodePostProcessorService.setPrintJobUUID(
                        result.getPrintJobUUID());
                    gcodePostProcessorService.setSettings(result.getSettings());
                    gcodePostProcessorService.setPrinterToUse(
                        result.getPrinterToUse());
                    gcodePostProcessorService.start();

                    PrintQueue.this.notificationsHandler.showInformationNotification(
                        notificationTitle, sliceSuccessfulNotification);
                    setPrintStatus(PrinterStatusEnumeration.POST_PROCESSING);
                } else
                {
                    PrintQueue.this.notificationsHandler.showErrorNotification(
                        notificationTitle, sliceFailedNotification);
                    abortPrint();
                }
            }
        };

        cancelGCodePostProcessEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                steno.info(t.getSource().getTitle() + " has been cancelled");
                abortPrint();
            }
        };

        failedGCodePostProcessEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                steno.info(t.getSource().getTitle() + " has failed");
                PrintQueue.this.notificationsHandler.showErrorNotification(
                    notificationTitle, gcodePostProcessFailedNotification);
                abortPrint();
            }
        };

        succeededGCodePostProcessEventHandler = (WorkerStateEvent t) ->
        {
            GCodePostProcessingResult result = (GCodePostProcessingResult) (t.getSource().
                getValue());

            if (result.getRoboxiserResult().isSuccess())
            {
                steno.info(t.getSource().getTitle() + " has succeeded");
                String jobUUID = result.getPrintJobUUID();

                Project project = printJobsAgainstProjects.get(jobUUID);
                project.addPrintJobID(jobUUID);

                PrintJobStatistics printJobStatistics = result.getRoboxiserResult().getPrintJobStatistics();

                makeETCCalculator(printJobStatistics, associatedPrinter);

                gcodePrintService.reset();
                gcodePrintService.setCurrentPrintJobID(jobUUID);
                gcodePrintService.setModelFileToPrint(result.getOutputFilename());
                gcodePrintService.setPrinterToUse(result.getPrinterToUse());
                gcodePrintService.start();

                printJobStartTime.set(new Date());

                PrintQueue.this.notificationsHandler.showInformationNotification(
                    notificationTitle, gcodePostProcessSuccessfulNotification);
                setPrintStatus(PrinterStatusEnumeration.SENDING_TO_PRINTER);
            } else
            {
                PrintQueue.this.notificationsHandler.showErrorNotification(
                    notificationTitle, gcodePostProcessFailedNotification);
                abortPrint();
            }
        };

        cancelPrintEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                steno.info(t.getSource().getTitle() + " has been cancelled");
                PrintQueue.this.notificationsHandler.showInformationNotification(
                    notificationTitle, printJobCancelledNotification);
            }
        };

        failedPrintEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                steno.error(t.getSource().getTitle() + " has failed");
                PrintQueue.this.notificationsHandler.showErrorNotification(
                    notificationTitle, printJobFailedNotification);
                abortPrint();
            }
        };

        succeededPrintEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                GCodePrintResult result = (GCodePrintResult) (t.getSource().getValue());
                if (result.isSuccess())
                {
                    steno.info(t.getSource().getTitle() + " has succeeded");
                    if (result.isIsMacro())
                    {
                        setPrintStatus(PrinterStatusEnumeration.EXECUTING_MACRO);
                        //Remove the print job from disk
                        String printjobFilename = ApplicationConfiguration.
                            getPrintSpoolDirectory() + result.getPrintJobID();
                        File directoryToDelete = new File(printjobFilename);
                        try
                        {
                            FileDeleteStrategy.FORCE.delete(directoryToDelete);
                        } catch (IOException ex)
                        {
                            steno.error(
                                "Error whilst deleting macro print directory "
                                + printjobFilename + " exception - "
                                + ex.getMessage());
                        }
                    } else
                    {
                        PrintQueue.this.notificationsHandler.showInformationNotification(
                            notificationTitle,
                            printTransferSuccessfulNotification + " "
                            + associatedPrinter.getPrinterFriendlyName() + "\n"
                            + printTransferSuccessfulNotificationEnd);
                        setPrintStatus(PrinterStatusEnumeration.PRINTING);
                    }
                } else
                {
                    PrintQueue.this.notificationsHandler.showErrorNotification(
                        notificationTitle, printJobFailedNotification);
                    steno.error("Submission of job to printer failed");
                    abortPrint();
                }
                sendingDataToPrinter.set(false);
            }
        };

        printJobIDListener = new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> ov,
                String oldValue,
                String newValue)
            {
                steno.info("Print job ID number is " + newValue + " and was "
                    + oldValue);

                detectAlreadyPrinting();
            }
        };

        printLineNumberListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov,
                Number oldValue,
                Number newValue)
            {
//                steno.info("Line number is " + newValue.toString() + " and was " + oldValue.toString());
//                System.out.println("Line number changed to " + newValue);
                switch (printState)
                {
                    case IDLE:
//Ignore this state...
                        break;
                    case PRINTING:
                    case SENDING_TO_PRINTER:
                        if (etcAvailable.get())
                        {
                            updateETCUsingETCCalculator(newValue);
                        } else
                        {
                            updateETCUsingLineNumber(newValue);
                        }
                        break;
                    case EXECUTING_MACRO:
                        updateETCUsingLineNumber(newValue);
                        break;
                    default:
                        break;
                }
            }

            private void updateETCUsingLineNumber(Number newValue)
            {
                if (linesInPrintingFile.get() > 0)
                {
                    double percentDone = newValue.doubleValue()
                        / linesInPrintingFile.doubleValue();
                    primaryProgressPercent.set(percentDone);
                }
            }
        };

        slicerService.setOnCancelled(cancelSliceEventHandler);

        slicerService.setOnFailed(failedSliceEventHandler);

        slicerService.setOnSucceeded(succeededSliceEventHandler);

        gcodePostProcessorService.setOnCancelled(
            cancelGCodePostProcessEventHandler);

        gcodePostProcessorService.setOnFailed(failedGCodePostProcessEventHandler);

        gcodePostProcessorService.setOnSucceeded(
            succeededGCodePostProcessEventHandler);

        gcodePrintService.setOnCancelled(cancelPrintEventHandler);

        gcodePrintService.setOnFailed(failedPrintEventHandler);

        gcodePrintService.setOnSucceeded(succeededPrintEventHandler);

        setPrintStatus(PrinterStatusEnumeration.IDLE);

        associatedPrinter.printJobLineNumberProperty().addListener(printLineNumberListener);
        associatedPrinter.printJobIDProperty().addListener(printJobIDListener);
    }

    /**
     * Create the ETCCalculator based on the given PrintJobStatistics.
     */
    private void makeETCCalculator(PrintJobStatistics printJobStatistics,
        Printer associatedPrinter)
    {
        int numberOfLines = printJobStatistics.getNumberOfLines();
        linesInPrintingFile.set(numberOfLines);
        List<Double> layerNumberToPredictedDuration = printJobStatistics.getLayerNumberToPredictedDuration();
        List<Integer> layerNumberToLineNumber = printJobStatistics.getLayerNumberToLineNumber();
        etcCalculator = new ETCCalculator(associatedPrinter,
                                          layerNumberToPredictedDuration, layerNumberToLineNumber);

        progressNumLayers.set(layerNumberToLineNumber.size());
        primaryProgressPercent.unbind();
        primaryProgressPercent.set(0);
        progressETC.set(etcCalculator.getETCPredicted(0));
        etcAvailable.set(true);
    }

    private void updateETCUsingETCCalculator(Number newValue)
    {
        int lineNumber = newValue.intValue();
        primaryProgressPercent.set(etcCalculator.getPercentCompleteAtLine(lineNumber));
        progressETC.set(etcCalculator.getETCPredicted(lineNumber));
        progressCurrentLayer.set(etcCalculator.getCompletedLayerNumberForLineNumber(lineNumber));
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
                    makeETCCalculatorForJobOfUUID(printJobID);
                }
            }

            switch (printState)
            {
                case IDLE:
                    if (roboxIsPrinting)
                    {
                        makeETCCalculatorForJobOfUUID(printJobID);
                        this.notificationsHandler.showInformationNotification(
                            notificationTitle,
                            detectedPrintInProgressNotification);

                        if (associatedPrinter.pauseStatusProperty().get() == PauseStatus.PAUSED)
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
                case EXECUTING_MACRO:
                    if (roboxIsPrinting == false)
                    {
                        setPrintStatus(PrinterStatusEnumeration.IDLE);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void makeETCCalculatorForJobOfUUID(String printJobID)
    {
        PrintJob printJob = PrintJob.readJobFromDirectory(printJobID);
        try
        {
            makeETCCalculator(printJob.getStatistics(), associatedPrinter);
        } catch (IOException ex)
        {
            etcAvailable.set(false);
        }
    }

    /**
     *
     */
    public void shutdown()
    {
    }

    /*
     * Properties
     */
    /**
     *
     * @param project
     * @param printQuality
     * @param settings
     * @return
     */
    public synchronized boolean printProject(Project project, PrintQualityEnumeration printQuality, RoboxProfile settings)
    {
        boolean acceptedPrintRequest = false;
        etcAvailable.set(false);

        if (printState == PrinterStatusEnumeration.IDLE)
        {
            boolean printFromScratchRequired = false;

            if (project.getLastPrintJobID() != null)
            {
                String jobUUID = project.getLastPrintJobID();
                PrintJob printJob = PrintJob.readJobFromDirectory(jobUUID);

                //Reprint the last job
                //Is it still on the printer?
                try
                {
                    ListFilesResponse listFilesResponse = associatedPrinter.
                        transmitListFiles();
                    if (listFilesResponse.getPrintJobIDs().contains(jobUUID))
                    {
                        acceptedPrintRequest = reprintDirectFromPrinter(printJob);
                    } else
                    {
                        //Need to send the file to the printer
                        //Is it still on disk?

                        if (printJob.roboxisedFileExists())
                        {
                            acceptedPrintRequest = reprintFileFromDisk(printJob);
                        } else
                        {
                            printFromScratchRequired = true;
                            steno.error(
                                "Print job " + jobUUID
                                + " not found on printer or disk - going ahead with print from scratch");
                        }
                    }

                    try
                    {
                        makeETCCalculator(printJob.getStatistics(), associatedPrinter);
                    } catch (IOException ex)
                    {
                        etcAvailable.set(false);
                    }
                } catch (RoboxCommsException ex)
                {
                    printFromScratchRequired = true;
                    steno.error(
                        "Error whilst attempting to list files on printer - going ahead with print from scratch");
                }
            } else
            {
                printFromScratchRequired = true;
            }

            if (printFromScratchRequired)
            {
                acceptedPrintRequest = printFromScratch(printQuality, settings,
                                                        project,
                                                        acceptedPrintRequest);
            }
        }

        return acceptedPrintRequest;
    }

    private boolean printFromScratch(PrintQualityEnumeration printQuality,
        RoboxProfile settings, Project project, boolean acceptedPrintRequest)
    {
        //Create the print job directory
        String printUUID = SystemUtils.generate16DigitID();
        String printJobDirectoryName = ApplicationConfiguration.
            getPrintSpoolDirectory() + printUUID;
        File printJobDirectory = new File(printJobDirectoryName);
        printJobDirectory.mkdirs();
        //Erase old print job directories
        File printSpoolDirectory = new File(
            ApplicationConfiguration.getPrintSpoolDirectory());
        File[] filesOnDisk = printSpoolDirectory.listFiles();
        if (filesOnDisk.length > ApplicationConfiguration.maxPrintSpoolFiles)
        {
            int filesToDelete = filesOnDisk.length
                - ApplicationConfiguration.maxPrintSpoolFiles;
            Arrays.sort(filesOnDisk, (File f1, File f2) -> Long.valueOf(
                        f1.lastModified()).compareTo(f2.lastModified()));
            for (int i = 0; i < filesToDelete; i++)
            {
                try
                {
                    FileUtils.deleteDirectory(filesOnDisk[i]);
                } catch (IOException ex)
                {
                    steno.error(
                        "Error whilst deleting " + filesOnDisk[i].toString());
                }
            }
        }

        if (project.getProjectMode() == ProjectMode.MESH)
        {

            //Write out the slicer config
            settings.filament_diameterProperty().set(ApplicationConfiguration.filamentDiameterToYieldVolumetricExtrusion);

            //We need to tell Slic3r where the centre of the printed objects is - otherwise everything is put in the centre of the bed...
            Vector3D centreOfPrintedObject = ThreeDUtils.calculateCentre(project.getLoadedModels());
            settings.getPrint_center().set((centreOfPrintedObject.getX() + ApplicationConfiguration.xPrintOffset) + "," + (centreOfPrintedObject.getZ() + ApplicationConfiguration.yPrintOffset));
            settings.writeToFile(printJobDirectoryName + File.separator + printUUID + ApplicationConfiguration.printProfileFileExtension);

            setPrintStatus(PrinterStatusEnumeration.SLICING);
            slicerService.reset();
            slicerService.setProject(project);
            slicerService.setSettings(settings);
            slicerService.setPrintJobUUID(printUUID);
            slicerService.setPrinterToUse(associatedPrinter);
            slicerService.start();

            printJobsAgainstProjects.put(printUUID, project);

//            fxToJMEInterface.clearGCodeDisplay();
            // Do we need to slice?
            acceptedPrintRequest = true;
        } else if (project.getProjectMode() == ProjectMode.GCODE)
        {
            String printjobFilename = ApplicationConfiguration.
                getPrintSpoolDirectory() + printUUID + File.separator
                + printUUID + ApplicationConfiguration.gcodeTempFileExtension;
            String fileToCopyname = project.getGCodeFilename();
            File printjobFile = new File(printjobFilename);
            File fileToCopy = new File(fileToCopyname);
            try
            {
                Files.copy(fileToCopy.toPath(), printjobFile.toPath(),
                           StandardCopyOption.REPLACE_EXISTING);
                setPrintStatus(PrinterStatusEnumeration.SENDING_TO_PRINTER);

                gcodePrintService.reset();
                gcodePrintService.setCurrentPrintJobID(printUUID);
                gcodePrintService.setModelFileToPrint(printjobFilename);
                gcodePrintService.setPrinterToUse(associatedPrinter);
                gcodePrintService.start();
                acceptedPrintRequest = true;
            } catch (IOException ex)
            {
                steno.error(
                    "Error whilt preparing for print. Can't copy "
                    + fileToCopyname + " to " + printjobFilename);
            }
        }
        return acceptedPrintRequest;
    }

    private boolean reprintFileFromDisk(PrintJob printJob)
    {
        String gCodeFileName = printJob.getRoboxisedFileLocation();
        String jobUUID = printJob.getJobUUID();
        boolean acceptedPrintRequest;
        try
        {
            linesInPrintingFile.set(printJob.getStatistics().getNumberOfLines());
        } catch (IOException ex)
        {
            steno.error("Couldn't get job statistics for job " + jobUUID);
        }
        setPrintStatus(PrinterStatusEnumeration.SENDING_TO_PRINTER);
        steno.info("Respooling job " + jobUUID + " to printer");
        gcodePrintService.reset();
        gcodePrintService.setCurrentPrintJobID(jobUUID);
        gcodePrintService.setModelFileToPrint(gCodeFileName);
        gcodePrintService.setPrinterToUse(associatedPrinter);
        gcodePrintService.start();
        this.notificationsHandler.showInformationNotification(
            notificationTitle, printTransferInitiatedNotification);
        acceptedPrintRequest = true;
        return acceptedPrintRequest;
    }

    private boolean reprintDirectFromPrinter(PrintJob printJob) throws RoboxCommsException
    {
        boolean acceptedPrintRequest;
        //Reprint directly from printer
        steno.info("Printing job " + printJob.getJobUUID() + " from printer store");
        this.notificationsHandler.showInformationNotification(
            notificationTitle, i18nBundle.getString(
                "notification.reprintInitiated"));

        if (printJob.roboxisedFileExists())
        {
            try
            {
                linesInPrintingFile.set(printJob.getStatistics().getNumberOfLines());
            } catch (IOException ex)
            {
                steno.error("Couldn't get job statistics for job " + printJob.getJobUUID());
            }
        }
        associatedPrinter.initiatePrint(printJob.getJobUUID());
        setPrintStatus(PrinterStatusEnumeration.PRINTING);
        acceptedPrintRequest = true;
        return acceptedPrintRequest;
    }

    /**
     *
     * @return
     */
    public PrinterStatusEnumeration getPrintStatus()
    {
        return printState;
    }
    
    public void addPrintStatusListener(PropertyChangeListener listener) {
        propertyChangeListeners.add(listener);
    }

    private void setPrintStatus(PrinterStatusEnumeration newState)
    {
        switch (newState)
        {
            case IDLE:
                printProgressMessage.unbind();
                setPrintProgressMessage("");
                primaryProgressPercent.unbind();
                setPrimaryProgressPercent(0);
                secondaryProgressPercent.unbind();
                setSecondaryProgressPercent(0);
                sendingDataToPrinter.set(false);
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
                primaryProgressPercent.unbind();
                setPrimaryProgressPercent(0);
                primaryProgressPercent.bind(slicerService.progressProperty());
                secondaryProgressPercent.unbind();
                setSecondaryProgressPercent(0);
                sendingDataToPrinter.set(false);
                setPrintInProgress(true);
                setDialogRequired(true);
                break;
            case POST_PROCESSING:
                printProgressMessage.unbind();
                printProgressMessage.bind(
                    gcodePostProcessorService.messageProperty());
                primaryProgressPercent.unbind();
                setPrimaryProgressPercent(0);
                primaryProgressPercent.bind(
                    gcodePostProcessorService.progressProperty());
                secondaryProgressPercent.unbind();
                sendingDataToPrinter.set(false);
                setSecondaryProgressPercent(0);
                setPrintInProgress(true);
                setDialogRequired(true);
                break;
            case SENDING_TO_PRINTER:
                printProgressMessage.unbind();
                printProgressMessage.bind(gcodePrintService.messageProperty());
                primaryProgressPercent.unbind();
                setPrimaryProgressPercent(0);
                secondaryProgressPercent.unbind();
                setSecondaryProgressPercent(0);
                secondaryProgressPercent.bind(
                    gcodePrintService.progressProperty());
                sendingDataToPrinter.set(true);
                setPrintInProgress(true);
                setDialogRequired(true);
                break;
            case PAUSED:
                setPrintInProgress(true);
                setDialogRequired(false);
                break;
            case PRINTING:
            case EXECUTING_MACRO:
                printProgressMessage.unbind();
                primaryProgressPercent.unbind();
                if (printState != PrinterStatusEnumeration.PAUSED)
                {
                    setPrimaryProgressPercent(0);
                }
                printProgressMessage.set("");
                setPrintInProgress(true);
                setDialogRequired(false);
                break;
            default:
                break;
        }
        setPrintProgressTitle(newState.getDescription());
        PrinterStatusEnumeration oldState = printState;
        printState = newState;
        consideringPrintRequest = false;
        
        firePropertyChange("printStatus", oldState, printState);
    }

    private void firePropertyChange(String propertyName, Object oldState, Object newState)
    {
        for (PropertyChangeListener propertyChangeListener : propertyChangeListeners)
        {
            PropertyChangeEvent propertyChangeEvent = new PropertyChangeEvent(this, propertyName,
                oldState, newState);
            propertyChangeListener.propertyChange(propertyChangeEvent);
        }
    }

    private void setDialogRequired(boolean value)
    {
        dialogRequired.set(value);
    }

    private void setPrintInProgress(boolean value)
    {
        printInProgress.set(value);
    }

    /**
     *
     * @return
     */
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

    private void setPrimaryProgressPercent(double value)
    {
        primaryProgressPercent.set(value);
    }

    private void setSecondaryProgressPercent(double value)
    {
        secondaryProgressPercent.set(value);
    }

    /**
     *
     * @return
     */
    public ReadOnlyDoubleProperty secondaryProgressProperty()
    {
        return secondaryProgressPercent;
    }

    /**
     *
     * @return
     */
    public ReadOnlyBooleanProperty sendingDataToPrinterProperty()
    {
        return sendingDataToPrinter;
    }

    /**
     *
     * @return
     */
    @Override
    public ReadOnlyBooleanProperty runningProperty()
    {
        return dialogRequired;
    }

    /**
     *
     * @return
     */
    @Override
    public ReadOnlyStringProperty messageProperty()
    {
        return printProgressMessage;
    }

    /**
     *
     * @return
     */
    @Override
    public ReadOnlyDoubleProperty progressProperty()
    {
        return primaryProgressPercent;
    }

    /**
     *
     * @return
     */
    @Override
    public ReadOnlyStringProperty titleProperty()
    {
        return printProgressTitle;
    }

    /**
     *
     */
    public void pausePrint()
    {
        switch (printState)
        {
            case SENDING_TO_PRINTER:
            case PRINTING:
            case EXECUTING_MACRO:
                lastStateBeforePause = printState;
                try
                {
                    associatedPrinter.transmitPausePrint();
                    setPrintStatus(PrinterStatusEnumeration.PAUSED);
                } catch (RoboxCommsException ex)
                {
                    steno.error(
                        "Robox comms exception when sending pause print command "
                        + ex);
                }
                break;
            default:
                steno.warning("Attempt to pause print in print state "
                    + printState);
                break;
        }
    }

    /**
     *
     */
    public void resumePrint()
    {
        if (associatedPrinter.pauseStatusProperty().get() == PauseStatus.PAUSED
            || associatedPrinter.pauseStatusProperty().get() == PauseStatus.PAUSE_PENDING)
        {
            try
            {
                associatedPrinter.transmitResumePrint();

                setPrintStatus(lastStateBeforePause);
            } catch (RoboxCommsException ex)
            {
                steno.error(
                    "Robox comms exception when sending resume print command "
                    + ex);
            }
        }
    }

    /**
     *
     * @return
     */
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
            case POST_PROCESSING:
                if (gcodePostProcessorService.isRunning())
                {
                    gcodePostProcessorService.cancelRun();
                    setPrintStatus(PrinterStatusEnumeration.IDLE);
                    cancelledRun = true;
                }
                break;
            case PAUSED:
            case SENDING_TO_PRINTER:
            case PRINTING:
            case EXECUTING_MACRO:
                if (gcodePrintService.isRunning())
                {
                    gcodePrintService.cancelRun();
                }
                try
                {
                    associatedPrinter.transmitAbortPrint();
                    PrinterUtils.waitOnBusy(associatedPrinter, null);
                } catch (RoboxCommsException ex)
                {
                    steno.error(
                        "Robox comms exception when sending abort print command "
                        + ex);
                }
                setPrintStatus(PrinterStatusEnumeration.IDLE);
//                fxToJMEInterface.clearGCodeDisplay();
                cancelledRun = true;
                break;
            default:
                steno.warning("Attempt to abort print in print state "
                    + printState);
                break;
        }

        if (cancelledRun)
        {
            try
            {
                associatedPrinter.transmitStoredGCode("abort_print", false);
            } catch (RoboxCommsException ex)
            {
                steno.error(
                    "Robox comms exception when sending abort print gcode " + ex);
            }
        }
        return cancelledRun;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean cancelRun()
    {
        return false;
    }

    /**
     *
     */
    public void printerHasPaused()
    {
        switch (printState)
        {
            case IDLE:
            case PRINTING:
            case EXECUTING_MACRO:
            case SENDING_TO_PRINTER:
                lastStateBeforePause = printState;
                setPrintStatus(PrinterStatusEnumeration.PAUSED);
                break;
            default:
                break;
        }
    }

    /**
     *
     */
    public void printerHasResumed()
    {
        switch (printState)
        {
            case PAUSED:
                setPrintStatus(lastStateBeforePause);
                break;
            default:
                break;
        }
    }

    /**
     *
     * @return
     */
    public ReadOnlyIntegerProperty linesInPrintingFileProperty()
    {
        return linesInPrintingFile;
    }

    /**
     *
     * @param filename
     * @param useSDCard
     * @return
     */
    public boolean printGCodeFile(final String filename, final boolean useSDCard)
    {
        boolean acceptedPrintRequest = false;
        consideringPrintRequest = true;

        if (printState == PrinterStatusEnumeration.IDLE)
        {
            if (Platform.isFxApplicationThread() == false)
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        runMacroPrintJob(filename, useSDCard);
                    }
                });
            } else
            {
                runMacroPrintJob(filename, useSDCard);
            }
            acceptedPrintRequest = true;
        }

        return acceptedPrintRequest;
    }

    private void runMacroPrintJob(String filename, boolean useSDCard)
    {
        //Create the print job directory
        String printUUID = SystemUtils.generate16DigitID();

        String printJobDirectoryName = ApplicationConfiguration.getPrintSpoolDirectory()
            + printUUID;

        File printJobDirectory = new File(printJobDirectoryName);
        printJobDirectory.mkdirs();

        //Erase old print job directories
        File printSpoolDirectory = new File(
            ApplicationConfiguration.getPrintSpoolDirectory());
        File[] filesOnDisk = printSpoolDirectory.listFiles();
        if (filesOnDisk.length > ApplicationConfiguration.maxPrintSpoolFiles)
        {
            int filesToDelete = filesOnDisk.length
                - ApplicationConfiguration.maxPrintSpoolFiles;
            Arrays.sort(filesOnDisk,
                        (File f1, File f2) -> Long.valueOf(f1.lastModified()).compareTo(
                            f2.lastModified()));
            for (int i = 0; i < filesToDelete; i++)
            {
                try
                {
                    FileUtils.deleteDirectory(filesOnDisk[i]);
                } catch (IOException ex)
                {
                    steno.error("Error whilst deleting "
                        + filesOnDisk[i].toString());
                }
            }
        }

        String printjobFilename = ApplicationConfiguration.getPrintSpoolDirectory()
            + printUUID + File.separator + printUUID
            + ApplicationConfiguration.gcodeTempFileExtension;
        File printjobFile = new File(printjobFilename);
        File fileToCopy = new File(filename);

        setPrintStatus(PrinterStatusEnumeration.EXECUTING_MACRO);
        try
        {
            Files.copy(fileToCopy.toPath(), printjobFile.toPath(),
                       StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex)
        {
            steno.error(
                "Error whilst preparing for gcode print. Can't copy " + filename
                + " to " + printjobFilename);
        }

        int numberOfLines = SystemUtils.countLinesInFile(printjobFile, ";");
        linesInPrintingFile.set(numberOfLines);
        gcodePrintService.reset();
        gcodePrintService.setPrintUsingSDCard(useSDCard);
        gcodePrintService.setCurrentPrintJobID(printUUID);
        gcodePrintService.setModelFileToPrint(printjobFilename);
        gcodePrintService.setPrinterToUse(associatedPrinter);
        gcodePrintService.setIsMacro(true);
        gcodePrintService.start();
        consideringPrintRequest = false;

    }

    /**
     *
     * @return
     */
    public boolean isConsideringPrintRequest()
    {
        return consideringPrintRequest;
    }

    /**
     * @return the progressETC
     */
    public IntegerProperty progressETCProperty()
    {
        return progressETC;
    }

    public ReadOnlyBooleanProperty etcAvailableProperty()
    {
        return etcAvailable;
    }

    public ReadOnlyIntegerProperty progressCurrentLayerProperty()
    {
        return progressCurrentLayer;
    }

    public ReadOnlyIntegerProperty progressNumLayersProperty()
    {
        return progressNumLayers;
    }
}
