/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl;

import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.configuration.FilamentContainer;
import celtech.configuration.Head;
import celtech.configuration.HeadContainer;
import celtech.configuration.PrintHead;
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
import celtech.printerControl.comms.commands.rx.PrinterIDResponse;
import celtech.printerControl.comms.commands.rx.ReelEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.comms.commands.tx.FormatHeadEEPROM;
import celtech.printerControl.comms.commands.tx.FormatReelEEPROM;
import celtech.printerControl.comms.commands.tx.PausePrint;
import celtech.printerControl.comms.commands.tx.QueryFirmwareVersion;
import celtech.printerControl.comms.commands.tx.ReadHeadEEPROM;
import celtech.printerControl.comms.commands.tx.ReadPrinterID;
import celtech.printerControl.comms.commands.tx.ReadReelEEPROM;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacketFactory;
import celtech.printerControl.comms.commands.tx.SetAmbientLEDColour;
import celtech.printerControl.comms.commands.tx.SetTemperatures;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;
import celtech.printerControl.comms.commands.tx.WriteHeadEEPROM;
import celtech.printerControl.comms.commands.tx.WritePrinterID;
import celtech.printerControl.comms.commands.tx.WriteReelEEPROM;
import celtech.printerControl.comms.events.RoboxEvent;
import celtech.services.printing.DatafileSendAlreadyInProgress;
import celtech.services.printing.DatafileSendNotInitialised;
import celtech.services.printing.PrintQueue;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SlicerSettings;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
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

/**
 *
 * @author ianhudson
 */
public class Printer
{

    private String portName = null;

    private BooleanProperty printerIDDataChangedToggle = new SimpleBooleanProperty(false);
    private StringProperty printermodel = new SimpleStringProperty("");
    private StringProperty printeredition = new SimpleStringProperty("");
    private StringProperty printerweekOfManufacture = new SimpleStringProperty("");
    private StringProperty printeryearOfManufacture = new SimpleStringProperty("");
    private StringProperty printerpoNumber = new SimpleStringProperty("");
    private StringProperty printerserialNumber = new SimpleStringProperty("");
    private StringProperty printercheckByte = new SimpleStringProperty("");
    private StringProperty printerFriendlyName = new SimpleStringProperty("");
    private final ObjectProperty<Color> printerColour = new SimpleObjectProperty<>();

    /*
     * Temperature-related data
     */
    private final int NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP = 180;
    private final IntegerProperty ambientTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty ambientTargetTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty bedTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty bedTargetTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty extruderTemperature = new SimpleIntegerProperty(0);
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
    private final LineChart.Data<Number, Number> ambientTargetPoint = new LineChart.Data<>(NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP + 5, 0);
    private final LineChart.Data<Number, Number> bedTargetPoint = new LineChart.Data<>(NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP + 5, 0);
    private final LineChart.Data<Number, Number> nozzleTargetPoint = new LineChart.Data<>(NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP + 5, 0);

    private long lastTimestamp = System.currentTimeMillis();

    private final BooleanProperty printerConnected = new SimpleBooleanProperty(false);
    private final BooleanProperty ambientFanOn = new SimpleBooleanProperty(false);
    private final BooleanProperty bedHeaterOn = new SimpleBooleanProperty(false);
    private final BooleanProperty nozzleHeaterOn = new SimpleBooleanProperty(false);
    private final BooleanProperty headFanOn = new SimpleBooleanProperty(false);
    private final BooleanProperty errorsDetected = new SimpleBooleanProperty(false);
    private final StringProperty firmwareVersion = new SimpleStringProperty();
    private final FloatProperty headXPosition = new SimpleFloatProperty(0);
    private FloatProperty headYPosition = new SimpleFloatProperty(0);
    private FloatProperty headZPosition = new SimpleFloatProperty(0);
    private FloatProperty EPosition = new SimpleFloatProperty(0);
    private FloatProperty DPosition = new SimpleFloatProperty(0);
    private FloatProperty BPosition = new SimpleFloatProperty(0);
    private final ObjectProperty<PrinterStatusEnumeration> printerStatus = new SimpleObjectProperty<>(PrinterStatusEnumeration.IDLE);
    private IntegerProperty printJobLineNumber = new SimpleIntegerProperty(0);
    private StringProperty printJobID = new SimpleStringProperty();
    private BooleanProperty busy = new SimpleBooleanProperty(false);
    private BooleanProperty paused = new SimpleBooleanProperty(false);

    private BooleanProperty SDCardError = new SimpleBooleanProperty(false);
    private BooleanProperty ChunkSequenceError = new SimpleBooleanProperty(false);
    private BooleanProperty FileTooLargeError = new SimpleBooleanProperty(false);
    private BooleanProperty GCodeLineTooLongError = new SimpleBooleanProperty(false);
    private BooleanProperty USBRxError = new SimpleBooleanProperty(false);
    private BooleanProperty USBTxError = new SimpleBooleanProperty(false);
    private BooleanProperty BadCommandError = new SimpleBooleanProperty(false);
    private BooleanProperty EEPROMError = new SimpleBooleanProperty(false);
    private BooleanProperty XStopSwitch = new SimpleBooleanProperty(false);
    private BooleanProperty YStopSwitch = new SimpleBooleanProperty(false);
    private BooleanProperty ZStopSwitch = new SimpleBooleanProperty(false);
    private BooleanProperty ZTopStopSwitch = new SimpleBooleanProperty(false);
    private BooleanProperty Filament1Loaded = new SimpleBooleanProperty(false);
    private BooleanProperty Filament2Loaded = new SimpleBooleanProperty(false);
    private BooleanProperty Filament1Index = new SimpleBooleanProperty(false);
    private BooleanProperty Filament2Index = new SimpleBooleanProperty(false);
    private BooleanProperty reelButton = new SimpleBooleanProperty(false);
    private BooleanProperty reelAttached = new SimpleBooleanProperty(false);
    private StringProperty reelFriendlyName = new SimpleStringProperty();

    /*
     * Head parameters  - consider moving to a separate object?
     */
    private BooleanProperty headDataChangedToggle = new SimpleBooleanProperty(false);
    private BooleanProperty headAttached = new SimpleBooleanProperty(false);
    private StringProperty headTypeCode = new SimpleStringProperty("");
    private StringProperty headType = new SimpleStringProperty("");
    private StringProperty headUniqueID = new SimpleStringProperty("");
    private FloatProperty headMaximumTemperature = new SimpleFloatProperty(0);
    private FloatProperty headThermistorBeta = new SimpleFloatProperty(0);
    private FloatProperty headThermistorTCal = new SimpleFloatProperty(0);
    private FloatProperty headNozzle1XOffset = new SimpleFloatProperty(0);
    private FloatProperty headNozzle1YOffset = new SimpleFloatProperty(0);
    private FloatProperty headNozzle1ZOffset = new SimpleFloatProperty(0);
    private FloatProperty headNozzle1BOffset = new SimpleFloatProperty(0);
    private FloatProperty headNozzle2XOffset = new SimpleFloatProperty(0);
    private FloatProperty headNozzle2YOffset = new SimpleFloatProperty(0);
    private FloatProperty headNozzle2ZOffset = new SimpleFloatProperty(0);
    private FloatProperty headNozzle2BOffset = new SimpleFloatProperty(0);
    private FloatProperty headHoursCounter = new SimpleFloatProperty(0);
    private ObjectProperty<Head> attachedHead = new SimpleObjectProperty<Head>();

    /* 
     * Reel parameters
     */
    /*
     * Reel data
     */
    private BooleanProperty reelDataChangedToggle = new SimpleBooleanProperty(false);
    private IntegerProperty reelAmbientTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty reelBedTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty reelFirstLayerBedTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty reelFirstLayerNozzleTemperature = new SimpleIntegerProperty(0);
    private IntegerProperty reelNozzleTemperature = new SimpleIntegerProperty(0);
    private FloatProperty reelFilamentDiameter = new SimpleFloatProperty(0);
    private FloatProperty reelExtrusionMultiplier = new SimpleFloatProperty(0);
    private FloatProperty reelMaxExtrusionRate = new SimpleFloatProperty(0);
    private FloatProperty reelRemainingFilament = new SimpleFloatProperty(0);
    private StringProperty reelTypeCode = new SimpleStringProperty("unknown");
    private StringProperty reelUniqueID = new SimpleStringProperty("unknown");

    private BooleanProperty NozzleHomed = new SimpleBooleanProperty(false);
    private BooleanProperty LidOpen = new SimpleBooleanProperty(false);
    private StringProperty errorList = new SimpleStringProperty();
    private ObjectProperty<Filament> loadedFilament = new SimpleObjectProperty<Filament>();

    private BooleanProperty sdCardPresent = new SimpleBooleanProperty(false);

    /*
    
     */
    private final ObservableList<String> gcodeTranscript = FXCollections.observableArrayList();

    private final PrintQueue printQueue = new PrintQueue();

    /*
     * From printer interface
     */
    private int dataFileSequenceNumber = 0;

    /*
     * From printer utils
     */
    private boolean sendInProgress = false;
    private String fileID = null;
    private int sequenceNumber = 0;
    private static final int bufferSize = 512;
    private static final StringBuffer outputBuffer = new StringBuffer(bufferSize);
    private boolean printInitiated = false;
    /*
     * 
     */
    private Stenographer steno = StenographerFactory.getStenographer(this.getClass().getName());
    private Configuration applicationConfiguration = null;
    /*
     * 
     */
    private ModalDialog noSDDialog = null;
    private ResourceBundle languageBundle = null;

    private RoboxCommsManager printerCommsManager = null;

    public Printer(String portName, RoboxCommsManager commsManager)
    {
        this.portName = portName;
        this.printerCommsManager = commsManager;

        languageBundle = DisplayManager.getLanguageBundle();

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

        ambientTemperatureHistory.setName(languageBundle.getString("printerStatus.temperatureGraphAmbientLabel"));
        bedTemperatureHistory.setName(languageBundle.getString("printerStatus.temperatureGraphBedLabel"));
        nozzleTemperatureHistory.setName(languageBundle.getString("printerStatus.temperatureGraphNozzleLabel"));

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
    }

    public String getPrinterPort()
    {
        return portName;
    }

    public BooleanProperty getPrinterIDDataChangedToggle()
    {
        return printerIDDataChangedToggle;
    }

    public StringProperty getPrintermodel()
    {
        return printermodel;
    }

    public StringProperty getPrinteredition()
    {
        return printeredition;
    }

    public StringProperty getPrinterweekOfManufacture()
    {
        return printerweekOfManufacture;
    }

    public StringProperty getPrinteryearOfManufacture()
    {
        return printeryearOfManufacture;
    }

    public StringProperty getPrinterpoNumber()
    {
        return printerpoNumber;
    }

    public StringProperty getPrinterserialNumber()
    {
        return printerserialNumber;
    }

    public StringProperty getPrintercheckByte()
    {
        return printercheckByte;
    }

    public StringProperty printerFriendlyNameProperty()
    {
        return printerFriendlyName;
    }

    public String getPrinterFriendlyName()
    {
        return printerFriendlyName.get();
    }

    public void setPrinterColour(Color value)
    {
        printerColour.set(value);
    }

    public Color getPrinterColour()
    {
        return printerColour.get();
    }

    public final ObjectProperty<Color> printerColourProperty()
    {
        return printerColour;
    }

    public final void setHeadXPosition(float value)
    {
        headXPosition.set(value);
    }

    public final float getHeadXPosition()
    {
        return headXPosition.get();
    }

    public final FloatProperty headXPositionProperty()
    {
        return headXPosition;
    }

    public final void setHeadYPosition(float value)
    {
        headYPosition.set(value);
    }

    public final float getHeadYPosition()
    {
        return headYPosition.get();
    }

    public final FloatProperty headYPositionProperty()
    {
        return headYPosition;
    }

    public final void setHeadZPosition(float value)
    {
        headZPosition.set(value);
    }

    public final float getHeadZPosition()
    {
        return headZPosition.get();
    }

    public final FloatProperty headZPositionProperty()
    {
        return headZPosition;
    }

    public final void setEPosition(float value)
    {
        EPosition.set(value);
    }

    public final float getEPosition()
    {
        return EPosition.get();
    }

    public final FloatProperty EPositionProperty()
    {
        return EPosition;
    }

    public final void setBPosition(float value)
    {
        BPosition.set(value);
    }

    public final float getBPosition()
    {
        return BPosition.get();
    }

    public final FloatProperty BPositionProperty()
    {
        return BPosition;
    }

    public final void setPrinterConnected(boolean value)
    {
        printerConnected.set(value);
    }

    public final boolean getPrinterConnected()
    {
        return printerConnected.get();
    }

    public final BooleanProperty printerConnectedProperty()
    {
        return printerConnected;
    }

    public final void setAmbientTemperature(int value)
    {
        ambientTemperature.set(value);
    }

    public final float getAmbientTemperature()
    {
        return ambientTemperature.get();
    }

    public final IntegerProperty ambientTemperatureProperty()
    {
        return ambientTemperature;
    }

    public final XYChart.Series<Number, Number> ambientTemperatureHistory()
    {
        return ambientTemperatureHistory;
    }

    public final XYChart.Series<Number, Number> ambientTargetTemperatureHistory()
    {
        return ambientTargetTemperatureSeries;
    }

    public final void setAmbientTargetTemperature(int value)
    {
        ambientTargetTemperature.set(value);
    }

    public final int getAmbientTargetTemperature()
    {
        return ambientTargetTemperature.get();
    }

    public final IntegerProperty ambientTargetTemperatureProperty()
    {
        return ambientTargetTemperature;
    }

    public final void setAmbientFanOn(boolean value)
    {
        ambientFanOn.set(value);
    }

    public final boolean getAmbientFanOn()
    {
        return ambientFanOn.get();
    }

    public final BooleanProperty ambientFanOnProperty()
    {
        return ambientFanOn;
    }

    public final void setBedTemperature(int value)
    {
        bedTemperature.set(value);
    }

    public final int getBedTemperature()
    {
        return bedTemperature.get();
    }

    public final IntegerProperty bedTemperatureProperty()
    {
        return bedTemperature;
    }

    public final XYChart.Series<Number, Number> bedTemperatureHistory()
    {
        return bedTemperatureHistory;
    }

    public final XYChart.Series<Number, Number> bedTargetTemperatureHistory()
    {
        return bedTargetTemperatureSeries;
    }

    public final void setBedTargetTemperature(int value)
    {
        bedTargetTemperature.set(value);
    }

    public final int getBedTargetTemperature()
    {
        return bedTargetTemperature.get();
    }

    public final IntegerProperty bedTargetTemperatureProperty()
    {
        return bedTargetTemperature;
    }

    public final void setBedHeaterOn(boolean value)
    {
        bedHeaterOn.set(value);
    }

    public final boolean getBedHeaterOn()
    {
        return bedHeaterOn.get();
    }

    public final BooleanProperty bedHeaterOnProperty()
    {
        return bedHeaterOn;
    }

    public final void setExtruderTemperature(int value)
    {
        extruderTemperature.set(value);
    }

    public final int getExtruderTemperature()
    {
        return extruderTemperature.get();
    }

    public final IntegerProperty extruderTemperatureProperty()
    {
        return extruderTemperature;
    }

    public final XYChart.Series<Number, Number> nozzleTemperatureHistory()
    {
        return nozzleTemperatureHistory;
    }

    public final XYChart.Series<Number, Number> nozzleTargetTemperatureHistory()
    {
        return nozzleTargetTemperatureSeries;
    }

    public final void setNozzleTargetTemperature(int value)
    {
        nozzleTargetTemperature.set(value);
    }

    public final int getNozzleTargetTemperature()
    {
        return nozzleTargetTemperature.get();
    }

    public final IntegerProperty extruderTargetTemperatureProperty()
    {
        return nozzleTargetTemperature;
    }

    public final void setNozzleHeaterOn(boolean value)
    {
        nozzleHeaterOn.set(value);
    }

    public final boolean getNozzleHeaterOn()
    {
        return nozzleHeaterOn.get();
    }

    public final BooleanProperty nozzleHeaterOnProperty()
    {
        return nozzleHeaterOn;
    }

    public final void setHeadFanOn(boolean value)
    {
        headFanOn.set(value);
    }

    public final boolean getHeadFanOn()
    {
        return headFanOn.get();
    }

    public final BooleanProperty headFanOnProperty()
    {
        return headFanOn;
    }

    public final void setBusy(boolean value)
    {
        busy.set(value);
    }

    public final boolean getBusy()
    {
        return busy.get();
    }

    public final BooleanProperty busyProperty()
    {
        return busy;
    }

    public final void setPaused(boolean value)
    {
        paused.set(value);
    }

    public final boolean getPaused()
    {
        return paused.get();
    }

    public final BooleanProperty pausedProperty()
    {
        return paused;
    }

    /*
     * Errors
     */
    public final void setErrorsDetected(boolean value)
    {
        errorsDetected.set(value);
    }

    public final boolean getErrorsDetected()
    {
        return errorsDetected.get();
    }

    public final BooleanProperty errorsDetectedProperty()
    {
        return errorsDetected;
    }

    public final BooleanProperty SDCardErrorProperty()
    {
        return SDCardError;
    }

    public boolean getSDCardError()
    {
        return SDCardError.get();
    }

    public void setSDCardError(boolean SDCardError)
    {
        this.SDCardError.set(SDCardError);
    }

    public final BooleanProperty chunkSequenceErrorProperty()
    {
        return ChunkSequenceError;
    }

    public boolean getChunkSequenceError()
    {
        return ChunkSequenceError.get();
    }

    public void setChunkSequenceError(boolean ChunkSequenceError)
    {
        this.ChunkSequenceError.set(ChunkSequenceError);
    }

    public final BooleanProperty fileTooLargeErrorProperty()
    {
        return FileTooLargeError;
    }

    public boolean getFileTooLargeError()
    {
        return FileTooLargeError.get();
    }

    public void setFileTooLargeError(boolean FileTooLargeError)
    {
        this.FileTooLargeError.set(FileTooLargeError);
    }

    public final BooleanProperty GCodeLineTooLongErrorProperty()
    {
        return GCodeLineTooLongError;
    }

    public boolean getGCodeLineTooLongError()
    {
        return GCodeLineTooLongError.get();
    }

    public void setGCodeLineTooLongError(boolean GCodeLineTooLongError)
    {
        this.GCodeLineTooLongError.set(GCodeLineTooLongError);
    }

    public final BooleanProperty USBRxErrorProperty()
    {
        return USBRxError;
    }

    public boolean getUSBRxError()
    {
        return USBRxError.get();
    }

    public void setUSBRxError(boolean USBRxError)
    {
        this.USBRxError.set(USBRxError);
    }

    public final BooleanProperty USBTxErrorProperty()
    {
        return USBTxError;
    }

    public boolean getUSBTxError()
    {
        return USBTxError.get();
    }

    public void setUSBTxError(boolean USBTxError)
    {
        this.USBTxError.set(USBTxError);
    }

    public final BooleanProperty BadCommandErrorProperty()
    {
        return BadCommandError;
    }

    public boolean getBadCommandError()
    {
        return BadCommandError.get();
    }

    public void setBadCommandError(boolean BadCommandError)
    {
        this.BadCommandError.set(BadCommandError);
    }

    public final BooleanProperty EEPROMErrorProperty()
    {
        return EEPROMError;
    }

    public boolean getEEPROMError()
    {
        return EEPROMError.get();
    }

    public void setEEPROMError(boolean EEPROMError)
    {
        this.EEPROMError.set(EEPROMError);
    }

    /*
     * Switches
     */
    public final BooleanProperty XStopSwitchProperty()
    {
        return XStopSwitch;
    }

    public boolean getXStopSwitch()
    {
        return XStopSwitch.get();
    }

    public void setXStopSwitch(boolean XStopSwitch)
    {
        this.XStopSwitch.set(XStopSwitch);
    }

    public final BooleanProperty YStopSwitchProperty()
    {
        return YStopSwitch;
    }

    public boolean getYStopSwitch()
    {
        return YStopSwitch.get();
    }

    public void setYStopSwitch(boolean YStopSwitch)
    {
        this.YStopSwitch.set(YStopSwitch);
    }

    public final BooleanProperty ZStopSwitchProperty()
    {
        return ZStopSwitch;
    }

    public boolean getZStopSwitch()
    {
        return ZStopSwitch.get();
    }

    public void setZStopSwitch(boolean ZStopSwitch)
    {
        this.ZStopSwitch.set(ZStopSwitch);
    }

    public final BooleanProperty ZTopStopSwitchProperty()
    {
        return ZTopStopSwitch;
    }

    public boolean getZTopStopSwitch()
    {
        return ZTopStopSwitch.get();
    }

    public void setZTopStopSwitch(boolean value)
    {
        this.ZTopStopSwitch.set(value);
    }

    public final BooleanProperty Filament1LoadedProperty()
    {
        return Filament1Loaded;
    }

    public boolean getFilament1Loaded()
    {
        return Filament1Loaded.get();
    }

    public void setFilament1Loaded(boolean FilamentLoaded)
    {
        this.Filament1Loaded.set(FilamentLoaded);
    }

    public final BooleanProperty Filament2LoadedProperty()
    {
        return Filament2Loaded;
    }

    public boolean getFilament2Loaded()
    {
        return Filament2Loaded.get();
    }

    public void setFilament2Loaded(boolean FilamentLoaded)
    {
        this.Filament2Loaded.set(FilamentLoaded);
    }

    public final BooleanProperty Filament1IndexProperty()
    {
        return Filament1Index;
    }

    public boolean getFilament1Index()
    {
        return Filament1Index.get();
    }

    public void setFilament1Index(boolean FilamentIndex)
    {
        this.Filament1Index.set(FilamentIndex);
    }

    public final BooleanProperty Filament2IndexProperty()
    {
        return Filament2Index;
    }

    public boolean getFilament2Index()
    {
        return Filament2Index.get();
    }

    public void setFilament2Index(boolean FilamentIndex)
    {
        this.Filament2Index.set(FilamentIndex);
    }

    public final BooleanProperty reelButtonProperty()
    {
        return reelButton;
    }

    public boolean getReelButton()
    {
        return reelButton.get();
    }

    public void setReelButton(boolean FilamentIndex)
    {
        this.reelButton.set(FilamentIndex);
    }

    /*
     * Head data
     */
    public ObjectProperty<Head> attachedHeadProperty()
    {
        return attachedHead;
    }

    public BooleanProperty getHeadDataChangedToggle()
    {
        return headDataChangedToggle;
    }

    public final BooleanProperty headAttachedProperty()
    {
        return headAttached;
    }

    public boolean getHeadAttached()
    {
        return headAttached.get();
    }

    public StringProperty getHeadTypeCode()
    {
        return headTypeCode;
    }

    public StringProperty getHeadType()
    {
        return headType;
    }

    public StringProperty getHeadUniqueID()
    {
        return headUniqueID;
    }

    public FloatProperty getHeadMaximumTemperature()
    {
        return headMaximumTemperature;
    }

    public FloatProperty getHeadThermistorBeta()
    {
        return headThermistorBeta;
    }

    public FloatProperty getHeadThermistorTCal()
    {
        return headThermistorTCal;
    }

    public FloatProperty getHeadNozzle1XOffset()
    {
        return headNozzle1XOffset;
    }

    public FloatProperty getHeadNozzle1YOffset()
    {
        return headNozzle1YOffset;
    }

    public FloatProperty getHeadNozzle1ZOffset()
    {
        return headNozzle1ZOffset;
    }

    public FloatProperty getHeadNozzle1BOffset()
    {
        return headNozzle1BOffset;
    }

    public FloatProperty getHeadNozzle2XOffset()
    {
        return headNozzle2XOffset;
    }

    public FloatProperty getHeadNozzle2YOffset()
    {
        return headNozzle2YOffset;
    }

    public FloatProperty getHeadNozzle2ZOffset()
    {
        return headNozzle2ZOffset;
    }

    public FloatProperty getHeadNozzle2BOffset()
    {
        return headNozzle2BOffset;
    }

    public FloatProperty getHeadHoursCounter()
    {
        return headHoursCounter;
    }

    /*
     * Reel data
     */
    public ObjectProperty<Filament> loadedFilamentProperty()
    {
        return loadedFilament;
    }

    public BooleanProperty reelDataChangedProperty()
    {
        return reelDataChangedToggle;
    }

    public final BooleanProperty reelAttachedProperty()
    {
        return reelAttached;
    }

    public boolean getReelAttached()
    {
        return reelAttached.get();
    }

    public final StringProperty reelFriendlyNameProperty()
    {
        return reelFriendlyName;
    }

    public String getReelFriendlyName()
    {
        return reelFriendlyName.get();
    }

    public IntegerProperty getReelAmbientTemperature()
    {
        return reelAmbientTemperature;
    }

    public IntegerProperty getReelBedTemperature()
    {
        return reelBedTemperature;
    }

    public FloatProperty getReelFilamentDiameter()
    {
        return reelFilamentDiameter;
    }

    public IntegerProperty getReelFirstLayerBedTemperature()
    {
        return reelFirstLayerBedTemperature;
    }

    public IntegerProperty getReelFirstLayerNozzleTemperature()
    {
        return reelFirstLayerNozzleTemperature;
    }

    public FloatProperty getReelMaxExtrusionRate()
    {
        return reelMaxExtrusionRate;
    }

    public FloatProperty getReelExtrusionMultiplier()
    {
        return reelExtrusionMultiplier;
    }

    public IntegerProperty getReelNozzleTemperature()
    {
        return reelNozzleTemperature;
    }

    public FloatProperty getReelRemainingFilament()
    {
        return reelRemainingFilament;
    }

    public StringProperty getReelTypeCode()
    {
        return reelTypeCode;
    }

    public StringProperty getReelUniqueID()
    {
        return reelUniqueID;
    }

    /*
     *
     */
    public final BooleanProperty NozzleHomedProperty()
    {
        return NozzleHomed;
    }

    public boolean getNozzleHomed()
    {
        return NozzleHomed.get();
    }

    public void setNozzleHomed(boolean NozzleHomed)
    {
        this.NozzleHomed.set(NozzleHomed);
    }

    public final BooleanProperty LidOpenProperty()
    {
        return LidOpen;
    }

    public boolean getLidOpen()
    {
        return LidOpen.get();
    }

    public void setLidOpen(boolean LidOpen)
    {
        this.LidOpen.set(LidOpen);
    }

    public final BooleanProperty sdCardPresentProperty()
    {
        return sdCardPresent;
    }

    public boolean sdCardPresent()
    {
        return sdCardPresent.get();
    }

    /*
     * Firmware
     */
    public final void setFirmwareVersion(String value)
    {
        firmwareVersion.set(value);
    }

    public final String getFirmwareVersion()
    {
        return firmwareVersion.get();
    }

    public final StringProperty firmwareVersionProperty()
    {
        return firmwareVersion;
    }

    public final void setPrinterStatus(PrinterStatusEnumeration value)
    {
        printerStatus.set(value);
    }

    public final PrinterStatusEnumeration getPrinterStatus()
    {
        return printerStatus.get();
    }

    public final ObjectProperty<PrinterStatusEnumeration> printerStatusProperty()
    {
        return printerStatus;
    }

    public final void setPrintJobLineNumber(int value)
    {
        printJobLineNumber.set(value);
    }

    public final int getPrintJobLineNumber()
    {
        return printJobLineNumber.get();
    }

    public final IntegerProperty printJobLineNumberProperty()
    {
        return printJobLineNumber;
    }

    public final void setPrintJobID(String value)
    {
        printJobID.set(value);
    }

    public final String getPrintJobID()
    {
        return printJobID.get();
    }

    public final StringProperty printJobIDProperty()
    {
        return printJobID;
    }

    /*
     * Error lists for tooltips
     */
    public final void setErrorList(String value)
    {
        errorList.set(value);
    }

    public final String getErrorList()
    {
        return errorList.get();
    }

    public final StringProperty errorListProperty()
    {
        return errorList;
    }

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
                AckResponse ackResponse = (AckResponse) printerEvent.getPayload();
                setErrorsDetected(ackResponse.isError());
                setSDCardError(ackResponse.isSdCardError());
                setChunkSequenceError(ackResponse.isChunkSequenceError());
                setFileTooLargeError(ackResponse.isFileTooLargeError());
                setGCodeLineTooLongError(ackResponse.isGcodeLineTooLongError());
                setUSBRxError(ackResponse.isUsbRXError());
                setUSBTxError(ackResponse.isUsbTXError());
                setBadCommandError(ackResponse.isBadCommandError());
                setEEPROMError(ackResponse.isEepromError());
                updatePrinterStatus();
                break;
            case PRINTER_STATUS_UPDATE:
                StatusResponse statusResponse = (StatusResponse) printerEvent.getPayload();
//                steno.info("Got:" + statusResponse.toString());

                setAmbientTemperature(statusResponse.getAmbientTemperature());
                setAmbientTargetTemperature(statusResponse.getAmbientTargetTemperature());
                setBedTemperature(statusResponse.getBedTemperature());
                setExtruderTemperature(statusResponse.getNozzleTemperature());
                setBedTargetTemperature(statusResponse.getBedTargetTemperature());
                setNozzleTargetTemperature(statusResponse.getNozzleTargetTemperature());

                long now = System.currentTimeMillis();
                if ((now - lastTimestamp) >= 999)
                {
                    lastTimestamp = now;

                    for (int pointCounter = 0; pointCounter < NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP - 1; pointCounter++)
                    {
                        ambientTemperatureDataPoints.get(pointCounter).setYValue(ambientTemperatureDataPoints.get(pointCounter + 1).getYValue());
                        bedTemperatureDataPoints.get(pointCounter).setYValue(bedTemperatureDataPoints.get(pointCounter + 1).getYValue());
                        nozzleTemperatureDataPoints.get(pointCounter).setYValue(nozzleTemperatureDataPoints.get(pointCounter + 1).getYValue());
                    }
                    ambientTemperatureDataPoints.get(NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP - 1).setYValue(statusResponse.getAmbientTemperature());
                    bedTemperatureDataPoints.get(NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP - 1).setYValue(statusResponse.getBedTemperature());
                    nozzleTemperatureDataPoints.get(NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP - 1).setYValue(statusResponse.getNozzleTemperature());
                }

                ambientTargetPoint.setYValue(statusResponse.getAmbientTargetTemperature());
                bedTargetPoint.setYValue(statusResponse.getBedTargetTemperature());
                nozzleTargetPoint.setYValue(statusResponse.getNozzleTargetTemperature());

                setAmbientFanOn(statusResponse.isAmbientFanOn());
                setBedHeaterOn(statusResponse.isBedHeaterOn());
                setNozzleHeaterOn(statusResponse.isNozzleHeaterOn());
                setHeadFanOn(statusResponse.isHeadFanOn());
                setPrintJobID(statusResponse.getRunningPrintJobID());
                setPrintJobLineNumber(statusResponse.getPrintJobLineNumber());
                setBusy(statusResponse.isBusyStatus());
                setPaused(statusResponse.isPauseStatus());
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
                reelAttached.set(statusResponse.isReelEEPROMPresent());
                headAttached.set(statusResponse.isHeadEEPROMPresent());
                sdCardPresent.set(statusResponse.isSDCardPresent());
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
                setHeadXPosition(statusResponse.getHeadXPosition());
                setHeadYPosition(statusResponse.getHeadYPosition());
                setHeadZPosition(statusResponse.getHeadZPosition());
                setEPosition(statusResponse.getEPosition());
                setBPosition(statusResponse.getBPosition());
                updatePrinterStatus();
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
                PrinterIDResponse idResponse = (PrinterIDResponse) printerEvent.getPayload();
                printermodel.set(idResponse.getModel());
                printeredition.set(idResponse.getEdition());
                printerweekOfManufacture.set(idResponse.getWeekOfManufacture());
                printeryearOfManufacture.set(idResponse.getYearOfManufacture());
                printerpoNumber.set(idResponse.getPoNumber());
                printerserialNumber.set(idResponse.getSerialNumber());
                printercheckByte.set(idResponse.getCheckByte());
                printerFriendlyName.set(idResponse.getPrinterFriendlyName());
                setPrinterColour(idResponse.getPrinterColour());
                try
                {
                    transmitSetAmbientLEDColour(idResponse.getPrinterColour());

                } catch (RoboxCommsException ex)
                {
                    steno.warning("Couldn't set printer LED colour");
                }
                printerIDDataChangedToggle.set(!printerIDDataChangedToggle.get());
                break;
            case REEL_EEPROM_DATA:
                ReelEEPROMDataResponse reelResponse = (ReelEEPROMDataResponse) printerEvent.getPayload();
                reelTypeCode.set(reelResponse.getReelTypeCode());
                try
                {
                    Filament loadedFilamentCandidate = FilamentContainer.getFilamentByID(reelTypeCode.get());
                    if (loadedFilamentCandidate != null)
                    {
                        reelFriendlyName.set(loadedFilamentCandidate.toString());
                        loadedFilament.set(loadedFilamentCandidate);
                        loadedFilament.get().setUniqueID(reelResponse.getReelUniqueID());
                        loadedFilament.get().setRequiredAmbientTemperature(reelResponse.getReelAmbientTemperature());
                        loadedFilament.get().setBedTemperature(reelResponse.getReelBedTemperature());
                        loadedFilament.get().setRequiredFirstLayerBedTemperature(reelResponse.getReelFirstLayerBedTemperature());
                        loadedFilament.get().setRequiredNozzleTemperature(reelResponse.getReelNozzleTemperature());
                        loadedFilament.get().setRequiredFirstLayerNozzleTemperature(reelResponse.getReelFirstLayerNozzleTemperature());
                        loadedFilament.get().setMaxExtrusionRate(reelResponse.getReelMaxExtrusionRate());
                        loadedFilament.get().setExtrusionMultiplier(reelResponse.getReelExtrusionMultiplier());
                        loadedFilament.get().setRemainingFilament(reelResponse.getReelRemainingFilament());
                        loadedFilament.get().setDiameter(reelResponse.getReelFilamentDiameter());
                    } else
                    {
                        reelFriendlyName.set(DisplayManager.getLanguageBundle().getString("sidePanel_settings.filamentUnknown"));
                        loadedFilament.set(null);
                    }
                } catch (IllegalArgumentException ex)
                {
                    reelFriendlyName.set(DisplayManager.getLanguageBundle().getString("sidePanel_settings.filamentUnknown"));
                    loadedFilament.set(null);
                }
                reelUniqueID.set(reelResponse.getReelUniqueID());
                reelAmbientTemperature.set(reelResponse.getReelAmbientTemperature());
                reelBedTemperature.set(reelResponse.getReelBedTemperature());
                reelFirstLayerBedTemperature.set(reelResponse.getReelFirstLayerBedTemperature());
                reelNozzleTemperature.set(reelResponse.getReelNozzleTemperature());
                reelFirstLayerNozzleTemperature.set(reelResponse.getReelFirstLayerNozzleTemperature());
                reelMaxExtrusionRate.set(reelResponse.getReelMaxExtrusionRate());
                reelExtrusionMultiplier.set(reelResponse.getReelExtrusionMultiplier());
                reelRemainingFilament.set(reelResponse.getReelRemainingFilament());
                reelFilamentDiameter.set(reelResponse.getReelFilamentDiameter());
                reelDataChangedToggle.set(!reelDataChangedToggle.get());
                break;
            case HEAD_EEPROM_DATA:
                HeadEEPROMDataResponse headResponse = (HeadEEPROMDataResponse) printerEvent.getPayload();
                String headTypeCodeString = headResponse.getHeadTypeCode();
                headTypeCode.set(headTypeCodeString);
                try
                {
                    Head attachedHeadCandidate = HeadContainer.getHeadByID(headTypeCodeString);
                    if (attachedHeadCandidate != null)
                    {
                        headType.set(PrintHead.getPrintHeadForType(headTypeCodeString).getShortName());
                        attachedHead.set(attachedHeadCandidate);
                        attachedHead.get().setUniqueID(headResponse.getUniqueID());
                        attachedHead.get().setHeadHours(headResponse.getHoursUsed());
                        attachedHead.get().setMaximumTemperature(headResponse.getMaximumTemperature());
                        attachedHead.get().setNozzle1_B_offset(headResponse.getNozzle1BOffset());
                        attachedHead.get().setNozzle1_X_offset(headResponse.getNozzle1XOffset());
                        attachedHead.get().setNozzle1_Y_offset(headResponse.getNozzle1YOffset());
                        attachedHead.get().setNozzle1_Z_offset(headResponse.getNozzle1ZOffset());
                        attachedHead.get().setNozzle2_B_offset(headResponse.getNozzle2BOffset());
                        attachedHead.get().setNozzle2_X_offset(headResponse.getNozzle2XOffset());
                        attachedHead.get().setNozzle2_Y_offset(headResponse.getNozzle2YOffset());
                        attachedHead.get().setNozzle2_Z_offset(headResponse.getNozzle2ZOffset());
                        attachedHead.get().setBeta(headResponse.getThermistorBeta());
                        attachedHead.get().setTcal(headResponse.getThermistorTCal());
                    } else
                    {
                        headType.set("Unknown");
                        attachedHead.set(null);
                    }
                } catch (IllegalArgumentException ex)
                {
                    reelFriendlyName.set(DisplayManager.getLanguageBundle().getString("sidePanel_settings.filamentUnknown"));
                    headType.set("Unknown");
                }

                headUniqueID.set(headResponse.getUniqueID());
                headHoursCounter.set(headResponse.getHoursUsed());
                headMaximumTemperature.set(headResponse.getMaximumTemperature());
                headNozzle1BOffset.set(headResponse.getNozzle1BOffset());
                headNozzle1XOffset.set(headResponse.getNozzle1XOffset());
                headNozzle1YOffset.set(headResponse.getNozzle1YOffset());
                headNozzle1ZOffset.set(headResponse.getNozzle1ZOffset());
                headNozzle2BOffset.set(headResponse.getNozzle2BOffset());
                headNozzle2XOffset.set(headResponse.getNozzle2XOffset());
                headNozzle2YOffset.set(headResponse.getNozzle2YOffset());
                headNozzle2ZOffset.set(headResponse.getNozzle2ZOffset());
                headThermistorBeta.set(headResponse.getThermistorBeta());
                headThermistorTCal.set(headResponse.getThermistorTCal());

                headDataChangedToggle.set(!headDataChangedToggle.get());
                break;
            default:
                steno.warning("Unknown packet type delivered to Printer Status: " + printerEvent.getEventType().name());
                break;
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

    private void updatePrinterStatus()
    {
        if (getErrorsDetected())
        {
            setPrinterStatus(PrinterStatusEnumeration.ERROR);
        } else if (paused.get() == true)
        {
            setPrinterStatus(PrinterStatusEnumeration.PAUSED);
        } else if (getPrintJobID() != null && getPrintJobID().charAt(0) != '\0')
        {
            setPrinterStatus(PrinterStatusEnumeration.PRINTING);
        } else
        {
            setPrinterStatus(PrinterStatusEnumeration.IDLE);
        }
    }

    @Override
    public String toString()
    {
        return printerFriendlyName.get();
    }

    public void addToGCodeTranscript(String gcodeToSend)
    {
        gcodeTranscript.add(gcodeToSend);
    }

    public ObservableList<String> gcodeTranscriptProperty()
    {
        return gcodeTranscript;
    }

    public PrintQueue getPrintQueue()
    {
        return printQueue;
    }

    /*
     * Data transmission commands
     */
    public String transmitDirectGCode(final String gcodeToSend, boolean addToTranscript) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.EXECUTE_GCODE);

        String gcodeToSendWithLF = gcodeToSend + "\n";

        gcodePacket.setMessagePayload(gcodeToSendWithLF);

        if (addToTranscript)
        {
            addToGCodeTranscript(gcodeToSendWithLF);
        }
        GCodeDataResponse response = (GCodeDataResponse) printerCommsManager.submitForWrite(portName, gcodePacket);
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

    public String transmitStoredGCode(final GCodeMacros macro, boolean addToTranscript) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.EXECUTE_GCODE);

        ArrayList<String> macroContents = macro.getMacroContents();
        StringBuilder finalResponse = new StringBuilder();

        for (String gcodeToSend : macroContents)
        {
            gcodeToSend += "\n";
            gcodePacket.setMessagePayload(gcodeToSend);

            if (addToTranscript)
            {
                addToGCodeTranscript(gcodeToSend);
            }
            GCodeDataResponse response = (GCodeDataResponse) printerCommsManager.submitForWrite(portName, gcodePacket);
            finalResponse.append(response.getGCodeResponse() + "\n");

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
        }

        return finalResponse.toString();
    }

    private boolean transmitDataFileStart(final String fileID) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.START_OF_DATA_FILE);
        gcodePacket.setMessagePayload(fileID);

        AckResponse response = (AckResponse) printerCommsManager.submitForWrite(portName, gcodePacket);
        boolean success = false;
        // Only check for SD card errors here...
        success = !response.isSdCardError();

        return success;
    }

    private void transmitDataFileChunk(final String payloadData, final int sequenceNumber) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.DATA_FILE_CHUNK);
        gcodePacket.setMessagePayload(payloadData);
        gcodePacket.setSequenceNumber(sequenceNumber);

        printerCommsManager.submitForWrite(portName, gcodePacket);
        dataFileSequenceNumber++;
    }

    private void transmitDataFileEnd(final String payloadData, final int sequenceNumber) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.END_OF_DATA_FILE);
        gcodePacket.setMessagePayload(payloadData);
        gcodePacket.setSequenceNumber(sequenceNumber);

        printerCommsManager.submitForWrite(portName, gcodePacket);
    }

    public void transmitResetErrors() throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.RESET_ERRORS);

        printerCommsManager.submitForWrite(portName, gcodePacket);
    }

    public boolean transmitUpdateFirmware(final String firmwareID) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.UPDATE_FIRMWARE);
        gcodePacket.setMessagePayload(firmwareID);

        AckResponse response = (AckResponse) printerCommsManager.submitForWrite(portName, gcodePacket);

        return (response.isError());
    }

    private void transmitInitiatePrint(final String printJobUUID) throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.INITIATE_PRINT);
        gcodePacket.setMessagePayload(printJobUUID);

        printerCommsManager.submitForWrite(portName, gcodePacket);
    }

    private void transmitAbortPrint() throws RoboxCommsException
    {
        RoboxTxPacket gcodePacket = RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.ABORT_PRINT);

        printerCommsManager.submitForWrite(portName, gcodePacket);
    }

    public void transmitPausePrint() throws RoboxCommsException
    {
        PausePrint gcodePacket = (PausePrint) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.PAUSE_RESUME_PRINT);
        gcodePacket.setPause();

        printerCommsManager.submitForWrite(portName, gcodePacket);
    }

    public void transmitResumePrint() throws RoboxCommsException
    {
        PausePrint gcodePacket = (PausePrint) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.PAUSE_RESUME_PRINT);
        gcodePacket.setResume();

        printerCommsManager.submitForWrite(portName, gcodePacket);
    }

    public void transmitFormatHeadEEPROM() throws RoboxCommsException
    {
        FormatHeadEEPROM formatHead = (FormatHeadEEPROM) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.FORMAT_HEAD_EEPROM);
        printerCommsManager.submitForWrite(portName, formatHead);
    }

    public void transmitFormatReelEEPROM() throws RoboxCommsException
    {
        FormatReelEEPROM formatReel = (FormatReelEEPROM) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.FORMAT_REEL_EEPROM);
        printerCommsManager.submitForWrite(portName, formatReel);
    }

    public void transmitReadReelEEPROM() throws RoboxCommsException
    {
        ReadReelEEPROM readReel = (ReadReelEEPROM) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_REEL_EEPROM);
        printerCommsManager.submitForWrite(portName, readReel);
    }

    public void transmitReadHeadEEPROM() throws RoboxCommsException
    {
        ReadHeadEEPROM readHead = (ReadHeadEEPROM) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_HEAD_EEPROM);
        printerCommsManager.submitForWrite(portName, readHead);
    }

    public void transmitWriteReelEEPROM(String reelTypeCode, String reelUniqueID, float reelFirstLayerNozzleTemperature, float reelNozzleTemperature,
            float reelFirstLayerBedTemperature, float reelBedTemperature, float reelAmbientTemperature, float reelFilamentDiameter,
            float reelMaxExtrusionRate, float reelExtrusionMultiplier, float reelRemainingFilament) throws RoboxCommsException
    {
        WriteReelEEPROM writeReelEEPROM = (WriteReelEEPROM) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.WRITE_REEL_EEPROM);
        writeReelEEPROM.populateEEPROM(reelTypeCode, reelUniqueID, reelFirstLayerNozzleTemperature, reelNozzleTemperature,
                reelFirstLayerBedTemperature, reelBedTemperature, reelAmbientTemperature, reelFilamentDiameter,
                reelMaxExtrusionRate, reelExtrusionMultiplier, reelRemainingFilament);
        printerCommsManager.submitForWrite(portName, writeReelEEPROM);
    }

    public void transmitWriteHeadEEPROM(String headTypeCode, String headUniqueID, float maximumTemperature,
            float thermistorBeta, float thermistorTCal,
            float nozzle1XOffset, float nozzle1YOffset, float nozzle1ZOffset, float nozzle1BOffset,
            float nozzle2XOffset, float nozzle2YOffset, float nozzle2ZOffset, float nozzle2BOffset,
            float hourCounter) throws RoboxCommsException
    {
        WriteHeadEEPROM writeHeadEEPROM = (WriteHeadEEPROM) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.WRITE_HEAD_EEPROM);
        writeHeadEEPROM.populateEEPROM(headTypeCode, headUniqueID, maximumTemperature, thermistorBeta, thermistorTCal, nozzle1XOffset, nozzle1YOffset, nozzle1ZOffset, nozzle1BOffset, nozzle2XOffset, nozzle2YOffset, nozzle2ZOffset, nozzle2BOffset, hourCounter);
        printerCommsManager.submitForWrite(portName, writeHeadEEPROM);
    }

    /*
     * Higher level controls
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

    public void transmitSetAmbientLEDColour(Color colour) throws RoboxCommsException
    {
        SetAmbientLEDColour ledColour = (SetAmbientLEDColour) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.SET_AMBIENT_LED_COLOUR);
        ledColour.setLEDColour(colour);
        printerCommsManager.submitForWrite(portName, ledColour);
    }

    public void transmitReadPrinterID() throws RoboxCommsException
    {
        ReadPrinterID readId = (ReadPrinterID) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_PRINTER_ID);
        printerCommsManager.submitForWrite(portName, readId);
    }

    public boolean transmitWritePrinterID(String model, String edition, String weekOfManufacture, String yearOfManufacture, String poNumber, String serialNumber, String checkByte, String printerFriendlyName, Color colour) throws RoboxCommsException
    {
        WritePrinterID writeIDCmd = (WritePrinterID) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.WRITE_PRINTER_ID);
        writeIDCmd.setIDAndColour(model, edition, weekOfManufacture, yearOfManufacture, poNumber, serialNumber, checkByte, printerFriendlyName, colour);

        AckResponse response = (AckResponse) printerCommsManager.submitForWrite(portName, writeIDCmd);

        boolean success = false;

        if (response.isError() == false)
        {
            success = true;
            // Special case - we don't get this information back in a status request so update it as we set the new value
            printermodel.set(model);
            printeredition.set(edition);
            printerweekOfManufacture.set(weekOfManufacture);
            printeryearOfManufacture.set(yearOfManufacture);
            printerpoNumber.set(poNumber);
            printerserialNumber.set(serialNumber);
            printercheckByte.set(checkByte);
            this.printerFriendlyName.set(printerFriendlyName);
            setPrinterColour(colour);
        }

        return success;
    }

    public void transmitReadFirmwareVersion() throws RoboxCommsException
    {
        QueryFirmwareVersion readFirmware = (QueryFirmwareVersion) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.QUERY_FIRMWARE_VERSION);
        printerCommsManager.submitForWrite(portName, readFirmware);
    }

    public void transmitSetTemperatures(double nozzleFirstLayerTarget, double nozzleTarget, double bedFirstLayerTarget, double bedTarget, double ambientTarget) throws RoboxCommsException
    {
        SetTemperatures setTemperatures = (SetTemperatures) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.SET_TEMPERATURES);
        setTemperatures.setTemperatures(nozzleTarget, nozzleFirstLayerTarget, bedTarget, bedFirstLayerTarget, ambientTarget);
        printerCommsManager.submitForWrite(portName, setTemperatures);
    }

    public boolean initialiseDataFileSend(String fileID) throws DatafileSendAlreadyInProgress, RoboxCommsException
    {
        boolean success = false;
        if (sendInProgress)
        {
            throw new DatafileSendAlreadyInProgress();
        } else
        {
            this.fileID = fileID;
            success = transmitDataFileStart(fileID);
            sendInProgress = true;
            outputBuffer.delete(0, outputBuffer.length());
            sequenceNumber = 0;
            printInitiated = false;
        }

        return success;
    }

    public void initiatePrint(String hexDigits) throws DatafileSendNotInitialised, RoboxCommsException
    {
        if (sendInProgress == false)
        {
            throw new DatafileSendNotInitialised();
        } else
        {
            transmitInitiatePrint(fileID);
            printInitiated = true;
        }
    }

    public void sendDataFileChunk(String hexDigits, boolean lastPacket, boolean appendCRLF) throws DatafileSendNotInitialised, RoboxCommsException
    {
        boolean dataIngested = false;

        if (appendCRLF)
        {
            hexDigits += "\r";
        }

        int remainingCharacters = hexDigits.length();

        if (sendInProgress == false)
        {
            throw new DatafileSendNotInitialised();
        }

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
                String stringToWrite = hexDigits.substring(hexDigits.length() - remainingCharacters, (hexDigits.length() - remainingCharacters) + (outputBuffer.capacity() - outputBuffer.length()));
                outputBuffer.append(stringToWrite);
                remainingCharacters -= stringToWrite.length();
            }

            /*
             * Send when full
             */
            if ((outputBuffer.capacity() - outputBuffer.length()) == 0)
            {
                /*
                 * If this is the last packet then send as an end...
                 */
                if (dataIngested && lastPacket)
                {
//                    steno.info("Final complete chunk:" + outputBuffer.toString() + " seq:" + sequenceNumber);
                    transmitDataFileEnd(outputBuffer.toString(), sequenceNumber);
                    sendInProgress = false;
                } else
                {
//                    steno.info("Sending chunk:" + outputBuffer.toString() + " seq:" + sequenceNumber);
                    transmitDataFileChunk(outputBuffer.toString(), sequenceNumber);
                    outputBuffer.delete(0, bufferSize);
                }
                sequenceNumber++;
            }
        }
    }

    public void printProject(Project project, Filament filament, PrintQualityEnumeration printQuality, SlicerSettings settings)
    {
        if (filament != null)
        {
            try
            {
                transmitSetTemperatures(filament.getNozzleTemperature(), filament.getNozzleTemperature(), filament.getFirstLayerBedTemperature(), filament.getBedTemperature(), filament.getAmbientTemperature());
                //Temporary fix as there is no interface to allow filament diameter or extrusion multiplier to be overridden using a Robox command.
                settings.setFilament_diameter(filament.getDiameter());
                settings.setExtrusion_multiplier(filament.getExtrusionMultiplier());
            } catch (RoboxCommsException ex)
            {
                steno.error("Failure to set temperatures prior to print");
            }
        }
        printQueue.printProject(this, project, printQuality, settings);
    }

    public void abortPrint()
    {
        printQueue.cancelRun();
        sendInProgress = false;
        if (isPrintInitiated())
        {
            try
            {
                transmitAbortPrint();
                String response = transmitStoredGCode(GCodeMacros.ABORT_PRINT, true);
            } catch (RoboxCommsException ex)
            {
                steno.error("Robox comms exception when sending abort print command " + ex);
            }
        }
    }

    public void pausePrint()
    {
        try
        {
            transmitPausePrint();
            transmitStoredGCode(GCodeMacros.PAUSE_PRINT, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Robox comms exception when sending pause print command " + ex);
        }
    }

    public void resumePrint()
    {
        try
        {
            transmitStoredGCode(GCodeMacros.RESUME_PRINT, true);
            transmitResumePrint();
        } catch (RoboxCommsException ex)
        {
            steno.error("Robox comms exception when sending resume print command " + ex);
        }
    }

    public int getSequenceNumber()
    {
        return sequenceNumber;
    }

    public boolean isPrintInitiated()
    {
        return printInitiated;
    }
}
