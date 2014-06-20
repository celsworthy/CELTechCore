/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.ParseException;

/**
 * This class provides a formatter suitable for used in Robox comms.
 *
 * The output string is 8 characters maximum, hence the largest possible number
 * is 99999999 The minimum possible value is 0.000001
 *
 * Leading spaces are used - e.g. ' 123.3' Negative numbers are also represented
 * using leading spaces - e.g. ' -1232'
 *
 * NB - DecimalFormat is NOT threadsafe, so instantiate one of these per thread at a minimum
 * 
 * @author Ian
 */
public class FixedDecimalFloatFormat extends DecimalFormat
{

    private final int fieldLength = 8;

    public FixedDecimalFloatFormat()
    {
        this.setGroupingUsed(false);
    }

    private StringBuffer padResult(StringBuffer output)
    {
        int charactersToPad = fieldLength - output.length();

        if (charactersToPad < 0)
        {
            throw new NumberFormatException("Number length exceeds maximum (" + fieldLength + ") : " + output);
        }

        while (charactersToPad > 0)
        {
            output.insert(0, " ");
            charactersToPad--;
        }
        
        return output;
    }

    @Override
    public StringBuffer format(double d, StringBuffer sb, FieldPosition fp)
    {
        StringBuffer formattedString = super.format(d, sb, fp);

        return padResult(formattedString);
    }

    @Override
    public StringBuffer format(long l, StringBuffer sb, FieldPosition fp)
    {
        StringBuffer formattedString = super.format(l, sb, fp);

        return padResult(formattedString);
    }

    @Override
    public Number parse(String string) throws ParseException
    {
        String stringToParse = string.trim();

        if (stringToParse.length() > fieldLength)
        {
            throw new NumberFormatException("Number length exceeds maximum (" + fieldLength + ") : " + stringToParse);
        }

        return super.parse(stringToParse);
    }
}
