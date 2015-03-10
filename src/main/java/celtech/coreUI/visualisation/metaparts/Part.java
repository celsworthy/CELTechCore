package celtech.coreUI.visualisation.metaparts;

import celtech.configuration.PrintBed;
import celtech.coreUI.visualisation.ShapeProvider;
import celtech.coreUI.visualisation.modelDisplay.ModelBounds;
import static celtech.utils.Math.MathUtils.RAD_TO_DEG;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point3D;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author Ian
 */
public class Part implements Comparable, ShapeProvider
{
   private List<ShapeProvider.ShapeChangeListener> shapeChangeListeners;
 
    private HashMap<Vector3D, Integer> vertices;
    private Set<Face> faces;
    /**
     * Print the part using the extruder of the given number.
     */
    private final IntegerProperty associateWithExtruderNumber = new SimpleIntegerProperty(0);
    private Scale transformScalePreferred;
    private Rotate transformRotateSnapToGround;
    private Translate transformSnapToGroundYAdjust;
    private static final Point3D Y_AXIS = new Point3D(0, 1, 0);
    private Rotate transformRotateYPreferred;
    private Translate transformMoveToCentre;
    private Translate transformMoveToPreferred;
    private Translate transformBedCentre;
    private double bedCentreOffsetX;
    private double bedCentreOffsetY;
    private double bedCentreOffsetZ;
    static int SNAP_FACE_INDEX_NOT_SELECTED = -1;
    /**
     * The index of the face that the user has requested face the bed.
     */
    private int snapFaceIndex = SNAP_FACE_INDEX_NOT_SELECTED;
    /**
     * Property wrapper around the scale.
     */
    private DoubleProperty preferredScale;
    /**
     * Property wrapper around the rotationY.
     */
    private DoubleProperty preferredRotationY;
    private ModelBounds originalModelBounds;
    private ModelBounds lastTransformedBounds;

    public Part(HashMap<Vector3D, Integer> vertices, Set<Face> faces)
    {
        this.vertices = vertices;
        this.faces = faces;
        associateWithExtruderNumber.set(0);

        transformScalePreferred = new Scale(1, 1, 1);
        transformRotateSnapToGround = new Rotate(0, 0, 0);
        transformSnapToGroundYAdjust = new Translate(0, 0, 0);
        transformRotateYPreferred = new Rotate(0, 0, 0, 0, Y_AXIS);
        transformMoveToCentre = new Translate(0, 0, 0);
        transformMoveToPreferred = new Translate(0, 0, 0);
        transformBedCentre = new Translate(0, 0, 0);

        preferredScale = new SimpleDoubleProperty(1);
        preferredRotationY = new SimpleDoubleProperty(0);
        shapeChangeListeners = new ArrayList<>();

    }

    private void initialiseTransforms()
    {

        setBedCentreOffsetTransform();

        getTransforms().addAll(transformSnapToGroundYAdjust, transformMoveToPreferred,
                               transformMoveToCentre, transformBedCentre,
                               transformRotateYPreferred, transformRotateSnapToGround);
        meshGroup.getTransforms().addAll(transformScalePreferred);

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

        transformMoveToPreferred.setX(0);
        transformMoveToPreferred.setY(0);
        transformMoveToPreferred.setZ(0);

        lastTransformedBounds = calculateBoundsInParent();

        notifyShapeChange();

    }

    public HashMap<Vector3D, Integer> getVertices()
    {
        return vertices;
    }

    public void setVertices(HashMap<Vector3D, Integer> vertices)
    {
        this.vertices = vertices;
    }

    public Set<Face> getFaces()
    {
        return faces;
    }

    public void setFaces(Set<Face> faces)
    {
        this.faces = faces;
    }

    public void setUseExtruder0Filament(boolean useExtruder0)
    {
        if (useExtruder0)
        {
            associateWithExtruderNumber.set(0);
        } else
        {
            associateWithExtruderNumber.set(1);
        }
    }

    public ReadOnlyIntegerProperty getAssociateWithExtruderNumberProperty()
    {
        return associateWithExtruderNumber;
    }

    void setAssociateWithExtruderNumber(int associateWithExtruderNumber)
    {
        this.associateWithExtruderNumber.set(associateWithExtruderNumber);
    }

    public Part makeCopy()
    {
        Part copyOfPart = new Part(vertices, faces);
        copyOfPart.setAssociateWithExtruderNumber(associateWithExtruderNumber.get());
        copy.setScale(this.getScale());
        copy.setRotationY(this.getRotationY());
        copy.setSnapFaceIndex(snapFaceIndex);
        return copyOfPart;
    }

    /**
     * This compareTo implementation compares based on the overall size of the model.
     */
    @Override
    public int compareTo(Object o) throws ClassCastException
    {
        int returnVal = 0;

        Part compareToThis = (Part) o;
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
     * @param scaleFactor
     */
    public void setScale(double scaleFactor)
    {
        preferredScale.set(scaleFactor);
        transformScalePreferred.setPivotX(originalModelBounds.getCentreX());
        transformScalePreferred.setPivotY(originalModelBounds.getCentreY());
        transformScalePreferred.setPivotZ(originalModelBounds.getCentreZ());
        transformScalePreferred.setX(preferredScale.get());
        transformScalePreferred.setY(preferredScale.get());
        transformScalePreferred.setZ(preferredScale.get());

        dropToBedAndUpdateLastTransformedBounds();
        checkOffBed();
        notifyShapeChange();
    }

    /**
     *
     * @return
     */
    public double getScale()
    {
        return preferredScale.get();
    }

    /**
     *
     * @param value
     */
    public void setRotationY(double value)
    {
        preferredRotationY.set(value);
        transformRotateYPreferred.setAngle(value);

        dropToBedAndUpdateLastTransformedBounds();
        checkOffBed();
        notifyShapeChange();
    }

    public double getRotationY()
    {
        return transformRotateYPreferred.getAngle();
    }

    public Set<Part> cutModelAtHeight(double heightAboveBed)
    {

        double minPrintableY = printBed.getPrintVolumeMinimums().getY();
        int numberOfBins = (int) Math.
            ceil(Math.abs(originalModelBounds.getHeight() / minPrintableY));

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
     * Set transformBedCentre according to the position of the centre of the bed.
     */
    private void setBedCentreOffsetTransform()
    {
        bedCentreOffsetX = PrintBed.getPrintVolumeCentreZeroHeight().getX();
        bedCentreOffsetY = PrintBed.getPrintVolumeCentreZeroHeight().getY();
        bedCentreOffsetZ = PrintBed.getPrintVolumeCentreZeroHeight().getZ();
        transformBedCentre.setX(bedCentreOffsetX);
        transformBedCentre.setY(bedCentreOffsetY);
        transformBedCentre.setZ(bedCentreOffsetZ);
    }

    /**
     *
     * @param xPosition
     * @param zPosition
     */
    public void translateFrontLeftTo(double xPosition, double zPosition)
    {
        double newXPosition = xPosition - bedCentreOffsetX + getTransformedBounds().getWidth() / 2.0;
        double newZPosition = zPosition - bedCentreOffsetZ + getTransformedBounds().getHeight()
            / 2.0;
        double deltaXPosition = newXPosition - transformMoveToPreferred.getX();
        double deltaZPosition = newZPosition - transformMoveToPreferred.getZ();
        transformMoveToPreferred.setX(newXPosition);
        transformMoveToPreferred.setZ(newZPosition);
        updateLastTransformedBoundsForTranslateByX(deltaXPosition);
        updateLastTransformedBoundsForTranslateByZ(deltaZPosition);
        checkOffBed();
        notifyShapeChange();
    }

    /**
     * This method checks if the model is off the print bed and if so it adjusts the
     * transformMoveToPreferred to bring it back to the nearest edge of the bed.
     */
    private void keepOnBedXZ()
    {
        double deltaX = 0;

        double minBedX = PrintBed.getPrintVolumeCentre().getX() - PrintBed.maxPrintableXSize / 2.0
            + 1;
        double maxBedX = PrintBed.getPrintVolumeCentre().getX() + PrintBed.maxPrintableXSize / 2.0
            - 1;
        if (getTransformedBounds().getMinX() < minBedX)
        {
            deltaX = -(getTransformedBounds().getMinX() - minBedX);
            transformMoveToPreferred.setX(transformMoveToPreferred.getX() + deltaX);
        } else if (getTransformedBounds().getMaxX() > maxBedX)
        {
            deltaX = -(getTransformedBounds().getMaxX() - maxBedX);
            transformMoveToPreferred.setX(transformMoveToPreferred.getX() + deltaX);
        }
        updateLastTransformedBoundsForTranslateByX(deltaX);

        double deltaZ = 0;
        double minBedZ = PrintBed.getPrintVolumeCentre().getZ() - PrintBed.maxPrintableZSize / 2.0
            + 1;
        double maxBedZ = PrintBed.getPrintVolumeCentre().getZ() + PrintBed.maxPrintableZSize / 2.0
            - 1;
        if (getTransformedBounds().getMinZ() < minBedZ)
        {
            deltaZ = -(getTransformedBounds().getMinZ() - minBedZ);
            transformMoveToPreferred.setZ(transformMoveToPreferred.getZ() + deltaZ);
        } else if (getTransformedBounds().getMaxZ() > maxBedZ)
        {
            deltaZ = -(getTransformedBounds().getMaxZ() - maxBedZ);
            transformMoveToPreferred.setZ(transformMoveToPreferred.getZ() + deltaZ);
        }
        updateLastTransformedBoundsForTranslateByZ(deltaZ);

        checkOffBed();
        notifyShapeChange();
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

        updateLastTransformedBoundsForTranslateByX(xMove);
        updateLastTransformedBoundsForTranslateByZ(zMove);

        keepOnBedXZ();
        checkOffBed();

    }

    /**
     * Move the CENTRE of the object to the desired x,z position.
     */
    public void translateTo(double xPosition, double zPosition)
    {
        translateXTo(xPosition);
        translateZTo(zPosition);

        checkOffBed();
    }

    /**
     *
     */
    public void centreObjectOnBed()
    {
        transformMoveToPreferred.setX(0);
        transformMoveToPreferred.setZ(0);
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
        steno.info("Relative sizes of model: X " + relativeXSize + " Y " + relativeYSize + " Z "
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

    }

    public void setSnapFaceIndex(int snapFaceIndex)
    {
        this.snapFaceIndex = snapFaceIndex;
        if (snapFaceIndex != SNAP_FACE_INDEX_NOT_SELECTED)
        {
            Vector3D faceNormal = getFaceNormal(snapFaceIndex);
            Vector3D downVector = new Vector3D(0, 1, 0);

            Rotation result = new Rotation(faceNormal, downVector);
            Vector3D axis = result.getAxis();
            double angleDegrees = result.getAngle() * RAD_TO_DEG;

            transformRotateSnapToGround.setAxis(new Point3D(axis.getX(), axis.getY(), axis.getZ()));
            transformRotateSnapToGround.setAngle(angleDegrees);
            transformRotateSnapToGround.setPivotX(originalModelBounds.getCentreX());
            transformRotateSnapToGround.setPivotY(originalModelBounds.getCentreY());
            transformRotateSnapToGround.setPivotZ(originalModelBounds.getCentreZ());

            dropToBedAndUpdateLastTransformedBounds();
        }
    }

    public double getPreferredScale()
    {
        return preferredScale.get();
    }

    /**
     * @param preferredScale the preferredScale to set
     */
    public void setPreferredScale(double preferredScale)
    {
        this.preferredScale.set(preferredScale);
        setScale(preferredScale);
    }

    public DoubleProperty preferredScaleProperty()
    {
        return preferredScale;
    }

    /**
     * @return the preferredYRotation
     */
    public double getPreferredRotationY()
    {
        return preferredRotationY.get();
    }

    /**
     * @param preferredYRotation the preferredYRotation to set
     */
    public void setPreferredRotationY(double preferredYRotation)
    {
        this.preferredRotationY.set(preferredYRotation);
        setRotationY(preferredYRotation);
    }

    public DoubleProperty preferredRotationYProperty()
    {
        return preferredRotationY;
    }

    /**
     *
     * @param width
     */
    public void resizeWidth(double width)
    {
        ModelBounds bounds = getLocalBounds();

        double originalWidth = bounds.getWidth();

        double newScale = width / originalWidth;
        setScale(newScale);
        notifyShapeChange();
    }

    /**
     *
     * @param height
     */
    public void resizeHeight(double height)
    {
        ModelBounds bounds = getLocalBounds();

        double currentHeight = bounds.getHeight();

        double newScale = height / currentHeight;

        setScale(newScale);
        notifyShapeChange();
    }

    /**
     *
     * @param depth
     */
    public void resizeDepth(double depth)
    {

        ModelBounds bounds = getLocalBounds();

        double currentDepth = bounds.getDepth();

        double newScale = depth / currentDepth;

        setScale(newScale);
        notifyShapeChange();
    }

    /**
     *
     * @param x
     */
    public void translateXTo(double xPosition)
    {
        ModelBounds bounds = getTransformedBounds();

        double newMaxX = xPosition + bounds.getWidth() / 2;
        double newMinX = xPosition - bounds.getWidth() / 2;

        double finalXPosition = xPosition;

        if (newMinX < 0)
        {
            finalXPosition += -newMinX;
        } else if (newMaxX > printBed.getPrintVolumeMaximums().getX())
        {
            finalXPosition -= (newMaxX - printBed.getPrintVolumeMaximums().getX());
        }

        double currentXPosition = getTransformedCentreX();
        double requiredTranslation = finalXPosition - currentXPosition;
        transformMoveToPreferred.setX(transformMoveToPreferred.getX() + requiredTranslation);

        updateLastTransformedBoundsForTranslateByX(requiredTranslation);
        checkOffBed();
        notifyShapeChange();
    }

    /**
     *
     */
    public void translateZTo(double zPosition)

    {
        ModelBounds bounds = getTransformedBounds();

        double newMaxZ = zPosition + bounds.getDepth() / 2;
        double newMinZ = zPosition - bounds.getDepth() / 2;

        double finalZPosition = zPosition;

        if (newMinZ < 0)
        {
            finalZPosition += -newMinZ;
        } else if (newMaxZ > printBed.getPrintVolumeMaximums().getZ())
        {
            finalZPosition -= (newMaxZ - printBed.getPrintVolumeMaximums().getZ());
        }

        double currentZPosition = getTransformedCentreZ();
        double requiredTranslation = finalZPosition - currentZPosition;
        transformMoveToPreferred.setZ(transformMoveToPreferred.getZ() + requiredTranslation);

        updateLastTransformedBoundsForTranslateByZ(requiredTranslation);
        checkOffBed();
        notifyShapeChange();
    }

    private void checkOffBed()
    {
        ModelBounds bounds = getTransformedBounds();

        double epsilon = 0.001;

        if (MathUtils.compareDouble(bounds.getMinX(), 0, epsilon) == MathUtils.LESS_THAN
            || MathUtils.compareDouble(bounds.getMaxX(), printBed.getPrintVolumeMaximums().getX(),
                                       epsilon) == MathUtils.MORE_THAN
            || MathUtils.compareDouble(bounds.getMinZ(), 0, epsilon) == MathUtils.LESS_THAN
            || MathUtils.compareDouble(bounds.getMaxZ(), printBed.getPrintVolumeMaximums().getZ(),
                                       epsilon) == MathUtils.MORE_THAN
            || MathUtils.compareDouble(bounds.getMaxY(), 0, epsilon) == MathUtils.MORE_THAN
            || MathUtils.compareDouble(bounds.getMinY(), printBed.getPrintVolumeMinimums().getY(),
                                       epsilon) == MathUtils.LESS_THAN)
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
        lastTransformedBounds = calculateBoundsInParent();
    }

    /**
     * Calculate max/min X,Y,Z before the transforms have been applied (ie the original model
     * dimensions before any transforms).
     */
    private ModelBounds calculateBounds()
    {
        TriangleMesh mesh = (TriangleMesh) getMeshView().getMesh();
        ObservableFloatArray originalPoints = mesh.getPoints();

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

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
    
        public ModelBounds getTransformedBounds()
    {
        return lastTransformedBounds;
    }

    public ModelBounds getLocalBounds()
    {
        return originalModelBounds;
    }
    public ModelBounds getOriginalModelBounds()
    {
        return originalModelBounds;
    }

    @Override
    public double getCentreZ()
    {
        return getLocalBounds().getCentreZ();
    }

    @Override
    public double getCentreY()
    {
        return getLocalBounds().getCentreY();
    }

    @Override
    public double getCentreX()
    {
        return getLocalBounds().getCentreX();
    }

    public double getTransformedCentreZ()
    {
        return getTransformedBounds().getCentreZ();
    }

    public double getTransformedCentreX()
    {
        return getTransformedBounds().getCentreX();
    }
    private void updateLastTransformedBoundsForTranslateByX(double deltaCentreX)
    {
        lastTransformedBounds.translateX(deltaCentreX);
        notifyShapeChange();
    }

    private void updateLastTransformedBoundsForTranslateByZ(double deltaCentreZ)
    {
        lastTransformedBounds.translateZ(deltaCentreZ);
        notifyShapeChange();
    }

    @Override
    public void addShapeChangeListener(ShapeChangeListener listener)
    {
        shapeChangeListeners.add(listener);
    }

    /**
     * This method must be called at the end of any operation that changes one or more of the
     * transforms.
     */
    private void notifyShapeChange()
    {
        for (ShapeChangeListener shapeChangeListener : shapeChangeListeners)
        {
            shapeChangeListener.shapeChanged(this);
        }
    }

    /**
     *
     * @return
     */
    public double getTotalWidth()
    {
        double totalwidth = originalModelBounds.getWidth() * preferredScale.get();
        return totalwidth;
    }

    /**
     *
     * @return
     */
    public double getTotalDepth()
    {
        double totaldepth = originalModelBounds.getDepth() * preferredScale.get();
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

    public void snapToGround(int faceNumber)
    {
        setSnapFaceIndex(faceNumber);

        checkOffBed();
        notifyShapeChange();
    }

    private void dropToBedAndUpdateLastTransformedBounds()
    {
        // Correct transformRotateSnapToGroundYAdjust for change in height (Y)
        transformSnapToGroundYAdjust.setY(0);
        ModelBounds modelBoundsParent = calculateBoundsInParent();
        transformSnapToGroundYAdjust.setY(-modelBoundsParent.getMaxY());
        lastTransformedBounds = calculateBoundsInParent();
    }

    @Override
    public double getOriginalHeight()
    {
        return getLocalBounds().getHeight();
    }

    @Override
    public double getScaledHeight()
    {
        return getLocalBounds().getHeight() * preferredScale.doubleValue();
    }

    @Override
    public double getOriginalDepth()
    {
        return getLocalBounds().getDepth();
    }

    @Override
    public double getScaledDepth()
    {
        return getLocalBounds().getDepth() * preferredScale.doubleValue();
    }

    @Override
    public double getOriginalWidth()
    {
        return getTransformedBounds().getWidth();
    }

    @Override
    public double getScaledWidth()
    {
        return getLocalBounds().getWidth() * preferredScale.doubleValue();
    }

    /*
     Persistence
     Save--
     out.writeDouble(transformMoveToPreferred.getX());
     out.writeDouble(transformMoveToPreferred.getZ());
     out.writeDouble(getScale());
     out.writeDouble(getRotationY());
     out.writeInt(snapFaceIndex);
     Load--
     transformMoveToPreferred.setX(storedX);
     transformMoveToPreferred.setZ(storedZ);
     setScale(storedScale);
     setRotationY(storedRotationY);
     if (storedSnapFaceIndex != SNAP_FACE_INDEX_NOT_SELECTED)
     {
     snapToGround(storedSnapFaceIndex);
     } else
     {
     snapFaceIndex = SNAP_FACE_INDEX_NOT_SELECTED;
     }
     */
}
