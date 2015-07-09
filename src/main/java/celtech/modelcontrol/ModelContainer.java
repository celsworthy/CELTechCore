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
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.transform.Affine;
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
    private static int nextModelId = 1;
    /**
     * The modelId is only unique at the project level because it is reloaded from saved models.
     */
    private int modelId;
    private Stenographer steno = null;
    private PrintBed printBed = null;
    private boolean isCollided = false;
    private BooleanProperty isSelected = null;
    private BooleanProperty isOffBed = null;
    private SimpleStringProperty modelName = null;
    private int numberOfMeshes = 0;
    private ModelContentsEnumeration modelContentsType = ModelContentsEnumeration.MESH;

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
    private Group modelContainersGroup = new Group();
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
    private ModelBounds lastTransformedBoundsInBed;
    private ModelBounds lastTransformedBoundsInParent;
    private SelectionHighlighter selectionHighlighter = null;
    private List<ShapeProvider.ShapeChangeListener> shapeChangeListeners;
    private List<ScreenExtentsProvider.ScreenExtentsListener> screenExtentsChangeListeners;
    private Set<Node> selectedMarkers;
    private Set<ModelContainer> childModelContainers;

    /**
     * Print the part using the extruder of the given number.
     */
    private ObservableList<Integer> meshExtruderAssociation;

    private File modelFile;

    public ModelContainer(File modelFile, MeshView meshToAdd)
    {
        super();
        getChildren().add(meshGroup);
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
     * A multiple mesh model Derive a per-mesh extruder number.
     */
    public ModelContainer(File modelFile, List<MeshView> meshes, List<Integer> extruderAssociation)
    {
        super();
        getChildren().add(meshGroup);
        modelContentsType = ModelContentsEnumeration.MESH;
        meshGroup.getChildren().addAll(meshes);
        numberOfMeshes = meshes.size();
        initialise(modelFile);
        initialiseTransforms();

        meshExtruderAssociation = FXCollections.observableArrayList(extruderAssociation);
    }

    public ModelContainer(Set<ModelContainer> modelContainers)
    {
        super();
        initialise(null);
        getChildren().add(modelContainersGroup);
        childModelContainers.addAll(modelContainers);
        modelContainersGroup.getChildren().addAll(modelContainers);
        initialiseTransforms();
        clearTransformMoveToCentre();
        for (ModelContainer modelContainer : modelContainers)
        {
            modelContainer.clearBedTransform();
        }
        lastTransformedBoundsInBed = calculateBoundsInBedCoordinateSystem();
        lastTransformedBoundsInParent = calculateBoundsInParentCoordinateSystem();
        originalModelBounds = calculateBoundsInLocal();
    }

    public ModelContainer getParentModelContainer()
    {
        if (getParent() != null && getParent().getParent() instanceof ModelContainer)
        {
            return (ModelContainer) getParent().getParent();
        } else
        {
            return null;
        }
    }

    public Set<ModelContainer> getChildModelContainers()
    {
        return Collections.unmodifiableSet(childModelContainers);
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

    public void printTransforms()
    {
        System.out.println("Transforms for: " + getId());
        System.out.println("==============================================");
        System.out.println("Scale preferred is " + transformScalePreferred);
        System.out.println("Move to centre is " + transformMoveToCentre);
        System.out.println("Move to preferred is " + transformMoveToPreferred);
        System.out.println("transformSnapToGroundYAdjust is " + transformPostRotationYAdjust);
        System.out.println("transformRotateLeanPreferred is " + transformRotateLeanPreferred);
        System.out.println("transformRotateTwistPreferred " + transformRotateTwistPreferred);
        System.out.println("transformRotateTurnPreferred " + transformRotateTurnPreferred);
        System.out.println("transformBedCentre " + transformBedCentre);
        System.out.println("==============================================");
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
         * Rotations (which are all around the centre of the model) must be applied before any
         * translations.
         */
        getTransforms().addAll(transformPostRotationYAdjust, transformMoveToPreferred,
                               transformMoveToCentre, transformBedCentre,
                               transformRotateTurnPreferred,
                               transformRotateLeanPreferred,
                               transformRotateTwistPreferred
        );
        meshGroup.getTransforms().addAll(transformScalePreferred);
        modelContainersGroup.getTransforms().addAll(transformScalePreferred);

        originalModelBounds = calculateBoundsInLocal();

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

        lastTransformedBoundsInBed = calculateBoundsInBedCoordinateSystem();
        lastTransformedBoundsInParent = calculateBoundsInParentCoordinateSystem();

        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    private void initialise(File modelFile)
    {
        this.modelFile = modelFile;
        modelId = nextModelId;
        nextModelId += 1;
        childModelContainers = new HashSet<>();
        meshExtruderAssociation = FXCollections.observableArrayList();
        shapeChangeListeners = new ArrayList<>();
        screenExtentsChangeListeners = new ArrayList<>();
        steno = StenographerFactory.getStenographer(ModelContainer.class.getName());
        printBed = PrintBed.getInstance();

        isSelected = new SimpleBooleanProperty(false);
        isOffBed = new SimpleBooleanProperty(false);

        if (modelFile != null)
        {
            modelName = new SimpleStringProperty(modelFile.getName());
            this.setId(modelFile.getName());
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

        selectedMarkers = new HashSet<>();

    }

    void clearBedTransform()
    {
        updateLastTransformedBoundsInParentForTranslateByX(-bedCentreOffsetX);
        updateLastTransformedBoundsInParentForTranslateByZ(-bedCentreOffsetZ);
        transformBedCentre.setX(0);
        transformBedCentre.setY(0);
        transformBedCentre.setZ(0);
    }

    /**
     * Set transformBedCentre according to the position of the centre of the bed.
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

    private void clearTransformMoveToCentre()
    {
        transformMoveToCentre.setX(0);
        transformMoveToCentre.setY(0);
        transformMoveToCentre.setZ(0);
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

        ObservableList<Integer> clonedMeshToExtruder = FXCollections.observableArrayList(
            meshExtruderAssociation);

        ModelContainer copy = new ModelContainer(this.modelFile, clonedMeshViews,
                                                 clonedMeshToExtruder);
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

        updateLastTransformedBoundsInParentForTranslateByX(xMove);
        updateLastTransformedBoundsInParentForTranslateByZ(zMove);

        keepOnBedXZ();
        checkOffBed();

    }

    public ModelBounds getTransformedBoundsInBed()
    {
        return lastTransformedBoundsInBed;
    }

    public ModelBounds getLocalBounds()
    {
        return originalModelBounds;
    }

    public void translateFrontLeftTo(double xPosition, double zPosition)
    {
        double newXPosition = xPosition - bedCentreOffsetX + getTransformedBoundsInBed().getWidth()
            / 2.0;
        double newZPosition = zPosition - bedCentreOffsetZ + getTransformedBoundsInBed().getHeight()
            / 2.0;
        double deltaXPosition = newXPosition - transformMoveToPreferred.getX();
        double deltaZPosition = newZPosition - transformMoveToPreferred.getZ();
        transformMoveToPreferred.setX(newXPosition);
        transformMoveToPreferred.setZ(newZPosition);
        updateLastTransformedBoundsInParentForTranslateByX(deltaXPosition);
        updateLastTransformedBoundsInParentForTranslateByZ(deltaZPosition);
        checkOffBed();
        notifyShapeChange();
        notifyScreenExtentsChange();
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
        if (getTransformedBoundsInBed().getMinX() < minBedX)
        {
            deltaX = -(getTransformedBoundsInBed().getMinX() - minBedX);
            transformMoveToPreferred.setX(transformMoveToPreferred.getX() + deltaX);
        } else if (getTransformedBoundsInBed().getMaxX() > maxBedX)
        {
            deltaX = -(getTransformedBoundsInBed().getMaxX() - maxBedX);
            transformMoveToPreferred.setX(transformMoveToPreferred.getX() + deltaX);
        }
        updateLastTransformedBoundsInParentForTranslateByX(deltaX);

        double deltaZ = 0;
        double minBedZ = PrintBed.getPrintVolumeCentre().getZ() - PrintBed.maxPrintableZSize / 2.0
            + 1;
        double maxBedZ = PrintBed.getPrintVolumeCentre().getZ() + PrintBed.maxPrintableZSize / 2.0
            - 1;
        if (getTransformedBoundsInBed().getMinZ() < minBedZ)
        {
            deltaZ = -(getTransformedBoundsInBed().getMinZ() - minBedZ);
            transformMoveToPreferred.setZ(transformMoveToPreferred.getZ() + deltaZ);
        } else if (getTransformedBoundsInBed().getMaxZ() > maxBedZ)
        {
            deltaZ = -(getTransformedBoundsInBed().getMaxZ() - maxBedZ);
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
     * Return a set of all descendent ModelContainers (and include this one) that have MeshView
     * children.
     */
    public Set<ModelContainer> getModelsHoldingMeshViews()
    {
        Set<ModelContainer> modelsHoldingMeshViews = new HashSet<>();
        if (meshGroup.getChildren().size() > 0)
        {
            modelsHoldingMeshViews.add(this);
        }
        for (Node modelNode : modelContainersGroup.getChildren())
        {
            ModelContainer modelContainer = (ModelContainer) modelNode;
            modelsHoldingMeshViews.addAll(modelContainer.getModelsHoldingMeshViews());
        }
        return modelsHoldingMeshViews;
    }

    public Collection<? extends ModelContainer> getModelsHoldingModels()
    {
        Set<ModelContainer> modelsHoldingModels = new HashSet<>();
        if (modelContainersGroup.getChildren().size() > 0)
        {
            modelsHoldingModels.add(this);
        }
        for (Node modelNode : modelContainersGroup.getChildren())
        {
            ModelContainer modelContainer = (ModelContainer) modelNode;
            modelsHoldingModels.addAll(modelContainer.getModelsHoldingModels());
        }
        return modelsHoldingModels;
    }

    public void addGroupStructure(Map<Integer, Set<Integer>> groupStructure)
    {
        for (Node modelNode : modelContainersGroup.getChildren())
        {
            ModelContainer modelContainer = (ModelContainer) modelNode;
            if (groupStructure.get(modelId) == null)
            {
                groupStructure.put(modelId, new HashSet<>());
            }
            groupStructure.get(modelId).add(modelContainer.modelId);
        }
    }

    /**
     * Rotate the model in Lean and Twist so that the chosen face is pointing down (ie aligned with
     * the Y axis). Lean is easy to get, and we then use an optimiser to establish Twist.
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
                                                               new ApplyTwist(pickedMesh,
                                                                              snapFaceIndex)),
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

        transformScalePreferred.setPivotX(getBoundsInLocal().getMinX()
            + getBoundsInLocal().getWidth() / 2.0);
        transformScalePreferred.setPivotY(getBoundsInLocal().getMinY()
            + getBoundsInLocal().getHeight() / 2.0);
        transformScalePreferred.setPivotZ(getBoundsInLocal().getMinZ()
            + getBoundsInLocal().getDepth() / 2.0);
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
        // was not used (was snapFaceIndex) - now modelId
        out.writeInt(modelId);
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
        modelContainersGroup = new Group();
        getChildren().addAll(meshGroup, modelContainersGroup);

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
            for (int i = 0; i < numberOfMeshes; i++)
            {
                meshExtruderAssociation.add(in.readInt());
            }
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

        initialiseTransforms();

        transformMoveToPreferred.setX(storedX);
        transformMoveToPreferred.setZ(storedZ);
        setXScale(storedScaleX);
        setYScale(storedScaleY);
        setZScale(storedScaleZ);
        setRotationLean(storedRotationLean);
        setRotationTwist(storedRotationTwist);
        setRotationTurn(storedRotationTurn);

        if (convertSnapFace)
        {
            snapToGround(getMeshViews().get(0), storedSnapFaceIndexLegacy);
        }

        notifyShapeChange();
        notifyScreenExtentsChange();
    }

    private void readObjectNoData()
        throws ObjectStreamException
    {

    }

    public List<MeshView> getMeshViews()
    {
        List<MeshView> meshViews = meshGroup.getChildrenUnmodifiable().stream()
            .filter(node -> node instanceof MeshView)
            .map(MeshView.class::cast)
            .collect(Collectors.toList());

        return meshViews;
    }

    public ModelContentsEnumeration getModelContentsType()
    {
        return modelContentsType;
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

    public void translateXTo(double xPosition)
    {
        ModelBounds bounds = getTransformedBoundsInBed();

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

    public void translateZTo(double zPosition)
    {
        ModelBounds bounds = getTransformedBoundsInBed();

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

    private void checkOffBed()
    {
        ModelBounds bounds = getTransformedBoundsInBed();

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
     * Calculate max/min X,Y,Z before the transforms have been applied (ie the original model
     * dimensions before any transforms).
     */
    ModelBounds calculateBoundsInLocal()
    {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        for (ModelContainer modelContainer : childModelContainers)
        {
            ModelBounds bounds = modelContainer.lastTransformedBoundsInParent; // parent of child model is this model
            minX = Math.min(bounds.getMinX(), minX);
            minY = Math.min(bounds.getMinY(), minY);
            minZ = Math.min(bounds.getMinZ(), minZ);

            maxX = Math.max(bounds.getMaxX(), maxX);
            maxY = Math.max(bounds.getMaxY(), maxY);
            maxZ = Math.max(bounds.getMaxZ(), maxZ);
        }

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

    public Set<Node> descendentMeshViews()
    {
        Set<Node> descendentMeshViews = new HashSet<>();
        descendentMeshViews.addAll(meshGroup.getChildren());

        for (ModelContainer modelContainer : childModelContainers)
        {
            descendentMeshViews.addAll(modelContainer.descendentMeshViews());
        }

        return descendentMeshViews;
    }

    /**
     * Return a single transform that combines all transforms that will be applied to a child mesh
     * by its parent ModelContainer.
     */
    private Transform getCombinedTransform()
    {
        Transform combinedTransform = new Translate(0, 0, 0);

        for (Transform transform : getTransforms())
        {
            combinedTransform = combinedTransform.createConcatenation(transform);
        }
        combinedTransform = combinedTransform.createConcatenation(transformScalePreferred);

        return combinedTransform;
    }

    /**
     * Return a single transform that combines all transforms that will be applied ( through all
     * parent ModelContainers) up to and including the top level ModelContainer. This will then
     * return a transform which will give the coordinates in the bed coordinate system.
     */
    private Transform getCombinedTransformForAllGroups(MeshView meshView)
    {
        Affine combinedTransform = new Affine();
        List<ModelContainer> modelContainers = new ArrayList<>();
        Node parentModelContainer = meshView.getParent().getParent();

        while (parentModelContainer instanceof ModelContainer)
        {
            modelContainers.add((ModelContainer) parentModelContainer);
            Transform transform = ((ModelContainer) parentModelContainer).getCombinedTransform();
            combinedTransform.prepend(transform);
            parentModelContainer = parentModelContainer.getParent();
        }

        return combinedTransform;
    }

    static ModelContainer getRootModelContainer(MeshView meshView)
    {
        Node parentModelContainer = meshView.getParent().getParent();

        while ((parentModelContainer.getParent() != null)
            && parentModelContainer.getParent().getParent() instanceof ModelContainer)
        {
            parentModelContainer = parentModelContainer.getParent().getParent();
        }
        return (ModelContainer) parentModelContainer;
    }

    /**
     * Calculate max/min X,Y,Z after all the transforms have been applied all the way to the bed
     * coordinate system.
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

//            Transform transform = getCombinedTransformForAllGroups(meshView);
            ModelContainer rootModelContainer = getRootModelContainer(meshView);

            TriangleMesh mesh = (TriangleMesh) meshView.getMesh();
            ObservableFloatArray originalPoints = mesh.getPoints();

            for (int pointOffset = 0; pointOffset < originalPoints.size(); pointOffset += 3)
            {
                float xPos = originalPoints.get(pointOffset);
                float yPos = originalPoints.get(pointOffset + 1);
                float zPos = originalPoints.get(pointOffset + 2);

//                Point3D pointInParent = transform.transform(xPos, yPos, zPos);
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
     * Calculate max/min X,Y,Z after the transforms have been applied (ie in the parent node).
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
     * THIS METHOD IS NOT CURRENTLY IN USE PROBABLY SHOULD BE BINNED IN FAVOUR OF AN APPROACH
     * SIMILAR TO THE SPLIT FUNCTION
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
     * This compareTo implementation compares based on the overall size of the model.
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

    public double getTotalWidth()
    {
        double totalwidth = originalModelBounds.getWidth() * preferredXScale.get();
        return totalwidth;
    }

    public double getTotalDepth()
    {
        double totaldepth = originalModelBounds.getDepth() * preferredZScale.get();
        return totaldepth;
    }

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
        ModelBounds modelBoundsParent = calculateBoundsInBedCoordinateSystem();
        transformPostRotationYAdjust.setY(-modelBoundsParent.getMaxY());
        lastTransformedBoundsInBed = calculateBoundsInBedCoordinateSystem();
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
        return getTransformedBoundsInBed().getCentreZ();
    }

    public double getTransformedCentreX()
    {
        return getTransformedBoundsInBed().getCentreX();
    }

    @Override
    public double getOriginalHeight()
    {
        return getLocalBounds().getHeight();
    }

    @Override
    public double getScaledHeight()
    {
        return getTransformedBoundsInBed().getHeight();
    }

    @Override
    public double getOriginalDepth()
    {
        return getLocalBounds().getDepth();
    }

    @Override
    public double getScaledDepth()
    {
        return getTransformedBoundsInBed().getDepth();
    }

    @Override
    public double getOriginalWidth()
    {
        return getTransformedBoundsInBed().getWidth();
    }

    @Override
    public double getScaledWidth()
    {
        return getTransformedBoundsInBed().getWidth();
    }

    public void addSelectionHighlighter()
    {
        selectionHighlighter = new SelectionHighlighter(this);
        getChildren().add(selectionHighlighter);
        selectedMarkers.add(selectionHighlighter);
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
     * If this model is associated with the given extruder number then recolour it to the given
     * colour, also taking into account if it is misplaced (off the bed). Also call the same method
     * on any child ModelContainers.
     */
    public void updateColour(final Color displayColourExtruder0, final Color displayColourExtruder1,
        boolean showMisplacedColour)
    {
        for (ModelContainer modelContainer : childModelContainers)
        {
            modelContainer.updateColour(displayColourExtruder0, displayColourExtruder1,
                                        showMisplacedColour);
        }

        for (int meshNumber = 0; meshNumber < getMeshViews().size(); meshNumber++)
        {
            MeshView mesh = getMeshViews().get(meshNumber);

            if (showMisplacedColour)
            {
                if (isOffBed.get())
                {
                    mesh.setMaterial(ApplicationMaterials.getOffBedModelMaterial());
                } else if (isCollided)
                {
                    mesh.setMaterial(ApplicationMaterials.getCollidedModelMaterial());
                }
            }
            if (!showMisplacedColour || (!isOffBed.get() && !isCollided))
            {
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
     * State captures the state of all the transforms being applied to this ModelContainer. It is
     * used as an efficient way of applying Undo and Redo to changes to a Set of ModelContainers.
     */
    public static class State
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

        @JsonCreator
        public State(
            @JsonProperty("modelId") int modelId,
            @JsonProperty("x") double x,
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
