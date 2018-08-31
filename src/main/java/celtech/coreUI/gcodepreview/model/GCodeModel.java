package celtech.coreUI.gcodepreview.model;

import celtech.coreUI.gcodepreview.gcode.GCodeLoader;
import celtech.coreUI.visualisation.Xform;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author George Salter
 */
public class GCodeModel {
    
    private final static Stenographer STENO = StenographerFactory.getStenographer(GCodeModel.class.getName());
    
    private Xform world = null;
        
    private List<Layer> layers = null;

    public GCodeModel() {
    }
    
    public int layerCount() {
        if (layers != null)
            return layers.size();
        else
            return 0;
    }
    
    public List<Layer> getLayers() {
        return layers;
    }

    public void loadFile(String filename) {
        layers = new GCodeLoader().loadFile(filename);
    }
}
