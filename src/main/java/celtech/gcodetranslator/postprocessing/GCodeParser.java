package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.CommentNode;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeDirectiveNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.MCodeNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressSubnodes;

/**
 *
 * @author Ian
 */
@BuildParseTree
public class GCodeParser extends BaseParser<GCodeEventNode>
{
//    protected StackOperations stack = new StackOperations();

    public Rule Layer()
    {
        return Sequence(
                Sequence(";LAYER:", OneOrMore(Digit()), Newline()),
                OneOrMore(FirstOf(
                                CuraFillSection(),
                                CuraInnerPerimeterSection(),
                                CuraOuterPerimeterSection(),
                                CommentDirective(),
                                MCode(),
                                LayerChangeDirective(),
                                GCodeDirective(),
                                ToolSelect(),
                                RetractDirective(),
                                TravelDirective(),
                                ExtrusionDirective())),
                push(new LayerNode()));
    }

    // ;Blah blah blah\n
    Rule CommentDirective()
    {
        return Sequence(';', ZeroOrMore(NotNewline()), Newline(),
                push(new CommentNode()));
    }

    // M14 or M104
    Rule MCode()
    {
        return Sequence('M', TwoOrThreeDigits(), Newline(),
                push(new MCodeNode()));
    }

    // T1 or T12 or T123...
    Rule ToolSelect()
    {
        return Sequence('T', OneOrMore(Digit()), Newline(),
                push(new ToolSelectNode()));
    }

    // G3 or G12
    Rule GCodeDirective()
    {
        return Sequence('G', OneOrTwoDigits(), Newline(),
                push(new GCodeDirectiveNode()));
    }

    //Retract
    // G1 F1800 E-0.50000
    Rule RetractDirective()
    {
        return Sequence("G1 ",
                Optional('F', OneOrMore(Digit())),
                Optional(' '),
                "E-", FloatingPointNumber(),
                Newline(),
                push(new RetractNode()));
    }

    //Travel
    // G0 F12000 X88.302 Y42.421 Z1.020
    Rule TravelDirective()
    {
        return Sequence("G0 ",
                Optional(Sequence('F', OneOrMore(Digit()))),
                Optional(' '),
                Optional(Sequence("X", FloatingPointNumber())),
                Optional(' '),
                Optional(Sequence("Y", FloatingPointNumber())),
                Optional(' '),
                Optional(Sequence("Z", FloatingPointNumber())),
                Newline(),
                push(new TravelNode()));
    }

    //Extrusion
    // G1 F840 X88.700 Y44.153 E5.93294
    Rule ExtrusionDirective()
    {
        return Sequence("G1 ",
                Optional(Sequence('F', OneOrMore(Digit(), ' '))),
                Sequence("X", FloatingPointNumber(), ' '),
                Sequence("Y", FloatingPointNumber(), ' '),
                Sequence("E", FloatingPointNumber(), ' '),
                Newline(),
                push(new ExtrusionNode()));
    }

    //Layer change
    // G[01] Z1.020
    Rule LayerChangeDirective()
    {
        return Sequence('G', FirstOf('0', '1'), ' ',
                Sequence("Z", FloatingPointNumber()),
                Newline());
    }

    /*
     * Cura specific
     */
    //Cura fill Section
    //;TYPE:FILL
    Rule CuraFillSection()
    {
        return Sequence(";TYPE:FILL",
                Newline(),
                OneOrMore(FirstOf(CommentDirective(),
                                MCode(),
                                LayerChangeDirective(),
                                GCodeDirective(),
                                ToolSelect(),
                                RetractDirective(),
                                TravelDirective(),
                                ExtrusionDirective())),
                push(new FillSectionNode()));
    }

    //Cura outer perimeter section
    //;TYPE:WALL-OUTER
    Rule CuraOuterPerimeterSection()
    {
        return Sequence(";TYPE:WALL-OUTER",
                Newline());
    }

    //Cura inner perimeter section
    //;TYPE:WALL-INNER
    Rule CuraInnerPerimeterSection()
    {
        return Sequence(";TYPE:WALL-INNER",
                Newline());
    }

    @SuppressSubnodes
    Rule OneOrTwoDigits()
    {
        return FirstOf(TwoDigits(), Digit());
    }

    @SuppressSubnodes
    Rule TwoOrThreeDigits()
    {
        return FirstOf(ThreeDigits(), TwoDigits());
    }

    @SuppressSubnodes
    Rule TwoDigits()
    {
        return Sequence(Digit(), Digit());
    }

    @SuppressSubnodes
    Rule ThreeDigits()
    {
        return Sequence(Digit(), Digit(), Digit());
    }

    @SuppressSubnodes
    Rule Digit()
    {
        return CharRange('0', '9');
    }

    @SuppressSubnodes
    Rule FloatingPointNumber()
    {
        return OneOrMore(FirstOf(Digit(), Ch('.'), Ch('-')));
    }

    @SuppressSubnodes
    Rule Newline()
    {
        return Ch('\n');
    }

    @SuppressSubnodes
    Rule NotNewline()
    {
        return NoneOf("\n");
    }
}
