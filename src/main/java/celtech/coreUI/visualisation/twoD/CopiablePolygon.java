package celtech.coreUI.visualisation.twoD;

import javafx.scene.shape.Polygon;

/**
 *
 * @author Ian
 */
public class CopiablePolygon extends Polygon implements Copiable
{    
    @Override
    public CopiablePolygon createCopy()
    {
        CopiablePolygon newPolygon = new CopiablePolygon();
        
        newPolygon.getPoints().addAll(getPoints());
        
        return newPolygon;
    }
    
}
