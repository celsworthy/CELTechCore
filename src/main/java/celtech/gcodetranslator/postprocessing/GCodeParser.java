package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.CommentNode;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeDirectiveNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerChangeDirectiveNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.MCodeNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import celtech.gcodetranslator.postprocessing.nodes.UnrecognisedLineNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.support.StringVar;
import org.parboiled.support.Var;
import org.parboiled.trees.TreeUtils;

/**
 *
 * @author Ian
 */
//@BuildParseTree
public class GCodeParser extends BaseParser<GCodeEventNode>
{

    private final Stenographer steno = StenographerFactory.getStenographer(GCodeParser.class.getName());
    private LayerNode thisLayer = new LayerNode();

    public LayerNode getLayerNode()
    {
        return thisLayer;
    }

    public void resetLayer()
    {
        thisLayer = new LayerNode();
    }

    public Rule Layer()
    {
        return Sequence(
                Sequence(";LAYER:", OneOrMore(Digit()),
                        (Action) (Context context1) ->
                        {
                            thisLayer.setLayerNumber(Integer.valueOf(context1.getMatch()));
                            return true;
                        },
                        Newline()
                ),
                OneOrMore(
                        FirstOf(
                                CuraFillSection(),
                                CuraInnerPerimeterSection(),
                                CuraOuterPerimeterSection(),
                                ChildDirective()
                        ),
                        (Action) (Context context1) ->
                        {
                            if (!context1.getValueStack().isEmpty())
                            {
                                steno.info("Adding child to layer");
                                GCodeEventNode node = (GCodeEventNode) context1.getValueStack().pop();
                                TreeUtils.addChild(thisLayer, node);
                            }
                            return true;
                        }
                ),
                EOI
        );
    }

    // ;Blah blah blah\n
    Rule CommentDirective()
    {
        StringVar comment = new StringVar();

        return Sequence(
                TestNot(FillSectionNode.designator),
                TestNot(InnerPerimeterSectionNode.designator),
                TestNot(OuterPerimeterSectionNode.designator),
                ';', ZeroOrMore(NotNewline()),
                comment.set(match()),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        CommentNode node = new CommentNode(comment.get());
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    // M14 or M104
    Rule MCode()
    {
        Var<Integer> mValue = new Var<>();
        Var<Integer> sValue = new Var<>();

        return Sequence(
                Sequence('M', OneToThreeDigits(),
                        mValue.set(Integer.valueOf(match()))
                ),
                Optional(
                        Sequence(
                                " S", ZeroOrMore(Digit()),
                                sValue.set(Integer.valueOf(match()))
                        )
                ),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        MCodeNode node = new MCodeNode();
                        node.setMNumber(mValue.get());
                        if (sValue.isSet())
                        {
                            node.setSNumber(sValue.get());
                        }
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    // T1 or T12 or T123...
    Rule ToolSelect()
    {
        Var<Integer> toolNumber = new Var<>();

        return Sequence('T', OneOrMore(Digit()),
                toolNumber.set(Integer.valueOf(match())),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        ToolSelectNode node = new ToolSelectNode();
                        node.setToolNumber(toolNumber.get());
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    // G3 or G12
    Rule GCodeDirective()
    {
        Var<Integer> gcodeValue = new Var<>();

        return Sequence('G', OneOrTwoDigits(),
                gcodeValue.set(Integer.valueOf(match())),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        GCodeDirectiveNode node = new GCodeDirectiveNode();
                        node.setGValue(gcodeValue.get());
                        context.getValueStack().push(node);
                        return true;
                    }
                });
    }

    //Retract
    // G1 F1800 E-0.50000
    Rule RetractDirective()
    {
        Var<Double> dValue = new Var<>();
        Var<Double> eValue = new Var<>();
        Var<Integer> fValue = new Var<>();

        return Sequence("G1 ",
                Optional(
                        Feedrate(fValue)
                ),
                OneOrMore(
                        FirstOf(
                                Sequence("D", NegativeFloatingPointNumber(),
                                        dValue.set(Double.valueOf(match())),
                                        Optional(' ')
                                ),
                                Sequence("E", NegativeFloatingPointNumber(),
                                        eValue.set(Double.valueOf(match())),
                                        Optional(' '))
                        )
                ),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        RetractNode node = new RetractNode();
                        if (dValue.isSet())
                        {
                            node.setD(dValue.get());
                        }
                        if (eValue.isSet())
                        {
                            node.setE(eValue.get());
                        }
                        if (fValue.isSet())
                        {
                            node.setFeedRate(fValue.get());
                        }
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    //Unetract
    // G1 F1800 E0.50000
    Rule UnretractDirective()
    {
        Var<Double> dValue = new Var<>();
        Var<Double> eValue = new Var<>();
        Var<Integer> fValue = new Var<>();

        return Sequence("G1 ",
                Optional(
                        Feedrate(fValue)
                ),
                OneOrMore(
                        FirstOf(
                                Sequence("D", PositiveFloatingPointNumber(),
                                        dValue.set(Double.valueOf(match())),
                                        Optional(' ')
                                ),
                                Sequence("E", PositiveFloatingPointNumber(),
                                        eValue.set(Double.valueOf(match())),
                                        Optional(' '))
                        )
                ),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context
                    )
                    {
                        UnretractNode node = new UnretractNode();
                        if (dValue.isSet())
                        {
                            node.setD(dValue.get());
                        }
                        if (eValue.isSet())
                        {
                            node.setE(eValue.get());
                        }
                        if (fValue.isSet())
                        {
                            node.setFeedRate(fValue.get());
                        }
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    //Travel
    // G0 F12000 X88.302 Y42.421 Z1.020
    Rule TravelDirective()
    {
        Var<Integer> fValue = new Var<>();
        Var<Double> xValue = new Var<>();
        Var<Double> yValue = new Var<>();
        Var<Double> zValue = new Var<>();

        return Sequence("G0 ",
                Optional(
                        Feedrate(fValue)
                ),
                OneOrMore(
                        FirstOf(
                                Sequence("X", FloatingPointNumber(),
                                        xValue.set(Double.valueOf(match())),
                                        Optional(' ')
                                ),
                                Sequence("Y", FloatingPointNumber(),
                                        yValue.set(Double.valueOf(match())),
                                        Optional(' ')
                                ),
                                Sequence("Z", FloatingPointNumber(),
                                        zValue.set(Double.valueOf(match())),
                                        Optional(' ')
                                )
                        )
                ),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        TravelNode node = new TravelNode();

                        if (fValue.isSet())
                        {
                            node.setFeedRate(fValue.get());
                        }

                        if (xValue.isSet())
                        {
                            node.setX(xValue.get());
                        }

                        if (yValue.isSet())
                        {
                            node.setY(yValue.get());
                        }

                        if (zValue.isSet())
                        {
                            node.setZ(zValue.get());
                        }

                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    //Extrusion
    // G1 F840 X88.700 Y44.153 E5.93294
    Rule ExtrusionDirective()
    {
        Var<Integer> fValue = new Var<>();
        Var<Double> xValue = new Var<>();
        Var<Double> yValue = new Var<>();
        Var<Double> zValue = new Var<>();
        Var<Double> eValue = new Var<>();
        Var<Double> dValue = new Var<>();

        return Sequence("G1 ",
                Optional(
                        Feedrate(fValue)
                ),
                Optional(
                        Sequence("X", FloatingPointNumber(),
                                xValue.set(Double.valueOf(match())),
                                ' ',
                                "Y", FloatingPointNumber(),
                                yValue.set(Double.valueOf(match())),
                                Optional(' ')
                        )
                ),
                OneOrMore(
                        FirstOf(
                                Sequence("D", PositiveFloatingPointNumber(),
                                        dValue.set(Double.valueOf(match())),
                                        Optional(' ')
                                ),
                                Sequence("E", PositiveFloatingPointNumber(),
                                        eValue.set(Double.valueOf(match())),
                                        Optional(' ')
                                )
                        )
                ),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        ExtrusionNode node = new ExtrusionNode();

                        if (fValue.isSet())
                        {
                            node.setFeedRate(fValue.get());
                        }

                        if (xValue.isSet())
                        {
                            node.setX(xValue.get());
                        }

                        if (yValue.isSet())
                        {
                            node.setY(yValue.get());
                        }

                        if (zValue.isSet())
                        {
                            node.setZ(zValue.get());
                        }

                        if (dValue.isSet())
                        {
                            node.setD(dValue.get());
                        }

                        if (eValue.isSet())
                        {
                            node.setE(eValue.get());
                        }

                        context.getValueStack()
                        .push(node);

                        return true;
                    }
                }
        );
    }

    //Layer change
    // G[01] Z1.020
    Rule LayerChangeDirective()
    {
        return Sequence('G', FirstOf('0', '1'), ' ',
                Sequence("Z", FloatingPointNumber()),
                push(new LayerChangeDirectiveNode()),
                Newline());
    }

    /*
     * Cura specific
     */
    //Cura fill Section
    //;TYPE:FILL
    Rule CuraFillSection()
    {
        FillSectionActionClass createSectionAction = new FillSectionActionClass();

        return Sequence(
                FillSectionNode.designator,
                createSectionAction,
                Newline(),
                OneOrMore(ChildDirective(),
                        (Action) (Context context1) ->
                        {
                            if (!context1.getValueStack().isEmpty())
                            {
                                TreeUtils.addChild(createSectionAction.getNode(), (GCodeEventNode) context1.getValueStack().pop());
                            }
                            return true;
                        }
                ),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        context.getValueStack().push(createSectionAction.getNode());
                        return true;
                    }
                }
        );
    }

    //Cura outer perimeter section
    //;TYPE:WALL-OUTER
    Rule CuraOuterPerimeterSection()
    {
        OuterPerimeterSectionActionClass createSectionAction = new OuterPerimeterSectionActionClass();

        return Sequence(
                OuterPerimeterSectionNode.designator,
                createSectionAction,
                Newline(),
                OneOrMore(ChildDirective(),
                        (Action) (Context context1) ->
                        {
                            if (!context1.getValueStack().isEmpty())
                            {
                                TreeUtils.addChild(createSectionAction.getNode(), (GCodeEventNode) context1.getValueStack().pop());
                            }
                            return true;
                        }
                ),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        context.getValueStack().push(createSectionAction.getNode());
                        return true;
                    }
                }
        );
    }

    //Cura inner perimeter section
    //;TYPE:WALL-INNER
    Rule CuraInnerPerimeterSection()
    {
        InnerPerimeterSectionActionClass createSectionAction = new InnerPerimeterSectionActionClass();

        return Sequence(
                InnerPerimeterSectionNode.designator,
                createSectionAction,
                Newline(),
                OneOrMore(ChildDirective(),
                        (Action) (Context context1) ->
                        {
                            if (!context1.getValueStack().isEmpty())
                            {
                                TreeUtils.addChild(createSectionAction.getNode(), (GCodeEventNode) context1.getValueStack().pop());
                            }
                            return true;
                        }
                ),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        context.getValueStack().push(createSectionAction.getNode());
                        return true;
                    }
                }
        );
    }

    @SuppressSubnodes
    Rule ChildDirective()
    {
        return FirstOf(
                CommentDirective(),
                MCode(),
                LayerChangeDirective(),
                GCodeDirective(),
                ToolSelect(),
                RetractDirective(),
                UnretractDirective(),
                TravelDirective(),
                ExtrusionDirective(),
                UnrecognisedLine()
        );
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
    Rule OneToThreeDigits()
    {
        return FirstOf(ThreeDigits(), TwoDigits(), Digit());
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
        return FirstOf(
                NegativeFloatingPointNumber(),
                PositiveFloatingPointNumber()
        );
    }

    @SuppressSubnodes
    Rule PositiveFloatingPointNumber()
    {
        //Positive float e.g. 1.23
        return Sequence(
                OneOrMore(Digit()),
                Ch('.'),
                OneOrMore(Digit()));
    }

    @SuppressSubnodes
    Rule NegativeFloatingPointNumber()
    {
        //Negative float e.g. -1.23
        return Sequence(
                Ch('-'),
                OneOrMore(Digit()),
                Ch('.'),
                OneOrMore(Digit()));
    }

    @SuppressSubnodes
    Rule Feedrate(Var<Integer> feedrate)
    {
        return Sequence('F', OneOrMore(Digit()),
                feedrate.set(Integer.valueOf(match())),
                Optional(' '));
    }

    //Anything else we didn't parse... must always be the last thing we look for
    // blah blah \n
    @SuppressSubnodes
    Rule UnrecognisedLine()
    {
        return Sequence(ZeroOrMore(ANY),
                push(new UnrecognisedLineNode()),
                Newline());
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
