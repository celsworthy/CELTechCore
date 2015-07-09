/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.material;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.appManager.undo.UndoableProject;
import celtech.configuration.Filament;
import celtech.configuration.MaterialType;
import celtech.configuration.datafileaccessors.FilamentContainer;
import static celtech.printerControl.comms.commands.ColourStringConverter.colourToString;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

/**
 *
 * @author tony
 */
public class MaterialComponent extends Pane implements PrinterListChangesListener
{

    private Printer printer;
    private int extruderNumber;
    private Mode mode;
    private boolean selected;
    private static PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
    private final static String UNKNOWN = Lookup.i18n("materialComponent.unknown");
    private final ObjectProperty<Filament> selectedFilamentProperty = new SimpleObjectProperty<>();
    private UndoableProject undoableProject;
    private final FilamentContainer filamentContainer = Lookup.getFilamentContainer();

    public enum ReelType
    {

        ROBOX, GEARS, SOLID_QUESTION, SOLID_CROSS;
    }

    public enum Mode
    {

        STATUS, LAYOUT, SETTINGS;
    }

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private Text reelNumberMaterial;

    @FXML
    private SVGPath reelSVGRobox;

    @FXML
    private SVGPath reelSVGGears;

    @FXML
    private Group reelSVGQuestion;

    @FXML
    private Group reelSVGCross;

    @FXML
    private SVGPath svgLoaded;

    @FXML
    private Text materialColour;

    @FXML
    private Text materialRemaining;

    @FXML
    private HBox materialColourContainer;

    @FXML
    private HBox materialRemainingContainer;

    @FXML
    private ComboBox<Object> cmbMaterials;

    public MaterialComponent()
    {
        // Should only be called from scene builder
    }

    public MaterialComponent(Mode mode, Printer printer, int extruderNumber)
    {
        super();
        URL fxml = getClass().getResource(
            "/celtech/resources/fxml/components/material/material.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(fxml);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try
        {
            fxmlLoader.load();
        } catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }

        this.mode = mode;
        this.printer = printer;
        this.extruderNumber = extruderNumber;
        setupComboBox();
        updateGUIForModeAndPrinterExtruder();
        Lookup.getPrinterListChangesNotifier().addListener(this);

        Lookup.getUserPreferences().advancedModeProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                repopulateCmbMaterials();
            });

        selectedFilamentProperty.addListener(
            (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
            {
                switch (mode)
                {
                    case STATUS:
                        displayForStatusScreen(newValue);
                        break;
                    case LAYOUT:
                        displayForLayoutScreen(newValue);
                        break;
                    case SETTINGS:
                        displayForSettingsScreen(newValue);
                        break;
                }
            });

        setUpFilamentLoadedListener();
        setUpFilamentChangedListener();
    }

    public ReadOnlyObjectProperty<Filament> getSelectedFilamentProperty()
    {
        return selectedFilamentProperty;
    }

    private boolean filamentLoaded()
    {
        return printer.extrudersProperty().get(extruderNumber).filamentLoadedProperty().get();
    }

    /**
     * When the filament property changes for STATUS mode, update the component appropriately.
     * selectedFilament of null indicates that there is no Reel loaded on this extruder.
     */
    private void displayForStatusScreen(Filament filament)
    {
        if (selectedFilamentProperty.get() == null)
        {
            if (filamentLoaded())
            {
                showFilamentUnknown();
            } else
            {
                showFilamentNotLoaded();
            }
        } else
        {
            Float remainingFilament = 0f;
            Float diameter = 0f;
            if (printer.reelsProperty().containsKey(extruderNumber))
            {
                Reel reel = printer.reelsProperty().get(extruderNumber);
                remainingFilament = reel.remainingFilamentProperty().get();
                diameter = reel.diameterProperty().get();
                if (selectedFilamentProperty.get().isMutable())
                {
                    setReelType(ReelType.GEARS);
                } else
                {
                    setReelType(ReelType.ROBOX);
                }
            } else
            {
                setReelType(ReelType.GEARS);
            }
            setMaterial(extruderNumber, filament.getMaterial(),
                        filament.getFriendlyFilamentName(),
                        filament.getDisplayColourProperty().get(),
                        remainingFilament,
                        diameter, filamentLoaded());
        }
    }

    /**
     * When the filament property changes for LAYOUT mode, update the component appropriately.
     * selectedFilament of null is not valid after the first setting.
     */
    private void displayForLayoutScreen(Filament filament)
    {

        Float remainingFilament = 0f;
        Float diameter = 0f;
        setReelType(ReelType.GEARS);
        if (filament != null)
        {
            setMaterial(extruderNumber, filament.getMaterial(),
                        filament.getFriendlyFilamentName(),
                        filament.getDisplayColourProperty().get(),
                        remainingFilament,
                        diameter, false);
        }
    }

    /**
     * In LAYOUT mode this widget is responsible for updating the project filaments directly, and
     * listening for changes to the project filament.
     */
    public void setLayoutProject(Project project)
    {
        if (project != null)
        {
            this.undoableProject = new UndoableProject(project);
        }
        getSelectedFilamentProperty().addListener(
            (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
            {
                if (extruderNumber == 0)
                {
                    undoableProject.setExtruder0Filament(newValue);
                } else
                {
                    undoableProject.setExtruder1Filament(newValue);
                }
            });

        ChangeListener<Filament> filamentChangeListener = new ChangeListener<Filament>()
        {

            @Override
            public void changed(
                ObservableValue<? extends Filament> observable, Filament oldValue, Filament filament)
            {
                if (filament != null)
                {
                    Float remainingFilament = 0f;
                    Float diameter = 0f;
                    setMaterial(extruderNumber, filament.getMaterial(),
                                filament.getFriendlyFilamentName(),
                                filament.getDisplayColourProperty().get(),
                                remainingFilament,
                                diameter, false);
                }
            }
        };

        if (extruderNumber == 0)
        {
            project.getExtruder0FilamentProperty().addListener(filamentChangeListener);
        } else
        {
            project.getExtruder1FilamentProperty().addListener(filamentChangeListener);
        }
    }

    /**
     * When the filament property changes for SETTINGS mode, update the component appropriately.
     * selectedFilament of null indicates that the component is in the initial "Unknown" state that
     * occurs if no reel is loaded on that extruder and the user has yet to pick a filament.
     */
    private void displayForSettingsScreen(Filament filament)
    {
        if (filament == null)
        {
            if (filamentLoaded())
            {
                showFilamentUnknown();
            } else
            {
                showFilamentNotLoaded();
            }
        } else
        {
            Float remainingFilament = 0f;
            Float diameter = 0f;
            if (printer.reelsProperty().containsKey(extruderNumber))
            {
                Reel reel = printer.reelsProperty().get(extruderNumber);
                remainingFilament = reel.remainingFilamentProperty().get();
                diameter = reel.diameterProperty().get();
                if (selectedFilamentProperty.get().isMutable())
                {
                    setReelType(ReelType.GEARS);
                } else
                {
                    setReelType(ReelType.ROBOX);
                }
            } else
            {
                setReelType(ReelType.GEARS);
            }
            setMaterial(extruderNumber, filament.getMaterial(),
                        filament.getFriendlyFilamentName(),
                        filament.getDisplayColourProperty().get(),
                        remainingFilament,
                        diameter, filamentLoaded());
        }
    }

    private ObservableList<Object> comboItems;

//    class RefreshableListViewSkin extends ListViewSkin
//    {
//
//        public RefreshableListViewSkin(ListView listView)
//        {
//            super(listView);
//        }
//
//        public void refresh()
//        {
//            super.flow.rebuildCells();
//        }
//
//    }
//
//    class RefreshableComboBoxListViewSkin<T> extends ComboBoxListViewSkin<T>
//    {
//
//        public RefreshableComboBoxListViewSkin(ComboBox comboBox)
//        {
//            super(comboBox);
//
//            getListView().setSkin(new RefreshableListViewSkin(getListView()));
//
//        }
//
//        public void refresh()
//        {
//            ((RefreshableListViewSkin) getListView().getSkin()).refresh();
//        }
//
//    }
    /**
     * Set up the materials combo box. This displays a list of filaments and can also display an
     * "Unknown" (string) option when required.
     */
    private void setupComboBox()
    {

//        cmbMaterials.setSkin(new RefreshableComboBoxListViewSkin<Object>(cmbMaterials));
        cmbMaterials.setCellFactory((ListView<Object> param) -> new FilamentCell());

        repopulateCmbMaterials();

        filamentContainer.getUserFilamentList().addListener(
            (ListChangeListener.Change<? extends Filament> change) ->
            {

                while (change.next())
                {
                    if (change.wasAdded())
                    {
                        for (Filament filament : change.getAddedSubList())
                        {
                            cmbMaterials.getItems().add(filament);
                        }
                    } else if (change.wasRemoved())
                    {
                        for (Filament filament : change.getRemoved())
                        {
                            cmbMaterials.getItems().remove(filament);
                        }
                    } else if (change.wasReplaced())
                    {
                    } else if (change.wasUpdated())
                    {
                    }
                }

            });

        cmbMaterials.valueProperty().addListener(
            (ObservableValue<? extends Object> observable, Object oldValue, Object newValue) ->
            {
                if (newValue instanceof Filament)
                {
                    selectedFilamentProperty.set((Filament) cmbMaterials.getValue());
                    removeUnknownFromCombo();
                } else
                {
                    // must be "Unknown"
                    selectedFilamentProperty.set(null);
                }
            });

        if (mode == Mode.SETTINGS)
        {
            cmbMaterials.setValue(UNKNOWN);
        }

    }

    private void repopulateCmbMaterials()
    {
        Object currentValue = cmbMaterials.getValue();
        String currentFilamentId = "";
        if (currentValue instanceof Filament)
        {
            currentFilamentId = ((Filament) currentValue).getFilamentID();
        }

        ObservableList<Filament> allFilaments = FXCollections.observableArrayList();
        ObservableList<Filament> userFilaments = FXCollections.observableArrayList();

        try
        {
            allFilaments.addAll(filamentContainer.getAppFilamentList().sorted(
                (Filament o1, Filament o2)
                -> o1.getFriendlyFilamentName().compareTo(o2.getFriendlyFilamentName())));
            if (Lookup.getUserPreferences().isAdvancedMode())
            {
                allFilaments.addAll(filamentContainer.getUserFilamentList().sorted(
                    (Filament o1, Filament o2)
                    -> o1.getFriendlyFilamentName().compareTo(o2.getFriendlyFilamentName())));
                userFilaments.addAll(filamentContainer.getUserFilamentList().sorted(
                    (Filament o1, Filament o2)
                    -> o1.getFriendlyFilamentName().compareTo(o2.getFriendlyFilamentName())));
            }
        } catch (NoClassDefFoundError exception)
        {
            // this should only happen in SceneBuilder            
        }

        List<Object> filamentsList = new ArrayList<>();
        if (mode == Mode.SETTINGS)
        {
            filamentsList.add(UNKNOWN);
            filamentsList.addAll(userFilaments);
        } else
        {
            filamentsList.addAll(allFilaments);
        }
        comboItems = FXCollections.observableArrayList(filamentsList);
        cmbMaterials.setItems(null);
        cmbMaterials.setItems(comboItems);
//        ((RefreshableComboBoxListViewSkin) cmbMaterials.getSkin()).refresh();

        if (mode == Mode.LAYOUT)
        {
            reselectFilamentId(currentFilamentId);
        }

    }

    /**
     * Set the combo box selection to the filament of the given id, or to the first filament in the
     * list if filamentId is empty.
     *
     * @param filamentId
     */
    private void reselectFilamentId(String filamentId)
    {
        boolean filamentFound = false;
        for (Object comboItem : comboItems)
        {
            if (comboItem instanceof Filament)
            {
                if (((Filament) comboItem).getFilamentID().equals(filamentId))
                {
                    cmbMaterials.setValue(comboItem);
                    filamentFound = true;
                    break;
                }
            }
        }
        if (!filamentFound && comboItems.size() > 0)
        {
            cmbMaterials.setValue(comboItems.get(0));
        }
    }

    public void whenMaterialSelected(ActionEvent actionEvent)
    {
        Object selectedMaterial = cmbMaterials.getValue();
        if (selectedMaterial instanceof Filament)
        {
            Filament filament = (Filament) selectedMaterial;
            selectedFilamentProperty.set(filament);
        } else
        {
            selectedFilamentProperty.set(null);
            showFilamentNotLoaded();
        }
    }

    /**
     * Set the operational mode.
     */
    public void setMode(Mode mode)
    {
        this.mode = mode;
        updateGUIForModeAndPrinterExtruder();
    }

    /**
     * Set the printer for this component.
     */
    public void setPrinter(Printer printer)
    {
        this.printer = printer;
        updateGUIForModeAndPrinterExtruder();
    }

    private void updateGUIForModeAndPrinterExtruder()
    {
        switch (mode)
        {
            case STATUS:
                customiseForStatusScreen();
                break;
            case LAYOUT:
                customiseForLayoutScreen();
                break;
            case SETTINGS:
                customiseForSettingsScreen();
                break;
        }
    }

    private void customiseForLayoutScreen()
    {
        cmbMaterials.setVisible(true);
        materialColourContainer.setVisible(true);
        materialRemainingContainer.setVisible(false);
        hideMaterialDetails();
        displayForLayoutScreen(selectedFilamentProperty.get());
    }

    private void customiseForSettingsScreen()
    {
        showMaterialDetails();
        if (printer.reelsProperty().containsKey(extruderNumber))
        {
            cmbMaterials.setVisible(false);
            materialColourContainer.setVisible(true);
            materialRemainingContainer.setVisible(true);
            setReelType(ReelType.ROBOX);
            Reel reel = printer.reelsProperty().get(extruderNumber);
            Filament filament = filamentContainer.getFilamentByID(reel.filamentIDProperty().get());
            selectedFilamentProperty.set(filament);
        } else
        {
            selectedFilamentProperty.set(null);
            cmbMaterials.setVisible(true);
            materialColourContainer.setVisible(false);
            materialRemainingContainer.setVisible(false);
        }
        displayForSettingsScreen(selectedFilamentProperty.get());
    }

    /**
     * Configure this component as required for the Status screen. It should only show the details
     * of a loaded SmartReel. If no SmartReel is present then simply display 'Unknown' as the
     * material with a ReelType of SOLID_QUESTION. Do not show the combo.
     */
    private void customiseForStatusScreen()
    {
        cmbMaterials.setVisible(false);
        materialColourContainer.setVisible(true);
        materialRemainingContainer.setVisible(true);
        showMaterialDetails();
        if (printer.reelsProperty().containsKey(extruderNumber))
        {
            setReelType(ReelType.ROBOX);
            Reel reel = printer.reelsProperty().get(extruderNumber);
            Filament filament = filamentContainer.getFilamentByID(reel.filamentIDProperty().get());
            selectedFilamentProperty.set(filament);
        } else
        {
            selectedFilamentProperty.set(null);
        }
        displayForStatusScreen(selectedFilamentProperty.get());
    }

    private void showMaterialDetails()
    {
        materialRemainingContainer.setPrefHeight(20);
        AnchorPane.setTopAnchor(materialRemainingContainer, 100.0);
        anchorPane.setPrefHeight(130);
        materialRemaining.setVisible(true);
    }

    private void hideMaterialDetails()
    {
        materialRemainingContainer.setPrefHeight(0);
        AnchorPane.setTopAnchor(materialRemainingContainer, 90.0);
        anchorPane.setPrefHeight(95);
        materialRemaining.setVisible(false);
    }

    public void setReelType(ReelType reelType)
    {
        reelSVGRobox.setVisible(false);
        reelSVGGears.setVisible(false);
        reelSVGQuestion.setVisible(false);
        reelSVGCross.setVisible(false);
        switch (reelType)
        {
            case ROBOX:
                reelSVGRobox.setVisible(true);
                break;
            case GEARS:
                reelSVGGears.setVisible(true);
                break;
            case SOLID_QUESTION:
                reelSVGQuestion.setVisible(true);
                break;
            case SOLID_CROSS:
                reelSVGCross.setVisible(true);
                break;
        }
    }

    private void setMaterial(int reelNumber, MaterialType materialType, String materialColourString,
        Color colour, double remainingFilament, double filamentDiameter, boolean filamentLoaded)
    {

        String numberMaterial = String.valueOf(reelNumber + 1) + ":"
            + materialType.getFriendlyName();

        double remainingLengthMeters = remainingFilament / 1000d;
        if (remainingLengthMeters < 0)
        {
            remainingLengthMeters = 0;
        }
        double densityKGM3 = materialType.getDensity() * 1000d;
        double crossSectionM2 = Math.PI * filamentDiameter * filamentDiameter / 4d * 1e-6;
        double remainingWeightG = remainingLengthMeters * crossSectionM2 * densityKGM3 * 1000d;
        String remaining = ((int) remainingLengthMeters) + "m / " + ((int) remainingWeightG)
            + "g " + Lookup.i18n("materialComponent.remaining");

        showDetails(numberMaterial, remaining, materialColourString, colour, filamentLoaded);
    }

    private void showDetails(String numberMaterial, String materialRemainingString,
        String materialColourString, Color colour, boolean filamentLoaded)
    {

        svgLoaded.setVisible(filamentLoaded);

        reelNumberMaterial.setText(numberMaterial);
        materialRemaining.setText(materialRemainingString);
        String colourString = colourToString(colour);
        reelNumberMaterial.setStyle("-fx-fill: #" + colourString + ";");
        materialColourContainer.setStyle("-fx-background-color: #" + colourString + ";");
        svgLoaded.setStyle("-fx-fill: #" + colourString + ";");
        setReelColourString(colourString);

        materialColour.setText(materialColourString);
        if (colour.getBrightness() < 0.5)
        {
            materialColour.setStyle("-fx-fill:white;");
        } else
        {
            materialColour.setStyle("-fx-fill:black;");
        }
    }

    /**
     * Indicate that no reel is attached and the filament is loaded but unknown.
     */
    private void showFilamentUnknown()
    {
        svgLoaded.setVisible(true);
        svgLoaded.setFill(Color.BLACK);
        setReelType(ReelType.SOLID_QUESTION);
        String materialUnknown = Lookup.i18n("materialComponent.materialUnknown");
        showDetails((1 + extruderNumber) + ":", "", materialUnknown,
                    Color.BLACK, true);
    }

    /**
     * Indicate that no reel is attached and no filament is loaded.
     */
    private void showFilamentNotLoaded()
    {
        svgLoaded.setVisible(false);
        setReelType(ReelType.SOLID_CROSS);
        String pleaseLoadAFilament = Lookup.i18n("materialComponent.pleaseLoadAFilament");
        showDetails((1 + extruderNumber) + ":", "", pleaseLoadAFilament, Color.BLACK, false);
    }

    private void setReelColourString(String colourString)
    {
        reelSVGRobox.setStyle("-fx-fill: #" + colourString + ";");
        reelSVGGears.setStyle("-fx-fill: #" + colourString + ";");
    }

    /**
     * Select the given filament and update the control to reflect the selected colour etc. The
     * remaining material is set to show 0.
     */
    public void setSelectedFilamentInComboBox(Filament filament)
    {
        selectedFilamentProperty.set(filament);
        if (filament != null)
        {
            cmbMaterials.setValue(filament);
        }
    }

    /**
     * Visually indicate that this component is selected.
     */
    public void select(boolean selected)
    {
        this.selected = selected;
        anchorPane.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, selected);
    }

    /**
     * In SETTINGS mode, when a reel is removed then re-add the Unknown option.
     */
    private void readdUnknownToCombo()
    {
        if (!comboItems.contains(UNKNOWN))
        {
            comboItems.add(0, UNKNOWN);
            cmbMaterials.setValue(UNKNOWN);
        }
    }

    /**
     * In SETTINGS mode, when a custom filament has been chosen remove the Unknown option.
     */
    private void removeUnknownFromCombo()
    {
        if (comboItems.contains(UNKNOWN))
        {
            Object currentVal = cmbMaterials.getValue();
            comboItems.remove(UNKNOWN);
            cmbMaterials.setValue(currentVal);
        }
    }

    ChangeListener<Color> filamentChanged
        = (ObservableValue<? extends Color> observable, Color oldValue, Color newValue) ->
        {
            updateGUIForModeAndPrinterExtruder();
            repopulateCmbMaterials();
        };

    private void setUpFilamentChangedListener()
    {
        selectedFilamentProperty.addListener(
            (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
            {
                if (oldValue != null)
                {
                    oldValue.getDisplayColourProperty().removeListener(filamentChanged);
                }
                if (newValue != null)
                {
                    newValue.getDisplayColourProperty().addListener(filamentChanged);
                }
            });
    }

    private void setUpFilamentLoadedListener()
    {
        if (printer != null && printer.extrudersProperty().get(extruderNumber) != null)
        {
            printer.extrudersProperty().get(extruderNumber).filamentLoadedProperty().addListener(
                new ChangeListener<Boolean>()
                {

                    @Override
                    public void changed(
                        ObservableValue<? extends Boolean> observable, Boolean oldValue,
                        Boolean newValue)
                    {
                        updateGUIForModeAndPrinterExtruder();
                        }
                });
        }
    }

    // PrinterListChangesNotifier
    @Override
    public void whenPrinterAdded(Printer printer)
    {
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
        if (this.printer == printer)
        {
            updateGUIForModeAndPrinterExtruder();
        }
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
        if (this.printer == printer)
        {
            readdUnknownToCombo();
            updateGUIForModeAndPrinterExtruder();
        }
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
        if (this.printer == printer)
        {
            updateGUIForModeAndPrinterExtruder();
        }
    }

    @Override
    public void whenExtruderAdded(Printer printer, int extruderIndex)
    {
        if (this.printer == printer)
        {
            setUpFilamentLoadedListener();
        }
    }

    @Override
    public void whenExtruderRemoved(Printer printer, int extruderIndex)
    {

    }

}
