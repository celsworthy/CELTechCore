/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation.modelDisplay;

import celtech.CoreTest;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.visualisation.SelectionContainer;
import celtech.coreUI.visualisation.Xform;
import celtech.coreUI.visualisation.importers.FloatArrayList;
import celtech.coreUI.visualisation.importers.ModelLoadResult;
import celtech.coreUI.visualisation.importers.obj.ObjImporter;
import celtech.utils.Math.MathUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SelectionHighlighter extends Group
{

    /**
     *
     */
    public static final String idString = "selectionHighlighter";

    /**
     *
     */
    public static final String scaleHandleString = "scaleHandle";
    private final Stenographer steno = StenographerFactory.getStenographer(SelectionHighlighter.class.getName());
    private ApplicationStatus applicationStatus = null;

    private final PhongMaterial greenMaterial = new PhongMaterial(Color.LIMEGREEN);
    private final PhongMaterial selectedMaterial = new PhongMaterial(Color.YELLOW);

    private final Xform selectionBox = new Xform(Xform.RotateOrder.XYZ);
    private Xform selectionBoxBackLeftTop = null;
    private Xform selectionBoxBackRightTop = null;
    private Xform selectionBoxFrontLeftTop = null;
    private Xform selectionBoxFrontRightTop = null;
    private Xform selectionBoxBackLeftBottom = null;
    private Xform selectionBoxBackRightBottom = null;
    private Xform selectionBoxFrontLeftBottom = null;
    private Xform selectionBoxFrontRightBottom = null;

    private Xform gizmoXform = new Xform(Xform.RotateOrder.XYZ);

    private final double cornerBracketLength = 5;
    private final double cornerBoxSize = 2;
    private final double halfCornerBoxSize = cornerBoxSize / 2;
    private boolean scaleActive = false;
    private Group scalingBoxes = new Group();

    private FloatArrayList texCoords = new FloatArrayList();

    private EventHandler<MouseEvent> scaleHighlight = new EventHandler<MouseEvent>()
    {
        @Override
        public void handle(MouseEvent event)
        {
            scaleActive = true;
            greenMaterial.setDiffuseColor(Color.PURPLE);
        }
    };

    private EventHandler<MouseEvent> scaleUnhighlight = new EventHandler<MouseEvent>()
    {
        @Override
        public void handle(MouseEvent event)
        {
            scaleActive = false;
            greenMaterial.setDiffuseColor(Color.LIMEGREEN);
        }
    };

    private ModelLoadResult scaleHandleLoadResult = null;

    /**
     *
     * @param selectionContainer
     * @param cameraDistance
     */
    public SelectionHighlighter(final SelectionContainer selectionContainer, final DoubleProperty cameraDistance)
    {
        texCoords.add(0f);
        texCoords.add(0f);

        String scaleHandleURL = CoreTest.class
                .getResource(ApplicationConfiguration.modelResourcePath + "scaleHandle.obj").toExternalForm();

        ObjImporter scaleHandleImporter = new ObjImporter();
        scaleHandleLoadResult = scaleHandleImporter.loadFile(null, scaleHandleURL, null);

        applicationStatus = ApplicationStatus.getInstance();

        this.setId(idString);

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
//                steno.info("Camera distance is now " + t1.doubleValue());
                double newScale = t1.doubleValue() / 295;

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
        selectionBoxBackLeftBottom = generateSelectionCornerGroup(0, 90, 0, halfCornerBoxSize, -halfCornerBoxSize, halfCornerBoxSize, false);

        selectionBoxBackRightBottom = generateSelectionCornerGroup(0, -180, 0, halfCornerBoxSize, -halfCornerBoxSize, halfCornerBoxSize, false);

        selectionBoxBackLeftTop = generateSelectionCornerGroup(180, 0, 0, halfCornerBoxSize, -halfCornerBoxSize, halfCornerBoxSize, true);

        selectionBoxBackRightTop = generateSelectionCornerGroup(180, 90, 0, halfCornerBoxSize, -halfCornerBoxSize, halfCornerBoxSize, true);

        selectionBoxFrontLeftBottom = generateSelectionCornerGroup(0, 0, 0, halfCornerBoxSize, -halfCornerBoxSize, halfCornerBoxSize, false);

        selectionBoxFrontRightBottom = generateSelectionCornerGroup(0, -90, 0, halfCornerBoxSize, -halfCornerBoxSize, halfCornerBoxSize, false);

        selectionBoxFrontLeftTop = generateSelectionCornerGroup(180, -90, 0, halfCornerBoxSize, -halfCornerBoxSize, halfCornerBoxSize, true);

        selectionBoxFrontRightTop = generateSelectionCornerGroup(0, 0, 180, halfCornerBoxSize, -halfCornerBoxSize, halfCornerBoxSize, true);

        selectionBox.getChildren().addAll(selectionBoxBackLeftBottom, selectionBoxBackRightBottom, selectionBoxBackLeftTop, selectionBoxBackRightTop,
                                          selectionBoxFrontLeftBottom, selectionBoxFrontRightBottom, selectionBoxFrontLeftTop, selectionBoxFrontRightTop);

    }

    private Xform generateSelectionCornerGroup(double xRotate, double yRotate, double zRotate, double cornerBoxXOffset, double cornerBoxYOffset, double cornerBoxZOffset, boolean generateCornerBox)
    {
        final int cylSamples = 4;
        final double cylRadius = .05;

        Xform selectionCornerTransform = new Xform();
        Group selectionCorner = new Group();
        selectionCornerTransform.getChildren().add(selectionCorner);

        Box part1 = new Box(cylRadius, cornerBracketLength, cylRadius);
//        Cylinder part1 = new Cylinder(cylRadius, cylHeight, cylSamples);
        part1.setMaterial(greenMaterial);
        part1.setDrawMode(DrawMode.LINE);
        part1.setTranslateY(-cornerBracketLength / 2);

        Box part2 = new Box(cylRadius, cornerBracketLength, cylRadius);
//        Cylinder part2 = new Cylinder(cylRadius, cylHeight, cylSamples);
        part2.setMaterial(greenMaterial);
        part2.setDrawMode(DrawMode.LINE);
        part2.setRotationAxis(MathUtils.zAxis);
        part2.setRotate(-90);
        part2.setTranslateX(cornerBracketLength / 2);

        Box part3 = new Box(cylRadius, cornerBracketLength, cylRadius);
//        Cylinder part3 = new Cylinder(cylRadius, cylHeight, cylSamples);
        part3.setMaterial(greenMaterial);
        part3.setRotationAxis(MathUtils.xAxis);
        part3.setDrawMode(DrawMode.LINE);
        part3.setRotate(-90);
        part3.setTranslateZ(cornerBracketLength / 2);
        selectionCorner.getChildren().addAll(part1, part2, part3);

        if (generateCornerBox)
        {

//            Box part4 = new Box(cornerBoxSize, cornerBoxSize, cornerBoxSize);
//            part4.setMaterial(greenMaterial);
//            part4.setId(scaleHandleString);
//            part4.setOnMouseDragged(new EventHandler<MouseEvent>()
//            {
//
//                @Override
//                public void handle(MouseEvent event)
//                {
//                    steno.info("Got " + event.toString());
//                }
//            });
//            ModelContainer box = scaleHandleLoadResult.getModelContainer().clone();
//            box.setTranslateX(cornerBoxXOffset);
//            box.setTranslateY(cornerBoxYOffset);
//            box.setTranslateZ(cornerBoxZOffset);
//            box.setOnMouseEntered(scaleHighlight);
//            box.setOnMouseExited(scaleUnhighlight);
//            box.setId("scaleHandle");
//            selectionCorner.getChildren().add(box);

        }

        selectionCornerTransform.setRotateX(xRotate);
        selectionCornerTransform.setRotateY(yRotate);
        selectionCornerTransform.setRotateZ(zRotate);

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
        
//        System.out.format("new coords %.2f %.2f %.2f %.2f, %.2f\n", minX, maxX, minZ, maxZ, minY);

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

    /**
     *
     * @return
     */
    public boolean isScaleActive()
    {
        return scaleActive;
    }
}
