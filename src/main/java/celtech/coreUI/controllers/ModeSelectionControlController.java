/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.EnhancedToggleGroup;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ModeSelectionControlController implements Initializable
{

    private Stenographer steno = StenographerFactory.getStenographer(ModeSelectionControlController.class.getName());
    private ApplicationStatus applicationStatus = null;
    private SettingsScreenState settingsScreenState = null;
    private ObjectProperty<Toggle> selectedToggle = new SimpleObjectProperty<>();
    private Color selectedColour = new Color(0.012, .65, .843, 1);
    private Color unselectedColour = new Color(0.38, .38, .38, 1);

    @FXML
    private Rectangle layoutOutline;

    @FXML
    private Rectangle settingsOutline;

    @FXML
    private Rectangle progressArrowPart1;
    
    @FXML
    private ImageView progressArrowPart2;

    @FXML
    private ToggleButton layoutModeButton;

    @FXML
    private ToggleButton settingsModeButton;

    @FXML
    private ToggleButton printModeButton;

    private ObservableList<Printer> printerStatusList = null;

    @FXML
    void gotoStatusMode(MouseEvent event)
    {
//        applicationStatus.setMode(ApplicationMode.STATUS);
    }

    @FXML
    void gotoLayoutMode(MouseEvent event)
    {
//        applicationStatus.setMode(ApplicationMode.LAYOUT);
    }

    @FXML
    void gotoSettingsMode(MouseEvent event)
    {
//        applicationStatus.setMode(ApplicationMode.SETTINGS);
    }

    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        applicationStatus = ApplicationStatus.getInstance();
        settingsScreenState = SettingsScreenState.getInstance();

        EnhancedToggleGroup toggleGroup = new EnhancedToggleGroup();

        layoutModeButton.setToggleGroup(toggleGroup);
        layoutModeButton.setUserData(ApplicationMode.LAYOUT);
        layoutModeButton.selectedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                if (t1.booleanValue() == true)
                {
                    applicationStatus.setMode(ApplicationMode.LAYOUT);
                }
            }
        });

        settingsModeButton.setToggleGroup(toggleGroup);
        settingsModeButton.setUserData(ApplicationMode.SETTINGS);
        settingsModeButton.selectedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                if (t1.booleanValue() == true)
                {
                    applicationStatus.setMode(ApplicationMode.SETTINGS);
                }
            }
        });

        printModeButton.setToggleGroup(toggleGroup);
        printModeButton.selectedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                if (t1.booleanValue() == true)
                {
                    Project currentProject = DisplayManager.getInstance().getCurrentlyVisibleProject();
                    settingsScreenState.getSelectedPrinter().printProject(currentProject, settingsScreenState.getFilament(), settingsScreenState.getPrintQuality(), settingsScreenState.getSettings());
                    applicationStatus.setMode(ApplicationMode.STATUS);
                }
            }
        });

        selectedToggle.bindBidirectional(toggleGroup.writableSelectedToggleProperty());

        setupModeButtons();

        applicationStatus.modeProperty().addListener(new ChangeListener<ApplicationMode>()
        {
            @Override
            public void changed(ObservableValue<? extends ApplicationMode> ov, ApplicationMode t, ApplicationMode t1)
            {
                setupModeButtons();
            }
        });

        layoutModeButton.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent t)
            {
                if (applicationStatus.getMode() == ApplicationMode.LAYOUT)
                {
                    t.consume();
                }
            }
        });

        settingsModeButton.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent t)
            {
                if (applicationStatus.getMode() == ApplicationMode.SETTINGS)
                {
                    t.consume();
                }
            }
        });

        printerStatusList = RoboxCommsManager.getInstance().getPrintStatusList();

        settingsModeButton.disableProperty().bind(Bindings.isEmpty(printerStatusList).or(applicationStatus.modeProperty().isEqualTo(ApplicationMode.STATUS)));
        printModeButton.disableProperty().bind(Bindings.isEmpty(printerStatusList).or(applicationStatus.modeProperty().isNotEqualTo(ApplicationMode.SETTINGS).or(settingsScreenState.selectedPrinterProperty().isNull())));

    }

    private void setupModeButtons()
    {
        switch (applicationStatus.getMode())
        {
            case LAYOUT:
                selectedToggle.setValue(layoutModeButton);
                layoutOutline.setStroke(selectedColour);
                settingsOutline.setStroke(unselectedColour);
                progressArrowPart1.setFill(unselectedColour);
                progressArrowPart1.setOpacity(1);
                break;
            case SETTINGS:
                selectedToggle.setValue(settingsModeButton);
                layoutOutline.setStroke(selectedColour);
                settingsOutline.setStroke(selectedColour);
                progressArrowPart1.setFill(selectedColour);
                progressArrowPart1.setOpacity(1);
                break;
            default:
                layoutOutline.setStroke(unselectedColour);
                settingsOutline.setStroke(unselectedColour);
                progressArrowPart1.setFill(unselectedColour);
                progressArrowPart1.setOpacity(.5);
                break;
        }
    }
}
