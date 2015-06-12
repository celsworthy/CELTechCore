package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;

/**
 *
 * @author Ian
 */
public class SkinSectionNode extends SectionNode implements Renderable
{

    public static final String designator = ";TYPE:SKIN";

    @Override
    public String renderForOutput()
    {
        StringBuilder stringToOutput = new StringBuilder();
        stringToOutput.append(designator);
        stringToOutput.append(' ');
        stringToOutput.append(getCommentText());
        return designator;
    }
}
