package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.MCodeNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.ObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParsingResult;

/**
 *
 * @author Ian
 */
public class GCodeParserTest
{

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
    }

    @After
    public void tearDown()
    {
    }

    @Test
    public void floatingPointNumberTest()
    {
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.FloatingPointNumber());

        String positiveNumber = "1.20\n";
        ParsingResult positiveNumberResult = runner.run(positiveNumber);
        assertFalse(positiveNumberResult.hasErrors());
        assertTrue(positiveNumberResult.matched);

        String negativeNumber = "-1.20\n";
        gcodeParser = Parboiled.createParser(GCodeParser.class);
        runner = new BasicParseRunner<>(gcodeParser.FloatingPointNumber());
        ParsingResult negativeNumberResult = runner.run(negativeNumber);
        assertFalse(negativeNumberResult.hasErrors());
        assertTrue(negativeNumberResult.matched);
    }

    @Test
    public void negativeFloatingPointNumberTest()
    {
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.NegativeFloatingPointNumber());

        String positiveNumber = "1.20\n";
        ParsingResult positiveNumberResult = runner.run(positiveNumber);
        assertFalse(positiveNumberResult.hasErrors());
        assertFalse(positiveNumberResult.matched);

        String negativeNumber = "-1.20\n";
        ParsingResult negativeNumberResult = runner.run(negativeNumber);
        assertFalse(negativeNumberResult.hasErrors());
        assertTrue(negativeNumberResult.matched);
    }

    @Test
    public void positiveFloatingPointNumberTest()
    {
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.PositiveFloatingPointNumber());

        String positiveNumber = "1.20\n";
        ParsingResult positiveNumberResult = runner.run(positiveNumber);
        assertFalse(positiveNumberResult.hasErrors());
        assertTrue(positiveNumberResult.matched);

        String negativeNumber = "-1.20\n";
        ParsingResult negativeNumberResult = runner.run(negativeNumber);
        assertFalse(negativeNumberResult.hasErrors());
        assertFalse(negativeNumberResult.matched);
    }

    @Test
    public void travelDirective()
    {
        String inputData = "G0 F12000 X88.302 Y42.421 Z1.020\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.TravelDirective());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
    }

    @Test
    public void retractDirective()
    {
        String inputData = "G1 F1800 E-0.50000\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.RetractDirective());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
    }

    @Test
    public void unretractDirective()
    {
        String inputData = "G1 F1800 E0.00000\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.UnretractDirective());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
    }

    @Test
    public void gcodeDirective()
    {
        String inputData = "G92\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
        );
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.GCodeDirective());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
    }

    @Test
    public void mcodeDirective()
    {
        String inputData = "M107\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.MCode());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
    }

    @Test
    public void extrusionDirective()
    {
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.ExtrusionDirective());

        String eOnlyExtrude = "G1 X1.4 Y12.3 E1.3\n";
        ParsingResult inputData1Result = runner.run(eOnlyExtrude);

        assertFalse(inputData1Result.hasErrors());
        assertTrue(inputData1Result.matched);

        gcodeParser.resetLayer();

        String dOnlyExtrude = "G1 F100 X1.4 Y12.3 D1.3\n";
        ParsingResult dOnlyExtrudeResult = runner.run(dOnlyExtrude);
        assertFalse(dOnlyExtrudeResult.hasErrors());
        assertTrue(dOnlyExtrudeResult.matched);
    }

    @Test
    public void commentDirective()
    {
        String inputData = ";Hello this is a comment \n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
        );
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.CommentDirective());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
    }

    @Test
    public void commentDirectiveNoMatch()
    {
        String inputData = InnerPerimeterSectionNode.designator + "\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
        );
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.CommentDirective());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertFalse(result.matched);
    }

    @Test
    public void layerChangeDirective()
    {
        String inputData = "G0 Z1.3\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
        );
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.LayerChangeDirective());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
    }

    @Test
    public void compoundTest()
    {
        String alternateInput = ";LAYER:0\n"
                + "M107\n"
                + "G1 F1800 E-0.50000\n"
                + "G0 F12000 X63.440 Y93.714 Z0.480\n"
                + ";TYPE:WALL-INNER\n"
                + "G1 F1800 E0.00000\n"
                + "G1 F1200 X63.569 Y94.143 E0.05379\n"
                + "G1 X63.398 Y94.154 E0.02058\n"
                + "G1 X63.353 Y93.880 E0.03334\n"
                + "G1 X63.309 Y93.825 E0.00846\n"
                + "G1 X63.440 Y93.714 E0.02062\n"
                + "G0 F12000 X63.120 Y93.197\n"
                + ";TYPE:WALL-OUTER\n"
                + "G1 F1200 X62.564 Y91.751 E0.18603\n"
                + "G1 X61.057 Y88.531 E0.42691\n"
                + "G1 X60.380 Y86.590 E0.24685\n"
                + "G1 X60.346 Y86.419 E0.02094\n"
                + "G1 X61.247 Y86.240 E0.11031\n";

        String inputData = ";LAYER:0\n"
                + "M107\n"
                + "G1 F1800 E-0.50000\n"
                + "G0 F12000 X88.302 Y42.421 Z1.020\n"
                + ";TYPE:WALL-INNER\n"
                + "G1 F1800 E0.00000\n"
                + "G1 F840 X115.304 Y42.421 E5.40403\n"
                + "G1 X115.304 Y114.420 E14.40948\n"
                + "G1 X88.302 Y114.420 E5.40403\n"
                + "G1 X88.302 Y42.421 E14.40948\n"
                + "G0 F12000 X87.302 Y41.421\n"
                + ";TYPE:WALL-OUTER\n"
                + "G1 F840 X116.304 Y41.421 E5.80430\n"
                + "G1 X116.304 Y115.420 E14.80975\n"
                + "G1 X87.302 Y115.420 E5.80430\n"
                + "G1 X87.302 Y41.421 E14.80975\n"
                + "G0 F12000 X87.902 Y41.931\n"
                + "G0 X88.782 Y42.820\n"
                + ";TYPE:FILL\n"
                + "G1 F840 X114.903 Y68.941 E5.91448\n"
                + "G0 F12000 X114.903 Y70.355\n"
                + "G1 F840 X88.700 Y44.153 E5.93294\n"
                + "G0 F12000 X88.700 Y45.567\n"
                + "G1 F840 X114.903 Y71.769 E5.93294\n"
                + "G0 F12000 X114.903 Y73.184\n";

        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.Layer());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
        LayerNode layerNode = gcodeParser.getLayerNode();

        assertNotNull(layerNode);

        assertEquals(
                4, layerNode.getChildren().size());

        assertEquals(MCodeNode.class, layerNode.getChildren().get(0).getClass());
        assertEquals(RetractNode.class, layerNode.getChildren().get(1).getClass());
        assertEquals(TravelNode.class, layerNode.getChildren().get(2).getClass());
        assertEquals(ObjectDelineationNode.class, layerNode.getChildren().get(3).getClass());

        ObjectDelineationNode objectNode = (ObjectDelineationNode) layerNode.getChildren().get(3);

        assertEquals(
                3, objectNode.getChildren().size());
        assertEquals(InnerPerimeterSectionNode.class, objectNode.getChildren().get(0).getClass());
        assertEquals(OuterPerimeterSectionNode.class, objectNode.getChildren().get(1).getClass());
        assertEquals(FillSectionNode.class, objectNode.getChildren().get(2).getClass());
    }

    @Test
    public void objectSectionTest()
    {
        String inputData = "T1\n"
                + ";TYPE:FILL\n"
                + "G1 F1800 E-0.50000\n"
                + "G0 F12000 X88.302 Y42.421 Z1.020\n"
                + "G1 F1800 E0.00000\n"
                + "G1 X12.3 Y14.5 E1.00000\n"
                + ";TYPE:WALL-OUTER\n"
                + "G1 X125.3 Y314.5 E1.00000\n";

        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
        );
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.ObjectSection());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);

        assertFalse(result.valueStack.isEmpty());
        assertTrue(result.valueStack.peek() instanceof ObjectDelineationNode);

        ObjectDelineationNode node = (ObjectDelineationNode) result.valueStack.pop();
        assertEquals(2, node.getChildren().size());
        assertEquals(FillSectionNode.class, node.getChildren().get(0).getClass());
        assertEquals(OuterPerimeterSectionNode.class, node.getChildren().get(1).getClass());

        FillSectionNode fillNode = (FillSectionNode) node.getChildren().get(0);
        assertEquals(4, fillNode.getChildren().size());
        assertEquals(RetractNode.class, fillNode.getChildren().get(0).getClass());
        assertEquals(TravelNode.class, fillNode.getChildren().get(1).getClass());
        assertEquals(UnretractNode.class, fillNode.getChildren().get(2).getClass());
        assertEquals(ExtrusionNode.class, fillNode.getChildren().get(3).getClass());

        OuterPerimeterSectionNode outerNode = (OuterPerimeterSectionNode) node.getChildren().get(1);
        assertEquals(1, outerNode.getChildren().size());
        assertEquals(ExtrusionNode.class, outerNode.getChildren().get(0).getClass());
    }

    @Test
    public void objectSectionNoTriggerTest()
    {
        String inputData = ";TYPE:FILL\n"
                + "G1 F1800 E-0.50000\n"
                + "G0 F12000 X88.302 Y42.421 Z1.020\n"
                + "G1 F1800 E0.00000\n"
                + "G1 X12.3 Y14.5 E1.00000\n"
                + ";TYPE:WALL-OUTER\n"
                + "G1 X125.3 Y314.5 E1.00000\n";

        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
        );
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.ObjectSection());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);

        assertFalse(result.valueStack.isEmpty());
        assertTrue(result.valueStack.peek() instanceof ObjectDelineationNode);

        ObjectDelineationNode node = (ObjectDelineationNode) result.valueStack.pop();
        assertEquals(2, node.getChildren().size());
        assertEquals(FillSectionNode.class, node.getChildren().get(0).getClass());
        assertEquals(OuterPerimeterSectionNode.class, node.getChildren().get(1).getClass());

        FillSectionNode fillNode = (FillSectionNode) node.getChildren().get(0);
        assertEquals(4, fillNode.getChildren().size());
        assertEquals(RetractNode.class, fillNode.getChildren().get(0).getClass());
        assertEquals(TravelNode.class, fillNode.getChildren().get(1).getClass());
        assertEquals(UnretractNode.class, fillNode.getChildren().get(2).getClass());
        assertEquals(ExtrusionNode.class, fillNode.getChildren().get(3).getClass());

        OuterPerimeterSectionNode outerNode = (OuterPerimeterSectionNode) node.getChildren().get(1);
        assertEquals(1, outerNode.getChildren().size());
        assertEquals(ExtrusionNode.class, outerNode.getChildren().get(0).getClass());
    }

    @Test
    public void fillSectionTest()
    {
        String inputData = ";TYPE:FILL\n"
                + "G1 F1800 E-0.50000\n"
                + "G0 F12000 X88.302 Y42.421 Z1.020\n"
                + "G1 F1800 E0.00000\n"
                + "G1 X12.3 Y14.5 E1.00000\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
        );
        RecoveringParseRunner runner = new RecoveringParseRunner<>(gcodeParser.FillSection());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);

        assertFalse(result.valueStack.isEmpty());
        assertTrue(result.valueStack.peek() instanceof FillSectionNode);

        FillSectionNode node = (FillSectionNode) result.valueStack.pop();
        assertEquals(4, node.getChildren().size());
        assertEquals(RetractNode.class, node.getChildren().get(0).getClass());
        assertEquals(TravelNode.class, node.getChildren().get(1).getClass());
        assertEquals(UnretractNode.class, node.getChildren().get(2).getClass());
        assertEquals(ExtrusionNode.class, node.getChildren().get(3).getClass());
    }

    @Test
    public void innerPerimeterTest()
    {
        String inputData = ";TYPE:WALL-INNER\n"
                + "G1 F1800 E-0.50000\n"
                + "G0 F12000 X88.302 Y42.421 Z1.020\n"
                + "G1 F1800 E0.00000\n"
                + "G1 X12.3 Y14.5 E1.00000\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        RecoveringParseRunner runner = new RecoveringParseRunner<>(gcodeParser.InnerPerimeterSection());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);

        assertFalse(result.valueStack.isEmpty());
        assertTrue(result.valueStack.peek() instanceof InnerPerimeterSectionNode);

        InnerPerimeterSectionNode sectionNode = (InnerPerimeterSectionNode) result.valueStack.pop();
        assertEquals(4, sectionNode.getChildren().size());
        assertEquals(RetractNode.class, sectionNode.getChildren().get(0).getClass());
        assertEquals(TravelNode.class, sectionNode.getChildren().get(1).getClass());
        assertEquals(UnretractNode.class, sectionNode.getChildren().get(2).getClass());
        assertEquals(ExtrusionNode.class, sectionNode.getChildren().get(3).getClass());
    }

    @Test
    public void outerPerimeterTest()
    {
        String inputData = ";TYPE:WALL-OUTER\n"
                + "G1 F1800 E-0.50000\n"
                + "G0 F12000 X88.302 Y42.421 Z1.020\n"
                + "G1 F1800 E0.00000\n"
                + "G1 X12.3 Y14.5 E1.00000\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
        );
        RecoveringParseRunner runner = new RecoveringParseRunner<>(gcodeParser.OuterPerimeterSection());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);

        assertFalse(result.valueStack.isEmpty());
        assertTrue(result.valueStack.peek() instanceof OuterPerimeterSectionNode);

        OuterPerimeterSectionNode sectionNode = (OuterPerimeterSectionNode) result.valueStack.pop();
        assertEquals(4, sectionNode.getChildren().size());
        assertEquals(RetractNode.class, sectionNode.getChildren().get(0).getClass());
        assertEquals(TravelNode.class, sectionNode.getChildren().get(1).getClass());
        assertEquals(UnretractNode.class, sectionNode.getChildren().get(2).getClass());
        assertEquals(ExtrusionNode.class, sectionNode.getChildren().get(3).getClass());
    }
}
