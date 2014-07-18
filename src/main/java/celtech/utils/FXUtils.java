/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils;

import javafx.util.StringConverter;

/**
 *
 * @author Ian
 */
public class FXUtils
{

    /**
     *
     * @return
     */
    public static StringConverter<Integer> getIntConverter()
    {
        return new StringConverter<Integer>()
        {
            @Override
            public String toString(Integer t)
            {
                return String.format("%d", t);
            }

            @Override
            public Integer fromString(String string)
            {
                Integer value = null;
                try
                {
                    value = Integer.valueOf(string);
                } catch (NumberFormatException ex)
                {
                    value = Integer.valueOf(0);
                }
                return value;
            }
        };
    }

    /**
     *
     * @param decimalPlaces
     * @return
     */
    public static StringConverter<Float> getFloatConverter(int decimalPlaces)
    {
        String formatString = "%." + decimalPlaces + "f";

        return new StringConverter<Float>()
        {

            @Override
            public String toString(Float t)
            {
                return String.format(formatString, t);
            }

            @Override
            public Float fromString(String string)
            {
                Float value = null;
                try
                {
                    value = Float.valueOf(string);
                } catch (NumberFormatException ex)
                {
                    value = Float.valueOf(0);
                }
                return value;
            }

        };
    }
}
