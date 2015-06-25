package celtech.modelcontrol;

import celtech.configuration.PrintBed;
import celtech.coreUI.visualisation.ApplicationMaterials;
import celtech.coreUI.visualisation.CameraViewChangeListener;
import celtech.coreUI.visualisation.Edge;
import celtech.coreUI.visualisation.ScreenExtents;
import celtech.coreUI.visualisation.ScreenExtentsProvider;
import celtech.coreUI.visualisation.ShapeProvider;
import celtech.coreUI.visualisation.metaparts.FloatArrayList;
import celtech.coreUI.visualisation.metaparts.IntegerArrayList;
import celtech.coreUI.visualisation.modelDisplay.ModelBounds;
import celtech.coreUI.visualisation.modelDisplay.SelectionHighlighter;
import celtech.utils.Math.MathUtils;
import celtech.utils.gcode.representation.GCodeElement;
import celtech.utils.gcode.representation.GCodeMeshData;
import celtech.utils.gcode.representation.MovementType;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ModelContainer extends Group implements Serializable, Comparable, ShapeProvider,
        ScreenExtentsProvider, CameraViewChangeListener
{

    private static final long serialVersionUID = 1L;
    private static int nextModelId = 0;
    private int modelId;
    private Stenographer steno = null;
    private PrintBed printBed = null;
    private boolean isCollided = false;
    private BooleanProperty isSelected = null;
    private BooleanProperty isOffBed = null;
    private SimpleStringProperty modelName = null;
    private int numberOfMeshes = 0;
    private ModelContentsEnumeration modelContentsType = ModelContentsEnumeration.MESH;
    //GCode only
    private final ObservableList<String> fileLines = FXCollections.observableArrayList();

    ModelBounds originalModelBounds;

    private Scale transformScalePreferred;
    private Translate transformPostRotationYAdjust;
    private static final Point3D Y_AXIS = new Point3D(0, 1, 0);
    private static final Point3D Z_AXIS = new Point3D(0, 0, 1);
    private static final Point3D X_AXIS = new Point3D(1, 0, 0);
    private Rotate transformRotateTwistPreferred;
    private Rotate transformRotateTurnPreferred;
    private Rotate transformRotateLeanPreferred;
    private Translate transformMoveToCentre;
    private Translate transformMoveToPreferred;
    private Translate transformBedCentre;

    private Group meshGroup = new Group();

    /**
     * Property wrapper around the scale.
     */
    private DoubleProperty preferredXScale;
    private DoubleProperty preferredYScale;
    private DoubleProperty preferredZScale;
    /**
     * Property wrapper around the rotationY.
     */
    private DoubleProperty preferredRotationTwist;
    private DoubleProperty preferredRotationLean;
    private DoubleProperty preferredRotationTurn;

    private double bedCentreOffsetX;
    private double bedCentreOffsetY;
    private double bedCentreOffsetZ;
    private ModelBounds lastTransformedBounds;
    private SelectionHighlighter selectionHighlighter = null;
    private List<ShapeProvider.ShapeChangeListener> shapeChangeListeners;
    private List<ScreenExtentsProvider.ScreenExtentsListener> screenExtentsChangeListeners;
    private Set<Node> selectedMarkers;

    /**
     * Print the part using the extruder of the given number.
     */
    private ObservableList<Integer> meshExtruderAssociation;

    private File modelFile;

    /**
     *
     * @param modelFile
     * @param meshToAdd
     */
    public ModelContainer(File modelFile, MeshView meshToAdd)
    {
        super();
        this.getChildren().add(meshGroup);
        modelContentsType = ModelContentsEnumeration.MESH;
        if (meshToAdd != null)
        {
            meshGroup.getChildren().add(meshToAdd);
            numberOfMeshes = 1;
        }

        initialise(modelFile);
        initialiseTransforms();

        //There's only one mesh
        //By default associate it with the first extruder
        meshExtruderAssociation.add(0);
    }

    /**
     * A multiple mesh model Derive a per-mesh extruder number
     *
     * @param modelFile
     * @param name
     * @param meshes
     */
    public ModelContainer(File modelFile, List<MeshView> meshes, List<Integer> extruderAssociation)
    {
        super();
        this.getChildren().add(meshGroup);
        modelContentsType = ModelContentsEnumeration.MESH;
        meshGroup.getChildren().addAll(meshes);
        numberOfMeshes = meshes.size();
        initialise(modelFile);
        initialiseTransforms();

        meshExtruderAssociation = FXCollections.observableArrayList(extruderAssociation);
    }

    public File getModelFile()
    {
        return modelFile;
    }

    public Scale getTransformScale()
    {
        return transformScalePreferred;
    }

    /**
     * Clear the meshes so as to free memory.
     */
    public void clearMeshes()
    {
        meshGroup.getChildren().clear();
    }

    public void setUseExtruder0(MeshView pickedMesh, boolean useExtruder0)
    {
        // Set the extruder for the picked mesh
        if (pickedMesh == null)
        {
            //Set one set all if no mesh specified
            for (int i = 0; i < meshExtruderAssociation.size(); i++)
            {
                meshExtruderAssociation.set(i, useExtruder0 ? 0 : 1);
            }
        } else
        {
            int meshIndex = meshGroup.getChildrenUnmodifiable().indexOf(pickedMesh);
            meshExtruderAssociation.set(meshIndex, useExtruder0 ? 0 : 1);
        }
    }

    void printTransforms()
    {
        System.out.println("Scale preferred is " + transformScalePreferred);
        System.out.println("Move to centre is " + transformMoveToCentre);
        System.out.println("transformSnapToGroundYAdjust is " + transformPostRotationYAdjust);
        System.out.println("transformRotateLeanPreferred is " + transformRotateLeanPreferred);
        System.out.println("transformRotateTwistPreferred " + transformRotateTwistPreferred);
        System.out.println("transformRotateTurnPreferred " + transformRotateTurnPreferred);
        System.out.println("transformBedCentre " + transformBedCentre);

    }

    private void initialiseTransforms()
    {
        transformScalePreferred = new Scale(1, 1, 1);
        transformPostRotationYAdjust = new Translate(0, 0, 0);
        transformRotateLeanPreferred = new Rotate(0, 0, 0, 0, X_AXIS);
        transformRotateTwistPreferred = new Rotate(0, 0, 0, 0, Y_AXIS);
        transformRotateTurnPreferred = new Rotate(0, 0, 0, 0, Z_AXIS);
        transformMoveToCentre = new Translate(0, 0, 0);
        transformMoveToPreferred = new Translate(0, 0, 0);
        transformBedCentre = new Translate(0, 0, 0);

        setBedCentreOffsetTransform();

        /**
         * Rotations (which are all around the centre of the model) must be
         * applied before any translations.
         */
        getTransforms().addAll(transformPostRotationYAdjust, transformMoveToPreferred,
                transformMoveToCentre, transformBedCentre,
                transformRotateTurnPreferred, transformRotateLeanPreferred,
                transformRotateTwistPreferred
        //            ,                   transformRotateSnapToGround
        );
        meshGroup.getTransforms().addAll(transformScalePreferred);

        originalModelBounds = calculateBounds();

        double centreXOffset = -originalModelBounds.getCentreX();
        double centreYOffset = -originalModelBounds.getMaxY();
        double centreZOffset = -originalModelBounds.getCentreZ();

        transformMoveToCentre.setX(centreXOffset);
        transformMoveToCentre.setY(centreYOffset);
        transformMoveToCentre.setZ(centreZOffset);

        transformRotateLeanPreferred.setPivotX(originalModelBounds.getCentreX());
        transformRotateLeanPreferred.setPivotY(originalModelBounds.getCentreY());
        transformRotateLeanPreferred.setPivotZ(originalModelBounds.getCentreZ());

        transformRotateTwistPreferred.setPivotX(originalModelBounds.getCentreX());
        transformRotateTwistPreferred.setPivotY(originalModelBounds.getCentreY());
        transformRotateTwistPreferred.setPivotZ(originalModelBounds.getCentreZ());

        transformRotateTurnPreferred.setPivotX(originalModelBounds.getCentreX());
        transformRotateTurnPreferred.setPivotY(originalModelBounds.getCentreY());
        transformRotateTurnPreferred.setPivotZ(originalModelBounds.getCentreZ());

        transformMoveToPreferred.setX(0);
        transformMoveToPreferred.setY(0);
        transformMoveToPreferred.setZ(0);

        lastTransformedBounds = calculateBoundsInParent();

        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    private void initialise(File modelFile)
    {
        this.modelFile = modelFile;
        modelId = nextModelId;
        nextModelId += 1;
        meshExtruderAssociation = FXCollections.observableArrayList();
        shapeChangeListeners = new ArrayList<>();
        screenExtentsChangeListeners = new ArrayList<>();
        steno = StenographerFactory.getStenographer(ModelContainer.class.getName());
        printBed = PrintBed.getInstance();

        isSelected = new SimpleBooleanProperty(false);
        isOffBed = new SimpleBooleanProperty(false);

        modelName = new SimpleStringProperty(modelFile.getName());

        preferredXScale = new SimpleDoubleProperty(1);
        preferredYScale = new SimpleDoubleProperty(1);
        preferredZScale = new SimpleDoubleProperty(1);
        preferredRotationLean = new SimpleDoubleProperty(0);
        preferredRotationTwist = new SimpleDoubleProperty(0);
        preferredRotationTurn = new SimpleDoubleProperty(0);

        selectedMarkers = new HashSet<>();

        this.setId(modelFile.getName());
    }

    /**
     * Set transformBedCentre according to the position of the centre of the
     * bed.
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
     * Make a copy of this ModelContainer and return it.
     *
     * @return
     */
    public ModelContainer makeCopy()
    {
        List<MeshView> meshViews = getMeshViews();

        List<MeshView> clonedMeshViews = new ArrayList<>();

        for (MeshView meshView : meshViews)
        {
            MeshView newMeshView = new MeshView();

            newMeshView.setMesh(meshView.getMesh());
            newMeshView.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
            newMeshView.setCullFace(CullFace.BACK);
            newMeshView.setId(meshView.getId());

            clonedMeshViews.add(newMeshView);
        }

        ObservableList<Integer> clonedMeshToExtruder = FXCollections.observableArrayList(meshExtruderAssociation);

        ModelContainer copy = new ModelContainer(this.modelFile, clonedMeshViews, clonedMeshToExtruder);
        copy.setXScale(this.getXScale());
        copy.setYScale(this.getYScale());
        copy.setZScale(this.getZScale());
        copy.setRotationLean(this.getRotationLean());
        copy.setRotationTwist(this.getRotationTwist());
        copy.setRotationTurn(this.getRotationTurn());
        return copy;
    }

    public void translateBy(double xMove, double zMove)
    {

        transformMoveToPreferred.setX(transformMoveToPreferred.getX() + xMove);
        transformMoveToPreferred.setZ(transformMoveToPreferred.getZ() + zMove);

        updateLastTransformedBoundsForTranslateByX(xMove);
        updateLastTransformedBoundsForTranslateByZ(zMove);

        keepOnBedXZ();
        checkOffBed();

    }

    public ModelBounds getTransformedBounds()
    {
        return lastTransformedBounds;
    }

    public ModelBounds getLocalBounds()
    {
        return originalModelBounds;
    }

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
        notifyScreenExtentsChange();
    }

    /**
     * This method checks if the model is off the print bed and if so it adjusts
     * the transformMoveToPreferred to bring it back to the nearest edge of the
     * bed.
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
        notifyScreenExtentsChange();
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

    public void centreObjectOnBed()
    {
        transformMoveToPreferred.setX(0);
        transformMoveToPreferred.setZ(0);
    }

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
            setXScale(scaling);
            setYScale(scaling);
            setZScale(scaling);
        }

    }

    public void setCollision(boolean hasCollided)
    {
        this.isCollided = hasCollided;
        updateMaterial();
    }

    private void updateMaterial()
    {
        for (MeshView meshView : getMeshViews())
        {
            if (isOffBed.get())
            {
                meshView.setMaterial(ApplicationMaterials.getOffBedModelMaterial());
            } else if (isCollided)
            {
                meshView.setMaterial(ApplicationMaterials.getCollidedModelMaterial());
            } else
            {
                //TODO sort me out!
//                meshView.setMaterial(material);
            }
        }
    }

    public boolean isCollided()
    {
        return isCollided;
    }

    public void setModelName(String modelName)
    {
        this.modelName.set(modelName);
    }

    public String getModelName()
    {
        return modelName.get();
    }

    /**
     * Rotate the model in Lean and Twist so that the chosen face is pointing
     * down (ie aligned with the Y axis). Lean is easy to get, and we then use
     * an optimiser to establish Twist.
     *
     * @param snapFaceIndex
     */
    public void snapToGround(MeshView pickedMesh, int snapFaceIndex)
    {
        Vector3D faceNormal = getFaceNormal(pickedMesh, snapFaceIndex);
        Vector3D downVector = new Vector3D(0, 1, 0);

        Rotation requiredRotation = new Rotation(faceNormal, downVector);

        /**
         * get angle that Y is moved through, to give RL (lean rotation).
         */
        Vector3D yPrime = requiredRotation.applyTo(new Vector3D(0, -1, 0));
        Vector3D Y = new Vector3D(0, -1, 0);
        double leanAngle = Vector3D.angle(yPrime, Y);
        setRotationLean(Math.toDegrees(leanAngle));

        if (Math.abs(leanAngle - 180) < 0.02)
        {
            // no twist required, we can stop here
            return;
        }

        // Calculate twist using an optimizer (typically needs less than 30 iterations in
        // this example)
        long start = System.nanoTime();
        BrentOptimizer optimizer = new BrentOptimizer(1e-3, 1e-4);
        UnivariatePointValuePair pair = optimizer.optimize(new MaxEval(70),
                new UnivariateObjectiveFunction(
                        new ApplyTwist(pickedMesh, snapFaceIndex)),
                GoalType.MINIMIZE,
                new SearchInterval(0, 360));
        steno.debug("optimiser took " + (int) ((System.nanoTime() - start) * 10e-6) + " ms"
                + " and "
                + optimizer.getEvaluations() + " evaluations");
        setRotationTwist(pair.getPoint());

        dropToBedAndUpdateLastTransformedBounds();
    }

    private Point3D toPoint3D(Vector3D vector)
    {
        return new Point3D(vector.getX(), vector.getY(), vector.getZ());
    }

    private class ApplyTwist implements UnivariateFunction
    {

        final Vector3D faceNormal;
        final Vector3D faceCentre;

        public ApplyTwist(MeshView meshView, int faceIndex)
        {
            faceNormal = getFaceNormal(meshView, faceIndex);
            faceCentre = getFaceCentre(meshView, faceIndex);
        }

        Point3D getRotatedFaceNormal()
        {
            Point3D rotatedFaceCentre = getLocalToParentTransform().transform(
                    toPoint3D(faceCentre));

            Point3D rotatedFaceCentrePlusNormal = getLocalToParentTransform().transform(
                    toPoint3D(faceCentre.add(faceNormal)));

            Point3D rotatedFaceNormal = rotatedFaceCentrePlusNormal.subtract(rotatedFaceCentre);
            return rotatedFaceNormal;
        }

        @Override
        public double value(double twistDegrees)
        {
            // This value function returns how far off the resultant rotated face normal is
            // from the Y axis. The optimiser tries to minimise this function (i.e. align
            // rotated face normal with Y).
            setRotationTwist(twistDegrees);
            Point3D rotatedFaceNormal = getRotatedFaceNormal();
            double deviation = rotatedFaceNormal.angle(Y_AXIS);
            return deviation;
        }
    }

    private void updateScaleTransform()
    {

        transformScalePreferred.setPivotX(originalModelBounds.getCentreX());
        transformScalePreferred.setPivotY(originalModelBounds.getCentreY());
        transformScalePreferred.setPivotZ(originalModelBounds.getCentreZ());
        transformScalePreferred.setX(preferredXScale.get());
        transformScalePreferred.setY(preferredYScale.get());
        transformScalePreferred.setZ(preferredZScale.get());

        dropToBedAndUpdateLastTransformedBounds();
        checkOffBed();
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    public void setXScale(double scaleFactor)
    {
        preferredXScale.set(scaleFactor);
        updateScaleTransform();
    }

    public void setYScale(double scaleFactor)
    {
        preferredYScale.set(scaleFactor);
        updateScaleTransform();
    }

    public void setZScale(double scaleFactor)
    {
        preferredZScale.set(scaleFactor);
        updateScaleTransform();
    }

    /**
     * We present the rotations to the user as Lean - Twist - Turn, however in
     * the code they are applied in the order twist, lean, turn.
     */
    private void updateTransformsFromLeanTwistTurnAngles()
    {
        // Twist - around Y axis
        transformRotateTwistPreferred.setPivotX(originalModelBounds.getCentreX());
        transformRotateTwistPreferred.setPivotY(originalModelBounds.getCentreY());
        transformRotateTwistPreferred.setPivotZ(originalModelBounds.getCentreZ());
        transformRotateTwistPreferred.setAngle(preferredRotationTwist.get());
        transformRotateTwistPreferred.setAxis(Y_AXIS);

        // Lean - around Z axis
        transformRotateLeanPreferred.setPivotX(originalModelBounds.getCentreX());
        transformRotateLeanPreferred.setPivotY(originalModelBounds.getCentreY());
        transformRotateLeanPreferred.setPivotZ(originalModelBounds.getCentreZ());
        transformRotateLeanPreferred.setAngle(preferredRotationLean.get());
        transformRotateLeanPreferred.setAxis(Z_AXIS);

        // Turn - around Y axis
        transformRotateTurnPreferred.setPivotX(originalModelBounds.getCentreX());
        transformRotateTurnPreferred.setPivotY(originalModelBounds.getCentreY());
        transformRotateTurnPreferred.setPivotZ(originalModelBounds.getCentreZ());
        transformRotateTurnPreferred.setAngle(preferredRotationTurn.get());
        transformRotateTurnPreferred.setAxis(Y_AXIS);
    }

    /**
     *
     * @return
     */
    public double getXScale()
    {
        return preferredXScale.get();
    }

    public double getYScale()
    {
        return preferredYScale.get();
    }

    public double getZScale()
    {
        return preferredZScale.get();
    }

    public void setRotationTwist(double value)
    {
        preferredRotationTwist.set(value);
        updateTransformsFromLeanTwistTurnAngles();

        dropToBedAndUpdateLastTransformedBounds();
        checkOffBed();
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    public double getRotationTwist()
    {
        return preferredRotationTwist.get();
    }

    public void setRotationTurn(double value)
    {
        preferredRotationTurn.set(value);
        updateTransformsFromLeanTwistTurnAngles();

        dropToBedAndUpdateLastTransformedBounds();
        checkOffBed();
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    public double getRotationTurn()
    {
        return preferredRotationTurn.get();
    }

    public void setRotationLean(double value)
    {
        preferredRotationLean.set(value);
        updateTransformsFromLeanTwistTurnAngles();

        dropToBedAndUpdateLastTransformedBounds();
        checkOffBed();
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    public double getRotationLean()
    {
        return preferredRotationLean.get();
    }

    /**
     *
     * @param selected
     */
    public void setSelected(boolean selected)
    {
        if (selected)
        {
            if (selectionHighlighter == null)
            {
                addSelectionHighlighter();
            }
            showSelectedMarkers();
        } else
        {
            hideSelectedMarkers();
        }
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
        out.writeUTF(modelName.get());

        out.writeObject(modelContentsType);

        if (modelContentsType == ModelContentsEnumeration.MESH)
        {
            out.writeInt(numberOfMeshes);

            for (Node node : meshGroup.getChildren())
            {
                if (node instanceof MeshView)
                {
                    MeshView mesh = (MeshView) node;
                    TriangleMesh triMesh = (TriangleMesh) mesh.getMesh();

                    int[] smoothingGroups = triMesh.getFaceSmoothingGroups().toArray(null);
                    out.writeObject(smoothingGroups);

                    int[] faces = triMesh.getFaces().toArray(null);
                    out.writeObject(faces);

                    float[] points = triMesh.getPoints().toArray(null);
                    out.writeObject(points);
                }
            }
        } else
        {
//            out.writeInt(getChildren().size());
//            for (Node node : getChildren())
//            {
//                out.writeObject(node);
//            }
        }

        out.writeDouble(transformMoveToPreferred.getX());
        out.writeDouble(transformMoveToPreferred.getZ());
        out.writeDouble(getXScale());
        out.writeDouble(getRotationTwist());
        // not used (was snapFaceIndex)
        out.writeInt(0);
        for (int extCount = 0; extCount < meshExtruderAssociation.size(); extCount++)
        {
            out.writeInt(meshExtruderAssociation.get(extCount));
        }
        out.writeDouble(getYScale());
        out.writeDouble(getZScale());
        out.writeDouble(getRotationLean());
        out.writeDouble(getRotationTurn());
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
    {
        meshExtruderAssociation = FXCollections.emptyObservableList();

        String modelName = in.readUTF();
        meshGroup = new Group();
        getChildren().add(meshGroup);

        modelContentsType = (ModelContentsEnumeration) in.readObject();

        numberOfMeshes = in.readInt();

        for (int i = 0; i < numberOfMeshes; i++)
        {
            int[] smoothingGroups = (int[]) in.readObject();
            int[] faces = (int[]) in.readObject();
            float[] points = (float[]) in.readObject();

            TriangleMesh triMesh = new TriangleMesh();

            FloatArrayList texCoords = new FloatArrayList();
            texCoords.add(0f);
            texCoords.add(0f);

            triMesh.getPoints().addAll(points);
            triMesh.getTexCoords().addAll(texCoords.toFloatArray());
            triMesh.getFaces().addAll(faces);
            triMesh.getFaceSmoothingGroups().addAll(smoothingGroups);

            MeshView newMesh = new MeshView(triMesh);
            newMesh.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
            newMesh.setCullFace(CullFace.BACK);
            newMesh.setId(modelName + "_mesh");

            meshGroup.getChildren().add(newMesh);
        }

        initialise(new File(modelName));

        double storedX = in.readDouble();
        double storedZ = in.readDouble();
        double storedScaleX = in.readDouble();
        double storedRotationTwist = in.readDouble();
        int notUsed = in.readInt();

        double storedScaleY = storedScaleX;
        double storedScaleZ = storedScaleX;
        double storedRotationLean = 0d;
        double storedRotationTurn = 0d;
        if (in.available() > 0)
        {
            // Introduced in version 1.??
            for (int i = 0; i < numberOfMeshes; i++)
            {
                meshExtruderAssociation.add(in.readInt());
            }
            storedScaleY = in.readDouble();
            storedScaleZ = in.readDouble();
            storedRotationLean = in.readDouble();
            storedRotationTurn = in.readDouble();
        }

        initialiseTransforms();

        transformMoveToPreferred.setX(storedX);
        transformMoveToPreferred.setZ(storedZ);
        setXScale(storedScaleX);
        setYScale(storedScaleY);
        setZScale(storedScaleZ);
        setRotationLean(storedRotationLean);
        setRotationTwist(storedRotationTwist);
        setRotationTurn(storedRotationTurn);

        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    private void readObjectNoData()
            throws ObjectStreamException
    {

    }

    /**
     *
     * @return
     */
    public List<MeshView> getMeshViews()
    {
        List<MeshView> meshViews = meshGroup.getChildrenUnmodifiable().stream()
                .filter(node -> node instanceof MeshView)
                .map(MeshView.class::cast)
                .collect(Collectors.toList());

        return meshViews;
    }

    /**
     *
     * @return
     */
    public ObservableList<Node> getMeshes()
    {
        if (modelContentsType == ModelContentsEnumeration.MESH)
        {
            return meshGroup.getChildren();
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
     * @param width
     */
    public void resizeWidth(double width)
    {
        ModelBounds bounds = getLocalBounds();

        double originalWidth = bounds.getWidth();

        double newScale = width / originalWidth;
        setXScale(newScale);
        notifyShapeChange();
        notifyScreenExtentsChange();
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

        setYScale(newScale);
        notifyShapeChange();
        notifyScreenExtentsChange();
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

        setZScale(newScale);
        notifyShapeChange();
        notifyScreenExtentsChange();
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
        notifyScreenExtentsChange();
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
        notifyScreenExtentsChange();
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

    /**
     * Calculate max/min X,Y,Z before the transforms have been applied (ie the
     * original model dimensions before any transforms).
     */
    private ModelBounds calculateBounds()
    {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        for (MeshView meshView : getMeshViews())
        {
            TriangleMesh mesh = (TriangleMesh) meshView.getMesh();
            ObservableFloatArray originalPoints = mesh.getPoints();

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
     * Calculate max/min X,Y,Z after the transforms have been applied (ie in the
     * parent node).
     */
    public ModelBounds calculateBoundsInParent()
    {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        for (MeshView meshView : getMeshViews())
        {
            TriangleMesh mesh = (TriangleMesh) meshView.getMesh();
            ObservableFloatArray originalPoints = mesh.getPoints();

            for (int pointOffset = 0; pointOffset < originalPoints.size(); pointOffset += 3)
            {
                float xPos = originalPoints.get(pointOffset);
                float yPos = originalPoints.get(pointOffset + 1);
                float zPos = originalPoints.get(pointOffset + 2);

                Point3D pointInParent = localToParent(meshGroup.localToParent(xPos, yPos, zPos));

                minX = Math.min(pointInParent.getX(), minX);
                minY = Math.min(pointInParent.getY(), minY);
                minZ = Math.min(pointInParent.getZ(), minZ);

                maxX = Math.max(pointInParent.getX(), maxX);
                maxY = Math.max(pointInParent.getY(), maxY);
                maxZ = Math.max(pointInParent.getZ(), maxZ);
            }
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
     * THIS METHOD IS NOT CURRENTLY IN USE PROBABLY SHOULD BE BINNED IN FAVOUR
     * OF AN APPROACH SIMILAR TO THE SPLIT FUNCTION
     *
     * @return
     */
    public ArrayList<ModelContainer> cutToSize()
    {
        TriangleMesh mesh = (TriangleMesh) getMeshViews().get(0).getMesh();
        ObservableFaceArray originalFaces = mesh.getFaces();
        ObservableFloatArray originalPoints = mesh.getPoints();

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

            ModelContainer modelContainer = new ModelContainer(modelFile, meshView);

            outputMeshes.add(modelContainer);
        }

        return outputMeshes;
    }

    /**
     * This compareTo implementation compares based on the overall size of the
     * model.
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
        double totalwidth = originalModelBounds.getWidth() * preferredXScale.get();
        return totalwidth;
    }

    /**
     *
     * @return
     */
    public double getTotalDepth()
    {
        double totaldepth = originalModelBounds.getDepth() * preferredZScale.get();
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

    /**
     * Return the face normal for the face of the given index.
     *
     */
    Vector3D getFaceNormal(MeshView meshView, int faceNumber) throws MathArithmeticException
    {
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

    Vector3D getFaceCentre(MeshView meshView, int faceNumber)
    {
        TriangleMesh triMesh = (TriangleMesh) meshView.getMesh();
        int baseFaceIndex = faceNumber * 6;
        int v1PointIndex = triMesh.getFaces().get(baseFaceIndex);
        int v2PointIndex = triMesh.getFaces().get(baseFaceIndex + 2);
        int v3PointIndex = triMesh.getFaces().get(baseFaceIndex + 4);
        ObservableFloatArray points = triMesh.getPoints();
        Vector3D v1 = convertToVector3D(points, v1PointIndex);
        Vector3D v2 = convertToVector3D(points, v2PointIndex);
        Vector3D v3 = convertToVector3D(points, v3PointIndex);

        return new Vector3D((v1.getX() + v2.getX() + v3.getX()) / 3.0d,
                (v1.getY() + v2.getY() + v3.getY()) / 3.0d,
                (v1.getZ() + v2.getZ() + v3.getZ()) / 3.0d);
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

    public Translate getTransformMoveToCentre()
    {
        return transformMoveToCentre;
    }

    private void dropToBedAndUpdateLastTransformedBounds()
    {
        // Correct transformRotateSnapToGroundYAdjust for change in height (Y)
        transformPostRotationYAdjust.setY(0);
        ModelBounds modelBoundsParent = calculateBoundsInParent();
        transformPostRotationYAdjust.setY(-modelBoundsParent.getMaxY());
        lastTransformedBounds = calculateBoundsInParent();
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

    @Override
    public double getOriginalHeight()
    {
        return getLocalBounds().getHeight();
    }

    @Override
    public double getScaledHeight()
    {
        return getLocalBounds().getHeight() * preferredYScale.doubleValue();
    }

    @Override
    public double getOriginalDepth()
    {
        return getLocalBounds().getDepth();
    }

    @Override
    public double getScaledDepth()
    {
        return getLocalBounds().getDepth() * preferredZScale.doubleValue();
    }

    @Override
    public double getOriginalWidth()
    {
        return getTransformedBounds().getWidth();
    }

    @Override
    public double getScaledWidth()
    {
        return getLocalBounds().getWidth() * preferredXScale.doubleValue();
    }

    public void addSelectionHighlighter()
    {
        selectionHighlighter = new SelectionHighlighter(this);
        getChildren().add(selectionHighlighter);
        selectedMarkers.add(selectionHighlighter);
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    private void updateLastTransformedBoundsForTranslateByX(double deltaCentreX)
    {
        lastTransformedBounds.translateX(deltaCentreX);
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    private void updateLastTransformedBoundsForTranslateByZ(double deltaCentreZ)
    {
        lastTransformedBounds.translateZ(deltaCentreZ);
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    private void showSelectedMarkers()
    {
        for (Node selectedMarker : selectedMarkers)
        {
            selectedMarker.setVisible(true);
        }
    }

    private void hideSelectedMarkers()
    {
        for (Node selectedMarker : selectedMarkers)
        {
            selectedMarker.setVisible(false);
        }
    }

    @Override
    public void addShapeChangeListener(ShapeChangeListener listener)
    {
        shapeChangeListeners.add(listener);
    }

    @Override
    public void removeShapeChangeListener(ShapeChangeListener listener)
    {
        shapeChangeListeners.remove(listener);
    }

    /**
     * This method must be called at the end of any operation that changes one
     * or more of the transforms.
     */
    private void notifyShapeChange()
    {
        for (ShapeChangeListener shapeChangeListener : shapeChangeListeners)
        {
            shapeChangeListener.shapeChanged(this);
        }
    }

    /**
     * @param preferredScale the preferredScale to set
     */
    public void setPreferredScale(double preferredScale)
    {
        this.preferredXScale.set(preferredScale);
        setXScale(preferredScale);
    }

    public DoubleProperty preferredScaleProperty()
    {
        return preferredXScale;
    }

    public Point3D transformMeshToRealWorldCoordinates(float vertexX, float vertexY, float vertexZ)
    {
        return localToParent(meshGroup.localToParent(vertexX, vertexY, vertexZ));
    }

    public ObservableList<Integer> getMeshExtruderAssociationProperty()
    {
        return meshExtruderAssociation;
    }

    /**
     * If this model is associated with the given extruder number then recolour
     * it to the given colour.
     *
     * @param displayColourExtruder0
     * @param displayColourExtruder1
     */
    public void setColour(final Color displayColourExtruder0, final Color displayColourExtruder1)
    {
        for (int meshNumber = 0; meshNumber < getMeshViews().size(); meshNumber++)
        {
            MeshView mesh = getMeshViews().get(meshNumber);
            switch (meshExtruderAssociation.get(meshNumber))
            {
                case 0:
                    if (displayColourExtruder0 == null)
                    {
                        mesh.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
                    } else
                    {
                        mesh.setMaterial(getMaterialForColour(displayColourExtruder0));
                    }
                    break;
                case 1:
                    if (displayColourExtruder1 == null)
                    {
                        mesh.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
                    } else
                    {
                        mesh.setMaterial(getMaterialForColour(displayColourExtruder1));
                    }
                    break;
                default:
                    mesh.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
                    break;
            }
        }
    }

    private PhongMaterial getMaterialForColour(Color displayColourExtruder)
    {
        PhongMaterial meshMaterial = new PhongMaterial(displayColourExtruder);
        if (displayColourExtruder.equals(Color.BLACK))
        {
            meshMaterial.setSpecularColor(Color.DARKGRAY);
            meshMaterial.setSpecularPower(20);
            meshMaterial.setDiffuseColor(new Color(0.1, 0.1, 0.1, 1));
        }
        return meshMaterial;
    }

    @Override
    public void addScreenExtentsChangeListener(ScreenExtentsListener listener)
    {
        screenExtentsChangeListeners.add(listener);
    }

    @Override
    public void removeScreenExtentsChangeListener(ScreenExtentsListener listener)
    {
        screenExtentsChangeListeners.remove(listener);
    }

    private void notifyScreenExtentsChange()
    {
        for (ScreenExtentsListener screenExtentsListener : screenExtentsChangeListeners)
        {
            screenExtentsListener.screenExtentsChanged(this);
        }
    }

    @Override
    public ScreenExtents getScreenExtents()
    {
        double halfWidth = getScaledWidth() / 2;
        double halfDepth = getScaledDepth() / 2;
        double halfHeight = getScaledHeight() / 2;
        double minX = getCentreX() - halfWidth;
        double maxX = getCentreX() + halfWidth;
        double minZ = getCentreZ() - halfDepth;
        double maxZ = getCentreZ() + halfDepth;
        double minY = getCentreY() - halfHeight;
        double maxY = getCentreY() + halfHeight;

        Point2D frontLeftBottom = localToScreen(minX, maxY, minZ);
        Point2D frontRightBottom = localToScreen(maxX, maxY, minZ);
        Point2D backLeftBottom = localToScreen(minX, maxY, maxZ);
        Point2D backRightBottom = localToScreen(maxX, maxY, maxZ);
        Point2D frontLeftTop = localToScreen(minX, minY, minZ);
        Point2D frontRightTop = localToScreen(maxX, minY, minZ);
        Point2D backLeftTop = localToScreen(minX, minY, maxZ);
        Point2D backRightTop = localToScreen(maxX, minY, maxZ);

        ScreenExtents extents = new ScreenExtents();
        extents.heightEdges[0] = new Edge(frontLeftBottom, frontLeftTop);
        extents.heightEdges[1] = new Edge(frontRightBottom, frontRightTop);
        extents.heightEdges[2] = new Edge(backLeftBottom, backLeftTop);
        extents.heightEdges[3] = new Edge(backRightBottom, backRightTop);

        extents.widthEdges[0] = new Edge(frontLeftBottom, frontRightBottom);
        extents.widthEdges[1] = new Edge(backLeftBottom, backRightBottom);
        extents.widthEdges[2] = new Edge(frontLeftTop, frontRightTop);
        extents.widthEdges[3] = new Edge(backLeftTop, backRightTop);

        extents.depthEdges[0] = new Edge(frontLeftBottom, backLeftBottom);
        extents.depthEdges[1] = new Edge(frontRightBottom, backRightBottom);
        extents.depthEdges[2] = new Edge(frontLeftTop, backLeftTop);
        extents.depthEdges[3] = new Edge(frontRightTop, backRightTop);

        return extents;
    }

    @Override
    public double getTransformedHeight()
    {
        return getScaledHeight();
    }

    @Override
    public double getTransformedWidth()
    {
        return getScaledWidth();
    }

    @Override
    public double getTransformedDepth()
    {
        return getScaledDepth();
    }

    @Override
    public void cameraViewOfYouHasChanged()
    {
        notifyScreenExtentsChange();
    }

    public void addChildNodes(ObservableList<Node> nodes)
    {
        meshGroup.getChildren().addAll(nodes);
    }

    public ObservableList<Node> getMeshGroupChildren()
    {
        return meshGroup.getChildren();

    }

    /**
     * State captures the state of all the transforms being applied to this
     * ModelContainer. It is used as an efficient way of applying Undo and Redo
     * to changes to a Set of ModelContainers.
     */
    public class State
    {

        public int modelId;
        public double x;
        public double z;
        public double preferredXScale;
        public double preferredYScale;
        public double preferredZScale;
        public double preferredRotationTwist;
        public double preferredRotationTurn;
        public double preferredRotationLean;

        public State(int modelId, double x, double z,
                double preferredXScale, double preferredYScale, double preferredZScale,
                double preferredRotationTwist, double preferredRotationTurn,
                double preferredRotationLean)
        {
            this.modelId = modelId;
            this.x = x;
            this.z = z;
            this.preferredXScale = preferredXScale;
            this.preferredYScale = preferredYScale;
            this.preferredZScale = preferredZScale;
            this.preferredRotationTwist = preferredRotationTwist;
            this.preferredRotationTurn = preferredRotationTurn;
            this.preferredRotationLean = preferredRotationLean;
        }

        /**
         * The assignment operator.
         */
        public void assignFrom(State fromState)
        {
            this.x = fromState.x;
            this.z = fromState.z;
            this.preferredXScale = fromState.preferredXScale;
            this.preferredYScale = fromState.preferredYScale;
            this.preferredZScale = fromState.preferredZScale;
            this.preferredRotationTwist = fromState.preferredRotationTwist;
            this.preferredRotationTurn = fromState.preferredRotationTurn;
            this.preferredRotationLean = fromState.preferredRotationLean;
        }

    }

    public State getState()
    {
        return new State(modelId,
                transformMoveToPreferred.getX(),
                transformMoveToPreferred.getZ(),
                preferredXScale.get(), preferredYScale.get(), preferredZScale.get(),
                preferredRotationTwist.get(), preferredRotationTurn.get(),
                preferredRotationLean.get());
    }

    public void setState(State state)
    {
        transformMoveToPreferred.setX(state.x);
        transformMoveToPreferred.setZ(state.z);
        setXScale(state.preferredXScale);
        setYScale(state.preferredYScale);
        setZScale(state.preferredZScale);
        setRotationLean(state.preferredRotationLean);
        setRotationTwist(state.preferredRotationTwist);
        setRotationTurn(state.preferredRotationTurn);
    }

    public int getModelId()
    {
        return modelId;
    }
}
