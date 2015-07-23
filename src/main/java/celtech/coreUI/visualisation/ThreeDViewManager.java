package celtech.coreUI.visualisation;

import celtech.CoreTest;
import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.undo.UndoableProject;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.PrintBed;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.coreUI.LayoutSubmode;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.coreUI.visualisation.metaparts.ModelLoadResult;
import celtech.coreUI.visualisation.modelDisplay.SelectionHighlighter;
import celtech.utils.threed.importers.obj.ObjImporter;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.printerControl.model.Printer;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
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
import javafx.geometry.Point3D;
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

    private ObservableList<ModelContainer> loadedModels;
    private final ApplicationStatus applicationStatus = ApplicationStatus.getInstance();

    private final PrintBed printBedData = PrintBed.getInstance();
    private final Group root3D = new Group();
    private SubScene subScene;
    private final SimpleObjectProperty<SubScene> subSceneProperty = new SimpleObjectProperty<>();

    final Group axisGroup = new Group();
    double DELTA_MULTIPLIER = 200.0;
    double CONTROL_MULTIPLIER = 0.1;
    double SHIFT_MULTIPLIER = 0.1;
    double ALT_MULTIPLIER = 0.5;

    /*
     * Model moving
     */
    private Point3D lastDragPosition;
    private final int dragPlaneHalfSize = 500;
    private final Box translationDragPlane = new Box(dragPlaneHalfSize * 2, 0.1, dragPlaneHalfSize
                                                     * 2);
    private final Box scaleDragPlane = new Box(dragPlaneHalfSize * 2, dragPlaneHalfSize * 2, 0.1);

    private Group gcodeParts;

    private Group models = new Group();
    /*
     * Selection stuff
     */
    private ObjectProperty<DragMode> dragMode = new SimpleObjectProperty(DragMode.IDLE);

    private ReadOnlyDoubleProperty widthPropertyToFollow;
    private ReadOnlyDoubleProperty heightPropertyToFollow;
    private final Set<ModelContainer> excludedFromSelection = new HashSet<>();

    /*
     * ALT stuff
     */
    private final Xform bedTranslateXform = new Xform(Xform.RotateOrder.YXZ, "BedXForm");
    private final Group bed;
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
    private final UndoableProject undoableProject;
    private final ObjectProperty<LayoutSubmode> layoutSubmode;
    private boolean justEnteredDragMode;

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

        notifyModelsOfCameraViewChange();
    }

    private void notifyModelsOfCameraViewChange()
    {
        for (Node node : models.getChildren())
        {
            //Relying on only models being here...
            ModelContainer model = (ModelContainer) node;
            model.cameraViewOfYouHasChanged();
        }
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

        notifyModelsOfCameraViewChange();
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

    private void handleMouseDoubleClickedEvent(MouseEvent event)
    {
        Node intersectedNode = event.getPickResult().getIntersectedNode();
        if (intersectedNode instanceof MeshView)
        {
            if (excludedFromSelection.contains((ModelContainer) intersectedNode.getParent()))
            {
                return;
            }
            // if clicked mc is within a selected group then isolate the group below the selected
            // group that contains selected mc.
            Set<ModelContainer> selectedModelContainers
                = Lookup.getProjectGUIState(project).getSelectedModelContainers().getSelectedModelsSnapshot();
            Set<MeshView> selectedMeshViews
                = selectedModelContainers.stream().
                map(mc -> mc.descendentMeshViews()).
                reduce(new HashSet<>(), (a, b) ->
                       {
                           a.addAll(b);
                           return a;
                });
            System.out.println("selected mcs is " + selectedMeshViews);
            if (selectedMeshViews.contains((MeshView) intersectedNode))
            {
                System.out.println("B");
                isolateForSelectedMeshView((MeshView) intersectedNode);
            }
        }
    }

    private void handleMouseSingleClickedEvent(MouseEvent event)
    {
        boolean handleThisEvent = true;

        mousePosX = event.getSceneX();
        mousePosY = event.getSceneY();
        mouseOldX = event.getSceneX();
        mouseOldY = event.getSceneY();

        PickResult pickResult = event.getPickResult();
        Point3D pickedPoint = pickResult.getIntersectedPoint();
        Node intersectedNode = pickResult.getIntersectedNode();

        if (intersectedNode.getParent() != null)
        {
            if (intersectedNode.getParent().getId() != null)
            {
                if (intersectedNode.getParent().getId().equals(SelectionHighlighter.idString))
                {
                    // Expect the selection highlighter to handle this one...
                    handleThisEvent = false;
                }
            }
        }

        if (handleThisEvent)
        {
            if (intersectedNode instanceof MeshView)
            {
                if (excludedFromSelection.contains((ModelContainer) intersectedNode.getParent()))
                {
                    return;
                }

                ModelContainer rootModelContainer = 
                    ModelContainer.getRootModelContainer((MeshView) intersectedNode);
                switch (layoutSubmode.get())
                {
                    case SNAP_TO_GROUND:
                        doSnapToGround(rootModelContainer, (MeshView) intersectedNode, pickResult);
                        break;
                    case SELECT:
                        doSelectTranslateModel(intersectedNode, pickedPoint, event);
                        break;
                }
            } else
            {
                selectedModelContainers.deselectAllModels();
                excludedFromSelection.clear();
                updateModelColoursForPositionModeAndTargetPrinter();
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
        justEnteredDragMode = true;

        Parent parent = intersectedNode.getParent();
        if (!(parent instanceof ModelContainer))
        {
            parent = parent.getParent();
        }

        ModelContainer pickedModel = (ModelContainer) parent;
        // get top-level ModelContainer (could be grouped) that is not excluded from
        // selection
        while (pickedModel.getParentModelContainer() instanceof ModelContainer
            && !excludedFromSelection.contains(pickedModel.getParentModelContainer()))
        {
            pickedModel = (ModelContainer) pickedModel.getParentModelContainer();
        }

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
    }

    private void doSnapToGround(ModelContainer modelContainer, MeshView meshView,
        PickResult pickResult)
    {
        if (modelContainer != null)
        {
            int faceNumber = pickResult.getIntersectedFace();
            undoableProject.snapToGround(modelContainer, meshView, faceNumber);
            layoutSubmode.set(LayoutSubmode.SELECT);
        }
    }
    
    /**
     * If any of the current selection are a child of a group then return true.
     */
    private boolean selectionHasChildOfGroup() {
        for (ModelContainer modelContainer : selectedModelContainers.getSelectedModelsSnapshot())
        {
            if (modelContainer.getParentModelContainer() != null) {
                return true;
            }
        }
        return false;
    }

    private void handleMouseDragEvent(MouseEvent event)
    {
        
        if (selectionHasChildOfGroup()) {
            return;
        }

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
            notifyModelsOfCameraViewChange();
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
                    justEnteredDragMode = false;
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
            if (event.getClickCount() == 2 && event.isPrimaryButtonDown())
            {
                handleMouseDoubleClickedEvent(event);
            } else if (event.isPrimaryButtonDown())
            {
                handleMouseSingleClickedEvent(event);
            }

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

        notifyModelsOfCameraViewChange();
    };
    private final EventHandler<ZoomEvent> zoomEventHandler = event ->
    {
        if (!Double.isNaN(event.getZoomFactor()) && event.getZoomFactor() > 0.8
            && event.getZoomFactor() < 1.2)
        {
            double z = bedTranslateXform.getTz() / event.getZoomFactor();
            cameraDistance.set(z);
            bedTranslateXform.setTz(z);
            notifyModelsOfCameraViewChange();
        }
    };

    private final ChangeListener<ApplicationMode> applicationModeListener
        = (ObservableValue<? extends ApplicationMode> ov, ApplicationMode oldMode, ApplicationMode newMode) ->
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
                updateModelColoursForPositionModeAndTargetPrinter();
            }
        };

    public ThreeDViewManager(Project project,
        ReadOnlyDoubleProperty widthProperty, ReadOnlyDoubleProperty heightProperty)
    {
        this.project = project;
        this.undoableProject = new UndoableProject(project);
        loadedModels = project.getTopLevelModels();
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

        PointLight cameraLight = new PointLight();
        cameraLight.setTranslateX(camera.getTranslateX());
        cameraLight.setTranslateY(camera.getTranslateY());
        cameraLight.setTranslateZ(camera.getTranslateZ());

//        demandedCameraRotationX.addListener((ObservableValue<? extends Number> ov, Number t, Number t1) ->
//        {
//            cameraLight.setColor(Color.rgb(255, 255, 255, 1 - (t1.doubleValue() / 90)));
//        });
        root3D.getChildren().add(cameraLight);

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

        PointLight overheadLight = new PointLight();

        overheadLight.setTranslateX(105);
        overheadLight.setTranslateY(-400);
        overheadLight.setTranslateZ(75);

        bedTranslateXform.getChildren().addAll(overheadLight, bed, models, translationDragPlane,
                                               scaleDragPlane);
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
                if (t1 == LayoutSubmode.SNAP_TO_GROUND)
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
        updateModelColoursForPositionModeAndTargetPrinter();
        Lookup.getSelectedPrinterProperty().addListener(
            (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
            {
                updateModelColoursForPositionModeAndTargetPrinter();
            });

        project.getPrinterSettings().getPrintSupportOverrideProperty().addListener(
            (ObservableValue<? extends Object> observable, Object oldValue, Object newValue) ->
            {
                updateModelColoursForPositionModeAndTargetPrinter();
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
            .getResource(ApplicationConfiguration.modelResourcePath + "bedBase.obj").
            toExternalForm();

        String peiSheetURL = CoreTest.class.getResource(ApplicationConfiguration.modelResourcePath
            + "pei.obj").toExternalForm();

        String bedClipsURL = CoreTest.class.getResource(ApplicationConfiguration.modelResourcePath
            + "clips.obj").toExternalForm();

        PhongMaterial bedOuterMaterial = new PhongMaterial(Color.web("#0a0a0a"));

        PhongMaterial peiSheetMaterial = new PhongMaterial(Color.web("#a0a0a0"));
        peiSheetMaterial.setSpecularPower(1.2f);

        PhongMaterial bedClipsMaterial = new PhongMaterial(Color.web("#f0f0f0"));
        bedClipsMaterial.setSpecularPower(20f);

        Group bed = new Group();

        bed.setId("Bed");

        ObjImporter bedOuterImporter = new ObjImporter();
        ModelLoadResult bedOuterLoadResult = bedOuterImporter.loadFile(null, bedOuterURL, null);
        MeshView outerMeshView = bedOuterLoadResult.getModelContainers().iterator().next().getMeshView();
        outerMeshView.setMaterial(bedOuterMaterial);
        bed.getChildren().addAll(outerMeshView);

        ObjImporter peiSheetImporter = new ObjImporter();
        ModelLoadResult peiSheetLoadResult = peiSheetImporter.loadFile(null, peiSheetURL, null);
        MeshView peiMeshView = peiSheetLoadResult.getModelContainers().iterator().next().getMeshView();
        peiMeshView.setMaterial(peiSheetMaterial);

        bed.getChildren().addAll(peiMeshView);

        ObjImporter bedClipsImporter = new ObjImporter();
        ModelLoadResult bedClipsLoadResult = bedClipsImporter.loadFile(null, bedClipsURL, null);
        MeshView bedClipsMeshView = bedClipsLoadResult.getModelContainers().iterator().next().getMeshView();
        bedClipsMeshView.setMaterial(bedClipsMaterial);
        bed.getChildren().addAll(bedClipsMeshView);

        bed.getChildren().add(createBoundingBox());

        final Image roboxLogoImage = new Image(CoreTest.class.getResource(
            ApplicationConfiguration.imageResourcePath + "BedGraphics.png").toExternalForm());
        final ImageView roboxLogoView = new ImageView();

        roboxLogoView.setImage(roboxLogoImage);

        final Xform roboxLogoTransformNode = new Xform();

        roboxLogoTransformNode.setTz(150);

        roboxLogoTransformNode.setRotateX(-90);

        roboxLogoTransformNode.setScale(0.084);

        roboxLogoTransformNode.setTy(-.25);

        roboxLogoTransformNode.getChildren()
            .add(roboxLogoView);
        roboxLogoTransformNode.setId("LogoImage");

        bed.getChildren()
            .add(roboxLogoTransformNode);
        bed.setMouseTransparent(true);

        return bed;
    }

    private Node createBoundingBox()
    {
        PhongMaterial boundsBoxMaterial = new PhongMaterial(Color.BLUE);
        Image illuminationMap = new Image(SelectionHighlighter.class.getResource(
            ApplicationConfiguration.imageResourcePath + "blueIlluminationMap.png").
            toExternalForm());
        boundsBoxMaterial.setSelfIlluminationMap(illuminationMap);

        Group boxGroup = new Group();

        double lineWidth = .1;
        double printAreaHeight = -printBedData.getPrintVolumeBounds().getHeight();
        double printAreaWidth = printBedData.getPrintVolumeBounds().getWidth();
        double printAreaDepth = printBedData.getPrintVolumeBounds().getDepth();

        Box lhf = new Box(lineWidth, printAreaHeight, lineWidth);
        lhf.setMaterial(boundsBoxMaterial);
        lhf.setTranslateY(-printAreaHeight / 2);

        Box rhf = new Box(lineWidth, printAreaHeight, lineWidth);
        rhf.setMaterial(boundsBoxMaterial);
        rhf.setTranslateY(-printAreaHeight / 2);
        rhf.setTranslateX(printAreaWidth);

        Box lhb = new Box(lineWidth, printAreaHeight, lineWidth);
        lhb.setMaterial(boundsBoxMaterial);
        lhb.setTranslateY(-printAreaHeight / 2);
        lhb.setTranslateZ(printAreaDepth);

        Box rhb = new Box(lineWidth, printAreaHeight, lineWidth);
        rhb.setMaterial(boundsBoxMaterial);
        rhb.setTranslateY(-printAreaHeight / 2);
        rhb.setTranslateX(printAreaWidth);
        rhb.setTranslateZ(printAreaDepth);

        Box lhftTOlhbt = new Box(lineWidth, lineWidth,
                                 printBedData.getPrintVolumeBounds().getDepth());
        lhftTOlhbt.setMaterial(boundsBoxMaterial);
        lhftTOlhbt.setTranslateY(-printAreaHeight);
        lhftTOlhbt.setTranslateZ(printAreaDepth / 2);

        Box rhftTOrhbt = new Box(lineWidth, lineWidth,
                                 printBedData.getPrintVolumeBounds().getDepth());
        rhftTOrhbt.setMaterial(boundsBoxMaterial);
        rhftTOrhbt.setTranslateX(printAreaWidth);
        rhftTOrhbt.setTranslateY(-printAreaHeight);
        rhftTOrhbt.setTranslateZ(printAreaDepth / 2);

        Box lhftTOrhft = new Box(printAreaWidth, lineWidth, lineWidth);
        lhftTOrhft.setMaterial(boundsBoxMaterial);
        lhftTOrhft.setTranslateX(printAreaWidth / 2);
        lhftTOrhft.setTranslateY(-printAreaHeight);

        Box lhbtTOrhbt = new Box(printAreaWidth, lineWidth, lineWidth);
        lhbtTOrhbt.setMaterial(boundsBoxMaterial);
        lhbtTOrhbt.setTranslateX(printAreaWidth / 2);
        lhbtTOrhbt.setTranslateY(-printAreaHeight);
        lhbtTOrhbt.setTranslateZ(printAreaDepth);

        boxGroup.getChildren().addAll(lhf, rhf, lhb, rhb,
                                      lhftTOlhbt, rhftTOrhbt,
                                      lhftTOrhft, lhbtTOrhbt);

        return boxGroup;
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
        undoableProject.translateModelsBy(selectedModelContainers.getSelectedModelsSnapshot(), x, z,
                                          !justEnteredDragMode);
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

    private void collideModels()
    {
        // stub this out at Chris' request until it is more precise
    }

    private void collideModelsOld()
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

    /**
     * Isolate the group contents for the group under the currently selected group that contains the
     * selected MeshView.
     */
    private void isolateForSelectedMeshView(MeshView meshView)
    {
        ModelGroup ancestorSelectedGroup = getAncestorSelectedGroup(meshView);
        if (ancestorSelectedGroup != null)
        {
            System.out.println("C " + ancestorSelectedGroup + " "
                + ancestorSelectedGroup.getChildModelContainers());
            // isolate children of ancestor group
            isolateModelContainer(ancestorSelectedGroup);
            //get group/container under ancestor group that contains meshview
            //then select that group
            selectedModelContainers.deselectAllModels();

            for (ModelContainer modelContainer : ancestorSelectedGroup.getChildModelContainers())
            {
                System.out.println("consider " + modelContainer + " "
                    + modelContainer.descendentMeshViews());
                if (modelContainer.descendentMeshViews().contains(meshView))
                {
                    System.out.println(modelContainer + " contains mesh view " + meshView);
                    System.out.println("isolate " + modelContainer);
                    selectedModelContainers.addModelContainer(modelContainer);
                    System.out.println("D");
                    break;
                } else
                {
                    System.out.println(modelContainer + " does not contain mesh view " + meshView);
                }
            }
        }
    }

    /**
     * Isolate the given ModelGroup/Container. De-isolate all other modelgroups/containers.
     */
    private void isolateModelContainer(ModelContainer modelGroupOrContainer)
    {
        for (ModelContainer modelContainer : project.getAllModels())
        {
            excludedFromSelection.add(modelContainer);
        }
        Set<MeshView> meshViews = modelGroupOrContainer.descendentMeshViews();
        Set<ModelContainer> isolatedModelContainers = new HashSet<>();
        for (MeshView meshView : meshViews)
        {
            isolatedModelContainers.add((ModelContainer) meshView.getParent());
        }
        for (ModelContainer modelContainer : isolatedModelContainers)
        {
            excludedFromSelection.remove(modelContainer);
        }

        updateModelColoursForPositionModeAndTargetPrinter();

    }

    /**
     * Get the ancestor group that is selected. If no ancestor is selected then return null.
     */
    private ModelGroup getAncestorSelectedGroup(MeshView meshView)
    {
        Set<ModelContainer> selectedModelGroups
            = Lookup.getProjectGUIState(project).getSelectedModelContainers().getSelectedModelsSnapshot().
            stream().filter(mc -> mc instanceof ModelGroup).collect(Collectors.toSet());
        for (ModelContainer selectedModelGroup : selectedModelGroups)
        {
            if (selectedModelGroup.descendentMeshViews().contains(meshView))
            {
                return (ModelGroup) selectedModelGroup;
            }
        }
        return null;
    }

    private void updateModelColours()
    {
        for (ModelContainer model : loadedModels)
        {
            updateModelColour(model);
        }

        for (ModelContainer model : loadedModels)
        {
            for (MeshView meshView : model.descendentMeshViews())
            {
                if (excludedFromSelection.contains((ModelContainer) meshView.getParent()))
                {
                    Color color = ((PhongMaterial) meshView.getMaterial()).getDiffuseColor();
                    meshView.setMaterial(new PhongMaterial(color.grayscale()));
                }
            }
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

        boolean showMisplacedColour = applicationStatus.getMode() == ApplicationMode.LAYOUT;
        model.updateColour(colour0, colour1, showMisplacedColour);
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
                updateModelColoursForPositionModeAndTargetPrinter();
            });

        project.getExtruder1FilamentProperty().addListener(
            (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
            {
                updateModelColoursForPositionModeAndTargetPrinter();
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
                updateModelColoursForPositionModeAndTargetPrinter();
            });

        project.getPrinterSettings().getFilament1Property().addListener(
            (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
            {
                updateModelColoursForPositionModeAndTargetPrinter();
            });
        updateModelColours();
    }

    private boolean targetPrinterHasOneExtruder()
    {
        boolean extruder1IsFitted
            = Lookup.getSelectedPrinterProperty().get().extrudersProperty().get(
                1).isFittedProperty().get();
        return !extruder1IsFitted;
    }

    /**
     * If either the chosen filaments, x/y/z position , application mode or printer changes then
     * this must be called. In LAYOUT mode the filament colours should reflect the project filament
     * colours except if the position is off the bed then that overrides the project colours. In
     * SETTINGS mode the filament colours should reflect the project print settings filament
     * colours, taking into account the support type.
     */
    private void updateModelColoursForPositionModeAndTargetPrinter()
    {
        PrinterSettings printerSettings = project.getPrinterSettings();
        Printer selectedPrinter = Lookup.getSelectedPrinterProperty().get();

        if (applicationStatus.getMode() == ApplicationMode.SETTINGS)
        {
            if (printerSettings.getPrintSupportOverride()
                == SlicerParametersFile.SupportType.NO_SUPPORT
                || printerSettings.getPrintSupportOverride()
                == SlicerParametersFile.SupportType.OBJECT_MATERIAL)
            {
                extruder0Filament = project.getPrinterSettings().getFilament0();
                extruder1Filament = project.getPrinterSettings().getFilament1();
            } else
            {
                if (printerSettings.getPrintSupportOverride()
                    == SlicerParametersFile.SupportType.MATERIAL_1)
                {
                    extruder0Filament = project.getPrinterSettings().getFilament1();
                    extruder1Filament = project.getPrinterSettings().getFilament1();
                } else
                {
                    extruder0Filament = project.getPrinterSettings().getFilament0();
                    extruder1Filament = project.getPrinterSettings().getFilament0();
                }
            }
            if (selectedPrinter != null && targetPrinterHasOneExtruder())
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
        for (ModelContainer model : modelContainer.getModelsHoldingMeshViews())
        {
            updateModelColour(model);
        }
        collideModels();
    }

    @Override
    public void whenModelsRemoved(Set<ModelContainer> modelContainers)
    {
        models.getChildren().removeAll(modelContainers);
        collideModels();
    }

    @Override
    public void whenAutoLaidOut()
    {
        collideModels();
        updateModelColoursForPositionModeAndTargetPrinter();
    }

    @Override
    public void whenModelsTransformed(Set<ModelContainer> modelContainers)
    {
        collideModels();
        updateModelColoursForPositionModeAndTargetPrinter();
    }

    @Override
    public void whenModelChanged(ModelContainer modelContainer, String propertyName)
    {
        for (ModelContainer model : modelContainer.getModelsHoldingMeshViews())
        {
            updateModelColour(model);
        }
    }

    @Override
    public void whenPrinterSettingsChanged(PrinterSettings printerSettings)
    {
    }

}
