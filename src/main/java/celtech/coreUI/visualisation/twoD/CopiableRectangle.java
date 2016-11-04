package celtech.coreUI.visualisation.twoD;

import javafx.scene.shape.Rectangle;

/**
 *
 * @author Ian
 */
public class CopiableRectangle extends Rectangle implements Copiable
{    

    public CopiableRectangle()
    {
        super();
    }

    public CopiableRectangle(double d, double d1)
    {
        super(d, d1);
    }
    
    @Override
    public CopiableRectangle createCopy()
    {
        CopiableRectangle newRectangle = new CopiableRectangle(getWidth(), getHeight());
                
        return newRectangle;
    }
    
}
