package celtech.coreUI.visualisation;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.undo.UndoableProject;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.modelcontrol.TranslateableTwoD;
import celtech.roboxbase.configuration.datafileaccessors.PrinterContainer;
import celtech.roboxbase.configuration.fileRepresentation.PrinterDefinitionFile;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.roboxbase.postprocessor.nouveau.nodes.GCodeEventNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.StylusLiftNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.StylusPlungeNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.StylusScribeNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.StylusSwivelNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.TravelNode;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.utils.threed.importers.svg.ShapeContainer;
import java.util.List;
import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 * 
 * The coordinate systems can be quite confusing.
 * In JFX, the origin is the top left corner of the screen.
 * X goes left to right, Y goes top to bottom and Z goes into the screen.
 * Thus, for a 3D shape X is width, Y is height and Z is depth.
 * 
 * For a 2D shape, X is width, Y is height. (There is no Z.)
 * This seems to be how it is drawn on the screen.
 * TODO - For the bed, the origin should be in the bottom left corner, and
 * Y should go from bottom to top.
 * 
 * When a 2D shape is placed on the 3D bed, the 2D Shapes X axis lies along the 3D beds X axis,
 * and the 2D shapes Y axis lies along the 3D beds Z axis, which is very confusing.
 * 
 * There is also scope for confusion because the TranslateableTwoD class translates x and z,
 * but the shape container subclass actually modifies x and y.
 * 
 * Curiously, the ScaleableTwoD scales x and y, not x and z.
 */
public class SVGViewManager extends Pane implements Project.ProjectChangesListener
{
    private final Stenographer steno = StenographerFactory.getStenographer(SVGViewManager.class.getName());
    private final Project project;
    private final UndoableProject undoableProject;
    private final ApplicationStatus applicationStatus = ApplicationStatus.getInstance();
    private final ProjectSelection projectSelection;
    private ObservableList<ProjectifiableThing> loadedModels;

    private Shape shapeBeingDragged = null;
    private Point2D lastDragPosition = null;
    private double mousePosX;
    private double mousePosY;
    private double mousePreviousX;
    private double mousePreviousY;
    
    private double fitScale = 1.0;
    private double minScale = 1.0;    
    private double xOffsetAtFitScale = 0.0;
    private double yOffsetAtFitScale = 0.0;

    private double bedWidth = 210;
    private double bedHeight = 150;
    private final double bedBorder = 10;
    private final Affine bedMirror = new Affine();

    private Group partsAndBed = new Group();
    private final Translate bedTranslate = new Translate();
    private final Scale bedScale = new Scale();
    private final Rectangle bed = new Rectangle(bedWidth, bedHeight);
    private final Group parts = new Group();

    private final ObjectProperty<DragMode> dragMode = new SimpleObjectProperty(DragMode.IDLE);
    private boolean justEnteredDragMode;

    public SVGViewManager(Project project)
    {
        //steno.info("SVGViewManager");
        this.project = project;
        this.undoableProject = new UndoableProject(project);

        this.setPickOnBounds(false);

        createBed();
        
        // In JFX, the origin is at the top left, with the X axis
        // pointing left to right and the Y axis pointing top to bottom:
        //
        // O--X-->--------------------------------------
        // Y                                           |
        // V                                           |
        // |                                           |
        // |                                           |
        // |                                           |
        // |                                           |
        // |                                           |
        // ---------------------------------------------
        
        // The following transform mirrors in Y, and moves the origin
        // to the bottom left.
        bedMirror.setMyy(-1.0);
        bedMirror.setTy(bedHeight);

        getChildren().add(partsAndBed);
        partsAndBed.getChildren().addAll(bed, parts);
        partsAndBed.getTransforms().addAll(bedTranslate, bedScale, bedMirror);
        // getTransforms().addAll(t, s, a) are applied in the order (x') = t s a (x)
        //                                                          (y')         (y)
        //partsAndBed.getTransforms().addAll(bedTranslate, bedScale);

        projectSelection = Lookup.getProjectGUIState(project).getProjectSelection();
        loadedModels = project.getTopLevelThings();
        applicationStatus.modeProperty().addListener(applicationModeListener);

        addEventHandler(MouseEvent.ANY, mouseEventHandler);
        addEventHandler(ZoomEvent.ANY, zoomEventHandler);
        addEventHandler(ScrollEvent.ANY, scrollEventHandler);
        setStyle("-fx-background-color: blue;");

        Lookup.getSelectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {
            @Override
            public void changed(ObservableValue<? extends Printer> ov, Printer t, Printer t1)
            {
                whenCurrentPrinterChanged(t1);
            }
        });
        whenCurrentPrinterChanged(Lookup.getSelectedPrinterProperty().get());

        this.widthProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (newValue.doubleValue() > 0.0)
                resizeBed();
        });

        this.heightProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (newValue.doubleValue() > 0.0)
                resizeBed();
        });

        /**
         * Listen for adding and removing of models from the project
         */
        project.addProjectChangesListener(this);

        for (ProjectifiableThing projectifiableThing : project.getAllModels())
        {
            if (projectifiableThing instanceof ShapeContainer)
            {
                ShapeContainer shape = (ShapeContainer)projectifiableThing;
                //steno.info("SVGViewManager::SVGViewManager - Bounds are " + shape.getBoundsInLocal());
                parts.getChildren().add(shape);
                shape.setBedReference(partsAndBed);
            }
        }
    }

    private void createBed()
    {
        //steno.info("createBed");
        bed.setFill(Color.ANTIQUEWHITE);
        // The bed is in mm units
    }

    private void calculateBedFitScale()
    {
        //steno.info("calculateBedFitScale");
        
        double viewAreaWidth = widthProperty().get();
        if (viewAreaWidth < 70)
            viewAreaWidth = 70;
        viewAreaWidth -= 2 * bedBorder;
        double viewAreaHeight = heightProperty().get();
        if (viewAreaHeight < 70)
            viewAreaHeight = 70;
        viewAreaHeight -= 2 * bedBorder;
        double displayAspect = viewAreaWidth / viewAreaHeight;
        double aspect = bedWidth / bedHeight;

        double newWidth = 0;
        double newHeight = 0;
        if (displayAspect >= aspect)
        {
            // Drive from height
            newWidth = viewAreaHeight * aspect;
            newHeight = viewAreaHeight;
        } else
        {
            //Drive from width
            newHeight = viewAreaWidth / aspect;
            newWidth = viewAreaWidth;
        }

        fitScale = newWidth / bedWidth;
        minScale = 0.1 * fitScale;

        xOffsetAtFitScale = ((viewAreaWidth - newWidth) / 2) + bedBorder;
        yOffsetAtFitScale = ((viewAreaHeight - newHeight) / 2) + bedBorder;

        //System.out.println("calculateBedFitScale");
        //System.out.println("  widthProperty = " +
        //                   Double.toString(widthProperty().get()) + 
        //                   ", heightProperty = " + 
        //                   Double.toString(heightProperty().get()));
        //System.out.println("  bed = (" +
        //                   Double.toString(bedWidth) + 
        //                   ", " + 
        //                   Double.toString(bedHeight) +
        //                   ")");
        //System.out.println("  viewArea = (" +
        //                   Double.toString(viewAreaWidth) + 
        //                   ", " + 
        //                   Double.toString(viewAreaHeight) + 
        //                   ")");
        //System.out.println("  fitScale = " +
        //                   Double.toString(fitScale));
        //System.out.println("  offsetAtFitScale = (" +
        //                   Double.toString(xOffsetAtFitScale) + 
        //                   ", " + 
        //                   Double.toString(yOffsetAtFitScale) + 
        //                   ")");
    }

    private void resizeBed()
    {
        // steno.info("resizeBed");
        calculateBedFitScale();
        
        bedScale.setX(fitScale);
        bedScale.setY(fitScale);

        bedTranslate.setX(xOffsetAtFitScale);
        bedTranslate.setY(yOffsetAtFitScale);

        //System.out.println("  originOnScreen = " + localToScreen(0,0));
        //System.out.println("  cornerOnScreen = " + localToScreen(widthProperty().get(),heightProperty().get()));
        //System.out.println("  bedOriginOnScreen = " + bed.localToScreen(0,0));
        //System.out.println("  bedCornerOnScreen = " + bed.localToScreen(bedWidth, bedHeight));

        notifyScreenExtentsChange();
    }

    @Override
    public void whenModelAdded(ProjectifiableThing projectifiableThing)
    {
        if (projectifiableThing instanceof ShapeContainer)
        {
            ShapeContainer shape = (ShapeContainer)projectifiableThing;
            parts.getChildren().add(shape);
            shape.setBedReference(partsAndBed);
            shape.setBedCentreOffsetTransform();
    //        projectifiableThing.shrinkToFitBed();
        }
    }

    @Override
    public void whenModelsRemoved(Set<ProjectifiableThing> projectifiableThing)
    {
        //steno.info("whenModelsRemoved");
        parts.getChildren().removeAll(projectifiableThing);
    }

    @Override
    public void whenAutoLaidOut()
    {
        //steno.info("whenAutoLaidOut");
    }

    @Override
    public void whenModelsTransformed(Set<ProjectifiableThing> projectifiableThing
    )
    {
        //steno.info("whenModelsTransformed");
    }

    @Override
    public void whenModelChanged(ProjectifiableThing modelContainer, String propertyName
    )
    {
        //steno.info("whenModelChanged");
    }

    @Override
    public void whenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings
    )
    {
        //steno.info("whenPrinterSettingsChanged");
    }

    private void debugPrintBounds(String message, Bounds b)
    {
        System.out.println(message +
                           "(" + 
                           Double.toString(b.getMinX()) + 
                           ", " + 
                           Double.toString(b.getMinY()) + 
                           ", " + 
                           Double.toString(b.getMaxX()) + 
                           ", " + 
                           Double.toString(b.getMaxY()) + 
                           ") [ " + 
                           Double.toString(b.getCenterX()) + 
                           ", " + 
                           Double.toString(b.getCenterY()) + 
                           "]");
    }
    
    private void debugPrintAllBounds(String message)
    {
        System.out.println(message);
        debugPrintBounds("  this L: ", getBoundsInLocal());
        debugPrintBounds("  this P: ", getBoundsInParent());
        debugPrintBounds("  partsAndBed L: ", partsAndBed.getBoundsInLocal());
        debugPrintBounds("  partsAndBed P: ", partsAndBed.getBoundsInParent());
        debugPrintBounds("  bed L: ", bed.getBoundsInLocal());
        debugPrintBounds("  bed P: ", bed.getBoundsInParent());
        System.out.println("  bed O: " + sceneToLocal(bed.localToScene(0.0, 0.0)));
        parts.getChildren().forEach(s ->
        {
            debugPrintBounds("    S L: ", s.getBoundsInLocal());
            debugPrintBounds("    S P: ", s.getBoundsInParent());
            if (s instanceof ShapeContainer)
            {
                ShapeContainer sc = (ShapeContainer)s;
                sc.debugPrintTransforms("    S: ");
            }
        });
    }
    
    private final EventHandler<MouseEvent> mouseEventHandler = event ->
    {
        mousePreviousX = mousePosX;
        mousePreviousY = mousePosY;
        mousePosX = event.getSceneX();
        mousePosY = event.getSceneY();

        //debugPrintAllBounds("mouseEvent");
        if (event.getEventType() == MouseEvent.MOUSE_PRESSED)
        {
            //steno.info("MouseEvent.MOUSE_PRESSED");
            if (event.isPrimaryButtonDown()
                    || event.isSecondaryButtonDown())
            {
                handleMouseSingleClickedEvent(event);
            }

        } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED)
        {
            //steno.info("MouseEvent.MOUSE_DRAGGED");
            dragShape(shapeBeingDragged, event);

        } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED)
        {
            //steno.info("MouseEvent.MOUSE_RELEASED");
            shapeBeingDragged = null;
            //steno.info("Setting DragMode to IDLE");
            dragMode.set(DragMode.IDLE);
        }
    };

    private final EventHandler<ZoomEvent> zoomEventHandler = event ->
    {
        //steno.info("Zoom event handler");
        if (!Double.isNaN(event.getZoomFactor()) && event.getZoomFactor() > 0.8
                && event.getZoomFactor() < 1.2)
        {
            double newScale = bedScale.getX() * event.getZoomFactor();

            bedScale.setX(newScale);
            bedScale.setY(newScale);
        }
    };

    private final EventHandler<ScrollEvent> scrollEventHandler = event ->
    {
        //steno.info("Scroll event handler");
        // What is the current position of the mouse in bed coordinates.
        mousePreviousX = mousePosX;
        mousePreviousY = mousePosY;
        mousePosX = event.getSceneX();
        mousePosY = event.getSceneY();

        double previousScale = bedScale.getX();
        double newScale = bedScale.getX() + (0.01 * event.getDeltaY());
        if (newScale < minScale)
            newScale = minScale;
        if (newScale != previousScale)
        {
            Point2D mousePaneLocal = sceneToLocal(mousePosX, mousePosY);
            Point2D mouseLocal = partsAndBed.sceneToLocal(mousePosX, mousePosY);
            double newOffsetX = mousePaneLocal.getX() - newScale * mouseLocal.getX();
            double newOffsetY = mousePaneLocal.getY() + newScale * (mouseLocal.getY() - bedHeight);
            bedScale.setX(newScale);
            bedScale.setY(newScale);
            bedTranslate.setX(newOffsetX);
            bedTranslate.setY(newOffsetY);

            // Adjust to ensure edges of bed do not creep around screen.
            Bounds parentBounds = getBoundsInLocal();
            Bounds pbBounds = partsAndBed.getBoundsInParent();
            double viewAreaWidth = widthProperty().get();
            double viewAreaHeight = heightProperty().get();
            double bedAreaWidth = newScale * bedWidth;
            double bedAreaHeight = newScale * bedHeight;
                    
            if (bedAreaWidth <= viewAreaWidth)
            {
                // Bed width is smaller than parent.
                // Centre in parent.
                newOffsetX = 0.5 * (viewAreaWidth - bedAreaWidth);
            }
            else if (newOffsetX < viewAreaWidth - bedAreaWidth)
            {
                // Bed width is larger than parent, but right edge is inside  parent.
                // Keep right edge at right of parent.
                newOffsetX = viewAreaWidth - bedAreaWidth;
            }
            else if (pbBounds.getMinX() > 0.0)
            {
                // Bed width is larger than parent, but left edge is inside  parent.
                // Keep left edge at left of parent.
                newOffsetX = 0.0;
            }
            
            if (bedAreaHeight <= viewAreaHeight)
            {
                // Bed height is smaller than parent.
                // Centre in parent.
                newOffsetY = 0.5 * (viewAreaHeight - bedAreaHeight);
            }
            else if (newOffsetY < viewAreaHeight - bedAreaHeight)
            {
                // Bed width is larger than parent, but bottom edge is inside  parent.
                // Keep bottom edge at bottom of parent.
                newOffsetY = viewAreaHeight - bedAreaHeight;
            }
            else if (newOffsetY > 0.0)
            {
                // Bed width is larger than parent, but top edge is inside  parent.
                // Keep top edge at top of parent.
                newOffsetY = 0.0;
            }
           
            bedTranslate.setX(newOffsetX);
            bedTranslate.setY(newOffsetY);
        }
        notifyScreenExtentsChange();
    };

    private void notifyScreenExtentsChange()
    {
        if (loadedModels != null)
            loadedModels.forEach(m -> m.notifyScreenExtentsChange());
    }
    
    private ShapeContainer findShapeContainerParent(Node shape)
    {
        //steno.info("findShapeContainerParent");
        ShapeContainer sc = null;
        Node currentNode = shape;

        while (currentNode != null)
        {
            if (currentNode instanceof ShapeContainer)
            {
                sc = (ShapeContainer) currentNode;
            }
            currentNode = currentNode.getParent();
        }

        return sc;
    }

    private void handleMouseSingleClickedEvent(MouseEvent event)
    {
        //steno.info("handleMouseSingleClickedEvent");
        boolean handleThisEvent = true;
        //steno.info("    source: " + event.getSource());
        //steno.info("    target: " + event.getTarget());
        PickResult pickResult = event.getPickResult();
        Point3D pickedPoint = pickResult.getIntersectedPoint();
        Node intersectedNode = pickResult.getIntersectedNode();
        //steno.info("   picked: " + intersectedNode + " @ " + pickedPoint);
                
        boolean shortcut = event.isShortcutDown();

        if (event.isPrimaryButtonDown())
        {
            //steno.info("    event.isPrimaryButtonDown");
            if (intersectedNode != bed
                    && intersectedNode instanceof Shape)
            {
                ShapeContainer sc = findShapeContainerParent(intersectedNode);
                if (sc != null)
                {
                    if (event.isShortcutDown())
                    {
                        projectSelection.addSelectedItem(sc);
                    } else
                    {
                        projectSelection.deselectAllModels();
                        projectSelection.addSelectedItem(sc);
                    }
                }
                //steno.info("    Setting drag mode to TRANSLATING");
                dragMode.set(DragMode.TRANSLATING);
                justEnteredDragMode = true;
                dragShape((Shape) intersectedNode, event);
            } else
            {
                projectSelection.deselectAllModels();
            }
        }
//        } else if (event.isSecondaryButtonDown())
//        {
////            intersectedNode.fireEvent(new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED,
////                    event.getX(), event.getY(),
////                    event.getScreenX(), event.getScreenY(),
////                    false,
////                    pickResult));
//        }
    }

    private void dragShape(Shape shapeToDrag, MouseEvent event)
    {
        //steno.info("dragShape");
        Point2D newPosition = new Point2D(event.getSceneX(), event.getSceneY());
        if (shapeBeingDragged != null)
        {
            //steno.info("New position = " + newPosition);
            Point2D resultantPosition = partsAndBed.sceneToLocal(newPosition).subtract(partsAndBed.sceneToLocal(lastDragPosition));
            //steno.info("Resultant " + resultantPosition);
//            if (shapeBeingDragged == bed)
//            {
//                bedTranslate.setX(bedTranslate.getX() + resultantPosition.getX());
//                bedTranslate.setY(bedTranslate.getY() + resultantPosition.getY());
//            } else
            if (shapeBeingDragged != bed) 
            {
                undoableProject.translateModelsBy(projectSelection.getSelectedModelsSnapshot(TranslateableTwoD.class), resultantPosition.getX(), resultantPosition.getY(),
                        !justEnteredDragMode);
            }

            justEnteredDragMode = false;
        }
        shapeBeingDragged = shapeToDrag;
        lastDragPosition = newPosition;
    }

    private final ChangeListener<ApplicationMode> applicationModeListener
            = (ObservableValue<? extends ApplicationMode> ov, ApplicationMode oldMode, ApplicationMode newMode) ->
            {
                //steno.info("applicationModeListener");
                if (oldMode != newMode)
                {
                    switch (newMode)
                    {
                        case SETTINGS:
                            removeEventHandler(MouseEvent.ANY, mouseEventHandler);
                            removeEventHandler(ZoomEvent.ANY, zoomEventHandler);
                            removeEventHandler(ScrollEvent.ANY, scrollEventHandler);
                            deselectAllModels();
                            break;
                        default:
                            addEventHandler(MouseEvent.ANY, mouseEventHandler);
                            addEventHandler(ZoomEvent.ANY, zoomEventHandler);
                            addEventHandler(ScrollEvent.ANY, scrollEventHandler);
                            break;
                    }
//                    updateModelColours();
                }
            };

    private void selectModel(ShapeContainer selectedNode, boolean multiSelect)
    {
        //steno.info("selectModel");
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
        //steno.info("deselectAllModels");
        for (ProjectifiableThing modelContainer : loadedModels)
        {
            deselectModel((ShapeContainer) modelContainer);
        }
    }

    public void deselectModel(ShapeContainer pickedModel)
    {
        //steno.info("deselectModel");
        if (pickedModel.isSelected())
        {
            projectSelection.removeModelContainer(pickedModel);
        }
    }
    
     public ReadOnlyObjectProperty<DragMode> getDragModeProperty()
    {
        return dragMode;
    }
     
    private void whenCurrentPrinterChanged(Printer printer)
    {
        PrinterDefinitionFile printerConfiguration = null;
        if (printer != null &&
            printer.printerConfigurationProperty().get() != null)
        {
            printerConfiguration = printer.printerConfigurationProperty().get();
        }
        else
        {
            printerConfiguration = PrinterContainer.getPrinterByID(PrinterContainer.defaultPrinterID);
        }
        deselectAllModels();

        bedWidth = printerConfiguration.getPrintVolumeWidth();
        bedHeight = printerConfiguration.getPrintVolumeDepth();
        bed.setWidth(bedWidth);
        bed.setHeight(bedHeight);
        
        // Bed mirror transform also translates by the bed height so the origin is at
        // the bottom left after the mirror.
        bedMirror.setTy(bedHeight);
        resizeBed();
    }
}
