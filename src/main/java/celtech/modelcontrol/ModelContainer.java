package celtech.modelcontrol;

import celtech.configuration.PrintBed;
import celtech.coreUI.visualisation.ApplicationMaterials;
import celtech.coreUI.visualisation.CameraViewChangeListener;
import celtech.coreUI.visualisation.Edge;
import celtech.coreUI.visualisation.ScreenExtents;
import celtech.coreUI.visualisation.ScreenExtentsProvider;
import celtech.coreUI.visualisation.ShapeProvider;
import celtech.coreUI.visualisation.Xform;
import celtech.coreUI.visualisation.metaparts.FloatArrayList;
import celtech.coreUI.visualisation.metaparts.IntegerArrayList;
import celtech.coreUI.visualisation.modelDisplay.ModelBounds;
import celtech.coreUI.visualisation.modelDisplay.SelectionHighlighter;
import celtech.utils.Math.MathUtils;
import celtech.utils.threed.MeshCutter2;
import celtech.utils.threed.MeshSeparator;
import com.sun.javafx.scene.CameraHelper;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Camera;
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
import javafx.scene.transform.Transform;
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
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ModelContainer extends Group implements Serializable, Comparable, ShapeProvider,
        ScreenExtentsProvider, CameraViewChangeListener
{

    private static final long serialVersionUID = 1L;
    protected static int nextModelId = 1;
    /**
     * The modelId is only guaranteed unique at the project level because it
     * could be reloaded with duplicate values from saved models into other
     * projects.
     */
    protected int modelId;
    private Stenographer steno;
    private PrintBed printBed;
    private boolean isCollided = false;
    private BooleanProperty isSelected;
    private BooleanProperty isOffBed;
    private SimpleStringProperty modelName;
    private boolean isInvalidMesh = false;

    ModelBounds originalModelBounds;

    protected Scale transformScalePreferred;
    private Translate transformDropToBedYAdjust;
    private static final Point3D Y_AXIS = new Point3D(0, 1, 0);
    private static final Point3D Z_AXIS = new Point3D(0, 0, 1);
    private static final Point3D X_AXIS = new Point3D(1, 0, 0);
    private Rotate transformRotateTwistPreferred;
    private Rotate transformRotateTurnPreferred;
    private Rotate transformRotateLeanPreferred;
    private Translate transformMoveToPreferred;
    private Translate transformBedCentre;

    private MeshView meshView;

    /**
     * Property wrapper around the scale.
     */
    private DoubleProperty preferredXScale;
    private DoubleProperty preferredYScale;
    private DoubleProperty preferredZScale;
    /**
     * Property wrappers around the rotations.
     */
    private DoubleProperty preferredRotationTwist;
    private DoubleProperty preferredRotationLean;
    private DoubleProperty preferredRotationTurn;

    private double bedCentreOffsetX;
    private double bedCentreOffsetY;
    private double bedCentreOffsetZ;

    /**
     * The bounds of the object in its parent. For top level objects this is
     * also the bounds in the bed coordinates. They are kept valid even after
     * translates etc.
     */
    protected ModelBounds lastTransformedBoundsInParent;
    private SelectionHighlighter selectionHighlighter;
    private List<ShapeProvider.ShapeChangeListener> shapeChangeListeners;
    private List<ScreenExtentsProvider.ScreenExtentsListener> screenExtentsChangeListeners;

    /**
     * Print the part using the extruder of the given number.
     */
    protected IntegerProperty associateWithExtruderNumber = new SimpleIntegerProperty(0);

    private File modelFile;

    private Node bed;

    private Camera cameraViewingMe = null;
    private ScreenExtents extents = new ScreenExtents();

    public ModelContainer()
    {
    }

    public ModelContainer(File modelFile, MeshView meshView)
    {
        this.meshView = meshView;
        getChildren().add(meshView);

        initialise(modelFile);
        initialiseTransforms();
    }

    public ModelContainer(File modelFile, MeshView meshView, int extruderAssociation)
    {
        this(modelFile, meshView);
        associateWithExtruderNumber.set(extruderAssociation);
    }

    /**
     * Return the parent ModelGroup else return null.
     */
    public ModelContainer getParentModelContainer()
    {
        if (getParent() instanceof ModelContainer)
        {
            return (ModelContainer) getParent();
        } else if (getParent() != null && getParent().getParent() instanceof ModelContainer)
        {
            return (ModelContainer) getParent().getParent();
        } else
        {
            return null;
        }
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
        getChildren().clear();
    }

    public void setUseExtruder0(boolean useExtruder0)
    {
        associateWithExtruderNumber.set(useExtruder0 ? 0 : 1);
    }

    public void printTransforms()
    {
        System.out.println("Transforms for: " + getId());
        System.out.println("==============================================");
        System.out.println("transformscalepreferred is " + transformScalePreferred);
        System.out.println("transformMovetopreferred is " + transformMoveToPreferred);
        System.out.println("transformSnapToGroundYAdjust is " + transformDropToBedYAdjust);
        System.out.println("transformRotateLeanPreferred is " + transformRotateLeanPreferred);
        System.out.println("transformRotateTwistPreferred " + transformRotateTwistPreferred);
        System.out.println("transformRotateTurnPreferred " + transformRotateTurnPreferred);
        System.out.println("transformBedCentre " + transformBedCentre);
        System.out.println("==============================================");
    }

    protected void initialiseTransforms()
    {
        transformScalePreferred = new Scale(1, 1, 1);
        transformDropToBedYAdjust = new Translate(0, 0, 0);
        transformRotateLeanPreferred = new Rotate(0, 0, 0, 0, X_AXIS);
        transformRotateTwistPreferred = new Rotate(0, 0, 0, 0, Y_AXIS);
        transformRotateTurnPreferred = new Rotate(0, 0, 0, 0, Z_AXIS);
        transformMoveToPreferred = new Translate(0, 0, 0);
        transformBedCentre = new Translate(0, 0, 0);

        setBedCentreOffsetTransform();

        /**
         * Rotations (which are all around the centre of the model) must be
         * applied before any translations.
         */
        getTransforms().addAll(transformDropToBedYAdjust,
                transformMoveToPreferred,
                transformBedCentre,
                transformRotateTurnPreferred,
                transformRotateLeanPreferred,
                transformRotateTwistPreferred
        );

        if (meshView != null)
        {
            meshView.getTransforms().addAll(transformScalePreferred);
        }

        updateOriginalModelBounds();

        lastTransformedBoundsInParent = calculateBoundsInParentCoordinateSystem();

        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    void updateOriginalModelBounds()
    {
        originalModelBounds = calculateBoundsInLocal();
        setScalePivotToCentreOfModel();
        setRotationPivotsToCentreOfModel();
    }

    void setScalePivotToCentreOfModel()
    {
        transformScalePreferred.setPivotX(getBoundsInLocal().getMinX()
                + getBoundsInLocal().getWidth() / 2.0);
        transformScalePreferred.setPivotY(getBoundsInLocal().getMinY()
                + getBoundsInLocal().getHeight() / 2.0);
        transformScalePreferred.setPivotZ(getBoundsInLocal().getMinZ()
                + getBoundsInLocal().getDepth() / 2.0);
    }

    void setRotationPivotsToCentreOfModel()
    {
        transformRotateLeanPreferred.setPivotX(originalModelBounds.getCentreX());
        transformRotateLeanPreferred.setPivotY(originalModelBounds.getCentreY());
        transformRotateLeanPreferred.setPivotZ(originalModelBounds.getCentreZ());

        transformRotateTwistPreferred.setPivotX(originalModelBounds.getCentreX());
        transformRotateTwistPreferred.setPivotY(originalModelBounds.getCentreY());
        transformRotateTwistPreferred.setPivotZ(originalModelBounds.getCentreZ());

        transformRotateTurnPreferred.setPivotX(originalModelBounds.getCentreX());
        transformRotateTurnPreferred.setPivotY(originalModelBounds.getCentreY());
        transformRotateTurnPreferred.setPivotZ(originalModelBounds.getCentreZ());
    }

    public void moveToCentre()
    {
        translateTo(bedCentreOffsetX, bedCentreOffsetZ);

        lastTransformedBoundsInParent = calculateBoundsInParentCoordinateSystem();
    }

    protected void initialise(File modelFile)
    {
        this.modelFile = modelFile;
        modelId = nextModelId;
        nextModelId += 1;

        shapeChangeListeners = new ArrayList<>();
        screenExtentsChangeListeners = new ArrayList<>();
        steno = StenographerFactory.getStenographer(ModelContainer.class.getName());
        printBed = PrintBed.getInstance();

        isSelected = new SimpleBooleanProperty(false);
        isOffBed = new SimpleBooleanProperty(false);

        if (modelFile != null)
        {
            modelName = new SimpleStringProperty(modelFile.getName());
            this.setId(modelFile.getName() + Integer.toString(modelId));
        } else
        {
            modelName = new SimpleStringProperty("group " + modelId);
            this.setId("group " + modelId);
        }

        preferredXScale = new SimpleDoubleProperty(1);
        preferredYScale = new SimpleDoubleProperty(1);
        preferredZScale = new SimpleDoubleProperty(1);
        preferredRotationLean = new SimpleDoubleProperty(0);
        preferredRotationTwist = new SimpleDoubleProperty(0);
        preferredRotationTurn = new SimpleDoubleProperty(0);

    }

    void clearBedTransform()
    {
        updateLastTransformedBoundsInParentForTranslateByX(-bedCentreOffsetX);
        updateLastTransformedBoundsInParentForTranslateByZ(-bedCentreOffsetZ);
        transformBedCentre.setX(0);
        transformBedCentre.setY(0);
        transformBedCentre.setZ(0);
    }

    private Translate currenttransformBedCentre;

    double currentDropY;

    public void saveAndClearDropToBedYTransform()
    {
        currentDropY = transformDropToBedYAdjust.getY();
        transformDropToBedYAdjust.setY(0);
    }

    public void restoreDropToBedYTransform()
    {
        transformDropToBedYAdjust.setY(currentDropY);
    }

    public void saveAndClearBedTransform()
    {
        currenttransformBedCentre = transformBedCentre.clone();
        transformBedCentre.setX(0);
        transformBedCentre.setY(0);
        transformBedCentre.setZ(0);
    }

    public void restoreBedTransform()
    {
        transformBedCentre.setX(currenttransformBedCentre.getX());
        transformBedCentre.setY(currenttransformBedCentre.getY());
        transformBedCentre.setZ(currenttransformBedCentre.getZ());
    }

    /**
     * Set transformBedCentre according to the position of the centre of the
     * bed.
     */
    public void setBedCentreOffsetTransform()
    {
        bedCentreOffsetX = PrintBed.getPrintVolumeCentreZeroHeight().getX();
        bedCentreOffsetY = PrintBed.getPrintVolumeCentreZeroHeight().getY();
        bedCentreOffsetZ = PrintBed.getPrintVolumeCentreZeroHeight().getZ();
        transformBedCentre.setX(bedCentreOffsetX);
        transformBedCentre.setY(bedCentreOffsetY);
        transformBedCentre.setZ(bedCentreOffsetZ);
        updateLastTransformedBoundsInParentForTranslateByX(bedCentreOffsetX);
        updateLastTransformedBoundsInParentForTranslateByZ(bedCentreOffsetZ);
    }

    /**
     * Make a copy of this ModelContainer and return it.
     */
    public ModelContainer makeCopy()
    {
        MeshView newMeshView = new MeshView();
        newMeshView.setMesh(meshView.getMesh());
        newMeshView.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
        newMeshView.setCullFace(CullFace.BACK);
        newMeshView.setId(meshView.getId());

        ModelContainer copy = new ModelContainer(this.modelFile, newMeshView);
        copy.setUseExtruder0(associateWithExtruderNumber.get() == 0);
        copy.setState(this.getState());
        return copy;
    }

    public void translateBy(double xMove, double zMove)
    {

        transformMoveToPreferred.setX(transformMoveToPreferred.getX() + xMove);
        transformMoveToPreferred.setZ(transformMoveToPreferred.getZ() + zMove);

        updateLastTransformedBoundsInParentForTranslateByX(xMove);
        updateLastTransformedBoundsInParentForTranslateByZ(zMove);

        keepOnBedXZ();
    }

    public double getMoveToPreferredX()
    {
        return transformMoveToPreferred.getX();
    }

    public double getMoveToPreferredZ()
    {
        return transformMoveToPreferred.getZ();
    }

    public ModelBounds getLocalBounds()
    {
        return originalModelBounds;
    }

    /**
     * N.Bï¼ŽIt only works for top level objects ieï¼Žtop level groups or
     * ungrouped models.
     */
    public void translateFrontLeftTo(double xPosition, double zPosition)
    {
        double newXPosition = xPosition - bedCentreOffsetX
                + lastTransformedBoundsInParent.getWidth() / 2.0;
        double newZPosition = zPosition - bedCentreOffsetZ
                + lastTransformedBoundsInParent.getHeight() / 2.0;
        double deltaXPosition = newXPosition - transformMoveToPreferred.getX();
        double deltaZPosition = newZPosition - transformMoveToPreferred.getZ();
        translateBy(deltaXPosition, deltaZPosition);
    }

    /**
     * This method checks if the model is off the print bed and if so it adjusts
     * the transformMoveToPreferred to bring it back to the nearest edge of the
     * bed. N.B. It only works for top level objects i.e. top level groups or
     * ungrouped models.
     */
    private void keepOnBedXZ()
    {
        double deltaX = 0;

        double minBedX = PrintBed.getPrintVolumeCentre().getX() - PrintBed.maxPrintableXSize / 2.0
                + 1;
        double maxBedX = PrintBed.getPrintVolumeCentre().getX() + PrintBed.maxPrintableXSize / 2.0
                - 1;
        if (lastTransformedBoundsInParent.getMinX() < minBedX)
        {
            deltaX = -(lastTransformedBoundsInParent.getMinX() - minBedX);
            transformMoveToPreferred.setX(transformMoveToPreferred.getX() + deltaX);
        } else if (lastTransformedBoundsInParent.getMaxX() > maxBedX)
        {
            deltaX = -(lastTransformedBoundsInParent.getMaxX() - maxBedX);
            transformMoveToPreferred.setX(transformMoveToPreferred.getX() + deltaX);
        }
        updateLastTransformedBoundsInParentForTranslateByX(deltaX);

        double deltaZ = 0;
        double minBedZ = PrintBed.getPrintVolumeCentre().getZ() - PrintBed.maxPrintableZSize / 2.0
                + 1;
        double maxBedZ = PrintBed.getPrintVolumeCentre().getZ() + PrintBed.maxPrintableZSize / 2.0
                - 1;
        if (lastTransformedBoundsInParent.getMinZ() < minBedZ)
        {
            deltaZ = -(lastTransformedBoundsInParent.getMinZ() - minBedZ);
            transformMoveToPreferred.setZ(transformMoveToPreferred.getZ() + deltaZ);
        } else if (lastTransformedBoundsInParent.getMaxZ() > maxBedZ)
        {
            deltaZ = -(lastTransformedBoundsInParent.getMaxZ() - maxBedZ);
            transformMoveToPreferred.setZ(transformMoveToPreferred.getZ() + deltaZ);
        }
        updateLastTransformedBoundsInParentForTranslateByZ(deltaZ);

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

        lastTransformedBoundsInParent = calculateBoundsInParentCoordinateSystem();

    }

    public void setCollision(boolean hasCollided)
    {
        this.isCollided = hasCollided;
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
     * Return a set of all descendent ModelContainers (and include this one)
     * that have MeshView children.
     *
     * @return
     */
    public Set<ModelContainer> getModelsHoldingMeshViews()
    {
        Set<ModelContainer> modelsHoldingMeshViews = new HashSet<>();
        modelsHoldingMeshViews.add(this);
        return modelsHoldingMeshViews;
    }

    /**
     * Return a set of all descendent ModelContainers (and include this one)
     * that have ModelContainer children.
     */
    public Collection<? extends ModelContainer> getModelsHoldingModels()
    {
        Set<ModelContainer> modelsHoldingModels = new HashSet<>();
        return modelsHoldingModels;
    }

    public void addGroupStructure(Map<Integer, Set<Integer>> groupStructure)
    {
    }

    /**
     * Rotate the model in Lean and Twist so that the chosen face is pointing
     * down (ie aligned with the Y axis). Lean is easy to get, and we then use
     * an optimiser to establish Twist.
     *
     * @param snapFaceIndex
     */
    public void snapToGround(MeshView meshView, int snapFaceIndex)
    {
        Vector3D faceNormal = getFaceNormal(meshView, snapFaceIndex);
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
                        new ApplyTwist(meshView,
                                snapFaceIndex)),
                GoalType.MINIMIZE,
                new SearchInterval(0, 360));
        steno.debug("optimiser took " + (int) ((System.nanoTime() - start) * 10e-6) + " ms"
                + " and "
                + optimizer.getEvaluations() + " evaluations");
        setRotationTwist(pair.getPoint());

        dropToBed();
    }

    private static Point3D toPoint3D(Vector3D vector)
    {
        return new Point3D(vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * Return the maximum and minimum y values of the coordinates of this model
     * in bed coords.
     */
    public List<Float> getMaxAndMinYInBedCoords()
    {
        List<Float> maxMin = new ArrayList<>();
        ModelBounds modelBounds = calculateBoundsInBedCoordinateSystem();
        maxMin.add((float) modelBounds.getMaxY());
        maxMin.add((float) modelBounds.getMinY());
        return maxMin;
    }

    public void setBedReference(Node bed)
    {
        this.bed = bed;
    }

    private class ApplyTwist implements UnivariateFunction
    {

        final Vector3D faceNormal;
        final Vector3D faceCentre;
        Transform localToBedTransform;
        Node bed;

        public ApplyTwist(MeshView meshView, int faceIndex)
        {
            faceNormal = getFaceNormal(meshView, faceIndex);
            faceCentre = getFaceCentre(meshView, faceIndex);
            bed = getRootModelContainer(meshView).getParent();
        }

        Point3D getRotatedFaceNormal()
        {

            Point3D rotatedFaceCentre = bed.sceneToLocal(localToScene(toPoint3D(faceCentre)));

            Point3D rotatedFaceCentrePlusNormal = bed.sceneToLocal(localToScene(toPoint3D(
                    faceCentre.add(faceNormal))));

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
        dropToBed();
        checkOffBed();
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    public void setXScale(double scaleFactor)
    {
        preferredXScale.set(scaleFactor);
        transformScalePreferred.setX(scaleFactor);
        updateScaleTransform();
    }

    public void setYScale(double scaleFactor)
    {
        preferredYScale.set(scaleFactor);
        transformScalePreferred.setY(scaleFactor);
        updateScaleTransform();
    }

    public void setZScale(double scaleFactor)
    {
        preferredZScale.set(scaleFactor);
        transformScalePreferred.setZ(scaleFactor);
        updateScaleTransform();
    }

    /**
     * We present the rotations to the user as Lean - Twist - Turn.
     */
    private void updateTransformsFromLeanTwistTurnAngles()
    {
        // Twist - around object Y axis
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

        // Turn - around bed Y axis
        transformRotateTurnPreferred.setPivotX(originalModelBounds.getCentreX());
        transformRotateTurnPreferred.setPivotY(originalModelBounds.getCentreY());
        transformRotateTurnPreferred.setPivotZ(originalModelBounds.getCentreZ());
        transformRotateTurnPreferred.setAngle(preferredRotationTurn.get());
        transformRotateTurnPreferred.setAxis(Y_AXIS);
    }

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

        dropToBed();
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

        dropToBed();
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

        dropToBed();
        checkOffBed();
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    public double getRotationLean()
    {
        return preferredRotationLean.get();
    }

    public void setSelected(boolean selected)
    {
        if (selected)
        {
            if (selectionHighlighter == null)
            {
                addSelectionHighlighter();
            }
            showSelectionHighlighter();
        } else
        {
            hideSelectionHighlighter();
        }
        isSelected.set(selected);
    }

    public boolean isSelected()
    {
        return isSelected.get();
    }

    public BooleanProperty isSelectedProperty()
    {
        return isSelected;
    }

    private void writeObject(ObjectOutputStream out)
            throws IOException
    {
        out.writeUTF(modelName.get());

        // unused
        out.writeInt(0);

        TriangleMesh triMesh = (TriangleMesh) meshView.getMesh();

        int[] smoothingGroups = triMesh.getFaceSmoothingGroups().toArray(null);
        out.writeObject(smoothingGroups);

        int[] faces = triMesh.getFaces().toArray(null);
        out.writeObject(faces);

        float[] points = triMesh.getPoints().toArray(null);
        out.writeObject(points);

        out.writeDouble(transformMoveToPreferred.getX());
        out.writeDouble(transformMoveToPreferred.getZ());
        out.writeDouble(getXScale());
        out.writeDouble(getRotationTwist());
        // was not used (was snapFaceIndex) - now modelId
        out.writeInt(modelId);
        out.writeInt(associateWithExtruderNumber.get());
        out.writeDouble(getYScale());
        out.writeDouble(getZScale());
        out.writeDouble(getRotationLean());
        out.writeDouble(getRotationTurn());

        out.writeDouble(transformDropToBedYAdjust.getY());
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
    {
        associateWithExtruderNumber = new SimpleIntegerProperty(0);

        String modelName = in.readUTF();

        int numberOfMeshesNowUnused = in.readInt();

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

        meshView = new MeshView(triMesh);
        meshView.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
        meshView.setCullFace(CullFace.BACK);
        meshView.setId(modelName + "_mesh");

        getChildren().add(meshView);

        initialise(new File(modelName));

        double storedX = in.readDouble();
        double storedY = 0;
        double storedZ = in.readDouble();
        double storedScaleX = in.readDouble();
        double storedRotationTwist = in.readDouble();
        int storedSnapFaceIndexLegacy = in.readInt();
        int storedModelId = storedSnapFaceIndexLegacy;

        double storedScaleY = storedScaleX;
        double storedScaleZ = storedScaleX;
        double storedRotationLean = 0d;
        double storedRotationTurn = 0d;
        boolean convertSnapFace = false;
        if (in.available() > 0)
        {
            // Introduced in version 1.??
            associateWithExtruderNumber.set(in.readInt());
            storedScaleY = in.readDouble();
            storedScaleZ = in.readDouble();
            storedRotationLean = in.readDouble();
            storedRotationTurn = in.readDouble();
            if (storedModelId > 0)
            {
                modelId = storedModelId;
            }
        } else
        {
            convertSnapFace = true;
        }

        if (in.available() > 0)
        {
            storedY = in.readDouble();
        }

        initialiseTransforms();

        transformMoveToPreferred.setX(storedX);
        transformMoveToPreferred.setZ(storedZ);
        transformDropToBedYAdjust.setY(storedY);

        preferredXScale.set(storedScaleX);
        preferredYScale.set(storedScaleY);
        preferredZScale.set(storedScaleZ);
        preferredRotationLean.set(storedRotationLean);
        preferredRotationTwist.set(storedRotationTwist);
        preferredRotationTurn.set(storedRotationTurn);

        transformScalePreferred.setX(storedScaleX);
        transformScalePreferred.setY(storedScaleY);
        transformScalePreferred.setZ(storedScaleZ);

        updateTransformsFromLeanTwistTurnAngles();

        if (convertSnapFace)
        {
            snapToGround(meshView, storedSnapFaceIndexLegacy);
        }

        lastTransformedBoundsInParent = calculateBoundsInParentCoordinateSystem();

        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    private void readObjectNoData()
            throws ObjectStreamException
    {

    }

    public MeshView getMeshView()
    {
        return meshView;
    }

    public IntegerProperty getAssociateWithExtruderNumberProperty()
    {
        return associateWithExtruderNumber;
    }

    public void resizeWidth(double width)
    {
        ModelBounds bounds = getLocalBounds();

        double originalWidth = bounds.getWidth();

        double newScale = width / originalWidth;
        setXScale(newScale);
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    public void resizeHeight(double height)
    {
        ModelBounds bounds = getLocalBounds();

        double currentHeight = bounds.getHeight();

        double newScale = height / currentHeight;

        setYScale(newScale);
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

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
     * N.B. It only works for top level objects i.e. top level groups or
     * ungrouped models.
     */
    public void translateXTo(double xPosition)
    {
        ModelBounds bounds = lastTransformedBoundsInParent;

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

        updateLastTransformedBoundsInParentForTranslateByX(requiredTranslation);
        checkOffBed();
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    /**
     * N.B. It only works for top level objects i.e. top level groups or
     * ungrouped models.
     */
    public void translateZTo(double zPosition)
    {
        ModelBounds bounds = lastTransformedBoundsInParent;

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

        updateLastTransformedBoundsInParentForTranslateByZ(requiredTranslation);
        checkOffBed();
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    public double getYAdjust()
    {
        return transformDropToBedYAdjust.getY();
    }

    /**
     * This method is used during an ungroup to blend the group's transform into
     * this one, thereby keeping this model in the same place.
     */
    public void applyGroupTransformToThis(ModelGroup modelGroup)
    {
        double scaleFactor = getXScale() * modelGroup.getXScale();
        preferredXScale.set(scaleFactor);
        transformScalePreferred.setX(scaleFactor);

        scaleFactor = getYScale() * modelGroup.getYScale();
        preferredYScale.set(scaleFactor);
        transformScalePreferred.setY(scaleFactor);

        scaleFactor = getZScale() * modelGroup.getZScale();
        preferredZScale.set(scaleFactor);
        transformScalePreferred.setZ(scaleFactor);

        // if scale was applied then this is wrong. Scale of group has moved subgroup towards/away
        // from centre of group and up/down, which needs to be taken into account
        translateBy(modelGroup.getMoveToPreferredX(),
                modelGroup.getMoveToPreferredZ());

        transformDropToBedYAdjust.setY(modelGroup.getYAdjust());

    }

    /**
     * Check if this object is off the bed. N.B. It only works for top level
     * objects i.e. top level groups or ungrouped models.
     */
    public void checkOffBed()
    {
        ModelBounds bounds = lastTransformedBoundsInParent;

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
    }

    public BooleanProperty isOffBedProperty()
    {
        return isOffBed;
    }

    /**
     * Calculate max/min X,Y,Z before the transforms have been applied (ie the
     * original model dimensions before any transforms).
     */
    ModelBounds calculateBoundsInLocal()
    {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

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

    public Set<MeshView> descendentMeshViews()
    {
        Set<MeshView> descendentMeshViews = new HashSet<>();
        descendentMeshViews.add(meshView);

        return descendentMeshViews;
    }

    public static ModelContainer getRootModelContainer(MeshView meshView)
    {
        return (ModelContainer) ((ModelContainer) meshView.getParent()).getRootModelContainer();
    }

    public ModelContainer getRootModelContainer()
    {
        ModelContainer parentModelContainer = this;

        while (parentModelContainer.getParentModelContainer() instanceof ModelContainer)
        {
            parentModelContainer = parentModelContainer.getParentModelContainer();
        }
        return (ModelContainer) parentModelContainer;
    }

    /**
     * Return a BedToLocal converter for this ModelContainer. N.B. Before using
     * this the bed centre transform and dropToBed transform must be cleared,
     * otherwise it does not work for the purposes intended.
     */
    public MeshCutter2.BedToLocalConverter getBedToLocalConverter()
    {
        return new MeshCutter2.BedToLocalConverter()
        {

            @Override
            public Point3D localToBed(Point3D point)
            {
                Point3D pointScene = localToScene(point);
                Point3D pointBed = bed.sceneToLocal(pointScene);
                return pointBed;
            }

            @Override
            public Point3D bedToLocal(Point3D point)
            {
                Point3D pointScene = bed.localToScene(point);
                return sceneToLocal(pointScene);
            }
        };

    }

    /**
     * Calculate max/min X,Y,Z after all the transforms have been applied all
     * the way to the bed coordinate system.
     */
    public ModelBounds calculateBoundsInBedCoordinateSystem()
    {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        for (Node meshViewNode : descendentMeshViews())
        {
            MeshView meshView = (MeshView) meshViewNode;

            ModelContainer rootModelContainer = getRootModelContainer(meshView);

            TriangleMesh mesh = (TriangleMesh) meshView.getMesh();
            ObservableFloatArray originalPoints = mesh.getPoints();

            for (int pointOffset = 0; pointOffset < originalPoints.size(); pointOffset += 3)
            {
                float xPos = originalPoints.get(pointOffset);
                float yPos = originalPoints.get(pointOffset + 1);
                float zPos = originalPoints.get(pointOffset + 2);

                Point3D pointInScene = meshView.localToScene(xPos, yPos, zPos);

                Point3D pointInBed = rootModelContainer.localToParent(
                        rootModelContainer.sceneToLocal(pointInScene));
//                System.out.println("point is " + xPos + " " + yPos + " " + zPos + " in bed is "
//                    + pointInBed.toString());

                minX = Math.min(pointInBed.getX(), minX);
                minY = Math.min(pointInBed.getY(), minY);
                minZ = Math.min(pointInBed.getZ(), minZ);

                maxX = Math.max(pointInBed.getX(), maxX);
                maxY = Math.max(pointInBed.getY(), maxY);
                maxZ = Math.max(pointInBed.getZ(), maxZ);
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
    public ModelBounds calculateBoundsInParentCoordinateSystem()
    {

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        ModelContainer parentModelContainer = getParentModelContainer();
        if (parentModelContainer == null)
        {
            return calculateBoundsInBedCoordinateSystem();
        }

        for (Node meshViewNode : descendentMeshViews())
        {
            MeshView meshView = (MeshView) meshViewNode;

            TriangleMesh mesh = (TriangleMesh) meshView.getMesh();
            ObservableFloatArray originalPoints = mesh.getPoints();

            for (int pointOffset = 0; pointOffset < originalPoints.size(); pointOffset += 3)
            {
                float xPos = originalPoints.get(pointOffset);
                float yPos = originalPoints.get(pointOffset + 1);
                float zPos = originalPoints.get(pointOffset + 2);

                Point3D pointInScene = meshView.localToScene(xPos, yPos, zPos);
                Point3D pointInParent = parentModelContainer.sceneToLocal(pointInScene);

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
     * Try to split into parts. If the ModelContainer is composed of more than
     * one part then make a group of them.
     */
    public ModelContainer splitIntoParts()
    {
        Set<ModelContainer> parts = new HashSet<>();
        ModelContainer.State state = getState();
        String modelName = getModelName();

        ModelContainer modelContainer = this;

        List<TriangleMesh> subMeshes = MeshSeparator.separate((TriangleMesh) getMeshView().getMesh());
        if (subMeshes.size() > 1)
        {
            int ix = 1;
            for (TriangleMesh subMesh : subMeshes)
            {
                MeshView meshView = new MeshView(subMesh);
                ModelContainer newModelContainer = new ModelContainer(
                        getModelFile(), meshView);
                newModelContainer.setState(state);
                newModelContainer.associateWithExtruderNumber.set(associateWithExtruderNumber.get());
                parts.add(newModelContainer);

                newModelContainer.setModelName(modelName + " " + ix);

                ix++;
            }
            modelContainer = new ModelGroup(parts);
        }
        return modelContainer;
    }

    /**
     * THIS METHOD IS NOT CURRENTLY IN USE PROBABLY SHOULD BE BINNED IN FAVOUR
     * OF AN APPROACH SIMILAR TO THE SPLIT FUNCTION
     *
     * @return
     */
    public ArrayList<ModelContainer> cutToSize()
    {
        TriangleMesh mesh = (TriangleMesh) meshView.getMesh();
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

    public void dropToBed()
    {
        // Correct transformPostRotationYAdjust for change in height (Y)
        transformDropToBedYAdjust.setY(0);
        ModelBounds modelBoundsParent = calculateBoundsInBedCoordinateSystem();
        transformDropToBedYAdjust.setY(-modelBoundsParent.getMaxY());
        lastTransformedBoundsInParent = calculateBoundsInParentCoordinateSystem();
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
        return lastTransformedBoundsInParent.getCentreZ();
    }

    public double getTransformedCentreX()
    {
        return lastTransformedBoundsInParent.getCentreX();
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
        return getLocalBounds().getWidth();
    }

    @Override
    public double getScaledWidth()
    {
        return getLocalBounds().getWidth() * preferredXScale.doubleValue();
    }

    /**
     * Get the width on the bed but ONLY for top-level models.
     */
    public double getTotalWidth()
    {
        double totalwidth = lastTransformedBoundsInParent.getWidth();
        return totalwidth;
    }

    /**
     * Get the depth on the bed but ONLY for top-level models.
     */
    public double getTotalDepth()
    {
        double totaldepth = lastTransformedBoundsInParent.getDepth();
        return totaldepth;
    }

    /**
     * Get a relative measure of the total size on the bed but ONLY for
     * top-level models.
     */
    public double getTotalSize()
    {
        return getTotalWidth() + getTotalDepth();
    }

    public void addSelectionHighlighter()
    {
        selectionHighlighter = new SelectionHighlighter(this);
        getChildren().add(selectionHighlighter);
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    private void updateLastTransformedBoundsInParentForTranslateByX(double deltaCentreX)
    {
        if (lastTransformedBoundsInParent != null)
        {
            lastTransformedBoundsInParent.translateX(deltaCentreX);
        }
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    private void updateLastTransformedBoundsInParentForTranslateByZ(double deltaCentreZ)
    {
        if (lastTransformedBoundsInParent != null)
        {
            lastTransformedBoundsInParent.translateZ(deltaCentreZ);
        }
        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    private void showSelectionHighlighter()
    {

        selectionHighlighter.setVisible(true);
    }

    private void hideSelectionHighlighter()
    {
        selectionHighlighter.setVisible(false);
    }

    @Override
    public void addShapeChangeListener(ShapeProvider.ShapeChangeListener listener)
    {
        shapeChangeListeners.add(listener);
    }

    @Override
    public void removeShapeChangeListener(ShapeProvider.ShapeChangeListener listener)
    {
        shapeChangeListeners.remove(listener);
    }

    /**
     * This method must be called at the end of any operation that changes one
     * or more of the transforms.
     */
    private void notifyShapeChange()
    {
        for (ShapeProvider.ShapeChangeListener shapeChangeListener : shapeChangeListeners)
        {
            shapeChangeListener.shapeChanged(this);
        }
    }

    public Point3D transformMeshToRealWorldCoordinates(float vertexX, float vertexY, float vertexZ)
    {
        return bed.sceneToLocal(meshView.localToScene(vertexX, vertexY, vertexZ));
    }

    /**
     * If this model is associated with the given extruder number then recolour
     * it to the given colour, also taking into account if it is misplaced (off
     * the bed). Also call the same method on any child ModelContainers.
     */
    public void updateColour(final PhongMaterial extruder0Material, final PhongMaterial extruder1Material,
            boolean showMisplacedColour)
    {
        boolean offBed = getRootModelContainer().isOffBed.get();
        if (showMisplacedColour)
        {
            if (offBed)
            {
                meshView.setMaterial(ApplicationMaterials.getOffBedModelMaterial());
            } else if (isCollided)
            {
                meshView.setMaterial(ApplicationMaterials.getCollidedModelMaterial());
            }
        }
        if (!showMisplacedColour || (!offBed && !isCollided))
        {
            switch (associateWithExtruderNumber.get())
            {
                case 0:
                    if (extruder0Material == null)
                    {
                        meshView.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
                    } else
                    {
                        meshView.setMaterial(extruder0Material);
                    }
                    break;
                case 1:
                    if (extruder1Material == null)
                    {
                        meshView.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
                    } else
                    {
                        meshView.setMaterial(extruder1Material);
                    }
                    break;
                default:
                    meshView.setMaterial(ApplicationMaterials.getDefaultModelMaterial());
                    break;
            }
        }
    }

    @Override
    public void addScreenExtentsChangeListener(ScreenExtentsProvider.ScreenExtentsListener listener)
    {
        screenExtentsChangeListeners.add(listener);
    }

    @Override
    public void removeScreenExtentsChangeListener(
            ScreenExtentsProvider.ScreenExtentsListener listener)
    {
        screenExtentsChangeListeners.remove(listener);
    }

    private void notifyScreenExtentsChange()
    {
        recalculateScreenExtents();

        for (ScreenExtentsProvider.ScreenExtentsListener screenExtentsListener : screenExtentsChangeListeners)
        {
            screenExtentsListener.screenExtentsChanged(this);
        }
    }

    private void recalculateScreenExtents()
    {
        if (getLocalBounds() != null
                && cameraViewingMe != null)
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

//            Point2D frontLeftBottom = localToScreen(minX, maxY, minZ);
//            Point2D frontRightBottom = localToScreen(maxX, maxY, minZ);
//            Point2D backLeftBottom = localToScreen(minX, maxY, maxZ);
//            Point2D backRightBottom = localToScreen(maxX, maxY, maxZ);
//            Point2D frontLeftTop = localToScreen(minX, minY, minZ);
//            Point2D frontRightTop = localToScreen(maxX, minY, minZ);
//            Point2D backLeftTop = localToScreen(minX, minY, maxZ);
//            Point2D backRightTop = localToScreen(maxX, minY, maxZ);

//            Point3D frontLeftBottomScene = localToScene(minX, maxY, minZ);
//            Point3D frontRightBottomScene = localToScene(maxX, maxY, minZ);
//            Point3D backLeftBottomScene = localToScene(minX, maxY, maxZ);
//            Point3D backRightBottomScene = localToScene(maxX, maxY, maxZ);
//            Point3D frontLeftTopScene = localToScene(minX, minY, minZ);
//            Point3D frontRightTopScene = localToScene(maxX, minY, minZ);
//            Point3D backLeftTopScene = localToScene(minX, minY, maxZ);
//            Point3D backRightTopScene = localToScene(maxX, minY, maxZ);

            Point2D frontLeftBottom = CameraHelper.project(cameraViewingMe, localToScene(minX, maxY, minZ));
            Point2D frontRightBottom = CameraHelper.project(cameraViewingMe, localToScene(maxX, maxY, minZ));
            Point2D backLeftBottom = CameraHelper.project(cameraViewingMe, localToScene(minX, maxY, maxZ));
            Point2D backRightBottom = CameraHelper.project(cameraViewingMe, localToScene(maxX, maxY, maxZ));
            Point2D frontLeftTop = CameraHelper.project(cameraViewingMe, localToScene(minX, minY, minZ));
            Point2D frontRightTop = CameraHelper.project(cameraViewingMe, localToScene(maxX, minY, minZ));
            Point2D backLeftTop = CameraHelper.project(cameraViewingMe, localToScene(minX, minY, maxZ));
            Point2D backRightTop = CameraHelper.project(cameraViewingMe, localToScene(maxX, minY, maxZ));
//        Point3D frontLeftBottomCamera = cameraTransform.sceneToLocal(frontLeftBottomScene);
//        Point3D frontRightBottomCamera = cameraTransform.sceneToLocal(frontRightBottomScene);
//        Point3D backLeftBottomCamera = cameraTransform.sceneToLocal(backLeftBottomScene);
//        Point3D backRightBottomCamera = cameraTransform.sceneToLocal(backRightBottomScene);
//        Point3D frontLeftTopCamera = cameraTransform.sceneToLocal(frontLeftTopScene);
//        Point3D frontRightTopCamera = cameraTransform.sceneToLocal(frontRightTopScene);
//        Point3D backLeftTopCamera = cameraTransform.sceneToLocal(backLeftTopScene);
//        Point3D backRightTopCamera = cameraTransform.sceneToLocal(backRightTopScene);
//
//        Point2D frontLeftBottom = cameraTransform.localToScreen(frontLeftBottomCamera);
//        Point2D frontRightBottom = cameraTransform.localToScreen(frontRightBottomCamera);
//        Point2D backLeftBottom = cameraTransform.localToScreen(backLeftBottomCamera);
//        Point2D backRightBottom = cameraTransform.localToScreen(backRightBottomCamera);
//        Point2D frontLeftTop = cameraTransform.localToScreen(frontLeftTopCamera);
//        Point2D frontRightTop = cameraTransform.localToScreen(frontRightTopCamera);
//        Point2D backLeftTop = cameraTransform.localToScreen(backLeftTopCamera);
//        Point2D backRightTop = cameraTransform.localToScreen(backRightTopCamera);
            if (extents == null)
            {
                extents = new ScreenExtents();
            }
            if (frontLeftBottom != null)
            {
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

                extents.recalculateMaxMin();
            }
        }
    }

    @Override
    public ScreenExtents getScreenExtents()
    {
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
    public void heresYourCamera(Camera camera)
    {
        this.cameraViewingMe = camera;
    }

    @Override
    public void cameraViewOfYouHasChanged()
    {
        notifyScreenExtentsChange();
    }

    public void addChildNodes(ObservableList<Node> nodes)
    {
        getChildren().addAll(nodes);
    }

    public void addChildNode(Node node)
    {
        getChildren().add(node);
    }

    public ObservableList<Node> getMeshGroupChildren()
    {
        return getChildren();

    }

    public Set<ModelContainer> getChildModelContainers()
    {
        return Collections.EMPTY_SET;
    }

    public Set<ModelContainer> getDescendentModelContainers()
    {
        return Collections.EMPTY_SET;
    }

    /**
     * State captures the state of all the transforms being applied to this
     * ModelContainer. It is used as an efficient way of applying Undo and Redo
     * to changes to a Set of ModelContainers.
     */
    public static class State
    {

        public int modelId;
        public double x;
        public double y;
        public double z;
        public double preferredXScale;
        public double preferredYScale;
        public double preferredZScale;
        public double preferredRotationTwist;
        public double preferredRotationTurn;
        public double preferredRotationLean;

        @JsonCreator
        public State(
                @JsonProperty("modelId") int modelId,
                @JsonProperty("x") double x,
                @JsonProperty("y") double y,
                @JsonProperty("z") double z,
                @JsonProperty("preferredXScale") double preferredXScale,
                @JsonProperty("preferredYScale") double preferredYScale,
                @JsonProperty("preferredZScale") double preferredZScale,
                @JsonProperty("preferredRotationTwist") double preferredRotationTwist,
                @JsonProperty("preferredRotationTurn") double preferredRotationTurn,
                @JsonProperty("preferredRotationLean") double preferredRotationLean)
        {
            this.modelId = modelId;
            this.x = x;
            this.y = y;
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
            this.y = fromState.y;
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
                transformDropToBedYAdjust.getY(),
                transformMoveToPreferred.getZ(),
                preferredXScale.get(), preferredYScale.get(), preferredZScale.get(),
                preferredRotationTwist.get(), preferredRotationTurn.get(),
                preferredRotationLean.get());
    }

    public void setState(State state)
    {
        transformMoveToPreferred.setX(state.x);
        transformMoveToPreferred.setZ(state.z);
        transformDropToBedYAdjust.setY(state.y);

        preferredXScale.set(state.preferredXScale);
        transformScalePreferred.setX(state.preferredXScale);
        preferredYScale.set(state.preferredYScale);
        transformScalePreferred.setY(state.preferredYScale);
        preferredZScale.set(state.preferredZScale);
        transformScalePreferred.setZ(state.preferredZScale);

        preferredRotationLean.set(state.preferredRotationLean);
        preferredRotationTwist.set(state.preferredRotationTwist);
        preferredRotationTurn.set(state.preferredRotationTurn);

        updateTransformsFromLeanTwistTurnAngles();

        lastTransformedBoundsInParent = calculateBoundsInParentCoordinateSystem();
    }

    public int getModelId()
    {
        return modelId;
    }

    public boolean isInvalidMesh()
    {
        return isInvalidMesh;
    }

    public void setIsInvalidMesh(boolean isInvalidMesh)
    {
        this.isInvalidMesh = isInvalidMesh;
    }

}
