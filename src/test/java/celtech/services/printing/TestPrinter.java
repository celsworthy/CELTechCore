/*
 * Copyright 2014 CEL UK
 */
package celtech.services.printing;

import celtech.appManager.Project;
import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.Head;
import celtech.configuration.HeaterMode;
import celtech.configuration.PauseStatus;
import celtech.configuration.WhyAreWeWaitingState;
import celtech.printerControl.Printer;
import celtech.printerControl.PrinterStatusEnumeration;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.ListFilesResponse;
import celtech.printerControl.comms.commands.rx.ReelEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.comms.events.RoboxEvent;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;

/**
 *
 * @author tony
 */
public class TestPrinter implements Printer
{

    private IntegerProperty printJobLineNumber = new SimpleIntegerProperty(0);
    private IntegerProperty bedTargetTemperature = new SimpleIntegerProperty(100);
    private IntegerProperty bedTemperature = new SimpleIntegerProperty(22);
    private boolean printInitiated = false;
    private int sequenceNumber;
    
    private ListFilesResponse listFilesResonse;

    @Override
    public IntegerProperty printJobLineNumberProperty()
    {
        return printJobLineNumber;
    }

    @Override
    public void setPrintJobLineNumber(int value)
    {
        printJobLineNumber.set(value);
    }

    @Override
    public StringProperty printJobIDProperty()
    {
        return new SimpleStringProperty("JobIdA");
    }

    @Override
    public int getBedTargetTemperature()
    {
        return bedTargetTemperature.get();
    }

    @Override
    public IntegerProperty bedTargetTemperatureProperty()
    {
        return bedTargetTemperature;
    }

    @Override
    public int getBedTemperature()
    {
        return bedTemperature.get();
    }

    @Override
    public IntegerProperty bedTemperatureProperty()
    {
        return bedTemperature;
    }

    @Override
    public void setBedTemperature(int temperature)
    {
        bedTemperature.set(temperature);
    }

    @Override
    public void setBedTargetTemperature(int temperature)
    {
        bedTargetTemperature.set(temperature);
    }

    @Override
    public void setPrintJobID(String value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPrintJobID()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPrinterPort()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty getPrinterIDDataChangedToggle()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty getPrintermodel()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty getPrinteredition()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty getPrinterweekOfManufacture()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty getPrinteryearOfManufacture()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty getPrinterpoNumber()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty getPrinterserialNumber()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty getPrintercheckByte()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPrinterUniqueID()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty printerFriendlyNameProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPrinterFriendlyName()
    {
        return "TestPrinter";
    }

    @Override
    public void setPrinterColour(Color value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Color getPrinterColour()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectProperty<Color> printerColourProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setHeadXPosition(float value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float getHeadXPosition()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty headXPositionProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setHeadYPosition(float value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float getHeadYPosition()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty headYPositionProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setHeadZPosition(float value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float getHeadZPosition()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty headZPositionProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setBPosition(float value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float getBPosition()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty BPositionProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPrinterConnected(boolean value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getPrinterConnected()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty printerConnectedProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAmbientTemperature(int value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float getAmbientTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IntegerProperty ambientTemperatureProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public XYChart.Series<Number, Number> ambientTemperatureHistory()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public XYChart.Series<Number, Number> ambientTargetTemperatureHistory()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAmbientTargetTemperature(int value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getAmbientTargetTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IntegerProperty ambientTargetTemperatureProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAmbientFanOn(boolean value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getAmbientFanOn()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty ambientFanOnProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public HeaterMode getBedHeaterMode()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectProperty<HeaterMode> getBedHeaterModeProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public HeaterMode getNozzleHeaterMode()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectProperty<HeaterMode> getNozzleHeaterModeProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public XYChart.Series<Number, Number> bedTemperatureHistory()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public XYChart.Series<Number, Number> bedTargetTemperatureHistory()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setBedFirstLayerTargetTemperature(int value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getBedFirstLayerTargetTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IntegerProperty bedFirstLayerTargetTemperatureProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setExtruderTemperature(int value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getExtruderTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IntegerProperty extruderTemperatureProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public XYChart.Series<Number, Number> nozzleTemperatureHistory()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public XYChart.Series<Number, Number> nozzleTargetTemperatureHistory()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setNozzleFirstLayerTargetTemperature(int value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getNozzleFirstLayerTargetTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IntegerProperty nozzleFirstLayerTargetTemperatureProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setNozzleTargetTemperature(int value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getNozzleTargetTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IntegerProperty nozzleTargetTemperatureProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setHeadFanOn(boolean value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getHeadFanOn()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty headFanOnProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setBusy(boolean value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getBusy()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty busyProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPauseStatus(PauseStatus value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PauseStatus getPauseStatus()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectProperty<PauseStatus> pauseStatusProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setWhyAreWeWaiting(WhyAreWeWaitingState value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WhyAreWeWaitingState getWhyAreWeWaiting()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty getWhyAreWeWaitingStringProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectProperty<WhyAreWeWaitingState> whyAreWeWaitingProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setErrorsDetected(boolean value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getErrorsDetected()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty errorsDetectedProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty SDCardErrorProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getSDCardError()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setSDCardError(boolean SDCardError)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty chunkSequenceErrorProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getChunkSequenceError()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setChunkSequenceError(boolean ChunkSequenceError)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty fileTooLargeErrorProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getFileTooLargeError()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setFileTooLargeError(boolean FileTooLargeError)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty GCodeLineTooLongErrorProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getGCodeLineTooLongError()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setGCodeLineTooLongError(boolean GCodeLineTooLongError)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty USBRxErrorProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getUSBRxError()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setUSBRxError(boolean USBRxError)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty USBTxErrorProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getUSBTxError()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setUSBTxError(boolean USBTxError)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty BadCommandErrorProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getBadCommandError()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setBadCommandError(boolean BadCommandError)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty EEPROMErrorProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getEEPROMError()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setEEPROMError(boolean EEPROMError)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty XStopSwitchProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getXStopSwitch()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setXStopSwitch(boolean XStopSwitch)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty YStopSwitchProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getYStopSwitch()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setYStopSwitch(boolean YStopSwitch)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty ZStopSwitchProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getZStopSwitch()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setZStopSwitch(boolean ZStopSwitch)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty ZTopStopSwitchProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getZTopStopSwitch()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setZTopStopSwitch(boolean value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty Filament1LoadedProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getFilament1Loaded()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setFilament1Loaded(boolean FilamentLoaded)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty Filament2LoadedProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getFilament2Loaded()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setFilament2Loaded(boolean FilamentLoaded)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty Filament1IndexProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getFilament1Index()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setFilament1Index(boolean FilamentIndex)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty Filament2IndexProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getFilament2Index()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setFilament2Index(boolean FilamentIndex)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty reelButtonProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getReelButton()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setReelButton(boolean FilamentIndex)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectProperty<Head> attachedHeadProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty getHeadDataChangedToggle()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectProperty<EEPROMState> headEEPROMStatusProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EEPROMState getHeadEEPROMStatus()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty getHeadTypeCode()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty getHeadType()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty getHeadUniqueID()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getHeadMaximumTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getHeadThermistorBeta()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getHeadThermistorTCal()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getHeadNozzle1XOffset()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getHeadNozzle1YOffset()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getHeadNozzle1ZOffset()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getHeadNozzle1BOffset()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getHeadNozzle2XOffset()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getHeadNozzle2YOffset()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getHeadNozzle2ZOffset()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getHeadNozzle2BOffset()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getHeadHoursCounter()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getLastFilamentTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectProperty<Filament> loadedFilamentProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty reelDataChangedProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectProperty<EEPROMState> reelEEPROMStatusProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EEPROMState getReelEEPROMStatus()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty reelFriendlyNameProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getReelFriendlyName()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IntegerProperty getReelAmbientTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IntegerProperty getReelBedTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getReelFilamentDiameter()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IntegerProperty getReelFirstLayerBedTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IntegerProperty getReelFirstLayerNozzleTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getReelFilamentMultiplier()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getReelFeedRateMultiplier()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IntegerProperty getReelNozzleTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FloatProperty getReelRemainingFilament()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty getReelTypeCode()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty getReelUniqueID()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty NozzleHomedProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getNozzleHomed()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setNozzleHomed(boolean NozzleHomed)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty LidOpenProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getLidOpen()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setLidOpen(boolean LidOpen)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BooleanProperty sdCardPresentProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean sdCardPresent()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setFirmwareVersion(String value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getFirmwareVersion()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty firmwareVersionProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPrinterStatus(PrinterStatusEnumeration value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PrinterStatusEnumeration getPrinterStatus()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectProperty<PrinterStatusEnumeration> printerStatusProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getPrintJobLineNumber()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setErrorList(String value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getErrorList()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringProperty errorListProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void processRoboxEvent(RoboxEvent printerEvent)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addToGCodeTranscript(String gcodeToSend)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObservableList<String> gcodeTranscriptProperty()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PrintQueue getPrintQueue()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String transmitDirectGCode(String gcodeToSend, boolean addToTranscript) throws RoboxCommsException
    {
        return "";
    }

    @Override
    public void transmitStoredGCode(String macroName) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void transmitStoredGCode(String macroName, boolean checkForPurge) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void transmitAbortPrint() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void transmitPausePrint() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void transmitResumePrint() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AckResponse transmitFormatHeadEEPROM() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AckResponse transmitFormatReelEEPROM() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ReelEEPROMDataResponse transmitReadReelEEPROM() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public HeadEEPROMDataResponse transmitReadHeadEEPROM() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AckResponse transmitWriteReelEEPROM(Filament filament) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void transmitWriteReelEEPROM(String reelTypeCode, String reelUniqueID,
            float reelFirstLayerNozzleTemperature, float reelNozzleTemperature,
            float reelFirstLayerBedTemperature, float reelBedTemperature,
            float reelAmbientTemperature, float reelFilamentDiameter,
            float reelFilamentMultiplier, float reelFeedRateMultiplier,
            float reelRemainingFilament) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void transmitWriteHeadEEPROM(Head headToWrite) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AckResponse transmitWriteHeadEEPROM(String headTypeCode, String headUniqueID,
            float maximumTemperature, float thermistorBeta, float thermistorTCal,
            float nozzle1XOffset, float nozzle1YOffset, float nozzle1ZOffset,
            float nozzle1BOffset, float nozzle2XOffset, float nozzle2YOffset,
            float nozzle2ZOffset, float nozzle2BOffset, float lastFilamentTemperature,
            float hourCounter) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void switchOnHeadLEDs(boolean on) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void transmitSetAmbientLEDColour(Color colour) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void transmitSetReelLEDColour(Color colour) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void transmitReadPrinterID() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean transmitWritePrinterID(String model, String edition,
            String weekOfManufacture, String yearOfManufacture, String poNumber,
            String serialNumber, String checkByte, String printerFriendlyName,
            Color colour) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FirmwareResponse transmitReadFirmwareVersion() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void transmitSetTemperatures(double nozzleFirstLayerTarget, double nozzleTarget,
            double bedFirstLayerTarget, double bedTarget, double ambientTarget) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void transmitSetFilamentInfo(double filamentDiameter, double filamentMultiplier,
            double feedRateMultiplier) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ListFilesResponse transmitListFiles() throws RoboxCommsException
    {
        return listFilesResonse;
    }

    @Override
    public StatusResponse transmitStatusRequest() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean initialiseDataFileSend(String fileID) throws DatafileSendAlreadyInProgress, RoboxCommsException
    {
        sequenceNumber = 0;
        return true;
    }

    @Override
    public void initiatePrint(String jobUUID) throws RoboxCommsException
    {
        printInitiated = true;
    }

    @Override
    public void sendDataFileChunk(String hexDigits, boolean lastPacket, boolean appendCRLF) throws DatafileSendNotInitialised, RoboxCommsException
    {

    }

    @Override
    public void printProject(Project project, Filament filament,
            PrintQualityEnumeration printQuality, RoboxProfile settings)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void abortPrint()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void pausePrint()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void resumePrint()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getSequenceNumber()
    {
        return sequenceNumber;
    }

    @Override
    public boolean isPrintInitiated()
    {
        return printInitiated;
    }

    @Override
    public void transmitWriteMaterialTemperatureToHeadEEPROM(int reelNozzleTemperature)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @param listFilesResonse the listFilesResonse to set
     */
    public void setListFilesResonse(ListFilesResponse listFilesResonse)
    {
        this.listFilesResonse = listFilesResonse;
    }

    @Override
    public void transmitResetErrors() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AckResponse transmitReportErrors() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean transmitUpdateFirmware(String firmwareID) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
