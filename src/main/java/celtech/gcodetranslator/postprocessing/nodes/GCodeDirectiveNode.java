package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class GCodeDirectiveNode extends GCodeEventNode
{
    private Integer gValue = -1;

    public Integer getGValue()
    {
        return gValue;
    }

    public void setGValue(Integer gValue)
    {
        this.gValue = gValue;
    }
}
