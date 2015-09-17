/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import celtech.utils.threed.NonManifoldLoopDetector.Direction;
import javafx.geometry.Point2D;


final class ManifoldEdge
{

    final int v0;
    final int v1;
    final Vertex vertex0;
    final Vertex vertex1;
    boolean visitedForwards = false;
    boolean visitedBackwards = false;

    public ManifoldEdge(int v0, int v1, Vertex vertex0, Vertex vertex1)
    {
        this.v0 = v0;
        this.v1 = v1;
        this.vertex0 = vertex0;
        this.vertex1 =vertex1;
    }
    
    public boolean isVisited(Direction direction) {
        if (direction == Direction.FORWARDS) {
            return isVisitedForwards();
        } else {
            return isVisitedBackwards();
        }
    }
    
    public void setVisited(Direction direction) {
        if (direction == Direction.FORWARDS) {
            setVisitedForwards(true);
        } else {
            setVisitedBackwards(true);
        }
    }
    
    public Point2D getVectorForDirection(Direction direction) {
        double x = vertex1.x - vertex0.x;
        double z = vertex1.z - vertex0.z;
        if (direction == Direction.FORWARDS) {
            return new Point2D(x, z);
        } else {
            return new Point2D(-x, -z);
        }
    }

    public boolean isVisitedForwards()
    {
        return visitedForwards;
    }

    public void setVisitedForwards(boolean visitedForwards)
    {
        this.visitedForwards = visitedForwards;
    }

    public boolean isVisitedBackwards()
    {
        return visitedBackwards;
    }

    public void setVisitedBackwards(boolean visitedBackwards)
    {
        this.visitedBackwards = visitedBackwards;
    }
    
    

    @Override
    public String toString()
    {
        return "ManifoldEdge{" + "v0=" + v0 + ", v1=" + v1 + ", vertex0=" + vertex0 + ", vertex1=" +
            vertex1 + '}';
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof ManifoldEdge))
        {
            return false;
        }
        if (obj == this)
        {
            return true;
        }

        ManifoldEdge other = (ManifoldEdge) obj;
        if ((other.v0 == v0 && other.v1 == v1) || (other.v1 == v0 && other.v0 == v1))
        {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        // hash code must be symmetrical in v0/v1 
        return v0 + v1;
    }
}


