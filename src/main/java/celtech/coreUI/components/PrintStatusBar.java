/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

import celtech.Lookup;
import celtech.configuration.BusyStatus;
import celtech.configuration.PauseStatus;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.model.Printer;
import java.io.IOException;
import java.net.URL;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

/**
 *
 * @author tony
 */
public class PrintStatusBar extends AppearingProgressBar implements Initializable
{

    private Printer printer = null;

    private ChangeListener<PrinterStatus> printerStatusChangeListener = (ObservableValue<? extends PrinterStatus> ov, PrinterStatus lastState, PrinterStatus newState) ->
    {
        reassessStatus();
    };

    private ChangeListener<PauseStatus> pauseStatusChangeListener = (ObservableValue<? extends PauseStatus> ov, PauseStatus lastState, PauseStatus newState) ->
    {
        reassessStatus();
    };

    private ChangeListener<BusyStatus> busyStatusChangeListener = (ObservableValue<? extends BusyStatus> ov, BusyStatus lastState, BusyStatus newState) ->
    {
        reassessStatus();
    };

    public PrintStatusBar(Printer printer)
    {
        super();
        this.printer = printer;

        printer.printerStatusProperty().addListener(printerStatusChangeListener);
        printer.pauseStatusProperty().addListener(pauseStatusChangeListener);
        printer.busyStatusProperty().addListener(busyStatusChangeListener);

        reassessStatus();
    }

    private void reassessStatus()
    {
        boolean statusProcessed = false;
        boolean barShouldBeDisplayed = false;

        unbindVariables();

        //Now busy status
        switch (printer.busyStatusProperty().get())
        {
            case NOT_BUSY:
                break;
            case BUSY:
                break;
            case LOADING_FILAMENT:
            case UNLOADING_FILAMENT:
                statusProcessed = true;
                barShouldBeDisplayed = true;
                largeProgressDescription.setText(printer.busyStatusProperty().get().getI18nString());
                progressBar.setVisible(false);
                largeTargetLegend.setVisible(false);
                break;
            default:
                break;
        }

        //Pause status takes precedence
        if (!statusProcessed)
        {
            switch (printer.pauseStatusProperty().get())
            {
                case NOT_PAUSED:
                    break;
                case PAUSED:
                case PAUSE_PENDING:
                case RESUME_PENDING:
                    statusProcessed = true;
                    barShouldBeDisplayed = true;
                    largeProgressDescription.setText(printer.pauseStatusProperty().get().getI18nString());
                    progressBar.setVisible(false);
                    largeTargetLegend.setVisible(false);
                    break;
                default:
                    break;
            }
        }

        //Now print status
        if (!statusProcessed)
        {
            switch (printer.printerStatusProperty().get())
            {
                case IDLE:
                    break;
                case PRINTING:
                    statusProcessed = true;
                    barShouldBeDisplayed = true;
                    largeProgressDescription.setText(printer.printerStatusProperty().get().getI18nString());

                    if (printer.getPrintEngine().etcAvailableProperty().get())
                    {
                        largeTargetValue.textProperty().bind(new StringBinding()
                        {
                            {
                                super.bind(printer.getPrintEngine().progressETCProperty());
                            }

                            @Override
                            protected String computeValue()
                            {
                                int secondsRemaining = printer.getPrintEngine().progressETCProperty().intValue();
                                secondsRemaining += 30;
                                if (secondsRemaining > 60)
                                {
                                    String hoursMinutes = convertToHoursMinutes(
                                            secondsRemaining);
                                    return hoursMinutes;
                                } else
                                {
                                    return Lookup.i18n("dialogs.lessThanOneMinute");
                                }
                            }
                        });

                        largeTargetValue.setVisible(true);
                        largeTargetLegend.setText(Lookup.i18n("dialogs.progressETCLabel"));
                        largeTargetLegend.setVisible(true);
                    }

                    largeProgressCurrentValue.textProperty().bind(printer.getPrintEngine().progressProperty().multiply(100).asString("%.0f%%"));
                    largeProgressCurrentValue.setVisible(true);

                    progressBar.progressProperty().bind(printer.getPrintEngine().progressProperty());
                    progressBar.setVisible(true);
                    break;
                case RUNNING_MACRO:
                    statusProcessed = true;
                    barShouldBeDisplayed = true;
                    largeProgressDescription.setText(printer.getPrintEngine().macroBeingRun.get().getFriendlyName());

                    if (printer.getPrintEngine().linesInPrintingFileProperty().get() > 0)
                    {
                        largeProgressCurrentValue.textProperty().bind(printer.getPrintEngine().progressProperty().multiply(100).asString("%.0f%%"));
                        largeProgressCurrentValue.setVisible(true);

                        progressBar.progressProperty().bind(printer.getPrintEngine().progressProperty());
                        progressBar.setVisible(true);
                    }
                    break;
                default:
                    statusProcessed = true;
                    barShouldBeDisplayed = true;
                    largeProgressDescription.setText(printer.printerStatusProperty().get().getI18nString());
                    break;
            }
        }

        if (barShouldBeDisplayed)
        {
            startSlidingOut();
        } else
        {
            startSlidingIn();
        }
    }

//    public void manuallyConfigure(String progressDescription,
//            ReadOnlyDoubleProperty currentValueProperty,
//            ReadOnlyDoubleProperty currentTargetProperty)
//    {
//        if (progressDescription == null)
//        {
//            startSlidingIn();
//            unbind();
//            currentTargetProperty = null;
//            currentValueProperty = null;
//        } else
//        {
//            unbind();
//            largeProgressDescription.setText(progressDescription);
//            this.currentTargetProperty = currentTargetProperty;
//            this.currentValueProperty = currentValueProperty;
//            if (this.currentTargetProperty != null
//                    && this.currentValueProperty != null)
//            {
//                this.currentTargetProperty.addListener(progressChangeListener);
//                this.currentValueProperty.addListener(progressChangeListener);
//                displayMode = DisplayMode.PERCENT;
//                progressBarElement.setVisible(true);
//                largeTargetLegend.setVisible(false);
//                largeTargetValue.setVisible(false);
//            } else
//            {
//                progressBarElement.setVisible(false);
//                largeTargetLegend.setVisible(false);
//                largeTargetValue.setVisible(false);
//            }
//            startSlidingOut();
//        }
//
//        redraw();
//    }
//
//    public void configureForStatus(DisplayState displayState,
//            ReadOnlyDoubleProperty currentValueProperty,
//            ReadOnlyDoubleProperty currentTargetProperty,
//            ReadOnlyIntegerProperty etcProperty)
//    {
//        if (displayState == null)
//        {
//            startSlidingIn();
//            unbind();
//            currentTargetProperty = null;
//            currentValueProperty = null;
//        } else
//        {
//            unbind();
//            largeProgressCurrentValue.setText("");
//
//            if (displayState != DisplayState.IDLE)
//            {
//                largeProgressDescription.setText(displayState.getI18nString());
//            }
//            this.currentTargetProperty = currentTargetProperty;
//            this.currentValueProperty = currentValueProperty;
//
//            switch (displayState)
//            {
//                case HEATING_BED:
//                case HEATING_NOZZLE:
//                    largeTargetValue.textProperty().bind(currentTargetProperty.asString("%.0f")
//                            .concat(Lookup.i18n("misc.degreesC")));
//                    largeTargetLegend.setText(Lookup.i18n("progressBar.targetTemperature"));
//                    progressBarElement.setVisible(true);
//                    largeTargetLegend.setVisible(true);
//                    largeTargetValue.setVisible(true);
//                    displayMode = DisplayMode.TEMPERATURE;
//                    startSlidingOut();
//                    break;
//                case PRINTING:
//                    displayMode = DisplayMode.PERCENT;
//                    if (etcProperty != null)
//                    {
//                        this.currentEtcProperty = etcProperty;
//                        largeTargetLegend.setVisible(true);
//                        largeTargetLegend.setText(Lookup.i18n("dialogs.progressETCLabel"));
//                        largeTargetValue.textProperty().bind(new StringBinding()
//                        {
//                            {
//                                super.bind(currentEtcProperty);
//                            }
//
//                            @Override
//                            protected String computeValue()
//                            {
//                                int secondsRemaining = currentEtcProperty.intValue();
//                                secondsRemaining += 30;
//                                if (secondsRemaining > 60)
//                                {
//                                    String hoursMinutes = convertToHoursMinutes(
//                                            secondsRemaining);
//                                    return hoursMinutes;
//                                } else
//                                {
//                                    return Lookup.i18n("dialogs.lessThanOneMinute");
//                                }
//                            }
//                        });
//                        largeTargetValue.setVisible(true);
//                        progressBarElement.setVisible(true);
//                    } else
//                    {
//                        largeTargetLegend.setVisible(false);
//                        largeTargetValue.setVisible(false);
//                        largeTargetLegend.setText("");
//                    }
//                    startSlidingOut();
//                    break;
//                case SLICING:
//                case POST_PROCESSING:
//                    largeTargetLegend.setVisible(false);
//                    largeTargetValue.setVisible(false);
//                    largeTargetValue.textProperty().bind(currentTargetProperty.asString("%.0f%%"));
//                    largeTargetLegend.setText("");
//                    displayMode = DisplayMode.PERCENT;
//                    startSlidingOut();
//                    progressBarElement.setVisible(true);
//                    break;
//                case IDLE:
//                    progressBarElement.setVisible(false);
//                    largeTargetLegend.setVisible(false);
//                    largeTargetValue.setVisible(false);
//                    startSlidingIn();
//                    break;
//                default:
//                    displayMode = DisplayMode.PERCENT;
//                    progressBarElement.setVisible(false);
//                    largeTargetLegend.setVisible(false);
//                    largeTargetValue.setVisible(false);
//                    startSlidingOut();
//                    break;
//            }
//            if (this.currentTargetProperty != null)
//            {
//                this.currentTargetProperty.addListener(targetChangeListener);
//            }
//
//            if (this.currentValueProperty != null)
//            {
//                this.currentValueProperty.addListener(progressChangeListener);
//            }
//        }
//
//        redraw();
//    }
    private String convertToHoursMinutes(int seconds)
    {
        int minutes = (int) (seconds / 60);
        int hours = minutes / 60;
        minutes = minutes - (60 * hours);
        return String.format("%02d:%02d", hours, minutes);
    }

////    private void redraw()
////    {
////        if (!isSlidIn())
////        {
////            double normalisedProgress = 0;
////
////            if (currentValueProperty != null
////                    && currentTargetProperty != null
////                    && currentValueProperty.doubleValue() > 0
////                    && currentTargetProperty.doubleValue() > 0)
////            {
////                normalisedProgress = Math.max(Math.min(currentValueProperty.doubleValue() / currentTargetProperty.doubleValue(), 1.0), 0);
////                switch (displayMode)
////                {
////                    case PERCENT:
////                        largeProgressCurrentValue.setText(String.format("%.0f", normalisedProgress * 100.0).concat("%"));
////                        break;
////                    case TEMPERATURE:
////                        largeProgressCurrentValue.setText(String.format("%.0f", currentValueProperty.doubleValue()).concat(Lookup.i18n("misc.degreesC")));
////                        break;
////                }
////                largeProgressCurrentValue.setVisible(true);
////            } else
////            {
////                largeProgressCurrentValue.setText("");
////                largeProgressCurrentValue.setVisible(false);
////                normalisedProgress = 0;
////            }
////            double progressBackWidth = largeProgressBarBack.getWidth();
////            double barWidth = progressBackWidth * normalisedProgress;
////
////            largeProgressBarInner.setWidth(barWidth);
////
////            // place currentValue in correct place on progress bar (just to the left of RHS of the bar)
////            double barEndXPosition = largeProgressBarInner.getLayoutX()
////                    + largeProgressBarInner.boundsInParentProperty().get().getWidth();
////            double barStartXPosition = largeProgressBarInner.getLayoutX();
////            double currentValueWidth = largeProgressCurrentValue.boundsInParentProperty().get().
////                    getWidth();
////            int OFFSET_FROM_PROGRESS_BAR_RHS = 10;  // px
////            double requiredCurrentValueXPosition = barEndXPosition - currentValueWidth
////                    - OFFSET_FROM_PROGRESS_BAR_RHS;
////
////            double leftmostValuePositionAllowed = barStartXPosition + 2;
////            if (requiredCurrentValueXPosition < leftmostValuePositionAllowed)
////            {
////                requiredCurrentValueXPosition = leftmostValuePositionAllowed;
////            }
////
////            double currentX = largeProgressCurrentValue.getLayoutX();
////            double requiredTranslate = requiredCurrentValueXPosition - currentX;
////
////            largeProgressCurrentValue.setTranslateX(requiredTranslate);
////        }
////    }
//

    public void unbindAll()
    {
        if (printer != null)
        {
            printer.printerStatusProperty().removeListener(printerStatusChangeListener);
            printer.pauseStatusProperty().removeListener(pauseStatusChangeListener);
            printer.busyStatusProperty().removeListener(busyStatusChangeListener);
            unbindVariables();
            printer = null;
        }
    }

//    public enum DisplayState
//    {
//
//        IDLE("printerStatus.idle", PrinterStatus.IDLE),
//        PRINTING("printerStatus.printing", PrinterStatus.PRINTING),
//        PRINTING_GCODE("printerStatus.printingGCode", PrinterStatus.PRINTING_GCODE),
//        RUNNING_TEST("printerStatus.runningTest", PrinterStatus.RUNNING_TEST),
//        RUNNING_MACRO("printerStatus.executingMacro", PrinterStatus.RUNNING_MACRO),
//        REMOVING_HEAD("printerStatus.removingHead", PrinterStatus.REMOVING_HEAD),
//        PURGING_HEAD("printerStatus.purging", PrinterStatus.PURGING_HEAD),
//        OPENING_DOOR("printerStatus.openingDoor", PrinterStatus.OPENING_DOOR),
//        CALIBRATING_NOZZLE_ALIGNMENT("printerStatus.calibratingNozzleAlignment", PrinterStatus.CALIBRATING_NOZZLE_ALIGNMENT),
//        CALIBRATING_NOZZLE_HEIGHT("printerStatus.calibratingNozzleHeight", PrinterStatus.CALIBRATING_NOZZLE_HEIGHT),
//        CALIBRATING_NOZZLE_OPENING("printerStatus.calibratingNozzleOpening", PrinterStatus.CALIBRATING_NOZZLE_OPENING),
//        HEATING_BED("printerStatus.heatingBed", null),
//        HEATING_NOZZLE("printerStatus.heatingNozzle", null),
//        SLICING("printerStatus.slicing", null),
//        POST_PROCESSING("printerStatus.postProcessing", null),
//        PAUSING("printerStatus.pausing", null),
//        PAUSED("printerStatus.paused", null),
//        RESUMING("printerStatus.resuming", null),
//        LOADING_FILAMENT("printerStatus.loadingFilament", null),
//        EJECTING_FILAMENT("printerStatus.ejectingFilament", null);
//
//        private final String i18nString;
//        private final PrinterStatus equivalentPrinterStatus;
//
//        private DisplayState(String i18nString, PrinterStatus equivalentPrinterStatus)
//        {
//            this.i18nString = i18nString;
//            this.equivalentPrinterStatus = equivalentPrinterStatus;
//        }
//
//        /**
//         *
//         * @return
//         */
//        public String getI18nString()
//        {
//            return Lookup.i18n(i18nString);
//        }
//
//        /**
//         *
//         * @return
//         */
//        public PrinterStatus getEquivalentPrinterStatus()
//        {
//            return equivalentPrinterStatus;
//        }
//
//        /**
//         *
//         * @return
//         */
//        @Override
//        public String toString()
//        {
//            return getI18nString();
//        }
//
//        public static Optional<DisplayState> mapFromPrinterStatus(PrinterStatus printerStatus)
//        {
//            Optional<DisplayState> mappedValue = Optional.empty();
//
//            for (DisplayState state : DisplayState.values())
//            {
//                if (state.getEquivalentPrinterStatus() == printerStatus)
//                {
//                    mappedValue = Optional.of(state);
//                    break;
//                }
//            }
//
//            return mappedValue;
//        }
//    }
}
