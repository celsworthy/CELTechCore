/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands;

import celtech.configuration.ApplicationConfiguration;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public enum GCodeMacros
{

    LEVEL_GANTRY("level_gantry"),
    EJECT_ABS("eject_ABS"),
    EJECT_PLA("eject_PLA"),
    PREHEAT_ABS("preheat_ABS"),
    PREHEAT_PLA("preheat_PLA"),
    LEVEL_Y("level_Y"),
    TWELVE_POINT_BED_CHECK("12_point_bed_check"),
    X_TEST("x_test"),
    Y_TEST("y_test"),
    Z_TEST("z_test"),
    SPEED_TEST("speed_test"),
    PRE_PRINT("before_print"),
    POST_PRINT("after_print"),
    PAUSE_PRINT("pause_print"),
    RESUME_PRINT("resume_print"),
    ABORT_PRINT("abort_print");

    private static final Stenographer steno = StenographerFactory.getStenographer(GCodeMacros.class.getName());

    private final String macroFilename;

    private GCodeMacros(String macroFilename)
    {
        this.macroFilename = macroFilename;
    }

    public String getMacroFilename()
    {
        return macroFilename;
    }

    public static ArrayList<String> getMacroContents(String macroFile)
    {
        ArrayList<String> contents = new ArrayList<>();

        String macrofile = ApplicationConfiguration.getApplicationInstallDirectory(null) + ApplicationConfiguration.macroFileSubpath + macroFile + ApplicationConfiguration.macroFileExtension;

        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(macrofile)));
            String line;
            while ((line = reader.readLine()) != null)
            {
                contents.add(line);
            }

        } catch (FileNotFoundException ex)
        {
            steno.error("Couldn't load macro file " + macroFile);
        } catch (IOException ex)
        {
            steno.error("IO Error whilst loading macro file " + macroFile);
        }

        return contents;
    }

    public ArrayList<String> getMacroContents()
    {
        ArrayList<String> contents = new ArrayList<>();

        String macrofile = ApplicationConfiguration.getApplicationInstallDirectory(null) + ApplicationConfiguration.macroFileSubpath + macroFilename + ApplicationConfiguration.macroFileExtension;

        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(macrofile)));
            String line;
            while ((line = reader.readLine()) != null)
            {
                String strippedLine = line.replaceAll(";.*", "");
                contents.add(strippedLine);
            }

        } catch (FileNotFoundException ex)
        {
            steno.error("Couldn't load macro file " + macroFilename);
        } catch (IOException ex)
        {
            steno.error("IO Error whilst loading macro file " + macroFilename);
        }

        return contents;
    }

    public String getMacroContentsInOneLine()
    {
        StringBuilder output = new StringBuilder();

        String macrofile = ApplicationConfiguration.getApplicationInstallDirectory(null) + ApplicationConfiguration.macroFileSubpath + macroFilename + ApplicationConfiguration.macroFileExtension;

        try
        {
            boolean firstLine = true;
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(macrofile)));
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (!firstLine)
                {
                    output.append("\\n");
                }
                String strippedLine = line.replaceAll(";.*", "");
                output.append(strippedLine.toUpperCase().trim());
                firstLine = false;
            }

        } catch (FileNotFoundException ex)
        {
            steno.error("Couldn't load macro file " + macroFilename);
        } catch (IOException ex)
        {
            steno.error("IO Error whilst loading macro file " + macroFilename);
        }

        return output.toString();
    }
}
