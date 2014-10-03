package celtech.printerControl.model;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.HeaterMode;
import java.util.ArrayList;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.LineChart;

/**
 *
 * @author ianhudson
 */
public class NozzleHeater implements Cloneable
{

    protected final ObjectProperty<HeaterMode> heaterMode = new SimpleObjectProperty<>(HeaterMode.OFF);

    protected final FloatProperty maximumTemperature = new SimpleFloatProperty(0);
    protected final FloatProperty beta = new SimpleFloatProperty(0);
    protected final FloatProperty tcal = new SimpleFloatProperty(0);
    protected final FloatProperty lastFilamentTemperature = new SimpleFloatProperty(0);

    private final IntegerProperty nozzleTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty nozzleFirstLayerTargetTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty nozzleTargetTemperature = new SimpleIntegerProperty(0);

    private final LineChart.Series<Number, Number> nozzleTemperatureHistory = new LineChart.Series<>();
    private final ArrayList<LineChart.Data<Number, Number>> nozzleTemperatureDataPoints = new ArrayList<>();
    private final LineChart.Series<Number, Number> nozzleTargetTemperatureSeries = new LineChart.Series<>();
    private final LineChart.Data<Number, Number> nozzleTargetPoint = new LineChart.Data<>(
        ApplicationConfiguration.NUMBER_OF_TEMPERATURE_POINTS_TO_KEEP + 5, 0);

    public NozzleHeater(float maximumTemperature,
        float beta,
        float tcal,
        float lastFilamentTemperature,
        int nozzleTemperature,
        int nozzleFirstLayerTargetTemperature,
        int nozzleTargetTemperature)
    {
        this.maximumTemperature.set(maximumTemperature);
        this.beta.set(beta);
        this.tcal.set(tcal);
        this.lastFilamentTemperature.set(lastFilamentTemperature);
        this.nozzleTemperature.set(nozzleTemperature);
        this.nozzleFirstLayerTargetTemperature.set(nozzleFirstLayerTargetTemperature);
        this.nozzleTargetTemperature.set(nozzleTargetTemperature);
    }

    public final ReadOnlyObjectProperty<HeaterMode> heaterModeProperty()
    {
        return heaterMode;
    }

    /**
     *
     * @return
     */
    public ReadOnlyFloatProperty getMaximumTemperatureProperty()
    {
        return maximumTemperature;
    }

    /**
     *
     * @return
     */
    public ReadOnlyFloatProperty getBetaProperty()
    {
        return beta;
    }

    /**
     *
     * @return
     */
    public ReadOnlyFloatProperty getTcalProperty()
    {
        return tcal;
    }

    /**
     *
     * @return
     */
    public ReadOnlyFloatProperty getLastFilamentTemperatureProperty()
    {
        return lastFilamentTemperature;
    }

    public ReadOnlyObjectProperty<HeaterMode> getHeaterModeProperty()
    {
        return heaterMode;
    }

    public IntegerProperty getNozzleTemperatureProperty()
    {
        return nozzleTemperature;
    }

    public IntegerProperty getNozzleFirstLayerTargetTemperatureProperty()
    {
        return nozzleFirstLayerTargetTemperature;
    }

    public IntegerProperty getNozzleTargetTemperatureProperty()
    {
        return nozzleTargetTemperature;
    }

    /**
     *
     * @return
     */
    @Override
    public NozzleHeater clone()
    {
        NozzleHeater clone = new NozzleHeater(
            maximumTemperature.floatValue(),
            beta.floatValue(),
            tcal.floatValue(),
            lastFilamentTemperature.floatValue(),
            nozzleTemperature.intValue(),
            nozzleFirstLayerTargetTemperature.intValue(),
            nozzleTargetTemperature.intValue()
        );

        return clone;
    }
}
