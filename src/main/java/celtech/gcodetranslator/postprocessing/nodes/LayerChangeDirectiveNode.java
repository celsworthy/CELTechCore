package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Feedrate;
import celtech.gcodetranslator.postprocessing.nodes.providers.FeedrateProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Movement;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;

/**
 *
 * @author Ian
 */
public class LayerChangeDirectiveNode extends GCodeEventNode implements MovementProvider, FeedrateProvider, Renderable, MergeableWithToolchange
{

    private int layerNumber = -1;
    private final Movement movement = new Movement();
    private final Feedrate feedrate = new Feedrate();
    private boolean isToolChangeRequired = false;
    private int toolNumber;

    @Override
    public Movement getMovement()
    {
        return movement;
    }

    @Override
    public Feedrate getFeedrate()
    {
        return feedrate;
    }

    public int getLayerNumber()
    {
        return layerNumber;
    }

    public void setLayerNumber(int layerNumber)
    {
        this.layerNumber = layerNumber;
    }

    @Override
    public void changeToolDuringMovement(int toolNumber)
    {
        isToolChangeRequired = true;
        this.toolNumber = toolNumber;
    }

    @Override
    public String renderForOutput()
    {
        StringBuilder stringToReturn = new StringBuilder();

        if (isToolChangeRequired)
        {
            stringToReturn.append('T');
            stringToReturn.append(toolNumber);
            stringToReturn.append(' ');
        } else
        {
            stringToReturn.append("G1 ");
        }

        stringToReturn.append(feedrate.renderForOutput());
        stringToReturn.append(' ');
        stringToReturn.append(movement.renderForOutput());
        stringToReturn.append(' ');
        stringToReturn.append(getCommentText());

        return stringToReturn.toString();
    }
}
