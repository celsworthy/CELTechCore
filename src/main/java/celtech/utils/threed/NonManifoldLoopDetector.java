/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import static celtech.utils.threed.MeshSeparator.makeFacesWithVertex;
import static celtech.utils.threed.MeshUtils.countFacesAdjacentToVertices;
import java.util.ArrayList;
import java.util.HashMap;
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
        System.out.println("non manifold edges " + edges);
        Map<Integer, Set<Edge>> edgesWithVertex = makeEdgesWithVertex(edges);

        Set<List<Edge>> loops = new HashSet<>();

        Optional<List<Edge>> loopEdges;
        do
        {
            try
            {
                loopEdges = getNextNonManifoldLoop(edges, edgesWithVertex);
                if (loopEdges.isPresent())
                {
                    loops.add(loopEdges.get());
                }
            } catch (Exception ex)
            {
                ex.printStackTrace();
            }
        } while (!edges.isEmpty());

        return loops;

    }

    static Optional<List<Edge>> getNextNonManifoldLoop(Set<Edge> edges,
        Map<Integer, Set<Edge>> edgesWithVertex)
    {

        assert !edges.isEmpty();

        List<Edge> loop = new ArrayList<>();

        Edge firstEdge = edges.iterator().next();
        loop.add(firstEdge);
        edges.remove(firstEdge);
        if (edges.isEmpty())
        {
            return Optional.empty();
        }

        int firstVertex = firstEdge.v0;
        int previousVertex = firstEdge.v1;
        Edge previousEdge = firstEdge;

        while (true)
        {
            if (edges.isEmpty())
            {
                return Optional.empty();
            }
            Set<Edge> edgesWithPreviousVertex = edgesWithVertex.get(previousVertex);
            edgesWithPreviousVertex.remove(previousEdge);
            //XXX is it possible to have multiple non-manifold edges connecting to same vertex?
            if (edgesWithPreviousVertex.size() != 1) {
                
                for (Edge edge : edgesWithPreviousVertex)
                {
                    System.out.println("connecting edge: "  + edge);
                }
            }
            assert edgesWithPreviousVertex.size() == 1: edgesWithPreviousVertex.size() + " other edges connect to vertex";
            Edge nextEdge = edgesWithPreviousVertex.iterator().next();
            assert edges.contains(nextEdge);
            loop.add(nextEdge);
            edges.remove(nextEdge);
            int nextVertex;
            if (nextEdge.v0 == previousVertex)
            {
                nextVertex = nextEdge.v1;
            } else
            {
                nextVertex = nextEdge.v0;
            }
            if (nextVertex == firstVertex)
            {
                break;
            }
            previousEdge = nextEdge;
            previousVertex = nextVertex;
        }

        return Optional.of(loop);
    }

    static Map<Integer, Set<Edge>> makeEdgesWithVertex(Set<Edge> edges)
    {
        Map<Integer, Set<Edge>> edgesWithVertex = new HashMap<>();
        for (Edge edge : edges)
        {
            if (!edgesWithVertex.containsKey(edge.v0))
            {
                edgesWithVertex.put(edge.v0, new HashSet<>());
            }
            if (!edgesWithVertex.containsKey(edge.v1))
            {
                edgesWithVertex.put(edge.v1, new HashSet<>());
            }
            edgesWithVertex.get(edge.v0).add(edge);
            edgesWithVertex.get(edge.v1).add(edge);
        }
        return edgesWithVertex;
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
