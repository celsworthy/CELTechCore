package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;

/**
 *
 * @author Ian
 */
public class InnerPerimeterSectionNode extends SectionNode implements Renderable
{

    public static final String designator = ";TYPE:WALL-INNER";

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
