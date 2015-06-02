package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class LayerNode extends GCodeEventNode
{
    private int layerNumber = -1;

    public LayerNode()
    {
    }

    public LayerNode(int layerNumber)
    {
        this.layerNumber = layerNumber;
    }
    
    public void setLayerNumber(int layerNumber)
    {
        this.layerNumber = layerNumber;
    }
    
    public int getLayerNumber()
    {
        return layerNumber;
    }

    @Override
    public String toString()
    {
        return ";Layer "  + layerNumber;
    }
        @Override
    public String renderForOutput()
    {
        return toString();
    }
}
