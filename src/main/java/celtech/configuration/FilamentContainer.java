/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

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

    private FilamentContainer()
    {
        File applicationFilamentDirHandle = new File(ApplicationConfiguration.getApplicationFilamentDirectory());
        File[] applicationfilaments = applicationFilamentDirHandle.listFiles(new FilamentFileFilter());
        ArrayList<Filament> filaments = ingestFilaments(applicationfilaments);
        appFilamentList.addAll(filaments);
        completeFilamentList.addAll(filaments);

        File userFilamentDirHandle = new File(ApplicationConfiguration.getUserFilamentDirectory());
        File[] userfilaments = userFilamentDirHandle.listFiles(new FilamentFileFilter());
        filaments = ingestFilaments(userfilaments);
        completeFilamentList.addAll(filaments);
        userFilamentList.addAll(filaments);
    }

    private ArrayList<Filament> ingestFilaments(File[] userfilaments)
    {
        ArrayList<Filament> filamentList = new ArrayList<>();

        for (File filamentFile : userfilaments)
        {
            try
            {
                Properties filamentProperties = new Properties();
                filamentProperties.load(new FileInputStream(filamentFile));

                String name = filamentProperties.getProperty("name");
                String material = filamentProperties.getProperty("material");
                String diameterString = filamentProperties.getProperty("diameter_mm");
                String maxExtrusionRateString = filamentProperties.getProperty("max_extrusion_rate");
                String extrusionMultiplierString = filamentProperties.getProperty("extrusion_multiplier");
                String ambientTempString = filamentProperties.getProperty("ambient_temperature_C");
                String firstLayerBedTempString = filamentProperties.getProperty("first_layer_bed_temperature_C");
                String bedTempString = filamentProperties.getProperty("bed_temperature_C");
                String firstLayerNozzleTempString = filamentProperties.getProperty("first_layer_nozzle_temperature_C");
                String nozzleTempString = filamentProperties.getProperty("nozzle_temperature_C");
                String displayColourString = filamentProperties.getProperty("display_colour");

                if (name != null
                        && material != null
                        && diameterString != null
                        && maxExtrusionRateString != null
                        && extrusionMultiplierString != null
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
                        float maxExtrusionRate = Float.valueOf(maxExtrusionRateString);
                        float extrusionMultiplier = Float.valueOf(extrusionMultiplierString);
                        int ambientTemp = Integer.valueOf(ambientTempString);
                        int firstLayerBedTemp = Integer.valueOf(firstLayerBedTempString);
                        int bedTemp = Integer.valueOf(bedTempString);
                        int firstLayerNozzleTemp = Integer.valueOf(firstLayerNozzleTempString);
                        int nozzleTemp = Integer.valueOf(nozzleTempString);
                        Color colour = Color.web(displayColourString);

                        String filamentTypeCode = filamentFile.getName().replaceAll(ApplicationConfiguration.filamentFileExtension, "");

                        Filament newFilament = new Filament(filamentTypeCode,
                                name,
                                material,
                                diameter,
                                maxExtrusionRate,
                                extrusionMultiplier,
                                ambientTemp,
                                firstLayerBedTemp,
                                bedTemp,
                                firstLayerNozzleTemp,
                                nozzleTemp,
                                colour);

                        filamentList.add(newFilament);
                        completeFilamentMap.put(filamentTypeCode, newFilament);

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

    public static FilamentContainer getInstance()
    {
        if (instance == null)
        {
            instance = new FilamentContainer();
        }

        return instance;
    }

    public static Filament getFilamentByID(String filamentID)
    {
        if (instance == null)
        {
            FilamentContainer.getInstance();
        }

        Filament returnedFilament = completeFilamentMap.get(filamentID);
        if (returnedFilament == null)
        {
            //Try replacing dashes with underscores...
            returnedFilament = completeFilamentMap.get(filamentID.replaceAll("-", "_"));
        }
        return returnedFilament;

    }

    public static ObservableList<Filament> getCompleteFilamentList()
    {
        if (instance == null)
        {
            instance = new FilamentContainer();
        }

        return completeFilamentList;
    }

    public static ObservableList<Filament> getUserFilamentList()
    {
        if (instance == null)
        {
            instance = new FilamentContainer();
        }

        return userFilamentList;
    }

    public static ObservableList<Filament> getAppFilamentList()
    {
        if (instance == null)
        {
            instance = new FilamentContainer();
        }

        return appFilamentList;
    }
}
