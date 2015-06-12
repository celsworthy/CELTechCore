package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.SupportsPrintTimeCalculation;
import celtech.gcodetranslator.postprocessing.nodes.providers.Feedrate;
import celtech.gcodetranslator.postprocessing.nodes.providers.FeedrateProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Movement;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Ian
 */
public class TravelNode extends GCodeEventNode implements MovementProvider, FeedrateProvider, SupportsPrintTimeCalculation, Renderable
{
    private final Movement movement = new Movement();
    private final Feedrate feedrate = new Feedrate();

    //Travel events should always use G1
    @Override
    public String renderForOutput()
    {
        StringBuilder stringToReturn = new StringBuilder();

        stringToReturn.append("G1 ");

        stringToReturn.append(feedrate.renderForOutput());
        stringToReturn.append(' ');
        stringToReturn.append(movement.renderForOutput());
        stringToReturn.append(' ');
        stringToReturn.append(getCommentText());

        return stringToReturn.toString();
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

    @Override
    public double timeToReach(SupportsPrintTimeCalculation destinationNode)
    {
        Vector2D source = movement.toVector2D();
        Vector2D destination = new Vector2D(((MovementProvider)destinationNode).getMovement().getX(), ((MovementProvider)destinationNode).getMovement().getY());

        double distance = source.distance(destination);

        double time = distance / feedrate.getFeedRate_mmPerSec();

        return time;
    }
}
