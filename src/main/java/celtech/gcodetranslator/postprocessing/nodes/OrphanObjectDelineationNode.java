package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class OrphanObjectDelineationNode extends GCodeEventNode
{
    private int potentialObjectNumber = -1;

    public int getPotentialObjectNumber()
    {
        return potentialObjectNumber;
    }

    public void setPotentialObjectNumber(int objectNumber)
    {
        this.potentialObjectNumber = objectNumber;
    }

    @Override
    public String renderForOutput()
    {
        return ";Orphan Object " + potentialObjectNumber;
    }
}
