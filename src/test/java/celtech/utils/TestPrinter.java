/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.configuration.MaterialType;
import celtech.configuration.PrinterEdition;
import celtech.configuration.PrinterModel;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.printerControl.PrintActionUnavailableException;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.ListFilesResponse;
import celtech.printerControl.comms.commands.rx.PrinterIDResponse;
import celtech.printerControl.comms.commands.rx.ReelEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.comms.events.ErrorConsumer;
import celtech.printerControl.model.Extruder;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.PrintEngine;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterAncillarySystems;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.PrinterIdentity;
import celtech.printerControl.model.PrinterMetaStatus;
import celtech.printerControl.model.PurgeStateTransitionManager;
import celtech.printerControl.model.Reel;
import celtech.printerControl.model.TemperatureAndPWMData;
import celtech.printerControl.model.calibration.NozzleHeightStateTransitionManager;
import celtech.printerControl.model.calibration.NozzleOpeningStateTransitionManager;
import celtech.printerControl.model.calibration.XAndYStateTransitionManager;
import celtech.services.printing.DatafileSendAlreadyInProgress;
import celtech.services.printing.DatafileSendNotInitialised;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.utils.tasks.Cancellable;
import celtech.utils.tasks.TaskResponder;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;

/**
 *
 * @author tony
 */
public class TestPrinter implements Printer
{

    private final SimpleObjectProperty<Head> headProperty = new SimpleObjectProperty<>();
    private final ObservableMap<Integer, Reel> reelsProperty = FXCollections.observableHashMap();
    private int numExtruders = 1;
    
    public TestPrinter()
    {
        this(1);
    }

    public TestPrinter(int numExtruders)
    {
        this.numExtruders = numExtruders;
    }

    void addHead()
    {
        HeadFile headFile = new HeadFile();
        Head head = new Head(headFile);
        headProperty.setValue(head);
    }

    void removeHead()
    {
        headProperty.setValue(null);
    }

    void addReel(int i)
    {
        Reel reel = new Reel();
        reelsProperty.put(i, reel);
    }

    void removeReel(int i)
    {
        reelsProperty.remove(i);
    }

    void changeReel(int i)
    {
        ReelEEPROMDataResponse eepromData = new ReelEEPROMDataResponse();
        eepromData.setReelFilamentID("ABC");
        eepromData.setReelAmbientTemperature(100);
        eepromData.setReelBedTemperature(120);
        eepromData.setReelDisplayColour(Color.DARKCYAN);
        eepromData.setReelFeedRateMultiplier(2);
        eepromData.setReelFilamentDiameter(3);
        eepromData.setReelFilamentMultiplier(2);
        eepromData.setReelFirstLayerBedTemperature(110);
        eepromData.setReelFirstLayerNozzleTemperature(180);
        eepromData.setReelFriendlyName("F1");
        eepromData.setReelMaterialType(MaterialType.NYL);
        eepromData.setReelNozzleTemperature(205);
        eepromData.setReelRemainingFilament(85);
        reelsProperty().get(i).updateFromEEPROMData(eepromData);
    }

    @Override
    public void addToGCodeTranscript(String gcodeToSend)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyBooleanProperty canCancelProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyBooleanProperty canPrintProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyBooleanProperty canPurgeHeadProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyBooleanProperty canRemoveHeadProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void cancel(TaskResponder responder) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void gotoNozzlePosition(float position)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void closeNozzleFully() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void ejectFilament(int extruderNumber, TaskResponder responder) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ObservableList<Extruder> extrudersProperty()
    {

        class FittedExtruder extends Extruder
        {

            public FittedExtruder(String extruderAxisLetter)
            {
                super(extruderAxisLetter);
                isFitted.set(true);
            }
        }

        class UnFittedExtruder extends Extruder
        {

            public UnFittedExtruder(String extruderAxisLetter)
            {
                super(extruderAxisLetter);
                isFitted.set(false);
            }
        }

        ObservableList<Extruder> extruders = FXCollections.observableList(new ArrayList<Extruder>());
        if (numExtruders == 0)
        {
            extruders.add(new UnFittedExtruder("D"));
            extruders.add(new UnFittedExtruder("E"));
        } else if (numExtruders == 1)
        {
            extruders.add(new FittedExtruder("D"));
            extruders.add(new UnFittedExtruder("E"));
        } else if (numExtruders == 2)
        {
            extruders.add(new FittedExtruder("D"));
            extruders.add(new FittedExtruder("E"));
        }
        return extruders;
    }

    @Override
    public AckResponse formatHeadEEPROM() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ObservableList<String> gcodeTranscriptProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyBooleanProperty canPauseProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyBooleanProperty canResumeProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getDataFileSequenceNumber()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PrintEngine getPrintEngine()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PrinterAncillarySystems getPrinterAncillarySystems()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PrinterIdentity getPrinterIdentity()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void goToOpenDoorPosition(TaskResponder responder) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void goToTargetBedTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void goToTargetNozzleTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void goToZPosition(double position)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyObjectProperty<Head> headProperty()
    {
        return headProperty;
    }

    @Override
    public void homeZ()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean initialiseDataFileSend(String fileID, boolean jobCanBeReprinted) throws DatafileSendAlreadyInProgress, RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void initiatePrint(String jobUUID) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isPrintInitiated()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void jogAxis(AxisSpecifier axis, float distance, float feedrate, boolean use_G1) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void openNozzleFully() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void pause() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void printProject(Project project)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyObjectProperty<PrinterStatus> printerStatusProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void processRoboxResponse(RoboxRxPacket rxPacket)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FirmwareResponse readFirmwareVersion() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public HeadEEPROMDataResponse readHeadEEPROM() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PrinterIDResponse readPrinterID() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeHead(TaskResponder responder) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void resume() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void selectNozzle(int nozzleNumber) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void sendDataFileChunk(String hexDigits, boolean lastPacket, boolean appendCRLF) throws DatafileSendNotInitialised, RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void sendRawGCode(String gCode, boolean addToTranscript)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setAmbientLEDColour(Color colour) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setAmbientTemperature(int targetTemperature)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setBedFirstLayerTargetTemperature(int targetTemperature)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setBedTargetTemperature(int targetTemperature)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setNozzleFirstLayerTargetTemperature(int targetTemperature)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setNozzleTargetTemperature(int targetTemperature)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setReelLEDColour(Color colour) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void shutdown()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void switchAllNozzleHeatersOff()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void switchBedHeaterOff()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void switchNozzleHeaterOff(int heaterNumber)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void switchOffHeadFan() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void switchOffHeadLEDs() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void switchOnHeadFan() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void switchOnHeadLEDs() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void switchToAbsoluteMoveMode()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void switchToRelativeMoveMode()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ListFilesResponse transmitListFiles() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AckResponse transmitReportErrors() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void transmitResetErrors() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StatusResponse transmitStatusRequest() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean transmitUpdateFirmware(String firmwareID) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AckResponse transmitWriteHeadEEPROM(String headTypeCode, String headUniqueID,
        float maximumTemperature, float thermistorBeta, float thermistorTCal, float nozzle1XOffset,
        float nozzle1YOffset, float nozzle1ZOffset, float nozzle1BOffset, float nozzle2XOffset,
        float nozzle2YOffset, float nozzle2ZOffset, float nozzle2BOffset,
        float lastFilamentTemperature, float hourCounter) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updatePrinterDisplayColour(Color displayColour) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updatePrinterName(String chosenPrinterName) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public float getZDelta() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeHeadEEPROM(Head headToWrite) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPrinterStatus(PrinterStatus printerStatus)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyIntegerProperty printJobLineNumberProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyStringProperty printJobIDProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyObjectProperty pauseStatusProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public XAndYStateTransitionManager startCalibrateXAndY()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NozzleHeightStateTransitionManager startCalibrateNozzleHeight()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyBooleanProperty canCalibrateHeadProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public NozzleOpeningStateTransitionManager startCalibrateNozzleOpening() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void executeGCodeFile(String fileName, boolean monitorForErrors) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void resetHeadToDefaults() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void inhibitHeadIntegrityChecks(boolean inhibit)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReelEEPROMDataResponse readReelEEPROM(int reelNumber) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AckResponse transmitWriteReelEEPROM(int reelNumber, Filament filament) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void transmitWriteReelEEPROM(int reelNumber, String filamentID,
        float reelFirstLayerNozzleTemperature, float reelNozzleTemperature,
        float reelFirstLayerBedTemperature,
        float reelBedTemperature, float reelAmbientTemperature, float reelFilamentDiameter,
        float reelFilamentMultiplier, float reelFeedRateMultiplier, float reelRemainingFilament,
        String friendlyName,
        MaterialType materialType, Color displayColour) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void changeFeedRateMultiplier(double feedRate) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void transmitSetTemperatures(double nozzle0FirstLayerTarget, double nozzle0Target,
        double nozzle1FirstLayerTarget, double nozzle1Target, double bedFirstLayerTarget,
        double bedTarget,
        double ambientTarget) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AckResponse formatReelEEPROM(int reelNumber) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ObservableMap<Integer, Reel> reelsProperty()
    {
        return reelsProperty;
    }

    @Override
    public void registerErrorConsumer(ErrorConsumer errorConsumer,
        List<FirmwareError> errorsOfInterest)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deregisterErrorConsumer(ErrorConsumer errorConsumer)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void goToXYPosition(double xPosition, double yPosition)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void levelGantryRaw()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void goToOpenDoorPositionDontWait(TaskResponder responder) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void openNozzleFullyExtra() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyBooleanProperty canOpenDoorProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

//    @Override
    public void resetPurgeTemperature()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void registerErrorConsumerAllErrors(ErrorConsumer errorConsumer)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyBooleanProperty canOpenCloseNozzleProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyBooleanProperty canCalibrateNozzleHeightProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyBooleanProperty canCalibrateXYAlignmentProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyBooleanProperty canCalibrateNozzleOpeningProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void goToXYZPosition(double xPosition, double yPosition, double zPosition)
    {

    }

    public void updatePrinterModelAndEdition(PrinterModel model, PrinterEdition edition) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updatePrinterWeek(String weekIdentifier) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updatePrinterYear(String yearIdentifier) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updatePrinterPONumber(String poIdentifier) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updatePrinterSerialNumber(String serialIdentifier) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updatePrinterIDChecksum(String checksum) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void connectionEstablished()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Integer> requestDebugData(boolean addToGCodeTranscript)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void callbackWhenNotBusy(TaskResponder responder)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyObjectProperty busyStatusProperty()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void resetPurgeTemperature(PrinterSettings printerSettings)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean doFilamentSlipActionWhilePrinting(FirmwareError error)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void resetDataFileSequenceNumber()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDataFileSequenceNumberStartPoint(int startingSequenceNumber)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void changeFilamentInfo(String extruderLetter, double filamentDiameter,
        double extrusionMultiplier) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void homeX()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void homeY()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void probeX()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public float getXDelta() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void probeY()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public float getYDelta() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void probeZ()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void extrudeUntilSlip(int extruderNumber) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void suppressEEPROMAndSDErrorHandling(boolean suppress)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TemperatureAndPWMData getTemperatureAndPWMData() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PurgeStateTransitionManager startPurge() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PrinterMetaStatus getPrinterMetaStatus()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void transferGCodeFileToPrinterAndCallbackWhenDone(String string, TaskResponder taskResponder)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void homeAllAxes(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void purgeMaterial(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void levelGantry(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void levelY(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void executeGCodeFileWithoutPurgeCheck(String fileName, boolean monitorForErrors) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void levelGantryTwoPoints(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void runCommissioningTest(String macroName, Cancellable cancellable) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void ejectStuckMaterial(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void testX(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void testY(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void testZ(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
