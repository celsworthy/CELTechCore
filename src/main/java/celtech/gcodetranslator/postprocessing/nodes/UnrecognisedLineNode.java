package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class UnrecognisedLineNode extends GCodeEventNode
{
    private final String discardedLine;

    public UnrecognisedLineNode(String discardedLine)
    {
        this.discardedLine = discardedLine;
    }

    @Override
    public String renderForOutput()
    {
        return "; DISCARDED:: " + discardedLine;
    }

}
