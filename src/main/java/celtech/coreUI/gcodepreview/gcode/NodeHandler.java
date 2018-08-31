package celtech.coreUI.gcodepreview.gcode;

import celtech.coreUI.gcodepreview.model.Layer;
import celtech.coreUI.gcodepreview.entities.Entity;
import celtech.coreUI.gcodepreview.entities.LineEntity;
import celtech.coreUI.gcodepreview.VectorUtils;
import celtech.roboxbase.postprocessor.nouveau.nodes.CommentNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.ExtrusionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.FillSectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.GCodeEventNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.InnerPerimeterSectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.LayerChangeDirectiveNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.LayerNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.MCodeNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.NozzleValvePositionDuringTravelNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.NozzleValvePositionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.OrphanObjectDelineationNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.OuterPerimeterSectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.SkinSectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.TravelNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.Movement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * Handles the nodes produced as a result of parsing G-Code
 * 
 * @author George Salter
 */
public class NodeHandler {
    
    private Vector3D previousPosition = new Vector3D(0, 0, 0);
    private double currentLayerHeight = 0;
    private double extrusionWidth = 0;
    
    public NodeHandler() {
    }
    
    public List<Layer> processLayerNodes(List<LayerNode> layerNodes) {
        List<Layer> layers = new ArrayList<>();
        
        layerNodes.forEach((layerNode) -> {
            Layer layer = new Layer(layerNode.getLayerNumber());
            Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);
            
            while (layerIterator.hasNext()) {
                GCodeEventNode node = layerIterator.next();
                
                if (node instanceof ExtrusionNode) {
                    Entity extrusionEntity = processExtrusionNode((ExtrusionNode) node);
                    layer.addEntity(extrusionEntity);
                } else if (node instanceof TravelNode) {
                    LineEntity travelEntity = processTravelNode((TravelNode) node);
                    layer.addLineEntity(travelEntity);
                } else if (node instanceof LayerChangeDirectiveNode) {
                    LineEntity layerChangeEntity = processLayerChangeDirectiveNode((LayerChangeDirectiveNode) node);
                    layer.addLineEntity(layerChangeEntity);
                } else if (node instanceof NozzleValvePositionDuringTravelNode) {
                    Entity nozzleValvePositionDuringTravelEntity = processNozzleValvePositionDuringTravelNode((NozzleValvePositionDuringTravelNode) node);
                    layer.addEntity(nozzleValvePositionDuringTravelEntity);
                } else if (node instanceof NozzleValvePositionNode) {
                    processNozzleValvePositionNode((NozzleValvePositionNode) node);
                } else if (node instanceof MCodeNode) {
                    processMCodeNode((MCodeNode) node);
                } else if (node instanceof CommentNode) {
                    processCommentNode((CommentNode) node);
                } else if (node instanceof OrphanObjectDelineationNode) {
                    processOrphanObjectDelineationNode((OrphanObjectDelineationNode) node);
                } else if (node instanceof InnerPerimeterSectionNode) {
                    processInnerPerimeterSectionNode((InnerPerimeterSectionNode) node);
                } else if (node instanceof OuterPerimeterSectionNode) {
                    processOuterPerimeterSectionNode((OuterPerimeterSectionNode) node);
                } else if (node instanceof SkinSectionNode) {
                    processSkinSectionNode((SkinSectionNode) node);
                } else if (node instanceof FillSectionNode) {
                    processFillSectionNode((FillSectionNode) node);
                } else {
                    System.out.println(node.getClass().getName());
                }
            }
            
            layers.add(layer);
        });
        
        return layers;
    }
        
    private Entity processExtrusionNode(ExtrusionNode extrusionNode) {
        
        // Switch Z and Y for the OpenGL coordinate system
        Movement toMove = extrusionNode.getMovement();
        double toX = (double) -toMove.getX();
        double toY;
        double toZ = (double) toMove.getY();
        if(toMove.isZSet()) {
            toY = (double) toMove.getZ();
            currentLayerHeight = (double) toMove.getZ();
        } else {
            toY = currentLayerHeight;
        }
        Vector3D toPosition = new Vector3D(toX, toY, toZ);
        Vector3D fromPosition = previousPosition;

        double length = VectorUtils.calculateLengthBetweenVectors(fromPosition, toPosition);
        double angleAroundY = VectorUtils.calculateRotationAroundYOfVectors(fromPosition, toPosition);
        double angleAroundZ = VectorUtils.calculateRotationAroundZOfVectors(fromPosition, toPosition);
        Vector3D entityPosition = VectorUtils.calculateCenterBetweenVectors(fromPosition, toPosition);
        
        Entity extrusionEntity = new Entity(entityPosition, 0, angleAroundY, angleAroundZ, length, extrusionWidth, extrusionWidth);
        extrusionEntity.setColour(new Vector3D(0.2f, 0.8f, 1.0f));
        
        previousPosition = new Vector3D(toX, toY, toZ);
        
        return extrusionEntity;
    }
    
    private LineEntity processTravelNode(TravelNode travelNode) {
        // Switch Z and Y for the OpenGL coordinate system
        Movement toMove = travelNode.getMovement();
        double toX = (double) -toMove.getX();
        double toY;
        double toZ = (double) toMove.getY();
        if(toMove.isZSet()) {
            toY = (double) toMove.getZ();
            currentLayerHeight = (double) toMove.getZ();
        } else {
            toY = currentLayerHeight;
        }
        Vector3D toPosition = new Vector3D(toX, toY, toZ);
        Vector3D fromPosition = previousPosition;
        
        LineEntity travelEntity = new LineEntity(fromPosition, toPosition);
        travelEntity.setColour(new Vector3D(1.0f, 0.9f, 1.0f));
        
        previousPosition = new Vector3D(toX, toY, toZ);
        
        return travelEntity;
    }
    
    private LineEntity processLayerChangeDirectiveNode(LayerChangeDirectiveNode layerChangeDirectiveNode) {
        // Switch Z and Y for the OpenGL coordinate system
        Movement toMove = layerChangeDirectiveNode.getMovement();
        double toX = (double) -toMove.getX();
        double toY;
        double toZ = (double) toMove.getY();
        if(toMove.isZSet()) {
            toY = (double) toMove.getZ();
            currentLayerHeight = (double) toMove.getZ();
            if(extrusionWidth == 0) {
                extrusionWidth = currentLayerHeight;
            }
        } else {
            toY = currentLayerHeight;
        }
        Vector3D toPosition = new Vector3D(toX, toY, toZ);
        Vector3D fromPosition = previousPosition;
          
        LineEntity layerChangeEntity = new LineEntity(fromPosition, toPosition);
        layerChangeEntity.setColour(new Vector3D(0.6f, 1.0f, 0.6f));
        
        previousPosition = new Vector3D(toX, toY, toZ);
        
        return layerChangeEntity;
    }
    
    private Entity processNozzleValvePositionDuringTravelNode(NozzleValvePositionDuringTravelNode nozzleValvePositionDuringTravelNode) {
        Movement toMove = nozzleValvePositionDuringTravelNode.getMovement();
        double toX = (double) -toMove.getX();
        double toY;
        double toZ = (double) toMove.getY();
        if(toMove.isZSet()) {
            toY = (double) toMove.getZ();
            currentLayerHeight = (double) toMove.getZ();
            if(extrusionWidth == 0) {
                extrusionWidth = currentLayerHeight;
            }
        } else {
            toY = currentLayerHeight;
        }
        Vector3D toPosition = new Vector3D(toX, toY, toZ);
        Vector3D fromPosition = previousPosition;
        
        // Calculate length of side
        Vector3D positionDiff = new Vector3D(toX - fromPosition.getX(), toY - fromPosition.getY(), toZ - fromPosition.getZ());
        double length = positionDiff.getNorm();
        
        // Calculate angle of rotation
        Vector3D positionDiffNormal = positionDiff.normalize();
        double angle = Vector3D.angle(positionDiffNormal, new Vector3D(1, 0, 0));
        if(toPosition.getZ() > fromPosition.getZ()) {
            angle = (double) Math.toRadians(180) - angle;
        }
        
        // Place position of entity in middle of vector
        Vector3D entityPosition = new Vector3D(0.5 * positionDiff.getX() + fromPosition.getX(),
                                               0.5 * positionDiff.getY() + fromPosition.getY(),
                                               0.5 * positionDiff.getZ() + fromPosition.getZ());
        
        Entity nozzleValvePositionDuringTravel = new Entity(entityPosition, 0, angle, 0, length, extrusionWidth, extrusionWidth);
        nozzleValvePositionDuringTravel.setColour(new Vector3D(1.0f, 0f, 1.0f));
        
        previousPosition = new Vector3D(toX, toY, toZ);
        
        return nozzleValvePositionDuringTravel;
    }
    
    private void processNozzleValvePositionNode(NozzleValvePositionNode nozzleValvePositionNode) {
        // Currently we don't do anything with these
    }
    
    private void processMCodeNode(MCodeNode mCodeNode) {
        // Currently we don't do anything with these.
    }
    
    private void processCommentNode(CommentNode commentNode) {
        // Currently we don't do anything with these.
    }
    
    private void processOrphanObjectDelineationNode(OrphanObjectDelineationNode orphanObjectDelineationNode) {
        // Currently we don't do anything with these.
    }
    
    private void processInnerPerimeterSectionNode(InnerPerimeterSectionNode innerPerimeterSectionNode) {
        // Currently we don't do anything with these.
    }
    
    private void processOuterPerimeterSectionNode(OuterPerimeterSectionNode outerPerimeterSectionNode) {
        // Currently we don't do anything with these.
    }
    
    private void processSkinSectionNode(SkinSectionNode skinSectionNode) {
        // Currently we don't do anything with these.
    }
    
    private void processFillSectionNode(FillSectionNode fillSectionNode) {
        // Currently we don't do anything with these.
    }
}
