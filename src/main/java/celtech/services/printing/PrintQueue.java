/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.printing;

import celtech.appManager.Project;
import celtech.appManager.ProjectMode;
import celtech.configuration.ApplicationConfiguration;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.GCodeMacros;
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

    private Printer boundPrinter = null;
    private PrintState printState = PrintState.IDLE;
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
    private int idleToPrintTrigger = 0;

    public PrintQueue()
    {
        exec = Executors.newFixedThreadPool(1);

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
                setPrintStatus(PrintState.IDLE);
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
                    setPrintStatus(PrintState.SENDING_TO_PRINTER);
                } else
                {
                    setPrintStatus(PrintState.IDLE);
                }
            }
        };

        cancelPrintEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                steno.info(t.getSource().getTitle() + " has been cancelled");
            }
        };

        failedPrintEventHandler = new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                steno.error(t.getSource().getTitle() + " has failed");
                setPrintStatus(PrintState.IDLE);
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
                    setPrintStatus(PrintState.PRINTING);
                } else
                {
                    steno.error("Submission of job to printer failed");
                    cancelRun();
                }
            }
        };

        printJobIDListener = new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> ov, String oldValue, String newValue)
            {
                steno.info("Print job ID number is " + newValue + " and was " + oldValue);

                boolean roboxIsPrinting = false;

                if (newValue != null)
                {
                    if (newValue.codePointAt(0) != 0)
                    {
                        roboxIsPrinting = true;
                    }
                }

                switch (printState)
                {
                    case IDLE:
                        if (roboxIsPrinting)
                        {
                            //We must be printing!
                            steno.info("Print progress detected whilst in idle... switching to print mode");
                            double percentDone = (double) boundPrinter.getPrintJobLineNumber() / (double) gcodePrintService.getLinesInGCodeFile() * 100;
                            printQueueStatusString.setValue(String.format("Printing %.1f%%", percentDone));
//                            fxToJMEInterface.exposeGCodeModel(percentDone);
                            setPrintStatus(PrintState.PRINTING);
                        }
                        break;
                    case SENDING_TO_PRINTER:
                    case PRINTING:
                        if (roboxIsPrinting == false)
                        {
//                            fxToJMEInterface.exposeGCodeModel(0);
                            setPrintStatus(PrintState.IDLE);
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        printLineNumberListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue)
            {
                steno.info("Line number is " + newValue.toString() + " and was " + oldValue.toString());
                switch (printState)
                {
                    case IDLE:
                    case SENDING_TO_PRINTER:
//Ignore this state...
                        break;
                    case PRINTING:
                        int linesInFile = gcodePrintService.getLinesInGCodeFile();
                        double percentDone = newValue.doubleValue() / (double) linesInFile;
                        printProgressPercent.set(percentDone);
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

        setPrintStatus(PrintState.IDLE);
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
    private void setPrintQueueStatusString(String value)
    {
        printQueueStatusString.set(value);
    }

    public final String getPrintQueueStatusString()
    {
        return printQueueStatusString.get();
    }

    public final StringProperty printQueueStatusStringProperty()
    {
        return printQueueStatusString;
    }

    public synchronized boolean printProject(Printer printerToUse, Project project, PrintQualityEnumeration printQuality, SlicerSettings settings)
    {
        boolean acceptedPrintRequest = false;

        if (printState == PrintState.IDLE)
        {
            //Create the print job directory
            String printUUID = SystemUtils.generate16DigitID();

            String printJobDirectoryName = ApplicationConfiguration.getPrintSpoolDirectory() + printUUID;

            //TODO PUT SOMETHING HERE TO GET RID OF OLD PRINT FILES
            File printJobDirectory = new File(printJobDirectoryName);
            printJobDirectory.mkdirs();

            if (project.getProjectMode() == ProjectMode.MESH)
            {

                //Write out the slicer config
                settings.setStart_gcode(GCodeMacros.PRE_PRINT.getMacroContentsInOneLine());
                settings.setEnd_gcode(GCodeMacros.POST_PRINT.getMacroContentsInOneLine());
                settings.renderToFile(printJobDirectoryName + File.separator + printUUID + ApplicationConfiguration.printProfileFileExtension);

                boundPrinter = printerToUse;
                printerToUse.printJobLineNumberProperty().addListener(printLineNumberListener);
                printerToUse.printJobIDProperty().addListener(printJobIDListener);

                setPrintStatus(PrintState.SLICING);
                slicerService.reset();
                slicerService.setProject(project);
                slicerService.setSettings(settings);
                slicerService.setPrintJobUUID(printUUID);
                slicerService.setPrinterToUse(printerToUse);
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
                    gcodePrintService.setPrinterToUse(printerToUse);
                    gcodePrintService.start();
                    setPrintStatus(PrintState.SENDING_TO_PRINTER);
                    acceptedPrintRequest = true;
                } catch (IOException ex)
                {
                    steno.error("Error whilt preparing for print. Can't copy " + fileToCopyname + " to " + printjobFilename);
                }
            }
        }

        return acceptedPrintRequest;
    }

    private void setPrintStatus(PrintState newState)
    {
        switch (newState)
        {
            case IDLE:
                setPrintQueueStatusString("Idle");
                printProgressMessage.unbind();
                setPrintProgressMessage("");
                printProgressPercent.unbind();
                setPrintProgressPercent(0);
                setPrintInProgress(false);
                setDialogRequired(false);
                idleToPrintTrigger = 0;
                if (boundPrinter != null)
                {
                    boundPrinter.printJobIDProperty().unbind();
                    boundPrinter.printJobLineNumberProperty().unbind();
                }
                break;
            case SLICING:
                setPrintQueueStatusString("Preparing");
                setPrintProgressTitle("Preparing print");
                printProgressMessage.unbind();
                printProgressMessage.bind(slicerService.messageProperty());
                printProgressPercent.unbind();
                printProgressPercent.bind(slicerService.progressProperty());
                setPrintInProgress(true);
                setDialogRequired(true);
                break;
            case SENDING_TO_PRINTER:
                setPrintProgressTitle("Sending layout to printer");
                printProgressMessage.unbind();
                printProgressMessage.bind(gcodePrintService.messageProperty());
                printProgressPercent.unbind();
                printProgressPercent.bind(gcodePrintService.progressProperty());
                setPrintInProgress(true);
                setDialogRequired(true);
                break;
            case PRINTING:
                setPrintProgressTitle("Printing");
                printProgressMessage.unbind();
                printProgressPercent.unbind();
                setPrintProgressPercent(0);
                printProgressMessage.set("");
                setPrintInProgress(true);
                setDialogRequired(false);
                break;
            default:
                setPrintQueueStatusString("?");
                break;
        }
        printState = newState;
    }

    @Override
    public boolean cancelRun()
    {
        boolean cancelledRun = false;

        switch (printState)
        {
            case SLICING:
                if (slicerService.isRunning())
                {
                    slicerService.cancelRun();
                    setPrintStatus(PrintState.IDLE);
                    cancelledRun = true;
                }
                break;
            case SENDING_TO_PRINTER:
            case PRINTING:

                if (gcodePrintService.isRunning())
                {
                    gcodePrintService.cancelRun();
                }
//                printerUtils.sendAbortPrint(printJobSettings.getPrinter());
                setPrintStatus(PrintState.IDLE);
//                fxToJMEInterface.clearGCodeDisplay();
                cancelledRun = true;
                break;
            default:
                break;
        }

        return cancelledRun;
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
}
