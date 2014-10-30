/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.HeaterMode;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

/**
 * ChartManager is an auxiliary class to PrinterStatusSidePanelController and manages the
 * temperature chart.
 *
 * @author tony
 */
class ChartManager
{

    private final LineChart<Number, Number> chart;
    private XYChart.Series<Number, Number> nozzleData = new XYChart.Series<>();
    private XYChart.Series<Number, Number> ambientData = new XYChart.Series<>();
    private XYChart.Series<Number, Number> bedData = new XYChart.Series<>();
    private final LineChart.Series<Number, Number> ambientTargetTemperatureSeries = new LineChart.Series<>();
    private final LineChart.Series<Number, Number> bedTargetTemperatureSeries = new LineChart.Series<>();
    private final LineChart.Series<Number, Number> nozzleTargetTemperatureSeries = new LineChart.Series<>();
    private final LineChart.Data<Number, Number> ambientTargetPoint = new LineChart.Data<>(
        ApplicationConfiguration.NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP - 5, 0);
    private final LineChart.Data<Number, Number> bedTargetPoint = new LineChart.Data<>(
        ApplicationConfiguration.NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP - 5, 0);
    private final LineChart.Data<Number, Number> nozzleTargetPoint = new LineChart.Data<>(
        ApplicationConfiguration.NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP - 5, 0);
    
    private ReadOnlyIntegerProperty nozzleTargetTemperatureProperty;
    private ReadOnlyIntegerProperty bedTargetTemperatureProperty;
    private ReadOnlyIntegerProperty ambientTargetTemperatureProperty;
    private ReadOnlyObjectProperty<HeaterMode> bedHeaterModeProperty;
    private ReadOnlyObjectProperty<HeaterMode> nozzleHeaterModeProperty;

    public ChartManager(LineChart<Number, Number> chart)
    {
        this.chart = chart;
        ambientTargetTemperatureSeries.getData().add(ambientTargetPoint);
        bedTargetTemperatureSeries.getData().add(bedTargetPoint);
        nozzleTargetTemperatureSeries.getData().add(nozzleTargetPoint);
    }

    public void setNozzleData(XYChart.Series<Number, Number> nozzleData)
    {
        this.nozzleData = nozzleData;
        updateChartDataSources();
    }

    public void setAmbientData(XYChart.Series<Number, Number> ambientData)
    {
        this.ambientData = ambientData;
        updateChartDataSources();
    }

    public void setBedData(XYChart.Series<Number, Number> bedData)
    {
        this.bedData = bedData;
        updateChartDataSources();
    }

    /**
     * update the chart to display all the correct data.
     */
    private void updateChartDataSources()
    {
        chart.getData().clear();
        chart.getData().add(ambientTargetTemperatureSeries);
        chart.getData().add(bedTargetTemperatureSeries);
        chart.getData().add(nozzleTargetTemperatureSeries);
        chart.getData().add(ambientData);
        chart.getData().add(bedData);
        chart.getData().add(nozzleData);

    }

    void clearNozzleData()
    {
        nozzleData = new XYChart.Series<>();
    }

    void clearBedData()
    {
        bedData = new XYChart.Series<>();
    }

    void clearAmbientData()
    {
        ambientData = new XYChart.Series<>();
    }

    ChangeListener<Number> ambientTargetTemperatureListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        ambientTargetPoint.setYValue(newValue);
    };

    void setTargetAmbientTemperatureProperty(
        ReadOnlyIntegerProperty ambientTargetTemperatureProperty)
    {
        if (this.ambientTargetTemperatureProperty != null)
        {
            ambientTargetTemperatureProperty.removeListener(ambientTargetTemperatureListener);
        }
        ambientTargetPoint.setYValue(ambientTargetTemperatureProperty.get());
        this.ambientTargetTemperatureProperty = ambientTargetTemperatureProperty;
        ambientTargetTemperatureProperty.addListener(ambientTargetTemperatureListener);
    }

    ChangeListener<Number> bedTargetTemperatureListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        updateBedTargetPoint();
    };

    void setTargetBedTemperatureProperty(
        ReadOnlyIntegerProperty bedTargetTemperatureProperty)
    {
        if (this.bedTargetTemperatureProperty != null)
        {
            this.bedTargetTemperatureProperty.removeListener(bedTargetTemperatureListener);
        }
        this.bedTargetTemperatureProperty = bedTargetTemperatureProperty;
        
        bedTargetTemperatureProperty.addListener(bedTargetTemperatureListener);
        updateBedTargetPoint();
    }
    
    ChangeListener<HeaterMode> bedHeaterModeListener = (ObservableValue<? extends HeaterMode> observable, HeaterMode oldValue, HeaterMode newValue) ->
    {
        updateBedTargetPoint();
    };

    void setBedHeaterModeProperty(ReadOnlyObjectProperty<HeaterMode> bedHeaterModeProperty)
    {
        if (this.bedHeaterModeProperty != null)
        {
            this.bedHeaterModeProperty.removeListener(bedHeaterModeListener);
        }
        bedHeaterModeProperty.addListener(bedHeaterModeListener);
        this.bedHeaterModeProperty = bedHeaterModeProperty;
        updateBedTargetPoint();
    }
    
    void updateBedTargetPoint()
    {
        if (bedHeaterModeProperty.get() == HeaterMode.OFF)
        {
            bedTargetPoint.setYValue(0);
        } else
        {
            bedTargetPoint.setYValue(bedTargetTemperatureProperty.get());
        }
    }      

    ChangeListener<HeaterMode> nozzleHeaterModeListener = (ObservableValue<? extends HeaterMode> observable, HeaterMode oldValue, HeaterMode newValue) ->
    {
        updateNozzleTargetPoint();
    };

    void setNozzleHeaterModeProperty(ReadOnlyObjectProperty<HeaterMode> nozzleHeaterModeProperty)
    {
        if (this.nozzleHeaterModeProperty != null)
        {
            nozzleHeaterModeProperty.removeListener(nozzleHeaterModeListener);
        }
        nozzleHeaterModeProperty.addListener(nozzleHeaterModeListener);
        this.nozzleHeaterModeProperty = nozzleHeaterModeProperty;
        updateNozzleTargetPoint();
    }
    
    ChangeListener<Number> nozzleTargetTemperatureListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        updateNozzleTargetPoint();
    };

    void setTargetNozzleTemperatureProperty(ReadOnlyIntegerProperty nozzleTargetTemperatureProperty)
    {
        if (this.nozzleTargetTemperatureProperty != null)
        {
            this.nozzleTargetTemperatureProperty.removeListener(nozzleTargetTemperatureListener);
        }
        this.nozzleTargetTemperatureProperty = nozzleTargetTemperatureProperty;
        nozzleTargetTemperatureProperty.addListener(nozzleTargetTemperatureListener);
        updateNozzleTargetPoint();

    }   
    
    private void updateNozzleTargetPoint()
    {
        if (nozzleHeaterModeProperty == null || nozzleTargetTemperatureProperty == null) {
            return;
        }
        if (nozzleHeaterModeProperty.get() == HeaterMode.OFF)
        {
            nozzleTargetPoint.setYValue(0);
        } else
        {
            nozzleTargetPoint.setYValue(nozzleTargetTemperatureProperty.get());
        }
    }    

}
