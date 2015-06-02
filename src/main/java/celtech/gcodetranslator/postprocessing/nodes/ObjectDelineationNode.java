package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class ObjectDelineationNode extends GCodeEventNode
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
