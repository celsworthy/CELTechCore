package celtech.coreUI.controllers.panels;

import celtech.coreUI.components.calibration.CalibrationMenu;
import static celtech.coreUI.controllers.panels.CalibrationMenuConfiguration.configureCalibrationMenu;
import celtech.services.calibration.NozzleBCalibrationState;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrationChoiceInsetPanelController implements Initializable
{

    @FXML
    private CalibrationMenu calibrationMenu;

    private Stenographer steno = StenographerFactory.getStenographer(
        CalibrationNozzleBInsetPanelController.class.getName());

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        configureCalibrationMenu(calibrationMenu);
    }

}
