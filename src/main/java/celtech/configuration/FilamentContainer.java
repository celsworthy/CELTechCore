/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.configuration;

import celtech.utils.DeDuplicator;
import celtech.utils.FXUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author ianhudson
 */
public class FilamentContainer
{
    
    private static final Stenographer steno = StenographerFactory.getStenographer(FilamentContainer.class.getName());
    private static FilamentContainer instance = null;
    private static final ObservableList<Filament> appFilamentList = FXCollections.observableArrayList();
    private static final ObservableList<Filament> userFilamentList = FXCollections.observableArrayList();
    private static final ObservableList<Filament> completeFilamentList = FXCollections.observableArrayList();
    private static final ObservableMap<String, Filament> completeFilamentMap = FXCollections.observableHashMap();

    public static final Filament createNewFilament = new Filament(null, null, null,
                                                                  0, 0, 0, 0, 0, 0, 0, 0, Color.ALICEBLUE, false);
    
    private static final String nameProperty = "name";
    private static final String materialProperty = "material";
    private static final String reelIDProperty = "reelID";
    private static final String diameterProperty = "diameter_mm";
    private static final String filamentMultiplierProperty = "filament_multiplier";
    private static final String feedRateMultiplierProperty = "feed_rate_multiplier";
    private static final String ambientTempProperty = "ambient_temperature_C";
    private static final String firstLayerBedTempProperty = "first_layer_bed_temperature_C";
    private static final String bedTempProperty = "bed_temperature_C";
    private static final String firstLayerNozzleTempProperty = "first_layer_nozzle_temperature_C";
    private static final String nozzleTempProperty = "nozzle_temperature_C";
    private static final String displayColourProperty = "display_colour";
    
    private static final StringConverter<Integer> intConverter = FXUtils.getIntConverter();
    private static final StringConverter<Float> floatConverter = FXUtils.getFloatConverter(2);
    
    private FilamentContainer()
    {
        loadFilamentData();
    }

    /**
     *
     * @param filament
     * @return
     */
    public static String constructFilePath(Filament filament)
    {
        return ApplicationConfiguration.getUserFilamentDirectory() + filament.getFriendlyFilamentName() + "-" + filament.getMaterial().getFriendlyName() + ApplicationConfiguration.filamentFileExtension;
    }
    
    private static void loadFilamentData()
    {
        completeFilamentList.clear();
        appFilamentList.clear();
        userFilamentList.clear();
        
        File applicationFilamentDirHandle = new File(ApplicationConfiguration.getApplicationFilamentDirectory());
        File[] applicationfilaments = applicationFilamentDirHandle.listFiles(new FilamentFileFilter());
        ArrayList<Filament> filaments = ingestFilaments(applicationfilaments, false);
        appFilamentList.addAll(filaments);
        completeFilamentList.addAll(filaments);
        
        File userFilamentDirHandle = new File(ApplicationConfiguration.getUserFilamentDirectory());
        File[] userfilaments = userFilamentDirHandle.listFiles(new FilamentFileFilter());
        filaments = ingestFilaments(userfilaments, true);
        completeFilamentList.addAll(filaments);
        userFilamentList.addAll(filaments);
    }
    
    private static ArrayList<Filament> ingestFilaments(File[] filamentFiles, boolean filamentsAreMutable)
    {
        ArrayList<Filament> filamentList = new ArrayList<>();
        
        for (File filamentFile : filamentFiles)
        {
            try
            {
                Properties filamentProperties = new Properties();
                filamentProperties.load(new FileInputStream(filamentFile));
                
                String filename = filamentFile.getName().trim();
                String name = filamentProperties.getProperty(nameProperty).trim();
                String reelID = filamentProperties.getProperty(reelIDProperty).trim();
                String material = filamentProperties.getProperty(materialProperty).trim();
                String diameterString = filamentProperties.getProperty(diameterProperty).trim();
                String filamentMultiplierString = filamentProperties.getProperty(filamentMultiplierProperty).trim();
                String feedRateMultiplierString = filamentProperties.getProperty(feedRateMultiplierProperty).trim();
                String ambientTempString = filamentProperties.getProperty(ambientTempProperty).trim();
                String firstLayerBedTempString = filamentProperties.getProperty(firstLayerBedTempProperty).trim();
                String bedTempString = filamentProperties.getProperty(bedTempProperty).trim();
                String firstLayerNozzleTempString = filamentProperties.getProperty(firstLayerNozzleTempProperty).trim();
                String nozzleTempString = filamentProperties.getProperty(nozzleTempProperty).trim();
                String displayColourString = filamentProperties.getProperty(displayColourProperty).trim();
                
                if (name != null
                        && material != null
                        && reelID != null
                        && diameterString != null
                        && feedRateMultiplierString != null
                        && filamentMultiplierString != null
                        && ambientTempString != null
                        && firstLayerBedTempString != null
                        && bedTempString != null
                        && firstLayerNozzleTempString != null
                        && nozzleTempString != null
                        && displayColourString != null)
                {
                    try
                    {
                        float diameter = Float.valueOf(diameterString);
                        float filamentMultiplier = Float.valueOf(filamentMultiplierString);
                        float feedRateMultiplier = Float.valueOf(feedRateMultiplierString);
                        int ambientTemp = Integer.valueOf(ambientTempString);
                        int firstLayerBedTemp = Integer.valueOf(firstLayerBedTempString);
                        int bedTemp = Integer.valueOf(bedTempString);
                        int firstLayerNozzleTemp = Integer.valueOf(firstLayerNozzleTempString);
                        int nozzleTemp = Integer.valueOf(nozzleTempString);
                        Color colour = Color.web(displayColourString);
                        MaterialType selectedMaterial = MaterialType.valueOf(material);
                        
                        Filament newFilament = new Filament(
                                name,
                                selectedMaterial,
                                reelID,
                                diameter,
                                filamentMultiplier,
                                feedRateMultiplier,
                                ambientTemp,
                                firstLayerBedTemp,
                                bedTemp,
                                firstLayerNozzleTemp,
                                nozzleTemp,
                                colour,
                                filamentsAreMutable);
                        
                        filamentList.add(newFilament);
                        completeFilamentMap.put(reelID, newFilament);
                        
                    } catch (NumberFormatException ex)
                    {
                        steno.error("Failed to parse filament file " + filamentFile.getAbsolutePath());
                    }
                    
                }
            } catch (IOException ex)
            {
                steno.error("Error loading filament " + filamentFile.getAbsolutePath());
            }
        }
        
        return filamentList;
    }
    
    /**
     * Suggest a safe name for a new filament name based on the proposed name.
     * @param originalName
     * @return 
     */
    public static String suggestNonDuplicateName(String proposedName) {
        List<String> currentFilamentNames = new ArrayList<>();
        for (Filament filament : completeFilamentList)
        {
            currentFilamentNames.add(filament.getFriendlyFilamentName());
        }
        return DeDuplicator.suggestNonDuplicateName(proposedName, currentFilamentNames);
    }
    
    private static Optional<String> getCurrentFileNameForFilamentID(String filamentID) {
        for (Filament filament : completeFilamentList)
        {
            if (filament.getFilamentID().equals(filamentID)) {
                return Optional.of(constructFilePath(filament));
            }
        }
        return Optional.empty();
    }

    /**
     *
     * @param filament
     * @return
     */
    public static boolean saveFilament(Filament filament)
    {
        boolean success = false;
        
        try
        {
            Properties filamentProperties = new Properties();
            
            filamentProperties.setProperty(nameProperty, filament.getFriendlyFilamentName());
            filamentProperties.setProperty(materialProperty, filament.getMaterial().getFriendlyName());
            if (filament.getFilamentID() == null)
            {
                String userReelID = Filament.generateUserReelID();
                filament.setFilamentID(userReelID);
                filamentProperties.setProperty(reelIDProperty, userReelID);
            } else
            {
                filamentProperties.setProperty(reelIDProperty, filament.getFilamentID());
            }
            filamentProperties.setProperty(diameterProperty, floatConverter.toString(filament.getFilamentDiameter()));
            filamentProperties.setProperty(filamentMultiplierProperty, floatConverter.toString(filament.getFilamentMultiplier()));
            filamentProperties.setProperty(feedRateMultiplierProperty, floatConverter.toString(filament.getFeedRateMultiplier()));
            filamentProperties.setProperty(ambientTempProperty, intConverter.toString(filament.getAmbientTemperature()));
            filamentProperties.setProperty(firstLayerBedTempProperty, intConverter.toString(filament.getFirstLayerBedTemperature()));
            filamentProperties.setProperty(bedTempProperty, intConverter.toString(filament.getBedTemperature()));
            filamentProperties.setProperty(firstLayerNozzleTempProperty, intConverter.toString(filament.getFirstLayerNozzleTemperature()));
            filamentProperties.setProperty(nozzleTempProperty, intConverter.toString(filament.getNozzleTemperature()));
            
            String webColour = String.format("#%02X%02X%02X",
                                             (int) (filament.getDisplayColour().getRed() * 255),
                                             (int) (filament.getDisplayColour().getGreen() * 255),
                                             (int) (filament.getDisplayColour().getBlue() * 255));
            filamentProperties.setProperty(displayColourProperty, webColour);
            
            String filename = constructFilePath(filament);
            Optional<String> previousFileName = getCurrentFileNameForFilamentID(filament.getFilamentID());
            
            File filamentFile = new File(filename);
            filamentProperties.store(new FileOutputStream(filamentFile), "Robox data");
            if (previousFileName.isPresent()) {
                Files.delete(Paths.get(previousFileName.get()));
            }
            loadFilamentData();
        } catch (IOException ex)
        {
            steno.error("Error whilst storing filament file " + filament.getFileName());
        }
        return success;
    }

    /**
     *
     * @param filamentToSave
     */
    public static void deleteFilament(Filament filamentToSave)
    {
        File filamentToDelete = new File(constructFilePath(filamentToSave));
        FileUtils.deleteQuietly(filamentToDelete);
        loadFilamentData();
    }

    /**
     *
     * @return
     */
    public static FilamentContainer getInstance()
    {
        if (instance == null)
        {
            instance = new FilamentContainer();
        }
        
        return instance;
    }

    /**
     *
     * @param filamentID
     * @return
     */
    public static Filament getFilamentByID(String filamentID)
    {
        Filament returnedFilament = null;
        
        if (instance == null)
        {
            FilamentContainer.getInstance();
        }
        
        if (filamentID != null)
        {
            returnedFilament = completeFilamentMap.get(filamentID);
            if (returnedFilament == null)
            {
                //Try replacing dashes with underscores...
                returnedFilament = completeFilamentMap.get(filamentID.replaceAll("-", "_"));
            }
        }
        return returnedFilament;
    }

    /**
     *
     * @return
     */
    public static ObservableList<Filament> getCompleteFilamentList()
    {
        if (instance == null)
        {
            instance = new FilamentContainer();
        }
        
        return completeFilamentList;
    }

    /**
     *
     * @return
     */
    public static ObservableList<Filament> getUserFilamentList()
    {
        if (instance == null)
        {
            instance = new FilamentContainer();
        }
        
        return userFilamentList;
    }

    /**
     *
     * @return
     */
    public static ObservableList<Filament> getAppFilamentList()
    {
        if (instance == null)
        {
            instance = new FilamentContainer();
        }
        
        return appFilamentList;
    }
}
