/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.gcodepreview.representation;

import celtech.coreUI.gcodepreview.entities.Entity;
import celtech.coreUI.gcodepreview.entities.LineEntity;
import celtech.coreUI.gcodepreview.model.Layer;
import celtech.coreUI.visualisation.Xform;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;

/**
 *
 * @author Tony
 */
public class LayerRepresentation {
    static private final double CYLINDER_RADIUS = 0.125;
    static private final double BOX_WIDTH = 1.0;
    static private final double RADIANS_TO_DEGREES = 57.2957795131;
    static private final PhongMaterial lineMaterial = new PhongMaterial();
    static private final PhongMaterial entityMaterial = new PhongMaterial();
    
    private final Xform world;
    private final Layer layer;
    private Group layerGroup;
    private List<Shape3D> lineReps;
    private boolean visible = true;
    private boolean linesVisible = false;
    
    public LayerRepresentation(Layer layer, Xform world)
    {
        this.world = world;
        this.layer = layer;
        this.layerGroup = null;
        this.lineReps = new ArrayList<>();
        
        lineMaterial.setDiffuseColor(Color.CORNFLOWERBLUE);
        lineMaterial.setSpecularColor(Color.LIGHTBLUE);
        entityMaterial.setDiffuseColor(Color.GREEN);
        entityMaterial.setSpecularColor(Color.LIGHTGREEN);
    }
    
    public Layer getLayer()
    {
        return layer;
    }
    
    public void render(boolean linesVisible)
    {
        this.linesVisible = linesVisible;
        layerGroup = new Group();
        layer.getEntities().forEach(this::renderEntity);
        layer.getLineEntitys().forEach(this::renderLine);
        layerGroup.setVisible(visible);
        world.getChildren().add(layerGroup);
    }
    
    public void renderLine(LineEntity line)
    {
        // For the moment, represent each piece of line as a cylinder.
        Cylinder cylinder = new Cylinder(CYLINDER_RADIUS, line.getLength());
        //Box cylinder = new Box(line.getLength(), BOX_WIDTH, BOX_WIDTH);
        cylinder.setMaterial(lineMaterial);
        Affine t = new Affine();
        t.appendTranslation(line.getPosition().getX(), line.getPosition().getY(), line.getPosition().getZ());
        t.appendRotation(RADIANS_TO_DEGREES * line.getRotationX(), 0.0, 0.0, 0.0, Rotate.X_AXIS);
        t.appendRotation(RADIANS_TO_DEGREES * line.getRotationY(), 0.0, 0.0, 0.0, Rotate.Y_AXIS);
        t.appendRotation(RADIANS_TO_DEGREES * line.getRotationZ(), 0.0, 0.0, 0.0, Rotate.Z_AXIS);
        t.appendRotation(90.0, 0.0, 0.0, 0.0, Rotate.Z_AXIS);
        cylinder.getTransforms().add(t);
        cylinder.setVisible(linesVisible);
        layerGroup.getChildren().add(cylinder);
        lineReps.add(cylinder);
    }
    
    public void renderEntity(Entity entity)
    {
        // For the moment, represent each entity as a scaledbox.
        Box box = new Box(BOX_WIDTH, BOX_WIDTH, BOX_WIDTH);
        box.setMaterial(entityMaterial);
        Affine t = new Affine();
        t.appendTranslation(entity.getPosition().getX(), entity.getPosition().getY(), entity.getPosition().getZ());
        t.appendRotation(RADIANS_TO_DEGREES * entity.getRotationX(), 0.0, 0.0, 0.0, Rotate.X_AXIS);
        t.appendRotation(RADIANS_TO_DEGREES * entity.getRotationY(), 0.0, 0.0, 0.0, Rotate.Y_AXIS);
        t.appendRotation(RADIANS_TO_DEGREES * entity.getRotationZ(), 0.0, 0.0, 0.0, Rotate.Z_AXIS);
        t.appendScale(entity.getScaleX(),entity.getScaleY(), entity.getScaleZ(), 0.0, 0.0, 0.0);
        box.getTransforms().add(t);
        layerGroup.getChildren().add(box);
    }
    
    public void setVisibility(boolean visible)
    {
        this.visible = visible;
        if (layerGroup != null)
            layerGroup.setVisible(visible);
    }

    public void setLineVisibility(boolean visible)
    {
        this.linesVisible = visible;
        lineReps.forEach(l -> l.setVisible(visible));
    }
}
