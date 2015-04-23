package celtech.printerControl.model;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.appManager.SystemNotificationManager;
import celtech.configuration.BusyStatus;
import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.MaterialType;
import celtech.configuration.PauseStatus;
import celtech.configuration.PrinterEdition;
import celtech.configuration.PrinterModel;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.printerControl.MacroType;
import celtech.printerControl.PrintActionUnavailableException;
import celtech.printerControl.PrintJobRejectedException;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.PurgeRequiredException;
import celtech.printerControl.comms.CommandInterface;
import celtech.printerControl.comms.PrinterStatusConsumer;
import celtech.printerControl.comms.commands.MacroPrintException;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.DebugDataResponse;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.comms.commands.rx.GCodeDataResponse;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.HoursCounterResponse;
import celtech.printerControl.comms.commands.rx.ListFilesResponse;
import celtech.printerControl.comms.commands.rx.PrinterIDResponse;
import celtech.printerControl.comms.commands.rx.ReelEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import static celtech.printerControl.comms.commands.rx.RxPacketTypeEnum.ACK_WITH_ERRORS;
import static celtech.printerControl.comms.commands.rx.RxPacketTypeEnum.FIRMWARE_RESPONSE;
import static celtech.printerControl.comms.commands.rx.RxPacketTypeEnum.HEAD_EEPROM_DATA;
import static celtech.printerControl.comms.commands.rx.RxPacketTypeEnum.PRINTER_ID_RESPONSE;
import static celtech.printerControl.comms.commands.rx.RxPacketTypeEnum.STATUS_RESPONSE;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.comms.commands.tx.FormatHeadEEPROM;
import celtech.printerControl.comms.commands.tx.ListFiles;
import celtech.printerControl.comms.commands.tx.PausePrint;
import celtech.printerControl.comms.commands.tx.QueryFirmwareVersion;
import celtech.printerControl.comms.commands.tx.ReadHeadEEPROM;
import celtech.printerControl.comms.commands.tx.ReadPrinterID;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacketFactory;
import celtech.printerControl.comms.commands.tx.SetAmbientLEDColour;
import celtech.printerControl.comms.commands.tx.SetDFilamentInfo;
import celtech.printerControl.comms.commands.tx.SetEFilamentInfo;
import celtech.printerControl.comms.commands.tx.SetFeedRateMultiplier;
import celtech.printerControl.comms.commands.tx.SetReelLEDColour;
import celtech.printerControl.comms.commands.tx.SetTemperatures;
import celtech.printerControl.comms.commands.tx.StatusRequest;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;
import celtech.printerControl.comms.commands.tx.WriteHeadEEPROM;
import celtech.printerControl.comms.commands.tx.WritePrinterID;
import celtech.printerControl.comms.commands.tx.WriteReel0EEPROM;
import celtech.printerControl.comms.commands.tx.WriteReel1EEPROM;
import celtech.printerControl.comms.events.ErrorConsumer;
import celtech.printerControl.model.calibration.NozzleHeightStateTransitionManager;
import celtech.printerControl.model.calibration.NozzleOpeningStateTransitionManager;
import celtech.printerControl.model.calibration.XAndYStateTransitionManager;
import celtech.printerControl.model.calibration.CalibrationNozzleHeightTransitions;
import celtech.printerControl.model.calibration.CalibrationNozzleOpeningTransitions;
import celtech.printerControl.model.calibration.CalibrationXAndYTransitions;
import celtech.services.printing.DatafileSendAlreadyInProgress;
import celtech.services.printing.DatafileSendNotInitialised;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.utils.AxisSpecifier;
import celtech.utils.Math.MathUtils;
import celtech.utils.PrinterUtils;
import celtech.utils.SystemUtils;
import celtech.utils.tasks.Cancellable;
import celtech.utils.tasks.SimpleCancellable;
import celtech.utils.tasks.TaskResponder;
import celtech.utils.tasks.TaskResponse;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;
import libertysystems.stenographer.LogLevel;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public final class HardwarePrinter implements Printer, ErrorConsumer
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        HardwarePrinter.class.getName());
    private final FilamentContainer filamentContainer = Lookup.getFilamentContainer();

    protected final ObjectProperty<PrinterStatus> printerStatus = new SimpleObjectProperty(
        PrinterStatus.IDLE);
    private final PrinterMetaStatus metaStatus;
    protected ObjectProperty<MacroType> macroType = new SimpleObjectProperty<>(null);
    protected BooleanProperty macroIsInterruptible = new SimpleBooleanProperty(false);

    private PrinterStatus lastStateBeforePause = null;
    private PrinterStatus lastStateBeforeLoadUnload = null;

    protected PrinterStatusConsumer printerStatusConsumer;
    protected CommandInterface commandInterface;

    private SystemNotificationManager systemNotificationManager;

    private NumberFormat threeDPformatter;

    private final float safeBedTemperatureForOpeningDoor = 60f;

    private final float MIN_FEEDRATE_MULTIPLIER = 0.5f;
    private final float MAX_FEEDRATE_MULTIPLIER = 1.5f;
    private boolean moderatingFeedrate = false;
    private float originalFeedrateMultiplier = 1.0f;

    /*
     * State machine data
     */
    private final BooleanProperty canRemoveHead = new SimpleBooleanProperty(false);
    private final BooleanProperty canPurgeHead = new SimpleBooleanProperty(false);
    private final BooleanProperty mustPurgeHead = new SimpleBooleanProperty(false);
    private final BooleanProperty canPrint = new SimpleBooleanProperty(false);
    private final BooleanProperty canOpenCloseNozzle = new SimpleBooleanProperty(false);
    private final BooleanProperty canPause = new SimpleBooleanProperty(false);
    private final BooleanProperty canResume = new SimpleBooleanProperty(false);
    private final BooleanProperty canRunMacro = new SimpleBooleanProperty(false);
    private final BooleanProperty canCancel = new SimpleBooleanProperty(false);
    private final BooleanProperty canOpenDoor = new SimpleBooleanProperty(false);
    private final BooleanProperty canCalibrateHead = new SimpleBooleanProperty(false);
    private final BooleanProperty canCalibrateNozzleHeight = new SimpleBooleanProperty(false);
    private final BooleanProperty canCalibrateXYAlignment = new SimpleBooleanProperty(false);
    private final BooleanProperty canCalibrateNozzleOpening = new SimpleBooleanProperty(false);

    private boolean headIntegrityChecksInhibited = false;

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
    private final ObservableMap<Integer, Reel> reels = FXCollections.observableHashMap();
    private final ObservableList<Extruder> extruders = FXCollections.observableArrayList();

    private EEPROMState lastHeadEEPROMState = null;
    private final int maxNumberOfReels = 2;
    private EEPROMState[] lastReelEEPROMState = new EEPROMState[maxNumberOfReels];

    /*
     * Temperature-related data
     */
    private long lastTimestamp = System.currentTimeMillis();

    private final ObservableList<String> gcodeTranscript = FXCollections.observableArrayList();

    /*
     * Data used for data chunk management
     */
    private int dataFileSequenceNumber = 0;
    private int dataFileSequenceNumberStartPoint = 0;
    private static final int bufferSize = 512;
    private static final StringBuffer outputBuffer = new StringBuffer(bufferSize);
    private boolean printInitiated = false;

    protected final ObjectProperty<PauseStatus> pauseStatus = new SimpleObjectProperty<>(
        PauseStatus.NOT_PAUSED);
    protected final ObjectProperty<BusyStatus> busyStatus = new SimpleObjectProperty<>(
        BusyStatus.NOT_BUSY);
    protected final IntegerProperty printJobLineNumber = new SimpleIntegerProperty(0);
    protected final StringProperty printJobID = new SimpleStringProperty("");

    private PrintEngine printEngine;

    private final String firstExtruderLetter = "E";
    private final int firstExtruderNumber = 0;
    private final String secondExtruderLetter = "D";
    private final int secondExtruderNumber = 1;

    private boolean suppressEEPROMAndSDErrorHandling = false;

    /*
     * Error handling
     */
    private final Map<ErrorConsumer, List<FirmwareError>> errorConsumers = new WeakHashMap<>();
    private boolean processErrors = false;
    private final FilamentLoadedGetter filamentLoadedGetter;

    @Override
    public PrinterMetaStatus getPrinterMetaStatus()
    {
        return metaStatus;
    }

    /**
     * A FilamentLoadedGetter can be provided to the HardwarePrinter to provide a way to override
     * the detection of whether a filament is loaded or not on a given extruder.
     */
    public interface FilamentLoadedGetter
    {

        public boolean getFilamentLoaded(StatusResponse statusResponse, int extruderNumber);
    }

    public HardwarePrinter(PrinterStatusConsumer printerStatusConsumer,
        CommandInterface commandInterface)
    {
        // The default FilamentLoadedGetter just checks the data in the statusResponse
        this(printerStatusConsumer, commandInterface,
             (StatusResponse statusResponse, int extruderNumber) ->
             {
                 if (extruderNumber == 1)
                 {
                     return statusResponse.isFilament1SwitchStatus();
                 } else
                 {
                     return statusResponse.isFilament2SwitchStatus();
                 }
             });
    }

    public HardwarePrinter(PrinterStatusConsumer printerStatusConsumer,
        CommandInterface commandInterface, FilamentLoadedGetter filamentLoadedGetter)
    {
        this.printerStatusConsumer = printerStatusConsumer;
        this.commandInterface = commandInterface;
        this.filamentLoadedGetter = filamentLoadedGetter;

        metaStatus = new PrinterMetaStatus(this);

        macroType.addListener(
            (ObservableValue<? extends MacroType> observable, MacroType oldValue, MacroType newValue) ->
            {
                if (newValue != null)
                {
                    macroIsInterruptible.set(newValue.isInterruptible());
                } else
                {
                    macroIsInterruptible.set(false);
                }
            });

        extruders.add(firstExtruderNumber, new Extruder(firstExtruderLetter));
        extruders.add(secondExtruderNumber, new Extruder(secondExtruderLetter));

        setupBindings();
        setupFilamentDatabaseChangeListeners();

        threeDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        threeDPformatter.setMaximumFractionDigits(3);
        threeDPformatter.setGroupingUsed(false);

        systemNotificationManager = Lookup.getSystemNotificationHandler();

        printEngine = new PrintEngine(this);
        setPrinterStatus(PrinterStatus.IDLE);

        commandInterface.setPrinter(this);
        commandInterface.start();
    }

    private void setupBindings()
    {
        extruders.stream().forEach(extruder -> extruder.canEject
            .bind((printerStatus.isEqualTo(PrinterStatus.IDLE)
                .or(printerStatus.isEqualTo(PrinterStatus.PAUSED)))
                .or(printerStatus.isEqualTo(PrinterStatus.REMOVING_HEAD))
                .and(extruder.isFitted)
                .and(extruder.filamentLoaded)));

        //TODO canPrint ought to take account of lid and filament
        canPrint.bind(head.isNotNull()
            .and(printerStatus.isEqualTo(PrinterStatus.IDLE)));
        canOpenCloseNozzle.bind(head.isNotNull()
            .and(printerStatus.isEqualTo(PrinterStatus.IDLE)
                .or(printerStatus.isEqualTo(PrinterStatus.PAUSED))));
        canCalibrateNozzleOpening.bind(head.isNotNull()
            .and(printerStatus.isEqualTo(PrinterStatus.IDLE).and(extrudersProperty().get(0).
                    filamentLoadedProperty()).and(Bindings.isNotEmpty(reels))));
        canCalibrateNozzleHeight.bind(head.isNotNull()
            .and(printerStatus.isEqualTo(PrinterStatus.IDLE)));
        canCalibrateXYAlignment.bind(head.isNotNull()
            .and(printerStatus.isEqualTo(PrinterStatus.IDLE)));

        canCancel.bind(
            printerStatus.isEqualTo(PrinterStatus.PAUSED)
            .or(printerStatus.isEqualTo(PrinterStatus.PAUSING))
            .or(printerStatus.isEqualTo(PrinterStatus.EXECUTING_MACRO)
                .and(macroType.isNotEqualTo(MacroType.ABORT)))
            .or(printerStatus.isEqualTo(PrinterStatus.POST_PROCESSING))
            .or(printerStatus.isEqualTo(PrinterStatus.SLICING))
            .or(printerStatus.isEqualTo(PrinterStatus.SENDING_TO_PRINTER).and(macroType.isNull().or(
                        macroType.isNotNull().and(macroIsInterruptible))))
            .or(printerStatus.isEqualTo(PrinterStatus.EXECUTING_MACRO).and(macroIsInterruptible))
            .or(printerStatus.isEqualTo(PrinterStatus.PURGING_HEAD))
            .or(printerStatus.isEqualTo(PrinterStatus.CALIBRATING_NOZZLE_ALIGNMENT))
            .or(printerStatus.isEqualTo(PrinterStatus.CALIBRATING_NOZZLE_HEIGHT))
            .or(printerStatus.isEqualTo(PrinterStatus.CALIBRATING_NOZZLE_OPENING)));

        canRunMacro.bind(printerStatus.isEqualTo(PrinterStatus.IDLE)
            .or(printerStatus.isEqualTo(PrinterStatus.CANCELLING)
                .or(printerStatus.isEqualTo(PrinterStatus.CALIBRATING_NOZZLE_ALIGNMENT)
                    .or(printerStatus.isEqualTo(PrinterStatus.CALIBRATING_NOZZLE_HEIGHT)
                        .or(printerStatus.isEqualTo(PrinterStatus.CALIBRATING_NOZZLE_OPENING)
                            .or(printerStatus.isEqualTo(PrinterStatus.PURGING_HEAD)))))));

        canPause.bind(printerStatus.isEqualTo(PrinterStatus.PRINTING)
            .or(printerStatus.isEqualTo(PrinterStatus.RESUMING))
            .or(printerStatus.isEqualTo(PrinterStatus.SENDING_TO_PRINTER))
            .or(printerStatus.isEqualTo(PrinterStatus.EXECUTING_MACRO)
                .and(macroType.isEqualTo(MacroType.GCODE_PRINT))));

        canCalibrateHead.bind(head.isNotNull()
            .and(printerStatus.isEqualTo(PrinterStatus.IDLE)));

        canRemoveHead.bind(printerStatus.isEqualTo(PrinterStatus.IDLE));

        canPurgeHead.bind(printerStatus.isEqualTo(PrinterStatus.IDLE)
            .and(extruders.get(firstExtruderNumber).filamentLoaded.or(extruders.get(
                        secondExtruderNumber).filamentLoaded)));

        canOpenDoor.bind(printerStatus.isEqualTo(PrinterStatus.IDLE));

        canResume.bind(printerStatus.isEqualTo(PrinterStatus.PAUSED));
    }

    /**
     * If the filament details change for a filament currently on a reel, then the reel should be
     * immediately updated with the new details.
     */
    private void setupFilamentDatabaseChangeListeners()
    {
        filamentContainer.addFilamentDatabaseChangesListener((String filamentId) ->
        {
            for (Map.Entry<Integer, Reel> posReel : reels.entrySet())
            {
                if (posReel.getValue().filamentIDProperty().get().equals(filamentId))
                {
                    try
                    {
                        steno.debug("Update reel with updated filament data");
                        transmitWriteReelEEPROM(posReel.getKey(), filamentContainer.getFilamentByID(
                                                filamentId));
                    } catch (RoboxCommsException ex)
                    {
                        steno.error("Unable to program reel with update filament of id: "
                            + filamentId);
                    }
                }
            }
        });
    }

    // Don't call this from anywhere but Print Engine!
    @Override
    public void setPrinterStatus(PrinterStatus printerStatus)
    {
        Lookup.getTaskExecutor().
            runOnGUIThread(() ->
                {
                    boolean okToChangeState = true;

                    switch (printerStatus)
                    {
                        case IDLE:
                            lastStateBeforePause = null;
                            printEngine.goToIdle();
                            macroType.set(null);
                            deregisterErrorConsumer(this);
                            break;
                        case REMOVING_HEAD:
                            break;
                        case PURGING_HEAD:
                            break;
                        case SLICING:
                            printEngine.goToSlicing();
                            break;
                        case POST_PROCESSING:
                            printEngine.goToPostProcessing();
                            break;
                        case SENDING_TO_PRINTER:
                            printEngine.goToSendingToPrinter();
                            break;
                        case PAUSING:
                            lastStateBeforePause = this.printerStatus.get();
                            break;
                        case PAUSED:
                            if (this.printerStatus.get() != PrinterStatus.PAUSING
                            && this.printerStatus.get() != PrinterStatus.LOADING_FILAMENT
                            && this.printerStatus.get() != PrinterStatus.EJECTING_FILAMENT)
                            {
                                lastStateBeforePause = this.printerStatus.get();
                            }
                            printEngine.goToPause();
                            break;
                        case PRINTING:
                            registerErrorConsumerAllErrors(this);
                            printEngine.goToPrinting();
                            break;
                        case EXECUTING_MACRO:
                            printEngine.goToExecutingMacro();
                            break;
                        case EJECTING_FILAMENT:
                            if (!extruders.get(0).canEject.get())
                            {
                                okToChangeState = false;
                            }
                            break;
                        case LOADING_FILAMENT:
                            break;
                    }

                    if (okToChangeState)
                    {
                        this.printerStatus.set(printerStatus);
                    }
                    steno.debug("Setting printer status to " + printerStatus);
            });
    }

    @Override

    public ReadOnlyObjectProperty<PrinterStatus> printerStatusProperty()
    {
        return printerStatus;
    }

    @Override
    public ReadOnlyObjectProperty<MacroType> macroTypeProperty()
    {
        return macroType;
    }

    @Override
    public PrinterIdentity getPrinterIdentity()
    {
        return printerIdentity;
    }

    @Override
    public PrinterAncillarySystems getPrinterAncillarySystems()
    {
        return printerAncillarySystems;
    }

    @Override
    public ReadOnlyObjectProperty<Head> headProperty()
    {
        return head;
    }

    @Override
    public ObservableMap<Integer, Reel> reelsProperty()
    {
        return reels;
    }

    @Override
    public ObservableList<Extruder> extrudersProperty()
    {
        return extruders;
    }

    @Override
    public ReadOnlyIntegerProperty printJobLineNumberProperty()
    {
        return printJobLineNumber;
    }

    @Override
    public ReadOnlyStringProperty printJobIDProperty()
    {
        return printJobID;
    }

    @Override
    public ReadOnlyObjectProperty pauseStatusProperty()
    {
        return pauseStatus;
    }

    @Override
    public ReadOnlyObjectProperty busyStatusProperty()
    {
        return busyStatus;
    }

    @Override
    public PrintEngine getPrintEngine()
    {
        return printEngine;
    }

    /*
     * Remove head
     */
    @Override
    public final ReadOnlyBooleanProperty canRemoveHeadProperty()
    {
        return canRemoveHead;
    }

    @Override
    public void removeHead(TaskResponder responder) throws PrinterException
    {
        if (!canRemoveHead.get())
        {
            throw new PrinterException("Head remove not permitted");
        }

        final Cancellable cancellable = new SimpleCancellable();

        new Thread(() ->
        {
            boolean success = doRemoveHeadActivity(cancellable);

            Lookup.getTaskExecutor().respondOnGUIThread(responder, success, "Ready to remove head");

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
                    ejectFilament(extruderIndex, null);
                }
            }

            PrinterUtils.waitOnBusy(this, cancellable);

            setPrinterStatus(PrinterStatus.REMOVING_HEAD);

            transmitDirectGCode(GCodeConstants.carriageAbsoluteMoveMode, false);
            transmitDirectGCode("G28 X Y Z", false);
            goToXYZPosition(120, 100, 20);
            transmitDirectGCode("G37 S", false);
            PrinterUtils.waitOnBusy(this, cancellable);

            success = true;
        } catch (PrinterException ex)
        {
            steno.error("Printer exception whilst executing remove head " + ex.getMessage());
        } catch (RoboxCommsException ex)
        {
            steno.error("Comms exception whilst executing remove head");
        }

        return success;
    }
    /*
     * Purge head
     */

    @Override
    public final ReadOnlyBooleanProperty canPurgeHeadProperty()
    {
        return canPurgeHead;
    }

    protected final FloatProperty purgeTemperatureProperty = new SimpleFloatProperty(0);

    @Override
    public void resetPurgeTemperature(PrinterSettings printerSettings)
    {
        //TODO modify for multiple reels
        Filament settingsFilament = printerSettings.getFilament0();
        float reelNozzleTemperature = 0;

        if (settingsFilament != null)
        {
            reelNozzleTemperature = settingsFilament.getNozzleTemperature();
        } else
        {
            //TODO modify for multiple reels
            reelNozzleTemperature = (float) reels.get(0).nozzleTemperatureProperty().get();
        }

        Head headToWrite = head.get().clone();
        headToWrite.nozzleHeaters.get(0).lastFilamentTemperature.set(reelNozzleTemperature);

        try
        {
            writeHeadEEPROM(headToWrite);
            readHeadEEPROM();
        } catch (RoboxCommsException ex)
        {
            steno.warning("Failed to write purge temperature");
        }
    }

    /*
     * Print
     */
    @Override
    public final ReadOnlyBooleanProperty canPrintProperty()
    {
        return canPrint;
    }

    @Override
    public ReadOnlyBooleanProperty canOpenCloseNozzleProperty()
    {
        return canOpenCloseNozzle;
    }

    @Override
    public ReadOnlyBooleanProperty canCalibrateNozzleHeightProperty()
    {
        return canCalibrateNozzleHeight;
    }

    @Override
    public ReadOnlyBooleanProperty canCalibrateXYAlignmentProperty()
    {
        return canCalibrateXYAlignment;
    }

    @Override
    public ReadOnlyBooleanProperty canCalibrateNozzleOpeningProperty()
    {
        return canCalibrateNozzleOpening;
    }

    /**
     * Calibrate head
     */
    public ReadOnlyBooleanProperty canCalibrateHeadProperty()
    {
        return canCalibrateHead;
    }

    /*
     * Cancel
     */
    @Override
    public final ReadOnlyBooleanProperty canCancelProperty()
    {
        return canCancel;
    }

    @Override
    public void cancel(TaskResponder responder) throws PrinterException
    {
        if (!canCancel.get())
        {
            throw new PrinterException("Cancel not permitted: printer status is " + printerStatus);
        }

        setPrinterStatus(PrinterStatus.CANCELLING);

        final Cancellable cancellable = new SimpleCancellable();

        new Thread(() ->
        {
            boolean success = doAbortActivity(cancellable);

            if (responder != null)
            {
                Lookup.getTaskExecutor().respondOnGUIThread(responder, success, "Abort complete");
            }

            setPrinterStatus(PrinterStatus.IDLE);

        }, "Aborting").start();
    }

    private boolean doAbortActivity(Cancellable cancellable)
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

        macroType.set(MacroType.ABORT);

        try
        {
            printEngine.runMacroPrintJob("abort_print", true);
            PrinterUtils.waitOnMacroFinished(this, cancellable);
            success = true;
        } catch (MacroPrintException ex)
        {
            steno.error("Failed to run abort macro: " + ex.getMessage());
        }

        return success;
    }

    /**
     *
     * @return
     */
    @Override
    public final ReadOnlyBooleanProperty canPauseProperty()
    {
        return canPause;
    }

    @Override
    public void pause() throws PrinterException
    {
        if (!canPause.get())
        {
            throw new PrintActionUnavailableException("Cannot pause at this time");
        }

        steno.debug("Printer model asked to pause");
        if (steno.getCurrentLogLevel().isLoggable(LogLevel.DEBUG))
        {
            Throwable t = new Throwable();
            t.printStackTrace();
        }

        setPrinterStatus(PrinterStatus.PAUSING);

        try
        {
            PausePrint gcodePacket = (PausePrint) RoboxTxPacketFactory.createPacket(
                TxPacketTypeEnum.PAUSE_RESUME_PRINT);
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
    @Override
    public final ReadOnlyBooleanProperty canResumeProperty()
    {
        return canResume;
    }

    /**
     *
     * @throws PrinterException
     */
    @Override
    public void resume() throws PrinterException
    {
        if (!canResume.get())
        {
            throw new PrintActionUnavailableException("Cannot resume at this time");
        }

        setPrinterStatus(PrinterStatus.RESUMING);

        try
        {
            PausePrint gcodePacket = (PausePrint) RoboxTxPacketFactory.createPacket(
                TxPacketTypeEnum.PAUSE_RESUME_PRINT);
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
    @Override
    public int getDataFileSequenceNumber()
    {
        return dataFileSequenceNumber;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isPrintInitiated()
    {
        return printInitiated;
    }

    @Override
    public void executeGCodeFile(String fileName, boolean monitorForErrors) throws PrinterException
    {
        if (!canRunMacro.get())
        {
            throw new PrintActionUnavailableException("Execute GCode not available");
        }

        if (mustPurgeHead.get())
        {
            throw new PurgeRequiredException("Cannot execute GCode - purge required");
        }

        if (monitorForErrors)
        {
            registerErrorConsumerAllErrors(this);
        }

        macroType.set(MacroType.GCODE_PRINT);

        boolean jobAccepted = false;

        try
        {
            jobAccepted = printEngine.printGCodeFile(fileName, true);
        } catch (MacroPrintException ex)
        {
            steno.error("Failed to print GCode file " + fileName + " : " + ex.getMessage());
        }

        if (!jobAccepted)
        {
            macroType.set(null);
            throw new PrintJobRejectedException("Could not run GCode " + fileName + " in mode "
                + printerStatus.get().name());
        }
    }

    @Override
    public final void executeMacro(String macroName) throws PrinterException
    {
        steno.debug("Request to run macro: " + macroName);

        if (!canRunMacro.get())
        {
            throw new PrintActionUnavailableException("Run macro not available");
        }

        if (mustPurgeHead.get())
        {
            throw new PurgeRequiredException("Cannot run macro - purge required");
        }

        macroType.set(MacroType.STANDARD_MACRO);

        boolean jobAccepted = false;

        try
        {
            jobAccepted = printEngine.runMacroPrintJob(macroName, true);
        } catch (MacroPrintException ex)
        {
            steno.error("Failed to run macro " + macroName + ": " + ex.getMessage());
        }

        if (!jobAccepted)
        {
            macroType.set(null);
            throw new PrintJobRejectedException("Macro " + macroName + " could not be run in mode "
                + printerStatus.get().name());
        }
    }

    @Override
    public final void executeMacroWithoutPurgeCheck(String macroName) throws PrinterException
    {
        if (!canRunMacro.get())
        {
            throw new PrintActionUnavailableException("Run macro not available");
        }

        macroType.set(MacroType.STANDARD_MACRO);

        boolean jobAccepted = false;

        try
        {
            jobAccepted = printEngine.runMacroPrintJob(macroName, true);
        } catch (MacroPrintException ex)
        {
            steno.error("Failed to macro: " + macroName + " reason:" + ex.getMessage());
        }

        if (!jobAccepted)
        {
            macroType.set(null);
            throw new PrintJobRejectedException("Macro " + macroName + " could not be run");
        }
    }

    @Override
    public void executeMacroWithoutPurgeCheckAndCallbackWhenDone(String macroName,
        TaskResponder responder)
    {
        final Cancellable cancellable = new SimpleCancellable();

        new Thread(() ->
        {
            boolean success = false;

            try
            {
                executeMacroWithoutPurgeCheck(macroName);
                PrinterUtils.waitOnMacroFinished(this, cancellable);
                success = true;
            } catch (PrinterException ex)
            {
                steno.error("PrinterException whilst invoking macro: " + ex.getMessage());
            }

            Lookup.getTaskExecutor().respondOnGUIThread(responder, success, "Complete");

        }, "Executing Macro").start();
    }

    @Override
    public void callbackWhenNotBusy(TaskResponder responder)
    {
        final Cancellable cancellable = new SimpleCancellable();

        new Thread(() ->
        {
            boolean success = false;

            PrinterUtils.waitOnBusy(this, cancellable);
            success = true;

            Lookup.getTaskExecutor().respondOnGUIThread(responder, success, "Complete");

        }, "Waiting until not busy").start();
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
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.
            createPacket(TxPacketTypeEnum.EXECUTE_GCODE);

        String gcodeToSendWithLF = SystemUtils.cleanGCodeForTransmission(gcodeToSend) + "\n";

        gcodePacket.setMessagePayload(gcodeToSendWithLF);

        GCodeDataResponse response = (GCodeDataResponse) commandInterface.
            writeToPrinter(gcodePacket);

        if (addToTranscript)
        {
            Lookup.getTaskExecutor().runOnGUIThread(new Runnable()
            {

                public void run()
                {
                    addToGCodeTranscript(gcodeToSendWithLF);
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

    private boolean transmitDataFileStart(final String fileID, boolean jobCanBeReprinted) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = null;

        if (jobCanBeReprinted)
        {
            gcodePacket = RoboxTxPacketFactory.createPacket(
                TxPacketTypeEnum.SEND_PRINT_FILE_START);
        } else
        {
            gcodePacket = RoboxTxPacketFactory.createPacket(
                TxPacketTypeEnum.START_OF_DATA_FILE);
        }

        gcodePacket.setMessagePayload(fileID);

        AckResponse response = (AckResponse) commandInterface.writeToPrinter(gcodePacket);
        boolean success = false;
        // Only check for SD card errors here...
        success = !response.getFirmwareErrors().contains(FirmwareError.SD_CARD);

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
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.END_OF_DATA_FILE);
        gcodePacket.setMessagePayload(payloadData);
        gcodePacket.setSequenceNumber(sequenceNumber);

        return (AckResponse) commandInterface.writeToPrinter(gcodePacket);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    @Override
    public AckResponse transmitReportErrors() throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.
            createPacket(TxPacketTypeEnum.REPORT_ERRORS);

        return (AckResponse) commandInterface.writeToPrinter(gcodePacket);
    }

    /**
     *
     * @throws RoboxCommsException
     */
    @Override
    public void transmitResetErrors() throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.RESET_ERRORS);

        commandInterface.writeToPrinter(gcodePacket);
    }

    /**
     *
     * @param firmwareID
     * @return
     * @throws PrinterException
     */
    @Override
    public boolean transmitUpdateFirmware(final String firmwareID) throws PrinterException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.UPDATE_FIRMWARE);
        gcodePacket.setMessagePayload(firmwareID);

        AckResponse response = null;

        try
        {
            response = (AckResponse) commandInterface.writeToPrinter(gcodePacket);
        } catch (RoboxCommsException ex)
        {
            //We expect to see an exception here as the printer disconnects after an update...
            steno.info("Post firmware update disconnect");
        }

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
     * @return @throws celtech.printerControl.model.PrinterException
     */
    @Override
    public AckResponse formatHeadEEPROM() throws PrinterException
    {
        FormatHeadEEPROM formatHead = (FormatHeadEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.FORMAT_HEAD_EEPROM);
        AckResponse response = null;
        try
        {
            response = (AckResponse) commandInterface.writeToPrinter(formatHead);
            steno.info("Head formatted");
        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending format head");
            throw new PrinterException("Error formatting head");
        }

        return response;
    }

    /**
     *
     * @param reelNumber
     * @return @throws celtech.printerControl.model.PrinterException
     */
    @Override
    public AckResponse formatReelEEPROM(final int reelNumber) throws PrinterException
    {
        RoboxTxPacket formatPacket = null;
        steno.info("Formatting reel " + reelNumber);
        switch (reelNumber)
        {
            case 0:
                formatPacket = RoboxTxPacketFactory.createPacket(
                    TxPacketTypeEnum.FORMAT_REEL_0_EEPROM);
                break;
            case 1:
                formatPacket = RoboxTxPacketFactory.createPacket(
                    TxPacketTypeEnum.FORMAT_REEL_1_EEPROM);
                break;
        }

        AckResponse response = null;
        try
        {
            response = (AckResponse) commandInterface.writeToPrinter(formatPacket);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending format reel");
            throw new PrinterException("Error formatting reel");
        }

        return response;
    }

    /**
     *
     * @param reelNumber
     * @return @throws RoboxCommsException
     */
    @Override
    public ReelEEPROMDataResponse readReelEEPROM(int reelNumber) throws RoboxCommsException
    {
        steno.info("Reading reel " + reelNumber + " EEPROM");

        RoboxTxPacket packet = null;

        switch (reelNumber)
        {
            case 0:
                packet = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_REEL_0_EEPROM);
                break;
            case 1:
                packet = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_REEL_1_EEPROM);
                break;
            default:
                steno.warning("Using default reel - was asked to read reel number " + reelNumber);
                packet = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_REEL_0_EEPROM);
                break;
        }

        return (ReelEEPROMDataResponse) commandInterface.writeToPrinter(packet);
    }

    /**
     *
     * @param reelToWrite
     * @throws RoboxCommsException
     */
    public void writeReelEEPROM(int reelNumber, Reel reelToWrite) throws RoboxCommsException
    {
        RoboxTxPacket readPacket = null;
        RoboxTxPacket writePacket = null;

        switch (reelNumber)
        {
            case 0:
                readPacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_REEL_0_EEPROM);
                writePacket = RoboxTxPacketFactory.
                    createPacket(TxPacketTypeEnum.WRITE_REEL_0_EEPROM);
                ((WriteReel0EEPROM) writePacket).populateEEPROM(reelToWrite.filamentID.get(),
                                                                reelToWrite.firstLayerNozzleTemperature.
                                                                get(),
                                                                reelToWrite.nozzleTemperature.get(),
                                                                reelToWrite.firstLayerBedTemperature.
                                                                get(),
                                                                reelToWrite.bedTemperature.get(),
                                                                reelToWrite.ambientTemperature.get(),
                                                                reelToWrite.diameter.get(),
                                                                reelToWrite.filamentMultiplier.get(),
                                                                reelToWrite.feedRateMultiplier.get(),
                                                                reelToWrite.remainingFilament.get(),
                                                                reelToWrite.friendlyFilamentName.
                                                                get(),
                                                                reelToWrite.material.get(),
                                                                reelToWrite.displayColour.get());
                break;
            case 1:
                readPacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_REEL_1_EEPROM);
                writePacket = RoboxTxPacketFactory.
                    createPacket(TxPacketTypeEnum.WRITE_REEL_1_EEPROM);
                ((WriteReel1EEPROM) writePacket).populateEEPROM(reelToWrite.filamentID.get(),
                                                                reelToWrite.firstLayerNozzleTemperature.
                                                                get(),
                                                                reelToWrite.nozzleTemperature.get(),
                                                                reelToWrite.firstLayerBedTemperature.
                                                                get(),
                                                                reelToWrite.bedTemperature.get(),
                                                                reelToWrite.ambientTemperature.get(),
                                                                reelToWrite.diameter.get(),
                                                                reelToWrite.filamentMultiplier.get(),
                                                                reelToWrite.feedRateMultiplier.get(),
                                                                reelToWrite.remainingFilament.get(),
                                                                reelToWrite.friendlyFilamentName.
                                                                get(),
                                                                reelToWrite.material.get(),
                                                                reelToWrite.displayColour.get());
                break;
        }

        AckResponse response = null;

        if (readPacket != null
            && writePacket != null)
        {
            response = (AckResponse) commandInterface.writeToPrinter(writePacket);
            commandInterface.writeToPrinter(readPacket);
        }
    }

    /**
     *
     * @param filament
     * @return
     * @throws RoboxCommsException
     */
    @Override
    public AckResponse transmitWriteReelEEPROM(int reelNumber, Filament filament) throws RoboxCommsException
    {
        RoboxTxPacket readPacket = null;
        RoboxTxPacket writePacket = null;

        switch (reelNumber)
        {
            case 0:
                readPacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_REEL_0_EEPROM);
                writePacket = RoboxTxPacketFactory.
                    createPacket(TxPacketTypeEnum.WRITE_REEL_0_EEPROM);
                ((WriteReel0EEPROM) writePacket).populateEEPROM(filament.getFilamentID(),
                                                                filament.
                                                                getFirstLayerNozzleTemperature(),
                                                                filament.getNozzleTemperature(),
                                                                filament.
                                                                getFirstLayerBedTemperature(),
                                                                filament.getBedTemperature(),
                                                                filament.getAmbientTemperature(),
                                                                filament.getDiameter(),
                                                                filament.getFilamentMultiplier(),
                                                                filament.getFeedRateMultiplier(),
                                                                filament.getRemainingFilament(),
                                                                filament.getFriendlyFilamentName(),
                                                                filament.getMaterial(),
                                                                filament.getDisplayColour());
                break;
            case 1:
                readPacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_REEL_1_EEPROM);
                writePacket = RoboxTxPacketFactory.
                    createPacket(TxPacketTypeEnum.WRITE_REEL_1_EEPROM);
                ((WriteReel1EEPROM) writePacket).populateEEPROM(filament.getFilamentID(),
                                                                filament.
                                                                getFirstLayerNozzleTemperature(),
                                                                filament.getNozzleTemperature(),
                                                                filament.
                                                                getFirstLayerBedTemperature(),
                                                                filament.getBedTemperature(),
                                                                filament.getAmbientTemperature(),
                                                                filament.getDiameter(),
                                                                filament.getFilamentMultiplier(),
                                                                filament.getFeedRateMultiplier(),
                                                                filament.getRemainingFilament(),
                                                                filament.getFriendlyFilamentName(),
                                                                filament.getMaterial(),
                                                                filament.getDisplayColour());
                break;
            default:
                steno.warning("Using default reel - was asked to read reel number " + reelNumber);
                break;
        }

        AckResponse response = null;

        if (readPacket != null
            && writePacket != null)
        {
            response = (AckResponse) commandInterface.writeToPrinter(writePacket);
            commandInterface.writeToPrinter(readPacket);
        }

        return response;
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
    @Override
    public void transmitWriteReelEEPROM(int reelNumber,
        String filamentID,
        float reelFirstLayerNozzleTemperature, float reelNozzleTemperature,
        float reelFirstLayerBedTemperature, float reelBedTemperature, float reelAmbientTemperature,
        float reelFilamentDiameter,
        float reelFilamentMultiplier, float reelFeedRateMultiplier, float reelRemainingFilament,
        String friendlyName, MaterialType materialType, Color displayColour) throws RoboxCommsException
    {

        RoboxTxPacket readPacket = null;
        RoboxTxPacket writePacket = null;

        switch (reelNumber)
        {
            case 0:
                readPacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_REEL_0_EEPROM);
                writePacket = RoboxTxPacketFactory.
                    createPacket(TxPacketTypeEnum.WRITE_REEL_0_EEPROM);
                ((WriteReel0EEPROM) writePacket).populateEEPROM(filamentID,
                                                                reelFirstLayerNozzleTemperature,
                                                                reelNozzleTemperature,
                                                                reelFirstLayerBedTemperature,
                                                                reelBedTemperature,
                                                                reelAmbientTemperature,
                                                                reelFilamentDiameter,
                                                                reelFilamentMultiplier,
                                                                reelFeedRateMultiplier,
                                                                reelRemainingFilament,
                                                                friendlyName,
                                                                materialType,
                                                                displayColour);
                break;
            case 1:
                readPacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_REEL_1_EEPROM);
                writePacket = RoboxTxPacketFactory.
                    createPacket(TxPacketTypeEnum.WRITE_REEL_1_EEPROM);
                ((WriteReel1EEPROM) writePacket).populateEEPROM(filamentID,
                                                                reelFirstLayerNozzleTemperature,
                                                                reelNozzleTemperature,
                                                                reelFirstLayerBedTemperature,
                                                                reelBedTemperature,
                                                                reelAmbientTemperature,
                                                                reelFilamentDiameter,
                                                                reelFilamentMultiplier,
                                                                reelFeedRateMultiplier,
                                                                reelRemainingFilament,
                                                                friendlyName,
                                                                materialType,
                                                                displayColour);
                break;
            default:
                steno.warning("Using default reel - was asked to read reel number " + reelNumber);
                break;
        }

        AckResponse response = null;

        if (readPacket != null
            && writePacket != null)
        {
            response = (AckResponse) commandInterface.writeToPrinter(writePacket);
            commandInterface.writeToPrinter(readPacket);
        }
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
    @Override
    public AckResponse transmitWriteHeadEEPROM(
        String headTypeCode, String headUniqueID,
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
        AckResponse response = (AckResponse) commandInterface.writeToPrinter(writeHeadEEPROM);
        commandInterface.writeToPrinter(RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.READ_HEAD_EEPROM));
        return response;
    }

    /*
     * Higher level controls
     */
    /**
     *
     * @param nozzle0FirstLayerTarget
     * @param nozzle0Target
     * @param bedFirstLayerTarget
     * @param bedTarget
     * @param ambientTarget
     * @throws RoboxCommsException
     */
    @Override
    public void transmitSetTemperatures(double nozzle0FirstLayerTarget, double nozzle0Target,
        double nozzle1FirstLayerTarget, double nozzle1Target,
        double bedFirstLayerTarget, double bedTarget, double ambientTarget) throws RoboxCommsException
    {
        SetTemperatures setTemperatures = (SetTemperatures) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.SET_TEMPERATURES);
        //TODO change this to support multiple nozzle heaters
        setTemperatures.setTemperatures(nozzle0FirstLayerTarget, nozzle0Target,
                                        nozzle1FirstLayerTarget, nozzle1Target, bedFirstLayerTarget,
                                        bedTarget, ambientTarget);
        commandInterface.writeToPrinter(setTemperatures);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    @Override
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
    @Override
    public StatusResponse transmitStatusRequest() throws RoboxCommsException
    {
        StatusRequest statusRequest = (StatusRequest) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.STATUS_REQUEST);
        return (StatusResponse) commandInterface.writeToPrinter(statusRequest);
    }

    /**
     *
     * @param fileID
     * @param jobCanBeReprinted
     * @return
     * @throws DatafileSendAlreadyInProgress
     * @throws RoboxCommsException
     */
    @Override
    public boolean initialiseDataFileSend(String fileID, boolean jobCanBeReprinted) throws DatafileSendAlreadyInProgress, RoboxCommsException
    {
        boolean success = false;
        success = transmitDataFileStart(fileID, jobCanBeReprinted);
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
    @Override
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
    @Override
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
                AckResponse response = transmitDataFileEnd(outputBuffer.toString(),
                                                           dataFileSequenceNumber);
                if (response.isError())
                {
                    steno.error("Error sending final data file chunk - seq "
                        + dataFileSequenceNumber);
                }
            } else if ((outputBuffer.capacity() - outputBuffer.length()) == 0)
            {
                /*
                 * Send when full
                 */

                if (dataFileSequenceNumber >= dataFileSequenceNumberStartPoint)
                {
                    steno.debug("Sending chunk seq:" + dataFileSequenceNumber);

                    AckResponse response = transmitDataFileChunk(outputBuffer.toString(),
                                                                 dataFileSequenceNumber);
                    if (response.isError())
                    {
                        steno.error("Error sending data file chunk - seq " + dataFileSequenceNumber);
                    }
                } else
                {
                    dataFileSequenceNumber++;
                }
                outputBuffer.delete(0, bufferSize);
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
    @Override
    public void printProject(Project project, Filament filament,
        PrintQualityEnumeration printQuality, SlicerParametersFile settings) throws PrinterException
    {

        //TODO modify for multiple reels
        if ((reels.size() > 0
            && filament != null
            && reels.get(0).isSameAs(filament) == false)
            || filament != null)
        {
            try
            {
                //TODO modify for multiple heaters
                transmitSetTemperatures(filament.getFirstLayerNozzleTemperature(),
                                        filament.getNozzleTemperature(),
                                        filament.getFirstLayerNozzleTemperature(),
                                        filament.getNozzleTemperature(),
                                        filament.getFirstLayerBedTemperature(),
                                        filament.getBedTemperature(),
                                        filament.getAmbientTemperature());

                changeFeedRateMultiplier(filament.getFeedRateMultiplier());
                //TODO modify for multiple extruders
                changeFilamentInfo("E", filament.getDiameter(), filament.getFilamentMultiplier());
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
    @Override
    public void addToGCodeTranscript(String gcodeToSend)
    {
        gcodeTranscript.add(gcodeToSend);
    }

    /**
     *
     * @return
     */
    @Override
    public ObservableList<String> gcodeTranscriptProperty()
    {
        return gcodeTranscript;
    }

    @Override
    public void processRoboxResponse(RoboxRxPacket rxPacket)
    {
        RoboxEventProcessor roboxEventProcessor = new RoboxEventProcessor(this, rxPacket);
        Lookup.getTaskExecutor().runOnGUIThread(roboxEventProcessor);
    }

    /*
     * Door open
     */
    @Override
    public ReadOnlyBooleanProperty canOpenDoorProperty()
    {
        return canOpenDoor;
    }

    @Override
    public void goToOpenDoorPosition(TaskResponder responder) throws PrinterException
    {
        if (!canOpenDoor.get())
        {
            throw new PrintActionUnavailableException("Door open not available");
        }

        setPrinterStatus(PrinterStatus.OPENING_DOOR);

        final Cancellable cancellable = new SimpleCancellable();

        new Thread(() ->
        {
            boolean success = doOpenDoorActivity(cancellable);

            if (responder != null)
            {
                Lookup.getTaskExecutor().respondOnGUIThread(responder, success, "Door open");
            }

            setPrinterStatus(PrinterStatus.IDLE);

        }, "Opening door").start();
    }

    private boolean doOpenDoorActivity(Cancellable cancellable)
    {
        boolean success = false;
        try
        {
            transmitDirectGCode(GCodeConstants.goToOpenDoorPosition, false);
            PrinterUtils.waitOnBusy(this, cancellable);
            success = true;
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when moving sending open door command");
        }

        return success;
    }

    @Override
    public void goToOpenDoorPositionDontWait(TaskResponder responder) throws PrinterException
    {
        if (!canOpenDoor.get())
        {
            throw new PrintActionUnavailableException("Door open not available");
        }

        setPrinterStatus(PrinterStatus.OPENING_DOOR);

        final Cancellable cancellable = new SimpleCancellable();

        new Thread(() ->
        {
            boolean success = doOpenDoorActivityDontWait(cancellable);

            if (responder != null)
            {
                Lookup.getTaskExecutor().respondOnGUIThread(responder, success,
                                                            "Door open don't wait");
            }

            setPrinterStatus(PrinterStatus.IDLE);

        }, "Opening door don't wait").start();
    }

    private boolean doOpenDoorActivityDontWait(Cancellable cancellable)
    {
        boolean success = false;
        try
        {
            transmitDirectGCode(GCodeConstants.goToOpenDoorPositionDontWait, false);
            PrinterUtils.waitOnBusy(this, cancellable);
            success = true;
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when moving sending open door command");
        }

        return success;
    }

    @Override
    public void updatePrinterName(String chosenPrinterName) throws PrinterException
    {
        WritePrinterID writeIDCmd
            = (WritePrinterID) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.WRITE_PRINTER_ID);

        PrinterIdentity newIdentity = printerIdentity.clone();
        newIdentity.printerFriendlyName.set(chosenPrinterName);
        writeIDCmd.populatePacket(newIdentity);
        try
        {
            AckResponse response = (AckResponse) commandInterface.writeToPrinter(writeIDCmd);
            PrinterIDResponse idResponse = (PrinterIDResponse) commandInterface.writeToPrinter(
                RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_PRINTER_ID));
        } catch (RoboxCommsException ex)
        {
            steno.error("Comms exception whilst writing printer name " + ex.getMessage());
            throw new PrinterException("Failed to write name to printer");
        }
    }

    @Override
    public void updatePrinterDisplayColour(Color displayColour) throws PrinterException
    {
        WritePrinterID writeIDCmd = (WritePrinterID) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.WRITE_PRINTER_ID);

        PrinterIdentity newIdentity = printerIdentity.clone();
        newIdentity.printerColour.set(displayColour);
        writeIDCmd.populatePacket(newIdentity);

        try
        {
            AckResponse response = (AckResponse) commandInterface.writeToPrinter(writeIDCmd);
            PrinterIDResponse idResponse = (PrinterIDResponse) commandInterface.writeToPrinter(
                RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_PRINTER_ID));
        } catch (RoboxCommsException ex)
        {
            steno.error("Comms exception whilst writing printer colour " + ex.getMessage());
            throw new PrinterException("Failed to write colour to printer");
        }
    }

    @Override
    public void updatePrinterModelAndEdition(PrinterModel model, PrinterEdition edition) throws PrinterException
    {
        WritePrinterID writeIDCmd
            = (WritePrinterID) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.WRITE_PRINTER_ID);

        PrinterIdentity newIdentity = printerIdentity.clone();
        newIdentity.printermodel.set(model.getCodeName());
        newIdentity.printeredition.set(edition.getCodeName());
        writeIDCmd.populatePacket(newIdentity);
        try
        {
            AckResponse response = (AckResponse) commandInterface.writeToPrinter(writeIDCmd);
            PrinterIDResponse idResponse = (PrinterIDResponse) commandInterface.writeToPrinter(
                RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_PRINTER_ID));
        } catch (RoboxCommsException ex)
        {
            steno.error("Comms exception whilst writing printer model and edition " + ex.
                getMessage());
            throw new PrinterException("Failed to write model and edition to printer");
        }
    }

    @Override
    public void updatePrinterWeek(String weekIdentifier) throws PrinterException
    {
        WritePrinterID writeIDCmd
            = (WritePrinterID) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.WRITE_PRINTER_ID);

        PrinterIdentity newIdentity = printerIdentity.clone();
        newIdentity.printerweekOfManufacture.set(weekIdentifier);
        writeIDCmd.populatePacket(newIdentity);
        try
        {
            AckResponse response = (AckResponse) commandInterface.writeToPrinter(writeIDCmd);
            PrinterIDResponse idResponse = (PrinterIDResponse) commandInterface.writeToPrinter(
                RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_PRINTER_ID));
        } catch (RoboxCommsException ex)
        {
            steno.error("Comms exception whilst writing printer week " + ex.
                getMessage());
            throw new PrinterException("Failed to write week to printer");
        }
    }

    @Override
    public void updatePrinterYear(String yearIdentifier) throws PrinterException
    {
        WritePrinterID writeIDCmd
            = (WritePrinterID) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.WRITE_PRINTER_ID);

        PrinterIdentity newIdentity = printerIdentity.clone();
        newIdentity.printeryearOfManufacture.set(yearIdentifier);
        writeIDCmd.populatePacket(newIdentity);
        try
        {
            AckResponse response = (AckResponse) commandInterface.writeToPrinter(writeIDCmd);
            PrinterIDResponse idResponse = (PrinterIDResponse) commandInterface.writeToPrinter(
                RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_PRINTER_ID));
        } catch (RoboxCommsException ex)
        {
            steno.error("Comms exception whilst writing printer year " + ex.
                getMessage());
            throw new PrinterException("Failed to write year to printer");
        }
    }

    @Override
    public void updatePrinterPONumber(String poIdentifier) throws PrinterException
    {
        WritePrinterID writeIDCmd
            = (WritePrinterID) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.WRITE_PRINTER_ID);

        PrinterIdentity newIdentity = printerIdentity.clone();
        newIdentity.printerpoNumber.set(poIdentifier);
        writeIDCmd.populatePacket(newIdentity);
        try
        {
            AckResponse response = (AckResponse) commandInterface.writeToPrinter(writeIDCmd);
            PrinterIDResponse idResponse = (PrinterIDResponse) commandInterface.writeToPrinter(
                RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_PRINTER_ID));
        } catch (RoboxCommsException ex)
        {
            steno.error("Comms exception whilst writing printer PO number " + ex.
                getMessage());
            throw new PrinterException("Failed to write PO number to printer");
        }
    }

    @Override
    public void updatePrinterSerialNumber(String serialIdentifier) throws PrinterException
    {
        WritePrinterID writeIDCmd
            = (WritePrinterID) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.WRITE_PRINTER_ID);

        PrinterIdentity newIdentity = printerIdentity.clone();
        newIdentity.printerserialNumber.set(serialIdentifier);
        writeIDCmd.populatePacket(newIdentity);
        try
        {
            AckResponse response = (AckResponse) commandInterface.writeToPrinter(writeIDCmd);
            PrinterIDResponse idResponse = (PrinterIDResponse) commandInterface.writeToPrinter(
                RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_PRINTER_ID));
        } catch (RoboxCommsException ex)
        {
            steno.error("Comms exception whilst writing printer serial number " + ex.
                getMessage());
            throw new PrinterException("Failed to write serial number to printer");
        }
    }

    @Override
    public void updatePrinterIDChecksum(String checksum) throws PrinterException
    {
        WritePrinterID writeIDCmd
            = (WritePrinterID) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.WRITE_PRINTER_ID);

        PrinterIdentity newIdentity = printerIdentity.clone();
        newIdentity.printercheckByte.set(checksum);
        writeIDCmd.populatePacket(newIdentity);
        try
        {
            AckResponse response = (AckResponse) commandInterface.writeToPrinter(writeIDCmd);
            PrinterIDResponse idResponse = (PrinterIDResponse) commandInterface.writeToPrinter(
                RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_PRINTER_ID));
        } catch (RoboxCommsException ex)
        {
            steno.error("Comms exception whilst writing printer id checksum " + ex.
                getMessage());
            throw new PrinterException("Failed to write checksum to printer");
        }
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void setNozzleFirstLayerTargetTemperature(int targetTemperature)
    {
        try
        {
            transmitDirectGCode(GCodeConstants.setFirstLayerNozzleTemperatureTarget
                + targetTemperature, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending set nozzle first layer temperature command");
        }
    }

    @Override
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

    @Override
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

    @Override
    public void setBedFirstLayerTargetTemperature(int targetTemperature)
    {
        try
        {
            transmitDirectGCode(GCodeConstants.setFirstLayerBedTemperatureTarget + targetTemperature,
                                false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending set bed first layer target temperature command");
        }
    }

    @Override
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

    @Override
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

    @Override
    public void gotoNozzlePosition(float position)
    {
        try
        {
            transmitDirectGCode("G0 B" + threeDPformatter.format(position), false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending close nozzle command");
        }
    }

    /**
     *
     * @throws celtech.printerControl.model.PrinterException
     */
    @Override
    public void switchOnHeadLEDs() throws PrinterException
    {
        try
        {
            transmitDirectGCode(GCodeConstants.switchOnHeadLEDs, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending switch head LED on command");
            throw new PrinterException("Error sending LED on command");
        }
    }

    @Override
    public void switchOffHeadLEDs() throws PrinterException
    {
        try
        {
            transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending switch head LED off command");
            throw new PrinterException("Error sending LED off command");
        }
    }

    @Override
    public void switchAllNozzleHeatersOff()
    {
        //TODO modify for multiple heaters
        switchNozzleHeaterOff(0);
    }

    @Override
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

    @Override
    public void homeX()
    {
        try
        {
            transmitDirectGCode("G28 X", false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending x home command");
        }
    }

    @Override
    public void homeY()
    {
        try
        {
            transmitDirectGCode("G28 Y", false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending y home command");
        }
    }

    @Override
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

    @Override
    public void probeX()
    {
        try
        {
            transmitDirectGCode("G28 X?", false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending probe X command");
        }
    }

    @Override
    public float getXDelta() throws PrinterException
    {
        float deltaValue = 0;
        String measurementString = null;

        try
        {
            String response = transmitDirectGCode("M111", false);
            steno.debug("X delta response: " + response);
            measurementString = response.replaceFirst("Xdelta:", "").replaceFirst(
                "\nok", "").trim();
            deltaValue = Float.valueOf(measurementString.trim());

        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending get X delta");
            throw new PrinterException("Error sending get X delta");
        } catch (NumberFormatException ex)
        {
            steno.error("Couldn't parse measurement string for X delta: " + measurementString);
            throw new PrinterException("Measurement string for X delta: " + measurementString
                + " : could not be parsed");
        }

        return deltaValue;
    }

    @Override
    public void probeY()
    {
        try
        {
            transmitDirectGCode("G28 Y?", false);
            String result = transmitDirectGCode("M112", false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending probe Y command");
        }
    }

    @Override
    public float getYDelta() throws PrinterException
    {
        float deltaValue = 0;
        String measurementString = null;

        try
        {
            String response = transmitDirectGCode("M112", false);
            steno.debug("Y delta response: " + response);
            measurementString = response.replaceFirst("Ydelta:", "").replaceFirst(
                "\nok", "").trim();
            deltaValue = Float.valueOf(measurementString.trim());

        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending get Y delta");
            throw new PrinterException("Error sending get Y delta");
        } catch (NumberFormatException ex)
        {
            steno.error("Couldn't parse measurement string for Y delta: " + measurementString);
            throw new PrinterException("Measurement string for Y delta: " + measurementString
                + " : could not be parsed");
        }

        return deltaValue;
    }

    @Override
    public void probeZ()
    {
        try
        {
            transmitDirectGCode("G28 Z?", false);
            String result = transmitDirectGCode("M113", false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending probe Z command");
        }
    }

    @Override
    public float getZDelta() throws PrinterException
    {
        float deltaValue = 0;
        String measurementString = null;

        try
        {
            String response = transmitDirectGCode("M113", false);
            steno.debug("Z delta response: " + response);
            measurementString = response.replaceFirst("Zdelta:", "").replaceFirst(
                "\nok", "").trim();
            deltaValue = Float.valueOf(measurementString.trim());

        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending get Z delta");
            throw new PrinterException("Error sending get Z delta");
        } catch (NumberFormatException ex)
        {
            steno.error("Couldn't parse measurement string for Z delta: " + measurementString);
            throw new PrinterException("Measurement string for Z delta: " + measurementString
                + " : could not be parsed");
        }

        return deltaValue;
    }

    @Override
    public void levelGantry()
    {
        try
        {
            transmitDirectGCode("G38", false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending level gantry command");
        }
    }

    @Override
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

    @Override
    public void goToXYPosition(double xPosition, double yPosition)
    {
        try
        {
            transmitDirectGCode("G0 X" + threeDPformatter.format(xPosition) + " Y"
                + threeDPformatter.format(yPosition), false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending x y position command");
        }
    }

    @Override
    public void goToXYZPosition(double xPosition, double yPosition, double zPosition)
    {
        try
        {
            transmitDirectGCode("G0 X" + threeDPformatter.format(xPosition)
                + " Y" + threeDPformatter.format(yPosition)
                + " Z" + threeDPformatter.format(zPosition), false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending x y z position command");
        }
    }

    @Override
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

    @Override
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

    /**
     *
     * @param headToWrite
     * @throws RoboxCommsException
     */
    @Override
    public void writeHeadEEPROM(Head headToWrite) throws RoboxCommsException
    {
        WriteHeadEEPROM writeHeadEEPROM = (WriteHeadEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.WRITE_HEAD_EEPROM);
        writeHeadEEPROM.populateEEPROM(headToWrite);
        commandInterface.writeToPrinter(writeHeadEEPROM);

        readHeadEEPROM();
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    @Override
    public HeadEEPROMDataResponse readHeadEEPROM() throws RoboxCommsException
    {
        ReadHeadEEPROM readHead = (ReadHeadEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.READ_HEAD_EEPROM);
        return (HeadEEPROMDataResponse) commandInterface.writeToPrinter(readHead);
    }

    @Override
    public void ejectFilament(int extruderNumber, TaskResponder responder) throws PrinterException
    {
        if (!extruders.get(extruderNumber).isFitted.get())
        {
            throw new PrintActionUnavailableException("Extruder " + extruderNumber
                + " is not present");
        }

        if (!extruders.get(extruderNumber).canEject.get())
        {
            throw new PrintActionUnavailableException("Eject is not available for extruder "
                + extruderNumber);
        }

        final Cancellable cancellable = new SimpleCancellable();

        new Thread(() ->
        {
            boolean success = doEjectFilamentActivity(extruderNumber, cancellable);

            Lookup.getTaskExecutor().respondOnGUIThread(responder, success, "Filament ejected");

        }, "Ejecting filament").start();

    }

    private boolean doEjectFilamentActivity(int extruderNumber, Cancellable cancellable)
    {
        boolean success = false;
        try
        {
            transmitDirectGCode(GCodeConstants.ejectFilament + " "
                + extruders.get(extruderNumber).getExtruderAxisLetter(), false);
            PrinterUtils.waitOnBusy(this, cancellable);
            success = true;
        } catch (RoboxCommsException ex)
        {
            steno.error("Error whilst ejecting filament");
        }

        return success;
    }

    @Override
    public void jogAxis(AxisSpecifier axis, float distance, float feedrate, boolean use_G1) throws PrinterException
    {
        try
        {
            transmitDirectGCode(GCodeConstants.carriageRelativeMoveMode, true);
            if (use_G1)
            {
                if (feedrate > 0)
                {
                    transmitDirectGCode(
                        "G1 " + axis.name()
                        + threeDPformatter.format(distance)
                        + " F"
                        + threeDPformatter.format(feedrate), true);
                } else
                {
                    transmitDirectGCode(
                        "G1 " + axis.name()
                        + threeDPformatter.format(distance), true);
                }
            } else
            {
                transmitDirectGCode(
                    "G0 " + axis.name()
                    + threeDPformatter.format(distance), true);
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Error jogging axis");
            throw new PrinterException("Comms error whilst jogging axis");
        }

    }

    @Override
    public void switchOffHeadFan() throws PrinterException
    {
        try
        {
            transmitDirectGCode(GCodeConstants.switchOffHeadFan, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending head fan off command");
            throw new PrinterException("Error whilst sending head fan off");
        }
    }

    @Override
    public void switchOnHeadFan() throws PrinterException
    {
        try
        {
            transmitDirectGCode(GCodeConstants.switchOnHeadFan, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending head fan on command");
            throw new PrinterException("Error whilst sending head fan on");
        }
    }

    @Override
    public void openNozzleFully() throws PrinterException
    {
        try
        {
            transmitDirectGCode(GCodeConstants.openNozzle, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending open nozzle command");
            throw new PrinterException("Error whilst sending nozzle open command");
        }
    }

    @Override
    public void openNozzleFullyExtra() throws PrinterException
    {
        try
        {
            transmitDirectGCode(GCodeConstants.openNozzleExtra, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending open nozzle command");
            throw new PrinterException("Error whilst sending nozzle open command");
        }
    }

    @Override
    public void closeNozzleFully() throws PrinterException
    {
        try
        {
            transmitDirectGCode(GCodeConstants.closeNozzle, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending nozzle close command");
            throw new PrinterException("Error whilst sending nozzle close command");
        }
    }

    /**
     *
     * @param colour
     * @throws celtech.printerControl.model.PrinterException
     */
    @Override
    public void setAmbientLEDColour(Color colour) throws PrinterException
    {
        SetAmbientLEDColour ledColour = (SetAmbientLEDColour) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.SET_AMBIENT_LED_COLOUR);

        ledColour.setLEDColour(colour);

        try
        {
            commandInterface.writeToPrinter(ledColour);
        } catch (RoboxCommsException ex)
        {
            throw new PrinterException("Error sending ambient LED command");
        }
    }

    /**
     *
     * @param colour
     * @throws celtech.printerControl.model.PrinterException
     */
    @Override
    public void setReelLEDColour(Color colour) throws PrinterException
    {
        SetReelLEDColour ledColour = (SetReelLEDColour) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.SET_REEL_LED_COLOUR);
        ledColour.setLEDColour(colour);
        try
        {
            commandInterface.writeToPrinter(ledColour);
        } catch (RoboxCommsException ex)
        {
            throw new PrinterException("Error sending reel LED command");
        }
    }

    /**
     *
     * @return @throws celtech.printerControl.model.PrinterException
     */
    @Override
    public PrinterIDResponse readPrinterID() throws PrinterException
    {
        PrinterIDResponse idResponse = null;

        ReadPrinterID readId = (ReadPrinterID) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.READ_PRINTER_ID);
        try
        {
            idResponse = (PrinterIDResponse) commandInterface.writeToPrinter(readId);
        } catch (RoboxCommsException ex)
        {
            throw new PrinterException("Error sending read printer ID command");
        }

        return idResponse;
    }

    /**
     *
     * @return @throws celtech.printerControl.model.PrinterException
     */
    @Override
    public FirmwareResponse readFirmwareVersion() throws PrinterException
    {
        FirmwareResponse response = null;

        QueryFirmwareVersion readFirmware = (QueryFirmwareVersion) RoboxTxPacketFactory
            .createPacket(TxPacketTypeEnum.QUERY_FIRMWARE_VERSION);
        try
        {
            response = (FirmwareResponse) commandInterface.writeToPrinter(readFirmware);
        } catch (RoboxCommsException ex)
        {
            throw new PrinterException("Error sending read printer ID command");
        }
        return response;
    }

    /**
     *
     * @param nozzleNumber
     * @throws PrinterException
     */
    @Override
    public void selectNozzle(int nozzleNumber) throws PrinterException
    {
        if (nozzleNumber >= head.get().nozzles.size())
        {
            throw new PrinterException("Nozzle number " + nozzleNumber + " does not exist");
        }

        try
        {
            transmitDirectGCode(GCodeConstants.selectNozzle + nozzleNumber, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error sending nozzle select command");
            throw new PrinterException("Error whilst sending nozzle select command");
        }
    }

    @Override
    public void shutdown()
    {
        steno.info("Shutdown print engine...");
        printEngine.shutdown();
        steno.info("Shutdown command interface...");
        commandInterface.shutdown();
    }

    @Override
    public XAndYStateTransitionManager startCalibrateXAndY() throws PrinterException
    {
        if (!canCalibrateXYAlignment.get())
        {
            throw new PrinterException("Calibrate not permitted");
        }
        
       StateTransitionManager.StateTransitionActionsFactory actionsFactory = (Cancellable userCancellable,
            Cancellable errorCancellable)
            -> new CalibrationXAndYActions(HardwarePrinter.this, userCancellable,
                                                   errorCancellable);

        StateTransitionManager.TransitionsFactory transitionsFactory = (StateTransitionActions actions)
            -> new CalibrationXAndYTransitions((CalibrationXAndYActions) actions);
        

        XAndYStateTransitionManager calibrationAlignmentManager
            = new XAndYStateTransitionManager(actionsFactory, transitionsFactory);
        return calibrationAlignmentManager;
    }

    @Override
    public NozzleHeightStateTransitionManager startCalibrateNozzleHeight() throws PrinterException
    {
        if (!canCalibrateNozzleHeight.get())
        {
            throw new PrinterException("Calibrate not permitted");
        }
        
       StateTransitionManager.StateTransitionActionsFactory actionsFactory = (Cancellable userCancellable,
            Cancellable errorCancellable)
            -> new CalibrationNozzleHeightActions(HardwarePrinter.this, userCancellable,
                                                   errorCancellable);

        StateTransitionManager.TransitionsFactory transitionsFactory = (StateTransitionActions actions)
            -> new CalibrationNozzleHeightTransitions((CalibrationNozzleHeightActions) actions);        
        
        NozzleHeightStateTransitionManager calibrationHeightManager
            = new NozzleHeightStateTransitionManager(actionsFactory, transitionsFactory);
        return calibrationHeightManager;
    }

    @Override
    public PurgeStateTransitionManager startPurge() throws PrinterException
    {
        if (!canPurgeHead.get())
        {
            throw new PrinterException("Purge not permitted");
        }

        StateTransitionManager.StateTransitionActionsFactory actionsFactory = (Cancellable userCancellable,
            Cancellable errorCancellable)
            -> new PurgeActions(HardwarePrinter.this, userCancellable, errorCancellable);

        StateTransitionManager.TransitionsFactory transitionsFactory = (StateTransitionActions actions)
            -> new PurgeTransitions((PurgeActions) actions);

        PurgeStateTransitionManager purgeManager
            = new PurgeStateTransitionManager(actionsFactory, transitionsFactory);
        return purgeManager;
    }

    @Override
    public NozzleOpeningStateTransitionManager startCalibrateNozzleOpening() throws PrinterException
    {
        if (!canCalibrateNozzleOpening.get())
        {
            throw new PrinterException("Calibrate not permitted");
        }

        StateTransitionManager.StateTransitionActionsFactory actionsFactory = (Cancellable userCancellable,
            Cancellable errorCancellable)
            -> new CalibrationNozzleOpeningActions(HardwarePrinter.this, userCancellable,
                                                   errorCancellable);

        StateTransitionManager.TransitionsFactory transitionsFactory = (StateTransitionActions actions)
            -> new CalibrationNozzleOpeningTransitions((CalibrationNozzleOpeningActions) actions);

        NozzleOpeningStateTransitionManager calibrationOpeningManager
            = new NozzleOpeningStateTransitionManager(actionsFactory, transitionsFactory);
        return calibrationOpeningManager;
    }

    @Override
    public void registerErrorConsumer(ErrorConsumer errorConsumer,
        List<FirmwareError> errorsOfInterest)
    {
        steno.debug("Registering printer error consumer - " + errorConsumer.toString()
            + " on printer " + printerIdentity.printerFriendlyName);
        errorConsumers.put(errorConsumer, errorsOfInterest);
    }

    @Override
    public void registerErrorConsumerAllErrors(ErrorConsumer errorConsumer)
    {
        steno.debug("Registering printer error consumer for all errors - " + errorConsumer.
            toString() + " on printer " + printerIdentity.printerFriendlyName);
        ArrayList<FirmwareError> errorsOfInterest = new ArrayList<>();
        errorsOfInterest.add(FirmwareError.ALL_ERRORS);
        errorConsumers.put(errorConsumer, errorsOfInterest);
    }

    @Override
    public void deregisterErrorConsumer(ErrorConsumer errorConsumer)
    {
        steno.debug("Deregistering printer error consumer for all errors - " + errorConsumer.
            toString() + " on printer " + printerIdentity.printerFriendlyName);
        errorConsumers.remove(errorConsumer);
    }

    @Override
    public boolean doFilamentSlipWhilePrinting(FirmwareError error)
    {
        boolean reachedLimit = false;

        float feedrateMultiplier = printerAncillarySystems.feedRateMultiplier.get();

        if (!moderatingFeedrate)
        {
            moderatingFeedrate = true;
            originalFeedrateMultiplier = feedrateMultiplier;
        }

        if (MathUtils.compareDouble(feedrateMultiplier, MIN_FEEDRATE_MULTIPLIER, 1e-2)
            == MathUtils.MORE_THAN)
        {
            feedrateMultiplier -= 0.2f;
            feedrateMultiplier = Math.max(feedrateMultiplier, MIN_FEEDRATE_MULTIPLIER);
            try
            {
                steno.info("Automatically reducing feedrate to " + feedrateMultiplier);
                changeFeedRateMultiplier(feedrateMultiplier);
            } catch (PrinterException ex)
            {
                steno.warning("Failed to automatically reduce feedrate during print - " + ex.
                    getMessage());
            }
        } else
        {
            //Unable to proceed
            steno.warning("Hit feedrate limit - autopause");
            try
            {
                pause();
            } catch (PrinterException ex)
            {
                steno.warning("Failed to automatically pause during print");
            }
            reachedLimit = true;
        }

        return reachedLimit;
    }

    @Override
    public void consumeError(FirmwareError error)
    {

        switch (error)
        {
            case E_FILAMENT_SLIP:
            case D_FILAMENT_SLIP:
                if (printerStatus.get() == PrinterStatus.PRINTING
                    || (printerStatus.get() == PrinterStatus.PRINTING && macroType.get().equals(
                        MacroType.GCODE_PRINT)))
                {
                    boolean reachedLimit = doFilamentSlipWhilePrinting(error);

                    if (reachedLimit)
                    {
                        Lookup.getSystemNotificationHandler().processErrorPacketFromPrinter(error,
                                                                                            this);
                    }
                }
                break;
            default:
                // Back stop
                Lookup.getSystemNotificationHandler().processErrorPacketFromPrinter(error, this);
                break;
        }
    }

    @Override
    public void connectionEstablished()
    {
        processErrors = true;
    }

    @Override
    public List<Integer> requestDebugData(boolean addToGCodeTranscript)
    {
        List<Integer> debugData = null;

        RoboxTxPacket debugRequest = RoboxTxPacketFactory.
            createPacket(TxPacketTypeEnum.READ_DEBUG_DATA);

        try
        {
            DebugDataResponse response = (DebugDataResponse) commandInterface.
                writeToPrinter(debugRequest);

            if (response != null)
            {
                debugData = response.getDebugData();
            }

            if (addToGCodeTranscript)
            {
                Lookup.getTaskExecutor().runOnGUIThread(new Runnable()
                {

                    public void run()
                    {
                        if (response == null)
                        {
                            addToGCodeTranscript("No data returned\n");
                        } else
                        {
                            addToGCodeTranscript(response.getDebugData() + "\n");
                        }
                    }
                });
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Error whilst requesting debug data: " + ex.getMessage());
        }

        return debugData;

    }

    @Override
    public void setDataFileSequenceNumberStartPoint(int startingSequenceNumber)
    {
        dataFileSequenceNumberStartPoint = startingSequenceNumber;
    }

    @Override
    public void resetDataFileSequenceNumber()
    {
        dataFileSequenceNumber = 0;
    }

    @Override
    public void extrudeUntilSlip(int extruderNumber) throws PrinterException
    {
        try
        {
            if (extrudersProperty().get(extruderNumber).isFitted.get())
            {
                transmitDirectGCode("G36 "
                    + extrudersProperty().get(extruderNumber).getExtruderAxisLetter(), false);
            } else
            {
                String errorText = "Attempt to extrude until slip on extruder " + extruderNumber
                    + " which is not fitted";
                steno.error(errorText);
                throw new PrinterException(errorText);
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when sending go to target bed temperature command");
            throw new PrinterException("Error when sending extrude until slip on extruder "
                + extruderNumber
                + ": "
                + ex.getMessage());
        }
    }

    @Override
    public void suppressEEPROMAndSDErrorHandling(boolean suppress)
    {
        suppressEEPROMAndSDErrorHandling = suppress;
    }

    @Override
    public TemperatureAndPWMData getTemperatureAndPWMData() throws PrinterException
    {
        TemperatureAndPWMData data = null;
        try
        {
            String response = transmitDirectGCode("M105", false);
            data = new TemperatureAndPWMData();
            data.populateFromPrinterData(response);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when requesting temperature and PWM data");
            throw new PrinterException("Error when requesting temperature and PWM data");
        }

        return data;
    }

    class RoboxEventProcessor implements Runnable
    {

        private Printer printer;
        private RoboxRxPacket rxPacket;
        private boolean errorWasConsumed;

        public RoboxEventProcessor(Printer printer, RoboxRxPacket rxPacket)
        {
            this.printer = printer;
            this.rxPacket = rxPacket;
        }

        @Override
        public void run()
        {
            switch (rxPacket.getPacketType())
            {
                case ACK_WITH_ERRORS:
                    AckResponse ackResponse = (AckResponse) rxPacket;
                    steno.trace(ackResponse.toString());

                    if (ackResponse.isError())
                    {
                        List<FirmwareError> errorsFound = new ArrayList<>(ackResponse.
                            getFirmwareErrors());
                        // Copy the error consumer list to stop concurrent modification exceptions if the consumer deregisters itself
                        Map<ErrorConsumer, List<FirmwareError>> errorsToIterateThrough = new WeakHashMap<>(
                            errorConsumers);

                        try
                        {
                            steno.debug("Clearing errors");
                            transmitResetErrors();
                        } catch (RoboxCommsException ex)
                        {
                            steno.warning("Couldn't clear firmware error list");
                        }

                        if (processErrors)
                        {
                            steno.debug(ackResponse.getErrorsAsString());

                            errorsFound.stream()
                                .forEach(foundError ->
                                    {
                                        errorWasConsumed = false;
                                        errorsToIterateThrough.forEach((consumer, errorList) ->
                                            {
                                                if (errorList.contains(foundError) || errorList.
                                                contains(FirmwareError.ALL_ERRORS))
                                                {
                                                    steno.info("Error:" + foundError.name()
                                                        + " passed to " + consumer.toString());
                                                    consumer.consumeError(foundError);
                                                    errorWasConsumed = true;
                                                }
                                        });

                                        if (!errorWasConsumed)
                                        {
                                            steno.info("Default action for error:" + foundError.
                                                name());
                                            systemNotificationManager.processErrorPacketFromPrinter(
                                                foundError, printer);
                                        }
                                });

                            steno.trace(ackResponse.toString());
                        } else
                        {
                            errorsFound.stream()
                                .forEach(foundError ->
                                    {
                                        steno.info("No action for error:" + foundError.
                                            name());
                                });
                        }
                    }
                    break;

                case STATUS_RESPONSE:
                    StatusResponse statusResponse = (StatusResponse) rxPacket;
                    steno.trace(statusResponse.toString());

                    /*
                     * Ancillary systems
                     */
                    printerAncillarySystems.ambientTemperature.set(
                        statusResponse.getAmbientTemperature());
                    printerAncillarySystems.ambientTargetTemperature.set(
                        statusResponse.getAmbientTargetTemperature());
                    printerAncillarySystems.bedTemperature.set(statusResponse.getBedTemperature());
                    printerAncillarySystems.bedTargetTemperature.set(
                        statusResponse.getBedTargetTemperature());
                    printerAncillarySystems.bedFirstLayerTargetTemperature.set(
                        statusResponse.getBedFirstLayerTargetTemperature());
                    printerAncillarySystems.ambientFanOn.set(statusResponse.isAmbientFanOn());
                    printerAncillarySystems.bedHeaterMode.set(statusResponse.getBedHeaterMode());
                    printerAncillarySystems.headFanOn.set(statusResponse.isHeadFanOn());
                    printerAncillarySystems.XStopSwitch.set(statusResponse.isxSwitchStatus());
                    printerAncillarySystems.YStopSwitch.set(statusResponse.isySwitchStatus());
                    printerAncillarySystems.ZStopSwitch.set(statusResponse.iszSwitchStatus());
                    printerAncillarySystems.ZTopStopSwitch.set(statusResponse.isTopZSwitchStatus());
                    printerAncillarySystems.bAxisHome.set(statusResponse.isNozzleSwitchStatus());
                    printerAncillarySystems.doorOpen.set(statusResponse.isDoorOpen());
                    printerAncillarySystems.reelButton.set(statusResponse.isReelButtonPressed());
                    printerAncillarySystems.feedRateMultiplier.set(statusResponse.
                        getFeedRateMultiplier());
                    printerAncillarySystems.whyAreWeWaitingProperty.set(
                        statusResponse.getWhyAreWeWaitingState());
                    printerAncillarySystems.updateGraphData();
                    printerAncillarySystems.sdCardInserted.set(statusResponse.isSDCardPresent());

                    if (!statusResponse.isSDCardPresent() && !suppressEEPROMAndSDErrorHandling)
                    {
                        Lookup.getSystemNotificationHandler().showNoSDCardDialog();
                    }

                    /*
                     * Extruders
                     */
                    boolean filament1Loaded = filamentLoadedGetter.getFilamentLoaded(statusResponse,
                                                                                     1);
                    boolean filament2Loaded = filamentLoadedGetter.getFilamentLoaded(statusResponse,
                                                                                     2);

                    //TODO configure properly for multiple extruders
                    extruders.get(firstExtruderNumber).filamentLoaded.set(filament1Loaded);
                    extruders.get(firstExtruderNumber).indexWheelState.set(statusResponse.
                        isEIndexStatus());
                    extruders.get(firstExtruderNumber).isFitted.set(statusResponse.
                        isExtruderEPresent());
                    extruders.get(firstExtruderNumber).filamentDiameter.set(statusResponse.
                        getEFilamentDiameter());
                    extruders.get(firstExtruderNumber).extrusionMultiplier.set(statusResponse.
                        getEFilamentMultiplier());

                    extruders.get(secondExtruderNumber).filamentLoaded.set(filament2Loaded);
                    extruders.get(secondExtruderNumber).indexWheelState.set(statusResponse.
                        isDIndexStatus());
                    extruders.get(secondExtruderNumber).isFitted.set(statusResponse.
                        isExtruderDPresent());
                    extruders.get(secondExtruderNumber).filamentDiameter.set(statusResponse.
                        getDFilamentDiameter());
                    extruders.get(secondExtruderNumber).extrusionMultiplier.set(statusResponse.
                        getDFilamentMultiplier());

                    if (pauseStatus.get() != statusResponse.getPauseStatus()
                        && statusResponse.getPauseStatus() == PauseStatus.PAUSED)
                    {
                        setPrinterStatus(PrinterStatus.PAUSED);
                    } else if (pauseStatus.get() != statusResponse.getPauseStatus()
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
                    if (busyStatus.get() != statusResponse.getBusyStatus())
                    {
                        switch (statusResponse.getBusyStatus())
                        {
                            case BUSY:
                                break;
                            case LOADING_FILAMENT:
                                lastStateBeforeLoadUnload = printerStatus.get();
                                setPrinterStatus(PrinterStatus.LOADING_FILAMENT);
                                break;
                            case UNLOADING_FILAMENT:
                                lastStateBeforeLoadUnload = printerStatus.get();
                                setPrinterStatus(PrinterStatus.EJECTING_FILAMENT);
                                break;
                            case NOT_BUSY:
                                if (printerStatus.get().equals(PrinterStatus.EJECTING_FILAMENT)
                                    || printerStatus.get().equals(PrinterStatus.LOADING_FILAMENT))
                                {
                                    if (lastStateBeforeLoadUnload != null)
                                    {
                                        setPrinterStatus(lastStateBeforeLoadUnload);
                                        lastStateBeforeLoadUnload = null;
                                    } else
                                    {
                                        setPrinterStatus(PrinterStatus.IDLE);
                                    }
                                }
                                break;
                        }
                    }
                    busyStatus.set(statusResponse.getBusyStatus());

                    pauseStatus.set(statusResponse.getPauseStatus());
                    printJobLineNumber.set(statusResponse.getPrintJobLineNumber());
                    printJobID.set(statusResponse.getRunningPrintJobID());

                    if (head.isNotNull().get())
                    {
                        /*
                         * Heater
                         */
                        if (head.get().nozzleHeaters.size() > 0)
                        {
                            //TODO modify for multiple heaters
                            head.get().nozzleHeaters.get(0).nozzleTemperature.set(
                                statusResponse.getNozzle0Temperature());
                            head.get().nozzleHeaters.get(0).nozzleFirstLayerTargetTemperature.set(
                                statusResponse.getNozzle0FirstLayerTargetTemperature());
                            head.get().nozzleHeaters.get(0).nozzleTargetTemperature.set(
                                statusResponse.getNozzle0TargetTemperature());

                            //TODO modify for multiple heaters
                            if (head.get().getNozzleHeaters().size() > 0)
                            {
                                head.get().getNozzleHeaters().get(0).heaterMode.set(
                                    statusResponse.getNozzle0HeaterMode());
                            }
                            head.get().nozzleHeaters
                                .stream()
                                .forEach(heater -> heater.updateGraphData());
                        }

                        /*
                         * Nozzle data
                         */
                        if (head.get().nozzles.size() > 0)
                        {
                            //TODO modify to work with multiple nozzles
                            //This is only true for the current cam-based heads that only really have one B axis
                            head.get().nozzles
                                .stream()
                                .forEach(nozzle -> nozzle.BPosition.set(
                                        statusResponse.getBPosition()));
                        }

                        head.get().BPosition.set(statusResponse.getBPosition());
                        head.get().headXPosition.set(statusResponse.getHeadXPosition());
                        head.get().headYPosition.set(statusResponse.getHeadYPosition());
                        head.get().headZPosition.set(statusResponse.getHeadZPosition());
                        head.get().nozzleInUse.set(statusResponse.getNozzleInUse());
                    }

                    checkHeadEEPROM(statusResponse);

                    checkReelEEPROMs(statusResponse);

                    break;

                case FIRMWARE_RESPONSE:
                    FirmwareResponse fwResponse = (FirmwareResponse) rxPacket;
                    printerIdentity.firmwareVersion.set(fwResponse.getFirmwareRevision());
                    break;

                case PRINTER_ID_RESPONSE:
                    PrinterIDResponse idResponse = (PrinterIDResponse) rxPacket;
                    printerIdentity.printermodel.set(idResponse.getModel());
                    printerIdentity.printeredition.set(idResponse.getEdition());
                    printerIdentity.printerweekOfManufacture.set(idResponse.getWeekOfManufacture());
                    printerIdentity.printeryearOfManufacture.set(idResponse.getYearOfManufacture());
                    printerIdentity.printerpoNumber.set(idResponse.getPoNumber());
                    printerIdentity.printerserialNumber.set(idResponse.getSerialNumber());
                    printerIdentity.printercheckByte.set(idResponse.getCheckByte());
                    printerIdentity.printerFriendlyName.set(idResponse.getPrinterFriendlyName());
                    printerIdentity.printerColour.set(idResponse.getPrinterColour());

                    if (idResponse.getPrinterColour() != null)
                    {
                        try
                        {
                            setAmbientLEDColour(idResponse.getPrinterColour());

                        } catch (PrinterException ex)
                        {
                            steno.warning("Couldn't set printer LED colour");
                        }
                    }
                    break;

                case REEL_EEPROM_DATA:
                    ReelEEPROMDataResponse reelResponse = (ReelEEPROMDataResponse) rxPacket;

                    if (!filamentContainer.isFilamentIDInDatabase(reelResponse.getReelFilamentID()))
                    {
                        // unrecognised reel
                        saveUnknownFilamentToDatabase(reelResponse);
                    }

                    Reel reel;
                    if (!reels.containsKey(reelResponse.getReelNumber()))
                    {
                        reel = new Reel();
                        reel.updateFromEEPROMData(reelResponse);
                        reels.put(reelResponse.getReelNumber(), reel);
                    } else
                    {
                        reel = reels.get(reelResponse.getReelNumber());
                        reel.updateFromEEPROMData(reelResponse);
                    }

                    if (filamentContainer.isFilamentIDInDatabase(reelResponse.getReelFilamentID()))
                    {
                        // Check to see if the data is in bounds
                        RepairResult result = reels.get(reelResponse.getReelNumber()).
                            bringDataInBounds();

                        switch (result)
                        {
                            case REPAIRED_WRITE_ONLY:
                                try
                                {
                                    writeReelEEPROM(reelResponse.getReelNumber(), reels.get(
                                                    reelResponse.getReelNumber()));
                                    steno.info("Automatically updated reel data");
                                    Lookup.getSystemNotificationHandler().
                                        showReelUpdatedNotification();
                                } catch (RoboxCommsException ex)
                                {
                                    steno.error("Error updating reel after repair " + ex.
                                        getMessage());
                                }
                                break;
                        }
                    }
                    break;

                case HEAD_EEPROM_DATA:
//                    steno.info("Head EEPROM data received");

                    HeadEEPROMDataResponse headResponse = (HeadEEPROMDataResponse) rxPacket;

                    if (!suppressEEPROMAndSDErrorHandling)
                    {
                        if (Head.isTypeCodeValid(headResponse.getTypeCode()))
                        {
                            // Might be unrecognised but correct format for a Robox head type code

                            if (Head.isTypeCodeInDatabase(headResponse.getTypeCode()))
                            {
                                if (head.get() == null)
                                {
                                    // No head attached to model
                                    Head newHead = Head.createHead(headResponse);
                                    head.set(newHead);
                                } else
                                {
                                    // Head already attached to model
                                    head.get().updateFromEEPROMData(headResponse);
                                }

                                // Check to see if the data is in bounds
                                // Suppress the check if we are calibrating, since out of bounds data is used during this operation
                                if (!headIntegrityChecksInhibited)
                                {
                                    RepairResult result = head.get().bringDataInBounds();

                                    switch (result)
                                    {
                                        case REPAIRED_WRITE_ONLY:
                                            try
                                            {
                                                writeHeadEEPROM(head.get());
                                                steno.info(
                                                    "Automatically updated head data - no calibration required");
                                                Lookup.getSystemNotificationHandler().
                                                    showHeadUpdatedNotification();
                                            } catch (RoboxCommsException ex)
                                            {
                                                steno.error("Error updating head after repair "
                                                    + ex.
                                                    getMessage());
                                            }
                                            break;
                                        case REPAIRED_WRITE_AND_RECALIBRATE:
                                            try
                                            {
                                                writeHeadEEPROM(head.get());
                                                Lookup.getSystemNotificationHandler().
                                                    showCalibrationDialogue();
                                                steno.info(
                                                    "Automatically updated head data - calibration suggested");
                                            } catch (RoboxCommsException ex)
                                            {
                                                steno.error("Error updating head after repair "
                                                    + ex.
                                                    getMessage());
                                            }
                                            break;
                                    }
                                }
                            } else
                            {
                                // We don't recognise the head but it seems to be valid
                                Lookup.getSystemNotificationHandler().showHeadNotRecognisedDialog(
                                    printerIdentity.printerFriendlyName.get());
                                steno.error("Head with type code: " + headResponse.getTypeCode()
                                    + " attached. Not in database so ignoring...");
                            }
                        } else
                        {
                            if (!suppressEEPROMAndSDErrorHandling)
                            {
                                // Either not set or type code doesn't match Robox head type code
                                Lookup.getSystemNotificationHandler().showProgramInvalidHeadDialog(
                                    (TaskResponse<HeadFile> taskResponse) ->
                                    {
                                        HeadFile chosenHeadFile = taskResponse.getReturnedObject();

                                        if (chosenHeadFile != null)
                                        {
                                            Head chosenHead = new Head(chosenHeadFile);
                                            chosenHead.allocateRandomID();
                                            head.set(chosenHead);
                                            steno.info("Reprogrammed head as " + chosenHeadFile.
                                                getName()
                                                + " with ID " + head.get().uniqueID.get());
                                            try
                                            {
                                                writeHeadEEPROM(head.get());
                                                Lookup.getSystemNotificationHandler().
                                                showCalibrationDialogue();
                                                steno.info(
                                                    "Automatically updated head data - calibration suggested");
                                            } catch (RoboxCommsException ex)
                                            {
                                                steno.error("Error updating head after repair "
                                                    + ex.
                                                    getMessage());
                                            }
                                        } else
                                        {
                                            //Force the head prompt - we must have been cancelled
                                            lastHeadEEPROMState = EEPROMState.NOT_PRESENT;
                                        }
                                    });
                            }
                        }
                    } else
                    {
                        // We've been asked to suppress EEPROM error handling - load the data anyway
                        if (head.get() == null)
                        {
                            // No head attached to model
                            Head newHead = Head.createHead(headResponse);
                            head.set(newHead);
                        } else
                        {
                            // Head already attached to model
                            head.get().updateFromEEPROMData(headResponse);
                        }
                    }
                    break;

                case GCODE_RESPONSE:
                    break;

                case HOURS_COUNTER:
                    HoursCounterResponse hoursResponse = (HoursCounterResponse) rxPacket;
                    printerAncillarySystems.hoursCounter.set(hoursResponse.getHoursCounter());
                    break;

                default:
                    steno.warning("Unknown packet type delivered to Printer Status: "
                        + rxPacket.getPacketType().name());
                    break;
            }
        }

        private void processReelEEPROMData(ReelEEPROMDataResponse reelResponse)
        {
            if (!reels.containsKey(reelResponse.getReelNumber()))
            {
                Reel newReel = new Reel();
                newReel.updateFromEEPROMData(reelResponse);
                reels.put(reelResponse.getReelNumber(), newReel);
            } else
            {
                reels.get(reelResponse.getReelNumber()).updateFromEEPROMData(reelResponse);
            }
        }

        private void checkHeadEEPROM(StatusResponse statusResponse)
        {
            if (lastHeadEEPROMState != statusResponse.getHeadEEPROMState())
            {
                lastHeadEEPROMState = statusResponse.getHeadEEPROMState();
                switch (statusResponse.getHeadEEPROMState())
                {
                    case NOT_PRESENT:
                        head.set(null);
                        break;
                    case NOT_PROGRAMMED:
                        try
                        {
                            formatHeadEEPROM();
                        } catch (PrinterException ex)
                        {
                            steno.error("Error formatting head");
                        }
                        break;
                    case PROGRAMMED:
                        try
                        {
                            steno.info("About to read head EEPROM");
                            readHeadEEPROM();
                        } catch (RoboxCommsException ex)
                        {
                            steno.error("Error attempting to read head eeprom");
                            ex.printStackTrace();
                        }
                        break;
                }
            }
        }

        private void checkReelEEPROMs(StatusResponse statusResponse)
        {
            for (int reelNumber = 0; reelNumber < maxNumberOfReels; reelNumber++)
            {
                if (lastReelEEPROMState[reelNumber] != statusResponse.getReelEEPROMState(reelNumber))
                {
                    lastReelEEPROMState[reelNumber] = statusResponse.getReelEEPROMState(reelNumber);
                    switch (statusResponse.getReelEEPROMState(reelNumber))
                    {
                        case NOT_PRESENT:
                            reels.remove(reelNumber);
                            break;
                        case NOT_PROGRAMMED:
                            try
                            {
                                formatReelEEPROM(reelNumber);
                            } catch (PrinterException ex)
                            {
                                steno.error("Error formatting reel " + reelNumber);
                            }
                            break;
                        case PROGRAMMED:
                            try
                            {
                                readReelEEPROM(reelNumber);
                            } catch (RoboxCommsException ex)
                            {
                                steno.error("Error attempting to read reel " + reelNumber
                                    + " eeprom");
                            }
                            break;
                    }
                }
            }
        }

        /**
         * If the filament is not a Robox filament then update the database with the filament
         * details, if it is an unknown Robox filament then add it to the database in memory but do
         * not save it to disk.
         *
         * @param reelResponse
         */
        private void saveUnknownFilamentToDatabase(ReelEEPROMDataResponse reelResponse)
        {
            Filament filament = new Filament(reelResponse);
            if (filament.isMutable())
            {
                filamentContainer.saveFilament(filament);
            } else
            {
                filamentContainer.addFilamentToUserFilamentList(filament);
            }
        }
    };

    @Override
    public void resetHeadToDefaults() throws PrinterException
    {
        if (head.get() != null)
        {
            head.get().resetToDefaults();
            try
            {
                writeHeadEEPROM(head.get());
            } catch (RoboxCommsException ex)
            {
                steno.error("Error whilst writing default head EEPROM data - " + ex.getMessage());
            }
        } else
        {
            throw new PrinterException("Asked to reset head to defaults when no head was attached");
        }
    }

    @Override
    public void inhibitHeadIntegrityChecks(boolean inhibit)
    {
        headIntegrityChecksInhibited = inhibit;
    }

    @Override
    public void changeFeedRateMultiplier(double feedRate) throws PrinterException
    {
        steno.debug("Firing change feed rate multiplier: " + feedRate);

        try
        {
            SetFeedRateMultiplier setFeedRateMultiplier = (SetFeedRateMultiplier) RoboxTxPacketFactory.
                createPacket(
                    TxPacketTypeEnum.SET_FEED_RATE_MULTIPLIER);
            setFeedRateMultiplier.setFeedRateMultiplier(feedRate);
            commandInterface.writeToPrinter(setFeedRateMultiplier);
        } catch (RoboxCommsException ex)
        {
            steno.error("Comms exception when settings feed rate");
            throw new PrinterException("Comms exception when settings feed rate");
        }
    }

    @Override
    public void changeFilamentInfo(String extruderLetter,
        double filamentDiameter,
        double extrusionMultiplier) throws PrinterException
    {
        Extruder selectedExtruder = null;

        for (Extruder extruder : extruders)
        {
            if (extruder.getExtruderAxisLetter().equalsIgnoreCase(extruderLetter) && extruder.
                isFittedProperty().get())
            {
                selectedExtruder = extruder;
                break;
            }
        }

        if (selectedExtruder == null)
        {
            throw new PrinterException("Attempt to change filament info for non-existent extruder: "
                + extruderLetter);
        }

        steno.debug("Firing change filament info:"
            + "Extruder " + extruderLetter
            + " Diameter " + filamentDiameter
            + " Multiplier " + extrusionMultiplier);

        try
        {
            switch (extruderLetter)
            {
                case "E":
                    SetEFilamentInfo setEFilamentInfo = (SetEFilamentInfo) RoboxTxPacketFactory.
                        createPacket(
                            TxPacketTypeEnum.SET_E_FILAMENT_INFO);
                    setEFilamentInfo.setFilamentInfo(filamentDiameter, extrusionMultiplier);
                    commandInterface.writeToPrinter(setEFilamentInfo);
                    break;
                case "D":
                    SetDFilamentInfo setDFilamentInfo = (SetDFilamentInfo) RoboxTxPacketFactory.
                        createPacket(
                            TxPacketTypeEnum.SET_D_FILAMENT_INFO);
                    setDFilamentInfo.setFilamentInfo(filamentDiameter, extrusionMultiplier);
                    commandInterface.writeToPrinter(setDFilamentInfo);
                    break;
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Comms exception when setting filament info for extruder " + extruderLetter);
            throw new PrinterException("Comms exception when setting filament info for extruder "
                + extruderLetter);
        }
    }

    @Override
    public String toString()
    {
        return printerIdentity.printerFriendlyName.get();
    }
}
