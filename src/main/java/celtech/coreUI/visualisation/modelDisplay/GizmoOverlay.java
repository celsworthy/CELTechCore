/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation.modelDisplay;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.visualisation.PolarCamera;
import celtech.coreUI.visualisation.SelectionContainer;
import celtech.coreUI.visualisation.Xform;
import celtech.coreUI.visualisation.importers.FloatArrayList;
import celtech.coreUI.visualisation.importers.IntegerArrayList;
import celtech.utils.Math.MathUtils;
import java.util.ArrayList;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Shape;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class GizmoOverlay extends Group
{
    
    private final Stenographer steno = StenographerFactory.getStenographer(GizmoOverlay.class.getName());
    private ApplicationStatus applicationStatus = null;
    
    private final PhongMaterial greyMaterial = new PhongMaterial(Color.LIGHTGREY);
    private final PhongMaterial greenMaterial = new PhongMaterial(Color.LIMEGREEN);
    private final PhongMaterial redMaterial = new PhongMaterial(Color.RED);
    
    private final Group selectionBox = new Group();
    private Xform selectionBoxBackLeftTop = null;
    private Xform selectionBoxBackRightTop = null;
    private Xform selectionBoxFrontLeftTop = null;
    private Xform selectionBoxFrontRightTop = null;
    private Xform selectionBoxBackLeftBottom = null;
    private Xform selectionBoxBackRightBottom = null;
    private Xform selectionBoxFrontLeftBottom = null;
    private Xform selectionBoxFrontRightBottom = null;
    
    private final double rotateRingStartingRadius = 50;
    private final double rotateRingBorder_mm = 10;
    private Xform rotateRingXform = null;
    private final Line rotateStartHandle = new Line();
    private final Line rotateEndHandle = new Line();
    private Shape rotationDisc = null;
    
    private MeshView xControlArrow = new MeshView();
    private MeshView zControlArrow = new MeshView();
    
    private float latestCameraAzimuth = 0;
    private float latestCameraPitch = 0;
    
    private double centreX = 0;
    private double centreZ = 0;
    
    private GizmoMode mode = GizmoMode.IDLE;
    private double rotationStartAngle = 0.0;
    private double currentRotationAngle = 0.0;
    
    public GizmoOverlay(SelectionContainer selectionContainer, PolarCamera camera)
    {
        applicationStatus = ApplicationStatus.getInstance();
        rotateStartHandle.setStartX(0);
        rotateStartHandle.setStartY(0);
        rotateStartHandle.setEndX(10);
        rotateStartHandle.setEndY(0);
        rotateStartHandle.setStroke(Color.GREEN);
        rotateStartHandle.setStrokeWidth(2);
//        displayManager = DisplayManager.getInstance();
        /*
         rotateRing.setMaterial(greyMaterial);
         rotateRing.setTranslateY(-.25);
        
         rotateStartHandle.setMaterial(greenMaterial);
         rotateStartHandle.setTranslateY(-.5);
         rotateEndHandle.setMaterial(redMaterial);
         rotateEndHandle.setTranslateY(-.5);
        
         rotateRingXform = new Xform();
         rotateRingXform.getChildren().addAll(rotateRing, rotateStartHandle, rotateEndHandle);
         getChildren().add(rotateRingXform);
        
         buildSelectionBox();
         getChildren().add(selectionBox);
         */
        /*
         xControlArrow.setMesh(buildArrow());
         xControlArrow.setMaterial(greenMaterial);
         xControlArrow.setTranslateY(-1);
        
         zControlArrow.setMesh(buildArrow());
         zControlArrow.setMaterial(redMaterial);
         zControlArrow.setTranslateY(-1);
         zControlArrow.setRotate(-90);
        
         getChildren().addAll(xControlArrow, zControlArrow);
         */

//        Path semi = drawSemiRing(50, 50, 100, 80, Color.CORAL, Color.BLACK);
//        ringXform.getChildren().add(semi);
//        ringXform.setRotateY(45);
//        getChildren().add(ringXform);
        rotationDisc = drawDiscWithHole(0, 0, 100, 80, Color.ALICEBLUE, Color.BLACK);
        steno.info("disc bounds L " + rotationDisc.getBoundsInLocal());
        steno.info("disc bounds P " + rotationDisc.getBoundsInParent());
        
        double arrowShaftWidth = 20;
        double arrowHeadWidth = 40;
        double arrowHeadLength = 20;
        double arrowLength = 80;
        
        ArrayList<PathElement> xArrowElements = new ArrayList<>();
        xArrowElements.add(new MoveTo(-arrowShaftWidth / 2, -arrowShaftWidth / 2));
        xArrowElements.add(new LineTo(arrowLength - arrowHeadLength, -arrowShaftWidth / 2));
        xArrowElements.add(new LineTo(arrowLength - arrowHeadLength, -arrowHeadWidth / 2));
        xArrowElements.add(new LineTo(arrowLength, 0));
        xArrowElements.add(new LineTo(arrowLength - arrowHeadLength, arrowHeadWidth / 2));
        xArrowElements.add(new LineTo(arrowLength - arrowHeadLength, arrowShaftWidth / 2));
        xArrowElements.add(new LineTo(arrowShaftWidth / 2, arrowShaftWidth / 2));
        xArrowElements.add(new ClosePath());
        
        Path xArrowPath = new Path(xArrowElements);
        xArrowPath.setFill(ApplicationConfiguration.xAxisColour);
        xArrowPath.setCacheHint(CacheHint.ROTATE);
        
        Xform xArrowXform = new Xform();
        xArrowXform.getChildren().add(xArrowPath);
        
        ArrayList<PathElement> zArrowElements = new ArrayList<>();
        zArrowElements.add(new MoveTo(-arrowShaftWidth / 2, -arrowShaftWidth / 2));
        zArrowElements.add(new LineTo(-arrowShaftWidth / 2, arrowLength - arrowHeadLength));
        zArrowElements.add(new LineTo(-arrowHeadWidth / 2, arrowLength - arrowHeadLength));
        zArrowElements.add(new LineTo(0, arrowLength));
        zArrowElements.add(new LineTo(arrowHeadWidth / 2, arrowLength - arrowHeadLength));
        zArrowElements.add(new LineTo(arrowShaftWidth / 2, arrowLength - arrowHeadLength));
        zArrowElements.add(new LineTo(arrowShaftWidth / 2, arrowShaftWidth / 2));
        zArrowElements.add(new ClosePath());
        
        Path zArrowPath = new Path(zArrowElements);
        zArrowPath.setFill(ApplicationConfiguration.zAxisColour);
        zArrowPath.setCacheHint(CacheHint.ROTATE);
        
        Xform zArrowXform = new Xform();
        zArrowXform.getChildren().add(zArrowPath);
        
        getChildren().add(rotationDisc);
        
        getChildren().add(rotateStartHandle);
        
        getChildren().addAll(xArrowXform, zArrowXform);
        
        steno.info("gizmo bounds L " + this.getBoundsInLocal());
        steno.info("P " + this.getBoundsInParent());
        
        Group me = this;

//        selectionContainer.centreXProperty().addListener(new ChangeListener<Number>()
//        {
//            @Override
//            public void changed(ObservableValue<? extends Number> ov, Number t, Number newCentreX)
//            {
//                centreX = newCentreX.doubleValue();
//            }
//        });
//
//        selectionContainer.centreZProperty().addListener(new ChangeListener<Number>()
//        {
//
//            @Override
//            public void changed(ObservableValue<? extends Number> ov, Number t, Number newCentreZ)
//            {
//                centreZ = newCentreZ.doubleValue();
//            }
//        });
        this.visibleProperty().bind(Bindings.isNotEmpty(selectionContainer.selectedModelsProperty()).and(applicationStatus.modeProperty().isNotEqualTo(ApplicationMode.SETTINGS)));
        
        xArrowXform.setRz(camera.getCameraAzimuthRadians() * MathUtils.RAD_TO_DEG);
        zArrowXform.setRz(camera.getCameraAzimuthRadians() * MathUtils.RAD_TO_DEG);
        
        camera.cameraAzimuthRadiansProperty().addListener(new ChangeListener<Number>()
        {
            
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                xArrowXform.setRz(t1.doubleValue() * MathUtils.RAD_TO_DEG);
                zArrowXform.setRz(t1.doubleValue() * MathUtils.RAD_TO_DEG);
                
            }
        });
        
        rotationDisc.setOnMousePressed(new EventHandler<MouseEvent>()
        {
            
            @Override
            public void handle(MouseEvent t)
            {
                enterRotateMode(t.getX(), t.getY());
            }
        });
        
        rotationDisc.setOnMouseDragged(new EventHandler<MouseEvent>()
        {
            
            @Override
            public void handle(MouseEvent t)
            {
                rotate(t.getX(), t.getY());
            }
        });
        
        rotationDisc.setOnMouseReleased(new EventHandler<MouseEvent>()
        {
            
            @Override
            public void handle(MouseEvent t)
            {
                exitRotateMode();
            }
        });
        
    }
    
    private void enterRotateMode(double xPos, double yPos)
    {
        mode = GizmoMode.ROTATE;
        steno.info("got " + xPos + ":" + yPos);
        steno.info("Angle " + MathUtils.cartesianToAngleDegreesCWFromTop(xPos, yPos));
    }
    
    private void exitRotateMode()
    {
        mode = GizmoMode.IDLE;
    }
    
    private void rotate(double xPos, double yPos)
    {
        if (mode == GizmoMode.ROTATE)
        {
            steno.info("got " + xPos + ":" + yPos);
            steno.info("Angle " + MathUtils.cartesianToAngleDegreesCWFromTop(xPos, yPos));
        }
    }
    
    private double getRotationAngleFromPosition(double xPos, double yPos)
    {
        double rotationAngle = 0;
        
        return rotationAngle;
    }
    
    private void centreChanged()
    {
        steno.info("Before " + centreX + ":" + centreZ);
//
//        Point2D translate = ringXform.sceneToLocal(newCentre.getX(), newCentre.getY());
//        steno.info("After " + translate.getX() + " Y" + translate.getY());
//        me.setTranslateX(newCentre.getX());
//        me.setTranslateY(newCentre.getY());
    }
    
    private Shape drawDiscWithHole(double centreX, double centreY, double outerRadius, double innerRadius, Color fillColour, Color strokeColour)
    {
        Circle innerCircle = new Circle();
        innerCircle.setCenterX(centreX);
        innerCircle.setCenterY(centreY);
        innerCircle.setRadius(innerRadius);
        
        Circle outerCircle = new Circle();
        outerCircle.setCenterX(centreX);
        outerCircle.setCenterY(centreY);
        outerCircle.setRadius(outerRadius);
        
        Shape disc = Path.subtract(outerCircle, innerCircle);
        disc.setFill(fillColour);
        disc.setStroke(strokeColour);
        
        return disc;
    }
    
    private Path drawSemiRing(double centerX, double centerY, double radius, double innerRadius, Color bgColor, Color strkColor)
    {
        Path path = new Path();
        path.setFill(bgColor);
        path.setStroke(strkColor);
        path.setFillRule(FillRule.EVEN_ODD);
        
        MoveTo moveTo = new MoveTo();
        moveTo.setX(centerX + innerRadius);
        moveTo.setY(centerY);
        
        ArcTo arcToInner = new ArcTo();
        arcToInner.setX(centerX - innerRadius);
        arcToInner.setY(centerY);
        arcToInner.setRadiusX(innerRadius);
        arcToInner.setRadiusY(innerRadius);
        
        MoveTo moveTo2 = new MoveTo();
        moveTo2.setX(centerX + innerRadius);
        moveTo2.setY(centerY);
        
        HLineTo hLineToRightLeg = new HLineTo();
        hLineToRightLeg.setX(centerX + radius);
        
        ArcTo arcTo = new ArcTo();
        arcTo.setX(centerX - radius);
        arcTo.setY(centerY);
        arcTo.setRadiusX(radius);
        arcTo.setRadiusY(radius);
        
        HLineTo hLineToLeftLeg = new HLineTo();
        hLineToLeftLeg.setX(centerX - innerRadius);
        
        path.getElements().add(moveTo);
        path.getElements().add(arcToInner);
        path.getElements().add(moveTo2);
        path.getElements().add(hLineToRightLeg);
        path.getElements().add(arcTo);
        path.getElements().add(hLineToLeftLeg);
        
        return path;
    }
    
    private TriangleMesh buildArrow()
    {
        TriangleMesh triangleMesh = new TriangleMesh();
        
        FloatArrayList vertices = new FloatArrayList();
        IntegerArrayList faces = new IntegerArrayList();
        
        FloatArrayList texCoords = new FloatArrayList();
        texCoords.add(0f);
        texCoords.add(0f);
        
        final float shaftLength = 5;
        final float shaftWidth = 2;
        final float arrowHeadWidth = 5;
        final float arrowHeadLength = 3;

        //Point 0
        vertices.add(0f);
        vertices.add(0f);
        vertices.add(-shaftWidth / 2);

        //Point 1
        vertices.add(shaftLength);
        vertices.add(0f);
        vertices.add(-shaftWidth / 2);

        //Point 2
        vertices.add(shaftLength);
        vertices.add(0f);
        vertices.add(-arrowHeadWidth / 2);

        //Point 3
        vertices.add(shaftLength + arrowHeadLength);
        vertices.add(0f);
        vertices.add(0f);

        //Point 4
        vertices.add(shaftLength);
        vertices.add(0f);
        vertices.add(arrowHeadWidth / 2);

        //Point 5
        vertices.add(shaftLength);
        vertices.add(0f);
        vertices.add(arrowHeadWidth / 2);

        //Point 6
        vertices.add(shaftLength);
        vertices.add(0f);
        vertices.add(0f);

        //Point 7
        vertices.add(0f);
        vertices.add(0f);
        vertices.add(shaftWidth / 2);
        
        faces.add(0);
        faces.add(1);
        faces.add(5);
        
        faces.add(0);
        faces.add(5);
        faces.add(7);
        
        faces.add(6);
        faces.add(3);
        faces.add(4);
        
        faces.add(6);
        faces.add(2);
        faces.add(3);
        
        triangleMesh.getPoints().addAll(vertices.toFloatArray());
        triangleMesh.getTexCoords().addAll(texCoords.toFloatArray());
        triangleMesh.getFaces().addAll(faces.toIntArray());
        
        return triangleMesh;
    }
    
    private void buildSelectionBox()
    {
        selectionBoxBackLeftBottom = generateSelectionCornerGroup();
        selectionBoxBackLeftBottom.setRotateY(90);
        
        selectionBoxBackRightBottom = generateSelectionCornerGroup();
        selectionBoxBackRightBottom.setRotateY(-180);
        
        selectionBoxBackLeftTop = generateSelectionCornerGroup();
        selectionBoxBackLeftTop.setRotateX(180);
        
        selectionBoxBackRightTop = generateSelectionCornerGroup();
        selectionBoxBackRightTop.setRotateX(180);
        selectionBoxBackRightTop.setRotateY(90);
        
        selectionBoxFrontLeftBottom = generateSelectionCornerGroup();
        
        selectionBoxFrontRightBottom = generateSelectionCornerGroup();
        selectionBoxFrontRightBottom.setRotateY(-90);
        
        selectionBoxFrontLeftTop = generateSelectionCornerGroup();
        selectionBoxFrontLeftTop.setRotateX(180);
        selectionBoxFrontLeftTop.setRotateY(-90);
        
        selectionBoxFrontRightTop = generateSelectionCornerGroup();
        selectionBoxFrontRightTop.setRotateZ(180);
        
        selectionBox.getChildren().addAll(selectionBoxBackLeftBottom, selectionBoxBackLeftTop, selectionBoxBackRightBottom, selectionBoxBackRightTop,
                selectionBoxFrontLeftBottom, selectionBoxFrontLeftTop, selectionBoxFrontRightBottom, selectionBoxFrontRightTop);
        
    }
    
    private Xform generateSelectionCornerGroup()
    {
        final int cylSamples = 4;
        final double cylHeight = 2.5;
        final double cylRadius = .1;
        
        Xform selectionCornerTransform = new Xform();
        Group selectionCorner = new Group();
        selectionCornerTransform.getChildren().add(selectionCorner);
        
        Cylinder part1 = new Cylinder(cylRadius, cylHeight, cylSamples);
        part1.setMaterial(greenMaterial);
        part1.setTranslateY(-cylHeight / 2);
        
        Cylinder part2 = new Cylinder(cylRadius, cylHeight, cylSamples);
        part2.setMaterial(greenMaterial);
        part2.setRotationAxis(MathUtils.zAxis);
        part2.setRotate(-90);
        part2.setTranslateX(cylHeight / 2);
        
        Cylinder part3 = new Cylinder(cylRadius, cylHeight, cylSamples);
        part3.setMaterial(greenMaterial);
        part3.setRotationAxis(MathUtils.xAxis);
        part3.setRotate(-90);
        part3.setTranslateZ(cylHeight / 2);
        
        selectionCorner.getChildren().addAll(part1, part2, part3);
        
        return selectionCornerTransform;
    }
    
    private void selectionBoundsChanged(Bounds newBounds)
    {
        selectionBoxBackLeftBottom.setTz(newBounds.getMaxZ());
        selectionBoxBackLeftBottom.setTx(newBounds.getMinX());
        
        selectionBoxBackRightBottom.setTz(newBounds.getMaxZ());
        selectionBoxBackRightBottom.setTx(newBounds.getMaxX());
        
        selectionBoxFrontLeftBottom.setTz(newBounds.getMinZ());
        selectionBoxFrontLeftBottom.setTx(newBounds.getMinX());
        
        selectionBoxFrontRightBottom.setTz(newBounds.getMinZ());
        selectionBoxFrontRightBottom.setTx(newBounds.getMaxX());
        
        selectionBoxBackLeftTop.setTz(newBounds.getMaxZ());
        selectionBoxBackLeftTop.setTx(newBounds.getMinX());
        selectionBoxBackLeftTop.setTy(newBounds.getMinY());
        
        selectionBoxBackRightTop.setTz(newBounds.getMaxZ());
        selectionBoxBackRightTop.setTx(newBounds.getMaxX());
        selectionBoxBackRightTop.setTy(newBounds.getMinY());
        
        selectionBoxFrontLeftTop.setTz(newBounds.getMinZ());
        selectionBoxFrontLeftTop.setTx(newBounds.getMinX());
        selectionBoxFrontLeftTop.setTy(newBounds.getMinY());
        
        selectionBoxFrontRightTop.setTz(newBounds.getMinZ());
        selectionBoxFrontRightTop.setTx(newBounds.getMaxX());
        selectionBoxFrontRightTop.setTy(newBounds.getMinY());
    }
    
    private void compensateForCameraPosition()
    {
        steno.info("pitch " + latestCameraPitch + " az " + latestCameraAzimuth);
        MathUtils.matrixRotateNode(rotationDisc, 0, latestCameraPitch - Math.PI, latestCameraAzimuth);
    }
    
    public void compensateForCameraPitch(float cameraPitch)
    {
        latestCameraPitch = cameraPitch;
        compensateForCameraPosition();
    }
    
    public void compensateForCameraAzimuth(float cameraAzimuth)
    {
        latestCameraAzimuth = cameraAzimuth;
        compensateForCameraPosition();
    }
}
