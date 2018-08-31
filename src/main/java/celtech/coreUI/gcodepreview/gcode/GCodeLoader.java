/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.gcodepreview.gcode;

import celtech.coreUI.gcodepreview.model.Layer;
import celtech.roboxbase.postprocessor.nouveau.nodes.LayerNode;
import java.util.List;

/**
 *
 * @author Tony
 */
public class GCodeLoader {

    private List<LayerNode> layerNodes = null;
    private List<Layer> layers = null;
    private NodeHandler nodeHandler = new NodeHandler();
    
    public List<Layer> loadFile(String gCodeFile)
    {
        try {
            GCodeConvertor gCodeConvertor = new GCodeConvertor();
            layerNodes = gCodeConvertor.convertGCode(gCodeFile);
            layers = nodeHandler.processLayerNodes(layerNodes);
            System.out.println("layerCount " + layers.size());
        }
        catch (RuntimeException ex)
        {
            layerNodes = null;
            layers = null;
            System.out.println("Parsing error");
        }
        
        return layers;
    }
}
