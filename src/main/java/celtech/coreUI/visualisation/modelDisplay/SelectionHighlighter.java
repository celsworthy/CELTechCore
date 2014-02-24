/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation.modelDisplay;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.coreUI.visualisation.SelectionContainer;
import celtech.coreUI.visualisation.Xform;
import celtech.utils.Math.MathUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SelectionHighlighter extends Group
{

    private final Stenographer steno = StenographerFactory.getStenographer(SelectionHighlighter.class.getName());
    private ApplicationStatus applicationStatus = null;

    private final PhongMaterial greenMaterial = new PhongMaterial(Color.LIMEGREEN);

    private final Xform selectionBox = new Xform(Xform.RotateOrder.XYZ);
    private Xform selectionBoxBackLeftTop = null;
    private Xform selectionBoxBackRightTop = null;
    private Xform selectionBoxFrontLeftTop = null;
    private Xform selectionBoxFrontRightTop = null;
    private Xform selectionBoxBackLeftBottom = null;
    private Xform selectionBoxBackRightBottom = null;
    private Xform selectionBoxFrontLeftBottom = null;
    private Xform selectionBoxFrontRightBottom = null;

    public SelectionHighlighter(final SelectionContainer selectionContainer, final DoubleProperty cameraDistance)
    {
        applicationStatus = ApplicationStatus.getInstance();

        buildSelectionBox();
        getChildren().add(selectionBox);

        selectionContainer.widthProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                selectionBoundsChanged(selectionContainer);
            }
        });

        selectionContainer.heightProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                selectionBoundsChanged(selectionContainer);
            }
        });

        selectionContainer.depthProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                selectionBoundsChanged(selectionContainer);
            }
        });

        selectionContainer.centreXProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                selectionBoundsChanged(selectionContainer);
            }
        });

        selectionContainer.centreZProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                selectionBoundsChanged(selectionContainer);
            }
        });

        selectionContainer.centreYProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                selectionBoundsChanged(selectionContainer);
            }
        });

        selectionContainer.scaleProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                selectionBoundsChanged(selectionContainer);
            }
        });
//
        selectionContainer.rotationYProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                selectionBox.setRy(t1.doubleValue());
            }
        });

        selectionContainer.rotationXProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                selectionBox.setRx(t1.doubleValue());
            }
        });

        selectionContainer.rotationZProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                selectionBox.setRz(t1.doubleValue());
            }
        });

        this.visibleProperty().bind(Bindings.isNotEmpty(selectionContainer.selectedModelsProperty()).and(applicationStatus.modeProperty().isNotEqualTo(ApplicationMode.SETTINGS)));

        cameraDistance.addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                double newScale = t1.doubleValue() / 400.0;

                for (Node node : selectionBox.getChildren())
                {
                    Xform xform = (Xform) node;
                    xform.setScale(newScale);
//                    for (Node subnode : xform.getChildren())
//                    {
//                        Group corner = (Group) subnode;
//                        for (Node subsubnode : corner.getChildren())
//                        {
//                            Box box = (Box) subsubnode;
//                            box.setScaleX(newScale);
//                            box.setScaleZ(newScale);
//                        }
//                    }
                }
            }
        });

    }

    private void buildSelectionBox()
    {
        selectionBoxBackLeftBottom = generateSelectionCornerGroup();
        selectionBoxBackLeftBottom.setRotateY(90);

        selectionBoxBackRightBottom = generateSelectionCornerGroup();
        selectionBoxBackRightBottom.setRotateY(-180);

        selectionBoxBackLeftTop = generateSelectionCornerGroup();
        selectionBoxBackLeftTop.setRotateX(180);

        selectionBoxBackRightTop = generateSelectionCornerGroup();
        selectionBoxBackRightTop.setRotateX(180);
        selectionBoxBackRightTop.setRotateY(90);

        selectionBoxFrontLeftBottom = generateSelectionCornerGroup();

        selectionBoxFrontRightBottom = generateSelectionCornerGroup();
        selectionBoxFrontRightBottom.setRotateY(-90);

        selectionBoxFrontLeftTop = generateSelectionCornerGroup();
        selectionBoxFrontLeftTop.setRotateX(180);
        selectionBoxFrontLeftTop.setRotateY(-90);

        selectionBoxFrontRightTop = generateSelectionCornerGroup();
        selectionBoxFrontRightTop.setRotateZ(180);

        selectionBox.getChildren().addAll(selectionBoxBackLeftBottom, selectionBoxBackLeftTop, selectionBoxBackRightBottom, selectionBoxBackRightTop,
                selectionBoxFrontLeftBottom, selectionBoxFrontLeftTop, selectionBoxFrontRightBottom, selectionBoxFrontRightTop);

    }

    private Xform generateSelectionCornerGroup()
    {
        final int cylSamples = 4;
        final double cylHeight = 5;
        final double cylRadius = .05;

        Xform selectionCornerTransform = new Xform();
        Group selectionCorner = new Group();
        selectionCornerTransform.getChildren().add(selectionCorner);

        Box part1 = new Box(cylRadius, cylHeight, cylRadius);
//        Cylinder part1 = new Cylinder(cylRadius, cylHeight, cylSamples);
        part1.setMaterial(greenMaterial);
        part1.setDrawMode(DrawMode.LINE);
        part1.setTranslateY(-cylHeight / 2);

        Box part2 = new Box(cylRadius, cylHeight, cylRadius);
//        Cylinder part2 = new Cylinder(cylRadius, cylHeight, cylSamples);
        part2.setMaterial(greenMaterial);
        part2.setDrawMode(DrawMode.LINE);
        part2.setRotationAxis(MathUtils.zAxis);
        part2.setRotate(-90);
        part2.setTranslateX(cylHeight / 2);

        Box part3 = new Box(cylRadius, cylHeight, cylRadius);
//        Cylinder part3 = new Cylinder(cylRadius, cylHeight, cylSamples);
        part3.setMaterial(greenMaterial);
        part3.setRotationAxis(MathUtils.xAxis);
        part3.setDrawMode(DrawMode.LINE);
        part3.setRotate(-90);
        part3.setTranslateZ(cylHeight / 2);

        Box part4 = new Box(2, 2, 2);
        part4.setMaterial(greenMaterial);
        part4.setId("scaleHandle");
//        part4.setDrawMode(DrawMode.LINE);
//        part4.se

        selectionCorner.getChildren().addAll(part1, part2, part3);

        return selectionCornerTransform;
    }

    private void selectionBoundsChanged(SelectionContainer selectionContainer)
    {
        double halfWidth = selectionContainer.getWidth() / 2;
        double halfDepth = selectionContainer.getDepth() / 2;
        double minX = selectionContainer.getCentreX() - halfWidth;
        double maxX = selectionContainer.getCentreX() + halfWidth;
        double minZ = selectionContainer.getCentreZ() - halfDepth;
        double maxZ = selectionContainer.getCentreZ() + halfDepth;
        double minY = -selectionContainer.getHeight();
        
        selectionBoxBackLeftBottom.setTz(maxZ);
        selectionBoxBackLeftBottom.setTx(minX);

        selectionBoxBackRightBottom.setTz(maxZ);
        selectionBoxBackRightBottom.setTx(maxX);

        selectionBoxFrontLeftBottom.setTz(minZ);
        selectionBoxFrontLeftBottom.setTx(minX);

        selectionBoxFrontRightBottom.setTz(minZ);
        selectionBoxFrontRightBottom.setTx(maxX);

        selectionBoxBackLeftTop.setTz(maxZ);
        selectionBoxBackLeftTop.setTx(minX);
        selectionBoxBackLeftTop.setTy(minY);

        selectionBoxBackRightTop.setTz(maxZ);
        selectionBoxBackRightTop.setTx(maxX);
        selectionBoxBackRightTop.setTy(minY);

        selectionBoxFrontLeftTop.setTz(minZ);
        selectionBoxFrontLeftTop.setTx(minX);
        selectionBoxFrontLeftTop.setTy(minY);

        selectionBoxFrontRightTop.setTz(minZ);
        selectionBoxFrontRightTop.setTx(maxX);
        selectionBoxFrontRightTop.setTy(minY);
        
        selectionBox.setPivot(selectionContainer.getCentreX(), 0, selectionContainer.getCentreZ());
    }
}
