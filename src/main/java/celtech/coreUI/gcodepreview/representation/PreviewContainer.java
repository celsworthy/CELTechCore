package celtech.coreUI.gcodepreview.representation;

import celtech.coreUI.gcodepreview.model.GCodeModel;
import celtech.coreUI.gcodepreview.model.Layer;
import celtech.coreUI.visualisation.Xform;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Tony Aldhous
 */
public class PreviewContainer extends Group
{

    private final Stenographer steno = StenographerFactory.getStenographer(PreviewContainer.class.getName());

    private final String gCodeFileName;
    private final GCodeModel gCodeModel;
    private final Xform world;
    private int topLayerIndex = 0;
    private boolean movesVisible = false;
    private List<LayerRepresentation> layerReps = null;
    private Group bed = null;
    
    public PreviewContainer(String fileName)
    {
        super();

        this.gCodeModel = new GCodeModel();
        this.world = new Xform();
        this.getChildren().add(world);
        this.gCodeFileName = fileName;
        initialise();
        initialiseTransforms();
    }

    private void initialise()
    {
    }
    
    protected final void initialiseTransforms()
    {
        Rotate correctOrientation = new Rotate(90, 0, 0, 0, Rotate.Z_AXIS);
        getTransforms().addAll(correctOrientation);
    }
    
    public void setBedReference(Group bed)
    {
        this.bed = bed;
        initialiseTransforms();
    }

    public void render(boolean movesVisible)
    {
        gCodeModel.loadFile(gCodeFileName);
        layerReps = new ArrayList<>();
        topLayerIndex = 0;
        this.movesVisible = movesVisible;
        gCodeModel.getLayers().forEach(layer -> renderLayer(layer));
        topLayerIndex = gCodeModel.getLayers().size();
    }
    
    public int getNumberOfLayers()
    {
        return gCodeModel.getLayers().size();
    }

    public void setTopVisibleLayerIndex(int index)
    {
        if (index < gCodeModel.getLayers().size())
            topLayerIndex = index;
        else
            topLayerIndex = gCodeModel.getLayers().size();
        layerReps.forEach(layerRep -> setLayerVisibility(layerRep));
    }

    public void setMovesVisibility(boolean movesVisible)
    {
        if (movesVisible != this.movesVisible)
        {
            this.movesVisible = movesVisible;
            layerReps.forEach(layerRep -> layerRep.setLineVisibility(movesVisible));
        }
    }

    private void renderLayer(Layer layer)
    {
        LayerRepresentation layerRep = new LayerRepresentation(layer, world);
        layerReps.add(layerRep);
        layerRep.render(movesVisible);
    }
    
    private void setLayerVisibility(LayerRepresentation layerRep)
    {
        layerRep.setVisibility(layerRep.getLayer().getLayerNo() < topLayerIndex);
    }
}
