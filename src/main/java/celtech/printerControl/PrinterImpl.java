package celtech.printerControl;

import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.FilamentContainer;
import celtech.configuration.Head;
import celtech.configuration.HeadContainer;
import celtech.configuration.HeaterMode;
import celtech.configuration.MaterialType;
import celtech.configuration.PauseStatus;
import celtech.configuration.PrintHead;
import celtech.configuration.WhyAreWeWaitingState;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ModalDialog;
import celtech.printerControl.comms.RoboxCommsManager;
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
import celtech.printerControl.comms.events.RoboxEvent;
import celtech.services.printing.DatafileSendAlreadyInProgress;
import celtech.services.printing.DatafileSendNotInitialised;
import celtech.services.printing.PrintQueue;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import celtech.utils.PrinterUtils;
import celtech.utils.SystemUtils;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import libertysystems.configuration.Configuration;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;

/**
 *
 * @author ianhudson
 */
public class PrinterImpl implements Printer
{

    private String portName = null;

    private String whyAreWeWaiting_cooling = null;
    private String whyAreWeWaiting_heatingBed = null;
    private String whyAreWeWaiting_heatingNozzle = null;

    private final static String USER_FILAMENT_PREFIX = "U";

    private final BooleanProperty printerIDDataChangedToggle = new SimpleBooleanProperty(false);
    private final StringProperty printermodel = new SimpleStringProperty("");
    private final StringProperty printeredition = new SimpleStringProperty("");
    private final StringProperty printerweekOfManufacture = new SimpleStringProperty("");
    private final StringProperty printeryearOfManufacture = new SimpleStringProperty("");
    private final StringProperty printerpoNumber = new SimpleStringProperty("");
    private final StringProperty printerserialNumber = new SimpleStringProperty("");
    private final StringProperty printercheckByte = new SimpleStringProperty("");
    private final StringProperty printerFriendlyName = new SimpleStringProperty("");
    private final ObjectProperty<Color> printerColour = new SimpleObjectProperty<>();

    /*
     * Temperature-related data
     */
    private final int NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP = 210;
    private final IntegerProperty ambientTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty ambientTargetTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty bedTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty bedFirstLayerTargetTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty bedTargetTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty nozzleTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty nozzleFirstLayerTargetTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty nozzleTargetTemperature = new SimpleIntegerProperty(0);

    private final LineChart.Series<Number, Number> ambientTemperatureHistory = new LineChart.Series<>();
    private ArrayList<LineChart.Data<Number, Number>> ambientTemperatureDataPoints = new ArrayList<>();
    private final LineChart.Series<Number, Number> bedTemperatureHistory = new LineChart.Series<>();
    private ArrayList<LineChart.Data<Number, Number>> bedTemperatureDataPoints = new ArrayList<>();
    private final LineChart.Series<Number, Number> nozzleTemperatureHistory = new LineChart.Series<>();
    private ArrayList<LineChart.Data<Number, Number>> nozzleTemperatureDataPoints = new ArrayList<>();
    private final LineChart.Series<Number, Number> ambientTargetTemperatureSeries = new LineChart.Series<>();
    private final LineChart.Series<Number, Number> bedTargetTemperatureSeries = new LineChart.Series<>();
    private final LineChart.Series<Number, Number> nozzleTargetTemperatureSeries = new LineChart.Series<>();
    private final LineChart.Data<Number, Number> ambientTargetPoint = new LineChart.Data<>(
        NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP - 10, 0);
    private final LineChart.Data<Number, Number> bedTargetPoint = new LineChart.Data<>(
        NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP - 10, 0);
    private final LineChart.Data<Number, Number> nozzleTargetPoint = new LineChart.Data<>(
        NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP - 10, 0);

    private long lastTimestamp = System.currentTimeMillis();

    private final StringProperty printerUniqueID = new SimpleStringProperty("");
    private final BooleanProperty printerConnected = new SimpleBooleanProperty(false);
    private final BooleanProperty ambientFanOn = new SimpleBooleanProperty(false);
    private final ObjectProperty<HeaterMode> bedHeaterMode = new SimpleObjectProperty<>();
    private final ObjectProperty<HeaterMode> nozzleHeaterMode = new SimpleObjectProperty<>();
    private final BooleanProperty headFanOn = new SimpleBooleanProperty(false);
    private final BooleanProperty errorsDetected = new SimpleBooleanProperty(false);
    private final StringProperty firmwareVersion = new SimpleStringProperty();
    private final FloatProperty headXPosition = new SimpleFloatProperty(0);
    private final FloatProperty headYPosition = new SimpleFloatProperty(0);
    private final FloatProperty headZPosition = new SimpleFloatProperty(0);
    private final FloatProperty BPosition = new SimpleFloatProperty(0);
    private final ObjectProperty<PrinterStatusEnumeration> printerStatus = new SimpleObjectProperty<>(
        PrinterStatusEnumeration.IDLE);
    // make the initial value to non-zero so that we get a change event when set to 0
    private final IntegerProperty printJobLineNumber = new SimpleIntegerProperty(-1);
    private final StringProperty printJobID = new SimpleStringProperty();
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final ObjectProperty<WhyAreWeWaitingState> whyAreWeWaitingState = new SimpleObjectProperty(
        WhyAreWeWaitingState.NOT_WAITING);
    private final StringProperty whyAreWeWaitingString = new SimpleStringProperty("");
    private ObjectProperty<PauseStatus> pauseStatus = new SimpleObjectProperty(PauseStatus.NOT_PAUSED);
    private final BooleanProperty SDCardError = new SimpleBooleanProperty(false);
    private final BooleanProperty ChunkSequenceError = new SimpleBooleanProperty(false);
    private final BooleanProperty FileTooLargeError = new SimpleBooleanProperty(false);
    private final BooleanProperty GCodeLineTooLongError = new SimpleBooleanProperty(false);
    private final BooleanProperty USBRxError = new SimpleBooleanProperty(false);
    private final BooleanProperty USBTxError = new SimpleBooleanProperty(false);
    private final BooleanProperty BadCommandError = new SimpleBooleanProperty(false);
    private final BooleanProperty EEPROMError = new SimpleBooleanProperty(false);
    private final BooleanProperty XStopSwitch = new SimpleBooleanProperty(false);
    private final BooleanProperty YStopSwitch = new SimpleBooleanProperty(false);
    private final BooleanProperty ZStopSwitch = new SimpleBooleanProperty(false);
    private final BooleanProperty ZTopStopSwitch = new SimpleBooleanProperty(false);
    private final BooleanProperty Filament1Loaded = new SimpleBooleanProperty(false);
    private final BooleanProperty Filament2Loaded = new SimpleBooleanProperty(false);
    private final BooleanProperty Filament1Index = new SimpleBooleanProperty(false);
    private final BooleanProperty Filament2Index = new SimpleBooleanProperty(false);
    private final BooleanProperty reelButton = new SimpleBooleanProperty(false);
    private final ObjectProperty<EEPROMState> reelEEPROMStatus = new SimpleObjectProperty<>(
        EEPROMState.NOT_PRESENT);
    private final StringProperty reelFriendlyName = new SimpleStringProperty();
    private MaterialType reelMaterialType;
    private Color reelDisplayColour;

    /*
     * Head parameters  - consider moving to a separate object?
     */
    private final Head temporaryHead = new Head(null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    private final BooleanProperty headDataChangedToggle = new SimpleBooleanProperty(false);
    private final ObjectProperty<EEPROMState> headEEPROMStatus = new SimpleObjectProperty<>(
        EEPROMState.NOT_PRESENT);
    private final StringProperty headTypeCode = new SimpleStringProperty("");
    private final StringProperty headType = new SimpleStringProperty("");
    private final StringProperty headUniqueID = new SimpleStringProperty("");
    private final FloatProperty headMaximumTemperature = new SimpleFloatProperty(0);
    private final FloatProperty headThermistorBeta = new SimpleFloatProperty(0);
    private final FloatProperty headThermistorTCal = new SimpleFloatProperty(0);
    private final FloatProperty headNozzle1XOffset = new SimpleFloatProperty(0);
    private final FloatProperty headNozzle1YOffset = new SimpleFloatProperty(0);
    private final FloatProperty headNozzle1ZOffset = new SimpleFloatProperty(0);
    private final FloatProperty headNozzle1BOffset = new SimpleFloatProperty(0);
    private final FloatProperty headNozzle2XOffset = new SimpleFloatProperty(0);
    private final FloatProperty headNozzle2YOffset = new SimpleFloatProperty(0);
    private final FloatProperty headNozzle2ZOffset = new SimpleFloatProperty(0);
    private final FloatProperty headNozzle2BOffset = new SimpleFloatProperty(0);
    private final FloatProperty lastFilamentTemperature = new SimpleFloatProperty(0);
    private final FloatProperty headHoursCounter = new SimpleFloatProperty(0);
    private final ObjectProperty<Head> attachedHead = new SimpleObjectProperty<>();

    /* 
     * Reel parameters
     */
    /*
     * Reel data
     */
    private final Filament temporaryFilament = new Filament(null, null, null,
                                                            0, 0, 0, 0, 0, 0, 0, 0, Color.ALICEBLUE, false);
    private final BooleanProperty reelDataChangedToggle = new SimpleBooleanProperty(false);
    private final BooleanProperty reelFilamentIsMutable = new SimpleBooleanProperty(false);
    private final IntegerProperty reelAmbientTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty reelBedTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty reelFirstLayerBedTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty reelFirstLayerNozzleTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty reelNozzleTemperature = new SimpleIntegerProperty(0);
    private final FloatProperty reelFilamentDiameter = new SimpleFloatProperty(0);
    private final FloatProperty reelFeedRateMultiplier = new SimpleFloatProperty(0);
    private final FloatProperty reelFilamentMultiplier = new SimpleFloatProperty(0);
    private final FloatProperty reelRemainingFilament = new SimpleFloatProperty(0);
    private final StringProperty reelFilamentID = new SimpleStringProperty("unknown");

    private final BooleanProperty NozzleHomed = new SimpleBooleanProperty(false);
    private final BooleanProperty LidOpen = new SimpleBooleanProperty(false);
    private final StringProperty errorList = new SimpleStringProperty();
    private final ObjectProperty<Filament> loadedFilament = new SimpleObjectProperty<Filament>(
        temporaryFilament);

    private final BooleanProperty sdCardPresent = new SimpleBooleanProperty(false);

    /*
    
     */
    private final ObservableList<String> gcodeTranscript = FXCollections.observableArrayList();

    private final PrintQueue printQueue = new PrintQueue(this);

    /*
     * From printer interface
     */
    private int dataFileSequenceNumber = 0;

    /*
     * From printer utils
     */
    private String fileID = null;
    private int sequenceNumber = 0;
    private static final int bufferSize = 512;
    private static final StringBuffer outputBuffer = new StringBuffer(bufferSize);
    private boolean printInitiated = false;
    /*
     * 
     */
    private final Stenographer steno = StenographerFactory.getStenographer(this.getClass().getName());
    private Configuration applicationConfiguration = null;
    /*
     * 
     */
    private ModalDialog noSDDialog = null;
    private ResourceBundle languageBundle = null;

    private RoboxCommsManager printerCommsManager = null;

    private static final Dialogs.CommandLink clearOnly = new Dialogs.CommandLink(
        DisplayManager.getLanguageBundle().getString("dialogs.error.clearOnly"), null);
    private static final Dialogs.CommandLink clearAndContinue = new Dialogs.CommandLink(
        DisplayManager.getLanguageBundle().getString("dialogs.error.clearAndContinue"), null);
    private static final Dialogs.CommandLink abortJob = new Dialogs.CommandLink(
        DisplayManager.getLanguageBundle().getString("dialogs.error.abortJob"), null);
    private boolean errorDialogOnDisplay = false;
    
    private boolean formatDenied = false;

    /**
     *
     * @param portName
     * @param commsManager
     */
    public PrinterImpl(String portName, RoboxCommsManager commsManager)
    {
        this.portName = portName;
        this.printerCommsManager = commsManager;

        languageBundle = DisplayManager.getLanguageBundle();

        initialiseSDDialog();

        ambientTemperatureHistory.setName(languageBundle.getString(
            "printerStatus.temperatureGraphAmbientLabel"));
        bedTemperatureHistory.setName(languageBundle.getString(
            "printerStatus.temperatureGraphBedLabel"));
        nozzleTemperatureHistory.setName(languageBundle.getString(
            "printerStatus.temperatureGraphNozzleLabel"));

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

        ResourceBundle i18nBundle = DisplayManager.getLanguageBundle();
        whyAreWeWaiting_cooling = i18nBundle.getString("printerStatus.printerCooling");
        whyAreWeWaiting_heatingBed = i18nBundle.getString("printerStatus.printerBedHeating");
        whyAreWeWaiting_heatingNozzle = i18nBundle.getString("printerStatus.printerNozzleHeating");
    }

    void initialiseSDDialog()
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                noSDDialog = new ModalDialog();
                noSDDialog.setTitle(languageBundle.getString("dialogs.noSDCardTitle"));
                noSDDialog.setMessage(languageBundle.getString("dialogs.noSDCardMessage"));
                noSDDialog.addButton(languageBundle.getString("dialogs.noSDCardOK"));
            }
        });
    }
    
    public StringProperty getPrinterUniqueIDProperty()
    {
        return printerUniqueID;
    }

    /**
     *
     * @return
     */
    @Override
    public String getPrinterPort()
    {
        return portName;
    }

    /**
     *
     * @return
     */
    @Override
    public BooleanProperty getPrinterIDDataChangedToggle()
    {
        return printerIDDataChangedToggle;
    }

    /**
     *
     * @return
     */
    @Override
    public StringProperty getPrintermodel()
    {
        return printermodel;
    }

    /**
     *
     * @return
     */
    @Override
    public StringProperty getPrinteredition()
    {
        return printeredition;
    }

    /**
     *
     * @return
     */
    @Override
    public StringProperty getPrinterweekOfManufacture()
    {
        return printerweekOfManufacture;
    }

    /**
     *
     * @return
     */
    @Override
    public StringProperty getPrinteryearOfManufacture()
    {
        return printeryearOfManufacture;
    }

    /**
     *
     * @return
     */
    @Override
    public StringProperty getPrinterpoNumber()
    {
        return printerpoNumber;
    }

    /**
     *
     * @return
     */
    @Override
    public StringProperty getPrinterserialNumber()
    {
        return printerserialNumber;
    }

    /**
     *
     * @return
     */
    @Override
    public StringProperty getPrintercheckByte()
    {
        return printercheckByte;
    }

    /**
     *
     * @return
     */
    @Override
    public String getPrinterUniqueID()
    {
        return printermodel.get()
            + printeredition.get()
            + printerweekOfManufacture.get()
            + printeryearOfManufacture.get()
            + printerpoNumber.get()
            + printerserialNumber.get()
            + printercheckByte.get();
    }

    /**
     *
     * @return
     */
    @Override
    public StringProperty printerFriendlyNameProperty()
    {
        return printerFriendlyName;
    }

    /**
     *
     * @return
     */
    @Override
    public String getPrinterFriendlyName()
    {
        return printerFriendlyName.get();
    }

    /**
     *
     * @param value
     */
    @Override
    public void setPrinterColour(Color value)
    {
        printerColour.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public Color getPrinterColour()
    {
        return printerColour.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final ObjectProperty<Color> printerColourProperty()
    {
        return printerColour;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setHeadXPosition(float value)
    {
        headXPosition.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final float getHeadXPosition()
    {
        return headXPosition.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final FloatProperty headXPositionProperty()
    {
        return headXPosition;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setHeadYPosition(float value)
    {
        headYPosition.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final float getHeadYPosition()
    {
        return headYPosition.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final FloatProperty headYPositionProperty()
    {
        return headYPosition;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setHeadZPosition(float value)
    {
        headZPosition.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final float getHeadZPosition()
    {
        return headZPosition.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final FloatProperty headZPositionProperty()
    {
        return headZPosition;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setBPosition(float value)
    {
        BPosition.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final float getBPosition()
    {
        return BPosition.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final FloatProperty BPositionProperty()
    {
        return BPosition;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setPrinterConnected(boolean value)
    {
        printerConnected.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final boolean getPrinterConnected()
    {
        return printerConnected.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty printerConnectedProperty()
    {
        return printerConnected;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setAmbientTemperature(int value)
    {
        ambientTemperature.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final float getAmbientTemperature()
    {
        return ambientTemperature.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final IntegerProperty ambientTemperatureProperty()
    {
        return ambientTemperature;
    }

    /**
     *
     * @return
     */
    @Override
    public final XYChart.Series<Number, Number> ambientTemperatureHistory()
    {
        return ambientTemperatureHistory;
    }

    /**
     *
     * @return
     */
    @Override
    public final XYChart.Series<Number, Number> ambientTargetTemperatureHistory()
    {
        return ambientTargetTemperatureSeries;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setAmbientTargetTemperature(int value)
    {
        ambientTargetTemperature.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final int getAmbientTargetTemperature()
    {
        return ambientTargetTemperature.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final IntegerProperty ambientTargetTemperatureProperty()
    {
        return ambientTargetTemperature;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setAmbientFanOn(boolean value)
    {
        ambientFanOn.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final boolean getAmbientFanOn()
    {
        return ambientFanOn.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty ambientFanOnProperty()
    {
        return ambientFanOn;
    }

    /**
     *
     * @return
     */
    @Override
    public final HeaterMode getBedHeaterMode()
    {
        return bedHeaterMode.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final ObjectProperty<HeaterMode> getBedHeaterModeProperty()
    {
        return bedHeaterMode;
    }

    /**
     *
     * @return
     */
    @Override
    public final HeaterMode getNozzleHeaterMode()
    {
        return nozzleHeaterMode.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final ObjectProperty<HeaterMode> getNozzleHeaterModeProperty()
    {
        return nozzleHeaterMode;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setBedTemperature(int value)
    {
        bedTemperature.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final int getBedTemperature()
    {
        return bedTemperature.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final IntegerProperty bedTemperatureProperty()
    {
        return bedTemperature;
    }

    /**
     *
     * @return
     */
    @Override
    public final XYChart.Series<Number, Number> bedTemperatureHistory()
    {
        return bedTemperatureHistory;
    }

    /**
     *
     * @return
     */
    @Override
    public final XYChart.Series<Number, Number> bedTargetTemperatureHistory()
    {
        return bedTargetTemperatureSeries;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setBedFirstLayerTargetTemperature(int value)
    {
        bedFirstLayerTargetTemperature.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final int getBedFirstLayerTargetTemperature()
    {
        return bedFirstLayerTargetTemperature.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final IntegerProperty bedFirstLayerTargetTemperatureProperty()
    {
        return bedFirstLayerTargetTemperature;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setBedTargetTemperature(int value)
    {
        bedTargetTemperature.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final int getBedTargetTemperature()
    {
        return bedTargetTemperature.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final IntegerProperty bedTargetTemperatureProperty()
    {
        return bedTargetTemperature;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setExtruderTemperature(int value)
    {
        nozzleTemperature.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final int getExtruderTemperature()
    {
        return nozzleTemperature.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final IntegerProperty extruderTemperatureProperty()
    {
        return nozzleTemperature;
    }

    /**
     *
     * @return
     */
    @Override
    public final XYChart.Series<Number, Number> nozzleTemperatureHistory()
    {
        return nozzleTemperatureHistory;
    }

    /**
     *
     * @return
     */
    @Override
    public final XYChart.Series<Number, Number> nozzleTargetTemperatureHistory()
    {
        return nozzleTargetTemperatureSeries;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setNozzleFirstLayerTargetTemperature(int value)
    {
        nozzleFirstLayerTargetTemperature.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final int getNozzleFirstLayerTargetTemperature()
    {
        return nozzleFirstLayerTargetTemperature.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final IntegerProperty nozzleFirstLayerTargetTemperatureProperty()
    {
        return nozzleFirstLayerTargetTemperature;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setNozzleTargetTemperature(int value)
    {
        nozzleTargetTemperature.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final int getNozzleTargetTemperature()
    {
        return nozzleTargetTemperature.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final IntegerProperty nozzleTargetTemperatureProperty()
    {
        return nozzleTargetTemperature;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setHeadFanOn(boolean value)
    {
        headFanOn.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final boolean getHeadFanOn()
    {
        return headFanOn.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty headFanOnProperty()
    {
        return headFanOn;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setBusy(boolean value)
    {
        busy.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final boolean getBusy()
    {
        return busy.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty busyProperty()
    {
        return busy;
    }

    /**
     *
     * @param value
     */
    public final void setPauseStatus(PauseStatus value)
    {
        pauseStatus.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final PauseStatus getPauseStatus()
    {
        return pauseStatus.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final ObjectProperty<PauseStatus> pauseStatusProperty()
    {
        return pauseStatus;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setWhyAreWeWaiting(WhyAreWeWaitingState value)
    {
        whyAreWeWaitingState.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final WhyAreWeWaitingState getWhyAreWeWaiting()
    {
        return whyAreWeWaitingState.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final StringProperty getWhyAreWeWaitingStringProperty()
    {
        return whyAreWeWaitingString;
    }

    /**
     *
     * @return
     */
    @Override
    public final ObjectProperty<WhyAreWeWaitingState> whyAreWeWaitingProperty()
    {
        return whyAreWeWaitingState;
    }

    /*
     * Errors
     */
    /**
     *
     * @param value
     */
    @Override
    public final void setErrorsDetected(boolean value)
    {
        errorsDetected.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final boolean getErrorsDetected()
    {
        return errorsDetected.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty errorsDetectedProperty()
    {
        return errorsDetected;
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty SDCardErrorProperty()
    {
        return SDCardError;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getSDCardError()
    {
        return SDCardError.get();
    }

    /**
     *
     * @param SDCardError
     */
    @Override
    public void setSDCardError(boolean SDCardError)
    {
        this.SDCardError.set(SDCardError);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty chunkSequenceErrorProperty()
    {
        return ChunkSequenceError;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getChunkSequenceError()
    {
        return ChunkSequenceError.get();
    }

    /**
     *
     * @param ChunkSequenceError
     */
    @Override
    public void setChunkSequenceError(boolean ChunkSequenceError)
    {
        this.ChunkSequenceError.set(ChunkSequenceError);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty fileTooLargeErrorProperty()
    {
        return FileTooLargeError;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getFileTooLargeError()
    {
        return FileTooLargeError.get();
    }

    /**
     *
     * @param FileTooLargeError
     */
    @Override
    public void setFileTooLargeError(boolean FileTooLargeError)
    {
        this.FileTooLargeError.set(FileTooLargeError);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty GCodeLineTooLongErrorProperty()
    {
        return GCodeLineTooLongError;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getGCodeLineTooLongError()
    {
        return GCodeLineTooLongError.get();
    }

    /**
     *
     * @param GCodeLineTooLongError
     */
    @Override
    public void setGCodeLineTooLongError(boolean GCodeLineTooLongError)
    {
        this.GCodeLineTooLongError.set(GCodeLineTooLongError);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty USBRxErrorProperty()
    {
        return USBRxError;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getUSBRxError()
    {
        return USBRxError.get();
    }

    /**
     *
     * @param USBRxError
     */
    @Override
    public void setUSBRxError(boolean USBRxError)
    {
        this.USBRxError.set(USBRxError);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty USBTxErrorProperty()
    {
        return USBTxError;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getUSBTxError()
    {
        return USBTxError.get();
    }

    /**
     *
     * @param USBTxError
     */
    @Override
    public void setUSBTxError(boolean USBTxError)
    {
        this.USBTxError.set(USBTxError);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty BadCommandErrorProperty()
    {
        return BadCommandError;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getBadCommandError()
    {
        return BadCommandError.get();
    }

    /**
     *
     * @param BadCommandError
     */
    @Override
    public void setBadCommandError(boolean BadCommandError)
    {
        this.BadCommandError.set(BadCommandError);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty EEPROMErrorProperty()
    {
        return EEPROMError;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getEEPROMError()
    {
        return EEPROMError.get();
    }

    /**
     *
     * @param EEPROMError
     */
    @Override
    public void setEEPROMError(boolean EEPROMError)
    {
        this.EEPROMError.set(EEPROMError);
    }

    /*
     * Switches
     */
    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty XStopSwitchProperty()
    {
        return XStopSwitch;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getXStopSwitch()
    {
        return XStopSwitch.get();
    }

    /**
     *
     * @param XStopSwitch
     */
    @Override
    public void setXStopSwitch(boolean XStopSwitch)
    {
        this.XStopSwitch.set(XStopSwitch);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty YStopSwitchProperty()
    {
        return YStopSwitch;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getYStopSwitch()
    {
        return YStopSwitch.get();
    }

    /**
     *
     * @param YStopSwitch
     */
    @Override
    public void setYStopSwitch(boolean YStopSwitch)
    {
        this.YStopSwitch.set(YStopSwitch);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty ZStopSwitchProperty()
    {
        return ZStopSwitch;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getZStopSwitch()
    {
        return ZStopSwitch.get();
    }

    /**
     *
     * @param ZStopSwitch
     */
    @Override
    public void setZStopSwitch(boolean ZStopSwitch)
    {
        this.ZStopSwitch.set(ZStopSwitch);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty ZTopStopSwitchProperty()
    {
        return ZTopStopSwitch;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getZTopStopSwitch()
    {
        return ZTopStopSwitch.get();
    }

    /**
     *
     * @param value
     */
    @Override
    public void setZTopStopSwitch(boolean value)
    {
        this.ZTopStopSwitch.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty Filament1LoadedProperty()
    {
        return Filament1Loaded;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getFilament1Loaded()
    {
        return Filament1Loaded.get();
    }

    /**
     *
     * @param FilamentLoaded
     */
    @Override
    public void setFilament1Loaded(boolean FilamentLoaded)
    {
        this.Filament1Loaded.set(FilamentLoaded);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty Filament2LoadedProperty()
    {
        return Filament2Loaded;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getFilament2Loaded()
    {
        return Filament2Loaded.get();
    }

    /**
     *
     * @param FilamentLoaded
     */
    @Override
    public void setFilament2Loaded(boolean FilamentLoaded)
    {
        this.Filament2Loaded.set(FilamentLoaded);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty Filament1IndexProperty()
    {
        return Filament1Index;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getFilament1Index()
    {
        return Filament1Index.get();
    }

    /**
     *
     * @param FilamentIndex
     */
    @Override
    public void setFilament1Index(boolean FilamentIndex)
    {
        this.Filament1Index.set(FilamentIndex);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty Filament2IndexProperty()
    {
        return Filament2Index;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getFilament2Index()
    {
        return Filament2Index.get();
    }

    /**
     *
     * @param FilamentIndex
     */
    @Override
    public void setFilament2Index(boolean FilamentIndex)
    {
        this.Filament2Index.set(FilamentIndex);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty reelButtonProperty()
    {
        return reelButton;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getReelButton()
    {
        return reelButton.get();
    }

    /**
     *
     * @param FilamentIndex
     */
    @Override
    public void setReelButton(boolean FilamentIndex)
    {
        this.reelButton.set(FilamentIndex);
    }

    /*
     * Head data
     */
    /**
     *
     * @return
     */
    @Override
    public ObjectProperty<Head> attachedHeadProperty()
    {
        return attachedHead;
    }

    /**
     *
     * @return
     */
    @Override
    public BooleanProperty getHeadDataChangedToggle()
    {
        return headDataChangedToggle;
    }

    /**
     *
     * @return
     */
    @Override
    public final ObjectProperty<EEPROMState> headEEPROMStatusProperty()
    {
        return headEEPROMStatus;
    }

    /**
     *
     * @return
     */
    @Override
    public EEPROMState getHeadEEPROMStatus()
    {
        return headEEPROMStatus.get();
    }

    /**
     *
     * @return
     */
    @Override
    public StringProperty getHeadTypeCode()
    {
        return headTypeCode;
    }

    /**
     *
     * @return
     */
    @Override
    public StringProperty getHeadType()
    {
        return headType;
    }

    /**
     *
     * @return
     */
    @Override
    public StringProperty getHeadUniqueID()
    {
        return headUniqueID;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getHeadMaximumTemperature()
    {
        return headMaximumTemperature;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getHeadThermistorBeta()
    {
        return headThermistorBeta;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getHeadThermistorTCal()
    {
        return headThermistorTCal;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getHeadNozzle1XOffset()
    {
        return headNozzle1XOffset;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getHeadNozzle1YOffset()
    {
        return headNozzle1YOffset;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getHeadNozzle1ZOffset()
    {
        return headNozzle1ZOffset;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getHeadNozzle1BOffset()
    {
        return headNozzle1BOffset;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getHeadNozzle2XOffset()
    {
        return headNozzle2XOffset;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getHeadNozzle2YOffset()
    {
        return headNozzle2YOffset;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getHeadNozzle2ZOffset()
    {
        return headNozzle2ZOffset;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getHeadNozzle2BOffset()
    {
        return headNozzle2BOffset;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getHeadHoursCounter()
    {
        return headHoursCounter;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getLastFilamentTemperature()
    {
        return lastFilamentTemperature;
    }
    /*
     * Reel data
     */

    /**
     *
     * @return
     */
    @Override
    public ObjectProperty<Filament> loadedFilamentProperty()
    {
        return loadedFilament;
    }

    /**
     *
     * @return
     */
    @Override
    public BooleanProperty reelDataChangedProperty()
    {
        return reelDataChangedToggle;
    }

    /**
     *
     * @return
     */
    @Override
    public final ObjectProperty<EEPROMState> reelEEPROMStatusProperty()
    {
        return reelEEPROMStatus;
    }

    /**
     *
     * @return
     */
    @Override
    public EEPROMState getReelEEPROMStatus()
    {
        return reelEEPROMStatus.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final StringProperty reelFriendlyNameProperty()
    {
        return reelFriendlyName;
    }

    /**
     *
     * @return
     */
    @Override
    public String getReelFriendlyName()
    {
        return reelFriendlyName.get();
    }

    @Override
    public MaterialType getReelMaterialType()
    {
        return reelMaterialType;
    }

    @Override
    public Color getReelDisplayColour()
    {
        return reelDisplayColour;
    }

    /**
     *
     * @return
     */
    @Override
    public IntegerProperty getReelAmbientTemperature()
    {
        return reelAmbientTemperature;
    }

    /**
     *
     * @return
     */
    @Override
    public IntegerProperty getReelBedTemperature()
    {
        return reelBedTemperature;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getReelFilamentDiameter()
    {
        return reelFilamentDiameter;
    }

    /**
     *
     * @return
     */
    @Override
    public IntegerProperty getReelFirstLayerBedTemperature()
    {
        return reelFirstLayerBedTemperature;
    }

    /**
     *
     * @return
     */
    @Override
    public IntegerProperty getReelFirstLayerNozzleTemperature()
    {
        return reelFirstLayerNozzleTemperature;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getReelFilamentMultiplier()
    {
        return reelFilamentMultiplier;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getReelFeedRateMultiplier()
    {
        return reelFeedRateMultiplier;
    }

    /**
     *
     * @return
     */
    @Override
    public IntegerProperty getReelNozzleTemperature()
    {
        return reelNozzleTemperature;
    }

    /**
     *
     * @return
     */
    @Override
    public FloatProperty getReelRemainingFilament()
    {
        return reelRemainingFilament;
    }

    /**
     *
     * @return
     */
    @Override
    public StringProperty getReelFilamentID()
    {
        return reelFilamentID;
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty NozzleHomedProperty()
    {
        return NozzleHomed;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getNozzleHomed()
    {
        return NozzleHomed.get();
    }

    /**
     *
     * @param NozzleHomed
     */
    @Override
    public void setNozzleHomed(boolean NozzleHomed)
    {
        this.NozzleHomed.set(NozzleHomed);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty LidOpenProperty()
    {
        return LidOpen;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean getLidOpen()
    {
        return LidOpen.get();
    }

    /**
     *
     * @param LidOpen
     */
    @Override
    public void setLidOpen(boolean LidOpen)
    {
        this.LidOpen.set(LidOpen);
    }

    /**
     *
     * @return
     */
    @Override
    public final BooleanProperty sdCardPresentProperty()
    {
        return sdCardPresent;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean sdCardPresent()
    {
        return sdCardPresent.get();
    }

    /*
     * Firmware
     */
    /**
     *
     * @param value
     */
    @Override
    public final void setFirmwareVersion(String value)
    {
        firmwareVersion.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final String getFirmwareVersion()
    {
        return firmwareVersion.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final StringProperty firmwareVersionProperty()
    {
        return firmwareVersion;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setPrinterStatus(PrinterStatusEnumeration value)
    {
        printerStatus.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final PrinterStatusEnumeration getPrinterStatus()
    {
        return printerStatus.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final ObjectProperty<PrinterStatusEnumeration> printerStatusProperty()
    {
        return printerStatus;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setPrintJobLineNumber(int value)
    {
        printJobLineNumber.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final int getPrintJobLineNumber()
    {
        return printJobLineNumber.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final IntegerProperty printJobLineNumberProperty()
    {
        return printJobLineNumber;
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setPrintJobID(String value)
    {
        printJobID.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final String getPrintJobID()
    {
        return printJobID.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final StringProperty printJobIDProperty()
    {
        return printJobID;
    }

    /*
     * Error lists for tooltips
     */
    /**
     *
     * @param value
     */
    @Override
    public final void setErrorList(String value)
    {
        errorList.set(value);
    }

    /**
     *
     * @return
     */
    @Override
    public final String getErrorList()
    {
        return errorList.get();
    }

    /**
     *
     * @return
     */
    @Override
    public final StringProperty errorListProperty()
    {
        return errorList;
    }

    /**
     *
     * @param printerEvent
     */
    @Override
    public void processRoboxEvent(RoboxEvent printerEvent)
    {
        switch (printerEvent.getEventType())
        {
            case PRINTER_CONNECTED:
                setPrinterConnected(true);
                break;
            case PRINTER_DISCONNECTED:
                setPrinterConnected(false);
                break;
            case PRINTER_ACK:
                handlePrinterAck(printerEvent);
                break;

            case PRINTER_STATUS_UPDATE:
                handlePrinterStatusUpdate(printerEvent);
                break;

            case PRINTER_INVALID_RESPONSE:
                setPrinterStatus(PrinterStatusEnumeration.ERROR);
                break;
            case PRINTER_COMMS_ERROR:
                setPrinterStatus(PrinterStatusEnumeration.ERROR);
                break;
            case FIRMWARE_VERSION_INFO:
                FirmwareResponse fwResponse = (FirmwareResponse) printerEvent.getPayload();
                setFirmwareVersion(fwResponse.getFirmwareRevision());
                break;
            case PRINTER_ID_INFO:
                handlePrinterIDInfo(printerEvent);
                break;
            case REEL_EEPROM_DATA:
                handleReelEEPROMData(printerEvent);
                break;
            case HEAD_EEPROM_DATA:
                handleHeadEEPROMData(printerEvent);
                break;
            default:
                steno.warning("Unknown packet type delivered to Printer Status: "
                    + printerEvent.getEventType().name());
                break;
        }
    }

    private void handleHeadEEPROMData(RoboxEvent printerEvent)
    {
        HeadEEPROMDataResponse headResponse = (HeadEEPROMDataResponse) printerEvent.getPayload();
        String headTypeCodeString = headResponse.getTypeCode();
        headTypeCode.set(headTypeCodeString);
        try
        {
            Head attachedHeadCandidate = HeadContainer.getHeadByID(headTypeCodeString);
            if (attachedHeadCandidate != null)
            {
                headType.set(
                    PrintHead.getPrintHeadForType(headTypeCodeString).getShortName());
                temporaryHead.setUniqueID(headResponse.getUniqueID());
                temporaryHead.setHeadHours(headResponse.getHeadHours());
                temporaryHead.setLastFilamentTemperature(
                    headResponse.getLastFilamentTemperature());
                temporaryHead.setMaximumTemperature(headResponse.getMaximumTemperature());
                temporaryHead.setNozzle1_B_offset(headResponse.getNozzle1BOffset());
                temporaryHead.setNozzle1_X_offset(headResponse.getNozzle1XOffset());
                temporaryHead.setNozzle1_Y_offset(headResponse.getNozzle1YOffset());
                temporaryHead.setNozzle1_Z_offset(headResponse.getNozzle1ZOffset());
                temporaryHead.setNozzle2_B_offset(headResponse.getNozzle2BOffset());
                temporaryHead.setNozzle2_X_offset(headResponse.getNozzle2XOffset());
                temporaryHead.setNozzle2_Y_offset(headResponse.getNozzle2YOffset());
                temporaryHead.setNozzle2_Z_offset(headResponse.getNozzle2ZOffset());
                temporaryHead.setBeta(headResponse.getBeta());
                temporaryHead.setTcal(headResponse.getTCal());
                temporaryHead.setFriendlyName(attachedHeadCandidate.getFriendlyName());
                attachedHead.set(temporaryHead);
            } else
            {
                headType.set("Unknown");
                attachedHead.set(null);
            }
        } catch (IllegalArgumentException ex)
        {
            headType.set("Unknown");
        }
        
        headUniqueID.set(headResponse.getUniqueID());
        lastFilamentTemperature.set(headResponse.getLastFilamentTemperature());
        headHoursCounter.set(headResponse.getHeadHours());
        headMaximumTemperature.set(headResponse.getMaximumTemperature());
        headNozzle1BOffset.set(headResponse.getNozzle1BOffset());
        headNozzle1XOffset.set(headResponse.getNozzle1XOffset());
        headNozzle1YOffset.set(headResponse.getNozzle1YOffset());
        headNozzle1ZOffset.set(headResponse.getNozzle1ZOffset());
        headNozzle2BOffset.set(headResponse.getNozzle2BOffset());
        headNozzle2XOffset.set(headResponse.getNozzle2XOffset());
        headNozzle2YOffset.set(headResponse.getNozzle2YOffset());
        headNozzle2ZOffset.set(headResponse.getNozzle2ZOffset());
        headThermistorBeta.set(headResponse.getBeta());
        headThermistorTCal.set(headResponse.getTCal());
        
        headDataChangedToggle.set(!headDataChangedToggle.get());
    }

    private void handleReelEEPROMData(RoboxEvent printerEvent)
    {
        ReelEEPROMDataResponse reelResponse = (ReelEEPROMDataResponse) printerEvent.getPayload();
        reelFilamentID.set(reelResponse.getReelFilamentID());
        try
        {
            Filament loadedFilamentCandidate = FilamentContainer.getFilamentByID(
                reelFilamentID.get());
            if (loadedFilamentCandidate != null)
            {
                temporaryFilament.setFriendlyFilamentName(
                    loadedFilamentCandidate.getFriendlyFilamentName());
                temporaryFilament.setMaterial(loadedFilamentCandidate.getMaterial());
                temporaryFilament.setFilamentID(reelResponse.getReelFilamentID());
                temporaryFilament.setDisplayColour(
                    loadedFilamentCandidate.getDisplayColour());
                temporaryFilament.setAmbientTemperature(reelResponse.getAmbientTemperature());
                temporaryFilament.setBedTemperature(reelResponse.getBedTemperature());
                temporaryFilament.setFirstLayerBedTemperature(
                    reelResponse.getFirstLayerBedTemperature());
                temporaryFilament.setNozzleTemperature(reelResponse.getNozzleTemperature());
                temporaryFilament.setFirstLayerNozzleTemperature(
                    reelResponse.getFirstLayerNozzleTemperature());
                temporaryFilament.setMutable(reelResponse.getReelFilamentID().startsWith(USER_FILAMENT_PREFIX));
                temporaryFilament.setFilamentMultiplier(reelResponse.getFilamentMultiplier());
                temporaryFilament.setFeedRateMultiplier(reelResponse.getFeedRateMultiplier());
                temporaryFilament.setRemainingFilament(
                    reelResponse.getReelRemainingFilament());
                temporaryFilament.setFilamentDiameter(reelResponse.getFilamentDiameter());
                loadedFilament.set(temporaryFilament);
                reelFriendlyName.set(loadedFilamentCandidate.toString());
            } else
            {
                reelFriendlyName.set(DisplayManager.getLanguageBundle().getString(
                    "sidePanel_settings.filamentUnknown"));
                loadedFilament.set(null);
            }
        } catch (IllegalArgumentException ex)
        {
            reelFriendlyName.set(DisplayManager.getLanguageBundle().getString(
                "sidePanel_settings.filamentUnknown"));
            loadedFilament.set(null);
        }
        
        reelFriendlyName.set(reelResponse.getReelFriendlyName());
        reelMaterialType = reelResponse.getReelMaterialType();
        reelDisplayColour = reelResponse.getReelDisplayColour();
        reelAmbientTemperature.set(reelResponse.getAmbientTemperature());
        reelBedTemperature.set(reelResponse.getBedTemperature());
        reelFirstLayerBedTemperature.set(reelResponse.getFirstLayerBedTemperature());
        reelNozzleTemperature.set(reelResponse.getNozzleTemperature());
        reelFirstLayerNozzleTemperature.set(reelResponse.getFirstLayerNozzleTemperature());
        reelFilamentMultiplier.set(reelResponse.getFilamentMultiplier());
        reelFeedRateMultiplier.set(reelResponse.getFeedRateMultiplier());
        reelRemainingFilament.set(reelResponse.getReelRemainingFilament());
        reelFilamentDiameter.set(reelResponse.getFilamentDiameter());
        reelFilamentIsMutable.set(reelFilamentID.get().startsWith(USER_FILAMENT_PREFIX));
        reelDataChangedToggle.set(!reelDataChangedToggle.get());
    }

    private void handlePrinterIDInfo(RoboxEvent printerEvent)
    {
        PrinterIDResponse idResponse = (PrinterIDResponse) printerEvent.getPayload();
        printermodel.set(idResponse.getModel());
        printeredition.set(idResponse.getEdition());
        printerweekOfManufacture.set(idResponse.getWeekOfManufacture());
        printeryearOfManufacture.set(idResponse.getYearOfManufacture());
        printerpoNumber.set(idResponse.getPoNumber());
        printerserialNumber.set(idResponse.getSerialNumber());
        printercheckByte.set(idResponse.getCheckByte());
        printerFriendlyName.set(idResponse.getPrinterFriendlyName());
        printerUniqueID.set(getPrinterUniqueID());
        setPrinterColour(idResponse.getPrinterColour());
        try
        {
            transmitSetAmbientLEDColour(idResponse.getPrinterColour());
            
        } catch (RoboxCommsException ex)
        {
            steno.warning("Couldn't set printer LED colour");
        }
        printerIDDataChangedToggle.set(!printerIDDataChangedToggle.get());
    }

    private void handlePrinterAck(RoboxEvent printerEvent)
    {
        AckResponse ackResponse = (AckResponse) printerEvent.getPayload();
        
        if (ackResponse.isError() && !errorDialogOnDisplay)
        {
            errorDialogOnDisplay = true;
            
            Action errorHandlingResponse = null;
            
            if (getPrintQueue().getPrintStatus() != PrinterStatusEnumeration.IDLE
                && getPrintQueue().getPrintStatus() != PrinterStatusEnumeration.ERROR)
            {
                errorHandlingResponse = Dialogs.create().title(
                    DisplayManager.getLanguageBundle().getString(
                        "dialogs.error.errorEncountered"))
                    .message(ackResponse.getErrorsAsString())
                    .masthead(null)
                    .showCommandLinks(clearAndContinue, clearAndContinue, abortJob);
            } else
            {
                errorHandlingResponse = Dialogs.create().title(
                    DisplayManager.getLanguageBundle().getString(
                        "dialogs.error.errorEncountered"))
                    .message(ackResponse.getErrorsAsString())
                    .masthead(null)
                    .showCommandLinks(clearOnly, clearOnly);
            }
            
            try
            {
                transmitResetErrors();
            } catch (RoboxCommsException ex)
            {
                steno.error("Couldn't reset errors after error detection");
            }
            
            if (errorHandlingResponse == clearAndContinue)
            {
                try
                {
                    if (pauseStatus.get() == PauseStatus.PAUSED
                        || pauseStatus.get() == PauseStatus.PAUSE_PENDING)
                    {
                        transmitResumePrint();
                    }
                } catch (RoboxCommsException ex)
                {
                    steno.error("Couldn't reset errors and resume after error");
                }
                
            } else if (errorHandlingResponse == abortJob)
            {
                try
                {
                    if (pauseStatus.get() == PauseStatus.NOT_PAUSED
                        || pauseStatus.get() == PauseStatus.RESUME_PENDING)
                    {
                        transmitPausePrint();
                    }
                    transmitAbortPrint();
                } catch (RoboxCommsException ex)
                {
                    steno.error("Couldn't abort print after error");
                }
            }
            
            errorDialogOnDisplay = false;
        }
        
        setErrorsDetected(ackResponse.isError());
        setSDCardError(ackResponse.isSdCardError());
        setChunkSequenceError(ackResponse.isChunkSequenceError());
        setFileTooLargeError(ackResponse.isFileTooLargeError());
        setGCodeLineTooLongError(ackResponse.isGcodeLineTooLongError());
        setUSBRxError(ackResponse.isUsbRXError());
        setUSBTxError(ackResponse.isUsbTXError());
        setBadCommandError(ackResponse.isBadCommandError());
        setEEPROMError(ackResponse.isHeadEepromError());
    }

    private void handlePrinterStatusUpdate(RoboxEvent printerEvent)
    {
        StatusResponse statusResponse = (StatusResponse) printerEvent.getPayload();
        
        setAmbientTemperature(statusResponse.getAmbientTemperature());
        setAmbientTargetTemperature(statusResponse.getAmbientTargetTemperature());
        setBedTemperature(statusResponse.getBedTemperature());
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
            
            for (int pointCounter = 0; pointCounter < NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP - 1;
                pointCounter++)
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
        
        setAmbientFanOn(statusResponse.isAmbientFanOn());
        bedHeaterMode.set(statusResponse.getBedHeaterMode());
        nozzleHeaterMode.set(statusResponse.getNozzleHeaterMode());
        setHeadFanOn(statusResponse.isHeadFanOn());
        setBusy(statusResponse.isBusyStatus());
        
        if (pauseStatus.get() != statusResponse.getPauseStatus()
            && statusResponse.getPauseStatus() == PauseStatus.PAUSED)
        {
            printQueue.printerHasPaused();
        } else if (pauseStatus.get() != statusResponse.getPauseStatus()
            && statusResponse.getPauseStatus() == PauseStatus.NOT_PAUSED)
        {
            printQueue.printerHasResumed();
        }
        setPauseStatus(statusResponse.getPauseStatus());
        
        setPrintJobLineNumber(statusResponse.getPrintJobLineNumber());
        setPrintJobID(statusResponse.getRunningPrintJobID());
        setXStopSwitch(statusResponse.isxSwitchStatus());
        setYStopSwitch(statusResponse.isySwitchStatus());
        setZStopSwitch(statusResponse.iszSwitchStatus());
        setZTopStopSwitch(statusResponse.isTopZSwitchStatus());
        setFilament1Loaded(statusResponse.isFilament1SwitchStatus());
        setFilament2Loaded(statusResponse.isFilament2SwitchStatus());
        setFilament1Index(statusResponse.isEIndexStatus());
        setFilament2Index(statusResponse.isDIndexStatus());
        setNozzleHomed(statusResponse.isNozzleSwitchStatus());
        setLidOpen(statusResponse.isLidSwitchStatus());
        setReelButton(statusResponse.isReelButtonStatus());
        
        if (reelEEPROMStatus.get() != EEPROMState.PROGRAMMED
            && statusResponse.getReelEEPROMState() == EEPROMState.PROGRAMMED)
        {
            Filament.repairFilamentIfNecessary(this);
        } else if (reelEEPROMStatus.get() != EEPROMState.NOT_PRESENT
            && statusResponse.getReelEEPROMState() == EEPROMState.NOT_PRESENT)
        {
            loadedFilament.set(null);
            reelFriendlyName.set(DisplayManager.getLanguageBundle().getString(
                "smartReelProgrammer.noReelLoaded"));
            reelAmbientTemperature.set(0);
            reelBedTemperature.set(0);
            reelFirstLayerBedTemperature.set(0);
            reelNozzleTemperature.set(0);
            reelFirstLayerNozzleTemperature.set(0);
            reelFilamentMultiplier.set(0);
            reelFeedRateMultiplier.set(0);
            reelRemainingFilament.set(0);
            reelFilamentDiameter.set(0);
            reelFilamentIsMutable.set(false);
            reelDataChangedToggle.set(!reelDataChangedToggle.get());
        }
        
        reelEEPROMStatus.set(statusResponse.getReelEEPROMState());
        
        if (statusResponse.getHeadEEPROMState() == EEPROMState.NOT_PROGRAMMED && ! formatDenied) {
            if (Head.checkHead(this)) {
                try
                {
                    transmitFormatHeadEEPROM();
                } catch (RoboxCommsException ex)
                {
                    steno.error("Did not format head successfully");
                }
            } else {
                formatDenied = true;
            }
        }        
        
        if (headEEPROMStatus.get() != EEPROMState.PROGRAMMED
            && statusResponse.getHeadEEPROMState() == EEPROMState.PROGRAMMED)
        {
            Head.repairHeadIfNecessary(this);
        } else if (headEEPROMStatus.get() != EEPROMState.NOT_PRESENT
            && statusResponse.getHeadEEPROMState() == EEPROMState.NOT_PRESENT)
        {
            attachedHead.set(null);
            headType.set(null);
            temporaryHead.setUniqueID(null);
            temporaryHead.setHeadHours(0);
            temporaryHead.setMaximumTemperature(0);
            temporaryHead.setNozzle1_B_offset(0);
            temporaryHead.setNozzle1_X_offset(0);
            temporaryHead.setNozzle1_Y_offset(0);
            temporaryHead.setNozzle1_Z_offset(0);
            temporaryHead.setNozzle2_B_offset(0);
            temporaryHead.setNozzle2_X_offset(0);
            temporaryHead.setNozzle2_Y_offset(0);
            temporaryHead.setNozzle2_Z_offset(0);
            temporaryHead.setBeta(0);
            temporaryHead.setTcal(0);
        }
        
        headEEPROMStatus.set(statusResponse.getHeadEEPROMState());
        
        sdCardPresent.set(statusResponse.isSDCardPresent());
        showSDDialogIfNotShowing(statusResponse);
        
        setHeadXPosition(statusResponse.getHeadXPosition());
        setHeadYPosition(statusResponse.getHeadYPosition());
        setHeadZPosition(statusResponse.getHeadZPosition());
        
        setWhyAreWeWaiting(statusResponse.getWhyAreWeWaitingState());
        switch (statusResponse.getWhyAreWeWaitingState())
        {
            case NOT_WAITING:
                whyAreWeWaitingString.set("");
                break;
            case BED_HEATING:
                whyAreWeWaitingString.set(whyAreWeWaiting_heatingBed);
                break;
            case NOZZLE_HEATING:
                whyAreWeWaitingString.set(whyAreWeWaiting_heatingNozzle);
                break;
            case COOLING:
                whyAreWeWaitingString.set(whyAreWeWaiting_cooling);
                break;
            default:
                whyAreWeWaitingString.set("");
                break;
        }
        
        setPrinterStatus(printQueue.getPrintStatus());
    }

    void showSDDialogIfNotShowing(StatusResponse statusResponse)
    {
        if (statusResponse.isSDCardPresent() == false)
        {
            if (!noSDDialog.isShowing())
            {
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        noSDDialog.show();
                    }
                });
            }
        }
    }

    private void setupErrorList(AckResponse response)
    {
        StringBuilder errors = new StringBuilder();

        /*
         * These should only appear in expert mode
         */
        if (response.isGcodeLineTooLongError())
        {
            if (errors.length() > 0)
            {
                errors.append('\r');
            }

            errors.append("Line too long <internal>");
        }

        setErrorList(errors.toString());
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        return printerFriendlyName.get();
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

    /**
     *
     * @return
     */
    @Override
    public PrintQueue getPrintQueue()
    {
        return printQueue;
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
    @Override
    public String transmitDirectGCode(final String gcodeToSend, boolean addToTranscript) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.EXECUTE_GCODE);

        String gcodeToSendWithLF = SystemUtils.cleanGCodeForTransmission(gcodeToSend) + "\n";

        gcodePacket.setMessagePayload(gcodeToSendWithLF);

        if (addToTranscript)
        {
            addToGCodeTranscript(gcodeToSendWithLF);
        }
        GCodeDataResponse response = (GCodeDataResponse) printerCommsManager.submitForWrite(portName,
                                                                                            gcodePacket);
        if (addToTranscript)
        {
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    if (response == null)
                    {
                        addToGCodeTranscript(languageBundle.getString("gcodeEntry.errorMessage"));
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
    @Override
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
    @Override
    public void transmitStoredGCode(final String macroName, boolean checkForPurge) throws RoboxCommsException
    {
        if (printQueue.getPrintStatus() == PrinterStatusEnumeration.IDLE)
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

        AckResponse response = (AckResponse) printerCommsManager.submitForWrite(portName,
                                                                                gcodePacket);
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

        AckResponse response = (AckResponse) printerCommsManager.submitForWrite(portName,
                                                                                gcodePacket);
        dataFileSequenceNumber++;

        return response;
    }

    private AckResponse transmitDataFileEnd(final String payloadData, final int sequenceNumber) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.END_OF_DATA_FILE);
        gcodePacket.setMessagePayload(payloadData);
        gcodePacket.setSequenceNumber(sequenceNumber);

        return (AckResponse) printerCommsManager.submitForWrite(portName, gcodePacket);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    @Override
    public AckResponse transmitReportErrors() throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.REPORT_ERRORS);

        return (AckResponse) printerCommsManager.submitForWrite(portName, gcodePacket);
    }

    /**
     *
     * @throws RoboxCommsException
     */
    @Override
    public void transmitResetErrors() throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.RESET_ERRORS);

        printerCommsManager.submitForWrite(portName, gcodePacket);
    }

    /**
     *
     * @param firmwareID
     * @return
     * @throws RoboxCommsException
     */
    @Override
    public boolean transmitUpdateFirmware(final String firmwareID) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.UPDATE_FIRMWARE);
        gcodePacket.setMessagePayload(firmwareID);

        AckResponse response = (AckResponse) printerCommsManager.submitForWrite(portName,
                                                                                gcodePacket);

        return (response.isError());
    }

    private void transmitInitiatePrint(final String printJobUUID) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.INITIATE_PRINT);
        gcodePacket.setMessagePayload(printJobUUID);

        printerCommsManager.submitForWrite(portName, gcodePacket);
    }

    /**
     *
     * @throws RoboxCommsException
     */
    @Override
    public void transmitAbortPrint() throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.ABORT_PRINT);

        printerCommsManager.submitForWrite(portName, gcodePacket);
    }

    /**
     *
     * @throws RoboxCommsException
     */
    @Override
    public void transmitPausePrint() throws RoboxCommsException
    {
        PausePrint gcodePacket = (PausePrint) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.PAUSE_RESUME_PRINT);
        gcodePacket.setPause();

        printerCommsManager.submitForWrite(portName, gcodePacket);
    }

    /**
     *
     * @throws RoboxCommsException
     */
    @Override
    public void transmitResumePrint() throws RoboxCommsException
    {
        PausePrint gcodePacket = (PausePrint) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.PAUSE_RESUME_PRINT);
        gcodePacket.setResume();

        printerCommsManager.submitForWrite(portName, gcodePacket);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    @Override
    public AckResponse transmitFormatHeadEEPROM() throws RoboxCommsException
    {
        FormatHeadEEPROM formatHead = (FormatHeadEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.FORMAT_HEAD_EEPROM);
        return (AckResponse) printerCommsManager.submitForWrite(portName, formatHead);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    @Override
    public AckResponse transmitFormatReelEEPROM() throws RoboxCommsException
    {
        FormatReelEEPROM formatReel = (FormatReelEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.FORMAT_REEL_EEPROM);
        return (AckResponse) printerCommsManager.submitForWrite(portName, formatReel);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    @Override
    public ReelEEPROMDataResponse transmitReadReelEEPROM() throws RoboxCommsException
    {
        ReadReelEEPROM readReel = (ReadReelEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.READ_REEL_EEPROM);
        return (ReelEEPROMDataResponse) printerCommsManager.submitForWrite(portName, readReel);
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    @Override
    public HeadEEPROMDataResponse transmitReadHeadEEPROM() throws RoboxCommsException
    {
        ReadHeadEEPROM readHead = (ReadHeadEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.READ_HEAD_EEPROM);
        return (HeadEEPROMDataResponse) printerCommsManager.submitForWrite(portName, readHead);
    }

    /**
     *
     * @param filament
     * @return
     * @throws RoboxCommsException
     */
    @Override
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
                                       filament.getFilamentDiameter(),
                                       filament.getFilamentMultiplier(),
                                       filament.getFeedRateMultiplier(),
                                       filament.getRemainingFilament(),
                                       filament.getFriendlyFilamentName(),
                                       filament.getMaterial(),
                                       filament.getDisplayColour());
        return (AckResponse) printerCommsManager.submitForWrite(portName, writeReelEEPROM);
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
        printerCommsManager.submitForWrite(portName, writeReelEEPROM);
    }

    /**
     *
     * @param headToWrite
     * @throws RoboxCommsException
     */
    @Override
    public void transmitWriteHeadEEPROM(Head headToWrite) throws RoboxCommsException
    {
        WriteHeadEEPROM writeHeadEEPROM = (WriteHeadEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.WRITE_HEAD_EEPROM);
        writeHeadEEPROM.populateEEPROM(headToWrite.getTypeCode(),
                                       headToWrite.getUniqueID(),
                                       headToWrite.getMaximumTemperature(),
                                       headToWrite.getBeta(),
                                       headToWrite.getTCal(),
                                       headToWrite.getNozzle1XOffset(),
                                       headToWrite.getNozzle1YOffset(),
                                       headToWrite.getNozzle1ZOffset(),
                                       headToWrite.getNozzle1BOffset(),
                                       headToWrite.getNozzle2XOffset(),
                                       headToWrite.getNozzle2YOffset(),
                                       headToWrite.getNozzle2ZOffset(),
                                       headToWrite.getNozzle2BOffset(),
                                       headToWrite.getLastFilamentTemperature(),
                                       headToWrite.getHeadHours());
        printerCommsManager.submitForWrite(portName, writeHeadEEPROM);
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
        return (AckResponse) printerCommsManager.submitForWrite(portName, writeHeadEEPROM);
    }

    /*
     * Higher level controls
     */
    /**
     *
     * @param on
     * @throws RoboxCommsException
     */
    @Override
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
    @Override
    public void transmitSetAmbientLEDColour(Color colour) throws RoboxCommsException
    {
        SetAmbientLEDColour ledColour = (SetAmbientLEDColour) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.SET_AMBIENT_LED_COLOUR);
        ledColour.setLEDColour(colour);
        printerCommsManager.submitForWrite(portName, ledColour);
    }

    /**
     *
     * @param colour
     * @throws RoboxCommsException
     */
    @Override
    public void transmitSetReelLEDColour(Color colour) throws RoboxCommsException
    {
        SetReelLEDColour ledColour = (SetReelLEDColour) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.SET_REEL_LED_COLOUR);
        ledColour.setLEDColour(colour);
        printerCommsManager.submitForWrite(portName, ledColour);
    }

    /**
     *
     * @throws RoboxCommsException
     */
    @Override
    public void transmitReadPrinterID() throws RoboxCommsException
    {
        ReadPrinterID readId = (ReadPrinterID) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.READ_PRINTER_ID);
        printerCommsManager.submitForWrite(portName, readId);
    }

    /**
     *
     * @param model
     * @param edition
     * @param weekOfManufacture
     * @param yearOfManufacture
     * @param poNumber
     * @param serialNumber
     * @param checkByte
     * @param printerFriendlyName
     * @param colour
     * @return
     * @throws RoboxCommsException
     */
    @Override
    public boolean transmitWritePrinterID(String model, String edition, String weekOfManufacture,
        String yearOfManufacture, String poNumber, String serialNumber, String checkByte,
        String printerFriendlyName, Color colour) throws RoboxCommsException
    {
        WritePrinterID writeIDCmd = (WritePrinterID) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.WRITE_PRINTER_ID);
        writeIDCmd.setIDAndColour(model, edition, weekOfManufacture, yearOfManufacture, poNumber,
                                  serialNumber, checkByte, printerFriendlyName, colour);

        AckResponse response = (AckResponse) printerCommsManager.submitForWrite(portName, writeIDCmd);

        return !response.isError();
    }

    /**
     *
     * @return @throws RoboxCommsException
     */
    @Override
    public FirmwareResponse transmitReadFirmwareVersion() throws RoboxCommsException
    {
        QueryFirmwareVersion readFirmware = (QueryFirmwareVersion) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.QUERY_FIRMWARE_VERSION);
        return (FirmwareResponse) printerCommsManager.submitForWrite(portName, readFirmware);
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
    @Override
    public void transmitSetTemperatures(double nozzleFirstLayerTarget, double nozzleTarget,
        double bedFirstLayerTarget, double bedTarget, double ambientTarget) throws RoboxCommsException
    {
        SetTemperatures setTemperatures = (SetTemperatures) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.SET_TEMPERATURES);
        setTemperatures.setTemperatures(nozzleFirstLayerTarget, nozzleTarget, bedFirstLayerTarget,
                                        bedTarget, ambientTarget);
        printerCommsManager.submitForWrite(portName, setTemperatures);
    }

    /**
     *
     * @param filamentDiameter
     * @param filamentMultiplier
     * @param feedRateMultiplier
     * @throws RoboxCommsException
     */
    @Override
    public void transmitSetFilamentInfo(double filamentDiameter, double filamentMultiplier,
        double feedRateMultiplier) throws RoboxCommsException
    {
        SetFilamentInfo setFilamentInfo = (SetFilamentInfo) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.SET_FILAMENT_INFO);
        setFilamentInfo.setFilamentInfo(filamentDiameter, filamentMultiplier, feedRateMultiplier);
        printerCommsManager.submitForWrite(portName, setFilamentInfo);
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
        return (ListFilesResponse) printerCommsManager.submitForWrite(portName, listFiles);
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
        return (StatusResponse) printerCommsManager.submitForWrite(portName, statusRequest);
    }

    /**
     *
     * @param fileID
     * @return
     * @throws DatafileSendAlreadyInProgress
     * @throws RoboxCommsException
     */
    @Override
    public boolean initialiseDataFileSend(String fileID) throws DatafileSendAlreadyInProgress, RoboxCommsException
    {
        boolean success = false;
        this.fileID = fileID;
        success = transmitDataFileStart(fileID);
        outputBuffer.delete(0, outputBuffer.length());
        sequenceNumber = 0;
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
                steno.info("Final complete chunk:" + outputBuffer.toString() + " seq:"
                    + sequenceNumber);
                AckResponse response = transmitDataFileEnd(outputBuffer.toString(), sequenceNumber);
                if (response.isError())
                {
                    steno.error("Error sending final data file chunk - seq " + sequenceNumber);
                }
            } else if ((outputBuffer.capacity() - outputBuffer.length()) == 0)
            {
                /*
                 * Send when full
                 */
//                steno.info("Sending chunk seq:" + sequenceNumber);
                AckResponse response = transmitDataFileChunk(outputBuffer.toString(), sequenceNumber);
                if (response.isError())
                {
                    steno.error("Error sending data file chunk - seq " + sequenceNumber);
                }
                outputBuffer.delete(0, bufferSize);
                sequenceNumber++;
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
        PrintQualityEnumeration printQuality, RoboxProfile settings)
    {

        if (filament != null
            && filament != loadedFilamentProperty().get())
        {
            try
            {
                transmitSetTemperatures(filament.getFirstLayerNozzleTemperature(),
                                        filament.getNozzleTemperature(),
                                        filament.getFirstLayerBedTemperature(),
                                        filament.getBedTemperature(),
                                        filament.getAmbientTemperature());
                transmitSetFilamentInfo(filament.getFilamentDiameter(),
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
     */
    @Override
    public void abortPrint()
    {
        printQueue.abortPrint();
    }

    /**
     *
     */
    @Override
    public void pausePrint()
    {
        printQueue.pausePrint();
    }

    /**
     *
     */
    @Override
    public void resumePrint()
    {
        printQueue.resumePrint();
    }

    /**
     *
     * @return
     */
    @Override
    public int getSequenceNumber()
    {
        return sequenceNumber;
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

    /**
     *
     * @param reelNozzleTemperature
     */
    @Override
    public void transmitWriteMaterialTemperatureToHeadEEPROM(int reelNozzleTemperature)
    {

    }

    @Override
    public BooleanProperty getReelFilamentIsMutable()
    {
        return reelFilamentIsMutable;
    }
}
