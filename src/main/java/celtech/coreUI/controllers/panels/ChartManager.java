/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

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

    public ChartManager(LineChart<Number, Number> chart)
    {
        this.chart = chart;
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

}
