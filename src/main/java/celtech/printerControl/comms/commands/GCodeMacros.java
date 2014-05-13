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
public class GCodeMacros
{

    private static final Stenographer steno = StenographerFactory.getStenographer(GCodeMacros.class.getName());

    public static ArrayList<String> getMacroContents(String macroFile)
    {
        ArrayList<String> contents = new ArrayList<>();

        String macrofile = ApplicationConfiguration.getCommonApplicationDirectory() + ApplicationConfiguration.macroFileSubpath + macroFile + ApplicationConfiguration.macroFileExtension;

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

    public static String getFilename(String macroName)
    {
        String macrofile = ApplicationConfiguration.getCommonApplicationDirectory() + ApplicationConfiguration.macroFileSubpath + macroName + ApplicationConfiguration.macroFileExtension;
        return macrofile;
    }

//    public String getMacroContentsInOneLine()
//    {
//        StringBuilder output = new StringBuilder();
//
//        String macrofile = ApplicationConfiguration.getApplicationInstallDirectory(null) + ApplicationConfiguration.macroFileSubpath + macroFilename + ApplicationConfiguration.macroFileExtension;
//
//        try
//        {
//            boolean firstLine = true;
//            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(macrofile)));
//            String line;
//            while ((line = reader.readLine()) != null)
//            {
//                if (!firstLine)
//                {
//                    output.append("\\n");
//                }
//                String strippedLine = line.replaceAll(";.*", "");
//                output.append(strippedLine.toUpperCase().trim());
//                firstLine = false;
//            }
//
//        } catch (FileNotFoundException ex)
//        {
//            steno.error("Couldn't load macro file " + macroFilename);
//        } catch (IOException ex)
//        {
//            steno.error("IO Error whilst loading macro file " + macroFilename);
//        }
//
//        return output.toString();
//    }
}
