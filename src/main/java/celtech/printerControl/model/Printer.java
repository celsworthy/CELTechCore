package celtech.printerControl.model;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.appManager.SystemNotificationManager;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.HeadContainer;
import celtech.configuration.MaterialType;
import celtech.configuration.PauseStatus;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.printerControl.PrintActionUnavailableException;
import celtech.printerControl.PrintJobRejectedException;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.PurgeRequiredException;
import celtech.printerControl.comms.CommandInterface;
import celtech.printerControl.comms.PrinterStatusConsumer;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.comms.commands.rx.GCodeDataResponse;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.ListFilesResponse;
import celtech.printerControl.comms.commands.rx.PrinterIDResponse;
import celtech.printerControl.comms.commands.rx.ReelEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.comms.commands.tx.FormatHeadEEPROM;
import celtech.printerControl.comms.commands.tx.FormatReelEEPROM;
import celtech.printerControl.comms.commands.tx.ListFiles;
import celtech.printerControl.comms.commands.tx.PausePrint;
import celtech.printerControl.comms.commands.tx.QueryFirmwareVersion;
import celtech.printerControl.comms.commands.tx.ReadHeadEEPROM;
import celtech.printerControl.comms.commands.tx.ReadPrinterID;
import celtech.printerControl.comms.commands.tx.ReadReelEEPROM;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacketFactory;
import celtech.printerControl.comms.commands.tx.SetAmbientLEDColour;
import celtech.printerControl.comms.commands.tx.SetFilamentInfo;
import celtech.printerControl.comms.commands.tx.SetReelLEDColour;
import celtech.printerControl.comms.commands.tx.SetTemperatures;
import celtech.printerControl.comms.commands.tx.StatusRequest;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;
import celtech.printerControl.comms.commands.tx.WriteHeadEEPROM;
import celtech.printerControl.comms.commands.tx.WritePrinterID;
import celtech.printerControl.comms.commands.tx.WriteReelEEPROM;
import celtech.printerControl.comms.events.RoboxResponseConsumer;
import celtech.services.printing.DatafileSendAlreadyInProgress;
import celtech.services.printing.DatafileSendNotInitialised;
import celtech.services.printing.PrintQueue;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import celtech.utils.PrinterUtils;
import celtech.utils.SystemUtils;
import celtech.utils.tasks.Cancellable;
import celtech.utils.tasks.TaskResponder;
import com.sun.javafx.print.PrintHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class Printer implements RoboxResponseConsumer
{

    private Stenographer steno = StenographerFactory.getStenographer(Printer.class.getName());

    protected final ObjectProperty<PrinterStatus> printerStatusProperty = new SimpleObjectProperty(PrinterStatus.IDLE);

    protected PrinterStatusConsumer printerStatusConsumer;
    protected CommandInterface commandInterface;

    private SystemNotificationManager systemNotificationManager;

    private boolean keepRunning = true;

    /*
     * State machine data
     */
    private final BooleanProperty canRemoveHeadProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty canPurgeHeadProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty mustPurgeHeadProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty canPrintProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty canRunMacroProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty canCancelProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty canAbortProperty = new SimpleBooleanProperty(false);

    /*
     * Physical model
     * Not sure how discovery will work, but we'll assume the following:
     *  - one identity
     *  - two reel controllers
     *  - one head controller
     *  - two extruder controllers
     * We'll use EEPROM status to drive whether they're enabled or not
     */
    private final PrinterIdentity printerIdentity = new PrinterIdentity();
    private final PrinterAncillarySystems printerAncillarySystems = new PrinterAncillarySystems();
    private final ObjectProperty<Head> head = new SimpleObjectProperty<>();
    private final ObservableList<Reel> reels = FXCollections.observableArrayList();
    private final ObservableList<Extruder> extruders = FXCollections.observableArrayList();

    /*
     * Temperature-related data
     */
    private long lastTimestamp = System.currentTimeMillis();

    private final ObservableList<String> gcodeTranscript = FXCollections.observableArrayList();

    private int dataFileSequenceNumber = 0;
    private static final int bufferSize = 512;
    private static final StringBuffer outputBuffer = new StringBuffer(bufferSize);
    private boolean printInitiated = false;

    private final ObjectProperty<PauseStatus> pauseStatus = new SimpleObjectProperty<>(PauseStatus.NOT_PAUSED);

    private PrintHelper printHelper;

    public Printer(PrinterStatusConsumer printerStatusConsumer, CommandInterface commandInterface)
    {
        this.printerStatusConsumer = printerStatusConsumer;
        this.commandInterface = commandInterface;

        printHelper = new PrintHelper();

        systemNotificationManager = Lookup.getSystemNotificationHandler();

        for (int i = 0; i < NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP; i++)
        {
            LineChart.Data<Number, Number> newAmbientPoint = new LineChart.Data<>(i, 0);
            ambientTemperatureDataPoints.add(newAmbientPoint);
            ambientTemperatureHistory.getData().add(newAmbientPoint);
            LineChart.Data<Number, Number> newBedPoint = new LineChart.Data<>(i, 0);
            bedTemperatureDataPoints.add(newBedPoint);
            bedTemperatureHistory.getData().add(newBedPoint);
            LineChart.Data<Number, Number> newNozzlePoint = new LineChart.Data<>(i, 0);
            nozzleTemperatureDataPoints.add(newNozzlePoint);
            nozzleTemperatureHistory.getData().add(newNozzlePoint);
        }

        ambientTargetTemperatureSeries.getData().add(ambientTargetPoint);
        bedTargetTemperatureSeries.getData().add(bedTargetPoint);
        nozzleTargetTemperatureSeries.getData().add(nozzleTargetPoint);

        setPrinterStatus(PrinterStatus.IDLE);
    }

    protected final void setPrinterStatus(PrinterStatus printerStatus)
    {

        // Can print should rely on filament loaded etc
//                            printerOKToPrint.bind(newValue.printerStatusProperty().isEqualTo(
//                        PrinterStatus.IDLE)
//                        .and(newValue.whyAreWeWaitingProperty().isEqualTo(
//                                WhyAreWeWaitingState.NOT_WAITING))
//                        .and(newValue.getHeadEEPROMStatusProperty().isEqualTo(EEPROMState.PROGRAMMED))
//                        .and((newValue.Filament1LoadedProperty().or(
//                                newValue.Filament2LoadedProperty())))
//                        .and(settingsScreenState.filamentProperty().isNotNull().or(
//                                newValue.loadedFilamentProperty().isNotNull())));
        switch (printerStatus)
        {
            case IDLE:
                canRemoveHeadProperty.set(true);
                canPurgeHeadProperty.set(true);
                canPrintProperty.set(true);
                canRunMacroProperty.set(true);
                canCancelProperty.set(false);
                canAbortProperty.set(false);
                break;
            case REMOVING_HEAD:
                canRemoveHeadProperty.set(false);
                canPurgeHeadProperty.set(false);
                canPrintProperty.set(false);
                canRunMacroProperty.set(false);
                canCancelProperty.set(false);
                canAbortProperty.set(false);
                break;
            case PURGING_HEAD:
                canRemoveHeadProperty.set(false);
                canPurgeHeadProperty.set(false);
                canPrintProperty.set(false);
                canRunMacroProperty.set(false);
                canCancelProperty.set(false);
                canAbortProperty.set(false);
                break;
            case PRINTING:
                canRemoveHeadProperty.set(false);
                canPurgeHeadProperty.set(false);
                canPrintProperty.set(false);
                canRunMacroProperty.set(false);
                canCancelProperty.set(true);
                canAbortProperty.set(true);
                break;
        }
        this.printerStatusProperty.set(printerStatus);
    }

    public ReadOnlyObjectProperty<PrinterStatus> getPrinterStatusProperty()
    {
        return printerStatusProperty;
    }

    public PrinterIdentity getPrinterIdentity()
    {
        return printerIdentity;
    }

    public PrinterAncillarySystems getPrinterAncillarySystems()
    {
        return printerAncillarySystems;
    }

    public ReadOnlyObjectProperty<Head> getHeadProperty()
    {
        return head;
    }

    public ObservableList<Reel> getReelsProperty()
    {
        return reels;
    }

    /*
     * Temperature-related methods
     */
    /**
     *
     * @return
     */
    public final IntegerProperty ambientTemperatureProperty()
    {
        return ambientTemperature;
    }

    /**
     *
     * @return
     */
    public final XYChart.Series<Number, Number> ambientTemperatureHistory()
    {
        return ambientTemperatureHistory;
    }

    /**
     *
     * @return
     */
    public final XYChart.Series<Number, Number> ambientTargetTemperatureHistory()
    {
        return ambientTargetTemperatureSeries;
    }

    /**
     *
     * @return
     */
    public final IntegerProperty ambientTargetTemperatureProperty()
    {
        return ambientTargetTemperature;
    }

    /**
     *
     * @return
     */
    public final IntegerProperty bedTemperatureProperty()
    {
        return bedTemperature;
    }

    /**
     *
     * @return
     */
    public final XYChart.Series<Number, Number> bedTemperatureHistory()
    {
        return bedTemperatureHistory;
    }

    /**
     *
     * @return
     */
    public final XYChart.Series<Number, Number> bedTargetTemperatureHistory()
    {
        return bedTargetTemperatureSeries;
    }

    /**
     *
     * @return
     */
    public final IntegerProperty bedFirstLayerTargetTemperatureProperty()
    {
        return bedFirstLayerTargetTemperature;
    }

    /**
     *
     * @return
     */
    public final IntegerProperty bedTargetTemperatureProperty()
    {
        return bedTargetTemperature;
    }

    /**
     *
     * @return
     */
    public final IntegerProperty extruderTemperatureProperty()
    {
        return nozzleTemperature;
    }

    /**
     *
     * @return
     */
    public final XYChart.Series<Number, Number> nozzleTemperatureHistory()
    {
        return nozzleTemperatureHistory;
    }

    /**
     *
     * @return
     */
    public final XYChart.Series<Number, Number> nozzleTargetTemperatureHistory()
    {
        return nozzleTargetTemperatureSeries;
    }

    /**
     *
     * @return
     */
    public final IntegerProperty nozzleFirstLayerTargetTemperatureProperty()
    {
        return nozzleFirstLayerTargetTemperature;
    }

    /**
     *
     * @return
     */
    public final IntegerProperty nozzleTargetTemperatureProperty()
    {
        return nozzleTargetTemperature;
    }

    /*
     * Remove head
     */
    public final ReadOnlyBooleanProperty canRemoveHeadProperty()
    {
        return canRemoveHeadProperty;
    }

    public void removeHead(TaskResponder responder) throws PrinterException
    {
        if (!canRemoveHead())
        {
            throw new PrinterException("Head remove not permitted");
        }

        setPrinterStatus(PrinterStatus.REMOVING_HEAD);

        final Cancellable cancellable = new Cancellable();

        new Thread(() ->
        {
            boolean success = doRemoveHeadActivity(cancellable);

            Lookup.getTaskExecutor().runOnGUIThread(responder, success, "Ready to remove head");

            setPrinterStatus(PrinterStatus.IDLE);

        }, "Removing head").start();
    }

    protected boolean doRemoveHeadActivity(Cancellable cancellable)
    {
        boolean success = false;

        try
        {
            for (Extruder extruderController : extruders)
            {
                if (extruderController.filamentLoaded.getValue())
                {
                    transmitDirectGCode(GCodeConstants.ejectFilament + " " + extruderController.getExtruderAxisLetter(), false);

                    if (PrinterUtils.waitOnBusy(this, cancellable))
                    {
                        return false;
                    }
                }
            }

            transmitDirectGCode(GCodeConstants.carriageAbsoluteMoveMode, false);
            transmitDirectGCode("G28 X Y", false);
            transmitDirectGCode("G0 X170 Y0 Z20", false);
            PrinterUtils.waitOnBusy(this, cancellable);

            success = true;
        } catch (RoboxCommsException ex)
        {
            steno.error("Comms exception whilst executing remove head");
        }

        return success;
    }
    /*
     * Purge head
     */

    public final ReadOnlyBooleanProperty canPurgeHeadProperty()
    {
        return canPurgeHeadProperty;
    }

    protected final FloatProperty purgeTemperatureProperty = new SimpleFloatProperty(0);

    public void prepareToPurgeHead(TaskResponder responder) throws PrinterException
    {
        if (!canPurgeHead())
        {
            throw new PrinterException("Purge not permitted");
        }

        setPrinterStatus(PrinterStatus.PURGING_HEAD);

        final Cancellable cancellable = new Cancellable();

        new Thread(() ->
        {
            boolean success = doPurgeHeadActivity(cancellable);

            Lookup.getTaskExecutor().runOnGUIThread(responder, success, "Head Prepared for Purge");

        }, "Preparing for purge").start();
    }

    /*
     * Purging
     */
    private HeadEEPROMDataResponse headDataPrePurge = null;

    protected boolean doPrepareToPurgeHeadActivity(Cancellable cancellable)
    {
        boolean success = false;

        try
        {
            headDataPrePurge = transmitReadHeadEEPROM();

            float nozzleTemperature = 0;

            // The nozzle should be heated to a temperature halfway between the last temperature stored on the head and the current required temperature stored on the reel
            SettingsScreenState settingsScreenState = SettingsScreenState.getInstance();

            Filament settingsFilament = settingsScreenState.getFilament();

            if (settingsFilament != null)
            {
                nozzleTemperature = settingsFilament.getNozzleTemperature();
            } else
            {
                //TODO Update for multiple reels
                nozzleTemperature = reels.get(0).nozzleTemperatureProperty.floatValue();
            }

            float temperatureDifference = nozzleTemperature - headDataPrePurge.getLastFilamentTemperature();
            setPurgeTemperature(nozzleTemperature);

            success = true;
        } catch (RoboxCommsException ex)
        {
            // Success is already false..
        }
        return success;
    }

    public void setPurgeTemperature(float purgeTemperature)
    {
        // Force the purge temperature to remain between 180 and max temp in head degrees
        purgeTemperatureProperty.set((int) Math.min(headDataPrePurge.getMaximumTemperature(), Math.max(180.0, purgeTemperature)));
    }

    public void purgeHead(TaskResponder responder) throws PrinterException
    {
        if (printerStatusProperty.get() != PrinterStatus.PURGING_HEAD)
        {
            throw new PrinterException("Purge not permitted");
        }

        final Cancellable cancellable = new Cancellable();

        new Thread(() ->
        {
            boolean success = doPurgeHeadActivity(cancellable);

            Lookup.getTaskExecutor().runOnGUIThread(responder, success, "Head Purged");

            setPrinterStatus(PrinterStatus.IDLE);

        }, "Purging head").start();
    }

    protected boolean doPurgeHeadActivity(Cancellable cancellable)
    {
        boolean success = false;

        try
        {
            runMacroWithoutPurgeCheck("Purge Material");
            PrinterUtils.waitOnMacroFinished(this, cancellable);
            success = true;
        } catch (PrinterException ex)
        {
            // success is already false...
        }

        return success;
    }

    /*
     * Print
     */
    public final ReadOnlyBooleanProperty getCanPrintProperty()
    {
        return canPrintProperty;
    }

    /*
     * Cancel
     */
    public final ReadOnlyBooleanProperty getCanCancelProperty()
    {
        return canCancelProperty;
    }

    /**
     *
     */
    public final ReadOnlyBooleanProperty getCanAbortProperty()
    {
        return canAbortProperty;
    }

    public void abort()
    {
        printQueue.abortPrint();
    }

    /**
     *
     */
    public final ReadOnlyBooleanProperty getCanPauseProperty()
    {
        return canPauseProperty;
    }

    public void pause()
    {
        printQueue.pausePrint();
    }

    /**
     *
     */
    public final ReadOnlyBooleanProperty getCanResumeProperty()
    {
        return canResumeProperty;
    }

    public void resume()
    {
        printQueue.resumePrint();
    }

    /**
     *
     * @return
     */
    public int getDataFileSequenceNumber()
    {
        return dataFileSequenceNumber;
    }

    /**
     *
     * @return
     */
    public boolean isPrintInitiated()
    {
        return printInitiated;
    }

    /*
     * Macros
     */
    public final void runMacro(String macroName) throws PrinterException
    {
        if (!canRunMacroProperty.get())
        {
            throw new PrintActionUnavailableException("Run macro not available");
        }

        if (mustPurgeHeadProperty.get())
        {
            throw new PurgeRequiredException("Cannot run macro - purge required");
        }

        boolean jobAccepted = printQueue.printGCodeFile(GCodeMacros.getFilename(macroName), true);

        if (!jobAccepted)
        {
            throw new PrintJobRejectedException("Macro " + macroName + " could not be run");
        }
    }

    public final void runMacroWithoutPurgeCheck(String macroName) throws PrinterException
    {
        if (!canRunMacroProperty.get())
        {
            throw new PrintActionUnavailableException("Run macro not available");
        }

        boolean jobAccepted = printQueue.printGCodeFile(GCodeMacros.getFilename(macroName), true);

        if (!jobAccepted)
        {
            throw new PrintJobRejectedException("Macro " + macroName + " could not be run");
        }
    }

    /*
     * Data transmission commands
     */
    /**
     *
     * @param gcodeToSend
     * @param addToTranscript
     * @return
     * @throws RoboxCommsException
     */
    private String transmitDirectGCode(final String gcodeToSend, boolean addToTranscript) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.EXECUTE_GCODE);
        gcodePacket.setMessagePayload(gcodeToSend);

        GCodeDataResponse response = (GCodeDataResponse) commandInterface.writeToPrinter(gcodePacket);

        if (addToTranscript)
        {
            Platform.runLater(new Runnable()
            {

                public void run()
                {
                    if (response == null)
                    {
                        addToGCodeTranscript(Lookup.i18n("gcodeEntry.errorMessage"));
                    } else if (!response.getGCodeResponse().trim().equals(""))
                    {
                        addToGCodeTranscript(response.getGCodeResponse());
                    }
                }
            });
        }

        return response.getGCodeResponse();
    }

    /**
     *
     * @param macroName
     * @throws RoboxCommsException
     */
    public void transmitStoredGCode(final String macroName) throws RoboxCommsException
    {
        transmitStoredGCode(macroName, true);
    }

    /**
     *
     * @param macroName
     * @param checkForPurge
     * @throws RoboxCommsException
     */
    public void transmitStoredGCode(final String macroName, boolean checkForPurge) throws RoboxCommsException
    {
        if (printQueue.getPrintStatus() == PrinterStatus.IDLE)
        {
            if (checkForPurge)
            {
                boolean purgeConsent = PrinterUtils.getInstance().offerPurgeIfNecessary(this);

                if (purgeConsent)
                {
                    DisplayManager.getInstance().getPurgeInsetPanelController().purgeAndRunMacro(macroName, this);
                } else
                {
                    printQueue.printGCodeFile(GCodeMacros.getFilename(macroName), true);
                }
            } else
            {
                printQueue.printGCodeFile(GCodeMacros.getFilename(macroName), true);
            }
        }
    }

    private boolean transmitDataFileStart(final String fileID) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.START_OF_DATA_FILE);
        gcodePacket.setMessagePayload(fileID);

        AckResponse response = (AckResponse) commandInterface.writeToPrinter(gcodePacket);
        boolean success = false;
        // Only check for SD card errors here...
        success = !response.isSdCardError();

        return success;
    }

    private AckResponse transmitDataFileChunk(final String payloadData, final int sequenceNumber) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.DATA_FILE_CHUNK);
        gcodePacket.setMessagePayload(payloadData);
        gcodePacket.setSequenceNumber(sequenceNumber);

        AckResponse response = (AckResponse) commandInterface.writeToPrinter(gcodePacket);
        dataFileSequenceNumber++;

        return response;
    }

    private AckResponse transmitDataFileEnd(final String payloadData, final int sequenceNumber) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.END_OF_DATA_FILE);
        gcodePacket.setMessagePayload(payloadData);
        gcodePacket.setSequenceNumber(sequenceNumber);

        return (AckResponse) commandInterface.writeToPrinter(gcodePacket);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    public AckResponse transmitReportErrors() throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.REPORT_ERRORS);

        return (AckResponse) commandInterface.writeToPrinter(gcodePacket);
    }

    /**
     *
     * @throws RoboxCommsException
     */
    public void transmitResetErrors() throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.RESET_ERRORS);

        commandInterface.writeToPrinter(gcodePacket);
    }

    /**
     *
     * @param firmwareID
     * @return
     * @throws RoboxCommsException
     */
    public boolean transmitUpdateFirmware(final String firmwareID) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.UPDATE_FIRMWARE);
        gcodePacket.setMessagePayload(firmwareID);

        AckResponse response = (AckResponse) commandInterface.writeToPrinter(
            gcodePacket);

        return (response.isError());
    }

    private void transmitInitiatePrint(final String printJobUUID) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.INITIATE_PRINT);
        gcodePacket.setMessagePayload(printJobUUID);

        commandInterface.writeToPrinter(gcodePacket);
    }

    /**
     *
     * @throws RoboxCommsException
     */
    public void transmitPausePrint() throws RoboxCommsException
    {
        PausePrint gcodePacket = (PausePrint) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.PAUSE_RESUME_PRINT);
        gcodePacket.setPause();

        commandInterface.writeToPrinter(gcodePacket);
    }

    /**
     *
     * @throws RoboxCommsException
     */
    public void transmitResumePrint() throws RoboxCommsException
    {
        PausePrint gcodePacket = (PausePrint) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.PAUSE_RESUME_PRINT);
        gcodePacket.setResume();

        commandInterface.writeToPrinter(gcodePacket);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    public AckResponse transmitFormatHeadEEPROM() throws RoboxCommsException
    {
        FormatHeadEEPROM formatHead = (FormatHeadEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.FORMAT_HEAD_EEPROM);
        return (AckResponse) commandInterface.writeToPrinter(formatHead);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    public AckResponse transmitFormatReelEEPROM() throws RoboxCommsException
    {
        FormatReelEEPROM formatReel = (FormatReelEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.FORMAT_REEL_EEPROM);
        return (AckResponse) commandInterface.writeToPrinter(formatReel);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    public ReelEEPROMDataResponse transmitReadReelEEPROM() throws RoboxCommsException
    {
        ReadReelEEPROM readReel = (ReadReelEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.READ_REEL_EEPROM);
        return (ReelEEPROMDataResponse) commandInterface.writeToPrinter(readReel);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    public HeadEEPROMDataResponse transmitReadHeadEEPROM() throws RoboxCommsException
    {
        ReadHeadEEPROM readHead = (ReadHeadEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.READ_HEAD_EEPROM);
        return (HeadEEPROMDataResponse) commandInterface.writeToPrinter(readHead);
    }

    /**
     *
     * @param filament
     * @return
     * @throws RoboxCommsException
     */
    public AckResponse transmitWriteReelEEPROM(Filament filament) throws RoboxCommsException
    {
        WriteReelEEPROM writeReelEEPROM = (WriteReelEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.WRITE_REEL_EEPROM);
        writeReelEEPROM.populateEEPROM(filament.getFilamentID(),
                                       filament.getFirstLayerNozzleTemperature(),
                                       filament.getNozzleTemperature(),
                                       filament.getFirstLayerBedTemperature(),
                                       filament.getBedTemperature(),
                                       filament.getAmbientTemperature(),
                                       filament.getDiameter(),
                                       filament.getFilamentMultiplier(),
                                       filament.getFeedRateMultiplier(),
                                       filament.getRemainingFilament(),
                                       filament.getFriendlyFilamentName(),
                                       filament.getMaterial(),
                                       filament.getDisplayColour());
        return (AckResponse) commandInterface.writeToPrinter(writeReelEEPROM);
    }

    /**
     *
     * @param filamentID
     * @param reelFirstLayerNozzleTemperature
     * @param reelNozzleTemperature
     * @param reelFirstLayerBedTemperature
     * @param reelBedTemperature
     * @param reelAmbientTemperature
     * @param reelFilamentDiameter
     * @param reelFilamentMultiplier
     * @param reelFeedRateMultiplier
     * @param reelRemainingFilament
     * @throws RoboxCommsException
     */
    public void transmitWriteReelEEPROM(String filamentID,
        float reelFirstLayerNozzleTemperature, float reelNozzleTemperature,
        float reelFirstLayerBedTemperature, float reelBedTemperature, float reelAmbientTemperature,
        float reelFilamentDiameter,
        float reelFilamentMultiplier, float reelFeedRateMultiplier, float reelRemainingFilament,
        String friendlyName, MaterialType materialType, Color displayColour) throws RoboxCommsException
    {
        WriteReelEEPROM writeReelEEPROM = (WriteReelEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.WRITE_REEL_EEPROM);
        writeReelEEPROM.populateEEPROM(filamentID, reelFirstLayerNozzleTemperature,
                                       reelNozzleTemperature,
                                       reelFirstLayerBedTemperature, reelBedTemperature,
                                       reelAmbientTemperature, reelFilamentDiameter,
                                       reelFilamentMultiplier, reelFeedRateMultiplier,
                                       reelRemainingFilament,
                                       friendlyName, materialType, displayColour);
        commandInterface.writeToPrinter(writeReelEEPROM);
    }

    /**
     *
     * @param headToWrite
     * @throws RoboxCommsException
     */
    public void transmitWriteHeadEEPROM(Head headToWrite) throws RoboxCommsException
    {
        WriteHeadEEPROM writeHeadEEPROM = (WriteHeadEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.WRITE_HEAD_EEPROM);
        writeHeadEEPROM.populateEEPROM(headToWrite);
        commandInterface.writeToPrinter(writeHeadEEPROM);
    }

    /**
     *
     * @param headTypeCode
     * @param headUniqueID
     * @param maximumTemperature
     * @param thermistorBeta
     * @param thermistorTCal
     * @param nozzle1XOffset
     * @param nozzle1YOffset
     * @param nozzle1ZOffset
     * @param nozzle1BOffset
     * @param nozzle2XOffset
     * @param nozzle2YOffset
     * @param nozzle2ZOffset
     * @param nozzle2BOffset
     * @param lastFilamentTemperature
     * @param hourCounter
     * @return
     * @throws RoboxCommsException
     */
    public AckResponse transmitWriteHeadEEPROM(String headTypeCode, String headUniqueID,
        float maximumTemperature,
        float thermistorBeta, float thermistorTCal,
        float nozzle1XOffset, float nozzle1YOffset, float nozzle1ZOffset, float nozzle1BOffset,
        float nozzle2XOffset, float nozzle2YOffset, float nozzle2ZOffset, float nozzle2BOffset,
        float lastFilamentTemperature, float hourCounter) throws RoboxCommsException
    {
        WriteHeadEEPROM writeHeadEEPROM = (WriteHeadEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.WRITE_HEAD_EEPROM);
        writeHeadEEPROM.populateEEPROM(headTypeCode,
                                       headUniqueID,
                                       maximumTemperature,
                                       thermistorBeta,
                                       thermistorTCal,
                                       nozzle1XOffset,
                                       nozzle1YOffset,
                                       nozzle1ZOffset,
                                       nozzle1BOffset,
                                       nozzle2XOffset,
                                       nozzle2YOffset,
                                       nozzle2ZOffset,
                                       nozzle2BOffset,
                                       lastFilamentTemperature,
                                       hourCounter);
        return (AckResponse) commandInterface.writeToPrinter(writeHeadEEPROM);
    }

    /*
     * Higher level controls
     */
    /**
     *
     * @param on
     * @throws RoboxCommsException
     */
    public void switchOnHeadLEDs(boolean on) throws RoboxCommsException
    {
        if (on)
        {
            transmitDirectGCode(GCodeConstants.switchOnHeadLEDs, false);
        } else
        {
            transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, false);
        }
    }

    /**
     *
     * @param colour
     * @throws RoboxCommsException
     */
    public void transmitSetAmbientLEDColour(Color colour) throws RoboxCommsException
    {
        SetAmbientLEDColour ledColour = (SetAmbientLEDColour) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.SET_AMBIENT_LED_COLOUR);
        ledColour.setLEDColour(colour);
        commandInterface.writeToPrinter(ledColour);
    }

    /**
     *
     * @param colour
     * @throws RoboxCommsException
     */
    public void transmitSetReelLEDColour(Color colour) throws RoboxCommsException
    {
        SetReelLEDColour ledColour = (SetReelLEDColour) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.SET_REEL_LED_COLOUR);
        ledColour.setLEDColour(colour);
        commandInterface.writeToPrinter(ledColour);
    }

    /**
     *
     * @throws RoboxCommsException
     */
    public void transmitReadPrinterID() throws RoboxCommsException
    {
        ReadPrinterID readId = (ReadPrinterID) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.READ_PRINTER_ID);
        commandInterface.writeToPrinter(readId);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    public FirmwareResponse transmitReadFirmwareVersion() throws RoboxCommsException
    {
        QueryFirmwareVersion readFirmware = (QueryFirmwareVersion) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.QUERY_FIRMWARE_VERSION);
        return (FirmwareResponse) commandInterface.writeToPrinter(readFirmware);
    }

    /**
     *
     * @param nozzleFirstLayerTarget
     * @param nozzleTarget
     * @param bedFirstLayerTarget
     * @param bedTarget
     * @param ambientTarget
     * @throws RoboxCommsException
     */
    public void transmitSetTemperatures(double nozzleFirstLayerTarget, double nozzleTarget,
        double bedFirstLayerTarget, double bedTarget, double ambientTarget) throws RoboxCommsException
    {
        SetTemperatures setTemperatures = (SetTemperatures) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.SET_TEMPERATURES);
        setTemperatures.setTemperatures(nozzleFirstLayerTarget, nozzleTarget, bedFirstLayerTarget,
                                        bedTarget, ambientTarget);
        commandInterface.writeToPrinter(setTemperatures);
    }

    /**
     *
     * @param filamentDiameter
     * @param filamentMultiplier
     * @param feedRateMultiplier
     * @throws RoboxCommsException
     */
    public void transmitSetFilamentInfo(double filamentDiameter, double filamentMultiplier,
        double feedRateMultiplier) throws RoboxCommsException
    {
        SetFilamentInfo setFilamentInfo = (SetFilamentInfo) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.SET_FILAMENT_INFO);
        setFilamentInfo.setFilamentInfo(filamentDiameter, filamentMultiplier, feedRateMultiplier);
        commandInterface.writeToPrinter(setFilamentInfo);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    public ListFilesResponse transmitListFiles() throws RoboxCommsException
    {
        ListFiles listFiles = (ListFiles) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.LIST_FILES);
        return (ListFilesResponse) commandInterface.writeToPrinter(listFiles);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    public StatusResponse transmitStatusRequest() throws RoboxCommsException
    {
        StatusRequest statusRequest = (StatusRequest) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.STATUS_REQUEST);
        return (StatusResponse) commandInterface.writeToPrinter(statusRequest);
    }

    /**
     *
     * @param fileID
     * @return
     * @throws DatafileSendAlreadyInProgress
     * @throws RoboxCommsException
     */
    public boolean initialiseDataFileSend(String fileID) throws DatafileSendAlreadyInProgress, RoboxCommsException
    {
        boolean success = false;
        success = transmitDataFileStart(fileID);
        outputBuffer.delete(0, outputBuffer.length());
        dataFileSequenceNumber = 0;
        printInitiated = false;

        return success;
    }

    /**
     *
     * @param jobUUID
     * @throws RoboxCommsException
     */
    public void initiatePrint(String jobUUID) throws RoboxCommsException
    {
        transmitInitiatePrint(jobUUID);
        printInitiated = true;
    }

    /**
     *
     * @param hexDigits
     * @param lastPacket
     * @param appendCRLF
     * @throws DatafileSendNotInitialised
     * @throws RoboxCommsException
     */
    public void sendDataFileChunk(String hexDigits, boolean lastPacket, boolean appendCRLF) throws DatafileSendNotInitialised, RoboxCommsException
    {
//        if (lastPacket == true)
//        {
//            steno.info("Got last packet");
//        }
        boolean dataIngested = false;

        if (appendCRLF)
        {
            hexDigits += "\r";
        }

        int remainingCharacters = hexDigits.length();

        while (remainingCharacters > 0)
        {
            /*
             * Load the entire line if possible
             */
            if ((outputBuffer.capacity() - outputBuffer.length()) >= remainingCharacters)
            {
                String stringToWrite = hexDigits.substring(hexDigits.length() - remainingCharacters);
                outputBuffer.append(stringToWrite);
                dataIngested = true;
                remainingCharacters -= stringToWrite.length();
            } else
            {
                /*
                 * put in what we can
                 */
                String stringToWrite = hexDigits.substring(hexDigits.length() - remainingCharacters,
                                                           (hexDigits.length() - remainingCharacters)
                                                           + (outputBuffer.capacity()
                                                           - outputBuffer.length()));
                outputBuffer.append(stringToWrite);
                remainingCharacters -= stringToWrite.length();
            }

            /*
             * If this is the last packet then send as an end...
             */
            if (dataIngested && lastPacket)
            {
                steno.debug("Final complete chunk:" + outputBuffer.toString() + " seq:"
                    + dataFileSequenceNumber);
                AckResponse response = transmitDataFileEnd(outputBuffer.toString(), dataFileSequenceNumber);
                if (response.isError())
                {
                    steno.error("Error sending final data file chunk - seq " + dataFileSequenceNumber);
                }
            } else if ((outputBuffer.capacity() - outputBuffer.length()) == 0)
            {
                /*
                 * Send when full
                 */
//                steno.info("Sending chunk seq:" + dataFileSequenceNumber);
                AckResponse response = transmitDataFileChunk(outputBuffer.toString(), dataFileSequenceNumber);
                if (response.isError())
                {
                    steno.error("Error sending data file chunk - seq " + dataFileSequenceNumber);
                }
                outputBuffer.delete(0, bufferSize);
                dataFileSequenceNumber++;
            }
        }
    }

    /**
     *
     * @param project
     * @param filament
     * @param printQuality
     * @param settings
     */
    public void printProject(Project project, Filament filament,
        PrintQualityEnumeration printQuality, RoboxProfile settings)
    {

        //TODO modify for multiple reels
        if (filament != null
            && reels.get(0).isSameAs(filament) == false)
        {
            try
            {
                transmitSetTemperatures(filament.getFirstLayerNozzleTemperature(),
                                        filament.getNozzleTemperature(),
                                        filament.getFirstLayerBedTemperature(),
                                        filament.getBedTemperature(),
                                        filament.getAmbientTemperature());
                transmitSetFilamentInfo(filament.getDiameter(),
                                        filament.getFilamentMultiplier(),
                                        filament.getFeedRateMultiplier());
            } catch (RoboxCommsException ex)
            {
                steno.error("Failure to set temperatures prior to print");
            }
        }

        try
        {
            transmitDirectGCode(GCodeConstants.goToTargetFirstLayerBedTemperature, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error whilst sending preheat commands");
        }

        printQueue.printProject(project, printQuality, settings);
    }

    /**
     *
     * @param gcodeToSend
     */
    public void addToGCodeTranscript(String gcodeToSend)
    {
        gcodeTranscript.add(gcodeToSend);
    }

    /**
     *
     * @return
     */
    public ObservableList<String> gcodeTranscriptProperty()
    {
        return gcodeTranscript;
    }

    public void shutdown()
    {
        keepRunning = false;
    }

    @Override
    public void processRoboxResponse(RoboxRxPacket rxPacket)
    {
        switch (rxPacket.getPacketType())
        {
            case ACK_WITH_ERRORS:
                systemNotificationManager.processErrorPacketFromPrinter((AckResponse) rxPacket, this);
                break;

            case STATUS_RESPONSE:
                StatusResponse statusResponse = (StatusResponse) rxPacket;
//                steno.info("Got:" + statusResponse.toString());

                setAmbientTemperature(statusResponse.getAmbientTemperature());
                setAmbientTargetTemperature(statusResponse.getAmbientTargetTemperature());
                setBedTargetTemperature(statusResponse.getBedTemperature());
                setExtruderTemperature(statusResponse.getNozzleTemperature());
                setBedFirstLayerTargetTemperature(statusResponse.getBedFirstLayerTargetTemperature());
                setBedTargetTemperature(statusResponse.getBedTargetTemperature());
                setNozzleFirstLayerTargetTemperature(
                    statusResponse.getNozzleFirstLayerTargetTemperature());
                setNozzleTargetTemperature(statusResponse.getNozzleTargetTemperature());

                long now = System.currentTimeMillis();
                if ((now - lastTimestamp) >= 999)
                {
                    lastTimestamp = now;

                    for (int pointCounter = 0; pointCounter < NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP
                        - 1; pointCounter++)
                    {
                        ambientTemperatureDataPoints.get(pointCounter).setYValue(
                            ambientTemperatureDataPoints.get(pointCounter + 1).getYValue());
                        bedTemperatureDataPoints.get(pointCounter).setYValue(
                            bedTemperatureDataPoints.get(pointCounter + 1).getYValue());
                        nozzleTemperatureDataPoints.get(pointCounter).setYValue(
                            nozzleTemperatureDataPoints.get(pointCounter + 1).getYValue());
                    }

                    ambientTemperatureDataPoints.get(NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP - 1).setYValue(
                        statusResponse.getAmbientTemperature());

                    if (statusResponse.getBedTemperature()
                        < ApplicationConfiguration.maxTempToDisplayOnGraph
                        && statusResponse.getBedTemperature()
                        > ApplicationConfiguration.minTempToDisplayOnGraph)
                    {
                        bedTemperatureDataPoints.get(NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP - 1).setYValue(
                            statusResponse.getBedTemperature());
                    }

                    nozzleTemperatureDataPoints.add(bedTargetPoint);

                    if (statusResponse.getNozzleTemperature()
                        < ApplicationConfiguration.maxTempToDisplayOnGraph
                        && statusResponse.getNozzleTemperature()
                        > ApplicationConfiguration.minTempToDisplayOnGraph)
                    {
                        nozzleTemperatureDataPoints.get(NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP - 1).setYValue(
                            statusResponse.getNozzleTemperature());
                    }
                }

                ambientTargetPoint.setYValue(statusResponse.getAmbientTargetTemperature());
                switch (statusResponse.getBedHeaterMode())
                {
                    case OFF:
                        bedTargetPoint.setYValue(0);
                        break;
                    case FIRST_LAYER:
                        bedTargetPoint.setYValue(statusResponse.getBedFirstLayerTargetTemperature());
                        break;
                    case NORMAL:
                        bedTargetPoint.setYValue(statusResponse.getBedTargetTemperature());
                        break;
                    default:
                        break;
                }
                switch (statusResponse.getNozzleHeaterMode())
                {
                    case OFF:
                        nozzleTargetPoint.setYValue(0);
                        break;
                    case FIRST_LAYER:
                        nozzleTargetPoint.setYValue(
                            statusResponse.getNozzleFirstLayerTargetTemperature());
                        break;
                    case NORMAL:
                        nozzleTargetPoint.setYValue(statusResponse.getNozzleTargetTemperature());
                        break;
                    default:
                        break;
                }

                /*
                 * Ancillary systems
                 */
                printerAncillarySystems.ambientFanOnProperty.set(statusResponse.isAmbientFanOn());
                printerAncillarySystems.bedHeaterModeProperty.set(statusResponse.getBedHeaterMode());
                printerAncillarySystems.headFanOnProperty.set(statusResponse.isHeadFanOn());
                printerAncillarySystems.XStopSwitchProperty.set(statusResponse.isxSwitchStatus());
                printerAncillarySystems.YStopSwitchProperty.set(statusResponse.isySwitchStatus());
                printerAncillarySystems.ZStopSwitchProperty.set(statusResponse.iszSwitchStatus());
                printerAncillarySystems.ZTopStopSwitchProperty.set(statusResponse.isTopZSwitchStatus());
                printerAncillarySystems.bAxisHomeProperty.set(statusResponse.isNozzleSwitchStatus());
                printerAncillarySystems.lidOpenProperty.set(statusResponse.isLidSwitchStatus());
                printerAncillarySystems.reelButtonProperty.set(statusResponse.isReelButtonStatus());
                printerAncillarySystems.whyAreWeWaitingProperty.set(statusResponse.getWhyAreWeWaitingState());

                /*
                 * Heater
                 */
                //TODO modify for multiple heaters
                if (head.getNozzleHeaters().size() > 0)
                {
                    head.getNozzleHeaters().get(0).heaterMode.set(statusResponse.getNozzleHeaterMode());
                }

                /*
                 * Extruders
                 */
                //TODO configure properly for multiple extruders
                extruders.get(0).filamentLoaded.set(statusResponse.isFilament1SwitchStatus());
                extruders.get(0).indexWheelState.set(statusResponse.isEIndexStatus());
                extruders.get(1).filamentLoaded.set(statusResponse.isFilament2SwitchStatus());
                extruders.get(1).indexWheelState.set(statusResponse.isDIndexStatus());

                if (pauseStatus.get() != statusResponse.getPauseStatus()
                    && statusResponse.getPauseStatus() == PauseStatus.PAUSED)
                {
                    printQueue.printerHasPaused();
                } else if (pauseStatus.get() != statusResponse.getPauseStatus()
                    && statusResponse.getPauseStatus() == PauseStatus.NOT_PAUSED)
                {
                    printQueue.printerHasResumed();
                }
                pauseStatus.set(statusResponse.getPauseStatus());

//                setPrintJobLineNumber(statusResponse.getPrintJobLineNumber());
//                setPrintJobID(statusResponse.getRunningPrintJobID());
                //TODO modify to work with multiple reels
                EEPROMState lastReelState = reels.get(0).reelEEPROMStatusProperty.get();

                if (lastReelState != EEPROMState.PROGRAMMED
                    && statusResponse.getReelEEPROMState() == EEPROMState.PROGRAMMED)
                {
                    Filament.repairFilamentIfNecessary(this);
                } else if (lastReelState != EEPROMState.NOT_PRESENT
                    && statusResponse.getReelEEPROMState() == EEPROMState.NOT_PRESENT)
                {
//                    loadedFilament.set(null);
                    //TODO modify to work with multiple reels
                    reels.get(0).noReelLoaded();
                }

                reels.get(0).reelEEPROMStatusProperty.set(statusResponse.getReelEEPROMState());

                EEPROMState lastHeadState = head.headEEPROMStatus.get();

                if (lastHeadState != EEPROMState.PROGRAMMED
                    && statusResponse.getHeadEEPROMState() == EEPROMState.PROGRAMMED)
                {
                    head.repair(null);
                } else if (lastHeadState != EEPROMState.NOT_PRESENT
                    && statusResponse.getHeadEEPROMState() == EEPROMState.NOT_PRESENT)
                {
                    head.noHeadAttached();
                }

                head.headEEPROMStatus.set(statusResponse.getHeadEEPROMState());

                head.headXPosition.set(statusResponse.getHeadXPosition());
                head.headYPosition.set(statusResponse.getHeadYPosition());
                head.headZPosition.set(statusResponse.getHeadZPosition());

                setPrinterStatus(printQueue.getPrintStatus());
                break;

            case FIRMWARE_RESPONSE:
                FirmwareResponse fwResponse = (FirmwareResponse) rxPacket;
                printerIdentity.firmwareVersionProperty.set(fwResponse.getFirmwareRevision());
                break;
            case PRINTER_ID_RESPONSE:
                PrinterIDResponse idResponse = (PrinterIDResponse) rxPacket;
                printerIdentity.printermodelProperty.set(idResponse.getModel());
                printerIdentity.printereditionProperty.set(idResponse.getEdition());
                printerIdentity.printerweekOfManufactureProperty.set(idResponse.getWeekOfManufacture());
                printerIdentity.printeryearOfManufactureProperty.set(idResponse.getYearOfManufacture());
                printerIdentity.printerpoNumberProperty.set(idResponse.getPoNumber());
                printerIdentity.printerserialNumberProperty.set(idResponse.getSerialNumber());
                printerIdentity.printercheckByteProperty.set(idResponse.getCheckByte());
                printerIdentity.printerFriendlyNameProperty.set(idResponse.getPrinterFriendlyName());
                printerIdentity.printerColourProperty.set(idResponse.getPrinterColour());
                try
                {
                    transmitSetAmbientLEDColour(idResponse.getPrinterColour());

                } catch (RoboxCommsException ex)
                {
                    steno.warning("Couldn't set printer LED colour");
                }
                break;
            case REEL_EEPROM_DATA:
                ReelEEPROMDataResponse reelResponse = (ReelEEPROMDataResponse) rxPacket;

                //TODO modify to work with multiple reels
                reels.get(0).updateFromEEPROMData(reelResponse);
                break;
            case HEAD_EEPROM_DATA:
                HeadEEPROMDataResponse headResponse = (HeadEEPROMDataResponse) rxPacket;
                head.updateFromEEPROMData(headResponse);
                break;
            default:
                steno.warning("Unknown packet type delivered to Printer Status: "
                    + rxPacket.getPacketType().name());
                break;
        }
    }

    public void hardResetHead()
    {
        if (head.headEEPROMStatusProperty().get() == EEPROMState.NOT_PROGRAMMED)
        {
            try
            {
                transmitFormatHeadEEPROM();
            } catch (RoboxCommsException ex)
            {
                steno.error("Error formatting head");
            }
        }

        try
        {
            HeadEEPROMDataResponse response = transmitReadHeadEEPROM();

            if (response != null)
            {
                String receivedTypeCode = response.getTypeCode();

                HeadFile referenceHeadData = null;
                if (receivedTypeCode != null)
                {
                    referenceHeadData = HeadContainer.getHeadByID(response.getTypeCode());
                }

                if (referenceHeadData != null)
                {
                    Head headToWrite = new Head(referenceHeadData);
                    headToWrite.setUniqueID(response.getUniqueID());
                    headToWrite.setHeadHours(response.getHeadHours());

                    // For now we only have one last filament temp from the printer...
                    head.getNozzleHeaters().get(0).lastFilamentTemperature.set(response.getLastFilamentTemperature());
                    transmitWriteHeadEEPROM(headToWrite);
                    transmitReadHeadEEPROM();

                    steno.info("Updated head data at user request for " + receivedTypeCode);
                    Lookup.getSystemNotificationHandler().showCalibrationDialogue();
                } else
                {
                    Head headToWrite = new Head(HeadContainer.getHeadByID(HeadContainer.defaultHeadID));
                    String typeCode = headToWrite.getTypeCode();
                    String idToCreate = typeCode + SystemUtils.generate16DigitID().substring(typeCode.length());
                    headToWrite.setUniqueID(idToCreate);

                    transmitWriteHeadEEPROM(headToWrite);
                    transmitReadHeadEEPROM();
                    steno.info("Updated head data at user request - type code could not be determined");
                    Lookup.getSystemNotificationHandler().showCalibrationDialogue();
                }
            } else
            {
                steno.warning("Request to hard reset head failed");
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Error during hard reset of head");
        }
    }

    public void repairHeadIfNecessary()
    {
        try
        {
            HeadEEPROMDataResponse response = transmitReadHeadEEPROM();

            if (ApplicationConfiguration.isAutoRepairHeads())
            {
                if (response != null)
                {
                    head.updateFromEEPROMData(response);

                    String receivedTypeCode = response.getTypeCode();

                    HeadRepairResult result = head.repair(receivedTypeCode);

                    switch (result)
                    {
                        case REPAIRED_WRITE_ONLY:
                            transmitWriteHeadEEPROM(head);
                            steno.info("Automatically updated head data for " + receivedTypeCode);
                            Lookup.getSystemNotificationHandler().showHeadUpdatedNotification();
                            break;
                        case REPAIRED_WRITE_AND_RECALIBRATE:
                            transmitWriteHeadEEPROM(head);
                            Lookup.getSystemNotificationHandler().showCalibrationDialogue();
                            steno.info("Automatically updated head data for " + receivedTypeCode + " calibration suggested");
                            break;
                    }
                }
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Error from triggered read of Head EEPROM");
        }
    }

    public void openLid()
    {
        try
        {
            transmitDirectGCode(GCodeConstants.goToOpenLidPosition,
                                false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when moving sending open lid command");
        }
    }

    public void updatePrinterName(String chosenPrinterName) throws PrinterException
    {
        WritePrinterID writeIDCmd = (WritePrinterID) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.WRITE_PRINTER_ID);

        PrinterIdentity newIdentity = printerIdentity.clone();
        newIdentity.printerFriendlyNameProperty.set(chosenPrinterName);
        writeIDCmd.populatePacket(newIdentity);
        try
        {
            AckResponse response = (AckResponse) commandInterface.writeToPrinter(writeIDCmd);
        } catch (RoboxCommsException ex)
        {
            steno.error("Comms exception whilst writing printer name " + ex.getMessage());
            throw new PrinterException("Failed to write name to printer");
        }
    }

    public void updatePrinterDisplayColour(Color displayColour) throws PrinterException
    {
        WritePrinterID writeIDCmd = (WritePrinterID) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.WRITE_PRINTER_ID);

        PrinterIdentity newIdentity = printerIdentity.clone();
        newIdentity.printerColourProperty.set(displayColour);
        writeIDCmd.populatePacket(newIdentity);

        try
        {
            AckResponse response = (AckResponse) commandInterface.writeToPrinter(writeIDCmd);
        } catch (RoboxCommsException ex)
        {
            steno.error("Comms exception whilst writing printer colour " + ex.getMessage());
            throw new PrinterException("Failed to write colour to printer");
        }
    }

    public void goToTargetBedTemperature()
    {
        try
        {
            transmitDirectGCode(GCodeConstants.goToTargetBedTemperature, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending go to target bed temperature command");
        }
    }

    public void switchBedHeaterOff()
    {
        try
        {
            transmitDirectGCode(GCodeConstants.switchBedHeaterOff, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending switch bed heater off command");
        }
    }

    public void goToTargetNozzleTemperature()
    {
        try
        {
            transmitDirectGCode(GCodeConstants.goToTargetNozzleTemperature, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending go to target nozzle temperature command");
        }
    }

    public void switchNozzleHeaterOff(int heaterNumber)
    {
        try
        {
            transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending switch nozzle heater off command");
        }
    }

    public void setNozzleFirstLayerTargetTemperature(int targetTemperature)
    {
        try
        {
            transmitDirectGCode(GCodeConstants.setFirstLayerNozzleTemperatureTarget + targetTemperature, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending set nozzle first layer temperature command");
        }
    }

    public void setNozzleTargetTemperature(int targetTemperature)
    {
        try
        {
            transmitDirectGCode(GCodeConstants.setNozzleTemperatureTarget + targetTemperature, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending set nozzle temperature command");
        }
    }

    public void setAmbientTemperature(int targetTemperature)
    {
        try
        {
            transmitDirectGCode(GCodeConstants.setAmbientTemperature + targetTemperature, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending set ambient temperature command");
        }
    }

    public void setBedFirstLayerTargetTemperature(int targetTemperature)
    {
        try
        {
            transmitDirectGCode(GCodeConstants.setFirstLayerBedTemperatureTarget + targetTemperature, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending set bed first layer target temperature command");
        }
    }

    public void setBedTargetTemperature(int targetTemperature)
    {
        try
        {
            transmitDirectGCode(GCodeConstants.setBedTemperatureTarget + targetTemperature, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending set bed target temperature command");
        }
    }

    public void sendRawGCode(String gCode, boolean addToTranscript)
    {
        try
        {
            transmitDirectGCode(gCode, addToTranscript);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending raw gcode : " + gCode);
        }
    }
}
