/*
 * Copyright 2014 CEL UK
 */

package celtech.printerControl.comms.commands;

import static celtech.printerControl.comms.commands.ColourStringConverter.colourToString;
import static celtech.printerControl.comms.commands.ColourStringConverter.stringToColor;
import javafx.scene.paint.Color;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tony
 */
public class ColourStringConverterTest
{
    

    @Test
    public void testColourToString()
    {
        Color colour =  Color.rgb(0x10, 0x20, 0x30);
        String strColour = colourToString(colour);
        assertEquals("102030", strColour);
    }

    @Test
    public void testStringToColor()
    {
       String colourStr = "102030";
       assertEquals(Color.rgb(0x10, 0x20, 0x30), stringToColor(colourStr));
    }
    
}
