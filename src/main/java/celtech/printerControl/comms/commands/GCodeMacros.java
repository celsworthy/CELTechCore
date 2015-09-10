package celtech.printerControl.comms.commands;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.printerControl.model.Head;
import celtech.utils.SystemUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.io.FileUtils;

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

    public interface FilenameEncoder
    {

        public String getFilenameCode();
    }

    public enum SafetyIndicator implements FilenameEncoder
    {

        // Safeties off
        SAFETIES_OFF("U"),
        // Safeties on
        SAFETIES_ON("S"),
        DONT_CARE(null);

        private final String filenameCode;

        private SafetyIndicator(String filenameCode)
        {
            this.filenameCode = filenameCode;
        }

        @Override
        public String getFilenameCode()
        {
            return filenameCode;
        }
    }

    public enum NozzleUseIndicator implements FilenameEncoder
    {

        // Nozzle 0 only
        NOZZLE_0("N0"),
        // Nozzle 1 only
        NOZZLE_1("N1"),
        //Both nozzles
        BOTH("NB"),
        DONT_CARE(null);

        private final String filenameCode;

        private NozzleUseIndicator(String filenameCode)
        {
            this.filenameCode = filenameCode;
        }

        @Override
        public String getFilenameCode()
        {
            return filenameCode;
        }
    }

    /**
     *
     * @param macroName - this can include the macro execution directive at the
     * start of the line
     * @param headTypeCode
     * @param useNozzle0
     * @param useNozzle1
     * @return
     * @throws java.io.IOException
     * @throws celtech.printerControl.comms.commands.MacroLoadException
     */
    public static ArrayList<String> getMacroContents(String macroName,
            String headTypeCode,
            boolean useNozzle0,
            boolean useNozzle1) throws IOException, MacroLoadException
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

        NozzleUseIndicator nozzleUse;

        if (headTypeCode == null
                || headTypeCode.equalsIgnoreCase(HeadContainer.defaultHeadID))
        {
            nozzleUse = NozzleUseIndicator.DONT_CARE;
        } else
        {
            if (!useNozzle0 && !useNozzle1)
            {
                nozzleUse = NozzleUseIndicator.DONT_CARE;
            } else if (useNozzle0 && !useNozzle1)
            {
                nozzleUse = NozzleUseIndicator.NOZZLE_0;
            } else if (!useNozzle0 && useNozzle1)
            {
                nozzleUse = NozzleUseIndicator.NOZZLE_1;
            } else
            {
                nozzleUse = NozzleUseIndicator.BOTH;
            }
        }

        appendMacroContents(contents, parentMacros, macroName,
                headTypeCode, nozzleUse,
                (Lookup.getUserPreferences().safetyFeaturesOnProperty().get() == false) ? GCodeMacros.SafetyIndicator.SAFETIES_OFF : GCodeMacros.SafetyIndicator.DONT_CARE);

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
            final String macroName,
            String headTypeCode,
            NozzleUseIndicator nozzleUse,
            SafetyIndicator safeties) throws IOException, MacroLoadException
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
                fileReader = new FileReader(GCodeMacros.getFilename(cleanedMacroName,
                        headTypeCode,
                        nozzleUse,
                        safeties
                ));
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

                            appendMacroContents(contents, parentMacros, subMacroName,
                                    headTypeCode,
                                    nozzleUse,
                                    safeties);
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
     * Macros are stored in a single directory They are named as follows:
     * <baseMacroName>_<[S|U]>_<headType>_<[nozzle0Used|nozzle1Used]>
     * e.g. macroA_S_RBX01-SM - is a macro that should be used for safe mode
     * when using head RBX01-SM
     *
     * @param macroName
     * @param headTypeCode
     * @param nozzleUse
     * @param safeties
     * @return
     */
    public static String getFilename(String macroName,
            String headTypeCode,
            NozzleUseIndicator nozzleUse,
            SafetyIndicator safeties) throws FileNotFoundException
    {

        //Try with all attributes first
        //
        FilenameFilter filterForMacrosWithCorrectBase = new FilenameStartsWithFilter(macroName);

        File macroDirectory = new File(ApplicationConfiguration.getCommonApplicationDirectory() + ApplicationConfiguration.macroFileSubpath);

        String[] matchingMacroFilenames = macroDirectory.list(filterForMacrosWithCorrectBase);

        int highestScore = -1;
        int indexOfHighestScoringFilename = -1;

        if (matchingMacroFilenames.length > 0)
        {
            for (int filenameCounter = 0; filenameCounter < matchingMacroFilenames.length; filenameCounter++)
            {
                int score = scoreMacroFilename(matchingMacroFilenames[filenameCounter], headTypeCode, nozzleUse, safeties);
                if (score > highestScore)
                {
                    indexOfHighestScoringFilename = filenameCounter;
                    highestScore = score;
                }
            }

            return ApplicationConfiguration.getCommonApplicationDirectory()
                    + ApplicationConfiguration.macroFileSubpath
                    + matchingMacroFilenames[indexOfHighestScoringFilename];
        } else
        {
            steno.error("Couldn't find macro " + macroName + " with head " + headTypeCode + " nozzle " + nozzleUse.name() + " safety " + safeties.name());
            throw new FileNotFoundException("Couldn't find macro " + macroName + " with head " + headTypeCode + " nozzle " + nozzleUse.name() + " safety " + safeties.name());
        }
//        
//        if (macroFiles.length == 0 && safeties == SafetyIndicator.SAFETIES_OFF)
//        {
//            //There may not be a safeties off version of the file - look for it with a don't care
//            filterForMacrosWithCorrectBase = new MacroFilenameFilter(macroName, headTypeCode, nozzleUse, SafetyIndicator.DONT_CARE);
//            macroFiles = macroDirectory.listFiles(filterForMacrosWithCorrectBase);
//        }
//
//        if (macroFiles.length > 0)
//        {
//            if (macroFiles.length > 1)
//            {
//                steno.info("Found " + macroFiles.length + " macro files:");
//                for (int counter = 0; counter < macroFiles.length; counter++)
//                {
//                    steno.info(macroFiles[counter].getName());
//                }
//            }
//            return macroFiles[0].getAbsolutePath();
//        } else
//        {
//            steno.error("Couldn't find macro " + macroName + " with head " + headTypeCode + " nozzle " + nozzleUse.name() + " safety " + safeties.name());
//            throw new FileNotFoundException("Couldn't find macro " + macroName + " with head " + headTypeCode + " nozzle " + nozzleUse.name() + " safety " + safeties.name());
//        }
    }

    private static int scoreMacroFilename(String filename,
            String headTypeCode,
            NozzleUseIndicator nozzleUse,
            SafetyIndicator safeties)
    {
        int score = 0;
        final String separator = "#";

        String[] filenameSplit = filename.split("\\.");

        if (filenameSplit.length == 2
                && ("." + filenameSplit[1]).equalsIgnoreCase(ApplicationConfiguration.macroFileExtension))
        {
            String[] nameParts = filenameSplit[0].split(separator);

            for (String namePart : nameParts)
            {
                if (namePart.equalsIgnoreCase(headTypeCode)
                        || namePart.equalsIgnoreCase(nozzleUse.getFilenameCode())
                        || namePart.equalsIgnoreCase(safeties.getFilenameCode()))
                {
                    score++;
                }
            }
        }

        return score;
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

    public static int getNumberOfOperativeLinesInMacro(String macroDirective,
            String headType,
            boolean useNozzle0,
            boolean useNozzle1)
    {
        int linesInMacro = 0;
        String macro = cleanMacroName(macroDirective);
        if (macro != null)
        {
            try
            {
                List<String> contents = getMacroContents(macro, headType, useNozzle0, useNozzle1);
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
