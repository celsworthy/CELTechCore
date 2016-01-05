package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Feedrate;
import celtech.gcodetranslator.postprocessing.nodes.providers.FeedrateProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Movement;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePosition;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePositionProvider;

/**
 *
 * @author Ian
 */
public class NozzleValvePositionDuringTravelNode extends GCodeEventNode implements NozzlePositionProvider, MovementProvider, FeedrateProvider, Renderable
{

    private boolean fastAsPossible = false;
    private final NozzlePosition nozzlePosition = new NozzlePosition();
    private final Movement movement = new Movement();
    private final Feedrate feedrate = new Feedrate();

    public void setMoveAsFastAsPossible(boolean fastAsPossible)
    {
        this.fastAsPossible = fastAsPossible;
    }

    @Override
    public String renderForOutput()
    {
        StringBuilder stringToOutput = new StringBuilder();
        stringToOutput.append('G');
        if (fastAsPossible)
        {
            stringToOutput.append('0');
        } else
        {
            stringToOutput.append('1');
        }
        stringToOutput.append(' ');

        String feedrateString = feedrate.renderForOutput();
        stringToOutput.append(feedrateString);
        if (feedrateString.length() > 0)
        {
            stringToOutput.append(' ');
        }

        String movementString = movement.renderForOutput();
        stringToOutput.append(movementString);
        if (movementString.length() > 0)
        {
            stringToOutput.append(' ');
        }

        String nozzlePositionString = nozzlePosition.renderForOutput();
        stringToOutput.append(nozzlePositionString);
        if (nozzlePositionString.length() > 0)
        {
            stringToOutput.append(' ');
        }

        stringToOutput.append(getCommentText());

        return stringToOutput.toString().trim();
    }

    @Override
    public NozzlePosition getNozzlePosition()
    {
        return nozzlePosition;
    }

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
}
