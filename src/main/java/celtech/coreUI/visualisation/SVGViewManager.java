package celtech.coreUI.visualisation;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.undo.UndoableProject;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.modelcontrol.TranslateableTwoD;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.utils.threed.importers.svg.ShapeContainer;
import java.util.Set;
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
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class SVGViewManager extends Pane implements Project.ProjectChangesListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(SVGViewManager.class.getName());
    private final Project project;
    private final ApplicationStatus applicationStatus = ApplicationStatus.getInstance();
    private final ProjectSelection projectSelection;
    private ObservableList<ProjectifiableThing> loadedModels;

    private Point3D lastDragPosition;
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;

    private final Translate bedTranslate = new Translate();
    private final Scale bedScale = new Scale();
    private DoubleProperty scale = new SimpleDoubleProperty(1.0);
    private final Group bed = new Group();
    private final Group parts = new Group();

    private final ObjectProperty<DragMode> dragMode = new SimpleObjectProperty(DragMode.IDLE);
    private boolean justEnteredDragMode;

    private final UndoableProject undoableProject;

    public SVGViewManager(Project project)
    {
        this.project = project;
        this.undoableProject = new UndoableProject(project);

        bedScale.xProperty().bind(scale);
        bedScale.yProperty().bind(scale);

        createBed();

        getChildren().add(parts);
        parts.getTransforms().addAll(bedTranslate, bedScale);

        for (ProjectifiableThing projectifiableThing : project.getAllModels())
        {
            parts.getChildren().add(projectifiableThing);
        }

        projectSelection = Lookup.getProjectGUIState(project).getProjectSelection();
        loadedModels = project.getTopLevelThings();

        applicationStatus.modeProperty().addListener(applicationModeListener);

        addEventHandler(MouseEvent.ANY, mouseEventHandler);
        addEventHandler(ZoomEvent.ANY, zoomEventHandler);
        addEventHandler(ScrollEvent.ANY, scrollEventHandler);

        setStyle("-fx-background-color: blue;");

    }

    private void createBed()
    {
        // The bed is in mm units
        StackPane stack = new StackPane();
        Rectangle bedPartToDisplay = new Rectangle(210, 150);
        Label label = new Label("Bed");
        label.setTextFill(Color.BLACK);
        stack.getChildren().addAll(bedPartToDisplay, label);
        bedPartToDisplay.setFill(Color.AZURE);
        bed.getChildren().add(stack);
        bed.setMouseTransparent(true);
        bed.getTransforms().addAll(bedTranslate, bedScale);
        this.getChildren().add(bed);
    }

    @Override
    public void whenModelAdded(ProjectifiableThing projectifiableThing)
    {
        this.getChildren().add(projectifiableThing);
    }

    @Override
    public void whenModelsRemoved(Set<ProjectifiableThing> projectifiableThing)
    {
        this.getChildren().remove(projectifiableThing);
    }

    @Override
    public void whenAutoLaidOut()
    {
    }

    @Override
    public void whenModelsTransformed(Set<ProjectifiableThing> projectifiableThing)
    {
    }

    @Override
    public void whenModelChanged(ProjectifiableThing modelContainer, String propertyName)
    {
    }

    @Override
    public void whenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings)
    {
    }

    private final EventHandler<MouseEvent> mouseEventHandler = event ->
    {

        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = event.getSceneX();
        mousePosY = event.getSceneY();

        if (event.getEventType() == MouseEvent.MOUSE_PRESSED)
        {
            if (event.isPrimaryButtonDown()
                    || event.isSecondaryButtonDown())
            {
                handleMouseSingleClickedEvent(event);
            }

        } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED)
        {
            handleMouseDragEvent(event);

        } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED)
        {
            lastDragPosition = null;
            dragMode.set(DragMode.IDLE);
        }
    };

    private final EventHandler<ZoomEvent> zoomEventHandler = event ->
    {
        if (!Double.isNaN(event.getZoomFactor()) && event.getZoomFactor() > 0.8
                && event.getZoomFactor() < 1.2)
        {
            scale.set(scale.get() * event.getZoomFactor());
        }
    };

    private final EventHandler<ScrollEvent> scrollEventHandler = event ->
    {
        bedTranslate.setX(bedTranslate.getTx() + (0.01 * event.getDeltaX()));
        bedTranslate.setY(bedTranslate.getTy() + (0.01 * event.getDeltaY()));
    };

    private void handleMouseSingleClickedEvent(MouseEvent event)
    {
        boolean handleThisEvent = true;
        PickResult pickResult = event.getPickResult();
        Point3D pickedPoint = pickResult.getIntersectedPoint();
        Node intersectedNode = pickResult.getIntersectedNode();

        boolean shortcut = event.isShortcutDown();

        if (event.isPrimaryButtonDown())
        {
            if (intersectedNode instanceof Shape)
            {
                steno.info("Picked: " + intersectedNode + " at " + pickedPoint);
                dragMode.set(DragMode.TRANSLATING);
                justEnteredDragMode = true;
                doSelectTranslateModel(intersectedNode, pickedPoint, event, true);
            }
        }
    }

    private void handleMouseDragEvent(MouseEvent event)
    {

        double mouseDeltaX = (mousePosX - mouseOldX);
        double mouseDeltaY = (mousePosY - mouseOldY);

        if (event.isSecondaryButtonDown())
        {
//            Node intersectedNode = event.getPickResult().getIntersectedNode();
//            Point3D pickedPoint = event.getPickResult().getIntersectedPoint();
//            Point3D pickedScenePoint = intersectedNode.localToScene(pickedPoint);
//            Point3D pickedBedTranslateXformPoint = sc
//
            bedTranslate.setX(bedTranslate.getX() + mouseDeltaX);
            bedTranslate.setY(bedTranslate.getY() + mouseDeltaY);
        } else if (event.isPrimaryButtonDown() && dragMode.get() == DragMode.TRANSLATING)
        {
            undoableProject.translateModelsBy(projectSelection.getSelectedModelsSnapshot(TranslateableTwoD.class), mouseDeltaX, mouseDeltaY,
                    !justEnteredDragMode);
                                justEnteredDragMode = false;
        }

//        if (event.isPrimaryButtonDown())
//        {
//            Node intersectedNode = event.getPickResult().getIntersectedNode();
//            //Move the model!
//            if (intersectedNode == translationDragPlane)
//            {
//                Point3D pickedPoint = event.getPickResult().getIntersectedPoint();
//                Point3D pickedScenePoint = intersectedNode.localToScene(pickedPoint);
//                Point3D pickedBedTranslateXformPoint = bedTranslateXform.sceneToLocal(
//                        pickedScenePoint);
//
////                translationDragPlane.setTranslateY(pickedBedTranslateXformPoint.getY());
//                Point3D pickedDragPlanePoint = translationDragPlane.sceneToLocal(pickedScenePoint);
//
//                if (lastDragPosition != null)
//                {
//                    Point3D resultant = pickedDragPlanePoint.subtract(lastDragPosition);
//                    translateSelection(resultant.getX(), resultant.getZ());
//                    justEnteredDragMode = false;
//                }
//                lastDragPosition = pickedDragPlanePoint;
//            }
////            else
////            {
////                steno.error(
////                    "In translation drag mode but intersected with something other than translation drag plane");
////            }
//        }
    }

    private final ChangeListener<ApplicationMode> applicationModeListener
            = (ObservableValue<? extends ApplicationMode> ov, ApplicationMode oldMode, ApplicationMode newMode) ->
            {
                if (oldMode != newMode)
                {
                    switch (newMode)
                    {
                        case SETTINGS:
                            removeEventHandler(MouseEvent.ANY, mouseEventHandler);
                            deselectAllModels();
                            break;
                        default:
                            addEventHandler(MouseEvent.ANY, mouseEventHandler);
//                            subScene.addEventHandler(ZoomEvent.ANY, zoomEventHandler);
//                            subScene.addEventHandler(ScrollEvent.ANY, scrollEventHandler);
                            break;
                    }
//                    updateModelColours();
                }
            };

    private void selectModel(ShapeContainer selectedNode, boolean multiSelect)
    {
        if (selectedNode == null)
        {
            projectSelection.deselectAllModels();
        } else if (selectedNode.isSelected() == false)
        {
            if (multiSelect == false)
            {
                projectSelection.deselectAllModels();
            }
            projectSelection.addSelectedItem(selectedNode);
        }
    }

    private void deselectAllModels()
    {
        for (ProjectifiableThing modelContainer : loadedModels)
        {
            deselectModel((ShapeContainer) modelContainer);
        }
    }

    public void deselectModel(ShapeContainer pickedModel)
    {
        if (pickedModel.isSelected())
        {
            projectSelection.removeModelContainer(pickedModel);
        }
    }

    private void doSelectTranslateModel(Node intersectedNode, Point3D pickedPoint, MouseEvent event, boolean findParentGroup)
    {
        lastDragPosition = pickedPoint;

        Parent parent = intersectedNode.getParent();
        if (!(parent instanceof ShapeContainer))
        {
            parent = parent.getParent();
        }

        ShapeContainer pickedModel = (ShapeContainer) parent;
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
}
