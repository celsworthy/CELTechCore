package celtech.modelcontrol;

import celtech.configuration.PrintBed;
import celtech.coreUI.visualisation.ApplicationMaterials;
import celtech.coreUI.visualisation.ShapeProvider;
import celtech.coreUI.visualisation.metaparts.FloatArrayList;
import celtech.coreUI.visualisation.metaparts.Part;
import celtech.coreUI.visualisation.modelDisplay.SelectionHighlighter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ModelContainer extends Group
{

    private Stenographer steno = StenographerFactory.getStenographer(ModelContainer.class.getName());
    private SimpleStringProperty modelName = null;

    private Group meshGroup = new Group();

    private SelectionHighlighter selectionHighlighter = null;
    private Set<Node> selectedMarkers;

    private TriangleMesh triangleMesh;
    private PhongMaterial material;

    private boolean isCollided = false;
    private boolean isOffBed = false;
    private boolean isSelected = false;
    private final Part associatedPart;

    /**
     *
     * @param name
     * @param partToAdd
     */
    public ModelContainer(String name, Part partToAdd)
    {
        super();

        associatedPart = partToAdd;
        
        triangleMesh = new TriangleMesh();

        int[] faceIndexArray = new int[6];

        partToAdd.getFaces().forEach(face ->
        {
            faceIndexArray[0] = face.getVertexIndex(0);
            faceIndexArray[2] = face.getVertexIndex(1);
            faceIndexArray[4] = face.getVertexIndex(2);
            // Add the face to the triangle mesh
            triangleMesh.getFaces().addAll(faceIndexArray, 0, 6);
        });

        float[] tempVertexPointArray = new float[3];
        partToAdd.getVertices().entrySet()
            .stream()
            .sorted((s1, s2) ->
                {
                    if (s1.getValue() == s2.getValue())
                    {
                        return 0;
                    } else if (s1.getValue() > s2.getValue())
                    {
                        return 1;
                    } else
                    {
                        return -1;
                    }
            })
            .forEach(vertexEntry ->
                {
                    tempVertexPointArray[0] = (float) vertexEntry.getKey().getX();
                    tempVertexPointArray[1] = (float) vertexEntry.getKey().getY();
                    tempVertexPointArray[2] = (float) vertexEntry.getKey().getZ();

                    triangleMesh.getPoints().addAll(tempVertexPointArray, 0, 3);
            });

        FloatArrayList texCoords = new FloatArrayList();
        texCoords.add(0f);
        texCoords.add(0f);
        triangleMesh.getTexCoords().addAll(texCoords.toFloatArray());

        int[] smoothingGroups = new int[partToAdd.getFaces().size()];
        for (int i = 0; i < smoothingGroups.length; i++)
        {
            smoothingGroups[i] = 0;
        }
        triangleMesh.getFaceSmoothingGroups().addAll(smoothingGroups);
        steno.info("The mesh contains " + triangleMesh.getPoints().size()
            + " points, " + triangleMesh.getTexCoords().size() + " tex coords and "
            + triangleMesh.getFaces().size() + " faces");

        MeshView meshView = new MeshView();

        meshView.setMesh(triangleMesh);
        material = ApplicationMaterials.getDefaultModelMaterial();
        meshView.setMaterial(material);
        meshView.setCullFace(CullFace.BACK);
        meshView.setId(name + "_mesh");

        this.getChildren().add(meshGroup);
        meshGroup.getChildren().add(meshView);

        modelName = new SimpleStringProperty(name);
        selectedMarkers = new HashSet<>();
        this.setId(name);
        getTransforms().addAll(transformSnapToGroundYAdjust, transformMoveToPreferred,
                               transformMoveToCentre, transformBedCentre,
                               transformRotateYPreferred, transformRotateSnapToGround);
        meshGroup.getTransforms().addAll(transformScalePreferred);
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
            if (isOffBed)
            {
                meshView.setMaterial(ApplicationMaterials.getOffBedModelMaterial());
            } else if (isCollided)
            {
                meshView.setMaterial(ApplicationMaterials.getCollidedModelMaterial());
            } else
            {
                meshView.setMaterial(material);
            }
        } else
        {
            for (Node node : ((Group) (meshGroup.getChildrenUnmodifiable().get(0))).
                getChildrenUnmodifiable())
            {
                if (node instanceof MeshView)
                {
                    if (isOffBed)
                    {
                        ((MeshView) node).setMaterial(ApplicationMaterials.getOffBedModelMaterial());
                    } else if (isCollided)
                    {
                        ((MeshView) node).setMaterial(
                            ApplicationMaterials.getCollidedModelMaterial());
                    } else
                    {
                        ((MeshView) node).
                            setMaterial(ApplicationMaterials.getDefaultModelMaterial());
                    }
                }
            }
        }
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
        isSelected = selected;
    }

    /**
     *
     * @return
     */
    public MeshView getMeshView()
    {
        if (meshGroup.getChildrenUnmodifiable().get(0) instanceof MeshView)
        {
            return (MeshView) (meshGroup.getChildrenUnmodifiable().get(0));
        } else
        {
            return null;
        }
    }

//    /**
//     * Calculate max/min X,Y,Z after the transforms have been applied (ie in the parent node).
//     */
//    public ModelBounds calculateBoundsInParent()
//    {
//        TriangleMesh mesh = (TriangleMesh) getMeshView().getMesh();
//        ObservableFloatArray originalPoints = mesh.getPoints();
//
//        double minX = Double.MAX_VALUE;
//        double minY = Double.MAX_VALUE;
//        double minZ = Double.MAX_VALUE;
//        double maxX = -Double.MAX_VALUE;
//        double maxY = -Double.MAX_VALUE;
//        double maxZ = -Double.MAX_VALUE;
//
//        for (int pointOffset = 0; pointOffset < originalPoints.size(); pointOffset += 3)
//        {
//            float xPos = originalPoints.get(pointOffset);
//            float yPos = originalPoints.get(pointOffset + 1);
//            float zPos = originalPoints.get(pointOffset + 2);
//
//            Point3D pointInParent = localToParent(meshGroup.localToParent(xPos, yPos, zPos));
//
//            minX = Math.min(pointInParent.getX(), minX);
//            minY = Math.min(pointInParent.getY(), minY);
//            minZ = Math.min(pointInParent.getZ(), minZ);
//
//            maxX = Math.max(pointInParent.getX(), maxX);
//            maxY = Math.max(pointInParent.getY(), maxY);
//            maxZ = Math.max(pointInParent.getZ(), maxZ);
//        }
//
//        double newwidth = maxX - minX;
//        double newdepth = maxZ - minZ;
//        double newheight = maxY - minY;
//
//        double newcentreX = minX + (newwidth / 2);
//        double newcentreY = minY + (newheight / 2);
//        double newcentreZ = minZ + (newdepth / 2);
//
//        return new ModelBounds(minX, maxX, minY, maxY, minZ, maxZ, newwidth,
//                               newheight, newdepth, newcentreX, newcentreY,
//                               newcentreZ);
//    }
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

    public void addSelectionHighlighter()
    {
        selectionHighlighter = new SelectionHighlighter(associatedPart);
        getChildren().add(selectionHighlighter);
        selectedMarkers.add(selectionHighlighter);
        notifyShapeChange();
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

    public Point3D transformMeshToRealWorldCoordinates(float vertexX, float vertexY, float vertexZ)
    {
        return localToParent(meshGroup.localToParent(vertexX, vertexY, vertexZ));
    }

    /**
     * If this model is associated with the given extruder number then recolour it to the given
     * colour.
     *
     * @param displayColourExtruder0
     * @param displayColourExtruder1
     */
    public void setColour(Color displayColourExtruder0, Color displayColourExtruder1)
    {

        PhongMaterial meshMaterial = null;
        if (associateWithExtruderNumber.get() == 0)
        {
            if (displayColourExtruder0 == null)
            {
                meshMaterial = ApplicationMaterials.getDefaultModelMaterial();
            } else
            {
                meshMaterial = new PhongMaterial(displayColourExtruder0);
            }
        } else
        {
            if (displayColourExtruder1 == null)
            {
                meshMaterial = ApplicationMaterials.getDefaultModelMaterial();
            } else
            {
                meshMaterial = new PhongMaterial(displayColourExtruder1);
            }
        }
        material = meshMaterial;
        for (Node mesh : meshGroup.getChildren())
        {
            MeshView meshView = (MeshView) mesh;
            meshView.setMaterial(meshMaterial);
        }
    }
    
    public boolean isSelected()
    {
        return isSelected;
    }

    public Part getAssociatedPart()
    {
        return associatedPart;
    }
}
