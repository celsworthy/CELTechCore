/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.modelcontrol;

import celtech.configuration.PrintBed;
import celtech.coreUI.visualisation.ApplicationMaterials;
import celtech.coreUI.visualisation.importers.FloatArrayList;
import celtech.coreUI.visualisation.importers.IntegerArrayList;
import celtech.coreUI.visualisation.modelDisplay.ModelBounds;
import static celtech.utils.Math.MathUtils.RAD_TO_DEG;
import celtech.utils.gcode.representation.GCodeElement;
import celtech.utils.gcode.representation.GCodeMeshData;
import celtech.utils.gcode.representation.MovementType;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ModelContainer extends Group implements Serializable, Comparable
{

    private static final long serialVersionUID = 1L;
    private Stenographer steno = null;
    private PrintBed printBed = null;
    private boolean isCollided = false;
    private BooleanProperty isSelected = null;
    private BooleanProperty isOffBed = null;
    private SimpleStringProperty modelName = null;
    private ModelContentsEnumeration modelContentsType = ModelContentsEnumeration.MESH;
    //GCode only
    private ObservableList<String> fileLines = FXCollections.observableArrayList();
    private GCodeMeshData gcodeMeshData = null;
    private GCodeElement lastSelectedPart = null;
    private IntegerProperty selectedGCodeLine = new SimpleIntegerProperty(0);
    private IntegerProperty linesOfGCode = new SimpleIntegerProperty(0);
    private IntegerProperty currentLayer = new SimpleIntegerProperty(0);
    private IntegerProperty numberOfLayers = new SimpleIntegerProperty(0);
    private IntegerProperty maxLayerVisible = new SimpleIntegerProperty(0);
    private IntegerProperty minLayerVisible = new SimpleIntegerProperty(0);

    private DoubleProperty height = new SimpleDoubleProperty(0);

    ModelBounds originalModelBounds;
    // New AL
    private final Scale transformScalePreferred = new Scale(1, 1, 1);
    private final Rotate transformRotateSnapToGround = new Rotate(0, 0, 0);
    private final Translate transformSnapToGroundYAdjust = new Translate(0, 0, 0);
    private static final Point3D Y_AXIS = new Point3D(0, 1, 0);
    private final Rotate transformRotateYPreferred = new Rotate(0, 0, 0, 0, Y_AXIS);
    private final Translate transformMoveToCentre = new Translate(0, 0, 0);
    private final Translate transformMoveToPreferred = new Translate(0, 0, 0);
    private final Translate transformBedCentre = new Translate(0, 0, 0);

    static int SNAP_FACE_INDEX_NOT_SELECTED = -1;
    private int snapFaceIndex = SNAP_FACE_INDEX_NOT_SELECTED;
    private double preferredScale = 1;
    private double preferredYRotation = 0;

    private double bedCentreOffsetX;
    private double bedCentreOffsetY;
    private double bedCentreOffsetZ;

    /**
     *
     */
    public ModelContainer()
    {
        super();
    }

    /**
     *
     * @param name
     */
    public ModelContainer(String name)
    {
        super();
        initialise(name);
        updateTransformMoveToCentre();
    }

    /**
     *
     * @param name
     * @param gcodeMeshData
     * @param fileLines
     */
    public ModelContainer(String name, GCodeMeshData gcodeMeshData, ArrayList<String> fileLines)
    {
        super();
        modelContentsType = ModelContentsEnumeration.GCODE;
        initialise(name);
        updateTransformMoveToCentre();
        getChildren().add(gcodeMeshData.getAllParts());
        this.gcodeMeshData = gcodeMeshData;

        this.fileLines.addAll(fileLines);

        linesOfGCode.set(fileLines.size());

        selectedGCodeLineProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                highlightGCodeLine(t1.intValue());
            }
        });

        minLayerVisible.set(0);
        maxLayerVisible.set(gcodeMeshData.getReferencedArrays().size());

        minLayerVisible.addListener(new ChangeListener<Number>()
        {

            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                setMinVisibleLayer(t1.intValue());
            }
        });

        maxLayerVisible.addListener(new ChangeListener<Number>()
        {

            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                setMaxVisibleLayer(t1.intValue());
            }
        });

        numberOfLayers.set(gcodeMeshData.getReferencedArrays().size());
    }

    /**
     *
     * @param name
     * @param meshToAdd
     */
    public ModelContainer(String name, MeshView meshToAdd)
    {
        super();
        modelContentsType = ModelContentsEnumeration.MESH;
        getChildren().add(meshToAdd);
        initialise(name);
        updateTransformMoveToCentre();
    }

    /**
     *
     * @param name
     * @param meshes
     */
    public ModelContainer(String name, ArrayList<MeshView> meshes)
    {
        super();
        modelContentsType = ModelContentsEnumeration.MESH;
        getChildren().addAll(meshes);
        initialise(name);
        updateTransformMoveToCentre();
    }

    private void updateTransformMoveToCentre()
    {
        originalModelBounds = calculateBounds();

        double centreXOffset = -originalModelBounds.getCentreX();
        double centreYOffset = -originalModelBounds.getMaxY();
        double centreZOffset = -originalModelBounds.getCentreZ();

        transformMoveToCentre.setX(centreXOffset);
        transformMoveToCentre.setY(centreYOffset);
        transformMoveToCentre.setZ(centreZOffset);

        transformRotateYPreferred.setPivotX(originalModelBounds.getCentreX());
        transformRotateYPreferred.setPivotY(originalModelBounds.getCentreY());
        transformRotateYPreferred.setPivotZ(originalModelBounds.getCentreZ());

    }

    private void initialise(String name)
    {
        steno = StenographerFactory.getStenographer(ModelContainer.class.getName());
        printBed = PrintBed.getInstance();

        setBedCentreOffsetTransform();

        isSelected = new SimpleBooleanProperty(false);
        isOffBed = new SimpleBooleanProperty(false);

        modelName = new SimpleStringProperty(name);
        selectedGCodeLine = new SimpleIntegerProperty(0);
        linesOfGCode = new SimpleIntegerProperty(0);
        currentLayer = new SimpleIntegerProperty(0);
        numberOfLayers = new SimpleIntegerProperty(0);
        maxLayerVisible = new SimpleIntegerProperty(0);
        minLayerVisible = new SimpleIntegerProperty(0);

        this.setId(name);

        getTransforms().addAll(transformSnapToGroundYAdjust, transformMoveToPreferred,
                               transformMoveToCentre, transformBedCentre,
                               transformRotateYPreferred, transformRotateSnapToGround,
                               transformScalePreferred);
    }

    /**
     * Set transformBedCentre according to the position of the centre of the bed.
     */
    private void setBedCentreOffsetTransform()
    {
        bedCentreOffsetX = PrintBed.getPrintVolumeCentreZeroHeight().getX();
        bedCentreOffsetY = PrintBed.getPrintVolumeCentreZeroHeight().getY();
        bedCentreOffsetZ = PrintBed.getPrintVolumeCentreZeroHeight().getZ();
        transformBedCentre.setX(bedCentreOffsetX);
        transformBedCentre.setX(bedCentreOffsetY);
        transformBedCentre.setZ(bedCentreOffsetZ);
        System.out.println("Bed centre at X " + bedCentreOffsetX + " Z " + bedCentreOffsetZ);
    }

    /**
     *
     * @return
     */
    @Override
    public ModelContainer clone()
    {
        MeshView newMeshView = new MeshView();

        newMeshView.setMesh(this.getMeshView().getMesh());
        newMeshView.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
        newMeshView.setCullFace(CullFace.BACK);
        newMeshView.setId(this.getMeshView().getId());

        ModelContainer copy = new ModelContainer(this.modelName.get(), newMeshView);
        copy.setScale(this.getScale());
//        copy.setRotationX(this.getRotationX());
//        copy.setRotationY(this.getRotationY());
//        copy.setRotationZ(this.getRotationZ());
        return copy;
    }

    /**
     *
     * @param xMove
     * @param zMove
     */
    public void translateBy(double xMove, double zMove)
    {

        transformMoveToPreferred.setX(transformMoveToPreferred.getX() + xMove);
        transformMoveToPreferred.setZ(transformMoveToPreferred.getZ() + zMove);
        dropToBed();

//        Bounds bounds = this.getBoundsInParent();
////        steno.info("BIP:" + bounds);
////
////        steno.info("xMove:" + xMove + " zMove:" + zMove);
////        steno.info("Tran:" + getTranslateX() + ":" + getTranslateZ());
//
//        double newMaxX = bounds.getMaxX() + xMove;
//        double newMinX = bounds.getMinX() + xMove;
//        double newMaxZ = bounds.getMaxZ() + zMove;
//        double newMinZ = bounds.getMinZ() + zMove;
//
//        double finalXMove = xMove;
//        double finalZMove = zMove;
//
//        if (newMinX < 0)
//        {
//            finalXMove -= bounds.getMinX();
//        }
//
//        if (newMinZ < 0)
//        {
//            finalZMove -= bounds.getMinZ();
//        }
//
//        if (newMaxX > printBed.getPrintVolumeMaximums().getX())
//        {
//            finalXMove += printBed.getPrintVolumeMaximums().getX() - bounds.getMaxX();
//        }
//
//        if (newMaxZ > printBed.getPrintVolumeMaximums().getZ())
//        {
//            finalZMove += printBed.getPrintVolumeMaximums().getZ() - bounds.getMaxZ();
//        }
//
//        double currentX = transformMoveToPreferred.getX();
//        double currentZ = transformMoveToPreferred.getZ();
//
//        transformMoveToPreferred.setX(finalXMove + currentX);
//        transformMoveToPreferred.setZ(finalZMove + currentZ);
//        centreX = transformMoveToPreferred.getX() + centreXOffset;
//        centreZ = transformMoveToPreferred.getZ() + centreZOffset;
        checkOffBed();
    }

    /**
     *
     * @param xPosition
     * @param zPosition
     */
    public void translateFrontLeftTo(double xPosition, double zPosition)
    {
        translateTo(xPosition, zPosition);
    }

    /**
     * Move the CENTRE of the object to the desired x,z position.
     */
    public void translateTo(double xPosition, double zPosition)
    {
        Bounds bounds = this.getBoundsInParent();

        double newMaxX = xPosition + bounds.getWidth() / 2;
        double newMinX = xPosition - bounds.getWidth() / 2;
        double newMaxZ = zPosition + bounds.getDepth() / 2;
        double newMinZ = zPosition - bounds.getDepth() / 2;

        double finalXPosition = xPosition;
        double finalZPosition = zPosition;

        if (newMinX < 0)
        {
            finalXPosition += -newMinX;
        } else if (newMaxX > printBed.getPrintVolumeMaximums().getX())
        {
            finalXPosition -= (newMaxX - printBed.getPrintVolumeMaximums().getX());
        }

        if (newMinZ < 0)
        {
            finalZPosition += -newMinZ;
        } else if (newMaxZ > printBed.getPrintVolumeMaximums().getZ())
        {
            finalZPosition -= (newMaxZ - printBed.getPrintVolumeMaximums().getZ());
        }

        transformMoveToPreferred.setX(finalXPosition);
        transformMoveToPreferred.setX(finalZPosition);

        dropToBed();
        checkOffBed();

    }

    /**
     *
     */
    public void centreObjectOnBed()
    {
        translateTo(0, 0);
    }

    /**
     *
     */
    public void shrinkToFitBed()
    {
        BoundingBox printableBoundingBox = (BoundingBox) getBoundsInLocal();

        BoundingBox printVolumeBounds = printBed.getPrintVolumeBounds();

        double scaling = 1.0;

        double relativeXSize = printableBoundingBox.getWidth() / printVolumeBounds.getWidth();
        double relativeYSize = printableBoundingBox.getHeight() / -printVolumeBounds.getHeight();
        double relativeZSize = printableBoundingBox.getDepth() / printVolumeBounds.getDepth();
        steno.info("Relative sizes of model: X" + relativeXSize + " Y" + relativeYSize + " Z"
            + relativeZSize);

        if (relativeXSize > relativeYSize && relativeXSize > relativeZSize)
        {
            if (relativeXSize > 1)
            {
                scaling = 1 / relativeXSize;
            }
        } else if (relativeYSize > relativeXSize && relativeYSize > relativeZSize)
        {
            if (relativeYSize > 1)
            {
                scaling = 1 / relativeYSize;
            }

        } else
        {
            //Z size must be the largest
            if (relativeZSize > 1)
            {
                scaling = 1 / relativeZSize;
            }
        }

        if (scaling != 1.0f)
        {
            setScale(scaling);
        }

        centreObjectOnBed();
    }

    /**
     *
     * @param hasCollided
     */
    public void setCollision(boolean hasCollided)
    {
        this.isCollided = hasCollided;
        updateMaterial();
    }

    private void updateMaterial()
    {
        MeshView meshView = getMeshView();
        if (meshView != null)
        {
            if (isOffBed.get())
            {
                meshView.setMaterial(ApplicationMaterials.getOffBedModelMaterial());
            } else if (isCollided)
            {
                meshView.setMaterial(ApplicationMaterials.getCollidedModelMaterial());
            } else
            {
                meshView.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
            }
        } else
        {
            for (Node node : ((Group) (getChildrenUnmodifiable().get(0))).getChildrenUnmodifiable())
            {
                if (node instanceof MeshView)
                {
                    if (isOffBed.get())
                    {
                        ((MeshView) node).setMaterial(ApplicationMaterials.getOffBedModelMaterial());
                    } else if (isCollided)
                    {
                        ((MeshView) node).setMaterial(
                            ApplicationMaterials.getCollidedModelMaterial());
                    } else
                    {
                        ((MeshView) node).setMaterial(ApplicationMaterials.getDefaultModelMaterial());
                    }
                }
            }
        }
    }

    /**
     *
     * @return
     */
    public boolean isCollided()
    {
        return isCollided;
    }

    /**
     *
     * @param modelName
     */
    public void setModelName(String modelName)
    {
        this.modelName.set(modelName);
    }

    /**
     *
     * @return
     */
    public String getModelName()
    {
        return modelName.get();
    }

    /**
     *
     * @param scaleFactor
     */
    public void setScale(double scaleFactor)
    {
        preferredScale = scaleFactor;
        transformScalePreferred.setPivotX(originalModelBounds.getCentreX());
        transformScalePreferred.setPivotY(originalModelBounds.getCentreY());
        transformScalePreferred.setPivotZ(originalModelBounds.getCentreZ());
        transformScalePreferred.setX(scaleFactor);
        transformScalePreferred.setY(scaleFactor);
        transformScalePreferred.setZ(scaleFactor);

        dropToBed();
        checkOffBed();
    }

    /**
     *
     * @return
     */
    public double getScale()
    {
        return transformScalePreferred.getX();
    }

    /**
     *
     * @param value
     */
    public void setRotationY(double value)
    {
        transformRotateYPreferred.setAngle(value);
        checkOffBed();
        dropToBed();
    }

    public double getRotationY()
    {
        return transformRotateYPreferred.getAngle();
    }

    /**
     *
     * @param selected
     */
    public void setSelected(boolean selected)
    {
        isSelected.set(selected);
    }

    /**
     *
     * @return
     */
    public boolean isSelected()
    {
        return isSelected.get();
    }

    /**
     *
     * @return
     */
    public BooleanProperty isSelectedProperty()
    {
        return isSelected;
    }

    private void writeObject(ObjectOutputStream out)
        throws IOException
    {
//        out.writeUTF(modelName.get());
//
//        out.writeObject(modelContentsType);
//
//        if (modelContentsType == ModelContentsEnumeration.MESH)
//        {
//            out.writeInt(numberOfMeshes);
//
//            for (Node node : getChildren())
//            {
//                MeshView mesh = (MeshView) node;
//                TriangleMesh triMesh = (TriangleMesh) mesh.getMesh();
//
//                int[] smoothingGroups = triMesh.getFaceSmoothingGroups().toArray(null);
//                out.writeObject(smoothingGroups);
//
//                int[] faces = triMesh.getFaces().toArray(null);
//                out.writeObject(faces);
//
//                float[] points = triMesh.getPoints().toArray(null);
//                out.writeObject(points);
//            }
//        } else
//        {
////            out.writeInt(getChildren().size());
////            for (Node node : getChildren())
////            {
////                out.writeObject(node);
////            }
//        }
//
////        steno.info("Pivot is " + this.getPivot());
//
//        double translationX = getTx();
//        double translationY = getTy();
//        double translationZ = getTz();
//        double storedscale = scale.get();
//        double xrot = getRotationX();
//        double yrot = getRotationY();
//        double zrot = getRotationZ();
//
//        out.writeDouble(translationX);
//        out.writeDouble(translationY);
//        out.writeDouble(translationZ);
//        out.writeDouble(storedscale);
//        out.writeDouble(xrot);
//        out.writeDouble(yrot);
//        out.writeDouble(zrot);
//
//        out.writeDouble(centreX);
//        out.writeDouble(centreY);
//        out.writeDouble(centreZ);
//        out.writeDouble(centreXOffset);
//        out.writeDouble(centreYOffset);
//        out.writeDouble(centreZOffset);
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
//        String modelName = in.readUTF();
//
//        modelContentsType = (ModelContentsEnumeration) in.readObject();
//
//        if (modelContentsType == ModelContentsEnumeration.MESH)
//        {
//            numberOfMeshes = in.readInt();
//
//            for (int i = 0; i < numberOfMeshes; i++)
//            {
//                int[] smoothingGroups = (int[]) in.readObject();
//                int[] faces = (int[]) in.readObject();
//                float[] points = (float[]) in.readObject();
//
//                TriangleMesh triMesh = new TriangleMesh();
//
//                FloatArrayList texCoords = new FloatArrayList();
//                texCoords.add(0f);
//                texCoords.add(0f);
//
//                triMesh.getPoints().addAll(points);
//                triMesh.getTexCoords().addAll(texCoords.toFloatArray());
//                triMesh.getFaces().addAll(faces);
//                triMesh.getFaceSmoothingGroups().addAll(smoothingGroups);
//
//                MeshView newMesh = new MeshView(triMesh);
//                newMesh.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
//                newMesh.setCullFace(CullFace.BACK);
//                newMesh.setId(modelName + "_mesh");
//
//                getChildren().add(newMesh);
//            }
//
//        } else
//        {
////            int numNodes = in.readInt();
////            
////            for (int i = 0; i < numNodes; i++)
////            {
////                Node node = (Node)in.readObject();
////                getChildren().add(node);
////            }
//        }
////        this.getTransforms().clear();
////        this.getTransforms().addAll(new Xform(RotateOrder.XYZ).getTransforms());
//
//        initialiseObject(modelName);
//
//        double translationX = in.readDouble();
//        double translationY = in.readDouble();
//        double translationZ = in.readDouble();
//        double storedscale = in.readDouble();
//        double xrot = in.readDouble();
//        double yrot = in.readDouble();
//        double zrot = in.readDouble();
//        double xCentre = in.readDouble();
//        double yCentre = in.readDouble();
//        double zCentre = in.readDouble();
//        double offsetX = in.readDouble();
//        double offsetY = in.readDouble();
//        double offsetZ = in.readDouble();
//
//        centreXOffset = offsetX;
//        centreYOffset = offsetY;
//        centreZOffset = offsetZ;
//
//        configureModelOnLoad();
//
//        double loadedPositionX = translationX;
//        double loadedPositionZ = translationZ - offsetZ;
//
//        translateTo(xCentre, zCentre);
//
////        centreX = xCentre;
////        centreY = yCentre;
////        centreZ = zCentre;
//        scale(storedscale);
//
//        setRotateX(xrot);
//        setRotateY(yrot);
//        setRotateZ(zrot);
    }

    private void readObjectNoData()
        throws ObjectStreamException
    {

    }

    /**
     *
     * @param newScale
     */
    public void scale(double newScale)
    {
//        steno.info("About to scale to " + newScale + "\n" + this.toString());
        setScale(newScale);
    }

    /**
     *
     * @return
     */
    public MeshView getMeshView()
    {
        if (getChildrenUnmodifiable().get(0) instanceof MeshView)
        {
            return (MeshView) (getChildrenUnmodifiable().get(0));
        } else
        {
            return null;
        }
    }

    /**
     *
     * @return
     */
    public ObservableList<Node> getMeshes()
    {
        if (modelContentsType == ModelContentsEnumeration.MESH)
        {
            return getChildren();
        } else
        {
            return null;
        }
    }

    /**
     *
     * @return
     */
    public ModelContentsEnumeration getModelContentsType()
    {
        return modelContentsType;
    }

    /**
     *
     * @return
     */
    public ObservableList<String> getGCodeLines()
    {
        return fileLines;
    }

    private void highlightGCodeLine(int lineNumber)
    {
        if (modelContentsType == ModelContentsEnumeration.GCODE)
        {
            if (lastSelectedPart != null)
            {
//                lastSelectedPart.getGcodeVisualRepresentation().setMaterial(ApplicationMaterials.getGCodeMaterial(lastSelectedPart.getMovementType(), false));
            }

            GCodeElement selectedPart = gcodeMeshData.getReferencedElements().get(lineNumber);

            if (selectedPart != null)
            {
                Group parentLayer = (Group) selectedPart.getGcodeVisualRepresentation().getParent().getParent().getParent();
                currentLayer.set(Integer.valueOf(parentLayer.getId()));
//                selectedPart.getGcodeVisualRepresentation().setMaterial(ApplicationMaterials.getGCodeMaterial(selectedPart.getMovementType(), true));
                lastSelectedPart = selectedPart;
            }
        }
    }

    /**
     *
     * @param lineNumber
     */
    public void selectGCodeLine(int lineNumber)
    {
        highlightGCodeLine(lineNumber);
        setSelectedGCodeLine(lineNumber);
    }

    /**
     *
     * @param value
     */
    public void setSelectedGCodeLine(int value)
    {
        selectedGCodeLine.set(value);
    }

    /**
     *
     * @return
     */
    public int getSelectedGCodeLine()
    {
        return selectedGCodeLine.get();
    }

    /**
     *
     * @return
     */
    public IntegerProperty selectedGCodeLineProperty()
    {
        return selectedGCodeLine;
    }

    /**
     *
     * @return
     */
    public IntegerProperty linesOfGCodeProperty()
    {
        return linesOfGCode;
    }

    /**
     *
     * @return
     */
    public IntegerProperty minLayerVisibleProperty()
    {
        return minLayerVisible;
    }

    /**
     *
     * @return
     */
    public IntegerProperty maxLayerVisibleProperty()
    {
        return maxLayerVisible;
    }

    /**
     *
     * @return
     */
    public IntegerProperty currentLayerProperty()
    {
        return currentLayer;
    }

    /**
     *
     * @return
     */
    public IntegerProperty numberOfLayersProperty()
    {
        return numberOfLayers;
    }

    /**
     *
     * @param visible
     */
    public void showTravel(boolean visible)
    {
        for (GCodeElement element : gcodeMeshData.getReferencedElements().values())
        {
            if (element.getMovementType() == MovementType.TRAVEL)
            {
                element.getGcodeVisualRepresentation().setVisible(visible);
            }
        }
    }

    /**
     *
     * @param visible
     */
    public void showSupport(boolean visible)
    {
        for (GCodeElement element : gcodeMeshData.getReferencedElements().values())
        {
            if (element.getMovementType() == MovementType.EXTRUDE_SUPPORT)
            {
                element.getGcodeVisualRepresentation().setVisible(visible);
            }
        }
    }

    private void setMinVisibleLayer(int layerNumber)
    {
        for (int i = 0; i < layerNumber; i++)
        {
            gcodeMeshData.getReferencedArrays().get(i).setVisible(false);
        }

        for (int i = layerNumber; i < maxLayerVisible.get(); i++)
        {
            gcodeMeshData.getReferencedArrays().get(i).setVisible(true);
        }
    }

    private void setMaxVisibleLayer(int layerNumber)
    {
        for (int i = maxLayerVisible.get(); i < gcodeMeshData.getReferencedArrays().size(); i++)
        {
            gcodeMeshData.getReferencedArrays().get(i).setVisible(false);
        }

        for (int i = minLayerVisible.get(); i < maxLayerVisible.get(); i++)
        {
            gcodeMeshData.getReferencedArrays().get(i).setVisible(true);
        }
    }

    /**
     *
     * @param width
     */
    public void resizeWidth(double width)
    {
        Bounds bounds = getBoundsInLocal();

        double currentWidth = bounds.getWidth();

        double newScale = width / currentWidth;

        setScale(newScale);
    }

    /**
     *
     * @param height
     */
    public void resizeHeight(double height)
    {
        Bounds bounds = getBoundsInLocal();

        double currentHeight = bounds.getHeight();

        double newScale = height / currentHeight;

        setScale(newScale);
    }

    /**
     *
     * @param depth
     */
    public void resizeDepth(double depth)
    {
        Bounds bounds = getBoundsInLocal();

        double currentDepth = bounds.getDepth();

        double newScale = depth / currentDepth;

        setScale(newScale);
    }

    /**
     *
     * @param x
     */
    public void translateXTo(double x)
    {
//        translateTo(x, centreZ);
    }

    /**
     *
     * @param z
     */
    public void translateZTo(double z)
    {
//        translateTo(centreX, z);
    }

    private void checkOffBed()
    {
        Bounds bounds = getBoundsInParent();
        if (bounds.getMinX() < 0
            || bounds.getMaxX() > printBed.getPrintVolumeMaximums().getX()
            || bounds.getMinZ() < 0
            || bounds.getMaxZ() > printBed.getPrintVolumeMaximums().getZ())
        {
            isOffBed.set(true);
        } else
        {
            isOffBed.set(false);
        }

        updateMaterial();
    }

    /**
     *
     * @return
     */
    public BooleanProperty isOffBedProperty()
    {
        return isOffBed;
    }

    public void deltaRotateAroundY(double newValue)
    {
        transformRotateYPreferred.setAngle(transformRotateYPreferred.getAngle() + newValue);
    }

    private ModelBounds calculateBounds()
    {
        TriangleMesh mesh = (TriangleMesh) getMeshView().getMesh();
        ObservableFloatArray originalPoints = mesh.getPoints();

        double minX = 999;
        double minY = 999;
        double minZ = 999;
        double maxX = -999;
        double maxY = -999;
        double maxZ = -999;

        for (int pointOffset = 0; pointOffset < originalPoints.size(); pointOffset += 3)
        {
            float xPos = originalPoints.get(pointOffset);
            float yPos = originalPoints.get(pointOffset + 1);
            float zPos = originalPoints.get(pointOffset + 2);

            minX = Math.min(xPos, minX);
            minY = Math.min(yPos, minY);
            minZ = Math.min(zPos, minZ);

            maxX = Math.max(xPos, maxX);
            maxY = Math.max(yPos, maxY);
            maxZ = Math.max(zPos, maxZ);
        }

        double newwidth = maxX - minX;
        double newdepth = maxZ - minZ;
        double newheight = maxY - minY;

        double newcentreX = minX + (newwidth / 2);
        double newcentreY = minY + (newheight / 2);
        double newcentreZ = minZ + (newdepth / 2);

        return new ModelBounds(minX, maxX, minY, maxY, minZ, maxZ, newwidth,
                               newheight, newdepth, newcentreX, newcentreY,
                               newcentreZ);
    }

    public ModelBounds calculateBoundsInParent()
    {
        TriangleMesh mesh = (TriangleMesh) getMeshView().getMesh();
        ObservableFloatArray originalPoints = mesh.getPoints();

        double minX = 999;
        double minY = 999;
        double minZ = 999;
        double maxX = -999;
        double maxY = -999;
        double maxZ = -999;

        for (int pointOffset = 0; pointOffset < originalPoints.size(); pointOffset += 3)
        {
            float xPos = originalPoints.get(pointOffset);
            float yPos = originalPoints.get(pointOffset + 1);
            float zPos = originalPoints.get(pointOffset + 2);

            Point3D pointInParent = localToParent(xPos, yPos, zPos);

            minX = Math.min(pointInParent.getX(), minX);
            minY = Math.min(pointInParent.getY(), minY);
            minZ = Math.min(pointInParent.getZ(), minZ);

            maxX = Math.max(pointInParent.getX(), maxX);
            maxY = Math.max(pointInParent.getY(), maxY);
            maxZ = Math.max(pointInParent.getZ(), maxZ);
        }

        double newwidth = maxX - minX;
        double newdepth = maxZ - minZ;
        double newheight = maxY - minY;

        double newcentreX = minX + (newwidth / 2);
        double newcentreY = minY + (newheight / 2);
        double newcentreZ = minZ + (newdepth / 2);

        return new ModelBounds(minX, maxX, minY, maxY, minZ, maxZ, newwidth,
                               newheight, newdepth, newcentreX, newcentreY,
                               newcentreZ);
    }

    /**
     *
     * @return
     */
    public ArrayList<ModelContainer> cutToSize()
    {
        TriangleMesh mesh = (TriangleMesh) getMeshView().getMesh();
        ObservableFaceArray originalFaces = mesh.getFaces();
        ObservableFloatArray originalPoints = mesh.getPoints();

        double minPrintableY = printBed.getPrintVolumeMinimums().getY();
        int numberOfBins = (int) Math.ceil(Math.abs(originalModelBounds.getHeight() / minPrintableY));

        ArrayList<ModelContainer> outputMeshes = new ArrayList<>();

        ArrayList<IntegerArrayList> newFaces = new ArrayList();
        ArrayList<FloatArrayList> newPoints = new ArrayList();

        for (int i = 0; i < numberOfBins; i++)
        {
            newFaces.add(new IntegerArrayList());
            newPoints.add(new FloatArrayList());
        }

        for (int triOffset = 0; triOffset < originalFaces.size(); triOffset += 6)
        {
            int vertex1Ref = originalFaces.get(triOffset) * 3;
            float x1Pos = originalPoints.get(vertex1Ref);
            float y1Pos = originalPoints.get(vertex1Ref + 1);
            float z1Pos = originalPoints.get(vertex1Ref + 2);
            int vertex1Bin = (int) Math.floor((Math.abs(y1Pos) + originalModelBounds.getMaxY())
                / -minPrintableY);

            int vertex2Ref = originalFaces.get(triOffset + 2) * 3;
            float x2Pos = originalPoints.get(vertex2Ref);
            float y2Pos = originalPoints.get(vertex2Ref + 1);
            float z2Pos = originalPoints.get(vertex2Ref + 2);
            int vertex2Bin = (int) Math.floor((Math.abs(y2Pos) + originalModelBounds.getMaxY())
                / -minPrintableY);

            int vertex3Ref = originalFaces.get(triOffset + 4) * 3;
            float x3Pos = originalPoints.get(vertex3Ref);
            float y3Pos = originalPoints.get(vertex3Ref + 1);
            float z3Pos = originalPoints.get(vertex3Ref + 2);
            int vertex3Bin = (int) Math.floor((Math.abs(y3Pos) + originalModelBounds.getMaxY())
                / -minPrintableY);

//            steno.info("Considering " + y1Pos + ":" + y2Pos + ":" + y3Pos);
            if (vertex1Bin == vertex2Bin && vertex1Bin == vertex3Bin)
            {
                newFaces.get(vertex1Bin).add(newPoints.size() / 3);
                newFaces.get(vertex1Bin).add(0);
                newPoints.get(vertex1Bin).add(x1Pos);
                newPoints.get(vertex1Bin).add(y1Pos);
                newPoints.get(vertex1Bin).add(z1Pos);

                newFaces.get(vertex1Bin).add(newPoints.size() / 3);
                newFaces.get(vertex1Bin).add(0);
                newPoints.get(vertex1Bin).add(x2Pos);
                newPoints.get(vertex1Bin).add(y2Pos);
                newPoints.get(vertex1Bin).add(z2Pos);

                newFaces.get(vertex1Bin).add(newPoints.size() / 3);
                newFaces.get(vertex1Bin).add(0);
                newPoints.get(vertex1Bin).add(x3Pos);
                newPoints.get(vertex1Bin).add(y3Pos);
                newPoints.get(vertex1Bin).add(z3Pos);
            }
        }

        FloatArrayList texCoords = new FloatArrayList();
        texCoords.add(0f);
        texCoords.add(0f);

        for (int binCounter = 0; binCounter < numberOfBins; binCounter++)
        {
            TriangleMesh output = new TriangleMesh();

            output.getPoints().addAll(newPoints.get(binCounter).toFloatArray());
            output.getTexCoords().addAll(texCoords.toFloatArray());
            output.getFaces().addAll(newFaces.get(binCounter).toIntArray());
            int[] smoothingGroups = new int[newFaces.get(binCounter).size() / 6];
            for (int i = 0; i < smoothingGroups.length; i++)
            {
                smoothingGroups[i] = 0;
            }
            output.getFaceSmoothingGroups().addAll(smoothingGroups);

            MeshView meshView = new MeshView();

            meshView.setMesh(output);
            meshView.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
            meshView.setCullFace(CullFace.BACK);
            meshView.setId(getId() + "-" + binCounter);

            ModelContainer modelContainer = new ModelContainer(getModelName(), meshView);

            outputMeshes.add(modelContainer);
        }

        return outputMeshes;
    }

    /**
     *
     * @param o
     * @return
     * @throws ClassCastException
     */
    @Override
    public int compareTo(Object o) throws ClassCastException
    {
        int returnVal = 0;

        ModelContainer compareToThis = (ModelContainer) o;
        if (getTotalSize() > compareToThis.getTotalSize())
        {
            returnVal = 1;
        } else if (getTotalSize() < compareToThis.getTotalSize())
        {
            returnVal = -1;
        }

        return returnVal;
    }

    /**
     *
     * @return
     */
    public double getTotalWidth()
    {
        double totalwidth = originalModelBounds.getWidth() * getScale();
        return totalwidth;
    }

    /**
     *
     * @return
     */
    public double getTotalDepth()
    {
        double totaldepth = originalModelBounds.getDepth() * getScale();
        return totaldepth;
    }

    /**
     *
     * @return
     */
    public double getTotalSize()
    {
        return getTotalWidth() + getTotalDepth();
    }

    public void rotateToMakeFaceParallelToGround(int faceNumber)
    {
        snapFaceIndex = faceNumber;

        Vector3D faceNormal = getFaceNormal(faceNumber);
        Vector3D downVector = new Vector3D(0, 1, 0); 

        Rotation result = new Rotation(faceNormal, downVector);
        Vector3D axis = result.getAxis();
        double angleDegrees = result.getAngle() * RAD_TO_DEG;

        ModelBounds modelBoundsParentBefore = calculateBoundsInParent();
        System.out.println("Snap bounds in parent before is " + modelBoundsParentBefore);

        transformRotateSnapToGround.setAxis(new Point3D(axis.getX(), axis.getY(), axis.getZ()));
        transformRotateSnapToGround.setAngle(angleDegrees);
        transformRotateSnapToGround.setPivotX(originalModelBounds.getCentreX());
        transformRotateSnapToGround.setPivotY(originalModelBounds.getCentreY());
        transformRotateSnapToGround.setPivotZ(originalModelBounds.getCentreZ());

        dropToBed();
    }

    /**
     * Return the face normal for the face of the given index. 
     *
     */
    private Vector3D getFaceNormal(int faceNumber) throws MathArithmeticException
    {
        MeshView meshView = getMeshView();
        TriangleMesh triMesh = (TriangleMesh) meshView.getMesh();
        int baseFaceIndex = faceNumber * 6;
        int v1PointIndex = triMesh.getFaces().get(baseFaceIndex);
        int v2PointIndex = triMesh.getFaces().get(baseFaceIndex + 2);
        int v3PointIndex = triMesh.getFaces().get(baseFaceIndex + 4);
        ObservableFloatArray points = triMesh.getPoints();
        Vector3D v1 = convertToVector3D(points, v1PointIndex);
        Vector3D v2 = convertToVector3D(points, v2PointIndex);
        Vector3D v3 = convertToVector3D(points, v3PointIndex);
        Vector3D result1 = v2.subtract(v1);
        Vector3D result2 = v3.subtract(v1);
        Vector3D faceNormal = result1.crossProduct(result2);
        Vector3D currentVectorNormalised = faceNormal.normalize();
        return currentVectorNormalised;
    }

    private Vector3D convertToVector3D(ObservableFloatArray points, int v1PointIndex)
    {
        Vector3D v1 = new Vector3D(points.get(v1PointIndex * 3), points.get((v1PointIndex * 3)
                                   + 1), points.get((v1PointIndex * 3) + 2));
        return v1;
    }

    public ModelBounds getOriginalModelBounds()
    {
        return originalModelBounds;
    }

    private void dropToBed()
    {
        // Correct transformRotateSnapToGroundYAdjust for change in height (Y)
        transformSnapToGroundYAdjust.setY(0);
        ModelBounds modelBoundsParent = calculateBoundsInParent();
        transformSnapToGroundYAdjust.setY(- modelBoundsParent.getMaxY());
    }

}
