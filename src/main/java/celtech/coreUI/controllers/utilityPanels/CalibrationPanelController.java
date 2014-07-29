/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.utilityPanels;

import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.controllers.panels.CalibrationNozzleBInsetPanelController;
import celtech.coreUI.controllers.panels.CalibrationNozzleOffsetInsetPanelController;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class CalibrationPanelController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(CalibrationPanelController.class.getName());
    private ResourceBundle i18nBundle = null;

    private Stage needleValvecalibrationStage = null;
    private CalibrationNozzleBInsetPanelController needleValveCalibrationController = null;
    private Stage offsetCalibrationStage = null;
    private CalibrationNozzleOffsetInsetPanelController nozzleOffsetCalibrationController = null;

    @FXML
    void calibrateB(ActionEvent event)
    {

        if (needleValvecalibrationStage == null)
        {
            needleValvecalibrationStage = new Stage(StageStyle.UTILITY);
            URL needleValveCalibrationFXMLURL = ModalDialog.class.getResource(ApplicationConfiguration.fxmlResourcePath + "CalibrationNozzleBPage.fxml");
            FXMLLoader needleValveCalibrationLoader = new FXMLLoader(needleValveCalibrationFXMLURL, i18nBundle);
            try
            {
                Parent dialogBoxScreen = (Parent) needleValveCalibrationLoader.load();
                needleValveCalibrationController = (CalibrationNozzleBInsetPanelController) needleValveCalibrationLoader.getController();
                Scene dialogScene = new Scene(dialogBoxScreen, Color.TRANSPARENT);
                dialogScene.getStylesheets().add(ApplicationConfiguration.mainCSSFile);
                needleValvecalibrationStage.setScene(dialogScene);
                needleValvecalibrationStage.initOwner(DisplayManager.getMainStage());
                needleValvecalibrationStage.initModality(Modality.WINDOW_MODAL);
                needleValvecalibrationStage.setOnCloseRequest(new EventHandler<WindowEvent>()
                {
                    @Override
                    public void handle(WindowEvent event)
                    {
                        needleValveCalibrationController.cancelCalibrationAction();
                    }
                });
            } catch (IOException ex)
            {
                steno.error("Couldn't load needle valve calibration FXML");
            }
        }

        needleValvecalibrationStage.showAndWait();
    }

    @FXML
    void calibrateZOffset(ActionEvent event)
    {

        if (offsetCalibrationStage == null)
        {
            offsetCalibrationStage = new Stage(StageStyle.UTILITY);
            URL needleValveCalibrationFXMLURL = ModalDialog.class.getResource(ApplicationConfiguration.fxmlResourcePath + "CalibrationNozzleOffsetPage.fxml");
            FXMLLoader nozzleOffsetCalibrationLoader = new FXMLLoader(needleValveCalibrationFXMLURL, i18nBundle);
            try
            {
                Parent dialogBoxScreen = (Parent) nozzleOffsetCalibrationLoader.load();
                nozzleOffsetCalibrationController = (CalibrationNozzleOffsetInsetPanelController) nozzleOffsetCalibrationLoader.getController();
                Scene dialogScene = new Scene(dialogBoxScreen, Color.TRANSPARENT);
                dialogScene.getStylesheets().add(ApplicationConfiguration.mainCSSFile);
                offsetCalibrationStage.setScene(dialogScene);
                offsetCalibrationStage.initOwner(DisplayManager.getMainStage());
                offsetCalibrationStage.initModality(Modality.WINDOW_MODAL);
            } catch (IOException ex)
            {
                steno.error("Couldn't load nozzle offset calibration FXML");
            }
        }

        offsetCalibrationStage.showAndWait();
    }

    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        i18nBundle = DisplayManager.getLanguageBundle();
    }
}
