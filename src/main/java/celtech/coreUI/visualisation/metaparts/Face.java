package celtech.coreUI.visualisation.metaparts;

/**
 *
 * @author Ian
 */
public class Face
{
    private int vertexIndices[] = new int[3];

    public int[] getVertexIndices()
    {
        return vertexIndices;
    }
    
    public int getVertexIndex(int vertexNumber)
    {
        return vertexIndices[vertexNumber];
    }

    public void setVertexIndices(int[] vertexIndices)
    {
        this.vertexIndices = vertexIndices;
    }
    
    public void setVertexIndex(int vertexNumber, int vertexIndex)
    {
        vertexIndices[vertexNumber] = vertexIndex;
    }
}
