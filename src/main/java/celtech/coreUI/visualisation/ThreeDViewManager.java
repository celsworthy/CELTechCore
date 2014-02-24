/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation;

import celtech.CoreTest;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.MachineType;
import celtech.configuration.PrintBed;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.visualisation.importers.ModelLoadResult;
import celtech.coreUI.visualisation.importers.obj.ObjImporter;
import celtech.coreUI.visualisation.modelDisplay.ModelBounds;
//import celtech.coreUI.LeapMotionListener;
import celtech.modelcontrol.ModelContainer;
import celtech.coreUI.visualisation.modelDisplay.SelectionHighlighter;
import celtech.coreUI.visualisation.shapes.SubdivisionMesh;
import celtech.modelcontrol.ModelContentsEnumeration;
import celtech.utils.Math.MathUtils;
//import com.leapmotion.leap.Controller;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableFloatArray;
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
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.TriangleMesh;
import javafx.util.Duration;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ThreeDViewManager
{

    private static final Stenographer steno = StenographerFactory.getStenographer(ThreeDViewManager.class.getName());

    private ObservableList<ModelContainer> loadedModels = null;
    private final ObjectProperty<Point2D> screenCentreOfSelection = new SimpleObjectProperty<>();
    private final SelectionContainer selectionContainer = new SelectionContainer();
    private final ApplicationStatus applicationStatus = ApplicationStatus.getInstance();

    private final PrintBed printBedData = PrintBed.getInstance();
    private final Group root3D = new Group();
    private final SimpleObjectProperty<SubScene> subSceneProperty = new SimpleObjectProperty<>();
    private final PolarCamera camera = new PolarCamera();

    private final PointLight pointLight1 = new PointLight(Color.WHITE);
    private final AmbientLight ambientLight = new AmbientLight(Color.WHITE);
    private final AutoScalingGroup autoScalingGroup = new AutoScalingGroup(2);

    final Group axisGroup = new Group();
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;
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
    private final Box dragPlane = new Box(dragPlaneHalfSize * 2, 0.1, dragPlaneHalfSize * 2);
    private SelectionHighlighter threeDControl = null;
    /*
     * Appearance
     */
    private boolean wireframe = false;
    private int subdivisionLevel = 0;
    private SubdivisionMesh.BoundaryMode boundaryMode = SubdivisionMesh.BoundaryMode.CREASE_EDGES;
    private SubdivisionMesh.MapBorderMode mapBorderMode = SubdivisionMesh.MapBorderMode.NOT_SMOOTH;
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
    private boolean inDragMode = false;


    /*
     * Leap Motion
     */
//    private Controller leapController = null;
//    private LeapMotionListener leapMotionListener = null;
    /*
    
     */
    private ReadOnlyDoubleProperty widthPropertyToFollow = null;
    private ReadOnlyDoubleProperty heightPropertyToFollow = null;

    private IntegerProperty screenCentreOfSelectionX = new SimpleIntegerProperty(0);
    private IntegerProperty screenCentreOfSelectionY = new SimpleIntegerProperty(0);

    /*
     * Snap to ground
     */
    private ObjectProperty<LayoutSubmode> layoutSubmode = new SimpleObjectProperty<>(LayoutSubmode.SELECT);

    public ThreeDViewManager(ObservableList<ModelContainer> loadedModels, ReadOnlyDoubleProperty widthProperty, ReadOnlyDoubleProperty heightProperty)
    {
        this.loadedModels = loadedModels;
        threeDControl = new SelectionHighlighter(selectionContainer, camera.cameraDistanceProperty());

        this.widthPropertyToFollow = widthProperty;
        this.heightPropertyToFollow = heightProperty;
//        this.getStyleClass().add("projectTab");

//        root3D.getStyleClass().add("threeDPane");
//        this.setC
        AnchorPane.setBottomAnchor(root3D, 0.0);
        AnchorPane.setTopAnchor(root3D, 0.0);
        AnchorPane.setLeftAnchor(root3D, 0.0);
        AnchorPane.setRightAnchor(root3D, 0.0);
        root3D.setPickOnBounds(false);
        root3D.getChildren().add(camera);
//        root3D.getStyleClass().add("roboxPanel");

        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.zoomCameraTo(400);
        camera.rotateAndElevateCameraTo(0, 45);
        root3D.getChildren().add(autoScalingGroup);

//        SessionManager sessionManager = SessionManager.getSessionManager();
//        sessionManager.bind(cameraLookXRotate.angleProperty(), "cameraLookXRotate");
//        sessionManager.bind(cameraLookZRotate.angleProperty(), "cameraLookZRotate");
//        sessionManager.bind(cameraPosition.xProperty(), "cameraPosition.x");
//        sessionManager.bind(cameraPosition.yProperty(), "cameraPosition.y");
//        sessionManager.bind(cameraPosition.zProperty(), "cameraPosition.z");
//        sessionManager.bind(cameraXRotate.angleProperty(), "cameraXRotate");
//        sessionManager.bind(cameraYRotate.angleProperty(), "cameraYRotate");
//        sessionManager.bind(camera.nearClipProperty(), "cameraNearClip");
//        sessionManager.bind(camera.farClipProperty(), "cameraFarClip");
        // Build SubScene
//        camera.repositionCamera();
        SubScene subScene = new SubScene(root3D, widthProperty.getValue(), heightProperty.getValue(), true, SceneAntialiasing.BALANCED);

        this.subSceneProperty.set(subScene);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(camera);

        buildBed();

        autoScalingGroup.getChildren().add(models);

        autoScalingGroup.getChildren().add(threeDControl);

        buildDragPlane();

//        buildAxes();
//        contentProperty()
//                .addListener(new ChangeListener<Node>()
//                        {
//                            @Override
//                            public void changed(ObservableValue<? extends Node> ov, Node oldContent, Node newContent
//                            )
//                            {
//                                autoScalingGroup.getChildren().remove(oldContent);
//                                autoScalingGroup.getChildren().add(newContent);
////                setWireFrame(newContent, wireframe);
//                                // TODO mesh is updated each time these are called even if no rendering needs to happen
//                                setSubdivisionLevel(newContent, subdivisionLevel);
//                                setBoundaryMode(newContent, boundaryMode);
//                                setMapBorderMode(newContent, mapBorderMode);
//                            }
//                }
//                );
        subScene.widthProperty().bind(widthPropertyToFollow);
        subScene.heightProperty().bind(heightPropertyToFollow);
        
        subScene.widthProperty().addListener(new ChangeListener<Number>()
        {

            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
steno.info("Scene width = " + t1.doubleValue());            }
        });
        
                subScene.heightProperty().addListener(new ChangeListener<Number>()
        {

            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
steno.info("Scene height = " + t1.doubleValue());            }
        });

//        leapController = new Controller();
//        leapMotionListener = new LeapMotionListener();
//        leapController.addListener(leapMotionListener);
        if (loadedModels.isEmpty() == false)
        {
            for (ModelContainer model : loadedModels)
            {
                models.getChildren().add(model);
            }
        }

        applicationStatus.modeProperty().addListener(new ChangeListener<ApplicationMode>()
        {

            @Override
            public void changed(ObservableValue<? extends ApplicationMode> ov, ApplicationMode oldMode, ApplicationMode newMode)
            {
                if (oldMode != newMode)
                {
                    switch (newMode)
                    {
                        case SETTINGS:
                            goToPreset(CameraPositionPreset.TOP);
                            unhandleGestures();
                            unHandleMouse();
//                            unHandleKeyboard();
                            break;
                        default:
                            goToPreset(CameraPositionPreset.FRONT);
                            handleGestures(getSubScene());
                            handleMouse(getSubScene());
//                            handleKeyboard(getSubScene());
                            break;
                    }
                }
            }
        }
        );

//        selectionContainer.scaleProperty().addListener(new ChangeListener<Number>()
//        {
//
//            @Override
//            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
//            {
//                scaleSelection(t1.doubleValue());
//            }
//        });
//
//        selectionContainer.rotationProperty().addListener(new ChangeListener<Number>()
//        {
//
//            @Override
//            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
//            {
//                rotateSelection(t1.doubleValue());
//            }
//        });
//
//        selectionContainer.heightProperty().addListener(new ChangeListener<Number>()
//        {
//
//            @Override
//            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
//            {
//                changeModelHeight(t1.doubleValue());
//            }
//        });
        handleMouse(subScene);
        handleGestures(subScene);
//        handleKeyboard(subScene);

        layoutSubmodeProperty().addListener(new ChangeListener<LayoutSubmode>()
        {
            @Override
            public void changed(ObservableValue<? extends LayoutSubmode> ov, LayoutSubmode t, LayoutSubmode t1)
            {
                if (t1 == LayoutSubmode.SNAP_TO_GROUND)
                {
                    getSubScene().setCursor(Cursor.HAND);
                } else
                {
                    getSubScene().setCursor(Cursor.DEFAULT);
                }
            }
        });

    }

    private void goToPreset(CameraPositionPreset preset)
    {
        camera.setCentreOfRotation(preset.getPointToLookAt());
        camera.rotateAndElevateCameraTo(preset.getAzimuth(), preset.getElevation());
        camera.zoomCameraTo(preset.getDistance());
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

    public void setSubScene(SubScene subScene)
    {
        this.subSceneProperty.setValue(subScene);
    }

    public SubScene getSubScene()
    {
        return this.subSceneProperty.getValue();
    }

    public SimpleObjectProperty subSceneProperty()
    {
        return subSceneProperty;
    }

    private void unHandleMouse()
    {
        getSubScene().setOnMousePressed(null);

        getSubScene().setOnMouseDragged(null);

        getSubScene().setOnMouseReleased(null);
    }

    private void handleMouse(SubScene scene)
    {
        scene.setOnMousePressed((MouseEvent me) ->
        {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();

            PickResult pickResult = me.getPickResult();

            Point3D pickedPoint = pickResult.getIntersectedPoint();

            steno.info("Picked at " + pickedPoint + " on " + pickResult.getIntersectedNode().getId());

//            displayManager.setScreenCentreOfSelection(new Point2D(me.getX(), me.getY()));
            Node intersectedNode = pickResult.getIntersectedNode();

            lastDragPosition = intersectedNode.localToScene(pickedPoint);
            dragMode(true);

            if (me.isPrimaryButtonDown())
            {
                if (layoutSubmode.get() == LayoutSubmode.SELECT)
                {

//            steno.info("Converts LtoP " + intersectedNode.localToParent(pickedPoint));
//            steno.info("Converts LtoS " + intersectedNode.localToScene(pickedPoint));
//            steno.info("Converts LtoScr " + intersectedNode.localToScreen(pickedPoint));
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
                            if (me.isControlDown() == false)
                            {
                                deselectAllModels();
                            }
                            selectModel(pickedModel);
                        } else if (me.isControlDown())
                        {
                            deselectModel(pickedModel);
                        }
                    } else
                    {
                        deselectAllModels();
                    }
                } else if (layoutSubmode.get() == LayoutSubmode.GCODE_VISUALISATION)
                {
                    lastDragPosition = intersectedNode.localToScene(pickedPoint);

                    dragMode(true);

                    if (intersectedNode != null && intersectedNode instanceof Shape3D)
                    {
                        String gcodeLineNumberString = intersectedNode.getId();
                        if (gcodeLineNumberString != null)
                        {
                            int gcodeLineNumber = Integer.valueOf(gcodeLineNumberString);
                            loadedModels.get(0).selectGCodeLine(gcodeLineNumber);
                        }
                    }
                } else
                {
                    steno.info("Snap: " + pickResult.toString());
                    if (intersectedNode instanceof MeshView)
                    {
                        MeshView meshView = (MeshView) intersectedNode;
                        TriangleMesh triMesh = (TriangleMesh) meshView.getMesh();

                        int faceNumber = pickResult.getIntersectedFace();

                        int baseFaceIndex = faceNumber * 6;

                        int v1PointIndex = triMesh.getFaces().get(baseFaceIndex);
                        int v2PointIndex = triMesh.getFaces().get(baseFaceIndex + 2);
                        int v3PointIndex = triMesh.getFaces().get(baseFaceIndex + 4);

                        ObservableFloatArray points = triMesh.getPoints();

                        Vector3D v1 = new Vector3D(points.get(v1PointIndex * 3), points.get((v1PointIndex * 3) + 1), points.get((v1PointIndex * 3) + 2));
                        Vector3D v2 = new Vector3D(points.get(v2PointIndex * 3), points.get((v2PointIndex * 3) + 1), points.get((v2PointIndex * 3) + 2));
                        Vector3D v3 = new Vector3D(points.get(v3PointIndex * 3), points.get((v3PointIndex * 3) + 1), points.get((v3PointIndex * 3) + 2));

                        float[] pointArray = triMesh.getPoints().toArray(null);
                        int[] faceArray = triMesh.getFaces().toArray(null);

//                    for (int i = 0; i < pointArray.length; i++)
//                    {
//                        steno.info("Point " + i + ":" + pointArray[i]);
//                    }
//
//                    for (int i = 0; i < faceArray.length; i++)
//                    {
//                        steno.info("Face " + i + ":" + faceArray[i]);
//                    }
                        Vector3D result1 = v2.subtract(v1);
                        Vector3D result2 = v3.subtract(v1);
                        Vector3D faceNormal = result1.crossProduct(result2);
                        Vector3D currentVectorNormalised = faceNormal.normalize();

                        Vector3D downvector = new Vector3D(0, 1, 0);

                        Rotation result = new Rotation(currentVectorNormalised, downvector);
                        double angles[] = result.getAngles(RotationOrder.XYZ);
                        steno.info("Angles were X:" + angles[0] * MathUtils.RAD_TO_DEG + " Y:" + angles[1] * MathUtils.RAD_TO_DEG + " Z:" + angles[2] * MathUtils.RAD_TO_DEG);

                        selectionContainer.selectedModelsProperty().get(0).rotateRadians(result);
                        recalculateSelectionBounds(false);

                        steno.info("For points " + v1 + ":" + v2 + ":" + v3 + " got normal " + currentVectorNormalised);
                        layoutSubmode.set(LayoutSubmode.SELECT);
                    } else
                    {
                        layoutSubmode.set(LayoutSubmode.SELECT);
                    }
                }
            }
            
            recalculateCentre();
        }
        );

        scene.setOnMouseDragged(
                (MouseEvent me) ->
                {
                    mouseOldX = mousePosX;
                    mouseOldY = mousePosY;
                    mousePosX = me.getSceneX();
                    mousePosY = me.getSceneY();
                    mouseDeltaX = (mousePosX - mouseOldX);
                    mouseDeltaY = (mousePosY - mouseOldY);

                    double modifier = 10;
                    double modifierFactor = 0.1;

                    if (me.isControlDown())
                    {
                        modifier = 0.1;
                    }
                    if (me.isShiftDown())
                    {
                        modifier = 10.0;
                    }

                    Node intersectedNode = me.getPickResult().getIntersectedNode();

//            steno.info("Picked " + intersectedNode.getId());
                    if (me.isMiddleButtonDown())
                    {
                        camera.rotateAndElevateCamera(-mouseDeltaX * modifier * modifierFactor, mouseDeltaY * modifier * modifierFactor);
                    } else if (me.isSecondaryButtonDown())
                    {
                        camera.translateCamera(-mouseDeltaX * modifier * modifierFactor, -mouseDeltaY * modifier * modifierFactor);
                    } else if (selectionContainer.selectedModelsProperty().size() > 0 && me.isPrimaryButtonDown())
                    {
                        //Move the model!
                        if (intersectedNode == dragPlane)
                        {
                            Point3D currentDragPosition = intersectedNode.localToScene(me.getPickResult().getIntersectedPoint());
//                steno.info("Pick drag " + me.getPickResult().getIntersectedNode().getId());
//                    steno.info("Drag from " + lastDragPosition + " to " + currentDragPosition);
                            if (lastDragPosition != null)
                            {
                                Point3D resultant = currentDragPosition.subtract(lastDragPosition);
//                        steno.info("Resultant " + resultant);
                                translateSelection(resultant.getX(), resultant.getZ());
                            }
                            dragPlane.setTranslateX(currentDragPosition.getX());
                            dragPlane.setTranslateZ(currentDragPosition.getZ());
                            lastDragPosition = currentDragPosition;
                        }
                    }
                    recalculateCentre();
                }
        );

        scene.setOnMouseReleased(
                (MouseEvent me) ->
                {
                    dragMode(false);
                    lastDragPosition = null;
                }
        );
    }

    private void recalculateCentre()
    {

        steno.info("orig " + selectionContainer.getCentreX() + ":" + selectionContainer.getCentreY() + ":" + selectionContainer.getCentreZ());
        Point3D zeroPosP = root3D.localToScene(selectionContainer.getCentreX(), 0, selectionContainer.getCentreZ());
        steno.info("S " + zeroPosP.toString());
//        Point2D scene = root3D.localToScreen(zeroPosP);
//        steno.info("X " + scene.toString());
//        Point3D zeroPosSc = camera.localToScene(selectionContainer.getCentreX(), 0, selectionContainer.getCentreZ());
//        steno.info("Sc " + zeroPosSc.toString());
//        Point2D zeroPosS = camera.localToScreen(selectionContainer.getCentreX(), 0, selectionContainer.getCentreZ());
//        steno.info("S " + zeroPosS.toString());
        
//        int screenX = (int)zeroPos.getX();
//        int screenY = (int)zeroPos.getY();
//        
//        steno.info("orig " + selectionContainer.getCentreX() + ":" + selectionContainer.getCentreZ());
//        steno.info("centre " + zeroPos.getX() + ":" + zeroPos.getY());
//        steno.info("Got " + screenX + ":" + screenY);
////        screenCentreOfSelectionX.set(screenX);
//        screenCentreOfSelectionY.set(screenY);
    }

    private void handleKeyboard(SubScene scene)
    {
        final boolean moveCamera = true;
        scene.setOnKeyPressed(new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent event)
            {
                Duration currentTime;
                switch (event.getCode())
                {
                    case Z:
                        steno.info("Z pressed");
                        if (event.isShiftDown())
                        {
//     cameraXform.ry.setAngle(0.0);
//     cameraXform.rx.setAngle(0.0);
//     camera.setTranslateZ(-300.0);
                        }
//     cameraXform.t.setX(0.0);
//     cameraXform.t.setY(0.0);
                        break;
                    case X:
//     if (event.isControlDown())
//     {
//     if (axisGroup.isVisible())
//     {
//     axisGroup.setVisible(false);
//     } else
//     {
//     axisGroup.setVisible(true);
//     }
//     }
                        break;
                    case UP:
//     if (event.isControlDown() && event.isShiftDown())
//     {
//     cameraXform.t.setY(cameraXform.t.getY() - 10.0 * CONTROL_MULTIPLIER);
//     } else if (event.isAltDown() && event.isShiftDown())
//     {
//     cameraXform.rx.setAngle(cameraXform.rx.getAngle() - 10.0 * ALT_MULTIPLIER);
//     } else if (event.isControlDown())
//     {
//     cameraXform.t.setY(cameraXform.t.getY() - 1.0 * CONTROL_MULTIPLIER);
//     } else if (event.isAltDown())
//     {
//     cameraXform.rx.setAngle(cameraXform.rx.getAngle() - 2.0 * ALT_MULTIPLIER);
//     } else if (event.isShiftDown())
//     {
//     double z = camera.getTranslateZ();
//     double newZ = z + 5.0 * SHIFT_MULTIPLIER;
//     camera.setTranslateZ(newZ);
//     }
                        break;
                    case DOWN:
//     if (event.isControlDown() && event.isShiftDown())
//     {
//     cameraXform.t.setY(cameraXform.t.getY() + 10.0 * CONTROL_MULTIPLIER);
//     } else if (event.isAltDown() && event.isShiftDown())
//     {
//     cameraXform.rx.setAngle(cameraXform.rx.getAngle() + 10.0 * ALT_MULTIPLIER);
//     } else if (event.isControlDown())
//     {
//     cameraXform.t.setY(cameraXform.t.getY() + 1.0 * CONTROL_MULTIPLIER);
//     } else if (event.isAltDown())
//     {
//     cameraXform.rx.setAngle(cameraXform.rx.getAngle() + 2.0 * ALT_MULTIPLIER);
//     } else if (event.isShiftDown())
//     {
//     double z = camera.getTranslateZ();
//     double newZ = z - 5.0 * SHIFT_MULTIPLIER;
//     camera.setTranslateZ(newZ);
//     }
                        break;
                    case RIGHT:
//     if (event.isControlDown() && event.isShiftDown())
//     {
//     cameraXform.t.setX(cameraXform.t.getX() + 10.0 * CONTROL_MULTIPLIER);
//     } else if (event.isAltDown() && event.isShiftDown())
//     {
//     cameraXform.ry.setAngle(cameraXform.ry.getAngle() - 10.0 * ALT_MULTIPLIER);
//     } else if (event.isControlDown())
//     {
//     cameraXform.t.setX(cameraXform.t.getX() + 1.0 * CONTROL_MULTIPLIER);
//     } else if (event.isAltDown())
//     {
//     cameraXform.ry.setAngle(cameraXform.ry.getAngle() - 2.0 * ALT_MULTIPLIER);
//     }
//     break;
                    case LEFT:
//     if (event.isControlDown() && event.isShiftDown())
//     {
//     cameraXform.t.setX(cameraXform.t.getX() - 10.0 * CONTROL_MULTIPLIER);
//     } else if (event.isAltDown() && event.isShiftDown())
//     {
//     cameraXform.ry.setAngle(cameraXform.ry.getAngle() + 10.0 * ALT_MULTIPLIER);  // -
//     } else if (event.isControlDown())
//     {
//     cameraXform.t.setX(cameraXform.t.getX() - 1.0 * CONTROL_MULTIPLIER);
//     } else if (event.isAltDown())
//     {
//     cameraXform.ry.setAngle(cameraXform.ry.getAngle() + 2.0 * ALT_MULTIPLIER);  // -
//     }
                        break;
                }
            }
        });
    }

    private void unHandleKeyboard()
    {
        getSubScene().setOnKeyPressed(null);
    }

    private void buildBed()
    {

        String bedOuterURL = CoreTest.class
                .getResource(ApplicationConfiguration.modelResourcePath + "bedOuter.obj").toExternalForm();
        String bedInnerURL = CoreTest.class.getResource(ApplicationConfiguration.modelResourcePath + "bedInner.obj").toExternalForm();

        PhongMaterial bedOuterMaterial = new PhongMaterial(Color.BLACK);

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
                -.1);
        roboxLogoTransformNode.setTx(PrintBed.getPrintVolumeCentre().getX() / 2);
        roboxLogoTransformNode.getChildren()
                .add(roboxLogoView);
        roboxLogoTransformNode.setId(
                "LogoImage");

        bedParts.getChildren()
                .add(roboxLogoTransformNode);
        bedParts.setMouseTransparent(
                true);

        autoScalingGroup.getChildren()
                .add(bedParts);

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
        autoScalingGroup.getChildren().addAll(axisGroup);
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
        while (modelIterator.hasNext())
        {
            ModelContainer model = modelIterator.next();

            if (model.isSelected())
            {
                deselectModel(model);
                models.getChildren().remove(model);
                modelIterator.remove();
            }
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

    private void unhandleGestures()
    {
        getSubScene().setOnRotate(null);
        getSubScene().setOnZoom(null);
        getSubScene().setOnScroll(null);
    }

    private void handleGestures(SubScene scene)
    {
        scene.setOnRotate((RotateEvent event) ->
        {
            double modifier = 1.0;
            double modifierFactor = 0.5;

            if (event.isControlDown())
            {
                modifier = 0.1;
            }
            if (event.isShiftDown())
            {
                modifier = 2.0;
            }
            camera.rotateAndElevateCamera(event.getAngle() * modifierFactor * modifier, 0);
        });

        scene.setOnZoom((ZoomEvent event) ->
        {
            double modifier = 1.0;

            if (event.isControlDown())
            {
                modifier = 0.1;
            }
            if (event.isShiftDown())
            {
                modifier = 2.0;
            }
            camera.zoomCamera(event.getZoomFactor() * modifier);
        });

        scene.setOnScroll((ScrollEvent event) ->
        {
            double modifier = 1.0;
            double modifierFactor = 0.1;

            if (event.isControlDown())
            {
                modifier = 0.1;
            }
            if (event.isShiftDown())
            {
                modifier = 2.0;
            }

            if (ApplicationConfiguration.getMachineType() == MachineType.WINDOWS)
            {
                camera.alterZoom(event.getDeltaY() * modifier * modifierFactor);
            } else
            {
                camera.translateCamera(-event.getDeltaX() * modifierFactor * modifier, -event.getDeltaY() * modifierFactor * modifier);
            }

        });
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
//        controller.removeListener(leapMotionListener);
    }

    private void buildDragPlane()
    {
        dragPlane.setId("DragPlane");

        dragPlane.setVisible(false);
        dragPlane.setOpacity(0.0);
        dragPlane.setMouseTransparent(true);

        root3D.getChildren().add(dragPlane);
    }

    private void dragMode(boolean on)
    {
        if (on && !inDragMode)
        {
            dragPlane.setVisible(true);
            dragPlane.setTranslateX(lastDragPosition.getX());
            dragPlane.setTranslateY(lastDragPosition.getY());
            dragPlane.setTranslateZ(lastDragPosition.getZ());
            dragPlane.setMouseTransparent(false);
            models.setMouseTransparent(true);

            inDragMode = true;
        } else if (!on && inDragMode)
        {
            models.setMouseTransparent(false);
            dragPlane.setVisible(false);
            dragPlane.setMouseTransparent(true);

            inDragMode = false;
        }
    }

    public PolarCamera getCamera()
    {
        return camera;
    }

    private void recalculateSelectionBounds(boolean addedOrRemoved)
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
            selectionContainer.setRotationY(model.getRotationY());
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
            selectionContainer.selectedModelsProperty().get(0).scale(newScale);
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
        collideModels();
    }

    public void rotateSelection(double rotation)
    {
        if (selectionContainer.selectedModelsProperty().size() == 1)
        {
            selectionContainer.selectedModelsProperty().get(0).setRotationY(rotation);
//            steno.info(selectionContainer.selectedModelsProperty().get(0).toString());
            selectionContainer.setRotationY(rotation);
            recalculateSelectionBounds(false);
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

    public final void setScreenCentreOfSelection(Point2D value)
    {
        screenCentreOfSelection.set(value);
    }

    public Point2D getScreenCentreOfSelection()
    {
        return screenCentreOfSelection.get();
    }

    public ObjectProperty<Point2D> screenCentreOfSelectionProperty()
    {
        return screenCentreOfSelection;
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
}
