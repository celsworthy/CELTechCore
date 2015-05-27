package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.CommentNode;
import celtech.gcodetranslator.postprocessing.nodes.CommentableNode;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeDirectiveNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerChangeDirectiveNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.MCodeNode;
import celtech.gcodetranslator.postprocessing.nodes.MovementNode;
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
                OneOrMore(FirstOf(
                                CuraFillSection(),
                                CuraInnerPerimeterSection(),
                                CuraOuterPerimeterSection(),
                                ChildDirective()),
                        (Action) (Context context1) ->
                        {
                            if (!context1.getValueStack().isEmpty())
                            {
                                GCodeEventNode node = (GCodeEventNode) context1.getValueStack().pop();
                                steno.info("Adding child " + node);
                                TreeUtils.addChild(thisLayer, node);
                            }
                            return true;
                        }
                )
        );
    }

    // ;Blah blah blah\n
    Rule CommentDirective()
    {
        return Sequence(';', ZeroOrMore(NotNewline()),
                push(new CommentNode(match())),
                Newline());
    }

    // M14 or M104
    Rule MCode()
    {
        MCodeNode node = new MCodeNode();

        return Sequence(
                Sequence('M', OneToThreeDigits(),
                        (Action) (Context context1) ->
                        {
                            node.setMNumber(Integer.valueOf(context1.getMatch()));
                            return true;
                        },
                        Optional(
                                Sequence(
                                        " S", ZeroOrMore(Digit()),
                                        (Action) (Context context1) ->
                                        {
                                            node.setSNumber(Integer.valueOf(context1.getMatch()));
                                            return true;
                                        }
                                )
                        )
                ),
                (Action) (Context context1) ->
                {
                    context1.getValueStack().push(node);
                    return true;
                },
                Newline()
        );
    }

    // T1 or T12 or T123...
    Rule ToolSelect()
    {
        return Sequence('T', OneOrMore(Digit()),
                push(new ToolSelectNode()),
                Newline());
    }

    // G3 or G12
    Rule GCodeDirective()
    {
        return Sequence('G', OneOrTwoDigits(),
                push(new GCodeDirectiveNode()),
                Newline());
    }

    //Retract
    // G1 F1800 E-0.50000
    Rule RetractDirective()
    {
        RetractNode node = new RetractNode();

        return Sequence("G1 ",
                Sequence(
                        (Action) (Context context1) ->
                        {
                            context1.getValueStack().push(node);
                            return true;
                        },
                        Feedrate()),
                FirstOf(
                        Sequence("D", NegativeFloatingPointNumber(),
                                (Action) (Context context1) ->
                                {
                                    steno.info("Retract - adding D to node : " + node.toString());
                                    node.setD(Float.valueOf(context1.getMatch()));
                                    return true;
                                }),
                        Sequence("E", NegativeFloatingPointNumber(),
                                (Action) (Context context1) ->
                                {
                                    steno.info("Retract - adding E to node : " + node.toString());
                                    node.setE(Float.valueOf(context1.getMatch()));
                                    return true;
                                })
                ),
                (Action) (Context context1) ->
                {
                    steno.info("Retract - adding node to stack: " + node.toString());
                    context1.getValueStack().push(node);
                    return true;
                },
                Newline()
        );
    }

    //Unetract
    // G1 F1800 E0.50000
    Rule UnretractDirective()
    {
        UnretractNode node = new UnretractNode();

        return Sequence("G1 ",
                Sequence(
                        (Action) (Context context1) ->
                        {
                            context1.getValueStack().push(node);
                            return true;
                        },
                        Feedrate()
                ),
                OneOrMore(
                        FirstOf(
                                Sequence("D", PositiveFloatingPointNumber(),
                                        (Action) (Context context1) ->
                                        {
                                            steno.info("Unretract - adding D to node : " + node.toString());
                                            node.setD(Float.valueOf(context1.getMatch()));
                                            return true;
                                        }),
                                Sequence("E", PositiveFloatingPointNumber(),
                                        (Action) (Context context1) ->
                                        {
                                            steno.info("Unretract - adding E to node : " + node.toString());
                                            node.setE(Float.valueOf(context1.getMatch()));
                                            return true;
                                        })
                        )
                ),
                (Action) (Context context1) ->
                {
                    steno.info("Unretract - adding node to stack: " + node.toString());
                    context1.getValueStack().push(node);
                    return true;
                },
                Newline());
    }

    //Travel
    // G0 F12000 X88.302 Y42.421 Z1.020
    Rule TravelDirective()
    {
        TravelNode node = new TravelNode();

        return Sequence("G0 ",
                Optional(
                        Sequence(
                                (Action) (Context context1) ->
                                {
                                    context1.getValueStack().push(node);
                                    return true;
                                },
                                Feedrate())
                ),
                Optional(
                        Sequence("X", FloatingPointNumber(),
                                (Action) (Context context1) ->
                                {
                                    node.setX(Float.valueOf(context1.getMatch()));
                                    return true;
                                },
                                Optional(' '))
                ),
                Optional(
                        Sequence("Y", FloatingPointNumber(),
                                (Action) (Context context1) ->
                                {
                                    node.setY(Float.valueOf(context1.getMatch()));
                                    return true;
                                },
                                Optional(' '))
                ),
                Optional(
                        Sequence("Z", FloatingPointNumber(),
                                (Action) (Context context1) ->
                                {
                                    node.setZ(Float.valueOf(context1.getMatch()));
                                    return true;
                                },
                                Optional(' '))
                ),
                (Action) (Context context1) ->
                {
                    context1.getValueStack().push(node);
                    return true;
                },
                Newline());
    }

    //Extrusion
    // G1 F840 X88.700 Y44.153 E5.93294
    Rule ExtrusionDirective()
    {
        ExtrusionNode node = new ExtrusionNode();

        return Sequence("G1 ",
                Optional(
                        Sequence(
                                (Action) (Context context1) ->
                                {
                                    context1.getValueStack().push(node);
                                    return true;
                                },
                                Feedrate())
                ),
                Sequence("X", FloatingPointNumber(),
                        (Action) (Context context1) ->
                        {
                            node.setX(Float.valueOf(context1.getMatch()));
                            return true;
                        },
                        ' '),
                Sequence("Y", FloatingPointNumber(),
                        (Action) (Context context1) ->
                        {
                            node.setY(Float.valueOf(context1.getMatch()));
                            return true;
                        },
                        ' '),
                OneOrMore(
                        FirstOf(
                                Sequence("D", FloatingPointNumber(),
                                        (Action) (Context context1) ->
                                        {
                                            node.setD(Float.valueOf(context1.getMatch()));
                                            return true;
                                        },
                                        Optional(' ')),
                                Sequence("E", FloatingPointNumber(),
                                        (Action) (Context context1) ->
                                        {
                                            node.setE(Float.valueOf(context1.getMatch()));
                                            return true;
                                        },
                                        Optional(' '))
                        )
                ),
                (Action) (Context context1) ->
                {
                    context1.getValueStack().push(node);
                    return true;
                },
                Newline());
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
                ";TYPE:FILL",
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
                (Action) (Context context1) ->
                {
                    context1.getValueStack().push(createSectionAction.getNode());
                    return true;
                }
        );
    }

    //Cura outer perimeter section
    //;TYPE:WALL-OUTER
    Rule CuraOuterPerimeterSection()
    {
        OuterPerimeterSectionActionClass createSectionAction = new OuterPerimeterSectionActionClass();

        return Sequence(
                ";TYPE:WALL-OUTER",
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
                (Action) (Context context1) ->
                {
                    context1.getValueStack().push(createSectionAction.getNode());
                    return true;
                }
        );
    }

    //Cura inner perimeter section
    //;TYPE:WALL-INNER
    Rule CuraInnerPerimeterSection()
    {
        InnerPerimeterSectionActionClass createSectionAction = new InnerPerimeterSectionActionClass();

        return Sequence(
                ";TYPE:WALL-INNER",
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
                (Action) (Context context1) ->
                {
                    context1.getValueStack().push(createSectionAction.getNode());
                    return true;
                }
        );
    }

    @SuppressSubnodes
    Rule ChildDirective()
    {
        return FirstOf(CommentDirective(),
                MCode(),
                LayerChangeDirective(),
                GCodeDirective(),
                ToolSelect(),
                RetractDirective(),
                UnretractDirective(),
                TravelDirective(),
                ExtrusionDirective(),
                UnrecognisedLine());
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
    Rule Feedrate()
    {
        return FirstOf(
                Sequence('F', OneOrMore(Digit()),
                        (Action) (Context context1) ->
                        {
                            ((MovementNode) context1.getValueStack().pop()).setFeedRate(Integer.valueOf(context1.getMatch()));
                            return true;
                        },
                        Optional(' ')),
                (Action) (Context context1) ->
                {
                    context1.getValueStack().pop();
                    return true;
                });
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
