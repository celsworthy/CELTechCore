/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;


/**
 * @author tony
 */
final class Vertex
{

    int meshVertexIndex;
    final float x;
    final float y;
    final float z;

    public Vertex(int meshVertexIndex, float x, float y, float z)
    {
        this.meshVertexIndex = meshVertexIndex;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vertex(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString()
    {
        return "Vertex{" + "meshVertexIndex=" + meshVertexIndex + ", x=" + x + ", y=" + y + ", z="
            + z + '}';
    }

//    @Override
//    public boolean equals(Object obj)
//    {
//        if (!(obj instanceof Vertex))
//        {
//            return false;
//        }
//        if (obj == this)
//        {
//            return true;
//        }
//
//        Vertex other = (Vertex) obj;
//        if (other.x != x)
//        {
//            return false;
//        }
//        if (other.y != y)
//        {
//            return false;
//        }
//        if (other.z != z)
//        {
//            return false;
//        }
//        return true;
//    }
//
//    @Override
//    public int hashCode()
//    {
//        return (int) (x * 1237f + y * 107f + 23 * z);
//    }
}


