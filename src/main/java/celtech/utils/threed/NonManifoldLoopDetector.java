/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import static celtech.utils.threed.MeshSeparator.makeFacesWithVertex;
import static celtech.utils.threed.MeshUtils.countFacesAdjacentToVertices;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javafx.scene.shape.TriangleMesh;


/**
 *
 * @author tony
 */
public class NonManifoldLoopDetector
{

    public static Set<List<Edge>> identifyNonManifoldLoops(TriangleMesh mesh)
    {
        
        Set<Edge> edges = getNonManifoldEdges(mesh);
        Set<Edge> usedEdges = new HashSet<>();
        
        Set<List<Edge>> loops = new HashSet<>();

        Optional<List<Edge>> loopEdges;
        do
        {
            loopEdges = getNextNonManifoldLoop(mesh, edges, usedEdges);
            if (loopEdges.isPresent())
            {
                loops.add(loopEdges.get());
            }
        } while (loopEdges.isPresent());

        return loops;

    }

    static Optional<List<Edge>> getNextNonManifoldLoop(TriangleMesh mesh,
        Set<Edge> edges, Set<Edge> usedEdge)
    {
        return null;
    }
    

    static Set<Edge> getNonManifoldEdges(TriangleMesh mesh)
    {
        Map<Integer, Set<Integer>> facesWithVertices = makeFacesWithVertex(mesh);
        Set<Edge> nonManifoldEdges = new HashSet<>();
        for (int faceIndex = 0; faceIndex < mesh.getFaces().size() / 6; faceIndex++)
        {
            int v0 = mesh.getFaces().get(faceIndex * 6);
            int v1 = mesh.getFaces().get(faceIndex * 6 + 2);
            int v2 = mesh.getFaces().get(faceIndex * 6 + 4);
            if (countFacesAdjacentToVertices(mesh, facesWithVertices, faceIndex, 0, 1) != 1)
            {
                nonManifoldEdges.add(new Edge(v0, v1));
            }
            if (countFacesAdjacentToVertices(mesh, facesWithVertices, faceIndex, 1, 2) != 1)
            {
                nonManifoldEdges.add(new Edge(v1, v2));
            }
            if (countFacesAdjacentToVertices(mesh, facesWithVertices, faceIndex, 0, 2) != 1)
            {
                nonManifoldEdges.add(new Edge(v0, v2));
            }
        }
        
        return nonManifoldEdges;
    }
}
