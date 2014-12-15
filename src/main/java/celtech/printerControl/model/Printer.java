package celtech.printerControl.model;

import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.configuration.MaterialType;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.printerControl.MacroType;
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
import celtech.printerControl.comms.events.RoboxResponseConsumer;
import celtech.printerControl.model.calibration.NozzleHeightStateTransitionManager;
import celtech.printerControl.model.calibration.NozzleOpeningStateTransitionManager;
import celtech.printerControl.model.calibration.XAndYStateTransitionManager;
import celtech.services.printing.DatafileSendAlreadyInProgress;
import celtech.services.printing.DatafileSendNotInitialised;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.utils.AxisSpecifier;
import celtech.utils.tasks.TaskResponder;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;

/**
 *
 * @author Ian
 */
public interface Printer extends RoboxResponseConsumer
{

    public ReadOnlyObjectProperty<Head> headProperty();

    /**
     *
     * @param gcodeToSend
     */
    public void addToGCodeTranscript(String gcodeToSend);

    /*
     * Cancel
     */
    public ReadOnlyBooleanProperty canCancelProperty();

    /*
     * Print
     */
    public ReadOnlyBooleanProperty canPrintProperty();

    /**
     * Purge
     */
    public ReadOnlyBooleanProperty canPurgeHeadProperty();

    public void resetPurgeTemperature();

    /**
     * Calibrate head
     */
    public ReadOnlyBooleanProperty canCalibrateHeadProperty();

    public XAndYStateTransitionManager startCalibrateXAndY() throws PrinterException;

    public NozzleHeightStateTransitionManager startCalibrateNozzleHeight() throws PrinterException;

    public NozzleOpeningStateTransitionManager startCalibrateNozzleOpening() throws PrinterException;

    /*
     * Remove head
     */
    public ReadOnlyBooleanProperty canRemoveHeadProperty();

    public void cancel(TaskResponder responder) throws PrinterException;

    public void gotoNozzlePosition(float position);

    public void closeNozzleFully() throws PrinterException;

    public void ejectFilament(int extruderNumber, TaskResponder responder) throws PrinterException;

    public ObservableList<Extruder> extrudersProperty();

    /**
     * Filament Info change
     * @return 
     */
    public ReadOnlyBooleanProperty canChangeFilamentInfoProperty();

    /**
     *
     * @param filamentDiameterE
     * @param filamentMultiplierE
     * @param filamentDiameterD
     * @param filamentMultiplierD
     * @param feedRateMultiplier
     * @throws RoboxCommsException
     */
    public void transmitSetFilamentInfo(
        double filamentDiameterE, double filamentMultiplierE,
        double filamentDiameterD, double filamentMultiplierD,
        double feedRateMultiplier) throws RoboxCommsException;

    /**
     *
     * @return @throws celtech.printerControl.model.PrinterException
     */
    public AckResponse formatHeadEEPROM() throws PrinterException;

    /**
     *
     * @return @throws celtech.printerControl.model.PrinterException
     */
    public AckResponse formatReelEEPROM(int reelNumber) throws PrinterException;

    /**
     *
     * @return
     */
    public ObservableList<String> gcodeTranscriptProperty();

    /**
     *
     * @return
     */
    public ReadOnlyBooleanProperty canPauseProperty();

    /**
     *
     * @return
     */
    public ReadOnlyBooleanProperty canResumeProperty();

    /**
     *
     * @return
     */
    public int getDataFileSequenceNumber();

    public PrintEngine getPrintEngine();

    public PrinterAncillarySystems getPrinterAncillarySystems();

    public PrinterIdentity getPrinterIdentity();
    
     /*
     * Door open
     */
    public ReadOnlyBooleanProperty canOpenDoorProperty();

    public void goToOpenDoorPosition(TaskResponder responder) throws PrinterException;
    
    public void goToOpenDoorPositionDontWait(TaskResponder responder) throws PrinterException;

    public void goToTargetBedTemperature();

    public void goToTargetNozzleTemperature();

    public void goToZPosition(double position);

    public void goToXYPosition(double xPosition, double yPosition);

    public void homeZ();

    public void levelGantry();

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
     * @return
     */
    public boolean isPrintInitiated();

    public void jogAxis(AxisSpecifier axis, float distance, float feedrate, boolean use_G1) throws PrinterException;

    /**
     * Opens the nozzle to the B1 position.
     * @throws PrinterException 
     */
    public void openNozzleFully() throws PrinterException;
    
    /**
     * Opens the nozzle to the B2 position.
     * @throws PrinterException 
     */
    public void openNozzleFullyExtra() throws PrinterException;

    public void pause() throws PrinterException;

    public void prepareToPurgeHead(TaskResponder responder) throws PrinterException;

    /**
     *
     * @param project
     * @param filament
     * @param printQuality
     * @param settings
     */
    public void printProject(Project project, Filament filament, PrintQualityEnumeration printQuality, SlicerParametersFile settings);

    public ReadOnlyObjectProperty<PrinterStatus> printerStatusProperty();
    public ReadOnlyObjectProperty<MacroType> macroTypeProperty();

    @Override
    public void processRoboxResponse(RoboxRxPacket rxPacket);

    public void purgeHead(TaskResponder responder) throws PrinterException;

    /**
     *
     * @return @throws celtech.printerControl.model.PrinterException
     */
    public FirmwareResponse readFirmwareVersion() throws PrinterException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public HeadEEPROMDataResponse readHeadEEPROM() throws RoboxCommsException;

    /**
     *
     * @return @throws celtech.printerControl.model.PrinterException
     */
    public PrinterIDResponse readPrinterID() throws PrinterException;

    /**
     *
     * @param reelNumber
     * @return @throws RoboxCommsException
     */
    public ReelEEPROMDataResponse readReelEEPROM(int reelNumber) throws RoboxCommsException;

    public ObservableMap<Integer, Reel> reelsProperty();

    public void removeHead(TaskResponder responder) throws PrinterException;

    /**
     *
     * @throws PrinterException
     */
    public void resume() throws PrinterException;

    /*
     * Macros
     */
    public void executeGCodeFile(String fileName) throws PrinterException;

    public void executeMacro(String macroName) throws PrinterException;

    public void executeMacroWithoutPurgeCheck(String macroName) throws PrinterException;

    /**
     *
     * @param nozzleNumber
     * @throws PrinterException
     */
    public void selectNozzle(int nozzleNumber) throws PrinterException;

    /**
     *
     * @param hexDigits
     * @param lastPacket
     * @param appendCRLF
     * @throws DatafileSendNotInitialised
     * @throws RoboxCommsException
     */
    public void sendDataFileChunk(String hexDigits, boolean lastPacket, boolean appendCRLF) throws DatafileSendNotInitialised, RoboxCommsException;

    public void sendRawGCode(String gCode, boolean addToTranscript);

    /**
     *
     * @param colour
     * @throws celtech.printerControl.model.PrinterException
     */
    public void setAmbientLEDColour(Color colour) throws PrinterException;

    public void setAmbientTemperature(int targetTemperature);

    public void setBedFirstLayerTargetTemperature(int targetTemperature);

    public void setBedTargetTemperature(int targetTemperature);

    public void setNozzleFirstLayerTargetTemperature(int targetTemperature);

    public void setNozzleTargetTemperature(int targetTemperature);

    public void setPurgeTemperature(float purgeTemperature);

    /**
     *
     * @param colour
     * @throws celtech.printerControl.model.PrinterException
     */
    public void setReelLEDColour(Color colour) throws PrinterException;

    public void shutdown();

    public void switchAllNozzleHeatersOff();

    public void switchBedHeaterOff();

    public void switchNozzleHeaterOff(int heaterNumber);

    public void switchOffHeadFan() throws PrinterException;

    public void switchOffHeadLEDs() throws PrinterException;

    public void switchOnHeadFan() throws PrinterException;

    /**
     *
     * @throws celtech.printerControl.model.PrinterException
     */
    public void switchOnHeadLEDs() throws PrinterException;

    public void switchToAbsoluteMoveMode();

    public void switchToRelativeMoveMode();

    /**
     *
     * @return @throws RoboxCommsException
     */
    public ListFilesResponse transmitListFiles() throws RoboxCommsException;

    /**
     *
     * @throws RoboxCommsException
     */
    public void transmitPausePrint() throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public AckResponse transmitReportErrors() throws RoboxCommsException;

    /**
     *
     * @throws RoboxCommsException
     */
    public void transmitResetErrors() throws RoboxCommsException;

    /*
     * Higher level controls
     */
    /**
     *
     * @param nozzle0FirstLayerTarget
     * @param nozzle0Target
     * @param nozzle1FirstLayerTarget
     * @param nozzle1Target
     * @param bedFirstLayerTarget
     * @param bedTarget
     * @param ambientTarget
     * @throws RoboxCommsException
     */
    public void transmitSetTemperatures(double nozzle0FirstLayerTarget, double nozzle0Target,
        double nozzle1FirstLayerTarget, double nozzle1Target,
        double bedFirstLayerTarget, double bedTarget, double ambientTarget) throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    public StatusResponse transmitStatusRequest() throws RoboxCommsException;

    /**
     *
     * @param firmwareID
     * @return
     * @throws RoboxCommsException
     */
    public boolean transmitUpdateFirmware(final String firmwareID) throws PrinterException;

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
    public AckResponse transmitWriteHeadEEPROM(String headTypeCode, String headUniqueID, float maximumTemperature, float thermistorBeta, float thermistorTCal, float nozzle1XOffset,
        float nozzle1YOffset,
        float nozzle1ZOffset, float nozzle1BOffset, float nozzle2XOffset, float nozzle2YOffset, float nozzle2ZOffset, float nozzle2BOffset, float lastFilamentTemperature, float hourCounter) throws RoboxCommsException;

    /**
     *
     * @param reelNumber
     * @param filament
     * @return
     * @throws RoboxCommsException
     */
    public AckResponse transmitWriteReelEEPROM(int reelNumber, Filament filament) throws RoboxCommsException;

    /**
     *
     * @param reelNumber
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
     * @param friendlyName
     * @param materialType
     * @param displayColour
     * @throws RoboxCommsException
     */
    public void transmitWriteReelEEPROM(int reelNumber, String filamentID, float reelFirstLayerNozzleTemperature, float reelNozzleTemperature, float reelFirstLayerBedTemperature,
        float reelBedTemperature,
        float reelAmbientTemperature, float reelFilamentDiameter, float reelFilamentMultiplier, float reelFeedRateMultiplier, float reelRemainingFilament, String friendlyName,
        MaterialType materialType, Color displayColour) throws RoboxCommsException;

    public void updatePrinterDisplayColour(Color displayColour) throws PrinterException;

    public void updatePrinterName(String chosenPrinterName) throws PrinterException;

    public void probeBed();

    public float getZDelta() throws PrinterException;

    /**
     *
     * @param headToWrite
     * @throws RoboxCommsException
     */
    public void writeHeadEEPROM(Head headToWrite) throws RoboxCommsException;

    void setPrinterStatus(PrinterStatus printerStatus);

    public ReadOnlyIntegerProperty printJobLineNumberProperty();

    public ReadOnlyStringProperty printJobIDProperty();

    public boolean printJobIDIndicatesPrinting();

    public ReadOnlyObjectProperty pauseStatusProperty();

    public void resetHeadToDefaults() throws PrinterException;

    public void inhibitHeadIntegrityChecks(boolean inhibit);

    public void changeFeedRateMultiplierDuringPrint(double feedRate) throws PrinterException;

    public void registerErrorConsumer(ErrorConsumer errorConsumer, List<FirmwareError> errorsOfInterest);
    
    public void registerErrorConsumerAllErrors(ErrorConsumer errorConsumer);

    public void deregisterErrorConsumer(ErrorConsumer errorConsumer);
}
