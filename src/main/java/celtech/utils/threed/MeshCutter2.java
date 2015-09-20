/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import static celtech.utils.threed.MeshDebug.visualiseEdgeLoops;
import static celtech.utils.threed.MeshSeparator.setTextureAndSmoothing;
import static celtech.utils.threed.MeshUtils.copyMesh;
import static celtech.utils.threed.NonManifoldLoopDetector.identifyNonManifoldLoops;
import static celtech.utils.threed.OpenFaceCloser.closeOpenFace;
import com.sun.javafx.scene.shape.ObservableFaceArrayImpl;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;

/**
 *
 * @author tony
 */
public class MeshCutter2 {

    /**
     * Cut the given mesh into two, at the given height.
     */
    public static List<TriangleMesh> cut(TriangleMesh mesh, float cutHeight,
            MeshCutter.BedToLocalConverter bedToLocalConverter) {

        System.out.println("cut at " + cutHeight);

        CutResult cutResult = getUncoveredMesh(mesh, cutHeight, bedToLocalConverter, MeshCutter.TopBottom.TOP);

        TriangleMesh topMesh = closeOpenFace(cutResult, cutHeight, bedToLocalConverter);
        MeshUtils.removeUnusedAndDuplicateVertices(topMesh);
        setTextureAndSmoothing(topMesh, topMesh.getFaces().size() / 6);

        Optional<MeshUtils.MeshError> error = MeshUtils.validate(topMesh);
        if (error.isPresent()) {
            System.out.println("Error in TOP mesh: " + error.toString());
//            throw new RuntimeException("Invalid mesh: " + error.toString());
        }

        cutResult = getUncoveredMesh(mesh, cutHeight, bedToLocalConverter, MeshCutter.TopBottom.BOTTOM);

        TriangleMesh bottomMesh = closeOpenFace(cutResult, cutHeight, bedToLocalConverter);
        MeshUtils.removeUnusedAndDuplicateVertices(bottomMesh);
        setTextureAndSmoothing(bottomMesh, bottomMesh.getFaces().size() / 6);

        error = MeshUtils.validate(bottomMesh);
        if (error.isPresent()) {
            System.out.println("Error in BOTTOM mesh: " + error.toString());
//            throw new RuntimeException("Invalid mesh: " + error.toString());
        }

        List<TriangleMesh> meshes = new ArrayList<>();
        meshes.add(topMesh);
        meshes.add(bottomMesh);

        return meshes;
    }

    static CutResult getUncoveredMesh(TriangleMesh mesh,
            float cutHeight, MeshCutter.BedToLocalConverter bedToLocalConverter,
            MeshCutter.TopBottom topBottom) {

        TriangleMesh childMesh = makeSplitMesh(mesh,
                cutHeight, bedToLocalConverter, topBottom);
        
        boolean orientable = MeshUtils.testMeshIsOrientable(childMesh);
        if (! orientable) {
            throw new RuntimeException("uncovered cut mesh is not orientable!");
        }

        // XXX remove duplicate vertices before trying to identify non-manifold edges ??
        Set<List<ManifoldEdge>> loops = identifyNonManifoldLoops(childMesh);
//
//        Set<ManifoldEdge> nonManifoldEdges = NonManifoldLoopDetector.getNonManifoldEdges(childMesh);
//        visualiseEdgeLoops(nonManifoldEdges, loops);
        Set<PolygonIndices> polygonIndices = convertEdgesToVertices(loops);

        polygonIndices = removeSequentialDuplicateVertices(polygonIndices);

        CutResult cutResultUpper = new CutResult(childMesh, polygonIndices,
                bedToLocalConverter, topBottom);
        return cutResultUpper;
    }

    /**
     * Given the mesh, cut faces and intersection points, create the child mesh.
     * Copy the original mesh, remove all the cut faces and replace with a new
     * set of faces using the new intersection points. Remove all the faces from
     * above the cut faces.
     */
    static TriangleMesh makeSplitMesh(TriangleMesh mesh,
            float cutHeight, MeshCutter.BedToLocalConverter bedToLocalConverter,
            MeshCutter.TopBottom topBottom) {
        TriangleMesh childMesh = copyMesh(mesh);

        Set<Integer> facesToRemove = new HashSet<>();

        for (int i = 0; i < mesh.getFaces().size() / 6; i++) {
            TriangleCutter.splitFaceAndAddLowerFacesToMesh(childMesh, facesToRemove, i, cutHeight,
                    bedToLocalConverter, topBottom);
        }
        System.out.println("faces to remove " + topBottom + " " + facesToRemove);

        removeFaces(childMesh, facesToRemove);

//        MeshDebug.showFace(mesh, 0);
        return childMesh;
    }

    /**
     * Remove the given faces from the mesh.
     */
    private static void removeFaces(TriangleMesh mesh, Set<Integer> facesToRemove) {

        ObservableFaceArray newFaceArray = new ObservableFaceArrayImpl();
        for (int faceIndex = 0; faceIndex < mesh.getFaces().size() / 6; faceIndex++) {
            if (facesToRemove.contains(faceIndex)) {
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

    static Set<PolygonIndices> convertEdgesToVertices(Set<List<ManifoldEdge>> loops) {
        Set<PolygonIndices> polygonIndicesSet = new HashSet<>();
        for (List<ManifoldEdge> loop : loops) {
            PolygonIndices polygonIndices = convertEdgesToPolygonIndices(loop);
            polygonIndicesSet.add(polygonIndices);
        }
        return polygonIndicesSet;
    }

    /**
     * Convert the edges to a list of vertex indices. The edges may be going
     * forwards or backwards we cant assume a given direction.
     *
     * @param loop
     * @return
     */
    static PolygonIndices convertEdgesToPolygonIndices(List<ManifoldEdge> loop) {
        PolygonIndices polygonIndices = new PolygonIndices();

        int previousVertexId = -1;
        int firstVertexId = -1;
        ManifoldEdge edge0 = loop.get(0);
        ManifoldEdge edge1 = loop.get(1);
        if (edge0.v0 == edge1.v0 || edge0.v0 == edge1.v1) {
            // second vertex is edge0.v0
            firstVertexId = edge0.v1;
            polygonIndices.add(firstVertexId);
        } else {
            // second vertex is edge0.v1
            firstVertexId = edge0.v0;
            polygonIndices.add(firstVertexId);
        }

        for (ManifoldEdge edge : loop) {
            if (previousVertexId == -1) {
                // this is the first edge
                previousVertexId = firstVertexId;
            }
            if (edge.v0 == previousVertexId) {
                polygonIndices.add(edge.v1);
                previousVertexId = edge.v1;
            } else {
                polygonIndices.add(edge.v0);
                previousVertexId = edge.v0;
            }
        }
        return polygonIndices;
    }

    private static Set<PolygonIndices> removeSequentialDuplicateVertices(Set<PolygonIndices> polygonIndices) {
        Set<PolygonIndices> polygonIndicesClean = new HashSet<>();
        for (PolygonIndices loop : polygonIndices) {
            PolygonIndices cleanLoop = new PolygonIndices();
            int previousVertexIndex = -1;
            for (Integer vertexIndex : loop) {
                if (vertexIndex != previousVertexIndex) {
                    cleanLoop.add(vertexIndex);
                }
                previousVertexIndex = vertexIndex;
            }
            if (cleanLoop.get(0) == cleanLoop.get(cleanLoop.size() - 1)) {
                cleanLoop.remove(cleanLoop.size() - 1);
            }
            polygonIndicesClean.add(cleanLoop);
        }
        return polygonIndicesClean;
    }

}
