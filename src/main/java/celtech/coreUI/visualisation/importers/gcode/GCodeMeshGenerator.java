/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation.importers.gcode;

import celtech.coreUI.visualisation.ApplicationMaterials;
import celtech.coreUI.visualisation.Xform;
import celtech.utils.Math.MathUtils;
import celtech.utils.Math.PolarCoordinate;
import celtech.utils.gcode.representation.CompoundMovement;
import celtech.utils.gcode.representation.GCodeFile;
import celtech.utils.gcode.representation.GCodeElement;
import celtech.utils.gcode.representation.GCodeMeshData;
import celtech.utils.gcode.representation.Layer;
import celtech.utils.gcode.representation.Movement;
import celtech.utils.gcode.representation.MovementType;
import java.util.ArrayList;
import java.util.HashMap;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Point3D;
import javafx.scene.CacheHint;
import javafx.scene.shape.Cylinder;
import javafx.scene.Group;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Line;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class GCodeMeshGenerator
{

    private static Stenographer steno = StenographerFactory.getStenographer(GCodeMeshGenerator.class.getName());
    public static final String layerPrefix = "Layer";

    public static GCodeMeshData generateMesh(GCodeFile gcodeMetaFile, DoubleProperty progressProperty)
    {
        Group gcodeParts = new Group();
        HashMap<Integer, GCodeElement> referencedElements = new HashMap<>();
        HashMap<Integer, Group> referencedLayers = new HashMap<>();

        Point3D lastPosition = Point3D.ZERO;
        
        int progressCounter = 0;
        int numberOfLayers = gcodeMetaFile.getLayers().size();

        for (Layer layer : gcodeMetaFile.getLayers())
        {
            progressProperty.set(((double)progressCounter / (double)numberOfLayers) * 100.0);
            
            steno.info("Processing layer " + progressCounter + " of " + numberOfLayers);
            
            Group layerNode = new Group();
            layerNode.setId(String.valueOf(layer.getLayerNumber()));
            referencedLayers.put(layer.getLayerNumber(), layerNode);

            for (CompoundMovement movement : layer.getMovements())
            {
                ArrayList<Cylinder> geomList = new ArrayList<>();

                String labelForGeom = movement.getMovementType() + ":" + movement.getStartingGCodeLine() + "-" + movement.getEndingGCodeLine();

                for (Movement segment : movement.getSegments())
                {
//                    if (movement.getMovementType() == MovementType.TRAVEL)
//                    {
//                                                           Point3D reorient = segment.getTargetPosition().subtract(lastPosition);
//
//                        Line travelLine = new Line(lastPosition.getX(), lastPosition.getY(), reorient.getX(), reorient.getY());
////                        B travelLine = new Line(lastPosition, position);
////                        geom.setMaterial(materials.getTravelMaterial());
//
////                        geomList.add(geom);
//                        layerNode.getChildren().add(travelLine);
//
//                    } else
                    {

                        Point3D reorient = segment.getTargetPosition().subtract(lastPosition);
                        PolarCoordinate polarCoord = MathUtils.cartesianToSphericalLocalSpaceUnadjusted(reorient);

//                        cylXform.setScale(1, reorient.magnitude(), 1);
                        Box newCyl = new Box(.25, reorient.magnitude(), .25);
                        
//                        Cylinder newCyl = new Cylinder(.25, reorient.magnitude(), 4);
                        newCyl.setUserData(layer);
                        newCyl.setId(String.valueOf(segment.getGCodeLineNumber()));
                        newCyl.setRotationAxis(MathUtils.zAxis);
                        newCyl.setRotate(90);
                        newCyl.setTranslateX(reorient.magnitude() / 2);
//                        referencedElements.put(segment.getGCodeLineNumber(), new GCodeElement(newCyl, movement.getMovementType()));

                        Xform cylZRotXform = new Xform();
                        cylZRotXform.getChildren().add(newCyl);
                        cylZRotXform.setRz(360 - (MathUtils.RAD_TO_DEG * polarCoord.getTheta()));

                        Xform cylYRotXForm = new Xform();
                        cylYRotXForm.getChildren().add(cylZRotXform);
                        cylYRotXForm.setRy(360 - (MathUtils.RAD_TO_DEG * polarCoord.getPhi()));

                        cylYRotXForm.setTx(lastPosition.getX());
                        cylYRotXForm.setTz(lastPosition.getZ());
                        cylYRotXForm.setTy(lastPosition.getY());
//                        cylXform.setTy(-reorient.magnitude() / 2);
//                        cylXform.setTx(position.getX());
//                        cylXform.setTz(position.getZ());
//                        Quaternion rotationDemandZ = new Quaternion();
//                        rotationDemandZ.lookAt(reorient, Point3D.UNIT_Z);
//
//                        Quaternion rotationDemandY = new Quaternion();
//                        rotationDemandY.lookAt(reorient, Point3D.UNIT_Y);
//
//                        Quaternion currentRotation = geom.getLocalRotation();
//
//                        Quaternion resultantRotation = rotationDemandZ.mult(currentRotation);
//                        Quaternion finalRotation = rotationDemandY.mult(resultantRotation);

//                        Point3D midpoint = lastPosition.midpoint(position);
//                        newCyl.setTranslateX(midpoint.getX());
//                        newCyl.setTranslateZ(midpoint.getZ());
//                        geom.setLocalRotation(resultantRotation);
                        newCyl.setMaterial(ApplicationMaterials.getGCodeMaterial(movement.getMovementType(), false));
                        newCyl.setCacheHint(CacheHint.SPEED);
                        newCyl.setCullFace(CullFace.BACK);

                        layerNode.getChildren().add(cylYRotXForm);
                    }

                    lastPosition = segment.getTargetPosition();
                }
            }

            steno.info("Adding layer " + layerNode.getId() + " with " + layerNode.getChildren().size() + " elements");
            gcodeParts.getChildren().add(layerNode);
            
            progressCounter++;
        }

        return new GCodeMeshData(gcodeParts, referencedElements, referencedLayers);
    }
}
