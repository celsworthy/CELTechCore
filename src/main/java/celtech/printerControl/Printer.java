/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl;

import celtech.appManager.Project;
import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.Head;
import celtech.configuration.HeaterMode;
import celtech.configuration.WhyAreWeWaitingState;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.ListFilesResponse;
import celtech.printerControl.comms.commands.rx.ReelEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.comms.events.RoboxEvent;
import celtech.services.printing.DatafileSendAlreadyInProgress;
import celtech.services.printing.DatafileSendNotInitialised;
import celtech.services.printing.PrintQueue;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;

/**
 *
 * @author tony
 */
public interface Printer
{

    /**
     *
     * @return
     */
    public String getPrinterPort();

    /**
     *
     * @return
     */
    public BooleanProperty getPrinterIDDataChangedToggle();

    /**
     *
     * @return
     */
    public StringProperty getPrintermodel();

    /**
     *
     * @return
     */
    public StringProperty getPrinteredition();

    /**
     *
     * @return
     */
    public StringProperty getPrinterweekOfManufacture();

    /**
     *
     * @return
     */
    public StringProperty getPrinteryearOfManufacture();

    /**
     *
     * @return
     */
    public StringProperty getPrinterpoNumber();

    /**
     *
     * @return
     */
    public StringProperty getPrinterserialNumber();

    /**
     *
     * @return
     */
    public StringProperty getPrintercheckByte();

    /**
     *
     * @return
     */
    public String getPrinterUniqueID();

    /**
     *
     * @return
     */
    public StringProperty printerFriendlyNameProperty();

    /**
     *
     * @return
     */
    public String getPrinterFriendlyName();

    /**
     *
     * @param value
     */
    public void setPrinterColour(Color value);

    /**
     *
     * @return
     */
    public Color getPrinterColour();

    /**
     *
     * @return
     */
    public ObjectProperty<Color> printerColourProperty();

    /**
     *
     * @param value
     */
    public void setHeadXPosition(float value);

    /**
     *
     * @return
     */
    public float getHeadXPosition();

    /**
     *
     * @return
     */
    public FloatProperty headXPositionProperty();

    /**
     *
     * @param value
     */
    public void setHeadYPosition(float value);

    /**
     *
     * @return
     */
    public float getHeadYPosition();

    /**
     *
     * @return
     */
    public FloatProperty headYPositionProperty();

    /**
     *
     * @param value
     */
    public void setHeadZPosition(float value);

    /**
     *
     * @return
     */
    public float getHeadZPosition();

    /**
     *
     * @return
     */
    public FloatProperty headZPositionProperty();

    /**
     *
     * @param value
     */
    public void setBPosition(float value);

    /**
     *
     * @return
     */
    public float getBPosition();

    /**
     *
     * @return
     */
    public FloatProperty BPositionProperty();

    /**
     *
     * @param value
     */
    public void setPrinterConnected(boolean value);

    /**
     *
     * @return
     */
    public boolean getPrinterConnected();

    /**
     *
     * @return
     */
    public BooleanProperty printerConnectedProperty();

    /**
     *
     * @param value
     */
    public void setAmbientTemperature(int value);

    /**
     *
     * @return
     */
    public float getAmbientTemperature();

    /**
     *
     * @return
     */
    public IntegerProperty ambientTemperatureProperty();

    /**
     *
     * @return
     */
    public XYChart.Series<Number, Number> ambientTemperatureHistory();

    /**
     *
     * @return
     */
    public XYChart.Series<Number, Number> ambientTargetTemperatureHistory();

    /**
     *
     * @param value
     */
    public void setAmbientTargetTemperature(int value);

    /**
     *
     * @return
     */
    public int getAmbientTargetTemperature();

    /**
     *
     * @return
     */
    public IntegerProperty ambientTargetTemperatureProperty();

    /**
     *
     * @param value
     */
    public void setAmbientFanOn(boolean value);

    /**
     *
     * @return
     */
    public boolean getAmbientFanOn();

    /**
     *
     * @return
     */
    public BooleanProperty ambientFanOnProperty();

    /**
     *
     * @return
     */
    public HeaterMode getBedHeaterMode();

    /**
     *
     * @return
     */
    public ObjectProperty<HeaterMode> getBedHeaterModeProperty();

    /**
     *
     * @return
     */
    public HeaterMode getNozzleHeaterMode();

    /**
     *
     * @return
     */
    public ObjectProperty<HeaterMode> getNozzleHeaterModeProperty();

    /**
     *
     * @param value
     */
    public void setBedTemperature(int value);

    /**
     *
     * @return
     */
    public int getBedTemperature();

    /**
     *
     * @return
     */
    public IntegerProperty bedTemperatureProperty();

    /**
     *
     * @return
     */
    public XYChart.Series<Number, Number> bedTemperatureHistory();

    /**
     *
     * @return
     */
    public XYChart.Series<Number, Number> bedTargetTemperatureHistory();

    /**
     *
     * @param value
     */
    public void setBedFirstLayerTargetTemperature(int value);

    /**
     *
     * @return
     */
    public int getBedFirstLayerTargetTemperature();

    /**
     *
     * @return
     */
    public IntegerProperty bedFirstLayerTargetTemperatureProperty();

    /**
     *
     * @param value
     */
    public void setBedTargetTemperature(int value);

    /**
     *
     * @return
     */
    public int getBedTargetTemperature();

    /**
     *
     * @return
     */
    public IntegerProperty bedTargetTemperatureProperty();

    /**
     *
     * @param value
     */
    public void setExtruderTemperature(int value);

    /**
     *
     * @return
     */
    public int getExtruderTemperature();

    /**
     *
     * @return
     */
    public IntegerProperty extruderTemperatureProperty();

    /**
     *
     * @return
     */
    public XYChart.Series<Number, Number> nozzleTemperatureHistory();

    /**
     *
     * @return
     */
    public XYChart.Series<Number, Number> nozzleTargetTemperatureHistory();

    /**
     *
     * @param value
     */
    public void setNozzleFirstLayerTargetTemperature(int value);

    /**
     *
     * @return
     */
    public int getNozzleFirstLayerTargetTemperature();

    /**
     *
     * @return
     */
    public IntegerProperty nozzleFirstLayerTargetTemperatureProperty();

    /**
     *
     * @param value
     */
    public void setNozzleTargetTemperature(int value);

    /**
     *
     * @return
     */
    public int getNozzleTargetTemperature();

    /**
     *
     * @return
     */
    public IntegerProperty nozzleTargetTemperatureProperty();

    /**
     *
     * @param value
     */
    public void setHeadFanOn(boolean value);

    /**
     *
     * @return
     */
    public boolean getHeadFanOn();

    /**
     *
     * @return
     */
    public BooleanProperty headFanOnProperty();

    /**
     *
     * @param value
     */
    public void setBusy(boolean value);

    /**
     *
     * @return
     */
    public boolean getBusy();

    /**
     *
     * @return
     */
    public BooleanProperty busyProperty();

    /**
     *
     * @param value
     */
    public void setPaused(boolean value);

    /**
     *
     * @return
     */
    public boolean getPaused();

    /**
     *
     * @return
     */
    public BooleanProperty pausedProperty();

    /**
     *
     * @param value
     */
    public void setWhyAreWeWaiting(WhyAreWeWaitingState value);

    /**
     *
     * @return
     */
    public WhyAreWeWaitingState getWhyAreWeWaiting();

    /**
     *
     * @return
     */
    public StringProperty getWhyAreWeWaitingStringProperty();

    /**
     *
     * @return
     */
    public ObjectProperty<WhyAreWeWaitingState> whyAreWeWaitingProperty();

    /*
     * Errors
     */
    /**
     *
     * @param value
     */
    public void setErrorsDetected(boolean value);

    /**
     *
     * @return
     */
    public boolean getErrorsDetected();

    /**
     *
     * @return
     */
    public BooleanProperty errorsDetectedProperty();

    /**
     *
     * @return
     */
    public BooleanProperty SDCardErrorProperty();

    /**
     *
     * @return
     */
    public boolean getSDCardError();

    /**
     *
     * @param SDCardError
     */
    public void setSDCardError(boolean SDCardError);

    /**
     *
     * @return
     */
    public BooleanProperty chunkSequenceErrorProperty();

    /**
     *
     * @return
     */
    public boolean getChunkSequenceError();

    /**
     *
     * @param ChunkSequenceError
     */
    public void setChunkSequenceError(boolean ChunkSequenceError);

    /**
     *
     * @return
     */
    public BooleanProperty fileTooLargeErrorProperty();

    /**
     *
     * @return
     */
    public boolean getFileTooLargeError();

    /**
     *
     * @param FileTooLargeError
     */
    public void setFileTooLargeError(boolean FileTooLargeError);

    /**
     *
     * @return
     */
    public BooleanProperty GCodeLineTooLongErrorProperty();

    /**
     *
     * @return
     */
    public boolean getGCodeLineTooLongError();

    /**
     *
     * @param GCodeLineTooLongError
     */
    public void setGCodeLineTooLongError(boolean GCodeLineTooLongError);

    /**
     *
     * @return
     */
    public BooleanProperty USBRxErrorProperty();

    /**
     *
     * @return
     */
    public boolean getUSBRxError();

    /**
     *
     * @param USBRxError
     */
    public void setUSBRxError(boolean USBRxError);

    /**
     *
     * @return
     */
    public BooleanProperty USBTxErrorProperty();

    /**
     *
     * @return
     */
    public boolean getUSBTxError();

    /**
     *
     * @param USBTxError
     */
    public void setUSBTxError(boolean USBTxError);

    /**
     *
     * @return
     */
    public BooleanProperty BadCommandErrorProperty();

    /**
     *
     * @return
     */
    public boolean getBadCommandError();

    /**
     *
     * @param BadCommandError
     */
    public void setBadCommandError(boolean BadCommandError);

    /**
     *
     * @return
     */
    public BooleanProperty EEPROMErrorProperty();

    /**
     *
     * @return
     */
    public boolean getEEPROMError();

    /**
     *
     * @param EEPROMError
     */
    public void setEEPROMError(boolean EEPROMError);

    /*
     * Switches
     */
    /**
     *
     * @return
     */
    public BooleanProperty XStopSwitchProperty();

    /**
     *
     * @return
     */
    public boolean getXStopSwitch();

    /**
     *
     * @param XStopSwitch
     */
    public void setXStopSwitch(boolean XStopSwitch);

    /**
     *
     * @return
     */
    public BooleanProperty YStopSwitchProperty();

    /**
     *
     * @return
     */
    public boolean getYStopSwitch();

    /**
     *
     * @param YStopSwitch
     */
    public void setYStopSwitch(boolean YStopSwitch);

    /**
     *
     * @return
     */
    public BooleanProperty ZStopSwitchProperty();

    /**
     *
     * @return
     */
    public boolean getZStopSwitch();

    /**
     *
     * @param ZStopSwitch
     */
    public void setZStopSwitch(boolean ZStopSwitch);

    /**
     *
     * @return
     */
    public BooleanProperty ZTopStopSwitchProperty();

    /**
     *
     * @return
     */
    public boolean getZTopStopSwitch();

    /**
     *
     * @param value
     */
    public void setZTopStopSwitch(boolean value);

    /**
     *
     * @return
     */
    public BooleanProperty Filament1LoadedProperty();

    /**
     *
     * @return
     */
    public boolean getFilament1Loaded();

    /**
     *
     * @param FilamentLoaded
     */
    public void setFilament1Loaded(boolean FilamentLoaded);

    /**
     *
     * @return
     */
    public BooleanProperty Filament2LoadedProperty();

    /**
     *
     * @return
     */
    public boolean getFilament2Loaded();

    /**
     *
     * @param FilamentLoaded
     */
    public void setFilament2Loaded(boolean FilamentLoaded);

    /**
     *
     * @return
     */
    public BooleanProperty Filament1IndexProperty();

    /**
     *
     * @return
     */
    public boolean getFilament1Index();

    /**
     *
     * @param FilamentIndex
     */
    public void setFilament1Index(boolean FilamentIndex);

    /**
     *
     * @return
     */
    public BooleanProperty Filament2IndexProperty();

    /**
     *
     * @return
     */
    public boolean getFilament2Index();

    /**
     *
     * @param FilamentIndex
     */
    public void setFilament2Index(boolean FilamentIndex);

    /**
     *
     * @return
     */
    public BooleanProperty reelButtonProperty();

    /**
     *
     * @return
     */
    public boolean getReelButton();

    /**
     *
     * @param FilamentIndex
     */
    public void setReelButton(boolean FilamentIndex);

    /*
     * Head data
     */
    /**
     *
     * @return
     */
    public ObjectProperty<Head> attachedHeadProperty();

    /**
     *
     * @return
     */
    public BooleanProperty getHeadDataChangedToggle();

    /**
     *
     * @return
     */
    public ObjectProperty<EEPROMState> headEEPROMStatusProperty();

    /**
     *
     * @return
     */
    public EEPROMState getHeadEEPROMStatus();

    /**
     *
     * @return
     */
    public StringProperty getHeadTypeCode();

    /**
     *
     * @return
     */
    public StringProperty getHeadType();

    /**
     *
     * @return
     */
    public StringProperty getHeadUniqueID();

    /**
     *
     * @return
     */
    public FloatProperty getHeadMaximumTemperature();

    /**
     *
     * @return
     */
    public FloatProperty getHeadThermistorBeta();

    /**
     *
     * @return
     */
    public FloatProperty getHeadThermistorTCal();

    /**
     *
     * @return
     */
    public FloatProperty getHeadNozzle1XOffset();

    /**
     *
     * @return
     */
    public FloatProperty getHeadNozzle1YOffset();

    /**
     *
     * @return
     */
    public FloatProperty getHeadNozzle1ZOffset();

    /**
     *
     * @return
     */
    public FloatProperty getHeadNozzle1BOffset();

    /**
     *
     * @return
     */
    public FloatProperty getHeadNozzle2XOffset();

    /**
     *
     * @return
     */
    public FloatProperty getHeadNozzle2YOffset();

    /**
     *
     * @return
     */
    public FloatProperty getHeadNozzle2ZOffset();

    /**
     *
     * @return
     */
    public FloatProperty getHeadNozzle2BOffset();

    /**
     *
     * @return
     */
    public FloatProperty getHeadHoursCounter();

    /**
     *
     * @return
     */
    public FloatProperty getLastFilamentTemperature();
    /*
     * Reel data
     */

    /**
     *
     * @return
     */
    public ObjectProperty<Filament> loadedFilamentProperty();

    /**
     *
     * @return
     */
    public BooleanProperty reelDataChangedProperty();

    /**
     *
     * @return
     */
    public ObjectProperty<EEPROMState> reelEEPROMStatusProperty();

    /**
     *
     * @return
     */
    public EEPROMState getReelEEPROMStatus();

    /**
     *
     * @return
     */
    public StringProperty reelFriendlyNameProperty();

    /**
     *
     * @return
     */
    public String getReelFriendlyName();

    /**
     *
     * @return
     */
    public IntegerProperty getReelAmbientTemperature();

    /**
     *
     * @return
     */
    public IntegerProperty getReelBedTemperature();

    /**
     *
     * @return
     */
    public FloatProperty getReelFilamentDiameter();

    /**
     *
     * @return
     */
    public IntegerProperty getReelFirstLayerBedTemperature();

    /**
     *
     * @return
     */
    public IntegerProperty getReelFirstLayerNozzleTemperature();

    /**
     *
     * @return
     */
    public FloatProperty getReelFilamentMultiplier();

    /**
     *
     * @return
     */
    public FloatProperty getReelFeedRateMultiplier();

    /**
     *
     * @return
     */
    public IntegerProperty getReelNozzleTemperature();

    /**
     *
     * @return
     */
    public FloatProperty getReelRemainingFilament();

    /**
     *
     * @return
     */
    public StringProperty getReelTypeCode();

    /**
     *
     * @return
     */
    public StringProperty getReelUniqueID();

    /*
     *
     */
    /**
     *
     * @return
     */
    public BooleanProperty NozzleHomedProperty();

    /**
     *
     * @return
     */
    public boolean getNozzleHomed();

    /**
     *
     * @param NozzleHomed
     */
    public void setNozzleHomed(boolean NozzleHomed);

    /**
     *
     * @return
     */
    public BooleanProperty LidOpenProperty();

    /**
     *
     * @return
     */
    public boolean getLidOpen();

    /**
     *
     * @param LidOpen
     */
    public void setLidOpen(boolean LidOpen);

    /**
     *
     * @return
     */
    public BooleanProperty sdCardPresentProperty();

    /**
     *
     * @return
     */
    public boolean sdCardPresent();

    /*
     * Firmware
     */
    /**
     *
     * @param value
     */
    public void setFirmwareVersion(String value);

    /**
     *
     * @return
     */
    public String getFirmwareVersion();

    /**
     *
     * @return
     */
    public StringProperty firmwareVersionProperty();

    /**
     *
     * @param value
     */
    public void setPrinterStatus(PrinterStatusEnumeration value);

    /**
     *
     * @return
     */
    public PrinterStatusEnumeration getPrinterStatus();

    /**
     *
     * @return
     */
    public ObjectProperty<PrinterStatusEnumeration> printerStatusProperty();

    /**
     *
     * @param value
     */
    public void setPrintJobLineNumber(int value);

    /**
     *
     * @return
     */
    public int getPrintJobLineNumber();

    /**
     *
     * @return
     */
    public IntegerProperty printJobLineNumberProperty();

    /**
     *
     * @param value
     */
    public void setPrintJobID(String value);

    /**
     *
     * @return
     */
    public String getPrintJobID();

    /**
     *
     * @return
     */
    public StringProperty printJobIDProperty();

    /*
     * Error lists for tooltips
     */
    /**
     *
     * @param value
     */
    public void setErrorList(String value);

    /**
     *
     * @return
     */
    public String getErrorList();

    /**
     *
     * @return
     */
    public StringProperty errorListProperty();

    /**
     *
     * @param printerEvent
     */
    public void processRoboxEvent(RoboxEvent printerEvent);

    /**
     *
     * @param gcodeToSend
     */
    public void addToGCodeTranscript(String gcodeToSend);

    /**
     *
     * @return
     */
    public ObservableList<String> gcodeTranscriptProperty();

    /**
     *
     * @return
     */
    public PrintQueue getPrintQueue();

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
    public String transmitDirectGCode(String gcodeToSend, boolean addToTranscript) throws RoboxCommsException;

    /**
     *
     * @param macroName
     * @throws RoboxCommsException
     */
    public void transmitStoredGCode(String macroName) throws RoboxCommsException;

    /**
     *
     * @param macroName
     * @param checkForPurge
     * @throws RoboxCommsException
     */
    public void transmitStoredGCode(String macroName, boolean checkForPurge) throws RoboxCommsException;

    /**
     *
     * @throws RoboxCommsException
     */
    public void transmitAbortPrint() throws RoboxCommsException;

    /**
     *
     * @throws RoboxCommsException
     */
    public void transmitPausePrint() throws RoboxCommsException;

    /**
     *
     * @throws RoboxCommsException
     */
    public void transmitResumePrint() throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public AckResponse transmitFormatHeadEEPROM() throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public AckResponse transmitFormatReelEEPROM() throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public ReelEEPROMDataResponse transmitReadReelEEPROM() throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public HeadEEPROMDataResponse transmitReadHeadEEPROM() throws RoboxCommsException;

    /**
     *
     * @param filament
     * @return
     * @throws RoboxCommsException
     */
    public AckResponse transmitWriteReelEEPROM(Filament filament) throws RoboxCommsException;

    /**
     *
     * @param reelTypeCode
     * @param reelUniqueID
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
    public void transmitWriteReelEEPROM(String reelTypeCode, String reelUniqueID, float reelFirstLayerNozzleTemperature, float reelNozzleTemperature,
            float reelFirstLayerBedTemperature, float reelBedTemperature, float reelAmbientTemperature, float reelFilamentDiameter,
            float reelFilamentMultiplier, float reelFeedRateMultiplier, float reelRemainingFilament) throws RoboxCommsException;

    /**
     *
     * @param headToWrite
     * @throws RoboxCommsException
     */
    public void transmitWriteHeadEEPROM(Head headToWrite) throws RoboxCommsException;

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
    public AckResponse transmitWriteHeadEEPROM(String headTypeCode, String headUniqueID, float maximumTemperature,
            float thermistorBeta, float thermistorTCal,
            float nozzle1XOffset, float nozzle1YOffset, float nozzle1ZOffset, float nozzle1BOffset,
            float nozzle2XOffset, float nozzle2YOffset, float nozzle2ZOffset, float nozzle2BOffset,
            float lastFilamentTemperature, float hourCounter) throws RoboxCommsException;

    /*
     * Higher level controls
     */
    /**
     *
     * @param on
     * @throws RoboxCommsException
     */
    public void switchOnHeadLEDs(boolean on) throws RoboxCommsException;

    /**
     *
     * @param colour
     * @throws RoboxCommsException
     */
    public void transmitSetAmbientLEDColour(Color colour) throws RoboxCommsException;

    /**
     *
     * @param colour
     * @throws RoboxCommsException
     */
    public void transmitSetReelLEDColour(Color colour) throws RoboxCommsException;

    /**
     *
     * @throws RoboxCommsException
     */
    public void transmitReadPrinterID() throws RoboxCommsException;

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
    public boolean transmitWritePrinterID(String model, String edition, String weekOfManufacture, String yearOfManufacture, String poNumber, String serialNumber, String checkByte, String printerFriendlyName, Color colour) throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public FirmwareResponse transmitReadFirmwareVersion() throws RoboxCommsException;

    /**
     *
     * @param nozzleFirstLayerTarget
     * @param nozzleTarget
     * @param bedFirstLayerTarget
     * @param bedTarget
     * @param ambientTarget
     * @throws RoboxCommsException
     */
    public void transmitSetTemperatures(double nozzleFirstLayerTarget, double nozzleTarget, double bedFirstLayerTarget, double bedTarget, double ambientTarget) throws RoboxCommsException;

    /**
     *
     * @param filamentDiameter
     * @param filamentMultiplier
     * @param feedRateMultiplier
     * @throws RoboxCommsException
     */
    public void transmitSetFilamentInfo(double filamentDiameter, double filamentMultiplier, double feedRateMultiplier) throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public ListFilesResponse transmitListFiles() throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public StatusResponse transmitStatusRequest() throws RoboxCommsException;

    /**
     *
     * @param fileID
     * @return
     * @throws DatafileSendAlreadyInProgress
     * @throws RoboxCommsException
     */
    public boolean initialiseDataFileSend(String fileID) throws DatafileSendAlreadyInProgress, RoboxCommsException;

    /**
     *
     * @param jobUUID
     * @throws RoboxCommsException
     */
    public void initiatePrint(String jobUUID) throws RoboxCommsException;

    /**
     *
     * @param hexDigits
     * @param lastPacket
     * @param appendCRLF
     * @throws DatafileSendNotInitialised
     * @throws RoboxCommsException
     */
    public void sendDataFileChunk(String hexDigits, boolean lastPacket, boolean appendCRLF) throws DatafileSendNotInitialised, RoboxCommsException;

    /**
     *
     * @param project
     * @param filament
     * @param printQuality
     * @param settings
     */
    public void printProject(Project project, Filament filament, PrintQualityEnumeration printQuality, RoboxProfile settings);

    /**
     *
     */
    public void abortPrint();

    /**
     *
     */
    public void pausePrint();

    /**
     *
     */
    public void resumePrint();

    /**
     *
     * @return
     */
    public int getSequenceNumber();

    /**
     *
     * @return
     */
    public boolean isPrintInitiated();

    /**
     *
     * @param reelNozzleTemperature
     */
    public void transmitWriteMaterialTemperatureToHeadEEPROM(int reelNozzleTemperature);
}
