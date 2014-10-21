/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.HeaterMode;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
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
    private HeaterMode nozzleHeaterMode;
    private HeaterMode bedHeaterMode;
    private ReadOnlyIntegerProperty nozzleTargetTemperatureProperty;
    private ReadOnlyIntegerProperty bedTargetTemperatureProperty;

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

    void setTargetAmbientTemperatureProperty(
        ReadOnlyIntegerProperty ambientTargetTemperatureProperty)
    {
        ambientTargetPoint.setYValue(ambientTargetTemperatureProperty.get());
        ambientTargetTemperatureProperty.addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                ambientTargetPoint.setYValue(newValue);
            });

    }

    void setTargetBedTemperatureProperty(
        ReadOnlyIntegerProperty bedTargetTemperatureProperty)
    {
        bedTargetPoint.setYValue(bedTargetTemperatureProperty.get());
        this.bedTargetTemperatureProperty = bedTargetTemperatureProperty;
        bedTargetTemperatureProperty.addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                if (bedHeaterMode == HeaterMode.OFF)
                {
                    bedTargetPoint.setYValue(0);
                } else
                {
                    bedTargetPoint.setYValue(newValue);
                }
            });

    }

    void setTargetNozzleTemperatureProperty(
        ReadOnlyIntegerProperty nozzleTargetTemperatureProperty)
    {
        this.nozzleTargetTemperatureProperty = nozzleTargetTemperatureProperty;
        nozzleTargetPoint.setYValue(nozzleTargetTemperatureProperty.get());
        nozzleTargetTemperatureProperty.addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                if (nozzleHeaterMode == HeaterMode.OFF)
                {
                    nozzleTargetPoint.setYValue(0);
                } else
                {
                    nozzleTargetPoint.setYValue(newValue);
                }
            });

    }

    void setBedHeaterModeProperty(ReadOnlyObjectProperty<HeaterMode> bedHeaterModeProperty)
    {
        bedHeaterMode = bedHeaterModeProperty.get();
        bedHeaterModeProperty.addListener(
            (ObservableValue<? extends HeaterMode> observable, HeaterMode oldValue, HeaterMode newValue) ->
            {
                bedHeaterMode = newValue;
                if (bedHeaterMode == HeaterMode.OFF)
                {
                    bedTargetPoint.setYValue(0);
                } else
                {
                    bedTargetPoint.setYValue(bedTargetTemperatureProperty.get());
                }
            });
    }

    void setNozzleHeaterModeProperty(ReadOnlyObjectProperty<HeaterMode> heaterModeProperty)
    {
        nozzleHeaterMode = heaterModeProperty.get();
        heaterModeProperty.addListener(
            (ObservableValue<? extends HeaterMode> observable, HeaterMode oldValue, HeaterMode newValue) ->
            {
                nozzleHeaterMode = newValue;
                if (nozzleHeaterMode == HeaterMode.OFF)
                {
                    nozzleTargetPoint.setYValue(0);
                } else
                {
                    nozzleTargetPoint.setYValue(nozzleTargetTemperatureProperty.get());
                }
            });
    }

}
