/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

import celtech.Lookup;
import celtech.configuration.HeaterMode;
import celtech.printerControl.model.PrinterAncillarySystems;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;

/**
 *
 * @author tony
 */
public class BedHeaterStatusBar extends AppearingProgressBar implements Initializable
{

    private ReadOnlyObjectProperty<HeaterMode> heaterMode;
    private ReadOnlyIntegerProperty bedTemperature;
    private ReadOnlyIntegerProperty bedTargetTemperature;
    private ReadOnlyIntegerProperty bedFirstLayerTargetTemperature;

    private static final double showBarIfMoreThanXDegreesOut = 5;

    private final ChangeListener<Number> numberChangeListener = (ObservableValue<? extends Number> ov, Number lastState, Number newState) ->
    {
        reassessStatus();
    };

    private final ChangeListener<HeaterMode> heaterModeChangeListener = (ObservableValue<? extends HeaterMode> ov, HeaterMode lastState, HeaterMode newState) ->
    {
        reassessStatus();
    };

    public BedHeaterStatusBar(PrinterAncillarySystems printerSystems)
    {
        super();
        this.heaterMode = printerSystems.bedHeaterModeProperty();
        this.bedTemperature = printerSystems.bedTemperatureProperty();
        this.bedFirstLayerTargetTemperature = printerSystems.bedFirstLayerTargetTemperatureProperty();
        this.bedTargetTemperature = printerSystems.bedTargetTemperatureProperty();

        this.bedTemperature.addListener(numberChangeListener);
        this.bedFirstLayerTargetTemperature.addListener(numberChangeListener);
        this.bedTargetTemperature.addListener(numberChangeListener);
        this.heaterMode.addListener(heaterModeChangeListener);

        reassessStatus();
    }

    private void reassessStatus()
    {
        boolean barShouldBeDisplayed = false;

        unbindVariables();

        switch (heaterMode.get())
        {
            case OFF:
                break;
            case FIRST_LAYER:
                if (Math.abs(bedTemperature.get() - bedFirstLayerTargetTemperature.get())
                        > showBarIfMoreThanXDegreesOut)
                {
                    largeProgressDescription.setText(Lookup.i18n("printerStatus.heatingBed"));

                    largeTargetValue.textProperty().bind(bedFirstLayerTargetTemperature.asString("%d")
                            .concat(Lookup.i18n("misc.degreesC")));
                    largeTargetValue.setVisible(true);

                    largeTargetLegend.setText(Lookup.i18n("progressBar.targetTemperature"));
                    largeTargetLegend.setVisible(true);

                    largeProgressCurrentValue.textProperty().bind(bedTemperature.asString("%d")
                            .concat(Lookup.i18n("misc.degreesC")));
                    largeProgressCurrentValue.setVisible(true);

                    progressBar.progressProperty().bind(new DoubleBinding()
                    {
                        {
                            super.bind(bedTemperature, bedFirstLayerTargetTemperature);
                        }

                        @Override
                        protected double computeValue()
                        {
                            double normalisedProgress = 0;
                            if (bedFirstLayerTargetTemperature.doubleValue() > 0)
                            {
                                normalisedProgress = bedTemperature.doubleValue() / bedFirstLayerTargetTemperature.doubleValue();
                                normalisedProgress = Math.max(0, normalisedProgress);
                                normalisedProgress = Math.min(1, normalisedProgress);
                            }
                            return normalisedProgress;
                        }
                    });
                    progressBar.setVisible(true);
                    barShouldBeDisplayed = true;
                }
                break;
            case NORMAL:
            case FILAMENT_EJECT:
                if (Math.abs(bedTemperature.get() - bedTargetTemperature.get())
                        > showBarIfMoreThanXDegreesOut)
                {
                    largeProgressDescription.setText(Lookup.i18n("printerStatus.heatingBed"));

                    largeTargetValue.textProperty().bind(bedTargetTemperature.asString("%d")
                            .concat(Lookup.i18n("misc.degreesC")));
                    largeTargetValue.setVisible(true);

                    largeTargetLegend.setText(Lookup.i18n("progressBar.targetTemperature"));
                    largeTargetLegend.setVisible(true);

                    largeProgressCurrentValue.textProperty().bind(bedTemperature.asString("%d")
                            .concat(Lookup.i18n("misc.degreesC")));
                    largeProgressCurrentValue.setVisible(true);

                    progressBar.progressProperty().bind(new DoubleBinding()
                    {
                        {
                            super.bind(bedTemperature, bedTargetTemperature);
                        }

                        @Override
                        protected double computeValue()
                        {
                            double normalisedProgress = 0;
                            if (bedTargetTemperature.doubleValue() > 0)
                            {
                                normalisedProgress = bedTemperature.doubleValue() / bedTargetTemperature.doubleValue();
                                normalisedProgress = Math.max(0, normalisedProgress);
                                normalisedProgress = Math.min(1, normalisedProgress);
                            }
                            return normalisedProgress;
                        }
                    });
                    progressBar.setVisible(true);
                    barShouldBeDisplayed = true;
                }
                break;
            default:
                break;
        }

        if (barShouldBeDisplayed)
        {
            startSlidingOut();
        } else
        {
            startSlidingIn();
        }
    }

    public void unbindAll()
    {
        if (heaterMode != null)
        {
            heaterMode.removeListener(heaterModeChangeListener);
            heaterMode = null;
        }

        if (bedTemperature != null)
        {
            bedTemperature.removeListener(numberChangeListener);
            bedTemperature = null;
        }

        if (bedFirstLayerTargetTemperature != null)
        {
            bedFirstLayerTargetTemperature.removeListener(numberChangeListener);
            bedFirstLayerTargetTemperature = null;
        }

        if (bedTargetTemperature != null)
        {
            bedTargetTemperature.removeListener(numberChangeListener);
            bedTargetTemperature = null;
        }
        unbindVariables();
    }
}
