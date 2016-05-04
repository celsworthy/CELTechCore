package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.DurationCalculationException;
import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.SupportsPrintTimeCalculation;
import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;
import celtech.gcodetranslator.postprocessing.nodes.providers.Extrusion;
import celtech.gcodetranslator.postprocessing.nodes.providers.ExtrusionProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Feedrate;
import celtech.gcodetranslator.postprocessing.nodes.providers.FeedrateProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Movement;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePosition;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePositionProvider;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Ian
 */
public class ExtrusionNode extends GCodeEventNode implements ExtrusionProvider, MovementProvider, FeedrateProvider, NozzlePositionProvider, SupportsPrintTimeCalculation, Renderable
{

    private Extrusion extrusion = new Extrusion();
    private Movement movement = new Movement();
    private Feedrate feedrate = new Feedrate();
    private NozzlePosition nozzlePosition = new NozzlePosition();
    //Elided extrusion is populated when a close is inserted just before this node
    private double elidedExtrusion = 0;

    @Override
    public Extrusion getExtrusion()
    {
        return extrusion;
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
    public NozzlePosition getNozzlePosition()
    {
        return nozzlePosition;
    }

    public double getElidedExtrusion()
    {
        return elidedExtrusion;
    }

    public void setElidedExtrusion(double elidedExtrusion)
    {
        this.elidedExtrusion = elidedExtrusion;
    }

    @Override
    public String renderForOutput()
    {
        StringBuilder stringToOutput = new StringBuilder();

        stringToOutput.append("G1 ");

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
        
        String extrusionString = extrusion.renderForOutput();
        stringToOutput.append(extrusionString);
        if (extrusionString.length() > 0)
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
        
        stringToOutput.append(":");
        stringToOutput.append(elidedExtrusion);
        return stringToOutput.toString().trim();
    }

    @Override
    public double timeToReach(MovementProvider destinationNode) throws DurationCalculationException
    {
        Vector2D source = movement.toVector2D();
        Vector2D destination = new Vector2D(destinationNode.getMovement().getX(), destinationNode.getMovement().getY());

        double distance = source.distance(destination);

        double time = distance / feedrate.getFeedRate_mmPerSec();

        if (time < 0)
        {
            throw new DurationCalculationException(this, destinationNode);
        }

        return time;
    }

    @Override
    public ExtrusionNode clone()
    {
        ExtrusionNode returnedNode = new ExtrusionNode();
        returnedNode.extrusion = extrusion.clone();
        returnedNode.movement = movement.clone();
        returnedNode.feedrate = feedrate.clone();
        returnedNode.nozzlePosition = nozzlePosition.clone();
        returnedNode.setCommentText(getCommentText());

        return returnedNode;
    }
}
