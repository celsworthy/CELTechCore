package celtech.coreUI.visualisation.twoD;

import javafx.scene.shape.Circle;

/**
 *
 * @author Ian
 */
public class CopiableCircle extends Circle implements Copiable
{    

    public CopiableCircle(double d)
    {
        super(d);
    }
    
    @Override
    public CopiableCircle createCopy()
    {
        CopiableCircle newCircle = new CopiableCircle(getRadius());
                
        return newCircle;
    }
    
}
