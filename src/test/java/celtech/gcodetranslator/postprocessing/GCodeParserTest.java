package celtech.gcodetranslator.postprocessing;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.support.ParsingResult;

/**
 *
 * @author Ian
 */
public class GCodeParserTest
{

    private GCodeParser gcodeParser;

    public GCodeParserTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
    {
        gcodeParser = new GCodeParser();
    }

    @After
    public void tearDown()
    {
    }

//    @Test
//    public void travelDirective()
//    {
//        String inputData = "G0 F12000 X88.302 Y42.421 Z1.020\n";
//        ParsingResult<?> result = new RecoveringParseRunner(gcodeParser.Layer()).run(inputData);
////        System.out.println(printNodeTree(result));
//        assertTrue(result.matched);
//    }
//
//    @Test
//    public void retractDirective()
//    {
//        String inputData = "G1 F1800 E-0.50000\n";
//        ParsingResult<?> result = new RecoveringParseRunner(gcodeParser.Layer()).run(inputData);
////        System.out.println(printNodeTree(result));
//        assertTrue(result.matched);
//    }
//
//    @Test
//    public void gcodeDirective()
//    {
//        String inputData = "G92\n";
//        ParsingResult<?> result = new RecoveringParseRunner(gcodeParser.Layer()).run(inputData);
////        System.out.println(printNodeTree(result));
//        assertTrue(result.matched);
//    }
//
//    @Test
//    public void mcodeDirective()
//    {
//        String inputData = "M107\n";
//        ParsingResult<?> result = new RecoveringParseRunner(gcodeParser.Layer()).run(inputData);
////        System.out.println(printNodeTree(result));
//        assertTrue(result.matched);
//    }
//
//    @Test
//    public void toolSelect()
//    {
//        String inputData = "T3\n";
//        ParsingResult<?> result = new RecoveringParseRunner(gcodeParser.Layer()).run(inputData);
////        System.out.println(printNodeTree(result));
//        assertTrue(result.matched);
//    }
//
//    @Test
//    public void commentDirective()
//    {
//        String inputData = ";Hello this is a comment \n";
//        ParsingResult<?> result = new RecoveringParseRunner(gcodeParser.Layer()).run(inputData);
////        System.out.println(printNodeTree(result));
//        assertTrue(result.matched);
//    }
//
//    @Test
//    public void layerChangeDirective()
//    {
//        String inputData = "G0 Z1.3\n";
//        ParsingResult<?> result = new RecoveringParseRunner(gcodeParser.Layer()).run(inputData);
////        System.out.println(printNodeTree(result));
//        assertTrue(result.matched);
//    }
//
//    @Test
//    public void compoundTest()
//    {
//        String inputData = ";Generated with Cura_SteamEngine DEV\n"
//                + ";Layer count: 9\n"
//                + ";LAYER:0\n"
//                + "M107\n"
//                + "G1 F1800 E-0.50000\n"
//                + "G0 F12000 X88.302 Y42.421 Z1.020\n"
//                + ";TYPE:WALL-INNER\n"
//                + "G1 F1800 E0.00000\n";
//        ParsingResult<?> result = new RecoveringParseRunner(gcodeParser.Layer()).run(inputData);
////        System.out.println(printNodeTree(result));
//        assertTrue(result.matched);
//    }
}
