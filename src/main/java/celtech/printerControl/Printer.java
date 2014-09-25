/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl;

import celtech.appManager.Project;
import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.Head;
import celtech.configuration.HeaterMode;
import celtech.configuration.MaterialType;
import celtech.configuration.PauseStatus;
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
public abstract class Printer extends NewPrinter
{
    public abstract StringProperty getPrinterUniqueIDProperty();

    /**
     *
     * @return
     */
    public abstract String getPrinterPort();

    /**
     *
     * @return
     */
    public abstract BooleanProperty getPrinterIDDataChangedToggle();

    /**
     *
     * @return
     */
    public abstract StringProperty getPrintermodel();

    /**
     *
     * @return
     */
    public abstract StringProperty getPrinteredition();

    /**
     *
     * @return
     */
    public abstract StringProperty getPrinterweekOfManufacture();

    /**
     *
     * @return
     */
    public abstract StringProperty getPrinteryearOfManufacture();

    /**
     *
     * @return
     */
    public abstract StringProperty getPrinterpoNumber();

    /**
     *
     * @return
     */
    public abstract StringProperty getPrinterserialNumber();

    /**
     *
     * @return
     */
    public abstract StringProperty getPrintercheckByte();

    /**
     *
     * @return
     */
    public abstract String getPrinterUniqueID();

    /**
     *
     * @return
     */
    public abstract StringProperty printerFriendlyNameProperty();

    /**
     *
     * @return
     */
    public abstract String getPrinterFriendlyName();

    /**
     *
     * @param value
     */
    public abstract void setPrinterColour(Color value);

    /**
     *
     * @return
     */
    public abstract Color getPrinterColour();

    /**
     *
     * @return
     */
    public abstract ObjectProperty<Color> printerColourProperty();

    /**
     *
     * @param value
     */
    public abstract void setHeadXPosition(float value);

    /**
     *
     * @return
     */
    public abstract float getHeadXPosition();

    /**
     *
     * @return
     */
    public abstract FloatProperty headXPositionProperty();

    /**
     *
     * @param value
     */
    public abstract void setHeadYPosition(float value);

    /**
     *
     * @return
     */
    public abstract float getHeadYPosition();

    /**
     *
     * @return
     */
    public abstract FloatProperty headYPositionProperty();

    /**
     *
     * @param value
     */
    public abstract void setHeadZPosition(float value);

    /**
     *
     * @return
     */
    public abstract float getHeadZPosition();

    /**
     *
     * @return
     */
    public abstract FloatProperty headZPositionProperty();

    /**
     *
     * @param value
     */
    public abstract void setBPosition(float value);

    /**
     *
     * @return
     */
    public abstract float getBPosition();

    /**
     *
     * @return
     */
    public abstract FloatProperty BPositionProperty();

    /**
     *
     * @param value
     */
    public abstract void setPrinterConnected(boolean value);

    /**
     *
     * @return
     */
    public abstract boolean getPrinterConnected();

    /**
     *
     * @return
     */
    public abstract BooleanProperty printerConnectedProperty();

    /**
     *
     * @param value
     */
    public abstract void setAmbientTemperature(int value);

    /**
     *
     * @return
     */
    public abstract float getAmbientTemperature();

    /**
     *
     * @return
     */
    public abstract IntegerProperty ambientTemperatureProperty();

    /**
     *
     * @return
     */
    public abstract XYChart.Series<Number, Number> ambientTemperatureHistory();

    /**
     *
     * @return
     */
    public abstract XYChart.Series<Number, Number> ambientTargetTemperatureHistory();

    /**
     *
     * @param value
     */
    public abstract void setAmbientTargetTemperature(int value);

    /**
     *
     * @return
     */
    public abstract int getAmbientTargetTemperature();

    /**
     *
     * @return
     */
    public abstract IntegerProperty ambientTargetTemperatureProperty();

    /**
     *
     * @param value
     */
    public abstract void setAmbientFanOn(boolean value);

    /**
     *
     * @return
     */
    public abstract boolean getAmbientFanOn();

    /**
     *
     * @return
     */
    public abstract BooleanProperty ambientFanOnProperty();

    /**
     *
     * @return
     */
    public abstract HeaterMode getBedHeaterMode();

    /**
     *
     * @return
     */
    public abstract ObjectProperty<HeaterMode> getBedHeaterModeProperty();

    /**
     *
     * @return
     */
    public abstract HeaterMode getNozzleHeaterMode();

    /**
     *
     * @return
     */
    public abstract ObjectProperty<HeaterMode> getNozzleHeaterModeProperty();

    /**
     *
     * @param value
     */
    public abstract void setBedTemperature(int value);

    /**
     *
     * @return
     */
    public abstract int getBedTemperature();

    /**
     *
     * @return
     */
    public abstract IntegerProperty bedTemperatureProperty();

    /**
     *
     * @return
     */
    public abstract XYChart.Series<Number, Number> bedTemperatureHistory();

    /**
     *
     * @return
     */
    public abstract XYChart.Series<Number, Number> bedTargetTemperatureHistory();

    /**
     *
     * @param value
     */
    public abstract void setBedFirstLayerTargetTemperature(int value);

    /**
     *
     * @return
     */
    public abstract int getBedFirstLayerTargetTemperature();

    /**
     *
     * @return
     */
    public abstract IntegerProperty bedFirstLayerTargetTemperatureProperty();

    /**
     *
     * @param value
     */
    public abstract void setBedTargetTemperature(int value);

    /**
     *
     * @return
     */
    public abstract int getBedTargetTemperature();

    /**
     *
     * @return
     */
    public abstract IntegerProperty bedTargetTemperatureProperty();

    /**
     *
     * @param value
     */
    public abstract void setExtruderTemperature(int value);

    /**
     *
     * @return
     */
    public abstract int getExtruderTemperature();

    /**
     *
     * @return
     */
    public abstract IntegerProperty extruderTemperatureProperty();

    /**
     *
     * @return
     */
    public abstract XYChart.Series<Number, Number> nozzleTemperatureHistory();

    /**
     *
     * @return
     */
    public abstract XYChart.Series<Number, Number> nozzleTargetTemperatureHistory();

    /**
     *
     * @param value
     */
    public abstract void setNozzleFirstLayerTargetTemperature(int value);

    /**
     *
     * @return
     */
    public abstract int getNozzleFirstLayerTargetTemperature();

    /**
     *
     * @return
     */
    public abstract IntegerProperty nozzleFirstLayerTargetTemperatureProperty();

    /**
     *
     * @param value
     */
    public abstract void setNozzleTargetTemperature(int value);

    /**
     *
     * @return
     */
    public abstract int getNozzleTargetTemperature();

    /**
     *
     * @return
     */
    public abstract IntegerProperty nozzleTargetTemperatureProperty();

    /**
     *
     * @param value
     */
    public abstract void setHeadFanOn(boolean value);

    /**
     *
     * @return
     */
    public abstract boolean getHeadFanOn();

    /**
     *
     * @return
     */
    public abstract BooleanProperty headFanOnProperty();

    /**
     *
     * @param value
     */
    public abstract void setBusy(boolean value);

    /**
     *
     * @return
     */
    public abstract boolean getBusy();

    /**
     *
     * @return
     */
    public abstract BooleanProperty busyProperty();

    /**
     *
     * @param value
     */
    public abstract void setPauseStatus(PauseStatus value);

    /**
     *
     * @return
     */
    public abstract PauseStatus getPauseStatus();

    /**
     *
     * @return
     */
    public abstract ObjectProperty<PauseStatus> pauseStatusProperty();

    /**
     *
     * @param value
     */
    public abstract void setWhyAreWeWaiting(WhyAreWeWaitingState value);

    /**
     *
     * @return
     */
    public abstract WhyAreWeWaitingState getWhyAreWeWaiting();

    /**
     *
     * @return
     */
    public abstract StringProperty getWhyAreWeWaitingStringProperty();

    /**
     *
     * @return
     */
    public abstract ObjectProperty<WhyAreWeWaitingState> whyAreWeWaitingProperty();

    /*
     * Errors
     */
    /**
     *
     * @param value
     */
    public abstract void setErrorsDetected(boolean value);

    /**
     *
     * @return
     */
    public abstract boolean getErrorsDetected();

    /**
     *
     * @return
     */
    public abstract BooleanProperty errorsDetectedProperty();

    /**
     *
     * @return
     */
    public abstract BooleanProperty SDCardErrorProperty();

    /**
     *
     * @return
     */
    public abstract boolean getSDCardError();

    /**
     *
     * @param SDCardError
     */
    public abstract void setSDCardError(boolean SDCardError);

    /**
     *
     * @return
     */
    public abstract BooleanProperty chunkSequenceErrorProperty();

    /**
     *
     * @return
     */
    public abstract boolean getChunkSequenceError();

    /**
     *
     * @param ChunkSequenceError
     */
    public abstract void setChunkSequenceError(boolean ChunkSequenceError);

    /**
     *
     * @return
     */
    public abstract BooleanProperty fileTooLargeErrorProperty();

    /**
     *
     * @return
     */
    public abstract boolean getFileTooLargeError();

    /**
     *
     * @param FileTooLargeError
     */
    public abstract void setFileTooLargeError(boolean FileTooLargeError);

    /**
     *
     * @return
     */
    public abstract BooleanProperty GCodeLineTooLongErrorProperty();

    /**
     *
     * @return
     */
    public abstract boolean getGCodeLineTooLongError();

    /**
     *
     * @param GCodeLineTooLongError
     */
    public abstract void setGCodeLineTooLongError(boolean GCodeLineTooLongError);

    /**
     *
     * @return
     */
    public abstract BooleanProperty USBRxErrorProperty();

    /**
     *
     * @return
     */
    public abstract boolean getUSBRxError();

    /**
     *
     * @param USBRxError
     */
    public abstract void setUSBRxError(boolean USBRxError);

    /**
     *
     * @return
     */
    public abstract BooleanProperty USBTxErrorProperty();

    /**
     *
     * @return
     */
    public abstract boolean getUSBTxError();

    /**
     *
     * @param USBTxError
     */
    public abstract void setUSBTxError(boolean USBTxError);

    /**
     *
     * @return
     */
    public abstract BooleanProperty BadCommandErrorProperty();

    /**
     *
     * @return
     */
    public abstract boolean getBadCommandError();

    /**
     *
     * @param BadCommandError
     */
    public abstract void setBadCommandError(boolean BadCommandError);

    /**
     *
     * @return
     */
    public abstract BooleanProperty EEPROMErrorProperty();

    /**
     *
     * @return
     */
    public abstract boolean getEEPROMError();

    /**
     *
     * @param EEPROMError
     */
    public abstract void setEEPROMError(boolean EEPROMError);

    /*
     * Switches
     */
    /**
     *
     * @return
     */
    public abstract BooleanProperty XStopSwitchProperty();

    /**
     *
     * @return
     */
    public abstract boolean getXStopSwitch();

    /**
     *
     * @param XStopSwitch
     */
    public abstract void setXStopSwitch(boolean XStopSwitch);

    /**
     *
     * @return
     */
    public abstract BooleanProperty YStopSwitchProperty();

    /**
     *
     * @return
     */
    public abstract boolean getYStopSwitch();

    /**
     *
     * @param YStopSwitch
     */
    public abstract void setYStopSwitch(boolean YStopSwitch);

    /**
     *
     * @return
     */
    public abstract BooleanProperty ZStopSwitchProperty();

    /**
     *
     * @return
     */
    public abstract boolean getZStopSwitch();

    /**
     *
     * @param ZStopSwitch
     */
    public abstract void setZStopSwitch(boolean ZStopSwitch);

    /**
     *
     * @return
     */
    public abstract BooleanProperty ZTopStopSwitchProperty();

    /**
     *
     * @return
     */
    public abstract boolean getZTopStopSwitch();

    /**
     *
     * @param value
     */
    public abstract void setZTopStopSwitch(boolean value);

    /**
     *
     * @return
     */
    public abstract BooleanProperty Filament1LoadedProperty();

    /**
     *
     * @return
     */
    public abstract boolean getFilament1Loaded();

    /**
     *
     * @param FilamentLoaded
     */
    public abstract void setFilament1Loaded(boolean FilamentLoaded);

    /**
     *
     * @return
     */
    public abstract BooleanProperty Filament2LoadedProperty();

    /**
     *
     * @return
     */
    public abstract boolean getFilament2Loaded();

    /**
     *
     * @param FilamentLoaded
     */
    public abstract void setFilament2Loaded(boolean FilamentLoaded);

    /**
     *
     * @return
     */
    public abstract BooleanProperty Filament1IndexProperty();

    /**
     *
     * @return
     */
    public abstract boolean getFilament1Index();

    /**
     *
     * @param FilamentIndex
     */
    public abstract void setFilament1Index(boolean FilamentIndex);

    /**
     *
     * @return
     */
    public abstract BooleanProperty Filament2IndexProperty();

    /**
     *
     * @return
     */
    public abstract boolean getFilament2Index();

    /**
     *
     * @param FilamentIndex
     */
    public abstract void setFilament2Index(boolean FilamentIndex);

    /**
     *
     * @return
     */
    public abstract BooleanProperty reelButtonProperty();

    /**
     *
     * @return
     */
    public abstract boolean getReelButton();

    /**
     *
     * @param FilamentIndex
     */
    public abstract void setReelButton(boolean FilamentIndex);

    /*
     * Head data
     */
    /**
     *
     * @return
     */
    public abstract ObjectProperty<Head> attachedHeadProperty();

    /**
     *
     * @return
     */
    public abstract BooleanProperty getHeadDataChangedToggle();

    /**
     *
     * @return
     */
    public abstract ObjectProperty<EEPROMState> headEEPROMStatusProperty();

    /**
     *
     * @return
     */
    public abstract EEPROMState getHeadEEPROMStatus();

    /**
     *
     * @return
     */
    public abstract StringProperty getHeadTypeCode();

    /**
     *
     * @return
     */
    public abstract StringProperty getHeadType();

    /**
     *
     * @return
     */
    public abstract StringProperty getHeadUniqueID();

    /**
     *
     * @return
     */
    public abstract FloatProperty getHeadMaximumTemperature();

    /**
     *
     * @return
     */
    public abstract FloatProperty getHeadThermistorBeta();

    /**
     *
     * @return
     */
    public abstract FloatProperty getHeadThermistorTCal();

    /**
     *
     * @return
     */
    public abstract FloatProperty getHeadNozzle1XOffset();

    /**
     *
     * @return
     */
    public abstract FloatProperty getHeadNozzle1YOffset();

    /**
     *
     * @return
     */
    public abstract FloatProperty getHeadNozzle1ZOffset();

    /**
     *
     * @return
     */
    public abstract FloatProperty getHeadNozzle1BOffset();

    /**
     *
     * @return
     */
    public abstract FloatProperty getHeadNozzle2XOffset();

    /**
     *
     * @return
     */
    public abstract FloatProperty getHeadNozzle2YOffset();

    /**
     *
     * @return
     */
    public abstract FloatProperty getHeadNozzle2ZOffset();

    /**
     *
     * @return
     */
    public abstract FloatProperty getHeadNozzle2BOffset();

    /**
     *
     * @return
     */
    public abstract FloatProperty getHeadHoursCounter();

    /**
     *
     * @return
     */
    public abstract FloatProperty getLastFilamentTemperature();
    /*
     * Reel data
     */

    /**
     *
     * @return
     */
    public abstract ObjectProperty<Filament> loadedFilamentProperty();

    /**
     *
     * @return
     */
    public abstract BooleanProperty reelDataChangedProperty();

    /**
     *
     * @return
     */
    public abstract ObjectProperty<EEPROMState> reelEEPROMStatusProperty();

    /**
     *
     * @return
     */
    public abstract EEPROMState getReelEEPROMStatus();

    /**
     *
     * @return
     */
    public abstract StringProperty reelFriendlyNameProperty();

    /**
     *
     * @return
     */
    public abstract String getReelFriendlyName();
    
    public abstract MaterialType getReelMaterialType();
    
    public abstract Color getReelDisplayColour();

    /**
     *
     * @return
     */
    public abstract IntegerProperty getReelAmbientTemperature();

    /**
     *
     * @return
     */
    public abstract IntegerProperty getReelBedTemperature();

    /**
     *
     * @return
     */
    public abstract FloatProperty getReelFilamentDiameter();

    /**
     *
     * @return
     */
    public abstract IntegerProperty getReelFirstLayerBedTemperature();

    /**
     *
     * @return
     */
    public abstract IntegerProperty getReelFirstLayerNozzleTemperature();

    /**
     *
     * @return
     */
    public abstract FloatProperty getReelFilamentMultiplier();

    /**
     *
     * @return
     */
    public abstract FloatProperty getReelFeedRateMultiplier();

    /**
     *
     * @return
     */
    public abstract IntegerProperty getReelNozzleTemperature();

    /**
     *
     * @return
     */
    public abstract FloatProperty getReelRemainingFilament();

    /**
     *
     * @return
     */
    public abstract StringProperty getReelFilamentID();

    /*
     *
     */
    /**
     *
     * @return
     */
    public abstract BooleanProperty NozzleHomedProperty();

    /**
     *
     * @return
     */
    public abstract boolean getNozzleHomed();

    /**
     *
     * @param NozzleHomed
     */
    public abstract void setNozzleHomed(boolean NozzleHomed);

    /**
     *
     * @return
     */
    public abstract BooleanProperty LidOpenProperty();

    /**
     *
     * @return
     */
    public abstract boolean getLidOpen();

    /**
     *
     * @param LidOpen
     */
    public abstract void setLidOpen(boolean LidOpen);

    /**
     *
     * @return
     */
    public abstract BooleanProperty sdCardPresentProperty();

    /**
     *
     * @return
     */
    public abstract boolean sdCardPresent();

    /*
     * Firmware
     */
    /**
     *
     * @param value
     */
    public abstract void setFirmwareVersion(String value);

    /**
     *
     * @return
     */
    public abstract String getFirmwareVersion();

    /**
     *
     * @return
     */
    public abstract StringProperty firmwareVersionProperty();

    /**
     *
     * @param value
     */
    public abstract void setPrinterStatus(PrinterStatusEnumeration value);

    /**
     *
     * @return
     */
    public abstract PrinterStatusEnumeration getPrinterStatus();

    /**
     *
     * @return
     */
    public abstract ObjectProperty<PrinterStatusEnumeration> printerStatusProperty();

    /**
     *
     * @param value
     */
    public abstract void setPrintJobLineNumber(int value);

    /**
     *
     * @return
     */
    public abstract int getPrintJobLineNumber();

    /**
     *
     * @return
     */
    public abstract IntegerProperty printJobLineNumberProperty();

    /**
     *
     * @param value
     */
    public abstract void setPrintJobID(String value);

    /**
     *
     * @return
     */
    public abstract String getPrintJobID();

    /**
     *
     * @return
     */
    public abstract StringProperty printJobIDProperty();

    /*
     * Error lists for tooltips
     */
    /**
     *
     * @param value
     */
    public abstract void setErrorList(String value);

    /**
     *
     * @return
     */
    public abstract String getErrorList();

    /**
     *
     * @return
     */
    public abstract StringProperty errorListProperty();

    /**
     *
     * @param printerEvent
     */
    public abstract void processRoboxEvent(RoboxEvent printerEvent);

    /**
     *
     * @param gcodeToSend
     */
    public abstract void addToGCodeTranscript(String gcodeToSend);

    /**
     *
     * @return
     */
    public abstract ObservableList<String> gcodeTranscriptProperty();

    /**
     *
     * @return
     */
    public abstract PrintQueue getPrintQueue();

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
    public abstract String transmitDirectGCode(String gcodeToSend, boolean addToTranscript) throws RoboxCommsException;

    /**
     *
     * @param macroName
     * @throws RoboxCommsException
     */
    public abstract void transmitStoredGCode(String macroName) throws RoboxCommsException;

    /**
     *
     * @param macroName
     * @param checkForPurge
     * @throws RoboxCommsException
     */
    public abstract void transmitStoredGCode(String macroName, boolean checkForPurge) throws RoboxCommsException;

    /**
     *
     * @throws RoboxCommsException
     */
    public abstract void transmitAbortPrint() throws RoboxCommsException;

    /**
     *
     * @throws RoboxCommsException
     */
    public abstract void transmitPausePrint() throws RoboxCommsException;

    /**
     *
     * @throws RoboxCommsException
     */
    public abstract void transmitResumePrint() throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public abstract AckResponse transmitFormatHeadEEPROM() throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public abstract AckResponse transmitFormatReelEEPROM() throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public abstract ReelEEPROMDataResponse transmitReadReelEEPROM() throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public abstract HeadEEPROMDataResponse transmitReadHeadEEPROM() throws RoboxCommsException;

    
    public abstract void transmitResetErrors() throws RoboxCommsException;
    
    
    public abstract AckResponse transmitReportErrors() throws RoboxCommsException;
    
    public abstract boolean transmitUpdateFirmware(final String firmwareID) throws RoboxCommsException;
    
    /**
     *
     * @param filament
     * @return
     * @throws RoboxCommsException
     */
    public abstract AckResponse transmitWriteReelEEPROM(Filament filament) throws RoboxCommsException;

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
    public abstract void transmitWriteReelEEPROM(String reelTypeCode, float reelFirstLayerNozzleTemperature, float reelNozzleTemperature,
            float reelFirstLayerBedTemperature, float reelBedTemperature, float reelAmbientTemperature, float reelFilamentDiameter,
            float reelFilamentMultiplier, float reelFeedRateMultiplier, float reelRemainingFilament,
            String friendlyName, MaterialType materialType, Color displayColour) throws RoboxCommsException;

    /**
     *
     * @param headToWrite
     * @throws RoboxCommsException
     */
    public abstract void transmitWriteHeadEEPROM(Head headToWrite) throws RoboxCommsException;

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
    public abstract AckResponse transmitWriteHeadEEPROM(String headTypeCode, String headUniqueID, float maximumTemperature,
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
    public abstract void switchOnHeadLEDs(boolean on) throws RoboxCommsException;

    /**
     *
     * @param colour
     * @throws RoboxCommsException
     */
    public abstract void transmitSetAmbientLEDColour(Color colour) throws RoboxCommsException;

    /**
     *
     * @param colour
     * @throws RoboxCommsException
     */
    public abstract void transmitSetReelLEDColour(Color colour) throws RoboxCommsException;

    /**
     *
     * @throws RoboxCommsException
     */
    public abstract void transmitReadPrinterID() throws RoboxCommsException;

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
    public abstract boolean transmitWritePrinterID(String model, String edition, String weekOfManufacture, String yearOfManufacture, String poNumber, String serialNumber, String checkByte, String printerFriendlyName, Color colour) throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public abstract FirmwareResponse transmitReadFirmwareVersion() throws RoboxCommsException;

    /**
     *
     * @param nozzleFirstLayerTarget
     * @param nozzleTarget
     * @param bedFirstLayerTarget
     * @param bedTarget
     * @param ambientTarget
     * @throws RoboxCommsException
     */
    public abstract void transmitSetTemperatures(double nozzleFirstLayerTarget, double nozzleTarget, double bedFirstLayerTarget, double bedTarget, double ambientTarget) throws RoboxCommsException;

    /**
     *
     * @param filamentDiameter
     * @param filamentMultiplier
     * @param feedRateMultiplier
     * @throws RoboxCommsException
     */
    public abstract void transmitSetFilamentInfo(double filamentDiameter, double filamentMultiplier, double feedRateMultiplier) throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public abstract ListFilesResponse transmitListFiles() throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public abstract StatusResponse transmitStatusRequest() throws RoboxCommsException;

    /**
     *
     * @param fileID
     * @return
     * @throws DatafileSendAlreadyInProgress
     * @throws RoboxCommsException
     */
    public abstract boolean initialiseDataFileSend(String fileID) throws DatafileSendAlreadyInProgress, RoboxCommsException;

    /**
     *
     * @param jobUUID
     * @throws RoboxCommsException
     */
    public abstract void initiatePrint(String jobUUID) throws RoboxCommsException;

    /**
     *
     * @param hexDigits
     * @param lastPacket
     * @param appendCRLF
     * @throws DatafileSendNotInitialised
     * @throws RoboxCommsException
     */
    public abstract void sendDataFileChunk(String hexDigits, boolean lastPacket, boolean appendCRLF) throws DatafileSendNotInitialised, RoboxCommsException;

    /**
     *
     * @param project
     * @param filament
     * @param printQuality
     * @param settings
     */
    public abstract void printProject(Project project, Filament filament, PrintQualityEnumeration printQuality, RoboxProfile settings);

    /**
     *
     */
    public abstract void abortPrint();

    /**
     *
     */
    public abstract void pausePrint();

    /**
     *
     */
    public abstract void resumePrint();

    /**
     *
     * @return
     */
    public abstract int getSequenceNumber();

    /**
     *
     * @return
     */
    public abstract boolean isPrintInitiated();

    /**
     *
     * @param reelNozzleTemperature
     */
    public abstract void transmitWriteMaterialTemperatureToHeadEEPROM(int reelNozzleTemperature);

    /**
     * Return if the filament on the reel is mutable (is a Robox predefined filament or not)
     * @return 
     */
    public abstract BooleanProperty getReelFilamentIsMutable();
    
}
