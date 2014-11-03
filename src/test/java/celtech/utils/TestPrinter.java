/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.configuration.MaterialType;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.SlicerParameters;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.ListFilesResponse;
import celtech.printerControl.comms.commands.rx.PrinterIDResponse;
import celtech.printerControl.comms.commands.rx.ReelEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.model.Extruder;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.PrintEngine;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterAncillarySystems;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.PrinterIdentity;
import celtech.printerControl.model.Reel;
import celtech.printerControl.model.calibration.NozzleHeightStateTransitionManager;
import celtech.printerControl.model.calibration.NozzleOpeningStateTransitionManager;
import celtech.printerControl.model.calibration.XAndYStateTransitionManager;
import celtech.services.printing.DatafileSendAlreadyInProgress;
import celtech.services.printing.DatafileSendNotInitialised;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.utils.tasks.TaskResponder;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

/**
 *
 * @author tony
 */
class TestPrinter implements Printer
{
    private final SimpleObjectProperty<Head> headProperty = new SimpleObjectProperty<>();
    private final ObservableList<Reel> reelsProperty = FXCollections.observableArrayList();
    
    void addHead()
    {
        HeadFile headFile = new HeadFile();
        Head head = new Head(headFile);
        headProperty.setValue(head);
    }
    
    void removeHead() {
        headProperty.setValue(null);
    }
    
    void addReel(int i)
    {
        Reel reel = new Reel();
        reelsProperty.add(reel);
    }    
    
    void removeReel(int i)
    {
        reelsProperty.remove(reelsProperty.get(0));
    }
    
    void changeReel(int i) {
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
        eepromData.setReelMaterialType(MaterialType.Nylon);
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void forceHeadReset()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AckResponse formatHeadEEPROM() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AckResponse formatReelEEPROM() throws PrinterException
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
    public void hardResetHead()
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
    public boolean initialiseDataFileSend(String fileID) throws DatafileSendAlreadyInProgress, RoboxCommsException
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
    public void prepareToPurgeHead(TaskResponder responder) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void printProject(Project project, Filament filament,
        PrintQualityEnumeration printQuality, SlicerParameters settings)
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
    public void purgeHead(TaskResponder responder) throws PrinterException
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
    public ReelEEPROMDataResponse readReelEEPROM() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ObservableList<Reel> reelsProperty()
    {
        return reelsProperty;
    }

    @Override
    public void removeHead(TaskResponder responder) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void repairHeadIfNecessary()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void resume() throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void runMacro(String macroName) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void runMacroWithoutPurgeCheck(String macroName) throws PrinterException
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
    public void setPurgeTemperature(float purgeTemperature)
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
    public void transmitPausePrint() throws RoboxCommsException
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
    public void transmitSetFilamentInfo(double filamentDiameter, double filamentMultiplier,
        double feedRateMultiplier) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void transmitSetTemperatures(double nozzleFirstLayerTarget, double nozzleTarget,
        double bedFirstLayerTarget, double bedTarget, double ambientTarget) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StatusResponse transmitStatusRequest() throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean transmitUpdateFirmware(String firmwareID) throws RoboxCommsException
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
    public AckResponse transmitWriteReelEEPROM(Filament filament) throws RoboxCommsException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void transmitWriteReelEEPROM(String filamentID, float reelFirstLayerNozzleTemperature,
        float reelNozzleTemperature, float reelFirstLayerBedTemperature, float reelBedTemperature,
        float reelAmbientTemperature, float reelFilamentDiameter, float reelFilamentMultiplier,
        float reelFeedRateMultiplier, float reelRemainingFilament, String friendlyName,
        MaterialType materialType, Color displayColour) throws RoboxCommsException
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
    public void probeBed()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getZDelta() throws PrinterException
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
    public void repairHead(String receivedTypeCode) throws PrinterException
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean printJobIDIndicatesPrinting()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
