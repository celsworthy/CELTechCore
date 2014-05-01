/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.JogButton;
import celtech.printerControl.Printer;
import celtech.printerControl.PrinterStatusEnumeration;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.utils.AxisSpecifier;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.dialog.Dialogs.CommandLink;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterStatusPageController implements Initializable {

    private Stenographer steno = StenographerFactory.getStenographer(PrinterStatusPageController.class.getName());
    private StatusScreenState statusScreenState = null;
    private RoboxCommsManager printerCommsManager = RoboxCommsManager.getInstance();
    private Printer printerToUse = null;
    private ChangeListener<Boolean> reelDataChangeListener = null;
    private ChangeListener<EEPROMState> reelChangeListener = null;

    private CommandLink goAheadAndOpenTheLid = null;
    private CommandLink dontOpenTheLid = null;
    private String openLidPrinterTooHotTitle = null;
    private String openLidPrinterTooHotInfo = null;
    
    private String transferringDataString = null;

    @FXML
    private AnchorPane container;

    @FXML
    private ImageView printerSilhouette;

    @FXML
    private Button headLEDButton;

    @FXML
    private Button ambientLEDButton;

    @FXML
    private Button xHomeButton;

    @FXML
    private JogButton x_minus1;

    @FXML
    private JogButton x_minus100;

    @FXML
    private JogButton z_plus0_1;

    @FXML
    private JogButton z_plus10;

    @FXML
    private JogButton z_minus0_1;

    @FXML
    private JogButton y_plus10;

    @FXML
    private JogButton extruder_minus20;

    @FXML
    private JogButton z_minus1;

    @FXML
    private JogButton x_plus100;

    @FXML
    private JogButton y_minus1;

    @FXML
    private Button yHomeButton;

    @FXML
    private Button zHomeButton;

    @FXML
    private JogButton x_plus10;

    @FXML
    private JogButton y_minus10;

    @FXML
    private ImageView printerOpenImage;

    @FXML
    private Button ejectReelButton;

    @FXML
    private ImageView reel;

    @FXML
    private JogButton x_plus1;

    @FXML
    private Button pausePrintButton;

    @FXML
    private JogButton extruder_plus100;

    @FXML
    private Rectangle printerColourRectangle;

    @FXML
    private Button selectNozzle2;

    @FXML
    private Button openNozzleButton;

    @FXML
    private StackPane statusPane;

    @FXML
    private Button selectNozzle1;

    @FXML
    private JogButton extruder_minus5;

    @FXML
    private Button headFanButton;

    @FXML
    private JogButton y_plus100;

    @FXML
    private JogButton y_minus100;

    @FXML
    private Text progressTitle;

    @FXML
    private Button resumePrintButton;

    @FXML
    private JogButton extruder_minus100;

    @FXML
    private StackPane pauseResumeStack;

    @FXML
    private Button unlockLidButton;

    @FXML
    private JogButton x_minus10;

    @FXML
    private Button closeNozzleButton;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ProgressBar secondProgressBar;

    @FXML
    private JogButton y_plus1;

    @FXML
    private Text progressPercent;

    @FXML
    private Text secondProgressPercent;

    @FXML
    private Button cancelPrintButton;

    @FXML
    private JogButton extruder_plus20;

    @FXML
    private HBox printControlButtons;

    @FXML
    private JogButton z_plus1;

    @FXML
    private Rectangle filamentRectangle;

    @FXML
    private JogButton extruder_plus5;

    @FXML
    private ImageView printerClosedImage;

    @FXML
    private VBox progressGroup;

    @FXML
    private JogButton z_minus10;

    @FXML
    private Text progressMessage;

    @FXML
    private Text temperatureWarning;

    private Node[] advancedControls = null;
    private BooleanProperty advancedControlsVisible = new SimpleBooleanProperty(false);

    private Printer lastSelectedPrinter = null;

    @FXML
    void homeX(ActionEvent event) {
        try {
            printerToUse.transmitDirectGCode(GCodeConstants.homeXAxis, true);
        } catch (RoboxCommsException ex) {
            steno.error("Couldn't send x home instruction");
        }
    }

    @FXML
    void homeY(ActionEvent event) {
        try {
            printerToUse.transmitDirectGCode(GCodeConstants.homeYAxis, true);
        } catch (RoboxCommsException ex) {
            steno.error("Couldn't send y home instruction");
        }
    }

    @FXML
    void homeZ(ActionEvent event) {
        try {
            printerToUse.transmitDirectGCode(GCodeConstants.homeZAxis, true);
        } catch (RoboxCommsException ex) {
            steno.error("Couldn't send z home instruction");
        }
    }

    @FXML
    void pausePrint(ActionEvent event) {
        printerToUse.getPrintQueue().pausePrint();
    }

    @FXML
    void resumePrint(ActionEvent event) {
        printerToUse.getPrintQueue().resumePrint();
    }

    @FXML
    void cancelPrint(ActionEvent event) {
        printerToUse.getPrintQueue().abortPrint();
    }

    @FXML
    void ejectReel(ActionEvent event) {
        if (printerToUse != null) {
            try {
                printerToUse.transmitDirectGCode(GCodeConstants.ejectFilament1, false);
            } catch (RoboxCommsException ex) {
                steno.error("Error when sending eject filament");
            }
        }
    }

    @FXML
    void unlockLid(ActionEvent event) {
        boolean openTheLid = true;

        if (printerToUse.bedTemperatureProperty().get() > 60) {
            Action tooBigResponse = Dialogs.create().title(openLidPrinterTooHotTitle)
                    .message(openLidPrinterTooHotInfo)
                    .masthead(null)
                    .showCommandLinks(dontOpenTheLid, dontOpenTheLid, goAheadAndOpenTheLid);

            if (tooBigResponse != goAheadAndOpenTheLid) {
                openTheLid = false;
            }
        }

        if (openTheLid) {
            if (printerToUse.getPrintQueue().printInProgressProperty().get() == true) {
                printerToUse.getPrintQueue().abortPrint();
            }
            try {
                printerToUse.transmitDirectGCode(GCodeConstants.goToOpenLidPosition, false);
            } catch (RoboxCommsException ex) {
                steno.error("Error when moving sending open lid command");
            }
        }
    }

    @FXML
    void jogButton(ActionEvent event) {
        JogButton button = (JogButton) event.getSource();
        AxisSpecifier axis = button.getAxis();
        float distance = button.getDistance();

        try {
            printerToUse.transmitDirectGCode(GCodeConstants.carriageRelativeMoveMode, true);
            printerToUse.transmitDirectGCode(String.format("G0 " + axis.name() + "%.2f", distance), true);
        } catch (RoboxCommsException ex) {
            steno.error("Failed to send printer jog command");
        }
    }

    boolean headLEDOn = false;

    @FXML
    void toggleHeadLED(ActionEvent event) {
        try {
            if (headLEDOn == true) {
                printerToUse.transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, true);
                headLEDOn = false;
            } else {
                printerToUse.transmitDirectGCode(GCodeConstants.switchOnHeadLEDs, true);
                headLEDOn = true;
            }
        } catch (RoboxCommsException ex) {
            steno.error("Failed to send head LED command");
        }
    }

    @FXML
    void toggleHeadFan(ActionEvent event) {
        try {
            if (printerToUse.getHeadFanOn()) {
                printerToUse.transmitDirectGCode(GCodeConstants.switchOffHeadFan, true);
            } else {
                printerToUse.transmitDirectGCode(GCodeConstants.switchOnHeadFan, true);
            }
        } catch (RoboxCommsException ex) {
            steno.error("Failed to send head fan command");
        }
    }

    boolean ambientLEDOn = true;

    @FXML
    void toggleAmbientLED(ActionEvent event) {
        try {
            if (ambientLEDOn == true) {
                printerToUse.transmitSetAmbientLEDColour(Color.BLACK);
                ambientLEDOn = false;
            } else {
                printerToUse.transmitSetAmbientLEDColour(printerToUse.getPrinterColour());
                ambientLEDOn = true;
            }
        } catch (RoboxCommsException ex) {
            steno.error("Failed to send ambient LED command");
        }
    }

    @FXML
    void selectNozzle1(ActionEvent event) {
        selectNozzle(0);
    }

    @FXML
    void selectNozzle2(ActionEvent event) {
        selectNozzle(1);
    }

    @FXML
    void openNozzle(ActionEvent event) {
        try {
            printerToUse.transmitDirectGCode(GCodeConstants.openNozzle, true);
        } catch (RoboxCommsException ex) {
            steno.error("Failed to send open nozzle");
        }
    }

    @FXML
    void closeNozzle(ActionEvent event) {
        try {
            printerToUse.transmitDirectGCode(GCodeConstants.closeNozzle, true);
        } catch (RoboxCommsException ex) {
            steno.error("Failed to send close nozzle");
        }
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusScreenState = StatusScreenState.getInstance();

        ResourceBundle i18nBundle = DisplayManager.getLanguageBundle();
        
        transferringDataString = i18nBundle.getString("PrintQueue.SendingToPrinter");

        goAheadAndOpenTheLid = new Dialogs.CommandLink(i18nBundle.getString("dialogs.openLidPrinterHotGoAheadHeading"), i18nBundle.getString("dialogs.openLidPrinterHotGoAheadInfo"));
        dontOpenTheLid = new Dialogs.CommandLink(i18nBundle.getString("dialogs.openLidPrinterHotDontOpenHeading"), null);
        openLidPrinterTooHotTitle = i18nBundle.getString("dialogs.openLidPrinterHotTitle");
        openLidPrinterTooHotInfo = i18nBundle.getString("dialogs.openLidPrinterHotInfo");

        reelDataChangeListener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                setFilamentColour(printerToUse.loadedFilamentProperty().get());
            }
        };

        reelChangeListener = new ChangeListener<EEPROMState>() {
            @Override
            public void changed(ObservableValue<? extends EEPROMState> ov, EEPROMState t, EEPROMState t1) {
                if (t1 == EEPROMState.PROGRAMMED) {
                    setFilamentColour(lastSelectedPrinter.loadedFilamentProperty().get());
                }
            }
        };

        printerSilhouette.setVisible(true);
        printerClosedImage.setVisible(false);
        printerOpenImage.setVisible(false);
        printerColourRectangle.setVisible(false);

        progressGroup.setVisible(false);
        cancelPrintButton.setVisible(false);
        pausePrintButton.setVisible(false);
        resumePrintButton.setVisible(false);

        ejectReelButton.setVisible(false);
        unlockLidButton.setVisible(false);
        temperatureWarning.setVisible(false);

        reel.setVisible(false);
        filamentRectangle.setVisible(false);

        if (statusScreenState.getCurrentlySelectedPrinter() != null) {
            Printer printer = statusScreenState.getCurrentlySelectedPrinter();
            switch (printer.getPrinterStatus()) {
                case IDLE:
                case ERROR:
                    break;
                case PAUSED:
                    cancelPrintButton.setVisible(true);
                    resumePrintButton.setVisible(true);
                    pausePrintButton.setVisible(false);
                    break;
                case SENDING_TO_PRINTER:
                case PRINTING:
                    cancelPrintButton.setVisible(false);
                    resumePrintButton.setVisible(false);
                    pausePrintButton.setVisible(true);
                    break;
                default:
                    break;
            }
        }

        statusScreenState.currentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>() {
            @Override
            public void changed(ObservableValue<? extends Printer> ov, Printer t, Printer selectedPrinter) {
                printerToUse = selectedPrinter;

                if (selectedPrinter == null) {
                    printerSilhouette.setVisible(true);
                } else {
                    printerSilhouette.setVisible(false);
                }

                if (selectedPrinter == null) {
                    unbindFromSelectedPrinter();

                    progressGroup.setVisible(false);
                    printerColourRectangle.setVisible(false);
                    pausePrintButton.setVisible(false);
                    resumePrintButton.setVisible(false);

                    ejectReelButton.setVisible(false);
                    unlockLidButton.setVisible(false);
                    temperatureWarning.setVisible(false);

                    reel.setVisible(false);
                    filamentRectangle.setVisible(false);
                } else {
                    unbindFromSelectedPrinter();

                    progressGroup.visibleProperty().bind(selectedPrinter.getPrintQueue().printInProgressProperty());
                    progressBar.progressProperty().bind(selectedPrinter.getPrintQueue().progressProperty());
                    progressPercent.textProperty().bind(Bindings.multiply(selectedPrinter.getPrintQueue().progressProperty(), 100).asString("%.0f%%"));
                    progressPercent.visibleProperty().bind(selectedPrinter.printerStatusProperty().isNotEqualTo(PrinterStatusEnumeration.PRINTING)
                            .or(Bindings.and(selectedPrinter.getPrintQueue().linesInPrintingFileProperty().greaterThan(0), selectedPrinter.getPrintQueue().progressProperty().greaterThanOrEqualTo(0))));
                    secondProgressBar.visibleProperty().bind(selectedPrinter.getPrintQueue().sendingDataToPrinterProperty());
                    secondProgressBar.progressProperty().bind(selectedPrinter.getPrintQueue().secondaryProgressProperty());
                    secondProgressPercent.visibleProperty().bind(selectedPrinter.getPrintQueue().sendingDataToPrinterProperty());
                    secondProgressPercent.textProperty().bind(Bindings.format("%s %.0f%%", transferringDataString, Bindings.multiply(selectedPrinter.getPrintQueue().secondaryProgressProperty(), 100)));
                    progressTitle.textProperty().bind(selectedPrinter.getPrintQueue().titleProperty());
                    progressMessage.textProperty().bind(selectedPrinter.getPrintQueue().messageProperty());

                    printerColourRectangle.setVisible(true);
                    printerColourRectangle.fillProperty().bind(selectedPrinter.printerColourProperty());

//                    steno.info("Status " + selectedPrinter.getPrinterStatus() + " in progress=" + selectedPrinter.getPrintQueue().printInProgressProperty().getValue());
                    pausePrintButton.visibleProperty().bind(selectedPrinter.printerStatusProperty().isEqualTo(PrinterStatusEnumeration.PRINTING).or(selectedPrinter.printerStatusProperty().isEqualTo(PrinterStatusEnumeration.SENDING_TO_PRINTER)));
                    resumePrintButton.visibleProperty().bind(selectedPrinter.printerStatusProperty().isEqualTo(PrinterStatusEnumeration.PAUSED));
                    cancelPrintButton.visibleProperty().bind(selectedPrinter.printerStatusProperty().isEqualTo(PrinterStatusEnumeration.PAUSED).or(selectedPrinter.printerStatusProperty().isEqualTo(PrinterStatusEnumeration.SLICING)));

                    ejectReelButton.visibleProperty().bind(selectedPrinter.Filament1LoadedProperty().and(selectedPrinter.printerStatusProperty().isNotEqualTo(PrinterStatusEnumeration.PRINTING)));

                    unlockLidButton.setVisible(true);
                    unlockLidButton.disableProperty().bind(selectedPrinter.LidOpenProperty());
                    temperatureWarning.visibleProperty().bind(selectedPrinter.bedTemperatureProperty().greaterThan(ApplicationConfiguration.bedHotAboveDegrees));

                    selectedPrinter.reelDataChangedProperty().addListener(reelDataChangeListener);
                    selectedPrinter.reelEEPROMStatusProperty().addListener(reelChangeListener);
                    setFilamentColour(selectedPrinter.loadedFilamentProperty().get());
                    filamentRectangle.visibleProperty().bind(selectedPrinter.reelEEPROMStatusProperty().isEqualTo(EEPROMState.PROGRAMMED));
                    reel.visibleProperty().bind(selectedPrinter.reelEEPROMStatusProperty().isEqualTo(EEPROMState.PROGRAMMED));

                    printerOpenImage.visibleProperty().bind(selectedPrinter.LidOpenProperty());
                    printerClosedImage.visibleProperty().bind(selectedPrinter.LidOpenProperty().not());

                    advancedControlsVisible.unbind();
                    advancedControlsVisible.bind(selectedPrinter.printerStatusProperty().isEqualTo(PrinterStatusEnumeration.IDLE).or(selectedPrinter.printerStatusProperty().isEqualTo(PrinterStatusEnumeration.PAUSED)));
                }

                lastSelectedPrinter = selectedPrinter;
            }
        });

        advancedControls = new Node[]{
            extruder_minus100, extruder_minus20, extruder_minus5, extruder_plus100, extruder_plus20, extruder_plus5,
            xHomeButton, x_minus1, x_minus10, x_minus100, x_plus1, x_plus10, x_plus100,
            yHomeButton, y_minus1, y_minus10, y_minus100, y_plus1, y_plus10, y_plus100,
            zHomeButton, z_minus0_1, z_minus1, z_minus10, z_plus0_1, z_plus1, z_plus10,
            openNozzleButton, closeNozzleButton, selectNozzle1, selectNozzle2,
            ambientLEDButton,
            headFanButton, headLEDButton
        };

        for (Node node : advancedControls) {
            node.setVisible(false);
        }

        if (statusScreenState.getCurrentlySelectedPrinter() != null) {
            boolean visible = (statusScreenState.getCurrentlySelectedPrinter().getPrinterStatus() == PrinterStatusEnumeration.IDLE
                    || statusScreenState.getCurrentlySelectedPrinter().getPrinterStatus() == PrinterStatusEnumeration.PAUSED);
            for (Node node : advancedControls) {
                node.setVisible(visible);
            }
        }

        advancedControlsVisible.addListener(
                new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue
                    ) {
                        for (Node node : advancedControls) {
                            node.setVisible(newValue);
                        }
                    }
                }
        );
    }

    public void configure(VBox parent) {

//        parent.widthProperty().addListener(new ChangeListener<Number>()
//        {
//
//            @Override
//            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
//            {
//                steno.info("Width is " + t1.doubleValue());
//            }
//        });
//
//        parent.heightProperty().addListener(new ChangeListener<Number>()
//        {
//
//            @Override
//            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
//            {
//                steno.info("Height is " + t1.doubleValue());
//            }
//        });
//        printerClosedImage.fitWidthProperty().bind(parent.widthProperty());
//        printerClosedImage.fitHeightProperty().bind(parent.heightProperty());
//        printerOpenImage.fitWidthProperty().bind(parent.widthProperty());
//        printerOpenImage.fitHeightProperty().bind(parent.heightProperty());
//        printerSilhouette.fitWidthProperty().bind(parent.widthProperty());
//        printerSilhouette.fitHeightProperty().bind(parent.heightProperty());
        parent.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                resizePrinterDisplay(parent);
            }
        });
        parent.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                resizePrinterDisplay(parent);
            }
        });
    }

    private void resizePrinterDisplay(VBox parent) {
        final double beginWidth = 750;
        final double beginHeight = 660;
        final double aspect = beginWidth / beginHeight;

        double displayAspect = parent.getWidth() / parent.getHeight();

        double newWidth = 0;
        double newHeight = 0;

        if (displayAspect >= aspect) {
            // Drive from height
            newWidth = parent.getHeight() * aspect;
            newHeight = parent.getHeight();
        } else {
            //Drive from width
            newHeight = parent.getWidth() / aspect;
            newWidth = parent.getWidth();
        }

        double xScale = Double.min((newWidth / beginWidth), 1.0);
        double yScale = Double.min((newHeight / beginHeight), 1.0);

        statusPane.setScaleX(xScale);
        statusPane.setScaleY(yScale);
    }

    private void selectNozzle(int nozzleNumber) {
        try {
            printerToUse.transmitDirectGCode(GCodeConstants.selectNozzle + nozzleNumber, true);
        } catch (RoboxCommsException ex) {
            steno.error("Error selecting nozzle");
        }
    }

    private void unbindFromSelectedPrinter() {
        progressGroup.visibleProperty().unbind();
        progressBar.progressProperty().unbind();
        progressPercent.textProperty().unbind();
        progressTitle.textProperty().unbind();
        progressMessage.textProperty().unbind();
        secondProgressBar.visibleProperty().unbind();
        secondProgressBar.progressProperty().unbind();
        secondProgressPercent.textProperty().unbind();
        printerColourRectangle.fillProperty().unbind();
        pausePrintButton.visibleProperty().unbind();
        resumePrintButton.visibleProperty().unbind();
        cancelPrintButton.visibleProperty().unbind();
//        printControlButtons.visibleProperty().unbind();

        reel.visibleProperty().unbind();
        if (lastSelectedPrinter != null) {
            lastSelectedPrinter.reelDataChangedProperty().removeListener(reelDataChangeListener);
            lastSelectedPrinter.reelEEPROMStatusProperty().removeListener(reelChangeListener);
        }

        filamentRectangle.visibleProperty().unbind();

        ejectReelButton.visibleProperty().unbind();
        ejectReelButton.setVisible(false);
        unlockLidButton.setVisible(false);
        temperatureWarning.visibleProperty().unbind();
        temperatureWarning.setVisible(false);

        printerOpenImage.visibleProperty().unbind();
        printerOpenImage.setVisible(false);
        printerClosedImage.visibleProperty().unbind();
        printerClosedImage.setVisible(false);
    }

    private void setFilamentColour(Filament filament) {
        if (filament != null) {
            filamentRectangle.setFill(filament.getDisplayColour());
        }
    }
}
