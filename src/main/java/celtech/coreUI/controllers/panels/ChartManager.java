/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.HeaterMode;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

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
    private ReadOnlyIntegerProperty nozzleFirstLayerTargetTemperatureProperty;
    private ReadOnlyIntegerProperty bedTargetTemperatureProperty;
    private ReadOnlyIntegerProperty bedFirstLayerTargetTemperatureProperty;
    private ReadOnlyIntegerProperty ambientTargetTemperatureProperty;
    private ReadOnlyIntegerProperty nozzleTemperatureProperty;
    private ReadOnlyIntegerProperty bedTemperatureProperty;
    private ReadOnlyIntegerProperty ambientTemperatureProperty;
    private ReadOnlyObjectProperty<HeaterMode> bedHeaterModeProperty;
    private ReadOnlyObjectProperty<HeaterMode> nozzleHeaterModeProperty;
    private Label legendNozzle;
    private Label legendBed;
    private Label legendAmbient;

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
        nozzleTemperatureProperty = null;
    }

    void clearBedData()
    {
        bedData = new XYChart.Series<>();
        bedTemperatureProperty = null;
    }

    void clearAmbientData()
    {
        ambientData = new XYChart.Series<>();
        ambientTemperatureProperty = null;
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

    ChangeListener<Number> bedFirstLayerTargetTemperatureListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        updateBedTargetPoint();
    };

    void setTargetBedFirstLayerTemperatureProperty(
        ReadOnlyIntegerProperty bedFirstLayerTargetTemperatureProperty)
    {
        if (this.bedFirstLayerTargetTemperatureProperty != null)
        {
            this.bedFirstLayerTargetTemperatureProperty.removeListener(bedTargetTemperatureListener);
        }
        this.bedFirstLayerTargetTemperatureProperty = bedFirstLayerTargetTemperatureProperty;

        bedFirstLayerTargetTemperatureProperty.addListener(bedFirstLayerTargetTemperatureListener);
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
        } else if (bedHeaterModeProperty.get() == HeaterMode.FIRST_LAYER)
        {
            bedTargetPoint.setYValue(bedFirstLayerTargetTemperatureProperty.get());
        } else if (bedHeaterModeProperty.get() == HeaterMode.NORMAL)
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

    ChangeListener<Number> nozzleFirstLayerTargetTemperatureListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        updateNozzleTargetPoint();
    };

    void setTargetFirstLayerNozzleTemperatureProperty(
        ReadOnlyIntegerProperty nozzleFirstLayerTargetTemperatureProperty)
    {
        if (this.nozzleFirstLayerTargetTemperatureProperty != null)
        {
            this.nozzleFirstLayerTargetTemperatureProperty.removeListener(
                nozzleFirstLayerTargetTemperatureListener);
        }
        this.nozzleFirstLayerTargetTemperatureProperty = nozzleFirstLayerTargetTemperatureProperty;
        nozzleFirstLayerTargetTemperatureProperty.addListener(
            nozzleFirstLayerTargetTemperatureListener);
        updateNozzleTargetPoint();

    }

    private void updateNozzleTargetPoint()
    {
        if (nozzleHeaterModeProperty == null || nozzleTargetTemperatureProperty == null
            || nozzleFirstLayerTargetTemperatureProperty == null)
        {
            return;
        }
        if (nozzleHeaterModeProperty.get() == HeaterMode.OFF)
        {
            nozzleTargetPoint.setYValue(0);
        } else if (nozzleHeaterModeProperty.get() == HeaterMode.FIRST_LAYER)
        {
            nozzleTargetPoint.setYValue(nozzleFirstLayerTargetTemperatureProperty.get());
        } else
        {
            nozzleTargetPoint.setYValue(nozzleTargetTemperatureProperty.get());
        }
    }

    void setLegendLabels(Label legendNozzle, Label legendBed, Label legendAmbient)
    {
        this.legendNozzle = legendNozzle;
        this.legendBed = legendBed;
        this.legendAmbient = legendAmbient;
        updateLegend();
    }

    void clearLegendLabels()
    {
        this.legendNozzle = null;
        this.legendBed = null;
        this.legendAmbient = null;
        updateLegend();
    }

    private void updateLegend()
    {
        String degreesC = Lookup.i18n("misc.degreesC");
        String legendNozzleText = Lookup.i18n("printerStatus.temperatureGraphNozzleLabel");
        String legendBedText = Lookup.i18n("printerStatus.temperatureGraphBedLabel");
        String legendAmbientText = Lookup.i18n("printerStatus.temperatureGraphAmbientLabel");

        if (legendNozzle != null && nozzleTemperatureProperty != null)
        {
            if (nozzleTemperatureProperty.get() >= ApplicationConfiguration.minTempToDisplayOnGraph)
            {
                legendNozzleText += String.format(" %s%s",
                                                  nozzleTemperatureProperty.get(),
                                                  degreesC);
            } else
            {
                legendNozzleText += " " + Lookup.i18n("printerStatus.tempOutOfRangeLow");
            }
        }

        if (legendBed != null && bedTemperatureProperty != null)
        {
            if (bedTemperatureProperty.get() >= ApplicationConfiguration.minTempToDisplayOnGraph)
            {
                legendBedText += String.format(" %s%s", bedTemperatureProperty.get(), degreesC);
            } else
            {
                legendBedText += " " + Lookup.i18n("printerStatus.tempOutOfRangeLow");
            }
        }

        if (legendAmbient != null && ambientTemperatureProperty != null)
        {
            legendAmbientText += String.format(" %s%s", ambientTemperatureProperty.get(), degreesC);
        }
        
        if (legendNozzle != null)
        {
            legendNozzle.setText(legendNozzleText);
        }
        if (legendBed != null)
        {
            legendBed.setText(legendBedText);
        }
        if (legendAmbient != null)
        {
            legendAmbient.setText(legendAmbientText);
        }
    }

    InvalidationListener nozzleTemperatureListener = (Observable observable) ->
    {
        updateLegend();
    };

    void setNozzleTemperatureProperty(ReadOnlyIntegerProperty nozzleTemperatureProperty)
    {
        if (this.nozzleTemperatureProperty != null)
        {
            this.nozzleTemperatureProperty.removeListener(nozzleTemperatureListener);
        }
        this.nozzleTemperatureProperty = nozzleTemperatureProperty;
        nozzleTemperatureProperty.addListener(nozzleTemperatureListener);
        updateLegend();
    }

    InvalidationListener bedTemperatureListener = (Observable observable) ->
    {
        updateLegend();
    };

    void setBedTemperatureProperty(ReadOnlyIntegerProperty bedTemperatureProperty)
    {
        if (this.bedTemperatureProperty != null)
        {
            this.bedTemperatureProperty.removeListener(bedTemperatureListener);
        }
        this.bedTemperatureProperty = bedTemperatureProperty;
        bedTemperatureProperty.addListener(bedTemperatureListener);
        updateLegend();
    }

    InvalidationListener ambientTemperatureListener = (Observable observable) ->
    {
        updateLegend();
    };

    void setAmbientTemperatureProperty(ReadOnlyIntegerProperty ambientTemperatureProperty)
    {
        if (this.ambientTemperatureProperty != null)
        {
            this.ambientTemperatureProperty.removeListener(ambientTemperatureListener);
        }
        this.ambientTemperatureProperty = ambientTemperatureProperty;
        ambientTemperatureProperty.addListener(ambientTemperatureListener);
        updateLegend();
    }

}
