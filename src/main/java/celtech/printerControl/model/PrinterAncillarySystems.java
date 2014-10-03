/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.model;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.HeaterMode;
import celtech.configuration.WhyAreWeWaitingState;
import java.util.ArrayList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.LineChart;

/**
 *
 * @author Ian
 */
public class PrinterAncillarySystems
{

    protected final BooleanProperty XStopSwitchProperty = new SimpleBooleanProperty(false);
    protected final BooleanProperty YStopSwitchProperty = new SimpleBooleanProperty(false);
    protected final BooleanProperty ZStopSwitchProperty = new SimpleBooleanProperty(false);
    protected final BooleanProperty ZTopStopSwitchProperty = new SimpleBooleanProperty(false);
    protected final BooleanProperty reelButtonProperty = new SimpleBooleanProperty(false);
    protected final BooleanProperty headFanOnProperty = new SimpleBooleanProperty(false);
    protected final BooleanProperty ambientFanOnProperty = new SimpleBooleanProperty(false);
    protected final BooleanProperty bAxisHomeProperty = new SimpleBooleanProperty(false);
    protected final BooleanProperty lidOpenProperty = new SimpleBooleanProperty(false);
    
    protected final ObjectProperty<HeaterMode> bedHeaterModeProperty = new SimpleObjectProperty<>(HeaterMode.OFF);
    private final IntegerProperty ambientTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty ambientTargetTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty bedTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty bedFirstLayerTargetTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty bedTargetTemperature = new SimpleIntegerProperty(0);
    private final LineChart.Series<Number, Number> ambientTemperatureHistory = new LineChart.Series<>();
    private final ArrayList<LineChart.Data<Number, Number>> ambientTemperatureDataPoints = new ArrayList<>();
    private final LineChart.Series<Number, Number> bedTemperatureHistory = new LineChart.Series<>();
    private final ArrayList<LineChart.Data<Number, Number>> bedTemperatureDataPoints = new ArrayList<>();
    private final LineChart.Series<Number, Number> ambientTargetTemperatureSeries = new LineChart.Series<>();
    private final LineChart.Series<Number, Number> bedTargetTemperatureSeries = new LineChart.Series<>();
    private final LineChart.Data<Number, Number> ambientTargetPoint = new LineChart.Data<>(
        ApplicationConfiguration.NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP + 5, 0);
    private final LineChart.Data<Number, Number> bedTargetPoint = new LineChart.Data<>(
        ApplicationConfiguration.NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP + 5, 0);
    
    protected final ObjectProperty<WhyAreWeWaitingState> whyAreWeWaitingProperty = new SimpleObjectProperty<>(WhyAreWeWaitingState.NOT_WAITING);

    public ReadOnlyBooleanProperty getXStopSwitchProperty()
    {
        return XStopSwitchProperty;
    }

    public ReadOnlyBooleanProperty getYStopSwitchProperty()
    {
        return YStopSwitchProperty;
    }

    public ReadOnlyBooleanProperty getZStopSwitchProperty()
    {
        return ZStopSwitchProperty;
    }

    public ReadOnlyBooleanProperty getZTopStopSwitchProperty()
    {
        return ZTopStopSwitchProperty;
    }

    public ReadOnlyBooleanProperty getReelButtonProperty()
    {
        return reelButtonProperty;
    }

    public ReadOnlyBooleanProperty getHeadFanOnProperty()
    {
        return headFanOnProperty;
    }

    public ReadOnlyBooleanProperty getAmbientFanOnProperty()
    {
        return ambientFanOnProperty;
    }

    public ReadOnlyBooleanProperty getBAxisHomeProperty()
    {
        return bAxisHomeProperty;
    }

    public ReadOnlyBooleanProperty getLidOpenProperty()
    {
        return lidOpenProperty;
    }

    public ReadOnlyObjectProperty<HeaterMode> getBedHeaterModeProperty()
    {
        return bedHeaterModeProperty;
    }

    public ReadOnlyObjectProperty<WhyAreWeWaitingState> getWhyAreWeWaitingProperty()
    {
        return whyAreWeWaitingProperty;
    }

    public ReadOnlyIntegerProperty getAmbientTemperatureProperty()
    {
        return ambientTemperature;
    }

    public ReadOnlyIntegerProperty getAmbientTargetTemperatureProperty()
    {
        return ambientTargetTemperature;
    }

    public ReadOnlyIntegerProperty getBedTemperatureProperty()
    {
        return bedTemperature;
    }

    public ReadOnlyIntegerProperty getBedFirstLayerTargetTemperatureProperty()
    {
        return bedFirstLayerTargetTemperature;
    }

    public ReadOnlyIntegerProperty getBedTargetTemperatureProperty()
    {
        return bedTargetTemperature;
    } 
}
