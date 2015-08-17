/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.shape.TriangleMesh;


/**
 *
 * @author tony
 */
class CutResult
{
    /**
     * The child mesh that was created by the split.
     */
    TriangleMesh childMesh;
    /**
     * The indices of the vertices of the child mesh, in sequence, that form the perimeter of the
     * new open face that needs to be triangulated. Some loops (list of points) may be holes inside
     * other loops.
     */
    List<List<Integer>> loopsOfVerticesOnOpenFace;

    public CutResult(TriangleMesh childMesh, List<List<Integer>> vertexsOnOpenFace)
    {
        this.childMesh = childMesh;
        this.loopsOfVerticesOnOpenFace = vertexsOnOpenFace;
    }

    /**
     * Identify which of the loops in loopsOfVerticesOnOpenFace are internal to other loops. There
     * should not be any overlapping loops. Each LoopSet has one outer loop and zero or more inner
     * loops.
     *
     * Find sets of polygons that are nested inside each other. Sort each set by area giving
     * a list of nested polygons. The first in the list is the outer polygon.
     */
    public Set<LoopSet> identifyOuterLoopsAndInnerLoops()
    {
        Set<LoopSet> loopSets = new HashSet<>();
        Set<List<List<Integer>>> nestedPolygonsSet = getNestedPolygonSets();
        for (List<List<Integer>> nestedPolygons : nestedPolygonsSet)
        {
            sortByArea(nestedPolygons);
            List<Integer> outerPolygon = nestedPolygons.get(0);
            // inner polygons is remaining polygons after removing outer polygon
            nestedPolygons.remove(0);
            loopSets.add(new LoopSet(outerPolygon, nestedPolygons));
        }
        return loopSets;
    }

    /**
     * Organise the loops from loopsOfVerticesOnOpenFace into sets. Each set contains polygons
     * that are nested inside each other. There is no need to sort the nested polygons according
     * to which is nested inside which.
     */
    Set<List<List<Integer>>> getNestedPolygonSets()
    {
        Set<List<List<Integer>>> nestedPolygonSets = new HashSet<>();
        return nestedPolygonSets;
    }

    /**
     * Sort the given list of polygons by area, largest first.
     */
    private void sortByArea(List<List<Integer>> nestedPolygons)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Return if the given loop contains the given point. see
     * http://stackoverflow.com/questions/8721406/how-to-determine-if-a-point-is-inside-a-2d-convex-polygon
     */
    public boolean contains(Point test, int loopIndex)
    {
        List<Integer> loop = loopsOfVerticesOnOpenFace.get(loopIndex);
        Point[] points = new Point[loop.size()];
        for (int k = 0; k < points.length; k++)
        {
            double x = childMesh.getPoints().get(loop.get(k) * 3);
            double y = childMesh.getPoints().get(loop.get(k) * 3 + 2);
            points[k] = new Point(x, y);
        }
        int i;
        int j;
        boolean result = false;
        for (i = 0, j = points.length - 1; i < points.length; j = i++)
        {
            if ((points[i].y > test.y) != (points[j].y > test.y) &&
                (test.x <
                (points[j].x - points[i].x) * (test.y - points[i].y) / (points[j].y - points[i].y) +
                points[i].x))
            {
                result = !result;
            }
        }
        return result;
    }

    double getPolygonArea(Point[] polygon, int N)
    {
        int i;
        int j;
        double area = 0;
        for (i = 0; i < N; i++)
        {
            j = (i + 1) % N;
            area += polygon[i].x * polygon[j].y;
            area -= polygon[i].y * polygon[j].x;
        }
        area /= 2;
        return area < 0 ? -area : area;
    }
    
}
