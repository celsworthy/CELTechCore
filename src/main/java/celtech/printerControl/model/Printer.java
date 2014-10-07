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
import static celtech.printerControl.comms.commands.rx.RxPacketTypeEnum.ACK_WITH_ERRORS;
import static celtech.printerControl.comms.commands.rx.RxPacketTypeEnum.FIRMWARE_RESPONSE;
import static celtech.printerControl.comms.commands.rx.RxPacketTypeEnum.HEAD_EEPROM_DATA;
import static celtech.printerControl.comms.commands.rx.RxPacketTypeEnum.PRINTER_ID_RESPONSE;
import static celtech.printerControl.comms.commands.rx.RxPacketTypeEnum.REEL_EEPROM_DATA;
import static celtech.printerControl.comms.commands.rx.RxPacketTypeEnum.STATUS_RESPONSE;
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
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import celtech.utils.PrinterUtils;
import celtech.utils.SystemUtils;
import celtech.utils.tasks.Cancellable;
import celtech.utils.tasks.TaskResponder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class Printer implements RoboxResponseConsumer
{

    private final Stenographer steno = StenographerFactory.getStenographer(Printer.class.getName());

    protected final ObjectProperty<PrinterStatus> printerStatus = new SimpleObjectProperty(PrinterStatus.IDLE);
    private PrinterStatus lastStateBeforePause = null;

    protected PrinterStatusConsumer printerStatusConsumer;
    protected CommandInterface commandInterface;

    private SystemNotificationManager systemNotificationManager;

    private NumberFormat threeDPformatter;

    /*
     * State machine data
     */
    private final BooleanProperty canRemoveHeadProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty canPurgeHeadProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty mustPurgeHeadProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty canPrintProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty canPauseProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty canResumeProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty canRunMacroProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty canCancelProperty = new SimpleBooleanProperty(false);

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
    private final ObjectProperty<Head> head = new SimpleObjectProperty<>(null);
    private final ObservableList<Reel> reels = FXCollections.observableArrayList();
    private final ObservableList<Extruder> extruders = FXCollections.observableArrayList();

    /*
     * Temperature-related data
     */
    private long lastTimestamp = System.currentTimeMillis();

    private final ObservableList<String> gcodeTranscript = FXCollections.observableArrayList();

    /*
     * Data used for data chunk management
     */
    private int dataFileSequenceNumber = 0;
    private static final int bufferSize = 512;
    private static final StringBuffer outputBuffer = new StringBuffer(bufferSize);
    private boolean printInitiated = false;

    protected final ObjectProperty<PauseStatus> pauseStatusProperty = new SimpleObjectProperty<>(PauseStatus.NOT_PAUSED);
    protected final IntegerProperty printJobLineNumberProperty = new SimpleIntegerProperty(0);
    protected final StringProperty printJobIDProperty = new SimpleStringProperty("");

    private PrintEngine printEngine;

    public Printer(PrinterStatusConsumer printerStatusConsumer, CommandInterface commandInterface)
    {
        this.printerStatusConsumer = printerStatusConsumer;
        this.commandInterface = commandInterface;

        printEngine = new PrintEngine(this);

        threeDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        threeDPformatter.setMaximumFractionDigits(3);
        threeDPformatter.setGroupingUsed(false);

        systemNotificationManager = Lookup.getSystemNotificationHandler();

        setPrinterStatus(PrinterStatus.IDLE);
    }

    protected final void setPrinterStatus(PrinterStatus printerStatus)
    {

        // Can print should rely on filament loaded etc
//                            printerOKToPrint.bind(newValue.printerStatus().isEqualTo(
//                        PrinterStatus.IDLE)
//                        .and(newValue.whyAreWeWaitingProperty().isEqualTo(
//                                WhyAreWeWaitingState.NOT_WAITING))
//                        .and(newValue.headEEPROMStatusProperty().isEqualTo(EEPROMState.PROGRAMMED))
//                        .and((newValue.Filament1LoadedProperty().or(
//                                newValue.Filament2LoadedProperty())))
//                        .and(settingsScreenState.filamentProperty().isNotNull().or(
//                                newValue.loadedFilamentProperty().isNotNull())));
        switch (printerStatus)
        {
            case IDLE:
                lastStateBeforePause = null;
                canRemoveHeadProperty.set(true);
                canPurgeHeadProperty.set(true);
                canPrintProperty.set(true);
                canPauseProperty.set(false);
                canResumeProperty.set(false);
                canRunMacroProperty.set(true);
                canCancelProperty.set(false);
                printEngine.goToIdle();
                break;
            case REMOVING_HEAD:
                canRemoveHeadProperty.set(false);
                canPurgeHeadProperty.set(false);
                canPrintProperty.set(false);
                canPauseProperty.set(false);
                canResumeProperty.set(false);
                canRunMacroProperty.set(false);
                canCancelProperty.set(false);
                break;
            case PURGING_HEAD:
                canRemoveHeadProperty.set(false);
                canPurgeHeadProperty.set(false);
                canPrintProperty.set(false);
                canPauseProperty.set(false);
                canResumeProperty.set(false);
                canRunMacroProperty.set(false);
                canCancelProperty.set(false);
                break;
            case SLICING:
                canRemoveHeadProperty.set(false);
                canPurgeHeadProperty.set(false);
                canPrintProperty.set(false);
                canPauseProperty.set(false);
                canResumeProperty.set(false);
                canRunMacroProperty.set(false);
                canCancelProperty.set(false);
                printEngine.goToSlicing();
                break;
            case POST_PROCESSING:
                canRemoveHeadProperty.set(false);
                canPurgeHeadProperty.set(false);
                canPrintProperty.set(false);
                canPauseProperty.set(false);
                canResumeProperty.set(false);
                canRunMacroProperty.set(false);
                canCancelProperty.set(false);
                printEngine.goToPostProcessing();
                break;
            case SENDING_TO_PRINTER:
                canRemoveHeadProperty.set(false);
                canPurgeHeadProperty.set(false);
                canPrintProperty.set(false);
                canPauseProperty.set(false);
                canResumeProperty.set(false);
                canRunMacroProperty.set(false);
                canCancelProperty.set(false);
                printEngine.goToSendingToPrinter();
                break;
            case PAUSED:
                canRemoveHeadProperty.set(false);
                canPurgeHeadProperty.set(false);
                canPrintProperty.set(false);
                canPauseProperty.set(false);
                canResumeProperty.set(true);
                canRunMacroProperty.set(false);
                canCancelProperty.set(false);
                printEngine.goToPause();
                break;
            case PRINTING:
                canRemoveHeadProperty.set(false);
                canPurgeHeadProperty.set(false);
                canPrintProperty.set(false);
                canPauseProperty.set(false);
                canResumeProperty.set(false);
                canRunMacroProperty.set(false);
                canCancelProperty.set(true);
                printEngine.goToPrinting();
                break;
            case EXECUTING_MACRO:
                printEngine.goToPrinting();
                break;
        }
        this.printerStatus.set(printerStatus);
    }

    public ReadOnlyObjectProperty<PrinterStatus> printerStatusProperty()
    {
        return printerStatus;
    }

    public PrinterIdentity getPrinterIdentity()
    {
        return printerIdentity;
    }

    public PrinterAncillarySystems getPrinterAncillarySystems()
    {
        return printerAncillarySystems;
    }

    public ReadOnlyObjectProperty<Head> headProperty()
    {
        return head;
    }

    public ObservableList<Reel> reelsProperty()
    {
        return reels;
    }

    public ObservableList<Extruder> extrudersProperty()
    {
        return extruders;
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
        if (!canRemoveHeadProperty.get())
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
            for (int extruderIndex = 0; extruderIndex < extruders.size(); extruderIndex++)
            {
                if (extruders.get(extruderIndex).canEject.get())
                {
                    ejectFilament(extruderIndex);
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
        if (!canPurgeHeadProperty.get())
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
            headDataPrePurge = readHeadEEPROM();

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
        if (printerStatus.get() != PrinterStatus.PURGING_HEAD)
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

    public void cancel(TaskResponder responder) throws PrinterException
    {
        if (!canCancelProperty.get())
        {
            throw new PrinterException("Cancel not permitted");
        }

        setPrinterStatus(PrinterStatus.CANCELLING);

        final Cancellable cancellable = new Cancellable();

        new Thread(() ->
        {
            boolean success = doAbortActivity(cancellable);

            Lookup.getTaskExecutor().runOnGUIThread(responder, success, "Abort complete");

            setPrinterStatus(PrinterStatus.IDLE);

        }, "Aborting").start();
    }

    public boolean doAbortActivity(Cancellable cancellable)
    {
        boolean success = false;

        printEngine.stopAllServices();

        switchBedHeaterOff();
        switchAllNozzleHeatersOff();

        RoboxTxPacket abortPacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.ABORT_PRINT);
        try
        {
            AckResponse response = (AckResponse) commandInterface.writeToPrinter(abortPacket);
        } catch (RoboxCommsException ex)
        {
            steno.error("Couldn't send abort command to printer");
        }
        PrinterUtils.waitOnBusy(this, cancellable);
        try
        {
            runMacro("abort_print");
            PrinterUtils.waitOnMacroFinished(this, cancellable);
            success = true;
        } catch (PrinterException ex)
        {
            steno.error("Error during abort sequence " + ex.getMessage());
        }

        return success;
    }

    /**
     *
     * @return
     */
    public final ReadOnlyBooleanProperty getCanPauseProperty()
    {
        return canPauseProperty;
    }

    public void pause() throws PrinterException
    {
        if (!canPauseProperty.get())
        {
            throw new PrintActionUnavailableException("Cannot pause at this time");
        }

        lastStateBeforePause = printerStatus.get();

        setPrinterStatus(PrinterStatus.PAUSING);

        try
        {
            PausePrint gcodePacket = (PausePrint) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.PAUSE_RESUME_PRINT);
            gcodePacket.setPause();

            commandInterface.writeToPrinter(gcodePacket);
        } catch (RoboxCommsException ex)
        {
            steno.error(
                "Robox comms exception when sending resume print command "
                + ex);
        }
    }

    /**
     *
     * @return
     */
    public final ReadOnlyBooleanProperty getCanResumeProperty()
    {
        return canResumeProperty;
    }

    /**
     *
     * @throws PrinterException
     */
    public void resume() throws PrinterException
    {
        if (!canResumeProperty.get())
        {
            throw new PrintActionUnavailableException("Cannot resume at this time");
        }

        setPrinterStatus(PrinterStatus.RESUMING);

        try
        {
            PausePrint gcodePacket = (PausePrint) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.PAUSE_RESUME_PRINT);
            gcodePacket.setResume();

            commandInterface.writeToPrinter(gcodePacket);
        } catch (RoboxCommsException ex)
        {
            steno.error(
                "Robox comms exception when sending resume print command "
                + ex);
        }
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

        boolean jobAccepted = printEngine.printGCodeFile(GCodeMacros.getFilename(macroName), true);

        if (!jobAccepted)
        {
            throw new PrintJobRejectedException("Macro " + macroName + " could not be run");
        }
    }

//    /**
//     *
//     * @param macroName
//     * @param checkForPurge
//     * @throws celtech.printerControl.model.PrinterException
//     */
//    public void runMacro(final String macroName, boolean checkForPurge) throws PrinterException
//    {
//        if (!canPrintProperty.get())
//        {
//            throw new PrintActionUnavailableException("Cannot print at this time");
//        }
//        if (checkForPurge)
//        {
//            boolean purgeConsent = PrinterUtils.getInstance().offerPurgeIfNecessary(this);
//
//            if (purgeConsent)
//            {
//                DisplayManager.getInstance().getPurgeInsetPanelController().purgeAndRunMacro(macroName, this);
//            } else
//            {
//                printEngine.printGCodeFile(GCodeMacros.getFilename(macroName), true);
//            }
//        } else
//        {
//            printEngine.printGCodeFile(GCodeMacros.getFilename(macroName), true);
//        }
//    }
    public final void runMacroWithoutPurgeCheck(String macroName) throws PrinterException
    {
        if (!canRunMacroProperty.get())
        {
            throw new PrintActionUnavailableException("Run macro not available");
        }

        boolean jobAccepted = printEngine.printGCodeFile(GCodeMacros.getFilename(macroName), true);

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

        printEngine.printProject(project, printQuality, settings);
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

                /*
                 * Ancillary systems
                 */
                printerAncillarySystems.ambientTemperature.set(statusResponse.getAmbientTemperature());
                printerAncillarySystems.ambientTargetTemperature.set(statusResponse.getAmbientTargetTemperature());
                printerAncillarySystems.bedTemperature.set(statusResponse.getBedTemperature());
                printerAncillarySystems.bedTargetTemperature.set(statusResponse.getBedTargetTemperature());
                printerAncillarySystems.bedFirstLayerTargetTemperature.set(statusResponse.getBedFirstLayerTargetTemperature());
                printerAncillarySystems.ambientFanOn.set(statusResponse.isAmbientFanOn());
                printerAncillarySystems.bedHeaterMode.set(statusResponse.getBedHeaterMode());
                printerAncillarySystems.headFanOn.set(statusResponse.isHeadFanOn());
                printerAncillarySystems.XStopSwitch.set(statusResponse.isxSwitchStatus());
                printerAncillarySystems.YStopSwitch.set(statusResponse.isySwitchStatus());
                printerAncillarySystems.ZStopSwitch.set(statusResponse.iszSwitchStatus());
                printerAncillarySystems.ZTopStopSwitch.set(statusResponse.isTopZSwitchStatus());
                printerAncillarySystems.bAxisHome.set(statusResponse.isNozzleSwitchStatus());
                printerAncillarySystems.lidOpen.set(statusResponse.isLidSwitchStatus());
                printerAncillarySystems.reelButton.set(statusResponse.isReelButtonStatus());
                printerAncillarySystems.whyAreWeWaitingProperty.set(statusResponse.getWhyAreWeWaitingState());
                printerAncillarySystems.updateGraphData();

                /*
                 * Heater
                 */
                //TODO modify for multiple heaters
                head.get().nozzleHeaters.get(0).nozzleTemperature.set(statusResponse.getNozzleTemperature());
                head.get().nozzleHeaters.get(0).nozzleFirstLayerTargetTemperature.set(statusResponse.getNozzleFirstLayerTargetTemperature());
                setNozzleTargetTemperature(statusResponse.getNozzleTargetTemperature());
                //TODO modify for multiple heaters
                if (head.get().getNozzleHeaters().size() > 0)
                {
                    head.get().getNozzleHeaters().get(0).heaterMode.set(statusResponse.getNozzleHeaterMode());
                }
                head.get().nozzleHeaters
                    .stream()
                    .forEach(heater -> heater.updateGraphData());

                /*
                 * Extruders
                 */
                //TODO configure properly for multiple extruders
                extruders.get(0).filamentLoaded.set(statusResponse.isFilament1SwitchStatus());
                extruders.get(0).indexWheelState.set(statusResponse.isEIndexStatus());
                extruders.get(1).filamentLoaded.set(statusResponse.isFilament2SwitchStatus());
                extruders.get(1).indexWheelState.set(statusResponse.isDIndexStatus());

                if (pauseStatusProperty.get() != statusResponse.getPauseStatus()
                    && statusResponse.getPauseStatus() == PauseStatus.PAUSED)
                {
                    lastStateBeforePause = printerStatus.get();
                    setPrinterStatus(PrinterStatus.PAUSED);
                } else if (pauseStatusProperty.get() != statusResponse.getPauseStatus()
                    && statusResponse.getPauseStatus() == PauseStatus.NOT_PAUSED)
                {
                    if (lastStateBeforePause != null)
                    {
                        setPrinterStatus(lastStateBeforePause);
                    }
                }

                /*
                 * Generic printer stuff that has no other home :)
                 */
                pauseStatusProperty.set(statusResponse.getPauseStatus());
                printJobLineNumberProperty.set(statusResponse.getPrintJobLineNumber());
                printJobIDProperty.set(statusResponse.getRunningPrintJobID());

                /*
                 * Reels
                 */
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

                //TODO modify to work with multiple reels
                reels.get(0).reelEEPROMStatusProperty.set(statusResponse.getReelEEPROMState());

                /*
                 * Core head data
                 */
                EEPROMState lastHeadState = head.get().headEEPROMStatusProperty.get();

                if (lastHeadState != EEPROMState.PROGRAMMED
                    && statusResponse.getHeadEEPROMState() == EEPROMState.PROGRAMMED)
                {
                    head.get().repair(null);
                } else if (lastHeadState != EEPROMState.NOT_PRESENT
                    && statusResponse.getHeadEEPROMState() == EEPROMState.NOT_PRESENT)
                {
                    head.get().noHeadAttached();
                }

                head.get().headEEPROMStatusProperty.set(statusResponse.getHeadEEPROMState());

                head.get().headXPosition.set(statusResponse.getHeadXPosition());
                head.get().headYPosition.set(statusResponse.getHeadYPosition());
                head.get().headZPosition.set(statusResponse.getHeadZPosition());

                /*
                 * Nozzle data
                 */
                //TODO modify to work with multiple nozzles
                //This is only true for the current cam-based heads that only really have one B axis
                head.get().nozzles
                    .stream()
                    .forEach(nozzle -> nozzle.BPosition.set(statusResponse.getBPosition()));

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
                head.get().updateFromEEPROMData(headResponse);
                break;

            default:
                steno.warning("Unknown packet type delivered to Printer Status: "
                    + rxPacket.getPacketType().name());
                break;
        }
    }

    public void hardResetHead()
    {
        if (head.get().headEEPROMStatusProperty.get() == EEPROMState.NOT_PROGRAMMED)
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
            HeadEEPROMDataResponse response = readHeadEEPROM();

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
                    headToWrite.uniqueID.set(response.getUniqueID());
                    headToWrite.headHours.set(response.getHeadHours());

                    // For now we only have one last filament temp from the printer...
                    head.get().getNozzleHeaters().get(0).lastFilamentTemperature.set(response.getLastFilamentTemperature());
                    writeHeadEEPROM(headToWrite);
                    readHeadEEPROM();

                    steno.info("Updated head data at user request for " + receivedTypeCode);
                    Lookup.getSystemNotificationHandler().showCalibrationDialogue();
                } else
                {
                    Head headToWrite = new Head(HeadContainer.getHeadByID(HeadContainer.defaultHeadID));
                    String typeCode = headToWrite.typeCode.get();
                    String idToCreate = typeCode + SystemUtils.generate16DigitID().substring(typeCode.length());
                    headToWrite.uniqueID.set(idToCreate);

                    writeHeadEEPROM(headToWrite);
                    readHeadEEPROM();
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
            HeadEEPROMDataResponse response = readHeadEEPROM();

            if (ApplicationConfiguration.isAutoRepairHeads())
            {
                if (response != null)
                {
                    head.get().updateFromEEPROMData(response);

                    String receivedTypeCode = response.getTypeCode();

                    HeadRepairResult result = head.get().repair(receivedTypeCode);

                    switch (result)
                    {
                        case REPAIRED_WRITE_ONLY:
                            writeHeadEEPROM(head.get());
                            steno.info("Automatically updated head data for " + receivedTypeCode);
                            Lookup.getSystemNotificationHandler().showHeadUpdatedNotification();
                            break;
                        case REPAIRED_WRITE_AND_RECALIBRATE:
                            writeHeadEEPROM(head.get());
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

    public void changeNozzlePosition(float position)
    {
        try
        {
            transmitDirectGCode("G0 B" + threeDPformatter.format(position), false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending close nozzle command");
        }
    }

    public void switchOffHeadLEDs()
    {
        try
        {
            transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending switch head LED off command");
        }
    }

    public void switchAllNozzleHeatersOff()
    {
        //TODO modify for multiple heaters
        switchNozzleHeaterOff(0);
    }

    public void switchNozzleHeaterOff(int heaterNumber)
    {
        //TODO modify for multiple heaters
        try
        {
            transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending switch nozzle heater off command");
        }
    }

    public void homeZ()
    {
        try
        {
            transmitDirectGCode("G28 Z", false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending z home command");
        }
    }

    public void goToZPosition(double position)
    {
        try
        {
            transmitDirectGCode("G0 Z" + threeDPformatter.format(position), false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending z position command");
        }
    }

    public void switchToAbsoluteMoveMode()
    {
        try
        {
            transmitDirectGCode("G90", false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending change to absolute move command");
        }
    }

    public void switchToRelativeMoveMode()
    {
        try
        {
            transmitDirectGCode("G91", false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending change to relative move command");
        }
    }

    public void forceHeadReset()
    {
        head.get().repair(null);
    }

    /**
     *
     * @param headToWrite
     * @throws RoboxCommsException
     */
    public void writeHeadEEPROM(Head headToWrite) throws RoboxCommsException
    {
        WriteHeadEEPROM writeHeadEEPROM = (WriteHeadEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.WRITE_HEAD_EEPROM);
        writeHeadEEPROM.populateEEPROM(headToWrite);
        commandInterface.writeToPrinter(writeHeadEEPROM);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    public HeadEEPROMDataResponse readHeadEEPROM() throws RoboxCommsException
    {
        ReadHeadEEPROM readHead = (ReadHeadEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.READ_HEAD_EEPROM);
        return (HeadEEPROMDataResponse) commandInterface.writeToPrinter(readHead);
    }

    public void ejectFilament(int extruderNumber, TaskResponder responder) throws PrinterException
    {
        if (extruderNumber >= extruders.size())
        {
            throw new PrintActionUnavailableException("Extruder " + extruderNumber + " is not present");
        }

        if (!extruders.get(extruderNumber).canEject.get())
        {
            throw new PrintActionUnavailableException("Eject is not available for extruder " + extruderNumber);
        }

        setPrinterStatus(PrinterStatus.EJECTING_FILAMENT);

        final Cancellable cancellable = new Cancellable();

        new Thread(() ->
        {
            boolean success = doEjectFilamentActivity(extruderNumber, cancellable);

            Lookup.getTaskExecutor().runOnGUIThread(responder, success, "Filament ejected");

            setPrinterStatus(PrinterStatus.IDLE);

        }, "Ejecting filament").start();

    }

    private boolean doEjectFilamentActivity(int extruderNumber, Cancellable cancellable)
    {
        boolean success = false;
        try
        {
            transmitDirectGCode(GCodeConstants.ejectFilament + " " + extruders.get(extruderNumber).getExtruderAxisLetter(), false);
            PrinterUtils.waitOnBusy(this, cancellable);
            success = true;
        } catch (RoboxCommsException ex)
        {
            steno.error("Error whilst ejecting filament");
        }

        return success;
    }

}
