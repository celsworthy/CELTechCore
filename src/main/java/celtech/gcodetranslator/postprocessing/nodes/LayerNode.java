package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;

/**
 *
 * @author Ian
 */
public class LayerNode extends GCodeEventNode implements Renderable
{

    private int layerNumber = -1;
    private int numberOfUnrecognisedElements = 0;

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

    public int getNumberOfUnrecognisedElements()
    {
        return numberOfUnrecognisedElements;
    }

    public void setNumberOfUnrecognisedElements(int numberOfUnrecognisedElements)
    {
        this.numberOfUnrecognisedElements = numberOfUnrecognisedElements;
    }

    @Override
    public String renderForOutput()
    {
        return ";LAYER:" + layerNumber;
    }
}
