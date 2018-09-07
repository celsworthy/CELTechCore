/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.gcodepreview.representation;

import celtech.coreUI.controllers.panels.TimeCostInsetPanelController;
import celtech.coreUI.gcodepreview.VectorUtils;
import celtech.coreUI.gcodepreview.entities.Entity;
import celtech.coreUI.gcodepreview.entities.LineEntity;
import celtech.coreUI.gcodepreview.entities.SequenceEntity;
import celtech.coreUI.gcodepreview.model.Layer;
import celtech.coreUI.visualisation.Xform;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableFloatArray;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

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
    
    private final Stenographer steno = StenographerFactory.getStenographer(LayerRepresentation.class.getName());

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
        layer.getLineEntities().forEach(this::renderLine);
        layer.getSequenceEntities().forEach(this::renderSequence);
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
        // For the moment, represent each entity as a scaled box.
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
    
    public void renderSequence(SequenceEntity sequenceEntity)
    {
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        ObservableFloatArray meshPoints = mesh.getPoints();
        ObservableFloatArray meshTexCoords = mesh.getTexCoords();
        ObservableFloatArray meshNormals = mesh.getNormals();
        ObservableFaceArray meshFaces = mesh.getFaces();
        
        // Create a mesh tube for the sequence.
        List<Vector3D> sequence = sequenceEntity.getSequence();

        // Texture coordinates. These are not used, but it seems nice to set them up properly.
        // Single texture over the end caps, with centre in the middle.
        meshTexCoords.addAll(0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F, 0.5F, 0.5F);
        // Single texture over the tube surface.
        meshTexCoords.addAll(0.0F, 0.25F, 0.0F, 0.5F, 0.0F, 0.75F, 1.0F, 0.25F, 1.0F, 0.5F, 1.0F, 0.75F);
        
        // There are at least 2 points.
        int vIndex = 0;
        int nIndex = 0;
        Vector3D u = null;
        Vector3D v = null;
        Vector3D w = null;

        Vector3D p0 = sequence.get(0);
        Vector3D p1 = null;
        Vector3D p = null;
        for (int index = 1; index < sequence.size(); ++index) {
            Vector3D vs = null;
            Vector3D ws = null;                
            p1 = sequence.get(index);
            try {
                u = p1.subtract(p0).normalize();
                if (v == null) {
                    v = VectorUtils.getNormalTo(u).normalize();
                    w = u.crossProduct(v).normalize();
                }
                else {
                    Vector3D uxv = u.crossProduct(v);
                    if (uxv.getNorm() > 10.0 * VectorUtils.EPSILON) {
                        w = u.crossProduct(v).normalize();
                        v = w.crossProduct(u).normalize();
                    }
                    else {
                        // Gone round a sharp corner, so find another normal.
                        v = VectorUtils.getNormalTo(u).normalize();
                        w = u.crossProduct(v).normalize();
                    }
                }
                vs = v.scalarMultiply(sequenceEntity.getRadius());
                ws = w.scalarMultiply(sequenceEntity.getRadius());                
            }
            catch(MathArithmeticException ex)
            {
                steno.error("Exception when rendering sequence " + ex);
            }

            if (meshPoints.size() == 0)
            {
                // Add the points for the end triangles:
                meshPoints.addAll((float)(p0.getX()), (float)(p0.getY()), (float)(p0.getZ()));
                meshNormals.addAll((float)(-u.getX()), (float)(-u.getY()), (float)(-u.getZ()));
                
                p = p0.add(vs);
                meshPoints.addAll((float)(p.getX()), (float)(p.getY()), (float)(p.getZ()));
                meshNormals.addAll((float)(v.getX()), (float)(v.getY()), (float)(v.getZ()));
                p = p0.add(ws);
                meshPoints.addAll((float)(p.getX()), (float)(p.getY()), (float)(p.getZ()));
                meshNormals.addAll((float)(v.getX()), (float)(v.getY()), (float)(v.getZ()));
                p = p0.subtract(vs);
                meshPoints.addAll((float)(p.getX()), (float)(p.getY()), (float)(p.getZ()));
                meshNormals.addAll((float)(-v.getX()), (float)(-v.getY()), (float)(-v.getZ()));
                p = p0.subtract(ws);
                meshPoints.addAll((float)(p.getX()), (float)(p.getY()), (float)(p.getZ()));
                meshNormals.addAll((float)(-w.getX()), (float)(-w.getY()), (float)(-w.getZ()));
 
                // Add the triangle faces.
                meshFaces.addAll(0, 0, 4, 1, 0, 0, 2, 0, 1);
                meshFaces.addAll(0, 0, 4, 2, 0, 1, 3, 0, 2);
                meshFaces.addAll(0, 0, 4, 3, 0, 2, 4, 0, 3);
                meshFaces.addAll(0, 0, 4, 4, 0, 3, 1, 0, 0);
                
                vIndex = 5;
                nIndex = 5;
            }

            // Add the points triangles for the walls of the tube.
            p = p1.add(vs);
            meshPoints.addAll((float)(p.getX()), (float)(p.getY()), (float)(p.getZ()));
            meshNormals.addAll((float)(v.getX()), (float)(v.getY()), (float)(v.getZ()));
            p = p1.add(ws);
            meshPoints.addAll((float)(p.getX()), (float)(p.getY()), (float)(p.getZ()));
            meshNormals.addAll((float)(w.getX()), (float)(w.getY()), (float)(w.getZ()));
            p = p1.subtract(vs);
            meshPoints.addAll((float)(p.getX()), (float)(p.getY()), (float)(p.getZ()));
            meshNormals.addAll((float)(-v.getX()), (float)(-v.getY()), (float)(-v.getZ()));
            p = p1.subtract(ws);
            meshPoints.addAll((float)(p.getX()), (float)(p.getY()), (float)(p.getZ()));
            meshNormals.addAll((float)(-w.getX()), (float)(-w.getY()), (float)(-w.getZ()));

            // Add the triangle faces.

            meshFaces.addAll(vIndex - 4, nIndex - 4, 0, vIndex, nIndex, 1, vIndex + 1, nIndex + 1, 8);
            meshFaces.addAll(vIndex - 4, nIndex - 4, 0, vIndex + 1, nIndex + 1, 8, vIndex - 3, nIndex - 3, 5);
            meshFaces.addAll(vIndex - 3, nIndex - 3, 5, vIndex + 1, nIndex + 1, 8, vIndex + 2, nIndex + 2, 9);
            meshFaces.addAll(vIndex - 3, nIndex - 3, 5, vIndex + 2, nIndex + 2, 9, vIndex - 2, nIndex - 2, 6);
            meshFaces.addAll(vIndex - 2, nIndex - 2, 6, vIndex + 2, nIndex + 2, 9, vIndex + 3, nIndex + 3, 10);
            meshFaces.addAll(vIndex - 2, nIndex - 2, 6, vIndex + 3, nIndex + 3, 10, vIndex - 1, nIndex - 1, 7);
            meshFaces.addAll(vIndex - 1, nIndex - 1, 7, vIndex + 3, nIndex + 3, 10, vIndex, nIndex, 2);
            meshFaces.addAll(vIndex - 1, nIndex - 1, 7, vIndex, nIndex, 2, vIndex - 4, nIndex - 4, 3);

            vIndex += 4;
            nIndex += 4;
            p0 = p1;
        }

        // Add the end cap.
        meshPoints.addAll((float)(p1.getX()), (float)(p1.getY()), (float)(p1.getZ()));
        meshNormals.addAll((float)(u.getX()), (float)(u.getY()), (float)(u.getZ()));

        // Add the triangle faces.
        meshFaces.addAll(vIndex, nIndex, 4, vIndex - 4, nIndex, 0, vIndex - 3, nIndex, 1);
        meshFaces.addAll(vIndex, nIndex, 4, vIndex - 3, nIndex, 1, vIndex - 2, nIndex, 2);
        meshFaces.addAll(vIndex, nIndex, 4, vIndex - 2, nIndex, 2, vIndex - 1, nIndex, 3);
        meshFaces.addAll(vIndex, nIndex, 4, vIndex - 1, nIndex, 3, vIndex - 4, nIndex, 0);

        MeshView meshView = new MeshView(mesh);
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.rgb((int)(255 * sequenceEntity.getColour().getX()),
                                           (int)(255 * sequenceEntity.getColour().getY()),
                                           (int)(255 * sequenceEntity.getColour().getZ())));
        material.setSpecularColor(material.getDiffuseColor().saturate());
        
        meshView.setDrawMode(DrawMode.FILL);
        meshView.cullFaceProperty().set(CullFace.NONE);
        meshView.setMaterial(material);
        layerGroup.getChildren().add(meshView);
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
