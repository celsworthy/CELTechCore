/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.configuration.Filament;
import celtech.printerControl.Printer;
import celtech.printerControl.PrinterStatusEnumeration;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterStatusPageController implements Initializable
{

    private Stenographer steno = StenographerFactory.getStenographer(PrinterStatusPageController.class.getName());
    private StatusScreenState statusScreenState = null;
    private RoboxCommsManager printerCommsManager = RoboxCommsManager.getInstance();
    private Printer printerToUse = null;
    private ChangeListener<Filament> filamentColourListener = null;

    @FXML
    private StackPane statusPane;

    @FXML
    private Button extrudeButton;

    @FXML
    private Rectangle filamentRectangle;

    @FXML
    private Button headBackButton;

    @FXML
    private Button headDownButton;

    @FXML
    private Button headForwardButton;

    @FXML
    private Button headLeftButton;

    @FXML
    private Button headRightButton;

    @FXML
    private Button headUpButton;

    @FXML
    private ImageView printerClosedImage;

    @FXML
    private ImageView printerOpenImage;

    @FXML
    private ImageView printerSilhouette;

    @FXML
    private ImageView reel;

    @FXML
    private ImageView retractArrow;

    @FXML
    private Button retractButton;

    @FXML
    private Button xHomeButton;

    @FXML
    private Button yHomeButton;

    @FXML
    private Button zHomeButton;

    @FXML
    private Rectangle printerColourRectangle;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private VBox progressGroup;

    @FXML
    private Text progressPercent;

    @FXML
    private Text progressTitle;

    @FXML
    private Text progressMessage;

    @FXML
    private HBox printControlButtons;

    @FXML
    private Button pausePrintButton;

    @FXML
    private Button resumePrintButton;

    @FXML
    private Button cancelPrintButton;

    @FXML
    private StackPane pauseResumeStack;

    @FXML
    private Button ejectReelButton;

    @FXML
    private Button unlockLidButton;

    @FXML
    private HBox headXBox;
    @FXML
    private HBox headYBox;
    @FXML
    private HBox headZBox;
    @FXML
    private HBox EBox;
    @FXML
    private HBox BBox;

    @FXML
    private ToggleGroup nozzleSelect;
    @FXML
    private ToggleButton selectNozzle1;
    @FXML
    private ToggleButton selectNozzle2;
    @FXML
    private ToggleButton openNozzleButton;
    @FXML
    private ToggleButton closeNozzleButton;
    @FXML
    private Button homeNozzleButton;

    private Node[] advancedControls = null;

    private double maxHeadMoveMM = 20;
    private double headMoveFactor = Math.sqrt(maxHeadMoveMM);

    private Printer lastSelectedPrinter = null;

    @FXML
    void extrude(MouseEvent event)
    {
        Node node = (Node) event.getSource();
        Bounds bounds = node.getLayoutBounds();
        double moveValue = Math.pow((event.getY() / bounds.getHeight()) * headMoveFactor, 2);
//        steno.info("Move  " + moveValue);

        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.extruderRelativeMoveMode, true);
            printerToUse.transmitDirectGCode(String.format("G1 E%.2f", moveValue), true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send printer control command");
        }
    }

    @FXML
    void retract(MouseEvent event)
    {
        Node node = (Node) event.getSource();
        Bounds bounds = node.getLayoutBounds();
        double moveValue = Math.pow(((bounds.getHeight() - event.getY()) / bounds.getHeight()) * headMoveFactor, 2);
//        steno.info("Move  " + moveValue);

        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.extruderRelativeMoveMode, true);
            printerToUse.transmitDirectGCode(String.format("G1 E-%.2f", moveValue), true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send printer control command");
        }
    }

    @FXML
    void headBack(MouseEvent event)
    {
        Node node = (Node) event.getSource();
        Bounds bounds = node.getLayoutBounds();
        double moveValue = Math.pow(((bounds.getHeight() - event.getY()) / bounds.getHeight()) * headMoveFactor, 2);
//        steno.info("Move  " + moveValue);

        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.carriageRelativeMoveMode, true);
            printerToUse.transmitDirectGCode(String.format("G0 Y-%.2f", moveValue), true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send printer control command");
        }
    }

    @FXML
    void headDown(MouseEvent event)
    {
        Node node = (Node) event.getSource();
        Bounds bounds = node.getLayoutBounds();
        double moveValue = Math.pow((event.getY() / bounds.getHeight()) * headMoveFactor, 2);
//        steno.info("Move  " + moveValue);

        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.carriageRelativeMoveMode, true);
            printerToUse.transmitDirectGCode(String.format("G0 Z-%.2f", moveValue), true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send printer control command");
        }
    }

    @FXML
    void headForward(MouseEvent event)
    {
        Node node = (Node) event.getSource();
        Bounds bounds = node.getLayoutBounds();
        double moveValue = Math.pow((event.getY() / bounds.getHeight()) * headMoveFactor, 2);
//        steno.info("Move  " + moveValue);

        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.carriageRelativeMoveMode, true);
            printerToUse.transmitDirectGCode(String.format("G0 Y%.2f", moveValue), true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send printer control command");
        }
    }

    @FXML
    void headLeft(MouseEvent event)
    {
        Node node = (Node) event.getSource();
        Bounds bounds = node.getLayoutBounds();
        double moveValue = Math.pow(((bounds.getWidth() - event.getX()) / bounds.getWidth()) * headMoveFactor, 2);
//        steno.info("Move  " + moveValue);

        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.carriageRelativeMoveMode, true);
            printerToUse.transmitDirectGCode(String.format("G0 X-%.2f", moveValue), true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send printer control command");
        }
    }

    @FXML
    void headRight(MouseEvent event)
    {
        Node node = (Node) event.getSource();
        Bounds bounds = node.getLayoutBounds();
        double moveValue = Math.pow((event.getX() / bounds.getWidth()) * headMoveFactor, 2);
//        steno.info("Move  " + moveValue);

        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.carriageRelativeMoveMode, true);
            printerToUse.transmitDirectGCode(String.format("G0 X%.2f", moveValue), true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send printer control command");
        }
    }

    @FXML
    void headUp(MouseEvent event)
    {
        Node node = (Node) event.getSource();
        Bounds bounds = node.getLayoutBounds();
        double moveValue = Math.pow(((bounds.getHeight() - event.getY()) / bounds.getHeight()) * headMoveFactor, 2);
//        steno.info("Move  " + moveValue);

        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.carriageRelativeMoveMode, true);
            printerToUse.transmitDirectGCode(String.format("G0 Z%.2f", moveValue), true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send printer control command");
        }
    }

    @FXML
    void closeNozzle(ActionEvent event)
    {
        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.closeNozzle, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send close nozzle");
        }
    }

    @FXML
    void homeNozzle(ActionEvent event)
    {
        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.homeNozzle, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send home nozzle");
        }
    }

    @FXML
    void openNozzle(ActionEvent event)
    {
        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.openNozzle, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Failed to send open nozzle");
        }
    }

    @FXML
    void xHome(MouseEvent event)
    {
        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.homeXAxis, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Couldn't send x home instruction");
        }
    }

    @FXML
    void yHome(MouseEvent event)
    {
        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.homeYAxis, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Couldn't send y home instruction");
        }
    }

    @FXML
    void zHome(MouseEvent event)
    {
        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.homeZAxis, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Couldn't send z home instruction");
        }
    }

    @FXML
    void pausePrint(ActionEvent event)
    {
        printerToUse.pausePrint();
    }

    @FXML
    void resumePrint(ActionEvent event)
    {
        printerToUse.resumePrint();
    }

    @FXML
    void cancelPrint(ActionEvent event)
    {
        printerToUse.abortPrint();
    }

    @FXML
    void ejectReel(ActionEvent event)
    {
        if (printerToUse != null)
        {
            try
            {
                printerToUse.transmitDirectGCode(GCodeConstants.ejectFilament1, false);
            } catch (RoboxCommsException ex)
            {
                steno.error("Error when sending eject filament");
            }
        }
    }

    @FXML
    void unlockLid(ActionEvent event)
    {
        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.carriageAbsoluteMoveMode, false);
            printerToUse.transmitDirectGCode("G0 Z50 Y160", false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error when moving carriage to unlock position");
        }
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        statusScreenState = StatusScreenState.getInstance();

        filamentColourListener = new ChangeListener<Filament>()
        {
            @Override
            public void changed(ObservableValue<? extends Filament> ov, Filament t, Filament t1)
            {
                setFilamentColour(t1);
            }
        };

        printerSilhouette.setVisible(true);
        printerClosedImage.setVisible(false);
        printerOpenImage.setVisible(false);
        printerColourRectangle.setVisible(false);

        progressGroup.setVisible(false);

        printControlButtons.setVisible(false);

        ejectReelButton.setVisible(false);
        unlockLidButton.setVisible(false);

        headXBox.setVisible(false);
        headYBox.setVisible(false);
        headZBox.setVisible(false);
        EBox.setVisible(false);
        BBox.setVisible(false);

        reel.setVisible(false);
        filamentRectangle.setVisible(false);

        if (statusScreenState.getCurrentlySelectedPrinter() != null)
        {
            Printer printer = statusScreenState.getCurrentlySelectedPrinter();
            switch (printer.getPrinterStatus())
            {
                case IDLE:
                case ERROR:
                    printControlButtons.setVisible(false);
                    break;
                case PAUSED:
                    printControlButtons.setVisible(true);
                    cancelPrintButton.setVisible(true);
                    resumePrintButton.setVisible(true);
                    pausePrintButton.setVisible(false);
                    break;
                case PRINTING:
                    printControlButtons.setVisible(true);
                    cancelPrintButton.setVisible(false);
                    resumePrintButton.setVisible(false);
                    pausePrintButton.setVisible(true);
                    break;
                default:
                    printControlButtons.setVisible(false);
                    break;
            }
        }

        statusScreenState.currentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {
            @Override
            public void changed(ObservableValue<? extends Printer> ov, Printer t, Printer selectedPrinter)
            {
                printerToUse = selectedPrinter;

                if (selectedPrinter == null)
                {
                    printerSilhouette.setVisible(true);
                } else
                {
                    printerSilhouette.setVisible(false);
                }

                if (selectedPrinter == null)
                {
                    unbindFromSelectedPrinter();

                    progressGroup.setVisible(false);
                    printerColourRectangle.setVisible(false);
                    pausePrintButton.setVisible(false);
                    resumePrintButton.setVisible(false);
                    printControlButtons.setVisible(false);

                    ejectReelButton.setVisible(false);
                    unlockLidButton.setVisible(false);

                    reel.setVisible(false);
                    filamentRectangle.setVisible(false);
                } else
                {
                    unbindFromSelectedPrinter();

                    progressGroup.visibleProperty().bind(selectedPrinter.getPrintQueue().printInProgressProperty());
                    progressBar.progressProperty().bind(selectedPrinter.getPrintQueue().progressProperty());
                    progressPercent.textProperty().bind(Bindings.multiply(selectedPrinter.getPrintQueue().progressProperty(), 100).asString("%.0f%%"));
                    progressTitle.textProperty().bind(selectedPrinter.getPrintQueue().titleProperty());
                    progressMessage.textProperty().bind(selectedPrinter.getPrintQueue().messageProperty());

                    printerColourRectangle.setVisible(true);
                    printerColourRectangle.fillProperty().bind(selectedPrinter.printerColourProperty());

                    steno.info("Status " + selectedPrinter.getPrinterStatus() + " in progress=" + selectedPrinter.getPrintQueue().printInProgressProperty().getValue());

                    pausePrintButton.visibleProperty().bind(selectedPrinter.printerStatusProperty().isEqualTo(PrinterStatusEnumeration.PRINTING));
                    resumePrintButton.visibleProperty().bind(selectedPrinter.printerStatusProperty().isEqualTo(PrinterStatusEnumeration.PAUSED));
                    cancelPrintButton.visibleProperty().bind(selectedPrinter.printerStatusProperty().isEqualTo(PrinterStatusEnumeration.PAUSED));

                    printControlButtons.visibleProperty().bind(selectedPrinter.getPrintQueue().printInProgressProperty());

                    ejectReelButton.visibleProperty().bind(selectedPrinter.Filament1LoadedProperty().and(selectedPrinter.printerStatusProperty().isNotEqualTo(PrinterStatusEnumeration.PRINTING)));

                    unlockLidButton.visibleProperty().bind(Bindings.not(selectedPrinter.LidOpenProperty()).and(selectedPrinter.bedTemperatureProperty().lessThan(65.0).and(selectedPrinter.extruderTemperatureProperty().lessThan(65.0))));

                    selectedPrinter.loadedFilamentProperty().addListener(filamentColourListener);
                    setFilamentColour(selectedPrinter.loadedFilamentProperty().get());
                    filamentRectangle.visibleProperty().bind(selectedPrinter.reelAttachedProperty());
                    reel.visibleProperty().bind(selectedPrinter.reelAttachedProperty());

                    printerOpenImage.visibleProperty().bind(selectedPrinter.LidOpenProperty());
                    printerClosedImage.visibleProperty().bind(selectedPrinter.LidOpenProperty().not());
                }

                controlAdvancedControlsVisibility();

                lastSelectedPrinter = selectedPrinter;
            }
        });

        advancedControls = new Node[]
        {
            retractButton, extrudeButton,
            headForwardButton, headBackButton, headLeftButton, headRightButton, headUpButton, headDownButton,
            xHomeButton, yHomeButton, zHomeButton,
            openNozzleButton, closeNozzleButton, homeNozzleButton, selectNozzle1, selectNozzle2,
            headXBox, headYBox, headZBox, EBox, BBox
        };

        controlAdvancedControlsVisibility();

        statusScreenState.modeProperty().addListener(new ChangeListener<StatusScreenMode>()
        {

            @Override
            public void changed(ObservableValue<? extends StatusScreenMode> ov, StatusScreenMode oldMode, StatusScreenMode newMode)
            {
                controlAdvancedControlsVisibility();
            }
        });

        selectNozzle1.setUserData(0);
        selectNozzle2.setUserData(1);

        nozzleSelect.selectedToggleProperty().addListener(new ChangeListener<Toggle>()
        {
            @Override
            public void changed(ObservableValue<? extends Toggle> ov, Toggle t, Toggle t1)
            {

                if (t1 != null)
                {
                    selectNozzle((int) t1.getUserData());
                }
            }
        });
    }

    public void configure(VBox parent)
    {

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
        printerClosedImage.fitWidthProperty().bind(parent.widthProperty());
        printerClosedImage.fitHeightProperty().bind(parent.heightProperty());
        printerOpenImage.fitWidthProperty().bind(parent.widthProperty());
        printerOpenImage.fitHeightProperty().bind(parent.heightProperty());
        printerSilhouette.fitWidthProperty().bind(parent.widthProperty());
        printerSilhouette.fitHeightProperty().bind(parent.heightProperty());
    }

    private void selectNozzle(int nozzleNumber)
    {
        try
        {
            printerToUse.transmitDirectGCode(GCodeConstants.selectNozzle + nozzleNumber, true);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error selecting nozzle");
        }
    }

    private void unbindFromSelectedPrinter()
    {
        progressGroup.visibleProperty().unbind();
        progressBar.progressProperty().unbind();
        progressPercent.textProperty().unbind();
        progressTitle.textProperty().unbind();
        progressMessage.textProperty().unbind();
        printerColourRectangle.fillProperty().unbind();
        pausePrintButton.visibleProperty().unbind();
        resumePrintButton.visibleProperty().unbind();
        cancelPrintButton.visibleProperty().unbind();
        printControlButtons.visibleProperty().unbind();

        reel.visibleProperty().unbind();
        if (lastSelectedPrinter != null)
        {
            lastSelectedPrinter.loadedFilamentProperty().removeListener(filamentColourListener);
        }

        filamentRectangle.visibleProperty().unbind();

        ejectReelButton.visibleProperty().unbind();
        ejectReelButton.setVisible(false);
        unlockLidButton.visibleProperty().unbind();
        unlockLidButton.setVisible(false);

        printerOpenImage.visibleProperty().unbind();
        printerOpenImage.setVisible(false);
        printerClosedImage.visibleProperty().unbind();
        printerClosedImage.setVisible(false);
    }

    private void controlAdvancedControlsVisibility()
    {
        boolean visible = false;

        if (statusScreenState.getCurrentlySelectedPrinter() != null)
        {
            visible = statusScreenState.getMode() == StatusScreenMode.ADVANCED && (statusScreenState.getCurrentlySelectedPrinter().getPrinterStatus() == PrinterStatusEnumeration.IDLE
                    || statusScreenState.getCurrentlySelectedPrinter().getPrinterStatus() == PrinterStatusEnumeration.PAUSED);
        }
        for (Node node : advancedControls)
        {
            node.setVisible(visible);
        }
    }

    private void setFilamentColour(Filament filament)
    {
        if (filament != null)
        {
            filamentRectangle.setFill(filament.getDisplayColour());
        }
    }
}
