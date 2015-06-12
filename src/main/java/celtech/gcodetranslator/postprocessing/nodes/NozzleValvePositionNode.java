package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePosition;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePositionProvider;

/**
 *
 * @author Ian
 */
public class NozzleValvePositionNode extends GCodeEventNode implements NozzlePositionProvider, Renderable
{
    private final NozzlePosition nozzlePosition = new NozzlePosition();

    @Override
    public String renderForOutput()
    {
        StringBuilder stringToOutput = new StringBuilder();
        stringToOutput.append("G0 ");
        stringToOutput.append(nozzlePosition.renderForOutput());
        stringToOutput.append(' ');
        stringToOutput.append(getCommentText());
        
        return stringToOutput.toString().trim();
    }

    @Override
    public NozzlePosition getNozzlePosition()
    {
        return nozzlePosition;
    }
}
