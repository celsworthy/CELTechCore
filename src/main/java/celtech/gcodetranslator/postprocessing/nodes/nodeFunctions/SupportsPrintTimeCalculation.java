package celtech.gcodetranslator.postprocessing.nodes.nodeFunctions;

import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;

/**
 *
 * @author Ian
 */
public interface SupportsPrintTimeCalculation
{
    public double timeToReach(MovementProvider destinationNode) throws DurationCalculationException;
}
