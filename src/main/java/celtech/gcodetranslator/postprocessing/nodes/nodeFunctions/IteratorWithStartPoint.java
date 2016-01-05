package celtech.gcodetranslator.postprocessing.nodes.nodeFunctions;

import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Ian
 */
public abstract class IteratorWithStartPoint<T> implements Iterator<T>
{

    public IteratorWithStartPoint(List<GCodeEventNode> startNodeHierarchy)
    {
        initialiseWithList(startNodeHierarchy);
    }

    public abstract void initialiseWithList(List<GCodeEventNode> startNodeHierarchy);
}
