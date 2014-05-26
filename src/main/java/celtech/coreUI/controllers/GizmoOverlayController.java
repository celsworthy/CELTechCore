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
import celtech.modelcontrol.ModelContainer;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point3D;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.SVGPath;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class GizmoOverlayController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(GizmoOverlayController.class.getName());

    @FXML
    private AnchorPane gizmoGroup;

    private final Line startRotationLine = new Line();
    private final Xform startRotationLineXform = new Xform();
    private final Line finishRotationLine = new Line();
    private final Xform finishRotationLineXform = new Xform();

    @FXML
    private Button rotationRing;

    @FXML
    private Button xHandleButton;

    @FXML
    private Button zHandleButton;

    private ThreeDViewManager viewManager = null;
    private ObservableList<ModelContainer> loadedModels = null;
    private SelectionContainer selectionContainer = null;

    private boolean rotationStarted = false;
    private double rotationStartedAt = 0;

    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        startRotationLine.setStartX(0);
        startRotationLine.setStartY(0);
        startRotationLine.setEndX(0);
        startRotationLine.setEndY(-170);
        startRotationLine.setStroke(Color.WHITE);
        startRotationLine.setStrokeWidth(3);
        startRotationLine.setMouseTransparent(true);
        
        startRotationLineXform.getChildren().add(startRotationLine);
        startRotationLineXform.setTx(175);
        startRotationLineXform.setTy(175);
        gizmoGroup.getChildren().add(startRotationLineXform);

                finishRotationLine.setStartX(0);
        finishRotationLine.setStartY(0);
        finishRotationLine.setEndX(0);
        finishRotationLine.setEndY(-170);
        finishRotationLine.setStroke(Color.WHITE);
        finishRotationLine.setStrokeWidth(3);
        finishRotationLine.setMouseTransparent(true);
        
        finishRotationLineXform.getChildren().add(finishRotationLine);
        finishRotationLineXform.setTx(175);
        finishRotationLineXform.setTy(175);
        gizmoGroup.getChildren().add(finishRotationLineXform);

        xHandleButton.addEventHandler(MouseEvent.ANY, (MouseEvent event) ->
                              {
                                  if (event.getEventType() == MouseEvent.MOUSE_PRESSED)
                                  {
                                      gizmoGroup.setMouseTransparent(true);
                                      viewManager.enterDragFromGizmo(DragMode.X_CONSTRAINED_TRANSLATE, event);
                                  } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED)
                                  {
                                      viewManager.exitDragFromGizmo();
                                      gizmoGroup.setMouseTransparent(false);
                                  } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED)
                                  {
                                      viewManager.dragFromGizmo(event);
                                  }
        });

        zHandleButton.addEventHandler(MouseEvent.ANY, (MouseEvent event) ->
                              {
                                  if (event.getEventType() == MouseEvent.MOUSE_PRESSED)
                                  {
                                      gizmoGroup.setMouseTransparent(true);
                                      viewManager.enterDragFromGizmo(DragMode.Z_CONSTRAINED_TRANSLATE, event);
                                  } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED)
                                  {
                                      viewManager.exitDragFromGizmo();
                                      gizmoGroup.setMouseTransparent(false);
                                  } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED)
                                  {
                                      viewManager.dragFromGizmo(event);
                                  }
        });

        rotationRing.addEventHandler(MouseEvent.ANY, (MouseEvent event) ->
                             {
                                 if (event.getEventType() == MouseEvent.MOUSE_PRESSED)
                                 {
                                     gizmoGroup.setMouseTransparent(true);
                                     viewManager.enterRotateFromGizmo(event);
                                 } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED)
                                 {
                                     viewManager.exitRotateFromGizmo();
                                     gizmoGroup.setMouseTransparent(false);
                                     startRotationLine.setVisible(false);
                                     finishRotationLine.setVisible(false);
                                     rotationStarted = false;
                                 } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED)
                                 {
                                     double currentAngle = viewManager.rotateFromGizmo(event);
                                     if (!rotationStarted)
                                     {
                                         startRotationLine.setVisible(true);
                                         finishRotationLine.setVisible(true);
                                         rotationStarted = true;
                                         rotationStartedAt = currentAngle;
                                         startRotationLineXform.setRz(currentAngle);
                                     } else
                                     {
                                         finishRotationLineXform.setRz(rotationStartedAt - currentAngle);
                                     }
                                 }
        });
    }

    /**
     *
     * @param viewManager
     */
    public void configure(ThreeDViewManager viewManager)
    {
        this.viewManager = viewManager;
        this.loadedModels = viewManager.getLoadedModels();
        this.selectionContainer = viewManager.getSelectionContainer();

        startRotationLine.setVisible(false);
        finishRotationLine.setVisible(false);

        gizmoGroup.visibleProperty().bind(Bindings.isNotEmpty(selectionContainer.selectedModelsProperty()));
    }
}
