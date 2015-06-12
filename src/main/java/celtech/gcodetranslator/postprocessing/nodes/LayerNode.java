package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;

/**
 *
 * @author Ian
 */
public class LayerNode extends GCodeEventNode implements Renderable
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
    public String renderForOutput()
    {
        return ";Layer " + layerNumber;
    }
}
