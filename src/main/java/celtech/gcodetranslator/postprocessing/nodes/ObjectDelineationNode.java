package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;

/**
 *
 * @author Ian
 */
public class ObjectDelineationNode extends GCodeEventNode implements Renderable
{

    private int objectNumber = -1;

    public int getObjectNumber()
    {
        return objectNumber;
    }

    public void setObjectNumber(int objectNumber)
    {
        this.objectNumber = objectNumber;
    }

    @Override
    public String renderForOutput()
    {
        return ";Object " + objectNumber;
    }
}
