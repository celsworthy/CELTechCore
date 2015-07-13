package celtech.printerControl.model;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Macro;
import celtech.configuration.MaterialType;
import celtech.configuration.SlicerType;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.PrintJobStatistics;
import celtech.printerControl.PrintJob;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.ListFilesResponse;
import celtech.services.ControllableService;
import celtech.services.postProcessor.GCodePostProcessingResult;
import celtech.services.postProcessor.PostProcessorService;
import celtech.services.printing.GCodePrintResult;
import celtech.services.printing.TransferGCodeToPrinterService;
import celtech.services.roboxmoviemaker.MovieMakerTask;
import celtech.services.slicer.AbstractSlicerService;
import celtech.services.slicer.SliceResult;
import celtech.configuration.slicer.SlicerConfigWriter;
import celtech.configuration.slicer.SlicerConfigWriterFactory;
import celtech.printerControl.PrintQueueStatus;
import celtech.printerControl.comms.commands.MacroLoadException;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.comms.commands.MacroPrintException;
import celtech.printerControl.model.Head.HeadType;
import celtech.printerControl.comms.commands.rx.SendFile;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SlicerService;
import celtech.utils.SystemUtils;
import celtech.utils.threed.ThreeDUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author ianhudson
 */
public class PrintEngine implements ControllableService
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            PrintEngine.class.getName());

    private Printer associatedPrinter = null;
    public final AbstractSlicerService slicerService = new SlicerService();
    public final PostProcessorService postProcessorService = new PostProcessorService();
    public final TransferGCodeToPrinterService transferGCodeToPrinterService = new TransferGCodeToPrinterService();
    private final IntegerProperty linesInPrintingFile = new SimpleIntegerProperty(0);

    /**
     * Indicates if ETC data is available for the current print
     */
    private final BooleanProperty etcAvailable = new SimpleBooleanProperty(false);
    /*
     * 
     */
    private EventHandler<WorkerStateEvent> scheduledSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> cancelSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> failedSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> succeededSliceEventHandler = null;

    private EventHandler<WorkerStateEvent> scheduledGCodePostProcessEventHandler = null;
    private EventHandler<WorkerStateEvent> cancelGCodePostProcessEventHandler = null;
    private EventHandler<WorkerStateEvent> failedGCodePostProcessEventHandler = null;
    private EventHandler<WorkerStateEvent> succeededGCodePostProcessEventHandler = null;

    private EventHandler<WorkerStateEvent> scheduledPrintEventHandler = null;
    private EventHandler<WorkerStateEvent> cancelPrintEventHandler = null;
    private EventHandler<WorkerStateEvent> failedPrintEventHandler = null;
    private EventHandler<WorkerStateEvent> succeededPrintEventHandler = null;

    private final StringProperty printProgressTitle = new SimpleStringProperty();
    private final StringProperty printProgressMessage = new SimpleStringProperty();
    private final BooleanProperty dialogRequired = new SimpleBooleanProperty(
            false);
    private final DoubleProperty primaryProgressPercent = new SimpleDoubleProperty(
            0);
    private final DoubleProperty secondaryProgressPercent = new SimpleDoubleProperty(
            0);
    private final ObjectProperty<Date> printJobStartTime = new SimpleObjectProperty<>();
    public final ObjectProperty<Macro> macroBeingRun = new SimpleObjectProperty<>();

    private ObjectProperty<PrintQueueStatus> printQueueStatus = new SimpleObjectProperty<>(PrintQueueStatus.IDLE);

    /*
     * 
     */
    private ChangeListener<Number> printLineNumberListener = null;
    private ChangeListener<String> printJobIDListener = null;

    private final Map<String, Project> printJobsAgainstProjects = new HashMap<>();

    private boolean consideringPrintRequest = false;
    ETCCalculator etcCalculator;
    /**
     * progressETC holds the number of seconds predicted for the ETC of the
     * print
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

    /**
     * The movie maker task
     */
    private MovieMakerTask movieMakerTask = null;

    private boolean raiseProgressNotifications = true;

    public PrintEngine(Printer associatedPrinter)
    {
        this.associatedPrinter = associatedPrinter;

        cancelSliceEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has been cancelled");
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on slice cancel");
            }
        };

        failedSliceEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has failed");
            if (raiseProgressNotifications)
            {
                Lookup.getSystemNotificationHandler().showSliceFailedNotification();
            }
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on slice fail");
            }
        };

        succeededSliceEventHandler = (WorkerStateEvent t) ->
        {
            SliceResult result = (SliceResult) (t.getSource().getValue());

            if (result.isSuccess())
            {
                steno.info(t.getSource().getTitle() + " has succeeded");
                postProcessorService.reset();
                postProcessorService.setPrintJobUUID(
                        result.getPrintJobUUID());
                postProcessorService.setPrinterToUse(
                        result.getPrinterToUse());
                postProcessorService.setProject(result.getProject());
                postProcessorService.start();

                if (raiseProgressNotifications)
                {
                    Lookup.getSystemNotificationHandler().showSliceSuccessfulNotification();
                }
            } else
            {
                if (raiseProgressNotifications)
                {
                    Lookup.getSystemNotificationHandler().showSliceFailedNotification();
                }
                try
                {
                    associatedPrinter.cancel(null);
                } catch (PrinterException ex)
                {
                    steno.error("Couldn't abort on slice fail");
                }
            }
        };

        cancelGCodePostProcessEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has been cancelled");
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on post process cancel");
            }
        };

        failedGCodePostProcessEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has failed");
            if (raiseProgressNotifications)
            {
                Lookup.getSystemNotificationHandler().showGCodePostProcessFailedNotification();
            }
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on post process fail");
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
                project.setLastPrintJobID(jobUUID);

                PrintJobStatistics printJobStatistics = result.getRoboxiserResult().
                        getPrintJobStatistics();

                makeETCCalculator(printJobStatistics, associatedPrinter);

                transferGCodeToPrinterService.reset();
                transferGCodeToPrinterService.setCurrentPrintJobID(jobUUID);
                transferGCodeToPrinterService.setStartFromSequenceNumber(0);
                transferGCodeToPrinterService.setModelFileToPrint(result.getOutputFilename());
                transferGCodeToPrinterService.setPrinterToUse(result.getPrinterToUse());
                transferGCodeToPrinterService.start();

                printJobStartTime.set(new Date());

                if (raiseProgressNotifications)
                {
                    Lookup.getSystemNotificationHandler().
                            showGCodePostProcessSuccessfulNotification();
                }
            } else
            {
                if (raiseProgressNotifications)
                {
                    Lookup.getSystemNotificationHandler().showGCodePostProcessFailedNotification();
                }
                try
                {
                    associatedPrinter.cancel(null);
                } catch (PrinterException ex)
                {
                    steno.error("Couldn't abort on post process fail");
                }

            }
        };

        cancelPrintEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has been cancelled");
            if (raiseProgressNotifications)
            {
                Lookup.getSystemNotificationHandler().showPrintJobCancelledNotification();
            }
        };

        failedPrintEventHandler = (WorkerStateEvent t) ->
        {
            steno.error(t.getSource().getTitle() + " has failed");
            if (raiseProgressNotifications)
            {
                Lookup.getSystemNotificationHandler().showPrintJobFailedNotification();
            }
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on print job fail");
            }
        };

        succeededPrintEventHandler = (WorkerStateEvent t) ->
        {
            GCodePrintResult result = (GCodePrintResult) (t.getSource().getValue());
            if (result.isSuccess())
            {
                steno.info(t.getSource().getTitle() + " has succeeded");
//                if (associatedPrinter.printerStatusProperty().get()
//                    == PrinterStatus.EJECTING_STUCK_MATERIAL)
//                {
////                    associatedPrinter.setPrinterStatus(PrinterStatus.EXECUTING_MACRO);
//                    //Remove the print job from disk
//                    String printjobFilename = ApplicationConfiguration.
//                        getApplicationStorageDirectory()
//                        + ApplicationConfiguration.macroFileSubpath
//                        + File.separator
//                        + result.getPrintJobID();
//                    File fileToDelete = new File(printjobFilename);
//                    try
//                    {
//                        FileDeleteStrategy.FORCE.delete(fileToDelete);
//                    } catch (IOException ex)
//                    {
//                        steno.error(
//                            "Error whilst deleting macro print directory "
//                            + printjobFilename + " exception - "
//                            + ex.getMessage());
//                    }
//                } else
                {
                    if (raiseProgressNotifications)
                    {
                        Lookup.getSystemNotificationHandler().
                                showPrintTransferSuccessfulNotification(
                                        associatedPrinter.getPrinterIdentity().printerFriendlyNameProperty().
                                        get());
                    }
                }
            } else
            {
                if (raiseProgressNotifications)
                {
                    Lookup.getSystemNotificationHandler().showPrintTransferFailedNotification(
                            associatedPrinter.getPrinterIdentity().printerFriendlyNameProperty().get());
                }
                steno.error("Submission of job to printer failed");
                try
                {
                    //TODO - can't submit in this case...?
                    associatedPrinter.cancel(null);
                } catch (PrinterException ex)
                {
                    steno.error("Couldn't abort on print job failed to submit");
                }
            }
        };

        printJobIDListener = (ObservableValue<? extends String> ov, String oldValue, String newValue) ->
        {
            detectAlreadyPrinting();
        };

        scheduledPrintEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has been scheduled");
            if (raiseProgressNotifications)
            {
                Lookup.getSystemNotificationHandler().showPrintTransferInitiatedNotification();
            }
        };

        printLineNumberListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov,
                    Number oldValue,
                    Number newValue)
            {
                if (etcAvailable.get())
                {
                    updateETCUsingETCCalculator(newValue);
                } else
                {
                    updateETCUsingLineNumber(newValue);
                }
            }
        };

        slicerService.setOnScheduled(scheduledSliceEventHandler);
        slicerService.setOnCancelled(cancelSliceEventHandler);

        slicerService.setOnFailed(failedSliceEventHandler);

        slicerService.setOnSucceeded(succeededSliceEventHandler);

        postProcessorService.setOnCancelled(
                cancelGCodePostProcessEventHandler);

        postProcessorService.setOnFailed(failedGCodePostProcessEventHandler);

        postProcessorService.setOnSucceeded(
                succeededGCodePostProcessEventHandler);

        transferGCodeToPrinterService.setOnScheduled(scheduledPrintEventHandler);

        transferGCodeToPrinterService.setOnCancelled(cancelPrintEventHandler);

        transferGCodeToPrinterService.setOnFailed(failedPrintEventHandler);

        transferGCodeToPrinterService.setOnSucceeded(succeededPrintEventHandler);

        associatedPrinter.printJobLineNumberProperty().addListener(printLineNumberListener);
        associatedPrinter.printJobIDProperty().addListener(printJobIDListener);

        detectAlreadyPrinting();
    }

    /**
     * Create the ETCCalculator based on the given PrintJobStatistics.
     */
    private void makeETCCalculator(PrintJobStatistics printJobStatistics,
            Printer associatedPrinter)
    {
        int numberOfLines = printJobStatistics.getNumberOfLines();
        linesInPrintingFile.set(numberOfLines);
        List<Double> layerNumberToPredictedDuration = printJobStatistics.
                getLayerNumberToPredictedDuration();
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

    private void updateETCUsingLineNumber(Number newValue)
    {
        if (linesInPrintingFile.get() > 0)
        {
            double percentDone = newValue.doubleValue()
                    / linesInPrintingFile.doubleValue();
            primaryProgressPercent.set(percentDone);
        }
    }

    public void makeETCCalculatorForJobOfUUID(String printJobID)
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
        stopAllServices();
    }

    public synchronized boolean printProject(Project project)
    {
        boolean acceptedPrintRequest = false;
        etcAvailable.set(false);

        if (associatedPrinter.printerStatusProperty().get() == PrinterStatus.IDLE)
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
                acceptedPrintRequest = printFromScratch(project,
                        acceptedPrintRequest);
            }

            movieMakerTask = new MovieMakerTask(project.getProjectName(), associatedPrinter);
            movieMakerTask.setOnSucceeded(new EventHandler<WorkerStateEvent>()
            {

                @Override
                public void handle(WorkerStateEvent event)
                {
                    steno.info("Movie maker succeeded");
                }
            });
            movieMakerTask.setOnFailed(new EventHandler<WorkerStateEvent>()
            {

                @Override
                public void handle(WorkerStateEvent event)
                {
                    steno.info("Movie maker failed");
                }
            });
            movieMakerTask.setOnCancelled(new EventHandler<WorkerStateEvent>()
            {

                @Override
                public void handle(WorkerStateEvent event)
                {
                    steno.info("Movie maker was cancelled");
                }
            });

            Thread movieThread = new Thread(movieMakerTask);
            movieThread.setName("Movie Maker - " + project.getProjectName());
            movieThread.setDaemon(true);
//            movieThread.start();
        }

        return acceptedPrintRequest;
    }

    private boolean printFromScratch(Project project, boolean acceptedPrintRequest)
    {
        Head currentHead = associatedPrinter.headProperty().get();
        HeadType headType = currentHead.headTypeProperty().get();
        SlicerParametersFile settingsToUse = project.getPrinterSettings().getSettings(headType).clone();

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
                FileUtils.deleteQuietly(filesOnDisk[i]);
            }
        }

        //Write out the slicer config
        SlicerType slicerTypeToUse = null;
        if (settingsToUse.getSlicerOverride() != null)
        {
            slicerTypeToUse = settingsToUse.getSlicerOverride();
        } else
        {
            slicerTypeToUse = Lookup.getUserPreferences().getSlicerType();
        }

        SlicerConfigWriter configWriter = SlicerConfigWriterFactory.getConfigWriter(
                slicerTypeToUse);

        //TODO DMH and/or material-dependent profiles
        // This is a hack to force the fan speed to 100% when using PLA - it only looks at the first reel...
        if (associatedPrinter.reelsProperty().containsKey(0))
        {
            if (associatedPrinter.reelsProperty().get(0).material.get() == MaterialType.PLA
                    && SlicerParametersContainer.applicationProfileListContainsProfile(settingsToUse.
                            getProfileName()))
            {
                settingsToUse.setEnableCooling(true);
                settingsToUse.setMinFanSpeed_percent(100);
                settingsToUse.setMaxFanSpeed_percent(100);
            }
        }
        // End of hack

        // Hack to change raft related settings for Draft ABS prints
        if (project.getPrintQuality() == PrintQualityEnumeration.DRAFT
                && project.getPrinterSettings().getFilament0().getMaterial() == MaterialType.ABS)
        {
            settingsToUse.setRaftBaseLinewidth_mm(1.250f);
            settingsToUse.setRaftAirGapLayer0_mm(0.285f);
            settingsToUse.setInterfaceLayers(1);
        }
        // End of hack

        Vector3D centreOfPrintedObject = ThreeDUtils.calculateCentre(project.getLoadedModels());
        configWriter.setPrintCentre((float) (centreOfPrintedObject.getX()),
                (float) (centreOfPrintedObject.getZ()));
        configWriter.generateConfigForSlicer(settingsToUse,
                printJobDirectoryName
                + File.separator
                + printUUID
                + ApplicationConfiguration.printProfileFileExtension);

        slicerService.reset();
        slicerService.setProject(project);
        slicerService.setSettings(settingsToUse);
        slicerService.setPrintJobUUID(printUUID);
        slicerService.setPrinterToUse(associatedPrinter);
        slicerService.start();

        printJobsAgainstProjects.put(printUUID, project);

        // Do we need to slice?
        acceptedPrintRequest = true;

        return acceptedPrintRequest;
    }

    private boolean reprintFileFromDisk(PrintJob printJob, int startFromLineNumber)
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
        steno.info("Respooling job " + jobUUID + " to printer from line " + startFromLineNumber);
        transferGCodeToPrinterService.reset();
        transferGCodeToPrinterService.setCurrentPrintJobID(jobUUID);
        transferGCodeToPrinterService.setStartFromSequenceNumber(startFromLineNumber);
        transferGCodeToPrinterService.setModelFileToPrint(gCodeFileName);
        transferGCodeToPrinterService.setPrinterToUse(associatedPrinter);
        transferGCodeToPrinterService.start();
        acceptedPrintRequest = true;
        return acceptedPrintRequest;
    }

    private boolean reprintFileFromDisk(PrintJob printJob)
    {
        return reprintFileFromDisk(printJob, 0);
    }

    private boolean reprintDirectFromPrinter(PrintJob printJob) throws RoboxCommsException
    {
        boolean acceptedPrintRequest;
        //Reprint directly from printer
        steno.info("Printing job " + printJob.getJobUUID() + " from printer store");
        if (raiseProgressNotifications)
        {
            Lookup.getSystemNotificationHandler().showReprintStartedNotification();
        }

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
        acceptedPrintRequest = true;
        return acceptedPrintRequest;
    }

    /**
     *
     * @return
     */
    public ReadOnlyDoubleProperty secondaryProgressProperty()
    {
        return secondaryProgressPercent;
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
        return primaryProgressPercent;
    }

    @Override
    public ReadOnlyStringProperty titleProperty()
    {
        return printProgressTitle;
    }

    @Override
    public boolean cancelRun()
    {
        return false;
    }

    public ReadOnlyIntegerProperty linesInPrintingFileProperty()
    {
        return linesInPrintingFile;
    }

    protected boolean printGCodeFile(final String filename, final boolean useSDCard) throws MacroPrintException
    {
        return printGCodeFile(filename, useSDCard, false);
    }

    protected boolean printGCodeFile(final String filename, final boolean useSDCard,
            final boolean dontInitiatePrint) throws MacroPrintException
    {
        boolean acceptedPrintRequest = false;
        consideringPrintRequest = true;

        //Create the print job directory
        String printUUID = createPrintJobDirectory();

        tidyPrintSpoolDirectory();

        String printjobFilename = ApplicationConfiguration.getPrintSpoolDirectory()
                + printUUID + File.separator + printUUID
                + ApplicationConfiguration.gcodeTempFileExtension;

        File src = new File(filename);
        File dest = new File(printjobFilename);
        try
        {
            FileUtils.copyFile(src, dest);
        } catch (IOException ex)
        {
            steno.error("Error copying file");
        }

        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            int numberOfLines = SystemUtils.countLinesInFile(dest, ";");
            raiseProgressNotifications = true;
            linesInPrintingFile.set(numberOfLines);
            transferGCodeToPrinterService.reset();
            transferGCodeToPrinterService.setPrintUsingSDCard(useSDCard);
            transferGCodeToPrinterService.setCurrentPrintJobID(printUUID);
            transferGCodeToPrinterService.setModelFileToPrint(printjobFilename);
            transferGCodeToPrinterService.setPrinterToUse(associatedPrinter);
            transferGCodeToPrinterService.dontInitiatePrint(dontInitiatePrint);
            transferGCodeToPrinterService.start();
            consideringPrintRequest = false;
        });

        acceptedPrintRequest = true;

        return acceptedPrintRequest;
    }

    private void tidyPrintSpoolDirectory()
    {
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
    }

    private void tidyMacroSpoolDirectory()
    {
        //Erase old print job directories
        File printSpoolDirectory = new File(
                ApplicationConfiguration.getApplicationStorageDirectory()
                + ApplicationConfiguration.macroFileSubpath);
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
                FileUtils.deleteQuietly(filesOnDisk[i]);
            }
        }
    }

    protected boolean runMacroPrintJob(Macro macro) throws MacroPrintException
    {
        return runMacroPrintJob(macro, true);
    }

    protected boolean runMacroPrintJob(Macro macro, boolean useSDCard) throws MacroPrintException
    {
        macroBeingRun.set(macro);

        boolean acceptedPrintRequest = false;
        consideringPrintRequest = true;

        //Create the print job directory
        String printUUID = macro.getMacroJobNumber();
        String printJobDirectoryName = ApplicationConfiguration.getApplicationStorageDirectory()
                + ApplicationConfiguration.macroFileSubpath;
        File printJobDirectory = new File(printJobDirectoryName);
        printJobDirectory.mkdirs();

        tidyMacroSpoolDirectory();

        String printjobFilename = printJobDirectoryName + printUUID
                + ApplicationConfiguration.gcodeTempFileExtension;

        File printjobFile = new File(printjobFilename);

        try
        {
            ArrayList<String> macroContents = GCodeMacros.getMacroContents(macro.getMacroFileName());
            // Write the contents of the macro file to the print area
            FileUtils.writeLines(printjobFile, macroContents, false);
        } catch (IOException ex)
        {
            throw new MacroPrintException("Error writing macro print job file: "
                    + printjobFilename + " : "
                    + ex.getMessage());
        } catch (MacroLoadException ex)
        {
            throw new MacroPrintException("Error whilst generating macro - " + ex.getMessage());
        }

        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            int numberOfLines = SystemUtils.countLinesInFile(printjobFile, ";");
            raiseProgressNotifications = false;
            linesInPrintingFile.set(numberOfLines);
            steno.
                    info("Print service is in state:" + transferGCodeToPrinterService.stateProperty().
                            get().name());
            if (transferGCodeToPrinterService.isRunning())
            {
                transferGCodeToPrinterService.cancel();
            }
            transferGCodeToPrinterService.reset();
            transferGCodeToPrinterService.setPrintUsingSDCard(useSDCard);
            transferGCodeToPrinterService.setStartFromSequenceNumber(0);
            transferGCodeToPrinterService.setCurrentPrintJobID(printUUID);
            transferGCodeToPrinterService.setModelFileToPrint(printjobFilename);
            transferGCodeToPrinterService.setPrinterToUse(associatedPrinter);
            transferGCodeToPrinterService.setThisCanBeReprinted(false);
            transferGCodeToPrinterService.start();
            consideringPrintRequest = false;
        });

        acceptedPrintRequest = true;

        return acceptedPrintRequest;
    }

    private String createPrintJobDirectory()
    {
        //Create the print job directory
        String printUUID = SystemUtils.generate16DigitID();
        String printJobDirectoryName = ApplicationConfiguration.getPrintSpoolDirectory()
                + printUUID;
        File printJobDirectory = new File(printJobDirectoryName);
        printJobDirectory.mkdirs();
        return printUUID;
    }

    public boolean isConsideringPrintRequest()
    {
        return consideringPrintRequest;
    }

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

    /**
     * Stop all services, in the GUI thread. Block current thread until the
     * routine has completed.
     */
    protected void stopAllServices()
    {

        Callable<Boolean> stopServices = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                steno.info("Shutdown print services...");
                if (slicerService.isRunning())
                {
                    steno.info("Shutdown slicer service...");
                    slicerService.cancelRun();
                }
                if (postProcessorService.isRunning())
                {
                    steno.info("Shutdown PP...");
                    postProcessorService.cancelRun();
                }
                if (transferGCodeToPrinterService.isRunning())
                {
                    steno.info("Shutdown print service...");
                    transferGCodeToPrinterService.cancelRun();
                }
                if (movieMakerTask.isRunning())
                {
                    steno.info("Shutdown move maker");
                    movieMakerTask.shutdown();
                }
                steno.info("Shutdown print services complete");
                return true;
            }
        };
        FutureTask<Boolean> stopServicesTask = new FutureTask<>(stopServices);
        Lookup.getTaskExecutor().runOnGUIThread(stopServicesTask);
        try
        {
            stopServicesTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error while stopping services: " + ex);
        }
    }

    public boolean reEstablishTransfer(String printJobID, int expectedSequenceNumber)
    {
        PrintJob printJob = PrintJob.readJobFromDirectory(printJobID);
        boolean acceptedPrintRequest = false;

        if (printJob.roboxisedFileExists())
        {
            acceptedPrintRequest = reprintFileFromDisk(printJob, expectedSequenceNumber);
            if (raiseProgressNotifications)
            {
                Lookup.getSystemNotificationHandler().removePrintTransferFailedNotification();
            }
        }

        return acceptedPrintRequest;
    }

    private void detectAlreadyPrinting()
    {
        boolean roboxIsPrinting = false;

        if (associatedPrinter != null)
        {
            String printJobID = associatedPrinter.printJobIDProperty().get();
            if (printJobID != null)
            {
                if (!printJobID.equals("")
                        && printJobID.codePointAt(0) != 0)
                {
                    roboxIsPrinting = true;
                }
            }

            if (roboxIsPrinting)
            {
                boolean incompleteTransfer = false;

                if (!transferGCodeToPrinterService.isRunning())
                {
                    try
                    {
                        SendFile sendFileData = (SendFile) associatedPrinter.requestSendFileReport();

                        if (sendFileData.getFileID() != null && !sendFileData.getFileID().equals(""))
                        {
                            steno.info("The printer is printing an incomplete job: File ID: "
                                    + sendFileData.getFileID()
                                    + " Expected sequence number: " + sendFileData.getExpectedSequenceNumber());

                            reEstablishTransfer(sendFileData.getFileID(),
                                    sendFileData.
                                    getExpectedSequenceNumber());

                            printQueueStatus.set(PrintQueueStatus.PRINTING);
                            setParentPrintStatusIfIdle(PrinterStatus.PRINTING_PROJECT);

                            incompleteTransfer = true;
                        }
                    } catch (RoboxCommsException ex)
                    {
                        steno.error(
                                "Error determining whether the printer has a partially transferred job in progress");
                    }
                }

                if (!incompleteTransfer)
                {
                    Optional<Macro> macroRunning = Macro.getMacroForPrintJobID(printJobID);

                    if (macroRunning.isPresent())
                    {
                        steno.debug("Printer "
                                + associatedPrinter.getPrinterIdentity().printerFriendlyName.get()
                                + " is running macro " + macroRunning.get().name());

                        macroBeingRun.set(macroRunning.get());
                        printQueueStatus.set(PrintQueueStatus.RUNNING_MACRO);
                        setParentPrintStatusIfIdle(PrinterStatus.RUNNING_MACRO_FILE);
                    } else
                    {
                        makeETCCalculatorForJobOfUUID(printJobID);

                        if (etcAvailable.get())
                        {
                            updateETCUsingETCCalculator(associatedPrinter.printJobLineNumberProperty().get());
                        } else
                        {
                            updateETCUsingLineNumber(associatedPrinter.printJobLineNumberProperty().get());
                        }

                        if (raiseProgressNotifications
                                && associatedPrinter.printerStatusProperty().get() != PrinterStatus.PRINTING_PROJECT)
                        {
                            Lookup.getSystemNotificationHandler().
                                    showDetectedPrintInProgressNotification();
                        }
                        steno.debug("Printer "
                                + associatedPrinter.getPrinterIdentity().printerFriendlyName.get()
                                + " is printing");

                        printQueueStatus.set(PrintQueueStatus.PRINTING);
                        setParentPrintStatusIfIdle(PrinterStatus.PRINTING_PROJECT);
                    }
                }
            } else
            {
                printQueueStatus.set(PrintQueueStatus.IDLE);
                switch (associatedPrinter.printerStatusProperty().get())
                {
                    case PRINTING_PROJECT:
                    case RUNNING_MACRO_FILE:
                        associatedPrinter.setPrinterStatus(PrinterStatus.IDLE);
                        break;
                }
                macroBeingRun.set(null);
            }
        }
    }

    private void setParentPrintStatusIfIdle(PrinterStatus desiredStatus)
    {
        switch (associatedPrinter.printerStatusProperty().get())
        {
            case IDLE:
                associatedPrinter.setPrinterStatus(desiredStatus);
                break;
        }
    }

    public ReadOnlyObjectProperty<PrintQueueStatus> printQueueStatusProperty()
    {
        return printQueueStatus;
    }
}
