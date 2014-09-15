/*
 * To change this license header.set(choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.configuration;

import celtech.appManager.Notifier;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.utilityPanels.MaintenancePanelController;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.utils.SystemUtils;
import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;

/**
 *
 * @author ianhudson
 */
public class Head implements Cloneable
{

    private static final Stenographer steno = StenographerFactory.getStenographer(Head.class.getName());
    private final StringProperty typeCode = new SimpleStringProperty("");
    private final StringProperty friendlyName = new SimpleStringProperty("");
    private final StringProperty uniqueID = new SimpleStringProperty("");
    private final FloatProperty maximumTemperature = new SimpleFloatProperty(0);
    private final FloatProperty beta = new SimpleFloatProperty(0);
    private final FloatProperty tcal = new SimpleFloatProperty(0);
    private final FloatProperty nozzle1_X_offset = new SimpleFloatProperty(0);
    private final FloatProperty nozzle1_Y_offset = new SimpleFloatProperty(0);
    private final FloatProperty nozzle1_Z_offset = new SimpleFloatProperty(0);
    private final FloatProperty nozzle1_Z_overrun = new SimpleFloatProperty(0);
    private final FloatProperty nozzle1_B_offset = new SimpleFloatProperty(0);
    private final FloatProperty nozzle2_X_offset = new SimpleFloatProperty(0);
    private final FloatProperty nozzle2_Y_offset = new SimpleFloatProperty(0);
    private final FloatProperty nozzle2_Z_offset = new SimpleFloatProperty(0);
    private final FloatProperty nozzle2_Z_overrun = new SimpleFloatProperty(0);
    private final FloatProperty nozzle2_B_offset = new SimpleFloatProperty(0);
    private final FloatProperty lastFilamentTemperature = new SimpleFloatProperty(0);
    private final FloatProperty headHours = new SimpleFloatProperty(0);

    private static final float normalX1OffsetMin = 6.8f;
    private static final float normalX1OffsetMax = 7.5f;

    private static final float normalX2OffsetMin = -7.5f;
    private static final float normalX2OffsetMax = -6.8f;

    private static final float normalYOffsetMin = -0.3f;
    private static final float normalYOffsetMax = 0.3f;

    public static final float normalZ1OffsetMin = -1.2f;
    public static final float normalZ1OffsetMax = 1.2f;
    public static final float normalZ1OverrunMin = 0f;
    public static final float normalZ1OverrunMax = 1.2f;

    public static final float normalZ2OffsetMin = -1.2f;
    public static final float normalZ2OffsetMax = 1.2f;
    public static final float normalZ2OverrunMin = 0f;
    public static final float normalZ2OverrunMax = 1.2f;

    private static final float normalB1OffsetMin = 0.7f;
    private static final float normalB1OffsetMax = 2f;

    private static final float normalB2OffsetMin = -2f;
    private static final float normalB2OffsetMax = -0.7f;

    private static final Dialogs.CommandLink okCalibrate = new Dialogs.CommandLink(DisplayManager.getLanguageBundle().getString("dialogs.headUpdateCalibrationYes"), null);
    private static final Dialogs.CommandLink dontCalibrate = new Dialogs.CommandLink(DisplayManager.getLanguageBundle().getString("dialogs.headUpdateCalibrationNo"), null);

    private static final float epsilon = 0.0001f;

    /**
     *
     * @param typeCode
     * @param friendlyName
     * @param maximumTemperature
     * @param beta
     * @param tcal
     * @param nozzle1_X_offset
     * @param nozzle1_Y_offset
     * @param nozzle1_Z_offset
     * @param nozzle1_B_offset
     * @param nozzle2_X_offset
     * @param nozzle2_Y_offset
     * @param nozzle2_Z_offset
     * @param nozzle2_B_offset
     */
    public Head(String typeCode, String friendlyName,
        float maximumTemperature,
        float beta,
        float tcal,
        float nozzle1_X_offset,
        float nozzle1_Y_offset,
        float nozzle1_Z_offset,
        float nozzle1_B_offset,
        float nozzle2_X_offset,
        float nozzle2_Y_offset,
        float nozzle2_Z_offset,
        float nozzle2_B_offset)
    {
        this.typeCode.set(typeCode);
        this.friendlyName.set(friendlyName);
        this.maximumTemperature.set(maximumTemperature);
        this.beta.set(beta);
        this.tcal.set(tcal);
        this.nozzle1_X_offset.set(nozzle1_X_offset);
        this.nozzle1_Y_offset.set(nozzle1_Y_offset);
        this.nozzle1_Z_offset.set(nozzle1_Z_offset);
        this.nozzle1_B_offset.set(nozzle1_B_offset);
        this.nozzle2_X_offset.set(nozzle2_X_offset);
        this.nozzle2_Y_offset.set(nozzle2_Y_offset);
        this.nozzle2_Z_offset.set(nozzle2_Z_offset);
        this.nozzle2_B_offset.set(nozzle2_B_offset);
        deriveZOverrunFromOffsets();
    }

    /**
     *
     * @param response
     */
    public Head(HeadEEPROMDataResponse response)
    {
        this.typeCode.set(response.getTypeCode());
        this.friendlyName.set("");
        this.maximumTemperature.set(response.getMaximumTemperature());
        this.beta.set(response.getBeta());
        this.tcal.set(response.getTCal());
        this.nozzle1_X_offset.set(response.getNozzle1XOffset());
        this.nozzle1_Y_offset.set(response.getNozzle1YOffset());
        this.nozzle1_Z_offset.set(response.getNozzle1ZOffset());
        this.nozzle1_B_offset.set(response.getNozzle1BOffset());
        this.nozzle2_X_offset.set(response.getNozzle2XOffset());
        this.nozzle2_Y_offset.set(response.getNozzle2YOffset());
        this.nozzle2_Z_offset.set(response.getNozzle2ZOffset());
        this.nozzle2_B_offset.set(response.getNozzle2BOffset());
        this.uniqueID.set(response.getUniqueID());
        deriveZOverrunFromOffsets();
    }

    /**
     *
     * @param value
     */
    public void setTypeCode(String value)
    {
        typeCode.set(value);
    }

    /**
     *
     * @return
     */
    public String getTypeCode()
    {
        return typeCode.get();
    }

    /**
     *
     * @return
     */
    public StringProperty typeCodeProperty()
    {
        return typeCode;
    }

    /**
     *
     * @param value
     */
    public void setFriendlyName(String value)
    {
        friendlyName.set(value);
    }

    /**
     *
     * @return
     */
    public String getFriendlyName()
    {
        return friendlyName.get();
    }

    /**
     *
     * @return
     */
    public StringProperty friendlyNameProperty()
    {
        return friendlyName;
    }

    /**
     *
     * @param value
     */
    public void setUniqueID(String value)
    {
        uniqueID.set(value);
    }

    /**
     *
     * @return
     */
    public String getUniqueID()
    {
        return uniqueID.get();
    }

    /**
     *
     * @return
     */
    public StringProperty uniqueIDProperty()
    {
        return uniqueID;
    }

    /**
     *
     * @return
     */
    public FloatProperty getMaximumTemperatureProperty()
    {
        return maximumTemperature;
    }

    /**
     *
     * @param value
     */
    public void setMaximumTemperature(float value)
    {
        maximumTemperature.set(value);
    }

    /**
     *
     * @return
     */
    public float getMaximumTemperature()
    {
        return maximumTemperature.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getBetaProperty()
    {
        return beta;
    }

    /**
     *
     * @param value
     */
    public void setBeta(float value)
    {
        beta.set(value);
    }

    /**
     *
     * @return
     */
    public float getBeta()
    {
        return beta.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getTcalProperty()
    {
        return tcal;
    }

    /**
     *
     * @param value
     */
    public void setTcal(float value)
    {
        tcal.set(value);
    }

    /**
     *
     * @return
     */
    public float getTCal()
    {
        return tcal.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle1XOffsetProperty()
    {
        return nozzle1_X_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle1_X_offset(float value)
    {
        nozzle1_X_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle1XOffset()
    {
        return nozzle1_X_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle1_Y_offsetProperty()
    {
        return nozzle1_Y_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle1_Y_offset(float value)
    {
        nozzle1_Y_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle1YOffset()
    {
        return nozzle1_Y_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle1_Z_offsetProperty()
    {
        return nozzle1_Z_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle1_Z_offset(float value)
    {
        nozzle1_Z_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle1ZOffset()
    {
        return nozzle1_Z_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle1_Z_overrunProperty()
    {
        return nozzle1_Z_overrun;
    }

    /**
     *
     * @param value
     */
    public void setNozzle1_Z_overrun(float value)
    {
        nozzle1_Z_overrun.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle1ZOverrun()
    {
        return nozzle1_Z_overrun.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle1_B_offsetProperty()
    {
        return nozzle1_B_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle1_B_offset(float value)
    {
        nozzle1_B_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle1BOffset()
    {
        return nozzle1_B_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle2_X_offsetProperty()
    {
        return nozzle2_X_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle2_X_offset(float value)
    {
        nozzle2_X_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle2XOffset()
    {
        return nozzle2_X_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle2_Y_offsetProperty()
    {
        return nozzle2_Y_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle2_Y_offset(float value)
    {
        nozzle2_Y_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle2YOffset()
    {
        return nozzle2_Y_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle2_Z_offsetProperty()
    {
        return nozzle2_Z_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle2_Z_offset(float value)
    {
        nozzle2_Z_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle2ZOffset()
    {
        return nozzle2_Z_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle2_Z_overrunProperty()
    {
        return nozzle2_Z_overrun;
    }

    /**
     *
     * @param value
     */
    public void setNozzle2_Z_overrun(float value)
    {
        nozzle2_Z_overrun.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle2ZOverrun()
    {
        return nozzle2_Z_overrun.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getNozzle2_B_offsetProperty()
    {
        return nozzle2_B_offset;
    }

    /**
     *
     * @param value
     */
    public void setNozzle2_B_offset(float value)
    {
        nozzle2_B_offset.set(value);
    }

    /**
     *
     * @return
     */
    public float getNozzle2BOffset()
    {
        return nozzle2_B_offset.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getHeadHoursProperty()
    {
        return headHours;
    }

    /**
     *
     * @param value
     */
    public void setHeadHours(float value)
    {
        headHours.set(value);
    }

    /**
     *
     * @return
     */
    public float getHeadHours()
    {
        return headHours.get();
    }

    /**
     *
     * @param value
     */
    public void setLastFilamentTemperature(float value)
    {
        lastFilamentTemperature.set(value);
    }

    /**
     *
     * @return
     */
    public float getLastFilamentTemperature()
    {
        return lastFilamentTemperature.get();
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        return friendlyName.get();
    }

    /**
     *
     * @return
     */
    @Override
    public Head clone()
    {
        Head clone = new Head(
            this.getTypeCode(),
            this.getFriendlyName(),
            this.getMaximumTemperature(),
            this.getBeta(),
            this.getTCal(),
            this.getNozzle1XOffset(),
            this.getNozzle1YOffset(),
            this.getNozzle1ZOffset(),
            this.getNozzle1BOffset(),
            this.getNozzle2XOffset(),
            this.getNozzle2YOffset(),
            this.getNozzle2ZOffset(),
            this.getNozzle2BOffset()
        );

        clone.deriveZOverrunFromOffsets();

        return clone;
    }

    /**
     *
     * @param printer
     */
    public static void hardResetHead(Printer printer)
    {
        if (printer.getHeadEEPROMStatus() == EEPROMState.NOT_PROGRAMMED)
        {
            try
            {
                printer.transmitFormatHeadEEPROM();
            } catch (RoboxCommsException ex)
            {
                steno.error("Error formatting head");
            }
        }

        try
        {
            HeadEEPROMDataResponse response = printer.transmitReadHeadEEPROM();

            if (response != null)
            {
                String receivedTypeCode = response.getTypeCode();

                Head referenceHead = null;
                if (receivedTypeCode != null)
                {
                    referenceHead = HeadContainer.getHeadByID(response.getTypeCode());
                }
                
                if (referenceHead != null)
                {
                    Head headToWrite = referenceHead.clone();
                    headToWrite.setUniqueID(response.getUniqueID());
                    headToWrite.setHeadHours(response.getHeadHours());
                    headToWrite.setLastFilamentTemperature(response.getLastFilamentTemperature());

                    printer.transmitWriteHeadEEPROM(headToWrite);
                    printer.transmitReadHeadEEPROM();
                    steno.info("Updated head data at user request for " + receivedTypeCode);
                    showCalibrationDialogue();
                } else
                {
                    Head headToWrite = HeadContainer.getCompleteHeadList().get(0).clone();
                    String typeCode = headToWrite.getTypeCode();
                    String idToCreate = typeCode + SystemUtils.generate16DigitID().substring(typeCode.length());
                    headToWrite.setUniqueID(idToCreate);
                    headToWrite.setLastFilamentTemperature(10);

                    printer.transmitWriteHeadEEPROM(headToWrite);
                    printer.transmitReadHeadEEPROM();
                    steno.info("Updated head data at user request - type code could not be determined");
                    showCalibrationDialogue();
                }
            } else
            {
                steno.warning("Request to hard reset head failed");
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Error during hard reset of head");
        }
    }

    /**
     *
     * @param printer
     */
    public static void repairHeadIfNecessary(Printer printer)
    {
        try
        {
            HeadEEPROMDataResponse response = printer.transmitReadHeadEEPROM();
            if (ApplicationConfiguration.isAutoRepairHeads())
            {
                // Check to see if the maximum temperature of the head matches our view
                // If not, change the max value and prompt to calibrate
                if (response != null)
                {
                    String receivedTypeCode = response.getTypeCode();
                    boolean calibrationRequired = false;

                    if (receivedTypeCode == null || receivedTypeCode.equals("") || receivedTypeCode.equals("null"))
                    {
                        // Force the defaults - we only have one head at the moment so pick the first...
                        Head headToWrite = HeadContainer.getCompleteHeadList().get(0).clone();
                        String typeCode = headToWrite.getTypeCode();
                        String idToCreate = typeCode + SystemUtils.generate16DigitID().substring(typeCode.length());
                        headToWrite.setUniqueID(idToCreate);
                        headToWrite.setLastFilamentTemperature(10);
                        printer.transmitWriteHeadEEPROM(headToWrite);
                        // Calibration should follow
                        calibrationRequired = true;
                    } else
                    {
                        Head referenceHead = HeadContainer.getHeadByID(response.getTypeCode());
                        if (referenceHead != null)
                        {
                            boolean needToWriteHeadData = false;

                            Head headToWrite = new Head(response);
                            headToWrite.setFriendlyName(referenceHead.getFriendlyName());

                            if (Math.abs(response.getMaximumTemperature() - referenceHead.getMaximumTemperature()) > epsilon)
                            {
                                headToWrite.setMaximumTemperature(referenceHead.getMaximumTemperature());
                                needToWriteHeadData = true;
                            }

                            if (Math.abs(response.getTCal() - referenceHead.getTCal()) > epsilon)
                            {
                                headToWrite.setTcal(referenceHead.getTCal());
                                needToWriteHeadData = true;
                            }

                            if (Math.abs(response.getBeta() - referenceHead.getBeta()) > epsilon)
                            {
                                headToWrite.setBeta(referenceHead.getBeta());
                                needToWriteHeadData = true;
                            }

                            if (response.getNozzle1XOffset() < normalX1OffsetMin || response.getNozzle1XOffset() > normalX1OffsetMax)
                            {
                                headToWrite.setNozzle1_X_offset(referenceHead.getNozzle1XOffset());
                                needToWriteHeadData = true;
                                calibrationRequired = true;
                            }

                            if (response.getNozzle1YOffset() < normalYOffsetMin || response.getNozzle1YOffset() > normalYOffsetMax)
                            {
                                headToWrite.setNozzle1_Y_offset(referenceHead.getNozzle1YOffset());
                                needToWriteHeadData = true;
                                calibrationRequired = true;
                            }

                            if (response.getNozzle1ZOffset() < normalZ1OffsetMin || response.getNozzle1ZOffset() > normalZ1OffsetMax)
                            {
                                headToWrite.setNozzle1_Z_offset(referenceHead.getNozzle1ZOffset());
                                needToWriteHeadData = true;
                                calibrationRequired = true;
                            }

                            if (response.getNozzle1BOffset() < normalB1OffsetMin || response.getNozzle1BOffset() > normalB1OffsetMax)
                            {
                                headToWrite.setNozzle1_B_offset(referenceHead.getNozzle1BOffset());
                                needToWriteHeadData = true;
                                calibrationRequired = true;
                            }

                            if (response.getNozzle2XOffset() < normalX2OffsetMin || response.getNozzle2XOffset() > normalX2OffsetMax)
                            {
                                headToWrite.setNozzle2_X_offset(referenceHead.getNozzle2XOffset());
                                needToWriteHeadData = true;
                                calibrationRequired = true;
                            }

                            if (response.getNozzle2YOffset() < normalYOffsetMin || response.getNozzle2YOffset() > normalYOffsetMax)
                            {
                                headToWrite.setNozzle2_Y_offset(referenceHead.getNozzle2YOffset());
                                needToWriteHeadData = true;
                                calibrationRequired = true;
                            }

                            if (response.getNozzle2ZOffset() < normalZ2OffsetMin || response.getNozzle2ZOffset() > normalZ2OffsetMax)
                            {
                                headToWrite.setNozzle2_Z_offset(referenceHead.getNozzle2ZOffset());
                                needToWriteHeadData = true;
                                calibrationRequired = true;
                            }

                            if (response.getNozzle2BOffset() < normalB2OffsetMin || response.getNozzle2BOffset() > normalB2OffsetMax)
                            {
                                headToWrite.setNozzle2_B_offset(referenceHead.getNozzle2BOffset());
                                needToWriteHeadData = true;
                                calibrationRequired = true;
                            }

                            if (needToWriteHeadData)
                            {
                                printer.transmitWriteHeadEEPROM(headToWrite);
                                printer.transmitReadHeadEEPROM();
                                steno.info("Automatically updated head data for " + receivedTypeCode);
                            }

                            if (needToWriteHeadData && !calibrationRequired)
                            {
                                Platform.runLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        Notifier.showInformationNotification(DisplayManager.getLanguageBundle().getString("notification.headSettingsUpdatedTitle"),
                                                                             DisplayManager.getLanguageBundle().getString("notification.noActionRequired"));
                                    }
                                });
                            }
                        }
                    }

                    if (calibrationRequired)
                    {
                        showCalibrationDialogue();
                    }
                }
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Error from triggered read of Head EEPROM");
        }
    }

    private static void showCalibrationDialogue()
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                Action calibrationResponse = Dialogs.create().title(DisplayManager.getLanguageBundle().getString("dialogs.headUpdateCalibrationRequiredTitle"))
                    .message(DisplayManager.getLanguageBundle().getString("dialogs.headUpdateCalibrationRequiredInstruction"))
                    .masthead(null)
                    .showCommandLinks(okCalibrate, okCalibrate, dontCalibrate);

                if (calibrationResponse == okCalibrate)
                {
                    MaintenancePanelController.calibrateBAction();
                    MaintenancePanelController.calibrateZOffsetAction();
                }
            }
        });
    }

    public void deriveZOverrunFromOffsets()
    {
        float nozzle1Offset = getNozzle1ZOffset();
        float nozzle2Offset = getNozzle2ZOffset();

        float delta = nozzle2Offset - nozzle1Offset;
        float halfdelta = delta / 2;

        float nozzle1Overrun = -(nozzle1Offset + halfdelta);
        float nozzle2Overrun = nozzle1Overrun + delta;

        nozzle1_Z_overrun.set(nozzle1Overrun);
        nozzle2_Z_overrun.set(nozzle2Overrun);
    }

    public void deriveZOffsetsFromOverrun()
    {
        float nozzle1OverrunValue = nozzle1_Z_overrun.get();
        float nozzle2OverrunValue = nozzle2_Z_overrun.get();
        float offsetAverage = -nozzle1OverrunValue;
        float delta = (nozzle2OverrunValue - nozzle1OverrunValue) / 2;
        float nozzle1Offset = offsetAverage - delta;
        float nozzle2Offset = offsetAverage + delta;

        nozzle1_Z_offset.set(nozzle1Offset);
        nozzle2_Z_offset.set(nozzle2Offset);
    }
}
