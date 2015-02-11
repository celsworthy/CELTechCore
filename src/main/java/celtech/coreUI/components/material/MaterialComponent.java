/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.material;

import celtech.Lookup;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.util.Callback;

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
    private final ObjectProperty<Filament> selectedFilamentProperty = new SimpleObjectProperty<>();

    public enum ReelType
    {

        ROBOX, GEARS, SOLID_QUESTION;
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
    private SVGPath reelSVGSolid;

    @FXML
    private Text materialColour;

    @FXML
    private Text materialRemaining;

    @FXML
    private HBox materialColourContainer;

    @FXML
    private HBox materialRemainingContainer;

    @FXML
    private ComboBox<Filament> cmbMaterials;

    private ObservableList<Filament> availableFilaments = FXCollections.observableArrayList();
    
    
    public MaterialComponent() {
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

        try
        {
            availableFilaments.addAll(FilamentContainer.getAppFilamentList());
            availableFilaments.addAll(FilamentContainer.getUserFilamentList());
        } catch (NoClassDefFoundError exception)
        {
            // this should only happen in SceneBuilder            
        }

        setupComboBox();

        this.mode = mode;
        this.printer = printer;
        this.extruderNumber = extruderNumber;
        updateGUIForModeAndPrinterExtruder();  
        Lookup.getPrinterListChangesNotifier().addListener(this);
    }
    
    public ReadOnlyObjectProperty<Filament> getSelectedFilamentProperty() {
        return selectedFilamentProperty;
    }

    private void setupComboBox()
    {
        cmbMaterials.setCellFactory(new Callback<ListView<Filament>, ListCell<Filament>>()
        {

            @Override
            public ListCell<Filament> call(ListView<Filament> param)
            {

                return new FilamentCell();
            }
        });
        cmbMaterials.setItems(availableFilaments);
        
        FilamentContainer.getUserFilamentList().addListener((ListChangeListener.Change<? extends Filament> c) ->
        {
            // do we need to do this given it is an ObservableList??
            updateFilamentList();
        });    
        
        cmbMaterials.valueProperty().addListener((ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
        {
            selectedFilamentProperty.set(cmbMaterials.getValue());
        });

    }
    
    private void updateFilamentList() {
        
    }

    public void whenMaterialSelected(ActionEvent actionEvent)
    {
        Filament selectedMaterial = cmbMaterials.getSelectionModel().getSelectedItem();
        setMaterial(1, selectedMaterial.getMaterial(), "",
                    selectedMaterial.getDisplayColourProperty().get(), 0, 0);
    }

    /**
     * Set the operational mode.
     */
    public void setMode(Mode mode)
    {
        this.mode = mode;
        updateGUIForModeAndPrinterExtruder();
    }
    
    private void updateGUIForModeAndPrinterExtruder() {
        switch (mode)
        {
//            case STATUS_CUSTOM:
//                cmbMaterials.setVisible(true);
//                materialColourContainer.setVisible(false);
//                materialRemainingContainer.setVisible(true);
//                showMaterialDetails();
//                setReelType(ReelType.SOLID_QUESTION);
//                break;
            case STATUS:
                cmbMaterials.setVisible(false);
                materialColourContainer.setVisible(true);
                materialRemainingContainer.setVisible(true);
                showMaterialDetails();
                customiseForStatusScreen();
                break;
            case LAYOUT:
                cmbMaterials.setVisible(true);
                materialColourContainer.setVisible(true);
                materialRemainingContainer.setVisible(false);
                hideMaterialDetails();
                setReelType(ReelType.ROBOX);
                break;
            case SETTINGS:
                cmbMaterials.setVisible(true);
                materialColourContainer.setVisible(true);
                materialRemainingContainer.setVisible(false);
                showMaterialDetails();
                if (printer.reelsProperty().containsKey(extruderNumber)) {
                    setReelType(ReelType.ROBOX);
                } else {
                    setReelType(ReelType.SOLID_QUESTION);
                }
                
                break;                
        }
    }

    /**
     * Configure this component as required for the Status screen. It should only show the details
     * of a loaded SmartReel. If no SmartReel is present then simply display 'unknown' as the 
     * material with a ReelType of SOLID_QUESTION. Do not show the combo.
     */
    private void customiseForStatusScreen()
    {
        if (printer.reelsProperty().containsKey(extruderNumber)) {
            setReelType(ReelType.ROBOX);
        } else {
            showReelNotLoaded();
        }
        
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
    
    private ChangeListener<Object> reelListener;
    
    /**
     * Update the material component with the appropriate details.
     */
    private void updateReelExtruderMaterial(int reelNumber, Reel reel)
    {
        if (reel == null)
        {
            showFilamentNotLoaded();
        } else
        {
            setMaterial(reelNumber, reel.materialProperty().get(),
                                          reel.friendlyFilamentNameProperty().get(),
                                          reel.displayColourProperty().get(),
                                          reel.remainingFilamentProperty().get(),
                                          reel.diameterProperty().get());
        }
    }    
    
//   

    public void setReelType(ReelType reelType)
    {
        reelSVGRobox.setVisible(false);
        reelSVGGears.setVisible(false);
        reelSVGSolid.setVisible(false);
        switch (reelType)
        {
            case ROBOX:
                reelSVGRobox.setVisible(true);
                break;
            case GEARS:
                reelSVGGears.setVisible(true);
                break;
            case SOLID_QUESTION:
                reelSVGSolid.setVisible(true);
                break;
        }
    }

    public void setMaterial(int reelNumber, MaterialType materialType, String materialColourString,
        Color colour, double remainingFilament, double filamentDiameter)
    {
        String numberMaterial = String.valueOf(reelNumber + 1) + ":"
            + materialType.getFriendlyName();

        double remainingLengthMeters = remainingFilament / 1000d;
        double densityKGM2 = materialType.getDensity() * 1000d;
        double crossSectionM2 = Math.PI * filamentDiameter * filamentDiameter / 4d * 1e-6;
        double remainingWeightG = remainingLengthMeters * crossSectionM2 * densityKGM2 * 1000d;
        String remaining = ((int) remainingLengthMeters) + "m / " + ((int) remainingWeightG)
            + "g " + Lookup.i18n("materialComponent.remaining");

        showDetails(numberMaterial, remaining, materialColourString, colour);
    }

    private void showDetails(String numberMaterial, String materialRemainingString,
        String materialColourString, Color colour)
    {
        reelNumberMaterial.setText(numberMaterial);
        materialRemaining.setText(materialRemainingString);
        String colourString = colourToString(colour);
        reelNumberMaterial.setStyle("-fx-fill: #" + colourString + ";");
        materialColourContainer.setStyle("-fx-background-color: #" + colourString + ";");
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
     * Indicate that the reel is not formatted
     */
    private void showReelNotFormatted()
    {
        String reelNotFormattedString = Lookup.i18n("smartReelProgrammer.reelNotFormatted");
        String notAvailable = Lookup.i18n("smartReelProgrammer.notAvailable");
        String error = Lookup.i18n("smartReelProgrammer.error");
        showDetails("1:" + error, notAvailable, reelNotFormattedString, Color.BLACK);

    }

    private void showFilamentNotLoaded()
    {
        String pleaseCreateAProfile = Lookup.i18n("smartReelProgrammer.pleaseCreateAProfile");
        String filamentNotLoadedString = Lookup.i18n("smartReelProgrammer.noReelLoaded");
        showDetails("1:", pleaseCreateAProfile, filamentNotLoadedString, Color.BLACK);
    }
    
    private void showReelNotLoaded()
    {
        setReelType(ReelType.SOLID_QUESTION);
        String unknown = "Unknown"; // Lookup.i18n("smartReelProgrammer.pleaseCreateAProfile");
        String noReelLoaded = Lookup.i18n("smartReelProgrammer.noReelLoaded");
        showDetails((1 + extruderNumber) + ":", unknown, noReelLoaded, Color.BLACK);
    }    

    private void setReelColourString(String colourString)
    {
        reelSVGRobox.setStyle("-fx-fill: #" + colourString + ";");
        reelSVGGears.setStyle("-fx-fill: #" + colourString + ";");
        reelSVGSolid.setStyle("-fx-fill: #" + colourString + ";");
    }
    
    /**
     * Visually indicate that this component is selected.
     */
    public void select(boolean selected)
    {
        this.selected = selected;
        anchorPane.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, selected);
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
        if (this.printer == printer) {
            updateGUIForModeAndPrinterExtruder();
        }
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
         if (this.printer == printer) {
            updateGUIForModeAndPrinterExtruder();
        }
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
         if (this.printer == printer) {
            updateGUIForModeAndPrinterExtruder();
        }
    }    

//        private void unbindReelExtruderProperties(int reelNumber, Reel reel)
//    {
//        if (reel != null)
//        {
//            reel.friendlyFilamentNameProperty().removeListener(reelListener);
//            reel.displayColourProperty().removeListener(reelListener);
//            reel.remainingFilamentProperty().removeListener(reelListener);
//            reel.diameterProperty().removeListener(reelListener);
//            reel.materialProperty().removeListener(reelListener);
//        }
//    }
//    
//    private void bindReelExtruderProperties(int reelNumber, Reel reel)
//    {
//        if (reel != null)
//        {
//            reelListener = (ObservableValue<? extends Object> observable, Object oldValue, Object newValue) ->
//            {
//                updateReelExtruderMaterial(reelNumber, reel);
//            };
//            reel.friendlyFilamentNameProperty().addListener(reelListener);
//            reel.displayColourProperty().addListener(reelListener);
//            reel.remainingFilamentProperty().addListener(reelListener);
//            reel.diameterProperty().addListener(reelListener);
//            reel.materialProperty().addListener(reelListener);
//        }
//    } 
    
}
