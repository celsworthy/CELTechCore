/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.coreUI.visualisation.DragMode;
import celtech.coreUI.visualisation.SelectionContainer;
import celtech.coreUI.visualisation.ThreeDViewManager;
import celtech.coreUI.visualisation.Xform;
import celtech.coreUI.visualisation.modelDisplay.GizmoMode;
import celtech.modelcontrol.ModelContainer;
import celtech.utils.Math.MathUtils;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Path;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class GizmoOverlayController implements Initializable
{
    
    private Stenographer steno = StenographerFactory.getStenographer(GizmoOverlayController.class.getName());
    
    @FXML
    private AnchorPane gizmoGroup;
    
    @FXML
    private Arc rotationArc;
    
    @FXML
    private Button rotationRing;
    
    @FXML
    private SVGPath rotationPath;
    
    @FXML
    private Button xHandleButton;
    
    @FXML
    private Button zHandleButton;
    
    @FXML
    private Button xzHandleButton;
    
    @FXML
    private Text rotationAngleText;
    
    private Xform parentXform = null;
    private AnchorPane base = null;
    
    Robot robot = null;
    
    @FXML
    void xHandlePressed(MouseEvent event)
    {
                steno.info("got event " + event);

//        xHandleButton.setMouseTransparent(true);
        
//        steno.info("got event " + event);
//        double screenX = event.getScreenX();
//        double screenY = event.getScreenY();
//        steno.info("Screen " + screenX + ":" + screenY);
//        
//        Point2D baseX = base.screenToLocal(screenX, screenY);
//        steno.info("Base " + baseX);
//        
//        Point3D parentX = parentXform.parentToLocal(baseX.getX(), baseX.getY(), 0);
//        steno.info("Parent " + parentX);

        
//        if (xHandleButton.contains(parentX))
//        {
//            steno.info("Contained");
//        }
//        else
//        {
//            steno.info("Not contained");
//        }
//        if (viewManager.getDragMode() == false)
//        {
//            steno.info("Fired press");
//            Platform.runLater(new Runnable()
//            {
//
//                @Override
//                public void run()
//                {
//            gizmoGroup.setMouseTransparent(true);
//            
//            viewManager.setDragMode(true);
//            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//                }
//            });
//        }
//        viewManager.checkit(event.getScreenX(), event.getScreenY());
        enterTranslateMode(GizmoMode.XTRANSLATE, event.getScreenX(), event.getScreenY());
    }

        @FXML
    void xHandleDragDetected(MouseEvent event)
    {
        steno.info("got event " + event);
//                xHandleButton.startFullDrag();
    }
    
    @FXML
    void xHandleDragged(MouseEvent event)
    {
        steno.info("got event " + event);
    
//        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);

        translate(event.getScreenX(), event.getScreenY());
    }
    
    @FXML
    void xHandleReleased(MouseEvent event)
    {
        steno.info("got event " + event);
//        xHandleButton.setMouseTransparent(false);
//        if (viewManager.getDragMode() == true)
//        {
//            steno.info("Fired release");
//            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
//            gizmoGroup.setMouseTransparent(false);
//            viewManager.setDragMode(false);
//        }
        exitTranslateMode();
    }
    
    @FXML
    void zHandlePressed(MouseEvent event)
    {
//        enterTranslateMode(GizmoMode.ZTRANSLATE, event.getX(), event.getY());
    }
    
    @FXML
    void zHandleDragged(MouseEvent event)
    {
//        translate(event.getX(), event.getY());
    }
    
    @FXML
    void zHandleReleased(MouseEvent event)
    {
//        exitTranslateMode();
    }
    
    @FXML
    void xzHandlePressed(MouseEvent event)
    {
//        enterTranslateMode(GizmoMode.XZTRANSLATE, event.getX(), event.getY());
    }
    
    @FXML
    void xzHandleDragged(MouseEvent event)
    {
//        translate(event.getX(), event.getY());
    }
    
    @FXML
    void xzHandleReleased(MouseEvent event)
    {
//        exitTranslateMode();
    }
    
    @FXML
    void rotationRingPressed(MouseEvent event)
    {
        Point3D intersectedPoint = event.getPickResult().getIntersectedPoint();
        enterRotateMode(intersectedPoint.getX(), intersectedPoint.getY());
    }
    
    @FXML
    void rotationRingDragged(MouseEvent event)
    {
        steno.info("Event: X" + event.getSceneX() + ":" + event.getSceneY());
        if (event.getPickResult().getIntersectedNode() == rotationRing || event.getPickResult().getIntersectedNode() == rotationPath)
        {
            Point3D intersectedPoint = event.getPickResult().getIntersectedPoint();
            rotate(event.isShiftDown(), intersectedPoint.getX(), intersectedPoint.getY());
        }
    }
    
    @FXML
    void rotationRingReleased(MouseEvent event)
    {
        exitRotateMode();
    }
    
    private Xform xHandleXform = new Xform();
    private Shape rotationDisc = null;
    private ObjectProperty<GizmoMode> modeProperty = new SimpleObjectProperty<>(GizmoMode.IDLE);
    
    private DoubleProperty rotationStartAngle = new SimpleDoubleProperty(0);
    private DoubleProperty rotationDelta = new SimpleDoubleProperty(0);
    private double lastRotationSent = 0;
    private DoubleProperty rotationTextX = new SimpleDoubleProperty(0);
    private DoubleProperty rotationTextY = new SimpleDoubleProperty(0);
    private double modelStartingAngle = 0;
    
    private Point2D translateStartPoint = null;
    private double modelStartingX = 0;
    private double modelStartingZ = 0;
    
    private ThreeDViewManager viewManager = null;
    private ObservableList<ModelContainer> loadedModels = null;
    private SelectionContainer selectionContainer = null;
    
    private double xoffset = 175;
    private double yoffset = 350;
    private double scaleStartPointY;
    
    private InnerShadow unselectedControlEffect = new InnerShadow(BlurType.THREE_PASS_BOX, Color.BLACK, 10, 0, 0, 0);
    private InnerShadow selectedControlEffect = new InnerShadow(BlurType.THREE_PASS_BOX, Color.BLACK, 10, 0.6, 0, 0);

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try
        {
            robot = new Robot();
        } catch (AWTException ex)
        {
            steno.error("Error creating robot for gizmo");
        }
        
        rotationArc.visibleProperty().bind(modeProperty.isEqualTo(GizmoMode.ROTATE));
        rotationArc.setStartAngle(0);
        rotationArc.setLength(0);
        
        rotationStartAngle.addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                double angle = MathUtils.boundAzimuthDegrees(90 - t1.doubleValue());
                rotationArc.setStartAngle(angle);
//                steno.info("Starting angle " + angle);
            }
        });
        
        rotationDelta.addListener(new ChangeListener<Number>()
        {
            
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                rotationArc.setLength(-t1.doubleValue());
//                steno.info("Start " + modelStartingAngle + " delta " + t1.doubleValue());
                if (selectionContainer.selectedModelsProperty().size() == 1)
                {
                    viewManager.rotateSelection(-modelStartingAngle + t1.doubleValue());
                } else
                {
                    steno.info("Rotate val = " + t1.doubleValue() + " last:" + lastRotationSent);
                    double sentValue = t1.doubleValue() - lastRotationSent;
                    viewManager.rotateSelection(-sentValue);
                    lastRotationSent = t1.doubleValue();
                }
            }
        });
        
        rotationAngleText.translateXProperty().bind(rotationTextX);
        rotationAngleText.translateYProperty().bind(rotationTextY);
    }
    
    public void configure(ThreeDViewManager viewManager)
    {
        this.viewManager = viewManager;
        this.loadedModels = viewManager.getLoadedModels();
        this.selectionContainer = viewManager.getSelectionContainer();
        
        rotationAngleText.setVisible(false);
        
        gizmoGroup.visibleProperty().bind(Bindings.isNotEmpty(selectionContainer.selectedModelsProperty()));
        
        viewManager.dragModeProperty().addListener(new ChangeListener<DragMode>()
        {
            @Override
            public void changed(ObservableValue<? extends DragMode> observable, DragMode oldValue, DragMode newValue)
            {
                gizmoGroup.setMouseTransparent(false);
            }
        });
        
    }
    
    private void enterRotateMode(double xPos, double yPos)
    {
//        rotationDisc.setEffect(selectedControlEffect);
//        viewManager.getSubScene().setCursor(Cursor.HAND);

        steno.info("<<<<<<<<<<<<<<<<<");
        steno.info("got " + xPos + ":" + yPos);
        xPos -= xoffset;
        yPos -= yoffset;
        
        modelStartingAngle = selectionContainer.getRotationX();
        
        modeProperty.set(GizmoMode.ROTATE);
        steno.info(">>>>>>>>>>>>>>>>>");
        steno.info("got " + xPos + ":" + yPos);
        steno.info("Angle " + MathUtils.cartesianToAngleDegreesCWFromTop(xPos, yPos));
        rotationStartAngle.set(MathUtils.cartesianToAngleDegreesCWFromTop(xPos, yPos));
        rotationArc.setLength(.01);
        
        lastRotationSent = 0;
        
        Point2D newTextPosition = MathUtils.angleDegreesToCartesianCWFromTop(180 + rotationStartAngle.doubleValue(), 50);
        
        rotationTextX.set(newTextPosition.getX());
        rotationTextY.set(newTextPosition.getY());
        rotationAngleText.setText("0º");
    }
    
    private void exitRotateMode()
    {
        modeProperty.set(GizmoMode.IDLE);
//        rotationDisc.setEffect(unselectedControlEffect);
//        viewManager.getSubScene().setCursor(Cursor.DEFAULT);

        rotationAngleText.textProperty().unbind();
        rotationAngleText.setVisible(false);
        
    }
    
    private void rotate(boolean shiftDown, double xPos, double yPos)
    {
        if (modeProperty.get() == GizmoMode.ROTATE)
        {
            rotationAngleText.textProperty().bind(rotationDelta.asString("%.0fº"));
            rotationAngleText.setVisible(true);
            
            xPos -= xoffset;
            yPos -= yoffset;
            
            double newAngle = MathUtils.cartesianToAngleDegreesCWFromTop(xPos, yPos);
            
            double resultantAngle = newAngle - rotationStartAngle.doubleValue();
            resultantAngle = ((int) resultantAngle / 10) * 10;
            if (resultantAngle > 180)
            {
                resultantAngle -= 360;
            } else if (resultantAngle < -180)
            {
                resultantAngle += 360;
            }
            
            steno.info("new " + newAngle + " start " + rotationStartAngle.doubleValue() + " Result " + resultantAngle);
            rotationDelta.set(resultantAngle);
            Point2D newTextPosition = MathUtils.angleDegreesToCartesianCWFromTop(180 - rotationStartAngle.doubleValue() - (rotationDelta.doubleValue() / 2), 50);
            
            rotationTextX.set(newTextPosition.getX());
            rotationTextY.set(newTextPosition.getY());
        }
    }
    
    private void enterTranslateMode(GizmoMode mode, double xPos, double yPos)
    {
        modeProperty.set(mode);
        modelStartingX = selectionContainer.getCentreX();
        modelStartingZ = selectionContainer.getCentreZ();
        
        translateStartPoint = gizmoGroup.localToScreen(xPos, yPos);
        steno.info("Got screen coords of " + translateStartPoint);
        
//        viewManager.setDragMode(true);
//        gizmoGroup.setMouseTransparent(true);

//        viewManager.getSubScene().setCursor(Cursor.HAND);
    }
    
    private void exitTranslateMode()
    {
//        viewManager.getSubScene().setCursor(Cursor.DEFAULT);

        modeProperty.set(GizmoMode.IDLE);
    }
    
    private void translate(double xPos, double yPos)
    {
        Point2D screenCoords = gizmoGroup.localToScreen(xPos, yPos);
        steno.info("Got screen coords of " + screenCoords);

        viewManager.translateSelectionFromScreenCoords(translateStartPoint, screenCoords);
        translateStartPoint = screenCoords;
    }
    
    private void enterScaleMode(double xPos, double yPos)
    {
        modeProperty.set(GizmoMode.SCALE);
        
        scaleStartPointY = yPos;
//        viewManager.getSubScene().setCursor(Cursor.HAND);
//        scaleHandle.setEffect(selectedControlEffect);
    }
    
    private void exitScaleMode()
    {
        modeProperty.set(GizmoMode.IDLE);
//        viewManager.getSubScene().setCursor(Cursor.DEFAULT);
//        scaleHandle.setEffect(unselectedControlEffect);
    }
    
    private void scale(double xPos, double yPos)
    {
        double modifier = 0.01;
        viewManager.deltaScaleSelection(1 + ((scaleStartPointY - yPos) * modifier));
        
        scaleStartPointY = yPos;
    }
    
    public void wasXHandleHit(double screenX, double screenY)
    {
        steno.info(xHandleButton.getBoundsInParent() + ":" + xHandleButton.getBoundsInParent());
        Point2D groupPoint = xHandleButton.screenToLocal(screenX, screenY);
//        Point2D scenePoint = gizmoGroup.localToScene(groupPoint);
//        xHandleButton.c
        if (xHandleButton.contains(groupPoint))
        {
            steno.info("A veritable hit (1)");
        }
//        if (xHandleButton.contains(scenePoint))
//        {
//            steno.info("A veritable hit (2)");
//        }
    }

    public void setXform(Xform gizmoXform)
    {
        parentXform = gizmoXform;
    }

    public void setBase(AnchorPane basePane)
    {
        base = basePane;
    }
}
