/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import celtech.utils.threed.MeshCutter.TopBottom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;


/**
 * CutResult represents one of the two parts of the cut mesh. It is also responsible for identifying
 * the topology of the nested polygons forming the perimeters on the closing top face (i.e. which
 * perimeters/polygons are inside which other polygons).
 *
 * @author tony
 */
class CutResult
{

    /**
     * The child mesh that was created by the split.
     */
    final TriangleMesh mesh;
    /**
     * The indices of the vertices of the child mesh, in sequence, that form the perimeter of the
     * new open face that needs to be triangulated. Some loops (list of points) may be holes inside
     * other loops.
     */
    final List<PolygonIndices> loopsOfVerticesOnOpenFace;

    final MeshCutter.BedToLocalConverter bedToLocalConverter;

    TopBottom topBottom;

    public CutResult(TriangleMesh mesh, List<PolygonIndices> loopsOfVerticesOnOpenFace,
        MeshCutter.BedToLocalConverter bedToLocalConverter, TopBottom topBottom)
    {
        this.mesh = mesh;
        this.loopsOfVerticesOnOpenFace = loopsOfVerticesOnOpenFace;
        this.bedToLocalConverter = bedToLocalConverter;
        this.topBottom = topBottom;
    }

    /**
     * Identify which of the loops in loopsOfVerticesOnOpenFace are internal to other loops. There
     * should not be any overlapping loops. Each LoopSet has one outer loop and zero or more inner
     * loops.
     *
     * Find sets of polygons that are nested inside each other. Sort each set by area giving a list
     * of nested polygons. The first in the list is the outer polygon.
     */
    public Set<LoopSet> identifyOuterLoopsAndInnerLoops()
    {
        Set<LoopSet> topLevelLoopSets = new HashSet<>();
        for (PolygonIndices polygonIndices : loopsOfVerticesOnOpenFace)
        {
            boolean added = false;
            for (LoopSet loopSet : topLevelLoopSets)
            {
                if (loopSet.contains(polygonIndices))
                {
                    System.out.println("Loop set " + loopSet.outerLoop.name + " contains " + polygonIndices.name);
                    loopSet.addToContainingChild(polygonIndices);
                    added = true;
                    break;
                } else if (contains(polygonIndices, loopSet.outerLoop))
                {
                    System.out.println(polygonIndices.name + " contains loop set " + loopSet.outerLoop.name);
                    Set<LoopSet> innerLoopSets = new HashSet<>();
                    innerLoopSets.add(loopSet);
                    LoopSet newLoopSet = new LoopSet(this, polygonIndices, innerLoopSets);
                    topLevelLoopSets.add(newLoopSet);
                    topLevelLoopSets.remove(loopSet);
                    added = true;
                    break;
                }
            }
            if (!added)
            {
                System.out.println("Create new loop set for " + polygonIndices.name);
                // polygonIndices is neither in a topLevelLoopSet nor contains a topLevelLoopSet
                // so create a new toplevelLoopSet.
                LoopSet newLoopSet = new LoopSet(this, polygonIndices, new HashSet<>());
                topLevelLoopSets.add(newLoopSet);
            }
        }
        return topLevelLoopSets;
    }

    /**
     * Sort the given list of polygons by area, largest first.
     */
    private List<PolygonIndices> sortByArea(Set<PolygonIndices> nestedPolygons)
    {
        List<PolygonIndices> sortedNestedPolygons = new ArrayList<>(nestedPolygons);
        Collections.sort(sortedNestedPolygons, new Comparator<PolygonIndices>()
        {

            @Override
            public int compare(PolygonIndices o1, PolygonIndices o2)
            {
                double a1 = getPolygonArea(o1);
                double a2 = getPolygonArea(o2);
                if (a1 > a2)
                {
                    return 1;
                } else if (a1 == a2)
                {
                    return 0;
                } else
                {
                    return -1;
                }
            }
        });
        return sortedNestedPolygons;
    }

    private Point getPointAt(PolygonIndices loop, int index)
    {
        Point3D point = MeshCutter.makePoint3D(mesh, loop.get(index));
        Point3D pointInBed = bedToLocalConverter.localToBed(point);
        return new Point(pointInBed.getX(), pointInBed.getZ());
    }

    public boolean contains(PolygonIndices outerPolygon, PolygonIndices innerPolygon)
    {
        Point point = getPointAt(innerPolygon, 0);
        return contains(point, outerPolygon);
    }

    /**
     * Return if the given loop contains the given point. see
     * http://stackoverflow.com/questions/8721406/how-to-determine-if-a-point-is-inside-a-2d-convex-polygon
     */
    public boolean contains(Point test, PolygonIndices loop)
    {
        Point[] points = new Point[loop.size()];
        for (int k = 0; k < points.length; k++)
        {
            points[k] = getPointAt(loop, k);
        }
        int i;
        int j;
        boolean result = false;
        for (i = 0, j = points.length - 1; i < points.length; j = i++)
        {
            if ((points[i].y > test.y) != (points[j].y > test.y) && (test.x < (points[j].x
                - points[i].x) * (test.y - points[i].y) / (points[j].y - points[i].y) + points[i].x))
            {
                result = !result;
            }
        }
        return result;
    }

    double getPolygonArea(PolygonIndices loop)
    {
        int N = loop.size();
        Point[] polygon = new Point[N];
        for (int k = 0; k < polygon.length; k++)
        {
            polygon[k] = getPointAt(loop, k);
        }

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


/**
 * PolygonIndices is a list of Integers each of which is a vertex (or face) id in the mesh. It is
 * therefore effectively a (usually closed) loop of vertices.
 *
 * @author tony
 */
class PolygonIndices extends ArrayList<Integer>
{
    String name;
    
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return "PolygonIndices{" + "name=" + name + '}';
    }
    
}


/**
 * A Region is an outer polygon and zero or more inner polygons (holes), forming a region that can
 * be triangulated.
 */
class Region
{

    final PolygonIndices outerLoop;
    final Set<PolygonIndices> innerLoops;

    public Region(PolygonIndices outerLoop, Set<PolygonIndices> innerLoops)
    {
        this.outerLoop = outerLoop;
        this.innerLoops = innerLoops;
    }
}


/**
 * A LoopSet is an outer polygon and a set of contained inner LoopSets.
 */
class LoopSet
{

    final PolygonIndices outerLoop;
    final Set<LoopSet> innerLoopSets;
    final CutResult cutResult;

    public LoopSet(CutResult cutResult, PolygonIndices outerLoop, Set<LoopSet> innerLoopSets)
    {
        this.cutResult = cutResult;
        this.outerLoop = outerLoop;
        this.innerLoopSets = innerLoopSets;
    }

    public boolean contains(PolygonIndices polygonIndices)
    {
        return cutResult.contains(outerLoop, polygonIndices);
    }

    /**
     * Return all the Regions ({@link Region}) described by this LoopSet.
     */
    public Set<Region> getRegions()
    {
        Set<Region> regions = new HashSet<>();
        Set<PolygonIndices> innerLoops = new HashSet<>();

        for (LoopSet innerLoopSet : innerLoopSets)
        {
            innerLoops.add(innerLoopSet.outerLoop);
        }
        Region region = new Region(outerLoop, innerLoops);
        regions.add(region);
        return regions;
    }

    /**
     * If the given polygonIndices is contained by one of the inner LoopSets then ask that inner
     * LoopSet to add it to one of its inner (containing) children, otherwise if no inner LoopSet
     * contains the given polygonIndices then add it as another inner LoopSet of this LoopSet.
     */
    public void addToContainingChild(PolygonIndices polygonIndices)
    {
        if (!contains(polygonIndices))
        {
            throw new RuntimeException("given polygonIndices must be contained by outer loop");
        }
        boolean innerLoopContainsGivenLoop = false;
        for (LoopSet innerLoopSet : innerLoopSets)
        {
            if (innerLoopSet.contains(polygonIndices))
            {
                innerLoopSet.addToContainingChild(polygonIndices);
                innerLoopContainsGivenLoop = true;
                break;
            }
        }
        if (!innerLoopContainsGivenLoop)
        {
            // add given polygonIndices as a new inner LoopSet.
            innerLoopSets.add(new LoopSet(cutResult, polygonIndices, new HashSet<>()));
        }
    }
}


/**
 * The X and Z coordinate of the point in the bed space maps to X and Y for polygon analysis. The
 * cut height (Y) is fixed in the bed coordinate system so we ignore that dimension for polygon
 * analysis.
 */
class Point
{

    final double x;
    final double y;

    public Point(double x, double y)
    {
        this.x = x;
        this.y = y;
    }
}
