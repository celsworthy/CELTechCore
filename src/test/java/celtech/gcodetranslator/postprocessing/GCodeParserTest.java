package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.CommentNode;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeDirectiveNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerChangeDirectiveNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.MCodeNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
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
        String inputData = ";LAYER:0\nG0 F12000 X88.302 Y42.421 Z1.020\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.Layer());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
        LayerNode layerNode = gcodeParser.getLayerNode();
        assertNotNull(layerNode);
        assertEquals(1, layerNode.getChildren().size());
        assertEquals(TravelNode.class, layerNode.getChildren().get(0).getClass());
    }

    @Test
    public void retractDirective()
    {
        String inputData = ";LAYER:0\nG1 F1800 E-0.50000\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.Layer());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
        LayerNode layerNode = gcodeParser.getLayerNode();
        assertNotNull(layerNode);
        assertEquals(1, layerNode.getChildren().size());
        assertEquals(RetractNode.class, layerNode.getChildren().get(0).getClass());
    }

    @Test
    public void unretractDirective()
    {
        String inputData = ";LAYER:0\nG1 F1800 E0.00000\nG1 F840 E1.2\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.Layer());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
        LayerNode layerNode = gcodeParser.getLayerNode();
        assertNotNull(layerNode);
        assertEquals(2, layerNode.getChildren().size());

        assertEquals(UnretractNode.class, layerNode.getChildren().get(0).getClass());
        UnretractNode node1 = (UnretractNode) layerNode.getChildren().get(0);
        assertEquals(1800, node1.getFeedRate(), 0.00001);
        assertEquals(0, node1.getE(), 0.00001);
        assertEquals(0, node1.getD(), 0.00001);

        assertEquals(UnretractNode.class, layerNode.getChildren().get(1).getClass());
        UnretractNode node2 = (UnretractNode) layerNode.getChildren().get(1);
        assertEquals(840, node2.getFeedRate(), 0.00001);
        assertEquals(1.2, node2.getE(), 0.00001);
        assertEquals(0, node2.getD(), 0.00001);
    }

    @Test
    public void gcodeDirective()
    {
        String inputData = ";LAYER:0\nG92\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
        );
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.Layer());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
        LayerNode layerNode = gcodeParser.getLayerNode();
        assertNotNull(layerNode);
        assertEquals(1, layerNode.getChildren().size());
        assertEquals(GCodeDirectiveNode.class, layerNode.getChildren().get(0).getClass());
    }

    @Test
    public void mcodeDirective()
    {
        String inputData = ";LAYER:0\nM107\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.Layer());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
        LayerNode layerNode = gcodeParser.getLayerNode();
        assertNotNull(layerNode);
        assertEquals(1, layerNode.getChildren().size());
        assertEquals(MCodeNode.class, layerNode.getChildren().get(0).getClass());
    }

    @Test
    public void extrusionDirective()
    {
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.Layer());

        String eOnlyExtrude = ";LAYER:0\nG1 X1.4 Y12.3 E1.3\n";
        ParsingResult inputData1Result = runner.run(eOnlyExtrude);
        assertFalse(inputData1Result.hasErrors());
        assertTrue(inputData1Result.matched);
        LayerNode layerNode = gcodeParser.getLayerNode();
        assertNotNull(layerNode);
        assertEquals(1, layerNode.getChildren().size());
        assertEquals(ExtrusionNode.class, layerNode.getChildren().get(0).getClass());

        gcodeParser.resetLayer();

        String dOnlyExtrude = ";LAYER:0\nG1 F100 X1.4 Y12.3 D1.3\n";
        ParsingResult dOnlyExtrudeResult = runner.run(dOnlyExtrude);
        assertFalse(dOnlyExtrudeResult.hasErrors());
        assertTrue(dOnlyExtrudeResult.matched);
        layerNode = gcodeParser.getLayerNode();
        assertNotNull(layerNode);
        assertEquals(1, layerNode.getChildren().size());
        assertEquals(ExtrusionNode.class, layerNode.getChildren().get(0).getClass());
    }

    @Test
    public void toolSelect()
    {
        String inputData = ";LAYER:0\nT3\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
        );
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.Layer());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
        LayerNode layerNode = gcodeParser.getLayerNode();
        assertNotNull(layerNode);
        assertEquals(1, layerNode.getChildren().size());
        assertEquals(ToolSelectNode.class, layerNode.getChildren().get(0).getClass());
    }

    @Test
    public void commentDirective()
    {
        String inputData = ";LAYER:0\n;Hello this is a comment \n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
        );
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.Layer());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
        LayerNode layerNode = gcodeParser.getLayerNode();
        assertNotNull(layerNode);
        assertEquals(1, layerNode.getChildren().size());
        assertEquals(CommentNode.class, layerNode.getChildren().get(0).getClass());
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
        String inputData = ";LAYER:0\nG0 Z1.3\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
        );
        BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.Layer());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
        LayerNode layerNode = gcodeParser.getLayerNode();
        assertNotNull(layerNode);
        assertEquals(1, layerNode.getChildren().size());
        assertEquals(LayerChangeDirectiveNode.class, layerNode.getChildren().get(0).getClass());
    }

    @Test
    public void compoundTest()
    {
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
        assertEquals(6, layerNode.getChildren().size());
        
        assertEquals(MCodeNode.class, layerNode.getChildren().get(0).getClass());
        assertEquals(RetractNode.class, layerNode.getChildren().get(1).getClass());
        assertEquals(TravelNode.class, layerNode.getChildren().get(2).getClass());
        assertEquals(InnerPerimeterSectionNode.class, layerNode.getChildren().get(3).getClass());
        assertEquals(OuterPerimeterSectionNode.class, layerNode.getChildren().get(4).getClass());
        assertEquals(FillSectionNode.class, layerNode.getChildren().get(5).getClass());
    }

    @Test
    public void fillSectionTest()
    {
        String inputData = ";LAYER:0\n"
                + ";TYPE:FILL\n"
                + "G1 F1800 E-0.50000\n"
                + "G0 F12000 X88.302 Y42.421 Z1.020\n"
                + "G1 F1800 E0.00000\n"
                + "G1 X12.3 Y14.5 E1.00000\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
        );
        RecoveringParseRunner runner = new RecoveringParseRunner<>(gcodeParser.Layer());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
        LayerNode layerNode = gcodeParser.getLayerNode();
        assertNotNull(layerNode);
        //One child at the first level - this should be the fill section
        assertEquals(1, layerNode.getChildren().size());
        assertEquals(FillSectionNode.class, layerNode.getChildren().get(0).getClass());

        FillSectionNode fillSection = (FillSectionNode) layerNode.getChildren().get(0);
        assertEquals(4, fillSection.getChildren().size());
        assertEquals(RetractNode.class, fillSection.getChildren().get(0).getClass());
        assertEquals(TravelNode.class, fillSection.getChildren().get(1).getClass());
        assertEquals(UnretractNode.class, fillSection.getChildren().get(2).getClass());
        assertEquals(ExtrusionNode.class, fillSection.getChildren().get(3).getClass());
    }

    @Test
    public void innerPerimeterTest()
    {
        String inputData = ";LAYER:0\n"
                + ";TYPE:WALL-INNER\n"
                + "G1 F1800 E-0.50000\n"
                + "G0 F12000 X88.302 Y42.421 Z1.020\n"
                + "G1 F1800 E0.00000\n"
                + "G1 X12.3 Y14.5 E1.00000\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class);
        RecoveringParseRunner runner = new RecoveringParseRunner<>(gcodeParser.Layer());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
        LayerNode layerNode = gcodeParser.getLayerNode();
        assertNotNull(layerNode);
        //One child at the first level - this should be the fill section
        assertEquals(1, layerNode.getChildren().size());
        assertEquals(InnerPerimeterSectionNode.class, layerNode.getChildren().get(0).getClass());

        InnerPerimeterSectionNode sectionNode = (InnerPerimeterSectionNode) layerNode.getChildren().get(0);
        assertEquals(4, sectionNode.getChildren().size());
        assertEquals(RetractNode.class, sectionNode.getChildren().get(0).getClass());
        assertEquals(TravelNode.class, sectionNode.getChildren().get(1).getClass());
        assertEquals(UnretractNode.class, sectionNode.getChildren().get(2).getClass());
        assertEquals(ExtrusionNode.class, sectionNode.getChildren().get(3).getClass());
    }

    @Test
    public void outerPerimeterTest()
    {
        String inputData = ";LAYER:0\n"
                + ";TYPE:WALL-OUTER\n"
                + "G1 F1800 E-0.50000\n"
                + "G0 F12000 X88.302 Y42.421 Z1.020\n"
                + "G1 F1800 E0.00000\n"
                + "G1 X12.3 Y14.5 E1.00000\n";
        GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
        );
        RecoveringParseRunner runner = new RecoveringParseRunner<>(gcodeParser.Layer());
        ParsingResult result = runner.run(inputData);

        assertFalse(result.hasErrors());
        assertTrue(result.matched);
        LayerNode layerNode = gcodeParser.getLayerNode();
        assertNotNull(layerNode);
        //One child at the first level - this should be the fill section
        assertEquals(1, layerNode.getChildren().size());
        assertEquals(OuterPerimeterSectionNode.class, layerNode.getChildren().get(0).getClass());

        OuterPerimeterSectionNode sectionNode = (OuterPerimeterSectionNode) layerNode.getChildren().get(0);
        assertEquals(4, sectionNode.getChildren().size());
        assertEquals(RetractNode.class, sectionNode.getChildren().get(0).getClass());
        assertEquals(TravelNode.class, sectionNode.getChildren().get(1).getClass());
        assertEquals(UnretractNode.class, sectionNode.getChildren().get(2).getClass());
        assertEquals(ExtrusionNode.class, sectionNode.getChildren().get(3).getClass());
    }
}
