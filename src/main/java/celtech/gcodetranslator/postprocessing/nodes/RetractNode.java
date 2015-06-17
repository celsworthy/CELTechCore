package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Extrusion;
import celtech.gcodetranslator.postprocessing.nodes.providers.ExtrusionProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Feedrate;
import celtech.gcodetranslator.postprocessing.nodes.providers.FeedrateProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;

/**
 *
 * @author Ian
 */
public class RetractNode extends GCodeEventNode implements ExtrusionProvider, FeedrateProvider, Renderable
{
    private final Feedrate feedrate = new Feedrate();
    private final Extrusion extrusion = new Extrusion();
    private double extrusionSinceLastRetract = 0;
    private ExtrusionNode priorExtrusion = null;

    @Override
    public Extrusion getExtrusion()
    {
        return extrusion;
    }

    @Override
    public Feedrate getFeedrate()
    {
        return feedrate;
    }
    
    public void setExtrusionSinceLastRetract(double value)
    {
        extrusionSinceLastRetract = value;
    }

    public double getExtrusionSinceLastRetract()
    {
        return extrusionSinceLastRetract;
    }
    
    public void setPriorExtrusionNode(ExtrusionNode node)
    {
        priorExtrusion = node;
    }
    
    public ExtrusionNode getPriorExtrusionNode()
    {
        return priorExtrusion;
    }

    @Override
    public String toString()
    {
        return "Retract " + feedrate.renderForOutput() + extrusion.renderForOutput();
    }

    @Override
    public String renderForOutput()
    {
        StringBuilder stringToOutput = new StringBuilder();

        stringToOutput.append("G1 ");
        stringToOutput.append(feedrate.renderForOutput());
        stringToOutput.append(' ');
        stringToOutput.append(extrusion.renderForOutput());
        stringToOutput.append(' ');
        stringToOutput.append(getCommentText());
        return stringToOutput.toString().trim();
    }
}
