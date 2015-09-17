/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import static celtech.utils.threed.MeshSeparator.makeFacesWithVertex;
import static celtech.utils.threed.MeshUtils.countFacesAdjacentToVertices;
import static celtech.utils.threed.TriangleCutter.getVertex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javafx.geometry.Point2D;
import javafx.scene.shape.TriangleMesh;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;


/**
 * For algorithm see http://stackoverflow.com/questions/838076/small-cycle-finding-in-a-planar-graph
 *
 * @author tony
 */
public class NonManifoldLoopDetector
{

    static enum Direction
    {

        FORWARDS, BACKWARDS;
    }

    public static Set<List<ManifoldEdge>> identifyNonManifoldLoops(TriangleMesh mesh)
    {

        Set<ManifoldEdge> edges = getNonManifoldEdges(mesh);
        System.out.println("non manifold edges " + edges);
        Map<Integer, Set<ManifoldEdge>> edgesWithVertex = makeEdgesWithVertex(edges);

        Set<List<ManifoldEdge>> loops = new HashSet<>();
        if (edges.isEmpty())
        {
            return loops;
        }

        for (ManifoldEdge edge : edges)
        {
            Optional<List<ManifoldEdge>> loop = getLoopForEdgeInDirection(edge,
                                                                          edgesWithVertex,
                                                                          Direction.FORWARDS);
            if (loop.isPresent())
            {
                loops.add(loop.get());
            }
            loop = getLoopForEdgeInDirection(edge, edgesWithVertex, Direction.BACKWARDS);
            if (loop.isPresent())
            {
                loops.add(loop.get());
            }
        }

        return loops;
    }

    static Optional<List<ManifoldEdge>> getLoopForEdgeInDirection(
        ManifoldEdge edge, Map<Integer, Set<ManifoldEdge>> edgesWithVertex, Direction direction)
    {
        List<ManifoldEdge> loop = new ArrayList<>();

        ManifoldEdge firstEdge = edge;
        ManifoldEdge previousEdge = edge;
        Direction previousDirection = direction;
        int previousVertexId;
        if (direction == Direction.FORWARDS)
        {
            previousVertexId = previousEdge.v1;
        } else
        {
            previousVertexId = previousEdge.v0;
        }

        while (true)
        {
            loop.add(previousEdge);

            Set<ManifoldEdge> availableEdges = edgesWithVertex.get(previousVertexId);
            assert availableEdges.contains(previousEdge);
            availableEdges.remove(previousEdge);
            ManifoldEdge nextEdge;
            if (availableEdges.size() == 1)
            {
                nextEdge = availableEdges.iterator().next();
            } else if (availableEdges.isEmpty())
            {
                return Optional.empty();
            } else
            {
                nextEdge = getRightmostEdge(previousVertexId, previousEdge, previousDirection,
                                            availableEdges);
            }
            if (nextEdge.equals(firstEdge))
            {
                break;
            }
            Direction nextDirection;
            int nextVertexId;
            if (nextEdge.v0 == previousVertexId)
            {
                nextDirection = Direction.FORWARDS;
                nextVertexId = nextEdge.v1;
            } else
            {
                nextDirection = Direction.BACKWARDS;
                nextVertexId = nextEdge.v0;
            }

            if (nextEdge.isVisited(nextDirection))
            {
                // already have explored this possible loop
//                return Optional.empty();
            } 
            nextEdge.setVisited(nextDirection);
            previousEdge = nextEdge;
            previousDirection = nextDirection;
            previousVertexId = nextVertexId;
        }

        return Optional.of(loop);
    }

    private static ManifoldEdge getRightmostEdge(int previousVertexId,
        ManifoldEdge previousEdge, Direction previousDirection, Set<ManifoldEdge> availableEdges)
    {
        assert availableEdges.size() > 0;
        assert !availableEdges.contains(previousEdge);
        double smallestAngle = Double.MAX_VALUE;

        Direction nextDirection;

        ManifoldEdge rightmostEdge = null;
        Point2D previousVector = previousEdge.getVectorForDirection(previousDirection);
        for (ManifoldEdge edge : availableEdges)
        {
            if (edge.v0 == previousVertexId)
            {
                nextDirection = Direction.FORWARDS;
            } else
            {
                nextDirection = Direction.BACKWARDS;
            }

            Point2D followingVector = edge.getVectorForDirection(nextDirection);
            double angle = previousVector.angle(followingVector);
            if (angle < smallestAngle)
            {
                smallestAngle = angle;
                rightmostEdge = edge;
            }
        }
        return rightmostEdge;
    }

    static Map<Integer, Set<ManifoldEdge>> makeEdgesWithVertex(Set<ManifoldEdge> edges)
    {
        Map<Integer, Set<ManifoldEdge>> edgesWithVertex = new HashMap<>();
        for (ManifoldEdge edge : edges)
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

    static Set<ManifoldEdge> getNonManifoldEdges(TriangleMesh mesh)
    {
        Map<Integer, Set<Integer>> facesWithVertices = makeFacesWithVertex(mesh);
        Set<ManifoldEdge> nonManifoldEdges = new HashSet<>();
        for (int faceIndex = 0; faceIndex < mesh.getFaces().size() / 6; faceIndex++)
        {
            int v0 = mesh.getFaces().get(faceIndex * 6);
            int v1 = mesh.getFaces().get(faceIndex * 6 + 2);
            int v2 = mesh.getFaces().get(faceIndex * 6 + 4);
            if (countFacesAdjacentToVertices(mesh, facesWithVertices, faceIndex, 0, 1) != 1)
            {
                nonManifoldEdges.add(new ManifoldEdge(v0, v1, getVertex(mesh, v0), getVertex(mesh,
                                                                                             v1)));
            }
            if (countFacesAdjacentToVertices(mesh, facesWithVertices, faceIndex, 1, 2) != 1)
            {
                nonManifoldEdges.add(new ManifoldEdge(v1, v2, getVertex(mesh, v1), getVertex(mesh,
                                                                                             v2)));
            }
            if (countFacesAdjacentToVertices(mesh, facesWithVertices, faceIndex, 0, 2) != 1)
            {
                nonManifoldEdges.add(new ManifoldEdge(v0, v2, getVertex(mesh, v0), getVertex(mesh,
                                                                                             v2)));
            }
        }

        return nonManifoldEdges;
    }

}
