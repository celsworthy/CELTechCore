/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

import celtech.Lookup;
import celtech.configuration.HeaterMode;
import celtech.printerControl.model.NozzleHeater;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;

/**
 *
 * @author tony
 */
public class NozzleHeaterStatusBar extends AppearingProgressBar implements Initializable
{

    private NozzleHeater heater = null;
    private static final double showBarIfMoreThanXDegreesOut = 3;

    private ChangeListener<Number> numberChangeListener = (ObservableValue<? extends Number> ov, Number lastState, Number newState) ->
    {
        reassessStatus();
    };

    private ChangeListener<HeaterMode> heaterModeChangeListener = (ObservableValue<? extends HeaterMode> ov, HeaterMode lastState, HeaterMode newState) ->
    {
        reassessStatus();
    };

    public NozzleHeaterStatusBar(NozzleHeater heater)
    {
        super();
        this.heater = heater;

        heater.nozzleTemperatureProperty().addListener(numberChangeListener);
        heater.nozzleTargetTemperatureProperty().addListener(numberChangeListener);
        heater.nozzleFirstLayerTargetTemperatureProperty().addListener(numberChangeListener);
        heater.heaterModeProperty().addListener(heaterModeChangeListener);

        reassessStatus();
    }

    private void reassessStatus()
    {
        boolean barShouldBeDisplayed = false;

        unbindVariables();

        switch (heater.heaterModeProperty().get())
        {
            case OFF:
                break;
            case FIRST_LAYER:
                if (Math.abs(heater.nozzleTemperatureProperty().get() - heater.nozzleFirstLayerTargetTemperatureProperty().get())
                        > showBarIfMoreThanXDegreesOut)
                {
                    largeProgressDescription.setText(Lookup.i18n("printerStatus.heatingNozzle"));

                    largeTargetValue.textProperty().bind(heater.nozzleFirstLayerTargetTemperatureProperty().asString("%d")
                            .concat(Lookup.i18n("misc.degreesC")));

                    largeTargetLegend.setText(Lookup.i18n("progressBar.targetTemperature"));

                    progressBar.progressProperty().bind(new DoubleBinding()
                    {
                        {
                            super.bind(heater.nozzleTemperatureProperty(), heater.nozzleFirstLayerTargetTemperatureProperty());
                        }

                        @Override
                        protected double computeValue()
                        {
                            double normalisedProgress = 0;
                            if (heater.nozzleFirstLayerTargetTemperatureProperty().doubleValue() > 0)
                            {
                                normalisedProgress = heater.nozzleTemperatureProperty().doubleValue() / heater.nozzleFirstLayerTargetTemperatureProperty().doubleValue();
                                normalisedProgress = Math.max(0, normalisedProgress);
                                normalisedProgress = Math.min(1, normalisedProgress);
                            }
                            return normalisedProgress;
                        }
                    });
                    showProgress();
                    showTargets();
                    barShouldBeDisplayed = true;
                }
                break;
            case NORMAL:
            case FILAMENT_EJECT:
                if (Math.abs(heater.nozzleTemperatureProperty().get() - heater.nozzleTargetTemperatureProperty().get())
                        > showBarIfMoreThanXDegreesOut)
                {
                    largeProgressDescription.setText(Lookup.i18n("printerStatus.heatingNozzle"));

                    largeTargetValue.textProperty().bind(heater.nozzleTargetTemperatureProperty().asString("%d")
                            .concat(Lookup.i18n("misc.degreesC")));

                    largeTargetLegend.setText(Lookup.i18n("progressBar.targetTemperature"));

                    progressBar.progressProperty().bind(new DoubleBinding()
                    {
                        {
                            super.bind(heater.nozzleTemperatureProperty(), heater.nozzleTargetTemperatureProperty());
                        }

                        @Override
                        protected double computeValue()
                        {
                            double normalisedProgress = 0;
                            if (heater.nozzleTargetTemperatureProperty().doubleValue() > 0)
                            {
                                normalisedProgress = heater.nozzleTemperatureProperty().doubleValue() / heater.nozzleTargetTemperatureProperty().doubleValue();
                                normalisedProgress = Math.max(0, normalisedProgress);
                                normalisedProgress = Math.min(1, normalisedProgress);
                            }
                            return normalisedProgress;
                        }
                    });
                    showProgress();
                    showTargets();
                    barShouldBeDisplayed = true;
                }
                break;
            default:
                break;
        }

        if (barShouldBeDisplayed)
        {
            startSlidingInToView();
        } else
        {
            startSlidingOutOfView();
        }
    }

    public void unbindAll()
    {
        if (heater != null)
        {
            heater.nozzleTemperatureProperty().removeListener(numberChangeListener);
            heater.nozzleTargetTemperatureProperty().removeListener(numberChangeListener);
            heater.nozzleFirstLayerTargetTemperatureProperty().removeListener(numberChangeListener);
            heater.heaterModeProperty().removeListener(heaterModeChangeListener);
            unbindVariables();
            slideOutOfView();
            heater = null;
        }
    }
}
