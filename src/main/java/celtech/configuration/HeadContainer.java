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
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class HeadContainer
{

    private static final Stenographer steno = StenographerFactory.getStenographer(HeadContainer.class.getName());
    private static HeadContainer instance = null;
    private static final ObservableList<Head> completeHeadList = FXCollections.observableArrayList();
    private static final ObservableMap<String, Head> completeHeadMap = FXCollections.observableHashMap();

    private HeadContainer()
    {
        File applicationHeadDirHandle = new File(ApplicationConfiguration.getApplicationHeadDirectory());
        File[] applicationheads = applicationHeadDirHandle.listFiles(new HeadFileFilter());
        ArrayList<Head> filaments = ingestHeads(applicationheads);
        completeHeadList.addAll(filaments);
    }

    private ArrayList<Head> ingestHeads(File[] userheads)
    {
        ArrayList<Head> headList = new ArrayList<>();

        for (File headFile : userheads)
        {
            try
            {
                Properties filamentProperties = new Properties();
                filamentProperties.load(new FileInputStream(headFile));

                String name = filamentProperties.getProperty("name");
                String headID = filamentProperties.getProperty("headID");
                String maximumTemperatureString = filamentProperties.getProperty("maximum_temperature_C");
                String betaString = filamentProperties.getProperty("beta");
                String tcalString = filamentProperties.getProperty("tcal");
                String nozzle1_X_offsetString = filamentProperties.getProperty("nozzle1_X_offset");
                String nozzle1_Y_offsetString = filamentProperties.getProperty("nozzle1_Y_offset");
                String nozzle1_Z_offsetString = filamentProperties.getProperty("nozzle1_Z_offset");
                String nozzle1_B_offsetString = filamentProperties.getProperty("nozzle1_B_offset");
                String nozzle2_X_offsetString = filamentProperties.getProperty("nozzle2_X_offset");
                String nozzle2_Y_offsetString = filamentProperties.getProperty("nozzle2_Y_offset");
                String nozzle2_Z_offsetString = filamentProperties.getProperty("nozzle2_Z_offset");
                String nozzle2_B_offsetString = filamentProperties.getProperty("nozzle2_B_offset");

                if (name != null
                        && headID != null
                        && maximumTemperatureString != null
                        && betaString != null
                        && tcalString != null
                        && nozzle1_X_offsetString != null
                        && nozzle1_Y_offsetString != null
                        && nozzle1_Z_offsetString != null
                        && nozzle1_B_offsetString != null
                        && nozzle2_X_offsetString != null
                        && nozzle2_Y_offsetString != null
                        && nozzle2_Z_offsetString != null
                        && nozzle2_B_offsetString != null)
                {
                    try
                    {
                        float maximumTemperature = Float.valueOf(maximumTemperatureString);
                        float beta = Float.valueOf(betaString);
                        float tcal = Float.valueOf(tcalString);
                        float nozzle1_X_offset = Float.valueOf(nozzle1_X_offsetString);
                        float nozzle1_Y_offset = Float.valueOf(nozzle1_Y_offsetString);
                        float nozzle1_Z_offset = Float.valueOf(nozzle1_Z_offsetString);
                        float nozzle1_B_offset = Float.valueOf(nozzle1_B_offsetString);
                        float nozzle2_X_offset = Float.valueOf(nozzle2_X_offsetString);
                        float nozzle2_Y_offset = Float.valueOf(nozzle2_Y_offsetString);
                        float nozzle2_Z_offset = Float.valueOf(nozzle2_Z_offsetString);
                        float nozzle2_B_offset = Float.valueOf(nozzle2_B_offsetString);

                        Head newHead = new Head(headID,
                                name,
                                maximumTemperature,
                                beta,
                                tcal,
                                nozzle1_X_offset,
                                nozzle1_Y_offset,
                                nozzle1_Z_offset,
                                nozzle1_B_offset,
                                nozzle2_X_offset,
                                nozzle2_Y_offset,
                                nozzle2_Z_offset,
                                nozzle2_B_offset);

                        headList.add(newHead);
                        completeHeadMap.put(headID, newHead);

                    } catch (NumberFormatException ex)
                    {
                        steno.error("Failed to parse filament file " + headFile.getAbsolutePath());
                    }

                }
            } catch (IOException ex)
            {
                steno.error("Error loading head " + headFile.getAbsolutePath());
            }
        }

        return headList;
    }

    /**
     *
     * @return
     */
    public static HeadContainer getInstance()
    {
        if (instance == null)
        {
            instance = new HeadContainer();
        }

        return instance;
    }

    /**
     *
     * @param filamentID
     * @return
     */
    public static Head getHeadByID(String filamentID)
    {
        if (instance == null)
        {
            HeadContainer.getInstance();
        }

        Head returnedHead = completeHeadMap.get(filamentID);
        return returnedHead;

    }

    /**
     *
     * @return
     */
    public static ObservableList<Head> getCompleteHeadList()
    {
        if (instance == null)
        {
            instance = new HeadContainer();
        }

        return completeHeadList;
    }
}
