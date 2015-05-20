package celtech.printerControl.comms.commands;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.utils.SystemUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class GCodeMacros
{

    private static final String safetyFeaturesOnDirectory = "Safety_Features_ON";
    private static final String safetyFeaturesOffDirectory = "Safety_Features_OFF";

    private static final Stenographer steno = StenographerFactory.getStenographer(GCodeMacros.class.
        getName());
    private static final String macroDefinitionString = "Macro:";

    /**
     *
     * @param macroName - this can include the macro execution directive at the start of the line
     * @return
     * @throws java.io.IOException
     * @throws celtech.printerControl.comms.commands.MacroLoadException
     */
    public static ArrayList<String> getMacroContents(String macroName) throws IOException, MacroLoadException
    {
        ArrayList<String> contents = new ArrayList<>();
        ArrayList<String> parentMacros = new ArrayList<>();

        if (Lookup.getUserPreferences().isSafetyFeaturesOn())
        {
            contents.add("; Printed with safety features ON");
        } else
        {
            contents.add("; Printed with safety features OFF");
        }

        appendMacroContents(contents, parentMacros, macroName);

        return contents;
    }
    
    private static String cleanMacroName(String macroName)
    {
        return macroName.replaceFirst(macroDefinitionString, "").trim();
    }

    /**
     *
     * @param macroName
     * @return
     */
    private static ArrayList<String> appendMacroContents(ArrayList<String> contents,
        final ArrayList<String> parentMacros,
        final String macroName) throws IOException, MacroLoadException
    {
        String cleanedMacroName = cleanMacroName(macroName);

        if (!parentMacros.contains(cleanedMacroName))
        {
            steno.debug("Processing macro: " + cleanedMacroName);
            contents.add(";");
            contents.add("; Macro Start - " + cleanedMacroName);
            contents.add(";");

            parentMacros.add(cleanedMacroName);

            FileReader fileReader = null;

            try
            {
                fileReader = new FileReader(GCodeMacros.getFilename(cleanedMacroName));
                Scanner scanner = new Scanner(fileReader);

                while (scanner.hasNextLine())
                {
                    String line = scanner.nextLine();
                    line = line.trim();

                    if (isMacroExecutionDirective(line))
                    {
                        String subMacroName = line.replaceFirst(macroDefinitionString, "").trim();
                        if (subMacroName != null)
                        {
                            steno.debug("Sub-macro " + subMacroName + " detected");

                            appendMacroContents(contents, parentMacros, subMacroName);
                        }
                    } else
                    {
                        contents.add(line);
                    }
                }
            } catch (FileNotFoundException ex)
            {
                throw new MacroLoadException("Failure to load contents of macro file " + macroName
                    + " : " + ex.getMessage());
            } finally
            {
                if (fileReader != null)
                {
                    fileReader.close();
                }
            }

            parentMacros.remove(macroName);
        } else
        {
            StringBuilder messageBuffer = new StringBuilder();
            messageBuffer.append("Macro circular dependency detected in chain: ");
            parentMacros.forEach(macro ->
            {
                messageBuffer.append(macro);
                messageBuffer.append("->");
            });
            messageBuffer.append(macroName);

            throw new MacroLoadException(messageBuffer.toString());
        }

        contents.add(";");
        contents.add("; Macro End - " + macroName);
        contents.add(";");

        return contents;
    }

    /**
     *
     * @param macroName
     * @return
     */
    public static String getFilename(String macroName)
    {
        StringBuilder fileNameBuffer = new StringBuilder();

        fileNameBuffer.append(ApplicationConfiguration.getCommonApplicationDirectory());
        fileNameBuffer.append(ApplicationConfiguration.macroFileSubpath);
        if (Lookup.getUserPreferences().isSafetyFeaturesOn())
        {
            fileNameBuffer.append(safetyFeaturesOnDirectory);
            fileNameBuffer.append(File.separator);
        } else
        {
            fileNameBuffer.append(safetyFeaturesOffDirectory);
            fileNameBuffer.append(File.separator);
        }
        fileNameBuffer.append(macroName);
        fileNameBuffer.append(ApplicationConfiguration.macroFileExtension);

        return fileNameBuffer.toString();
    }

    public static boolean isMacroExecutionDirective(String input)
    {
        return input.startsWith(macroDefinitionString);
    }

    private String getMacroNameFromDirective(String macroDirective)
    {
        String macroName = null;
        String[] parts = macroDirective.split(":");
        if (parts.length == 2)
        {
            macroName = parts[1].trim();
        } else
        {
            steno.error("Saw macro directive but couldn't understand it: " + macroDirective);
        }
        return macroName;
    }

    public static int getNumberOfOperativeLinesInMacro(String macroDirective)
    {
        int linesInMacro = 0;
        String macro = cleanMacroName(macroDirective);
        if (macro != null)
        {
            try
            {
                List<String> contents = getMacroContents(macro);
                for (String line : contents)
                {
                    if (line.trim().startsWith(";") == false && line.equals("") == false)
                    {
                        linesInMacro++;
                    }
                }
            } catch (IOException | MacroLoadException ex)
            {
                steno.error("Error trying to get number of lines in macro " + macro);
            }
        }

        return linesInMacro;
    }
}
