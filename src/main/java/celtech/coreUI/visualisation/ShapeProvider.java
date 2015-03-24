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

   public double getOriginalWidth();

    public double getOriginalDepth();

    public double getOriginalHeight();
    
    public double getScaledWidth();

    public double getScaledDepth();

    public double getScaledHeight();
    
    public double getCentreX();
    
    public double getCentreY();

    public double getCentreZ();
    
    public void addShapeChangeListener(ShapeChangeListener listener);

    public void removeShapeChangeListener(ShapeChangeListener listener);
    
    public interface ShapeChangeListener {
        
        public void shapeChanged(ShapeProvider shapeProvider);
        
    }
    
}
