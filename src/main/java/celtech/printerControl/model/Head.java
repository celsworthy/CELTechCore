package celtech.printerControl.model;

import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.NozzleData;
import celtech.configuration.fileRepresentation.NozzleHeaterData;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.utils.Math.MathUtils;
import celtech.utils.SystemUtils;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class Head implements Cloneable
{

    private static final Stenographer steno = StenographerFactory.getStenographer(Head.class.getName());
    protected final FloatProperty headXPosition = new SimpleFloatProperty(0);
    protected final FloatProperty headYPosition = new SimpleFloatProperty(0);
    protected final FloatProperty headZPosition = new SimpleFloatProperty(0);

    protected final StringProperty typeCode = new SimpleStringProperty("");
    protected final StringProperty name = new SimpleStringProperty("");
    protected final StringProperty uniqueID = new SimpleStringProperty("");
    protected final FloatProperty headHours = new SimpleFloatProperty(0);

    protected final ArrayList<NozzleHeater> nozzleHeaters = new ArrayList<>();
    protected final ArrayList<Nozzle> nozzles = new ArrayList<>();

    public Head()
    {
    }

    public Head(HeadFile headData)
    {
        updateFromHeadFileData(headData);
    }

    protected static Head createHead(HeadEEPROMDataResponse headResponse)
    {
        Head createdHead = null;

        HeadFile headData = HeadContainer.getHeadByID(headResponse.getTypeCode());
        if (headData != null)
        {
            createdHead = new Head(headData);
            createdHead.updateFromEEPROMData(headResponse);
        } else
        {
            steno.error("Attempt to create head with invalid or absent type code");
        }

        return createdHead;
    }

    private void updateFromHeadFileData(HeadFile headData)
    {
        this.typeCode.set(headData.getTypeCode());
        this.name.set(headData.getName());

        nozzleHeaters.clear();
        headData.getNozzleHeaters().stream().
            map((nozzleHeaterData) -> new NozzleHeater(nozzleHeaterData.getMaximum_temperature_C(),
                                                       nozzleHeaterData.getBeta(),
                                                       nozzleHeaterData.getTcal(),
                                                       0, 0, 0, 0))
            .forEach((newNozzleHeater) ->
                {
                    nozzleHeaters.add(newNozzleHeater);
            });

        nozzles.clear();
        headData.getNozzles().stream().
            map((nozzleData) -> new Nozzle(nozzleData.getDiameter(),
                                           nozzleData.getDefaultXOffset(),
                                           nozzleData.getDefaultYOffset(),
                                           nozzleData.getDefaultZOffset(),
                                           nozzleData.getDefaultBOffset())).
            forEach((newNozzle) ->
                {
                    nozzles.add(newNozzle);
            });
    }

    /**
     */
    private Head(String typeCode,
        String friendlyName,
        String uniqueID,
        float headHours,
        List<NozzleHeater> nozzleHeaters,
        List<Nozzle> nozzles)
    {
        this.typeCode.set(typeCode);
        this.name.set(friendlyName);
        this.uniqueID.set(uniqueID);
        this.headHours.set(headHours);
        this.nozzleHeaters.addAll(nozzleHeaters);
        this.nozzles.addAll(nozzles);
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
     * @return
     */
    public StringProperty nameProperty()
    {
        return name;
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
    public FloatProperty headHoursProperty()
    {
        return headHours;
    }

    public ArrayList<NozzleHeater> getNozzleHeaters()
    {
        return nozzleHeaters;
    }

    public ArrayList<Nozzle> getNozzles()
    {
        return nozzles;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        return name.get();
    }

    /**
     *
     * @return
     */
    @Override
    public Head clone()
    {
        ArrayList<NozzleHeater> newNozzleHeaters = new ArrayList<>();
        ArrayList<Nozzle> newNozzles = new ArrayList<>();

        nozzleHeaters.stream().
            forEach((nozzleHeater) ->
                {
                    newNozzleHeaters.add(nozzleHeater.clone());
            });

        nozzles.stream().
            forEach((nozzle) ->
                {
                    newNozzles.add(nozzle.clone());
            });

        Head clone = new Head(
            typeCode.get(),
            name.get(),
            uniqueID.get(),
            headHours.get(),
            newNozzleHeaters,
            newNozzles
        );

        return clone;
    }

    public final void updateFromEEPROMData(HeadEEPROMDataResponse eepromData)
    {
        //TODO update for multiple nozzles/heater
        // This only supports the initial RBX01-SM head since this is the only data from the printer at the moment...

        typeCode.set(eepromData.getTypeCode());
        uniqueID.set(eepromData.getUniqueID());
        headHours.set(eepromData.getHeadHours());

        if (nozzleHeaters.size() > 0)
        {
            nozzleHeaters.get(0).beta.set(eepromData.getBeta());
            nozzleHeaters.get(0).tcal.set(eepromData.getTCal());
            nozzleHeaters.get(0).lastFilamentTemperature.set(eepromData.getLastFilamentTemperature());
            nozzleHeaters.get(0).maximumTemperature.set(eepromData.getMaximumTemperature());
        }

        if (nozzles.size() > 0)
        {
            nozzles.get(0).xOffset.set(eepromData.getNozzle1XOffset());
            nozzles.get(0).yOffset.set(eepromData.getNozzle1YOffset());
            nozzles.get(0).zOffset.set(eepromData.getNozzle1ZOffset());
            nozzles.get(0).bOffset.set(eepromData.getNozzle1BOffset());
        }

        if (nozzles.size() == 2)
        {
            nozzles.get(1).xOffset.set(eepromData.getNozzle2XOffset());
            nozzles.get(1).yOffset.set(eepromData.getNozzle2YOffset());
            nozzles.get(1).zOffset.set(eepromData.getNozzle2ZOffset());
            nozzles.get(1).bOffset.set(eepromData.getNozzle2BOffset());
        }
    }

    protected HeadRepairResult bringDataInBounds()
    {
        float epsilon = 1e-5f;

        HeadRepairResult result = HeadRepairResult.NO_REPAIR_NECESSARY;

        HeadFile referenceHeadData = HeadContainer.getHeadByID(typeCode.get());
        if (referenceHeadData != null)
        {
            // Iterate through the nozzle heaters and check for differences
            for (int i = 0; i < getNozzleHeaters().size(); i++)
            {
                NozzleHeater nozzleHeater = getNozzleHeaters().get(i);
                NozzleHeaterData nozzleHeaterData = referenceHeadData.getNozzleHeaters().get(i);

                if (MathUtils.compareDouble(nozzleHeater.maximumTemperatureProperty().get(), nozzleHeaterData.getMaximum_temperature_C(), epsilon) != MathUtils.EQUAL)
                {
                    nozzleHeater.maximumTemperature.set(nozzleHeaterData.getMaximum_temperature_C());
                    result = HeadRepairResult.REPAIRED_WRITE_ONLY;
                }

                if (Math.abs(nozzleHeater.tCalProperty().get() - nozzleHeaterData.getTcal()) > epsilon)
                {
                    nozzleHeater.tcal.set(nozzleHeaterData.getTcal());
                    result = HeadRepairResult.REPAIRED_WRITE_ONLY;
                }

                if (Math.abs(nozzleHeater.betaProperty().get() - nozzleHeaterData.getBeta()) > epsilon)
                {
                    nozzleHeater.beta.set(nozzleHeaterData.getBeta());
                    result = HeadRepairResult.REPAIRED_WRITE_ONLY;
                }
            }

            // Now for the nozzles...
            for (int i = 0; i < getNozzles().size(); i++)
            {
                Nozzle nozzle = getNozzles().get(i);
                NozzleData nozzleData = referenceHeadData.getNozzles().get(i);

                if (nozzle.xOffsetProperty().get() < nozzleData.getMinXOffset() || nozzle.xOffsetProperty().get() > nozzleData.getMaxXOffset())
                {
                    nozzle.xOffset.set(nozzleData.getDefaultXOffset());
                    result = HeadRepairResult.REPAIRED_WRITE_AND_RECALIBRATE;
                }

                if (nozzle.yOffsetProperty().get() < nozzleData.getMinYOffset() || nozzle.yOffsetProperty().get() > nozzleData.getMaxYOffset())
                {
                    nozzle.yOffset.set(nozzleData.getDefaultYOffset());
                    result = HeadRepairResult.REPAIRED_WRITE_AND_RECALIBRATE;
                }

                if (nozzle.zOffsetProperty().get() < nozzleData.getMinZOffset() || nozzle.zOffsetProperty().get() > nozzleData.getMaxZOffset())
                {
                    nozzle.zOffset.set(nozzleData.getDefaultZOffset());
                    result = HeadRepairResult.REPAIRED_WRITE_AND_RECALIBRATE;
                }

                if (nozzle.bOffsetProperty().get() < nozzleData.getMinBOffset() || nozzle.bOffsetProperty().get() > nozzleData.getMaxBOffset())
                {
                    nozzle.bOffset.set(nozzleData.getDefaultBOffset());
                    result = HeadRepairResult.REPAIRED_WRITE_AND_RECALIBRATE;
                }
            }

            steno.info("Head data bounds check - result is " + result.name());
        } else
        {
            steno.warning("Head bounds check requested but reference data could not be obtained.");
        }

        return result;
    }

    protected void resetToDefaults()
    {
        HeadFile referenceHeadData = HeadContainer.getHeadByID(typeCode.get());
        if (referenceHeadData != null)
        {
            updateFromHeadFileData(referenceHeadData);
            steno.info("Reset head to defaults with data set - " + referenceHeadData.getTypeCode());
        } else
        {
            steno.warning("Attempt to reset head to defaults failed - reference data cannot be derived");
        }
    }

    protected static boolean isTypeCodeValid(String typeCode)
    {
        boolean typeCodeIsValid = false;

        if (typeCode != null
            && typeCode.matches("RBX-.*"))
        {
            typeCodeIsValid = true;
        }

        return typeCodeIsValid;
    }

    protected static boolean isTypeCodeInDatabase(String typeCode)
    {
        boolean typeCodeIsInDatabase = false;

        if (typeCode != null
            && HeadContainer.getHeadByID(typeCode) != null)
        {
            typeCodeIsInDatabase = true;
        }

        return typeCodeIsInDatabase;
    }

    protected void allocateRandomID()
    {
        String idToCreate = typeCode + SystemUtils.generate16DigitID().substring(typeCode.get().length());
        uniqueID.set(idToCreate);
    }
}
