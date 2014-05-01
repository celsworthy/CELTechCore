/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.utilityPanels;

import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.ProgressDialog;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.services.printing.GCodePrintService;
import celtech.utils.SystemUtils;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class GCodeMacroPanelController 
{

    private final Stenographer steno = StenographerFactory.getStenographer(GCodeMacroPanelController.class.getName());
    private Printer connectedPrinter = null;

    @FXML
    private AnchorPane container;

//    @FXML
//    void levelGantry(ActionEvent event)
//    {
//        try
//        {
//            connectedPrinter.transmitStoredGCode(GCodeMacros.LEVEL_GANTRY, true);
//        } catch (RoboxCommsException ex)
//        {
//            steno.error("Error sending level gantry commands");
//        }
//    }
//
//    @FXML
//    void eject_ABS(ActionEvent event)
//    {
//        try
//        {
//            connectedPrinter.transmitStoredGCode(GCodeMacros.EJECT_ABS, true);
//        } catch (RoboxCommsException ex)
//        {
//            steno.error("Error sending eject ABS commands");
//        }
//    }
//
//    @FXML
//    void eject_PLA(ActionEvent event)
//    {
//        try
//        {
//            connectedPrinter.transmitStoredGCode(GCodeMacros.EJECT_PLA, true);
//        } catch (RoboxCommsException ex)
//        {
//            steno.error("Error sending eject PLA commands");
//        }
//    }
//
//    @FXML
//    void preheat_ABS(ActionEvent event)
//    {
//        try
//        {
//            connectedPrinter.transmitStoredGCode(GCodeMacros.PREHEAT_ABS, true);
//        } catch (RoboxCommsException ex)
//        {
//            steno.error("Error sending preheat ABS commands");
//        }
//    }
//
//    @FXML
//    void preheat_PLA(ActionEvent event)
//    {
//        try
//        {
//            connectedPrinter.transmitStoredGCode(GCodeMacros.PREHEAT_PLA, true);
//        } catch (RoboxCommsException ex)
//        {
//            steno.error("Error sending preheat PLA commands");
//        }
//    }
//
//    @FXML
//    void level_Y(ActionEvent event)
//    {
//        try
//        {
//            connectedPrinter.transmitStoredGCode(GCodeMacros.LEVEL_Y, true);
//        } catch (RoboxCommsException ex)
//        {
//            steno.error("Error sending level Y commands");
//        }
//    }
//
//    @FXML
//    void twelve_point_bed_check(ActionEvent event)
//    {
//        try
//        {
//            connectedPrinter.transmitStoredGCode(GCodeMacros.TWELVE_POINT_BED_CHECK, true);
//        } catch (RoboxCommsException ex)
//        {
//            steno.error("Error sending 12 point bed check commands");
//        }
//    }
//
//    @FXML
//    void x_test(ActionEvent event)
//    {
//        try
//        {
//            connectedPrinter.transmitStoredGCode(GCodeMacros.X_TEST, true);
//        } catch (RoboxCommsException ex)
//        {
//            steno.error("Error sending x test commands");
//        }
//    }
//
//    @FXML
//    void y_test(ActionEvent event)
//    {
//        try
//        {
//            connectedPrinter.transmitStoredGCode(GCodeMacros.Y_TEST, true);
//        } catch (RoboxCommsException ex)
//        {
//            steno.error("Error sending y test commands");
//        }
//    }
//
//    @FXML
//    void z_test(ActionEvent event)
//    {
//        try
//        {
//            connectedPrinter.transmitStoredGCode(GCodeMacros.Z_TEST, true);
//        } catch (RoboxCommsException ex)
//        {
//            steno.error("Error sending z test commands");
//        }
//    }
//
//    @FXML
//    void speed_test(ActionEvent event)
//    {
//        try
//        {
//            connectedPrinter.transmitStoredGCode(GCodeMacros.SPEED_TEST, true);
//        } catch (RoboxCommsException ex)
//        {
//            steno.error("Error sending speed test commands");
//        }
//    }
//
//    private ModalDialog generalPurposeDialog = null;
//    private ProgressDialog gcodeUpdateProgress = null;
//    private FileChooser gcodeFileChooser = new FileChooser();
//    private File lastGCodeDirectory = null;
//    private final GCodePrintService gcodePrintService = new GCodePrintService();
//
//    @FXML
//    void sendGCodeStream(ActionEvent event)
//    {
//        gcodeFileChooser.setInitialFileName("Untitled");
//
//        gcodeFileChooser.setInitialDirectory(lastGCodeDirectory);
//
//        final File file = gcodeFileChooser.showOpenDialog(container.getScene().getWindow());
//        if (file != null)
//        {
//            gcodePrintService.reset();
//            gcodePrintService.setPrintUsingSDCard(false);
//            gcodePrintService.setPrinterToUse(connectedPrinter);
//            gcodePrintService.setModelFileToPrint(file.getAbsolutePath());
//            gcodePrintService.start();
//            lastGCodeDirectory = file.getParentFile();
//        }
//    }
//
//    @FXML
//    void sendGCodeSD(ActionEvent event)
//    {
//        gcodeFileChooser.setInitialFileName("Untitled");
//
//        gcodeFileChooser.setInitialDirectory(lastGCodeDirectory);
//
//        final File file = gcodeFileChooser.showOpenDialog(container.getScene().getWindow());
//
//        if (file != null)
//        {
//            gcodePrintService.reset();
//            gcodePrintService.setPrintUsingSDCard(true);
//            gcodePrintService.setPrinterToUse(connectedPrinter);
//            gcodePrintService.setCurrentPrintJobID(SystemUtils.generate16DigitID());
//            gcodePrintService.setModelFileToPrint(file.getAbsolutePath());
//            gcodePrintService.start();
//            lastGCodeDirectory = file.getParentFile();
//        }
//    }
//
//    /**
//     * Initializes the controller class.
//     */
//    @Override
//    public void initialize(URL url, ResourceBundle rb)
//    {
//        Platform.runLater(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                generalPurposeDialog = new ModalDialog();
//
//                generalPurposeDialog.addButton(DisplayManager.getLanguageBundle().getString("genericFirstLetterCapitalised.Ok"));
//
//                gcodeUpdateProgress = new ProgressDialog(gcodePrintService);
//            }
//        });
//        gcodeFileChooser.setTitle(DisplayManager.getLanguageBundle().getString("gcodeMacroPanel.fileChooserTitle"));
//        gcodeFileChooser.getExtensionFilters()
//                .addAll(
//                        new FileChooser.ExtensionFilter(DisplayManager.getLanguageBundle().getString("gcodeMacroPanel.gcodeFileDescription"), "*.gcode"));
//
//        lastGCodeDirectory = new File(ApplicationConfiguration.getProjectDirectory());
//
//        gcodePrintService.setOnSucceeded(new EventHandler<WorkerStateEvent>()
//        {
//            @Override
//            public void handle(WorkerStateEvent t)
//            {
//                boolean gcodePrintSuccess = (boolean) (t.getSource().getValue());
//                if (gcodePrintSuccess)
//                {
//                    generalPurposeDialog.setTitle(DisplayManager.getLanguageBundle().getString("gcodeMacroPanel.gcodePrintSuccessTitle"));
//                    generalPurposeDialog.setMessage(DisplayManager.getLanguageBundle().getString("gcodeMacroPanel.gcodePrintSuccessMessage"));
//                    generalPurposeDialog.show();
//                } else
//                {
//                    generalPurposeDialog.setTitle(DisplayManager.getLanguageBundle().getString("gcodeMacroPanel.gcodePrintFailedTitle"));
//                    generalPurposeDialog.setMessage(DisplayManager.getLanguageBundle().getString("gcodeMacroPanel.gcodePrintFailedMessage"));
//                    generalPurposeDialog.show();
//
//                    steno.warning("In gcode print succeeded but with failure flag");
//                }
//            }
//        });
//
//        gcodePrintService.setOnFailed(new EventHandler<WorkerStateEvent>()
//        {
//            @Override
//            public void handle(WorkerStateEvent t)
//            {
//
////                Worker worker = t.getSource();
////                if (t.getSource() != null)
////                {
////                    boolean gcodePrintSuccess = (boolean) (t.getSource().getValue());
////                    if (gcodePrintSuccess == false)
////                    {
////                        steno.warning("In gcode print failed but with success flag");
////                    }
////                }
//                generalPurposeDialog.setTitle(DisplayManager.getLanguageBundle().getString("dialogs.gcodePrintFailedTitle"));
//                generalPurposeDialog.setMessage(DisplayManager.getLanguageBundle().getString("dialogs.gcodePrintFailedMessage"));
//                generalPurposeDialog.addButton(DisplayManager.getLanguageBundle().getString("dialogs.gcodePrintOK"));
//                generalPurposeDialog.show();
//            }
//        });
//
//        StatusScreenState statusScreenState = StatusScreenState.getInstance();
//        statusScreenState.currentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>()
//        {
//
//            @Override
//            public void changed(ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue)
//            {
//                connectedPrinter = newValue;
//            }
//        });
//    }
}
