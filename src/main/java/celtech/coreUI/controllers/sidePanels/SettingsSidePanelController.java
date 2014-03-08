/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.sidePanels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.FilamentContainer;
import celtech.configuration.PrintProfileContainer;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.MaterialChoiceListCell;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.coreUI.controllers.utilityPanels.MaterialDetailsController;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SlicerSettings;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.StringConverter;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.control.ButtonBar;
import org.controlsfx.control.ButtonBar.ButtonType;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.AbstractDialogAction;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialog.ActionTrait;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SettingsSidePanelController implements Initializable, SidePanelManager
{

    private Stenographer steno = StenographerFactory.getStenographer(SettingsSidePanelController.class.getName());
    private ObservableList<Printer> printerStatusList = null;
    private SettingsScreenState settingsScreenState = null;
    private ApplicationStatus applicationStatus = null;
    private DisplayManager displayManager = null;

    @FXML
    private ComboBox<Printer> printerChooser;

    @FXML
    private ComboBox<Filament> materialChooser;

    @FXML
    private Slider qualityChooser;

    @FXML
    void go(MouseEvent event)
    {
        settingsScreenState.getSettings().renderToFile("/tmp/settings.dat");
    }

    private SlicerSettings draftSettings = PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.draftSettingsProfileName);
    private SlicerSettings normalSettings = PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.normalSettingsProfileName);
    private SlicerSettings fineSettings = PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.fineSettingsProfileName);
    private SlicerSettings customSettings = null;
    private SlicerSettings lastSettings = null;

    private ChangeListener<Toggle> nozzleSelectionListener = null;
    private ChangeListener<Filament> filamentChangeListener = null;
    private ChangeListener<Boolean> reelDataChangedListener = null;

    private ObservableList<Filament> availableFilaments = FXCollections.observableArrayList();

    private Printer currentPrinter = null;
    private Filament currentlyLoadedFilament = null;

    private VBox createMaterialPage = null;
    private Dialog createMaterialDialogue = null;
    private String saveNewMaterialActionName = "createMaterial";
    private String saveNewMaterialString = null;
    private Action actionCreateMaterial = null;

    private SettingsSlideOutPanelController slideOutController = null;

    private MaterialDetailsController materialDetailsController = null;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        applicationStatus = ApplicationStatus.getInstance();
        displayManager = DisplayManager.getInstance();
        settingsScreenState = SettingsScreenState.getInstance();
        printerStatusList = RoboxCommsManager.getInstance().getPrintStatusList();

        saveNewMaterialString = DisplayManager.getLanguageBundle().getString("sidePanel_settings.saveNewMaterial");

        actionCreateMaterial = new AbstractDialogAction(saveNewMaterialString,
                ActionTrait.CLOSING, ActionTrait.DEFAULT)
                {
                    {
                        ButtonBar.setType(this, ButtonType.OK_DONE);
                    }

                    @Override
                    public void execute(ActionEvent ae)
                    {
                        Dialog dlg = (Dialog) ae.getSource();
                        Filament filamentToSave = materialDetailsController.getMaterialData();
                        FilamentContainer.saveFilament(filamentToSave);
                        dlg.setResult(this);
                    }

                    public String toString()
                    {
                        return saveNewMaterialString;
                    }
                ;

        };
        
        try
        {
            FXMLLoader createMaterialPageLoader = new FXMLLoader(getClass().getResource(ApplicationConfiguration.fxmlUtilityPanelResourcePath + "materialDetails.fxml"), DisplayManager.getLanguageBundle());
            createMaterialPage = createMaterialPageLoader.load();
            materialDetailsController = createMaterialPageLoader.getController();
            materialDetailsController.updateMaterialData(new Filament("", "", "", "",
                    0, 0, 0, 0, 0, 0, 0, 0, Color.ALICEBLUE, true));
            materialDetailsController.showButtons(false);

            createMaterialDialogue = new Dialog(DisplayManager.getMainStage(), DisplayManager.getLanguageBundle().getString("sidePanel_settings.createMaterialDialogueTitle"), false, true);
            createMaterialDialogue.setContent(createMaterialPage);
            createMaterialDialogue.setResizable(false);
            createMaterialDialogue.getStylesheets().add(ApplicationConfiguration.mainCSSFile);
            createMaterialDialogue.getActions().addAll(actionCreateMaterial, Dialog.Actions.CANCEL);
        } catch (Exception ex)
        {
            steno.error("Failed to load material creation page");
        }

        qualityChooser.setLabelFormatter(new StringConverter<Double>()
        {
            @Override
            public String toString(Double n)
            {
                PrintQualityEnumeration selectedQuality = PrintQualityEnumeration.fromEnumPosition(n.intValue());
                return selectedQuality.getFriendlyName();
            }

            @Override
            public Double fromString(String s)
            {
                PrintQualityEnumeration selectedQuality = PrintQualityEnumeration.valueOf(s);
                return (double) selectedQuality.getEnumPosition();
            }
        });

        printerChooser.setItems(printerStatusList);

        printerChooser.getSelectionModel()
                .clearSelection();

        printerChooser.getItems()
                .addListener(new ListChangeListener<Printer>()
                        {

                            @Override
                            public void onChanged(ListChangeListener.Change<? extends Printer> change
                            )
                            {
                                while (change.next())
                                {
                                    if (change.wasAdded())
                                    {
                                        for (Printer addedPrinter : change.getAddedSubList())
                                        {
                                            if (printerChooser.getSelectionModel().getSelectedItem() == null)
                                            {
                                                printerChooser.getSelectionModel().select(0);
                                                break;
                                            }
                                        }
                                    } else if (change.wasRemoved())
                                    {
                                        if (printerChooser.getItems().isEmpty() && applicationStatus.getMode() == ApplicationMode.SETTINGS)
                                        {
                                            applicationStatus.setMode(ApplicationMode.STATUS);
                                        }
                                    } else if (change.wasReplaced())
                                    {
                                    } else if (change.wasUpdated())
                                    {
                                    }
                                }
                            }
                }
                );

        settingsScreenState.selectedPrinterProperty()
                .bind(printerChooser.valueProperty());

        printerChooser.getSelectionModel()
                .selectedItemProperty().addListener(new ChangeListener<Printer>()
                        {
                            @Override
                            public void changed(ObservableValue<? extends Printer> ov, Printer lastSelectedPrinter, Printer selectedPrinter
                            )
                            {
                                if (lastSelectedPrinter != null)
                                {
                                    lastSelectedPrinter.reelDataChangedProperty().removeListener(reelDataChangedListener);
                                    lastSelectedPrinter.loadedFilamentProperty().removeListener(filamentChangeListener);
                                }
                                if (selectedPrinter != null && selectedPrinter != lastSelectedPrinter)
                                {
                                    currentPrinter = selectedPrinter;
                                    selectedPrinter.reelDataChangedProperty().addListener(reelDataChangedListener);
                                    selectedPrinter.loadedFilamentProperty().addListener(filamentChangeListener);
                                }

                                if (selectedPrinter == null)
                                {
                                    currentPrinter = null;
                                }
                            }
                }
                );

        Callback<ListView<Filament>, ListCell<Filament>> materialChooserCellFactory
                = new Callback<ListView<Filament>, ListCell<Filament>>()
                {
                    @Override
                    public ListCell<Filament> call(ListView<Filament> list)
                    {
                        return new MaterialChoiceListCell();
                    }
                };

        materialChooser.setCellFactory(materialChooserCellFactory);
        materialChooser.setButtonCell(materialChooserCellFactory.call(null));
        materialChooser.setItems(availableFilaments);

        materialChooser.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Filament>()
        {
            @Override
            public void changed(ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue)
            {
                if (newValue == FilamentContainer.createNewFilament)
                {
                    Action response = createMaterialDialogue.show();
                    materialChooser.getSelectionModel().selectFirst();
                } else
                {
                    slideOutController.updateFilamentData(newValue);
                }
            }
        });

//        bindQualitySpecificSettings(printerChooser.getValue());
//
//        printQualityChoice.valueProperty().addListener(new ChangeListener<PrintQualityEnumeration>()
//        {
//            @Override
//            public void changed(ObservableValue<? extends PrintQualityEnumeration> ov, PrintQualityEnumeration t, PrintQualityEnumeration t1)
//            {
//                switch (t1)
//                {
//                    case DRAFT:
//                        settingsScreenState.setSettings(draftSettings);
//                        break;
//                    case NORMAL:
//                        settingsScreenState.setSettings(normalSettings);
//                        break;
//                    case FINE:
//                        settingsScreenState.setSettings(fineSettings);
//                        break;
//                    case CUSTOM:
//                        settingsScreenState.setSettings(customSettings);
//                        break;
//                    default:
//                        break;
//                }
//                unbindQualitySpecificSettings(t);
//                bindQualitySpecificSettings(t1);
//            }
//        });
        filamentChangeListener = new ChangeListener<Filament>()
        {
            @Override
            public void changed(ObservableValue<? extends Filament> ov, Filament t, Filament t1)
            {
                currentlyLoadedFilament = t1;
                updateFilamentList();
            }
        };

        reelDataChangedListener = new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
//                updateMaterialTextBoxes();
            }
        };

        FilamentContainer.getUserFilamentList().addListener(new ListChangeListener<Filament>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends Filament> c)
            {
                updateFilamentList();
            }
        });

    }

    private void updateFilamentList()
    {
        availableFilaments.clear();

        if (currentlyLoadedFilament != null)
        {
            availableFilaments.add(currentlyLoadedFilament);
            materialChooser.getSelectionModel().select(currentlyLoadedFilament);
        }

        availableFilaments.addAll(FilamentContainer.getUserFilamentList());
        availableFilaments.add(FilamentContainer.createNewFilament);
    }

    private void populatePrinterChooser()
    {
        for (Printer printer : printerStatusList)
        {
            printerChooser.getItems().add(printer);
        }
    }

    @Override
    public void configure(Initializable slideOutController)
    {
        this.slideOutController = (SettingsSlideOutPanelController) slideOutController;
        updateFilamentList();
    }
}
