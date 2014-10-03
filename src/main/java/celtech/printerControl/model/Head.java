package celtech.printerControl.model;

import celtech.configuration.EEPROMState;
import celtech.configuration.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.NozzleData;
import celtech.configuration.fileRepresentation.NozzleHeaterData;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.utils.SystemUtils;
import java.util.ArrayList;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author ianhudson
 */
public class Head implements Cloneable
{

    protected final ObjectProperty<EEPROMState> headEEPROMStatusProperty = new SimpleObjectProperty<>(
        EEPROMState.NOT_PRESENT);

    protected final FloatProperty headXPosition = new SimpleFloatProperty(0);
    protected final FloatProperty headYPosition = new SimpleFloatProperty(0);
    protected final FloatProperty headZPosition = new SimpleFloatProperty(0);

    protected final StringProperty typeCode = new SimpleStringProperty("");
    protected final StringProperty name = new SimpleStringProperty("");
    protected final StringProperty uniqueID = new SimpleStringProperty("");
    protected final FloatProperty headHours = new SimpleFloatProperty(0);

    protected final ArrayList<NozzleHeater> nozzleHeaters = new ArrayList<>();
    protected final ArrayList<Nozzle> nozzles = new ArrayList<>();

    public Head(HeadFile headData)
    {
        updateFromHeadFileData(headData);
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
                                                       0, 0, 0, 0)).
            forEach((newNozzleHeater) ->
                {
                    nozzleHeaters.add(newNozzleHeater);
            });

        nozzles.clear();
        headData.getNozzles().stream().
            map((nozzleData) -> new Nozzle(nozzleData.getDiameter(), nozzleData.getDefaultXOffset(), nozzleData.getDefaultYOffset(), nozzleData.getDefaultZOffset(), nozzleData.getDefaultBOffset())).
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
        ArrayList<NozzleHeater> nozzleHeaters,
        ArrayList<Nozzle> nozzles)
    {
        this.typeCode.set(typeCode);
        this.name.set(friendlyName);
        this.uniqueID.set(uniqueID);
        this.headHours.set(headHours);
        this.nozzleHeaters.addAll(nozzleHeaters);
        this.nozzles.addAll(nozzles);
    }
//
//    /**
//     *
//     * @param response
//     */
//    public Head(HeadEEPROMDataResponse response)
//    {
//        this.typeCode.set(response.getTypeCode());
//        this.name.set("");
//        this.maximumTemperature.set(response.getMaximumTemperature());
//        this.beta.set(response.getBeta());
//        this.tcal.set(response.getTCal());
//        this.uniqueID.set(response.getUniqueID());
//        deriveZOverrunFromOffsets();
//    }

    public final ReadOnlyObjectProperty<EEPROMState> getHeadEEPROMStatusProperty()
    {
        return headEEPROMStatusProperty;
    }

    /**
     *
     * @return
     */
    public StringProperty getTypeCodeProperty()
    {
        return typeCode;
    }

    /**
     *
     * @return
     */
    public StringProperty getNameProperty()
    {
        return name;
    }

    /**
     *
     * @return
     */
    public StringProperty getUniqueIDProperty()
    {
        return uniqueID;
    }

    /**
     *
     * @return
     */
    public FloatProperty getHeadHoursProperty()
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

    public void updateFromEEPROMData(HeadEEPROMDataResponse eepromData)
    {
        // This only supports the initial RBX01-SM head since this is the only data from the printer at the moment...

        typeCode.set(eepromData.getTypeCode());
        uniqueID.set(eepromData.getUniqueID());
        headHours.set(eepromData.getHeadHours());

        nozzleHeaters.get(0).beta.set(eepromData.getBeta());
        nozzleHeaters.get(0).tcal.set(eepromData.getTCal());
        nozzleHeaters.get(0).lastFilamentTemperature.set(eepromData.getLastFilamentTemperature());
        nozzleHeaters.get(0).maximumTemperature.set(eepromData.getMaximumTemperature());

        nozzles.get(0).xOffset.set(eepromData.getNozzle1XOffset());
        nozzles.get(0).yOffset.set(eepromData.getNozzle1YOffset());
        nozzles.get(0).zOffset.set(eepromData.getNozzle1ZOffset());
        nozzles.get(0).bOffset.set(eepromData.getNozzle1BOffset());

        nozzles.get(1).xOffset.set(eepromData.getNozzle2XOffset());
        nozzles.get(1).yOffset.set(eepromData.getNozzle2YOffset());
        nozzles.get(1).zOffset.set(eepromData.getNozzle2ZOffset());
        nozzles.get(1).bOffset.set(eepromData.getNozzle2BOffset());
    }

    public HeadRepairResult repair(String receivedTypeCode)
    {
        HeadRepairResult result = HeadRepairResult.NO_REPAIR_NECESSARY;

        if (typeCode.get() == null || typeCode.equals("") || receivedTypeCode.equals("null"))
        {
            HeadFile headFile = HeadContainer.getHeadByID(HeadContainer.defaultHeadID);

            updateFromHeadFileData(headFile);

            String idToCreate = typeCode + SystemUtils.generate16DigitID().substring(typeCode.get().length());
            uniqueID.set(idToCreate);

            result = HeadRepairResult.REPAIRED_WRITE_AND_RECALIBRATE;
        } else
        {
            float epsilon = 0.00001f;

            HeadFile referenceHeadData = HeadContainer.getHeadByID(receivedTypeCode);
            if (referenceHeadData != null)
            {
                // Iterate through the nozzle heaters and check for differences
                for (int i = 0; i < getNozzleHeaters().size(); i++)
                {
                    NozzleHeater nozzleHeater = getNozzleHeaters().get(i);
                    NozzleHeaterData nozzleHeaterData = referenceHeadData.getNozzleHeaters().get(i);

                    if (Math.abs(nozzleHeater.getMaximumTemperatureProperty().get() - nozzleHeaterData.getMaximum_temperature_C()) > epsilon)
                    {
                        nozzleHeater.maximumTemperature.set(nozzleHeaterData.getMaximum_temperature_C());
                        result = HeadRepairResult.REPAIRED_WRITE_ONLY;
                    }

                    if (Math.abs(nozzleHeater.getTcalProperty().get() - nozzleHeaterData.getTcal()) > epsilon)
                    {
                        nozzleHeater.tcal.set(nozzleHeaterData.getTcal());
                        result = HeadRepairResult.REPAIRED_WRITE_ONLY;
                    }

                    if (Math.abs(nozzleHeater.getBetaProperty().get() - nozzleHeaterData.getBeta()) > epsilon)
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

                    if (nozzle.getXOffsetProperty().get() < nozzleData.getMinXOffset() || nozzle.getXOffsetProperty().get() > nozzleData.getMaxXOffset())
                    {
                        nozzle.xOffset.set(nozzleData.getDefaultXOffset());
                        result = HeadRepairResult.REPAIRED_WRITE_AND_RECALIBRATE;
                    }

                    if (nozzle.getYOffsetProperty().get() < nozzleData.getMinYOffset() || nozzle.getYOffsetProperty().get() > nozzleData.getMaxYOffset())
                    {
                        nozzle.yOffset.set(nozzleData.getDefaultYOffset());
                        result = HeadRepairResult.REPAIRED_WRITE_AND_RECALIBRATE;
                    }

                    if (nozzle.getZOffsetProperty().get() < nozzleData.getMinZOffset() || nozzle.getZOffsetProperty().get() > nozzleData.getMaxZOffset())
                    {
                        nozzle.zOffset.set(nozzleData.getDefaultZOffset());
                        result = HeadRepairResult.REPAIRED_WRITE_AND_RECALIBRATE;
                    }

                    if (nozzle.getBOffsetProperty().get() < nozzleData.getMinBOffset() || nozzle.getBOffsetProperty().get() > nozzleData.getMaxBOffset())
                    {
                        nozzle.bOffset.set(nozzleData.getDefaultBOffset());
                        result = HeadRepairResult.REPAIRED_WRITE_AND_RECALIBRATE;
                    }
                }
            }
        }

        return result;
    }

    void noHeadAttached()
    {
        typeCode.set("");
        name.set("");
        uniqueID.set("");
        headHours.set(0);

        nozzleHeaters.clear();
        nozzles.clear();
    }
}
