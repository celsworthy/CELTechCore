/*
 * Copyright 2014 CEL UK
 */

package celtech.coreUI.visualisation;

/**
 *
 * @author tony
 */
public interface ShapeProvider
{

    public double getWidth();

    public double getDepth();

    public double getCentreX();

    public double getCentreZ();

    public double getHeight();
    
    public void addShapeChangeListener(ShapeChangeListener listener);
    
    public interface ShapeChangeListener {
        
        public void shapeChanged(ShapeProvider shapeProvider);
        
    }
    
}
