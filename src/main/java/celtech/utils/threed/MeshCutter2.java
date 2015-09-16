/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import static celtech.utils.threed.MeshCutter.makePoint3D;
import static celtech.utils.threed.MeshSeparator.setTextureAndSmoothing;
import static celtech.utils.threed.MeshUtils.copyMesh;
import static celtech.utils.threed.NonManifoldLoopDetector.identifyNonManifoldLoops;
import com.sun.javafx.scene.shape.ObservableFaceArrayImpl;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;


/**
 *
 * @author tony
 */
public class MeshCutter2
{
    
    static CutResult getUncoveredUpperMesh(TriangleMesh mesh,
        float cutHeight, MeshCutter.BedToLocalConverter bedToLocalConverter)
    {

        TriangleMesh childMesh = makeSplitMesh(mesh,
                                         cutHeight, bedToLocalConverter, MeshCutter.TopBottom.TOP);
        
        Set<List<Edge>> polygonIndices = identifyNonManifoldLoops(childMesh);
//        CutResult cutResultUpper = new CutResult(childMesh, polygonIndices,
//                                                 bedToLocalConverter, MeshCutter.TopBottom.TOP);
//        return cutResultUpper;
        return null;
    }
    
    /**
     * Given the mesh, cut faces and intersection points, create the child mesh. Copy the
     * original mesh, remove all the cut faces and replace with a new set of faces using the new
     * intersection points. Remove all the faces from above the cut faces.
     */
    static TriangleMesh makeSplitMesh(TriangleMesh mesh,
         float cutHeight, MeshCutter.BedToLocalConverter bedToLocalConverter, 
         MeshCutter.TopBottom topBottom)
    {
        TriangleMesh childMesh = copyMesh(mesh);

        removeCutFacesAndFacesAboveCutPlane(childMesh, cutHeight,
                                            bedToLocalConverter, topBottom);
        
        for (int i = 0; i < mesh.getFaces().size() / 6; i++) {
            TriangleCutter.splitFaceAndAddLowerFacesToMesh(mesh, childMesh, i, cutHeight,
                                                      bedToLocalConverter, topBottom);
        }

//        MeshDebug.showFace(mesh, 0);

        return childMesh;
    }
    
    /**
     * Remove the cut faces and any other faces above cut height from the mesh.
     */
    private static void removeCutFacesAndFacesAboveCutPlane(TriangleMesh mesh,
        float cutHeight,
        MeshCutter.BedToLocalConverter bedToLocalConverter,
        MeshCutter.TopBottom topBottom)
    {
        Set<Integer> facesAboveBelowCut = new HashSet<>();

        // compare vertices' -Y to cutHeight
        for (int faceIndex = 0; faceIndex < mesh.getFaces().size() / 6; faceIndex++)
        {
            int vertex0 = mesh.getFaces().get(faceIndex * 6);
            float vertex0YInBed = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, vertex0)).getY();

            // for BOTTOM we want vY is "above" the cut
            // if a vertex is on the line then ignore it, one of the other vertices will be
            // above or below the line.
            if (topBottom == MeshCutter.TopBottom.BOTTOM && vertex0YInBed < cutHeight || topBottom
                == MeshCutter.TopBottom.TOP && vertex0YInBed > cutHeight)
            {
                facesAboveBelowCut.add(faceIndex);
                continue;
            }
            int vertex1 = mesh.getFaces().get(faceIndex * 6 + 2);
            float vertex1YInBed = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, vertex1)).getY();
            if (topBottom == MeshCutter.TopBottom.BOTTOM && vertex1YInBed < cutHeight || topBottom
                == MeshCutter.TopBottom.TOP && vertex1YInBed > cutHeight)
            {
                facesAboveBelowCut.add(faceIndex);
                continue;
            }
            int vertex2 = mesh.getFaces().get(faceIndex * 6 + 4);
            float vertex2YInBed = (float) bedToLocalConverter.localToBed(makePoint3D(mesh, vertex2)).getY();
            if (topBottom == MeshCutter.TopBottom.BOTTOM && vertex2YInBed < cutHeight || topBottom
                == MeshCutter.TopBottom.TOP && vertex2YInBed > cutHeight)
            {
                facesAboveBelowCut.add(faceIndex);
                continue;
            }
        }

        ObservableFaceArray newFaceArray = new ObservableFaceArrayImpl();
        for (int faceIndex = 0; faceIndex < mesh.getFaces().size() / 6; faceIndex++)
        {
            if (facesAboveBelowCut.contains(faceIndex))
            {
                continue;
            }
            int[] vertices = new int[6];
            vertices[0] = mesh.getFaces().get(faceIndex * 6);
            vertices[2] = mesh.getFaces().get(faceIndex * 6 + 2);
            vertices[4] = mesh.getFaces().get(faceIndex * 6 + 4);
            newFaceArray.addAll(vertices);
        }
        mesh.getFaces().setAll(newFaceArray);
        setTextureAndSmoothing(mesh, mesh.getFaces().size() / 6);
    }


    
}
