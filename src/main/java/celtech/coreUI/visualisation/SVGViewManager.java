package celtech.coreUI.visualisation;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.utils.threed.importers.svg.RenderableSVG;
import java.util.Set;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Pane;
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

    public SVGViewManager(Project project)
    {
        this.project = project;

        for (ProjectifiableThing projectifiableThing : project.getAllModels())
        {
            this.getChildren().add(projectifiableThing);
        }

        projectSelection = Lookup.getProjectGUIState(project).getProjectSelection();
        loadedModels = project.getTopLevelThings();

        applicationStatus.modeProperty().addListener(applicationModeListener);

        addEventHandler(MouseEvent.ANY, mouseEventHandler);
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
        }
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
            steno.info("Picked: " + intersectedNode + " at " + pickedPoint);
            doSelectTranslateModel(intersectedNode, pickedPoint, event, true);
        }
    }

    private void handleMouseDragEvent(MouseEvent event)
    {

        double mouseDeltaX = (mousePosX - mouseOldX);
        double mouseDeltaY = (mousePosY - mouseOldY);

        boolean shortcut = event.isShortcutDown();

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

    private void selectModel(RenderableSVG selectedNode, boolean multiSelect)
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
            deselectModel((RenderableSVG) modelContainer);
        }
    }

    public void deselectModel(RenderableSVG pickedModel)
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
        if (!(parent instanceof RenderableSVG))
        {
            parent = parent.getParent();
        }

        RenderableSVG pickedModel = (RenderableSVG) parent;
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
