/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.material;

import celtech.Lookup;
import celtech.configuration.Filament;
import celtech.configuration.MaterialType;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.coreUI.StandardColours;
import static celtech.printerControl.comms.commands.ColourStringConverter.colourToString;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
import javafx.scene.text.TextFlow;

/**
 *
 * @author tony
 */
public class MaterialComponent extends Pane implements PrinterListChangesListener
{

    private Printer printer;
    private int extruderNumber;
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
    private Text materialColour1;

    @FXML
    private Text materialColour2;

    @FXML
    private Text materialRemaining;

    @FXML
    private TextFlow materialColourContainer;

    @FXML
    private ComboBox<Filament> cmbMaterials;

    private Filament filamentInUse = FilamentContainer.UNKNOWN_FILAMENT;

    public MaterialComponent()
    {
        // Should only be called from scene builder
    }

    public MaterialComponent(Printer printer, int extruderNumber)
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

        this.printer = printer;
        this.extruderNumber = extruderNumber;
        setupComboBox();

        Lookup.getPrinterListChangesNotifier().addListener(this);

        Lookup.getUserPreferences().advancedModeProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    repopulateCmbMaterials();
                });

        setUpFilamentLoadedListener();
        configureDisplay();
    }

    private boolean filamentLoaded()
    {
        return printer.extrudersProperty().get(extruderNumber).filamentLoadedProperty().get();
    }

    /**
     * Initialisation code
     */
    private ObservableList<Filament> comboItems;

    /**
     * Set up the materials combo box. This displays a list of filaments and can
     * also display an "Unknown" (string) option when required.
     */
    private void setupComboBox()
    {
        cmbMaterials.setCellFactory((ListView<Filament> param) -> new FilamentCell());
        cmbMaterials.setButtonCell(cmbMaterials.getCellFactory().call(null));

        repopulateCmbMaterials();

        comboItems.add(0, FilamentContainer.UNKNOWN_FILAMENT);
        cmbMaterials.setValue(FilamentContainer.UNKNOWN_FILAMENT);

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
    }

    public void whenMaterialSelected(ActionEvent e)
    {
        filamentInUse = cmbMaterials.getValue();
        if (filamentInUse != FilamentContainer.UNKNOWN_FILAMENT)
        {
            Platform.runLater(() ->
            {
                comboItems.remove(FilamentContainer.UNKNOWN_FILAMENT);
            });
        }
        configureDisplay();
        if (Lookup.getSelectedPrinterProperty().get() != null)
        {
            Lookup.getSelectedPrinterProperty().get().overrideFilament(extruderNumber, filamentInUse);
        } else
        {
            System.out.println("Called with null filament");
        }
    }

    private void setUpFilamentLoadedListener()
    {
        if (printer != null && printer.extrudersProperty().get(extruderNumber) != null)
        {
            printer.extrudersProperty().get(extruderNumber).filamentLoadedProperty().addListener(
                    (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                    {
                        configureDisplay();
                    });
        }
    }

    private void repopulateCmbMaterials()
    {
        ObservableList<Filament> allFilaments = FXCollections.observableArrayList();
        ObservableList<Filament> userFilaments = FXCollections.observableArrayList();

        try
        {

            if (Lookup.getUserPreferences().isAdvancedMode())
            {
                allFilaments.addAll(filamentContainer.getCompleteFilamentList());
                userFilaments.addAll(filamentContainer.getUserFilamentList());
            } else
            {
                allFilaments.addAll(filamentContainer.getAppFilamentList());
            }
        } catch (NoClassDefFoundError exception)
        {
            // this should only happen in SceneBuilder            
        }

        Filament currentVal = cmbMaterials.getValue();

        List<Filament> filamentsList = new ArrayList<>();
        filamentsList.addAll(allFilaments);

        comboItems = FXCollections.observableArrayList(filamentsList);
        cmbMaterials.setItems(null);
        cmbMaterials.setItems(comboItems);

        if (comboItems.contains(currentVal))
        {
            cmbMaterials.setValue(currentVal);
        } else if (comboItems.contains(FilamentContainer.UNKNOWN_FILAMENT))
        {
            cmbMaterials.setValue(FilamentContainer.UNKNOWN_FILAMENT);
        } else
        {
            cmbMaterials.setValue(null);
        }
    }

    private void configureDisplay()

    {
        materialColourContainer.setVisible(true);
        if (printer.reelsProperty().containsKey(extruderNumber))
        {
            //Reel is attached
            cmbMaterials.setVisible(false);
            setReelType(ReelType.ROBOX);
            Reel reel = printer.reelsProperty().get(extruderNumber);
            filamentInUse = filamentContainer.getFilamentByID(reel.filamentIDProperty().get());
            materialRemaining.setVisible(true);
        } else if (printer.extrudersProperty().get(extruderNumber).filamentLoadedProperty().get())
        {
            //Loaded but no reel attached
            cmbMaterials.setVisible(true);
            materialRemaining.setVisible(false);
        } else
        {
            //No reel and not loaded
            cmbMaterials.setVisible(false);
            materialRemaining.setVisible(false);
            resetFilament();
        }

        if (filamentInUse == FilamentContainer.UNKNOWN_FILAMENT
                && !printer.extrudersProperty().get(extruderNumber).filamentLoadedProperty().get())
        {
            svgLoaded.setVisible(false);
            setReelType(ReelType.SOLID_CROSS);
            String filamentNotLoaded = Lookup.i18n("materialComponent.filamentNotLoaded");
            showDetails((1 + extruderNumber) + ":", "", filamentNotLoaded, Color.BLACK, false, false);
        } else
        {
            Float remainingFilament = 0f;
            Float diameter = 0f;
            if (filamentInUse == FilamentContainer.UNKNOWN_FILAMENT)
            {
                svgLoaded.setVisible(true);
                svgLoaded.setFill(Color.BLACK);
                setReelType(ReelType.SOLID_QUESTION);
                String materialUnknown = Lookup.i18n("materialComponent.materialUnknown");
                showDetails((1 + extruderNumber) + ":", "", materialUnknown,
                        Color.BLACK, true, true);
            } else
            {
                if (printer.reelsProperty().containsKey(extruderNumber))
                {
                    Reel reel = printer.reelsProperty().get(extruderNumber);
                    remainingFilament = reel.remainingFilamentProperty().get();
                    diameter = reel.diameterProperty().get();
                    if (filamentInUse.isMutable())
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
                setMaterial(extruderNumber, filamentInUse.getMaterial(),
                        filamentInUse.getFriendlyFilamentName(),
                        filamentInUse.getDisplayColourProperty().get(),
                        remainingFilament,
                        diameter, filamentLoaded());
            }
        }
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

        String numberMaterial = "";
        double densityKGM3 = 1;

        if (materialType != null)
        {
            numberMaterial = String.valueOf(reelNumber + 1) + ":"
                    + materialType.getFriendlyName();
            densityKGM3 = materialType.getDensity() * 1000d;
        } else
        {
            numberMaterial = String.valueOf(reelNumber + 1) + ":";
        }

        double remainingLengthMeters = remainingFilament / 1000d;
        if (remainingLengthMeters < 0)
        {
            remainingLengthMeters = 0;
        }
        double crossSectionM2 = Math.PI * filamentDiameter * filamentDiameter / 4d * 1e-6;
        double remainingWeightG = remainingLengthMeters * crossSectionM2 * densityKGM3 * 1000d;
        String remaining = ((int) remainingLengthMeters) + "m / " + ((int) remainingWeightG)
                + "g " + Lookup.i18n("materialComponent.remaining");

        showDetails(numberMaterial, remaining, materialColourString, colour, filamentLoaded, true);
    }

    private void showDetails(String numberMaterial, String materialRemainingString,
            String materialColourString, Color colour, boolean filamentLoaded,
            boolean dualWeightTitle)
    {

        svgLoaded.setVisible(filamentLoaded);

        reelNumberMaterial.setText(numberMaterial);
        materialRemaining.setText(materialRemainingString);
        String colourString = colourToString(colour);
        reelNumberMaterial.setStyle("-fx-fill: #" + colourString + ";");
        materialColourContainer.setStyle("-fx-background-color: #" + colourString + ";");
        svgLoaded.setFill(StandardColours.HIGHLIGHT_ORANGE);
        setReelColourString(colourString);

        int endOfManufacturerSection = materialColourString.indexOf(' ');
        if (dualWeightTitle && endOfManufacturerSection > 0 && endOfManufacturerSection < materialColourString.length() - 1)
        {
            materialColour1.setText(materialColourString.substring(0, endOfManufacturerSection));
            materialColour2.setText(materialColourString.substring(endOfManufacturerSection));
        } else
        {
            materialColour1.setText("");
            materialColour2.setText(materialColourString);
        }

        if (colour.getBrightness() < 0.5)
        {
            materialColour1.setStyle("-fx-fill:white;");
            materialColour2.setStyle("-fx-fill:white;");
        } else
        {
            materialColour1.setStyle("-fx-fill:black;");
            materialColour2.setStyle("-fx-fill:black;");
        }
    }

    private void setReelColourString(String colourString)
    {
        reelSVGRobox.setStyle("-fx-fill: #" + colourString + ";");
        reelSVGGears.setStyle("-fx-fill: #" + colourString + ";");
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
            configureDisplay();
        }
    }

    private void resetFilament()
    {
        if (!comboItems.contains(FilamentContainer.UNKNOWN_FILAMENT))
        {
            comboItems.add(0, FilamentContainer.UNKNOWN_FILAMENT);
            cmbMaterials.setValue(FilamentContainer.UNKNOWN_FILAMENT);
        }
        filamentInUse = FilamentContainer.UNKNOWN_FILAMENT;
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
        if (this.printer == printer)
        {
            resetFilament();
            configureDisplay();
        }
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
        if (this.printer == printer)
        {
            configureDisplay();
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
