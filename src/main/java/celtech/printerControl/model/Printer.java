package celtech.printerControl.model;

import celtech.appManager.Project;
import celtech.configuration.Filament;
import celtech.configuration.MaterialType;
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
import celtech.printerControl.comms.events.RoboxResponseConsumer;
import celtech.services.printing.DatafileSendAlreadyInProgress;
import celtech.services.printing.DatafileSendNotInitialised;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import celtech.utils.AxisSpecifier;
import celtech.utils.tasks.Cancellable;
import celtech.utils.tasks.TaskResponder;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

/**
 *
 * @author Ian
 */
public interface Printer extends RoboxResponseConsumer
{

    /**
     *
     * @param gcodeToSend
     */
    void addToGCodeTranscript(String gcodeToSend);

    /*
     * Cancel
     */
    ReadOnlyBooleanProperty canCancelProperty();

    /*
     * Print
     */
    ReadOnlyBooleanProperty canPrintProperty();

    ReadOnlyBooleanProperty canPurgeHeadProperty();

    /*
     * Remove head
     */
    ReadOnlyBooleanProperty canRemoveHeadProperty();

    void cancel(TaskResponder responder) throws PrinterException;

    void gotoNozzlePosition(float position);

    void closeNozzleFully() throws PrinterException;

    boolean doAbortActivity(Cancellable cancellable);

    void ejectFilament(int extruderNumber, TaskResponder responder) throws PrinterException;

    ObservableList<Extruder> extrudersProperty();

    void forceHeadReset();

    /**
     *
     * @return @throws celtech.printerControl.model.PrinterException
     */
    AckResponse formatHeadEEPROM() throws PrinterException;

    /**
     *
     * @return @throws celtech.printerControl.model.PrinterException
     */
    AckResponse formatReelEEPROM() throws PrinterException;

    /**
     *
     * @return
     */
    ObservableList<String> gcodeTranscriptProperty();

    /**
     *
     * @return
     */
    ReadOnlyBooleanProperty getCanPauseProperty();

    /**
     *
     * @return
     */
    ReadOnlyBooleanProperty getCanResumeProperty();

    /**
     *
     * @return
     */
    int getDataFileSequenceNumber();

    PrintEngine getPrintEngine();

    PrinterAncillarySystems getPrinterAncillarySystems();

    PrinterIdentity getPrinterIdentity();

    void goToOpenDoorPosition(TaskResponder responder) throws PrinterException;

    void goToTargetBedTemperature();

    void goToTargetNozzleTemperature();

    void goToZPosition(double position);

    void hardResetHead();

    ReadOnlyObjectProperty<Head> headProperty();

    void homeZ();

    /**
     *
     * @param fileID
     * @return
     * @throws DatafileSendAlreadyInProgress
     * @throws RoboxCommsException
     */
    boolean initialiseDataFileSend(String fileID) throws DatafileSendAlreadyInProgress, RoboxCommsException;

    /**
     *
     * @param jobUUID
     * @throws RoboxCommsException
     */
    void initiatePrint(String jobUUID) throws RoboxCommsException;

    /**
     *
     * @return
     */
    boolean isPrintInitiated();

    void jogAxis(AxisSpecifier axis, float distance, float feedrate, boolean use_G1) throws PrinterException;

    void openNozzleFully() throws PrinterException;

    void pause() throws PrinterException;

    void prepareToPurgeHead(TaskResponder responder) throws PrinterException;

    /**
     *
     * @param project
     * @param filament
     * @param printQuality
     * @param settings
     */
    void printProject(Project project, Filament filament, PrintQualityEnumeration printQuality, RoboxProfile settings);

    ReadOnlyObjectProperty<PrinterStatus> printerStatusProperty();

    void processRoboxResponse(RoboxRxPacket rxPacket);

    void purgeHead(TaskResponder responder) throws PrinterException;

    /**
     *
     * @return @throws celtech.printerControl.model.PrinterException
     */
    FirmwareResponse readFirmwareVersion() throws PrinterException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    HeadEEPROMDataResponse readHeadEEPROM() throws RoboxCommsException;

    /**
     *
     * @return @throws celtech.printerControl.model.PrinterException
     */
    PrinterIDResponse readPrinterID() throws PrinterException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    ReelEEPROMDataResponse readReelEEPROM() throws RoboxCommsException;

    ObservableList<Reel> reelsProperty();

    void removeHead(TaskResponder responder) throws PrinterException;

    void repairHeadIfNecessary();

    /**
     *
     * @throws PrinterException
     */
    void resume() throws PrinterException;

    /*
     * Macros
     */
    void runMacro(String macroName) throws PrinterException;

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
    void runMacroWithoutPurgeCheck(String macroName) throws PrinterException;

    /**
     *
     * @param nozzleNumber
     * @throws PrinterException
     */
    void selectNozzle(int nozzleNumber) throws PrinterException;

    /**
     *
     * @param hexDigits
     * @param lastPacket
     * @param appendCRLF
     * @throws DatafileSendNotInitialised
     * @throws RoboxCommsException
     */
    void sendDataFileChunk(String hexDigits, boolean lastPacket, boolean appendCRLF) throws DatafileSendNotInitialised, RoboxCommsException;

    void sendRawGCode(String gCode, boolean addToTranscript);

    /**
     *
     * @param colour
     * @throws celtech.printerControl.model.PrinterException
     */
    void setAmbientLEDColour(Color colour) throws PrinterException;

    void setAmbientTemperature(int targetTemperature);

    void setBedFirstLayerTargetTemperature(int targetTemperature);

    void setBedTargetTemperature(int targetTemperature);

    void setNozzleFirstLayerTargetTemperature(int targetTemperature);

    void setNozzleTargetTemperature(int targetTemperature);

    void setPurgeTemperature(float purgeTemperature);

    /**
     *
     * @param colour
     * @throws celtech.printerControl.model.PrinterException
     */
    void setReelLEDColour(Color colour) throws PrinterException;

    void shutdown();

    void switchAllNozzleHeatersOff();

    void switchBedHeaterOff();

    void switchNozzleHeaterOff(int heaterNumber);

    void switchOffHeadFan() throws PrinterException;

    void switchOffHeadLEDs() throws PrinterException;

    void switchOnHeadFan() throws PrinterException;

    /**
     *
     * @throws celtech.printerControl.model.PrinterException
     */
    void switchOnHeadLEDs() throws PrinterException;

    void switchToAbsoluteMoveMode();

    void switchToRelativeMoveMode();

    /**
     *
     * @return @throws RoboxCommsException
     */
    ListFilesResponse transmitListFiles() throws RoboxCommsException;

    /**
     *
     * @throws RoboxCommsException
     */
    void transmitPausePrint() throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    AckResponse transmitReportErrors() throws RoboxCommsException;

    /**
     *
     * @throws RoboxCommsException
     */
    void transmitResetErrors() throws RoboxCommsException;

    /**
     *
     * @param filamentDiameter
     * @param filamentMultiplier
     * @param feedRateMultiplier
     * @throws RoboxCommsException
     */
    void transmitSetFilamentInfo(double filamentDiameter, double filamentMultiplier, double feedRateMultiplier) throws RoboxCommsException;

    /*
     * Higher level controls
     */
    /**
     *
     * @param nozzleFirstLayerTarget
     * @param nozzleTarget
     * @param bedFirstLayerTarget
     * @param bedTarget
     * @param ambientTarget
     * @throws RoboxCommsException
     */
    void transmitSetTemperatures(double nozzleFirstLayerTarget, double nozzleTarget, double bedFirstLayerTarget, double bedTarget, double ambientTarget) throws RoboxCommsException;

    /**
     *
     * @return @throws RoboxCommsException
     */
    StatusResponse transmitStatusRequest() throws RoboxCommsException;

    /**
     *
     * @param firmwareID
     * @return
     * @throws RoboxCommsException
     */
    boolean transmitUpdateFirmware(final String firmwareID) throws RoboxCommsException;

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
    AckResponse transmitWriteHeadEEPROM(String headTypeCode, String headUniqueID, float maximumTemperature, float thermistorBeta, float thermistorTCal, float nozzle1XOffset, float nozzle1YOffset,
        float nozzle1ZOffset, float nozzle1BOffset, float nozzle2XOffset, float nozzle2YOffset, float nozzle2ZOffset, float nozzle2BOffset, float lastFilamentTemperature, float hourCounter) throws RoboxCommsException;

    /**
     *
     * @param filament
     * @return
     * @throws RoboxCommsException
     */
    AckResponse transmitWriteReelEEPROM(Filament filament) throws RoboxCommsException;

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
    void transmitWriteReelEEPROM(String filamentID, float reelFirstLayerNozzleTemperature, float reelNozzleTemperature, float reelFirstLayerBedTemperature, float reelBedTemperature,
        float reelAmbientTemperature, float reelFilamentDiameter, float reelFilamentMultiplier, float reelFeedRateMultiplier, float reelRemainingFilament, String friendlyName,
        MaterialType materialType, Color displayColour) throws RoboxCommsException;

    void updatePrinterDisplayColour(Color displayColour) throws PrinterException;

    void updatePrinterName(String chosenPrinterName) throws PrinterException;

    /**
     *
     * @param headToWrite
     * @throws RoboxCommsException
     */
    void writeHeadEEPROM(Head headToWrite) throws RoboxCommsException;
    
}
