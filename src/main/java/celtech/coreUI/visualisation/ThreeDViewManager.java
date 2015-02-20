package celtech.coreUI.visualisation;

import celtech.CoreTest;
import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.PrintBed;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.controllers.GizmoOverlayController;
import celtech.coreUI.visualisation.importers.ModelLoadResult;
import celtech.coreUI.visualisation.importers.obj.ObjImporter;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.model.Printer;
import java.util.Set;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
public class ThreeDViewManager implements Project.ProjectChangesListener
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
        ThreeDViewManager.class.getName());

    private ObservableList<ModelContainer> loadedModels = null;
    private final ApplicationStatus applicationStatus = ApplicationStatus.getInstance();

    private final PrintBed printBedData = PrintBed.getInstance();
    private final Group root3D = new Group();
    private SubScene subScene = null;
    private final SimpleObjectProperty<SubScene> subSceneProperty = new SimpleObjectProperty<>();

    final Group axisGroup = new Group();
    double DELTA_MULTIPLIER = 200.0;
    double CONTROL_MULTIPLIER = 0.1;
    double SHIFT_MULTIPLIER = 0.1;
    double ALT_MULTIPLIER = 0.5;

    /*
     * Model moving
     */
    private Point3D lastDragPosition = null;
    private final int dragPlaneHalfSize = 500;
    private final Box translationDragPlane = new Box(dragPlaneHalfSize * 2, 0.1, dragPlaneHalfSize
                                                     * 2);
    private final Box scaleDragPlane = new Box(dragPlaneHalfSize * 2, dragPlaneHalfSize * 2, 0.1);
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

    /*
     * ALT stuff
     */
    private final Xform bedTranslateXform = new Xform(Xform.RotateOrder.YXZ, "BedXForm");
    private Group bed;
    private final PerspectiveCamera camera = new PerspectiveCamera(true);

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

    private SelectedModelContainers selectedModelContainers = null;

    private long lastAnimationTrigger = 0;

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
    private Filament extruder0Filament;
    private Filament extruder1Filament;
    private final Project project;
    private final ObjectProperty<LayoutSubmode> layoutSubmode;

    private void rotateCameraAroundAxes(double xangle, double yangle)
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

    private void rotateCameraAroundAxesTo(double xangle, double yangle)
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
            ModelContainer modelContainer = null;
            if (intersectedNode instanceof MeshView)
            {
                modelContainer = (ModelContainer) intersectedNode.getParent().getParent();
            }

            switch (layoutSubmode.get())
            {
                case SNAP_TO_GROUND:
                    doSnapToGround(modelContainer, pickResult);
                    break;
                case ASSOCIATE_WITH_EXTRUDER0:
                    doAssociateWithExtruder0(modelContainer);
                    break;
                case ASSOCIATE_WITH_EXTRUDER1:
                    doAssociateWithExtruder1(modelContainer);
                    break;
                case SELECT:
                    doSelectTranslateModel(intersectedNode, pickedPoint, event);
                    break;
            }
        }
    }

    private void doSelectTranslateModel(Node intersectedNode, Point3D pickedPoint, MouseEvent event)
    {
        Point3D pickedScenePoint = intersectedNode.localToScene(pickedPoint);
        Point3D pickedBedTranslateXformPoint = bedTranslateXform.sceneToLocal(
            pickedScenePoint);

        translationDragPlane.setTranslateY(pickedBedTranslateXformPoint.getY());
        Point3D pickedDragPlanePoint = translationDragPlane.sceneToLocal(
            pickedScenePoint);
        lastDragPosition = pickedDragPlanePoint;

        Point3D bedXToS = bedTranslateXform.localToParent(pickedPoint);
        scaleDragPlane.setTranslateX(bedXToS.getX());
        scaleDragPlane.setTranslateY(bedXToS.getY());
        scaleDragPlane.setTranslateZ(pickedPoint.getZ());

        setDragMode(DragMode.TRANSLATING);

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

    private void doAssociateWithExtruder1(ModelContainer modelContainer)
    {
        if (modelContainer != null)
        {
            modelContainer.setUseExtruder0Filament(false);
            updateModelColour(modelContainer);
            layoutSubmode.set(LayoutSubmode.SELECT);
            project.projectModified();
        }
    }

    private void doAssociateWithExtruder0(ModelContainer modelContainer)
    {
        if (modelContainer != null)
        {
            modelContainer.setUseExtruder0Filament(true);
            updateModelColour(modelContainer);
            layoutSubmode.set(LayoutSubmode.SELECT);
            project.projectModified();
        }
    }

    private void doSnapToGround(ModelContainer modelContainer, PickResult pickResult)
    {
        if (modelContainer != null)
        {
            int faceNumber = pickResult.getIntersectedFace();
            snapToGround(modelContainer, faceNumber);
            collideModels();
            project.projectModified();
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
                        deselectAllModels();
                        startSettingsAnimation();
                        break;
                    default:
                        goToPreset(CameraPositionPreset.FRONT);
                        subScene.addEventHandler(MouseEvent.ANY, mouseEventHandler);
                        subScene.addEventHandler(ZoomEvent.ANY, zoomEventHandler);
                        subScene.addEventHandler(ScrollEvent.ANY, scrollEventHandler);
                        stopSettingsAnimation();
                        break;
                }
                updateFilamentColoursForModeAndTargetPrinter();
            }
        }

    };

    public ThreeDViewManager(Project project,
        ReadOnlyDoubleProperty widthProperty, ReadOnlyDoubleProperty heightProperty)
    {
        this.project = project;
        loadedModels = project.getLoadedModels();
        selectedModelContainers = Lookup.getProjectGUIState(project).getSelectedModelContainers();
        layoutSubmode = Lookup.getProjectGUIState(project).getLayoutSubmodeProperty();

        widthPropertyToFollow = widthProperty;
        heightPropertyToFollow = heightProperty;

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

        layoutSubmode.addListener(
            (ObservableValue<? extends LayoutSubmode> ov, LayoutSubmode t, LayoutSubmode t1) ->
        {
            if (t1 == LayoutSubmode.SNAP_TO_GROUND ||
                t1 == LayoutSubmode.ASSOCIATE_WITH_EXTRUDER0 ||
                t1 == LayoutSubmode.ASSOCIATE_WITH_EXTRUDER1)
            {
                subScene.setCursor(Cursor.HAND);
            } else
            {
                subScene.setCursor(Cursor.DEFAULT);
            }
        });

        dragMode.addListener(dragModeListener);

        /**
         * Set up filament, application mode and printer listeners so that the correct model colours
         * are displayed.
         */
        setupFilamentListeners(project);
        setupPrintSettingsFilamentListeners(project);
        updateFilamentColoursForModeAndTargetPrinter();
        project.getPrinterSettings().selectedPrinterProperty().addListener(
            (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
            {
                updateFilamentColoursForModeAndTargetPrinter();
            });

        /**
         * Listen for adding and removing of models from the project
         */
        project.addProjectChangesListener(this);
    }

    private void goToPreset(CameraPositionPreset preset)
    {
//        camera.setCentreOfRotation(preset.getPointToLookAt());
//        camera.rotateAndElevateCameraTo(preset.getAzimuth(), preset.getElevation());
//        camera.zoomCameraTo(preset.getDistance());
    }

    private Group buildBed()
    {
        String bedOuterURL = CoreTest.class
            .getResource(ApplicationConfiguration.modelResourcePath + "bedOuter.obj").
            toExternalForm();
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

    public void addGCodeParts(Group gCodeParts)
    {
        if (this.gcodeParts != null)
        {
            models.getChildren().remove(this.gcodeParts);
        }
        this.gcodeParts = gCodeParts;
        models.getChildren().add(gCodeParts);
    }

    public void shutdown()
    {
        applicationStatus.modeProperty().removeListener(applicationModeListener);
        dragMode.removeListener(dragModeListener);
    }

    private void selectModel(ModelContainer selectedNode, boolean multiSelect)
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

    private void translateSelection(double x, double z)
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
        project.projectModified();
    }

    private void deselectModel(ModelContainer pickedModel)
    {
        if (pickedModel.isSelected())
        {
            selectedModelContainers.removeModelContainer(pickedModel);
        }
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

    private void snapToGround(ModelContainer modelContainer, int faceNumber)
    {
        if (modelContainer != null)
        {
            modelContainer.snapToGround(faceNumber);
            collideModels();
            project.projectModified();
        }
        layoutSubmode.set(LayoutSubmode.SELECT);
    }

    public SubScene getSubScene()
    {
        return subScene;
    }

    private DoubleProperty demandedCameraRotationYProperty()
    {
        return demandedCameraRotationY;
    }

    private void setDragMode(DragMode value)
    {
        dragMode.set(value);
    }

    public void associateGizmoOverlayController(GizmoOverlayController controller)
    {
//        this.gizmoOverlayController = controller;
    }

    private void checkit(double screenX, double screenY)
    {
        Point2D screenToLocal = camera.screenToLocal(screenX, screenY);
//        Point2D localToSceneToScreen = camera.localToScreen(localToScene);
        steno.debug("screen to local " + screenToLocal);

        Point3D localToScene = camera.localToScene(screenToLocal.getX(), screenToLocal.getY(), 0);
        steno.debug("Local to scene " + localToScene);

        Point3D correctedBed = bedTranslateXform.sceneToLocal(localToScene);
        steno.debug("Corrected bed = " + correctedBed);

//        Point3D testPoint = new Point3D(selectionContainer.getCentreX(), selectionContainer.getCentreY(), selectionContainer.getCentreZ());
    }

    private double preAnimationCameraXAngle = 0;
    private double preAnimationCameraYAngle = 0;
    private boolean needToRevertCameraPosition = false;

    private void startSettingsAnimation()
    {
        preAnimationCameraXAngle = demandedCameraRotationX.get();
        preAnimationCameraYAngle = demandedCameraRotationY.get();
        rotateCameraAroundAxesTo(30, demandedCameraRotationYProperty().get());
        needToRevertCameraPosition = true;
        settingsScreenAnimationTimer.start();
    }

    private void stopSettingsAnimation()
    {
        settingsScreenAnimationTimer.stop();
        if (needToRevertCameraPosition == true)
        {
            rotateCameraAroundAxesTo(preAnimationCameraXAngle, preAnimationCameraYAngle);
            needToRevertCameraPosition = false;
        }
    }

    public void enterDragFromGizmo(DragMode requiredDragMode, MouseEvent event)
    {
        Point3D currentDragPosition = event.getPickResult().getIntersectedPoint();
        lastDragPosition = null;
        dragMode.set(requiredDragMode);
    }

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

    public void exitDragFromGizmo()
    {
        dragMode.set(DragMode.IDLE);
    }

    public void enterRotateFromGizmo(MouseEvent event)
    {
        translationDragPlane.setTranslateY(0);
        dragMode.set(DragMode.ROTATE);
    }

    public void exitRotateFromGizmo()
    {
        dragMode.set(DragMode.IDLE);
        gizmoRotationStarted = false;
    }


    private void updateModelColours()
    {
        for (ModelContainer model : loadedModels)
        {
            updateModelColour(model);
        }
    }

    private void updateModelColour(ModelContainer model)
    {
        Color colour0 = null;
        Color colour1 = null;
        if (extruder0Filament != null)
        {
            colour0 = extruder0Filament.getDisplayColour();
        }
        if (extruder1Filament != null)
        {
            colour1 = extruder1Filament.getDisplayColour();
        }
        model.setColour(colour0, colour1);
    }

    private void deselectAllModels()
    {
        for (ModelContainer modelContainer : loadedModels)
        {
            deselectModel(modelContainer);
        }
    }

    /**
     * Models must reflect the project filament colours.
     */
    private void setupFilamentListeners(Project project)
    {
        project.getExtruder0FilamentProperty().addListener(
            (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
            {
                updateFilamentColoursForModeAndTargetPrinter();
            });

        project.getExtruder1FilamentProperty().addListener(
            (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
            {
                updateFilamentColoursForModeAndTargetPrinter();
            });
        updateModelColours();
    }

    /**
     * Models must reflect the project's print settings filament colours.
     */
    private void setupPrintSettingsFilamentListeners(Project project)
    {
        project.getPrinterSettings().getFilament0Property().addListener(
            (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
            {
                updateFilamentColoursForModeAndTargetPrinter();
            });

        project.getPrinterSettings().getFilament1Property().addListener(
            (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
            {
                updateFilamentColoursForModeAndTargetPrinter();
            });
        updateModelColours();
    }

    private boolean targetPrinterHasOneExtruder()
    {
        boolean extruder1IsFitted
            = project.getPrinterSettings().getSelectedPrinter().extrudersProperty().get(
                1).isFittedProperty().get();
        return !extruder1IsFitted;
    }

    /**
     * If either the chosen filaments, application mode or project printsettings printer
     * changes then this must be called. In LAYOUT mode the filament colours should reflect the
     * project filament colours In SETTINGS mode the filament colours should reflect the project
     * print settings filament colours.
     */
    private void updateFilamentColoursForModeAndTargetPrinter()
    {
        if (applicationStatus.getMode() == ApplicationMode.SETTINGS)
        {
            extruder0Filament = project.getPrinterSettings().getFilament0();
            extruder1Filament = project.getPrinterSettings().getFilament1();
            if (targetPrinterHasOneExtruder())
            {
                extruder1Filament = extruder0Filament;
            }
        } else
        {
            extruder0Filament = project.getExtruder0FilamentProperty().get();
            extruder1Filament = project.getExtruder1FilamentProperty().get();
        }
        updateModelColours();
    }

    @Override
    public void whenModelAdded(ModelContainer modelContainer)
    {
        models.getChildren().add(modelContainer);
        updateModelColour(modelContainer);
        collideModels();
    }

    @Override
    public void whenModelRemoved(ModelContainer modelContainer)
    {
        models.getChildren().remove(modelContainer);
        collideModels();
    }

    @Override
    public void whenAutoLaidOut()
    {
        collideModels();
    }

    @Override
    public void whenModelsTransformed(Set<ModelContainer> modelContainers)
    {
        collideModels();
    }

}
