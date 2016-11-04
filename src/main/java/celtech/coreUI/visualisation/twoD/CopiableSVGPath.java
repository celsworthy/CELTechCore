package celtech.coreUI.visualisation.twoD;

import javafx.scene.shape.SVGPath;

/**
 *
 * @author Ian
 */
public class CopiableSVGPath extends SVGPath implements Copiable
{    
    @Override
    public CopiableSVGPath createCopy()
    {
        CopiableSVGPath newSVGPath = new CopiableSVGPath();
        
        newSVGPath.setContent(getContent());
        
        return newSVGPath;
    }
    
}
