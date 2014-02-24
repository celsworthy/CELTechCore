/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation;

import celtech.configuration.PrintBed;
import celtech.utils.Math.MathUtils;
import celtech.utils.Math.PolarCoordinate;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PolarCamera extends PerspectiveCamera
{

    private final static Stenographer steno = StenographerFactory.getStenographer(PolarCamera.class.getName());

    private double cameraDollyPitch = 0;
    private final double minElevation = 0;
    private final double maxElevation = 89.9;
    private double cameraDollyAzimuth = 0;
    private final double minAzimuth = 0;
    private final double maxAzimuth = 360;
    private DoubleProperty cameraDistance = new SimpleDoubleProperty(50);
    private final double minCameraDistance = 1;
    private final double maxCameraDistance = 600;
    private Point3D centreOfRotation = new Point3D(PrintBed.getPrintVolumeCentre().getX(), PrintBed.getPrintVolumeCentre().getY(), PrintBed.getPrintVolumeCentre().getZ());
    private final PolarCoordinate cameraPolarCoordinates = new PolarCoordinate(0, 0, 0);

    private FloatProperty cameraPitchRadiansProperty = new SimpleFloatProperty();
    private FloatProperty cameraAzimuthRadiansProperty = new SimpleFloatProperty();

    public PolarCamera()
    {
        super(true);

        this.setNearClip(1.0); // TODO: Workaround as per RT-31255
        this.setFarClip(10000.0); // TODO: Workaround as per RT-31255
        setCameraAzimuthRadians((float) cameraPolarCoordinates.getPhi());
        setCameraPitchRadians((float) cameraPolarCoordinates.getTheta());

//        this.getTransforms().addAll(
//                cameraLookXRotate, cameraLookZRotate, cameraPosition);
    }

    public void rotateAndElevateCameraTo(double newAzimuth, double newElevation)
    {
        cameraDollyPitch = newElevation;
        if (cameraDollyPitch > maxElevation)
        {
            cameraDollyPitch = maxElevation;
        } else if (cameraDollyPitch < minElevation)
        {
            cameraDollyPitch = minElevation;
        }

        cameraDollyAzimuth = newAzimuth;
        if (cameraDollyAzimuth > maxAzimuth)
        {
            cameraDollyAzimuth -= maxAzimuth;
        } else if (cameraDollyAzimuth < minAzimuth)
        {
            cameraDollyAzimuth += maxAzimuth;
        }

        cameraPolarCoordinates.setPhiDegrees(cameraDollyAzimuth);
        cameraPolarCoordinates.setThetaDegrees(cameraDollyPitch);

        updateCameraLocation();
    }

    public void rotateAndElevateCamera(double azimuthChange, double elevationChange)
    {
        cameraDollyPitch += elevationChange;
        if (cameraDollyPitch > maxElevation)
        {
            cameraDollyPitch = maxElevation;
        } else if (cameraDollyPitch < minElevation)
        {
            cameraDollyPitch = minElevation;
        }

        cameraDollyAzimuth += azimuthChange;
        if (cameraDollyAzimuth > maxAzimuth)
        {
            cameraDollyAzimuth -= maxAzimuth;
        } else if (cameraDollyAzimuth < minAzimuth)
        {
            cameraDollyAzimuth += maxAzimuth;
        }

        cameraPolarCoordinates.setPhiDegrees(cameraDollyAzimuth);
        cameraPolarCoordinates.setThetaDegrees(cameraDollyPitch);

        updateCameraLocation();
    }

    public void zoomCamera(double zoomFactor)
    {
        cameraDistance.set(cameraDistance.get()*zoomFactor);
        if (cameraDistance.get() > maxCameraDistance)
        {
            cameraDistance.set(maxCameraDistance);
        } else if (cameraDistance.get() < minCameraDistance)
        {
            cameraDistance.set(minCameraDistance);
        }

        cameraPolarCoordinates.setRadius(cameraDistance.get());
        updateCameraLocation();
    }

    public void zoomCameraTo(double distance)
    {
        cameraDistance.set(distance);
        if (cameraDistance.get() > maxCameraDistance)
        {
            cameraDistance.set(maxCameraDistance);
        } else if (cameraDistance.get() < minCameraDistance)
        {
            cameraDistance.set(minCameraDistance);
        }

        cameraPolarCoordinates.setRadius(cameraDistance.get());
        updateCameraLocation();
    }

    public void alterZoom(double distanceDifference)
    {
        cameraDistance.set(cameraDistance.get() + distanceDifference);
        if (cameraDistance.get() > maxCameraDistance)
        {
            cameraDistance.set(maxCameraDistance);
        } else if (cameraDistance.get() < minCameraDistance)
        {
            cameraDistance.set(minCameraDistance);
        }

        cameraPolarCoordinates.setRadius(cameraDistance.get());
        updateCameraLocation();
    }

    public void updateCameraLocation()
    {
//        steno.info("Reposition camera to A:" + cameraDollyAzimuth + " E:" + cameraDollyPitch + " D:" + cameraDistance);

        Point3D newCameraPosition = MathUtils.sphericalToCartesianLocalSpaceAdjusted(cameraPolarCoordinates);

//        steno.info("New centre " + centreOfRotation);
        setTranslateX(newCameraPosition.getX() + centreOfRotation.getX());
        setTranslateY(newCameraPosition.getY() + centreOfRotation.getY());
        setTranslateZ(newCameraPosition.getZ() + centreOfRotation.getZ());
        // Roll(+ = clockwise), Pitch(+ = down), Yaw(+ = left)
        setCameraAzimuthRadians((float) cameraPolarCoordinates.getPhi());
        setCameraPitchRadians((float) cameraPolarCoordinates.getTheta());
        MathUtils.matrixRotateNode(this, 0, cameraPolarCoordinates.getTheta(), cameraPolarCoordinates.getPhi());
//        steno.info("Camera demand: Az-" + cameraDollyAzimuth + " El-" + cameraDollyPitch + " Dist-" + cameraDistance);

    }

    void translateCamera(double xTranslate, double yTranslate)
    {
        double deltaX = xTranslate * Math.cos(cameraPolarCoordinates.getPhi());
        double deltaZ = xTranslate * Math.sin(cameraPolarCoordinates.getPhi());
        centreOfRotation = new Point3D(centreOfRotation.getX() + deltaX, centreOfRotation.getY() + yTranslate, centreOfRotation.getZ() + deltaZ);
        updateCameraLocation();
    }

    void setCentreOfRotation(Point3D newCentrePosition)
    {
        centreOfRotation = newCentrePosition;
        updateCameraLocation();
    }

    public FloatProperty cameraAzimuthRadiansProperty()
    {
        return cameraAzimuthRadiansProperty;
    }

    public void setCameraAzimuthRadians(float value)
    {
        cameraAzimuthRadiansProperty.set(value);
    }

    public float getCameraAzimuthRadians()
    {
        return cameraAzimuthRadiansProperty.get();
    }

    public FloatProperty cameraPitchRadiansProperty()
    {
        return cameraPitchRadiansProperty;
    }

    public void setCameraPitchRadians(float value)
    {
        cameraPitchRadiansProperty.set(value);
    }

    public float getCameraPitchRadians()
    {
        return cameraPitchRadiansProperty.get();
    }

    public void gotoPreset(CameraPositionPreset cameraPositionPreset)
    {
        setCentreOfRotation(new Point3D(PrintBed.getPrintVolumeCentre().getX(), PrintBed.getPrintVolumeCentre().getY(), PrintBed.getPrintVolumeCentre().getZ()));
        rotateAndElevateCameraTo(cameraPositionPreset.getAzimuth(), cameraPositionPreset.getElevation());
        zoomCameraTo(cameraPositionPreset.getDistance());
    }
    
    public DoubleProperty cameraDistanceProperty()
    {
        return cameraDistance;
    }
}
