package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class UnrecognisedLineNode extends GCodeEventNode
{

    @Override
    public String renderForOutput()
    {
        return toString();
    }

}
