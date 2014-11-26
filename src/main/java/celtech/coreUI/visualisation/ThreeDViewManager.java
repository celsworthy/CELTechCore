package celtech.coreUI.visualisation;

import celtech.CoreTest;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintBed;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.controllers.GizmoOverlayController;
import celtech.coreUI.visualisation.importers.ModelLoadResult;
import celtech.coreUI.visualisation.importers.obj.ObjImporter;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelContentsEnumeration;
import javafx.animation.AnimationTimer;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

    private static final Stenographer steno = StenographerFactory.getStenographer(
        ThreeDViewManager.class.getName());

    private ObservableList<ModelContainer> loadedModels = null;
    private final ApplicationStatus applicationStatus = ApplicationStatus.getInstance();

    private final PrintBed printBedData = PrintBed.getInstance();
    private final Group root3D = new Group();
    private SubScene subScene = null;
    private final SimpleObjectProperty<SubScene> subSceneProperty = new SimpleObjectProperty<>();

//    private final PointLight pointLight1 = new PointLight(Color.WHITE);
//    private final AmbientLight ambientLight = new AmbientLight(Color.WHITE);
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
    private final Box translationDragPlane = new Box(dragPlaneHalfSize * 2, 0.1, dragPlaneHalfSize
                                                     * 2);
    private final Box scaleDragPlane = new Box(dragPlaneHalfSize * 2, dragPlaneHalfSize * 2, 0.1);
//    private GizmoOverlayController gizmoOverlayController = null;
    /*
     * 
     */
    private Group gcodeParts = null;

    private Group models = new Group();
    /*
     * Selection stuff
     */
    private ObjectProperty<DragMode> dragMode = new SimpleObjectProperty(DragMode.IDLE);

    private ReadOnlyDoubleProperty widthPropertyToFollow = null;
    private ReadOnlyDoubleProperty heightPropertyToFollow = null;

    private final IntegerProperty screenCentreOfSelectionX = new SimpleIntegerProperty(0);
    private final IntegerProperty screenCentreOfSelectionY = new SimpleIntegerProperty(0);

    /*
     * ALT stuff
     */
    private final Xform bedTranslateXform = new Xform(Xform.RotateOrder.YXZ, "BedXForm");
    private Group bed;
    private final PerspectiveCamera camera = new PerspectiveCamera(true);

//    private final Rotate rotateCameraAroundXAxis = new Rotate(0, MathUtils.xAxis);
//    private final Rotate rotateCameraAroundYAxis = new Rotate(0, MathUtils.yAxis);
//    private final Rotate rotateCameraAroundZAxis = new Rotate(0, MathUtils.zAxis);
//    private final Translate translateCamera = new Translate();
    private final static double initialCameraDistance = -350;
    private final DoubleProperty cameraDistance = new SimpleDoubleProperty(initialCameraDistance);
    private final DoubleProperty demandedCameraRotationX = new SimpleDoubleProperty(0);
    private final DoubleProperty demandedCameraRotationY = new SimpleDoubleProperty(0);

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    private final double bedXOffsetFromCameraZero;
    private final double bedZOffsetFromCameraZero;

    private final Point3D centreCoordsScene = null;

    private SelectedModelContainers selectedModelContainers = null;

//    private final double settingsAnimationYAngle = 30;
//    private final double settingsAnimationXAngle = 0;
    private long lastAnimationTrigger = 0;

    private double gizmoStartingRotationAngle = 0;
    private double gizmoRotationOffset = 0;
    private boolean gizmoRotationStarted = false;

    private final AnimationTimer settingsScreenAnimationTimer = new AnimationTimer()
    {
        @Override
        public void handle(long now)
        {
            long difference = now - lastAnimationTrigger;
            if (difference > 50000000)
            {
                rotateCameraAroundAxes(0, 0.1);
                lastAnimationTrigger = now;
            }
        }
    };

    /**
     *
     * @param xangle
     * @param yangle
     */
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

    /**
     *
     * @param xangle
     * @param yangle
     */
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

    private final ChangeListener<DragMode> dragModeListener = new ChangeListener<DragMode>()
    {
        @Override
        public void changed(ObservableValue<? extends DragMode> observable, DragMode oldValue,
            DragMode newValue)
        {
            switch (newValue)
            {
                case IDLE:
                    models.setMouseTransparent(false);
                    translationDragPlane.setMouseTransparent(true);
                    scaleDragPlane.setMouseTransparent(true);
                    break;
                case TRANSLATING:
                case X_CONSTRAINED_TRANSLATE:
                case Z_CONSTRAINED_TRANSLATE:
                case ROTATE:
                    translationDragPlane.setMouseTransparent(false);
                    scaleDragPlane.setMouseTransparent(true);
                    models.setMouseTransparent(true);
                    break;
                case SCALING:
                    scaleDragPlane.setMouseTransparent(false);
                    translationDragPlane.setMouseTransparent(true);
                    models.setMouseTransparent(true);
                    break;
            }
        }
    };

    private void handleMousePressedEvent(MouseEvent event)
    {
        mousePosX = event.getSceneX();
        mousePosY = event.getSceneY();
        mouseOldX = event.getSceneX();
        mouseOldY = event.getSceneY();

        if (event.isPrimaryButtonDown())
        {

            PickResult pickResult = event.getPickResult();
            Point3D pickedPoint = pickResult.getIntersectedPoint();

            Node intersectedNode = pickResult.getIntersectedNode();

            if (layoutSubmode.get().equals(LayoutSubmode.SNAP_TO_GROUND))
            {
                int faceNumber = pickResult.getIntersectedFace();
                snapToGround(intersectedNode, faceNumber);
                return;
            }

            Point3D pickedScenePoint = intersectedNode.localToScene(pickedPoint);
            Point3D pickedBedTranslateXformPoint = bedTranslateXform.sceneToLocal(pickedScenePoint);

            translationDragPlane.setTranslateY(pickedBedTranslateXformPoint.getY());
            Point3D pickedDragPlanePoint = translationDragPlane.sceneToLocal(pickedScenePoint);
            lastDragPosition = pickedDragPlanePoint;

            Point3D bedXToS = bedTranslateXform.localToParent(pickedPoint);
            scaleDragPlane.setTranslateX(bedXToS.getX());
            scaleDragPlane.setTranslateY(bedXToS.getY());
            scaleDragPlane.setTranslateZ(pickedPoint.getZ());

            setDragMode(DragMode.TRANSLATING);

            if (intersectedNode != null)
            {
                if (intersectedNode instanceof MeshView)
                {
                    Parent parent = intersectedNode.getParent();
                    if (!(parent instanceof ModelContainer))
                    {
                        parent = parent.getParent();
                    }

                    ModelContainer pickedModel = (ModelContainer) parent;

                    if (pickedModel.isSelected() == false)
                    {
                        boolean multiSelect = event.isShortcutDown();
                        selectModel(pickedModel, multiSelect);
                    } else
                    {
                        boolean multiSelect = event.isShortcutDown();
                        if (multiSelect)
                        {
                            deselectModel(pickedModel);
                        }
                    }
                } else if (true) //intersectedNode == subScene)
                {
                    selectedModelContainers.deselectAllModels();
                }

            }
        }
    }

    private void handleMouseDragEvent(MouseEvent event)
    {

        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = event.getSceneX();
        mousePosY = event.getSceneY();
        mouseDeltaX = (mousePosX - mouseOldX); //*DELTA_MULTIPLIER;
        mouseDeltaY = (mousePosY - mouseOldY); //*DELTA_MULTIPLIER;

        boolean shortcut = event.isShortcutDown();
        if (shortcut && event.isSecondaryButtonDown())
        {
            bedTranslateXform.setTx(bedTranslateXform.getTx() + mouseDeltaX * 0.3);  // -
            bedTranslateXform.setTy(bedTranslateXform.getTy() + mouseDeltaY * 0.3);  // -
        } else if (event.isSecondaryButtonDown())
        {
            rotateCameraAroundAxes(-mouseDeltaY * 2.0, mouseDeltaX * 2.0);
        } else if (dragMode.get() == DragMode.TRANSLATING && event.isPrimaryButtonDown())
        {
            Node intersectedNode = event.getPickResult().getIntersectedNode();
            //Move the model!
            if (intersectedNode == translationDragPlane)
            {
                Point3D pickedPoint = event.getPickResult().getIntersectedPoint();
                Point3D pickedScenePoint = intersectedNode.localToScene(pickedPoint);
                Point3D pickedDragPlanePoint = translationDragPlane.sceneToLocal(pickedScenePoint);
                if (lastDragPosition != null)
                {
                    Point3D resultant = pickedDragPlanePoint.subtract(lastDragPosition);
                    translateSelection(resultant.getX(), resultant.getZ());
                }
                lastDragPosition = pickedDragPlanePoint;
            } else
            {
                steno.error(
                    "In translation drag mode but intersected with something other than translation drag plane");
            }
        } else if (dragMode.get() == DragMode.SCALING && event.isPrimaryButtonDown())
        {
            Node intersectedNode = event.getPickResult().getIntersectedNode();
            //Move the model!
            if (intersectedNode != scaleDragPlane)
            {
                steno.error(
                    "In scale drag mode but intersected with something other than scale drag plane");
            }
        }
    }

    private final EventHandler<MouseEvent> mouseEventHandler = event ->
    {

        if (event.getEventType() == MouseEvent.MOUSE_PRESSED)
        {
            handleMousePressedEvent(event);

        } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED && dragMode.get()
            != DragMode.SCALING)
        {
            handleMouseDragEvent(event);

        } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED)
        {
            setDragMode(DragMode.IDLE);
            lastDragPosition = null;
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
            cameraDistance.set(z);
            bedTranslateXform.setTz(z);
        }
    };
    private final EventHandler<ZoomEvent> zoomEventHandler = event ->
    {
        if (!Double.isNaN(event.getZoomFactor()) && event.getZoomFactor() > 0.8
            && event.getZoomFactor() < 1.2)
        {
            double z = bedTranslateXform.getTz() / event.getZoomFactor();
            cameraDistance.set(z);
            bedTranslateXform.setTz(z);
        }
    };

    /*
     * Snap to ground
     */
    private ObjectProperty<LayoutSubmode> layoutSubmode = new SimpleObjectProperty<>(
        LayoutSubmode.SELECT);

    private ChangeListener<ApplicationMode> applicationModeListener = new ChangeListener<ApplicationMode>()
    {
        @Override
        public void changed(ObservableValue<? extends ApplicationMode> ov, ApplicationMode oldMode,
            ApplicationMode newMode)
        {
            if (oldMode != newMode)
            {
                switch (newMode)
                {
                    case SETTINGS:
                        subScene.removeEventHandler(MouseEvent.ANY, mouseEventHandler);
                        subScene.removeEventHandler(ZoomEvent.ANY, zoomEventHandler);
                        subScene.removeEventHandler(ScrollEvent.ANY, scrollEventHandler);
                        goToPreset(CameraPositionPreset.TOP);
                        break;
                    default:
                        goToPreset(CameraPositionPreset.FRONT);
                        subScene.addEventHandler(MouseEvent.ANY, mouseEventHandler);
                        subScene.addEventHandler(ZoomEvent.ANY, zoomEventHandler);
                        subScene.addEventHandler(ScrollEvent.ANY, scrollEventHandler);
                        break;
                }
            }
        }
    };

    /**
     *
     * @param loadedModels
     * @param widthProperty
     * @param heightProperty
     */
    public ThreeDViewManager(ObservableList<ModelContainer> loadedModels,
        ReadOnlyDoubleProperty widthProperty, ReadOnlyDoubleProperty heightProperty)
    {
        this.loadedModels = loadedModels;
        selectedModelContainers = new SelectedModelContainers();

        this.widthPropertyToFollow = widthProperty;
        this.heightPropertyToFollow = heightProperty;

        root3D.setId("Root");
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
        subScene = new SubScene(root3D, widthProperty.getValue(), heightProperty.getValue(), true,
                                SceneAntialiasing.BALANCED);
        this.subSceneProperty.set(subScene);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(camera);

        bed = buildBed();
        translationDragPlane.setId("DragPlane");
//        translationDragPlane.setVisible(true);
        translationDragPlane.setOpacity(0.0);
        translationDragPlane.setMouseTransparent(true);
        translationDragPlane.setTranslateX(PrintBed.getPrintVolumeCentre().getX());
        translationDragPlane.setTranslateZ(PrintBed.getPrintVolumeCentre().getZ());

        scaleDragPlane.setId("ScaleDragPlane");
//        translationDragPlane.setVisible(true);
        scaleDragPlane.setOpacity(0.0);
        scaleDragPlane.setMouseTransparent(true);

        bedTranslateXform.getChildren().addAll(bed, models, translationDragPlane, scaleDragPlane);
        root3D.getChildren().add(bedTranslateXform);

        bedXOffsetFromCameraZero = -printBedData.getPrintVolumeBounds().getWidth() / 2;
        bedZOffsetFromCameraZero = -printBedData.getPrintVolumeBounds().getDepth() / 2;

        bedTranslateXform.setTx(bedXOffsetFromCameraZero);
        bedTranslateXform.setTz(bedZOffsetFromCameraZero - cameraDistance.get());
        bedTranslateXform.setPivot(-bedXOffsetFromCameraZero, 0, -bedZOffsetFromCameraZero);

        rotateCameraAroundAxes(-30, 0);

        subScene.widthProperty().bind(widthPropertyToFollow);
        subScene.heightProperty().bind(heightPropertyToFollow);

        for (ModelContainer model : loadedModels)
        {
            models.getChildren().add(model);
        }

        applicationStatus.modeProperty().addListener(applicationModeListener);

        subScene.addEventHandler(MouseEvent.ANY, mouseEventHandler);
        subScene.addEventHandler(ZoomEvent.ANY, zoomEventHandler);
        subScene.addEventHandler(ScrollEvent.ANY, scrollEventHandler);

        layoutSubmodeProperty().addListener(new ChangeListener<LayoutSubmode>()
        {
            @Override
            public void changed(ObservableValue<? extends LayoutSubmode> ov, LayoutSubmode t,
                LayoutSubmode t1)
            {
                if (t1 == LayoutSubmode.SNAP_TO_GROUND)
                {
                    subScene.setCursor(Cursor.HAND);
                } else
                {
                    subScene.setCursor(Cursor.DEFAULT);
                }
            }
        });

        dragMode.addListener(dragModeListener);
    }

    private void goToPreset(CameraPositionPreset preset)
    {
//        camera.setCentreOfRotation(preset.getPointToLookAt());
//        camera.rotateAndElevateCameraTo(preset.getAzimuth(), preset.getElevation());
//        camera.zoomCameraTo(preset.getDistance());
    }

    /**
     *
     * @param timeline
     */
    public void setTimeline(Timeline timeline)
    {
        this.timeline.setValue(timeline);
    }

    /**
     *
     * @return
     */
    public Timeline getTimeline()
    {
        return this.timeline.getValue();
    }

    /**
     *
     * @return
     */
    public SimpleObjectProperty timelineProperty()
    {
        return timeline;
    }

    private Group buildBed()
    {
        String bedOuterURL = CoreTest.class
            .getResource(ApplicationConfiguration.modelResourcePath + "bedOuter.obj").toExternalForm();
        String bedInnerURL = CoreTest.class.getResource(ApplicationConfiguration.modelResourcePath
            + "bedInner.obj").toExternalForm();

        PhongMaterial bedOuterMaterial = new PhongMaterial(Color.rgb(65, 65, 65));

        bedOuterMaterial.setSpecularColor(Color.WHITE);

        bedOuterMaterial.setSpecularPower(5.0);

        PhongMaterial bedInnerMaterial = new PhongMaterial(Color.GREY);

        bedInnerMaterial.setSpecularColor(Color.WHITE);

        bedInnerMaterial.setSpecularPower(.1);

        Group bed = new Group();
        bed.setId("Bed");

        ObjImporter bedOuterImporter = new ObjImporter();
        ModelLoadResult bedOuterLoadResult = bedOuterImporter.loadFile(null, bedOuterURL, null);

        bed.getChildren().addAll(bedOuterLoadResult.getModelContainer().getMeshes());

        ObjImporter bedInnerImporter = new ObjImporter();
        ModelLoadResult bedInnerLoadResult = bedInnerImporter.loadFile(null, bedInnerURL, null);

        bed.getChildren().addAll(bedInnerLoadResult.getModelContainer().getMeshes());

        final Image roboxLogoImage = new Image(CoreTest.class.getResource(
            ApplicationConfiguration.imageResourcePath + "roboxLogo.png").toExternalForm());
        final ImageView roboxLogoView = new ImageView();

        roboxLogoView.setImage(roboxLogoImage);

        final Xform roboxLogoTransformNode = new Xform();

        roboxLogoTransformNode.setRotateX(-90);

        final double logoSide_mm = 100;
        double logoScale = logoSide_mm / roboxLogoImage.getWidth();

        roboxLogoTransformNode.setScale(logoScale);

        roboxLogoTransformNode.setTz(logoSide_mm
            + PrintBed.getPrintVolumeCentre().getZ() / 2);
        roboxLogoTransformNode.setTy(-.25);
        roboxLogoTransformNode.setTx(PrintBed.getPrintVolumeCentre().getX() / 2);
        roboxLogoTransformNode.getChildren().add(roboxLogoView);
        roboxLogoTransformNode.setId("LogoImage");

        bed.getChildren().add(roboxLogoTransformNode);
        bed.setMouseTransparent(true);

        return bed;
    }

//    private void buildAxes()
//    {
//        final PhongMaterial redMaterial = new PhongMaterial();
//        redMaterial.setDiffuseColor(Color.DARKRED);
//        redMaterial.setSpecularColor(Color.RED);
//
//        final PhongMaterial greenMaterial = new PhongMaterial();
//        greenMaterial.setDiffuseColor(Color.DARKGREEN);
//        greenMaterial.setSpecularColor(Color.GREEN);
//
//        final Box xAxis = new Box(40, 1, 1);
//        xAxis.setTranslateZ(-25);
//        xAxis.setTranslateX(-20);
//
//        final Box zAxis = new Box(1, 1, 40);
//        zAxis.setTranslateX(-25);
//        zAxis.setTranslateX(-20);
//        zAxis.setTranslateZ(-20);
//
//        xAxis.setMaterial(redMaterial);
//        zAxis.setMaterial(greenMaterial);
//
//        axisGroup.getChildren().addAll(xAxis, zAxis);
//        root3D.getChildren().add(axisGroup);
//
////        autoScalingGroup.getChildren().addAll(axisGroup);
//    }
    /**
     *
     * @param modelContainer
     */
    public void addModel(ModelContainer modelContainer)
    {
        if (modelContainer.getModelContentsType() == ModelContentsEnumeration.MESH)
        {
            models.getChildren().add(modelContainer);
            loadedModels.add(modelContainer);
            collideModels();
        } else
        {
            steno.info("About to add gcode to model");
            models.getChildren().add(modelContainer);
            steno.info("Done adding gcode");
        }
        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
    }

    /**
     *
     */
    public void deleteSelectedModels()
    {
        for (ModelContainer chosenModel : selectedModelContainers.getSelectedModelsSnapshot())
        {
            selectedModelContainers.removeModelContainer(chosenModel);
            loadedModels.remove(chosenModel);
            models.getChildren().remove(chosenModel);
        }
        collideModels();
        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
    }

    /**
     *
     */
    public void copySelectedModels()
    {
        for (ModelContainer model : selectedModelContainers.getSelectedModelsSnapshot())
        {
            addModel(model.makeCopy());
        }
        collideModels();
    }

    /**
     *
     * @param modelGroup
     */
    public void removeModel(ModelContainer modelGroup)
    {
        models.getChildren().remove(modelGroup);
        loadedModels.remove(modelGroup);
        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
    }

    /**
     *
     * @param gCodeParts
     */
    public void addGCodeParts(Group gCodeParts)
    {
        if (this.gcodeParts != null)
        {
            models.getChildren().remove(this.gcodeParts);
        }
        this.gcodeParts = gcodeParts;
        models.getChildren().add(gCodeParts);
    }

    /**
     *
     */
    public void shutdown()
    {
        applicationStatus.modeProperty().removeListener(applicationModeListener);
        dragMode.removeListener(dragModeListener);
    }

    /**
     *
     * @param selectedNode
     * @param multiSelect
     */
    public void selectModel(ModelContainer selectedNode, boolean multiSelect)
    {
        if (selectedNode == null)
        {
            selectedModelContainers.deselectAllModels();
        } else if (selectedNode.isSelected() == false)
        {
            if (multiSelect == false)
            {
                selectedModelContainers.deselectAllModels();
            }
            selectedModelContainers.addModelContainer(selectedNode);
        }
    }

    /**
     *
     * @param x
     * @param z
     */
    public void translateSelection(double x, double z)
    {
        for (ModelContainer model : loadedModels)
        {
            if (selectedModelContainers.isSelected(model))
            {
                model.translateBy(x, z);
            }
        }
        selectedModelContainers.updateSelectedValues();

        collideModels();
        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
    }

    /**
     *
     * @param x
     */
    public void translateSelectionX(double x)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.translateBy(x, 0);
            }
        }

        collideModels();
        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
    }

    /**
     *
     * @param z
     */
    public void translateSelectionZ(double z)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.translateBy(0, z);
            }
        }

        collideModels();
        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
    }

    /**
     *
     * @param x
     */
    public void translateSelectionXTo(double x)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.translateXTo(x);
            }
        }

        collideModels();
        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
    }

    /**
     *
     * @param z
     */
    public void translateSelectionZTo(double z)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.translateZTo(z);
            }
        }

        collideModels();
        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
    }

    /**
     *
     * @param width
     */
    public void resizeSelectionWidth(double width)
    {
        for (ModelContainer model : loadedModels)
        {
            if (selectedModelContainers.isSelected(model))
            {
                model.resizeWidth(width);
            }
        }
        selectedModelContainers.updateSelectedValues();

        collideModels();
        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
    }

    /**
     *
     * @param height
     */
    public void resizeSelectionHeight(double height)
    {
        for (ModelContainer model : loadedModels)
        {
            if (selectedModelContainers.isSelected(model))
            {
                model.resizeHeight(height);
            }
        }
        selectedModelContainers.updateSelectedValues();

        collideModels();
        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
    }

    /**
     *
     * @param depth
     */
    public void resizeSelectionDepth(double depth)
    {
        for (ModelContainer model : loadedModels)
        {
            if (selectedModelContainers.isSelected(model))
            {
                model.resizeDepth(depth);
            }
        }
        selectedModelContainers.updateSelectedValues();

        collideModels();
        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
    }

    /**
     *
     * @param newScale
     */
    public void scaleSelection(double newScale)
    {
        for (ModelContainer model : loadedModels)
        {
            if (selectedModelContainers.isSelected(model))
            {
                model.setScale(newScale);
            }
        }
        selectedModelContainers.updateSelectedValues();

        collideModels();
        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
    }

    /**
     *
     * @param rotation
     */
    public void rotateSelection(double rotation)
    {
        for (ModelContainer model : loadedModels)
        {
            if (selectedModelContainers.isSelected(model))
            {
                model.setRotationY(rotation);
            }
        }
        selectedModelContainers.updateSelectedValues();

        // TODO: multi select rotate should be around the common centre of the multi select
//            steno.info("Pivot is " + selectionContainer.selectedModelsProperty().get(0).getPivot());
//            selectionContainer.setRotationY(rotation);
//            recalculateSelectionBounds(false);
//        } else
//        {
//            for (ModelContainer model : loadedModels)
//            {
//                if (model.isSelected())
//                {
//                    model.deltaRotateAroundY(rotation);
//                }
//            }
//            selectionContainer.setRotationX(0);
//        }
        collideModels();
        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
    }

    /**
     *
     * @param pickedModel
     */
    public void deselectModel(ModelContainer pickedModel)
    {
        if (pickedModel.isSelected())
        {
            selectedModelContainers.removeModelContainer(pickedModel);
        }
    }

    /**
     *
     * @return
     */
    public ObservableList<ModelContainer> getLoadedModels()
    {
        return loadedModels;
    }

    public void collideModels()
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

                    if (modelToCollide.getBoundsInParent().intersects(
                        modelToCollideWith.getBoundsInParent()))
                    {
                        collidedModels[printableNum] = true;
                        collidedModels[secondaryPrintableNum] = true;
                    }
                }
            }
        }

        for (int index = 0; index < collidedModels.length; index++)
        {
            loadedModels.get(index).setCollision(collidedModels[index]);
        }
    }

    /**
     *
     * @param delta
     */
    public void deltaScaleSelection(double delta)
    {
        for (ModelContainer model : loadedModels)
        {
            if (model.isSelected())
            {
                model.setScale(delta * model.getScale());
            }
        }
        collideModels();
        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
    }

    /**
     *
     */
    public void activateSnapToGround()
    {
        layoutSubmode.set(LayoutSubmode.SNAP_TO_GROUND);
    }

    private void snapToGround(Node intersectedNode, int faceNumber)
    {
        if (intersectedNode instanceof MeshView)
        {
            ModelContainer modelContainer = (ModelContainer) intersectedNode.getParent().getParent();
            modelContainer.snapToGround(faceNumber);
            collideModels();
            DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
        }
        layoutSubmode.set(LayoutSubmode.SELECT);
    }

    /**
     *
     * @return
     */
    public ObjectProperty<LayoutSubmode> layoutSubmodeProperty()
    {
        return layoutSubmode;
    }

    /**
     *
     */
    public void activateGCodeVisualisationMode()
    {
        layoutSubmode.set(LayoutSubmode.GCODE_VISUALISATION);
    }

    /**
     *
     * @return
     */
    public IntegerProperty screenCentreOfSelectionXProperty()
    {
        return screenCentreOfSelectionX;
    }

    /**
     *
     * @return
     */
    public IntegerProperty screenCentreOfSelectionYProperty()
    {
        return screenCentreOfSelectionY;
    }

    /**
     *
     * @return
     */
    public SubScene getSubScene()
    {
        return subScene;
    }

    /**
     *
     * @return
     */
    public Group getRoot()
    {
        return root3D;
    }

    /**
     *
     * @return
     */
    public DoubleProperty demandedCameraRotationYProperty()
    {
        return demandedCameraRotationY;
    }

    /**
     *
     * @return
     */
    public DoubleProperty demandedCameraRotationXProperty()
    {
        return demandedCameraRotationX;
    }

    /**
     *
     * @param value
     */
    public void setDragMode(DragMode value)
    {
        dragMode.set(value);
    }

    /**
     *
     * @return
     */
    public DragMode getDragMode()

    {
        return dragMode.get();
    }

    /**
     *
     * @return
     */
    public ObjectProperty<DragMode> dragModeProperty()
    {
        return dragMode;
    }

    /**
     *
     * @param controller
     */
    public void associateGizmoOverlayController(GizmoOverlayController controller)
    {
//        this.gizmoOverlayController = controller;
    }

    /**
     *
     * @param screenX
     * @param screenY
     */
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

    /**
     *
     * @param translateStartPoint
     * @param screenCoords
     */
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
    private double preAnimationCameraXAngle = 0;
    private double preAnimationCameraYAngle = 0;
    private boolean needToRevertCameraPosition = false;

    /**
     *
     */
    public void startSettingsAnimation()
    {
        preAnimationCameraXAngle = demandedCameraRotationX.get();
        preAnimationCameraYAngle = demandedCameraRotationY.get();
        rotateCameraAroundAxesTo(30, demandedCameraRotationYProperty().get());
        needToRevertCameraPosition = true;
        settingsScreenAnimationTimer.start();
    }

    /**
     *
     */
    public void stopSettingsAnimation()
    {
        settingsScreenAnimationTimer.stop();
        if (needToRevertCameraPosition == true)
        {
            rotateCameraAroundAxesTo(preAnimationCameraXAngle, preAnimationCameraYAngle);
            needToRevertCameraPosition = false;
        }
    }

    /**
     *
     * @param requiredDragMode
     * @param event
     */
    public void enterDragFromGizmo(DragMode requiredDragMode, MouseEvent event)
    {
        Point3D currentDragPosition = event.getPickResult().getIntersectedPoint();
        lastDragPosition = null;
        dragMode.set(requiredDragMode);
    }

    /**
     *
     * @param event
     */
    public void dragFromGizmo(MouseEvent event)
    {
        if (dragMode.get() == DragMode.X_CONSTRAINED_TRANSLATE)
        {
            Point3D currentDragPosition = event.getPickResult().getIntersectedPoint();
            if (lastDragPosition != null)
            {
                Point3D resultant = currentDragPosition.subtract(lastDragPosition);

                translateSelection(resultant.getX(), 0);
            }
            lastDragPosition = currentDragPosition;
        } else if (dragMode.get() == DragMode.Z_CONSTRAINED_TRANSLATE)
        {
            Point3D currentDragPosition = event.getPickResult().getIntersectedPoint();
            if (lastDragPosition != null)
            {
                Point3D resultant = currentDragPosition.subtract(lastDragPosition);

                translateSelection(0, resultant.getZ());
            }
            lastDragPosition = currentDragPosition;
        }
    }

    /**
     *
     */
    public void exitDragFromGizmo()
    {
        dragMode.set(DragMode.IDLE);
    }

    /**
     *
     * @param event
     */
    public void enterRotateFromGizmo(MouseEvent event)
    {
        translationDragPlane.setTranslateY(0);
        dragMode.set(DragMode.ROTATE);
    }

    /**
     *
     * @param event
     * @return
     */
//    public double rotateFromGizmo(MouseEvent event)
//    {
//        Point3D currentDragPosition = event.getPickResult().getIntersectedPoint();
////        double xPos = currentDragPosition.getX() - selectionContainer.getCentreX();
////        double yPos = currentDragPosition.getZ() - selectionContainer.getCentreZ();
//
//        double rotationAngle = MathUtils.cartesianToAngleDegreesCWFromTop(currentDragPosition.getX(),
//                                                                          currentDragPosition.getZ());
//
//        double outputAngle = gizmoStartingRotationAngle - rotationAngle;
//
//        if (!gizmoRotationStarted)
//        {
//            gizmoRotationStarted = true;
//            gizmoStartingRotationAngle = rotationAngle;
//            gizmoRotationOffset = selectionContainer.getRotationY();
//            return rotationAngle;
//        } else
//        {
//            steno.info("Rotating to " + outputAngle + " selRot=" + selectionContainer.getRotationY());
//            rotateSelection(outputAngle + gizmoRotationOffset);
//            return outputAngle;
//        }
//    }
    /**
     *
     */
    public void exitRotateFromGizmo()
    {
        dragMode.set(DragMode.IDLE);
        gizmoRotationStarted = false;
    }

    /**
     *
     * @param loadedModels
     */
    public void setLoadedModels(ObservableList<ModelContainer> loadedModels)
    {
        this.loadedModels = loadedModels;
        if (loadedModels.isEmpty() == false)
        {
            for (ModelContainer model : loadedModels)
            {
                models.getChildren().add(model);
            }
        }
    }

    public SelectedModelContainers getSelectedModelContainers()
    {
        return selectedModelContainers;
    }

    public void selectAllModels()
    {
        for (ModelContainer modelContainer : loadedModels)
        {
            selectedModelContainers.addModelContainer(modelContainer);
        }
    }
}
