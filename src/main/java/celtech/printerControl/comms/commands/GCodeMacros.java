package celtech.printerControl.comms.commands;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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

        String cleanedMacroName = macroName.replaceFirst(macroDefinitionString, "").trim();
        appendMacroContents(contents, parentMacros, cleanedMacroName);

        return contents;
    }

    /**
     *
     * @param macroName
     * @return
     */
    private static ArrayList<String> appendMacroContents(ArrayList<String> contents,
        final ArrayList<String> parentMacros,
        String macroName) throws IOException, MacroLoadException
    {
        if (!parentMacros.contains(macroName))
        {
            steno.debug("Processing macro: " + macroName);
            contents.add(";");
            contents.add("; Macro Start - " + macroName);
            contents.add(";");

            parentMacros.add(macroName);

            FileReader fileReader = null;

            try
            {
                fileReader = new FileReader(GCodeMacros.getFilename(macroName));
                Scanner scanner = new Scanner(fileReader);

                while (scanner.hasNextLine())
                {
                    String line = scanner.nextLine();
                    line = line.trim();

                    if (isMacroExecutionDirective(line))
                    {
                        String[] parts = line.split(":");
                        if (parts.length == 2)
                        {
                            String subMacroName = parts[1].trim();
                            steno.debug("Sub-macro " + subMacroName + " detected");

                            appendMacroContents(contents, parentMacros, subMacroName);
                        } else
                        {
                            steno.error("Saw macro directive but couldn't understand it: " + line);
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
}
