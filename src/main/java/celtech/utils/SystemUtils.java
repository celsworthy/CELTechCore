/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;
import java.util.UUID;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class SystemUtils
{

    private static Stenographer steno = StenographerFactory.getStenographer(SystemUtils.class.getName());

    public static String generate16DigitID()
    {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
    }

    public static boolean isDoubleSame(double doubleA, double doubleB, double tolerance)
    {
        return Math.abs(doubleA - doubleB) < tolerance;
    }

    public static char generateModulo10Checksum(String inputString) throws InvalidChecksumException
    {
        boolean weighter = false;
// allowable characters within identifier
        String validChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVYWXZ";

// remove leading or trailing whitespace, convert to uppercase
        inputString = inputString.trim().toUpperCase();

// this will be a running total
        int sum = 0;

// loop through digits from right to left
        for (int i = 0; i < inputString.length(); i++)
        {

//set ch to "current" character to be processed
            char ch = inputString
                    .charAt(i);

// throw exception for invalid characters
            if (validChars.indexOf(ch) == -1)
            {
                throw new InvalidChecksumException(
                        "\"" + ch + "\" is an invalid character");
            }

// our "digit" is calculated using ASCII value - 48
            int digit = (int) ch - 48;

            //Weight will alternate between 1 and 3
// weight will be the current digit's contribution to
// the running total
//            int weight;
//            if (i % 2 == 0)
//            {
//
//                // for alternating digits starting with the rightmost, we
//                // use our formula this is the same as multiplying x 2 and
//                // adding digits together for values 0 to 9.  Using the
//                // following formula allows us to gracefully calculate a
//                // weight for non-numeric "digits" as well (from their
//                // ASCII value - 48).
////                weight = (2 * digit) - (int) (digit / 5) * 9;
//                weight = (2 * digit) - (int) (digit / 5) * 9;
//
//            } else
//            {
//
//                // even-positioned digits just contribute their ascii
//                // value minus 48
//                weight = digit;
//
//            }
//            // keep a running total of weights
//            sum += weight;
////            sum += (weighter == false) ? 3 : 1 * digit;
////            weighter = !weighter;
            sum+=digit;
        }

// avoid sum less than 10 (if characters below "0" allowed,
// this could happen)
        sum = Math.abs(sum) + 10;

// check digit is amount needed to reach next number
// divisible by ten
        return Character.forDigit((10 - (sum % 10)) % 10, 10);

    }

    public static int countLinesInFile(File aFile, String commentCharacter)
    {
        LineNumberReader reader = null;
        int numberOfLines = 0;
        try
        {
            String lineRead;
            reader = new LineNumberReader(new FileReader(aFile));

            while ((lineRead = reader.readLine()) != null)
            {
                if (lineRead.startsWith(commentCharacter) == false && lineRead.equals("") == false)
                {
                    numberOfLines++;
                }
            };
            return numberOfLines;
        } catch (Exception ex)
        {
            return -1;
        } finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                } catch (IOException ex)
                {
                    steno.error("Failed to close file during line number read: " + ex);
                }
            }
        }
    }

    public static int countLinesInFile(File aFile)
    {
        LineNumberReader reader = null;
        try
        {
            reader = new LineNumberReader(new FileReader(aFile));
            while ((reader.readLine()) != null);
            return reader.getLineNumber();
        } catch (Exception ex)
        {
            return -1;
        } finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                } catch (IOException ex)
                {
                    steno.error("Failed to close file during line number read: " + ex);
                }
            }
        }
    }

    public static String getIncrementalFilenameOnly(String directory, String filename, String fileextension)
    {
        String chosenFilename = null;

        boolean notFound = true;
        int suffix = 1;

//        File testFile = new File(directory + File.separator + filename + "_" + suffix + fileextension);
//
//        if (testFile.exists() == true)
//        {
        while (notFound)
        {
            File outfile = new File(directory + File.separator + filename + "_" + suffix + fileextension);
            if (!outfile.exists())
            {
                chosenFilename = outfile.getName().replaceFirst("\\..*$", "");
                break;
            }

            suffix++;
        }
//        }
//        else
//        {
//            chosenFilename = filename;
//        }

        return chosenFilename;
    }

}
