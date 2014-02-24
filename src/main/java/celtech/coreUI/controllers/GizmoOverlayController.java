/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.coreUI.visualisation.PolarCamera;
import celtech.coreUI.visualisation.SelectionContainer;
import celtech.coreUI.visualisation.ThreeDViewManager;
import celtech.coreUI.visualisation.Xform;
import celtech.coreUI.visualisation.modelDisplay.GizmoMode;
import celtech.modelcontrol.ModelContainer;
import celtech.utils.Math.MathUtils;
import java.net.URL;
import java.util.ResourceBundle;
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
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Path;
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
    private Circle innerCircle;
    
    @FXML
    private Circle outerCircle;
    
    @FXML
    private Arc rotationArc;
    
    @FXML
    private Path xHandle;
    
    @FXML
    private Path zHandle;
    
    @FXML
    private Arc xzHandle;
    
    @FXML
    private Text rotationAngleText;
    
    private Xform xHandleXform = new Xform();
    private Shape rotationDisc = null;
    private ObjectProperty<GizmoMode> modeProperty = new SimpleObjectProperty<>(GizmoMode.IDLE);
    
    private DoubleProperty rotationStartAngle = new SimpleDoubleProperty(0);
    private DoubleProperty rotationDelta = new SimpleDoubleProperty(0);
    private double lastRotationSent = 0;
    private DoubleProperty rotationTextX = new SimpleDoubleProperty(0);
    private DoubleProperty rotationTextY = new SimpleDoubleProperty(0);
    private double modelStartingAngle = 0;
    
    private double translateStartPointX = 0;
    private double translateStartPointZ = 0;
    private double modelStartingX = 0;
    private double modelStartingZ = 0;
    
    private ThreeDViewManager viewManager = null;
    private ObservableList<ModelContainer> loadedModels = null;
    private SelectionContainer selectionContainer = null;
    
    private double radius = 0;
    private double scaleStartPointY;
    
    private InnerShadow unselectedControlEffect = new InnerShadow(BlurType.THREE_PASS_BOX, Color.BLACK, 10, 0, 0, 0);
    private InnerShadow selectedControlEffect = new InnerShadow(BlurType.THREE_PASS_BOX, Color.BLACK, 10, 0.6, 0, 0);
    
    private Color rotationDiscDefaultColour = Color.LIGHTGREY;
    private Color rotationDiscPressedColour = Color.web("#404040");
    private Color xArrowDefaultColour = Color.RED;
    private Color xArrowPressedColour = Color.DARKRED;
    private Color zArrowDefaultColour = Color.web("#10cc00");
    private Color zArrowPressedColour = Color.web("#0c9500");
    private Color rotationArcDefaultColour = Color.web("#99a9ff");
    private Color rotationArcPressedColour = Color.web("#545d8c");
    private Color xzArcDefaultColour = Color.web("#7266ff");
    private Color xzArcPressedColour = Color.web("#3f398d");

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        radius = outerCircle.getRadius();
        
        rotationDisc = Path.subtract(outerCircle, innerCircle);
        rotationDisc.setFill(rotationDiscDefaultColour);
        rotationDisc.setTranslateX(radius);
        rotationDisc.setTranslateY(radius);
        
        gizmoGroup.getChildren().removeAll(outerCircle, innerCircle);
        
        xHandle.setFill(xArrowDefaultColour);
        zHandle.setFill(zArrowDefaultColour);
        xzHandle.setFill(xzArcDefaultColour);
        rotationArc.setFill(rotationArcDefaultColour);
        
//        xHandle.setTranslateX(-radius);
//        xHandle.setTranslateY(-radius);
//        xzHandle.setTranslateX(-radius);
//        xzHandle.setTranslateY(-radius);
//        zHandle.setTranslateX(-radius);
//        zHandle.setTranslateY(-radius);
//        xHandleXform.setTranslate(radius, radius);
//        xHandleXform.getChildren().addAll(xHandle, xzHandle, zHandle);
        
        gizmoGroup.getChildren().add(0, rotationDisc);
        
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
                    viewManager.rotateSelection(modelStartingAngle + t1.doubleValue());
                } else
                {
                    steno.info("Rotate val = " + t1.doubleValue() + " last:" + lastRotationSent);
                    double sentValue = t1.doubleValue() - lastRotationSent;
                    viewManager.rotateSelection(sentValue);
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
        PolarCamera camera = viewManager.getCamera();
        
        rotationAngleText.setVisible(false);
        
        gizmoGroup.visibleProperty().bind(Bindings.isNotEmpty(selectionContainer.selectedModelsProperty()));
        
        xHandleXform.setRz(camera.getCameraAzimuthRadians() * MathUtils.RAD_TO_DEG);
        
        camera.cameraAzimuthRadiansProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                xHandleXform.setRz(t1.doubleValue() * MathUtils.RAD_TO_DEG);
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
                rotate(t.isShiftDown(), t.getX(), t.getY());
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
        
        xHandle.setOnMousePressed(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent t)
            {
                enterTranslateMode(GizmoMode.XTRANSLATE, t.getX(), t.getY());
            }
        });
        
        xHandle.setOnMouseDragged(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent t)
            {
                translate(t.getX(), t.getY());
            }
        });
        
        xHandle.setOnMouseReleased(new EventHandler<MouseEvent>()
        {
            
            @Override
            public void handle(MouseEvent t)
            {
                exitTranslateMode();
            }
        });
        
        zHandle.setOnMousePressed(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent t)
            {
                enterTranslateMode(GizmoMode.ZTRANSLATE, t.getX(), t.getY());
            }
        });
        
        zHandle.setOnMouseDragged(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent t)
            {
                translate(t.getX(), t.getY());
            }
        });
        
        zHandle.setOnMouseReleased(new EventHandler<MouseEvent>()
        {
            
            @Override
            public void handle(MouseEvent t)
            {
                exitTranslateMode();
            }
        });
        
        xzHandle.setOnMousePressed(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent t)
            {
                enterTranslateMode(GizmoMode.XZTRANSLATE, t.getX(), t.getY());
            }
        });
        
        xzHandle.setOnMouseDragged(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent t)
            {
                translate(t.getX(), t.getY());
            }
        });
        
        xzHandle.setOnMouseReleased(new EventHandler<MouseEvent>()
        {
            
            @Override
            public void handle(MouseEvent t)
            {
                exitTranslateMode();
            }
        });
    }
    
    private void enterRotateMode(double xPos, double yPos)
    {
//        rotationDisc.setEffect(selectedControlEffect);
        rotationDisc.setFill(rotationDiscPressedColour);
        viewManager.getSubScene().setCursor(Cursor.HAND);
        
        xPos -= radius;
        yPos -= radius;
        
        modelStartingAngle = selectionContainer.getRotationX();
        
        modeProperty.set(GizmoMode.ROTATE);
//        steno.info("got " + xPos + ":" + yPos);
//        steno.info("Angle " + MathUtils.cartesianToAngleDegreesCWFromTop(xPos, yPos));
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
        rotationDisc.setFill(rotationDiscDefaultColour);
//        rotationDisc.setEffect(unselectedControlEffect);
        viewManager.getSubScene().setCursor(Cursor.DEFAULT);
        
        rotationAngleText.textProperty().unbind();
        rotationAngleText.setVisible(false);
        
    }
    
    private void rotate(boolean shiftDown, double xPos, double yPos)
    {
        if (modeProperty.get() == GizmoMode.ROTATE)
        {
            rotationAngleText.textProperty().bind(rotationDelta.asString("%.0fº"));
            rotationAngleText.setVisible(true);
            
            xPos -= radius;
            yPos -= radius;
            
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

//            steno.info("new " + newAngle + " start " + rotationStartAngle.doubleValue() + " Result " + resultantAngle);
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
        
        translateStartPointX = xPos;
        translateStartPointZ = yPos;
        
        viewManager.getSubScene().setCursor(Cursor.HAND);
        
        switch (mode)
        {
            case XTRANSLATE:
                xHandle.setFill(xArrowPressedColour);
//                xHandle.setEffect(selectedControlEffect);
                break;
            case ZTRANSLATE:
                zHandle.setFill(zArrowPressedColour);
//                zHandle.setEffect(selectedControlEffect);
                break;
            case XZTRANSLATE:
                xzHandle.setFill(xzArcPressedColour);
//                xzHandle.setEffect(selectedControlEffect);
                break;
        }
    }
    
    private void exitTranslateMode()
    {
        viewManager.getSubScene().setCursor(Cursor.DEFAULT);
        
        modeProperty.set(GizmoMode.IDLE);
        xHandle.setFill(xArrowDefaultColour);
        zHandle.setFill(zArrowDefaultColour);
        xzHandle.setFill(xzArcDefaultColour);
//        xHandle.setEffect(unselectedControlEffect);
//        zHandle.setEffect(unselectedControlEffect);
//        xzHandle.setEffect(unselectedControlEffect);
    }
    
    private void translate(double xPos, double yPos)
    {
        double resultantX = 0;
        double resultantZ = 0;
        double modifier = 0.1;
        
        switch (modeProperty.get())
        {
            case XTRANSLATE:
                resultantX = xPos - translateStartPointX;
                break;
            case ZTRANSLATE:
                resultantZ = translateStartPointZ - yPos;
                break;
            case XZTRANSLATE:
                resultantX = xPos - translateStartPointX;
                resultantZ = translateStartPointZ - yPos;
                break;
        }
        
        viewManager.translateSelection(resultantX * modifier, resultantZ * modifier);
        
        translateStartPointX = xPos;
        translateStartPointZ = yPos;
    }
    
    private void enterScaleMode(double xPos, double yPos)
    {
        modeProperty.set(GizmoMode.SCALE);
        
        scaleStartPointY = yPos;
        viewManager.getSubScene().setCursor(Cursor.HAND);
//        scaleHandle.setEffect(selectedControlEffect);
    }
    
    private void exitScaleMode()
    {
        modeProperty.set(GizmoMode.IDLE);
        viewManager.getSubScene().setCursor(Cursor.DEFAULT);
//        scaleHandle.setEffect(unselectedControlEffect);
    }
    
    private void scale(double xPos, double yPos)
    {
        double modifier = 0.01;
        viewManager.deltaScaleSelection(1 + ((scaleStartPointY - yPos) * modifier));
        
        scaleStartPointY = yPos;
    }

    private void centreChanged()
    {
//        steno.info("Before " + centreX + ":" + centreZ);
//
//        Point2D translate = ringXform.sceneToLocal(newCentre.getX(), newCentre.getY());
//        steno.info("After " + translate.getX() + " Y" + translate.getY());
//        me.setTranslateX(newCentre.getX());
//        me.setTranslateY(newCentre.getY());
    }
}
