package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;

/**
 *
 * @author Ian
 */
public class RetractHolder
{

    private final RetractNode node;
    private final NozzleProxy nozzle;

    public RetractHolder(RetractNode node, NozzleProxy nozzle)
    {
        this.node = node;
        this.nozzle = nozzle;
    }

    public RetractNode getNode()
    {
        return node;
    }

    public NozzleProxy getNozzle()
    {
        return nozzle;
    }

}
