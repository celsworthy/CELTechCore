package celtech.gcodetranslator.postprocessing.nodes.nodeFunctions;

import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import java.util.Iterator;

/**
 *
 * @author Ian
 */
public abstract class IteratorWithOrigin<T> implements Iterator<T>
{
    /**
     *
     * @param originNode
     */
    public abstract void setOriginNode(GCodeEventNode originNode);

}
