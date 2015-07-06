/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

import celtech.Lookup;
import celtech.configuration.BusyStatus;
import celtech.configuration.Macro;
import celtech.configuration.PauseStatus;
import celtech.printerControl.PrintQueueStatus;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.model.Printer;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;

/**
 *
 * @author tony
 */
public class PrintStatusBar extends AppearingProgressBar implements Initializable
{

    private Printer printer = null;

    private final ChangeListener<PrinterStatus> printerStatusChangeListener = (ObservableValue<? extends PrinterStatus> ov, PrinterStatus lastState, PrinterStatus newState) ->
    {
        reassessStatus();
    };

    private final ChangeListener<PrintQueueStatus> printQueueStatusChangeListener = (ObservableValue<? extends PrintQueueStatus> ov, PrintQueueStatus lastState, PrintQueueStatus newState) ->
    {
        reassessStatus();
    };

    private final ChangeListener<PauseStatus> pauseStatusChangeListener = (ObservableValue<? extends PauseStatus> ov, PauseStatus lastState, PauseStatus newState) ->
    {
        reassessStatus();
    };

    private final ChangeListener<BusyStatus> busyStatusChangeListener = (ObservableValue<? extends BusyStatus> ov, BusyStatus lastState, BusyStatus newState) ->
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
        printer.getPrintEngine().printQueueStatusProperty().addListener(printQueueStatusChangeListener);

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
            case LOADING_FILAMENT_E:
            case UNLOADING_FILAMENT_E:
            case LOADING_FILAMENT_D:
            case UNLOADING_FILAMENT_D:
                statusProcessed = true;
                barShouldBeDisplayed = true;
                largeProgressDescription.setText(printer.busyStatusProperty().get().getI18nString());
                progressBar.setVisible(false);
                hideTargets();
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
                    hideTargets();
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
                case PRINTING_PROJECT:
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

                        largeTargetLegend.setText(Lookup.i18n("dialogs.progressETCLabel"));
                        showTargets();
                    }

                    largeProgressCurrentValue.textProperty().bind(printer.getPrintEngine().progressProperty().multiply(100).asString("%.0f%%"));
                    progressBar.progressProperty().bind(printer.getPrintEngine().progressProperty());
                    
                    showProgress();
                    break;
                case RUNNING_MACRO_FILE:
                    statusProcessed = true;
                    barShouldBeDisplayed = true;
                    largeProgressDescription.setText(printer.getPrintEngine().macroBeingRun.get().getFriendlyName());

                    if (printer.getPrintEngine().macroBeingRun.get() != Macro.CANCEL_PRINT)
                    {
                        if (printer.getPrintEngine().linesInPrintingFileProperty().get() > 0)
                        {
                            largeProgressCurrentValue.textProperty().bind(printer.getPrintEngine().progressProperty().multiply(100).asString("%.0f%%"));
                            largeProgressCurrentValue.setVisible(true);

                            progressBar.progressProperty().bind(printer.getPrintEngine().progressProperty());
                            showProgress();
                        }
                    }
                    
                    hideTargets();
                    break;
                case CALIBRATING_NOZZLE_ALIGNMENT:
                case CALIBRATING_NOZZLE_OPENING:
                case CALIBRATING_NOZZLE_HEIGHT:
                    statusProcessed = true;
                    barShouldBeDisplayed = true;
                    largeProgressDescription.setText(printer.printerStatusProperty().get().getI18nString());

                    if (printer.getPrintEngine().printQueueStatusProperty().get() == PrintQueueStatus.PRINTING)
                    {
                        if (printer.getPrintEngine().linesInPrintingFileProperty().get() > 0)
                        {
                            largeProgressCurrentValue.textProperty().bind(printer.getPrintEngine().progressProperty().multiply(100).asString("%.0f%%"));
                            largeProgressCurrentValue.setVisible(true);

                            progressBar.progressProperty().bind(printer.getPrintEngine().progressProperty());
                            showProgress();
                        }
                    }
                    hideTargets();
                    break;
                case PURGING_HEAD:
                    statusProcessed = true;
                    barShouldBeDisplayed = true;
                    largeProgressDescription.setText(printer.printerStatusProperty().get().getI18nString());

                    if (printer.getPrintEngine().printQueueStatusProperty().get() == PrintQueueStatus.RUNNING_MACRO
                            && printer.getPrintEngine().macroBeingRun.get() == Macro.PURGE)
                    {
                        if (printer.getPrintEngine().linesInPrintingFileProperty().get() > 0)
                        {
                            largeProgressCurrentValue.textProperty().bind(printer.getPrintEngine().progressProperty().multiply(100).asString("%.0f%%"));
                            largeProgressCurrentValue.setVisible(true);

                            progressBar.progressProperty().bind(printer.getPrintEngine().progressProperty());
                            showProgress();
                        }
                    }
                    hideTargets();
                    break;
                default:
                    statusProcessed = true;
                    barShouldBeDisplayed = true;
                    largeProgressDescription.setText(printer.printerStatusProperty().get().getI18nString());
                    hideTargets();
                    hideProgress();
                    break;
            }
        }

        if (barShouldBeDisplayed)
        {
            startSlidingInToView();
        } else
        {
            startSlidingOutOfView();
        }
    }

    private String convertToHoursMinutes(int seconds)
    {
        int minutes = (int) (seconds / 60);
        int hours = minutes / 60;
        minutes = minutes - (60 * hours);
        return String.format("%02d:%02d", hours, minutes);
    }

    public void unbindAll()
    {
        if (printer != null)
        {
            printer.printerStatusProperty().removeListener(printerStatusChangeListener);
            printer.pauseStatusProperty().removeListener(pauseStatusChangeListener);
            printer.busyStatusProperty().removeListener(busyStatusChangeListener);
            printer.getPrintEngine().printQueueStatusProperty().removeListener(printQueueStatusChangeListener);
            unbindVariables();
            slideOutOfView();
            printer = null;
        }
    }
}
