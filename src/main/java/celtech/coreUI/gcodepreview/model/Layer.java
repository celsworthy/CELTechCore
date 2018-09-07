package celtech.coreUI.gcodepreview.model;

import celtech.coreUI.gcodepreview.entities.Entity;
import celtech.coreUI.gcodepreview.entities.LineEntity;
import celtech.coreUI.gcodepreview.entities.SequenceEntity;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a layer as described by G-Code.
 * Holds the {@link Entity} objects that will allow the layer to be displayed.
 *
 * @author George Salter
 */
public class Layer {
    
    private final List<Entity> entities = new ArrayList<>();
    private final List<LineEntity> lineEntities = new ArrayList<>();
    private final List<SequenceEntity> sequenceEntities = new ArrayList<>();
   
    private final int layerNo;
    
    private boolean rendered = true;
    
    public Layer(int layerNo) {
        this.layerNo = layerNo;
    }
    
    public void addEntity(Entity entity) {
        entities.add(entity);
    }
    
    public List<Entity> getEntities() {
        return entities;
    }
    
    public void addLineEntity(LineEntity lineEntity) {
        lineEntities.add(lineEntity);
    }
    
    public List<LineEntity> getLineEntities() {
        return lineEntities;
    }
    
    public void addSequenceEntity(SequenceEntity entity) {
        sequenceEntities.add(entity);
    }
    
    public List<SequenceEntity> getSequenceEntities() {
        return sequenceEntities;
    }
    
    public int getLayerNo() {
        return this.layerNo;
    }
}
