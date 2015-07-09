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
import celtech.printerControl.model.PrinterException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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

    private final EventHandler<ActionEvent> pauseEventHandler = new EventHandler<ActionEvent>()
    {
        @Override
        public void handle(ActionEvent t)
        {
            try
            {
                printer.pause();
            } catch (PrinterException ex)
            {
                System.out.println("Couldn't pause printer");
            }
        }
    };

    private final EventHandler<ActionEvent> resumeEventHandler = new EventHandler<ActionEvent>()
    {
        @Override
        public void handle(ActionEvent t)
        {
            try
            {
                printer.resume();
            } catch (PrinterException ex)
            {
                System.out.println("Couldn't resume print");
            }
        }
    };

    private final EventHandler<ActionEvent> cancelEventHandler = new EventHandler<ActionEvent>()
    {
        @Override
        public void handle(ActionEvent t)
        {
            try
            {
                printer.cancel(null);
            } catch (PrinterException ex)
            {
                System.out.println("Couldn't resume print");
            }
        }
    };

    private final BooleanProperty buttonsAllowed = new SimpleBooleanProperty(false);

    public PrintStatusBar(Printer printer)
    {
        super();
        this.printer = printer;

        printer.printerStatusProperty().addListener(printerStatusChangeListener);
        printer.pauseStatusProperty().addListener(pauseStatusChangeListener);
        printer.busyStatusProperty().addListener(busyStatusChangeListener);
        printer.getPrintEngine().printQueueStatusProperty().addListener(printQueueStatusChangeListener);

        pauseButton.visibleProperty().bind(printer.canPauseProperty().and(buttonsAllowed));
        pauseButton.setOnAction(pauseEventHandler);
        resumeButton.visibleProperty().bind(printer.canResumeProperty().and(buttonsAllowed));
        resumeButton.setOnAction(resumeEventHandler);
        cancelButton.visibleProperty().bind(printer.canCancelProperty().and(buttonsAllowed));
        cancelButton.setOnAction(cancelEventHandler);

        reassessStatus();
    }

    private void reassessStatus()
    {
        boolean statusProcessed = false;
        boolean barShouldBeDisplayed = false;

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
                progressRequired(false);
                targetRequired(false);
                buttonsAllowed.set(false);
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
                    progressRequired(false);
                    targetRequired(false);
                    buttonsAllowed.set(true);
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
                        int secondsRemaining = printer.getPrintEngine().progressETCProperty().intValue();
                        secondsRemaining += 30;
                        if (secondsRemaining > 60)
                        {
                            String hoursMinutes = convertToHoursMinutes(
                                    secondsRemaining);
                            largeTargetValue.setText(hoursMinutes);
                        } else
                        {
                            largeTargetValue.setText(Lookup.i18n("dialogs.lessThanOneMinute"));
                        }

                        largeTargetLegend.setText(Lookup.i18n("dialogs.progressETCLabel"));
                        targetRequired(true);
                    } else
                    {
                        targetRequired(false);
                    }

                    progressBar.progressProperty().bind(printer.getPrintEngine().progressProperty());
                    progressRequired(true);
                    buttonsAllowed.set(true);
                    break;
                case RUNNING_MACRO_FILE:
                    statusProcessed = true;
                    barShouldBeDisplayed = true;
                    largeProgressDescription.setText(printer.getPrintEngine().macroBeingRun.get().getFriendlyName());

                    targetRequired(false);

                    if (printer.getPrintEngine().macroBeingRun.get() != Macro.CANCEL_PRINT)
                    {
                        if (printer.getPrintEngine().linesInPrintingFileProperty().get() > 0)
                        {
                            progressBar.setProgress(printer.getPrintEngine().progressProperty().get());
                            progressRequired(true);
                        } else
                        {
                            progressRequired(false);
                        }
                    } else
                    {
                        progressRequired(false);
                    }
                    buttonsAllowed.set(false);
                    break;
                case CALIBRATING_NOZZLE_ALIGNMENT:
                case CALIBRATING_NOZZLE_OPENING:
                case CALIBRATING_NOZZLE_HEIGHT:
                    statusProcessed = true;
//                    barShouldBeDisplayed = true;
//                    largeProgressDescription.setText(printer.printerStatusProperty().get().getI18nString());
//
//                    targetRequired(false);
//
//                    if (printer.getPrintEngine().printQueueStatusProperty().get() == PrintQueueStatus.PRINTING)
//                    {
//                        if (printer.getPrintEngine().linesInPrintingFileProperty().get() > 0)
//                        {
//                            progressBar.setProgress(printer.getPrintEngine().progressProperty().get());
//                            progressRequired(true);
//                        } else
//                        {
//                            progressRequired(false);
//                        }
//                    } else
//                    {
//                        progressRequired(false);
//                    }
//                    buttonsAllowed.set(false);
                    break;
                case PURGING_HEAD:
                    statusProcessed = true;
//                    barShouldBeDisplayed = true;
//                    largeProgressDescription.setText(printer.printerStatusProperty().get().getI18nString());
//
//                    targetRequired(false);
//
//                    if (printer.getPrintEngine().printQueueStatusProperty().get() == PrintQueueStatus.RUNNING_MACRO
//                            && printer.getPrintEngine().macroBeingRun.get() == Macro.PURGE)
//                    {
//                        if (printer.getPrintEngine().linesInPrintingFileProperty().get() > 0)
//                        {
//                            progressBar.setProgress(printer.getPrintEngine().progressProperty().get());
//                            progressRequired(true);
//                        } else
//                        {
//                            progressRequired(false);
//                        }
//                    } else
//                    {
//                        progressRequired(false);
//                    }
//                    buttonsAllowed.set(false);
                    break;
                default:
                    statusProcessed = true;
                    barShouldBeDisplayed = true;
                    targetRequired(false);
                    progressRequired(false);
                    largeProgressDescription.setText(printer.printerStatusProperty().get().getI18nString());
                    buttonsAllowed.set(false);
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
            pauseButton.visibleProperty().unbind();
            pauseButton.setOnAction(null);
            resumeButton.visibleProperty().unbind();
            resumeButton.setOnAction(null);
            cancelButton.visibleProperty().unbind();
            cancelButton.setOnAction(null);
            printer = null;
        }
    }
}
