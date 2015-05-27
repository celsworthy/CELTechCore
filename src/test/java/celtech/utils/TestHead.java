/*
 * Copyright 2015 CEL UK
 */
package celtech.utils;

import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.NozzleHeaterData;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.NozzleHeater;
import javafx.beans.property.FloatProperty;

/**
 *
 * @author tony
 */
public class TestHead extends Head
{

    public TestHead(HeadFile headFile)
    {
        super(headFile);
    }

    @Override
    protected NozzleHeater makeNozzleHeater(NozzleHeaterData nozzleHeaterData)
    {
        return new TestNozzleHeater(nozzleHeaterData.getMaximum_temperature_C(),
                                    nozzleHeaterData.getBeta(),
                                    nozzleHeaterData.getTcal(),
                                    0, 0, 0, 0);
    }

    public class TestNozzleHeater extends NozzleHeater
    {

        public TestNozzleHeater(float maximumTemperature,
            float beta,
            float tcal,
            float lastFilamentTemperature,
            int nozzleTemperature,
            int nozzleFirstLayerTargetTemperature,
            int nozzleTargetTemperature)
        {
            super(maximumTemperature, beta, tcal, lastFilamentTemperature, nozzleTemperature,
                  nozzleFirstLayerTargetTemperature, nozzleTargetTemperature);
        }

        public FloatProperty lastFilamentTemperatureProperty()
        {
            return lastFilamentTemperature;
        }

    }

}
