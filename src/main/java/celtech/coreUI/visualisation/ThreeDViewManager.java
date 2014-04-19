


package celtech.coreUI.visualisation;

import celtech.CoreTest;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintBed;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.controllers.GizmoOverlayController;
import celtech.coreUI.visualisation.importers.ModelLoadResult;
import celtech.coreUI.visualisation.importers.obj.ObjImporter;
import celtech.coreUI.visualisation.modelDisplay.ModelBounds;
import celtech.coreUI.visualisation.modelDisplay.SelectionHighlighter;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelContentsEnumeration;
import com.leapmotion.leap.Controller;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ThreeDViewManager
{

    private static final Stenographer steno = StenographerFactory.getStenographer(ThreeDViewManager.class.getName());

    private ObservableList<ModelContainer> loadedModels = null;
    private final SelectionContainer selectionContainer = new SelectionContainer();
    private final ApplicationStatus applicationStatus = ApplicationStatus.getInstance();

    private final PrintBed printBedData = PrintBed.getInstance();
    private final Group root3D = new Group();
    private SubScene subScene = null;
    private final SimpleObjectProperty<SubScene> subSceneProperty = new SimpleObjectProperty<>();

    private final PointLight pointLight1 = new PointLight(Color.WHITE);
    private final AmbientLight ambientLight = new AmbientLight(Color.WHITE);

    final Group axisGroup = new Group();
    double DELTA_MULTIPLIER = 200.0;
    double CONTROL_MULTIPLIER = 0.1;
    double SHIFT_MULTIPLIER = 0.1;
    double ALT_MULTIPLIER = 0.5;

    private final SimpleObjectProperty<Timeline> timeline = new SimpleObjectProperty<>();
    private ObjectProperty<Node> content = new SimpleObjectProperty<>();
    /*
     * Model moving
     */
    private Point3D lastDragPosition = null;
    private final int dragPlaneHalfSize = 500;
    private final Box translationDragPlane = new Box(dragPlaneHalfSize * 2, 0.1, dragPlaneHalfSize * 2);
    private final Box scaleDragPlane = new Box(dragPlaneHalfSize * 2, dragPlaneHalfSize * 2, 0.1);
    private SelectionHighlighter threeDControl = null;
    private GizmoOverlayController gizmoOverlayController = null;
    /*
     * 
     */
    private Group gcodeParts = null;
    /*
    
     */
    private Group models = new Group();
    /*
     * Selection stuff
     */
    private BooleanProperty dragMode = new SimpleBooleanProperty(false);


    /*
     * Leap Motion
     */
    private Controller leapController = null;
    private LeapMotionListener leapMotionListener = null;
    /*
    
     */
    private ReadOnlyDoubleProperty widthPropertyToFollow = null;
    private ReadOnlyDoubleProperty heightPropertyToFollow = null;

    private IntegerProperty screenCentreOfSelectionX = new SimpleIntegerProperty(0);
    private IntegerProperty screenCentreOfSelectionY = new SimpleIntegerProperty(0);

    /*
     * ALT stuff
     */
    private final Xform bedTranslateXform = new Xform(Xform.RotateOrder.YXZ);
    private final PerspectiveCamera camera = new PerspectiveCamera(true);

//    private final Rotate rotateCameraAroundXAxis = new Rotate(0, MathUtils.xAxis);
//    private final Rotate rotateCameraAroundYAxis = new Rotate(0, MathUtils.yAxis);
//    private final Rotate rotateCameraAroundZAxis = new Rotate(0, MathUtils.zAxis);
//    private final Translate translateCamera = new Translate();
    private final DoubleProperty cameraDistance = new SimpleDoubleProperty(-350);
    private final DoubleProperty demandedCameraRotationX = new SimpleDoubleProperty(0);
    private final DoubleProperty demandedCameraRotationY = new SimpleDoubleProperty(0);

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    private double bedXOffsetFromCameraZero;
    private double bedZOffsetFromCameraZero;

    private double dragStartX, dragStartY;

    public void rotateCameraAroundAxes(double xangle, double yangle)
    {
        double yAxisRotation = demandedCameraRotationY.get() - yangle;

        if (yAxisRotation > 360)
        {
            yAxisRotation = yAxisRotation - 360;
        } else if (yAxisRotation < 0)
        {
            yAxisRotation = yAxisRotation + 360;
        }
        demandedCameraRotationY.set(yAxisRotation);

        double xAxisRotation = demandedCameraRotationX.get() - xangle;
        if (xAxisRotation > 89)
        {
            xAxisRotation = 89;
        } else if (xAxisRotation < 0)
        {
            xAxisRotation = 0;
        }
        demandedCameraRotationX.set(xAxisRotation);

        bedTranslateXform.setRotateY(yAxisRotation);
        bedTranslateXform.setRotateX(xAxisRotation);

    }

    public void rotateCameraAroundAxesTo(double xangle, double yangle)
    {
        double yAxisRotation = yangle;

        if (yAxisRotation > 360)
        {
            yAxisRotation = yAxisRotation - 360;
        } else if (yAxisRotation < 0)
        {
            yAxisRotation = yAxisRotation + 360;
        }
        demandedCameraRotationY.set(yAxisRotation);

        double xAxisRotation = xangle;
        if (xAxisRotation > 89)
        {
            xAxisRotation = 89;
        } else if (xAxisRotation < 0)
        {
            xAxisRotation = 0;
        }
        demandedCameraRotationX.set(xAxisRotation);

        bedTranslateXform.setRotateY(yAxisRotation);
        bedTranslateXform.setRotateX(xAxisRotation);

    }

    private final ChangeListener<Boolean> dragModeListener = new ChangeListener<Boolean>()
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            if (oldValue == false && newValue == true)
            {
//                translationDragPlane.setVisible(true);
                translationDragPlane.setMouseTransparent(false);
                models.setMouseTransparent(true);
            } else if (oldValue == true && newValue == false)
            {
                models.setMouseTransparent(false);
//                translationDragPlane.setVisible(false);
                translationDragPlane.setMouseTransparent(true);
            }
        }
    };

    private final EventHandler<MouseEvent> mouseEventHandler = event ->
    {
//        steno.info("Mouse event " + event);

        if (event.getEventType() == MouseEvent.MOUSE_PRESSED)
        {
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
            mousePosX = event.getSceneX();
            mousePosY = event.getSceneY();
            mouseOldX = event.getSceneX();
            mouseOldY = event.getSceneY();

            if (event.isPrimaryButtonDown())
            {
//                gizmoOverlayController.wasXHandleHit(event.getScreenX(), event.getScreenY());

                PickResult pickResult = event.getPickResult();
                Point3D pickedPoint = pickResult.getIntersectedPoint();

                Node intersectedNode = pickResult.getIntersectedNode();
                lastDragPosition = null;
                translationDragPlane.setTranslateX(pickedPoint.getX());
                translationDragPlane.setTranslateY(pickedPoint.getY());
                translationDragPlane.setTranslateZ(pickedPoint.getZ());

                setDragMode(true);
                if (intersectedNode != null && intersectedNode instanceof MeshView)
                {
                    Parent parent = intersectedNode.getParent();
                    if (!(parent instanceof ModelContainer))
                    {
                        parent = parent.getParent();
                    }
                    ModelContainer pickedModel = (ModelContainer) parent;

                    if (pickedModel.isSelected() == false)
                    {
                        if (event.isControlDown() == false)
                        {
                            deselectAllModels();
                        }
                        selectModel(pickedModel);
                    } else if (event.isControlDown())
                    {
                        deselectModel(pickedModel);
                    }
                } else if (intersectedNode == subScene)
                {
                    deselectAllModels();
                }
            }
            recalculateCentre();
        } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED)
        {
            double xDelta = event.getSceneX() - dragStartX;
            double yDelta = event.getSceneY() - dragStartY;

            double modifier = 1.0;
            double modifierFactor = 0.3;

            if (event.isControlDown())
            {
                modifier = 0.1;
            }
            if (event.isShiftDown())
            {
                modifier = 10.0;
            }

            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = event.getSceneX();
            mousePosY = event.getSceneY();
            mouseDeltaX = (mousePosX - mouseOldX); //*DELTA_MULTIPLIER;
            mouseDeltaY = (mousePosY - mouseOldY); //*DELTA_MULTIPLIER;

            double flip = -1.0;

            boolean alt = event.isAltDown();
            if (alt && event.isSecondaryButtonDown())
            {
                bedTranslateXform.setTx(bedTranslateXform.getTx() + mouseDeltaX * modifierFactor * modifier * 0.3);  // -
                bedTranslateXform.setTy(bedTranslateXform.getTy() + mouseDeltaY * modifierFactor * modifier * 0.3);  // -
            } else if (event.isSecondaryButtonDown())
            {
                rotateCameraAroundAxes(-mouseDeltaY * modifierFactor * modifier * 2.0, mouseDeltaX * modifierFactor * modifier * 2.0);
            } else if (dragMode.get() && event.isPrimaryButtonDown())
            {
                Node intersectedNode = event.getPickResult().getIntersectedNode();
//                    steno.info("Pick drag " + event.getPickResult().getIntersectedNode().getId());
                //Move the model!
                if (intersectedNode == translationDragPlane)
                {
//                    Point3D currentDragPosition = intersectedNode.localToScene(event.getPickResult().getIntersectedPoint());
                    Point3D currentDragPosition = event.getPickResult().getIntersectedPoint();
//                    steno.info("Pick drag " + event.getPickResult().getIntersectedNode().getId());
//                    steno.info("Drag from " + lastDragPosition + " to " + currentDragPosition);
                    if (lastDragPosition != null)
                    {
                        Point3D resultant = currentDragPosition.subtract(lastDragPosition);

//                        steno.info("Resultant " + resultant);
                        translateSelection(resultant.getX(), resultant.getZ());
                    }
//                    translationDragPlane.setTranslateX(currentDragPosition.getX());
//                    translationDragPlane.setTranslateZ(currentDragPosition.getZ());
                    lastDragPosition = currentDragPosition;
                } else if (intersectedNode == scaleDragPlane)
                {
//                    Point3D currentDragPosition = intersectedNode.localToScene(event.getPickResult().getIntersectedPoint());
////                steno.info("Pick drag " + me.getPickResult().getIntersectedNode().getId());
////                    steno.info("Drag from " + lastDragPosition + " to " + currentDragPosition);
//                    if (lastDragPosition != null)
//                    {
//                        Point3D resultant = currentDragPosition.subtract(lastDragPosition);
////                        steno.info("Resultant " + resultant);
//                        translateSelection(resultant.getX(), resultant.getZ());
//                    }
//                    scaleDragPlane.setTranslateX(currentDragPosition.getX());
//                    scaleDragPlane.setTranslateZ(currentDragPosition.getZ());
//                    lastDragPosition = currentDragPosition;
                }
            }
            recalculateCentre();

        } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED)
        {
            setDragMode(false);
            lastDragPosition = null;
            recalculateCentre();
        }
    };

    private final EventHandler<ScrollEvent> scrollEventHandler = event ->
    {
        if (event.getTouchCount() > 0)
        { // touch pad scroll
            bedTranslateXform.setTx(bedTranslateXform.getTx() - (0.01 * event.getDeltaX()));  // -
            bedTranslateXform.setTy(bedTranslateXform.getTy() + (0.01 * event.getDeltaY()));  // -
        } else
        {
            double z = bedTranslateXform.getTz() - (event.getDeltaY() * 0.2);
//            z = Math.max(z, 2000);
//            z = Math.min(z, -2000);
            cameraDistance.set(z);
            bedTranslateXform.setTz(z);
        }
        recalculateCentre();
    };
    private final EventHandler<ZoomEvent> zoomEventHandler = event ->
    {
        if (!Double.isNaN(event.getZoomFactor()) && event.getZoomFactor() > 0.8 && event.getZoomFactor() < 1.2)
        {
            double z = bedTranslateXform.getTz() / event.getZoomFactor();
//            z = Math.max(z, 2000);
//            z = Math.min(z, -2000);
            cameraDistance.set(z);
            bedTranslateXform.setTz(z);
            recalculateCentre();
        }
    };
    private final EventHandler<KeyEvent> keyEventHandler = event ->
    {
        /*
         if (!Double.isNaN(event.getZoomFactor()) && event.getZoomFactor() > 0.8 && event.getZoomFactor() < 1.2) {
         double z = cameraPosition.getZ()/event.getZoomFactor();
         z = Math.max(z,-1000);
         z = Math.min(z,0);
         cameraPosition.setZ(z);
         }
         
        
         System.out.println("KeyEvent ...");
         Timeline timeline = getTimeline();
         Duration currentTime;
         double CONTROL_MULTIPLIER = 0.1;
         double SHIFT_MULTIPLIER = 0.1;
         double ALT_MULTIPLIER = 0.5;
         //System.out.println("--> handleKeyboard>handle");

         // event.getEventType();
         switch (event.getCode())
         {
         case F:
         if (event.isControlDown())
         {
         //onButtonSave();
         }
         break;
         case O:
         if (event.isControlDown())
         {
         //onButtonLoad();
         }
         break;
         case Z:
         if (event.isShiftDown())
         {
         //                    rotationXform.ry.setAngle(0.0);
         //                    rotationXform.rx.setAngle(0.0);
         rotateCameraAroundXAxis.setAngle(0.0);
         camera.setTranslateZ(-300.0);
         }
         translateCamera.setX(0.0);
         translateCamera.setY(0.0);
         break;
            
         case SPACE:
         if (timelinePlaying) {
         timeline.pause();
         timelinePlaying = false;
         }
         else {
         timeline.play();
         timelinePlaying = true;
         }
         break;
             
         case UP:
         if (event.isControlDown() && event.isShiftDown())
         {
         translateCamera.setY(translateCamera.getY() - 10.0 * CONTROL_MULTIPLIER);
         } else if (event.isAltDown() && event.isShiftDown())
         {
         //                    rotationXform.rx.setAngle(rotationXform.rx.getAngle() - 10.0 * ALT_MULTIPLIER);
         } else if (event.isControlDown())
         {
         translateCamera.setY(translateCamera.getY() - 1.0 * CONTROL_MULTIPLIER);
         } else if (event.isAltDown())
         {
         //                    rotationXform.rx.setAngle(rotationXform.rx.getAngle() - 2.0 * ALT_MULTIPLIER);
         } else if (event.isShiftDown())
         {
         double z = camera.getTranslateZ();
         double newZ = z + 5.0 * SHIFT_MULTIPLIER;
         camera.setTranslateZ(newZ);
         }
         break;
         case DOWN:
         if (event.isControlDown() && event.isShiftDown())
         {
         translateCamera.setY(translateCamera.getY() + 10.0 * CONTROL_MULTIPLIER);
         } else if (event.isAltDown() && event.isShiftDown())
         {
         //                    rotationXform.rx.setAngle(rotationXform.rx.getAngle() + 10.0 * ALT_MULTIPLIER);
         } else if (event.isControlDown())
         {
         translateCamera.setY(translateCamera.getY() + 1.0 * CONTROL_MULTIPLIER);
         } else if (event.isAltDown())
         {
         //                    rotationXform.rx.setAngle(rotationXform.rx.getAngle() + 2.0 * ALT_MULTIPLIER);
         } else if (event.isShiftDown())
         {
         double z = camera.getTranslateZ();
         double newZ = z - 5.0 * SHIFT_MULTIPLIER;
         camera.setTranslateZ(newZ);
         }
         break;
         case RIGHT:
         if (event.isControlDown() && event.isShiftDown())
         {
         translateCamera.setX(translateCamera.getX() + 10.0 * CONTROL_MULTIPLIER);
         } else if (event.isAltDown() && event.isShiftDown())
         {
         //                    rotationXform.ry.setAngle(rotationXform.ry.getAngle() - 10.0 * ALT_MULTIPLIER);
         } else if (event.isControlDown())
         {
         translateCamera.setX(translateCamera.getX() + 1.0 * CONTROL_MULTIPLIER);
         } else if (event.isShiftDown())
         {
         currentTime = timeline.getCurrentTime();
         timeline.jumpTo(Frame.frame(Math.round(Frame.toFrame(currentTime) / 10.0) * 10 + 10));
         // timeline.jumpTo(Duration.seconds(currentTime.toSeconds() + ONE_FRAME));
         } else if (event.isAltDown())
         {
         //                    rotationXform.ry.setAngle(rotationXform.ry.getAngle() - 2.0 * ALT_MULTIPLIER);
         } else
         {
         currentTime = timeline.getCurrentTime();
         timeline.jumpTo(Frame.frame(Frame.toFrame(currentTime) + 1));
         // timeline.jumpTo(Duration.seconds(currentTime.toSeconds() + ONE_FRAME));
         }
         break;
         case LEFT:
         if (event.isControlDown() && event.isShiftDown())
         {
         translateCamera.setX(translateCamera.getX() - 10.0 * CONTROL_MULTIPLIER);
         } else if (event.isAltDown() && event.isShiftDown())
         {
         //                    rotationXform.ry.setAngle(rotationXform.ry.getAngle() + 10.0 * ALT_MULTIPLIER);  // -
         } else if (event.isControlDown())
         {
         translateCamera.setX(translateCamera.getX() - 1.0 * CONTROL_MULTIPLIER);
         } else if (event.isShiftDown())
         {
         currentTime = timeline.getCurrentTime();
         timeline.jumpTo(Frame.frame(Math.round(Frame.toFrame(currentTime) / 10.0) * 10 - 10));
         // timeline.jumpTo(Duration.seconds(currentTime.toSeconds() - ONE_FRAME));
         } else if (event.isAltDown())
         {
         //                    rotationXform.ry.setAngle(rotationXform.ry.getAngle() + 2.0 * ALT_MULTIPLIER);  // -
         } else
         {
         currentTime = timeline.getCurrentTime();
         timeline.jumpTo(Frame.frame(Frame.toFrame(currentTime) - 1));
         // timeline.jumpTo(Duration.seconds(currentTime.toSeconds() - ONE_FRAME));
         }
         break;
         }
         //System.out.println(cameraXform.getTranslateX() + ", " + cameraXform.getTranslateY() + ", " + cameraXform.getTranslateZ());
         */
    };


    /*
     * Snap to ground
     */
    private ObjectProperty<LayoutSubmode> layoutSubmode = new SimpleObjectProperty<>(LayoutSubmode.SELECT);

    private Project associatedProject = null;

    private ChangeListener<Number> sceneSizeChangeListener = new ChangeListener<Number>()
    {

        @Override
        public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
        {
            recalculateCentre();
        }
    };

    private ChangeListener<ApplicationMode> applicationModeListener = new ChangeListener<ApplicationMode>()
    {
        @Override
        public void changed(ObservableValue<? extends ApplicationMode> ov, ApplicationMode oldMode, ApplicationMode newMode)
        {
            if (oldMode != newMode)
            {
                switch (newMode)
                {
                    case SETTINGS:
                        subScene.removeEventHandler(MouseEvent.ANY, mouseEventHandler);
                        subScene.removeEventHandler(KeyEvent.ANY, keyEventHandler);
                        // subScene.addEventFilter(KeyEvent.ANY, keyEventHandler);
                        subScene.removeEventHandler(ZoomEvent.ANY, zoomEventHandler);
                        subScene.removeEventHandler(ScrollEvent.ANY, scrollEventHandler);
                        goToPreset(CameraPositionPreset.TOP);
                        break;
                    default:
                        goToPreset(CameraPositionPreset.FRONT);
                        subScene.addEventHandler(MouseEvent.ANY, mouseEventHandler);
                        subScene.addEventHandler(KeyEvent.ANY, keyEventHandler);
                        // subScene.addEventFilter(KeyEvent.ANY, keyEventHandler);
                        subScene.addEventHandler(ZoomEvent.ANY, zoomEventHandler);
                        subScene.addEventHandler(ScrollEvent.ANY, scrollEventHandler);
                        break;
                }
            }
        }
    };

    public ThreeDViewManager(Project project, ReadOnlyDoubleProperty widthProperty, ReadOnlyDoubleProperty heightProperty)
    {
        this.associatedProject = project;
        this.loadedModels = project.getLoadedModels();
        threeDControl = new SelectionHighlighter(selectionContainer, cameraDistance);

        this.widthPropertyToFollow = widthProperty;
        this.heightPropertyToFollow = heightProperty;

        AnchorPane.setBottomAnchor(root3D, 0.0);
        AnchorPane.setTopAnchor(root3D, 0.0);
        AnchorPane.setLeftAnchor(root3D, 0.0);
        AnchorPane.setRightAnchor(root3D, 0.0);
        root3D.setPickOnBounds(false);

//        camera.getTransforms().addAll(rotateCameraAroundYAxis, translateCamera);
        root3D.getChildren().add(camera);

        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);

        // Build SubScene
        subScene = new SubScene(root3D, widthProperty.getValue(), heightProperty.getValue(), true, SceneAntialiasing.BALANCED);
        this.subSceneProperty.set(subScene);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(camera);

        Group bed = buildBed();
        translationDragPlane.setId("DragPlane");
//        translationDragPlane.setVisible(false);
        translationDragPlane.setOpacity(0.0);
        translationDragPlane.setMouseTransparent(true);

        bedTranslateXform.getChildren().addAll(bed, models, translationDragPlane, threeDControl);
        root3D.getChildren().add(bedTranslateXform);

        bedXOffsetFromCameraZero = -printBedData.getPrintVolumeBounds().getWidth() / 2;
        bedZOffsetFromCameraZero = -printBedData.getPrintVolumeBounds().getDepth() / 2;

        bedTranslateXform.setTx(bedXOffsetFromCameraZero);
        bedTranslateXform.setTz(bedZOffsetFromCameraZero - cameraDistance.get());
        bedTranslateXform.setPivot(-bedXOffsetFromCameraZero, 0, -bedZOffsetFromCameraZero);

        rotateCameraAroundAxes(-30, 0);

        subScene.widthProperty().bind(widthPropertyToFollow);
        subScene.heightProperty().bind(heightPropertyToFollow);

        subScene.widthProperty().addListener(sceneSizeChangeListener);
        subScene.heightProperty().addListener(sceneSizeChangeListener);

        leapController = new Controller();
        leapMotionListener = new LeapMotionListener(this);
        leapController.addListener(leapMotionListener);
        if (loadedModels.isEmpty() == false)
        {
            for (ModelContainer model : loadedModels)
            {
                models.getChildren().add(model);
            }
        }

        applicationStatus.modeProperty().addListener(applicationModeListener);

        subScene.addEventHandler(MouseEvent.ANY, mouseEventHandler);
        subScene.addEventHandler(KeyEvent.ANY, keyEventHandler);
        // subScene.addEventFilter(KeyEvent.ANY, keyEventHandler);
        subScene.addEventHandler(ZoomEvent.ANY, zoomEventHandler);
        subScene.addEventHandler(ScrollEvent.ANY, scrollEventHandler);
//
//        subScene.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>()
//        {
//
//            @Override
//            public void handle(MouseEvent event)
//            {
//                steno.info("Filter caught event " + event);
//            }
//        });
//        layoutSubmodeProperty().addListener(new ChangeListener<LayoutSubmode>()
//        {
//            @Override
//            public void changed(ObservableValue<? extends LayoutSubmode> ov, LayoutSubmode t, LayoutSubmode t1)
//            {
//                if (t1 == LayoutSubmode.SNAP_TO_GROUND)
//                {
//                    subScene.setCursor(Cursor.HAND);
//                } else
//                {
//                    subScene.setCursor(Cursor.DEFAULT);
//                }
//            }
//        });

        dragMode.addListener(dragModeListener);
    }

    private void goToPreset(CameraPositionPreset preset)
    {
//        camera.setCentreOfRotation(preset.getPointToLookAt());
//        camera.rotateAndElevateCameraTo(preset.getAzimuth(), preset.getElevation());
//        camera.zoomCameraTo(preset.getDistance());
    }

    public void setTimeline(Timeline timeline)
    {
        this.timeline.setValue(timeline);
    }

    public Timeline getTimeline()
    {
        return this.timeline.getValue();
    }

    public SimpleObjectProperty timelineProperty()
    {
        return timeline;
    }

    private void recalculateCentre()
    {
//        steno.info("=================");
//        steno.info("From " + selectionContainer.getCentreX() + ":" + selectionContainer.getCentreY() + ":" + selectionContainer.getCentreZ());
        Point3D testPoint = new Point3D(selectionContainer.getCentreX(), selectionContainer.getCentreY(), selectionContainer.getCentreZ());
        Point3D correctedBed = bedTranslateXform.localToScene(testPoint);
//        steno.info("Corrected bed = " + correctedBed);
        Point3D localToScene = camera.sceneToLocal(correctedBed);
//        steno.info("Local to scene " + localToScene);
        Point2D localToSceneToScreen = camera.localToScreen(localToScene);
//        steno.info("Local to scene to screen " + localToSceneToScreen);
        selectionContainer.setScreenX(localToSceneToScreen.getX());
        selectionContainer.setScreenY(localToSceneToScreen.getY());

//        Point2D dragPoint = CameraHelper.pickNodeXYPlane(camera, translationDragPlane, localToSceneToScreen.getX(), localToSceneToScreen.getY());
//        steno.info("Drag point = " + dragPoint);
//        Point3D projectPoint = CameraHelper.pickProjectPlane(camera, localToSceneToScreen.getX(), localToSceneToScreen.getY());
//        steno.info("Project point = " + projectPoint);
//        PickRay pickRay = pickingHelper.getDeathRay(localToSceneToScreen.getX(), localToSceneToScreen.getY());
//        PickResult pickResult = pickingHelper.pick(pickRay);
//        steno.info("Pick result was " + pickResult);
//steno.info("View width = " + viewWidth);
//             PickRay pickRay = PickRay.computePerspectivePickRay(x, y, true,
//                camera.g,
//                getViewHeight(),
//                camera.getFieldOfView(),
//                camera.isVerticalFieldOfView(),
//                camera.getCameraTransform(),
//                camera.getNearClip(), camera.getFarClip(),
//                null);
//             
//             camera.impl_pickNode(pickRay, null);
//        Point3D screenToScene = camera.sceneToLocal(localToSceneToScreen.getX(), localToSceneToScreen.getY(), cameraDistance.get());
//        steno.info("Reverse 1 " + screenToScene);
//        
//        PickRay.computePerspectivePickRay(mousePosX, mousePosX, true, mousePosX, mousePosX, mouseOldX, true, null, mouseOldX, mouseOldX, null)
//        PickRay pickRay = new PickRay(, mousePosX, mousePosX, mouseOldX, mouseOldX)
    }

    private Group buildBed()
    {

        String bedOuterURL = CoreTest.class
                .getResource(ApplicationConfiguration.modelResourcePath + "bedOuter.obj").toExternalForm();
        String bedInnerURL = CoreTest.class.getResource(ApplicationConfiguration.modelResourcePath + "bedInner.obj").toExternalForm();

        PhongMaterial bedOuterMaterial = new PhongMaterial(Color.rgb(65, 65, 65));

        bedOuterMaterial.setSpecularColor(Color.WHITE);

        bedOuterMaterial.setSpecularPower(
                5.0);

        PhongMaterial bedInnerMaterial = new PhongMaterial(Color.GREY);

        bedInnerMaterial.setSpecularColor(Color.WHITE);

        bedInnerMaterial.setSpecularPower(
                .1);

        Group bedParts = new Group();

        ObjImporter bedOuterImporter = new ObjImporter();
        ModelLoadResult bedOuterLoadResult = bedOuterImporter.loadFile(null, bedOuterURL, null);

        bedParts.getChildren()
                .addAll(bedOuterLoadResult.getModelContainer().getMeshes());

        ObjImporter bedInnerImporter = new ObjImporter();
        ModelLoadResult bedInnerLoadResult = bedInnerImporter.loadFile(null, bedInnerURL, null);

        bedParts.getChildren()
                .addAll(bedInnerLoadResult.getModelContainer().getMeshes());

        final Image roboxLogoImage = new Image(CoreTest.class.getResource(ApplicationConfiguration.imageResourcePath + "roboxLogo.png").toExternalForm());
        final ImageView roboxLogoView = new ImageView();

        roboxLogoView.setImage(roboxLogoImage);

        final Xform roboxLogoTransformNode = new Xform();

        roboxLogoTransformNode.setRotateX(
                -90);

        final double logoSide_mm = 100;
        double logoScale = logoSide_mm / roboxLogoImage.getWidth();

        roboxLogoTransformNode.setScale(logoScale);

        roboxLogoTransformNode.setTz(logoSide_mm
                + PrintBed.getPrintVolumeCentre().getZ() / 2);
        roboxLogoTransformNode.setTy(
                -.25);
        roboxLogoTransformNode.setTx(PrintBed.getPrintVolumeCentre().getX() / 2);
        roboxLogoTransformNode.getChildren()
                .add(roboxLogoView);
        roboxLogoTransformNode.setId(
                "LogoImage");

        bedParts.getChildren()
                .add(roboxLogoTransformNode);
        bedParts.setMouseTransparent(
                true);

        return bedParts;
    }

    private void buildAxes()
    {
        final PhongMaterial redMaterial = new PhongMaterial();
        redMaterial.setDiffuseColor(Color.DARKRED);
        redMaterial.setSpecularColor(Color.RED);

        final PhongMaterial greenMaterial = new PhongMaterial();
        greenMaterial.setDiffuseColor(Color.DARKGREEN);
        greenMaterial.setSpecularColor(Color.GREEN);

        final Box xAxis = new Box(40, 1, 1);
        xAxis.setTranslateZ(-25);
        xAxis.setTranslateX(-20);

        final Box zAxis = new Box(1, 1, 40);
        zAxis.setTranslateX(-25);
        zAxis.setTranslateX(-20);
        zAxis.setTranslateZ(-20);

        xAxis.setMaterial(redMaterial);
        zAxis.setMaterial(greenMaterial);

        axisGroup.getChildren().addAll(xAxis, zAxis);
        root3D.getChildren().add(axisGroup);

//        autoScalingGroup.getChildren().addAll(axisGroup);
    }

    public void addModel(ModelContainer modelGroup)
    {
        if (modelGroup.getModelContentsType() == ModelContentsEnumeration.MESH)
        {
            modelGroup.centreObjectOnBed();
            models.getChildren().add(modelGroup);
            loadedModels.add(modelGroup);
            collideModels();
        } else
        {
            steno.info("About to add gcode to model");
            models.getChildren().add(modelGroup);
            steno.info("Done adding gcode");
        }
    }

    public void deleteSelectedModels()
    {
        ListIterator<ModelContainer> modelIterator = loadedModels.listIterator();
        ArrayList<ModelContainer> modelsToRemove = new ArrayList<>();
        while (modelIterator.hasNext())
        {
            ModelContainer model = modelIterator.next();

            if (model.isSelected())
            {
                modelsToRemove.add(model);
            }
        }

        for (ModelContainer chosenModel : modelsToRemove)
        {
            selectionContainer.removeSelectedModel(chosenModel);
            loadedModels.remove(chosenModel);
            models.getChildren().remove(chosenModel);
        }
        collideModels();
    }

    public void copySelectedModels()
    {
        ArrayList<ModelContainer> modelsToAdd = new ArrayList<>();

        ListIterator<ModelContainer> modelIterator = loadedModels.listIterator();
        while (modelIterator.hasNext())
        {
            ModelContainer model = modelIterator.next();

            if (model.isSelected())
            {
                ModelContainer modelCopy = model.clone();
                modelCopy.centreObjectOnBed();
                modelsToAdd.add(modelCopy);
            }
        }

        for (ModelContainer model : modelsToAdd)
        {
            addModel(model);
        }
        collideModels();
    }

    public void removeModel(ModelContainer modelGroup)
    {
        models.getChildren().remove(modelGroup);
        loadedModels.remove(modelGroup);

    }

    public void addGCodeParts(Group gCodeParts)
    {
        if (this.gcodeParts != null)
        {
            models.getChildren().remove(this.gcodeParts);
        }
        this.gcodeParts = gcodeParts;
        models.getChildren().add(gCodeParts);
    }

    public void shutdown()
    {
        subScene.widthProperty().removeListener(sceneSizeChangeListener);
        subScene.heightProperty().removeListener(sceneSizeChangeListener);
        leapController.removeListener(leapMotionListener);
        applicationStatus.modeProperty().removeListener(applicationModeListener);
        dragMode.removeListener(dragModeListener);
    }

//    public PolarCamera getCamera()
//    {
//        return camera;
//    }
    public void recalculateSelectionBounds(boolean addedOrRemoved)
    {

        if (selectionContainer.selectedModelsProperty().size() == 1)
        {
            ModelContainer model = selectionContainer.selectedModelsProperty().get(0);
            ModelBounds originalBounds = model.getOriginalModelBounds();
            double width = originalBounds.getWidth() * model.getScale();
            double height = originalBounds.getHeight() * model.getScale();
            double depth = originalBounds.getDepth() * model.getScale();

            selectionContainer.setWidth(width);
            selectionContainer.setHeight(height);
            selectionContainer.setDepth(depth);
            selectionContainer.setScale(model.getScale());
//            selectionContainer.setRotationX(model.getRotationX());
            selectionContainer.setRotationY(model.getRotateY());
//            selectionContainer.setRotationZ(model.getRotationZ());
//            selectionContainer.setMinX(model.getCentreX() - );

            Bounds parentBounds = model.getBoundsInParent();
//            
            double centreX = model.getCentreX();
            double centreY = model.getTranslateY();
            double centreZ = model.getCentreZ();
            selectionContainer.setCentreX(centreX);
            selectionContainer.setCentreY(centreY);
            selectionContainer.setCentreZ(centreZ);

            steno.info("Ctr X" + centreX + ":Y" + centreY + ":Z" + centreZ);
//            steno.info("Screen " + newScreen.getX() + ":Y" + newScreen.getY());
//            Point3D scene = root3D.localToScene(centreX, centreY, centreZ);
//            steno.info("Scene " + scene);
//            Point3D parent = root3D.localToParent(centreX, centreY, centreZ);
//            steno.info("Parent " + parent);
        } else
        {
            double minX = 999;
            double minY = 999;
            double minZ = 999;
            double maxX = 0;
            double maxY = 0;
            double maxZ = 0;

            for (ModelContainer model : loadedModels)
            {
                if (model.isSelected())
                {
                    Bounds modelBounds = model.getBoundsInParent();

                    minX = Math.min(modelBounds.getMinX(), minX);
                    minY = Math.min(modelBounds.getMinY(), minY);
                    minZ = Math.min(modelBounds.getMinZ(), minZ);

                    maxX = Math.max(modelBounds.getMaxX(), maxX);
                    maxY = Math.max(modelBounds.getMaxY(), maxY);
                    maxZ = Math.max(modelBounds.getMaxZ(), maxZ);
                }
            }

            double width = maxX - minX;
            double depth = maxZ - minZ;
            double height = maxY - minY;

            double centreX = minX + (width / 2);
            double centreY = maxY - (height / 2);
            double centreZ = minZ + (depth / 2);

            selectionContainer.setCentreX(centreX);
            selectionContainer.setCentreY(centreY);
            selectionContainer.setCentreZ(centreZ);
            selectionContainer.setWidth(width);
            selectionContainer.setDepth(depth);
            selectionContainer.setHeight(height);
            if (addedOrRemoved)
            {
                selectionContainer.setRotationY(0);
                selectionContainer.setScale(1);
            }
        }

        recalculateCentre();
    }

    public void selectModel(ModelContainer selectedNode)
    {
        if (selectedNode == null)
        {
            deselectAllModels();
        } else if (selectedNode.isSelected() == false)
        {
            selectedNode.setSelected(true);
            selectionContainer.addSelectedModel(selectedNode);
            recalculateSelectionBounds(true);
        }
    }

    public void deselectAllModels()
    {
        Iterator<ModelContainer> loadedModelIterator = loadedModels.iterator();
        while (loadedModelIterator.hasNext())
        {
            ModelContainer model = loadedModelIterator.next();

            deselectModel(model);
        }
        recalculateSelectionBounds(true);
    }

    public void translateSelection(double x, double z)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.translateBy(x, z);
            }
        }

        recalculateSelectionBounds(false);
        collideModels();
    }

    public void translateSelectionX(double x)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.translateX(x);
            }
        }

        recalculateSelectionBounds(false);
        collideModels();
    }

    public void translateSelectionZ(double z)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.translateZ(z);
            }
        }

        recalculateSelectionBounds(false);
        collideModels();
    }

    public void translateSelectionXTo(double x)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.translateXTo(x);
            }
        }

        recalculateSelectionBounds(false);
        collideModels();
    }

    public void translateSelectionZTo(double z)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.translateZTo(z);
            }
        }

        recalculateSelectionBounds(false);
        collideModels();
    }

    public void resizeSelectionWidth(double width)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.resizeWidth(width);
            }
        }

        recalculateSelectionBounds(false);
        collideModels();
    }

    public void resizeSelectionHeight(double height)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.resizeHeight(height);
            }
        }

        recalculateSelectionBounds(false);
        collideModels();
    }

    public void resizeSelectionDepth(double depth)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.resizeDepth(depth);
            }
        }

        recalculateSelectionBounds(false);
        collideModels();
    }

    public void scaleSelection(double newScale)
    {
        if (selectionContainer.selectedModelsProperty().size() == 1)
        {
            ModelContainer model = selectionContainer.selectedModelsProperty().get(0);
            model.scale(newScale);
        } else
        {
            for (ModelContainer model : loadedModels)
            {
                if (model.isSelected())
                {
                    model.scale(model.getScale() * newScale);
                }
            }
        }

        recalculateSelectionBounds(false);
        recalculateCentre();
        collideModels();
    }

    public void rotateSelection(double rotation)
    {
        if (selectionContainer.selectedModelsProperty().size() == 1)
        {

            selectionContainer.selectedModelsProperty().get(0).setRy(rotation);
            steno.info("Pivot is " + selectionContainer.selectedModelsProperty().get(0).getPivot());
            selectionContainer.setRotationY(rotation);
//            recalculateSelectionBounds(false);
        } else
        {
            for (ModelContainer model : loadedModels)
            {
                if (model.isSelected())
                {
                    model.deltaRotateAroundY(selectionContainer.getCentreX(), selectionContainer.getCentreY(), selectionContainer.getCentreZ(), rotation);
                }
            }
            selectionContainer.setRotationX(0);
        }
        recalculateSelectionBounds(false);
        collideModels();
    }

    public void changeModelHeight(double newHeight)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.resizeHeight(newHeight);
            }
        }
        recalculateSelectionBounds(false);
        collideModels();
    }

    public void deselectModel(ModelContainer pickedModel)
    {
        if (pickedModel.isSelected())
        {
            pickedModel.setSelected(false);
            selectionContainer.removeSelectedModel(pickedModel);
            recalculateSelectionBounds(false);
        }
    }

    public ObservableList<ModelContainer> getLoadedModels()
    {
        return loadedModels;
    }

    public SelectionContainer getSelectionContainer()
    {
        return selectionContainer;
    }

    private void collideModels()
    {
        boolean[] collidedModels = new boolean[loadedModels.size()];

        for (int printableNum = 0; printableNum < loadedModels.size(); printableNum++)
        {
            ModelContainer modelToCollide = loadedModels.get(printableNum);

            for (int secondaryPrintableNum = 0; secondaryPrintableNum < loadedModels.size(); secondaryPrintableNum++)
            {
                if (secondaryPrintableNum > printableNum)
                {
                    ModelContainer modelToCollideWith = loadedModels.get(secondaryPrintableNum);

                    if (modelToCollide.getBoundsInParent().intersects(modelToCollideWith.getBoundsInParent()))
                    {
                        collidedModels[printableNum] = true;
                        collidedModels[secondaryPrintableNum] = true;
//                        steno.info(modelToCollide.getId() + " collided with " + modelToCollideWith.getId());
                    }
                }
            }
        }

        for (int index = 0; index < collidedModels.length; index++)
        {
            loadedModels.get(index).setCollision(collidedModels[index]);
        }
    }

    public void deltaScaleSelection(double d)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.scale(d * model.getScale());
            }
        }
        recalculateSelectionBounds(false);
        collideModels();
    }

    public void activateSnapToGround()
    {
        layoutSubmode.set(LayoutSubmode.SNAP_TO_GROUND);
    }

    public ObjectProperty<LayoutSubmode> layoutSubmodeProperty()
    {
        return layoutSubmode;
    }

    public void activateGCodeVisualisationMode()
    {
        layoutSubmode.set(LayoutSubmode.GCODE_VISUALISATION);
    }

    public SelectionHighlighter getSelectionHighlighter()
    {
        return threeDControl;
    }

    public IntegerProperty screenCentreOfSelectionXProperty()
    {
        return screenCentreOfSelectionX;
    }

    public IntegerProperty screenCentreOfSelectionYProperty()
    {
        return screenCentreOfSelectionY;
    }

    public SubScene getSubScene()
    {
        return subScene;
    }

    public Group getRoot()
    {
        return root3D;
    }

    public DoubleProperty demandedCameraRotationYProperty()
    {
        return demandedCameraRotationY;
    }

    public DoubleProperty demandedCameraRotationXProperty()
    {
        return demandedCameraRotationX;
    }

    public void setDragMode(boolean value)
    {
        dragMode.set(value);
    }

    public boolean getDragMode()
    {
        return dragMode.get();
    }

    public BooleanProperty dragModeProperty()
    {
        return dragMode;
    }

    public void associateGizmoOverlayController(GizmoOverlayController controller)
    {
        this.gizmoOverlayController = controller;
    }

    public void checkit(double screenX, double screenY)
    {
        Point2D screenToLocal = camera.screenToLocal(screenX, screenY);
//        Point2D localToSceneToScreen = camera.localToScreen(localToScene);
        steno.info("screen to local " + screenToLocal);

        Point3D localToScene = camera.localToScene(screenToLocal.getX(), screenToLocal.getY(), 0);
        steno.info("Local to scene " + localToScene);

        Point3D correctedBed = bedTranslateXform.sceneToLocal(localToScene);
        steno.info("Corrected bed = " + correctedBed);

//        Point3D testPoint = new Point3D(selectionContainer.getCentreX(), selectionContainer.getCentreY(), selectionContainer.getCentreZ());
    }

    public void translateSelectionFromScreenCoords(Point2D translateStartPoint, Point2D screenCoords)
    {
        Point2D screenToLocal = camera.screenToLocal(translateStartPoint);
        steno.info("Screen to local " + screenToLocal);

//    final PickResultChooser result = new PickResultChooser();
//    PickRay pickRay = new PickRay(null, null, mouseOldX, mouseOldX)
//    subScene.getRoot().impl_pickNode(new PickRay(screenToLocal.getX(), screenToLocal.getY()), result);
//    Node nodeToSendEvent = result.getIntersectedNode();
//        
    }

//        public Point3D sceneToLocal3D(Node n, double sceneX, double sceneY) {
//        Scene scene = n.getScene();
//        if (scene == null) {
//            return null;
//        }
//
//        Point2D pt = new Point2D(screenX, screenY);
//        final SubScene subScene = NodeHelper.getSubScene(n);
//        if (subScene != null) {
//            pt = SceneUtils.sceneToSubScenePlane(subScene, pt);
//            if (pt == null) {
//                return null;
//            }
//        }
//
//        // compute pick ray
//        final Camera cam = subScene != null
//                ? SubSceneHelper.getEffectiveCamera(subScene)
//                : SceneHelper.getEffectiveCamera(scene);
//        
//        final PickRay pickRay = cam.computePickRay(pt.getX(), pt.getY(), null);
//
//        // convert it to node's local pickRay
//        final Affine3D localToSceneTx = new Affine3D();
//        n.getLocalToSceneTransform().impl_apply(localToSceneTx);
//        try {
//            Vec3d origin = pickRay.getOriginNoClone();
//            Vec3d direction = pickRay.getDirectionNoClone();
//            localToSceneTx.inverseTransform(origin, origin);
//            localToSceneTx.inverseDeltaTransform(direction, direction);
//        } catch (NoninvertibleTransformException e) {
//            return null;
//        }
//
//        // compute the intersection
//        final PickResultChooser result = new PickResultChooser();
//        impl_computeIntersects(pickRay, result);
//        if (result.getIntersectedNode() == n) {
//            return result.getIntersectedPoint();
//        }
//
//        // there is none, use point on projection plane instead
//        final Point3D ppIntersect = CameraAccess.getCameraAccess().pickProjectPlane(cam, pt.getX(), pt.getY());
//        return n.sceneToLocal(ppIntersect);
//    }
}
